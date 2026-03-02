package com.skilora.utils;

import com.skilora.framework.components.TLDialog;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;
import java.util.Optional;

public class DialogUtils {

    /**
     * Try to find the currently focused window to use as dialog owner.
     * This ensures theme tokens (light/dark) and CSS variables propagate.
     */
    private static Window findOwner() {
        return Window.getWindows().stream()
                .filter(Window::isFocused)
                .findFirst()
                .orElse(Window.getWindows().isEmpty() ? null : Window.getWindows().get(0));
    }

    /**
     * Show a confirmation dialog with OK and Cancel buttons.
     * 
     * @param title   The main title of the dialog (e.g. "Delete User?")
     * @param message The detailed message (e.g. "Are you sure you want to
     *                delete...")
     * @return An Optional containing the ButtonType clicked (typically OK or
     *         CANCEL)
     */
    public static Optional<ButtonType> showConfirmation(String title, String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        Window owner = findOwner();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18n.get("dialog.confirmation"));
        dialog.setDialogTitle(title);
        dialog.setDescription(message);

        dialog.addButton(ButtonType.CANCEL);
        dialog.addButton(ButtonType.OK);

        return dialog.showAndWait();
    }

    public static void showInfo(String title, String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        Window owner = findOwner();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18n.get("dialog.information"));
        dialog.setDialogTitle(title);
        dialog.setDescription(message);

        dialog.addButton(ButtonType.OK);
        dialog.showAndWait();
    }

    public static void showError(String title, String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        Window owner = findOwner();
        if (owner != null) dialog.initOwner(owner);
        dialog.setTitle(I18n.get("dialog.error"));
        dialog.setDialogTitle(title);
        dialog.setDescription(message);

        dialog.addButton(ButtonType.OK);
        dialog.showAndWait();
    }
}
