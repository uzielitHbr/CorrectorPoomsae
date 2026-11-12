package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.poomsae.MetricId

/**
 * Grado de evaluabilidad técnica de un movimiento según la sección 13.7.
 */
enum class Evaluability {
    /** Todas las métricas críticas disponibles y cobertura >= FULL_COVERAGE (80%). */
    EVALUABLE,

    /** Todas las críticas disponibles pero cobertura entre MIN_COVERAGE (50%) y 80%. */
    PARTIAL,

    /** Falta alguna métrica crítica o la cobertura de pesos es menor que 50%. */
    NOT_EVALUABLE,
}

/**
 * Categoría cualitativa asignada únicamente a movimientos evaluables o parciales (Sección 13.7).
 */
enum class MovementCategory {
    /** Cumple con la banda técnica y no viola límites exteriores críticos. */
    CORRECT,

    /** Desviación técnica moderada subsanable con retroalimentación. */
    IMPROVABLE,

    /** Puntuación deficiente o alguna métrica crítica fuera de su límite exterior. */
    INCORRECT,
}

/**
 * Resultado temporal o de desenlace de la ejecución del movimiento.
 */
enum class MovementOutcome {
    EVALUATED,
    OMITTED,
    MOTION_LOST,
}

/**
 * Evaluación completa y trazable de un movimiento individual de poomsae (Sección 12.9.2).
 */
data class MovementEvaluation(
    val index: Int,
    val outcome: MovementOutcome,
    val evaluability: Evaluability?,
    val category: MovementCategory?,
    val score: Double?,
    val metrics: Map<MetricId, MetricResult>,
    val feedback: List<FeedbackItem>,
    val attempts: Int = 1,
    val flags: Set<String> = emptySet(),
)
