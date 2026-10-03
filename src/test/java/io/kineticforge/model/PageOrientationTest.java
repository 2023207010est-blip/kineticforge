package io.kineticforge.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PageOrientation - Orientación de página")
class PageOrientationTest {

    @Test
    @DisplayName("PORTRAIT es vertical")
    void portraitIsPortrait() {
        assertTrue(PageOrientation.PORTRAIT.isPortrait());
        assertFalse(PageOrientation.PORTRAIT.isLandscape());
    }

    @Test
    @DisplayName("LANDSCAPE es horizontal")
    void landscapeIsLandscape() {
        assertTrue(PageOrientation.LANDSCAPE.isLandscape());
        assertFalse(PageOrientation.LANDSCAPE.isPortrait());
    }
}
