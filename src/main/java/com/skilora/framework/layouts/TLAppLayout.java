package com.skilora.framework.layouts;

import javafx.geometry.Rectangle2D;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.SVGPath;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.animation.Interpolator;
import javafx.scene.layout.Priority;
import javafx.util.Duration;

/**
 * TLAppLayout - Skilora Application Layout
 *
 * Main window chrome: top bar, sidebar, content. Styled via CSS (theme.css +
 * layout.css + framework.css + views.css).
 * Dark mode is default; use setDarkMode(false) or toggle "light" for light
 * mode.
 */
public class TLAppLayout extends BorderPane {

    private static final String STYLESHEET_THEME = "/com/skilora/ui/styles/theme.css";
    private static final String STYLESHEET_LAYOUT = "/com/skilora/ui/styles/layout.css";
    private static final String STYLESHEET_FRAMEWORK = "/com/skilora/ui/styles/framework.css";
    private static final String STYLESHEET_VIEWS = "/com/skilora/ui/styles/views.css";
    private static final String STYLESHEET_UTILITIES = "/com/skilora/ui/styles/utilities.css";

    // Component CSS promoted to scene level so plain FXML nodes (Button, TableView, Label, etc.) get styled
    private static final String STYLESHEET_BUTTON = "/com/skilora/framework/styles/tl-button.css";
    private static final String STYLESHEET_TABLE = "/com/skilora/framework/styles/tl-table.css";
    private static final String STYLESHEET_TYPOGRAPHY = "/com/skilora/framework/styles/tl-typography.css";
    private static final String STYLESHEET_CHECKBOX = "/com/skilora/framework/styles/tl-checkbox.css";
    private static final String STYLESHEET_BADGE = "/com/skilora/framework/styles/tl-badge.css";
    private static final String STYLESHEET_CARD = "/com/skilora/framework/styles/tl-card.css";
    private static final String STYLESHEET_SELECT = "/com/skilora/framework/styles/tl-select.css";
    private static final String STYLESHEET_SWITCH = "/com/skilora/framework/styles/tl-switch.css";

    private final HBox topBar;
    private final VBox sidebar;
    private final VBox contentArea;

    // Sidebar state
    private boolean isSidebarCollapsed = false;
    private static final double SIDEBAR_WIDTH_EXPANDED = 240;
    private static final double SIDEBAR_WIDTH_COLLAPSED = 72;

    // Window dragging vars
    private double xOffset = 0;
    private double yOffset = 0;

    // Window resize restoration vars
    private double prevX, prevY, prevWidth, prevHeight;

