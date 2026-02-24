# 📖 EXPLICATIONS DÉTAILLÉES — Nouvelles Fonctionnalités Sprint 2

> **Module** : Communauté (GestionComNet)  
> **Étudiant** : Mouhamed Aziz Khaldi  
> **Date** : Sprint Java — Février 2026  
> **Fichiers modifiés/créés** : 5 fichiers (4 services + 1 contrôleur)

---

## 📋 RÉSUMÉ DES AJOUTS

| # | Fonctionnalité | Type | Fichier | Lignes |
|---|----------------|------|---------|--------|
| 1 | API Traduction MyMemory | API externe | `TranslationService.java` | ~200 |
| 2 | API Upload Image Cloudinary | API externe | `CloudinaryUploadService.java` | ~250 |
| 3 | Mentions @utilisateur | Feature avancée | `MentionService.java` | ~250 |
| 4 | Recherche Avancée multi-entités | Feature avancée | `SearchService.java` | ~260 |
| 5 | Emoji Picker | Amélioration UI | `CommunityController.java` | ~150 |
| 6 | Animations d'entrée | Amélioration UI | `CommunityController.java` | ~50 |

---

## ╔═══════════════════════════════════════════════════════════════╗
## ║  1. API TRADUCTION — TranslationService.java                  ║
## ╚═══════════════════════════════════════════════════════════════╝

### 📁 Fichier : `src/main/java/com/skilora/community/service/TranslationService.java`

### 🎯 Objectif
Permettre aux utilisateurs de **traduire les posts** de la communauté en un clic.
Utilise l'API gratuite **MyMemory Translated** (api.mymemory.translated.net).

### 🔧 Comment ça marche ?

```
Utilisateur clique "🌐 Traduire"
        │
        ▼
detectLanguage(texte)  ──►  Heuristique : détecte si c'est fr, en, ou ar
        │
        ▼
translate(texte, "fr", "en")  ──►  Appel HTTP GET à MyMemory API
        │
        ▼
Affiche le texte traduit dans la carte du post
```

### 📝 Fonctions à expliquer au professeur

