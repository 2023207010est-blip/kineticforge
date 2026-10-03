package io.kineticforge.core.background;

/**
 * Operaciones morfológicas y de propagación sobre máscaras planas
 * ({@code boolean[width*height]}). Todas son O(n) o O(n·r) sin objetos
 * temporales por píxel (mucho más rápido que las versiones con listas).
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class Morphology {

    private Morphology() {
    }

    /** Dilatación con elemento estructurante cuadrado (2r+1). */
    public static boolean[] dilate(boolean[] src, int w, int h, int r) {
        if (r <= 0) return src.clone();
        return pass(src, w, h, r, false);
    }

    /** Erosión con elemento cuadrado. Lo que está fuera de la imagen no erosiona. */
    public static boolean[] erode(boolean[] src, int w, int h, int r) {
        if (r <= 0) return src.clone();
        return pass(src, w, h, r, true);
    }

    /** Cierre morfológico: dilatar y luego erosionar. */
    public static boolean[] close(boolean[] src, int w, int h, int r) {
        return erode(dilate(src, w, h, r), w, h, r);
    }

    private static boolean[] pass(boolean[] src, int w, int h, int r, boolean erode) {
        boolean[] tmp = new boolean[w * h];
        boolean[] out = new boolean[w * h];
        int[] pre = new int[Math.max(w, h) + 1];

        for (int y = 0; y < h; y++) {
            int base = y * w;
            for (int x = 0; x < w; x++) {
                pre[x + 1] = pre[x] + (src[base + x] ? 1 : 0);
            }
            for (int x = 0; x < w; x++) {
                int lo = Math.max(0, x - r);
                int hi = Math.min(w - 1, x + r);
                int cnt = pre[hi + 1] - pre[lo];
                tmp[base + x] = erode ? cnt == (hi - lo + 1) : cnt > 0;
            }
        }
        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                pre[y + 1] = pre[y] + (tmp[y * w + x] ? 1 : 0);
            }
            for (int y = 0; y < h; y++) {
                int lo = Math.max(0, y - r);
                int hi = Math.min(h - 1, y + r);
                int cnt = pre[hi + 1] - pre[lo];
                out[y * w + x] = erode ? cnt == (hi - lo + 1) : cnt > 0;
            }
        }
        return out;
    }

    /**
     * Propaga {@code bg} (in-place) por 4-vecinos a través de píxeles NO bloqueados.
     * Los píxeles no bloqueados del borde de la imagen actúan también como semilla.
     *
     * @param bg      máscara de fondo (semillas iniciales; se modifica)
     * @param blocked píxeles que el fondo no puede atravesar
     * @param dfs     true = pila (DFS), false = cola (BFS)
     */
    public static void spreadFromSeeds(boolean[] bg, boolean[] blocked,
                                       int w, int h, boolean dfs, boolean seedBorders) {
        int n = w * h;
        int[] buf = new int[n];
        int head = 0, tail = 0;

        for (int i = 0; i < n; i++) {
            if (bg[i]) buf[tail++] = i;
        }
        if (seedBorders) {
            for (int x = 0; x < w; x++) {
                tail = trySeed(bg, blocked, buf, tail, x);
                tail = trySeed(bg, blocked, buf, tail, (h - 1) * w + x);
            }
            for (int y = 0; y < h; y++) {
                tail = trySeed(bg, blocked, buf, tail, y * w);
                tail = trySeed(bg, blocked, buf, tail, y * w + w - 1);
            }
        }

        while (head < tail) {
            int p = dfs ? buf[--tail] : buf[head++];
            int x = p % w;
            int y = p / w;
            if (x > 0) tail = trySeed(bg, blocked, buf, tail, p - 1);
            if (x < w - 1) tail = trySeed(bg, blocked, buf, tail, p + 1);
            if (y > 0) tail = trySeed(bg, blocked, buf, tail, p - w);
            if (y < h - 1) tail = trySeed(bg, blocked, buf, tail, p + w);
        }
    }

    private static int trySeed(boolean[] bg, boolean[] blocked, int[] buf, int tail, int idx) {
        if (bg[idx] || blocked[idx]) return tail;
        bg[idx] = true;
        buf[tail++] = idx;
        return tail;
    }

    /**
     * Expande el fondo (8 vecinos) hacia píxeles casi blancos (gris >= umbral).
     * Se come el borde de papel que la morfología dejó pegado al dibujo.
     */
    public static void expandIntoLight(boolean[] bg, int[] gray, int w, int h, int threshold) {
        int n = w * h;
        int[] buf = new int[n];
        int head = 0, tail = 0;
        for (int i = 0; i < n; i++) {
            if (bg[i]) buf[tail++] = i;
        }
        while (head < tail) {
            int p = buf[head++];
            int x = p % w;
            int y = p / w;
            for (int dy = -1; dy <= 1; dy++) {
                int ny = y + dy;
                if (ny < 0 || ny >= h) continue;
                for (int dx = -1; dx <= 1; dx++) {
                    int nx = x + dx;
                    if (nx < 0 || nx >= w) continue;
                    int q = ny * w + nx;
                    if (!bg[q] && gray[q] >= threshold) {
                        bg[q] = true;
                        buf[tail++] = q;
                    }
                }
            }
        }
    }

    /** Resultado del etiquetado de componentes conexos (8-vecinos) del "no fondo". */
    public static final class Components {
        /** Etiqueta por píxel (0 = fondo, 1..count = componente). */
        public final int[] label;
        public final int count;
        public final int[] area;
        public final int[] minX, minY, maxX, maxY;

        Components(int[] label, int count, int[] area,
                   int[] minX, int[] minY, int[] maxX, int[] maxY) {
            this.label = label;
            this.count = count;
            this.area = area;
            this.minX = minX;
            this.minY = minY;
            this.maxX = maxX;
            this.maxY = maxY;
        }

        public int bboxWidth(int id) { return maxX[id] - minX[id] + 1; }
        public int bboxHeight(int id) { return maxY[id] - minY[id] + 1; }
    }

    /** Etiqueta los componentes de los píxeles donde {@code excluded} es false. */
    public static Components label(boolean[] excluded, int w, int h) {
        int n = w * h;
        int[] label = new int[n];
        int[] stack = new int[n];

        int cap = 64;
        int[] area = new int[cap], minX = new int[cap], minY = new int[cap],
                maxX = new int[cap], maxY = new int[cap];
        int count = 0;

        for (int start = 0; start < n; start++) {
            if (excluded[start] || label[start] != 0) continue;

            count++;
            if (count >= cap) {
                cap *= 2;
                area = java.util.Arrays.copyOf(area, cap);
                minX = java.util.Arrays.copyOf(minX, cap);
                minY = java.util.Arrays.copyOf(minY, cap);
                maxX = java.util.Arrays.copyOf(maxX, cap);
                maxY = java.util.Arrays.copyOf(maxY, cap);
            }
            minX[count] = Integer.MAX_VALUE;
            minY[count] = Integer.MAX_VALUE;
            maxX[count] = -1;
            maxY[count] = -1;

            int sp = 0;
            stack[sp++] = start;
            label[start] = count;

            while (sp > 0) {
                int p = stack[--sp];
                int x = p % w;
                int y = p / w;
                area[count]++;
                if (x < minX[count]) minX[count] = x;
                if (x > maxX[count]) maxX[count] = x;
                if (y < minY[count]) minY[count] = y;
                if (y > maxY[count]) maxY[count] = y;

                for (int dy = -1; dy <= 1; dy++) {
                    int ny = y + dy;
                    if (ny < 0 || ny >= h) continue;
                    for (int dx = -1; dx <= 1; dx++) {
                        int nx = x + dx;
                        if (nx < 0 || nx >= w) continue;
                        int q = ny * w + nx;
                        if (!excluded[q] && label[q] == 0) {
                            label[q] = count;
                            stack[sp++] = q;
                        }
                    }
                }
            }
        }
        return new Components(label, count, area, minX, minY, maxX, maxY);
    }
}
