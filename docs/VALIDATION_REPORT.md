# Informe de Calibración y Validación con Datos Reales (docs/VALIDATION_REPORT.md)

**Proyecto:** Taegeuk Coach  
**Fase:** F12 — Calibración y Validación con Datos Reales  
**Versión de especificación:** 3.0  
**Fecha de emisión:** 12 de noviembre de 2026  
**Estado normativo del sistema:** `NO_VALIDADO` (Modo Experimental)  
**Estado de parámetros de configuración:** 100% `PENDIENTE_CALIBRAR`  
**Identificador de configuración:** `0.2.0-f2`  

---

## 1. Resumen Ejecutivo y Estado Normativo (Sección 16.5)

El proyecto **Taegeuk Coach** implementa una arquitectura rigurosa para la evaluación cinemática y biomecánica de la forma *Taegeuk 1 Jang* (*Taegeuk Il Jang*). En cumplimiento de la **Regla R-05**, la **Regla R-21** y la **Sección 16.5** de las directrices técnicas del proyecto:

1. **Estado de validación del sistema:** La aplicación opera bajo el estado formal:
   $$\text{AnalysisConfig.VALIDATION\_STATE} = \text{ValidationState.NO\_VALIDADO}$$
2. **Modo operativo:** La aplicación se encuentra configurada y bloqueada en **Modo Experimental**. Todos los puntajes globales ($0\text{ a }100$), clasificaciones de técnica (*Correcto*, *Mejorable*, *Incorrecto*, *No evaluado*) y observaciones biomecánicas tienen carácter **estrictamente orientativo y no oficial**.
3. **Prohibición de certificación:** Se prohíbe terminantemente a la interfaz de usuario, al motor de exportación (texto y PDF) y a los metadatos persistidos en base de datos exhibir la palabra o insignia «Validado» mientras no se satisfagan formalmente los criterios muestrales del Nivel 2 (`VALIDADO_ALCANCE_LIMITADO`).

---

## 2. Declaración Formal de No Disponibilidad de Datos Reales

### 2.1. Restricción Estricta: Prohibición de Datos Simulados o Inventados
En cumplimiento de las instrucciones de la Fase F12 y del principio de integridad científica del proyecto:

> **RESTRICCIÓN NORMATIVA:** Al no disponer actualmente de grabaciones reales autorizadas con consentimiento informado ni de una evaluación independiente etiquetada por un profesor certificado por Kukkiwon o por un árbitro oficial de poomsae, **NO SE HAN SIMULADO NI INVENTADO DATOS**.

Los modelos de prueba sintéticos desarrollados en fases anteriores (`SkeletonBuilder`, inyecciones geométricas y trayectorias sinusoidales ruidosas) se emplean exclusivamente como herramientas de verificación unitaria de invariantes cinemáticos (ortogonalidad, no linealidad, invarianza ante rotación/traslación y robustez ante divisiones por cero o descarte temporal). **No sustituyen ni pretenden sustituir un dataset empírico de campo**.

### 2.2. Justificación Técnica y de Privacidad
1. **Consentimiento y Protección de Datos Biométricos:** La captura y almacenamiento de datos corporales (video y coordenadas de landmarks de MediaPipe) de practicantes de taekwondo requiere un protocolo de bioética y consentimiento informado formal conforme a normativas de protección de datos personales (LFPDPPP y estándares internacionales equivalentes). Dicho protocolo no se ha instrumentado aún en esta fase de desarrollo de software autónomo.
2. **Etiquetado por Panel Calificado:** La asignación de bandas de tolerancia y veredictos técnicos (*deducciones menores de -0.1 y mayores de -0.3* según el reglamento de poomsae de World Taekwondo) requiere un panel de jueceo ciego compuesto por instructores con grado mínimo de 4.° Dan Kukkiwon o árbitros de poomsae activos. No se dispone de dicho etiquetado en el presente ciclo.

---

## 3. Inventario de Parámetros en `AnalysisConfig` y Estado de Calibración

Todos los parámetros numéricos de análisis biomecánico, filtrado y segmentación temporal residen de forma centralizada en `AnalysisConfig.kt` (módulo `:analysis`). Ningún valor empírico se encuentra disperso como número mágico en el código fuente.

