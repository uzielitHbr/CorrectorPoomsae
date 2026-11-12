package tkd.apptkd.data.room

import kotlinx.serialization.json.Json
import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.model.MetricResult
import tkd.apptkd.ui.SessionSummaryData
import java.util.UUID

data class SessionPersistParams(
    val sessionId: String = UUID.randomUUID().toString(),
    val startedAtMs: Long = System.currentTimeMillis(),
    val endedAtMs: Long? = System.currentTimeMillis(),
    val status: String = "COMPLETED",
    val mode: String = "LIVE",
    val strictMode: Boolean = false,
    val cameraFacing: String = "FRONT",
    val snapshotPaths: Map<Int, String> = emptyMap(),
)

/**
 * Mapeador entre modelos de dominio y entidades Room con serialización JSON versionada (Sección 12.10).
 */
object SessionEntityMapper {

    private val json = Json { ignoreUnknownKeys = true }

    fun toEntities(
        summaryData: SessionSummaryData,
        params: SessionPersistParams = SessionPersistParams(),
    ): Pair<SessionEntity, List<MovementResultEntity>> {
        val flagsList: List<String> = summaryData.evaluations
            .flatMap { it.flags }
            .distinct()
        val flagsJson = json.encodeToString(
            FlagsJsonContainer.serializer(),
            FlagsJsonContainer(flags = flagsList),
        )

        val sessionEntity = SessionEntity(
            id = params.sessionId,
            startedAtMs = params.startedAtMs,
            endedAtMs = params.endedAtMs,
            status = params.status,
            mode = params.mode,
            strictMode = params.strictMode,
            poomsaeStatus = summaryData.definition.validationStatus.name,
            configVersion = AnalysisConfig.CONFIG_VERSION,
            poseModelName = "pose_landmarker_lite",
            appVersion = "1.0",
            cameraFacing = params.cameraFacing,
            globalScore = summaryData.globalScore,
            evaluableCount = summaryData.evaluableCount,
            partialCount = summaryData.partialCount,
            notEvaluableCount = summaryData.notEvaluableCount,
            flagsJson = flagsJson,
        )

        val movementEntities = summaryData.evaluations.map { eval ->
            val feedbackEntries = eval.feedback.map { item ->
                FeedbackEntryDto(
                    code = item.code.name,
                    metricId = item.metricId.name,
                    measuredValue = item.measuredValue,
                    targetMin = item.targetMin,
                    targetMax = item.targetMax,
                )
            }
            val feedbackJson = json.encodeToString(
                FeedbackJsonContainer.serializer(),
                FeedbackJsonContainer(items = feedbackEntries),
            )

            val metricEntries = eval.metrics.map { (id, result) ->
                when (result) {
                    is MetricResult.Value -> MetricEntryDto(
                        metricId = id.name,
                        value = result.value,
                        confidence = result.confidence,
                        isUnavailable = false,
                        unavailableReason = null,
                    )
                    is MetricResult.Unavailable -> MetricEntryDto(
                        metricId = id.name,
                        value = null,
                        confidence = null,
                        isUnavailable = true,
                        unavailableReason = result.reason.name,
                    )
                }
            }
            val metricsJson = json.encodeToString(
                MetricsJsonContainer.serializer(),
                MetricsJsonContainer(metrics = metricEntries),
            )

            MovementResultEntity(
                sessionId = params.sessionId,
                movementIndex = eval.index,
                outcome = eval.outcome.name,
                evaluability = eval.evaluability?.name,
                category = eval.category?.name,
                score = eval.score,
                attempts = eval.attempts,
                feedbackJson = feedbackJson,
                metricsJson = metricsJson,
                snapshotPath = params.snapshotPaths[eval.index],
            )
        }

        return Pair(sessionEntity, movementEntities)
    }
}

