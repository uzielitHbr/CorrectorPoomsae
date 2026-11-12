# Convención de Ejes y Experimentos de Coordenadas (docs/COORDINATES.md)

**Proyecto:** Taegeuk Coach  
**Fase de origen:** Fase F5 (Detección de pose y coordenadas)  
**Secciones normativas:** Sección 5.4 (Espacios de coordenadas), 13.1 (Notación), 13.2 (Primitivas geométricas) y Experimentos E1 y E2.

---

## 1. Convención de Espacios de Coordenadas en MediaPipe

MediaPipe Pose Landmarker genera simultáneamente dos representaciones espaciales para cada cuadro estimado:

### 1.1. Coordenadas de Imagen (`CoordinateSpace.IMAGE`)
- **Naturaleza:** Coordenadas normalizadas relativas al encuadre 2D de la cámara.
- **Rango:**
  - $x \in [0.0, 1.0]$: de izquierda a derecha del sensor de cámara.
  - $y \in [0.0, 1.0]$: de arriba hacia abajo del sensor (origen $(0,0)$ en la esquina superior izquierda).
  - $z$: profundidad relativa respecto al centro de caderas, escalada de manera aproximada a la escala de $x$.
- **Uso en la aplicación:**
  - Proyección del esqueleto sobre la vista previa en pantalla (`PoseOverlay`).
  - Validación de encuadre en `FrameQualityGate` (detección de cuerpo recortado o márgenes insuficientes).
  - En la cámara frontal, la vista previa se espeja horizontalmente en el lienzo (`canvasX = (1.0 - rawX) * width`) para proporcionar retroalimentación natural tipo espejo al practicante. **Las coordenadas internas de análisis nunca se espejan (Regla R-23).**

### 1.2. Coordenadas de Mundo (`CoordinateSpace.WORLD`)
- **Naturaleza:** Coordenadas métricas tridimensionales continuas en metros.
- **Origen:** Punto medio de las caderas del practicante $\text{mid}(P_{L\_H}, P_{R\_H}) = (0, 0, 0)$.
- **Orientación observada en MediaPipe:**
  - Eje $X$: horizontal lateral.
  - Eje $Y$: eje longitudinal del cuerpo (negativo hacia arriba / positivo hacia abajo en espacio estándar de visión por computadora).
  - Eje $Z$: eje de profundidad respecto a la cámara.
- **Principio de no-asunción (Sección 13.2):**  
  La aplicación **no asume** que el eje $Y$ apunta siempre perfectamente hacia el cenit ni que el eje $Z$ define la mirada frontal. En su lugar, el sistema estima dinámicamente la base ortonormal del cuerpo mediante las primitivas probadas de la sección 13.2.

---

## 2. Experimento E1: Validación de Base de Orientación Corporal

### 2.1. Objetivo
Determinar de forma empírica y matemática que:
1. El vector unitario hacia arriba $u$ mantiene su sentido sin importar la inclinación física del dispositivo o variaciones antropométricas.
2. La orientación frontal del torso $F$ apunta inequívocamente hacia donde mira el practicante.
3. Un giro de $+90^\circ$ a la derecha produce un rumbo $\psi \approx +90^\circ$ y a la izquierda $\psi \approx -90^\circ$.
4. Las articulaciones `LEFT_*` y `RIGHT_*` corresponden siempre al lado anatómico del cuerpo, tanto con la cámara frontal como con la trasera.

### 2.2. Protocolo Experimental (Procedimiento E1)
1. **Sujeto de prueba:** Persona de pie en posición inicial erguida (*moa seogi* o *narani seogi*) mirando directamente a la cámara a 2.5 metros.
2. **Estimación del vector vertical $u$:**
   $$u = \frac{\text{mid}(S_L, S_R) - \text{mid}(A_L, A_R)}{\lVert \text{mid}(S_L, S_R) - \text{mid}(A_L, A_R) \rVert}$$
   - *Verificación:* Al elevar el cuerpo (ponerse de puntas) o inclinar levemente el teléfono $\pm 15^\circ$, $u$ conserva la dirección vertical anatómica.
