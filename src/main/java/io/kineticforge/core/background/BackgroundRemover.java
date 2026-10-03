package io.kineticforge.core.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Elimina el fondo de papel de un frame preservando los detalles internos
 * (por ejemplo, el blanco de un ojo dibujado).
 *
 * <p>Pipeline:</p>
 * <ol>
 *     <li>Estimación del color del papel y balance de blancos</li>
 *     <li>Filtro de mediana 3×3</li>
 *     <li>Canny + dilatación + cierre morfológico (sella huecos del contorno)</li>
 *     <li>Flood fill desde los bordes</li>
 *     <li>Expansión del fondo hacia el papel residual</li>
 *     <li>Filtro de componentes pequeños y de líneas largas (restos de la plantilla)</li>
 *     <li><b>Matte de bordes con descontaminación de color</b> (sin aura blanca)</li>
 * </ol>
 *
 * <p>Es seguro usar una misma instancia desde varios hilos.</p>
 *
 * @author KineticForge Team
 * @version 10.0.0
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

    // Umbrales (sobre luminancia con el papel normalizado a 255)
    private static final int SAFE_CONTENT_LUMINANCE = 200;
    private static final int NEAR_WHITE_LUMINANCE = 240;

    // Filtro de componentes
    private static final double MIN_COMPONENT_AREA_RATIO = 0.005;
    private static final int MAX_COMPONENTS = 3;

    // Filtro de líneas
    private static final double MAX_ASPECT_RATIO = 5.0;
    private static final double MAX_LINE_AREA_RATIO = 0.02;

    /** Ancho (px) de la franja de borde donde se calcula el alpha suave. */
    private static final int EDGE_BAND = 4;

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

        int w = frame.getWidth();
        int h = frame.getHeight();
        log.debug("Eliminando fondo de frame {}x{}", w, h);

        // 1. Papel + balance de blancos
        int[] px = frame.getRGB(0, 0, w, h, null, 0, w);
        int[] paper = Paper.estimate(px);
        int[] balanced = Paper.whiteBalance(px, paper);
        int[] gray = luminance(balanced);

        // 2. Mediana 3x3
        int[] filtered = median3x3(gray, w, h);

        // 3. Canny → dilatar → cerrar
        boolean[] edges = cannyEdges(filtered, w, h);
        boolean[] blocked = Morphology.close(
                Morphology.dilate(edges, w, h, DILATE_RADIUS), w, h, CLOSE_RADIUS);

        // 4. Contenido seguro (oscuro)
        for (int i = 0; i < blocked.length; i++) {
            if (filtered[i] < SAFE_CONTENT_LUMINANCE) blocked[i] = true;
        }

        // 5. Flood fill desde los bordes
        boolean[] bg = new boolean[w * h];
        Morphology.spreadFromSeeds(bg, blocked, w, h,
                strategy == FloodFillStrategy.DFS, true);

        // 6. Expandir fondo hacia el papel residual
        Morphology.expandIntoLight(bg, filtered, w, h, NEAR_WHITE_LUMINANCE);

        // 7. Componentes pequeños y líneas de la plantilla
        removeSmallComponents(bg, w, h);
        removeLongLines(bg, w, h);

        // 8. Alpha suave + descontaminación de color
        return AlphaMatte.compose(balanced, bg, w, h, EDGE_BAND, applyAntialiasing);
    }

    // ============================================================
    // Etapas
    // ============================================================

    private boolean[] cannyEdges(int[] gray, int w, int h) {
        int[][] g2 = new int[h][w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(gray, y * w, g2[y], 0, w);
        }
        boolean[][] e2 = canny.detect(g2, CANNY_LOW, CANNY_HIGH);
        boolean[] edges = new boolean[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                edges[y * w + x] = e2[y][x];
            }
        }
        return edges;
    }

    /** Conserva solo los componentes grandes (los 3 mayores con área mínima). */
    private void removeSmallComponents(boolean[] bg, int w, int h) {
        Morphology.Components comps = Morphology.label(bg, w, h);
        if (comps.count == 0) return;

        int minArea = (int) (w * h * MIN_COMPONENT_AREA_RATIO);

        Integer[] order = new Integer[comps.count];
        for (int i = 0; i < order.length; i++) order[i] = i + 1;
        java.util.Arrays.sort(order, (a, b) -> Integer.compare(comps.area[b], comps.area[a]));

        boolean[] keep = new boolean[comps.count + 1];
        int kept = 0;
        for (int i = 0; i < Math.min(order.length, MAX_COMPONENTS); i++) {
            if (comps.area[order[i]] >= minArea) {
                keep[order[i]] = true;
                kept++;
            }
        }
        log.debug("Conservando {} componentes de {}", kept, comps.count);

        for (int i = 0; i < bg.length; i++) {
            int l = comps.label[i];
            if (l != 0 && !keep[l]) bg[i] = true;
        }
    }

    /** Elimina componentes con forma de línea (restos de las guías de la plantilla). */
    private void removeLongLines(boolean[] bg, int w, int h) {
        Morphology.Components comps = Morphology.label(bg, w, h);
        if (comps.count == 0) return;

        int maxLineArea = (int) (w * h * MAX_LINE_AREA_RATIO);
        boolean[] kill = new boolean[comps.count + 1];
        boolean any = false;

        for (int id = 1; id <= comps.count; id++) {
            if (comps.area[id] <= 0 || comps.area[id] > maxLineArea) continue;
            double bw = comps.bboxWidth(id);
            double bh = comps.bboxHeight(id);
            double aspect = Math.max(bw / bh, bh / bw);
            if (aspect > MAX_ASPECT_RATIO) {
                kill[id] = true;
                any = true;
            }
        }
        if (!any) return;

        for (int i = 0; i < bg.length; i++) {
            int l = comps.label[i];
            if (l != 0 && kill[l]) bg[i] = true;
        }
    }

    // ============================================================
    // Utilidades
    // ============================================================

    private static int[] luminance(int[] rgb) {
        int[] g = new int[rgb.length];
        for (int i = 0; i < rgb.length; i++) {
            g[i] = Paper.luminance(rgb[i]);
        }
        return g;
    }

    /** Mediana 3x3 con bordes replicados (ordenación de 9 valores). */
    static int[] median3x3(int[] src, int w, int h) {
        int[] out = new int[src.length];
        int[] v = new int[9];
        for (int y = 0; y < h; y++) {
            int ym = Math.max(0, y - 1) * w;
            int y0 = y * w;
            int yp = Math.min(h - 1, y + 1) * w;
            for (int x = 0; x < w; x++) {
                int xm = Math.max(0, x - 1);
                int xp = Math.min(w - 1, x + 1);
                v[0] = src[ym + xm]; v[1] = src[ym + x]; v[2] = src[ym + xp];
                v[3] = src[y0 + xm]; v[4] = src[y0 + x]; v[5] = src[y0 + xp];
                v[6] = src[yp + xm]; v[7] = src[yp + x]; v[8] = src[yp + xp];
                // ordenación por inserción de 9 elementos
                for (int i = 1; i < 9; i++) {
                    int key = v[i];
                    int j = i - 1;
                    while (j >= 0 && v[j] > key) {
                        v[j + 1] = v[j];
                        j--;
                    }
                    v[j + 1] = key;
                }
                out[y0 + x] = v[4];
            }
        }
        return out;
    }
}
