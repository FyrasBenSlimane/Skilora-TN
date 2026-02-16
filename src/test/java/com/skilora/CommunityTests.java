package com.skilora;

import com.skilora.community.entity.Post;
import com.skilora.community.entity.PostComment;
import com.skilora.community.entity.Message;
import com.skilora.community.entity.Conversation;
import com.skilora.community.enums.PostType;
import com.skilora.community.service.PostService;
import com.skilora.community.service.MessagingService;
import com.skilora.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitaires CRUD — Module Communauté
 * 
 * Teste les opérations CRUD de :
 *   - PostService      : create, findById, update, delete, toggleLike, addComment, getComments, updateComment, deleteComment
 *   - MessagingService : getOrCreateConversation, sendMessage, getMessages, updateMessage, deleteMessage, markAsRead, getUnreadCount
 * 
 * Suivant le workshop "Workshop Test unitaire" (ESPRIT - UP JAVA)
 * Utilise : JUnit 5, @TestMethodOrder, @BeforeAll, @AfterAll, assertions
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Tests CRUD — Module Communauté (Posts & Messages)")
class CommunityTests {

    // ──────────────────────────────────────────────
    //  Services (initialisés une seule fois)
    // ──────────────────────────────────────────────
    static PostService postService;
    static MessagingService messagingService;

    // IDs utilisés pour le chaînage des tests (créer → lire → modifier → supprimer)
    static int testPostId;
    static int testCommentId;
    static int testConversationId;
    static int testMessageId;

    // IDs d'utilisateurs existants en base (admin=1, user=2 par convention)
    static final int USER_ID_1 = 1;
    static final int USER_ID_2 = 2;

    // ──────────────────────────────────────────────
    //  Initialisation — @BeforeAll
    // ──────────────────────────────────────────────

    @BeforeAll
    static void setup() {
        // Initialisation des services Singleton
        postService = PostService.getInstance();
        messagingService = MessagingService.getInstance();

        assertNotNull(postService, "PostService doit être initialisé");
        assertNotNull(messagingService, "MessagingService doit être initialisé");
    }

    // ═══════════════════════════════════════════════════════════
    //  PARTIE 1 — TESTS CRUD DES POSTS
    // ═══════════════════════════════════════════════════════════

    // ── 1. CREATE ──

    @Test
    @Order(1)
    @DisplayName("1.1 Créer un post — PostService.create()")
    void testCreerPost() {
        // Préparer les données d'entrée
        Post post = new Post();
        post.setAuthorId(USER_ID_1);
        post.setContent("Ceci est un post de test unitaire — JUnit 5");
        post.setImageUrl("https://test.com/image.png");
        post.setPostType(PostType.STATUS);

        // Exécuter l'opération CREATE
        testPostId = postService.create(post);

        // Vérifier : l'ID retourné doit être > 0 (succès d'insertion)
        assertTrue(testPostId > 0, "L'ID du post créé doit être > 0");
        System.out.println("✅ Post créé avec l'ID : " + testPostId);
    }

    // ── 2. READ ──

    @Test
    @Order(2)
    @DisplayName("1.2 Lire un post par ID — PostService.findById()")
    void testLirePostParId() {
        // Exécuter l'opération READ
        Post post = postService.findById(testPostId);

        // Vérifier : le post doit exister et contenir les bonnes données
        assertNotNull(post, "Le post doit exister en base");
        assertEquals(testPostId, post.getId(), "L'ID doit correspondre");
        assertEquals(USER_ID_1, post.getAuthorId(), "L'auteur doit correspondre");
        assertEquals("Ceci est un post de test unitaire — JUnit 5", post.getContent(), "Le contenu doit correspondre");
        assertEquals("https://test.com/image.png", post.getImageUrl(), "L'URL image doit correspondre");
        assertEquals(PostType.STATUS, post.getPostType(), "Le type doit être STATUS");
        assertTrue(post.isPublished(), "Le post doit être publié par défaut");
        assertNotNull(post.getAuthorName(), "Le nom de l'auteur doit être rempli par le JOIN");
        System.out.println("✅ Post lu : " + post.getContent() + " (auteur: " + post.getAuthorName() + ")");
    }

