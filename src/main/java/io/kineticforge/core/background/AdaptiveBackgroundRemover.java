package io.kineticforge.core.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Elimina el fondo detectando el color dominante en los bordes de la imagen.
 *
 * <p>Algoritmo:</p>
 * <ol>
 *     <li>Detectar el color más frecuente en los 4 bordes</li>
 *     <li>Flood fill desde los bordes usando ese color como semilla</li>
 *     <li>Propagar solo a píxeles DENTRO de la tolerancia del color de fondo</li>
 *     <li>Alpha binario (sin aura)</li>
 * </ol>
 *
 * @author KineticForge Team
 * @version 1.1.0
 * @since 2026
 */
public class AdaptiveBackgroundRemover {

    private static final Logger log = LoggerFactory.getLogger(AdaptiveBackgroundRemover.class);

    public static final int DEFAULT_TOLERANCE = 30;

    private final int tolerance;

    public AdaptiveBackgroundRemover() {
        this(DEFAULT_TOLERANCE);
    }

    public AdaptiveBackgroundRemover(int tolerance) {
        this.tolerance = Math.max(0, Math.min(255, tolerance));
    }

    public BufferedImage removeBackground(BufferedImage frame) {
        Objects.requireNonNull(frame, "frame no puede ser nulo");

        int width = frame.getWidth();
        int height = frame.getHeight();

        log.debug("Eliminando fondo adaptativo de frame {}x{} (tolerancia={})",
                width, height, tolerance);

        int[] bgColor = detectBorderColor(frame);
        log.debug("Color de fondo detectado: RGB({}, {}, {})",
                bgColor[0], bgColor[1], bgColor[2]);

        boolean[][] isBackground = floodFillFromBorders(frame, bgColor);

        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x]) {
                    result.setRGB(x, y, 0x00000000);
                } else {
                    int rgb = frame.getRGB(x, y);
                    result.setRGB(x, y, 0xFF000000 | (rgb & 0x00FFFFFF));
                }
            }
        }

        return result;
    }

    // ============================================================
    // Detección del color de fondo
    // ============================================================

    private int[] detectBorderColor(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        Map<Integer, Integer> histogram = new HashMap<>();

        for (int x = 0; x < width; x++) {
            countColor(histogram, frame.getRGB(x, 0));
            countColor(histogram, frame.getRGB(x, height - 1));
        }
        for (int y = 0; y < height; y++) {
            countColor(histogram, frame.getRGB(0, y));
            countColor(histogram, frame.getRGB(width - 1, y));
        }

        int maxCount = 0;
        int dominantKey = 0;
        for (Map.Entry<Integer, Integer> entry : histogram.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                dominantKey = entry.getKey();
            }
        }

        int refR = (dominantKey >> 16) & 0xFF;
        int refG = (dominantKey >> 8) & 0xFF;
        int refB = dominantKey & 0xFF;

        long sumR = 0, sumG = 0, sumB = 0;
        int count = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (x != 0 && x != width - 1 && y != 0 && y != height - 1) continue;

                int rgb = frame.getRGB(x, y);
                int cr = (rgb >> 16) & 0xFF;
                int cg = (rgb >> 8) & 0xFF;
                int cb = rgb & 0xFF;

                if (isInCube(cr, cg, cb, refR, refG, refB)) {
                    sumR += cr;
                    sumG += cg;
                    sumB += cb;
                    count++;
                }
            }
        }

        if (count > 0) {
            return new int[]{(int) (sumR / count), (int) (sumG / count), (int) (sumB / count)};
        }

        return new int[]{refR, refG, refB};
    }

    private void countColor(Map<Integer, Integer> histogram, int rgb) {
        int r = ((rgb >> 16) & 0xFF) & 0xF0;
        int g = ((rgb >> 8) & 0xFF) & 0xF0;
        int b = (rgb & 0xFF) & 0xF0;
        int key = (r << 16) | (g << 8) | b;
        histogram.merge(key, 1, Integer::sum);
    }

    private boolean isInCube(int r, int g, int b, int refR, int refG, int refB) {
        return (r & 0xF0) == (refR & 0xF0)
                && (g & 0xF0) == (refG & 0xF0)
                && (b & 0xF0) == (refB & 0xF0);
    }

    // ============================================================
    // Flood fill adaptativo
    // ============================================================

    private boolean[][] floodFillFromBorders(BufferedImage frame, int[] bgColor) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        boolean[][] isBackground = new boolean[height][width];
        Deque<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            tryAddSeed(frame, bgColor, isBackground, queue, x, 0);
            tryAddSeed(frame, bgColor, isBackground, queue, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            tryAddSeed(frame, bgColor, isBackground, queue, 0, y);
            tryAddSeed(frame, bgColor, isBackground, queue, width - 1, y);
        }

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];

            tryAddSeed(frame, bgColor, isBackground, queue, x + 1, y);
            tryAddSeed(frame, bgColor, isBackground, queue, x - 1, y);
            tryAddSeed(frame, bgColor, isBackground, queue, x, y + 1);
            tryAddSeed(frame, bgColor, isBackground, queue, x, y - 1);
        }

        return isBackground;
    }

    private void tryAddSeed(BufferedImage frame, int[] bgColor, boolean[][] isBackground,
                             Deque<int[]> queue, int x, int y) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        if (x < 0 || x >= width || y < 0 || y >= height) return;
        if (isBackground[y][x]) return;

        int rgb = frame.getRGB(x, y);
        if (isSimilarToBackground(rgb, bgColor)) {
            isBackground[y][x] = true;
            queue.add(new int[]{x, y});
        }
    }

    private boolean isSimilarToBackground(int rgb, int[] bgColor) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        int dr = r - bgColor[0];
        int dg = g - bgColor[1];
        int db = b - bgColor[2];

        double distance = Math.sqrt(dr * dr + dg * dg + db * db);
        return distance <= tolerance;
    }
}
