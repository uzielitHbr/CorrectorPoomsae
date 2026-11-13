# Resultados Acumulados de Pruebas y Regresión (docs/TEST_RESULTS.md)

**Proyecto:** Taegeuk Coach  
**Última actualización:** 11 de noviembre de 2026  

---

## Resumen por Fase

| Fase | Fecha | Pruebas Ejecutadas | Estado | Cobertura :analysis | Comentarios / Evidencia |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **F0** | 2026-11-11 | Validación estructural y matemática del JSON (`poomsae_taegeuk1.json`), verificación de Gradle dry-run | **PASS (Verde)** | N/A (Fase documental) | Se verificaron los 18 movimientos, índices 1..18, giros válidos {-180, -90, 0, 90, 180}, técnicas cerradas, kihap único en 18 y cierre angular perfecto (0° tras giro de retorno). |
| **F1** | 2026-11-11 | Compilación completa (`assembleDebug`), pruebas unitarias (`testDebugUnitTest`, `:analysis:test`), análisis estático (`detekt`, `ktlintCheck`), script de CI local (`scripts/verify.sh`) | **PASS (Verde)** | 100% (Marcador base de módulo aislado) | 0 errores de compilación, 0 fallos de tests unitarios, 0 violaciones Detekt, 0 violaciones ktlint. |
| **F2** | 2026-11-12 | Pruebas unitarias `:analysis:test` (34 tests), cobertura JaCoCo, compilación `:app` + `:analysis`, análisis estático (`detekt`, `ktlintCheck`) | **PASS (Verde)** | `geometry`: 96.03% (387/403)<br>`signal`: 98.09% (154/157)<br>`model`: 100.00% (54/54) | 34/34 tests en verde (incluye cálculos manuales de 0°, 90°, 180°, casos degenerados sin NaN, invariancia a traslación/rotación/handedness y filtros One Euro / EMA). |
| **F3** | 2026-11-12 | Pruebas unitarias `:analysis:test` (44 tests totales, 10 nuevos de poomsae), cobertura JaCoCo, compilación `:app` + `:analysis`, análisis estático (`detekt`, `ktlintCheck`) | **PASS (Verde)** | `poomsae`: 87.57% (324/370)<br>`geometry`: 96.03%<br>`signal`: 98.09% | 1 carga canónica v2 con cierre angular (0° tras retorno), 8+ casos inválidos específicos, y cobertura de fichas 13.4 para todos los pares posición/técnica. |
| **F4** | 2026-11-12 | Pruebas unitarias `CameraPermissionViewModelTest` (9 tests), compilación CameraX, `scripts/verify.sh`, Detekt y ktlint | **PASS (Verde)** | N/A (Módulo `:app`) | Flujo completo de permisos (explicación previa MSG_PERM_RATIONALE, solicitud, denegado permanente MSG_PERM_SETTINGS con enlace a ajustes). Pruebas en hardware registradas como Pendiente Externo. |
| **F5** | 2026-11-12 | Pruebas unitarias `MediaPipePoseAdapterTest` (9 tests), modelo Lite en assets con licencia Apache 2.0, proyección de esqueleto Compose `PoseOverlay`, Detekt y ktlint | **PASS (Verde)** | N/A (Módulo `:app`) | Mapeo riguroso de las 17 articulaciones (RIGHT_WRIST verificado), timestamps estrictamente crecientes (Sección 12.9.1), descarte atómico de cuadros y documentación de experimentos E1, E2 y E3 en `docs/`. |
| **F6** | 2026-11-12 | Pruebas unitarias `FrameQualityGateTest` (4 tests), `BodyProfileCalibratorTest` (3 tests), Detekt y ktlintCheck | **PASS (Verde)** | `quality`: 91.86% (203/221) | Validación de compuerta de calidad (rechazo por encuadre incompleto, distancia inadecuada con MSG_FRAME_TOO_CLOSE/FAR, bordes cortados) y calibración antropométrica con ráfaga estable y mediana robusta MAD/Mediana <= CAL_MAX_REL_MAD. |
| **F7** | 2026-11-12 | Pruebas unitarias `MovementEvaluatorTest` (6 tests), `MetricCalculatorTest` (9 tests), `FeedbackMessageMapperTest` (1 test), Detekt y ktlintCheck | **PASS (Verde)** | `evaluation`: 89.79% (475/529)<br>`geometry`: 96.03%<br>`signal`: 98.09%<br>`poomsae`: 89.19%<br>**Global `:analysis`:** 92.25% (1618/1754) | Implementación de las 21 métricas biomecánicas (13.4), bandas de puntuación 0..100 (13.8), 27 FeedbackCodes en español, y modelo de evaluabilidad de 13.7 (evaluable, parcialmente evaluable, no evaluable, incorrecto) verificado con SkeletonBuilder para monotonía, simetría y límites críticos. |
| **F8** | 2026-11-12 | Pruebas unitarias `JointSpeedCalculatorTest` (5 tests), `MotionSegmenterTest` (5 tests), `SessionEngineTest` (7 tests), regresión completa (`scripts/verify.sh`), Detekt y ktlintCheck | **PASS (Verde)** | `segmentation`: 91.02% (403/443)<br>`evaluation`: 89.79%<br>`geometry`: 96.03%<br>`quality`: 91.86%<br>**Global `:analysis`:** 93.03% (2056/2210) | Segmentación temporal con velocidad articular adimensional en torsos/segundo (12.3), detección de inicio y ventana de sostén (600 ms), detección de picos en patadas (14 y 16), no confusión en posiciones consecutivas sin movimiento de pies (5-6 y 11-12), mecanismos de recuperación SC-13 (pausa, repetición, omisión y pérdida/recuperación de tracking) y recorrido de los 18 movimientos sin interbloqueos. |
| **F9** | 2026-11-12 | Pruebas unitarias `HomeViewModelTest`, `PracticeViewModelTest`, `SummaryViewModelTest`, `MovementDetailViewModelTest`, previews Compose, Turbine | **PASS (Verde)** | N/A (Módulo `:app`) | Pantallas Inicio, Práctica vertical bloqueada, Resumen, Detalle. Superposiciones de esqueleto y flechas de giro. Vistas previas @Preview y tokens M3. |
| **F10** | 2026-11-12 | Pruebas unitarias `RoomDatabaseTest`, `DatabaseMigrationTest`, `UserSettingsRepositoryTest`, `DataErasureServiceTest`, `SessionExporterTest`, `HistoryViewModelTest`, `SettingsViewModelTest` (138 tests totales) | **PASS (Verde)** | N/A (Módulo `:app`) | Persistencia Room de sesiones/resultados, DataStore de ajustes, borrador de sesión, borrado total irreversible SC-30, exportación PDF/texto y esquemas Room exportados. |
| **F11** | 2026-11-12 | Pruebas unitarias `VideoValidatorTest` (11 tests), `VideoFrameScalerTest` (6 tests), `VideoFrameRotatorTest` (5 tests), `OfflineVideoProcessorTest` (3 tests), `VideoAnalysisViewModelTest` (4 tests) — 167 tests totales en verde | **PASS (Verde)** | `video`: 100.00%<br>**Global `:analysis`:** 93.4% | Análisis de video fuera de línea con PhotoPicker del sistema (`PickVisualMedia`), decodificación muestreada con `MediaMetadataRetriever` y memoria acotada ($O(1)$), `PoseLandmarker` en `RunningMode.VIDEO` síncrono, corrección de orientación, rechazo temprano SC-28 y diálogo con cancelación inmediata SC-29. |
| **F12** | 2026-11-12 | Pruebas unitarias `ValidationStateEvaluatorTest` (4 tests), `ExperimentalModeVerificationTest` (3 tests) — 174 tests totales en verde | **PASS (Verde)** | `config`: 100.00%<br>**Global `:analysis`:** 93.5% | Implementación del evaluador de estado de validación (Sección 16.5), estado operativo normativo `NO_VALIDADO` (Modo experimental), parámetros en `AnalysisConfig` al 100% `PENDIENTE_CALIBRAR`, emisión del informe `docs/VALIDATION_REPORT.md` y restricción absoluta de la insignia «Validado» en UI y exportación. |

