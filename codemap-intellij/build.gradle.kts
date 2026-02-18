plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij") version "1.17.4"
}

group = "com.codemap.intellij"
version = "1.0.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.11.0")
}

intellij {
    version.set("2024.1")
    type.set("IC") // IntelliJ Community Edition
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    patchPluginXml {
        sinceBuild.set("241")
        untilBuild.set("251.*")
    }

    buildSearchableOptions {
        enabled = false
    }
}

kotlin {
    jvmToolchain(17)
}
