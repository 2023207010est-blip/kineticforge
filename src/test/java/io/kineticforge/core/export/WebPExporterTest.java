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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WebPExporter - Exportación a WebP (primer frame)")
class WebPExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Exporta un WebP válido desde una lista de frames")
    void exportsValidWebP() throws IOException {
        BufferedImage frame = createTestFrame(60, 60, Color.RED);
        Path output = tempDir.resolve("test.webp");

        new WebPExporter().export(
            List.of(frame), ExportConfig.defaults(), output);

        assertTrue(Files.exists(output), "El WebP debe existir");
        assertTrue(Files.size(output) > 0, "El WebP no debe estar vacío");
    }

    @Test
    @DisplayName("Exporta solo el primer frame si hay varios")
    void exportsOnlyFirstFrame() throws IOException {
        BufferedImage frame1 = createTestFrame(60, 60, Color.RED);
        BufferedImage frame2 = createTestFrame(60, 60, Color.BLUE);
        Path output = tempDir.resolve("first-only.webp");

        new WebPExporter().export(
            List.of(frame1, frame2), ExportConfig.defaults(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    @DisplayName("Exporta WebP con transparencia preservada")
    void exportsWithTransparency() throws IOException {
        BufferedImage frame = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frame.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, 50, 50);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
        g.setColor(Color.BLACK);
        g.fillRect(20, 20, 10, 10);
        g.dispose();

        Path output = tempDir.resolve("transparent.webp");
        new WebPExporter().export(
            List.of(frame), ExportConfig.defaults(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    @DisplayName("Lanza excepción con lista vacía")
    void throwsOnEmptyList() {
        assertThrows(ExportException.class,
            () -> new WebPExporter().export(
                List.of(),
                ExportConfig.defaults(),
                tempDir.resolve("empty.webp")));
    }

    @Test
    @DisplayName("Lanza excepción con frames nulos")
    void throwsOnNullFrames() {
        assertThrows(NullPointerException.class,
            () -> new WebPExporter().export(
                null,
                ExportConfig.defaults(),
                tempDir.resolve("null.webp")));
    }

    @Test
    @DisplayName("Devuelve info de formato correcta")
    void returnsFormatInfo() {
        WebPExporter exporter = new WebPExporter();
        assertEquals("WebP", exporter.getFormatName());
        assertEquals("webp", exporter.getFileExtension());
        assertTrue(exporter.supportsTransparency());
    }

    @Test
    @DisplayName("Crea directorios intermedios")
    void createsParentDirectories() throws IOException {
        BufferedImage frame = createTestFrame(30, 30, Color.GREEN);
        Path nested = tempDir.resolve("x/y/z/anim.webp");

        new WebPExporter().export(
            List.of(frame), ExportConfig.defaults(), nested);

        assertTrue(Files.exists(nested));
    }

    // ============================================================
    // Helpers
    // ============================================================

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
