## Limitaciones conocidas (v1.0)

### Calidad del GIF
El formato GIF tiene una limitación intrínseca de **256 colores**. Esto significa que
los degradados, antialiasing y detalles finos pierden fidelidad al exportar.

**El pipeline de procesamiento funciona correctamente.** La aplicación muestra el
resultado en preview con calidad profesional (24 bits + alpha). La limitación es
del formato GIF, no del código.

### ¿Por qué pasa esto?
GIF es un formato de 1987. Su paleta máxima es de 256 colores por frame. Para
dibujos a mano con grises y antialiasing, esto produce "banding" y pérdida de
detalle en los bordes.

### ¿Qué se puede hacer?
- **Para v1.0:** usar GIF si el dibujo es simple (pocos colores)
- **Para v1.1 (en desarrollo):** se agregarán formatos modernos:
    - **WebP animado** (24 bits + alpha 8 bits)
    - **APNG animado** (24 bits + alpha 8 bits)
    - **MP4/WebM** (vía FFmpeg)

Estos formatos preservan la calidad original del escaneo.
