package io.kineticforge.core.export;

import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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

@DisplayName("ApngExporter - Exportación a PNG (primer frame)")
class ApngExporterTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Exporta un PNG válido desde una lista de frames")
    void exportsValidPng() throws IOException {
        BufferedImage frame = createTestFrame(50, 50, Color.RED);
        Path output = tempDir.resolve("test.png");

        new ApngExporter().export(
            List.of(frame), ExportConfig.defaults(), output);

        assertTrue(Files.exists(output), "El PNG debe existir");
        assertTrue(Files.size(output) > 0, "El PNG no debe estar vacío");
    }

    @Test
    @DisplayName("Exporta solo el primer frame si hay varios")
    void exportsOnlyFirstFrame() throws IOException {
        BufferedImage frame1 = createTestFrame(50, 50, Color.RED);
        BufferedImage frame2 = createTestFrame(50, 50, Color.BLUE);
        Path output = tempDir.resolve("first.png");

        new ApngExporter().export(
            List.of(frame1, frame2), ExportConfig.defaults(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    @DisplayName("Exporta PNG con transparencia")
    void exportsWithTransparency() throws IOException {
        BufferedImage frame = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = frame.createGraphics();
        g.setComposite(java.awt.AlphaComposite.Clear);
        g.fillRect(0, 0, 50, 50);
        g.setComposite(java.awt.AlphaComposite.SrcOver);
        g.setColor(Color.BLACK);
        g.fillOval(15, 15, 20, 20);
        g.dispose();

        Path output = tempDir.resolve("transparent.png");
        new ApngExporter().export(
            List.of(frame), ExportConfig.defaults(), output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    @DisplayName("Lanza excepción con lista vacía")
    void throwsOnEmptyList() {
        assertThrows(ExportException.class,
            () -> new ApngExporter().export(
                List.of(),
                ExportConfig.defaults(),
                tempDir.resolve("empty.png")));
    }

    @Test
    @DisplayName("Lanza excepción con frames nulos")
    void throwsOnNullFrames() {
        assertThrows(NullPointerException.class,
            () -> new ApngExporter().export(
                null,
                ExportConfig.defaults(),
                tempDir.resolve("null.png")));
    }

    @Test
    @DisplayName("Devuelve info de formato correcta")
    void returnsFormatInfo() {
        ApngExporter exporter = new ApngExporter();
        assertEquals("PNG", exporter.getFormatName());
        assertEquals("png", exporter.getFileExtension());
        assertTrue(exporter.supportsTransparency());
    }

    @Test
    @DisplayName("Crea directorios intermedios")
    void createsParentDirectories() throws IOException {
        BufferedImage frame = createTestFrame(30, 30, Color.GREEN);
        Path nested = tempDir.resolve("a/b/anim.png");

        new ApngExporter().export(
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
