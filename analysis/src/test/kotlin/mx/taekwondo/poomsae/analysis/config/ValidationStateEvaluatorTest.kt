package mx.taekwondo.poomsae.analysis.config

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ValidationStateEvaluator] and [AnalysisConfig] validation lifecycle rules (Section 16.5).
 */
class ValidationStateEvaluatorTest {
    @Test
    fun testDefaultSystemStateIsNoValidadoAndPendienteCalibrar() {
        assertEquals(ValidationState.NO_VALIDADO, AnalysisConfig.VALIDATION_STATE)
        assertEquals(ParameterCalibrationStatus.PENDIENTE_CALIBRAR, AnalysisConfig.CALIBRATION_STATUS)
    }

    @Test
    fun testIsExperimentalReturnsTrueOnlyForNoValidado() {
        // Default parameter uses AnalysisConfig.VALIDATION_STATE
        assertTrue(ValidationStateEvaluator.isExperimental())

        // Explicit states
        assertTrue(ValidationStateEvaluator.isExperimental(ValidationState.NO_VALIDADO))
        assertFalse(ValidationStateEvaluator.isExperimental(ValidationState.VALIDACION_PRELIMINAR))
        assertFalse(ValidationStateEvaluator.isExperimental(ValidationState.VALIDADO_ALCANCE_LIMITADO))
    }

    @Test
    fun testCanDisplayValidatedBadgeReturnsTrueOnlyForValidadoAlcanceLimitado() {
        // Default parameter uses AnalysisConfig.VALIDATION_STATE
        assertFalse(ValidationStateEvaluator.canDisplayValidatedBadge())

        // Explicit states
        assertFalse(ValidationStateEvaluator.canDisplayValidatedBadge(ValidationState.NO_VALIDADO))
        assertFalse(ValidationStateEvaluator.canDisplayValidatedBadge(ValidationState.VALIDACION_PRELIMINAR))
        assertTrue(ValidationStateEvaluator.canDisplayValidatedBadge(ValidationState.VALIDADO_ALCANCE_LIMITADO))
    }

    @Test
    fun testGetValidationStateDescriptionCoversAllEnumEntries() {
        for (state in ValidationState.entries) {
            val description = ValidationStateEvaluator.getValidationStateDescription(state)
            assertTrue("Description for $state should not be blank", description.isNotBlank())
            assertTrue("Description should mention enum name $state", description.contains(state.name))
        }
    }
}
