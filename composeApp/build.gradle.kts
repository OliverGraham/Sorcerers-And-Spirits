import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val keystoreProperties =
    Properties().apply {
        val keystorePropertiesFile = rootProject.file("keystore.properties")
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use { load(it) }
        }
    }

fun signingProperty(key: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(key)

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.googleServices)
    alias(libs.plugins.firebaseCrashlytics)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.spotless)
    alias(libs.plugins.koin.compiler)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(dependencyNotation = compose.preview)
            implementation(dependencyNotation = libs.androidx.activity.compose)

            implementation(dependencyNotation = libs.androidx.credentials)
            implementation(dependencyNotation = libs.androidx.credentials.play.services.auth)
            implementation(dependencyNotation = libs.googleid)

            implementation(dependencyNotation = libs.androidx.media3.exoplayer)

            implementation(dependencyNotation = project.dependencies.platform(libs.firebase.bom))
            implementation(dependencyNotation = libs.firebase.crashlytics)

            implementation(dependencyNotation = libs.koin.android)
            implementation(dependencyNotation = libs.koin.androidx.compose)

            implementation(dependencyNotation = libs.ktor.client.okhttp)
        }
        androidUnitTest.dependencies {
            implementation(dependencyNotation = libs.kotlin.test)
            implementation(dependencyNotation = libs.kotlinx.coroutines.test)
            implementation(dependencyNotation = libs.mockk)
            implementation(dependencyNotation = libs.turbine)
        }
        androidInstrumentedTest.dependencies {
            implementation(dependencyNotation = project.dependencies.platform(libs.firebase.bom))
        }
        commonMain.dependencies {
            implementation(dependencyNotation = compose.runtime)
            implementation(dependencyNotation = compose.foundation)
            implementation(dependencyNotation = compose.material3)
            implementation(dependencyNotation = compose.ui)
            implementation(dependencyNotation = compose.components.resources)
            implementation(dependencyNotation = compose.components.uiToolingPreview)
            implementation(dependencyNotation = compose.preview)
            implementation(dependencyNotation = libs.androidx.datastore.preferences.core)
            implementation(dependencyNotation = libs.androidx.lifecycle.runtimeCompose)
            implementation(dependencyNotation = libs.androidx.lifecycle.viewmodelCompose)
            implementation(dependencyNotation = libs.androidx.lifecycle.viewmodel.navigation3)
            implementation(dependencyNotation = libs.androidx.navigation3.runtime)
            implementation(dependencyNotation = libs.androidx.navigation3.ui)

            implementation(dependencyNotation = libs.firebase.gitlive.app)
            implementation(dependencyNotation = libs.firebase.gitlive.auth)
            implementation(dependencyNotation = libs.firebase.gitlive.database)
            implementation(dependencyNotation = libs.firebase.gitlive.firestore)

            implementation(dependencyNotation = libs.kotlinx.serialization.json)

            implementation(dependencyNotation = libs.ktor.client.core)

            implementation(dependencyNotation = project.dependencies.platform(libs.koin.bom))
            implementation(dependencyNotation = libs.koin.annotations)
            implementation(dependencyNotation = libs.koin.compose)
            implementation(dependencyNotation = libs.koin.compose.viewmodel)
            implementation(dependencyNotation = libs.koin.core)
        }
        iosMain.dependencies {
            implementation(dependencyNotation = libs.ktor.client.darwin)
        }
        commonTest.dependencies {
            implementation(dependencyNotation = libs.kotlin.test)
        }
    }
}

android {
    namespace = "com.linklabs.sorcerersandspirits"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.linklabs.sorcerersandspirits"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    signingConfigs {
        create("release") {
            val releaseStoreFile = signingProperty("RELEASE_STORE_FILE")
            if (releaseStoreFile != null) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingProperty("RELEASE_PASSWORD")
                keyAlias = signingProperty("RELEASE_KEY_ALIAS")
                keyPassword = signingProperty("RELEASE_PASSWORD")
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            ndk {
                debugSymbolLevel = "FULL"
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            val releaseSigningConfig = signingConfigs.getByName("release")
            val isReleaseKeystoreConfigured = releaseSigningConfig.storeFile != null
            if (isReleaseKeystoreConfigured) {
                signingConfig = releaseSigningConfig
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint().editorConfigOverride(
            mapOf(
                "ktlint_standard_function-naming" to "disabled",
                "ktlint_standard_comment-wrapping" to "disabled",

            ),
        )
        suppressLintsFor {
            step = "ktlint"
            shortCode = "standard:max-line-length"
        }
        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target("*.gradle.kts")
        ktlint().editorConfigOverride(
            mapOf(
                "max_line_length" to "100",
            ),
        )
    }
}
