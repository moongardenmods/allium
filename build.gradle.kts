import java.util.*

plugins {
    id("maven-publish")
    id("net.fabricmc.fabric-loom") version "1.14-SNAPSHOT"
}

// Fabric Properties
val minecraftVersion: String by project
val loaderVersion: String by project

dependencies {
    minecraft("com.mojang", "minecraft", minecraftVersion)
}

subprojects {
    apply(plugin = "maven-publish")
    apply(plugin = "net.fabricmc.fabric-loom")

    repositories {
        maven("https://maven.hugeblank.dev/releases") {
            content {
                includeGroup("dev.hugeblank")
                includeGroup("cc.tweaked")
            }
        }
        maven("https://basique.top/maven/releases") {
            content {
                includeGroup("me.basiqueevangelist")
            }
        }
    }

    dependencies {
        minecraft("com.mojang", "minecraft", minecraftVersion)
        implementation("net.fabricmc", "fabric-loader", loaderVersion)
    }

    tasks {
        processResources {
            inputs.property("version", project.version)

            filesMatching("fabric.mod.json") {
                expand(mutableMapOf("version" to project.version, "name" to project.name))
            }
        }

        jar {
            from("LICENSE") {
                rename { "${it}_${project.base.archivesName.get()}" }
            }
        }

        loom {
            val moduleName = project.name.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
            }
            runs {
                named("client") {
                    configName = "$moduleName Client"
                    ideConfigGenerated(true)
                    runDir("../run")
                    programArgs("-username", "GradleDev")
                }
                named("server") {
                    configName = "$moduleName Server"
                    ideConfigGenerated(true)
                    runDir("../run")
                }
            }
        }
    }

    publishing {
        repositories {
            maven {
                name = "hugeblankRelease"
                url = uri("https://maven.hugeblank.dev/releases")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
            maven {
                name = "hugeblankSnapshot"
                url = uri("https://maven.hugeblank.dev/snapshots")
                credentials(PasswordCredentials::class)
                authentication {
                    create<BasicAuthentication>("basic")
                }
            }
        }
    }
}

tasks {
    register<GradleBuild>("buildAll") {
        group = "build"
        tasks = subprojects.map { ":${it.name}:build" }
    }

    loom {
        runs {
            named("client") {
                ideConfigGenerated(false)
            }
            named("server") {
                ideConfigGenerated(false)
            }
        }
    }
}