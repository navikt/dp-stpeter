plugins {
    id("common")
    `java-library`
}

dependencies {
    implementation(libs.kotlin.logging)
    implementation("org.slf4j:slf4j-api:2.0.20")
    implementation("ch.qos.logback:logback-classic:1.6.6")
    implementation("net.logstash.logback:logstash-logback-encoder:9.0")
    implementation("no.nav.common:audit-log:4.2026.11.05_07.24-dd0ce2805c16")

    testImplementation("io.kotest:kotest-assertions-core-jvm:${libs.versions.kotest.get()}")
    testImplementation("io.kotest:kotest-assertions-json:${libs.versions.kotest.get()}")
    testImplementation("io.kotest:kotest-runner-junit5:${libs.versions.kotest.get()}")
}
