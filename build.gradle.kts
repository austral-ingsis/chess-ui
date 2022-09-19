plugins {
    application
    `java-library`
    `maven-publish`

    kotlin("jvm") version "1.7.10"
    id("org.openjfx.javafxplugin").version("0.0.13")

}

group = "edu.austral.dissis.chess"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

application {
    mainClass.set("edu.austral.dissis.chess.gui.ChessGameApplication")
}

javafx {
    version = "18"
    modules = listOf("javafx.graphics")
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
            "Implementation-Version" to project.version))
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
