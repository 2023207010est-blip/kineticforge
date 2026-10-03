package io.kineticforge.model;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/**
 * Una hoja (o imagen suelta) cargada por el usuario, junto con su grilla.
 *
 * <p>Una animación se arma con una o varias hojas: los frames de cada hoja
 * se concatenan, en el orden de la lista, en una única progresión.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class SheetScan {

    private final Path path;
    private final BufferedImage image;

    private GridMetadata metadata;
    private GridSpec spec;
    private List<Rectangle> cells;
    private boolean wholeImage;

    public SheetScan(Path path, BufferedImage image) {
        this.path = Objects.requireNonNull(path, "path no puede ser nulo");
        this.image = Objects.requireNonNull(image, "image no puede ser nula");
    }

    public Path getPath() { return path; }
    public BufferedImage getImage() { return image; }
    public String getFileName() { return path.getFileName().toString(); }

    public GridMetadata getMetadata() { return metadata; }
    public void setMetadata(GridMetadata metadata) { this.metadata = metadata; }

    public GridSpec getSpec() { return spec; }

    public List<Rectangle> getCells() { return cells; }

    public boolean isWholeImage() { return wholeImage; }

    public boolean hasGrid() {
        return cells != null && !cells.isEmpty();
    }

    /** Define la grilla de esta hoja. */
    public void setGrid(GridSpec spec, List<Rectangle> cells) {
        this.spec = spec;
        this.cells = List.copyOf(cells);
        this.wholeImage = false;
    }

    /** Marca la imagen completa como un único frame. */
    public void setWholeImage(List<Rectangle> singleCell) {
        this.spec = null;
        this.cells = List.copyOf(singleCell);
        this.wholeImage = true;
    }

    public void clearGrid() {
        this.spec = null;
        this.cells = null;
        this.wholeImage = false;
    }

    public int frameCount() {
        return cells == null ? 0 : cells.size();
    }

    /** Texto para mostrar en listas. */
    public String describe() {
        StringBuilder sb = new StringBuilder(getFileName());
        sb.append("  —  ").append(image.getWidth()).append("×").append(image.getHeight()).append(" px");
        if (hasGrid()) {
            sb.append("  ·  ").append(wholeImage ? "1 frame (imagen suelta)"
                    : frameCount() + " celdas");
        } else {
            sb.append("  ·  sin grilla");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return describe();
    }
}
