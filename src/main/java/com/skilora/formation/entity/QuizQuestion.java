package com.skilora.formation.entity;

import java.util.Objects;

/**
 * QuizQuestion Entity
 * 
 * Represents a multiple-choice question in a quiz.
 * Maps to the 'quiz_questions' table.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class QuizQuestion {
    private int id;
    private int quizId;
    private String questionText;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private char correctOption;
    private int points;
    private int orderIndex;

    // Default constructor
    public QuizQuestion() {
        this.points = 1;
        this.orderIndex = 0;
    }

    // Constructor with basic fields
    public QuizQuestion(int quizId, String questionText, char correctOption) {
        this();
        this.quizId = quizId;
        this.questionText = questionText;
        this.correctOption = correctOption;
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getQuizId() {
        return quizId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public String getOptionA() {
        return optionA;
    }

    public String getOptionB() {
        return optionB;
    }

    public String getOptionC() {
        return optionC;
    }

    public String getOptionD() {
        return optionD;
    }

    public char getCorrectOption() {
        return correctOption;
    }

    public int getPoints() {
        return points;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setQuizId(int quizId) {
        this.quizId = quizId;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public void setOptionA(String optionA) {
        this.optionA = optionA;
    }

    public void setOptionB(String optionB) {
        this.optionB = optionB;
    }

    public void setOptionC(String optionC) {
        this.optionC = optionC;
    }

    public void setOptionD(String optionD) {
        this.optionD = optionD;
    }

    public void setCorrectOption(char correctOption) {
        this.correctOption = correctOption;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    @Override
    public String toString() {
        return "QuizQuestion{" +
                "id=" + id +
                ", quizId=" + quizId +
                ", questionText='" + questionText + '\'' +
                ", correctOption=" + correctOption +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuizQuestion that = (QuizQuestion) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
