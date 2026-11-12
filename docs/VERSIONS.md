# Catálogo Oficial de Versiones de Dependencias (docs/VERSIONS.md)

**Proyecto:** Taegeuk Coach  
**Fecha de consulta y verificación:** 11 de noviembre de 2026  
**Reglas asociadas:** Regla **R-01** (no inventar versiones) y Regla **R-03** (verificar antes de usar).  
**Repositorios oficiales consultados:** Google Maven Repository (`dl.google.com/dl/android/maven2/`), Maven Central (`repo1.maven.org/maven2/` / `search.maven.org`), Android Developers Release Notes y GitHub Releases oficiales.

---

## 1. Principio Normativo de Versiones

- No se utilizan versiones en estado «alpha» o «beta» para componentes críticos de producción.
- No se utiliza `latest.release` en ninguna parte de la configuración de Gradle, garantizando compilaciones 100% deterministas y reproducibles.
- Todas las dependencias se gestionarán centralizadamente a través del catálogo de versiones `gradle/libs.versions.toml`.

---

## 2. Dependencias Principales y Verificación

| Dependencia | Grupo / Artefacto | Versión Estable Verificada | Repositorio / URL Oficial de Consulta | Fecha Verificación |
| :--- | :--- | :---: | :--- | :---: |
| **Android Gradle Plugin** | `com.android.tools.build:gradle` | `9.3.3` | Existente en proyecto / Google Maven | 2026-11-11 |
| **Kotlin Compiler** | `org.jetbrains.kotlin:kotlin-gradle-plugin` | `2.2.10` | Existente en proyecto / JetBrains | 2026-11-11 |
| **Compose BOM** | `androidx.compose:compose-bom` | `2026.02.01` | Existente en proyecto / Google Maven | 2026-11-11 |
| **MediaPipe Tasks Vision** | `com.google.mediapipe:tasks-vision` | `1.2.2` | Maven Central (`https://search.maven.org/artifact/com.google.mediapipe/tasks-vision`) | 2026-11-11 |
| **CameraX Core** | `androidx.camera:camera-core` | `1.6.2` | Google Maven (`https://developer.android.com/training/camerax`) | 2026-11-11 |
| **CameraX Camera2** | `androidx.camera:camera-camera2` | `1.6.2` | Google Maven (`https://developer.android.com/training/camerax`) | 2026-11-11 |
| **CameraX Lifecycle** | `androidx.camera:camera-lifecycle` | `1.6.2` | Google Maven (`https://developer.android.com/training/camerax`) | 2026-11-11 |
| **CameraX View** | `androidx.camera:camera-view` | `1.6.2` | Google Maven (`https://developer.android.com/training/camerax`) | 2026-11-11 |
| **CameraX Video** | `androidx.camera:camera-video` | `1.6.2` | Google Maven (`https://developer.android.com/training/camerax`) | 2026-11-11 |
| **Room Runtime & KTX** | `androidx.room:room-runtime`, `room-ktx` | `2.8.5` | Google Maven (`https://developer.android.com/training/data-storage/room`) | 2026-11-11 |
| **Room Compiler** | `androidx.room:room-compiler` | `2.8.5` | Google Maven (`https://developer.android.com/training/data-storage/room`) | 2026-11-11 |
| **DataStore Preferences** | `androidx.datastore:datastore-preferences` | `1.3.0` | Google Maven (`https://developer.android.com/topic/libraries/architecture/datastore`) | 2026-11-11 |
| **Dagger Hilt** | `com.google.dagger:hilt-android`, `hilt-compiler` | `2.60.1` | Maven Central (`https://github.com/google/dagger/releases`) | 2026-11-11 |
| **Hilt Navigation Compose** | `androidx.hilt:hilt-navigation-compose` | `1.4.0` | Google Maven (`https://developer.android.com/jetpack/androidx/releases/hilt`) | 2026-11-11 |
| **Navigation Compose** | `androidx.navigation:navigation-compose` | `2.10.2` | Google Maven (`https://developer.android.com/jetpack/androidx/releases/navigation`) | 2026-11-11 |
| **Kotlinx Serialization JSON** | `org.jetbrains.kotlinx:kotlinx-serialization-json` | `1.9.0` | Maven Central (`https://github.com/Kotlin/kotlinx.serialization/releases`, alineada a Kotlin 2.2.10) | 2026-11-12 |
| **Core KTX** | `androidx.core:core-ktx` | `1.10.1` | Existente en proyecto / Google Maven | 2026-11-11 |
| **Lifecycle Runtime KTX** | `androidx.lifecycle:lifecycle-runtime-ktx` | `2.6.1` | Existente en proyecto / Google Maven | 2026-11-11 |
| **Activity Compose** | `androidx.activity:activity-compose` | `1.8.0` | Existente en proyecto / Google Maven | 2026-11-11 |

---

## 3. Bibliotecas de Pruebas y Análisis de Calidad

| Dependencia | Grupo / Artefacto | Versión Estable Verificada | Repositorio / URL Oficial de Consulta | Fecha Verificación |
| :--- | :--- | :---: | :--- | :---: |
| **JUnit** | `junit:junit` | `4.13.2` | Maven Central (`https://junit.org/junit4/`) | 2026-11-11 |
| **MockK** | `io.mockk:mockk` | `1.14.11` | Maven Central (`https://mockk.io/`) | 2026-11-11 |
| **Turbine** | `app.cash.turbine:turbine` | `1.2.1` | Maven Central (`https://github.com/cashapp/turbine`) | 2026-11-11 |
| **Kotlinx Coroutines Test** | `org.jetbrains.kotlinx:kotlinx-coroutines-test` | `1.10.2` | Maven Central (`https://github.com/Kotlin/kotlinx.coroutines`) | 2026-11-11 |
| **AndroidX Test JUnit** | `androidx.test.ext:junit` | `1.1.5` | Existente en proyecto / Google Maven | 2026-11-11 |
| **Espresso Core** | `androidx.test.espresso:espresso-core` | `3.5.1` | Existente en proyecto / Google Maven | 2026-11-11 |
| **Detekt** | `io.gitlab.arturbosch.detekt` | `1.23.8` | Gradle Plugin Portal (`https://detekt.dev/`) | 2026-11-11 |
| **ktlint** | `com.pinterest.ktlint` | `1.5.0` | Maven Central / GitHub (`https://pinterest.github.io/ktlint/`) | 2026-11-11 |

