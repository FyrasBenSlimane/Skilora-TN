# 📋 RAPPORT DÉTAILLÉ — APIs & Métiers Avancés
## Projet Skilora — JavaFX / Java 21

**Auteur :** Mouhamed Aziz Khaldi  
**Date :** 23 Février 2026  
**Module :** Communauté & Fonctionnalités Transversales  

---

# 📑 TABLE DES MATIÈRES

1. [Vue d'ensemble du projet](#1-vue-densemble-du-projet)
2. [API 1 — Traduction dynamique (MyMemory)](#2-api-1--traduction-dynamique-mymemory)
3. [API 2 — Upload d'images (Cloudinary)](#3-api-2--upload-dimages-cloudinary)
4. [API 3 — Envoi d'e-mails (Gmail SMTP)](#4-api-3--envoi-de-mails-gmail-smtp)
5. [API 4 — Reconnaissance faciale (Python + dlib)](#5-api-4--reconnaissance-faciale-python--dlib)
6. [API 5 — Crawling d'offres d'emploi (ANETI, Reddit, RSS)](#6-api-5--crawling-doffres-demploi-aneti-reddit-rss)
7. [Métier avancé 1 — Algorithme de matching recrutement](#7-métier-avancé-1--algorithme-de-matching-recrutement)
8. [Métier avancé 2 — Chatbot & réponses automatiques](#8-métier-avancé-2--chatbot--réponses-automatiques)
9. [Métier avancé 3 — Calcul de paie tunisien (CNSS + IRPP)](#9-métier-avancé-3--calcul-de-paie-tunisien-cnss--irpp)
10. [Métier avancé 4 — Système de mentions @](#10-métier-avancé-4--système-de-mentions-)
11. [Métier avancé 5 — Notifications temps réel](#11-métier-avancé-5--notifications-temps-réel)
12. [Métier avancé 6 — Gamification / Achievements](#12-métier-avancé-6--gamification--achievements)
13. [Résumé des endpoints API externes](#13-résumé-des-endpoints-api-externes)
14. [Résumé des dépendances (pom.xml)](#14-résumé-des-dépendances-pomxml)
15. [Arborescence des fichiers concernés](#15-arborescence-des-fichiers-concernés)

---

# 1. Vue d'ensemble du projet

## Qu'est-ce que Skilora ?

Skilora est une **plateforme de gestion RH et communautaire** développée en **JavaFX** (Java 21). Elle intègre 6 modules principaux :

| Module | Description | Dossier |
|--------|-------------|---------|
| **Communauté** | Posts, messages, groupes, événements, blog | `com.skilora.community` |
| **Utilisateurs** | Authentification, profil, biométrie | `com.skilora.user` |
| **Recrutement** | Offres d'emploi, candidatures, matching | `com.skilora.recruitment` |
| **Formation** | Formations, certifications, mentorat | `com.skilora.formation` |
| **Finance** | Paie, contrats, taux de change | `com.skilora.finance` |
| **Support** | Tickets, chatbot, FAQ | `com.skilora.support` |

## Technologies principales

- **Langage :** Java 21 + JavaFX 21
- **Base de données :** MySQL 8 (via HikariCP)
- **Scripts IA/ML :** Python 3 (face_recognition, aiohttp)
- **Build tool :** Maven
- **Hashing :** jBCrypt
- **JSON :** org.json + Gson

---

# 2. API 1 — Traduction dynamique (MyMemory)

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/community/service/TranslationService.java
```

## 🎯 Rôle

Traduire **dynamiquement** le contenu des posts en 3 langues (Français, Anglais, Arabe) en utilisant l'API **MyMemory Translated** — une API REST gratuite de traduction automatique.

## 🌐 API utilisée

| Propriété | Valeur |
|-----------|--------|
| **Nom** | MyMemory Translated |
| **URL** | `https://api.mymemory.translated.net/get` |
| **Méthode HTTP** | GET |
| **Authentification** | Aucune (gratuite) |
| **Limite** | 5000 caractères/requête, ~10 000/jour |
| **Format réponse** | JSON |

## 📐 Architecture : Stratégie multi-couche (5 niveaux)

```
┌─────────────────────────────────────────┐
│         Texte à traduire                │
└─────────────┬───────────────────────────┘
              │
    ┌─────────▼─────────┐
    │  COUCHE 1 : Cache  │ ← LRU (100 entrées) — résultat instantané
    │  LRU en mémoire    │
    └─────────┬─────────┘
              │ (miss)
    ┌─────────▼──────────────┐
    │  COUCHE 2 : Dictionnaire│ ← 80+ mots courants fr/en/ar
    │  local intégré          │
    └─────────┬──────────────┘
              │ (miss)
    ┌─────────▼──────────────┐
    │  COUCHE 3 : API         │ ← Appel HTTP GET à MyMemory
    │  MyMemory (dynamique)   │
    └─────────┬──────────────┘
              │ (échec)
    ┌─────────▼──────────────┐
    │  COUCHE 4 : Traduction  │ ← Ex: fr→en→ar (pivot anglais)
    │  en 2 étapes            │
    └─────────┬──────────────┘
              │ (échec)
    ┌─────────▼──────────────┐
    │  COUCHE 5 : Retourner   │ ← Dernier recours
    │  le texte original      │
    └─────────────────────────┘
```

## 🔧 Étapes détaillées de la traduction

### Étape 1 — Détection de la langue source

Le service détecte automatiquement la langue du texte avant traduction :

```java
// Fichier : TranslationService.java — Méthode detectLanguage()

public String detectLanguage(String text) {
    // Étape 0 : Nettoyer (supprimer emojis ✏🌐 etc.)
    String cleaned = stripEmojisAndSpecialChars(text);

    // Étape 1 : Caractères arabes (Unicode \u0600-\u06FF)
    long arabicChars = cleaned.chars()
        .filter(c -> c >= 0x0600 && c <= 0x06FF).count();
    if (arabicChars > totalLetters * 0.3) return "ar";

    // Étape 2 : Accents français (é, è, ê, ç, à, ù...)
    long frenchAccents = lower.chars().filter(c ->
        c == 'é' || c == 'è' || c == 'ê' || c == 'ç' ...
    ).count();
    if (frenchAccents > 0) return "fr";

    // Étape 3 : Contractions françaises (j', l', d', c'est)
    if (lower.matches(".*\\b[jldcnqs]'\\w+.*")) return "fr";

    // Étape 4 : Vocabulaire français (50+ mots)
    // Étape 5 : Mots-outils français (le, la, les, de, du...)
    // Étape 6 : Mots-outils anglais (the, is, are, has...)
    // Par défaut : français (contexte tunisien)
    return "fr";
}
```

### Étape 2 — Nettoyage du texte

Avant d'envoyer le texte à l'API, on supprime les emojis et caractères spéciaux qui perturbent la traduction :

```java
// Fichier : TranslationService.java — Méthode stripEmojisAndSpecialChars()

private String stripEmojisAndSpecialChars(String text) {
    // Garde : lettres (toutes langues), chiffres, espaces, apostrophes
    return text.replaceAll("[^\\p{L}\\p{N}\\s'-]", "").trim();
}
// Exemple : "bonjour✏🌐" → "bonjour"
```

### Étape 3 — Appel API MyMemory

L'appel HTTP GET construit dynamiquement l'URL avec le texte et la paire de langues :

```java
// Fichier : TranslationService.java — Méthode callMyMemoryAPI()

private String callMyMemoryAPI(String text, String sourceLang, String targetLang) {
    // 1. Nettoyer le texte
    String cleanText = stripEmojisAndSpecialChars(text);

    // 2. Construire l'URL
    String encodedText = URLEncoder.encode(cleanText, StandardCharsets.UTF_8);
    String langPair = sourceLang + "|" + targetLang;
    String urlString = API_URL + "?q=" + encodedText
                     + "&langpair=" + langPair + "&mt=1";
    // Exemple : https://api.mymemory.translated.net/get
    //           ?q=bonjour&langpair=fr|en&mt=1

    // 3. Ouvrir connexion HTTP GET
    URL url = new URL(urlString);
    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
    connection.setRequestMethod("GET");
    connection.setConnectTimeout(8000);   // 8 secondes max
    connection.setReadTimeout(8000);
    connection.setRequestProperty("User-Agent", "Skilora/1.0");

    // 4. Lire la réponse JSON
    int responseCode = connection.getResponseCode();  // 200 = OK
    // ... lire le body via BufferedReader ...

    // 5. Parser le JSON
    JSONObject jsonResponse = new JSONObject(response.toString());
    // Structure retournée par MyMemory :
    // {
    //   "responseData": {
    //     "translatedText": "hello",
    //     "match": 1.0
    //   },
    //   "matches": [
    //     {"translation": "hello", "match": 1.0, "created-by": "MT!"},
    //     {"translation": "hi", "match": 0.8, "created-by": "user123"}
    //   ]
    // }

    // 6. Choisir la meilleure traduction
    // Priorité : MT (machine) > TM (mémoire communautaire) > primaire
    if (bestMT != null) return bestMT;
    if (bestTM != null && bestTMScore >= 0.3) return bestTM;
    return primaryTranslation;
}
```

### Étape 4 — Validation de qualité

Chaque traduction est vérifiée avant d'être acceptée :

```java
// Fichier : TranslationService.java — Méthode isValidTranslation()

private boolean isValidTranslation(String translation, String original, String targetLang) {
    // Rejeter si identique au texte source
    if (cleanTranslation.equalsIgnoreCase(cleanOriginal)) return false;
    // Rejeter si contient des URLs ou caractères suspects
    if (translation.contains("http") || translation.contains("@")) return false;
    // Rejeter si anormalement long (5x le texte source)
    if (cleanTranslation.length() > cleanOriginal.length() * 5 + 50) return false;
    // Pour l'arabe : vérifier la présence de caractères arabes
    if ("ar".equals(targetLang)) {
        long arabicChars = translation.chars()
            .filter(c -> c >= 0x0600 && c <= 0x06FF).count();
        if (arabicChars == 0) return false;
    }
    return true;
}
```

### Étape 5 — Affichage dans l'interface

L'utilisateur clique sur "🌐 Traduire" → un menu popup apparaît → il choisit la langue → le résultat s'affiche :

```java
// Fichier : CommunityController.java — Méthode showTranslationMenu()
// Localisation : ligne ~2988

private void showTranslationMenu(TLButton translateBtn, Label contentLabel, String originalText) {
    Popup popup = new Popup();
    // Menu avec 3 options : Français, English, العربية

    item.setOnMouseClicked(ev -> {
        popup.hide();
        translateBtn.setText("⏳  Traduction...");

        new Thread(() -> {
            String sourceLang = detectedLang;
            String translated = translationService.translate(originalText, sourceLang, langCode);
            Platform.runLater(() -> {
                // Afficher : "🌐 [EN] Hello"
                contentLabel.setText("🌐 [" + langCode.toUpperCase() + "] " + translated);
                translateBtn.setText("↩  Original");
            });
        }, "TranslateThread").start();
    });
}
```

## 📊 Résumé des méthodes

| Méthode | Rôle | Ligne |
|---------|------|-------|
| `translate()` | Point d'entrée principal (5 couches) | ~213 |
| `callMyMemoryAPI()` | Appel HTTP GET à MyMemory | ~340 |
| `detectLanguage()` | Détecter la langue automatiquement | ~454 |
| `isValidTranslation()` | Filtrer les résultats garbage | ~560 |
| `stripEmojisAndSpecialChars()` | Nettoyer emojis/symboles | ~436 |
| `clearCache()` | Vider le cache LRU | ~595 |

---

# 3. API 2 — Upload d'images (Cloudinary)

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/community/service/CloudinaryUploadService.java
```

## 🎯 Rôle

Permettre aux utilisateurs d'**uploader des images** (photos de profil, images de posts, événements) vers le cloud **Cloudinary**, et obtenir une URL HTTPS publique.

## 🌐 API utilisée

| Propriété | Valeur |
|-----------|--------|
| **Nom** | Cloudinary |
| **URL** | `https://api.cloudinary.com/v1_1/skilora/image/upload` |
| **Méthode HTTP** | POST (multipart/form-data) |
| **Authentification** | Upload non signé (preset `skilora_unsigned`) |
| **Limite** | 10 MB par fichier |
| **Formats** | jpg, jpeg, png, gif, webp, bmp |

## 📐 Architecture du flux d'upload

```
┌──────────────┐     ┌───────────────────┐     ┌──────────────────┐
│  FileChooser │ ──► │  Validation       │ ──► │  HTTP POST       │
│  (JavaFX)    │     │  (taille, format) │     │  Multipart       │
└──────────────┘     └───────────────────┘     └────────┬─────────┘
                                                        │
                                               ┌────────▼─────────┐
                                               │  Cloudinary API   │
                                               │  (cloud upload)   │
                                               └────────┬─────────┘
                                                        │
                              ┌──────────────────────────┤
                              │                          │
                     ┌────────▼─────────┐      ┌────────▼─────────┐
                     │   ✅ Succès       │      │   ❌ Échec        │
                     │   secure_url     │      │   Fallback local  │
                     │   (HTTPS)        │      │   data/uploads/   │
                     └──────────────────┘      └──────────────────┘
```

## 🔧 Étapes détaillées

### Étape 1 — Validation du fichier

```java
// Fichier : CloudinaryUploadService.java — Méthode uploadImage()

public String uploadImage(File file) {
    // Validation 1 : Le fichier existe ?
    if (file == null || !file.exists())
        throw new RuntimeException("Le fichier est introuvable.");

    // Validation 2 : Taille ≤ 10 MB ?
    if (file.length() > MAX_FILE_SIZE)  // 10 * 1024 * 1024 = 10 485 760 bytes
        throw new RuntimeException("Le fichier dépasse 10 MB.");

    // Validation 3 : Extension autorisée ?
    if (!isAllowedExtension(file.getName()))
        throw new RuntimeException("Extension non autorisée.");
}
```

### Étape 2 — Construction de la requête multipart

Le standard HTTP **multipart/form-data** permet d'envoyer des fichiers binaires via POST :

```java
// Fichier : CloudinaryUploadService.java

// Séparateur unique entre les parties du multipart
String boundary = "----CloudinaryBoundary" + UUID.randomUUID().toString().replace("-", "");

HttpURLConnection connection = (HttpURLConnection) url.openConnection();
connection.setDoOutput(true);
connection.setRequestMethod("POST");
connection.setConnectTimeout(30000);  // 30 secondes
connection.setRequestProperty("Content-Type",
    "multipart/form-data; boundary=" + boundary);

// Corps de la requête :
// ─── Partie 1 : Le fichier image (binaire) ───
writer.append("--").append(boundary).append("\r\n");
writer.append("Content-Disposition: form-data; name=\"file\"; filename=\"")
      .append(file.getName()).append("\"\r\n");
writer.append("Content-Type: image/jpeg\r\n\r\n");
writer.flush();
Files.copy(file.toPath(), outputStream);  // Envoi des bytes

// ─── Partie 2 : Le preset d'upload ───
writer.append("--").append(boundary).append("\r\n");
writer.append("Content-Disposition: form-data; name=\"upload_preset\"\r\n\r\n");
writer.append("skilora_unsigned").append("\r\n");

// ─── Partie 3 : Le dossier destination ───
writer.append("Content-Disposition: form-data; name=\"folder\"\r\n\r\n");
writer.append("skilora/community").append("\r\n");

writer.append("--").append(boundary).append("--\r\n");  // Fin
```

### Étape 3 — Lecture de la réponse JSON

```java
// Réponse Cloudinary (JSON) :
// {
//   "secure_url": "https://res.cloudinary.com/skilora/image/upload/v1234/image.jpg",
//   "public_id": "skilora/community/abc123",
//   "format": "jpg",
//   "width": 800,
//   "height": 600
// }

if (responseCode == 200) {
    JSONObject jsonResponse = new JSONObject(responseBody.toString());
    String secureUrl = jsonResponse.getString("secure_url");  // ← URL finale
    return secureUrl;
} else {
    return saveLocally(file);  // Fallback si Cloudinary échoue
}
```

### Étape 4 — Fallback local (en cas d'échec)

```java
// Fichier : CloudinaryUploadService.java — Méthode saveLocally()

private String saveLocally(File originalFile) {
    Path uploadsDir = Paths.get("data", "uploads");
    Files.createDirectories(uploadsDir);

    String uniqueName = UUID.randomUUID().toString().substring(0, 8) + extension;
    Path destination = uploadsDir.resolve(uniqueName);
    Files.copy(originalFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

    return destination.toAbsolutePath().toUri().toString();
    // Retourne : file:///C:/project/data/uploads/a1b2c3d4.jpg
}
```

## 📊 Résumé des méthodes

| Méthode | Rôle |
|---------|------|
| `uploadImage(File)` | Upload vers Cloudinary (ou fallback local) |
| `saveLocally(File)` | Copie locale si Cloudinary est indisponible |
| `isAllowedExtension()` | Vérifie l'extension du fichier |
| `getContentType()` | Détermine le type MIME |
| `getAllowedExtensionPatterns()` | Pour le FileChooser JavaFX |

---

# 4. API 3 — Envoi d'e-mails (Gmail SMTP)

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/community/service/EmailService.java
```

## 🎯 Rôle

Envoyer des **e-mails HTML** (code OTP pour la réinitialisation de mot de passe) via le **serveur SMTP de Gmail**.

## 🌐 API utilisée

| Propriété | Valeur |
|-----------|--------|
| **Protocole** | SMTP avec STARTTLS |
| **Serveur** | `smtp.gmail.com` |
| **Port** | 587 |
| **Sécurité** | TLS 1.2 |
| **Authentification** | Email + App Password (variables d'environnement) |
| **Bibliothèque** | `javax.mail` (JavaMail) |

## 🔧 Étapes détaillées

### Étape 1 — Configuration SMTP

```java
// Fichier : EmailService.java

// Variables d'environnement nécessaires :
// SKILORA_MAIL_EMAIL=noreply.skilora@gmail.com
// SKILORA_MAIL_PASSWORD=xxxx xxxx xxxx xxxx  (App Password Google)

Properties props = new Properties();
props.put("mail.smtp.auth", "true");           // Authentification requise
props.put("mail.smtp.starttls.enable", "true"); // Chiffrement TLS
props.put("mail.smtp.host", "smtp.gmail.com");  // Serveur Gmail
props.put("mail.smtp.port", "587");              // Port STARTTLS
props.put("mail.smtp.ssl.protocols", "TLSv1.2");// Version TLS forcée
```

### Étape 2 — Envoi asynchrone (CompletableFuture)

```java
// L'envoi est NON-BLOQUANT grâce à CompletableFuture

public CompletableFuture<Boolean> sendOtpEmail(String toEmail, String otpCode) {
    return CompletableFuture.supplyAsync(() -> {
        String subject = "Votre code de verification Skilora";
        String body = buildOtpEmailBody(otpCode);  // Template HTML
        sendEmail(toEmail, subject, body);
        return true;
    });
}
// Utilisation dans le contrôleur :
// EmailService.getInstance().sendOtpEmail("user@email.com", "123456")
//     .thenAccept(ok -> { if (ok) showToast("Email envoyé !"); });
```

### Étape 3 — Construction du message MIME

```java
// Fichier : EmailService.java — Méthode sendEmail()

Session session = Session.getInstance(props, new Authenticator() {
    protected PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(fromEmail, fromPassword);
    }
});

Message message = new MimeMessage(session);
message.setFrom(new InternetAddress(fromEmail, "Skilora Support"));
message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
message.setSubject(subject);

// Corps HTML
MimeBodyPart mimeBodyPart = new MimeBodyPart();
mimeBodyPart.setContent(body, "text/html; charset=utf-8");
Multipart multipart = new MimeMultipart();
multipart.addBodyPart(mimeBodyPart);
message.setContent(multipart);

Transport.send(message);  // Envoi effectif
```

---

# 5. API 4 — Reconnaissance faciale (Python + dlib)

## 📍 Où trouver le code ?

```
python/face_recognition_service.py              ← Service Python (IA)
src/main/java/com/skilora/user/service/BiometricService.java  ← Bridge Java
```

## 🎯 Rôle

Permettre l'**authentification biométrique** par reconnaissance faciale : enregistrer un visage, vérifier l'identité, détecter les doublons.

## 🧠 Technologies IA utilisées

| Composant | Technologie |
|-----------|-------------|
| **Détection de visage** | HOG (Histogram of Oriented Gradients) via dlib |
| **Encodage facial** | Réseau neuronal profond (128 dimensions) |
| **Comparaison** | Distance euclidienne entre vecteurs |
| **Caméra** | OpenCV (cv2) |

## 📐 Architecture Java ↔ Python

```
┌────────────────────┐    stdin (Base64)     ┌─────────────────────┐
│  BiometricService  │ ──────────────────────► face_recognition    │
│  (Java)            │                        │ _service.py         │
│                    │ ◄──────────────────────│ (Python)            │
│  ProcessBuilder    │    stdout (JSON)       │  dlib + OpenCV      │
└────────────────────┘                        └─────────────────────┘
```

## 🔧 Étapes détaillées

### Étape 1 — Capture d'image (côté Java)

```java
// Fichier : BiometricService.java — Méthode runPythonService()

// 1. Convertir BufferedImage → Base64
ByteArrayOutputStream baos = new ByteArrayOutputStream();
ImageIO.write(image, "jpg", baos);
String base64Image = Base64.getEncoder().encodeToString(baos.toByteArray());

// 2. Lancer le processus Python
ProcessBuilder pb = new ProcessBuilder(
    "python", "python/face_recognition_service.py",
    "verify",    // Commande : register | verify | detect | check_duplicate
    username     // Nom d'utilisateur
);
pb.directory(new File("."));
Process process = pb.start();

// 3. Envoyer l'image Base64 via stdin
OutputStream stdin = process.getOutputStream();
stdin.write(base64Image.getBytes(StandardCharsets.UTF_8));
stdin.close();

// 4. Lire le résultat JSON via stdout
BufferedReader reader = new BufferedReader(
    new InputStreamReader(process.getInputStream()));
String jsonStr = reader.readLine();

// 5. Timeout de 30 secondes
process.waitFor(30, TimeUnit.SECONDS);

JSONObject result = new JSONObject(jsonStr);
// { "success": true, "verified": true, "confidence": 0.92 }
```

### Étape 2 — Détection et encodage (côté Python)

```python
# Fichier : python/face_recognition_service.py

# Constantes de précision
MATCH_TOLERANCE    = 0.45   # Seuil de correspondance (plus strict que 0.6 par défaut)
DUPLICATE_TOLERANCE = 0.42  # Seuil anti-doublon (encore plus strict)
DETECTION_SCALE    = 0.5    # Réduction pour détection rapide
ENCODING_JITTERS   = 3      # Re-échantillonnage pour meilleure qualité

def detect_face_live(self, frame_bytes):
    """
    Pipeline en 2 passes :
      1) Détection rapide sur image réduite (50%) via HOG
      2) Encodage haute qualité sur image originale avec 3 jitters
    """
    # Decoder Base64 → image OpenCV
    nparr = np.frombuffer(frame_bytes, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)

    # Passe 1 : Détection (image réduite pour la vitesse)
    small = cv2.resize(frame, (0, 0), fx=0.5, fy=0.5)
    face_locations = face_recognition.face_locations(small, model="hog")

    # Passe 2 : Encodage (image originale pour la qualité)
    encoding = face_recognition.face_encodings(
        frame, [full_location], num_jitters=3
    )[0]
    # Résultat : vecteur de 128 nombres (empreinte faciale unique)
    return encoding
```

### Étape 3 — Vérification d'identité

```python
# Comparer le visage capturé avec l'encodage stocké
def verify(self, username, frame_bytes):
    encoding = self.detect_face_live(frame_bytes)
    known = self.known_encodings[username]

    # Distance euclidienne entre les deux vecteurs 128-D
    distance = face_recognition.face_distance([known], encoding)[0]

    if distance < MATCH_TOLERANCE:  # < 0.45
        return {"success": True, "verified": True,
                "confidence": round(1.0 - distance, 4)}
    else:
        return {"success": True, "verified": False}
```

### Stockage des données biométriques

```
biometric_data/encodings.json   ← Fichier JSON local
Table MySQL : biometric_data    ← Persistance en BDD
```

---

# 6. API 5 — Crawling d'offres d'emploi (ANETI, Reddit, RSS)

## 📍 Où trouver le code ?

```
python/job_feed_crawler.py                     ← Crawler Python (async)
python/job_feed_config.json                    ← Configuration
src/main/java/com/skilora/recruitment/service/JobService.java  ← Consommateur Java
data/job_feed.json                              ← Fichier de sortie
```

## 🎯 Rôle

Collecter automatiquement des **offres d'emploi** depuis 3 sources différentes et les agréger dans un fichier JSON consommé par l'application.

## 🌐 Sources crawlées

| Source | URL | Méthode |
|--------|-----|---------|
| **ANETI** (Tunisie) | `https://www.emploi.nat.tn/fo/Fr/global.php?page=146` | HTML scraping (BeautifulSoup) |
| **Reddit** | `https://www.reddit.com/r/{sub}/new.json` | JSON API |
| **Remote OK** | `https://remoteok.com/remote-jobs.rss` | Flux RSS (feedparser) |
| **We Work Remotely** | `https://weworkremotely.com/categories/remote-programming-jobs.rss` | Flux RSS |

## 📐 Architecture async

```python
# Fichier : python/job_feed_crawler.py

# Architecture asynchrone avec aiohttp (concurrent I/O)
# 16 connexions simultanées max, 4 par hôte

async def main():
    connector = aiohttp.TCPConnector(limit=16, limit_per_host=4)
    async with aiohttp.ClientSession(connector=connector) as session:
        # Lancer les 3 crawlers en parallèle
        tasks = [
            crawl_aneti(session),     # ANETI (gov.tn)
            crawl_reddit(session),     # Reddit (4 subreddits)
            crawl_rss_feeds(session),  # RSS (2 feeds)
        ]
        results = await asyncio.gather(*tasks)

    # Dédupliquer par URL et sauvegarder
    all_jobs = deduplicate(flatten(results))
    save_to_json(all_jobs, "data/job_feed.json")
```

## 🔧 Configuration

```json
// Fichier : python/job_feed_config.json
{
    "aneti": {
        "enabled": true,
        "listing_url": "https://www.emploi.nat.tn/fo/Fr/global.php?page=146",
        "max_jobs": 0
    },
    "reddit": {
        "enabled": true,
        "subreddits": ["jobs", "RemoteJobs", "forhire", "jobbit"],
        "max_per_sub": 25
    },
    "rss": {
        "enabled": true,
        "feeds": [
            ["Remote OK", "https://remoteok.com/remote-jobs.rss"],
            ["We Work Remotely", "https://weworkremotely.com/..."]
        ],
        "max_per_feed": 30
    },
    "timeout": 8
}
```

## Sortie : data/job_feed.json

```json
[
    {
        "source": "ANETI",
        "title": "Développeur Java Senior",
        "url": "https://www.emploi.nat.tn/...",
        "description": "...",
        "location": "Tunis, Tunisie",
        "posted_date": "2026-02-20"
    },
    {
        "source": "Reddit",
        "title": "[Hiring] Full Stack Developer",
        "url": "https://reddit.com/r/jobs/...",
        "description": "...",
        "location": "Remote",
        "posted_date": "2026-02-22"
    }
]
```

---

# 7. Métier avancé 1 — Algorithme de matching recrutement

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/recruitment/service/MatchingService.java
```

## 🎯 Rôle

Calculer un **score de compatibilité** (0-100%) entre un candidat et une offre d'emploi, basé sur 4 critères pondérés.

## 📐 Formule de scoring : 40-30-20-10

```
Score final = (Skills × 0.40) + (Experience × 0.30)
            + (Language × 0.20) + (Location × 0.10)
```

| Critère | Poids | Ce qui est évalué |
|---------|-------|-------------------|
| **Compétences** | 40% | Correspondance des skills, niveau, expérience, vérification |
| **Expérience** | 30% | Années totales, pertinence du poste, emploi actuel |
| **Langue** | 20% | Scoring spécifique Tunisie (27 villes, pays francophones) |
| **Localisation** | 10% | Correspondance géographique |

## 🔧 Code du scoring

### Compétences (40%)

```java
// Fichier : MatchingService.java — Méthode calculateSkillMatch()

public double calculateSkillMatch(List<Skill> candidateSkills, List<String> requiredSkills) {
    double totalScore = 0;
    for (String required : requiredSkills) {
        for (Skill skill : candidateSkills) {
            if (skill.getName().equalsIgnoreCase(required)) {
                double score = 60;  // Base : 60 points pour une correspondance
                score += skill.getProficiencyLevel() * 6.25;  // 0-25 points bonus
                score += Math.min(skill.getYearsExperience() * 2, 10);  // 0-10 points
                if (skill.isVerified()) score += 5;  // +5 si certifié
                totalScore += Math.min(score, 100);
            }
        }
    }
    return totalScore / requiredSkills.size();  // Moyenne
}
```

### Expérience (30%)

```java
// Fichier : MatchingService.java — Méthode calculateExperienceMatch()

// Base : 30 points
// + Année d'expérience : ≥5 ans → +40 | ≥3 ans → +30 | ≥1 an → +20 | sinon → +10
// + Poste pertinent (même mots-clés) → +20
// + Emploi actuel (non terminé) → +10
```

### Langue (20%) — Spécifique Tunisie

```java
// Fichier : MatchingService.java — Méthode calculateLanguageMatch()

// 27 villes tunisiennes reconnues :
// Tunis, Sfax, Sousse, Kairouan, Bizerte, Gabès, Ariana,
// Gafsa, Monastir, Ben Arous, Kasserine, Médenine,
// Nabeul, Tataouine, Béja, Jendouba, Mahdia, Siliana,
// Le Kef, Tozeur, Manouba, Kébili, Zaghouan, Sidi Bouzid...

// Même localisation → 100
// Deux tunisiens → 90
// Deux pays francophones → 80
// Sinon → 50-60
```

### Cache de performance

```java
// Cache LRU (500 entrées) pour les scores calculés
private final Map<String, MatchingScore> scoreCache = new LinkedHashMap<>(500, 0.75f, true) {
    protected boolean removeEldestEntry(Map.Entry eldest) {
        return size() > 500;
    }
};

// Cache concurrent pour les données de profil (évite les requêtes N+1)
private final ConcurrentHashMap<Integer, ProfileData> profileDataCache = new ConcurrentHashMap<>();
```

---

# 8. Métier avancé 2 — Chatbot & réponses automatiques

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/support/service/ChatbotService.java
src/main/java/com/skilora/support/service/AutoResponseService.java
```

## 🎯 Rôle

Fournir un **assistant automatique** qui répond aux questions courantes des utilisateurs via un système de **mots-clés** en base de données.

## 📐 Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌─────────────────┐
│  Message         │ ──► │  ChatbotService  │ ──► │  Table MySQL     │
│  utilisateur     │     │  (keyword match) │     │  auto_responses  │
└─────────────────┘     └────────┬─────────┘     └─────────────────┘
                                 │
                    ┌────────────┼────────────┐
                    │            │            │
              ┌─────▼────┐ ┌────▼─────┐ ┌───▼──────────┐
              │ Réponse  │ │ Aucune   │ │ Escalade     │
              │ auto     │ │ réponse  │ │ vers ticket  │
              │ trouvée  │ │ → défaut │ │ support      │
              └──────────┘ └──────────┘ └──────────────┘
```

## 🔧 Code principal

```java
// Fichier : ChatbotService.java — Méthode getAutoResponse()

public String getAutoResponse(String userMessage) {
    // Recherche SQL : le mot-clé le plus long qui correspond
    String sql = """
        SELECT id, response_text FROM auto_responses
        WHERE is_active = TRUE
        AND ? LIKE CONCAT('%', trigger_keyword, '%')
        ORDER BY LENGTH(trigger_keyword) DESC
        LIMIT 1
    """;
    // Exemple :
    // Message : "comment réinitialiser mon mot de passe ?"
    // trigger_keyword : "mot de passe" → match !
    // response_text : "Pour réinitialiser votre mot de passe, allez dans..."

    // Incrémenter le compteur d'utilisation
    incrementUsageCount(conn, responseId);
}
```

### Gestion des conversations

```java
// Créer une conversation
int convId = chatbotService.startConversation(userId);

// Ajouter des messages
chatbotService.addMessage(new ChatbotMessage(convId, "USER", "Aide SVP"));
String response = chatbotService.getAutoResponse("Aide SVP");
chatbotService.addMessage(new ChatbotMessage(convId, "BOT", response));

// Escalader vers un ticket support si le bot ne peut pas aider
chatbotService.escalateToTicket(convId, ticketId);
```

---

# 9. Métier avancé 3 — Calcul de paie tunisien (CNSS + IRPP)

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/finance/service/PayslipService.java
src/main/java/com/skilora/finance/service/TaxConfigurationService.java
```

## 🎯 Rôle

Générer des **fiches de paie** conformes à la législation fiscale tunisienne (CNSS + IRPP progressif).

## 📊 Taux et barèmes tunisiens

### CNSS (Caisse Nationale de Sécurité Sociale)

| Cotisation | Taux |
|-----------|------|
| **Salarié** | 9.18% du salaire brut |
| **Employeur** | 16.57% du salaire brut |

### IRPP (Impôt sur le Revenu des Personnes Physiques)

Barème progressif par tranches (annuel) — calculé par `TaxConfigurationService` :

| Tranche annuelle | Taux |
|-----------------|------|
| 0 – 5 000 TND | 0% |
| 5 001 – 20 000 TND | 26% |
| 20 001 – 30 000 TND | 28% |
| 30 001 – 50 000 TND | 32% |
| > 50 000 TND | 35% |

## 🔧 Code du calcul de paie

```java
// Fichier : PayslipService.java — Méthode generatePayslip()

// Constantes
private static final BigDecimal CNSS_EMPLOYEE_RATE = new BigDecimal("0.0918");
private static final BigDecimal CNSS_EMPLOYER_RATE = new BigDecimal("0.1657");

public Payslip generatePayslip(int contractId, int month, int year) {
    // 1. Récupérer le contrat → salaire de base
    EmploymentContract contract = ContractService.getInstance().findById(contractId);
    BigDecimal gross = contract.getSalaryBase();

    // 2. CNSS salarié = brut × 9.18%
    BigDecimal cnssEmployee = gross.multiply(CNSS_EMPLOYEE_RATE);
    //   Ex: 2500 TND × 0.0918 = 229.50 TND

    // 3. CNSS employeur = brut × 16.57%
    BigDecimal cnssEmployer = gross.multiply(CNSS_EMPLOYER_RATE);
    //   Ex: 2500 TND × 0.1657 = 414.25 TND

    // 4. IRPP = (brut annualisé → barème progressif) ÷ 12
    BigDecimal annualGross = gross.multiply(new BigDecimal("12"));
    BigDecimal annualIrpp = TaxConfigurationService.getInstance()
        .calculateIRPP(annualGross, "Tunisia");
    BigDecimal monthlyIrpp = annualIrpp.divide(
        new BigDecimal("12"), 2, RoundingMode.HALF_UP);

    // 5. Net = brut - CNSS salarié - IRPP + primes
    BigDecimal net = gross
        .subtract(cnssEmployee)
        .subtract(monthlyIrpp)
        .add(bonuses);

    // 6. Persister en base et retourner
    Payslip payslip = new Payslip(contractId, userId, month, year, gross);
    create(payslip);
    return payslip;
}
```

### Exemple concret

```
Salaire brut mensuel : 2 500 TND

CNSS salarié (9.18%)     : - 229.50 TND
IRPP mensuel (barème)     : - 108.33 TND
─────────────────────────────────────────
Salaire net              :  2 162.17 TND

CNSS employeur (16.57%)  :   414.25 TND (charge patronale)
```

---

# 10. Métier avancé 4 — Système de mentions @

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/community/service/MentionService.java
```

## 🎯 Rôle

Permettre aux utilisateurs de **mentionner** d'autres personnes dans les posts/commentaires avec `@nom_utilisateur`, avec autocomplétion et notifications.

## 🔧 Code

```java
// Fichier : MentionService.java

// Regex pour extraire les mentions
private static final Pattern MENTION_PATTERN =
    Pattern.compile("@(\\w+(?:_\\w+)*)");

// Extraction des mentions d'un texte
public List<String> extractMentions(String text) {
    List<String> mentions = new ArrayList<>();
    Matcher matcher = MENTION_PATTERN.matcher(text);
    while (matcher.find()) {
        mentions.add(matcher.group(1));  // "aziz_khaldi"
    }
    return mentions;
}

// Autocomplétion (recherche SQL LIKE)
public List<User> searchUsers(String prefix) {
    String sql = "SELECT * FROM users WHERE username LIKE ? LIMIT 10";
    // prefix = "azi" → LIKE 'azi%' → [aziz_khaldi, aziz_ben, ...]
}

// Notification automatique lors d'une mention
public void notifyMentionedUsers(int postId, String content) {
    for (String username : extractMentions(content)) {
        notificationService.create(userId, "Vous avez été mentionné(e)");
    }
}
```

---

# 11. Métier avancé 5 — Notifications temps réel

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/community/service/CommunityNotificationService.java
```

## 🎯 Rôle

Vérifier en **temps réel** les messages non lus et les demandes de connexion en attente, avec mise à jour des badges dans l'interface.

## 🔧 Code

```java
// Fichier : CommunityNotificationService.java

// Polling toutes les 8 secondes via JavaFX Timeline
private final Timeline pollingTimeline = new Timeline(
    new KeyFrame(Duration.seconds(8), e -> checkNotifications())
);

private void checkNotifications() {
    // Thread séparé pour ne pas bloquer l'UI
    new Thread(() -> {
        int unreadMessages = messagingService.getUnreadCount(userId);
        int pendingRequests = connectionService.getPendingCount(userId);

        // Mettre à jour les badges sur le thread JavaFX
        Platform.runLater(() -> {
            messagesBadge.setText(String.valueOf(unreadMessages));
            requestsBadge.setText(String.valueOf(pendingRequests));
            // Callbacks pour l'UI
            if (onUpdate != null) onUpdate.accept(unreadMessages, pendingRequests);
        });
    }).start();
}

// Démarrer/arrêter le polling
public void startPolling() { pollingTimeline.play(); }
public void stopPolling()  { pollingTimeline.stop(); }
```

---

# 12. Métier avancé 6 — Gamification / Achievements

## 📍 Où trouver le code ?

```
src/main/java/com/skilora/formation/service/AchievementService.java
```

## 🎯 Rôle

Système de **badges** et **récompenses** pour encourager l'engagement (nombre de posts, connexions, événements créés, etc.).

## 🔧 Principes

```java
// Fichier : AchievementService.java

// Vérification automatique des achievements
public void checkAndAwardAchievements(int userId) {
    // Compter les activités cross-module
    int postsCount = postService.countByUser(userId);
    int connectionsCount = connectionService.countByUser(userId);
    int eventsCount = eventService.countByUser(userId);
    int blogsCount = blogService.countByUser(userId);

    // Attribuer les badges selon les seuils
    if (postsCount >= 10) award(userId, "FIRST_10_POSTS", Rarity.COMMON);
    if (postsCount >= 100) award(userId, "CENTURION", Rarity.RARE);
    if (connectionsCount >= 50) award(userId, "NETWORKER", Rarity.EPIC);
    // ...
}

// Niveaux de rareté : COMMON, UNCOMMON, RARE, EPIC, LEGENDARY
```

---

# 13. Résumé des endpoints API externes

| # | API | URL | Méthode | Auth | Service Java |
|---|-----|-----|---------|------|--------------|
| 1 | **MyMemory** | `https://api.mymemory.translated.net/get` | GET | Aucune | `TranslationService` |
| 2 | **Cloudinary** | `https://api.cloudinary.com/v1_1/skilora/image/upload` | POST | Preset non signé | `CloudinaryUploadService` |
| 3 | **Gmail SMTP** | `smtp.gmail.com:587` | SMTP/TLS | Email + App Password | `EmailService` |
| 4 | **Python dlib** | Processus local (`ProcessBuilder`) | stdin/stdout | Aucune | `BiometricService` |
| 5 | **ANETI** | `https://www.emploi.nat.tn/fo/Fr/global.php?page=146` | GET (scraping) | Aucune | `job_feed_crawler.py` |
| 6 | **Reddit** | `https://www.reddit.com/r/{sub}/new.json` | GET (JSON) | Aucune | `job_feed_crawler.py` |
| 7 | **Remote OK** | `https://remoteok.com/remote-jobs.rss` | GET (RSS) | Aucune | `job_feed_crawler.py` |
| 8 | **WWR** | `https://weworkremotely.com/.../remote-programming-jobs.rss` | GET (RSS) | Aucune | `job_feed_crawler.py` |

---

# 14. Résumé des dépendances (pom.xml)

| Bibliothèque | Artifact | Version | Rôle |
|-------------|----------|---------|------|
| JavaFX | controls, fxml, media, web | 21 | Interface graphique |
| org.json | json | 20231013 | Parsing JSON (APIs) |
| Gson | gson | 2.10.1 | Sérialisation JSON |
| JavaCV | javacv-platform | 1.5.9 | Accès caméra |
| Webcam Capture | webcam-capture | 0.3.12 | Capture webcam |
| MySQL Connector | mysql-connector-j | 8.3.0 | Driver MySQL |
| HikariCP | HikariCP | 5.1.0 | Pool de connexions |
| JavaMail | javax.mail | 1.6.2 | Envoi d'emails |
| jBCrypt | jbcrypt | 0.4 | Hashage mots de passe |
| SLF4J | slf4j-simple | 1.7.36 | Logging |
| JUnit 5 | junit-jupiter | 5.10.1 | Tests unitaires |

**Python :** `face_recognition` (dlib), `opencv-python`, `numpy`, `aiohttp`, `beautifulsoup4`, `feedparser`, `lxml`

---

# 15. Arborescence des fichiers concernés

```
JAVAFX/
├── python/
│   ├── face_recognition_service.py          ← API 4 : Reconnaissance faciale
│   ├── job_feed_crawler.py                  ← API 5 : Crawling offres d'emploi
│   └── job_feed_config.json                 ← Configuration du crawler
│
├── data/
│   └── job_feed.json                        ← Sortie du crawler
│
├── biometric_data/
│   └── encodings.json                       ← Encodages faciaux stockés
│
├── src/main/java/com/skilora/
│   ├── community/service/
│   │   ├── TranslationService.java          ← API 1 : Traduction (MyMemory)
│   │   ├── CloudinaryUploadService.java     ← API 2 : Upload images (Cloudinary)
│   │   ├── EmailService.java                ← API 3 : Email (Gmail SMTP)
│   │   ├── MentionService.java              ← Métier : Système @mentions
│   │   ├── CommunityNotificationService.java← Métier : Notifications temps réel
│   │   ├── SearchService.java               ← Métier : Recherche multi-entités
│   │   └── DashboardStatsService.java       ← Métier : Analytics
│   │
│   ├── user/service/
│   │   ├── BiometricService.java            ← API 4 : Bridge Java→Python
│   │   └── AuthService.java                 ← Métier : Auth + rate limiting
│   │
│   ├── recruitment/service/
│   │   ├── MatchingService.java             ← Métier : Algorithme 40-30-20-10
│   │   └── JobService.java                  ← Consomme job_feed.json
│   │
│   ├── finance/service/
│   │   ├── PayslipService.java              ← Métier : Calcul paie tunisien
│   │   ├── TaxConfigurationService.java     ← Métier : Barèmes IRPP
│   │   └── ExchangeRateService.java         ← Métier : Conversion devises
│   │
│   ├── formation/service/
│   │   └── AchievementService.java          ← Métier : Gamification
│   │
│   └── support/service/
│       ├── ChatbotService.java              ← Métier : Chatbot auto
│       └── AutoResponseService.java         ← Métier : Réponses automatiques
│
└── pom.xml                                  ← Dépendances Maven
```

---

**Fin du rapport**

> Ce document couvre l'ensemble des APIs externes et métiers avancés du projet Skilora.  
> Chaque section indique précisément **où trouver le code**, **les étapes**, **les rôles** et **des extraits de code commentés**.
