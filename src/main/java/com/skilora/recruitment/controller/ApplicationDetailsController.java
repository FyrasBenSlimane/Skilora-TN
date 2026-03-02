package com.skilora.recruitment.controller;

import com.skilora.framework.components.TLAvatar;
import com.skilora.framework.components.TLDialog;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Application.Status;
import com.skilora.recruitment.entity.Interview;
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.service.ApplicationService;
import com.skilora.recruitment.service.InterviewService;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.MatchingService;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.service.ProfileService;
import com.skilora.utils.DialogUtils;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * ApplicationDetailsController
 *
 * Full employer view of a job application. Loads and displays:
 *  - Tab 1: Candidate profile (personal info, skills, experience)
 *  - Tab 2: Application (cover letter, CV, interview info)
 *  - Tab 3: Matching analysis (AI score, matched / missing skills)
 *  - Tab 4: Job offer (description, requirements, salary)
 *
 * Action bar allows Accept / Review / Schedule Interview / Reject
 * without leaving the dialog.
 */
public class ApplicationDetailsController {

    private static final Logger logger = LoggerFactory.getLogger(ApplicationDetailsController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy 'à' HH:mm");

    // Header
    @FXML private TLAvatar candidateAvatar;
    @FXML private Label    candidateNameLabel;
    @FXML private Label    jobInfoLabel;
    @FXML private Label    phoneLabelHdr;
    @FXML private Label    locationLabelHdr;
    @FXML private Label    appliedDateLabel;
    @FXML private Label    matchBadgeLabel;
    @FXML private Label    statusBadgeLabel;
    @FXML private Label    scoreLabelHdr;

    // Tab containers (filled programmatically)
    @FXML private VBox profileTabContent;
    @FXML private VBox applicationTabContent;
    @FXML private VBox matchingTabContent;
    @FXML private VBox offerTabContent;

    // Actions
    @FXML private Button acceptBtn;
    @FXML private Button reviewBtn;
    @FXML private Button interviewBtn;
    @FXML private Button rejectBtn;
    @FXML private Button closeBtn;
    @FXML private Label  actionFeedbackLabel;

    // State
    private Application      application;
    private Profile          candidateProfile;
    private Stage            dialogStage;
    private Consumer<Status> onStatusChanged;

    private final ProfileService     profileService     = ProfileService.getInstance();
    private final JobService         jobService         = JobService.getInstance();
    private final InterviewService   interviewService   = InterviewService.getInstance();
    private final ApplicationService applicationService = ApplicationService.getInstance();

    /**
     * @param app             The application to display
     * @param stage           The owning dialog stage
     * @param onStatusChanged Called with the new status when the employer changes it; may be null
     */
    public void setup(Application app, Stage stage, Consumer<Status> onStatusChanged) {
        this.application     = app;
        this.dialogStage     = stage;
        this.onStatusChanged = onStatusChanged;

        populateHeaderSync();
        styleActionButtons();
        loadAllDataAsync();
    }

    public void setup(Application app, Stage stage) {
        setup(app, stage, null);
    }

    // ── Header (synchronous) ──────────────────────────────────────

    private void populateHeaderSync() {
        String name = nvl(application.getCandidateName(), "Candidat #" + application.getCandidateProfileId());
        candidateNameLabel.setText(name);
        if (candidateAvatar != null) {
            candidateAvatar.setFallback(initials(name));
            candidateAvatar.setSize(TLAvatar.Size.LG);
        }

        String job = nvl(application.getJobTitle(), "Offre #" + application.getJobOfferId());
        String company = application.getCompanyName();
        jobInfoLabel.setText(company != null ? job + " · " + company : job);

        if (application.getAppliedDate() != null) {
            appliedDateLabel.setText("Postulé le " + application.getAppliedDate().format(DATE_FMT));
        }

        styleMatchBadge(application.getMatchPercentage());
        styleStatusBadge(application.getStatus());

        if (application.getCandidateScore() > 0) {
            scoreLabelHdr.setText("Score talent : " + application.getCandidateScore() + " / 100");
        }
    }

    // ── Async data load ───────────────────────────────────────────

    private void loadAllDataAsync() {
        Thread loader = new Thread(() -> {
            try {
                candidateProfile = profileService.findProfileById(application.getCandidateProfileId());
                List<Skill>      skills  = profileService.findSkillsByProfileId(application.getCandidateProfileId());
                List<Experience> exps    = profileService.findExperiencesByProfileId(application.getCandidateProfileId());

                JobOffer offer = null;
                try { offer = jobService.findJobOfferById(application.getJobOfferId()); } catch (Exception ignored) {}

                Interview interview = null;
                try {
                    List<Interview> interviews = interviewService.findByApplication(application.getId());
                    if (!interviews.isEmpty()) interview = interviews.get(0);
                } catch (Exception ignored) {}

                MatchingService.MatchResult matchResult =
                        MatchingService.getInstance().quickMatch(
                                application.getCandidateProfileId(), application.getJobOfferId());

                int liveScore = matchResult.score;
                if (liveScore > application.getMatchPercentage()) {
                    application.setMatchPercentage(liveScore);
                }

                final JobOffer      fOffer     = offer;
                final Interview     fInterview = interview;
                final MatchingService.MatchResult fMatch = matchResult;

                Platform.runLater(() -> {
                    refreshHeaderWithProfile();
                    styleMatchBadge(application.getMatchPercentage());
                    buildProfileTab(skills, exps);
                    buildApplicationTab(fInterview);
                    buildMatchingTab(fMatch);
                    buildOfferTab(fOffer);
                });
            } catch (Exception e) {
                logger.error("Failed to load application details", e);
                Platform.runLater(() -> {
                    buildProfileTab(List.of(), List.of());
                    buildApplicationTab(null);
                    buildMatchingTab(MatchingService.MatchResult.EMPTY);
                    buildOfferTab(null);
                });
            }
        }, "AppDetails-Loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void refreshHeaderWithProfile() {
        if (candidateProfile == null) return;
        String fullName = candidateProfile.getFullName();
        if (fullName == null || fullName.isBlank())
            fullName = nvl(application.getCandidateName(), candidateNameLabel.getText());

        candidateNameLabel.setText(fullName);
        if (candidateAvatar != null) {
            candidateAvatar.setFallback(initials(fullName));
            loadPhoto(candidateProfile.getPhotoUrl());
        }
        if (candidateProfile.getPhone() != null && !candidateProfile.getPhone().isBlank())
            phoneLabelHdr.setText(candidateProfile.getPhone());
        if (candidateProfile.getLocation() != null && !candidateProfile.getLocation().isBlank())
            locationLabelHdr.setText(candidateProfile.getLocation());
    }

    // ── TAB 1 – Profil candidat ───────────────────────────────────

    private void buildProfileTab(List<Skill> skills, List<Experience> exps) {
        profileTabContent.getChildren().clear();

        profileTabContent.getChildren().add(sectionTitle("Informations personnelles"));
        GridPane grid = new GridPane();
        grid.setHgap(24); grid.setVgap(10);
        int row = 0;
        if (candidateProfile != null) {
            addRow(grid, row++, "Prénom",      candidateProfile.getFirstName());
            addRow(grid, row++, "Nom",         candidateProfile.getLastName());
            addRow(grid, row++, "Téléphone",   candidateProfile.getPhone());
            addRow(grid, row++, "Localisation",candidateProfile.getLocation());
            if (candidateProfile.getBirthDate() != null)
                addRow(grid, row++, "Date de naissance", candidateProfile.getBirthDate().format(DATE_FMT));
        } else {
            addRow(grid, row++, "Nom", nvl(application.getCandidateName(), "—"));
        }
        profileTabContent.getChildren().add(grid);

        // Skills
        profileTabContent.getChildren().add(sectionTitle("Compétences (" + skills.size() + ")"));
        if (skills.isEmpty()) {
            profileTabContent.getChildren().add(mutedLabel("Aucune compétence enregistrée."));
        } else {
            FlowPane chips = new FlowPane(); chips.setHgap(8); chips.setVgap(8);
            for (Skill s : skills) {
                Label chip = new Label(s.getSkillName());
                chip.setStyle("-fx-background-color:#27272a; -fx-text-fill:#e4e4e7; -fx-padding:4 10; -fx-background-radius:20; -fx-font-size:12;");
                if (s.getProficiencyLevel() != null)
                    Tooltip.install(chip, new Tooltip(s.getProficiencyLevel().name()));
                chips.getChildren().add(chip);
            }
            profileTabContent.getChildren().add(chips);
        }

        // Experience
        profileTabContent.getChildren().add(sectionTitle("Expérience professionnelle (" + exps.size() + ")"));
        if (exps.isEmpty()) {
            profileTabContent.getChildren().add(mutedLabel("Aucune expérience enregistrée."));
        } else {
            for (Experience e : exps) profileTabContent.getChildren().add(buildExperienceCard(e));
        }
    }

    private HBox buildExperienceCard(Experience e) {
        HBox card = new HBox(14);
        card.setStyle("-fx-background-color:-fx-muted; -fx-background-radius:8; -fx-border-color:-fx-border; -fx-border-radius:8; -fx-border-width:1; -fx-padding:14;");
        card.setAlignment(Pos.TOP_LEFT);

        VBox dateCol = new VBox(2); dateCol.setMinWidth(100);
        String start = e.getStartDate() != null ? e.getStartDate().format(DATE_FMT) : "?";
        String end   = e.isCurrentJob() ? "Présent" : (e.getEndDate() != null ? e.getEndDate().format(DATE_FMT) : "?");
        Label dateRange = new Label(start + "\n" + end);
        dateRange.getStyleClass().add("text-muted"); dateRange.setStyle("-fx-font-size:11;"); dateRange.setWrapText(true);
        dateCol.getChildren().add(dateRange);

        VBox content = new VBox(4); HBox.setHgrow(content, Priority.ALWAYS);
        Label posLabel = new Label(nvl(e.getPosition(), "Poste non précisé"));
        posLabel.setStyle("-fx-font-weight:bold;");
        Label compLabel = new Label(nvl(e.getCompany(), "Entreprise inconnue"));
        compLabel.getStyleClass().add("text-muted");
        content.getChildren().addAll(posLabel, compLabel);
        if (e.getDescription() != null && !e.getDescription().isBlank()) {
            Label desc = new Label(e.getDescription());
            desc.getStyleClass().add("text-muted"); desc.setWrapText(true); desc.setStyle("-fx-font-size:12;");
            content.getChildren().add(desc);
        }
        card.getChildren().addAll(dateCol, new Separator(javafx.geometry.Orientation.VERTICAL), content);
        return card;
    }

    // ── TAB 2 – Candidature ──────────────────────────────────────

    private void buildApplicationTab(Interview interview) {
        applicationTabContent.getChildren().clear();

        // Cover letter
        applicationTabContent.getChildren().add(sectionTitle("Lettre de motivation"));
        String cl = application.getCoverLetter();
        if (cl != null && !cl.isBlank()) {
            TextArea coverArea = new TextArea(cl);
            coverArea.setEditable(false); coverArea.setWrapText(true); coverArea.setPrefRowCount(10);
            applicationTabContent.getChildren().add(coverArea);
        } else {
            applicationTabContent.getChildren().add(mutedLabel("Aucune lettre de motivation fournie."));
        }

        // CV
        applicationTabContent.getChildren().add(sectionTitle("Curriculum Vitae"));
        String cvUrl = application.getCustomCvUrl();
        if ((cvUrl == null || cvUrl.isBlank()) && candidateProfile != null) cvUrl = candidateProfile.getCvUrl();
        HBox cvRow = new HBox(12); cvRow.setAlignment(Pos.CENTER_LEFT);
        if (cvUrl != null && !cvUrl.isBlank()) {
            Label cvName = new Label(Paths.get(cvUrl).getFileName().toString());
            cvName.getStyleClass().add("text-muted");
            Button openBtn = new Button("Ouvrir le CV"); openBtn.getStyleClass().add("btn-outline");
            String finalCvUrl = cvUrl;
            openBtn.setOnAction(e -> openFile(finalCvUrl));
            cvRow.getChildren().addAll(cvName, openBtn);
        } else {
            cvRow.getChildren().add(mutedLabel("Aucun CV joint à cette candidature."));
        }
        applicationTabContent.getChildren().add(cvRow);

        // Interview
        applicationTabContent.getChildren().add(sectionTitle("Entretien"));
        if (interview == null) {
            applicationTabContent.getChildren().add(mutedLabel("Aucun entretien programmé pour le moment."));
        } else {
            GridPane ivGrid = new GridPane(); ivGrid.setHgap(20); ivGrid.setVgap(8);
            int row = 0;
            if (interview.getScheduledDate() != null)
                addRow(ivGrid, row++, "Date et heure", interview.getScheduledDate().format(DATETIME_FMT));
            if (interview.getType() != null)
                addRow(ivGrid, row++, "Type", interview.getType());
            if (interview.getLocation() != null && !interview.getLocation().isBlank())
                addRow(ivGrid, row++, "Lieu", interview.getLocation());
            if (interview.getStatus() != null)
                addRow(ivGrid, row++, "Statut", interview.getStatus());
            if (interview.getNotes() != null && !interview.getNotes().isBlank())
                addRow(ivGrid, row++, "Notes", interview.getNotes());
            applicationTabContent.getChildren().add(ivGrid);
        }

        // Application metadata
        applicationTabContent.getChildren().add(sectionTitle("Informations de la candidature"));
        GridPane meta = new GridPane(); meta.setHgap(20); meta.setVgap(8);
        int r = 0;
        addRow(meta, r++, "ID candidature", String.valueOf(application.getId()));
        addRow(meta, r++, "Date de postulation",
                application.getAppliedDate() != null ? application.getAppliedDate().format(DATETIME_FMT) : "—");
        addRow(meta, r++, "Statut actuel", application.getStatus().getDisplayName());
        applicationTabContent.getChildren().add(meta);
    }

    // ── TAB 3 – Matching ─────────────────────────────────────────

    private void buildMatchingTab(MatchingService.MatchResult result) {
        matchingTabContent.getChildren().clear();

        int score = result.score;
        String colour = result.colorHex();

        VBox scoreBox = new VBox(8); scoreBox.setAlignment(Pos.CENTER); scoreBox.setMaxWidth(250);
        scoreBox.setStyle("-fx-background-color:" + colour + "22; -fx-border-color:" + colour + "; -fx-border-radius:12; -fx-background-radius:12; -fx-padding:20;");

        Label scoreValue = new Label(score + "%");
        scoreValue.setStyle("-fx-font-size:48; -fx-font-weight:bold; -fx-text-fill:" + colour + ";");
        Label scoreLabel = new Label(result.label());
        scoreLabel.setStyle("-fx-font-size:14; -fx-font-weight:600; -fx-text-fill:" + colour + ";");
        Label scoreSub = new Label(result.matchedSkills.size() + " / " + result.totalRequired + " compétences requises trouvées");
        scoreSub.getStyleClass().add("text-muted"); scoreSub.setStyle("-fx-font-size:12;");
        ProgressBar bar = new ProgressBar(score / 100.0); bar.setPrefWidth(180); bar.setStyle("-fx-accent:" + colour + ";");
        scoreBox.getChildren().addAll(scoreValue, scoreLabel, bar, scoreSub);

        HBox scoreRow = new HBox(scoreBox); scoreRow.setAlignment(Pos.CENTER_LEFT);
        matchingTabContent.getChildren().add(scoreRow);

        if (application.getCandidateScore() > 0) {
            HBox qualBox = new HBox(12); qualBox.setAlignment(Pos.CENTER_LEFT);
            Label qualTitle = new Label("Score de qualité du candidat :");
            Label qualValue = new Label(application.getCandidateScore() + " / 100");
            qualValue.setStyle("-fx-font-weight:bold;");
            ProgressBar qualBar = new ProgressBar(application.getCandidateScore() / 100.0); qualBar.setPrefWidth(140);
            qualBox.getChildren().addAll(qualTitle, qualBar, qualValue);
            matchingTabContent.getChildren().add(qualBox);
        }

        // Matched skills
        matchingTabContent.getChildren().add(sectionTitle("Compétences correspondantes (" + result.matchedSkills.size() + ")"));
        if (result.matchedSkills.isEmpty()) {
            matchingTabContent.getChildren().add(mutedLabel("Aucune compétence correspondante détectée."));
        } else {
            FlowPane matched = new FlowPane(); matched.setHgap(8); matched.setVgap(8);
            for (String s : result.matchedSkills) {
                Label chip = new Label(s);
                chip.setStyle("-fx-background-color:#14532d; -fx-text-fill:#bbf7d0; -fx-padding:4 10; -fx-background-radius:20; -fx-font-size:12; -fx-font-weight:600;");
                matched.getChildren().add(chip);
            }
            matchingTabContent.getChildren().add(matched);
        }

        // Missing skills
        matchingTabContent.getChildren().add(sectionTitle("Compétences manquantes (" + result.missingSkills.size() + ")"));
        if (result.missingSkills.isEmpty()) {
            Label perfect = new Label("Toutes les compétences requises sont présentes !");
            perfect.setStyle("-fx-text-fill:#16a34a; -fx-font-weight:bold;");
            matchingTabContent.getChildren().add(perfect);
        } else {
            FlowPane missing = new FlowPane(); missing.setHgap(8); missing.setVgap(8);
            for (String s : result.missingSkills) {
                Label chip = new Label(s);
                chip.setStyle("-fx-background-color:#7f1d1d; -fx-text-fill:#fca5a5; -fx-padding:4 10; -fx-background-radius:20; -fx-font-size:12; -fx-font-weight:600;");
                missing.getChildren().add(chip);
            }
            matchingTabContent.getChildren().add(missing);
        }

        // Recommendation
        matchingTabContent.getChildren().add(sectionTitle("Recommandation"));
        String advice;
        if      (score >= 85) advice = "Excellente correspondance — candidat fortement recommandé.";
        else if (score >= 70) advice = "Bonne correspondance — candidat pertinent pour ce poste.";
        else if (score >= 50) advice = "Correspondance partielle — quelques lacunes techniques.";
        else if (score >= 30) advice = "Correspondance faible — profil à évaluer avec précaution.";
        else                  advice = "Faible correspondance — profil peu adapté à cette offre.";
        Label adviceLabel = new Label(advice); adviceLabel.setWrapText(true); adviceLabel.setStyle("-fx-font-size:13;");
        matchingTabContent.getChildren().add(adviceLabel);
    }

    // ── TAB 4 – Offre d'emploi ───────────────────────────────────

    private void buildOfferTab(JobOffer offer) {
        offerTabContent.getChildren().clear();
        if (offer == null) {
            offerTabContent.getChildren().add(mutedLabel("Impossible de charger les détails de l'offre."));
            return;
        }

        offerTabContent.getChildren().add(sectionTitle("Informations principales"));
        GridPane grid = new GridPane(); grid.setHgap(24); grid.setVgap(10);
        int row = 0;
        addRow(grid, row++, "Titre",           nvl(offer.getTitle(), "—"));
        addRow(grid, row++, "Entreprise",      nvl(offer.getCompanyName(), "—"));
        addRow(grid, row++, "Localisation",    nvl(offer.getLocation(), "—"));
        addRow(grid, row++, "Type de travail", nvl(offer.getWorkType(), "—"));
        if (offer.getSalaryMin() > 0 || offer.getSalaryMax() > 0) {
            String sal = offer.getSalaryRange() + " " + nvl(offer.getCurrency(), "TND");
            addRow(grid, row++, "Salaire", sal);
        }
        if (offer.getPostedDate() != null) addRow(grid, row++, "Date de publication", offer.getPostedDate().format(DATE_FMT));
        if (offer.getDeadline() != null) addRow(grid, row++, "Date limite", offer.getDeadline().format(DATE_FMT));
        addRow(grid, row++, "Statut offre", offer.getStatus() != null ? offer.getStatus().name() : "—");
        offerTabContent.getChildren().add(grid);

        // Required skills
        List<String> req = offer.getRequiredSkills();
        offerTabContent.getChildren().add(sectionTitle("Compétences requises (" + (req == null ? 0 : req.size()) + ")"));
        if (req == null || req.isEmpty()) {
            offerTabContent.getChildren().add(mutedLabel("Aucune compétence spécifiée."));
        } else {
            FlowPane chips = new FlowPane(); chips.setHgap(8); chips.setVgap(8);
            for (String s : req) {
                Label chip = new Label(s);
                chip.setStyle("-fx-background-color:#1e3a5f; -fx-text-fill:#bae6fd; -fx-padding:4 10; -fx-background-radius:20; -fx-font-size:12;");
                chips.getChildren().add(chip);
            }
            offerTabContent.getChildren().add(chips);
        }

        // Description
        if (offer.getDescription() != null && !offer.getDescription().isBlank()) {
            offerTabContent.getChildren().add(sectionTitle("Description du poste"));
            TextArea desc = new TextArea(offer.getDescription());
            desc.setEditable(false); desc.setWrapText(true); desc.setPrefRowCount(12);
            offerTabContent.getChildren().add(desc);
        }
    }

    // ── Action handlers ──────────────────────────────────────────

    @FXML private void handleAccept()  { updateStatus(Status.ACCEPTED, "Candidature acceptée."); }
    @FXML private void handleReview()  { updateStatus(Status.REVIEWING, "Candidature mise en révision."); }

    @FXML
    private void handleReject() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Confirmer le refus de la candidature de " + candidateNameLabel.getText() + " ?",
                ButtonType.YES, ButtonType.NO);
        confirm.setHeaderText("Refuser la candidature");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) updateStatus(Status.REJECTED, "Candidature refusée.");
        });
    }

    @FXML
    private void handleScheduleInterview() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/skilora/view/recruitment/ScheduleInterviewView.fxml"));
            VBox content = loader.load();
            ScheduleInterviewController ctrl = loader.getController();

            if (ctrl != null) {
                ctrl.setup(application, null, () -> {
                    updateStatus(Status.INTERVIEW, "Entretien programmé — statut mis à jour.");
                    loadAllDataAsync();
                });
            }

            TLDialog<Boolean> dialog = new TLDialog<>();
            dialog.initOwner(dialogStage);
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setDialogTitle("Programmer un entretien");
            dialog.setDescription("Définissez la date, l'heure et le type d'entretien.");
            dialog.setContent(content);

            ButtonType saveBtnType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            ButtonType cancelBtnType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
            dialog.getDialogPane().getButtonTypes().addAll(saveBtnType, cancelBtnType);
            dialog.setResultConverter(btn -> {
                if (btn == saveBtnType && ctrl != null && ctrl.validateAndSave()) return true;
                return null;
            });
            dialog.showAndWait();
        } catch (Exception e) {
            logger.error("Failed to open schedule interview dialog", e);
            feedback("Impossible d'ouvrir le formulaire d'entretien.");
        }
    }

    @FXML
    private void handleClose() {
        if (dialogStage != null) dialogStage.close();
        else if (closeBtn != null && closeBtn.getScene() != null)
            ((Stage) closeBtn.getScene().getWindow()).close();
    }

    // ── Helpers ──────────────────────────────────────────────────

    private void updateStatus(Status newStatus, String message) {
        Thread t = new Thread(() -> {
            try {
                applicationService.updateStatus(application.getId(), newStatus);
                application.setStatus(newStatus);
                Platform.runLater(() -> {
                    styleStatusBadge(newStatus);
                    styleActionButtons();
                    feedback(message);
                    if (onStatusChanged != null) onStatusChanged.accept(newStatus);
                });
            } catch (Exception e) {
                logger.error("Failed to update status", e);
                Platform.runLater(() -> feedback("Erreur lors de la mise à jour du statut."));
            }
        }, "AppDetails-StatusUpdate");
        t.setDaemon(true);
        t.start();
    }

    private void styleMatchBadge(int score) {
        if (score <= 0) {
            matchBadgeLabel.setText("Match : —");
            matchBadgeLabel.setStyle("-fx-text-fill:-fx-muted-foreground;");
            return;
        }
        String colour;
        if      (score >= 85) colour = "#16a34a";
        else if (score >= 70) colour = "#2563eb";
        else if (score >= 50) colour = "#d97706";
        else if (score >= 30) colour = "#ea580c";
        else                  colour = "#dc2626";
        matchBadgeLabel.setText("Match : " + score + "%");
        matchBadgeLabel.setStyle("-fx-background-color:" + colour + "; -fx-text-fill:white; -fx-padding:4 12; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:13;");
    }

    private void styleStatusBadge(Status status) {
        String bg, fg, label;
        switch (status) {
            case ACCEPTED:  bg = "#14532d"; fg = "#bbf7d0"; label = "Accepté";     break;
            case REJECTED:  bg = "#7f1d1d"; fg = "#fca5a5"; label = "Refusé";      break;
            case INTERVIEW: bg = "#1e3a5f"; fg = "#bae6fd"; label = "Entretien";   break;
            case REVIEWING: bg = "#312e81"; fg = "#c7d2fe"; label = "En révision"; break;
            case OFFER:     bg = "#064e3b"; fg = "#a7f3d0"; label = "Offre faite"; break;
            default:        bg = "#27272a"; fg = "#a1a1aa"; label = "En attente";  break;
        }
        statusBadgeLabel.setText(label);
        statusBadgeLabel.setStyle("-fx-background-color:" + bg + "; -fx-text-fill:" + fg + "; -fx-padding:4 12; -fx-background-radius:20; -fx-font-weight:bold; -fx-font-size:12;");
    }

    private void styleActionButtons() {
        if (application == null) return;
        Status s = application.getStatus();
        if (acceptBtn    != null) acceptBtn.setDisable(s == Status.ACCEPTED || s == Status.REJECTED);
        if (rejectBtn    != null) rejectBtn.setDisable(s == Status.REJECTED);
        if (reviewBtn    != null) reviewBtn.setDisable(s == Status.REVIEWING);
        if (interviewBtn != null) interviewBtn.setDisable(s == Status.REJECTED);
    }

    private void feedback(String msg) { if (actionFeedbackLabel != null) actionFeedbackLabel.setText(msg); }

    private void loadPhoto(String url) {
        if (url == null || url.isBlank() || candidateAvatar == null) return;
        try {
            File f = new File(url);
            String uri = f.exists() ? f.toURI().toString() : url;
            Image img = new Image(uri, 80, 80, true, true, true);
            img.progressProperty().addListener((obs, o, n) -> {
                if (n.doubleValue() >= 1.0 && !img.isError())
                    Platform.runLater(() -> candidateAvatar.setImage(img));
            });
        } catch (Exception ignored) {}
    }

    private void openFile(String path) {
        try {
            File f = Paths.get(path).toFile();
            if (!f.exists()) { DialogUtils.showError("Erreur", "Fichier introuvable : " + path); return; }
            if (java.awt.Desktop.isDesktopSupported()) java.awt.Desktop.getDesktop().open(f);
        } catch (Exception e) {
            DialogUtils.showError("Erreur", "Impossible d'ouvrir le fichier.");
        }
    }

    private Label sectionTitle(String text) {
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size:14; -fx-font-weight:bold; -fx-border-color:transparent transparent -fx-border transparent; -fx-border-width:0 0 1.5 0; -fx-padding:0 0 6 0;");
        lbl.setMaxWidth(Double.MAX_VALUE);
        return lbl;
    }

    private Label mutedLabel(String text) { Label l = new Label(text); l.getStyleClass().add("text-muted"); return l; }

    private void addRow(GridPane g, int row, String key, String value) {
        Label k = new Label(key); k.setStyle("-fx-text-fill:-fx-muted-foreground; -fx-font-weight:600;"); k.setMinWidth(160);
        Label v = new Label(value != null && !value.isBlank() ? value : "—"); v.setWrapText(true);
        g.add(k, 0, row); g.add(v, 1, row);
    }

    private static String initials(String name) {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private static String nvl(String s, String fallback) { return (s != null && !s.isBlank()) ? s : fallback; }
}
