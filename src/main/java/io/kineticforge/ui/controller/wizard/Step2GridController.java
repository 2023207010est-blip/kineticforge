package io.kineticforge.ui.controller.wizard;

import io.kineticforge.core.grid.GridDetectionResult;
import io.kineticforge.core.grid.GridDetector;
import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

/**
 * Controlador del paso 2: detección de grilla.
 *
 * @author KineticForge Team
 * @version 1.0.2
 * @since 2026
 */
public class Step2GridController {

    private static final Logger log = LoggerFactory.getLogger(Step2GridController.class);

    @FXML private StackPane previewContainer;
    @FXML private Canvas previewCanvas;
    @FXML private ComboBox<GridPreset> presetCombo;
    @FXML private ToggleButton portraitToggle;
    @FXML private ToggleButton landscapeToggle;
    @FXML private Slider marginSlider;
    @FXML private Label marginLabel;
    @FXML private CheckBox guideDotCheck;
    @FXML private Button detectButton;
    @FXML private Label statusLabel;

    private WizardState state;
    private Runnable onComplete;
    private GridDetector detector;
    private Image previewImage;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;
        this.detector = new GridDetector();

        setupControls();
        loadPreview();

        log.info("Paso 2 inicializado");
    }

    private void setupControls() {
        presetCombo.getItems().setAll(GridPreset.values());
        presetCombo.getSelectionModel().select(GridPreset.COMPACTA);
        presetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(GridPreset preset) {
                return preset == null ? "" :
                    preset.getDisplayName() + " (" + preset.getTotalCells() + " celdas)";
            }

            @Override
            public GridPreset fromString(String s) {
                return null;
            }
        });

        marginSlider.valueProperty().addListener((obs, old, val) -> {
            marginLabel.setText(String.format("Margen: %.0f mm", val.doubleValue()));
        });

        detectButton.setOnAction(e -> detectGrid());
    }

    private void loadPreview() {
        BufferedImage image = state.getOriginalImage();
        if (image == null) {
            statusLabel.setText("No hay imagen cargada.");
            detectButton.setDisable(true);
            return;
        }

        previewImage = ImageConverter.toFxImage(image);

        // El Canvas siempre tiene el mismo tamaño que el StackPane
        previewCanvas.widthProperty().bind(previewContainer.widthProperty());
        previewCanvas.heightProperty().bind(previewContainer.heightProperty());

        // Redibujar cada vez que cambie el tamaño
        previewCanvas.widthProperty().addListener((o, ov, nv) -> redrawIfNeeded());
        previewCanvas.heightProperty().addListener((o, ov, nv) -> redrawIfNeeded());

        // Primer dibujado cuando la escena esté lista
        previewContainer.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                Platform.runLater(this::redrawIfNeeded);
            }
        });
    }

    private void redrawIfNeeded() {
        if (previewImage == null) return;
        drawPreview();
    }

    private void drawPreview() {
        if (previewImage == null) return;

        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();

        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);

        // Fondo blanco
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        // Escalar la imagen para que entre con un margen
        double imgW = previewImage.getWidth();
        double imgH = previewImage.getHeight();
        double scale = Math.min((w - 40) / imgW, (h - 40) / imgH);

        double drawW = imgW * scale;
        double drawH = imgH * scale;
        double offsetX = (w - drawW) / 2;
        double offsetY = (h - drawH) / 2;

        gc.drawImage(previewImage, offsetX, offsetY, drawW, drawH);

        // Dibujar la grilla detectada
        GridDetectionResult result = state.getDetectionResult();
        if (result != null && !result.cells().isEmpty()) {
            gc.setStroke(Color.rgb(45, 127, 249, 0.9));
            gc.setLineWidth(2);

            for (Rectangle cell : result.cells()) {
                double x = offsetX + cell.x * scale;
                double y = offsetY + cell.y * scale;
                double cw = cell.width * scale;
                double ch = cell.height * scale;
                gc.strokeRect(x, y, cw, ch);
            }
        }
    }

    private void detectGrid() {
        BufferedImage image = state.getOriginalImage();
        if (image == null) {
            Dialogs.warn("Sin imagen", "Cargá una imagen primero.");
            return;
        }

        try {
            GridSpec spec = buildGridSpec();
            log.info("Detectando con spec: {}", spec);

            statusLabel.setText("Detectando...");

            GridDetectionResult result = detector.detect(image, spec);
            state.setDetectionResult(result);
            state.setGridSpec(spec);

            String status = String.format(
                "✅ Detectadas %d de %d celdas (confianza: %.0f%%)",
                result.cellCount(), spec.totalCells(), result.confidence() * 100);
            statusLabel.setText(status);

            if (result.isSuccessful()) {
                state.setGridComplete(true);
            } else {
                state.setGridComplete(false);
                log.warn("Detección incompleta: {} de {} celdas",
                    result.cellCount(), spec.totalCells());
            }

            drawPreview();

        } catch (Exception e) {
            log.error("Error en la detección", e);
            statusLabel.setText("❌ Error: " + e.getMessage());
            Dialogs.error("Error de detección", e.getMessage());
        }
    }

    private GridSpec buildGridSpec() {
        GridPreset preset = presetCombo.getSelectionModel().getSelectedItem();
        PageOrientation orientation = portraitToggle.isSelected()
            ? PageOrientation.PORTRAIT
            : PageOrientation.LANDSCAPE;

        return GridSpec.builder()
            .preset(preset)
            .orientation(orientation)
            .pageSize(PageSize.A4)
            .marginMm(marginSlider.getValue())
            .gutterMm(2.0)
            .includeGuideDot(guideDotCheck.isSelected())
            .build();
    }
}
