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

---

## 5. Decisiones Técnicas Específicas de la Fase F1 (Gradle, Tooling y SDK)

### D-F1-01: Fijación y Justificación de Niveles de SDK (`minSdk = 26`, `compileSdk = 37`, `targetSdk = 37`)
- **Fecha:** 11 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  1. **`minSdk = 26` (Android 8.0 Oreo):**
     - Requisitos de dependencias clave: MediaPipe Tasks Vision (`tasks-vision`) exige `minSdk >= 24`; CameraX (`camera-core`, `camera-camera2`, `camera-lifecycle`) exige `minSdk >= 21`.
     - `minSdk 26` garantiza soporte nativo de la API temporal `java.time` (Instant, Duration) sin necesidad de desugaring agresivo y cubre >95% de los dispositivos Android activos a nivel global.
  2. **`compileSdk = 37` y `targetSdk = 37`:**
     - Establecidos por la plantilla moderna de Android Studio / AGP 9 para soportar las últimas optimizaciones del runtime y APIs del sistema Android, manteniendo compatibilidad hacia atrás mediante el toolchain Java 21.

### D-F1-02: Versión de Dagger Hilt (2.60.1) ante Android Gradle Plugin 9.3
- **Fecha:** 11 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - AGP 9 eliminó la clase legada `com.android.build.gradle.BaseExtension`. Versiones previas de Hilt ($\le 2.58$) arrojaban `NoClassDefFoundError: com/android/build/gradle/BaseExtension` al aplicar el plugin `dagger.hilt.android.plugin`.
  - Hilt `2.60.1` fue verificado como la versión estable que implementa la nueva API de extensiones de AGP 9.

### D-F1-03: Fijación del Toolchain JVM del Demonio Gradle a JDK 21
- **Fecha:** 11 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - Detekt 1.23.8 utiliza el compilador Kotlin/JVM embebido con el parser de AST de IntelliJ. Al ejecutarse el demonio de Gradle sobre OpenJDK 25, Detekt fallaba con `IllegalArgumentException: 25` debido a que la versión de bytecode 25 no es reconocida por el analizador de Detekt.
  - Se configuró `gradle/gradle-daemon-jvm.properties` con `toolchainVersion=21` y `org.gradle.java.home=/Library/Java/JavaVirtualMachines/jdk-21.jdk/Contents/Home` en `gradle.properties`, garantizando reproducibilidad y estabilidad en CI y local.

### D-F1-04: Configuración de KSP con Kotlin Integrado en AGP 9
- **Fecha:** 11 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - AGP 9 con Kotlin integrado impone validación estricta sobre la modificación de `kotlin.sourceSets`. KSP añade directorios autogenerados mediante esta API legada.
  - Se habilitó `android.disallowKotlinSourceSets=false` y se suprimió la advertencia correspondiente en `gradle.properties` según la documentación oficial de migración de AGP 9.

### D-F1-05: Reglas de Análisis Estático (Detekt y ktlint) para Jetpack Compose
- **Fecha:** 11 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - En Detekt (`config/detekt/detekt.yml`), se configuró la regla `naming > FunctionNaming` con `ignoreAnnotated: ['Composable']` para permitir la convención estándar de Jetpack Compose (funciones PascalCase).
  - Se habilitó `ignorePropertyDeclaration: true` en `style > MagicNumber` para permitir valores hexadecimales de color de Material 3 (`0xFF...`).
  - Se ajustó el umbral de `complexity > LongMethod` a 100 para permitir definiciones declarativas de pantallas Compose completas.
  - Se integró `org.jlleitschuh.gradle.ktlint` (versión 12.1.2) con tareas paraguas `ktlintCheck` y `ktlintFormat`.

---

## 6. Decisiones Técnicas de las Fases F4 y F5 (Cámara, Permisos y Detección de Pose)

### D-F4-01: Gestión de Permisos de Cámara con Máquina de Estados Desacoplada y Navegación a Ajustes
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - El flujo de permisos se desacopló en `CameraPermissionViewModel` con una máquina de estados sellada (`CameraPermissionState`: `NotDetermined`, `RationaleRequired`, `Requesting`, `Granted`, `Denied`, `PermanentlyDenied`).
  - Cumple estrictamente con la guía de Android y los mensajes normativos en español de México (`MSG_PERM_RATIONALE` y `MSG_PERM_SETTINGS`, Sección 12.8).
  - Al recibir una denegación permanente ("No volver a preguntar"), la UI proporciona un acceso directo a los ajustes del sistema (`Settings.ACTION_APPLICATION_DETAILS_SETTINGS`).

