package com.skilora.community.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * CloudinaryUploadService — Service d'intégration de l'API Cloudinary pour l'upload d'images.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║  API UTILISÉE : Cloudinary (https://cloudinary.com)                  ║
 * ║  Type        : API REST avec upload non signé (unsigned upload)      ║
 * ║  Format      : Multipart/form-data (envoi de fichier binaire)        ║
 * ║  Limite      : 10 MB par image (plan gratuit : 25 crédits/mois)     ║
 * ║  Retour      : URL publique HTTPS de l'image hébergée               ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *
 * Qu'est-ce que Cloudinary ?
 *   Cloudinary est un service cloud spécialisé dans l'hébergement et la transformation
 *   d'images/vidéos. Il offre un CDN (Content Delivery Network) mondial pour une
 *   distribution rapide des fichiers.
 *
 * Pourquoi Cloudinary dans Skilora ?
 *   - Les posts peuvent contenir des images (profil, partages, événements)
 *   - Au lieu de stocker les images localement, on les héberge dans le cloud
 *   - L'utilisateur sélectionne un fichier local → il est uploadé vers Cloudinary
 *   - L'API retourne une URL publique HTTPS qu'on stocke dans la base de données
 *
 * Fonctionnement de l'upload non signé (Unsigned Upload) :
 *   1. L'utilisateur choisit un fichier image via un FileChooser JavaFX
 *   2. Le fichier est lu en bytes et envoyé en multipart/form-data
 *   3. Cloudinary reçoit le fichier, le stocke, et retourne l'URL
 *   4. L'URL est sauvegardée dans la colonne image_url du post
 *
 * Configuration nécessaire :
 *   - CLOUD_NAME  : nom de votre cloud Cloudinary (visible dans le dashboard)
 *   - UPLOAD_PRESET : preset d'upload non signé (créé dans Settings > Upload)
 *
 * Pattern : Singleton thread-safe
 */
public class CloudinaryUploadService {

    private static final Logger logger = LoggerFactory.getLogger(CloudinaryUploadService.class);

    // ══════════════════════════════════════════════════════════
    //  CONFIGURATION CLOUDINARY
    //  Ces valeurs viennent du dashboard Cloudinary :
    //    https://console.cloudinary.com/settings/upload
    // ══════════════════════════════════════════════════════════

    /** Nom du cloud Cloudinary (identifiant unique de votre compte) */
    private static final String CLOUD_NAME = "skilora";

    /**
     * Preset d'upload non signé.
     * Créé dans Cloudinary Dashboard > Settings > Upload > Upload presets
     * Le mode "unsigned" permet l'upload sans authentification côté serveur.
     */
    private static final String UPLOAD_PRESET = "skilora_unsigned";

    /** URL de l'API d'upload Cloudinary (format: /v1_1/{cloud_name}/image/upload) */
    private static final String UPLOAD_URL = "https://api.cloudinary.com/v1_1/" + CLOUD_NAME + "/image/upload";

    /** Taille max d'upload en bytes (10 MB) */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

    /** Extensions de fichiers image autorisées */
    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp"};

    // ── Singleton ──
    private static volatile CloudinaryUploadService instance;

    /** Constructeur privé (Singleton) */
    private CloudinaryUploadService() {}

    /**
     * Retourne l'instance unique du service d'upload.
     * Double-Checked Locking pour thread-safety.
     */
    public static CloudinaryUploadService getInstance() {
        if (instance == null) {
            synchronized (CloudinaryUploadService.class) {
                if (instance == null) {
                    instance = new CloudinaryUploadService();
                }
            }
        }
        return instance;
    }

    /**
     * Upload un fichier image vers Cloudinary et retourne l'URL publique.
     *
     * Étapes détaillées :
     *   1. Valider le fichier (existe, taille, extension)
     *   2. Lire le fichier en bytes
     *   3. Construire la requête HTTP multipart/form-data
     *   4. Envoyer le fichier à Cloudinary via POST
     *   5. Lire la réponse JSON
     *   6. Extraire l'URL sécurisée (secure_url) de la réponse
     *
     * Format de la requête multipart :
     *   --boundary
     *   Content-Disposition: form-data; name="file"; filename="image.jpg"
     *   Content-Type: image/jpeg
     *   [contenu binaire du fichier]
     *   --boundary
     *   Content-Disposition: form-data; name="upload_preset"
     *   skilora_unsigned
     *   --boundary--
     *
     * @param file le fichier image à uploader (depuis FileChooser)
     * @return l'URL HTTPS publique de l'image, ou null si échec
     */
    public String uploadImage(File file) {
        // ── VALIDATION 1 : Le fichier existe et est lisible ──
        if (file == null || !file.exists()) {
            throw new RuntimeException("Le fichier est introuvable ou n'existe pas.");
        }

        // ── VALIDATION 2 : Taille maximale (10 MB) ──
        if (file.length() > MAX_FILE_SIZE) {
            throw new RuntimeException("Le fichier dépasse la taille maximale de 10 MB.");
        }

        // ── VALIDATION 3 : Extension autorisée ──
        if (!isAllowedExtension(file.getName())) {
            throw new RuntimeException("Extension non autorisée. Formats : jpg, png, gif, webp, bmp.");
        }

        try {
            // ── ÉTAPE 1 : Préparer la requête multipart ──
            // Le "boundary" est un séparateur unique qui sépare les parties
            String boundary = "----CloudinaryBoundary" + UUID.randomUUID().toString().replace("-", "");

            // ── ÉTAPE 2 : Ouvrir la connexion HTTP POST ──
            URI uri = URI.create(UPLOAD_URL);
            java.net.URL url = uri.toURL();
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);  // Active l'envoi de données (POST body)
            connection.setRequestMethod("POST");
            connection.setConnectTimeout(30000);  // Timeout connexion : 30 secondes
            connection.setReadTimeout(30000);      // Timeout lecture : 30 secondes
            // Header indiquant le type de contenu (multipart avec le boundary)
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            // ── ÉTAPE 3 : Écrire le corps de la requête ──
            try (OutputStream outputStream = connection.getOutputStream();
                 PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8), true)) {

                // ── Partie 1 : le fichier image (données binaires) ──
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
                        .append(file.getName()).append("\"\r\n");
                writer.append("Content-Type: ").append(getContentType(file.getName())).append("\r\n");
                writer.append("\r\n");
                writer.flush();

                // Écrire les bytes du fichier dans le flux de sortie
                Files.copy(file.toPath(), outputStream);
                outputStream.flush();
                writer.append("\r\n");

                // ── Partie 2 : le upload_preset (identifie la configuration Cloudinary) ──
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n");
                writer.append("\r\n");
                writer.append(UPLOAD_PRESET).append("\r\n");

                // ── Partie 3 : dossier de destination dans Cloudinary ──
                writer.append("--").append(boundary).append("\r\n");
                writer.append("Content-Disposition: form-data; name=\"folder\"\r\n");
                writer.append("\r\n");
                writer.append("skilora/community").append("\r\n"); // Dossier dans le cloud

                // ── Fin du multipart ──
                writer.append("--").append(boundary).append("--\r\n");
                writer.flush();
            }

            // ── ÉTAPE 4 : Lire la réponse de Cloudinary ──
            int responseCode = connection.getResponseCode();
            logger.info("Cloudinary upload response code: {}", responseCode);

            // Lire le corps de la réponse
            InputStream responseStream = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();

            StringBuilder responseBody = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBody.append(line);
                }
            }

            // ── ÉTAPE 5 : Parser la réponse JSON ──
            if (responseCode == 200) {
                // Réponse réussie — structure JSON Cloudinary :
                // { "secure_url": "https://res.cloudinary.com/skilora/image/upload/v123/image.jpg",
                //   "public_id": "skilora/community/abc123", "format": "jpg", "width": 800, ... }
                JSONObject jsonResponse = new JSONObject(responseBody.toString());
                String secureUrl = jsonResponse.getString("secure_url"); // URL HTTPS de l'image
                String publicId = jsonResponse.getString("public_id");   // ID public dans Cloudinary
                logger.info("Image uploaded successfully: {} (public_id: {})", secureUrl, publicId);
                return secureUrl; // ← URL de l'image hébergée dans le cloud
            } else {
                // Erreur de l'API — loguer et tenter le fallback local
                logger.warn("Cloudinary upload failed with code {}: {} — fallback local", responseCode, responseBody);
                return saveLocally(file);
            }

        } catch (IOException e) {
            // Erreur réseau (timeout, DNS, etc.) — fallback local
            logger.warn("Cloudinary upload error: {} — fallback local", e.getMessage());
            return saveLocally(file);
        }
    }

    /**
     * Fallback local : copie l'image dans le dossier data/uploads/ du projet.
     * Utilisé quand l'upload Cloudinary échoue (erreur réseau, credentials invalides, etc.).
     *
     * Fonctionnement :
     *   1. Crée le dossier data/uploads/ s'il n'existe pas
     *   2. Génère un nom unique (UUID + extension originale)
     *   3. Copie le fichier image dans ce dossier
     *   4. Retourne l'URI file:/// pour affichage JavaFX
     *
     * @param originalFile le fichier image d'origine
     * @return URI locale du fichier copié (file:///...)
     */
    private String saveLocally(File originalFile) {
        try {
            // Créer le dossier data/uploads/ à la racine du projet
            Path uploadsDir = Paths.get("data", "uploads");
            Files.createDirectories(uploadsDir); // Crée le dossier s'il n'existe pas

            // Générer un nom unique pour éviter les conflits
            String extension = getFileExtension(originalFile.getName());
            String uniqueName = UUID.randomUUID().toString().substring(0, 8) + extension;
            Path destination = uploadsDir.resolve(uniqueName);

            // Copier le fichier (remplacer si existe)
            Files.copy(originalFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            // Retourner l'URI locale (file:///C:/...)
            String localUri = destination.toAbsolutePath().toUri().toString();
            logger.info("Image sauvegardée localement : {}", localUri);
            return localUri;
        } catch (IOException e) {
            logger.error("Fallback local failed: {}", e.getMessage());
            throw new RuntimeException("Impossible de sauvegarder l'image : " + e.getMessage());
        }
    }

    /**
     * Extrait l'extension d'un fichier (incluant le point).
     * Ex: "photo.jpg" → ".jpg"
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex).toLowerCase() : ".jpg";
    }

    /**
     * Vérifie si l'extension du fichier est autorisée.
     * Compare l'extension (en minuscules) avec la liste des extensions acceptées.
     *
     * @param filename le nom du fichier
     * @return true si l'extension est .jpg, .jpeg, .png, .gif, .webp ou .bmp
     */
    private boolean isAllowedExtension(String filename) {
        String lower = filename.toLowerCase();
        for (String ext : ALLOWED_EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

    /**
     * Détermine le type MIME du fichier selon son extension.
     * Utilisé dans le header Content-Type de la requête multipart.
     *
     * @param filename le nom du fichier
     * @return le type MIME (ex: "image/jpeg", "image/png")
     */
    private String getContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/jpeg"; // Par défaut pour .jpg et .jpeg
    }

    /**
     * Retourne les extensions de fichiers autorisées pour le FileChooser de JavaFX.
     * Format : ["*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"]
     *
     * @return tableau des patterns d'extension pour javafx.stage.FileChooser.ExtensionFilter
     */
    public String[] getAllowedExtensionPatterns() {
        return new String[]{"*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp", "*.bmp"};
    }
}
