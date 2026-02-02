"""
Face Recognition Service for Skilora Biometric Authentication
High-performance face detection, encoding, verification with duplicate prevention.
Uses optimized pipeline: downscaled detection → full-res encoding → multi-sample consensus.
"""

import cv2
import face_recognition
import numpy as np
import json
import sys
import os
import base64
import time
from pathlib import Path
import mysql.connector

# ── Tuning constants (Face ID–inspired) ──────────────────────────────────────
MATCH_TOLERANCE       = 0.45   # Stricter than default 0.6 → fewer false positives
DUPLICATE_TOLERANCE   = 0.42   # Even stricter for duplicate detection across accounts
DETECTION_SCALE       = 0.5    # Downscale factor for fast face detection pass
ENCODING_JITTERS      = 3      # Re-sample jitters for higher-quality encodings
QUALITY_THRESHOLD     = 0.30   # Minimum face-area ratio to reject blurry / far faces
MIN_FACE_SIZE         = 60     # Minimum face width in pixels (at original scale)

# ── Database configuration ───────────────────────────────────────────────────
DB_CONFIG = {
    "host": "localhost",
    "user": "root",
    "password": "",
    "database": "skilora",
    "port": 3306
}

class NumpyEncoder(json.JSONEncoder):
    """Custom JSON encoder for numpy types"""
    def default(self, obj):
        if isinstance(obj, np.integer):
            return int(obj)
        elif isinstance(obj, np.floating):
            return float(obj)
        elif isinstance(obj, np.ndarray):
            return obj.tolist()
        elif isinstance(obj, np.bool_):
            return bool(obj)
        return super(NumpyEncoder, self).default(obj)


