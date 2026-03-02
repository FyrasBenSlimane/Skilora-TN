package com.skilora.formation.controller;

import com.skilora.formation.entity.Quiz;
import com.skilora.formation.entity.QuizQuestion;
import com.skilora.formation.entity.QuizResult;
import com.skilora.formation.service.QuizService;
import com.skilora.framework.components.*;
import com.skilora.utils.AppThreadPool;
import com.skilora.utils.I18n;
import com.skilora.utils.SvgIcons;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.util.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * QuizController - Programmatic controller for students to take quizzes.
 * Not FXML-based; exposes a static entry point that other controllers can call.
 */
public class QuizController {

    private static final Logger logger = LoggerFactory.getLogger(QuizController.class);
    private static final QuizService quizService = QuizService.getInstance();

    private QuizController() {}

    // ── Public entry point ──────────────────────────────────────────

    public static void showQuizDialog(int formationId, int userId, Scene scene) {
        if (scene == null) return;

        AppThreadPool.execute(() -> {
            List<Quiz> quizzes = quizService.findQuizzesByFormation(formationId);
            Platform.runLater(() -> {
                if (quizzes == null || quizzes.isEmpty()) {
                    TLToast.info(scene, I18n.get("quiz.title"), I18n.get("quiz.no_quizzes"));
                    return;
                }
                if (quizzes.size() == 1) {
                    openQuizSession(quizzes.get(0), userId, scene);
                } else {
                    showQuizPicker(quizzes, userId, scene);
                }
            });
        });
    }

    // ── Quiz picker (multiple quizzes) ──────────────────────────────

    private static void showQuizPicker(List<Quiz> quizzes, int userId, Scene scene) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.initOwner(scene.getWindow());
        dialog.setDialogTitle(I18n.get("quiz.select_quiz"));
        dialog.setDescription(I18n.get("quiz.select_quiz_desc"));

        VBox list = new VBox(10);
        list.setPadding(new Insets(4, 0, 4, 0));

        for (Quiz quiz : quizzes) {
            TLCard card = new TLCard();
            card.setCursor(javafx.scene.Cursor.HAND);

            Label title = new Label(quiz.getTitle());
            title.getStyleClass().add("card-title");

            HBox meta = new HBox(12);
            meta.setAlignment(Pos.CENTER_LEFT);
            meta.getChildren().addAll(
                    SvgIcons.withText(SvgIcons.CLOCK, quiz.getTimeLimitMinutes() + " min"),
                    SvgIcons.withText(SvgIcons.CHECK_CIRCLE, I18n.get("quiz.pass_score") + ": " + quiz.getPassScore() + "%")
            );

            VBox content = new VBox(6, title, meta);
            if (quiz.getDescription() != null && !quiz.getDescription().isBlank()) {
                Label desc = new Label(quiz.getDescription());
                desc.setWrapText(true);
                desc.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 13px;");
                content.getChildren().add(1, desc);
            }

            appendAttemptInfo(content, quiz, userId);

            card.setBody(content);
            card.setOnMouseClicked(e -> {
                dialog.setResult(ButtonType.CLOSE);
                dialog.close();
                openQuizSession(quiz, userId, scene);
            });

            list.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(list);
        scroll.setFitToWidth(true);
        scroll.setPrefViewportHeight(360);
        scroll.setStyle("-fx-background-color: transparent;");

        dialog.setContent(scroll);
        dialog.addButton(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    private static void appendAttemptInfo(VBox content, Quiz quiz, int userId) {
        AppThreadPool.execute(() -> {
            int attempts = quizService.getAttemptCount(quiz.getId(), userId);
            boolean passed = quizService.hasPassedQuiz(quiz.getId(), userId);
            QuizResult best = quizService.getBestResult(quiz.getId(), userId);
            Platform.runLater(() -> {
                HBox badges = new HBox(8);
                badges.setAlignment(Pos.CENTER_LEFT);

                if (passed) {
                    badges.getChildren().add(new TLBadge(I18n.get("quiz.passed"), TLBadge.Variant.SUCCESS));
                }
                if (attempts > 0) {
                    String attemptsText = I18n.get("quiz.attempts", attempts, quiz.getMaxAttempts());
                    badges.getChildren().add(new TLBadge(attemptsText, TLBadge.Variant.SECONDARY));
                }
                if (best != null) {
                    String bestText = I18n.get("quiz.best_score", best.getScore(), best.getMaxScore());
                    badges.getChildren().add(new TLBadge(bestText, TLBadge.Variant.OUTLINE));
                }
                if (attempts >= quiz.getMaxAttempts()) {
                    badges.getChildren().add(new TLBadge(I18n.get("quiz.max_attempts_reached"), TLBadge.Variant.DESTRUCTIVE));
                }

                if (!badges.getChildren().isEmpty()) {
                    content.getChildren().add(badges);
                }
            });
        });
    }

    // ── Quiz session ────────────────────────────────────────────────

    private static void openQuizSession(Quiz quiz, int userId, Scene scene) {
        AppThreadPool.execute(() -> {
            List<QuizQuestion> questions = quizService.getQuestions(quiz.getId());
            int attempts = quizService.getAttemptCount(quiz.getId(), userId);
            QuizResult bestResult = quizService.getBestResult(quiz.getId(), userId);

            Platform.runLater(() -> {
                if (questions == null || questions.isEmpty()) {
                    TLToast.warning(scene, I18n.get("quiz.title"), I18n.get("quiz.no_questions"));
                    return;
                }
                if (attempts >= quiz.getMaxAttempts()) {
                    showMaxAttemptsDialog(quiz, bestResult, scene);
                    return;
                }
                showQuizTakingDialog(quiz, questions, userId, attempts, bestResult, scene);
            });
        });
    }

    private static void showMaxAttemptsDialog(Quiz quiz, QuizResult bestResult, Scene scene) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.initOwner(scene.getWindow());
        dialog.setDialogTitle(quiz.getTitle());
        dialog.setDescription(I18n.get("quiz.max_attempts_reached"));

        VBox body = new VBox(12);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(12));

        Label icon = new Label();
        icon.setGraphic(SvgIcons.icon(SvgIcons.LOCK, 48, "-fx-muted-foreground"));

        Label message = new Label(I18n.get("quiz.max_attempts_message", quiz.getMaxAttempts()));
        message.setWrapText(true);
        message.setAlignment(Pos.CENTER);
        message.setStyle("-fx-text-fill: -fx-muted-foreground; -fx-font-size: 14px;");

        body.getChildren().addAll(icon, message);

        if (bestResult != null) {
            body.getChildren().add(new TLSeparator());
            body.getChildren().add(buildResultSummary(bestResult, quiz));
        }

        dialog.setContent(body);
        dialog.addButton(ButtonType.OK);
        dialog.showAndWait();
    }

