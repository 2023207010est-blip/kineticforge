package io.kineticforge.core.grid;

import io.kineticforge.model.GridSpec;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

/**
 * Calcula los rectángulos (en píxeles) de cada celda de una hoja escaneada
 * a partir de las medidas exactas de la plantilla.
 *
 * <p>Los mm se convierten a píxeles usando la escala implícita del escaneo
 * (ancho en px / ancho de página en mm), asumiendo que el escaneo cubre la
 * página completa.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public final class CellCalculator {

    private CellCalculator() {
    }

    /**
     * @return celdas en orden de lectura (izquierda→derecha, arriba→abajo)
     */
    public static List<Rectangle> compute(GridSpec spec, int imageWidth, int imageHeight) {
        double pxPerMmX = imageWidth / spec.pageWidthMm();
        double pxPerMmY = imageHeight / spec.pageHeightMm();

        double marginPx = spec.marginMm() * pxPerMmX;
        double cellWpx = spec.cellWidthMm() * pxPerMmX;
        double cellHpx = spec.cellHeightMm() * pxPerMmY;
        double gutterPx = spec.gutterMm() * pxPerMmX;

        List<Rectangle> cells = new ArrayList<>(spec.totalCells());

        for (int row = 0; row < spec.rows(); row++) {
            for (int col = 0; col < spec.columns(); col++) {
                int x = (int) Math.round(marginPx + col * (cellWpx + gutterPx));
                int y = (int) Math.round(marginPx + row * (cellHpx + gutterPx));
                int w = (int) Math.round(cellWpx);
                int h = (int) Math.round(cellHpx);

                x = Math.max(0, Math.min(x, imageWidth - 1));
                y = Math.max(0, Math.min(y, imageHeight - 1));
                w = Math.min(w, imageWidth - x);
                h = Math.min(h, imageHeight - y);

                cells.add(new Rectangle(x, y, w, h));
            }
        }
        return cells;
    }

    /** Una sola celda que cubre toda la imagen (imagen suelta = 1 frame). */
    public static List<Rectangle> wholeImage(int imageWidth, int imageHeight) {
        List<Rectangle> cells = new ArrayList<>(1);
        cells.add(new Rectangle(0, 0, imageWidth, imageHeight));
        return cells;
    }
}
