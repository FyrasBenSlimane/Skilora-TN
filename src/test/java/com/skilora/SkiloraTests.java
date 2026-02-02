package com.skilora;

import com.skilora.config.DatabaseConfig;

// === User Module ===
import com.skilora.user.entity.User;
import com.skilora.user.enums.Role;
import com.skilora.user.service.UserService;
import com.skilora.user.service.AuthService;

// === Recruitment Module ===
import com.skilora.recruitment.entity.JobOffer;
import com.skilora.recruitment.entity.JobOpportunity;
import com.skilora.recruitment.entity.Application;
import com.skilora.recruitment.entity.Application.Status;
import com.skilora.recruitment.enums.JobStatus;
import com.skilora.recruitment.service.JobService;
import com.skilora.recruitment.service.ApplicationService;

// === Formation Module ===
import com.skilora.formation.entity.Formation;
import com.skilora.formation.enums.FormationLevel;

// === Support Module ===
import com.skilora.support.entity.SupportTicket;

// === Finance Module ===
import com.skilora.finance.entity.EmploymentContract;

// === Community Module ===
import com.skilora.community.entity.Post;
import com.skilora.community.enums.PostType;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *   SKILORA - Comprehensive Test Suite
 *   Tests: Entities, Getters/Setters, Enums, DB Connection,
 *          CRUD Operations, Service Logic, Password Hashing
 * ╚══════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Skilora Comprehensive Tests")
class SkiloraTests {

    // ═══════════════════════════════════════════════════════════
    //  SECTION 1: DATABASE CONNECTION
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. Database Connection")
    @TestMethodOrder(OrderAnnotation.class)
    class DatabaseConnectionTests {

        @Test
        @Order(1)
        @DisplayName("DatabaseConfig singleton returns same instance")
        void testSingletonInstance() {
            DatabaseConfig db1 = DatabaseConfig.getInstance();
            DatabaseConfig db2 = DatabaseConfig.getInstance();
            assertSame(db1, db2, "DatabaseConfig should return the same singleton instance");
        }

