package mx.taekwondo.poomsae.analysis.poomsae

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class PoomsaeLoaderTest {
    private companion object {
        private const val EXPECTED_MOVEMENTS = 18
        private const val MOV_1_INDEX = 1
        private const val MOV_3_INDEX = 3
        private const val MOV_5_INDEX = 5
        private const val MOV_6_INDEX = 6
        private const val MOV_12_INDEX = 12
        private const val MOV_14_INDEX = 14
        private const val MOV_16_INDEX = 16
        private const val MOV_17_INDEX = 17
        private const val MOV_18_INDEX = 18
        private const val DEG_0 = 0.0
        private const val DEG_PLUS_90 = 90.0
        private const val DEG_MINUS_90 = -90.0
        private const val DEG_180 = 180.0
        private const val EPS = 1e-6
    }

    private fun canonicalJsonText(): String {
        val candidates =
            listOf(
                File("poomsae_taegeuk1.json"),
                File("../poomsae_taegeuk1.json"),
            )
        val file =
            candidates.firstOrNull { it.exists() && it.isFile }
                ?: error("No se encontró poomsae_taegeuk1.json en el directorio de trabajo")
        return file.readText(Charsets.UTF_8)
    }

    @Test
    fun `canonical taegeuk1 json loads validly with cumulative headings and angular closure`() {
        val result = PoomsaeLoader.loadFromString(canonicalJsonText())
        assertTrue("Se esperaba carga exitosa, pero falló: $result", result is PoomsaeLoadResult.Success)
        val def = (result as PoomsaeLoadResult.Success).definition

        assertEquals(PoomsaeValidator.EXPECTED_SCHEMA_VERSION, def.schemaVersion)
        assertEquals("taegeuk_1", def.poomsae)
        assertEquals(ValidationStatus.DOCUMENTALLY_FIXED, def.validationStatus)
        assertFalse(def.officialCertification)
        assertEquals(EXPECTED_MOVEMENTS, def.movements.size)
        assertEquals(EXPECTED_MOVEMENTS, def.headingsByMovement.size)

        // Rumbos acumulados calculados a mano:
        // Mov 1: -90°, Mov 3: -90 + 180 = +90°, Mov 5: +90 - 90 = 0°, Mov 17: 180°, Mov 18: 180°
        assertEquals(DEG_MINUS_90, def.headingsByMovement[MOV_1_INDEX - 1].normalizedHeadingDeg, EPS)
        assertEquals(DEG_PLUS_90, def.headingsByMovement[MOV_3_INDEX - 1].normalizedHeadingDeg, EPS)
        assertEquals(DEG_0, def.headingsByMovement[MOV_5_INDEX - 1].normalizedHeadingDeg, EPS)
        assertEquals(DEG_180, def.headingsByMovement[MOV_17_INDEX - 1].normalizedHeadingDeg, EPS)
        assertEquals(DEG_180, def.finalHeadingDeg, EPS)
        // Tras giro de retorno (-180°), el rumbo cierra en 0°
        assertEquals(DEG_0, def.returnTurnHeadingDeg, EPS)

        assertTrue(def.movements[MOV_6_INDEX - 1].sharesStanceWithPrevious)
        assertTrue(def.movements[MOV_12_INDEX - 1].sharesStanceWithPrevious)
        assertEquals(EvaluationMode.PEAK_THEN_HOLD, def.movements[MOV_14_INDEX - 1].evaluationMode)
        assertEquals(EvaluationMode.PEAK_THEN_HOLD, def.movements[MOV_16_INDEX - 1].evaluationMode)
        assertTrue(def.movements[MOV_18_INDEX - 1].kihap)
    }

    @Test
    fun `invalid movement count fails with InvalidMovementCount`() {
        val jsonWithoutMov18 =
            canonicalJsonText().replace(
                Regex(""",\s*\{\s*"index":\s*18[\s\S]*?\}\s*\]"""),
                "\n  ]",
            )
        val result = PoomsaeLoader.loadFromString(jsonWithoutMov18)
        assertTrue(result is PoomsaeLoadResult.Failure)
        val errors = (result as PoomsaeLoadResult.Failure).errors
        assertTrue(errors.any { it is PoomsaeValidationError.InvalidMovementCount && it.found == MOV_17_INDEX })
    }

    @Test
    fun `disallowed turn angle fails with DisallowedTurnAngle`() {
        val invalidTurnJson =
            canonicalJsonText().replaceFirst(
                """"expectedTurnDeg": -90,
      "turnDegrees": -90,""",
                """"expectedTurnDeg": 45,
      "turnDegrees": 45,""",
            )
        val result = PoomsaeLoader.loadFromString(invalidTurnJson)
        assertTrue(result is PoomsaeLoadResult.Failure)
        val errors = (result as PoomsaeLoadResult.Failure).errors
        assertTrue(errors.any { it is PoomsaeValidationError.DisallowedTurnAngle })
    }

    @Test
    fun `unknown technique fails with readable InvalidJsonFormat`() {
        val unknownTechJson = canonicalJsonText().replaceFirst(""""ARAE_MAKKI"""", """"DOLLYEO_CHAGI"""")
        val result = PoomsaeLoader.loadFromString(unknownTechJson)
        assertTrue(result is PoomsaeLoadResult.Failure)
        val errors = (result as PoomsaeLoadResult.Failure).errors
        assertTrue(errors.any { it is PoomsaeValidationError.InvalidJsonFormat })
    }

    @Test
    fun `broken angular closure fails with InvalidAngularClosure`() {
        // Cambiar el giro del mov 17 de +90 a -90 rompe el cierre angular (final = 0° en lugar de 180°)
        val brokenClosureJson =
            canonicalJsonText().replace(
                """"index": 17,
      "expectedTurnDeg": 90,
      "turnDegrees": 90,""",
                """"index": 17,
      "expectedTurnDeg": -90,
      "turnDegrees": -90,""",
            )
        val result = PoomsaeLoader.loadFromString(brokenClosureJson)
        assertTrue(result is PoomsaeLoadResult.Failure)
        val errors = (result as PoomsaeLoadResult.Failure).errors
        assertTrue(errors.any { it is PoomsaeValidationError.InvalidAngularClosure })
    }

    @Test
    fun `duplicate movement index fails with NonConsecutiveMovementIndex`() {
        val duplicateIndexJson =
            canonicalJsonText().replaceFirst(
                """"index": 2,""",
                """"index": 1,""",
            )
        val result = PoomsaeLoader.loadFromString(duplicateIndexJson)
        assertTrue(result is PoomsaeLoadResult.Failure)
        val errors = (result as PoomsaeLoadResult.Failure).errors
        assertTrue(errors.any { it is PoomsaeValidationError.NonConsecutiveMovementIndex })
    }

    @Test
    fun `missing required v2 fields fail with MissingRequiredField`() {
        val v2Fields =
            listOf(
                """"activeJoints": ["LEFT_ANKLE", "RIGHT_ANKLE", "LEFT_WRIST", "RIGHT_WRIST"],""",
                """"evaluationMode": "HOLD",""",
                """"expectedTurnDeg": -90,""",
                """"sharesStanceWithPrevious": false,""",
            )
        v2Fields.forEach { fieldSnippet ->
            val missingFieldJson = canonicalJsonText().replaceFirst(fieldSnippet, "")
            val result = PoomsaeLoader.loadFromString(missingFieldJson)
            assertTrue("Faltante $fieldSnippet debía fallar", result is PoomsaeLoadResult.Failure)
            val errors = (result as PoomsaeLoadResult.Failure).errors
            assertTrue(
                "Se esperaba MissingRequiredField para $fieldSnippet, pero fue $errors",
                errors.any { it is PoomsaeValidationError.MissingRequiredField },
            )
        }
    }

    @Test
    fun `incompatible schemaVersion fails with IncompatibleSchemaVersion`() {
        val oldSchemaJson = canonicalJsonText().replaceFirst(""""schemaVersion": 2,""", """"schemaVersion": 1,""")
        val result = PoomsaeLoader.loadFromString(oldSchemaJson)
        assertTrue(result is PoomsaeLoadResult.Failure)
        val errors = (result as PoomsaeLoadResult.Failure).errors
        assertTrue(errors.any { it is PoomsaeValidationError.IncompatibleSchemaVersion && it.found == 1 })
    }

    @Test
    fun `misplaced or missing kihap fails with InvalidKihapPlacement`() {
        val misplacedKihap =
            canonicalJsonText().replaceFirst(
                """"index": 1,
      "expectedTurnDeg": -90,
      "turnDegrees": -90,
      "sharesStanceWithPrevious": false,
      "stance": { "type": "AS", "frontLeg": "LEFT" },
      "technique": { "type": "ARAE_MAKKI", "arm": "LEFT" },
      "extra": null,
      "evaluationMode": "HOLD",
      "activeJoints": ["LEFT_ANKLE", "RIGHT_ANKLE", "LEFT_WRIST", "RIGHT_WRIST"],
      "kihap": false,""",
                """"index": 1,
      "expectedTurnDeg": -90,
      "turnDegrees": -90,
      "sharesStanceWithPrevious": false,
      "stance": { "type": "AS", "frontLeg": "LEFT" },
      "technique": { "type": "ARAE_MAKKI", "arm": "LEFT" },
      "extra": null,
      "evaluationMode": "HOLD",
      "activeJoints": ["LEFT_ANKLE", "RIGHT_ANKLE", "LEFT_WRIST", "RIGHT_WRIST"],
      "kihap": true,""",
            )
        val resultMisplaced = PoomsaeLoader.loadFromString(misplacedKihap)
        assertTrue(resultMisplaced is PoomsaeLoadResult.Failure)
        assertTrue(
            (resultMisplaced as PoomsaeLoadResult.Failure).errors.any {
                it is PoomsaeValidationError.InvalidKihapPlacement
            },
        )

        val missingFinalKihap = canonicalJsonText().replaceFirst(""""kihap": true,""", """"kihap": false,""")
        val resultMissing = PoomsaeLoader.loadFromString(missingFinalKihap)
        assertTrue(resultMissing is PoomsaeLoadResult.Failure)
        assertTrue(
            (resultMissing as PoomsaeLoadResult.Failure).errors.any {
                it is PoomsaeValidationError.InvalidKihapPlacement
            },
        )
    }

    @Test
    fun `every stance and technique pair in poomsae resolves required joints in metric registry`() {
        val result = PoomsaeLoader.loadFromString(canonicalJsonText()) as PoomsaeLoadResult.Success
        val movements = result.definition.movements

        movements.forEach { movement ->
            val resolvedMetrics = MetricSheetRegistry.resolveMovementMetrics(movement)
            assertTrue(
                "Movimiento ${movement.index} debe resolver métricas",
                resolvedMetrics.isNotEmpty(),
            )
            resolvedMetrics.forEach { metric ->
                assertTrue(
                    "Métrica ${metric.id} en mov ${movement.index} debe declarar articulaciones",
                    metric.requiredJoints.isNotEmpty(),
                )
            }
            if (movement.turnDegrees != 0) {
                assertTrue(resolvedMetrics.any { it.id == MetricId.DI_01 })
                assertTrue(resolvedMetrics.any { it.id == MetricId.DI_02 })
            }
        }
    }
}
