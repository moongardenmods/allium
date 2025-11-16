val mavenGroup: String by project

val alliumVersion: String by project
val alliumReleaseCandidate: String by project
val alliumBaseName: String by project

// Common Dependencies
val cobalt: String by project
val enhancedReflections: String by project

// Following by example, using semantic versioning null
var v = alliumVersion
if ("0" != alliumReleaseCandidate) {
	v = "$v-rc$alliumReleaseCandidate"
}
version = v
group = mavenGroup

base {
	archivesName.set(alliumBaseName)
}

dependencies {
	implementation(include("cc.tweaked", "cobalt", cobalt))
	implementation(include("me.basiqueevangelist","enhanced-reflection", enhancedReflections))
}

java {
	withSourcesJar()

	sourceCompatibility = JavaVersion.VERSION_17
	targetCompatibility = JavaVersion.VERSION_17
}

// configure the maven publication
publishing {
	publications {
		register("mavenJava", MavenPublication::class) {
			from(components["java"])
			groupId = mavenGroup
			artifactId = alliumBaseName
			version = version
		}
	}
}