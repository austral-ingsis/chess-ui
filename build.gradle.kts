import java.net.URI

plugins {
    application
    `java-library`
    `maven-publish`

    kotlin("jvm") version "2.3.21"
    id("org.openjfx.javafxplugin").version("0.1.0")
}

group = "edu.austral.dissis.chess"
version = "3.0.1"

repositories {
    mavenCentral()
}

application {
    mainClass.set("edu.austral.dissis.chess.gui.ChessGameApplication")
}

javafx {
    version = "21"
    modules = listOf("javafx.graphics", "javafx.controls")
}

tasks.jar {
    manifest {
        attributes(
            mapOf(
                "Implementation-Title" to project.name,
                "Implementation-Version" to project.version,
            ),
        )
    }
}

kotlin {
    jvmToolchain(25)
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI.create("https://maven.pkg.github.com/austral-ingsis/chess-ui")
            credentials {
                username = System.getenv("GITHUB_USER")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }

    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
dependencies {
    implementation(kotlin("script-runtime"))
    implementation("io.reactivex.rxjava3:rxjava:3.1.8")
    implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
}
