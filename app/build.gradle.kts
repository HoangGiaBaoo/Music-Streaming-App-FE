plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.musicstreamingapp"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.musicstreamingapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.fragment)

    // Retrofit + OkHttp
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)

    // ExoPlayer
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)

    // Glide + BlurTransformation
    implementation(libs.glide)
    implementation(libs.glide.transformations)

    // CircleImageView
    implementation(libs.circleimageview)

    // ViewPager2
    implementation(libs.viewpager2)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
