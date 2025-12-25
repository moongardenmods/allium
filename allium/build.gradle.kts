plugins {
    id("maven-publish")
    id("allium.fabric-conventions")
}

dependencies {
	implementation(include("cc.tweaked:cobalt:${project.properties["cobalt"]}")!!)
	implementation(include("me.basiqueevangelist:enhanced-reflection:${project.properties["enhancedReflections"]}")!!)
}