package io.kineticforge.ui.controller.wizard;

import io.kineticforge.core.alignment.FrameAligner;
import io.kineticforge.core.background.BackgroundRemover;
import io.kineticforge.core.frames.FrameExtractor;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
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
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class Step3ProcessController {

    private static final Logger log = LoggerFactory.getLogger(Step3ProcessController.class);

    private static final int THUMBNAIL_SIZE = 100;
    private static final int GRID_COLUMNS = 8;

    @FXML private StackPane previewContainer;
    @FXML private GridPane framesGrid;
    @FXML private ProgressBar progressBar;
    @FXML private Label progressLabel;
    @FXML private Button processButton;
    @FXML private Button nextButton;

    private WizardState state;
    private Runnable onComplete;
    private List<BufferedImage> processedFrames;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        setupButtons();

        if (state.getProcessedFrames() != null && !state.getProcessedFrames().isEmpty()) {
            this.processedFrames = state.getProcessedFrames();
            displayThumbnails();
            nextButton.setDisable(false);
            progressLabel.setText("✅ " + processedFrames.size() + " frames procesados");
        }

        log.info("Paso 3 inicializado");
    }

    private void setupButtons() {
        processButton.setOnAction(e -> startProcessing());
        nextButton.setOnAction(e -> {
            if (onComplete != null) onComplete.run();
        });
    }

    private void startProcessing() {
        List<Rectangle> cells = state.getCells();
        BufferedImage originalImage = state.getOriginalImage();

        if (cells == null || cells.isEmpty() || originalImage == null) {
            Dialogs.warn("Faltan datos", "Primero completá los pasos 1 y 2.");
            return;
        }

        processButton.setDisable(true);
        nextButton.setDisable(true);
        framesGrid.getChildren().clear();

        Task<List<BufferedImage>> task = new Task<>() {
            @Override
            protected List<BufferedImage> call() throws Exception {
                int total = cells.size();
                log.info("Pipeline iniciado: {} frames", total);

                // ============================================================
                // PASO 1: Extraer frames
                // ============================================================
                updateMessage("Extrayendo frames...");
                updateProgress(0.0, 1.0);

                FrameExtractor extractor = new FrameExtractor();
                List<BufferedImage> extracted = extractor.extract(originalImage, cells);

                updateProgress(0.15, 1.0);
                log.info("Extracción completa: {} frames", extracted.size());

                // ============================================================
                // PASO 2: Quitar fondo EN PARALELO
                // ============================================================
                int cores = Math.min(Runtime.getRuntime().availableProcessors(), 4);
                log.info("Quitando fondo en paralelo con {} threads", cores);

                updateMessage("Quitando fondo en paralelo con " + cores + " threads...");

                ExecutorService executor = Executors.newFixedThreadPool(cores);
                List<BufferedImage> noBackground = new ArrayList<>(Collections.nCopies(total, null));
                AtomicInteger completed = new AtomicInteger(0);

                try {
                    List<Future<?>> futures = new ArrayList<>(total);

                    for (int i = 0; i < extracted.size(); i++) {
                        final int index = i;
                        final BufferedImage frame = extracted.get(i);

                        futures.add(executor.submit(() -> {
                            // Cada thread tiene su propio removedor (thread-safe)
                            BackgroundRemover remover = new BackgroundRemover();
                            BufferedImage result = remover.removeBackground(frame);
                            noBackground.set(index, result);

                            int done = completed.incrementAndGet();
                            double progress = 0.15 + 0.7 * done / (double) total;
                            updateProgress(progress, 1.0);
                            updateMessage("Quitando fondo: " + done + " de " + total);
                        }));
                    }

                    // Esperar a que terminen todos
                    for (Future<?> f : futures) {
                        f.get();
                    }
                } finally {
                    executor.shutdown();
                }

                // ============================================================
                // PASO 3: Alinear
                // ============================================================
                updateMessage("Alineando frames...");
                updateProgress(0.85, 1.0);

                FrameAligner aligner = new FrameAligner();
                List<BufferedImage> aligned = aligner.align(noBackground);

                updateProgress(1.0, 1.0);
                updateMessage("✅ Procesamiento completo");

                log.info("Pipeline completo: {} frames alineados", aligned.size());
                return aligned;
            }
        };

        progressLabel.textProperty().bind(task.messageProperty());
        progressBar.progressProperty().bind(task.progressProperty());

        task.setOnSucceeded(e -> {
            progressLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();

            processedFrames = task.getValue();
            state.setProcessedFrames(processedFrames);
            state.setProcessComplete(true);

            displayThumbnails();
            progressLabel.setText("✅ " + processedFrames.size() + " frames procesados");
            progressBar.setProgress(1.0);
            nextButton.setDisable(false);
            processButton.setDisable(false);

            log.info("Procesamiento completo: {} frames", processedFrames.size());
        });

        task.setOnFailed(e -> {
            progressLabel.textProperty().unbind();
            progressBar.progressProperty().unbind();

            Throwable ex = task.getException();
            log.error("Error en el procesamiento", ex);
            progressLabel.setText("❌ Error: " + ex.getMessage());
            progressBar.setProgress(0.0);
            processButton.setDisable(false);
            Dialogs.error("Error al procesar",
                    ex.getMessage() != null ? ex.getMessage() : ex.toString());
        });

        Thread thread = new Thread(task, "frame-processor");
        thread.setDaemon(true);
        thread.start();
    }

    private void displayThumbnails() {
        framesGrid.getChildren().clear();
        if (processedFrames == null || processedFrames.isEmpty()) return;

        for (int i = 0; i < processedFrames.size(); i++) {
            int row = i / GRID_COLUMNS;
            int col = i % GRID_COLUMNS;
            VBox thumb = createThumbnail(processedFrames.get(i), i);
            framesGrid.add(thumb, col, row);
        }
    }

    private VBox createThumbnail(BufferedImage frame, int index) {
        Image fxImage = ImageConverter.toFxImage(frame);
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
