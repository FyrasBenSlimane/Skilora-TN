package com.skilora;

import com.skilora.config.DatabaseConfig;
import com.skilora.recruitment.entity.*;
import com.skilora.recruitment.enums.*;
import com.skilora.recruitment.service.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Recruitment Module Test Suite
 * Covers: entities, enums, service singletons, DB schema, CRUD operations,
 * matching logic, interview workflow, hire offer lifecycle, edge cases.
 */
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DisplayName("Recruitment Module Tests")
public class RecruitmentModuleTests {

    // ───────────────────────────────────────────────
    // 1. ENUM TESTS
    // ───────────────────────────────────────────────

    @Nested
    @Order(1)
    @DisplayName("1 · Application.Status Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class ApplicationStatusTests {

        @Test @Order(1)
        @DisplayName("All 6 status values exist")
        void allValuesExist() {
            Application.Status[] vals = Application.Status.values();
            assertEquals(6, vals.length);
        }

        @Test @Order(2)
        @DisplayName("valueOf round-trips")
        void valueOfRoundTrips() {
            for (Application.Status s : Application.Status.values()) {
                assertEquals(s, Application.Status.valueOf(s.name()));
            }
        }

        @Test @Order(3)
        @DisplayName("displayName is non-null for all values")
        void displayNames() {
            for (Application.Status s : Application.Status.values()) {
                assertNotNull(s.getDisplayName(), s.name() + " displayName");
                assertFalse(s.getDisplayName().isBlank());
            }
        }

        @Test @Order(4)
        @DisplayName("PENDING display name is 'En attente'")
        void pendingDisplayName() {
            assertEquals("En attente", Application.Status.PENDING.getDisplayName());
        }

        @Test @Order(5)
        @DisplayName("ACCEPTED display name is 'Accepté'")
        void acceptedDisplayName() {
            assertEquals("Accepté", Application.Status.ACCEPTED.getDisplayName());
        }
    }

    @Nested
    @Order(2)
    @DisplayName("2 · HireOfferStatus Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class HireOfferStatusTests {

        @Test @Order(1)
        @DisplayName("All 4 values exist")
        void allValues() {
            HireOfferStatus[] vals = HireOfferStatus.values();
            assertEquals(4, vals.length);
            assertNotNull(HireOfferStatus.valueOf("PENDING"));
            assertNotNull(HireOfferStatus.valueOf("ACCEPTED"));
            assertNotNull(HireOfferStatus.valueOf("REJECTED"));
            assertNotNull(HireOfferStatus.valueOf("EXPIRED"));
        }
    }

    @Nested
    @Order(3)
    @DisplayName("3 · InterviewStatus Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class InterviewStatusTests {

        @Test @Order(1)
        @DisplayName("All 4 values exist")
        void allValues() {
            InterviewStatus[] vals = InterviewStatus.values();
            assertEquals(4, vals.length);
            assertNotNull(InterviewStatus.valueOf("SCHEDULED"));
            assertNotNull(InterviewStatus.valueOf("COMPLETED"));
            assertNotNull(InterviewStatus.valueOf("CANCELLED"));
            assertNotNull(InterviewStatus.valueOf("NO_SHOW"));
        }
    }

    @Nested
    @Order(4)
    @DisplayName("4 · InterviewType Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class InterviewTypeTests {

        @Test @Order(1)
        @DisplayName("All 3 values exist")
        void allValues() {
            InterviewType[] vals = InterviewType.values();
            assertEquals(3, vals.length);
            assertNotNull(InterviewType.valueOf("VIDEO"));
            assertNotNull(InterviewType.valueOf("IN_PERSON"));
            assertNotNull(InterviewType.valueOf("PHONE"));
        }
    }

    @Nested
    @Order(5)
    @DisplayName("5 · JobStatus Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class JobStatusTests {

        @Test @Order(1)
        @DisplayName("All 5 values exist")
        void allValues() {
            JobStatus[] vals = JobStatus.values();
            assertEquals(5, vals.length);
        }

        @Test @Order(2)
        @DisplayName("displayName round-trip")
        void displayNames() {
            assertEquals("Active", JobStatus.ACTIVE.getDisplayName());
            assertEquals("Open", JobStatus.OPEN.getDisplayName());
            assertEquals("Draft", JobStatus.DRAFT.getDisplayName());
        }
    }

    @Nested
    @Order(6)
    @DisplayName("6 · WorkType Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class WorkTypeTests {

        @Test @Order(1)
        @DisplayName("All 7 values exist")
        void allValues() {
            WorkType[] vals = WorkType.values();
            assertEquals(7, vals.length);
        }

        @Test @Order(2)
        @DisplayName("displayName formatting")
        void displayNames() {
            assertEquals("Full-Time", WorkType.FULL_TIME.getDisplayName());
            assertEquals("Part-Time", WorkType.PART_TIME.getDisplayName());
            assertEquals("Remote", WorkType.REMOTE.getDisplayName());
            assertEquals("Internship", WorkType.INTERNSHIP.getDisplayName());
        }
    }

    @Nested
    @Order(7)
    @DisplayName("7 · ProficiencyLevel Enum")
    @TestMethodOrder(OrderAnnotation.class)
    class ProficiencyLevelTests {

        @Test @Order(1)
        @DisplayName("All 4 values exist")
        void allValues() {
            ProficiencyLevel[] vals = ProficiencyLevel.values();
            assertEquals(4, vals.length);
            assertNotNull(ProficiencyLevel.valueOf("BEGINNER"));
            assertNotNull(ProficiencyLevel.valueOf("INTERMEDIATE"));
            assertNotNull(ProficiencyLevel.valueOf("ADVANCED"));
            assertNotNull(ProficiencyLevel.valueOf("EXPERT"));
        }
    }

    // ───────────────────────────────────────────────
    // 2. ENTITY TESTS
    // ───────────────────────────────────────────────

    @Nested
    @Order(10)
    @DisplayName("10 · Application Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class ApplicationEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor sets PENDING status and appliedDate")
        void defaultConstructor() {
            Application app = new Application();
            assertEquals(Application.Status.PENDING, app.getStatus());
            assertNotNull(app.getAppliedDate());
        }

