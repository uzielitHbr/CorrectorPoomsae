package tkd.apptkd.data.room

import kotlinx.serialization.Serializable

/**
 * Contenedor DTO serializable para los flags de evaluación de la sesión (Sección 12.10).
 */
@Serializable
data class FlagsJsonContainer(
    val schemaVersion: Int = 1,
    val flags: List<String> = emptyList(),
)

/**
 * Entrada DTO serializable para un ítem de retroalimentación técnica.
 */
@Serializable
data class FeedbackEntryDto(
    val code: String,
    val metricId: String,
    val measuredValue: Double?,
    val targetMin: Double,
    val targetMax: Double,
)

/**
 * Contenedor DTO serializable para las observaciones de retroalimentación (Sección 12.10).
 */
@Serializable
data class FeedbackJsonContainer(
    val schemaVersion: Int = 1,
    val items: List<FeedbackEntryDto> = emptyList(),
)

/**
 * Entrada DTO serializable para el resultado de una métrica individual.
 */
@Serializable
data class MetricEntryDto(
    val metricId: String,
    val value: Double?,
    val confidence: Float?,
    val isUnavailable: Boolean = false,
    val unavailableReason: String? = null,
)

/**
 * Contenedor DTO serializable para las métricas biomecánicas calculadas (Sección 12.10).
 */
@Serializable
data class MetricsJsonContainer(
    val schemaVersion: Int = 1,
    val metrics: List<MetricEntryDto> = emptyList(),
)

/**
 * DTO para un movimiento evaluado dentro del borrador de sesión interrumpida.
 */
@Serializable
data class MovementDraftDto(
    val index: Int,
    val outcome: String,
    val evaluability: String?,
    val category: String?,
    val score: Double?,
    val attempts: Int,
    val feedback: List<FeedbackEntryDto>,
    val metrics: List<MetricEntryDto>,
)

/**
 * Contenedor DTO serializable para el estado completo de un borrador de sesión (Sección 12.10 y SC-18).
 */
@Serializable
data class SessionDraftStateDto(
    val schemaVersion: Int = 1,
    val sessionId: String,
    val currentMovementIndex: Int,
    val lastEvaluatedIndex: Int,
    val startedAtMs: Long,
    val mode: String,
    val strictMode: Boolean,
    val cameraFacing: String,
    val evaluatedMovements: List<MovementDraftDto> = emptyList(),
)

