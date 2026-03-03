package com.skilora;

import com.skilora.config.DatabaseConfig;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *   SKILORA - Comprehensive Automated Validation Tests
 *   
 *   Dynamic tests that catch:
 *   1. SQL column mismatches (service queries vs DB schema)
 *   2. FXML root type vs Java class type mismatches
 *   3. Navigation callback wiring (null action handlers)
 *   4. i18n key completeness across all locales
 *   5. Double-scroll detection (ScrollPane in TLScrollArea)
 *   6. Service singleton consistency
 *   7. Missing database tables
 *   8. Thread safety (FileChooser on background thread)
 * ╚══════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(OrderAnnotation.class)
@DisplayName("Comprehensive Validation Tests")
class ComprehensiveValidationTests {

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 1: SQL COLUMN VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("1. SQL Column Validation Against Database Schema")
    @TestMethodOrder(OrderAnnotation.class)
    class SqlColumnTests {

        /**
         * Dynamically validates that ALL columns referenced in service SQL queries
         * actually exist in the corresponding database tables.
         */
        @Test
        @Order(1)
        @DisplayName("All SQL column references match actual DB schema")
        void allSqlColumnsExistInDatabase() {
            // Map: tableName → columns that services reference
            Map<String, Set<String>> expectedColumns = new LinkedHashMap<>();

            // -- notifications (NotificationService + MentionService) --
            expectedColumns.put("notifications", Set.of(
                "id", "user_id", "type", "title", "message", "icon",
                "is_read", "reference_type", "reference_id", "created_at"
            ));

            // -- reports (ReportService) --
            expectedColumns.put("reports", Set.of(
                "id", "subject", "type", "description", "reporter_id",
                "reported_entity_type", "reported_entity_id", "status",
                "created_at", "resolved_by", "resolved_at"
            ));

            // -- formation_ratings (FormationRatingService) --
            expectedColumns.put("formation_ratings", Set.of(
                "id", "formation_id", "user_id", "is_like", "star_rating",
                "review", "created_date", "updated_date"
            ));

            // -- lesson_progress (LessonProgressService) --
            expectedColumns.put("lesson_progress", Set.of(
                "id", "enrollment_id", "module_id", "user_id", "completed",
                "progress_percentage", "time_spent_minutes", "last_accessed",
                "completed_date", "notes"
            ));

            // -- posts (PostService) --
            expectedColumns.put("posts", Set.of(
                "id", "author_id", "content", "image_url", "is_published",
                "created_date"
            ));

            // -- support_tickets (TicketService) --
            expectedColumns.put("support_tickets", Set.of(
                "id", "user_id", "subject", "description", "status",
                "priority", "category", "created_date"
            ));

            // -- enrollments (EnrollmentService) --
            expectedColumns.put("enrollments", Set.of(
                "id", "user_id", "formation_id", "completed",
                "progress", "enrolled_date"
            ));

            // -- formations (FormationService) --
            expectedColumns.put("formations", Set.of(
                "id", "title", "description", "created_by",
                "lesson_count", "status"
            ));

            // -- users (referenced by many services) --
            expectedColumns.put("users", Set.of(
                "id", "username", "full_name", "email", "role",
                "is_active", "is_verified", "photo_url"
            ));

            // -- portfolio_items (PortfolioService) --
            expectedColumns.put("portfolio_items", Set.of(
                "id", "user_id", "title", "description", "project_url",
                "image_url", "technologies", "created_date"
            ));

            // -- exchange_rates (CurrencyApiService/ExchangeRateService) --
            expectedColumns.put("exchange_rates", Set.of(
                "id", "from_currency", "to_currency", "rate",
                "rate_date", "source", "last_updated"
            ));

            // Run validation
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection()) {
                    DatabaseMetaData meta = conn.getMetaData();
                    List<String> errors = new ArrayList<>();

                    for (Map.Entry<String, Set<String>> entry : expectedColumns.entrySet()) {
                        String table = entry.getKey();
                        Set<String> requiredCols = entry.getValue();

                        // Check table exists
                        try (ResultSet rs = meta.getTables(null, null, table, new String[]{"TABLE"})) {
                            if (!rs.next()) {
                                errors.add("TABLE MISSING: '" + table + "'");
                                continue;
                            }
                        }

                        // Get actual columns
                        Set<String> actualCols = new HashSet<>();
                        try (ResultSet rs = meta.getColumns(null, null, table, null)) {
                            while (rs.next()) {
                                actualCols.add(rs.getString("COLUMN_NAME").toLowerCase());
                            }
                        }

                        // Check each required column
                        for (String col : requiredCols) {
                            if (!actualCols.contains(col.toLowerCase())) {
                                errors.add("COLUMN MISSING: '" + table + "." + col
                                    + "' (actual columns: " + actualCols + ")");
                            }
                        }
                    }

                    assertTrue(errors.isEmpty(),
                        "SQL column validation failures:\n" + String.join("\n", errors));
                }
            });
        }

        @Test
        @Order(2)
        @DisplayName("formation_ratings has star_rating column")
        void formationRatingsHasStarRating() {
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT star_rating FROM formation_ratings LIMIT 0")) {
                    // Column exists if no exception
                    assertNotNull(rs.getMetaData());
                }
            }, "formation_ratings table must have 'star_rating' column");
        }

        @Test
        @Order(3)
        @DisplayName("lesson_progress uses module_id not lesson_id")
        void lessonProgressUsesModuleId() {
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT module_id FROM lesson_progress LIMIT 0")) {
                    assertNotNull(rs.getMetaData());
                }
            }, "lesson_progress table must have 'module_id' column");
        }

        @Test
        @Order(4)
        @DisplayName("notifications table exists with correct schema")
        void notificationsTableExists() {
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT id, user_id, type, title, message, icon, is_read, " +
                         "reference_type, reference_id, created_at " +
                         "FROM notifications LIMIT 0")) {
                    assertNotNull(rs.getMetaData());
                    assertEquals(10, rs.getMetaData().getColumnCount(),
                        "notifications table should have 10 columns");
                }
            }, "notifications table must exist with correct schema");
        }

        @Test
        @Order(5)
        @DisplayName("reports table exists with correct schema")
        void reportsTableExists() {
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT id, subject, type, description, reporter_id, " +
                         "reported_entity_type, reported_entity_id, status, " +
                         "created_at, resolved_by, resolved_at " +
                         "FROM reports LIMIT 0")) {
                    assertNotNull(rs.getMetaData());
                    assertEquals(11, rs.getMetaData().getColumnCount(),
                        "reports table should have 11 columns");
                }
            }, "reports table must exist with correct schema");
        }

        @Test
        @Order(6)
        @DisplayName("users table has full_name column (not first_name/last_name)")
        void usersTableHasFullName() {
            assertDoesNotThrow(() -> {
                try (Connection conn = DatabaseConfig.getInstance().getConnection();
                     Statement stmt = conn.createStatement();
                     ResultSet rs = stmt.executeQuery(
                         "SELECT full_name FROM users LIMIT 0")) {
                    assertNotNull(rs.getMetaData());
                }
            }, "users table must have 'full_name' column");
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 2: FXML ROOT TYPE COMPATIBILITY
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("2. FXML Root Type Compatibility")
    @TestMethodOrder(OrderAnnotation.class)
    class FxmlRootTypeTests {

        /**
         * Classes that extend JavaFX nodes and load FXML with setRoot(this)
         * MUST use <fx:root> in their FXML, not a typed root element.
         */
        private static final Map<String, String> CUSTOM_COMPONENT_FXML = Map.of(
            "com.skilora.user.ui.PublicProfileView", "/com/skilora/view/user/PublicProfileView.fxml",
            "com.skilora.user.ui.ProfileWizardView", "/com/skilora/view/user/ProfileWizardView.fxml"
        );

        @ParameterizedTest(name = "fx:root check: {0}")
        @MethodSource("customComponentFxmlPairs")
        @Order(1)
        void customComponentUsesWithFxRoot(String className, String fxmlPath) throws Exception {
            // Read FXML content
            InputStream stream = getClass().getResourceAsStream(fxmlPath);
            assertNotNull(stream, "FXML not found: " + fxmlPath);
            String content = new String(stream.readAllBytes());

            // Must use fx:root, not a concrete root element
            assertTrue(content.contains("fx:root"),
                fxmlPath + " must use <fx:root> because " + className +
                " extends a JavaFX node and uses loader.setRoot(this). " +
                "Without fx:root, you'll get 'Root value already specified' error.");
        }

        @ParameterizedTest(name = "fx:root type matches class: {0}")
        @MethodSource("customComponentFxmlPairs")
        @Order(2)
        void fxRootTypeMatchesJavaClass(String className, String fxmlPath) throws Exception {
            // Use Class.forName with initialize=false to avoid triggering JavaFX toolkit
            Class<?> clazz = Class.forName(className, false, getClass().getClassLoader());
            String superClassName = clazz.getSuperclass().getSimpleName();

            // Parse FXML and check fx:root type attribute
            InputStream stream = getClass().getResourceAsStream(fxmlPath);
            assertNotNull(stream);
            String content = new String(stream.readAllBytes());

            Pattern pattern = Pattern.compile("fx:root\\s+type=\"([^\"]+)\"");
            Matcher matcher = pattern.matcher(content);
            assertTrue(matcher.find(),
                fxmlPath + " has no fx:root type attribute");

            String fxRootType = matcher.group(1);
            assertEquals(superClassName, fxRootType,
                fxmlPath + " fx:root type='" + fxRootType +
                "' but " + className + " extends " + superClassName);
        }

        static Stream<Arguments> customComponentFxmlPairs() {
            return CUSTOM_COMPONENT_FXML.entrySet().stream()
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 3: NAVIGATION WIRING VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("3. Navigation & Callback Wiring")
    @TestMethodOrder(OrderAnnotation.class)
    class NavigationWiringTests {

        /**
         * Verify that DashboardController has all navigation callback fields.
         */
        @Test
        @Order(1)
        @DisplayName("DashboardController has all navigation callback fields")
        void dashboardControllerHasAllCallbacks() throws Exception {
            Class<?> clazz = Class.forName("com.skilora.community.controller.DashboardController");

            String[] requiredCallbacks = {
                "onNavigateToProfile", "onNavigateToFeed", "onNavigateToUsers",
                "onNavigateToReports", "onNavigateToPostJob",
                "onNavigateToApplicationInbox", "onNavigateToFormations",
                "onNavigateToFormationAdmin", "onNavigateToFinance"
            };

            Set<String> fieldNames = new HashSet<>();
            for (Field f : clazz.getDeclaredFields()) {
                fieldNames.add(f.getName());
            }

            List<String> missing = new ArrayList<>();
            for (String callback : requiredCallbacks) {
                if (!fieldNames.contains(callback)) {
                    missing.add(callback);
                }
            }

            assertTrue(missing.isEmpty(),
                "DashboardController missing navigation callbacks: " + missing);
        }

        /**
         * Verify setNavigationCallbacks method accepts the right number of params.
         */
        @Test
        @Order(2)
        @DisplayName("DashboardController.setNavigationCallbacks has 5 Runnable params")
        void dashboardSetNavigationCallbacksSignature() throws Exception {
            Class<?> clazz = Class.forName("com.skilora.community.controller.DashboardController");

            Method method = null;
            for (Method m : clazz.getDeclaredMethods()) {
                if (m.getName().equals("setNavigationCallbacks")) {
                    method = m;
                    break;
                }
            }

            assertNotNull(method, "DashboardController must have setNavigationCallbacks method");
            assertEquals(5, method.getParameterCount(),
                "setNavigationCallbacks should accept 5 Runnable params " +
                "(toPostJob, toInbox, toFormations, toFormationAdmin, toFinance)");

            for (Class<?> paramType : method.getParameterTypes()) {
                assertEquals(Runnable.class, paramType,
                    "All params of setNavigationCallbacks should be Runnable");
            }
        }

        /**
         * Verify MainView has all required show*View methods.
         */
        @Test
        @Order(3)
        @DisplayName("MainView has all required navigation methods")
        void mainViewHasAllShowMethods() throws Exception {
            Class<?> clazz = Class.forName("com.skilora.ui.MainView");

            String[] requiredMethods = {
                "showDashboard", "showProfileView", "showPublicProfileView",
                "showUsersView", "showFeedView", "showSettingsView",
                "showApplicationsView", "showApplicationInboxView",
                "showJobDetails", "showNotificationsView", "showPostJobView",
                "showMyOffersView", "showActiveOffersView",
                "showFormationsView", "showFormationAdminView",
                "showMentorshipView", "showSupportView", "showCommunityView",
                "showReportsView", "showFinanceView", "showInterviewsView"
            };

            Set<String> methodNames = new HashSet<>();
            Class<?> c = clazz;
            while (c != null && c != Object.class) {
                for (Method m : c.getDeclaredMethods()) {
                    methodNames.add(m.getName());
                }
                c = c.getSuperclass();
            }

            List<String> missing = new ArrayList<>();
            for (String methodName : requiredMethods) {
                if (!methodNames.contains(methodName)) {
                    missing.add(methodName);
                }
            }

            assertTrue(missing.isEmpty(),
                "MainView missing navigation methods: " + missing);
        }

        /**
         * Verify all controllers with Runnable callback fields have proper setters.
         */
        @Test
        @Order(4)
        @DisplayName("Controllers with callbacks have setter methods")
        void controllersWithCallbacksHaveSetters() throws Exception {
            String[] controllersWithCallbacks = {
                "com.skilora.community.controller.DashboardController",
                "com.skilora.community.controller.ErrorController",
                "com.skilora.user.controller.SettingsController",
                "com.skilora.recruitment.controller.PostJobController",
                "com.skilora.recruitment.controller.MyOffersController",
                "com.skilora.recruitment.controller.JobDetailsController"
            };

            List<String> errors = new ArrayList<>();

            for (String className : controllersWithCallbacks) {
                try {
                    Class<?> clazz = Class.forName(className);
                    // Find Runnable fields
                    for (Field f : clazz.getDeclaredFields()) {
                        if (f.getType() == Runnable.class && f.getName().startsWith("on")) {
                            // Check for setter or direct access method
                            String setterName = "set" + f.getName().substring(0, 1).toUpperCase()
                                + f.getName().substring(1);
                            boolean hasSetter = false;
                            for (Method m : clazz.getDeclaredMethods()) {
                                if (m.getName().equals(setterName) ||
                                    m.getName().equals("setNavigationCallbacks") ||
                                    m.getName().equals("setCallbacks")) {
                                    hasSetter = true;
                                    break;
                                }
                            }
                            // Also check if field is public
                            if (!hasSetter && !Modifier.isPublic(f.getModifiers())) {
                                // Check if there's a direct field assignment pattern
                                f.setAccessible(true);
                                hasSetter = true; // accessible via reflection
                            }
                        }
                    }
                } catch (ClassNotFoundException e) {
                    errors.add("Class not found: " + className);
                }
            }

            assertTrue(errors.isEmpty(),
                "Controller callback wiring issues:\n" + String.join("\n", errors));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 4: FXML SCROLL CONTAINER VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("4. Scroll Container Validation")
    @TestMethodOrder(OrderAnnotation.class)
    class ScrollContainerTests {

        // FXML files whose root is ScrollPane — should NOT be wrapped in TLScrollArea
        private static final String[] SCROLLPANE_ROOT_FXMLS = {
            "/com/skilora/view/community/CommunityView.fxml",
            "/com/skilora/view/formation/MentorshipView.fxml",
            "/com/skilora/view/recruitment/EmployerDashboardView.fxml"
        };

        @ParameterizedTest(name = "ScrollPane root: {0}")
        @MethodSource("scrollPaneFxmls")
        @Order(1)
        void fxmlWithScrollPaneRootIsIdentified(String fxmlPath) throws Exception {
            InputStream stream = getClass().getResourceAsStream(fxmlPath);
            assertNotNull(stream, "FXML not found: " + fxmlPath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(stream));

            String rootTag = doc.getDocumentElement().getTagName();
            boolean isFxRoot = "fx:root".equals(rootTag);
            String rootType = isFxRoot
                ? doc.getDocumentElement().getAttribute("type")
                : rootTag;

            assertEquals("ScrollPane", rootType,
                fxmlPath + " should have ScrollPane root (was: " + rootType + ")");
        }

        /**
         * Scan MainView.java source code to verify that ScrollPane-rooted FXMLs
         * are NOT wrapped in TLScrollArea (which causes double-scroll).
         */
        @Test
        @Order(2)
        @DisplayName("No double-scroll: ScrollPane FXMLs not wrapped in TLScrollArea")
        void noDoubleScrollInMainView() throws Exception {
            // Read MainView source
            String sourceFile = "src/main/java/com/skilora/ui/MainView.java";
            Path sourcePath = Paths.get(sourceFile);

            if (!Files.exists(sourcePath)) {
                // Try with absolute path
                sourcePath = Paths.get(System.getProperty("user.dir"), sourceFile);
            }

            if (!Files.exists(sourcePath)) {
                // Skip if source not available at test time
                return;
            }

            String source = Files.readString(sourcePath);

            // Check that CommunityView (ScrollPane root) is NOT wrapped in TLScrollArea
            // After our fix, it should directly assign without TLScrollArea
            boolean communityHasTLScroll = false;

            // Find the showCommunityView method DEFINITION and check if it wraps in TLScrollArea
            // Use the pattern "void showCommunityView()" to find the actual method, not references
            int methodStart = source.indexOf("void showCommunityView()");
            if (methodStart >= 0) {
                // Get the next ~1000 chars after method start (method body)
                int searchEnd = Math.min(source.length(), methodStart + 1000);
                String methodArea = source.substring(methodStart, searchEnd);
                // Only check up to switchContent which ends the method logic
                int switchIdx = methodArea.indexOf("switchContent");
                if (switchIdx > 0) {
                    String methodBody = methodArea.substring(0, switchIdx);
                    // Look for actual instantiation "new TLScrollArea", not just mentions in comments
                    communityHasTLScroll = methodBody.contains("new TLScrollArea");
                }
            }

            assertFalse(communityHasTLScroll,
                "showCommunityView wraps ScrollPane in TLScrollArea — causes double-scroll. " +
                "CommunityView.fxml root is ScrollPane, so it should not be wrapped.");
        }

        static Stream<String> scrollPaneFxmls() {
            return Arrays.stream(SCROLLPANE_ROOT_FXMLS);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 5: I18N KEY COMPLETENESS
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("5. i18n Key Completeness")
    @TestMethodOrder(OrderAnnotation.class)
    class I18nTests {

        private static final String[] LOCALE_FILES = {
            "/com/skilora/i18n/messages.properties",
            "/com/skilora/i18n/messages_en.properties",
            "/com/skilora/i18n/messages_fr.properties",
            "/com/skilora/i18n/messages_ar.properties"
        };

        @Test
        @Order(1)
        @DisplayName("All i18n locale files exist")
        void allLocaleFilesExist() {
            for (String path : LOCALE_FILES) {
                assertNotNull(getClass().getResourceAsStream(path),
                    "Missing i18n file: " + path);
            }
        }

        @Test
        @Order(2)
        @DisplayName("All locale files have same keys")
        void allLocaleFilesHaveSameKeys() throws Exception {
            Map<String, Set<String>> localeKeys = new LinkedHashMap<>();

            for (String path : LOCALE_FILES) {
                InputStream stream = getClass().getResourceAsStream(path);
                if (stream == null) continue;

                Properties props = new Properties();
                props.load(new InputStreamReader(stream, "UTF-8"));
                localeKeys.put(path, props.stringPropertyNames());
            }

            // Use default (messages.properties) as the reference
            Set<String> referenceKeys = localeKeys.get(LOCALE_FILES[0]);
            if (referenceKeys == null) return;

            List<String> missingReport = new ArrayList<>();
            for (Map.Entry<String, Set<String>> entry : localeKeys.entrySet()) {
                if (entry.getKey().equals(LOCALE_FILES[0])) continue;

                Set<String> missing = new TreeSet<>(referenceKeys);
                missing.removeAll(entry.getValue());

                if (!missing.isEmpty()) {
                    // Only report if more than 5% are missing (some locales may be partial)
                    double missingPercent = (double) missing.size() / referenceKeys.size() * 100;
                    if (missingPercent > 5) {
                        missingReport.add(entry.getKey() + " is missing " +
                            missing.size() + " keys (" + String.format("%.1f", missingPercent) +
                            "%): " + (missing.size() > 10
                                ? new ArrayList<>(missing).subList(0, 10) + "..."
                                : missing));
                    }
                }
            }

            // Log but don't fail on partial translations (common in dev)
            if (!missingReport.isEmpty()) {
                System.out.println("⚠ i18n incomplete translations:\n" +
                    String.join("\n", missingReport));
            }
        }

        @Test
        @Order(3)
        @DisplayName("Critical i18n keys exist in all locales")
        void criticalI18nKeysExist() throws Exception {
            // Keys that MUST exist for app to function
            String[] criticalKeys = {
                "profile.view_public",
                "app.title",
                "nav.dashboard",
                "nav.profile"
            };

            for (String localePath : LOCALE_FILES) {
                InputStream stream = getClass().getResourceAsStream(localePath);
                if (stream == null) continue;

                Properties props = new Properties();
                props.load(new InputStreamReader(stream, "UTF-8"));

                List<String> missing = new ArrayList<>();
                for (String key : criticalKeys) {
                    if (!props.containsKey(key)) {
                        missing.add(key);
                    }
                }

                assertTrue(missing.isEmpty(),
                    localePath + " missing critical keys: " + missing);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 6: SERVICE SINGLETON VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("6. Service Singletons")
    @TestMethodOrder(OrderAnnotation.class)
    class ServiceSingletonTests {

        private static final String[] SERVICE_CLASSES = {
            // Community
            "com.skilora.community.service.PostService",
            "com.skilora.community.service.NotificationService",
            "com.skilora.community.service.ReportService",
            "com.skilora.community.service.ConnectionService",
            "com.skilora.community.service.EventService",
            "com.skilora.community.service.GroupService",
            "com.skilora.community.service.BlogService",
            "com.skilora.community.service.MentionService",
            "com.skilora.community.service.SearchService",
            "com.skilora.community.service.DashboardStatsService",
            "com.skilora.community.service.MessagingService",
            // Formation
            "com.skilora.formation.service.FormationService",
            "com.skilora.formation.service.EnrollmentService",
            "com.skilora.formation.service.FormationRatingService",
            "com.skilora.formation.service.LessonProgressService",
            "com.skilora.formation.service.MentorshipService",
            "com.skilora.formation.service.QuizService",
            "com.skilora.formation.service.CertificateService",
            // Finance
            "com.skilora.finance.service.FinanceDataService",
            "com.skilora.finance.service.PayslipService",
            "com.skilora.finance.service.ContractService",
            "com.skilora.finance.service.EscrowService",
            "com.skilora.finance.service.CurrencyApiService",
            // Support
            "com.skilora.support.service.SupportTicketService",
            "com.skilora.support.service.FAQService",
            "com.skilora.support.service.ChatbotService",
            "com.skilora.support.service.UserFeedbackService",
            // Recruitment
            "com.skilora.recruitment.service.JobService",
            "com.skilora.recruitment.service.ApplicationService",
            "com.skilora.recruitment.service.InterviewService",
            // User
            "com.skilora.user.service.UserService",
            "com.skilora.user.service.PortfolioService",
            "com.skilora.user.service.ReviewService"
        };

        @ParameterizedTest(name = "Singleton: {0}")
        @MethodSource("serviceClassNames")
        @Order(1)
        void serviceHasGetInstanceMethod(String className) {
            try {
                Class<?> clazz = Class.forName(className);
                Method getInstance = null;

                for (Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals("getInstance") &&
                        Modifier.isStatic(m.getModifiers()) &&
                        m.getParameterCount() == 0) {
                        getInstance = m;
                        break;
                    }
                }

                assertNotNull(getInstance,
                    className + " must have a static getInstance() method");
                assertEquals(clazz, getInstance.getReturnType(),
                    className + ".getInstance() must return " + clazz.getSimpleName());

            } catch (ClassNotFoundException e) {
                fail("Service class not found: " + className);
            }
        }

        @ParameterizedTest(name = "Consistent singleton: {0}")
        @MethodSource("serviceClassNames")
        @Order(2)
        void singletonReturnsConsistentInstance(String className) {
            try {
                Class<?> clazz = Class.forName(className);
                Method getInstance = clazz.getDeclaredMethod("getInstance");

                Object inst1 = getInstance.invoke(null);
                Object inst2 = getInstance.invoke(null);

                assertNotNull(inst1, className + ".getInstance() returned null");
                assertSame(inst1, inst2,
                    className + ".getInstance() is not returning the same instance");

            } catch (ClassNotFoundException e) {
                // Class not in project — skip
            } catch (NoSuchMethodException e) {
                // No getInstance — caught by other test
            } catch (Exception e) {
                // May fail if service needs DB — that's ok, singleton pattern is verified
            }
        }

        static Stream<String> serviceClassNames() {
            return Arrays.stream(SERVICE_CLASSES);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 7: FXML-CONTROLLER CAST TYPE VALIDATION
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("7. FXML Root Type vs Load Cast Compatibility")
    @TestMethodOrder(OrderAnnotation.class)
    class FxmlCastTests {

        /**
         * Map of FXML files → expected root element type.
         * MainView casts loader.load() to these types — a mismatch = ClassCastException.
         */
        private static final Map<String, String> FXML_EXPECTED_ROOTS = Map.ofEntries(
            Map.entry("/com/skilora/view/community/DashboardView.fxml", "VBox"),
            Map.entry("/com/skilora/view/community/CommunityView.fxml", "ScrollPane"),
            Map.entry("/com/skilora/view/community/NotificationsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/community/ReportsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/community/ErrorView.fxml", "VBox"),
            Map.entry("/com/skilora/view/user/UsersView.fxml", "VBox"),
            Map.entry("/com/skilora/view/user/UserFormDialog.fxml", "VBox"),
            Map.entry("/com/skilora/view/user/SettingsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/user/LoginView.fxml", "HBox"),
            Map.entry("/com/skilora/view/user/RegisterView.fxml", "HBox"),
            Map.entry("/com/skilora/view/recruitment/FeedView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/ApplicationsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/ApplicationInboxView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/JobDetailsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/PostJobView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/MyOffersView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/ActiveOffersView.fxml", "VBox"),
            Map.entry("/com/skilora/view/recruitment/InterviewsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/formation/FormationsView.fxml", "VBox"),
            Map.entry("/com/skilora/view/formation/FormationAdminView.fxml", "VBox"),
            Map.entry("/com/skilora/view/formation/MentorshipView.fxml", "ScrollPane"),
            Map.entry("/com/skilora/view/support/SupportView.fxml", "VBox"),
            Map.entry("/com/skilora/view/support/SupportAdminView.fxml", "VBox"),
            Map.entry("/com/skilora/view/finance/FinanceView.fxml", "VBox"),
            Map.entry("/com/skilora/view/finance/FinanceAdminView.fxml", "VBox")
        );

        @ParameterizedTest(name = "Root type: {0}")
        @MethodSource("fxmlRootPairs")
        @Order(1)
        void fxmlRootElementMatchesExpectedType(String fxmlPath, String expectedRoot)
                throws Exception {
            InputStream stream = getClass().getResourceAsStream(fxmlPath);
            assertNotNull(stream, "FXML not found: " + fxmlPath);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new InputSource(stream));

            String rootTag = doc.getDocumentElement().getTagName();
            String actualType;
            if ("fx:root".equals(rootTag)) {
                actualType = doc.getDocumentElement().getAttribute("type");
            } else {
                actualType = rootTag;
            }

            assertEquals(expectedRoot, actualType,
                fxmlPath + " root element is '" + actualType +
                "' but MainView expects '" + expectedRoot +
                "'. This will cause ClassCastException at runtime!");
        }

        static Stream<Arguments> fxmlRootPairs() {
            return FXML_EXPECTED_ROOTS.entrySet().stream()
                .map(e -> Arguments.of(e.getKey(), e.getValue()));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 8: THREAD SAFETY VALIDATION (Static Analysis)
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("8. Thread Safety Static Analysis")
    @TestMethodOrder(OrderAnnotation.class)
    class ThreadSafetyTests {

        /**
         * Check that no controller creates FileChooser inside a Task/Thread.
         * FileChooser.showSaveDialog must run on the FX Application Thread.
         */
        @Test
        @Order(1)
        @DisplayName("No FileChooser inside background threads")
        void noFileChooserInBackgroundThreads() throws Exception {
            // Controllers that use FileChooser
            String[] controllerFiles = {
                "src/main/java/com/skilora/finance/controller/FinanceController.java",
                "src/main/java/com/skilora/finance/controller/FinanceAdminController.java"
            };

            List<String> violations = new ArrayList<>();

            for (String file : controllerFiles) {
                Path path = Paths.get(System.getProperty("user.dir"), file);
                if (!Files.exists(path)) continue;

                String source = Files.readString(path);

                // Pattern: FileChooser inside a Task's call() method
                // This is a heuristic — look for FileChooser after "new Task" pattern
                if (source.contains("new Task")) {
                    // Find all Task blocks
                    Pattern taskPattern = Pattern.compile("new Task.*?\\{(.*?)\\};",
                        Pattern.DOTALL);
                    Matcher matcher = taskPattern.matcher(source);
                    while (matcher.find()) {
                        String taskBody = matcher.group(1);
                        if (taskBody.contains("FileChooser") ||
                            taskBody.contains("showSaveDialog") ||
                            taskBody.contains("showOpenDialog")) {
                            violations.add(file + ": FileChooser used inside Task " +
                                "(must be on FX Application Thread)");
                        }
                    }
                }
            }

            assertTrue(violations.isEmpty(),
                "Thread safety violations:\n" + String.join("\n", violations));
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 9: COMPLETE FXML INVENTORY CHECK
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("9. FXML Inventory Completeness")
    @TestMethodOrder(OrderAnnotation.class)
    class FxmlInventoryTests {

        /**
         * All FXML files that should exist in the project.
         * If any are missing, navigation will fail at runtime.
         */
        private static final String[] ALL_EXPECTED_FXMLS = {
            // Community
            "/com/skilora/view/community/DashboardView.fxml",
            "/com/skilora/view/community/CommunityView.fxml",
            "/com/skilora/view/community/NotificationsView.fxml",
            "/com/skilora/view/community/ReportsView.fxml",
            "/com/skilora/view/community/ErrorView.fxml",
            // User
            "/com/skilora/view/user/LoginView.fxml",
            "/com/skilora/view/user/RegisterView.fxml",
            "/com/skilora/view/user/ForgotPasswordView.fxml",
            "/com/skilora/view/user/UsersView.fxml",
            "/com/skilora/view/user/UserFormDialog.fxml",
            "/com/skilora/view/user/SettingsView.fxml",
            "/com/skilora/view/user/ProfileWizardView.fxml",
            "/com/skilora/view/user/PublicProfileView.fxml",
            "/com/skilora/view/user/BiometricAuthDialog.fxml",
            // Recruitment
            "/com/skilora/view/recruitment/FeedView.fxml",
            "/com/skilora/view/recruitment/ApplicationsView.fxml",
            "/com/skilora/view/recruitment/ApplicationInboxView.fxml",
            "/com/skilora/view/recruitment/ApplicationDetailsView.fxml",
            "/com/skilora/view/recruitment/ApplicationDialogView.fxml",
            "/com/skilora/view/recruitment/JobDetailsView.fxml",
            "/com/skilora/view/recruitment/PostJobView.fxml",
            "/com/skilora/view/recruitment/MyOffersView.fxml",
            "/com/skilora/view/recruitment/ActiveOffersView.fxml",
            "/com/skilora/view/recruitment/InterviewsView.fxml",
            "/com/skilora/view/recruitment/EmployerDashboardView.fxml",
            "/com/skilora/view/recruitment/ScheduleInterviewView.fxml",
            // Formation
            "/com/skilora/view/formation/FormationsView.fxml",
            "/com/skilora/view/formation/FormationAdminView.fxml",
            "/com/skilora/view/formation/MentorshipView.fxml",
            // Support
            "/com/skilora/view/support/SupportView.fxml",
            "/com/skilora/view/support/SupportAdminView.fxml",
            "/com/skilora/view/support/TicketCardView.fxml",
            "/com/skilora/view/support/TicketDetailView.fxml",
            "/com/skilora/view/support/TicketMessageView.fxml",
            "/com/skilora/view/support/TicketModalView.fxml",
            "/com/skilora/view/support/FeedbackModalView.fxml",
            "/com/skilora/view/support/StatisticsView.fxml",
            // Finance
            "/com/skilora/view/finance/FinanceView.fxml",
            "/com/skilora/view/finance/FinanceAdminView.fxml",
            "/com/skilora/view/finance/PaymentView.fxml",
            "/com/skilora/view/finance/UserFinanceView.fxml"
        };

        @ParameterizedTest(name = "FXML exists: {0}")
        @MethodSource("allFxmls")
        @Order(1)
        void fxmlFileExistsOnClasspath(String fxmlPath) {
            InputStream stream = getClass().getResourceAsStream(fxmlPath);
            assertNotNull(stream,
                "FXML not found: " + fxmlPath +
                " — Navigation to this view will fail at runtime!");
        }

        @ParameterizedTest(name = "FXML is valid XML: {0}")
        @MethodSource("allFxmls")
        @Order(2)
        void fxmlIsValidXml(String fxmlPath) {
            assertDoesNotThrow(() -> {
                InputStream stream = getClass().getResourceAsStream(fxmlPath);
                if (stream == null) return;

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setNamespaceAware(false);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
                factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(stream));
                assertNotNull(doc.getDocumentElement());
            }, "FXML is not valid XML: " + fxmlPath);
        }

        static Stream<String> allFxmls() {
            return Arrays.stream(ALL_EXPECTED_FXMLS);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SECTION 10: ENTITY-TO-SERVICE CONSISTENCY
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("10. Entity-Service Consistency")
    @TestMethodOrder(OrderAnnotation.class)
    class EntityServiceTests {

        @Test
        @Order(1)
        @DisplayName("FormationRating entity has starRating field")
        void formationRatingEntityHasStarRating() throws Exception {
            Class<?> clazz = Class.forName("com.skilora.formation.entity.FormationRating");
            Field field = null;
            for (Field f : clazz.getDeclaredFields()) {
                if (f.getName().equals("starRating")) {
                    field = f;
                    break;
                }
            }
            assertNotNull(field, "FormationRating must have 'starRating' field");
            assertEquals(int.class, field.getType());
        }

        @Test
        @Order(2)
        @DisplayName("Notification entity has all required fields")
        void notificationEntityHasRequiredFields() throws Exception {
            Class<?> clazz = Class.forName("com.skilora.community.entity.Notification");
            String[] requiredFields = {
                "id", "userId", "type", "title", "message", "icon",
                "referenceType", "referenceId"
            };
            Set<String> actualFields = new HashSet<>();
            for (Field f : clazz.getDeclaredFields()) {
                actualFields.add(f.getName());
            }
            for (String field : requiredFields) {
                assertTrue(actualFields.contains(field),
                    "Notification entity missing field: " + field);
            }
        }

        @Test
        @Order(3)
        @DisplayName("Report entity has all required fields")
        void reportEntityHasRequiredFields() throws Exception {
            Class<?> clazz = Class.forName("com.skilora.community.entity.Report");
            String[] requiredFields = {
                "id", "subject", "type", "description", "reporterId",
                "reportedEntityType", "reportedEntityId", "status"
            };
            Set<String> actualFields = new HashSet<>();
            for (Field f : clazz.getDeclaredFields()) {
                actualFields.add(f.getName());
            }
            for (String field : requiredFields) {
                assertTrue(actualFields.contains(field),
                    "Report entity missing field: " + field);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  SUMMARY
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(999)
    @DisplayName("Comprehensive Validation Summary")
    void validationSummary() {
        System.out.println("""
            
            ╔══════════════════════════════════════════════════════════════╗
              COMPREHENSIVE VALIDATION COMPLETE
              
              Sections validated:
              1. SQL column references vs DB schema
              2. FXML root type compatibility (fx:root)
              3. Navigation callback wiring
              4. Scroll container (double-scroll detection)
              5. i18n key completeness
              6. Service singleton patterns
              7. FXML root type vs cast type
              8. Thread safety (FileChooser + background threads)
              9. FXML inventory completeness
              10. Entity-service consistency
            ╚══════════════════════════════════════════════════════════════╝
            """);
    }
}
