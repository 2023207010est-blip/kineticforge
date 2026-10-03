package io.kineticforge.core.export;

import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Exportador de animaciones usando FFmpeg.
 *
 * <p>Soporta múltiples formatos de salida de alta calidad:
 * MP4 (H.264), WebM (VP9), GIF, APNG.</p>
 *
 * <p>Requiere FFmpeg instalado en el sistema (comando {@code ffmpeg} disponible en el PATH).</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class FFmpegExporter implements Exporter {

    private static final Logger log = LoggerFactory.getLogger(FFmpegExporter.class);

    /** Formato de salida. */
    public enum Format {
        MP4("MP4 (H.264)", "mp4"),
        WEBM("WebM (VP9)", "webm"),
        GIF("GIF (FFmpeg)", "gif");

        private final String displayName;
        private final String extension;

        Format(String displayName, String extension) {
            this.displayName = displayName;
            this.extension = extension;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getExtension() {
            return extension;
        }
    }

    private final Format format;

    public FFmpegExporter(Format format) {
        this.format = Objects.requireNonNull(format, "format no puede ser nulo");
    }

    @Override
    public String getFormatName() {
        return format.getDisplayName();
    }

    @Override
    public String getFileExtension() {
        return format.getExtension();
    }

    @Override
    public boolean supportsTransparency() {
        return format == Format.WEBM || format == Format.GIF;
    }

    @Override
    public void export(List<BufferedImage> frames, ExportConfig config, Path output) {
        Objects.requireNonNull(frames, "frames no puede ser nulo");
        Objects.requireNonNull(config, "config no puede ser nulo");
        Objects.requireNonNull(output, "output no puede ser nulo");

        if (frames.isEmpty()) {
            throw new ExportException("No hay frames para exportar");
        }

        // Verificar FFmpeg disponible
        if (!isFFmpegAvailable()) {
            throw new ExportException(
                    "FFmpeg no está instalado. Instalalo con: sudo apt install ffmpeg");
        }

        log.info("Exportando {} con FFmpeg: {} frames, {} fps",
                format.getDisplayName(), frames.size(), config.fps());

        // Crear directorio temporal para los frames PNG
        Path tempDir;
        try {
            tempDir = Files.createTempDirectory("kineticforge-frames-");
        } catch (IOException e) {
            throw new ExportException("No se pudo crear directorio temporal", e);
        }

        try {
            // Guardar frames como PNG numerados
            writeFramesAsPng(frames, tempDir);

            // Ejecutar FFmpeg
            runFFmpeg(tempDir, output, config);

            log.info("Exportación completa: {}", output);

        } finally {
            // Limpiar carpeta temporal
            cleanUp(tempDir);
        }
    }

    /**
     * Escribe cada frame como PNG numerado (frame_0000.png, frame_0001.png...).
     */
    private void writeFramesAsPng(List<BufferedImage> frames, Path tempDir) {
        try {
            for (int i = 0; i < frames.size(); i++) {
                Path framePath = tempDir.resolve(String.format("frame_%04d.png", i));
                ImageIO.write(frames.get(i), "png", framePath.toFile());
            }
            log.debug("{} frames escritos como PNG en {}", frames.size(), tempDir);

        } catch (IOException e) {
            throw new ExportException("No se pudieron escribir los frames temporales", e);
        }
    }

    /**
     * Ejecuta FFmpeg con los parámetros adecuados según el formato.
     */
    private void runFFmpeg(Path tempDir, Path output, ExportConfig config) {
        try {
            // Crear directorio padre si no existe
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            ProcessBuilder pb = new ProcessBuilder();
            pb.command(buildCommand(tempDir, output, config));
            pb.redirectErrorStream(true);

            log.debug("Comando FFmpeg: {}", String.join(" ", pb.command()));

            Process process = pb.start();

            // Leer output de FFmpeg
            try (var reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.trace("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ExportException("FFmpeg terminó con código de error: " + exitCode);
            }

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExportException("Error ejecutando FFmpeg", e);
        }
    }

    /**
     * Construye el comando de FFmpeg según el formato de salida.
     */
    private List<String> buildCommand(Path tempDir, Path output, ExportConfig config) {
        Path inputPattern = tempDir.resolve("frame_%04d.png");

        return switch (format) {
            case MP4 -> List.of(
                    "ffmpeg", "-y",
                    "-framerate", String.valueOf(config.fps()),
                    "-i", inputPattern.toString(),
                    "-c:v", "libx264",
                    "-pix_fmt", "yuv420p",
                    "-crf", "18",              // Calidad alta
                    "-preset", "slow",         // Mejor compresión
                    "-movflags", "+faststart", // Streaming optimizado
                    output.toString()
            );

            case WEBM -> List.of(
                    "ffmpeg", "-y",
                    "-framerate", String.valueOf(config.fps()),
                    "-i", inputPattern.toString(),
                    "-c:v", "libvpx-vp9",
                    "-pix_fmt", "yuva420p",    // Con alpha
                    "-crf", "30",
                    "-b:v", "0",
                    "-auto-alt-ref", "0",      // Compatibilidad con alpha
                    output.toString()
            );

            case GIF -> List.of(
                    "ffmpeg", "-y",
                    "-framerate", String.valueOf(config.fps()),
                    "-i", inputPattern.toString(),
                    "-vf", "split[s0][s1];[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=sierra2_4a",
                    "-loop", config.loop() ? "0" : "-1",
                    output.toString()
            );
        };
    }

    /**
     * Verifica si FFmpeg está disponible en el sistema.
     */
    private boolean isFFmpegAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder("ffmpeg", "-version");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            p.waitFor();
            return p.exitValue() == 0;
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    /**
     * Elimina recursivamente el directorio temporal.
     */
    private void cleanUp(Path tempDir) {
        try (var stream = Files.walk(tempDir)) {
            stream.sorted(java.util.Comparator.reverseOrder())
                    .forEach(p -> {
                        try { Files.deleteIfExists(p); }
                        catch (IOException ignored) { }
                    });
        } catch (IOException e) {
            log.warn("No se pudo limpiar el directorio temporal: {}", tempDir);
        }
    }
}
