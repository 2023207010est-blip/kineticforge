package io.kineticforge.ui.model;

import io.kineticforge.model.ExportConfig;
import io.kineticforge.model.SheetScan;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

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

    private SheetScan sheet(String name) {
        return new SheetScan(Path.of("/tmp/" + name), new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB));
    }

    @Test
    @DisplayName("El estado inicial está vacío")
    void initialStateIsEmpty() {
        assertTrue(state.getSheets().isEmpty());
        assertNull(state.getProcessedFrames());
        assertFalse(state.isLoadingComplete());
        assertFalse(state.isGridComplete());
        assertFalse(state.isProcessComplete());
        assertFalse(state.allSheetsHaveGrid());
        assertEquals(0, state.totalCells());
    }

    @Test
    @DisplayName("La config de exportación viene por defecto")
    void exportConfigHasDefaults() {
        assertNotNull(state.getExportConfig());
        assertEquals(12, state.getExportConfig().fps());
        assertTrue(state.getExportConfig().loop());
    }

    @Test
    @DisplayName("Se puede cambiar la config de exportación")
    void canChangeExportConfig() {
        state.setExportConfig(ExportConfig.defaults().withFps(24).withLoop(false));
        assertEquals(24, state.getExportConfig().fps());
        assertFalse(state.getExportConfig().loop());
    }

    @Test
    @DisplayName("allSheetsHaveGrid exige que TODAS las hojas tengan grilla")
    void allSheetsHaveGridRequiresEverySheet() {
        SheetScan a = sheet("a.png");
        SheetScan b = sheet("b.png");
        state.getSheets().addAll(List.of(a, b));

        a.setWholeImage(List.of(new Rectangle(0, 0, 10, 10)));
        assertFalse(state.allSheetsHaveGrid());

        b.setWholeImage(List.of(new Rectangle(0, 0, 10, 10)));
        assertTrue(state.allSheetsHaveGrid());
    }

    @Test
    @DisplayName("totalCells suma las celdas de todas las hojas")
    void totalCellsSumsAllSheets() {
        SheetScan a = sheet("a.png");
        SheetScan b = sheet("b.png");
        a.setWholeImage(List.of(new Rectangle(0, 0, 10, 10)));
        b.setGrid(null, List.of(new Rectangle(0, 0, 5, 5), new Rectangle(5, 0, 5, 5),
                new Rectangle(0, 5, 5, 5)));
        state.getSheets().addAll(List.of(a, b));

        assertEquals(4, state.totalCells());
    }

    @Test
    @DisplayName("invalidateProcessing descarta el resultado")
    void invalidateProcessingClearsResult() {
        state.setProcessedFrames(List.of(new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB)));
        state.setProcessComplete(true);

        state.invalidateProcessing();

        assertNull(state.getProcessedFrames());
        assertFalse(state.isProcessComplete());
    }

    @Test
    @DisplayName("reset() limpia todo el estado")
    void resetClearsEverything() {
        state.getSheets().add(sheet("a.png"));
        state.setLoadComplete(true);
        state.setGridComplete(true);
        state.setProcessComplete(true);
        state.setExportConfig(ExportConfig.defaults().withFps(30));

        state.reset();

        assertTrue(state.getSheets().isEmpty());
        assertNull(state.getProcessedFrames());
        assertFalse(state.isLoadingComplete());
        assertFalse(state.isGridComplete());
        assertFalse(state.isProcessComplete());
        assertEquals(12, state.getExportConfig().fps());
    }

    @Test
    @DisplayName("Las propiedades observables están disponibles")
    void observablePropertiesAreAvailable() {
        assertNotNull(state.processedFramesProperty());
        assertNotNull(state.exportConfigProperty());
        assertNotNull(state.loadCompleteProperty());
        assertNotNull(state.gridCompleteProperty());
        assertNotNull(state.processCompleteProperty());
    }
}
