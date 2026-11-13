package mx.taekwondo.poomsae.analysis.config

/**
 * Evaluador de estados de validación del sistema según la Sección 16.5.
 *
 * Determina si la aplicación debe operar en modo experimental y restringe
 * la exhibición de insignias de validación si no se satisfacen los criterios muestrales.
 */
object ValidationStateEvaluator {
    /**
     * Determina si el sistema se encuentra en modo experimental.
     * En estado [ValidationState.NO_VALIDADO], este valor es obligatoriamente true.
     */
    fun isExperimental(state: ValidationState = AnalysisConfig.VALIDATION_STATE): Boolean {
        return state == ValidationState.NO_VALIDADO
    }

    /**
     * Determina si se puede exhibir la palabra o insignia "Validado" en la interfaz (Sección 16.5).
     * Solo permitido si el estado ha alcanzado [ValidationState.VALIDADO_ALCANCE_LIMITADO].
     */
    fun canDisplayValidatedBadge(state: ValidationState = AnalysisConfig.VALIDATION_STATE): Boolean {
        return state == ValidationState.VALIDADO_ALCANCE_LIMITADO
    }

    /**
     * Descripción textual técnica del estado de validación para reportes y depuración.
     */
    fun getValidationStateDescription(state: ValidationState = AnalysisConfig.VALIDATION_STATE): String =
        when (state) {
            ValidationState.NO_VALIDADO ->
                "NO_VALIDADO (Modo experimental / Puntuación orientativa)"
            ValidationState.VALIDACION_PRELIMINAR ->
                "VALIDACION_PRELIMINAR (Dataset preliminar sin panel ciego)"
            ValidationState.VALIDADO_ALCANCE_LIMITADO ->
                "VALIDADO_ALCANCE_LIMITADO (Muestra mínima y panel de jueces)"
        }
}
