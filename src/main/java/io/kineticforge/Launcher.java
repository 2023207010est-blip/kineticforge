package io.kineticforge;

import javafx.application.Application;

/**
 * Clase de arranque de KineticForge.
 */
public class Launcher {
    private Launcher() {}
    public static void main(String[] args) {
        Application.launch(KineticForgeApplication.class, args);
    }
}
