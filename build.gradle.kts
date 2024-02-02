plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.dokka) apply false
}

group = "at.bitfire"
version = System.getenv("GIT_COMMIT")