#### `translate(String text, String sourceLang, String targetLang)`
- **Rôle** : Traduit un texte d'une langue source vers une langue cible
- **API utilisée** : MyMemory (GET `https://api.mymemory.translated.net/get?q=...&langpair=fr|en`)
- **Cache LRU** : Stocke les traductions déjà faites pour éviter les appels réseau répétés
- **Comment** : 
  1. Vérifie d'abord le cache (HashMap avec clé `"texte|fr|en"`)
  2. Si pas en cache → construit l'URL avec `URLEncoder.encode()`
  3. Ouvre une connexion HTTP GET avec `HttpURLConnection`
  4. Timeout de 5 secondes (pour ne pas bloquer l'UI)
  5. Parse la réponse JSON avec `org.json.JSONObject`
  6. Extrait `responseData.translatedText`
  7. Met en cache le résultat avant de le retourner

#### `detectLanguage(String text)`
- **Rôle** : Détecte automatiquement la langue d'un texte (fr, en, ou ar)
- **Méthode** : Heuristique (pas d'appel API)
  - **Arabe** : Vérifie les caractères Unicode dans la plage `\u0600-\u06FF` (alphabet arabe)
  - **Français** : Cherche des mots-clés typiquement français (le, la, les, est, dans, avec, pour, etc.)
  - **Par défaut** : Anglais si aucun pattern ne correspond

#### Cache LRU (LinkedHashMap)
- **Pourquoi** : Éviter d'appeler l'API plusieurs fois pour le même texte
- **Comment** : `LinkedHashMap` avec `accessOrder=true` et `removeEldestEntry()` qui supprime l'entrée la plus ancienne quand le cache dépasse 100 éléments
- **Pattern** : C'est un cache **Least Recently Used** (LRU) classique

#### Pattern Singleton
- **Pourquoi** : Il ne faut qu'une seule instance du service dans toute l'application
- **Comment** : `private static volatile` + `synchronized` dans `getInstance()` (double-checked locking)

### 🔗 Intégration dans le contrôleur
- Dans `createPostCard()` : bouton "🌐 Traduire" ajouté à la barre d'actions
- Thread séparé pour l'appel réseau (`new Thread(..., "TranslateThread")`)
- Le label du contenu est mis à jour avec `contentLabel.setText(traduction)`
- Toggle : après traduction, le bouton devient "↩ Original" pour revenir au texte original

---

## ╔═══════════════════════════════════════════════════════════════╗
## ║  2. API UPLOAD IMAGE — CloudinaryUploadService.java           ║
## ╚═══════════════════════════════════════════════════════════════╝

### 📁 Fichier : `src/main/java/com/skilora/community/service/CloudinaryUploadService.java`

### 🎯 Objectif
Permettre aux utilisateurs de **téléverser des images** lors de la création d'un post,
au lieu de taper manuellement une URL. Utilise l'API **Cloudinary** (cloud d'images).

### 🔧 Comment ça marche ?

```
Utilisateur clique "📷 Upload Image"
        │
        ▼
FileChooser.showOpenDialog()  ──►  Sélectionne un fichier .jpg/.png/.gif
        │
        ▼
isAllowedExtension(file)  ──►  Vérifie l'extension et la taille (max 10 MB)
        │
        ▼
uploadImage(file)  ──►  Appel HTTP POST multipart/form-data à Cloudinary
        │
        ▼
Récupère l'URL sécurisée (HTTPS) et remplit le champ imageUrl
```

### 📝 Fonctions à expliquer au professeur

#### `uploadImage(File imageFile)`
- **Rôle** : Téléverse un fichier image vers le cloud Cloudinary et retourne l'URL
- **API** : Cloudinary Upload API (POST `https://api.cloudinary.com/v1_1/skilora/image/upload`)
- **Méthode d'upload** : `unsigned` avec un preset prédéfini (pas besoin de clé API secrète)
- **Comment** :
  1. Vérifie que le fichier existe et n'est pas trop gros (`MAX_FILE_SIZE = 10 MB`)
  2. Vérifie l'extension (`.jpg`, `.png`, `.gif`, `.webp`, `.bmp`)
  3. Construit une requête POST **multipart/form-data** :
     - `boundary` : séparateur unique entre les champs du formulaire
     - Champ `upload_preset` : identifie le preset Cloudinary
     - Champ `file` : le contenu binaire de l'image (lu avec `Files.readAllBytes()`)
  4. Envoie via `HttpURLConnection` avec `Content-Type: multipart/form-data`
  5. Parse la réponse JSON pour extraire le champ `secure_url`

#### Multipart/form-data (le format d'envoi)
- **Pourquoi** : C'est le standard HTTP pour envoyer des fichiers
- **Structure** :
  ```
  --boundary
  Content-Disposition: form-data; name="upload_preset"
  
  skilora_unsigned
  --boundary
  Content-Disposition: form-data; name="file"; filename="photo.jpg"
  Content-Type: image/jpeg
  
  [contenu binaire de l'image]
  --boundary--
  ```

#### Validations
- `isAllowedExtension()` : vérifie que le fichier a une extension image autorisée
- `getContentType()` : détermine le type MIME (`image/jpeg`, `image/png`, etc.)
- Taille max : 10 MB (protection contre les fichiers trop lourds)

### 🔗 Intégration dans le contrôleur
- Dans `showPostDialog()` : bouton "📷 Upload Image" avec `FileChooser`
- Le bouton change de texte pendant l'upload ("⏳ Upload en cours...")
- Après succès : "✅ Image uploadée" et l'URL est remplie automatiquement
- Gestion d'erreur : `DialogUtils.showError()` si l'upload échoue

---

## ╔═══════════════════════════════════════════════════════════════╗
## ║  3. MENTIONS @USER — MentionService.java                     ║
## ╚═══════════════════════════════════════════════════════════════╝

### 📁 Fichier : `src/main/java/com/skilora/community/service/MentionService.java`

### 🎯 Objectif
Permettre aux utilisateurs de **mentionner d'autres utilisateurs** dans leurs posts 
avec la syntaxe `@prenom_nom`, comme sur Twitter/Instagram.

### 🔧 Comment ça marche ?

```
Utilisateur tape "@" dans le champ texte
        │
        ▼
setupMentionDetection()  ──►  Écoute les changements de texte
        │
        ▼
searchUsers("azi", 5)  ──►  SQL LIKE '%azi%' sur la table users
        │
        ▼
Popup d'autocomplétion avec les utilisateurs trouvés
        │  (clic sur un résultat)
        ▼
Remplace "@azi" par "@mouhamed_khaldi " dans le texte
        │  (à la soumission du post)
        ▼
processMentions(texte, authorId, postId)  ──►  Crée des notifications
```

### 📝 Fonctions à expliquer au professeur

#### `searchUsers(String query, int limit)`
- **Rôle** : Cherche des utilisateurs dont le nom contient la requête
- **SQL** : `SELECT id, full_name FROM users WHERE REPLACE(full_name, ' ', '_') LIKE ?`
- **Pourquoi REPLACE** : Les handles utilisent `_` au lieu de espaces (ex: `mouhamed_khaldi`)
- Limité à `limit` résultats pour ne pas surcharger l'interface

#### `extractMentions(String text)`
- **Rôle** : Extrait toutes les mentions @xxx d'un texte
- **Regex** : `@(\w+(?:_\w+)*)` — capture les mots commençant par @ séparés par _
- **Comment** : Utilise `Pattern.compile()` et `Matcher.find()` en boucle
- **Retourne** : Liste de chaînes (ex: `["mouhamed_khaldi", "jean_dupont"]`)

#### `processMentions(String text, int authorId, int postId)`
- **Rôle** : Pour chaque mention trouvée, crée une notification pour l'utilisateur mentionné
- **Étapes** :
  1. Appelle `extractMentions()` pour trouver les handles
  2. Pour chaque handle, appelle `findUserIdByHandle()` pour obtenir l'ID utilisateur
  3. Si l'utilisateur existe et n'est pas l'auteur → `createMentionNotification()`
  4. La notification est insérée dans la table `notifications` avec le type `MENTION`

#### `createMentionNotification(int userId, int authorId, int postId)`
- **SQL** : `INSERT INTO notifications (user_id, type, message, reference_id, is_read, created_date)`
- **Message** : "Vous avez été mentionné dans un post"
- Le `reference_id` pointe vers le post pour permettre la navigation

#### Classe interne `UserMention`
- **Rôle** : Objet de transfert (DTO) contenant `userId`, `fullName`, `handle`
- **Pourquoi** : Encapsuler les données d'un utilisateur trouvé par la recherche

### 🔗 Intégration dans le contrôleur

#### `setupMentionDetection(TLTextarea, TLDialog)`
- Écoute `textProperty()` du TextArea interne
- Quand l'utilisateur tape `@` + 2+ caractères :
  1. Extrait le texte après le dernier `@`
  2. Vérifie qu'il n'y a pas d'espace (c'est un seul mot)
  3. Lance `searchUsers()` dans un thread séparé
  4. Affiche un `Popup` JavaFX avec la liste des résultats
  5. Au clic sur un résultat : remplace le texte dans le champ

