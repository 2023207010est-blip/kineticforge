package io.kineticforge.exception;

/**
 * Excepción lanzada cuando falla la generación de un PDF.
 *
 * <p>Se usa para envolver errores de I/O de PDFBox o errores
 * de lógica relacionados con la creación de plantillas.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class PdfGenerationException extends KineticForgeException {

    private static final long serialVersionUID = 1L;

    public PdfGenerationException(String message) {
        super(message);
    }

    public PdfGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
