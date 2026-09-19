package io.kineticforge.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PageSize - Tamaños de página")
class PageSizeTest {

    @Test
    @DisplayName("A4 tiene las dimensiones correctas (210x297 mm)")
    void a4HasCorrectDimensions() {
        assertEquals(210.0, PageSize.A4.getWidthMm(), 0.01);
        assertEquals(297.0, PageSize.A4.getHeightMm(), 0.01);
    }

    @Test
    @DisplayName("A3 tiene las dimensiones correctas (297x420 mm)")
    void a3HasCorrectDimensions() {
        assertEquals(297.0, PageSize.A3.getWidthMm(), 0.01);
        assertEquals(420.0, PageSize.A3.getHeightMm(), 0.01);
    }

    @Test
    @DisplayName("Letter tiene las dimensiones correctas")
    void letterHasCorrectDimensions() {
        assertEquals(215.9, PageSize.LETTER.getWidthMm(), 0.01);
        assertEquals(279.4, PageSize.LETTER.getHeightMm(), 0.01);
    }

    @Test
    @DisplayName("A4 vertical mantiene dimensiones originales")
    void a4PortraitKeepsOriginalDimensions() {
        assertEquals(210.0, PageSize.A4.widthFor(PageOrientation.PORTRAIT), 0.01);
        assertEquals(297.0, PageSize.A4.heightFor(PageOrientation.PORTRAIT), 0.01);
    }

    @Test
    @DisplayName("A4 horizontal intercambia ancho y alto")
    void a4LandscapeSwapsDimensions() {
        assertEquals(297.0, PageSize.A4.widthFor(PageOrientation.LANDSCAPE), 0.01);
        assertEquals(210.0, PageSize.A4.heightFor(PageOrientation.LANDSCAPE), 0.01);
    }

    @ParameterizedTest
    @EnumSource(PageSize.class)
    @DisplayName("widthFor siempre devuelve un valor positivo")
    void widthForIsAlwaysPositive(PageSize size) {
        assertTrue(size.widthFor(PageOrientation.PORTRAIT) > 0);
        assertTrue(size.widthFor(PageOrientation.LANDSCAPE) > 0);
    }

    @ParameterizedTest
    @EnumSource(PageSize.class)
    @DisplayName("heightFor siempre devuelve un valor positivo")
    void heightForIsAlwaysPositive(PageSize size) {
        assertTrue(size.heightFor(PageOrientation.PORTRAIT) > 0);
        assertTrue(size.heightFor(PageOrientation.LANDSCAPE) > 0);
    }
}
