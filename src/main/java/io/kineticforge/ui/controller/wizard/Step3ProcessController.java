package io.kineticforge.ui.controller.wizard;

import io.kineticforge.ui.model.WizardState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controlador del paso 3: procesamiento de frames.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class Step3ProcessController {

    private static final Logger log = LoggerFactory.getLogger(Step3ProcessController.class);

    private WizardState state;
    private Runnable onComplete;

    public void init(WizardState state, Runnable onComplete) {
        this.state = state;
        this.onComplete = onComplete;
        log.info("Paso 3 inicializado (pendiente de implementar)");
    }
}
