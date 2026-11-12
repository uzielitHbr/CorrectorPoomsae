# Changelog

Todas las modificaciones notables a este proyecto serán documentadas en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

## [0.8.0] - 2026-11-12
### Added
- **Fase F8 (Segmentación temporal y secuenciación guiada):**
  - Implementación de `JointSpeedCalculator` para velocidad articular instantánea normalizada por tiempo y altura instantánea del torso en espacio de imagen en torsos/segundo (Sección 12.3).
  - Modelos y contratos normativos de sesión en `SessionModels.kt`: `PauseReason`, `MovementSubState` (`AwaitingMovement`, `InMovement`, `Holding`, `Evaluated`), `SessionState` (`Idle`, `Framing`, `Calibrating`, `Ready`, `Running`, `Paused`, `Finished`, `Aborted`), `SessionCommand`, `SegmenterEvent` y `SessionEvent`.
  - Máquina de estados de segmentación temporal `MotionSegmenter` que monitorea las velocidades articulares activas (`v_active >= V_START` para inicio con lockout de 400 ms, y `v_active < V_STILL` durante `>= T_HOLD_MS = 600 ms` para sostén).
  - Detección de picos de elevación en patadas (`PEAK_THEN_HOLD`, movimientos 14 y 16) extrayendo el `peakFrame` para evaluación técnica de `Ap chagi`.
  - Garantía de no confusión entre movimientos consecutivos que comparten posición sin desplazamiento de pies (movimientos 5 -> 6 y 11 -> 12), evaluando únicamente las articulaciones activas de los brazos.
  - Mecanismos de recuperación SC-13: comandos `Pause`, `Resume`, `RepeatCurrent` (reintento con incremento de intentos), `SkipCurrent` (omisión voluntaria con `MovementOutcome.OMITTED`), `RestartForm` y `Discard`.
  - Detección de pérdida de tracking (`T_LOSS_SHORT_MS = 1000 ms`) con pausa automática y reanudación automática al recuperar cuadros estables (`T_RECOVER_MS = 1500 ms`).
  - Motor de sesión reactivo `SessionEngine` con `StateFlow<SessionState>` y `SharedFlow<SessionEvent>`, cálculo del rumbo medido relativo $\tau_{\text{meas}}(n)$ respecto al movimiento previo para verificar giros esperados y consolidación de la puntuación global de la sesión.
  - Generador determinista de secuencias sintéticas ruidosas `SyntheticPoomsaeSequenceGenerator` para los 18 movimientos y casos de esquina.
  - Suites de pruebas unitarias `JointSpeedCalculatorTest`, `MotionSegmenterTest` y `SessionEngineTest` con 86 tests unitarios totales en verde y cobertura de líneas del 91.02% en el paquete `segmentation` (93.03% global en `:analysis`).

## [0.6.0] - 2026-11-12
### Added
- **Fase F6 (Calidad de señal y calibración del usuario):**
  - Implementación de `FrameQualityGate` para rechazar cuadros antes del procesamiento por cuerpo incompleto, cortado por márgenes o a distancia inadecuada (torso demasiado grande o pequeño en imagen), con mensajes de ayuda estructurados (`MSG_FRAME_TOO_CLOSE`, `MSG_FRAME_TOO_FAR`, `MSG_FRAME_CUT`, `MSG_LOW_CONF`, `MSG_LOST`).
  - Implementación del calibrador de proporciones `BodyProfileCalibrator`, calculando la mediana estadística de las dimensiones de segmentos corporales (`lThigh`, `lShank`, `lLeg`, `torsoLength`, `shoulderWidth`, `armLength`, `dNoseShoulder`, `tImgMedian`) en posición erguida lista (*junbi seogi*) y verificando su estabilidad temporal con MAD / Mediana <= `CAL_MAX_REL_MAD`.
  - Captura y persistencia de la base de orientación espacial de referencia ($u$, $\sigma$, $F_0$ y $r_0$) en la entidad `BodyProfile`.
  - Suite de pruebas unitarias `FrameQualityGateTest` y `BodyProfileCalibratorTest`.
