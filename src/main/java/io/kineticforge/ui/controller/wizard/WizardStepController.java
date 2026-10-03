package io.kineticforge.ui.controller.wizard;

import io.kineticforge.ui.model.WizardState;

/**
 * Contrato de ciclo de vida para los controladores del wizard.
 * Garantiza que cada paso libere listeners, bindings y tareas al salir.
 */
public interface WizardStepController {

    /** Inicializa el controlador con el estado compartido. */
    void init(WizardState state, Runnable onComplete);

    /** Libera recursos antes de descartar el controlador. */
    default void cleanup() {
        // opcional
    }
}
