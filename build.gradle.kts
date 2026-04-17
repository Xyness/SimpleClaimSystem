plugins {
    `java-library`
    `maven-publish`
    alias(libs.plugins.shadow)
}

group = "fr.xyness"
version = "1.13.0.5"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")
    maven("https://repo.opencollab.dev/main/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.mikeprimm.com/") {
        content {
            includeGroup("us.dynmap")
        }
    }
    maven("https://repo.bluecolored.de/releases/")
    maven("https://api.modrinth.com/maven/")
    maven("https://repo.codemc.io/repository/maven-public/")
}

dependencies {
    compileOnly(libs.authlib)
    implementation(libs.hikaricp)
    implementation(libs.bmutils)
    compileOnly(libs.folia.api)
    compileOnly(libs.flow.math)
    compileOnly(libs.floodgate)
    compileOnly(libs.itemsadder)
    compileOnly(libs.bluemap)
    compileOnly(libs.pl3xmap)
    compileOnly(libs.griefprevention)
    compileOnly(libs.bungeecord.chat)
    compileOnly(libs.placeholderapi)
    compileOnly(libs.dynmap)
    compileOnly(libs.dynmap.core)
    compileOnly(libs.worldguard) {
        exclude(group = "org.bstats", module = "bstats-bukkit")
    }
    compileOnly(libs.vault) {
        exclude(group = "org.bukkit", module = "bukkit")
    }

    testImplementation(libs.junit.engine)
    testImplementation(libs.junit.params)
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName.set("SimpleClaimSystem-${project.version}.jar")
    relocate("com.zaxxer.hikari", "fr.xyness.libs.hikari")

    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
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
                username = project.findProperty("gpr.user") as String? ?: System.getenv("GITHUB_ACTOR")
                password = project.findProperty("gpr.token") as String? ?: System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
