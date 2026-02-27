plugins {
    id("maven-publish")
    id("allium.fabric-conventions")
}

configurations {
    register("combineLocal") {
        isCanBeResolved = true
        isCanBeConsumed = false
    }

    runtimeClasspath {
        extendsFrom(getByName("combineLocal"))
    }
}

tasks {
    var output = "-Dcombine.output=../docs"
    var extensions = "-Dcombine.extensions=dev.moongarden.allium.util.combine.AlliumCombineExtension"
    register<JavaExec>("genLuaSources") {
        group = "allium"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass = "dev.moongarden.combine.Combine"
        jvmArgs = listOf(output, extensions)
        workingDir = file(".")
    }

    register<JavaExec>("genLuaSourcesAll") {
        group = "allium"

        classpath = sourceSets["main"].runtimeClasspath
        mainClass = "dev.moongarden.combine.Combine"
        jvmArgs = listOf(output, extensions, "-Dcombine.ignoreAccess")
        workingDir = file(".")
    }

    register<Exec>("updateGlobals") {
        group = "allium"
        commandLine = listOf("git", "submodule", "update", "--recursive")
        workingDir = file("..")
    }
}

dependencies {
    implementation("dev.moongarden:combine:${project.properties["combine"]}")
	implementation(include("cc.tweaked:cobalt:${project.properties["cobalt"]}")!!)
	implementation(include("me.basiqueevangelist:enhanced-reflection:${project.properties["enhancedReflections"]}")!!)
}