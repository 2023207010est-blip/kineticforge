package io.kineticforge.model;

/**
 * Estilo visual del punto guía central que se dibuja en cada celda.
 *
 * <p>El punto guía sirve al animador como referencia para centrar
 * el personaje, y al software como punto fiducial para alinear
 * los frames durante el procesamiento.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum GuideDotStyle {

    /** Sin punto guía. La plantilla queda limpia. */
    NONE,

    /** Círculo hueco (más discreto). */
    CIRCLE,

    /** Cruz pequeña (más fácil de centrar visualmente). */
    CROSS,

    /** Círculo + cruz en el centro (más visible). */
    BOTH
}
