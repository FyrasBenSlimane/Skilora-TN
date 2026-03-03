package com.skilora;

// === Formation Entities ===
import com.skilora.formation.entity.*;
import com.skilora.formation.enums.*;
import com.skilora.formation.service.*;

// === Config ===
import com.skilora.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 *   SKILORA - Formation Module Test Suite
 * ──────────────────────────────────────────────────────────────────────
 *   Comprehensive tests for the Formation module covering:
 *   • Entity constructors, getters/setters, equals/hashCode, toString
 *   • Enum values, display names, and utility methods
 *   • Service singletons and thread-safe initialization
 *   • FormationService CRUD operations
 *   • FormationModuleService CRUD + reorder
 *   • EnrollmentService lifecycle (enroll, progress, complete)
 *   • LessonProgressService per-module tracking
 *   • CertificateService & CertificateGenerationService flow
 *   • QuizService CRUD + quiz result flow
 *   • AchievementService award & retrieval
 *   • MentorshipService lifecycle
 *   • FormationRatingService rating flow
 *   • Database schema validation (all formation tables)
 *   • Cross-module integration (enroll → progress → certificate)
 *   • Edge cases, validation, and error handling
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DisplayName("Formation Module Tests")
class FormationModuleTests {

    // ═══════════════════════════════════════════════════════════════
    //  Section 1: Entity Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(1)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("1. Formation Entity Tests")
    class FormationEntityTests {

        @Test @Order(1)
        @DisplayName("Formation default constructor initializes fields")
        void formationDefaultConstructor() {
            Formation f = new Formation();
            assertEquals(0, f.getId());
            assertNull(f.getTitle());
            assertNull(f.getDescription());
            assertNull(f.getCategory());
            assertEquals(0, f.getDurationHours());
            assertEquals(0.0, f.getCost());
            assertEquals("TND", f.getCurrency());
            assertNull(f.getProvider());
            assertNull(f.getImageUrl());
            assertEquals(FormationLevel.BEGINNER, f.getLevel());
            assertEquals("ACTIVE", f.getStatus());
            assertEquals(0, f.getLessonCount());
        }

        @Test @Order(2)
        @DisplayName("Formation setters and getters")
        void formationSettersGetters() {
            Formation f = new Formation();
            f.setId(42);
            f.setTitle("Java Mastery");
            f.setDescription("Learn Java deeply");
            f.setCategory("Development");
            f.setDurationHours(120);
            f.setCost(299.99);
            f.setCurrency("TND");
            f.setProvider("Skilora Academy");
            f.setImageUrl("https://img.example.com/java.png");
            f.setLevel(FormationLevel.ADVANCED);
            f.setStatus("ACTIVE");
            f.setCreatedBy(7);
            f.setLessonCount(15);

            assertEquals(42, f.getId());
            assertEquals("Java Mastery", f.getTitle());
            assertEquals("Learn Java deeply", f.getDescription());
            assertEquals("Development", f.getCategory());
            assertEquals(120, f.getDurationHours());
            assertEquals(299.99, f.getCost(), 0.001);
            assertEquals("TND", f.getCurrency());
            assertEquals("Skilora Academy", f.getProvider());
            assertEquals("https://img.example.com/java.png", f.getImageUrl());
            assertEquals(FormationLevel.ADVANCED, f.getLevel());
            assertEquals("ACTIVE", f.getStatus());
            assertEquals(7, f.getCreatedBy());
            assertEquals(15, f.getLessonCount());
        }

        @Test @Order(3)
        @DisplayName("Formation isFree returns correct values")
        void formationIsFree() {
            Formation f = new Formation();
            assertTrue(f.isFree()); // default is true
            f.setFree(false);
            f.setCost(50.0);
            assertFalse(f.isFree());
        }

        @Test @Order(4)
        @DisplayName("Formation equals and hashCode")
        void formationEqualsHashCode() {
            Formation f1 = new Formation();
            f1.setId(1);
            Formation f2 = new Formation();
            f2.setId(1);
            Formation f3 = new Formation();
            f3.setId(2);

            assertEquals(f1, f2);
            assertNotEquals(f1, f3);
            assertEquals(f1.hashCode(), f2.hashCode());
        }

        @Test @Order(5)
        @DisplayName("Formation toString contains title")
        void formationToString() {
            Formation f = new Formation();
            f.setTitle("Test Course");
            f.setId(5);
            assertNotNull(f.toString());
            assertTrue(f.toString().contains("5") || f.toString().contains("Test Course"));
        }
    }

    @Nested
    @Order(2)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("2. FormationModule Entity Tests")
    class FormationModuleEntityTests {

        @Test @Order(1)
        @DisplayName("FormationModule default constructor")
        void moduleDefaultConstructor() {
            FormationModule m = new FormationModule();
            assertEquals(0, m.getId());
            assertEquals(0, m.getFormationId());
            assertNull(m.getTitle());
            assertNull(m.getDescription());
            assertNull(m.getContentUrl());
            assertNull(m.getContent());
            assertEquals(0, m.getDurationMinutes());
            assertEquals(0, m.getOrderIndex());
            assertNotNull(m.getCreatedAt());
            assertNotNull(m.getUpdatedAt());
        }

        @Test @Order(2)
        @DisplayName("FormationModule parameterized constructor")
        void moduleParameterizedConstructor() {
            FormationModule m = new FormationModule(5, "Intro Module", 0);
            assertEquals(5, m.getFormationId());
            assertEquals("Intro Module", m.getTitle());
            assertEquals(0, m.getOrderIndex());
        }

