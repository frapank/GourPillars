import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission.Default

plugins {
    kotlin("jvm") version "2.4.0"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.gradleup.shadow") version "9.4.3"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.diffplug.spotless") version "8.8.0"
}

group = "org.gourmet"
version = "1.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven {
        url = uri("https://repo.extendedclip.com/releases/")
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.11-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.12.2")
    implementation("com.zaxxer:HikariCP:7.1.0")
    implementation("org.xerial:sqlite-jdbc:3.53.2.0")
    implementation("io.github.revxrsal:lamp.common:4.0.0-rc.17")
    implementation("io.github.revxrsal:lamp.bukkit:4.0.0-rc.17")
    implementation("io.github.revxrsal:lamp.brigadier:4.0.0-rc.17")
}

val targetJavaVersion = 21
kotlin {

    compilerOptions {
        javaParameters = true
    }

    jvmToolchain(targetJavaVersion)
}

spotless {
    kotlin {
        target("src/**/*.kt")
        ktlint()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.runServer {
    minecraftVersion("1.21.11")
}

tasks {
    shadowJar {
        minimize {
            // JDBC drivers are loaded via ServiceLoader/Class.forName, minimize()'s bytecode
            // reachability analysis can't see that and would otherwise strip them.
            exclude(dependency("org.xerial:sqlite-jdbc:.*"))
        }
    }
    build {
        dependsOn(shadowJar)
    }
}

bukkit {
    main = "org.gourmet.gourPillars.GourPillars"
    apiVersion = "1.21"
    name = getName()
    version = getVersion().toString()
    author = "Gourmet"
    depend = listOf("Multiverse-Core")
    softDepend = listOf("PlaceholderAPI")

    permissions {
        register("gpillars.party.create") { default = Default.TRUE }
        register("gpillars.party.invite") { default = Default.TRUE }
        register("gpillars.party.accept") { default = Default.TRUE }
        register("gpillars.party.remove") { default = Default.TRUE }
        register("gpillars.party.leave") { default = Default.TRUE }
        register("gpillars.party.disband") { default = Default.TRUE }
        register("gpillars.party.promote") { default = Default.TRUE }
        register("gpillars.party.info") { default = Default.TRUE }
        register("gpillars.party.join") { default = Default.TRUE }
        register("gpillars.party.public") { default = Default.OP }
        register("gpillars.party.broadcast") { default = Default.OP }
        register("gpillars.spectate") { default = Default.TRUE }
    }
}
