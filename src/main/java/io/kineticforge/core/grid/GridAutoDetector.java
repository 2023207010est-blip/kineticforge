package io.kineticforge.core.grid;

import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

public final class GridAutoDetector {

    private static final int DARK_THRESHOLD = 240;
    private static final double MIN_RUN_RATIO = 0.60;
    private static final int MIN_LINE_GAP_PX = 15;
    private static final int MIN_CELL_PX = 25;
    private static final int MAX_LINES = 30;

    private GridAutoDetector() {}

    public static GridDetectionResult detect(BufferedImage image) {
        int w = image.getWidth();
        int h = image.getHeight();

        boolean[][] dark = binarize(image);

        // Runs horizontales
        int[] hRun = new int[h];
        for (int y = 0; y < h; y++) {
            int maxRun = 0, cur = 0;
            for (int x = 0; x < w; x++) {
                if (dark[y][x]) { cur++; if (cur > maxRun) maxRun = cur; }
                else cur = 0;
            }
            hRun[y] = maxRun;
        }

        // Runs verticales
        int[] vRun = new int[w];
        for (int x = 0; x < w; x++) {
            int maxRun = 0, cur = 0;
            for (int y = 0; y < h; y++) {
                if (dark[y][x]) { cur++; if (cur > maxRun) maxRun = cur; }
                else cur = 0;
            }
            vRun[x] = maxRun;
        }

        int hThresh = (int) (w * MIN_RUN_RATIO);
        int vThresh = (int) (h * MIN_RUN_RATIO);

        List<Integer> hLines = findLineCenters(hRun, hThresh, MIN_LINE_GAP_PX);
        List<Integer> vLines = findLineCenters(vRun, vThresh, MIN_LINE_GAP_PX);

        // ===== DEBUG =====
        int maxHRun = 0, maxVRun = 0;
        for (int v : hRun) if (v > maxHRun) maxHRun = v;
        for (int v : vRun) if (v > maxVRun) maxVRun = v;
        System.out.println("DEBUG === w=" + w + " h=" + h
            + " maxHRun=" + maxHRun + " (> " + hThresh + ")"
            + " maxVRun=" + maxVRun + " (> " + vThresh + ")"
            + " hLines=" + hLines.size() + " vLines=" + vLines.size());
        // =================

        if (hLines.size() < 2 || vLines.size() < 2) return null;

        int rows = hLines.size() - 1;
        int cols = vLines.size() - 1;

        if (rows < 2 || cols < 2 || rows > MAX_LINES || cols > MAX_LINES) return null;

        List<Rectangle> cells = new ArrayList<>(rows * cols);
        for (int r = 0; r < rows; r++) {
            int y1 = hLines.get(r);
            int y2 = hLines.get(r + 1);
            for (int c = 0; c < cols; c++) {
                int x1 = vLines.get(c);
                int x2 = vLines.get(c + 1);
                int cw = x2 - x1;
                int ch = y2 - y1;
                if (cw < MIN_CELL_PX || ch < MIN_CELL_PX) continue;
                cells.add(new Rectangle(x1, y1, cw, ch));
            }
        }

        if (cells.isEmpty()) return null;

        GridSpec spec = inferSpec(rows, cols);
        double expected = rows * (double) cols;
        double confidence = Math.min(1.0, cells.size() / expected);

        return new GridDetectionResult(spec, cells, confidence, w, h);
    }

    private static boolean[][] binarize(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        boolean[][] out = new boolean[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = img.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;
                int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                out[y][x] = lum < DARK_THRESHOLD;
            }
        }
        return out;
    }

    private static List<Integer> findLineCenters(int[] run, int threshold, int minGap) {
        List<Integer> centers = new ArrayList<>();
        int i = 0, n = run.length;
        while (i < n) {
            if (run[i] > threshold) {
                int start = i;
                while (i < n && run[i] > threshold) i++;
                int end = i - 1;
                int center = (start + end) / 2;
                if (centers.isEmpty() || center - centers.get(centers.size() - 1) >= minGap) {
                    centers.add(center);
                } else {
                    int last = centers.get(centers.size() - 1);
                    centers.set(centers.size() - 1, (last + center) / 2);
                }
            } else {
                i++;
            }
        }
        return centers;
    }

    private static GridSpec inferSpec(int rows, int cols) {
        GridPreset best = GridPreset.COMPACTA;
        int bestDist = Integer.MAX_VALUE;
        for (GridPreset p : GridPreset.values()) {
            int d = Math.abs(p.getRows() - rows) + Math.abs(p.getColumns() - cols);
            if (d < bestDist) { bestDist = d; best = p; }
        }
        return GridSpec.builder()
            .preset(best)
            .orientation(cols >= rows ? PageOrientation.LANDSCAPE : PageOrientation.PORTRAIT)
            .pageSize(PageSize.A4)
            .marginMm(13.0)
            .gutterMm(2.0)
            .build();
    }
}
