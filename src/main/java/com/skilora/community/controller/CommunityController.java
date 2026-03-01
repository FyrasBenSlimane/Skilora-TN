package com.skilora.community.controller;

import com.skilora.framework.components.*;
import com.skilora.config.DatabaseConfig;
import com.skilora.user.entity.*;
import com.skilora.user.enums.Role;
import com.skilora.community.entity.*;
import com.skilora.community.service.*;
import com.skilora.community.enums.*;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.stage.FileChooser;
import javafx.stage.Popup;
import javafx.util.Duration;

import java.io.File;
import java.net.URL;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * CommunityController — Contrôleur principal du module Communauté.
 *
 * Architecture MVC (Modèle-Vue-Contrôleur) :
 * - Modèle : PostService, MessagingService, ConnectionService, etc. (couche
 * DAO/Service)
 * - Vue : composants JavaFX (TLCard, TLDialog, TLButton, TLTabs, etc.)
 * - Contrôleur : cette classe — gère les interactions utilisateur et la logique
 * métier
 *
 * Fonctionnalités CRUD gérées :
 * - Posts : Créer, Lire (feed/findAll), Modifier, Supprimer, Like, Commenter
 * - Messages : Créer conversation, Envoyer, Lire, Modifier, Supprimer, Marquer
 * lu
 * - Connexions : Envoyer demande, Accepter, Refuser, Supprimer, Suggestions
 * - Événements : Créer, Lire, Modifier, Supprimer, RSVP
 * - Groupes : Créer, Lire, Modifier, Supprimer, Rejoindre/Quitter
 * - Blog : Créer article, Lire, Modifier, Supprimer, Publier/Brouillon
 *
 * Contrôle de saisie (validation) :
 * - Post : contenu non vide (DialogUtils.showError si vide)
 * - Message : contenu non vide (ignoré silencieusement si vide)
 * - Commentaire : contenu non vide (ignoré si vide)
 * - Événement : titre obligatoire + format date validé
 * - Groupe : nom obligatoire
 * - Blog : titre + contenu obligatoires
 *
 * Droits par rôle :
 * ADMIN : CRUD complet sur tout le contenu. Peut supprimer n'importe quel post.
 * EMPLOYER : CRUD sur ses propres posts/événements/groupes/blogs. Messagerie.
 * TRAINER : CRUD sur ses propres posts/événements/groupes/blogs. Contenu
 * éducatif.
 * USER : CRUD sur ses propres posts. Rejoindre groupes/événements. Messagerie.
 */
