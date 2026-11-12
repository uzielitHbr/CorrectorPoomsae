# Bitácora de Implementación (docs/IMPLEMENTATION_LOG.md)

Historial cronológico de cambios de ingeniería del proyecto Taegeuk Coach, en cumplimiento de la sección 15.1 y 15.5 de la especificación técnica.

---

## [2026-11-11] - Fase F0: Preparación y fijación documental de la secuencia

- **Objetivo de la fase:** Establecer la base documental formal, fijar la secuencia de movimientos de Taegeuk 1 Jang bajo el procedimiento CR-001 y crear los artefactos iniciales de configuración y memoria del proyecto.
- **Acciones realizadas:**
  1. Inspección inicial del proyecto Android Studio preexistente (`AppTkd`), validando estructura básica de Gradle, `app/` y archivos de configuración.
  2. Adopción y documentación formal de la solicitud de cambio **CR-001** en `docs/DECISIONS.md`.
  3. Redacción exhaustiva de `docs/sources/taegeuk1_sources.md` con la tabla de 18 movimientos (6.3), contraste de 6 fuentes secundarias (6.3.1) y resolución justificada de discrepancias (giro de 90° a la derecha en movimiento 17 para asegurar cierre angular a 0°; kihap único en movimiento 18).
  4. Creación de `poomsae_taegeuk1.json` con esquema v2 y validación estructural automatizada con Python:
     - 18 movimientos canónicos ordenados consecutivamente (1 al 18).
     - Rumbos acumulados verificados con cierre exacto a 0° al finalizar.
     - `validation.status = "DOCUMENTALLY_FIXED"`.
     - Duplicación del archivo JSON a `app/src/main/assets/poomsae_taegeuk1.json`.
  5. Creación de `docs/DECISIONS.md`, documentando CR-001, decisiones de producto D-01 a D-08, arquitectura modular `:app` / `:analysis`, y URLs oficiales verificadas de documentación para cada fase.
  6. Creación de `docs/ASSUMPTIONS.md` según el formato del Apéndice B con los supuestos ASM-001 a ASM-006.
  7. Creación de `docs/VERSIONS.md` con catálogo verificado en Google Maven y Maven Central (MediaPipe 1.2.2, CameraX 1.6.2, Room 2.8.5, DataStore 1.3.0, Hilt 2.60.1, Navigation Compose 2.10.2, kotlinx.serialization 1.12.0, MockK 1.14.11, Turbine 1.2.1, etc.).
  8. Redacción de `README.md` con descripción, advertencia obligatoria de secuencia documental no oficial (R-14), diagrama de módulos, pipeline de datos y comandos de compilación.
  9. Inicialización de los archivos de memoria de la sección 15.1: `docs/PROJECT_STATUS.md`, `docs/ARCHITECTURE.md`, `docs/KNOWN_ISSUES.md`, `docs/TEST_RESULTS.md` y `CHANGELOG.md`.
- **Archivos creados / modificados:**
  - `README.md`
  - `poomsae_taegeuk1.json`
  - `app/src/main/assets/poomsae_taegeuk1.json`
  - `docs/sources/taegeuk1_sources.md`
  - `docs/DECISIONS.md`
  - `docs/ASSUMPTIONS.md`
  - `docs/VERSIONS.md`
  - `docs/PROJECT_STATUS.md`
  - `docs/IMPLEMENTATION_LOG.md`
  - `docs/ARCHITECTURE.md`
  - `docs/KNOWN_ISSUES.md`
  - `docs/TEST_RESULTS.md`
  - `CHANGELOG.md`
- **Pruebas ejecutadas:**
  - Inspección del proyecto con Gradle Wrapper (`./gradlew tasks --dry-run` - OK).
  - Script automatizado de validación estructural de `poomsae_taegeuk1.json` (18 movimientos, tipos, giros, cierre angular 0° - OK).
- **Decisiones pendientes:** Ninguna para F0.
- **Siguiente paso:** Proceder a Fase F1 (Inspección y completado del proyecto Android Studio existente).

---

## [2026-11-11] - Fase F1: Inspección y completado del proyecto Android Studio existente

