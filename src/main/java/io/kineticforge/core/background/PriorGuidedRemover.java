package io.kineticforge.core.background;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.util.Objects;

/**
 * Quitafondos híbrido: una máscara "a priori" (por ejemplo la de la IA U2-Net)
 * decide QUÉ es sujeto y QUÉ es fondo, y la tinta del dibujo decide el borde EXACTO.
 *
 * <p>Los modelos de saliencia como U2-Net trabajan a 320×320: su máscara
 * tiene un error de borde de varios píxeles al reescalarla, y en dibujos de
 * línea suele tragarse trazos finos o regalar un margen de papel. Por eso
 * la máscara NO se usa como alpha directo; se usa para:</p>
 * <ul>
 *     <li>descartar todo lo que está lejos del sujeto (manchas, restos de plantilla);</li>
 *     <li>decidir si una zona blanca encerrada por el dibujo es interior (ojo,
 *         camisa) o un hueco de fondo (espacio entre las piernas);</li>
 * </ul>
 * <p>El borde final sale del mismo matte con descontaminación de color que
 * usa {@link BackgroundRemover}, así que no queda aura.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class PriorGuidedRemover {

    private static final Logger log = LoggerFactory.getLogger(PriorGuidedRemover.class);

    /** Distancia al papel (0-441) a partir de la cual un píxel es "tinta". */
    private static final float INK_DISTANCE = 45f;
    private static final int NEAR_WHITE_LUMINANCE = 238;
    private static final int EDGE_BAND = 4;

    private final float priorThreshold;

    public PriorGuidedRemover() {
        this(0.5f);
    }

    /**
     * @param priorThreshold umbral (0.05-0.95) para considerar "sujeto" a la máscara
     */
    public PriorGuidedRemover(float priorThreshold) {
        this.priorThreshold = Math.max(0.05f, Math.min(0.95f, priorThreshold));
    }

    /**
     * @param src   imagen original
     * @param prior máscara [alto][ancho] con valores 0..1 (1 = sujeto)
     * @return imagen ARGB con fondo transparente, o {@code null} si la máscara
     *         es inútil (vacía o cubre todo) y conviene usar otro método
     */
    public BufferedImage removeBackground(BufferedImage src, float[][] prior) {
        Objects.requireNonNull(src, "src no puede ser nulo");
        Objects.requireNonNull(prior, "prior no puede ser nulo");

        int w = src.getWidth();
        int h = src.getHeight();
        int n = w * h;

        int[] px = src.getRGB(0, 0, w, h, null, 0, w);
        int[] paper = Paper.estimate(px);
        int[] balanced = Paper.whiteBalance(px, paper);
        int[] gray = new int[n];
        for (int i = 0; i < n; i++) gray[i] = Paper.luminance(balanced[i]);

        // --- máscara a priori ---
        boolean[] subject = new boolean[n];
        int subjectCount = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (prior[y][x] > priorThreshold) {
                    subject[y * w + x] = true;
                    subjectCount++;
                }
            }
        }
        double fraction = subjectCount / (double) n;
        if (fraction < 0.002 || fraction > 0.985) {
            log.warn("Máscara IA inutilizable (cubre {}%)", Math.round(fraction * 100));
            return null;
        }

        // --- tinta ---
        boolean[] ink = new boolean[n];
        for (int i = 0; i < n; i++) {
            int c = balanced[i];
            float dr = 255 - ((c >> 16) & 0xFF);
            float dg = 255 - ((c >> 8) & 0xFF);
            float db = 255 - (c & 0xFF);
            ink[i] = Math.sqrt(dr * dr + dg * dg + db * db) > INK_DISTANCE;
        }
        int closeR = Math.max(2, Math.round(Math.min(w, h) * 0.006f));
        boolean[] sealed = Morphology.close(ink, w, h, closeR);

        // Zona de confianza: sujeto + margen para el error de borde del modelo
        int margin = Math.max(3, (int) Math.ceil(Math.max(w, h) / 320.0 * 2.5) + 2);
        boolean[] zone = Morphology.dilate(subject, w, h, margin);
        zone = attachConnectedInk(zone, sealed, w, h, closeR + 2);

        // --- fondo: todo lo que está fuera de la zona + lo que se conecta a él ---
        boolean[] bg = new boolean[n];
        for (int i = 0; i < n; i++) bg[i] = !zone[i];
        Morphology.spreadFromSeeds(bg, sealed, w, h, false, true);

        // --- huecos internos: los decide la IA ---
        boolean[] notHole = new boolean[n];
        for (int i = 0; i < n; i++) notHole[i] = bg[i] || ink[i];
        Morphology.Components holes = Morphology.label(notHole, w, h);
        if (holes.count > 0) {
            double[] sum = new double[holes.count + 1];
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int l = holes.label[y * w + x];
                    if (l != 0) sum[l] += prior[y][x];
                }
            }
            boolean[] clear = new boolean[holes.count + 1];
            for (int id = 1; id <= holes.count; id++) {
                clear[id] = (sum[id] / holes.area[id]) < priorThreshold;
            }
            for (int i = 0; i < n; i++) {
                int l = holes.label[i];
                if (l != 0 && clear[l]) bg[i] = true;
            }
        }

        // --- papel residual pegado al dibujo ---
        Morphology.expandIntoLight(bg, gray, w, h, NEAR_WHITE_LUMINANCE);

        // --- manchas sueltas lejos del sujeto ---
        removeStrays(bg, prior, w, h);

        return AlphaMatte.compose(balanced, bg, w, h, EDGE_BAND, true);
    }

    /**
     * La IA suele perder trazos finos (brazos, antenas). Todo trazo conectado
     * a la zona del sujeto se incorpora a la zona, aunque la máscara no lo cubra.
     */
    private boolean[] attachConnectedInk(boolean[] zone, boolean[] sealed, int w, int h, int grow) {
        int n = w * h;
        boolean[] notInk = new boolean[n];
        for (int i = 0; i < n; i++) notInk[i] = !sealed[i];
        Morphology.Components comps = Morphology.label(notInk, w, h);
        if (comps.count == 0) return zone;

        boolean[] attached = new boolean[comps.count + 1];
        for (int i = 0; i < n; i++) {
            int l = comps.label[i];
            if (l != 0 && zone[i]) attached[l] = true;
        }
        boolean[] extra = new boolean[n];
        boolean any = false;
        for (int i = 0; i < n; i++) {
            int l = comps.label[i];
            if (l != 0 && attached[l] && !zone[i]) {
                extra[i] = true;
                any = true;
            }
        }
        if (!any) return zone;

        extra = Morphology.dilate(extra, w, h, grow);
        for (int i = 0; i < n; i++) {
            if (extra[i]) zone[i] = true;
        }
        return zone;
    }

    /** Quita componentes chicos cuya máscara IA es baja (ruido / restos de plantilla). */
    private void removeStrays(boolean[] bg, float[][] prior, int w, int h) {
        Morphology.Components comps = Morphology.label(bg, w, h);
        if (comps.count == 0) return;

        int minArea = Math.max(24, (int) (w * h * 0.0005));
        double[] sum = new double[comps.count + 1];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int l = comps.label[y * w + x];
                if (l != 0) sum[l] += prior[y][x];
            }
        }
        boolean[] kill = new boolean[comps.count + 1];
        for (int id = 1; id <= comps.count; id++) {
            double mean = sum[id] / comps.area[id];
            if (comps.area[id] < minArea && mean < priorThreshold) kill[id] = true;
        }
        for (int i = 0; i < bg.length; i++) {
            int l = comps.label[i];
            if (l != 0 && kill[l]) bg[i] = true;
        }
    }
}
