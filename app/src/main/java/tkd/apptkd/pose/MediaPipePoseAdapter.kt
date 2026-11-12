package tkd.apptkd.pose

import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.Landmark
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import java.util.concurrent.atomic.AtomicLong
import com.google.mediapipe.tasks.components.containers.Landmark as MpWorldLandmark
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark as MpImageLandmark

/**
 * Resultado de la adaptación de un cuadro de pose de MediaPipe.
 */
sealed interface PoseAdaptationResult {
    /** Cuadro adaptado correctamente; null si no se detectó ninguna persona en el cuadro. */
    data class Success(val frame: PoseFrame?) : PoseAdaptationResult

    /** Cuadro descartado por infracción de contrato temporal o datos corruptos. */
    data class Rejected(val reason: PoseDropReason) : PoseAdaptationResult
}

/**
 * Motivos de descarte de un cuadro de pose según la sección 12.9.1.
 */
enum class PoseDropReason {
    NEGATIVE_TIMESTAMP,
    NON_INCREASING_TIMESTAMP,
    CORRUPTED_COORDINATES,
}

/**
 * Adaptador puro y aislado que transforma resultados de MediaPipe en la entidad canónica [PoseFrame]
 * respetando el contrato de la sección 12.9.1 (timestamps estrictamente crecientes y línea de tiempo única).
 */
class MediaPipePoseAdapter(
    initialTimestampMs: Long = -1L,
) {
    private val lastTimestampMs = AtomicLong(initialTimestampMs)

    /** Adapta un resultado completo de [PoseLandmarkerResult]. */
    fun adapt(result: PoseLandmarkerResult): PoseAdaptationResult =
        adaptRaw(
            imageLandmarks = result.landmarks().firstOrNull(),
            worldLandmarks = result.worldLandmarks().firstOrNull(),
            timestampMs = result.timestampMs(),
        )

    /**
     * Adapta colecciones de articulaciones sin requerir el objeto contenedor de MediaPipe,
     * facilitando pruebas unitarias exhaustivas en JVM pura.
     */
    fun adaptRaw(
        imageLandmarks: List<MpImageLandmark>?,
        worldLandmarks: List<MpWorldLandmark>?,
        timestampMs: Long,
    ): PoseAdaptationResult {
        val timeError = checkTimestamp(timestampMs)
        if (timeError != null) return timeError

        return if (imageLandmarks.isNullOrEmpty() || worldLandmarks.isNullOrEmpty()) {
            lastTimestampMs.set(timestampMs)
            PoseAdaptationResult.Success(null)
        } else {
            extractFrames(imageLandmarks, worldLandmarks, timestampMs)
        }
    }

    private fun checkTimestamp(timestampMs: Long): PoseAdaptationResult? {
        val prev = lastTimestampMs.get()
        return when {
            timestampMs < 0L -> PoseAdaptationResult.Rejected(PoseDropReason.NEGATIVE_TIMESTAMP)
            prev != -1L && timestampMs <= prev -> PoseAdaptationResult.Rejected(PoseDropReason.NON_INCREASING_TIMESTAMP)
            else -> null
        }
    }

    private fun extractFrames(
        imageLandmarks: List<MpImageLandmark>,
        worldLandmarks: List<MpWorldLandmark>,
        timestampMs: Long,
    ): PoseAdaptationResult {
        val jointMaps = buildJointMaps(imageLandmarks, worldLandmarks)
        return if (jointMaps != null) {
            lastTimestampMs.set(timestampMs)
            PoseAdaptationResult.Success(
                PoseFrame(
                    timestampMs = timestampMs,
                    image = jointMaps.first,
                    world = jointMaps.second,
                ),
            )
        } else {
            PoseAdaptationResult.Rejected(PoseDropReason.CORRUPTED_COORDINATES)
        }
    }

    private fun buildJointMaps(
        imageLandmarks: List<MpImageLandmark>,
        worldLandmarks: List<MpWorldLandmark>,
    ): Pair<Map<Joint, Landmark>, Map<Joint, Landmark>>? {
        val imageMap = mutableMapOf<Joint, Landmark>()
        val worldMap = mutableMapOf<Joint, Landmark>()
        var isValid = true

        for (joint in Joint.entries) {
            val pair = extractJointPair(imageLandmarks, worldLandmarks, joint.mediaPipeIndex)
            if (pair != null) {
                imageMap[joint] = pair.first
                worldMap[joint] = pair.second
            } else {
                isValid = false
                break
            }
        }
        return if (isValid) imageMap to worldMap else null
    }

    private fun extractJointPair(
        imageLandmarks: List<MpImageLandmark>,
        worldLandmarks: List<MpWorldLandmark>,
        idx: Int,
    ): Pair<Landmark, Landmark>? =
        if (idx < imageLandmarks.size && idx < worldLandmarks.size) {
            val img = toLandmark(imageLandmarks[idx])
            val wrd = toLandmark(worldLandmarks[idx])
            if (img != null && wrd != null) img to wrd else null
        } else {
            null
        }

    private fun toLandmark(lm: MpImageLandmark): Landmark? {
        val x = lm.x()
        val y = lm.y()
        val z = lm.z()
        val vis = lm.visibility().orElse(1.0f)
        return if (isCoordinateValid(x, y, z)) Landmark(x, y, z, vis) else null
    }

    private fun toLandmark(lm: MpWorldLandmark): Landmark? {
        val x = lm.x()
        val y = lm.y()
        val z = lm.z()
        val vis = lm.visibility().orElse(1.0f)
        return if (isCoordinateValid(x, y, z)) Landmark(x, y, z, vis) else null
    }

    private fun isCoordinateValid(x: Float, y: Float, z: Float): Boolean =
        x.isFinite() && y.isFinite() && z.isFinite()

    /** Restablece el contador temporal (e.g. al reiniciar la sesión). */
    fun reset() {
        lastTimestampMs.set(-1L)
    }
}
