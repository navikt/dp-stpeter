plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
}
