package io.kineticforge.io;

import io.kineticforge.exception.PdfGenerationException;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.GuideDotColor;
import io.kineticforge.model.GuideDotStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import io.kineticforge.model.GridMetadata;
import io.kineticforge.util.MetadataPaths;
/**
 * Renderizador de plantillas PDF minimalista.
 *
 * <p>Usa EXACTAMENTE el mismo margen y gutter que el {@link GridSpec}
 * para garantizar que las celdas caben siempre en el espacio disponible.</p>
 *
 * @author KineticForge Team
 * @version 2.1.0
 * @since 2026
 */
public class PdfTemplateRenderer implements TemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(PdfTemplateRenderer.class);

    public static final float MM_TO_POINTS = 72.0f / 25.4f;

    private static final float GRID_LINE_WIDTH = 0.4f;
    private static final float FOOTER_FONT_SIZE = 6.5f;

    /** Espacio reservado al pie, en mm. */
    private static final float FOOTER_HEIGHT_MM = 0.0f;

    private static final float[] GRID_COLOR = {0.80f, 0.80f, 0.80f};
    private static final float[] FOOTER_COLOR = {0.55f, 0.55f, 0.55f};

    @Override
    public String getFileExtension() {
        return "pdf";
    }

    @Override
    public String getFormatName() {
        return "PDF";
    }

    @Override
    public void render(GridSpec spec, Path output) {
        Objects.requireNonNull(spec, "spec no puede ser nulo");
        Objects.requireNonNull(output, "output no puede ser nulo");

        log.info("Generando plantilla PDF: preset={}, orientación={}, página={}",
            spec.preset(), spec.orientation(), spec.pageSize());

        try (PDDocument document = new PDDocument()) {
            PDPage page = createPage(spec);
            document.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                drawGrid(cs, spec);
                drawGuideDots(cs, spec);
                //drawFooter(cs, spec);
            }

            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            document.save(output.toFile());
            log.info("Plantilla PDF generada: {}", output);

            // Guardar metadatos JSON asociados
            try {
                GridMetadata metadata = GridMetadata.from(spec);
                Path metadataPath = MetadataPaths.forTemplate(output);
                metadata.save(metadataPath);
                log.info("Metadatos JSON generados: {}", metadataPath);
            } catch (IOException e) {
                log.warn("No se pudieron guardar los metadatos JSON: {}", e.getMessage());
            }

        } catch (IOException e) {
            throw new PdfGenerationException("No se pudo generar el PDF en: " + output, e);
        }
    }

    // ============================================================
    // Página
    // ============================================================

    private PDPage createPage(GridSpec spec) {
        float w = (float) (spec.pageWidthMm() * MM_TO_POINTS);
        float h = (float) (spec.pageHeightMm() * MM_TO_POINTS);
        return new PDPage(new PDRectangle(w, h));
    }

    // ============================================================
    // Coordenadas
    // ============================================================

    /**
     * X del borde izquierdo de la grilla: el margen del spec, centrado.
     * Como el spec ya reserva el margen, acá solo lo aplicamos.
     */
    private float gridOriginX(GridSpec spec) {
        float pageW = (float) (spec.pageWidthMm() * MM_TO_POINTS);
        float marginPt = (float) (spec.marginMm() * MM_TO_POINTS);

        float totalGridW = spec.columns() * cellWidthPt(spec)
            + (spec.columns() - 1) * gutterPt(spec);

        float availableW = pageW - 2 * marginPt;
        float offset = Math.max(0f, (availableW - totalGridW) / 2.0f);

        return marginPt + offset;
    }

    /**
     * Y del borde superior de la grilla, centrada verticalmente.
     */
    private float gridTopY(GridSpec spec) {
        float pageH = (float) (spec.pageHeightMm() * MM_TO_POINTS);
        float marginPt = (float) (spec.marginMm() * MM_TO_POINTS);

        float totalGridH = spec.rows() * cellHeightPt(spec)
            + (spec.rows() - 1) * gutterPt(spec);

        float availableH = pageH - 2 * marginPt;
        float offset = Math.max(0f, (availableH - totalGridH) / 2.0f);

        return pageH - marginPt - offset;
    }

    private float cellWidthPt(GridSpec spec) {
        return (float) (spec.cellWidthMm() * MM_TO_POINTS);
    }

    private float cellHeightPt(GridSpec spec) {
        return (float) (spec.cellHeightMm() * MM_TO_POINTS);
    }

    private float gutterPt(GridSpec spec) {
        return (float) (spec.gutterMm() * MM_TO_POINTS);
    }

    private float cellX(GridSpec spec, int col) {
        return gridOriginX(spec) + col * (cellWidthPt(spec) + gutterPt(spec));
    }

    private float cellY(GridSpec spec, int row) {
        return gridTopY(spec)
            - (row + 1) * cellHeightPt(spec)
            - row * gutterPt(spec);
    }

    // ============================================================
    // Dibujo
    // ============================================================

    private void drawGrid(PDPageContentStream cs, GridSpec spec) throws IOException {
        cs.setStrokingColor(GRID_COLOR[0], GRID_COLOR[1], GRID_COLOR[2]);
        cs.setLineWidth(GRID_LINE_WIDTH);

        int rows = spec.rows();
        int cols = spec.columns();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                cs.addRect(cellX(spec, c), cellY(spec, r),
                    cellWidthPt(spec), cellHeightPt(spec));
            }
        }
        cs.stroke();
    }

    private void drawGuideDots(PDPageContentStream cs, GridSpec spec) throws IOException {
        if (!spec.includeGuideDot() || spec.guideDotStyle() == GuideDotStyle.NONE) {
            return;
        }

        GuideDotColor color = spec.guideDotColor();
        cs.setStrokingColor(color.getRed() / 255.0f,
            color.getGreen() / 255.0f,
            color.getBlue() / 255.0f);
        cs.setLineWidth(GRID_LINE_WIDTH);

        float dotDiameterPt = (float) (spec.guideDotDiameterMm() * MM_TO_POINTS);
        float dotRadius = dotDiameterPt / 2.0f;

        int rows = spec.rows();
        int cols = spec.columns();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cx = cellX(spec, c) + cellWidthPt(spec) / 2.0f;
                float cy = cellY(spec, r) + cellHeightPt(spec) / 2.0f;

                GuideDotStyle style = spec.guideDotStyle();
                if (style == GuideDotStyle.CIRCLE || style == GuideDotStyle.BOTH) {
                    drawCircle(cs, cx, cy, dotRadius);
                }
                if (style == GuideDotStyle.CROSS || style == GuideDotStyle.BOTH) {
                    drawCross(cs, cx, cy, dotRadius);
                }
            }
        }
    }

    private void drawCircle(PDPageContentStream cs, float cx, float cy, float r)
        throws IOException {
        float k = 0.5523f * r;
        cs.moveTo(cx - r, cy);
        cs.curveTo(cx - r, cy + k, cx - k, cy + r, cx, cy + r);
        cs.curveTo(cx + k, cy + r, cx + r, cy + k, cx + r, cy);
        cs.curveTo(cx + r, cy - k, cx + k, cy - r, cx, cy - r);
        cs.curveTo(cx - k, cy - r, cx - r, cy - k, cx - r, cy);
        cs.closePath();
        cs.stroke();
    }

    private void drawCross(PDPageContentStream cs, float cx, float cy, float r)
        throws IOException {
        cs.moveTo(cx - r, cy);
        cs.lineTo(cx + r, cy);
        cs.moveTo(cx, cy - r);
        cs.lineTo(cx, cy + r);
        cs.stroke();
    }

    private void drawFooter(PDPageContentStream cs, GridSpec spec) throws IOException {
        PDFont font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        cs.setNonStrokingColor(FOOTER_COLOR[0], FOOTER_COLOR[1], FOOTER_COLOR[2]);

        String text = String.format(
            "KineticForge · %s · %d celdas · %s",
            spec.preset().getDisplayName(),
            spec.totalCells(),
            spec.includeGuideDot() ? "con punto guía" : "sin punto guía");

        float marginPt = (float) (spec.marginMm() * MM_TO_POINTS);
        float footerY = (FOOTER_HEIGHT_MM * MM_TO_POINTS) / 3.0f;

        cs.beginText();
        cs.setFont(font, FOOTER_FONT_SIZE);
        cs.newLineAtOffset(marginPt, footerY);
        cs.showText(text);
        cs.endText();
    }
}
