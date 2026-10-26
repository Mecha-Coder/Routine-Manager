import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Release signing (docs/INSTALL.md): the key file is never committed, and
// routine-manager/keystore.properties (gitignored) holds its path, alias and passwords.
val signingFile: File = rootProject.file("keystore.properties")
val signing = Properties().apply { if (signingFile.exists()) signingFile.inputStream().use { load(it) } }

android {
    namespace = "com.jsk.routinemanager"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.jsk.routinemanager"
        minSdk = 31
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = signing.getProperty("storeFile")?.let { file(it) }
            storePassword = signing.getProperty("storePassword")
            keyAlias = signing.getProperty("keyAlias")
            keyPassword = signing.getProperty("keyPassword")
        }
    }

    buildTypes {
        debug {
            // Installs beside the real app and never touches its data (SPEC D13).
            applicationIdSuffix = ".debug"
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            optimization {
                // No packageScope (experimental in AGP 9): with the wizard's
                // setOf("androidx.**", "kotlin.**", "kotlinx.**") the app crashed at start (M12 notes).
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

// Only a release build needs the key: without the file it stops here with a pointer to the
// guide instead of AGP's "Keystore file not set". Debug builds and lint don't run this task.
tasks.named { it == "validateSigningRelease" }.configureEach {
    val missing = if (signingFile.exists()) null else signingFile.path
    doFirst {
        if (missing != null) {
            throw GradleException("Release signing isn't set up: $missing is missing. See docs/INSTALL.md, step 1.")
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}