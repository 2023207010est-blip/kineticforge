package io.kineticforge.ui.controller.wizard;

import io.kineticforge.ui.model.WizardState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del paso 4: exportación.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class Step4ExportController {

    private static final Logger log = LoggerFactory.getLogger(Step4ExportController.class);

    private WizardState state;
    private Runnable onComplete;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;
        log.info("Paso 4 inicializado (pendiente de implementar)");
    }
}
