package io.kineticforge.core.background;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("Calidad de bordes - sin aura blanca")
class EdgeQualityTest {

    /** Escaneo realista: papel gris claro (no 255) y dibujo con antialiasing. */
    private BufferedImage scan() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(new Color(232, 230, 225));
        g.fillRect(0, 0, 200, 200);
        g.setColor(new Color(200, 30, 30));
        g.fillOval(50, 50, 100, 100);
        g.dispose();
        return img;
    }

    private int lum(int argb) {
        return (int) (0.299 * ((argb >> 16) & 255) + 0.587 * ((argb >> 8) & 255) + 0.114 * (argb & 255));
    }

    @Test
    @DisplayName("Papel gris claro (no blanco puro) se elimina por completo")
    void offWhitePaperIsRemoved() {
        BufferedImage r = new BackgroundRemover().removeBackground(scan());
        assertEquals(0, r.getRGB(5, 5) >>> 24);
        assertEquals(0, r.getRGB(195, 100) >>> 24);
        assertEquals(255, r.getRGB(100, 100) >>> 24);
    }

    @Test
    @DisplayName("Ningún píxel visible del borde es casi blanco")
    void noWhiteHaloAtEdges() {
        BufferedImage r = new BackgroundRemover().removeBackground(scan());
        int halo = 0;
        for (int y = 1; y < 199; y++) {
            for (int x = 1; x < 199; x++) {
                int c = r.getRGB(x, y);
                if ((c >>> 24) < 64 || lum(c) < 170) continue;
                boolean nearBg = false;
                for (int dy = -2; dy <= 2 && !nearBg; dy++) {
                    for (int dx = -2; dx <= 2; dx++) {
                        if ((r.getRGB(x + dx, y + dy) >>> 24) < 8) { nearBg = true; break; }
                    }
                }
                if (nearBg) halo++;
            }
        }
        assertEquals(0, halo, "No debe haber halo claro junto al fondo");
    }

    @Test
    @DisplayName("El blanco encerrado por el dibujo se conserva")
    void enclosedWhiteIsKept() {
        BufferedImage img = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.BLACK);
        g.fillOval(40, 40, 120, 120);
        g.setColor(Color.WHITE);
        g.fillOval(75, 75, 50, 50);
        g.dispose();

        BufferedImage r = new BackgroundRemover().removeBackground(img);
        assertEquals(255, r.getRGB(100, 100) >>> 24, "El 'ojo' blanco debe seguir opaco");
        assertEquals(0, r.getRGB(5, 5) >>> 24);
    }

    @Test
    @DisplayName("PriorGuidedRemover usa la máscara y no deja aura")
    void priorGuidedRemoverWorks() {
        float[][] prior = new float[200][200];
        for (int y = 0; y < 200; y++) {
            for (int x = 0; x < 200; x++) {
                prior[y][x] = Math.hypot(x - 100, y - 100) < 58 ? 1f : 0f;
            }
        }
        BufferedImage r = new PriorGuidedRemover().removeBackground(scan(), prior);
        assertTrue(r != null);
        assertEquals(0, r.getRGB(5, 5) >>> 24);
        assertEquals(255, r.getRGB(100, 100) >>> 24);
    }

    @Test
    @DisplayName("PriorGuidedRemover devuelve null si la máscara es inútil")
    void priorGuidedRemoverRejectsUselessMask() {
        float[][] empty = new float[200][200];
        assertEquals(null, new PriorGuidedRemover().removeBackground(scan(), empty));
    }
}