3. **Plano del suelo y eje de cadera horizontal:**
   $$G(v) = v - (v \cdot u) u, \qquad h = P_{R\_H} - P_{L\_H}, \qquad \hat{h}_g = \frac{G(h)}{\lVert G(h) \rVert}$$
4. **Determinación del vector frontal $F$ y signo $\sigma$:**
   $$\sigma = \operatorname{sign}\left( G(u \times h) \cdot \overline{G(P_{Toe} - P_{Heel})} \right), \qquad F = \sigma \frac{G(u \times h)}{\lVert G(u \times h) \rVert}$$
   - *Verificación:* Debido al producto punto con la dirección talón-punta de los pies, $\sigma$ corrige automáticamente cualquier ambigüedad de mano (*handedness*) o inversión de producto cruz, garantizando que $F$ señale hacia el frente visual del atleta.
5. **Cálculo de rumbo $\psi$:**
   $$\psi = \operatorname{atan2}(F \cdot r_0, \; F \cdot F_0) \cdot \frac{180}{\pi}$$
   - Rotación $+90^\circ$ a la derecha: $\psi \approx +90.0^\circ \pm 3.5^\circ$.
   - Rotación $-90^\circ$ a la izquierda: $\psi \approx -90.0^\circ \pm 3.5^\circ$.
   - Rotación $180^\circ$: $\psi \approx \pm 180.0^\circ \pm 4.0^\circ$.

---

## 3. Experimento E2: Comportamiento en Giros de 180° e Intercambio de Lados

### 3.1. Problema Identificado
Al ejecutar giros completos de $180^\circ$ (movimientos 3, 9 y 15 de Taegeuk 1 Jang), el practicante queda de espaldas a la cámara. En modelos de visión monoculares:
- La profundidad $Z$ es susceptible a mayor ruido.
- Existe riesgo de que el estimador intercambie inadvertidamente las articulaciones izquierda y derecha (e.g. clasificar el brazo derecho anatómico como izquierdo).

### 3.2. Criterios de Mitigación y Detección
1. **Invariancia del eje de cadera:**  
   Se monitorea la longitud del segmento de cadera $\lVert P_{R\_H} - P_{L\_H} \rVert$. Si la relación respecto al valor calibrado supera `HIP_AXIS_REL_TOL` (tolerancia relativa de $\pm 35\%$), el cuadro se marca como `DEGENERATE_GEOMETRY`.
2. **Detección de saltos bruscos de rumbo (`PSI_JUMP_DEG`):**  
   Físicamente, un giro de taekwondo en un intervalo de 33 ms ($\sim 30\text{ fps}$) no puede exceder $60^\circ$ entre cuadros consecutivos. Si $\lvert \psi_t - \psi_{t-1} \rvert > 60^\circ$, el cuadro se invalida con motivo `UNAVAILABLE(UNSTABLE)`.
3. **Visibilidad mínima:**  
   En posiciones de espaldas donde las articulaciones de ataque (puño/muñeca) quedan ocluidas por el torso, la visibilidad cae por debajo de `VIS_MIN = 0.5`, resultando en `UNAVAILABLE(LOW_VISIBILITY)`.

---

## 4. Estado de Ejecución de los Experimentos

| Experimento | Tipo de Validación | Estado Actual | Evidencia |
| :--- | :--- | :---: | :--- |
| **E1 (Ejes y Signos)** | Matemática y pruebas unitarias sintéticas | **SUPERADO EN JVM** | `BodyPrimitivesTest` valida $u$, $F$, $\sigma$, $\psi$, invariancia ante rotación, traslación e inversión de handedness. |
| **E1 (Cámara física)** | Validación con persona real y hardware | **Pendiente Externo** | Requiere dispositivo físico conectado (no bloqueante para CI). |
| **E2 (Giros 180°)** | Algoritmo de detección de saltos y tolerancias | **IMPLEMENTADO** | `AngleGeometry.wrapDegrees` y filtros en `AnalysisConfig`. Pruebas sintéticas verificadas. |
| **E2 (Cámara física)** | Verificación de intercambio de lado en vivo | **Pendiente Externo** | Se registrará con capturas en dispositivo físico. |

