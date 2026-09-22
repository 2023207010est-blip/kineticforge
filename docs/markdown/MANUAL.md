# Manual de Usuario — KineticForge v1.0

**Convierte hojas A4 con dibujos a mano en GIF animados con fondo transparente.**

---

## Índice

1. Introducción
2. Requisitos
3. Cómo ejecutar
4. Flujo de uso
5. Parámetros del wizard
6. Preguntas frecuentes
7. Limitaciones conocidas

---

## 1. Introducción

**KineticForge** es una aplicación de escritorio que automatiza el pipeline completo de conversión **hoja escaneada → GIF animado**.

Está diseñada para artistas que dibujan animaciones cuadro por cuadro en papel y necesitan digitalizarlas sin herramientas costosas.

---

## 2. Requisitos

- **Java 21+** (obligatorio)
- **Sistema operativo:** Windows 10+, Linux, macOS 11+
- **RAM:** 4 GB mínimo
- **Escáner** o cámara de celular para digitalizar

---

## 3. Cómo ejecutar

### Opción A: Desde el JAR (recomendado para usuarios)

1. Compilar el JAR:
   mvn clean package

2. Ejecutar:
   java -jar target/kineticforge-1.0.0.jar

### Opción B: Desde IntelliJ IDEA (para desarrolladores)

1. Abrir el proyecto en IntelliJ
2. Click derecho en Launcher.java → Run 'Launcher.main()'

### Opción C: Desde Maven

mvn javafx:run

---

## 4. Flujo de uso

KineticForge usa un wizard de 4 pasos:

### Paso 1: Cargar imagen
- Arrastrar el escaneo al área de carga
- O click en "Elegir archivo..."

### Paso 2: Detectar grilla
- Elegir preset (Compacta, Densa, etc.)
- Click en "Detectar automáticamente"
- La app muestra las celdas detectadas

### Paso 3: Procesar frames
- Click en "Procesar frames"
- La app extrae, quita el fondo y alinea automáticamente

### Paso 4: Exportar
- Elegir formato (GIF, PNG, WebP)
- Ajustar FPS y loop
- Click en "Exportar..."

**Tiempo total:** menos de 2 minutos.

---

## 5. Parámetros del wizard

### Presets de grilla disponibles

| Preset | Grid | Total | Uso |
|--------|------|-------|-----|
| Detalle | 4×3 | 12 | Bocetos grandes |
| Estándar | 6×4 | 24 | 1 segundo a 24 fps |
| Compacta | 8×6 | 48 | 2 segundos a 24 fps |
| Densa | 12×8 | 96 | 4 segundos a 24 fps |
| Cinemática | 16×6 | 96 | Widescreen |
| Súper densa | 24×12 | 288 | 12 segundos a 24 fps |

### Formatos de exportación

| Formato | Transparencia | Uso |
|---------|---------------|-----|
| GIF | Sí (1 bit) | Redes sociales |
| PNG spritesheet | Sí (8 bits) | Motores de juego |
| WebP | Sí (8 bits) | Web moderna |

---

## 6. Preguntas frecuentes

### ¿Cómo genero la plantilla para dibujar?

En v1.0, la plantilla se genera desde código con DemoPdf.java. En v1.1 habrá una sección integrada en la UI.

### ¿Por qué el GIF se ve con banding?

Es limitación del formato GIF (256 colores). Para mejor calidad, usar WebP o PNG spritesheet.

### ¿Puedo usar foto de celular en vez de escáner?

Sí. Asegurate de buena iluminación, hoja plana, y resolución alta (más de 2000 px de ancho).

### ¿Puedo animar más de 48 frames?

Sí, elegí un preset con más celdas (Densa, Súper densa). Combinar varias hojas es feature de v1.1.

---

## 7. Limitaciones conocidas (v1.0)

### Calidad del GIF

El GIF tiene limitación de 256 colores. Los degradados pierden fidelidad.

El pipeline funciona correctamente. La app muestra el resultado en preview con calidad profesional. La limitación es del formato GIF, no del código.

### Sin instalador nativo

v1.0 no incluye instalador. Se ejecuta desde JAR o desde IntelliJ. El instalador con jpackage está planeado para v1.1.

### Generación de plantillas

La generación de plantillas requiere DemoPdf.java o terminal. En v1.1 estará integrada en la UI.

### Detección de bordes

El detector de grilla puede fallar en escaneos de baja calidad o con rotación.

---

## Más información

- Repo: https://github.com/2023207010est-blip/kineticforge
- Issues: https://github.com/2023207010est-blip/kineticforge/issues

---

**Fin del manual.**
