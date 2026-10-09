plugins {
    java
}

group = "net.sacredlabyrinth.phaed.squaremap.simpleclans"
version = "2.2.0"
description = "Shows SimpleClans homes, territory and kills on squaremap"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.roinujnosde.me/releases/")
}

val paperApi = "io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT"

dependencies {
    compileOnly(paperApi)
    compileOnly("xyz.jpenilla:squaremap-api:1.4.0")
    compileOnly("net.sacredlabyrinth.phaed.simpleclans:SimpleClans:2.19.2") {
        isTransitive = false
    }

    // tests run against the same Adventure version the plugin is compiled with
    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    withType<JavaCompile>().configureEach {
        options.encoding = Charsets.UTF_8.name()
        options.release = 21
    }

    processResources {
        val props = mapOf("version" to project.version, "description" to project.description)
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    jar {
        archiveFileName = "${rootProject.name}-${project.version}.jar"
    }

    test {
        useJUnitPlatform()
    }
}
