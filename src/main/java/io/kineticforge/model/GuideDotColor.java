package io.kineticforge.model;

/**
 * Colores predefinidos para el punto guía.
 *
 * <p>Se usan colores tenues para que el punto sea visible al
 * dibujar pero fácil de detectar y borrar en el procesamiento.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum GuideDotColor {

    /** Gris muy claro (#E0E0E0). Muy discreto. */
    LIGHT_GRAY(0xE0, 0xE0, 0xE0),

    /** Gris medio (#BDBDBD). Balance entre visible y discreto. */
    MEDIUM_GRAY(0xBD, 0xBD, 0xBD),

    /** Azul tenue (#BBDEFB). Fácil de detectar, poco invasivo. */
    LIGHT_BLUE(0xBB, 0xDE, 0xFB),

    /** Rojo tenue (#FFCDD2). Máxima visibilidad. */
    LIGHT_RED(0xFF, 0xCD, 0xD2);

    private final int r;
    private final int g;
    private final int b;

    GuideDotColor(int r, int g, int b) {
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public int getRed() {
        return r;
    }

    public int getGreen() {
        return g;
    }

    public int getBlue() {
        return b;
    }

    /**
     * @return color en formato RGB de 24 bits (0xRRGGBB).
     */
    public int toRgb() {
        return (r << 16) | (g << 8) | b;
    }
}
