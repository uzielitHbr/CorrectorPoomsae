# Registro de Decisiones de Arquitectura y Producto (docs/DECISIONS.md)

**Proyecto:** Taegeuk Coach  
**Versión de especificación:** 3.0  
**Fecha de inicio:** 11 de noviembre de 2026  

---

## 1. Solicitudes de Cambio y Fijaciones Normativas

### CR-001: Fijación documental de la secuencia Taegeuk 1 Jang por contraste de fuentes publicadas
- **Fecha:** 11 de noviembre de 2026
- **Estado:** `APROBADO` / `DOCUMENTALLY_FIXED`
- **Fase:** F0
- **Motivo y Contexto:**
  El proyecto Android existente no contaba con una definición formal machine-readable de la forma Taegeuk 1 Jang (Taegeuk Il Jang). Para iniciar el desarrollo de forma autónoma sin depender de validaciones presenciales de un profesor o de la adquisición directa del manual impreso de Kukkiwon (*Taekwondo Textbook*, Kim 2012), se contrastaron fuentes secundarias independientes de dominio público.
- **Fuentes contrastadas:**
  - Yong Gi Taekwondo (s. f.) — Base de la secuencia.
  - Tae Kwon Do Nation (2026).
  - Taekwondo Wiki (s. f.).
  - Blue Dragon Taekwondo (s. f.).
  - Cheongnyongyu (s. f.).
  - United Taekwondo (s. f.).
- **Discrepancias resueltas:**
  1. *Movimiento 17:* Se adopta giro de 90° a la derecha (+90°) en postura *AK izquierda* con bloqueo bajo *arae makki* (brazo izquierdo). Es la variante respaldada por 5 de las 6 fuentes y la única matemáticamente consistente que permite el cierre angular del poomsae tras el giro final de regreso.
  2. *Ubicación del Kihap:* Se fija un único *kihap* en el movimiento 18 (último golpe), descartando el reporte aislado de Fight Encyclopedia en el movimiento 7 (incompatible con la técnica de bloqueo).
  3. *Conteo:* 18 movimientos canónicos consecutivos (1 al 18).
- **Consecuencias e Impacto:**
  - El archivo `poomsae_taegeuk1.json` queda fijado como fuente única de verdad con `validation.status = "DOCUMENTALLY_FIXED"`.
  - La interfaz de usuario, exportaciones y `README.md` deben exhibir obligatoriamente la advertencia: *"Secuencia fijada documentalmente; no certificada oficialmente"* (Regla R-14).
  - La eventual consulta del libro oficial de Kukkiwon se clasifica como mejora futura opcional vía solicitud de cambio (sección 15.3).

---

## 2. Decisiones de Producto (Sección 12.2)

| ID | Decisión | Detalle / Justificación | Estado |
| :--- | :--- | :--- | :--- |
| **D-01** | Compatibilidad mínima (`minSdk`) | Se define `minSdk = 26` (Android 8.0 Oreo). Justificación: MediaPipe Tasks Vision requiere `minSdk >= 24`; CameraX requiere `minSdk >= 21`; `minSdk 26` provee soporte nativo para `java.time` sin desugaring pesado y cubre >95% del ecosistema activo. | **Vigente** |
| **D-02** | Selección de cámara por defecto | Cámara frontal predeterminada (permite al practicante auto-observarse frente al teléfono a 2-3 m). Configurable a cámara trasera en Ajustes. | **Vigente** |
| **D-03** | Orientación de pantalla | Práctica bloqueada en orientación vertical (*portrait*) para capturar el cuerpo completo del usuario de pies a cabeza. Pantallas de Inicio, Historial, Resumen y Ajustes son libres. | **Vigente** |
| **D-04** | Modo de práctica por defecto | Guiado continuo por defecto. Modo estricto opcional configurable (repite el movimiento ante fallo hasta `STRICT_MAX_ATTEMPTS`). | **Vigente** |
| **D-05** | Mecánica de repetición | El usuario puede repetir el movimiento actual o toda la forma. El puntaje toma el último intento y preserva el contador de intentos. | **Vigente** |
| **D-06** | Sesión interrumpida y borrador | Guardado automático de borrador en Room/DataStore ante interrupciones (llamada, app en background, suspensión). Recuperable si su antigüedad es $\le$ `DRAFT_MAX_AGE_HOURS`. | **Vigente** |
| **D-07** | Criterio de terminación de forma | La evaluación formal concluye al evaluar el movimiento 18. El regreso a *junbi seogi* es una transición física que no recibe puntaje. | **Vigente** |
| **D-08** | Idioma y localización | Español de México (`es-MX`) como idioma predeterminado en `strings.xml`. Todo texto visible proviene de recursos sin texto embebido (R-10). | **Vigente** |

---

## 3. Decisiones Técnicas y de Arquitectura (Secciones 4 y 5)

