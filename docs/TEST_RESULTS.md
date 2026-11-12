# Resultados Acumulados de Pruebas y Regresión (docs/TEST_RESULTS.md)

**Proyecto:** Taegeuk Coach  
**Última actualización:** 11 de noviembre de 2026  

---

## Resumen por Fase

| Fase | Fecha | Pruebas Ejecutadas | Estado | Cobertura :analysis | Comentarios / Evidencia |
| :---: | :---: | :---: | :---: | :---: | :--- |
| **F0** | 2026-11-11 | Validación estructural y matemática del JSON (`poomsae_taegeuk1.json`), verificación de Gradle dry-run | **PASS (Verde)** | N/A (Fase documental) | Se verificaron los 18 movimientos, índices 1..18, giros válidos {-180, -90, 0, 90, 180}, técnicas cerradas, kihap único en 18 y cierre angular perfecto (0° tras giro de retorno). |

---

## Detalle de Pruebas - Fase F0

### Test T-F0-01: Verificación de Integridad y Esquema de `poomsae_taegeuk1.json`
- **Comando:** `python3 -c "import json; ..."`
- **Resultado:** PASS
- **Evidencia:**
  - `schemaVersion`: 2
  - `validation.status`: "DOCUMENTALLY_FIXED"
  - `validation.validatedBy`: "Contraste documental con fuentes publicadas (CR-001)"
  - Cantidad de movimientos: 18 (índices 1 a 18 consecutivos)
  - Posturas válidas: AS, AK
  - Técnicas válidas: ARAE_MAKKI, MOMTONG_JIREUGI, MOMTONG_AN_MAKKI, EOLGUL_MAKKI, AP_CHAGI
  - Kihap: `false` en 1..17, `true` en 18

### Test T-F0-02: Verificación de Cierre Angular (Rumbo Acumulado)
- **Resultado:** PASS
- **Rumbo acumulado paso a paso:**
  - Mov 1: -90° (Cum: -90°)
  - Mov 2: 0° (Cum: -90°)
  - Mov 3: +180° (Cum: +90°)
  - Mov 4: 0° (Cum: +90°)
  - Mov 5: -90° (Cum: 0°)
  - Mov 6: 0° (Cum: 0°)
  - Mov 7: +90° (Cum: +90°)
  - Mov 8: 0° (Cum: +90°)
  - Mov 9: -180° (Cum: -90°)
  - Mov 10: 0° (Cum: -90°)
  - Mov 11: +90° (Cum: 0°)
  - Mov 12: 0° (Cum: 0°)
  - Mov 13: -90° (Cum: -90°)
  - Mov 14: 0° (Cum: -90°)
  - Mov 15: +180° (Cum: +90°)
  - Mov 16: 0° (Cum: +90°)
  - Mov 17: +90° (Cum: +180°)
  - Mov 18: 0° (Cum: +180°)
  - Giro final de retorno: -180° -> Rumbo final: (180 - 180) % 360 = 0° (Orientación inicial exacta).

### Test T-F0-03: Verificación de Línea Base Gradle
- **Comando:** `./gradlew tasks --dry-run`
- **Resultado:** BUILD SUCCESSFUL in 1s.

