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

### 2.3. Tubería de Análisis de Video Grabado Fuera de Línea (F11)

```
Video File (Uri)
       │
       ▼
[ActivityResultContracts.PickVisualMedia] (PhotoPicker del sistema, sin permisos)
       │
       ▼
[AndroidMediaMetadataRetrieverExtractor] (Extracción de metadatos técnicos)
       │
       ▼
[VideoValidator] (SC-28: Validación anticipada de límites en pure Kotlin)
       │
       ├─► (Rechazado) ──► VideoAnalysisOutcome.Rejected ──► Diálogo de error específico
       │
       ▼ (Válido)
[VideoFrameScaler] (Reducción previa de escala a max 640px para memoria acotada)
       │
       ▼
[AndroidMediaMetadataRetrieverExtractor] (Muestreo a ~15 fps con getScaledFrameAtTime)
       │
       ▼
[VideoFrameRotator] (Corrección de rotación según METADATA_KEY_VIDEO_ROTATION)
       │  (Reciclado inmediato del Bitmap original)
       ▼
[MediaPipeVideoPoseDetector] (RunningMode.VIDEO síncrono con timestampMs intrínseco)
       │  (Reciclado inmediato del Bitmap procesado)
       ▼
[MediaPipePoseAdapter] (Adaptador puro a PoseFrame)
       │
       ▼
[SessionEngine] (Misma máquina de estados de análisis: Framing -> Calibrating -> Running -> Finished)
       │
       ├──► UI (VideoProcessingDialog modal con barra de progreso y Cancelar SC-29)
       └──► Repository (Persistencia Room con mode = "VIDEO" y resumen inmediato)
```

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

---

## 7. Segmentación Temporal y Máquina de Estados (Fase F8)

- **`JointSpeedCalculator`:**
  - Velocidad articular instantánea y agregada ponderada $\bar{v}_{\text{active}}$ normalizada en torsos/segundo.
- **`MotionSegmenter`:**
  - Máquina de estados para los 18 movimientos (`AwaitingMovement`, `InMovement`, `Holding`, `Evaluated`).
  - Detección de inicio de movimiento ($v \ge V_{\text{START}}$ con lockout de 400 ms) y sostén de postura ($v < V_{\text{STILL}}$ durante $\ge 600\text{ ms}$).
  - Extracción de postura clave estática y picos de elevación en patadas `PEAK_THEN_HOLD` (movimientos 14 y 16).
  - Escenarios de no confusión para posturas estáticas sucesivas (movimientos 5 $\rightarrow$ 6 y 11 $\rightarrow$ 12).
- **`SessionEngine`:**
  - Orquestador de la sesión de práctica en tiempo real con `StateFlow<SessionState>` y `SharedFlow<SessionEvent>`.
  - Soporte a comandos de recuperación SC-13 (`Pause`, `Resume`, `RepeatCurrent`, `SkipCurrent`, `Discard`).
  - Detección de pérdida de tracking prolongada y reanudación automática tras recuperación de cuadros confiables.

---

## 8. Capa de Presentación y Experiencia de Usuario (Fase F9)

- **Diseño Cerrado (Sección 14):**
  - Paleta Material 3 con `ExtendedColors` para retroalimentación semántica (verde, ámbar, rojo, gris).
  - Escala tipográfica oficial basada en tokens Material 3.
  - Regla R-10: 100% de textos centralizados en `strings.xml`.
- **Pantallas Compose:**
  - `HomeScreen`: Tarjeta técnica, resumen, recuperación de borrador, avisos normativos.
  - `PracticeScreen`: Orientación vertical bloqueada (D-03), HUD CameraX con `OverlayScrim`, superposición de esqueleto con 4dp y nodos de 6dp, banner temporal de 2500 ms y controles táctiles de 56 dp.
  - `SummaryScreen`: Anillo de puntuación orientativa `ScoreRing`, tarjetas de movimientos `MovementCard` con chips de categoría.
  - `MovementDetailScreen`: Instantánea de pose clave con esqueleto, métricas biomecánicas en barras visuales y observaciones mapeadas en español de México.
- **ViewModels con Turbine:**
  - `PracticeViewModel`, `HomeViewModel`, `SummaryViewModel`, `MovementDetailViewModel`.

---

## 9. Persistencia Local, Historial y Exportación (Fase F10)

- **Base de Datos Room (`AppDatabase`):**
  - Esquema SQLite versionado con exportación JSON en `app/schemas/`.
  - Tabla `session`: Identificador UUID, fecha de inicio y fin, puntuación global orientativa, conteo de evaluables/parciales/no evaluables, modo y JSON de flags versionado.
  - Tabla `movement_result`: Resultados por movimiento (1..18) con clave foránea en cascada `ON DELETE CASCADE` hacia `session(id)`, clave primaria compuesta `(sessionId, movementIndex)`, outcome, evaluabilidad, puntaje y JSONs estructurados de métricas y feedback.
  - Tabla `session_draft`: Borrador único (`id = 1`) para recuperación de sesiones interrumpidas (SC-18, expiración 24h).
  - Relación 1:N `SessionWithMovements` para consultas atómicas transaccionales.
  - Prohibición de migraciones destructivas (`fallbackToDestructiveMigration` prohibido). Estrategia de respaldo preventivo de base de datos (`backupDatabaseFile()`) ante anomalías.
- **DataStore Preferences (`UserSettingsRepository`):**
  - Almacena de forma reactiva: `defaultCameraFacing` (FRONT/BACK), `voiceEnabled` (Boolean), `exigencyLevel` (RELAXED, NORMAL, STRICT), `strictMode` (Boolean) y `saveSnapshots` (Boolean).
- **Servicio de Borrado Total SC-30 (`DataErasureService`):**
  - Elimina de forma irreversible: base de datos Room, borradores, preferencias DataStore, instantáneas privadas en disco, reportes en caché y reseteo de la sesión activa en memoria, protegido con doble diálogo modal de confirmación destructiva.
- **Exportación Segura (`SessionExporter`, RF-11):**
  - Generación de reportes en texto plano `.txt` y documentos nativos `.pdf` (`PdfDocument`) conteniendo obligatoriamente los disclaimers normativos (`MSG_DISCLAIMER` y `MSG_UNVALIDATED`).
  - Distribución a través de `FileProvider` con `FLAG_GRANT_READ_URI_PERMISSION` mediante el menú nativo del sistema Android (`Intent.ACTION_SEND`).



