package io.kineticforge.core.background.ml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.function.DoubleConsumer;

/**
 * Descarga el modelo U2-Net ONNX la primera vez que se necesita.
 *
 * <p>Se descarga desde GitHub Releases (repositorio oficial de rembg) y se
 * guarda en {@code ~/kineticforge/models/}. Después funciona offline.</p>
 *
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class ModelDownloader {

    private static final Logger log = LoggerFactory.getLogger(ModelDownloader.class);

    /** Variantes disponibles del modelo. */
    public enum Variant {
        /** U2-Net "portable": ~5 MB, rápido. */
        LIGHT("u2netp.onnx", "Ligero (5 MB, rápido)",
                "https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx",
                5_000_000L, 1_000_000L),
        /** U2-Net completo: ~176 MB, bordes más precisos. */
        FULL("u2net.onnx", "Preciso (176 MB)",
                "https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2net.onnx",
                176_000_000L, 100_000_000L);

        private final String fileName;
        private final String label;
        private final String url;
        private final long expectedSize;
        private final long minValidSize;

        Variant(String fileName, String label, String url, long expectedSize, long minValidSize) {
            this.fileName = fileName;
            this.label = label;
            this.url = url;
            this.expectedSize = expectedSize;
            this.minValidSize = minValidSize;
        }

        public String getLabel() { return label; }

        @Override
        public String toString() { return label; }
    }

    /**
     * Asegura que el modelo esté descargado. Si no existe, lo descarga.
     *
     * @param progressCallback callback opcional para reportar progreso (0.0 a 1.0)
     * @return ruta al archivo del modelo
     * @throws IOException si falla la descarga
     */
    public static Path ensureModel(Variant variant, DoubleConsumer progressCallback) throws IOException {
        Path modelPath = getModelPath(variant);

        if (isModelDownloaded(variant)) {
            log.info("Modelo ya descargado: {} ({} MB)",
                    modelPath, Files.size(modelPath) / (1024 * 1024));
            return modelPath;
        }

        log.info("Descargando modelo {} desde: {}", variant.fileName, variant.url);
        download(variant.url, modelPath, variant.expectedSize, progressCallback);
        log.info("Modelo descargado: {}", modelPath);

        return modelPath;
    }

    public static Path ensureModel(DoubleConsumer progressCallback) throws IOException {
        return ensureModel(Variant.LIGHT, progressCallback);
    }

    public static Path ensureModel() throws IOException {
        return ensureModel(Variant.LIGHT, null);
    }

    public static Path getModelPath(Variant variant) {
        return Path.of(System.getProperty("user.home"),
                "kineticforge", "models", variant.fileName);
    }

    public static Path getModelPath() {
        return getModelPath(Variant.LIGHT);
    }

    public static boolean isModelDownloaded(Variant variant) {
        Path path = getModelPath(variant);
        return Files.exists(path) && path.toFile().length() >= variant.minValidSize;
    }

    public static boolean isModelDownloaded() {
        return isModelDownloaded(Variant.LIGHT);
    }

    // ============================================================
    // Descarga
    // ============================================================

    private static void download(String url, Path destination, long expectedSize,
                                 DoubleConsumer progress)
            throws IOException {

        Path parent = destination.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        Path tempFile = destination.resolveSibling(destination.getFileName() + ".tmp");

        try {
            HttpClient client = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<InputStream> response;
            try {
                response = client.send(request,
                        HttpResponse.BodyHandlers.ofInputStream());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Descarga interrumpida", e);
            }

            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " al descargar: " + url);
            }

            long totalSize = response.headers()
                    .firstValueAsLong("Content-Length")
                    .orElse(expectedSize);

            try (InputStream in = response.body();
                 var out = Files.newOutputStream(tempFile)) {

                byte[] buffer = new byte[64 * 1024];
                long downloaded = 0;
                int bytesRead;
                long lastReport = 0;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                    downloaded += bytesRead;

                    if (progress != null && downloaded - lastReport > 2_000_000) {
                        progress.accept((double) downloaded / totalSize);
                        lastReport = downloaded;
                    }
                }

                if (progress != null) {
                    progress.accept(1.0);
                }
            }

            // Mover el archivo temporal al destino final
            Files.move(tempFile, destination, StandardCopyOption.REPLACE_EXISTING);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }
}
