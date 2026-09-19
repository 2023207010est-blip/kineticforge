package io.kineticforge.io;

import io.kineticforge.model.GridPreset;
import io.kineticforge.model.GridSpec;
import io.kineticforge.model.PageOrientation;
import io.kineticforge.model.PageSize;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("PdfTemplateRenderer - Generación de plantillas PDF")
class PdfTemplateRendererTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Genera un PDF válido con la configuración por defecto")
    void generatesValidPdfWithDefaults() throws IOException {
        GridSpec spec = GridSpec.defaults();
        Path output = tempDir.resolve("plantilla-default.pdf");

        new PdfTemplateRenderer().render(spec, output);

        assertTrue(Files.exists(output), "El PDF debe existir");
        assertTrue(Files.size(output) > 0, "El PDF no debe estar vacío");

        try (PDDocument doc = Loader.loadPDF(output.toFile())) {
            assertEquals(1, doc.getNumberOfPages(), "Debe tener 1 página");
            PDPage page = doc.getPage(0);
            assertNotNull(page);
        }
    }

    @Test
    @DisplayName("Genera un PDF con tamaño A4 horizontal")
    void generatesA4LandscapePdf() throws IOException {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .build();

        Path output = tempDir.resolve("a4-landscape.pdf");
        new PdfTemplateRenderer().render(spec, output);

        try (PDDocument doc = Loader.loadPDF(output.toFile())) {
            PDRectangle box = doc.getPage(0).getMediaBox();
            // A4 horizontal: 297mm x 210mm = 841.89pt x 595.28pt
            assertEquals(841.89f, box.getWidth(), 1.0f);
            assertEquals(595.28f, box.getHeight(), 1.0f);
        }
    }

    @Test
    @DisplayName("Genera un PDF con tamaño A4 vertical")
    void generatesA4PortraitPdf() throws IOException {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .orientation(PageOrientation.PORTRAIT)
            .pageSize(PageSize.A4)
            .build();

        Path output = tempDir.resolve("a4-portrait.pdf");
        new PdfTemplateRenderer().render(spec, output);

        try (PDDocument doc = Loader.loadPDF(output.toFile())) {
            PDRectangle box = doc.getPage(0).getMediaBox();
            assertEquals(595.28f, box.getWidth(), 1.0f);
            assertEquals(841.89f, box.getHeight(), 1.0f);
        }
    }

    @Test
    @DisplayName("Genera un PDF con 288 celdas (SUPER_DENSA)")
    void generatesSuperDensePdf() throws IOException {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.SUPER_DENSA)
            .orientation(PageOrientation.LANDSCAPE)
            .pageSize(PageSize.A4)
            .build();

        Path output = tempDir.resolve("super-densa.pdf");
        new PdfTemplateRenderer().render(spec, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 1000, "El PDF debe pesar más de 1KB");
    }

    @Test
    @DisplayName("Genera PDF sin punto guía correctamente")
    void generatesPdfWithoutGuideDot() throws IOException {
        GridSpec spec = GridSpec.builder()
            .preset(GridPreset.COMPACTA)
            .includeGuideDot(false)
            .build();

        Path output = tempDir.resolve("sin-punto.pdf");
        new PdfTemplateRenderer().render(spec, output);

        assertTrue(Files.exists(output));
        assertTrue(Files.size(output) > 0);
    }

    @Test
    @DisplayName("Rechaza spec nulo")
    void rejectsNullSpec() {
        Path output = tempDir.resolve("invalido.pdf");
        assertThrows(NullPointerException.class,
            () -> new PdfTemplateRenderer().render(null, output));
    }

    @Test
    @DisplayName("Rechaza ruta de salida nula")
    void rejectsNullOutput() {
        GridSpec spec = GridSpec.defaults();
        assertThrows(NullPointerException.class,
            () -> new PdfTemplateRenderer().render(spec, null));
    }

    @Test
    @DisplayName("Crea carpetas intermedias si no existen")
    void createsParentDirectories() throws IOException {
        Path nested = tempDir.resolve("subdir/otro/mas/plantilla.pdf");
        new PdfTemplateRenderer().render(GridSpec.defaults(), nested);

        assertTrue(Files.exists(nested));
    }

    @Test
    @DisplayName("Genera los 6 presets sin errores")
    void generatesAllPresets() throws IOException {
        for (GridPreset preset : GridPreset.values()) {
            GridSpec spec = GridSpec.builder()
                .preset(preset)
                .orientation(PageOrientation.LANDSCAPE)
                .pageSize(PageSize.A4)
                .build();

            Path output = tempDir.resolve("preset-" + preset.name() + ".pdf");
            new PdfTemplateRenderer().render(spec, output);

            assertTrue(Files.exists(output),
                "El preset " + preset + " debe generar un PDF");
            assertTrue(Files.size(output) > 0,
                "El preset " + preset + " no debe generar un PDF vacío");
        }
    }
}