- **Objetivo de la fase:** Inspeccionar el proyecto existente, completar su configuración Gradle (Kotlin DSL), crear el módulo `:analysis` como biblioteca pura de Kotlin/JVM aislada, y configurar el módulo `:app` con Hilt, Jetpack Compose (Material 3), Navigation Compose, temas y catálogo de mensajes en español (México).
- **Acciones realizadas:**
  1. Configuración de `settings.gradle.kts` incorporando ambos módulos: `:app` y `:analysis`.
  2. Creación del módulo `:analysis` (`analysis/build.gradle.kts`, `src/main/kotlin`, `src/test/kotlin`) como biblioteca pura Kotlin/JVM con toolchain Java 21, cero dependencias de Android framework, y dependencias unitarias (JUnit 4, MockK, Turbine, Coroutines Test).
  3. Actualización de `gradle/libs.versions.toml` con versiones estables verificadas para Hilt 2.60.1, KSP 2.2.10-2.0.2, Detekt 1.23.8, ktlint plugin 12.1.2, Navigation Compose 2.8.8, Kotlin Coroutines 1.10.2, Turbine 1.2.1, MockK 1.14.11.
  4. Configuración del módulo `:app` (`app/build.gradle.kts`) con plugins de Android Application, Kotlin Android, KSP, Hilt Android, Detekt y ktlint. Configuración de `minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`, Java 21, Compose activado con Compose Compiler integrado en Kotlin 2.2.10, y enlace con `:analysis` (`implementation(project(":analysis"))`).
  5. Configuración de Dagger Hilt: creación de `TaegeukCoachApp` anotada con `@HiltAndroidApp`, vinculada en `AndroidManifest.xml`.
  6. Actualización de `MainActivity` con `@AndroidEntryPoint` y configuración del host de navegación Jetpack Compose.
  7. Implementación de diseño y tokens Material 3 en `Color.kt` y `Theme.kt` conforme a la Sección 14.2 (paleta primaria, secundaria, superficies, contornos, colores semánticos de resultados VERDE/AMARILLO/ROJO y scrim para overlays de cámara).
  8. Configuración de catálogo de recursos en español de México (`app/src/main/res/values/strings.xml`) cubriendo nombres de pantallas y catálogo formal de mensajes de sistema (Sección 12.8: cámara, pose, calidad, estado y advertencia de no certificación Kukkiwon).
  9. Implementación de arquitectura de navegación:
     - `Screen`: sealed class con rutas tipadas (`home`, `practice`, `summary`, `history`, `settings`).
     - 5 pantallas vacías Compose con títulos accesibles e indicadores de fase: `HomeScreen`, `PracticeScreen`, `SummaryScreen`, `HistoryScreen`, `SettingsScreen`.
     - `AppNavigation`: NavHost que enlaza las 5 pantallas con punto de entrada en Inicio (`home`).
  10. Configuración de análisis estático:
      - Archivo de configuración Detekt en `config/detekt/detekt.yml` ajustado para Jetpack Compose (`FunctionNaming` Composable, `MagicNumber` ignorePropertyDeclaration, `LongMethod` threshold 100).
      - Plugins de Detekt y ktlint aplicados a nivel raíz y en subproyectos con tareas aggregate `detekt` y `ktlintCheck`.
  11. Implementación de automatización y CI:
      - Script local `scripts/verify.sh` ejecutable con verificación por fases: assembleDebug, testDebugUnitTest, detekt y ktlintCheck.
      - Flujo de GitHub Actions en `.github/workflows/ci.yml` configurado con JDK 21.
  12. Creación de pruebas unitarias:
      - `AnalysisModuleMarkerTest` en `:analysis` para validar aislamiento JVM.
      - `NavigationTest` en `:app` para validar completitud de rutas y consistencia de destinos.
- **Archivos creados / modificados:**
  - `build.gradle.kts`
  - `settings.gradle.kts`
  - `gradle.properties`
  - `gradle/gradle-daemon-jvm.properties`
  - `gradle/libs.versions.toml`
  - `.gitignore`
  - `.github/workflows/ci.yml`
  - `scripts/verify.sh`
  - `config/detekt/detekt.yml`
  - `analysis/build.gradle.kts`
  - `analysis/src/main/kotlin/tkd/analysis/AnalysisModuleMarker.kt`
  - `analysis/src/test/kotlin/tkd/analysis/AnalysisModuleMarkerTest.kt`
  - `app/build.gradle.kts`
  - `app/proguard-rules.pro`
  - `app/src/main/AndroidManifest.xml`
  - `app/src/main/java/tkd/apptkd/TaegeukCoachApp.kt`
  - `app/src/main/java/tkd/apptkd/MainActivity.kt`
  - `app/src/main/java/tkd/apptkd/ui/navigation/Screen.kt`
  - `app/src/main/java/tkd/apptkd/ui/navigation/AppNavigation.kt`
  - `app/src/main/java/tkd/apptkd/ui/screens/HomeScreen.kt`
  - `app/src/main/java/tkd/apptkd/ui/screens/PracticeScreen.kt`
  - `app/src/main/java/tkd/apptkd/ui/screens/SummaryScreen.kt`
  - `app/src/main/java/tkd/apptkd/ui/screens/HistoryScreen.kt`
  - `app/src/main/java/tkd/apptkd/ui/screens/SettingsScreen.kt`
  - `app/src/main/java/tkd/apptkd/ui/theme/Color.kt`
  - `app/src/main/java/tkd/apptkd/ui/theme/Theme.kt`
  - `app/src/main/java/tkd/apptkd/ui/theme/Type.kt`
  - `app/src/main/res/values/strings.xml`
  - `app/src/test/java/tkd/apptkd/ExampleUnitTest.kt`
  - `app/src/test/java/tkd/apptkd/ui/NavigationTest.kt`
  - `docs/DECISIONS.md`
  - `docs/PROJECT_STATUS.md`
  - `docs/TEST_RESULTS.md`
  - `CHANGELOG.md`
