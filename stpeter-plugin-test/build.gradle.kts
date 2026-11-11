plugins {
    id("common")
    `java-library`
    id("publish-lib")
}
dependencies {

    val dpBibliotekerVersion = "2026.11.11-13.39.1535e94d2015"

    implementation(libs.bundles.ktor.server)
    implementation(libs.mock.oauth2.server)
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")
}
