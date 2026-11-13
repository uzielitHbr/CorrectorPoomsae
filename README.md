# Taegeuk Coach (태극 코치) — v1.0.0

> **Aplicación Android nativa en Kotlin para el análisis biomecánico, guía en tiempo real y retroalimentación del poomsae Taegeuk 1 Jang (Taegeuk Il Jang).**  
> Proyecto de investigación para el tema libre de la tesis de grado de cinta negra en Taekwondo.

---

> [!WARNING]
> ### Secuencia fijada documentalmente; no certificada oficialmente
> Esta aplicación es exclusivamente una herramienta de asistencia y apoyo al entrenamiento y la práctica individual. La secuencia de movimientos ha sido fijada documentalmente mediante el procedimiento **CR-001** (*Contraste documental con fuentes secundarias publicadas*). No constituye una certificación oficial emitida por el **Kukkiwon** ni por **World Taekwondo (WT)**. Toda puntuación y sugerencia de corrección es meramente orientativa y no sustituye la supervisión de un profesor o sinodal acreditado (Regla **R-14**).
> La aplicación opera bajo el estado normativo **Modo Experimental (`NO_VALIDADO`)** con parámetros 100% `PENDIENTE_CALIBRAR` en estricto apego a la Sección 16.5 de la especificación técnica.

---

## 1. Alcance y Objetivos

- **Poomsae evaluado:** Taegeuk 1 Jang (18 movimientos canónicos según la secuencia fijada documentalmente en F0/F3 con cierre angular exacto a 0° de rumbo acumulado).
- **Procesamiento 100% en el dispositivo (*on-device*):** Estimación de pose articular mediante Google MediaPipe Tasks Vision (`RunningMode.LIVE_STREAM` para cámara en vivo y `RunningMode.VIDEO` síncrono para video grabado).
- **Privacidad desde el diseño (RNF-02):** La aplicación no solicita el permiso de red (`android.permission.INTERNET`). Ninguna imagen, video o métrica biométrica sale del dispositivo.
- **Modos de uso:**
  - *Práctica guiada en vivo:* Detección y evaluación continua mediante la cámara frontal o trasera, con orientación vertical bloqueada, superposición de esqueleto y flechas de giro.
  - *Análisis sobre video grabado:* Procesamiento fuera de línea cuadro por cuadro mediante el PhotoPicker del sistema (`PickVisualMedia`), decodificación con reducción de escala nativa a 640 px y memoria acotada ($O(1)$).
- **Evaluación biomecánica completa (Sección 13.4):**
  - Posturas (*stances*): *Ap seogi* y *Ap kubi*.
  - Técnicas de defensa y ataque (*techniques*): *Arae makki*, *Momtong jireugi*, *Momtong an makki*, *Eolgul makki* y *Ap chagi*.
  - Rumbos acumulados, ángulos de giros (90° / 180°), flexión de rodillas y codos, altura de bloqueos y golpes, y tiempo de sostén.

---

## 2. Arquitectura del Sistema

El proyecto implementa una separación modular estricta entre la lógica de análisis matemático y el subsistema Android:

```
Taegeuk Coach (Raíz)
├── app/                  # Módulo Android: Jetpack Compose (Material 3), CameraX, MediaPipe, Room, DataStore, Hilt
│   └── src/main/...
└── analysis/             # Módulo JVM puro (sin dependencias Android):
    └── src/main/kotlin/  # Geometría 3D inmutable, filtros One Euro / EMA, segmentación temporal, métricas y evaluación
```

### Pipeline de Procesamiento de Datos

```
Cámara en vivo (CameraX) / Video decodificado (MediaMetadataRetriever)
    │
    ▼
MediaPipe Pose Landmarker (33 landmarks 2D/3D con marcas de tiempo monótonas crecientes)
    │
    ▼
Compuerta de Calidad y Filtrado (FrameQualityGate, Calibrador antropométrico, One Euro / EMA)
    │
    ▼
Segmentador Temporal (MotionSegmenter: máquina de estados, sostén articular y picos en patadas)
    │
    ▼
Evaluador Biomecánico (MovementEvaluator: 21 métricas 3D, bandas 0-100, 27 códigos de feedback)
    │
    ▼
SessionEngine (Flujo reactivo StateFlow) ──► UI Jetpack Compose (M3)
                                         └──► Persistencia Local Room & DataStore (SQLite)
```

