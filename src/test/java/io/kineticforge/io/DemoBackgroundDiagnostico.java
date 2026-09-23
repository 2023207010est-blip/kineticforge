package io.kineticforge.io;

import io.kineticforge.core.background.BackgroundRemover;
import io.kineticforge.io.ImageLoader;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Herramienta de diagnóstico del BackgroundRemover.
 */
public class DemoBackgroundDiagnostico {

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Uso: DemoBackgroundDiagnostico <ruta-imagen>");
            return;
        }

        Path inputPath = Path.of(args[0]);
        if (!Files.exists(inputPath)) {
            System.out.println("No existe: " + inputPath);
            return;
        }

        Path outputDir = Path.of(System.getProperty("user.home"),
            "kineticforge", "diagnostico");
        Files.createDirectories(outputDir);

        System.out.println("Cargando: " + inputPath);
        BufferedImage original = new ImageLoader().load(inputPath);
        System.out.println("Tamaño: " + original.getWidth() + "x" + original.getHeight());

        // Analizar estadísticas de luminancia
        System.out.println("\n=== Estadísticas de luminancia ===");
        int[] histogram = new int[256];
        for (int y = 0; y < original.getHeight(); y++) {
            for (int x = 0; x < original.getWidth(); x++) {
                int rgb = original.getRGB(x, y);
                int lum = (int) (0.299 * ((rgb >> 16) & 0xFF)
                    + 0.587 * ((rgb >> 8) & 0xFF)
                    + 0.114 * (rgb & 0xFF));
                histogram[lum]++;
            }
        }

        int total = original.getWidth() * original.getHeight();
        System.out.println("Píxeles < 200 (oscuros): " + countRange(histogram, 0, 199)
            + " (" + (100.0 * countRange(histogram, 0, 199) / total) + "%)");
        System.out.println("Píxeles 200-240 (medios): " + countRange(histogram, 200, 240)
            + " (" + (100.0 * countRange(histogram, 200, 240) / total) + "%)");
        System.out.println("Píxeles 240-250 (claros): " + countRange(histogram, 240, 250)
            + " (" + (100.0 * countRange(histogram, 240, 250) / total) + "%)");
        System.out.println("Píxeles 250-255 (casi blanco): " + countRange(histogram, 250, 255)
            + " (" + (100.0 * countRange(histogram, 250, 255) / total) + "%)");

        // Guardar original
        Path originalOut = outputDir.resolve("01-original.png");
        ImageIO.write(original, "png", originalOut.toFile());
        System.out.println("\nGuardado: " + originalOut);

        // Aplicar BackgroundRemover
        System.out.println("\n=== Aplicando BackgroundRemover ===");
        BackgroundRemover remover = new BackgroundRemover();
        BufferedImage result = remover.removeBackground(original);

        Path resultOut = outputDir.resolve("02-resultado.png");
        ImageIO.write(result, "png", resultOut.toFile());
        System.out.println("Guardado: " + resultOut);

        // Guardar preview con fondo verde para ver transparencia
        BufferedImage preview = new BufferedImage(
            result.getWidth(), result.getHeight(), BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < result.getHeight(); y++) {
            for (int x = 0; x < result.getWidth(); x++) {
                int argb = result.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;
                if (alpha < 128) {
                    // Transparente → verde
                    preview.setRGB(x, y, 0x00FF00);
                } else {
                    preview.setRGB(x, y, argb & 0x00FFFFFF);
                }
            }
        }

        Path previewOut = outputDir.resolve("03-preview-verde.png");
        ImageIO.write(preview, "png", previewOut.toFile());
        System.out.println("Guardado: " + previewOut);

        System.out.println("\n✅ Diagnóstico completo. Revisá: " + outputDir);
    }

    private static int countRange(int[] histogram, int min, int max) {
        int sum = 0;
        for (int i = min; i <= max; i++) sum += histogram[i];
        return sum;
    }
}
