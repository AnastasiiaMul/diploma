plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.diplomaappmodeltflite"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.diplomaappmodeltflite"
        minSdk = 24
        targetSdk = 35
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
    buildFeatures {
        mlModelBinding = true
        dataBinding = true
    }
    androidResources {
        noCompress += "tflite"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.tensorflow.lite.support)
    implementation(libs.tensorflow.lite.metadata)
    implementation(libs.tensorflow.lite.gpu){
        exclude(group = "org.tensorflow", module = "tensorflow-lite")
    }
    implementation(libs.camera.view)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.camera2)
    implementation(libs.camera.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}