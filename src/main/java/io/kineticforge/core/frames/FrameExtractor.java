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
 * <p>Recorta cada celda con un MARGEN INTERNO para evitar capturar
 * las líneas del template en el frame resultante.</p>
 *
 * @author KineticForge Team
 * @version 3.0.0
 * @since 2026
 */
public class FrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(FrameExtractor.class);

    /** Margen interno (en píxeles) para evitar las líneas del template. */
    private static final int INNER_MARGIN_PX = 8;

    /**
     * Extrae todos los frames de la imagen según los rectángulos dados.
     */
    public List<BufferedImage> extract(BufferedImage image, List<Rectangle> cells) {
        Objects.requireNonNull(image, "image no puede ser nulo");
        Objects.requireNonNull(cells, "cells no puede ser nulo");

        log.info("Extrayendo {} frames de imagen de {}x{} (margen interno: {} px)",
                cells.size(), image.getWidth(), image.getHeight(), INNER_MARGIN_PX);

        List<BufferedImage> frames = new ArrayList<>(cells.size());

        for (int i = 0; i < cells.size(); i++) {
            frames.add(extractSingleFrame(image, cells.get(i), i));
        }

        log.info("Extracción completa: {} frames", frames.size());
        return frames;
    }

    private BufferedImage extractSingleFrame(BufferedImage image, Rectangle cell, int index) {
        // Recortar con margen interno
        int x = cell.x + INNER_MARGIN_PX;
        int y = cell.y + INNER_MARGIN_PX;
        int w = cell.width - 2 * INNER_MARGIN_PX;
        int h = cell.height - 2 * INNER_MARGIN_PX;

        // Validar que quede espacio
        if (w <= 0 || h <= 0) {
            throw new ImageProcessingException(String.format(
                    "Celda %d es demasiado chica para el margen interno: %dx%d",
                    index, cell.width, cell.height));
        }

        // Ajustar si se sale de la imagen
        if (x < 0) { w += x; x = 0; }
        if (y < 0) { h += y; y = 0; }
        if (x + w > image.getWidth()) w = image.getWidth() - x;
        if (y + h > image.getHeight()) h = image.getHeight() - y;

        if (w <= 0 || h <= 0) {
            throw new ImageProcessingException(String.format(
                    "Celda %d fuera de límites: %s", index, cell));
        }

        BufferedImage frame = new BufferedImage(w, h, image.getType());

        for (int fy = 0; fy < h; fy++) {
            for (int fx = 0; fx < w; fx++) {
                int rgb = image.getRGB(x + fx, y + fy);
                frame.setRGB(fx, fy, rgb);
            }
        }

        return frame;
    }
}
