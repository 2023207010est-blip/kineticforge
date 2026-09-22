package io.kineticforge.ui.util;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;

import java.util.prefs.Preferences;

/**
 * Gestor de temas visuales de la aplicación.
 *
 * <p>Permite alternar entre modo claro y oscuro, guardando la preferencia
 * del usuario para que se restaure en el próximo inicio.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class ThemeManager {

    private static final String PREF_KEY = "kineticforge.theme";
    private static final String THEME_LIGHT = "light";
    private static final String THEME_DARK = "dark";

    private static final Preferences prefs =
        Preferences.userNodeForPackage(ThemeManager.class);

    private ThemeManager() {
    }

    /**
     * @return true si el tema actual es oscuro.
     */
    public static boolean isDarkMode() {
        return THEME_DARK.equals(prefs.get(PREF_KEY, THEME_LIGHT));
    }

    /**
     * Aplica el tema guardado al arrancar la app.
     */
    public static void applyCurrentTheme() {
        apply(isDarkMode());
    }

    /**
     * Cambia el tema actual y guarda la preferencia.
     *
     * @param dark true para oscuro, false para claro
     */
    public static void setDarkMode(boolean dark) {
        prefs.put(PREF_KEY, dark ? THEME_DARK : THEME_LIGHT);
        apply(dark);
    }

    /**
     * Alterna entre claro y oscuro.
     *
     * @return true si quedó en modo oscuro
     */
    public static boolean toggle() {
        boolean newDark = !isDarkMode();
        setDarkMode(newDark);
        return newDark;
    }

    private static void apply(boolean dark) {
        if (dark) {
            Application.setUserAgentStylesheet(
                new PrimerDark().getUserAgentStylesheet());
        } else {
            Application.setUserAgentStylesheet(
                new PrimerLight().getUserAgentStylesheet());
        }
    }
}
