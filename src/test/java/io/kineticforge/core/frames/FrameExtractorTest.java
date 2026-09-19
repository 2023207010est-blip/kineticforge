package io.kineticforge.core.frames;

import io.kineticforge.core.grid.GridDetectionResult;
import io.kineticforge.exception.ImageProcessingException;
import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("FrameExtractor - Extracción de frames individuales")
class FrameExtractorTest {

    @Test
    @DisplayName("Extrae 48 frames de una grilla 8x6")
    void extracts48FramesFrom8x6Grid() {
        GridSpec spec = GridSpec.defaults();
        BufferedImage image = createTestImage(842, 595);
        GridDetectionResult result = createFakeDetectionResult(spec, 8, 6, 842, 595);

        List<BufferedImage> frames = new FrameExtractor().extract(image, result);

        assertNotNull(frames);
        assertEquals(48, frames.size());
    }

    @Test
    @DisplayName("Extrae 12 frames de una grilla 4x3")
    void extracts12FramesFrom4x3Grid() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.DETALLE)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .build();

        BufferedImage image = createTestImage(842, 595);
        GridDetectionResult result = createFakeDetectionResult(spec, 4, 3, 842, 595);

        List<BufferedImage> frames = new FrameExtractor().extract(image, result);

        assertEquals(12, frames.size());
    }

    @Test
    @DisplayName("Los frames tienen las dimensiones del rectángulo")
    void framesHaveRectangleDimensions() {
        GridSpec spec = GridSpec.defaults();
        BufferedImage image = createTestImage(842, 595);
        GridDetectionResult result = createFakeDetectionResult(spec, 8, 6, 842, 595);

        List<BufferedImage> frames = new FrameExtractor().extract(image, result);

        // Cada frame debe tener el tamaño del rectángulo correspondiente
        for (int i = 0; i < frames.size(); i++) {
            BufferedImage frame = frames.get(i);
            Rectangle cell = result.cellAt(i);

            assertEquals(cell.width, frame.getWidth(),
                "Frame " + i + " debe tener ancho " + cell.width);
            assertEquals(cell.height, frame.getHeight(),
                "Frame " + i + " debe tener alto " + cell.height);
        }
    }

    @Test
    @DisplayName("Los frames preservan los colores originales")
    void framesPreserveOriginalColors() {
        // Imagen con un pixel rojo en la esquina superior izquierda
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.RED);
        g.fillRect(10, 10, 20, 20);
        g.dispose();

        GridSpec spec = GridSpec.defaults();
        List<Rectangle> cells = List.of(new Rectangle(0, 0, 50, 50));
        GridDetectionResult result = new GridDetectionResult(
            spec, cells, 1.0, 100, 100);

        List<BufferedImage> frames = new FrameExtractor().extract(image, result);

        // El frame debe tener el pixel rojo en (10, 10)
        int rgb = frames.get(0).getRGB(10, 10);
        int r = (rgb >> 16) & 0xFF;
        int g2 = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        assertEquals(255, r, "Rojo debe ser 255");
        assertEquals(0, g2, "Verde debe ser 0");
        assertEquals(0, b, "Azul debe ser 0");
    }

    @Test
    @DisplayName("Lanza excepción con imagen nula")
    void throwsOnNullImage() {
        GridSpec spec = GridSpec.defaults();
        GridDetectionResult result = createFakeDetectionResult(spec, 8, 6, 842, 595);

        assertThrows(NullPointerException.class,
            () -> new FrameExtractor().extract(null, result));
    }

    @Test
    @DisplayName("Lanza excepción con result nulo")
    void throwsOnNullResult() {
        BufferedImage image = createTestImage(842, 595);
        assertThrows(NullPointerException.class,
            () -> new FrameExtractor().extract(image, null));
    }

    @Test
    @DisplayName("Lanza excepción si el rectángulo está fuera de límites")
    void throwsWhenRectangleOutsideBounds() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        GridSpec spec = GridSpec.defaults();

        // Rectángulo que se sale de la imagen
        List<Rectangle> badCells = List.of(new Rectangle(90, 90, 50, 50));
        GridDetectionResult result = new GridDetectionResult(
            spec, badCells, 1.0, 100, 100);

        assertThrows(ImageProcessingException.class,
            () -> new FrameExtractor().extract(image, result));
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

    private GridDetectionResult createFakeDetectionResult(
        GridSpec spec, int cols, int rows, int imgWidth, int imgHeight) {

        int cellW = imgWidth / cols;
        int cellH = imgHeight / rows;

        List<Rectangle> cells = new ArrayList<>(cols * rows);
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cells.add(new Rectangle(c * cellW, r * cellH, cellW, cellH));
            }
        }

        return new GridDetectionResult(spec, cells, 1.0, imgWidth, imgHeight);
    }
}
