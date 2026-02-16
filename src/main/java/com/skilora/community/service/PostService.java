package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import com.skilora.community.entity.Post;
import com.skilora.community.entity.PostComment;
import com.skilora.community.enums.PostType;
import com.skilora.formation.service.AchievementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * PostService — Service CRUD pour la gestion des publications (posts).
 *
 * Responsabilités :
 *   - CREATE : Créer un nouveau post (create)
 *   - READ   : Lire un post par ID (findById), lire le fil d'actualité (getFeed),
 *              lire tous les posts admin (findAll), lire par auteur (getByAuthor)
 *   - UPDATE : Modifier le contenu, l'image et le type d'un post (update)
 *   - DELETE : Supprimer un post (delete)
 *   - LIKE   : Ajouter/retirer un like (toggleLike), vérifier si liké (isLikedBy)
 *   - COMMENT: Ajouter (addComment), lire (getComments), modifier (updateComment),
 *              supprimer un commentaire (deleteComment)
 *
 * Pattern : Singleton thread-safe (Double-Checked Locking)
 * Base de données : MySQL/MariaDB via HikariCP (DatabaseConfig)
 */
public class PostService {

    private static final Logger logger = LoggerFactory.getLogger(PostService.class);

    // ── Instance unique (pattern Singleton) ──
    private static volatile PostService instance;

    // Constructeur privé : empêche l'instanciation directe (Singleton)
    private PostService() {}

    /**
     * Retourne l'instance unique du PostService.
     * Utilise le pattern Double-Checked Locking pour la sécurité multi-thread.
     * @return l'instance singleton de PostService
     */
    public static PostService getInstance() {
        if (instance == null) {                      // 1ère vérification (sans verrou)
            synchronized (PostService.class) {       // Verrou pour thread-safety
                if (instance == null) {              // 2ème vérification (avec verrou)
                    instance = new PostService();
                }
            }
        }
        return instance;
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION CREATE — Créer un nouveau post
    // ═══════════════════════════════════════════════════════════

    /**
     * Crée un nouveau post dans la base de données.
     * Insère les données du post et récupère l'ID auto-généré.
     * Déclenche aussi la vérification des achievements (gamification).
     *
     * @param post l'objet Post contenant les données (authorId, content, imageUrl, postType)
     * @return l'ID du post créé, ou -1 en cas d'échec
     */
    public int create(Post post) {
        // Requête SQL INSERT avec paramètres préparés (protection contre injection SQL)
        String sql = "INSERT INTO posts (author_id, content, image_url, post_type, is_published, created_date) VALUES (?, ?, ?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            // Remplissage des paramètres de la requête préparée
            stmt.setInt(1, post.getAuthorId());        // ID de l'auteur
            stmt.setString(2, post.getContent());      // Contenu du post
            stmt.setString(3, post.getImageUrl());     // URL de l'image (peut être null)
            stmt.setString(4, post.getPostType().name()); // Type : STATUS, ARTICLE_SHARE, etc.
            stmt.setBoolean(5, post.isPublished());    // Publié ou brouillon
            stmt.executeUpdate();                      // Exécution de l'INSERT
            
            // Récupération de l'ID auto-généré par MySQL (clé primaire)
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                logger.info("Post created with id {}", id);
                
                // Vérifier et attribuer un badge/achievement pour le premier post
                AchievementService.getInstance().checkAndAward(post.getAuthorId());
                return id; // Retourne l'ID du post créé avec succès
            }
        } catch (SQLException e) {
            logger.error("Error creating post: {}", e.getMessage(), e);
        }
        return -1; // Échec de la création
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION READ — Lire les posts
    // ═══════════════════════════════════════════════════════════