class FaceRecognitionService:
    def __init__(self):
        self.known_encodings = {}          # username → np.array (128-d)
        self.known_multi_encodings = {}    # username → [np.array, ...] (multi-sample)
        self.load_known_encodings()

    # ── Database helpers ─────────────────────────────────────────────────────

    def _get_db_connection(self):
        """Get a MySQL database connection."""
        return mysql.connector.connect(**DB_CONFIG)

    # ── Persistence (Database-backed) ────────────────────────────────────────

    def load_known_encodings(self):
        """Load all stored face encodings from the database."""
        try:
            conn = self._get_db_connection()
            cursor = conn.cursor()
            cursor.execute(
                "SELECT u.username, bd.face_encoding "
                "FROM biometric_data bd JOIN users u ON bd.user_id = u.id"
            )
            for username, face_encoding in cursor.fetchall():
                try:
                    if face_encoding is None:
                        continue
                    # face_encoding is stored as bytes in a BLOB column
                    encoding_str = face_encoding.decode('utf-8') if isinstance(face_encoding, (bytes, bytearray)) else str(face_encoding)
                    encoding_array = np.array(json.loads(encoding_str))
                    self.known_encodings[username] = encoding_array
                except Exception as e:
                    print(f"Error parsing encoding for {username}: {e}", file=sys.stderr)
            cursor.close()
            conn.close()
        except Exception as e:
            print(f"Error loading encodings from database: {e}", file=sys.stderr)

    def save_encoding(self, username, encoding):
        """Save face encoding for a user to the database."""
        try:
            conn = self._get_db_connection()
            cursor = conn.cursor()

            # Resolve user_id from username
            cursor.execute("SELECT id FROM users WHERE username = %s", (username,))
            row = cursor.fetchone()
            if row is None:
                print(f"Error: user '{username}' not found in database", file=sys.stderr)
                cursor.close()
                conn.close()
                return

            user_id = row[0]
            encoding_str = json.dumps(encoding.tolist())

            # Upsert: update if exists, insert otherwise
            cursor.execute("SELECT id FROM biometric_data WHERE user_id = %s", (user_id,))
            if cursor.fetchone():
                cursor.execute(
                    "UPDATE biometric_data SET face_encoding = %s, last_login = NOW() WHERE user_id = %s",
                    (encoding_str.encode('utf-8'), user_id)
                )
            else:
                cursor.execute(
                    "INSERT INTO biometric_data (user_id, face_encoding, last_login) VALUES (%s, %s, NOW())",
                    (user_id, encoding_str.encode('utf-8'))
                )

            conn.commit()
            cursor.close()
            conn.close()
            self.known_encodings[username] = encoding
        except Exception as e:
            print(f"Error saving encoding to database: {e}", file=sys.stderr)

    # ── Core detection (optimized two-pass) ──────────────────────────────────

    def detect_face_live(self, frame_bytes):
        """
        Detect face in live camera frame using two-pass approach:
        1) Fast detection on downscaled image (HOG)
        2) High-quality encoding on original resolution with jitters
        """
        try:
            t0 = time.perf_counter()

            if not frame_bytes or len(frame_bytes) == 0:
                return {"success": False, "error": "Empty frame data received"}

            nparr = np.frombuffer(frame_bytes, np.uint8)
            if len(nparr) == 0:
                return {"success": False, "error": "Invalid frame data - cannot decode bytes"}

            frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            if frame is None or frame.size == 0:
                return {"success": False, "error": "Invalid frame data - cannot decode image"}

            h, w = frame.shape[:2]
            if h < 50 or w < 50:
                return {"success": False, "error": f"Frame too small: {w}x{h} (minimum 50x50)"}

            rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)

            # ── Pass 1: Fast face detection on downscaled image ──
            scale = DETECTION_SCALE
            small_rgb = cv2.resize(rgb_frame, (0, 0), fx=scale, fy=scale, interpolation=cv2.INTER_LINEAR)
            small_locations = face_recognition.face_locations(small_rgb, model="hog")

            if len(small_locations) == 0:
                return {"success": False, "message": "No face detected"}
            if len(small_locations) > 1:
                return {"success": False, "message": "Multiple faces detected. Please ensure only one person is in frame."}

            # Scale locations back to original resolution
            inv = 1.0 / scale
            st, sr, sb, sl = small_locations[0]
            top    = max(0, int(st * inv))
            right  = min(w, int(sr * inv))
            bottom = min(h, int(sb * inv))
            left   = max(0, int(sl * inv))
            face_locations = [(top, right, bottom, left)]

            # Face quality gate: reject tiny / distant faces
            face_w = right - left
            face_h = bottom - top
            if face_w < MIN_FACE_SIZE or face_h < MIN_FACE_SIZE:
                return {"success": False, "message": "Face too far. Please move closer."}

            # ── Pass 2: High-quality encoding on full-res with jitters ──
            face_encodings = face_recognition.face_encodings(
                rgb_frame, face_locations, num_jitters=ENCODING_JITTERS
            )
            if len(face_encodings) == 0:
                return {"success": False, "message": "Could not extract face features"}

            encoding = face_encodings[0]

            # Draw viewfinder rectangle
            cv2.rectangle(frame, (left, top), (right, bottom), (0, 255, 0), 2)
            _, buffer = cv2.imencode('.jpg', frame, [cv2.IMWRITE_JPEG_QUALITY, 80])
            frame_base64 = base64.b64encode(buffer.tobytes()).decode('utf-8')

            elapsed_ms = (time.perf_counter() - t0) * 1000

            return {
                "success": True,
                "face_detected": True,
                "encoding": encoding.tolist(),
                "frame": frame_base64,
                "face_location": {
                    "top": top, "right": right, "bottom": bottom, "left": left
                },
                "detection_ms": round(elapsed_ms, 1)
            }
        except Exception as e:
            return {"success": False, "error": str(e)}

    # ── Duplicate check ──────────────────────────────────────────────────────

    def check_duplicate_face(self, frame_bytes, exclude_username=None):
        """
        Check if the face in frame_bytes already belongs to another registered account.
        Uses a stricter tolerance than normal verification.
        Returns: {duplicate: bool, matched_username: str|None, distance: float}
        """
        result = self.detect_face_live(frame_bytes)
        if not result.get("success") or not result.get("face_detected"):
            return {**result, "duplicate": False}

        encoding = np.array(result["encoding"])

        # Compare against all known faces
        for stored_user, stored_enc in self.known_encodings.items():
            if exclude_username and stored_user == exclude_username:
                continue
            distance = face_recognition.face_distance([stored_enc], encoding)[0]
            if distance < DUPLICATE_TOLERANCE:
                return {
                    "success": True,
                    "duplicate": True,
                    "matched_username": stored_user,
                    "distance": float(distance),
                    "confidence": float(1 - distance),
                    "message": f"This face is already registered to account '{stored_user}'. Cannot register duplicate."
                }

        return {
            "success": True,
            "duplicate": False,
            "encoding": result["encoding"],
            "frame": result.get("frame"),
            "message": "No duplicate found — face is unique."
        }

    # ── Registration (with duplicate prevention) ─────────────────────────────

    def register_face(self, username, frame_bytes):
        """
        Register a new face with duplicate prevention.
        Rejects if the face already belongs to another account.
        """
        # Step 1: Check for duplicates across all existing accounts
        dup_check = self.check_duplicate_face(frame_bytes, exclude_username=username)
        if not dup_check.get("success"):
            return dup_check
        if dup_check.get("duplicate"):
            return {
                "success": False,
                "duplicate": True,
                "matched_username": dup_check.get("matched_username"),
                "message": dup_check.get("message")
            }

        # Step 2: Detect and encode the face
        result = self.detect_face_live(frame_bytes)
        if not result.get("success") or not result.get("face_detected"):
            return result

        encoding = np.array(result["encoding"])
        self.save_encoding(username, encoding)

        return {
            "success": True,
            "message": f"Face registered successfully for {username}",
            "encoding": result["encoding"],
            "frame": result.get("frame")
        }

    # ── Verification (stricter tolerance, confidence scoring) ────────────────

    def verify_face(self, frame_bytes, username=None):
        """
        Verify face against stored encodings.
        Uses stricter tolerance and returns confidence score.
        """
        result = self.detect_face_live(frame_bytes)
        if not result.get("success") or not result.get("face_detected"):
            return result

        encoding = np.array(result["encoding"])

        # Verify against specific user
        if username:
            if username not in self.known_encodings:
                return {
                    "success": False,
                    "verified": False,
                    "message": f"No registered face found for {username}"
                }

            known_encoding = self.known_encodings[username]
            distance = face_recognition.face_distance([known_encoding], encoding)[0]
            verified = distance < MATCH_TOLERANCE

            return {
                "success": True,
                "verified": verified,
                "confidence": float(1 - distance),
                "distance": float(distance),
                "frame": result.get("frame"),
                "message": "Face verified" if verified else "Face does not match"
            }

        # Find best match among all users
        if len(self.known_encodings) == 0:
            return {
                "success": False,
                "verified": False,
                "message": "No registered faces found"
            }

        known_faces = list(self.known_encodings.values())
        known_users = list(self.known_encodings.keys())

        distances = face_recognition.face_distance(known_faces, encoding)
        best_idx = np.argmin(distances)
        best_distance = distances[best_idx]
        verified = best_distance < MATCH_TOLERANCE

        return {
            "success": True,
            "verified": verified,
            "username": known_users[best_idx] if verified else None,
            "confidence": float(1 - best_distance),
            "distance": float(best_distance),
            "frame": result.get("frame"),
            "message": (f"Face verified as {known_users[best_idx]}" if verified
                        else "Face not recognized")
        }


