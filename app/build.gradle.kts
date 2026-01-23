import com.android.Version
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Load keystore properties
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    packaging {
        resources {
            pickFirsts += mutableSetOf(
                "META-INF/LICENSE*",
                "META-INF/NOTICE*"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = false
    }
    kotlinOptions {
        jvmTarget = "17"
        // Kotlin compiler optimizations
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
        // Disable unused features for faster builds
        aidl = false
        renderScript = false
        shaders = false
    }

    namespace = "info.meuse24.smsforwarderneoA1"
    compileSdk = 35

    signingConfigs {
        create("release") {
            if (keystoreProperties.containsKey("storeFile")) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    defaultConfig {
        applicationId = "info.meuse24.smsforwarderneoA1"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "Barracuda"

        val agpVersion = Version.ANDROID_GRADLE_PLUGIN_VERSION
        buildConfigField("String", "AGP_VERSION", "\"$agpVersion\"")
        buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")

        // Compose Version aus dem Version Catalog
        buildConfigField("String", "COMPOSE_VERSION", "\"${libs.versions.composeBom.get()}\"")

        // SDK Versions
        buildConfigField("int", "COMPILE_SDK", "${android.compileSdk}")

        // Library Versions aus dem Version Catalog
        buildConfigField("String", "LIBPHONENUMBER_VERSION", "\"${libs.versions.libphonenumber.get()}\"")
        buildConfigField("String", "NAVIGATION_VERSION", "\"${libs.versions.navigationCompose.get()}\"")
        buildConfigField("String", "SECURITY_CRYPTO_VERSION", "\"${libs.versions.securityCrypto.get()}\"")
        buildConfigField("String", "JAVAMAIL_VERSION", "\"${libs.versions.javamail.get()}\"")
        buildConfigField("String", "LIFECYCLE_VERSION", "\"${libs.versions.lifecycleRuntimeKtx.get()}\"")
        buildConfigField("String", "COMPOSE_ICONS_VERSION", "\"${libs.versions.composeIcons.get()}\"")
        buildConfigField("String", "CORE_KTX_VERSION", "\"${libs.versions.coreKtx.get()}\"")

        buildConfigField("String", "JDK_VERSION", "\"${System.getProperty("java.version")}\"")
        buildConfigField("String", "BUILD_TOOLS_VERSION", "\"${android.buildToolsVersion}\"")
        buildConfigField("String", "CMAKE_VERSION", "\"${project.findProperty("cmake.version") ?: "not used"}\"")
        buildConfigField("String", "NDK_VERSION", "\"${project.findProperty("android.ndkVersion") ?: "not used"}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // Build time: Use dynamic timestamp when built from command line (./build.sh)
            // Use static value for Android Studio incremental builds (better caching)
            val useDynamicBuildTime = project.hasProperty("dynamicBuildTime") &&
                                      project.property("dynamicBuildTime") == "true"
            val buildTime = if (useDynamicBuildTime) {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.format(Date())
            } else {
                "dev-build (use ./build.sh for timestamp)"
            }
            buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
            buildConfigField("String", "GRADLE_VERSION", "\"${gradle.gradleVersion}\"")
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")

            // Dialog consolidation feature flag
            buildConfigField("boolean", "USE_NEW_DIALOGS", "true")

            // Debug optimizations
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
        }
        release {
            // Enable R8 optimization for release builds
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            // Apply signing configuration
            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Build time generated at build time for release builds
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            buildConfigField("String", "BUILD_TIME", "\"${sdf.format(Date())}\"")
            buildConfigField("String", "GRADLE_VERSION", "\"${gradle.gradleVersion}\"")
            buildConfigField("String", "BUILD_TYPE", "\"release\"")

            // Dialog consolidation feature flag (enabled - migration complete)
            buildConfigField("boolean", "USE_NEW_DIALOGS", "true")
        }
    }

    // Lint options - strict for release builds
    lint {
        checkReleaseBuilds = true
        abortOnError = true
        warningsAsErrors = false
        // Baseline file for tracking existing issues
        baseline = file("lint-baseline.xml")
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// BUILD_TIME behavior:
// - Debug builds: Static "dev-build" in Android Studio for better caching
//                 Dynamic timestamp when built via ./build.sh (sets -PdynamicBuildTime=true)
// - Release builds: Always dynamic timestamp
// This balances Android Studio performance with accurate build information

dependencies {
    // Core Android & Kotlin
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.coil.compose)
    implementation(libs.compose.icons.core)
    implementation(libs.compose.icons.extended)

    // Navigation
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)

    // Security & Phone Utils
    implementation(libs.androidx.security.crypto)
    implementation(libs.libphonenumber)

    // Email
    implementation(libs.android.mail)
    implementation(libs.android.activation)

    // Logging
    implementation(libs.timber)

    // Baseline Profile for improved startup performance
    implementation(libs.androidx.profileinstaller)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    // Debug
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


}


