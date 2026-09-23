package io.kineticforge.io;

import io.kineticforge.exception.ImageProcessingException;
import io.kineticforge.model.GridMetadata;
import io.kineticforge.model.GridSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Cargador de metadatos de plantilla.
 *
 * <p>Busca un archivo `.json` asociado a un escaneo. Si lo encuentra,
 * devuelve las medidas exactas de la grilla. Si no, devuelve
 * {@code Optional.empty()} para que el wizard pida al usuario
 * que elija el preset manualmente.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class MetadataLoader {

    private static final Logger log = LoggerFactory.getLogger(MetadataLoader.class);

    /**
     * Busca y carga los metadatos asociados a un archivo escaneado.
     *
     * <p>Busca el archivo con el mismo nombre pero extensión `.json`.
     * Ejemplo: {@code escaneo.png} → {@code escaneo.json}</p>
     *
     * @param scanPath ruta del escaneo
     * @return metadatos si existen, {@code Optional.empty()} si no
     * @throws ImageProcessingException si el `.json` existe pero está corrupto
     */
    public Optional<GridMetadata> loadFor(Path scanPath) {
        if (scanPath == null) {
            return Optional.empty();
        }

        Path metadataPath = GridMetadata.findMetadataFor(scanPath);

        if (metadataPath == null) {
            log.info("No se encontró archivo de metadatos para: {}", scanPath.getFileName());
            return Optional.empty();
        }

        try {
            GridMetadata metadata = GridMetadata.load(metadataPath);
            log.info("Metadatos cargados: {} ({}×{})",
                metadataPath.getFileName(),
                metadata.columns(),
                metadata.rows());
            return Optional.of(metadata);

        } catch (IOException e) {
            throw new ImageProcessingException(
                "No se pudo leer el archivo de metadatos: " + metadataPath, e);
        }
    }

    /**
     * Verifica si un escaneo tiene metadatos asociados.
     */
    public boolean hasMetadata(Path scanPath) {
        return scanPath != null && GridMetadata.findMetadataFor(scanPath) != null;
    }

    /**
     * Guarda metadatos junto a un archivo.
     *
     * <p>Útil cuando el usuario elige manualmente el preset y queremos
     * asociar los metadatos al escaneo para futuras sesiones.</p>
     *
     * @param scanPath ruta del escaneo
     * @param spec     especificación de la grilla
     */
    public void saveFor(Path scanPath, GridSpec spec) {
        if (scanPath == null || spec == null) {
            throw new IllegalArgumentException("scanPath y spec no pueden ser nulos");
        }

        Path metadataPath = GridMetadata.findMetadataFor(scanPath);

        if (metadataPath == null) {
            // Construir la ruta manualmente (aún no existe)
            String fileName = scanPath.getFileName().toString();
            String baseName = fileName.replaceAll("\\.[^.]+$", "");
            metadataPath = scanPath.getParent().resolve(baseName + ".json");
        }

        try {
            GridMetadata metadata = GridMetadata.from(spec);
            metadata.save(metadataPath);
            log.info("Metadatos guardados: {}", metadataPath);

        } catch (IOException e) {
            throw new ImageProcessingException(
                "No se pudieron guardar los metadatos: " + metadataPath, e);
        }
    }
}
