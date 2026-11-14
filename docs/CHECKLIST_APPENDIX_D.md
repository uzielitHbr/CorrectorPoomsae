# Lista de Verificación Global de Cumplimiento (Apéndice D)

**Proyecto:** Taegeuk Coach  
**Versión de entrega:** v1.0.0 (Release)  
**Fecha de auditoría:** 13 de noviembre de 2026  
**Resultado general:** **100% CUMPLIDA (Todas las casillas verificadas en verde)**  

---

## 1. Fijación Documental y Secuencia Canónica (Fases F0 y F3)

- [x] **D-01 / CR-001:** Adopción formal del procedimiento de fijación documental en `docs/DECISIONS.md`.
- [x] **D-02:** Documento de contraste de fuentes secundarias publicado en `docs/sources/taegeuk1_sources.md`.
- [x] **D-03:** Resolución formal y justificada de discrepancias (giro a la derecha de 90° en movimiento 17 para asegurar cierre angular a 0°; kihap único en movimiento 18).
- [x] **D-04:** Archivo canónico `poomsae_taegeuk1.json` con esquema v2 conteniendo los 18 movimientos consecutivos.
- [x] **D-05:** Cierre angular perfecto a 0° de rumbo acumulado tras el movimiento 18.
- [x] **D-06:** Aviso normativo obligatorio en README y UI: *«Secuencia fijada documentalmente; no certificada oficialmente»* (Regla R-14).
- [x] **D-07:** Cargador `PoomsaeLoader` en `:analysis` probado exhaustivamente ante esquemas corruptos o inválidos.

---

## 2. Base Matemática y Geométrica (`:analysis`, Fase F2)

- [x] **G-01:** Entidad `Vec3` inmutable con suma, resta, producto punto, producto cruz, norma y normalización.
- [x] **G-02:** Entidades `Vec2`, `Joint`, `Landmark` y `PoseFrame` con modelado riguroso de timestamps y visibilidad.
- [x] **G-03:** Manejo explícito de vectores de norma cero (`ZeroVectorException` o `MetricResult.Unavailable`), eliminando `NaN` silenciosos.
- [x] **G-04:** Cálculo de ángulos 3D y 2D entre 3 articulaciones, ángulo con la vertical y proyección sobre planos.
- [x] **G-05:** Cálculo de ángulo con signo para giros e invariancia ante rotación, traslación y lateralidad (*handedness*).
- [x] **G-06:** Filtro de suavizado One Euro y media móvil exponencial (EMA) implementados y probados.
- [x] **G-07:** Tolerancias matemáticas analíticas fijadas en `AnalysisConfig` (`EPS_NORM`, `EPS_ANGLE_DEG`, `EPS_SCALAR`, `EPS_RATIO`, `EPS_DOT`).
- [x] **G-08:** Cobertura de líneas $\ge 90\%$ en paquetes `geometry` y `signal`.

---

## 3. Calidad de Señal y Calibración del Usuario (Fase F6)

- [x] **Q-01:** `FrameQualityGate` implementado para rechazo de encuadres incompletos, bordes cortados o distancia inapropiada.
- [x] **Q-02:** Emisión de mensajes de orientación específicos en español de México (`MSG_FRAME_TOO_CLOSE`, `MSG_FRAME_TOO_FAR`, `MSG_FRAME_CUT`, `MSG_ONE_PERSON`).
- [x] **Q-03:** Calibrador `BodyProfileCalibrator` que calcula la mediana de cuadros estables y verifica estabilidad con MAD relativo.
- [x] **Q-04:** Captura del rumbo de referencia ($F_0$ y $r_0$) en la posición inicial lista (*Chunbi*).
- [x] **Q-05:** Cobertura de líneas $\ge 90\%$ en paquete `quality`.

---

## 4. Evaluador Biomecánico y Puntuación (Fase F7)

- [x] **E-01:** Implementación de las 21 métricas técnicas de la sección 13.4 para posturas y técnicas de Taegeuk 1 Jang.
- [x] **E-02:** Puntuación normalizada por bandas ideales, permisibles y fuera de rango ($0\text{ a }100$) según la sección 13.8.
- [x] **E-03:** Enumeración cerrada `FeedbackCode` con sus parámetros específicos en español de México.
- [x] **E-04:** Modelo estricto de evaluabilidad (Sección 13.7) con los 4 conceptos: *Evaluable*, *Parcialmente evaluable*, *No evaluable* e *Incorrecto*.
- [x] **E-05:** Prohibición estricta de números mágicos: todos los umbrales centralizados en `AnalysisConfig`.
- [x] **E-06:** Pruebas sintéticas con `SkeletonBuilder` para monotonía, simetría y casos límite.
- [x] **E-07:** Cobertura de líneas $\ge 90\%$ en paquete `evaluation`.

---

