package com.skilora.community.service;

import com.skilora.config.DatabaseConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * SearchService — Service de recherche avancée dans le module Communauté.
 *
 * ╔═══════════════════════════════════════════════════════════════════════╗
 * ║  FONCTIONNALITÉ : Recherche globale multi-entités avec filtres       ║
 * ║  Entités cherchées : Posts, Messages, Événements, Groupes, Blog     ║
 * ║  Filtres : Mot-clé, Type de contenu, Date                           ║
 * ║  SQL : LIKE '%keyword%' avec protection PreparedStatement            ║
 * ╚═══════════════════════════════════════════════════════════════════════╝
 *
 * Pourquoi une recherche avancée ?
 *   L'application contient plusieurs types de contenu (posts, messages, événements,
 *   groupes, articles de blog). L'utilisateur doit pouvoir chercher rapidement dans
 *   tous ces contenus depuis un seul point d'entrée.
 *
 * Types de recherche :
 *   - GLOBAL : cherche dans tous les types de contenu en parallèle
 *   - POSTS  : cherche uniquement dans les posts
 *   - MESSAGES : cherche dans les messages des conversations de l'utilisateur
 *   - EVENTS : cherche dans les événements (titre, description, lieu)
 *   - GROUPS : cherche dans les groupes (nom, description)
 *   - BLOG   : cherche dans les articles de blog (titre, contenu, tags)
 *
 * Pattern : Singleton
 */
public class SearchService {

    private static final Logger logger = LoggerFactory.getLogger(SearchService.class);

    /**
     * Classe interne représentant un résultat de recherche.
     * Chaque résultat a un type (POST, MESSAGE, EVENT, GROUP, BLOG),
     * un titre, un extrait et la date de création.
     */
    public static class SearchResult {
        private final String type;          // Type : "POST", "MESSAGE", "EVENT", "GROUP", "BLOG"
        private final int id;               // ID de l'élément trouvé
        private final String title;         // Titre ou nom de l'élément
        private final String excerpt;       // Extrait du contenu (max 100 caractères)
        private final String author;        // Auteur ou créateur
        private final LocalDateTime date;   // Date de création

        public SearchResult(String type, int id, String title, String excerpt, String author, LocalDateTime date) {
            this.type = type;
            this.id = id;
            this.title = title;
            // Tronquer l'extrait à 100 caractères pour l'affichage compact
            this.excerpt = excerpt != null && excerpt.length() > 100
                    ? excerpt.substring(0, 100) + "..." : excerpt;
            this.author = author;
            this.date = date;
        }

        public String getType() { return type; }
        public int getId() { return id; }
        public String getTitle() { return title; }
        public String getExcerpt() { return excerpt; }
        public String getAuthor() { return author; }
        public LocalDateTime getDate() { return date; }
    }

    /** Enum des filtres de type de contenu pour la recherche */
    public enum SearchFilter {
        ALL("Tous"),            // Chercher dans tout
        POSTS("Posts"),          // Posts uniquement
        MESSAGES("Messages"),    // Messages uniquement
        EVENTS("Événements"),    // Événements uniquement
        GROUPS("Groupes"),       // Groupes uniquement
        BLOG("Blog");           // Blog uniquement

        private final String label;
        SearchFilter(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /**
     * Enum des filtres de date pour la recherche avancée.
     * Permet de limiter les résultats à une période donnée.
     */
    public enum DateFilter {
        ALL("Tout"),                  // Aucune limite de date
        TODAY("Aujourd'hui"),          // Dernières 24h
        THIS_WEEK("Cette semaine"),    // 7 derniers jours
        THIS_MONTH("Ce mois"),         // 30 derniers jours
        THIS_YEAR("Cette année");      // 365 derniers jours

        private final String label;
        DateFilter(String label) { this.label = label; }
        public String getLabel() { return label; }
    }

    /**
     * Filtre une liste de résultats par période de date.
     * @param results  la liste de résultats à filtrer
     * @param dateFilter  le filtre de date
     * @return liste filtrée
     */
    public List<SearchResult> filterByDate(List<SearchResult> results, DateFilter dateFilter) {
        if (dateFilter == null || dateFilter == DateFilter.ALL) return results;

        LocalDateTime cutoff = switch (dateFilter) {
            case TODAY      -> LocalDateTime.now().minusDays(1);
            case THIS_WEEK  -> LocalDateTime.now().minusDays(7);
            case THIS_MONTH -> LocalDateTime.now().minusDays(30);
            case THIS_YEAR  -> LocalDateTime.now().minusDays(365);
            default         -> null;
        };

        if (cutoff == null) return results;

        List<SearchResult> filtered = new ArrayList<>();
        for (SearchResult r : results) {
            if (r.getDate() != null && r.getDate().isAfter(cutoff)) {
                filtered.add(r);
            }
        }
        return filtered;
    }

    // ── Singleton ──
    private static volatile SearchService instance;
    private SearchService() {}

    public static SearchService getInstance() {
        if (instance == null) {
            synchronized (SearchService.class) {
                if (instance == null) {
                    instance = new SearchService();
                }
            }
        }
        return instance;
    }

    /**
     * Recherche globale dans tous les types de contenu ou filtré par type.
     *
     * @param keyword le mot-clé à chercher
     * @param filter  le filtre de type (ALL, POSTS, MESSAGES, etc.)
     * @param userId  l'ID de l'utilisateur (pour filtrer ses messages privés)
     * @return liste des résultats de recherche triés par date (récent d'abord)
     */
    public List<SearchResult> search(String keyword, SearchFilter filter, int userId) {
        List<SearchResult> results = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) return results;

        String likePattern = "%" + keyword + "%"; // Pattern LIKE pour la recherche partielle

        // Rechercher dans chaque type selon le filtre choisi
        if (filter == SearchFilter.ALL || filter == SearchFilter.POSTS) {
            results.addAll(searchPosts(likePattern));
        }
        if (filter == SearchFilter.ALL || filter == SearchFilter.MESSAGES) {
            results.addAll(searchMessages(likePattern, userId));
        }
        if (filter == SearchFilter.ALL || filter == SearchFilter.EVENTS) {
            results.addAll(searchEvents(likePattern));
        }
        if (filter == SearchFilter.ALL || filter == SearchFilter.GROUPS) {
            results.addAll(searchGroups(likePattern));
        }
        if (filter == SearchFilter.ALL || filter == SearchFilter.BLOG) {
            results.addAll(searchBlog(likePattern));
        }

        // Trier par date décroissante (plus récent en premier)
        results.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });

