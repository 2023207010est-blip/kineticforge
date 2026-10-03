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
 * @version 4.0.0
 * @since 2026
 */
public class FrameExtractor {

    private static final Logger log = LoggerFactory.getLogger(FrameExtractor.class);

    /** Margen interno por defecto (en píxeles) para evitar las líneas del template. */
    public static final int DEFAULT_INNER_MARGIN_PX = 8;

    private final int innerMargin;

    public FrameExtractor() {
        this(DEFAULT_INNER_MARGIN_PX);
    }

    /**
     * @param innerMargin margen interno en píxeles (0 = recorte exacto de la celda)
     */
    public FrameExtractor(int innerMargin) {
        if (innerMargin < 0) {
            throw new IllegalArgumentException("innerMargin no puede ser negativo");
        }
        this.innerMargin = innerMargin;
    }

    /**
     * Extrae todos los frames de la imagen según los rectángulos dados.
     */
    public List<BufferedImage> extract(BufferedImage image, List<Rectangle> cells) {
        Objects.requireNonNull(image, "image no puede ser nulo");
        Objects.requireNonNull(cells, "cells no puede ser nulo");

        log.info("Extrayendo {} frames de imagen de {}x{} (margen interno: {} px)",
                cells.size(), image.getWidth(), image.getHeight(), innerMargin);

        List<BufferedImage> frames = new ArrayList<>(cells.size());

        for (int i = 0; i < cells.size(); i++) {
            frames.add(extractSingleFrame(image, cells.get(i), i));
        }

        log.info("Extracción completa: {} frames", frames.size());
        return frames;
    }

    private BufferedImage extractSingleFrame(BufferedImage image, Rectangle cell, int index) {
        // Recortar con margen interno
        int x = cell.x + innerMargin;
        int y = cell.y + innerMargin;
        int w = cell.width - 2 * innerMargin;
        int h = cell.height - 2 * innerMargin;

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

        // getType() puede ser 0 (imágenes TIFF/16 bits): se usa siempre un tipo estándar
        int type = image.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        BufferedImage frame = new BufferedImage(w, h, type);
        int[] pixels = image.getRGB(x, y, w, h, null, 0, w);
        frame.setRGB(0, 0, w, h, pixels, 0, w);
        return frame;
    }
}
