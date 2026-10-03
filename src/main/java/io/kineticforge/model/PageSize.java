package io.kineticforge.model;

/**
 * Tamaños de página soportados por KineticForge.
 *
 * <p>Las dimensiones se expresan en milímetros y corresponden
 * al formato vertical (portrait). Para horizontal se intercambian
 * ancho y alto.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public enum PageSize {

    /** ISO A4: 210 mm × 297 mm. El formato más común. */
    A4(210.0, 297.0),

    /** ISO A3: 297 mm × 420 mm. Para más detalle. */
    A3(297.0, 420.0),

    /** ANSI Letter: 215.9 mm × 279.4 mm. Estándar en EE.UU. */
    LETTER(215.9, 279.4);

    private final double widthMm;
    private final double heightMm;

    PageSize(double widthMm, double heightMm) {
        this.widthMm = widthMm;
        this.heightMm = heightMm;
    }

    /**
     * @return ancho de la página en milímetros (en vertical).
     */
    public double getWidthMm() {
        return widthMm;
    }

    /**
     * @return alto de la página en milímetros (en vertical).
     */
    public double getHeightMm() {
        return heightMm;
    }

    /**
     * Devuelve el ancho efectivo según la orientación.
     *
     * @param orientation orientación deseada
     * @return ancho en milímetros
     */
    public double widthFor(PageOrientation orientation) {
        return orientation.isPortrait() ? widthMm : heightMm;
    }

    /**
     * Devuelve el alto efectivo según la orientación.
     *
     * @param orientation orientación deseada
     * @return alto en milímetros
     */
    public double heightFor(PageOrientation orientation) {
        return orientation.isPortrait() ? heightMm : widthMm;
    }
}
