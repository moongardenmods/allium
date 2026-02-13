pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/")
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "Allium Dev"

include("allium")
include("bouquet")
include("bouquet:examples:cactallium")
include("bouquet:examples:hangman")
include("bouquet:examples:luckbrew")
include("bouquet:examples:moonflower")
include("bouquet:examples:script_a")
include("bouquet:examples:script_b")
include("bouquet:examples:wailua")
include("bouquet:examples:wetworks")
include("bouquet:examples:whats_my_ip")