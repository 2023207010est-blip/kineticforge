package io.kineticforge.ui.model;

import io.kineticforge.core.grid.GridDetectionResult;
import io.kineticforge.model.ExportConfig;
import io.kineticforge.model.GridSpec;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

/**
 * Estado compartido entre los pasos del wizard.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class WizardState {

    private final ObjectProperty<Path> sourceFile = new SimpleObjectProperty<>();
    private final ObjectProperty<BufferedImage> originalImage = new SimpleObjectProperty<>();
    private final ObjectProperty<GridSpec> gridSpec = new SimpleObjectProperty<>();
    private final ObjectProperty<GridDetectionResult> detectionResult = new SimpleObjectProperty<>();
    private final ObjectProperty<List<BufferedImage>> processedFrames = new SimpleObjectProperty<>();
    private final ObjectProperty<ExportConfig> exportConfig =
            new SimpleObjectProperty<>(ExportConfig.defaults());

    private final BooleanProperty loadComplete = new SimpleBooleanProperty(false);
    private final BooleanProperty gridComplete = new SimpleBooleanProperty(false);
    private final BooleanProperty processComplete = new SimpleBooleanProperty(false);

    public Path getSourceFile() { return sourceFile.get(); }
    public void setSourceFile(Path p) { sourceFile.set(p); }
    public ObjectProperty<Path> sourceFileProperty() { return sourceFile; }

    public BufferedImage getOriginalImage() { return originalImage.get(); }
    public void setOriginalImage(BufferedImage img) { originalImage.set(img); }
    public ObjectProperty<BufferedImage> originalImageProperty() { return originalImage; }

    public GridSpec getGridSpec() { return gridSpec.get(); }
    public void setGridSpec(GridSpec spec) { gridSpec.set(spec); }
    public ObjectProperty<GridSpec> gridSpecProperty() { return gridSpec; }

    public GridDetectionResult getDetectionResult() { return detectionResult.get(); }
    public void setDetectionResult(GridDetectionResult r) { detectionResult.set(r); }
    public ObjectProperty<GridDetectionResult> detectionResultProperty() { return detectionResult; }

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
        detectionResult.set(null);
        processedFrames.set(null);
        exportConfig.set(ExportConfig.defaults());
        loadComplete.set(false);
        gridComplete.set(false);
        processComplete.set(false);
    }
}
