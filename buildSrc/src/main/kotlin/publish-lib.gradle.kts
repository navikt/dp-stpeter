import java.net.URI

plugins {
    `java-library`
    `maven-publish`
}

group = "no.nav.dagpenger"
version = (findProperty("version") as String?) ?: "local-SNAPSHOT"

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    archiveClassifier.set("sources")
    from(sourceSets["main"].allSource)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifact(sourcesJar)
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/navikt/dp-stpeter")
            credentials {
                username = project.findProperty("githubUser") as String?
                password = project.findProperty("githubPassword") as String?
            }
        }
    }
}