- **Fase F7 (Evaluador estático de movimientos):**
  - Implementación de las 21 métricas biomecánicas definidas en la sección 13.4 en `MetricCalculator` (desglosadas en `StanceMetrics`, `ArmTechniqueMetrics`, `ArmBiometrics`, `KickMetrics` y `TurnMetrics`).
  - Función de puntuación por bandas de 0 a 100 con descenso lineal (`MetricScoringBand`) y registro central normativo `MetricBandRegistry`, con todos los umbrales declarados explícitamente como `PENDIENTE_CALIBRAR` (Regla R-05).
  - Catálogo de 27 códigos normativos `FeedbackCode` en dominio puro `:analysis` y su mapeador `FeedbackMessageMapper` a recursos en español de México en `:app`.
  - Motor de reglas `FeedbackRuleEngine` para generar retroalimentación estructurada ante cualquier desviación fuera de la banda ideal.
  - Motor de evaluación estática `MovementEvaluator` implementando estrictamente el modelo de evaluabilidad de la sección 13.7 con sus cuatro conceptos (*evaluable*, *parcialmente evaluable*, *no evaluable* e *incorrecto*) y ponderación crítica vs secundaria.
  - Constructor sintético 3D `SkeletonBuilder` para inyección determinista de poses y ángulos en pruebas.
  - Suites de pruebas `MovementEvaluatorTest`, `MetricCalculatorTest` y `FeedbackMessageMapperTest` con demostración de monotonía, simetría y cobertura de líneas del 89.79% en el paquete `evaluation`.

## [0.5.0] - 2026-11-12
### Added
- **Fases F4 y F5 (Cámara, permisos y detección de pose con MediaPipe):**
  - Flujo de permisos de cámara completo y desacoplado en `CameraPermissionViewModel` (explicación previa con `MSG_PERM_RATIONALE`, solicitud y denegación permanente con `MSG_PERM_SETTINGS` y enlace directo a ajustes del sistema).
  - Integración de CameraX con casos de uso `Preview` e `ImageAnalysis` (estrategia `STRATEGY_KEEP_ONLY_LATEST`, formato `RGBA_8888`, selección frontal/trasera y ejecutor dedicado de un solo hilo según sección 12.9.1).
  - Modelo ligero MediaPipe Pose Landmarker Lite (`pose_landmarker_lite.task`, $5.5\text{ MB}$, FP16) en assets bajo licencia Apache 2.0 (registrada en `MODEL_LICENSE.txt` y `DECISIONS.md`).
  - Adaptador puro `MediaPipePoseAdapter` que transforma landmarks a `PoseFrame` (`image` y `world`), exigiendo marcas de tiempo estrictamente crecientes y verificando las 17 articulaciones requeridas (`RIGHT_WRIST` en índice 16).
  - Estimador `MediaPipePoseEstimator` en modo `LIVE_STREAM` con protección atómica contra cuadros en vuelo (`inFlight`), descarte concurrente y cálculo de FPS y cuadros descartados para depuración.
  - Lienzo de esqueleto `PoseOverlay` en Jetpack Compose con colores diferenciados para lados izquierdo (cyan) y derecho (ámbar) y soporte de espejado en cámara frontal.
  - Documentos técnicos de experimentos: `docs/COORDINATES.md` (Experimento E1 y E2), `docs/RELIABILITY.md` (Experimento E3) y `docs/PERFORMANCE.md` (benchmarks y presupuesto de latencia).
  - Suite de pruebas unitarias automatizadas (`CameraPermissionViewModelTest` y `MediaPipePoseAdapterTest`), elevando el total a 64 pruebas unitarias en verde.

## [0.4.0] - 2026-11-12
### Added
- **Fase F3 (Definición del poomsae):**
  - Tipos cerrados en `:analysis` (`Side`, `StanceType`, `Stance`, `TechniqueType`, `Technique`, `EvaluationMode`, `ValidationStatus`, `Movement`, `MovementHeading`, `PoomsaeDefinition`) con `kotlinx.serialization` (`1.9.0` compatible con Kotlin `2.2.10`).
  - `PoomsaeLoader` y `PoomsaeValidator` con validación estricta del esquema v2 (`activeJoints`, `evaluationMode`, `expectedTurnDeg`, `sharesStanceWithPrevious`) y reglas de sección 6.5 (18 movimientos consecutivos, giros permitidos, `kihap` exclusivo en movimiento 18, rumbo acumulado y cierre angular en 0° tras retorno).
  - `MetricSheetRegistry` para resolver las articulaciones MediaPipe requeridas por cada ficha de métrica (13.4) según postura, técnica, extra y giro.
  - Suite `PoomsaeLoaderTest` con carga canónica de `poomsae_taegeuk1.json`, 8+ casos inválidos específicos y prueba de cobertura de fichas de métricas.

