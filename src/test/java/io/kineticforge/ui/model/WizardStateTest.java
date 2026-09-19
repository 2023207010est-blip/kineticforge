package io.kineticforge.ui.model;

import io.kineticforge.model.ExportConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("WizardState - Estado compartido del wizard")
class WizardStateTest {

    private WizardState state;

    @BeforeEach
    void setUp() {
        state = new WizardState();
    }

    @Test
    @DisplayName("El estado inicial está vacío")
    void initialStateIsEmpty() {
        assertNull(state.getSourceFile());
        assertNull(state.getOriginalImage());
        assertNull(state.getGridSpec());
        assertNull(state.getDetectionResult());
        assertNull(state.getProcessedFrames());
        assertFalse(state.isLoadingComplete());
        assertFalse(state.isGridComplete());
        assertFalse(state.isProcessComplete());
    }

    @Test
    @DisplayName("La config de exportación viene por defecto")
    void exportConfigHasDefaults() {
        assertNotNull(state.getExportConfig());
        assertEquals(12, state.getExportConfig().fps());
        assertTrue(state.getExportConfig().loop());
    }

    @Test
    @DisplayName("Se puede setear y obtener el archivo fuente")
    void canSetSourceFile() {
        Path path = Path.of("/tmp/test.png");
        state.setSourceFile(path);
        assertEquals(path, state.getSourceFile());
    }

    @Test
    @DisplayName("Se puede marcar el paso 1 como completo")
    void canMarkLoadComplete() {
        state.setLoadComplete(true);
        assertTrue(state.isLoadingComplete());

        state.setLoadComplete(false);
        assertFalse(state.isLoadingComplete());
    }

    @Test
    @DisplayName("Se puede marcar el paso 2 como completo")
    void canMarkGridComplete() {
        state.setGridComplete(true);
        assertTrue(state.isGridComplete());
    }

    @Test
    @DisplayName("Se puede marcar el paso 3 como completo")
    void canMarkProcessComplete() {
        state.setProcessComplete(true);
        assertTrue(state.isProcessComplete());
    }

    @Test
    @DisplayName("Se puede cambiar la config de exportación")
    void canChangeExportConfig() {
        ExportConfig custom = ExportConfig.defaults().withFps(24).withLoop(false);
        state.setExportConfig(custom);

        assertEquals(24, state.getExportConfig().fps());
        assertFalse(state.getExportConfig().loop());
    }

    @Test
    @DisplayName("reset() limpia todo el estado")
    void resetClearsEverything() {
        // Poblar el estado
        state.setSourceFile(Path.of("/tmp/test.png"));
        state.setLoadComplete(true);
        state.setGridComplete(true);
        state.setProcessComplete(true);
        state.setExportConfig(ExportConfig.defaults().withFps(30));

        // Resetear
        state.reset();

        // Verificar
        assertNull(state.getSourceFile());
        assertNull(state.getOriginalImage());
        assertNull(state.getGridSpec());
        assertNull(state.getDetectionResult());
        assertNull(state.getProcessedFrames());
        assertFalse(state.isLoadingComplete());
        assertFalse(state.isGridComplete());
        assertFalse(state.isProcessComplete());
        assertEquals(12, state.getExportConfig().fps());  // vuelve al default
    }

    @Test
    @DisplayName("Las propiedades observables están disponibles")
    void observablePropertiesAreAvailable() {
        assertNotNull(state.sourceFileProperty());
        assertNotNull(state.originalImageProperty());
        assertNotNull(state.gridSpecProperty());
        assertNotNull(state.detectionResultProperty());
        assertNotNull(state.processedFramesProperty());
        assertNotNull(state.exportConfigProperty());
        assertNotNull(state.loadCompleteProperty());
        assertNotNull(state.gridCompleteProperty());
        assertNotNull(state.processCompleteProperty());
    }
}
