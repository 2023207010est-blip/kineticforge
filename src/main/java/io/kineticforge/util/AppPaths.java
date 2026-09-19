package io.kineticforge.util;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Rutas estándar de KineticForge dentro del home del usuario.
 *
 * <p>Estructura creada automáticamente:</p>
 * <pre>
 * ~/kineticforge/
 * ├── plantillas/       ← plantillas PDF y PNG
 * ├── proyectos/        ← proyectos guardados (.ffp)
 * ├── exportaciones/    ← GIFs, WebPs, APNGs
 * └── logs/             ← logs de la aplicación
 * </pre>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class AppPaths {

    private static final String APP_DIR_NAME = "kineticforge";

    private AppPaths() {
    }

    /**
     * @return raíz de la aplicación: {@code ~/kineticforge}.
     */
    public static Path getRootDir() {
        return Path.of(System.getProperty("user.home"), APP_DIR_NAME);
    }

    /**
     * @return carpeta para plantillas: {@code ~/kineticforge/plantillas}.
     */
    public static Path getTemplatesDir() {
        return getRootDir().resolve("plantillas");
    }

    /**
     * @return carpeta para proyectos: {@code ~/kineticforge/proyectos}.
     */
    public static Path getProjectsDir() {
        return getRootDir().resolve("proyectos");
    }

    /**
     * @return carpeta para exportaciones: {@code ~/kineticforge/exportaciones}.
     */
    public static Path getExportsDir() {
        return getRootDir().resolve("exportaciones");
    }

    /**
     * @return carpeta para logs: {@code ~/kineticforge/logs}.
     */
    public static Path getLogsDir() {
        return getRootDir().resolve("logs");
    }

    /**
     * Crea todas las carpetas estándar si no existen.
     *
     * @throws RuntimeException si falla la creación
     */
    public static void ensureDirectories() {
        try {
            Files.createDirectories(getTemplatesDir());
            Files.createDirectories(getProjectsDir());
            Files.createDirectories(getExportsDir());
            Files.createDirectories(getLogsDir());
        } catch (Exception e) {
            throw new RuntimeException(
                "No se pudieron crear las carpetas de la aplicación en: "
                    + getRootDir(), e);
        }
    }
}
