package io.kineticforge.model;

/**
 * Especificación completa de una grilla para generar plantillas.
 *
 * <p>Es un {@code record} inmutable: una vez creado no se puede
 * modificar. Para cambiar algo, se crea uno nuevo con {@code with...}.</p>
 *
 * <p>Ejemplo de uso:</p>
 * <pre>{@code
 * GridSpec spec = GridSpec.builder()
 *     .preset(GridPreset.COMPACTA)
 *     .orientation(PageOrientation.LANDSCAPE)
 *     .pageSize(PageSize.A4)
 *     .marginMm(5.0)
 *     .gutterMm(2.0)
 *     .includeGuideDot(true)
 *     .guideDotStyle(GuideDotStyle.CIRCLE)
 *     .guideDotColor(GuideDotColor.LIGHT_GRAY)
 *     .guideDotDiameterMm(1.5)
 *     .build();
 * }</pre>
 *
 * @param preset              preset de grilla (filas y columnas)
 * @param orientation         orientación de la página
 * @param pageSize            tamaño de página (A4, A3, Letter)
 * @param marginMm            margen exterior en milímetros
 * @param gutterMm            separación entre celdas en milímetros
 * @param includeGuideDot     si se dibuja el punto guía central
 * @param guideDotStyle       estilo visual del punto guía
 * @param guideDotColor       color del punto guía
 * @param guideDotDiameterMm  diámetro del punto guía en milímetros
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public record GridSpec(
    GridPreset preset,
    PageOrientation orientation,
    PageSize pageSize,
    double marginMm,
    double gutterMm,
    boolean includeGuideDot,
    GuideDotStyle guideDotStyle,
    GuideDotColor guideDotColor,
    double guideDotDiameterMm
) {

    /**
     * Constructor compacto con validaciones.
     */
    public GridSpec {
        if (preset == null) {
            throw new IllegalArgumentException("El preset no puede ser nulo");
        }
        if (orientation == null) {
            throw new IllegalArgumentException("La orientación no puede ser nula");
        }
        if (pageSize == null) {
            throw new IllegalArgumentException("El tamaño de página no puede ser nulo");
        }
        if (marginMm < 0) {
            throw new IllegalArgumentException("El margen no puede ser negativo");
        }
        if (gutterMm < 0) {
            throw new IllegalArgumentException("El gutter no puede ser negativo");
        }
        if (includeGuideDot) {
            if (guideDotStyle == null || guideDotStyle == GuideDotStyle.NONE) {
                throw new IllegalArgumentException(
                    "Si includeGuideDot es true, guideDotStyle no puede ser NONE");
            }
            if (guideDotColor == null) {
                throw new IllegalArgumentException("El color del punto guía no puede ser nulo");
            }
            if (guideDotDiameterMm <= 0) {
                throw new IllegalArgumentException(
                    "El diámetro del punto guía debe ser positivo");
            }
        }

        // Validar que las celdas sean positivas
        double cellW = preset.calculateCellWidthMm(
            pageSize.widthFor(orientation), marginMm, gutterMm);
        double cellH = preset.calculateCellHeightMm(
            pageSize.heightFor(orientation), marginMm, gutterMm);

        if (cellW <= 0 || cellH <= 0) {
            throw new IllegalArgumentException(
                "El margen y gutter son demasiado grandes para la página elegida");
        }
    }

    // ============================================================
    // Getters de conveniencia (records ya exponen los campos)
    // ============================================================

    /**
     * @return ancho total de la página en milímetros según orientación.
     */
    public double pageWidthMm() {
        return pageSize.widthFor(orientation);
    }

    /**
     * @return alto total de la página en milímetros según orientación.
     */
    public double pageHeightMm() {
        return pageSize.heightFor(orientation);
    }

    /**
     * @return ancho de cada celda en milímetros.
     */
    public double cellWidthMm() {
        return preset.calculateCellWidthMm(pageWidthMm(), marginMm, gutterMm);
    }

    /**
     * @return alto de cada celda en milímetros.
     */
    public double cellHeightMm() {
        return preset.calculateCellHeightMm(pageHeightMm(), marginMm, gutterMm);
    }

    /**
     * @return cantidad total de celdas.
     */
    public int totalCells() {
        return preset.getTotalCells();
    }

    /**
     * @return cantidad de columnas.
     */
    public int columns() {
        return preset.getColumns();
    }

    /**
     * @return cantidad de filas.
     */
    public int rows() {
        return preset.getRows();
    }

    // ============================================================
    // Métodos "with" para crear copias modificadas
    // ============================================================

    public GridSpec withPreset(GridPreset newPreset) {
        return new GridSpec(newPreset, orientation, pageSize, marginMm, gutterMm,
            includeGuideDot, guideDotStyle, guideDotColor, guideDotDiameterMm);
    }

    public GridSpec withOrientation(PageOrientation newOrientation) {
        return new GridSpec(preset, newOrientation, pageSize, marginMm, gutterMm,
            includeGuideDot, guideDotStyle, guideDotColor, guideDotDiameterMm);
    }

    public GridSpec withPageSize(PageSize newPageSize) {
        return new GridSpec(preset, orientation, newPageSize, marginMm, gutterMm,
            includeGuideDot, guideDotStyle, guideDotColor, guideDotDiameterMm);
    }

    public GridSpec withGuideDot(boolean newInclude) {
        return new GridSpec(preset, orientation, pageSize, marginMm, gutterMm,
            newInclude, guideDotStyle, guideDotColor, guideDotDiameterMm);
    }

    // ============================================================
    // Builder
    // ============================================================

    /**
     * @return un builder con valores por defecto razonables.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return una configuración por defecto: A4 horizontal, 8×6, con punto guía.
     */
    public static GridSpec defaults() {
        return builder().build();
    }

    /**
     * Builder mutable para crear instancias de {@link GridSpec}.
     */
    public static final class Builder {
        private GridPreset preset = GridPreset.COMPACTA;
        private PageOrientation orientation = PageOrientation.LANDSCAPE;
        private PageSize pageSize = PageSize.A4;
        private double marginMm = 5.0;
        private double gutterMm = 2.0;
        private boolean includeGuideDot = false;
        private GuideDotStyle guideDotStyle = GuideDotStyle.CIRCLE;
        private GuideDotColor guideDotColor = GuideDotColor.LIGHT_GRAY;
        private double guideDotDiameterMm = 1.5;

        private Builder() {
        }

        public Builder preset(GridPreset preset) {
            this.preset = preset;
            return this;
        }

        public Builder orientation(PageOrientation orientation) {
            this.orientation = orientation;
            return this;
        }

        public Builder pageSize(PageSize pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder marginMm(double marginMm) {
            this.marginMm = marginMm;
            return this;
        }

        public Builder gutterMm(double gutterMm) {
            this.gutterMm = gutterMm;
            return this;
        }

        public Builder includeGuideDot(boolean includeGuideDot) {
            this.includeGuideDot = includeGuideDot;
            return this;
        }

        public Builder guideDotStyle(GuideDotStyle guideDotStyle) {
            this.guideDotStyle = guideDotStyle;
            return this;
        }

        public Builder guideDotColor(GuideDotColor guideDotColor) {
            this.guideDotColor = guideDotColor;
            return this;
        }

        public Builder guideDotDiameterMm(double guideDotDiameterMm) {
            this.guideDotDiameterMm = guideDotDiameterMm;
            return this;
        }

        public GridSpec build() {
            return new GridSpec(preset, orientation, pageSize, marginMm, gutterMm,
                includeGuideDot, guideDotStyle, guideDotColor, guideDotDiameterMm);
        }
    }
}