---

## 3. Limitaciones Técnicas y Operativas

1. **Condiciones de Iluminación y Espacio:** La precisión del modelo MediaPipe requiere iluminación uniforme frontal y una distancia de entre 2.5 y 3.5 metros. Se debe capturar el cuerpo entero de pies a cabeza sin oclusiones.
2. **Población Unipersonal:** La compuerta de calidad exige que solo una persona esté presente en el cuadro.
3. **Calibración Empírica Futura:** Al no disponer actualmente de grabaciones reales autorizadas con consentimiento informado ni panel ciego de jueces certificados, los umbrales permanecen en estado `PENDIENTE_CALIBRAR` y las calificaciones son puramente orientativas (ver [docs/VALIDATION_REPORT.md](docs/VALIDATION_REPORT.md)).
4. **Dispositivos Soportados:** Dispositivos Android con API 26 (Android 8.0 Oreo) o superior.

---

## 4. Documentación del Proyecto

- 📖 **Manual de Usuario:** [docs/USER_MANUAL.md](docs/USER_MANUAL.md) — Guía rápida de uso de una página.
- 📋 **Lista de Verificación Global:** [docs/CHECKLIST_APPENDIX_D.md](docs/CHECKLIST_APPENDIX_D.md) — Lista de verificación del Apéndice D completada al 100%.
- 📊 **Informe de Validación:** [docs/VALIDATION_REPORT.md](docs/VALIDATION_REPORT.md) — Declaración de datos reales e inventario de 37 parámetros.
- 📑 **Fuentes y Secuencia:** [docs/sources/taegeuk1_sources.md](docs/sources/taegeuk1_sources.md) — Fijación documental (CR-001).
- 📐 **Registro de Decisiones:** [docs/DECISIONS.md](docs/DECISIONS.md) — Decisiones técnicas y de arquitectura (D-01 a D-F13-01).
- 🧪 **Resultados de Pruebas:** [docs/TEST_RESULTS.md](docs/TEST_RESULTS.md) — 174 pruebas unitarias acumuladas en verde.
- 📝 **Bitácora de Implementación:** [docs/IMPLEMENTATION_LOG.md](docs/IMPLEMENTATION_LOG.md) — Bitácora cronológica de todas las fases (F0 a F13).

---

## 5. Requisitos y Compilación

- **JDK:** Java 21 (o Java 17+)
- **Android SDK:** `compileSdk = 37`, `minSdk = 26`, `targetSdk = 37`
- **Gradle:** Gradle 8.13 con Kotlin DSL

### Comandos de Compilación y Verificación

```bash
# 1. Ejecutar la suite completa de verificación (compilación, 174 tests, Detekt y ktlint)
bash scripts/verify.sh

# 2. Compilar paquete Release optimizado con R8 y reducción de código/recursos
./gradlew assembleRelease

# 3. Ejecutar pruebas unitarias en la JVM (:analysis y :app)
./gradlew :analysis:test :app:testDebugUnitTest

# 4. Análisis estático y formateo de código
./gradlew detekt ktlintCheck
```

### Artefacto de Distribución (Release)

- **Ubicación del APK:** `app/build/outputs/apk/release/app-release.apk`
- **Tamaño:** ~73 MB (incluye modelo de MediaPipe Pose Landmarker y bibliotecas nativas de inferencia para todas las arquitecturas ABI).
- **Hash SHA-256:** `b13c3fae2db711f9bd85620ad6f92c06e183787f2c7c15c72e788b6c1f4ae4c4`
- **Firma:** Firmado localmente con configuración de pruebas; la firma con la clave privada de producción del usuario queda registrada como pendiente externo.
