import java.util.Locale

plugins {
    `maven-publish`
    id("net.fabricmc.fabric-loom")
}

repositories {
    maven("https://maven.moongarden.dev/releases") {
        content {
            includeGroup("dev.moongarden")
            includeGroup("cc.tweaked")
        }
    }
    maven("https://maven.moongarden.dev/snapshots") {
        content {
            includeGroup("dev.moongarden")
        }
    }
    maven("https://basique.top/maven/releases") {
        content {
            includeGroup("me.basiqueevangelist")
        }
    }
}

val rc = project.properties[project.name+"ReleaseCandidate"].toString()
var v = project.properties[project.name+"Version"].toString()
if ("0" != rc) {
    v = "$v-rc.$rc"
}
version = v
group = project.properties["mavenGroup"].toString()

layout.buildDirectory = file("../build/" + project.name)

base {
    archivesName = project.properties[project.name + "BaseName"].toString()
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraftVersion"]}")
    implementation("net.fabricmc:fabric-loader:${project.properties["loaderVersion"]}")
}

loom {
    splitEnvironmentSourceSets()

    mods {
        register(project.name) {
            sourceSet(sourceSets["main"])
            if (project.name != "allium") sourceSet(sourceSets["client"])
        }
    }

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

java {
    withSourcesJar()

    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    processResources {
        inputs.property("version", project.version)
        inputs.property("name", project.name)

        filesMatching("fabric.mod.json") {
            expand(mutableMapOf("version" to project.version, "name" to project.name))
        }
    }

    jar {
        from("../LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }
}

// configure the maven publication
publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            groupId = group.toString()
            artifactId = base.archivesName.get()
            version = project.version.toString()
        }
    }

    repositories {
        maven {
            name = "moongardenRelease"
            url = uri("https://maven.moongarden.dev/releases")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
        maven {
            name = "moongardenSnapshot"
            url = uri("https://maven.moongarden.dev/snapshots")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
}