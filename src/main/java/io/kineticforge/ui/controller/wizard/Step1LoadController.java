package io.kineticforge.ui.controller.wizard;

import io.kineticforge.io.ImageLoader;
import io.kineticforge.model.SheetScan;
import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.ImageConverter;
import javafx.collections.ListChangeListener;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controlador del paso 1: cargar una o varias hojas.
 *
 * <p>Las hojas se muestran en una lista reordenable; los frames de todas
 * ellas se concatenan, en ese orden, en una única animación.</p>
 *
 * @author KineticForge Team
 * @version 2.0.0
 * @since 2026
 */
public class Step1LoadController implements WizardStepController {

    private static final Logger log = LoggerFactory.getLogger(Step1LoadController.class);

    private static final int PREVIEW_MAX_SIDE = 1000;
    private static final Pattern DIGITS = Pattern.compile("(\\d+)");

    @FXML private StackPane dropZone;
    @FXML private Label dropLabel;
    @FXML private ImageView previewImage;
    @FXML private ListView<SheetScan> sheetList;
    @FXML private Button addButton;
    @FXML private Button upButton;
    @FXML private Button downButton;
    @FXML private Button removeButton;
    @FXML private Button nextButton;
    @FXML private Label fileInfoLabel;

    private final ImageLoader imageLoader = new ImageLoader();
    private WizardState state;
    private Runnable onComplete;
    private Path lastDirectory;

    @Override
    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;

