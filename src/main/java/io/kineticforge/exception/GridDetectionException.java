package io.kineticforge.exception;

/**
 * Excepción lanzada cuando falla la detección de grilla.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class GridDetectionException extends KineticForgeException {

    private static final long serialVersionUID = 1L;

    public GridDetectionException(String message) {
        super(message);
    }

    public GridDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
