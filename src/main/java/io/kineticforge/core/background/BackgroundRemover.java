package io.kineticforge.core.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Elimina el fondo blanco de un frame preservando detalles internos.
 *
 * <p>Usa un algoritmo en 3 etapas:</p>
 * <ol>
 *     <li>Identificación de "contenido" (píxeles oscuros / con color)</li>
 *     <li>Cierre morfológico para unir huecos pequeños en el contorno</li>
 *     <li>Flood fill desde los 4 bordes sobre el "no contenido"</li>
 * </ol>
 *
 * <p>Solo se vuelve transparente lo que el flood fill alcanza desde los bordes.
 * Los píxeles blancos internos (rodeados por contenido) se preservan.</p>
 *
 * @author KineticForge Team
 * @version 3.0.0
 * @since 2026
 */
public class BackgroundRemover {

    private static final Logger log = LoggerFactory.getLogger(BackgroundRemover.class);

    /** Umbral de luminancia para considerar un píxel "oscuro/contenido". */
    private static final int CONTENT_LUMINANCE = 240;

    /** Radio del cierre morfológico (en píxeles). Cierra huecos de hasta 2*radio. */
    private static final int MORPHOLOGY_RADIUS = 1;

    private final FloodFillStrategy strategy;
    private final boolean applyAntialiasing;

    public BackgroundRemover() {
        this(FloodFillStrategy.BFS, true);
    }

    public BackgroundRemover(FloodFillStrategy strategy, boolean applyAntialiasing) {
        this.strategy = Objects.requireNonNull(strategy, "strategy no puede ser nulo");
        this.applyAntialiasing = applyAntialiasing;
    }

    public BufferedImage removeBackground(BufferedImage frame) {
        Objects.requireNonNull(frame, "frame no puede ser nulo");

        int width = frame.getWidth();
        int height = frame.getHeight();

        log.debug("Eliminando fondo de frame {}x{}", width, height);

        // 1. Detectar "contenido" (píxeles que NO son blanco papel)
        boolean[][] isContent = detectContent(frame);

        // 2. Cerrar huecos pequeños en el contorno
        boolean[][] closedContent = morphologicalClose(isContent, MORPHOLOGY_RADIUS);

        // 3. Flood fill desde los bordes por lo que NO es contenido
        boolean[][] isBackground = floodFillBackground(closedContent);

        // 4. Crear imagen con transparencia
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x]) {
                    // Fondo → transparente
                    result.setRGB(x, y, 0x00000000);
                } else {
                    // Contenido → preservar color original (con alpha según luminancia)
                    int rgb = frame.getRGB(x, y);
                    if (applyAntialiasing) {
                        int alpha = computeAlphaFromLuminance(rgb);
                        result.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
                    } else {
                        result.setRGB(x, y, 0xFF000000 | (rgb & 0x00FFFFFF));
                    }
                }
            }
        }

        return result;
    }

    // ============================================================
    // Etapa 1: Detección de contenido
    // ============================================================

    /**
     * Marca como "contenido" los píxeles que NO son blanco papel.
     */
    private boolean[][] detectContent(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        boolean[][] isContent = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = frame.getRGB(x, y);
                if (luminance(rgb) < CONTENT_LUMINANCE) {
                    isContent[y][x] = true;
                }
            }
        }

        return isContent;
    }

    // ============================================================
    // Etapa 2: Cierre morfológico
    // ============================================================

    /**
     * Aplica cierre morfológico (dilatación + erosión) para cerrar huecos
     * pequeños en el contorno del contenido.
     */
    private boolean[][] morphologicalClose(boolean[][] mask, int radius) {
        boolean[][] dilated = dilate(mask, radius);
        boolean[][] eroded = erode(dilated, radius);
        return eroded;
    }

    private boolean[][] dilate(boolean[][] mask, int radius) {
        int height = mask.length;
        int width = mask[0].length;
        boolean[][] result = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (mask[y][x]) {
                    // Propagar a los vecinos en el radio
                    for (int dy = -radius; dy <= radius; dy++) {
                        for (int dx = -radius; dx <= radius; dx++) {
                            int nx = x + dx;
                            int ny = y + dy;
                            if (nx >= 0 && nx < width && ny >= 0 && ny < height) {
                                result[ny][nx] = true;
                            }
                        }
                    }
                }
            }
        }

        return result;
    }

    private boolean[][] erode(boolean[][] mask, int radius) {
        int height = mask.length;
        int width = mask[0].length;
        boolean[][] result = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) continue;

                boolean allNeighborsSet = true;
                for (int dy = -radius; dy <= radius && allNeighborsSet; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int nx = x + dx;
                        int ny = y + dy;
                        if (nx < 0 || nx >= width || ny < 0 || ny >= height || !mask[ny][nx]) {
                            allNeighborsSet = false;
                            break;
                        }
                    }
                }

                if (allNeighborsSet) {
                    result[y][x] = true;
                }
            }
        }

        return result;
    }

    // ============================================================
    // Etapa 3: Flood fill desde bordes
    // ============================================================

    /**
     * Marca como "fondo" todos los píxeles que NO son contenido Y que están
     * conectados a los bordes de la imagen.
     */
    private boolean[][] floodFillBackground(boolean[][] isContent) {
        int height = isContent.length;
        int width = isContent[0].length;
        boolean[][] isBackground = new boolean[height][width];

        Deque<int[]> queue = new ArrayDeque<>();

        // Semillas: bordes superior e inferior
        for (int x = 0; x < width; x++) {
            tryAddSeed(isContent, isBackground, queue, x, 0);
            tryAddSeed(isContent, isBackground, queue, x, height - 1);
        }

        // Semillas: bordes izquierdo y derecho
        for (int y = 0; y < height; y++) {
            tryAddSeed(isContent, isBackground, queue, 0, y);
            tryAddSeed(isContent, isBackground, queue, width - 1, y);
        }

        // Flood fill
        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0];
            int y = p[1];

            // 4 vecinos
            tryAddSeed(isContent, isBackground, queue, x + 1, y);
            tryAddSeed(isContent, isBackground, queue, x - 1, y);
            tryAddSeed(isContent, isBackground, queue, x, y + 1);
            tryAddSeed(isContent, isBackground, queue, x, y - 1);
        }

        return isBackground;
    }

    private void tryAddSeed(boolean[][] isContent, boolean[][] isBackground,
                             Deque<int[]> queue, int x, int y) {
        int width = isContent[0].length;
        int height = isContent.length;

        if (x < 0 || x >= width || y < 0 || y >= height) return;
        if (isBackground[y][x]) return;
        if (isContent[y][x]) return;

        isBackground[y][x] = true;
        queue.add(new int[]{x, y});
    }

    // ============================================================
    // Utilidades
    // ============================================================

    private int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    private int computeAlphaFromLuminance(int rgb) {
        int lum = luminance(rgb);
        return Math.max(0, Math.min(255, 255 - lum));
    }
}
