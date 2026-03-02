package com.skilora;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;
import org.xml.sax.InputSource;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 *   SKILORA - Automated FXML Validation Tests
 *   
 *   Static analysis of ALL FXML files to catch:
 *   - Missing/invalid fx:controller classes
 *   - Missing onAction handler methods
 *   - Missing properties on custom components
 *   - Unresolvable class imports
 *   - Missing fx:id field bindings
 * ╚══════════════════════════════════════════════════════════════╝
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("FXML Validation Tests")
class FxmlValidationTests {

    // ═══════════════════════════════════════════════════════════════
    //  All FXML files in the project
    // ═══════════════════════════════════════════════════════════════

    private static final String[] ALL_FXML_PATHS = {
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
        "/com/skilora/view/recruitment/JobDetailsView.fxml",
        "/com/skilora/view/recruitment/PostJobView.fxml",
        "/com/skilora/view/recruitment/MyOffersView.fxml",
        "/com/skilora/view/recruitment/ActiveOffersView.fxml",
        "/com/skilora/view/recruitment/InterviewsView.fxml",
        // Formation
        "/com/skilora/view/formation/FormationsView.fxml",
        "/com/skilora/view/formation/FormationAdminView.fxml",
        "/com/skilora/view/formation/MentorshipView.fxml",
        // Support
        "/com/skilora/view/support/SupportView.fxml",
        "/com/skilora/view/support/SupportAdminView.fxml",
        // Finance
        "/com/skilora/view/finance/FinanceView.fxml",
        "/com/skilora/view/finance/FinanceAdminView.fxml",
    };

    // Standard JavaFX properties that always exist (inherited from Node/Control/Pane)
    private static final Set<String> STANDARD_PROPERTIES = Set.of(
        "text", "style", "styleClass", "visible", "managed", "disable",
        "prefWidth", "prefHeight", "minWidth", "minHeight", "maxWidth", "maxHeight",
        "alignment", "spacing", "fillWidth", "fillHeight", "fitToWidth", "fitToHeight",
        "hgap", "vgap", "orientation", "wrapText", "promptText",
        "prefWrapLength", "hbarPolicy", "vbarPolicy", "pannable",
        "content", "graphic", "contentDisplay", "textAlignment",
        "editable", "selected", "toggleGroup", "userData",
        "id", "layoutX", "layoutY", "opacity", "rotate", "cursor",
        "focusTraversable", "pickOnBounds", "mouseTransparent",
        "percentWidth", "percentHeight", "columnIndex", "rowIndex",
        "columnSpan", "rowSpan", "halignment", "valignment",
        "hgrow", "vgrow", "margin", "padding",
        // fx:* attributes
        "fx:id", "fx:controller", "fx:value", "fx:factory",
        "xmlns", "xmlns:fx", "type",
        // Common layout constraints
        "GridPane.columnIndex", "GridPane.rowIndex", 
        "GridPane.columnSpan", "GridPane.rowSpan",
        "GridPane.halignment", "GridPane.valignment",
        "GridPane.hgrow", "GridPane.vgrow",
        "HBox.hgrow", "VBox.vgrow",
        "StackPane.alignment", "BorderPane.alignment",
        "AnchorPane.topAnchor", "AnchorPane.bottomAnchor",
        "AnchorPane.leftAnchor", "AnchorPane.rightAnchor"
    );

    // Known custom component classes and their settable properties
    private static final Map<String, Set<String>> CUSTOM_COMPONENT_PROPERTIES = new HashMap<>();

