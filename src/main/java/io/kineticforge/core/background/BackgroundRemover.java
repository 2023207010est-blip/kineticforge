package io.kineticforge.core.background;

import io.kineticforge.exception.ImageProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Elimina el fondo blanco de un frame preservando detalles internos.
 *
 * <p>Utiliza flood fill desde los 4 bordes del frame. El contorno negro
 * del personaje actúa como barrera natural: el flood fill no lo atraviesa,
 * por lo que las zonas blancas internas (ojos, ropas, brillos) se preservan.</p>
 *
 * <p>Además, suaviza la frontera entre el fondo y el personaje con
 * un gradiente de alpha basado en la luminancia.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class BackgroundRemover {

    private static final Logger log = LoggerFactory.getLogger(BackgroundRemover.class);

    /** Umbral de luminancia para considerar un píxel como "blanco papel". */
    private static final int WHITE_THRESHOLD = 248;

    /** Estrategia de flood fill. */
    private final FloodFillStrategy strategy;

    /** Si se debe aplicar antialiasing en la frontera. */
    private final boolean applyAntialiasing;

    public BackgroundRemover() {
        this(FloodFillStrategy.BFS, true);
    }

    public BackgroundRemover(FloodFillStrategy strategy, boolean applyAntialiasing) {
        this.strategy = Objects.requireNonNull(strategy, "strategy no puede ser nulo");
        this.applyAntialiasing = applyAntialiasing;
    }

    /**
     * Elimina el fondo blanco del frame.
     *
     * @param frame frame original (fondo blanco opaco)
     * @return frame con fondo transparente
     */
    public BufferedImage removeBackground(BufferedImage frame) {
        Objects.requireNonNull(frame, "frame no puede ser nulo");

        int width = frame.getWidth();
        int height = frame.getHeight();

        log.debug("Eliminando fondo de frame {}×{}", width, height);

        // 1. Crear imagen destino con canal alpha
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // 2. Copiar la imagen original al destino
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                result.setRGB(x, y, frame.getRGB(x, y));
            }
        }

        // 3. Detectar fondo con flood fill desde bordes
        boolean[][] isBackground = new boolean[height][width];
        floodFillFromBorders(frame, isBackground);

        // 4. Aplicar transparencia + antialiasing
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x]) {
                    // Píxel de fondo → completamente transparente
                    result.setRGB(x, y, 0x00000000);
                } else if (applyAntialiasing) {
                    // Píxel no-fondo → ajustar alpha según luminancia
                    int rgb = frame.getRGB(x, y);
                    int alpha = computeAlpha(rgb);
                    int newRgb = (alpha << 24) | (rgb & 0x00FFFFFF);
                    result.setRGB(x, y, newRgb);
                }
            }
        }

        // 5. Aplicar antialiasing en la frontera (una pasada extra)
        if (applyAntialiasing) {
            applyBorderAntialiasing(frame, result, isBackground);
        }

        log.debug("Fondo eliminado: {} píxeles de fondo detectados", countBackground(isBackground));

        return result;
    }

    // ============================================================
    // Flood fill
    // ============================================================

    /**
     * Marca como "fondo" todos los píxeles blanco-papel conectados a los bordes.
     */
    private void floodFillFromBorders(BufferedImage frame, boolean[][] isBackground) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        // Semillas: los 4 bordes
        Deque<int[]> queue = new ArrayDeque<>();

        // Borde superior e inferior
        for (int x = 0; x < width; x++) {
            tryAddSeed(frame, isBackground, queue, x, 0);
            tryAddSeed(frame, isBackground, queue, x, height - 1);
        }

        // Borde izquierdo y derecho
        for (int y = 0; y < height; y++) {
            tryAddSeed(frame, isBackground, queue, 0, y);
            tryAddSeed(frame, isBackground, queue, width - 1, y);
        }

        // Flood fill
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0];
            int y = p[1];

            // 4 vecinos (arriba, abajo, izquierda, derecha)
            tryAddSeed(frame, isBackground, queue, x + 1, y);
            tryAddSeed(frame, isBackground, queue, x - 1, y);
            tryAddSeed(frame, isBackground, queue, x, y + 1);
            tryAddSeed(frame, isBackground, queue, x, y - 1);
        }
    }

    /**
     * Intenta agregar un píxel a la cola de flood fill si cumple las condiciones.
     */
    private void tryAddSeed(BufferedImage frame, boolean[][] isBackground,
                            Deque<int[]> queue, int x, int y) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        if (x < 0 || x >= width || y < 0 || y >= height) return;
        if (isBackground[y][x]) return;

        int rgb = frame.getRGB(x, y);
        if (isWhitePaper(rgb)) {
            isBackground[y][x] = true;
            queue.add(new int[]{x, y});
        }
    }

    // ============================================================
    // Utilidades
    // ============================================================

    /**
     * Determina si un píxel es "blanco papel" (candidato a fondo).
     */
    private boolean isWhitePaper(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        return luminance >= WHITE_THRESHOLD;
    }

    /**
     * Calcula el alpha de un píxel según su luminancia.
     * <p>Blanco puro → alpha 0 (transparente).</p>
     * <p>Negro puro → alpha 255 (opaco).</p>
     */
    private int computeAlpha(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        int luminance = (int) (0.299 * r + 0.587 * g + 0.114 * b);
        return Math.max(0, Math.min(255, 255 - luminance));
    }

    /**
     * Aplica antialiasing en la frontera entre fondo y personaje.
     */
    private void applyBorderAntialiasing(BufferedImage original,
                                         BufferedImage result,
                                         boolean[][] isBackground) {
        int width = original.getWidth();
        int height = original.getHeight();

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                if (isBackground[y][x]) continue;

                // Si es un píxel frontera (tiene un vecino que es fondo)
                boolean isBorder =
                    isBackground[y - 1][x] || isBackground[y + 1][x] ||
                        isBackground[y][x - 1] || isBackground[y][x + 1];

                if (isBorder) {
                    int rgb = original.getRGB(x, y);
                    int alpha = computeAlpha(rgb);
                    int newRgb = (alpha << 24) | (rgb & 0x00FFFFFF);
                    result.setRGB(x, y, newRgb);
                }
            }
        }
    }

    private int countBackground(boolean[][] isBackground) {
        int count = 0;
        for (boolean[] row : isBackground) {
            for (boolean b : row) {
                if (b) count++;
            }
        }
        return count;
    }
}
