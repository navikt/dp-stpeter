plugins {
    id("common")
    `java-library`
}
dependencies {

    val dpBibliotekerVersion = "2026.05.04-11.00.ccf523d33b63"

    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
}
