plugins {
    id("com.android.library") version "8.0.1" apply false
    id("org.jetbrains.kotlin.android") version "1.8.22" apply false
    id("org.jetbrains.dokka") version "1.8.20" apply false
}

group = "at.bitfire"
version = System.getenv("GIT_COMMIT")
