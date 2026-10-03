package io.kineticforge.ui.controller;

import io.kineticforge.core.background.AlphaMatte;
import io.kineticforge.core.background.BackgroundRemover;
import io.kineticforge.core.background.Paper;
import io.kineticforge.core.background.ml.ModelDownloader;
import io.kineticforge.core.background.ml.U2NetBackgroundRemover;
import io.kineticforge.io.ImageLoader;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
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
 * Herramienta independiente para quitar el fondo de una imagen.
 *
 * <p>Tres métodos: blancos por umbral, clásico por contorno, e IA (U2-Net
 * con refinado de bordes). Todos producen un borde sin aura blanca.</p>
 *
 * @author KineticForge Team
 * @version 3.0.0
 * @since 2026
 */
public class BgToolController {

    private static final Logger log = LoggerFactory.getLogger(BgToolController.class);

    private static final int PREVIEW_MAX_SIDE = 1600;
    private static final String LIGHT_BG = "-fx-background-color: white;";
    private static final String DARK_BG = "-fx-background-color: #202020;";

    private enum Method {
        WHITE_THRESHOLD("Blancos por umbral"),
        CLASSIC("Clásico (contorno)"),
        AI("IA (U2-Net)");

        private final String label;
        Method(String label) { this.label = label; }

        @Override
        public String toString() { return label; }
    }

    @FXML private StackPane previewContainer;
    @FXML private ImageView previewImage;
    @FXML private Button loadButton;
    @FXML private ComboBox<Method> methodCombo;
    @FXML private CheckBox aiFullCheck;
    @FXML private Slider thresholdSlider;
    @FXML private Label thresholdLabel;
    @FXML private Label thresholdHelp;
    @FXML private ToggleButton darkPreviewToggle;
    @FXML private Button processButton;
    @FXML private Label statusLabel;
    @FXML private Button saveButton;

    private BufferedImage originalImage;
    private BufferedImage processedImage;
    private Path lastDirectory;

    @FXML
    private void initialize() {
        methodCombo.getItems().setAll(Method.values());
        methodCombo.getSelectionModel().select(Method.CLASSIC);
        methodCombo.getSelectionModel().selectedItemProperty()
                .addListener((obs, o, n) -> updateMethodUi());

        thresholdSlider.setBlockIncrement(1);
        thresholdSlider.valueProperty().addListener((obs, old, val) -> updateThresholdLabel());

        darkPreviewToggle.selectedProperty().addListener((obs, o, dark) ->
                previewContainer.setStyle(dark ? DARK_BG : LIGHT_BG));

        loadButton.setOnAction(e -> loadImage());
        processButton.setOnAction(e -> processImage());
        saveButton.setOnAction(e -> saveImage());

        updateMethodUi();
        log.info("BgTool inicializado");
    }

    private Method method() {
        Method m = methodCombo.getValue();
        return m == null ? Method.CLASSIC : m;
    }

    private void updateMethodUi() {
        Method m = method();
        aiFullCheck.setVisible(m == Method.AI);
        aiFullCheck.setManaged(m == Method.AI);

        if (m == Method.WHITE_THRESHOLD) {
            setSliderRange(200, 255);
            thresholdSlider.setValue(240);
            thresholdSlider.setMajorTickUnit(10);
            thresholdSlider.setDisable(false);
            thresholdHelp.setText("Borra todo lo que sea más claro que este valor, "
                    + "incluso dentro del dibujo. Bajalo si queda papel; subilo si se come detalles.");
        } else if (m == Method.AI) {
            setSliderRange(10, 90);
            thresholdSlider.setValue(50);
            thresholdSlider.setMajorTickUnit(20);
            thresholdSlider.setDisable(false);
            thresholdHelp.setText("Confianza de la IA. Bajala si faltan partes del dibujo; "
                    + "subila si sobra fondo.");
        } else {
            thresholdSlider.setDisable(true);
            thresholdHelp.setText("Detecta el contorno y borra el fondo conectado al borde, "
                    + "conservando los blancos interiores. No tiene parámetros.");
        }
        updateThresholdLabel();
    }