        @Test
        @Order(2)
        @DisplayName("Database connection is established")
        void testConnectionNotNull() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                assertNotNull(conn, "Connection should not be null");
                assertFalse(conn.isClosed(), "Connection should be open");
            }
        }

        @Test
        @Order(3)
        @DisplayName("Database URL is configured")
        void testDatabaseUrl() {
            String url = DatabaseConfig.getInstance().getUrl();
            assertNotNull(url, "Database URL should not be null");
            assertTrue(url.contains("skilora"), "URL should reference 'skilora' database");
        }

        @Test
        @Order(4)
        @DisplayName("Can execute simple query")
        void testSimpleQuery() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT 1 AS test_col");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Should return a row");
                assertEquals(1, rs.getInt("test_col"));
            }
        }

        @Test
        @Order(5)
        @DisplayName("Users table exists and is accessible")
        void testUsersTableExists() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM users");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Should return count from users table");
                assertTrue(rs.getInt(1) >= 0, "User count should be >= 0");
            }
        }

        @Test
        @Order(6)
        @DisplayName("Job offers table exists and is accessible")
        void testJobOffersTableExists() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT COUNT(*) FROM job_offers");
                 ResultSet rs = stmt.executeQuery()) {
                assertTrue(rs.next(), "Should return count from job_offers table");
            }
        }

        @Test
        @Order(7)
        @DisplayName("Core tables all exist")
        void testCoreTablesExist() throws SQLException {
            String[] tables = {
                "users", "profiles", "skills", "experiences",
                "job_offers", "applications", "companies",
                "formations", "enrollments",
                "support_tickets", "faq_articles",
                "employment_contracts", "payslips", "bank_accounts",
                "posts", "conversations", "messages", "connections"
            };
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                for (String table : tables) {
                    try (PreparedStatement stmt = conn.prepareStatement("SELECT 1 FROM " + table + " LIMIT 1")) {
                        stmt.execute(); // No exception = table exists
                    } catch (SQLException e) {
                        fail("Table '" + table + "' does not exist or is not accessible: " + e.getMessage());
                    }
                }
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 2: ENTITY GETTERS / SETTERS / CONSTRUCTORS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. Entity Tests")
    class EntityTests {

        // ---------- User Entity ----------

        @Nested
        @DisplayName("2.1 User Entity")
        class UserEntityTests {

            @Test
            @DisplayName("Default constructor sets active=true")
            void testDefaultConstructor() {
                User user = new User();
                assertTrue(user.isActive(), "New user should be active by default");
            }

            @Test
            @DisplayName("Parameterized constructor sets all fields")
            void testParameterizedConstructor() {
                User user = new User("john", "pass123", Role.USER, "John Doe");
                assertEquals("john", user.getUsername());
                assertEquals("pass123", user.getPassword());
                assertEquals(Role.USER, user.getRole());
                assertEquals("John Doe", user.getFullName());
                assertTrue(user.isActive());
            }

            @Test
            @DisplayName("All getters and setters work")
            void testGettersSetters() {
                User user = new User();
                user.setId(42);
                user.setUsername("alice");
                user.setEmail("alice@skilora.com");
                user.setPassword("secret");
                user.setRole(Role.EMPLOYER);
                user.setFullName("Alice Smith");
                user.setPhotoUrl("https://example.com/photo.jpg");
                user.setVerified(true);
                user.setActive(false);

                assertEquals(42, user.getId());
                assertEquals("alice", user.getUsername());
                assertEquals("alice@skilora.com", user.getEmail());
                assertEquals("secret", user.getPassword());
                assertEquals(Role.EMPLOYER, user.getRole());
                assertEquals("Alice Smith", user.getFullName());
                assertEquals("https://example.com/photo.jpg", user.getPhotoUrl());
                assertTrue(user.isVerified());
                assertFalse(user.isActive());
            }

            @Test
            @DisplayName("toString uses role display name")
            void testToString() {
                User user = new User("bob", "pw", Role.ADMIN, "Bob Admin");
                String str = user.toString();
                assertTrue(str.contains("Bob Admin"), "toString should contain full name");
                assertTrue(str.contains("Administrator"), "toString should contain role display name");
            }
        }

        // ---------- Role Enum ----------

        @Nested
        @DisplayName("2.2 Role Enum")
        class RoleEnumTests {

            @Test
            @DisplayName("All roles have display names")
            void testRoleDisplayNames() {
                assertEquals("Administrator", Role.ADMIN.getDisplayName());
                assertEquals("Job Seeker", Role.USER.getDisplayName());
                assertEquals("Employer", Role.EMPLOYER.getDisplayName());
                assertEquals("Trainer", Role.TRAINER.getDisplayName());
            }

            @Test
            @DisplayName("Role.values() contains all 4 roles")
            void testRoleCount() {
                assertEquals(4, Role.values().length);
            }

            @Test
            @DisplayName("Role.valueOf works correctly")
            void testRoleValueOf() {
                assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
                assertEquals(Role.USER, Role.valueOf("USER"));
                assertEquals(Role.EMPLOYER, Role.valueOf("EMPLOYER"));
                assertEquals(Role.TRAINER, Role.valueOf("TRAINER"));
            }
        }

        // ---------- JobOffer Entity ----------

        @Nested
        @DisplayName("2.3 JobOffer Entity")
        class JobOfferEntityTests {

            @Test
            @DisplayName("Default constructor sets defaults")
            void testDefaults() {
                JobOffer job = new JobOffer();
                assertNotNull(job.getRequiredSkills(), "Skills list should be initialized");
                assertEquals(JobStatus.DRAFT, job.getStatus(), "Default status should be DRAFT");
                assertNotNull(job.getPostedDate(), "Posted date should be set");
            }

            @Test
            @DisplayName("3-arg constructor sets fields")
            void testThreeArgConstructor() {
                JobOffer job = new JobOffer(1, "Java Dev", "Tunis");
                assertEquals(1, job.getEmployerId());
                assertEquals("Java Dev", job.getTitle());
                assertEquals("Tunis", job.getLocation());
            }

            @Test
            @DisplayName("Getters and setters work")
            void testGettersSetters() {
                JobOffer job = new JobOffer();
                job.setId(10);
                job.setEmployerId(5);
                job.setTitle("Backend Developer");
                job.setDescription("Build APIs");
                job.setLocation("Sfax");
                job.setSalaryMin(2000);
                job.setSalaryMax(4000);
                job.setCurrency("TND");
                job.setWorkType("REMOTE");
                job.setStatus(JobStatus.ACTIVE);
                job.setDeadline(LocalDate.of(2026, 6, 30));
                job.setCompanyName("TechCorp");

                assertEquals(10, job.getId());
                assertEquals(5, job.getEmployerId());
                assertEquals("Backend Developer", job.getTitle());
                assertEquals("Build APIs", job.getDescription());
                assertEquals("Sfax", job.getLocation());
                assertEquals(2000, job.getSalaryMin());
                assertEquals(4000, job.getSalaryMax());
                assertEquals("TND", job.getCurrency());
                assertEquals("REMOTE", job.getWorkType());
                assertEquals(JobStatus.ACTIVE, job.getStatus());
                assertEquals(LocalDate.of(2026, 6, 30), job.getDeadline());
                assertEquals("TechCorp", job.getCompanyName());
            }

            @Test
            @DisplayName("getSalaryRange formats correctly")
            void testSalaryRange() {
                JobOffer job = new JobOffer();

                // Both min and max
                job.setSalaryMin(2000);
                job.setSalaryMax(4000);
                assertEquals("2000 - 4000", job.getSalaryRange());

                // Only min
                job.setSalaryMax(0);
                assertEquals("From 2000", job.getSalaryRange());

                // Only max
                job.setSalaryMin(0);
                job.setSalaryMax(5000);
                assertEquals("Up to 5000", job.getSalaryRange());

                // Neither
                job.setSalaryMax(0);
                assertEquals("Negotiable", job.getSalaryRange());
            }

            @Test
            @DisplayName("isActive checks ACTIVE status")
            void testIsActive() {
                JobOffer job = new JobOffer();
                assertFalse(job.isActive(), "DRAFT job should not be active");

                job.setStatus(JobStatus.ACTIVE);
                assertTrue(job.isActive(), "ACTIVE job should be active");

                job.setStatus(JobStatus.CLOSED);
                assertFalse(job.isActive(), "CLOSED job should not be active");
            }

            @Test
            @DisplayName("addRequiredSkill deduplicates")
            void testAddSkillDedup() {
                JobOffer job = new JobOffer();
                job.addRequiredSkill("Java");
                job.addRequiredSkill("Spring");
                job.addRequiredSkill("Java"); // duplicate

                assertEquals(2, job.getRequiredSkills().size(), "Should have 2 unique skills");
                assertTrue(job.getRequiredSkills().contains("Java"));
                assertTrue(job.getRequiredSkills().contains("Spring"));
            }
        }

        // ---------- JobStatus Enum ----------

        @Nested
        @DisplayName("2.4 JobStatus Enum")
        class JobStatusEnumTests {

            @Test
            @DisplayName("All statuses have display names")
            void testDisplayNames() {
                assertEquals("Active", JobStatus.ACTIVE.getDisplayName());
                assertEquals("Open", JobStatus.OPEN.getDisplayName());
                assertEquals("Closed", JobStatus.CLOSED.getDisplayName());
                assertEquals("Pending", JobStatus.PENDING.getDisplayName());
                assertEquals("Draft", JobStatus.DRAFT.getDisplayName());
            }
        }

        // ---------- Application Entity ----------

        @Nested
        @DisplayName("2.5 Application Entity")
        class ApplicationEntityTests {

            @Test
            @DisplayName("Default constructor sets PENDING + now()")
            void testDefaults() {
                Application app = new Application();
                assertEquals(Status.PENDING, app.getStatus());
                assertNotNull(app.getAppliedDate());
            }

            @Test
            @DisplayName("2-arg constructor sets IDs")
            void testTwoArgConstructor() {
                Application app = new Application(5, 10);
                assertEquals(5, app.getJobOfferId());
                assertEquals(10, app.getCandidateProfileId());
                assertEquals(Status.PENDING, app.getStatus());
            }

            @Test
            @DisplayName("Getters and setters work")
            void testGettersSetters() {
                Application app = new Application();
                app.setId(1);
                app.setJobOfferId(2);
                app.setCandidateProfileId(3);
                app.setStatus(Status.INTERVIEW);
                app.setCoverLetter("I am interested...");
                app.setCustomCvUrl("https://cv.pdf");
                app.setJobTitle("Dev Java");
                app.setCompanyName("Skilora");
                app.setCandidateName("Ahmed");
                app.setJobLocation("Tunis");

                assertEquals(1, app.getId());
                assertEquals(2, app.getJobOfferId());
                assertEquals(3, app.getCandidateProfileId());
                assertEquals(Status.INTERVIEW, app.getStatus());
                assertEquals("I am interested...", app.getCoverLetter());
                assertEquals("https://cv.pdf", app.getCustomCvUrl());
                assertEquals("Dev Java", app.getJobTitle());
                assertEquals("Skilora", app.getCompanyName());
                assertEquals("Ahmed", app.getCandidateName());
                assertEquals("Tunis", app.getJobLocation());
            }

            @Test
            @DisplayName("Application.Status enum display names")
            void testStatusDisplayNames() {
                assertEquals("En attente", Status.PENDING.getDisplayName());
                assertEquals("En cours", Status.REVIEWING.getDisplayName());
                assertEquals("Entretien", Status.INTERVIEW.getDisplayName());
                assertEquals("Offre", Status.OFFER.getDisplayName());
                assertEquals("Refusé", Status.REJECTED.getDisplayName());
                assertEquals("Accepté", Status.ACCEPTED.getDisplayName());
            }
        }

        // ---------- Formation Entity ----------

        @Nested
        @DisplayName("2.6 Formation Entity")
        class FormationEntityTests {

            @Test
            @DisplayName("Default constructor sets defaults")
            void testDefaults() {
                Formation f = new Formation();
                assertEquals(0, f.getDurationHours());
                assertEquals(0.00, f.getCost());
                assertEquals("TND", f.getCurrency());
                assertEquals(FormationLevel.BEGINNER, f.getLevel());
                assertTrue(f.isFree());
                assertEquals("ACTIVE", f.getStatus());
                assertNotNull(f.getCreatedDate());
            }

            @Test
            @DisplayName("3-arg constructor sets fields")
            void testThreeArgConstructor() {
                Formation f = new Formation("Java Basics", "Programming", 40);
                assertEquals("Java Basics", f.getTitle());
                assertEquals("Programming", f.getCategory());
                assertEquals(40, f.getDurationHours());
            }

            @Test
            @DisplayName("All getters and setters")
            void testGettersSetters() {
                Formation f = new Formation();
                f.setId(1);
                f.setTitle("Spring Boot");
                f.setDescription("Learn Spring");
                f.setCategory("Backend");
                f.setDurationHours(60);
                f.setCost(299.99);
                f.setCurrency("EUR");
                f.setProvider("Skilora Academy");
                f.setImageUrl("img.png");
                f.setLevel(FormationLevel.ADVANCED);
                f.setFree(false);
                f.setCreatedBy(5);
                f.setStatus("ARCHIVED");

                assertEquals(1, f.getId());
                assertEquals("Spring Boot", f.getTitle());
                assertEquals("Learn Spring", f.getDescription());
                assertEquals("Backend", f.getCategory());
                assertEquals(60, f.getDurationHours());
                assertEquals(299.99, f.getCost());
                assertEquals("EUR", f.getCurrency());
                assertEquals("Skilora Academy", f.getProvider());
                assertEquals("img.png", f.getImageUrl());
                assertEquals(FormationLevel.ADVANCED, f.getLevel());
                assertFalse(f.isFree());
                assertEquals(5, f.getCreatedBy());
                assertEquals("ARCHIVED", f.getStatus());
            }

            @Test
            @DisplayName("equals/hashCode by ID")
            void testEquality() {
                Formation f1 = new Formation();
                f1.setId(10);
                Formation f2 = new Formation();
                f2.setId(10);
                Formation f3 = new Formation();
                f3.setId(20);

                assertEquals(f1, f2, "Same ID should be equal");
                assertNotEquals(f1, f3, "Different ID should not be equal");
                assertEquals(f1.hashCode(), f2.hashCode());
            }
        }

        // ---------- SupportTicket Entity ----------

        @Nested
        @DisplayName("2.7 SupportTicket Entity")
        class SupportTicketEntityTests {

            @Test
            @DisplayName("All getters and setters")
            void testGettersSetters() {
                SupportTicket t = new SupportTicket();
                t.setId(1);
                t.setUserId(5);
                t.setCategory("Technical");
                t.setPriority("HIGH");
                t.setStatus("OPEN");
                t.setSubject("Login issue");
                t.setDescription("Cannot login");
                t.setAssignedTo(10);
                LocalDateTime now = LocalDateTime.now();
                t.setCreatedDate(now);
                t.setUpdatedDate(now);
                t.setResolvedDate(now);
                t.setUserName("Ahmed");
                t.setAssignedToName("Admin");

                assertEquals(1, t.getId());
                assertEquals(5, t.getUserId());
                assertEquals("Technical", t.getCategory());
                assertEquals("HIGH", t.getPriority());
                assertEquals("OPEN", t.getStatus());
                assertEquals("Login issue", t.getSubject());
                assertEquals("Cannot login", t.getDescription());
                assertEquals(10, t.getAssignedTo());
                assertEquals(now, t.getCreatedDate());
                assertEquals(now, t.getUpdatedDate());
                assertEquals(now, t.getResolvedDate());
                assertEquals("Ahmed", t.getUserName());
                assertEquals("Admin", t.getAssignedToName());
            }

            @Test
            @DisplayName("Parameterized constructor")
            void testConstructor() {
                SupportTicket t = new SupportTicket(1, "Bug", "MEDIUM", "OPEN", "Error 500", "Server crash");
                assertEquals(1, t.getUserId());
                assertEquals("Bug", t.getCategory());
                assertEquals("MEDIUM", t.getPriority());
                assertEquals("OPEN", t.getStatus());
                assertEquals("Error 500", t.getSubject());
                assertEquals("Server crash", t.getDescription());
            }

            @Test
            @DisplayName("equals/hashCode by ID")
            void testEquality() {
                SupportTicket t1 = new SupportTicket();
                t1.setId(5);
                SupportTicket t2 = new SupportTicket();
                t2.setId(5);
                assertEquals(t1, t2);
                assertEquals(t1.hashCode(), t2.hashCode());
            }
        }

        // ---------- EmploymentContract Entity ----------

        @Nested
        @DisplayName("2.8 EmploymentContract Entity")
        class EmploymentContractEntityTests {

            @Test
            @DisplayName("Default constructor sets defaults")
            void testDefaults() {
                EmploymentContract c = new EmploymentContract();
                assertEquals("TND", c.getCurrency());
                assertEquals("CDI", c.getContractType());
                assertEquals("DRAFT", c.getStatus());
                assertFalse(c.isSigned());
            }

            @Test
            @DisplayName("3-arg constructor")
            void testConstructor() {
                EmploymentContract c = new EmploymentContract(1, new BigDecimal("3500.00"), LocalDate.of(2026, 1, 1));
                assertEquals(1, c.getUserId());
                assertEquals(new BigDecimal("3500.00"), c.getSalaryBase());
                assertEquals(LocalDate.of(2026, 1, 1), c.getStartDate());
            }

            @Test
            @DisplayName("All getters and setters")
            void testGettersSetters() {
                EmploymentContract c = new EmploymentContract();
                c.setId(1);
                c.setUserId(10);
                c.setEmployerId(20);
                c.setJobOfferId(30);
                c.setSalaryBase(new BigDecimal("5000"));
                c.setCurrency("EUR");
                c.setStartDate(LocalDate.of(2026, 3, 1));
                c.setEndDate(LocalDate.of(2027, 3, 1));
                c.setContractType("CDD");
                c.setStatus("ACTIVE");
                c.setPdfUrl("contract.pdf");
                c.setSigned(true);
                LocalDateTime now = LocalDateTime.now();
                c.setSignedDate(now);
                c.setCreatedDate(now);
                c.setUserName("Employee");
                c.setEmployerName("Boss");
                c.setJobTitle("Dev");

                assertEquals(1, c.getId());
                assertEquals(10, c.getUserId());
                assertEquals(20, c.getEmployerId());
                assertEquals(30, c.getJobOfferId());
                assertEquals(new BigDecimal("5000"), c.getSalaryBase());
                assertEquals("EUR", c.getCurrency());
                assertEquals(LocalDate.of(2026, 3, 1), c.getStartDate());
                assertEquals(LocalDate.of(2027, 3, 1), c.getEndDate());
                assertEquals("CDD", c.getContractType());
                assertEquals("ACTIVE", c.getStatus());
                assertEquals("contract.pdf", c.getPdfUrl());
                assertTrue(c.isSigned());
                assertEquals(now, c.getSignedDate());
                assertEquals(now, c.getCreatedDate());
                assertEquals("Employee", c.getUserName());
                assertEquals("Boss", c.getEmployerName());
                assertEquals("Dev", c.getJobTitle());
            }
        }

        // ---------- Post Entity ----------

        @Nested
        @DisplayName("2.9 Post Entity")
        class PostEntityTests {

            @Test
            @DisplayName("Default constructor sets defaults")
            void testDefaults() {
                Post p = new Post();
                assertEquals(PostType.STATUS, p.getPostType());
                assertTrue(p.isPublished());
                assertEquals(0, p.getLikesCount());
                assertEquals(0, p.getCommentsCount());
                assertEquals(0, p.getSharesCount());
            }

            @Test
            @DisplayName("All getters and setters")
            void testGettersSetters() {
                Post p = new Post();
                p.setId(1);
                p.setAuthorId(5);
                p.setContent("Hello world!");
                p.setImageUrl("img.png");
                p.setPostType(PostType.STATUS);
                p.setLikesCount(10);
                p.setCommentsCount(3);
                p.setSharesCount(1);
                p.setPublished(false);
                LocalDateTime now = LocalDateTime.now();
                p.setCreatedDate(now);
                p.setUpdatedDate(now);
                p.setAuthorName("Ahmed");
                p.setAuthorPhoto("photo.jpg");
                p.setLikedByCurrentUser(true);

                assertEquals(1, p.getId());
                assertEquals(5, p.getAuthorId());
                assertEquals("Hello world!", p.getContent());
                assertEquals("img.png", p.getImageUrl());
                assertEquals(PostType.STATUS, p.getPostType());
                assertEquals(10, p.getLikesCount());
                assertEquals(3, p.getCommentsCount());
                assertEquals(1, p.getSharesCount());
                assertFalse(p.isPublished());
                assertEquals(now, p.getCreatedDate());
                assertEquals(now, p.getUpdatedDate());
                assertEquals("Ahmed", p.getAuthorName());
                assertEquals("photo.jpg", p.getAuthorPhoto());
                assertTrue(p.isLikedByCurrentUser());
            }
        }

        // ---------- JobOpportunity Entity ----------

        @Nested
        @DisplayName("2.10 JobOpportunity Entity")
        class JobOpportunityEntityTests {

            @Test
            @DisplayName("All getters and setters")
            void testGettersSetters() {
                JobOpportunity j = new JobOpportunity();
                j.setSource("LinkedIn");
                j.setTitle("Full Stack Dev");
                j.setUrl("https://linkedin.com/job/123");
                j.setDescription("Great opportunity");
                j.setLocation("Tunis");
                j.setPostedDate("2026-01-15");
                j.setRawId("LI-123");

                assertEquals("LinkedIn", j.getSource());
                assertEquals("Full Stack Dev", j.getTitle());
                assertEquals("https://linkedin.com/job/123", j.getUrl());
                assertEquals("Great opportunity", j.getDescription());
                assertEquals("Tunis", j.getLocation());
                assertEquals("2026-01-15", j.getPostedDate());
                assertEquals("LI-123", j.getRawId());
            }
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 3: PASSWORD HASHING (no DB needed)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Password Hashing")
    class PasswordHashingTests {

        @Test
        @DisplayName("hashPassword returns BCrypt hash")
        void testHashFormat() {
            String hash = UserService.hashPassword("myPassword123");
            assertNotNull(hash);
            assertTrue(hash.startsWith("$2a$"), "Hash should be BCrypt format");
        }

        @Test
        @DisplayName("verifyPassword matches correct password")
        void testVerifyCorrect() {
            String hash = UserService.hashPassword("secret");
            assertTrue(UserService.verifyPassword("secret", hash));
        }

        @Test
        @DisplayName("verifyPassword rejects wrong password")
        void testVerifyWrong() {
            String hash = UserService.hashPassword("secret");
            assertFalse(UserService.verifyPassword("wrong", hash));
        }

        @Test
        @DisplayName("verifyPassword handles null stored password")
        void testVerifyNullStored() {
            assertFalse(UserService.verifyPassword("test", null));
        }

        @Test
        @DisplayName("verifyPassword handles null input password")
        void testVerifyNullInput() {
            assertFalse(UserService.verifyPassword(null, "$2a$12$abcdefgh"));
        }

        @Test
        @DisplayName("verifyPassword legacy plaintext fallback")
        void testLegacyPlaintext() {
            // Passwords not starting with $2a$ or $2b$ fall back to equals
            assertTrue(UserService.verifyPassword("plain", "plain"));
            assertFalse(UserService.verifyPassword("plain", "other"));
        }

        @Test
        @DisplayName("Different passwords produce different hashes")
        void testDifferentHashes() {
            String h1 = UserService.hashPassword("password1");
            String h2 = UserService.hashPassword("password2");
            assertNotEquals(h1, h2);
        }

        @Test
        @DisplayName("Same password produces different hashes (salted)")
        void testSaltedHashes() {
            String h1 = UserService.hashPassword("same");
            String h2 = UserService.hashPassword("same");
            assertNotEquals(h1, h2, "BCrypt uses random salt, hashes should differ");
            // But both should verify
            assertTrue(UserService.verifyPassword("same", h1));
            assertTrue(UserService.verifyPassword("same", h2));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 4: SERVICE SINGLETONS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Service Singletons")
    class ServiceSingletonTests {

        @Test
        @DisplayName("UserService singleton")
        void testUserServiceSingleton() {
            UserService s1 = UserService.getInstance();
            UserService s2 = UserService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @DisplayName("AuthService singleton")
        void testAuthServiceSingleton() {
            AuthService s1 = AuthService.getInstance();
            AuthService s2 = AuthService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @DisplayName("JobService singleton")
        void testJobServiceSingleton() {
            JobService s1 = JobService.getInstance();
            JobService s2 = JobService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }

        @Test
        @DisplayName("ApplicationService singleton")
        void testApplicationServiceSingleton() {
            ApplicationService s1 = ApplicationService.getInstance();
            ApplicationService s2 = ApplicationService.getInstance();
            assertNotNull(s1);
            assertSame(s1, s2);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 5: USER CRUD OPERATIONS (DB)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. User CRUD Operations")
    @TestMethodOrder(OrderAnnotation.class)
    class UserCrudTests {

        private static final String TEST_USERNAME = "skilora_test_user_" + System.currentTimeMillis();
        private static int createdUserId;

        @Test
        @Order(1)
        @DisplayName("Create user")
        void testCreateUser() {
            User user = new User(TEST_USERNAME, "TestPass123!", Role.USER, "Test User");
            user.setEmail(TEST_USERNAME + "@test.com");

            assertDoesNotThrow(() -> UserService.getInstance().create(user));
            assertTrue(user.getId() > 0, "Created user should have a generated ID");
            createdUserId = user.getId();
        }

        @Test
        @Order(2)
        @DisplayName("Find user by username")
        void testFindByUsername() {
            Optional<User> found = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(found.isPresent(), "Should find the created user");
            assertEquals(TEST_USERNAME, found.get().getUsername());
            assertEquals("Test User", found.get().getFullName());
            assertEquals(Role.USER, found.get().getRole());
        }

        @Test
        @Order(3)
        @DisplayName("Find user by ID")
        void testFindById() {
            Optional<User> found = UserService.getInstance().findById(createdUserId);
            assertTrue(found.isPresent(), "Should find user by ID");
            assertEquals(TEST_USERNAME, found.get().getUsername());
        }

        @Test
        @Order(4)
        @DisplayName("Password was hashed on create")
        void testPasswordHashed() {
            Optional<User> found = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(found.isPresent());
            String storedPw = found.get().getPassword();
            assertTrue(storedPw.startsWith("$2a$") || storedPw.startsWith("$2b$"),
                    "Stored password should be BCrypt hashed, got: " + storedPw);
            assertTrue(UserService.verifyPassword("TestPass123!", storedPw),
                    "Original password should verify against hash");
        }

        @Test
        @Order(5)
        @DisplayName("Update user")
        void testUpdateUser() {
            Optional<User> found = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(found.isPresent());
            User user = found.get();
            user.setFullName("Updated Test User");
            user.setEmail("updated_" + TEST_USERNAME + "@test.com");

            assertDoesNotThrow(() -> UserService.getInstance().update(user));

            // Verify update persisted
            Optional<User> updated = UserService.getInstance().findById(createdUserId);
            assertTrue(updated.isPresent());
            assertEquals("Updated Test User", updated.get().getFullName());
        }

        @Test
        @Order(6)
        @DisplayName("findAll returns list including our user")
        void testFindAll() {
            List<User> users = UserService.getInstance().findAll();
            assertNotNull(users);
            assertFalse(users.isEmpty(), "Should have at least 1 user");
            assertTrue(users.stream().anyMatch(u -> u.getUsername().equals(TEST_USERNAME)),
                    "findAll should include our test user");
        }

        @Test
        @Order(7)
        @DisplayName("Delete user")
        void testDeleteUser() {
            assertDoesNotThrow(() -> UserService.getInstance().delete(createdUserId));

            Optional<User> deleted = UserService.getInstance().findById(createdUserId);
            assertFalse(deleted.isPresent(), "User should be deleted");
        }

        @Test
        @Order(8)
        @DisplayName("findByUsername returns empty for non-existent user")
        void testFindNonExistent() {
            Optional<User> found = UserService.getInstance().findByUsername("nonexistent_user_xyz_999");
            assertFalse(found.isPresent());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 6: AUTH SERVICE (DB)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Auth Service")
    @TestMethodOrder(OrderAnnotation.class)
    class AuthServiceTests {

        private static final String AUTH_USERNAME = "skilora_auth_test_" + System.currentTimeMillis();
        private static int authUserId;

        @Test
        @Order(1)
        @DisplayName("Register new user")
        void testRegister() {
            User user = new User(AUTH_USERNAME, "AuthPass123!", Role.USER, "Auth Test");
            user.setEmail(AUTH_USERNAME + "@test.com");
            assertDoesNotThrow(() -> AuthService.getInstance().register(user));
            authUserId = user.getId();
            assertTrue(authUserId > 0);
        }

        @Test
        @Order(2)
        @DisplayName("Register duplicate username throws")
        void testRegisterDuplicate() {
            User dup = new User(AUTH_USERNAME, "other", Role.USER, "Dup");
            assertThrows(RuntimeException.class, () -> AuthService.getInstance().register(dup),
                    "Should throw on duplicate username");
        }

        @Test
        @Order(3)
        @DisplayName("Login with correct credentials")
        void testLoginSuccess() {
            Optional<User> result = AuthService.getInstance().login(AUTH_USERNAME, "AuthPass123!");
            assertTrue(result.isPresent(), "Login should succeed with correct password");
            assertEquals(AUTH_USERNAME, result.get().getUsername());
        }

        @Test
        @Order(4)
        @DisplayName("Login with wrong password fails")
        void testLoginWrongPassword() {
            Optional<User> result = AuthService.getInstance().login(AUTH_USERNAME, "WrongPassword!");
            assertFalse(result.isPresent(), "Login should fail with wrong password");
        }

        @Test
        @Order(5)
        @DisplayName("Login with non-existent user fails")
        void testLoginNonExistent() {
            Optional<User> result = AuthService.getInstance().login("ghost_user_xyz", "any");
            assertFalse(result.isPresent());
        }

        @Test
        @Order(6)
        @DisplayName("getUser returns user by username")
        void testGetUser() {
            Optional<User> result = AuthService.getInstance().getUser(AUTH_USERNAME);
            assertTrue(result.isPresent());
            assertEquals("Auth Test", result.get().getFullName());
        }

        @Test
        @Order(7)
        @DisplayName("Cleanup: delete auth test user")
        void cleanup() {
            assertDoesNotThrow(() -> UserService.getInstance().delete(authUserId));
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 7: JOB OFFER CRUD (DB)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. JobOffer CRUD Operations")
    @TestMethodOrder(OrderAnnotation.class)
    class JobOfferCrudTests {

        private static int testEmployerId;
        private static int testJobId;

        @Test
        @Order(1)
        @DisplayName("Setup: create employer user for job tests")
        void setupEmployer() {
            // We need a company/employer ID. Let's find one or use a safe value.
            // Check if companies table has entries
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT id FROM companies LIMIT 1");
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    testEmployerId = rs.getInt("id");
                } else {
                    // Insert a test company
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO companies (owner_id, name, industry) VALUES (1, 'Test Corp', 'IT')",
                            java.sql.Statement.RETURN_GENERATED_KEYS)) {
                        ins.executeUpdate();
                        ResultSet keys = ins.getGeneratedKeys();
                        if (keys.next()) testEmployerId = keys.getInt(1);
                    }
                }
            } catch (SQLException e) {
                fail("Cannot setup employer: " + e.getMessage());
            }
            assertTrue(testEmployerId > 0, "Should have an employer ID");
        }

        @Test
        @Order(2)
        @DisplayName("Create job offer")
        void testCreateJobOffer() throws SQLException {
            JobOffer job = new JobOffer(testEmployerId, "Test Java Developer", "Tunis");
            job.setDescription("Test job description");
            job.setSalaryMin(2000);
            job.setSalaryMax(4000);
            job.setCurrency("TND");
            job.setWorkType("ONSITE");
            job.setStatus(JobStatus.ACTIVE);
            job.setRequiredSkills(Arrays.asList("Java", "Spring", "SQL"));

            testJobId = JobService.getInstance().createJobOffer(job);
            assertTrue(testJobId > 0, "Should return generated job ID");
        }

        @Test
        @Order(3)
        @DisplayName("Find job offer by ID")
        void testFindById() throws SQLException {
            JobOffer found = JobService.getInstance().findJobOfferById(testJobId);
            assertNotNull(found, "Should find the created job");
            assertEquals("Test Java Developer", found.getTitle());
            assertEquals("Tunis", found.getLocation());
            assertEquals(2000, found.getSalaryMin());
            assertEquals(4000, found.getSalaryMax());
        }

        @Test
        @Order(4)
        @DisplayName("Find job offers by employer (owner)")
        void testFindByEmployer() throws SQLException {
            // findJobOffersByCompanyOwner takes an owner user ID, not a company ID.
            // We look up the owner_id from our test company.
            int ownerUserId = 1; // default fallback
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement("SELECT owner_id FROM companies WHERE id = ?")) {
                stmt.setInt(1, testEmployerId);
                ResultSet rs = stmt.executeQuery();
                if (rs.next()) ownerUserId = rs.getInt("owner_id");
            }
            List<JobOffer> jobs = JobService.getInstance().findJobOffersByCompanyOwner(ownerUserId);
            assertNotNull(jobs, "findJobOffersByCompanyOwner should return a non-null list");
            // The test job should be in this list since it belongs to the test company
            // whose owner is ownerUserId
            if (!jobs.isEmpty()) {
                assertNotNull(jobs.get(0).getTitle(), "Each job should have a title");
            }
        }

        @Test
        @Order(5)
        @DisplayName("Update job offer")
        void testUpdateJobOffer() throws SQLException {
            JobOffer job = JobService.getInstance().findJobOfferById(testJobId);
            assertNotNull(job);
            job.setTitle("Updated Java Developer");
            job.setSalaryMax(5000);

            boolean updated = JobService.getInstance().updateJobOffer(job);
            assertTrue(updated, "Update should succeed");

            JobOffer verify = JobService.getInstance().findJobOfferById(testJobId);
            assertEquals("Updated Java Developer", verify.getTitle());
            assertEquals(5000, verify.getSalaryMax());
        }

        @Test
        @Order(6)
        @DisplayName("Delete job offer")
        void testDeleteJobOffer() throws SQLException {
            boolean deleted = JobService.getInstance().deleteJobOffer(testJobId);
            assertTrue(deleted, "Delete should succeed");

            JobOffer verify = JobService.getInstance().findJobOfferById(testJobId);
            assertNull(verify, "Deleted job should not be found");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 8: APPLICATION CRUD (DB)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Application CRUD Operations")
    @TestMethodOrder(OrderAnnotation.class)
    class ApplicationCrudTests {

        private static int testJobOfferId;
        private static int testProfileId;
        private static int testAppId;
        private static int testCompanyId;

        @Test
        @Order(1)
        @DisplayName("Setup: create job & profile for application tests")
        void setup() throws SQLException {
            // Get or create a company
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM companies LIMIT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        testCompanyId = rs.getInt("id");
                    } else {
                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO companies (owner_id, name, industry) VALUES (1, 'App Test Corp', 'IT')",
                                java.sql.Statement.RETURN_GENERATED_KEYS)) {
                            ins.executeUpdate();
                            ResultSet keys = ins.getGeneratedKeys();
                            if (keys.next()) testCompanyId = keys.getInt(1);
                        }
                    }
                }

                // Create a job offer
                JobOffer job = new JobOffer(testCompanyId, "Application Test Job", "Tunis");
                job.setStatus(JobStatus.ACTIVE);
                testJobOfferId = JobService.getInstance().createJobOffer(job);

                // Get or create a profile
                try (PreparedStatement stmt = conn.prepareStatement("SELECT id FROM profiles LIMIT 1");
                     ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        testProfileId = rs.getInt("id");
                    } else {
                        // Create a user + profile
                        try (PreparedStatement ins = conn.prepareStatement(
                                "INSERT INTO profiles (user_id, bio) VALUES (1, 'Test')",
                                java.sql.Statement.RETURN_GENERATED_KEYS)) {
                            ins.executeUpdate();
                            ResultSet keys = ins.getGeneratedKeys();
                            if (keys.next()) testProfileId = keys.getInt(1);
                        }
                    }
                }
            }
            assertTrue(testJobOfferId > 0);
            assertTrue(testProfileId > 0);
        }

        @Test
        @Order(2)
        @DisplayName("Apply to job")
        void testApply() throws SQLException {
            testAppId = ApplicationService.getInstance().apply(testJobOfferId, testProfileId, "Test cover letter");
            assertTrue(testAppId > 0, "Application ID should be > 0");
        }

        @Test
        @Order(3)
        @DisplayName("hasApplied returns true after applying")
        void testHasApplied() throws SQLException {
            assertTrue(ApplicationService.getInstance().hasApplied(testJobOfferId, testProfileId),
                    "hasApplied should return true");
        }

        @Test
        @Order(4)
        @DisplayName("Duplicate application returns -1")
        void testDuplicateApply() throws SQLException {
            int result = ApplicationService.getInstance().apply(testJobOfferId, testProfileId, "Duplicate");
            assertEquals(-1, result, "Duplicate application should return -1");
        }

        @Test
        @Order(5)
        @DisplayName("Get application by ID")
        void testGetById() throws SQLException {
            Optional<Application> found = ApplicationService.getInstance().getById(testAppId);
            assertTrue(found.isPresent(), "Should find the application");
            assertEquals(Status.PENDING, found.get().getStatus());
            assertEquals(testJobOfferId, found.get().getJobOfferId());
        }

        @Test
        @Order(6)
        @DisplayName("Update application status")
        void testUpdateStatus() throws SQLException {
            boolean updated = ApplicationService.getInstance().updateStatus(testAppId, Status.REVIEWING);
            assertTrue(updated, "Status update should succeed");

            Optional<Application> found = ApplicationService.getInstance().getById(testAppId);
            assertTrue(found.isPresent());
            assertEquals(Status.REVIEWING, found.get().getStatus());
        }

        @Test
        @Order(7)
        @DisplayName("Get applications by profile")
        void testGetByProfile() throws SQLException {
            List<Application> apps = ApplicationService.getInstance().getApplicationsByProfile(testProfileId);
            assertNotNull(apps);
            assertTrue(apps.stream().anyMatch(a -> a.getId() == testAppId));
        }

        @Test
        @Order(8)
        @DisplayName("Get applications by job offer")
        void testGetByJobOffer() throws SQLException {
            List<Application> apps = ApplicationService.getInstance().getApplicationsByJobOffer(testJobOfferId);
            assertNotNull(apps);
            assertTrue(apps.stream().anyMatch(a -> a.getId() == testAppId));
        }

        @Test
        @Order(9)
        @DisplayName("Delete application")
        void testDeleteApplication() throws SQLException {
            boolean deleted = ApplicationService.getInstance().delete(testAppId);
            assertTrue(deleted);

            Optional<Application> found = ApplicationService.getInstance().getById(testAppId);
            assertFalse(found.isPresent(), "Deleted application should not be found");
        }

        @Test
        @Order(10)
        @DisplayName("Cleanup: delete test job offer")
        void cleanup() throws SQLException {
            JobService.getInstance().deleteJobOffer(testJobOfferId);
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 9: DIRECT SQL OPERATIONS (DB integrity checks)
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. Direct SQL Operations")
    class DirectSqlTests {

        @Test
        @DisplayName("INSERT, SELECT, UPDATE, DELETE on users table")
        void testCrudCycle() throws SQLException {
            String uniqueName = "sql_test_" + System.currentTimeMillis();
            int insertedId;

            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                // INSERT
                try (PreparedStatement ins = conn.prepareStatement(
                        "INSERT INTO users (username, password, role, full_name, is_active) VALUES (?, ?, 'USER', ?, TRUE)",
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {
                    ins.setString(1, uniqueName);
                    ins.setString(2, "test_hash");
                    ins.setString(3, "SQL Test User");
                    assertEquals(1, ins.executeUpdate(), "Should insert 1 row");
                    ResultSet keys = ins.getGeneratedKeys();
                    assertTrue(keys.next());
                    insertedId = keys.getInt(1);
                }

                // SELECT
                try (PreparedStatement sel = conn.prepareStatement("SELECT * FROM users WHERE id = ?")) {
                    sel.setInt(1, insertedId);
                    ResultSet rs = sel.executeQuery();
                    assertTrue(rs.next(), "Should find inserted row");
                    assertEquals(uniqueName, rs.getString("username"));
                    assertEquals("SQL Test User", rs.getString("full_name"));
                }

                // UPDATE
                try (PreparedStatement upd = conn.prepareStatement("UPDATE users SET full_name = ? WHERE id = ?")) {
                    upd.setString(1, "Updated SQL User");
                    upd.setInt(2, insertedId);
                    assertEquals(1, upd.executeUpdate(), "Should update 1 row");
                }

                // Verify UPDATE
                try (PreparedStatement sel = conn.prepareStatement("SELECT full_name FROM users WHERE id = ?")) {
                    sel.setInt(1, insertedId);
                    ResultSet rs = sel.executeQuery();
                    assertTrue(rs.next());
                    assertEquals("Updated SQL User", rs.getString("full_name"));
                }

                // DELETE
                try (PreparedStatement del = conn.prepareStatement("DELETE FROM users WHERE id = ?")) {
                    del.setInt(1, insertedId);
                    assertEquals(1, del.executeUpdate(), "Should delete 1 row");
                }

                // Verify DELETE
                try (PreparedStatement sel = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE id = ?")) {
                    sel.setInt(1, insertedId);
                    ResultSet rs = sel.executeQuery();
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Deleted row should not exist");
                }
            }
        }

        @Test
        @DisplayName("Transaction rollback works")
        void testTransactionRollback() throws SQLException {
            String uniqueName = "tx_test_" + System.currentTimeMillis();

            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ins = conn.prepareStatement(
                            "INSERT INTO users (username, password, role, full_name, is_active) VALUES (?, 'x', 'USER', 'TX Test', TRUE)")) {
                        ins.setString(1, uniqueName);
                        ins.executeUpdate();
                    }
                    // Rollback instead of commit
                    conn.rollback();
                } finally {
                    conn.setAutoCommit(true);
                }

                // Verify row was NOT persisted
                try (PreparedStatement sel = conn.prepareStatement("SELECT COUNT(*) FROM users WHERE username = ?")) {
                    sel.setString(1, uniqueName);
                    ResultSet rs = sel.executeQuery();
                    assertTrue(rs.next());
                    assertEquals(0, rs.getInt(1), "Rolled-back row should not exist");
                }
            }
        }

        @Test
        @DisplayName("Multiple connections from pool work")
        void testConnectionPool() throws SQLException {
            Connection c1 = DatabaseConfig.getInstance().getConnection();
            Connection c2 = DatabaseConfig.getInstance().getConnection();
            assertNotNull(c1);
            assertNotNull(c2);
            assertFalse(c1.isClosed());
            assertFalse(c2.isClosed());
            c1.close();
            c2.close();
        }

        @Test
        @DisplayName("Foreign key constraint prevents orphan records")
        void testForeignKeyConstraint() {
            // Trying to insert an application referencing a non-existent job should fail
            assertThrows(Exception.class, () -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     PreparedStatement stmt = conn.prepareStatement(
                             "INSERT INTO applications (job_offer_id, candidate_profile_id, status) VALUES (999999, 999999, 'PENDING')")) {
                    stmt.executeUpdate();
                }
            }, "Should fail due to FK constraint on non-existent job/profile");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  SECTION 10: JOB FEED CACHE
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("10. Job Feed Cache")
    class JobFeedCacheTests {

        @Test
        @DisplayName("getJobsFromCache returns a list (possibly empty)")
        void testCacheReturnsList() {
            List<JobOpportunity> jobs = JobService.getInstance().getJobsFromCache();
            assertNotNull(jobs, "Cache should never return null");
        }

        @Test
        @DisplayName("getJobsFromCache returns a defensive copy")
        void testCacheDefensiveCopy() {
            List<JobOpportunity> list1 = JobService.getInstance().getJobsFromCache();
            List<JobOpportunity> list2 = JobService.getInstance().getJobsFromCache();
            assertNotSame(list1, list2, "Each call should return a new list (defensive copy)");
        }
    }
}
