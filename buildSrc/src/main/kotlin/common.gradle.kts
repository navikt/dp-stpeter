import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint")
}

repositories {
    mavenCentral()
    maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    maven("https://jitpack.io")
}

dependencies {
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
    maxHeapSize = "1g"
    // Forward Docker socket to test JVM (required for Testcontainers with Colima/Rancher Desktop)
    System.getenv("DOCKER_HOST")?.let { environment("DOCKER_HOST", it) }
    System.getenv("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE")?.let { environment("TESTCONTAINERS_DOCKER_SOCKET_OVERRIDE", it) }
    System.getenv("TESTCONTAINERS_HOST_OVERRIDE")?.let { environment("TESTCONTAINERS_HOST_OVERRIDE", it) }
    reports.junitXml.includeSystemOutLog = false
    reports.junitXml.includeSystemErrLog = false
    testLogging {
        showExceptions = true
        showStandardStreams = false
        exceptionFormat = TestExceptionFormat.FULL
        // events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
    }
}

tasks.withType<KotlinCompile>().configureEach {
    dependsOn("ktlintFormat")
}
