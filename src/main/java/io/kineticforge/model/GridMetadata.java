package io.kineticforge.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Metadatos de una plantilla generada.
 *
 * <p>Se guarda como archivo `.json` al lado del PDF/PNG de la plantilla.
 * Contiene las medidas EXACTAS de cada celda para permitir un recorte
 * perfecto sin depender del auto-detector de líneas.</p>
 *
 * @param version         versión del formato de metadatos
 * @param preset          nombre del preset (DETALLE, COMPACTA, etc.)
 * @param orientation     orientación (PORTRAIT / LANDSCAPE)
 * @param pageSize        tamaño de página (A4, A3, LETTER)
 * @param marginMm        margen exterior en mm
 * @param gutterMm        separación entre celdas en mm
 * @param includeGuideDot si tiene punto guía
 * @param columns         cantidad de columnas
 * @param rows            cantidad de filas
 * @param cellWidthMm     ancho de cada celda en mm
 * @param cellHeightMm    alto de cada celda en mm
 * @param pageWidthMm     ancho de página en mm
 * @param pageHeightMm    alto de página en mm
 * @param dpi             resolución recomendada en DPI
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public record GridMetadata(
    String version,
    String preset,
    String orientation,
    String pageSize,
    double marginMm,
    double gutterMm,
    boolean includeGuideDot,
    int columns,
    int rows,
    double cellWidthMm,
    double cellHeightMm,
    double pageWidthMm,
    double pageHeightMm,
    int dpi
) {

    public static final String CURRENT_VERSION = "1.0";
    public static final int DEFAULT_DPI = 300;

    private static final Gson GSON = new GsonBuilder()
        .setPrettyPrinting()
        .create();

    /**
     * Crea un GridMetadata a partir de un GridSpec.
     */
    public static GridMetadata from(GridSpec spec) {
        return new GridMetadata(
            CURRENT_VERSION,
            spec.preset().name(),
            spec.orientation().name(),
            spec.pageSize().name(),
            spec.marginMm(),
            spec.gutterMm(),
            spec.includeGuideDot(),
            spec.columns(),
            spec.rows(),
            spec.cellWidthMm(),
            spec.cellHeightMm(),
            spec.pageWidthMm(),
            spec.pageHeightMm(),
            DEFAULT_DPI
        );
    }

    /**
     * Guarda los metadatos en un archivo `.json`.
     */
    public void save(Path jsonPath) throws IOException {
        String json = GSON.toJson(this);
        Files.writeString(jsonPath, json);
    }

    /**
     * Carga los metadatos desde un archivo `.json`.
     */
    public static GridMetadata load(Path jsonPath) throws IOException {
        String json = Files.readString(jsonPath);
        return GSON.fromJson(json, GridMetadata.class);
    }

    /**
     * Reconstruye el GridSpec original a partir de los metadatos.
     */
    public GridSpec toGridSpec() {
        return GridSpec.builder()
            .preset(GridPreset.valueOf(preset))
            .orientation(PageOrientation.valueOf(orientation))
            .pageSize(PageSize.valueOf(pageSize))
            .marginMm(marginMm)
            .gutterMm(gutterMm)
            .includeGuideDot(includeGuideDot)
            .build();
    }

    /**
     * Verifica si existe un archivo `.json` asociado a una imagen.
     *
     * @param imagePath ruta de la imagen (PDF o PNG)
     * @return ruta del `.json` si existe, o null
     */
    public static Path findMetadataFor(Path imagePath) {
        String fileName = imagePath.getFileName().toString();
        String baseName = fileName.replaceAll("\\.[^.]+$", "");
        Path metadataPath = imagePath.getParent().resolve(baseName + ".json");

        if (Files.exists(metadataPath)) {
            return metadataPath;
        }
        return null;
    }
}
