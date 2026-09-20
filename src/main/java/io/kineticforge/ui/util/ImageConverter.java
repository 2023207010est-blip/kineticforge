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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                pixelWriter.setArgb(x, y, bufferedImage.getRGB(x, y));
            }
        }

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

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                bufferedImage.setRGB(x, y, fxImage.getPixelReader().getArgb(x, y));
            }
        }

        return bufferedImage;
    }
}
