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
 * @version 3.0.0
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
            encoder.setTransparent(new java.awt.Color(0xFF, 0x00, 0xFF));
            encoder.setQuality(10);

            for (BufferedImage frame : frames) {
                BufferedImage prepared = prepareFrame(frame);
                encoder.addFrame(prepared);
            }

            encoder.finish();
            log.info("GIF exportado: {}", output);

        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el GIF en: " + output, e);
        }
    }

    private BufferedImage prepareFrame(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        BufferedImage prepared = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = frame.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha < 128) {
                    prepared.setRGB(x, y, 0xFF00FF);
                } else {
                    prepared.setRGB(x, y, argb & 0x00FFFFFF);
                }
            }
        }

        return prepared;
    }
}
