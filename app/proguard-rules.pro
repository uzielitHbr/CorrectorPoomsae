# ==============================================================================
# Taegeuk Coach — Reglas de Optimización y Conservación ProGuard/R8 (Fase F13)
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. MediaPipe Pose Landmarker & Tasks Vision
# ------------------------------------------------------------------------------
-keep class com.google.mediapipe.tasks.vision.poselandmarker.** { *; }
-keep class com.google.mediapipe.tasks.vision.core.** { *; }
-keep class com.google.mediapipe.tasks.core.** { *; }
-keep class com.google.mediapipe.framework.image.** { *; }
-keep class com.google.mediapipe.framework.** { *; }
-keepnames class com.google.mediapipe.** { *; }
-dontwarn com.google.mediapipe.proto.**
-dontwarn com.google.mediapipe.framework.**

# Mantener métodos nativos (JNI) de MediaPipe y TensorFlow Lite
-keepclasseswithmembernames class * {
    native <methods>;
}

# ------------------------------------------------------------------------------
# 2. kotlinx.serialization
# ------------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt

-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable class *;
}
-keepclassmembers class **$$serializer {
    public static final **$$serializer INSTANCE;
}
-keepclasseswithmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-keepclassmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Conservación explícita de DTOs JSON del proyecto
-keep class tkd.apptkd.data.room.**JsonContainer { *; }
-keep class mx.taekwondo.poomsae.analysis.poomsae.model.** { *; }

# ------------------------------------------------------------------------------
# 3. AndroidX Room Database
# ------------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep class * extends androidx.room.migration.Migration
-keep class * extends androidx.room.migration.AutoMigrationSpec
-dontwarn androidx.room.paging.**

# ------------------------------------------------------------------------------
# 4. Módulo de Análisis Cinemático (:analysis)
# ------------------------------------------------------------------------------
-keep class mx.taekwondo.poomsae.analysis.config.AnalysisConfig { *; }
-keep class mx.taekwondo.poomsae.analysis.config.ValidationState { *; }
-keep class mx.taekwondo.poomsae.analysis.config.ParameterCalibrationStatus { *; }
-keep class mx.taekwondo.poomsae.analysis.config.ValidationStateEvaluator { *; }
-keep class mx.taekwondo.poomsae.analysis.geometry.Joint { *; }
-keep class mx.taekwondo.poomsae.analysis.geometry.PoseFrame { *; }
-keep class mx.taekwondo.poomsae.analysis.geometry.Landmark { *; }
-keep class mx.taekwondo.poomsae.analysis.geometry.Vec3 { *; }
-keep class mx.taekwondo.poomsae.analysis.geometry.Vec2 { *; }
-keep class mx.taekwondo.poomsae.analysis.evaluation.** { *; }
-keep class mx.taekwondo.poomsae.analysis.segmentation.** { *; }

# ------------------------------------------------------------------------------
# 5. Jetpack DataStore & Hilt
# ------------------------------------------------------------------------------
-keep class * extends androidx.datastore.preferences.core.Preferences
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

