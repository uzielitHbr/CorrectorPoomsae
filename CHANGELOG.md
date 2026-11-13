# Changelog

Todas las modificaciones notables a este proyecto serán documentadas en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

## [1.0.0] - 2026-11-13
### Added
- **Fase F13 (Cierre, calidad final y empaquetado release):**
  - Configuración del tipo de compilación `release` con minificación de código (`isMinifyEnabled = true`) y reducción de recursos (`isShrinkResources = true`).
  - Reglas exhaustivas de conservación ProGuard/R8 en `app/proguard-rules.pro` para MediaPipe Pose Landmarker, métodos JNI, `kotlinx.serialization`, Room Database, modelos matemáticos de `:analysis`, DataStore y Hilt.
  - Artefacto release generado exitosamente: `app-release.apk` con SHA-256 `b13c3fae2db711f9bd85620ad6f92c06e183787f2c7c15c72e788b6c1f4ae4c4`.
  - Auditoría de accesibilidad completa (A11y):
    - Inclusión de semánticas `contentDescription` en todos los controles interactivos `IconButton` (retroceso, navegación a ajustes, exportación, borrado, pausar, cerrar y descarte de avisos).
    - Soporte verificado para escalado de texto al 200% sin truncamiento mediante `verticalScroll` y contenedor scrollable en `ConfirmDialog`.
  - Auditoría de permisos de Android:
    - Verificación formal de que únicamente se solicita `android.permission.CAMERA`.
    - Ausencia total de permisos de red (`android.permission.INTERNET`), garantizando privacidad y procesamiento 100% desconectado.
  - Transparencia y cumplimiento normativo:
    - Integración de diálogo de Aviso de Privacidad Integral en `SettingsScreen`.
    - Integración de diálogo de Licencias de Terceros de Código Abierto (Apache 2.0).
  - Documentación final de entrega:
    - Manual de usuario estructurado de una página en `docs/USER_MANUAL.md`.
    - Actualización exhaustiva de `README.md` (arquitectura, limitaciones, instalación y compilación).
    - Verificación y completado al 100% de la Lista de Verificación Global del Apéndice D en `docs/CHECKLIST_APPENDIX_D.md`.
  - Verificación de regresión integral: 174/174 pruebas unitarias en verde, 0 incidencias Detekt, 0 violaciones ktlint, suite `bash scripts/verify.sh` en verde.
### Added
- **Fase F12 (Calibración y validación con datos reales):**
  - Implementación del evaluador normativo de estados de validación `ValidationStateEvaluator` según la Sección 16.5:
    - Evaluación de modo experimental obligatorio (`isExperimental = true`) bajo `ValidationState.NO_VALIDADO`.
    - Prohibición estricta de badges o textos afirmativos «Validado» (`canDisplayValidatedBadge = false`) mientras no se cumplan los criterios muestrales del Nivel 2 (`VALIDADO_ALCANCE_LIMITADO`).
  - Formalización del documento `docs/VALIDATION_REPORT.md`:
    - Declaración formal de no disponibilidad de grabaciones reales autorizadas con consentimiento informado (LFPDPPP/GDPR) ni evaluación independiente etiquetada por profesores/árbitros de poomsae.
    - Prohibición estricta de simulación o invención de datos empíricos.
    - Inventario tabular exhaustivo de los 37 parámetros en `AnalysisConfig` con valores de desarrollo y estado 100% `PENDIENTE_CALIBRAR`.
    - Protocolo y hoja de ruta metodológica para futura calibración con practicantes reales (muestreo estratificado $N \ge 30$, curvas ROC, índice de Youden, panel ciego con Kappa de Fleiss $\ge 0.70$).
  - Vinculación con la UI en `SummaryViewModel`:
    - Exposición de `isExperimental` en `SummaryUiState` consumida por `SummaryScreen` para renderizar el banner obligatorio `MSG_EXPERIMENTAL` («Modo experimental: los resultados son orientativos.»).
    - Verificación de avisos normativos `MSG_DISCLAIMER` y `MSG_UNVALIDATED` en vistas y exportador.
  - 7 nuevas pruebas unitarias automatizadas (174 pruebas totales en el proyecto en verde):
    - `ValidationStateEvaluatorTest` (4 pruebas en `:analysis`).
    - `ExperimentalModeVerificationTest` (3 pruebas en `:app`).
  - 0 incidencias Detekt, 0 violaciones ktlintCheck, `bash scripts/verify.sh` en verde.
