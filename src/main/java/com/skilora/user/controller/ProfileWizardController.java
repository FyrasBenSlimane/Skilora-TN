package com.skilora.user.controller;

import com.skilora.framework.components.TLAvatar;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLToast;
import com.skilora.user.entity.User;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.PortfolioItem;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.enums.ProficiencyLevel;
import com.skilora.user.service.BiometricService;
import com.skilora.user.service.PortfolioService;
import com.skilora.user.service.ProfileService;
import com.skilora.user.service.UserService;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.ImageUtils;
import com.skilora.utils.SvgIcons;
import com.skilora.utils.Validators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.skilora.user.ui.PhotoCropDialog;
import com.skilora.utils.I18n;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import com.skilora.framework.components.TLTextarea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;
import javafx.concurrent.Task;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.util.Base64;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ProfileWizardController {

    private static final Logger logger = LoggerFactory.getLogger(ProfileWizardController.class);

    @FXML
    private TLAvatar photoAvatar;
    @FXML
    private TLButton uploadPhotoBtn;
    @FXML
    private TLButton removePhotoBtn;
    @FXML
    private Label photoStatusLabel;
    @FXML
    private Label cvSectionLabel;
    @FXML
    private TLButton uploadCvBtn;
    @FXML
    private TLButton removeCvBtn;
    @FXML
    private Label cvStatusLabel;

    @FXML
    private TLTextField firstNameField;
    @FXML
    private TLTextField lastNameField;
    @FXML
    private TLTextField headlineField;
    @FXML
    private TLTextarea bioArea;

    @FXML
    private TLTextField emailField;
    @FXML
    private TLTextField phoneField;
    @FXML
    private TLTextField locationField;
    @FXML
    private TLTextField websiteField;

    @FXML private Label skillsSectionLabel;
    @FXML private TLTextField skillNameField;
    @FXML private TLSelect<String> skillLevelSelect;
    @FXML private TLButton addSkillBtn;
    @FXML private FlowPane skillsContainer;

    @FXML private Label experienceSectionLabel;
    @FXML private TLButton addExperienceBtn;
    @FXML private VBox experienceList;

    @FXML private Label portfolioSectionLabel;
    @FXML private TLButton addPortfolioBtn;
    @FXML private FlowPane portfolioList;

    @FXML
    private TLButton saveBtn;
    @FXML
    private TLButton cancelBtn;
    @FXML
    private HBox actionsBar;
    @FXML
    private Label statusLabel;
    @FXML
    private Label biometricStatusLabel;
    @FXML
    private TLButton biometricBtn;
    @FXML
    private Label profileTitleLabel;
    @FXML
    private Label profileSubtitleLabel;
    @FXML
    private Label photoSectionLabel;
    @FXML
    private Label personalInfoLabel;
    @FXML
    private Label bioLabel;
    @FXML
    private Label contactLabel;
    @FXML
    private Label biometricSectionLabel;
    @FXML
    private Label biometricDescLabel;

    @FXML
    private TLButton viewPublicProfileBtn;

    private User currentUser;
    private Profile currentProfile;
    private UserService userService;
    private ProfileService profileService;
    private PortfolioService portfolioService;
    private Runnable onProfileUpdated;
    private Runnable onViewPublicProfile;
    private boolean dirty = false;

    /** Snapshot of original field values for cancel/reset */
    private String origFirstName, origLastName, origHeadline, origBio;
    private String origEmail, origPhone, origLocation, origWebsite;

    private final List<Skill> skills = new ArrayList<>();
    private final List<Experience> experiences = new ArrayList<>();

    private static final DateTimeFormatter EXP_DATE_FMT = DateTimeFormatter.ofPattern("MMM yyyy");

    public void initializeContext(User user, Runnable onProfileUpdated) {
        this.currentUser = user;
        this.onProfileUpdated = onProfileUpdated;
        this.userService = UserService.getInstance();
        this.profileService = ProfileService.getInstance();
        this.portfolioService = PortfolioService.getInstance();

        if (viewPublicProfileBtn != null) {
            viewPublicProfileBtn.setText(I18n.get("profile.view_public"));
            viewPublicProfileBtn.setOnAction(e -> {
                if (onViewPublicProfile != null) onViewPublicProfile.run();
            });
        }

        applyI18n();
        adaptUIForRole();
        loadProfileFromDb();
        populateFields();
        setupSkillsAndExperience();
        setupPortfolio();
    }

    private void applyI18n() {
        if (profileTitleLabel != null) profileTitleLabel.setText(I18n.get("profile.title"));
        if (profileSubtitleLabel != null) profileSubtitleLabel.setText(I18n.get("profile.subtitle"));
        if (photoSectionLabel != null) photoSectionLabel.setText(I18n.get("profile.photo.title"));
        if (uploadPhotoBtn != null) uploadPhotoBtn.setText(I18n.get("profile.photo.change"));
        if (removePhotoBtn != null) removePhotoBtn.setText(I18n.get("profile.photo.remove"));
        if (photoStatusLabel != null) photoStatusLabel.setText(I18n.get("profile.photo.formats"));
        if (cvSectionLabel != null) cvSectionLabel.setText(I18n.get("profile.cv.title"));
        if (uploadCvBtn != null) uploadCvBtn.setText(I18n.get("profile.cv.upload"));
        if (removeCvBtn != null) removeCvBtn.setText(I18n.get("profile.cv.remove"));
        if (cvStatusLabel != null) cvStatusLabel.setText(I18n.get("profile.cv.formats"));
        updateCvUi();
        if (personalInfoLabel != null) personalInfoLabel.setText(I18n.get("profile.personal_info"));
        if (firstNameField != null) { firstNameField.setLabel(I18n.get("profile.first_name")); firstNameField.setPromptText(I18n.get("profile.first_name.placeholder")); }
        if (lastNameField != null) { lastNameField.setLabel(I18n.get("profile.last_name")); lastNameField.setPromptText(I18n.get("profile.last_name.placeholder")); }
        if (headlineField != null) { headlineField.setLabel(I18n.get("profile.job_title")); headlineField.setPromptText(I18n.get("profile.job_title.placeholder")); }
        if (bioLabel != null) bioLabel.setText(I18n.get("profile.bio"));
        if (bioArea != null) bioArea.setPromptText(I18n.get("profile.bio.placeholder"));
        if (contactLabel != null) contactLabel.setText(I18n.get("profile.contact"));
        if (emailField != null) emailField.setLabel(I18n.get("register.email"));
        if (phoneField != null) { phoneField.setLabel(I18n.get("profile.phone")); phoneField.setPromptText("+216"); }
        if (locationField != null) { locationField.setLabel(I18n.get("profile.city")); locationField.setPromptText(I18n.get("profile.city.placeholder")); }
        if (websiteField != null) { websiteField.setLabel(I18n.get("profile.website")); websiteField.setPromptText("https://"); }

        if (skillsSectionLabel != null) skillsSectionLabel.setText(I18n.get("profile.skills.title"));
        if (skillNameField != null) { skillNameField.setLabel(I18n.get("profile.skills.add.label")); skillNameField.setPromptText(I18n.get("profile.skills.add.prompt")); }
        if (skillLevelSelect != null) {
            skillLevelSelect.getItems().setAll(
                    ProficiencyLevel.BEGINNER.getDisplayName(),
                    ProficiencyLevel.INTERMEDIATE.getDisplayName(),
                    ProficiencyLevel.ADVANCED.getDisplayName(),
                    ProficiencyLevel.EXPERT.getDisplayName()
            );
            if (skillLevelSelect.getValue() == null) {
                skillLevelSelect.setValue(ProficiencyLevel.INTERMEDIATE.getDisplayName());
            }
        }
        if (addSkillBtn != null) addSkillBtn.setText(I18n.get("profile.skills.add.action"));

        if (experienceSectionLabel != null) experienceSectionLabel.setText(I18n.get("profile.experience.title"));
        if (addExperienceBtn != null) addExperienceBtn.setText(I18n.get("profile.experience.add.action"));

        if (biometricSectionLabel != null) biometricSectionLabel.setText(I18n.get("profile.biometric.title"));
        if (biometricDescLabel != null) biometricDescLabel.setText(I18n.get("profile.biometric.subtitle"));
        if (cancelBtn != null) cancelBtn.setText(I18n.get("common.cancel"));
        if (saveBtn != null) saveBtn.setText(I18n.get("profile.save"));
    }

    /**
     * Loads (or creates) the Profile row from the database for the current user.
     */
    private void adaptUIForRole() {
        if (currentUser == null) return;

        if (currentUser.getRole() == com.skilora.user.enums.Role.EMPLOYER) {
            // Employer View: Hide personal skills/experience, show company info labels
            if (skillsSectionLabel != null) {
                skillsSectionLabel.setVisible(false);
                skillsSectionLabel.setManaged(false);
            }
            if (skillsContainer != null) {
                skillsContainer.setVisible(false);
                skillsContainer.setManaged(false);
            }
            if (addSkillBtn != null) {
                addSkillBtn.setVisible(false);
                addSkillBtn.setManaged(false);
            }

            if (experienceSectionLabel != null) {
                experienceSectionLabel.setVisible(false);
                experienceSectionLabel.setManaged(false);
            }
            if (experienceList != null) {
                experienceList.setVisible(false);
                experienceList.setManaged(false);
            }
            if (addExperienceBtn != null) {
                addExperienceBtn.setVisible(false);
                addExperienceBtn.setManaged(false);
            }

            // Rename Bio -> Company Description
            if (bioLabel != null) bioLabel.setText(I18n.get("profile.bio.company"));
            if (bioArea != null) bioArea.setPromptText(I18n.get("profile.bio.company.placeholder"));
            
            // Rename Headline -> Company Tagline
            if (headlineField != null) {
                headlineField.setPromptText(I18n.get("profile.headline.company.placeholder"));
                // If there's a label for headline, update it too (assuming prompts for now based on FXML structure inference)
            }
            
            if (profileSubtitleLabel != null) {
                profileSubtitleLabel.setText(I18n.get("profile.company.info"));
            }

        } else {
            // Freelancer / User View: Ensure everything is visible
            if (skillsSectionLabel != null) {
                skillsSectionLabel.setVisible(true);
                skillsSectionLabel.setManaged(true);
            }
            if (skillsContainer != null) {
                skillsContainer.setVisible(true);
                skillsContainer.setManaged(true);
            }
            if (addSkillBtn != null) {
                addSkillBtn.setVisible(true);
                addSkillBtn.setManaged(true);
            }
            // ... similar for experience if needed, though default is visible
        }
    }

    private void loadProfileFromDb() {
        try {
            currentProfile = profileService.findProfileByUserId(currentUser.getId());
            if (currentProfile == null) {
                // First visit — seed names from User.fullName
                String firstName = "";
                String lastName = "";
                if (currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
                    String[] parts = currentUser.getFullName().trim().split("\\s+", 2);
                    firstName = parts[0];
                    if (parts.length > 1) lastName = parts[1];
                }
                currentProfile = new Profile(currentUser.getId(),
                        firstName.isEmpty() ? "-" : firstName,
                        lastName.isEmpty() ? "-" : lastName);
                profileService.createProfile(currentProfile);
            }
        } catch (Exception e) {
            logger.error("Failed to load/create profile for user {}", currentUser.getId(), e);
            currentProfile = new Profile(currentUser.getId(), "-", "-");
        }
    }

    private void populateFields() {
        if (currentUser == null)
            return;

        // Populate from Profile entity (has precedence for name parts)
        if (currentProfile != null) {
            if (currentProfile.getFirstName() != null) {
                firstNameField.setText(currentProfile.getFirstName());
            }
            if (currentProfile.getLastName() != null) {
                lastNameField.setText(currentProfile.getLastName());
            }
            if (headlineField != null && currentProfile.getHeadline() != null) {
                headlineField.setText(currentProfile.getHeadline());
            }
            if (bioArea != null && currentProfile.getBio() != null) {
                bioArea.setText(currentProfile.getBio());
            }
            if (phoneField != null && currentProfile.getPhone() != null) {
                phoneField.setText(currentProfile.getPhone());
            }
            if (locationField != null && currentProfile.getLocation() != null) {
                locationField.setText(currentProfile.getLocation());
            }
            if (websiteField != null && currentProfile.getWebsite() != null) {
                websiteField.setText(currentProfile.getWebsite());
            }
        }

        // Fallback: if Profile names are empty, split from User.fullName
        if ((firstNameField.getText() == null || firstNameField.getText().isBlank())
                && currentUser.getFullName() != null) {
            String[] parts = currentUser.getFullName().split(" ", 2);
            firstNameField.setText(parts[0]);
            if (parts.length > 1)
                lastNameField.setText(parts[1]);
        }

        // Populate email from User entity
        if (emailField != null && currentUser.getEmail() != null) {
            emailField.setText(currentUser.getEmail());
        }

        // Load profile photo
        loadProfilePhoto();
        updateCvUi();

        // Check biometric status
        updateBiometricStatus();

        // Snapshot original values for cancel/reset
        snapshotOriginalValues();

        // Install dirty-state listeners (must be AFTER population)
        setupDirtyTracking();
    }

    private void setupSkillsAndExperience() {
        if (addSkillBtn != null) {
            addSkillBtn.setOnAction(e -> handleAddSkill());
        }
        if (addExperienceBtn != null) {
            addExperienceBtn.setOnAction(e -> handleAddExperience(null));
        }
        loadSkillsAndExperiences();
    }

    private void loadSkillsAndExperiences() {
        if (currentProfile == null) return;
        int profileId = currentProfile.getId();
        if (profileId <= 0) return;

        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Skill> s = profileService.findSkillsByProfileId(profileId);
                List<Experience> ex = profileService.findExperiencesByProfileId(profileId);
                skills.clear();
                experiences.clear();
                if (s != null) skills.addAll(s);
                if (ex != null) experiences.addAll(ex);
                return null;
            }
        };

        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            renderSkills();
            renderExperiences();
        }));
        task.setOnFailed(e -> logger.warn("Failed to load skills/experiences: {}", task.getException().getMessage()));

        AppThreadPool.execute(task);
    }

    private void renderSkills() {
        if (skillsContainer == null) return;
        skillsContainer.getChildren().clear();
        for (Skill s : skills) {
            HBox chip = new HBox(6);
            chip.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            String label = s.getSkillName();
            if (s.getProficiencyLevel() != null) {
                label = label + " · " + s.getProficiencyLevel().getDisplayName();
            }
            TLBadge badge = new TLBadge(label, TLBadge.Variant.SECONDARY);

            TLButton remove = new TLButton("", TLButton.ButtonVariant.GHOST);
            remove.setGraphic(SvgIcons.icon(SvgIcons.X_CIRCLE, 12, "-fx-muted-foreground"));
            remove.setTooltip(new Tooltip("Remove"));
            remove.setOnAction(e -> deleteSkill(s));

            chip.getChildren().addAll(badge, remove);
            chip.getStyleClass().add("skill-chip");
            skillsContainer.getChildren().add(chip);
        }
    }

    private void deleteSkill(Skill skill) {
        if (skill == null || skill.getId() <= 0) return;
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return profileService.deleteSkill(skill.getId());
            }
        };
        task.setOnSucceeded(e -> {
            boolean ok = task.getValue() != null && task.getValue();
            javafx.application.Platform.runLater(() -> {
                if (ok) {
                    skills.remove(skill);
                    renderSkills();
                    markDirty();
                }
            });
        });
        task.setOnFailed(e -> logger.warn("Failed to delete skill: {}", task.getException().getMessage()));
        AppThreadPool.execute(task);
    }

    private void handleAddSkill() {
        if (currentProfile == null || skillNameField == null) return;
        String name = skillNameField.getText();
        if (name == null || name.trim().isEmpty()) {
            TLToast.warning(skillNameField.getScene(), I18n.get("common.error"), I18n.get("profile.skills.add.required"));
            return;
        }
        ProficiencyLevel level = ProficiencyLevel.INTERMEDIATE;
        if (skillLevelSelect != null && skillLevelSelect.getValue() != null) {
            String val = skillLevelSelect.getValue();
            for (ProficiencyLevel p : ProficiencyLevel.values()) {
                if (p.getDisplayName().equalsIgnoreCase(val)) {
                    level = p;
                    break;
                }
            }
        }

        Skill skill = new Skill();
        skill.setProfileId(currentProfile.getId());
        skill.setSkillName(name.trim());
        skill.setProficiencyLevel(level);
        skill.setYearsExperience(0);
        skill.setVerified(false);

        addSkillBtn.setDisable(true);
        Task<Integer> task = new Task<>() {
            @Override
            protected Integer call() throws Exception {
                return profileService.addOrUpdateSkill(skill);
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            addSkillBtn.setDisable(false);
            int id = task.getValue() != null ? task.getValue() : -1;
            if (id > 0) {
                skillNameField.setText("");
                // refresh list (covers update + correct ordering)
                loadSkillsAndExperiences();
                markDirty();
            } else {
                TLToast.error(skillNameField.getScene(), I18n.get("common.error"), I18n.get("profile.skills.add.failed"));
            }
        }));
        task.setOnFailed(e -> javafx.application.Platform.runLater(() -> {
            addSkillBtn.setDisable(false);
            TLToast.error(skillNameField.getScene(), I18n.get("common.error"), I18n.get("profile.skills.add.failed"));
        }));
        AppThreadPool.execute(task);
    }

    private void renderExperiences() {
        if (experienceList == null) return;
        experienceList.getChildren().clear();
        for (Experience exp : experiences) {
            TLCard card = new TLCard();
            VBox body = new VBox(8);

            HBox header = new HBox(10);
            header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

            Label title = new Label((exp.getPosition() != null ? exp.getPosition() : "-")
                    + " @ " + (exp.getCompany() != null ? exp.getCompany() : "-"));
            title.getStyleClass().add("h4");

            Region spacer = new Region();
            HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

            TLButton edit = new TLButton(I18n.get("common.edit"), TLButton.ButtonVariant.OUTLINE);
            edit.setOnAction(e -> handleAddExperience(exp));

            TLButton del = new TLButton(I18n.get("common.delete"), TLButton.ButtonVariant.GHOST);
            del.getStyleClass().add("text-destructive");
            del.setOnAction(e -> deleteExperience(exp));

            header.getChildren().addAll(title, spacer, edit, del);

            String range = formatExpRange(exp.getStartDate(), exp.isCurrentJob() ? null : exp.getEndDate(), exp.isCurrentJob());
            Label dates = new Label(range);
            dates.getStyleClass().add("text-muted");

            body.getChildren().addAll(header, dates);

            if (exp.getDescription() != null && !exp.getDescription().isBlank()) {
                Label desc = new Label(exp.getDescription());
                desc.setWrapText(true);
                desc.getStyleClass().add("text-sm");
                body.getChildren().add(desc);
            }

            card.setBody(body);
            experienceList.getChildren().add(card);
        }
    }

    private String formatExpRange(LocalDate start, LocalDate end, boolean current) {
        if (start == null) return "-";
        String s = EXP_DATE_FMT.format(start);
        if (current) return s + " — " + I18n.get("profile.experience.present");
        if (end == null) return s;
        return s + " — " + EXP_DATE_FMT.format(end);
    }

    private void handleAddExperience(Experience existing) {
        if (currentProfile == null) return;
        TLDialog<ButtonType> dialog = new TLDialog<>();
        if (saveBtn != null && saveBtn.getScene() != null) {
            dialog.initOwner(saveBtn.getScene().getWindow());
        }
        dialog.setDialogTitle(existing == null ? I18n.get("profile.experience.add.title") : I18n.get("profile.experience.edit.title"));
        dialog.setDescription(existing == null ? I18n.get("profile.experience.add.desc") : I18n.get("profile.experience.edit.desc"));

        TLTextField companyField = new TLTextField(I18n.get("profile.experience.company"), "");
        TLTextField positionField = new TLTextField(I18n.get("profile.experience.position"), "");
        DatePicker startPicker = new DatePicker();
        DatePicker endPicker = new DatePicker();
        startPicker.getStyleClass().add("text-field");
        endPicker.getStyleClass().add("text-field");

        CheckBox currentJob = new CheckBox(I18n.get("profile.experience.current"));
        currentJob.getStyleClass().add("checkbox");
        currentJob.selectedProperty().addListener((obs, was, isSel) -> endPicker.setDisable(isSel));

        TLTextarea descArea = new TLTextarea(I18n.get("profile.experience.description"), "");

        if (existing != null) {
            companyField.setText(existing.getCompany());
            positionField.setText(existing.getPosition());
            if (existing.getStartDate() != null) startPicker.setValue(existing.getStartDate());
            if (existing.getEndDate() != null) endPicker.setValue(existing.getEndDate());
            currentJob.setSelected(existing.isCurrentJob());
            descArea.setText(existing.getDescription());
        }

        VBox content = new VBox(12,
                companyField,
                positionField,
                new VBox(6, new Label(I18n.get("profile.experience.start_date")), startPicker),
                new VBox(6, new Label(I18n.get("profile.experience.end_date")), endPicker),
                currentJob,
                descArea
        );
        content.getChildren().add(0, new TLSeparator());

        dialog.setContent(content);
        ButtonType saveType = new ButtonType(I18n.get("common.save"), ButtonBar.ButtonData.OK_DONE);
        dialog.addButton(saveType);
        dialog.addButton(ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(saveType).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                boolean valid = true;
                String company = companyField.getText() != null ? companyField.getText().trim() : "";
                String position = positionField.getText() != null ? positionField.getText().trim() : "";
                if (company.isEmpty()) {
                    companyField.setError(I18n.get("error.validation.required", I18n.get("profile.experience.company")));
                    valid = false;
                } else { companyField.clearValidation(); }
                if (position.isEmpty()) {
                    positionField.setError(I18n.get("error.validation.required", I18n.get("profile.experience.position")));
                    valid = false;
                } else { positionField.clearValidation(); }
                if (!valid) event.consume();
            }
        );

        dialog.setResultConverter(bt -> bt == saveType ? saveType : null);
        dialog.showAndWait().ifPresent(bt -> {
            Experience exp = existing != null ? existing : new Experience();
            exp.setProfileId(currentProfile.getId());
            exp.setCompany(companyField.getText());
            exp.setPosition(positionField.getText());
            exp.setStartDate(startPicker.getValue());
            exp.setCurrentJob(currentJob.isSelected());
            exp.setEndDate(currentJob.isSelected() ? null : endPicker.getValue());
            exp.setDescription(descArea.getText());
            persistExperience(exp, existing != null);
        });
    }

    private void persistExperience(Experience exp, boolean isUpdate) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if (isUpdate) {
                    return profileService.updateExperience(exp);
                }
                return profileService.addExperience(exp) > 0;
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            loadSkillsAndExperiences();
            markDirty();
        }));
        task.setOnFailed(e -> logger.warn("Failed to persist experience: {}", task.getException().getMessage()));
        AppThreadPool.execute(task);
    }

    private void deleteExperience(Experience exp) {
        if (exp == null || exp.getId() <= 0) return;
        if (saveBtn == null || saveBtn.getScene() == null) return;
        boolean ok = DialogUtils.showConfirmation(
                I18n.get("common.confirm"),
                I18n.get("profile.experience.delete.confirm"))
                .orElse(ButtonType.CANCEL) == ButtonType.OK;
        if (!ok) return;

        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                return profileService.deleteExperience(exp.getId());
            }
        };
        task.setOnSucceeded(e -> javafx.application.Platform.runLater(() -> {
            loadSkillsAndExperiences();
            markDirty();
        }));
        task.setOnFailed(e -> logger.warn("Failed to delete experience: {}", task.getException().getMessage()));
        AppThreadPool.execute(task);
    }

    /** Stores current field values so cancel can restore them. */
    private void snapshotOriginalValues() {
        origFirstName = getText(firstNameField);
        origLastName  = getText(lastNameField);
        origHeadline  = getText(headlineField);
        origBio       = bioArea != null ? (bioArea.getText() != null ? bioArea.getText() : "") : "";
        origEmail     = getText(emailField);
        origPhone     = getText(phoneField);
        origLocation  = getText(locationField);
        origWebsite   = getText(websiteField);
    }

    private String getText(TLTextField field) {
        if (field == null) return "";
        return field.getText() != null ? field.getText() : "";
    }

    /** Installs text-change listeners on every editable field to show/hide the actions bar. */
    private void setupDirtyTracking() {
        javafx.beans.value.ChangeListener<String> dirtyListener = (obs, oldVal, newVal) -> markDirty();

        if (firstNameField != null) firstNameField.getControl().textProperty().addListener(dirtyListener);
        if (lastNameField  != null) lastNameField.getControl().textProperty().addListener(dirtyListener);
        if (headlineField  != null) headlineField.getControl().textProperty().addListener(dirtyListener);
        if (emailField     != null) emailField.getControl().textProperty().addListener(dirtyListener);
        if (phoneField     != null) phoneField.getControl().textProperty().addListener(dirtyListener);
        if (locationField  != null) locationField.getControl().textProperty().addListener(dirtyListener);
        if (websiteField   != null) websiteField.getControl().textProperty().addListener(dirtyListener);
        if (bioArea        != null) bioArea.textProperty().addListener(dirtyListener);
    }

    /** Shows the actions bar when the form becomes dirty. */
    private void markDirty() {
        if (!dirty) {
            dirty = true;
            if (actionsBar != null) {
                actionsBar.setVisible(true);
                actionsBar.setManaged(true);
            }
            statusLabel.setText("");
        }
    }

    /** Hides the actions bar and clears dirty state. */
    private void clearDirty() {
        dirty = false;
        if (actionsBar != null) {
            actionsBar.setVisible(false);
            actionsBar.setManaged(false);
        }
    }

    @FXML
    private void handleCancel() {
        // Restore original values
        if (firstNameField != null) firstNameField.setText(origFirstName);
        if (lastNameField  != null) lastNameField.setText(origLastName);
        if (headlineField  != null) headlineField.setText(origHeadline);
        if (bioArea        != null) bioArea.setText(origBio);
        if (emailField     != null) emailField.setText(origEmail);
        if (phoneField     != null) phoneField.setText(origPhone);
        if (locationField  != null) locationField.setText(origLocation);
        if (websiteField   != null) websiteField.setText(origWebsite);

        clearDirty();
    }

    private void loadProfilePhoto() {
        if (photoAvatar == null) return;

        // Set avatar size to LG for the profile wizard
        photoAvatar.setSize(TLAvatar.Size.LG);

        String initials = "?";
        if (currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
            String[] parts = currentUser.getFullName().trim().split("\\s+");
            if (parts.length >= 2) {
                initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            } else {
                initials = parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase();
            }
        }
        photoAvatar.setFallback(initials);

        String photoUrl = currentUser.getPhotoUrl();
        Image img = ImageUtils.loadProfileImage(photoUrl, 96, 96);
        if (img != null) {
            photoAvatar.setImage(img);
            if (removePhotoBtn != null) removePhotoBtn.setVisible(true);
        } else {
            photoAvatar.setImage(null);
            if (removePhotoBtn != null) removePhotoBtn.setVisible(false);
        }
    }

    @FXML
    private void handleUploadPhoto() {
        if (currentUser == null) return;

        Stage stage = (Stage) uploadPhotoBtn.getScene().getWindow();
        java.util.Optional<String> croppedDataUri = PhotoCropDialog.chooseAndCrop(stage);

        if (croppedDataUri.isEmpty()) return;

        try {
            String base64DataUri = croppedDataUri.get();
            currentUser.setPhotoUrl(base64DataUri);

            Image img = ImageUtils.decodeBase64ToImage(base64DataUri, 96, 96);
            if (img != null) {
                photoAvatar.setImage(img);
                removePhotoBtn.setVisible(true);
                photoStatusLabel.setText(I18n.get("profile.photo.uploaded_cropped", "Photo recadrée et enregistrée."));
                photoStatusLabel.getStyleClass().removeAll("text-destructive", "text-muted");
                photoStatusLabel.getStyleClass().add("text-success");
            }
        } catch (Exception e) {
            logger.error("Failed to set cropped photo", e);
            photoStatusLabel.setText(I18n.get("profile.photo.error.encode_failed"));
            photoStatusLabel.getStyleClass().removeAll("text-success", "text-muted");
            photoStatusLabel.getStyleClass().add("text-destructive");
        }
    }

    @FXML
    private void handleRemovePhoto() {
        currentUser.setPhotoUrl(null);
        photoAvatar.setImage(null);
        removePhotoBtn.setVisible(false);
        photoStatusLabel.setText(I18n.get("profile.photo.removed"));
        photoStatusLabel.getStyleClass().removeAll("text-destructive", "text-success");
        photoStatusLabel.getStyleClass().add("text-muted");
    }

    private static final long MAX_CV_SIZE_BYTES = 10L * 1024 * 1024;

    private void updateCvUi() {
        if (cvStatusLabel == null || removeCvBtn == null) return;
        boolean hasCv = currentProfile != null && currentProfile.getCvUrl() != null && !currentProfile.getCvUrl().isBlank();
        removeCvBtn.setVisible(hasCv);
        if (hasCv) {
            cvStatusLabel.setText(I18n.get("profile.cv.uploaded", "—"));
            cvStatusLabel.getStyleClass().removeAll("text-destructive", "text-muted");
            cvStatusLabel.getStyleClass().add("text-success");
        } else {
            cvStatusLabel.setText(I18n.get("profile.cv.formats"));
            cvStatusLabel.getStyleClass().removeAll("text-success", "text-destructive");
            cvStatusLabel.getStyleClass().add("text-muted");
        }
    }

    @FXML
    private void handleUploadCv() {
        if (currentProfile == null) return;
        FileChooser fc = new FileChooser();
        fc.setTitle(I18n.get("profile.cv.upload"));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF, DOC, DOCX", "*.pdf", "*.doc", "*.docx"));
        Stage stage = uploadCvBtn != null && uploadCvBtn.getScene() != null ? (Stage) uploadCvBtn.getScene().getWindow() : null;
        File file = fc.showOpenDialog(stage);
        if (file == null) return;
        if (file.length() > MAX_CV_SIZE_BYTES) {
            if (cvStatusLabel != null) {
                cvStatusLabel.setText(I18n.get("profile.cv.error.too_large"));
                cvStatusLabel.getStyleClass().removeAll("text-success", "text-muted");
                cvStatusLabel.getStyleClass().add("text-destructive");
            }
            return;
        }
        String name = file.getName().toLowerCase();
        if (!name.endsWith(".pdf") && !name.endsWith(".doc") && !name.endsWith(".docx")) {
            if (cvStatusLabel != null) {
                cvStatusLabel.setText(I18n.get("profile.cv.error.invalid_format"));
                cvStatusLabel.getStyleClass().removeAll("text-success", "text-muted");
                cvStatusLabel.getStyleClass().add("text-destructive");
            }
            return;
        }
        try {
            byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
            String mime = name.endsWith(".pdf") ? "application/pdf" : "application/msword";
            if (name.endsWith(".docx")) mime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            String base64 = Base64.getEncoder().encodeToString(bytes);
            currentProfile.setCvUrl("data:" + mime + ";base64," + base64);
            markDirty();
            updateCvUi();
        } catch (Exception e) {
            logger.error("Failed to read CV file", e);
            if (cvStatusLabel != null) {
                cvStatusLabel.setText(I18n.get("profile.photo.error.encode_failed"));
                cvStatusLabel.getStyleClass().removeAll("text-success", "text-muted");
                cvStatusLabel.getStyleClass().add("text-destructive");
            }
        }
    }

    @FXML
    private void handleRemoveCv() {
        if (currentProfile == null) return;
        currentProfile.setCvUrl(null);
        markDirty();
        updateCvUi();
    }

    private void updateBiometricStatus() {
        AppThreadPool.execute(() -> {
            try {
                boolean hasBiometric = BiometricService.getInstance().hasBiometricData(currentUser.getUsername());
                javafx.application.Platform.runLater(() -> {
                    if (hasBiometric) {
                        biometricStatusLabel.setText(I18n.get("profile.biometric.status.configured"));
                        biometricStatusLabel.getStyleClass().removeAll("text-destructive");
                        biometricStatusLabel.getStyleClass().addAll("text-success", "font-bold");
                        biometricBtn.setText(I18n.get("profile.biometric.reconfigure"));
                    } else {
                        biometricStatusLabel.setText(I18n.get("profile.biometric.status.not_configured"));
                        biometricStatusLabel.getStyleClass().removeAll("text-success");
                        biometricStatusLabel.getStyleClass().addAll("text-destructive", "font-bold");
                        biometricBtn.setText(I18n.get("profile.biometric.configure"));
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to check biometric status", e);
            }
        });
    }

    @FXML
    private void handleBiometricSetup() {
        if (currentUser == null) return;

        Stage ownerStage = (Stage) biometricBtn.getScene().getWindow();
        BiometricAuthController.showRegistrationDialog(ownerStage, currentUser.getUsername(), (registeredUser) -> {
            updateBiometricStatus();
            com.skilora.utils.DialogUtils.showInfo(
                    I18n.get("profile.biometric.success"),
                    I18n.get("profile.biometric.success_msg"));
        });
    }

    private boolean validateProfileFields() {
        boolean valid = true;

        if (firstNameField != null) {
            String fn = firstNameField.getText();
            if (fn == null || fn.trim().isEmpty()) {
                firstNameField.setError(I18n.get("validation.fullname.required"));
                valid = false;
            } else { firstNameField.clearValidation(); }
        }
        if (emailField != null) {
            String err = Validators.validateEmail(emailField.getText());
            if (err != null) { emailField.setError(err); valid = false; }
            else { emailField.clearValidation(); }
        }
        if (phoneField != null && phoneField.getText() != null && !phoneField.getText().trim().isEmpty()) {
            String err = Validators.validatePhone(phoneField.getText());
            if (err != null) { phoneField.setError(err); valid = false; }
            else { phoneField.clearValidation(); }
        }
        return valid;
    }

    @FXML
    private void handleSave() {
        if (currentUser == null)
            return;

        if (!validateProfileFields()) return;

        saveBtn.setDisable(true);
        saveBtn.setText(I18n.get("profile.saving"));
        statusLabel.setText(I18n.get("profile.saving"));

        Task<Void> saveTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String firstName = firstNameField.getText() != null ? firstNameField.getText().trim() : "";
                String lastName = lastNameField.getText() != null ? lastNameField.getText().trim() : "";

                // Update User entity
                currentUser.setFullName((firstName + " " + lastName).trim());

                String email = emailField != null ? emailField.getText() : null;
                if (email != null && !email.trim().isEmpty()) {
                    currentUser.setEmail(email.trim());
                }

                userService.saveUser(currentUser);

                // Update Profile entity
                currentProfile.setFirstName(firstName.isEmpty() ? null : firstName);
                currentProfile.setLastName(lastName.isEmpty() ? null : lastName);

                if (headlineField != null) {
                    String h = headlineField.getText();
                    currentProfile.setHeadline(h != null && !h.isBlank() ? h.trim() : null);
                }
                if (bioArea != null) {
                    String b = bioArea.getText();
                    currentProfile.setBio(b != null && !b.isBlank() ? b.trim() : null);
                }
                if (phoneField != null) {
                    String p = phoneField.getText();
                    currentProfile.setPhone(p != null && !p.isBlank() ? p.trim() : null);
                }
                if (locationField != null) {
                    String l = locationField.getText();
                    currentProfile.setLocation(l != null && !l.isBlank() ? l.trim() : null);
                }
                if (websiteField != null) {
                    String w = websiteField.getText();
                    currentProfile.setWebsite(w != null && !w.isBlank() ? w.trim() : null);
                }

                // Sync photo URL and CV to profile
                currentProfile.setPhotoUrl(currentUser.getPhotoUrl());
                // cvUrl is already on currentProfile (set by handleUploadCv/handleRemoveCv)

                profileService.updateProfile(currentProfile);
                return null;
            }
        };

        saveTask.setOnSucceeded(e -> {
            saveBtn.setDisable(false);
            saveBtn.setText(I18n.get("profile.save"));
            statusLabel.setText(I18n.get("profile.updated"));
            statusLabel.getStyleClass().removeAll("text-destructive");
            statusLabel.getStyleClass().add("text-success");

            // Re-snapshot and clear dirty state
            snapshotOriginalValues();
            dirty = false;
            // Keep actionsBar visible briefly so user sees success message
            // It will hide on next populateFields or after a short delay
            javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(javafx.util.Duration.seconds(2));
            pause.setOnFinished(ev -> clearDirty());
            pause.play();

            if (onProfileUpdated != null) {
                onProfileUpdated.run();
            }
        });

        saveTask.setOnFailed(e -> {
            saveBtn.setDisable(false);
            saveBtn.setText(I18n.get("profile.save"));
            statusLabel.setText(I18n.get("profile.error"));
            statusLabel.getStyleClass().removeAll("text-success");
            statusLabel.getStyleClass().add("text-destructive");
            logger.error("Failed to save profile", e.getSource().getException());
        });

        AppThreadPool.execute(saveTask);
    }

    public void setOnViewPublicProfile(Runnable onViewPublicProfile) {
        this.onViewPublicProfile = onViewPublicProfile;
    }

    private void setupPortfolio() {
        if (addPortfolioBtn != null) {
            addPortfolioBtn.setOnAction(e -> handleAddPortfolioItem());
        }
        loadPortfolio();
    }

    private void loadPortfolio() {
        if (portfolioList == null) return;
        
        AppThreadPool.execute(() -> {
            try {
                // Ensure userId is valid
                if (currentUser == null) return;
                
                List<PortfolioItem> items = portfolioService.findByUserId(currentUser.getId());
                Platform.runLater(() -> {
                    portfolioList.getChildren().clear();
                    for (PortfolioItem item : items) {
                        portfolioList.getChildren().add(createPortfolioCard(item));
                    }
                });
            } catch (Exception e) {
                logger.error("Failed to load portfolio items", e);
            }
        });
    }

    private void handleAddPortfolioItem() {
        TLDialog<PortfolioItem> dialog = new TLDialog<>();
        dialog.setTitle("Add Portfolio Item");
        dialog.setDialogTitle("Add New Project");
        dialog.setDescription("Showcase your best work.");

        VBox form = new VBox(16);
        TLTextField titleField = new TLTextField("Project Title", "e.g. E-commerce Website");
        TLTextarea descArea = new TLTextarea("Description", "Briefly describe the project...");
        TLTextField imgUrlField = new TLTextField("Image URL", "https://...");
        TLTextField projectUrlField = new TLTextField("Project Link", "https://...");

        form.getChildren().addAll(titleField, descArea, imgUrlField, projectUrlField);
        dialog.setContent(form);

        dialog.addButton(ButtonType.CANCEL);
        ButtonType addButtonType = new ButtonType("Add Item", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().add(addButtonType);

        dialog.getDialogPane().lookupButton(addButtonType).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String title = titleField.getText() != null ? titleField.getText().trim() : "";
                if (title.isEmpty()) {
                    titleField.setError("Project title is required");
                    event.consume();
                } else {
                    titleField.clearValidation();
                }
            }
        );

        dialog.setResultConverter(btnType -> {
            if (btnType == addButtonType) {
                return new PortfolioItem(
                    currentUser.getId(), 
                    titleField.getText().trim(), 
                    descArea.getText(), 
                    imgUrlField.getText(), 
                    projectUrlField.getText()
                );
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newItem -> {
            AppThreadPool.execute(() -> {
                try {
                    portfolioService.create(newItem);
                    Platform.runLater(() -> {
                        loadPortfolio();
                        if (photoAvatar.getScene() != null)
                            TLToast.success(photoAvatar.getScene(), "Success", "Portfolio item added");
                    });
                } catch (Exception e) {
                    logger.error("Failed to create portfolio item", e);
                    Platform.runLater(() -> {
                        if (photoAvatar.getScene() != null)
                            TLToast.error(photoAvatar.getScene(), "Error", "Failed to add item");
                    });
                }
            });
        });
    }

    private HBox createPortfolioCard(PortfolioItem item) {
        // Thumbnail
        javafx.scene.layout.StackPane thumb = new javafx.scene.layout.StackPane();
        thumb.setPrefSize(60, 60);
        thumb.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 6;");
        if (item.getImageUrl() != null && !item.getImageUrl().isEmpty()) {
             ImageView img = new ImageView();
             try {
                img.setImage(new Image(item.getImageUrl(), 60, 60, true, true));
                img.setFitWidth(60);
                img.setFitHeight(60);
                // Create a clip
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(60, 60);
                clip.setArcWidth(6);
                clip.setArcHeight(6);
                img.setClip(clip);
                thumb.getChildren().add(img);
            } catch (Exception e) {}
        }

        VBox info = new VBox(4);
        Label title = new Label(item.getTitle());
        title.getStyleClass().add("text-strong");
        Label link = new Label(item.getProjectUrl());
        link.getStyleClass().add("text-xs-muted");
        info.getChildren().addAll(title, link);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton deleteBtn = new TLButton("", TLButton.ButtonVariant.DANGER, TLButton.ButtonSize.SM);
        deleteBtn.setGraphic(com.skilora.utils.SvgIcons.icon(com.skilora.utils.SvgIcons.TRASH, 16));
        
        deleteBtn.setOnAction(e -> {
             if (DialogUtils.showConfirmation("Delete Item", "Are you sure you want to delete this portfolio item?")
                     .orElse(ButtonType.CANCEL) == ButtonType.OK) {
                 AppThreadPool.execute(() -> {
                     try {
                         portfolioService.delete(item.getId());
                         Platform.runLater(() -> {
                             loadPortfolio(); 
                             if (photoAvatar.getScene() != null)
                                TLToast.success(photoAvatar.getScene(), "Success", "Item deleted");
                         });
                     } catch (Exception ex) {
                         logger.error("Failed to delete item", ex);
                     }
                 });
             }
        });

        HBox card = new HBox(12);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: -fx-card; -fx-padding: 12; -fx-background-radius: 8; -fx-border-color: -fx-border; -fx-border-width: 1; -fx-effect: null;");
        card.setPrefWidth(280);
        card.getChildren().addAll(thumb, info, spacer, deleteBtn);
        return card;
    }
}
