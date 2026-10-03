package io.kineticforge.ui.controller.wizard;

import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.model.WizardStep;
import io.kineticforge.ui.util.Dialogs;
import io.kineticforge.ui.util.Notifications;
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
 * Controlador principal del wizard — versión 2 pasos.
 *
 * @author KineticForge Team
 * @version 3.0.0
 * @since 2026
 */
public class WizardController {

    private static final Logger log = LoggerFactory.getLogger(WizardController.class);

    @FXML private StackPane rootStack;
    @FXML private StackPane contentArea;
    @FXML private Label stepIndicator;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button themeButton;
    @FXML private Button templatesButton;
    @FXML private Button bgToolButton;

    private final WizardState state = new WizardState();
    private WizardStep currentStep = WizardStep.LOAD_IMAGE;
    private WizardStepController activeController;

    @FXML
    public void initialize() {
        log.info("Wizard inicializado (2 pasos)");

        // Enganchar el sistema de notificaciones al StackPane raíz
        Notifications.attach(rootStack);

        updateThemeButton(ThemeManager.isDarkMode());
        showStep(WizardStep.LOAD_IMAGE);
    }

    private void showStep(WizardStep step) {
        log.debug("Mostrando paso: {}", step.getTitle());

        cleanupActiveController();
        this.currentStep = step;

        try {
            String fxmlPath = getFxmlPathFor(step);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent stepContent = loader.load();

            Object controller = loader.getController();
            if (!(controller instanceof WizardStepController wsc)) {
                throw new IllegalStateException(
                    "El controlador " + controller.getClass().getSimpleName()
                        + " no implementa WizardStepController");
            }
            activeController = wsc;
            wsc.init(state, this::goNext);

            contentArea.getChildren().clear();
            contentArea.getChildren().add(stepContent);

            updateNavigation(step);

        } catch (IOException e) {
            log.error("Error al cargar el paso: {}", step, e);
        }
    }

    private void cleanupActiveController() {
        if (activeController != null) {
            try {
                activeController.cleanup();
            } catch (Exception e) {
                log.error("Error en cleanup", e);
            }
            activeController = null;
        }
    }

    private String getFxmlPathFor(WizardStep step) {
        return switch (step) {
            case LOAD_IMAGE -> "/view/wizard/step1-load.fxml";
            case WORKSPACE  -> "/view/wizard/workspace.fxml";
        };
    }

    private void updateNavigation(WizardStep step) {
        stepIndicator.setText(step.getTitle());
        backButton.setDisable(step == WizardStep.LOAD_IMAGE);
        nextButton.setDisable(true); // Se controla desde cada paso
    }

    @FXML
    private void onBack() {
        if (currentStep == WizardStep.WORKSPACE) {
            showStep(WizardStep.LOAD_IMAGE);
        }
    }

    @FXML
    private void onNext() {
        String problem = validateBeforeLeaving(currentStep);
        if (problem != null) {
            Dialogs.warn("Falta un paso", problem);
            return;
        }
        if (currentStep.hasNext()) {
            showStep(currentStep.next());
        }
    }

    private String validateBeforeLeaving(WizardStep step) {
        if (step == WizardStep.LOAD_IMAGE) {
            return state.getSheets().isEmpty()
                ? "Cargá al menos una hoja o imagen." : null;
        }
        return null;
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
        themeButton.setText(isDark ? "☀ Claro" : "☾ Oscuro");
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
        } catch (Exception e) {
            log.error("Error abriendo bg-tool", e);
        }
    }

    public WizardState getState() {
        return state;
    }
}
