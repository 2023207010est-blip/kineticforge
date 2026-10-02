package io.kineticforge.core.background.ml;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import io.kineticforge.exception.ImageProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

/**
 * Removedor de fondo usando el modelo U2-Net con ONNX Runtime.
 *
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class U2NetBackgroundRemover {

    private static final Logger log = LoggerFactory.getLogger(U2NetBackgroundRemover.class);

    private static final int INPUT_SIZE = 320;
    private static final float MASK_THRESHOLD = 0.5f;

    /** Radio de erosión para comerse el "aura" de los bordes. */
    private static final int ERODE_RADIUS = 1;

    private final OrtEnvironment environment;
    private final OrtSession session;

    public U2NetBackgroundRemover(Path modelPath) {
        try {
            log.info("Cargando modelo U2-Net desde: {}", modelPath);

            this.environment = OrtEnvironment.getEnvironment();
            OrtSession.SessionOptions options = new OrtSession.SessionOptions();
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT);

            // Limitar threads para no saturar el CPU
            int cores = Runtime.getRuntime().availableProcessors();
            int onnxThreads = Math.max(1, cores / 4); // Solo 1/4 de los núcleos
            options.setIntraOpNumThreads(onnxThreads);
            options.setInterOpNumThreads(1);
            log.info("ONNX configurado con {} threads (de {} núcleos)", onnxThreads, cores);

            this.session = environment.createSession(modelPath.toString(), options);
            log.info("Modelo cargado. Outputs: {}", session.getOutputNames().size());

        } catch (OrtException e) {
            throw new ImageProcessingException(
                    "No se pudo cargar el modelo U2-Net: " + modelPath, e);
        }
    }

    public BufferedImage removeBackground(BufferedImage input) {
        try {
            int originalW = input.getWidth();
            int originalH = input.getHeight();

            float[] inputData = preprocess(input);
            float[] maskData = runInference(inputData);
            float[][] mask = reshapeMask(maskData, INPUT_SIZE, INPUT_SIZE);
            float[][] scaledMask = resizeMask(mask, originalW, originalH);

            // Aplicar máscara con alpha BINARIO
            boolean[][] contentMask = new boolean[originalH][originalW];
            for (int y = 0; y < originalH; y++) {
                for (int x = 0; x < originalW; x++) {
                    contentMask[y][x] = scaledMask[y][x] > MASK_THRESHOLD;
                }
            }

            // NUEVO: Erosionar el contenido para comerse el aura
            if (ERODE_RADIUS > 0) {
                contentMask = erode(contentMask, ERODE_RADIUS);
            }

            // Aplicar alpha binario
            BufferedImage result = new BufferedImage(originalW, originalH, BufferedImage.TYPE_INT_ARGB);
            for (int y = 0; y < originalH; y++) {
                for (int x = 0; x < originalW; x++) {
                    if (contentMask[y][x]) {
                        int rgb = input.getRGB(x, y);
                        result.setRGB(x, y, 0xFF000000 | (rgb & 0x00FFFFFF));
                    } else {
                        result.setRGB(x, y, 0x00000000);
                    }
                }
            }

            return result;

        } catch (OrtException e) {
            throw new ImageProcessingException("Error ejecutando inferencia U2-Net", e);
        }
    }

    /**
     * Erosiona el contenido (encoge 1px los bordes) para comerse el aura blanca.
     */
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

    private float[] preprocess(BufferedImage input) {
        BufferedImage resized = resizeImage(input, INPUT_SIZE, INPUT_SIZE);
        float[] data = new float[3 * INPUT_SIZE * INPUT_SIZE];
        float[] mean = {0.485f, 0.456f, 0.406f};
        float[] std = {0.229f, 0.224f, 0.225f};

        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int rgb = resized.getRGB(x, y);
                float r = ((rgb >> 16) & 0xFF) / 255.0f;
                float g = ((rgb >> 8) & 0xFF) / 255.0f;
                float b = (rgb & 0xFF) / 255.0f;

                int idx = y * INPUT_SIZE + x;
                data[0 * INPUT_SIZE * INPUT_SIZE + idx] = (r - mean[0]) / std[0];
                data[1 * INPUT_SIZE * INPUT_SIZE + idx] = (g - mean[1]) / std[1];
                data[2 * INPUT_SIZE * INPUT_SIZE + idx] = (b - mean[2]) / std[2];
            }
        }
        return data;
    }

    private float[] runInference(float[] inputData) throws OrtException {
        long[] shape = {1, 3, INPUT_SIZE, INPUT_SIZE};

        try (OnnxTensor inputTensor = OnnxTensor.createTensor(
                environment, FloatBuffer.wrap(inputData), shape)) {

            Map<String, OnnxTensor> inputs = Collections.singletonMap(
                    session.getInputNames().iterator().next(), inputTensor);

            try (OrtSession.Result result = session.run(inputs)) {
                Object output = result.get(0).getValue();

                if (output instanceof float[][][][] out4d) {
                    return flatten4D(out4d);
                } else if (output instanceof float[][][] out3d) {
                    return flatten3D(out3d);
                } else {
                    throw new ImageProcessingException("Formato inesperado: " + output.getClass());
                }
            }
        }
    }

    private float[] flatten4D(float[][][][] data) {
        return flatten3D(data[0]);
    }

    private float[] flatten3D(float[][][] data) {
        float[][] plane = data[0];
        int h = plane.length;
        int w = plane[0].length;
        float[] flat = new float[h * w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                flat[y * w + x] = plane[y][x];
            }
        }
        return flat;
    }

    private float[][] reshapeMask(float[] data, int h, int w) {
        float min = Float.MAX_VALUE, max = -Float.MAX_VALUE;
        for (float v : data) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        float range = max - min;

        float[][] mask = new float[h][w];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                mask[y][x] = range > 0 ? (data[y * w + x] - min) / range : 0;
            }
        }
        return mask;
    }

    private float[][] resizeMask(float[][] mask, int targetW, int targetH) {
        int srcH = mask.length;
        int srcW = mask[0].length;
        float[][] result = new float[targetH][targetW];
        float scaleX = (float) srcW / targetW;
        float scaleY = (float) srcH / targetH;

        for (int y = 0; y < targetH; y++) {
            for (int x = 0; x < targetW; x++) {
                int srcX = Math.min((int) (x * scaleX), srcW - 1);
                int srcY = Math.min((int) (y * scaleY), srcH - 1);
                result[y][x] = mask[srcY][srcX];
            }
        }
        return result;
    }

    private BufferedImage resizeImage(BufferedImage input, int targetW, int targetH) {
        BufferedImage resized = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = resized.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(input, 0, 0, targetW, targetH, null);
        g.dispose();
        return resized;
    }

    public void close() {
        try {
            if (session != null) session.close();
        } catch (OrtException e) {
            log.warn("Error cerrando sesión ONNX: {}", e.getMessage());
        }
    }
}
