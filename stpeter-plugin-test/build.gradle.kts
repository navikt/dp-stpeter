plugins {
    id("common")
    `java-library`
    id("publish-lib")
}
dependencies {

    val dpBibliotekerVersion = "2026.09.25-06.21.cba57db93eac"

    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
}