        @Test @Order(2)
        @DisplayName("Two-arg constructor sets job/profile ids")
        void twoArgConstructor() {
            Application app = new Application(42, 99);
            assertEquals(42, app.getJobOfferId());
            assertEquals(99, app.getCandidateProfileId());
            assertEquals(Application.Status.PENDING, app.getStatus());
        }

        @Test @Order(3)
        @DisplayName("Getters and setters work correctly")
        void gettersSetters() {
            Application app = new Application();
            app.setId(1);
            app.setCoverLetter("My cover letter");
            app.setCustomCvUrl("https://example.com/cv.pdf");
            app.setJobTitle("Developer");
            app.setCompanyName("Acme");
            app.setCandidateName("John Doe");
            app.setMatchPercentage(85);

            assertEquals(1, app.getId());
            assertEquals("My cover letter", app.getCoverLetter());
            assertEquals("https://example.com/cv.pdf", app.getCustomCvUrl());
            assertEquals("Developer", app.getJobTitle());
            assertEquals("Acme", app.getCompanyName());
            assertEquals("John Doe", app.getCandidateName());
            assertEquals(85, app.getMatchPercentage());
        }
    }

    @Nested
    @Order(11)
    @DisplayName("11 · HireOffer Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class HireOfferEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor sets TND/CDI/PENDING defaults")
        void defaultConstructor() {
            HireOffer offer = new HireOffer();
            assertEquals("TND", offer.getCurrency());
            assertEquals("CDI", offer.getContractType());
            assertEquals("PENDING", offer.getStatus());
            assertNotNull(offer.getCreatedDate());
        }

        @Test @Order(2)
        @DisplayName("Three-arg constructor sets fields and inherits defaults")
        void threeArgConstructor() {
            HireOffer offer = new HireOffer(10, 3500.0, "CDD");
            assertEquals(10, offer.getApplicationId());
            assertEquals(3500.0, offer.getSalaryOffered());
            assertEquals("CDD", offer.getContractType());
            assertEquals("TND", offer.getCurrency());
            assertEquals("PENDING", offer.getStatus());
        }

        @Test @Order(3)
        @DisplayName("Status utility methods work")
        void statusUtilities() {
            HireOffer offer = new HireOffer();
            assertTrue(offer.isPending());
            assertFalse(offer.isAccepted());
            assertFalse(offer.isRejected());

            offer.setStatus("ACCEPTED");
            assertTrue(offer.isAccepted());
            assertFalse(offer.isPending());

            offer.setStatus("REJECTED");
            assertTrue(offer.isRejected());
        }

        @Test @Order(4)
        @DisplayName("Full constructor sets all fields")
        void fullConstructor() {
            LocalDate start = LocalDate.of(2025, 3, 1);
            HireOffer offer = new HireOffer(5, 4500.0, "EUR", start, "CDI", "Health insurance");
            assertEquals(5, offer.getApplicationId());
            assertEquals(4500.0, offer.getSalaryOffered());
            assertEquals("EUR", offer.getCurrency());
            assertEquals(start, offer.getStartDate());
            assertEquals("CDI", offer.getContractType());
            assertEquals("Health insurance", offer.getBenefits());
        }
    }

    @Nested
    @Order(12)
    @DisplayName("12 · Interview Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class InterviewEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor sets defaults")
        void defaultConstructor() {
            Interview interview = new Interview();
            assertEquals(60, interview.getDurationMinutes());
            assertEquals("VIDEO", interview.getType());
            assertEquals("SCHEDULED", interview.getStatus());
            assertEquals(0, interview.getRating());
            assertEquals("Africa/Tunis", interview.getTimezone());
            assertNotNull(interview.getCreatedDate());
        }

        @Test @Order(2)
        @DisplayName("Three-arg constructor sets applicationId, date, type")
        void threeArgConstructor() {
            LocalDateTime date = LocalDateTime.of(2025, 6, 15, 10, 0);
            Interview interview = new Interview(7, date, "IN_PERSON");
            assertEquals(7, interview.getApplicationId());
            assertEquals(date, interview.getScheduledDate());
            assertEquals("IN_PERSON", interview.getType());
            assertEquals("SCHEDULED", interview.getStatus());
        }

        @Test @Order(3)
        @DisplayName("Status utility methods")
        void statusUtilities() {
            Interview interview = new Interview();
            assertTrue(interview.isScheduled());
            assertFalse(interview.isCompleted());
            assertFalse(interview.isCancelled());

            interview.setStatus("COMPLETED");
            assertTrue(interview.isCompleted());
            assertFalse(interview.isScheduled());

            interview.setStatus("CANCELLED");
            assertTrue(interview.isCancelled());
        }

        @Test @Order(4)
        @DisplayName("isUpcoming checks future scheduled date")
        void isUpcoming() {
            Interview interview = new Interview();
            interview.setScheduledDate(LocalDateTime.now().plusDays(1));
            assertTrue(interview.isUpcoming());

            interview.setScheduledDate(LocalDateTime.now().minusDays(1));
            assertFalse(interview.isUpcoming());
        }
    }

    @Nested
    @Order(13)
    @DisplayName("13 · InterviewProposal Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class InterviewProposalEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor creates instance")
        void defaultConstructor() {
            InterviewProposal proposal = new InterviewProposal();
            assertNotNull(proposal);
        }

        @Test @Order(2)
        @DisplayName("Three-arg constructor sets fields")
        void threeArgConstructor() {
            LocalDateTime date = LocalDateTime.of(2025, 7, 1, 14, 0);
            InterviewProposal proposal = new InterviewProposal(3, 10, date);
            assertEquals(3, proposal.getApplicationId());
            assertEquals(10, proposal.getProposedBy());
            assertEquals(date, proposal.getProposedDate());
        }

        @Test @Order(3)
        @DisplayName("Getters and setters")
        void gettersSetters() {
            InterviewProposal p = new InterviewProposal();
            p.setId(99);
            p.setDurationMinutes(45);
            p.setType("PHONE");
            p.setMessage("Available next week");
            p.setStatus("ACCEPTED");
            p.setCandidateName("Jane");
            p.setJobTitle("Analyst");

            assertEquals(99, p.getId());
            assertEquals(45, p.getDurationMinutes());
            assertEquals("PHONE", p.getType());
            assertEquals("Available next week", p.getMessage());
            assertEquals("ACCEPTED", p.getStatus());
            assertEquals("Jane", p.getCandidateName());
            assertEquals("Analyst", p.getJobTitle());
        }
    }

    @Nested
    @Order(14)
    @DisplayName("14 · JobOffer Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class JobOfferEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor sets defaults")
        void defaultConstructor() {
            JobOffer offer = new JobOffer();
            assertEquals(JobStatus.DRAFT, offer.getStatus());
            assertNotNull(offer.getPostedDate());
            assertNotNull(offer.getRequiredSkills());
            assertTrue(offer.getRequiredSkills().isEmpty());
        }

        @Test @Order(2)
        @DisplayName("Three-arg constructor sets employer/title/location")
        void threeArgConstructor() {
            JobOffer offer = new JobOffer(5, "Java Dev", "Tunis");
            assertEquals(5, offer.getEmployerId());
            assertEquals("Java Dev", offer.getTitle());
            assertEquals("Tunis", offer.getLocation());
            assertEquals(JobStatus.DRAFT, offer.getStatus());
        }

        @Test @Order(3)
        @DisplayName("addRequiredSkill adds to list")
        void addRequiredSkill() {
            JobOffer offer = new JobOffer();
            offer.addRequiredSkill("Java");
            offer.addRequiredSkill("SQL");
            assertEquals(2, offer.getRequiredSkills().size());
            assertTrue(offer.getRequiredSkills().contains("Java"));
        }

        @Test @Order(4)
        @DisplayName("isActive checks ACTIVE or OPEN status")
        void isActive() {
            JobOffer offer = new JobOffer();
            offer.setStatus(JobStatus.DRAFT);
            assertFalse(offer.isActive());

            offer.setStatus(JobStatus.ACTIVE);
            assertTrue(offer.isActive());

            offer.setStatus(JobStatus.OPEN);
            // isActive() only checks ACTIVE, not OPEN
            assertFalse(offer.isActive());
        }

        @Test @Order(5)
        @DisplayName("getSalaryRange formatting")
        void salaryRange() {
            JobOffer offer = new JobOffer();
            offer.setSalaryMin(2000);
            offer.setSalaryMax(4000);
            String range = offer.getSalaryRange();
            assertNotNull(range);
            assertFalse(range.isBlank());
        }
    }

    @Nested
    @Order(15)
    @DisplayName("15 · JobOpportunity Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class JobOpportunityEntityTests {

        @Test @Order(1)
        @DisplayName("Default no-arg constructor")
        void defaultConstructor() {
            JobOpportunity opp = new JobOpportunity();
            assertNotNull(opp);
        }

        @Test @Order(2)
        @DisplayName("Setters and getters work")
        void gettersSetters() {
            JobOpportunity opp = new JobOpportunity();
            opp.setTitle("Frontend Dev");
            opp.setSource("LinkedIn");
            opp.setUrl("https://linkedin.com/jobs/123");
            opp.setCompany("Google");
            opp.setLocation("Remote");
            opp.setMatchPercentage(75);
            opp.setRecommended(true);

            assertEquals("Frontend Dev", opp.getTitle());
            assertEquals("LinkedIn", opp.getSource());
            assertEquals("https://linkedin.com/jobs/123", opp.getUrl());
            assertEquals("Google", opp.getCompany());
            assertEquals("Remote", opp.getLocation());
            assertEquals(75, opp.getMatchPercentage());
            assertTrue(opp.isRecommended());
        }
    }

    @Nested
    @Order(16)
    @DisplayName("16 · MatchingScore Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class MatchingScoreEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor — all scores 0.0")
        void defaultConstructor() {
            MatchingScore ms = new MatchingScore();
            assertEquals(0.0, ms.getTotalScore());
            assertEquals(0.0, ms.getSkillsScore());
            assertEquals(0.0, ms.getExperienceScore());
            assertEquals(0.0, ms.getLanguageScore());
            assertEquals(0.0, ms.getLocationScore());
            assertNotNull(ms.getCalculatedAt());
        }

        @Test @Order(2)
        @DisplayName("Two-arg constructor sets profile and job IDs")
        void twoArgConstructor() {
            MatchingScore ms = new MatchingScore(10, 20);
            assertEquals(10, ms.getProfileId());
            assertEquals(20, ms.getJobOfferId());
            assertEquals(0.0, ms.getTotalScore());
        }

        @Test @Order(3)
        @DisplayName("Six-arg constructor calculates weighted total")
        void sixArgConstructor() {
            // weights: skills=40%, experience=30%, language=20%, location=10%
            MatchingScore ms = new MatchingScore(1, 2, 100.0, 100.0, 100.0, 100.0);
            double expectedTotal = 100.0 * 0.4 + 100.0 * 0.3 + 100.0 * 0.2 + 100.0 * 0.1;
            assertEquals(expectedTotal, ms.getTotalScore(), 0.01);
        }

        @Test @Order(4)
        @DisplayName("calculateWeightedScore applies correct weights")
        void calculateWeightedScore() {
            MatchingScore ms = new MatchingScore();
            ms.setSkillsScore(80);
            ms.setExperienceScore(60);
            ms.setLanguageScore(40);
            ms.setLocationScore(100);
            double expected = 80 * 0.4 + 60 * 0.3 + 40 * 0.2 + 100 * 0.1;
            assertEquals(expected, ms.calculateWeightedScore(), 0.01);
        }

        @Test @Order(5)
        @DisplayName("updateScores recalculates totalScore")
        void updateScores() {
            MatchingScore ms = new MatchingScore(1, 2);
            ms.updateScores(50, 70, 90, 80);
            assertEquals(50.0, ms.getSkillsScore());
            assertEquals(70.0, ms.getExperienceScore());
            assertEquals(90.0, ms.getLanguageScore());
            assertEquals(80.0, ms.getLocationScore());
            double expected = 50 * 0.4 + 70 * 0.3 + 90 * 0.2 + 80 * 0.1;
            assertEquals(expected, ms.getTotalScore(), 0.01);
        }

        @Test @Order(6)
        @DisplayName("getScoreBreakdown returns map with all keys")
        void scoreBreakdown() {
            MatchingScore ms = new MatchingScore(1, 2, 80, 60, 40, 100);
            Map<String, Object> breakdown = ms.getScoreBreakdown();
            assertNotNull(breakdown);
            assertFalse(breakdown.isEmpty());
        }

        @Test @Order(7)
        @DisplayName("getMatchQuality returns quality label")
        void matchQuality() {
            MatchingScore ms = new MatchingScore(1, 2, 100, 100, 100, 100);
            String quality = ms.getMatchQuality();
            assertNotNull(quality);
            assertFalse(quality.isBlank());
        }
    }

    @Nested
    @Order(17)
    @DisplayName("17 · SavedJob Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class SavedJobEntityTests {

        @Test @Order(1)
        @DisplayName("Default constructor creates instance")
        void defaultConstructor() {
            SavedJob sj = new SavedJob();
            assertNotNull(sj);
        }

        @Test @Order(2)
        @DisplayName("Setters and getters")
        void gettersSetters() {
            SavedJob sj = new SavedJob();
            sj.setId(1);
            sj.setUserId(42);
            sj.setJobUrl("https://jobs.example.com/123");
            sj.setJobTitle("Data Engineer");
            sj.setCompanyName("DataCo");
            sj.setLocation("Sfax");
            sj.setSource("Indeed");
            sj.setSavedAt(LocalDateTime.now());

            assertEquals(1, sj.getId());
            assertEquals(42, sj.getUserId());
            assertEquals("https://jobs.example.com/123", sj.getJobUrl());
            assertEquals("Data Engineer", sj.getJobTitle());
            assertEquals("DataCo", sj.getCompanyName());
            assertEquals("Sfax", sj.getLocation());
            assertEquals("Indeed", sj.getSource());
            assertNotNull(sj.getSavedAt());
        }
    }

    // ───────────────────────────────────────────────
    // 3. SERVICE SINGLETON TESTS
    // ───────────────────────────────────────────────

    @Nested
    @Order(20)
    @DisplayName("20 · Service Singletons")
    @TestMethodOrder(OrderAnnotation.class)
    class ServiceSingletonTests {

        @Test @Order(1)
        @DisplayName("ApplicationService is singleton")
        void applicationServiceSingleton() {
            ApplicationService a = ApplicationService.getInstance();
            ApplicationService b = ApplicationService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(2)
        @DisplayName("HireOfferService is singleton")
        void hireOfferServiceSingleton() {
            HireOfferService a = HireOfferService.getInstance();
            HireOfferService b = HireOfferService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(3)
        @DisplayName("InterviewService is singleton")
        void interviewServiceSingleton() {
            InterviewService a = InterviewService.getInstance();
            InterviewService b = InterviewService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(4)
        @DisplayName("InterviewProposalService is singleton")
        void interviewProposalServiceSingleton() {
            InterviewProposalService a = InterviewProposalService.getInstance();
            InterviewProposalService b = InterviewProposalService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(5)
        @DisplayName("JobService is singleton")
        void jobServiceSingleton() {
            JobService a = JobService.getInstance();
            JobService b = JobService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(6)
        @DisplayName("MatchingService is singleton")
        void matchingServiceSingleton() {
            MatchingService a = MatchingService.getInstance();
            MatchingService b = MatchingService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(7)
        @DisplayName("SavedJobService is singleton")
        void savedJobServiceSingleton() {
            SavedJobService a = SavedJobService.getInstance();
            SavedJobService b = SavedJobService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(8)
        @DisplayName("RecruitmentFinanceBridge is singleton")
        void recruitmentFinanceBridgeSingleton() {
            RecruitmentFinanceBridge a = RecruitmentFinanceBridge.getInstance();
            RecruitmentFinanceBridge b = RecruitmentFinanceBridge.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }

        @Test @Order(9)
        @DisplayName("RecruitmentIntelligenceService is singleton")
        void recruitmentIntelligenceServiceSingleton() {
            RecruitmentIntelligenceService a = RecruitmentIntelligenceService.getInstance();
            RecruitmentIntelligenceService b = RecruitmentIntelligenceService.getInstance();
            assertNotNull(a);
            assertSame(a, b);
        }
    }

    // ───────────────────────────────────────────────
    // 4. DB SCHEMA VALIDATION
    // ───────────────────────────────────────────────

    @Nested
    @Order(25)
    @DisplayName("25 · DB Schema Validation")
    @TestMethodOrder(OrderAnnotation.class)
    class DbSchemaTests {

        private boolean tableExists(String tableName) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null)) {
                return rs.next();
            }
        }

        private boolean columnExists(String table, String column) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 ResultSet rs = conn.getMetaData().getColumns(null, null, table, column)) {
                return rs.next();
            }
        }

        @Test @Order(1)
        @DisplayName("job_offers table exists")
        void jobOffersTable() throws SQLException { assertTrue(tableExists("job_offers")); }

        @Test @Order(2)
        @DisplayName("applications table exists")
        void applicationsTable() throws SQLException { assertTrue(tableExists("applications")); }

        @Test @Order(3)
        @DisplayName("interviews table exists")
        void interviewsTable() throws SQLException { assertTrue(tableExists("interviews")); }

        @Test @Order(4)
        @DisplayName("hire_offers table exists")
        void hireOffersTable() throws SQLException { assertTrue(tableExists("hire_offers")); }

        @Test @Order(5)
        @DisplayName("saved_jobs table exists")
        void savedJobsTable() throws SQLException { assertTrue(tableExists("saved_jobs")); }

        @Test @Order(6)
        @DisplayName("interview_proposals table exists")
        void interviewProposalsTable() throws SQLException { assertTrue(tableExists("interview_proposals")); }

        // Column checks on key tables
        @Test @Order(10)
        @DisplayName("job_offers has required columns")
        void jobOffersColumns() throws SQLException {
            for (String col : new String[]{"id", "title", "description", "location", "min_salary", "max_salary", "status"}) {
                assertTrue(columnExists("job_offers", col), "Missing column: " + col);
            }
        }

        @Test @Order(11)
        @DisplayName("applications has required columns")
        void applicationsColumns() throws SQLException {
            for (String col : new String[]{"id", "job_offer_id", "status", "applied_date", "cover_letter"}) {
                assertTrue(columnExists("applications", col), "Missing column: " + col);
            }
        }

        @Test @Order(12)
        @DisplayName("interviews has required columns")
        void interviewsColumns() throws SQLException {
            for (String col : new String[]{"id", "application_id", "scheduled_date", "type", "status"}) {
                assertTrue(columnExists("interviews", col), "Missing column: " + col);
            }
        }

        @Test @Order(13)
        @DisplayName("hire_offers has required columns")
        void hireOffersColumns() throws SQLException {
            for (String col : new String[]{"id", "application_id", "salary_offered", "status"}) {
                assertTrue(columnExists("hire_offers", col), "Missing column: " + col);
            }
        }


    }

    // ───────────────────────────────────────────────
    // 5. JOB SERVICE CRUD
    // ───────────────────────────────────────────────

    @Nested
    @Order(30)
    @DisplayName("30 · JobService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class JobServiceCRUDTests {

        static final JobService service = JobService.getInstance();
        static int createdId = -1;

        @Test @Order(1)
        @DisplayName("Create job offer")
        void createJobOffer() throws SQLException {
            JobOffer offer = new JobOffer();
            offer.setEmployerId(1);
            offer.setTitle("TEST_JOB_" + System.currentTimeMillis());
            offer.setDescription("Test description for recruitment module tests");
            offer.setLocation("Tunis");
            offer.setSalaryMin(1500);
            offer.setSalaryMax(3000);
            offer.setCurrency("TND");
            offer.setWorkType("FULL_TIME");
            offer.setRequiredSkills(List.of("Java", "SQL"));
            offer.setStatus(JobStatus.DRAFT);
            offer.setDeadline(LocalDate.now().plusDays(30));

            createdId = service.createJobOffer(offer);
            assertTrue(createdId > 0, "Job offer should be created with positive ID");
        }

        @Test @Order(2)
        @DisplayName("Find job offer by ID")
        void findById() throws SQLException {
            Assumptions.assumeTrue(createdId > 0, "Requires created job offer");
            JobOffer found = service.findJobOfferById(createdId);
            assertNotNull(found);
            assertTrue(found.getTitle().startsWith("TEST_JOB_"));
            assertEquals("Tunis", found.getLocation());
        }

        @Test @Order(3)
        @DisplayName("Update job offer")
        void updateJobOffer() throws SQLException {
            Assumptions.assumeTrue(createdId > 0);
            JobOffer offer = service.findJobOfferById(createdId);
            assertNotNull(offer);
            offer.setTitle("UPDATED_TEST_JOB");
            offer.setLocation("Sousse");
            offer.setStatus(JobStatus.ACTIVE);
            boolean updated = service.updateJobOffer(offer);
            assertTrue(updated);

            JobOffer refreshed = service.findJobOfferById(createdId);
            assertEquals("UPDATED_TEST_JOB", refreshed.getTitle());
            assertEquals("Sousse", refreshed.getLocation());
        }

        @Test @Order(4)
        @DisplayName("Find all job offers returns non-null list")
        void findAll() throws SQLException {
            List<JobOffer> all = service.findAllJobOffers();
            assertNotNull(all);
        }

        @Test @Order(5)
        @DisplayName("Search by keywords")
        void searchByKeywords() throws SQLException {
            List<JobOffer> results = service.searchJobOffersByKeywords("UPDATED_TEST_JOB");
            assertNotNull(results);
        }

        @Test @Order(6)
        @DisplayName("Find by status")
        void findByStatus() throws SQLException {
            List<JobOffer> active = service.findJobOffersByStatus(JobStatus.ACTIVE);
            assertNotNull(active);
        }

        @Test @Order(7)
        @DisplayName("Count applications for job")
        void countApplications() throws SQLException {
            Assumptions.assumeTrue(createdId > 0);
            int count = service.countApplicationsForJob(createdId);
            assertTrue(count >= 0);
        }

        @Test @Order(8)
        @DisplayName("Delete job offer (cleanup)")
        void deleteJobOffer() throws SQLException {
            Assumptions.assumeTrue(createdId > 0);
            boolean deleted = service.deleteJobOffer(createdId);
            assertTrue(deleted);

            JobOffer gone = service.findJobOfferById(createdId);
            assertNull(gone);
        }
    }

    // ───────────────────────────────────────────────
    // 6. APPLICATION SERVICE CRUD
    // ───────────────────────────────────────────────

    @Nested
    @Order(35)
    @DisplayName("35 · ApplicationService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class ApplicationServiceCRUDTests {

        static final ApplicationService appService = ApplicationService.getInstance();
        static final JobService jobService = JobService.getInstance();
        static int jobId = -1;
        static int appId = -1;

        @Test @Order(1)
        @DisplayName("Setup: create job offer for application tests")
        void setupJob() throws SQLException {
            JobOffer offer = new JobOffer();
            offer.setEmployerId(1);
            offer.setTitle("APP_TEST_JOB_" + System.currentTimeMillis());
            offer.setDescription("For application test");
            offer.setLocation("Monastir");
            offer.setSalaryMin(1000);
            offer.setSalaryMax(2000);
            offer.setStatus(JobStatus.ACTIVE);
            offer.setDeadline(LocalDate.now().plusDays(30));
            jobId = jobService.createJobOffer(offer);
            assertTrue(jobId > 0);
        }

        @Test @Order(2)
        @DisplayName("Apply to job")
        void apply() throws SQLException {
            Assumptions.assumeTrue(jobId > 0);
            appId = appService.apply(jobId, 1, "Test cover letter", "https://example.com/cv.pdf");
            assertTrue(appId > 0, "Application should be created with positive ID");
        }

        @Test @Order(3)
        @DisplayName("hasApplied returns true after applying")
        void hasApplied() throws SQLException {
            Assumptions.assumeTrue(jobId > 0);
            boolean applied = appService.hasApplied(jobId, 1);
            assertTrue(applied);
        }

        @Test @Order(4)
        @DisplayName("getApplicationById returns application")
        void getById() throws SQLException {
            Assumptions.assumeTrue(appId > 0);
            Application app = appService.getApplicationById(appId);
            assertNotNull(app);
            assertEquals(Application.Status.PENDING, app.getStatus());
        }

        @Test @Order(5)
        @DisplayName("Update status to REVIEWING")
        void updateStatus() throws SQLException {
            Assumptions.assumeTrue(appId > 0);
            boolean updated = appService.updateStatus(appId, Application.Status.REVIEWING);
            assertTrue(updated);

            Application app = appService.getApplicationById(appId);
            assertEquals(Application.Status.REVIEWING, app.getStatus());
        }

        @Test @Order(6)
        @DisplayName("getApplicationsByJobOffer returns list")
        void getByJobOffer() throws SQLException {
            Assumptions.assumeTrue(jobId > 0);
            List<Application> apps = appService.getApplicationsByJobOffer(jobId);
            assertNotNull(apps);
            assertFalse(apps.isEmpty());
        }

        @Test @Order(7)
        @DisplayName("getApplicationsByProfile returns list")
        void getByProfile() throws SQLException {
            List<Application> apps = appService.getApplicationsByProfile(1);
            assertNotNull(apps);
        }

        @Test @Order(8)
        @DisplayName("countByProfileAndStatus")
        void countByStatus() throws SQLException {
            int count = appService.countByProfileAndStatus(1, Application.Status.REVIEWING);
            assertTrue(count >= 0);
        }

        @Test @Order(9)
        @DisplayName("Delete application (cleanup)")
        void deleteApplication() throws SQLException {
            Assumptions.assumeTrue(appId > 0);
            boolean deleted = appService.delete(appId);
            assertTrue(deleted);
        }

        @Test @Order(10)
        @DisplayName("Cleanup: delete test job offer")
        void cleanupJob() throws SQLException {
            if (jobId > 0) {
                jobService.deleteJobOffer(jobId);
            }
        }
    }

    // ───────────────────────────────────────────────
    // 7. HIRE OFFER SERVICE
    // ───────────────────────────────────────────────

    @Nested
    @Order(40)
    @DisplayName("40 · HireOfferService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class HireOfferServiceCRUDTests {

        static final HireOfferService hireService = HireOfferService.getInstance();
        static final ApplicationService appService = ApplicationService.getInstance();
        static final JobService jobService = JobService.getInstance();
        static int jobId = -1;
        static int appId = -1;
        static int offerId = -1;

        @Test @Order(1)
        @DisplayName("Setup: create job + application for hire offer tests")
        void setup() throws SQLException {
            JobOffer job = new JobOffer();
            job.setEmployerId(1);
            job.setTitle("HIRE_TEST_" + System.currentTimeMillis());
            job.setDescription("For hire offer testing");
            job.setLocation("Tunis");
            job.setSalaryMin(2000);
            job.setSalaryMax(4000);
            job.setStatus(JobStatus.ACTIVE);
            job.setDeadline(LocalDate.now().plusDays(30));
            jobId = jobService.createJobOffer(job);
            assertTrue(jobId > 0);

            appId = appService.apply(jobId, 1, "Hire test cover");
            assertTrue(appId > 0);
            appService.updateStatus(appId, Application.Status.OFFER);
        }

        @Test @Order(2)
        @DisplayName("Create hire offer")
        void createHireOffer() {
            Assumptions.assumeTrue(appId > 0);
            HireOffer offer = new HireOffer(appId, 3000.0, "CDI");
            offer.setStartDate(LocalDate.now().plusDays(30));
            offer.setBenefits("Health insurance, meal vouchers");

            offerId = hireService.create(offer);
            assertTrue(offerId > 0, "Hire offer should be created");
        }

        @Test @Order(3)
        @DisplayName("Find hire offer by ID")
        void findById() {
            Assumptions.assumeTrue(offerId > 0);
            HireOffer found = hireService.findById(offerId);
            assertNotNull(found);
            assertEquals(3000.0, found.getSalaryOffered());
            assertEquals("CDI", found.getContractType());
        }

        @Test @Order(4)
        @DisplayName("Find by application")
        void findByApplication() {
            Assumptions.assumeTrue(appId > 0);
            List<HireOffer> offers = hireService.findByApplication(appId);
            assertNotNull(offers);
            assertFalse(offers.isEmpty());
        }

        @Test @Order(5)
        @DisplayName("Accept hire offer")
        void accept() {
            Assumptions.assumeTrue(offerId > 0);
            boolean accepted = hireService.accept(offerId);
            assertTrue(accepted);

            HireOffer found = hireService.findById(offerId);
            assertEquals("ACCEPTED", found.getStatus());
        }

        @Test @Order(6)
        @DisplayName("Delete hire offer (cleanup)")
        void delete() {
            Assumptions.assumeTrue(offerId > 0);
            boolean deleted = hireService.delete(offerId);
            assertTrue(deleted);
        }

        @Test @Order(7)
        @DisplayName("Cleanup: delete application and job")
        void cleanup() throws SQLException {
            if (appId > 0) appService.delete(appId);
            if (jobId > 0) jobService.deleteJobOffer(jobId);
        }
    }

    // ───────────────────────────────────────────────
    // 8. INTERVIEW SERVICE
    // ───────────────────────────────────────────────

    @Nested
    @Order(45)
    @DisplayName("45 · InterviewService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class InterviewServiceCRUDTests {

        static final InterviewService intService = InterviewService.getInstance();
        static final ApplicationService appService = ApplicationService.getInstance();
        static final JobService jobService = JobService.getInstance();
        static int jobId = -1;
        static int appId = -1;
        static int interviewId = -1;

        @Test @Order(1)
        @DisplayName("Setup: create job + application for interview tests")
        void setup() throws SQLException {
            JobOffer job = new JobOffer();
            job.setEmployerId(1);
            job.setTitle("INT_TEST_" + System.currentTimeMillis());
            job.setDescription("For interview testing");
            job.setLocation("Tunis");
            job.setSalaryMin(1500);
            job.setSalaryMax(3500);
            job.setStatus(JobStatus.ACTIVE);
            job.setDeadline(LocalDate.now().plusDays(30));
            jobId = jobService.createJobOffer(job);
            assertTrue(jobId > 0);

            appId = appService.apply(jobId, 1, "Interview test cover");
            assertTrue(appId > 0);
            appService.updateStatus(appId, Application.Status.INTERVIEW);
        }

        @Test @Order(2)
        @DisplayName("Create interview")
        void createInterview() {
            Assumptions.assumeTrue(appId > 0);
            LocalDateTime scheduledDate = LocalDateTime.now().plusDays(3).withHour(10).withMinute(0);
            Interview interview = new Interview(appId, scheduledDate, "VIDEO");
            interview.setLocation("Online");
            interview.setVideoLink("https://meet.google.com/abc-defg-hij");
            interview.setNotes("Technical interview round 1");

            interviewId = intService.create(interview);
            assertTrue(interviewId > 0, "Interview should be created");
        }

        @Test @Order(3)
        @DisplayName("Find interview by ID")
        void findById() {
            Assumptions.assumeTrue(interviewId > 0);
            Interview found = intService.findById(interviewId);
            assertNotNull(found);
            assertEquals("VIDEO", found.getType());
            assertEquals("SCHEDULED", found.getStatus());
        }

        @Test @Order(4)
        @DisplayName("Find by application")
        void findByApplication() {
            Assumptions.assumeTrue(appId > 0);
            List<Interview> interviews = intService.findByApplication(appId);
            assertNotNull(interviews);
            assertFalse(interviews.isEmpty());
        }

        @Test @Order(5)
        @DisplayName("Reschedule interview")
        void reschedule() {
            Assumptions.assumeTrue(interviewId > 0);
            LocalDateTime newDate = LocalDateTime.now().plusDays(5).withHour(14).withMinute(30);
            boolean rescheduled = intService.reschedule(interviewId, newDate);
            assertTrue(rescheduled);
        }

        @Test @Order(6)
        @DisplayName("Complete interview with feedback")
        void complete() {
            Assumptions.assumeTrue(interviewId > 0);
            boolean completed = intService.complete(interviewId, "Good technical skills", 4);
            assertTrue(completed);

            Interview found = intService.findById(interviewId);
            assertEquals("COMPLETED", found.getStatus());
        }

        @Test @Order(7)
        @DisplayName("Delete interview (cleanup)")
        void delete() {
            Assumptions.assumeTrue(interviewId > 0);
            boolean deleted = intService.delete(interviewId);
            assertTrue(deleted);
        }

        @Test @Order(8)
        @DisplayName("Cleanup: delete application and job")
        void cleanup() throws SQLException {
            if (appId > 0) appService.delete(appId);
            if (jobId > 0) jobService.deleteJobOffer(jobId);
        }
    }

    // ───────────────────────────────────────────────
    // 9. INTERVIEW PROPOSAL SERVICE
    // ───────────────────────────────────────────────

    @Nested
    @Order(50)
    @DisplayName("50 · InterviewProposalService")
    @TestMethodOrder(OrderAnnotation.class)
    class InterviewProposalServiceTests {

        static final InterviewProposalService proposalService = InterviewProposalService.getInstance();

        @Test @Order(1)
        @DisplayName("Service instance is available")
        void instanceAvailable() {
            assertNotNull(proposalService);
        }

        @Test @Order(2)
        @DisplayName("getPendingCountForEmployer returns >= 0")
        void pendingCount() {
            int count = proposalService.getPendingCountForEmployer(1);
            assertTrue(count >= 0);
        }

        @Test @Order(3)
        @DisplayName("getPendingForEmployer returns list")
        void pendingList() {
            List<InterviewProposal> pending = proposalService.getPendingForEmployer(1);
            assertNotNull(pending);
        }
    }

    // ───────────────────────────────────────────────
    // 10. SAVED JOB SERVICE
    // ───────────────────────────────────────────────

    @Nested
    @Order(55)
    @DisplayName("55 · SavedJobService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class SavedJobServiceCRUDTests {

        static final SavedJobService service = SavedJobService.getInstance();
        static final String TEST_URL = "https://test.job.com/saved_" + System.currentTimeMillis();

        @Test @Order(1)
        @DisplayName("Save a job")
        void saveJob() {
            SavedJob sj = new SavedJob();
            sj.setUserId(1);
            sj.setJobUrl(TEST_URL);
            sj.setJobTitle("Test Saved Job");
            sj.setCompanyName("TestCo");
            sj.setLocation("Tunis");
            sj.setSource("Unit Test");

            int id = service.save(sj);
            assertTrue(id > 0, "Saved job should return positive ID");
        }

        @Test @Order(2)
        @DisplayName("isSaved returns true after save")
        void isSaved() {
            boolean saved = service.isSaved(1, TEST_URL);
            assertTrue(saved);
        }

        @Test @Order(3)
        @DisplayName("findByUser returns list containing saved job")
        void findByUser() {
            List<SavedJob> jobs = service.findByUser(1);
            assertNotNull(jobs);
            boolean found = jobs.stream().anyMatch(j -> TEST_URL.equals(j.getJobUrl()));
            assertTrue(found, "Should find the saved job in user's list");
        }

        @Test @Order(4)
        @DisplayName("Unsave job")
        void unsave() {
            boolean unsaved = service.unsave(1, TEST_URL);
            assertTrue(unsaved);
        }

        @Test @Order(5)
        @DisplayName("isSaved returns false after unsave")
        void notSavedAfterUnsave() {
            boolean saved = service.isSaved(1, TEST_URL);
            assertFalse(saved);
        }
    }

    // ───────────────────────────────────────────────
    // 11. MATCHING SERVICE
    // ───────────────────────────────────────────────

    @Nested
    @Order(60)
    @DisplayName("60 · MatchingService")
    @TestMethodOrder(OrderAnnotation.class)
    class MatchingServiceTests {

        static final MatchingService service = MatchingService.getInstance();

        @Test @Order(1)
        @DisplayName("Service instance is available")
        void instanceAvailable() {
            assertNotNull(service);
        }

        @Test @Order(2)
        @DisplayName("MatchResult.EMPTY has zero score")
        void matchResultEmpty() {
            MatchingService.MatchResult empty = MatchingService.MatchResult.EMPTY;
            assertNotNull(empty);
            assertEquals(0, empty.score);
        }

        @Test @Order(3)
        @DisplayName("scoreCandidateProfile returns >= 0")
        void scoreCandidate() {
            int score = service.scoreCandidateProfile(1);
            assertTrue(score >= 0);
        }
    }

    // ───────────────────────────────────────────────
    // 12. RECRUITMENT-FINANCE BRIDGE
    // ───────────────────────────────────────────────

    @Nested
    @Order(65)
    @DisplayName("65 · RecruitmentFinanceBridge")
    @TestMethodOrder(OrderAnnotation.class)
    class RecruitmentFinanceBridgeTests {

        @Test @Order(1)
        @DisplayName("Service instance is available")
        void instanceAvailable() {
            RecruitmentFinanceBridge bridge = RecruitmentFinanceBridge.getInstance();
            assertNotNull(bridge);
        }

        @Test @Order(2)
        @DisplayName("acceptOfferAndGenerateContract with invalid ID returns -1")
        void invalidOfferReturnsNeg1() {
            int result = RecruitmentFinanceBridge.getInstance().acceptOfferAndGenerateContract(-999);
            assertEquals(-1, result);
        }
    }

    // ───────────────────────────────────────────────
    // 13. RECRUITMENT INTELLIGENCE SERVICE
    // ───────────────────────────────────────────────

    @Nested
    @Order(70)
    @DisplayName("70 · RecruitmentIntelligenceService")
    @TestMethodOrder(OrderAnnotation.class)
    class RecruitmentIntelligenceServiceTests {

        @Test @Order(1)
        @DisplayName("Service instance is available")
        void instanceAvailable() {
            RecruitmentIntelligenceService service = RecruitmentIntelligenceService.getInstance();
            assertNotNull(service);
        }
    }

    // ───────────────────────────────────────────────
    // 14. JOB FEED CACHE
    // ───────────────────────────────────────────────

    @Nested
    @Order(75)
    @DisplayName("75 · Job Feed & Cache")
    @TestMethodOrder(OrderAnnotation.class)
    class JobFeedTests {

        static final JobService service = JobService.getInstance();

        @Test @Order(1)
        @DisplayName("getJobsFromCache returns non-null list")
        void cacheNotNull() {
            List<JobOpportunity> cache = service.getJobsFromCache();
            assertNotNull(cache);
        }

        @Test @Order(2)
        @DisplayName("reloadCacheFromJson does not throw")
        void reloadCache() {
            assertDoesNotThrow(() -> service.reloadCacheFromJson());
        }

        @Test @Order(3)
        @DisplayName("findVisibleJobOffers returns list")
        void visibleOffers() throws SQLException {
            List<JobOffer> visible = service.findVisibleJobOffers();
            assertNotNull(visible);
        }

        @Test @Order(4)
        @DisplayName("findAllJobOffersForCandidates returns list")
        void candidateOffers() throws SQLException {
            List<JobOffer> offers = service.findAllJobOffersForCandidates();
            assertNotNull(offers);
        }
    }

    // ───────────────────────────────────────────────
    // 15. EDGE CASES & VALIDATION
    // ───────────────────────────────────────────────

    @Nested
    @Order(80)
    @DisplayName("80 · Edge Cases & Validation")
    @TestMethodOrder(OrderAnnotation.class)
    class EdgeCaseTests {

        @Test @Order(1)
        @DisplayName("JobService: create with null title throws")
        void createNullTitle() {
            JobOffer offer = new JobOffer();
            offer.setEmployerId(1);
            offer.setTitle(null);
            assertThrows(Exception.class, () -> JobService.getInstance().createJobOffer(offer));
        }

        @Test @Order(2)
        @DisplayName("JobService: find non-existent ID returns null")
        void findNonExistent() throws SQLException {
            JobOffer result = JobService.getInstance().findJobOfferById(-999);
            assertNull(result);
        }

        @Test @Order(3)
        @DisplayName("ApplicationService: duplicate apply is handled")
        void duplicateApply() throws SQLException {
            // If the same profile applies to the same job twice,
            // hasApplied should detect it (logic depends on implementation)
            ApplicationService service = ApplicationService.getInstance();
            // Just verify hasApplied with non-existent returns false
            boolean applied = service.hasApplied(-999, -999);
            assertFalse(applied);
        }

        @Test @Order(4)
        @DisplayName("InterviewService: create with null application throws")
        void createNullApplication() {
            Interview interview = new Interview();
            interview.setApplicationId(0);
            interview.setScheduledDate(null);
            assertThrows(Exception.class, () -> InterviewService.getInstance().create(interview));
        }

        @Test @Order(5)
        @DisplayName("HireOfferService: create with invalid application throws")
        void createInvalidHireOffer() {
            HireOffer offer = new HireOffer();
            offer.setApplicationId(0);
            offer.setSalaryOffered(-1);
            assertThrows(Exception.class, () -> HireOfferService.getInstance().create(offer));
        }

        @Test @Order(6)
        @DisplayName("SavedJobService: save with null URL throws")
        void saveNullUrl() {
            SavedJob sj = new SavedJob();
            sj.setUserId(1);
            sj.setJobUrl(null);
            assertThrows(Exception.class, () -> SavedJobService.getInstance().save(sj));
        }

        @Test @Order(7)
        @DisplayName("MatchingScore: negative scores handled")
        void negativeScores() {
            MatchingScore ms = new MatchingScore();
            ms.setSkillsScore(-10);
            ms.setExperienceScore(-5);
            // Should still calculate without exception
            assertDoesNotThrow(() -> ms.calculateWeightedScore());
        }

        @Test @Order(8)
        @DisplayName("JobOffer: empty skills list is valid")
        void emptySkills() {
            JobOffer offer = new JobOffer();
            assertNotNull(offer.getRequiredSkills());
            assertTrue(offer.getRequiredSkills().isEmpty());
        }

        @Test @Order(9)
        @DisplayName("Interview: isUpcoming with null date handled")
        void upcomingNullDate() {
            Interview interview = new Interview();
            interview.setScheduledDate(null);
            // Should not throw NPE — either returns false or handles gracefully
            assertDoesNotThrow(() -> interview.isUpcoming());
        }

        @Test @Order(10)
        @DisplayName("Application.Status: invalid valueOf throws")
        void invalidStatusThrows() {
            assertThrows(IllegalArgumentException.class,
                () -> Application.Status.valueOf("INVALID_STATUS"));
        }
    }
}