    @Test
    @Order(3)
    @DisplayName("1.3 Lire le fil d'actualité — PostService.getFeed()")
    void testLireFeed() {
        // Exécuter l'opération READ (Feed)
        List<Post> feed = postService.getFeed(USER_ID_1, 1, 50);

        // Vérifier : le feed ne doit pas être vide (on vient de créer un post)
        assertNotNull(feed, "Le feed ne doit pas être null");
        assertFalse(feed.isEmpty(), "Le feed ne doit pas être vide");

        // Vérifier que notre post de test est dans le feed
        boolean trouve = feed.stream().anyMatch(p -> p.getId() == testPostId);
        assertTrue(trouve, "Le post de test doit apparaître dans le feed");
        System.out.println("✅ Feed chargé : " + feed.size() + " posts");
    }

    @Test
    @Order(4)
    @DisplayName("1.4 Lire tous les posts (admin) — PostService.findAll()")
    void testLireTousPosts() {
        // Exécuter l'opération READ (findAll pour Admin)
        List<Post> allPosts = postService.findAll();

        // Vérifier
        assertNotNull(allPosts, "La liste ne doit pas être null");
        assertFalse(allPosts.isEmpty(), "La liste ne doit pas être vide");

        boolean trouve = allPosts.stream().anyMatch(p -> p.getId() == testPostId);
        assertTrue(trouve, "Le post de test doit apparaître dans findAll");
        System.out.println("✅ findAll : " + allPosts.size() + " posts au total");
    }

    @Test
    @Order(5)
    @DisplayName("1.5 Lire les posts d'un auteur — PostService.getByAuthor()")
    void testLirePostsParAuteur() {
        List<Post> posts = postService.getByAuthor(USER_ID_1);

        assertNotNull(posts, "La liste ne doit pas être null");
        assertFalse(posts.isEmpty(), "L'auteur doit avoir au moins 1 post");

        // Tous les posts doivent appartenir à l'auteur
        boolean tousLesMemes = posts.stream().allMatch(p -> p.getAuthorId() == USER_ID_1);
        assertTrue(tousLesMemes, "Tous les posts doivent être de l'auteur USER_ID_1");
        System.out.println("✅ Posts de l'auteur " + USER_ID_1 + " : " + posts.size());
    }

    // ── 3. UPDATE ──

    @Test
    @Order(6)
    @DisplayName("1.6 Modifier un post — PostService.update()")
    void testModifierPost() {
        // Charger le post existant
        Post post = postService.findById(testPostId);
        assertNotNull(post, "Le post doit exister avant modification");

        // Modifier les données
        post.setContent("Contenu modifié par le test unitaire");
        post.setImageUrl("https://test.com/nouvelle-image.png");
        post.setPostType(PostType.ARTICLE_SHARE);

        // Exécuter l'opération UPDATE
        boolean resultat = postService.update(post);

        // Vérifier : la mise à jour doit réussir
        assertTrue(resultat, "La modification doit retourner true");

        // Relire pour confirmer les changements en base
        Post postModifie = postService.findById(testPostId);
        assertEquals("Contenu modifié par le test unitaire", postModifie.getContent(), "Le contenu doit être modifié");
        assertEquals("https://test.com/nouvelle-image.png", postModifie.getImageUrl(), "L'URL doit être modifiée");
        assertEquals(PostType.ARTICLE_SHARE, postModifie.getPostType(), "Le type doit être modifié");
        System.out.println("✅ Post modifié : " + postModifie.getContent());
    }

    // ── LIKE ──

    @Test
    @Order(7)
    @DisplayName("1.7 Ajouter un like — PostService.toggleLike()")
    void testAjouterLike() {
        // Exécuter : ajouter un like
        boolean resultat = postService.toggleLike(testPostId, USER_ID_1);
        assertTrue(resultat, "toggleLike doit retourner true");

        // Vérifier que le like est enregistré
        boolean isLiked = postService.isLikedBy(testPostId, USER_ID_1);
        assertTrue(isLiked, "Le post doit être liké par l'utilisateur");

        // Vérifier le compteur
        Post post = postService.findById(testPostId);
        assertTrue(post.getLikesCount() >= 1, "Le compteur de likes doit être >= 1");
        System.out.println("✅ Like ajouté — likes_count = " + post.getLikesCount());
    }