        logger.info("Search '{}' (filter={}) returned {} results", keyword, filter, results.size());
        return results;
    }

    /**
     * Recherche dans les posts.
     * Cherche dans le contenu des posts publiés + nom de l'auteur.
     *
     * SQL : SELECT p.*, u.full_name FROM posts p JOIN users u
     *       WHERE (p.content LIKE ? OR u.full_name LIKE ?) AND is_published = TRUE
     */
    private List<SearchResult> searchPosts(String pattern) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT p.id, p.content, u.full_name, p.created_date " +
                "FROM posts p JOIN users u ON p.author_id = u.id " +
                "WHERE p.is_published = TRUE AND (p.content LIKE ? OR u.full_name LIKE ?) " +
                "ORDER BY p.created_date DESC LIMIT 20";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_date");
                results.add(new SearchResult(
                        "POST",
                        rs.getInt("id"),
                        "Post",                              // Type affiché
                        rs.getString("content"),             // Contenu comme extrait
                        rs.getString("full_name"),           // Nom de l'auteur
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            logger.error("Error searching posts: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Recherche dans les messages de l'utilisateur.
     * Filtre par les conversations auxquelles l'utilisateur participe.
     *
     * SÉCURITÉ : Seuls les messages des conversations de l'utilisateur sont inclus.
     */
    private List<SearchResult> searchMessages(String pattern, int userId) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT m.id, m.content, u.full_name, m.created_date " +
                "FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "JOIN conversations c ON m.conversation_id = c.id " +
                "WHERE m.content LIKE ? " +
                "AND (c.participant_1 = ? OR c.participant_2 = ?) " +
                "ORDER BY m.created_date DESC LIMIT 20";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setInt(2, userId);
            stmt.setInt(3, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_date");
                results.add(new SearchResult(
                        "MESSAGE",
                        rs.getInt("id"),
                        "Message de " + rs.getString("full_name"), // Titre avec expéditeur
                        rs.getString("content"),
                        rs.getString("full_name"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            logger.error("Error searching messages: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Recherche dans les événements.
     * Cherche dans le titre, la description et le lieu.
     */
    private List<SearchResult> searchEvents(String pattern) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT e.id, e.title, e.description, e.location, e.start_date, u.full_name " +
                "FROM events e JOIN users u ON e.organizer_id = u.id " +
                "WHERE e.title LIKE ? OR e.description LIKE ? OR e.location LIKE ? " +
                "ORDER BY e.start_date DESC LIMIT 20";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("start_date");
                results.add(new SearchResult(
                        "EVENT",
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("description"),
                        rs.getString("full_name"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            logger.error("Error searching events: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Recherche dans les groupes.
     * Cherche dans le nom et la description.
     */
    private List<SearchResult> searchGroups(String pattern) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT g.id, g.name, g.description, g.created_date, u.full_name " +
                "FROM community_groups g JOIN users u ON g.created_by = u.id " +
                "WHERE g.name LIKE ? OR g.description LIKE ? " +
                "ORDER BY g.created_date DESC LIMIT 20";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_date");
                results.add(new SearchResult(
                        "GROUP",
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getString("full_name"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            logger.error("Error searching groups: {}", e.getMessage());
        }
        return results;
    }

    /**
     * Recherche dans les articles de blog.
     * Cherche dans le titre, le contenu et les tags.
     */
    private List<SearchResult> searchBlog(String pattern) {
        List<SearchResult> results = new ArrayList<>();
        String sql = "SELECT b.id, b.title, b.summary, b.tags, b.created_date, u.full_name " +
                "FROM blog_articles b JOIN users u ON b.author_id = u.id " +
                "WHERE b.status = 'PUBLISHED' AND (b.title LIKE ? OR b.summary LIKE ? OR b.tags LIKE ?) " +
                "ORDER BY b.created_date DESC LIMIT 20";
        try (Connection conn = DatabaseConfig.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("created_date");
                results.add(new SearchResult(
                        "BLOG",
                        rs.getInt("id"),
                        rs.getString("title"),
                        rs.getString("summary"),
                        rs.getString("full_name"),
                        ts != null ? ts.toLocalDateTime() : null
                ));
            }
        } catch (SQLException e) {
            logger.error("Error searching blog: {}", e.getMessage());
        }
        return results;
    }
}