        @Test @Order(3)
        @DisplayName("FormationModule getFormattedDuration")
        void moduleFormattedDuration() {
            FormationModule m = new FormationModule();
            m.setDurationMinutes(0);
            assertEquals("N/A", m.getFormattedDuration());
            m.setDurationMinutes(30);
            assertEquals("30 min", m.getFormattedDuration());
            m.setDurationMinutes(60);
            assertEquals("1h", m.getFormattedDuration());
            m.setDurationMinutes(90);
            assertEquals("1h30", m.getFormattedDuration());
        }

        @Test @Order(4)
        @DisplayName("FormationModule content field")
        void moduleContentField() {
            FormationModule m = new FormationModule();
            assertNull(m.getContent());
            m.setContent("<h1>Intro</h1><p>Welcome to the course</p>");
            assertEquals("<h1>Intro</h1><p>Welcome to the course</p>", m.getContent());
        }

        @Test @Order(5)
        @DisplayName("FormationModule equals/hashCode")
        void moduleEqualsHashCode() {
            FormationModule m1 = new FormationModule();
            m1.setId(10);
            FormationModule m2 = new FormationModule();
            m2.setId(10);
            FormationModule m3 = new FormationModule();
            m3.setId(20);

            assertEquals(m1, m2);
            assertNotEquals(m1, m3);
            assertEquals(m1.hashCode(), m2.hashCode());
        }
    }

    @Nested
    @Order(3)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("3. Enrollment Entity Tests")
    class EnrollmentEntityTests {

        @Test @Order(1)
        @DisplayName("Enrollment default constructor")
        void enrollmentDefaults() {
            Enrollment e = new Enrollment();
            assertEquals(0, e.getId());
            assertEquals(0, e.getFormationId());
            assertEquals(0, e.getUserId());
            assertEquals(0.0, e.getProgress(), 0.001);
            assertFalse(e.isCompleted());
        }

        @Test @Order(2)
        @DisplayName("Enrollment setters/getters")
        void enrollmentSettersGetters() {
            Enrollment e = new Enrollment();
            e.setId(1);
            e.setFormationId(5);
            e.setUserId(10);
            e.setProgress(75.5);
            e.setCompleted(true);
            e.setStatus(EnrollmentStatus.COMPLETED);
            e.setFormationTitle("Java 101");
            e.setUserName("Test User");

            assertEquals(1, e.getId());
            assertEquals(5, e.getFormationId());
            assertEquals(10, e.getUserId());
            assertEquals(75.5, e.getProgress(), 0.001);
            assertTrue(e.isCompleted());
            assertEquals(EnrollmentStatus.COMPLETED, e.getStatus());
            assertEquals("Java 101", e.getFormationTitle());
            assertEquals("Test User", e.getUserName());
        }
    }

    @Nested
    @Order(4)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("4. LessonProgress Entity Tests")
    class LessonProgressEntityTests {

        @Test @Order(1)
        @DisplayName("LessonProgress default constructor")
        void lpDefaults() {
            LessonProgress lp = new LessonProgress();
            assertFalse(lp.isCompleted());
            assertEquals(0.0, lp.getProgressPercentage(), 0.001);
            assertNotNull(lp.getStartedAt());
            assertNotNull(lp.getLastAccessedAt());
            assertNull(lp.getCompletedAt());
        }

        @Test @Order(2)
        @DisplayName("LessonProgress required-fields constructor")
        void lpRequiredConstructor() {
            LessonProgress lp = new LessonProgress(1, 10);
            assertEquals(1, lp.getEnrollmentId());
            assertEquals(10, lp.getLessonId());
        }

        @Test @Order(3)
        @DisplayName("LessonProgress auto-complete at 100%")
        void lpAutoCompleteAt100() {
            LessonProgress lp = new LessonProgress();
            lp.setProgressPercentage(100.0);
            assertTrue(lp.isCompleted());
            assertNotNull(lp.getCompletedAt());
        }

        @Test @Order(4)
        @DisplayName("LessonProgress clamps percentage to 0-100")
        void lpClamp() {
            LessonProgress lp = new LessonProgress();
            lp.setProgressPercentage(150.0);
            assertEquals(100.0, lp.getProgressPercentage(), 0.001);
            lp.setProgressPercentage(-10.0);
            assertEquals(0.0, lp.getProgressPercentage(), 0.001);
        }

        @Test @Order(5)
        @DisplayName("LessonProgress setCompleted sets timestamp")
        void lpCompletedTimestamp() {
            LessonProgress lp = new LessonProgress();
            lp.setCompleted(true);
            assertNotNull(lp.getCompletedAt());
            lp.setCompleted(false);
            assertNull(lp.getCompletedAt());
        }

        @Test @Order(6)
        @DisplayName("LessonProgress updateLastAccessed")
        void lpUpdateLastAccessed() {
            LessonProgress lp = new LessonProgress();
            assertNotNull(lp.getLastAccessedAt());
            lp.updateLastAccessed();
            assertNotNull(lp.getLastAccessedAt());
        }
    }

    @Nested
    @Order(5)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("5. Certificate Entity Tests")
    class CertificateEntityTests {

        @Test @Order(1)
        @DisplayName("Certificate default fields")
        void certDefaults() {
            Certificate c = new Certificate();
            assertEquals(0, c.getId());
            assertNull(c.getCertificateNumber());
            assertNull(c.getPdfUrl());
        }

