package io.kineticforge.ui.model;

import io.kineticforge.model.ExportConfig;
import io.kineticforge.model.SheetScan;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.awt.image.BufferedImage;
import java.util.List;

/**
 * Estado compartido entre los pasos del wizard.
 *
 * <p>Una animación puede estar formada por VARIAS hojas ({@link SheetScan}):
 * sus frames se concatenan, en el orden de la lista, en una sola progresión.</p>
 *
 * @author KineticForge Team
 * @version 3.0.0
 * @since 2026
 */
public class WizardState {

    private final ObservableList<SheetScan> sheets = FXCollections.observableArrayList();
    private final ObjectProperty<List<BufferedImage>> processedFrames = new SimpleObjectProperty<>();
    private final ObjectProperty<ExportConfig> exportConfig =
            new SimpleObjectProperty<>(ExportConfig.defaults());

    private final BooleanProperty loadComplete = new SimpleBooleanProperty(false);
    private final BooleanProperty gridComplete = new SimpleBooleanProperty(false);
    private final BooleanProperty processComplete = new SimpleBooleanProperty(false);

    // --- Hojas ---

    public ObservableList<SheetScan> getSheets() { return sheets; }

    /** @return true si hay al menos una hoja y todas tienen grilla definida. */
    public boolean allSheetsHaveGrid() {
        if (sheets.isEmpty()) return false;
        for (SheetScan sheet : sheets) {
            if (!sheet.hasGrid()) return false;
        }
        return true;
    }

    /** @return cantidad total de celdas de todas las hojas. */
    public int totalCells() {
        int total = 0;
        for (SheetScan sheet : sheets) {
            total += sheet.frameCount();
        }
        return total;
    }

    /**
     * Descarta el resultado del procesamiento (se llama cuando cambian las
     * hojas, su orden o su grilla).
     */
    public void invalidateProcessing() {
        processedFrames.set(null);
        processComplete.set(false);
    }

    // --- Resultado y exportación ---

    public List<BufferedImage> getProcessedFrames() { return processedFrames.get(); }
    public void setProcessedFrames(List<BufferedImage> frames) { processedFrames.set(frames); }
    public ObjectProperty<List<BufferedImage>> processedFramesProperty() { return processedFrames; }

    public ExportConfig getExportConfig() { return exportConfig.get(); }
    public void setExportConfig(ExportConfig c) { exportConfig.set(c); }
    public ObjectProperty<ExportConfig> exportConfigProperty() { return exportConfig; }

    // --- Banderas de avance ---

    public boolean isLoadingComplete() { return loadComplete.get(); }
    public void setLoadComplete(boolean b) { loadComplete.set(b); }
    public BooleanProperty loadCompleteProperty() { return loadComplete; }

    public boolean isGridComplete() { return gridComplete.get(); }
    public void setGridComplete(boolean b) { gridComplete.set(b); }
    public BooleanProperty gridCompleteProperty() { return gridComplete; }

    public boolean isProcessComplete() { return processComplete.get(); }
    public void setProcessComplete(boolean b) { processComplete.set(b); }
    public BooleanProperty processCompleteProperty() { return processComplete; }

    public void reset() {
        sheets.clear();
        processedFrames.set(null);
        exportConfig.set(ExportConfig.defaults());
        loadComplete.set(false);
        gridComplete.set(false);
        processComplete.set(false);
    }
}
