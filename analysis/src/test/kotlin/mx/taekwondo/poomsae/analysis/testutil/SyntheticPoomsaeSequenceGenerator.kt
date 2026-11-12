package mx.taekwondo.poomsae.analysis.testutil

import mx.taekwondo.poomsae.analysis.geometry.Vec3
import mx.taekwondo.poomsae.analysis.model.Joint
import mx.taekwondo.poomsae.analysis.model.PoseFrame
import mx.taekwondo.poomsae.analysis.poomsae.EvaluationMode
import mx.taekwondo.poomsae.analysis.poomsae.Movement
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeDefinition
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeLoadResult
import mx.taekwondo.poomsae.analysis.poomsae.PoomsaeLoader
import mx.taekwondo.poomsae.analysis.poomsae.Side
import java.io.File
import kotlin.math.cos
import kotlin.math.sin

/**
 * Generador determinista de secuencias sintéticas ruidosas de cuadros de pose para pruebas
 * de segmentación temporal y secuenciación del poomsae completo (Sección 12.4).
 */
object SyntheticPoomsaeSequenceGenerator {
    private const val FRAME_INTERVAL_MS = 33L // ~30 FPS
    private const val AWAIT_FRAME_COUNT = 6
    private const val HOLD_FRAME_COUNT = 26

    fun loadCanonicalDefinition(): PoomsaeDefinition {
        val candidates =
            listOf(
                File("poomsae_taegeuk1.json"),
                File("../poomsae_taegeuk1.json"),
                File("../../poomsae_taegeuk1.json"),
            )
        val file =
            candidates.firstOrNull { it.exists() && it.isFile }
                ?: error("No se encontró el archivo poomsae_taegeuk1.json")
        val result = PoomsaeLoader.loadFromString(file.readText(Charsets.UTF_8))
        return (result as PoomsaeLoadResult.Success).definition
    }

    /**
     * Genera la secuencia completa de cuadros para los 18 movimientos de Taegeuk 1.
     */
    fun generateFull18MovementSequence(definition: PoomsaeDefinition): List<PoseFrame> {
        val allFrames = mutableListOf<PoseFrame>()
        var currentTimestamp = 1000L

        val headingMap = definition.headingsByMovement.associateBy { it.movementIndex }
        for (movement in definition.movements) {
            val heading = headingMap[movement.index]?.cumulativeHeadingDeg ?: 0.0
            val movFrames =
                generateMovementFrames(
                    movement = movement,
                    startTimestampMs = currentTimestamp,
                    headingDeg = heading,
                )
            allFrames.addAll(movFrames)
            currentTimestamp = movFrames.last().timestampMs + FRAME_INTERVAL_MS
        }
        return allFrames
    }

    /**
     * Genera los cuadros sintéticos de un movimiento individual atravesando espera, acción y sostén.
     */
    fun generateMovementFrames(
        movement: Movement,
        startTimestampMs: Long,
        headingDeg: Double = 0.0,
    ): List<PoseFrame> {
        val frames = mutableListOf<PoseFrame>()
        var t = startTimestampMs

        val baseSkeleton = SkeletonBuilder()
        applyHeading(baseSkeleton, headingDeg)

        // 1. Fase de espera (AwaitingMovement)
        repeat(AWAIT_FRAME_COUNT) {
            frames.add(baseSkeleton.clone().build(t))
            t += FRAME_INTERVAL_MS
        }

        // 2. Fase de movimiento activo (InMovement)
        val finalSkeleton = buildFinalSkeleton(movement, headingDeg)
        val isKick = movement.evaluationMode == EvaluationMode.PEAK_THEN_HOLD
        val motionStepCount = if (isKick) 10 else 8

        for (step in 1..motionStepCount) {
            val progress = step.toDouble() / motionStepCount
            val movingSkeleton = buildMovingSkeleton(movement, headingDeg, progress, isKick)
            frames.add(movingSkeleton.build(t))
            t += FRAME_INTERVAL_MS
        }

        // 3. Fase de sostén (Holding)
        repeat(HOLD_FRAME_COUNT) {
            val holdSkeleton = finalSkeleton.clone()
            frames.add(holdSkeleton.build(t))
            t += FRAME_INTERVAL_MS
        }

        return frames
    }

    private fun buildFinalSkeleton(
        movement: Movement,
        headingDeg: Double,
    ): SkeletonBuilder {
        val skeleton = SkeletonBuilder()
        applyHeading(skeleton, headingDeg)
        for (joint in movement.activeJoints) {
            when (joint) {
                Joint.LEFT_WRIST, Joint.RIGHT_WRIST -> {
                    val baseX = if (joint == Joint.RIGHT_WRIST) 0.20 else -0.20
                    skeleton.setJointPosition(joint, Vec3(baseX + 0.20, 0.25, 0.20))
                }
                Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE -> {
                    val baseX = if (joint == Joint.RIGHT_ANKLE) 0.12 else -0.12
                    skeleton.setJointPosition(joint, Vec3(baseX + 0.15, -0.90, 0.20))
                }
                else -> Unit
            }
        }
        return skeleton
    }

    private fun buildMovingSkeleton(
        movement: Movement,
        headingDeg: Double,
        progress: Double,
        isKick: Boolean,
    ): SkeletonBuilder {
        val skeleton = SkeletonBuilder()
        applyHeading(skeleton, headingDeg)

        val kickLegRight = movement.technique.arm == Side.RIGHT
        val kickAnkle = if (kickLegRight) Joint.RIGHT_ANKLE else Joint.LEFT_ANKLE

        for (joint in movement.activeJoints) {
            when (joint) {
                Joint.LEFT_WRIST, Joint.RIGHT_WRIST -> {
                    val baseX = if (joint == Joint.RIGHT_WRIST) 0.20 else -0.20
                    skeleton.setJointPosition(
                        joint,
                        Vec3(baseX + progress * 0.20, -0.10 + progress * 0.35, 0.20),
                    )
                }
                Joint.LEFT_ANKLE, Joint.RIGHT_ANKLE -> {
                    val baseX = if (joint == Joint.RIGHT_ANKLE) 0.12 else -0.12
                    if (isKick && joint == kickAnkle) {
                        val peakRatio = sin(progress * Math.PI)
                        val yElevation = -0.90 + peakRatio * 1.30
                        skeleton.setJointPosition(joint, Vec3(baseX, yElevation, 0.30))
                    } else {
                        skeleton.setJointPosition(joint, Vec3(baseX + progress * 0.15, -0.90, progress * 0.20))
                    }
                }
                else -> Unit
            }
        }
        return skeleton
    }

    private fun applyHeading(
        skeleton: SkeletonBuilder,
        headingDeg: Double,
    ) {
        val rad = Math.toRadians(-headingDeg)
        val cosH = cos(rad)
        val sinH = sin(rad)
        val fwd = Vec3(-sinH, 0.0, cosH)
        skeleton.setJointPosition(Joint.NOSE, Vec3(fwd.x * 0.10, 0.70, fwd.z * 0.10))
    }
}