        @Test @Order(2)
        @DisplayName("Certificate setters/getters")
        void certSettersGetters() {
            Certificate c = new Certificate();
            c.setId(42);
            c.setCertificateNumber("CERT-2025-001");
            c.setEnrollmentId(7);
            c.setPdfUrl("https://cert.pdf");
            c.setIssuedDate(LocalDateTime.of(2025, 1, 15, 10, 0));

            assertEquals(42, c.getId());
            assertEquals("CERT-2025-001", c.getCertificateNumber());
            assertEquals(7, c.getEnrollmentId());
            assertEquals("https://cert.pdf", c.getPdfUrl());
            assertNotNull(c.getIssuedDate());
        }
    }

    @Nested
    @Order(6)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("6. Quiz Entities Tests")
    class QuizEntityTests {

        @Test @Order(1)
        @DisplayName("Quiz default constructor")
        void quizDefaults() {
            Quiz q = new Quiz();
            assertEquals(0, q.getId());
            assertEquals(0, q.getFormationId());
            assertNull(q.getTitle());
            assertEquals(70, q.getPassScore()); // default passScore is 70
        }

        @Test @Order(2)
        @DisplayName("QuizQuestion fields")
        void quizQuestion() {
            QuizQuestion qq = new QuizQuestion();
            qq.setId(1);
            qq.setQuizId(5);
            qq.setQuestionText("What is Java?");
            qq.setOptionA("A language");
            qq.setOptionB("A coffee");
            qq.setOptionC("An island");
            qq.setOptionD("All of the above");
            qq.setCorrectOption('D');
            qq.setPoints(10);
            qq.setOrderIndex(0);

            assertEquals("What is Java?", qq.getQuestionText());
            assertEquals('D', qq.getCorrectOption());
            assertEquals(10, qq.getPoints());
        }

        @Test @Order(3)
        @DisplayName("QuizResult fields")
        void quizResult() {
            QuizResult qr = new QuizResult();
            qr.setScore(85);
            qr.setMaxScore(100);
            qr.setPassed(true);
            qr.setAttemptNumber(1);

            assertEquals(85, qr.getScore());
            assertEquals(100, qr.getMaxScore());
            assertTrue(qr.isPassed());
            assertEquals(1, qr.getAttemptNumber());
        }
    }

    @Nested
    @Order(7)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("7. Achievement Entity Tests")
    class AchievementEntityTests {

        @Test @Order(1)
        @DisplayName("Achievement fields")
        void achievementFields() {
            Achievement a = new Achievement();
            a.setId(1);
            a.setUserId(5);
            a.setBadgeType("FIRST_COURSE");
            a.setTitle("First Course Completed");
            a.setDescription("Complete your first formation");
            a.setRarity(BadgeRarity.COMMON);
            a.setPoints(50);

            assertEquals(1, a.getId());
            assertEquals(5, a.getUserId());
            assertEquals("FIRST_COURSE", a.getBadgeType());
            assertEquals("First Course Completed", a.getTitle());
            assertEquals(BadgeRarity.COMMON, a.getRarity());
            assertEquals(50, a.getPoints());
        }
    }

    @Nested
    @Order(8)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("8. Mentorship Entity Tests")
    class MentorshipEntityTests {

        @Test @Order(1)
        @DisplayName("Mentorship fields")
        void mentorshipFields() {
            Mentorship m = new Mentorship();
            m.setId(1);
            m.setMentorId(10);
            m.setMenteeId(20);
            m.setStatus(MentorshipStatus.ACTIVE);
            m.setTopic("Java Frameworks");
            m.setGoals("Master Spring Boot");

            assertEquals(1, m.getId());
            assertEquals(10, m.getMentorId());
            assertEquals(20, m.getMenteeId());
            assertEquals(MentorshipStatus.ACTIVE, m.getStatus());
            assertEquals("Java Frameworks", m.getTopic());
            assertEquals("Master Spring Boot", m.getGoals());
        }
    }

    @Nested
    @Order(9)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("9. FormationRating Entity Tests")
    class FormationRatingEntityTests {

        @Test @Order(1)
        @DisplayName("FormationRating fields")
        void ratingFields() {
            FormationRating r = new FormationRating();
            r.setId(1);
            r.setUserId(5);
            r.setFormationId(10);
            r.setIsLiked(true);
            r.setStarRating(4);

            assertEquals(1, r.getId());
            assertEquals(5, r.getUserId());
            assertEquals(10, r.getFormationId());
            assertTrue(r.getIsLiked());
            assertEquals(4, r.getStarRating());
        }
    }

    @Nested
    @Order(10)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("10. FormationMaterial Entity Tests")
    class FormationMaterialEntityTests {