## 5. Segmentación Temporal y Máquina de Estados (Fase F8)

- [x] **S-01:** Máquina de estados `MotionSegmenter` con los estados de la sección 12.4 (`Idle`, `Calibrating`, `ReadyPose`, `AwaitingMovement`, `InMovement`, `Holding`, `Evaluated`, `Finished`, `Aborted`).
- [x] **S-02:** Detección de sostén (*Holding*) mediante velocidad articular adimensional en torsos/segundo por debajo de umbral constante en ventana temporal.
- [x] **S-03:** Detección de picos de elevación para patadas (*Ap Chagi*, `PEAK_THEN_HOLD`).
- [x] **S-04:** Confirmación de rumbos acumulados para transiciones de giros esperados.
- [x] **S-05:** Mecanismos de recuperación (SC-13): pausar, repetir, omitir y reanudación por pérdida de tracking corporal.
- [x] **S-06:** Cobertura de escenarios de «no confusión» entre movimientos consecutivos sin desplazamiento de pies (5-6 y 11-12).
- [x] **S-07:** Pruebas sintéticas con secuencias ruidosas que completan los 18 movimientos de principio a fin sin interbloqueos.

---

## 6. Cámara, Permisos y Detección de Pose (Fases F4 y F5)

- [x] **C-01:** Flujo de permisos de cámara en Jetpack Compose (explicación previa `MSG_PERM_RATIONALE`, solicitud, denegación permanente con enlace a ajustes `MSG_PERM_SETTINGS`).
- [x] **C-02:** Integración de CameraX (`Preview` e `ImageAnalysis`) con vinculación al ciclo de vida.
- [x] **C-03:** Modelo ligero de MediaPipe (`pose_landmarker_lite.task`) colocado en assets con registro de licencia Apache 2.0.
- [x] **C-04:** Adaptador desacoplado `MediaPipePoseAdapter` con timestamps estrictamente crecientes y descarte atómico de cuadros sin bloquear el hilo principal (Sección 12.9.1).
- [x] **C-05:** Superposición de esqueleto `PoseOverlay` dibujada sobre la vista previa en Compose.
- [x] **C-06:** Registro formal de los experimentos E1 (convención de ejes), E2 (inversión de lados en giros) y E3 (fiabilidad) en `docs/`.

---

## 7. Interfaz de Usuario y Experiencia en Compose (Fase F9)

- [x] **U-01:** Pantalla de Inicio (`HomeScreen`) con acceso a Práctica, Video, Historial y Ajustes.
- [x] **U-02:** Pantalla de Práctica (`PracticeScreen`) con orientación vertical bloqueada, HUD con movimiento actual/siguiente, flechas de giro, timeline y botones de control.
- [x] **U-03:** Pantalla de Resumen (`SummaryScreen`) con ScoreRing global, conteo de evaluabilidad, desglose de tarjetas y avisos normativos.
- [x] **U-04:** Pantalla de Detalle de Movimiento (`MovementDetailScreen`) con desglose de métricas, comparativa frente a rangos ideales y correcciones.
- [x] **U-05:** Aplicación de tokens de color Material 3, tipografías y componentes reutilizables (Sección 14).
- [x] **U-06:** Regla R-10: Todo el texto proviene de `strings.xml` utilizando el catálogo de mensajes de la sección 12.8.
- [x] **U-07:** Exhibición visible y permanente de los avisos obligatorios `MSG_DISCLAIMER` y `MSG_UNVALIDATED`.
- [x] **U-08:** Vistas previas de Compose (`@Preview`) implementadas para estados principales y componentes.
- [x] **U-09:** Pruebas unitarias de ViewModels con Turbine (`HomeViewModelTest`, `PracticeViewModelTest`, `SummaryViewModelTest`, `MovementDetailViewModelTest`).

---

## 8. Persistencia, Historial y Exportación (Fase F10)

- [x] **P-01:** Base de datos Room con entidades `SessionEntity`, `MovementResultEntity`, `SessionDraftEntity` y relación `SessionWithMovements` (Sección 12.10).
- [x] **P-02:** DataStore Preferences para guardado de ajustes de usuario (`defaultCameraFacing`, `voiceEnabled`, `exigencyLevel`, `strictMode`, `saveSnapshots`).
- [x] **P-03:** Pantalla de Historial (`HistoryScreen`) con listado cronológico reactivo, borrado individual y exportación.
- [x] **P-04:** Servicio de borrado total de datos `DataErasureService` (SC-30) que elimina de forma irreversible bases de datos, borradores, preferencias y capturas.
- [x] **P-05:** Servicio de exportación `SessionExporter` que genera resúmenes en texto plano (.txt) y documentos PDF (.pdf) mediante `FileProvider`, incluyendo avisos normativos obligatorios.
- [x] **P-06:** Prohibición de borrado destructivo en Room: migraciones explícitas (`MIGRATION_1_2`) y respaldo de emergencia ante fallos (Sección 8.1).
- [x] **P-07:** Esquemas Room exportados en `app/schemas/`.
- [x] **P-08:** Pruebas unitarias de Room en memoria, DataStore, migraciones y borrado total.

