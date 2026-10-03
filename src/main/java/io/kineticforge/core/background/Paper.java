package io.kineticforge.core.background;

/**
 * Estimación del color del papel escaneado y balance de blancos.
 *
 * <p>Un escaneo casi nunca tiene el papel en blanco puro (255,255,255):
 * suele quedar en 225-248 y con una leve dominante de color. Si se trabaja
 * con umbrales fijos, esa diferencia deja un "aura" clara alrededor del
 * dibujo. Normalizando el papel a blanco puro, todos los umbrales del
 * pipeline pasan a ser independientes del escáner.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class Paper {

    /** Si el "papel" estimado es más oscuro que esto, se asume blanco puro. */
    private static final int MIN_PAPER_LUMINANCE = 150;

    private Paper() {
    }

    /**
     * Estima el color del papel: promedio de los píxeles más claros
     * (alrededor del percentil 90 de luminancia).
     *
     * @param px pixeles ARGB
     * @return {r, g, b} del papel
     */
    public static int[] estimate(int[] px) {
        int n = px.length;
        int[] hist = new int[256];
        for (int i = 0; i < n; i++) {
            hist[luminance(px[i])]++;
        }

        long target = (long) (n * 0.90);
        long acc = 0;
        int p90 = 255;
        for (int v = 0; v < 256; v++) {
            acc += hist[v];
            if (acc >= target) {
                p90 = v;
                break;
            }
        }

        int lo = Math.max(0, p90 - 12);
        long sr = 0, sg = 0, sb = 0, count = 0;
        for (int i = 0; i < n; i++) {
            int c = px[i];
            if (luminance(c) >= lo) {
                sr += (c >> 16) & 0xFF;
                sg += (c >> 8) & 0xFF;
                sb += c & 0xFF;
                count++;
            }
        }

        if (count == 0) {
            return new int[]{255, 255, 255};
        }
        int r = (int) (sr / count);
        int g = (int) (sg / count);
        int b = (int) (sb / count);

        if (luminance((r << 16) | (g << 8) | b) < MIN_PAPER_LUMINANCE) {
            return new int[]{255, 255, 255};
        }
        return new int[]{Math.max(r, 120), Math.max(g, 120), Math.max(b, 120)};
    }

    /**
     * Devuelve una copia opaca donde el color del papel pasa a ser 255,255,255.
     */
    public static int[] whiteBalance(int[] px, int[] paper) {
        int n = px.length;
        int[] out = new int[n];
        float gr = 255f / paper[0];
        float gg = 255f / paper[1];
        float gb = 255f / paper[2];
        for (int i = 0; i < n; i++) {
            int c = px[i];
            int r = Math.min(255, Math.round(((c >> 16) & 0xFF) * gr));
            int g = Math.min(255, Math.round(((c >> 8) & 0xFF) * gg));
            int b = Math.min(255, Math.round((c & 0xFF) * gb));
            out[i] = 0xFF000000 | (r << 16) | (g << 8) | b;
        }
        return out;
    }

    /** Luminancia 0-255 de un pixel RGB/ARGB. */
    public static int luminance(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return (299 * r + 587 * g + 114 * b) / 1000;
    }
}
