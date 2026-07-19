plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
}

android {
    namespace = "com.sergiojosemp.obddashboard"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.sergiojosemp.obddash"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        dataBinding = true
        viewBinding = true
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

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    lint {
        abortOnError = false
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.hellocharts.library) {
        exclude(group = "com.android.support", module = "support-v4")
    }
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.roundcornerprogressbar)
    implementation(libs.plain.pie)
    implementation(libs.obd.java.api)

    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.material)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.recyclerview)

    implementation(libs.bundles.coroutines)

    // DataStore
    implementation(libs.datastore.preferences)

    // Preferences (for PreferenceFragmentCompat)
    implementation(libs.preference.ktx)

    // WorkManager
    implementation(libs.bundles.work)

    // Kotlin stdlib
    implementation(libs.kotlin.stdlib.jdk8)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose.ui)

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.work.testing)
}
