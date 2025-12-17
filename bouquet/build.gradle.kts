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
val fabricApi: String by project

var v = bouquetVersion
if ("0" != bouquetReleaseCandidate) {
    v = "$v-rc$bouquetReleaseCandidate"
}
version = v
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
    runtimeOnly("net.fabricmc.fabric-api", "fabric-api", fabricApi)

	implementation(project(path = ":allium"))
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_25
	targetCompatibility = JavaVersion.VERSION_25
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