| Categoría | Parámetro | Valor de Desarrollo | Unidad | Estado de Calibración | Método Previsto de Calibración Futura |
| :--- | :--- | :---: | :---: | :---: | :--- |
| **Invariantes Matemáticos** | `EPS_NORM` | $10^{-6}$ | m | `CALIBRADO` (Analítico) | Invariante geométrico analítico (cero vectorial). |
| | `EPS_ANGLE_DEG` | $10^{-4}$ | grados | `CALIBRADO` (Analítico) | Invariante geométrico analítico (frontera angular). |
| | `EPS_SCALAR` | $10^{-6}$ | escalar | `CALIBRADO` (Analítico) | Tolerancia de aritmética flotante IEEE 754. |
| | `EPS_RATIO` | $10^{-4}$ | adim. | `CALIBRADO` (Analítico) | Tolerancia de verificación de razones antropométricas. |
| | `EPS_DOT` | $10^{-6}$ | adim. | `CALIBRADO` (Analítico) | Detección de ortogonalidad no ambigua en rumbos. |
| **Calidad de Señal** | `VIS_MIN` | $0.50$ | score $[0, 1]$ | `PENDIENTE_CALIBRAR` | Curvas ROC de visibilidad vs. error articular en video real. |
| | `MIN_WINDOW_FRAMES`| $5$ | cuadros | `PENDIENTE_CALIBRAR` | Optimización de ventana de mediana según tasa de cuadros reales. |
| **Filtro One Euro** | `ONE_EURO_MIN_CUTOFF_HZ`| $1.0$ | Hz | `PENDIENTE_CALIBRAR` | Medición de temblor en reposo (*junbi seogi*) en muestra humana. |
| | `ONE_EURO_BETA` | $1.5$ | $\text{m}^{-1}$ | `PENDIENTE_CALIBRAR` | Minimización de retardo en patadas y golpes rápidos. |
| | `ONE_EURO_D_CUTOFF_HZ`| $1.0$ | Hz | `PENDIENTE_CALIBRAR` | Filtro derivativo para estimación de velocidad articular. |
| **Calibración Antropométrica**| `CAL_MIN_FRAMES` | $10$ | cuadros | `PENDIENTE_CALIBRAR` | Estabilidad de mediana frente a micro-movimientos del practicante. |
| | `CAL_MAX_FRAMES` | $90$ | cuadros | `PENDIENTE_CALIBRAR` | Tiempo máximo de espera en posición de atención previa. |
| | `CAL_MAX_REL_MAD` | $0.08$ | adim. | `PENDIENTE_CALIBRAR` | Dispersión antropométrica tolerable en muestra de $N \ge 30$ sujetos. |
| | `CAL_TORSO_HEAD_MIN_RATIO`| $1.10$ | adim. | `PENDIENTE_CALIBRAR` | Distribución percentilar de proporciones corporales humanas. |
| | `CAL_TORSO_HEAD_MAX_RATIO`| $2.20$ | adim. | `PENDIENTE_CALIBRAR` | Distribución percentilar de proporciones corporales humanas. |
| | `CAL_LEG_TORSO_MIN_RATIO` | $1.20$ | adim. | `PENDIENTE_CALIBRAR` | Distribución percentilar de extremidades inferiores. |
| | `CAL_LEG_TORSO_MAX_RATIO` | $2.40$ | adim. | `PENDIENTE_CALIBRAR` | Distribución percentilar de extremidades inferiores. |
| **Compuerta de Encuadre** | `FRAME_HEAD_MARGIN_MIN` | $0.04$ | norm. | `PENDIENTE_CALIBRAR` | Validación de margen superior en lentes gran angular móvil. |
| | `FRAME_FEET_MARGIN_MIN` | $0.04$ | norm. | `PENDIENTE_CALIBRAR` | Validación de margen inferior para captura de pies completos. |
| | `FRAME_LATERAL_MARGIN_MIN`| $0.05$ | norm. | `PENDIENTE_CALIBRAR` | Margen lateral para evitar recorte en giros de 90° y 180°. |
| | `FRAME_BOX_MIN_COVERAGE` | $0.45$ | norm. | `PENDIENTE_CALIBRAR` | Distancia óptima al teléfono ($2.0\text{ a }3.0\text{ m}$). |
| | `FRAME_BOX_MAX_COVERAGE` | $0.92$ | norm. | `PENDIENTE_CALIBRAR` | Límite de proximidad para evitar saturación de encuadre. |
| **Segmentación Cinemática** | `V_START` | $0.80$ | torsos/s | `PENDIENTE_CALIBRAR` | Umbral de velocidad articular de arranque de paso/bloqueo. |
| | `T_START_MS` | $100$ | ms | `PENDIENTE_CALIBRAR` | Duración mínima por encima de `V_START` para confirmar inicio. |
| | `T_LOCKOUT_MS` | $400$ | ms | `PENDIENTE_CALIBRAR` | Ventana de bloqueo para evitar falsos arranques en desaceleración. |
| | `V_STILL` | $0.25$ | torsos/s | `PENDIENTE_CALIBRAR` | Velocidad máxima para considerar una articulación inmóvil. |
| | `T_HOLD_MS` | $600$ | ms | `PENDIENTE_CALIBRAR` | Tiempo de sostén técnico de postura (mínimo normativo). |
| | `T_EVAL_WINDOW_MS` | $300$ | ms | `PENDIENTE_CALIBRAR` | Ventana temporal final del sostén para captura de métricas. |
| | `T_PEAK_WINDOW_MS` | $150$ | ms | `PENDIENTE_CALIBRAR` | Ventana de captura en punto álgido de patada (*ap chagi*). |
| | `T_READY_MS` | $1000$ | ms | `PENDIENTE_CALIBRAR` | Tiempo en postura de listos (*junbi*) antes de iniciar conteo. |
| | `T_MOVEMENT_TIMEOUT_MS` | $8000$ | ms | `PENDIENTE_CALIBRAR` | Límite de inactividad antes de sugerir omisión (SC-13). |
| | `T_LOSS_SHORT_MS` | $1000$ | ms | `PENDIENTE_CALIBRAR` | Distinción entre oclusión momentánea y pérdida de encuadre. |
| | `T_RECOVER_MS` | $1500$ | ms | `PENDIENTE_CALIBRAR` | Estabilidad requerida para reanudar sesión tras recuperación. |
| | `T_FEEDBACK_MS` | $2500$ | ms | `PENDIENTE_CALIBRAR` | Duración en pantalla de banner de retroalimentación (SC-10). |
| **Límites de Video Offline**| `VIDEO_MAX_DURATION_SECONDS`| $180$ | s | `PENDIENTE_CALIBRAR` | Duración máxima de archivo de video (SC-28). |
| | `VIDEO_MAX_FILE_SIZE_BYTES` | $200\text{ MB}$ | bytes | `PENDIENTE_CALIBRAR` | Límite de carga de archivo para evitar saturación de memoria. |
| | `VIDEO_MAX_WIDTH` | $1920$ | px | `PENDIENTE_CALIBRAR` | Resolución máxima de decodificación. |
| | `VIDEO_MAX_HEIGHT`| $1920$ | px | `PENDIENTE_CALIBRAR` | Resolución máxima de decodificación. |
| | `VIDEO_DOWNSCALE_TARGET_MAX_DIM`| $640$ | px | `PENDIENTE_CALIBRAR` | Reducción previa para contención estricta de memoria $O(1)$. |
| | `VIDEO_FRAME_INTERVAL_MS`| $66$ | ms | `PENDIENTE_CALIBRAR` | Muestreo a ~15 fps determinista y sin descarte de cuadros. |
| **Exigencia de Calificación**| `EXIGENCY_FACTOR_RELAXED`| $1.25$ | factor $g$ | `PENDIENTE_CALIBRAR` | Apertura de bandas de puntuación para practicantes principiantes. |
| | `EXIGENCY_FACTOR_NORMAL` | $1.00$ | factor $g$ | `PENDIENTE_CALIBRAR` | Ancho estándar de bandas de calificación. |
| | `EXIGENCY_FACTOR_STRICT` | $0.80$ | factor $g$ | `PENDIENTE_CALIBRAR` | Estrechamiento de bandas para practicantes avanzados. |

