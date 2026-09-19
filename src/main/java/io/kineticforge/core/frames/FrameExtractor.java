package io.kineticforge.core.frames;

import io.kineticforge.core.grid.GridDetectionResult;
import io.kineticforge.exception.ImageProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Extrae los frames individuales de una imagen escaneada.
 *
 * <p>Toma la imagen completa y los rectángulos detectados por el
 * {@link io.kineticforge.core.grid.GridDetector}, y devuelve cada
 * celda como una {@link BufferedImage} independiente.</p>
 *
 * <p>Los frames se devuelven en el mismo orden que los rectángulos:
 * de izquierda a derecha, de arriba a abajo.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class FrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(FrameExtractor.class);

    /**
     * Extrae todos los frames de la imagen.
     *
     * @param image  imagen escaneada completa
     * @param result resultado de la detección de grilla
     * @return lista de frames individuales
     * @throws ImageProcessingException si falla la extracción
     */
    public List<BufferedImage> extract(BufferedImage image, GridDetectionResult result) {
        Objects.requireNonNull(image, "image no puede ser nulo");
        Objects.requireNonNull(result, "result no puede ser nulo");

        log.info("Extrayendo {} frames de imagen de {}×{}",
            result.cellCount(), image.getWidth(), image.getHeight());

        List<BufferedImage> frames = new ArrayList<>(result.cellCount());

        for (int i = 0; i < result.cellCount(); i++) {
            Rectangle cell = result.cellAt(i);
            BufferedImage frame = extractSingleFrame(image, cell, i);
            frames.add(frame);
        }

        log.info("Extracción completa: {} frames", frames.size());
        return frames;
    }

    /**
     * Extrae un solo frame de la imagen.
     *
     * @param image     imagen completa
     * @param cell      rectángulo de la celda
     * @param frameIndex índice del frame (para mensajes)
     * @return frame individual
     */
    private BufferedImage extractSingleFrame(BufferedImage image, Rectangle cell, int frameIndex) {
        // Validar que el rectángulo esté dentro de los límites de la imagen
        if (cell.x < 0 || cell.y < 0
            || cell.x + cell.width > image.getWidth()
            || cell.y + cell.height > image.getHeight()) {
            throw new ImageProcessingException(String.format(
                "Rectángulo del frame %d está fuera de los límites: %s (imagen: %dx%d)",
                frameIndex, cell, image.getWidth(), image.getHeight()));
        }

        // Crear imagen del mismo tipo que la original
        BufferedImage frame = new BufferedImage(
            cell.width, cell.height, image.getType());

        // Copiar píxeles
        for (int y = 0; y < cell.height; y++) {
            for (int x = 0; x < cell.width; x++) {
                int rgb = image.getRGB(cell.x + x, cell.y + y);
                frame.setRGB(x, y, rgb);
            }
        }

        return frame;
    }
}
