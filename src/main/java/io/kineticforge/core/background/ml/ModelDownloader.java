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
 * <p>El modelo pesa ~170 MB y se descarga desde GitHub Releases
 * (repositorio oficial de rembg). Una vez descargado, se guarda en
 * {@code ~/kineticforge/models/u2net.onnx} y se reutiliza offline.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class ModelDownloader {

    private static final Logger log = LoggerFactory.getLogger(ModelDownloader.class);

    /** URL del modelo en GitHub Releases. */
    private static final String MODEL_URL =
            "https://github.com/danielgatis/rembg/releases/download/v0.0.0/u2netp.onnx";

    /** Nombre del archivo del modelo. */
    private static final String MODEL_FILENAME = "u2netp.onnx";

    /** Tamaño esperado del modelo (~170 MB). */
    private static final long EXPECTED_SIZE = 5_000_000L;

    /**
     * Asegura que el modelo esté descargado. Si no existe, lo descarga.
     *
     * @param progressCallback callback opcional para reportar progreso (0.0 a 1.0)
     * @return ruta al archivo del modelo
     * @throws IOException si falla la descarga
     */
    public static Path ensureModel(DoubleConsumer progressCallback) throws IOException {
        Path modelPath = getModelPath();

        if (Files.exists(modelPath) && Files.size(modelPath) > 1_000_000) {
            log.info("Modelo U2-Net ya descargado: {} ({} MB)",
                    modelPath, Files.size(modelPath) / (1024 * 1024));
            return modelPath;
        }

        log.info("Descargando modelo U2-Net (~170 MB) desde: {}", MODEL_URL);
        download(MODEL_URL, modelPath, progressCallback);
        log.info("Modelo descargado: {}", modelPath);

        return modelPath;
    }

    /**
     * Asegura que el modelo esté descargado (sin callback de progreso).
     */
    public static Path ensureModel() throws IOException {
        return ensureModel(null);
    }

    /**
     * @return ruta donde se guarda el modelo.
     */
    public static Path getModelPath() {
        return Path.of(System.getProperty("user.home"),
                "kineticforge", "models", MODEL_FILENAME);
    }

    /**
     * @return true si el modelo ya está descargado.
     */
    public static boolean isModelDownloaded() {
        Path path = getModelPath();
        return Files.exists(path) && path.toFile().length() > 1_000_000;
    }

    // ============================================================
    // Descarga
    // ============================================================

    private static void download(String url, Path destination, DoubleConsumer progress)
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
                    .orElse(EXPECTED_SIZE);

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
