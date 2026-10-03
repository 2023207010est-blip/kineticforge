package io.kineticforge.ui.controller.wizard;

import io.kineticforge.core.alignment.FrameAligner;
import io.kineticforge.core.background.BackgroundRemover;
import io.kineticforge.core.background.ml.ModelDownloader;
import io.kineticforge.core.background.ml.U2NetBackgroundRemover;
import io.kineticforge.core.export.Exporter;
import io.kineticforge.core.export.FFmpegExporter;
import io.kineticforge.core.export.GifExporter;
import io.kineticforge.core.export.SpritesheetExporter;
import io.kineticforge.core.export.WebPExporter;
import io.kineticforge.core.frames.FrameExtractor;
import io.kineticforge.core.grid.CellCalculator;
import io.kineticforge.core.grid.GridAutoDetector;
import io.kineticforge.core.grid.GridDetectionResult;
import io.kineticforge.io.MetadataLoader;
import io.kineticforge.model.ExportConfig;
import io.kineticforge.model.GridMetadata;
import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import io.kineticforge.model.SheetScan;
import io.kineticforge.ui.model.ExportHistoryEntry;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ExportHistoryService;
import io.kineticforge.ui.util.ImageConverter;
import io.kineticforge.ui.util.Notifications;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.prefs.Preferences;

/**
 * Controlador unificado del workspace (paso 2).
 *
 * @author KineticForge Team
 * @version 3.0.1
 * @since 2026
 */
public class WorkspaceController implements WizardStepController {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    private static final int PREVIEW_MAX_SIDE = 1600;
    private static final int THUMBNAIL_SIZE = 100;
    private static final int GRID_COLUMNS = 8;
    private static final int EMPTY_FRAME_MAX_PIXELS = 40;
    private static final int MAX_ANIMATION_FRAMES = 60;

    private static final Preferences PREFS = Preferences.userNodeForPackage(WorkspaceController.class);
    private static final String PREF_LAST_FORMAT = "last_export_format";
    private static final String PREF_LAST_DIR = "last_export_dir";

    private enum Method {
        CLASSIC("Clásico (rápido, sin descargas)", null),
        AI_LIGHT("IA U2-Net ligero (5 MB)", ModelDownloader.Variant.LIGHT),
        AI_FULL("IA U2-Net preciso (176 MB)", ModelDownloader.Variant.FULL);

        private final String label;
        private final ModelDownloader.Variant variant;

        Method(String label, ModelDownloader.Variant variant) {
            this.label = label;
            this.variant = variant;
        }

        @Override public String toString() { return label; }
    }

    // ---- Grilla ----
    @FXML private TitledPane gridPane;
    @FXML private Label gridAutoLabel;
    @FXML private ListView<SheetScan> sheetList;
    @FXML private Button autoDetectButton;
    @FXML private CheckBox manualModeCheck;
    @FXML private VBox manualPanel;
    @FXML private ToggleButton gridModeToggle, singleModeToggle;
    @FXML private VBox gridControls;
    @FXML private ComboBox<GridPreset> presetCombo;
    @FXML private ToggleButton portraitToggle, landscapeToggle;
    @FXML private Slider marginSlider;
    @FXML private Label marginLabel;
    @FXML private CheckBox guideDotCheck;
    @FXML private Button applyCurrentButton, applyAllButton;
    @FXML private Label gridStatusLabel;

    // ---- Preview ----
    @FXML private ToggleButton viewGridToggle, viewFramesToggle;
    @FXML private StackPane gridPreviewPane;
    @FXML private Canvas previewCanvas;
    @FXML private ScrollPane framesScroll;
    @FXML private GridPane framesGrid;
    @FXML private ImageView animatedPreview;
    @FXML private Label framesCountLabel;

    // ---- Procesar ----
    @FXML private TitledPane processPane;
    @FXML private ComboBox<Method> methodCombo;
    @FXML private CheckBox skipEmptyCheck;
    @FXML private ProgressBar processProgressBar;
    @FXML private Label processStatusLabel;
    @FXML private Button processButton;

