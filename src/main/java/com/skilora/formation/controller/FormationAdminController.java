package com.skilora.formation.controller;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLDialog;
import com.skilora.framework.components.TLSelect;
import com.skilora.framework.components.TLSeparator;
import com.skilora.framework.components.TLTextField;
import com.skilora.framework.components.TLTextarea;
import com.skilora.framework.components.TLToast;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.entity.FormationModule;
import com.skilora.formation.enums.FormationLevel;
import com.skilora.formation.service.EnrollmentService;
import com.skilora.formation.service.FormationModuleService;
import com.skilora.formation.service.FormationService;
import com.skilora.user.entity.User;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.DialogUtils;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FormationAdminController — Trainer view for managing their own courses.
 * Supports full CRUD on formations and their modules, with enrollment stats.
 */
public class FormationAdminController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FormationAdminController.class);

    private final FormationService formationService = FormationService.getInstance();
    private final FormationModuleService moduleService = FormationModuleService.getInstance();
    private final EnrollmentService enrollmentService = EnrollmentService.getInstance();

    private User currentUser;
    private List<Formation> myFormations = new ArrayList<>();
    private final Map<Integer, Long> enrollmentCounts = new HashMap<>();

    @FXML private Label statsLabel;
    @FXML private VBox formationsContainer;
    @FXML private VBox emptyState;
    @FXML private TLButton createBtn;
    @FXML private TLButton refreshBtn;
    @FXML private Label titleLabel;
    @FXML private Label emptyTitleLabel;
    @FXML private Label emptySubtitleLabel;

    public void setCurrentUser(User user) {
        this.currentUser = user;
        if (user == null || (user.getRole() != com.skilora.user.enums.Role.TRAINER
                && user.getRole() != com.skilora.user.enums.Role.ADMIN)) {
            if (formationsContainer != null && formationsContainer.getScene() != null) {
                TLToast.error(formationsContainer.getScene(), I18n.get("errorpage.access_denied"),
                        I18n.get("errorpage.access_denied.desc"));
            }
            return;
        }
        if (user != null) {
            loadFormations();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // I18n overrides for FXML labels
        if (titleLabel != null) {
            titleLabel.setText(I18n.get("formation.admin.title"));
        }
        if (statsLabel != null) {
            statsLabel.setText(I18n.get("common.loading"));
        }
        if (createBtn != null) {
            createBtn.setText(I18n.get("formation.admin.create"));
            createBtn.setOnAction(e -> showFormationDialog(null));
        }
        if (refreshBtn != null) {
            refreshBtn.setText(I18n.get("common.refresh"));
            refreshBtn.setOnAction(e -> handleRefresh());
        }
        if (emptyTitleLabel != null) {
            emptyTitleLabel.setText(I18n.get("formation.admin.empty"));
        }
        if (emptySubtitleLabel != null) {
            emptySubtitleLabel.setText(I18n.get("formation.admin.empty.desc"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Data Loading
    // ═══════════════════════════════════════════════════════════════

    private void loadFormations() {
        if (currentUser == null) return;

        Task<List<Formation>> task = new Task<>() {
            @Override
            protected List<Formation> call() throws Exception {
                if (currentUser.getRole() == com.skilora.user.enums.Role.ADMIN) {
                    return formationService.findAll();
                }
                return formationService.findByCreator(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            myFormations = task.getValue();
            loadEnrollmentCounts();
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to load trainer formations", task.getException());
            Platform.runLater(() -> {
                myFormations = new ArrayList<>();
                renderFormations();
                TLToast.error(formationsContainer.getScene(),
                        I18n.get("common.error"), I18n.get("formation.admin.load_error"));
            });
        });

        AppThreadPool.execute(task);
    }

    private void loadEnrollmentCounts() {
        Task<Map<Integer, Long>> task = new Task<>() {
            @Override
            protected Map<Integer, Long> call() throws Exception {
                Map<Integer, Long> counts = new HashMap<>();
                for (Formation f : myFormations) {
                    counts.put(f.getId(), enrollmentService.countByFormation(f.getId()));
                }
                return counts;
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            enrollmentCounts.clear();
            enrollmentCounts.putAll(task.getValue());
            renderFormations();
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to load enrollment counts", task.getException());
            Platform.runLater(this::renderFormations);
        });

        AppThreadPool.execute(task);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Rendering
    // ═══════════════════════════════════════════════════════════════

    private void renderFormations() {
        formationsContainer.getChildren().clear();

        if (myFormations.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("formation.admin.no_formations"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);

        long totalEnrollments = enrollmentCounts.values().stream().mapToLong(Long::longValue).sum();
        statsLabel.setText(I18n.get("formation.admin.stats", myFormations.size(), totalEnrollments));

        for (Formation f : myFormations) {
            formationsContainer.getChildren().add(buildFormationCard(f));
        }
    }

    private TLCard buildFormationCard(Formation formation) {
        TLCard card = new TLCard();
        card.setMaxWidth(Double.MAX_VALUE);

        VBox content = new VBox(10);

        // Row 1: badges
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        TLBadge catBadge = new TLBadge(
                formation.getCategory() != null ? formation.getCategory() : "-",
                TLBadge.Variant.DEFAULT);

        String levelKey = "formations.level."
                + (formation.getLevel() != null ? formation.getLevel().name().toLowerCase() : "beginner");
        TLBadge levelBadge = new TLBadge(I18n.get(levelKey), TLBadge.Variant.SECONDARY);
        if (formation.getLevel() == FormationLevel.BEGINNER) {
            levelBadge.getStyleClass().add("badge-success");
        } else if (formation.getLevel() == FormationLevel.ADVANCED) {
            levelBadge.getStyleClass().add("badge-destructive");
        }

        TLBadge statusBadge = buildStatusBadge(formation.getStatus());
        badgeRow.getChildren().addAll(catBadge, levelBadge, statusBadge);

        // Title
        Label titleLabel = new Label(formation.getTitle());
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);

        // Meta: enrollments + duration
        HBox metaRow = new HBox(16);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        long enrolled = enrollmentCounts.getOrDefault(formation.getId(), 0L);
        Label enrollLabel = new Label(I18n.get("formation.admin.enrolled", enrolled));
        enrollLabel.setGraphic(SvgIcons.icon(SvgIcons.USERS, 14, "-fx-muted-foreground"));
        enrollLabel.getStyleClass().add("text-muted");

        Label durationLabel = new Label(I18n.get("formations.duration", formation.getDurationHours()));
        durationLabel.setGraphic(SvgIcons.icon(SvgIcons.TIMER, 14, "-fx-muted-foreground"));
        durationLabel.getStyleClass().add("text-muted");

        metaRow.getChildren().addAll(enrollLabel, durationLabel);

        // Actions row
        HBox actionsRow = new HBox(8);
        actionsRow.setAlignment(Pos.CENTER_LEFT);

        TLButton editBtn = new TLButton(I18n.get("formation.admin.edit"), TLButton.ButtonVariant.OUTLINE, TLButton.ButtonSize.SM);
        editBtn.setGraphic(SvgIcons.icon(SvgIcons.TOOL, 14, "-fx-foreground"));
        editBtn.setOnAction(e -> showFormationDialog(formation));

        TLButton modulesBtn = new TLButton(I18n.get("formation.admin.modules"), TLButton.ButtonVariant.SECONDARY, TLButton.ButtonSize.SM);
        modulesBtn.setGraphic(SvgIcons.icon(SvgIcons.LAYERS, 14, "-fx-foreground"));
        modulesBtn.setOnAction(e -> showModulesDialog(formation));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        TLButton deleteBtn = new TLButton(I18n.get("formation.admin.delete"), TLButton.ButtonVariant.DANGER, TLButton.ButtonSize.SM);
        deleteBtn.setGraphic(SvgIcons.icon(SvgIcons.TRASH, 14, "-fx-destructive-foreground"));
        deleteBtn.setOnAction(e -> handleDelete(formation));

        actionsRow.getChildren().addAll(editBtn, modulesBtn, spacer, deleteBtn);

        content.getChildren().addAll(badgeRow, titleLabel, metaRow, new TLSeparator(), actionsRow);
        card.getContent().add(content);
        return card;
    }

    private TLBadge buildStatusBadge(String status) {
        if (status == null) status = "ACTIVE";
        TLBadge.Variant variant;
        switch (status.toUpperCase()) {
            case "ACTIVE" -> variant = TLBadge.Variant.DEFAULT;
            case "DRAFT" -> variant = TLBadge.Variant.SECONDARY;
            case "ARCHIVED" -> variant = TLBadge.Variant.OUTLINE;
            default -> variant = TLBadge.Variant.SECONDARY;
        }
        TLBadge badge = new TLBadge(status, variant);
        if ("ACTIVE".equalsIgnoreCase(status)) {
            badge.getStyleClass().add("badge-success");
        }
        return badge;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Create / Edit Formation Dialog
    // ═══════════════════════════════════════════════════════════════

    private void showFormationDialog(Formation existing) {
        boolean isEdit = existing != null;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(isEdit
                ? I18n.get("formation.admin.edit_title")
                : I18n.get("formation.admin.create_title"));
        dialog.setDialogTitle(isEdit
                ? I18n.get("formation.admin.edit_title")
                : I18n.get("formation.admin.create_title"));
        dialog.setDescription(isEdit
                ? I18n.get("formation.admin.edit_desc")
                : I18n.get("formation.admin.create_desc"));

        // Fields
        TLTextField titleField = new TLTextField(I18n.get("formation.admin.field.title"), "");
        TLTextarea descField = new TLTextarea(I18n.get("formation.admin.field.description"));
        TLTextField categoryField = new TLTextField(I18n.get("formation.admin.field.category"), "");
        TLTextField durationField = new TLTextField(I18n.get("formation.admin.field.duration"), "");
        TLTextField costField = new TLTextField(I18n.get("formation.admin.field.cost"), "");
        TLTextField currencyField = new TLTextField(I18n.get("formation.admin.field.currency"), "");
        TLTextField providerField = new TLTextField(I18n.get("formation.admin.field.provider"), "");
        TLTextField imageUrlField = new TLTextField(I18n.get("formation.admin.field.image_url"), "");

        TLSelect<String> levelSelect = new TLSelect<>(I18n.get("formation.admin.field.level"));
        for (FormationLevel lvl : FormationLevel.values()) {
            levelSelect.getItems().add(lvl.name());
        }

        TLSelect<String> statusSelect = new TLSelect<>(I18n.get("formation.admin.field.status"));
        statusSelect.getItems().addAll("ACTIVE", "DRAFT", "ARCHIVED");

        // Pre-fill for edit
        if (isEdit) {
            titleField.setText(existing.getTitle());
            descField.setText(existing.getDescription() != null ? existing.getDescription() : "");
            categoryField.setText(existing.getCategory() != null ? existing.getCategory() : "");
            durationField.setText(String.valueOf(existing.getDurationHours()));
            costField.setText(String.valueOf(existing.getCost()));
            currencyField.setText(existing.getCurrency() != null ? existing.getCurrency() : "TND");
            providerField.setText(existing.getProvider() != null ? existing.getProvider() : "");
            imageUrlField.setText(existing.getImageUrl() != null ? existing.getImageUrl() : "");
            levelSelect.setValue(existing.getLevel() != null ? existing.getLevel().name() : "BEGINNER");
            statusSelect.setValue(existing.getStatus() != null ? existing.getStatus() : "ACTIVE");
        } else {
            currencyField.setText("TND");
            levelSelect.setValue("BEGINNER");
            statusSelect.setValue("DRAFT");
        }

        VBox basicSection = dialog.createFormSection(
                I18n.get("formation.admin.section.basic"),
                titleField, descField, categoryField);

        VBox detailSection = dialog.createFormSection(
                I18n.get("formation.admin.section.details"),
                durationField, costField, currencyField, providerField, imageUrlField);

        VBox settingsSection = dialog.createFormSection(
                I18n.get("formation.admin.section.settings"),
                levelSelect, statusSelect);

        // ── Director Electronic Signature Section ──
        VBox signatureSection = buildSignatureSection(isEdit ? existing : null);

        // ── Two-step wizard layout ──
        VBox step1Content = new VBox(8, basicSection, detailSection);
        step1Content.setPadding(new Insets(4));

        VBox step2Content = new VBox(8, settingsSection, signatureSection);
        step2Content.setPadding(new Insets(4));
        step2Content.setVisible(false);
        step2Content.setManaged(false);

        VBox formContent = new VBox(8, step1Content, step2Content);

        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(formContent);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(420);
        scrollPane.getStyleClass().add("edge-to-edge");

        // Navigation buttons (inside dialog content)
        TLButton nextBtn = new TLButton(I18n.get("common.next"), TLButton.ButtonVariant.PRIMARY);
        TLButton backBtn = new TLButton(I18n.get("common.back"), TLButton.ButtonVariant.OUTLINE);
        backBtn.setVisible(false);
        backBtn.setManaged(false);

        HBox wizardNav = new HBox(8, backBtn, nextBtn);
        wizardNav.setAlignment(Pos.CENTER_RIGHT);
        wizardNav.setPadding(new Insets(8, 0, 0, 0));

        VBox wizardWrapper = new VBox(8, scrollPane, wizardNav);

        dialog.setContent(wizardWrapper);
        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.styleButtons();

        // Get the Cancel and OK buttons from the dialog pane
        Button cancelButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);

        // Initially on Step 1: show Cancel + Next, hide OK + Back
        okButton.setVisible(false);
        okButton.setManaged(false);

        nextBtn.setOnAction(e -> {
            // Validate Step 1 before proceeding
            String title = titleField.getText();
            if (title == null || title.isBlank()) {
                titleField.setError(I18n.get("formation.admin.validation.title_required"));
                return;
            }
            titleField.clearValidation();

            // Switch to Step 2
            step1Content.setVisible(false);
            step1Content.setManaged(false);
            step2Content.setVisible(true);
            step2Content.setManaged(true);
            nextBtn.setVisible(false);
            nextBtn.setManaged(false);
            backBtn.setVisible(true);
            backBtn.setManaged(true);

            // Hide Cancel, show OK (Create/Save)
            cancelButton.setVisible(false);
            cancelButton.setManaged(false);
            okButton.setVisible(true);
            okButton.setManaged(true);

            scrollPane.setVvalue(0);
        });

        backBtn.setOnAction(e -> {
            // Switch back to Step 1
            step2Content.setVisible(false);
            step2Content.setManaged(false);
            step1Content.setVisible(true);
            step1Content.setManaged(true);
            backBtn.setVisible(false);
            backBtn.setManaged(false);
            nextBtn.setVisible(true);
            nextBtn.setManaged(true);

            // Show Cancel, hide OK
            cancelButton.setVisible(true);
            cancelButton.setManaged(true);
            okButton.setVisible(false);
            okButton.setManaged(false);

            scrollPane.setVvalue(0);
        });

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String title = titleField.getText();
                if (title == null || title.isBlank()) {
                    titleField.setError(I18n.get("formation.admin.validation.title_required"));
                    event.consume();
                } else {
                    titleField.clearValidation();
                }
            }
        );

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                Formation formation = isEdit ? existing : new Formation();
                formation.setTitle(titleField.getText().trim());
                formation.setDescription(descField.getText());
                formation.setCategory(categoryField.getText());
                formation.setProvider(providerField.getText());
                formation.setImageUrl(imageUrlField.getText());
                formation.setCurrency(currencyField.getText());

                try {
                    formation.setDurationHours(Integer.parseInt(durationField.getText().trim()));
                } catch (NumberFormatException ex) {
                    formation.setDurationHours(0);
                }
                try {
                    formation.setCost(Double.parseDouble(costField.getText().trim()));
                } catch (NumberFormatException ex) {
                    formation.setCost(0);
                }
                formation.setFree(formation.getCost() <= 0);

                String selectedLevel = levelSelect.getValue();
                if (selectedLevel != null) {
                    try {
                        formation.setLevel(FormationLevel.valueOf(selectedLevel));
                    } catch (IllegalArgumentException ex) {
                        formation.setLevel(FormationLevel.BEGINNER);
                    }
                }
                formation.setStatus(statusSelect.getValue() != null ? statusSelect.getValue() : "DRAFT");

                if (!isEdit) {
                    formation.setCreatedBy(currentUser.getId());
                }

                // Capture electronic signature from canvas
                formation.setDirectorSignature(captureSignatureFromCanvas(signatureSection));

                saveFormation(formation, isEdit);
            }
            return bt;
        });

        dialog.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════
    //  Electronic Signature Canvas
    // ═══════════════════════════════════════════════════════════════

    /**
     * Build the director electronic signature section with a drawing canvas,
     * clear and save buttons, and instructions.
     */
    private VBox buildSignatureSection(Formation existing) {
        VBox section = new VBox(8);
        section.setPadding(new Insets(12, 0, 0, 0));

        // Section header
        Label sectionTitle = new Label(I18n.get("formation.admin.section.signature"));
        sectionTitle.getStyleClass().addAll("label", "h4");

        HBox headerRow = new HBox(8);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label signatureLabel = new Label(I18n.get("formation.admin.field.signature"));
        signatureLabel.getStyleClass().addAll("label", "text-sm");
        Label optionalLabel = new Label(I18n.get("formation.admin.field.signature.optional"));
        optionalLabel.getStyleClass().addAll("text-xs", "text-muted");
        headerRow.getChildren().addAll(signatureLabel, optionalLabel);

        // Signature canvas (420x140)
        Canvas signatureCanvas = new Canvas(420, 140);
        signatureCanvas.setId("signatureCanvas");
        signatureCanvas.setStyle("-fx-cursor: crosshair;");

        // Wrapper with border for the canvas
        VBox canvasWrapper = new VBox(signatureCanvas);
        canvasWrapper.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 1; " +
                "-fx-border-radius: 6; -fx-background-radius: 6; -fx-background-color: white; -fx-padding: 2;");
        canvasWrapper.setAlignment(Pos.CENTER);
        canvasWrapper.setMaxWidth(430);

        // Setup drawing on canvas
        GraphicsContext gc = signatureCanvas.getGraphicsContext2D();
        clearCanvas(gc, signatureCanvas);

        final double[] lastPos = new double[2]; // lastX, lastY

        signatureCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, e -> {
            lastPos[0] = e.getX();
            lastPos[1] = e.getY();
        });

        signatureCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, e -> {
            gc.setStroke(Color.BLACK);
            gc.setLineWidth(2.0);
            gc.strokeLine(lastPos[0], lastPos[1], e.getX(), e.getY());
            lastPos[0] = e.getX();
            lastPos[1] = e.getY();
        });

        // Action buttons
        TLButton clearBtn = new TLButton(I18n.get("formation.admin.signature.clear"),
                TLButton.ButtonVariant.OUTLINE, TLButton.ButtonSize.SM);
        clearBtn.setOnAction(e -> clearCanvas(gc, signatureCanvas));

        TLButton saveBtn = new TLButton(I18n.get("formation.admin.signature.save"),
                TLButton.ButtonVariant.PRIMARY, TLButton.ButtonSize.SM);
        saveBtn.setOnAction(e -> {
            String base64 = captureCanvasToBase64(signatureCanvas);
            if (base64 != null) {
                DialogUtils.showInfo(
                        I18n.get("formation.admin.signature.saved_title"),
                        I18n.get("formation.admin.signature.saved_msg"));
            }
        });

        HBox btnRow = new HBox(8);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.getChildren().addAll(clearBtn, saveBtn);

        // Instruction text
        Label instructionLabel = new Label(I18n.get("formation.admin.signature.instruction"));
        instructionLabel.getStyleClass().addAll("text-xs", "text-muted");
        instructionLabel.setWrapText(true);

        section.getChildren().addAll(sectionTitle, new TLSeparator(), headerRow,
                canvasWrapper, btnRow, instructionLabel);

        // Load existing signature if editing
        if (existing != null && existing.getDirectorSignature() != null
                && !existing.getDirectorSignature().isBlank()) {
            loadSignatureToCanvas(gc, signatureCanvas, existing.getDirectorSignature());
        }

        return section;
    }

    /**
     * Clear the signature canvas to white.
     */
    private void clearCanvas(GraphicsContext gc, Canvas canvas) {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
    }

    /**
     * Capture the canvas content as a base64-encoded PNG string.
     * Returns null if the canvas is blank (all white).
     */
    private String captureCanvasToBase64(Canvas canvas) {
        try {
            WritableImage snapshot = canvas.snapshot(null, null);
            java.awt.image.BufferedImage img = SwingFXUtils.fromFXImage(snapshot, null);
            if (img == null) return null;

            // Sample pixels to check if canvas has actual content (not all white)
            boolean hasContent = false;
            int sampleStep = Math.max(1, Math.min(img.getWidth(), img.getHeight()) / 20);
            for (int y = 0; y < img.getHeight() && !hasContent; y += sampleStep) {
                for (int x = 0; x < img.getWidth(); x += sampleStep) {
                    int rgb = img.getRGB(x, y);
                    if ((rgb & 0xFFFFFF) != 0xFFFFFF) {
                        hasContent = true;
                        break;
                    }
                }
            }

            if (!hasContent) return null;

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            baos.flush();
            byte[] pngBytes = baos.toByteArray();
            baos.close();

            return java.util.Base64.getEncoder().encodeToString(pngBytes);
        } catch (Exception ex) {
            logger.error("Error capturing signature from canvas: {}", ex.getMessage(), ex);
            return null;
        }
    }

    /**
     * Capture signature from the canvas embedded in the signature section VBox.
     * Returns the base64-encoded PNG, or null if canvas is blank.
     */
    private String captureSignatureFromCanvas(VBox signatureSection) {
        // Find the Canvas inside the section hierarchy
        Canvas canvas = findCanvas(signatureSection);
        if (canvas == null) return null;
        return captureCanvasToBase64(canvas);
    }

    /**
     * Recursively find a Canvas node inside a parent node.
     */
    private Canvas findCanvas(javafx.scene.Parent parent) {
        for (javafx.scene.Node child : parent.getChildrenUnmodifiable()) {
            if (child instanceof Canvas c) return c;
            if (child instanceof javafx.scene.Parent p) {
                Canvas found = findCanvas(p);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Load a base64-encoded signature image onto the canvas.
     */
    private void loadSignatureToCanvas(GraphicsContext gc, Canvas canvas, String base64Signature) {
        if (base64Signature == null || base64Signature.isBlank()) return;
        try {
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Signature);
            Image image = new Image(new ByteArrayInputStream(imageBytes));
            clearCanvas(gc, canvas);
            gc.drawImage(image, 0, 0, canvas.getWidth(), canvas.getHeight());
        } catch (Exception ex) {
            logger.error("Error loading signature to canvas: {}", ex.getMessage(), ex);
        }
    }

    private void saveFormation(Formation formation, boolean isEdit) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if (isEdit) {
                    return formationService.updateFormation(formation);
                } else {
                    return formationService.createFormation(formation) > 0;
                }
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            boolean ok = task.getValue();
            if (ok) {
                TLToast.success(formationsContainer.getScene(),
                        I18n.get("common.success"),
                        isEdit ? I18n.get("formation.admin.updated") : I18n.get("formation.admin.created"));
                loadFormations();
            } else {
                TLToast.error(formationsContainer.getScene(),
                        I18n.get("common.error"), I18n.get("formation.admin.save_error"));
            }
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to save formation", task.getException());
            Platform.runLater(() -> TLToast.error(formationsContainer.getScene(),
                    I18n.get("common.error"), I18n.get("formation.admin.save_error")));
        });

        AppThreadPool.execute(task);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Delete Formation
    // ═══════════════════════════════════════════════════════════════

    private void handleDelete(Formation formation) {
        Optional<ButtonType> result = DialogUtils.showConfirmation(
                I18n.get("formation.admin.delete_title"),
                I18n.get("formation.admin.delete_confirm", formation.getTitle()));

        if (result.isPresent() && result.get() == ButtonType.OK) {
            formationsContainer.setDisable(true);
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return formationService.deleteFormation(formation.getId());
                }
            };

            task.setOnSucceeded(e -> Platform.runLater(() -> {
                formationsContainer.setDisable(false);
                if (task.getValue()) {
                    TLToast.success(formationsContainer.getScene(),
                            I18n.get("common.success"), I18n.get("formation.admin.deleted"));
                    loadFormations();
                } else {
                    TLToast.error(formationsContainer.getScene(),
                            I18n.get("common.error"), I18n.get("formation.admin.delete_error"));
                }
            }));

            task.setOnFailed(e -> {
                logger.error("Failed to delete formation", task.getException());
                Platform.runLater(() -> {
                    formationsContainer.setDisable(false);
                    TLToast.error(formationsContainer.getScene(),
                        I18n.get("common.error"), I18n.get("formation.admin.delete_error"));
                });
            });

            AppThreadPool.execute(task);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Manage Modules Dialog
    // ═══════════════════════════════════════════════════════════════

    private void showModulesDialog(Formation formation) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle(I18n.get("formation.admin.modules_title"));
        dialog.setDialogTitle(I18n.get("formation.admin.modules_title"));
        dialog.setDescription(I18n.get("formation.admin.modules_desc", formation.getTitle()));

        VBox modulesBox = new VBox(10);
        modulesBox.setPadding(new Insets(4));

        Label loadingLabel = new Label(I18n.get("common.loading"));
        loadingLabel.getStyleClass().add("text-muted");
        modulesBox.getChildren().add(loadingLabel);

        TLButton addModuleBtn = new TLButton(I18n.get("formation.admin.module.add"), TLButton.ButtonVariant.PRIMARY, TLButton.ButtonSize.SM);
        addModuleBtn.setGraphic(SvgIcons.icon(SvgIcons.PLUS, 14, "-fx-primary-foreground"));
        addModuleBtn.setOnAction(e -> showModuleEditor(formation, null, modulesBox));

        VBox wrapper = new VBox(12, addModuleBtn, new TLSeparator(), modulesBox);
        ScrollPane scrollPane = new ScrollPane(wrapper);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(400);
        scrollPane.getStyleClass().add("edge-to-edge");

        dialog.setContent(scrollPane);
        dialog.addButton(ButtonType.CLOSE);
        dialog.styleButtons();

        loadModulesInto(formation, modulesBox);
        dialog.showAndWait();
    }

    private void loadModulesInto(Formation formation, VBox modulesBox) {
        Task<List<FormationModule>> task = new Task<>() {
            @Override
            protected List<FormationModule> call() throws Exception {
                return moduleService.findByFormation(formation.getId());
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            modulesBox.getChildren().clear();
            List<FormationModule> modules = task.getValue();

            if (modules.isEmpty()) {
                Label emptyLabel = new Label(I18n.get("formation.admin.module.empty"));
                emptyLabel.getStyleClass().add("text-muted");
                modulesBox.getChildren().add(emptyLabel);
                return;
            }

            for (int i = 0; i < modules.size(); i++) {
                modulesBox.getChildren().add(buildModuleRow(formation, modules.get(i), i, modules.size(), modulesBox));
            }
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to load modules", task.getException());
            Platform.runLater(() -> {
                modulesBox.getChildren().clear();
                Label errorLabel = new Label(I18n.get("formation.admin.module.load_error"));
                errorLabel.getStyleClass().add("text-destructive");
                modulesBox.getChildren().add(errorLabel);
            });
        });

        AppThreadPool.execute(task);
    }

    private TLCard buildModuleRow(Formation formation, FormationModule module, int index, int total, VBox modulesBox) {
        TLCard card = new TLCard();
        card.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label indexLabel = new Label(String.valueOf(index + 1));
        indexLabel.getStyleClass().add("h4");
        indexLabel.setMinWidth(28);
        indexLabel.setAlignment(Pos.CENTER);

        VBox infoBox = new VBox(2);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        Label modTitle = new Label(module.getTitle());
        modTitle.getStyleClass().add("label");
        modTitle.setWrapText(true);

        HBox modMeta = new HBox(8);
        modMeta.setAlignment(Pos.CENTER_LEFT);
        if (module.getDurationMinutes() > 0) {
            Label durLabel = new Label(module.getDurationMinutes() + " min");
            durLabel.getStyleClass().add("text-muted");
            modMeta.getChildren().add(durLabel);
        }
        if (module.getContentUrl() != null && !module.getContentUrl().isBlank()) {
            Label urlLabel = new Label(I18n.get("formation.admin.module.has_content"));
            urlLabel.getStyleClass().add("text-muted");
            modMeta.getChildren().add(urlLabel);
        }
        infoBox.getChildren().add(modTitle);
        if (!modMeta.getChildren().isEmpty()) {
            infoBox.getChildren().add(modMeta);
        }

        // Reorder buttons
        VBox reorderBox = new VBox(2);
        reorderBox.setAlignment(Pos.CENTER);

        TLButton moveUpBtn = new TLButton("", TLButton.ButtonVariant.GHOST, TLButton.ButtonSize.SM);
        moveUpBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 12, "-fx-muted-foreground"));
        moveUpBtn.setTooltip(new Tooltip("Move up"));
        moveUpBtn.setRotate(90);
        moveUpBtn.setDisable(index == 0);
        moveUpBtn.setOnAction(e -> reorderModule(formation, modulesBox, index, index - 1));

        TLButton moveDownBtn = new TLButton("", TLButton.ButtonVariant.GHOST, TLButton.ButtonSize.SM);
        moveDownBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_RIGHT, 12, "-fx-muted-foreground"));
        moveDownBtn.setTooltip(new Tooltip("Move down"));
        moveDownBtn.setRotate(90);
        moveDownBtn.setDisable(index >= total - 1);
        moveDownBtn.setOnAction(e -> reorderModule(formation, modulesBox, index, index + 1));

        reorderBox.getChildren().addAll(moveUpBtn, moveDownBtn);

        // Edit / Delete
        HBox btnBox = new HBox(4);
        btnBox.setAlignment(Pos.CENTER);

        TLButton editBtn = new TLButton("", TLButton.ButtonVariant.GHOST, TLButton.ButtonSize.SM);
        editBtn.setGraphic(SvgIcons.icon(SvgIcons.TOOL, 14, "-fx-foreground"));
        editBtn.setTooltip(new Tooltip("Edit"));
        editBtn.setOnAction(e -> showModuleEditor(formation, module, modulesBox));

        TLButton delBtn = new TLButton("", TLButton.ButtonVariant.GHOST, TLButton.ButtonSize.SM);
        delBtn.setGraphic(SvgIcons.icon(SvgIcons.TRASH, 14, "-fx-destructive"));
        delBtn.setTooltip(new Tooltip("Delete"));
        delBtn.setOnAction(e -> handleDeleteModule(formation, module, modulesBox));

        btnBox.getChildren().addAll(editBtn, delBtn);

        row.getChildren().addAll(indexLabel, infoBox, reorderBox, btnBox);
        card.getContent().add(row);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════
    //  Module Editor (Add / Edit)
    // ═══════════════════════════════════════════════════════════════

    private void showModuleEditor(Formation formation, FormationModule existing, VBox modulesBox) {
        boolean isEdit = existing != null;

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setDialogTitle(isEdit
                ? I18n.get("formation.admin.module.edit_title")
                : I18n.get("formation.admin.module.add_title"));

        TLTextField titleField = new TLTextField(I18n.get("formation.admin.module.field.title"), "");
        TLTextarea descField = new TLTextarea(I18n.get("formation.admin.module.field.description"));
        TLTextarea contentField = new TLTextarea(I18n.get("formation.admin.module.field.content"));
        contentField.setPrefRowCount(6);
        TLTextField contentUrlField = new TLTextField(I18n.get("formation.admin.module.field.content_url"), "");
        TLTextField durationField = new TLTextField(I18n.get("formation.admin.module.field.duration"), "");

        if (isEdit) {
            titleField.setText(existing.getTitle());
            descField.setText(existing.getDescription() != null ? existing.getDescription() : "");
            contentField.setText(existing.getContent() != null ? existing.getContent() : "");
            contentUrlField.setText(existing.getContentUrl() != null ? existing.getContentUrl() : "");
            durationField.setText(String.valueOf(existing.getDurationMinutes()));
        }

        // Section labels
        Label resourcesLabel = new Label(I18n.get("formation.admin.module.section.resources"));
        resourcesLabel.getStyleClass().add("h4");

        VBox form = new VBox(14, titleField, descField,
                new TLSeparator(), resourcesLabel, contentField, contentUrlField,
                new TLSeparator(), durationField);
        form.setPadding(new Insets(4));
        dialog.setContent(form);

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);
        dialog.styleButtons();

        dialog.getDialogPane().lookupButton(ButtonType.OK).addEventFilter(
            javafx.event.ActionEvent.ACTION, event -> {
                String title = titleField.getText();
                if (title == null || title.isBlank()) {
                    titleField.setError(I18n.get("formation.admin.module.validation.title_required"));
                    event.consume();
                } else {
                    titleField.clearValidation();
                }
            }
        );

        dialog.setResultConverter(bt -> {
            if (bt == ButtonType.OK) {
                FormationModule mod = isEdit ? existing : new FormationModule();
                mod.setFormationId(formation.getId());
                mod.setTitle(titleField.getText().trim());
                mod.setDescription(descField.getText());
                mod.setContent(contentField.getText());
                mod.setContentUrl(contentUrlField.getText());

                try {
                    mod.setDurationMinutes(Integer.parseInt(durationField.getText().trim()));
                } catch (NumberFormatException ex) {
                    mod.setDurationMinutes(0);
                }

                if (!isEdit) {
                    mod.setOrderIndex(modulesBox.getChildren().size());
                }

                saveModule(mod, isEdit, formation, modulesBox);
            }
            return bt;
        });

        dialog.showAndWait();
    }

    private void saveModule(FormationModule module, boolean isEdit, Formation formation, VBox modulesBox) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                if (isEdit) {
                    return moduleService.update(module);
                } else {
                    return moduleService.create(module) > 0;
                }
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (task.getValue()) {
                TLToast.success(formationsContainer.getScene(),
                        I18n.get("common.success"),
                        isEdit ? I18n.get("formation.admin.module.updated") : I18n.get("formation.admin.module.created"));
                loadModulesInto(formation, modulesBox);
            } else {
                TLToast.error(formationsContainer.getScene(),
                        I18n.get("common.error"), I18n.get("formation.admin.module.save_error"));
            }
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to save module", task.getException());
            Platform.runLater(() -> TLToast.error(formationsContainer.getScene(),
                    I18n.get("common.error"), I18n.get("formation.admin.module.save_error")));
        });

        AppThreadPool.execute(task);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Delete Module
    // ═══════════════════════════════════════════════════════════════

    private void handleDeleteModule(Formation formation, FormationModule module, VBox modulesBox) {
        Optional<ButtonType> result = DialogUtils.showConfirmation(
                I18n.get("formation.admin.module.delete_title"),
                I18n.get("formation.admin.module.delete_confirm", module.getTitle()));

        if (result.isPresent() && result.get() == ButtonType.OK) {
            modulesBox.setDisable(true);
            Task<Boolean> task = new Task<>() {
                @Override
                protected Boolean call() throws Exception {
                    return moduleService.delete(module.getId());
                }
            };

            task.setOnSucceeded(e -> Platform.runLater(() -> {
                modulesBox.setDisable(false);
                if (task.getValue()) {
                    TLToast.success(formationsContainer.getScene(),
                            I18n.get("common.success"), I18n.get("formation.admin.module.deleted"));
                    loadModulesInto(formation, modulesBox);
                } else {
                    TLToast.error(formationsContainer.getScene(),
                            I18n.get("common.error"), I18n.get("formation.admin.module.delete_error"));
                }
            }));

            task.setOnFailed(e -> {
                logger.error("Failed to delete module", task.getException());
                Platform.runLater(() -> {
                    modulesBox.setDisable(false);
                    TLToast.error(formationsContainer.getScene(),
                        I18n.get("common.error"), I18n.get("formation.admin.module.delete_error"));
                });
            });

            AppThreadPool.execute(task);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Reorder Modules
    // ═══════════════════════════════════════════════════════════════

    private void reorderModule(Formation formation, VBox modulesBox, int fromIndex, int toIndex) {
        Task<Boolean> task = new Task<>() {
            @Override
            protected Boolean call() throws Exception {
                List<FormationModule> modules = moduleService.findByFormation(formation.getId());
                if (fromIndex < 0 || fromIndex >= modules.size() || toIndex < 0 || toIndex >= modules.size()) {
                    return false;
                }
                List<Integer> ids = modules.stream()
                        .map(FormationModule::getId)
                        .collect(Collectors.toList());

                int moved = ids.remove(fromIndex);
                ids.add(toIndex, moved);
                return moduleService.reorder(formation.getId(), ids);
            }
        };

        task.setOnSucceeded(e -> Platform.runLater(() -> {
            if (task.getValue()) {
                loadModulesInto(formation, modulesBox);
            } else {
                TLToast.error(formationsContainer.getScene(),
                        I18n.get("common.error"), I18n.get("formation.admin.module.reorder_error"));
            }
        }));

        task.setOnFailed(e -> {
            logger.error("Failed to reorder modules", task.getException());
            Platform.runLater(() -> TLToast.error(formationsContainer.getScene(),
                    I18n.get("common.error"), I18n.get("formation.admin.module.reorder_error")));
        });

        AppThreadPool.execute(task);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Refresh
    // ═══════════════════════════════════════════════════════════════

    @FXML
    private void handleCreate() {
        showFormationDialog(null);
    }

    @FXML
    private void handleRefresh() {
        loadFormations();
    }
}
