package io.kineticforge.ui.controller.wizard;

import io.kineticforge.core.alignment.FrameAligner;
import io.kineticforge.core.background.BackgroundRemover;
import io.kineticforge.core.background.ml.ModelDownloader;
import io.kineticforge.core.background.ml.U2NetBackgroundRemover;
import io.kineticforge.core.frames.FrameExtractor;
import io.kineticforge.model.SheetScan;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Controlador del paso 3: procesamiento de frames.
 *
 * <p>Toma los frames de TODAS las hojas, en el orden de la lista, y los
 * procesa como una única progresión: extracción → quitafondos → descartar
 * celdas vacías → alineación común.</p>
 *
 * @author KineticForge Team
 * @version 3.0.1
 * @since 2026
 */
public class Step3ProcessController {

    private static final Logger log = LoggerFactory.getLogger(Step3ProcessController.class);

    private static final int THUMBNAIL_SIZE = 100;
    private static final int GRID_COLUMNS = 8;
    /** Un frame con menos píxeles visibles que esto se considera vacío. */
    private static final int EMPTY_FRAME_MAX_PIXELS = 40;

    /** Métodos de quitafondos disponibles. */
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

        @Override
        public String toString() { return label; }
    }

    @FXML private StackPane previewContainer;
    @FXML private GridPane framesGrid;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Button processButton;
    @FXML private Button nextButton;
    // Opcional: Si agregas un botón rápido en el fxml de step3, lo inyectas aquí.
    // @FXML private Button quickExportButton;
    @FXML private ComboBox<Method> methodCombo;
    @FXML private CheckBox skipEmptyCheck;

    private WizardState state;
    private Runnable onComplete;
    private List<BufferedImage> processedFrames;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        methodCombo.getItems().setAll(Method.values());
        methodCombo.getSelectionModel().select(Method.CLASSIC);

        processButton.setOnAction(e -> startProcessing());
        nextButton.setOnAction(e -> {
            if (onComplete != null) onComplete.run();
        });

        int total = state.totalCells();
        if (state.getProcessedFrames() != null && !state.getProcessedFrames().isEmpty()) {
            this.processedFrames = state.getProcessedFrames();
            displayThumbnails();
            nextButton.setDisable(false);
            progressLabel.setText("✅ " + processedFrames.size() + " frames procesados");
        } else if (total > 0) {
            progressLabel.setText(String.format(
                "%d celda(s) en %d hoja(s). Presioná «Procesar» para comenzar.",
                total, state.getSheets().size()));
        }

        log.info("Paso 3 inicializado con optimización de flujo");
    }

    private void startProcessing() {
        if (!state.allSheetsHaveGrid()) {
            Dialogs.warn("Faltan datos", "Todas las hojas necesitan una grilla (paso 2).");
            return;
        }

        final List<SheetScan> sheets = List.copyOf(state.getSheets());
        final Method method = methodCombo.getValue() == null ? Method.CLASSIC : methodCombo.getValue();
        final boolean skipEmpty = skipEmptyCheck.isSelected();

        processButton.setDisable(true);
        nextButton.setDisable(true);
        methodCombo.setDisable(true);
        framesGrid.getChildren().clear();

        Task<List<BufferedImage>> task = new Task<>() {
            @Override
            protected List<BufferedImage> call() throws Exception {
                updateMessage("Extrayendo frames...");
                updateProgress(0.0, 1.0);

                List<BufferedImage> extracted = new ArrayList<>();
                for (SheetScan sheet : sheets) {
                    int margin = sheet.isWholeImage() ? 0 : FrameExtractor.DEFAULT_INNER_MARGIN_PX;
                    extracted.addAll(new FrameExtractor(margin)
                        .extract(sheet.getImage(), sheet.getCells()));
                }
                final int total = extracted.size();
                updateProgress(0.10, 1.0);

                U2NetBackgroundRemover ai = null;
                String fallbackNote = null;

                if (method.variant != null) {
                    try {
                        updateMessage("Preparando modelo de IA...");
                        Path model = ModelDownloader.ensureModel(method.variant, p ->
                            updateMessage(String.format("Descargando modelo: %d%%", (int) (p * 100))));
                        updateMessage("Cargando modelo de IA...");
                        ai = U2NetBackgroundRemover.shared(model);
                    } catch (Exception ex) {
                        log.error("No se pudo preparar la IA; se usa el método clásico", ex);
                        fallbackNote = "IA no disponible (" + ex.getMessage() + "), se usó el método clásico";
                    }
                }
                final U2NetBackgroundRemover aiRemover = ai;
                final BackgroundRemover classic = new BackgroundRemover();

                int cores = Math.max(1, Math.min(Runtime.getRuntime().availableProcessors(), 4));
                updateMessage("Quitando fondo con " + cores + " hilos...");

                ExecutorService executor = Executors.newFixedThreadPool(cores);
                List<BufferedImage> cleaned = new ArrayList<>(Collections.nCopies(total, null));
                AtomicInteger done = new AtomicInteger();

                try {
                    List<Future<?>> futures = new ArrayList<>(total);
                    for (int i = 0; i < total; i++) {
                        final int index = i;
                        final BufferedImage frame = extracted.get(i);
                        futures.add(executor.submit(() -> {
                            BufferedImage result = aiRemover != null
                                ? aiRemover.removeBackground(frame)
                                : classic.removeBackground(frame);
                            cleaned.set(index, result);

                            int n = done.incrementAndGet();
                            updateProgress(0.10 + 0.75 * n / (double) total, 1.0);
                            updateMessage("Quitando fondo: " + n + " de " + total);
                        }));
                    }
                    for (Future<?> f : futures) {
                        f.get();
                    }
                } finally {
                    executor.shutdown();
                }

                List<BufferedImage> kept = new ArrayList<>(cleaned.size());
                for (BufferedImage img : cleaned) {
                    if (!skipEmpty || !isEmpty(img)) {
                        kept.add(img);
                    }
                }
                int skipped = cleaned.size() - kept.size();
                if (kept.isEmpty()) {
                    throw new IllegalStateException("Todas las celdas están vacías. Revisá la grilla o el escaneo.");
                }

                updateMessage("Alineando frames...");
                updateProgress(0.90, 1.0);
                List<BufferedImage> aligned = new FrameAligner().align(kept);

                updateProgress(1.0, 1.0);
                StringBuilder msg = new StringBuilder("✅ ").append(aligned.size()).append(" frames");
                if (skipped > 0) msg.append(" (").append(skipped).append(" celdas vacías omitidas)");
                if (fallbackNote != null) msg.append(" — ").append(fallbackNote);
                updateMessage(msg.toString());

                return aligned;
            }
        };

        progressLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            String finalMessage = task.getMessage();
            progressLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();

            processedFrames = task.getValue();
            state.setProcessedFrames(processedFrames);
            state.setProcessComplete(true);

            displayThumbnails();
            progressLabel.setText(finalMessage);
            progressBar.setProgress(1.0);
            nextButton.setDisable(false);
            processButton.setDisable(false);
            methodCombo.setDisable(false);

            // 🚀 MEJORA DE FLUJO: Auto-avanzar o permitir un salto rápido si el usuario lo prefiere
            // Aquí habilitamos el botón y opcionalmente podemos disparar un evento de guardado automático
            log.info("Procesamiento finalizado con éxito. Listo para exportar.");
        });

        task.setOnFailed(e -> {
            progressLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();

            Throwable ex = task.getException();
            log.error("Error en el procesamiento", ex);
            progressLabel.setText("❌ Error: " + ex.getMessage());
            progressBar.setProgress(0.0);
            processButton.setDisable(false);
            methodCombo.setDisable(false);
            Dialogs.error("Error al procesar", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        });

        Thread thread = new Thread(task, "frame-processor");
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean isEmpty(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int[] px = img.getRGB(0, 0, w, h, null, 0, w);
        int visible = 0;
        for (int p : px) {
            if (((p >>> 24) & 0xFF) > 128 && ++visible > EMPTY_FRAME_MAX_PIXELS) {
                return false;
            }
        }
        return true;
    }

    private void displayThumbnails() {
        framesGrid.getChildren().clear();
        if (processedFrames == null || processedFrames.isEmpty()) return;

        for (int i = 0; i < processedFrames.size(); i++) {
            VBox thumb = createThumbnail(processedFrames.get(i), i);
            framesGrid.add(thumb, i % GRID_COLUMNS, i / GRID_COLUMNS);
        }
    }

    private VBox createThumbnail(BufferedImage frame, int index) {
        Image fxImage = ImageConverter.toFxImage(
            ImageConverter.scaleToFit(frame, THUMBNAIL_SIZE * 2));
        ImageView imageView = new ImageView(fxImage);
        imageView.setFitWidth(THUMBNAIL_SIZE);
        imageView.setFitHeight(THUMBNAIL_SIZE);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);

        Label numberLabel = new Label(String.valueOf(index + 1));
        numberLabel.getStyleClass().add("thumbnail-label");

        VBox container = new VBox(2, imageView, numberLabel);
        container.setAlignment(Pos.CENTER);
        container.getStyleClass().add("thumbnail-cell");
        return container;
    }
}