public class CommunityController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CommunityController.class);
    // Format d'affichage des dates dans l'interface (jour/mois/année heure:minute)
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    // ── Composants FXML injectés depuis le fichier FXML ──
    @FXML
    private Label titleLabel; // Titre principal de la page
    @FXML
    private Label subtitleLabel; // Sous-titre
    @FXML
    private TLButton newPostBtn; // Bouton "Nouveau Post"
    @FXML
    private HBox tabBox; // Conteneur des onglets
    @FXML
    private VBox contentPane; // Zone d'affichage du contenu principal

    // ── État du contrôleur ──
    private User currentUser; // Utilisateur connecté (injecté par MainView)
    private String activeTab = "feed"; // Onglet actuellement actif
    private TLTabs communityTabs; // Composant des onglets avec badges
    private CommunityNotificationService notificationService; // Service de notifications temps réel

    // ── Services des nouvelles fonctionnalités (Sprint 2) ──
    private final TranslationService translationService = TranslationService.getInstance();
    private final CloudinaryUploadService cloudinaryService = CloudinaryUploadService.getInstance();
    private final MentionService mentionService = MentionService.getInstance();
    private final SearchService searchService = SearchService.getInstance();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // L'initialisation est différée — les onglets sont configurés après
        // réception du contexte utilisateur via initializeContext()
    }

    /**
     * Initialise le contrôleur avec le contexte de l'utilisateur connecté.
     * Appelée par MainView après le chargement du FXML.
     * Configure le titre, les onglets, les notifications et charge le feed.
     *
     * @param user l'utilisateur actuellement connecté
     */
    public void initializeContext(User user) {
        this.currentUser = user;
        titleLabel.setText(I18n.get("community.title")); // Titre i18n
        subtitleLabel.setText(I18n.get("community.subtitle")); // Sous-titre i18n

        // Afficher le bouton « Nouveau Post » pour tous les rôles
        newPostBtn.setVisible(true);
        newPostBtn.setManaged(true);

        setupTabs(); // Créer les 6 onglets
        startRealTimeNotifications(); // Démarrer le poller de notifications

        // ── Démarrer le heartbeat de présence en ligne ──
        OnlineStatusService.getInstance().startHeartbeat(currentUser.getId());

        loadFeedTab(); // Charger le fil d'actualité par défaut
    }

    /**
     * Démarre le service de notifications en temps réel.
     * Utilise un poller qui interroge la base toutes les 6 secondes.
     * Met à jour les badges des onglets et affiche des toasts
     * quand de nouveaux messages ou invitations arrivent.
     */
    private void startRealTimeNotifications() {
        // Arrêter l'ancien service s'il existe (éviter les doublons)
        if (notificationService != null) {
            notificationService.stop();
        }

        // Créer un nouveau poller avec un intervalle de 6 secondes
        notificationService = new CommunityNotificationService(currentUser.getId(), 6);

        // Callback : mise à jour du badge de l'onglet Messages + toast
        notificationService.setOnUnreadMessagesChanged((oldCount, newCount) -> {
            if (communityTabs != null) {
                communityTabs.setTabBadge("messages", newCount); // Mise à jour du badge
            }
            // Afficher un toast uniquement pour les NOUVEAUX messages (pas au premier poll)
            if (oldCount >= 0 && newCount > oldCount) {
                int diff = newCount - oldCount;
                showToast("\uD83D\uDCE9  " + I18n.get("notification.new_messages", diff));
                // Rafraîchir automatiquement si l'onglet messages est actif
                if ("messages".equals(activeTab)) {
                    loadMessagesTab();
                }
            }
        });

        // Callback : mise à jour du badge de l'onglet Connexions + toast
        notificationService.setOnPendingConnectionsChanged((oldCount, newCount) -> {
            if (communityTabs != null) {
                communityTabs.setTabBadge("connections", newCount); // Mise à jour du badge
            }
            if (oldCount >= 0 && newCount > oldCount) {
                int diff = newCount - oldCount;
                showToast("\uD83D\uDD14  " + I18n.get("notification.new_invitations", diff));
                // Rafraîchir automatiquement si l'onglet connexions est actif
                if ("connections".equals(activeTab)) {
                    loadConnectionsTab();
                }
            }
        });

        notificationService.start();
        logger.info("Real-time notifications started for user {}", currentUser.getId());
    }

    /**
     * Arrête le poller de notifications (appelé quand on quitte la vue communauté).
     */
    public void stopRealTimeNotifications() {
        if (notificationService != null) {
            notificationService.stop();
            notificationService = null;
        }
        // Arrêter le heartbeat de présence en ligne
        OnlineStatusService.getInstance().stopHeartbeat();
    }

    // ═══════════════════════════════════════════════════════════
    // CONFIGURATION DES ONGLETS — 6 onglets avec badges
    // ═══════════════════════════════════════════════════════════

    /**
     * Crée les 6 onglets du module communauté :
     * Feed, Connexions, Messages, Événements, Groupes, Blog.
     * Chaque onglet peut avoir un badge (nombre de notifications).
     */
    private void setupTabs() {
        tabBox.getChildren().clear();
        communityTabs = new TLTabs();
        communityTabs.addTab("feed", I18n.get("community.tab.feed"), (Node) null);
        communityTabs.addTab("connections", I18n.get("community.tab.connections"), (Node) null);
        communityTabs.addTab("messages", I18n.get("community.tab.messages"), (Node) null);
        communityTabs.addTab("events", I18n.get("community.tab.events"), (Node) null);
        communityTabs.addTab("groups", I18n.get("community.tab.groups"), (Node) null);
        communityTabs.addTab("blog", I18n.get("blog.title"), (Node) null);

        communityTabs.setOnTabChanged(this::onTabChanged);
        tabBox.getChildren().add(communityTabs);
        HBox.setHgrow(communityTabs, Priority.ALWAYS);
    }

    /**
     * Gestionnaire de changement d'onglet.
     * Switch vers la méthode de chargement correspondante.
     */
    private void onTabChanged(String tabId) {
        activeTab = tabId;
        switch (tabId) {
            case "feed" -> loadFeedTab(); // Fil d'actualité
            case "connections" -> loadConnectionsTab(); // Réseau de connexions
            case "messages" -> loadMessagesTab(); // Messagerie
            case "events" -> loadEventsTab(); // Événements
            case "groups" -> loadGroupsTab(); // Groupes
            case "blog" -> loadBlogTab(); // Articles de blog
        }
    }

    // ═══════════════════════════════════════════════════════════
    // VÉRIFICATION DES RÔLES — Contrôle d'accès
    // ═══════════════════════════════════════════════════════════

    /** Vérifie si l'utilisateur connecté est un Administrateur */
    private boolean isAdmin() {
        return currentUser != null && currentUser.getRole() == Role.ADMIN;
    }

    /** Vérifie si l'utilisateur connecté est un Employeur */
    private boolean isEmployer() {
        return currentUser != null && currentUser.getRole() == Role.EMPLOYER;
    }

    /** Vérifie si l'utilisateur connecté est un Formateur */
    private boolean isTrainer() {
        return currentUser != null && currentUser.getRole() == Role.TRAINER;
    }

    /**
     * Vérifie si l'utilisateur peut modifier ou supprimer un contenu.
     * L'Admin peut tout modifier. Les autres ne peuvent modifier que leur propre
     * contenu.
     *
     * @param authorId l'ID de l'auteur du contenu
     * @return true si l'utilisateur a le droit de modifier/supprimer
     */
    private boolean canEditOrDelete(int authorId) {
        if (currentUser == null)
            return false;
        return isAdmin() || currentUser.getId() == authorId; // Admin OU propriétaire
    }

    /** Vérifie si le rôle peut créer des événements (Admin, Employer, Trainer) */
    private boolean canCreateEvents() {
        return isAdmin() || isEmployer() || isTrainer();
    }

    /** Vérifie si le rôle peut créer des groupes (tous les rôles) */
    private boolean canCreateGroups() {
        return true; // Tous les rôles peuvent créer des groupes
    }

    /**
     * Vérifie si le rôle peut créer des articles de blog (Admin, Employer, Trainer)
     */
    private boolean canCreateBlog() {
        return isAdmin() || isEmployer() || isTrainer();
    }

    // ═══════════════════════════════════════════════════════════
    // POSTS / FEED — CRUD complet (Créer, Lire, Modifier, Supprimer)
    // ═══════════════════════════════════════════════════════════

    /** Gestionnaire du bouton FXML « Nouveau Post » */
    @FXML
    private void handleNewPost() {
        if (currentUser == null)
            return; // Sécurité : utilisateur non connecté
        showPostDialog(null); // null = création (pas de post existant)
    }

    /**
     * Affiche le dialogue de création ou modification d'un post.
     * Si existingPost est null → mode création.
     * Si existingPost n'est pas null → mode modification (préremplit les champs).
     *
     * CONTRÔLE DE SAISIE :
     * - Le contenu ne doit pas être vide (sinon DialogUtils.showError)
     * - L'URL image est optionnelle
     *
     * @param existingPost le post à modifier, ou null pour créer
     */
    private void showPostDialog(Post existingPost) {
        TLDialog<Void> dialog = new TLDialog<>();
        boolean isEdit = existingPost != null; // Déterminer le mode : création ou modification
        dialog.setDialogTitle(isEdit ? I18n.get("post.edit") : I18n.get("post.new"));
        dialog.setDescription(I18n.get("post.new.description"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextarea textArea = new TLTextarea("", I18n.get("post.placeholder"));
        textArea.getControl().setPrefRowCount(5);
        if (isEdit) {
            textArea.setText(existingPost.getContent());
        }

        TLTextField imageUrlField = new TLTextField(I18n.get("post.image_url"), "https://...");
        if (isEdit && existingPost.getImageUrl() != null) {
            imageUrlField.setText(existingPost.getImageUrl());
        }

        // ── UPLOAD CLOUDINARY — Bouton pour téléverser une image via l'API Cloudinary
        // ──
        TLButton uploadBtn = new TLButton("📷  Upload Image", TLButton.ButtonVariant.OUTLINE);
        uploadBtn.setSize(TLButton.ButtonSize.SM);
        uploadBtn.setOnAction(ev -> {
            // Ouvrir un sélecteur de fichiers (FileChooser) filtré par images
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Choisir une image");
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"));
            File file = fileChooser.showOpenDialog(dialog.getDialogPane().getScene().getWindow());
            if (file != null) {
                uploadBtn.setText("⏳  Upload en cours...");
                uploadBtn.setDisable(true);
                // Uploader dans un thread séparé (appel réseau vers Cloudinary)
                new Thread(() -> {
                    try {
                        String url = cloudinaryService.uploadImage(file); // Appel API Cloudinary
                        Platform.runLater(() -> {
                            imageUrlField.setText(url); // Remplir le champ URL
                            // Vérifier si c'est un upload cloud ou un fallback local
                            if (url != null && url.startsWith("file:")) {
                                uploadBtn.setText("💾  Image sauvegardée");
                                showToast("Image sauvegardée localement (mode hors-ligne)");
                            } else {
                                uploadBtn.setText("✅  Image uploadée");
                                showToast("Image uploadée sur le cloud !");
                            }
                            uploadBtn.setDisable(false);
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            uploadBtn.setText("📷  Upload Image");
                            uploadBtn.setDisable(false);
                            DialogUtils.showError("Erreur Upload", ex.getMessage());
                        });
                    }
                }, "CloudinaryUploadThread").start();
            }
        });

        // ── EMOJI PICKER — Grille d'emojis pour enrichir le contenu ──
        TLButton emojiBtn = new TLButton("😀  Emoji", TLButton.ButtonVariant.GHOST);
        emojiBtn.setSize(TLButton.ButtonSize.SM);
        emojiBtn.setOnAction(ev -> showEmojiPicker(emojiBtn, textArea));

        // ── MENTIONS @USER — Détection de @ et autocomplétion ──
        // Ajouter un écouteur sur le texte pour détecter les saisies @
        setupMentionDetection(textArea, dialog);

        HBox toolBar = new HBox(8, uploadBtn, emojiBtn);
        toolBar.setAlignment(Pos.CENTER_LEFT);

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton saveBtn = new TLButton(isEdit ? I18n.get("common.save") : I18n.get("post.publish"),
                TLButton.ButtonVariant.PRIMARY);
        saveBtn.setOnAction(e -> {
            String text = textArea.getText();
            String imageUrl = imageUrlField.getText();
            boolean hasText = text != null && !text.trim().isEmpty();
            boolean hasImage = imageUrl != null && !imageUrl.trim().isEmpty();

            // ═══ CONTRÔLE DE SAISIE : texte OU image requis (pas les deux obligatoires)
            // ═══
            if (!hasText && !hasImage) {
                DialogUtils.showError(I18n.get("message.error"),
                        "Veuillez saisir un texte ou uploader une image.");
                return; // Bloquer la soumission
            }

            String finalText = hasText ? text.trim() : "";
            if (isEdit) {
                // Mode MODIFICATION : mettre à jour le post existant
                existingPost.setContent(finalText);
                existingPost.setImageUrl(imageUrl);
                updatePost(existingPost);
            } else {
                // Mode CRÉATION : créer un nouveau post (texte optionnel si image présente)
                createPost(finalText, imageUrl);
            }
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);
        content.getChildren().addAll(textArea, toolBar, imageUrlField, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    /**
     * OPÉRATION CREATE — Crée un nouveau post dans un thread séparé.
     * Utilise JavaFX Task pour ne pas bloquer le thread UI.
     * Après succès : affiche un toast et recharge le feed.
     *
     * @param text     le contenu du post (déjà validé et trimmé)
     * @param imageUrl l'URL de l'image (optionnelle)
     */
    private void createPost(String text, String imageUrl) {
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() {
                // Construire l'objet Post avec les données de l'utilisateur
                Post post = new Post();
                post.setAuthorId(currentUser.getId());
                post.setContent(text);
                post.setImageUrl(imageUrl != null && !imageUrl.isBlank() ? imageUrl : null);
                // Appeler le service pour insérer en base de données
                return PostService.getInstance().create(post);
            }
        };
        // Callback après succès (exécuté sur le thread JavaFX)
        task.setOnSucceeded(e -> {
            if (task.getValue() > 0) { // ID > 0 = succès
                logger.info("Post created successfully");
                // MENTIONS : traiter les @mentions et créer des notifications
                int postId = task.getValue();
                new Thread(() -> {
                    try {
                        mentionService.processMentions(text, currentUser.getId(), postId);
                    } catch (Exception ex) {
                        logger.warn("Mention processing failed: {}", ex.getMessage());
                    }
                }, "MentionProcessThread").start();
                showToast(I18n.get("post.success.created"));
                loadFeedTab(); // Recharger le feed pour afficher le nouveau post
            }
        });
        task.setOnFailed(e -> logger.error("Failed to create post", task.getException()));
        new Thread(task, "CreatePostThread").start(); // Lancer dans un thread séparé
    }

    /**
     * OPÉRATION UPDATE — Met à jour un post dans un thread séparé.
     * Après succès : affiche un toast et recharge le feed.
     *
     * @param post l'objet Post avec les données modifiées
     */
    private void updatePost(Post post) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() {
                return PostService.getInstance().update(post); // Appel au service UPDATE
            }
        };
        task.setOnSucceeded(e -> {
            if (task.getValue()) { // true = mise à jour réussie
                logger.info("Post updated successfully");
                showToast(I18n.get("post.success.created"));
                loadFeedTab(); // Recharger le feed
            }
        });
        new Thread(task, "UpdatePostThread").start();
    }

    /**
     * OPÉRATION DELETE — Supprime un post après confirmation de l'utilisateur.
     * Affiche une boîte de dialogue de confirmation avant la suppression.
     *
     * @param post le post à supprimer
     */
    private void deletePost(Post post) {
        // Afficher une confirmation avant de supprimer
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) { // L'utilisateur a confirmé
                        Task<Boolean> task = new Task<>() {
                            @Override
                            protected Boolean call() {
                                return PostService.getInstance().delete(post.getId()); // Appel au service DELETE
                            }
                        };
                        task.setOnSucceeded(e -> {
                            if (task.getValue()) { // Suppression réussie
                                logger.info("Post deleted");
                                loadFeedTab(); // Recharger le feed
                            }
                        });
                        new Thread(task, "DeletePostThread").start();
                    }
                });
    }

    /**
     * OPÉRATION READ — Charge le fil d'actualité (feed).
     * - Admin : voit TOUS les posts (findAll)
     * - Autres rôles : voit ses posts + ceux de ses connexions (getFeed)
     */
    private void loadFeedTab() {
        contentPane.getChildren().clear();
        if (currentUser == null)
            return;

        // ── RECHERCHE AVANCÉE — Barre de recherche globale en haut du feed ──
        contentPane.getChildren().add(buildSearchBar());

        // ── TRI DES POSTS — Plus récent / Plus ancien ──
        HBox sortBar = new HBox(10);
        sortBar.setAlignment(Pos.CENTER_LEFT);
        sortBar.setPadding(new Insets(0, 0, 8, 0));
        Label sortLabel = new Label("Trier par :");
        sortLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");
        TLSelect<String> feedSortSelect = new TLSelect<>("",
                "Plus récent", "Plus ancien");
        feedSortSelect.setValue("Plus récent");
        sortBar.getChildren().addAll(sortLabel, feedSortSelect);
        contentPane.getChildren().add(sortBar);

        // Conteneur dédié pour les posts (rechargeable par le tri)
        VBox postsContainer = new VBox();
        contentPane.getChildren().add(postsContainer);

        // Chargement asynchrone pour ne pas bloquer l'interface
        Task<List<Post>> task = new Task<>() {
            @Override
            protected List<Post> call() {
                if (isAdmin()) {
                    return PostService.getInstance().findAll(); // Admin : tous les posts
                }
                return PostService.getInstance().getFeed(currentUser.getId(), 1, 50); // Feed filtré
            }
        };

        task.setOnSucceeded(e -> {
            List<Post> posts = task.getValue();

            // Afficher les posts avec le tri par défaut
            displaySortedPosts(postsContainer, posts, feedSortSelect.getValue());

            // Re-trier automatiquement quand l'utilisateur change le tri
            feedSortSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
                displaySortedPosts(postsContainer, posts, newVal);
            });
        });
        new Thread(task, "LoadFeedThread").start();
    }

    /**
     * Affiche les posts triés dans le conteneur.
     * Trie par date de création : "Plus récent" (DESC) ou "Plus ancien" (ASC).
     *
     * @param container le VBox conteneur des posts
     * @param posts     la liste de posts à afficher
     * @param sortVal   "Plus récent" ou "Plus ancien"
     */
    private void displaySortedPosts(VBox container, List<Post> posts, String sortVal) {
        container.getChildren().clear();

        // Role info header
        TLBadge roleBadge = new TLBadge(currentUser.getRole().getDisplayName(), TLBadge.Variant.DEFAULT);
        HBox roleHeader = new HBox(8, roleBadge);
        if (isAdmin()) {
            Label adminLabel = new Label("Viewing all posts (Admin)");
            adminLabel.getStyleClass().add("text-muted");
            roleHeader.getChildren().add(adminLabel);
        }
        roleHeader.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().add(roleHeader);

        if (posts.isEmpty()) {
            Label empty = new Label(I18n.get("post.empty"));
            empty.getStyleClass().add("text-muted");
            container.getChildren().add(empty);
            return;
        }

        // Copier la liste pour la trier sans modifier l'originale
        List<Post> sorted = new java.util.ArrayList<>(posts);
        boolean ascending = "Plus ancien".equals(sortVal);
        sorted.sort((a, b) -> {
            LocalDateTime da = a.getCreatedDate();
            LocalDateTime db = b.getCreatedDate();
            if (da == null && db == null)
                return 0;
            if (da == null)
                return 1;
            if (db == null)
                return -1;
            return ascending ? da.compareTo(db) : db.compareTo(da);
        });

        int delay = 0;
        for (Post post : sorted) {
            TLCard card = createPostCard(post);
            container.getChildren().add(card);
            // ANIMATION : Entrée en fondu + glissement de chaque carte
            animateCardEntry(card, delay);
            delay += 80; // Décalage de 80ms entre chaque carte (effet cascade)
        }
    }

    /**
     * Construit la carte visuelle d'un post (style réseau social).
     * Contient : avatar, nom auteur, badge type, contenu, boutons like/commentaire,
     * et les boutons Modifier/Supprimer si l'utilisateur a les droits.
     *
     * @param post le post à afficher
     * @return TLCard contenant tout le contenu du post
     */
    private TLCard createPostCard(Post post) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        VBox body = new VBox(10);
        body.setPadding(new Insets(20));

        // ── En-tête : Avatar + Infos auteur + Badge type de post ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        String authorName = post.getAuthorName() != null ? post.getAuthorName() : "Unknown";
        StackPane avatar = createAvatar(authorName); // Avatar circulaire avec initiales

        VBox authorInfo = new VBox(2);
        Label author = new Label(authorName); // Nom de l'auteur en gras
        author.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        Label date = new Label(formatDate(post.getCreatedDate())); // Date de création
        date.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        authorInfo.getChildren().addAll(author, date);
        HBox.setHgrow(authorInfo, Priority.ALWAYS);

        TLBadge typeBadge = new TLBadge(post.getPostType().name(), TLBadge.Variant.SECONDARY); // Badge type

        header.getChildren().addAll(avatar, authorInfo, typeBadge);

        // ── Contenu du post ──
        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-foreground;");

        body.getChildren().addAll(header, contentLabel);

        // Affichage de l'image du post si présente (Cloudinary ou fichier local)
        if (post.getImageUrl() != null && !post.getImageUrl().isBlank()) {
            try {
                String imgUrl = post.getImageUrl();
                Image image = new Image(imgUrl, 500, 0, true, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setPreserveRatio(true);
                imageView.setFitWidth(500);
                imageView.setSmooth(true);
                // Coins arrondis sur l'image
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(500, 350);
                clip.setArcWidth(16);
                clip.setArcHeight(16);
                imageView.setClip(clip);
                // Adapter le clip à la taille réelle de l'image une fois chargée
                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && image.getHeight() > 0) {
                        double ratio = 500.0 / image.getWidth();
                        double displayHeight = Math.min(image.getHeight() * ratio, 350);
                        clip.setHeight(displayHeight);
                    }
                });
                imageView.setStyle("-fx-cursor: hand;");
                body.getChildren().add(imageView);
            } catch (Exception imgEx) {
                // Fallback : afficher le lien si l'image ne charge pas
                Label imgLabel = new Label("📷  Image non disponible");
                imgLabel.getStyleClass().add("text-muted");
                imgLabel.setStyle("-fx-font-size: 12px; -fx-font-style: italic;");
                body.getChildren().add(imgLabel);
            }
        }

        // ── Barre d'actions (Like, Commenter, Modifier, Supprimer) ──
        body.getChildren().add(new TLSeparator());

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        // Bouton LIKE avec compteur — bascule entre PRIMARY (aimé) et GHOST (pas aimé)
        TLButton likeBtn = new TLButton("♥  " + I18n.get("post.like") + " (" + post.getLikesCount() + ")",
                post.isLikedByCurrentUser() ? TLButton.ButtonVariant.PRIMARY : TLButton.ButtonVariant.GHOST);
        likeBtn.setSize(TLButton.ButtonSize.SM);
        likeBtn.setOnAction(e -> {
            // Toggle like dans un thread séparé puis recharger le feed
            Task<Boolean> likeTask = new Task<>() {
                @Override
                protected Boolean call() {
                    return PostService.getInstance().toggleLike(post.getId(), currentUser.getId());
                }
            };
            likeTask.setOnSucceeded(ev -> loadFeedTab());
            new Thread(likeTask, "LikeThread").start();
        });

        // Section commentaires — toggle afficher/masquer (style Instagram)
        VBox commentsSection = new VBox(8);
        commentsSection.setVisible(false);
        commentsSection.setManaged(false);
        commentsSection.setPadding(new Insets(8, 0, 0, 0));

        // Bouton COMMENTAIRE avec compteur — toggle la section de commentaires
        TLButton commentBtn = new TLButton("💬  " + I18n.get("post.comment") + " (" + post.getCommentsCount() + ")",
                TLButton.ButtonVariant.GHOST);
        commentBtn.setSize(TLButton.ButtonSize.SM);
        commentBtn.setOnAction(e -> {
            boolean show = !commentsSection.isVisible(); // Basculer la visibilité
            commentsSection.setVisible(show);
            commentsSection.setManaged(show);
            if (show) {
                loadInlineComments(post, commentsSection); // Charger les commentaires si ouvert
            }
        });

        actions.getChildren().addAll(likeBtn, commentBtn);

        // ── BOUTON TRADUCTION — API MyMemory avec choix de la langue ──
        // Affiche un menu popup pour choisir la langue cible avant de traduire
        TLButton translateBtn = new TLButton("🌐  Traduire", TLButton.ButtonVariant.GHOST);
        translateBtn.setSize(TLButton.ButtonSize.SM);
        final String originalPostText = post.getContent(); // Sauvegarder le texte original
        translateBtn.setOnAction(e -> showTranslationMenu(translateBtn, contentLabel, originalPostText));
        actions.getChildren().add(translateBtn);

        // Boutons Modifier/Supprimer — sur une ligne séparée, alignés à droite
        // Visibles seulement si l'utilisateur est propriétaire du post ou admin
        if (canEditOrDelete(post.getAuthorId())) {
            HBox editDeleteRow = new HBox(8);
            editDeleteRow.setAlignment(Pos.CENTER_RIGHT);
            editDeleteRow.setPadding(new Insets(4, 0, 0, 0));

            TLButton editBtn = new TLButton(I18n.get("post.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setSize(TLButton.ButtonSize.SM);
            editBtn.setOnAction(e -> showPostDialog(post)); // Ouvrir le dialogue en mode édition

            TLButton deleteBtn = new TLButton(I18n.get("post.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setSize(TLButton.ButtonSize.SM);
            deleteBtn.setOnAction(e -> deletePost(post)); // Supprimer avec confirmation

            editDeleteRow.getChildren().addAll(editBtn, deleteBtn);
            body.getChildren().add(actions);
            body.getChildren().add(editDeleteRow);
            body.getChildren().add(commentsSection);
            card.setContent(body);
            return card;
        }

        body.getChildren().addAll(actions, commentsSection);
        card.setContent(body);
        return card;
    }

    /**
     * Charge les commentaires en ligne sous un post (style Instagram/Facebook).
     * - Champ de saisie en haut pour ajouter un commentaire
     * - Liste des commentaires existants avec avatar, auteur, date
     * - CONTRÔLE DE SAISIE : le texte ne doit pas être vide
     *
     * @param post            le post parent
     * @param commentsSection le conteneur VBox des commentaires
     */
    private void loadInlineComments(Post post, VBox commentsSection) {
        commentsSection.getChildren().clear();

        // Champ de saisie de commentaire en haut (style Instagram)
        HBox inputBox = new HBox(8);
        inputBox.setAlignment(Pos.CENTER_LEFT);
        TLTextField commentInput = new TLTextField("", I18n.get("post.comment") + "...");
        HBox.setHgrow(commentInput, Priority.ALWAYS);

        // ── MENTIONS @USER dans les commentaires — autocomplétion ──
        setupMentionDetectionForTextField(commentInput);

        TLButton sendBtn = new TLButton(I18n.get("message.send"), TLButton.ButtonVariant.PRIMARY);
        sendBtn.setSize(TLButton.ButtonSize.SM);
        sendBtn.setOnAction(e -> {
            String text = commentInput.getText();
            // CONTRÔLE DE SAISIE : vérifier que le commentaire n'est pas vide
            if (text != null && !text.trim().isEmpty()) {
                // Construire l'objet commentaire
                PostComment comment = new PostComment();
                comment.setPostId(post.getId());
                comment.setAuthorId(currentUser.getId());
                comment.setContent(text.trim());
                commentInput.setText(""); // Vider le champ après envoi
                // Ajouter le commentaire dans un thread séparé
                new Thread(() -> {
                    PostService.getInstance().addComment(comment);
                    Platform.runLater(() -> loadInlineComments(post, commentsSection)); // Recharger
                }, "AddCommentThread").start();
            }
        });
        inputBox.getChildren().addAll(commentInput, sendBtn);
        commentsSection.getChildren().add(inputBox);

        // Indicateur de chargement
        Label loadingLabel = new Label(I18n.get("message.loading"));
        loadingLabel.getStyleClass().add("text-muted");
        commentsSection.getChildren().add(loadingLabel);

        // Charger les commentaires depuis la base de données
        Task<List<PostComment>> commentsTask = new Task<>() {
            @Override
            protected List<PostComment> call() {
                return PostService.getInstance().getComments(post.getId());
            }
        };
        commentsTask.setOnSucceeded(e -> {
            List<PostComment> comments = commentsTask.getValue();
            Platform.runLater(() -> {
                commentsSection.getChildren().remove(loadingLabel);

                if (comments.isEmpty()) {
                    Label empty = new Label(I18n.get("post.empty"));
                    empty.getStyleClass().add("text-muted");
                    empty.setStyle("-fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                    commentsSection.getChildren().add(empty);
                    return;
                }

                for (PostComment c : comments) {
                    commentsSection.getChildren().add(createCommentRow(post, c, commentsSection));
                }
            });
        });
        new Thread(commentsTask, "LoadCommentsThread").start();
    }

    /**
     * Crée une ligne de commentaire avec avatar, auteur, texte, date,
     * et boutons Modifier/Supprimer (visibles si propriétaire ou admin).
     *
     * @param post            le post parent
     * @param comment         le commentaire à afficher
     * @param commentsSection le conteneur pour recharger après modification
     * @return VBox représentant la ligne du commentaire
     */
    private VBox createCommentRow(Post post, PostComment comment, VBox commentsSection) {
        VBox commentBox = new VBox(4);
        commentBox.getStyleClass().add("comment-row");

        HBox contentRow = new HBox(10);
        contentRow.setAlignment(Pos.TOP_LEFT);

        // Mini avatar (28px) pour le commentaire
        String commentAuthor = comment.getAuthorName() != null ? comment.getAuthorName() : "Unknown";
        StackPane commentAvatar = createAvatar(commentAuthor, 28);

        VBox textColumn = new VBox(2);
        HBox.setHgrow(textColumn, Priority.ALWAYS);

        // Nom de l'auteur + contenu du commentaire
        Label authorLabel = new Label(commentAuthor);
        authorLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        authorLabel.setMinWidth(Region.USE_PREF_SIZE);

        Label contentLabel = new Label(comment.getContent()); // Contenu du commentaire
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");

        // Horodatage + boutons d'actions
        HBox metaRow = new HBox(8);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label timeLabel = new Label(formatDate(comment.getCreatedDate())); // Date du commentaire
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: -fx-muted-foreground;");
        metaRow.getChildren().add(timeLabel);

        // Boutons Modifier/Supprimer — visibles si propriétaire ou admin
        if (canEditOrDelete(comment.getAuthorId())) {
            // Bouton MODIFIER le commentaire
            TLButton editCommentBtn = new TLButton(I18n.get("post.edit"), TLButton.ButtonVariant.GHOST);
            editCommentBtn.setSize(TLButton.ButtonSize.SM);
            editCommentBtn.setStyle("-fx-font-size: 10px; -fx-padding: 0 4;");
            editCommentBtn.setOnAction(ev -> showEditCommentInline(post, comment, commentBox, commentsSection));

            // Bouton SUPPRIMER le commentaire
            TLButton delCommentBtn = new TLButton(I18n.get("post.delete"), TLButton.ButtonVariant.GHOST);
            delCommentBtn.setSize(TLButton.ButtonSize.SM);
            delCommentBtn.setStyle("-fx-font-size: 10px; -fx-padding: 0 4; -fx-text-fill: -fx-destructive;");
            delCommentBtn.setOnAction(ev -> {
                // Supprimer dans un thread séparé puis recharger les commentaires
                new Thread(() -> {
                    PostService.getInstance().deleteComment(comment.getId(), post.getId());
                    Platform.runLater(() -> loadInlineComments(post, commentsSection));
                }, "DeleteCommentThread").start();
            });

            metaRow.getChildren().addAll(editCommentBtn, delCommentBtn);
        }

        textColumn.getChildren().addAll(authorLabel, contentLabel, metaRow);
        contentRow.getChildren().addAll(commentAvatar, textColumn);
        commentBox.getChildren().add(contentRow);
        return commentBox;
    }

    /**
     * Remplace la ligne du commentaire par un champ d'édition en ligne.
     * CONTRÔLE DE SAISIE : le nouveau texte ne doit pas être vide.
     * Boutons Sauvegarder et Annuler.
     *
     * @param post            le post parent
     * @param comment         le commentaire à modifier
     * @param commentBox      le conteneur du commentaire à remplacer
     * @param commentsSection le conteneur global pour recharger
     */
    private void showEditCommentInline(Post post, PostComment comment, VBox commentBox, VBox commentsSection) {
        commentBox.getChildren().clear(); // Vider le contenu actuel

        HBox editRow = new HBox(8);
        editRow.setAlignment(Pos.CENTER_LEFT);
        editRow.setPadding(new Insets(4, 0, 4, 12));

        // Champ de texte pré-rempli avec le contenu actuel
        TLTextField editField = new TLTextField("", "");
        editField.setText(comment.getContent()); // Pré-remplir
        HBox.setHgrow(editField, Priority.ALWAYS);

        // Bouton SAUVEGARDER
        TLButton saveBtn = new TLButton(I18n.get("common.save"), TLButton.ButtonVariant.PRIMARY);
        saveBtn.setSize(TLButton.ButtonSize.SM);
        saveBtn.setOnAction(ev -> {
            String newText = editField.getText();
            // CONTRÔLE DE SAISIE : vérifier que le texte n'est pas vide
            if (newText != null && !newText.trim().isEmpty()) {
                new Thread(() -> {
                    PostService.getInstance().updateComment(comment.getId(), newText.trim()); // UPDATE
                    Platform.runLater(() -> loadInlineComments(post, commentsSection)); // Recharger
                }, "UpdateCommentThread").start();
            }
        });

        // Bouton ANNULER — recharger les commentaires sans modifier
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.GHOST);
        cancelBtn.setSize(TLButton.ButtonSize.SM);
        cancelBtn.setOnAction(ev -> loadInlineComments(post, commentsSection));

        editRow.getChildren().addAll(editField, saveBtn, cancelBtn);
        commentBox.getChildren().add(editRow);
    }

    // ═══════════════════════════════════════════════════════════
    // CONNEXIONS — CRUD avec gestion par rôle
    // (Demandes en attente, Connexions acceptées, Suggestions)
    // ═══════════════════════════════════════════════════════════

    /**
     * Charge l'onglet Connexions avec 3 sections :
     * 1. Demandes en attente (pending) avec boutons Accepter/Refuser
     * 2. Connexions acceptées avec boutons Message/Supprimer
     * 3. Suggestions de connexions avec bouton Envoyer demande
     * Chaque section est chargée de manière asynchrone.
     */
    private void loadConnectionsTab() {
        contentPane.getChildren().clear();
        if (currentUser == null)
            return;

        VBox connectionsPane = new VBox(16);

        // Tâche 1 : charger les demandes en attente
        Task<List<Connection>> pendingTask = new Task<>() {
            @Override
            protected List<Connection> call() {
                return ConnectionService.getInstance().getPendingRequests(currentUser.getId());
            }
        };

        // Tâche 2 : charger les connexions acceptées
        Task<List<Connection>> connectionsTask = new Task<>() {
            @Override
            protected List<Connection> call() {
                return ConnectionService.getInstance().getConnections(currentUser.getId());
            }
        };

        // Tâche 3 : charger les suggestions de connexions
        Task<List<Connection>> suggestionsTask = new Task<>() {
            @Override
            protected List<Connection> call() {
                return ConnectionService.getInstance().getSuggestions(currentUser.getId(), 5);
            }
        };

        pendingTask.setOnSucceeded(e1 -> {
            List<Connection> pending = pendingTask.getValue();
            if (!pending.isEmpty()) {
                Label pendingLabel = new Label("⏳  " + I18n.get("connection.pending") + " (" + pending.size() + ")");
                pendingLabel.getStyleClass().add("community-section-title");
                connectionsPane.getChildren().add(pendingLabel);

                for (Connection p : pending) {
                    connectionsPane.getChildren().add(createPendingCard(p));
                }
                connectionsPane.getChildren().add(new TLSeparator());
            }

            // Now load accepted connections
            connectionsTask.setOnSucceeded(e2 -> {
                List<Connection> connections = connectionsTask.getValue();
                Label countLabel = new Label("🤝  " + I18n.get("connection.count", connections.size()));
                countLabel.getStyleClass().add("community-section-title");
                connectionsPane.getChildren().add(countLabel);

                if (connections.isEmpty()) {
                    Label empty = new Label(I18n.get("connection.empty"));
                    empty.getStyleClass().add("text-muted");
                    connectionsPane.getChildren().add(empty);
                } else {
                    for (Connection conn : connections) {
                        connectionsPane.getChildren().add(createConnectionCard(conn));
                    }
                }

                // Suggestions section
                suggestionsTask.setOnSucceeded(e3 -> {
                    List<Connection> suggestions = suggestionsTask.getValue();
                    if (!suggestions.isEmpty()) {
                        connectionsPane.getChildren().add(new TLSeparator());
                        Label sugLabel = new Label("💡  " + I18n.get("connection.suggestions"));
                        sugLabel.getStyleClass().add("community-section-title");
                        connectionsPane.getChildren().add(sugLabel);

                        for (Connection s : suggestions) {
                            connectionsPane.getChildren().add(createSuggestionCard(s));
                        }
                    }
                });
                new Thread(suggestionsTask, "SuggestionsThread").start();
            });
            new Thread(connectionsTask, "ConnectionsThread").start();
        });
        new Thread(pendingTask, "PendingThread").start();

        contentPane.getChildren().add(connectionsPane);
    }

    /**
     * Crée une carte pour une demande de connexion en attente.
     * Affiche le nom de l'utilisateur avec les boutons Accepter et Refuser.
     *
     * @param conn la connexion en attente
     * @return TLCard avec les boutons d'action
     */
    private TLCard createPendingCard(Connection conn) {
        TLCard card = new TLCard();
        card.getStyleClass().add("community-card");
        HBox content = new HBox(14);
        content.setPadding(new Insets(16));
        content.setAlignment(Pos.CENTER_LEFT);

        String userName = conn.getOtherUserName() != null ? conn.getOtherUserName() : "User #" + conn.getUserId1();
        StackPane avatar = createAvatar(userName);

        VBox info = new VBox(2);
        Label name = new Label(userName);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        Label statusLabel = new Label("⏳  " + I18n.get("connection.pending"));
        statusLabel.getStyleClass().add("community-status");
        info.getChildren().addAll(name, statusLabel);
        HBox.setHgrow(info, Priority.ALWAYS);

        TLButton acceptBtn = new TLButton("✓  " + I18n.get("connection.accept"), TLButton.ButtonVariant.SUCCESS);
        acceptBtn.setSize(TLButton.ButtonSize.SM);
        acceptBtn.setOnAction(e -> {
            // Accepter la demande dans un thread séparé
            new Thread(() -> {
                ConnectionService.getInstance().acceptRequest(conn.getId());
                Platform.runLater(() -> {
                    showToast(I18n.get("connection.success.accepted"));
                    loadConnectionsTab(); // Recharger l'onglet
                    if (notificationService != null)
                        notificationService.pollNow(); // Actualiser les notifications
                });
            }, "AcceptThread").start();
        });

        // Bouton REFUSER la demande
        TLButton rejectBtn = new TLButton(I18n.get("connection.reject"), TLButton.ButtonVariant.GHOST);
        rejectBtn.setSize(TLButton.ButtonSize.SM);
        rejectBtn.setStyle("-fx-text-fill: -fx-destructive;");
        rejectBtn.setOnAction(e -> {
            new Thread(() -> {
                ConnectionService.getInstance().rejectRequest(conn.getId());
                Platform.runLater(() -> {
                    loadConnectionsTab();
                    if (notificationService != null)
                        notificationService.pollNow();
                });
            }, "RejectThread").start();
        });

        HBox buttons = new HBox(8, acceptBtn, rejectBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(avatar, info, buttons);
        card.setContent(content);
        return card;
    }

    /**
     * Crée une carte pour une connexion acceptée.
     * Affiche le nom, les badges, et les boutons Message / Supprimer.
     *
     * @param conn la connexion acceptée
     * @return TLCard avec les actions disponibles
     */
    private TLCard createConnectionCard(Connection conn) {
        TLCard card = new TLCard();
        card.getStyleClass().add("community-card");
        HBox content = new HBox(14);
        content.setPadding(new Insets(16));
        content.setAlignment(Pos.CENTER_LEFT);

        String userName = conn.getOtherUserName() != null ? conn.getOtherUserName() : "User";
        StackPane avatar = createAvatar(userName);

        VBox info = new VBox(2);
        Label name = new Label(userName);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");

        HBox badges = new HBox(6);
        if (conn.getConnectionType() != null && !conn.getConnectionType().isBlank()) {
            TLBadge typeBadge = new TLBadge(conn.getConnectionType(), TLBadge.Variant.SECONDARY);
            badges.getChildren().add(typeBadge);
        }
        TLBadge connectedBadge = new TLBadge("Connected", TLBadge.Variant.SUCCESS);
        badges.getChildren().add(connectedBadge);

        info.getChildren().addAll(name, badges);
        HBox.setHgrow(info, Priority.ALWAYS);

        // Déterminer l'ID de l'autre utilisateur pour ouvrir une conversation
        int otherUserId = conn.getUserId1() == currentUser.getId() ? conn.getUserId2() : conn.getUserId1();
        // Bouton pour envoyer un message à la connexion
        TLButton messageBtn = new TLButton("✉  " + I18n.get("message.new"), TLButton.ButtonVariant.OUTLINE);
        messageBtn.setSize(TLButton.ButtonSize.SM);
        messageBtn.setOnAction(e -> openConversation(otherUserId, conn.getOtherUserName()));

        // Bouton SUPPRIMER la connexion
        TLButton removeBtn = new TLButton(I18n.get("connection.remove"), TLButton.ButtonVariant.GHOST);
        removeBtn.setSize(TLButton.ButtonSize.SM);
        removeBtn.setStyle("-fx-text-fill: -fx-destructive;");
        removeBtn.setOnAction(e -> removeConnection(conn.getId()));

        HBox buttons = new HBox(8, messageBtn, removeBtn);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        content.getChildren().addAll(avatar, info, buttons);
        card.setContent(content);
        return card;
    }

    /**
     * Crée une carte de suggestion de connexion.
     * Affiche le nom avec un bouton pour envoyer une demande.
     *
     * @param suggestion la suggestion de connexion
     * @return TLCard avec le bouton d'envoi de demande
     */
    private TLCard createSuggestionCard(Connection suggestion) {
        TLCard card = new TLCard();
        card.getStyleClass().add("community-card");
        HBox content = new HBox(14);
        content.setPadding(new Insets(16));
        content.setAlignment(Pos.CENTER_LEFT);

        String userName = suggestion.getOtherUserName() != null ? suggestion.getOtherUserName() : "User";
        StackPane avatar = createAvatar(userName);

        VBox info = new VBox(2);
        Label name = new Label(userName);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        Label hint = new Label("Suggested for you");
        hint.getStyleClass().add("community-status");
        info.getChildren().addAll(name, hint);
        HBox.setHgrow(info, Priority.ALWAYS);

        TLButton connectBtn = new TLButton("＋  " + I18n.get("connection.send_request"), TLButton.ButtonVariant.PRIMARY);
        connectBtn.setSize(TLButton.ButtonSize.SM);
        connectBtn.setOnAction(e -> {
            new Thread(() -> {
                ConnectionService.getInstance().sendRequest(currentUser.getId(), suggestion.getUserId2());
                Platform.runLater(() -> {
                    showToast(I18n.get("connection.success.sent"));
                    loadConnectionsTab();
                    if (notificationService != null)
                        notificationService.pollNow();
                });
            }, "SendRequestThread").start();
        });

        content.getChildren().addAll(avatar, info, connectBtn);
        card.setContent(content);
        return card;
    }

    /**
     * Supprime une connexion après confirmation.
     * Affiche une boîte de dialogue de confirmation avant la suppression.
     *
     * @param connectionId l'identifiant de la connexion à supprimer
     */
    private void removeConnection(int connectionId) {
        DialogUtils.showConfirmation(
                I18n.get("community.remove.confirm.title"),
                I18n.get("community.remove.confirm.message")).ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            ConnectionService.getInstance().removeConnection(connectionId);
                            Platform.runLater(() -> {
                                showToast(I18n.get("connection.success.removed"));
                                loadConnectionsTab();
                                if (notificationService != null)
                                    notificationService.pollNow();
                            });
                        }, "RemoveConnThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // MESSAGES — Messagerie complète avec liste de conversations
    // (Lire, Envoyer, Modifier, Supprimer des messages)
    // ═══════════════════════════════════════════════════════════

    /**
     * Charge l'onglet Messages.
     * Affiche le nombre de messages non lus et la liste des conversations.
     */
    private void loadMessagesTab() {
        contentPane.getChildren().clear();
        if (currentUser == null)
            return;

        VBox messagesPane = new VBox(12);

        // Unread badge
        Task<Integer> unreadTask = new Task<>() {
            @Override
            protected Integer call() {
                return MessagingService.getInstance().getUnreadCount(currentUser.getId());
            }
        };

        Task<List<Conversation>> convTask = new Task<>() {
            @Override
            protected List<Conversation> call() {
                return MessagingService.getInstance().getConversations(currentUser.getId());
            }
        };

        unreadTask.setOnSucceeded(e1 -> {
            int unread = unreadTask.getValue();

            // ── En-tête avec titre + badge non lus ──
            HBox headerRow = new HBox(12);
            headerRow.setAlignment(Pos.CENTER_LEFT);
            Label title = new Label(I18n.get("message.title"));
            title.getStyleClass().add("h4");
            if (unread > 0) {
                TLBadge unreadBadge = new TLBadge(I18n.get("message.unread", unread), TLBadge.Variant.DESTRUCTIVE);
                headerRow.getChildren().addAll(title, unreadBadge);
            } else {
                headerRow.getChildren().add(title);
            }
            messagesPane.getChildren().add(headerRow);

            // ── Barre de filtres conversations (style Facebook) ──
            HBox filterBar = new HBox(8);
            filterBar.setAlignment(Pos.CENTER_LEFT);
            filterBar.setPadding(new Insets(4, 0, 8, 0));

            // Filtre Non lus (style Facebook)
            TLButton filterAllBtn = new TLButton("Toutes", TLButton.ButtonVariant.PRIMARY);
            filterAllBtn.setSize(TLButton.ButtonSize.SM);
            TLButton filterUnreadBtn = new TLButton("Non lues", TLButton.ButtonVariant.OUTLINE);
            filterUnreadBtn.setSize(TLButton.ButtonSize.SM);

            // Filtre par date
            TLSelect<String> dateFilter = new TLSelect<>("",
                    "Toutes les dates", "Aujourd'hui", "Cette semaine", "Ce mois");
            dateFilter.setValue("Toutes les dates");

            // Tri par date (Plus récent / Plus ancien)
            TLSelect<String> sortFilter = new TLSelect<>("",
                    "Plus récent", "Plus ancien");
            sortFilter.setValue("Plus récent");

            filterBar.getChildren().addAll(filterAllBtn, filterUnreadBtn, dateFilter, sortFilter);
            messagesPane.getChildren().add(filterBar);

            // Conteneur pour la liste de conversations (rechargeable par les filtres)
            VBox convListContainer = new VBox(8);
            messagesPane.getChildren().add(convListContainer);

            convTask.setOnSucceeded(e2 -> {
                List<Conversation> allConversations = convTask.getValue();
                // Afficher toutes les conversations par défaut
                displayFilteredConversations(convListContainer, allConversations, false, "Toutes les dates",
                        sortFilter.getValue());

                // ── Filtre "Toutes" : afficher toutes les conversations ──
                filterAllBtn.setOnAction(ev -> {
                    filterAllBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
                    filterUnreadBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
                    displayFilteredConversations(convListContainer, allConversations, false, dateFilter.getValue(),
                            sortFilter.getValue());
                });

                // ── Filtre "Non lues" : seulement celles avec unreadCount > 0 ──
                filterUnreadBtn.setOnAction(ev -> {
                    filterUnreadBtn.setVariant(TLButton.ButtonVariant.PRIMARY);
                    filterAllBtn.setVariant(TLButton.ButtonVariant.OUTLINE);
                    displayFilteredConversations(convListContainer, allConversations, true, dateFilter.getValue(),
                            sortFilter.getValue());
                });

                // ── Filtre par date : re-filtrer quand la date change ──
                dateFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                    boolean unreadOnly = filterUnreadBtn.getVariant() == TLButton.ButtonVariant.PRIMARY;
                    displayFilteredConversations(convListContainer, allConversations, unreadOnly, newVal,
                            sortFilter.getValue());
                });

                // ── Tri par date : re-trier quand le tri change ──
                sortFilter.valueProperty().addListener((obs, oldVal, newVal) -> {
                    boolean unreadOnly = filterUnreadBtn.getVariant() == TLButton.ButtonVariant.PRIMARY;
                    displayFilteredConversations(convListContainer, allConversations, unreadOnly, dateFilter.getValue(),
                            newVal);
                });
            });
            new Thread(convTask, "ConversationsThread").start();
        });
        new Thread(unreadTask, "UnreadCountThread").start();

        contentPane.getChildren().add(messagesPane);
    }

    /**
     * Affiche les conversations filtrées dans le conteneur.
     * Filtre par : non lues uniquement + période de date.
     * Style Facebook : les conversations non lues ont un fond accentué.
     *
     * @param container     le VBox conteneur des conversations
     * @param conversations toutes les conversations
     * @param unreadOnly    true = afficher seulement les non lues
     * @param dateFilterVal valeur du filtre date ("Toutes les dates",
     *                      "Aujourd'hui", etc.)
     * @param sortVal       valeur du tri ("Plus récent" ou "Plus ancien")
     */
    private void displayFilteredConversations(VBox container, List<Conversation> conversations,
            boolean unreadOnly, String dateFilterVal, String sortVal) {
        container.getChildren().clear();

        // Déterminer la date limite selon le filtre
        LocalDateTime dateCutoff = null;
        if (dateFilterVal != null) {
            dateCutoff = switch (dateFilterVal) {
                case "Aujourd'hui" -> LocalDateTime.now().minusDays(1);
                case "Cette semaine" -> LocalDateTime.now().minusDays(7);
                case "Ce mois" -> LocalDateTime.now().minusDays(30);
                default -> null; // Toutes les dates
            };
        }

        // Construire la liste filtrée
        List<Conversation> filtered = new java.util.ArrayList<>();
        for (Conversation conv : conversations) {
            // Filtre non lus
            if (unreadOnly && conv.getUnreadCount() <= 0)
                continue;
            // Filtre par date
            if (dateCutoff != null && conv.getLastMessageDate() != null
                    && conv.getLastMessageDate().isBefore(dateCutoff))
                continue;
            filtered.add(conv);
        }

        // Trier par date : Plus récent (DESC) ou Plus ancien (ASC)
        boolean ascending = "Plus ancien".equals(sortVal);
        filtered.sort((a, b) -> {
            LocalDateTime da = a.getLastMessageDate();
            LocalDateTime db = b.getLastMessageDate();
            if (da == null && db == null)
                return 0;
            if (da == null)
                return 1;
            if (db == null)
                return -1;
            return ascending ? da.compareTo(db) : db.compareTo(da);
        });

        int count = 0;
        for (Conversation conv : filtered) {
            TLCard card = createConversationCard(conv);
            // Style Facebook : fond légèrement accentué pour les conversations non lues
            if (conv.getUnreadCount() > 0) {
                card.setStyle("-fx-border-color: -fx-primary; -fx-border-width: 0 0 0 3;");
            }
            container.getChildren().add(card);
            count++;
        }

        if (count == 0) {
            Label empty = new Label(unreadOnly
                    ? "Aucune conversation non lue"
                    : I18n.get("message.empty"));
            empty.getStyleClass().add("text-muted");
            empty.setStyle("-fx-font-size: 13px; -fx-padding: 12 0;");
            container.getChildren().add(empty);
        }
    }

    /**
     * Crée une carte de conversation dans la liste des messages.
     * Affiche : avatar, nom, aperçu du dernier message, date, badge non lus.
     * Au clic, ouvre la vue de conversation complète.
     *
     * @param conv la conversation à afficher
     * @return TLCard cliquable
     */
    private TLCard createConversationCard(Conversation conv) {
        TLCard card = new TLCard();
        card.getStyleClass().add("community-card");
        HBox content = new HBox(14);
        content.setPadding(new Insets(14, 16, 14, 16));
        content.setAlignment(Pos.CENTER_LEFT);

        String userName = conv.getOtherUserName() != null ? conv.getOtherUserName() : "User";

        // ── Avatar avec indicateur de présence en ligne (point vert/gris) ──
        StackPane avatar = createAvatar(userName);
        int otherUserId = (conv.getParticipant1() == currentUser.getId())
                ? conv.getParticipant2()
                : conv.getParticipant1();

        // Point indicateur en ligne / hors ligne
        javafx.scene.shape.Circle onlineDot = new javafx.scene.shape.Circle(5);
        onlineDot.setStroke(javafx.scene.paint.Color.WHITE);
        onlineDot.setStrokeWidth(1.5);
        onlineDot.setFill(javafx.scene.paint.Color.GRAY); // Gris par défaut
        StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
        avatar.getChildren().add(onlineDot);

        // Vérifier le statut en ligne en arrière-plan
        new Thread(() -> {
            boolean online = OnlineStatusService.getInstance().isUserOnline(otherUserId);
            Platform.runLater(() -> {
                onlineDot.setFill(
                        online ? javafx.scene.paint.Color.web("#22c55e") : javafx.scene.paint.Color.web("#9ca3af"));
            });
        }, "OnlineDotThread").start();

        VBox textBox = new VBox(3);
        Label name = new Label(userName);
        name.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        Label preview = new Label(conv.getLastMessagePreview() != null ? conv.getLastMessagePreview() : "");
        preview.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");
        preview.setMaxWidth(300);
        textBox.getChildren().addAll(name, preview);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        VBox rightBox = new VBox(4);
        rightBox.setAlignment(Pos.CENTER_RIGHT);
        if (conv.getLastMessageDate() != null) {
            Label time = new Label(formatDate(conv.getLastMessageDate()));
            time.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
            rightBox.getChildren().add(time);
        }
        if (conv.getUnreadCount() > 0) {
            TLBadge unread = new TLBadge(String.valueOf(conv.getUnreadCount()), TLBadge.Variant.DESTRUCTIVE);
            rightBox.getChildren().add(unread);
        }

        content.getChildren().addAll(avatar, textBox, rightBox);
        card.setContent(content);
        card.setOnMouseClicked(e -> openConversationView(conv));
        return card;
    }

    /**
     * Ouvre ou crée une conversation avec un autre utilisateur.
     * Si la conversation n'existe pas, elle est créée automatiquement.
     *
     * @param otherUserId ID de l'autre utilisateur
     * @param otherName   nom de l'autre utilisateur
     */
    private void openConversation(int otherUserId, String otherName) {
        new Thread(() -> {
            // Obtenir ou créer la conversation (utilise Math.min/Math.max pour l'ordre)
            int convId = MessagingService.getInstance().getOrCreateConversation(currentUser.getId(), otherUserId);
            if (convId > 0) {
                Conversation conv = new Conversation();
                conv.setId(convId);
                conv.setOtherUserName(otherName);
                conv.setParticipant1(Math.min(currentUser.getId(), otherUserId));
                conv.setParticipant2(Math.max(currentUser.getId(), otherUserId));
                Platform.runLater(() -> openConversationView(conv));
            }
        }, "OpenConvThread").start();
    }

    /**
     * Affiche la vue de conversation complète (style chat).
     * Contient :
     * - En-tête avec bouton retour, avatar et nom
     * - Zone de messages avec bulles (mes messages à droite, les autres à gauche)
     * - Boutons Modifier/Supprimer au survol sur ses propres messages
     * - Barre de saisie en bas avec bouton envoyer
     *
     * @param conv la conversation à afficher
     */
    private void openConversationView(Conversation conv) {
        contentPane.getChildren().clear();

        VBox chatPane = new VBox(0);

        // ── En-tête du chat : bouton retour + avatar + nom ──
        HBox chatHeader = new HBox(12);
        chatHeader.setAlignment(Pos.CENTER_LEFT);
        chatHeader.setPadding(new Insets(12, 16, 12, 16));
        chatHeader.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; -fx-border-width: 0 0 1 0;");

        // ── Déterminer l'ID de l'autre participant ──
        int otherUserId = (conv.getParticipant1() == currentUser.getId())
                ? conv.getParticipant2()
                : conv.getParticipant1();

        TLButton backBtn = new TLButton("←", TLButton.ButtonVariant.GHOST);
        backBtn.setSize(TLButton.ButtonSize.SM);
        backBtn.setOnAction(e -> {
            // Effacer le statut de saisie en quittant la conversation
            new Thread(() -> MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId()),
                    "ClearTypingThread").start();
            loadMessagesTab(); // Retour à la liste
        });

        String otherName = conv.getOtherUserName() != null ? conv.getOtherUserName() : "Conversation";
        StackPane headerAvatar = createAvatar(otherName, 36);

        VBox headerInfo = new VBox(1);
        Label chatTitle = new Label(otherName);
        chatTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");

        // ── STATUT EN LIGNE/HORS LIGNE (temps réel) ──
        Label onlineLabel = new Label("...");
        onlineLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground; -fx-font-weight: 500;");
        headerInfo.getChildren().addAll(chatTitle, onlineLabel);

        // Vérification initiale + polling du statut en ligne toutes les 5 secondes
        javafx.animation.Timeline onlinePollTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO, initEv -> {
                    new Thread(() -> {
                        boolean online = OnlineStatusService.getInstance().isUserOnline(otherUserId);
                        String statusText = OnlineStatusService.getInstance().getStatusText(otherUserId);
                        Platform.runLater(() -> {
                            onlineLabel.setText(statusText);
                            onlineLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: "
                                    + (online ? "#22c55e;" : "-fx-muted-foreground;"));
                        });
                    }, "OnlineCheckThread").start();
                }),
                new javafx.animation.KeyFrame(Duration.seconds(5), tickEv -> {
                    new Thread(() -> {
                        boolean online = OnlineStatusService.getInstance().isUserOnline(otherUserId);
                        String statusText = OnlineStatusService.getInstance().getStatusText(otherUserId);
                        Platform.runLater(() -> {
                            onlineLabel.setText(statusText);
                            onlineLabel.setStyle("-fx-font-size: 11px; -fx-font-weight: 500; -fx-text-fill: "
                                    + (online ? "#22c55e;" : "-fx-muted-foreground;"));
                        });
                    }, "OnlineCheckThread").start();
                }));
        onlinePollTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        onlinePollTimeline.play();

        chatHeader.getChildren().addAll(backBtn, headerAvatar, headerInfo);

        // Bouton Résumé IA pour la conversation privée
        Button privateSummaryBtn = new Button("📝 Résumé IA");
        privateSummaryBtn.getStyleClass().addAll("btn", "btn-outline");
        privateSummaryBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 12; -fx-cursor: hand;");
        Region chatHeaderSpacer = new Region();
        HBox.setHgrow(chatHeaderSpacer, Priority.ALWAYS);
        chatHeader.getChildren().addAll(chatHeaderSpacer, privateSummaryBtn);

        // Action du bouton Résumé IA (conversation privée)
        privateSummaryBtn.setOnAction(summaryEv -> {
            privateSummaryBtn.setDisable(true);
            privateSummaryBtn.setText("⏳ Résumé en cours...");
            new Thread(() -> {
                List<Message> allMsgs = MessagingService.getInstance().getMessages(conv.getId(), 1, 500);
                List<String> formatted = new ArrayList<>();
                for (Message m : allMsgs) {
                    String sender = m.getSenderName() != null ? m.getSenderName() : (m.getSenderId() == currentUser.getId() ? "Moi" : otherName);
                    String text = m.getContent() != null && !m.getContent().isEmpty() ? m.getContent() : "[" + m.getMessageType() + "]";
                    formatted.add(sender + ": " + text);
                }
                String summary = AISummaryService.getInstance().summarize(formatted);
                Platform.runLater(() -> {
                    privateSummaryBtn.setDisable(false);
                    privateSummaryBtn.setText("📝 Résumé IA");
                    TLDialog<Void> summaryDialog = new TLDialog<>();
                    summaryDialog.setDialogTitle("📝 Résumé IA - " + otherName);
                    summaryDialog.setDescription("Résumé généré par intelligence artificielle");
                    Label summaryLabel = new Label(summary);
                    summaryLabel.setWrapText(true);
                    summaryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground; -fx-line-spacing: 4;");
                    summaryLabel.setMaxWidth(500);
                    javafx.scene.control.ScrollPane summaryScroll = new javafx.scene.control.ScrollPane(summaryLabel);
                    summaryScroll.setFitToWidth(true);
                    summaryScroll.setPrefHeight(300);
                    summaryScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                    summaryDialog.setContent(summaryScroll);
                    summaryDialog.addButton(ButtonType.OK);
                    summaryDialog.setResultConverter(bt -> null);
                    summaryDialog.showAndWait();
                });
            }, "PrivateAISummaryThread").start();
        });

        // ── Zone des messages avec défilement ──
        VBox messagesList = new VBox(6);
        messagesList.setPadding(new Insets(16));
        ScrollPane scroll = new ScrollPane(messagesList);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(400);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        // Charger les messages et marquer comme lus
        // Holder for reaction data fetched alongside messages
        final java.util.Map<Integer, java.util.Map<String, Integer>>[] convReactionsHolder = new java.util.Map[]{null};
        final java.util.Map<Integer, java.util.Set<String>>[] convMyReactionsHolder = new java.util.Map[]{null};
        Task<List<Message>> messagesTask = new Task<>() {
            @Override
            protected List<Message> call() {
                // D'abord marquer les messages de cette conversation comme lus
                MessagingService.getInstance().markAsRead(conv.getId(), currentUser.getId());
                // Charger les réactions
                convReactionsHolder[0] = MessagingService.getInstance().getReactionsForConversation(conv.getId());
                convMyReactionsHolder[0] = MessagingService.getInstance().getUserReactionsForConversation(conv.getId(), currentUser.getId());
                // Puis charger les messages (page 1, max 100)
                return MessagingService.getInstance().getMessages(conv.getId(), 1, 100);
            }
        };
        messagesTask.setOnSucceeded(e -> Platform.runLater(() -> {
            List<Message> messages = messagesTask.getValue();
            java.util.Map<Integer, java.util.Map<String, Integer>> allReactions =
                    convReactionsHolder[0] != null ? convReactionsHolder[0] : java.util.Collections.emptyMap();
            java.util.Map<Integer, java.util.Set<String>> allMyReactions =
                    convMyReactionsHolder[0] != null ? convMyReactionsHolder[0] : java.util.Collections.emptyMap();
            // Map pour stocker les labels "Envoyé" non lus → mise à jour temps réel vers
            // "Vu"
            java.util.Map<Integer, Label> unreadStatusLabels = new java.util.HashMap<>();
            for (Message msg : messages) {
                boolean isMine = msg.getSenderId() == currentUser.getId(); // Mon message ?
                HBox row = new HBox(8);
                row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT); // Droite/Gauche
                row.setPadding(new Insets(2));

                // Avatar de l'autre utilisateur (seulement pour ses messages)
                StackPane msgAvatar = null;
                if (!isMine) {
                    msgAvatar = createAvatar(otherName, 30);
                }

                // Bulle de message avec style différent selon l'expéditeur
                VBox bubble = new VBox(3);
                bubble.setPadding(new Insets(10, 14, 10, 14));
                bubble.setMaxWidth(350);
                bubble.getStyleClass().add(isMine ? "msg-bubble-mine" : "msg-bubble-theirs");

                // ── CONTENU DU MESSAGE — Texte, Image ou Vidéo ──
                if (msg.isImage() && msg.hasMedia()) {
                    // ── IMAGE — Afficher l'image dans la bulle ──
                    try {
                        ImageView imgView = new ImageView();
                        imgView.setPreserveRatio(true);
                        imgView.setFitWidth(280);
                        imgView.setSmooth(true);
                        imgView.setStyle("-fx-cursor: hand;");

                        // Charger l'image en arrière-plan pour ne pas bloquer l'UI
                        Image image = new Image(msg.getMediaUrl(), 280, 0, true, true, true);
                        imgView.setImage(image);

                        // Coins arrondis sur l'image
                        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 200);
                        clip.setArcWidth(12);
                        clip.setArcHeight(12);
                        image.progressProperty().addListener((obsImg, ov, nv) -> {
                            if (nv.doubleValue() >= 1.0 && image.getHeight() > 0) {
                                double ratio = 280.0 / image.getWidth();
                                clip.setHeight(image.getHeight() * ratio);
                                imgView.setClip(clip);
                            }
                        });

                        bubble.getChildren().add(imgView);

                        // Légende textuelle (si présente)
                        if (msg.getContent() != null && !msg.getContent().isBlank()) {
                            Label captionLabel = new Label(msg.getContent());
                            captionLabel.setWrapText(true);
                            captionLabel.setStyle(isMine
                                    ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;"
                                    : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                            bubble.getChildren().add(captionLabel);
                        }
                    } catch (Exception imgEx) {
                        // Fallback : afficher le lien si l'image ne charge pas
                        Label fallback = new Label("📷 " + (msg.getFileName() != null ? msg.getFileName() : "Image"));
                        fallback.setWrapText(true);
                        fallback.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 13px;"
                                : "-fx-text-fill: -fx-foreground; -fx-font-size: 13px;");
                        bubble.getChildren().add(fallback);
                    }
                } else if (msg.isVideo() && msg.hasMedia()) {
                    // ── VIDÉO — Afficher une vignette cliquable ──
                    VBox videoBox = new VBox(4);
                    videoBox.setAlignment(Pos.CENTER);

                    // Icône play + nom du fichier
                    Label videoIcon = new Label("🎬");
                    videoIcon.setStyle("-fx-font-size: 36px;");

                    Label videoName = new Label(msg.getFileName() != null ? msg.getFileName() : "Vidéo");
                    videoName.setWrapText(true);
                    videoName.setStyle(isMine
                            ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px;"
                            : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px;");

                    Label playHint = new Label("▶ Cliquer pour ouvrir");
                    playHint.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                            + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));

                    videoBox.getChildren().addAll(videoIcon, videoName, playHint);
                    videoBox.setStyle("-fx-cursor: hand; -fx-padding: 12;");

                    // Clic : ouvrir la vidéo dans le lecteur par défaut du système
                    videoBox.setOnMouseClicked(vidEv -> {
                        try {
                            String mediaUrl = msg.getMediaUrl();
                            if (mediaUrl.startsWith("file:")) {
                                java.awt.Desktop.getDesktop().open(new java.io.File(java.net.URI.create(mediaUrl)));
                            } else {
                                java.awt.Desktop.getDesktop().browse(java.net.URI.create(mediaUrl));
                            }
                        } catch (Exception ex) {
                            logger.warn("Cannot open video: {}", ex.getMessage());
                        }
                    });

                    bubble.getChildren().add(videoBox);

                    // Légende textuelle (si présente)
                    if (msg.getContent() != null && !msg.getContent().isBlank()) {
                        Label captionLabel = new Label(msg.getContent());
                        captionLabel.setWrapText(true);
                        captionLabel.setStyle(isMine
                                ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;"
                                : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                        bubble.getChildren().add(captionLabel);
                    }
                } else if (msg.isVocal() && msg.hasMedia()) {
                    // ── VOCAL — Lecteur audio intégré dans la bulle (comme Messenger) ──
                    HBox vocalBox = new HBox(8);
                    vocalBox.setAlignment(Pos.CENTER_LEFT);
                    vocalBox.getStyleClass().add("msg-vocal-player");

                    // Bouton play/pause
                    Label playPauseIcon = new Label("▶");
                    playPauseIcon.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-text-fill: " +
                            (isMine ? "-fx-primary-foreground;" : "-fx-primary;"));

                    // Barre de progression
                    javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(0);
                    progressBar.setPrefWidth(150);
                    progressBar.setPrefHeight(6);
                    progressBar.getStyleClass().add("vocal-progress-bar");

                    // Durée
                    String durationText = com.skilora.community.service.AudioRecorderService
                            .formatDuration(msg.getDuration());
                    Label durationLabel = new Label(durationText);
                    durationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: " +
                            (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));

                    // État de lecture (partagé dans un tableau pour accès depuis lambda)
                    final javafx.scene.media.MediaPlayer[] playerHolder = { null };
                    final boolean[] isPlaying = { false };

                    playPauseIcon.setOnMouseClicked(playEv -> {
                        if (isPlaying[0] && playerHolder[0] != null) {
                            // Pause
                            playerHolder[0].pause();
                            playPauseIcon.setText("▶");
                            isPlaying[0] = false;
                        } else {
                            if (playerHolder[0] == null) {
                                try {
                                    javafx.scene.media.Media media = new javafx.scene.media.Media(msg.getMediaUrl());
                                    playerHolder[0] = new javafx.scene.media.MediaPlayer(media);

                                    playerHolder[0].currentTimeProperty().addListener((obsT, oldT, newT) -> {
                                        if (playerHolder[0].getTotalDuration() != null &&
                                                playerHolder[0].getTotalDuration().toMillis() > 0) {
                                            double progress = newT.toMillis()
                                                    / playerHolder[0].getTotalDuration().toMillis();
                                            Platform.runLater(() -> {
                                                progressBar.setProgress(progress);
                                                int elapsed = (int) (newT.toSeconds());
                                                durationLabel.setText(com.skilora.community.service.AudioRecorderService
                                                        .formatDuration(elapsed));
                                            });
                                        }
                                    });

                                    playerHolder[0].setOnEndOfMedia(() -> Platform.runLater(() -> {
                                        playPauseIcon.setText("▶");
                                        progressBar.setProgress(0);
                                        durationLabel.setText(durationText);
                                        isPlaying[0] = false;
                                        playerHolder[0].stop();
                                        playerHolder[0].dispose();
                                        playerHolder[0] = null;
                                    }));

                                    playerHolder[0].setOnError(() -> Platform.runLater(() -> {
                                        playPauseIcon.setText("▶");
                                        isPlaying[0] = false;
                                        logger.warn("Audio playback error: {}", playerHolder[0].getError());
                                    }));

                                } catch (Exception audioEx) {
                                    logger.warn("Cannot create audio player: {}", audioEx.getMessage());
                                    return;
                                }
                            }
                            playerHolder[0].play();
                            playPauseIcon.setText("⏸");
                            isPlaying[0] = true;
                        }
                    });

                    vocalBox.getChildren().addAll(playPauseIcon, progressBar, durationLabel);
                    bubble.getChildren().add(vocalBox);
                } else {
                    // ── TEXTE — Message classique ──
                    Label msgText = new Label(msg.getContent());
                    msgText.setWrapText(true);
                    msgText.setStyle(isMine
                            ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 13px;"
                            : "-fx-text-fill: -fx-foreground; -fx-font-size: 13px;");
                    bubble.getChildren().add(msgText);
                }

                Label timeLabel = new Label(formatDate(msg.getCreatedDate()));
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                        + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));

                // ── INDICATEUR "VU" — Affiché sur mes messages quand l'autre les a lus ──
                if (isMine) {
                    HBox statusRow = new HBox(4);
                    statusRow.setAlignment(Pos.CENTER_RIGHT);
                    statusRow.getChildren().add(timeLabel);

                    if (msg.isRead()) {
                        Label seenLabel = new Label("✓✓ Vu");
                        seenLabel.getStyleClass().add("msg-seen-indicator");
                        statusRow.getChildren().add(seenLabel);
                    } else {
                        Label sentLabel = new Label("✓ Envoyé");
                        sentLabel.getStyleClass().add("msg-sent-indicator");
                        statusRow.getChildren().add(sentLabel);
                        unreadStatusLabels.put(msg.getId(), sentLabel);
                    }
                    bubble.getChildren().add(statusRow);
                } else {
                    bubble.getChildren().add(timeLabel);
                }

                // ── Boutons Modifier / Supprimer pour ses propres messages ──
                // Visibles seulement au survol (setOnMouseEntered/Exited)
                if (isMine) {
                    HBox actionBtns = new HBox(4);
                    actionBtns.setAlignment(Pos.CENTER_RIGHT);
                    actionBtns.setVisible(false); // Caché par défaut
                    actionBtns.setManaged(false); // Ne prend pas de place

                    // Bouton MODIFIER le message
                    TLButton editBtn = new TLButton("✏", TLButton.ButtonVariant.GHOST);
                    editBtn.setSize(TLButton.ButtonSize.SM);
                    editBtn.setStyle("-fx-padding: 2 6; -fx-font-size: 11px; -fx-cursor: hand;");
                    editBtn.setOnAction(ev -> {
                        // Dialogue de modification du message
                        TLDialog<Void> editDlg = new TLDialog<>();
                        editDlg.setDialogTitle(I18n.get("message.edit"));

                        VBox editContent = new VBox(12);
                        editContent.setPadding(new Insets(16));

                        // Champ pré-rempli avec le contenu actuel
                        TLTextarea editArea = new TLTextarea("", "");
                        editArea.setText(msg.getContent()); // Pré-remplir
                        editArea.getControl().setPrefRowCount(3);

                        HBox editBtns = new HBox(8);
                        editBtns.setAlignment(Pos.CENTER_RIGHT);
                        TLButton cancelEditBtn = new TLButton(I18n.get("common.cancel"),
                                TLButton.ButtonVariant.SECONDARY);
                        cancelEditBtn.setOnAction(ev2 -> editDlg.close());
                        TLButton saveEditBtn = new TLButton(I18n.get("common.save"), TLButton.ButtonVariant.PRIMARY);
                        saveEditBtn.setOnAction(ev2 -> {
                            String newText = editArea.getText();
                            // CONTRÔLE DE SAISIE : vérifier que le texte n'est pas vide
                            if (newText != null && !newText.trim().isEmpty()) {
                                editDlg.close();
                                // CONTRÔLE DE SÉCURITÉ : updateMessage vérifie sender_id
                                new Thread(() -> {
                                    MessagingService.getInstance().updateMessage(msg.getId(), currentUser.getId(),
                                            newText.trim());
                                    Platform.runLater(() -> openConversationView(conv)); // Recharger
                                }, "EditMsgThread").start();
                            }
                        });
                        editBtns.getChildren().addAll(cancelEditBtn, saveEditBtn);
                        editContent.getChildren().addAll(editArea, editBtns);
                        editDlg.setContent(editContent);
                        editDlg.show();
                    });

                    // Bouton SUPPRIMER le message
                    TLButton deleteBtn = new TLButton("🗑", TLButton.ButtonVariant.GHOST);
                    deleteBtn.setSize(TLButton.ButtonSize.SM);
                    deleteBtn.setStyle("-fx-padding: 2 6; -fx-font-size: 11px; -fx-cursor: hand;");
                    deleteBtn.setOnAction(ev -> {
                        // Dialogue de confirmation avant suppression
                        TLDialog<Void> confirmDlg = new TLDialog<>();
                        confirmDlg.setDialogTitle(I18n.get("message.delete"));
                        confirmDlg.setDescription(I18n.get("message.delete.confirm"));

                        VBox confirmContent = new VBox(12);
                        confirmContent.setPadding(new Insets(16));
                        HBox confirmBtns = new HBox(8);
                        confirmBtns.setAlignment(Pos.CENTER_RIGHT);
                        TLButton cancelDelBtn = new TLButton(I18n.get("common.cancel"),
                                TLButton.ButtonVariant.SECONDARY);
                        cancelDelBtn.setOnAction(ev2 -> confirmDlg.close());
                        TLButton okDelBtn = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.DANGER);
                        okDelBtn.setOnAction(ev2 -> {
                            confirmDlg.close();
                            // CONTRÔLE DE SÉCURITÉ : deleteMessage vérifie sender_id
                            new Thread(() -> {
                                MessagingService.getInstance().deleteMessage(msg.getId(), currentUser.getId());
                                Platform.runLater(() -> openConversationView(conv)); // Recharger
                            }, "DeleteMsgThread").start();
                        });
                        confirmBtns.getChildren().addAll(cancelDelBtn, okDelBtn);
                        confirmContent.getChildren().add(confirmBtns);
                        confirmDlg.setContent(confirmContent);
                        confirmDlg.show();
                    });

                    actionBtns.getChildren().addAll(editBtn, deleteBtn);
                    bubble.getChildren().add(actionBtns);

                    // Afficher les actions au survol de la souris (hover)
                    row.setOnMouseEntered(ev -> {
                        actionBtns.setVisible(true);
                        actionBtns.setManaged(true);
                    });
                    row.setOnMouseExited(ev -> {
                        actionBtns.setVisible(false);
                        actionBtns.setManaged(false);
                    });
                }

                // ── REACTION BAR below the private message bubble ──
                java.util.Map<String, Integer> msgReactions = allReactions.getOrDefault(msg.getId(), java.util.Collections.emptyMap());
                java.util.Set<String> msgMyReactions = allMyReactions.getOrDefault(msg.getId(), java.util.Collections.emptySet());
                final int pmMsgId = msg.getId();
                FlowPane reactionBar = buildReactionBar(pmMsgId, isMine, msgReactions, msgMyReactions, emoji -> {
                    new Thread(() -> {
                        MessagingService.getInstance().toggleReaction(pmMsgId, currentUser.getId(), emoji);
                        Platform.runLater(() -> openConversationView(conv));
                    }, "PrivateReactionThread").start();
                });

                VBox bubbleWithReactions = new VBox(0);
                bubbleWithReactions.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
                bubbleWithReactions.getChildren().addAll(bubble, reactionBar);

                if (isMine) {
                    row.getChildren().add(bubbleWithReactions); // Mon message : bulle seule à droite
                } else {
                    row.getChildren().addAll(msgAvatar, bubbleWithReactions); // Son message : avatar + bulle à gauche
                }
                messagesList.getChildren().add(row);
            }
            Platform.runLater(() -> scroll.setVvalue(1.0)); // Défiler vers le bas

            // ── POLLING TEMPS RÉEL DU "VU" — Vérifier si mes messages ont été lus (toutes
            // les 2s) ──
            if (!unreadStatusLabels.isEmpty()) {
                javafx.animation.Timeline seenPollTimeline = new javafx.animation.Timeline(
                        new javafx.animation.KeyFrame(Duration.millis(2000), seenEv -> {
                            new Thread(() -> {
                                java.util.Map<Integer, Boolean> readStatus = MessagingService.getInstance()
                                        .getReadStatusForMyMessages(conv.getId(), currentUser.getId());
                                Platform.runLater(() -> {
                                    // Parcourir les labels non lus et mettre à jour ceux qui sont devenus "lus"
                                    java.util.List<Integer> nowRead = new java.util.ArrayList<>();
                                    for (java.util.Map.Entry<Integer, Label> entry : unreadStatusLabels.entrySet()) {
                                        Boolean isRead = readStatus.get(entry.getKey());
                                        if (isRead != null && isRead) {
                                            Label label = entry.getValue();
                                            label.setText("✓✓ Vu");
                                            label.getStyleClass().remove("msg-sent-indicator");
                                            label.getStyleClass().add("msg-seen-indicator");
                                            nowRead.add(entry.getKey());
                                        }
                                    }
                                    // Retirer les messages déjà marqués "Vu" du polling
                                    nowRead.forEach(unreadStatusLabels::remove);
                                });
                            }, "SeenPollThread").start();
                        }));
                seenPollTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
                seenPollTimeline.play();

                // Arrêter le polling "Vu" quand on quitte la conversation
                contentPane.sceneProperty().addListener((obs2, os2, ns2) -> {
                    if (ns2 == null)
                        seenPollTimeline.stop();
                });
            }
        }));
        new Thread(messagesTask, "LoadMessagesThread").start();

        // ── Barre de saisie en bas du chat ──
        HBox inputBar = new HBox(10);
        inputBar.setAlignment(Pos.CENTER_LEFT);
        inputBar.getStyleClass().add("chat-input-bar");
        inputBar.setPadding(new Insets(10, 14, 10, 14));

        TLTextField msgInput = new TLTextField("", I18n.get("message.placeholder"));
        HBox.setHgrow(msgInput, Priority.ALWAYS);

        // ── INDICATEUR DE SAISIE — "en train d'écrire..." (comme Facebook) ──
        HBox typingIndicatorRow = new HBox(8);
        typingIndicatorRow.setAlignment(Pos.CENTER_LEFT);
        typingIndicatorRow.setPadding(new Insets(2, 16, 2, 16));
        typingIndicatorRow.setVisible(false);
        typingIndicatorRow.setManaged(false);

        // Avatar de l'autre utilisateur dans l'indicateur
        StackPane typingAvatar = createAvatar(otherName, 24);

        // Bulle avec les 3 points animés
        HBox typingBubble = new HBox(4);
        typingBubble.setAlignment(Pos.CENTER);
        typingBubble.getStyleClass().add("typing-indicator-bubble");

        // Trois points qui s'animent (opacité alternée via Timeline)
        Label dot1 = new Label("●");
        Label dot2 = new Label("●");
        Label dot3 = new Label("●");
        dot1.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        dot2.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        dot3.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 10px;");
        typingBubble.getChildren().addAll(dot1, dot2, dot3);

        // Animation des points (effet de "pulsation" séquentielle)
        javafx.animation.Timeline dotAnimation = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.ZERO,
                        new javafx.animation.KeyValue(dot1.opacityProperty(), 0.3),
                        new javafx.animation.KeyValue(dot2.opacityProperty(), 0.3),
                        new javafx.animation.KeyValue(dot3.opacityProperty(), 0.3)),
                new javafx.animation.KeyFrame(Duration.millis(300),
                        new javafx.animation.KeyValue(dot1.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(Duration.millis(600),
                        new javafx.animation.KeyValue(dot1.opacityProperty(), 0.3),
                        new javafx.animation.KeyValue(dot2.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(Duration.millis(900),
                        new javafx.animation.KeyValue(dot2.opacityProperty(), 0.3),
                        new javafx.animation.KeyValue(dot3.opacityProperty(), 1.0)),
                new javafx.animation.KeyFrame(Duration.millis(1200),
                        new javafx.animation.KeyValue(dot3.opacityProperty(), 0.3)));
        dotAnimation.setCycleCount(javafx.animation.Animation.INDEFINITE);

        // Texte "en train d'écrire..."
        Label typingText = new Label(otherName + " est en train d'écrire");
        typingText.getStyleClass().add("typing-indicator");

        typingIndicatorRow.getChildren().addAll(typingAvatar, typingBubble, typingText);

        // ── POLLING TEMPS RÉEL — Vérifier si l'autre utilisateur tape (toutes les
        // 1.5s) ──
        javafx.animation.Timeline typingPollTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.millis(1500), ev -> {
                    new Thread(() -> {
                        boolean isTyping = MessagingService.getInstance().isUserTyping(conv.getId(), otherUserId);
                        Platform.runLater(() -> {
                            if (isTyping && !typingIndicatorRow.isVisible()) {
                                typingIndicatorRow.setVisible(true);
                                typingIndicatorRow.setManaged(true);
                                dotAnimation.play();
                                // Défiler vers le bas pour voir l'indicateur
                                scroll.setVvalue(1.0);
                            } else if (!isTyping && typingIndicatorRow.isVisible()) {
                                typingIndicatorRow.setVisible(false);
                                typingIndicatorRow.setManaged(false);
                                dotAnimation.stop();
                            }
                        });
                    }, "TypingPollThread").start();
                }));
        typingPollTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        typingPollTimeline.play();

        // ── DÉTECTION DE FRAPPE — Envoyer le statut "en train d'écrire" ──
        msgInput.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                new Thread(() -> MessagingService.getInstance().updateTypingStatus(conv.getId(), currentUser.getId()),
                        "TypingStatusThread").start();
            }
        });

        // ── EMOJI PICKER — Bouton emoji dans le chat ──
        TLButton chatEmojiBtn = new TLButton("😀", TLButton.ButtonVariant.GHOST);
        chatEmojiBtn.setSize(TLButton.ButtonSize.SM);
        chatEmojiBtn.setOnAction(ev -> showEmojiPickerForTextField(chatEmojiBtn, msgInput));

        // ── PIÈCE JOINTE — Bouton pour envoyer une image ou vidéo (📎) ──
        TLButton attachBtn = new TLButton("📎", TLButton.ButtonVariant.GHOST);
        attachBtn.setSize(TLButton.ButtonSize.SM);
        attachBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        attachBtn.setOnAction(ev -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Envoyer une image ou vidéo");
            fileChooser.getExtensionFilters().addAll(
                    new javafx.stage.FileChooser.ExtensionFilter("Tous les médias",
                            cloudinaryService.getAllowedMediaExtensionPatterns()),
                    new javafx.stage.FileChooser.ExtensionFilter("Images",
                            cloudinaryService.getAllowedExtensionPatterns()),
                    new javafx.stage.FileChooser.ExtensionFilter("Vidéos",
                            cloudinaryService.getAllowedVideoExtensionPatterns()));
            java.io.File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
            if (file != null) {
                String caption = msgInput.getText();
                msgInput.setText(""); // Vider le champ
                attachBtn.setText("⏳");
                attachBtn.setDisable(true);

                new Thread(() -> {
                    try {
                        String mediaType = cloudinaryService.detectMediaType(file);
                        String mediaUrl;
                        if ("VIDEO".equals(mediaType)) {
                            mediaUrl = cloudinaryService.uploadVideo(file);
                        } else {
                            mediaUrl = cloudinaryService.uploadImage(file);
                        }

                        // Effacer le statut de saisie + envoyer le message média
                        MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
                        MessagingService.getInstance().sendMessage(
                                conv.getId(), currentUser.getId(),
                                (caption != null && !caption.trim().isEmpty()) ? caption.trim() : null,
                                mediaType, mediaUrl, file.getName());

                        Platform.runLater(() -> {
                            attachBtn.setText("📎");
                            attachBtn.setDisable(false);
                            openConversationView(conv); // Recharger la vue
                            if (notificationService != null)
                                notificationService.pollNow();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            attachBtn.setText("📎");
                            attachBtn.setDisable(false);
                            DialogUtils.showError("Erreur d'envoi", ex.getMessage());
                        });
                    }
                }, "MediaUploadThread").start();
            }
        });

        // sendBtn declaration moved down after micBtn

        // ── BOUTON MICRO — Enregistrement vocal (comme Messenger) ──
        com.skilora.community.service.AudioRecorderService audioRecorder = com.skilora.community.service.AudioRecorderService
                .getInstance();
        TLButton micBtn = new TLButton("🎤", TLButton.ButtonVariant.GHOST);
        micBtn.setSize(TLButton.ButtonSize.SM);
        micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");

        // Indicateur d'enregistrement (caché par défaut)
        HBox recordingIndicator = new HBox(8);
        recordingIndicator.setAlignment(Pos.CENTER_LEFT);
        recordingIndicator.setVisible(false);
        recordingIndicator.setManaged(false);

        Label recordDot = new Label("🔴");
        recordDot.setStyle("-fx-font-size: 12px;");
        Label recordTimer = new Label("0:00");
        recordTimer.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        Label recordHint = new Label("Enregistrement en cours...");
        recordHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");

        TLButton cancelRecordBtn = new TLButton("✖", TLButton.ButtonVariant.GHOST);
        cancelRecordBtn.setSize(TLButton.ButtonSize.SM);
        cancelRecordBtn.setStyle("-fx-text-fill: #e74c3c; -fx-cursor: hand;");

        recordingIndicator.getChildren().addAll(recordDot, recordTimer, recordHint, cancelRecordBtn);

        // Timer de mise à jour pendant l'enregistrement
        javafx.animation.Timeline recordTimerTimeline = new javafx.animation.Timeline(
                new javafx.animation.KeyFrame(Duration.seconds(1), ev -> {
                    if (audioRecorder.isRecording()) {
                        int elapsed = audioRecorder.getElapsedSeconds();
                        recordTimer.setText(com.skilora.community.service.AudioRecorderService.formatDuration(elapsed));
                        // Limite 5 minutes
                        if (elapsed >= 300) {
                            micBtn.fire(); // Auto-stop
                        }
                    }
                }));
        recordTimerTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);

        // Annuler l'enregistrement
        cancelRecordBtn.setOnAction(ev -> {
            audioRecorder.cancelRecording();
            recordTimerTimeline.stop();
            recordingIndicator.setVisible(false);
            recordingIndicator.setManaged(false);
            msgInput.setVisible(true);
            msgInput.setManaged(true);
            micBtn.setText("🎤");
            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
        });

        micBtn.setOnAction(ev -> {
            if (!audioRecorder.isRecording()) {
                // ── Démarrer l'enregistrement ──
                try {
                    audioRecorder.startRecording();
                    micBtn.setText("⏹");
                    micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #e74c3c;");
                    recordTimer.setText("0:00");
                    recordingIndicator.setVisible(true);
                    recordingIndicator.setManaged(true);
                    msgInput.setVisible(false);
                    msgInput.setManaged(false);
                    recordTimerTimeline.play();
                } catch (Exception ex) {
                    DialogUtils.showError("Microphone", ex.getMessage());
                }
            } else {
                // ── Arrêter et envoyer le vocal ──
                recordTimerTimeline.stop();
                recordingIndicator.setVisible(false);
                recordingIndicator.setManaged(false);
                msgInput.setVisible(true);
                msgInput.setManaged(true);
                micBtn.setText("⏳");
                micBtn.setDisable(true);

                java.io.File wavFile = audioRecorder.stopRecording();
                if (wavFile == null) {
                    micBtn.setText("🎤");
                    micBtn.setDisable(false);
                    micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                    return;
                }

                int durationSec = com.skilora.community.service.AudioRecorderService.getWavDurationSeconds(wavFile);

                new Thread(() -> {
                    try {
                        String audioUrl = cloudinaryService.uploadAudio(wavFile);
                        MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
                        MessagingService.getInstance().sendMessage(
                                conv.getId(), currentUser.getId(),
                                null, "VOCAL", audioUrl, wavFile.getName(), durationSec);
                        Platform.runLater(() -> {
                            micBtn.setText("🎤");
                            micBtn.setDisable(false);
                            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                            openConversationView(conv);
                            if (notificationService != null)
                                notificationService.pollNow();
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            micBtn.setText("🎤");
                            micBtn.setDisable(false);
                            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                            DialogUtils.showError("Erreur d'envoi vocal", ex.getMessage());
                        });
                    }
                }, "VocalUploadThread").start();
            }
        });

        TLButton sendBtn = new TLButton("➤  " + I18n.get("message.send"), TLButton.ButtonVariant.PRIMARY);
        sendBtn.setOnAction(e -> {
            if (audioRecorder.isRecording()) {
                micBtn.fire();
                return;
            }
            String text = msgInput.getText();
            // CONTRÔLE DE SAISIE : vérifier que le message n'est pas vide
            if (text != null && !text.trim().isEmpty()) {
                msgInput.setText(""); // Vider le champ immédiatement
                // Effacer le statut de saisie + envoyer le message
                new Thread(() -> {
                    MessagingService.getInstance().clearTypingStatus(conv.getId(), currentUser.getId());
                    MessagingService.getInstance().sendMessage(conv.getId(), currentUser.getId(), text.trim());
                    Platform.runLater(() -> {
                        openConversationView(conv); // Recharger la vue
                        if (notificationService != null)
                            notificationService.pollNow(); // Actualiser les notifications
                    });
                }, "SendMsgThread").start();
            }
        });

        inputBar.getChildren().addAll(msgInput, recordingIndicator, attachBtn, micBtn, chatEmojiBtn, sendBtn);

        // ── Assembler : header + messages + typing indicator + input bar ──
        chatPane.getChildren().addAll(chatHeader, scroll, typingIndicatorRow, inputBar);
        contentPane.getChildren().add(chatPane);

        // ── Arrêter le polling quand on quitte la conversation ──
        contentPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                typingPollTimeline.stop();
                dotAnimation.stop();
                onlinePollTimeline.stop();
                recordTimerTimeline.stop();
                if (audioRecorder.isRecording())
                    audioRecorder.cancelRecording();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════
    // ÉVÉNEMENTS — CRUD avec gestion par rôle
    // (Créer, Lire, Modifier, Supprimer, RSVP)
    // ═══════════════════════════════════════════════════════════

    /**
     * Charge l'onglet Événements.
     * - Admin/Employer/Trainer : peuvent créer des événements
     * - Tous : voient les événements à venir et peuvent s'inscrire (RSVP)
     */
    private void loadEventsTab() {
        contentPane.getChildren().clear();
        if (currentUser == null)
            return;

        VBox eventsPane = new VBox(12);

        // Bouton Créer un Événement (Admin, Employer, Trainer seulement)
        if (canCreateEvents()) {
            TLButton createEventBtn = new TLButton(I18n.get("event.new"), TLButton.ButtonVariant.PRIMARY);
            createEventBtn.setOnAction(e -> showEventDialog(null));
            HBox actionBar = new HBox(createEventBtn);
            actionBar.setAlignment(Pos.CENTER_RIGHT);
            eventsPane.getChildren().add(actionBar);
        }

        // Section "Mes événements" (si l'utilisateur est organisateur)
        if (canCreateEvents()) {
            Task<List<Event>> myEventsTask = new Task<>() {
                @Override
                protected List<Event> call() {
                    return EventService.getInstance().findByOrganizer(currentUser.getId());
                }
            };
            myEventsTask.setOnSucceeded(e -> {
                List<Event> myEvents = myEventsTask.getValue();
                if (!myEvents.isEmpty()) {
                    Label myLabel = new Label("My Events (" + myEvents.size() + ")");
                    myLabel.getStyleClass().add("h4");
                    eventsPane.getChildren().add(myLabel);
                    for (Event ev : myEvents) {
                        eventsPane.getChildren().add(createEventCard(ev, true));
                    }
                    eventsPane.getChildren().add(new TLSeparator());
                }
            });
            new Thread(myEventsTask, "MyEventsThread").start();
        }

        // Événements à venir (pour tous les utilisateurs)
        Task<List<Event>> upcomingTask = new Task<>() {
            @Override
            protected List<Event> call() {
                return EventService.getInstance().findUpcoming();
            }
        };
        upcomingTask.setOnSucceeded(e -> {
            List<Event> events = upcomingTask.getValue();
            Label upcoming = new Label(I18n.get("event.title") + " (" + events.size() + ")");
            upcoming.getStyleClass().add("h4");
            eventsPane.getChildren().add(upcoming);

            if (events.isEmpty()) {
                Label empty = new Label(I18n.get("event.empty"));
                empty.getStyleClass().add("text-muted");
                eventsPane.getChildren().add(empty);
            } else {
                for (Event event : events) {
                    eventsPane.getChildren().add(createEventCard(event, false));
                }
            }
        });
        new Thread(upcomingTask, "UpcomingEventsThread").start();

        contentPane.getChildren().add(eventsPane);
    }

    /**
     * Crée une carte d'événement avec détails et actions.
     * Affiche : titre, type, statut, date, lieu, participants, organisateur.
     * Actions : RSVP, Annuler RSVP, Modifier, Supprimer.
     *
     * @param event             l'événement à afficher
     * @param showManageActions true pour afficher les boutons de gestion
     * @return TLCard avec toutes les informations
     */
    private TLCard createEventCard(Event event, boolean showManageActions) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        VBox body = new VBox(10);
        body.setPadding(new Insets(20));

        // ── Bannière avec icône et titre ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        // Icône circulaire de l'événement
        StackPane eventIcon = createAvatar(event.getTitle(), 48);

        VBox titleBox = new VBox(3);
        Label title = new Label(event.getTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        HBox badges = new HBox(6);
        TLBadge statusBadge = new TLBadge(event.getStatus().name(),
                event.getStatus() == EventStatus.CANCELLED ? TLBadge.Variant.DESTRUCTIVE : TLBadge.Variant.SUCCESS);
        TLBadge typeBadge = new TLBadge(event.getEventType().name(), TLBadge.Variant.SECONDARY);
        badges.getChildren().addAll(statusBadge, typeBadge);
        titleBox.getChildren().addAll(title, badges);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        header.getChildren().addAll(eventIcon, titleBox);

        // ── Grille de détails ──
        VBox details = new VBox(6);
        details.setPadding(new Insets(4, 0, 4, 0));

        Label dateLabel = new Label("📅  " + I18n.get("event.date") + ": " + formatDate(event.getStartDate()));
        dateLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");

        Label locationLabel = new Label(
                event.isOnline()
                        ? "🌐  " + I18n.get("event.online")
                        : "📍  " + I18n.get("event.location") + ": "
                                + (event.getLocation() != null ? event.getLocation() : ""));
        locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");

        Label attendeesLabel = new Label("👥  " + I18n.get("event.attendees", event.getCurrentAttendees())
                + (event.getMaxAttendees() > 0 ? " / " + event.getMaxAttendees() : ""));
        attendeesLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");

        Label organizer = new Label(
                "🎤  Organizer: " + (event.getOrganizerName() != null ? event.getOrganizerName() : ""));
        organizer.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

        details.getChildren().addAll(dateLabel, locationLabel, attendeesLabel, organizer);

        // ── Boutons d'action ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        // Boutons RSVP si l'événement n'est pas annulé
        if (event.getStatus() != EventStatus.CANCELLED) {
            TLButton rsvpBtn = new TLButton("✓  " + I18n.get("event.rsvp.going"), TLButton.ButtonVariant.PRIMARY);
            rsvpBtn.setSize(TLButton.ButtonSize.SM);
            rsvpBtn.setOnAction(e -> {
                new Thread(() -> {
                    EventService.getInstance().rsvp(event.getId(), currentUser.getId(), "GOING");
                    Platform.runLater(() -> {
                        showToast(I18n.get("event.success.rsvp"));
                        loadEventsTab();
                    });
                }, "RSVPThread").start();
            });

            TLButton cancelRsvpBtn = new TLButton(I18n.get("event.rsvp.cancel"), TLButton.ButtonVariant.GHOST);
            cancelRsvpBtn.setSize(TLButton.ButtonSize.SM);
            cancelRsvpBtn.setOnAction(e -> {
                new Thread(() -> {
                    EventService.getInstance().cancelRsvp(event.getId(), currentUser.getId());
                    Platform.runLater(this::loadEventsTab);
                }, "CancelRSVPThread").start();
            });

            actions.getChildren().addAll(rsvpBtn, cancelRsvpBtn);
        }

        // Boutons Modifier/Supprimer pour le propriétaire ou admin
        if (showManageActions || canEditOrDelete(event.getOrganizerId())) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TLButton editBtn = new TLButton(I18n.get("post.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setSize(TLButton.ButtonSize.SM);
            editBtn.setOnAction(e -> showEventDialog(event));

            TLButton deleteBtn = new TLButton(I18n.get("post.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setSize(TLButton.ButtonSize.SM);
            deleteBtn.setOnAction(e -> deleteEvent(event));

            actions.getChildren().addAll(spacer, editBtn, deleteBtn);
        }

        body.getChildren().addAll(header, details, new TLSeparator(), actions);
        card.setContent(body);
        return card;
    }

    /**
     * Affiche le dialogue de création ou modification d'un événement.
     * 
     * CONTRÔLES DE SAISIE :
     * - Titre obligatoire (sinon DialogUtils.showError)
     * - Date au format yyyy-MM-dd HH:mm (sinon DialogUtils.showError)
     * - Max participants : doit être un nombre (0 = illimité)
     *
     * @param existingEvent l'événement à modifier, ou null pour créer
     */
    private void showEventDialog(Event existingEvent) {
        TLDialog<Void> dialog = new TLDialog<>();
        boolean isEdit = existingEvent != null;
        dialog.setDialogTitle(isEdit ? I18n.get("post.edit") + " Event" : I18n.get("event.new"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextField titleField = new TLTextField("Title", "Event title");
        TLTextarea descField = new TLTextarea("Description", "Event description");
        descField.getControl().setPrefRowCount(3);
        TLTextField locationField = new TLTextField(I18n.get("event.location"), "Location or online link");
        TLTextField startField = new TLTextField(I18n.get("event.date"), "yyyy-MM-dd HH:mm");
        TLTextField maxField = new TLTextField("Max Attendees", "0 = unlimited");

        TLSelect<String> typeSelect = new TLSelect<>("Event Type",
                "MEETUP", "WEBINAR", "WORKSHOP", "CONFERENCE", "NETWORKING");

        if (isEdit) {
            titleField.setText(existingEvent.getTitle());
            descField.setText(existingEvent.getDescription());
            locationField.setText(existingEvent.getLocation());
            if (existingEvent.getStartDate() != null)
                startField
                        .setText(existingEvent.getStartDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            maxField.setText(String.valueOf(existingEvent.getMaxAttendees()));
            typeSelect.setValue(existingEvent.getEventType().name());
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton saveBtn = new TLButton(I18n.get("common.save"), TLButton.ButtonVariant.PRIMARY);
        saveBtn.setOnAction(e -> {
            String t = titleField.getText();
            // CONTRÔLE DE SAISIE : titre obligatoire
            if (t == null || t.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"), "Title is required");
                return; // Bloquer la soumission
            }

            Event event = isEdit ? existingEvent : new Event();
            event.setTitle(t.trim());
            event.setDescription(descField.getText());
            event.setLocation(locationField.getText());
            try {
                // CONTRÔLE DE SAISIE : format de date valide (yyyy-MM-dd HH:mm)
                event.setStartDate(LocalDateTime.parse(startField.getText().trim(),
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            } catch (Exception ex) {
                // Format de date invalide : afficher une erreur
                DialogUtils.showError(I18n.get("message.error"), "Invalid date format (use yyyy-MM-dd HH:mm)");
                return; // Bloquer la soumission
            }
            try {
                // CONTRÔLE DE SAISIE : max participants doit être un nombre
                event.setMaxAttendees(Integer.parseInt(maxField.getText().trim()));
            } catch (NumberFormatException ex) {
                event.setMaxAttendees(0); // Par défaut : illimité
            }
            String selectedType = typeSelect.getValue();
            if (selectedType != null)
                event.setEventType(EventType.valueOf(selectedType));

            if (isEdit) {
                new Thread(() -> {
                    EventService.getInstance().update(event);
                    Platform.runLater(() -> {
                        showToast(I18n.get("event.success.created"));
                        loadEventsTab();
                    });
                }, "UpdateEventThread").start();
            } else {
                event.setOrganizerId(currentUser.getId());
                new Thread(() -> {
                    EventService.getInstance().create(event);
                    Platform.runLater(() -> {
                        showToast(I18n.get("event.success.created"));
                        loadEventsTab();
                    });
                }, "CreateEventThread").start();
            }
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);
        content.getChildren().addAll(titleField, descField, typeSelect, locationField, startField, maxField, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    /**
     * Supprime un événement après confirmation.
     *
     * @param event l'événement à supprimer
     */
    private void deleteEvent(Event event) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            EventService.getInstance().delete(event.getId());
                            Platform.runLater(this::loadEventsTab);
                        }, "DeleteEventThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // GROUPES — CRUD complet avec gestion des membres
    // (Créer, Lire, Modifier, Supprimer, Rejoindre, Quitter)
    // ═══════════════════════════════════════════════════════════

    /**
     * Charge l'onglet Groupes.
     * Affiche un champ de recherche, les groupes de l'utilisateur,
     * puis les groupes publics à découvrir (non encore rejoints).
     */
    private void loadGroupsTab() {
        contentPane.getChildren().clear();
        if (currentUser == null)
            return;

        VBox groupsPane = new VBox(14);
        groupsPane.setPadding(new Insets(4, 0, 0, 0));

        // ── Action bar : recherche + bouton créer ──
        HBox actionBar = new HBox(10);
        actionBar.setAlignment(Pos.CENTER_LEFT);

        TLTextField searchField = new TLTextField("", "🔍  Search groups...");
        searchField.getControl().setPrefWidth(280);
        HBox.setHgrow(searchField, Priority.ALWAYS);

        TLButton createGroupBtn = new TLButton("＋  " + I18n.get("group.new"), TLButton.ButtonVariant.PRIMARY);
        createGroupBtn.setOnAction(e -> showGroupDialog(null));

        actionBar.getChildren().addAll(searchField, createGroupBtn);
        groupsPane.getChildren().add(actionBar);

        // Container for dynamic group lists (replaced on search)
        VBox groupListContainer = new VBox(12);
        groupsPane.getChildren().add(groupListContainer);

        // Search handler
        searchField.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
            String query = newVal != null ? newVal.trim() : "";
            if (query.length() >= 2) {
                Task<List<CommunityGroup>> searchTask = new Task<>() {
                    @Override
                    protected List<CommunityGroup> call() {
                        return GroupService.getInstance().search(query);
                    }
                };
                searchTask.setOnSucceeded(ev -> {
                    groupListContainer.getChildren().clear();
                    List<CommunityGroup> results = searchTask.getValue();
                    Label resultLabel = new Label("🔎  Search Results (" + results.size() + ")");
                    resultLabel.getStyleClass().add("h4");
                    groupListContainer.getChildren().add(resultLabel);
                    if (results.isEmpty()) {
                        Label empty = new Label("No groups found matching \"" + query + "\"");
                        empty.getStyleClass().add("text-muted");
                        groupListContainer.getChildren().add(empty);
                    } else {
                        for (CommunityGroup g : results) {
                            boolean memberOfThis = GroupService.getInstance().isMember(g.getId(), currentUser.getId());
                            groupListContainer.getChildren().add(createGroupCard(g, memberOfThis));
                        }
                    }
                });
                new Thread(searchTask, "SearchGroupsThread").start();
            } else if (query.isEmpty()) {
                loadGroupLists(groupListContainer);
            }
        });

        // Initial load
        loadGroupLists(groupListContainer);

        contentPane.getChildren().add(groupsPane);
    }

    /**
     * Charge les listes de groupes (Mes Groupes + Découvrir).
     * Utilisée pour l'affichage initial et le reset après effacement de recherche.
     *
     * @param container le conteneur VBox à remplir
     */
    private void loadGroupLists(VBox container) {
        container.getChildren().clear();

        // ── My Groups ──
        Task<List<CommunityGroup>> myGroupsTask = new Task<>() {
            @Override
            protected List<CommunityGroup> call() {
                return GroupService.getInstance().findByMember(currentUser.getId());
            }
        };
        myGroupsTask.setOnSucceeded(e -> {
            List<CommunityGroup> myGroups = myGroupsTask.getValue();

            // My Groups section
            if (!myGroups.isEmpty()) {
                Label myLabel = new Label("👤  My Groups (" + myGroups.size() + ")");
                myLabel.getStyleClass().add("h4");
                container.getChildren().add(myLabel);
                for (CommunityGroup g : myGroups) {
                    container.getChildren().add(createGroupCard(g, true));
                }
                container.getChildren().add(new TLSeparator());
            }

            // ── Discover Groups (public groups user is NOT a member of) ──
            // Collect IDs of groups the user already joined
            java.util.Set<Integer> myGroupIds = new java.util.HashSet<>();
            for (CommunityGroup g : myGroups) {
                myGroupIds.add(g.getId());
            }

            Task<List<CommunityGroup>> allGroupsTask = new Task<>() {
                @Override
                protected List<CommunityGroup> call() {
                    return GroupService.getInstance().findAll();
                }
            };
            allGroupsTask.setOnSucceeded(e2 -> {
                List<CommunityGroup> allGroups = allGroupsTask.getValue();

                // Filter out groups user already belongs to
                List<CommunityGroup> discoverGroups = new ArrayList<>();
                for (CommunityGroup g : allGroups) {
                    if (!myGroupIds.contains(g.getId())) {
                        discoverGroups.add(g);
                    }
                }

                Label discoverLabel = new Label("🌐  Discover Groups (" + discoverGroups.size() + ")");
                discoverLabel.getStyleClass().add("h4");
                container.getChildren().add(discoverLabel);

                if (discoverGroups.isEmpty()) {
                    Label empty = new Label(
                            "You've joined all available groups! Create a new one to start a community.");
                    empty.getStyleClass().add("text-muted");
                    container.getChildren().add(empty);
                } else {
                    for (CommunityGroup g : discoverGroups) {
                        container.getChildren().add(createGroupCard(g, false));
                    }
                }
            });
            new Thread(allGroupsTask, "AllGroupsThread").start();
        });
        new Thread(myGroupsTask, "MyGroupsThread").start();
    }

    /**
     * Crée une carte de groupe avec détails et actions.
     * Actions possibles : Voir / Rejoindre / Quitter / Modifier / Supprimer.
     *
     * @param group    le groupe à afficher
     * @param isMember true si l'utilisateur est déjà membre
     * @return TLCard avec toutes les informations du groupe
     */
    private TLCard createGroupCard(CommunityGroup group, boolean isMember) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        VBox body = new VBox(10);
        body.setPadding(new Insets(20));

        // ── En-tête avec icône avatar ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane groupIcon = createAvatar(group.getName(), 48); // Icône circulaire

        VBox titleBox = new VBox(3);
        Label name = new Label(group.getName());
        name.setStyle("-fx-font-size: 16px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        HBox badges = new HBox(6);
        TLBadge categoryBadge = new TLBadge(
                group.getCategory() != null ? group.getCategory() : "General", TLBadge.Variant.SECONDARY);
        TLBadge accessBadge = new TLBadge(
                group.isPublic() ? "Public" : "Private", TLBadge.Variant.OUTLINE);
        if (isMember) {
            TLBadge memberBadge = new TLBadge("✓ Member", TLBadge.Variant.SUCCESS);
            badges.getChildren().addAll(categoryBadge, accessBadge, memberBadge);
        } else {
            badges.getChildren().addAll(categoryBadge, accessBadge);
        }
        titleBox.getChildren().addAll(name, badges);
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        header.getChildren().addAll(groupIcon, titleBox);

        // ── Description du groupe ──
        Label desc = new Label(group.getDescription() != null ? group.getDescription() : "");
        desc.setWrapText(true);
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");
        desc.setMaxHeight(40);

        // ── Statistiques du groupe ──
        HBox stats = new HBox(16);
        Label members = new Label("👥  " + I18n.get("group.members", group.getMemberCount()));
        members.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
        Label creator = new Label("Created by " + (group.getCreatorName() != null ? group.getCreatorName() : ""));
        creator.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        stats.getChildren().addAll(members, creator);

        // ── Actions du groupe ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        // Bouton VOIR le détail du groupe
        TLButton viewBtn = new TLButton("👁  View", TLButton.ButtonVariant.OUTLINE);
        viewBtn.setSize(TLButton.ButtonSize.SM);
        viewBtn.setOnAction(e -> openGroupDetail(group, isMember));
        actions.getChildren().add(viewBtn);

        if (isMember) {
            // Bouton QUITTER le groupe
            TLButton leaveBtn = new TLButton(I18n.get("group.leave"), TLButton.ButtonVariant.GHOST);
            leaveBtn.setSize(TLButton.ButtonSize.SM);
            leaveBtn.setOnAction(e -> {
                if (group.getCreatorId() == currentUser.getId()) {
                    DialogUtils.showError("Cannot leave", "You are the group creator. Delete the group instead.");
                    return;
                }
                new Thread(() -> {
                    GroupService.getInstance().leave(group.getId(), currentUser.getId());
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.left"));
                        loadGroupsTab();
                    });
                }, "LeaveGroupThread").start();
            });
            actions.getChildren().add(leaveBtn);
        } else {
            // Bouton REJOINDRE le groupe
            TLButton joinBtn = new TLButton("＋  " + I18n.get("group.join"), TLButton.ButtonVariant.PRIMARY);
            joinBtn.setSize(TLButton.ButtonSize.SM);
            joinBtn.setOnAction(e -> {
                new Thread(() -> {
                    GroupService.getInstance().join(group.getId(), currentUser.getId());
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.joined"));
                        loadGroupsTab();
                    });
                }, "JoinGroupThread").start();
            });
            actions.getChildren().add(joinBtn);
        }

        // Boutons Modifier/Supprimer pour le propriétaire ou admin
        if (canEditOrDelete(group.getCreatorId())) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TLButton editBtn = new TLButton(I18n.get("post.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setSize(TLButton.ButtonSize.SM);
            editBtn.setOnAction(e -> showGroupDialog(group));

            TLButton deleteBtn = new TLButton(I18n.get("post.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setSize(TLButton.ButtonSize.SM);
            deleteBtn.setOnAction(e -> deleteGroup(group));

            actions.getChildren().addAll(spacer, editBtn, deleteBtn);
        }

        body.getChildren().addAll(header, desc, stats, new TLSeparator(), actions);
        card.setContent(body);
        return card;
    }

    /**
     * Affiche la vue détaillée d'un groupe avec la liste de ses membres.
     * Comprend : bannière, description, statistiques, actions (Join/Leave),
     * et la liste complète des membres avec leur rôle.
     *
     * @param group    le groupe à afficher
     * @param isMember true si l'utilisateur est membre de ce groupe
     */
    private void openGroupDetail(CommunityGroup group, boolean isMember) {
        contentPane.getChildren().clear();

        VBox detailPane = new VBox(16);
        detailPane.setPadding(new Insets(20));

        // ── Bouton retour ──
        TLButton backBtn = new TLButton("←  " + I18n.get("community.tab.groups"), TLButton.ButtonVariant.GHOST);
        backBtn.setOnAction(e -> loadGroupsTab());

        // ── En-tête du groupe ──
        HBox header = new HBox(16);
        header.setAlignment(Pos.CENTER_LEFT);

        StackPane groupAvatar = createAvatar(group.getName(), 64);

        VBox titleBox = new VBox(6);
        Label groupName = new Label(group.getName());
        groupName.setStyle("-fx-font-size: 22px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");

        HBox badges = new HBox(8);
        TLBadge categoryBadge = new TLBadge(
                group.getCategory() != null ? group.getCategory() : "General", TLBadge.Variant.SECONDARY);
        TLBadge accessBadge = new TLBadge(
                group.isPublic() ? "Public" : "Private", TLBadge.Variant.OUTLINE);
        TLBadge memberCountBadge = new TLBadge(group.getMemberCount() + " members", TLBadge.Variant.DEFAULT);
        badges.getChildren().addAll(categoryBadge, accessBadge, memberCountBadge);

        Label creatorLabel = new Label(
                "Created by " + (group.getCreatorName() != null ? group.getCreatorName() : "Unknown"));
        creatorLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

        if (group.getCreatedDate() != null) {
            Label dateLabel = new Label("📅  " + formatDate(group.getCreatedDate()));
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
            titleBox.getChildren().addAll(groupName, badges, creatorLabel, dateLabel);
        } else {
            titleBox.getChildren().addAll(groupName, badges, creatorLabel);
        }
        HBox.setHgrow(titleBox, Priority.ALWAYS);

        header.getChildren().addAll(groupAvatar, titleBox);

        // ── Description ──
        VBox descSection = new VBox(6);
        Label descTitle = new Label("📝  About this group");
        descTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-foreground;");
        Label descContent = new Label(group.getDescription() != null && !group.getDescription().isBlank()
                ? group.getDescription()
                : "No description provided.");
        descContent.setWrapText(true);
        descContent.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground; -fx-line-spacing: 4;");
        descSection.getChildren().addAll(descTitle, descContent);

        // ── Action buttons ──
        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(Pos.CENTER_LEFT);
        actionButtons.setPadding(new Insets(4, 0, 4, 0));

        if (isMember) {
            TLButton leaveBtn = new TLButton("🚪  " + I18n.get("group.leave"), TLButton.ButtonVariant.GHOST);
            leaveBtn.setOnAction(e -> {
                if (group.getCreatorId() == currentUser.getId()) {
                    DialogUtils.showError("Cannot leave", "You are the group creator. Delete the group instead.");
                    return;
                }
                new Thread(() -> {
                    GroupService.getInstance().leave(group.getId(), currentUser.getId());
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.left"));
                        loadGroupsTab();
                    });
                }, "LeaveGroupThread").start();
            });
            actionButtons.getChildren().add(leaveBtn);
        } else {
            TLButton joinBtn = new TLButton("＋  " + I18n.get("group.join"), TLButton.ButtonVariant.PRIMARY);
            joinBtn.setOnAction(e -> {
                new Thread(() -> {
                    GroupService.getInstance().join(group.getId(), currentUser.getId());
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.joined"));
                        // Refresh: reopen the detail as member
                        openGroupDetail(group, true);
                    });
                }, "JoinGroupThread").start();
            });
            actionButtons.getChildren().add(joinBtn);
        }

        if (canEditOrDelete(group.getCreatorId())) {
            TLButton editBtn = new TLButton(I18n.get("post.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setOnAction(e -> showGroupDialog(group));

            TLButton deleteBtn = new TLButton(I18n.get("post.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setOnAction(e -> deleteGroup(group));

            actionButtons.getChildren().addAll(editBtn, deleteBtn);
        }

        detailPane.getChildren().addAll(backBtn, header, descSection, new TLSeparator(), actionButtons,
                new TLSeparator());

        // ── Members section ──
        Label membersTitle = new Label("👥  Members");
        membersTitle.getStyleClass().add("h4");
        detailPane.getChildren().add(membersTitle);

        VBox membersContainer = new VBox(8);
        Task<List<GroupMember>> membersTask = new Task<>() {
            @Override
            protected List<GroupMember> call() {
                return GroupService.getInstance().getMembers(group.getId());
            }
        };
        membersTask.setOnSucceeded(ev -> {
            List<GroupMember> membersList = membersTask.getValue();
            if (membersList.isEmpty()) {
                Label noMembers = new Label("No members yet.");
                noMembers.getStyleClass().add("text-muted");
                membersContainer.getChildren().add(noMembers);
            } else {
                for (GroupMember member : membersList) {
                    TLCard memberCard = new TLCard();
                    HBox memberRow = new HBox(12);
                    memberRow.setAlignment(Pos.CENTER_LEFT);
                    memberRow.setPadding(new Insets(10, 16, 10, 16));

                    String memberName = member.getUserName() != null ? member.getUserName()
                            : "User #" + member.getUserId();
                    StackPane memberAvatar = createAvatar(memberName, 36);

                    VBox memberInfo = new VBox(2);
                    Label nameLabel = new Label(memberName);
                    nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-foreground;");

                    HBox memberBadges = new HBox(6);
                    String roleStr = member.getRole() != null ? member.getRole() : "MEMBER";
                    TLBadge.Variant roleVariant = switch (roleStr) {
                        case "ADMIN" -> TLBadge.Variant.DESTRUCTIVE;
                        case "MODERATOR" -> TLBadge.Variant.OUTLINE;
                        default -> TLBadge.Variant.SECONDARY;
                    };
                    TLBadge roleBadge = new TLBadge(roleStr, roleVariant);
                    memberBadges.getChildren().add(roleBadge);

                    if (member.getJoinedDate() != null) {
                        Label joinDate = new Label("Joined " + formatDate(member.getJoinedDate()));
                        joinDate.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
                        memberInfo.getChildren().addAll(nameLabel, memberBadges, joinDate);
                    } else {
                        memberInfo.getChildren().addAll(nameLabel, memberBadges);
                    }
                    HBox.setHgrow(memberInfo, Priority.ALWAYS);

                    memberRow.getChildren().addAll(memberAvatar, memberInfo);

                    // Message button for members (if it's not the current user)
                    if (member.getUserId() != currentUser.getId()) {
                        TLButton msgBtn = new TLButton("💬", TLButton.ButtonVariant.GHOST);
                        msgBtn.setSize(TLButton.ButtonSize.SM);
                        msgBtn.setOnAction(e -> openConversation(member.getUserId(), memberName));
                        memberRow.getChildren().add(msgBtn);
                    }

                    memberCard.setContent(memberRow);
                    membersContainer.getChildren().add(memberCard);
                }
            }
        });
        new Thread(membersTask, "GroupMembersThread").start();

        detailPane.getChildren().add(membersContainer);

        // ── Group Discussion (Only for members) ──
        if (isMember) {
            Label discussionTitle = new Label("💬  Group Discussion");
            discussionTitle.getStyleClass().add("h4");

            // Bouton Résumé IA pour le groupe
            Button groupSummaryBtn = new Button("📝 Résumé IA");
            groupSummaryBtn.getStyleClass().addAll("btn", "btn-outline");
            groupSummaryBtn.setStyle("-fx-font-size: 11px; -fx-padding: 4 12; -fx-cursor: hand;");

            HBox discussionHeader = new HBox(10);
            discussionHeader.setAlignment(Pos.CENTER_LEFT);
            Region discussionSpacer = new Region();
            HBox.setHgrow(discussionSpacer, Priority.ALWAYS);
            discussionHeader.getChildren().addAll(discussionTitle, discussionSpacer, groupSummaryBtn);

            VBox chatContainer = new VBox(10);
            chatContainer.setPadding(new Insets(10));
            chatContainer.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8px;");

            // Messages wrapper
            VBox messagesBox = new VBox(8);
            messagesBox.setPrefHeight(300);
            javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(messagesBox);
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setVbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.AS_NEEDED);
            scrollPane.setHbarPolicy(javafx.scene.control.ScrollPane.ScrollBarPolicy.NEVER);
            scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent; -fx-padding: 0;");

            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            // Enregistrer l'ID du dernier message chargé pour éviter de tout recréer
            final int[] lastLoadedMessageId = { 0 };

            // Holder so the lambda can reference itself (Java requires effectively-final locals)
            final Runnable[] reloadMessagesHolder = { null };
            Runnable reloadMessages = () -> {
                new Thread(() -> {
                    List<GroupMessage> msgs = GroupService.getInstance().getMessages(group.getId());
                    // Mark other users' messages as read
                    GroupService.getInstance().markMessagesAsRead(group.getId(), currentUser.getId());
                    // Get read counts for my messages
                    java.util.Map<Integer, Integer> readCounts = GroupService.getInstance()
                            .getReadCountsForUserMessages(group.getId(), currentUser.getId());
                    // Fetch reactions for group messages
                    java.util.Map<Integer, java.util.Map<String, Integer>> allReactions =
                            GroupService.getInstance().getReactionsForGroup(group.getId());
                    java.util.Map<Integer, java.util.Set<String>> myReactions =
                            GroupService.getInstance().getUserReactionsForGroup(group.getId(), currentUser.getId());
                    Platform.runLater(() -> {
                        messagesBox.getChildren().clear();
                        for (GroupMessage m : msgs) {
                            int readBy = readCounts.getOrDefault(m.getId(), 0);
                            java.util.Map<String, Integer> msgReactions = allReactions.getOrDefault(m.getId(), java.util.Collections.emptyMap());
                            java.util.Set<String> msgMyReactions = myReactions.getOrDefault(m.getId(), java.util.Collections.emptySet());
                            messagesBox.getChildren().add(createGroupMessageView(m, readBy, msgReactions, msgMyReactions, reloadMessagesHolder[0]));
                            lastLoadedMessageId[0] = Math.max(lastLoadedMessageId[0], m.getId());
                        }
                        scrollPane.setVvalue(1.0);
                    });
                }).start();
            };
            reloadMessagesHolder[0] = reloadMessages;
            reloadMessages.run();

            // ── Action du bouton Résumé IA (groupe) ──
            groupSummaryBtn.setOnAction(ev -> {
                groupSummaryBtn.setDisable(true);
                groupSummaryBtn.setText("⏳ Résumé en cours...");
                new Thread(() -> {
                    List<GroupMessage> allMsgs = GroupService.getInstance().getMessages(group.getId());
                    List<String> formatted = new ArrayList<>();
                    for (GroupMessage gm : allMsgs) {
                        String sender = gm.getSenderName() != null ? gm.getSenderName() : "Utilisateur #" + gm.getSenderId();
                        String text = gm.getContent() != null && !gm.getContent().isEmpty() ? gm.getContent() : "[" + gm.getMessageType() + "]";
                        formatted.add(sender + ": " + text);
                    }
                    String summary = AISummaryService.getInstance().summarize(formatted);
                    Platform.runLater(() -> {
                        groupSummaryBtn.setDisable(false);
                        groupSummaryBtn.setText("📝 Résumé IA");
                        // Afficher le résumé dans un TLDialog
                        TLDialog<Void> summaryDialog = new TLDialog<>();
                        summaryDialog.setDialogTitle("📝 Résumé IA - " + group.getName());
                        summaryDialog.setDescription("Résumé généré par intelligence artificielle");
                        Label summaryLabel = new Label(summary);
                        summaryLabel.setWrapText(true);
                        summaryLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground; -fx-line-spacing: 4;");
                        summaryLabel.setMaxWidth(500);
                        javafx.scene.control.ScrollPane summaryScroll = new javafx.scene.control.ScrollPane(summaryLabel);
                        summaryScroll.setFitToWidth(true);
                        summaryScroll.setPrefHeight(300);
                        summaryScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
                        summaryDialog.setContent(summaryScroll);
                        summaryDialog.addButton(ButtonType.OK);
                        summaryDialog.setResultConverter(bt -> null);
                        summaryDialog.showAndWait();
                    });
                }, "GroupAISummaryThread").start();
            });

            // ── POLLING TEMPS REEL (MESSAGES ET TYPING) ──
            HBox typingIndicatorRow = new HBox(8);
            typingIndicatorRow.setAlignment(Pos.CENTER_LEFT);
            typingIndicatorRow.setPadding(new Insets(2, 16, 2, 16));
            typingIndicatorRow.setVisible(false);
            typingIndicatorRow.setManaged(false);
            Label typingText = new Label("... est en train d'écrire");
            typingText.getStyleClass().add("typing-indicator");
            typingIndicatorRow.getChildren().addAll(typingText);

            javafx.animation.Timeline groupPollTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.millis(3000), ev -> {
                        new Thread(() -> {
                            // Check messages
                            List<GroupMessage> newMsgs = GroupService.getInstance().getMessages(group.getId());
                            // Mark messages as read
                            GroupService.getInstance().markMessagesAsRead(group.getId(), currentUser.getId());
                            // Get read counts for my messages
                            java.util.Map<Integer, Integer> readCounts = GroupService.getInstance()
                                    .getReadCountsForUserMessages(group.getId(), currentUser.getId());
                            // Fetch reactions for group messages
                            java.util.Map<Integer, java.util.Map<String, Integer>> allReactions =
                                    GroupService.getInstance().getReactionsForGroup(group.getId());
                            java.util.Map<Integer, java.util.Set<String>> myReactions =
                                    GroupService.getInstance().getUserReactionsForGroup(group.getId(), currentUser.getId());
                            // Check typing
                            List<String> typers = GroupService.getInstance().getTypingUsers(group.getId(),
                                    currentUser.getId());

                            Platform.runLater(() -> {
                                boolean hasNew = false;
                                for (GroupMessage m : newMsgs) {
                                    if (m.getId() > lastLoadedMessageId[0]) {
                                        int readBy = readCounts.getOrDefault(m.getId(), 0);
                                        java.util.Map<String, Integer> msgReactions = allReactions.getOrDefault(m.getId(), java.util.Collections.emptyMap());
                                        java.util.Set<String> msgMyReactions = myReactions.getOrDefault(m.getId(), java.util.Collections.emptySet());
                                        messagesBox.getChildren().add(createGroupMessageView(m, readBy, msgReactions, msgMyReactions, reloadMessages));
                                        lastLoadedMessageId[0] = m.getId();
                                        hasNew = true;
                                    }
                                }

                                // Update seen status on existing messages
                                for (javafx.scene.Node node : messagesBox.getChildren()) {
                                    if (node instanceof HBox msgRow && node.getUserData() instanceof Integer msgId) {
                                        int readBy = readCounts.getOrDefault(msgId, 0);
                                        // Find the status label inside the bubble
                                        for (javafx.scene.Node child : msgRow.getChildren()) {
                                            if (child instanceof VBox bubbleBox) {
                                                for (javafx.scene.Node bubbleChild : bubbleBox.getChildren()) {
                                                    if (bubbleChild instanceof HBox statusRow
                                                            && statusRow.getStyleClass().contains("group-msg-status-row")) {
                                                        for (javafx.scene.Node statusChild : statusRow.getChildren()) {
                                                            if (statusChild instanceof Label lbl
                                                                    && lbl.getStyleClass().contains("msg-seen-indicator")) {
                                                                if (readBy > 0) {
                                                                    lbl.setText("✓✓ Vu");
                                                                    lbl.setStyle("-fx-text-fill: #3b82f6; -fx-font-size: 10px;");
                                                                } else {
                                                                    lbl.setText("✓ Envoyé");
                                                                    lbl.setStyle("-fx-text-fill: -fx-primary-foreground; -fx-font-size: 10px; -fx-opacity: 0.7;");
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                if (hasNew)
                                    scrollPane.setVvalue(1.0);

                                if (typers.isEmpty()) {
                                    typingIndicatorRow.setVisible(false);
                                    typingIndicatorRow.setManaged(false);
                                } else {
                                    typingIndicatorRow.setVisible(true);
                                    typingIndicatorRow.setManaged(true);
                                    typingText.setText(String.join(", ", typers) + " en train d'écrire...");
                                }
                            });
                        }).start();
                    }));
            groupPollTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
            groupPollTimeline.play();

            // Stop polling on close
            contentPane.sceneProperty().addListener((obs, os, ns) -> {
                if (ns == null)
                    groupPollTimeline.stop();
            });

            // Input area
            HBox inputBar = new HBox(10);
            inputBar.setAlignment(Pos.CENTER_LEFT);
            inputBar.getStyleClass().add("chat-input-bar");
            inputBar.setPadding(new Insets(10, 14, 10, 14));

            TLTextField msgInput = new TLTextField("", I18n.get("message.placeholder"));
            HBox.setHgrow(msgInput, Priority.ALWAYS);

            msgInput.getControl().textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.trim().isEmpty()) {
                    new Thread(() -> GroupService.getInstance().updateTypingStatus(group.getId(), currentUser.getId()))
                            .start();
                }
            });

            TLButton chatEmojiBtn = new TLButton("😀", TLButton.ButtonVariant.GHOST);
            chatEmojiBtn.setSize(TLButton.ButtonSize.SM);
            chatEmojiBtn.setOnAction(ev -> showEmojiPickerForTextField(chatEmojiBtn, msgInput));

            TLButton attachBtn = new TLButton("📎", TLButton.ButtonVariant.GHOST);
            attachBtn.setSize(TLButton.ButtonSize.SM);
            attachBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
            attachBtn.setOnAction(ev -> {
                javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
                fileChooser.getExtensionFilters().addAll(
                        new javafx.stage.FileChooser.ExtensionFilter("Tous les médias",
                                cloudinaryService.getAllowedMediaExtensionPatterns()));
                java.io.File file = fileChooser.showOpenDialog(contentPane.getScene().getWindow());
                if (file != null) {
                    attachBtn.setText("⏳");
                    attachBtn.setDisable(true);
                    new Thread(() -> {
                        try {
                            String mediaType = cloudinaryService.detectMediaType(file);
                            String mediaUrl = "VIDEO".equals(mediaType) ? cloudinaryService.uploadVideo(file)
                                    : cloudinaryService.uploadImage(file);
                            GroupService.getInstance().clearTypingStatus(group.getId(), currentUser.getId());
                            GroupMessage newMsg = new GroupMessage();
                            newMsg.setGroupId(group.getId());
                            newMsg.setSenderId(currentUser.getId());
                            newMsg.setContent(msgInput.getText().trim());
                            newMsg.setMessageType(mediaType);
                            newMsg.setMediaUrl(mediaUrl);
                            newMsg.setFileName(file.getName());
                            GroupService.getInstance().addMessage(newMsg);
                            reloadMessages.run();
                            Platform.runLater(() -> {
                                attachBtn.setText("📎");
                                attachBtn.setDisable(false);
                                msgInput.setText("");
                            });
                        } catch (Exception ex) {
                            Platform.runLater(() -> {
                                attachBtn.setText("📎");
                                attachBtn.setDisable(false);
                            });
                        }
                    }).start();
                }
            });

            // Bouton Micro
            com.skilora.community.service.AudioRecorderService audioRecorder = com.skilora.community.service.AudioRecorderService
                    .getInstance();
            TLButton micBtn = new TLButton("🎤", TLButton.ButtonVariant.GHOST);
            micBtn.setSize(TLButton.ButtonSize.SM);
            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");

            HBox recordingIndicator = new HBox(8);
            recordingIndicator.setAlignment(Pos.CENTER_LEFT);
            recordingIndicator.setVisible(false);
            recordingIndicator.setManaged(false);
            Label recordTimer = new Label("0:00");
            recordTimer.setStyle("-fx-font-size: 13px; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            TLButton cancelRecordBtn = new TLButton("✖", TLButton.ButtonVariant.GHOST);
            recordingIndicator.getChildren().addAll(new Label("🔴"), recordTimer, new Label("Enregistrement..."),
                    cancelRecordBtn);

            javafx.animation.Timeline recordTimerTimeline = new javafx.animation.Timeline(
                    new javafx.animation.KeyFrame(Duration.seconds(1), ev -> {
                        if (audioRecorder.isRecording()) {
                            int elapsed = audioRecorder.getElapsedSeconds();
                            recordTimer.setText(
                                    com.skilora.community.service.AudioRecorderService.formatDuration(elapsed));
                            if (elapsed >= 300)
                                micBtn.fire();
                        }
                    }));
            recordTimerTimeline.setCycleCount(javafx.animation.Animation.INDEFINITE);

            cancelRecordBtn.setOnAction(ev -> {
                audioRecorder.cancelRecording();
                recordTimerTimeline.stop();
                recordingIndicator.setVisible(false);
                recordingIndicator.setManaged(false);
                msgInput.setVisible(true);
                msgInput.setManaged(true);
                micBtn.setText("🎤");
            });

            micBtn.setOnAction(ev -> {
                if (!audioRecorder.isRecording()) {
                    try {
                        audioRecorder.startRecording();
                        micBtn.setText("⏹");
                        micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand; -fx-text-fill: #e74c3c;");
                        recordTimer.setText("0:00");
                        recordingIndicator.setVisible(true);
                        recordingIndicator.setManaged(true);
                        msgInput.setVisible(false);
                        msgInput.setManaged(false);
                        recordTimerTimeline.play();
                    } catch (Exception ex) {
                    }
                } else {
                    recordTimerTimeline.stop();
                    recordingIndicator.setVisible(false);
                    recordingIndicator.setManaged(false);
                    msgInput.setVisible(true);
                    msgInput.setManaged(true);
                    micBtn.setText("⏳");
                    micBtn.setDisable(true);
                    final int elapsedRec = audioRecorder.getElapsedSeconds();
                    java.io.File wavFile = audioRecorder.stopRecording();
                    if (wavFile == null) {
                        micBtn.setText("🎤");
                        micBtn.setDisable(false);
                        return;
                    }
                    new Thread(() -> {
                        try {
                            String mediaUrl = cloudinaryService.uploadAudio(wavFile);
                            GroupService.getInstance().clearTypingStatus(group.getId(), currentUser.getId());
                            GroupMessage newMsg = new GroupMessage();
                            newMsg.setGroupId(group.getId());
                            newMsg.setSenderId(currentUser.getId());
                            newMsg.setContent("");
                            newMsg.setMessageType("VOCAL");
                            newMsg.setMediaUrl(mediaUrl);
                            newMsg.setFileName(wavFile.getName());
                            newMsg.setDuration(elapsedRec > 0 ? elapsedRec : com.skilora.community.service.AudioRecorderService.getWavDurationSeconds(wavFile));
                            GroupService.getInstance().addMessage(newMsg);
                            reloadMessages.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            micBtn.setText("🎤");
                            micBtn.setDisable(false);
                            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                        });
                    }).start();
                }
            });

            TLButton sendBtn = new TLButton("➤  " + I18n.get("message.send"), TLButton.ButtonVariant.PRIMARY);
            Runnable sendAction = () -> {
                if (audioRecorder.isRecording()) {
                    // ── Arrêter l'enregistrement et envoyer le vocal directement ──
                    recordTimerTimeline.stop();
                    recordingIndicator.setVisible(false);
                    recordingIndicator.setManaged(false);
                    msgInput.setVisible(true);
                    msgInput.setManaged(true);
                    micBtn.setText("⏳");
                    micBtn.setDisable(true);
                    sendBtn.setDisable(true);
                    final int elapsedRec = audioRecorder.getElapsedSeconds();
                    java.io.File wavFile = audioRecorder.stopRecording();
                    if (wavFile == null) {
                        micBtn.setText("🎤");
                        micBtn.setDisable(false);
                        micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                        sendBtn.setDisable(false);
                        return;
                    }
                    new Thread(() -> {
                        try {
                            String mediaUrl = cloudinaryService.uploadAudio(wavFile);
                            GroupService.getInstance().clearTypingStatus(group.getId(), currentUser.getId());
                            GroupMessage newMsg = new GroupMessage();
                            newMsg.setGroupId(group.getId());
                            newMsg.setSenderId(currentUser.getId());
                            newMsg.setContent("");
                            newMsg.setMessageType("VOCAL");
                            newMsg.setMediaUrl(mediaUrl);
                            newMsg.setFileName(wavFile.getName());
                            newMsg.setDuration(elapsedRec > 0 ? elapsedRec : com.skilora.community.service.AudioRecorderService.getWavDurationSeconds(wavFile));
                            GroupService.getInstance().addMessage(newMsg);
                            reloadMessages.run();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                        Platform.runLater(() -> {
                            micBtn.setText("🎤");
                            micBtn.setDisable(false);
                            micBtn.setStyle("-fx-font-size: 16px; -fx-cursor: hand;");
                            sendBtn.setDisable(false);
                        });
                    }).start();
                    return;
                }
                String text = msgInput.getText();
                if (text != null && !text.trim().isEmpty()) {
                    msgInput.setText("");
                    new Thread(() -> {
                        GroupService.getInstance().clearTypingStatus(group.getId(), currentUser.getId());
                        GroupMessage newMsg = new GroupMessage();
                        newMsg.setGroupId(group.getId());
                        newMsg.setSenderId(currentUser.getId());
                        newMsg.setContent(text.trim());
                        newMsg.setMessageType("TEXT");
                        GroupService.getInstance().addMessage(newMsg);
                        reloadMessages.run();
                    }).start();
                }
            };
            sendBtn.setOnAction(e -> sendAction.run());
            msgInput.getControl().setOnAction(e -> sendAction.run());

            inputBar.getChildren().addAll(chatEmojiBtn, attachBtn, recordingIndicator, msgInput, micBtn, sendBtn);
            chatContainer.getChildren().addAll(scrollPane, typingIndicatorRow, inputBar);

            // ── Layout: group info scrolls on top, chat pinned at bottom ──
            javafx.scene.control.ScrollPane infoScroll = new javafx.scene.control.ScrollPane(detailPane);
            infoScroll.setFitToWidth(true);
            infoScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            VBox.setVgrow(infoScroll, Priority.ALWAYS);

            VBox chatSection = new VBox(8);
            chatSection.setPadding(new Insets(0, 20, 10, 20));
            chatSection.getChildren().addAll(new TLSeparator(), discussionHeader, chatContainer);

            VBox groupLayout = new VBox();
            VBox.setVgrow(groupLayout, Priority.ALWAYS);
            groupLayout.getChildren().addAll(infoScroll, chatSection);

            // Make outer ScrollPane fill viewport height so chat stays pinned
            if (contentPane.getParent() != null && contentPane.getParent().getParent() instanceof javafx.scene.control.ScrollPane outerScroll) {
                outerScroll.setFitToHeight(true);
                // Reset fitToHeight when navigating away (tab switch clears children)
                javafx.collections.ListChangeListener<javafx.scene.Node> resetListener = new javafx.collections.ListChangeListener<>() {
                    @Override
                    public void onChanged(Change<? extends javafx.scene.Node> c) {
                        outerScroll.setFitToHeight(false);
                        contentPane.getChildren().removeListener(this);
                    }
                };
                contentPane.getChildren().addListener(resetListener);
            }

            contentPane.getChildren().add(groupLayout);
            return;
        }

        contentPane.getChildren().add(detailPane);
    }

    // ═══════════════════════════════════════════════════════════
    //  REACTIONS — UI pour réagir aux messages (emoji)
    // ═══════════════════════════════════════════════════════════

    private static final String[] REACTION_EMOJIS = { "👍", "❤️", "😂", "😮", "😢", "🔥" };

    /**
     * Creates the reaction bar displayed below a message bubble.
     * Shows existing reactions with counts + a "+" button to add new ones.
     *
     * @param msgId          the message ID
     * @param isMine         whether this is the current user's message
     * @param reactions      map of emoji -> count for this message
     * @param userReactions  set of emojis the current user has reacted with
     * @param onReact        callback (emoji) -> toggle reaction then refresh
     */
    private FlowPane buildReactionBar(int msgId, boolean isMine,
                                       java.util.Map<String, Integer> reactions,
                                       java.util.Set<String> userReactions,
                                       java.util.function.Consumer<String> onReact) {
        FlowPane bar = new FlowPane(4, 4);
        bar.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bar.setPadding(new Insets(2, 0, 0, 0));

        // Show existing reactions as mini badges
        if (reactions != null && !reactions.isEmpty()) {
            for (var entry : reactions.entrySet()) {
                String emoji = entry.getKey();
                int count = entry.getValue();
                boolean iReacted = userReactions != null && userReactions.contains(emoji);

                Label badge = new Label(emoji + (count > 1 ? " " + count : ""));
                badge.setStyle(
                    "-fx-font-size: 12px; -fx-padding: 2 6; -fx-cursor: hand; "
                    + "-fx-background-radius: 10; "
                    + (iReacted
                        ? "-fx-background-color: derive(-fx-primary, 80%); -fx-border-color: -fx-primary; -fx-border-radius: 10; -fx-border-width: 1;"
                        : "-fx-background-color: -fx-muted; -fx-border-color: -fx-border; -fx-border-radius: 10; -fx-border-width: 1;"));
                badge.setOnMouseClicked(ev -> onReact.accept(emoji));
                bar.getChildren().add(badge);
            }
        }

        // "+" button to add a reaction
        Label addBtn = new Label("＋");
        addBtn.setStyle(
            "-fx-font-size: 11px; -fx-padding: 2 6; -fx-cursor: hand; "
            + "-fx-background-color: -fx-muted; -fx-background-radius: 10; "
            + "-fx-text-fill: -fx-muted-foreground;");
        addBtn.setOnMouseClicked(ev -> {
            // Show reaction picker popup
            javafx.stage.Popup popup = new javafx.stage.Popup();
            popup.setAutoHide(true);
            HBox picker = new HBox(2);
            picker.setPadding(new Insets(6, 10, 6, 10));
            picker.setStyle(
                "-fx-background-color: -fx-card; -fx-background-radius: 20; "
                + "-fx-border-color: -fx-border; -fx-border-radius: 20; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);");
            for (String emoji : REACTION_EMOJIS) {
                Label emojiLabel = new Label(emoji);
                emojiLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 2 4;");
                emojiLabel.setOnMouseEntered(e2 -> emojiLabel.setStyle("-fx-font-size: 22px; -fx-cursor: hand; -fx-padding: 0 2;"));
                emojiLabel.setOnMouseExited(e2 -> emojiLabel.setStyle("-fx-font-size: 18px; -fx-cursor: hand; -fx-padding: 2 4;"));
                emojiLabel.setOnMouseClicked(e2 -> {
                    popup.hide();
                    onReact.accept(emoji);
                });
                picker.getChildren().add(emojiLabel);
            }
            popup.getContent().add(picker);
            javafx.geometry.Bounds bounds = addBtn.localToScreen(addBtn.getBoundsInLocal());
            popup.show(addBtn.getScene().getWindow(), bounds.getMinX(), bounds.getMinY() - 40);
        });
        bar.getChildren().add(addBtn);

        return bar;
    }

    private HBox createGroupMessageView(GroupMessage msg, int readByCount,
                                        java.util.Map<String, Integer> reactions,
                                        java.util.Set<String> myReactions,
                                        Runnable onReactionChanged) {
        boolean isMine = msg.getSenderId() == currentUser.getId();
        HBox row = new HBox(8);
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2));
        // Store message id for polling update
        if (isMine) row.setUserData(msg.getId());

        // Avatar de l'utilisateur (seulement pour ses messages)
        StackPane msgAvatar = null;
        if (!isMine) {
            String senderName = msg.getSenderName() != null ? msg.getSenderName() : "User " + msg.getSenderId();
            msgAvatar = createAvatar(senderName, 30);

            // Point indicateur en ligne / hors ligne
            javafx.scene.shape.Circle onlineDot = new javafx.scene.shape.Circle(4);
            onlineDot.setStroke(javafx.scene.paint.Color.WHITE);
            onlineDot.setStrokeWidth(1);
            onlineDot.setFill(javafx.scene.paint.Color.GRAY);
            StackPane.setAlignment(onlineDot, Pos.BOTTOM_RIGHT);
            msgAvatar.getChildren().add(onlineDot);

            new Thread(() -> {
                boolean online = OnlineStatusService.getInstance().isUserOnline(msg.getSenderId());
                Platform.runLater(() -> {
                    onlineDot.setFill(
                            online ? javafx.scene.paint.Color.web("#22c55e") : javafx.scene.paint.Color.web("#9ca3af"));
                });
            }, "OnlineDotThread").start();
        }

        VBox bubble = new VBox(3);
        bubble.setPadding(new Insets(10, 14, 10, 14));
        bubble.setMaxWidth(350);
        bubble.getStyleClass().add(isMine ? "msg-bubble-mine" : "msg-bubble-theirs");

        // Sender name for groups if not mine
        if (!isMine) {
            Label senderLabel = new Label(
                    msg.getSenderName() != null ? msg.getSenderName() : "User " + msg.getSenderId());
            senderLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
            bubble.getChildren().add(senderLabel);
        }

        // ── CONTENU DU MESSAGE — Texte, Image ou Vidéo ──
        if (msg.isImage() && msg.hasMedia()) {
            try {
                ImageView imgView = new ImageView();
                imgView.setPreserveRatio(true);
                imgView.setFitWidth(280);
                imgView.setSmooth(true);
                imgView.setStyle("-fx-cursor: hand;");
                Image image = new Image(msg.getMediaUrl(), 280, 0, true, true, true);
                imgView.setImage(image);
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(280, 200);
                clip.setArcWidth(12);
                clip.setArcHeight(12);
                image.progressProperty().addListener((obsImg, ov, nv) -> {
                    if (nv.doubleValue() >= 1.0 && image.getHeight() > 0) {
                        double ratio = 280.0 / image.getWidth();
                        clip.setHeight(image.getHeight() * ratio);
                        imgView.setClip(clip);
                    }
                });
                bubble.getChildren().add(imgView);
                if (msg.getContent() != null && !msg.getContent().isBlank()) {
                    Label captionLabel = new Label(msg.getContent());
                    captionLabel.setWrapText(true);
                    captionLabel.setStyle(
                            isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;"
                                    : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                    bubble.getChildren().add(captionLabel);
                }
            } catch (Exception imgEx) {
            }
        } else if (msg.isVideo() && msg.hasMedia()) {
            VBox videoBox = new VBox(4);
            videoBox.setAlignment(Pos.CENTER);
            Label videoIcon = new Label("🎬");
            videoIcon.setStyle("-fx-font-size: 36px;");
            Label videoName = new Label(msg.getFileName() != null ? msg.getFileName() : "Vidéo");
            videoName.setWrapText(true);
            videoName.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px;"
                    : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px;");
            Label playHint = new Label("▶ Cliquer pour ouvrir");
            playHint.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                    + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));
            videoBox.getChildren().addAll(videoIcon, videoName, playHint);
            videoBox.setStyle("-fx-cursor: hand; -fx-padding: 12;");
            videoBox.setOnMouseClicked(vidEv -> {
                try {
                    String mediaUrl = msg.getMediaUrl();
                    if (mediaUrl.startsWith("file:")) {
                        java.awt.Desktop.getDesktop().open(new java.io.File(java.net.URI.create(mediaUrl)));
                    } else {
                        java.awt.Desktop.getDesktop().browse(java.net.URI.create(mediaUrl));
                    }
                } catch (Exception ex) {
                }
            });
            bubble.getChildren().add(videoBox);
            if (msg.getContent() != null && !msg.getContent().isBlank()) {
                Label captionLabel = new Label(msg.getContent());
                captionLabel.setWrapText(true);
                captionLabel.setStyle(
                        isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;"
                                : "-fx-text-fill: -fx-foreground; -fx-font-size: 12px; -fx-padding: 4 0 0 0;");
                bubble.getChildren().add(captionLabel);
            }
        } else if (msg.isVocal() && msg.hasMedia()) {
            HBox vocalBox = new HBox(8);
            vocalBox.setAlignment(Pos.CENTER_LEFT);
            vocalBox.getStyleClass().add("msg-vocal-player");
            Label playPauseIcon = new Label("▶");
            playPauseIcon.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-text-fill: "
                    + (isMine ? "-fx-primary-foreground;" : "-fx-primary;"));
            javafx.scene.control.ProgressBar progressBar = new javafx.scene.control.ProgressBar(0);
            progressBar.setPrefWidth(150);
            progressBar.setPrefHeight(6);
            progressBar.getStyleClass().add("vocal-progress-bar");
            String durationText = com.skilora.community.service.AudioRecorderService.formatDuration(msg.getDuration());
            Label durationLabel = new Label(durationText);
            durationLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: "
                    + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));
            final javafx.scene.media.MediaPlayer[] playerHolder = { null };
            final boolean[] isPlaying = { false };
            playPauseIcon.setOnMouseClicked(playEv -> {
                if (isPlaying[0] && playerHolder[0] != null) {
                    playerHolder[0].pause();
                    playPauseIcon.setText("▶");
                    isPlaying[0] = false;
                } else {
                    if (playerHolder[0] == null) {
                        try {
                            javafx.scene.media.Media media = new javafx.scene.media.Media(msg.getMediaUrl());
                            playerHolder[0] = new javafx.scene.media.MediaPlayer(media);
                            playerHolder[0].currentTimeProperty().addListener((obsT, oldT, newT) -> {
                                if (playerHolder[0].getTotalDuration() != null
                                        && playerHolder[0].getTotalDuration().toMillis() > 0) {
                                    double progress = newT.toMillis() / playerHolder[0].getTotalDuration().toMillis();
                                    Platform.runLater(() -> {
                                        progressBar.setProgress(progress);
                                        durationLabel.setText(com.skilora.community.service.AudioRecorderService
                                                .formatDuration((int) newT.toSeconds()));
                                    });
                                }
                            });
                            playerHolder[0].setOnEndOfMedia(() -> Platform.runLater(() -> {
                                playPauseIcon.setText("▶");
                                progressBar.setProgress(0);
                                durationLabel.setText(durationText);
                                isPlaying[0] = false;
                                playerHolder[0].stop();
                                playerHolder[0].dispose();
                                playerHolder[0] = null;
                            }));
                        } catch (Exception audioEx) {
                            return;
                        }
                    }
                    playerHolder[0].play();
                    playPauseIcon.setText("⏸");
                    isPlaying[0] = true;
                }
            });
            vocalBox.getChildren().addAll(playPauseIcon, progressBar, durationLabel);
            bubble.getChildren().add(vocalBox);
        } else {
            Label msgText = new Label(msg.getContent());
            msgText.setWrapText(true);
            msgText.setStyle(isMine ? "-fx-text-fill: -fx-primary-foreground; -fx-font-size: 13px;"
                    : "-fx-text-fill: -fx-foreground; -fx-font-size: 13px;");
            bubble.getChildren().add(msgText);
        }

        Label timeLabel = new Label(
                formatDate(msg.getCreatedDate() != null ? msg.getCreatedDate() : java.time.LocalDateTime.now()));
        timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: "
                + (isMine ? "-fx-primary-foreground;" : "-fx-muted-foreground;"));

        if (isMine) {
            HBox statusRow = new HBox(4);
            statusRow.setAlignment(Pos.CENTER_RIGHT);
            statusRow.getStyleClass().add("group-msg-status-row");
            statusRow.getChildren().add(timeLabel);
            if (readByCount > 0) {
                Label seenLabel = new Label("✓✓ Vu");
                seenLabel.getStyleClass().add("msg-seen-indicator");
                statusRow.getChildren().add(seenLabel);
            } else {
                Label sentLabel = new Label("✓ Envoyé");
                sentLabel.getStyleClass().add("msg-seen-indicator");
                sentLabel.setStyle("-fx-text-fill: -fx-primary-foreground; -fx-font-size: 10px; -fx-opacity: 0.7;");
                statusRow.getChildren().add(sentLabel);
            }
            bubble.getChildren().add(statusRow);
        } else {
            bubble.getChildren().add(timeLabel);
        }

        // ── REACTION BAR below the bubble ──
        FlowPane reactionBar = buildReactionBar(msg.getId(), isMine, reactions, myReactions, emoji -> {
            new Thread(() -> {
                GroupService.getInstance().toggleReaction(msg.getId(), currentUser.getId(), emoji);
                Platform.runLater(onReactionChanged);
            }, "GroupReactionThread").start();
        });

        VBox bubbleWithReactions = new VBox(0);
        bubbleWithReactions.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        bubbleWithReactions.getChildren().addAll(bubble, reactionBar);

        if (isMine) {
            row.getChildren().add(bubbleWithReactions);
        } else {
            row.getChildren().addAll(msgAvatar, bubbleWithReactions);
        }
        return row;
    }

    /**
     * Affiche le dialogue de création ou modification d'un groupe.
     *
     * CONTRÔLE DE SAISIE :
     * - Nom du groupe obligatoire (sinon DialogUtils.showError)
     *
     * @param existingGroup le groupe à modifier, ou null pour créer
     */
    private void showGroupDialog(CommunityGroup existingGroup) {
        TLDialog<Void> dialog = new TLDialog<>();
        boolean isEdit = existingGroup != null;
        dialog.setDialogTitle(isEdit ? I18n.get("post.edit") + " Group" : I18n.get("group.new"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextField nameField = new TLTextField("Name", "Group name");
        TLTextarea descField = new TLTextarea("Description", "Group description");
        descField.getControl().setPrefRowCount(3);
        TLTextField categoryField = new TLTextField("Category", "e.g., Technology, Finance");

        // Public/Private toggle
        HBox visibilityRow = new HBox(12);
        visibilityRow.setAlignment(Pos.CENTER_LEFT);
        Label visibilityLabel = new Label("🌐  Public Group");
        visibilityLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
        javafx.scene.control.CheckBox publicToggle = new javafx.scene.control.CheckBox();
        publicToggle.setSelected(true);
        Label visibilityHint = new Label("Public groups are visible and joinable by everyone");
        visibilityHint.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
        VBox visibilityInfo = new VBox(2, visibilityLabel, visibilityHint);
        visibilityRow.getChildren().addAll(publicToggle, visibilityInfo);

        // Update hint text when toggled
        publicToggle.selectedProperty().addListener((obs, was, isNow) -> {
            visibilityLabel.setText(isNow ? "🌐  Public Group" : "🔒  Private Group");
            visibilityHint.setText(isNow
                    ? "Public groups are visible and joinable by everyone"
                    : "Private groups are hidden from search and discovery");
        });

        if (isEdit) {
            nameField.setText(existingGroup.getName());
            descField.setText(existingGroup.getDescription());
            categoryField.setText(existingGroup.getCategory());
            publicToggle.setSelected(existingGroup.isPublic());
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton saveBtn = new TLButton(I18n.get("common.save"), TLButton.ButtonVariant.PRIMARY);
        saveBtn.setOnAction(e -> {
            String n = nameField.getText();
            // CONTRÔLE DE SAISIE : nom du groupe obligatoire
            if (n == null || n.trim().isEmpty()) {
                DialogUtils.showError(I18n.get("message.error"), "Name is required");
                return; // Bloquer la soumission
            }

            if (isEdit) {
                existingGroup.setName(n.trim());
                existingGroup.setDescription(descField.getText());
                existingGroup.setCategory(categoryField.getText());
                existingGroup.setPublic(publicToggle.isSelected());
                new Thread(() -> {
                    GroupService.getInstance().update(existingGroup);
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.created"));
                        loadGroupsTab();
                    });
                }, "UpdateGroupThread").start();
            } else {
                CommunityGroup group = new CommunityGroup();
                group.setName(n.trim());
                group.setDescription(descField.getText());
                group.setCategory(categoryField.getText());
                group.setPublic(publicToggle.isSelected());
                group.setCreatorId(currentUser.getId());
                new Thread(() -> {
                    GroupService.getInstance().create(group);
                    Platform.runLater(() -> {
                        showToast(I18n.get("group.success.created"));
                        loadGroupsTab();
                    });
                }, "CreateGroupThread").start();
            }
            dialog.close();
        });

        buttons.getChildren().addAll(cancelBtn, saveBtn);
        content.getChildren().addAll(nameField, descField, categoryField, visibilityRow, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    /**
     * Supprime un groupe après confirmation.
     *
     * @param group le groupe à supprimer
     */
    private void deleteGroup(CommunityGroup group) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            GroupService.getInstance().delete(group.getId());
                            Platform.runLater(this::loadGroupsTab);
                        }, "DeleteGroupThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // BLOG — CRUD complet (Admin, Employer, Trainer peuvent créer)
    // (Créer, Lire, Modifier, Supprimer, Publier/Brouillon)
    // ═══════════════════════════════════════════════════════════

    /**
     * Charge l'onglet Blog.
     * - Admin : voit tous les articles (publiés et brouillons)
     * - Autres : voient seulement les articles publiés
     * - Admin/Employer/Trainer : peuvent créer des articles
     */
    private void loadBlogTab() {
        contentPane.getChildren().clear();
        if (currentUser == null)
            return;

        VBox blogPane = new VBox(12);

        // Bouton Créer un article (pour les rôles autorisés)
        if (canCreateBlog()) {
            TLButton createBtn = new TLButton(I18n.get("blog.new"), TLButton.ButtonVariant.PRIMARY);
            createBtn.setOnAction(e -> showBlogDialog(null));
            HBox actionBar = new HBox(createBtn);
            actionBar.setAlignment(Pos.CENTER_RIGHT);
            blogPane.getChildren().add(actionBar);
        }

        // Charger les articles : admin voit tout, les autres voient les publiés
        Task<List<BlogArticle>> task = new Task<>() {
            @Override
            protected List<BlogArticle> call() {
                if (isAdmin()) {
                    return BlogService.getInstance().findAll();
                }
                return BlogService.getInstance().findPublished();
            }
        };

        task.setOnSucceeded(e -> {
            List<BlogArticle> articles = task.getValue();
            Label header = new Label(I18n.get("blog.title") + " (" + articles.size() + ")");
            header.getStyleClass().add("h4");
            blogPane.getChildren().add(header);

            if (articles.isEmpty()) {
                Label empty = new Label(I18n.get("blog.empty"));
                empty.getStyleClass().add("text-muted");
                blogPane.getChildren().add(empty);
            } else {
                for (BlogArticle article : articles) {
                    blogPane.getChildren().add(createBlogCard(article));
                }
            }
        });
        new Thread(task, "LoadBlogThread").start();

        contentPane.getChildren().add(blogPane);
    }

    /**
     * Crée une carte d'article de blog avec en-tête, résumé, méta-données et
     * actions.
     *
     * @param article l'article à afficher
     * @return TLCard avec toutes les informations
     */
    private TLCard createBlogCard(BlogArticle article) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        VBox body = new VBox(10);
        body.setPadding(new Insets(20));

        // ── En-tête avec avatar de l'auteur ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);

        String authorName = article.getAuthorName() != null ? article.getAuthorName() : "Unknown";
        StackPane avatar = createAvatar(authorName);

        VBox authorInfo = new VBox(2);
        Label authorLabel = new Label(authorName);
        authorLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: -fx-muted-foreground;");
        Label dateLabel = new Label(formatDate(article.getCreatedDate()));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");
        authorInfo.getChildren().addAll(authorLabel, dateLabel);

        Region spacerH = new Region();
        HBox.setHgrow(spacerH, Priority.ALWAYS);

        HBox badgesBox = new HBox(6);
        TLBadge pubBadge = new TLBadge(
                article.isPublished() ? I18n.get("blog.publish") : I18n.get("blog.draft"),
                article.isPublished() ? TLBadge.Variant.SUCCESS : TLBadge.Variant.OUTLINE);
        badgesBox.getChildren().add(pubBadge);
        if (article.getCategory() != null && !article.getCategory().isBlank()) {
            TLBadge catBadge = new TLBadge(article.getCategory(), TLBadge.Variant.SECONDARY);
            badgesBox.getChildren().add(catBadge);
        }

        header.getChildren().addAll(avatar, authorInfo, spacerH, badgesBox);

        // ── Titre de l'article ──
        Label title = new Label(article.getTitle());
        title.setStyle("-fx-font-size: 17px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        title.setWrapText(true);

        // ── Résumé ──
        Label summary = new Label(article.getSummary() != null ? article.getSummary() : "");
        summary.setWrapText(true);
        summary.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");
        summary.setMaxHeight(40);

        // ── Méta-données (nombre de vues) ──
        HBox meta = new HBox(16);
        Label views = new Label("👁  " + I18n.get("blog.views", article.getViewsCount()));
        views.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        meta.getChildren().add(views);

        // ── Actions (Lire, Modifier, Supprimer) ──
        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(4, 0, 0, 0));

        // Bouton "Lire la suite"
        TLButton readBtn = new TLButton("📖  " + I18n.get("blog.read_more"), TLButton.ButtonVariant.OUTLINE);
        readBtn.setSize(TLButton.ButtonSize.SM);
        readBtn.setOnAction(e -> showBlogDetail(article));
        actions.getChildren().add(readBtn);

        // Boutons Modifier/Supprimer pour le propriétaire ou admin
        if (canEditOrDelete(article.getAuthorId())) {
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            TLButton editBtn = new TLButton(I18n.get("post.edit"), TLButton.ButtonVariant.OUTLINE);
            editBtn.setSize(TLButton.ButtonSize.SM);
            editBtn.setOnAction(e -> showBlogDialog(article));

            TLButton deleteBtn = new TLButton(I18n.get("post.delete"), TLButton.ButtonVariant.DANGER);
            deleteBtn.setSize(TLButton.ButtonSize.SM);
            deleteBtn.setOnAction(e -> deleteBlog(article));

            actions.getChildren().addAll(spacer, editBtn, deleteBtn);
        }

        body.getChildren().addAll(header, title, summary, meta, new TLSeparator(), actions);
        card.setContent(body);
        return card;
    }

    /**
     * Affiche le détail complet d'un article de blog.
     * Incrémente le compteur de vues en arrière-plan.
     * Affiche le titre, l'auteur avec avatar, le contenu complet et les tags.
     *
     * @param article l'article à afficher en détail
     */
    private void showBlogDetail(BlogArticle article) {
        // Incrémenter le nombre de vues dans un thread séparé
        new Thread(() -> BlogService.getInstance().incrementViews(article.getId()), "ViewThread").start();

        contentPane.getChildren().clear();
        VBox detailPane = new VBox(16);
        detailPane.setPadding(new Insets(20));

        TLButton backBtn = new TLButton("←  " + I18n.get("blog.title"), TLButton.ButtonVariant.GHOST);
        backBtn.setOnAction(e -> loadBlogTab());

        Label title = new Label(article.getTitle());
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        title.setWrapText(true);

        // Ligne auteur avec avatar
        HBox authorRow = new HBox(12);
        authorRow.setAlignment(Pos.CENTER_LEFT);
        String authorName = article.getAuthorName() != null ? article.getAuthorName() : "Unknown";
        StackPane authorAvatar = createAvatar(authorName, 36);
        VBox authorInfo = new VBox(1);
        Label authorLabel = new Label(authorName);
        authorLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-foreground;");
        Label dateLabel = new Label(
                formatDate(article.getPublishedDate() != null ? article.getPublishedDate() : article.getCreatedDate()));
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        authorInfo.getChildren().addAll(authorLabel, dateLabel);
        authorRow.getChildren().addAll(authorAvatar, authorInfo);

        Label contentLabel = new Label(article.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setMaxWidth(Double.MAX_VALUE);
        contentLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-foreground; -fx-line-spacing: 4;");

        detailPane.getChildren().addAll(backBtn, title, authorRow, new TLSeparator(), contentLabel);

        if (article.getTags() != null && !article.getTags().isBlank()) {
            HBox tagsBox = new HBox(6);
            tagsBox.setPadding(new Insets(8, 0, 0, 0));
            for (String tag : article.getTags().split(",")) {
                TLBadge tagBadge = new TLBadge(tag.trim(), TLBadge.Variant.OUTLINE);
                tagsBox.getChildren().add(tagBadge);
            }
            detailPane.getChildren().addAll(new TLSeparator(), tagsBox);
        }

        contentPane.getChildren().add(detailPane);
    }

    /**
     * Affiche le dialogue de création ou modification d'un article de blog.
     *
     * CONTRÔLES DE SAISIE :
     * - Titre obligatoire (sinon DialogUtils.showError)
     * - Contenu obligatoire (sinon DialogUtils.showError)
     * - Options : Sauvegarder en brouillon ou Publier
     *
     * @param existingArticle l'article à modifier, ou null pour créer
     */
    private void showBlogDialog(BlogArticle existingArticle) {
        TLDialog<Void> dialog = new TLDialog<>();
        boolean isEdit = existingArticle != null;
        dialog.setDialogTitle(isEdit ? I18n.get("post.edit") + " Article" : I18n.get("blog.new"));

        VBox content = new VBox(12);
        content.setPadding(new Insets(16));

        TLTextField titleField = new TLTextField("Title", "Article title");
        TLTextarea contentField = new TLTextarea("Content", "Write your article...");
        contentField.getControl().setPrefRowCount(8);
        TLTextField summaryField = new TLTextField("Summary", "Short description");
        TLTextField categoryField = new TLTextField("Category", "e.g., Technology, Career");
        TLTextField tagsField = new TLTextField("Tags", "Comma-separated tags");

        if (isEdit) {
            titleField.setText(existingArticle.getTitle());
            contentField.setText(existingArticle.getContent());
            summaryField.setText(existingArticle.getSummary());
            categoryField.setText(existingArticle.getCategory());
            tagsField.setText(existingArticle.getTags());
        }

        HBox buttons = new HBox(8);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        TLButton cancelBtn = new TLButton(I18n.get("common.cancel"), TLButton.ButtonVariant.SECONDARY);
        cancelBtn.setOnAction(e -> dialog.close());

        TLButton draftBtn = new TLButton(I18n.get("blog.draft"), TLButton.ButtonVariant.OUTLINE);
        draftBtn.setOnAction(e -> saveBlogArticle(isEdit, existingArticle, titleField, contentField, summaryField,
                categoryField, tagsField, false, dialog));

        TLButton publishBtn = new TLButton(I18n.get("blog.publish"), TLButton.ButtonVariant.PRIMARY);
        publishBtn.setOnAction(e -> saveBlogArticle(isEdit, existingArticle, titleField, contentField, summaryField,
                categoryField, tagsField, true, dialog));

        buttons.getChildren().addAll(cancelBtn, draftBtn, publishBtn);
        content.getChildren().addAll(titleField, summaryField, contentField, categoryField, tagsField, buttons);
        dialog.setContent(content);
        dialog.show();
    }

    /**
     * Sauvegarde un article de blog (création ou mise à jour).
     *
     * CONTRÔLES DE SAISIE :
     * - Titre obligatoire
     * - Contenu obligatoire
     *
     * @param isEdit        true si modification, false si création
     * @param existing      l'article existant (en mode édition)
     * @param titleField    champ titre
     * @param contentField  champ contenu
     * @param summaryField  champ résumé
     * @param categoryField champ catégorie
     * @param tagsField     champ tags
     * @param publish       true pour publier, false pour brouillon
     * @param dialog        le dialogue à fermer après sauvegarde
     */
    private void saveBlogArticle(boolean isEdit, BlogArticle existing, TLTextField titleField,
            TLTextarea contentField, TLTextField summaryField, TLTextField categoryField,
            TLTextField tagsField, boolean publish, TLDialog<?> dialog) {
        String t = titleField.getText();
        // CONTRÔLE DE SAISIE : titre obligatoire
        if (t == null || t.trim().isEmpty()) {
            DialogUtils.showError(I18n.get("message.error"), "Title is required");
            return; // Bloquer
        }
        String c = contentField.getText();
        // CONTRÔLE DE SAISIE : contenu obligatoire
        if (c == null || c.trim().isEmpty()) {
            DialogUtils.showError(I18n.get("message.error"), "Content is required");
            return; // Bloquer
        }

        BlogArticle article = isEdit ? existing : new BlogArticle();
        article.setTitle(t.trim());
        article.setContent(c.trim());
        article.setSummary(summaryField.getText());
        article.setCategory(categoryField.getText());
        article.setTags(tagsField.getText());
        article.setPublished(publish);

        if (isEdit) {
            new Thread(() -> {
                BlogService.getInstance().update(article);
                Platform.runLater(() -> {
                    showToast(publish ? I18n.get("blog.success.published") : I18n.get("blog.success.created"));
                    loadBlogTab();
                });
            }, "UpdateBlogThread").start();
        } else {
            article.setAuthorId(currentUser.getId());
            new Thread(() -> {
                BlogService.getInstance().create(article);
                Platform.runLater(() -> {
                    showToast(publish ? I18n.get("blog.success.published") : I18n.get("blog.success.created"));
                    loadBlogTab();
                });
            }, "CreateBlogThread").start();
        }
        dialog.close();
    }

    /**
     * Supprime un article de blog après confirmation.
     *
     * @param article l'article à supprimer
     */
    private void deleteBlog(BlogArticle article) {
        DialogUtils.showConfirmation(I18n.get("post.delete"), I18n.get("community.remove.confirm.message"))
                .ifPresent(result -> {
                    if (result == ButtonType.OK) {
                        new Thread(() -> {
                            BlogService.getInstance().delete(article.getId());
                            Platform.runLater(this::loadBlogTab);
                        }, "DeleteBlogThread").start();
                    }
                });
    }

    // ═══════════════════════════════════════════════════════════
    // HELPERS AVATAR — Style réseau social moderne
    // (Cercle coloré avec initiales du nom)
    // ═══════════════════════════════════════════════════════════

    /** Palette de couleurs pour les avatars (8 couleurs différentes) */
    private static final String[] AVATAR_COLORS = {
            "#3b82f6", "#22c55e", "#a855f7", "#f97316",
            "#ec4899", "#06b6d4", "#ef4444", "#6366f1"
    };

    /**
     * Crée un avatar circulaire avec les initiales du nom.
     * La couleur est déterminée par le hashCode du nom pour consistance.
     *
     * @param name le nom de l'utilisateur
     * @param size la taille en pixels du cercle
     * @return StackPane contenant le cercle et les initiales
     */
    private StackPane createAvatar(String name, double size) {
        String initials = getInitials(name); // Extraire les initiales
        int colorIndex = Math.abs((name != null ? name : "").hashCode()) % AVATAR_COLORS.length; // Couleur consistante
        String color = AVATAR_COLORS[colorIndex];

        Circle circle = new Circle(size / 2);
        circle.setFill(Color.web(color));
        circle.setStroke(Color.web(color, 0.3));
        circle.setStrokeWidth(2);

        Text text = new Text(initials);
        text.setFill(Color.WHITE);
        text.setFont(Font.font("Inter", FontWeight.BOLD, size * 0.38));

        StackPane avatar = new StackPane(circle, text);
        avatar.setMinSize(size, size);
        avatar.setMaxSize(size, size);
        avatar.setPrefSize(size, size);
        return avatar;
    }

    /** Surcharge : avatar par défaut de 42px pour les cartes. */
    private StackPane createAvatar(String name) {
        return createAvatar(name, 42);
    }

    /**
     * Extrait les initiales (max 2) d'un nom.
     * Ex: "Jean Dupont" → "JD", "Alice" → "A"
     *
     * @param name le nom complet
     * @return les initiales en majuscules
     */
    private String getInitials(String name) {
        if (name == null || name.isBlank())
            return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
        }
        return ("" + parts[0].charAt(0)).toUpperCase();
    }

    // ═══════════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES
    // ═══════════════════════════════════════════════════════════

    /** Formate une date en chaîne lisible (dd MMM yyyy, HH:mm). */
    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";
        return dateTime.format(DATE_FMT); // Format défini dans les constantes
    }

    /**
     * Affiche un toast de succès dans l'interface.
     * Protégé par try-catch pour éviter les erreurs si la scène n'est pas
     * disponible.
     *
     * @param message le message à afficher
     */
    private void showToast(String message) {
        try {
            if (contentPane.getScene() != null) {
                TLToast.success(contentPane.getScene(), "Community", message);
            }
        } catch (Exception e) {
            logger.debug("Toast not shown: {}", e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // RECHERCHE AVANCÉE — Barre de recherche globale multi-entités
    // (Feature F1 — Sprint 2)
    // ═══════════════════════════════════════════════════════════

    /**
     * Construit la barre de recherche avancée pour le fil d'actualité.
     *
     * Composants :
     * - TLTextField : champ de saisie du mot-clé
     * - TLSelect : filtre par type de contenu (Tous, Posts, Messages, etc.)
     * - TLButton : bouton de recherche
     *
     * Fonctionnement :
     * 1. L'utilisateur saisit un mot-clé et choisit un filtre
     * 2. La recherche est effectuée dans un thread séparé via SearchService
     * 3. Les résultats sont affichés sous forme de cartes dans le contentPane
     * 4. Chaque résultat a un badge de type (POST, MESSAGE, EVENT, GROUP, BLOG)
     *
     * @return HBox contenant la barre de recherche complète
     */
    private HBox buildSearchBar() {
        HBox searchBar = new HBox(10);
        searchBar.setAlignment(Pos.CENTER_LEFT);
        searchBar.setPadding(new Insets(8, 0, 12, 0));

        // Champ de recherche
        TLTextField searchField = new TLTextField("", "🔍  Rechercher dans la communauté...");
        HBox.setHgrow(searchField, Priority.ALWAYS);

        // Sélecteur de filtre par DATE (Tout, Aujourd'hui, Cette semaine, Ce mois,
        // Cette année)
        TLSelect<String> dateFilterSelect = new TLSelect<>("Période",
                "Tout", "Aujourd'hui", "Cette semaine", "Ce mois", "Cette année");
        dateFilterSelect.setValue("Tout");

        // Sélecteur de tri (Plus récent / Plus ancien)
        TLSelect<String> sortSelect = new TLSelect<>("Tri",
                "Plus récent", "Plus ancien");
        sortSelect.setValue("Plus récent");

        // ── Action de recherche extraite en Runnable pour pouvoir la déclencher
        // depuis le bouton ET depuis les changements de filtres ──
        Runnable executeSearch = () -> {
            String keyword = searchField.getText();
            if (keyword == null || keyword.isBlank())
                return;

            // Convertir le filtre de date sélectionné en enum DateFilter
            String dateVal = dateFilterSelect.getValue();
            SearchService.DateFilter dateFilter = switch (dateVal != null ? dateVal : "Tout") {
                case "Aujourd'hui" -> SearchService.DateFilter.TODAY;
                case "Cette semaine" -> SearchService.DateFilter.THIS_WEEK;
                case "Ce mois" -> SearchService.DateFilter.THIS_MONTH;
                case "Cette année" -> SearchService.DateFilter.THIS_YEAR;
                default -> SearchService.DateFilter.ALL;
            };

            // Récupérer le tri sélectionné
            boolean sortAscending = "Plus ancien".equals(sortSelect.getValue());

            // Exécuter la recherche dans un thread séparé (requête SQL)
            Task<List<SearchService.SearchResult>> searchTask = new Task<>() {
                @Override
                protected List<SearchService.SearchResult> call() {
                    List<SearchService.SearchResult> all = searchService.search(
                            keyword, SearchService.SearchFilter.ALL, currentUser.getId());
                    List<SearchService.SearchResult> filtered = searchService.filterByDate(all, dateFilter);
                    // Trier par date : plus récent ou plus ancien
                    filtered.sort((a, b) -> {
                        if (a.getDate() == null && b.getDate() == null)
                            return 0;
                        if (a.getDate() == null)
                            return 1;
                        if (b.getDate() == null)
                            return -1;
                        return sortAscending ? a.getDate().compareTo(b.getDate())
                                : b.getDate().compareTo(a.getDate());
                    });
                    return filtered;
                }
            };

            searchTask.setOnSucceeded(ev -> {
                List<SearchService.SearchResult> results = searchTask.getValue();
                contentPane.getChildren().clear();
                contentPane.getChildren().add(searchBar);

                if (results.isEmpty()) {
                    Label noResults = new Label("Aucun résultat pour \"" + keyword + "\"");
                    noResults.getStyleClass().add("text-muted");
                    noResults.setStyle("-fx-font-size: 14px; -fx-padding: 20 0;");
                    contentPane.getChildren().add(noResults);
                } else {
                    Label countLabel = new Label("🔍  " + results.size() + " résultat(s) pour \"" + keyword + "\"");
                    countLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 600; -fx-text-fill: -fx-foreground;");
                    contentPane.getChildren().add(countLabel);

                    int delay = 0;
                    for (SearchService.SearchResult r : results) {
                        TLCard card = createSearchResultCard(r);
                        contentPane.getChildren().add(card);
                        animateCardEntry(card, delay);
                        delay += 60;
                    }
                }

                TLButton backBtn = new TLButton("←  Retour au feed", TLButton.ButtonVariant.GHOST);
                backBtn.setOnAction(ev2 -> loadFeedTab());
                contentPane.getChildren().add(backBtn);
            });

            new Thread(searchTask, "SearchThread").start();
        };

        // Bouton de recherche
        TLButton searchBtn = new TLButton("🔍  Chercher", TLButton.ButtonVariant.PRIMARY);
        searchBtn.setSize(TLButton.ButtonSize.SM);
        searchBtn.setOnAction(e -> executeSearch.run());

        // Re-exécuter la recherche automatiquement quand le filtre date ou le tri
        // change
        dateFilterSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (searchField.getText() != null && !searchField.getText().isBlank()) {
                executeSearch.run();
            }
        });
        sortSelect.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (searchField.getText() != null && !searchField.getText().isBlank()) {
                executeSearch.run();
            }
        });

        searchBar.getChildren().addAll(searchField, dateFilterSelect, sortSelect, searchBtn);
        return searchBar;
    }

    /**
     * Crée une carte visuelle pour un résultat de recherche.
     * Affiche le type sous forme de badge coloré, le titre, l'extrait et la date.
     *
     * @param result le résultat de recherche à afficher
     * @return TLCard contenant les détails du résultat
     */
    private TLCard createSearchResultCard(SearchService.SearchResult result) {
        TLCard card = new TLCard();
        card.getStyleClass().add("post-card");
        VBox body = new VBox(6);
        body.setPadding(new Insets(14));

        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);

        // Badge de type coloré selon le type de résultat
        TLBadge.Variant badgeVariant = switch (result.getType()) {
            case "POST" -> TLBadge.Variant.DEFAULT;
            case "MESSAGE" -> TLBadge.Variant.SECONDARY;
            case "EVENT" -> TLBadge.Variant.SUCCESS;
            case "GROUP" -> TLBadge.Variant.OUTLINE;
            case "BLOG" -> TLBadge.Variant.DESTRUCTIVE;
            default -> TLBadge.Variant.DEFAULT;
        };
        TLBadge typeBadge = new TLBadge(result.getType(), badgeVariant);

        Label titleLabel = new Label(result.getTitle());
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: -fx-foreground;");
        HBox.setHgrow(titleLabel, Priority.ALWAYS);

        Label dateLabel = new Label(formatDate(result.getDate()));
        dateLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");

        header.getChildren().addAll(typeBadge, titleLabel, dateLabel);

        Label excerpt = new Label(result.getExcerpt());
        excerpt.setWrapText(true);
        excerpt.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");

        Label authorLabel = new Label("Par " + (result.getAuthor() != null ? result.getAuthor() : "Unknown"));
        authorLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: -fx-muted-foreground;");

        body.getChildren().addAll(header, excerpt, authorLabel);
        card.setContent(body);
        return card;
    }

    // ═══════════════════════════════════════════════════════════
    // EMOJI PICKER — Grille d'emojis avec catégories
    // (Feature U4 — Sprint 2)
    // ═══════════════════════════════════════════════════════════

    /** Tableau des emojis fréquemment utilisés, organisés en grille */
    private static final String[] EMOJI_LIST = {
            "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂",
            "🙂", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗",
            "😜", "🤪", "😝", "🤑", "🤗", "🤔", "🤐", "😐",
            "😏", "😒", "😔", "😢", "😭", "😤", "🤬", "😈",
            "👍", "👎", "👏", "🙌", "🤝", "💪", "✌️", "🤞",
            "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "💔",
            "🔥", "⭐", "🌟", "💯", "✅", "❌", "⚡", "🎯",
            "📌", "💡", "📢", "🎉", "🎊", "🏆", "🥇", "🚀"
    };

    /**
     * Affiche un petit panneau popup d'emojis sous le bouton cliqué.
     * L'emoji sélectionné est inséré à la fin du texte dans le TLTextarea.
     *
     * ARCHITECTURE :
     * - Popup JavaFX positionné sous le bouton déclencheur
     * - GridPane 8×8 pour afficher 64 emojis
     * - Chaque emoji est un Label cliquable
     * - Au clic : insérer l'emoji dans le textarea et fermer le popup
     *
     * @param anchor   le bouton qui a déclenché l'ouverture (pour positionnement)
     * @param textArea le champ de texte dans lequel insérer l'emoji
     */
    private void showEmojiPicker(Node anchor, TLTextarea textArea) {
        Popup popup = new Popup();
        popup.setAutoHide(true); // Se ferme automatiquement quand on clique ailleurs

        // Grille d'emojis 8 colonnes
        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        // Remplir la grille avec les emojis
        for (int i = 0; i < EMOJI_LIST.length; i++) {
            String emoji = EMOJI_LIST[i];
            Label emojiLabel = new Label(emoji);
            emojiLabel.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;");
            // Effet de survol : agrandir l'emoji
            emojiLabel.setOnMouseEntered(e -> emojiLabel.setStyle(
                    "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 2; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            emojiLabel.setOnMouseExited(e -> emojiLabel.setStyle(
                    "-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;"));
            // Au clic : insérer l'emoji dans le textarea
            emojiLabel.setOnMouseClicked(e -> {
                String current = textArea.getText() != null ? textArea.getText() : "";
                textArea.setText(current + emoji); // Ajouter l'emoji à la fin
                popup.hide();
            });
            grid.add(emojiLabel, i % 8, i / 8); // Positionnement grille (col, row)
        }

        popup.getContent().add(grid);
        // Positionner le popup sous le bouton déclencheur
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    /**
     * Variante du emoji picker pour les TLTextField (messages).
     * Même logique mais insère dans un TLTextField au lieu d'un TLTextarea.
     *
     * @param anchor    le bouton emoji dans la barre de chat
     * @param textField le champ de texte du message
     */
    private void showEmojiPickerForTextField(Node anchor, TLTextField textField) {
        Popup popup = new Popup();
        popup.setAutoHide(true);

        GridPane grid = new GridPane();
        grid.setHgap(4);
        grid.setVgap(4);
        grid.setPadding(new Insets(10));
        grid.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");

        for (int i = 0; i < EMOJI_LIST.length; i++) {
            String emoji = EMOJI_LIST[i];
            Label emojiLabel = new Label(emoji);
            emojiLabel.setStyle("-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;");
            emojiLabel.setOnMouseEntered(e -> emojiLabel.setStyle(
                    "-fx-font-size: 24px; -fx-cursor: hand; -fx-padding: 2; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            emojiLabel.setOnMouseExited(e -> emojiLabel.setStyle(
                    "-fx-font-size: 20px; -fx-cursor: hand; -fx-padding: 4;"));
            emojiLabel.setOnMouseClicked(e -> {
                String current = textField.getText() != null ? textField.getText() : "";
                textField.setText(current + emoji);
                popup.hide();
            });
            grid.add(emojiLabel, i % 8, i / 8);
        }

        popup.getContent().add(grid);
        var bounds = anchor.localToScreen(anchor.getBoundsInLocal());
        if (bounds != null) {
            popup.show(anchor, bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // TRADUCTION AVEC CHOIX DE LANGUE — API MyMemory
    // (API A1 — Sprint 2)
    // ═══════════════════════════════════════════════════════════

    /**
     * Affiche un menu popup permettant de choisir la langue cible
     * avant de lancer la traduction via l'API MyMemory.
     *
     * Options :
     * 🇫🇷 Français (fr)
     * 🇬🇧 English (en)
     * 🇸🇦 العربية (ar)
     * ↩ Original — restaure le texte d'origine
     *
     * @param translateBtn le bouton "Traduire" (pour positionner le popup)
     * @param contentLabel le Label contenant le texte du post
     * @param originalText le texte original du post (pour restauration)
     */
    private void showTranslationMenu(TLButton translateBtn, Label contentLabel, String originalText) {
        Popup popup = new Popup();
        popup.setAutoHide(true); // Se ferme quand on clique ailleurs

        VBox menu = new VBox(2);
        menu.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 8; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 12, 0, 0, 4);");
        menu.setMinWidth(180);

        // Titre du menu
        Label title = new Label("🌐  Traduire en :");
        title.setStyle(
                "-fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: -fx-muted-foreground; -fx-padding: 4 8;");
        menu.getChildren().add(title);

        // Détecter la langue source actuelle du texte
        String detectedLang = translationService.detectLanguage(originalText);

        // ── Options de langue (sans emojis drapeaux — incompatibles Windows) ──
        String[][] languages = {
                { "Français", "fr" },
                { "English", "en" },
                { "العربية", "ar" }
        };

        for (String[] lang : languages) {
            String label = lang[0];
            String langCode = lang[1];

            HBox item = new HBox(8);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setPadding(new Insets(8, 12, 8, 12));
            item.setStyle("-fx-cursor: hand; -fx-background-radius: 6;");

            Label itemLabel = new Label(label);
            itemLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-foreground;");

            // Indiquer la langue détectée avec un badge
            if (langCode.equals(detectedLang)) {
                TLBadge currentBadge = new TLBadge("détectée", TLBadge.Variant.OUTLINE);
                item.getChildren().addAll(itemLabel, currentBadge);
            } else {
                item.getChildren().add(itemLabel);
            }

            // Effets de survol
            item.setOnMouseEntered(ev -> item.setStyle(
                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            item.setOnMouseExited(ev -> item.setStyle(
                    "-fx-cursor: hand; -fx-background-radius: 6;"));

            // Au clic : lancer la traduction vers la langue choisie
            item.setOnMouseClicked(ev -> {
                popup.hide();
                translateBtn.setText("⏳  Traduction...");
                translateBtn.setDisable(true);

                new Thread(() -> {
                    try {
                        String sourceLang = detectedLang;
                        String translated = translationService.translate(originalText, sourceLang, langCode);
                        Platform.runLater(() -> {
                            contentLabel.setText("🌐 [" + langCode.toUpperCase() + "] " + translated);
                            translateBtn.setText("↩  Original");
                            translateBtn.setDisable(false);
                            // Rebrancher : clic suivant = restaurer l'original
                            translateBtn.setOnAction(ev2 -> {
                                contentLabel.setText(originalText);
                                translateBtn.setText("🌐  Traduire");
                                // Rebrancher le menu de choix pour les futures traductions
                                translateBtn.setOnAction(
                                        ev3 -> showTranslationMenu(translateBtn, contentLabel, originalText));
                            });
                            showToast("Traduit : " + sourceLang.toUpperCase() + " → " + langCode.toUpperCase());
                        });
                    } catch (Exception ex) {
                        Platform.runLater(() -> {
                            translateBtn.setText("🌐  Traduire");
                            translateBtn.setDisable(false);
                            showToast("Erreur de traduction : " + ex.getMessage());
                        });
                    }
                }, "TranslateThread").start();
            });

            menu.getChildren().add(item);
        }

        // ── Séparateur + Bouton "Original" (si déjà traduit) ──
        if (!contentLabel.getText().equals(originalText)) {
            menu.getChildren().add(new TLSeparator());
            HBox restoreItem = new HBox(8);
            restoreItem.setAlignment(Pos.CENTER_LEFT);
            restoreItem.setPadding(new Insets(8, 12, 8, 12));
            restoreItem.setStyle("-fx-cursor: hand; -fx-background-radius: 6;");
            Label restoreLabel = new Label("↩  Texte original");
            restoreLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-muted-foreground;");
            restoreItem.getChildren().add(restoreLabel);
            restoreItem.setOnMouseEntered(ev -> restoreItem.setStyle(
                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 6;"));
            restoreItem.setOnMouseExited(ev -> restoreItem.setStyle(
                    "-fx-cursor: hand; -fx-background-radius: 6;"));
            restoreItem.setOnMouseClicked(ev -> {
                popup.hide();
                contentLabel.setText(originalText);
                translateBtn.setText("🌐  Traduire");
                translateBtn.setOnAction(ev2 -> showTranslationMenu(translateBtn, contentLabel, originalText));
            });
            menu.getChildren().add(restoreItem);
        }

        popup.getContent().add(menu);
        var bounds = translateBtn.localToScreen(translateBtn.getBoundsInLocal());
        if (bounds != null) {
            popup.show(translateBtn, bounds.getMinX(), bounds.getMaxY() + 4);
        }
    }

    // ═══════════════════════════════════════════════════════════
    // MENTIONS @USER — Autocomplétion et traitement des mentions
    // (Feature F7 — Sprint 2)
    // ═══════════════════════════════════════════════════════════

    /**
     * Configure la détection des mentions @user dans un TLTextarea.
     * Quand l'utilisateur tape '@' suivi de lettres, un popup d'autocomplétion
     * apparaît avec les utilisateurs correspondants.
     *
     * FONCTIONNEMENT :
     * 1. Écouter les changements de texte dans le textarea
     * 2. Détecter le pattern @xxx (au moins 2 caractères après @)
     * 3. Chercher les utilisateurs correspondants via MentionService.searchUsers()
     * 4. Afficher un popup avec la liste des utilisateurs trouvés
     * 5. Au clic sur un utilisateur, remplacer @xxx par @prenom_nom
     *
     * @param textArea le champ de texte à surveiller
     * @param dialog   le dialogue parent (pour le positionnement du popup)
     */
    private void setupMentionDetection(TLTextarea textArea, TLDialog<?> dialog) {
        // Accéder au contrôle interne du TLTextarea (TextArea JavaFX)
        TextArea innerControl = textArea.getControl();
        Popup mentionPopup = new Popup();
        mentionPopup.setAutoHide(true);
        VBox mentionList = new VBox(2);
        mentionList.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        mentionList.setMinWidth(220);
        mentionPopup.getContent().add(mentionList);

        // Écouter chaque modification du texte
        // On utilise Platform.runLater car getCaretPosition() n'est pas encore mis à
        // jour
        // dans le listener textProperty — le curseur est actualisé après le listener
        innerControl.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (newText == null || newText.isEmpty()) {
                    mentionPopup.hide();
                    return;
                }
                // Trouver la position du curseur (fiable dans runLater)
                int caretPos = innerControl.getCaretPosition();
                if (caretPos <= 0 || caretPos > newText.length()) {
                    mentionPopup.hide();
                    return;
                }

                // Chercher le dernier @ avant le curseur
                String textBeforeCaret = newText.substring(0, caretPos);
                int atIndex = textBeforeCaret.lastIndexOf('@');
                if (atIndex < 0) {
                    mentionPopup.hide();
                    return;
                }

                // Extraire le texte après @ (la requête de recherche)
                String query = textBeforeCaret.substring(atIndex + 1);
                // Vérifier que c'est un seul mot (pas d'espace entre @ et le curseur)
                if (query.contains(" ") || query.contains("\n") || query.length() < 1) {
                    mentionPopup.hide();
                    return;
                }

                // Chercher les utilisateurs en arrière-plan
                final String searchQuery = query;
                final int mentionStart = atIndex;
                final int currentCaretPos = caretPos;
                new Thread(() -> {
                    List<MentionService.UserMention> users = mentionService.searchUsers(searchQuery, 5);
                    Platform.runLater(() -> {
                        mentionList.getChildren().clear();
                        if (users.isEmpty()) {
                            mentionPopup.hide();
                            return;
                        }
                        for (MentionService.UserMention user : users) {
                            // Créer une entrée dans le popup pour chaque utilisateur trouvé
                            HBox item = new HBox(8);
                            item.setAlignment(Pos.CENTER_LEFT);
                            item.setPadding(new Insets(6, 10, 6, 10));
                            item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;");
                            item.setOnMouseEntered(ev -> item.setStyle(
                                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 4;"));
                            item.setOnMouseExited(ev -> item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;"));

                            StackPane userAvatar = createAvatar(user.getFullName(), 24);
                            Label userName = new Label(user.getFullName() + " (@" + user.getHandle() + ")");
                            userName.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
                            item.getChildren().addAll(userAvatar, userName);

                            // Au clic : remplacer @query par @handle dans le texte
                            item.setOnMouseClicked(ev -> {
                                String currentText = innerControl.getText();
                                String before = currentText.substring(0, mentionStart);
                                String after = currentCaretPos < currentText.length()
                                        ? currentText.substring(currentCaretPos)
                                        : "";
                                String replacement = "@" + user.getHandle() + " ";
                                innerControl.setText(before + replacement + after);
                                innerControl.positionCaret(before.length() + replacement.length());
                                mentionPopup.hide();
                            });

                            mentionList.getChildren().add(item);
                        }
                        // Positionner et afficher le popup au-dessus du dialogue
                        if (!mentionPopup.isShowing()) {
                            var bounds = innerControl.localToScreen(innerControl.getBoundsInLocal());
                            if (bounds != null) {
                                // Afficher le popup en passant la Window du dialogue comme owner
                                // Cela garantit que le popup s'affiche AU-DESSUS du dialogue modal
                                javafx.stage.Window owner = innerControl.getScene() != null
                                        ? innerControl.getScene().getWindow()
                                        : null;
                                if (owner != null) {
                                    mentionPopup.show(owner, bounds.getMinX() + 20, bounds.getMinY() + 40);
                                } else {
                                    mentionPopup.show(innerControl, bounds.getMinX() + 20, bounds.getMinY() + 40);
                                }
                            }
                        }
                    });
                }, "MentionSearchThread").start();
            });
        });
    }

    /**
     * Configure la détection des mentions @user dans un TLTextField (commentaires,
     * messages).
     * Même logique que setupMentionDetection() mais adapté pour TextField (une
     * seule ligne).
     *
     * @param textField le champ de texte à surveiller
     */
    private void setupMentionDetectionForTextField(TLTextField textField) {
        TextField innerControl = textField.getControl();
        Popup mentionPopup = new Popup();
        mentionPopup.setAutoHide(true);
        VBox mentionList = new VBox(2);
        mentionList.setStyle("-fx-background-color: -fx-card; -fx-border-color: -fx-border; "
                + "-fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 6; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 10, 0, 0, 4);");
        mentionList.setMinWidth(220);
        mentionPopup.getContent().add(mentionList);

        innerControl.textProperty().addListener((obs, oldText, newText) -> {
            Platform.runLater(() -> {
                if (newText == null || newText.isEmpty()) {
                    mentionPopup.hide();
                    return;
                }
                int caretPos = innerControl.getCaretPosition();
                if (caretPos <= 0 || caretPos > newText.length()) {
                    mentionPopup.hide();
                    return;
                }

                String textBeforeCaret = newText.substring(0, caretPos);
                int atIndex = textBeforeCaret.lastIndexOf('@');
                if (atIndex < 0) {
                    mentionPopup.hide();
                    return;
                }

                String query = textBeforeCaret.substring(atIndex + 1);
                if (query.contains(" ") || query.length() < 1) {
                    mentionPopup.hide();
                    return;
                }

                final String searchQuery = query;
                final int mentionStart = atIndex;
                final int currentCaretPos = caretPos;
                new Thread(() -> {
                    List<MentionService.UserMention> users = mentionService.searchUsers(searchQuery, 5);
                    Platform.runLater(() -> {
                        mentionList.getChildren().clear();
                        if (users.isEmpty()) {
                            mentionPopup.hide();
                            return;
                        }
                        for (MentionService.UserMention user : users) {
                            HBox item = new HBox(8);
                            item.setAlignment(Pos.CENTER_LEFT);
                            item.setPadding(new Insets(6, 10, 6, 10));
                            item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;");
                            item.setOnMouseEntered(ev -> item.setStyle(
                                    "-fx-cursor: hand; -fx-background-color: -fx-accent; -fx-background-radius: 4;"));
                            item.setOnMouseExited(ev -> item.setStyle("-fx-cursor: hand; -fx-background-radius: 4;"));

                            StackPane userAvatar = createAvatar(user.getFullName(), 24);
                            Label userName = new Label(user.getFullName() + " (@" + user.getHandle() + ")");
                            userName.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-foreground;");
                            item.getChildren().addAll(userAvatar, userName);

                            item.setOnMouseClicked(ev -> {
                                String currentText = innerControl.getText();
                                String before = currentText.substring(0, mentionStart);
                                String after = currentCaretPos < currentText.length()
                                        ? currentText.substring(currentCaretPos)
                                        : "";
                                String replacement = "@" + user.getHandle() + " ";
                                innerControl.setText(before + replacement + after);
                                innerControl.positionCaret(before.length() + replacement.length());
                                mentionPopup.hide();
                            });

                            mentionList.getChildren().add(item);
                        }
                        if (!mentionPopup.isShowing()) {
                            var bounds = innerControl.localToScreen(innerControl.getBoundsInLocal());
                            if (bounds != null) {
                                javafx.stage.Window owner = innerControl.getScene() != null
                                        ? innerControl.getScene().getWindow()
                                        : null;
                                double popupX = bounds.getMinX();
                                double popupY = bounds.getMinY() - (users.size() * 36 + 16); // Au-dessus du champ
                                if (owner != null) {
                                    mentionPopup.show(owner, popupX, popupY);
                                } else {
                                    mentionPopup.show(innerControl, popupX, popupY);
                                }
                            }
                        }
                    });
                }, "MentionSearchThread").start();
            });
        });
    }

    // ═══════════════════════════════════════════════════════════
    // ANIMATIONS — Transitions fluides pour l'UX
    // (Feature U1 — Sprint 2)
    // ═══════════════════════════════════════════════════════════

    /**
     * Anime l'entrée d'une carte avec un effet combiné :
     * 1. FadeTransition : opacité de 0 → 1 (apparition progressive)
     * 2. TranslateTransition : glissement de 30px vers le haut (montée douce)
     *
     * L'animation est décalée par un délai (delay) pour créer un effet cascade
     * quand plusieurs cartes sont chargées ensemble (staggered reveal).
     *
     * POURQUOI CES ANIMATIONS ?
     * - Améliore la fluidité perçue de l'interface
     * - Donne un feedback visuel que le contenu est en train de charger
     * - Crée un effet professionnel similaire aux réseaux sociaux modernes
     *
     * @param node  le composant à animer (généralement un TLCard)
     * @param delay délai avant le début de l'animation en millisecondes
     */
    private void animateCardEntry(Node node, int delay) {
        // Étape 1 : rendre la carte invisible au départ
        node.setOpacity(0);
        node.setTranslateY(30); // Décalée de 30px vers le bas

        // Étape 2 : animation de fondu (fade-in)
        FadeTransition fade = new FadeTransition(Duration.millis(400), node);
        fade.setFromValue(0.0); // Départ : invisible
        fade.setToValue(1.0); // Arrivée : complètement visible
        fade.setDelay(Duration.millis(delay)); // Décalage pour effet cascade

        // Étape 3 : animation de glissement vers le haut (slide-up)
        TranslateTransition slide = new TranslateTransition(Duration.millis(400), node);
        slide.setFromY(30); // Départ : 30px plus bas
        slide.setToY(0); // Arrivée : position normale
        slide.setDelay(Duration.millis(delay));

        // Lancer les deux animations en parallèle
        fade.play();
        slide.play();
    }
}
