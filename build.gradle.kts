plugins {
    id("dev.architectury.loom") version "1.9-SNAPSHOT" apply false
    id("architectury-plugin") version "3.4-SNAPSHOT"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
}

// Mod Properties
val mavenGroup: String by project
val version: String by project
val releaseCandidate: String by project
val baseName: String by project

// Fabric Properties
val minecraftVersion: String by project
val yarnMappings: String by project
val yarnMappingsNeoforge: String by project
val loaderVersion: String by project

// Dependencies
val cobalt: String by project
val tinyParser: String by project
val enhancedReflections: String by project

var v = version
if ("0" != releaseCandidate) {
    v = "$v-rc$releaseCandidate"
}

architectury {
    minecraft = minecraftVersion
}

allprojects {
    project.version = v
    group = mavenGroup
}

subprojects {
    apply(plugin = "dev.architectury.loom")
    apply(plugin = "architectury-plugin")
    apply(plugin = "maven-publish")

    base {
        archivesName.set("$baseName-${project.name}")
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

    dependencies {
        minecraft("com.mojang:minecraft:$minecraftVersion")
        mappings(loom.layered {
            mappings("net.fabricmc:yarn:$yarnMappings:v2")
            mappings("dev.architectury:yarn-mappings-patch-neoforge:$yarnMappingsNeoforge")
        })


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
                artifactId = base.archivesName.get()
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
}


/*tasks {
    processResources {
        inputs.property("version", project.version)

        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version))
        }
    }

    loom {
        runs {
            named("client") {
                programArgs("-username", "GradleDev")
            }
        }
    }
}*/
