plugins {
    id("common")
    `java-library`
    id("publish-lib")
}
dependencies {

    val dpBibliotekerVersion = "2026.10.05-18.24.72dfe9185852"

    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
}
