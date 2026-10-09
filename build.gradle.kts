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

/** Adventure 5, as bundled with newer Paper builds; the jar must link against it too. */
val adventure5: Configuration by configurations.creating

dependencies {
    compileOnly(paperApi)
    compileOnly("xyz.jpenilla:squaremap-api:1.4.0") {
        // squaremap-api 1.4 drags in Adventure 5. Code compiled against Adventure 5 fails on
        // Paper 1.21's Adventure 4 (some methods changed return types), while code compiled
        // against Adventure 4 links on both, so compile against Paper's Adventure 4.
        exclude(group = "net.kyori")
    }
    compileOnly("net.sacredlabyrinth.phaed.simpleclans:SimpleClans:2.19.2") {
        isTransitive = false
    }

    testImplementation(paperApi)
    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    adventure5("net.kyori:adventure-api:5.2.0")
    adventure5(platform("org.junit:junit-bom:5.11.4"))
    adventure5("org.junit.jupiter:junit-jupiter")
    adventure5("org.junit.platform:junit-platform-launcher")
}

val testAdventure5 by tasks.registering(Test::class) {
    description = "Runs the tests on Adventure 5 to make sure the jar works on newer Paper too."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.main.get().output + sourceSets.test.get().output + adventure5
    useJUnitPlatform()
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

    check {
        dependsOn(testAdventure5)
    }
}
