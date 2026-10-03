package io.kineticforge.io;

import io.kineticforge.exception.ImageProcessingException;
import io.kineticforge.model.GridMetadata;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.GuideDotColor;
import io.kineticforge.model.GuideDotStyle;
import io.kineticforge.util.MetadataPaths;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Renderizador de plantillas PNG minimalista.
 *
 * @author KineticForge Team
 * @version 2.1.0
 * @since 2026
 */
public class PngTemplateRenderer implements TemplateRenderer {

    public static final int DEFAULT_DPI = 300;
    private static final double MM_PER_INCH = 25.4;
    private static final float GRID_LINE_WIDTH_MM = 0.15f;
    private static final float FOOTER_HEIGHT_MM = 10.0f;
    private static final int FOOTER_FONT_SIZE_PT = 7;
    private static final Color GRID_COLOR = new Color(0xCC, 0xCC, 0xCC);
    private static final Color FOOTER_COLOR = new Color(0x8C, 0x8C, 0x8C);

    private final int dpi;

    public PngTemplateRenderer() {
        this(DEFAULT_DPI);
    }

    public PngTemplateRenderer(int dpi) {
        if (dpi <= 0) {
            throw new IllegalArgumentException("El DPI debe ser positivo");
        }
        this.dpi = dpi;
    }

    @Override
    public String getFileExtension() {
        return "png";
    }

    @Override
    public String getFormatName() {
        return "PNG";
    }

    @Override
    public void render(GridSpec spec, Path output) {
        Objects.requireNonNull(spec, "spec no puede ser nulo");
        Objects.requireNonNull(output, "output no puede ser nulo");

        int widthPx = mmToPxInt(spec.pageWidthMm());
        int heightPx = mmToPxInt(spec.pageHeightMm());

        BufferedImage image = new BufferedImage(widthPx, heightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                    RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, widthPx, heightPx);

            drawGrid(g, spec, heightPx);
            drawGuideDots(g, spec, heightPx);
        } finally {
            g.dispose();
        }

        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            ImageIO.write(image, "png", output.toFile());

            // Guardar metadatos JSON asociados
            GridMetadata metadata = GridMetadata.from(spec);
            Path metadataPath = MetadataPaths.forTemplate(output);
            metadata.save(metadataPath);

        } catch (IOException e) {
            throw new ImageProcessingException(
                    "No se pudo guardar el PNG en: " + output, e);
        }
    }

    private int mmToPxInt(double mm) {
        return (int) Math.round(mm * dpi / MM_PER_INCH);
    }

    private float mmToPxFloat(double mm) {
        return (float) (mm * dpi / MM_PER_INCH);
    }

    private float gridOriginX(GridSpec spec) {
        int widthPx = mmToPxInt(spec.pageWidthMm());
        float marginPx = mmToPxFloat(spec.marginMm());
        float totalGridW = spec.columns() * cellWidthPx(spec)
                + (spec.columns() - 1) * gutterPx(spec);
        float availableW = widthPx - 2 * marginPx;
        float offset = Math.max(0f, (availableW - totalGridW) / 2.0f);
        return marginPx + offset;
    }

    private float gridTopY(GridSpec spec, int heightPx) {
        float marginPx = mmToPxFloat(spec.marginMm());
        float totalGridH = spec.rows() * cellHeightPx(spec)
                + (spec.rows() - 1) * gutterPx(spec);
        float availableH = heightPx - 2 * marginPx;
        float offset = Math.max(0f, (availableH - totalGridH) / 2.0f);
        return heightPx - marginPx - offset;
    }

    private float cellWidthPx(GridSpec spec) {
        return mmToPxFloat(spec.cellWidthMm());
    }

    private float cellHeightPx(GridSpec spec) {
        return mmToPxFloat(spec.cellHeightMm());
    }

    private float gutterPx(GridSpec spec) {
        return mmToPxFloat(spec.gutterMm());
    }

    private float cellX(GridSpec spec, int col) {
        return gridOriginX(spec) + col * (cellWidthPx(spec) + gutterPx(spec));
    }

    private float cellY(GridSpec spec, int row, int heightPx) {
        return gridTopY(spec, heightPx)
                - (row + 1) * cellHeightPx(spec)
                - row * gutterPx(spec);
    }

    private void drawGrid(Graphics2D g, GridSpec spec, int heightPx) {
        g.setColor(GRID_COLOR);
        g.setStroke(new BasicStroke(mmToPxFloat(GRID_LINE_WIDTH_MM)));

        int rows = spec.rows();
        int cols = spec.columns();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                g.drawRect(
                        Math.round(cellX(spec, c)),
                        Math.round(cellY(spec, r, heightPx)),
                        Math.round(cellWidthPx(spec)),
                        Math.round(cellHeightPx(spec)));
            }
        }
    }

    private void drawGuideDots(Graphics2D g, GridSpec spec, int heightPx) {
        if (!spec.includeGuideDot() || spec.guideDotStyle() == GuideDotStyle.NONE) {
            return;
        }

        GuideDotColor color = spec.guideDotColor();
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue()));
        g.setStroke(new BasicStroke(mmToPxFloat(GRID_LINE_WIDTH_MM)));

        float radius = mmToPxFloat(spec.guideDotDiameterMm()) / 2.0f;
        int rows = spec.rows();
        int cols = spec.columns();

        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                float cx = cellX(spec, c) + cellWidthPx(spec) / 2.0f;
                float cy = cellY(spec, r, heightPx) + cellHeightPx(spec) / 2.0f;

                GuideDotStyle style = spec.guideDotStyle();
                if (style == GuideDotStyle.CIRCLE || style == GuideDotStyle.BOTH) {
                    g.draw(new Ellipse2D.Float(cx - radius, cy - radius,
                            radius * 2, radius * 2));
                }
                if (style == GuideDotStyle.CROSS || style == GuideDotStyle.BOTH) {
                    g.drawLine(Math.round(cx - radius), Math.round(cy),
                            Math.round(cx + radius), Math.round(cy));
                    g.drawLine(Math.round(cx), Math.round(cy - radius),
                            Math.round(cx), Math.round(cy + radius));
                }
            }
        }
    }
}