#### Dans `createPost()`
- Après succès de la création → appelle `mentionService.processMentions()` dans un thread

---

## ╔═══════════════════════════════════════════════════════════════╗
## ║  4. RECHERCHE AVANCÉE — SearchService.java                    ║
## ╚═══════════════════════════════════════════════════════════════╝

### 📁 Fichier : `src/main/java/com/skilora/community/service/SearchService.java`

### 🎯 Objectif
Permettre aux utilisateurs de **chercher dans tout le contenu** de la communauté 
à partir d'un seul champ de recherche, avec filtres par type.

### 🔧 Comment ça marche ?

```
Utilisateur tape "java" + filtre "Tous" + clic "🔍 Chercher"
        │
        ▼
search("java", ALL, userId)
        │
        ├── searchPosts("%java%")        → Posts contenant "java"
        ├── searchMessages("%java%", 5)  → Messages de l'utilisateur 5
        ├── searchEvents("%java%")       → Événements avec "java"
        ├── searchGroups("%java%")       → Groupes avec "java"
        └── searchBlog("%java%")         → Articles avec "java"
        │
        ▼
Tri par date (récent d'abord) → Affichage en cartes
```

### 📝 Fonctions à expliquer au professeur

#### `search(String keyword, SearchFilter filter, int userId)`
- **Rôle** : Point d'entrée unique de la recherche
- **Paramètres** :
  - `keyword` : le mot-clé à chercher
  - `filter` : le filtre de type (ALL, POSTS, MESSAGES, EVENTS, GROUPS, BLOG)
  - `userId` : ID de l'utilisateur (pour filtrer les messages privés)
