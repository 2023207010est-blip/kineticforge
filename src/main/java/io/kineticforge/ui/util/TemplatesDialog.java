package io.kineticforge.ui.util;

import io.kineticforge.io.TemplateGeneratorService;
import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.GuideDotColor;
import io.kineticforge.model.GuideDotStyle;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import io.kineticforge.util.AppPaths;
import javafx.geometry.Insets;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Diálogo para generar plantillas de grilla.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class TemplatesDialog {

    private static final Logger log = LoggerFactory.getLogger(TemplatesDialog.class);

    private TemplatesDialog() {
    }

    /**
     * Muestra el diálogo de generación de plantillas.
     */
    public static void show() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Generar plantilla");
        dialog.setHeaderText("Elegí las opciones y generá tu plantilla imprimible");

        // Botones
        ButtonType generateButton = new ButtonType("📄 Generar", ButtonBar.ButtonData.OK_DONE);
        ButtonType closeButton = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(generateButton, closeButton);

        // Contenido
        VBox content = new VBox(12);
        content.setPadding(new Insets(20));

        // Preset
        Label presetLabel = new Label("Preset de grilla:");
        ComboBox<GridPreset> presetCombo = new ComboBox<>();
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
        presetCombo.setMaxWidth(Double.MAX_VALUE);

        // Orientación
        Label orientLabel = new Label("Orientación:");
        ToggleButton portraitToggle = new ToggleButton("Vertical");
        ToggleButton landscapeToggle = new ToggleButton("Horizontal");
        landscapeToggle.setSelected(true);
        ToggleGroup orientationGroup = new ToggleGroup();
        portraitToggle.setToggleGroup(orientationGroup);
        landscapeToggle.setToggleGroup(orientationGroup);
        HBox orientationBox = new HBox(8, portraitToggle, landscapeToggle);

        // Tamaño de página
        Label sizeLabel = new Label("Tamaño de página:");
        ComboBox<PageSize> sizeCombo = new ComboBox<>();
        sizeCombo.getItems().setAll(PageSize.values());
        sizeCombo.getSelectionModel().select(PageSize.A4);
        sizeCombo.setMaxWidth(Double.MAX_VALUE);

        // Margen
        Label marginLabel = new Label("Margen: 15 mm");
        Slider marginSlider = new Slider(5, 30, 15);
        marginSlider.setBlockIncrement(1);
        marginSlider.setShowTickMarks(true);
        marginSlider.setShowTickLabels(true);
        marginSlider.setMajorTickUnit(5);
        marginSlider.valueProperty().addListener((obs, o, n) ->
            marginLabel.setText(String.format("Margen: %.0f mm", n.doubleValue())));

        // Punto guía
        CheckBox guideDotCheck = new CheckBox("Incluir punto guía (opcional)");
        guideDotCheck.setSelected(false);

        // Info
        Label infoLabel = new Label("Las plantillas se guardan en:\n" + AppPaths.getTemplatesDir());
        infoLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888;");

        content.getChildren().addAll(
            presetLabel, presetCombo,
            orientLabel, orientationBox,
            sizeLabel, sizeCombo,
            marginLabel, marginSlider,
            guideDotCheck,
            infoLabel
        );

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(400);

        // Handler del botón Generar
        dialog.setResultConverter(buttonType -> {
            if (buttonType == generateButton) {
                generate(presetCombo.getValue(),
                    portraitToggle.isSelected()
                        ? PageOrientation.PORTRAIT
                        : PageOrientation.LANDSCAPE,
                    sizeCombo.getValue(),
                    marginSlider.getValue(),
                    guideDotCheck.isSelected());
            }
            return buttonType;
        });

        dialog.showAndWait();
    }

    /**
     * Genera la plantilla con los parámetros dados.
     */
    private static void generate(GridPreset preset,
                                 PageOrientation orientation,
                                 PageSize pageSize,
                                 double margin,
                                 boolean includeGuideDot) {
        try {
            GridSpec spec = GridSpec.builder()
                .preset(preset)
                .orientation(orientation)
                .pageSize(pageSize)
                .marginMm(margin)
                .gutterMm(2.0)
                .includeGuideDot(includeGuideDot)
                .guideDotStyle(GuideDotStyle.CIRCLE)
                .guideDotColor(GuideDotColor.LIGHT_GRAY)
                .guideDotDiameterMm(1.5)
                .build();

            TemplateGeneratorService service = new TemplateGeneratorService();
            List<Path> generated = service.generateAll(spec);

            StringBuilder message = new StringBuilder("Plantilla generada:\n\n");
            for (Path path : generated) {
                message.append("• ").append(path.getFileName()).append("\n");
            }
            message.append("\nCarpeta: ").append(AppPaths.getTemplatesDir());

            Dialogs.info("✅ Plantilla generada", message.toString());
            log.info("Plantilla generada: {} archivos", generated.size());

        } catch (Exception e) {
            log.error("Error generando plantilla", e);
            Dialogs.error("Error al generar", e.getMessage());
        }
    }
}
