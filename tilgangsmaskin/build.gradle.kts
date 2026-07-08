plugins {
    id("common")
    `java-library`
}

dependencies {

//    implementation(project(path = ":konfigurasjon"))
    implementation(project(path = ":openapi"))
//
//    implementation(libs.bundles.jackson)
//    implementation("tools.jackson.module:jackson-module-blackbird:${libs.versions.jackson.get()}")
//
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:1.7.3")
    implementation(libs.kotlin.logging)

//    implementation(libs.konfig)
//    implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations:2.1.0")
//    implementation("io.opentelemetry:opentelemetry-api:1.36.0")
//    implementation("io.prometheus:prometheus-metrics-core:1.3.1")
//    implementation("io.micrometer:micrometer-registry-prometheus:1.16.2")
//    implementation("org.slf4j:slf4j-api:2.0.17")
//
//    implementation(libs.bundles.ktor.client)
//    implementation(libs.bundles.ktor.server)
//    implementation("io.ktor:ktor-server-core-jvm:${libs.versions.ktor.get()}")
//    implementation("io.ktor:ktor-server-cio:${libs.versions.ktor.get()}")
//    implementation("io.ktor:ktor-server-swagger:${libs.versions.ktor.get()}")
//    implementation("io.ktor:ktor-server-content-negotiation:${libs.versions.ktor.get()}")
//    implementation("io.ktor:ktor-server-status-pages:${libs.versions.ktor.get()}")
//    implementation("io.ktor:ktor-server-metrics-micrometer:${libs.versions.ktor.get()}")
//    implementation("io.ktor:ktor-serialization-jackson3:${libs.versions.ktor.get()}")
//
//    testImplementation("io.kotest:kotest-assertions-core-jvm:${libs.versions.kotest.get()}")
//    testImplementation("io.kotest:kotest-assertions-json:${libs.versions.kotest.get()}")
//    testImplementation(libs.mockk)
//    testImplementation(libs.mock.oauth2.server)
//    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
//    testImplementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
//    testImplementation("com.approvaltests:approvaltests:22.3.3")
//    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.0")
//    testImplementation("io.kotest:kotest-runner-junit5:${libs.versions.kotest.get()}")
}
