plugins {
    id("maven-publish")
    id("allium.fabric-conventions")
}

dependencies {
	implementation("cc.tweaked:cobalt:${project.properties["cobalt"]}")
	implementation("me.basiqueevangelist:enhanced-reflection:${project.properties["enhancedReflections"]}")

    localRuntime("net.fabricmc.fabric-api:fabric-api:${project.properties["fabricApi"]}")
    localRuntime(project(path = ":bouquet:examples:cactallium"))
    localRuntime(project(path = ":bouquet:examples:hangman"))
    localRuntime(project(path = ":bouquet:examples:luckbrew"))
    localRuntime(project(path = ":bouquet:examples:moonflower"))
    localRuntime(project(path = ":bouquet:examples:script_a"))
    localRuntime(project(path = ":bouquet:examples:script_b"))
    localRuntime(project(path = ":bouquet:examples:wailua"))
    localRuntime(project(path = ":bouquet:examples:wetworks"))
    localRuntime(project(path = ":bouquet:examples:whats_my_ip"))

	implementation(project(path = ":allium"))
}