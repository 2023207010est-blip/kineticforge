package io.kineticforge.core.grid;

import io.kineticforge.model.GridSpec;

import java.awt.Rectangle;
import java.util.List;

/**
 * Resultado de la detección de grilla en una imagen.
 *
 * <p>Contiene los rectángulos de cada celda en orden de lectura
 * (fila por fila, izquierda a derecha), más metadata sobre la
 * confianza de la detección.</p>
 *
 * @param spec         especificación de la grilla usada
 * @param cells        rectángulos de las celdas en orden de lectura
 * @param confidence   confianza de la detección (0.0 a 1.0)
 * @param imageWidth   ancho de la imagen original en píxeles
 * @param imageHeight  alto de la imagen original en píxeles
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public record GridDetectionResult(
    GridSpec spec,
    List<Rectangle> cells,
    double confidence,
    int imageWidth,
    int imageHeight
) {

    /**
     * Constructor compacto con validaciones.
     */
    public GridDetectionResult {
        if (spec == null) {
            throw new IllegalArgumentException("spec no puede ser nulo");
        }
        if (cells == null) {
            throw new IllegalArgumentException("cells no puede ser nulo");
        }
        if (confidence < 0.0 || confidence > 1.0) {
            throw new IllegalArgumentException(
                "confidence debe estar entre 0.0 y 1.0, se recibió: " + confidence);
        }
        if (imageWidth <= 0 || imageHeight <= 0) {
            throw new IllegalArgumentException(
                "Las dimensiones de la imagen deben ser positivas");
        }
        // Hacer la lista inmutable por seguridad
        cells = List.copyOf(cells);
    }

    /**
     * @return {@code true} si la confianza es alta (≥ 0.7).
     */
    public boolean isReliable() {
        return confidence >= 0.7;
    }

    /**
     * @return cantidad de celdas detectadas.
     */
    public int cellCount() {
        return cells.size();
    }

    /**
     * @return {@code true} si se detectaron todas las celdas esperadas.
     */
    public boolean isComplete() {
        return cells.size() == spec.totalCells();
    }

    /**
     * @return {@code true} si la detección es confiable Y completa.
     */
    public boolean isSuccessful() {
        return isReliable() && isComplete();
    }

    /**
     * @return rectángulo de la celda en la posición indicada.
     */
    public Rectangle cellAt(int index) {
        return cells.get(index);
    }

    /**
     * @return rectángulo de la celda en la fila y columna indicadas.
     */
    public Rectangle cellAt(int row, int col) {
        int index = row * spec.columns() + col;
        return cells.get(index);
    }
}
