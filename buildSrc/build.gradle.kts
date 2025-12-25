import org.jetbrains.kotlin.konan.properties.Properties
import kotlin.io.path.inputStream

plugins {
    `kotlin-dsl`
}

val props: Properties = Properties()
props.load(rootDir.toPath().resolve("../gradle.properties").inputStream())
props.forEach { project.ext[it.key.toString()] = it.value }

layout.buildDirectory = file("../build/buildSrc")

dependencies {
    implementation("net.fabricmc:fabric-loom:${project.properties["loomVersion"]}")
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
}