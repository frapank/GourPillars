plugins {
    kotlin("jvm") version "2.4.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.gradleup.shadow") version "9.4.3"
}

group = "org.gourmet"
version = "1.0"

repositories {
    mavenCentral()
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly(project(":"))
}

kotlin {
    jvmToolchain(21)
}

tasks.build {
    dependsOn("shadowJar")
}

bukkit {
    main = "org.gourmet.gourpillarseventsaddon.EventsAddonPlugin"
    apiVersion = "1.21"
    name = "GourPillarsEvents"
    version = getVersion().toString()
    author = "Gourmet"
    depend = listOf("GourPillars")
}
