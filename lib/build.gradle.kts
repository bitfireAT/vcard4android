plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    `maven-publish`
}

android {
    namespace = "at.bitfire.vcard4android"

    compileSdk = 33

    defaultConfig {
        minSdk = 21        // Android 5

        aarMetadata {
            minCompileSdk = 29
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17

        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        jvmToolchain(17)
    }

    @Suppress("UnstableApiUsage")
    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += listOf("LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
        }
    }

    lint {
        disable += listOf("AllowBackup", "InvalidPackage")
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    publishing {
        // Configure publish variant
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

publishing {
    // Configure publishing data
    publications {
        register("release", MavenPublication::class.java) {
            groupId = "com.github.bitfireAT"
            artifactId = "vcard4android"
            version = System.getenv("GIT_COMMIT")

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation(libs.kotlin.stdlib)
    coreLibraryDesugaring(libs.desugar)

    implementation(libs.androidx.annotation)
    @Suppress("VulnerableLibrariesLocal", "RedundantSuppression")
    implementation(libs.commons.io)
    implementation(libs.commons.text)

    // ez-vcard to parse/generate vCards
    api(libs.ezvcard) {    // requires Java 8
        // hCard functionality not needed
        exclude(group = "org.jsoup")
        exclude(group = "org.freemarker")
    }

    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)

    testImplementation(libs.junit)
}