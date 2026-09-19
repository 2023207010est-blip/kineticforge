package io.kineticforge.exception;

/**
 * Excepción base de KineticForge.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class KineticForgeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public KineticForgeException(String message) {
        super(message);
    }

    public KineticForgeException(String message, Throwable cause) {
        super(message, cause);
    }
}
