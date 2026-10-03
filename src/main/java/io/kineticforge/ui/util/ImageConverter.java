package io.kineticforge.ui.util;

import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;

/**
 * Utilidades para convertir entre {@code BufferedImage} (AWT)
 * y {@code WritableImage} (JavaFX).
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class ImageConverter {

    private ImageConverter() {
    }

    /**
     * Convierte un {@link BufferedImage} a {@link WritableImage} de JavaFX.
     *
     * @param bufferedImage imagen AWT
     * @return imagen JavaFX
     */
    public static WritableImage toFxImage(BufferedImage bufferedImage) {
        if (bufferedImage == null) {
            return null;
        }

        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        WritableImage fxImage = new WritableImage(width, height);
        PixelWriter pixelWriter = fxImage.getPixelWriter();

        // Conversión en bloque (mucho más rápida que píxel a píxel)
        int[] pixels = bufferedImage.getRGB(0, 0, width, height, null, 0, width);
        pixelWriter.setPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);

        return fxImage;
    }

    /**
     * Convierte un {@link Image} de JavaFX a {@link BufferedImage} de AWT.
     *
     * @param fxImage imagen JavaFX
     * @return imagen AWT
     */
    public static BufferedImage toAwtImage(Image fxImage) {
        if (fxImage == null) {
            return null;
        }

        int width = (int) fxImage.getWidth();
        int height = (int) fxImage.getHeight();

        BufferedImage bufferedImage = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_ARGB);

        int[] pixels = new int[width * height];
        fxImage.getPixelReader().getPixels(0, 0, width, height,
                javafx.scene.image.PixelFormat.getIntArgbInstance(), pixels, 0, width);
        bufferedImage.setRGB(0, 0, width, height, pixels, 0, width);

        return bufferedImage;
    }

    /**
     * Reduce la imagen para que su lado mayor no supere {@code maxSide}
     * (promedio progresivo por mitades: no pierde trazos finos). Si ya cabe,
     * devuelve la misma imagen. Útil para vistas previas y miniaturas.
     */
    public static BufferedImage scaleToFit(BufferedImage src, int maxSide) {
        if (src == null) {
            return null;
        }
        int w = src.getWidth();
        int h = src.getHeight();
        int longest = Math.max(w, h);
        if (longest <= maxSide) {
            return src;
        }

        double scale = maxSide / (double) longest;
        int targetW = Math.max(1, (int) Math.round(w * scale));
        int targetH = Math.max(1, (int) Math.round(h * scale));

        BufferedImage current = src;
        int curW = w;
        int curH = h;
        while (curW / 2 >= targetW && curH / 2 >= targetH) {
            curW /= 2;
            curH /= 2;
            current = drawScaled(current, curW, curH);
        }
        if (curW != targetW || curH != targetH) {
            current = drawScaled(current, targetW, targetH);
        }
        return current;
    }

    private static BufferedImage drawScaled(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }
}
