# Problemas Conocidos (docs/KNOWN_ISSUES.md)

**Proyecto:** Taegeuk Coach  
**Última actualización:** 11 de noviembre de 2026 (Fase F0)  

Este documento registra incidencias técnicas, anomalías identificadas, bloqueos o limitaciones detectadas durante la implementación del sistema.

---

| ID | Fecha | Fase Afectada | Descripción del Problema | Causa Probable | Estado (Abierto / Resuelto / Mitigado) | Solución / Mitigación |
| :--- | :---: | :---: | :--- | :--- | :---: | :--- |
| **KI-001** | 2026-11-11 | F0 | Discrepancia bibliográfica entre fuentes sobre la dirección del giro en el movimiento 17 (Tae Kwon Do Nation indicaba izquierda en versión 2026). | Divergencia entre fuentes secundarias no oficiales al describir el ingreso al eje central posterior. | **Resuelto** | Se adoptó formalmente el giro de 90° a la derecha (+90°) en CR-001 tras comprobación matemática de que es la única opción con la que el rumbo acumulado del poomsae cierra en 0° exactos. |
| **KI-002** | 2026-11-11 | F0 | El ejemplar oficial impreso *Taekwondo Textbook* de Kukkiwon (Kim, 2012) no está disponible en el repositorio inicial. | Ausencia de acceso a fuentes físicas primarias. | **Mitigado** | Se contrastaron 6 fuentes secundarias independientes bajo CR-001, estableciendo el estado `DOCUMENTALLY_FIXED` y la advertencia visual reglamentaria "Secuencia fijada documentalmente; no certificada oficialmente". |
