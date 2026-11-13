package tkd.apptkd.video

import mx.taekwondo.poomsae.analysis.config.AnalysisConfig
import kotlin.math.roundToInt

/**
 * Utilidad pura para el cálculo de dimensiones con reducción de escala previa (bounded memory).
 */
object VideoFrameScaler {

    fun computeScaledDimensions(
        width: Int,
        height: Int,
        targetMaxDim: Int = AnalysisConfig.VIDEO_DOWNSCALE_TARGET_MAX_DIM,
    ): Pair<Int, Int> {
        val maxDim = maxOf(width, height)
        return when {
            width <= 0 || height <= 0 -> Pair(targetMaxDim, targetMaxDim)
            maxDim <= targetMaxDim -> Pair(width, height)
            else -> {
                val scale = targetMaxDim.toDouble() / maxDim.toDouble()
                Pair(
                    (width * scale).roundToInt().coerceAtLeast(1),
                    (height * scale).roundToInt().coerceAtLeast(1),
                )
            }
        }
    }
}