    @Test
    @Order(8)
    @DisplayName("1.8 Retirer un like (toggle) — PostService.toggleLike()")
    void testRetirerLike() {
        // Exécuter : retirer le like (toggle = 2ème appel)
        boolean resultat = postService.toggleLike(testPostId, USER_ID_1);
        assertTrue(resultat, "toggleLike doit retourner true");

        // Vérifier que le like est retiré
        boolean isLiked = postService.isLikedBy(testPostId, USER_ID_1);
        assertFalse(isLiked, "Le post ne doit plus être liké");
        System.out.println("✅ Like retiré");
    }

    // ── COMMENTAIRES ──

    @Test
    @Order(9)
    @DisplayName("1.9 Ajouter un commentaire — PostService.addComment()")
    void testAjouterCommentaire() {
        // Préparer le commentaire
        PostComment comment = new PostComment();
        comment.setPostId(testPostId);
        comment.setAuthorId(USER_ID_1);
        comment.setContent("Commentaire de test unitaire");

        // Exécuter l'opération CREATE (commentaire)
        testCommentId = postService.addComment(comment);

        // Vérifier
        assertTrue(testCommentId > 0, "L'ID du commentaire doit être > 0");

        // Vérifier le compteur dénormalisé
        Post post = postService.findById(testPostId);
        assertTrue(post.getCommentsCount() >= 1, "Le compteur de commentaires doit être >= 1");
        System.out.println("✅ Commentaire créé avec l'ID : " + testCommentId);
    }

    @Test
    @Order(10)
    @DisplayName("1.10 Lire les commentaires — PostService.getComments()")
    void testLireCommentaires() {
        // Exécuter l'opération READ (commentaires)
        List<PostComment> comments = postService.getComments(testPostId);

        // Vérifier
        assertNotNull(comments, "La liste ne doit pas être null");
        assertFalse(comments.isEmpty(), "Il doit y avoir au moins 1 commentaire");

        // Chercher notre commentaire de test
        boolean trouve = comments.stream().anyMatch(c -> c.getId() == testCommentId);
        assertTrue(trouve, "Le commentaire de test doit être dans la liste");

        // Vérifier les données
        PostComment commentTrouve = comments.stream()
                .filter(c -> c.getId() == testCommentId)
                .findFirst().orElse(null);
        assertNotNull(commentTrouve);
        assertEquals("Commentaire de test unitaire", commentTrouve.getContent());
        assertNotNull(commentTrouve.getAuthorName(), "Le nom de l'auteur doit être rempli par le JOIN");
        System.out.println("✅ Commentaires lus : " + comments.size());
    }

    @Test
    @Order(11)
    @DisplayName("1.11 Modifier un commentaire — PostService.updateComment()")
    void testModifierCommentaire() {
        // Exécuter l'opération UPDATE
        boolean resultat = postService.updateComment(testCommentId, "Commentaire modifié par test");

        // Vérifier
        assertTrue(resultat, "La modification doit retourner true");

        // Relire pour confirmer
        List<PostComment> comments = postService.getComments(testPostId);
        PostComment modifie = comments.stream()
                .filter(c -> c.getId() == testCommentId)
                .findFirst().orElse(null);
        assertNotNull(modifie, "Le commentaire doit toujours exister");
        assertEquals("Commentaire modifié par test", modifie.getContent(), "Le contenu doit être modifié");
        System.out.println("✅ Commentaire modifié : " + modifie.getContent());
    }

    @Test
    @Order(12)
    @DisplayName("1.12 Supprimer un commentaire — PostService.deleteComment()")
    void testSupprimerCommentaire() {
        // Exécuter l'opération DELETE
        boolean resultat = postService.deleteComment(testCommentId, testPostId);

        // Vérifier
        assertTrue(resultat, "La suppression doit retourner true");

        // Confirmer : le commentaire ne doit plus exister
        List<PostComment> comments = postService.getComments(testPostId);
        boolean existe = comments.stream().anyMatch(c -> c.getId() == testCommentId);
        assertFalse(existe, "Le commentaire ne doit plus exister après suppression");
        System.out.println("✅ Commentaire supprimé");
    }

