package com.skilora.recruitment.entity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * MatchingScore Entity
 * 
 * Represents matching score between a profile and a job offer.
 * Uses the 40-30-20-10 scoring algorithm.
 * Pure data class with no business logic or JavaFX dependencies.
 */
public class MatchingScore {
    private int id;
    private int profileId;
    private int jobOfferId;
    private double totalScore;
    private double skillsScore; // 40% weight
    private double experienceScore; // 30% weight
    private double languageScore; // 20% weight
    private double locationScore; // 10% weight
    private String matchFactorsJson;
    private LocalDateTime calculatedAt;

    // Default constructor
    public MatchingScore() {
        this.totalScore = 0.0;
        this.skillsScore = 0.0;
        this.experienceScore = 0.0;
        this.languageScore = 0.0;
        this.locationScore = 0.0;
        this.calculatedAt = LocalDateTime.now();
    }

    // Constructor with profile and job offer IDs
    public MatchingScore(int profileId, int jobOfferId) {
        this();
        this.profileId = profileId;
        this.jobOfferId = jobOfferId;
    }

    // Constructor with all score components
    public MatchingScore(int profileId, int jobOfferId, double skillsScore, double experienceScore,
            double languageScore, double locationScore) {
        this.profileId = profileId;
        this.jobOfferId = jobOfferId;
        this.skillsScore = skillsScore;
        this.experienceScore = experienceScore;
        this.languageScore = languageScore;
        this.locationScore = locationScore;
        this.totalScore = calculateWeightedScore();
        this.calculatedAt = LocalDateTime.now();
    }

    // Getters
    public int getId() {
        return id;
    }

    public int getProfileId() {
        return profileId;
    }

    public int getJobOfferId() {
        return jobOfferId;
    }

    public double getTotalScore() {
        return totalScore;
    }

    public double getSkillsScore() {
        return skillsScore;
    }

    public double getExperienceScore() {
        return experienceScore;
    }

    public double getLanguageScore() {
        return languageScore;
    }

    public double getLocationScore() {
        return locationScore;
    }

    public String getMatchFactorsJson() {
        return matchFactorsJson;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setProfileId(int profileId) {
        this.profileId = profileId;
    }

    public void setJobOfferId(int jobOfferId) {
        this.jobOfferId = jobOfferId;
    }

    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public void setSkillsScore(double skillsScore) {
        this.skillsScore = skillsScore;
    }

    public void setExperienceScore(double experienceScore) {
        this.experienceScore = experienceScore;
    }

    public void setLanguageScore(double languageScore) {
        this.languageScore = languageScore;
    }

    public void setLocationScore(double locationScore) {
        this.locationScore = locationScore;
    }

    public void setMatchFactorsJson(String matchFactorsJson) {
        this.matchFactorsJson = matchFactorsJson;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    /**
     * Calculates the weighted total score using the 40-30-20-10 algorithm.
     */
    public double calculateWeightedScore() {
        double weighted = (skillsScore * 0.40) +
                (experienceScore * 0.30) +
                (languageScore * 0.20) +
                (locationScore * 0.10);
        return Math.round(weighted * 100.0) / 100.0;
    }

    /**
     * Updates all score components and recalculates total score.
     */
    public void updateScores(double skills, double experience, double language, double location) {
        this.skillsScore = skills;
        this.experienceScore = experience;
        this.languageScore = language;
        this.locationScore = location;
        this.totalScore = calculateWeightedScore();
        this.calculatedAt = LocalDateTime.now();
    }

    /**
     * Gets a breakdown of score components with their weights.
     */
    public Map<String, Object> getScoreBreakdown() {
        Map<String, Object> breakdown = new HashMap<>();
        breakdown.put("skills", Map.of("score", skillsScore, "weight", 40, "weighted", skillsScore * 0.40));
        breakdown.put("experience", Map.of("score", experienceScore, "weight", 30, "weighted", experienceScore * 0.30));
        breakdown.put("language", Map.of("score", languageScore, "weight", 20, "weighted", languageScore * 0.20));
        breakdown.put("location", Map.of("score", locationScore, "weight", 10, "weighted", locationScore * 0.10));
        breakdown.put("total", totalScore);
        return breakdown;
    }

    /**
     * Gets the match quality level based on total score.
     */
    public String getMatchQuality() {
        if (totalScore >= 90)
            return "Excellent";
        if (totalScore >= 75)
            return "Very Good";
        if (totalScore >= 60)
            return "Good";
        if (totalScore >= 45)
            return "Fair";
        return "Poor";
    }

    @Override
    public String toString() {
        return "MatchingScore{" +
                "id=" + id +
                ", profileId=" + profileId +
                ", jobOfferId=" + jobOfferId +
                ", totalScore=" + totalScore +
                ", quality='" + getMatchQuality() + '\'' +
                ", skills=" + skillsScore +
                ", experience=" + experienceScore +
                ", language=" + languageScore +
                ", location=" + locationScore +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MatchingScore that = (MatchingScore) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
