package mx.taekwondo.poomsae.analysis.poomsae

/** Errores explícitos y legibles al cargar o validar una definición de poomsae. */
sealed class PoomsaeValidationError(
    val message: String,
) {
    /** Error de sintaxis JSON o tipo de dato inválido al deserializar. */
    class InvalidJsonFormat(
        val detail: String,
    ) : PoomsaeValidationError("Formato JSON inválido: $detail")

    /** Versión de esquema incompatible con el validador v2. */
    class IncompatibleSchemaVersion(
        val found: Int?,
        val expected: Int,
    ) : PoomsaeValidationError("Versión de esquema incompatible: encontrada=$found, esperada=$expected")

    /** Campo obligatorio faltante según el esquema v2. */
    class MissingRequiredField(
        val fieldName: String,
        val context: String,
    ) : PoomsaeValidationError("Campo obligatorio faltante '$fieldName' en $context")

    /** Cantidad de movimientos distinta a la esperada para Taegeuk 1 Jang. */
    class InvalidMovementCount(
        val found: Int,
        val expected: Int,
    ) : PoomsaeValidationError("Cantidad de movimientos inválida: encontrados=$found, esperados=$expected")

    /** Índices de movimientos duplicados o no consecutivos. */
    class NonConsecutiveMovementIndex(
        val expected: Int,
        val found: Int,
    ) : PoomsaeValidationError("Índice de movimiento no consecutivo o duplicado: esperado=$expected, encontrado=$found")

    /** Ángulo de giro fuera del conjunto permitido {-180, -90, 0, 90, 180}. */
    class DisallowedTurnAngle(
        val movementIndex: Int,
        val turnDegrees: Int,
        val allowed: Set<Int>,
    ) : PoomsaeValidationError(
            "Giro no permitido en movimiento $movementIndex: $turnDegrees° (permitidos: ${allowed.sorted()})",
        )

    /** Discrepancia entre expectedTurnDeg y turnDegrees en un movimiento. */
    class TurnMismatch(
        val movementIndex: Int,
        val expectedTurnDeg: Int,
        val turnDegrees: Int,
    ) : PoomsaeValidationError(
            "Inconsistencia de giro en movimiento $movementIndex: " +
                "expectedTurnDeg=$expectedTurnDeg, turnDegrees=$turnDegrees",
        )

    /** Lista de articulaciones activas vacía en un movimiento. */
    class EmptyActiveJoints(
        val movementIndex: Int,
    ) : PoomsaeValidationError("El movimiento $movementIndex debe declarar al menos una articulación activa")

    /** Regla de kihap violada (solo permitido en el movimiento final 18). */
    class InvalidKihapPlacement(
        val detail: String,
    ) : PoomsaeValidationError("Ubicación inválida de kihap: $detail")

    /** La secuencia de giros no cierra angularmente tras el giro de retorno. */
    class InvalidAngularClosure(
        val finalHeadingDeg: Double,
        val returnHeadingDeg: Double,
        val expectedDeg: Double,
    ) : PoomsaeValidationError(
            "Falta cierre angular: rumbo final=$finalHeadingDeg°, " +
                "rumbo tras retorno=$returnHeadingDeg°, esperado=$expectedDeg°",
        )
}

/** Resultado explícito de la carga y validación de un poomsae. */
sealed class PoomsaeLoadResult {
    /** Definición válida y lista para análisis. */
    data class Success(
        val definition: PoomsaeDefinition,
    ) : PoomsaeLoadResult()

    /** Lista no vacía de errores específicos encontrados durante la carga o validación. */
    data class Failure(
        val errors: List<PoomsaeValidationError>,
    ) : PoomsaeLoadResult()
}