    // ── 4. DELETE ──

    @Test
    @Order(13)
    @DisplayName("1.13 Supprimer un post — PostService.delete()")
    void testSupprimerPost() {
        // Exécuter l'opération DELETE
        boolean resultat = postService.delete(testPostId);

        // Vérifier
        assertTrue(resultat, "La suppression doit retourner true");

        // Confirmer : le post ne doit plus exister
        Post postSupprime = postService.findById(testPostId);
        assertNull(postSupprime, "Le post ne doit plus exister après suppression");
        System.out.println("✅ Post supprimé (ID: " + testPostId + ")");
    }

    // ═══════════════════════════════════════════════════════════
    //  PARTIE 2 — TESTS CRUD DES MESSAGES
    // ═══════════════════════════════════════════════════════════

    // ── 1. CREATE CONVERSATION ──

    @Test
    @Order(20)
    @DisplayName("2.1 Créer/Récupérer une conversation — MessagingService.getOrCreateConversation()")
    void testCreerConversation() {
        // Exécuter : créer une conversation entre USER_ID_1 et USER_ID_2
        testConversationId = messagingService.getOrCreateConversation(USER_ID_1, USER_ID_2);

        // Vérifier : l'ID doit être valide
        assertTrue(testConversationId > 0, "L'ID de la conversation doit être > 0");
        System.out.println("✅ Conversation créée/récupérée avec l'ID : " + testConversationId);
    }

    @Test
    @Order(21)
    @DisplayName("2.2 Récupérer la même conversation (pas de doublon)")
    void testConversationPasDeDoublon() {
        // Appeler avec les mêmes utilisateurs dans l'ordre inverse
        int conversationId2 = messagingService.getOrCreateConversation(USER_ID_2, USER_ID_1);

        // Vérifier : doit retourner le même ID (Math.min/Math.max normalise)
        assertEquals(testConversationId, conversationId2,
                "La même conversation doit être retournée (pas de doublon)");
        System.out.println("✅ Pas de doublon — même ID : " + conversationId2);
    }

    // ── 2. CREATE MESSAGE ──

    @Test
    @Order(22)
    @DisplayName("2.3 Envoyer un message — MessagingService.sendMessage()")
    void testEnvoyerMessage() {
        // Exécuter l'opération CREATE
        testMessageId = messagingService.sendMessage(
                testConversationId, USER_ID_1, "Message de test unitaire — JUnit 5");

        // Vérifier : l'ID du message doit être > 0
        assertTrue(testMessageId > 0, "L'ID du message envoyé doit être > 0");
        System.out.println("✅ Message envoyé avec l'ID : " + testMessageId);
    }

    // ── 3. READ MESSAGES ──

    @Test
    @Order(23)
    @DisplayName("2.4 Lire les messages — MessagingService.getMessages()")
    void testLireMessages() {
        // Exécuter l'opération READ
        List<Message> messages = messagingService.getMessages(testConversationId, 1, 100);

        // Vérifier
        assertNotNull(messages, "La liste ne doit pas être null");
        assertFalse(messages.isEmpty(), "Il doit y avoir au moins 1 message");

        // Chercher notre message de test
        boolean trouve = messages.stream().anyMatch(m -> m.getId() == testMessageId);
        assertTrue(trouve, "Le message de test doit être dans la liste");

        // Vérifier les données
        Message msg = messages.stream()
                .filter(m -> m.getId() == testMessageId)
                .findFirst().orElse(null);
        assertNotNull(msg);
        assertEquals("Message de test unitaire — JUnit 5", msg.getContent(), "Le contenu doit correspondre");
        assertEquals(USER_ID_1, msg.getSenderId(), "L'expéditeur doit correspondre");
        assertEquals(testConversationId, msg.getConversationId(), "L'ID conversation doit correspondre");
        assertFalse(msg.isRead(), "Le message doit être non lu par défaut");
        assertNotNull(msg.getSenderName(), "Le nom de l'expéditeur doit être rempli par le JOIN");
        System.out.println("✅ Messages lus : " + messages.size() + " — contenu : " + msg.getContent());
    }

