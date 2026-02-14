package com.skilora.controller;

import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLTextField;

import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Separator;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import com.skilora.utils.I18n;

/**
 * FormationsController - Training/Courses view for job seekers.
 * Displays available formations, certifications, and learning resources.
 */
public class FormationsController implements Initializable {

    private static final Logger logger = LoggerFactory.getLogger(FormationsController.class);

    @FXML private Label statsLabel;
    @FXML private HBox categoryBox;
    @FXML private TLTextField searchField;
    @FXML private FlowPane formationsGrid;
    @FXML private VBox emptyState;
    @FXML private TLButton refreshBtn;

    private List<Formation> allFormations;
    private ToggleGroup categoryGroup;
    private String currentCategory = "ALL";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCategories();
        setupSearch();
        loadFormations();
    }

    private void setupCategories() {
        categoryGroup = new ToggleGroup();
        String[][] categories = {
            {I18n.get("formations.filter.all"), "ALL"},
            {I18n.get("formations.filter.development"), "Développement"},
            {I18n.get("formations.filter.design"), "Design"},
            {I18n.get("formations.filter.marketing"), "Marketing"},
            {I18n.get("formations.filter.data_science"), "Data Science"},
            {I18n.get("formations.filter.languages"), "Langues"},
            {I18n.get("formations.filter.soft_skills"), "Soft Skills"}
        };

        for (String[] cat : categories) {
            ToggleButton btn = new ToggleButton(cat[0]);
            btn.setUserData(cat[1]);
            btn.getStyleClass().add("chip-filter");
            btn.setToggleGroup(categoryGroup);
            if ("ALL".equals(cat[1])) btn.setSelected(true);

            final String catKey = cat[1];
            btn.setOnAction(e -> {
                if (btn.isSelected()) {
                    currentCategory = catKey;
                    applyFilters();
                } else if (categoryGroup.getSelectedToggle() == null) {
                    btn.setSelected(true);
                }
            });
            categoryBox.getChildren().add(btn);
        }
    }

    private void setupSearch() {
        if (searchField != null && searchField.getControl() != null) {
            PauseTransition pause = new PauseTransition(Duration.millis(300));
            pause.setOnFinished(e -> applyFilters());
            searchField.getControl().setOnKeyReleased(e -> pause.playFromStart());
        }
    }

    private void loadFormations() {
        allFormations = getSampleFormations();
        applyFilters();
    }

    private void applyFilters() {
        if (allFormations == null) return;

        String query = searchField != null ? searchField.getText() : "";
        String lowerQuery = query == null ? "" : query.toLowerCase();

        List<Formation> filtered = allFormations.stream()
            .filter(f -> {
                if (!"ALL".equals(currentCategory) && !f.category.equals(currentCategory)) {
                    return false;
                }
                if (!lowerQuery.isEmpty()) {
                    return f.title.toLowerCase().contains(lowerQuery) ||
                           f.provider.toLowerCase().contains(lowerQuery) ||
                           f.description.toLowerCase().contains(lowerQuery);
                }
                return true;
            })
            .collect(Collectors.toList());

        renderFormations(filtered);
    }

    private void renderFormations(List<Formation> formations) {
        formationsGrid.getChildren().clear();

        if (formations.isEmpty()) {
            emptyState.setVisible(true);
            emptyState.setManaged(true);
            statsLabel.setText(I18n.get("formations.not_found"));
            return;
        }

        emptyState.setVisible(false);
        emptyState.setManaged(false);
        statsLabel.setText(I18n.get("formations.count", formations.size()));

        for (Formation f : formations) {
            formationsGrid.getChildren().add(createFormationCard(f));
        }
    }

    private TLCard createFormationCard(Formation formation) {
        TLCard card = new TLCard();
        card.setPrefWidth(320);
        card.setMinWidth(280);
        card.setMaxWidth(360);

        VBox content = new VBox(10);
        content.setPadding(new Insets(16));

        // Category badge + Level
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        TLBadge catBadge = new TLBadge(formation.category, TLBadge.Variant.DEFAULT);
        TLBadge levelBadge = new TLBadge(formation.level, TLBadge.Variant.SECONDARY);
        if ("Débutant".equals(formation.level) || I18n.get("formations.level.beginner").equals(formation.level)) {
            levelBadge.getStyleClass().add("badge-success");
        } else if ("Avancé".equals(formation.level) || I18n.get("formations.level.advanced").equals(formation.level)) {
            levelBadge.getStyleClass().add("badge-destructive");
        }
        badgeRow.getChildren().addAll(catBadge, levelBadge);

        // Title
        Label titleLabel = new Label(formation.title);
        titleLabel.getStyleClass().add("h4");
        titleLabel.setWrapText(true);

        // Description
        Label descLabel = new Label(formation.description);
        descLabel.getStyleClass().add("text-muted");
        descLabel.setWrapText(true);
        descLabel.setMaxHeight(60);

        // Provider + Duration
        HBox metaRow = new HBox(12);
        metaRow.setAlignment(Pos.CENTER_LEFT);
        Label providerLabel = new Label("🏫 " + formation.provider);
        providerLabel.getStyleClass().add("text-muted");
        Label durationLabel = new Label("⏱ " + formation.duration);
        durationLabel.getStyleClass().add("text-muted");
        metaRow.getChildren().addAll(providerLabel, durationLabel);

        // Progress bar (if enrolled)
        if (formation.progress > 0) {
            VBox progressBox = new VBox(4);
            Label progLabel = new Label(I18n.get("formations.completion", String.format("%.0f", formation.progress * 100)));
            progLabel.getStyleClass().add("text-muted");
            ProgressBar progressBar = new ProgressBar(formation.progress);
            progressBar.setMaxWidth(Double.MAX_VALUE);
            progressBar.setPrefHeight(6);
            progressBox.getChildren().addAll(progLabel, progressBar);
            content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow, new Separator(), progressBox);
        } else {
            // Enroll button
            Separator sep = new Separator();
            TLButton enrollBtn = new TLButton(formation.isFree ? I18n.get("formations.enroll.free") : I18n.get("formations.enroll.paid", formation.price), TLButton.ButtonVariant.PRIMARY);
            enrollBtn.setMaxWidth(Double.MAX_VALUE);
            enrollBtn.setOnAction(e -> {
                formation.progress = 0.01;
                applyFilters();
                logger.info("Enrolled in formation: {}", formation.title);
            });
            content.getChildren().addAll(badgeRow, titleLabel, descLabel, metaRow, sep, enrollBtn);
        }

        card.getContent().add(content);
        return card;
    }

    @FXML
    private void handleRefresh() {
        loadFormations();
    }

    /**
     * Sample formations data.
     * In production, these would come from a database or API.
     */
    private List<Formation> getSampleFormations() {
        List<Formation> formations = new ArrayList<>();

        formations.add(new Formation("Java Spring Boot Masterclass", "Développement",
            "Apprenez à construire des applications enterprise avec Spring Boot, Spring Security et Spring Data.",
            "Skilora Academy", "40 heures", "Intermédiaire", true, "", 0));

        formations.add(new Formation("React & TypeScript Avancé", "Développement",
            "Maîtrisez React avec TypeScript, les hooks avancés, et les patterns de state management.",
            "OpenClassrooms", "30 heures", "Avancé", false, "49 TND", 0));

        formations.add(new Formation("UX/UI Design avec Figma", "Design",
            "Créez des interfaces utilisateur modernes et accessibles avec les meilleures pratiques UX.",
            "Skilora Academy", "25 heures", "Débutant", true, "", 0));

        formations.add(new Formation("Marketing Digital", "Marketing",
            "Stratégies de marketing digital, SEO, réseaux sociaux et campagnes publicitaires.",
            "Google Ateliers Numériques", "20 heures", "Débutant", true, "", 0));

        formations.add(new Formation("Python pour la Data Science", "Data Science",
            "Analyse de données, visualisation et machine learning avec Python, Pandas et Scikit-learn.",
            "Coursera", "50 heures", "Intermédiaire", false, "79 TND", 0));

        formations.add(new Formation("Français Professionnel B2", "Langues",
            "Perfectionnez votre français professionnel pour le monde des affaires et les entretiens.",
            "Alliance Française", "60 heures", "Intermédiaire", false, "120 TND", 0));

        formations.add(new Formation("DevOps & CI/CD Pipeline", "Développement",
            "Docker, Kubernetes, Jenkins et automatisation du déploiement continu.",
            "Udemy", "35 heures", "Avancé", false, "39 TND", 0));

        formations.add(new Formation("Leadership & Gestion d'Équipe", "Soft Skills",
            "Développez vos compétences en leadership, communication et gestion de conflits.",
            "Skilora Academy", "15 heures", "Débutant", true, "", 0));

        formations.add(new Formation("Angular 17 Complet", "Développement",
            "De zéro à héro avec Angular 17, RxJS, NgRx et les dernières fonctionnalités.",
            "Pluralsight", "45 heures", "Intermédiaire", false, "59 TND", 0));

        formations.add(new Formation("Communication & Prise de Parole", "Soft Skills",
            "Techniques de présentation, storytelling et communication efficace en entreprise.",
            "Skilora Academy", "10 heures", "Débutant", true, "", 0));

        formations.add(new Formation("SQL & Bases de Données", "Data Science",
            "Maîtrisez SQL, la modélisation de données et l'optimisation de requêtes.",
            "Khan Academy", "20 heures", "Débutant", true, "", 0));

        formations.add(new Formation("Anglais des Affaires C1", "Langues",
            "Business English avancé: négociation, rédaction professionnelle et présentations.",
            "British Council", "80 heures", "Avancé", false, "150 TND", 0));

        return formations;
    }

    /**
     * Formation data model.
     * In production, this would be a proper entity class.
     */
    static class Formation {
        String title;
        String category;
        String description;
        String provider;
        String duration;
        String level;
        boolean isFree;
        String price;
        double progress;

        Formation(String title, String category, String description, String provider,
                  String duration, String level, boolean isFree, String price, double progress) {
            this.title = title;
            this.category = category;
            this.description = description;
            this.provider = provider;
            this.duration = duration;
            this.level = level;
            this.isFree = isFree;
            this.price = price;
            this.progress = progress;
        }
    }
}
