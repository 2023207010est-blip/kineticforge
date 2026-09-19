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
 * Exportador de animaciones a PNG spritesheet.
 *
 * <p>Une todos los frames en una sola imagen PNG organizada en una grilla.
 * Útil para motores de videojuegos, animaciones CSS y herramientas de edición.</p>
 *
 * <p>La cantidad de columnas se calcula automáticamente como la raíz cuadrada
 * del total de frames, redondeada hacia arriba.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class SpritesheetExporter implements Exporter {

    private static final Logger log = LoggerFactory.getLogger(SpritesheetExporter.class);

    /** Padding entre celdas por defecto (en píxeles). */
    private static final int DEFAULT_PADDING = 0;

    @Override
    public String getFormatName() {
        return "PNG Spritesheet";
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

        // 1. Verificar que todos los frames sean del mismo tamaño
        int frameWidth = frames.get(0).getWidth();
        int frameHeight = frames.get(0).getHeight();

        for (int i = 1; i < frames.size(); i++) {
            BufferedImage f = frames.get(i);
            if (f.getWidth() != frameWidth || f.getHeight() != frameHeight) {
                throw new ExportException(String.format(
                    "Todos los frames deben tener el mismo tamaño. "
                        + "Frame 0: %dx%d, Frame %d: %dx%d",
                    frameWidth, frameHeight, i, f.getWidth(), f.getHeight()));
            }
        }

        // 2. Calcular columnas óptimas (raíz cuadrada)
        int columns = (int) Math.ceil(Math.sqrt(frames.size()));
        int rows = (int) Math.ceil((double) frames.size() / columns);

        log.info("Exportando spritesheet: {} frames → {}×{} ({}x{} px c/u)",
            frames.size(), columns, rows, frameWidth, frameHeight);

        // 3. Calcular tamaño total del spritesheet
        int padding = DEFAULT_PADDING;
        int sheetWidth = columns * frameWidth + (columns - 1) * padding;
        int sheetHeight = rows * frameHeight + (rows - 1) * padding;

        // 4. Crear la imagen del spritesheet
        BufferedImage sheet = new BufferedImage(sheetWidth, sheetHeight,
            BufferedImage.TYPE_INT_ARGB);

        // 5. Copiar cada frame en su posición de la grilla
        for (int i = 0; i < frames.size(); i++) {
            int row = i / columns;
            int col = i % columns;

            int x = col * (frameWidth + padding);
            int y = row * (frameHeight + padding);

            BufferedImage frame = frames.get(i);

            for (int fy = 0; fy < frameHeight; fy++) {
                for (int fx = 0; fx < frameWidth; fx++) {
                    int argb = frame.getRGB(fx, fy);
                    sheet.setRGB(x + fx, y + fy, argb);
                }
            }
        }

        // 6. Guardar como PNG
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(sheet, "png", output.toFile());
            log.info("Spritesheet exportado: {} ({}×{})",
                output, sheetWidth, sheetHeight);

        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el spritesheet en: " + output, e);
        }
    }
}
