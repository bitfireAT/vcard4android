plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.dokka")
    id("maven-publish")
}

android {
    namespace = "at.bitfire.vcard4android"

    compileSdk = 33

    defaultConfig {
        minSdk = 21        // Android 5
        targetSdk = 33     // Android 13

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

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    packagingOptions {
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
            version = project.properties["vcard4android.version"] as String

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${properties["vcard4android.lib.kotlin.version"]}")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")

    implementation("androidx.annotation:annotation:1.6.0")
    // noinspection GradleDependency
    implementation("commons-io:commons-io:${properties["vcard4android.lib.commonsIO.version"]}")
    // noinspection GradleDependency
    implementation("org.apache.commons:commons-text:${properties["vcard4android.lib.commonsText.version"]}")

    // ez-vcard to parse/generate vCards
    api("com.googlecode.ez-vcard:ez-vcard:0.12.0") {    // requires Java 8
        // hCard functionality not needed
        exclude(group = "org.jsoup")
        exclude(group = "org.freemarker")
    }

    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")

    testImplementation("junit:junit:4.13.2")
}
