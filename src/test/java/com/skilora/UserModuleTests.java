package com.skilora;

import com.skilora.config.DatabaseConfig;

// === User Module Entities ===
import com.skilora.user.entity.User;
import com.skilora.user.entity.Profile;
import com.skilora.user.entity.Skill;
import com.skilora.user.entity.Experience;
import com.skilora.user.entity.JobPreference;
import com.skilora.user.entity.BiometricData;

// === User Module Enums ===
import com.skilora.user.enums.Role;
import com.skilora.user.enums.ProficiencyLevel;
import com.skilora.recruitment.enums.WorkType;

// === User Module Services ===
import com.skilora.user.service.UserService;
import com.skilora.user.service.AuthService;
import com.skilora.user.service.ProfileService;
import com.skilora.user.service.PreferencesService;
import com.skilora.user.service.BiometricService;

// === Utilities ===
import com.skilora.utils.ImageUtils;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.ClassOrderer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════════════╗
 *   SKILORA - User Module Test Suite
 * ──────────────────────────────────────────────────────────────────────
 *   Comprehensive tests for the User module covering:
 *   • Database connectivity
 *   • Entity getters/setters & constructors
 *   • Enum values & utility methods
 *   • UserService CRUD (Create, Read, Update, Delete)
 *   • AuthService (login, registration, lockout)
 *   • ProfileService CRUD (profiles, skills, experiences)
 *   • PreferencesService CRUD
 *   • BiometricService (hasBiometricData)
 *   • Password hashing & verification
 *   • Input validation (Validators utility)
 *   • Edge cases & error handling
 * ╚══════════════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DisplayName("User Module Tests")
class UserModuleTests {

    // Test data constants
    private static final String TEST_USERNAME = "test_user_junit_" + System.currentTimeMillis();
    private static final String TEST_EMAIL = "junit_" + System.currentTimeMillis() + "@test.com";
    private static final String TEST_PASSWORD = "Test@1234";
    private static final String TEST_FULLNAME = "JUnit Test User";

    // Track created IDs for cleanup
    private static int createdUserId = -1;
    private static int createdProfileId = -1;


    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 1: DATABASE CONNECTION
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(1)
    @DisplayName("1. Database Connection")
    @TestMethodOrder(OrderAnnotation.class)
    class DatabaseConnectionTests {

        @Test
        @Order(1)
        @DisplayName("1.1 DatabaseConfig singleton returns same instance")
        void testSingletonInstance() {
            DatabaseConfig db1 = DatabaseConfig.getInstance();
            DatabaseConfig db2 = DatabaseConfig.getInstance();
            assertSame(db1, db2, "DatabaseConfig should return the same singleton instance");
        }

