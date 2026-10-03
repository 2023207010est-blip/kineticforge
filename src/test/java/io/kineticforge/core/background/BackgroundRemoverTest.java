package io.kineticforge.core.background;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("BackgroundRemover - Eliminación de fondo blanco")
class BackgroundRemoverTest {

    private static final int ALPHA_OPAQUE = 255;
    private static final int ALPHA_TRANSPARENT = 0;

    @Test
    @DisplayName("Un frame completamente blanco queda completamente transparente")
    void allWhiteFrameBecomesTransparent() {
        BufferedImage frame = createSolidImage(100, 100, Color.WHITE);
        BufferedImage result = new BackgroundRemover().removeBackground(frame);

        // Todos los píxeles deben ser transparentes
        for (int y = 0; y < 100; y++) {
            for (int x = 0; x < 100; x++) {
                int alpha = getAlpha(result.getRGB(x, y));
                assertEquals(ALPHA_TRANSPARENT, alpha,
                    "Píxel (" + x + "," + y + ") debe ser transparente");
            }
        }
    }

    @Test
    @DisplayName("Un círculo negro en el centro se preserva")
    void blackCircleInCenterIsPreserved() {
        BufferedImage frame = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);

        // Círculo negro en el centro
        g.setColor(Color.BLACK);
        g.fillOval(70, 70, 60, 60);
        g.dispose();

        BufferedImage result = new BackgroundRemover().removeBackground(frame);

        // El centro del círculo debe ser opaco
        int centerAlpha = getAlpha(result.getRGB(100, 100));
        assertEquals(ALPHA_OPAQUE, centerAlpha,
            "El centro del círculo debe ser opaco");

        // El fondo (esquina) debe ser transparente
        int cornerAlpha = getAlpha(result.getRGB(5, 5));
        assertEquals(ALPHA_TRANSPARENT, cornerAlpha,
            "El fondo debe ser transparente");
    }



    @Test
    @DisplayName("Fondo exterior conectado al borde se elimina")
    void outerBackgroundIsRemoved() {
        BufferedImage frame = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);

        g.setColor(Color.BLACK);
        g.fillRect(80, 80, 40, 40);
        g.dispose();

        BufferedImage result = new BackgroundRemover().removeBackground(frame);

        // Esquina (fondo conectado al borde) → transparente
        assertEquals(ALPHA_TRANSPARENT, getAlpha(result.getRGB(5, 5)),
            "Esquina superior izquierda debe ser transparente");

        // Borde superior (fondo conectado al borde) → transparente
        assertEquals(ALPHA_TRANSPARENT, getAlpha(result.getRGB(100, 5)),
            "Borde superior debe ser transparente");

        // Cuadrado negro → opaco
        assertEquals(ALPHA_OPAQUE, getAlpha(result.getRGB(100, 100)),
            "Cuadrado negro debe ser opaco");
    }

    @Test
    @DisplayName("La imagen resultado tiene el mismo tamaño")
    void resultHasSameSize() {
        BufferedImage frame = createSolidImage(300, 200, Color.WHITE);
        BufferedImage result = new BackgroundRemover().removeBackground(frame);

        assertEquals(300, result.getWidth());
        assertEquals(200, result.getHeight());
    }

    @Test
    @DisplayName("La imagen resultado tiene canal alpha (TYPE_INT_ARGB)")
    void resultHasAlphaChannel() {
        BufferedImage frame = createSolidImage(100, 100, Color.WHITE);
        BufferedImage result = new BackgroundRemover().removeBackground(frame);

        assertEquals(BufferedImage.TYPE_INT_ARGB, result.getType(),
            "El resultado debe tener canal alpha");
    }

    @Test
    @DisplayName("Frame con antialiasing produce bordes suaves")
    void antialiasingProducesSoftBorders() {
        BufferedImage frame = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();

        // Activar antialiasing para generar bordes suaves
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.BLACK);
        g.fillOval(50, 50, 100, 100);
        g.dispose();

        BufferedImage result = new BackgroundRemover().removeBackground(frame);

        // Contar píxeles con alpha intermedio (bordes suaves)
        int softBorderCount = 0;
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) {
                int alpha = getAlpha(result.getRGB(x, y));
                if (alpha > 20 && alpha < 235) {
                    softBorderCount++;
                }
            }
        }

        assertTrue(softBorderCount > 0,
            "Debe haber píxeles con alpha intermedio (bordes suaves)");
    }

    @Test
    @DisplayName("Lanza excepción con frame nulo")
    void throwsOnNullFrame() {
        assertThrows(NullPointerException.class,
            () -> new BackgroundRemover().removeBackground(null));
    }

    @Test
    @DisplayName("Constructor rechaza estrategia nula")
    void constructorRejectsNullStrategy() {
        assertThrows(NullPointerException.class,
            () -> new BackgroundRemover(null, true));
    }

    @Test
    @DisplayName("Estrategia BFS y DFS dan resultados similares")
    void bfsAndDfsGiveSimilarResults() {
        BufferedImage frame = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = frame.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 100, 100);
        g.setColor(Color.BLACK);
        g.fillRect(40, 40, 20, 20);
        g.dispose();

        BufferedImage resultBfs = new BackgroundRemover(
            FloodFillStrategy.BFS, false).removeBackground(frame);
        BufferedImage resultDfs = new BackgroundRemover(
            FloodFillStrategy.DFS, false).removeBackground(frame);

        // Ambos deben producir el mismo resultado en este caso
        int bfsCornerAlpha = getAlpha(resultBfs.getRGB(5, 5));
        int dfsCornerAlpha = getAlpha(resultDfs.getRGB(5, 5));

        assertEquals(bfsCornerAlpha, dfsCornerAlpha,
            "BFS y DFS deben coincidir en píxeles obvios");
    }

    // ============================================================
    // Helpers
    // ============================================================

    private BufferedImage createSolidImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, width, height);
        g.dispose();
        return image;
    }

    private int getAlpha(int argb) {
        return (argb >> 24) & 0xFF;
    }
}
