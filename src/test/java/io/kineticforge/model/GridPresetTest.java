package io.kineticforge.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GridPreset - Presets de grilla")
class GridPresetTest {

    @Test
    @DisplayName("COMPACTA tiene 8 columnas x 6 filas = 48 celdas")
    void compactaHasCorrectDimensions() {
        assertEquals(8, GridPreset.COMPACTA.getColumns());
        assertEquals(6, GridPreset.COMPACTA.getRows());
        assertEquals(48, GridPreset.COMPACTA.getTotalCells());
    }

    @Test
    @DisplayName("ESTANDAR tiene 6 columnas x 4 filas = 24 celdas")
    void estandarHasCorrectDimensions() {
        assertEquals(6, GridPreset.ESTANDAR.getColumns());
        assertEquals(4, GridPreset.ESTANDAR.getRows());
        assertEquals(24, GridPreset.ESTANDAR.getTotalCells());
    }

    @Test
    @DisplayName("SUPER_DENSA tiene 24 columnas x 12 filas = 288 celdas")
    void superDensaHasCorrectDimensions() {
        assertEquals(24, GridPreset.SUPER_DENSA.getColumns());
        assertEquals(12, GridPreset.SUPER_DENSA.getRows());
        assertEquals(288, GridPreset.SUPER_DENSA.getTotalCells());
    }

    @ParameterizedTest
    @EnumSource(GridPreset.class)
    @DisplayName("Todos los presets tienen columnas y filas positivas")
    void allPresetsHavePositiveDimensions(GridPreset preset) {
        assertTrue(preset.getColumns() > 0);
        assertTrue(preset.getRows() > 0);
        assertTrue(preset.getTotalCells() > 0);
    }

    @ParameterizedTest
    @EnumSource(GridPreset.class)
    @DisplayName("Total de celdas es columnas por filas")
    void totalCellsIsColumnsTimesRows(GridPreset preset) {
        assertEquals(preset.getColumns() * preset.getRows(), preset.getTotalCells());
    }

    @Test
    @DisplayName("Calculo de celda para 8x6 en A4 horizontal")
    void calculateCellDimensionsForA4Landscape() {
        double cellW = GridPreset.COMPACTA.calculateCellWidthMm(297.0, 5.0, 2.0);
        double cellH = GridPreset.COMPACTA.calculateCellHeightMm(210.0, 5.0, 2.0);

        assertEquals(34.125, cellW, 0.001);
        assertEquals(31.6667, cellH, 0.001);
    }

    @ParameterizedTest
    @EnumSource(GridPreset.class)
    @DisplayName("Calculo de celda devuelve valores positivos para A4")
    void cellDimensionsArePositiveForA4(GridPreset preset) {
        double cellW = preset.calculateCellWidthMm(297.0, 5.0, 2.0);
        double cellH = preset.calculateCellHeightMm(210.0, 5.0, 2.0);

        assertTrue(cellW > 0, "Ancho de celda debe ser positivo para " + preset);
        assertTrue(cellH > 0, "Alto de celda debe ser positivo para " + preset);
    }

    @Test
    @DisplayName("toString devuelve formato legible")
    void toStringReturnsReadableFormat() {
        String result = GridPreset.COMPACTA.toString();
        assertTrue(result.contains("8"));
        assertTrue(result.contains("6"));
        assertTrue(result.contains("48"));
    }
}
