package com.skilora.recruitment.service;

import com.skilora.user.entity.*;
import com.skilora.recruitment.entity.*;
import com.skilora.user.service.ProfileService;
import com.skilora.utils.I18n;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MatchingService - CORE ALGORITHM
 * 
 * Implements the 40-30-20-10 weighted matching algorithm.
 * This follows the MVC pattern where Service = Model layer (data + logic).
 * 
 * ALGORITHM BREAKDOWN:
 * - Skills Match: 40% weight
 * • Matches required skills with candidate skills
 * • Considers proficiency level and years of experience
 * • Verified skills get bonus points
 * 
 * - Experience Match: 30% weight
 * • Evaluates total years of relevant experience
 * • Considers position similarity and company reputation
 * • Current job status weighted higher
 * 
 * - Language Match: 20% weight
 * • Language proficiency alignment (placeholder for future)
 * • Currently returns 100 (perfect match) as baseline
 * 
 * - Location Match: 10% weight
 * • Location proximity calculation
 * • Remote work compatibility check
 * 
 * Note: No JavaFX imports allowed in this class.
 */
public class MatchingService {

    private static volatile MatchingService instance;
    private final ProfileService profileService;

    // LRU cache for match scores: "profileId_jobId" -> MatchingScore
    private static final int SCORE_CACHE_SIZE = 500;
    private final Map<String, MatchingScore> scoreCache = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, MatchingScore> eldest) {
            return size() > SCORE_CACHE_SIZE;
        }
    };

    // Cache for profile data to avoid repeated DB queries within a batch
    private final ConcurrentHashMap<Integer, ProfileData> profileDataCache = new ConcurrentHashMap<>();

    private MatchingService() {
        this.profileService = ProfileService.getInstance();
    }

    public static MatchingService getInstance() {
        if (instance == null) {
            synchronized (MatchingService.class) {
                if (instance == null) {
                    instance = new MatchingService();
                }
            }
        }
        return instance;
    }

    /**
     * Calculates matching score between a profile and a job offer.
     * Uses 40-30-20-10 weighted algorithm.
     * 
     * @param profileId Candidate profile ID
     * @param jobOffer  Job offer to match against
     * @return MatchingScore object with detailed breakdown
     * @throws Exception if database error occurs
     */
    public MatchingScore calculateMatch(int profileId, JobOffer jobOffer) throws Exception {
        // Check score cache first
        String cacheKey = profileId + "_" + jobOffer.getId();
        synchronized (scoreCache) {
            MatchingScore cached = scoreCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }
        }

        try {
            // Get profile data (cached per batch)
            ProfileData data = profileDataCache.computeIfAbsent(profileId, id -> {
                try {
                    Profile profile = profileService.findProfileById(id);
                    List<Skill> skills = profileService.findSkillsByProfileId(id);
                    List<Experience> experiences = profileService.findExperiencesByProfileId(id);
                    return new ProfileData(profile, skills, experiences);
                } catch (Exception e) {
                    return null;
                }
            });

            if (data == null || data.profile == null) {
                throw new Exception(I18n.get("error.profile.not_found"));
            }

            // Calculate individual scores
            double skillsScore = calculateSkillMatch(data.skills, jobOffer.getRequiredSkills());
            double experienceScore = calculateExperienceMatch(data.experiences, jobOffer);
            double languageScore = calculateLanguageMatch(data.profile, jobOffer);
            double locationScore = calculateLocationMatch(data.profile, jobOffer);

            // Create matching score object
            MatchingScore matchingScore = new MatchingScore(profileId, jobOffer.getId());
            matchingScore.updateScores(skillsScore, experienceScore, languageScore, locationScore);

            // Build detailed factors JSON
            Map<String, Object> factors = buildMatchFactors(data.skills, data.experiences, data.profile, jobOffer);
            matchingScore.setMatchFactorsJson(factors.toString());

            // Cache the result
            synchronized (scoreCache) {
                scoreCache.put(cacheKey, matchingScore);
            }

            return matchingScore;

        } catch (SQLException e) {
            throw new Exception("Failed to calculate match: " + e.getMessage(), e);
        }
    }

    /**
     * Calculates skill match score (40% weight).
     * 
     * @param candidateSkills List of candidate skills
     * @param requiredSkills  List of required skills for job
     * @return Score from 0-100
     */
    public double calculateSkillMatch(List<Skill> candidateSkills, List<String> requiredSkills) {
        if (requiredSkills == null || requiredSkills.isEmpty()) {
            return 100.0; // No requirements = perfect match
        }

        if (candidateSkills == null || candidateSkills.isEmpty()) {
            return 0.0; // No skills = no match
        }

        int totalRequired = requiredSkills.size();
        double totalScore = 0.0;

        for (String requiredSkill : requiredSkills) {
            double bestMatchScore = 0.0;

            for (Skill candidateSkill : candidateSkills) {
                // Check if skill names match (case-insensitive, partial match)
                if (candidateSkill.getSkillName().toLowerCase().contains(requiredSkill.toLowerCase()) ||
                        requiredSkill.toLowerCase().contains(candidateSkill.getSkillName().toLowerCase())) {

                    // Base score for matching skill
                    double matchScore = 60.0;

                    // Proficiency level bonus (0-25 points)
                    if (candidateSkill.getProficiencyLevel() != null) {
                        matchScore += (candidateSkill.getProficiencyLevel().getLevel() * 6.25);
                    }

                    // Years of experience bonus (0-10 points)
                    matchScore += Math.min(candidateSkill.getYearsExperience() * 2, 10);

                    // Verified skill bonus (5 points)
                    if (candidateSkill.isVerified()) {
                        matchScore += 5.0;
                    }

                    bestMatchScore = Math.max(bestMatchScore, matchScore);
                }
            }

            totalScore += bestMatchScore;
        }

        // Average score across all required skills
        double averageScore = totalScore / totalRequired;
        return Math.min(averageScore, 100.0);
    }

    /**
     * Calculates experience match score (30% weight).
     * 
     * @param experiences List of candidate experiences
     * @param jobOffer    Job offer details
     * @return Score from 0-100
     */
    public double calculateExperienceMatch(List<Experience> experiences, JobOffer jobOffer) {
        if (experiences == null || experiences.isEmpty()) {
            return 30.0; // Some base score for entry-level positions
        }

        double score = 30.0; // Base score

        // Total experience in months
        int totalMonths = 0;
        for (Experience exp : experiences) {
            totalMonths += exp.getDurationInMonths();
        }
        int totalYears = totalMonths / 12;

        // Years of experience score (0-40 points)
        if (totalYears >= 5) {
            score += 40;
        } else if (totalYears >= 3) {
            score += 30;
        } else if (totalYears >= 1) {
            score += 20;
        } else {
            score += 10;
        }

        // Position relevance check (0-20 points)
        for (Experience exp : experiences) {
            if (exp.getPosition() != null && jobOffer.getTitle() != null) {
                String expPos = exp.getPosition().toLowerCase();
                String jobPos = jobOffer.getTitle().toLowerCase();

                // Check for keyword overlap
                if (expPos.contains(jobPos) || jobPos.contains(expPos)) {
                    score += 20;
                    break;
                }
            }
        }

        // Current job bonus (10 points)
        boolean hasCurrentJob = experiences.stream().anyMatch(Experience::isCurrentJob);
        if (hasCurrentJob) {
            score += 10;
        }

        return Math.min(score, 100.0);
    }

    /**
     * Calculates language match score (20% weight).
     * Compares profile location's language context with job requirements.
     * Tunisia-specific: French/Arabic regions get different scores.
     */
    public double calculateLanguageMatch(Profile profile, JobOffer jobOffer) {
        if (profile.getLocation() == null || jobOffer.getLocation() == null) {
            return 70.0; // Neutral score if data missing
        }

        String profileLoc = profile.getLocation().toLowerCase().trim();
        String jobLoc = jobOffer.getLocation().toLowerCase().trim();

        // Same location implies same language environment
        if (profileLoc.equals(jobLoc)) {
            return 100.0;
        }

        // Check if both are in Tunisia (same country context)
        boolean profileInTunisia = isTunisianLocation(profileLoc);
        boolean jobInTunisia = isTunisianLocation(jobLoc);

        if (profileInTunisia && jobInTunisia) {
            return 90.0; // Same country, similar language needs
        }

        // International match - lower confidence
        if (profileInTunisia != jobInTunisia) {
            // Check for French-speaking countries
            boolean profileFrench = isFrenchSpeaking(profileLoc);
            boolean jobFrench = isFrenchSpeaking(jobLoc);
            if (profileFrench && jobFrench) {
                return 80.0;
            }
            return 50.0; // Different language context
        }

        return 60.0;
    }

    private boolean isTunisianLocation(String location) {
        String[] tunisianCities = {"tunis", "sfax", "sousse", "kairouan", "bizerte",
                "gabes", "ariana", "gafsa", "monastir", "ben arous", "kasserine",
                "medenine", "nabeul", "tataouine", "beja", "jendouba", "mahdia",
                "sidi bouzid", "tozeur", "siliana", "zaghouan", "kebili", "manouba",
                "la marsa", "hammamet", "djerba", "tunisia", "tunisie"};
        for (String city : tunisianCities) {
            if (location.contains(city)) return true;
        }
        return false;
    }

    private boolean isFrenchSpeaking(String location) {
        String[] frenchLocations = {"france", "paris", "lyon", "marseille", "belgium",
                "bruxelles", "brussels", "canada", "montreal", "quebec", "switzerland",
                "geneve", "geneva", "luxembourg", "senegal", "dakar", "morocco",
                "casablanca", "rabat", "algeria", "alger"};
        for (String loc : frenchLocations) {
            if (location.contains(loc)) return true;
        }
        return false;
    }

    /**
     * Calculates location match score (10% weight).
     */
    public double calculateLocationMatch(Profile profile, JobOffer jobOffer) {
        if (profile.getLocation() == null || jobOffer.getLocation() == null) {
            return 50.0; // Neutral score if location data missing
        }

        String profileLocation = profile.getLocation().toLowerCase().trim();
        String jobLocation = jobOffer.getLocation().toLowerCase().trim();

        // Exact match
        if (profileLocation.equals(jobLocation)) {
            return 100.0;
        }

        // Same city/region (contains check)
        if (profileLocation.contains(jobLocation) || jobLocation.contains(profileLocation)) {
            return 80.0;
        }

        // Different location
        return 40.0;
    }

    /**
     * Builds detailed match factors for JSON storage.
     */
    private Map<String, Object> buildMatchFactors(List<Skill> skills, List<Experience> experiences,
            Profile profile, JobOffer jobOffer) {
        Map<String, Object> factors = new HashMap<>();

        // Skills breakdown
        factors.put("totalSkills", skills.size());
        factors.put("verifiedSkills", skills.stream().filter(Skill::isVerified).count());
        factors.put("requiredSkills", jobOffer.getRequiredSkills().size());

        // Experience breakdown
        int totalMonths = experiences.stream()
                .mapToInt(Experience::getDurationInMonths)
                .sum();
        factors.put("totalExperienceMonths", totalMonths);
        factors.put("totalExperienceYears", totalMonths / 12);
        factors.put("hasCurrentJob", experiences.stream().anyMatch(Experience::isCurrentJob));

        // Location
        factors.put("profileLocation", profile.getLocation());
        factors.put("jobLocation", jobOffer.getLocation());

        return factors;
    }

    /**
     * Holds cached profile data to avoid N+1 queries when matching
     * the same profile against multiple jobs.
     */
    private static class ProfileData {
        final Profile profile;
        final List<Skill> skills;
        final List<Experience> experiences;

        ProfileData(Profile profile, List<Skill> skills, List<Experience> experiences) {
            this.profile = profile;
            this.skills = skills;
            this.experiences = experiences;
        }
    }
}
