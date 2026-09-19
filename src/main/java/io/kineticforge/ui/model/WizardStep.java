package io.kineticforge.ui.model;

/**
 * Pasos del wizard de creación de animación.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum WizardStep {

    LOAD_IMAGE("1. Cargar imagen", 1),
    DETECT_GRID("2. Detectar grilla", 2),
    PROCESS_FRAMES("3. Procesar frames", 3),
    EXPORT("4. Exportar", 4);

    private final String title;
    private final int number;

    WizardStep(String title, int number) {
        this.title = title;
        this.number = number;
    }

    public String getTitle() { return title; }
    public int getNumber() { return number; }

    public WizardStep previous() {
        return number > 1 ? fromNumber(number - 1) : this;
    }

    public WizardStep next() {
        return number < 4 ? fromNumber(number + 1) : this;
    }

    public static WizardStep fromNumber(int num) {
        for (WizardStep step : values()) {
            if (step.number == num) return step;
        }
        throw new IllegalArgumentException("Número de paso inválido: " + num);
    }
}
