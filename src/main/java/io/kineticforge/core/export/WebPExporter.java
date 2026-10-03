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
 * Exportador de imágenes en formato WebP.
 *
 * <p>La librería {@code webp-imageio} de Sejda solo soporta escritura de
 * imágenes individuales (WebP estático), no animaciones. Por lo tanto,
 * este exportador genera un WebP con el <strong>primer frame</strong>.</p>
 *
 * <p>Ventajas de WebP estático:</p>
 * <ul>
 *     <li>Mejor compresión que PNG o JPG</li>
 *     <li>Soporta transparencia de 8 bits</li>
 *     <li>Compatible con navegadores modernos</li>
 * </ul>
 *
 * <p>Para animaciones, se recomienda exportar a GIF o APNG.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class WebPExporter implements Exporter {

    private static final Logger log = LoggerFactory.getLogger(WebPExporter.class);

    @Override
    public String getFormatName() {
        return "WebP";
    }

    @Override
    public String getFileExtension() {
        return "webp";
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

        log.info("Exportando WebP (solo primer frame de {}): {}", frames.size(), output);

        // Verificar que exista el writer de WebP
        if (!ImageIO.getImageWritersByFormatName("webp").hasNext()) {
            throw new ExportException(
                "No hay writer de WebP disponible. "
                    + "Verificá la dependencia webp-imageio.");
        }

        // Asegurar que exista el directorio padre
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
            boolean written = ImageIO.write(firstFrame, "webp", output.toFile());
            if (!written) {
                throw new ExportException(
                    "No se encontró writer para WebP al intentar escribir");
            }
            log.info("WebP exportado: {} ({}×{})",
                output, firstFrame.getWidth(), firstFrame.getHeight());

        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el WebP en: " + output, e);
        }
    }
}
