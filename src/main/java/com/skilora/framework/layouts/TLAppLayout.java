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
import javafx.util.Duration;

/**
 * TLAppLayout - Skilora Application Layout
 *
 * Main window chrome: top bar, sidebar, content. Styled via CSS (theme.css +
 * components.css).
 * Dark mode is default; use setDarkMode(false) or toggle "light" for light
 * mode.
 */
public class TLAppLayout extends BorderPane {

    private static final String STYLESHEET_THEME = "/com/skilora/ui/styles/theme.css";
    private static final String STYLESHEET_COMPONENTS = "/com/skilora/ui/styles/components.css";
    private static final String STYLESHEET_UTILITIES = "/com/skilora/ui/styles/utilities.css";

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

        // Window Controls Container
        HBox controls = new HBox(8);
        controls.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        // Minimize
        // Path: Horizontal line
        Button btnMin = createWindowButton("min", "M4 11h8v1H4z");
        btnMin.setOnAction(e -> stage.setIconified(true));

        // Maximize/Restore
        // Path: Hollow square (Maximize)
        final String svgMax = "M4 4H12V12H4V4ZM5 5V11H11V5H5Z";
        // Path: Two overlapping squares (Restore)
        final String svgRestore = "M4 6H10V12H4V6ZM5 7V11H9V7H5ZM6 4H12V10H11V5H6V4Z";

        final Button btnMax = createWindowButton("max", svgMax);
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
                updateIcon(btnMax, svgMax);
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
                updateIcon(btnMax, svgRestore);
            }
        };

        btnMax.setOnAction(e -> toggleMaximize.run());

        // Close
        // Path: X - Filled X shape
        final String svgClose = "M 4.7 3.3 L 3.3 4.7 L 6.6 8 L 3.3 11.3 L 4.7 12.7 L 8 9.4 L 11.3 12.7 L 12.7 11.3 L 9.4 8 L 12.7 4.7 L 11.3 3.3 L 8 6.6 L 4.7 3.3 z";

        Button btnClose = createWindowButton("close", svgClose);
        btnClose.getStyleClass().add("window-control-close");
        btnClose.setOnAction(e -> stage.close());

        controls.getChildren().addAll(btnMin, btnMax, btnClose);
        topBar.getChildren().add(controls);

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

        // Simple Resizer logic... (omitted to keep code short as in previous step, but
        // variable handling is updated)
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
        icon.setStyle("-fx-fill: -fx-foreground;");

        btn.setGraphic(icon);
        return btn;
    }

    /**
     * Apply stylesheets to the given scene. Call once when creating the scene (e.g.
     * in DemoView).
     */
    public static void applyStylesheets(javafx.scene.Scene scene) {
        String theme = TLAppLayout.class.getResource(STYLESHEET_THEME).toExternalForm();
        String components = TLAppLayout.class.getResource(STYLESHEET_COMPONENTS).toExternalForm();
        String utilities = TLAppLayout.class.getResource(STYLESHEET_UTILITIES).toExternalForm();
        scene.getStylesheets().addAll(theme, components, utilities);
    }

    public static void applyStylesheets(javafx.scene.control.DialogPane pane) {
        String theme = TLAppLayout.class.getResource(STYLESHEET_THEME).toExternalForm();
        String components = TLAppLayout.class.getResource(STYLESHEET_COMPONENTS).toExternalForm();
        String utilities = TLAppLayout.class.getResource(STYLESHEET_UTILITIES).toExternalForm();
        pane.getStylesheets().addAll(theme, components, utilities);
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
    }
}
