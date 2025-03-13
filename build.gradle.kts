plugins {
    id("maven-publish")
    id("fabric-loom") version "1.7-SNAPSHOT"
}

repositories {
    maven("https://maven.hugeblank.dev/releases") {
        content {
            includeGroup("dev.hugeblank")
        }
    }
    maven("https://squiddev.cc/maven") {
        content {
            includeGroup("cc.tweaked")
        }
    }
    maven("https://maven.nucleoid.xyz") {
        content {
            includeGroup("eu.pb4")
        }
    }
    maven("https://basique.top/maven/releases") {
        content {
            includeGroup("me.basiqueevangelist")
        }
    }
}

// Mod Properties
val mavenGroup: String by project
val version: String by project
val releaseCandidate: String by project
val baseName: String by project

// Fabric Properties
val minecraftVersion: String by project
val yarnMappings: String by project
val loaderVersion: String by project

// Dependencies
val cobalt: String by project
val tinyParser: String by project
val enhancedReflections: String by project

var v = version
if ("0" != releaseCandidate) {
    v = "$v-rc$releaseCandidate"
}

project.version = v
group = mavenGroup

base {
    archivesName.set(baseName)
}

loom {
    splitEnvironmentSourceSets()
    mods {
        register("allium") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    mappings("net.fabricmc:yarn:$yarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$loaderVersion")

    modImplementation("cc.tweaked", "cobalt", cobalt)
    modImplementation("me.basiqueevangelist", "enhanced-reflection", enhancedReflections)
    modImplementation("net.fabricmc", "tiny-mappings-parser", tinyParser)
}

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            groupId = mavenGroup
            artifactId = baseName
            version = version
        }
    }
    repositories {
        maven {
            name = "hugeblankRepo"
            url = uri("https://maven.hugeblank.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}
