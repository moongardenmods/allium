plugins {
    `maven-publish`
    id("net.fabricmc.fabric-loom")
}

group = project.properties["mavenGroup"].toString() + ".bouquet.examples"

layout.buildDirectory = file("../../../build/" + project.name)

base {
    archivesName = project.name
}

dependencies {
    minecraft("com.mojang:minecraft:${project.properties["minecraftVersion"]}")
    implementation("net.fabricmc:fabric-loader:${project.properties["loaderVersion"]}")
}

loom {
    mods {
        register(project.name) {
            sourceSet(sourceSets["main"])
        }
    }

    runs {
        named("client") {
            ideConfigGenerated(false)
        }
        named("server") {
            ideConfigGenerated(false)
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
        from("../../LICENSE") {
            rename { "${it}_${project.base.archivesName.get()}" }
        }
    }
}