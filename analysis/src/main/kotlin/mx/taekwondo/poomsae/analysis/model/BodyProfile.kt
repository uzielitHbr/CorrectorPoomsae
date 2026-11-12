package mx.taekwondo.poomsae.analysis.model

import mx.taekwondo.poomsae.analysis.geometry.Vec3

/**
 * Perfil antropométrico y base de orientación espacial normalizada del usuario (Sección 13.3).
 *
 * Se obtiene durante la calibración inicial estática erguida y permite evaluar longitudes
 * normalizadas de pasos, técnicas y rumbos de forma invariante a la estatura del atleta.
 *
 * @property lThigh Longitud media del muslo (metros).
 * @property lShank Longitud media de la pantorrilla (metros).
 * @property lLeg Longitud total de la pierna (`lThigh + lShank`, metros).
 * @property torsoLength Longitud del torso en espacio métrico (T, metros).
 * @property shoulderWidth Ancho biacromial entre hombros (W, metros).
 * @property armLength Longitud total del brazo extendido (A, metros).
 * @property dNoseShoulder Distancia entre la nariz y los hombros (metros).
 * @property upVector Vector unitario vertical hacia arriba estimado del cuerpo (u).
 * @property forwardSign Signo de orientación de avance corporal (+1 o -1, sigma).
 * @property referenceForward Vector frontal de referencia en posición inicial (F_0).
 * @property referenceHipAxis Vector horizontal del eje de caderas en posición inicial (r_0 = h_hat_g).
 * @property tImgMedian Altura del torso en coordenadas de imagen (proporción normalizada).
 */
data class BodyProfile(
    val lThigh: Double,
    val lShank: Double,
    val lLeg: Double,
    val torsoLength: Double,
    val shoulderWidth: Double,
    val armLength: Double,
    val dNoseShoulder: Double,
    val upVector: Vec3,
    val forwardSign: Double,
    val referenceForward: Vec3,
    val referenceHipAxis: Vec3,
    val tImgMedian: Double,
)
