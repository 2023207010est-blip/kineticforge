package io.kineticforge.ui.controller.wizard;

import io.kineticforge.core.export.Exporter;
import io.kineticforge.core.export.FFmpegExporter;
import io.kineticforge.core.export.GifExporter;
import io.kineticforge.core.export.SpritesheetExporter;
import io.kineticforge.core.export.WebPExporter;
import io.kineticforge.model.ExportConfig;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Controlador del paso 4: exportación.
 *
 * @author KineticForge Team
 * @version 1.1.0
 * @since 2026
 */
public class Step4ExportController {

    private static final Logger log = LoggerFactory.getLogger(Step4ExportController.class);

    /** Referencia estática para detener la animación al salir del paso. */
    private static Step4ExportController currentInstance;

    @FXML private StackPane previewContainer;
    @FXML private ImageView animatedPreview;
    @FXML private ComboBox<String> formatCombo;
    @FXML private Slider fpsSlider;
    @FXML private Label fpsLabel;
    @FXML private CheckBox loopCheck;
    @FXML private Label summaryLabel;
    @FXML private Button exportButton;
    @FXML private Label statusLabel;

    private WizardState state;
    private Runnable onComplete;
    private List<Image> previewImages;
    private Timeline animationTimeline;
    private int currentFrameIndex = 0;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        // Registrar instancia actual
        currentInstance = this;

        setupControls();
        preparePreview();
        startAnimation();

        log.info("Paso 4 inicializado");
    }

    /**
     * Detiene la animación de la instancia actual.
     * Llamado por WizardController al cambiar de paso.
     */
    public static void stopInstance() {
        if (currentInstance != null) {
            currentInstance.stop();
        }
    }

    private void setupControls() {
        formatCombo.getItems().setAll(
            "GIF animado",
            "PNG spritesheet",
            "WebP (primer frame)",
            "MP4 (H.264)",
            "WebM (VP9)",
            "GIF (FFmpeg)"
        );
        formatCombo.getSelectionModel().selectFirst();

        fpsSlider.valueProperty().addListener((obs, old, val) -> {
            int fps = val.intValue();
            fpsLabel.setText("FPS: " + fps);
            restartAnimation(fps);
        });

        loopCheck.selectedProperty().addListener((obs, old, val) -> updateSummary());

        exportButton.setOnAction(e -> export());

        updateSummary();
    }

    private void preparePreview() {
        List<BufferedImage> frames = state.getProcessedFrames();
        if (frames == null || frames.isEmpty()) {
            statusLabel.setText("⚠ No hay frames procesados.");
            exportButton.setDisable(true);
            return;
        }

        previewImages = new ArrayList<>(frames.size());
        for (BufferedImage frame : frames) {
            previewImages.add(ImageConverter.toFxImage(frame));
        }

        if (!previewImages.isEmpty()) {
            animatedPreview.setImage(previewImages.get(0));
        }

        log.debug("Preview preparado: {} frames", previewImages.size());
    }

    private void startAnimation() {
        if (previewImages == null || previewImages.isEmpty()) return;

        int fps = (int) fpsSlider.getValue();
        double durationMs = 1000.0 / fps;

        animationTimeline = new Timeline(
            new KeyFrame(Duration.millis(durationMs), e -> {
                currentFrameIndex = (currentFrameIndex + 1) % previewImages.size();
                animatedPreview.setImage(previewImages.get(currentFrameIndex));
            })
        );
        animationTimeline.setCycleCount(Timeline.INDEFINITE);
        animationTimeline.play();
    }

    private void restartAnimation(int fps) {
        if (animationTimeline != null) {
            animationTimeline.stop();
        }
        startAnimation();
    }

    private void updateSummary() {
        List<BufferedImage> frames = state.getProcessedFrames();
        if (frames == null || frames.isEmpty()) {
            summaryLabel.setText("Sin frames");
            return;
        }

        int fps = (int) fpsSlider.getValue();
        double seconds = frames.size() / (double) fps;

        summaryLabel.setText(String.format(
            "• %d frames\n• %d fps\n• %.1f segundos\n• %s",
            frames.size(),
            fps,
            seconds,
            loopCheck.isSelected() ? "loop infinito" : "una sola vez"
        ));
    }

    private void export() {
        List<BufferedImage> frames = state.getProcessedFrames();
        if (frames == null || frames.isEmpty()) {
            Dialogs.warn("Sin frames", "No hay nada para exportar.");
            return;
        }

        String selectedFormat = formatCombo.getSelectionModel().getSelectedItem();
        Exporter exporter = pickExporter(selectedFormat);
        if (exporter == null) {
            Dialogs.error("Formato no soportado", selectedFormat);
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar animación como…");
        chooser.setInitialFileName("animacion." + exporter.getFileExtension());
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter(
                exporter.getFormatName(),
                "*." + exporter.getFileExtension()));

        Path defaultDir = Path.of(System.getProperty("user.home"),
            "kineticforge", "exportaciones");
        if (defaultDir.toFile().exists()) {
            chooser.setInitialDirectory(defaultDir.toFile());
        }

        File target = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (target == null) return;

        ExportConfig config = ExportConfig.defaults()
            .withFps((int) fpsSlider.getValue())
            .withLoop(loopCheck.isSelected());

        exportButton.setDisable(true);
        statusLabel.setText("Exportando…");

        Path output = target.toPath();
        Window ownerWindow = exportButton.getScene().getWindow();

        new Thread(() -> {
            try {
                exporter.export(frames, config, output);

                Platform.runLater(() -> {
                    statusLabel.setText("✅ Exportado: " + output.getFileName());
                    exportButton.setDisable(false);
                    // FIX: pasar owner para que el diálogo no se vaya al fondo
                    Dialogs.info(ownerWindow,
                        "Exportación completa",
                        "Archivo guardado en:\n" + output);
                });

            } catch (Exception ex) {
                log.error("Error exportando", ex);
                Platform.runLater(() -> {
                    statusLabel.setText("❌ Error: " + ex.getMessage());
                    exportButton.setDisable(false);
                    Dialogs.error(ownerWindow, "Error al exportar", ex.getMessage());
                });
            }
        }, "exporter-thread").start();
    }

    private Exporter pickExporter(String formatName) {
        if (formatName == null) return null;
        return switch (formatName) {
            case "GIF animado" -> new GifExporter();
            case "PNG spritesheet" -> new SpritesheetExporter();
            case "WebP (primer frame)" -> new WebPExporter();
            case "MP4 (H.264)" -> new FFmpegExporter(FFmpegExporter.Format.MP4);
            case "WebM (VP9)" -> new FFmpegExporter(FFmpegExporter.Format.WEBM);
            case "GIF (FFmpeg)" -> new FFmpegExporter(FFmpegExporter.Format.GIF);
            default -> null;
        };
    }

    /**
     * Detiene la animación del preview.
     * Llamado por WizardController al cambiar de paso.
     */
    public void stop() {
        if (animationTimeline != null) {
            animationTimeline.stop();
            animationTimeline = null;
            log.debug("Timeline detenido");
        }
    }
}
