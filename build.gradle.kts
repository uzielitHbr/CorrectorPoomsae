// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.ktlint) apply false
}

tasks.register("detekt") {
    description = "Run detekt on all subprojects"
    group = "verification"
    dependsOn(":app:detekt", ":analysis:detekt")
}

tasks.register("ktlintCheck") {
    description = "Run ktlint check on all subprojects"
    group = "verification"
    dependsOn(":app:ktlintCheck", ":analysis:ktlintCheck")
}
