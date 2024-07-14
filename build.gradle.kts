plugins {
    java
    `maven-publish`
}

group = "fr.xyness"
version = "1.10.0.1"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mikeprimm.com/")
    maven("https://repo.bluecolored.de/releases/")
    maven("https://api.modrinth.com/maven/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT") {
        isTransitive = false
    }
    compileOnly("dev.folia:folia-api:1.20.4-R0.1-SNAPSHOT") {
        isTransitive = false
    }
    implementation("com.zaxxer:HikariCP:4.0.3")
    compileOnly("me.clip:placeholderapi:2.10.9") {
        isTransitive = false
    }
    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1") {
        isTransitive = false
    }
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.4") {
        isTransitive = false
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        isTransitive = false
    }
    compileOnly("us.dynmap:dynmap-api:3.4") {
        isTransitive = false
    }
    compileOnly("de.bluecolored.bluemap:BlueMapAPI:2.7.2") {
        isTransitive = false
    }
    compileOnly("maven.modrinth:pl3xmap:1.21-500") {
        isTransitive = false
    }
    implementation("com.mojang:authlib:1.5.21")
    compileOnly("com.github.GriefPrevention:GriefPrevention:16.18.2") {
        isTransitive = false
    }
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.1")
}

tasks.withType<JavaCompile> {
    sourceCompatibility = "22"
    targetCompatibility = "22"
    options.encoding = "UTF-8"
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "fr.xyness.SCS.SimpleClaimSystem"
    }
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
            url = uri("https://maven.pkg.github.com/xyness/SimpleClaimSystem")
            credentials {
                username = project.findProperty("gpr.user") as String? ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("TOKEN")
            }
        }
    }
}