    static {
        CUSTOM_COMPONENT_PROPERTIES.put("TLButton", Set.of(
            "variant", "size", "icon", // custom
            "text", "onAction", "disable", "visible", "managed", "styleClass",
            "prefWidth", "prefHeight", "minWidth", "maxWidth", "fx:id"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLTextField", Set.of(
            "label", "promptText", "disable", "text", "prefWidth", "fx:id",
            "visible", "managed", "styleClass", "editable",
            "GridPane.columnIndex", "GridPane.rowIndex", "GridPane.columnSpan",
            "GridPane.rowSpan", "HBox.hgrow", "VBox.vgrow", "minWidth", "maxWidth"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLTextarea", Set.of(
            "promptText", "text", "prefHeight", "prefWidth", "fx:id",
            "visible", "managed", "styleClass", "editable", "wrapText",
            "prefRowCount"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLSelect", Set.of(
            "fx:id", "label", "minWidth", "prefWidth", "maxWidth", "visible", "managed",
            "styleClass", "disable", "promptText"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLAvatar", Set.of(
            "fx:id", "size", "visible", "managed", "styleClass"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLSeparator", Set.of(
            "fx:id", "visible", "managed", "styleClass", "orientation"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLSwitch", Set.of(
            "fx:id", "text", "selected", "visible", "managed", "styleClass",
            "disable"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLBadge", Set.of(
            "fx:id", "text", "variant", "visible", "managed", "styleClass"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLTabs", Set.of(
            "fx:id", "visible", "managed", "styleClass"
        ));
        CUSTOM_COMPONENT_PROPERTIES.put("TLScrollArea", Set.of(
            "fx:id", "fitToWidth", "fitToHeight", "visible", "managed",
            "styleClass", "hbarPolicy", "vbarPolicy"
        ));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: All FXML files exist on classpath
    // ═══════════════════════════════════════════════════════════════

    static Stream<String> allFxmlPaths() {
        return Arrays.stream(ALL_FXML_PATHS);
    }

    @ParameterizedTest(name = "FXML exists: {0}")
    @MethodSource("allFxmlPaths")
    @Order(1)
    void fxmlFileExistsOnClasspath(String fxmlPath) {
        InputStream stream = getClass().getResourceAsStream(fxmlPath);
        assertNotNull(stream, "FXML not found on classpath: " + fxmlPath);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: fx:controller classes exist
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Controller exists: {0}")
    @MethodSource("allFxmlPaths")
    @Order(2)
    void controllerClassExists(String fxmlPath) throws Exception {
        Document doc = parseFxml(fxmlPath);
        if (doc == null) return; // skip if parse fails (caught by other test)

        String controllerClass = findControllerClass(doc);
        if (controllerClass == null) {
            // fx:root types may not have controller in root — check all elements
            return; // Some FXML files use fx:root with controller
        }

        try {
            Class.forName(controllerClass);
        } catch (ClassNotFoundException e) {
            fail("Controller class not found for " + fxmlPath + ": " + controllerClass);
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: All onAction handlers exist as @FXML methods
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Handlers valid: {0}")
    @MethodSource("allFxmlPaths")
    @Order(3)
    void allOnActionHandlersExist(String fxmlPath) throws Exception {
        Document doc = parseFxml(fxmlPath);
        if (doc == null) return;

        String controllerClassName = findControllerClass(doc);
        if (controllerClassName == null) return;

        Class<?> controllerClass;
        try {
            controllerClass = Class.forName(controllerClassName);
        } catch (ClassNotFoundException e) {
            return; // Caught by controller test
        }

        List<String> handlers = findAllOnActionHandlers(doc.getDocumentElement());
        List<String> missing = new ArrayList<>();

        for (String handler : handlers) {
            if (!hasHandlerMethod(controllerClass, handler)) {
                missing.add(handler);
            }
        }

        assertTrue(missing.isEmpty(),
            fxmlPath + " has missing handler methods: " + missing +
            "\nController: " + controllerClassName +
            "\nAdd @FXML annotated methods for each missing handler.");
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: All fx:id bindings have matching fields in controller
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "fx:id bindings: {0}")
    @MethodSource("allFxmlPaths")
    @Order(4)
    void allFxIdBindingsHaveControllerFields(String fxmlPath) throws Exception {
        Document doc = parseFxml(fxmlPath);
        if (doc == null) return;

        String controllerClassName = findControllerClass(doc);
        if (controllerClassName == null) return;

        Class<?> controllerClass;
        try {
            controllerClass = Class.forName(controllerClassName);
        } catch (ClassNotFoundException e) {
            return;
        }

        List<String> fxIds = findAllFxIds(doc.getDocumentElement());
        List<String> missing = new ArrayList<>();

        // Collect all fields from the class hierarchy
        Set<String> allFields = getAllFieldNames(controllerClass);

        for (String fxId : fxIds) {
            if (!allFields.contains(fxId)) {
                missing.add(fxId);
            }
        }

        assertTrue(missing.isEmpty(),
            fxmlPath + " has fx:id bindings without matching controller fields: " + missing +
            "\nController: " + controllerClassName);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: All FXML imports resolve to real classes
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Imports valid: {0}")
    @MethodSource("allFxmlPaths")
    @Order(5)
    void allImportsResolve(String fxmlPath) throws Exception {
        InputStream stream = getClass().getResourceAsStream(fxmlPath);
        if (stream == null) return;

        String content = new String(stream.readAllBytes());

        // Find all <?import ...?> processing instructions
        Pattern importPattern = Pattern.compile("<\\?import\\s+([\\w.]+)\\.\\*\\s*\\?>|<\\?import\\s+([\\w.]+)\\s*\\?>");
        Matcher matcher = importPattern.matcher(content);

        List<String> unresolvable = new ArrayList<>();
        while (matcher.find()) {
            @SuppressWarnings("unused")
            String packageImport = matcher.group(1); // wildcard
            String classImport = matcher.group(2);   // specific class

            if (classImport != null) {
                try {
                    // Use loadClass to avoid triggering static initializers
                    // (JavaFX classes need toolkit to initialize)
                    ClassLoader cl = Thread.currentThread().getContextClassLoader();
                    if (cl == null) cl = getClass().getClassLoader();
                    cl.loadClass(classImport);
                } catch (ClassNotFoundException e) {
                    unresolvable.add(classImport);
                }
            }
            // For wildcard imports, check if package has at least one class
            // (harder to validate, skip for now)
        }

        assertTrue(unresolvable.isEmpty(),
            fxmlPath + " has unresolvable imports: " + unresolvable);
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: Custom component properties are valid
    // ═══════════════════════════════════════════════════════════════

    @ParameterizedTest(name = "Properties valid: {0}")
    @MethodSource("allFxmlPaths")
    @Order(6)
    void customComponentPropertiesExist(String fxmlPath) throws Exception {
        Document doc = parseFxml(fxmlPath);
        if (doc == null) return;

        List<String> errors = new ArrayList<>();
        validateProperties(doc.getDocumentElement(), errors);

        assertTrue(errors.isEmpty(),
            fxmlPath + " has invalid properties:\n" + String.join("\n", errors));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Test: Summary report of all FXML files
    // ═══════════════════════════════════════════════════════════════

    @Test
    @Order(100)
    @DisplayName("Full FXML Validation Summary")
    void fullValidationSummary() {
        StringBuilder report = new StringBuilder();
        report.append("\n╔══════════════════════════════════════════════════════════╗\n");
        report.append("  FXML VALIDATION SUMMARY REPORT\n");
        report.append("╚══════════════════════════════════════════════════════════╝\n\n");

        int passed = 0;
        int failed = 0;
        List<String> failures = new ArrayList<>();

        for (String fxmlPath : ALL_FXML_PATHS) {
            try {
                // 1. File exists
                InputStream stream = getClass().getResourceAsStream(fxmlPath);
                assertNotNull(stream, "Not found");

                // 2. Parse FXML
                Document doc = parseFxml(fxmlPath);
                assertNotNull(doc, "Parse failed");

                // 3. Controller exists
                String ctrl = findControllerClass(doc);
                if (ctrl != null) {
                    Class<?> clazz = Class.forName(ctrl);

                    // 4. Handlers exist
                    List<String> handlers = findAllOnActionHandlers(doc.getDocumentElement());
                    for (String h : handlers) {
                        assertTrue(hasHandlerMethod(clazz, h),
                            "Missing handler: " + h);
                    }
                }

                // 5. Properties valid
                List<String> propErrors = new ArrayList<>();
                validateProperties(doc.getDocumentElement(), propErrors);
                assertTrue(propErrors.isEmpty(), String.join("; ", propErrors));

                passed++;
                report.append("  ✓ ").append(fxmlPath).append("\n");

            } catch (Throwable t) {
                failed++;
                String msg = t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName();
                report.append("  ✗ ").append(fxmlPath).append(" → ").append(msg).append("\n");
                failures.add(fxmlPath + ": " + msg);
            }
        }

        report.append("\n────────────────────────────────────────────────────────\n");
        report.append("  Total: ").append(ALL_FXML_PATHS.length)
              .append(" | Passed: ").append(passed)
              .append(" | Failed: ").append(failed).append("\n");
        report.append("────────────────────────────────────────────────────────\n");

        System.out.println(report);

        assertTrue(failures.isEmpty(),
            "FXML validation failures:\n" + String.join("\n", failures));
    }

    // ═══════════════════════════════════════════════════════════════
    //  Helper methods
    // ═══════════════════════════════════════════════════════════════

    private Document parseFxml(String fxmlPath) {
        try {
            InputStream stream = getClass().getResourceAsStream(fxmlPath);
            if (stream == null) return null;

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            // Disable DTD loading to avoid network calls
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            return builder.parse(new InputSource(stream));
        } catch (Exception e) {
            return null;
        }
    }

    private String findControllerClass(Document doc) {
        Element root = doc.getDocumentElement();
        // Check fx:controller
        String ctrl = root.getAttribute("fx:controller");
        if (ctrl != null && !ctrl.isEmpty()) return ctrl;

        // Check all attributes for controller
        NamedNodeMap attrs = root.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            if (attr.getNodeName().contains("controller")) {
                return attr.getNodeValue();
            }
        }
        return null;
    }

    private List<String> findAllOnActionHandlers(Element element) {
        List<String> handlers = new ArrayList<>();
        collectHandlers(element, handlers);
        return handlers;
    }

    private void collectHandlers(Element element, List<String> handlers) {
        NamedNodeMap attrs = element.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            String name = attr.getNodeName();
            String value = attr.getNodeValue();

            // onAction="#methodName", onMouseClicked="#methodName", etc.
            if (name.startsWith("on") && value.startsWith("#")) {
                String methodName = value.substring(1); // remove #
                handlers.add(methodName);
            }
        }

        // Recurse into children
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                collectHandlers(child, handlers);
            }
        }
    }

    private boolean hasHandlerMethod(Class<?> controllerClass, String methodName) {
        // Search current class and all superclasses
        Class<?> clazz = controllerClass;
        while (clazz != null && clazz != Object.class) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    return true;
                }
            }
            clazz = clazz.getSuperclass();
        }
        return false;
    }