---

## Detalle de Pruebas - Fase F0

### Test T-F0-01: Verificación de Integridad y Esquema de `poomsae_taegeuk1.json`
- **Comando:** `python3 -c "import json; ..."`
- **Resultado:** PASS
- **Evidencia:**
  - `schemaVersion`: 2
  - `validation.status`: "DOCUMENTALLY_FIXED"
  - `validation.validatedBy`: "Contraste documental con fuentes publicadas (CR-001)"
  - Cantidad de movimientos: 18 (índices 1 a 18 consecutivos)
  - Posturas válidas: AS, AK
  - Técnicas válidas: ARAE_MAKKI, MOMTONG_JIREUGI, MOMTONG_AN_MAKKI, EOLGUL_MAKKI, AP_CHAGI
  - Kihap: `false` en 1..17, `true` en 18

### Test T-F0-02: Verificación de Cierre Angular (Rumbo Acumulado)
- **Resultado:** PASS
- **Rumbo acumulado paso a paso:**
  - Mov 1: -90° (Cum: -90°)
  - Mov 2: 0° (Cum: -90°)
  - Mov 3: +180° (Cum: +90°)
  - Mov 4: 0° (Cum: +90°)
  - Mov 5: -90° (Cum: 0°)
  - Mov 6: 0° (Cum: 0°)
  - Mov 7: +90° (Cum: +90°)
  - Mov 8: 0° (Cum: +90°)
  - Mov 9: -180° (Cum: -90°)
  - Mov 10: 0° (Cum: -90°)
  - Mov 11: +90° (Cum: 0°)
  - Mov 12: 0° (Cum: 0°)
  - Mov 13: -90° (Cum: -90°)
  - Mov 14: 0° (Cum: -90°)
  - Mov 15: +180° (Cum: +90°)
  - Mov 16: 0° (Cum: +90°)
  - Mov 17: +90° (Cum: +180°)
  - Mov 18: 0° (Cum: +180°)
  - Giro final de retorno: -180° -> Rumbo final: (180 - 180) % 360 = 0° (Orientación inicial exacta).

