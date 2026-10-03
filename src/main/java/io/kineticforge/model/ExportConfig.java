package io.kineticforge.model;

/**
 * Configuración de exportación de animaciones.
 *
 * @param fps        fotogramas por segundo (1-60)
 * @param loop       si la animación se reproduce en bucle infinito
 * @param scale      factor de escala (0.1 a 4.0)
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public record ExportConfig(
    int fps,
    boolean loop,
    double scale
) {

    public static final int MIN_FPS = 1;
    public static final int MAX_FPS = 60;
    public static final double MIN_SCALE = 0.1;
    public static final double MAX_SCALE = 4.0;

    public ExportConfig {
        if (fps < MIN_FPS || fps > MAX_FPS) {
            throw new IllegalArgumentException(
                "fps debe estar entre " + MIN_FPS + " y " + MAX_FPS);
        }
        if (scale < MIN_SCALE || scale > MAX_SCALE) {
            throw new IllegalArgumentException(
                "scale debe estar entre " + MIN_SCALE + " y " + MAX_SCALE);
        }
    }

    /**
     * Configuración por defecto: 12 fps, loop infinito, escala 1.0.
     */
    public static ExportConfig defaults() {
        return new ExportConfig(12, true, 1.0);
    }

    /**
     * @return delay entre frames en milisegundos.
     */
    public int getFrameDelayMs() {
        return (int) Math.round(1000.0 / fps);
    }

    public ExportConfig withFps(int newFps) {
        return new ExportConfig(newFps, loop, scale);
    }

    public ExportConfig withLoop(boolean newLoop) {
        return new ExportConfig(fps, newLoop, scale);
    }

    public ExportConfig withScale(double newScale) {
        return new ExportConfig(fps, loop, newScale);
    }
}
