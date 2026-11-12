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

---

## 5. Calidad de Señal y Calibración Antropométrica (Fase F6)

- **`FrameQualityGate` (`analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/quality/FrameQualityGate.kt`):**
  - Evalúa la presencia y confianza mínima de articulaciones clave (`AnalysisConfig.VIS_MIN`).
  - Verifica la distancia del usuario evaluando la proporción del torso en la imagen:
    - $T_{\text{img}} < \text{TORSO\_MIN\_HEIGHT\_RATIO}$ (0.15) $\rightarrow$ Rechazo con `QualityRejectionCode.FRAME_TOO_FAR` ("Acércate un poco al teléfono").
    - $T_{\text{img}} > \text{TORSO\_MAX\_HEIGHT\_RATIO}$ (0.65) $\rightarrow$ Rechazo con `QualityRejectionCode.FRAME_TOO_CLOSE` ("Aléjate un poco para que se vea tu cuerpo completo").
  - Verifica márgenes respecto a los bordes de la pantalla (`FRAME_MARGIN_RATIO` = 0.05).
- **`BodyProfileCalibrator` (`analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/quality/BodyProfileCalibrator.kt`):**
  - Extrae el `BodyProfile` del usuario a partir de una ráfaga de al menos `CAL_MIN_FRAMES` (15) cuadros estables en *junbi seogi*.
  - Exige estabilidad temporal relativa en todas las longitudes corporales: $\frac{\text{MAD}(S)}{\text{Mediana}(S)} \le \text{CAL\_MAX\_REL\_MAD}$ (0.15).
  - Captura y almacena la base de orientación espacial erguida ($u$, $\sigma$, $F_0$, $r_0$) requerida para cálculo invariante de rumbos y pasos.

---

## 6. Evaluador Estático de Movimientos (Fase F7)

- **`MetricCalculator` (`analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/evaluation/MetricCalculator.kt`):**
  - Calcula las 21 métricas biomecánicas oficiales de la sección 13.4 distribuidas modularmente en:
    - `StanceMetrics`: `ST_01` a `ST_06` (largo de paso, ancho de separación, flexión de rodillas, torso erguido y balanceo de cadera).
    - `ArmTechniqueMetrics` y `ArmBiometrics`: `AR_01` a `AR_08` (ángulo de codo, altura y avance de muñeca, desviación horizontal, inclinación de antebrazo, puño sobre hombro, muñeca sobre cabeza).
    - `KickMetrics`: `KI_01` a `KI_05` (elevación de rodilla, altura de pie, extensión de pierna de patada, rodilla de apoyo extendida, avance de patada).
    - `TurnMetrics`: `DI_01` (error de giro relativo) y `DI_02` (desviación de rumbo absoluto).
- **`MetricBandRegistry` y `MetricScoringBand` (`MetricBands.kt`):**
  - Banda de puntuación continua $[c, a, b, d]$ con puntuación 100 en $[a, b]$, descenso lineal en $(c, a)$ y $(b, d)$, y 0 fuera de límites.
  - Todos los umbrales declarados como `PENDIENTE_CALIBRAR`.
- **`MovementEvaluator` (`MovementEvaluator.kt`):**
  - Implementación estricta de los 4 conceptos de evaluabilidad de la sección 13.7:
    - **Evaluable:** Todas las métricas críticas disponibles y cobertura $\ge 80\%$.
    - **Parcialmente evaluable:** Críticas disponibles y cobertura entre $50\%$ y $80\%$.
    - **No evaluable:** Falta alguna crítica o cobertura $< 50\%$.
    - **Incorrecto:** Movimiento evaluable con puntaje $< 50.0$ o violación total de límites exteriores en métricas críticas.
- **`FeedbackRuleEngine` y `FeedbackMessageMapper`:**
  - 27 códigos normativos `FeedbackCode` generados en dominio puro y mapeados a recursos en español de México (`strings.xml`).


