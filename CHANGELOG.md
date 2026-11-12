# Changelog

Todas las modificaciones notables a este proyecto serán documentadas en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

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