- **Pruebas ejecutadas:**
  - `./gradlew assembleDebug` (OK - APK de depuración generado).
  - `./gradlew testDebugUnitTest` y `./gradlew test` (OK - 100% pruebas unitarias pasando en ambos módulos).
  - `./gradlew detekt` (OK - 0 violaciones reportadas).
  - `./gradlew ktlintCheck` (OK - 0 violaciones de estilo de código).
  - `./scripts/verify.sh` (OK - Suite completa en verde).
- **Decisiones pendientes:** Ninguna.
- **Siguiente paso:** Proceder a Fase F2 (Módulo :analysis, modelos geométricos puros y validador del poomsae).

---

## Sesión 3 — Fase F2: Módulo `:analysis`: modelos y geometría
- **Fecha:** 2026-11-12
- **Objetivo:** Construir la base matemática probada del proyecto en Kotlin/JVM puro (`Joint`, `Landmark`, `PoseFrame`, `PoseEstimator`, `MetricResult`, `Vec3`, `Vec2`, primitivas 13.2, tolerancias `EPS_*` en `AnalysisConfig` y filtros de señal `OneEuroFilter`, `OneEuroFilter3D`, `ExponentialMovingAverageFilter`, `SignalStats`).
- **Artefactos creados/modificados:**
  - `gradle/libs.versions.toml`
  - `analysis/build.gradle.kts`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/config/AnalysisConfig.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/model/Joint.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/model/Landmark.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/model/PoseFrame.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/model/MetricResult.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/geometry/GeometryResult.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/geometry/Vec3.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/geometry/Vec2.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/geometry/AngleGeometry.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/geometry/BodyPrimitives.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/signal/SignalStats.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/signal/ExponentialMovingAverageFilter.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/signal/OneEuroFilter.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/signal/OneEuroFilter3D.kt`
  - `analysis/src/test/kotlin/mx/taekwondo/poomsae/analysis/geometry/TestSkeletonHelper.kt`
  - `analysis/src/test/kotlin/mx/taekwondo/poomsae/analysis/geometry/Vec3AndVec2Test.kt`
  - `analysis/src/test/kotlin/mx/taekwondo/poomsae/analysis/geometry/AngleGeometryTest.kt`
  - `analysis/src/test/kotlin/mx/taekwondo/poomsae/analysis/geometry/BodyPrimitivesTest.kt`
  - `analysis/src/test/kotlin/mx/taekwondo/poomsae/analysis/signal/SignalFilterTest.kt`
  - `docs/PROJECT_STATUS.md`, `docs/TEST_RESULTS.md`, `CHANGELOG.md`
- **Pruebas ejecutadas:**
  - `./gradlew :analysis:test :analysis:jacocoTestReport` (OK — 34/34 tests en verde; cobertura `geometry`: 96.03%, `signal`: 98.09%, `model`: 100.00%).
  - `./gradlew assembleDebug test detekt ktlintCheck` (OK — 67 tareas exitosas, 0 errores, 0 violaciones Detekt/ktlint).
- **Decisiones pendientes:** Ninguna.
- **Siguiente paso:** Proceder a Fase F3 (Definición del poomsae: carga y validación estricta v2 de `poomsae_taegeuk1.json`).

---

## Sesión 4 — Fase F3: Definición del poomsae
- **Fecha:** 2026-11-12
- **Objetivo:** Cargar y validar estrictamente la secuencia `poomsae_taegeuk1.json` (esquema v2, sección 6.5) mediante `kotlinx.serialization`, exponiendo `PoomsaeDefinition` con `validationStatus`, cálculo de rumbo acumulado, verificación de cierre angular y resolución de fichas de métricas 13.4.
- **Artefactos creados/modificados:**
  - `build.gradle.kts`, `analysis/build.gradle.kts`, `gradle/libs.versions.toml` (`kotlinxSerialization` ajustada a `1.9.0` por compatibilidad binaria con Kotlin `2.2.10`)
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/poomsae/PoomsaeModels.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/poomsae/PoomsaeValidationError.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/poomsae/PoomsaeValidator.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/poomsae/PoomsaeLoader.kt`
  - `analysis/src/main/kotlin/mx/taekwondo/poomsae/analysis/poomsae/MetricSheetRegistry.kt`
  - `analysis/src/test/kotlin/mx/taekwondo/poomsae/analysis/poomsae/PoomsaeLoaderTest.kt`
  - `docs/VERSIONS.md`, `docs/PROJECT_STATUS.md`, `docs/TEST_RESULTS.md`, `CHANGELOG.md`
- **Pruebas ejecutadas:**
  - `./gradlew :analysis:test :analysis:jacocoTestReport` (OK — 44/44 tests en `:analysis`, 10/10 en `PoomsaeLoaderTest`).
  - `./gradlew assembleDebug test :analysis:jacocoTestReport detekt ktlintCheck` (OK — 0 errores, 0 violaciones Detekt/ktlint).
- **Decisiones pendientes:** Ninguna.
- **Siguiente paso:** Proceder a Fase F4 (Evaluador de postura, técnicas y métricas biomecánicas 13.4).