- **Comment** : Appelle chaque méthode `searchXxx()` selon le filtre choisi
- **Tri** : Les résultats sont triés par date décroissante (`Comparator`)

#### `searchPosts(String pattern)`
- **SQL** : `SELECT ... FROM posts p JOIN users u ON p.author_id = u.id WHERE p.content LIKE ? OR u.full_name LIKE ?`
- **Pourquoi LIKE** : Recherche partielle — `%java%` trouve "Cours Java", "javascript", etc.
- **LIMIT 20** : Éviter de retourner trop de résultats

#### `searchMessages(String pattern, int userId)`
- **Sécurité** : Filtre par les conversations auxquelles l'utilisateur participe
- **SQL** : `... WHERE m.content LIKE ? AND (c.participant_1 = ? OR c.participant_2 = ?)`
- **Pourquoi cette condition** : Un utilisateur ne doit pas voir les messages des autres

#### `searchEvents(String pattern)` / `searchGroups(String pattern)` / `searchBlog(String pattern)`
- Même logique : `LIKE` sur les champs pertinents (titre, description, contenu, tags, lieu)
- Limité à 20 résultats chacun

#### Classe interne `SearchResult`
- **DTO** avec : `type` (POST/MESSAGE/EVENT/GROUP/BLOG), `id`, `title`, `excerpt`, `author`, `date`
- L'`excerpt` est tronqué à 100 caractères pour l'affichage compact

#### Enum `SearchFilter`
- 6 valeurs : `ALL`, `POSTS`, `MESSAGES`, `EVENTS`, `GROUPS`, `BLOG`
- Chaque valeur a un `label` en français pour l'affichage

### 🔗 Intégration dans le contrôleur

#### `buildSearchBar()`
- Construit un `HBox` avec :
  - `TLTextField` : champ de saisie du mot-clé
  - `TLSelect<String>` : sélecteur de filtre (Tous, Posts, Messages, etc.)
  - `TLButton` : bouton "🔍 Chercher"
- Au clic : lance la recherche dans un thread séparé
- Affiche les résultats sous forme de cartes colorées par type

#### `createSearchResultCard(SearchResult result)`
- Crée une `TLCard` avec badge de type coloré, titre, extrait, auteur, date
- Les badges utilisent différentes variantes selon le type (SUCCESS pour événements, etc.)

---

## ╔═══════════════════════════════════════════════════════════════╗
## ║  5. EMOJI PICKER — Grille d'emojis popup                     ║
## ╚═══════════════════════════════════════════════════════════════╝

