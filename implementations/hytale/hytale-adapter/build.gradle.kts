plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "dev.orbisoptimizer"
version = "0.1.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly(fileTree("libs") { include("*.jar") })
}

tasks.shadowJar {
    archiveBaseName.set("orbisoptimizer-early")
    archiveClassifier.set("")
    archiveVersion.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
