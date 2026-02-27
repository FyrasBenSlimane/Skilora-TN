package com.skilora.service.recruitment;

import com.google.gson.JsonObject;
import com.skilora.model.entity.recruitment.JobOffer;
import com.skilora.service.usermanagement.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI / intelligence layer for recruitment: match score, candidate score, job recommendations, CV analysis.
 * Can delegate to Python API or use local heuristics; stubs return safe defaults when unavailable.
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
     * Compatibility (match) percentage between a candidate profile and a job offer (0–100).
     * Delegates to CvMatchingService for keyword-based skill matching.
     */
    public CompletableFuture<Integer> calculateCompatibility(int profileId, int jobOfferId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return CvMatchingService.getInstance().calculateScore(profileId, jobOfferId);
            } catch (Exception e) {
                logger.debug("calculateCompatibility failed: {}", e.getMessage());
                return 0;
            }
        }, executor);
    }

    /**
     * Overall candidate quality score 0–100 based on profile completeness,
     * skills count, and work experience.
     */
    public CompletableFuture<Integer> scoreCandidate(int profileId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return CvMatchingService.getInstance().scoreCandidateProfile(profileId);
            } catch (Exception e) {
                logger.debug("scoreCandidate failed: {}", e.getMessage());
                return 0;
            }
        }, executor);
    }

    /**
     * Convenience synchronous wrapper – returns the full MatchResult
     * (score + matched/missing skills breakdown).
     */
    public CvMatchingService.MatchResult getMatchResult(int profileId, int jobOfferId) {
        try {
            return CvMatchingService.getInstance().calculate(profileId, jobOfferId);
        } catch (Exception e) {
            logger.debug("getMatchResult failed: {}", e.getMessage());
            return CvMatchingService.MatchResult.EMPTY;
        }
    }

    /**
     * Recommended job offers for this candidate (e.g. by skills/domain). domainPreference can be null.
     */
    public CompletableFuture<List<JobOffer>> recommendJobs(int profileId, String domainPreference, int limit) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Optional: call Python API /recommend-jobs or JobService + filter by profile skills
                return new ArrayList<>();
            } catch (Exception e) {
                logger.debug("recommendJobs failed: {}", e.getMessage());
                return new ArrayList<>();
            }
        }, executor);
    }

    /**
     * Analyze CV PDF and store detected skills on the profile. Returns analysis JSON.
     */
    public CompletableFuture<JsonObject> analyzeCvAndStore(int profileId, File cvFile) {
        if (profileId <= 0 || cvFile == null || !cvFile.isFile()) {
            return CompletableFuture.completedFuture(null);
        }
        return ProfileService.getInstance().analyzeCvAndEnrich(profileId, cvFile);
    }
}
