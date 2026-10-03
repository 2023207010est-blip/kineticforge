package io.kineticforge.io;

import io.kineticforge.model.GridSpec;
import io.kineticforge.util.AppPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.List;

/**
 * Servicio que orquesta la generación de plantillas.
 *
 * <p>Genera la plantilla en todos los formatos soportados y las guarda
 * en la carpeta estándar {@code ~/kineticforge/plantillas/}.</p>
 *
 * @author KineticForge Team
 * @version 1.0.0
 * @since 2026
 */
public class TemplateGeneratorService {

    private static final Logger log = LoggerFactory.getLogger(TemplateGeneratorService.class);

    private final List<TemplateRenderer> renderers;

    public TemplateGeneratorService() {
        this.renderers = List.of(
            new PdfTemplateRenderer(),
            new PngTemplateRenderer()
        );
    }

    /**
     * Genera la plantilla en todos los formatos soportados y las guarda
     * en la carpeta estándar.
     *
     * @param spec especificación de la grilla
     * @return lista de rutas generadas
     */
    public List<Path> generateAll(GridSpec spec) {
        AppPaths.ensureDirectories();

        String baseName = buildBaseName(spec);
        Path templatesDir = AppPaths.getTemplatesDir();

        return renderers.stream()
            .map(r -> {
                Path output = templatesDir.resolve(
                    baseName + "." + r.getFileExtension());
                r.render(spec, output);
                log.info("Plantilla {} generada: {}", r.getFormatName(), output);
                return output;
            })
            .toList();
    }

    /**
     * Genera la plantilla en un formato específico.
     *
     * @param spec         especificación de la grilla
     * @param formatName   "PDF" o "PNG"
     * @return ruta del archivo generado
     */
    public Path generateOne(GridSpec spec, String formatName) {
        AppPaths.ensureDirectories();

        TemplateRenderer renderer = renderers.stream()
            .filter(r -> r.getFormatName().startsWith(formatName))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException(
                "Formato no soportado: " + formatName));

        String baseName = buildBaseName(spec);
        Path output = AppPaths.getTemplatesDir()
            .resolve(baseName + "." + renderer.getFileExtension());

        renderer.render(spec, output);
        return output;
    }

    /**
     * Construye un nombre de archivo predecible basado en la spec.
     *
     * <p>Formato: {@code <preset>-<pageSize>-<orientation>}</p>
     * <p>Ejemplo: {@code compacta-a4-horizontal}</p>
     */
    public String buildBaseName(GridSpec spec) {
        String preset = spec.preset().name().toLowerCase().replace('_', '-');
        String page = spec.pageSize().name().toLowerCase();
        String orientation = spec.orientation().isLandscape() ? "horizontal" : "vertical";
        return preset + "-" + page + "-" + orientation;
    }
}
