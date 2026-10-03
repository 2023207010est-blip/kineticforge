package io.kineticforge.ui.controller.wizard;

import io.kineticforge.core.grid.CellCalculator;
import io.kineticforge.io.MetadataLoader;
import io.kineticforge.model.GridMetadata;
import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import io.kineticforge.model.SheetScan;
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
import javafx.scene.control.ListView;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.util.List;
import java.util.Optional;

/**
 * Controlador del paso 2: Carga automática por plantilla oficial (.json) o selección manual estricta.
 *
 * @author KineticForge Team
 * @version 3.1.0
 * @since 2026
 */
public class Step2GridController {

    private static final Logger log = LoggerFactory.getLogger(Step2GridController.class);

    private static final int PREVIEW_MAX_SIDE = 1600;

    @FXML private StackPane previewContainer;
    @FXML private Canvas previewCanvas;
    @FXML private ListView<SheetScan> sheetList;
    @FXML private ToggleButton gridModeToggle;
    @FXML private ToggleButton singleModeToggle;
    @FXML private VBox gridControls;
    @FXML private ComboBox<GridPreset> presetCombo;
    @FXML private ToggleButton portraitToggle;
    @FXML private ToggleButton landscapeToggle;
    @FXML private Slider marginSlider;
    @FXML private Label marginLabel;
    @FXML private CheckBox guideDotCheck;
    @FXML private Button detectButton;
    @FXML private Button applyAllButton;
    @FXML private Label statusLabel;

    private WizardState state;
    private Runnable onComplete;
    private Image previewImage;
    private SheetScan currentSheet;
    private boolean updatingControls;
    private final MetadataLoader metadataLoader = new MetadataLoader();

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        setupControls();
        setupCanvas();

        // Intentar cargar automáticamente el .json de todas las hojas
        for (SheetScan sheet : state.getSheets()) {
            if (!sheet.hasGrid()) {
                tryAutoLoadMetadata(sheet);
            }
        }

