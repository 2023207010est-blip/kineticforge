package io.kineticforge.core.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Elimina el fondo blanco de un frame preservando detalles internos.
 *
 * <p>Pipeline de 7 etapas:</p>
 * <ol>
 *     <li>Filtro de mediana (reduce ruido puntual)</li>
 *     <li>Canny edge detection</li>
 *     <li>Dilatación + cierre morfológico</li>
 *     <li>Flood fill desde bordes</li>
 *     <li>Expansión del fondo</li>
 *     <li>Filtro de componentes (elimina ruido)</li>
 *     <li>Eliminación de líneas largas (líneas del template)</li>
 * </ol>
 *
 * @author KineticForge Team
 * @version 9.0.0
 * @since 2026
 */
public class BackgroundRemover {

    private static final Logger log = LoggerFactory.getLogger(BackgroundRemover.class);

    // Canny
    private static final int CANNY_LOW = 15;
    private static final int CANNY_HIGH = 50;

    // Morfología
    private static final int DILATE_RADIUS = 4;
    private static final int CLOSE_RADIUS = 3;

    // Umbrales
    private static final int SAFE_CONTENT_LUMINANCE = 200;
    private static final int NEAR_WHITE_LUMINANCE = 240;

    // Filtro de componentes
    private static final double MIN_COMPONENT_AREA_RATIO = 0.005;
    private static final int MAX_COMPONENTS = 3;

    // Filtro de líneas
    private static final double MAX_ASPECT_RATIO = 5.0;
    private static final double MAX_LINE_AREA_RATIO = 0.02;

    // Radio de mediana
    private static final int MEDIAN_RADIUS = 1;

    // Umbral de alpha para bordes suaves
    private static final int ANTIALIAS_ALPHA_MIN = 30;
    private static final int ANTIALIAS_ALPHA_MAX = 255;

    private final FloodFillStrategy strategy;
    private final boolean applyAntialiasing;
    private final CannyEdgeDetector canny = new CannyEdgeDetector();

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

        // 1. Luminancia
        int[][] gray = toLuminanceMatrix(frame);

        // 2. Filtro de mediana (reduce ruido puntual)
        int[][] filtered = medianFilter(gray, MEDIAN_RADIUS);

        // 3. Canny
        boolean[][] edges = canny.detect(filtered, CANNY_LOW, CANNY_HIGH);

        // 4. Dilatar + cerrar
        boolean[][] dilatedEdges = dilate(edges, DILATE_RADIUS);
        boolean[][] closedEdges = morphologicalClose(dilatedEdges, CLOSE_RADIUS);

