package io.kineticforge;

import atlantafx.base.theme.PrimerLight;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Aplicación principal de KineticForge.
 */
public class KineticForgeApplication extends Application {

    private static final Logger log = LoggerFactory.getLogger(KineticForgeApplication.class);

    @Override
    public void start(Stage stage) throws Exception {
        log.info("Iniciando KineticForge...");
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/view/main-view.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/css/app.css")).toExternalForm());
        stage.setTitle("KineticForge v1.0.0");
        stage.setScene(scene);
        stage.setMinWidth(1024);
        stage.setMinHeight(720);
        stage.show();
    }

    @Override
    public void stop() {
        System.exit(0);
    }
}
