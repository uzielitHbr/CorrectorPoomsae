# Taegeuk Coach (태극 코치)

> **Aplicación Android nativa en Kotlin para el análisis biomecánico, guía en tiempo real y retroalimentación del poomsae Taegeuk 1 Jang (Taegeuk Il Jang).**  
> Proyecto de investigación para el tema libre de la tesis de grado de cinta negra en Taekwondo.

---

> [!WARNING]
> ### Secuencia fijada documentalmente; no certificada oficialmente
> Esta aplicación es exclusivamente una herramienta de asistencia y apoyo al entrenamiento y la práctica individual. La secuencia de movimientos ha sido fijada documentalmente mediante el procedimiento **CR-001** (*Contraste documental con fuentes secundarias publicadas*). No constituye una certificación oficial emitida por el **Kukkiwon** ni por **World Taekwondo (WT)**. Toda puntuación y sugerencia de corrección es meramente orientativa y no sustituye la supervisión de un profesor o sinodal acreditado (Regla **R-14**).

---

## 1. Alcance y Objetivos

- **Poomsae evaluado:** Taegeuk 1 Jang (18 movimientos canónicos según la secuencia fijada en F0).
- **Procesamiento en el dispositivo (*on-device*):** Detección de pose local mediante MediaPipe Tasks Vision (sin envío de imágenes ni video a servidores externos; privacidad garantizada, **RNF-02**).
- **Modos de uso:**
  - *Modo guiado en vivo:* Análisis en tiempo real mediante la cámara frontal/trasera.
  - *Modo de análisis sobre video grabado:* Procesamiento detallado cuadro por cuadro de un video importado o grabado previamente.
- **Evaluación biomecánica modular:**
  - Postura (*stances*): *Ap seogi* y *Ap kubi*.
  - Técnicas de defensa y ataque (*techniques*): *Arae makki*, *Momtong jireugi*, *Momtong an makki*, *Eolgul makki* y *Ap chagi*.
  - Rumbo acumulado y dirección de giros.
  - Estabilidad y balance del centro de masa corporal.

---

## 2. Arquitectura del Sistema

El proyecto sigue una separación modular limpia para garantizar que la lógica biomecánica y el análisis se evalúen en la JVM de manera reproducible, determinista y sin depender de emuladores ni dispositivos físicos:

```
Taegeuk Coach (Raíz)
├── app/                  # Módulo Android: UI Compose, CameraX, MediaPipe, Room, Hilt
│   └── src/main/...
└── analysis/             # Módulo JVM puro (sin dependencias Android):
    └── src/main/kotlin/  # Geometría 3D, filtros, segmentación, métricas y evaluación
```

### Flujo de Datos

```
CameraX (ImageAnalysis) / VideoDecoder
    │
    ▼
PoseEstimator (MediaPipe Pose Landmarker: 33 puntos 2D y 3D métricos)
    │
    ▼
SignalPipeline (Control de encuadre, filtrado One Euro / EMA, calibración corporal)
    │
    ▼
MotionSegmenter (Máquina de estados guiada por poomsae_taegeuk1.json)
    │
    ▼
MovementEvaluator (Fórmulas biomecánicas puras 3D -> Puntaje orientativo 0-100 y Feedback)
    │
    ▼
SessionState (StateFlow) ──► UI (Jetpack Compose / Material 3)
                         └──► Persistencia Local (Room Database & DataStore)
```

---

## 3. Fuentes de Verdad y Memoria del Proyecto

- `poomsae_taegeuk1.json`: Fuente única de verdad técnica de Taegeuk 1 Jang (esquema v2, 18 movimientos).
- `docs/sources/taegeuk1_sources.md`: Contraste de fuentes bibliográficas y resolución de discrepancias (CR-001).
- `docs/DECISIONS.md`: Registro de decisiones de producto, arquitectura y solicitudes de cambio.
- `docs/ASSUMPTIONS.md`: Registro formal de supuestos técnicos y operativos.
- `docs/VERSIONS.md`: Catálogo verificado de versiones estables de dependencias.
- `docs/PROJECT_STATUS.md`: Estado actual de ejecución por fases del agente autónomo.
- `docs/IMPLEMENTATION_LOG.md`: Bitácora cronológica de avances de ingeniería.
- `docs/ARCHITECTURE.md`: Contratos, interfaces, estados y persistencia vigentes.

---

## 4. Requisitos y Compilación

- **JDK:** Java 17 o Java 21/25
- **Android SDK:** `compileSdk = 37`, `minSdk = 26`, `targetSdk = 37`
- **Herramienta de construcción:** Gradle Wrapper (`./gradlew`)

### Comandos Principales

```bash
# Compilar todo el proyecto
./gradlew assembleDebug

# Ejecutar pruebas unitarias en JVM
./gradlew testDebugUnitTest

# Análisis estático de código
./gradlew detekt ktlintCheck
```
