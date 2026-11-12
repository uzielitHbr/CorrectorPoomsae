package mx.taekwondo.poomsae.analysis.poomsae

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import mx.taekwondo.poomsae.analysis.geometry.AngleGeometry
import kotlin.math.abs

/**
 * Validador estricto de la definición de Taegeuk 1 Jang conforme a las secciones 6.5 y 8.1 (v2).
 *
 * Nunca corrige ni altera silenciosamente los datos cargados; cualquier discrepancia produce
 * errores explícitos y legibles.
 */
object PoomsaeValidator {
    const val EXPECTED_SCHEMA_VERSION: Int = 2
    const val EXPECTED_MOVEMENT_COUNT: Int = 18
    const val KIHAP_MOVEMENT_INDEX: Int = 18
    private const val TURN_MINUS_180: Int = -180
    private const val TURN_MINUS_90: Int = -90
    private const val TURN_ZERO: Int = 0
    private const val TURN_PLUS_90: Int = 90
    private const val TURN_PLUS_180: Int = 180
    private const val RETURN_TURN_DEG: Double = 180.0
    private const val ZERO_HEADING_DEG: Double = 0.0

    val ALLOWED_TURNS: Set<Int> =
        setOf(
            TURN_MINUS_180,
            TURN_MINUS_90,
            TURN_ZERO,
            TURN_PLUS_90,
            TURN_PLUS_180,
        )

    /** Valida una estructura deserializada de poomsae y calcula los rumbos acumulados. */
    fun validate(
        schemaVersion: Int,
        poomsae: String,
        validation: PoomsaeValidationInfo,
        movements: List<Movement>,
    ): PoomsaeLoadResult {
        val errors = mutableListOf<PoomsaeValidationError>()

        if (schemaVersion != EXPECTED_SCHEMA_VERSION) {
            errors.add(PoomsaeValidationError.IncompatibleSchemaVersion(schemaVersion, EXPECTED_SCHEMA_VERSION))
        }
        if (poomsae.isBlank()) {
            errors.add(PoomsaeValidationError.MissingRequiredField("poomsae", "raíz"))
        }
        if (movements.size != EXPECTED_MOVEMENT_COUNT) {
            errors.add(PoomsaeValidationError.InvalidMovementCount(movements.size, EXPECTED_MOVEMENT_COUNT))
        }

        validateMovements(movements, errors)
        val headings = computeHeadings(movements)
        val finalHeadingDeg = headings.lastOrNull()?.normalizedHeadingDeg ?: ZERO_HEADING_DEG
        val returnTurnHeadingDeg = AngleGeometry.wrapDegrees(finalHeadingDeg - RETURN_TURN_DEG)

        if (movements.size == EXPECTED_MOVEMENT_COUNT &&
            abs(returnTurnHeadingDeg) > AnalysisConfig.EPS_ANGLE_DEG
        ) {
            errors.add(
                PoomsaeValidationError.InvalidAngularClosure(
                    finalHeadingDeg = finalHeadingDeg,
                    returnHeadingDeg = returnTurnHeadingDeg,
                    expectedDeg = ZERO_HEADING_DEG,
                ),
            )
        }

        return if (errors.isEmpty()) {
            PoomsaeLoadResult.Success(
                PoomsaeDefinition(
                    schemaVersion = schemaVersion,
                    poomsae = poomsae,
                    validation = validation,
                    movements = movements,
                    headingsByMovement = headings,
                    finalHeadingDeg = finalHeadingDeg,
                    returnTurnHeadingDeg = returnTurnHeadingDeg,
                ),
            )
        } else {
            PoomsaeLoadResult.Failure(errors)
        }
    }

    private fun validateMovements(
        movements: List<Movement>,
        errors: MutableList<PoomsaeValidationError>,
    ) {
        movements.forEachIndexed { idx, movement ->
            val expectedIndex = idx + 1
            if (movement.index != expectedIndex) {
                errors.add(PoomsaeValidationError.NonConsecutiveMovementIndex(expectedIndex, movement.index))
            }
            if (movement.turnDegrees !in ALLOWED_TURNS) {
                errors.add(
                    PoomsaeValidationError.DisallowedTurnAngle(
                        movement.index,
                        movement.turnDegrees,
                        ALLOWED_TURNS,
                    ),
                )
            }
            if (movement.expectedTurnDeg != movement.turnDegrees) {
                errors.add(
                    PoomsaeValidationError.TurnMismatch(
                        movement.index,
                        movement.expectedTurnDeg,
                        movement.turnDegrees,
                    ),
                )
            }
            if (movement.activeJoints.isEmpty()) {
                errors.add(PoomsaeValidationError.EmptyActiveJoints(movement.index))
            }
            validateKihap(movement, errors)
        }
    }

    private fun validateKihap(
        movement: Movement,
        errors: MutableList<PoomsaeValidationError>,
    ) {
        if (movement.index != KIHAP_MOVEMENT_INDEX && movement.kihap) {
            errors.add(
                PoomsaeValidationError.InvalidKihapPlacement(
                    "kihap=true en movimiento ${movement.index}; solo permitido en $KIHAP_MOVEMENT_INDEX",
                ),
            )
        }
        if (movement.index == KIHAP_MOVEMENT_INDEX && !movement.kihap) {
            errors.add(
                PoomsaeValidationError.InvalidKihapPlacement(
                    "el movimiento final $KIHAP_MOVEMENT_INDEX debe tener kihap=true",
                ),
            )
        }
    }

    private fun computeHeadings(movements: List<Movement>): List<MovementHeading> {
        var cumulative = ZERO_HEADING_DEG
        return movements.map { movement ->
            cumulative += movement.turnDegrees.toDouble()
            MovementHeading(
                movementIndex = movement.index,
                turnDegrees = movement.turnDegrees,
                cumulativeHeadingDeg = cumulative,
                normalizedHeadingDeg = AngleGeometry.wrapDegrees(cumulative),
            )
        }
    }
}
