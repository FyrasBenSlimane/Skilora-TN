package com.skilora.formation.service;

import com.skilora.formation.entity.Enrollment;
import com.skilora.formation.entity.Formation;
import com.skilora.formation.enums.FormationLevel;
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.service.JobService;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class FormationMatchingService {

    private static final Logger logger = LoggerFactory.getLogger(FormationMatchingService.class);
    private static volatile FormationMatchingService instance;

    private final FormationService formationService;
    private final EnrollmentService enrollmentService;
    private final ProfileService profileService;
    private final JobService jobService;

    private FormationMatchingService() {
        this.formationService = FormationService.getInstance();
        this.enrollmentService = EnrollmentService.getInstance();
        this.profileService = ProfileService.getInstance();
        this.jobService = JobService.getInstance();
    }

    public static FormationMatchingService getInstance() {
        if (instance == null) {
            synchronized (FormationMatchingService.class) {
                if (instance == null) {
                    instance = new FormationMatchingService();
                }
            }
        }
        return instance;
    }

    /**
     * Returns scored formation recommendations for a user.
     */
    public List<ScoredFormation> getRecommendations(int userId, int limit) {
        try {
            Profile profile = profileService.findProfileByUserId(userId);
            if (profile == null) {
                logger.warn("No profile found for user {}", userId);
                return Collections.emptyList();
            }

            List<Skill> userSkills = profileService.findSkillsByProfileId(profile.getId());
            List<Experience> experiences = profileService.findExperiencesByProfileId(profile.getId());
            Set<String> ownedSkillNames = userSkills.stream()
                    .map(s -> s.getSkillName().toLowerCase())
                    .collect(Collectors.toSet());

            Set<Integer> enrolledFormationIds = enrollmentService.findByUserId(userId).stream()
                    .map(Enrollment::getFormationId)
                    .collect(Collectors.toSet());

            String userField = extractUserField(profile);
            int userExpLevel = inferExperienceLevel(experiences, userSkills);

            List<Formation> allFormations = formationService.findAll();
            List<ScoredFormation> scored = new ArrayList<>();

            for (Formation f : allFormations) {
                if (enrolledFormationIds.contains(f.getId())) {
                    continue;
                }
                if (!"ACTIVE".equalsIgnoreCase(f.getStatus())) {
                    continue;
                }

                double skillGapScore = computeSkillGapScore(f, ownedSkillNames);
                double categoryScore = computeCategoryScore(f, userField);
                double levelScore = computeLevelScore(f, userExpLevel);

                double total = (skillGapScore * 0.50) + (categoryScore * 0.30) + (levelScore * 0.20);
                total = Math.round(total * 100.0) / 100.0;

                if (total > 0) {
                    String reason = buildReason(skillGapScore, categoryScore, levelScore, f);
                    scored.add(new ScoredFormation(f, total, reason));
                }
            }

            scored.sort(Comparator.comparingDouble(ScoredFormation::getScore).reversed());
            return scored.stream().limit(limit).collect(Collectors.toList());

        } catch (SQLException e) {
            logger.error("Error computing recommendations for user {}: {}", userId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Returns formations that teach skills required by a job the user was rejected from.
     */
    public List<ScoredFormation> getSuggestionsForRejected(int userId, int jobOfferId) {
        try {
            JobOffer job = jobService.findJobOfferById(jobOfferId);
            if (job == null) {
                logger.warn("Job offer {} not found", jobOfferId);
                return Collections.emptyList();
            }

            List<String> requiredSkills = job.getRequiredSkills();
            if (requiredSkills == null || requiredSkills.isEmpty()) {
                return Collections.emptyList();
            }

            Profile profile = profileService.findProfileByUserId(userId);
            final Set<String> ownedSkillNames;
            if (profile != null) {
                ownedSkillNames = profileService.findSkillsByProfileId(profile.getId()).stream()
                        .map(s -> s.getSkillName().toLowerCase())
                        .collect(Collectors.toSet());
            } else {
                ownedSkillNames = new HashSet<>();
            }

            Set<String> missingSkills = requiredSkills.stream()
                    .map(String::toLowerCase)
                    .filter(s -> !ownedSkillNames.contains(s))
                    .collect(Collectors.toSet());

            if (missingSkills.isEmpty()) {
                return Collections.emptyList();
            }

            Set<Integer> enrolledIds = enrollmentService.findByUserId(userId).stream()
                    .map(Enrollment::getFormationId)
                    .collect(Collectors.toSet());

            List<Formation> allFormations = formationService.findAll();
            List<ScoredFormation> scored = new ArrayList<>();

            for (Formation f : allFormations) {
                if (enrolledIds.contains(f.getId())) {
                    continue;
                }
                if (!"ACTIVE".equalsIgnoreCase(f.getStatus())) {
                    continue;
                }

                long matchCount = missingSkills.stream()
                        .filter(skill -> containsKeyword(f, skill))
                        .count();

                if (matchCount > 0) {
                    double score = ((double) matchCount / missingSkills.size()) * 100.0;
                    score = Math.round(score * 100.0) / 100.0;

                    List<String> matched = missingSkills.stream()
                            .filter(skill -> containsKeyword(f, skill))
                            .collect(Collectors.toList());

                    String reason = "Teaches missing skills: " + String.join(", ", matched)
                            + " (required for " + job.getTitle() + ")";
                    scored.add(new ScoredFormation(f, score, reason));
                }
            }

            scored.sort(Comparator.comparingDouble(ScoredFormation::getScore).reversed());
            return scored;

        } catch (SQLException e) {
            logger.error("Error computing rejected suggestions for user {} / job {}: {}",
                    userId, jobOfferId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    // ── Scoring helpers ──

    private double computeSkillGapScore(Formation formation, Set<String> ownedSkills) {
        String corpus = buildSearchableText(formation);
        if (corpus.isEmpty()) {
            return 0;
        }

        String[] keywords = corpus.split("\\s+");
        Set<String> formationKeywords = new HashSet<>();
        for (String kw : keywords) {
            String clean = kw.replaceAll("[^a-zA-Z0-9]", "").toLowerCase();
            if (clean.length() > 2) {
                formationKeywords.add(clean);
            }
        }

        long gapSkills = formationKeywords.stream()
                .filter(kw -> !ownedSkills.contains(kw))
                .count();

        if (formationKeywords.isEmpty()) {
            return 0;
        }

        return Math.min(100.0, (gapSkills * 100.0) / Math.max(formationKeywords.size(), 1));
    }

    private double computeCategoryScore(Formation formation, String userField) {
        if (userField == null || userField.isEmpty()) {
            return 0;
        }
        String category = formation.getCategory() != null ? formation.getCategory().toLowerCase() : "";
        String title = formation.getTitle() != null ? formation.getTitle().toLowerCase() : "";
        String description = formation.getDescription() != null ? formation.getDescription().toLowerCase() : "";

        String[] fieldTokens = userField.toLowerCase().split("\\s+");
        int matches = 0;
        for (String token : fieldTokens) {
            if (token.length() <= 2) continue;
            if (category.contains(token) || title.contains(token) || description.contains(token)) {
                matches++;
            }
        }
        double ratio = (double) matches / Math.max(fieldTokens.length, 1);
        return Math.min(100.0, ratio * 100.0);
    }

    private double computeLevelScore(Formation formation, int userExpLevel) {
        FormationLevel fLevel = formation.getLevel();
        if (fLevel == null) {
            return 50;
        }

        int formationNumeric = switch (fLevel) {
            case BEGINNER -> 1;
            case INTERMEDIATE -> 2;
            case ADVANCED -> 3;
        };

        int diff = Math.abs(formationNumeric - userExpLevel);
        return switch (diff) {
            case 0 -> 100;
            case 1 -> 60;
            default -> 20;
        };
    }

    private int inferExperienceLevel(List<Experience> experiences, List<Skill> skills) {
        if (!skills.isEmpty()) {
            double avgLevel = skills.stream()
                    .mapToInt(s -> s.getProficiencyLevel() != null ? s.getProficiencyLevel().getLevel() : 1)
                    .average()
                    .orElse(1.0);
            if (avgLevel >= 3.0) return 3;
            if (avgLevel >= 2.0) return 2;
            return 1;
        }

        int totalMonths = experiences.stream()
                .mapToInt(Experience::getDurationInMonths)
                .sum();
        if (totalMonths >= 60) return 3;
        if (totalMonths >= 24) return 2;
        return 1;
    }

    private String extractUserField(Profile profile) {
        StringBuilder sb = new StringBuilder();
        if (profile.getHeadline() != null) sb.append(profile.getHeadline()).append(" ");
        if (profile.getBio() != null) sb.append(profile.getBio());
        return sb.toString().trim();
    }

    private boolean containsKeyword(Formation formation, String keyword) {
        String text = buildSearchableText(formation);
        return text.contains(keyword);
    }

    private String buildSearchableText(Formation formation) {
        StringBuilder sb = new StringBuilder();
        if (formation.getTitle() != null) sb.append(formation.getTitle()).append(" ");
        if (formation.getDescription() != null) sb.append(formation.getDescription()).append(" ");
        if (formation.getCategory() != null) sb.append(formation.getCategory());
        return sb.toString().toLowerCase();
    }

    private String buildReason(double skillGap, double category, double level, Formation f) {
        List<String> reasons = new ArrayList<>();
        if (skillGap >= 50) reasons.add("fills skill gaps");
        if (category >= 50) reasons.add("matches your field");
        if (level >= 80) reasons.add("right difficulty level");
        if (reasons.isEmpty()) reasons.add("general relevance");
        return String.join(", ", reasons) + " — " + f.getCategory();
    }

    // ── Inner class ──

    public static class ScoredFormation {
        private final Formation formation;
        private final double score;
        private final String reason;

        public ScoredFormation(Formation formation, double score, String reason) {
            this.formation = formation;
            this.score = score;
            this.reason = reason;
        }

        public Formation getFormation() { return formation; }
        public double getScore() { return score; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("ScoredFormation{title='%s', score=%.2f, reason='%s'}",
                    formation.getTitle(), score, reason);
        }
    }
}
