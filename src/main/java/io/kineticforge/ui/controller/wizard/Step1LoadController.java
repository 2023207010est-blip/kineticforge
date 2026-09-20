package io.kineticforge.ui.controller.wizard;

import io.kineticforge.io.ImageLoader;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Path;

/**
 * Controlador del paso 1: cargar imagen.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class Step1LoadController {

    private static final Logger log = LoggerFactory.getLogger(Step1LoadController.class);

    @FXML private StackPane dropZone;
    @FXML private Label dropLabel;
    @FXML private ImageView previewImage;
    @FXML private Button chooseButton;
    @FXML private Button nextButton;
    @FXML private Label fileInfoLabel;

    private final ImageLoader imageLoader = new ImageLoader();
    private WizardState state;
    private Runnable onComplete;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        setupDragAndDrop();
        setupButtonHandlers();

        // Estado inicial
        nextButton.setDisable(true);
    }

    private void setupDragAndDrop() {
        dropZone.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });

        dropZone.setOnDragEntered(event -> {
            if (event.getDragboard().hasFiles()) {
                dropZone.setStyle("-fx-border-color: #2D7FF9; -fx-border-width: 2;");
            }
        });

        dropZone.setOnDragExited(event ->
                dropZone.setStyle("-fx-border-color: #CCCCCC; -fx-border-width: 2;"));

        dropZone.setOnDragDropped(this::handleDrop);
    }

    private void handleDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;

        if (db.hasFiles() && !db.getFiles().isEmpty()) {
            File file = db.getFiles().get(0);
            success = loadFile(file.toPath());
        }

        event.setDropCompleted(success);
        event.consume();
    }

    private void setupButtonHandlers() {
        chooseButton.setOnAction(e -> openFileChooser());
        nextButton.setOnAction(e -> {
            if (onComplete != null) {
                onComplete.run();
            }
        });
    }

    private void openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir imagen escaneada");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes",
                        "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));

        File file = chooser.showOpenDialog(dropZone.getScene().getWindow());
        if (file != null) {
            loadFile(file.toPath());
        }
    }

    private boolean loadFile(Path path) {
        try {
            log.info("Cargando imagen: {}", path);
            BufferedImage image = imageLoader.load(path);

            state.setSourceFile(path);
            state.setOriginalImage(image);
            state.setLoadComplete(true);

            // Actualizar UI
            previewImage.setImage(ImageConverter.toFxImage(image));
            fileInfoLabel.setText(String.format("%s — %d × %d px",
                    path.getFileName(), image.getWidth(), image.getHeight()));
            dropLabel.setVisible(false);
            dropZone.setStyle("-fx-border-color: #2D7FF9; -fx-border-width: 2;");
            nextButton.setDisable(false);

            log.info("Imagen cargada correctamente");
            return true;

        } catch (Exception e) {
            log.error("Error al cargar la imagen", e);
            Dialogs.error("No se pudo cargar la imagen",
                    "Archivo: " + path.getFileName() + "\n\n" + e.getMessage());
            return false;
        }
    }
}
