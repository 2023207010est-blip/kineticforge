package io.kineticforge.io;

import io.kineticforge.exception.ImageProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Cargador de imágenes desde disco.
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class ImageLoader {

    private static final Logger log = LoggerFactory.getLogger(ImageLoader.class);

    public BufferedImage load(Path path) {
        Objects.requireNonNull(path, "path no puede ser nulo");

        if (!Files.exists(path)) {
            throw new ImageProcessingException("El archivo no existe: " + path);
        }

        if (!Files.isRegularFile(path)) {
            throw new ImageProcessingException("La ruta no es un archivo: " + path);
        }

        try {
            log.debug("Cargando imagen: {}", path);

            BufferedImage image = ImageIO.read(path.toFile());
            if (image == null) {
                throw new ImageProcessingException(
                        "Formato de imagen no soportado: " + path.getFileName());
            }

            log.info("Imagen cargada: {} ({}x{} px)",
                    path.getFileName(), image.getWidth(), image.getHeight());

            return image;

        } catch (IOException e) {
            throw new ImageProcessingException(
                    "Error leyendo el archivo: " + path, e);
        }
    }

    public BufferedImage load(String path) {
        return load(Path.of(path));
    }

    public boolean canLoad(Path path) {
        try {
            return ImageIO.read(path.toFile()) != null;
        } catch (IOException e) {
            return false;
        }
    }
}
