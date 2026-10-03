package io.kineticforge.ui.controller.wizard;

import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.model.WizardStep;
import io.kineticforge.ui.util.ThemeManager;
import io.kineticforge.ui.util.TemplatesDialog;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controlador principal del wizard.
 *
 * @author KineticForge Team
 * @version 1.2.0
 * @since 2026
 */
public class WizardController {

    private static final Logger log = LoggerFactory.getLogger(WizardController.class);

    @FXML private StackPane contentArea;
    @FXML private Label stepIndicator;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button themeButton;
    @FXML private Button templatesButton;
    @FXML private Button bgToolButton;

    private final WizardState state = new WizardState();
    private WizardStep currentStep = WizardStep.LOAD_IMAGE;

    @FXML
    public void initialize() {
        log.info("Wizard inicializado");
        updateThemeButton(ThemeManager.isDarkMode());
        showStep(WizardStep.LOAD_IMAGE);
    }

    private void showStep(WizardStep step) {
        log.debug("Mostrando paso: {}", step.getTitle());

        Step4ExportController.stopInstance();

        this.currentStep = step;

        try {
            String fxmlPath = getFxmlPathFor(step);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent stepContent = loader.load();

            initializeStepController(loader.getController(), step);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(stepContent);

            updateNavigation(step);

        } catch (IOException e) {
            log.error("Error al cargar el paso: {}", step, e);
        }
    }

    private String getFxmlPathFor(WizardStep step) {
        return switch (step) {
            case LOAD_IMAGE -> "/view/wizard/step1-load.fxml";
            case DETECT_GRID -> "/view/wizard/step2-grid.fxml";
            case PROCESS_FRAMES -> "/view/wizard/step3-process.fxml";
            case EXPORT -> "/view/wizard/step4-export.fxml";
        };
    }

    private void initializeStepController(Object controller, WizardStep step) {
        if (controller instanceof Step1LoadController c) {
            c.init(state, () -> goNext());
        } else if (controller instanceof Step2GridController c) {
            c.init(state, () -> goNext());
        } else if (controller instanceof Step3ProcessController c) {
            c.init(state, () -> goNext());
        } else if (controller instanceof Step4ExportController c) {
            c.init(state, () -> log.info("Wizard completado"));
        }
    }

    private void updateNavigation(WizardStep step) {
        stepIndicator.setText(step.getTitle());
        backButton.setDisable(step == WizardStep.LOAD_IMAGE);
        nextButton.setDisable(step == WizardStep.EXPORT);
    }

    @FXML
    private void onBack() {
        if (currentStep != WizardStep.LOAD_IMAGE) {
            showStep(currentStep.previous());
        }
    }

    @FXML
    private void onNext() {
        if (currentStep == WizardStep.EXPORT) {
            return;
        }
        String problem = validateBeforeLeaving(currentStep);
        if (problem != null) {
            Dialogs.warn("Falta un paso", problem);
            return;
        }
        showStep(currentStep.next());
    }

    /** @return mensaje si no se puede avanzar desde el paso dado; null si se puede. */
    private String validateBeforeLeaving(WizardStep step) {
        return switch (step) {
            case LOAD_IMAGE -> state.getSheets().isEmpty()
                    ? "Cargá al menos una hoja o imagen." : null;
            case DETECT_GRID -> state.allSheetsHaveGrid()
                    ? null : "Todas las hojas necesitan una grilla. Presioná «Aplicar» en cada una.";
            case PROCESS_FRAMES -> state.getProcessedFrames() == null
                    || state.getProcessedFrames().isEmpty()
                    ? "Presioná «Procesar frames» antes de continuar." : null;
            default -> null;
        };
    }

    private void goNext() {
        onNext();
    }

    @FXML
    private void onToggleTheme() {
        boolean darkNow = ThemeManager.toggle();
        updateThemeButton(darkNow);
        log.info("Tema cambiado a: {}", darkNow ? "oscuro" : "claro");
    }

    private void updateThemeButton(boolean isDark) {
        if (isDark) {
            themeButton.setText("☀ Claro");
        } else {
            themeButton.setText("☾ Oscuro");
        }
    }

    @FXML
    private void onOpenTemplates() {
        TemplatesDialog.show();
    }

    @FXML
    private void onOpenBgTool() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/view/bg-tool.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle("Quitar fondo - KineticForge");
            Scene scene = new Scene(root);
            scene.getStylesheets().add(
                    getClass().getResource("/css/app.css").toExternalForm());
            stage.setScene(scene);
            stage.initOwner(contentArea.getScene().getWindow());
            stage.show();

            log.info("Herramienta de fondo abierta");
        } catch (Exception e) {
            log.error("Error abriendo bg-tool", e);
        }
    }

    public WizardState getState() {
        return state;
    }
}