## [0.3.0] - 2026-11-12
### Added
- **Fase F2 (Módulo `:analysis`: modelos y geometría):**
  - Entidades puras de pose y análisis: `Joint`, `Landmark`, `PoseFrame`, `CoordinateSpace`, `PoseEstimator`, `MetricResult` (12.9.2) y `AnalysisConfig` con tolerancias `EPS_*`.
  - Tipos vectoriales inmutables `Vec3` y `Vec2` con manejo explícito de errores para vectores de longitud cero o entradas no finitas (`GeometryResult`, nunca `NaN` silencioso).
  - Primitivas geométricas y de orientación corporal (13.2): ángulos 2D/3D, ángulo con la vertical, proyección sobre plano, ángulo con signo, `estimateUpVector`, `hipAxisHorizontal`, `estimateForwardSign`, `bodyForwardDirection`, `computeHeadingDeg` y `computeHeadingFromFrame`.
  - Filtros de suavizado y estadística robusta: `OneEuroFilter`, `OneEuroFilter3D`, `ExponentialMovingAverageFilter` y `SignalStats`.
  - Configuración de reporte JaCoCo en `:analysis` y suite de 34 pruebas unitarias con cobertura de líneas > 96% en `geometry` y > 98% en `signal`.

## [0.2.0] - 2026-11-11
### Added
- **Fase F1 (Inspección y completado del proyecto Android Studio existente):**
  - Módulo `:analysis` como biblioteca pura Kotlin/JVM aislada sin dependencias de Android.
  - Catálogo de versiones `gradle/libs.versions.toml` con versiones estables verificadas (Hilt 2.60.1, KSP 2.2.10-2.0.2, Detekt 1.23.8, ktlint 12.1.2, Navigation Compose 2.8.8, Turbine 1.2.1, MockK 1.14.11).
  - Configuración de Dagger Hilt en aplicación (`TaegeukCoachApp`) y actividad principal (`MainActivity`).
  - Navegación declarativa con Jetpack Navigation Compose para 5 pantallas base: Inicio, Práctica, Resumen, Historial y Ajustes.
  - Tema y paleta de diseño Material 3 con tokens oficiales de color (`Color.kt` y `Theme.kt`).
  - Catálogo de mensajes del sistema y recursos de interfaz en español de México (`strings.xml`).
  - Análisis estático con Detekt (`config/detekt/detekt.yml`) y ktlint integrados con tareas aggregate en Gradle.
  - Script local de verificación por fases `scripts/verify.sh` y flujo de integración continua en `.github/workflows/ci.yml`.
  - Pruebas unitarias de aislamiento para `:analysis` y de rutas para la navegación.

## [0.1.0] - 2026-11-11
### Added
- **Fase F0 (Preparación y fijación documental de la secuencia):**
  - Adopción de la solicitud de cambio **CR-001** (*Fijación documental de Taegeuk 1 Jang*).
  - Documentación detallada de fuentes doctrinales y secundarias en `docs/sources/taegeuk1_sources.md`.
  - Definición machine-readable canónica en `poomsae_taegeuk1.json` con 18 movimientos y estado `DOCUMENTALLY_FIXED`.
  - Catálogo de decisiones de arquitectura, producto y dependencias en `docs/DECISIONS.md`.
  - Registro normativo de supuestos técnicos en `docs/ASSUMPTIONS.md`.
  - Catálogo oficial de versiones de dependencias verificadas en `docs/VERSIONS.md`.
  - Documentos de memoria y orquestación del proyecto en `docs/`: `PROJECT_STATUS.md`, `IMPLEMENTATION_LOG.md`, `ARCHITECTURE.md`, `KNOWN_ISSUES.md` y `TEST_RESULTS.md`.
  - `README.md` actualizado con advertencia de secuencia fijada documentalmente y directrices de compilación.

