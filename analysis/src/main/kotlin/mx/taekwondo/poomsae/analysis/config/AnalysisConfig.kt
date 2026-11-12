package mx.taekwondo.poomsae.analysis.config

/**
 * Calibration lifecycle status of an [AnalysisConfig] parameter (Rule R-05).
 */
enum class ParameterCalibrationStatus {
    /** Parameter uses provisional development value pending real-data calibration in Phase F12. */
    PENDIENTE_CALIBRAR,

    /** Parameter is calibrated from an empirical dataset recorded in VALIDATION_REPORT.md. */
    CALIBRADO,
}

/**
 * Overall validation state displayed by the application and exported reports (Section 16.5).
 */
enum class ValidationState {
    /** No real calibration or sequence is provisional; displayed as experimental mode. */
    NO_VALIDADO,

    /** Calibrated with a preliminary or small dataset without blind teacher evaluation. */
    VALIDACION_PRELIMINAR,

    /** Meets the minimum sample criteria documented with advisor and coach. */
    VALIDADO_ALCANCE_LIMITADO,
}

/**
 * Centralized configuration of numerical tolerances, thresholds, and filter parameters
 * for the `:analysis` module (Rules R-05, R-21, and Section 16.4).
 *
 * No code outside this file should embed magic numbers for tolerances, thresholds, or filter tuning.
 */
object AnalysisConfig {
    /** Configuration identifier persisted alongside session metadata. */
    const val CONFIG_VERSION: String = "0.2.0-f2"

    /** Current validation state of thresholds and scoring bands (Section 16.5). */
    val VALIDATION_STATE: ValidationState = ValidationState.NO_VALIDADO

    /** Global calibration status of the current parameter set. */
    val CALIBRATION_STATUS: ParameterCalibrationStatus = ParameterCalibrationStatus.PENDIENTE_CALIBRAR

    // -------------------------------------------------------------------------
    // Numerical Comparison Tolerances (EPS_*) — Pure mathematical invariants
    // -------------------------------------------------------------------------

    /** Minimum Euclidean vector norm (meters or dimensionless) below which a vector is degenerate. */
    const val EPS_NORM: Double = 1e-6

    /** Angular comparison tolerance in degrees for test verification and boundary checks. */
    const val EPS_ANGLE_DEG: Double = 1e-4

    /** Generic scalar comparison tolerance for floating-point arithmetic. */
    const val EPS_SCALAR: Double = 1e-6

    /** Dimensionless ratio comparison tolerance for invariance and metric verification. */
    const val EPS_RATIO: Double = 1e-4

    /** Minimum dot product magnitude threshold to determine unambiguous orientation sign. */
    const val EPS_DOT: Double = 1e-6

    // -------------------------------------------------------------------------
    // Quality & Visibility Thresholds — PENDIENTE_CALIBRAR (F6 / F12)
    // -------------------------------------------------------------------------

    /**
     * Minimum MediaPipe landmark visibility score `[0.0, 1.0]` required to include a joint
     * in geometric evaluation.
     * Status: PENDIENTE_CALIBRAR (calibrated empirically from Experiment E3 / F6).
     */
    const val VIS_MIN: Float = 0.5f

    /**
     * Minimum number of frames required inside an evaluation window (Section 13.6).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val MIN_WINDOW_FRAMES: Int = 5

    // -------------------------------------------------------------------------
    // Signal Smoothing Parameters — PENDIENTE_CALIBRAR (F6 / F12)
    // -------------------------------------------------------------------------

    /**
     * One Euro filter minimum cutoff frequency (Hz) applied at zero/low joint velocity to suppress jitter.
     * Unit: Hz. Status: PENDIENTE_CALIBRAR.
     */
    const val ONE_EURO_MIN_CUTOFF_HZ: Double = 1.0

    /**
     * One Euro filter speed coefficient controlling cutoff increase during fast movements (kicks/punches).
     * Unit: 1 / (meter or normalized coordinate unit). Status: PENDIENTE_CALIBRAR.
     */
    const val ONE_EURO_BETA: Double = 1.5

    /**
     * One Euro filter derivative cutoff frequency (Hz) for smoothing joint velocity estimates.
     * Unit: Hz. Status: PENDIENTE_CALIBRAR.
     */
    const val ONE_EURO_D_CUTOFF_HZ: Double = 1.0

    /**
     * Default smoothing factor alpha in `(0.0, 1.0]` for Exponential Moving Average filtering.
     * Unit: dimensionless. Status: PENDIENTE_CALIBRAR.
     */
    const val EMA_DEFAULT_ALPHA: Double = 0.3

    /** Milliseconds per second conversion factor. */
    const val MILLIS_PER_SECOND: Double = 1000.0

    // -------------------------------------------------------------------------
    // Frame Quality Gate Parameters — PENDIENTE_CALIBRAR (F6)
    // -------------------------------------------------------------------------