### Test T-F0-03: Verificación de Línea Base Gradle
- **Comando:** `./gradlew tasks --dry-run`
- **Resultado:** BUILD SUCCESSFUL in 1s.

---

## Detalle de Pruebas - Fase F1

### Test T-F1-01: Ensamblado Completo de Módulos (`assembleDebug`)
- **Comando:** `./gradlew assembleDebug`
- **Resultado:** BUILD SUCCESSFUL
- **Evidencia:** Tareas `:analysis:compileKotlin`, `:analysis:jar`, `:app:kspDebugKotlin`, `:app:compileDebugKotlin`, `:app:hiltJavaCompileDebug`, `:app:transformDebugClassesWithAsm`, `:app:packageDebug`, `:app:assembleDebug` ejecutadas exitosamente. Se genera APK válido.

### Test T-F1-02: Pruebas Unitarias del Módulo `:analysis` (JVM Puro)
- **Comando:** `./gradlew :analysis:test`
- **Resultado:** BUILD SUCCESSFUL (100% pasando)
- **Pruebas ejecutadas:**
  - `AnalysisModuleMarkerTest`: valida que el módulo `:analysis` esté aislado en JVM sin referencias al runtime de Android, comprobando inicialización y metadatos del módulo.

### Test T-F1-03: Pruebas Unitarias del Módulo `:app` (`testDebugUnitTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest`
- **Resultado:** BUILD SUCCESSFUL (100% pasando)
- **Pruebas ejecutadas:**
  - `ExampleUnitTest`: verificación base de aserciones.
  - `NavigationTest`: valida que las 5 rutas canónicas (`home`, `practice`, `summary`, `history`, `settings`) estén definidas, posean identificadores únicos no vacíos y cubran la navegación completa requerida por la especificación.

### Test T-F1-04: Análisis Estático Detekt
- **Comando:** `./gradlew detekt`
- **Resultado:** BUILD SUCCESSFUL
- **Evidencia:** Tareas `:analysis:detekt`, `:app:detekt` y `:detekt` completadas con 0 'smells' o violaciones de complejidad, estilo, nomenclatura o números mágicos.

### Test T-F1-05: Verificación de Estilo y Formato ktlint
- **Comando:** `./gradlew ktlintCheck`
- **Resultado:** BUILD SUCCESSFUL
- **Evidencia:** Revisión de código fuente Kotlin y scripts Gradle (`.kts`) en `:app` y `:analysis` sin ninguna violación de formato.

### Test T-F1-06: Verificación Integral Local (`scripts/verify.sh`)
- **Comando:** `./scripts/verify.sh`
- **Resultado:** `¡TODAS LAS VERIFICACIONES COMPLETADAS EN VERDE!`
- **Pasos validados:** Ensamblado Debug, Pruebas Unitarias JVM, Análisis Estático Detekt y Verificación ktlint.

---

## Detalle de Evidencia - Fase F2

### Test T-F2-01: Pruebas Unitarias de `:analysis` (Geometría, Primitivas 13.2 y Filtros de Señal)
- **Comando:** `./gradlew :analysis:test`
- **Resultado:** BUILD SUCCESSFUL (34 tests ejecutados, 0 fallos, 0 omitidos)
- **Suites ejecutadas:**
  - `AnalysisModuleMarkerTest`: verificación de aislamiento JVM puro sin dependencias Android.
  - `Vec3AndVec2Test`: operaciones algebraicas 2D/3D, producto punto, producto cruz, norma, normalización explícita ante vector nulo y valores no finitos (`NaN`/`Infinity`).
  - `AngleGeometryTest`: ángulos 3D y 2D de `0°`, `90°`, `180°` calculados a mano, ángulo con la vertical, proyección sobre plano, ángulo con signo, `wrapDegrees` e invariancia ante traslación y rotación.
  - `BodyPrimitivesTest`: primitivas de la sección 13.2 (`estimateUpVector`, `hipAxisHorizontal`, `estimateForwardSign`, `bodyForwardDirection`, `computeHeadingDeg`, `computeHeadingFromFrame`), validación de invariancia ante traslación, inclinación de cámara, escala e inversión de mano (handedness) del eje Z, y retorno de `MetricResult.Unavailable` con motivo explícito ante entradas incompletas.
  - `SignalFilterTest`: `OneEuroFilter`, `OneEuroFilter3D`, `ExponentialMovingAverageFilter` y `SignalStats` (`median`, `medianVec3`, `mad`, `variance`), incluyendo rechazo de timestamps no crecientes o valores `NaN`.

### Test T-F2-02: Reporte de Cobertura JaCoCo (`:analysis:jacocoTestReport`)
- **Comando:** `./gradlew :analysis:test :analysis:jacocoTestReport`
- **Resultado:** Cobertura de líneas >= 90% superada en todos los paquetes requeridos:
  - `mx/taekwondo/poomsae/analysis/geometry`: **96.03%** (`387/403` líneas cubiertas)
  - `mx/taekwondo/poomsae/analysis/signal`: **98.09%** (`154/157` líneas cubiertas)
  - `mx/taekwondo/poomsae/analysis/model`: **100.00%** (`54/54` líneas cubiertas)

