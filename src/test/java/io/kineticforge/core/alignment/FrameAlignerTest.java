package io.kineticforge.core.alignment;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FrameAligner - Alineación de frames")
class FrameAlignerTest {

    @Test
    @DisplayName("Alinea una lista vacía sin problemas")
    void alignsEmptyList() {
        List<BufferedImage> result = new FrameAligner().align(List.of());
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Alinea un solo frame")
    void alignsSingleFrame() {
        BufferedImage frame = createFrameWithContent(100, 100, 30, 30, 40, 40);

        List<BufferedImage> result = new FrameAligner().align(List.of(frame));

        assertEquals(1, result.size());
        assertNotNull(result.get(0));
    }

    @Test
    @DisplayName("Todos los frames alineados tienen el mismo tamaño")
    void allAlignedFramesHaveSameSize() {
        List<BufferedImage> frames = List.of(
            createFrameWithContent(200, 200, 50, 50, 30, 30),
            createFrameWithContent(200, 200, 60, 60, 40, 40),
            createFrameWithContent(200, 200, 70, 70, 50, 50)
        );

        List<BufferedImage> aligned = new FrameAligner().align(frames);

        assertEquals(3, aligned.size());

        int firstWidth = aligned.get(0).getWidth();
        int firstHeight = aligned.get(0).getHeight();

        for (BufferedImage frame : aligned) {
            assertEquals(firstWidth, frame.getWidth(),
                "Todos los frames deben tener el mismo ancho");
            assertEquals(firstHeight, frame.getHeight(),
                "Todos los frames deben tener el mismo alto");
        }
    }

    @Test
    @DisplayName("El contenido está centrado horizontalmente")
    void contentIsHorizontallyCentered() {
        // Cuadrado en diferentes posiciones
        List<BufferedImage> frames = List.of(
            createFrameWithContent(200, 200, 30, 30, 20, 20),
            createFrameWithContent(200, 200, 150, 150, 20, 20)
        );

        List<BufferedImage> aligned = new FrameAligner().align(frames);

        // Ambos frames alineados: el contenido debe estar en la misma posición
        int centerX0 = findContentCenterX(aligned.get(0));
        int centerX1 = findContentCenterX(aligned.get(1));

        assertEquals(centerX0, centerX1, 2,
            "El contenido debe estar centrado en la misma X");
    }

    @Test
    @DisplayName("El contenido está centrado verticalmente")
    void contentIsVerticallyCentered() {
        List<BufferedImage> frames = List.of(
            createFrameWithContent(200, 200, 30, 30, 20, 20),
            createFrameWithContent(200, 200, 150, 150, 20, 20)
        );

        List<BufferedImage> aligned = new FrameAligner().align(frames);

        int centerY0 = findContentCenterY(aligned.get(0));
        int centerY1 = findContentCenterY(aligned.get(1));

        assertEquals(centerY0, centerY1, 2,
            "El contenido debe estar centrado en la misma Y");
    }

    @Test
    @DisplayName("El canvas común tiene el tamaño del contenido más grande")
    void canvasMatchesLargestContent() {
        // El segundo frame tiene contenido más grande
        List<BufferedImage> frames = List.of(
            createFrameWithContent(200, 200, 50, 50, 20, 20),   // 20x20
            createFrameWithContent(200, 200, 50, 50, 60, 40)    // 60x40
        );

        List<BufferedImage> aligned = new FrameAligner().align(frames);

        // El canvas debe ser 60+padding x 40+padding (padding = 8)
        int width = aligned.get(0).getWidth();
        int height = aligned.get(0).getHeight();

        assertTrue(width >= 60, "Ancho debe ser al menos 60");
        assertTrue(height >= 40, "Alto debe ser al menos 40");
    }

    @Test
    @DisplayName("Frames completamente transparentes no rompen la alineación")
    void transparentFramesDoNotBreakAlignment() {
        BufferedImage transparent = new BufferedImage(100, 100, BufferedImage.TYPE_INT_ARGB);
        BufferedImage withContent = createFrameWithContent(100, 100, 30, 30, 20, 20);

        List<BufferedImage> aligned = new FrameAligner().align(
            List.of(transparent, withContent));

        assertEquals(2, aligned.size());
        assertEquals(aligned.get(0).getWidth(), aligned.get(1).getWidth());
    }

    @Test
    @DisplayName("Lanza excepción con lista nula")
    void throwsOnNullList() {
        assertThrows(NullPointerException.class,
            () -> new FrameAligner().align(null));
    }

    // ============================================================
    // Helpers
    // ============================================================

    /**
     * Crea un frame con un cuadrado negro en la posición indicada.
     */
    private BufferedImage createFrameWithContent(int width, int height,
                                                 int contentX, int contentY,
                                                 int contentW, int contentH) {
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frame.createGraphics();

        // Fondo transparente
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, width, height);
        g.setComposite(java.awt.AlphaComposite.SrcOver);

        // Contenido negro (opaco)
        g.setColor(Color.BLACK);
        g.fillRect(contentX, contentY, contentW, contentH);
        g.dispose();

        return frame;
    }

    private int findContentCenterX(BufferedImage frame) {
        int minX = frame.getWidth();
        int maxX = -1;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int alpha = (frame.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 10) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                }
            }
        }
        return (minX + maxX) / 2;
    }

    private int findContentCenterY(BufferedImage frame) {
        int minY = frame.getHeight();
        int maxY = -1;
        for (int y = 0; y < frame.getHeight(); y++) {
            for (int x = 0; x < frame.getWidth(); x++) {
                int alpha = (frame.getRGB(x, y) >> 24) & 0xFF;
                if (alpha > 10) {
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        return (minY + maxY) / 2;
    }
}
