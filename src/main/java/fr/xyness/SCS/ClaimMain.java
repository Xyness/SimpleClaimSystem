package fr.xyness.SCS;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.io.FileReader;
import java.io.IOException;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ClaimMain {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** List of claims by chunk. */
    private Map<Chunk, Claim> listClaims = new HashMap<>();

    /** Mapping of player uuid to their claims. */
    private Map<UUID, Set<Claim>> playerClaims = new HashMap<>();
    
    /** Key UUID for protected areas */
    public static final UUID SERVER_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    /** Mapping of players to their original locations. */
    private final Map<Player, Location> playerLocations = new HashMap<>();

    /** Mapping of players to their active Bukkit tasks. */
    private final Map<Player, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    /** Mapping of players to their active Folia tasks. */
    private final Map<Player, ScheduledTask> activeFoliaTasks = new ConcurrentHashMap<>();

    /** Set of command arguments for /claim. */
    private Set<String> commandArgsClaim = Set.of("add", "autoclaim", "automap", "list",
            "map", "members", "remove", "see", "setdesc", "setname", "setspawn", "settings", 
            "tp", "chat", "ban", "unban", "bans", "owner", "autofly", "fly", "merge", "sell",
            "cancel", "addchunk", "removechunk", "chunks", "main", "kick");
    
    /** Set of command arguments for /scs. */
    private Set<String> commandArgsScs = Set.of("reload", "config-reload", "transfer", "list", "player", "group", "forceunclaim", "setowner", "set-lang", 
            "reset-all-player-claims-settings", "reset-all-admin-claims-settings","admin");
    
    /** Set of command arguments for /parea. */
    private Set<String> commandArgsParea = Set.of("setdesc", "settings", "setname", "members", "tp",
    		"list", "ban", "unban", "bans", "add", "remove", "unclaim", "main", "kick");
    
    /** Instance of instance. */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for 
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMain(SimpleClaimSystem instance) {
    	this.instance = instance;
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Clears all maps and variables.
     */
    public void clearAll() {
        playerClaims.clear();
        playerLocations.clear();
        listClaims.clear();
        activeTasks.values().stream().forEach(t -> t.cancel());
        activeTasks.clear();
        if(instance.isFolia()) {
        	activeFoliaTasks.values().stream().forEach(t -> t.cancel());
        	activeFoliaTasks.clear();
        }
    }
    
    public void clearDataForPlayer(Player player) {
    	playerLocations.remove(player);
    	if(activeTasks.containsKey(player)) {
    		activeTasks.get(player).cancel();
    		activeTasks.remove(player);
    	}
    	if(instance.isFolia() && activeFoliaTasks.containsKey(player)) {
    		activeFoliaTasks.get(player).cancel();
    		activeFoliaTasks.remove(player);
    	}
    }

    /**
     * Sends a message to a player.
     *
     * @param player  the player to send the message to
     * @param message the message to send
     * @param type    the type of message (ACTION_BAR, SUBTITLE, TITLE, CHAT, BOSSBAR)
     */
    public void sendMessage(Player player, String message, String type) {
        switch (type) {
            case "ACTION_BAR":
                player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                return;
            case "SUBTITLE":
                player.sendTitle("", message, 0, 25, 0);
                return;
            case "TITLE":
                player.sendTitle(message, "", 0, 25, 0);
                return;
            case "CHAT":
                player.sendMessage(message);
                return;
            case "BOSSBAR":
                sendBossbarMessage(player, message);
                return;
        }
    }

    /**
     * Sends a boss bar message to a player.
     *
     * @param player  the player to send the message to
     * @param message the message to send
     */
    public void sendBossbarMessage(Player player, String message) {
        BossBar b = instance.getBossBars().checkBossBar(player);
        b.setTitle(message);
        b.setVisible(true);
        b.setColor(BarColor.RED);

        Runnable updateTask = () -> {
            if (!player.isOnline()) {
                return;
            }

            final int[] counter = {20};
            Runnable countdownTask = () -> {
                if (counter[0] <= 0) {
                    b.setColor(BarColor.valueOf(instance.getSettings().getSetting("bossbar-color")));
                    b.setProgress(1);
                    instance.getBossBars().activeBossBar(player, player.getLocation().getChunk());
                } else {
                    counter[0]--;
                    b.setProgress(counter[0] / 20.0);
                }
            };

            if (instance.isFolia()) {
                if (activeFoliaTasks.containsKey(player)) {
                    activeFoliaTasks.get(player).cancel();
                }
                ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(instance, subtask -> {
                    countdownTask.run();
                    if (!player.isOnline()) {
                        subtask.cancel();
                    }
                    if (counter[0] <= 0) {
                        subtask.cancel();
                        b.setColor(BarColor.valueOf(instance.getSettings().getSetting("bossbar-color")));
                        b.setProgress(1);
                        instance.getBossBars().activeBossBar(player, player.getLocation().getChunk());
                    }
                }, 0, 100, TimeUnit.MILLISECONDS);
                activeFoliaTasks.put(player, task);
            } else {
                if (activeTasks.containsKey(player)) {
                    activeTasks.get(player).cancel();
                }
                BukkitTask task = new BukkitRunnable() {
                    public void run() {
                        countdownTask.run();
                        if (!player.isOnline()) {
                            this.cancel();
                        }
                        if (counter[0] <= 0) {
                            this.cancel();
                            b.setColor(BarColor.valueOf(instance.getSettings().getSetting("bossbar-color")));
                            b.setProgress(1);
                            instance.getBossBars().activeBossBar(player, player.getLocation().getChunk());
                        }
                    }
                }.runTaskTimer(instance, 0L, 2L);
                activeTasks.put(player, task);
            }
        };
        updateTask.run();
    }
    
    /**
     * Returns the number separated with commas for big numbers.
     * 
     * @param text The number in string format.
     * @return The string with new format.
     */
    public String getNumberSeparate(String text) {
        if (text.contains(".")) {
            String[] parts = text.split("\\.");
            return getNumberSeparate(parts[0]) + "." + parts[1];
        }

        StringBuilder sb = new StringBuilder(text);
        int length = sb.length();

        for (int i = length - 3; i > 0; i -= 3) {
            sb.insert(i, ',');
        }

        return sb.toString();
    }

    
    // ********************
    // *  CLAIMS Methods  *
    // ********************


    /**
     * Gets a claim by chunk.
     *
     * @param chunk The chunk to get the claim for
     * @return The claim associated with the chunk, or null if none exists
     */
    public Claim getClaim(Chunk chunk) {
        return listClaims.get(chunk);
    }
    
    /**
     * Gets a claim by its name.
     *
     * @param name The name of the target claim.
     * @param owner The owner.
     * @return The claim associated with the name, or null if none exists
     */
    public Claim getClaimByName(String name, Player owner) {
        return playerClaims.getOrDefault(owner.getUniqueId(), new HashSet<>()).stream()
                .filter(claim -> claim.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets a claim by its name.
     *
     * @param name The name of the target claim.
     * @param ownerUUID The uuid of owner.
     * @return The claim associated with the name, or null if none exists
     */
    public Claim getClaimByName(String name, UUID ownerUUID) {
        return playerClaims.getOrDefault(ownerUUID, new HashSet<>()).stream()
                .filter(claim -> claim.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets a protected area by its name.
     *
     * @param name The name of the target claim.
     * @return The claim associated with the name, or null if none exists
     */
    public Claim getProtectedAreaByName(String name) {
    	return playerClaims.getOrDefault(SERVER_UUID, new HashSet<>()).stream()
    			.filter(claim -> claim.getName().equalsIgnoreCase(name))
    			.findFirst()
    			.orElse(null);
    }
    
    /**
     * Gets the set of protected areas claim object.
     * 
     * @return The set of claim of protected areas.
     */
    public Set<Claim> getProtectedAreas(){
    	return playerClaims.getOrDefault(SERVER_UUID, new HashSet<>());
    }
    
    /**
     * Gets all chunks of all claims of a player
     * 
     * @param owner The name of the owner of claims
     * @return A set of chunks
     */
    public Set<Chunk> getAllChunksFromAllClaims(String owner) {
        return listClaims.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    /**
     * Gets a set of claims in sale of a player.
     *
     * @param owner The name of the owner.
     * @return A set of claims in sale
     */
    public Set<Claim> getClaimsInSale(String owner) {
        return listClaims.values()
                .stream()
                .filter(claim -> claim.getOwner().equals(owner) && claim.getSale())
                .collect(Collectors.toSet());
    }

    /**
     * Gets the list of claims for the specified owner.
     *
     * @param owner The owner of the claims.
     * @return A list of claims belonging to the specified owner.
     */
    public Set<Claim> getPlayerClaims(String owner) {
        return listClaims.values()
                         .stream()
                         .filter(claim -> claim.getOwner().equals(owner))
                         .collect(Collectors.toSet());
    }
    
    /**
     * Gets the list of claims for the specified owner.
     *
     * @param targetUUID The owner's uuid of the claims.
     * @return A list of claims belonging to the specified owner.
     */
    public Set<Claim> getPlayerClaims(UUID targetUUID) {
        return playerClaims.getOrDefault(targetUUID, new HashSet<>());
    }
    
    /**
     * Sets the new claims list for a UUID.
     * 
     * @param targetUUID The owner's uuid of the claims
     * @param claims The set of new claims
     */
    public void setPlayerClaims(UUID targetUUID, Set<Claim> claims) {
    	playerClaims.put(targetUUID, claims);
    }

    /**
     * Gets the claim from the given chunk.
     *
     * @param chunk the chunk to get the claim from
     * @return the claim, or null if no claim exists for the chunk
     */
    public Claim getClaimFromChunk(Chunk chunk) {
        return listClaims.get(chunk);
    }
    
    /**
     * Gets the set of chunks in String format
     *
     * @param claim The target claim
     * @return the set of chunk information strings, or an empty set if no chunks are present
     */
    public Set<String> getStringChunkFromClaim(Claim claim) {
        return claim.getChunks().stream()
                .map(chunk -> chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ())
                .collect(Collectors.toSet());
    }

    /**
     * Gets the number of claims a player has.
     *
     * @param targetUUID the uuid of target player
     * @return the number of claims the player has
     */
    public int getPlayerClaimsCount(UUID targetUUID) {
        return playerClaims.getOrDefault(targetUUID, new HashSet<>()).size();
    }

    /**
     * Gets all the claim owners (excluding admin).
     *
     * @return a map of claim owners and their claim counts
     */
    public Map<String, Integer> getClaimsOwnersGui() {
    	Map<String,Integer> players = new HashMap<>();
    	playerClaims.keySet().stream().forEach(UUID -> {
    		if(!UUID.equals(SERVER_UUID)) {
    			players.put(instance.getPlayerMain().getPlayerName(UUID), playerClaims.get(UUID).size());
    		}
    	});
        return players;
    }
    
    /**
     * Gets all the claim owners (excluding admin).
     *
     * @return a set of claim owners
     */
    public Set<String> getClaimsOwners() {
        return playerClaims.values()
                .stream()
                .filter(claims -> !claims.isEmpty())
                .map(claims -> claims.iterator().next().getOwner())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all claimed chunks.
     *
     * @return a set of all claimed chunks
     */
    public Set<Chunk> getAllClaimsChunk() {
        return listClaims.keySet();
    }
    
    /**
     * Gets all claims.
     *
     * @return a set of all claims
     */
    public Set<Claim> getAllClaims() {
        return new HashSet<>(listClaims.values());
    }
    
    /**
     * Gets the claims count (total)
     * 
     * @return an integer of the total claims count
     */
    public int getAllClaimsCount() {
    	return listClaims.values().size();
    }
    
    /**
     * Gets the claims count of protected areas
     * 
     * @return an integer of the protected areas claims count
     */
    public int getProtectedAreasCount() {
    	return playerClaims.get(SERVER_UUID).size();
    }

    /**
     * Gets all members of all claims owned by the specified player.
     *
     * @param owner the owner of the claims
     * @return a set of all members of the owner's claims
     */
    public Set<String> getAllMembersOfAllPlayerClaim(String owner) {
        return listClaims.values().stream()
                .filter(claim -> claim.getOwner().equals(owner))
                .flatMap(claim -> claim.getMembers().stream())
                .map(uuid -> instance.getPlayerMain().getPlayerName(uuid))
                .collect(Collectors.toSet());
    }

    /**
     * Gets all online claim owners and their claim counts.
     *
     * @return a map of online claim owners and their claim counts
     */
    public Map<String, Integer> getClaimsOnlineOwners() {
        return playerClaims.values().stream()
                .flatMap(Set::stream) // Flatten the list of sets into a single stream of claims
                .filter(claim -> {
                    Player player = Bukkit.getPlayer(claim.getOwner());
                    return player != null && player.isOnline() && !claim.getUUID().equals(SERVER_UUID);
                })
                .collect(Collectors.toConcurrentMap(
                        Claim::getOwner, // Key: owner name
                        claim -> 1, // Initial value for the count
                        Integer::sum, // Merge function: sum the counts
                        ConcurrentHashMap::new // Map supplier: use ConcurrentHashMap
                ));
    }
    
    /**
     * Gets all claim owners with claims in sale and their claim counts.
     *
     * @return a map of claim owners with claims in sale and their claim counts
     */
    public Map<String, Integer> getClaimsOwnersWithSales() {
        return playerClaims.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals(SERVER_UUID))
                .filter(entry -> entry.getValue().stream().anyMatch(Claim::getSale))
                .collect(Collectors.toMap(
                        entry -> {
                            Set<Claim> claims = entry.getValue();
                            return claims.isEmpty() ? "Unknown" : claims.iterator().next().getOwner();
                        }, 
                        entry -> (int) entry.getValue().stream().filter(Claim::getSale).count(),
                        (oldValue, newValue) -> oldValue // In case of key collisions
                ));
    }

    /**
     * Gets all offline claim owners and their claim counts.
     *
     * @return a map of offline claim owners and their claim counts
     */
    public Map<String, Integer> getClaimsOfflineOwners() {
        return playerClaims.values().stream()
                .flatMap(Set::stream) // Flatten the list of sets into a single stream of claims
                .filter(claim -> {
                    Player player = Bukkit.getPlayer(claim.getOwner());
                    return player == null && !claim.getUUID().equals(SERVER_UUID);
                })
                .collect(Collectors.toConcurrentMap(
                        Claim::getOwner, // Key: owner name
                        claim -> 1, // Initial value for the count
                        Integer::sum, // Merge function: sum the counts
                        ConcurrentHashMap::new // Map supplier: use ConcurrentHashMap
                ));
    }

    /**
     * Gets the claims where the specified player is a member but not the owner.
     *
     * @param playerName the name of the player
     * @return a set of claims where the player is a member but not the owner
     */
    public Set<Claim> getClaimsWhereMemberNotOwner(String playerName) {
        return listClaims.entrySet().stream()
                .filter(entry -> !entry.getValue().getOwner().equals(playerName) && entry.getValue().getMembers().contains(playerName))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());
    }

    /**
     * Gets the names of all claims owned by the specified owner.
     *
     * @param owner the owner of the claims
     * @return a set of claim names owned by the owner
     */
    public Set<String> getClaimsNameFromOwner(String owner) {
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner))
                .map(entry -> entry.getValue().getName())
                .collect(Collectors.toSet());
    }

    /**
     * Gets the names of all claims in sale owned by the specified owner.
     *
     * @param owner the owner of the claims
     * @return a set of claim names in sale owned by the owner
     */
    public Set<String> getClaimsNameInSaleFromOwner(String owner) {
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner) && entry.getValue().getSale())
                .map(entry -> entry.getValue().getName())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all members in claim chat mode for the specified player.
     *
     * @param playerName the name of the player
     * @return a list of all members in claim chat mode for the player
     */
    public List<String> getAllMembersWithPlayerParallel(String playerName) {
        return listClaims.values().stream()
                .filter(claim -> claim.getMembers().contains(instance.getPlayerMain().getPlayerUUID(playerName)))
                .flatMap(claim -> claim.getMembers().stream())
                .map(uuid -> instance.getPlayerMain().getPlayerName(uuid))
                .filter(member -> !member.equals(playerName))
                .distinct()
                .collect(Collectors.toList());
    }
    
    /**
     * Converts a set of UUIDs to a set of player names using instance.getPlayerMain().getPlayerName(UUID).
     *
     * @param uuids The set of UUIDs to be converted.
     * @return A set of player names.
     */
    public Set<String> convertUUIDSetToStringSet(Set<UUID> uuids) {
        return uuids.stream()
                .map(uuid -> instance.getPlayerMain().getPlayerName(uuid))
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
    
    /**
     * Checks if all chunks in the set are from the same world.
     *
     * @param chunks The set of chunks to check.
     * @return true if all chunks are from the same world, false otherwise.
     */
    public boolean areChunksInSameWorld(Set<Chunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return true; // Or false depending on the desired logic for null or empty sets
        }

        World firstWorld = null;
        for (Chunk chunk : chunks) {
            if (firstWorld == null) {
                firstWorld = chunk.getWorld();
            } else if (!chunk.getWorld().equals(firstWorld)) {
                return false;
            }
        }

        return true;
    }
    
    /**
     * Checks if at least one chunk in the given set is adjacent to the specified chunk.
     *
     * @param chunks The set of chunks to check.
     * @param targetChunk The chunk to check adjacency against.
     * @return true if at least one chunk is adjacent to the target chunk, false otherwise.
     */
    public boolean isAnyChunkAdjacent(Set<Chunk> chunks, Chunk targetChunk) {
        int targetX = targetChunk.getX();
        int targetZ = targetChunk.getZ();
        World targetWorld = targetChunk.getWorld();

        for (Chunk chunk : chunks) {
            if (!chunk.getWorld().equals(targetWorld)) {
                continue; // Skip chunks from different worlds
            }
            int x = chunk.getX();
            int z = chunk.getZ();

            // Check adjacency (4 directions)
            if ((x == targetX + 1 && z == targetZ) || 
                (x == targetX - 1 && z == targetZ) || 
                (x == targetX && z == targetZ + 1) || 
                (x == targetX && z == targetZ - 1)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if at least one chunk in the first set is adjacent to any chunk in the second set.
     *
     * @param set1 The first set of chunks.
     * @param set2 The second set of chunks.
     * @return true if at least one chunk from set1 is adjacent to any chunk in set2, false otherwise.
     */
    public boolean isAnyChunkAdjacentBetweenSets(Set<Chunk> set1, Set<Chunk> set2) {
        for (Chunk chunk1 : set1) {
            int x1 = chunk1.getX();
            int z1 = chunk1.getZ();
            World world1 = chunk1.getWorld();

            for (Chunk chunk2 : set2) {
                if (!chunk2.getWorld().equals(world1)) {
                    continue; // Skip chunks from different worlds
                }
                int x2 = chunk2.getX();
                int z2 = chunk2.getZ();

                // Check adjacency (4 directions)
                if ((x1 == x2 + 1 && z1 == z2) || 
                    (x1 == x2 - 1 && z1 == z2) || 
                    (x1 == x2 && z1 == z2 + 1) || 
                    (x1 == x2 && z1 == z2 - 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Teleports the player to the specified location.
     *
     * @param player the player to teleport
     * @param loc    the location to teleport to
     */
    public void goClaim(Player player, Location loc) {
        if (loc == null) return;

        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
        int delay = cPlayer.getDelay();

        if (instance.getPlayerMain().checkPermPlayer(player, "scs.bypass") || delay == 0) {
            teleportPlayer(player, loc);
            player.sendMessage(instance.getLanguage().getMessage("teleportation-success"));
            return;
        }

        player.sendMessage(instance.getLanguage().getMessage("teleportation-in-progress").replace("%delay%", String.valueOf(delay)));
        Location originalLocation = player.getLocation().clone();
        playerLocations.put(player, originalLocation);

        Runnable teleportTask = createTeleportTask(player, loc, originalLocation, delay);
        if (instance.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(instance, task -> {
                teleportTask.run();
                if (!playerLocations.containsKey(player)) task.cancel();
            }, 0, 500, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                public void run() {
                    teleportTask.run();
                    if (!playerLocations.containsKey(player)) this.cancel();
                }
            }.runTaskTimer(instance, 0L, 10L);
        }
    }

    /**
     * Teleports the player.
     *
     * @param player the player to teleport
     * @param loc    the location to teleport to
     */
    public void teleportPlayer(Player player, Location loc) {
        if (instance.isFolia()) {
            player.teleportAsync(loc).thenAccept(success -> instance.getBossBars().activeBossBar(player, loc.getChunk()));
        } else {
            player.teleport(loc);
        }
    }

    /**
     * Creates the teleport task for goClaim method.
     *
     * @param player           the player to teleport
     * @param loc              the location to teleport to
     * @param originalLocation the original location of the player
     * @param delay            the delay for teleportation
     * @return the teleport task
     */
    private Runnable createTeleportTask(Player player, Location loc, Location originalLocation, int delay) {
        return new Runnable() {
            int countdown = delay * 2;

            @Override
            public void run() {
                if (!player.isOnline() || !playerLocations.containsKey(player)) {
                    playerLocations.remove(player);
                    return;
                }

                if (!instance.getSettings().getBooleanSetting("teleportation-delay-moving")) {
                    Location currentLocation = player.getLocation();
                    if (!currentLocation.equals(originalLocation)) {
                        player.sendMessage(instance.getLanguage().getMessage("teleportation-canceled-moving"));
                        playerLocations.remove(player);
                        return;
                    }
                }

                if (countdown <= 0) {
                    teleportPlayer(player, loc);
                    player.sendMessage(instance.getLanguage().getMessage("teleportation-success"));
                    playerLocations.remove(player);
                } else {
                    countdown--;
                }
            }
        };
    }

    /**
     * Checks if the given claim name is already used.
     *
     * @param ownerId the owner's uuid of the claim
     * @param name  the name of the claim
     * @return true if the name is already used, false otherwise
     */
    public boolean checkName(UUID ownerId, String name) {
        return playerClaims.getOrDefault(ownerId, new HashSet<>()).stream()
                .noneMatch(claim -> claim.getName().toLowerCase().equals(name.toLowerCase()));
    }

    /**
     * Gets the claim name by the chunk.
     *
     * @param chunk the chunk to get the name from
     * @return the claim name
     */
    public String getClaimNameByChunk(Chunk chunk) {
        Claim claim = listClaims.getOrDefault(chunk, null);
        return claim == null ? "" : claim.getName();
    }

    /**
     * Gets the claim coordinates as a string by the claim.
     *
     * @param claim the claim to get the coordinates from
     * @return the claim coordinates as a string
     */
    public String getClaimCoords(Claim claim) {
        Location loc = claim.getLocation();
        String world = loc.getWorld().getName();
        String x = String.valueOf(Math.round(loc.getX() * 10.0 / 10.0));
        String y = String.valueOf(Math.round(loc.getY() * 10.0 / 10.0));
        String z = String.valueOf(Math.round(loc.getZ() * 10.0 / 10.0));
        return world + ", " + x + ", " + y + ", " + z;
    }
    
    /**
     * Finds a free ID for a new claim.
     *
     * @param targetUUID The target player uuid
     * @return the next available ID
     */
    public int findFreeId(UUID targetUUID) {
        return playerClaims.getOrDefault(targetUUID, Collections.emptySet())
                .stream()
                .mapToInt(Claim::getId)
                .max()
                .orElse(-1) + 1;
    }
    
    /**
     * Finds a free ID for a new protected area.
     *
     * @return the next available ID
     */
    public int findFreeIdProtectedArea() {
        return playerClaims.getOrDefault(SERVER_UUID, Collections.emptySet())
                .stream()
                .mapToInt(Claim::getId)
                .max()
                .orElse(-1) + 1;
    }
    
    /**
     * Gets the location of the center of the chunk with the Y-coordinate at the maximum height.
     *
     * @param chunk The chunk for which the center location is to be calculated.
     * @return The location at the center of the chunk with the Y-coordinate at the maximum height.
     */
    public Location getCenterLocationOfChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int centerX = (chunk.getX() << 4) + 8;
        int centerZ = (chunk.getZ() << 4) + 8;
        int maxY = world.getHighestBlockYAt(centerX, centerZ);
        return new Location(world, centerX, maxY, centerZ);
    }
    
    /**
     * Replaces the character at the specified index in the given string with the specified new character.
     *
     * @param str the original string
     * @param index the index of the character to be replaced
     * @param newChar the new character to replace the old character
     * @return a new string with the character at the specified index replaced by the new character, 
     *         or {@code null} if the input string is {@code null}
     * @throws IllegalArgumentException if the index is out of bounds (less than 0 or greater than or equal to the length of the string)
     */
    public static String replaceCharAt(String str, int index, char newChar) {
        if (str == null) {
            return null;
        }
        if (index < 0 || index >= str.length()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        StringBuilder sb = new StringBuilder(str);
        sb.setCharAt(index, newChar);
        return sb.toString();
    }

    /**
     * Removes the character at the specified index in the given string.
     *
     * @param str the original string
     * @param index the index of the character to be removed
     * @return a new string with the character at the specified index removed, 
     *         or {@code null} if the input string is {@code null}
     * @throws IllegalArgumentException if the index is out of bounds (less than 0 or greater than or equal to the length of the string)
     */
    public static String removeCharAt(String str, int index) {
        if (str == null) {
            return null;
        }
        if (index < 0 || index >= str.length()) {
            throw new IllegalArgumentException("Index out of bounds");
        }
        StringBuilder sb = new StringBuilder(str);
        sb.deleteCharAt(index);
        return sb.toString();
    }
    
    /**
     * Removes characters from the given string at the specified indices.
     *
     * @param str the original string
     * @param indices the list of indices of characters to be removed
     * @return a new string with the characters at the specified indices removed
     */
    public static String removeCharsAtIndices(String str, List<Integer> indices) {
        if (str == null) {
            return null;
        }

        // Sort indices in descending order
        Collections.sort(indices, Collections.reverseOrder());

        StringBuilder sb = new StringBuilder(str);
        for (int index : indices) {
            if (index < 0 || index >= sb.length()) {
                throw new IllegalArgumentException("Index out of bounds: " + index);
            }
            sb.deleteCharAt(index);
        }

        return sb.toString();
    }
    
    /**
     * Class for converting claim
     */
    private static class ClaimData {
        int id;
        String owner_uuid;
        String owner_name;
        String claim_name;
        String claim_description;
        String chunks;
        String world_name;
        String location;
        String members;
        String permissions;
        boolean for_sale;
        double sale_price;
        String bans;

        public ClaimData(int id, String owner_uuid, String owner_name, String claim_name, String claim_description, String chunks, String world_name, String location, String members, String permissions, boolean for_sale, double sale_price, String bans) {
            this.id = id;
            this.owner_uuid = owner_uuid;
            this.owner_name = owner_name;
            this.claim_name = claim_name;
            this.claim_description = claim_description;
            this.chunks = chunks;
            this.world_name = world_name;
            this.location = location;
            this.members = members;
            this.permissions = permissions;
            this.for_sale = for_sale;
            this.sale_price = sale_price;
            this.bans = bans;
        }
    }
    
    /**
     * Converts old local to new local
     */
    public void convertLocalToNewLocal() {
    	
    	StringBuilder natural = new StringBuilder();
    	StringBuilder visitors = new StringBuilder();
    	StringBuilder members_ = new StringBuilder();

        for (String key : instance.getSettings().getDefaultValues().get("natural").keySet()) {
            if (instance.getSettings().getDefaultValues().get("natural").get(key)) {
                natural.append("1");
                continue;
            }
            natural.append("0");
        }
        
        for (String key : instance.getSettings().getDefaultValues().get("visitors").keySet()) {
            if (instance.getSettings().getDefaultValues().get("visitors").get(key)) {
                visitors.append("1");
                continue;
            }
            visitors.append("0");
        }
        
        for (String key : instance.getSettings().getDefaultValues().get("members").keySet()) {
            if (instance.getSettings().getDefaultValues().get("members").get(key)) {
                members_.append("1");
                continue;
            }
            members_.append("0");
        }
        
        instance.getSettings().setDefaultValuesCode(natural.toString(),"natural");
        instance.getSettings().setDefaultValuesCode(visitors.toString(),"visitors");
        instance.getSettings().setDefaultValuesCode(members_.toString(),"members");
    	
    	instance.info("Starting the conversion.");
        HikariConfig localConfig = new HikariConfig();
        localConfig.setJdbcUrl("jdbc:sqlite:plugins/SimpleClaimSystem/claims.db");
        localConfig.addDataSourceProperty("busy_timeout", "5000"); // Set busy timeout to 5 seconds

        int[] count = {0};

        try (HikariDataSource localDataSource = new HikariDataSource(localConfig);
             Connection connection = localDataSource.getConnection()) {

            String getQuery = "SELECT * FROM scs_claims";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery);
                 ResultSet rs = preparedStatement.executeQuery()) {

                List<CompletableFuture<ClaimData>> futureClaims = new ArrayList<>();

                while (rs.next()) {
                    int id = rs.getInt("id");
                    String uuid = rs.getString("uuid");
                    String owner_uuid = uuid.equals("aucun") ? SERVER_UUID.toString() : uuid;
                    String owner_name = rs.getString("name").equals("admin") ? "*" : rs.getString("name");
                    String claim_name = rs.getString("claim_name");
                    String claim_description = rs.getString("claim_description");
                    String world_name = rs.getString("World");
                    World check_world = Bukkit.getWorld(world_name);
                    World world = check_world == null ? Bukkit.createWorld(new WorldCreator(world_name)) : check_world;
                    if (world == null) continue;
                    String location = rs.getString("Location");
                    String members = rs.getString("Members");
                    String permissions = instance.getSettings().getDefaultValuesCode("all");
                    
                    boolean for_sale = rs.getBoolean("isSale");
                    double sale_price = rs.getDouble("SalePrice");
                    String bans = rs.getString("Bans");
                    Set<Chunk> chunks = ConcurrentHashMap.newKeySet();

                    List<Integer> X = Arrays.stream(rs.getString("X").split(";"))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    List<Integer> Z = Arrays.stream(rs.getString("Z").split(";"))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    if (X.size() != Z.size()) continue;

                    if (instance.isFolia()) {
                        List<CompletableFuture<Void>> futures = new ArrayList<>();
                        for (int i = 0; i < X.size(); i++) {
                            int x = X.get(i);
                            int z = Z.get(i);
                            CompletableFuture<Void> future = world.getChunkAtAsync(x, z).thenAccept(chunks::add).exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });
                            futures.add(future);
                        }

                        CompletableFuture<ClaimData> claimFuture = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                                .thenApply(v -> {
                                    count[0]++;
                                    return new ClaimData(id, owner_uuid, owner_name, claim_name, claim_description, serializeChunks(chunks), world_name, location, members, permissions, for_sale, sale_price, bans);
                                });
                        futureClaims.add(claimFuture);
                    } else {
                        for (int i = 0; i < X.size(); i++) {
                            int x = X.get(i);
                            int z = Z.get(i);
                            Chunk chunk = world.getChunkAt(x, z);
                            chunks.add(chunk);
                        }
                        count[0]++;
                        ClaimData claimData = new ClaimData(id, owner_uuid, owner_name, claim_name, claim_description, serializeChunks(chunks), world_name, location, members, permissions, for_sale, sale_price, bans);
                        futureClaims.add(CompletableFuture.completedFuture(claimData));
                    }
                }

                // Wait for all async operations to complete
                CompletableFuture<Void> allClaimsFuture = CompletableFuture.allOf(futureClaims.toArray(new CompletableFuture[0]));
                allClaimsFuture.thenRun(() -> {
                    try (Connection targetConnection = instance.getDataSource().getConnection();
                         PreparedStatement stmt = targetConnection.prepareStatement("INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions, for_sale, sale_price, bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {

                        for (CompletableFuture<ClaimData> future : futureClaims) {
                            ClaimData claimData;
                            try {
                                claimData = future.get();
                            } catch (Exception e) {
                                e.printStackTrace();
                                continue;
                            }
                            stmt.setInt(1, claimData.id);
                            stmt.setString(2, claimData.owner_uuid);
                            stmt.setString(3, claimData.owner_name);
                            stmt.setString(4, claimData.claim_name);
                            stmt.setString(5, claimData.claim_description);
                            stmt.setString(6, claimData.chunks);
                            stmt.setString(7, claimData.world_name);
                            stmt.setString(8, claimData.location);
                            stmt.setString(9, claimData.members);
                            stmt.setString(10, claimData.permissions);
                            stmt.setBoolean(11, claimData.for_sale);
                            stmt.setDouble(12, claimData.sale_price);
                            stmt.setString(13, claimData.bans);
                            stmt.addBatch();
                        }

                        stmt.executeBatch();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    // Send logs
                    instance.info(net.md_5.bungee.api.ChatColor.DARK_GREEN + getNumberSeparate(String.valueOf(count[0])) + " claims converted.");

                }).exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                }).join(); // Ensure all async operations complete before finishing
                
                Runnable task = () -> {
                	if (localDataSource != null) {
                        localDataSource.close();
                    }

                    // Delete the database file
                    File dbFile = new File("plugins/SimpleClaimSystem/claims.db");
                    if (dbFile.exists()) {
                        boolean isDeleted = dbFile.delete();
                        if (isDeleted) {
                            instance.getLogger().info("Old database file deleted successfully.");
                        }
                    }
                };
                
                if(instance.isFolia()) {
                	Bukkit.getAsyncScheduler().runDelayed(instance, t -> task.run(), 10, TimeUnit.SECONDS);
                } else {
                    Bukkit.getScheduler().runTaskLater(instance, new Runnable() {
                        @Override
                        public void run() {
                        	task.run();
                        }
                    }, 200L); // 20 ticks = 1 seconde, donc 200 ticks = 10 secondes
                }

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Converts old distant to new distant
     */
    public void convertDistantToNewDistant() {
    	
    	StringBuilder natural = new StringBuilder();
    	StringBuilder visitors = new StringBuilder();
    	StringBuilder members_ = new StringBuilder();

        for (String key : instance.getSettings().getDefaultValues().get("natural").keySet()) {
            if (instance.getSettings().getDefaultValues().get("natural").get(key)) {
                natural.append("1");
                continue;
            }
            natural.append("0");
        }
        
        for (String key : instance.getSettings().getDefaultValues().get("visitors").keySet()) {
            if (instance.getSettings().getDefaultValues().get("visitors").get(key)) {
                visitors.append("1");
                continue;
            }
            visitors.append("0");
        }
        
        for (String key : instance.getSettings().getDefaultValues().get("members").keySet()) {
            if (instance.getSettings().getDefaultValues().get("members").get(key)) {
                members_.append("1");
                continue;
            }
            members_.append("0");
        }
        
        instance.getSettings().setDefaultValuesCode(natural.toString(),"natural");
        instance.getSettings().setDefaultValuesCode(visitors.toString(),"visitors");
        instance.getSettings().setDefaultValuesCode(members_.toString(),"members");
    	
    	int count = 0;
        try (Connection connection = instance.getDataSource().getConnection()) {
            String getQuery = "SELECT * FROM scs_claims";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery)) {
                try (ResultSet rs = preparedStatement.executeQuery()) {
                    while (rs.next()) {
        	   
                    	int id = rs.getInt("id");
		        	    String owner_name = rs.getString("name");
		        	    String owner_uuid = owner_name.equals("*") ? SERVER_UUID.toString() : rs.getString("uuid");
		        	    String claim_name = rs.getString("claim_name");
		        	    String claim_description = rs.getString("claim_description");
		        	    String world_name = rs.getString("World");
                        World check_world = Bukkit.getWorld(world_name);
                        World world = check_world == null ? Bukkit.createWorld(new WorldCreator(world_name)) : check_world;
                        if(world == null) continue;
		        	    String location = rs.getString("Location");
		        	    String members = rs.getString("Members");
		        	    String permissions = instance.getSettings().getDefaultValuesCode("all");
		        	    boolean for_sale = rs.getBoolean("isSale");
		        	    Double sale_price = rs.getDouble("SalePrice");
		        	    String bans = rs.getString("Bans");
		        	    Set<Chunk> chunks = ConcurrentHashMap.newKeySet();
		        	   
		                List<Integer> X = Arrays.stream(rs.getString("X").split(";"))
		                		.map(String::trim)
		                        .map(Integer::parseInt)
		                        .collect(Collectors.toList());
		                List<Integer> Z = Arrays.stream(rs.getString("Z").split(";"))
		                        .map(String::trim)
		                        .map(Integer::parseInt)
		                        .collect(Collectors.toList());
		                if(X.size() != Z.size()) continue;
		               
		                Iterator<Integer> xIterator = X.iterator();
		                Iterator<Integer> zIterator = Z.iterator();
		               
		                count++;
		               
		                Runnable task = () -> {
		            	    instance.executeAsync(() -> {
		                        try (PreparedStatement stmt = connection.prepareStatement("INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions, for_sale, sale_price, bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
								    stmt.setInt(1, id);
								    stmt.setString(2, owner_uuid);
								    stmt.setString(3, owner_name);
								    stmt.setString(4, claim_name);
								    stmt.setString(5, claim_description);
								    stmt.setString(6, serializeChunks(chunks));
								    stmt.setString(7, world_name);
								    stmt.setString(8, location);
								    stmt.setString(9, members);
							 	    stmt.setString(10, permissions);
							 	    stmt.setBoolean(11, for_sale);
								    stmt.setDouble(12, sale_price);
								    stmt.setString(13, bans);
								    stmt.executeUpdate();
		                        } catch (SQLException e) {
		                    	    e.printStackTrace();
		                        }
		            	    });
		                };
		               
		                if (instance.isFolia()) {
                            List<CompletableFuture<Void>> futures = new ArrayList<>();

                            while (xIterator.hasNext() && zIterator.hasNext()) {
                                int x = xIterator.next();
                                int z = zIterator.next();
                                CompletableFuture<Void> future = world.getChunkAtAsync(x, z).thenAccept(chunk -> {
                                    chunks.add(chunk);
                                }).exceptionally(ex -> {
                                    ex.printStackTrace();
                                    return null;
                                });
                                futures.add(future);
                            }

                            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                            allOf.thenRun(() -> {
                                task.run();
                            }).exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });
                        } else {
                            while (xIterator.hasNext() && zIterator.hasNext()) {
                                int x = xIterator.next();
                                int z = zIterator.next();
                                Chunk chunk = world.getChunkAt(x, z);
                                chunks.add(chunk);
                            }
                            task.run();
                        }

                    }
                }
            }
            instance.getLogger().info(getNumberSeparate(String.valueOf(count)) + " claims converted.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Imports the claims from GriefPrevention
     */
    public void importFromGriefPrevention(CommandSender sender) {
    	instance.executeAsync(() -> {
    		int[] i = {0};
    		for (me.ryanhamshire.GriefPrevention.Claim claim : GriefPrevention.instance.dataStore.getClaims()) {
        		// Get data of the claim
        		Set<Chunk> chunks = new HashSet<>(claim.getChunks());
        		String owner = claim.getOwnerName();
        		String uuid = claim.getOwnerID().toString();
        		int id = findFreeId(claim.getOwnerID());
        		String claim_name = "claim-"+ String.valueOf(id);
        		
        		// Check if the chunks are in the same world, even skip
        		if(!instance.getMain().areChunksInSameWorld(chunks)) continue;

        		// Check if one of the chunk is not already taken by an other claim, even skip
        		boolean check = false;
        		Chunk last_chunk = null;
        		for(Chunk c : chunks) {
        			last_chunk = c;
        			if(listClaims.containsKey(c)) {
        				check = true;
        			}
        		}
        		if(check) continue;
        		
        		// Check if the selected chunk is not null, even get chunk data
        		if(last_chunk == null);
        		Location loc = getCenterLocationOfChunk(last_chunk);
        		String world = last_chunk.getWorld().getName();
                
                String chunksData = serializeChunks(chunks);
                
                // Update database
                try (Connection connection = instance.getDataSource().getConnection();
                        PreparedStatement stmt = connection.prepareStatement(
                                "INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                       stmt.setInt(1, id);
                   	   stmt.setString(2, uuid);
                       stmt.setString(3, owner);
                       stmt.setString(4, claim_name);
                       stmt.setString(5, instance.getLanguage().getMessage("default-description"));
                       stmt.setString(6, chunksData);
                       stmt.setString(7, world);
                       stmt.setString(8, getLocationString(loc));
                       stmt.setString(9, owner);
                       stmt.setString(10, instance.getSettings().getDefaultValuesCode("all"));
                       stmt.executeUpdate();
                       i[0]++;
                   } catch (SQLException e) {
                       e.printStackTrace();
                   }
        	}
    		instance.executeSync(() -> {
    			sender.sendMessage(getNumberSeparate(String.valueOf(i[0]))+" imported claims, reloading..");
    			Bukkit.dispatchCommand(sender, "scs reload");
    		});
    	});
    }

    /**
     * Transfers local claims database to a distant database.
     */
    public void transferClaims() {
    	instance.executeAsync(() -> {;
            HikariConfig localConfig = new HikariConfig();
            localConfig.setJdbcUrl("jdbc:sqlite:plugins/SimpleClaimSystem/storage.db");
            localConfig.setDriverClassName("org.sqlite.JDBC");
            try (HikariDataSource localDataSource = new HikariDataSource(localConfig);
                 Connection localConn = localDataSource.getConnection();
                 PreparedStatement selectStmt = localConn.prepareStatement("SELECT * FROM scs_claims_1");
                 ResultSet rs = selectStmt.executeQuery();
                 Connection remoteConn = instance.getDataSource().getConnection();
                 PreparedStatement insertStmt = remoteConn.prepareStatement(
                         "INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions, for_sale, sale_price) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                 )) {

                int count = 0;
                while (rs.next()) {
                    for (int i = 1; i <= 11; i++) {
                        insertStmt.setObject(i, rs.getObject(i));
                    }
                    insertStmt.addBatch();
                    count++;
                }
                insertStmt.executeBatch();
                instance.getLogger().info(getNumberSeparate(String.valueOf(count)) + " claims transferred.");
                instance.getLogger().info("Safe reloading..");
                instance.executeSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scs reload"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
    	});
    }
    
    /**
     * Serializes a set of chunks into a Base64 encoded string.
     *
     * @param chunks The set of chunks to serialize.
     * @return A Base64 encoded string representing the serialized chunks, or null if an error occurs.
     */
    private String serializeChunks(Set<Chunk> chunks) {
        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)) {
            for (Chunk chunk : chunks) {
                objectOutputStream.writeInt(chunk.getX());
                objectOutputStream.writeInt(chunk.getZ());
            }
            objectOutputStream.flush();
            String encoded = Base64.getEncoder().encodeToString(byteArrayOutputStream.toByteArray());
            return encoded;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads claims from the database.
     */
    public void loadClaims() {
    	instance.info(" ");
    	instance.info(net.md_5.bungee.api.ChatColor.DARK_GREEN + "Loading claims..");
    	
    	StringBuilder natural = new StringBuilder();
    	StringBuilder visitors = new StringBuilder();
    	StringBuilder members_ = new StringBuilder();

        for (String key : instance.getSettings().getDefaultValues().get("natural").keySet()) {
            if (instance.getSettings().getDefaultValues().get("natural").get(key)) {
                natural.append("1");
                continue;
            }
            natural.append("0");
        }
        
        for (String key : instance.getSettings().getDefaultValues().get("visitors").keySet()) {
            if (instance.getSettings().getDefaultValues().get("visitors").get(key)) {
                visitors.append("1");
                continue;
            }
            visitors.append("0");
        }
        
        for (String key : instance.getSettings().getDefaultValues().get("members").keySet()) {
            if (instance.getSettings().getDefaultValues().get("members").get(key)) {
                members_.append("1");
                continue;
            }
            members_.append("0");
        }
        
        instance.getSettings().setDefaultValuesCode(natural.toString(),"natural");
        instance.getSettings().setDefaultValuesCode(visitors.toString(),"visitors");
        instance.getSettings().setDefaultValuesCode(members_.toString(),"members");

        // Checking permissions (for update or new features)
        try (Connection connection = instance.getDataSource().getConnection()) {
            connection.setAutoCommit(false); // Start transaction
            String insertQuery = "UPDATE scs_claims_1 SET permissions = ? WHERE id = ?";
            String updateQuery = "UPDATE scs_claims_1 SET members = ?, bans = ? WHERE id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
            	PreparedStatement preparedStatement2 = connection.prepareStatement(updateQuery);
                String getQuery = "SELECT * FROM scs_claims_1";
                try (PreparedStatement stat = connection.prepareStatement(getQuery);
                     ResultSet resultSet = stat.executeQuery()) {

                    int batchCount = 0;
                    int batchCount2 = 0;

                    while (resultSet.next()) {
                    	
                    	// Get id 
                    	int id = resultSet.getInt("id");
                    	
                        // Check for update
                        boolean[] isToUpdate = {false};
                    	
                    	String[] parts;
                        // Members data
                        String s_members = resultSet.getString("members");
                        Set<UUID> members = new HashSet<>();
                        if (!s_members.isBlank()) {
                            parts = s_members.split(";");
                            for (String m : parts) {
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(m);
                                } catch (IllegalArgumentException e) {
                                    uuid = Bukkit.getOfflinePlayer(m).getUniqueId();
                                    isToUpdate[0] = true;
                                }
                                members.add(uuid);
                            }
                        }

                        // Banned players data
                        String s_bans = resultSet.getString("bans");
                        Set<UUID> bans = new HashSet<>();
                        if (!s_bans.isBlank()) {
                            parts = s_bans.split(";");
                            for (String m : parts) {
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(m);
                                } catch (IllegalArgumentException e) {
                                    uuid = Bukkit.getOfflinePlayer(m).getUniqueId();
                                    isToUpdate[0] = true;
                                }
                                bans.add(uuid);
                            }
                        }
                        
                        if(isToUpdate[0]) {
                        	String members_new = members.stream()
                                    .map(UUID::toString)
                                    .collect(Collectors.joining(";"));
                        	preparedStatement2.setString(1, members_new);
                        	String bans_new = bans.stream()
                                    .map(UUID::toString)
                                    .collect(Collectors.joining(";"));
                        	preparedStatement2.setString(2, bans_new);
                        	preparedStatement2.setInt(3, id);
                        	preparedStatement2.addBatch();
                        	batchCount2++;
                        }
                        
                        // Check permissions for update
                        String perms = resultSet.getString("permissions");
                        parts = perms.split(";");
                        
                        // Set into map
                        Map<String,String> permList = new HashMap<>();
                        for(String s : parts) {
                        	String[] parts2 = s.split(":");
                        	permList.put(parts2[0], parts2[1]);
                        }
                        
                        StringBuilder final_perm = new StringBuilder();
                        
                        for (Map.Entry<String, String> entry : permList.entrySet()) {
                        	String key = entry.getKey();
                        	String perm = entry.getValue();
                        	
                        	int defaultLength = instance.getSettings().getDefaultValuesCode(key).length();
                        	
                        	StringBuilder permCompleted = new StringBuilder(perm);
                        	if (perm.length() != defaultLength) {
                                int diff = defaultLength - perm.length();
                                
                                if (diff < 0) {
                                    for (int i = 0; i < perm.length() - diff; i++) {
                                        if (perm.length() + i < defaultLength) {
                                            permCompleted.append(instance.getSettings().getDefaultValuesCode(key).charAt(perm.length() + i));
                                        } else {
                                            break;
                                        }
                                    }
                                } else {
                                    for (int i = 0; i < diff; i++) {
                                        if (perm.length() + i < defaultLength) {
                                            permCompleted.append(instance.getSettings().getDefaultValuesCode(key).charAt(perm.length() + i));
                                        } else {
                                            break;
                                        }
                                    }
                                }
                        	}
                        	
                        	if(final_perm.toString().isBlank()) {
                        		final_perm.append(key+":"+permCompleted.toString());
                        	} else {
                        		final_perm.append(";"+key+":"+permCompleted.toString());
                        	}
                        	
                        }

                        String permFinal = final_perm.toString();
                        preparedStatement.setString(1, permFinal);
                        preparedStatement.setInt(2, id);
                        preparedStatement.addBatch();
                        batchCount++;

                    }

                    if (batchCount2 > 0) {
                        preparedStatement2.executeBatch();
                        connection.commit(); // Commit transaction
                    }
                    
                    if (batchCount > 0) {
                        preparedStatement.executeBatch();
                        connection.commit(); // Commit transaction
                    }
                }
            } catch (SQLException e) {
                connection.rollback(); // Rollback transaction in case of error
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        int[] i = {0};
        int max_i = 0;
        int protected_areas_count = 0;
        Map<String,String> owners = new HashMap<>();
        try (Connection connection = instance.getDataSource().getConnection()) {
            String getQuery = "SELECT * FROM scs_claims_1";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        max_i++;

                        // General data
                        String uuid_string = resultSet.getString("owner_uuid");
                        UUID uuid_owner = (uuid_string.equals("none") || uuid_string.equals("aucun")) ? SERVER_UUID : UUID.fromString(uuid_string);
                        String permissions = resultSet.getString("permissions");
                        String name = resultSet.getString("claim_name");
                        String description = resultSet.getString("claim_description");
                        int id = resultSet.getInt("id_claim");
                        String owner = resultSet.getString("owner_name");
                        if (uuid_owner.equals(SERVER_UUID)) protected_areas_count++;

                        // World data
                        String world_name = resultSet.getString("world_name");
                        World check_world = Bukkit.getWorld(world_name);
                        World world = check_world == null ? Bukkit.createWorld(new WorldCreator(world_name)) : check_world;
                        if (world == null) continue;

                        // Location data
                        String[] parts = resultSet.getString("location").split(";");
                        double L_X = Double.parseDouble(parts[0]);
                        double L_Y = Double.parseDouble(parts[1]);
                        double L_Z = Double.parseDouble(parts[2]);
                        float L_Yaw = (float) Double.parseDouble(parts[3]);
                        float L_Pitch = (float) Double.parseDouble(parts[4]);
                        Location location = new Location(world, L_X, L_Y, L_Z, L_Yaw, L_Pitch);

                        // Members data
                        String s_members = resultSet.getString("members");
                        Set<UUID> members = new HashSet<>();
                        if (!s_members.isBlank()) {
                            parts = s_members.split(";");
                            for (String m : parts) {
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(m);
                                } catch (IllegalArgumentException e) {
                                	instance.info("Error when loading uuid:" + m);
                                    continue;
                                }
                                members.add(uuid);
                            }
                        }

                        // Banned players data
                        String s_bans = resultSet.getString("bans");
                        Set<UUID> bans = new HashSet<>();
                        if (!s_bans.isBlank()) {
                            parts = s_bans.split(";");
                            for (String m : parts) {
                                UUID uuid = null;
                                try {
                                    uuid = UUID.fromString(m);
                                } catch (IllegalArgumentException e) {
                                	instance.info("Error when loading uuid:" + m);
                                    continue;
                                }
                                bans.add(uuid);
                            }
                        }

                        // Permissions data
                        Map<String,LinkedHashMap<String, Boolean>> perms = new HashMap<>();
                        parts = permissions.split(";");
                        Map<String,String> permList = new HashMap<>();
                        for(String s : parts) {
                        	String[] parts2 = s.split(":");
                        	permList.put(parts2[0], parts2[1]);
                        }
                        
                        for (Map.Entry<String, String> entry : permList.entrySet()) {
                        	String key = entry.getKey();
                        	String perm = entry.getValue();
                            int count_i = 0;
                            LinkedHashMap<String, Boolean> perm_value = new LinkedHashMap<>();
                            for (String perm_key : instance.getSettings().getDefaultValues().get(key).keySet()) {
                                char currentChar = perm.charAt(count_i);
                                count_i++;
                                perm_value.put(perm_key, currentChar == '1');
                            }
                            perms.put(key, perm_value);
                        }

                        // Economy data
                        boolean sale = resultSet.getBoolean("for_sale");
                        double price = resultSet.getDouble("sale_price");

                        // Chunks data
                        String chunksData = resultSet.getString("chunks");
                        Set<Chunk> chunks = ConcurrentHashMap.newKeySet();
                        i[0]++;

                        Runnable task = () -> {
                            Claim claim = new Claim(uuid_owner, chunks, owner, members, location, name, description, perms, sale, price, bans, id);

                            // Add chunks
                            chunks.forEach(c -> listClaims.put(c, claim));

                            // Dynmap
                            if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createClaimZone(claim);

                            // Preload chunks
                            if (instance.getSettings().getBooleanSetting("preload-chunks")) {
                                if (instance.isFolia()) {
                                    chunks.forEach(c -> Bukkit.getRegionScheduler().execute(instance, world, c.getX(), c.getZ(), () -> c.load(true)));
                                } else {
                                    List<CompletableFuture<Void>> loadFutures = chunks.stream()
                                        .map(chunk -> CompletableFuture.runAsync(() -> {
                                            Bukkit.getScheduler().callSyncMethod(instance, (Callable<Void>) () -> {
                                                chunk.load(true);
                                                return null;
                                            });
                                        }))
                                        .collect(Collectors.toList());

                                    CompletableFuture<Void> allLoaded = CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]));
                                    allLoaded.join();
                                }
                            }

                            // Keep chunks loaded
                            if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
                                if (instance.isFolia()) {
                                    chunks.forEach(c -> Bukkit.getRegionScheduler().execute(instance, world, c.getX(), c.getZ(), () -> c.setForceLoaded(true)));
                                } else {
                                    List<CompletableFuture<Void>> keepLoadedFutures = chunks.stream()
                                        .map(chunk -> CompletableFuture.runAsync(() -> {
                                            Bukkit.getScheduler().callSyncMethod(instance, (Callable<Void>) () -> {
                                                chunk.setForceLoaded(true);
                                                return null;
                                            });
                                        }))
                                        .collect(Collectors.toList());

                                    CompletableFuture<Void> allKeptLoaded = CompletableFuture.allOf(keepLoadedFutures.toArray(new CompletableFuture[0]));
                                    allKeptLoaded.join();
                                }
                            }

                            // Add claim to owner
                            if (owner != null) {
                                if(!owner.equals("*")) owners.put(owner, uuid_owner.toString());
                                playerClaims.computeIfAbsent(uuid_owner, k -> ConcurrentHashMap.newKeySet()).add(claim);
                            }

                            // Enable bossbar
                            instance.getBossBars().activateBossBar(chunks);
                        };

                        List<CompletableFuture<Void>> futures = new ArrayList<>();
                        try {
                            byte[] data = Base64.getDecoder().decode(chunksData);
                            try (ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(data))) {
                                while (true) {
                                    try {
                                        int x = objectInputStream.readInt();
                                        int z = objectInputStream.readInt();
                                        CompletableFuture<Void> future;
                                        if (instance.isFolia()) {
                                            future = world.getChunkAtAsync(x, z).thenAccept(chunk -> {
                                                synchronized (chunks) {
                                                    chunks.add(chunk);
                                                }
                                            }).exceptionally(ex -> {
                                                ex.printStackTrace();
                                                return null;
                                            });
                                        } else {
                                            Chunk chunk = world.getChunkAt(x, z);
                                            chunks.add(chunk);
                                            future = CompletableFuture.completedFuture(null);
                                        }
                                        futures.add(future);
                                    } catch (EOFException e) {
                                        break;
                                    }
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                        allOf.thenRun(() -> {
                            task.run();
                        }).exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
        } catch (SQLException e1) {
			e1.printStackTrace();
		}

        instance.info(getNumberSeparate(String.valueOf(i[0]))+"/"+getNumberSeparate(String.valueOf(max_i))+" claims loaded.");
        instance.info("> including "+getNumberSeparate(String.valueOf(protected_areas_count))+" protected areas.");
        instance.getPlayerMain().loadOwners(owners);
        return;
    }

    /**
     * Creates a new claim for the player.
     *
     * @param player the player creating the claim
     * @param chunk  the chunk to claim
     */
    public CompletableFuture<Boolean> createClaim(Player player, Chunk chunk) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	    		// Get data
		        String playerName = player.getName();
		        UUID playerId = player.getUniqueId();
		        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerId);
		
		        // Update player claims count
		        cPlayer.setClaimsCount(cPlayer.getClaimsCount() + 1);
		
		        // Create default values, name, loc, perms and Claim
		        int id = findFreeId(playerId);
		        String uuid = playerId.toString();
		        String claimName = "claim-" + String.valueOf(id);
		        String description = instance.getLanguage().getMessage("default-description");
		        String locationString = getLocationString(player.getLocation());
		        Map<String,LinkedHashMap<String, Boolean>> perms = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
		        Claim newClaim = new Claim(playerId, new HashSet<>(Set.of(chunk)), playerName, new HashSet<>(Set.of(playerId)), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(),id);
		
		        // Add claim to claims list and player claims list
		        listClaims.put(chunk, newClaim);
		        playerClaims.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(newClaim);
		        
		        // Create bossbars and maps
		        if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createClaimZone(newClaim);
		        if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createClaimZone(newClaim);
		        if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createClaimZone(newClaim);
		        if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		Bukkit.getRegionScheduler().execute(instance, chunk.getWorld(), chunk.getX(), chunk.getZ(), () -> chunk.setForceLoaded(true));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance, (Callable<Void>) () -> {
	            			chunk.setForceLoaded(true);
	            			return null;
	            		});
	            	}
		        }
		        instance.executeSync(() -> instance.getBossBars().activateBossBar(chunk));
                updateWeatherChunk(newClaim);
                updateFlyChunk(newClaim);
		        
		        // Update database
		        return insertClaimIntoDatabase(id, uuid, playerName, claimName, description, chunk, locationString);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
    	});
    }

    /**
     * Handles the case where a claim conflict occurs when creating a claim.
     *
     * @param player the player attempting to create the claim
     * @param chunk  the chunk to claim
     */
    public void handleClaimConflict(Player player, Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        String owner = claim.getOwner();
        if (owner.equals("*")) {
            player.sendMessage(instance.getLanguage().getMessage("create-error-protected-area"));
        } else if (owner.equals(player.getName())) {
            player.sendMessage(instance.getLanguage().getMessage("create-already-yours"));
        } else {
            player.sendMessage(instance.getLanguage().getMessage("create-already-claim").replace("%player%", owner));
        }
    }

    /**
     * Gets the location string for the specified location.
     *
     * @param location the location to get the string for
     * @return the location string
     */
    private String getLocationString(Location location) {
        return String.format("%s;%s;%s;%s;%s", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * Creates a new protected area (admin claim).
     *
     * @param player the player creating the claim
     * @param chunk  the chunk to claim
     */
    public CompletableFuture<Boolean> createAdminClaim(Player player, Chunk chunk) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
		        // Create default values, name, loc, perms and Claim
		        String uuid = SERVER_UUID.toString();
		        int id = findFreeIdProtectedArea();
		        String claimName = "admin-" + String.valueOf(id);
		        String description = instance.getLanguage().getMessage("default-description");
		        String locationString = getLocationString(player.getLocation());
		        Map<String,LinkedHashMap<String, Boolean>> perms = new HashMap<>(instance.getSettings().getDefaultValues());
		        Claim newClaim = new Claim(SERVER_UUID, Set.of(chunk), "*", new HashSet<>(), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(),id);
		
		        // Add claim to claims list and protected areas list ("*" in playerClaims)
		        listClaims.put(chunk, newClaim);
		        playerClaims.computeIfAbsent(SERVER_UUID, k -> new HashSet<>()).add(newClaim);
		
		        // Create bossbars and maps
		        if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createClaimZone(newClaim);
		        if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createClaimZone(newClaim);
		        if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createClaimZone(newClaim);
		        if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		Bukkit.getRegionScheduler().execute(instance, chunk.getWorld(), chunk.getX(), chunk.getZ(), () -> chunk.setForceLoaded(true));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance, (Callable<Void>) () -> {
	            			chunk.setForceLoaded(true);
	            			return null;
	            		});
	            	}
		        }
		        instance.executeSync(() -> instance.getBossBars().activateBossBar(chunk));
                updateWeatherChunk(newClaim);
                updateFlyChunk(newClaim);
		
		        // Updata database
		        return insertClaimIntoDatabase(id, uuid, "*", claimName, description, chunk, locationString);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
    	});
    }
    
    /**
     * Inserts a new claim into the database.
     *
     * @param id             the ID of the claim
     * @param uuid           the UUID of the player creating the claim
     * @param owner          the owner creating the claim
     * @param claimName      the name of the claim
     * @param description    the description of the claim
     * @param chunk          the chunk being claimed
     * @param locationString the location string of the claim
     */
    private boolean insertClaimIntoDatabase(int id, String uuid, String owner, String claimName, String description, Chunk chunk, String locationString) {
        try (Connection connection = instance.getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions, bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, id);
        	stmt.setString(2, uuid);
        	stmt.setString(3, owner);
            stmt.setString(4, claimName);
            stmt.setString(5, description);
            stmt.setString(6, serializeChunks(Set.of(chunk)));
            stmt.setString(7, chunk.getWorld().getName());
            stmt.setString(8, locationString);
            stmt.setString(9, owner.equals("*") ? "" : uuid);
            stmt.setString(10, instance.getSettings().getDefaultValuesCode("all"));
            stmt.setString(11, "");
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Creates multiple claims within a radius.
     *
     * @param player the player creating the claims
     * @param chunks the chunks to claim
     * @param radius the radius within which to claim chunks
     * @return true if the claims were created successfully, false otherwise
     */
    public CompletableFuture<Boolean> createClaimRadius(Player player, Set<Chunk> chunks, int radius) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	            // Get data
	            String playerName = player.getName();
	            UUID playerId = player.getUniqueId();
	            Chunk chunk = player.getLocation().getChunk();
	            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerId);
	
	            // Get uuid of the player
	            String uuid = player.getUniqueId().toString();
	
	            // Create default values, name, loc, perms and Claim
	            int id = findFreeId(playerId);
	            String claimName = "claim-" + id;
	            String description = instance.getLanguage().getMessage("default-description");
	            String locationString = getLocationString(player.getLocation());
	            Map<String,LinkedHashMap<String, Boolean>> perms = new HashMap<>(instance.getSettings().getDefaultValues());
	            Claim newClaim = new Claim(playerId, chunks, playerName, Set.of(playerId), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(), id);
	
	            // Add the claim to claims list of the player
	            playerClaims.computeIfAbsent(player.getUniqueId(), k -> ConcurrentHashMap.newKeySet()).add(newClaim);
	
	            // Update his claims count
	            cPlayer.setClaimsCount(cPlayer.getClaimsCount() + 1);
	
	            // Create bossbars, maps
	            List<Integer> X = Collections.synchronizedList(new ArrayList<>());
	            List<Integer> Z = Collections.synchronizedList(new ArrayList<>());
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	            chunks.forEach(c -> {
	                listClaims.put(c, newClaim);
	                X.add(c.getX());
	                Z.add(c.getZ());
	            });
	            if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createClaimZone(newClaim);
	            if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createClaimZone(newClaim);
	            if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createClaimZone(newClaim);
	            if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		chunks.stream().forEach(c -> Bukkit.getRegionScheduler().execute(instance, c.getWorld(), c.getX(), c.getZ(), () -> c.setForceLoaded(true)));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance, (Callable<Void>) () -> {
	            			instance.executeSync(() -> chunks.stream().forEach(c -> c.setForceLoaded(true)));
	            			return null;
	            		});
	            	}
	            }
                updateWeatherChunk(newClaim);
                updateFlyChunk(newClaim);
	
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection();
	                 PreparedStatement stmt = connection.prepareStatement(
	                         "INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions, bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
	                stmt.setInt(1, id);
	                stmt.setString(2, uuid);
	                stmt.setString(3, playerName);
	                stmt.setString(4, claimName);
	                stmt.setString(5, description);
	                stmt.setString(6, serializeChunks(chunks));
	                stmt.setString(7, chunk.getWorld().getName());
	                stmt.setString(8, locationString);
	                stmt.setString(9, uuid);
	                stmt.setString(10, instance.getSettings().getDefaultValuesCode("all"));
	                stmt.setString(11, "");
	                stmt.executeUpdate();
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Creates multiple protected areas (admin claims) within a radius.
     *
     * @param player the player creating the claims
     * @param chunks the chunks to claim
     * @param radius the radius within which to claim chunks
     * @return true if the claims were created successfully, false otherwise
     */
    public CompletableFuture<Boolean> createAdminClaimRadius(Player player, Set<Chunk> chunks, int radius) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	    		// Get data
		        String playerName = "*";
		        Chunk chunk = player.getLocation().getChunk();
		        
		        // Create default values, name, loc, perms and Claim
		        int id = findFreeIdProtectedArea();
		        String claimName = "admin-" + String.valueOf(id);
		        String description = instance.getLanguage().getMessage("default-description");
		        String locationString = getLocationString(player.getLocation());
		        Map<String,LinkedHashMap<String, Boolean>> perms = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
		        Claim newClaim = new Claim(SERVER_UUID, chunks, playerName, new HashSet<>(), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(),id);
		
		        // Add the claim to protected areas list
		        playerClaims.computeIfAbsent(SERVER_UUID, k -> new HashSet<>()).add(newClaim);
		        
		        // Create bossbars, maps
		        List<Integer> X = Collections.synchronizedList(new ArrayList<>());
		        List<Integer> Z = Collections.synchronizedList(new ArrayList<>());
		        instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
		        chunks.stream().forEach(c -> {
		            listClaims.put(c, newClaim);
		            X.add(c.getX());
		            Z.add(c.getZ());
		        });
	            if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createClaimZone(newClaim);
	            if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createClaimZone(newClaim);
	            if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createClaimZone(newClaim);
	            if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		chunks.stream().forEach(c -> Bukkit.getRegionScheduler().execute(instance, c.getWorld(), c.getX(), c.getZ(), () -> c.setForceLoaded(true)));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance, (Callable<Void>) () -> {
	            			instance.executeSync(() -> chunks.stream().forEach(c -> c.setForceLoaded(true)));
	            			return null;
	            		});
	            	}
	            }
                updateWeatherChunk(newClaim);
                updateFlyChunk(newClaim);
		        
		        // Update database
	            try (Connection connection = instance.getDataSource().getConnection();
	                    PreparedStatement stmt = connection.prepareStatement(
	                    		"INSERT INTO scs_claims_1 (id_claim, owner_uuid, owner_name, claim_name, claim_description, chunks, world_name, location, members, permissions, bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
	                   stmt.setInt(1, id);
	            	   stmt.setString(2, SERVER_UUID.toString());
	            	   stmt.setString(3, "*");
	                   stmt.setString(4, claimName);
	                   stmt.setString(5, description);
	                   stmt.setString(6, serializeChunks(chunks));
	                   stmt.setString(7, chunk.getWorld().getName());
	                   stmt.setString(8, locationString);
	                   stmt.setString(9, "");
	                   stmt.setString(10, instance.getSettings().getDefaultValuesCode("all"));
	                   stmt.setString(11, "");
	                   stmt.executeUpdate();
	                   return true;
	               } catch (SQLException e) {
	                   e.printStackTrace();
	                   return false;
	               }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Calculates the price for creating multiple claims.
     *
     * @param cPlayer  the player creating the claims
     * @param numClaims the number of claims being created
     * @return the total price for creating the claims
     */
    public double calculateClaimPrice(CPlayer cPlayer, int numClaims) {
        if (!instance.getSettings().getBooleanSetting("economy") || !instance.getSettings().getBooleanSetting("claim-cost")) {
            return 0;
        }
        return instance.getSettings().getBooleanSetting("claim-cost-multiplier") ? cPlayer.getRadiusMultipliedCost(numClaims) : cPlayer.getCost() * numClaims;
    }

    /**
     * Checks if a claim exists in the given chunk.
     *
     * @param chunk the chunk to check
     * @return true if a claim exists in the chunk, false otherwise
     */
    public boolean checkIfClaimExists(Chunk chunk) {
        return listClaims.containsKey(chunk);
    }
    
    /**
     * Checks if a claim exists.
     *
     * @param claim the claim to check
     * @return true if a claim exists, false otherwise
     */
    public boolean checkIfClaimExists(Claim claim) {
        return listClaims.containsValue(claim);
    }

    /**
     * Checks if a permission is allowed for the given chunk.
     *
     * @param chunk the chunk to check
     * @param perm  the permission to check
     * @return true if the permission is allowed, false otherwise
     */
    public boolean canPermCheck(Chunk chunk, String perm, String role) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getPermission(perm, role == null ? "natural" : role.toLowerCase());
    }
    
    /**
     * Gets the owner of a claim by the chunk.
     *
     * @param chunk the chunk to get the owner from
     * @return the owner of the claim, or an empty string if no claim exists for the chunk
     */
    public String getOwnerInClaim(Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        return claim == null ? "" : claim.getOwner();
    }

    /**
     * Checks if a player is a member of a claim.
     *
     * @param claim  the claim to check
     * @param player the player to check
     * @return true if the player is a member of the claim, false otherwise
     */
    public boolean checkMembre(Claim claim, Player player) {
    	return claim == null ? false : claim.getMembers().contains(player.getUniqueId());
    }

    /**
     * Checks if a player name is a member of a claim.
     *
     * @param claim  the claim to check
     * @param targetName the name of the player to check
     * @return true if the player name is a member of the claim, false otherwise
     */
    public boolean checkMembre(Claim claim, String targetName) {
        return claim != null && claim.getMembers().stream()
                .map(ban -> instance.getPlayerMain().getPlayerName(ban))
                .anyMatch(playerName -> playerName != null && playerName.equalsIgnoreCase(targetName));
    }
    
    /**
     * Checks if a player is banned from a claim.
     *
     * @param claim  the claim to check
     * @param player the player to check
     * @return true if the player is banned from the claim, false otherwise
     */
    public boolean checkBan(Claim claim, Player player) {
    	return claim == null ? false : claim.getBans().contains(player.getUniqueId());
    }

    /**
     * Checks if a player name is banned from a claim.
     *
     * @param claim  the claim to check
     * @param targetName the name of the player to check
     * @return true if the player name is banned from the claim, false otherwise
     */
    public boolean checkBan(Claim claim, String targetName) {
        return claim != null && claim.getBans().stream()
            .map(ban -> instance.getPlayerMain().getPlayerName(ban))
            .anyMatch(playerName -> playerName != null && playerName.equalsIgnoreCase(targetName));
    }

    /**
     * Gets the real name of a player from claim members.
     *
     * @param claim      the claim to check
     * @param targetName the name of the player to check
     * @return the real name of the player, or the target name if not found
     */
    public String getRealNameFromClaimMembers(Claim claim, String targetName) {
        if (claim != null) {
            return claim.getMembers().stream()
                    .map(member -> instance.getPlayerMain().getPlayerName(member))
                    .filter(playerName -> playerName.equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(targetName);
        }
        return targetName;
    }

    /**
     * Gets the real name of a player from claim bans.
     *
     * @param claim      the claim to check
     * @param targetName the name of the player to check
     * @return the real name of the player, or the target name if not found
     */
    public String getRealNameFromClaimBans(Claim claim, String targetName) {
        if (claim != null) {
            return claim.getBans().stream()
                    .map(ban -> instance.getPlayerMain().getPlayerName(ban))
                    .filter(playerName -> playerName.equalsIgnoreCase(targetName))
                    .findFirst()
                    .orElse(targetName);
        }
        return targetName;
    }

    /**
     * Converts a set of UUIDs to a single string where each UUID is separated by a semicolon.
     *
     * @param claim The claim containing the bans.
     * @return A string representation of the bans, or an empty string if there are no bans.
     */
    public String getBanString(Claim claim) {
        if (claim != null) {
            return claim.getBans().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(";"));
        }
        return "";
    }
    
    /**
     * Converts a set of UUIDs to a single string where each UUID is separated by a semicolon.
     *
     * @param claim The claim containing the members.
     * @return A string representation of the members, or an empty string if there are no members.
     */
    public String getMemberString(Claim claim) {
        if (claim != null) {
            return claim.getMembers().stream()
                    .map(UUID::toString)
                    .collect(Collectors.joining(";"));
        }
        return "";
    }

    /**
     * Updates a claim's permission.
     *
     * @param claim        the claim to update the permission for
     * @param permission   the permission to update
     * @param value 	   the new value of the permission
     * @param role         the role for which the permission is updated
     * @return true if the permission was updated successfully, false otherwise
     */
    public CompletableFuture<Boolean> updatePerm(Claim claim, String permission, boolean value, String role) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Get the owner's name
                String owner = claim.getOwner();
                
                // Get the current permissions map for the specified role
                String roleKey = (role == null ? "natural" : role.toLowerCase());
                LinkedHashMap<String, Boolean> currentPermissions = claim.getPermissions().get(roleKey);
                
                // Clone the current permissions map to avoid affecting other claims
                LinkedHashMap<String, Boolean> newPermissions = new LinkedHashMap<>(currentPermissions);
                newPermissions.put(permission, value);
                
                // Update the permissions map in the claim with the cloned and updated map
                claim.getPermissions().put(roleKey, newPermissions);

                // Check if permission is Weather, then update weather for players in the chunks
                if (permission.equals("Weather")) updateWeatherChunk(claim);
                // Check if permission is Fly, then update fly for players in the chunks
                if (permission.equals("Fly")) updateFlyChunk(claim);
                
                // Get the UUID of the owner
                String uuid = owner.equals("*") ? SERVER_UUID.toString() : instance.getPlayerMain().getPlayerUUID(owner).toString();
        
                // Build the perms string
                String permissions = claim.getPermissions().entrySet().stream()
                        .map(entry -> entry.getKey() + ":" + entry.getValue().entrySet().stream()
                                .map(subEntry -> subEntry.getValue() ? "1" : "0")
                                .collect(Collectors.joining()))
                        .collect(Collectors.joining(";"));
                
                // Update the database
                String updateQuery = "UPDATE scs_claims_1 SET permissions = ? WHERE owner_uuid = ? AND claim_name = ?";
                try (Connection connection = instance.getDataSource().getConnection();
                     PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, permissions);
                    preparedStatement.setString(2, uuid);
                    preparedStatement.setString(3, claim.getName());
                    preparedStatement.executeUpdate();
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to apply current settings to all owner's claims.
     *
     * @param owner the owner whose claims will be updated
     * @param claim the claim from which to apply settings to all player's claims
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> applyAllSettings(Claim claim) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
            	// Get data
            	UUID uuid = claim.getUUID();
            	
	        	// Update perms
	            Map<String,LinkedHashMap<String, Boolean>> perms = new LinkedHashMap<>(claim.getPermissions());
	            
	            // Update settings
	            playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(c -> {
	            	c.setPermissions(perms);
	                updateWeatherChunk(c);
	                updateFlyChunk(c);
	            });
	        	
	        	// Build the perms string
		        String permissions = perms.entrySet().stream()
		                .map(entry -> entry.getKey() + ":" + entry.getValue().entrySet().stream()
		                        .map(subEntry -> subEntry.getValue() ? "1" : "0")
		                        .collect(Collectors.joining()))
		                .collect(Collectors.joining(";"));
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET permissions = ? WHERE owner_uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, permissions);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to ban a player from a claim.
     *
     * @param claim the claim representing the claim
     * @param name the name of the player to be banned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addClaimBan(Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String claimName = claim.getName();
	        	UUID uuid = claim.getUUID();
	        	
	        	// Add banned and remove member
	        	UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
	        	claim.addBan(targetUUID);
	        	claim.removeMember(targetUUID);
		        
		        // Update database
		        String banString = getBanString(claim);
		        String memberString = getMemberString(claim);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET bans = ?, members = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, banString);
	                    preparedStatement.setString(2, memberString);
	                    preparedStatement.setString(3, uuid.toString());
	                    preparedStatement.setString(4, claimName);
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to unban a player from a claim
     *
     * @param claim the claim representing the claim
     * @param name the name of the player to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeClaimBan(Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String claimName = claim.getName();
	        	UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
	        	UUID uuid = claim.getUUID();
	        	
	        	// Remove banned
	            claim.removeBan(targetUUID);
	            
	            // Update database
	            String banString = getBanString(claim);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET bans = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, banString);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claimName);
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to ban a member from all player's claims.
     *
     * @param owner the owner of the claims
     * @param name the name of the member to be banned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAllClaimBan(String owner, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
            	
	            // Get uuid of the owner and target
            	UUID uuid = owner.equals("*") ? SERVER_UUID : instance.getPlayerMain().getPlayerUUID(owner);
            	String uuid_string = uuid.toString();
	            UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
	            
	            // Add banned and remove member
	            playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(claim -> {
	            	claim.addBan(targetUUID);
	            	claim.removeMember(targetUUID);
	            });
	
		        // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET bans = ?, members = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    for (Claim claim : playerClaims.computeIfAbsent(uuid, k -> new HashSet<>())) {
	                        preparedStatement.setString(1, getBanString(claim));
	                        preparedStatement.setString(2, getMemberString(claim));
	                        preparedStatement.setString(3, uuid_string);
	                        preparedStatement.setString(4, claim.getName());
	                        preparedStatement.addBatch();
	                    }
	                    preparedStatement.executeBatch();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to unban a player from all owner's claims.
     *
     * @param owner the owner of the claims
     * @param name the name of the player to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAllClaimBan(String owner, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
            	
	            // Get uuid of the owner and target
            	UUID uuid = owner.equals("*") ? SERVER_UUID : instance.getPlayerMain().getPlayerUUID(owner);
            	String uuid_string = uuid.toString();
	            UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);

		        playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(claim -> claim.removeBan(targetUUID));
	            
	            // Updata database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET bans = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	for (Claim claim : playerClaims.computeIfAbsent(uuid, k -> new HashSet<>())) {
	                        preparedStatement.setString(1, getBanString(claim));
	                        preparedStatement.setString(2, uuid_string);
	                        preparedStatement.setString(3, claim.getName());
	                        preparedStatement.addBatch();
	                    }
	                    preparedStatement.executeBatch();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to add a member to a claim.
     *
     * @param claim the claim
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addClaimMember(Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
            	
            	// Get data
            	UUID uuid = claim.getUUID();
            	UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
            	
	        	// Add member
	            claim.addMember(targetUUID);
	            
	            // Update database
	            String membersString = getMemberString(claim);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET members = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, membersString);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
	        } catch (Exception e) {
	            e.printStackTrace();
	            return false;
	        }
        });
    }
    
    /**
     * Method to add a member to all owner's claims.
     *
     * @param owner the owner of the claims
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAllClaimsMember(String owner, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            
	            // Get uuid of the owner and target
            	UUID uuid = owner.equals("*") ? SERVER_UUID : instance.getPlayerMain().getPlayerUUID(owner);
            	String uuid_string = uuid.toString();
	            UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
	            
	            // Remove member
		        playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(claim -> claim.addMember(targetUUID));
	
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET members = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	for (Claim claim : playerClaims.computeIfAbsent(uuid, k -> new HashSet<>())) {
	                        preparedStatement.setString(1, getMemberString(claim));
	                        preparedStatement.setString(2, uuid_string);
	                        preparedStatement.setString(3, claim.getName());
	                        preparedStatement.addBatch();
	                    }
	                    preparedStatement.executeBatch();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to remove a member from a claim.
     *
     * @param claim the claim
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeClaimMember(Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	// Get data
            	UUID uuid = claim.getUUID();
            	UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
            	
	        	// Add member
	            claim.removeMember(targetUUID);
	            
	            // Update database
	            String membersString = getMemberString(claim);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET Members = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, membersString);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to remove a member from all owner's claims.
     *
     * @param owner the owner of the claims
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAllClaimsMember(String owner, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {

	            // Get uuid of the owner and target
            	UUID uuid = owner.equals("*") ? SERVER_UUID : instance.getPlayerMain().getPlayerUUID(owner);
            	String uuid_string = uuid.toString();
	            UUID targetUUID = instance.getPlayerMain().getPlayerUUID(name);
	            playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(claim -> claim.removeMember(targetUUID));
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET Members = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	for (Claim claim : playerClaims.computeIfAbsent(uuid, k -> new HashSet<>())) {
	                        preparedStatement.setString(1, getMemberString(claim));
	                        preparedStatement.setString(2, uuid_string);
	                        preparedStatement.setString(3, claim.getName());
	                        preparedStatement.addBatch();
	                    }
	                    preparedStatement.executeBatch();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to change a claim's name.
     *
     * @param claim the claim
     * @param name the new name for the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setClaimName(Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	
	        	// Get data and update name
            	UUID uuid = claim.getUUID();
	            String old_name = claim.getName();
	            claim.setName(name);
	            
	            // Update name on bossbars and maps
	        	Set<Chunk> chunks = claim.getChunks();
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(claim);
	        	
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET claim_name = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, name);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, old_name);
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to change the claim's spawn location.
     *
     * @param claim the claim representing the claim
     * @param loc the new location for the claim's spawn
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setClaimLocation(Claim claim, Location loc) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	
	        	// Get data and update loc
            	UUID uuid = claim.getUUID();
	        	claim.setLocation(loc);
	        	
	        	// Update database
	            String loc_string = String.valueOf(loc.getX()) + ";" + String.valueOf(loc.getY()) + ";" + String.valueOf(loc.getZ()) + ";" + String.valueOf(loc.getYaw()) + ";" + String.valueOf(loc.getPitch());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET Location = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, loc_string);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to delete a claim.
     *
     * @param claim the claim representing the claim to be deleted
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> deleteClaim(Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	
	        	// Get data
            	UUID uuid = claim.getUUID();
	            
	        	// Delete all chunks and deactivate bossbars
	        	Set<Chunk> chunks = claim.getChunks();
	        	instance.executeSync(() -> instance.getBossBars().deactivateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
	        	chunks.stream().forEach(c -> listClaims.remove(c));
                resetWeatherChunk(claim);
                resetFlyChunk(claim);
	            
	            // Update player's claims count if its not a protected area
                if(!claim.getOwner().equals("*")) {
	            	Player player = Bukkit.getPlayer(uuid);
		            if (player != null && player.isOnline()) {
	    	            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(uuid);
	    	            cPlayer.setClaimsCount(cPlayer.getClaimsCount() - 1);
		            }
                }
            
	        	// Remove claim from owner's claims list
	            playerClaims.get(uuid).remove(claim);
	            if (playerClaims.get(uuid).isEmpty()) playerClaims.remove(uuid);
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims_1 WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid.toString());
	                    preparedStatement.setString(2, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to delete all owner's claims.
     *
     * @param owner the owner whose claims will be deleted
     * @return CompletableFuture<Boolean> indicating success or failure
     */
    public CompletableFuture<Boolean> deleteAllClaims(String owner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                
	            // Get uuid of the owner and update owner claims count
            	UUID uuid = owner.equals("*") ? SERVER_UUID : instance.getPlayerMain().getPlayerUUID(owner);
	            if(!owner.equals("*")) {
	            	Player player = Bukkit.getPlayer(owner);
		            if (player != null && player.isOnline()) {
			            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(uuid);
			            cPlayer.setClaimsCount(0);
		            }
	            }

                // Delete all claims of target player, and remove him from data
                playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(claim -> {
                    Set<Chunk> chunks = claim.getChunks();
                    instance.executeSync(() -> instance.getBossBars().deactivateBossBar(chunks));
                    if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
                    if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
                    if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
                    chunks.stream().forEach(c -> listClaims.remove(c));
                    updateWeatherChunk(claim);
                    updateFlyChunk(claim);
                });
                playerClaims.remove(uuid);

                // Update database
                try (Connection connection = instance.getDataSource().getConnection()) {
                    String deleteQuery = "DELETE FROM scs_claims_1 WHERE owner_uuid = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                        preparedStatement.setString(1, uuid.toString());
                        preparedStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                    return false;
                }

                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to change claim's description.
     *
     * @param claim the claim
     * @param description the new description for the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setClaimDescription(Claim claim, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	// Get data
            	UUID uuid = claim.getUUID();
            	
            	// Update description
	        	claim.setDescription(description);
	        	
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET claim_description = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, description);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to put a claim on sale.
     *
     * @param claim the claim
     * @param price the sale price of the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setChunkSale(Claim claim, double price) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	// Get data
            	UUID uuid = claim.getUUID();
            	
            	// Update sale and price
	            claim.setSale(true);
	            claim.setPrice(price);
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET for_sale = true, sale_price = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, String.valueOf(price));
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to remove a claim from sales.
     *
     * @param claim the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> delChunkSale(Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	// Get data
            	UUID uuid = claim.getUUID();
            	
            	// Update sale and price
	            claim.setSale(false);
	            claim.setPrice(0.0);
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET for_sale = false, sale_price = 0 WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, uuid.toString());
	                    preparedStatement.setString(2, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to reset settings of player's claims
     * 
     * @param owner The target owner
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> resetAllOwnerClaimsSettings(String owner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	// Get data
	        	String defaultValue = instance.getSettings().getDefaultValuesCode("all");
	        	Map<String,LinkedHashMap<String,Boolean>> perm = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
	            
	            // Get uuid of the owner
	        	UUID uuid = owner.equals("*") ? SERVER_UUID : instance.getPlayerMain().getPlayerUUID(owner);

	        	// Update perms
	            playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).stream().forEach(c -> {
	            	c.setPermissions(perm);
	                updateWeatherChunk(c);
	                updateFlyChunk(c);
	            });
	            
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET permissions = ? WHERE owner_uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, defaultValue);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to reset settings of claim
     * 
     * @param claim The target claim
     * @param owner The target owner
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> resetClaimSettings(Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
            	
            	// Get data
            	UUID uuid = claim.getUUID();
	        	String defaultValue = instance.getSettings().getDefaultValuesCode("all");
	        	Map<String,LinkedHashMap<String,Boolean>> perm = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
	        	String owner = claim.getOwner();
	        	
	        	// Update perms
	            claim.setPermissions(perm);
	            
	            // Update weather and fly
                updateWeatherChunk(claim);
                updateFlyChunk(claim);
	            
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET Permissions = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, defaultValue);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to reset all player claims settings
     * 
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> resetAllPlayerClaimsSettings() {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	String defaultValue = instance.getSettings().getDefaultValuesCode("all");
	        	Map<String,LinkedHashMap<String,Boolean>> perm = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
	            listClaims.values().stream().forEach(c -> {
	            	if(!c.getUUID().equals(SERVER_UUID)) {
	                    c.setPermissions(perm);
	    	            // Update weather and fly
	                    updateWeatherChunk(c);
	                    updateFlyChunk(c);
	            	}
	            });
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET permissions = ? WHERE owner_uuid <> ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, defaultValue);
	                    preparedStatement.setString(2, SERVER_UUID.toString());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method when a claim is sold.
     *
     * @param player player buying the claim
     * @param chunk the chunk representing the claim
     */
    public CompletableFuture<Boolean> sellChunk(Player player, Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
            	String playerName = player.getName();
            	UUID playerId = player.getUniqueId();
	        	String old_name = claim.getName();
	            String owner = claim.getOwner();
	            double price = claim.getPrice();
	            
	            // Money transfer
	            if(!owner.equalsIgnoreCase("*")) {
	            	instance.getVault().addPlayerBalance(owner, price);
	            }
	            instance.getVault().removePlayerBalance(playerName, price);
	
	            // Set uuid of the old owner, and update his claims count
	            UUID uuid = claim.getUUID();
	            if(!owner.equals("*")) {
		            Player ownerP = Bukkit.getPlayer(owner);
		            if (ownerP != null && ownerP.isOnline()) {
		                CPlayer cOwner = instance.getPlayerMain().getCPlayer(uuid);
		                cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
		            }
	            }
	            
	            // Delete old owner claim
	            playerClaims.get(uuid).remove(claim);
	            if (playerClaims.get(uuid).isEmpty()) playerClaims.remove(uuid);
	            
	            // Set uuid of the new owner and update his claims count
		        CPlayer cTarget = instance.getPlayerMain().getCPlayer(playerId);
		        cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
	            
	            // Set the new owner to him
	            claim.setOwner(playerName);
	            
	            // Set the new name of the bought claim
	            int id = findFreeId(playerId);
	            String new_name = "claim-" + String.valueOf(id);
	            claim.setName(new_name);
	            
	            // Add the new owner to members if not member, and remove the old owner
	            Set<UUID> members = new HashSet<>(claim.getMembers());
	            if (!members.contains(playerId)) {
	                members.add(playerId);
	            }
	            members.remove(uuid);
	            claim.setMembers(members);
	            String members_string = getMemberString(claim);
	            
	            // Delete the sale and set the price to 0.0
	            claim.setSale(false);
	            claim.setPrice(0.0);
	            
	            // Add the claim to the new owner
	            playerClaims.computeIfAbsent(playerId, k -> new HashSet<>()).add(claim);
	            
	            // Update the bossbars, and maps
	        	Set<Chunk> chunks = claim.getChunks();
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(claim);
	            
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET id_claim = ?, owner_uuid = ?, owner_name = ?, members = ?, claim_name = ?, for_sale = false, sale_price = 0 WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	preparedStatement.setInt(1, id);
	                    preparedStatement.setString(2, playerId.toString());
	                    preparedStatement.setString(3, playerName);
	                    preparedStatement.setString(4, members_string);
	                    preparedStatement.setString(5, new_name);
	                    preparedStatement.setString(6, uuid.toString());
	                    preparedStatement.setString(7, old_name);
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Method to change the owner of a claim.
     *
     * @param sender the player sending the request
     * @param playerName the name of the new owner
     * @param claim the claim
     * @param msg whether to send a message to the sender
     */
    public CompletableFuture<Boolean> setOwner(String playerName, Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String old_name = claim.getName();
	            String owner = claim.getOwner();
	            
	            // Set uuid of the old owner, and update his claims count if online
	            UUID uuid = claim.getUUID();
	            if(!owner.equals("*")) {
		            Player ownerP = Bukkit.getPlayer(owner);
		            if (ownerP != null && ownerP.isOnline()) {
		                CPlayer cOwner = instance.getPlayerMain().getCPlayer(uuid);
		                cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
		            }
	            }
	            
	            // Delete old owner claim
	            playerClaims.get(uuid).remove(claim);
	            if (playerClaims.get(uuid).isEmpty()) playerClaims.remove(uuid);
	            
	            // Update the claims count of new owner if online, and set the new owner to him
	            UUID uuidNewOwner = instance.getPlayerMain().getPlayerUUID(playerName);
	            String uuid_new_owner = uuidNewOwner.toString();
	            Player player = Bukkit.getPlayer(playerName);
	            if (player != null && player.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(uuidNewOwner);
	                cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
	            }
	            
	            // Set the new owner to him
	            claim.setOwner(playerName);
	            
	            // Set the new name of the bought claim
	            int id = findFreeId(uuid);
	            String new_name = "claim-" + String.valueOf(id);
	            claim.setName(new_name);
	            
	            // Add the new owner to members if not member, and remove the old owner
	            Set<UUID> members = new HashSet<>(claim.getMembers());
	            if (!members.contains(uuidNewOwner)) {
	                members.add(uuidNewOwner);
	            }
	            members.remove(uuid);
	            claim.setMembers(members);
	            String members_string = getMemberString(claim);
	            
	            // Add the claim to the new owner
	            playerClaims.getOrDefault(playerName, new HashSet<>()).add(claim);
	            
	            // Update the bossbars, and maps
	        	Set<Chunk> chunks = claim.getChunks();
	        	instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(claim);
	            
	            // Updata database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET id_claim = ?, owner_uuid = ?, members = ?, claim_name = ?, for_sale = false, sale_price = 0 WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	preparedStatement.setInt(1, id);
	                    preparedStatement.setString(2, uuid_new_owner);
	                    preparedStatement.setString(3, members_string);
	                    preparedStatement.setString(4, new_name);
	                    preparedStatement.setString(5, uuid.toString());
	                    preparedStatement.setString(6, old_name);
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Method to change the owner of a claim.
     *
     * @param sender the player sending the request
     * @param playerName the name of the new owner
     * @param claims the claims
     * @param owner The owner of the claims
     */
    public CompletableFuture<Boolean> setOwner(String playerName, Set<Claim> claims, String owner) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            
	            // Set uuid of the old owner, and update his claims count if online
	            String uuid = "";
	            UUID uuid_real = instance.getPlayerMain().getPlayerUUID(owner);
	            if(!owner.equals("*")) {
		            Player ownerP = Bukkit.getPlayer(owner);
		            if (ownerP != null && ownerP.isOnline()) {
		                CPlayer cOwner = instance.getPlayerMain().getCPlayer(uuid_real);
		                cOwner.setClaimsCount(cOwner.getClaimsCount() - claims.size());
		            }
	            }
	            
	            // Delete old owner claim
	            playerClaims.get(uuid_real).removeAll(claims);
	            if (playerClaims.get(uuid_real).isEmpty()) playerClaims.remove(uuid_real);
	            
	            // Update the claims count of new owner if online, and set the new owner to him
	            UUID uuidNewOwner = instance.getPlayerMain().getPlayerUUID(playerName);
	            String uuid_new_owner = uuidNewOwner.toString();
	            Player player = Bukkit.getPlayer(playerName);
	            if (player != null && player.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(uuidNewOwner);
	                cTarget.setClaimsCount(cTarget.getClaimsCount() + claims.size());
	            }
	            
	            // Updata database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET id_claim = ?, owner_uuid = ?, members = ?, claim_name = ?, for_sale = false, sale_price = 0 WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	
	                	for(Claim claim : claims) {
	                		
	                		int id = findFreeId(uuid_real);
	                		String old_name = claim.getName();
	                		
	                		// Set the new owner to him
	        	            claim.setOwner(playerName);
	        	            
	        	            // Set the new name of the bought claim
	        	            String new_name = "claim-" + String.valueOf(id);
	        	            claim.setName(new_name);
	        	            
	        	            // Add the new owner to members if not member, and remove the old owner
	        	            Set<UUID> members = new HashSet<>(claim.getMembers());
	        	            if (!members.contains(uuidNewOwner)) {
	        	                members.add(uuidNewOwner);
	        	            }
	        	            members.remove(UUID.fromString(uuid));
	        	            claim.setMembers(members);
	        	            String members_string = getMemberString(claim);
	        	            
	        	            // Add the claim to the new owner
	        	            playerClaims.computeIfAbsent(uuidNewOwner, k -> new HashSet<>()).add(claim);
	        	            
	        	            // Update the bossbars, and maps
	        	        	Set<Chunk> chunks = claim.getChunks();
	        	        	instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(claim);
	        	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(claim);
	        	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(claim);
	        	        	
		                	preparedStatement.setInt(1, id);
		                    preparedStatement.setString(2, uuid_new_owner);
		                    preparedStatement.setString(3, members_string);
		                    preparedStatement.setString(4, new_name);
		                    preparedStatement.setString(5, uuid);
		                    preparedStatement.setString(6, old_name);
		                    preparedStatement.addBatch();
	                	}
	                	
	                	int[] n = preparedStatement.executeBatch();
		                return n[0]>0;
		            } catch (SQLException e) {
		                e.printStackTrace();
		                return false;
		            }
	            } catch (Exception e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Remove a chunk from a claim
     * 
     * @param claim The target claim
     * @param chunk_default The target chunk in string format (world;x;z)
     * @return true if the merge process was initiated successfully
     */
    public CompletableFuture<Boolean> removeClaimChunk(Claim claim, String chunk_default){
        return CompletableFuture.supplyAsync(() -> {
            try {
            	String[] parts = chunk_default.split(";");
            	World world = Bukkit.getWorld(parts[0]);
            	if(world == null) return false;
            	int X_;
            	int Z_;
            	try {
            		X_ = Integer.parseInt(parts[1]);
            	} catch (NumberFormatException e) {
            		return false;
            	}
            	try {
            		Z_ = Integer.parseInt(parts[2]);
            	} catch (NumberFormatException e) {
            		return false;
            	}
            	if(instance.isFolia()) {
            		CompletableFuture<Boolean> future = world.getChunkAtAsync(X_, Z_).thenApply(chunk -> {
            			
            			// Remove chunk
            			Set<Chunk> chunks = new HashSet<>(claim.getChunks());
            			if(!chunks.contains(chunk)) return false;
                    	chunks.remove(chunk);
                    	claim.setChunks(chunks);
                    	listClaims.remove(chunk);
                    	
                    	// Remove bossbar and maps
                        if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(Set.of(chunk));
                        if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(Set.of(chunk));
                        if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(Set.of(chunk));
                    	instance.executeSync(() -> instance.getBossBars().deactivateBossBar(Set.of(chunk)));
                        updateWeatherChunk(claim);
                        updateFlyChunk(claim);
        	            
        	            // Serialize chunks
        	            String chunksData = serializeChunks(chunks);
        	            
        	            // Get uuid of the owner
        	            UUID uuid = claim.getUUID();
        	            
        	            // Update database
        	            try (Connection connection = instance.getDataSource().getConnection()) {
        	                String updateQuery = "UPDATE scs_claims_1 SET chunks = ? WHERE owner_uuid = ? AND claim_name = ?";
        	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
        	                    preparedStatement.setString(1, chunksData);
        	                    preparedStatement.setString(2, uuid.toString());
        	                    preparedStatement.setString(3, claim.getName());
        	                    preparedStatement.executeUpdate();
        	                }
        	                return true;
        	            } catch (SQLException e) {
        	                e.printStackTrace();
        	                return false;
        	            }
            		});
            		return future.join();
            	} else {
            		Chunk chunk = world.getChunkAt(X_, Z_);
            		// Remove chunk
            		Set<Chunk> chunks = new HashSet<>(claim.getChunks());
                	chunks.remove(chunk);
                	claim.setChunks(chunks);
                	listClaims.remove(chunk);
                	
                	// Remove bossbar and maps
                    if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(Set.of(chunk));
                    if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(Set.of(chunk));
                    if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(Set.of(chunk));
                	instance.executeSync(() -> instance.getBossBars().deactivateBossBar(Set.of(chunk)));
                    updateWeatherChunk(claim);
                    updateFlyChunk(claim);
    	            
    	            // Serialize chunks
    	            String chunksData = serializeChunks(chunks);
    	            
    	            // Get uuid of the owner
    	            UUID uuid = claim.getUUID();
    	            
    	            // Update database
    	            try (Connection connection = instance.getDataSource().getConnection()) {
    	                String updateQuery = "UPDATE scs_claims_1 SET chunks = ? WHERE owner_uuid = ? AND claim_name = ?";
    	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	                    preparedStatement.setString(1, chunksData);
    	                    preparedStatement.setString(2, uuid.toString());
    	                    preparedStatement.setString(3, claim.getName());
    	                    preparedStatement.executeUpdate();
    	                }
    	                return true;
    	            } catch (SQLException e) {
    	                e.printStackTrace();
    	                return false;
    	            }
            	}
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Add a chunk to a claim
     * 
     * @param claim The target claim
     * @param chunk The target chunk
     * @return true if the merge process was initiated successfully
     */
    public CompletableFuture<Boolean> addClaimChunk(Claim claim, Chunk chunk){
        return CompletableFuture.supplyAsync(() -> {
            try {
    			// Add chunk
            	Set<Chunk> chunks = new HashSet<>(claim.getChunks());
            	if(chunks.contains(chunk)) return false;
            	chunks.add(chunk);
            	claim.setChunks(chunks);
            	listClaims.put(chunk,claim);
            	
            	// Add bossbar and maps
                if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createClaimZone(claim);
                if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createClaimZone(claim);
                if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createClaimZone(claim);
            	instance.executeSync(() -> instance.getBossBars().activateBossBar(Set.of(chunk)));
                updateWeatherChunk(claim);
                updateFlyChunk(claim);
            	
                // Serialize chunks
	            String chunksData = serializeChunks(chunks);
	            
	            // Get uuid of the owner
	            UUID uuid = claim.getUUID();
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET chunks = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, chunksData);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.setString(3, claim.getName());
	                    preparedStatement.executeUpdate();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }
    
    /**
     * Merges claims into one.
     *
     * @param claim1 the primary claim to merge into
     * @param claims the set of claims to merge with claim1
     * @return true if the merge process was initiated successfully
     */
    public CompletableFuture<Boolean> mergeClaims(Claim claim1, Set<Claim> claims) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            
	            // Collect chunks from claims and update listClaims map and add new chunks
	            claims.stream().forEach(claim -> {
	            	claim1.addChunks(claim.getChunks());
	            	instance.executeSync(() -> instance.getBossBars().activateBossBar(claim.getChunks()));
	            	Set<Chunk> chunks = claim.getChunks();
	            	chunks.stream().forEach(c -> listClaims.put(c, claim1));
	                if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(claim1);
	                if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(claim1);
	                if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(claim1);
	                updateWeatherChunk(claim1);
	                updateFlyChunk(claim1);
	            });
	            
	            // Get uuid of the owner
	            UUID uuid = claim1.getUUID();
	            String uuid_string = uuid.toString();
	            String owner = claim1.getOwner();
	            if(!owner.equals("*")) {
		            Player player = Bukkit.getPlayer(owner);
		            if(player != null && player.isOnline()) {
		            	// Update claims count of player
			            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(uuid);
			            cPlayer.setClaimsCount(cPlayer.getClaimsCount()-claims.size());
		            }
	            }
		            
	            // Remove claims from player's claims
		        playerClaims.computeIfAbsent(uuid, k -> new HashSet<>()).removeAll(claims);
	            
	            // Serialize chunks
	            String chunksData = serializeChunks(claim1.getChunks());
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims_1 SET chunks = ? WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, chunksData);
	                    preparedStatement.setString(2, uuid_string);
	                    preparedStatement.setString(3, claim1.getName());
	                    preparedStatement.executeUpdate();
	                }
	                String deleteQuery = "DELETE FROM scs_claims_1 WHERE owner_uuid = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                	for(Claim claim : claims) {
	                        preparedStatement.setString(1, uuid_string);
	                        preparedStatement.setString(2, claim.getName());
	                        preparedStatement.addBatch();
	                	};
	                	preparedStatement.executeBatch();
	                }
	                return true;
	            } catch (SQLException e) {
	                e.printStackTrace();
	                return false;
	            }
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
    }

    /**
     * Displays particles around the specified chunks for claiming.
     * Particles are shown only at the borders of the chunks and not between adjacent chunks.
     *
     * @param player the player claiming the chunks
     * @param chunks the set of chunks to be displayed
     * @param claim  whether the chunks are being claimed or not
     * @param see if its from the /claim see command
     */
    public void displayChunks(Player player, Set<Chunk> chunks, boolean claim, boolean see) {
        Particle.DustOptions dustOptions = getDustOptions(player, claim, see);

        CompletableFuture<Set<Location>> futureLocations = getParticleLocations(chunks);
        
        futureLocations.thenAccept(particleLocations -> {
	        if (instance.isFolia()) {
	            final int[] counter = {0};
	            Bukkit.getAsyncScheduler().runAtFixedRate(instance, task -> {
	                if (counter[0] >= 10) {
	                    task.cancel();
	                }
	                World world = player.getWorld();
	                particleLocations.stream().forEach(location -> world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0, dustOptions));
	                counter[0]++;
	            }, 0, 500, TimeUnit.MILLISECONDS);
	        } else {
	            new BukkitRunnable() {
	                int counter = 0;
	
	                @Override
	                public void run() {
	                    if (counter >= 10) {
	                        this.cancel();
	                    }
	                    World world = player.getWorld();
	                    particleLocations.stream().forEach(location -> world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0, dustOptions));
	                    counter++;
	                }
	            }.runTaskTimerAsynchronously(instance, 0, 10L);
	        }
        });
    }

    /**
     * Determines the particle options based on the player's claim status.
     *
     * @param player the player
     * @param claim  whether the chunk is being claimed or not
     * @param see if its from the /claim see command
     * @return the dust options for the particles
     */
    private Particle.DustOptions getDustOptions(Player player, boolean claim, boolean see) {
        if (!claim && !see) {
            Chunk chunk = player.getLocation().getChunk();
            if (checkIfClaimExists(chunk)) {
                String playerName = player.getName();
                if (listClaims.get(chunk).getOwner().equals(playerName)) {
                    return new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);
                } else {
                    return new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);
                }
            } else {
                return new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f);
            }
        } else if (see) {
        	return new Particle.DustOptions(Color.fromRGB(124, 0, 255), 1.5f);
        } else {
        	return new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);
        }
    }

    /**
     * Generates a set of locations for particle spawning around the specified chunks.
     * Particles are shown only at the borders of the chunks and not between adjacent chunks.
     *
     * @param chunks the set of chunks
     * @return the set of locations for particle spawning
     */
    private CompletableFuture<Set<Location>> getParticleLocations(Set<Chunk> chunks) {
        Set<Location> locations = new HashSet<>();
        CompletableFuture<Set<Location>> resultFuture = new CompletableFuture<>();

        if (instance.isFolia()) {
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (Chunk chunk : chunks) {
                World world = chunk.getWorld();
                int xStart = chunk.getX() << 4;
                int zStart = chunk.getZ() << 4;
                int xEnd = xStart + 15;
                int zEnd = zStart + 15;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;

                // Asynchronously add border locations only if adjacent chunks are not present
                futures.add(world.getChunkAtAsync(chunk.getX() - 1, chunk.getZ()).thenAccept(adjChunk -> {
                    if (!chunks.contains(adjChunk)) {
                        for (int y = yStart; y <= yEnd; y += 2) {
                            for (int z = zStart; z <= zEnd; z += 2) {
                                locations.add(new Location(world, xStart, y, z));
                            }
                        }
                    }
                }));

                futures.add(world.getChunkAtAsync(chunk.getX() + 1, chunk.getZ()).thenAccept(adjChunk -> {
                    if (!chunks.contains(adjChunk)) {
                        for (int y = yStart; y <= yEnd; y += 2) {
                            for (int z = zStart; z <= zEnd; z += 2) {
                                locations.add(new Location(world, xEnd + 1, y, z));
                            }
                        }
                    }
                }));

                futures.add(world.getChunkAtAsync(chunk.getX(), chunk.getZ() - 1).thenAccept(adjChunk -> {
                    if (!chunks.contains(adjChunk)) {
                        for (int y = yStart; y <= yEnd; y += 2) {
                            for (int x = xStart; x <= xEnd; x += 2) {
                                locations.add(new Location(world, x, y, zStart));
                            }
                        }
                    }
                }));

                futures.add(world.getChunkAtAsync(chunk.getX(), chunk.getZ() + 1).thenAccept(adjChunk -> {
                    if (!chunks.contains(adjChunk)) {
                        for (int y = yStart; y <= yEnd; y += 2) {
                            for (int x = xStart; x <= xEnd; x += 2) {
                                locations.add(new Location(world, x, y, zEnd + 1));
                            }
                        }
                    }
                }));
            }
            // Wait for all async chunk loads to complete
            CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
            allOf.thenRun(() -> {
                resultFuture.complete(locations);
            }).exceptionally(ex -> {
                ex.printStackTrace();
                resultFuture.completeExceptionally(ex);
                return null;
            });
        } else {
            for (Chunk chunk : chunks) {
                World world = chunk.getWorld();
                int xStart = chunk.getX() << 4;
                int zStart = chunk.getZ() << 4;
                int xEnd = xStart + 15;
                int zEnd = zStart + 15;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;

                if (!chunks.contains(world.getChunkAt(chunk.getX() - 1, chunk.getZ()))) {
                    for (int y = yStart; y <= yEnd; y += 2) {
                        for (int z = zStart; z <= zEnd; z += 2) {
                            locations.add(new Location(world, xStart, y, z));
                        }
                    }
                }
                if (!chunks.contains(world.getChunkAt(chunk.getX() + 1, chunk.getZ()))) {
                    for (int y = yStart; y <= yEnd; y += 2) {
                        for (int z = zStart; z <= zEnd; z += 2) {
                            locations.add(new Location(world, xEnd + 1, y, z));
                        }
                    }
                }
                if (!chunks.contains(world.getChunkAt(chunk.getX(), chunk.getZ() - 1))) {
                    for (int y = yStart; y <= yEnd; y += 2) {
                        for (int x = xStart; x <= xEnd; x += 2) {
                            locations.add(new Location(world, x, y, zStart));
                        }
                    }
                }
                if (!chunks.contains(world.getChunkAt(chunk.getX(), chunk.getZ() + 1))) {
                    for (int y = yStart; y <= yEnd; y += 2) {
                        for (int x = xStart; x <= xEnd; x += 2) {
                            locations.add(new Location(world, x, y, zEnd + 1));
                        }
                    }
                }
            }
            resultFuture.complete(locations); // Complete the result future with the locations set
        }

        return resultFuture;
    }


    /**
     * Method to display a chunk when radius claiming.
     *
     * @param player the player claiming the chunk
     * @param centralChunk the central chunk to be displayed
     * @param radius the radius around the central chunk to be displayed
     */
    public void displayChunkBorderWithRadius(Player player, Chunk centralChunk, int radius) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);
        if (instance.isFolia()) {
            final int[] counter = {0};
            Bukkit.getAsyncScheduler().runAtFixedRate(instance, task -> {
                if (counter[0] >= 10) {
                    task.cancel();
                }
                World world = player.getWorld();
                int xStart = (centralChunk.getX() - radius) << 4;
                int zStart = (centralChunk.getZ() - radius) << 4;
                int xEnd = (centralChunk.getX() + radius + 1) << 4;
                int zEnd = (centralChunk.getZ() + radius + 1) << 4;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;
                for (int y = yStart; y <= yEnd; y+=2) {
                    for (int x = xStart; x < xEnd; x+=2) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z < zEnd; z+=2) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }
                counter[0]++;
            }, 0, 500, TimeUnit.MILLISECONDS);
            return;
        }
        new BukkitRunnable() {
            int counter = 0;
            @Override
            public void run() {
                if (counter >= 10) {
                    this.cancel();
                }
                World world = player.getWorld();
                int xStart = (centralChunk.getX() - radius) << 4;
                int zStart = (centralChunk.getZ() - radius) << 4;
                int xEnd = (centralChunk.getX() + radius + 1) << 4;
                int zEnd = (centralChunk.getZ() + radius + 1) << 4;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;
                for (int y = yStart; y <= yEnd; y+=2) {
                    for (int x = xStart; x < xEnd; x+=2) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z < zEnd; z+=2) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }

                counter++;
            }
        }.runTaskTimerAsynchronously(instance, 0, 10L);
    }

    /**
     * Method to send help for commands.
     *
     * @param player the player requesting help
     * @param help the help message
     * @param cmd the command for which help is requested
     */
    public void getHelp(Player player, String help, String cmd) {
    	if(help.equalsIgnoreCase("no arg")) {
            if(cmd.equalsIgnoreCase("claim")) {
            	player.sendMessage(instance.getLanguage().getMessage("available-args").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%args%", String.join(", ", commandArgsClaim)));
            } else if (cmd.equalsIgnoreCase("scs")) {
            	player.sendMessage(instance.getLanguage().getMessage("available-args").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%args%", String.join(", ", commandArgsScs)));
            } else if (cmd.equalsIgnoreCase("parea") || cmd.equalsIgnoreCase("protectedarea")) {
            	player.sendMessage(instance.getLanguage().getMessage("available-args").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%args%", String.join(", ", commandArgsParea)));
            }
            return;
    	}
        String help_msg = instance.getLanguage().getMessage("help-command." + cmd + "-" + help.toLowerCase());
        if (!help_msg.isEmpty()) {
            player.sendMessage(instance.getLanguage().getMessage("help-separator"));
            player.sendMessage(help_msg);
            player.sendMessage(instance.getLanguage().getMessage("help-separator"));
            return;
        }
        if(cmd.equalsIgnoreCase("claim")) {
        	player.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%arg%", help).replace("%args%", String.join(", ", commandArgsClaim)));
        } else if (cmd.equalsIgnoreCase("scs")) {
        	player.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%arg%", help).replace("%args%", String.join(", ", commandArgsScs)));
        } else if (cmd.equalsIgnoreCase("parea") || cmd.equalsIgnoreCase("protectedarea")) {
        	player.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%arg%", help).replace("%args%", String.join(", ", commandArgsParea)));
        }
    }
    
    /**
     * Method to send help for commands.
     *
     * @param sender The command sender
     * @param help the help message
     * @param cmd the command for which help is requested
     */
    public void getHelp(CommandSender sender, String help, String cmd) {
    	if(help.equalsIgnoreCase("no arg")) {
            if(cmd.equalsIgnoreCase("claim")) {
            	sender.sendMessage(instance.getLanguage().getMessage("available-args").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%args%", String.join(", ", commandArgsClaim)));
            } else if (cmd.equalsIgnoreCase("scs")) {
            	sender.sendMessage(instance.getLanguage().getMessage("available-args").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%args%", String.join(", ", commandArgsScs)));
            }
            return;
    	}
        String help_msg = instance.getLanguage().getMessage("help-command." + cmd + "-" + help.toLowerCase());
        if (!help_msg.isEmpty()) {
        	sender.sendMessage(instance.getLanguage().getMessage("help-separator"));
        	sender.sendMessage(help_msg);
        	sender.sendMessage(instance.getLanguage().getMessage("help-separator"));
            return;
        }
        if(cmd.equalsIgnoreCase("claim")) {
        	sender.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%arg%", help).replace("%args%", String.join(", ", commandArgsClaim)));
        } else if (cmd.equalsIgnoreCase("scs")) {
        	sender.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")).replace("%arg%", help).replace("%args%", String.join(", ", commandArgsScs)));
        }
    }
    
    /**
     * Checks if there are no claims within a specified radius around a given chunk,
     * excluding claims that belong to the player.
     *
     * @param centerChunk The central chunk from which to check.
     * @param distance    The radius, in chunks, within which to check for claims.
     * @param playerName  The name of the player to exclude their claims.
     * @return true if there are no conflicting claims within the specified radius, false otherwise.
     */
    public CompletableFuture<Boolean> isAreaClaimFree(Chunk centerChunk, int distance, String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            if (distance == 0) {
                return true;
            }

            World world = centerChunk.getWorld();
            int centerX = centerChunk.getX();
            int centerZ = centerChunk.getZ();

            // Iterate through the listClaims to find claims within the distance
            for (Map.Entry<Chunk, Claim> entry : listClaims.entrySet()) {
                Chunk chunk = entry.getKey();
                Claim claim = entry.getValue();

                if (chunk.getWorld().equals(world)) {
                    int chunkX = chunk.getX();
                    int chunkZ = chunk.getZ();

                    int deltaX = Math.abs(chunkX - centerX);
                    int deltaZ = Math.abs(chunkZ - centerZ);

                    // Check if the chunk is within the specified distance and does not belong to the player
                    if (deltaX <= distance && deltaZ <= distance && !claim.getOwner().equals(playerName)) {
                        return false; // A conflicting claim is found
                    }
                }
            }

            return true; // No conflicting claims found
        });
    }

    /**
     * Method to get the direction (north, south, east or west).
     *
     * @param yaw the yaw angle
     * @return the direction as a string
     */
    private String getDirection(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        if (0 <= yaw && yaw < 45) return instance.getLanguage().getMessage("map-direction-south");
        else if (45 <= yaw && yaw < 135) return instance.getLanguage().getMessage("map-direction-west");
        else if (135 <= yaw && yaw < 225) return instance.getLanguage().getMessage("map-direction-north");
        else if (225 <= yaw && yaw < 315) return instance.getLanguage().getMessage("map-direction-east");
        else if (315 <= yaw && yaw < 360.0) return instance.getLanguage().getMessage("map-direction-south");
        else return "Unknown";
    }

    /**
     * Method to get the map for a player.
     *
     * @param player the player requesting the map
     * @param to the chunk to be displayed on the map
     */
    public void getMap(Player player, Chunk to) {
        instance.executeAsync(() -> {
            String direction = getDirection(player.getLocation().getYaw());
            Chunk centerChunk = to;
            int centerX = centerChunk.getX();
            int centerZ = centerChunk.getZ();
            boolean isClaimed = checkIfClaimExists(centerChunk);
            
            String name = isClaimed 
                ? instance.getLanguage().getMessage("map-actual-claim-name-message").replace("%name%", getClaimNameByChunk(centerChunk)) 
                : instance.getLanguage().getMessage("map-no-claim-name-message");
            String coords = instance.getLanguage().getMessage("map-coords-message").replace("%coords%", centerX + "," + centerZ).replace("%direction%", direction);
            String colorRelationNoClaim = instance.getLanguage().getMessage("map-no-claim-color");
            String colorCursor = instance.getLanguage().getMessage("map-cursor-color");
            String symbolNoClaim = instance.getLanguage().getMessage("map-symbol-no-claim");
            String symbolClaim = instance.getLanguage().getMessage("map-symbol-claim");
            String mapCursor = instance.getLanguage().getMessage("map-cursor");
            World world = player.getWorld();

            StringBuilder mapMessage = new StringBuilder("\n"+colorRelationNoClaim);
            Function<Chunk, String> getChunkSymbol = chunk -> chunk.equals(centerChunk) 
                ? colorCursor + mapCursor + colorRelationNoClaim
                : checkIfClaimExists(chunk) 
                    ? getRelation(player, chunk) + symbolClaim + colorRelationNoClaim
                    : colorRelationNoClaim + symbolNoClaim;

            Map<Integer, String> legendMap = new HashMap<>();
            legendMap.put(-3, "  " + name + (isClaimed ? " " + instance.getLanguage().getMessage("map-actual-claim-name-message-owner").replace("%owner%", listClaims.get(centerChunk).getOwner()) : ""));
            legendMap.put(-2, "  " + coords);
            legendMap.put(0, "  " + instance.getLanguage().getMessage("map-legend-you").replace("%cursor-color%", colorCursor));
            legendMap.put(1, "  " + instance.getLanguage().getMessage("map-legend-free").replace("%no-claim-color%", colorRelationNoClaim));
            legendMap.put(2, "  " + instance.getLanguage().getMessage("map-legend-yours").replace("%claim-relation-member%", instance.getLanguage().getMessage("map-claim-relation-member")));
            legendMap.put(3, "  " + instance.getLanguage().getMessage("map-legend-other").replace("%claim-relation-visitor%", instance.getLanguage().getMessage("map-claim-relation-visitor")));

            if(instance.isFolia()) {
                Bukkit.getRegionScheduler().run(instance, player.getLocation(), task -> {
                	IntStream.rangeClosed(-4, 4).forEach(dz -> {
                        IntStream.rangeClosed(-10, 10).forEach(dx -> {
                            int[] offset = adjustDirection(dx, dz, direction);
                            int X = centerX + offset[0];
                            int Z = centerZ + offset[1];
                            Chunk chunk = world.getChunkAt(X, Z);
                            mapMessage.append(getChunkSymbol.apply(chunk));
                        });
                        if (legendMap.containsKey(dz)) {
                            mapMessage.append(legendMap.get(dz));
                        }
                        mapMessage.append("\n");
                    });
                	instance.executeEntitySync(player, () -> player.sendMessage(mapMessage.toString()));
                });
            } else {
                IntStream.rangeClosed(-4, 4).forEach(dz -> {
                    IntStream.rangeClosed(-10, 10).forEach(dx -> {
                        int[] offset = adjustDirection(dx, dz, direction);
                        int X = centerX + offset[0];
                        int Z = centerZ + offset[1];
                        Chunk chunk = world.getChunkAt(X, Z);
                        mapMessage.append(getChunkSymbol.apply(chunk));
                    });
                    if (legendMap.containsKey(dz)) {
                        mapMessage.append(legendMap.get(dz));
                    }
                    mapMessage.append("\n");
                });
                instance.executeEntitySync(player, () -> player.sendMessage(mapMessage.toString()));
            }
        });
    }

    /**
     * Method to adjust direction (to get North, South, East or West).
     *
     * @param dx the x offset
     * @param dz the z offset
     * @param direction the direction as a string
     * @return an array of adjusted x and z offsets
     */
    private int[] adjustDirection(int dx, int dz, String direction) {
        int relX = dx, relZ = dz;
        if (direction.equalsIgnoreCase(instance.getLanguage().getMessage("map-direction-north"))) return new int[]{relX, relZ};
        if (direction.equalsIgnoreCase(instance.getLanguage().getMessage("map-direction-south"))) {
            relX = -dx;
            relZ = -dz;
            return new int[]{relX, relZ};
        }
        if (direction.equalsIgnoreCase(instance.getLanguage().getMessage("map-direction-east"))) {
            relX = -dz;
            relZ = dx;
            return new int[]{relX, relZ};
        }
        if (direction.equalsIgnoreCase(instance.getLanguage().getMessage("map-direction-west"))) {
            relX = dz;
            relZ = -dx;
            return new int[]{relX, relZ};
        }
        return new int[]{relX, relZ};
    }

    /**
     * Method to get the relation between a player and a claim.
     *
     * @param player the player
     * @param chunk the chunk representing the claim
     * @return the relation as a string
     */
    public String getRelation(Player player, Chunk chunk) {
    	Claim claim = listClaims.get(chunk);
    	if(claim == null) return instance.getLanguage().getMessage("map-claim-relation-visitor");
    	return checkMembre(claim, player) ? instance.getLanguage().getMessage("map-claim-relation-member") : instance.getLanguage().getMessage("map-claim-relation-visitor");
    }
    
    /**
     * Method to update the weather in the claim.
     *
     * @param claim the claim to be updated
     * @param result the new weather state
     */
    public void updateWeatherChunk(Claim claim) {
		Set<Chunk> chunks = claim.getChunks();
    	Bukkit.getOnlinePlayers().stream().forEach(p -> {
			Chunk c = p.getLocation().getChunk();
			if(chunks.contains(c)) {
				boolean value = claim.getPermissionForPlayer("Weather", p);
                if(value) {
                	p.resetPlayerWeather();
                } else {
                	p.setPlayerWeather(WeatherType.CLEAR);
                }
			}
    	});
    }

    /**
     * Method to update the fly in the claim.
     *
     * @param claim The claim to be updated
     * @param result the new fly state
     */
    public void updateFlyChunk(Claim claim) {
		Set<Chunk> chunks = claim.getChunks();
    	Bukkit.getOnlinePlayers().stream().forEach(p -> {
			Chunk c = p.getLocation().getChunk();
			if(chunks.contains(c)) {
				boolean value = claim.getPermissionForPlayer("Fly", p);
                CPlayer cPlayer = instance.getPlayerMain().getCPlayer(p.getUniqueId());
                if(value) {
                    if (cPlayer.getClaimAutofly()) {
                        instance.getPlayerMain().activePlayerFly(p);
                    }
                } else {
                    if (cPlayer.getClaimFly()) {
                        instance.getPlayerMain().removePlayerFly(p);
                    }
                }

			}
    	});
    }
    
    /**
     * Method to reset the weather in the claim.
     *
     * @param claim the claim to be updated
     * @param result the new weather state
     */
    public void resetWeatherChunk(Claim claim) {
		Set<Chunk> chunks = claim.getChunks();
    	Bukkit.getOnlinePlayers().stream().forEach(p -> {
			Chunk c = p.getLocation().getChunk();
			if(chunks.contains(c)) {
                p.resetPlayerWeather();
			}
    	});
    }

    /**
     * Method to reset the fly in the claim.
     *
     * @param claim The claim to be updated
     * @param result the new fly state
     */
    public void resetFlyChunk(Claim claim) {
		Set<Chunk> chunks = claim.getChunks();
    	Bukkit.getOnlinePlayers().stream().forEach(p -> {
			Chunk c = p.getLocation().getChunk();
			if(chunks.contains(c)) {
                CPlayer cPlayer = instance.getPlayerMain().getCPlayer(p.getUniqueId());
                if (cPlayer.getClaimFly()) {
                    instance.getPlayerMain().removePlayerFly(p);
                }
			}
    	});
    }
}