    public TLAppLayout() {
        getStyleClass().addAll("root", "app-layout");

        topBar = new HBox(16);
        topBar.getStyleClass().add("app-topbar");

        sidebar = new VBox(8);
        sidebar.getStyleClass().add("app-sidebar");
        sidebar.setPrefWidth(SIDEBAR_WIDTH_EXPANDED);
        sidebar.setMinWidth(SIDEBAR_WIDTH_EXPANDED);
        sidebar.setFillWidth(true);
        sidebar.setClip(null);

        contentArea = new VBox(24);
        contentArea.getStyleClass().add("app-content");

        setTop(topBar);
        setLeft(sidebar);
        setCenter(contentArea);

        // Responsive breakpoints: auto-collapse sidebar and adjust padding
        sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((wObs, oldW, newW) -> {
                    if (newW instanceof Stage stage) {
                        setupResponsiveBreakpoints(stage);
                    }
                });
                if (newScene.getWindow() instanceof Stage stage) {
                    setupResponsiveBreakpoints(stage);
                }
            }
        });
    }

    public void toggleSidebar() {
        setSidebarCollapsed(!isSidebarCollapsed);
    }

    public void setSidebarCollapsed(boolean collapsed) {
        this.isSidebarCollapsed = collapsed;

        double targetWidth = collapsed ? SIDEBAR_WIDTH_COLLAPSED : SIDEBAR_WIDTH_EXPANDED;

        // PERFORMANCE: Only animate prefWidth; set min/max directly to avoid 3x layout invalidations per frame
        sidebar.setMinWidth(targetWidth);
        sidebar.setMaxWidth(targetWidth);

        Timeline timeline = new Timeline();
        timeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(250),
                        new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH)));
        timeline.play();

        if (collapsed) {
            sidebar.getStyleClass().add("collapsed");
        } else {
            sidebar.getStyleClass().remove("collapsed");
        }
    }

    public boolean isSidebarCollapsed() {
        return isSidebarCollapsed;
    }

    /**
     * Initializes custom window controls and dragging behaviors.
     * Call this from your Application start method if using
     * StageStyle.TRANSPARENT/UNDECORATED.
     */
    public void setupWindowControls(Stage stage) {
        // Add custom window style class for border/shadow
        getStyleClass().add("custom-window");

        String osClass = detectOsStyleClass();
        if (!getStyleClass().contains(osClass)) {
            getStyleClass().add(osClass);
        }
        final boolean isMac = "os-mac".equals(osClass);

        // Window Controls Container
        HBox controls = new HBox(isMac ? 6 : 8);
        controls.getStyleClass().add("window-controls");
        controls.setAlignment(isMac ? javafx.geometry.Pos.CENTER_LEFT : javafx.geometry.Pos.CENTER_RIGHT);

        // Minimize
        // Path: Horizontal line
        Button btnMin = isMac
                ? createMacControlButton("min")
                : createWindowButton("min", "M4 11h8v1H4z");
        btnMin.setOnAction(e -> stage.setIconified(true));

        // Maximize/Restore
        // Path: Hollow square (Maximize)
        final String svgMax = "M4 4H12V12H4V4ZM5 5V11H11V5H5Z";
        // Path: Two overlapping squares (Restore)
        final String svgRestore = "M4 6H10V12H4V6ZM5 7V11H9V7H5ZM6 4H12V10H11V5H6V4Z";

        final Button btnMax = isMac ? createMacControlButton("max") : createWindowButton("max", svgMax);
        final boolean[] isMax = { false };

        Runnable toggleMaximize = () -> {
            if (isMax[0]) {
                // Restore
                stage.setX(prevX);
                stage.setY(prevY);
                stage.setWidth(prevWidth);
                stage.setHeight(prevHeight);
                getStyleClass().remove("maximized");
                isMax[0] = false;
                if (!isMac) updateIcon(btnMax, svgMax);
            } else {
                // Maximize (Visual Bounds = work area excluding taskbar)
                prevX = stage.getX();
                prevY = stage.getY();
                prevWidth = stage.getWidth();
                prevHeight = stage.getHeight();

                // Get current screen of the window
                Screen screen = Screen
                        .getScreensForRectangle(stage.getX(), stage.getY(), stage.getWidth(), stage.getHeight()).get(0);
                Rectangle2D bounds = screen.getVisualBounds();

                stage.setX(bounds.getMinX());
                stage.setY(bounds.getMinY());
                stage.setWidth(bounds.getWidth());
                stage.setHeight(bounds.getHeight());

                getStyleClass().add("maximized");
                isMax[0] = true;
                if (!isMac) updateIcon(btnMax, svgRestore);
            }
        };

        btnMax.setOnAction(e -> toggleMaximize.run());

        // Close
        // Path: X - Filled X shape
        final String svgClose = "M 4.7 3.3 L 3.3 4.7 L 6.6 8 L 3.3 11.3 L 4.7 12.7 L 8 9.4 L 11.3 12.7 L 12.7 11.3 L 9.4 8 L 12.7 4.7 L 11.3 3.3 L 8 6.6 L 4.7 3.3 z";

        Button btnClose = isMac ? createMacControlButton("close") : createWindowButton("close", svgClose);
        if (!isMac) btnClose.getStyleClass().add("window-control-close");
        btnClose.setOnAction(e -> stage.close());

        if (isMac) {
            // macOS order: close (red), minimize (yellow), zoom (green)
            controls.getChildren().addAll(btnClose, btnMin, btnMax);
            topBar.getChildren().add(0, controls);
        } else {
            controls.getChildren().addAll(btnMin, btnMax, btnClose);
            topBar.getChildren().add(controls);
        }

        // Drag Logic on TopBar
        topBar.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });
        topBar.setOnMouseDragged(event -> {
            // Only drag if not maximized
            if (!isMax[0]) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });

        // Double-click to maximize/restore
        topBar.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                toggleMaximize.run();
            }
        });

        // ═══ Edge-drag resize ═══
        final int RESIZE_MARGIN = 6;
        final double MIN_W = 800, MIN_H = 600;

        setOnMouseMoved(event -> {
            if (isMax[0]) { setCursor(javafx.scene.Cursor.DEFAULT); return; }
            double x = event.getX(), y = event.getY();
            double w = getWidth(), h = getHeight();
            boolean left = x < RESIZE_MARGIN;
            boolean right = x > w - RESIZE_MARGIN;
            boolean top = y < RESIZE_MARGIN;
            boolean bottom = y > h - RESIZE_MARGIN;
            if      (left && top)     setCursor(javafx.scene.Cursor.NW_RESIZE);
            else if (right && top)    setCursor(javafx.scene.Cursor.NE_RESIZE);
            else if (left && bottom)  setCursor(javafx.scene.Cursor.SW_RESIZE);
            else if (right && bottom) setCursor(javafx.scene.Cursor.SE_RESIZE);
            else if (left)            setCursor(javafx.scene.Cursor.W_RESIZE);
            else if (right)           setCursor(javafx.scene.Cursor.E_RESIZE);
            else if (top)             setCursor(javafx.scene.Cursor.N_RESIZE);
            else if (bottom)          setCursor(javafx.scene.Cursor.S_RESIZE);
            else                      setCursor(javafx.scene.Cursor.DEFAULT);
        });

        final double[] dragStart = new double[4]; // startScreenX, startScreenY, startW, startH

        setOnMousePressed(event -> {
            dragStart[0] = event.getScreenX();
            dragStart[1] = event.getScreenY();
            dragStart[2] = stage.getWidth();
            dragStart[3] = stage.getHeight();
        });

        setOnMouseDragged(event -> {
            javafx.scene.Cursor c = getCursor();
            if (c == javafx.scene.Cursor.DEFAULT || c == null || isMax[0]) return;
            double dx = event.getScreenX() - dragStart[0];
            double dy = event.getScreenY() - dragStart[1];

            if (c == javafx.scene.Cursor.E_RESIZE || c == javafx.scene.Cursor.NE_RESIZE || c == javafx.scene.Cursor.SE_RESIZE) {
                double newW = Math.max(MIN_W, dragStart[2] + dx);
                stage.setWidth(newW);
            }
            if (c == javafx.scene.Cursor.W_RESIZE || c == javafx.scene.Cursor.NW_RESIZE || c == javafx.scene.Cursor.SW_RESIZE) {
                double newW = Math.max(MIN_W, dragStart[2] - dx);
                if (newW > MIN_W) { stage.setWidth(newW); stage.setX(event.getScreenX()); }
            }
            if (c == javafx.scene.Cursor.S_RESIZE || c == javafx.scene.Cursor.SE_RESIZE || c == javafx.scene.Cursor.SW_RESIZE) {
                double newH = Math.max(MIN_H, dragStart[3] + dy);
                stage.setHeight(newH);
            }
            if (c == javafx.scene.Cursor.N_RESIZE || c == javafx.scene.Cursor.NE_RESIZE || c == javafx.scene.Cursor.NW_RESIZE) {
                double newH = Math.max(MIN_H, dragStart[3] - dy);
                if (newH > MIN_H) { stage.setHeight(newH); stage.setY(event.getScreenY()); }
            }
        });
    }

    private void updateIcon(Button btn, String svgContent) {
        ((SVGPath) btn.getGraphic()).setContent(svgContent);
    }

    private Button createWindowButton(String type, String svgContent) {
        Button btn = new Button();
        btn.getStyleClass().add("window-control-btn");

        SVGPath icon = new SVGPath();
        icon.setContent(svgContent);
        // Ensure icon scales nicely
        // Viewport 0 0 16 16 approximately
        icon.getStyleClass().add("window-control-icon");

        // We set style here for fill, but css should handle color.
        icon.getStyleClass().add("icon-foreground");

        btn.setGraphic(icon);
        return btn;
    }

    private Button createMacControlButton(String type) {
        Button btn = new Button();
        btn.getStyleClass().addAll("window-control-btn", "window-control-mac");
        switch (type) {
            case "close" -> btn.getStyleClass().add("window-control-mac-close");
            case "min" -> btn.getStyleClass().add("window-control-mac-min");
            default -> btn.getStyleClass().add("window-control-mac-max");
        }
        return btn;
    }

    private static String detectOsStyleClass() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac") || os.contains("darwin") || os.contains("os x")) return "os-mac";
        if (os.contains("win")) return "os-win";
        return "os-linux";
    }

    /**
     * Resolve all scene-level + promoted component stylesheets.
     */
    private static java.util.List<String> resolveAllStylesheets() {
        return java.util.List.of(
            TLAppLayout.class.getResource(STYLESHEET_THEME).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_LAYOUT).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_FRAMEWORK).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_VIEWS).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_UTILITIES).toExternalForm(),
            // Component CSS promoted to scene level
            TLAppLayout.class.getResource(STYLESHEET_BUTTON).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_TABLE).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_TYPOGRAPHY).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_CHECKBOX).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_BADGE).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_CARD).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_SELECT).toExternalForm(),
            TLAppLayout.class.getResource(STYLESHEET_SWITCH).toExternalForm()
        );
    }

    public static void applyStylesheets(javafx.scene.Scene scene) {
        scene.getStylesheets().addAll(resolveAllStylesheets());
    }

    public static void applyStylesheets(javafx.scene.control.DialogPane pane) {
        pane.getStylesheets().addAll(resolveAllStylesheets());
    }

    /**
     * Dark mode = true (default), light mode = false.
     * Implemented by toggling 'light' class (absence of class = dark).
     */
    public void setDarkMode(boolean dark) {
        if (dark) {
            getStyleClass().remove("light");
        } else {
            if (!getStyleClass().contains("light"))
                getStyleClass().add("light");
        }
    }

    public boolean isDarkMode() {
        return !getStyleClass().contains("light");
    }

    public void toggleTheme() {
        setDarkMode(!isDarkMode());
    }

    public HBox getTopBar() {
        return topBar;
    }

    public VBox getSidebar() {
        return sidebar;
    }

    public VBox getContentArea() {
        return contentArea;
    }

    public void addToSidebar(javafx.scene.Node node) {
        sidebar.getChildren().add(node);
    }

    public void setContent(javafx.scene.Node... nodes) {
        contentArea.getChildren().clear();
        contentArea.getChildren().addAll(nodes);
        for (javafx.scene.Node n : nodes) {
            VBox.setVgrow(n, Priority.ALWAYS);
        }
    }

    private void setupResponsiveBreakpoints(Stage stage) {
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            double w = newVal.doubleValue();
            // Auto-collapse sidebar at narrow widths
            if (w < 1000 && !isSidebarCollapsed) {
                setSidebarCollapsed(true);
            } else if (w >= 1000 && isSidebarCollapsed) {
                setSidebarCollapsed(false);
            }
            // Reduce content padding at very narrow widths
            if (w < 900) {
                contentArea.setStyle("-fx-padding: 12;");
            } else {
                contentArea.setStyle("-fx-padding: 24;");
            }
        });
    }
}
