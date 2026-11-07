// Pure Kotlin on purpose: the logic most likely to be wrong (fuel economy, maintenance due dates)
// runs as plain JVM tests, and the compiler stops Android types from leaking in.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
    explicitApi()
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