    // ---- Exportar ----
    @FXML private TitledPane exportPane;
    @FXML private ComboBox<String> formatCombo;
    @FXML private Slider fpsSlider;
    @FXML private Label fpsLabel;
    @FXML private CheckBox loopCheck;
    @FXML private Label exportSummaryLabel;
    @FXML private ProgressBar exportProgressBar;
    @FXML private Button exportButton;
    @FXML private Label exportStatusLabel;

    // ---- Historial ----
    @FXML private TitledPane historyPane;
    @FXML private ListView<ExportHistoryEntry> historyList;
    @FXML private Button openFolderButton, clearHistoryButton;

    // ---- Estado interno ----
    private WizardState state;
    private Image previewImage;
    private SheetScan currentSheet;
    private final MetadataLoader metadataLoader = new MetadataLoader();

    private Task<List<BufferedImage>> processingTask;
    private Task<Void> exportTask;

    // ---- Animación ----
    private List<Image> animationFrames;
    private Timeline animationTimeline;
    private int animationIndex = 0;

    @Override
    public void init(WizardState state, Runnable onComplete) {
        this.state = state;   // ⚠️ LÍNEA CRÍTICA

        setupGridControls();
        setupProcessControls();
        setupExportControls();
        setupHistory();
        setupViewToggle();
        setupCanvas();

        // Cargar metadatos .json si existen
        for (SheetScan sheet : state.getSheets()) {
            if (!sheet.hasGrid()) tryAutoLoadMetadata(sheet);
        }

        sheetList.setItems(state.getSheets());
        sheetList.getSelectionModel().selectedItemProperty()
            .addListener((obs, old, sel) -> onSheetSelected(sel));

        if (!state.getSheets().isEmpty()) {
            sheetList.getSelectionModel().select(0);
        }

        // Si ya hay frames procesados (al volver al paso), mostrarlos
        if (state.getProcessedFrames() != null && !state.getProcessedFrames().isEmpty()) {
            displayThumbnails(state.getProcessedFrames());
            processStatusLabel.setText("✅ " + state.getProcessedFrames().size() + " frames procesados");
            updateExportEnabled();
        } else {
            viewGridToggle.setSelected(true);
        }

        log.info("Workspace inicializado");
    }

    // ============================================================
    // Setup
    // ============================================================

    private void setupViewToggle() {
        viewGridToggle.selectedProperty().addListener((o, ov, sel) -> {
            gridPreviewPane.setVisible(sel);
            gridPreviewPane.setManaged(sel);
            if (sel) {
                stopAnimation();
                redrawGrid();
            }
        });

        viewFramesToggle.selectedProperty().addListener((o, ov, sel) -> {
            framesScroll.setVisible(sel);
            framesScroll.setManaged(sel);
            if (sel) {
                if (state != null && state.getProcessedFrames() != null
                    && !state.getProcessedFrames().isEmpty()) {
                    startAnimation();
                }
            } else {
                stopAnimation();
            }
        });
    }

    private void setupCanvas() {
        previewCanvas.setManaged(false);
        previewCanvas.widthProperty().bind(gridPreviewPane.widthProperty());
        previewCanvas.heightProperty().bind(gridPreviewPane.heightProperty());
        previewCanvas.widthProperty().addListener((o, ov, nv) -> redrawGrid());
        previewCanvas.heightProperty().addListener((o, ov, nv) -> redrawGrid());
        gridPreviewPane.sceneProperty().addListener((obs, o, n) -> {
            if (n != null) Platform.runLater(this::redrawGrid);
        });
    }

