plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("net.fabricmc:fabric-loom:${project.properties["loomVersion"]}")
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
}