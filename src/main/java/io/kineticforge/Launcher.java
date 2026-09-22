package io.kineticforge;

import io.kineticforge.util.AppPaths;
import javafx.application.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;

/**
 * Clase de arranque de KineticForge.
 *
 * <p>Crea la carpeta raíz de la aplicación y lanza la UI de JavaFX.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class Launcher {

    private static final Logger log = LoggerFactory.getLogger(Launcher.class);

    private Launcher() {
        // utility class
    }

    public static void main(String[] args) {
        // Crear carpetas estándar de la app
        try {
            Files.createDirectories(AppPaths.getRootDir());
            AppPaths.ensureDirectories();
            log.info("Carpetas de la aplicación listas en: {}", AppPaths.getRootDir());
        } catch (Exception e) {
            log.error("Error creando carpetas de la aplicación", e);
        }

        // Lanzar la UI
        Application.launch(KineticForgeApplication.class, args);
    }
}
