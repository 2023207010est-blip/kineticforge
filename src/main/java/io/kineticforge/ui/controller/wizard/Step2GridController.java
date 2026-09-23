package io.kineticforge.ui.controller.wizard;

import io.kineticforge.io.MetadataLoader;
import io.kineticforge.model.GridMetadata;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Controlador del paso 2: verificar/definir la grilla.
 *
 * <p>Usa el {@link MetadataLoader} para leer las medidas EXACTAS de la
 * plantilla (archivo .json asociado). Si no hay metadatos, permite al
 * usuario configurar manualmente el preset.</p>
 *
 * @author KineticForge Team
 * @version 2.0.0
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
    private Image previewImage;
    private final MetadataLoader metadataLoader = new MetadataLoader();

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        setupControls();
        loadPreview();
        tryAutoLoadMetadata();

        log.info("Paso 2 inicializado");
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
                marginLabel.setText(String.format("Margen: %.0f mm", n.doubleValue())));

        detectButton.setText("✓ Verificar grilla");
        detectButton.setOnAction(e -> verifyGrid());
    }

    private void loadPreview() {
        BufferedImage image = state.getOriginalImage();
        if (image == null) {
            statusLabel.setText("No hay imagen cargada.");
            detectButton.setDisable(true);
            return;
        }

        previewImage = ImageConverter.toFxImage(image);
        previewCanvas.widthProperty().bind(previewContainer.widthProperty());
        previewCanvas.heightProperty().bind(previewContainer.heightProperty());
        previewCanvas.widthProperty().addListener((o, ov, nv) -> redrawIfNeeded());
        previewCanvas.heightProperty().addListener((o, ov, nv) -> redrawIfNeeded());

        previewContainer.sceneProperty().addListener((obs, oldS, newS) -> {
            if (newS != null) Platform.runLater(this::redrawIfNeeded);
        });
    }

    /**
     * Si existe un archivo .json asociado al escaneo, lo carga automáticamente.
     */
    private void tryAutoLoadMetadata() {
        try {
            Optional<GridMetadata> meta = metadataLoader.loadFor(state.getSourceFile());

            if (meta.isEmpty()) {
                log.info("Sin metadatos. Usuario debe elegir preset manualmente.");
                statusLabel.setText("⚠ No se encontró archivo .json. Elegí el preset manualmente.");
                return;
            }

            GridMetadata m = meta.get();
            state.setMetadata(m);
            state.setGridSpec(m.toGridSpec());

            // Actualizar UI con los valores del metadata
            presetCombo.getSelectionModel().select(GridPreset.valueOf(m.preset()));
            if ("PORTRAIT".equals(m.orientation())) {
                portraitToggle.setSelected(true);
            } else {
                landscapeToggle.setSelected(true);
            }
            marginSlider.setValue(m.marginMm());
            guideDotCheck.setSelected(m.includeGuideDot());

            // Calcular celdas
            List<Rectangle> cells = computeCellsFromMetadata(m,
                    state.getOriginalImage().getWidth(),
                    state.getOriginalImage().getHeight());
            state.setCells(cells);
            state.setGridComplete(true);

            statusLabel.setText(String.format("✅ %d celdas cargadas del archivo .json", cells.size()));
            redrawIfNeeded();

        } catch (Exception e) {
            log.error("Error cargando metadatos", e);
            statusLabel.setText("⚠ Error al leer .json: " + e.getMessage());
        }
    }

    /**
     * Verifica la grilla con los parámetros actuales del usuario.
     */
    private void verifyGrid() {
        BufferedImage image = state.getOriginalImage();
        if (image == null) {
            Dialogs.warn("Sin imagen", "Cargá una imagen primero.");
            return;
        }

        try {
            GridSpec spec = buildGridSpec();
            state.setGridSpec(spec);

            // Calcular celdas a partir de las medidas exactas
            List<Rectangle> cells = computeCells(spec,
                    image.getWidth(), image.getHeight());

            state.setCells(cells);
            state.setGridComplete(true);

            statusLabel.setText(String.format("✅ %d celdas calculadas (medidas exactas)", cells.size()));
            redrawIfNeeded();

        } catch (Exception e) {
            log.error("Error verificando grilla", e);
            statusLabel.setText("❌ Error: " + e.getMessage());
            Dialogs.error("Error de verificación", e.getMessage());
        }
    }

    /**
     * Calcula las celdas a partir de las medidas EXACTAS del GridSpec.
     *
     * <p>Convertimos mm a píxeles usando el DPI implícito del escaneo
     * (calculado como anchoPx / anchoMm).</p>
     */
    private List<Rectangle> computeCells(GridSpec spec, int imageWidth, int imageHeight) {
        // DPI efectivo del escaneo (asumiendo que el escaneo coincide con la página completa)
        double pxPerMmX = imageWidth / spec.pageWidthMm();
        double pxPerMmY = imageHeight / spec.pageHeightMm();

        log.debug("Escala del escaneo: {} px/mm X, {} px/mm Y", pxPerMmX, pxPerMmY);

        double marginPx = spec.marginMm() * pxPerMmX;
        double cellWpx = spec.cellWidthMm() * pxPerMmX;
        double cellHpx = spec.cellHeightMm() * pxPerMmY;
        double gutterPx = spec.gutterMm() * pxPerMmX;

        List<Rectangle> cells = new ArrayList<>(spec.totalCells());

        for (int row = 0; row < spec.rows(); row++) {
            for (int col = 0; col < spec.columns(); col++) {
                int x = (int) Math.round(marginPx + col * (cellWpx + gutterPx));
                int y = (int) Math.round(marginPx + row * (cellHpx + gutterPx));
                int w = (int) Math.round(cellWpx);
                int h = (int) Math.round(cellHpx);

                // Asegurar que no se salga de la imagen
                x = Math.max(0, Math.min(x, imageWidth - 1));
                y = Math.max(0, Math.min(y, imageHeight - 1));
                w = Math.min(w, imageWidth - x);
                h = Math.min(h, imageHeight - y);

                cells.add(new Rectangle(x, y, w, h));
            }
        }

        return cells;
    }

    private List<Rectangle> computeCellsFromMetadata(GridMetadata m,
                                                      int imageWidth, int imageHeight) {
        return computeCells(m.toGridSpec(), imageWidth, imageHeight);
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
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, w, h);

        double imgW = previewImage.getWidth();
        double imgH = previewImage.getHeight();
        double scale = Math.min((w - 40) / imgW, (h - 40) / imgH);

        double drawW = imgW * scale;
        double drawH = imgH * scale;
        double offsetX = (w - drawW) / 2;
        double offsetY = (h - drawH) / 2;

        gc.drawImage(previewImage, offsetX, offsetY, drawW, drawH);

        List<Rectangle> cells = state.getCells();
        if (cells != null && !cells.isEmpty()) {
            gc.setStroke(Color.rgb(45, 127, 249, 0.9));
            gc.setLineWidth(2);

            for (Rectangle cell : cells) {
                double x = offsetX + cell.x * scale;
                double y = offsetY + cell.y * scale;
                double cw = cell.width * scale;
                double ch = cell.height * scale;
                gc.strokeRect(x, y, cw, ch);
            }
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
