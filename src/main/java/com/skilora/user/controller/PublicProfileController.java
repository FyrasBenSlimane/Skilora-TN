package com.skilora.user.controller;

import com.skilora.framework.components.TLAvatar;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLToast;
import com.skilora.framework.components.TLCard;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.PortfolioItem;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Review;
import com.skilora.user.entity.Skill;
import com.skilora.user.entity.User;
import com.skilora.user.service.PortfolioService;
import com.skilora.user.service.ProfileService;
import com.skilora.user.service.ReviewService;
import com.skilora.user.service.AuthService;
import com.skilora.formation.entity.Certificate;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.service.CertificateService;
import com.skilora.formation.service.FormationService;
import com.skilora.formation.controller.CertificateViewController;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLTextarea;
import com.skilora.utils.AppThreadPool;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Slider;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Hyperlink;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class PublicProfileController {

    private static final Logger logger = LoggerFactory.getLogger(PublicProfileController.class);
    private final ProfileService profileService = ProfileService.getInstance();
    private final PortfolioService portfolioService = PortfolioService.getInstance();
    private final ReviewService reviewService = ReviewService.getInstance();
    private final CertificateService certificateService = CertificateService.getInstance();
    private final FormationService formationService = FormationService.getInstance();
    
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    @FXML private Label coverPlaceholder;
    @FXML private TLButton shareBtn;
    @FXML private TLAvatar profileAvatar;
    @FXML private Label nameLabel;
    @FXML private TLBadge verifiedBadge;
    @FXML private Label headlineLabel;
    @FXML private Label locationLabel;
    @FXML private Label emailLabel;
    @FXML private Label websiteLabel;
    @FXML private HBox websiteContainer;
    
    @FXML private TLButton hireBtn;
    @FXML private TLButton messageBtn;

    @FXML private Label bioLabel;
    @FXML private FlowPane skillsContainer;
    @FXML private VBox experienceContainer;
    
    @FXML private FlowPane portfolioContainer;
    @FXML private Label emptyPortfolioLabel;
    
    @FXML private VBox reviewsContainer;
    @FXML private Label emptyReviewsLabel;
    
    @FXML private VBox certificatesContainer;
    @FXML private Label emptyCertificatesLabel;
    
    // Stats Placeholders
    @FXML private Label rateLabel;
    @FXML private Label ratingLabel;
    @FXML private Label jobsLabel;

    private User targetUser;
    private Profile targetProfile;

    /**
     * Initializes the view with data for the specified user.
     * @param targetUser The user to display
     */
    public void loadUser(User targetUser) {
        this.targetUser = targetUser;
        updateUIBasic();
        
        AppThreadPool.execute(() -> {
            try {
                // Fetch full profile details
                final Profile profile = profileService.findProfileByUserId(targetUser.getId());
                
                if (profile != null) {
                    Map<String, Object> details = profileService.getProfileWithDetails(profile.getId());
                    Platform.runLater(() -> updateUIFull(details));
                } else {
                    Platform.runLater(() -> showEmptyProfile());
                }
                
            } catch (SQLException e) {
                logger.error("Failed to load profile for user " + targetUser.getUsername(), e);
                Platform.runLater(() -> TLToast.error(nameLabel.getScene(), "Error", "Error loading profile"));
            }
        });
        
        loadPortfolio();
        loadReviews();
        loadCertificates();
    }

    private void updateUIBasic() {
        if (targetUser == null) return;
        nameLabel.setText(targetUser.getFullName());
        emailLabel.setText(targetUser.getEmail());
        
        if (targetUser.getPhotoUrl() != null && !targetUser.getPhotoUrl().isEmpty()) {
             try {
                 profileAvatar.setImage(new Image(targetUser.getPhotoUrl(), 120, 120, true, true));
             } catch (Exception e) {}
        }
        
        verifiedBadge.setVisible(targetUser.isVerified());
        
        // Setup buttons
        hireBtn.setOnAction(e -> handleHire());
        messageBtn.setOnAction(e -> handleMessage());
    }

    @SuppressWarnings("unchecked")
    private void updateUIFull(Map<String, Object> details) {
        this.targetProfile = (Profile) details.get("profile");
        List<Skill> skills = (List<Skill>) details.get("skills");
        List<Experience> experiences = (List<Experience>) details.get("experiences");

        // Profile Details
        if (targetProfile != null) {
            String fullHeadline = targetProfile.getHeadline();
            if (fullHeadline == null || fullHeadline.isEmpty()) fullHeadline = "Skilora Freelancer";
            headlineLabel.setText(fullHeadline);
            
            locationLabel.setText(targetProfile.getLocation() != null ? targetProfile.getLocation() : "Remote");
            
            if (targetProfile.getWebsite() != null && !targetProfile.getWebsite().isEmpty()) {
                websiteLabel.setText(targetProfile.getWebsite());
                websiteContainer.setVisible(true);
            } else {
                websiteContainer.setVisible(false);
            }
            
            bioLabel.setText((targetProfile.getBio() != null && !targetProfile.getBio().isEmpty()) 
                    ? targetProfile.getBio() 
                    : "No biography provided yet.");
            
            // Should prefer Profile photo if set, else User photo
            if (targetProfile.getPhotoUrl() != null && !targetProfile.getPhotoUrl().isEmpty()) {
                try {
                    profileAvatar.setImage(new Image(targetProfile.getPhotoUrl(), 120, 120, true, true));
                } catch (Exception e) {}
            }
        }

        // Skills
        skillsContainer.getChildren().clear();
        if (skills != null && !skills.isEmpty()) {
            for (Skill skill : skills) {
                // Create a pill/tag for each skill
                TLBadge skillBadge = new TLBadge(skill.getSkillName(), TLBadge.Variant.SECONDARY);
                skillBadge.getStyleClass().add("skill-pill"); // Add specific styling if needed
                skillsContainer.getChildren().add(skillBadge);
            }
        } else {
            Label placeholder = new Label("No skills listed.");
            placeholder.getStyleClass().add("text-small-muted");
            skillsContainer.getChildren().add(placeholder);
        }

        // Experience
        experienceContainer.getChildren().clear();
        if (experiences != null && !experiences.isEmpty()) {
            for (Experience exp : experiences) {
                experienceContainer.getChildren().add(createExperienceItem(exp));
            }
        } else {
            Label placeholder = new Label("No experience listed.");
            placeholder.getStyleClass().add("text-small-muted");
            experienceContainer.getChildren().add(placeholder);
        }
        
        // Load Portfolio & Reviews
        loadPortfolio();
        loadReviews();
        
        // Fake Stats (since we don't have this data yet)
        rateLabel.setText("$25.00/hr"); // Placeholder
        jobsLabel.setText("0");        // Placeholder
    }

    private void loadPortfolio() {
        if (portfolioContainer == null) return;
        AppThreadPool.execute(() -> {
            try {
                List<PortfolioItem> items = portfolioService.findByUserId(targetUser.getId());
                Platform.runLater(() -> {
                    portfolioContainer.getChildren().clear();
                    if (items.isEmpty()) {
                        emptyPortfolioLabel.setVisible(true);
                    } else {
                        emptyPortfolioLabel.setVisible(false);
                        for (PortfolioItem item : items) {
                            portfolioContainer.getChildren().add(createPortfolioCard(item));
                        }
                    }
                });
            } catch (SQLException e) {
                logger.error("Failed to load portfolio", e);
            }
        });
    }

    private void loadReviews() {
        if (reviewsContainer == null) return;
        
        final User currentUser = AuthService.getInstance().getCurrentUser();

        AppThreadPool.execute(() -> {
            try {
                List<Review> reviews = reviewService.findByTargetUserId(targetUser.getId());
                Platform.runLater(() -> {
                    reviewsContainer.getChildren().clear();
                    
                    if (currentUser != null && currentUser.getId() != targetUser.getId()) {
                        TLButton writeReviewBtn = new TLButton("Write a Review", TLButton.ButtonVariant.OUTLINE, TLButton.ButtonSize.MD);
                        writeReviewBtn.setGraphic(com.skilora.utils.SvgIcons.icon(com.skilora.utils.SvgIcons.EDIT, 16));
                        
                        writeReviewBtn.setOnAction(e -> handleWriteReview(currentUser));
                        HBox btnContainer = new HBox(writeReviewBtn);
                        btnContainer.setAlignment(Pos.CENTER_RIGHT);
                        btnContainer.setPadding(new javafx.geometry.Insets(0, 0, 16, 0));
                        reviewsContainer.getChildren().add(btnContainer);
                    }

                    if (reviews.isEmpty()) {
                        emptyReviewsLabel.setVisible(true);
                    } else {
                        emptyReviewsLabel.setVisible(false);
                        for (Review r : reviews) {
                            reviewsContainer.getChildren().add(createReviewCard(r));
                        }
                    }
                });
            } catch (SQLException e) {
                logger.error("Failed to load reviews", e);
            }
        });
    }

    private void loadCertificates() {
        if (certificatesContainer == null) return;
        AppThreadPool.execute(() -> {
            List<Certificate> certs = certificateService.findByUser(targetUser.getId());
            Platform.runLater(() -> {
                certificatesContainer.getChildren().clear();
                if (certs.isEmpty()) {
                    emptyCertificatesLabel.setVisible(true);
                } else {
                    emptyCertificatesLabel.setVisible(false);
                    for (Certificate cert : certs) {
                        certificatesContainer.getChildren().add(createCertificateCard(cert));
                    }
                }
            });
        });
    }

    private TLCard createCertificateCard(Certificate cert) {
        TLCard card = new TLCard();
        VBox content = new VBox(8);
        content.setPadding(new javafx.geometry.Insets(12));

        Label titleLabel = new Label("Loading...");
        titleLabel.getStyleClass().add("text-strong");
        titleLabel.setWrapText(true);

        Label certNumber = new Label("N° Certificat " + cert.getCertificateNumber());
        certNumber.getStyleClass().add("text-muted");

        String dateStr = cert.getIssuedDate() != null
                ? "Délivré le " + cert.getIssuedDate().toLocalDate().toString()
                : "";
        Label dateLabel = new Label(dateStr);
        dateLabel.getStyleClass().addAll("text-small-muted");

        HBox actions = new HBox(8);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

        TLButton viewBtn = new TLButton("View", TLButton.ButtonVariant.OUTLINE, TLButton.ButtonSize.SM);
        viewBtn.setOnAction(e -> {
            AppThreadPool.execute(() -> {
                Formation formation = resolveFormationForCert(cert);
                String formationTitle = formation != null ? formation.getTitle() : resolveFormationTitle(cert);
                String directorSignature = formation != null ? formation.getDirectorSignature() : null;
                String recipientName = targetUser.getFullName();
                final String sig = directorSignature;
                Platform.runLater(() -> {
                    if (nameLabel.getScene() != null) {
                        javafx.stage.Stage stage = (javafx.stage.Stage) nameLabel.getScene().getWindow();
                        CertificateViewController.show(stage, cert, recipientName, formationTitle, sig);
                    }
                });
            });
        });

        actions.getChildren().add(viewBtn);
        content.getChildren().addAll(titleLabel, certNumber, dateLabel, actions);
        card.getChildren().add(content);

        // Resolve formation title asynchronously
        AppThreadPool.execute(() -> {
            String fTitle = resolveFormationTitle(cert);
            Platform.runLater(() -> titleLabel.setText(fTitle));
        });

        return card;
    }

    private Formation resolveFormationForCert(Certificate cert) {
        try {
            if (cert.getFormationId() > 0) {
                Formation f = formationService.findById(cert.getFormationId());
                if (f != null) return f;
            }
            if (cert.getEnrollmentId() > 0) {
                com.skilora.formation.entity.Enrollment enrollment =
                        com.skilora.formation.service.EnrollmentService.getInstance().findById(cert.getEnrollmentId());
                if (enrollment != null) {
                    return formationService.findById(enrollment.getFormationId());
                }
            }
        } catch (Exception e) {
            logger.warn("Could not resolve formation for cert {}: {}", cert.getCertificateNumber(), e.getMessage());
        }
        return null;
    }

    private String resolveFormationTitle(Certificate cert) {
        Formation f = resolveFormationForCert(cert);
        return f != null ? f.getTitle() : "Unknown Formation";
    }

    private void handleWriteReview(User currentUser) {
        TLDialog<Review> dialog = new TLDialog<>();
        dialog.setTitle("Write a Review");
        dialog.setDialogTitle("Rate your experience");
        dialog.setDescription("Share your feedback for " + targetUser.getFullName());
        
        VBox form = new VBox(16);
        
        Label ratingLabel = new Label("Rating");
        ratingLabel.getStyleClass().add("text-strong");
        Slider ratingSlider = new Slider(1, 5, 5);
        ratingSlider.setShowTickLabels(true);
        ratingSlider.setShowTickMarks(true);
        ratingSlider.setMajorTickUnit(1);
        ratingSlider.setMinorTickCount(0);
        ratingSlider.setSnapToTicks(true);
        
        TLTextarea commentArea = new TLTextarea("Comment", "Write your review here...");
        
        form.getChildren().addAll(ratingLabel, ratingSlider, commentArea);
        dialog.setContent(form);
        
        dialog.addButton(ButtonType.CANCEL);
        ButtonType submitType = new ButtonType("Submit Review", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(submitType);
        
        dialog.setResultConverter(btnType -> {
             if (btnType == submitType) {
                 Review r = new Review();
                 r.setReviewerId(currentUser.getId());
                 r.setTargetUserId(targetUser.getId());
                 r.setRating((int) ratingSlider.getValue());
                 r.setComment(commentArea.getText());
                 r.setJobId(-1); // No job context here
                 return r;
             }
             return null;
        });
        
        dialog.showAndWait().ifPresent(review -> {
             AppThreadPool.execute(() -> {
                 try {
                     reviewService.create(review);
                     Platform.runLater(() -> {
                         loadReviews();
                         TLToast.success(nameLabel.getScene(), "Success", "Review submitted");
                     });
                 } catch (Exception e) {
                     logger.error("Failed to submit review", e);
                     Platform.runLater(() -> TLToast.error(nameLabel.getScene(), "Error", "Failed to submit review"));
                 }
             });
        });
    }

    private VBox createPortfolioCard(PortfolioItem item) {
        VBox card = new VBox(8);
        card.setPrefWidth(280);
        card.getStyleClass().add("portfolio-card");
        card.setStyle("-fx-background-color: -fx-card; -fx-background-radius: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 4, 0, 0, 1); -fx-padding: 0;");
        
        StackPane imgWrapper = new StackPane();
        imgWrapper.setPrefHeight(160);
        imgWrapper.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8 8 0 0;");
        
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
            ImageView img = new ImageView();
             try {
                img.setImage(new Image(item.getImageUrl(), 280, 160, true, true));
                img.setFitWidth(280);
                img.setFitHeight(160);
                // Create a clip to round top corners
                Rectangle clip = new Rectangle(280, 160);
                clip.setArcWidth(16);
                clip.setArcHeight(16);
                img.setClip(clip);
                imgWrapper.getChildren().add(img);
            } catch (Exception e) {
                 // fallback
            }
        }
        
        VBox content = new VBox(4);
        content.setPadding(new javafx.geometry.Insets(12));
        
        Label title = new Label(item.getTitle());
        title.getStyleClass().add("text-strong");
        title.setWrapText(true);
        
        Label desc = new Label(item.getDescription() != null ? item.getDescription() : "");
        desc.getStyleClass().add("text-small-muted");
        desc.setWrapText(true);
        desc.setMaxHeight(40); // 2 lines approx
        
        content.getChildren().addAll(title, desc);
        
        if (item.getProjectUrl() != null && !item.getProjectUrl().isEmpty()) {
            Hyperlink link = new Hyperlink("View Project");
            link.setOnAction(e -> {
                 // Open link via HostServices usually, but we lack access here perhaps
                 TLToast.info(nameLabel.getScene(), "Info", "Opening project...");
            });
            content.getChildren().add(link);
        }
        
        card.getChildren().addAll(imgWrapper, content);
        return card;
    }

    private TLCard createReviewCard(Review r) {
        VBox content = new VBox(8);
        
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        TLAvatar avatar = new TLAvatar();
        // Use default avatar if no photo
        if (r.getReviewerPhotoUrl() != null && !r.getReviewerPhotoUrl().isEmpty()) {
            try {
                avatar.setImage(new Image(r.getReviewerPhotoUrl(), 40, 40, true, true));
            } catch (Exception e) {}
        }
        avatar.setSize(TLAvatar.Size.DEFAULT);
        
        VBox meta = new VBox(2);
        Label name = new Label(r.getReviewerName() != null ? r.getReviewerName() : "Anonymous Client");
        name.getStyleClass().add("text-strong");
        
        Label ratingText = new Label("★".repeat(r.getRating()));
        ratingText.setStyle("-fx-text-fill: #eab308; -fx-font-size: 14px;");
        
        meta.getChildren().addAll(name, ratingText);
        header.getChildren().addAll(avatar, meta);
        
        Label comment = new Label(r.getComment());
        comment.setWrapText(true);
        
        Label date = new Label(r.getCreatedAt() != null ? DATE_FMT.format(r.getCreatedAt()) : "");
        date.getStyleClass().add("text-xs-muted");
        
        content.getChildren().addAll(header, comment, date);
        
        TLCard card = new TLCard();
        card.getChildren().add(content);
        return card;
    }

    private void showEmptyProfile() {
        bioLabel.setText("This user has not set up their profile yet.");
        skillsContainer.getChildren().clear();
        experienceContainer.getChildren().clear();
    }

    private VBox createExperienceItem(Experience exp) {
        VBox item = new VBox(4);
        item.getStyleClass().add("experience-item");
        item.setPadding(new javafx.geometry.Insets(0, 0, 16, 0));
        
        Label role = new Label(exp.getPosition());
        role.getStyleClass().add("text-strong");
        
        Label company = new Label(exp.getCompany());
        company.getStyleClass().add("text-body");
        
        String dateStr = exp.getStartDate().format(DATE_FMT) + " - " + 
                         (exp.getEndDate() != null ? exp.getEndDate().format(DATE_FMT) : "Present");
        
        Label date = new Label(dateStr);
        date.getStyleClass().add("text-small-muted");
        
        item.getChildren().addAll(role, company, date);
        return item;
    }

    private void handleHire() {
        TLToast.success(nameLabel.getScene(), "Hire Request", "Hire request sent to " + targetUser.getFullName());
    }

    private void handleMessage() {
        // TODO: Navigate to Chat with this user
        TLToast.info(nameLabel.getScene(), "Info", "Features coming soon!");
    }
}
