package io.kineticforge.ui.controller.wizard;

import io.kineticforge.ui.model.WizardState;
import io.kineticforge.ui.model.WizardStep;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kineticforge.ui.util.ThemeManager;
import java.io.IOException;
import io.kineticforge.ui.util.TemplatesDialog;
/**
 * Controlador principal del wizard.
 *
 * <p>Orquesta la navegación entre los 4 pasos y mantiene el estado
 * compartido. Cada paso se carga dinámicamente en el panel central.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class WizardController {

    private static final Logger log = LoggerFactory.getLogger(WizardController.class);

    // Elementos del wizard.fxml
    @FXML private StackPane contentArea;
    @FXML private Label stepIndicator;
    @FXML private Button backButton;
    @FXML private Button nextButton;
    @FXML private Button themeButton;
    @FXML private Button templatesButton;
    @FXML
    private void onOpenTemplates() {
        TemplatesDialog.show();
    }

    private final WizardState state = new WizardState();
    private WizardStep currentStep = WizardStep.LOAD_IMAGE;

    @FXML
    public void initialize() {
        log.info("Wizard inicializado");
        updateThemeButton(ThemeManager.isDarkMode());
        showStep(WizardStep.LOAD_IMAGE);
    }

    /**
     * Muestra el paso indicado cargando su FXML.
     */
    private void showStep(WizardStep step) {
        log.debug("Mostrando paso: {}", step.getTitle());

        this.currentStep = step;

        try {
            String fxmlPath = getFxmlPathFor(step);
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent stepContent = loader.load();

            // Inicializar el controlador del paso con el estado compartido
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
        if (currentStep != WizardStep.EXPORT) {
            showStep(currentStep.next());
        }
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

    public WizardState getState() {
        return state;
    }
}
