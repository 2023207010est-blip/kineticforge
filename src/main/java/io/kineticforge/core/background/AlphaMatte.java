package io.kineticforge.core.background;

import java.awt.image.BufferedImage;

/**
 * Generación del canal alpha final con descontaminación de color.
 *
 * <p>Un píxel de borde de un dibujo sobre papel es una mezcla
 * {@code C = a·F + (1-a)·papel}. Los métodos anteriores dejaban el color
 * mezclado (casi blanco) con alpha parcial, y por eso aparecía un halo
 * blanco al ponerlo sobre un fondo oscuro. Acá se hace lo correcto:</p>
 * <ol>
 *     <li>Se estima el color de tinta puro {@code F} del entorno del píxel
 *         (solo píxeles colineales con la mezcla papel→tinta).</li>
 *     <li>Se resuelve {@code a} por proyección.</li>
 *     <li>Se escribe {@code F} (no {@code C}) como color: sin contaminación de blanco.</li>
 * </ol>
 *
 * <p>Solo se procesa la franja exterior (unos pocos píxeles) que separa el
 * fondo del dibujo. El interior queda opaco, y las zonas blancas encerradas
 * por el dibujo no se tocan.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class AlphaMatte {

    /** Por debajo de este alpha crudo, el píxel se descarta (mata la "pelusa"). */
    private static final float ALPHA_FLOOR = 0.06f;
    /** Por encima de este alpha crudo, el píxel se considera sólido. */
    private static final float ALPHA_SOLID = 0.94f;
    /** Un píxel con alpha crudo >= esto frena la propagación hacia el interior. */
    private static final float PROPAGATE_LIMIT = 0.90f;
    /** Coseno mínimo para considerar que dos píxeles son "la misma tinta". */
    private static final float MIN_COLLINEAR = 0.96f;
    /** Distancia mínima al papel para tratar algo como tinta firme. */
    private static final float MIN_INK_LENGTH = 40f;

    private AlphaMatte() {
    }

    /**
     * @param rgb        pixeles RGB con balance de blancos (papel = 255,255,255)
     * @param background máscara: true = fondo (alpha 0)
     * @param band       ancho en píxeles de la franja de borde a refinar
     * @param soft       false = alpha binario (0/255)
     */
    public static BufferedImage compose(int[] rgb, boolean[] background,
                                        int w, int h, int band, boolean soft) {
        int n = w * h;
        int[] out = new int[n];
        int[] depth = new int[n];
        int[] queue = new int[n];
        int head = 0, tail = 0;

        // Contenido opaco por defecto, fondo transparente
        for (int i = 0; i < n; i++) {
            out[i] = background[i] ? 0 : (0xFF000000 | (rgb[i] & 0xFFFFFF));
        }

        // Semillas: píxeles de contenido pegados al fondo (4-vecinos)
        for (int i = 0; i < n; i++) {
            if (background[i]) continue;
            int x = i % w;
            int y = i / w;
            boolean edge = (x > 0 && background[i - 1])
                    || (x < w - 1 && background[i + 1])
                    || (y > 0 && background[i - w])
                    || (y < h - 1 && background[i + w]);
            if (edge) {
                depth[i] = 1;
                queue[tail++] = i;
            }
        }

        float[] fv = new float[3];
        while (head < tail) {
            int p = queue[head++];
            int x = p % w;
            int y = p / w;

            float raw = solve(rgb, background, w, h, x, y, band, fv);
            float alpha = remap(raw, soft);

            if (alpha <= 0f) {
                out[p] = 0;
            } else if (raw >= ALPHA_SOLID) {
                // sólido: color original
                out[p] = 0xFF000000 | (rgb[p] & 0xFFFFFF);
            } else {
                int a = Math.round(alpha * 255f);
                int r = clamp255(255f - fv[0]);
                int g = clamp255(255f - fv[1]);
                int b = clamp255(255f - fv[2]);
                out[p] = (a << 24) | (r << 16) | (g << 8) | b;
            }

            // Seguimos hacia el interior solo a través de píxeles NO sólidos
            if (raw < PROPAGATE_LIMIT && depth[p] < band) {
                int d = depth[p] + 1;
                if (x > 0) tail = push(background, depth, queue, tail, p - 1, d);
                if (x < w - 1) tail = push(background, depth, queue, tail, p + 1, d);
                if (y > 0) tail = push(background, depth, queue, tail, p - w, d);
                if (y < h - 1) tail = push(background, depth, queue, tail, p + w, d);
            }
        }

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        result.setRGB(0, 0, w, h, out, 0, w);
        return result;
    }

    private static int push(boolean[] bg, int[] depth, int[] queue, int tail, int idx, int d) {
        if (bg[idx] || depth[idx] != 0) return tail;
        depth[idx] = d;
        queue[tail++] = idx;
        return tail;
    }

    private static float remap(float raw, boolean soft) {
        if (!soft) return raw >= 0.5f ? 1f : 0f;
        float a = (raw - ALPHA_FLOOR) / (ALPHA_SOLID - ALPHA_FLOOR);
        return a < 0f ? 0f : Math.min(a, 1f);
    }

    /**
     * Calcula el alpha crudo (0..1) del píxel y deja en {@code fv} el
     * vector (papel - tinta) estimado.
     */
    private static float solve(int[] rgb, boolean[] bg, int w, int h,
                               int x, int y, int radius, float[] fv) {
        int c = rgb[y * w + x];
        float dr = 255 - ((c >> 16) & 0xFF);
        float dg = 255 - ((c >> 8) & 0xFF);
        float db = 255 - (c & 0xFF);
        float lp = (float) Math.sqrt(dr * dr + dg * dg + db * db);

        if (lp < 4f) {
            fv[0] = fv[1] = fv[2] = 0f;
            return 0f;
        }

        int y0 = Math.max(0, y - radius), y1 = Math.min(h - 1, y + radius);
        int x0 = Math.max(0, x - radius), x1 = Math.min(w - 1, x + radius);

        // 1) Longitud máxima entre los vecinos colineales con este píxel
        float maxLen = lp;
        for (int yy = y0; yy <= y1; yy++) {
            for (int xx = x0; xx <= x1; xx++) {
                int idx = yy * w + xx;
                if (bg[idx]) continue;
                int q = rgb[idx];
                float qr = 255 - ((q >> 16) & 0xFF);
                float qg = 255 - ((q >> 8) & 0xFF);
                float qb = 255 - (q & 0xFF);
                float lq = (float) Math.sqrt(qr * qr + qg * qg + qb * qb);
                if (lq <= maxLen) continue;
                float cos = (dr * qr + dg * qg + db * qb) / (lp * lq);
                if (cos >= MIN_COLLINEAR) maxLen = lq;
            }
        }

        // 2) Promedio de los colineales cercanos a esa longitud máxima (tinta "sólida")
        float lim = maxLen * 0.90f;
        float sr = 0, sg = 0, sb = 0;
        int cnt = 0;
        for (int yy = y0; yy <= y1; yy++) {
            for (int xx = x0; xx <= x1; xx++) {
                int idx = yy * w + xx;
                if (bg[idx]) continue;
                int q = rgb[idx];
                float qr = 255 - ((q >> 16) & 0xFF);
                float qg = 255 - ((q >> 8) & 0xFF);
                float qb = 255 - (q & 0xFF);
                float lq = (float) Math.sqrt(qr * qr + qg * qg + qb * qb);
                if (lq < lim || lq < 1f) continue;
                float cos = (dr * qr + dg * qg + db * qb) / (lp * lq);
                if (cos >= MIN_COLLINEAR) {
                    sr += qr;
                    sg += qg;
                    sb += qb;
                    cnt++;
                }
            }
        }
        if (cnt == 0) {
            fv[0] = dr;
            fv[1] = dg;
            fv[2] = db;
            return 1f;
        }
        fv[0] = sr / cnt;
        fv[1] = sg / cnt;
        fv[2] = sb / cnt;

        float lf = (float) Math.sqrt(fv[0] * fv[0] + fv[1] * fv[1] + fv[2] * fv[2]);
        if (lf < 1f) return 0f;

        float proj = (dr * fv[0] + dg * fv[1] + db * fv[2]) / (lf * Math.max(lf, MIN_INK_LENGTH));
        return proj < 0f ? 0f : Math.min(proj, 1f);
    }

    private static int clamp255(float v) {
        int i = Math.round(v);
        return i < 0 ? 0 : Math.min(i, 255);
    }
}