    private void setupGridControls() {
        autoDetectButton.setOnAction(e -> autoDetectAllSheets());

        manualModeCheck.selectedProperty().addListener((obs, ov, sel) -> {
            manualPanel.setVisible(sel);
            manualPanel.setManaged(sel);
        });

        presetCombo.getItems().setAll(GridPreset.values());
        presetCombo.getSelectionModel().select(GridPreset.ESTANDAR);
        presetCombo.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(GridPreset p) {
                return p == null ? "" : p.getDisplayName() + " (" + p.getTotalCells() + " celdas)";
            }
            @Override public GridPreset fromString(String s) { return null; }
        });

        marginSlider.setValue(13);
        marginLabel.setText("Margen: 13 mm");
        marginSlider.valueProperty().addListener((obs, o, n) ->
            marginLabel.setText(String.format("Margen: %.0f mm", n.doubleValue())));

        singleModeToggle.selectedProperty().addListener((obs, o, single) ->
            gridControls.setDisable(single));

        applyCurrentButton.setOnAction(e -> applyToCurrent());
        applyAllButton.setOnAction(e -> applyToAll());
    }

    private void setupProcessControls() {
        methodCombo.getItems().setAll(Method.values());
        methodCombo.getSelectionModel().select(Method.CLASSIC);
        processButton.setOnAction(e -> startProcessing());
    }

    private void setupExportControls() {
        fpsSlider.setValue(24);
        fpsLabel.setText("FPS: 24");

        formatCombo.getItems().setAll(
            "GIF animado", "PNG spritesheet", "WebP (primer frame)",
            "MP4 (H.264)", "WebM (VP9)", "GIF (FFmpeg)");
        String saved = PREFS.get(PREF_LAST_FORMAT, "GIF animado");
        formatCombo.getSelectionModel().select(
            formatCombo.getItems().contains(saved) ? saved : "GIF animado");
        formatCombo.getSelectionModel().selectedItemProperty().addListener((obs, o, n) -> {
            if (n != null) PREFS.put(PREF_LAST_FORMAT, n);
        });

        fpsSlider.valueProperty().addListener((obs, o, n) -> {
            fpsLabel.setText("FPS: " + n.intValue());
            updateExportSummary();
            if (viewFramesToggle.isSelected()
                && state != null
                && state.getProcessedFrames() != null
                && !state.getProcessedFrames().isEmpty()) {
                startAnimation();
            }
        });
        loopCheck.selectedProperty().addListener((o, ov, nv) -> updateExportSummary());
        exportButton.setOnAction(e -> export());
    }

    private void setupHistory() {
        historyList.setItems(FXCollections.observableArrayList(ExportHistoryService.load()));
        historyList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(ExportHistoryEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.toString());
            }
        });
        openFolderButton.setOnAction(e -> openSelectedInFolder());
        clearHistoryButton.setOnAction(e -> {
            ExportHistoryService.clear();
            historyList.getItems().clear();
            Notifications.info("Historial limpiado");
        });
    }

    // ============================================================
    // Detección automática
    // ============================================================

    private void autoDetectAllSheets() {
        if (state == null) return;
        if (state.getSheets().isEmpty()) {
            Notifications.warn("No hay hojas cargadas");
            return;
        }

        int detected = 0, failed = 0;

        for (SheetScan sheet : state.getSheets()) {
            try {
                GridDetectionResult result = GridAutoDetector.detect(sheet.getImage());
                if (result != null && result.isReliable()) {
                    sheet.setMetadata(null);
                    sheet.setGrid(result.spec(), result.cells());
                    detected++;
                    log.info("Grilla detectada en {}: {} celdas",
                        sheet.getFileName(), result.cellCount());
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                log.error("Error detectando grilla en {}", sheet.getFileName(), ex);
                failed++;
            }
        }

        sheetList.refresh();
        state.invalidateProcessing();
        updateExportEnabled();
        markCompleted(gridPane, state.allSheetsHaveGrid());

        if (currentSheet != null) {
            gridStatusLabel.setText(describeStatus(currentSheet));
        }

        if (failed == 0) {
            Notifications.success("Grilla detectada en " + detected + " hoja(s)");
        } else if (detected > 0) {
            Notifications.warn(detected + " detectadas, " + failed + " fallaron. Usá el modo manual.");
            manualModeCheck.setSelected(true);
        } else {
            Notifications.error("No se pudo detectar grilla. Activá el modo manual.");
            manualModeCheck.setSelected(true);
        }
    }

    // ============================================================
    // Grilla manual
    // ============================================================

    private void onSheetSelected(SheetScan sheet) {
        currentSheet = sheet;
        if (sheet == null) {
            previewImage = null;
            redrawGrid();
            return;
        }
        previewImage = ImageConverter.toFxImage(
            ImageConverter.scaleToFit(sheet.getImage(), PREVIEW_MAX_SIDE));
        syncControlsFromSheet(sheet);
        gridStatusLabel.setText(describeStatus(sheet));
        redrawGrid();
    }

    private void syncControlsFromSheet(SheetScan sheet) {
        if (sheet.isWholeImage()) {
            singleModeToggle.setSelected(true);
            gridControls.setDisable(true);
        } else {
            gridModeToggle.setSelected(true);
            GridSpec spec = sheet.getSpec();
            if (spec != null) {
                presetCombo.getSelectionModel().select(spec.preset());
                if (spec.orientation() == PageOrientation.PORTRAIT)
                    portraitToggle.setSelected(true);
                else
                    landscapeToggle.setSelected(true);
                marginSlider.setValue(spec.marginMm());
                guideDotCheck.setSelected(spec.includeGuideDot());
            }
            gridControls.setDisable(sheet.getMetadata() != null);
        }
    }

    private String describeStatus(SheetScan sheet) {
        if (!sheet.hasGrid()) return "⚠ Esta hoja no tiene grilla asociada.";
        if (sheet.isWholeImage()) return "✅ Imagen suelta (1 frame completo).";
        if (sheet.getMetadata() != null)
            return "✅ Grilla cargada automáticamente desde la plantilla oficial (.json).";
        return "✅ " + sheet.frameCount() + " celdas configuradas.";
    }

    private void tryAutoLoadMetadata(SheetScan sheet) {
        try {
            Optional<GridMetadata> meta = metadataLoader.loadFor(sheet.getPath());
            if (meta.isEmpty()) return;
            GridMetadata m = meta.get();
            GridSpec spec = m.toGridSpec();
            List<Rectangle> cells = CellCalculator.compute(spec,
                sheet.getImage().getWidth(), sheet.getImage().getHeight());
            sheet.setMetadata(m);
            sheet.setGrid(spec, cells);
        } catch (Exception e) {
            log.warn("No se pudo leer .json de {}: {}", sheet.getFileName(), e.getMessage());
        }
    }

    private void applyToCurrent() {
        if (currentSheet == null || state == null) return;
        try {
            applySettings(currentSheet);
            sheetList.refresh();
            gridStatusLabel.setText(describeStatus(currentSheet));
            redrawGrid();
            markCompleted(gridPane, state.allSheetsHaveGrid());
        } catch (Exception e) {
            gridStatusLabel.setText("❌ Error: " + e.getMessage());
        }
    }

    private void applyToAll() {
        if (state == null) return;
        try {
            for (SheetScan s : state.getSheets()) applySettings(s);
            sheetList.refresh();
            if (currentSheet != null) gridStatusLabel.setText(describeStatus(currentSheet));
            redrawGrid();
            markCompleted(gridPane, state.allSheetsHaveGrid());
        } catch (Exception e) {
            Dialogs.error("Error", e.getMessage());
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
        updateExportEnabled();
    }

    private GridSpec buildGridSpec() {
        GridPreset preset = presetCombo.getSelectionModel().getSelectedItem();
        PageOrientation orientation = portraitToggle.isSelected()
            ? PageOrientation.PORTRAIT : PageOrientation.LANDSCAPE;
        return GridSpec.builder()
            .preset(preset).orientation(orientation).pageSize(PageSize.A4)
            .marginMm(marginSlider.getValue()).gutterMm(2.0)
            .includeGuideDot(guideDotCheck.isSelected())
            .build();
    }

    private void redrawGrid() {
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

        double drawW = imgW * scale, drawH = imgH * scale;
        double offsetX = (w - drawW) / 2, offsetY = (h - drawH) / 2;
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

    // ============================================================
    // Procesar
    // ============================================================

    private void startProcessing() {
        if (state == null) return;
        if (!state.allSheetsHaveGrid()) {
            Notifications.warn("Todas las hojas necesitan grilla");
            gridPane.setExpanded(true);
            return;
        }
        final List<SheetScan> sheets = List.copyOf(state.getSheets());
        final Method method = methodCombo.getValue() == null ? Method.CLASSIC : methodCombo.getValue();
        final boolean skipEmpty = skipEmptyCheck.isSelected();

        processButton.setDisable(true);
        methodCombo.setDisable(true);
        framesGrid.getChildren().clear();
        stopAnimation();

        processingTask = new Task<>() {
            @Override protected List<BufferedImage> call() throws Exception {
                updateMessage("Extrayendo frames...");
                updateProgress(0.0, 1.0);

                List<BufferedImage> extracted = new ArrayList<>();
                for (SheetScan sheet : sheets) {
                    int margin = sheet.isWholeImage() ? 0 : FrameExtractor.DEFAULT_INNER_MARGIN_PX;
                    extracted.addAll(new FrameExtractor(margin).extract(sheet.getImage(), sheet.getCells()));
                }
                final int total = extracted.size();
                updateProgress(0.10, 1.0);

                U2NetBackgroundRemover ai = null;
                if (method.variant != null) {
                    try {
                        updateMessage("Preparando modelo IA...");
                        Path model = ModelDownloader.ensureModel(method.variant,
                            p -> updateMessage(String.format("Descargando: %d%%", (int)(p*100))));
                        ai = U2NetBackgroundRemover.shared(model);
                    } catch (Exception ex) {
                        log.warn("IA no disponible", ex);
                    }
                }
                final U2NetBackgroundRemover aiR = ai;
                final BackgroundRemover classic = new BackgroundRemover();

                int cores = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 4));
                ExecutorService exec = Executors.newFixedThreadPool(cores);
                List<BufferedImage> cleaned = new ArrayList<>(Collections.nCopies(total, null));
                AtomicInteger done = new AtomicInteger();

                try {
                    List<Future<?>> futures = new ArrayList<>(total);
                    for (int i = 0; i < total; i++) {
                        final int idx = i;
                        final BufferedImage frame = extracted.get(i);
                        futures.add(exec.submit(() -> {
                            BufferedImage r = aiR != null ? aiR.removeBackground(frame) : classic.removeBackground(frame);
                            cleaned.set(idx, r);
                            int n = done.incrementAndGet();
                            updateProgress(0.10 + 0.75 * n / (double) total, 1.0);
                            updateMessage("Quitando fondo: " + n + " / " + total);
                        }));
                    }
                    for (Future<?> f : futures) f.get();
                } finally {
                    exec.shutdown();
                }

                List<BufferedImage> kept = new ArrayList<>();
                for (BufferedImage img : cleaned) if (!skipEmpty || !isEmpty(img)) kept.add(img);
                if (kept.isEmpty()) throw new IllegalStateException("Todas las celdas están vacías.");

                updateMessage("Alineando...");
                updateProgress(0.90, 1.0);
                List<BufferedImage> aligned = new FrameAligner().align(kept);
                updateProgress(1.0, 1.0);
                updateMessage("✅ " + aligned.size() + " frames listos");
                return aligned;
            }
        };

        processStatusLabel.textProperty().bind(processingTask.messageProperty());
        processProgressBar.progressProperty().bind(processingTask.progressProperty());

        processingTask.setOnSucceeded(e -> {
            processStatusLabel.textProperty().unbind();
            processProgressBar.progressProperty().unbind();
            String msg = processingTask.getMessage();
            List<BufferedImage> frames = processingTask.getValue();
            state.setProcessedFrames(frames);
            state.setProcessComplete(true);
            displayThumbnails(frames);
            processStatusLabel.setText(msg);
            processProgressBar.setProgress(1.0);
            processButton.setDisable(false);
            methodCombo.setDisable(false);
            updateExportEnabled();

            viewFramesToggle.setSelected(true);
            Notifications.success("Frames procesados: " + frames.size());
            exportPane.setExpanded(true);
        });

        processingTask.setOnFailed(e -> {
            processStatusLabel.textProperty().unbind();
            processProgressBar.progressProperty().unbind();
            Throwable ex = processingTask.getException();
            log.error("Error procesando", ex);
            processStatusLabel.setText("❌ Error: " + (ex != null ? ex.getMessage() : "?"));
            processProgressBar.setProgress(0);
            processButton.setDisable(false);
            methodCombo.setDisable(false);
            Notifications.error("Error al procesar: " + (ex != null ? ex.getMessage() : "?"));
        });

        Thread t = new Thread(processingTask, "frame-processor");
        t.setDaemon(true);
        t.start();
    }

    private static boolean isEmpty(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int visible = 0;
        for (int p : px) if (((p >>> 24) & 0xFF) > 128 && ++visible > EMPTY_FRAME_MAX_PIXELS) return false;
        return true;
    }

    private void displayThumbnails(List<BufferedImage> frames) {
        framesGrid.getChildren().clear();
        for (int i = 0; i < frames.size(); i++) {
            framesGrid.add(createThumbnail(frames.get(i), i), i % GRID_COLUMNS, i / GRID_COLUMNS);
        }
        if (framesCountLabel != null) {
            framesCountLabel.setText(frames.size() + " frames");
        }
    }

    private VBox createThumbnail(BufferedImage frame, int index) {
        Image img = ImageConverter.toFxImage(ImageConverter.scaleToFit(frame, THUMBNAIL_SIZE * 2));
        ImageView iv = new ImageView(img);
        iv.setFitWidth(THUMBNAIL_SIZE);
        iv.setFitHeight(THUMBNAIL_SIZE);
        iv.setPreserveRatio(true);
        Label lbl = new Label(String.valueOf(index + 1));
        lbl.getStyleClass().add("thumbnail-label");
        VBox box = new VBox(2, iv, lbl);
        box.setAlignment(Pos.CENTER);
        box.getStyleClass().add("thumbnail-cell");
        return box;
    }

    // ============================================================
    // Animación
    // ============================================================

    private void startAnimation() {
        if (state == null || state.getProcessedFrames() == null
            || state.getProcessedFrames().isEmpty()
            || animatedPreview == null) {
            return;
        }

        stopAnimation();

        List<BufferedImage> frames = state.getProcessedFrames();
        int step = Math.max(1, frames.size() / MAX_ANIMATION_FRAMES);

        animationFrames = new ArrayList<>();
        for (int i = 0; i < frames.size(); i += step) {
            animationFrames.add(ImageConverter.toFxImage(frames.get(i)));
        }

        if (animationFrames.isEmpty()) return;

        animatedPreview.setImage(animationFrames.get(0));
        animationIndex = 0;

        int fps = (int) fpsSlider.getValue();
        double durationMs = 1000.0 / Math.max(1, fps);

        animationTimeline = new Timeline(new KeyFrame(
            Duration.millis(durationMs),
            e -> {
                animationIndex = (animationIndex + 1) % animationFrames.size();
                animatedPreview.setImage(animationFrames.get(animationIndex));
            }
        ));
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();
    }

    private void stopAnimation() {
        if (animationTimeline != null) {
            animationTimeline.stop();
            animationTimeline = null;
        }
    }

    // ============================================================
    // Exportar
    // ============================================================

    private void updateExportEnabled() {
        if (state == null) return;
        boolean ready = state.getProcessedFrames() != null && !state.getProcessedFrames().isEmpty();
        exportButton.setDisable(!ready);
        updateExportSummary();
        markCompleted(processPane, ready);
        markCompleted(exportPane, ready);
    }

    private void markCompleted(TitledPane pane, boolean completed) {
        if (pane == null) return;
        if (completed && !pane.getStyleClass().contains("completed")) {
            pane.getStyleClass().add("completed");
        } else if (!completed) {
            pane.getStyleClass().remove("completed");
        }
    }

    private void updateExportSummary() {
        if (state == null) return;
        List<BufferedImage> frames = state.getProcessedFrames();
        if (frames == null || frames.isEmpty()) {
            exportSummaryLabel.setText("Sin frames");
            return;
        }
        int fps = (int) fpsSlider.getValue();
        double seconds = frames.size() / (double) fps;
        exportSummaryLabel.setText(String.format(
            "• %d frames\n• %d fps\n• %.1f s\n• %s",
            frames.size(), fps, seconds,
            loopCheck.isSelected() ? "loop" : "una vez"));
    }

    private void export() {
        if (state == null) return;
        List<BufferedImage> frames = state.getProcessedFrames();
        if (frames == null || frames.isEmpty()) {
            Notifications.warn("No hay frames para exportar");
            return;
        }
        String fmt = formatCombo.getSelectionModel().getSelectedItem();
        Exporter exporter = pickExporter(fmt);
        if (exporter == null) { Notifications.error("Formato no soportado"); return; }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar animación");
        chooser.setInitialFileName("animacion." + exporter.getFileExtension());
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
            exporter.getFormatName(), "*." + exporter.getFileExtension()));

        String lastDir = PREFS.get(PREF_LAST_DIR, null);
        if (lastDir != null && new File(lastDir).isDirectory()) {
            chooser.setInitialDirectory(new File(lastDir));
        } else {
            Path def = Path.of(System.getProperty("user.home"), "kineticforge", "exportaciones");
            if (def.toFile().exists()) chooser.setInitialDirectory(def.toFile());
        }

        File target = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (target == null) return;
        PREFS.put(PREF_LAST_DIR, target.getParent());

        ExportConfig config = ExportConfig.defaults()
            .withFps((int) fpsSlider.getValue())
            .withLoop(loopCheck.isSelected());

        Path output = target.toPath();
        final int fps = (int) fpsSlider.getValue();

        exportButton.setDisable(true);

        exportTask = new Task<>() {
            @Override protected Void call() throws Exception {
                updateMessage("Exportando…");
                updateProgress(0, 1);
                exporter.export(frames, config, output);
                updateProgress(1, 1);
                updateMessage("✅ Exportado: " + output.getFileName());
                return null;
            }
        };

        exportStatusLabel.textProperty().bind(exportTask.messageProperty());
        exportProgressBar.progressProperty().bind(exportTask.progressProperty());

        exportTask.setOnSucceeded(e -> {
            exportStatusLabel.textProperty().unbind();
            exportProgressBar.progressProperty().unbind();
            exportProgressBar.setProgress(0);
            exportButton.setDisable(false);

            ExportHistoryEntry entry = new ExportHistoryEntry(
                output.getFileName().toString(),
                output.toString(),
                fmt, frames.size(), fps, System.currentTimeMillis());
            ExportHistoryService.add(entry);
            historyList.getItems().add(0, entry);

            Notifications.success("Exportado: " + output.getFileName());
            exportStatusLabel.setText("✅ " + output.getFileName());
        });

        exportTask.setOnFailed(e -> {
            exportStatusLabel.textProperty().unbind();
            exportProgressBar.progressProperty().unbind();
            exportProgressBar.setProgress(0);
            exportButton.setDisable(false);
            Throwable ex = exportTask.getException();
            log.error("Error exportando", ex);
            exportStatusLabel.setText("❌ " + (ex != null ? ex.getMessage() : "?"));
            Notifications.error("Error al exportar");
        });

        Thread t = new Thread(exportTask, "exporter");
        t.setDaemon(true);
        t.start();
    }

    private void openSelectedInFolder() {
        ExportHistoryEntry sel = historyList.getSelectionModel().getSelectedItem();
        if (sel == null) {
            File dir = new File(System.getProperty("user.home"), "kineticforge/exportaciones");
            if (dir.exists()) Notifications.info("Carpeta: " + dir);
            return;
        }
        File f = new File(sel.fullPath());
        if (f.exists()) Notifications.info("Archivo: " + f.getName());
        else Notifications.warn("Ya no existe: " + sel.fileName());
    }

    private Exporter pickExporter(String name) {
        if (name == null) return null;
        return switch (name) {
            case "GIF animado" -> new GifExporter();
            case "PNG spritesheet" -> new SpritesheetExporter();
            case "WebP (primer frame)" -> new WebPExporter();
            case "MP4 (H.264)" -> new FFmpegExporter(FFmpegExporter.Format.MP4);
            case "WebM (VP9)" -> new FFmpegExporter(FFmpegExporter.Format.WEBM);
            case "GIF (FFmpeg)" -> new FFmpegExporter(FFmpegExporter.Format.GIF);
            default -> null;
        };
    }

    // ============================================================
    // Cleanup
    // ============================================================

    @Override
    public void cleanup() {
        if (processingTask != null && processingTask.isRunning()) processingTask.cancel(true);
        if (exportTask != null && exportTask.isRunning()) exportTask.cancel(true);
        stopAnimation();
        processingTask = null;
        exportTask = null;
        animationFrames = null;
        previewImage = null;
        state = null;
        log.debug("Workspace cleanup");
    }
}
