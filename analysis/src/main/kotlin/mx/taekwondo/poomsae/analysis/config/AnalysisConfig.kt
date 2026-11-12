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
}