---

## 9. Análisis de Video Grabado Fuera de Línea (Fase F11)

- [x] **V-01:** Selector oficial de video del sistema mediante `PickVisualMedia` (`VideoOnly`), respetando la privacidad sin requerir permisos de almacenamiento en tiempo de ejecución.
- [x] **V-02:** Decodificación por cuadros muestreados mediante `MediaMetadataRetriever.getScaledFrameAtTime` con contención estricta de memoria ($O(1)$) y reducción de escala a 640 px máx.
- [x] **V-03:** MediaPipe Pose Landmarker configurado en `RunningMode.VIDEO` síncrono con marcas de tiempo intrínsecas del archivo.
- [x] **V-04:** Corrección de orientación leyendo `METADATA_KEY_VIDEO_ROTATION` con reciclaje inmediato de mapas de bits.
- [x] **V-05:** Tubería de análisis completa reutilizada, persistiendo la sesión con `mode = "VIDEO"` y distintivo en Historial.
- [x] **V-06:** Rechazo anticipado (SC-28) mediante `VideoValidator` para videos que excedan límites de duración (180 s), tamaño (200 MB) o resolución (1080p).
- [x] **V-07:** Diálogo modal de procesamiento con barra de progreso y botón de cancelación inmediata (SC-29) que libera recursos sin persistir datos.
- [x] **V-08:** Pruebas automatizadas en `:analysis` y `:app` cubriendo validador, escalador, rotador, orquestador y ViewModel.

---

## 10. Calibración y Validación con Datos Reales (Fase F12)

- [x] **R-01:** Restricción estricta de datos: no se simularon ni inventaron datos de grabaciones reales ni evaluaciones docentes.
- [x] **R-02:** Parámetros de `AnalysisConfig` conservados al 100% en estado `ParameterCalibrationStatus.PENDIENTE_CALIBRAR`.
- [x] **R-03:** Aplicación operando formalmente en Modo Experimental (`ValidationState.NO_VALIDADO`).
- [x] **R-04:** Componente `ValidationStateEvaluator` que restringe terminantemente insignias o textos afirmativos «Validado» (Sección 16.5).
- [x] **R-05:** Informe formal exhaustivo generado en `docs/VALIDATION_REPORT.md` con inventario de 37 parámetros, protocolo de bioética y hoja de ruta metodológica.
- [x] **R-06:** Pruebas unitarias que validan el comportamiento del sistema en modo experimental.

---

## 11. Compilación Release, Calidad Final y Empaquetado (Fase F13)

- [x] **F-01:** Tipo de compilación `release` configurado con reducción de código (`isMinifyEnabled = true`) y reducción de recursos (`isShrinkResources = true`).
- [x] **F-02:** Reglas de conservación de ProGuard/R8 exhaustivas para MediaPipe, JNI nativo, kotlinx.serialization, Room, Analysis models, Hilt y DataStore en `app/proguard-rules.pro`.
- [x] **F-03:** Auditoría de accesibilidad completada: `contentDescription` en todos los controles interactivos e iconos (`IconButton`, flechas, botones de retroceso, descarte y borrado) y soporte para escalado de texto al 200% (`verticalScroll` en todos los contenedores y `ConfirmDialog` adaptativo).
- [x] **F-04:** Auditoría de permisos completada: solo `android.permission.CAMERA` solicitado, **cero permisos de red** (ausencia total de `android.permission.INTERNET`).
- [x] **F-05:** Aviso de privacidad integral accesible desde Ajustes con diálogo modal informativo.
- [x] **F-06:** Avisos de licencias de terceros de código abierto (Apache 2.0) accesibles desde Ajustes con diálogo modal informativo.
- [x] **F-07:** Manual de usuario conciso y estructurado redactado en `docs/USER_MANUAL.md`.
- [x] **F-08:** `README.md` actualizado con instrucciones completas de instalación, arquitectura, limitaciones y compilación.
- [x] **F-09:** Compilación `assembleRelease` ejecutada exitosamente, generando el APK optimizado `app-release.apk` con hash SHA-256 documentado (`b13c3fae2db711f9bd85620ad6f92c06e183787f2c7c15c72e788b6c1f4ae4c4`).
- [x] **F-10:** Firma de producción con clave privada del usuario documentada formalmente como Pendiente Externo.
- [x] **F-11:** Suite completa de pruebas unitarias (174/174) y suite de verificación local `bash scripts/verify.sh` en verde sin regresiones.
- [x] **F-12:** Etiqueta Git `v1.0.0` creada en la rama `main`.

