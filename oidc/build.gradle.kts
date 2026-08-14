plugins {
    id("common")
    `java-library`
}

dependencies {

    implementation(libs.ktor.server.auth.jwt)
}