### 📁 Fichier : `CommunityController.java` (méthodes ajoutées)

### 🎯 Objectif
Permettre aux utilisateurs d'**ajouter des emojis** dans les posts et messages 
via un panneau graphique au lieu de chercher les codes emoji.

### 🔧 Comment ça marche ?

```
Utilisateur clique "😀 Emoji"
        │
        ▼
showEmojiPicker(bouton, textArea)
        │
        ▼
Popup JavaFX avec GridPane 8×8 (64 emojis)
        │  (clic sur un emoji)
        ▼
textArea.setText(texteActuel + emoji)  →  Emoji ajouté à la fin
```

### 📝 Fonctions à expliquer au professeur

#### `EMOJI_LIST` (tableau statique)
- 64 emojis fréquemment utilisés, rangés par catégorie :
  - Visages (😀-😈), Gestes (👍-🤞), Cœurs (❤️-💔), Symboles (🔥-🚀)

#### `showEmojiPicker(Node anchor, TLTextarea textArea)`
- **Rôle** : Affiche le panneau d'emojis pour un TLTextarea (posts)
- **Composants** :
  - `Popup` JavaFX : fenêtre flottante qui se ferme automatiquement (`autoHide=true`)
  - `GridPane` 8 colonnes : grille pour organiser les emojis
  - Chaque emoji est un `Label` avec événements de souris
- **Positionnement** : `anchor.localToScreen()` pour placer le popup sous le bouton
- **Survol** : L'emoji grossit au survol (20px → 24px) + background coloré
- **Insertion** : `textArea.setText(texteActuel + emoji)` — ajoute à la fin du texte

#### `showEmojiPickerForTextField(Node anchor, TLTextField textField)`
- **Variante** : Même logique mais pour les `TLTextField` (messages de chat)
- **Pourquoi 2 méthodes** : `TLTextarea` et `TLTextField` sont des classes différentes
  avec des méthodes `getText()`/`setText()` différentes

#### Effets visuels au survol
```java
emojiLabel.setOnMouseEntered(e -> emojiLabel.setStyle(
    "-fx-font-size: 24px; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
emojiLabel.setOnMouseExited(e -> emojiLabel.setStyle(
    "-fx-font-size: 20px;"));
```
- L'emoji grossit et a un fond coloré au survol → feedback visuel

#### `GridPane.add(node, col, row)` — Calcul de position
```java
grid.add(emojiLabel, i % 8, i / 8);  // i=0 → col=0,row=0 ; i=9 → col=1,row=1
```
- `i % 8` : colonne (reste de la division par 8)
- `i / 8` : ligne (division entière par 8)

### 🔗 Où c'est utilisé ?
1. Dans `showPostDialog()` : bouton "😀 Emoji" dans la barre d'outils du post
2. Dans `openConversationView()` : bouton "😀" dans la barre de saisie du chat

---

## ╔═══════════════════════════════════════════════════════════════╗
## ║  6. ANIMATIONS — Transitions fluides                          ║
## ╚═══════════════════════════════════════════════════════════════╝

### 📁 Fichier : `CommunityController.java` (méthode ajoutée)

### 🎯 Objectif
Ajouter des **animations d'entrée** sur les cartes du feed pour donner une 
impression de fluidité professionnelle (comme les vrais réseaux sociaux).

### 🔧 Comment ça marche ?

```
Chargement du feed
        │
        ▼
Pour chaque carte post :
   carte.setOpacity(0)           →  Invisible au départ
   carte.setTranslateY(30)       →  30px plus bas
        │
        ▼
   FadeTransition(400ms)         →  Opacité 0 → 1 (apparition)
   TranslateTransition(400ms)    →  Y: 30 → 0 (glissement vers le haut)
   delay += 80ms                 →  Décalage entre chaque carte
```

### 📝 Fonctions à expliquer au professeur

