package io.kineticforge.ui.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

/**
 * Utilidades para mostrar diálogos (alertas, confirmaciones, etc.).
 *
 * @author KineticForge Team
 * @version 1.1.0
 * @since 2026
 */
public final class Dialogs {

    private Dialogs() {
    }

    // ============================================================
    // Métodos sin owner (fallback)
    // ============================================================

    public static void info(String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message, null);
    }

    public static void warn(String title, String message) {
        show(Alert.AlertType.WARNING, title, message, null);
    }

    public static void error(String title, String message) {
        show(Alert.AlertType.ERROR, title, message, null);
    }

    public static boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
            ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane().setMinWidth(420);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ============================================================
    // Métodos con owner (recomendado - evita que se vayan al fondo)
    // ============================================================

    public static void info(Window owner, String title, String message) {
        show(Alert.AlertType.INFORMATION, title, message, owner);
    }

    public static void warn(Window owner, String title, String message) {
        show(Alert.AlertType.WARNING, title, message, owner);
    }

    public static void error(Window owner, String title, String message) {
        show(Alert.AlertType.ERROR, title, message, owner);
    }

    public static boolean confirm(Window owner, String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message,
            ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane().setMinWidth(420);

        if (owner != null) {
            alert.initOwner(owner);
        }

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    // ============================================================
    // Método común
    // ============================================================

    private static void show(Alert.AlertType type, String title, String message, Window owner) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.getDialogPane().setMinWidth(420);

        if (owner != null) {
            alert.initOwner(owner);
        }

        alert.showAndWait();
    }
}
