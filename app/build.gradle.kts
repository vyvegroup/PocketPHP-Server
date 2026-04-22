plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.phpserver.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.phpserver.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            val storeFilePath = System.getenv("KEYSTORE_PATH") ?: "../release.keystore"
            storeFile = file(storeFilePath)
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: "phpserver2024"
            keyAlias = System.getenv("KEY_ALIAS") ?: "phpserver"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "phpserver2024"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    applicationVariants.all {
        outputs.all {
            (this as com.android.build.gradle.internal.api.ApkVariantOutputImpl).outputFileName =
                "php-server-${versionName}-${versionCode}.apk"
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.material.icons.extended)
    implementation(libs.navigation.compose)
    implementation(libs.nanohttpd)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
}