### D-F4-02: Estrategia de Contrapresión de CameraX (`STRATEGY_KEEP_ONLY_LATEST`) y Ejecutor Dedicado
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - En cumplimiento de la sección 12.9.1, `ImageAnalysis` se configura con `STRATEGY_KEEP_ONLY_LATEST` y formato `RGBA_8888`.
  - La captura y análisis corren en un ejecutor dedicado de un solo hilo (`Executors.newSingleThreadExecutor()`). Nunca se ejecutan transformaciones de imagen ni inferencias en el hilo principal.
  - La liberación de buffers de memoria gráfica (`ImageProxy.close()`) se garantiza mediante bloques `finally`.

### D-F5-01: Selección del Modelo MediaPipe Pose Landmarker Lite (`pose_landmarker_lite.task`) y Licencia Apache 2.0
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - Se seleccionó la variante **Lite** (modelo en coma flotante FP16, tamaño $5.5\text{ MB}$) para optimizar latencia y consumo de batería en dispositivos móviles en tiempo real ($30\text{ fps}$).
  - El modelo fue descargado del repositorio oficial de MediaPipe y colocado en `app/src/main/assets/pose_landmarker_lite.task`.
  - **Licencia:** Distribuido por Google LLC bajo licencia Apache 2.0. Se registró la licencia en `app/src/main/assets/MODEL_LICENSE.txt` y en este documento.
  - La arquitectura deja la ruta del modelo configurable en `MediaPipePoseEstimator` para permitir comparaciones futuras con variantes Full/Heavy.

### D-F5-02: Adaptador Puro y Modelo de Concurrencia con Timestamps Estrictamente Crecientes (Sección 12.9.1)
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - `MediaPipePoseAdapter` es una clase pura sin dependencias de Android que mapea los resultados de MediaPipe a la entidad canónica `PoseFrame` (`image` y `world`).
  - Mapea las 17 articulaciones requeridas por la sección 5.4, validando específicamente que `RIGHT_WRIST` corresponda al índice 16.
  - Exige marcas de tiempo estrictamente crecientes (`timestampMs > lastTimestampMs`). Cuadros con marcas no crecientes o negativas son rechazados y contabilizados en `droppedFrames`.
  - En la vista previa de la cámara frontal, el lienzo de Compose aplica espejado horizontal (`canvasX = (1.0 - rawX) * width`) para interacción natural, mientras que las coordenadas internas de análisis se conservan sin reflejar (Regla R-23).

---

## 6. Decisiones Técnicas Específicas de la Fase F10 (Persistencia, Historial y Exportación)

### D-F10-01: Esquema Relacional de Room y Mapeo en Cascada con Resguardo de Esquemas JSON
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - Persistencia de `SessionEntity` y `MovementResultEntity` con clave foránea en cascada (`ForeignKey.CASCADE`) e índices por `startedAtMs` y `sessionId`.
  - Prohibido el uso de `fallbackToDestructiveMigration()`. Esquemas JSON exportados en `app/schemas/` para verificación estática de Room.
  - Soporte de borrador de sesión interrumpida (`SessionDraftEntity`, D-06) con vigencia máxima de 24 horas (`DRAFT_MAX_AGE_HOURS`).

### D-F10-02: Borrado Irreversible de Datos (SC-30) y Exportación con FileProvider y PdfDocument
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - `DataErasureService` implementa borrado atómico e irreversible de bases de datos Room, preferencias DataStore, archivos de borrador y capturas de pantalla asociadas sin dejar rastro (R-09).
  - Exportación de resumen técnico en texto plano y PDF generado mediante `android.graphics.pdf.PdfDocument`, servido mediante `FileProvider` con permisos temporales `FLAG_GRANT_READ_URI_PERMISSION`.

---

## 7. Decisiones Técnicas Específicas de la Fase F11 (Análisis de Video Grabado)

### D-F11-01: Selector de Video del Sistema (`PickVisualMedia` / PhotoPicker)
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación Oficial:**
  - Se seleccionó `ActivityResultContracts.PickVisualMedia` configurado con `VisualMediaType.VideoOnly`.
  - **Documentación oficial verificada:** Android Developers Guide (*Photo Picker*, `https://developer.android.com/training/data-storage/shared/photopicker`).
  - Cumple estrictamente con las directrices de privacidad de Android moderno: no requiere permisos de almacenamiento en tiempo de ejecución (`READ_MEDIA_VIDEO` o `READ_EXTERNAL_STORAGE` son innecesarios), funciona de forma retrocompatible en Android 8.0+ mediante los servicios de Google Play y otorga un `Uri` de acceso de lectura temporal y acotado.

