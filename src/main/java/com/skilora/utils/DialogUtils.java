package com.skilora.utils;

import com.skilora.framework.components.TLDialog;
import javafx.scene.control.ButtonType;
import java.util.Optional;

public class DialogUtils {

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
        dialog.setTitle("Confirmation"); // Window title (though invisible in TRANSPARENT style)
        dialog.setDialogTitle(title);
        dialog.setDescription(message);

        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        return dialog.showAndWait();
    }

    public static void showInfo(String title, String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle("Information");
        dialog.setDialogTitle(title);
        dialog.setDescription(message);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }

    public static void showError(String title, String message) {
        TLDialog<ButtonType> dialog = new TLDialog<>();
        dialog.setTitle("Erreur");
        dialog.setDialogTitle(title);
        dialog.setDescription(message);

        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.showAndWait();
    }
}
