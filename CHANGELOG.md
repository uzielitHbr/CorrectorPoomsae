# Changelog

Todas las modificaciones notables a este proyecto serán documentadas en este archivo.
El formato se basa en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).

## [Unreleased]

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