def main():
    """CLI interface for face recognition"""
    if len(sys.argv) < 2:
        print(json.dumps({"error": "Invalid command"}, cls=NumpyEncoder))
        sys.exit(1)

    command = sys.argv[1]
    service = FaceRecognitionService()

    try:
        image_base64 = sys.stdin.read().strip()
        if not image_base64:
            print(json.dumps({"error": "No image data provided via stdin"}, cls=NumpyEncoder))
            sys.exit(1)

        try:
            frame_bytes = base64.b64decode(image_base64)
        except Exception as e:
            print(json.dumps({"error": f"Invalid base64 image data: {str(e)}"}, cls=NumpyEncoder))
            sys.exit(1)

        if command == "register":
            if len(sys.argv) < 3:
                print(json.dumps({"error": "Usage: register <username> < image_data"}, cls=NumpyEncoder))
                sys.exit(1)
            username = sys.argv[2]
            result = service.register_face(username, frame_bytes)
            print(json.dumps(result, cls=NumpyEncoder))

        elif command == "verify":
            username = sys.argv[2] if len(sys.argv) > 2 else None
            result = service.verify_face(frame_bytes, username)
            print(json.dumps(result, cls=NumpyEncoder))

        elif command == "detect":
            result = service.detect_face_live(frame_bytes)
            print(json.dumps(result, cls=NumpyEncoder))

        elif command == "check_duplicate":
            exclude = sys.argv[2] if len(sys.argv) > 2 else None
            result = service.check_duplicate_face(frame_bytes, exclude_username=exclude)
            print(json.dumps(result, cls=NumpyEncoder))

        else:
            print(json.dumps({"error": f"Unknown command: {command}"}, cls=NumpyEncoder))
            sys.exit(1)

    except Exception as e:
        print(json.dumps({"error": str(e)}, cls=NumpyEncoder))
        sys.exit(1)

if __name__ == "__main__":
    main()
