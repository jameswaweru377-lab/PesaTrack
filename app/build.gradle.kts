plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val entityEnrichmentUrl = providers
    .gradleProperty("PESATRACK_ENTITY_ENRICHMENT_URL")
    .orElse("")
    .get()


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
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.navigation:navigation-compose:2.9.8")
    implementation("androidx.work:work-runtime:2.12.0")

    testImplementation("junit:junit:4.13.2")
}
