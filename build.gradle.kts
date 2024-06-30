import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.8.22"
    kotlin("plugin.serialization") version "1.8.22"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "top.e404"
version = "1.6.5"
val epluginVersion = "1.2.0"

fun kotlinx(id: String, version: String) = "org.jetbrains.kotlinx:kotlinx-$id:$version"
fun eplugin(id: String, version: String = epluginVersion) = "top.e404:eplugin-$id:$version"

repositories {
    // sf
    maven("https://jitpack.io/")
    // spigot
    // maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    // paper
    maven("https://repo.papermc.io/repository/maven-public/")
    // papi
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    // mm
    maven("https://mvn.lumine.io/repository/maven-public/")
    mavenCentral()
    mavenLocal()
}

dependencies {
    // spigot
    // compileOnly("org.spigotmc:spigot-api:1.16.5-R0.1-SNAPSHOT")
    compileOnly("io.papermc.paper:paper-api:1.20.1-R0.1-SNAPSHOT")
    // eplugin
    implementation(eplugin("core"))
    implementation(eplugin("serialization"))
    implementation(eplugin("menu"))
    implementation(eplugin("adventure"))
    implementation(eplugin("serialization-adventure"))
    implementation(eplugin("hook-slimefun"))
    implementation(eplugin("hook-mmoitems"))
    // sf
    compileOnly("com.github.StarWishsama:Slimefun4:master-SNAPSHOT")
    // networks
    compileOnly("io.github.sefiraat:networks:MODIFIED_1.2.0")
    // mi
    compileOnly("net.Indyuce:MMOItems:6.7.3")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "17"
    }

    withType<JavaCompile> {
        targetCompatibility = "17"
    }

    processResources {
        filesMatching("plugin.yml") {
            expand(project.properties)
        }
    }

    build {
        finalizedBy(shadowJar)
    }

    var prepare = false

    create("prepare") {
        doFirst {
            prepare = true
        }
    }

    shadowJar {
        archiveFileName.set("${project.name}-${project.version}.jar")
        exclude("META-INF/**")
        relocate("kotlin", "top.e404.slimefun.stackingmachine.relocate.kotlin")
        relocate("top.e404.eplugin", "top.e404.slimefun.stackingmachine.relocate.eplugin")

        val outFile = this.archiveFile.get().asFile
        val jarDir = rootDir.resolve("jar").also(File::mkdir)
        val copyFile = jarDir.resolve(outFile.name)

        doFirst {
            jarDir.listFiles()?.forEach {
                println("deleted ${it.absolutePath}")
                it.delete()
            }
        }

        doLast {
            println("copy ${copyFile.absolutePath}")
            outFile.copyTo(copyFile)

            if (prepare) {
                rootDir.resolve("run/plugins").run {
                    val pluginFile = resolve(outFile.name)
                    pluginFile.delete()
                    outFile.copyTo(pluginFile)
                }
            }
        }
    }
}