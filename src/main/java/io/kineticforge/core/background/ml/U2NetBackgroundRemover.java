package io.kineticforge.core.background.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.kineticforge.core.background.BackgroundRemover;
import io.kineticforge.core.background.Paper;
import io.kineticforge.core.background.PriorGuidedRemover;
import io.kineticforge.exception.ImageProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Removedor de fondo con U2-Net (ONNX Runtime) + refinado de bordes.
 *
 * <p>La red solo aporta una máscara de "qué es sujeto"; el borde final lo
 * calcula {@link PriorGuidedRemover} a partir de la tinta del dibujo (ver
 * esa clase). Si la máscara de la red no sirve, se cae automáticamente al
 * {@link BackgroundRemover} clásico, de modo que siempre hay resultado.</p>
 *
 * <p>Cargar el modelo es caro: usar {@link #shared(Path)} para reutilizar la sesión.</p>
 *
 * @author KineticForge Team
 * @version 3.0.0
 * @since 2026
 */
public class U2NetBackgroundRemover implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(U2NetBackgroundRemover.class);

    private static final int INPUT_SIZE = 320;
    private static final float[] MEAN = {0.485f, 0.456f, 0.406f};
    private static final float[] STD = {0.229f, 0.224f, 0.225f};

    private static U2NetBackgroundRemover sharedInstance;
    private static Path sharedPath;

    private final OrtEnvironment environment;
    private final OrtSession session;
    private final String inputName;

    public U2NetBackgroundRemover(Path modelPath) {
        this(modelPath, Math.max(1, Runtime.getRuntime().availableProcessors() / 2));
    }

    public U2NetBackgroundRemover(Path modelPath, int threads) {
        try {
            log.info("Cargando modelo U2-Net desde: {}", modelPath);
            this.environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);
            options.setIntraOpNumThreads(Math.max(1, threads));
            options.setInterOpNumThreads(1);

            this.session = environment.createSession(modelPath.toString(), options);
            this.inputName = session.getInputNames().iterator().next();
            log.info("Modelo cargado ({} threads)", threads);
        } catch (OrtException e) {
            throw new ImageProcessingException(
                    "No se pudo cargar el modelo U2-Net: " + modelPath, e);
        }
    }

    /** Instancia compartida (se recarga solo si cambia el archivo del modelo). */
    public static synchronized U2NetBackgroundRemover shared(Path modelPath) {
        if (sharedInstance == null || !modelPath.equals(sharedPath)) {
            if (sharedInstance != null) {
                sharedInstance.close();
            }
            sharedInstance = new U2NetBackgroundRemover(modelPath);
            sharedPath = modelPath;
        }
        return sharedInstance;
    }

    /**
     * Quita el fondo usando la IA + refinado por tinta.
     *
     * @param input     imagen original
     * @param threshold umbral de la máscara IA (0.05-0.95; 0.5 recomendado)
     */
    public BufferedImage removeBackground(BufferedImage input, float threshold) {
        float[][] mask = predictMask(input);
        BufferedImage result = new PriorGuidedRemover(threshold).removeBackground(input, mask);
        if (result == null) {
            log.warn("La máscara IA no fue útil; se usa el quitafondos clásico");
            result = new BackgroundRemover().removeBackground(input);
        }
        return result;
    }

    public BufferedImage removeBackground(BufferedImage input) {
        return removeBackground(input, 0.5f);
    }

    /**
     * Máscara de saliencia a la resolución original, valores 0..1.
     */
    public synchronized float[][] predictMask(BufferedImage input) {
        try {
            int w = input.getWidth();
            int h = input.getHeight();

            float[] inputData = preprocess(input);
            float[] maskData = runInference(inputData);
            normalizeMinMax(maskData);
            return upscaleBilinear(maskData, INPUT_SIZE, INPUT_SIZE, w, h);
        } catch (OrtException e) {
            throw new ImageProcessingException("Error ejecutando inferencia U2-Net", e);
        }
    }

    // ============================================================
    // Preproceso (equivalente al de rembg)
    // ============================================================

    private float[] preprocess(BufferedImage input) {
        int w = input.getWidth();
        int h = input.getHeight();

        // Balance de blancos: el papel gris del escáner confunde a la red
        int[] px = input.getRGB(0, 0, w, h, null, 0, w);
        int[] balanced = Paper.whiteBalance(px, Paper.estimate(px));

        int plane = INPUT_SIZE * INPUT_SIZE;
        float[] rgb = new float[3 * plane];
        float max = 1f;

        // Reducción por promedio de área (evita perder trazos finos por aliasing)
        for (int y = 0; y < INPUT_SIZE; y++) {
            int y0 = (int) ((long) y * h / INPUT_SIZE);
            int y1 = Math.max(y0 + 1, (int) (((long) (y + 1) * h + INPUT_SIZE - 1) / INPUT_SIZE));
            y1 = Math.min(y1, h);
            for (int x = 0; x < INPUT_SIZE; x++) {
                int x0 = (int) ((long) x * w / INPUT_SIZE);
                int x1 = Math.max(x0 + 1, (int) (((long) (x + 1) * w + INPUT_SIZE - 1) / INPUT_SIZE));
                x1 = Math.min(x1, w);

                long sr = 0, sg = 0, sb = 0;
                int cnt = 0;
                for (int yy = y0; yy < y1; yy++) {
                    int base = yy * w;
                    for (int xx = x0; xx < x1; xx++) {
                        int c = balanced[base + xx];
                        sr += (c >> 16) & 0xFF;
                        sg += (c >> 8) & 0xFF;
                        sb += c & 0xFF;
                        cnt++;
                    }
                }
                int idx = y * INPUT_SIZE + x;
                float r = sr / (float) cnt;
                float g = sg / (float) cnt;
                float b = sb / (float) cnt;
                rgb[idx] = r;
                rgb[plane + idx] = g;
                rgb[2 * plane + idx] = b;
                max = Math.max(max, Math.max(r, Math.max(g, b)));
            }
        }

        float[] data = new float[3 * plane];
        for (int c = 0; c < 3; c++) {
            for (int i = 0; i < plane; i++) {
                data[c * plane + i] = (rgb[c * plane + i] / max - MEAN[c]) / STD[c];
            }
        }
        return data;
    }

    private float[] runInference(float[] inputData) throws OrtException {
        long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                environment, FloatBuffer.wrap(inputData), shape);
             OrtSession.Result result = session.run(
                     Collections.<String, OnnxTensor>singletonMap(inputName, inputTensor))) {

            Object output = result.get(0).getValue();
            if (output instanceof float[][][][] out4d) {
                return flatten(out4d[0][0]);
            } else if (output instanceof float[][][] out3d) {
                return flatten(out3d[0]);
            }
            throw new ImageProcessingException("Formato de salida inesperado: " + output.getClass());
        }
    }

    private float[] flatten(float[][] plane) {
        int h = plane.length;
        int w = plane[0].length;
        float[] flat = new float[h * w];
        for (int y = 0; y < h; y++) {
            System.arraycopy(plane[y], 0, flat, y * w, w);
        }
        return flat;
    }

    private void normalizeMinMax(float[] data) {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float range = max - min;
        for (int i = 0; i < data.length; i++) {
            data[i] = range > 0 ? (data[i] - min) / range : 0f;
        }
    }

    /** Escalado bilineal (el vecino más cercano anterior dejaba bordes en escalera). */
    private float[][] upscaleBilinear(float[] src, int sw, int sh, int tw, int th) {
        float[][] out = new float[th][tw];
        float sx = sw / (float) tw;
        float sy = sh / (float) th;

        for (int y = 0; y < th; y++) {
            float fy = Math.max(0f, Math.min(sh - 1f, (y + 0.5f) * sy - 0.5f));
            int y0 = (int) fy;
            int y1 = Math.min(sh - 1, y0 + 1);
            float wy = fy - y0;
            for (int x = 0; x < tw; x++) {
                float fx = Math.max(0f, Math.min(sw - 1f, (x + 0.5f) * sx - 0.5f));
                int x0 = (int) fx;
                int x1 = Math.min(sw - 1, x0 + 1);
                float wx = fx - x0;

                float top = src[y0 * sw + x0] * (1 - wx) + src[y0 * sw + x1] * wx;
                float bot = src[y1 * sw + x0] * (1 - wx) + src[y1 * sw + x1] * wx;
                out[y][x] = top * (1 - wy) + bot * wy;
            }
        }
        return out;
    }

    @Override
    public void close() {
        try {
            if (session != null) session.close();
        } catch (OrtException e) {
            log.warn("Error cerrando sesión ONNX: {}", e.getMessage());
        }
    }
}