| Componente | Decisión Adoptada | Justificación Técnica | Documentación Oficial Verificada |
| :--- | :--- | :--- | :--- |
| **Lenguaje** | Kotlin 2.2.10 (compatible con compilador Compose integrado) | Lenguaje estándar oficial de Android. Tipos inmutables, `sealed interface`, corrutinas nativas. | `https://kotlinlang.org/docs/home.html` |
| **UI** | Jetpack Compose + Material 3 (BOM `2026.02.01`) | Estándar declarativo de Android. Soporte para temas dinámicos, TalkBack, previews y escalado tipográfico. | `https://developer.android.com/develop/ui/compose` |
| **Cámara** | CameraX 1.6.2 (`Preview` + `ImageAnalysis`) | API robusta con manejo de ciclo de vida, contrapresión de descarte de cuadros (`STRATEGY_KEEP_ONLY_LATEST`) y soporte multi-cámara. | `https://developer.android.com/training/camerax/analyze` |
| **Detección de Pose** | MediaPipe Tasks Vision (`com.google.mediapipe:tasks-vision:1.2.2`) | Detección on-device de 33 landmarks corporales en coordenadas 2D de imagen y 3D métricas de mundo. Modo `LIVE_STREAM`. | `https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/android` |
| **Concurrencia** | Corrutinas Kotlin + `StateFlow` / `SharedFlow` | Concurrencia estructurada, reactiva y testeable en JVM y Android sin bloqueo de hilo principal (R-11). | `https://kotlinlang.org/docs/coroutines-overview.html` |
| **Arquitectura** | MVVM + Unidirectional Data Flow (UDF) | Separación estricta en 2 módulos Gradle: `:analysis` (JVM puro, sin dependencias Android) y `:app` (Android, UI, hardware). | `https://developer.android.com/topic/architecture` |
| **Inyección de Dependencias** | Dagger Hilt 2.60.1 / `androidx.hilt:hilt-navigation-compose:1.4.0` | Inyección de dependencias estándar en Android, soporte de ciclo de vida de ViewModels y Navigation Compose. | `https://developer.android.com/training/dependency-injection/hilt-android` |
| **Persistencia** | Room 2.8.5 (BD local) + DataStore Preferences 1.3.0 | Soluciones oficiales de Jetpack. Room almacena sesiones y resultados con esquema exportado; DataStore guarda ajustes de usuario. | `https://developer.android.com/training/data-storage/room` |
| **Serialización** | `kotlinx.serialization` JSON 1.12.0 | Carga declarativa, segura y testeable del poomsae y fixtures de prueba. | `https://github.com/Kotlin/kotlinx.serialization` |
| **Navegación** | Jetpack Navigation Compose 2.10.2 | Navegación declarativa entre pantallas base con backstack controlado. | `https://developer.android.com/guide/navigation/design` |
| **Pruebas** | JUnit 4.13.2, MockK 1.14.11, Turbine 1.2.1, Coroutines-Test 1.10.2 | Cobertura integral de pruebas unitarias en JVM para `:analysis` ($\ge 80\%$) y flujos de UI. | `https://developer.android.com/training/testing` |
| **Análisis Estático** | Detekt 1.23.8 y ktlint | Control de calidad de código, formato y ausencia de números mágicos o antipatrones. | `https://detekt.dev/` |

---

## 4. Fuentes Oficiales de Documentación por Fase (R-03)

- **F0 / General:**
  - Especificación Técnica y Plan de Ejecución Autónoma Taegeuk Coach v3.0 (11 de noviembre de 2026).
  - Taekwondo Kukkiwon / World Taekwondo Poomsae guidelines.
- **F1 (Infraestructura y Gradle):**
  - Android Developers Gradle DSL & Version Catalog: `https://developer.android.com/build`
  - Jetpack Compose Foundation: `https://developer.android.com/develop/ui/compose`
  - Hilt Dependency Injection: `https://developer.android.com/training/dependency-injection/hilt-android`
- **F2 / F3 (:analysis, Geometría y Poomsae):**
  - Kotlinx Serialization Guide: `https://github.com/Kotlin/kotlinx.serialization`
  - MediaPipe Pose Landmark model card & coordinates: `https://developers.google.com/mediapipe/solutions/vision/pose_landmarker`
- **F4 / F5 (CameraX y Detección en Vivo):**
  - CameraX ImageAnalysis Reference: `https://developer.android.com/reference/androidx/camera/core/ImageAnalysis`
  - MediaPipe Pose Landmarker Android Guide: `https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/android`
- **F9 (Compose UI y Feedback):**
  - Material 3 Design Tokens: `https://m3.material.io/`
  - Android Accessibility Principles: `https://developer.android.com/guide/topics/ui/accessibility`
- **F10 (Persistencia y Exportación):**
  - Android Room Training Guide: `https://developer.android.com/training/data-storage/room`
  - Android DataStore: `https://developer.android.com/topic/libraries/architecture/datastore`
  - Android PdfDocument API: `https://developer.android.com/reference/android/graphics/pdf/PdfDocument`
  - Android FileProvider: `https://developer.android.com/reference/androidx/core/content/FileProvider`
