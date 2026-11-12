# Protocolo de Fiabilidad de Métricas y Experimento E3 (docs/RELIABILITY.md)

**Proyecto:** Taegeuk Coach  
**Fase de origen:** Fase F5 (Fiabilidad y experimentos métricos)  
**Secciones normativas:** Secciones 13.6 (Ventanas y robustez), 13.9 (Limitaciones de estimación) y 13.10 (Experimento E3).

---

## 1. Fundamento Normativo del Experimento E3

Las coordenadas tridimensionales generadas por MediaPipe Pose Landmarker no constituyen una captura de movimiento biomecánica de grado clínico (*mocap* óptico de marcadores pasivos). Constituyen estimaciones estadísticas monoculares sujetas a ruido sensorial, oclusiones y variaciones lumínicas.

El **Experimento E3** tiene como objetivo medir empíricamente el sesgo (*bias*) y la dispersión (*desviación estándar del error $\sigma_{\text{error}}$*) de cada una de las 21 métricas biomecánicas (grupos ST, AR, KI y DI) bajo diferentes condiciones ambientales y corporales.

---

## 2. Metodología de Medición

### 2.1. Poses Estáticas de Referencia (*Ground Truth*)
Se establecen poses estándar de Taekwondo medidas con instrumental físico:
- **Ángulos articulares:** Medidos con goniómetro manual y plantilla graduada de precisión ($\pm 1^\circ$).
- **Distancias y separaciones:** Medidas con cinta métrica inextensible ($\pm 0.5\text{ cm}$).
- **Posiciones fijas evaluadas:**
  1. *Ap Seogi* izquierdo y derecho (paso corto, ST-01, ST-02, ST-03, ST-04, ST-05, ST-06).
  2. *Ap Kubi* izquierdo y derecho (paso largo, flexión de rodilla $90^\circ$).
  3. *Arae Makki* estático (bloqueo bajo a $20\text{ cm}$ del muslo, codo $150^\circ$).
  4. *Momtong Jireugi* estático (puño al plexo solar, brazo alineado).
  5. *Eolgul Makki* estático (bloqueo alto, un puño de separación de la frente).
  6. *Ap Chagi* en punto culminante (*peak*, rodilla elevada, pie en extensión).

### 2.2. Factores de Variación Evaluados

| Factor | Niveles de Prueba | Justificación Técnica |
| :--- | :--- | :--- |
| **Distancia a cámara** | $1.8\text{ m}$, $2.5\text{ m}$, $3.5\text{ m}$ | Evalúa el efecto del tamaño del practicante en píxeles ($T_{img}$) en la precisión. |
| **Iluminación** | Óptima ($>400\text{ lux}$), Tenue ($\sim 120\text{ lux}$), Contraluz | Verifica la robustez del detector de bordes y landmarks. |
| **Resolución** | $640\times 480$, $1280\times 720$, $1920\times 1080$ | Determina si resoluciones mayores mejoran la fiabilidad o solo consumen cómputo. |
| **Indumentaria** | Dobok tradicional blanco, ropa oscura holgada, ropa ajustada | Oclusión y contraste del cuerpo respecto al fondo. |

---

## 3. Criterio de Habilitación de Métricas para Puntuación

Para que una métrica participe en el puntaje global de una sesión, su desviación típica del error debe satisfacer la condición estricta:

$$\sigma_{\text{error}} < \text{RELIABILITY\_MAX\_FRACTION} \times \text{ancho\_banda\_ideal}$$

- **`RELIABILITY_MAX_FRACTION`:** Fracción máxima acordada con el comité técnico (valor provisional: $0.30$, es decir, el ruido no debe superar el 30% del ancho de la banda permitida).
- **Tratamiento si no cumple:**  
  La métrica pasa al estado `MetricResult.Unavailable(UnavailableReason.DISABLED_LOW_RELIABILITY)`.
  - **Efecto:** Se visualiza en la interfaz como informativa con advertencia de precisión reducida, pero **no suma ni resta puntos al cálculo de la calificación global**.

---

## 4. Estado de los Resultados de E3

| Métrica | Articulaciones | Error Medido ($\sigma$) | Ancho Banda Ideal | Estado de Habilitación |
| :--- | :--- | :---: | :---: | :---: |
| **ST-01** (Largo de paso) | Tobillos, Caderas | *Pendiente E3* | Provis. ($0.25$) | `PENDIENTE_CALIBRAR` |
| **ST-02** (Ancho de paso) | Tobillos, Caderas | *Pendiente E3* | Provis. ($0.20$) | `PENDIENTE_CALIBRAR` |
| **ST-03** (Rodilla delantera) | Cadera, Rodilla, Tobillo | *Pendiente E3* | Provis. ($15^\circ$) | `PENDIENTE_CALIBRAR` |
| **ST-04** (Rodilla trasera) | Cadera, Rodilla, Tobillo | *Pendiente E3* | Provis. ($10^\circ$) | `PENDIENTE_CALIBRAR` |
| **AR-01** (Ángulo de codo) | Hombro, Codo, Muñeca | *Pendiente E3* | Provis. ($15^\circ$) | `PENDIENTE_CALIBRAR` |
| **AR-02** (Altura de muñeca) | Muñeca, Caderas | *Pendiente E3* | Provis. ($0.15$) | `PENDIENTE_CALIBRAR` |
| **KI-01** (Altura de patada) | Cadera, Rodilla | *Pendiente E3* | Provis. ($0.25$) | `PENDIENTE_CALIBRAR` |
| **DI-01** (Error de giro) | Caderas, Orientación | *Pendiente E3* | Provis. ($15^\circ$) | `PENDIENTE_CALIBRAR` |

> [!NOTE]  
> Las mediciones numéricas sobre sujetos humanos en vivo quedan registradas como **Pendiente Externo** por ausencia de dispositivo físico y atletas en el entorno de ejecución autónomo. La infraestructura de captura, filtros de robustez, ventanas de mediana, MAD y el tipo cerrado `MetricResult.Unavailable(DISABLED_LOW_RELIABILITY)` están completamente implementados y probados en código.

