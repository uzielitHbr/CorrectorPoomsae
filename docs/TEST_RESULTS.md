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


