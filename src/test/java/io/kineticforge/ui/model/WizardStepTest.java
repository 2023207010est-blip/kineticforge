package io.kineticforge.ui.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WizardStep - Pasos del wizard")
class WizardStepTest {

    @Test
    @DisplayName("Hay exactamente 4 pasos")
    void thereAreFourSteps() {
        assertEquals(4, WizardStep.values().length);
    }

    @Test
    @DisplayName("LOAD_IMAGE es el paso 1")
    void loadImageIsStepOne() {
        assertEquals(1, WizardStep.LOAD_IMAGE.getNumber());
        assertEquals("1. Cargar imagen", WizardStep.LOAD_IMAGE.getTitle());
    }

    @Test
    @DisplayName("EXPORT es el paso 4")
    void exportIsStepFour() {
        assertEquals(4, WizardStep.EXPORT.getNumber());
        assertEquals("4. Exportar", WizardStep.EXPORT.getTitle());
    }

    @Test
    @DisplayName("previous() del primer paso devuelve el mismo")
    void previousOfFirstIsSame() {
        assertSame(WizardStep.LOAD_IMAGE, WizardStep.LOAD_IMAGE.previous());
    }

    @Test
    @DisplayName("next() del último paso devuelve el mismo")
    void nextOfLastIsSame() {
        assertSame(WizardStep.EXPORT, WizardStep.EXPORT.next());
    }

    @Test
    @DisplayName("previous() navega correctamente entre pasos")
    void previousNavigatesCorrectly() {
        assertEquals(WizardStep.LOAD_IMAGE, WizardStep.DETECT_GRID.previous());
        assertEquals(WizardStep.DETECT_GRID, WizardStep.PROCESS_FRAMES.previous());
        assertEquals(WizardStep.PROCESS_FRAMES, WizardStep.EXPORT.previous());
    }

    @Test
    @DisplayName("next() navega correctamente entre pasos")
    void nextNavigatesCorrectly() {
        assertEquals(WizardStep.DETECT_GRID, WizardStep.LOAD_IMAGE.next());
        assertEquals(WizardStep.PROCESS_FRAMES, WizardStep.DETECT_GRID.next());
        assertEquals(WizardStep.EXPORT, WizardStep.PROCESS_FRAMES.next());
    }

    @ParameterizedTest
    @EnumSource(WizardStep.class)
    @DisplayName("fromNumber devuelve el paso correcto para cada número")
    void fromNumberReturnsCorrectStep(WizardStep step) {
        assertEquals(step, WizardStep.fromNumber(step.getNumber()));
    }

    @Test
    @DisplayName("fromNumber lanza excepción con número inválido")
    void fromNumberThrowsOnInvalid() {
        assertThrows(IllegalArgumentException.class,
            () -> WizardStep.fromNumber(0));
        assertThrows(IllegalArgumentException.class,
            () -> WizardStep.fromNumber(5));
        assertThrows(IllegalArgumentException.class,
            () -> WizardStep.fromNumber(-1));
    }

    @ParameterizedTest
    @EnumSource(WizardStep.class)
    @DisplayName("Todos los pasos tienen título y número")
    void allStepsHaveTitleAndNumber(WizardStep step) {
        assertTrue(step.getNumber() >= 1 && step.getNumber() <= 4);
        assertTrue(step.getTitle() != null && !step.getTitle().isBlank());
    }
}
