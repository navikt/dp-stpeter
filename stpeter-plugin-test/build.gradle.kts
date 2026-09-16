plugins {
    id("common")
    `java-library`
}
dependencies {

    val dpBibliotekerVersion = "2026.05.04-11.00.ccf523d33b63"

//    implementation(libs.bundles.ktor.client)
    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")

//    testImplementation(libs.bundles.kotest.assertions)
//
//    testImplementation(libs.mockk)
//    testImplementation("io.ktor:ktor-server-test-host-jvm:${libs.versions.ktor.get()}")
//    testImplementation("io.ktor:ktor-client-content-negotiation:${libs.versions.ktor.get()}")
//    testImplementation("com.approvaltests:approvaltests:31.0.0")
//    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.0")
//    testImplementation("io.kotest:kotest-runner-junit5:${libs.versions.kotest.get()}")
//    testImplementation("com.h2database:h2:2.5.250")
}
