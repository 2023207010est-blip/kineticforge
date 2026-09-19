package io.kineticforge.core.grid;

import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("GridDetector - Detección automática de grilla")
class GridDetectorTest {

    /** DPI para las imágenes de prueba. */
    private static final int TEST_DPI = 72;

    /** Grosor de línea de la grilla sintética (en px). */
    private static final float LINE_WIDTH = 2.0f;

    @Test
    @DisplayName("Detecta correctamente una grilla 8x6 en imagen sintética")
    void detectsEightBySixGridInSyntheticImage() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)
            .gutterMm(2.0)
            .includeGuideDot(false)
            .build();

        BufferedImage image = createSyntheticGridImage(spec);
        GridDetector detector = new GridDetector();
        GridDetectionResult result = detector.detect(image, spec);

        assertNotNull(result);
        assertEquals(spec.totalCells(), result.cellCount(),
            "Debe detectar " + spec.totalCells() + " celdas");
        assertTrue(result.isComplete(), "La detección debe estar completa");
        assertTrue(result.isReliable(), "La confianza debe ser ≥ 0.7");
    }

    @Test
    @DisplayName("Detecta correctamente grilla 4x3 (DETALLE)")
    void detectsFourByThreeGrid() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.DETALLE)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)
            .includeGuideDot(false)
            .build();

        BufferedImage image = createSyntheticGridImage(spec);
        GridDetectionResult result = new GridDetector().detect(image, spec);

        assertEquals(12, result.cellCount());
        assertTrue(result.isSuccessful());
    }

    @Test
    @DisplayName("Detecta correctamente grilla 6x4 (ESTANDAR)")
    void detectsSixByFourGrid() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.ESTANDAR)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)
            .includeGuideDot(false)
            .build();

        BufferedImage image = createSyntheticGridImage(spec);
        GridDetectionResult result = new GridDetector().detect(image, spec);

        assertEquals(24, result.cellCount());
        assertTrue(result.isSuccessful());
    }

    @Test
    @DisplayName("Detecta correctamente grilla 12x8 (DENSA)")
    void detectsTwelveByEightGrid() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.DENSA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)
            .includeGuideDot(false)
            .build();

        BufferedImage image = createSyntheticGridImage(spec);
        GridDetectionResult result = new GridDetector().detect(image, spec);

        assertEquals(96, result.cellCount());
        assertTrue(result.isSuccessful());
    }

    @Test
    @DisplayName("Las celdas detectadas son rectangulares y positivas")
    void detectedCellsAreValidRectangles() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)
            .includeGuideDot(false)
            .build();

        BufferedImage image = createSyntheticGridImage(spec);
        GridDetectionResult result = new GridDetector().detect(image, spec);

        for (int i = 0; i < result.cellCount(); i++) {
            Rectangle cell = result.cellAt(i);
            assertTrue(cell.width > 0, "Celda " + i + " debe tener ancho positivo");
            assertTrue(cell.height > 0, "Celda " + i + " debe tener alto positivo");
        }
    }

    @Test
    @DisplayName("Las celdas están ordenadas de izquierda a derecha, arriba a abajo")
    void cellsAreInReadingOrder() {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)
            .includeGuideDot(false)
            .build();

        BufferedImage image = createSyntheticGridImage(spec);
        GridDetectionResult result = new GridDetector().detect(image, spec);

        Rectangle first = result.cellAt(0);
        Rectangle second = result.cellAt(1);

        assertTrue(second.x > first.x,
            "La 2da celda debe estar a la derecha de la 1ra");
        assertEquals(first.y, second.y, 2,
            "La 2da celda debe estar en la misma fila");

        Rectangle ninth = result.cellAt(8);
        assertTrue(ninth.y > first.y,
            "La 9na celda debe estar debajo de la 1ra");
    }

    @Test
    @DisplayName("Lanza excepción con imagen nula")
    void throwsOnNullImage() {
        GridSpec spec = GridSpec.defaults();
        assertThrows(NullPointerException.class,
            () -> new GridDetector().detect(null, spec));
    }

    @Test
    @DisplayName("Lanza excepción con spec nulo")
    void throwsOnNullSpec() {
        BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        assertThrows(NullPointerException.class,
            () -> new GridDetector().detect(image, null));
    }

    // ============================================================
    // Helper: crear imagen sintética con grilla
    // ============================================================

    /**
     * Genera una imagen sintética con la grilla dibujada como líneas
     * continuas (simulando la plantilla real).
     */
    private BufferedImage createSyntheticGridImage(GridSpec spec) {
        int widthPx = mmToPx(spec.pageWidthMm());
        int heightPx = mmToPx(spec.pageHeightMm());

        BufferedImage image = new BufferedImage(widthPx, heightPx,
            BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // Fondo blanco
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, widthPx, heightPx);

        // Dibujar líneas de la grilla
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(LINE_WIDTH));

        float marginPx = mmToPx(spec.marginMm());
        float cellW = mmToPx(spec.cellWidthMm());
        float cellH = mmToPx(spec.cellHeightMm());
        float gutter = mmToPx(spec.gutterMm());

        int rows = spec.rows();
        int cols = spec.columns();

        float gridWidth = cols * cellW + (cols - 1) * gutter;
        float gridHeight = rows * cellH + (rows - 1) * gutter;
        float gridRight = marginPx + gridWidth;
        float gridBottom = marginPx + gridHeight;

        // Líneas verticales continuas
        for (int c = 0; c <= cols; c++) {
            float x = marginPx + c * (cellW + gutter);
            g.drawLine(
                Math.round(x), Math.round(marginPx),
                Math.round(x), Math.round(gridBottom));
        }

        // Líneas horizontales continuas
        for (int r = 0; r <= rows; r++) {
            float y = marginPx + r * (cellH + gutter);
            g.drawLine(
                Math.round(marginPx), Math.round(y),
                Math.round(gridRight), Math.round(y));
        }

        g.dispose();
        return image;
    }

    private int mmToPx(double mm) {
        return (int) Math.round(mm * TEST_DPI / 25.4);
    }
}
