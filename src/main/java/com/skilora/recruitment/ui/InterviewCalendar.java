package com.skilora.recruitment.ui;

import com.skilora.framework.components.TLBadge;
import com.skilora.framework.components.TLButton;
import com.skilora.framework.components.TLCard;
import com.skilora.recruitment.entity.Interview;
import com.skilora.utils.I18n;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.*;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Reusable calendar component that displays interviews on a monthly grid.
 * Not FXML-based; instantiate and add to any container.
 */
public class InterviewCalendar extends VBox {

    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    private LocalDate currentMonth;
    private List<Interview> interviews = new ArrayList<>();
    private final GridPane calendarGrid = new GridPane();
    private final Label monthLabel = new Label();
    private final VBox dayDetailBox = new VBox(8);

    public InterviewCalendar() {
        this.currentMonth = LocalDate.now().withDayOfMonth(1);
        setSpacing(16);
        setPadding(new Insets(16));
        buildHeader();
        buildCalendar();
        getChildren().addAll(calendarGrid, dayDetailBox);
    }

    public void setInterviews(List<Interview> interviews) {
        this.interviews = interviews != null ? interviews : new ArrayList<>();
        renderCalendar();
    }

    public void setMonth(LocalDate month) {
        this.currentMonth = month.withDayOfMonth(1);
        renderCalendar();
    }

    private void buildHeader() {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER);

        TLButton prevBtn = new TLButton("<", TLButton.ButtonVariant.GHOST);
        prevBtn.setOnAction(e -> {
            currentMonth = currentMonth.minusMonths(1);
            renderCalendar();
        });

        TLButton nextBtn = new TLButton(">", TLButton.ButtonVariant.GHOST);
        nextBtn.setOnAction(e -> {
            currentMonth = currentMonth.plusMonths(1);
            renderCalendar();
        });

        monthLabel.getStyleClass().add("h3");

        TLButton todayBtn = new TLButton(I18n.get("calendar.today"), TLButton.ButtonVariant.OUTLINE);
        todayBtn.setOnAction(e -> {
            currentMonth = LocalDate.now().withDayOfMonth(1);
            renderCalendar();
        });

        Region spacer1 = new Region();
        Region spacer2 = new Region();
        HBox.setHgrow(spacer1, Priority.ALWAYS);
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        header.getChildren().addAll(prevBtn, spacer1, monthLabel, spacer2, todayBtn, nextBtn);
        getChildren().add(0, header);
    }

    private void buildCalendar() {
        calendarGrid.setHgap(2);
        calendarGrid.setVgap(2);
        calendarGrid.getStyleClass().add("calendar-grid");

        for (int col = 0; col < 7; col++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(cc);
        }
    }

    private void renderCalendar() {
        calendarGrid.getChildren().clear();
        dayDetailBox.getChildren().clear();
        monthLabel.setText(currentMonth.format(MONTH_FMT));

        Map<LocalDate, List<Interview>> byDate = interviews.stream()
            .filter(iv -> iv.getScheduledDate() != null)
            .collect(Collectors.groupingBy(iv -> iv.getScheduledDate().toLocalDate()));

        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        for (int i = 0; i < 7; i++) {
            Label dayHeader = new Label(days[i].getDisplayName(TextStyle.SHORT, Locale.getDefault()));
            dayHeader.getStyleClass().addAll("text-muted", "text-xs");
            dayHeader.setAlignment(Pos.CENTER);
            dayHeader.setMaxWidth(Double.MAX_VALUE);
            calendarGrid.add(dayHeader, i, 0);
        }

        LocalDate firstDay = currentMonth;
        int startCol = (firstDay.getDayOfWeek().getValue() - 1) % 7;
        int daysInMonth = currentMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int row = 1;
        int col = startCol;

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = currentMonth.withDayOfMonth(day);
            List<Interview> dayInterviews = byDate.getOrDefault(date, List.of());

            VBox cell = createDayCell(day, date, dayInterviews, date.equals(today));
            calendarGrid.add(cell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    private VBox createDayCell(int day, LocalDate date, List<Interview> dayInterviews, boolean isToday) {
        VBox cell = new VBox(2);
        cell.setPadding(new Insets(4));
        cell.setMinHeight(60);
        cell.setAlignment(Pos.TOP_CENTER);

        if (isToday) {
            cell.setStyle("-fx-background-color: -fx-primary; -fx-background-radius: 6;");
        } else if (!dayInterviews.isEmpty()) {
            cell.setStyle("-fx-background-color: -fx-card; -fx-background-radius: 6; -fx-border-color: -fx-border; -fx-border-radius: 6;");
        } else {
            cell.setStyle("-fx-background-color: transparent;");
        }

        Label dayLabel = new Label(String.valueOf(day));
        dayLabel.getStyleClass().add(isToday ? "text-primary-foreground" : "label");
        cell.getChildren().add(dayLabel);

        if (!dayInterviews.isEmpty()) {
            Label countLabel = new Label(dayInterviews.size() + " iv");
            countLabel.getStyleClass().addAll("text-2xs", isToday ? "text-primary-foreground" : "text-muted");
            cell.getChildren().add(countLabel);

            cell.setOnMouseClicked(e -> showDayDetail(date, dayInterviews));
            cell.setCursor(javafx.scene.Cursor.HAND);
        }

        return cell;
    }

    private void showDayDetail(LocalDate date, List<Interview> dayInterviews) {
        dayDetailBox.getChildren().clear();

        Label header = new Label(date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")));
        header.getStyleClass().add("h4");
        dayDetailBox.getChildren().add(header);

        for (Interview iv : dayInterviews) {
            TLCard card = new TLCard();
            HBox content = new HBox(12);
            content.setPadding(new Insets(10));
            content.setAlignment(Pos.CENTER_LEFT);

            Label timeLabel = new Label(iv.getScheduledDate().format(TIME_FMT));
            timeLabel.getStyleClass().add("h4");
            timeLabel.setMinWidth(50);

            VBox info = new VBox(2);
            HBox.setHgrow(info, Priority.ALWAYS);

            Label nameLabel = new Label(iv.getCandidateName() != null ? iv.getCandidateName() : "—");
            nameLabel.getStyleClass().add("label");

            Label jobLabel = new Label(iv.getJobTitle() != null ? iv.getJobTitle() : "");
            jobLabel.getStyleClass().add("text-muted");

            info.getChildren().addAll(nameLabel, jobLabel);

            TLBadge typeBadge = new TLBadge(iv.getType(), TLBadge.Variant.OUTLINE);
            TLBadge statusBadge = new TLBadge(iv.getStatus(),
                "COMPLETED".equals(iv.getStatus()) ? TLBadge.Variant.SUCCESS :
                "CANCELLED".equals(iv.getStatus()) ? TLBadge.Variant.DESTRUCTIVE :
                TLBadge.Variant.SECONDARY);

            Label durLabel = new Label(iv.getDurationMinutes() + " min");
            durLabel.getStyleClass().add("text-muted");

            content.getChildren().addAll(timeLabel, info, typeBadge, statusBadge, durLabel);
            card.getContent().add(content);
            dayDetailBox.getChildren().add(card);
        }
    }
}
