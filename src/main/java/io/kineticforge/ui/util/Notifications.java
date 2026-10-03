package io.kineticforge.ui.util;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
/**
 * Sistema de notificaciones toast no bloqueantes.
 *
 * <p>Se engancha a un StackPane raíz y muestra mensajes flotantes
 * en la parte inferior que se auto-ocultan.</p>
 */
public final class Notifications {

    public enum Level { INFO, SUCCESS, WARN, ERROR }

    private static StackPane host;

    private Notifications() {}

    /** Engancha el sistema a un StackPane raíz. */
    public static void attach(StackPane host) {
        Notifications.host = host;
    }

    public static void info(String msg)    { show(msg, Level.INFO); }
    public static void success(String msg) { show(msg, Level.SUCCESS); }
    public static void warn(String msg)    { show(msg, Level.WARN); }
    public static void error(String msg)   { show(msg, Level.ERROR); }

    private static void show(String msg, Level level) {
        if (host == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(msg, level));
            return;
        }

        Label toast = new Label(prefix(level) + "  " + msg);
        toast.getStyleClass().addAll("toast", "toast-" + level.name().toLowerCase());
        toast.setWrapText(true);
        toast.setMaxWidth(520);

        StackPane.setAlignment(toast, Pos.BOTTOM_CENTER);
        StackPane.setMargin(toast, new Insets(0, 0, 30, 0));

        host.getChildren().add(toast);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(200), toast);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition stay = new PauseTransition(Duration.seconds(4));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(350), toast);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);

        // Deslizar hacia abajo un poco al salir
        TranslateTransition slide = new TranslateTransition(Duration.millis(350), toast);
        slide.setByY(20);

        ParallelTransition out = new ParallelTransition(fadeOut, slide);

        SequentialTransition seq = new SequentialTransition(fadeIn, stay, out);
        seq.setOnFinished(e -> host.getChildren().remove(toast));
        seq.play();
    }

    private static String prefix(Level level) {
        return switch (level) {
            case INFO    -> "ℹ";
            case SUCCESS -> "✅";
            case WARN    -> "⚠";
            case ERROR   -> "❌";
        };
    }
}
