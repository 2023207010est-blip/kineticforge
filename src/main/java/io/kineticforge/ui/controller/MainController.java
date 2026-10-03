package io.kineticforge.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.BorderPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Controlador principal de la aplicación.
 *
 * <p>Carga el wizard de creación de animación.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class MainController {

    private static final Logger log = LoggerFactory.getLogger(MainController.class);

    @FXML private BorderPane rootPane;

    @FXML
    private void initialize() {
        log.info("Inicializando MainController");
        loadWizard();
    }

    private void loadWizard() {
        try {
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/view/wizard/wizard.fxml"));
            Parent wizard = loader.load();
            rootPane.setCenter(wizard);
            log.info("Wizard cargado");

        } catch (IOException e) {
            log.error("Error al cargar el wizard", e);
        }
    }
}
