package io.kineticforge.core.export;

import io.kineticforge.model.ExportConfig;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * Interfaz común para todos los exportadores.
 *
 * <p>Cada implementación sabe cómo convertir una lista de frames
 * al formato específico (GIF, WebP, APNG, PNG spritesheet).</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public interface Exporter {

    /**
     * @return nombre legible del formato (ej: "GIF", "WebP").
     */
    String getFormatName();

    /**
     * @return extensión del archivo sin punto (ej: "gif", "webp").
     */
    String getFileExtension();

    /**
     * @return {@code true} si el formato soporta transparencia.
     */
    boolean supportsTransparency();

    /**
     * Exporta los frames al formato indicado.
     *
     * @param frames frames a exportar (todos del mismo tamaño)
     * @param config configuración de exportación (fps, loop, escala)
     * @param output ruta donde guardar el archivo
     */
    void export(List<BufferedImage> frames, ExportConfig config, Path output);
}
