package io.kineticforge.util;

import java.nio.file.Path;

/**
 * Utilidades para rutas de archivos de metadatos.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class MetadataPaths {

    private MetadataPaths() {
    }

    /**
     * Construye la ruta del archivo .json asociado a una plantilla.
     *
     * <p>Ejemplo: compacta-a4-horizontal.pdf -> compacta-a4-horizontal.json</p>
     *
     * @param templatePath ruta del archivo de plantilla (PDF o PNG)
     * @return ruta del archivo .json correspondiente
     */
    public static Path forTemplate(Path templatePath) {
        String fileName = templatePath.getFileName().toString();
        String baseName = fileName.replaceAll("\\.[^.]+$", "");
        return templatePath.getParent().resolve(baseName + ".json");
    }
}
