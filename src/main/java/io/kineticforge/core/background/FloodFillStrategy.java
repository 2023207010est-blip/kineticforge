package io.kineticforge.core.background;

/**
 * Estrategias para el algoritmo de flood fill.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum FloodFillStrategy {

    /**
     * BFS (Breadth-First Search) usando una cola.
     * Procesa por niveles, más predecible y menos profundo en memoria.
     * <p>Recomendado para la mayoría de casos.</p>
     */
    BFS,

    /**
     * DFS (Depth-First Search) usando una pila.
     * Más rápido en algunos casos pero usa más memoria en imágenes grandes.
     */
    DFS
}
