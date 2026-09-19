package io.kineticforge.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GuideDotColor - Colores del punto guía")
class GuideDotColorTest {

    @Test
    @DisplayName("LIGHT_GRAY tiene RGB correcto")
    void lightGrayHasCorrectRgb() {
        assertEquals(0xE0, GuideDotColor.LIGHT_GRAY.getRed());
        assertEquals(0xE0, GuideDotColor.LIGHT_GRAY.getGreen());
        assertEquals(0xE0, GuideDotColor.LIGHT_GRAY.getBlue());
        assertEquals(0xE0E0E0, GuideDotColor.LIGHT_GRAY.toRgb());
    }

    @Test
    @DisplayName("LIGHT_BLUE tiene RGB correcto")
    void lightBlueHasCorrectRgb() {
        assertEquals(0xBB, GuideDotColor.LIGHT_BLUE.getRed());
        assertEquals(0xDE, GuideDotColor.LIGHT_BLUE.getGreen());
        assertEquals(0xFB, GuideDotColor.LIGHT_BLUE.getBlue());
        assertEquals(0xBBDEFB, GuideDotColor.LIGHT_BLUE.toRgb());
    }

    @ParameterizedTest
    @EnumSource(GuideDotColor.class)
    @DisplayName("Todos los colores tienen componentes entre 0 y 255")
    void allColorsHaveValidComponents(GuideDotColor color) {
        assertTrue(color.getRed() >= 0 && color.getRed() <= 255);
        assertTrue(color.getGreen() >= 0 && color.getGreen() <= 255);
        assertTrue(color.getBlue() >= 0 && color.getBlue() <= 255);
    }
}
