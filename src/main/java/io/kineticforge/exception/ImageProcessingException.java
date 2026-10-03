package io.kineticforge.exception;

/**
 * Excepción lanzada durante el procesamiento de imágenes.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class ImageProcessingException extends KineticForgeException {

    private static final long serialVersionUID = 1L;

    public ImageProcessingException(String message) {
        super(message);
    }

    public ImageProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