        sheetList.setItems(state.getSheets());
        sheetList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, sel) -> showPreview(sel));
        state.getSheets().addListener((ListChangeListener<SheetScan>) c -> refreshState());

        setupDragAndDrop(dropZone);
        setupDragAndDrop(sheetList);

        addButton.setOnAction(e -> openFileChooser());
        removeButton.setOnAction(e -> removeSelected());
        upButton.setOnAction(e -> moveSelected(-1));
        downButton.setOnAction(e -> moveSelected(1));
        nextButton.setOnAction(e -> {
            if (onComplete != null) onComplete.run();
        });

        if (!state.getSheets().isEmpty()) {
            sheetList.getSelectionModel().select(0);
        }
        refreshState();
    }

    // ============================================================
    // Estado de la UI
    // ============================================================

    private void refreshState() {
        int size = state.getSheets().size();
        int idx = sheetList.getSelectionModel().getSelectedIndex();

        state.setLoadComplete(size > 0);
        nextButton.setDisable(size == 0);
        removeButton.setDisable(idx < 0);
        upButton.setDisable(idx <= 0);
        downButton.setDisable(idx < 0 || idx >= size - 1);
        dropLabel.setVisible(size == 0 || sheetList.getSelectionModel().getSelectedItem() == null);

        if (size == 0) {
            previewImage.setImage(null);
            fileInfoLabel.setText("");
        } else {
            fileInfoLabel.setText(size == 1 ? "1 hoja cargada" : size + " hojas cargadas");
        }
    }

    private void showPreview(SheetScan sheet) {
        if (sheet == null) {
            previewImage.setImage(null);
        } else {
            previewImage.setImage(ImageConverter.toFxImage(
                    ImageConverter.scaleToFit(sheet.getImage(), PREVIEW_MAX_SIDE)));
            dropLabel.setVisible(false);
        }
        refreshState();
    }

    // ============================================================
    // Arrastrar y soltar
    // ============================================================

    private void setupDragAndDrop(javafx.scene.Node node) {
        node.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY);
            }
            event.consume();
        });
        node.setOnDragDropped(this::handleDrop);
    }

    private void handleDrop(DragEvent event) {
        Dragboard db = event.getDragboard();
        boolean success = false;
        if (db.hasFiles() && !db.getFiles().isEmpty()) {
            loadFiles(db.getFiles());
            success = true;
        }
        event.setDropCompleted(success);
        event.consume();
    }

    // ============================================================
    // Acciones
    // ============================================================

    private void openFileChooser() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Elegir hojas escaneadas");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imágenes",
                        "*.png", "*.jpg", "*.jpeg", "*.tif", "*.tiff", "*.bmp"),
                new FileChooser.ExtensionFilter("Todos los archivos", "*.*"));
        if (lastDirectory != null && lastDirectory.toFile().isDirectory()) {
            chooser.setInitialDirectory(lastDirectory.toFile());
        }

        List<File> files = chooser.showOpenMultipleDialog(dropZone.getScene().getWindow());
        if (files != null && !files.isEmpty()) {
            loadFiles(files);
        }
    }

    private void removeSelected() {
        int idx = sheetList.getSelectionModel().getSelectedIndex();
        if (idx < 0) return;
        state.getSheets().remove(idx);
        state.invalidateProcessing();
        if (!state.getSheets().isEmpty()) {
            sheetList.getSelectionModel().select(Math.min(idx, state.getSheets().size() - 1));
        }
        refreshState();
    }

    private void moveSelected(int delta) {
        int idx = sheetList.getSelectionModel().getSelectedIndex();
        int target = idx + delta;
        if (idx < 0 || target < 0 || target >= state.getSheets().size()) return;

        Collections.swap(state.getSheets(), idx, target);
        state.invalidateProcessing();
        sheetList.getSelectionModel().select(target);
        refreshState();
    }

    /** Carga las imágenes en segundo plano (los escaneos grandes tardan). */
    private void loadFiles(List<File> files) {
        List<File> ordered = new ArrayList<>(files);
        ordered.sort(Comparator.comparing(File::getName, Step1LoadController::naturalCompare));

        addButton.setDisable(true);
        fileInfoLabel.setText("Cargando " + ordered.size() + " archivo(s)…");

        Task<List<Object>> task = new Task<>() {
            @Override
            protected List<Object> call() {
                List<Object> results = new ArrayList<>();
                for (File f : ordered) {
                    try {
                        BufferedImage img = imageLoader.load(f.toPath());
                        results.add(new SheetScan(f.toPath(), img));
                    } catch (Exception ex) {
                        log.error("Error cargando {}", f, ex);
                        results.add(f.getName() + ": " + ex.getMessage());
                    }
                }
                return results;
            }
        };

        task.setOnSucceeded(e -> {
            addButton.setDisable(false);
            StringBuilder errors = new StringBuilder();
            SheetScan firstAdded = null;

            for (Object r : task.getValue()) {
                if (r instanceof SheetScan sheet) {
                    state.getSheets().add(sheet);
                    lastDirectory = sheet.getPath().getParent();
                    if (firstAdded == null) firstAdded = sheet;
                } else {
                    errors.append("• ").append(r).append('\n');
                }
            }
            state.invalidateProcessing();

            if (firstAdded != null) {
                sheetList.getSelectionModel().select(firstAdded);
            }
            refreshState();

            if (errors.length() > 0) {
                Dialogs.error("No se pudieron cargar algunos archivos", errors.toString());
            }
        });

        task.setOnFailed(e -> {
            addButton.setDisable(false);
            refreshState();
            Dialogs.error("Error al cargar", String.valueOf(task.getException()));
        });

        Thread t = new Thread(task, "sheet-loader");
        t.setDaemon(true);
        t.start();
    }

    /** Orden "natural": scan_2 va antes que scan_10. */
    static int naturalCompare(String a, String b) {
        Matcher ma = DIGITS.matcher(a);
        Matcher mb = DIGITS.matcher(b);
        int posA = 0, posB = 0;

        while (ma.find() && mb.find()) {
            int c = a.substring(posA, ma.start()).compareToIgnoreCase(b.substring(posB, mb.start()));
            if (c != 0) return c;

            String na = ma.group().replaceFirst("^0+(?=.)", "");
            String nb = mb.group().replaceFirst("^0+(?=.)", "");
            c = Integer.compare(na.length(), nb.length());
            if (c == 0) c = na.compareTo(nb);
            if (c != 0) return c;

            posA = ma.end();
            posB = mb.end();
        }
        return a.substring(posA).compareToIgnoreCase(b.substring(posB));
    }
    @Override
    public void cleanup() {
        if (previewImage != null) {
            previewImage.setImage(null);
        }
        // ⚠️ NO limpiar sheetList aquí: comparte items con state.getSheets()
        state = null;
        onComplete = null;
        log.debug("Step1 cleanup completo");
    }
}
