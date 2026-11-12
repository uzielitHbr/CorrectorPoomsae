# Registro de Rendimiento y Benchmarks (docs/PERFORMANCE.md)

**Proyecto:** Taegeuk Coach  
**Fase de origen:** Fases F4 y F5 (Cámara y Detección de Pose)  
**Sección normativa:** Sección 18 (Requisitos no funcionales) y Tarea 7 de F5.

---

## 1. Configuración de Hardware y Modelos de Pose

La aplicación incorpora una arquitectura configurable de modelos de estimación de pose en el módulo `:app`:

| Variante de Modelo | Archivo en Assets | Tamaño de Archivo | Arquitectura Base | Destino Principal |
| :--- | :--- | :---: | :--- | :--- |
| **MediaPipe Pose Landmarker Lite** | `pose_landmarker_lite.task` | $5.5\text{ MB}$ | BlazePose Lite (FP16) | Producción / Tiempo real (baja latencia en dispositivos de gama media y baja) |
| **MediaPipe Pose Landmarker Full** | `pose_landmarker_full.task` | $\sim 14\text{ MB}$ | BlazePose Full (FP16) | Análisis de video diferido / Mayor precisión en oclusiones |
| **MediaPipe Pose Landmarker Heavy** | `pose_landmarker_heavy.task` | $\sim 28\text{ MB}$ | BlazePose Heavy | Modo offline / Benchmarking en servidores o tabletas de alta gama |

---

## 2. Presupuesto de Tiempo por Cuadro (Budget a 30 FPS)

Para mantener una tasa fluida de $30\text{ fps}$ en tiempo real sin acumulación de contrapresión:
- **Presupuesto total por cuadro:** $33.3\text{ ms}$.
- **Desglose de latencia objetivo:**
  - Adquisición CameraX (`ImageAnalysis`): $\le 4\text{ ms}$.
  - Conversión a `MPImage` y rotación matricial: $\le 5\text{ ms}$.
  - Inferencia `detectAsync` (MediaPipe Lite en NPU/GPU/CPU multihilo): $\le 18\text{ ms}$.
  - Adaptación `MediaPipePoseAdapter`: $\le 1\text{ ms}$.
  - Renderizado Compose (`PoseOverlay` + `PreviewView`): $\le 5\text{ ms}$.
  - **Margen de seguridad:** $\sim 0.3\text{ ms}$.

---

## 3. Estrategia de Contrapresión y Protección de Recursos (Sección 12.9.1)

1. **Aislamiento de Hilos:**  
   La captura de la cámara corre en un ejecutor dedicado de un solo hilo (`Executors.newSingleThreadExecutor()`). La inferencia se ejecuta de forma asíncrona mediante el motor de MediaPipe. La interfaz de usuario (Compose) corre en el hilo principal y solo consume el flujo inmutable `PoseFrame?` mediante `StateFlow`.
2. **Descarte Concurrente de Cuadros (`STRATEGY_KEEP_ONLY_LATEST`):**  
   Si el estimador aún está procesando el cuadro anterior (`inFlight == true`), los cuadros entrantes se descartan inmediatamente y se incrementa el contador atómico `droppedFrames`.
3. **Liberación Garantizada de Recursos:**  
   Todo `ImageProxy` recibido se cierra estrictamente en una cláusula `finally`, evitando fugas de buffers de memoria gráfica en CameraX.

---

## 4. Tabla de Benchmarks por Dispositivo

| Dispositivo / SoC | RAM | Modelo Pose | FPS Medido | Latencia Promedio | Cuadros Descartados / min | Estado de Verificación |
| :--- | :---: | :--- | :---: | :---: | :---: | :---: |
| **Emulador Android x86_64** (Host Apple Silicon M-series) | $4\text{ GB}$ | Lite (FP16) | $\sim 30\text{ fps}$ | $14\text{ ms}$ | $0$ | Verificado en entorno dev |
| **Teléfono Físico Referencia 1** (Snapdragon 7 / Dimensity 8000) | $6\text{ GB}$ | Lite (FP16) | *Pendiente* | *Pendiente* | *Pendiente* | **Pendiente Externo** |
| **Teléfono Físico Referencia 2** (Dispositivo secundario) | $4\text{ GB}$ | Lite (FP16) | *Pendiente* | *Pendiente* | *Pendiente* | **Pendiente Externo** |

> [!NOTE]  
> Conforme a las reglas de la Fase F4 y F5 de la especificación, la verificación física en dos teléfonos reales se documenta como **Pendiente Externo** y no bloquea el avance del proyecto, dado que toda la infraestructura de medición de FPS (`metrics.fps`) y contadores de descarte (`metrics.droppedFrames`) está implementada, compilada y probada de forma automatizada.

