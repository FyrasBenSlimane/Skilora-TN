# EXPLICATION DÉTAILLÉE DU CRUD — MODULE COMMUNAUTÉ
## Posts & Messages — Skilora JavaFX

**Date :** 16 Février 2026  
**Module :** Communauté (community)  
**Auteur :** Équipe Skilora

---

# TABLE DES MATIÈRES

1. [Architecture Générale](#1-architecture-générale)
2. [PARTIE 1 — CRUD des Posts](#2-partie-1--crud-des-posts)
   - 2.1 Structure Base de Données
   - 2.2 Entités Java
   - 2.3 Service (PostService) — Toutes les méthodes
   - 2.4 Contrôleur (CommunityController) — Interface utilisateur
3. [PARTIE 2 — CRUD des Messages](#3-partie-2--crud-des-messages)
   - 3.1 Structure Base de Données
   - 3.2 Entités Java
   - 3.3 Service (MessagingService) — Toutes les méthodes
   - 3.4 Contrôleur (CommunityController) — Interface utilisateur
4. [Design Pattern utilisé : Singleton](#4-design-pattern-utilisé--singleton)
5. [Sécurité & Contrôle d'accès par rôle](#5-sécurité--contrôle-daccès-par-rôle)
6. [Schéma de flux CRUD](#6-schéma-de-flux-crud)

---

# 1. ARCHITECTURE GÉNÉRALE

Le module communauté suit l'architecture **MVC (Modèle-Vue-Contrôleur)** :

```
┌─────────────────────────────────────────────────────┐
│                  ARCHITECTURE MVC                   │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ┌──────────────┐    ┌──────────────┐    ┌────────┐│
│  │   Entity     │    │   Service    │    │ Control││
│  │  (Modèle)    │◄───│  (Logique)   │◄───│  -ler  ││
│  │              │    │              │    │  (Vue)  ││
│  │  Post.java   │    │ PostService  │    │Community││
│  │  Message.java│    │ Messaging    │    │Control- ││
│  │  PostComment │    │   Service    │    │  ler    ││
│  │  Conversation│    │              │    │         ││
│  └──────┬───────┘    └──────┬───────┘    └────────┘│
│         │                   │                       │
│         ▼                   ▼                       │
│  ┌──────────────────────────────────┐               │
│  │     Base de Données MySQL       │               │
│  │  (via DatabaseConfig/HikariCP)  │               │
│  └──────────────────────────────────┘               │
└─────────────────────────────────────────────────────┘
```

**Packages concernés :**
- `com.skilora.community.entity` — Les modèles de données (Post, Message, PostComment, Conversation)
- `com.skilora.community.service` — La logique métier et les requêtes SQL (PostService, MessagingService)
- `com.skilora.community.controller` — L'interface utilisateur JavaFX (CommunityController)
- `com.skilora.config` — La configuration de la base de données (DatabaseConfig avec HikariCP)

**Connexion à la Base de Données :**
Toutes les opérations CRUD utilisent `DatabaseConfig.getInstance().getConnection()` qui retourne une connexion depuis le pool HikariCP. La syntaxe `try-with-resources` garantit la fermeture automatique des connexions.

---

# 2. PARTIE 1 — CRUD DES POSTS

## 2.1 Structure Base de Données

### Table `posts`
```sql
CREATE TABLE posts (
    id            INT(11)      NOT NULL AUTO_INCREMENT,
    author_id     INT(11)      NOT NULL,         -- FK vers users.id
    content       TEXT         NOT NULL,          -- Le contenu textuel du post
    image_url     TEXT         DEFAULT NULL,      -- URL optionnelle d'une image
    post_type     VARCHAR(30)  DEFAULT 'STATUS',  -- Type: STATUS, ARTICLE, etc.
    likes_count   INT(11)      DEFAULT 0,         -- Compteur de likes (dénormalisé)
    comments_count INT(11)     DEFAULT 0,         -- Compteur de commentaires (dénormalisé)
    shares_count  INT(11)      DEFAULT 0,         -- Compteur de partages
    is_published  TINYINT(1)   DEFAULT 1,         -- 1 = publié, 0 = brouillon
    created_date  DATETIME     DEFAULT NOW(),     -- Date de création
    updated_date  DATETIME     DEFAULT NOW()      -- Date de dernière modification
        ON UPDATE CURRENT_TIMESTAMP
);
```

### Table `post_comments`
```sql
CREATE TABLE post_comments (
    id           INT(11)  NOT NULL AUTO_INCREMENT,
    post_id      INT(11)  NOT NULL,    -- FK vers posts.id
    author_id    INT(11)  NOT NULL,    -- FK vers users.id
    content      TEXT     NOT NULL,    -- Le texte du commentaire
    created_date DATETIME DEFAULT NOW()
);
```

### Table `post_likes`
```sql
CREATE TABLE post_likes (
    id           INT(11)  NOT NULL AUTO_INCREMENT,
    post_id      INT(11)  NOT NULL,    -- FK vers posts.id
    user_id      INT(11)  NOT NULL,    -- FK vers users.id
    created_date DATETIME DEFAULT NOW()
);
```

**Relations :**
```
users (1) ──────< (N) posts
posts (1) ──────< (N) post_comments
posts (1) ──────< (N) post_likes
users (1) ──────< (N) post_comments
users (1) ──────< (N) post_likes
```

---

## 2.2 Entités Java

### Classe `Post.java`
**Chemin :** `com.skilora.community.entity.Post`

```java
public class Post {
    // ── Champs persistants (stockés en base) ──
    private int id;                    // Clé primaire auto-incrémentée
    private int authorId;              // ID de l'auteur (FK → users.id)
    private String content;            // Contenu textuel du post
    private String imageUrl;           // URL de l'image attachée (optionnel)
    private PostType postType;         // Enum : STATUS, ARTICLE, etc.
    private int likesCount;            // Nombre de likes
    private int commentsCount;         // Nombre de commentaires
    private int sharesCount;           // Nombre de partages
    private boolean isPublished;       // true = visible, false = brouillon
    private LocalDateTime createdDate; // Date de création
    private LocalDateTime updatedDate; // Date de dernière modification

    // ── Champs transitoires (pour l'affichage UI uniquement) ──
    private String authorName;         // Nom complet de l'auteur (JOIN users)
    private String authorPhoto;        // Photo de l'auteur (JOIN users)
    private boolean isLikedByCurrentUser; // Si l'utilisateur actuel a liké
}
```

**Explication des champs transitoires :**
Les champs `authorName`, `authorPhoto` et `isLikedByCurrentUser` ne sont PAS des colonnes de la table `posts`. Ils sont remplis par des JOIN SQL ou des requêtes supplémentaires lors de la lecture, pour éviter de faire des requêtes séparées dans l'interface.

### Classe `PostComment.java`
**Chemin :** `com.skilora.community.entity.PostComment`

```java
public class PostComment {
    private int id;                    // Clé primaire
    private int postId;                // FK → posts.id
    private int authorId;              // FK → users.id
    private String content;            // Texte du commentaire
    private LocalDateTime createdDate; // Date de création

    // Transitoires
    private String authorName;         // Nom de l'auteur (JOIN)
    private String authorPhoto;        // Photo de l'auteur (JOIN)
}
```

---

## 2.3 Service (PostService) — Toutes les Méthodes CRUD

**Chemin :** `com.skilora.community.service.PostService`
**Design Pattern :** Singleton (voir section 4)

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC C — CREATE POST                                                ║ -->
<!-- ║  Méthode : create(Post post) → int                                   ║ -->
<!-- ║  Rôle : Insérer un nouveau post dans la table 'posts'                ║ -->
<!-- ║  SQL : INSERT INTO posts (...) VALUES (?, ?, ?, ?, ?, NOW())         ║ -->
<!-- ║  Retour : ID du post créé, ou -1 si erreur                          ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### C — CREATE : `create(Post post)` → int

**But :** Insérer un nouveau post dans la base de données.

**Requête SQL :**
```sql
INSERT INTO posts (author_id, content, image_url, post_type, is_published, created_date)
VALUES (?, ?, ?, ?, ?, NOW())
```

**Fonctionnement détaillé :**
1. On ouvre une connexion via `DatabaseConfig.getInstance().getConnection()`
2. On prépare un `PreparedStatement` avec `Statement.RETURN_GENERATED_KEYS` pour récupérer l'ID auto-généré
3. On remplit les paramètres `?` :
   - `?1` → `post.getAuthorId()` (l'ID de l'utilisateur connecté)
   - `?2` → `post.getContent()` (le texte saisi)
   - `?3` → `post.getImageUrl()` (URL image, peut être null)
   - `?4` → `post.getPostType().name()` (convertit l'enum en String : "STATUS")
   - `?5` → `post.isPublished()` (true par défaut)
4. `executeUpdate()` exécute l'INSERT
5. On récupère la clé générée via `getGeneratedKeys()`
6. Si succès, on vérifie les achievements (gamification) via `AchievementService`
7. **Retourne** l'ID du nouveau post, ou -1 en cas d'erreur

**Gestion d'erreur :** Bloc try-with-resources + catch SQLException → log de l'erreur + retour -1

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC R — READ POSTS                                                ║ -->
<!-- ║  4 méthodes de lecture :                                            ║ -->
<!-- ║    1. findById(id)           → Un seul post par ID                  ║ -->
<!-- ║    2. getFeed(userId,p,size) → Fil d'actualité paginé (réseau)      ║ -->
<!-- ║    3. findAll()              → Tous les posts (Admin uniquement)    ║ -->
<!-- ║    4. getByAuthor(authorId)  → Posts d'un auteur spécifique         ║ -->
<!-- ║  Toutes utilisent JOIN users pour récupérer nom + photo auteur      ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### R — READ : 4 méthodes de lecture

#### `findById(int id)` → Post

**But :** Récupérer un seul post par son identifiant.

**Requête SQL :**
```sql
SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
FROM posts p
JOIN users u ON p.author_id = u.id
WHERE p.id = ?
```

**Explication :**
- Le `JOIN users` permet de récupérer le nom et la photo de l'auteur en une seule requête
- `p.*` sélectionne toutes les colonnes de la table posts
- Le résultat est mappé via `mapPost(ResultSet)` qui convertit chaque colonne en attribut Java
- **Retourne** un objet Post complet, ou null si non trouvé

---

#### `getFeed(int userId, int page, int pageSize)` → List<Post>

**But :** Récupérer le fil d'actualité de l'utilisateur avec pagination.

**Requête SQL :**
```sql
SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
FROM posts p
JOIN users u ON p.author_id = u.id
WHERE p.is_published = TRUE
AND (p.author_id = ?                          -- Mes propres posts
  OR p.author_id IN (                          -- OU les posts de mes connexions
    SELECT user_id_2 FROM connections
    WHERE user_id_1 = ? AND status = 'ACCEPTED'
    UNION
    SELECT user_id_1 FROM connections
    WHERE user_id_2 = ? AND status = 'ACCEPTED'
  ))
ORDER BY p.created_date DESC
LIMIT ? OFFSET ?
```

**Explication détaillée :**
1. **Filtrage par publication** : Seulement les posts publiés (`is_published = TRUE`)
2. **Filtrage par réseau** : L'utilisateur voit :
   - Ses propres posts (`p.author_id = ?`)
   - Les posts de ses connexions acceptées (sous-requête UNION sur la table connections)
3. **Sous-requête UNION** : La table connections a deux colonnes (user_id_1, user_id_2), donc on cherche dans les deux directions
4. **Pagination** : `LIMIT ? OFFSET ?` — par exemple page 1 avec 50 résultats = LIMIT 50 OFFSET 0
5. **Tri** : Plus récent en premier (`DESC`)
6. Pour chaque post, on vérifie si l'utilisateur actuel a liké via `isLikedBy()`

---

#### `findAll()` → List<Post>

**But :** Récupérer TOUS les posts (vue Admin uniquement, inclut les non-publiés).

**Requête SQL :**
```sql
SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
FROM posts p
JOIN users u ON p.author_id = u.id
ORDER BY p.created_date DESC
```

**Différence avec `getFeed`** : Pas de filtre `WHERE is_published = TRUE`, pas de filtre par connexions. L'admin voit tout.

---

#### `getByAuthor(int authorId)` → List<Post>

**But :** Récupérer tous les posts d'un auteur spécifique.

**Requête SQL :**
```sql
SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
FROM posts p JOIN users u ON p.author_id = u.id
WHERE p.author_id = ?
ORDER BY p.created_date DESC
```

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC U — UPDATE POST                                               ║ -->
<!-- ║  Méthode : update(Post post) → boolean                              ║ -->
<!-- ║  Rôle : Modifier contenu, image ou type d'un post existant          ║ -->
<!-- ║  SQL : UPDATE posts SET content=?, image_url=?, post_type=? WHERE id=? ║ -->
<!-- ║  Sécurité : Vérifié côté contrôleur via canEditOrDelete()            ║ -->
<!-- ║  Retour : true si modifié, false sinon                              ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### U — UPDATE : `update(Post post)` → boolean

**But :** Modifier le contenu, l'image ou le type d'un post existant.

**Requête SQL :**
```sql
UPDATE posts
SET content = ?, image_url = ?, post_type = ?, updated_date = NOW()
WHERE id = ?
```

**Fonctionnement :**
1. Met à jour les 3 champs modifiables du post
2. `updated_date = NOW()` enregistre automatiquement la date de modification
3. La clause `WHERE id = ?` cible le post exact à modifier
4. `executeUpdate()` retourne le nombre de lignes affectées
5. **Retourne** `true` si au moins 1 ligne a été modifiée (`> 0`), `false` sinon

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC D — DELETE POST                                               ║ -->
<!-- ║  Méthode : delete(int id) → boolean                                 ║ -->
<!-- ║  Rôle : Supprimer un post et ses données associées                  ║ -->
<!-- ║  SQL : DELETE FROM posts WHERE id = ?                               ║ -->
<!-- ║  Cascade : Commentaires et likes supprimés automatiquement (FK)     ║ -->
<!-- ║  Sécurité : Vérifié côté contrôleur via canEditOrDelete()            ║ -->
<!-- ║  Retour : true si supprimé, false sinon                             ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### D — DELETE : `delete(int id)` → boolean

**But :** Supprimer un post de la base de données.

**Requête SQL :**
```sql
DELETE FROM posts WHERE id = ?
```

**Fonctionnement :**
1. Suppression directe du post par son ID
2. Les commentaires et likes associés sont automatiquement supprimés par les contraintes ON DELETE CASCADE de la base
3. **Retourne** `true` si supprimé, `false` sinon

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC LIKES & COMMENTAIRES — Fonctionnalités supplémentaires         ║ -->
<!-- ║  Likes : toggleLike() — Ajouter/Retirer un like (bascule)           ║ -->
<!-- ║  Commentaires CRUD :                                                ║ -->
<!-- ║    C → addComment()       — Ajouter un commentaire                  ║ -->
<!-- ║    R → getComments()      — Lire les commentaires d'un post         ║ -->
<!-- ║    U → updateComment()    — Modifier un commentaire                 ║ -->
<!-- ║    D → deleteComment()    — Supprimer un commentaire                ║ -->
<!-- ║  Compteurs dénormalisés : likes_count, comments_count               ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Fonctionnalités Supplémentaires des Posts

#### `toggleLike(int postId, int userId)` → boolean

**But :** Ajouter ou retirer un "like" sur un post (toggle = bascule).

**Logique :**
```
1. Vérifier si l'utilisateur a déjà liké :
   SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?

2. Si OUI → Retirer le like :
   DELETE FROM post_likes WHERE post_id = ? AND user_id = ?
   UPDATE posts SET likes_count = likes_count - 1 WHERE id = ?

3. Si NON → Ajouter le like :
   INSERT INTO post_likes (post_id, user_id, created_date) VALUES (?, ?, NOW())
   UPDATE posts SET likes_count = likes_count + 1 WHERE id = ?
```

**Explication :** Le compteur `likes_count` est dénormalisé (stocké directement dans la table `posts`) pour éviter un `COUNT(*)` à chaque affichage, ce qui améliore les performances.

---

#### `addComment(PostComment comment)` → int

**But :** Ajouter un commentaire à un post.

**Requête SQL :**
```sql
INSERT INTO post_comments (post_id, author_id, content, created_date)
VALUES (?, ?, ?, NOW())
```

**Après insertion :** Met à jour le compteur dénormalisé :
```sql
UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?
```

---

#### `getComments(int postId)` → List<PostComment>

**But :** Charger tous les commentaires d'un post (avec infos auteur).

**Requête SQL :**
```sql
SELECT c.*, u.full_name as author_name, u.photo_url as author_photo
FROM post_comments c
JOIN users u ON c.author_id = u.id
WHERE c.post_id = ?
ORDER BY c.created_date ASC
```

**Tri :** Du plus ancien au plus récent (ASC) pour un affichage chronologique.

---

#### `updateComment(int commentId, String newContent)` → boolean

**Requête SQL :**
```sql
UPDATE post_comments SET content = ? WHERE id = ?
```

---

#### `deleteComment(int commentId, int postId)` → boolean

**Requête SQL :**
```sql
DELETE FROM post_comments WHERE id = ?
```

**Après suppression :** Décrémente le compteur (avec protection contre les valeurs négatives) :
```sql
UPDATE posts SET comments_count = GREATEST(comments_count - 1, 0) WHERE id = ?
```

---

### Méthode Utilitaire : `mapPost(ResultSet rs)` → Post

**But :** Convertir une ligne de résultat SQL en objet Java Post.

**Fonctionnement :**
```java
Post post = new Post();
post.setId(rs.getInt("id"));                           // Colonne → attribut
post.setAuthorId(rs.getInt("author_id"));
post.setContent(rs.getString("content"));
post.setImageUrl(rs.getString("image_url"));
post.setPostType(PostType.valueOf(rs.getString("post_type")));  // String → Enum
post.setLikesCount(rs.getInt("likes_count"));
post.setCommentsCount(rs.getInt("comments_count"));
post.setSharesCount(rs.getInt("shares_count"));
post.setPublished(rs.getBoolean("is_published"));

Timestamp created = rs.getTimestamp("created_date");
if (created != null) post.setCreatedDate(created.toLocalDateTime());  // SQL → Java

post.setAuthorName(rs.getString("author_name"));       // Vient du JOIN users
post.setAuthorPhoto(rs.getString("author_photo"));     // Vient du JOIN users
return post;
```

---

## 2.4 Contrôleur — Interface Utilisateur des Posts

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — CREATE POST (Interface UI)                       ║ -->
<!-- ║  Flux : Clic "Nouveau Post" → Dialog → Validation → Thread →        ║ -->
<!-- ║         PostService.create() → Toast + Rafraîchissement             ║ -->
<!-- ║  Contrôle de saisie : Contenu non vide obligatoire                  ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux CREATE (Créer un Post)

```
Utilisateur clique "Nouveau Post"
        │
        ▼
handleNewPost() → showPostDialog(null)
        │
        ▼
┌───────────────────────────────────┐
│      TLDialog s'ouvre avec :      │
│  • TLTextarea (contenu)           │
│  • TLTextField (URL image)        │
│  • Boutons : Annuler / Publier    │
└───────────────┬───────────────────┘
                │ Clic "Publier"
                ▼
        Validation : texte non vide ?
        │ OUI                  │ NON
        ▼                      ▼
createPost(text, imageUrl)   Message d'erreur
        │
        ▼
Thread séparé → PostService.create(post)
        │
        ▼ succès
showToast("Post créé")
loadFeedTab() → Rafraîchit la liste
```

**Code du contrôleur :**
- `showPostDialog(null)` : Ouvre un dialogue vierge (null = création, pas modification)
- `createPost(text, imageUrl)` : Crée un objet Post, appelle le service dans un Thread séparé
- Thread séparé : Les opérations base de données sont faites hors du thread JavaFX pour ne pas bloquer l'interface
- `Platform.runLater()` : Retour sur le thread JavaFX pour mettre à jour l'interface

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — READ POSTS (Interface UI)                        ║ -->
<!-- ║  Flux : Onglet "Fil" → Thread → PostService.getFeed/findAll →       ║ -->
<!-- ║         createPostCard() pour chaque post → Affichage               ║ -->
<!-- ║  Admin : voit TOUS les posts (findAll)                              ║ -->
<!-- ║  Autres : voit son feed filtré par connexions (getFeed)             ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux READ (Afficher le Fil d'Actualité)

```
Onglet "Fil" sélectionné
        │
        ▼
loadFeedTab()
        │
        ▼
Thread séparé :
  └── Admin ? → PostService.findAll()
  └── Autre ? → PostService.getFeed(userId, 1, 50)
        │
        ▼ succès (sur thread JavaFX)
Pour chaque Post :
  └── createPostCard(post) → Crée une carte visuelle
        │
        ▼
┌─────────────────────────────────────────┐
│  ┌─────────────────────────────────┐    │
│  │ 🟢 Avatar  │ Nom Auteur        │    │
│  │            │ 12 fév 2026       │    │
│  ├─────────────────────────────────┤    │
│  │ Contenu du post...             │    │
│  │                                │    │
│  ├─────────────────────────────────┤    │
│  │ ♥ Like (5)  💬 Commenter (3)  │    │
│  │                 ✏ Éditer  🗑   │    │
│  └─────────────────────────────────┘    │
│                                         │
│  Commentaires (inline, style Instagram) │
│  ┌─────────────────────────────────┐    │
│  │ 🟣 User │ Super post !         │    │
│  │         │ il y a 2h  Édit Supp │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — UPDATE POST (Interface UI)                       ║ -->
<!-- ║  Flux : Clic "Éditer" → Dialog pré-rempli → Modification →          ║ -->
<!-- ║         Thread → PostService.update() → Toast + Rafraîchissement    ║ -->
<!-- ║  Condition : canEditOrDelete() = Auteur OU Admin                    ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux UPDATE (Modifier un Post)

```
Utilisateur clique "Éditer" sur son post
        │
        ▼
showPostDialog(existingPost)    ← existingPost != null
        │
        ▼
┌───────────────────────────────────┐
│  Dialog pré-rempli avec :         │
│  • Contenu actuel dans textarea   │
│  • URL image actuelle             │
│  • Boutons : Annuler / Enregistrer│
└───────────────┬───────────────────┘
                │ Clic "Enregistrer"
                ▼
existingPost.setContent(nouveauTexte)
existingPost.setImageUrl(nouvelleUrl)
updatePost(existingPost)
        │
        ▼
Thread séparé → PostService.update(post)
        │
        ▼ succès
showToast("Post modifié")
loadFeedTab() → Rafraîchit
```

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — DELETE POST (Interface UI)                       ║ -->
<!-- ║  Flux : Clic "Supprimer" → Confirmation → Thread →                  ║ -->
<!-- ║         PostService.delete() → Rafraîchissement                     ║ -->
<!-- ║  Condition : canEditOrDelete() = Auteur OU Admin                    ║ -->
<!-- ║  Dialogue de confirmation obligatoire avant suppression             ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux DELETE (Supprimer un Post)

```
Utilisateur clique "Supprimer" sur son post
        │
        ▼
deletePost(post)
        │
        ▼
┌─────────────────────────────────┐
│  Dialogue de confirmation :     │
│  "Voulez-vous supprimer ?"      │
│  [Annuler]  [OK]                │
└───────────────┬─────────────────┘
                │ Clic "OK"
                ▼
Thread séparé → PostService.delete(post.getId())
        │
        ▼ succès
loadFeedTab() → Rafraîchit
```

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — CRUD COMMENTAIRES INLINE (Style Instagram)       ║ -->
<!-- ║  C → Ajout : champ texte + bouton Envoyer → addComment()            ║ -->
<!-- ║  R → Lecture : loadInlineComments() affiche tous les commentaires    ║ -->
<!-- ║  U → Modification : showEditCommentInline() → updateComment()       ║ -->
<!-- ║  D → Suppression : deleteComment() directe sans confirmation        ║ -->
<!-- ║  Sécurité : Boutons ✏🗑 visibles uniquement pour auteur/admin       ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Commentaires — CRUD Inline (Style Instagram)

**Ajout de commentaire :**
```
Clic "💬 Commenter" → commentsSection s'ouvre
        │
        ▼
loadInlineComments(post, commentsSection)
        │
        ▼
Affiche : [______champ de texte______] [Envoyer]
        + liste des commentaires existants

Utilisateur tape + clic Envoyer :
  → PostService.addComment(comment) dans un Thread
  → Rafraîchit la section commentaires
```

**Modification de commentaire :**
```
Clic "Éditer" sur un commentaire (visible seulement au propriétaire/admin)
        │
        ▼
showEditCommentInline() → Remplace le commentaire par un champ éditable
        │
        ▼
┌──────────────────────────────────────────┐
│ [____texte modifiable____] [Save] [Annul]│
└─────────────────┬────────────────────────┘
                  │ Clic "Save"
                  ▼
PostService.updateComment(commentId, nouveauTexte)
  → Rafraîchit la section commentaires
```

**Suppression de commentaire :**
```
Clic "Supprimer" sur un commentaire
  → PostService.deleteComment(commentId, postId)
  → Rafraîchit la section commentaires
```

---

# 3. PARTIE 2 — CRUD DES MESSAGES

## 3.1 Structure Base de Données

### Table `conversations`
```sql
CREATE TABLE conversations (
    id              INT(11)    NOT NULL AUTO_INCREMENT,
    participant_1   INT(11)    NOT NULL,          -- FK → users.id (ID le plus petit)
    participant_2   INT(11)    NOT NULL,          -- FK → users.id (ID le plus grand)
    last_message_date DATETIME DEFAULT NULL,      -- Date du dernier message
    is_archived_1   TINYINT(1) DEFAULT 0,         -- Archivée par participant_1 ?
    is_archived_2   TINYINT(1) DEFAULT 0,         -- Archivée par participant_2 ?
    created_date    DATETIME   DEFAULT NOW()
);
```

**Note importante :** `participant_1` est TOUJOURS l'ID le plus petit (`Math.min`), et `participant_2` le plus grand (`Math.max`). Cela garantit l'unicité : la conversation entre user 3 et user 7 est toujours stockée comme (3, 7), jamais (7, 3).

### Table `messages`
```sql
CREATE TABLE messages (
    id              INT(11)    NOT NULL AUTO_INCREMENT,
    conversation_id INT(11)    NOT NULL,          -- FK → conversations.id
    sender_id       INT(11)    NOT NULL,          -- FK → users.id
    content         TEXT       NOT NULL,          -- Le texte du message
    is_read         TINYINT(1) DEFAULT 0,         -- 0 = non lu, 1 = lu
    created_date    DATETIME   DEFAULT NOW()
);
```

**Relations :**
```
users (1) ──────< (N) conversations (via participant_1 ou participant_2)
conversations (1) ──────< (N) messages
users (1) ──────< (N) messages (via sender_id)
```

---

## 3.2 Entités Java

### Classe `Conversation.java`

```java
public class Conversation {
    // ── Champs persistants ──
    private int id;                        // Clé primaire
    private int participant1;              // ID du participant 1 (le plus petit)
    private int participant2;              // ID du participant 2 (le plus grand)
    private LocalDateTime lastMessageDate; // Date du dernier message envoyé
    private boolean isArchived1;           // Archivée par participant 1
    private boolean isArchived2;           // Archivée par participant 2
    private LocalDateTime createdDate;     // Date de création

    // ── Champs transitoires (pour l'UI) ──
    private String otherUserName;          // Nom de l'autre participant (JOIN)
    private String otherUserPhoto;         // Photo de l'autre participant (JOIN)
    private String lastMessagePreview;     // Aperçu du dernier message (sous-requête)
    private int unreadCount;               // Nombre de messages non lus (sous-requête)
}
```

### Classe `Message.java`

```java
public class Message {
    // ── Champs persistants ──
    private int id;                    // Clé primaire
    private int conversationId;        // FK → conversations.id
    private int senderId;              // FK → users.id
    private String content;            // Texte du message
    private boolean isRead;            // Message lu ou non
    private LocalDateTime createdDate; // Date d'envoi

    // ── Champ transitoire ──
    private String senderName;         // Nom de l'expéditeur (JOIN)
}
```

---

## 3.3 Service (MessagingService) — Toutes les Méthodes CRUD

**Chemin :** `com.skilora.community.service.MessagingService`
**Design Pattern :** Singleton

### Gestion des Conversations

#### `getOrCreateConversation(int userId1, int userId2)` → int

**But :** Trouver une conversation existante entre deux utilisateurs, ou en créer une nouvelle.

**Logique en 2 étapes :**

**Étape 1 — Chercher si une conversation existe :**
```sql
SELECT id FROM conversations
WHERE participant_1 = ? AND participant_2 = ?
```
- `participant_1 = Math.min(userId1, userId2)` — Toujours l'ID le plus petit
- `participant_2 = Math.max(userId1, userId2)` — Toujours l'ID le plus grand
- Si trouvé → retourne l'ID de la conversation existante

**Étape 2 — Créer si n'existe pas :**
```sql
INSERT INTO conversations (participant_1, participant_2, created_date)
VALUES (?, ?, NOW())
```
- **Retourne** l'ID de la nouvelle conversation

**Pourquoi Math.min/Math.max ?** Pour garantir l'unicité. Sans cette normalisation, on pourrait avoir deux conversations pour le même couple d'utilisateurs.

---

#### `getConversations(int userId)` → List<Conversation>

**But :** Récupérer toutes les conversations d'un utilisateur avec aperçu et compteur de non-lus.

**Requête SQL (complexe) :**
```sql
SELECT c.*,
    CASE
        WHEN c.participant_1 = ? THEN u2.full_name
        ELSE u1.full_name
    END as other_name,
    CASE
        WHEN c.participant_1 = ? THEN u2.photo_url
        ELSE u1.photo_url
    END as other_photo,
    (SELECT content FROM messages
     WHERE conversation_id = c.id
     ORDER BY created_date DESC LIMIT 1
    ) as last_msg,
    (SELECT COUNT(*) FROM messages
     WHERE conversation_id = c.id
     AND sender_id != ?
     AND is_read = FALSE
    ) as unread
FROM conversations c
JOIN users u1 ON c.participant_1 = u1.id
JOIN users u2 ON c.participant_2 = u2.id
WHERE (c.participant_1 = ? OR c.participant_2 = ?)
AND ((c.participant_1 = ? AND c.is_archived_1 = FALSE)
  OR (c.participant_2 = ? AND c.is_archived_2 = FALSE))
ORDER BY
    CASE WHEN c.last_message_date IS NULL THEN 1 ELSE 0 END,
    c.last_message_date DESC
```

**Explication détaillée de chaque partie :**

1. **CASE WHEN `other_name` / `other_photo`** :
   - Si l'utilisateur est participant_1, l'autre est participant_2 → on prend le nom de u2
   - Si l'utilisateur est participant_2, l'autre est participant_1 → on prend le nom de u1
   - Cela permet de toujours afficher le nom de l'AUTRE personne

2. **Sous-requête `last_msg`** :
   - Récupère le contenu du dernier message de la conversation
   - `ORDER BY created_date DESC LIMIT 1` → le plus récent

3. **Sous-requête `unread`** :
   - Compte les messages non lus (`is_read = FALSE`)
   - Et qui ne sont PAS envoyés par l'utilisateur actuel (`sender_id != ?`)
   - On ne compte pas ses propres messages comme "non lus"

4. **WHERE avec archivage** :
   - Exclut les conversations archivées par l'utilisateur
   - Chaque participant a son propre flag d'archivage

5. **ORDER BY** :
   - `CASE WHEN ... IS NULL THEN 1 ELSE 0 END` → Les conversations avec messages passent en premier
   - `c.last_message_date DESC` → Tri par dernier message récent
   - Compatible MariaDB (pas de NULLS LAST)

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC C — CREATE MESSAGE                                            ║ -->
<!-- ║  Méthode : sendMessage(conversationId, senderId, content) → int      ║ -->
<!-- ║  Rôle : Insérer un message + mettre à jour last_message_date        ║ -->
<!-- ║  SQL : INSERT INTO messages (...) + UPDATE conversations            ║ -->
<!-- ║  Retour : ID du message créé, ou -1 si erreur                       ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### C — CREATE : `sendMessage(int conversationId, int senderId, String content)` → int

**But :** Envoyer un nouveau message dans une conversation.

**Requête SQL (2 opérations) :**
```sql
-- 1. Insérer le message
INSERT INTO messages (conversation_id, sender_id, content, created_date)
VALUES (?, ?, ?, NOW())

-- 2. Mettre à jour la date du dernier message de la conversation
UPDATE conversations SET last_message_date = NOW() WHERE id = ?
```

**Fonctionnement :**
1. INSERT le message avec l'ID de conversation, l'expéditeur et le contenu
2. Récupère la clé générée (l'ID du message)
3. Met à jour `last_message_date` de la conversation pour le tri
4. **Retourne** l'ID du message créé, ou -1 en cas d'erreur

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC R — READ MESSAGES                                             ║ -->
<!-- ║  Méthode : getMessages(conversationId, page, pageSize) → List       ║ -->
<!-- ║  Rôle : Charger les messages avec pagination + nom expéditeur       ║ -->
<!-- ║  SQL : SELECT m.*, u.full_name FROM messages m JOIN users u ...      ║ -->
<!-- ║  Tri : Chronologique ASC (ancien → récent)                          ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### R — READ : `getMessages(int conversationId, int page, int pageSize)` → List<Message>

**But :** Charger les messages d'une conversation avec pagination.

**Requête SQL :**
```sql
SELECT m.*, u.full_name as sender_name
FROM messages m
JOIN users u ON m.sender_id = u.id
WHERE m.conversation_id = ?
ORDER BY m.created_date ASC
LIMIT ? OFFSET ?
```

**Explication :**
- `JOIN users` → récupère le nom de l'expéditeur
- `ORDER BY ASC` → ordre chronologique (ancien → récent)
- `LIMIT ? OFFSET ?` → pagination (par défaut : page 1, 100 messages)
- Chaque message est mappé via `mapMessage(ResultSet)`

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC U — UPDATE MESSAGE                                            ║ -->
<!-- ║  Méthode : updateMessage(messageId, senderId, newContent) → boolean  ║ -->
<!-- ║  Rôle : Modifier le contenu d'un message existant                   ║ -->
<!-- ║  SQL : UPDATE messages SET content=? WHERE id=? AND sender_id=?     ║ -->
<!-- ║  SÉCURITÉ : WHERE sender_id=? empêche la modification par autrui    ║ -->
<!-- ║  Retour : true si modifié, false si non autorisé                    ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### U — UPDATE : `updateMessage(int messageId, int senderId, String newContent)` → boolean

**But :** Modifier le contenu d'un message existant.

**Requête SQL :**
```sql
UPDATE messages SET content = ?
WHERE id = ? AND sender_id = ?
```

**Sécurité :** La clause `AND sender_id = ?` garantit que seul l'expéditeur peut modifier son propre message. Si un autre utilisateur essaie de modifier, la condition WHERE ne matchera aucune ligne et `executeUpdate()` retournera 0.

**Retourne :** `true` si modifié, `false` si non autorisé ou erreur.

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC D — DELETE MESSAGE                                            ║ -->
<!-- ║  Méthode : deleteMessage(messageId, senderId) → boolean             ║ -->
<!-- ║  Rôle : Supprimer un message de la conversation                     ║ -->
<!-- ║  SQL : DELETE FROM messages WHERE id=? AND sender_id=?              ║ -->
<!-- ║  SÉCURITÉ : WHERE sender_id=? empêche la suppression par autrui     ║ -->
<!-- ║  Retour : true si supprimé, false sinon                             ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### D — DELETE : `deleteMessage(int messageId, int senderId)` → boolean

**But :** Supprimer un message.

**Requête SQL :**
```sql
DELETE FROM messages WHERE id = ? AND sender_id = ?
```

**Sécurité :** Même principe — `AND sender_id = ?` empêche la suppression d'un message par quelqu'un d'autre que l'expéditeur.

**Retourne :** `true` si supprimé, `false` sinon.

---

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC AUTRES — Méthodes utilitaires des Messages                    ║ -->
<!-- ║  markAsRead()    → Marquer messages reçus comme lus                 ║ -->
<!-- ║  getUnreadCount() → Compter les messages non lus (badges)           ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Autres Méthodes

#### `markAsRead(int conversationId, int userId)` → boolean

**But :** Marquer tous les messages reçus comme lus quand l'utilisateur ouvre une conversation.

```sql
UPDATE messages SET is_read = TRUE
WHERE conversation_id = ? AND sender_id != ?
```

**Logique :** Marque comme lus (`is_read = TRUE`) tous les messages de la conversation qui n'ont PAS été envoyés par l'utilisateur (`sender_id != ?`). On ne marque que les messages REÇUS.

---

#### `getUnreadCount(int userId)` → int

**But :** Compter le total de messages non lus pour l'utilisateur (pour les badges de notification).

```sql
SELECT COUNT(*) FROM messages m
JOIN conversations c ON m.conversation_id = c.id
WHERE (c.participant_1 = ? OR c.participant_2 = ?)
AND m.sender_id != ?
AND m.is_read = FALSE
```

---

## 3.4 Contrôleur — Interface Utilisateur des Messages

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — READ CONVERSATIONS (Interface UI)                ║ -->
<!-- ║  Flux : Onglet "Messages" → Thread → getConversations() →           ║ -->
<!-- ║         createConversationCard() → Affichage des conversations      ║ -->
<!-- ║  Affiche : Avatar, nom, aperçu dernier msg, badge non lus           ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux : Liste des Conversations

```
Onglet "Messages" sélectionné
        │
        ▼
loadMessagesTab()
        │
        ▼
Thread séparé → MessagingService.getConversations(userId)
        │
        ▼ succès
Pour chaque Conversation :
  └── createConversationCard(conv)
        │
        ▼
┌─────────────────────────────────────┐
│  🟢 Avatar  │ Nom de l'autre       │
│             │ Dernier message...   │
│             │ 🔵 3 non lus         │
└─────────────────────────────────────┘
  clic → openConversationView(conv)
```

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — VUE CHAT (Interface UI)                          ║ -->
<!-- ║  Flux : openConversationView() → markAsRead() → getMessages() →    ║ -->
<!-- ║         Bulles droite (moi) / gauche (autre) + boutons ✏🗑 au survol ║ -->
<!-- ║  Input bar en bas : champ texte + bouton envoi                       ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux : Vue de Conversation (Chat)

```
openConversationView(conv) est appelé
        │
        ▼
Efface le contenu, crée le layout :
┌─────────────────────────────────────┐
│ ← Retour │ 🟢 Avatar │ Nom │ Online│  ← Header
├─────────────────────────────────────┤
│                                     │  ← Zone de messages
│  ┌──────────────────┐               │     (ScrollPane)
│  │ Message reçu     │ 🟣            │
│  │ 10:30            │               │
│  └──────────────────┘               │
│               ┌──────────────────┐  │
│            🟢 │ Mon message      │  │
│               │ 10:32    ✏ 🗑   │  │  ← Actions au survol
│               └──────────────────┘  │
│                                     │
├─────────────────────────────────────┤
│ [______Tapez votre message____] [➤] │  ← Input bar
└─────────────────────────────────────┘
```

**Détails de l'affichage des messages :**
1. `markAsRead(conv.getId(), currentUser.getId())` — Marque les messages reçus comme lus
2. `getMessages(conv.getId(), 1, 100)` — Charge les 100 derniers messages
3. Pour CHAQUE message :
   - Vérifie si c'est le mien : `msg.getSenderId() == currentUser.getId()`
   - **Si c'est le mien** : Bulle à droite (style `.msg-bubble-mine`, fond bleu)
   - **Si c'est l'autre** : Bulle à gauche avec avatar (style `.msg-bubble-theirs`, fond gris)

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — CREATE MESSAGE (Interface UI)                    ║ -->
<!-- ║  Flux : Texte + Clic ➤ → Validation non vide → Thread →             ║ -->
<!-- ║         sendMessage() → Rafraîchit le chat + notifications          ║ -->
<!-- ║  Contrôle de saisie : Message non vide obligatoire                  ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux CREATE (Envoyer un Message)

```
Utilisateur tape du texte et clique ➤
        │
        ▼
Validation : texte non vide ?
        │ OUI
        ▼
msgInput.setText("") → Vide le champ immédiatement
        │
        ▼
Thread séparé :
  └── MessagingService.sendMessage(conv.getId(), userId, texte)
        │
        ▼ succès
Platform.runLater :
  └── openConversationView(conv) → Rafraîchit le chat
  └── notificationService.pollNow() → Met à jour les badges
```

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — UPDATE MESSAGE (Interface UI)                    ║ -->
<!-- ║  Flux : Survol → ✏ → Dialog pré-rempli → Thread →                   ║ -->
<!-- ║         updateMessage() → Rafraîchit le chat                        ║ -->
<!-- ║  Double sécurité : UI (if isMine) + SQL (WHERE sender_id=?)        ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux UPDATE (Modifier un Message)

```
Survol d'un de mes messages → Boutons ✏ et 🗑 apparaissent
        │
        ▼ Clic ✏
TLDialog s'ouvre avec :
  • TLTextarea pré-remplie avec le texte actuel
  • Boutons : Annuler / Enregistrer
        │
        ▼ Clic "Enregistrer"
Thread séparé :
  └── MessagingService.updateMessage(msg.getId(), userId, nouveauTexte)
        │
        ▼ succès
openConversationView(conv) → Rafraîchit le chat
```

**Sécurité UI :** Les boutons ✏ 🗑 n'apparaissent que sur les messages de l'utilisateur (`if (isMine)`).  
**Sécurité SQL :** Même si quelqu'un modifie le code client, la requête SQL vérifie `WHERE sender_id = ?`.

<!-- ╔══════════════════════════════════════════════════════════════════════╗ -->
<!-- ║  BLOC CONTRÔLEUR — DELETE MESSAGE (Interface UI)                    ║ -->
<!-- ║  Flux : Survol → 🗑 → Confirmation → Thread →                      ║ -->
<!-- ║         deleteMessage() → Rafraîchit le chat                        ║ -->
<!-- ║  Double sécurité : UI (if isMine) + SQL (WHERE sender_id=?)        ║ -->
<!-- ║  Bouton DANGER rouge pour confirmer la suppression                  ║ -->
<!-- ╚══════════════════════════════════════════════════════════════════════╝ -->

### Flux DELETE (Supprimer un Message)

```
Survol d'un de mes messages → Clic 🗑
        │
        ▼
TLDialog de confirmation :
  "Voulez-vous vraiment supprimer ce message ?"
  [Annuler]  [Supprimer]  (bouton rouge DANGER)
        │
        ▼ Clic "Supprimer"
Thread séparé :
  └── MessagingService.deleteMessage(msg.getId(), userId)
        │
        ▼ succès
openConversationView(conv) → Rafraîchit le chat
```

---

# 4. DESIGN PATTERN UTILISÉ : SINGLETON

Les deux services utilisent le pattern **Singleton Thread-Safe avec Double-Check Locking** :

```java
public class PostService {
    // volatile : garantit la visibilité entre threads
    private static volatile PostService instance;

    // Constructeur privé : empêche l'instanciation externe
    private PostService() {}

    // Méthode d'accès unique
    public static PostService getInstance() {
        if (instance == null) {                    // 1er check (sans verrou)
            synchronized (PostService.class) {     // Verrou sur la classe
                if (instance == null) {            // 2ème check (avec verrou)
                    instance = new PostService();  // Création unique
                }
            }
        }
        return instance;                           // Retourne l'instance unique
    }
}
```

**Pourquoi le Singleton ?**
1. **Une seule instance** du service dans toute l'application
2. **Économie de ressources** : pas de création d'objets inutiles
3. **Point d'accès global** : `PostService.getInstance().create(post)`
4. **Thread-safe** : le mot-clé `volatile` + `synchronized` garantissent qu'un seul thread crée l'instance

**Pourquoi Double-Check Locking ?**
- Le 1er `if` évite de prendre le verrou à chaque appel (performance)
- Le `synchronized` protège la création de l'instance
- Le 2ème `if` vérifie après le verrou (un autre thread a pu créer l'instance pendant l'attente)

---

# 5. SÉCURITÉ & CONTRÔLE D'ACCÈS PAR RÔLE

### Rôles Disponibles
| Rôle | Description |
|------|-------------|
| `ADMIN` | Administrateur — Voit tout, peut tout supprimer |
| `USER` | Chercheur d'emploi — CRUD sur ses propres contenus |
| `EMPLOYER` | Employeur — CRUD sur ses propres contenus |
| `TRAINER` | Formateur — CRUD sur ses propres contenus |

### Méthode `canEditOrDelete(int authorId)`
```java
private boolean canEditOrDelete(int authorId) {
    if (currentUser == null) return false;
    return isAdmin() || currentUser.getId() == authorId;
    //      ↑ Admin peut tout    ↑ OU c'est mon propre contenu
}
```

### Application par fonctionnalité

| Opération | Posts | Commentaires | Messages |
|-----------|-------|--------------|----------|
| **Create** | Tous les rôles | Tous les rôles | Tous les rôles |
| **Read** | Admin = tous posts, Autres = feed filtré | Tous | Ses propres conversations |
| **Update** | Auteur ou Admin | Auteur ou Admin | Expéditeur uniquement (SQL) |
| **Delete** | Auteur ou Admin | Auteur ou Admin | Expéditeur uniquement (SQL) |

### Double Sécurité (UI + SQL)
- **Côté UI** : Les boutons Éditer/Supprimer ne s'affichent que si autorisé
- **Côté SQL** : Les messages vérifient `WHERE sender_id = ?` même si quelqu'un contourne l'UI

---

# 6. SCHÉMA DE FLUX CRUD — RÉSUMÉ

```
┌───────────────────────────────────────────────────────────────┐
│                    FLUX GÉNÉRAL D'UNE OPÉRATION CRUD          │
├───────────────────────────────────────────────────────────────┤
│                                                               │
│  1. Action Utilisateur (clic bouton)                          │
│         │                                                     │
│  2. Contrôleur (CommunityController)                          │
│     └── Validation des données                                │
│     └── Vérification des droits (canEditOrDelete)             │
│         │                                                     │
│  3. Thread Séparé (new Thread / Task)                         │
│     └── Service.getInstance().méthode(paramètres)             │
│         │                                                     │
│  4. Service (PostService / MessagingService)                  │
│     └── Connection = DatabaseConfig.getInstance().getConnection│
│     └── PreparedStatement avec paramètres ?                   │
│     └── executeUpdate() ou executeQuery()                     │
│     └── Mapping ResultSet → Objet Java (pour les READ)        │
│         │                                                     │
│  5. Base de Données MySQL/MariaDB                             │
│     └── Exécution SQL                                         │
│     └── Retour résultat                                       │
│         │                                                     │
│  6. Retour sur Thread JavaFX (Platform.runLater)              │
│     └── Mise à jour de l'interface                            │
│     └── Toast de confirmation ou message d'erreur             │
│     └── Rafraîchissement de la liste                          │
│                                                               │
└───────────────────────────────────────────────────────────────┘
```

**Points Clés à Retenir :**
- `try-with-resources` → Fermeture automatique des connexions et statements
- `PreparedStatement` avec `?` → Protection contre les injections SQL
- `Thread séparé` → Ne bloque pas l'interface utilisateur
- `Platform.runLater()` → Seul moyen de modifier l'UI depuis un autre thread
- `volatile` + `synchronized` → Thread-safety du Singleton
- Compteurs dénormalisés (likes_count, comments_count) → Performance à l'affichage

---

*Fin du document — Bonne chance pour la validation !* 🎓
