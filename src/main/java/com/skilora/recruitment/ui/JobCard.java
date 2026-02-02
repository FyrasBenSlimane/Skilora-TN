package com.skilora.recruitment.ui;

import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLButton.ButtonVariant;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.Cursor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Desktop;
import java.net.URI;
import java.util.function.Consumer;

public class JobCard extends VBox {

    private static final Logger logger = LoggerFactory.getLogger(JobCard.class);

    private final JobOpportunity job;

    public JobCard(JobOpportunity job) {
        this(job, null);
    }

    public JobCard(JobOpportunity job, Consumer<JobOpportunity> onCardClick) {
        this.job = job;
        getStyleClass().add("job-card");
        setPadding(new Insets(20));
        setSpacing(16);
        setPrefWidth(300);
        setMinWidth(300);
        setMaxWidth(300);
        
        // Make card clickable - use CSS :hover instead of inline setStyle()
        if (onCardClick != null) {
            getStyleClass().add("job-card-clickable");
            setCursor(Cursor.HAND);
            setOnMouseClicked(e -> {
                if (e.getClickCount() == 1) {
                    onCardClick.accept(job);
                }
            });
        }

        // Header
        VBox header = new VBox(4);
        Label title = new Label(job.getTitle());
        title.getStyleClass().add("job-card-title");
        title.setWrapText(true);

        Label source = new Label(job.getSource());
        source.getStyleClass().add("job-card-source");

        header.getChildren().addAll(title, source);

        // Tags / Metadata
        HBox meta = new HBox(12);
        meta.setAlignment(Pos.CENTER_LEFT);

        // Location Badge
        Label locLabel = createBadge(job.getLocation().isEmpty() ? "Remote" : job.getLocation());
        Label dateLabel = createBadge(job.getPostedDate());

        meta.getChildren().addAll(locLabel, dateLabel);

        // Description (Truncated)
        String descText = job.getDescription().length() > 80
                ? job.getDescription().substring(0, 80).replace("\n", " ") + "..."
                : job.getDescription().replace("\n", " ");
        Label desc = new Label(descText);
        desc.setWrapText(true);
        desc.getStyleClass().add("job-card-desc");
        desc.setMinHeight(40);

        // Footer (View Details / Apply Button)
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        HBox buttonRow = new HBox(8);
        buttonRow.setAlignment(Pos.CENTER);
        
        if (onCardClick != null) {
            TLButton viewBtn = new TLButton("View Details", ButtonVariant.OUTLINE);
            viewBtn.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(viewBtn, Priority.ALWAYS);
            viewBtn.setOnAction(e -> {
                e.consume(); // Prevent card click
                onCardClick.accept(job);
            });
            buttonRow.getChildren().add(viewBtn);
        }

        TLButton applyBtn = new TLButton("Apply", ButtonVariant.PRIMARY);
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(applyBtn, Priority.ALWAYS);
        applyBtn.setOnAction(e -> {
            e.consume(); // Prevent card click
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(job.getUrl()));
                }
            } catch (Exception ex) {
                logger.error("Failed to open job URL: " + job.getUrl(), ex);
            }
        });
        buttonRow.getChildren().add(applyBtn);

        getChildren().addAll(header, meta, desc, spacer, buttonRow);
    }

    private Label createBadge(String text) {
        Label badge = new Label(text);
        badge.getStyleClass().add("job-card-badge");
        return badge;
    }

    public JobOpportunity getJob() {
        return job;
    }
}