---

## 4. Marco de Niveles de Validación Normativa (Sección 16.5)

El sistema implementa tres niveles progresivos de validación formal:

```
[ Nivel 0: NO_VALIDADO ]  ◄── (ESTADO ACTUAL ACTIVO)
       │
       │  (Adquisición de dataset piloto N ≥ 10 sujetos, calibración inicial de umbrales)
       ▼
[ Nivel 1: VALIDACION_PRELIMINAR ]
       │
       │  (Dataset representativo N ≥ 30, jueceo ciego por panel doble, Kappa ≥ 0.70)
       ▼
[ Nivel 2: VALIDADO_ALCANCE_LIMITADO ]
```

### Nivel 0: `NO_VALIDADO` (Estado Actual)
- **Criterios muestrales:** 0 sujetos reales etiquetados; valores provisionales basados en literatura técnica y heurísticas iniciales.
- **Comportamiento en UI:**
  - Banner permanente: `MSG_EXPERIMENTAL` («Modo experimental: los resultados son orientativos»).
  - Aviso general: `MSG_DISCLAIMER` («La puntuación es orientativa y no es una calificación oficial»).
  - En Inicio: `status_documentally_fixed` («Secuencia fijada documentalmente; no certificada oficialmente»).
- **Restricción:** La UI prohíbe terminantemente mostrar cualquier texto o etiqueta que afirme que el sistema está validado.

