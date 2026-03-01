package com.skilora.framework.components;

import javafx.geometry.Bounds;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.stage.Window;

/**
 * TLDropdownMenu - shadcn/ui Dropdown Menu (theme-adaptive ContextMenu).
 * Use {@link #showWithinWindow(Node, Side, double)} so the menu stays inside the app window.
 */
public class TLDropdownMenu extends ContextMenu {

    private static final double ESTIMATED_MENU_WIDTH = 220;
    private static final double ESTIMATED_ITEM_HEIGHT = 36;
    private static final double MENU_VERTICAL_PADDING = 16;

    public TLDropdownMenu() {
        getStyleClass().add("dropdown-menu");
    }

    public MenuItem addItem(String text) {
        MenuItem item = new MenuItem(text);
        item.getStyleClass().add("dropdown-item");
        getItems().add(item);
        return item;
    }

    public MenuItem addItem(String text, javafx.event.EventHandler<javafx.event.ActionEvent> onAction) {
        MenuItem item = addItem(text);
        item.setOnAction(onAction);
        return item;
    }

    /**
     * Shows this menu relative to the anchor, constrained to the application window.
     * If there is not enough space below/right, the menu is shifted in/up so it stays inside the window.
     *
     * @param anchor node to align to (e.g. the button that opened the menu)
     * @param preferredSide preferred side of the anchor (e.g. {@link Side#RIGHT})
     * @param gap gap in pixels between anchor and menu
     */
    public void showWithinWindow(Node anchor, Side preferredSide, double gap) {
        if (anchor.getScene() == null || anchor.getScene().getWindow() == null) {
            show(anchor, preferredSide, gap, 0);
            return;
        }
        Window win = anchor.getScene().getWindow();
        Bounds anchorInScreen = anchor.localToScreen(anchor.getBoundsInLocal());
        double winX = win.getX();
        double winY = win.getY();
        double winW = win.getWidth();
        double winH = win.getHeight();
        double menuW = ESTIMATED_MENU_WIDTH;
        double menuH = getItems().size() * ESTIMATED_ITEM_HEIGHT + MENU_VERTICAL_PADDING;

        double screenX;
        double screenY;
        if (preferredSide == Side.RIGHT) {
            screenX = anchorInScreen.getMaxX() + gap;
            screenY = anchorInScreen.getMinY();
        } else if (preferredSide == Side.LEFT) {
            screenX = anchorInScreen.getMinX() - gap - menuW;
            screenY = anchorInScreen.getMinY();
        } else if (preferredSide == Side.TOP) {
            screenX = anchorInScreen.getMinX();
            screenY = anchorInScreen.getMinY() - gap - menuH;
        } else {
            screenX = anchorInScreen.getMinX();
            screenY = anchorInScreen.getMaxY() + gap;
        }

        double minX = winX;
        double maxX = winX + winW - menuW;
        double minY = winY;
        double maxY = winY + winH - menuH;
        screenX = Math.max(minX, Math.min(maxX, screenX));
        screenY = Math.max(minY, Math.min(maxY, screenY));

        // show(anchor, screenX, screenY) expects screen coordinates (menu top-left at that position)
        show(anchor, screenX, screenY);
    }
}