        @Test
        @Order(2)
        @DisplayName("1.2 Database connection is established and open")
        void testConnectionIsOpen() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                assertNotNull(conn, "Connection should not be null");
                assertFalse(conn.isClosed(), "Connection should be open");
            }
        }

        @Test
        @Order(3)
        @DisplayName("1.3 Database URL references 'skilora'")
        void testDatabaseUrl() {
            String url = DatabaseConfig.getInstance().getUrl();
            assertNotNull(url, "Database URL should not be null");
            assertTrue(url.contains("skilora"), "URL should reference 'skilora' database");
        }

        @Test
        @Order(4)
        @DisplayName("1.4 Can execute a simple query (SELECT 1)")
        void testSimpleQuery() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 var stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT 1")) {
                assertTrue(rs.next(), "Should return a result for SELECT 1");
                assertEquals(1, rs.getInt(1));
            }
        }

        @Test
        @Order(5)
        @DisplayName("1.5 Users table exists in the database")
        void testUsersTableExists() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, null, "users", null)) {
                assertTrue(rs.next(), "Table 'users' should exist");
            }
        }

        @Test
        @Order(6)
        @DisplayName("1.6 Profiles table exists in the database")
        void testProfilesTableExists() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, null, "profiles", null)) {
                assertTrue(rs.next(), "Table 'profiles' should exist");
            }
        }

        @Test
        @Order(7)
        @DisplayName("1.7 Skills table exists in the database")
        void testSkillsTableExists() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, null, "skills", null)) {
                assertTrue(rs.next(), "Table 'skills' should exist");
            }
        }

        @Test
        @Order(8)
        @DisplayName("1.8 Biometric_data table exists in the database")
        void testBiometricTableExists() throws SQLException {
            try (Connection conn = DatabaseConfig.getInstance().getConnection();
                 ResultSet rs = conn.getMetaData().getTables(null, null, "biometric_data", null)) {
                assertTrue(rs.next(), "Table 'biometric_data' should exist");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 2: ENUMS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(2)
    @DisplayName("2. User Enums")
    @TestMethodOrder(OrderAnnotation.class)
    class EnumTests {

        // --- Role Enum ---

        @Test
        @Order(1)
        @DisplayName("2.1 Role enum has 4 values")
        void testRoleCount() {
            assertEquals(4, Role.values().length, "Role should have 4 values");
        }

        @Test
        @Order(2)
        @DisplayName("2.2 Role enum contains ADMIN, USER, EMPLOYER, TRAINER")
        void testRoleValues() {
            assertNotNull(Role.ADMIN);
            assertNotNull(Role.USER);
            assertNotNull(Role.EMPLOYER);
            assertNotNull(Role.TRAINER);
        }

        @Test
        @Order(3)
        @DisplayName("2.3 Role display names are correct")
        void testRoleDisplayNames() {
            assertEquals("Administrator", Role.ADMIN.getDisplayName());
            assertEquals("Job Seeker", Role.USER.getDisplayName());
            assertEquals("Employer", Role.EMPLOYER.getDisplayName());
            assertEquals("Trainer", Role.TRAINER.getDisplayName());
        }

        @Test
        @Order(4)
        @DisplayName("2.4 Role valueOf works correctly")
        void testRoleValueOf() {
            assertEquals(Role.ADMIN, Role.valueOf("ADMIN"));
            assertEquals(Role.USER, Role.valueOf("USER"));
            assertThrows(IllegalArgumentException.class, () -> Role.valueOf("INVALID"));
        }

        // --- ProficiencyLevel Enum ---

        @Test
        @Order(5)
        @DisplayName("2.5 ProficiencyLevel enum has 4 values")
        void testProficiencyLevelCount() {
            assertEquals(4, ProficiencyLevel.values().length);
        }

        @Test
        @Order(6)
        @DisplayName("2.6 ProficiencyLevel display names and levels are correct")
        void testProficiencyLevelDetails() {
            assertEquals("Beginner", ProficiencyLevel.BEGINNER.getDisplayName());
            assertEquals(1, ProficiencyLevel.BEGINNER.getLevel());
            assertEquals("Intermediate", ProficiencyLevel.INTERMEDIATE.getDisplayName());
            assertEquals(2, ProficiencyLevel.INTERMEDIATE.getLevel());
            assertEquals("Advanced", ProficiencyLevel.ADVANCED.getDisplayName());
            assertEquals(3, ProficiencyLevel.ADVANCED.getLevel());
            assertEquals("Expert", ProficiencyLevel.EXPERT.getDisplayName());
            assertEquals(4, ProficiencyLevel.EXPERT.getLevel());
        }

        @Test
        @Order(7)
        @DisplayName("2.7 ProficiencyLevel.fromLevel() resolves correctly")
        void testProficiencyFromLevel() {
            assertEquals(ProficiencyLevel.BEGINNER, ProficiencyLevel.fromLevel(1));
            assertEquals(ProficiencyLevel.INTERMEDIATE, ProficiencyLevel.fromLevel(2));
            assertEquals(ProficiencyLevel.ADVANCED, ProficiencyLevel.fromLevel(3));
            assertEquals(ProficiencyLevel.EXPERT, ProficiencyLevel.fromLevel(4));
            assertEquals(ProficiencyLevel.BEGINNER, ProficiencyLevel.fromLevel(999), "Invalid level should default to BEGINNER");
        }

        // --- WorkType Enum ---

        @Test
        @Order(8)
        @DisplayName("2.8 WorkType enum has 6 values")
        void testWorkTypeCount() {
            assertEquals(6, WorkType.values().length);
        }

        @ParameterizedTest
        @EnumSource(WorkType.class)
        @Order(9)
        @DisplayName("2.9 All WorkType values have non-null display names")
        void testWorkTypeDisplayNames(WorkType type) {
            assertNotNull(type.getDisplayName(), type.name() + " should have a display name");
            assertFalse(type.getDisplayName().isEmpty(), type.name() + " display name should not be empty");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 3: USER ENTITY (GETTERS/SETTERS)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(3)
    @DisplayName("3. User Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class UserEntityTests {

        @Test
        @Order(1)
        @DisplayName("3.1 Default constructor sets active=true")
        void testDefaultConstructor() {
            User user = new User();
            assertTrue(user.isActive(), "New user should be active by default");
        }

        @Test
        @Order(2)
        @DisplayName("3.2 Parameterized constructor sets fields correctly")
        void testParameterizedConstructor() {
            User user = new User("testUser", "pass123", Role.USER, "Test User");
            assertEquals("testUser", user.getUsername());
            assertEquals("pass123", user.getPassword());
            assertEquals(Role.USER, user.getRole());
            assertEquals("Test User", user.getFullName());
            assertTrue(user.isActive());
        }

        @Test
        @Order(3)
        @DisplayName("3.3 All getters/setters work correctly")
        void testGettersSetters() {
            User user = new User();

            user.setId(42);
            assertEquals(42, user.getId());

            user.setUsername("alice");
            assertEquals("alice", user.getUsername());

            user.setEmail("alice@skilora.com");
            assertEquals("alice@skilora.com", user.getEmail());

            user.setPassword("secret");
            assertEquals("secret", user.getPassword());

            user.setRole(Role.EMPLOYER);
            assertEquals(Role.EMPLOYER, user.getRole());

            user.setFullName("Alice Wonderland");
            assertEquals("Alice Wonderland", user.getFullName());

            user.setPhotoUrl("/photos/alice.jpg");
            assertEquals("/photos/alice.jpg", user.getPhotoUrl());

            user.setVerified(true);
            assertTrue(user.isVerified());

            user.setActive(false);
            assertFalse(user.isActive());
        }

        @Test
        @Order(4)
        @DisplayName("3.4 toString includes full name and role display name")
        void testToString() {
            User user = new User("bob", "pass", Role.TRAINER, "Bob Builder");
            String str = user.toString();
            assertTrue(str.contains("Bob Builder"), "toString should include full name");
            assertTrue(str.contains("Trainer"), "toString should include role display name");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 4: PROFILE ENTITY (GETTERS/SETTERS)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(4)
    @DisplayName("4. Profile Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class ProfileEntityTests {

        @Test
        @Order(1)
        @DisplayName("4.1 Default constructor creates empty profile")
        void testDefaultConstructor() {
            Profile profile = new Profile();
            assertEquals(0, profile.getId());
            assertEquals(0, profile.getUserId());
            assertNull(profile.getFirstName());
        }

        @Test
        @Order(2)
        @DisplayName("4.2 Parameterized constructor sets userId, firstName, lastName")
        void testParameterizedConstructor() {
            Profile profile = new Profile(1, "Jane", "Doe");
            assertEquals(1, profile.getUserId());
            assertEquals("Jane", profile.getFirstName());
            assertEquals("Doe", profile.getLastName());
        }

        @Test
        @Order(3)
        @DisplayName("4.3 All getters/setters work correctly")
        void testGettersSetters() {
            Profile p = new Profile();
            p.setId(10);
            p.setUserId(5);
            p.setFirstName("Alice");
            p.setLastName("Smith");
            p.setPhone("+21612345678");
            p.setPhotoUrl("/img/alice.png");
            p.setCvUrl("/cv/alice.pdf");
            p.setLocation("Tunis");
            p.setBirthDate(LocalDate.of(1998, 5, 20));

            assertEquals(10, p.getId());
            assertEquals(5, p.getUserId());
            assertEquals("Alice", p.getFirstName());
            assertEquals("Smith", p.getLastName());
            assertEquals("+21612345678", p.getPhone());
            assertEquals("/img/alice.png", p.getPhotoUrl());
            assertEquals("/cv/alice.pdf", p.getCvUrl());
            assertEquals("Tunis", p.getLocation());
            assertEquals(LocalDate.of(1998, 5, 20), p.getBirthDate());
        }

        @Test
        @Order(4)
        @DisplayName("4.4 getFullName combines first and last name")
        void testGetFullName() {
            Profile p = new Profile(1, "Jane", "Doe");
            assertEquals("Jane Doe", p.getFullName());
        }

        @Test
        @Order(5)
        @DisplayName("4.5 getFullName handles null names")
        void testGetFullNameNulls() {
            Profile p = new Profile();
            assertEquals("", p.getFullName());

            p.setFirstName("Solo");
            assertEquals("Solo", p.getFullName());
        }

        @Test
        @Order(6)
        @DisplayName("4.6 equals and hashCode are consistent")
        void testEqualsAndHashCode() {
            Profile p1 = new Profile();
            p1.setId(1);
            p1.setUserId(10);

            Profile p2 = new Profile();
            p2.setId(1);
            p2.setUserId(10);

            assertEquals(p1, p2);
            assertEquals(p1.hashCode(), p2.hashCode());

            p2.setId(2);
            assertNotEquals(p1, p2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 5: SKILL ENTITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(5)
    @DisplayName("5. Skill Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class SkillEntityTests {

        @Test
        @Order(1)
        @DisplayName("5.1 Default constructor values")
        void testDefaultConstructor() {
            Skill skill = new Skill();
            assertEquals(0, skill.getId());
            assertNull(skill.getSkillName());
            assertFalse(skill.isVerified());
        }

        @Test
        @Order(2)
        @DisplayName("5.2 Two-arg constructor sets defaults")
        void testTwoArgConstructor() {
            Skill skill = new Skill(1, "Java");
            assertEquals(1, skill.getProfileId());
            assertEquals("Java", skill.getSkillName());
            assertEquals(ProficiencyLevel.BEGINNER, skill.getProficiencyLevel());
            assertEquals(0, skill.getYearsExperience());
            assertFalse(skill.isVerified());
        }

        @Test
        @Order(3)
        @DisplayName("5.3 Four-arg constructor sets all fields")
        void testFourArgConstructor() {
            Skill skill = new Skill(2, "Python", ProficiencyLevel.ADVANCED, 5);
            assertEquals(2, skill.getProfileId());
            assertEquals("Python", skill.getSkillName());
            assertEquals(ProficiencyLevel.ADVANCED, skill.getProficiencyLevel());
            assertEquals(5, skill.getYearsExperience());
        }

        @Test
        @Order(4)
        @DisplayName("5.4 All getters/setters work")
        void testGettersSetters() {
            Skill s = new Skill();
            s.setId(99);
            s.setProfileId(7);
            s.setSkillName("Docker");
            s.setProficiencyLevel(ProficiencyLevel.EXPERT);
            s.setYearsExperience(8);
            s.setVerified(true);

            assertEquals(99, s.getId());
            assertEquals(7, s.getProfileId());
            assertEquals("Docker", s.getSkillName());
            assertEquals(ProficiencyLevel.EXPERT, s.getProficiencyLevel());
            assertEquals(8, s.getYearsExperience());
            assertTrue(s.isVerified());
        }

        @Test
        @Order(5)
        @DisplayName("5.5 equals uses id comparison")
        void testEquals() {
            Skill s1 = new Skill();
            s1.setId(5);
            Skill s2 = new Skill();
            s2.setId(5);
            assertEquals(s1, s2);

            s2.setId(6);
            assertNotEquals(s1, s2);
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 6: EXPERIENCE ENTITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(6)
    @DisplayName("6. Experience Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class ExperienceEntityTests {

        @Test
        @Order(1)
        @DisplayName("6.1 Default constructor")
        void testDefaultConstructor() {
            Experience exp = new Experience();
            assertEquals(0, exp.getId());
            assertNull(exp.getCompany());
            assertFalse(exp.isCurrentJob());
        }

        @Test
        @Order(2)
        @DisplayName("6.2 Parameterized constructor sets fields")
        void testParameterizedConstructor() {
            LocalDate start = LocalDate.of(2023, 1, 15);
            Experience exp = new Experience(1, "Google", "Software Engineer", start);
            assertEquals(1, exp.getProfileId());
            assertEquals("Google", exp.getCompany());
            assertEquals("Software Engineer", exp.getPosition());
            assertEquals(start, exp.getStartDate());
            assertFalse(exp.isCurrentJob());
        }

        @Test
        @Order(3)
        @DisplayName("6.3 setCurrentJob(true) clears endDate")
        void testCurrentJobClearsEndDate() {
            Experience exp = new Experience();
            exp.setEndDate(LocalDate.of(2024, 6, 1));
            assertNotNull(exp.getEndDate());

            exp.setCurrentJob(true);
            assertTrue(exp.isCurrentJob());
            assertNull(exp.getEndDate(), "endDate should be null when currentJob is true");
        }

        @Test
        @Order(4)
        @DisplayName("6.4 getDurationInMonths calculates correctly")
        void testGetDurationInMonths() {
            Experience exp = new Experience();
            exp.setStartDate(LocalDate.of(2023, 1, 1));
            exp.setEndDate(LocalDate.of(2023, 7, 1));
            assertEquals(6, exp.getDurationInMonths());
        }

        @Test
        @Order(5)
        @DisplayName("6.5 getDurationInMonths returns 0 for null dates")
        void testGetDurationInMonthsNullDates() {
            Experience exp = new Experience();
            assertEquals(0, exp.getDurationInMonths());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 7: JOB PREFERENCE ENTITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(7)
    @DisplayName("7. JobPreference Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class JobPreferenceEntityTests {

        @Test
        @Order(1)
        @DisplayName("7.1 Default constructor sets FULL_TIME and remoteWork=false")
        void testDefaultConstructor() {
            JobPreference pref = new JobPreference();
            assertEquals(WorkType.FULL_TIME, pref.getWorkType());
            assertFalse(pref.isRemoteWork());
        }

        @Test
        @Order(2)
        @DisplayName("7.2 All getters/setters work")
        void testGettersSetters() {
            JobPreference p = new JobPreference();
            p.setId(1);
            p.setProfileId(5);
            p.setDesiredPosition("Backend Developer");
            p.setMinSalary(3000);
            p.setMaxSalary(5000);
            p.setWorkType(WorkType.FREELANCE);
            p.setLocationPreference("Tunis");
            p.setRemoteWork(true);

            assertEquals(1, p.getId());
            assertEquals(5, p.getProfileId());
            assertEquals("Backend Developer", p.getDesiredPosition());
            assertEquals(3000, p.getMinSalary());
            assertEquals(5000, p.getMaxSalary());
            assertEquals(WorkType.FREELANCE, p.getWorkType());
            assertEquals("Tunis", p.getLocationPreference());
            assertTrue(p.isRemoteWork());
        }

        @Test
        @Order(3)
        @DisplayName("7.3 getExpectedSalaryRange formats correctly")
        void testExpectedSalaryRange() {
            JobPreference p = new JobPreference();

            p.setMinSalary(2000);
            p.setMaxSalary(4000);
            assertEquals("2000 - 4000", p.getExpectedSalaryRange());

            p.setMaxSalary(0);
            assertEquals("From 2000", p.getExpectedSalaryRange());

            p.setMinSalary(0);
            p.setMaxSalary(5000);
            assertEquals("Up to 5000", p.getExpectedSalaryRange());

            p.setMaxSalary(0);
            assertEquals("Negotiable", p.getExpectedSalaryRange());
        }

        @Test
        @Order(4)
        @DisplayName("7.4 Full constructor sets all fields")
        void testFullConstructor() {
            JobPreference p = new JobPreference(3, "DevOps", 4000, 7000,
                    WorkType.CONTRACT, "Sousse", true);
            assertEquals(3, p.getProfileId());
            assertEquals("DevOps", p.getDesiredPosition());
            assertEquals(4000, p.getMinSalary());
            assertEquals(7000, p.getMaxSalary());
            assertEquals(WorkType.CONTRACT, p.getWorkType());
            assertEquals("Sousse", p.getLocationPreference());
            assertTrue(p.isRemoteWork());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 8: BIOMETRIC DATA ENTITY
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(8)
    @DisplayName("8. BiometricData Entity")
    @TestMethodOrder(OrderAnnotation.class)
    class BiometricDataEntityTests {

        @Test
        @Order(1)
        @DisplayName("8.1 Default constructor")
        void testDefaultConstructor() {
            BiometricData bd = new BiometricData();
            assertNull(bd.getUsername());
            assertNull(bd.getFaceEncodingJson());
        }

        @Test
        @Order(2)
        @DisplayName("8.2 Parameterized constructor sets fields and timestamp")
        void testParameterizedConstructor() {
            BiometricData bd = new BiometricData("testUser", "[1.0, 2.0, 3.0]");
            assertEquals("testUser", bd.getUsername());
            assertEquals("[1.0, 2.0, 3.0]", bd.getFaceEncodingJson());
            assertTrue(bd.getRegisteredAt() > 0, "registeredAt should be set to current time");
        }

        @Test
        @Order(3)
        @DisplayName("8.3 Setters work correctly")
        void testSetters() {
            BiometricData bd = new BiometricData();
            bd.setUsername("alice");
            bd.setFaceEncodingJson("{encoding}");
            bd.setRegisteredAt(123456789L);

            assertEquals("alice", bd.getUsername());
            assertEquals("{encoding}", bd.getFaceEncodingJson());
            assertEquals(123456789L, bd.getRegisteredAt());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 9: PASSWORD HASHING
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(9)
    @DisplayName("9. Password Hashing")
    @TestMethodOrder(OrderAnnotation.class)
    class PasswordHashingTests {

        @Test
        @Order(1)
        @DisplayName("9.1 hashPassword returns a BCrypt hash")
        void testHashPasswordFormat() {
            String hash = UserService.hashPassword("MyPassword1!");
            assertNotNull(hash);
            assertTrue(hash.startsWith("$2a$"), "Hash should start with $2a$ (BCrypt)");
        }

        @Test
        @Order(2)
        @DisplayName("9.2 hashPassword produces different hashes for same input (salted)")
        void testHashIsSalted() {
            String hash1 = UserService.hashPassword("SamePassword");
            String hash2 = UserService.hashPassword("SamePassword");
            assertNotEquals(hash1, hash2, "BCrypt hashes should be unique due to salting");
        }

        @Test
        @Order(3)
        @DisplayName("9.3 verifyPassword matches correct password")
        void testVerifyPasswordCorrect() {
            String hash = UserService.hashPassword("Correct@123");
            assertTrue(UserService.verifyPassword("Correct@123", hash));
        }

        @Test
        @Order(4)
        @DisplayName("9.4 verifyPassword rejects wrong password")
        void testVerifyPasswordWrong() {
            String hash = UserService.hashPassword("Correct@123");
            assertFalse(UserService.verifyPassword("Wrong@456", hash));
        }

        @Test
        @Order(5)
        @DisplayName("9.5 verifyPassword handles legacy plaintext passwords")
        void testVerifyPasswordLegacy() {
            // Legacy passwords are not BCrypt-hashed
            assertTrue(UserService.verifyPassword("plain123", "plain123"));
            assertFalse(UserService.verifyPassword("plain123", "differentPlain"));
        }

        @Test
        @Order(6)
        @DisplayName("9.6 verifyPassword handles null inputs")
        void testVerifyPasswordNulls() {
            assertFalse(UserService.verifyPassword(null, "hash"));
            assertFalse(UserService.verifyPassword("pass", null));
            assertFalse(UserService.verifyPassword(null, null));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 10: INPUT VALIDATION (Validators)
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(10)
    @DisplayName("10. Input Validation")
    @TestMethodOrder(OrderAnnotation.class)
    class ValidationTests {

        // --- Username Validation ---

        @Test
        @Order(1)
        @DisplayName("10.1 Valid username passes")
        void testValidUsername() {
            assertNull(com.skilora.utils.Validators.validateUsername("john_doe"),
                    "Valid username should return null");
        }

        @Test
        @Order(2)
        @DisplayName("10.2 Username too short fails")
        void testUsernameTooShort() {
            assertNotNull(com.skilora.utils.Validators.validateUsername("ab"));
        }

        @Test
        @Order(3)
        @DisplayName("10.3 Username with special chars fails")
        void testUsernameSpecialChars() {
            assertNotNull(com.skilora.utils.Validators.validateUsername("user@name"));
        }

        @ParameterizedTest
        @NullAndEmptySource
        @Order(4)
        @DisplayName("10.4 Null/empty username fails")
        void testUsernameNullEmpty(String username) {
            assertNotNull(com.skilora.utils.Validators.validateUsername(username));
        }

        // --- Email Validation ---

        @Test
        @Order(5)
        @DisplayName("10.5 Valid email passes")
        void testValidEmail() {
            assertNull(com.skilora.utils.Validators.validateEmail("user@example.com"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"notanemail", "missing@", "@no-user.com", "a@b"})
        @Order(6)
        @DisplayName("10.6 Invalid email formats fail")
        void testInvalidEmails(String email) {
            assertNotNull(com.skilora.utils.Validators.validateEmail(email),
                    "'" + email + "' should fail validation");
        }

        // --- Password Validation ---

        @Test
        @Order(7)
        @DisplayName("10.7 Strong password passes")
        void testStrongPassword() {
            assertNull(com.skilora.utils.Validators.validatePasswordStrength("Str0ng@Pass"));
        }

        @Test
        @Order(8)
        @DisplayName("10.8 Weak password fails (no uppercase)")
        void testWeakPasswordNoUppercase() {
            assertNotNull(com.skilora.utils.Validators.validatePasswordStrength("weak@1234"));
        }

        @Test
        @Order(9)
        @DisplayName("10.9 Short password fails (<8 chars)")
        void testShortPassword() {
            assertNotNull(com.skilora.utils.Validators.validatePasswordStrength("Ab@1"));
        }

        @Test
        @Order(10)
        @DisplayName("10.10 Password without special char fails")
        void testPasswordNoSpecial() {
            assertNotNull(com.skilora.utils.Validators.validatePasswordStrength("Password123"));
        }

        // --- Full Name Validation ---

        @Test
        @Order(11)
        @DisplayName("10.11 Valid full name passes")
        void testValidFullName() {
            assertNull(com.skilora.utils.Validators.validateFullName("Jean-Pierre O'Brien"));
        }

        @Test
        @Order(12)
        @DisplayName("10.12 Full name too short fails")
        void testFullNameTooShort() {
            assertNotNull(com.skilora.utils.Validators.validateFullName("A"));
        }

        // --- Phone Validation ---

        @Test
        @Order(13)
        @DisplayName("10.13 Valid phone passes")
        void testValidPhone() {
            assertNull(com.skilora.utils.Validators.validatePhone("+216 12 345 678"));
        }

        @Test
        @Order(14)
        @DisplayName("10.14 Empty phone passes (optional field)")
        void testEmptyPhoneOptional() {
            assertNull(com.skilora.utils.Validators.validatePhone(""));
            assertNull(com.skilora.utils.Validators.validatePhone(null));
        }

        @Test
        @Order(15)
        @DisplayName("10.15 Phone with letters fails")
        void testPhoneWithLetters() {
            assertNotNull(com.skilora.utils.Validators.validatePhone("123abc456"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 11: USER SERVICE — CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(11)
    @DisplayName("11. UserService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class UserServiceCrudTests {

        @Test
        @Order(1)
        @DisplayName("11.1 UserService singleton returns same instance")
        void testSingleton() {
            UserService s1 = UserService.getInstance();
            UserService s2 = UserService.getInstance();
            assertSame(s1, s2);
        }

        @Test
        @Order(2)
        @DisplayName("11.2 CREATE — Insert a new test user")
        void testCreateUser() {
            User user = new User(TEST_USERNAME, TEST_PASSWORD, Role.USER, TEST_FULLNAME);
            user.setEmail(TEST_EMAIL);
            user.setActive(true);

            assertDoesNotThrow(() -> UserService.getInstance().create(user));
            assertTrue(user.getId() > 0, "User ID should be set after creation");
            createdUserId = user.getId();
        }

        @Test
        @Order(3)
        @DisplayName("11.3 READ — Find user by username")
        void testFindByUsername() {
            Optional<User> found = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(found.isPresent(), "User should be found by username");
            assertEquals(TEST_USERNAME, found.get().getUsername());
            assertEquals(TEST_FULLNAME, found.get().getFullName());
            assertEquals(Role.USER, found.get().getRole());
        }

        @Test
        @Order(4)
        @DisplayName("11.4 READ — Find user by email")
        void testFindByEmail() {
            Optional<User> found = UserService.getInstance().findByEmail(TEST_EMAIL);
            assertTrue(found.isPresent(), "User should be found by email");
            assertEquals(TEST_EMAIL, found.get().getEmail());
        }

        @Test
        @Order(5)
        @DisplayName("11.5 READ — Find user by ID")
        void testFindById() {
            assertTrue(createdUserId > 0, "User should have been created first");
            Optional<User> found = UserService.getInstance().findById(createdUserId);
            assertTrue(found.isPresent(), "User should be found by ID");
            assertEquals(createdUserId, found.get().getId());
        }

        @Test
        @Order(6)
        @DisplayName("11.6 READ — Find all users returns non-empty list")
        void testFindAll() {
            List<User> users = UserService.getInstance().findAll();
            assertNotNull(users);
            assertFalse(users.isEmpty(), "There should be at least one user in the database");
        }

        @Test
        @Order(7)
        @DisplayName("11.7 READ — Password was stored as BCrypt hash (not plaintext)")
        void testPasswordStoredAsHash() {
            Optional<User> found = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(found.isPresent());
            String storedPwd = found.get().getPassword();
            assertTrue(storedPwd.startsWith("$2a$") || storedPwd.startsWith("$2b$"),
                    "Stored password should be a BCrypt hash");
            assertTrue(UserService.verifyPassword(TEST_PASSWORD, storedPwd),
                    "Original password should verify against stored hash");
        }

        @Test
        @Order(8)
        @DisplayName("11.8 UPDATE — Modify user full name and role")
        void testUpdateUser() {
            Optional<User> found = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(found.isPresent());

            User user = found.get();
            user.setFullName("Updated JUnit User");
            user.setRole(Role.EMPLOYER);
            assertDoesNotThrow(() -> UserService.getInstance().update(user));

            Optional<User> updated = UserService.getInstance().findById(user.getId());
            assertTrue(updated.isPresent());
            assertEquals("Updated JUnit User", updated.get().getFullName());
            assertEquals(Role.EMPLOYER, updated.get().getRole());
        }

        @Test
        @Order(9)
        @DisplayName("11.9 UPDATE — updatePassword changes the password")
        void testUpdatePassword() {
            assertTrue(createdUserId > 0);
            String newPassword = "Updated@5678";
            boolean result = UserService.getInstance().updatePassword(createdUserId, newPassword);
            assertTrue(result, "updatePassword should return true on success");

            Optional<User> found = UserService.getInstance().findById(createdUserId);
            assertTrue(found.isPresent());
            assertTrue(UserService.verifyPassword(newPassword, found.get().getPassword()));
            assertFalse(UserService.verifyPassword(TEST_PASSWORD, found.get().getPassword()),
                    "Old password should no longer work");
        }

        @Test
        @Order(10)
        @DisplayName("11.10 READ — Find non-existent user returns empty")
        void testFindNonExistentUser() {
            Optional<User> found = UserService.getInstance().findByUsername("nonexistent_user_xyz_999");
            assertTrue(found.isEmpty(), "Non-existent user should return empty Optional");
        }

        @Test
        @Order(11)
        @DisplayName("11.11 CREATE — Duplicate username throws exception")
        void testCreateDuplicateUsername() {
            User duplicate = new User(TEST_USERNAME, "Pass@1111", Role.USER, "Duplicate User");
            duplicate.setEmail("other_" + TEST_EMAIL);
            assertThrows(RuntimeException.class, () -> UserService.getInstance().create(duplicate),
                    "Creating a user with duplicate username should throw");
        }

        @Test
        @Order(12)
        @DisplayName("11.12 saveUser delegates to create/update correctly")
        void testSaveUserDelegation() {
            Optional<User> found = UserService.getInstance().findById(createdUserId);
            assertTrue(found.isPresent());
            User user = found.get();
            user.setFullName("SaveUser Updated");
            assertDoesNotThrow(() -> UserService.getInstance().saveUser(user));

            Optional<User> refetched = UserService.getInstance().findById(createdUserId);
            assertTrue(refetched.isPresent());
            assertEquals("SaveUser Updated", refetched.get().getFullName());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 12: AUTH SERVICE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(12)
    @DisplayName("12. AuthService")
    @TestMethodOrder(OrderAnnotation.class)
    class AuthServiceTests {

        @Test
        @Order(1)
        @DisplayName("12.1 AuthService singleton returns same instance")
        void testSingleton() {
            AuthService a1 = AuthService.getInstance();
            AuthService a2 = AuthService.getInstance();
            assertSame(a1, a2);
        }

        @Test
        @Order(2)
        @DisplayName("12.2 Login with correct credentials succeeds")
        void testLoginSuccess() {
            // Use the updated password from test 11.9
            Optional<User> result = AuthService.getInstance().login(TEST_USERNAME, "Updated@5678");
            assertTrue(result.isPresent(), "Login with correct credentials should succeed");
            assertEquals(TEST_USERNAME, result.get().getUsername());
        }

        @Test
        @Order(3)
        @DisplayName("12.3 Login with wrong password fails")
        void testLoginWrongPassword() {
            Optional<User> result = AuthService.getInstance().login(TEST_USERNAME, "WrongPassword999!");
            assertTrue(result.isEmpty(), "Login with wrong password should fail");
        }

        @Test
        @Order(4)
        @DisplayName("12.4 Login with non-existent username fails")
        void testLoginNonExistentUser() {
            Optional<User> result = AuthService.getInstance().login("ghost_user_404", "Any@Pass1");
            assertTrue(result.isEmpty(), "Login with non-existent user should fail");
        }

        @Test
        @Order(5)
        @DisplayName("12.5 getUser retrieves user by username")
        void testGetUser() {
            Optional<User> result = AuthService.getInstance().getUser(TEST_USERNAME);
            assertTrue(result.isPresent());
            assertEquals(TEST_USERNAME, result.get().getUsername());
        }

        @Test
        @Order(6)
        @DisplayName("12.6 register throws for duplicate username")
        void testRegisterDuplicate() {
            User duplicate = new User(TEST_USERNAME, "Pass@1234", Role.USER, "Dup");
            assertThrows(RuntimeException.class, () -> AuthService.getInstance().register(duplicate));
        }

        @Test
        @Order(7)
        @DisplayName("12.7 getRemainingLockoutMinutes returns >= 0")
        void testLockoutMinutesNonNegative() {
            int minutes = AuthService.getInstance().getRemainingLockoutMinutes(TEST_USERNAME);
            assertTrue(minutes >= 0, "Lockout minutes should be >= 0");
        }

        @Test
        @Order(8)
        @DisplayName("12.8 isLockedOut returns false for normal user")
        void testIsNotLockedOut() {
            // After a successful login, user should not be locked out
            assertFalse(AuthService.getInstance().isLockedOut(TEST_USERNAME));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 13: PROFILE SERVICE — CRUD OPERATIONS
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(13)
    @DisplayName("13. ProfileService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class ProfileServiceCrudTests {

        @Test
        @Order(1)
        @DisplayName("13.1 ProfileService singleton returns same instance")
        void testSingleton() {
            ProfileService s1 = ProfileService.getInstance();
            ProfileService s2 = ProfileService.getInstance();
            assertSame(s1, s2);
        }

        @Test
        @Order(2)
        @DisplayName("13.2 CREATE — Create profile for test user")
        void testCreateProfile() throws SQLException {
            assertTrue(createdUserId > 0, "User must be created first");

            Profile profile = new Profile(createdUserId, "JUnit", "TestProfile");
            profile.setPhone("+21655000111");
            profile.setLocation("Tunis");
            profile.setBirthDate(LocalDate.of(2000, 3, 15));

            int id = ProfileService.getInstance().createProfile(profile);
            assertTrue(id > 0, "Profile ID should be generated");
            createdProfileId = id;
        }

        @Test
        @Order(3)
        @DisplayName("13.3 READ — Find profile by ID")
        void testFindProfileById() throws SQLException {
            assertTrue(createdProfileId > 0);
            Profile found = ProfileService.getInstance().findProfileById(createdProfileId);
            assertNotNull(found, "Profile should be found by ID");
            assertEquals("JUnit", found.getFirstName());
            assertEquals("TestProfile", found.getLastName());
        }

        @Test
        @Order(4)
        @DisplayName("13.4 READ — Find profile by user ID")
        void testFindProfileByUserId() throws SQLException {
            Profile found = ProfileService.getInstance().findProfileByUserId(createdUserId);
            assertNotNull(found, "Profile should be found by user ID");
            assertEquals(createdUserId, found.getUserId());
            assertEquals("+21655000111", found.getPhone());
        }

        @Test
        @Order(5)
        @DisplayName("13.5 UPDATE — Update profile fields")
        void testUpdateProfile() throws SQLException {
            Profile profile = ProfileService.getInstance().findProfileById(createdProfileId);
            assertNotNull(profile);

            profile.setFirstName("UpdatedFirst");
            profile.setLastName("UpdatedLast");
            profile.setLocation("Sfax");

            boolean updated = ProfileService.getInstance().updateProfile(profile);
            assertTrue(updated, "Update should succeed");

            Profile refetch = ProfileService.getInstance().findProfileById(createdProfileId);
            assertEquals("UpdatedFirst", refetch.getFirstName());
            assertEquals("UpdatedLast", refetch.getLastName());
            assertEquals("Sfax", refetch.getLocation());
        }

        @Test
        @Order(6)
        @DisplayName("13.6 READ — findAllProfiles returns non-empty list")
        void testFindAllProfiles() throws SQLException {
            List<Profile> profiles = ProfileService.getInstance().findAllProfiles();
            assertNotNull(profiles);
            assertFalse(profiles.isEmpty());
        }

        @Test
        @Order(7)
        @DisplayName("13.7 CREATE — Validation rejects null profile")
        void testCreateNullProfile() {
            assertThrows(IllegalArgumentException.class,
                    () -> ProfileService.getInstance().createProfile(null));
        }

        @Test
        @Order(8)
        @DisplayName("13.8 CREATE — Validation rejects profile with empty first name")
        void testCreateProfileEmptyFirstName() {
            Profile p = new Profile(createdUserId, "", "Last");
            assertThrows(IllegalArgumentException.class,
                    () -> ProfileService.getInstance().createProfile(p));
        }

        @Test
        @Order(9)
        @DisplayName("13.9 getProfileWithDetails returns profile + skills + experiences")
        void testGetProfileWithDetails() throws SQLException {
            Map<String, Object> details = ProfileService.getInstance().getProfileWithDetails(createdProfileId);
            assertNotNull(details);
            assertTrue(details.containsKey("profile"));
            assertTrue(details.containsKey("skills"));
            assertTrue(details.containsKey("experiences"));
            assertTrue(details.containsKey("completionPercentage"));
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 14: SKILL CRUD VIA PROFILE SERVICE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(14)
    @DisplayName("14. Skill CRUD (via ProfileService)")
    @TestMethodOrder(OrderAnnotation.class)
    class SkillCrudTests {

        @Test
        @Order(1)
        @DisplayName("14.1 CREATE — Add a skill to the profile")
        void testCreateSkill() throws SQLException {
            assertTrue(createdProfileId > 0);
            Skill skill = new Skill(createdProfileId, "JavaFX", ProficiencyLevel.ADVANCED, 3);
            int id = ProfileService.getInstance().createSkill(skill);
            assertTrue(id > 0, "Skill ID should be generated");
        }

        @Test
        @Order(2)
        @DisplayName("14.2 READ — Find skills by profile ID")
        void testFindSkillsByProfileId() throws SQLException {
            List<Skill> skills = ProfileService.getInstance().findSkillsByProfileId(createdProfileId);
            assertNotNull(skills);
            assertFalse(skills.isEmpty(), "Should have at least one skill");
            assertEquals("JavaFX", skills.get(0).getSkillName());
        }

        @Test
        @Order(3)
        @DisplayName("14.3 SAVE — saveSkills replaces existing skills")
        void testSaveSkills() throws SQLException {
            List<String> newSkills = List.of("Java", "Spring Boot", "MySQL");
            ProfileService.getInstance().saveSkills(createdProfileId, newSkills);

            List<Skill> skills = ProfileService.getInstance().findSkillsByProfileId(createdProfileId);
            assertEquals(3, skills.size(), "Should have exactly 3 skills after save");
        }

        @Test
        @Order(4)
        @DisplayName("14.4 DELETE — deleteSkillsByProfileId removes all skills")
        void testDeleteSkills() throws SQLException {
            int deleted = ProfileService.getInstance().deleteSkillsByProfileId(createdProfileId);
            assertTrue(deleted >= 0);

            List<Skill> skills = ProfileService.getInstance().findSkillsByProfileId(createdProfileId);
            assertTrue(skills.isEmpty(), "No skills should remain after deletion");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 15: EXPERIENCE CRUD VIA PROFILE SERVICE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(15)
    @DisplayName("15. Experience CRUD (via ProfileService)")
    @TestMethodOrder(OrderAnnotation.class)
    class ExperienceCrudTests {

        @Test
        @Order(1)
        @DisplayName("15.1 CREATE — Add an experience to the profile")
        void testCreateExperience() throws SQLException {
            assertTrue(createdProfileId > 0);
            Experience exp = new Experience(createdProfileId, "Skilora Inc", "Fullstack Developer",
                    LocalDate.of(2024, 1, 1));
            exp.setEndDate(LocalDate.of(2025, 6, 30));
            exp.setDescription("Developed JavaFX modules");
            exp.setCurrentJob(false);

            int id = ProfileService.getInstance().createExperience(exp);
            assertTrue(id > 0, "Experience ID should be generated");
        }

        @Test
        @Order(2)
        @DisplayName("15.2 READ — Find experiences by profile ID")
        void testFindExperiencesByProfileId() throws SQLException {
            List<Experience> exps = ProfileService.getInstance().findExperiencesByProfileId(createdProfileId);
            assertNotNull(exps);
            assertFalse(exps.isEmpty(), "Should have at least one experience");
            assertEquals("Skilora Inc", exps.get(0).getCompany());
            assertEquals("Fullstack Developer", exps.get(0).getPosition());
        }

        @Test
        @Order(3)
        @DisplayName("15.3 DELETE — deleteExperiencesByProfileId removes all experiences")
        void testDeleteExperiences() throws SQLException {
            int deleted = ProfileService.getInstance().deleteExperiencesByProfileId(createdProfileId);
            assertTrue(deleted >= 0);

            List<Experience> exps = ProfileService.getInstance().findExperiencesByProfileId(createdProfileId);
            assertTrue(exps.isEmpty(), "No experiences should remain after deletion");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 16: PREFERENCES SERVICE — CRUD
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(16)
    @DisplayName("16. PreferencesService CRUD")
    @TestMethodOrder(OrderAnnotation.class)
    class PreferencesServiceTests {

        @Test
        @Order(1)
        @DisplayName("16.1 PreferencesService singleton returns same instance")
        void testSingleton() {
            PreferencesService p1 = PreferencesService.getInstance();
            PreferencesService p2 = PreferencesService.getInstance();
            assertSame(p1, p2);
        }

        @Test
        @Order(2)
        @DisplayName("16.2 ensureTable does not throw")
        void testEnsureTable() {
            assertDoesNotThrow(() -> PreferencesService.getInstance().ensureTable());
        }

        @Test
        @Order(3)
        @DisplayName("16.3 SET — Save a preference")
        void testSetPreference() {
            assertTrue(createdUserId > 0);
            assertDoesNotThrow(() ->
                    PreferencesService.getInstance().set(createdUserId, "dark_mode", "true"));
        }

        @Test
        @Order(4)
        @DisplayName("16.4 GET — Retrieve a preference")
        void testGetPreference() {
            Optional<String> value = PreferencesService.getInstance().get(createdUserId, "dark_mode");
            assertTrue(value.isPresent(), "Preference should be found");
            assertEquals("true", value.get());
        }

        @Test
        @Order(5)
        @DisplayName("16.5 GET — Non-existent preference returns empty")
        void testGetNonExistentPreference() {
            Optional<String> value = PreferencesService.getInstance().get(createdUserId, "nonexistent_key");
            assertTrue(value.isEmpty());
        }

        @Test
        @Order(6)
        @DisplayName("16.6 SET — Update overwrites existing preference")
        void testUpdatePreference() {
            PreferencesService.getInstance().set(createdUserId, "dark_mode", "false");
            Optional<String> value = PreferencesService.getInstance().get(createdUserId, "dark_mode");
            assertTrue(value.isPresent());
            assertEquals("false", value.get());
        }

        @Test
        @Order(7)
        @DisplayName("16.7 SAVE ALL — Batch save multiple preferences")
        void testSaveAll() {
            Map<String, String> prefs = Map.of(
                    "language", "fr",
                    "animations", "true",
                    "notifications", "true"
            );
            assertDoesNotThrow(() ->
                    PreferencesService.getInstance().saveAll(createdUserId, prefs));
        }

        @Test
        @Order(8)
        @DisplayName("16.8 GET ALL — Retrieve all preferences")
        void testGetAll() {
            Map<String, String> allPrefs = PreferencesService.getInstance().getAll(createdUserId);
            assertNotNull(allPrefs);
            assertTrue(allPrefs.size() >= 3, "Should have at least 3 preferences saved");
            assertEquals("fr", allPrefs.get("language"));
        }

        @Test
        @Order(9)
        @DisplayName("16.9 getBoolean helper works correctly")
        void testGetBoolean() {
            boolean animations = PreferencesService.getInstance().getBoolean(createdUserId, "animations", false);
            assertTrue(animations);

            boolean missing = PreferencesService.getInstance().getBoolean(createdUserId, "missing_key", true);
            assertTrue(missing, "Missing key should return default value");
        }

        @Test
        @Order(10)
        @DisplayName("16.10 getString helper works correctly")
        void testGetString() {
            String lang = PreferencesService.getInstance().getString(createdUserId, "language", "en");
            assertEquals("fr", lang);

            String missing = PreferencesService.getInstance().getString(createdUserId, "missing_key", "default");
            assertEquals("default", missing);
        }

        @Test
        @Order(11)
        @DisplayName("16.11 DELETE ALL — Remove all preferences for user")
        void testDeleteAll() {
            PreferencesService.getInstance().deleteAll(createdUserId);
            Map<String, String> allPrefs = PreferencesService.getInstance().getAll(createdUserId);
            assertTrue(allPrefs.isEmpty(), "All preferences should be deleted");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 17: BIOMETRIC SERVICE
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(17)
    @DisplayName("17. BiometricService")
    @TestMethodOrder(OrderAnnotation.class)
    class BiometricServiceTests {

        @Test
        @Order(1)
        @DisplayName("17.1 BiometricService singleton returns same instance")
        void testSingleton() {
            BiometricService b1 = BiometricService.getInstance();
            BiometricService b2 = BiometricService.getInstance();
            assertSame(b1, b2);
        }

        @Test
        @Order(2)
        @DisplayName("17.2 hasBiometricData returns true for 'admin' (migrated)")
        void testAdminHasBiometricData() {
            assertTrue(BiometricService.getInstance().hasBiometricData("admin"),
                    "Admin should have biometric data in DB");
        }

        @Test
        @Order(3)
        @DisplayName("17.3 hasBiometricData returns true for 'user' (migrated)")
        void testUserHasBiometricData() {
            assertTrue(BiometricService.getInstance().hasBiometricData("user"),
                    "'user' should have biometric data in DB");
        }

        @Test
        @Order(4)
        @DisplayName("17.4 hasBiometricData returns true for 'nour' (migrated)")
        void testNourHasBiometricData() {
            assertTrue(BiometricService.getInstance().hasBiometricData("nour"),
                    "'nour' should have biometric data in DB");
        }

        @Test
        @Order(5)
        @DisplayName("17.5 hasBiometricData returns false for non-existent user")
        void testNonExistentUserNoBiometric() {
            assertFalse(BiometricService.getInstance().hasBiometricData("ghost_user_no_biometric"));
        }

        @Test
        @Order(6)
        @DisplayName("17.6 hasBiometricData returns false for test user (not registered)")
        void testTestUserNoBiometric() {
            assertFalse(BiometricService.getInstance().hasBiometricData(TEST_USERNAME),
                    "Newly created test user should not have biometric data");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 18: PROFILE COMPLETION CALCULATION
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(18)
    @DisplayName("18. Profile Completion")
    @TestMethodOrder(OrderAnnotation.class)
    class ProfileCompletionTests {

        @Test
        @Order(1)
        @DisplayName("18.1 Empty profile has low completion percentage")
        void testEmptyProfileCompletion() {
            Profile p = new Profile(1, "A", "B"); // Only first and last name
            int pct = ProfileService.getInstance().calculateProfileCompletion(p, List.of(), List.of());
            assertTrue(pct > 0 && pct < 50, "Minimal profile should have low completion: " + pct + "%");
        }

        @Test
        @Order(2)
        @DisplayName("18.2 Full profile has high completion percentage")
        void testFullProfileCompletion() {
            Profile p = new Profile(1, "Full", "Profile");
            p.setPhone("+21611223344");
            p.setLocation("Tunis");
            p.setBirthDate(LocalDate.of(1995, 1, 1));
            p.setPhotoUrl("/photo.jpg");
            p.setCvUrl("/cv.pdf");

            List<Skill> skills = List.of(
                    new Skill(1, "Java"),
                    new Skill(1, "Python"),
                    new Skill(1, "SQL")
            );
            List<Experience> exps = List.of(
                    new Experience(1, "Co", "Dev", LocalDate.of(2020, 1, 1))
            );

            int pct = ProfileService.getInstance().calculateProfileCompletion(p, skills, exps);
            assertEquals(100, pct, "Fully filled profile should be 100%");
        }

        @Test
        @Order(3)
        @DisplayName("18.3 Profile with no skills/experiences has partial completion")
        void testPartialCompletion() {
            Profile p = new Profile(1, "Partial", "User");
            p.setPhone("+21699887766");
            p.setLocation("Sousse");

            int pct = ProfileService.getInstance().calculateProfileCompletion(p, List.of(), List.of());
            assertTrue(pct > 0 && pct < 100, "Partial profile: " + pct + "%");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 19: CLEANUP — DELETE TEST DATA
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(19)
    @DisplayName("19. Cleanup — Delete Test Data")
    @TestMethodOrder(OrderAnnotation.class)
    class CleanupTests {

        @Test
        @Order(1)
        @DisplayName("19.1 DELETE — Delete test profile (cascades skills/experiences)")
        void testDeleteProfile() throws SQLException {
            if (createdProfileId > 0) {
                boolean deleted = ProfileService.getInstance().deleteProfile(createdProfileId);
                assertTrue(deleted, "Profile should be deleted");

                Profile refetch = ProfileService.getInstance().findProfileById(createdProfileId);
                assertNull(refetch, "Profile should no longer exist after deletion");
            }
        }

        @Test
        @Order(2)
        @DisplayName("19.2 DELETE — Delete test user")
        void testDeleteUser() {
            if (createdUserId > 0) {
                assertDoesNotThrow(() -> UserService.getInstance().delete(createdUserId));

                Optional<User> refetch = UserService.getInstance().findById(createdUserId);
                assertTrue(refetch.isEmpty(), "User should no longer exist after deletion");
            }
        }

        @Test
        @Order(3)
        @DisplayName("19.3 Verify cleanup — test user is fully removed")
        void testVerifyCleanup() {
            Optional<User> byUsername = UserService.getInstance().findByUsername(TEST_USERNAME);
            assertTrue(byUsername.isEmpty(), "Test user should be cleaned up");

            Optional<User> byEmail = UserService.getInstance().findByEmail(TEST_EMAIL);
            assertTrue(byEmail.isEmpty(), "Test user email should be cleaned up");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  SECTION 20: IMAGE UTILS — BASE64 ENCODING & VALIDATION
    // ═══════════════════════════════════════════════════════════════════

    @Nested
    @Order(20)
    @DisplayName("20. ImageUtils — Base64 Encoding & Validation")
    @TestMethodOrder(OrderAnnotation.class)
    class ImageUtilsTests {

        private File createTempImage(String extension, int sizeKb) throws IOException {
            File temp = File.createTempFile("test_photo_", extension);
            temp.deleteOnExit();

            // Write a minimal valid image header + padding to reach desired size
            try (FileOutputStream fos = new FileOutputStream(temp)) {
                if (extension.equals(".png")) {
                    // PNG signature + minimal IHDR
                    fos.write(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});
                } else if (extension.equals(".jpg") || extension.equals(".jpeg")) {
                    // JPEG SOI marker
                    fos.write(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0});
                } else if (extension.equals(".gif")) {
                    // GIF89a header
                    fos.write("GIF89a".getBytes());
                } else {
                    fos.write(new byte[]{0x00, 0x00, 0x00, 0x00});
                }
                // Pad to desired size
                int remaining = (sizeKb * 1024) - 8;
                if (remaining > 0) {
                    fos.write(new byte[remaining]);
                }
            }
            return temp;
        }

        @Test
        @Order(1)
        @DisplayName("20.1 validateImageFile — null file returns error key")
        void testValidateNull() {
            String error = ImageUtils.validateImageFile(null);
            assertEquals("profile.photo.error.not_found", error);
        }

        @Test
        @Order(2)
        @DisplayName("20.2 validateImageFile — nonexistent file returns error key")
        void testValidateNonexistent() {
            String error = ImageUtils.validateImageFile(new File("/nonexistent/image.png"));
            assertEquals("profile.photo.error.not_found", error);
        }

        @Test
        @Order(3)
        @DisplayName("20.3 validateImageFile — valid PNG returns null")
        void testValidatePng() throws IOException {
            File png = createTempImage(".png", 10);
            assertNull(ImageUtils.validateImageFile(png), "Valid PNG should pass validation");
        }

        @Test
        @Order(4)
        @DisplayName("20.4 validateImageFile — valid JPG returns null")
        void testValidateJpg() throws IOException {
            File jpg = createTempImage(".jpg", 10);
            assertNull(ImageUtils.validateImageFile(jpg), "Valid JPG should pass validation");
        }

        @Test
        @Order(5)
        @DisplayName("20.5 validateImageFile — valid GIF returns null")
        void testValidateGif() throws IOException {
            File gif = createTempImage(".gif", 10);
            assertNull(ImageUtils.validateImageFile(gif), "Valid GIF should pass validation");
        }

        @Test
        @Order(6)
        @DisplayName("20.6 validateImageFile — unsupported extension returns error key")
        void testValidateUnsupported() throws IOException {
            File txt = File.createTempFile("test_", ".txt");
            txt.deleteOnExit();
            String error = ImageUtils.validateImageFile(txt);
            assertEquals("profile.photo.error.invalid_format", error);
        }

        @Test
        @Order(7)
        @DisplayName("20.7 validateImageFile — file > 5MB returns error key")
        void testValidateOversized() throws IOException {
            File large = createTempImage(".png", 5 * 1024 + 1); // 5MB + 1KB
            String error = ImageUtils.validateImageFile(large);
            assertEquals("profile.photo.error.too_large", error);
        }

        @Test
        @Order(8)
        @DisplayName("20.8 validateImageFile — file exactly at 5MB limit passes")
        void testValidateExactLimit() throws IOException {
            File exact = createTempImage(".png", 5 * 1024); // exactly 5MB
            assertNull(ImageUtils.validateImageFile(exact), "File at exactly 5MB should pass");
        }

        @Test
        @Order(9)
        @DisplayName("20.9 encodeToBase64DataUri — produces valid data URI prefix")
        void testEncodeDataUri() throws IOException {
            File png = createTempImage(".png", 1);
            String dataUri = ImageUtils.encodeToBase64DataUri(png);
            assertNotNull(dataUri);
            assertTrue(dataUri.startsWith("data:image/png;base64,"), "Should start with data URI scheme");
        }

        @Test
        @Order(10)
        @DisplayName("20.10 encodeToBase64DataUri — JPG produces correct MIME type")
        void testEncodeJpgMime() throws IOException {
            File jpg = createTempImage(".jpg", 1);
            String dataUri = ImageUtils.encodeToBase64DataUri(jpg);
            assertTrue(dataUri.startsWith("data:image/jpeg;base64,"), "JPG should produce image/jpeg MIME");
        }

        @Test
        @Order(11)
        @DisplayName("20.11 encodeToBase64DataUri — oversized file throws exception")
        void testEncodeOversizedThrows() throws IOException {
            File large = createTempImage(".png", 5 * 1024 + 1);
            assertThrows(IllegalArgumentException.class, () -> ImageUtils.encodeToBase64DataUri(large));
        }

        @Test
        @Order(12)
        @DisplayName("20.12 encodeToBase64DataUri — invalid format throws exception")
        void testEncodeInvalidFormatThrows() throws IOException {
            File txt = File.createTempFile("test_", ".txt");
            txt.deleteOnExit();
            assertThrows(IllegalArgumentException.class, () -> ImageUtils.encodeToBase64DataUri(txt));
        }

        @Test
        @Order(13)
        @DisplayName("20.13 isBase64DataUri — recognises valid data URI")
        void testIsBase64True() {
            assertTrue(ImageUtils.isBase64DataUri("data:image/png;base64,iVBOR..."));
            assertTrue(ImageUtils.isBase64DataUri("data:image/jpeg;base64,/9j/4A..."));
        }

        @Test
        @Order(14)
        @DisplayName("20.14 isBase64DataUri — rejects non-data strings")
        void testIsBase64False() {
            assertFalse(ImageUtils.isBase64DataUri(null));
            assertFalse(ImageUtils.isBase64DataUri(""));
            assertFalse(ImageUtils.isBase64DataUri("http://example.com/photo.png"));
            assertFalse(ImageUtils.isBase64DataUri("file:///local/photo.png"));
            assertFalse(ImageUtils.isBase64DataUri("data:text/plain;base64,abc"));
        }

        @Test
        @Order(15)
        @DisplayName("20.15 decodeBase64ToImage — null input returns null")
        void testDecodeNull() {
            assertNull(ImageUtils.decodeBase64ToImage(null));
        }

        @Test
        @Order(16)
        @DisplayName("20.16 decodeBase64ToImage — non-data-uri returns null")
        void testDecodeNonDataUri() {
            assertNull(ImageUtils.decodeBase64ToImage("http://example.com/img.png"));
        }

        @Test
        @Order(17)
        @DisplayName("20.17 decodeBase64ToImage — malformed base64 returns null")
        void testDecodeMalformed() {
            assertNull(ImageUtils.decodeBase64ToImage("data:image/png;base64,!!!NOTVALID!!!"));
        }

        @Test
        @Order(18)
        @DisplayName("20.18 loadProfileImage — null/blank URL returns null")
        void testLoadProfileNull() {
            assertNull(ImageUtils.loadProfileImage(null, 32, 32));
            assertNull(ImageUtils.loadProfileImage("", 32, 32));
            assertNull(ImageUtils.loadProfileImage("   ", 32, 32));
        }

        @Test
        @Order(19)
        @DisplayName("20.19 formatFileSize — bytes formatting")
        void testFormatFileSizeBytes() {
            assertEquals("0 B", ImageUtils.formatFileSize(0));
            assertEquals("512 B", ImageUtils.formatFileSize(512));
            assertEquals("1023 B", ImageUtils.formatFileSize(1023));
        }

        @Test
        @Order(20)
        @DisplayName("20.20 formatFileSize — kilobytes formatting")
        void testFormatFileSizeKb() {
            String result = ImageUtils.formatFileSize(1024);
            assertTrue(result.contains("KB"), "1024 bytes should format as KB");
            assertEquals("1.0 KB", result);

            assertEquals("1.5 KB", ImageUtils.formatFileSize(1536));
        }

        @Test
        @Order(21)
        @DisplayName("20.21 formatFileSize — megabytes formatting")
        void testFormatFileSizeMb() {
            assertEquals("1.0 MB", ImageUtils.formatFileSize(1024 * 1024));
            assertEquals("5.0 MB", ImageUtils.formatFileSize(5L * 1024 * 1024));
        }

        @Test
        @Order(22)
        @DisplayName("20.22 MAX_IMAGE_SIZE_BYTES — constant equals 5MB")
        void testMaxSizeConstant() {
            assertEquals(5L * 1024 * 1024, ImageUtils.MAX_IMAGE_SIZE_BYTES);
            assertEquals(5, ImageUtils.MAX_IMAGE_SIZE_MB);
        }

        @Test
        @Order(23)
        @DisplayName("20.23 encodeToBase64DataUri — round-trip encode integrity")
        void testRoundTripIntegrity() throws IOException {
            File png = createTempImage(".png", 2);
            String dataUri = ImageUtils.encodeToBase64DataUri(png);

            // Re-decode the base64 and compare to original bytes
            int base64Start = dataUri.indexOf("base64,") + 7;
            String base64Data = dataUri.substring(base64Start);
            byte[] decoded = java.util.Base64.getDecoder().decode(base64Data);
            byte[] original = java.nio.file.Files.readAllBytes(png.toPath());

            assertArrayEquals(original, decoded, "Decoded bytes must match original file bytes");
        }

        @Test
        @Order(24)
        @DisplayName("20.24 validateImageFile — WebP extension accepted")
        void testValidateWebp() throws IOException {
            File webp = createTempImage(".webp", 10);
            assertNull(ImageUtils.validateImageFile(webp), "WebP should pass validation");
        }

        @Test
        @Order(25)
        @DisplayName("20.25 validateImageFile — BMP extension accepted")
        void testValidateBmp() throws IOException {
            File bmp = createTempImage(".bmp", 10);
            assertNull(ImageUtils.validateImageFile(bmp), "BMP should pass validation");
        }
    }
}