        sheetList.setItems(state.getSheets());
        sheetList.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> onSheetSelected(sel));

        if (state.getSheets().isEmpty()) {
            statusLabel.setText("No hay hojas cargadas.");
            detectButton.setDisable(true);
            applyAllButton.setDisable(true);
        } else {
            sheetList.getSelectionModel().select(0);
        }
        updateGridComplete();

        log.info("Paso 2 inicializado automáticamente ({} hojas)", state.getSheets().size());
    }



    private void setupControls() {
        presetCombo.getItems().setAll(GridPreset.values());
        presetCombo.getSelectionModel().select(GridPreset.COMPACTA);
        presetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(GridPreset p) {
                return p == null ? "" : p.getDisplayName() + " (" + p.getTotalCells() + " celdas)";
            }
            @Override
            public GridPreset fromString(String s) { return null; }
        });

        marginSlider.valueProperty().addListener((obs, o, n) ->
            marginLabel.setText(String.format("Margen fijo plantilla: %.0f mm", n.doubleValue())));

        singleModeToggle.selectedProperty().addListener((obs, o, single) ->
            gridControls.setDisable(single));

        detectButton.setOnAction(e -> applyToCurrent());
        applyAllButton.setOnAction(e -> applyToAll());
    }

    private void setupCanvas() {
        previewCanvas.widthProperty().bind(previewContainer.widthProperty());
        previewCanvas.heightProperty().bind(previewContainer.heightProperty());
        previewCanvas.widthProperty().addListener((o, ov, nv) -> redraw());
        previewCanvas.heightProperty().addListener((o, ov, nv) -> redraw());
        previewContainer.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) Platform.runLater(this::redraw);
        });
    }

    private void onSheetSelected(SheetScan sheet) {
        currentSheet = sheet;
        if (sheet == null) {
            previewImage = null;
            redraw();
            return;
        }

        previewImage = ImageConverter.toFxImage(
            ImageConverter.scaleToFit(sheet.getImage(), PREVIEW_MAX_SIDE));
        syncControlsFromSheet(sheet);
        statusLabel.setText(describeStatus(sheet));
        redraw();
    }

    private void syncControlsFromSheet(SheetScan sheet) {
        updatingControls = true;
        try {
            if (sheet.isWholeImage()) {
                singleModeToggle.setSelected(true);
                gridControls.setDisable(true);
            } else {
                gridModeToggle.setSelected(true);
                GridSpec spec = sheet.getSpec();
                if (spec != null) {
                    presetCombo.getSelectionModel().select(spec.preset());
                    if (spec.orientation() == PageOrientation.PORTRAIT) {
                        portraitToggle.setSelected(true);
                    } else {
                        landscapeToggle.setSelected(true);
                    }
                    marginSlider.setValue(spec.marginMm());
                    guideDotCheck.setSelected(spec.includeGuideDot());
                }

                // Si la hoja tiene metadatos del .json oficial, bloqueamos los controles manuales
                // para garantizar que nadie altere las medidas de la plantilla.
                boolean hasJsonMeta = (sheet.getMetadata() != null);
                gridControls.setDisable(hasJsonMeta);
            }
        } finally {
            updatingControls = false;
        }
    }

    private String describeStatus(SheetScan sheet) {
        if (!sheet.hasGrid()) {
            return "⚠ Esta hoja no tiene grilla asociada.";
        }
        if (sheet.isWholeImage()) {
            return "✅ Imagen suelta (1 frame completo).";
        }
        if (sheet.getMetadata() != null) {
            return "✅ Grilla cargada automáticamente desde la plantilla oficial (.json). Medidas protegidas.";
        }
        return "✅ " + sheet.frameCount() + " celdas configuradas manualmente.";
    }

    private void tryAutoLoadMetadata(SheetScan sheet) {
        try {
            Optional<GridMetadata> meta = metadataLoader.loadFor(sheet.getPath());
            if (meta.isEmpty()) {
                return;
            }
            GridMetadata m = meta.get();
            GridSpec spec = m.toGridSpec();
            List<Rectangle> cells = CellCalculator.compute(spec,
                sheet.getImage().getWidth(), sheet.getImage().getHeight());
            sheet.setMetadata(m);
            sheet.setGrid(spec, cells);
            log.info("Grilla de {} cargada exitosamente del .json oficial ({} celdas)",
                sheet.getFileName(), cells.size());
        } catch (Exception e) {
            log.warn("No se pudo leer el .json de {}: {}", sheet.getFileName(), e.getMessage());
        }
    }

    private void applyToCurrent() {
        if (currentSheet == null) return;
        try {
            applySettings(currentSheet);
            sheetList.refresh();
            statusLabel.setText(describeStatus(currentSheet));
            updateGridComplete();
            redraw();
        } catch (Exception e) {
            log.error("Error aplicando grilla", e);
            statusLabel.setText("❌ Error: " + e.getMessage());
            Dialogs.error("Error de verificación", e.getMessage());
        }
    }

    private void applyToAll() {
        try {
            for (SheetScan sheet : state.getSheets()) {
                applySettings(sheet);
            }
            sheetList.refresh();
            if (currentSheet != null) {
                statusLabel.setText(describeStatus(currentSheet));
            }
            updateGridComplete();
            redraw();
        } catch (Exception e) {
            log.error("Error aplicando grilla", e);
            Dialogs.error("Error de verificación", e.getMessage());
        }
    }

    private void applySettings(SheetScan sheet) {
        int w = sheet.getImage().getWidth();
        int h = sheet.getImage().getHeight();

        if (singleModeToggle.isSelected()) {
            sheet.setWholeImage(CellCalculator.wholeImage(w, h));
        } else {
            GridSpec spec = buildGridSpec();
            sheet.setGrid(spec, CellCalculator.compute(spec, w, h));
        }
        state.invalidateProcessing();
    }

    private void updateGridComplete() {
        boolean complete = state.allSheetsHaveGrid();
        state.setGridComplete(complete);
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

    private void redraw() {
        double w = previewCanvas.getWidth();
        double h = previewCanvas.getHeight();
        if (w <= 0 || h <= 0) return;

        GraphicsContext gc = previewCanvas.getGraphicsContext2D();
        gc.clearRect(0, 0, w, h);
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        if (previewImage == null || currentSheet == null) return;

        double imgW = currentSheet.getImage().getWidth();
        double imgH = currentSheet.getImage().getHeight();
        double scale = Math.min((w - 40) / imgW, (h - 40) / imgH);
        if (scale <= 0) return;

        double drawW = imgW * scale;
        double drawH = imgH * scale;
        double offsetX = (w - drawW) / 2;
        double offsetY = (h - drawH) / 2;

        gc.drawImage(previewImage, offsetX, offsetY, drawW, drawH);

        List<Rectangle> cells = currentSheet.getCells();
        if (cells != null && !cells.isEmpty()) {
            gc.setStroke(Color.rgb(45, 127, 249, 0.9));
            gc.setLineWidth(2);
            for (Rectangle cell : cells) {
                gc.strokeRect(offsetX + cell.x * scale, offsetY + cell.y * scale,
                    cell.width * scale, cell.height * scale);
            }
        }
    }
}
