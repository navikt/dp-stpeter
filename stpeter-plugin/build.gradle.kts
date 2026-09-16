plugins {
    id("common")
    `java-library`
}
dependencies {
    val dpBibliotekerVersion = "2026.05.04-11.00.ccf523d33b63"

    implementation(libs.bundles.ktor.client)
    implementation("no.nav.dagpenger:ktor-client-metrics:$dpBibliotekerVersion")
    implementation("io.prometheus:prometheus-metrics-core:1.3.1")
    implementation("io.micrometer:micrometer-registry-prometheus:1.16.2")
    implementation("tools.jackson.module:jackson-module-blackbird:${libs.versions.jackson.get()}")
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")

    testImplementation(libs.bundles.kotest.assertions)
    testImplementation("io.kotest:kotest-runner-junit5:${libs.versions.kotest.get()}")
    testImplementation(project(path = ":stpeter-plugin-test"))
    testImplementation(libs.mock.oauth2.server)
}