    private List<String> findAllFxIds(Element element) {
        List<String> fxIds = new ArrayList<>();
        collectFxIds(element, fxIds);
        return fxIds;
    }

    private void collectFxIds(Element element, List<String> fxIds) {
        String fxId = element.getAttribute("fx:id");
        if (fxId != null && !fxId.isEmpty()) {
            fxIds.add(fxId);
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                collectFxIds(child, fxIds);
            }
        }
    }

    private Set<String> getAllFieldNames(Class<?> clazz) {
        Set<String> names = new HashSet<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                names.add(f.getName());
            }
            current = current.getSuperclass();
        }
        return names;
    }

    private void validateProperties(Element element, List<String> errors) {
        String tagName = element.getTagName();
        
        // Check if this is a known custom component
        if (CUSTOM_COMPONENT_PROPERTIES.containsKey(tagName)) {
            Set<String> knownProps = CUSTOM_COMPONENT_PROPERTIES.get(tagName);
            NamedNodeMap attrs = element.getAttributes();
            for (int i = 0; i < attrs.getLength(); i++) {
                Node attr = attrs.item(i);
                String attrName = attr.getNodeName();
                
                // Skip standard attributes, xmlns, event handlers
                if (attrName.startsWith("xmlns") || attrName.startsWith("on") ||
                    attrName.startsWith("fx:")) continue;

                if (!knownProps.contains(attrName) && !STANDARD_PROPERTIES.contains(attrName)) {
                    errors.add("<" + tagName + "> has unknown property: " + attrName +
                              "=\"" + attr.getNodeValue() + "\"");
                }
            }
        }

        // Recurse
        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i) instanceof Element child) {
                validateProperties(child, errors);
            }
        }
    }
}