    // ── Main quiz-taking dialog ─────────────────────────────────────

    private static void showQuizTakingDialog(Quiz quiz, List<QuizQuestion> questions,
                                              int userId, int previousAttempts,
                                              QuizResult bestResult, Scene scene) {
        final int totalQuestions = questions.size();
        final int[] currentIndex = {0};
        final Map<Integer, Character> answers = new HashMap<>();
        final long startTime = System.currentTimeMillis();

        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.initOwner(scene.getWindow());
        dialog.setDialogTitle(quiz.getTitle());

        VBox container = new VBox(16);
        container.setPrefWidth(560);
        container.setMinHeight(400);

        // -- Header: progress + timer
        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label counterLabel = new Label();
        counterLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label timerLabel = new Label();
        timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-muted-foreground;");
        timerLabel.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 14, "-fx-muted-foreground"));

        headerRow.getChildren().addAll(counterLabel, spacer, timerLabel);

        TLProgress progressBar = new TLProgress(0.0);

        // -- Question area
        Label questionText = new Label();
        questionText.setWrapText(true);
        questionText.setStyle("-fx-font-size: 15px; -fx-font-weight: 600;");

        ToggleGroup optionGroup = new ToggleGroup();
        VBox optionsBox = new VBox(8);
        optionsBox.setPadding(new Insets(4, 0, 4, 0));

        RadioButton optA = createOptionRadio('A', optionGroup);
        RadioButton optB = createOptionRadio('B', optionGroup);
        RadioButton optC = createOptionRadio('C', optionGroup);
        RadioButton optD = createOptionRadio('D', optionGroup);
        optionsBox.getChildren().addAll(optA, optB, optC, optD);

        // -- Navigation buttons
        TLButton prevBtn = new TLButton(I18n.get("quiz.previous"), TLButton.ButtonVariant.OUTLINE);
        prevBtn.setGraphic(SvgIcons.icon(SvgIcons.ARROW_LEFT, 14));

        TLButton nextBtn = new TLButton(I18n.get("quiz.next"), TLButton.ButtonVariant.PRIMARY);

        TLButton submitBtn = new TLButton(I18n.get("quiz.submit"), TLButton.ButtonVariant.SUCCESS);
        submitBtn.setGraphic(SvgIcons.icon(SvgIcons.CHECK, 14, "-fx-primary-foreground"));

        Region navSpacer = new Region();
        HBox.setHgrow(navSpacer, Priority.ALWAYS);

        HBox navBar = new HBox(10, prevBtn, navSpacer, nextBtn, submitBtn);
        navBar.setAlignment(Pos.CENTER);

        // -- Best result banner
        VBox bestBanner = new VBox();
        if (bestResult != null) {
            HBox bannerContent = new HBox(8);
            bannerContent.setAlignment(Pos.CENTER_LEFT);
            bannerContent.setPadding(new Insets(8, 12, 8, 12));
            bannerContent.setStyle(
                    "-fx-background-color: -fx-muted; -fx-background-radius: 8px;");

            Label bannerText = new Label(I18n.get("quiz.previous_best",
                    bestResult.getScore(), bestResult.getMaxScore()));
            bannerText.setStyle("-fx-font-size: 13px; -fx-text-fill: -fx-muted-foreground;");

            TLBadge passBadge = bestResult.isPassed()
                    ? new TLBadge(I18n.get("quiz.passed"), TLBadge.Variant.SUCCESS)
                    : new TLBadge(I18n.get("quiz.failed"), TLBadge.Variant.DESTRUCTIVE);

            bannerContent.getChildren().addAll(bannerText, passBadge);
            bestBanner.getChildren().add(bannerContent);
        }

        container.getChildren().addAll(
                headerRow, progressBar, new TLSeparator(),
                questionText, optionsBox, new TLSeparator(), navBar
        );
        if (!bestBanner.getChildren().isEmpty()) {
            container.getChildren().add(0, bestBanner);
        }

        dialog.setContent(container);

        // -- Timer logic
        final int[] remainingSeconds = {quiz.getTimeLimitMinutes() * 60};
        Timeline timer = new Timeline();
        if (quiz.getTimeLimitMinutes() > 0) {
            timer.getKeyFrames().add(new KeyFrame(Duration.seconds(1), e -> {
                remainingSeconds[0]--;
                int mins = remainingSeconds[0] / 60;
                int secs = remainingSeconds[0] % 60;
                timerLabel.setText(String.format("%02d:%02d", mins, secs));

                if (remainingSeconds[0] <= 60) {
                    timerLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: -fx-destructive;");
                }
                if (remainingSeconds[0] <= 0) {
                    timer.stop();
                    submitQuiz(quiz, questions, answers, userId, previousAttempts,
                            startTime, dialog, scene);
                }
            }));
            timer.setCycleCount(Animation.INDEFINITE);
            timer.play();
            timerLabel.setText(String.format("%02d:%02d",
                    quiz.getTimeLimitMinutes(), 0));
        } else {
            timerLabel.setVisible(false);
            timerLabel.setManaged(false);
        }

        // -- Save answer on toggle
        optionGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal instanceof RadioButton rb && rb.getUserData() instanceof Character c) {
                answers.put(questions.get(currentIndex[0]).getId(), c);
                updateSubmitVisibility(submitBtn, nextBtn, currentIndex[0], totalQuestions, answers, questions);
            }
        });

        // -- Render question
        Runnable renderQuestion = () -> {
            QuizQuestion q = questions.get(currentIndex[0]);
            counterLabel.setText(I18n.get("quiz.question_of", currentIndex[0] + 1, totalQuestions));
            progressBar.setProgress((double) (currentIndex[0] + 1) / totalQuestions);
            questionText.setText(q.getQuestionText());

            optA.setText("A.  " + (q.getOptionA() != null ? q.getOptionA() : ""));
            optB.setText("B.  " + (q.getOptionB() != null ? q.getOptionB() : ""));
            optC.setText("C.  " + (q.getOptionC() != null ? q.getOptionC() : ""));
            optD.setText("D.  " + (q.getOptionD() != null ? q.getOptionD() : ""));

            Character saved = answers.get(q.getId());
            if (saved != null) {
                switch (saved) {
                    case 'A' -> optA.setSelected(true);
                    case 'B' -> optB.setSelected(true);
                    case 'C' -> optC.setSelected(true);
                    case 'D' -> optD.setSelected(true);
                }
            } else {
                optionGroup.selectToggle(null);
            }

            prevBtn.setDisable(currentIndex[0] == 0);
            nextBtn.setVisible(currentIndex[0] < totalQuestions - 1);
            nextBtn.setManaged(currentIndex[0] < totalQuestions - 1);

            updateSubmitVisibility(submitBtn, nextBtn, currentIndex[0], totalQuestions, answers, questions);
        };

        // Initial render
        renderQuestion.run();

        // -- Navigation handlers
        prevBtn.setOnAction(e -> {
            if (currentIndex[0] > 0) {
                currentIndex[0]--;
                renderQuestion.run();
            }
        });

        nextBtn.setOnAction(e -> {
            if (currentIndex[0] < totalQuestions - 1) {
                currentIndex[0]++;
                renderQuestion.run();
            }
        });

        submitBtn.setOnAction(e -> {
            timer.stop();
            submitQuiz(quiz, questions, answers, userId, previousAttempts,
                    startTime, dialog, scene);
        });

        // Cleanup timer when dialog closes
        dialog.setOnHidden(e -> timer.stop());

        dialog.addButton(ButtonType.CLOSE);
        Button closeBtn = (Button) dialog.getDialogPane().lookupButton(ButtonType.CLOSE);
        if (closeBtn != null) {
            closeBtn.setText(I18n.get("quiz.cancel"));
            closeBtn.setOnAction(e -> {
                timer.stop();
                dialog.setResult(ButtonType.CLOSE);
                dialog.close();
            });
        }

        dialog.showAndWait();
    }

    private static RadioButton createOptionRadio(char option, ToggleGroup group) {
        RadioButton rb = new RadioButton();
        rb.setToggleGroup(group);
        rb.setUserData(option);
        rb.setWrapText(true);
        rb.setStyle("-fx-font-size: 14px;");
        rb.setPadding(new Insets(6, 12, 6, 4));
        return rb;
    }

    private static void updateSubmitVisibility(TLButton submitBtn, TLButton nextBtn,
                                                int currentIndex, int totalQuestions,
                                                Map<Integer, Character> answers,
                                                List<QuizQuestion> questions) {
        boolean isLast = currentIndex == totalQuestions - 1;
        boolean allAnswered = questions.stream()
                .allMatch(q -> answers.containsKey(q.getId()));

        submitBtn.setVisible(isLast || allAnswered);
        submitBtn.setManaged(isLast || allAnswered);
    }

    // ── Submit & score ──────────────────────────────────────────────

    private static void submitQuiz(Quiz quiz, List<QuizQuestion> questions,
                                    Map<Integer, Character> answers, int userId,
                                    int previousAttempts, long startTime,
                                    TLDialog<ButtonType> quizDialog, Scene scene) {
        int score = 0;
        int maxScore = 0;
        for (QuizQuestion q : questions) {
            maxScore += q.getPoints();
            Character userAnswer = answers.get(q.getId());
            if (userAnswer != null && userAnswer == q.getCorrectOption()) {
                score += q.getPoints();
            }
        }

        int percentage = maxScore > 0 ? (int) Math.round((double) score / maxScore * 100) : 0;
        boolean passed = percentage >= quiz.getPassScore();
        int timeSpent = (int) ((System.currentTimeMillis() - startTime) / 1000);

        QuizResult result = new QuizResult();
        result.setQuizId(quiz.getId());
        result.setUserId(userId);
        result.setScore(score);
        result.setMaxScore(maxScore);
        result.setPassed(passed);
        result.setAttemptNumber(previousAttempts + 1);
        result.setTakenDate(LocalDateTime.now());
        result.setTimeSpentSeconds(timeSpent);

        quizDialog.setResult(ButtonType.CLOSE);
        quizDialog.close();

        AppThreadPool.execute(() -> {
            int resultId = quizService.submitResult(result);
            if (resultId < 0) {
                logger.error("Failed to save quiz result for quiz={}, user={}", quiz.getId(), userId);
            }
            Platform.runLater(() -> showResultDialog(result, quiz, questions, answers, scene));
        });
    }

    // ── Result dialog ───────────────────────────────────────────────

    private static void showResultDialog(QuizResult result, Quiz quiz,
                                          List<QuizQuestion> questions,
                                          Map<Integer, Character> answers, Scene scene) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.initOwner(scene.getWindow());
        dialog.setDialogTitle(I18n.get("quiz.result"));

        VBox body = new VBox(16);
        body.setAlignment(Pos.CENTER);
        body.setPadding(new Insets(8));
        body.setPrefWidth(480);

        // Pass/fail header
        boolean passed = result.isPassed();
        Label statusIcon = new Label();
        statusIcon.setGraphic(SvgIcons.icon(
                passed ? SvgIcons.CHECK_CIRCLE : SvgIcons.X_CIRCLE,
                56,
                passed ? "-fx-success" : "-fx-destructive"
        ));

        Label statusLabel = new Label(passed ? I18n.get("quiz.passed") : I18n.get("quiz.failed"));
        statusLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: "
                + (passed ? "-fx-success" : "-fx-destructive") + ";");

        body.getChildren().addAll(statusIcon, statusLabel);

        // Score summary
        body.getChildren().add(buildResultSummary(result, quiz));

        // Question breakdown
        body.getChildren().add(new TLSeparator());

        Label breakdownTitle = new Label(I18n.get("quiz.breakdown"));
        breakdownTitle.setStyle("-fx-font-size: 15px; -fx-font-weight: 600;");
        body.getChildren().add(breakdownTitle);

        VBox breakdownBox = new VBox(6);
        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            Character userAnswer = answers.get(q.getId());
            boolean correct = userAnswer != null && userAnswer == q.getCorrectOption();

            HBox row = new HBox(8);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(4, 8, 4, 8));
            row.setStyle("-fx-background-color: " + (correct ? "-fx-success" : "-fx-destructive")
                    + "; -fx-background-radius: 6px; -fx-opacity: 0.12;");

            Label qNum = new Label(String.valueOf(i + 1));
            qNum.setMinWidth(24);
            qNum.setAlignment(Pos.CENTER);
            qNum.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");

            Label qText = new Label(truncate(q.getQuestionText(), 50));
            qText.setStyle("-fx-font-size: 13px;");
            HBox.setHgrow(qText, Priority.ALWAYS);

            Label pointsLabel = new Label("+" + (correct ? q.getPoints() : 0) + "/" + q.getPoints());
            pointsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

            Label statusMark = new Label();
            statusMark.setGraphic(SvgIcons.icon(
                    correct ? SvgIcons.CHECK : SvgIcons.ALERT_TRIANGLE,
                    14,
                    correct ? "-fx-success" : "-fx-destructive"
            ));

            row.getChildren().addAll(qNum, qText, pointsLabel, statusMark);
            breakdownBox.getChildren().add(row);
        }

        ScrollPane breakdownScroll = new ScrollPane(breakdownBox);
        breakdownScroll.setFitToWidth(true);
        breakdownScroll.setPrefViewportHeight(200);
        breakdownScroll.setStyle("-fx-background-color: transparent;");

        body.getChildren().add(breakdownScroll);

        dialog.setContent(body);
        dialog.addButton(ButtonType.OK);
        dialog.showAndWait();
    }

    private static VBox buildResultSummary(QuizResult result, Quiz quiz) {
        VBox summary = new VBox(10);
        summary.setAlignment(Pos.CENTER);
        summary.setPadding(new Insets(12));
        summary.setStyle("-fx-background-color: -fx-muted; -fx-background-radius: 10px;");

        int percentage = result.getMaxScore() > 0
                ? (int) Math.round((double) result.getScore() / result.getMaxScore() * 100)
                : 0;

        Label scoreLabel = new Label(result.getScore() + " / " + result.getMaxScore());
        scoreLabel.setStyle("-fx-font-size: 28px; -fx-font-weight: bold;");

        Label percentLabel = new Label(percentage + "%");
        percentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: -fx-muted-foreground;");

        TLProgress scoreBar = new TLProgress(
                (double) percentage / 100,
                TLProgress.Size.DEFAULT,
                result.isPassed() ? TLProgress.Variant.SUCCESS : TLProgress.Variant.DESTRUCTIVE
        );
        scoreBar.setMaxWidth(300);

        HBox detailsRow = new HBox(16);
        detailsRow.setAlignment(Pos.CENTER);

        Label passScoreInfo = new Label(I18n.get("quiz.pass_score") + ": " + quiz.getPassScore() + "%");
        passScoreInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

        int mins = result.getTimeSpentSeconds() / 60;
        int secs = result.getTimeSpentSeconds() % 60;
        Label timeInfo = new Label(I18n.get("quiz.time_spent", String.format("%d:%02d", mins, secs)));
        timeInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");
        timeInfo.setGraphic(SvgIcons.icon(SvgIcons.CLOCK, 12, "-fx-muted-foreground"));

        Label attemptInfo = new Label(I18n.get("quiz.attempt_number", result.getAttemptNumber()));
        attemptInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: -fx-muted-foreground;");

        detailsRow.getChildren().addAll(passScoreInfo, timeInfo, attemptInfo);

        summary.getChildren().addAll(scoreLabel, percentLabel, scoreBar, detailsRow);
        return summary;
    }

    // ── Utilities ───────────────────────────────────────────────────

    private static String truncate(String text, int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen - 1) + "\u2026";
    }
}
