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


