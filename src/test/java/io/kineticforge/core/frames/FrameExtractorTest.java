package io.kineticforge.core.frames;

import io.kineticforge.exception.ImageProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("FrameExtractor - Extracción de frames individuales")
class FrameExtractorTest {

    @Test
    @DisplayName("Extrae 48 frames de una grilla 8x6")
    void extracts48FramesFrom8x6Grid() {
        BufferedImage image = createTestImage(800, 600);
        List<Rectangle> cells = createGridCells(8, 6, 800, 600);

        List<BufferedImage> frames = new FrameExtractor().extract(image, cells);

        assertNotNull(frames);
        assertEquals(48, frames.size());
    }

    @Test
    @DisplayName("Extrae 12 frames de una grilla 4x3")
    void extracts12FramesFrom4x3Grid() {
        BufferedImage image = createTestImage(800, 600);
        List<Rectangle> cells = createGridCells(4, 3, 800, 600);

        List<BufferedImage> frames = new FrameExtractor().extract(image, cells);

        assertEquals(12, frames.size());
    }

    @Test
    @DisplayName("Los frames tienen las dimensiones del rectángulo")
    void framesHaveRectangleDimensions() {
        BufferedImage image = createTestImage(800, 600);
        List<Rectangle> cells = createGridCells(8, 6, 800, 600);

        List<BufferedImage> frames = new FrameExtractor().extract(image, cells);

        for (int i = 0; i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);
            Rectangle cell = cells.get(i);

            assertEquals(cell.width, frame.getWidth());
            assertEquals(cell.height, frame.getHeight());
        }
    }

    @Test
    @DisplayName("Los frames preservan los colores originales")
    void framesPreserveOriginalColors() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.RED);
        g.fillRect(10, 10, 20, 20);
        g.dispose();

        List<Rectangle> cells = List.of(new Rectangle(0, 0, 50, 50));
        List<BufferedImage> frames = new FrameExtractor().extract(image, cells);

        int rgb = frames.get(0).getRGB(10, 10);
        int r = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        assertEquals(255, r);
        assertEquals(0, g2);
        assertEquals(0, b);
    }

    @Test
    @DisplayName("Lanza excepción con imagen nula")
    void throwsOnNullImage() {
        List<Rectangle> cells = createGridCells(8, 6, 800, 600);
        assertThrows(NullPointerException.class,
                () -> new FrameExtractor().extract(null, cells));
    }

    @Test
    @DisplayName("Lanza excepción con cells nulo")
    void throwsOnNullCells() {
        BufferedImage image = createTestImage(800, 600);
        assertThrows(NullPointerException.class,
                () -> new FrameExtractor().extract(image, null));
    }

    @Test
    @DisplayName("Lanza excepción si el rectángulo está fuera de límites")
    void throwsWhenRectangleOutsideBounds() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        List<Rectangle> badCells = List.of(new Rectangle(90, 90, 50, 50));

        assertThrows(ImageProcessingException.class,
                () -> new FrameExtractor().extract(image, badCells));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private BufferedImage createTestImage(int width, int height) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private List<Rectangle> createGridCells(int cols, int rows, int imgWidth, int imgHeight) {
        int cellW = imgWidth / cols;
        int cellH = imgHeight / rows;

        List<Rectangle> cells = new ArrayList<>(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells.add(new Rectangle(c * cellW, r * cellH, cellW, cellH));
            }
        }
        return cells;
    }
}
