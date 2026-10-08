plugins {
    id("common")
    `java-library`
    id("publish-lib")
}
dependencies {

    val dpBibliotekerVersion = "2026.10.08-18.23.f84047ea6970"

    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
}
