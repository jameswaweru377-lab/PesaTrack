plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.chaquo.python")
}

val entityEnrichmentUrl = providers
    .gradleProperty("PESATRACK_ENTITY_ENRICHMENT_URL")
    .orElse(
        providers.environmentVariable(
            "PESATRACK_ENTITY_ENRICHMENT_URL"
        )
    )
    .orElse("")
    .get()


val appVersionCode =
    providers
        .gradleProperty(
            "PESATRACK_VERSION_CODE"
        )
        .orElse(
            providers.environmentVariable(
                "PESATRACK_VERSION_CODE"
            )
        )
        .orElse("1")
        .get()
        .toIntOrNull()
        ?: error(
            "PESATRACK_VERSION_CODE must be an integer."
        )

val appVersionName =
    providers
        .gradleProperty(
            "PESATRACK_VERSION_NAME"
        )
        .orElse(
            providers.environmentVariable(
                "PESATRACK_VERSION_NAME"
            )
        )
        .orElse("0.1.0")
        .get()

val releaseStoreFilePath =
    providers
        .environmentVariable(
            "PESATRACK_RELEASE_KEYSTORE"
        )
        .orNull
        ?.trim()
        ?.takeIf { it.isNotEmpty() }

val releaseStorePassword =
    providers
        .environmentVariable(
            "PESATRACK_RELEASE_STORE_PASSWORD"
        )
        .orNull
        ?.takeIf { it.isNotEmpty() }

val releaseKeyAlias =
    providers
        .environmentVariable(
            "PESATRACK_RELEASE_KEY_ALIAS"
        )
        .orNull
        ?.takeIf { it.isNotEmpty() }

val releaseKeyPassword =
    providers
        .environmentVariable(
            "PESATRACK_RELEASE_KEY_PASSWORD"
        )
        .orNull
        ?.takeIf { it.isNotEmpty() }

val releaseSigningConfigured =
    listOf(
        releaseStoreFilePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword
    ).all { it != null }


android {
    namespace = "com.kedevjimmy.pesatrack"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.kedevjimmy.pesatrack"
        buildConfigField(
            "String",
            "ENTITY_ENRICHMENT_URL",
            "\"${entityEnrichmentUrl
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")}\""
        )
        minSdk = 24
        // Embedded Python native dependencies were resolved for ARM64.
        ndk { abiFilters += listOf("arm64-v8a") }
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseSigningConfigured) {
            create("release") {
                storeFile =
                    file(
                        requireNotNull(
                            releaseStoreFilePath
                        )
                    )
                storePassword =
                    requireNotNull(
                        releaseStorePassword
                    )
                keyAlias =
                    requireNotNull(
                        releaseKeyAlias
                    )
                keyPassword =
                    requireNotNull(
                        releaseKeyPassword
                    )
            }
        }
    }

    buildTypes {
        getByName("release") {
            if (releaseSigningConfigured) {
                signingConfig =
                    signingConfigs
                        .getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
        execution = "ANDROIDX_TEST_ORCHESTRATOR"
    }
}

// Offline statement parser, no remote processing of customer documents.
chaquopy {
    defaultConfig {
        version = "3.13"
        pip { install("pdfplumber==0.11.9") }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.4")
    implementation("androidx.work:work-runtime:2.12.0")
    implementation("com.google.code.gson:gson:2.13.1")

    testImplementation("junit:junit:4.13.2")
    // Local JVM HTTP tests need actual JSON behavior instead of Android's null-returning stubs.
    testImplementation("org.json:json:20260719")

    androidTestImplementation("androidx.test:core-ktx:1.7.0")
    androidTestImplementation("androidx.test.ext:junit-ktx:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestUtil("androidx.test:orchestrator:1.6.1")
}

