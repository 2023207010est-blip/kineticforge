package io.kineticforge.model;

/**
 * Orientación de la página para la plantilla PDF.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum PageOrientation {

    /**
     * Orientación vertical: el lado más corto está arriba.
     * A4 vertical = 210 mm × 297 mm.
     */
    PORTRAIT,

    /**
     * Orientación horizontal: el lado más largo está arriba.
     * A4 horizontal = 297 mm × 210 mm.
     */
    LANDSCAPE;

    /**
     * @return {@code true} si la orientación es vertical.
     */
    public boolean isPortrait() {
        return this == PORTRAIT;
    }

    /**
     * @return {@code true} si la orientación es horizontal.
     */
    public boolean isLandscape() {
        return this == LANDSCAPE;
    }
}
