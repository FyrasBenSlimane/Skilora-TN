package com.skilora;

import com.skilora.formation.entity.Certificate;
import com.skilora.formation.entity.FormationRating;
import com.skilora.formation.service.CertificateVerificationServer;
import com.skilora.formation.service.FormationRatingService;
import com.skilora.formation.service.FormationRatingService.RatingStatistics;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 *   SKILORA - Formation Feedback & QR Code Test Suite
 * ──────────────────────────────────────────────────────────────────────
 *   Targeted tests for the three features audited:
 *   1. Feedback Stars (1-5 star rating)
 *   2. Like / Dislike
 *   3. QR Code & Certificate Verification Server
 *
 *   Coverage:
 *   • FormationRating entity — constructors, validation, edge cases
 *   • FormationRatingService — singleton, RatingStatistics
 *   • CertificateVerificationServer — lifecycle, IP detection, helpers
 *   • Integration wiring checks
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DisplayName("Formation Feedback & QR Code Tests")
class FormationFeedbackAndQRTests {

    // ═══════════════════════════════════════════════════════════════
    //  Section 1: FormationRating Entity — Star Rating
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(1)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("1. Star Rating — FormationRating Entity")
    class StarRatingEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor sets timestamps")
        void defaultConstructorSetsTimestamps() {
            FormationRating r = new FormationRating();
            assertNotNull(r.getCreatedAt(), "createdAt should be set by default constructor");
            assertNotNull(r.getUpdatedAt(), "updatedAt should be set by default constructor");
        }

        @Test @Order(2)
        @DisplayName("3-arg constructor sets userId, formationId, starRating")
        void threeArgConstructor() {
            FormationRating r = new FormationRating(10, 20, 4);
            assertEquals(10, r.getUserId());
            assertEquals(20, r.getFormationId());
            assertEquals(4, r.getStarRating());
            assertNull(r.getIsLiked(), "isLiked should be null when not set");
            assertNotNull(r.getCreatedAt());
        }

        @Test @Order(3)
        @DisplayName("4-arg constructor sets all fields including isLiked")
        void fourArgConstructor() {
            FormationRating r = new FormationRating(10, 20, true, 5);
            assertEquals(10, r.getUserId());
            assertEquals(20, r.getFormationId());
            assertEquals(5, r.getStarRating());
            assertTrue(r.getIsLiked());
        }

        @ParameterizedTest
        @ValueSource(ints = {1, 2, 3, 4, 5})
        @Order(4)
        @DisplayName("Valid star ratings (1-5) accepted by setter")
        void validStarRatings(int stars) {
            FormationRating r = new FormationRating();
            r.setStarRating(stars);
            assertEquals(stars, r.getStarRating());
        }

