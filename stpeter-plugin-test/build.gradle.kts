plugins {
    id("common")
    `java-library`
    id("publish-lib")
}
dependencies {

    val dpBibliotekerVersion = "2026.10.15-12.21.08e956f76dd5"

    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
}