#### `animateCardEntry(Node node, int delay)`
- **Rôle** : Anime l'apparition d'une carte avec un double effet
- **Paramètres** :
  - `node` : le composant à animer (TLCard)
  - `delay` : décalage en ms avant de commencer (pour l'effet cascade)

#### `FadeTransition`
```java
FadeTransition fade = new FadeTransition(Duration.millis(400), node);
fade.setFromValue(0.0);   // Départ : transparent
fade.setToValue(1.0);     // Arrivée : opaque
fade.setDelay(Duration.millis(delay));
```
- **Classe JavaFX** : `javafx.animation.FadeTransition`
- **Propriété animée** : `opacity` (opacité/transparence)
- **Durée** : 400ms (0.4 secondes)

#### `TranslateTransition`
```java
TranslateTransition slide = new TranslateTransition(Duration.millis(400), node);
slide.setFromY(30);   // Départ : 30px plus bas
slide.setToY(0);      // Arrivée : position originale
slide.setDelay(Duration.millis(delay));
```
- **Classe JavaFX** : `javafx.animation.TranslateTransition`
- **Propriété animée** : `translateY` (position verticale)
- **Durée** : 400ms

#### Effet cascade (staggered reveal)
```java
int delay = 0;
for (Post post : posts) {
    TLCard card = createPostCard(post);
    animateCardEntry(card, delay);
    delay += 80;  // +80ms pour chaque carte
}
```
- La 1ère carte apparaît à 0ms, la 2ème à 80ms, la 3ème à 160ms...
- Donne l'impression que les cartes "tombent" une par une

#### Pourquoi `fade.play()` et `slide.play()` séparément ?
- Les deux animations s'exécutent **en parallèle** (pas besoin de `ParallelTransition`)
- JavaFX gère automatiquement les animations concurrentes sur le même noeud

---

## 🔑 QUESTIONS FRÉQUENTES DU PROFESSEUR

### Q: Pourquoi utiliser des threads séparés ?
**R:** JavaFX a un seul thread UI (Application Thread). Si on fait un appel réseau 
ou une requête SQL sur ce thread, l'interface se fige. On utilise `new Thread()` 
ou `Task<>` pour exécuter le travail lourd en arrière-plan, puis `Platform.runLater()` 
pour mettre à jour l'interface de manière sûre.

### Q: Qu'est-ce que le pattern Singleton ?
**R:** Le Singleton assure qu'il n'y a qu'**une seule instance** d'un service dans 
toute l'application. On utilise `private static volatile` + `synchronized` 
(double-checked locking) pour la sécurité thread-safe. L'instance est créée 
la première fois qu'on appelle `getInstance()`.

### Q: Pourquoi un cache LRU dans TranslationService ?
**R:** Si l'utilisateur traduit le même texte plusieurs fois, on évite d'appeler 
l'API réseau. Le cache LRU (Least Recently Used) garde les 100 dernières traductions 
et supprime la plus ancienne quand il est plein. Implémenté avec `LinkedHashMap` 
en mode `accessOrder=true`.

### Q: Comment fonctionne le multipart/form-data ?
**R:** C'est le format HTTP standard pour envoyer des fichiers. Le corps de la requête 
est divisé en sections par un **boundary** (séparateur unique). Chaque section contient 
un champ du formulaire avec son nom, son type MIME et sa valeur.

### Q: Pourquoi PreparedStatement au lieu de String concatenation ?
**R:** Pour se protéger contre les **injections SQL**. Un PreparedStatement utilise 
des paramètres `?` qui sont automatiquement échappés par le driver JDBC. 
Exemple dangereux : `"WHERE name = '" + input + "'"` → si `input = "'; DROP TABLE users; --"` 
l'attaquant supprime la table.

### Q: Que fait Platform.runLater() ?
**R:** `Platform.runLater(Runnable)` planifie l'exécution d'un code sur le 
**thread UI de JavaFX**. C'est obligatoire car les composants visuels (Label, Button, etc.) 
ne peuvent être modifiés que depuis le thread JavaFX. Sans ça → `IllegalStateException`.

### Q: Comment fonctionne la regex des mentions ?
**R:** Le pattern `@(\w+(?:_\w+)*)` signifie :
- `@` : commence par arobase
- `\w+` : un ou plusieurs caractères de mot (a-z, A-Z, 0-9, _)
- `(?:_\w+)*` : suivi optionnellement de `_` + d'autres mots (pour les noms composés)
- Exemple : `@jean_dupont` → capture `jean_dupont`

### Q: Pourquoi Popup au lieu de Dialog pour l'emoji picker ?
**R:** Un `Popup` est léger, sans barre de titre, et se ferme automatiquement 
quand on clique ailleurs (`autoHide=true`). Un `Dialog` est plus lourd et bloque 
l'interaction avec la fenêtre principale. Pour un sélecteur rapide comme les emojis, 
le Popup est plus adapté.

---

## 📊 ARCHITECTURE GLOBALE

```
┌─────────────────────────────────────────────────────────────┐
│                    CommunityController                       │
│  (Contrôleur MVC — gère toutes les interactions UI)          │
│                                                              │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │ 🌐 Traduction│  │ 📷 Upload    │  │ 😀 Emoji     │       │
│  │ (bouton post)│  │ (dialog post)│  │ (popup grille)│      │
│  └──────┬───────┘  └──────┬───────┘  └──────────────┘       │
│         │                  │                                  │
│  ┌──────▼───────┐  ┌──────▼───────┐  ┌──────────────┐       │
│  │ Translation  │  │  Cloudinary  │  │   Mention    │       │
│  │  Service     │  │   Upload     │  │   Service    │       │
│  │              │  │   Service    │  │              │       │
│  │ • translate()│  │ • upload()   │  │ • search()   │       │
│  │ • detect()   │  │ • validate() │  │ • process()  │       │
│  │ • cache LRU  │  │ • multipart  │  │ • extract()  │       │
│  └──────────────┘  └──────────────┘  └──────┬───────┘       │
│                                              │               │
│  ┌──────────────┐  ┌──────────────────────────▼──────┐       │
│  │ 🔍 Search    │  │ 🔔 Notifications (table SQL)    │       │
│  │   Service    │  │    INSERT INTO notifications     │       │
│  │              │  └─────────────────────────────────┘       │
│  │ • search()   │                                            │
│  │ • Posts      │  ┌─────────────────────────────────┐       │
│  │ • Messages   │  │ ✨ Animations (FadeTransition)   │       │
│  │ • Events     │  │    + TranslateTransition          │       │
│  │ • Groups     │  │    Effet cascade sur les cartes   │       │
│  │ • Blog       │  └─────────────────────────────────┘       │
│  └──────────────┘                                            │
└─────────────────────────────────────────────────────────────┘
         │                    │                    │
    ┌────▼────┐         ┌────▼────┐          ┌────▼────┐
    │ MyMemory│         │Cloudinary│         │  MySQL  │
    │  API    │         │  API     │         │   DB    │
    │(gratuit)│         │ (cloud)  │         │(HikariCP)│
    └─────────┘         └──────────┘         └─────────┘
```

---

## 📁 FICHIERS CRÉÉS/MODIFIÉS

| Fichier | Action | Lignes ajoutées |
|---------|--------|-----------------|
| `TranslationService.java` | **CRÉÉ** | ~200 |
| `CloudinaryUploadService.java` | **CRÉÉ** | ~250 |
| `MentionService.java` | **CRÉÉ** | ~250 |
| `SearchService.java` | **CRÉÉ** | ~260 |
| `CommunityController.java` | **MODIFIÉ** | ~400 (nouvelles méthodes + intégrations) |

**Total** : ~1360 lignes de code ajoutées pour 6 fonctionnalités.
