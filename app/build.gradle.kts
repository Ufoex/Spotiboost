plugins { id("com.android.application") }

android {
    namespace = "com.ufoex.spotiboost.gecko"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.ufoex.spotiboost"
        minSdk = 26
        targetSdk = 36
        // CI passes -PversionNameOverride=1.2.3 from the tag: versionCode = major*1_000_000 + minor*1_000 + patch
        val v = (project.findProperty("versionNameOverride") as String?)?.takeIf { it.isNotBlank() }
        val p = (v ?: "0.1.0").split("-")[0].split(".").map { it.toIntOrNull() ?: 0 }
        versionName = v ?: "0.1.0-dev"
        versionCode = p.getOrElse(0) { 0 } * 1_000_000 + p.getOrElse(1) { 0 } * 1_000 + p.getOrElse(2) { 0 }
    }
    packaging { jniLibs { useLegacyPackaging = true } } // Gecko's native libs are extracted at install

    // Release key comes from the environment (GitHub secrets). Without it the release is signed with the debug key:
    // it installs, but it cannot update a build signed with another key.
    val ks = System.getenv("KEYSTORE_FILE")
    signingConfigs {
        if (ks != null) create("release") {
            storeFile = file(ks)
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            // no R8: GeckoView ships its own native code and reflection-heavy Java, shrinking gains little and risks a lot
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".gecko" // keeps the id of the existing debug install (com.ufoex.spotiboost.gecko)
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    // arm64 only: the phone is arm64 and the universal artifact is ~4x bigger
    implementation("org.mozilla.geckoview:geckoview-arm64-v8a:150.0.20260511200624")
    testImplementation("junit:junit:4.13.2")
    implementation("androidx.core:core:1.16.0") // FileProvider: hands the downloaded update to the system installer
}