### Added
- **Fase F11 (Análisis de video grabado):**
  - Selector de video oficial del sistema mediante `ActivityResultContracts.PickVisualMedia` (`VisualMediaType.VideoOnly`), garantizando privacidad sin requerir permisos de almacenamiento en tiempo de ejecución.
  - Decodificación por cuadros muestreados mediante `MediaMetadataRetriever` con reducción de escala nativa (`getScaledFrameAtTime`, max 640 px) para contención estricta de memoria ($O(1)$) y muestreo temporal determinista (~15 fps, 66 ms).
  - Integración de MediaPipe Pose Landmarker en `RunningMode.VIDEO` síncrono con marcas de tiempo monótonas crecientes `timestampMs` extraídas de la pista de video.
  - Corrección de orientación con lectura de metadatos de rotación (`METADATA_KEY_VIDEO_ROTATION`) mediante `Matrix.postRotate` y reciclaje inmediato del mapa de bits original.
  - Conexión de los cuadros decodificados con la tubería de análisis completa (`SessionEngine`, `MotionSegmenter`, `MovementEvaluator`), persistiendo la sesión con `mode = "VIDEO"` y badge distintivo en Historial.
  - Ampliaciones v2.0:
    - Límites normativos en `AnalysisConfig`: duración máxima 180 s (`VIDEO_MAX_DURATION_SECONDS`), tamaño máximo 200 MB (`VIDEO_MAX_FILE_SIZE_BYTES`), resolución máxima 1920x1920 (`VIDEO_MAX_WIDTH`/`HEIGHT`).
    - Rechazo anticipado (SC-28) mediante `VideoValidator` en `:analysis` con emisión de mensajes específicos (`video_err_too_long`, `video_err_too_large`, `video_err_resolution_too_high`, `video_err_unsupported_format`).
    - Diálogo modal de procesamiento `VideoProcessingDialog` con porcentaje, tiempo transcurrido/total, movimientos detectados y botón destructivo «Cancelar» (SC-29) que detiene la corrutina de inmediato, libera decodificador y detector nativos sin persistir datos en base de datos.
  - 29 nuevas pruebas unitarias automatizadas (167 pruebas totales en el proyecto en verde):
    - `VideoValidatorTest` (11 pruebas en `:analysis`).
    - `VideoFrameScalerTest` (6 pruebas en `:app`).
    - `VideoFrameRotatorTest` (5 pruebas Robolectric en `:app`).
    - `OfflineVideoProcessorTest` (3 pruebas Robolectric en `:app`).
    - `VideoAnalysisViewModelTest` (4 pruebas Robolectric con Turbine en `:app`).
  - 0 incidencias Detekt, 0 violaciones ktlintCheck.

## [0.10.0] - 2026-11-12
### Added
- **Fase F10 (Persistencia, historial y exportación):**
  - Implementación de la base de datos local **Room** (`AppDatabase`) cumpliendo los contratos de la sección 12.10:
    - Entidad `SessionEntity` (tabla `session` con índice en `startedAtMs DESC`, campos de puntuación, estado, configuración y JSONs de flags).
    - Entidad `MovementResultEntity` (tabla `movement_result` con clave primaria compuesta `(sessionId, movementIndex)` y clave foránea con eliminación en cascada hacia `session(id)`).
    - Entidad `SessionDraftEntity` (tabla `session_draft` con id fijo 1 para recuperación de sesiones interrumpidas SC-18).
    - Objeto de relación 1:N `SessionWithMovements`.
    - DTOs versionados con `schemaVersion = 1` para serialización JSON segura (`FlagsJsonContainer`, `FeedbackJsonContainer`, `MetricsJsonContainer`).
    - DAO reactivo `SessionDao` con `Flow<List<SessionEntity>>` e inserción atómica `@Transaction`.
    - `SessionDraftDao` para persistencia de borradores.
    - Estrategia de migración `DatabaseMigrations` con `MIGRATION_1_2` y método de respaldo `backupDatabaseFile()` ante fallos de migración (prohibición estricta de `fallbackToDestructiveMigration` según sección 8.1).
    - Exportación de esquema Room KSP en `app/schemas/tkd.apptkd.data.room.AppDatabase/1.json`.
  - Persistencia de preferencias de usuario con **Jetpack DataStore Preferences** (`UserSettingsRepository`):
    - Parámetros: orientación de cámara predeterminada (`defaultCameraFacing`), lectura por voz (`voiceEnabled`), nivel de exigencia (`exigencyLevel`: RELAXED, NORMAL, STRICT), modo estricto (`strictMode`) y guardado de capturas (`saveSnapshots`).
  - Repositorio central de persistencia `SessionRepository`:
    - Guardado de sesiones completadas (`saveCompletedSession`) e incompletas (`saveIncompleteSession`).
    - Consulta de sesión con resultados (`getSessionWithMovements`), borrado individual y total.
  - Servicio de **Borrado Total de Datos** (`DataErasureService`, SC-30):
    - Eliminación completa e irreversible de bases de datos Room, borradores, preferencias en DataStore, capturas privadas en disco, archivos temporales y reseteo de la memoria activa.
  - Servicio de **Exportación de Sesiones** (`SessionExporter`, RF-11):
    - Exportación a texto plano estructurado y documentos PDF nativos (`PdfDocument`) con inclusión obligatoria de disclaimers normativos (`MSG_DISCLAIMER` y `MSG_UNVALIDATED`).
    - Creación de intents seguros mediante `FileProvider` con `FLAG_GRANT_READ_URI_PERMISSION`.
  - Interfaz de usuario en Jetpack Compose (Sección 14.8):
    - **Pantalla de Historial (`HistoryScreen`)**: Tarjeta de resumen con promedio histórico y total de prácticas, listado de sesiones con fecha, ID, badges de estado ("Incompleta", "Estricto"), chip de categoría/puntuación, opciones de exportación individual y borrado con diálogo de confirmación.
    - **Pantalla de Ajustes (`SettingsScreen`)**: Configuración interactiva de cámara, voz, exigencia cíclica, modo estricto, capturas, sección de información de versión y botón de borrado total irreversible con diálogo modal destructivo (SC-30).
    - Componentes `SettingsRow.kt` (`SettingsSwitchRow`, `SettingsSelectionRow`, `SettingsActionRow`).
    - Cableado de exportación y menú compartir en `SummaryScreen`.
  - Suite de 20 nuevas pruebas unitarias automatizadas con Robolectric y Turbine (138 pruebas totales en verde en el proyecto):
    - `RoomDatabaseTest`, `DatabaseMigrationTest`, `UserSettingsRepositoryTest`, `DataErasureServiceTest`, `SessionExporterTest` (con `ShadowPdfDocument`), `HistoryViewModelTest`, `SettingsViewModelTest`.
    - 0 incidencias Detekt y 0 violaciones ktlintCheck.
