plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "ltd.realquick.nitnem"
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "ltd.realquick.nitnem"
        minSdk = 26
        targetSdk = 37
        versionCode = 6
        versionName = "1.0.3"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (file("release.jks").exists()) {
            create("release") {
                storeFile = file("release.jks")
                storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
                keyAlias = System.getenv("KEY_ALIAS") ?: ""
                keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            }
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
            signingConfig = signingConfigs.findByName("release")
                ?: signingConfigs.getByName("debug")
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.activity:activity:1.13.0")
        force("androidx.activity:activity-ktx:1.13.0")
    }
}

dependencies {
    implementation(libs.oneui.design) {
        exclude(group = "com.google.mlkit")
        exclude(group = "com.google.android.gms")
        exclude(group = "com.google.firebase")
        exclude(group = "com.google.android.datatransport")
        exclude(group = "androidx.camera")
    }
    implementation(libs.bundles.sesl.androidx)
    implementation(libs.sesl.material)
    implementation(libs.oneui.icons)
    implementation(libs.activity.ktx)
}
