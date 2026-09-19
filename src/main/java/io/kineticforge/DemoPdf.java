package io.kineticforge;

import io.kineticforge.io.TemplateGeneratorService;
import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import io.kineticforge.util.AppPaths;

import java.nio.file.Path;
import java.util.List;

/**
 * Clase temporal para verificar visualmente las plantillas generadas.
 * Se puede borrar después de verificar.
 */
public class DemoPdf {

    public static void main(String[] args) {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .marginMm(15.0)          // ← margen exterior de 15 mm
            .gutterMm(2.0)            // ← separación entre celdas
            .includeGuideDot(true)
            .build();

        TemplateGeneratorService service = new TemplateGeneratorService();
        List<Path> generados = service.generateAll(spec);

        System.out.println();
        System.out.println("=== Plantillas generadas ===");
        for (Path p : generados) {
            System.out.println("  " + p);
        }
        System.out.println();
        System.out.println("Carpeta de plantillas: " + AppPaths.getTemplatesDir());
        System.out.println();
        System.out.println("Para ver el PDF:");
        System.out.println("  xdg-open " + generados.get(0));
        System.out.println();
        System.out.println("Para ver el PNG:");
        System.out.println("  xdg-open " + generados.get(1));
    }
}
