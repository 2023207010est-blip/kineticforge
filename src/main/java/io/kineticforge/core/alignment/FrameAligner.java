package io.kineticforge.core.alignment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Alinea los frames de una animación para evitar la vibración visual.
 *
 * <p>El algoritmo:</p>
 * <ol>
 *     <li>Calcula el bounding box del contenido no transparente de cada frame</li>
 *     <li>Determina el tamaño máximo entre todos los bounding boxes</li>
 *     <li>Crea un canvas común del tamaño máximo</li>
 *     <li>Dibuja cada frame centrado en ese canvas</li>
 * </ol>
 *
 * <p>El resultado es una lista de frames del mismo tamaño, con el contenido
 * centrado, lo que elimina la vibración al reproducir el GIF.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class FrameAligner {

    private static final Logger log = LoggerFactory.getLogger(FrameAligner.class);

    /**
     * Alinea una lista de frames centrándolos en un canvas común.
     *
     * @param frames frames a alinear (todos con canal alpha)
     * @return lista de frames alineados del mismo tamaño
     */
    public List<BufferedImage> align(List<BufferedImage> frames) {
        Objects.requireNonNull(frames, "frames no puede ser nulo");

        if (frames.isEmpty()) {
            return List.of();
        }

        log.info("Alineando {} frames", frames.size());

        // 1. Calcular bounding box de contenido no transparente de cada frame
        List<Rectangle> bounds = new ArrayList<>(frames.size());
        int maxContentWidth = 0;
        int maxContentHeight = 0;

        for (int i = 0; i < frames.size(); i++) {
            Rectangle bbox = computeContentBounds(frames.get(i));
            bounds.add(bbox);

            if (bbox != null) {
                maxContentWidth = Math.max(maxContentWidth, bbox.width);
                maxContentHeight = Math.max(maxContentHeight, bbox.height);
            }
        }

        if (maxContentWidth == 0 || maxContentHeight == 0) {
            log.warn("Todos los frames están vacíos (sin contenido no transparente)");
            return new ArrayList<>(frames);
        }

        // Añadimos un pequeño padding alrededor para no cortar los bordes
        int padding = 20;
        int canvasWidth = maxContentWidth + padding * 2;
        int canvasHeight = maxContentHeight + padding * 2;

        log.debug("Canvas común: {}×{} (padding: {})",
            canvasWidth, canvasHeight, padding);

        // 2. Crear los frames alineados
        List<BufferedImage> aligned = new ArrayList<>(frames.size());

        for (int i = 0; i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);
            Rectangle bbox = bounds.get(i);

            BufferedImage alignedFrame = createAlignedFrame(frame, bbox,
                canvasWidth, canvasHeight, padding);
            aligned.add(alignedFrame);
        }

        log.info("Alineación completa: {} frames de {}×{}",
            aligned.size(), canvasWidth, canvasHeight);

        return aligned;
    }

    /**
     * Calcula el bounding box del contenido no transparente.
     * Devuelve null si el frame está completamente transparente.
     */
    private Rectangle computeContentBounds(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        int minX = width;
        int minY = height;
        int maxX = -1;
        int maxY = -1;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int alpha = (frame.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 10) {  // ignorar píxeles casi transparentes
                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }

        if (maxX < 0 || maxY < 0) {
            return null;  // frame completamente transparente
        }

        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    /**
     * Crea un frame alineado: dibuja el contenido original centrado
     * en un canvas del tamaño indicado.
     */
    private BufferedImage createAlignedFrame(BufferedImage original, Rectangle bbox,
                                             int canvasWidth, int canvasHeight,
                                             int padding) {
        BufferedImage aligned = new BufferedImage(canvasWidth, canvasHeight,
            BufferedImage.TYPE_INT_ARGB);

        if (bbox == null) {
            // Frame vacío: dejar canvas transparente
            return aligned;
        }

        // Calcular posición para centrar el contenido en el canvas
        int offsetX = (canvasWidth - bbox.width) / 2;
        int offsetY = (canvasHeight - bbox.height) / 2;

        // Copiar el contenido de la región del bbox al canvas centrado
        for (int y = 0; y < bbox.height; y++) {
            for (int x = 0; x < bbox.width; x++) {
                int srcX = bbox.x + x;
                int srcY = bbox.y + y;
                int rgb = original.getRGB(srcX, srcY);

                int dstX = offsetX + x;
                int dstY = offsetY + y;
                aligned.setRGB(dstX, dstY, rgb);
            }
        }

        return aligned;
    }
}
