plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.github.diiafyra.motoman.core.data"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = true
    }
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

dependencies {
    implementation(project(":core:domain"))

    testImplementation(libs.junit)
}
