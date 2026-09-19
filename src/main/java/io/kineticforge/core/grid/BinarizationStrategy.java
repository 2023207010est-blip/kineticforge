package io.kineticforge.core.grid;

/**
 * Estrategias de binarización para preprocesar la imagen.
 *
 * <p>La binarización convierte una imagen en escala de grises a blanco
 * y negro puro, lo que simplifica la detección de líneas.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum BinarizationStrategy {

    /**
     * Método de Otsu: calcula el umbral óptimo automáticamente
     * analizando el histograma de la imagen.
     * <p>Recomendado para la mayoría de casos.</p>
     */
    OTSU,

    /**
     * Umbral fijo: todos los píxeles con luminancia menor a un valor
     * se consideran "oscuros". Rápido pero sensible a la iluminación.
     */
    FIXED_THRESHOLD,

    /**
     * Umbral adaptativo: calcula un umbral local por regiones.
     * Útil para imágenes con iluminación desigual.
     */
    ADAPTIVE
}