### Nivel 1: `VALIDACION_PRELIMINAR`
- **Requisitos de muestra para activación:**
  - Mínimo 10 practicantes con distribución de estaturas representativa ($1.50\text{ a }1.90\text{ m}$).
  - Mínimo 3 repeticiones grabadas por practicante ($N = 30$ ejecuciones completas de Taegeuk 1 Jang).
  - Revisión y ajuste manual de los umbrales de velocidad (`V_START`, `V_STILL`) y tiempos de sostén (`T_HOLD_MS`).
- **Comportamiento en UI:** Se retira la leyenda de modo experimental estricto, pero se mantiene la advertencia de alcance preliminar.

### Nivel 2: `VALIDADO_ALCANCE_LIMITADO`
- **Requisitos de muestra para activación:**
  - Mínimo 30 practicantes con estratificación por grados (cintas blancas, amarillas, verdes, azules, rojas y negras).
  - Grabaciones con diversidad de iluminación (interior iluminado, interior con luz natural difusa, luz artificial cálida y fría).
  - Evaluación independiente a doble ciego por al menos 2 profesores cinta negra $\ge 4.^\circ\text{ Dan}$ o árbitros de poomsae.
  - Concordancia inter-evaluador medida mediante el coeficiente Kappa ponderado de Cohen / Fleiss con $\kappa \ge 0.70$.
- **Comportamiento en UI:** Se permite exhibir la insignia «Validación con alcance limitado (Taegeuk 1 Jang)», acompañada de su ficha metodológica en Ajustes.

---

## 5. Garantías Operativas del Modo Experimental en la Aplicación

La aplicación implementa controles de software para garantizar que el usuario nunca sea inducido a creer que los resultados son una calificación arbitral oficial:

1. **Pantalla de Inicio (`HomeScreen`):**
   - Exhibe visiblemente el banner superior: *«Secuencia fijada documentalmente; no certificada oficialmente»*.
   - Muestra al pie de página el aviso reglamentario: *«La puntuación es orientativa y no es una calificación oficial»*.
2. **Pantalla de Práctica (`PracticeScreen`):**
   - El HUD superior muestra el avance de movimientos sin emitir veredictos de pase o reprobación oficial.
   - Las observaciones correctivas se muestran en banners de advertencia visual no punitiva.
3. **Pantalla de Resumen (`SummaryScreen`):**
   - El componente `ScoreRing` incluye bajo el puntaje numérico la palabra obligatoria **«orientativo»**.
   - Se despliega el banner de estado: *«Modo experimental: los resultados son orientativos»* (`MSG_EXPERIMENTAL`).
   - El pie de página reitera: *«La puntuación es orientativa y no es una calificación oficial»* (`MSG_DISCLAIMER`).
