package mx.taekwondo.poomsae.analysis.evaluation

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.BodyProfile
import mx.taekwondo.poomsae.analysis.model.MetricResult
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.MetricId
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.TechniqueType

/** Opciones accesorias para la evaluación de un movimiento. */
data class EvaluationOptions(
    val window: List<PoseFrame> = emptyList(),
    val measuredTurnDeg: Double? = null,
    val attempts: Int = 1,
    val peakFrame: PoseFrame? = null,
)

private data class ScoringContext(
    val movement: Movement,
    val metrics: Map<MetricId, MetricResult>,
    val weights: Map<MetricId, Double>,
    val criticalSet: Set<MetricId>,
    val evaluability: Evaluability,
    val attempts: Int,
)

/**
 * Evaluador de movimientos del poomsae implementando estrictamente el modelo
 * de evaluabilidad de la sección 13.7 y la puntuación por bandas de la sección 13.8.
 */
object MovementEvaluator {
    private const val PERFECT_SCORE = 100.0

    /** Evalúa un movimiento de forma estática en su pose clave o ventana de sostén. */
    fun evaluate(
        frame: PoseFrame,
        movement: Movement,
        profile: BodyProfile,
        options: EvaluationOptions = EvaluationOptions(),
    ): MovementEvaluation {
        val window = options.window.ifEmpty { listOf(frame) }
        val calculatedMetrics =
            MetricCalculator.calculateMetrics(
                frame = frame,
                movement = movement,
                profile = profile,
                options = options,
            )

        val criticalSet = resolveCriticalMetrics(movement)
        val metricWeights =
            calculatedMetrics.keys.associateWith { id ->
                if (id in criticalSet) AnalysisConfig.WEIGHT_CRITICAL else AnalysisConfig.WEIGHT_SECONDARY
            }

        val totalWeight = metricWeights.values.sum()
        val availableWeights =
            metricWeights.filter { (id, _) ->
                calculatedMetrics[id] is MetricResult.Value
            }.values.sum()

        val coverage = if (totalWeight > 0.0) availableWeights / totalWeight else 0.0
        val allCriticalAvailable =
            criticalSet.all { id ->
                calculatedMetrics[id] is MetricResult.Value
            }

        val evaluability = determineEvaluability(allCriticalAvailable, coverage)
        if (evaluability == Evaluability.NOT_EVALUABLE) {
            return MovementEvaluation(
                index = movement.index,
                outcome = MovementOutcome.EVALUATED,
                evaluability = Evaluability.NOT_EVALUABLE,
                category = null,
                score = null,
                metrics = calculatedMetrics,
                feedback = emptyList(),
                attempts = options.attempts,
            )
        }

        val scoringContext =
            ScoringContext(
                movement = movement,
                metrics = calculatedMetrics,
                weights = metricWeights,
                criticalSet = criticalSet,
                evaluability = evaluability,
                attempts = options.attempts,
            )
        return scoreMovement(scoringContext)
    }

    /** Sobrecarga conveniente para evaluación con ventana temporal. */
    fun evaluate(
        frame: PoseFrame,
        movement: Movement,
        profile: BodyProfile,
        window: List<PoseFrame>,
        measuredTurnDeg: Double? = null,
    ): MovementEvaluation =
        evaluate(
            frame = frame,
            movement = movement,
            profile = profile,
            options =
                EvaluationOptions(
                    window = window,
                    measuredTurnDeg = measuredTurnDeg,
                ),
        )

    private fun determineEvaluability(
        allCriticalAvailable: Boolean,
        coverage: Double,
    ): Evaluability =
        when {
            !allCriticalAvailable || coverage < AnalysisConfig.MIN_COVERAGE -> Evaluability.NOT_EVALUABLE
            coverage >= AnalysisConfig.FULL_COVERAGE -> Evaluability.EVALUABLE
            else -> Evaluability.PARTIAL
        }

    private fun scoreMovement(context: ScoringContext): MovementEvaluation {
        var weightedSum = 0.0
        var weightSum = 0.0
        var hasCriticalBreach = false
        val feedbackList = mutableListOf<FeedbackItem>()

        for ((id, result) in context.metrics) {
            if (result !is MetricResult.Value) continue
            val band = MetricBandRegistry.resolveBand(id, context.movement)
            val metricScore = band.score(result.value)
            val weight = context.weights[id] ?: AnalysisConfig.WEIGHT_SECONDARY

            weightedSum += metricScore * weight
            weightSum += weight

            if (metricScore < PERFECT_SCORE) {
                FeedbackRuleEngine.generateFeedback(id, result.value, band)?.let { feedbackList.add(it) }
                if (id in context.criticalSet && metricScore <= 0.0) {
                    hasCriticalBreach = true
                }
            }
        }

        val finalScore = if (weightSum > 0.0) weightedSum / weightSum else 0.0
        val category = resolveCategory(finalScore, hasCriticalBreach)

        return MovementEvaluation(
            index = context.movement.index,
            outcome = MovementOutcome.EVALUATED,
            evaluability = context.evaluability,
            category = category,
            score = finalScore,
            metrics = context.metrics,
            feedback = feedbackList,
            attempts = context.attempts,
        )
    }

    private fun resolveCategory(
        finalScore: Double,
        hasCriticalBreach: Boolean,
    ): MovementCategory =
        when {
            hasCriticalBreach || finalScore < AnalysisConfig.T_IMPROVABLE -> MovementCategory.INCORRECT
            finalScore >= AnalysisConfig.T_CORRECT -> MovementCategory.CORRECT
            else -> MovementCategory.IMPROVABLE
        }

    private fun resolveCriticalMetrics(movement: Movement): Set<MetricId> {
        val stanceCritical = setOf(MetricId.ST_01, MetricId.ST_03, MetricId.ST_04)
        val techCritical =
            when (movement.technique.type) {
                TechniqueType.MOMTONG_JIREUGI -> setOf(MetricId.AR_01, MetricId.AR_05)
                TechniqueType.ARAE_MAKKI -> setOf(MetricId.AR_02, MetricId.AR_03)
                TechniqueType.MOMTONG_AN_MAKKI -> setOf(MetricId.AR_06, MetricId.AR_07)
                TechniqueType.EOLGUL_MAKKI -> setOf(MetricId.AR_08)
                TechniqueType.AP_CHAGI -> setOf(MetricId.KI_01, MetricId.KI_02)
            }
        return stanceCritical + techCritical
    }
}
