package mx.taekwondo.poomsae.analysis.poomsae

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import java.io.File

/**
 * Cargador oficial de definiciones de poomsae desde JSON v2 mediante kotlinx.serialization.
 */
object PoomsaeLoader {
    private val jsonParser: Json =
        Json {
            ignoreUnknownKeys = false
            isLenient = false
        }

    /** Carga y valida una definición de poomsae desde una cadena JSON. */
    fun loadFromString(jsonText: String): PoomsaeLoadResult {
        val rootElement =
            try {
                jsonParser.parseToJsonElement(jsonText)
            } catch (ex: SerializationException) {
                null to ex
            }

        return if (rootElement is Pair<*, *>) {
            val ex = rootElement.second as SerializationException
            PoomsaeLoadResult.Failure(
                listOf(PoomsaeValidationError.InvalidJsonFormat(ex.message ?: "JSON malformado")),
            )
        } else {
            val preCheckError = preCheckSchemaVersion(rootElement as JsonElement)
            if (preCheckError != null) {
                PoomsaeLoadResult.Failure(listOf(preCheckError))
            } else {
                decodeAndValidate(jsonText)
            }
        }
    }

    /** Carga y valida una definición de poomsae desde un archivo en disco. */
    fun loadFromFile(file: File): PoomsaeLoadResult =
        if (!file.exists() || !file.isFile) {
            PoomsaeLoadResult.Failure(
                listOf(PoomsaeValidationError.InvalidJsonFormat("Archivo no encontrado: ${file.path}")),
            )
        } else {
            loadFromString(file.readText(Charsets.UTF_8))
        }

    private fun preCheckSchemaVersion(rootElement: JsonElement): PoomsaeValidationError? {
        val rootObj = rootElement as? JsonObject
        return when {
            rootObj == null ->
                PoomsaeValidationError.InvalidJsonFormat("La raíz del JSON debe ser un objeto")
            "schemaVersion" !in rootObj ->
                PoomsaeValidationError.MissingRequiredField("schemaVersion", "raíz")
            else -> {
                val version = rootObj["schemaVersion"]?.jsonPrimitive?.intOrNull
                if (version != PoomsaeValidator.EXPECTED_SCHEMA_VERSION) {
                    PoomsaeValidationError.IncompatibleSchemaVersion(
                        found = version,
                        expected = PoomsaeValidator.EXPECTED_SCHEMA_VERSION,
                    )
                } else {
                    null
                }
            }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun decodeAndValidate(jsonText: String): PoomsaeLoadResult =
        try {
            val dto = jsonParser.decodeFromString<RawPoomsaeDto>(jsonText)
            PoomsaeValidator.validate(
                schemaVersion = dto.schemaVersion,
                poomsae = dto.poomsae,
                validation = dto.validation,
                movements = dto.movements,
            )
        } catch (ex: MissingFieldException) {
            val fieldName = ex.missingFields.firstOrNull() ?: "desconocido"
            PoomsaeLoadResult.Failure(
                listOf(
                    PoomsaeValidationError.MissingRequiredField(
                        fieldName = fieldName,
                        context = ex.message ?: "esquema v2",
                    ),
                ),
            )
        } catch (ex: SerializationException) {
            PoomsaeLoadResult.Failure(
                listOf(PoomsaeValidationError.InvalidJsonFormat(ex.message ?: "Error de deserialización")),
            )
        }

    @Serializable
    private data class RawPoomsaeDto(
        val schemaVersion: Int,
        val validation: PoomsaeValidationInfo,
        val poomsae: String,
        val movements: List<Movement>,
    )
}
