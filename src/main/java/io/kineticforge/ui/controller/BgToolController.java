package io.kineticforge.ui.controller;

import io.kineticforge.io.ImageLoader;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Herramienta de prueba de borrado de fondo.
 *
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class BgToolController {

    private static final Logger log = LoggerFactory.getLogger(BgToolController.class);

    @FXML private StackPane previewContainer;
    @FXML private ImageView previewImage;
    @FXML private Button loadButton;
    @FXML private Slider thresholdSlider;
    @FXML private Label thresholdLabel;
    @FXML private Button processButton;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private BufferedImage originalImage;
    private BufferedImage processedImage;
    private Path lastDirectory;

    @FXML
    private void initialize() {
        thresholdSlider.setMin(200);
        thresholdSlider.setMax(255);
        thresholdSlider.setValue(240);
        thresholdSlider.setBlockIncrement(1);
        thresholdSlider.setShowTickMarks(true);
        thresholdSlider.setShowTickLabels(true);
        thresholdSlider.setMajorTickUnit(10);

        thresholdSlider.valueProperty().addListener((obs, old, val) ->
            thresholdLabel.setText("Tolerancia: " + val.intValue()));

        thresholdLabel.setText("Tolerancia: 240");

        loadButton.setOnAction(e -> loadImage());
        processButton.setOnAction(e -> processImage());
        saveButton.setOnAction(e -> saveImage());

        log.info("BgTool inicializado");
    }

    private void loadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Cargar imagen");
        chooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Imágenes",
                "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff", "*.bmp"),
            new FileChooser.ExtensionFilter("Todos", "*.*"));

        if (lastDirectory != null) {
            chooser.setInitialDirectory(lastDirectory.toFile());
        }

        File file = chooser.showOpenDialog(loadButton.getScene().getWindow());
        if (file == null) return;

        try {
            originalImage = new ImageLoader().load(file.toPath());
            lastDirectory = file.toPath().getParent();

            previewImage.setImage(ImageConverter.toFxImage(originalImage));
            statusLabel.setText(String.format("✅ Cargado: %s (%dx%d)",
                file.getName(), originalImage.getWidth(), originalImage.getHeight()));
            processButton.setDisable(false);
            saveButton.setDisable(true);
            processedImage = null;

        } catch (Exception e) {
            log.error("Error cargando imagen", e);
            Dialogs.error("Error", "No se pudo cargar: " + e.getMessage());
        }
    }

    private void processImage() {
        if (originalImage == null) return;

        int threshold = (int) thresholdSlider.getValue();

        processButton.setDisable(true);
        statusLabel.setText("Procesando...");

        // Procesar en el mismo thread (es rápido)
        Platform.runLater(() -> {
            try {
                processedImage = removeAllWhite(originalImage, threshold);
                previewImage.setImage(ImageConverter.toFxImage(processedImage));
                statusLabel.setText("✅ Procesado. Guardá como PNG.");
                saveButton.setDisable(false);
            } catch (Exception e) {
                log.error("Error procesando", e);
                statusLabel.setText("❌ Error: " + e.getMessage());
                Dialogs.error("Error", e.getMessage());
            } finally {
                processButton.setDisable(false);
            }
        });
    }

    /**
     * Elimina TODOS los píxeles blancos (no solo los conectados al borde).
     * Alpha binario (0 o 255) para que no haya aura.
     *
     * @param input imagen original
     * @param threshold umbral de luminancia (200-255)
     * @return imagen con fondo transparente
     */
    private BufferedImage removeAllWhite(BufferedImage input, int threshold) {
        int w = input.getWidth();
        int h = input.getHeight();

        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb = input.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                int lum = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                if (lum >= threshold) {
                    // Blanco → transparente puro
                    result.setRGB(x, y, 0x00000000);
                } else {
                    // Contenido → opaco puro (alpha binario, sin aura)
                    result.setRGB(x, y, 0xFF000000 | (rgb & 0x00FFFFFF));
                }
            }
        }

        return result;
    }

    private void saveImage() {
        if (processedImage == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar PNG");
        chooser.setInitialFileName("sin-fondo.png");
        chooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("PNG", "*.png"));

        if (lastDirectory != null) {
            chooser.setInitialDirectory(lastDirectory.toFile());
        }

        File file = chooser.showSaveDialog(saveButton.getScene().getWindow());
        if (file == null) return;

        try {
            ImageIO.write(processedImage, "png", file);
            statusLabel.setText("✅ Guardado: " + file.getName());
            Dialogs.info("Guardado", "PNG guardado en:\n" + file.getAbsolutePath());
        } catch (Exception e) {
            log.error("Error guardando", e);
            Dialogs.error("Error", e.getMessage());
        }
    }
}
