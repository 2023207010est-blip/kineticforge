package io.kineticforge.ui.model;

/**
 * Pasos del wizard. Ahora solo hay 2.
 */
public enum WizardStep {

    LOAD_IMAGE("Paso 1: Cargar hojas"),
    WORKSPACE("Paso 2: Procesar y exportar");

    private final String title;

    WizardStep(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public boolean hasNext() {
        return ordinal() < values().length - 1;
    }

    public WizardStep next() {
        return hasNext() ? values()[ordinal() + 1] : this;
    }

    public boolean hasPrevious() {
        return ordinal() > 0;
    }

    public WizardStep previous() {
        return hasPrevious() ? values()[ordinal() - 1] : this;
    }
}
