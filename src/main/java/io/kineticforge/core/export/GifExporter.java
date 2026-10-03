package io.kineticforge.core.export;

import com.madgag.gif.fmsware.AnimatedGifEncoder;
import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Exportador de animaciones GIF usando AnimatedGifEncoder de madgag.
 * Usa cuantización NeuQuant para mejor calidad de imagen.
 *
 * @author KineticForge Team
 * @version 4.0.0
 * @since 2026
 */
public class GifExporter implements Exporter {

    private static final Logger log = LoggerFactory.getLogger(GifExporter.class);

    @Override
    public String getFormatName() {
        return "GIF";
    }

    @Override
    public String getFileExtension() {
        return "gif";
    }

    @Override
    public boolean supportsTransparency() {
        return true;
    }

    @Override
    public void export(List<BufferedImage> frames, ExportConfig config, Path output) {
        Objects.requireNonNull(frames, "frames no puede ser nulo");
        Objects.requireNonNull(config, "config no puede ser nulo");
        Objects.requireNonNull(output, "output no puede ser nulo");

        if (frames.isEmpty()) {
            throw new ExportException("No hay frames para exportar");
        }

        log.info("Exportando GIF: {} frames, {} fps, loop={}",
            frames.size(), config.fps(), config.loop());

        int width = frames.get(0).getWidth();
        int height = frames.get(0).getHeight();
        for (int i = 1; i < frames.size(); i++) {
            BufferedImage f = frames.get(i);
            if (f.getWidth() != width || f.getHeight() != height) {
                throw new ExportException(String.format(
                    "Todos los frames deben tener el mismo tamaño. "
                        + "Frame 0: %dx%d, Frame %d: %dx%d",
                    width, height, i, f.getWidth(), f.getHeight()));
            }
        }

        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ExportException("No se pudo crear el directorio: " + output, e);
        }

        try (OutputStream os = Files.newOutputStream(output)) {
            AnimatedGifEncoder encoder = new AnimatedGifEncoder();
            encoder.start(os);
            encoder.setRepeat(config.loop() ? 0 : 1);
            encoder.setDelay(config.getFrameDelayMs());
            encoder.setDispose(2);

            // Color "transparente" que no se parezca a nada del dibujo: si el
            // dibujo tiene rosas/magentas, el cuantizador los volvería transparentes.
            int key = chooseKeyColor(frames);
            encoder.setTransparent(new java.awt.Color(key));
            encoder.setQuality(qualityFor(frames.size(), width, height));

            for (BufferedImage frame : frames) {
                BufferedImage prepared = prepareFrame(frame, key);
                encoder.addFrame(prepared);
            }

            encoder.finish();
            log.info("GIF exportado: {}", output);

        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el GIF en: " + output, e);
        }
    }

    /**
     * Calidad del cuantizador NeuQuant (1 = mejor, 30 = más rápido).
     * Con animaciones chicas se usa la mejor; con muchas/grandes se baja para no tardar minutos.
     */
    static int qualityFor(int frameCount, int width, int height) {
        long totalPixels = (long) frameCount * width * height;
        if (totalPixels <= 12_000_000L) return 1;
        if (totalPixels <= 40_000_000L) return 3;
        return 8;
    }

    private static final int[] KEY_CANDIDATES = {
            0xFF00FF, 0x00FF00, 0x00FFFF, 0xFFFF00, 0xFF0000, 0x0000FF
    };

    /**
     * Elige, de una lista de colores chillones, el que esté más lejos de todos
     * los colores opacos que aparecen en los frames.
     */
    static int chooseKeyColor(List<BufferedImage> frames) {
        long[] minDist = new long[KEY_CANDIDATES.length];
        java.util.Arrays.fill(minDist, Long.MAX_VALUE);

        for (BufferedImage frame : frames) {
            int w = frame.getWidth();
            int h = frame.getHeight();
            int[] px = frame.getRGB(0, 0, w, h, null, 0, w);
            int stride = Math.max(1, px.length / 20_000);
            for (int i = 0; i < px.length; i += stride) {
                int argb = px[i];
                if (((argb >> 24) & 0xFF) < 128) continue;
                int r = (argb >> 16) & 0xFF;
                int g = (argb >> 8) & 0xFF;
                int b = argb & 0xFF;
                for (int k = 0; k < KEY_CANDIDATES.length; k++) {
                    int c = KEY_CANDIDATES[k];
                    long dr = r - ((c >> 16) & 0xFF);
                    long dg = g - ((c >> 8) & 0xFF);
                    long db = b - (c & 0xFF);
                    long d = dr * dr + dg * dg + db * db;
                    if (d < minDist[k]) minDist[k] = d;
                }
            }
        }

        int best = 0;
        for (int k = 1; k < KEY_CANDIDATES.length; k++) {
            if (minDist[k] > minDist[best]) best = k;
        }
        return KEY_CANDIDATES[best];
    }

    private BufferedImage prepareFrame(BufferedImage frame, int keyColor) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        int[] src = frame.getRGB(0, 0, width, height, null, 0, width);
        int[] dst = new int[src.length];

        for (int i = 0; i < src.length; i++) {
            int argb = src[i];
            int alpha = (argb >> 24) & 0xFF;
            // El color de los píxeles de borde ya viene descontaminado (tinta pura,
            // sin mezcla de blanco), así que un corte de alpha binario no deja aura.
            dst[i] = alpha < 128 ? keyColor : (argb & 0x00FFFFFF);
        }

        BufferedImage prepared = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        prepared.setRGB(0, 0, width, height, dst, 0, width);
        return prepared;
    }
}