    @Test
    @Order(24)
    @DisplayName("2.5 Lire les conversations — MessagingService.getConversations()")
    void testLireConversations() {
        // Exécuter l'opération READ
        List<Conversation> conversations = messagingService.getConversations(USER_ID_1);

        // Vérifier
        assertNotNull(conversations, "La liste ne doit pas être null");
        assertFalse(conversations.isEmpty(), "L'utilisateur doit avoir au moins 1 conversation");

        // Chercher notre conversation de test
        boolean trouve = conversations.stream().anyMatch(c -> c.getId() == testConversationId);
        assertTrue(trouve, "La conversation de test doit être dans la liste");

        // Vérifier les champs transitoires
        Conversation conv = conversations.stream()
                .filter(c -> c.getId() == testConversationId)
                .findFirst().orElse(null);
        assertNotNull(conv);
        assertNotNull(conv.getOtherUserName(), "Le nom de l'autre participant doit être rempli");
        assertNotNull(conv.getLastMessagePreview(), "L'aperçu du dernier message doit être rempli");
        System.out.println("✅ Conversations : " + conversations.size()
                + " — autre: " + conv.getOtherUserName()
                + " — dernier msg: " + conv.getLastMessagePreview());
    }

    // ── UNREAD COUNT ──

    @Test
    @Order(25)
    @DisplayName("2.6 Compter les messages non lus — MessagingService.getUnreadCount()")
    void testCompterMessagesNonLus() {
        // USER_ID_2 a reçu un message de USER_ID_1
        int unreadCount = messagingService.getUnreadCount(USER_ID_2);

        // Vérifier : doit être >= 1
        assertTrue(unreadCount >= 1, "L'utilisateur 2 doit avoir au moins 1 message non lu");
        System.out.println("✅ Messages non lus pour user " + USER_ID_2 + " : " + unreadCount);
    }

    // ── MARK AS READ ──

    @Test
    @Order(26)
    @DisplayName("2.7 Marquer comme lu — MessagingService.markAsRead()")
    void testMarquerCommeLu() {
        // Exécuter : USER_ID_2 ouvre la conversation → marque ses messages reçus comme lus
        boolean resultat = messagingService.markAsRead(testConversationId, USER_ID_2);

        // Vérifier
        assertTrue(resultat, "markAsRead doit retourner true");

        // Relire les messages pour confirmer
        List<Message> messages = messagingService.getMessages(testConversationId, 1, 100);
        Message msg = messages.stream()
                .filter(m -> m.getId() == testMessageId)
                .findFirst().orElse(null);
        assertNotNull(msg);
        assertTrue(msg.isRead(), "Le message doit être marqué comme lu");
        System.out.println("✅ Message marqué comme lu");
    }

    // ── 4. UPDATE MESSAGE ──

    @Test
    @Order(27)
    @DisplayName("2.8 Modifier un message — MessagingService.updateMessage()")
    void testModifierMessage() {
        // Exécuter l'opération UPDATE
        boolean resultat = messagingService.updateMessage(
                testMessageId, USER_ID_1, "Message modifié par le test unitaire");

        // Vérifier
        assertTrue(resultat, "La modification doit retourner true");

        // Relire pour confirmer
        List<Message> messages = messagingService.getMessages(testConversationId, 1, 100);
        Message msg = messages.stream()
                .filter(m -> m.getId() == testMessageId)
                .findFirst().orElse(null);
        assertNotNull(msg);
        assertEquals("Message modifié par le test unitaire", msg.getContent(),
                "Le contenu doit être modifié");
        System.out.println("✅ Message modifié : " + msg.getContent());
    }

    @Test
    @Order(28)
    @DisplayName("2.9 Modifier un message par un autre utilisateur (doit échouer)")
    void testModifierMessageParAutreUtilisateur() {
        // Exécuter : USER_ID_2 essaie de modifier le message de USER_ID_1
        boolean resultat = messagingService.updateMessage(
                testMessageId, USER_ID_2, "Tentative de modification non autorisée");

        // Vérifier : la modification doit échouer (WHERE sender_id = ? ne matche pas)
        assertFalse(resultat, "La modification par un autre utilisateur doit échouer");
        System.out.println("✅ Sécurité : modification refusée pour un autre utilisateur");
    }

