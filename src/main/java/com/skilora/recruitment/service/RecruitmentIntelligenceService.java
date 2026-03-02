package com.skilora.recruitment.service;

import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.entity.MatchingScore;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.entity.Experience;
import com.skilora.user.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI / intelligence layer for recruitment: match score, candidate score, job recommendations.
 * Delegates to MatchingService for compatibility calculations and ProfileService for profile data.
 * Returns safe defaults when unavailable.
 */
public class RecruitmentIntelligenceService {
    private static final Logger logger = LoggerFactory.getLogger(RecruitmentIntelligenceService.class);
    private static volatile RecruitmentIntelligenceService instance;
    private static final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r, "RecruitmentIntelligence");
        t.setDaemon(true);
        return t;
    });

    private RecruitmentIntelligenceService() {}

    public static synchronized RecruitmentIntelligenceService getInstance() {
        if (instance == null) {
            instance = new RecruitmentIntelligenceService();
        }
        return instance;
    }

    /**
     * Compatibility (match) percentage between a candidate profile and a job offer (0-100).
     * Delegates to MatchingService for weighted skill matching.
     */
    public CompletableFuture<Integer> calculateCompatibility(int profileId, int jobOfferId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                JobOffer offer = JobService.getInstance().findJobOfferById(jobOfferId);
                if (offer == null) return 0;
                MatchingScore score = MatchingService.getInstance().calculateMatch(profileId, offer);
                return score != null ? (int) score.getTotalScore() : 0;
            } catch (Exception e) {
                logger.debug("calculateCompatibility failed: {}", e.getMessage());
                return 0;
            }
        }, executor);
    }

    /**
     * Overall candidate quality score 0-100 based on profile completeness,
     * skills count, and work experience.
     */
    public CompletableFuture<Integer> scoreCandidate(int profileId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ProfileService ps = ProfileService.getInstance();
                Profile profile = ps.findProfileById(profileId);
                if (profile == null) return 0;

                List<Skill> skills = ps.findSkillsByProfileId(profileId);
                List<Experience> exps = ps.findExperiencesByProfileId(profileId);

                int score = 0;

                // Profile completeness (up to 25 pts)
                if (profile.getFirstName() != null && !profile.getFirstName().isBlank()) score += 5;
                if (profile.getLastName() != null && !profile.getLastName().isBlank()) score += 5;
                if (profile.getPhone() != null && !profile.getPhone().isBlank()) score += 5;
                if (profile.getLocation() != null && !profile.getLocation().isBlank()) score += 5;
                if (profile.getBirthDate() != null) score += 5;

                // Skills (up to 35 pts – 3 pts each, capped at 10 skills + bonus)
                int skillPts = Math.min(skills.size(), 10);
                score += skillPts * 3 + (skills.size() >= 5 ? 5 : 0);

                // Experience (up to 30 pts)
                if (!exps.isEmpty()) score += 15;
                if (exps.size() >= 2) score += 10;
                if (exps.size() >= 3) score += 5;

                // CV uploaded (10 pts)
                if (profile.getCvUrl() != null && !profile.getCvUrl().isBlank()) score += 10;

                return Math.min(100, score);
            } catch (Exception e) {
                logger.debug("scoreCandidate failed: {}", e.getMessage());
                return 0;
            }
        }, executor);
    }

    /**
     * Recommended job offers for this candidate (e.g. by skills/domain).
     * domainPreference can be null.
     */
    public CompletableFuture<List<JobOffer>> recommendJobs(int profileId, String domainPreference, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Placeholder: could call Python API /recommend-jobs or filter by profile skills
                return new ArrayList<>();
            } catch (Exception e) {
                logger.debug("recommendJobs failed: {}", e.getMessage());
                return new ArrayList<>();
            }
        }, executor);
    }
}