    /**
     * Minimum normalized distance margin `[0.0, 0.5]` from image borders for key joints.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val FRAME_MARGIN_RATIO: Double = 0.05

    /**
     * Minimum torso height ratio relative to image frame height (detects person too far).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val TORSO_MIN_HEIGHT_RATIO: Double = 0.15

    /**
     * Maximum torso height ratio relative to image frame height (detects person too close).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val TORSO_MAX_HEIGHT_RATIO: Double = 0.65

    // -------------------------------------------------------------------------
    // Body Profile Calibration Parameters — PENDIENTE_CALIBRAR (F6 / F12)
    // -------------------------------------------------------------------------

    /**
     * Minimum consecutive stable frames required to complete user calibration (Section 13.3).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val CAL_MIN_FRAMES: Int = 15

    /**
     * Maximum relative median absolute deviation (MAD / median) allowed across body segments.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val CAL_MAX_REL_MAD: Double = 0.15

    /**
     * Maximum relative variation of hip axis length against calibrated distance.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val HIP_AXIS_REL_TOL: Double = 0.35

    // -------------------------------------------------------------------------
    // Evaluability Model & Scoring Weights — PENDIENTE_CALIBRAR (F7 / Section 13.7)
    // -------------------------------------------------------------------------

    /** Coverage threshold of total metric weights to classify a movement as EVALUABLE. */
    const val FULL_COVERAGE: Double = 0.80

    /** Minimum coverage threshold of total metric weights to classify as PARTIALLY_EVALUABLE. */
    const val MIN_COVERAGE: Double = 0.50

    /** Weight assigned to critical biomechanical metrics. Status: PENDIENTE_CALIBRAR. */
    const val WEIGHT_CRITICAL: Double = 2.0

    /** Weight assigned to secondary biomechanical metrics. Status: PENDIENTE_CALIBRAR. */
    const val WEIGHT_SECONDARY: Double = 1.0

    /** Flag indicating whether partially evaluable movements count toward global score. */
    const val INCLUDE_PARTIAL_IN_GLOBAL: Boolean = false

    /** Minimum score threshold for category CORRECT. Status: PENDIENTE_CALIBRAR. */
    const val T_CORRECT: Double = 75.0

    /** Minimum score threshold for category IMPROVABLE (below is INCORRECT). Status: PENDIENTE_CALIBRAR. */
    const val T_IMPROVABLE: Double = 50.0

    /** Maximum allowed heading jump between consecutive frames before invalidating (degrees). */
    const val PSI_JUMP_DEG: Double = 60.0

    /** Minimum horizontal projection ratio of punch vector relative to arm length (AR-05). */
    const val MIN_HORIZ_RATIO: Double = 0.20

    // -------------------------------------------------------------------------
    // Motion Segmentation & Temporal State Machine — PENDIENTE_CALIBRAR (F8 / Section 12.3)
    // -------------------------------------------------------------------------

    /**
     * Minimum joint speed threshold (in torso heights per second) to trigger movement start.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val V_START: Double = 0.80

    /**
     * Minimum duration (milliseconds) joint speed must stay above V_START to confirm movement start.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_START_MS: Long = 100L

    /**
     * Lockout window (milliseconds) following movement evaluation before next start can be detected,
     * preventing movement deceleration from triggering false starts.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_LOCKOUT_MS: Long = 400L

    /**
     * Maximum joint speed threshold (in torso heights per second) below which a joint is considered still.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val V_STILL: Double = 0.25

    /**
     * Minimum continuous duration (milliseconds) joint speed must remain below V_STILL to confirm hold/completion.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_HOLD_MS: Long = 600L

    /**
     * Duration (milliseconds) of evaluation window at the tail of the hold period used for static metrics median.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_EVAL_WINDOW_MS: Long = 300L

    /**
     * Half-width of temporal window around kick peak (± milliseconds) for kick metric extraction.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_PEAK_WINDOW_MS: Long = 150L

    /**
     * Required duration (milliseconds) in stable junbi seogi ready posture before starting session.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_READY_MS: Long = 1000L

    /**
     * Inactivity timeout (milliseconds) waiting for movement start before prompting user about omission (SC-13).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_MOVEMENT_TIMEOUT_MS: Long = 8000L

    /**
     * Maximum duration (milliseconds) for a brief tracking loss before treating it as prolonged loss (SC-06/SC-07).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_LOSS_SHORT_MS: Long = 1000L

    /**
     * Required duration (milliseconds) of recovered stable frames before resuming session after tracking loss.
     * Status: PENDIENTE_CALIBRAR.
     */
    const val T_RECOVER_MS: Long = 1500L

    /**
     * Maximum allowed attempts in strict practice mode before marking movement failed (SC-11).
     * Status: PENDIENTE_CALIBRAR.
     */
    const val STRICT_MAX_ATTEMPTS: Int = 3
}