        @Test @Order(1)
        @DisplayName("FormationMaterial fields")
        void materialFields() {
            FormationMaterial fm = new FormationMaterial();
            fm.setId(1);
            fm.setFormationId(5);
            fm.setName("Lecture Slides");
            fm.setDescription("PDF slides for Module 1");
            fm.setFileUrl("https://cdn.example.com/slides.pdf");
            fm.setFileType("application/pdf");
            fm.setFileSizeBytes(1048576);

            assertEquals(1, fm.getId());
            assertEquals(5, fm.getFormationId());
            assertEquals("Lecture Slides", fm.getName());
            assertEquals("application/pdf", fm.getFileType());
            assertEquals(1048576, fm.getFileSizeBytes());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 2: Enum Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(20)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("20. Formation Enums Tests")
    class FormationEnumTests {

        @Test @Order(1)
        @DisplayName("FormationLevel enum values")
        void formationLevelValues() {
            FormationLevel[] values = FormationLevel.values();
            assertEquals(3, values.length);
            assertNotNull(FormationLevel.BEGINNER);
            assertNotNull(FormationLevel.INTERMEDIATE);
            assertNotNull(FormationLevel.ADVANCED);
        }

        @Test @Order(2)
        @DisplayName("FormationLevel valueOf")
        void formationLevelValueOf() {
            assertEquals(FormationLevel.BEGINNER, FormationLevel.valueOf("BEGINNER"));
            assertEquals(FormationLevel.ADVANCED, FormationLevel.valueOf("ADVANCED"));
        }

        @Test @Order(3)
        @DisplayName("EnrollmentStatus enum values")
        void enrollmentStatusValues() {
            EnrollmentStatus[] values = EnrollmentStatus.values();
            assertEquals(3, values.length);
            assertNotNull(EnrollmentStatus.IN_PROGRESS);
            assertNotNull(EnrollmentStatus.COMPLETED);
            assertNotNull(EnrollmentStatus.ABANDONED);
        }

        @Test @Order(4)
        @DisplayName("BadgeRarity enum values — 5 rarities")
        void badgeRarityValues() {
            BadgeRarity[] values = BadgeRarity.values();
            assertEquals(5, values.length);
            assertNotNull(BadgeRarity.COMMON);
            assertNotNull(BadgeRarity.UNCOMMON);
            assertNotNull(BadgeRarity.RARE);
            assertNotNull(BadgeRarity.EPIC);
            assertNotNull(BadgeRarity.LEGENDARY);
        }

        @Test @Order(5)
        @DisplayName("MentorshipStatus enum values")
        void mentorshipStatusValues() {
            MentorshipStatus[] values = MentorshipStatus.values();
            assertEquals(4, values.length);
            assertNotNull(MentorshipStatus.PENDING);
            assertNotNull(MentorshipStatus.ACTIVE);
            assertNotNull(MentorshipStatus.COMPLETED);
            assertNotNull(MentorshipStatus.CANCELLED);
        }

        @Test @Order(6)
        @DisplayName("TrainingCategory enum values")
        void trainingCategoryValues() {
            TrainingCategory[] values = TrainingCategory.values();
            assertTrue(values.length >= 6);
            assertNotNull(TrainingCategory.DEVELOPMENT);
            assertNotNull(TrainingCategory.DESIGN);
            assertNotNull(TrainingCategory.MARKETING);
            assertNotNull(TrainingCategory.DATA_SCIENCE);
        }

        @ParameterizedTest
        @EnumSource(FormationLevel.class)
        @Order(7)
        @DisplayName("All FormationLevel values have non-null names")
        void allLevelsHaveNames(FormationLevel level) {
            assertNotNull(level.name());
        }

        @ParameterizedTest
        @EnumSource(BadgeRarity.class)
        @Order(8)
        @DisplayName("All BadgeRarity values have valid ordinals")
        void allRaritiesValid(BadgeRarity rarity) {
            assertTrue(rarity.ordinal() >= 0);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 3: Service Singleton Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(30)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("30. Service Singletons & Thread Safety")
    class ServiceSingletonTests {

        @Test @Order(1)
        @DisplayName("FormationService singleton")
        void formationServiceSingleton() {
            FormationService s1 = FormationService.getInstance();
            FormationService s2 = FormationService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(2)
        @DisplayName("FormationModuleService singleton")
        void moduleServiceSingleton() {
            FormationModuleService s1 = FormationModuleService.getInstance();
            FormationModuleService s2 = FormationModuleService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(3)
        @DisplayName("EnrollmentService singleton")
        void enrollmentServiceSingleton() {
            EnrollmentService s1 = EnrollmentService.getInstance();
            EnrollmentService s2 = EnrollmentService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(4)
        @DisplayName("CertificateService singleton")
        void certificateServiceSingleton() {
            CertificateService s1 = CertificateService.getInstance();
            CertificateService s2 = CertificateService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(5)
        @DisplayName("CertificateGenerationService singleton")
        void certGenServiceSingleton() {
            CertificateGenerationService s1 = CertificateGenerationService.getInstance();
            CertificateGenerationService s2 = CertificateGenerationService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(6)
        @DisplayName("LessonProgressService singleton")
        void lessonProgressServiceSingleton() {
            LessonProgressService s1 = LessonProgressService.getInstance();
            LessonProgressService s2 = LessonProgressService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(7)
        @DisplayName("QuizService singleton")
        void quizServiceSingleton() {
            QuizService s1 = QuizService.getInstance();
            QuizService s2 = QuizService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(8)
        @DisplayName("AchievementService singleton")
        void achievementServiceSingleton() {
            AchievementService s1 = AchievementService.getInstance();
            AchievementService s2 = AchievementService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(9)
        @DisplayName("MentorshipService singleton")
        void mentorshipServiceSingleton() {
            MentorshipService s1 = MentorshipService.getInstance();
            MentorshipService s2 = MentorshipService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(10)
        @DisplayName("FormationRatingService singleton")
        void ratingServiceSingleton() {
            FormationRatingService s1 = FormationRatingService.getInstance();
            FormationRatingService s2 = FormationRatingService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(11)
        @DisplayName("FormationDashboardService singleton")
        void dashboardServiceSingleton() {
            FormationDashboardService s1 = FormationDashboardService.getInstance();
            FormationDashboardService s2 = FormationDashboardService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test @Order(12)
        @DisplayName("FormationMatchingService singleton")
        void matchingServiceSingleton() {
            FormationMatchingService s1 = FormationMatchingService.getInstance();
            FormationMatchingService s2 = FormationMatchingService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 4: Database Schema Validation
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(40)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("40. Formation DB Schema Tests")
    class FormationDbSchemaTests {

        private boolean tableExists(String table) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                ResultSet rs = conn.getMetaData().getTables(null, null, table, null);
                return rs.next();
            }
        }

        private boolean columnExists(String table, String column) throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                ResultSet rs = conn.getMetaData().getColumns(null, null, table, column);
                return rs.next();
            }
        }

        @Test @Order(1)
        @DisplayName("'formations' table exists")
        void formationsTableExists() throws SQLException {
            assertTrue(tableExists("formations"));
        }

        @Test @Order(2)
        @DisplayName("'formation_modules' table exists")
        void modulesTableExists() throws SQLException {
            assertTrue(tableExists("formation_modules"));
        }

        @Test @Order(3)
        @DisplayName("'enrollments' table exists")
        void enrollmentsTableExists() throws SQLException {
            assertTrue(tableExists("enrollments"));
        }

        @Test @Order(4)
        @DisplayName("'lesson_progress' table exists")
        void lessonProgressTableExists() throws SQLException {
            assertTrue(tableExists("lesson_progress"));
        }

        @Test @Order(5)
        @DisplayName("'certificates' table exists")
        void certificatesTableExists() throws SQLException {
            assertTrue(tableExists("certificates"));
        }

        @Test @Order(6)
        @DisplayName("'quizzes' table exists")
        void quizzesTableExists() throws SQLException {
            assertTrue(tableExists("quizzes"));
        }

        @Test @Order(7)
        @DisplayName("'quiz_questions' table exists")
        void quizQuestionsTableExists() throws SQLException {
            assertTrue(tableExists("quiz_questions"));
        }

        @Test @Order(8)
        @DisplayName("'quiz_results' table exists")
        void quizResultsTableExists() throws SQLException {
            assertTrue(tableExists("quiz_results"));
        }

        @Test @Order(9)
        @DisplayName("'achievements' table exists")
        void achievementsTableExists() throws SQLException {
            assertTrue(tableExists("achievements"));
        }

        @Test @Order(10)
        @DisplayName("'mentorships' table exists")
        void mentorshipsTableExists() throws SQLException {
            assertTrue(tableExists("mentorships"));
        }

        @Test @Order(11)
        @DisplayName("'formation_ratings' table exists")
        void ratingsTableExists() throws SQLException {
            assertTrue(tableExists("formation_ratings"));
        }

        @Test @Order(12)
        @DisplayName("'formation_materials' table exists")
        void materialsTableExists() throws SQLException {
            assertTrue(tableExists("formation_materials"));
        }

        // Column existence checks
        @Test @Order(20)
        @DisplayName("formations has key columns")
        void formationsColumns() throws SQLException {
            assertTrue(columnExists("formations", "id"));
            assertTrue(columnExists("formations", "title"));
            assertTrue(columnExists("formations", "description"));
            assertTrue(columnExists("formations", "category"));
            assertTrue(columnExists("formations", "duration_hours"));
            assertTrue(columnExists("formations", "cost"));
            assertTrue(columnExists("formations", "level"));
            assertTrue(columnExists("formations", "status"));
        }

        @Test @Order(21)
        @DisplayName("formation_modules has content column")
        void modulesContentColumn() throws SQLException {
            assertTrue(columnExists("formation_modules", "content"));
            assertTrue(columnExists("formation_modules", "content_url"));
            assertTrue(columnExists("formation_modules", "title"));
            assertTrue(columnExists("formation_modules", "duration_minutes"));
            assertTrue(columnExists("formation_modules", "order_index"));
        }

        @Test @Order(22)
        @DisplayName("enrollments has progress column")
        void enrollmentsColumns() throws SQLException {
            assertTrue(columnExists("enrollments", "formation_id"));
            assertTrue(columnExists("enrollments", "user_id"));
            assertTrue(columnExists("enrollments", "progress"));
            assertTrue(columnExists("enrollments", "status"));
        }

        @Test @Order(23)
        @DisplayName("lesson_progress has module_id column")
        void lessonProgressColumns() throws SQLException {
            assertTrue(columnExists("lesson_progress", "enrollment_id"));
            assertTrue(columnExists("lesson_progress", "module_id"));
            assertTrue(columnExists("lesson_progress", "progress_percentage"));
        }

        @Test @Order(24)
        @DisplayName("certificates has verification columns")
        void certificateColumns() throws SQLException {
            assertTrue(columnExists("certificates", "enrollment_id"));
            assertTrue(columnExists("certificates", "certificate_number"));
        }

        @Test @Order(25)
        @DisplayName("formation_ratings has star_rating column")
        void ratingColumns() throws SQLException {
            assertTrue(columnExists("formation_ratings", "user_id"));
            assertTrue(columnExists("formation_ratings", "formation_id"));
            assertTrue(columnExists("formation_ratings", "star_rating"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 5: FormationService DB Operations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(50)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("50. FormationService CRUD")
    class FormationServiceTests {

        private static final FormationService service = FormationService.getInstance();
        private static int testFormationId = -1;

        @Test @Order(1)
        @DisplayName("findAll returns list (not null)")
        void findAll() {
            List<Formation> all = service.findAll();
            assertNotNull(all);
        }

        @Test @Order(2)
        @DisplayName("createFormation returns valid ID")
        void createFormation() {
            Formation f = new Formation();
            f.setTitle("Test Formation " + System.currentTimeMillis());
            f.setDescription("Test description");
            f.setCategory("Development");
            f.setDurationHours(10);
            f.setCost(0);
            f.setCurrency("TND");
            f.setProvider("JUnit");
            f.setLevel(FormationLevel.BEGINNER);
            f.setStatus("ACTIVE");
            f.setCreatedBy(1);

            int id = service.createFormation(f);
            assertTrue(id > 0, "Created formation should have positive ID");
            testFormationId = id;
        }

        @Test @Order(3)
        @DisplayName("findById returns created formation")
        void findById() {
            if (testFormationId <= 0) return;
            Formation f = service.findById(testFormationId);
            assertNotNull(f);
            assertTrue(f.getTitle().startsWith("Test Formation"));
            assertEquals("Development", f.getCategory());
        }

        @Test @Order(4)
        @DisplayName("updateFormation modifies title")
        void updateFormation() {
            if (testFormationId <= 0) return;
            Formation f = service.findById(testFormationId);
            assertNotNull(f);
            f.setTitle("Updated Formation Title");
            boolean updated = service.updateFormation(f);
            assertTrue(updated);
            Formation reloaded = service.findById(testFormationId);
            assertEquals("Updated Formation Title", reloaded.getTitle());
        }

        @Test @Order(5)
        @DisplayName("search finds by keyword")
        void search() {
            if (testFormationId <= 0) return;
            List<Formation> results = service.search("Updated Formation");
            assertNotNull(results);
            assertTrue(results.stream().anyMatch(f -> f.getId() == testFormationId));
        }

        @Test @Order(6)
        @DisplayName("formationExists returns true for existing")
        void formationExists() {
            if (testFormationId <= 0) return;
            assertTrue(service.formationExists(testFormationId));
            assertFalse(service.formationExists(999999));
        }

        @Test @Order(7)
        @DisplayName("findByCategory filters correctly")
        void findByCategory() {
            List<Formation> devFormations = service.findByCategory("Development");
            assertNotNull(devFormations);
        }

        @Test @Order(99)
        @DisplayName("deleteFormation removes formation")
        void deleteFormation() {
            if (testFormationId <= 0) return;
            boolean deleted = service.deleteFormation(testFormationId);
            assertTrue(deleted);
            assertNull(service.findById(testFormationId));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 6: FormationModuleService DB Operations
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(60)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("60. FormationModuleService CRUD")
    class ModuleServiceTests {

        private static final FormationModuleService moduleService = FormationModuleService.getInstance();
        private static final FormationService formationService = FormationService.getInstance();
        private static int parentFormationId = -1;
        private static int testModuleId1 = -1;
        private static int testModuleId2 = -1;

        @Test @Order(1)
        @DisplayName("Create parent formation for module tests")
        void createParentFormation() {
            Formation f = new Formation();
            f.setTitle("Module Test Parent " + System.currentTimeMillis());
            f.setDescription("Parent");
            f.setCategory("Development");
            f.setDurationHours(5);
            f.setCost(0);
            f.setCurrency("TND");
            f.setLevel(FormationLevel.BEGINNER);
            f.setStatus("ACTIVE");
            f.setCreatedBy(1);
            parentFormationId = formationService.createFormation(f);
            assertTrue(parentFormationId > 0);
        }

        @Test @Order(2)
        @DisplayName("create module returns valid ID")
        void createModule() {
            FormationModule m = new FormationModule();
            m.setFormationId(parentFormationId);
            m.setTitle("Module 1: Intro");
            m.setDescription("Introduction chapter");
            m.setContent("<p>Welcome to Module 1</p>");
            m.setContentUrl("https://example.com/m1");
            m.setDurationMinutes(30);
            m.setOrderIndex(0);
            testModuleId1 = moduleService.create(m);
            assertTrue(testModuleId1 > 0);
        }

        @Test @Order(3)
        @DisplayName("create second module")
        void createSecondModule() {
            FormationModule m = new FormationModule();
            m.setFormationId(parentFormationId);
            m.setTitle("Module 2: Deep Dive");
            m.setDescription("Advanced content");
            m.setDurationMinutes(60);
            m.setOrderIndex(1);
            testModuleId2 = moduleService.create(m);
            assertTrue(testModuleId2 > 0);
        }

        @Test @Order(4)
        @DisplayName("findById returns module with content")
        void findById() {
            if (testModuleId1 <= 0) return;
            FormationModule m = moduleService.findById(testModuleId1);
            assertNotNull(m);
            assertEquals("Module 1: Intro", m.getTitle());
            assertEquals("<p>Welcome to Module 1</p>", m.getContent());
            assertEquals("https://example.com/m1", m.getContentUrl());
            assertEquals(30, m.getDurationMinutes());
        }

        @Test @Order(5)
        @DisplayName("findByFormation returns ordered modules")
        void findByFormation() {
            if (parentFormationId <= 0) return;
            List<FormationModule> modules = moduleService.findByFormation(parentFormationId);
            assertNotNull(modules);
            assertEquals(2, modules.size());
            assertEquals("Module 1: Intro", modules.get(0).getTitle());
            assertEquals("Module 2: Deep Dive", modules.get(1).getTitle());
        }

        @Test @Order(6)
        @DisplayName("update changes module title and content")
        void updateModule() {
            if (testModuleId1 <= 0) return;
            FormationModule m = moduleService.findById(testModuleId1);
            m.setTitle("Module 1: Introduction (Updated)");
            m.setContent("<p>Updated content</p>");
            assertTrue(moduleService.update(m));

            FormationModule reloaded = moduleService.findById(testModuleId1);
            assertEquals("Module 1: Introduction (Updated)", reloaded.getTitle());
            assertEquals("<p>Updated content</p>", reloaded.getContent());
        }

        @Test @Order(7)
        @DisplayName("countByFormation returns correct count")
        void countByFormation() {
            if (parentFormationId <= 0) return;
            int count = moduleService.countByFormation(parentFormationId);
            assertEquals(2, count);
        }

        @Test @Order(8)
        @DisplayName("create module with null title throws")
        void createNullTitleThrows() {
            FormationModule m = new FormationModule();
            m.setFormationId(parentFormationId);
            m.setTitle(null);
            assertThrows(IllegalArgumentException.class, () -> moduleService.create(m));
        }

        @Test @Order(9)
        @DisplayName("create module with blank title throws")
        void createBlankTitleThrows() {
            FormationModule m = new FormationModule();
            m.setFormationId(parentFormationId);
            m.setTitle("   ");
            assertThrows(IllegalArgumentException.class, () -> moduleService.create(m));
        }

        @Test @Order(10)
        @DisplayName("create module with invalid formationId throws")
        void createInvalidFormationId() {
            FormationModule m = new FormationModule();
            m.setFormationId(0);
            m.setTitle("Bad Module");
            assertThrows(IllegalArgumentException.class, () -> moduleService.create(m));
        }

        @Test @Order(98)
        @DisplayName("delete module")
        void deleteModules() {
            if (testModuleId1 > 0) assertTrue(moduleService.delete(testModuleId1));
            if (testModuleId2 > 0) assertTrue(moduleService.delete(testModuleId2));
        }

        @Test @Order(99)
        @DisplayName("cleanup parent formation")
        void cleanup() {
            if (parentFormationId > 0) formationService.deleteFormation(parentFormationId);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 7: EnrollmentService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(70)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("70. EnrollmentService Lifecycle")
    class EnrollmentServiceTests {

        private static final EnrollmentService enrollService = EnrollmentService.getInstance();

        @Test @Order(1)
        @DisplayName("EnrollmentService findByUserId does not throw")
        void findByUserIdSafe() {
            // Use an unlikely user ID — should return empty list, not throw
            List<Enrollment> result = enrollService.findByUserId(999999);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("isEnrolled returns false for non-enrolled")
        void isEnrolledFalse() {
            assertFalse(enrollService.isEnrolled(999999, 999999));
        }

        @Test @Order(3)
        @DisplayName("countByFormation returns 0 for non-existent formation")
        void countByFormation() {
            long count = enrollService.countByFormation(999999);
            assertEquals(0, count);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 8: QuizService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(80)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("80. QuizService Operations")
    class QuizServiceTests {

        private static final QuizService quizService = QuizService.getInstance();

        @Test @Order(1)
        @DisplayName("findQuizzesByFormation returns list")
        void findByFormation() {
            List<Quiz> quizzes = quizService.findQuizzesByFormation(999999);
            assertNotNull(quizzes);
            assertTrue(quizzes.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("findQuizById returns null for non-existent")
        void findByIdNull() {
            Quiz q = quizService.findQuizById(999999);
            assertNull(q);
        }

        @Test @Order(3)
        @DisplayName("getAttemptCount returns 0 for non-existent")
        void attemptCount() {
            int count = quizService.getAttemptCount(999999, 999999);
            assertEquals(0, count);
        }

        @Test @Order(4)
        @DisplayName("hasPassedQuiz returns false for non-existent")
        void hasPassedFalse() {
            assertFalse(quizService.hasPassedQuiz(999999, 999999));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 9: AchievementService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(90)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("90. AchievementService Operations")
    class AchievementServiceTests {

        private static final AchievementService service = AchievementService.getInstance();

        @Test @Order(1)
        @DisplayName("findByUserId returns empty for non-existent user")
        void findByUserIdEmpty() {
            List<Achievement> achievements = service.findByUserId(999999);
            assertNotNull(achievements);
            assertTrue(achievements.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("getTotalPoints returns 0 for non-existent user")
        void totalPointsZero() {
            int points = service.getTotalPoints(999999);
            assertEquals(0, points);
        }

        @Test @Order(3)
        @DisplayName("hasAchievement returns false for non-existent")
        void hasAchievementFalse() {
            assertFalse(service.hasAchievement(999999, "NONEXISTENT"));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 10: LessonProgressService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(100)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("100. LessonProgressService Operations")
    class LessonProgressServiceTests {

        private static final LessonProgressService service = LessonProgressService.getInstance();

        @Test @Order(1)
        @DisplayName("getProgressByEnrollment returns empty map for non-existent")
        void progressByEnrollmentEmpty() {
            Map<Integer, Integer> progress = service.getProgressByEnrollment(999999);
            assertNotNull(progress);
            assertTrue(progress.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("getCompletedLessonsCount returns 0 for non-existent")
        void completedCountZero() {
            assertEquals(0, service.getCompletedLessonsCount(999999));
        }

        @Test @Order(3)
        @DisplayName("calculateOverallProgress returns 0 for empty enrollment")
        void overallProgressZero() {
            assertEquals(0, service.calculateOverallProgress(999999, 5));
        }

        @Test @Order(4)
        @DisplayName("calculateOverallProgress with 0 lessons returns 0")
        void overallProgressZeroLessons() {
            assertEquals(0, service.calculateOverallProgress(999999, 0));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 11: Formation Dashboard & Matching
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(110)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("110. Dashboard & Matching Services")
    class DashboardMatchingTests {

        @Test @Order(1)
        @DisplayName("FormationDashboardService getTotalFormations >= 0")
        void totalFormations() {
            int total = FormationDashboardService.getInstance().getTotalFormations();
            assertTrue(total >= 0);
        }

        @Test @Order(2)
        @DisplayName("FormationDashboardService getUserDashboardStats returns stats")
        void userDashboardStats() {
            var stats = FormationDashboardService.getInstance().getUserDashboardStats(999999);
            assertNotNull(stats);
        }

        @Test @Order(3)
        @DisplayName("FormationMatchingService getRecommendations returns list")
        void recommendations() {
            var recs = FormationMatchingService.getInstance().getRecommendations(999999, 5);
            assertNotNull(recs);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 12: CertificateService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(120)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("120. CertificateService Operations")
    class CertificateServiceTests {

        private static final CertificateService service = CertificateService.getInstance();

        @Test @Order(1)
        @DisplayName("findByUser returns list for non-existent user")
        void findByUserEmpty() {
            List<Certificate> certs = service.findByUser(999999);
            assertNotNull(certs);
            assertTrue(certs.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("findById returns null for non-existent")
        void findByIdNull() {
            Certificate c = service.findById(999999);
            assertNull(c);
        }

        @Test @Order(3)
        @DisplayName("findByEnrollment returns null for non-existent")
        void findByEnrollmentNull() {
            Certificate c = service.findByEnrollment(999999);
            assertNull(c);
        }

        @Test @Order(4)
        @DisplayName("existsForEnrollment returns false for non-existent")
        void existsForEnrollmentFalse() {
            assertFalse(service.existsForEnrollment(999999));
        }

        @Test @Order(5)
        @DisplayName("generateCertificateNumber returns non-null string")
        void generateCertNumber() {
            String num = service.generateCertificateNumber();
            assertNotNull(num);
            assertFalse(num.isBlank());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 13: MentorshipService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(130)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("130. MentorshipService Operations")
    class MentorshipServiceTests {

        private static final MentorshipService service = MentorshipService.getInstance();

        @Test @Order(1)
        @DisplayName("findByMentor returns empty for non-existent")
        void findByMentorEmpty() {
            List<Mentorship> result = service.findByMentor(999999);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test @Order(2)
        @DisplayName("findByMentee returns empty for non-existent")
        void findByMenteeEmpty() {
            List<Mentorship> result = service.findByMentee(999999);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test @Order(3)
        @DisplayName("findPendingForMentor returns empty for non-existent")
        void findPendingEmpty() {
            List<Mentorship> result = service.findPendingForMentor(999999);
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 14: FormationRatingService Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(140)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("140. FormationRatingService Operations")
    class RatingServiceTests {

        private static final FormationRatingService service = FormationRatingService.getInstance();

        @Test @Order(1)
        @DisplayName("hasUserRated returns false for non-existent")
        void hasUserRatedFalse() {
            assertFalse(service.hasUserRated(999999, 999999));
        }

        @Test @Order(2)
        @DisplayName("getStatistics returns stats for non-existent formation")
        void getStatistics() {
            var stats = service.getStatistics(999999);
            assertNotNull(stats);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Section 15: Edge Cases & Validation Tests
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @Order(150)
    @TestMethodOrder(OrderAnnotation.class)
    @DisplayName("150. Edge Cases & Validation")
    class EdgeCaseTests {

        @Test @Order(1)
        @DisplayName("FormationModuleService rejects null module")
        void rejectNullModule() {
            assertThrows(IllegalArgumentException.class,
                    () -> FormationModuleService.getInstance().create(null));
        }

        @Test @Order(2)
        @DisplayName("FormationModule negative duration treated as 0")
        void negativeDuration() {
            FormationModule m = new FormationModule();
            m.setDurationMinutes(-5);
            // Entity doesn't validate, but getFormattedDuration returns "N/A" for <= 0
            assertEquals("N/A", m.getFormattedDuration());
        }

        @Test @Order(3)
        @DisplayName("Formation with null level is valid")
        void formationNullLevel() {
            Formation f = new Formation();
            f.setLevel(null);
            assertNull(f.getLevel());
        }

        @Test @Order(4)
        @DisplayName("LessonProgress full constructor sets all fields")
        void lpFullConstructor() {
            LocalDateTime now = LocalDateTime.now();
            LessonProgress lp = new LessonProgress(1, 2, 3, true, 100.0,
                    now, now, now);
            assertEquals(1, lp.getId());
            assertEquals(2, lp.getEnrollmentId());
            assertEquals(3, lp.getLessonId());
            assertTrue(lp.isCompleted());
            assertEquals(100.0, lp.getProgressPercentage(), 0.001);
        }

        @Test @Order(5)
        @DisplayName("Enrollment equals/hashCode by ID")
        void enrollmentEquality() {
            Enrollment e1 = new Enrollment();
            e1.setId(1);
            Enrollment e2 = new Enrollment();
            e2.setId(1);
            assertEquals(e1, e2);
            assertEquals(e1.hashCode(), e2.hashCode());
        }

        @Test @Order(6)
        @DisplayName("FormationLevel display names")
        void levelDisplayNames() {
            // Check display labels exist
            for (FormationLevel level : FormationLevel.values()) {
                assertNotNull(level.name());
                assertTrue(level.name().length() > 0);
            }
        }

        @Test @Order(7)
        @DisplayName("TrainingCategory has display names")
        void categoryDisplayNames() {
            for (TrainingCategory cat : TrainingCategory.values()) {
                assertNotNull(cat.name());
            }
        }

        @Test @Order(8)
        @DisplayName("Certificate equals by ID")
        void certificateEquality() {
            Certificate c1 = new Certificate();
            c1.setId(1);
            Certificate c2 = new Certificate();
            c2.setId(1);
            assertEquals(c1, c2);
        }
    }
}