### Added
- **Fase F9 (Interfaz de la sesión de práctica):**
  - Implementación de la experiencia interactiva completa de práctica conectando `SessionEngine` a Jetpack Compose con Material 3.
  - Pantallas de la sección 14.8:
    - **Inicio (`HomeScreen`)**: Tarjeta oficial de Taegeuk 1 Jang, desglose de 18 movimientos, última puntuación, tarjeta de recuperación de borrador (SC-18), aviso de secuencia fijada documentalmente y disclaimer obligatorio.
    - **Preparación y Calibración**: Paso 1 (encuadre del cuerpo completo dentro de la silueta) y Paso 2 (calibración en quietud erguida *junbi seogi* con barra de progreso y reintento en caso de rechazo).
    - **Práctica (`PracticeScreen`)**: Bloqueo de orientación vertical obligatoria (D-03) con `DisposableEffect`, vista previa en vivo de CameraX con análisis MediaPipe, HUD con capa semitransparente `OverlayScrim`, superposición anatómica `SkeletonOverlay`, indicador de rumbo/giro en grados `TurnIndicator`, línea temporal de 18 segmentos `MovementTimeline`, recordatorio de Kihap en movimiento 18, banner no bloqueante de retroalimentación de 2500 ms y controles táctiles de 56 dp.
    - **Resumen (`SummaryScreen`)**: Anillo de puntuación circular `ScoreRing` con etiqueta "orientativo", conteo de movimientos evaluables, parcialmente evaluables y no evaluables, lista completa de 18 tarjetas clicables `MovementCard` con chips de categoría, aviso de motor experimental y navegación de retorno.
    - **Detalle del Movimiento (`MovementDetailScreen`)**: Instantánea estática del esqueleto en la pose clave, cabecera técnica en español y coreano, observaciones estructuradas mapeadas en español de México mediante `FeedbackMessageMapper`, y desglose de métricas con rangos y barras de estado visuales.
  - Componentes reutilizables de la sección 14.6: `PrimaryButton`, `SecondaryButton`, `ResultChip`, `ScoreRing`, `MovementTimeline`, `MovementCard`, `TurnIndicator`, `SkeletonOverlay`, `GuidanceBanner`, `StatusBanner`, `MetricRow`, `ConfirmDialog`, `LoadingState`, `EmptyState` y `ErrorState`.
  - Estricto cumplimiento de la regla R-10: 100% de los textos e indicaciones de interfaz centralizados en `strings.xml` utilizando el catálogo de las secciones 12.8 y 14.5.
  - Vistas previas Compose (@Preview) para todos los componentes y estados en temas Claro y Oscuro.
  - Inyección de dependencias con Hilt para `PoomsaeDefinition` y `VoicePlayer` en `AppModule`.
  - Orquestación y estado UI desacoplado en `PracticeViewModel`, `HomeViewModel`, `SummaryViewModel` y `MovementDetailViewModel`.
  - Suite de pruebas unitarias exhaustivas con **Turbine** (`HomeViewModelTest`, `PracticeViewModelTest`, `SummaryViewModelTest`, `MovementDetailViewModelTest`, `NavigationTest`), alcanzando 118 pruebas unitarias totales en verde en el proyecto.

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