### D-F11-02: API de Decodificación y Muestreo por Cuadros (`MediaMetadataRetriever` con `getScaledFrameAtTime`)
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación Oficial:**
  - Se eligió `MediaMetadataRetriever` con `getScaledFrameAtTime(timeUs, option, dstWidth, dstHeight)` (API 27+) y fallback retrocompatible con escalado en API 26.
  - **Documentación oficial verificada:** `https://developer.android.com/reference/android/media/MediaMetadataRetriever#getScaledFrameAtTime(long,%20int,%20int,%20int)`.
  - **Comparativa técnica frente a MediaCodec + Surface:**
    - `MediaCodec` requiere inicialización asíncrona de decodificadores de hardware, enlaces con `SurfaceTexture`/`ImageReader` o contextos OpenGL, y una gestión compleja de colorimetría para muestreo estático.
    - `MediaMetadataRetriever.getScaledFrameAtTime` realiza la reducción de escala directamente en el subsistema de medios nativo a la dimensión máxima acotada (`VIDEO_DOWNSCALE_TARGET_MAX_DIM = 640`), evitando que mapas de bits a resolución nativa 1080p/4K ocupen memoria en el heap de la JVM.
    - Permite muestreo temporal determinista (`VIDEO_FRAME_INTERVAL_MS = 66L`, ~15 fps), sincronizado con el motor `SessionEngine`.
    - Extrae de manera instantánea metadatos de duración, dimensiones, rotación y presencia de pista de video para rechazo temprano.

### D-F11-03: MediaPipe Pose Landmarker en `RunningMode.VIDEO` y Timestamps Intrínsecos
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación Oficial:**
  - Se configuró `PoseLandmarker` en `RunningMode.VIDEO` invocando `detectForVideo(image, timestampMs)`.
  - **Documentación oficial verificada:** `https://developers.google.com/edge/mediapipe/solutions/vision/pose_landmarker/android#video`.
  - A diferencia de `RunningMode.LIVE_STREAM` (asíncrono con descarte de cuadros), `RunningMode.VIDEO` es síncrono y garantiza que ningún cuadro sea descartado por saturación de CPU, procesando el video a ritmo de decodificación en hilo de fondo (`Dispatchers.Default`).
  - Las marcas de tiempo `timestampMs` corresponden estrictamente a la posición temporal intrínseca del video en milisegundos (`timeMs = 0, 66, 132, ...`), satisfaciendo la exigencia de marcas estrictamente monótonas crecientes de MediaPipe y la arquitectura de línea de tiempo única de `PoseFrame`.

### D-F11-04: Corrección de Orientación y Contención de Memoria ($O(1)$)
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - Los videos grabados en teléfonos móviles suelen registrarse con rotación de 90° o 270°. `VideoFrameRotator` lee `METADATA_KEY_VIDEO_ROTATION` y aplica `Matrix.postRotate` si la rotación normalizada es distinta de 0°.
  - Para evitar fugas de memoria o errores OutOfMemory (OOM), el bitmap original antes de la rotación es reciclado inmediatamente (`bitmap.recycle()`).
  - Cada mapa de bits decodificado es reciclado inmediatamente en el bloque `finally` tras pasar por `detector.processFrame`, garantizando un uso de memoria constante ($O(1)$, un único bitmap en vuelo a la vez).

### D-F11-05: Rechazo Anticipado (SC-28) y Cancelación Inmediata de Recursos (SC-29)
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación:**
  - **SC-28:** Antes de decodificar ningún cuadro, `VideoValidator` verifica en pure Kotlin los límites estipulados en `AnalysisConfig`:
    - Duración máxima: 180 s (`VIDEO_MAX_DURATION_SECONDS`).
    - Tamaño máximo: 200 MB (`VIDEO_MAX_FILE_SIZE_BYTES`).
    - Resolución máxima: 1920x1920 (`VIDEO_MAX_WIDTH`, `VIDEO_MAX_HEIGHT`).
    - Formato y pista de video válida (`hasVideoTrack`, dimensiones > 0, duración > 0).
    - Los fallos emiten mensajes específicos en español de México (`video_err_too_long`, `video_err_too_large`, `video_err_resolution_too_high`, `video_err_unsupported_format`).
  - **SC-29:** La interfaz presenta un diálogo modal `VideoProcessingDialog` con porcentaje y botón «Cancelar». Al pulsar cancelar, la corrutina se cancela inmediatamente (`currentCoroutineContext().isActive`), el bucle de decodificación finaliza en la siguiente verificación (~66 ms), se liberan los recursos nativos (`retriever.release()`, `detector.close()`), no se persiste nada en base de datos ni borrador, y se muestra el aviso «Análisis cancelado» (`MSG_VIDEO_CANCEL`).