4. **Pantalla de Historial (`HistoryScreen`):**
   - Cada entrada de práctica en el listado muestra el puntaje con la anotación explícita `(orient.)`.
   - Las sesiones de video grabado se distinguen con el badge visual «Video».
5. **Servicio de Exportación (`SessionExporter`):**
   - El reporte en texto plano incluye una sección prominente titulada `AVISOS NORMATIVOS OBLIGATORIOS` con los disclaimers reglamentarios y la indicación del estado `NO_VALIDADO`.
   - El documento PDF generado dibuja en el encabezado la leyenda:
     *«AVISO: La puntuación es orientativa y no oficial. Secuencia no validada oficialmente.»*

---

## 6. Protocolo y Plan de Trabajo para la Calibración Futura con Datos Reales

Para cuando se disponga de la autorización y el acceso al panel de practicantes e instructores, el plan de calibración seguirá estrictamente el siguiente protocolo técnico:

### 6.1. Protocolo de Captura y Grabación de Datos
1. **Dispositivos y Colocación:** Dispositivos Android representativos de gama media y alta montados en trípode a $1.0\text{ m}$ de altura y a una distancia fija de $2.5\text{ m}$ del área central de práctica.
2. **Sujetos:** Mínimo 30 participantes voluntarios con consentimiento informado debidamente firmado.
3. **Pauta de Ejecución:** Cada practicante ejecutará la forma Taegeuk 1 Jang completa en dos repeticiones: una a velocidad normal de práctica y otra con errores deliberados comunes (ej. paso corto, flexión insuficiente de rodilla delantera, bloqueo bajo a destiempo).

### 6.2. Protocolo de Jueceo Ciego y Etiquetado
1. **Panel de Jueces:** Dos profesores certificados evaluarán las grabaciones de video de forma independiente sin conocer las métricas calculadas por la aplicación.
2. **Rúbrica de Evaluación:** Se registrarán deducciones por cada movimiento según el reglamento oficial de poomsae (WT):
   - Deducción menor ($-0.1$): imprecisiones de ángulo $\pm 10^\circ$, separación de pies deficiente.
   - Deducción mayor ($-0.3$): postura incorrecta, técnica equivocada, falta de equilibrio visible.
3. **Consenso:** Discrepancias mayores a $0.3$ puntos se resolverán por revisión conjunta con un tercer juez árbitro.

### 6.3. Análisis Estadístico y Optimización de Umbrales
1. **Curvas ROC:** Para cada una de las 21 métricas biomecánicas (ej. `FRONT_KNEE_ANGLE`, `STANCE_LENGTH_FACTOR`, `WRIST_HEIGHT`), se trazarán curvas ROC comparando el valor métrico continuo frente a las deducciones de los jueces.
2. **Punto de Operación Óptimo:** Se determinará el umbral óptimo mediante el índice de Youden ($J = \text{Sensibilidad} + \text{Especificidad} - 1$), balanceando la detección de defectos reales sin frustrar al practicante con falsos positivos.
3. **Ajuste de Bandas:** Los anchos de banda ($b_1, b_2, b_3, b_4$) de la función de puntuación (Sección 13.8) se ajustarán para que el error cuadrático medio (RMSE) entre la puntuación del algoritmo y la del panel de jueces sea inferior a $5.0$ puntos en una escala de 100.
4. **Transición Formal de Estado:** Una vez alcanzados los criterios de convergencia, los parámetros calibrados pasarán de `PENDIENTE_CALIBRAR` a `CALIBRADO` en `AnalysisConfig.kt` y se emitirá una versión actualizada de este informe.

---

## 7. Conclusión de la Fase F12

El sistema **Taegeuk Coach** se encuentra técnica y normativamente protegido contra afirmaciones falsas de validación. La aplicación opera con total honestidad de producto en **Modo Experimental**, garantizando una base arquitectónica lista para recibir calibraciones empíricas futuras sin haber inventado ni fabricado datos.