        @Test @Order(5)
        @DisplayName("Star rating 0 throws IllegalArgumentException")
        void starRatingZeroThrows() {
            FormationRating r = new FormationRating();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> r.setStarRating(0));
            assertTrue(ex.getMessage().contains("between 1 and 5"));
        }

        @Test @Order(6)
        @DisplayName("Star rating 6 throws IllegalArgumentException")
        void starRatingSixThrows() {
            FormationRating r = new FormationRating();
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> r.setStarRating(6));
            assertTrue(ex.getMessage().contains("between 1 and 5"));
        }

        @Test @Order(7)
        @DisplayName("Negative star rating throws IllegalArgumentException")
        void negativeStarRatingThrows() {
            FormationRating r = new FormationRating();
            assertThrows(IllegalArgumentException.class, () -> r.setStarRating(-1));
        }

        @Test @Order(8)
        @DisplayName("Very large star rating throws IllegalArgumentException")
        void largeStarRatingThrows() {
            FormationRating r = new FormationRating();
            assertThrows(IllegalArgumentException.class, () -> r.setStarRating(100));
        }

        @Test @Order(9)
        @DisplayName("Star rating boundary — exactly 1 (minimum)")
        void starRatingBoundaryMin() {
            FormationRating r = new FormationRating();
            r.setStarRating(1);
            assertEquals(1, r.getStarRating());
        }

        @Test @Order(10)
        @DisplayName("Star rating boundary — exactly 5 (maximum)")
        void starRatingBoundaryMax() {
            FormationRating r = new FormationRating();
            r.setStarRating(5);
            assertEquals(5, r.getStarRating());
        }

        @Test @Order(11)
        @DisplayName("Star rating can be updated after initial set")
        void starRatingCanBeUpdated() {
            FormationRating r = new FormationRating(1, 1, 3);
            assertEquals(3, r.getStarRating());
            r.setStarRating(5);
            assertEquals(5, r.getStarRating());
            r.setStarRating(1);
            assertEquals(1, r.getStarRating());
        }

        @Test @Order(12)
        @DisplayName("Timestamps can be set manually")
        void timestampsCanBeSet() {
            FormationRating r = new FormationRating();
            LocalDateTime now = LocalDateTime.of(2025, 1, 15, 10, 30);
            r.setCreatedAt(now);
            r.setUpdatedAt(now);
            assertEquals(now, r.getCreatedAt());
            assertEquals(now, r.getUpdatedAt());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 2: FormationRating Entity — Like / Dislike
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(2)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("2. Like / Dislike — FormationRating Entity")
    class LikeDislikeEntityTests {

        @Test @Order(1)
        @DisplayName("isLiked defaults to null (no choice)")
        void isLikedDefaultsToNull() {
            FormationRating r = new FormationRating(1, 1, 3);
            assertNull(r.getIsLiked(), "isLiked should default to null (no like/dislike choice)");
        }

        @Test @Order(2)
        @DisplayName("isLiked = TRUE represents 'liked'")
        void isLikedTrue() {
            FormationRating r = new FormationRating();
            r.setStarRating(4);
            r.setIsLiked(true);
            assertTrue(r.getIsLiked());
        }

        @Test @Order(3)
        @DisplayName("isLiked = FALSE represents 'disliked'")
        void isLikedFalse() {
            FormationRating r = new FormationRating();
            r.setStarRating(2);
            r.setIsLiked(false);
            assertFalse(r.getIsLiked());
        }

        @Test @Order(4)
        @DisplayName("isLiked = NULL represents 'no choice'")
        void isLikedNull() {
            FormationRating r = new FormationRating();
            r.setStarRating(3);
            r.setIsLiked(null);
            assertNull(r.getIsLiked());
        }

        @Test @Order(5)
        @DisplayName("isLiked can be toggled from liked to disliked")
        void isLikedToggle() {
            FormationRating r = new FormationRating(1, 1, true, 4);
            assertTrue(r.getIsLiked());
            r.setIsLiked(false);
            assertFalse(r.getIsLiked());
        }

        @Test @Order(6)
        @DisplayName("isLiked can be toggled from disliked to null")
        void isLikedToNull() {
            FormationRating r = new FormationRating(1, 1, false, 2);
            assertFalse(r.getIsLiked());
            r.setIsLiked(null);
            assertNull(r.getIsLiked());
        }

        @Test @Order(7)
        @DisplayName("Like with 5-star rating combination")
        void likeWithFiveStars() {
            FormationRating r = new FormationRating(1, 1, true, 5);
            assertTrue(r.getIsLiked());
            assertEquals(5, r.getStarRating());
        }

        @Test @Order(8)
        @DisplayName("Dislike with 1-star rating combination")
        void dislikeWithOneStar() {
            FormationRating r = new FormationRating(1, 1, false, 1);
            assertFalse(r.getIsLiked());
            assertEquals(1, r.getStarRating());
        }

        @Test @Order(9)
        @DisplayName("4-arg constructor with null isLiked")
        void fourArgWithNullLike() {
            FormationRating r = new FormationRating(1, 1, null, 3);
            assertNull(r.getIsLiked());
            assertEquals(3, r.getStarRating());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 3: FormationRating equals / hashCode / toString
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(3)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("3. FormationRating equals/hashCode/toString")
    class RatingEqualsHashTests {

        @Test @Order(1)
        @DisplayName("equals by ID — same ID")
        void equalsSameId() {
            FormationRating r1 = new FormationRating();
            r1.setId(42);
            r1.setStarRating(3);
            FormationRating r2 = new FormationRating();
            r2.setId(42);
            r2.setStarRating(5);
            assertEquals(r1, r2, "Ratings with same ID should be equal");
        }

        @Test @Order(2)
        @DisplayName("not equals — different ID")
        void notEqualsDifferentId() {
            FormationRating r1 = new FormationRating();
            r1.setId(1);
            r1.setStarRating(3);
            FormationRating r2 = new FormationRating();
            r2.setId(2);
            r2.setStarRating(3);
            assertNotEquals(r1, r2);
        }

        @Test @Order(3)
        @DisplayName("hashCode consistent for same ID")
        void hashCodeConsistent() {
            FormationRating r1 = new FormationRating();
            r1.setId(10);
            r1.setStarRating(4);
            FormationRating r2 = new FormationRating();
            r2.setId(10);
            r2.setStarRating(2);
            assertEquals(r1.hashCode(), r2.hashCode());
        }

        @Test @Order(4)
        @DisplayName("not equals null")
        void notEqualsNull() {
            FormationRating r = new FormationRating();
            r.setId(1);
            r.setStarRating(3);
            assertNotEquals(null, r);
        }

        @Test @Order(5)
        @DisplayName("not equals different type")
        void notEqualsDifferentType() {
            FormationRating r = new FormationRating();
            r.setId(1);
            r.setStarRating(3);
            assertNotEquals("string", r);
        }

        @Test @Order(6)
        @DisplayName("equals self")
        void equalsSelf() {
            FormationRating r = new FormationRating();
            r.setId(1);
            r.setStarRating(3);
            assertEquals(r, r);
        }

        @Test @Order(7)
        @DisplayName("toString contains key fields")
        void toStringContainsFields() {
            FormationRating r = new FormationRating(5, 10, true, 4);
            r.setId(1);
            String str = r.toString();
            assertTrue(str.contains("userId=5"));
            assertTrue(str.contains("formationId=10"));
            assertTrue(str.contains("starRating=4"));
            assertTrue(str.contains("isLiked=true"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 4: FormationRatingService — Singleton & Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(4)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("4. FormationRatingService — Structure & Validation")
    class RatingServiceTests {

        @Test @Order(1)
        @DisplayName("Singleton returns same instance")
        void singletonInstance() {
            FormationRatingService s1 = FormationRatingService.getInstance();
            FormationRatingService s2 = FormationRatingService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2, "getInstance() must return the same singleton instance");
        }

        @Test @Order(2)
        @DisplayName("Singleton is thread-safe (concurrent access)")
        void singletonThreadSafe() throws InterruptedException {
            final FormationRatingService[] instances = new FormationRatingService[10];
            Thread[] threads = new Thread[10];
            for (int i = 0; i < 10; i++) {
                final int idx = i;
                threads[i] = new Thread(() -> instances[idx] = FormationRatingService.getInstance());
                threads[i].start();
            }
            for (Thread t : threads) t.join();
            for (int i = 1; i < 10; i++) {
                assertSame(instances[0], instances[i],
                        "All threads must get the same singleton instance");
            }
        }

        @Test @Order(3)
        @DisplayName("submitRating rejects star rating < 1")
        void submitRatingRejectsLow() {
            assertThrows(IllegalArgumentException.class,
                    () -> FormationRatingService.getInstance().submitRating(1, 1, true, 0));
        }

        @Test @Order(4)
        @DisplayName("submitRating rejects star rating > 5")
        void submitRatingRejectsHigh() {
            assertThrows(IllegalArgumentException.class,
                    () -> FormationRatingService.getInstance().submitRating(1, 1, true, 6));
        }

        @Test @Order(5)
        @DisplayName("submitRating rejects negative star rating")
        void submitRatingRejectsNegative() {
            assertThrows(IllegalArgumentException.class,
                    () -> FormationRatingService.getInstance().submitRating(1, 1, true, -1));
        }

        @Test @Order(6)
        @DisplayName("submitRating rejects star rating 100")
        void submitRatingRejects100() {
            assertThrows(IllegalArgumentException.class,
                    () -> FormationRatingService.getInstance().submitRating(1, 1, null, 100));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 5: RatingStatistics Inner Class
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(5)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("5. RatingStatistics — Aggregation Model")
    class RatingStatisticsTests {

        @Test @Order(1)
        @DisplayName("RatingStatistics stores all fields")
        void statisticsFields() {
            RatingStatistics stats = new RatingStatistics(25, 4.2, 18, 7);
            assertEquals(25, stats.getTotalRatings());
            assertEquals(4.2, stats.getAverageRating(), 0.001);
            assertEquals(18, stats.getLikeCount());
            assertEquals(7, stats.getDislikeCount());
        }

        @Test @Order(2)
        @DisplayName("RatingStatistics with zero totals")
        void statisticsZero() {
            RatingStatistics stats = new RatingStatistics(0, 0.0, 0, 0);
            assertEquals(0, stats.getTotalRatings());
            assertEquals(0.0, stats.getAverageRating(), 0.001);
            assertEquals(0, stats.getLikeCount());
            assertEquals(0, stats.getDislikeCount());
        }

        @Test @Order(3)
        @DisplayName("RatingStatistics — all likes no dislikes")
        void statisticsAllLikes() {
            RatingStatistics stats = new RatingStatistics(10, 5.0, 10, 0);
            assertEquals(10, stats.getTotalRatings());
            assertEquals(5.0, stats.getAverageRating(), 0.001);
            assertEquals(10, stats.getLikeCount());
            assertEquals(0, stats.getDislikeCount());
        }

        @Test @Order(4)
        @DisplayName("RatingStatistics — all dislikes no likes")
        void statisticsAllDislikes() {
            RatingStatistics stats = new RatingStatistics(5, 1.4, 0, 5);
            assertEquals(5, stats.getTotalRatings());
            assertEquals(1.4, stats.getAverageRating(), 0.001);
            assertEquals(0, stats.getLikeCount());
            assertEquals(5, stats.getDislikeCount());
        }

        @Test @Order(5)
        @DisplayName("RatingStatistics — fractional average rating")
        void statisticsFractionalAverage() {
            RatingStatistics stats = new RatingStatistics(3, 3.6667, 2, 1);
            assertEquals(3, stats.getTotalRatings());
            assertEquals(3.667, stats.getAverageRating(), 0.01);
        }

        @Test @Order(6)
        @DisplayName("RatingStatistics — like + dislike = total (or less, since some may have null isLiked)")
        void statisticsLikesPlusDislikes() {
            RatingStatistics stats = new RatingStatistics(10, 3.5, 6, 3);
            // 6 likes + 3 dislikes = 9, but total is 10 (1 has null isLiked)
            assertTrue(stats.getLikeCount() + stats.getDislikeCount() <= stats.getTotalRatings(),
                    "likes + dislikes should be ≤ totalRatings (some may have null isLiked)");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 6: QR Code — CertificateVerificationServer
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(6)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("6. QR Code — CertificateVerificationServer")
    class CertificateVerificationServerTests {

        @Test @Order(1)
        @DisplayName("getLocalIPAddress returns non-null, non-empty string")
        void localIPAddressNotNull() {
            String ip = CertificateVerificationServer.getLocalIPAddress();
            assertNotNull(ip, "getLocalIPAddress() should never return null");
            assertFalse(ip.isEmpty(), "getLocalIPAddress() should not return empty string");
        }

        @Test @Order(2)
        @DisplayName("getLocalIPAddress returns valid IPv4 or 'localhost'")
        void localIPAddressFormat() {
            String ip = CertificateVerificationServer.getLocalIPAddress();
            // Should be either "localhost" or a valid IPv4 address
            boolean isLocalhost = "localhost".equals(ip);
            boolean isIPv4 = ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
            assertTrue(isLocalhost || isIPv4,
                    "IP should be 'localhost' or valid IPv4 format, got: " + ip);
        }

        @Test @Order(3)
        @DisplayName("Server port is -1 when not running")
        void portWhenNotRunning() {
            // If server hasn't been started in this test context, port should be -1
            if (!CertificateVerificationServer.isRunning()) {
                assertEquals(-1, CertificateVerificationServer.getPort());
            }
        }

        @Test @Order(4)
        @DisplayName("Server start → isRunning → port > 0 → stop lifecycle")
        void serverLifecycle() {
            try {
                // Start the server
                CertificateVerificationServer.start();
                assertTrue(CertificateVerificationServer.isRunning(), "Server should be running after start()");

                int port = CertificateVerificationServer.getPort();
                assertTrue(port > 0, "Port should be positive when server is running, got: " + port);

                // Stop the server
                CertificateVerificationServer.stop();
                assertFalse(CertificateVerificationServer.isRunning(), "Server should NOT be running after stop()");
            } finally {
                // Ensure cleanup even if test fails
                CertificateVerificationServer.stop();
            }
        }

        @Test @Order(5)
        @DisplayName("Double start is safe (idempotent)")
        void doubleStartIsSafe() {
            try {
                CertificateVerificationServer.start();
                int port1 = CertificateVerificationServer.getPort();
                CertificateVerificationServer.start(); // Should not throw
                int port2 = CertificateVerificationServer.getPort();
                assertEquals(port1, port2, "Double start should keep the same port");
            } finally {
                CertificateVerificationServer.stop();
            }
        }

        @Test @Order(6)
        @DisplayName("Double stop is safe (idempotent)")
        void doubleStopIsSafe() {
            CertificateVerificationServer.start();
            CertificateVerificationServer.stop();
            assertDoesNotThrow(() -> CertificateVerificationServer.stop(),
                    "Double stop should not throw");
        }

        @Test @Order(7)
        @DisplayName("Server binds to port in expected range (8443+)")
        void portInExpectedRange() {
            try {
                CertificateVerificationServer.start();
                int port = CertificateVerificationServer.getPort();
                assertTrue(port >= 8443 && port < 8493,
                        "Port should be in range [8443, 8493), got: " + port);
            } finally {
                CertificateVerificationServer.stop();
            }
        }

        @Test @Order(8)
        @DisplayName("Server start → stop → restart works")
        void restartCycle() {
            try {
                CertificateVerificationServer.start();
                assertTrue(CertificateVerificationServer.isRunning());
                CertificateVerificationServer.stop();
                assertFalse(CertificateVerificationServer.isRunning());
                CertificateVerificationServer.start();
                assertTrue(CertificateVerificationServer.isRunning());
            } finally {
                CertificateVerificationServer.stop();
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 7: QR URL Construction Logic
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(7)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("7. QR Code — URL Construction & Validation")
    class QRUrlConstructionTests {

        @Test @Order(1)
        @DisplayName("QR URL format: http://{ip}:{port}/verify/{certId}")
        void qrUrlFormat() {
            // Simulate the URL construction logic from CertificateViewController
            String localIP = "192.168.1.50";
            int serverPort = 8443;
            String certId = "CERT-2025-001";
            String qrUrl = "http://" + localIP + ":" + serverPort + "/verify/" + certId.trim();
            assertEquals("http://192.168.1.50:8443/verify/CERT-2025-001", qrUrl);
        }

        @Test @Order(2)
        @DisplayName("QR URL with whitespace certId is trimmed")
        void qrUrlTrims() {
            String certId = "  CERT-2025-001  ";
            String qrUrl = "http://192.168.1.50:8443/verify/" + certId.trim();
            assertEquals("http://192.168.1.50:8443/verify/CERT-2025-001", qrUrl);
        }

        @Test @Order(3)
        @DisplayName("LAN IP detection — 192.168.x.x is LAN")
        void isLanIP192() {
            String ip = "192.168.1.100";
            boolean isLan = ip.startsWith("192.168.") || ip.startsWith("10.");
            assertTrue(isLan);
        }

        @Test @Order(4)
        @DisplayName("LAN IP detection — 10.x.x.x is LAN")
        void isLanIP10() {
            String ip = "10.0.0.5";
            boolean isLan = ip.startsWith("192.168.") || ip.startsWith("10.");
            assertTrue(isLan);
        }

        @Test @Order(5)
        @DisplayName("LAN IP detection — 172.16.x.x is LAN")
        void isLanIP172() {
            String ip = "172.16.0.1";
            boolean isLan = ip.startsWith("192.168.") || ip.startsWith("10.") ||
                    (ip.startsWith("172.") && Integer.parseInt(ip.split("\\.")[1]) >= 16
                            && Integer.parseInt(ip.split("\\.")[1]) <= 31);
            assertTrue(isLan);
        }

        @Test @Order(6)
        @DisplayName("LAN IP detection — 172.32.x.x is NOT LAN")
        void isNotLanIP172_32() {
            String ip = "172.32.0.1";
            boolean isLan = ip.startsWith("192.168.") || ip.startsWith("10.") ||
                    (ip.startsWith("172.") && Integer.parseInt(ip.split("\\.")[1]) >= 16
                            && Integer.parseInt(ip.split("\\.")[1]) <= 31);
            assertFalse(isLan);
        }

        @Test @Order(7)
        @DisplayName("LAN IP detection — 8.8.8.8 is NOT LAN")
        void isNotLanPublicIP() {
            String ip = "8.8.8.8";
            boolean isLan = ip.startsWith("192.168.") || ip.startsWith("10.") ||
                    (ip.startsWith("172.") && Integer.parseInt(ip.split("\\.")[1]) >= 16
                            && Integer.parseInt(ip.split("\\.")[1]) <= 31);
            assertFalse(isLan);
        }

        @Test @Order(8)
        @DisplayName("Certificate has certNumber for QR — identifier resolution")
        void certIdentifierResolution() {
            Certificate cert = new Certificate();
            cert.setCertificateNumber("CERT-2025-042");
            String certId = cert.getCertificateNumber();
            if (certId == null || certId.isBlank()) {
                certId = cert.getVerificationToken();
            }
            assertNotNull(certId);
            assertEquals("CERT-2025-042", certId);
        }

        @Test @Order(9)
        @DisplayName("Certificate falls back to verificationToken when certNumber is null")
        void certIdentifierFallback() {
            Certificate cert = new Certificate();
            // certNumber is null by default
            cert.setVerificationToken("TOKEN-ABC-123");
            String certId = cert.getCertificateNumber();
            if (certId == null || certId.isBlank()) {
                certId = cert.getVerificationToken();
            }
            assertEquals("TOKEN-ABC-123", certId);
        }

        @Test @Order(10)
        @DisplayName("Certificate with blank certNumber falls back to token")
        void certIdentifierBlankFallback() {
            Certificate cert = new Certificate();
            cert.setCertificateNumber("   ");
            cert.setVerificationToken("TOKEN-XYZ-789");
            String certId = cert.getCertificateNumber();
            if (certId == null || certId.isBlank()) {
                certId = cert.getVerificationToken();
            }
            assertEquals("TOKEN-XYZ-789", certId);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 8: Integration Wiring Checks
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(8)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("8. Integration — Wiring Checks")
    class IntegrationWiringTests {

        @Test @Order(1)
        @DisplayName("FormationRating entity can represent full feedback (stars + like)")
        void fullFeedbackEntity() {
            FormationRating r = new FormationRating(100, 200, true, 5);
            r.setId(42);
            assertEquals(100, r.getUserId());
            assertEquals(200, r.getFormationId());
            assertEquals(5, r.getStarRating());
            assertTrue(r.getIsLiked());
            assertEquals(42, r.getId());
            assertNotNull(r.getCreatedAt());
            assertNotNull(r.getUpdatedAt());
            assertNotNull(r.toString());
        }

        @Test @Order(2)
        @DisplayName("FormationRating entity can represent dislike with low stars")
        void dislikeWithLowStars() {
            FormationRating r = new FormationRating(100, 200, false, 1);
            assertFalse(r.getIsLiked());
            assertEquals(1, r.getStarRating());
        }

        @Test @Order(3)
        @DisplayName("FormationRating entity can represent neutral (null isLiked)")
        void neutralFeedback() {
            FormationRating r = new FormationRating(100, 200, null, 3);
            assertNull(r.getIsLiked());
            assertEquals(3, r.getStarRating());
        }

        @Test @Order(4)
        @DisplayName("RatingStatistics correctly represents mixed feedback")
        void mixedFeedbackStats() {
            // 10 ratings, avg 3.5, 6 likes, 3 dislikes, 1 neutral
            RatingStatistics stats = new RatingStatistics(10, 3.5, 6, 3);
            assertEquals(10, stats.getTotalRatings());
            assertEquals(3.5, stats.getAverageRating(), 0.001);
            assertEquals(6, stats.getLikeCount());
            assertEquals(3, stats.getDislikeCount());
            // 6 + 3 = 9 < 10 → 1 user chose neutral (null isLiked)
            assertEquals(1, stats.getTotalRatings() - stats.getLikeCount() - stats.getDislikeCount());
        }

        @Test @Order(5)
        @DisplayName("FormationRatingService is accessible from getInstance()")
        void serviceAccessible() {
            FormationRatingService service = FormationRatingService.getInstance();
            assertNotNull(service);
        }

        @Test @Order(6)
        @DisplayName("CertificateVerificationServer static methods are accessible")
        void serverMethodsAccessible() {
            // These should not throw — just verifying API surface
            assertDoesNotThrow(() -> CertificateVerificationServer.isRunning());
            assertDoesNotThrow(() -> CertificateVerificationServer.getPort());
            assertDoesNotThrow(() -> CertificateVerificationServer.getLocalIPAddress());
        }

        @Test @Order(7)
        @DisplayName("Full QR flow: server start → build URL → validate → stop")
        void fullQRFlow() {
            try {
                CertificateVerificationServer.start();
                assertTrue(CertificateVerificationServer.isRunning());

                int port = CertificateVerificationServer.getPort();
                assertTrue(port > 0);

                String ip = CertificateVerificationServer.getLocalIPAddress();
                assertNotNull(ip);
                assertFalse(ip.isEmpty());

                // Build QR URL like CertificateViewController does
                String certId = "CERT-TEST-001";
                String qrUrl = "http://" + ip + ":" + port + "/verify/" + certId;

                assertTrue(qrUrl.startsWith("http://"));
                assertTrue(qrUrl.contains("/verify/"));
                assertTrue(qrUrl.contains(certId));
                assertTrue(qrUrl.contains(String.valueOf(port)));
            } finally {
                CertificateVerificationServer.stop();
            }
        }
    }
}
