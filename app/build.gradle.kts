plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.jfedor.frozenbubble"
    compileSdk = 36

    defaultConfig {
        applicationId = "org.jfedor.frozenbubble"
        minSdk = 24
        targetSdk = 36
        versionCode = 55
        versionName = "4.5"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    lint {
        // Old game code; report issues but don't block builds on them.
        abortOnError = false
    }
}
