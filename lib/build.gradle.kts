plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.dokka)
    `maven-publish`
}

android {
    namespace = "at.bitfire.vcard4android"

    compileSdk = 35

    defaultConfig {
        minSdk = 23        // Android 6

        aarMetadata {
            minCompileSdk = 29
        }

        // These ProGuard/R8 rules will be included in the final APK.
        consumerProguardFiles("consumer-rules.pro")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }
    kotlin {
        jvmToolchain(21)
    }

    packaging {
        resources {
            excludes += listOf("LICENSE", "META-INF/LICENSE.txt", "META-INF/NOTICE.txt")
        }
    }

    buildTypes {
        release {
            // Android libraries shouldn't be minified:
            // https://developer.android.com/studio/projects/android-library#Considerations
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    lint {
        disable += listOf("AllowBackup", "InvalidPackage")
    }

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    @Suppress("UnstableApiUsage")
    testOptions {
        managedDevices {
            localDevices {
                create("virtual") {
                    device = "Pixel 3"
                    apiLevel = 33
                    systemImageSource = "aosp-atd"
                }
            }
        }
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
    implementation(libs.guava)

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