---

## 8. Decisiones Técnicas Específicas de la Fase F12 (Calibración y Validación con Datos Reales)

### D-F12-01: Adopción del Estado Normativo NO_VALIDADO y Salvaguarda de Parámetros PENDIENTE_CALIBRAR
- **Fecha:** 12 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación Oficial (Sección 16.5 y R-05):**
  - **Restricción Estricta de Integridad de Datos:** Al no contar en la fase actual de desarrollo autónomo con grabaciones biométricas humanas bajo consentimiento informado (LFPDPPP/GDPR) ni con un panel ciego de jueces o instructores certificados (4.° Dan Kukkiwon / árbitros oficiales de poomsae), se prohíbe explícitamente simular o inventar datos empíricos.
  - **Conservación de Ciclo de Vida de Parámetros:** Todos los parámetros heurísticos y cinemáticos en `AnalysisConfig` se mantienen en estado `ParameterCalibrationStatus.PENDIENTE_CALIBRAR`. No se alteran ni forzan valores mágicos.
  - **Operación en Modo Experimental:** Se formaliza el estado `ValidationState.NO_VALIDADO`. La clase evaluadora `ValidationStateEvaluator`:
    1. Garantiza `isExperimental() == true`.
    2. Prohíbe terminantemente la exhibición de insignias o textos afirmativos «Validado» (`canDisplayValidatedBadge() == false`).
    3. Asegura que la interfaz (`HomeScreen`, `PracticeScreen`, `SummaryScreen`, `HistoryScreen`) y el motor de exportación (`SessionExporter`) muestren los avisos obligatorios `MSG_EXPERIMENTAL`, `MSG_DISCLAIMER` y `MSG_UNVALIDATED`.
  - **Documentación:** Se emite el informe formal exhaustivo `docs/VALIDATION_REPORT.md` con el inventario completo de los 37 parámetros, el protocolo de calibración biométrica futura y los criterios formales para transitar a los niveles 1 y 2.

---

## 9. Decisiones Técnicas Específicas de la Fase F13 (Cierre, Calidad Final y Empaquetado v1.0.0)

### D-F13-01: Configuración de Compilación Release, Reglas ProGuard/R8, Auditorías y Empaquetado
- **Fecha:** 13 de noviembre de 2026
- **Estado:** Aprobado / Implementado
- **Justificación Oficial (Sección 16.6):**
  - **Reducción de Código y Recursos (R8 / ProGuard):** Se activaron `isMinifyEnabled = true` e `isShrinkResources = true`. Se redactó exhaustivamente `app/proguard-rules.pro` conservando clases y métodos nativos JNI de MediaPipe (`com.google.mediapipe.**`), serializadores de `kotlinx.serialization`, entidades/DAOs de Room (`androidx.room.**`), modelos inmutables de `:analysis` y componentes Hilt/DataStore. Se agregaron reglas `-dontwarn` para protobufs opcionales de MediaPipe.
  - **Auditoría de Accesibilidad:** Se integraron semánticas `contentDescription` en todos los controles `IconButton` interactivos (retroceso, ajustes, exportar, borrar, pausar, cerrar, descartar banner). Se verificó la adaptabilidad a escalado de fuentes al 200% mediante `verticalScroll` y contenedor scrollable en `ConfirmDialog`.
  - **Auditoría de Permisos (Sin Red):** Se auditó `AndroidManifest.xml` confirmando que únicamente se solicita `android.permission.CAMERA`. Se confirmó la ausencia total de `android.permission.INTERNET`, garantizando que la app opera 100% desconectada y fuera de línea sin fugas de datos.
  - **Transparencia y Privacidad:** Se integraron diálogos modales en Ajustes para el Aviso de Privacidad Integral (explicando procesamiento local y borrado permanente) y las Licencias de Terceros de Código Abierto (Apache 2.0).
  - **Generación de Artefacto Release:** Se ejecutó `assembleRelease` produciendo el APK optimizado `app-release.apk` con hash SHA-256 `b13c3fae2db711f9bd85620ad6f92c06e183787f2c7c15c72e788b6c1f4ae4c4`. La firma con la clave privada de producción del usuario queda registrada formalmente como Pendiente Externo.
  - **Lista de Verificación Global:** Se completó y auditó al 100% la lista de verificación del Apéndice D en `docs/CHECKLIST_APPENDIX_D.md`.






