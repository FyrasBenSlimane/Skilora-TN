package com.skilora.formation.entity;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * QuizResult Entity
 * 
 * Represents the result of a user's quiz attempt.
 * Maps to the 'quiz_results' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class QuizResult {
    private int id;
    private int quizId;
    private int userId;
    private int score;
    private int maxScore;
    private boolean passed;
    private int attemptNumber;
    private LocalDateTime takenDate;
    private int timeSpentSeconds;

    // Default constructor
    public QuizResult() {
        this.score = 0;
        this.maxScore = 0;
        this.passed = false;
        this.attemptNumber = 1;
        this.takenDate = LocalDateTime.now();
        this.timeSpentSeconds = 0;
    }

    // Constructor with basic fields
    public QuizResult(int quizId, int userId, int score, int maxScore) {
        this();
        this.quizId = quizId;
        this.userId = userId;
        this.score = score;
        this.maxScore = maxScore;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getQuizId() {
        return quizId;
    }

    public int getUserId() {
        return userId;
    }

    public int getScore() {
        return score;
    }

    public int getMaxScore() {
        return maxScore;
    }

    public boolean isPassed() {
        return passed;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public LocalDateTime getTakenDate() {
        return takenDate;
    }

    public int getTimeSpentSeconds() {
        return timeSpentSeconds;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public void setMaxScore(int maxScore) {
        this.maxScore = maxScore;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public void setAttemptNumber(int attemptNumber) {
        this.attemptNumber = attemptNumber;
    }

    public void setTakenDate(LocalDateTime takenDate) {
        this.takenDate = takenDate;
    }

    public void setTimeSpentSeconds(int timeSpentSeconds) {
        this.timeSpentSeconds = timeSpentSeconds;
    }

    @Override
    public String toString() {
        return "QuizResult{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", userId=" + userId +
                ", score=" + score +
                ", maxScore=" + maxScore +
                ", passed=" + passed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizResult that = (QuizResult) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
