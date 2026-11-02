plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "org.jfedor.frozenbubble"
    compileSdk = 36

    defaultConfig {
        // Its own app ID, so it installs beside the original Frozen Bubble.
        // (The Java packages keep their original names; see namespace.)
        applicationId = "io.github.dotelpenguin.frozenbubblehd"
        minSdk = 24
        targetSdk = 36
        // Frozen Bubble HD is a new app (its own ID), so its versions start
        // again at 1.0.  Raise versionCode by 1 for every release.
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    buildFeatures {
        buildConfig = true   // BuildConfig.VERSION_NAME, shown on the home screen
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

// List every deprecated or unchecked API use in the build output (as
// warnings with file and line), to work through docs/DEPRECATIONS.md.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked",
                                       "-Xmaxwarns", "1000"))
}
