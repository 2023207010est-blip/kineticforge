package io.kineticforge.core.export;

import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Exportador de imágenes PNG (primer frame de la animación).
 *
 * <p><strong>Nota:</strong> el writer PNG nativo de Java NO soporta
 * escribir secuencias animadas (APNG). Solo escribe una imagen individual.
 * Por lo tanto, este exportador genera un PNG con el <strong>primer frame</strong>.</p>
 *
 * <p>Para animaciones, se recomienda:</p>
 * <ul>
 *     <li>{@link GifExporter} — GIF animado con transparencia 1 bit</li>
 *     <li>{@link SpritesheetExporter} — todos los frames en una sola imagen</li>
 * </ul>
 *
 * <p>El soporte de APNG animado real está planificado para v1.1
 * (requiere librería externa como {@code imageio-apng}).</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class ApngExporter implements Exporter {

    private static final Logger log = LoggerFactory.getLogger(ApngExporter.class);

    @Override
    public String getFormatName() {
        return "PNG";
    }

    @Override
    public String getFileExtension() {
        return "png";
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

        log.info("Exportando PNG (primer frame de {}): {}", frames.size(), output);

        // Asegurar directorio padre
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ExportException("No se pudo crear el directorio: " + output, e);
        }

        // Tomar solo el primer frame
        BufferedImage firstFrame = frames.get(0);

        try {
            boolean written = ImageIO.write(firstFrame, "png", output.toFile());
            if (!written) {
                throw new ExportException("No se encontró writer para PNG");
            }
            log.info("PNG exportado: {} ({}×{})",
                output, firstFrame.getWidth(), firstFrame.getHeight());

        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el PNG en: " + output, e);
        }
    }
}
