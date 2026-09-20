package io.kineticforge.core.grid;

import io.kineticforge.exception.GridDetectionException;
import io.kineticforge.model.GridSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Detector automático de grilla en imágenes escaneadas.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class GridDetector {

    private static final Logger log = LoggerFactory.getLogger(GridDetector.class);

    private static final int DEFAULT_BINARY_THRESHOLD = 230;
    private static final double LINE_DETECTION_RATIO = 0.3;

    private final BinarizationStrategy strategy;

    public GridDetector() {
        this(BinarizationStrategy.FIXED_THRESHOLD);
    }

    public GridDetector(BinarizationStrategy strategy) {
        this.strategy = Objects.requireNonNull(strategy, "strategy no puede ser nulo");
    }

    public GridDetectionResult detect(BufferedImage image, GridSpec spec) {
        Objects.requireNonNull(image, "image no puede ser nulo");
        Objects.requireNonNull(spec, "spec no puede ser nulo");

        log.info("Detectando grilla: {}×{} en imagen de {}×{}",
            spec.columns(), spec.rows(), image.getWidth(), image.getHeight());

        boolean[][] binary = binarize(image);
        int[] verticalProjection = projectVertical(binary);
        int[] horizontalProjection = projectHorizontal(binary);

        List<Integer> verticalLines = detectLines(verticalProjection,
            image.getHeight(), spec.columns() + 1);
        List<Integer> horizontalLines = detectLines(horizontalProjection,
            image.getWidth(), spec.rows() + 1);

        log.debug("Líneas verticales detectadas: {}", verticalLines.size());
        log.debug("Líneas horizontales detectadas: {}", horizontalLines.size());

        List<Rectangle> cells = buildCells(verticalLines, horizontalLines, spec);
        double confidence = computeConfidence(verticalLines, horizontalLines, spec);

        log.info("Detección completa: {} celdas, confianza={}",
            cells.size(), String.format("%.2f", confidence));

        return new GridDetectionResult(
            spec, cells, confidence, image.getWidth(), image.getHeight());
    }

    // ============================================================
    // Paso 1: Preprocesamiento
    // ============================================================

    private boolean[][] binarize(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        boolean[][] binary = new boolean[height][width];

        int threshold = computeThreshold(image);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);
                int luminance = computeLuminance(rgb);
                binary[y][x] = luminance < threshold;
            }
        }

        return binary;
    }

    private int computeThreshold(BufferedImage image) {
        int threshold = switch (strategy) {
            case OTSU -> computeOtsuThreshold(image);
            case FIXED_THRESHOLD -> DEFAULT_BINARY_THRESHOLD;
            case ADAPTIVE -> computeOtsuThreshold(image);
        };
        log.debug("Umbral de binarización: {} (estrategia: {})", threshold, strategy);
        return threshold;
    }

    private int computeLuminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (int) (0.299 * r + 0.587 * g + 0.114 * b);
    }

    private int computeOtsuThreshold(BufferedImage image) {
        int[] histogram = new int[256];
        int totalPixels = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int rgb = image.getRGB(x, y);
                int luminance = computeLuminance(rgb);
                histogram[luminance]++;
                totalPixels++;
            }
        }

        double sum = 0;
        for (int i = 0; i < 256; i++) {
            sum += i * histogram[i];
        }

        double sumB = 0;
        int wB = 0;
        int wF;
        double maxVariance = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++) {
            wB += histogram[t];
            if (wB == 0) continue;

            wF = totalPixels - wB;
            if (wF == 0) break;

            sumB += (double) t * histogram[t];

            double mB = sumB / wB;
            double mF = (sum - sumB) / wF;

            double variance = (double) wB * wF * (mB - mF) * (mB - mF);

            if (variance > maxVariance) {
                maxVariance = variance;
                threshold = t;
            }
        }

        log.debug("Umbral Otsu calculado: {}", threshold);
        return threshold;
    }

    // ============================================================
    // Paso 2: Proyecciones
    // ============================================================

    private int[] projectVertical(boolean[][] binary) {
        int height = binary.length;
        int width = binary[0].length;
        int[] projection = new int[width];

        for (int x = 0; x < width; x++) {
            int count = 0;
            for (int y = 0; y < height; y++) {
                if (binary[y][x]) count++;
            }
            projection[x] = count;
        }

        return projection;
    }

    private int[] projectHorizontal(boolean[][] binary) {
        int height = binary.length;
        int width = binary[0].length;
        int[] projection = new int[height];

        for (int y = 0; y < height; y++) {
            int count = 0;
            for (int x = 0; x < width; x++) {
                if (binary[y][x]) count++;
            }
            projection[y] = count;
        }

        return projection;
    }

    // ============================================================
    // Paso 3: Detección de líneas
    // ============================================================

    private List<Integer> detectLines(int[] projection, int maxValue, int expectedCount) {
        int threshold = (int) (maxValue * LINE_DETECTION_RATIO);

        List<Integer> lines = new ArrayList<>();
        int start = -1;

        for (int i = 0; i < projection.length; i++) {
            boolean isLinePixel = projection[i] >= threshold;

            if (isLinePixel && start == -1) {
                start = i;
            } else if (!isLinePixel && start != -1) {
                int center = (start + i - 1) / 2;
                lines.add(center);
                start = -1;
            }
        }

        if (start != -1) {
            int center = (start + projection.length - 1) / 2;
            lines.add(center);
        }

        log.debug("Detectadas {} líneas (esperadas: {})", lines.size(), expectedCount);
        return lines;
    }

    // ============================================================
    // Paso 4: Construcción de celdas (con filtro)
    // ============================================================

    private List<Rectangle> buildCells(List<Integer> verticalLines,
                                       List<Integer> horizontalLines,
                                       GridSpec spec) {
        int expectedCols = spec.columns();
        int expectedRows = spec.rows();

        if (verticalLines.size() < expectedCols + 1) {
            throw new GridDetectionException(String.format(
                "Se esperaban al menos %d líneas verticales, se detectaron %d",
                expectedCols + 1, verticalLines.size()));
        }
        if (horizontalLines.size() < expectedRows + 1) {
            throw new GridDetectionException(String.format(
                "Se esperaban al menos %d líneas horizontales, se detectaron %d",
                expectedRows + 1, horizontalLines.size()));
        }

        // FILTRO: quedarse con las líneas que mejor se ajusten al
        // espaciado uniforme esperado (descarta puntos guía)
        List<Integer> vLines = selectEvenlySpaced(verticalLines, expectedCols + 1);
        List<Integer> hLines = selectEvenlySpaced(horizontalLines, expectedRows + 1);

        log.debug("Verticales filtradas: {} → {}", verticalLines.size(), vLines.size());
        log.debug("Horizontales filtradas: {} → {}", horizontalLines.size(), hLines.size());

        List<Rectangle> cells = new ArrayList<>(expectedRows * expectedCols);

        for (int row = 0; row < expectedRows; row++) {
            for (int col = 0; col < expectedCols; col++) {
                int x1 = vLines.get(col);
                int x2 = vLines.get(col + 1);
                int y1 = hLines.get(row);
                int y2 = hLines.get(row + 1);

                int width = x2 - x1;
                int height = y2 - y1;

                if (width <= 0 || height <= 0) {
                    throw new GridDetectionException(String.format(
                        "Celda inválida en fila %d, col %d: %dx%d",
                        row, col, width, height));
                }

                cells.add(new Rectangle(x1, y1, width, height));
            }
        }

        return cells;
    }

    /**
     * De una lista de candidatos, selecciona el subconjunto de tamaño N
     * cuyas posiciones mejor se ajusten a un espaciado uniforme.
     * Descarta líneas "extra" (puntos guía) que estén demasiado cerca de otras.
     *
     * @param candidates candidatos ordenados por posición
     * @param n          cantidad deseada (filas + 1 o columnas + 1)
     * @return sublista con N elementos espaciados uniformemente
     */
    private List<Integer> selectEvenlySpaced(List<Integer> candidates, int n) {
        if (candidates.size() <= n) {
            return new ArrayList<>(candidates);
        }

        // Estrategia: para cada posición "ideal" (espaciado uniforme entre
        // el primero y el último candidato), elegir el candidato más cercano.
        // Evita repetir candidatos usando un conjunto de índices ya elegidos.

        List<Integer> selected = new ArrayList<>(n);
        boolean[] used = new boolean[candidates.size()];

        int first = candidates.get(0);
        int last = candidates.get(candidates.size() - 1);
        double step = (last - first) / (double) (n - 1);

        for (int i = 0; i < n; i++) {
            int target = (int) Math.round(first + step * i);

            // Buscar el candidato más cercano al target (no usado)
            int bestIdx = -1;
            int bestDist = Integer.MAX_VALUE;

            for (int j = 0; j < candidates.size(); j++) {
                if (used[j]) continue;
                int dist = Math.abs(candidates.get(j) - target);
                if (dist < bestDist) {
                    bestDist = dist;
                    bestIdx = j;
                }
            }

            if (bestIdx == -1) {
                bestIdx = candidates.size() - 1;
            }

            used[bestIdx] = true;
            selected.add(candidates.get(bestIdx));
        }

        // Ordenar de nuevo (por si el algoritmo los eligió desordenados)
        selected.sort(Integer::compareTo);

        log.debug("selectEvenlySpaced: {} candidatos → {} seleccionados",
            candidates.size(), selected.size());

        return selected;
    }

    // ============================================================
    // Paso 5: Confianza
    // ============================================================

    private double computeConfidence(List<Integer> verticalLines,
                                     List<Integer> horizontalLines,
                                     GridSpec spec) {
        int expectedV = spec.columns() + 1;
        int expectedH = spec.rows() + 1;

        double vRatio = Math.min(1.0, (double) verticalLines.size() / expectedV);
        double hRatio = Math.min(1.0, (double) horizontalLines.size() / expectedH);

        return (vRatio + hRatio) / 2.0;
    }
}
