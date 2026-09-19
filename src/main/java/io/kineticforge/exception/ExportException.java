package io.kineticforge.exception;

/**
 * Excepción lanzada durante la exportación de animaciones.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class ExportException extends KineticForgeException {

    private static final long serialVersionUID = 1L;

    public ExportException(String message) {
        super(message);
    }

    public ExportException(String message, Throwable cause) {
        super(message, cause);
    }
}
