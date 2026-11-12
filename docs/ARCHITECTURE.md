# Arquitectura del Sistema (docs/ARCHITECTURE.md)

**Proyecto:** Taegeuk Coach  
**Versión de especificación:** 3.0  
**Fecha de actualización:** 12 de noviembre de 2026 (Fases F4 y F5)  

---

## 1. Módulos del Proyecto

El proyecto está diseñado bajo una arquitectura modular limpia de dos componentes principales:

```
AppTkd (Raíz)
├── app/                  # Módulo Android (:app)
│   ├── di/               # Módulos de inyección de dependencias Hilt
│   ├── camera/           # Integración con CameraX (Preview + ImageAnalysis)
│   ├── pose/             # Adaptador de MediaPipe Tasks Vision a modelos de dominio
│   ├── ui/               # Vistas Jetpack Compose, navegación y temas Material 3
│   ├── data/             # Persistencia local (Room database, DAO y DataStore preferences)
│   ├── tts/              # Motor opcional de Text-to-Speech
│   └── export/           # Exportación a formato texto y PDF (FileProvider)
│
└── analysis/             # Módulo JVM puro (:analysis) - Sin dependencias de Android
    ├── model/            # Modelos inmutables (PoseFrame, Joint, Landmark, BodyProfile, Movement)
    ├── geometry/         # Tipos vectoriales 3D (Vec3), cálculo de ángulos y proyecciones
    ├── signal/           # Filtros (One Euro / EMA), compuerta de calidad (FrameQualityGate)
    ├── poomsae/          # Cargador y validador de secuencia poomsae_taegeuk1.json
    ├── segmentation/     # Máquina de estados temporal (MotionSegmenter)
    └── evaluation/       # Fórmulas de métricas 3D, evaluador de técnicas y feedback codes
```

---

## 2. Flujo de Datos y Sincronización (Sección 12.9.1)

```
Camera Executor (1 hilo dedicado)
       │
       ▼
PoseEstimator (MediaPipe Tasks Vision - LIVE_STREAM mode)
       │
       ▼
Channel<PoseFrame?>(CONFLATED)   [Descarte de cuadros antiguos si hay procesamiento en curso]
       │
       ▼
SessionEngine (Dispatchers.Default en :analysis)
       │
       ▼
StateFlow<SessionState>
       │
       ├──► UI (Main Thread / Jetpack Compose)
       └──► Repository (Room Database & DataStore)
```

- **Línea de tiempo única:** Basada estrictamente en `PoseFrame.timestampMs` (determinado por el sensor de cámara en vivo o por la pista de video en modo offline). El `SessionEngine` es determinista y no depende del reloj del sistema.
- **Sin bloqueo del hilo principal:** Inferencia y análisis geométrico se ejecutan fuera del hilo principal de UI.

---

## 3. Contratos de Dominio Centrales (Sección 5.4 y 12.9.2)

```kotlin
enum class Joint(val mediaPipeIndex: Int) {
    NOSE(0),
    LEFT_SHOULDER(11), RIGHT_SHOULDER(12),
    LEFT_ELBOW(13), RIGHT_ELBOW(14),
    LEFT_WRIST(15), RIGHT_WRIST(16),
    LEFT_HIP(23), RIGHT_HIP(24),
    LEFT_KNEE(25), RIGHT_KNEE(26),
    LEFT_ANKLE(27), RIGHT_ANKLE(28),
    LEFT_HEEL(29), RIGHT_HEEL(30),
    LEFT_FOOT_INDEX(31), RIGHT_FOOT_INDEX(32)
}

data class Landmark(val x: Float, val y: Float, val z: Float, val visibility: Float)

data class PoseFrame(
    val timestampMs: Long,
    val image: Map<Joint, Landmark>,
    val world: Map<Joint, Landmark>
)

interface PoseEstimator {
    val frames: Flow<PoseFrame?>
    fun start()
    fun stop()
}

sealed interface MetricResult {
    data class Value(val value: Double, val confidence: Float) : MetricResult
    data class Unavailable(val reason: UnavailableReason) : MetricResult
}
```

---

## 4. Persistencia y Ajustes (Sección 12.10)

- **Room Database:**
  - Tabla `session`: Metadatos de la sesión de práctica (ID UUID, fecha, duración, modo, puntaje global, estado del poomsae, flags).
  - Tabla `movement_result`: Registro por movimiento (1..18, outcome, evaluabilidad, categoría, score, intentos, métricas y feedback JSON).
  - Tabla `session_draft`: Borrador de una única fila para recuperación tras interrupciones involuntarias.
- **DataStore Preferences:**
  - `camera_facing`: FRONT / BACK (por defecto FRONT).
  - `voice_guidance_enabled`: Boolean (por defecto false).
  - `strictness_level`: RELAXED / NORMAL / STRICT (por defecto NORMAL).
  - `save_keypoint_snapshots`: Boolean (por defecto false).

