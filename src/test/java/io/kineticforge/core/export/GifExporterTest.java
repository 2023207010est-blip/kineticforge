package io.kineticforge.core.export;

import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GifExporter - Exportación a GIF animado")
class GifExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Exporta un GIF válido con 3 frames")
    void exportsValidGifWithThreeFrames() throws IOException {
        List<BufferedImage> frames = createTestFrames(3, 100, 100);
        Path output = tempDir.resolve("test.gif");

        new GifExporter().export(frames, ExportConfig.defaults(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);

        try (ImageInputStream iis = ImageIO.createImageInputStream(output.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            assertTrue(readers.hasNext());
            ImageReader reader = readers.next();
            reader.setInput(iis);
            assertEquals("gif", reader.getFormatName().toLowerCase());
        }
    }

    @Test
    @DisplayName("El GIF tiene la cantidad correcta de frames")
    void gifHasCorrectFrameCount() throws IOException {
        List<BufferedImage> frames = createTestFrames(5, 80, 80);
        Path output = tempDir.resolve("5-frames.gif");

        new GifExporter().export(frames, ExportConfig.defaults(), output);

        try (ImageInputStream iis = ImageIO.createImageInputStream(output.toFile())) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            ImageReader reader = readers.next();
            reader.setInput(iis);
            assertEquals(5, reader.getNumImages(true));
        }
    }

    @Test
    @DisplayName("Exporta GIF con loop infinito")
    void exportsGifWithInfiniteLoop() throws IOException {
        List<BufferedImage> frames = createTestFrames(3, 100, 100);
        Path output = tempDir.resolve("loop.gif");

        ExportConfig config = ExportConfig.defaults().withLoop(true);
        new GifExporter().export(frames, config, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    @DisplayName("Lanza excepción con lista vacía")
    void throwsOnEmptyList() {
        assertThrows(ExportException.class,
            () -> new GifExporter().export(
                List.of(),
                ExportConfig.defaults(),
                tempDir.resolve("empty.gif")));
    }

    @Test
    @DisplayName("Lanza excepción con frames de tamaños diferentes")
    void throwsOnMismatchedFrameSizes() {
        List<BufferedImage> frames = List.of(
            createTestFrame(100, 100, Color.RED),
            createTestFrame(50, 50, Color.BLUE)
        );

        assertThrows(ExportException.class,
            () -> new GifExporter().export(
                frames,
                ExportConfig.defaults(),
                tempDir.resolve("mismatched.gif")));
    }

    @Test
    @DisplayName("Devuelve el nombre de formato y extensión correctos")
    void returnsCorrectFormatInfo() {
        GifExporter exporter = new GifExporter();
        assertEquals("GIF", exporter.getFormatName());
        assertEquals("gif", exporter.getFileExtension());
        assertTrue(exporter.supportsTransparency());
    }

    @Test
    @DisplayName("Crea carpetas intermedias si no existen")
    void createsParentDirectories() throws IOException {
        List<BufferedImage> frames = createTestFrames(2, 50, 50);
        Path nested = tempDir.resolve("sub/dir/nested.gif");

        new GifExporter().export(frames, ExportConfig.defaults(), nested);

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
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);
        g.setColor(color);
        g.fillRect(width / 4, height / 4, width / 2, height / 2);
        g.dispose();
        return frame;
    }
}