        // 5. Marcar contenido seguro
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (filtered[y][x] < SAFE_CONTENT_LUMINANCE) {
                    closedEdges[y][x] = true;
                }
            }
        }

        // 6. Flood fill desde los bordes
        boolean[][] isBackground = floodFillFromBorders(closedEdges);

        // 7. Expandir fondo (come aura blanca)
        expandBackground(filtered, isBackground, NEAR_WHITE_LUMINANCE);

        // 8. Filtrar componentes pequeños
        int minArea = (int) (width * height * MIN_COMPONENT_AREA_RATIO);
        removeSmallComponents(isBackground, minArea);

        // 9. Eliminar líneas largas (template)
        removeLongLines(isBackground);

        // 10. Aplicar transparencia con alpha suave
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x]) {
                    result.setRGB(x, y, 0x00000000);
                } else {
                    int rgb = frame.getRGB(x, y);
                    int alpha = computeAlpha(gray[y][x]);
                    result.setRGB(x, y, (alpha << 24) | (rgb & 0x00FFFFFF));
                }
            }
        }

        return result;
    }

    // ============================================================
    // Etapa 1: Filtro de mediana
    // ============================================================

    private int[][] medianFilter(int[][] gray, int radius) {
        int height = gray.length;
        int width = gray[0].length;
        int[][] result = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                List<Integer> neighbors = new ArrayList<>();

                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            neighbors.add(gray[ny][nx]);
                        }
                    }
                }

                neighbors.sort(Integer::compareTo);
                result[y][x] = neighbors.get(neighbors.size() / 2);
            }
        }

        return result;
    }

    // ============================================================
    // Etapa 9: Eliminar líneas largas
    // ============================================================

    /**
     * Detecta componentes que son "líneas" (aspect ratio alto) y los elimina.
     * Esto limpia las líneas del template que quedan.
     */
    private void removeLongLines(boolean[][] isBackground) {
        int height = isBackground.length;
        int width = isBackground[0].length;

        boolean[][] visited = new boolean[height][width];
        int maxLineArea = (int) (width * height * MAX_LINE_AREA_RATIO);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x] || visited[y][x]) continue;

                Component comp = floodFillComponent(isBackground, visited, x, y);

                // Verificar aspect ratio y área
                if (comp.area <= maxLineArea && comp.area > 0) {
                    Rectangle bbox = computeBoundingBox(comp);

                    double aspectH = (double) bbox.width / bbox.height;
                    double aspectV = (double) bbox.height / bbox.width;
                    double aspect = Math.max(aspectH, aspectV);

                    // Si el aspect ratio es muy alto → es una línea
                    if (aspect > MAX_ASPECT_RATIO) {
                        log.debug("Línea detectada: bbox={}x{}, aspect={}, area={}",
                                bbox.width, bbox.height, String.format("%.1f", aspect), comp.area);

                        for (int[] p : comp.pixels) {
                            isBackground[p[1]][p[0]] = true;
                        }
                    }
                }
            }
        }
    }

    private Rectangle computeBoundingBox(Component comp) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;

        for (int[] p : comp.pixels) {
            minX = Math.min(minX, p[0]);
            minY = Math.min(minY, p[1]);
            maxX = Math.max(maxX, p[0]);
            maxY = Math.max(maxY, p[1]);
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // ============================================================
    // Etapa 8: Filtro de componentes pequeños
    // ============================================================

    private void removeSmallComponents(boolean[][] isBackground, int minArea) {
        int height = isBackground.length;
        int width = isBackground[0].length;

        boolean[][] visited = new boolean[height][width];
        List<Component> components = new ArrayList<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x] || visited[y][x]) continue;
                components.add(floodFillComponent(isBackground, visited, x, y));
            }
        }

        components.sort(Comparator.comparingInt((Component c) -> c.area).reversed());

        List<Component> keepers = new ArrayList<>();
        for (int i = 0; i < Math.min(components.size(), MAX_COMPONENTS); i++) {
            Component comp = components.get(i);
            if (comp.area >= minArea) {
                keepers.add(comp);
            }
        }

        log.debug("Conservando {} componentes de {}", keepers.size(), components.size());

        for (Component comp : components) {
            if (!keepers.contains(comp)) {
                for (int[] p : comp.pixels) {
                    isBackground[p[1]][p[0]] = true;
                }
            }
        }
    }

    private Component floodFillComponent(boolean[][] isBackground, boolean[][] visited,
                                          int startX, int startY) {
        int height = isBackground.length;
        int width = isBackground[0].length;

        Component comp = new Component();
        Deque<int[]> queue = new ArrayDeque<>();
        queue.add(new int[]{startX, startY});
        visited[startY][startX] = true;

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];
            comp.pixels.add(p);
            comp.area++;

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                    if (visited[ny][nx] || isBackground[ny][nx]) continue;

                    visited[ny][nx] = true;
                    queue.add(new int[]{nx, ny});
                }
            }
        }

        return comp;
    }

    private static class Component {
        final List<int[]> pixels = new ArrayList<>();
        int area = 0;
    }

    // ============================================================
    // Etapa 7: Expansión del fondo
    // ============================================================

    private void expandBackground(int[][] gray, boolean[][] isBackground, int nearWhiteThreshold) {
        int height = gray.length;
        int width = gray[0].length;

        Deque<int[]> queue = new ArrayDeque<>();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isBackground[y][x]) {
                    queue.add(new int[]{x, y});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                    if (isBackground[ny][nx]) continue;

                    if (gray[ny][nx] >= nearWhiteThreshold) {
                        isBackground[ny][nx] = true;
                        queue.add(new int[]{nx, ny});
                    }
                }
            }
        }
    }

    // ============================================================
    // Etapa 10: Alpha suave
    // ============================================================

    /**
     * Calcula el alpha de un píxel de contenido según su luminancia.
     * Píxeles oscuros → alpha alto (opacos).
     * Píxeles grises claros → alpha bajo (semi-transparentes).
     */
    private int computeAlpha(int lum) {
        if (lum <= 200) return 255;  // Oscuro → totalmente opaco
        if (lum >= 250) return 0;    // Muy claro → transparente
        // Interpolar entre 200 y 250
        int alpha = (int) (255.0 * (250 - lum) / 50.0);
        return Math.max(ANTIALIAS_ALPHA_MIN, Math.min(ANTIALIAS_ALPHA_MAX, alpha));
    }

    // ============================================================
    // Utilidades
    // ============================================================

    private int[][] toLuminanceMatrix(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();
        int[][] gray = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                gray[y][x] = luminance(frame.getRGB(x, y));
            }
        }
        return gray;
    }

    private boolean[][] dilate(boolean[][] mask, int radius) {
        int height = mask.length;
        int width = mask[0].length;
        boolean[][] result = new boolean[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (!mask[y][x]) continue;
                for (int dy = -radius; dy <= radius; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ny = y + dy, nx = x + dx;
                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            result[ny][nx] = true;
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
                boolean allSet = true;
                for (int dy = -radius; dy <= radius && allSet; dy++) {
                    for (int dx = -radius; dx <= radius; dx++) {
                        int ny = y + dy, nx = x + dx;
                        if (ny < 0 || ny >= height || nx < 0 || nx >= width || !mask[ny][nx]) {
                            allSet = false;
                            break;
                        }
                    }
                }
                if (allSet) result[y][x] = true;
            }
        }
        return result;
    }

    private boolean[][] morphologicalClose(boolean[][] mask, int radius) {
        return erode(dilate(mask, radius), radius);
    }

    private boolean[][] floodFillFromBorders(boolean[][] isContent) {
        int height = isContent.length;
        int width = isContent[0].length;
        boolean[][] isBackground = new boolean[height][width];

        Deque<int[]> queue = new ArrayDeque<>();

        for (int x = 0; x < width; x++) {
            tryAddSeed(isContent, isBackground, queue, x, 0);
            tryAddSeed(isContent, isBackground, queue, x, height - 1);
        }
        for (int y = 0; y < height; y++) {
            tryAddSeed(isContent, isBackground, queue, 0, y);
            tryAddSeed(isContent, isBackground, queue, width - 1, y);
        }

        while (!queue.isEmpty()) {
            int[] p = queue.poll();
            int x = p[0], y = p[1];

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
        if (isBackground[y][x] || isContent[y][x]) return;

        isBackground[y][x] = true;
        queue.add(new int[]{x, y});
    }

    private int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }
}
