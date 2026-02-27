package com.skilora.controller.usermanagement;

import com.skilora.model.cv.CvData;
import com.skilora.model.cv.CvData.*;
import com.skilora.model.entity.usermanagement.Experience;
import com.skilora.model.entity.usermanagement.Profile;
import com.skilora.model.entity.usermanagement.Skill;
import com.skilora.model.entity.usermanagement.User;
import com.skilora.service.export.CvPdfExportService;
import com.skilora.service.usermanagement.ProfileService;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

/**
 * CvGeneratorController
 *
 * Manages the CV generator form (CvGeneratorView.fxml).
 * Pre-populates fields from the user's existing profile, skills, and experiences.
 * Collects form data, validates it, then delegates PDF generation to CvPdfExportService.
 */
public class CvGeneratorController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(CvGeneratorController.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/yyyy");

    // ── Personal tab ──────────────────────────────────────────
    @FXML private TextField fullNameField;
    @FXML private TextField jobTitleField;
    @FXML private TextField emailField;
    @FXML private TextField phoneField;
    @FXML private TextField locationField;
    @FXML private TextField linkedInField;
    @FXML private TextField websiteField;
    @FXML private TextArea  summaryArea;

    // ── Dynamic section containers ────────────────────────────
    @FXML private VBox experienceContainer;
    @FXML private VBox educationContainer;
    @FXML private VBox certificationContainer;
    @FXML private VBox projectContainer;

    // ── Skills ────────────────────────────────────────────────
    @FXML private TextArea skillsArea;

    // ── Bottom bar ────────────────────────────────────────────
    @FXML private Label  statusLabel;
    @FXML private Button generateBtn;

    // ── State ─────────────────────────────────────────────────
    private User currentUser;
    private final ProfileService profileService = ProfileService.getInstance();
    private final CvPdfExportService pdfService = new CvPdfExportService();

    // ─────────────────────────────────────────────────────────
    //  Initialisation
    // ─────────────────────────────────────────────────────────

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Empty – real setup happens in setup(User)
    }

    /**
     * Call this after loading the FXML to inject the current user and
     * pre-populate all form fields from the existing profile data.
     */
    public void setup(User user) {
        this.currentUser = user;
        loadAndPopulate();
    }

    private void loadAndPopulate() {
        // Pre-fill personal fields from the User object immediately
        if (currentUser != null) {
            set(fullNameField, currentUser.getFullName());
            set(emailField, currentUser.getEmail());
        }

        // Load the rest from the database on a background thread
        Thread loader = new Thread(() -> {
            try {
                Profile profile = profileService.findProfileByUserId(currentUser.getId());
                if (profile == null) return;

                Map<String, Object> details = profileService.getProfileWithDetails(profile.getId());

                Object rawSkills = details.getOrDefault("skills", new ArrayList<>());
                Object rawExps   = details.getOrDefault("experiences", new ArrayList<>());
                List<Skill>      skills = rawSkills instanceof List ? castSkillList(rawSkills) : new ArrayList<>();
                List<Experience> exps   = rawExps   instanceof List ? castExpList(rawExps)    : new ArrayList<>();

                Platform.runLater(() -> populateFromProfile(profile, skills, exps));

            } catch (Exception e) {
                logger.warn("Could not pre-populate CV form: {}", e.getMessage());
            }
        }, "CvForm-Loader");
        loader.setDaemon(true);
        loader.start();
    }

    private void populateFromProfile(Profile profile, List<Skill> skills, List<Experience> exps) {
        // Personal
        if (notBlank(profile.getFirstName()) || notBlank(profile.getLastName())) {
            set(fullNameField, (profile.getFirstName() + " " + profile.getLastName()).trim());
        }
        set(phoneField,    profile.getPhone());
        set(locationField, profile.getLocation());

        // Skills → comma-separated
        if (!skills.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (Skill s : skills) {
                if (sb.length() > 0) sb.append(", ");
                sb.append(s.getSkillName());
            }
            skillsArea.setText(sb.toString());
        }

        // Experience rows
        for (Experience e : exps) {
            String start = e.getStartDate() != null ? e.getStartDate().format(DATE_FMT) : "";
            String end   = e.isCurrentJob() ? "Présent"
                         : (e.getEndDate() != null ? e.getEndDate().format(DATE_FMT) : "");
            ExperienceEntry entry = new ExperienceEntry(
                    nvl(e.getCompany()), nvl(e.getPosition()), start, end, nvl(e.getDescription()));
            addExperienceRow(entry);
        }

        // Always add one blank row if list is empty
        if (exps.isEmpty()) addExperienceRow(new ExperienceEntry());
    }

    // ─────────────────────────────────────────────────────────
    //  Dynamic row factories
    // ─────────────────────────────────────────────────────────

    @FXML
    private void handleAddExperience() { addExperienceRow(new ExperienceEntry()); }

    @FXML
    private void handleAddEducation() { addEducationRow(new EducationEntry()); }

    @FXML
    private void handleAddCertification() { addCertificationRow(new CertificationEntry()); }

    @FXML
    private void handleAddProject() { addProjectRow(new ProjectEntry()); }

    // ── Experience row ────────────────────────────────────────

    private void addExperienceRow(ExperienceEntry entry) {
        VBox card = entryCard(experienceContainer);

        TextField companyField = labeled(card, "Entreprise", entry.company, "ex. Google");
        TextField roleField    = labeled(card, "Poste / Rôle", entry.role, "ex. Développeur Backend");

        HBox dates = new HBox(16);
        TextField startField = smallField("MM/AAAA", entry.startDate);
        TextField endField   = smallField("MM/AAAA  ou  Présent", entry.endDate);
        dates.getChildren().addAll(
                labeledInline("Date début", startField),
                labeledInline("Date fin",   endField));
        card.getChildren().add(dates);

        TextArea descArea = new TextArea(entry.description);
        descArea.setPromptText("Décrivez vos missions (une par ligne pour des puces dans le PDF)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        card.getChildren().add(descArea);

        // Store field refs as user data for collection later
        card.setUserData(new Object[]{companyField, roleField, startField, endField, descArea});
        experienceContainer.getChildren().add(card);
    }

    // ── Education row ─────────────────────────────────────────

    private void addEducationRow(EducationEntry entry) {
        VBox card = entryCard(educationContainer);

        TextField instField   = labeled(card, "Établissement", entry.institution, "ex. Université de Carthage");
        TextField degreeField = labeled(card, "Diplôme", entry.degree, "ex. Licence / Master / Ingénieur");

        HBox row2 = new HBox(16);
        TextField fieldField = smallField("ex. Informatique", entry.field);
        TextField startYear  = smallField("Année début", entry.startYear);
        TextField endYear    = smallField("Année fin",   entry.endYear);
        row2.getChildren().addAll(
                labeledInline("Filière", fieldField),
                labeledInline("Début",   startYear),
                labeledInline("Fin",     endYear));
        card.getChildren().add(row2);

        card.setUserData(new Object[]{instField, degreeField, fieldField, startYear, endYear});
        educationContainer.getChildren().add(card);
    }

    // ── Certification row ─────────────────────────────────────

    private void addCertificationRow(CertificationEntry entry) {
        VBox card = entryCard(certificationContainer);

        HBox row = new HBox(16);
        TextField nameField   = smallField("Nom de la certification", entry.name);
        TextField issuerField = smallField("Organisme", entry.issuer);
        TextField yearField   = smallField("Année", entry.year);
        yearField.setPrefWidth(80);
        row.getChildren().addAll(
                labeledInline("Certification", nameField),
                labeledInline("Organisme",     issuerField),
                labeledInline("Année",         yearField));
        HBox.setHgrow(nameField,   Priority.ALWAYS);
        HBox.setHgrow(issuerField, Priority.ALWAYS);
        card.getChildren().add(row);

        card.setUserData(new Object[]{nameField, issuerField, yearField});
        certificationContainer.getChildren().add(card);
    }

    // ── Project row ───────────────────────────────────────────

    private void addProjectRow(ProjectEntry entry) {
        VBox card = entryCard(projectContainer);

        TextField nameField = labeled(card, "Nom du projet", entry.name, "ex. Skilora Platform");
        TextField techField = labeled(card, "Technologies", entry.technologies, "ex. Java, Spring Boot, MySQL");

        TextArea descArea = new TextArea(entry.description);
        descArea.setPromptText("Décrivez le projet, vos contributions, les résultats...");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        card.getChildren().add(descArea);

        card.setUserData(new Object[]{nameField, techField, descArea});
        projectContainer.getChildren().add(card);
    }

    // ─────────────────────────────────────────────────────────
    //  Generate PDF
    // ─────────────────────────────────────────────────────────

    @FXML
    private void handleGeneratePdf() {
        String name = fullNameField.getText().trim();
        if (name.isEmpty()) {
            statusLabel.setText("⚠ Le nom complet est obligatoire.");
            return;
        }

        // Choose save location
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Enregistrer le CV PDF");
        chooser.setInitialFileName(name.replaceAll("\\s+", "_") + "_CV.pdf");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier PDF (*.pdf)", "*.pdf"));

        Stage stage = (Stage) generateBtn.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) return;  // user cancelled

        // Build the data object
        CvData data = collectFormData();

        // Disable button, run export off the FX thread
        generateBtn.setDisable(true);
        statusLabel.setText("Génération en cours...");

        Thread exportThread = new Thread(() -> {
            try {
                pdfService.export(data, file);
                Platform.runLater(() -> {
                    statusLabel.setText("✅ CV exporté : " + file.getName());
                    generateBtn.setDisable(false);
                    showSuccessAlert(file);
                });
            } catch (Exception e) {
                logger.error("PDF export failed", e);
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Erreur : " + e.getMessage());
                    generateBtn.setDisable(false);
                });
            }
        }, "CV-Export");
        exportThread.setDaemon(true);
        exportThread.start();
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) generateBtn.getScene().getWindow();
        stage.close();
    }

    // ─────────────────────────────────────────────────────────
    //  Data collection
    // ─────────────────────────────────────────────────────────

    private CvData collectFormData() {
        CvData data = new CvData();

        // Personal
        data.fullName  = text(fullNameField);
        data.jobTitle  = text(jobTitleField);
        data.email     = text(emailField);
        data.phone     = text(phoneField);
        data.location  = text(locationField);
        data.linkedIn  = text(linkedInField);
        data.website   = text(websiteField);
        data.summary   = text(summaryArea);

        // Skills
        String rawSkills = text(skillsArea);
        if (!rawSkills.isEmpty()) {
            for (String s : rawSkills.split(",")) {
                String trimmed = s.trim();
                if (!trimmed.isEmpty()) data.skills.add(trimmed);
            }
        }

        // Experience
        for (javafx.scene.Node node : experienceContainer.getChildren()) {
            if (node instanceof VBox) {
                Object[] fields = (Object[]) ((VBox) node).getUserData();
                if (fields != null && fields.length == 5) {
                    ExperienceEntry e = new ExperienceEntry(
                            tv(fields[0]), tv(fields[1]), tv(fields[2]), tv(fields[3]), tv(fields[4]));
                    if (!e.company.isEmpty() || !e.role.isEmpty())
                        data.experiences.add(e);
                }
            }
        }

        // Education
        for (javafx.scene.Node node : educationContainer.getChildren()) {
            if (node instanceof VBox) {
                Object[] fields = (Object[]) ((VBox) node).getUserData();
                if (fields != null && fields.length == 5) {
                    EducationEntry e = new EducationEntry(
                            tv(fields[0]), tv(fields[1]), tv(fields[2]), tv(fields[3]), tv(fields[4]));
                    if (!e.institution.isEmpty() || !e.degree.isEmpty())
                        data.educations.add(e);
                }
            }
        }

        // Certifications
        for (javafx.scene.Node node : certificationContainer.getChildren()) {
            if (node instanceof VBox) {
                Object[] fields = (Object[]) ((VBox) node).getUserData();
                if (fields != null && fields.length == 3) {
                    CertificationEntry c = new CertificationEntry(tv(fields[0]), tv(fields[1]), tv(fields[2]));
                    if (!c.name.isEmpty())
                        data.certifications.add(c);
                }
            }
        }

        // Projects
        for (javafx.scene.Node node : projectContainer.getChildren()) {
            if (node instanceof VBox) {
                Object[] fields = (Object[]) ((VBox) node).getUserData();
                if (fields != null && fields.length == 3) {
                    ProjectEntry p = new ProjectEntry(tv(fields[0]), tv(fields[1]), tv(fields[2]));
                    if (!p.name.isEmpty())
                        data.projects.add(p);
                }
            }
        }

        return data;
    }

    // ─────────────────────────────────────────────────────────
    //  UI helpers (entry card builder)
    // ─────────────────────────────────────────────────────────

    /** Creates a styled card VBox for one entry and appends a Remove button. */
    private VBox entryCard(VBox parent) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 8; -fx-padding: 14;");

        // Remove button aligned right inside card
        Button removeBtn = new Button("✕ Supprimer");
        removeBtn.getStyleClass().addAll("btn-outline");
        removeBtn.setOnAction(e -> parent.getChildren().remove(card));

        HBox headerRow = new HBox(removeBtn);
        headerRow.setAlignment(Pos.CENTER_RIGHT);
        card.getChildren().add(headerRow);

        return card;
    }

    /** Adds a labeled TextField inside a VBox card and returns the TextField. */
    private TextField labeled(VBox parent, String label, String value, String prompt) {
        VBox wrapper = new VBox(4);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        TextField tf = new TextField(nvl(value));
        tf.setPromptText(prompt);
        wrapper.getChildren().addAll(lbl, tf);
        parent.getChildren().add(wrapper);
        return tf;
    }

    /** Creates a labeled inline group (label + field in a VBox). */
    private VBox labeledInline(String label, TextField field) {
        VBox v = new VBox(4);
        Label lbl = new Label(label);
        lbl.getStyleClass().add("form-label");
        HBox.setHgrow(field, Priority.ALWAYS);
        field.setMaxWidth(Double.MAX_VALUE);
        v.getChildren().addAll(lbl, field);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    private TextField smallField(String prompt, String value) {
        TextField tf = new TextField(nvl(value));
        tf.setPromptText(prompt);
        return tf;
    }

    // ─────────────────────────────────────────────────────────
    //  Misc helpers
    // ─────────────────────────────────────────────────────────

    private void showSuccessAlert(File file) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("CV généré");
        alert.setHeaderText("Votre CV a été généré avec succès !");
        alert.setContentText("Fichier enregistré :\n" + file.getAbsolutePath());
        alert.showAndWait();
    }

    private String text(TextField f)  { return f != null && f.getText() != null ? f.getText().trim() : ""; }
    private String text(TextArea  a)  { return a != null && a.getText() != null ? a.getText().trim() : ""; }

    /** Extract text value from a stored field object (TextField or TextArea). */
    private String tv(Object field) {
        if (field instanceof TextField) return ((TextField) field).getText().trim();
        if (field instanceof TextArea)  return ((TextArea)  field).getText().trim();
        return "";
    }

    private void set(TextField f, String value) {
        if (f != null && value != null && !value.isBlank() && f.getText().isBlank())
            f.setText(value.trim());
    }

    private String nvl(String s) { return s != null ? s : ""; }

    private boolean notBlank(String s) { return s != null && !s.isBlank(); }

    @SuppressWarnings("unchecked")
    private static List<Skill> castSkillList(Object o) { return (List<Skill>) o; }

    @SuppressWarnings("unchecked")
    private static List<Experience> castExpList(Object o) { return (List<Experience>) o; }
}