### Test T-F2-03: Regresión Completa y Calidad Estática
- **Comando:** `./gradlew assembleDebug test detekt ktlintCheck`
- **Resultado:** BUILD SUCCESSFUL (0 errores de compilación, 0 fallos de pruebas, 0 violaciones Detekt, 0 violaciones ktlint).

---

## Detalle de Evidencia - Fase F3

### Test T-F3-01: Carga y Validación Estricta v2 de `poomsae_taegeuk1.json` (`PoomsaeLoaderTest`)
- **Comando:** `./gradlew :analysis:test`
- **Resultado:** BUILD SUCCESSFUL (44 tests totales en `:analysis`, 10 tests de `PoomsaeLoaderTest`, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Carga exitosa de `poomsae_taegeuk1.json` v2: 18 movimientos consecutivos, `validationStatus == DOCUMENTALLY_FIXED`, `officialCertification == false`, rumbos acumulados por movimiento y cierre angular en `0°` tras giro de retorno.
  - Casos inválidos específicos con error legible: cantidad de movimientos != 18 (`InvalidMovementCount`), giro no permitido (`DisallowedTurnAngle`), técnica desconocida (`InvalidJsonFormat`), falta de cierre angular (`InvalidAngularClosure`), índices duplicados/no consecutivos (`NonConsecutiveMovementIndex`), campos v2 faltantes (`activeJoints`, `evaluationMode`, `expectedTurnDeg`, `sharesStanceWithPrevious` -> `MissingRequiredField`), `schemaVersion` incompatible (`IncompatibleSchemaVersion`), y `kihap` fuera de lugar o ausente en 18 (`InvalidKihapPlacement`).
  - Cobertura de fichas de métricas (13.4): verificación de que cada movimiento y par posición/técnica/extra en los 18 movimientos resuelve sus métricas y articulaciones requeridas en `MetricSheetRegistry`.

### Test T-F3-02: Regresión Completa y Calidad Estática
- **Comando:** `./gradlew assembleDebug test :analysis:jacocoTestReport detekt ktlintCheck`
- **Resultado:** BUILD SUCCESSFUL (0 errores de compilación, 0 fallos de pruebas, 0 violaciones Detekt, 0 violaciones ktlint).

---

## Detalle de Evidencia - Fase F4

### Test T-F4-01: Pruebas Unitarias de Máquina de Estados de Permisos (`CameraPermissionViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.camera.CameraPermissionViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (9 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Estado inicial `NotDetermined`.
  - Transición a `Granted` cuando el sistema reporta permiso existente.
  - Transición a `RationaleRequired` cuando `shouldShowRequestPermissionRationale` es verdadero (`MSG_PERM_RATIONALE`).
  - Transición a `Requesting` cuando el usuario pulsa solicitar permiso.
  - Transición a `PermanentlyDenied` cuando el permiso fue rechazado y no procede rationale (`MSG_PERM_SETTINGS` y redirección a ajustes).
  - Transición a `Denied` al descartar el diálogo.

### Test T-F4-02: Integración de Casos de Uso CameraX
- **Comando:** `./gradlew :app:assembleDebug`
- **Resultado:** BUILD SUCCESSFUL
- **Evidencia:** `Preview` e `ImageAnalysis` configurados con `STRATEGY_KEEP_ONLY_LATEST`, formato `RGBA_8888`, selección frontal/trasera y ejecutor dedicado de un solo hilo según la sección 12.9.1. Pruebas físicas en teléfono real registradas como Pendiente Externo.

---

## Detalle de Evidencia - Fase F5

### Test T-F5-01: Pruebas Unitarias del Adaptador MediaPipe (`MediaPipePoseAdapterTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.pose.MediaPipePoseAdapterTest"`
- **Resultado:** BUILD SUCCESSFUL (9 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Mapeo exacto de las 17 articulaciones canónicas en espacio de imagen (`IMAGE`) y mundo métrico (`WORLD`).
  - Verificación explícita de `RIGHT_WRIST` mapeado al índice MediaPipe 16 según criterios de aceptación de F5.
  - Manejo de lista vacía o nula retornando `Success(null)` (persona fuera de cuadro).
  - Aceptación de marcas de tiempo estrictamente crecientes.
  - Rechazo de marcas de tiempo iguales, decrecientes o negativas con `NON_INCREASING_TIMESTAMP` y `NEGATIVE_TIMESTAMP`.
  - Rechazo de listas truncadas o coordenadas no finitas (`NaN` o `Infinity`) con `CORRUPTED_COORDINATES`.
  - Restablecimiento del estado temporal con `reset()`.

### Test T-F5-02: Verificación de Modelo Lite y Licencia en Assets
- **Archivo:** `app/src/main/assets/pose_landmarker_lite.task` ($5.5\text{ MB}$, FP16)
- **Licencia:** Apache 2.0 (Google LLC) registrada en `app/src/main/assets/MODEL_LICENSE.txt` y en `docs/DECISIONS.md`.

### Test T-F5-03: Regresión Completa del Proyecto
- **Comando:** `./gradlew assembleDebug test detekt ktlintCheck`
- **Resultado:** BUILD SUCCESSFUL (64 tests unitarios totales en verde, 0 errores, 0 violaciones Detekt, 0 violaciones ktlint).

---

## Detalle de Evidencia - Fase F8

### Test T-F8-01: Velocidad Articular Normalizada (`JointSpeedCalculatorTest`)
- **Comando:** `./gradlew :analysis:test --tests "mx.taekwondo.poomsae.analysis.segmentation.JointSpeedCalculatorTest"`
- **Resultado:** BUILD SUCCESSFUL (5 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - `null previous frame` y $\Delta t \le 0$ retornan mapa vacío y velocidad activa 0.0.
  - Esqueleto estático produce velocidades articulares nulas ($< 10^{-6}$).
  - Movimiento activo produce velocidad proporcional en torsos/segundo ($> 0.80$).
  - Invariancia a la distancia de la cámara demostrada por normalización con $T_{\text{img}}$.

### Test T-F8-02: Segmentación Temporal y Picos (`MotionSegmenterTest`)
- **Comando:** `./gradlew :analysis:test --tests "mx.taekwondo.poomsae.analysis.segmentation.MotionSegmenterTest"`
- **Resultado:** BUILD SUCCESSFUL (5 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Transición completa `AwaitingMovement` -> `InMovement` ($v \ge 0.80$ durante $> 100\text{ ms}$) -> `Holding` ($v < 0.25$ durante $> 600\text{ ms}$).
  - Detección precisa de cuadro de pico de elevación del tobillo pateador para movimientos `PEAK_THEN_HOLD` (Ap chagi, mov. 14).
  - No confusión en posiciones estacionarias consecutivas (movimientos 5 -> 6 con misma postura Ap kubi): los pies inmóviles no detienen ni confunden al segmentador, disparando el inicio en base a las articulaciones activas de los brazos.
  - Inactividad prolongada detectada con evento `TimedOut` tras 8000 ms.
  - Pérdida de tracking ($> 1000\text{ ms}$) y recuperación automática ($> 1500\text{ ms}$) sin interbloqueos.

### Test T-F8-03: Secuenciación Guiada y Recuperación SC-13 (`SessionEngineTest`)
- **Comando:** `./gradlew :analysis:test --tests "mx.taekwondo.poomsae.analysis.segmentation.SessionEngineTest"`
- **Resultado:** BUILD SUCCESSFUL (7 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Recorrido completo de los 18 movimientos de Taegeuk 1 de principio a fin sin interbloqueos, produciendo 18 evaluaciones y puntuación global calculada.
  - Comandos de pausa (`Pause`) y reanudación (`Resume`) preservando el estado previo de ejecución.
  - Repetición del movimiento actual (`RepeatCurrent`) incrementando el contador de intentos.
  - Omisión voluntaria (`SkipCurrent`) registrando `MovementOutcome.OMITTED` y avanzando al siguiente movimiento.
  - Omisión del último movimiento (18) finalizando la sesión limpiamente.
  - Pausa automática por pérdida de tracking prolongada (`PauseReason.LOST_TRACKING`) y reanudación automática al recuperar cuadros válidos.
  - Comandos de ciclo de vida (`RestartForm`, `Finish`, `Discard` -> `Aborted`).
  - Transición completa desde estado inicial sin calibrar: `Framing` -> `Calibrating` -> `Ready`.

### Test T-F8-04: Verificación Integral del Proyecto y CI Local
- **Comando:** `bash scripts/verify.sh`
- **Resultado:** BUILD SUCCESSFUL (86/86 tests unitarios en verde, 0 incidencias Detekt, 0 violaciones ktlintCheck).
- **Cobertura JaCoCo en `:analysis`:**
  - Paquete `mx.taekwondo.poomsae.analysis.segmentation`: **91.02%** de líneas (403/443).
  - Paquete `mx.taekwondo.poomsae.analysis.evaluation`: **89.79%** de líneas (475/529).
  - Paquete `mx.taekwondo.poomsae.analysis.geometry`: **96.03%** de líneas (387/403).
  - Paquete `mx.taekwondo.poomsae.analysis.quality`: **91.86%** de líneas (203/221).
  - Total global `:analysis`: **93.03%** de líneas (2,056/2,210), superando el requisito de $\ge 85\%$.

## FASE F9: Interfaz de la sesión de práctica (2026-11-12)

### Test T-F9-01: Estado Inicial y Descarte de Borrador (`HomeViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.HomeViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (2 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Emisión de estado inicial sin borrador y con última puntuación reflejada reactivamente vía Turbine.
  - Detección reactiva de borrador previo (SC-18) y acción de descarte limpiando el estado.

### Test T-F9-02: Flujo Reactivo de Sesión de Práctica (`PracticeViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.PracticeViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (6 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados con Turbine:**
  - Estado inicial `Framing` con guía de silueta y cámara frontal predeterminada.
  - Alternancia de facing de cámara (frontal / trasera).
  - Alternancia de lectura por voz (`voiceEnabled`).
  - Flujo de pausa y reanudación con diálogos de confirmación modales.
  - Guardado de borrador con salida a Inicio (SC-18).
  - Descarte voluntario de sesión confirmando estado `Aborted`.
  - Comandos de omisión (`SkipCurrent`) y repetición (`RepeatCurrent`) durante la práctica activa.

### Test T-F9-03: Resumen y Desglose de 18 Movimientos (`SummaryViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.SummaryViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (2 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados con Turbine:**
  - Estado vacío cuando no hay resumen registrado.
  - Mapeo fiel de resumen con 18 tarjetas (`MovementCardData`), categorías asignadas (`CORRECT`, `IMPROVABLE`, etc.), puntuación global orientativa y conteo de evaluables/parciales.

### Test T-F9-04: Detalle Técnico del Movimiento (`MovementDetailViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.MovementDetailViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (1 test ejecutado, 0 fallos, 0 omitidos)
- **Casos validados:**
  - Mapeo de pose clave, instantánea (`snapshotFrame`), cabeceras en español y coreano, retroalimentación estructurada mapeada con `FeedbackMessageMapper` y filas de métricas con estado dentro/fuera de banda.

### Test T-F9-05: Verificación Integral del Proyecto y CI Local
- **Comando:** `bash scripts/verify.sh`
- **Resultado:** BUILD SUCCESSFUL (118/118 tests unitarios totales en verde, 0 incidencias Detekt, 0 violaciones ktlintCheck, compilación assembleDebug OK).
- **Desglose de pruebas unitarias:**
  - `:analysis`: 86 pruebas unitarias (100% pasando).
  - `:app`: 32 pruebas unitarias (100% pasando).
  - Total: 118 pruebas unitarias.

## 11. Fase F10: Persistencia, Historial y Exportación (2026-11-12)

### Test T-F10-01: Persistencia Room y Cascada (`RoomDatabaseTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.data.RoomDatabaseTest"`
- **Resultado:** BUILD SUCCESSFUL (4 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados con Robolectric:**
  - `testInsertAndQuerySessionWithMovements`: Inserción atómica transaccional de `SessionEntity` y lista de `MovementResultEntity` recuperada con `@Relation` en `SessionWithMovements`.
  - `testCascadeDeleteMovementsWhenSessionDeleted`: Eliminación de una sesión borra en cascada todas sus filas secundarias en `movement_result`.
  - `testDraftLifecycle`: Guardado, actualización, recuperación y limpieza del borrador único `SessionDraftEntity` (id = 1).
  - `testDeleteAllWipesData`: Limpieza integral de sesiones y borradores.

### Test T-F10-02: Migración y Respaldo de Base de Datos (`DatabaseMigrationTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.data.DatabaseMigrationTest"`
- **Resultado:** BUILD SUCCESSFUL (3 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - `testMigration1To2SqlExecution`: Validación sintáctica y ejecución de migración `MIGRATION_1_2`.
  - `testBackupDatabaseFileWhenExists`: Creación de archivo `.bak` preservando datos antes de cualquier migración de riesgo (Sección 8.1).
  - `testBackupDatabaseFileWhenNotExists`: Manejo seguro cuando la base de datos es nueva.

### Test T-F10-03: Repositorio de Preferencias DataStore (`UserSettingsRepositoryTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.data.UserSettingsRepositoryTest"`
- **Resultado:** BUILD SUCCESSFUL (4 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - `testDefaultSettings`: Valores por defecto oficiales (`CameraFacing.FRONT`, `voiceEnabled = true`, `exigencyLevel = NORMAL`, `strictMode = false`, `saveSnapshots = false`).
  - `testUpdateCameraFacingAndVoice`: Conmutación reactiva de facing y voz.
  - `testUpdateExigencyAndStrict`: Conmutación de nivel de exigencia y modo estricto.
  - `testClearAllResetsToDefaults`: Restablecimiento integral a valores iniciales.

### Test T-F10-04: Servicio de Borrado Total SC-30 (`DataErasureServiceTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.data.DataErasureServiceTest"`
- **Resultado:** BUILD SUCCESSFUL (1 test ejecutado, 0 fallos, 0 omitidos)
- **Casos validados:**
  - `testDeleteAllUserDataLeavesEmptyState`: Borrado integral e irreversible de tablas Room, borrador en disco, preferencias DataStore, archivos privados de capturas y exportaciones en caché, y reseteo de la sesión activa en memoria.

### Test T-F10-05: Exportador de Sesiones a Texto y PDF (`SessionExporterTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.export.SessionExporterTest"`
- **Resultado:** BUILD SUCCESSFUL (3 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados:**
  - `testExportAsTextContainsRequiredHeadersAndDisclaimers`: Generación de archivo `.txt` con encabezados oficiales, ID, fecha, desglose de 18 movimientos y los disclaimers normativos obligatorios `MSG_DISCLAIMER` y `MSG_UNVALIDATED`.
  - `testExportAsPdfCreatesValidFile`: Generación de documento PDF estructurado mediante `PdfDocument` con `ShadowPdfDocument`.
  - `testCreateShareIntent`: Construcción de `Intent.ACTION_SEND` con URI segura mediante `FileProvider` y flags `FLAG_GRANT_READ_URI_PERMISSION`.

### Test T-F10-06: ViewModel de Historial (`HistoryViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.HistoryViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (3 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados con Turbine:**
  - `testHistoryStateEmitsSessionsAndCalculatesAverage`: Flujo reactivo emitiendo lista de sesiones históricas y cálculo exacto del promedio de puntuaciones completadas.
  - `testDeleteSessionRemovesFromState`: Eliminación de una sesión reflejada inmediatamente en el estado UI.
  - `testOpenSessionSummaryLoadsHolder`: Reconstrucción de la sesión en `SessionSummaryData` y carga en `PracticeSessionHolder` para navegación a pantalla de resumen.

### Test T-F10-07: ViewModel de Ajustes (`SettingsViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.SettingsViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (2 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos validados con Turbine:**
  - `testSettingsEmissionsAndUpdateFlow`: Flujo continuo de emisión y actualización de todos los ajustes (`defaultCameraFacing`, `voiceEnabled`, `exigencyLevel`, `strictMode`, `saveSnapshots`).
  - `testDeleteAllDataInvokesCallback`: Invocación del servicio SC-30 y callback de navegación hacia Inicio.

### Test T-F10-08: Verificación Integral del Proyecto y CI Local
- **Comando:** `bash scripts/verify.sh`
- **Resultado:** BUILD SUCCESSFUL (138/138 tests unitarios totales en verde, 0 incidencias Detekt, 0 violaciones ktlintCheck, compilación assembleDebug OK).
- **Desglose de pruebas unitarias:**
  - `:analysis`: 86 pruebas unitarias (100% pasando).
  - `:app`: 52 pruebas unitarias (100% pasando).
  - Total: 138 pruebas unitarias.

---

## Detalle de Pruebas - Fase F11 (Análisis de Video Grabado)

### Test T-F11-01: Validación de Límites y Rechazo Temprano (`VideoValidatorTest`)
- **Comando:** `./gradlew :analysis:test --tests "mx.taekwondo.poomsae.analysis.video.VideoValidatorTest"`
- **Resultado:** BUILD SUCCESSFUL (11 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados:**
  - `valid video metadata returns Valid`: Video dentro de parámetros estándar (60 s, 1280x720, 50 MB) es aceptado.
  - `boundary test - exactly max duration is accepted`: 180,000 ms es aceptado.
  - `video exceeding max duration is rejected with DURATION_TOO_LONG`: 180,001 ms es rechazado (SC-28).
  - `boundary test - exactly max file size is accepted`: 200 MB exactos es aceptado.
  - `video exceeding max file size is rejected with FILE_TOO_LARGE`: 200 MB + 1 byte es rechazado (SC-28).
  - `boundary test - exactly max resolution is accepted`: 1920x1920 es aceptado.
  - `video exceeding max width/height is rejected with RESOLUTION_TOO_HIGH`: 1921x1080 o 1080x1921 es rechazado (SC-28).
  - `video with zero or negative duration is rejected with UNSUPPORTED_FORMAT`: 0 ms o -100 ms es rechazado.
  - `video with invalid dimensions is rejected with UNSUPPORTED_FORMAT`: ancho o alto $\le 0$ es rechazado.
  - `video without video track is rejected with NO_VIDEO_TRACK`: Archivo sin pista de video es rechazado.

### Test T-F11-02: Escalador de Dimensiones con Reducción Previa (`VideoFrameScalerTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.video.VideoFrameScalerTest"`
- **Resultado:** BUILD SUCCESSFUL (6 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados:**
  - 1080p horizontal (1920x1080) escalado a max 640 px $\to$ 640x360.
  - 1080p vertical (1080x1920) escalado a max 640 px $\to$ 360x640.
  - Resolución baja (320x240) se mantiene sin reescalado innecesario.
  - Cuadrado (1000x1000) mantiene aspecto (500x500).
  - Dimensiones degeneradas $\le 0$ devuelven targetMaxDim.
  - Parámetro por defecto coincide con `AnalysisConfig.VIDEO_DOWNSCALE_TARGET_MAX_DIM`.

### Test T-F11-03: Corrección de Orientación y Reciclado de Bitmaps (`VideoFrameRotatorTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.video.VideoFrameRotatorTest"`
- **Resultado:** BUILD SUCCESSFUL (5 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados en Robolectric:**
  - 0° y 360° devuelven el mismo objeto `Bitmap` sin reciclarlo.
  - 90° rota dimensiones (100x200 $\to$ 200x100) y recicla inmediatamente el original (`original.isRecycled == true`).
  - 180° invierte dimensiones y recicla el original.
  - 270° rota dimensiones y recicla el original.

### Test T-F11-04: Orquestador Fuera de Línea (`OfflineVideoProcessorTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.video.OfflineVideoProcessorTest"`
- **Resultado:** BUILD SUCCESSFUL (3 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados en Robolectric:**
  - `rejected video stops immediately without extracting frames and closes resources (SC-28)`: Verifica que un video inválido no decodifique cuadros y libere recursos inmediatamente.
  - `successful processing samples frames with intrinsic timestamps and recycles bitmaps`: Verifica muestreo secuencial con marcas de tiempo monótonamente crecientes, reciclado de todos los bitmaps creados (memoria acotada $O(1)$) y cierre de recursos.
  - `cancellation aborts processing immediately and releases resources (SC-29)`: Cancela corrutina en vuelo y verifica aborto inmediato y cierre garantizado en bloque `finally`.

### Test T-F11-05: ViewModel de Análisis de Video (`VideoAnalysisViewModelTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.video.VideoAnalysisViewModelTest"`
- **Resultado:** BUILD SUCCESSFUL (4 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados con Turbine:**
  - `initial state is idle`: Estado inicial con flags en falso y mensajes en null.
  - `valid video completes and persists session as VIDEO`: Ejecuta análisis, actualiza `PracticeSessionHolder` con el resumen y persiste en Room con `mode = "VIDEO"` y `cameraFacing = "VIDEO"`.
  - `video exceeding max duration is rejected with error message (SC-28)`: Rechaza anticipadamente y emite el string de error mapeado `video_err_too_long`. Descarte de error verificado.
  - `cancelling analysis stops processing and sets cancel message (SC-29)`: Cancela la tarea, no persiste nada y emite `MSG_VIDEO_CANCEL`. Descarte de mensaje verificado.

### Test T-F11-06: Verificación Integral de Regresión (167 tests totales)
- **Comando:** `bash scripts/verify.sh`
- **Resultado:** BUILD SUCCESSFUL (167/167 tests unitarios en verde, 0 incidencias Detekt, 0 violaciones ktlint, assembleDebug OK).
- **Desglose de pruebas unitarias:**
  - `:analysis`: 97 pruebas unitarias (100% pasando).
  - `:app`: 70 pruebas unitarias (100% pasando).
  - Total: 167 pruebas unitarias.

---

## Detalle de Pruebas - Fase F12

### Test T-F12-01: Evaluador de Estados de Validación (`ValidationStateEvaluatorTest`)
- **Comando:** `./gradlew :analysis:test --tests "mx.taekwondo.poomsae.analysis.config.ValidationStateEvaluatorTest"`
- **Resultado:** BUILD SUCCESSFUL (4 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados:**
  - `testDefaultSystemStateIsNoValidadoAndPendienteCalibrar`: Comprueba que `AnalysisConfig.VALIDATION_STATE` es `ValidationState.NO_VALIDADO` y `CALIBRATION_STATUS` es `ParameterCalibrationStatus.PENDIENTE_CALIBRAR`.
  - `testIsExperimentalReturnsTrueOnlyForNoValidado`: Verifica que `isExperimental()` devuelva `true` únicamente en `NO_VALIDADO` (por defecto y de forma explícita) y `false` en `VALIDACION_PRELIMINAR` y `VALIDADO_ALCANCE_LIMITADO`.
  - `testCanDisplayValidatedBadgeReturnsTrueOnlyForValidadoAlcanceLimitado`: Verifica la regla estricta de la Sección 16.5; devuelve `false` en `NO_VALIDADO` y `VALIDACION_PRELIMINAR`, y únicamente `true` al alcanzar `VALIDADO_ALCANCE_LIMITADO`.
  - `testGetValidationStateDescriptionCoversAllEnumEntries`: Cobertura exhaustiva de descripciones textuales para auditoría en todos los valores del enum.

### Test T-F12-02: Garantías de Interfaz y Modo Experimental (`ExperimentalModeVerificationTest`)
- **Comando:** `./gradlew :app:testDebugUnitTest --tests "tkd.apptkd.ui.ExperimentalModeVerificationTest"`
- **Resultado:** BUILD SUCCESSFUL (3 tests ejecutados, 0 fallos, 0 omitidos)
- **Casos probados:**
  - `system configuration enforces NO_VALIDADO and PENDIENTE_CALIBRAR`: Verificación a nivel de integración entre el módulo Android y la configuración de análisis.
  - `ValidationStateEvaluator forbids validated badge and confirms experimental mode`: Comprueba que la UI no tiene autorización para exhibir insignias afirmativas de validación.
  - `SummaryViewModel defaults and emits isExperimental true`: Verifica reactivamente con un `PracticeSessionHolder` que `SummaryUiState.isExperimental` se mantenga en `true` tras emitir resultados de sesión.

### Test T-F12-03: Verificación Integral de Regresión de Fase F12 (174 tests totales)
- **Comando:** `bash scripts/verify.sh`
- **Resultado:** BUILD SUCCESSFUL (174/174 tests unitarios en verde, 0 incidencias Detekt, 0 violaciones ktlint, assembleDebug OK).
- **Desglose de pruebas unitarias:**
  - `:analysis`: 101 pruebas unitarias (100% pasando).
  - `:app`: 73 pruebas unitarias (100% pasando).
  - Total: 174 pruebas unitarias.





