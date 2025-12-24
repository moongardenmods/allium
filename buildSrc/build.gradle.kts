plugins {
    `kotlin-dsl`
}

dependencies {
    implementation("net.fabricmc:fabric-loom:1.14-SNAPSHOT")
}

repositories {
    gradlePluginPortal()
    maven("https://maven.fabricmc.net/")
}