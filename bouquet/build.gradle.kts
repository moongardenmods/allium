plugins {
    id("maven-publish")
    id("allium.fabric-conventions")
}

dependencies {
	implementation("cc.tweaked:cobalt:${project.properties["cobalt"]}")
	implementation("me.basiqueevangelist:enhanced-reflection:${project.properties["enhancedReflections"]}")

    localRuntime("net.fabricmc.fabric-api:fabric-api:${project.properties["fabricApi"]}")

	implementation(project(path = ":allium"))
}