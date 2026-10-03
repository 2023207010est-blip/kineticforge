# Changelog

Todos los cambios notables del proyecto se documentan en este archivo.

El formato está basado en [Keep a Changelog](https://keepachangelog.com/es/1.1.0/)
y el proyecto sigue [Semantic Versioning](https://semver.org/lang/es/).

## [Unreleased] — v1.1.0 (versión final)

### Changed
- **Quitafondos sin aura blanca**: nuevo pipeline con balance de blancos del papel y
  *matte* de bordes con descontaminación de color (`Paper`, `AlphaMatte`). El borde
  conserva el color real de la tinta en vez de una mezcla casi blanca, así que se ve
  limpio sobre cualquier fondo (también sobre oscuro).
- `BackgroundRemover` ~2× más rápido (máscaras planas, sin listas por píxel).
- `GifExporter`: elige automáticamente un color de transparencia que no choca con el
  dibujo (antes el magenta fijo se volvía transparente si el dibujo lo contenía) y
  ajusta la calidad del cuantizador según el tamaño de la animación.
- Conversión AWT↔JavaFX, extracción y alineación de frames en bloque (más rápido).

### Fixed
- **Quitafondos con IA (U2-Net)**: la máscara se escalaba con vecino más cercano y se
  usaba como alpha directo; ahora se escala bilineal, se usa solo para decidir qué es
  sujeto, y el borde sale de la tinta (`PriorGuidedRemover`). Recupera trazos finos
  que la red pierde, descarta manchas/restos de plantilla y decide los huecos
  interiores. Si la máscara no sirve, cae al método clásico.
- La IA ahora está disponible en el paso 3 y en la herramienta «Quitar fondo»
  (modelo ligero 5 MB o preciso 176 MB).
- `FrameExtractorTest` desactualizado respecto al margen interno.

### Added
- **Multi-hoja**: varias hojas/imágenes en una sola progresión. Lista reordenable en el
  paso 1 (orden natural: `scan_2` antes que `scan_10`), grilla por hoja en el paso 2
  (con «aplicar a todas» e «imagen suelta = 1 frame»), y el paso 3 une todos los frames,
  omite celdas vacías y alinea en un canvas común.
- Validación al avanzar entre pasos del wizard.
- Vista previa sobre fondo oscuro en «Quitar fondo» para revisar bordes.

## Historial previo de v1.1.0 (en desarrollo)

### Added
- **FFmpeg** para exportación a MP4 y WebM (calidad profesional)
- **Metadatos del PDF** con medidas exactas de la grilla
- **Archivo `.json`** generado junto con cada plantilla
- **`MetadataLoader`** que lee las medidas exactas del escaneo
- **Recorte con medidas exactas** (reemplaza al auto-detector)
- **Mejora del `BackgroundRemover`** para eliminar ruido residual
- **Multi-hoja** (varias hojas → 1 animación larga)
- **Pantalla principal** con menú
- **WebP animado real** (24 bits + alpha)
- **APNG animado real** (24 bits + alpha)

### Changed
- **Refactor general:** limpieza de clases, carpetas y espacios
- **`FrameExtractor`** ahora usa metadatos en vez de detectar líneas
- **Wizard** más limpio y profesional

### Deprecated
- **`GridDetector`** (auto-detección de líneas) — reemplazado por `MetadataLoader`
    - Se mantiene para compatibilidad con escaneos sin `.json`
    - Se elimina en v2.0

### Fixed
- **Ruido residual** alrededor del personaje después del `BackgroundRemover`
- **Celdas mal recortadas** en escaneos con rotación mínima

---

## [1.0.0] — 2026-09-22

### Added
- Wizard de 4 pasos (cargar, detectar, procesar, exportar)
- Generación de plantillas PDF con 6 presets
- Detección automática de grilla por proyección de histogramas
- Extracción de hasta 288 frames por hoja
- Eliminación de fondo por flood fill
- Alineación de frames por bounding box
- Exportación a GIF, PNG spritesheet y WebP
- Modo claro/oscuro con preferencias persistentes
- Sección de plantillas integrada en la UI
- 157 tests unitarios
- Manual de usuario (Markdown)

### Known Issues
- GIF limitado a 256 colores (limitación del formato)
- Sin instalador nativo (se ejecuta desde JAR)
- Sin FFmpeg para MP4/WebM
- Ruido residual en algunas imágenes después del `BackgroundRemover`
