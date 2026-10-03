package io.kineticforge.core.export;

import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("SpritesheetExporter - Exportación a PNG spritesheet")
class SpritesheetExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Exporta un spritesheet válido con 9 frames")
    void exportsValidSpritesheet() throws IOException {
        List<BufferedImage> frames = createTestFrames(9, 50, 50);
        Path output = tempDir.resolve("sheet.png");

        new SpritesheetExporter().export(frames, ExportConfig.defaults(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);

        BufferedImage sheet = ImageIO.read(output.toFile());
        // 9 frames → 3x3 grid
        assertEquals(150, sheet.getWidth(), "Ancho debe ser 3 * 50");
        assertEquals(150, sheet.getHeight(), "Alto debe ser 3 * 50");
    }

    @Test
    @DisplayName("Exporta spritesheet con 1 frame")
    void exportsSingleFrame() throws IOException {
        List<BufferedImage> frames = createTestFrames(1, 40, 40);
        Path output = tempDir.resolve("single.png");

        new SpritesheetExporter().export(frames, ExportConfig.defaults(), output);

        BufferedImage sheet = ImageIO.read(output.toFile());
        assertEquals(40, sheet.getWidth());
        assertEquals(40, sheet.getHeight());
    }

    @Test
    @DisplayName("Exporta spritesheet con 4 frames (2x2)")
    void exportsFourFrames() throws IOException {
        List<BufferedImage> frames = createTestFrames(4, 30, 30);
        Path output = tempDir.resolve("four.png");

        new SpritesheetExporter().export(frames, ExportConfig.defaults(), output);

        BufferedImage sheet = ImageIO.read(output.toFile());
        assertEquals(60, sheet.getWidth());
        assertEquals(60, sheet.getHeight());
    }

    @Test
    @DisplayName("Exporta spritesheet con 48 frames (7x7)")
    void exports48Frames() throws IOException {
        List<BufferedImage> frames = createTestFrames(48, 32, 32);
        Path output = tempDir.resolve("48.png");

        new SpritesheetExporter().export(frames, ExportConfig.defaults(), output);

        BufferedImage sheet = ImageIO.read(output.toFile());
        // ceil(sqrt(48)) = 7 columnas, ceil(48/7) = 7 filas
        assertEquals(7 * 32, sheet.getWidth(), "Ancho debe ser 7 * 32");
        assertEquals(7 * 32, sheet.getHeight(), "Alto debe ser 7 * 32");
    }

    @Test
    @DisplayName("Lanza excepción con lista vacía")
    void throwsOnEmptyList() {
        assertThrows(ExportException.class,
            () -> new SpritesheetExporter().export(
                List.of(),
                ExportConfig.defaults(),
                tempDir.resolve("empty.png")));
    }

    @Test
    @DisplayName("Lanza excepción con frames de tamaños distintos")
    void throwsOnMismatchedSizes() {
        List<BufferedImage> frames = List.of(
            createTestFrame(50, 50, Color.RED),
            createTestFrame(30, 30, Color.BLUE)
        );

        assertThrows(ExportException.class,
            () -> new SpritesheetExporter().export(
                frames,
                ExportConfig.defaults(),
                tempDir.resolve("bad.png")));
    }

    @Test
    @DisplayName("Lanza excepción con frames nulos")
    void throwsOnNullFrames() {
        assertThrows(NullPointerException.class,
            () -> new SpritesheetExporter().export(
                null,
                ExportConfig.defaults(),
                tempDir.resolve("null.png")));
    }

    @Test
    @DisplayName("Devuelve info de formato correcta")
    void returnsFormatInfo() {
        SpritesheetExporter exporter = new SpritesheetExporter();
        assertEquals("PNG Spritesheet", exporter.getFormatName());
        assertEquals("png", exporter.getFileExtension());
        assertTrue(exporter.supportsTransparency());
    }

    @Test
    @DisplayName("Crea directorios intermedios")
    void createsParentDirectories() throws IOException {
        List<BufferedImage> frames = createTestFrames(4, 20, 20);
        Path nested = tempDir.resolve("a/b/c/sheet.png");

        new SpritesheetExporter().export(frames, ExportConfig.defaults(), nested);

        assertTrue(Files.exists(nested));
    }

    // ============================================================
    // Helpers
    // ============================================================

    private List<BufferedImage> createTestFrames(int count, int width, int height) {
        List<BufferedImage> frames = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Color color = Color.getHSBColor(i / (float) count, 0.8f, 0.8f);
            frames.add(createTestFrame(width, height, color));
        }
        return frames;
    }

    private BufferedImage createTestFrame(int width, int height, Color color) {
        BufferedImage frame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frame.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return frame;
    }
}