    // ── 5. DELETE MESSAGE ──

    @Test
    @Order(29)
    @DisplayName("2.10 Supprimer un message par un autre utilisateur (doit échouer)")
    void testSupprimerMessageParAutreUtilisateur() {
        // Exécuter : USER_ID_2 essaie de supprimer le message de USER_ID_1
        boolean resultat = messagingService.deleteMessage(testMessageId, USER_ID_2);

        // Vérifier : la suppression doit échouer
        assertFalse(resultat, "La suppression par un autre utilisateur doit échouer");
        System.out.println("✅ Sécurité : suppression refusée pour un autre utilisateur");
    }

    @Test
    @Order(30)
    @DisplayName("2.11 Supprimer un message — MessagingService.deleteMessage()")
    void testSupprimerMessage() {
        // Exécuter l'opération DELETE (par l'expéditeur)
        boolean resultat = messagingService.deleteMessage(testMessageId, USER_ID_1);

        // Vérifier
        assertTrue(resultat, "La suppression doit retourner true");

        // Confirmer : le message ne doit plus exister
        List<Message> messages = messagingService.getMessages(testConversationId, 1, 100);
        boolean existe = messages.stream().anyMatch(m -> m.getId() == testMessageId);
        assertFalse(existe, "Le message ne doit plus exister après suppression");
        System.out.println("✅ Message supprimé (ID: " + testMessageId + ")");
    }

    // ═══════════════════════════════════════════════════════════
    //  PARTIE 3 — TESTS ENTITÉS (Getters/Setters)
    // ═══════════════════════════════════════════════════════════

    @Test
    @Order(40)
    @DisplayName("3.1 Post Entity — Valeurs par défaut du constructeur")
    void testPostDefaults() {
        Post p = new Post();
        assertEquals(PostType.STATUS, p.getPostType(), "Type par défaut = STATUS");
        assertTrue(p.isPublished(), "Publié par défaut = true");
        assertEquals(0, p.getLikesCount(), "Likes par défaut = 0");
        assertEquals(0, p.getCommentsCount(), "Comments par défaut = 0");
        assertEquals(0, p.getSharesCount(), "Shares par défaut = 0");
        System.out.println("✅ Post : valeurs par défaut correctes");
    }

    @Test
    @Order(41)
    @DisplayName("3.2 Post Entity — Getters et Setters")
    void testPostGettersSetters() {
        Post p = new Post();
        p.setId(99);
        p.setAuthorId(5);
        p.setContent("Contenu test");
        p.setImageUrl("img.png");
        p.setPostType(PostType.ARTICLE_SHARE);
        p.setLikesCount(10);
        p.setCommentsCount(3);
        p.setSharesCount(1);
        p.setPublished(false);
        LocalDateTime now = LocalDateTime.now();
        p.setCreatedDate(now);
        p.setUpdatedDate(now);
        p.setAuthorName("Ahmed");
        p.setAuthorPhoto("photo.jpg");
        p.setLikedByCurrentUser(true);

        assertEquals(99, p.getId());
        assertEquals(5, p.getAuthorId());
        assertEquals("Contenu test", p.getContent());
        assertEquals("img.png", p.getImageUrl());
        assertEquals(PostType.ARTICLE_SHARE, p.getPostType());
        assertEquals(10, p.getLikesCount());
        assertEquals(3, p.getCommentsCount());
        assertEquals(1, p.getSharesCount());
        assertFalse(p.isPublished());
        assertEquals(now, p.getCreatedDate());
        assertEquals(now, p.getUpdatedDate());
        assertEquals("Ahmed", p.getAuthorName());
        assertEquals("photo.jpg", p.getAuthorPhoto());
        assertTrue(p.isLikedByCurrentUser());
        System.out.println("✅ Post : getters et setters OK");
    }

