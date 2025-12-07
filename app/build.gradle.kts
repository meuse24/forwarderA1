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
    compileSdk = 36

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
        versionCode = 3
        versionName = "Anchovy"

        val agpVersion = Version.ANDROID_GRADLE_PLUGIN_VERSION
        buildConfigField("String", "AGP_VERSION", "\"$agpVersion\"")
        buildConfigField("String", "KOTLIN_VERSION", "\"${libs.versions.kotlin.get()}\"")

        // Compose Version aus dem Version Catalog
        buildConfigField("String", "COMPOSE_VERSION", "\"${libs.versions.composeBom.get()}\"")

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
            // Static build time for debug builds to enable build cache
            buildConfigField("String", "BUILD_TIME", "\"dev-build\"")
            buildConfigField("String", "GRADLE_VERSION", "\"${gradle.gradleVersion}\"")
            buildConfigField("String", "BUILD_TYPE", "\"debug\"")

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
        }
    }

    // Duplicate buildFeatures block removed - already defined at line 30-33
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

// Removed: Task override that disabled BuildConfig caching
// Debug builds now use static BUILD_TIME for better caching
// Release builds still generate BUILD_TIME dynamically

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.appcompat)
    // Removed: androidx.room.common - not used in codebase
    implementation(libs.androidx.security.crypto)
    implementation(libs.libphonenumber)
    implementation(libs.androidx.espresso.core)
    implementation(libs.material)
    implementation(libs.android.mail)
    implementation(libs.android.activation)
    implementation(libs.compose.icons.core)
    implementation(libs.compose.icons.extended)
    // Removed: androidx.datastore.core.android - not used in codebase

    // Animation & Visual Effects
    implementation("com.airbnb.android:lottie-compose:6.7.1")

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


}


