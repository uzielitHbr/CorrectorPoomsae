package mx.taekwondo.poomsae.analysis.poomsae

import kotlinx.serialization.Serializable
import mx.taekwondo.poomsae.analysis.model.Joint

/** Lateralidad corporal para piernas de apoyo/ataque y brazos ejecutores. */
@Serializable
enum class Side {
    LEFT,
    RIGHT,
    ;

    /** Devuelve el lado contrario. */
    fun opposite(): Side = if (this == LEFT) RIGHT else LEFT
}

/** Posiciones (Sogi) cerradas para Taegeuk 1 Jang. */
@Serializable
enum class StanceType {
    /** Ap Seogi (paso corto). */
    AS,

    /** Ap Kubi (paso largo). */
    AK,
}

/** Especificación cerrada de postura corporal en un movimiento. */
@Serializable
data class Stance(
    val type: StanceType,
    val frontLeg: Side,
)

/** Técnicas cerradas permitidas en Taegeuk 1 Jang. */
@Serializable
enum class TechniqueType {
    ARAE_MAKKI,
    MOMTONG_JIREUGI,
    MOMTONG_AN_MAKKI,
    EOLGUL_MAKKI,
    AP_CHAGI,
}

/** Especificación cerrada de técnica y extremidad ejecutora. */
@Serializable
data class Technique(
    val type: TechniqueType,
    val arm: Side,
)

/** Modo de evaluación temporal del movimiento (sección 8.1 / v2). */
@Serializable
enum class EvaluationMode {
    HOLD,
    PEAK_THEN_HOLD,
}

/** Estado documental o de certificación oficial del poomsae (Regla R-14). */
@Serializable
enum class ValidationStatus {
    UNVERIFIED,
    DOCUMENTALLY_FIXED,
    OFFICIALLY_CERTIFIED,
}

/** Metadatos de trazabilidad documental de la definición del poomsae. */
@Serializable
data class PoomsaeValidationInfo(
    val status: ValidationStatus,
    val validatedBy: String,
    val validatedOn: String,
    val officialSource: String? = null,
    val sourceReferences: String,
    val officialCertification: Boolean,
    val limitations: List<String>,
)

/** Definición cerrada de un movimiento individual del poomsae (esquema v2). */
@Serializable
data class Movement(
    val index: Int,
    val expectedTurnDeg: Int,
    val turnDegrees: Int,
    val sharesStanceWithPrevious: Boolean,
    val stance: Stance,
    val technique: Technique,
    val extra: Technique? = null,
    val evaluationMode: EvaluationMode,
    val activeJoints: List<Joint>,
    val kihap: Boolean,
    val nameKo: String,
    val nameEs: String,
)

/** Rumbo acumulado calculado para un movimiento específico. */
data class MovementHeading(
    val movementIndex: Int,
    val turnDegrees: Int,
    val cumulativeHeadingDeg: Double,
    val normalizedHeadingDeg: Double,
)

/** Definición validada e inmutable de un poomsae completo. */
data class PoomsaeDefinition(
    val schemaVersion: Int,
    val poomsae: String,
    val validation: PoomsaeValidationInfo,
    val movements: List<Movement>,
    val headingsByMovement: List<MovementHeading>,
    val finalHeadingDeg: Double,
    val returnTurnHeadingDeg: Double,
) {
    /** Estado explícito de validación documental o certificación oficial. */
    val validationStatus: ValidationStatus
        get() = validation.status

    /** Bandera explícita de certificación oficial de Kukkiwon. */
    val officialCertification: Boolean
        get() = validation.officialCertification
}