    /** Cambia el rango sin dejar nunca min > max (JavaFX no lo tolera bien). */
    private void setSliderRange(double min, double max) {
        if (min > thresholdSlider.getMax()) {
            thresholdSlider.setMax(max);
            thresholdSlider.setMin(min);
        } else {
            thresholdSlider.setMin(min);
            thresholdSlider.setMax(max);
        }
    }

    private void updateThresholdLabel() {
        Method m = method();
        int v = (int) thresholdSlider.getValue();
        if (m == Method.WHITE_THRESHOLD) {
            thresholdLabel.setText("Tolerancia: " + v);
        } else if (m == Method.AI) {
            thresholdLabel.setText("Confianza IA: " + v + "%");
        } else {
            thresholdLabel.setText("Sin parámetros");
        }
    }

    private void loadImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Cargar imagen");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes",
                        "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos", "*.*"));
        if (lastDirectory != null && lastDirectory.toFile().isDirectory()) {
            chooser.setInitialDirectory(lastDirectory.toFile());
        }

        File file = chooser.showOpenDialog(loadButton.getScene().getWindow());
        if (file == null) return;

        try {
            originalImage = new ImageLoader().load(file.toPath());
            lastDirectory = file.toPath().getParent();

            show(originalImage);
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

    private void show(BufferedImage image) {
        previewImage.setImage(ImageConverter.toFxImage(
                ImageConverter.scaleToFit(image, PREVIEW_MAX_SIDE)));
    }

    private void processImage() {
        if (originalImage == null) return;

        final BufferedImage source = originalImage;
        final Method m = method();
        final int sliderValue = (int) thresholdSlider.getValue();
        final boolean full = aiFullCheck.isSelected();

        processButton.setDisable(true);
        saveButton.setDisable(true);
        statusLabel.setText("Procesando...");

        Task<BufferedImage> task = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                switch (m) {
                    case WHITE_THRESHOLD:
                        return removeWhite(source, sliderValue);
                    case AI:
                        ModelDownloader.Variant variant = full
                                ? ModelDownloader.Variant.FULL : ModelDownloader.Variant.LIGHT;
                        Path model = ModelDownloader.ensureModel(variant, p ->
                                updateMessage(String.format("Descargando modelo: %d%%", (int) (p * 100))));
                        updateMessage("Quitando fondo con IA...");
                        return U2NetBackgroundRemover.shared(model)
                                .removeBackground(source, sliderValue / 100f);
                    default:
                        return new BackgroundRemover().removeBackground(source);
                }
            }
        };
        task.messageProperty().addListener((obs, o, msg) -> statusLabel.setText(msg));

        task.setOnSucceeded(e -> {
            processedImage = task.getValue();
            show(processedImage);
            statusLabel.setText("✅ Listo. Probá «Ver sobre fondo oscuro» para revisar los bordes.");
            saveButton.setDisable(false);
            processButton.setDisable(false);
        });
        task.setOnFailed(e -> {
            Throwable ex = task.getException();
            log.error("Error procesando", ex);
            statusLabel.setText("❌ Error: " + ex.getMessage());
            processButton.setDisable(false);
            Dialogs.error("Error", ex.getMessage() != null ? ex.getMessage() : ex.toString());
        });

        Thread t = new Thread(task, "bg-tool");
        t.setDaemon(true);
        t.start();
    }

    /**
     * Borra TODOS los píxeles claros (no solo los conectados al borde).
     * El borde se calcula con descontaminación de color: sin aura blanca.
     */
    private BufferedImage removeWhite(BufferedImage input, int threshold) {
        int w = input.getWidth();
        int h = input.getHeight();

        int[] px = input.getRGB(0, 0, w, h, null, 0, w);
        int[] balanced = Paper.whiteBalance(px, Paper.estimate(px));

        boolean[] background = new boolean[w * h];
        for (int i = 0; i < background.length; i++) {
            background[i] = Paper.luminance(balanced[i]) >= threshold;
        }
        return AlphaMatte.compose(balanced, background, w, h, 4, true);
    }

    private void saveImage() {
        if (processedImage == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar PNG");
        chooser.setInitialFileName("sin-fondo.png");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG", "*.png"));
        if (lastDirectory != null && lastDirectory.toFile().isDirectory()) {
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
