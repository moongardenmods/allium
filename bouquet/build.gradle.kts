val mavenGroup: String by project

// Common Dependencies
val cobalt: String by project
val enhancedReflections: String by project

val bouquetVersion: String by project
val bouquetReleaseCandidate: String by project
val bouquetBaseName: String by project

// Bouquet Dependencies
val nettyHttp: String by project
val placeholderApi: String by project

version = bouquetVersion
group = mavenGroup

base {
	archivesName.set(bouquetBaseName)
}

loom {
	splitEnvironmentSourceSets()
	mods {
		register("bouquet") {
			sourceSet(sourceSets["main"])
			sourceSet(sourceSets["client"])
		}
	}

}

dependencies {
	implementation("cc.tweaked", "cobalt", cobalt)
	implementation("me.basiqueevangelist","enhanced-reflection", enhancedReflections)
//	implementation("eu.pb4", "placeholder-api", placeholderApi)

    // Probably don't need anymore now that it's bundled with game
//	implementation(include("io.netty", "netty-codec-http", nettyHttp))

	implementation(project(path = ":allium"))
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
			artifactId = bouquetBaseName
			version = version
		}
	}
}