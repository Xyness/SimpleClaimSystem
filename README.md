<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-1.18+-green?style=for-the-badge&logo=minecraft" alt="Minecraft 1.18+"/>
  <img src="https://img.shields.io/badge/API-Spigot-orange?style=for-the-badge" alt="Spigot API"/>
  <img src="https://img.shields.io/github/v/release/Xyness/SimpleClaimSystem?style=for-the-badge&label=Version" alt="Version"/>
  <img src="https://img.shields.io/github/license/Xyness/SimpleClaimSystem?style=for-the-badge" alt="License"/>
  <img src="https://img.shields.io/github/actions/workflow/status/Xyness/SimpleClaimSystem/gradle.yml?style=for-the-badge&label=Build" alt="Build"/>
</p>

# SimpleClaimSystem

A powerful and fully configurable chunk-based land protection plugin for Minecraft servers. Players can claim, manage, and customize their territories with an intuitive GUI system.

## Features

- **Chunk-based claiming** with multi-chunk support, radius claiming, and merging
- **Fully configurable GUIs** for claim management, settings, members, bans, and more
- **Per-claim permissions** for visitors, members, and natural events (30+ settings)
- **Economy integration** with Vault for claim purchasing and selling
- **Map visualization** via Dynmap, BlueMap, and Pl3xMap
- **Bedrock Edition support** through Floodgate/Geyser
- **Multi-platform** - built on Spigot API, fully compatible with Paper, Purpur, and Folia
- **Multi-language** support
- **Auto-claim & auto-map** modes for quick territory expansion
- **Protected areas** for server-managed regions
- **PlaceholderAPI** expansion for scoreboards and chat
- **Auto-purge** system for inactive player claims
- **Complete API** with sync and async methods for developers
- **GriefPrevention** migration support

## Compatibility

| Software | Version |
|:--------:|:-------:|
| [Spigot](https://www.spigotmc.org) | 1.18+ |
| [PaperMC](https://papermc.io/downloads/paper) | 1.18+ |
| [Purpur](https://purpurmc.org) | 1.18+ |
| [Folia](https://papermc.io/software/folia) | 1.18+ |

## Integrations

| Plugin | Type |
|:------:|:----:|
| [Vault](https://www.spigotmc.org/resources/vault.34315/) | Economy |
| [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) | Placeholders |
| [GriefPrevention](https://www.spigotmc.org/resources/griefprevention.1884/) | Migration |
| [WorldGuard](https://enginehub.org/worldguard) | Region protection |
| [Dynmap](https://www.spigotmc.org/resources/dynmap.274/) | Map visualization |
| [BlueMap](https://bluemap.bluecolored.de/) | Map visualization |
| [Pl3xMap](https://modrinth.com/mod/pl3xmap) | Map visualization |
| [Floodgate](https://geysermc.org/) | Bedrock support |
| [ItemsAdder](https://www.spigotmc.org/resources/itemsadder.73355/) | Custom items |

## Installation

1. Download the latest release from [Releases](https://github.com/Xyness/SimpleClaimSystem/releases)
2. Place the JAR file in your server's `plugins/` folder
3. Restart the server
4. Configure `plugins/SimpleClaimSystem/config.yml` to your needs

## Building from source

**Requirements:** Java 21+, Gradle 8.9+

```bash
git clone https://github.com/Xyness/SimpleClaimSystem.git
cd SimpleClaimSystem
./gradlew shadowJar
```

The output JAR will be at `build/libs/SimpleClaimSystem-1.13.0.4.jar`.

## API Usage

Add SimpleClaimSystem as a dependency via [JitPack](https://jitpack.io/#Xyness/SimpleClaimSystem):

**Gradle (Kotlin DSL):**
```kotlin
repositories {
    maven("https://jitpack.io")
}

dependencies {
    compileOnly("com.github.Xyness:SimpleClaimSystem:1.13.0.4")
}
```

**Maven:**
```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>

<dependency>
    <groupId>com.github.Xyness</groupId>
    <artifactId>SimpleClaimSystem</artifactId>
    <version>1.13.0.4</version>
    <scope>provided</scope>
</dependency>
```

**Example (sync):**
```java
SimpleClaimSystemAPI api = SimpleClaimSystemAPI_Provider.getAPI();

// Check if a chunk is claimed
boolean claimed = api.isClaimed(chunk);

// Get a player's claims
Set<Claim> claims = api.getPlayerClaims(player);

// Get claim info
String owner = api.getClaimOwnerAt(chunk);
int count = api.getAllClaimsCount();
```

**Example (async):**
```java
// Non-blocking operations with CompletableFuture
api.unclaimAsync(claim).thenAccept(success -> {
    if (success) {
        player.sendMessage("Claim removed!");
    }
});

api.addPlayerToClaimAsync(claim, "PlayerName").thenAccept(success -> {
    // Handle result
});
```

## Commands

| Command | Description | Permission |
|:-------:|:-----------:|:----------:|
| `/claim` | Claim management | `scs.command.claim` |
| `/claims` | View all claims GUI | `scs.command.claims` |
| `/unclaim` | Unclaim territory | `scs.command.unclaim` |
| `/scs` | Admin commands | `scs.admin` |
| `/protectedarea` | Protected areas management | `scs.admin` |

## Links

- **Wiki:** [Documentation](https://celestis.dev/plugins/simpleclaimsystem/wiki/getting-started)
- **Discord:** [Join](https://discord.gg/6sRTGprM95)
- **Issues:** [GitHub Issues](https://github.com/Xyness/SimpleClaimSystem/issues)

## bStats

[![bStats](https://bstats.org/signatures/bukkit/SimpleCLaimSystem.svg)](https://bstats.org/plugin/bukkit/SimpleClaimSystem/21435)

## License

This project is licensed under the [MIT License](LICENSE).
