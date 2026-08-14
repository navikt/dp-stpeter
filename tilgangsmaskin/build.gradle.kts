plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(project(path = ":openapi"))
    implementation(project(path = ":oidc"))

    implementation(libs.kotlin.logging)
    implementation("io.ktor:ktor-http-jvm:${libs.versions.ktor.get()}")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.21")
}
