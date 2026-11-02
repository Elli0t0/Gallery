plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val versionMajor = (project.findProperty("VERSION_MAJOR") as? String)?.toInt() ?: 0
val versionMinor = (project.findProperty("VERSION_MINOR") as? String)?.toInt() ?: 0
val versionPatch = (project.findProperty("VERSION_PATCH") as? String)?.toInt() ?: 1
val appVersionName = "$versionMajor.$versionMinor.$versionPatch"
val appVersionCode = versionMajor * 10000 + versionMinor * 100 + versionPatch

android {
    namespace = "com.elli0t0.gallery"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.elli0t0.gallery"
        minSdk = 26
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        ndk {
            abiFilters += listOf("arm64-v8a")
        }
    }

    packaging {
        resources {
            excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "DebugProbesKt.bin", "kotlin-tooling-metadata.json")
        }
    }

    androidResources {
        localeFilters += listOf("en", "fa")
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("GALLERY_KEYSTORE_PATH")?.trim()
            val keystorePassword = System.getenv("GALLERY_KEYSTORE_PASSWORD")?.trim()
            val keyAliasVal = System.getenv("GALLERY_KEY_ALIAS")?.trim()
            val keyPasswordVal = System.getenv("GALLERY_KEY_PASSWORD")?.trim()
            if (!keystorePath.isNullOrEmpty() && !keystorePassword.isNullOrEmpty()
                && !keyAliasVal.isNullOrEmpty() && !keyPasswordVal.isNullOrEmpty()
            ) {
                storeFile = file(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAliasVal
                keyPassword = keyPasswordVal
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
            val releaseKeystore = signingConfigs.getByName("release")
            if (releaseKeystore.storeFile != null) {
                signingConfig = releaseKeystore
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

base {
    archivesName.set("Gallery-$appVersionName")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.print)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
