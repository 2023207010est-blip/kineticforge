package io.kineticforge;

import io.kineticforge.ui.util.ThemeManager;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Aplicación principal de KineticForge.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class KineticForgeApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(KineticForgeApplication.class);

    @Override
    public void start(Stage stage) throws Exception {
        log.info("Iniciando KineticForge...");

        // Aplicar tema (claro u oscuro según preferencia guardada)
        ThemeManager.applyCurrentTheme();

        FXMLLoader loader = new FXMLLoader(
            getClass().getResource("/view/main-view.fxml"));
        Scene scene = new Scene(loader.load());

        scene.getStylesheets().add(
            Objects.requireNonNull(
                getClass().getResource("/css/app.css")).toExternalForm());

        stage.setTitle("KineticForge v1.0.0");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(720);
        stage.show();

        log.info("KineticForge iniciado");
    }

    @Override
    public void stop() {
        log.info("Cerrando KineticForge...");
        System.exit(0);
    }
}
