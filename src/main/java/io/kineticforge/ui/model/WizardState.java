package io.kineticforge.ui.model;

import io.kineticforge.model.ExportConfig;
import io.kineticforge.model.GridMetadata;
import io.kineticforge.model.GridSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * Estado compartido entre los pasos del wizard.
 *
 * <p>Ahora usa {@link GridMetadata} (medidas exactas del PDF) en vez
 * de {@code GridDetectionResult} (auto-detección de líneas).</p>
 *
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class WizardState {

    private final ObjectProperty<Path> sourceFile = new SimpleObjectProperty<>();
    private final ObjectProperty<BufferedImage> originalImage = new SimpleObjectProperty<>();
    private final ObjectProperty<GridSpec> gridSpec = new SimpleObjectProperty<>();
    private final ObjectProperty<GridMetadata> metadata = new SimpleObjectProperty<>();
    private final ObjectProperty<List<Rectangle>> cells = new SimpleObjectProperty<>();
    private final ObjectProperty<List<BufferedImage>> processedFrames = new SimpleObjectProperty<>();
    private final ObjectProperty<ExportConfig> exportConfig =
            new SimpleObjectProperty<>(ExportConfig.defaults());

    private final BooleanProperty loadComplete = new SimpleBooleanProperty(false);
    private final BooleanProperty gridComplete = new SimpleBooleanProperty(false);
    private final BooleanProperty processComplete = new SimpleBooleanProperty(false);

    // --- Getters y setters ---

    public Path getSourceFile() { return sourceFile.get(); }
    public void setSourceFile(Path p) { sourceFile.set(p); }
    public ObjectProperty<Path> sourceFileProperty() { return sourceFile; }

    public BufferedImage getOriginalImage() { return originalImage.get(); }
    public void setOriginalImage(BufferedImage img) { originalImage.set(img); }
    public ObjectProperty<BufferedImage> originalImageProperty() { return originalImage; }

    public GridSpec getGridSpec() { return gridSpec.get(); }
    public void setGridSpec(GridSpec spec) { gridSpec.set(spec); }
    public ObjectProperty<GridSpec> gridSpecProperty() { return gridSpec; }

    public GridMetadata getMetadata() { return metadata.get(); }
    public void setMetadata(GridMetadata m) { metadata.set(m); }
    public ObjectProperty<GridMetadata> metadataProperty() { return metadata; }

    public List<Rectangle> getCells() { return cells.get(); }
    public void setCells(List<Rectangle> c) { cells.set(c); }
    public ObjectProperty<List<Rectangle>> cellsProperty() { return cells; }

    public List<BufferedImage> getProcessedFrames() { return processedFrames.get(); }
    public void setProcessedFrames(List<BufferedImage> frames) { processedFrames.set(frames); }
    public ObjectProperty<List<BufferedImage>> processedFramesProperty() { return processedFrames; }

    public ExportConfig getExportConfig() { return exportConfig.get(); }
    public void setExportConfig(ExportConfig c) { exportConfig.set(c); }
    public ObjectProperty<ExportConfig> exportConfigProperty() { return exportConfig; }

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
        sourceFile.set(null);
        originalImage.set(null);
        gridSpec.set(null);
        metadata.set(null);
        cells.set(null);
        processedFrames.set(null);
        exportConfig.set(ExportConfig.defaults());
        loadComplete.set(false);
        gridComplete.set(false);
        processComplete.set(false);
    }
}
