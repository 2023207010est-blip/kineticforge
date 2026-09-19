package io.kineticforge.io;

import io.kineticforge.model.GridSpec;

import java.nio.file.Path;

/**
 * Interfaz común para renderizadores de plantillas.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public interface TemplateRenderer {

    /**
     * @return extensión del formato (sin punto): "pdf", "png", etc.
     */
    String getFileExtension();

    /**
     * @return nombre legible del formato: "PDF", "PNG", etc.
     */
    String getFormatName();

    /**
     * Renderiza la plantilla y la guarda en la ruta indicada.
     *
     * @param spec   especificación de la grilla
     * @param output ruta de salida
     */
    void render(GridSpec spec, Path output);
}
