package io.kineticforge.model;

/**
 * Presets de grilla disponibles en KineticForge.
 *
 * <p>Cada preset define una cantidad fija de filas y columnas,
 * pensada para un caso de uso específico de animación tradicional.</p>
 *
 * <p>Las dimensiones de celda se calculan en base a la página y
 * la orientación elegidas (ver {@link GridSpec}).</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum GridPreset {

    /** 4×3 = 12 celdas. Bocetos grandes con mucho detalle. */
    DETALLE(4, 3, "Detalle", "Bocetos grandes, máximo detalle"),

    /** 6×4 = 24 celdas. 1 segundo a 24 fps. */
    ESTANDAR(6, 4, "Estándar", "1 segundo a 24 fps"),

    /** 8×6 = 48 celdas. 2 segundos a 24 fps. Default. */
    COMPACTA(8, 6, "Compacta", "2 segundos a 24 fps"),

    /** 12×8 = 96 celdas. 4 segundos a 24 fps. */
    DENSA(12, 8, "Densa", "4 segundos a 24 fps"),

    /** 16×6 = 96 celdas. Widescreen. */
    CINEMATICA(16, 6, "Cinemática", "Formato widescreen"),

    /** 24×12 = 288 celdas. 12 segundos a 24 fps. */
    SUPER_DENSA(24, 12, "Súper densa", "12 segundos a 24 fps");

    private final int columns;
    private final int rows;
    private final String displayName;
    private final String description;

    GridPreset(int columns, int rows, String displayName, String description) {
        this.columns = columns;
        this.rows = rows;
        this.displayName = displayName;
        this.description = description;
    }

    public int getColumns() {
        return columns;
    }

    public int getRows() {
        return rows;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return cantidad total de celdas del preset.
     */
    public int getTotalCells() {
        return columns * rows;
    }

    /**
     * Calcula el ancho en milímetros de cada celda.
     *
     * <p>Fórmula: (ancho página - 2·margen - (columnas-1)·gutter) / columnas</p>
     *
     * @param pageWidthMm ancho de la página en mm
     * @param marginMm    margen exterior en mm
     * @param gutterMm    separación entre celdas en mm
     * @return ancho de cada celda en mm
     */
    public double calculateCellWidthMm(double pageWidthMm, double marginMm, double gutterMm) {
        return (pageWidthMm - 2.0 * marginMm - (columns - 1) * gutterMm) / columns;
    }

    /**
     * Calcula el alto en milímetros de cada celda.
     *
     * <p>Fórmula: (alto página - 2·margen - (filas-1)·gutter) / filas</p>
     *
     * @param pageHeightMm alto de la página en mm
     * @param marginMm     margen exterior en mm
     * @param gutterMm     separación entre celdas en mm
     * @return alto de cada celda en mm
     */
    public double calculateCellHeightMm(double pageHeightMm, double marginMm, double gutterMm) {
        return (pageHeightMm - 2.0 * marginMm - (rows - 1) * gutterMm) / rows;
    }

    /**
     * @return representación legible: "8×6 (48 celdas)".
     */
    @Override
    public String toString() {
        return String.format("%d×%d (%d celdas)", columns, rows, getTotalCells());
    }
}
