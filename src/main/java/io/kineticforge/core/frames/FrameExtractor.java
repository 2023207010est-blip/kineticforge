package io.kineticforge.core.frames;

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
 * <p>Toma la imagen completa y una lista de rectángulos (calculados
 * a partir de las medidas EXACTAS de la plantilla), y devuelve cada
 * celda como una {@link BufferedImage} independiente.</p>
 *
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class FrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(FrameExtractor.class);

    /**
     * Extrae todos los frames de la imagen según los rectángulos dados.
     */
    public List<BufferedImage> extract(BufferedImage image, List<Rectangle> cells) {
        Objects.requireNonNull(image, "image no puede ser nulo");
        Objects.requireNonNull(cells, "cells no puede ser nulo");

        log.info("Extrayendo {} frames de imagen de {}×{}",
                cells.size(), image.getWidth(), image.getHeight());

        List<BufferedImage> frames = new ArrayList<>(cells.size());

        for (int i = 0; i < cells.size(); i++) {
            frames.add(extractSingleFrame(image, cells.get(i), i));
        }

        log.info("Extracción completa: {} frames", frames.size());
        return frames;
    }

    private BufferedImage extractSingleFrame(BufferedImage image, Rectangle cell, int index) {
        if (cell.x < 0 || cell.y < 0
                || cell.x + cell.width > image.getWidth()
                || cell.y + cell.height > image.getHeight()) {
            throw new ImageProcessingException(String.format(
                    "Rectángulo del frame %d está fuera de los límites: %s (imagen: %dx%d)",
                    index, cell, image.getWidth(), image.getHeight()));
        }

        BufferedImage frame = new BufferedImage(
                cell.width, cell.height, image.getType());

        for (int y = 0; y < cell.height; y++) {
            for (int x = 0; x < cell.width; x++) {
                int rgb = image.getRGB(cell.x + x, cell.y + y);
                frame.setRGB(x, y, rgb);
            }
        }

        return frame;
    }
}
