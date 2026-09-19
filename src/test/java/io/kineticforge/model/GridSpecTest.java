package io.kineticforge.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GridSpec - Especificacion de grilla")
class GridSpecTest {

    @Test
    @DisplayName("Builder por defecto crea 8x6 horizontal con punto guia")
    void defaultBuilderCreatesCompactLandscapeWithGuideDot() {
        GridSpec spec = GridSpec.defaults();

        assertEquals(GridPreset.COMPACTA, spec.preset());
        assertEquals(PageOrientation.LANDSCAPE, spec.orientation());
        assertEquals(PageSize.A4, spec.pageSize());
        assertTrue(spec.includeGuideDot());
        assertEquals(GuideDotStyle.CIRCLE, spec.guideDotStyle());
        assertEquals(GuideDotColor.LIGHT_GRAY, spec.guideDotColor());
    }

    @Test
    @DisplayName("Constructor completo asigna todos los campos")
    void fullConstructorAssignsAllFields() {
        GridSpec spec = new GridSpec(
                GridPreset.DETALLE, PageOrientation.PORTRAIT, PageSize.A3,
                10.0, 3.0, false, GuideDotStyle.NONE,
                GuideDotColor.LIGHT_GRAY, 2.0);

        assertEquals(GridPreset.DETALLE, spec.preset());
        assertEquals(PageOrientation.PORTRAIT, spec.orientation());
        assertEquals(PageSize.A3, spec.pageSize());
        assertEquals(10.0, spec.marginMm());
        assertEquals(3.0, spec.gutterMm());
        assertFalse(spec.includeGuideDot());
    }

    @Test
    @DisplayName("Constructor rechaza preset nulo")
    void constructorRejectsNullPreset() {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(
                null, PageOrientation.LANDSCAPE, PageSize.A4,
                5.0, 2.0, true, GuideDotStyle.CIRCLE,
                GuideDotColor.LIGHT_GRAY, 1.5));
    }

    @Test
    @DisplayName("Constructor rechaza margen negativo")
    void constructorRejectsNegativeMargin() {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(
                GridPreset.COMPACTA, PageOrientation.LANDSCAPE, PageSize.A4,
                -1.0, 2.0, true, GuideDotStyle.CIRCLE,
                GuideDotColor.LIGHT_GRAY, 1.5));
    }

    @Test
    @DisplayName("Constructor rechaza gutter negativo")
    void constructorRejectsNegativeGutter() {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(
                GridPreset.COMPACTA, PageOrientation.LANDSCAPE, PageSize.A4,
                5.0, -1.0, true, GuideDotStyle.CIRCLE,
                GuideDotColor.LIGHT_GRAY, 1.5));
    }

    @Test
    @DisplayName("Constructor rechaza punto guia con estilo NONE")
    void constructorRejectsGuideDotWithNoneStyle() {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(
                GridPreset.COMPACTA, PageOrientation.LANDSCAPE, PageSize.A4,
                5.0, 2.0, true, GuideDotStyle.NONE,
                GuideDotColor.LIGHT_GRAY, 1.5));
    }

    @Test
    @DisplayName("Constructor rechaza diametro no positivo")
    void constructorRejectsNonPositiveGuideDotDiameter() {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(
                GridPreset.COMPACTA, PageOrientation.LANDSCAPE, PageSize.A4,
                5.0, 2.0, true, GuideDotStyle.CIRCLE,
                GuideDotColor.LIGHT_GRAY, 0.0));
    }

    @Test
    @DisplayName("Constructor rechaza margen demasiado grande")
    void constructorRejectsTooLargeMargin() {
        assertThrows(IllegalArgumentException.class, () -> new GridSpec(
                GridPreset.SUPER_DENSA, PageOrientation.PORTRAIT, PageSize.A4,
                50.0, 10.0, false, GuideDotStyle.NONE,
                GuideDotColor.LIGHT_GRAY, 1.5));
    }

    @Test
    @DisplayName("pageWidthMm respeta orientacion")
    void pageWidthRespectsOrientation() {
        GridSpec portrait = GridSpec.builder()
                .orientation(PageOrientation.PORTRAIT).build();
        GridSpec landscape = GridSpec.builder()
                .orientation(PageOrientation.LANDSCAPE).build();

        assertEquals(210.0, portrait.pageWidthMm(), 0.01);
        assertEquals(297.0, landscape.pageWidthMm(), 0.01);
    }

    @Test
    @DisplayName("cellWidthMm y cellHeightMm son positivos")
    void cellDimensionsArePositive() {
        GridSpec spec = GridSpec.defaults();
        assertTrue(spec.cellWidthMm() > 0);
        assertTrue(spec.cellHeightMm() > 0);
    }

    @Test
    @DisplayName("totalCells coincide con preset")
    void totalCellsMatchesPreset() {
        GridSpec spec = GridSpec.builder()
                .preset(GridPreset.COMPACTA).build();
        assertEquals(48, spec.totalCells());
    }

    @Test
    @DisplayName("withPreset devuelve nueva instancia")
    void withPresetReturnsNewInstance() {
        GridSpec original = GridSpec.defaults();
        GridSpec modified = original.withPreset(GridPreset.DETALLE);

        assertEquals(GridPreset.COMPACTA, original.preset());
        assertEquals(GridPreset.DETALLE, modified.preset());
        assertNotSame(original, modified);
    }

    @Test
    @DisplayName("withOrientation devuelve nueva instancia")
    void withOrientationReturnsNewInstance() {
        GridSpec original = GridSpec.defaults();
        GridSpec modified = original.withOrientation(PageOrientation.PORTRAIT);

        assertEquals(PageOrientation.LANDSCAPE, original.orientation());
        assertEquals(PageOrientation.PORTRAIT, modified.orientation());
    }

    @ParameterizedTest
    @EnumSource(GridPreset.class)
    @DisplayName("Todos los presets funcionan en A4 horizontal")
    void allPresetsWorkInA4Landscape(GridPreset preset) {
        GridSpec spec = GridSpec.builder()
                .preset(preset)
                .orientation(PageOrientation.LANDSCAPE)
                .pageSize(PageSize.A4)
                .build();

        assertTrue(spec.cellWidthMm() > 0);
        assertTrue(spec.cellHeightMm() > 0);
    }
}
