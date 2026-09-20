package io.kineticforge.core.export;

import io.kineticforge.exception.ExportException;
import io.kineticforge.model.ExportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Exportador de animaciones en formato GIF.
 *
 * <p>Utiliza {@code javax.imageio} nativo para escribir GIFs animados
 * con soporte de transparencia (1 bit) y loop infinito.</p>
 *
 * <p>Nota sobre transparencia: GIF solo soporta 1 bit de alpha.
 * Los píxeles con alpha menor a 128 se vuelven completamente transparentes;
 * el resto se vuelven opacos.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class GifExporter implements Exporter {

    private static final Logger log = LoggerFactory.getLogger(GifExporter.class);

    /** Umbral de alpha para considerar un píxel transparente (GIF es 1 bit). */
    private static final int ALPHA_THRESHOLD = 128;

    @Override
    public String getFormatName() {
        return "GIF";
    }

    @Override
    public String getFileExtension() {
        return "gif";
    }

    @Override
    public boolean supportsTransparency() {
        return true;
    }

    @Override
    public void export(List<BufferedImage> frames, ExportConfig config, Path output) {
        Objects.requireNonNull(frames, "frames no puede ser nulo");
        Objects.requireNonNull(config, "config no puede ser nulo");
        Objects.requireNonNull(output, "output no puede ser nulo");

        if (frames.isEmpty()) {
            throw new ExportException("No hay frames para exportar");
        }

        log.info("Exportando GIF: {} frames, {} fps, loop={}",
            frames.size(), config.fps(), config.loop());

        // 1. Convertir todos los frames a formato compatible con GIF
        List<BufferedImage> gifFrames = frames.stream()
            .map(this::prepareForGif)
            .toList();

        // 2. Verificar que todos tengan el mismo tamaño
        int width = gifFrames.get(0).getWidth();
        int height = gifFrames.get(0).getHeight();

        for (int i = 1; i < gifFrames.size(); i++) {
            BufferedImage f = gifFrames.get(i);
            if (f.getWidth() != width || f.getHeight() != height) {
                throw new ExportException(String.format(
                    "Todos los frames deben tener el mismo tamaño. "
                        + "Frame 0: %dx%d, Frame %d: %dx%d",
                    width, height, i, f.getWidth(), f.getHeight()));
            }
        }

        // 3. Asegurar que exista el directorio padre
        try {
            Path parent = output.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
        } catch (IOException e) {
            throw new ExportException("No se pudo crear el directorio: " + output, e);
        }

        // 4. Obtener el writer de GIF
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("gif");
        if (!writers.hasNext()) {
            throw new ExportException("No hay writer de GIF disponible");
        }
        ImageWriter writer = writers.next();

        // 5. Escribir el GIF
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output.toFile())) {
            writer.setOutput(ios);

            ImageWriteParam params = writer.getDefaultWriteParam();
            IIOMetadata metadata = writer.getDefaultImageMetadata(
                ImageTypeSpecifier.createFromBufferedImageType(
                    BufferedImage.TYPE_INT_ARGB),
                params);

            configureMetadata(metadata, config);
            writer.prepareWriteSequence(null);

            // Escribir cada frame
            for (int i = 0; i < gifFrames.size(); i++) {
                BufferedImage frame = gifFrames.get(i);
                IIOImage image = new IIOImage(frame, null, metadata);
                writer.writeToSequence(image, params);
            }

            writer.endWriteSequence();
            log.info("GIF exportado: {}", output);

        } catch (IOException e) {
            throw new ExportException("No se pudo escribir el GIF en: " + output, e);
        } finally {
            writer.dispose();
        }
    }

    // ============================================================
    // Preparación de frames
    // ============================================================

    /**
     * Prepara un frame para GIF: convierte a formato RGB y aplica
     * transparencia 1 bit.
     */
    private BufferedImage prepareForGif(BufferedImage frame) {
        int width = frame.getWidth();
        int height = frame.getHeight();

        // Usamos TYPE_INT_ARGB: el writer de GIF va a mapear alpha<128
        // a transparente automáticamente
        BufferedImage gifFrame = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = frame.getRGB(x, y);
                int alpha = (argb >> 24) & 0xFF;

                if (alpha < ALPHA_THRESHOLD) {
                    // Transparente
                    gifFrame.setRGB(x, y, 0x00000000);
                } else {
                    // Opaco (sin alpha)
                    gifFrame.setRGB(x, y, 0xFF000000 | (argb & 0x00FFFFFF));
                }
            }
        }

        return gifFrame;
    }

    // ============================================================
    // Configuración de metadatos
    // ============================================================

    /**
     * Configura los metadatos del GIF: loop y delay.
     */
    private void configureMetadata(IIOMetadata metadata, ExportConfig config) {
        String formatName = metadata.getNativeMetadataFormatName();
        IIOMetadataNode root = (IIOMetadataNode) metadata.getAsTree(formatName);

        // Loop (NETSCAPE 2.0)
        IIOMetadataNode appExtensions = getOrCreateNode(root, "ApplicationExtensions");
        IIOMetadataNode appNode = new IIOMetadataNode("ApplicationExtension");
        appNode.setAttribute("applicationID", "NETSCAPE");
        appNode.setAttribute("authenticationCode", "2.0");

        int loopCount = config.loop() ? 0 : 1;
        byte[] loopBytes = new byte[]{
            0x1,                 // sub-block size
            (byte) loopCount,    // 0 = infinito, 1 = una vez
            0x0                  // terminator
        };
        appNode.setUserObject(loopBytes);
        appExtensions.appendChild(appNode);

        // Delay entre frames (GraphicControlExtension)
        int delayCentisec = (int) Math.round(config.getFrameDelayMs() / 10.0);

        IIOMetadataNode gce = getOrCreateNode(root, "GraphicControlExtension");
        gce.setAttribute("disposalMethod", "restoreToBackgroundColor");
        gce.setAttribute("userInputFlag", "FALSE");
        gce.setAttribute("transparentColorFlag", "TRUE");
        gce.setAttribute("delayTime", String.valueOf(delayCentisec));
        gce.setAttribute("transparentColorIndex", "0");

        try {
            metadata.setFromTree(formatName, root);
        } catch (IIOInvalidTreeException e) {
            throw new ExportException("Error configurando metadatos del GIF", e);
        }
    }

    private IIOMetadataNode getOrCreateNode(IIOMetadataNode root, String nodeName) {
        for (int i = 0; i < root.getLength(); i++) {
            if (root.item(i).getNodeName().equalsIgnoreCase(nodeName)) {
                return (IIOMetadataNode) root.item(i);
            }
        }
        IIOMetadataNode node = new IIOMetadataNode(nodeName);
        root.appendChild(node);
        return node;
    }
}