    /**
     * Recherche un post par son ID avec JOIN sur la table users
     * pour récupérer le nom et la photo de l'auteur.
     *
     * @param id l'identifiant du post à rechercher
     * @return l'objet Post trouvé, ou null si non trouvé
     */
    public Post findById(int id) {
        // JOIN avec users pour récupérer les infos de l'auteur en une seule requête
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            WHERE p.id = ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapPost(rs); // Conversion ResultSet → objet Post
            }
        } catch (SQLException e) {
            logger.error("Error finding post: {}", e.getMessage(), e);
        }
        return null; // Post non trouvé
    }

    /**
     * Récupère le fil d'actualité (feed) d'un utilisateur.
     * Affiche les posts de l'utilisateur + ses connexions acceptées.
     * Utilise la pagination (page, pageSize) pour limiter les résultats.
     *
     * @param userId   l'ID de l'utilisateur connecté
     * @param page     numéro de page (commence à 1)
     * @param pageSize nombre de posts par page
     * @return liste des posts du fil d'actualité
     */
    public List<Post> getFeed(int userId, int page, int pageSize) {
        List<Post> feed = new ArrayList<>();
        int offset = (page - 1) * pageSize; // Calcul de l'offset pour la pagination
        
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            WHERE p.is_published = TRUE
            AND (p.author_id = ? OR p.author_id IN (
                SELECT user_id_2 FROM connections WHERE user_id_1 = ? AND status = 'ACCEPTED'
                UNION
                SELECT user_id_1 FROM connections WHERE user_id_2 = ? AND status = 'ACCEPTED'
            ))
            ORDER BY p.created_date DESC
            LIMIT ? OFFSET ?
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            stmt.setInt(4, pageSize);
            stmt.setInt(5, offset);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Post post = mapPost(rs);
                post.setLikedByCurrentUser(isLikedBy(post.getId(), userId));
                feed.add(post);
            }
        } catch (SQLException e) {
            logger.error("Error fetching feed: {}", e.getMessage(), e);
        }
        return feed;
    }

    /**
     * Get all posts (admin view) - includes unpublished.
     */
    public List<Post> findAll() {
        List<Post> posts = new ArrayList<>();
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            ORDER BY p.created_date DESC
            """;
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching all posts: {}", e.getMessage(), e);
        }
        return posts;
    }

    /**
     * Récupère tous les posts d'un auteur spécifique.
     * Utilisé pour afficher le profil d'un utilisateur.
     *
     * @param authorId l'ID de l'auteur
     * @return liste des posts de cet auteur, triés du plus récent au plus ancien
     */
    public List<Post> getByAuthor(int authorId) {
        List<Post> posts = new ArrayList<>();
        String sql = """
            SELECT p.*, u.full_name as author_name, u.photo_url as author_photo
            FROM posts p
            JOIN users u ON p.author_id = u.id
            WHERE p.author_id = ?
            ORDER BY p.created_date DESC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, authorId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                posts.add(mapPost(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching author posts: {}", e.getMessage(), e);
        }
        return posts;
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION UPDATE — Modifier un post existant
    // ═══════════════════════════════════════════════════════════

    /**
     * Met à jour le contenu, l'image et le type d'un post existant.
     * Met aussi à jour la date de modification (updated_date = NOW()).
     *
     * @param post l'objet Post avec les nouvelles données (id requis)
     * @return true si la mise à jour a réussi, false sinon
     */
    public boolean update(Post post) {
        // UPDATE avec paramètres préparés — WHERE id = ? cible le post exact
        String sql = "UPDATE posts SET content = ?, image_url = ?, post_type = ?, updated_date = NOW() WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, post.getContent());       // Nouveau contenu
            stmt.setString(2, post.getImageUrl());      // Nouvelle URL image
            stmt.setString(3, post.getPostType().name()); // Nouveau type
            stmt.setInt(4, post.getId());               // ID du post à modifier
            return stmt.executeUpdate() > 0;            // true si au moins 1 ligne modifiée
        } catch (SQLException e) {
            logger.error("Error updating post: {}", e.getMessage(), e);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  OPÉRATION DELETE — Supprimer un post
    // ═══════════════════════════════════════════════════════════

    /**
     * Supprime un post de la base de données.
     * La suppression en cascade supprime aussi les likes et commentaires associés.
     *
     * @param id l'ID du post à supprimer
     * @return true si la suppression a réussi, false sinon
     */
    public boolean delete(int id) {
        String sql = "DELETE FROM posts WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);                          // ID du post à supprimer
            return stmt.executeUpdate() > 0;             // true si 1 ligne supprimée
        } catch (SQLException e) {
            logger.error("Error deleting post: {}", e.getMessage(), e);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTION DES LIKES — Toggle (ajouter/retirer)
    // ═══════════════════════════════════════════════════════════

    /**
     * Ajoute ou retire un like sur un post (toggle).
     * Si l'utilisateur a déjà liké → retire le like (unlike).
     * Si l'utilisateur n'a pas liké → ajoute le like.
     * Met à jour le compteur dénormalisé likes_count dans la table posts.
     *
     * @param postId l'ID du post
     * @param userId l'ID de l'utilisateur qui like/unlike
     * @return true si l'opération a réussi, false sinon
     */
    public boolean toggleLike(int postId, int userId) {
        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Vérifier si le like existe déjà dans la table post_likes
            String checkSql = "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkSql)) {
                stmt.setInt(1, postId);
                stmt.setInt(2, userId);
                ResultSet rs = stmt.executeQuery();
                
                if (rs.next()) {
                    // ── CAS 1 : L'utilisateur a déjà liké → UNLIKE (retirer le like) ──
                    String deleteSql = "DELETE FROM post_likes WHERE post_id = ? AND user_id = ?";
                    try (PreparedStatement deleteStmt = conn.prepareStatement(deleteSql)) {
                        deleteStmt.setInt(1, postId);
                        deleteStmt.setInt(2, userId);
                        deleteStmt.executeUpdate();
                    }
                    updateLikeCount(conn, postId, -1); // Décrémenter le compteur dénormalisé
                } else {
                    // ── CAS 2 : L'utilisateur n'a pas liké → LIKE (ajouter le like) ──
                    String insertSql = "INSERT INTO post_likes (post_id, user_id, created_date) VALUES (?, ?, NOW())";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                        insertStmt.setInt(1, postId);
                        insertStmt.setInt(2, userId);
                        insertStmt.executeUpdate();
                    }
                    updateLikeCount(conn, postId, 1); // Incrémenter le compteur dénormalisé
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error toggling like: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Met à jour le compteur dénormalisé de likes dans la table posts.
     * @param delta +1 pour un like, -1 pour un unlike
     */
    private void updateLikeCount(Connection conn, int postId, int delta) throws SQLException {
        String sql = "UPDATE posts SET likes_count = likes_count + ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, delta);
            stmt.setInt(2, postId);
            stmt.executeUpdate();
        }
    }

    /**
     * Vérifie si un utilisateur a liké un post donné.
     * @return true si le like existe, false sinon
     */
    public boolean isLikedBy(int postId, int userId) {
        String sql = "SELECT 1 FROM post_likes WHERE post_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();
            return rs.next();
        } catch (SQLException e) {
            logger.error("Error checking like: {}", e.getMessage(), e);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  GESTION DES COMMENTAIRES — CRUD complet
    // ═══════════════════════════════════════════════════════════

    /**
     * Ajoute un commentaire sous un post.
     * Insère dans post_comments et incrémente le compteur dénormalisé comments_count.
     *
     * @param comment l'objet PostComment (postId, authorId, content)
     * @return l'ID du commentaire créé, ou -1 en cas d'échec
     */
    public int addComment(PostComment comment) {
        String sql = "INSERT INTO post_comments (post_id, author_id, content, created_date) VALUES (?, ?, ?, NOW())";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setInt(1, comment.getPostId());     // Le post associé
            stmt.setInt(2, comment.getAuthorId());   // L'auteur du commentaire
            stmt.setString(3, comment.getContent()); // Le texte du commentaire
            stmt.executeUpdate();
            
            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                // Incrémenter le compteur dénormalisé dans la table posts
                String updateSql = "UPDATE posts SET comments_count = comments_count + 1 WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, comment.getPostId());
                    updateStmt.executeUpdate();
                }
                return id; // Retourne l'ID du commentaire créé
            }
        } catch (SQLException e) {
            logger.error("Error adding comment: {}", e.getMessage(), e);
        }
        return -1;
    }

    /**
     * Récupère tous les commentaires d'un post, triés par date croissante.
     * JOIN avec users pour afficher le nom et la photo de l'auteur.
     *
     * @param postId l'ID du post
     * @return liste des commentaires
     */
    public List<PostComment> getComments(int postId) {
        List<PostComment> comments = new ArrayList<>();
        // JOIN avec la table users pour récupérer author_name et author_photo
        String sql = """
            SELECT c.*, u.full_name as author_name, u.photo_url as author_photo
            FROM post_comments c
            JOIN users u ON c.author_id = u.id
            WHERE c.post_id = ?
            ORDER BY c.created_date ASC
            """;
        
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, postId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                comments.add(mapComment(rs));
            }
        } catch (SQLException e) {
            logger.error("Error fetching comments: {}", e.getMessage(), e);
        }
        return comments;
    }

    /**
     * Supprime un commentaire et décrémente le compteur dénormalisé.
     * Utilise GREATEST(..., 0) pour éviter un compteur négatif.
     *
     * @param commentId l'ID du commentaire à supprimer
     * @param postId    l'ID du post parent (pour décrémenter comments_count)
     * @return true si supprimé avec succès
     */
    public boolean deleteComment(int commentId, int postId) {
        String sql = "DELETE FROM post_comments WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, commentId);
            if (stmt.executeUpdate() > 0) {
                // Décrémenter le compteur sans descendre sous 0
                String updateSql = "UPDATE posts SET comments_count = GREATEST(comments_count - 1, 0) WHERE id = ?";
                try (PreparedStatement updateStmt = conn.prepareStatement(updateSql)) {
                    updateStmt.setInt(1, postId);
                    updateStmt.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            logger.error("Error deleting comment: {}", e.getMessage(), e);
        }
        return false;
    }

    /**
     * Modifie le contenu d'un commentaire existant.
     *
     * @param commentId  l'ID du commentaire à modifier
     * @param newContent le nouveau contenu textuel
     * @return true si la modification a réussi
     */
    public boolean updateComment(int commentId, String newContent) {
        String sql = "UPDATE post_comments SET content = ? WHERE id = ?";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newContent);    // Nouveau contenu
            stmt.setInt(2, commentId);        // ID du commentaire à modifier
            return stmt.executeUpdate() > 0;  // true si 1 ligne modifiée
        } catch (SQLException e) {
            logger.error("Error updating comment: {}", e.getMessage(), e);
        }
        return false;
    }

    // ═══════════════════════════════════════════════════════════
    //  MAPPING — Conversion ResultSet → Objet Java
    // ═══════════════════════════════════════════════════════════

    /**
     * Convertit une ligne du ResultSet en objet Post.
     * Mappe toutes les colonnes de la table posts + les colonnes du JOIN (author_name, author_photo).
     */
    private Post mapPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("id"));
        post.setAuthorId(rs.getInt("author_id"));
        post.setContent(rs.getString("content"));
        post.setImageUrl(rs.getString("image_url"));
        post.setPostType(PostType.valueOf(rs.getString("post_type")));
        post.setLikesCount(rs.getInt("likes_count"));
        post.setCommentsCount(rs.getInt("comments_count"));
        post.setSharesCount(rs.getInt("shares_count"));
        post.setPublished(rs.getBoolean("is_published"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) post.setCreatedDate(created.toLocalDateTime());
        
        Timestamp updated = rs.getTimestamp("updated_date");
        if (updated != null) post.setUpdatedDate(updated.toLocalDateTime());
        
        post.setAuthorName(rs.getString("author_name"));
        post.setAuthorPhoto(rs.getString("author_photo"));
        
        return post;
    }

    /**
     * Convertit une ligne du ResultSet en objet PostComment.
     * Mappe les colonnes de post_comments + les colonnes du JOIN (author_name, author_photo).
     */
    private PostComment mapComment(ResultSet rs) throws SQLException {
        PostComment comment = new PostComment();
        comment.setId(rs.getInt("id"));
        comment.setPostId(rs.getInt("post_id"));
        comment.setAuthorId(rs.getInt("author_id"));
        comment.setContent(rs.getString("content"));
        
        Timestamp created = rs.getTimestamp("created_date");
        if (created != null) comment.setCreatedDate(created.toLocalDateTime());
        
        comment.setAuthorName(rs.getString("author_name"));
        comment.setAuthorPhoto(rs.getString("author_photo"));
        
        return comment;
    }
}
