plugins {
    id("java-library")
    id("java")
    id("maven-publish")
    id("io.papermc.paperweight.userdev") version "1.7.1" apply false
    id("io.ktor.plugin") version "3.1.0"
}

group = "fr.xyness"
version = "1.11.6.1"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(22))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven {
        url = uri("https://repo.opencollab.dev/main/")
    }
    maven("https://maven.enginehub.org/repo/")
    maven {
        url = uri("https://repo.mikeprimm.com/")
        content {
            includeGroup("us.dynmap")
        }
    }
    maven("https://repo.bluecolored.de/releases/")
    maven("https://api.modrinth.com/maven/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.3-SNAPSHOT")
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT")
    implementation("com.zaxxer:HikariCP:4.0.3")
    compileOnly(files("libs/PlaceholderAPI-2.11.6.jar"))
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
    }
    compileOnly(files("libs/Dynmap-3.7-beta-6-spigot.jar"))
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2")
    compileOnly("maven.modrinth:pl3xmap:1.21-500")
    implementation("com.mojang:authlib:1.5.21")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.2")
    compileOnly("net.md-5:bungeecord-chat:1.16-R0.4")
    implementation("com.flowpowered:flow-math:1.0.3")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "fr.xyness.SCS.SimpleClaimSystem"
    }
}

application {
       mainClass.set("fr.xyness.SCS.SimpleClaimSystem")
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Xyness/SimpleClaimSystem")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}

ktor {
    fatJar {
        archiveFileName.set("fat.jar")
    }
}
