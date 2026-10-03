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
import java.util.prefs.Preferences;

/**
 * Controlador mejorado del paso 4: exportación con memoria de formato y ruta.
 *
 * @author KineticForge Team
 * @version 1.3.0
 * @since 2026
 */
public class Step4ExportController {

    private static final Logger log = LoggerFactory.getLogger(Step4ExportController.class);
    private static final Preferences PREFS = Preferences.userNodeForPackage(Step4ExportController.class);
    private static final String PREF_LAST_FORMAT = "last_export_format";
    private static final String PREF_LAST_DIR = "last_export_dir";

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
        currentInstance = this;

        setupControls();
        preparePreview();
        startAnimation();

        log.info("Paso 4 (Exportación) inicializado con persistencia");
    }

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

        // Recuperar último formato usado (memoria de sesión/usuario)
        String savedFormat = PREFS.get(PREF_LAST_FORMAT, "GIF animado");
        if (formatCombo.getItems().contains(savedFormat)) {
            formatCombo.getSelectionModel().select(savedFormat);
        } else {
            formatCombo.getSelectionModel().selectFirst();
        }

        formatCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                PREFS.put(PREF_LAST_FORMAT, newVal);
            }
        });

        fpsSlider.valueProperty().addListener((obs, old, val) -> {
            int fps = val.intValue();
            fpsLabel.setText("FPS: " + fps);
            restartAnimation(fps);
            updateSummary();
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
    }

    private void startAnimation() {
        if (previewImages == null || previewImages.isEmpty()) return;

        int fps = (int) fpsSlider.getValue();
        double durationMs = 1000.0 / Math.max(1, fps);

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

        // Recuperar última ruta guardada o usar por defecto
        String lastDirStr = PREFS.get(PREF_LAST_DIR, null);
        if (lastDirStr != null) {
            File lastDir = new File(lastDirStr);
            if (lastDir.isDirectory()) {
                chooser.setInitialDirectory(lastDir);
            }
        } else {
            Path defaultDir = Path.of(System.getProperty("user.home"), "kineticforge", "exportaciones");
            if (defaultDir.toFile().exists()) {
                chooser.setInitialDirectory(defaultDir.toFile());
            }
        }

        File target = chooser.showSaveDialog(exportButton.getScene().getWindow());
        if (target == null) return;

        // Guardar la carpeta seleccionada para la próxima vez
        PREFS.put(PREF_LAST_DIR, target.getParent());

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

    public void stop() {
        if (animationTimeline != null) {
            animationTimeline.stop();
            animationTimeline = null;
            log.debug("Timeline detenido");
        }
    }
}