    @Test
    @Order(42)
    @DisplayName("3.3 PostComment Entity — Getters et Setters")
    void testPostCommentGettersSetters() {
        PostComment c = new PostComment();
        c.setId(10);
        c.setPostId(20);
        c.setAuthorId(30);
        c.setContent("Un commentaire");
        LocalDateTime now = LocalDateTime.now();
        c.setCreatedDate(now);
        c.setAuthorName("Nour");
        c.setAuthorPhoto("nour.jpg");

        assertEquals(10, c.getId());
        assertEquals(20, c.getPostId());
        assertEquals(30, c.getAuthorId());
        assertEquals("Un commentaire", c.getContent());
        assertEquals(now, c.getCreatedDate());
        assertEquals("Nour", c.getAuthorName());
        assertEquals("nour.jpg", c.getAuthorPhoto());
        System.out.println("✅ PostComment : getters et setters OK");
    }

    @Test
    @Order(43)
    @DisplayName("3.4 Message Entity — Getters et Setters")
    void testMessageGettersSetters() {
        Message m = new Message();
        assertFalse(m.isRead(), "isRead par défaut = false");

        m.setId(100);
        m.setConversationId(200);
        m.setSenderId(300);
        m.setContent("Salut !");
        m.setRead(true);
        LocalDateTime now = LocalDateTime.now();
        m.setCreatedDate(now);
        m.setSenderName("Ali");

        assertEquals(100, m.getId());
        assertEquals(200, m.getConversationId());
        assertEquals(300, m.getSenderId());
        assertEquals("Salut !", m.getContent());
        assertTrue(m.isRead());
        assertEquals(now, m.getCreatedDate());
        assertEquals("Ali", m.getSenderName());
        System.out.println("✅ Message : getters et setters OK");
    }

    @Test
    @Order(44)
    @DisplayName("3.5 Conversation Entity — Getters et Setters")
    void testConversationGettersSetters() {
        Conversation c = new Conversation();
        assertFalse(c.isArchived1(), "isArchived1 par défaut = false");
        assertFalse(c.isArchived2(), "isArchived2 par défaut = false");

        c.setId(1);
        c.setParticipant1(10);
        c.setParticipant2(20);
        LocalDateTime now = LocalDateTime.now();
        c.setLastMessageDate(now);
        c.setArchived1(true);
        c.setArchived2(false);
        c.setCreatedDate(now);
        c.setOtherUserName("Sara");
        c.setOtherUserPhoto("sara.jpg");
        c.setLastMessagePreview("Hello");
        c.setUnreadCount(5);

        assertEquals(1, c.getId());
        assertEquals(10, c.getParticipant1());
        assertEquals(20, c.getParticipant2());
        assertEquals(now, c.getLastMessageDate());
        assertTrue(c.isArchived1());
        assertFalse(c.isArchived2());
        assertEquals(now, c.getCreatedDate());
        assertEquals("Sara", c.getOtherUserName());
        assertEquals("sara.jpg", c.getOtherUserPhoto());
        assertEquals("Hello", c.getLastMessagePreview());
        assertEquals(5, c.getUnreadCount());
        System.out.println("✅ Conversation : getters et setters OK");
    }

    // ═══════════════════════════════════════════════════════════
    //  NETTOYAGE — @AfterAll
    // ═══════════════════════════════════════════════════════════

    @AfterAll
    static void cleanup() {
        System.out.println("\n🧹 Nettoyage : suppression des données de test...");

        try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
            // Supprimer les likes de test (au cas où)
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM post_likes WHERE post_id = ?")) {
                stmt.setInt(1, testPostId);
                stmt.executeUpdate();
            }

            // Supprimer les commentaires de test
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM post_comments WHERE post_id = ?")) {
                stmt.setInt(1, testPostId);
                stmt.executeUpdate();
            }

            // Supprimer le post de test
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM posts WHERE id = ?")) {
                stmt.setInt(1, testPostId);
                stmt.executeUpdate();
            }

            // Supprimer les messages de test dans la conversation
            try (PreparedStatement stmt = conn.prepareStatement(
                    "DELETE FROM messages WHERE conversation_id = ? AND content LIKE '%test unitaire%'")) {
                stmt.setInt(1, testConversationId);
                stmt.executeUpdate();
            }

            System.out.println("✅ Nettoyage terminé — aucune trace de test en base");

        } catch (SQLException e) {
            System.err.println("⚠️ Erreur lors du nettoyage : " + e.getMessage());
        }
    }
}
