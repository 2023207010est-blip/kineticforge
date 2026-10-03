package io.kineticforge.core.background;

/**
 * Detector de bordes Canny.
 *
 * <p>Implementación clásica de John Canny (1986). Pasos:</p>
 * <ol>
 *     <li>Suavizado gaussiano</li>
 *     <li>Gradiente (Sobel)</li>
 *     <li>Supresión de no-máximos</li>
 *     <li>Umbral doble (histéresis)</li>
 * </ol>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class CannyEdgeDetector {

    private static final double GAUSSIAN_SIGMA = 1.4;
    private static final int GAUSSIAN_KERNEL_RADIUS = 2;
    private static final int SOBEL_KERNEL_SIZE = 3;

    /**
     * Detecta bordes en la imagen dada.
     *
     * @param gray matriz de luminancia [height][width] (0-255)
     * @param lowThreshold  umbral bajo (para bordes débiles)
     * @param highThreshold umbral alto (para bordes fuertes)
     * @return matriz booleana [height][width] true = borde
     */
    public boolean[][] detect(int[][] gray, int lowThreshold, int highThreshold) {
        int height = gray.length;
        int width = gray[0].length;

        // 1. Suavizado gaussiano
        int[][] smoothed = gaussianBlur(gray);

        // 2. Gradiente (Sobel)
        double[][] magnitude = new double[height][width];
        double[][] direction = new double[height][width];
        computeGradient(smoothed, magnitude, direction);

        // 3. Supresión de no-máximos
        double[][] suppressed = nonMaxSuppression(magnitude, direction);

        // 4. Umbral doble (histéresis)
        return hysteresis(suppressed, lowThreshold, highThreshold);
    }

    // ============================================================
    // 1. Suavizado gaussiano
    // ============================================================

    private int[][] gaussianBlur(int[][] input) {
        int height = input.length;
        int width = input[0].length;
        double[][] kernel = buildGaussianKernel();
        int k = GAUSSIAN_KERNEL_RADIUS;

        int[][] output = new int[height][width];

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double sum = 0;
                double weightSum = 0;

                for (int dy = -k; dy <= k; dy++) {
                    for (int dx = -k; dx <= k; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            double w = kernel[dy + k][dx + k];
                            sum += input[ny][nx] * w;
                            weightSum += w;
                        }
                    }
                }

                output[y][x] = (int) Math.round(sum / weightSum);
            }
        }

        return output;
    }

    private double[][] buildGaussianKernel() {
        int size = 2 * GAUSSIAN_KERNEL_RADIUS + 1;
        double[][] kernel = new double[size][size];
        double sigma2 = 2 * GAUSSIAN_SIGMA * GAUSSIAN_SIGMA;
        double sum = 0;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int dy = y - GAUSSIAN_KERNEL_RADIUS;
                int dx = x - GAUSSIAN_KERNEL_RADIUS;
                double value = Math.exp(-(dx * dx + dy * dy) / sigma2);
                kernel[y][x] = value;
                sum += value;
            }
        }

        // Normalizar
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                kernel[y][x] /= sum;
            }
        }

        return kernel;
    }

    // ============================================================
    // 2. Gradiente (Sobel)
    // ============================================================

    private void computeGradient(int[][] input, double[][] magnitude, double[][] direction) {
        int height = input.length;
        int width = input[0].length;

        int[][] sobelX = {
                {-1, 0, 1},
                {-2, 0, 2},
                {-1, 0, 1}
        };

        int[][] sobelY = {
                {-1, -2, -1},
                {0, 0, 0},
                {1, 2, 1}
        };

        int k = SOBEL_KERNEL_SIZE / 2;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                double gx = 0, gy = 0;

                for (int dy = -k; dy <= k; dy++) {
                    for (int dx = -k; dx <= k; dx++) {
                        int ny = y + dy;
                        int nx = x + dx;
                        if (ny >= 0 && ny < height && nx >= 0 && nx < width) {
                            int pixel = input[ny][nx];
                            gx += pixel * sobelX[dy + k][dx + k];
                            gy += pixel * sobelY[dy + k][dx + k];
                        }
                    }
                }

                magnitude[y][x] = Math.sqrt(gx * gx + gy * gy);
                direction[y][x] = Math.atan2(gy, gx);
            }
        }
    }

    // ============================================================
    // 3. Supresión de no-máximos
    // ============================================================

    private double[][] nonMaxSuppression(double[][] magnitude, double[][] direction) {
        int height = magnitude.length;
        int width = magnitude[0].length;
        double[][] output = new double[height][width];

        for (int y = 1; y < height - 1; y++) {
            for (int x = 1; x < width - 1; x++) {
                double angle = Math.toDegrees(direction[y][x]);
                if (angle < 0) angle += 180;

                double m = magnitude[y][x];
                double neighbor1, neighbor2;

                // Cuantizar la dirección a 0, 45, 90 o 135 grados
                if ((angle >= 0 && angle < 22.5) || (angle >= 157.5 && angle < 180)) {
                    neighbor1 = magnitude[y][x - 1];
                    neighbor2 = magnitude[y][x + 1];
                } else if (angle >= 22.5 && angle < 67.5) {
                    neighbor1 = magnitude[y - 1][x + 1];
                    neighbor2 = magnitude[y + 1][x - 1];
                } else if (angle >= 67.5 && angle < 112.5) {
                    neighbor1 = magnitude[y - 1][x];
                    neighbor2 = magnitude[y + 1][x];
                } else {
                    neighbor1 = magnitude[y - 1][x - 1];
                    neighbor2 = magnitude[y + 1][x + 1];
                }

                if (m >= neighbor1 && m >= neighbor2) {
                    output[y][x] = m;
                } else {
                    output[y][x] = 0;
                }
            }
        }

        return output;
    }

    // ============================================================
    // 4. Umbral doble (histéresis)
    // ============================================================

    private boolean[][] hysteresis(double[][] suppressed, int lowThreshold, int highThreshold) {
        int height = suppressed.length;
        int width = suppressed[0].length;
        boolean[][] edges = new boolean[height][width];
        boolean[][] visited = new boolean[height][width];

        // Paso 1: marcar bordes fuertes (mayor a highThreshold)
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (suppressed[y][x] >= highThreshold) {
                    edges[y][x] = true;
                    visited[y][x] = true;
                }
            }
        }

        // Paso 2: expandir desde bordes fuertes a través de bordes débiles
        java.util.Deque<int[]> stack = new java.util.ArrayDeque<>();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (edges[y][x]) {
                    stack.push(new int[]{x, y});
                }
            }
        }

        while (!stack.isEmpty()) {
            int[] p = stack.pop();
            int x = p[0], y = p[1];

            for (int dy = -1; dy <= 1; dy++) {
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    int ny = y + dy;

                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) continue;
                    if (visited[ny][nx]) continue;

                    // Si es un borde débil (mayor a lowThreshold), marcarlo
                    if (suppressed[ny][nx] >= lowThreshold) {
                        edges[ny][nx] = true;
                        visited[ny][nx] = true;
                        stack.push(new int[]{nx, ny});
                    }
                }
            }
        }

        return edges;
    }
}
