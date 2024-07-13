package fr.xyness.SCS;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import fr.xyness.SCS.Guis.AdminGestion.AdminGestionGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import me.ryanhamshire.GriefPrevention.GriefPrevention;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ClaimMain {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** List of claims by chunk. */
    private Map<Chunk, Claim> listClaims = new HashMap<>();

    /** Mapping of player names to their claims. */
    private Map<String, Set<Claim>> playerClaims = new HashMap<>();

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
            "cancel", "addchunk", "removechunk", "chunks", "main");
    
    /** Set of command arguments for /scs. */
    private Set<String> commandArgsScs = Set.of("transfer", "list", "player", "group", "forceunclaim", "setowner", "set-lang", "set-actionbar", "set-auto-claim", 
    		"set-title-subtitle", "set-economy", "set-claim-confirmation", "set-claim-particles", "set-max-sell-price", "set-bossbar", "set-bossbar-color",
    		"set-bossbar-style", "set-teleportation", "set-teleportation-moving", "add-blocked-interact-block", "add-blocked-entity", "add-blocked-item",
            "remove-blocked-interact-block", "remove-blocked-item", "remove-blocked-entity", "add-disabled-world", "remove-disabled-world", "set-status-setting", 
            "set-default-value", "set-max-length-claim-description", "set-max-length-claim-name", "set-claims-visitors-off-visible", "set-claim-cost", 
            "set-claim-cost-multiplier", "set-chat", "set-protection-message", "set-claim-fly-message-auto-fly", "set-claim-fly-disabled-on-damage",
            "reset-all-player-claims-settings", "reset-all-admin-claims-settings","admin");
    
    /** Set of command arguments for /parea. */
    private Set<String> commandArgsParea = Set.of("setdesc", "settings", "setname", "members", "tp",
    		"list", "ban", "unban", "bans", "add", "remove", "unclaim", "main");
    
    /** Instance of instance. */
    private SimpleClaimSystem instance;
    
    /** Mapping of players to admin setting to modify. */
    private Map<Player,String> playerAdminSetting = new HashMap<>();
    
    
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
        activeTasks.values().parallelStream().forEach(t -> t.cancel());
        activeTasks.clear();
        if(instance.isFolia()) {
        	activeFoliaTasks.values().parallelStream().forEach(t -> t.cancel());
        	activeFoliaTasks.clear();
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
                ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(instance.getPlugin(), subtask -> {
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
                }.runTaskTimer(instance.getPlugin(), 0L, 2L);
                activeTasks.put(player, task);
            }
        };
        updateTask.run();
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
     * @param owner The name of the owner.
     * @return The claim associated with the name, or null if none exists
     */
    public Claim getClaimByName(String name, String owner) {
        return playerClaims.getOrDefault(owner, new HashSet<>()).parallelStream()
                .filter(claim -> claim.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets a set of claims in sale of a player.
     *
     * @param owner The name of the owner.
     * @return A set of claims in sale
     */
    public Set<Claim> getClaimsInSale(String owner) {
        return playerClaims.getOrDefault(owner, new HashSet<>()).stream()
                .filter(claim -> claim.getSale())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all claims for a player.
     *
     * @param player The player to get claims for
     * @return The set of claims for the player
     */
    public Set<Claim> getPlayerClaims(String player) {
        return playerClaims.getOrDefault(player, new HashSet<>());
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
        return claim.getChunks().parallelStream()
                .map(chunk -> chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ())
                .collect(Collectors.toSet());
    }

    /**
     * Gets the number of claims a player has.
     *
     * @param playerName the name of the player
     * @return the number of claims the player has
     */
    public int getPlayerClaimsCount(String playerName) {
        return playerClaims.getOrDefault(playerName, new HashSet<>()).size();
    }

    /**
     * Gets all the claim owners (excluding admin).
     *
     * @return a map of claim owners and their claim counts
     */
    public Map<String, Integer> getClaimsOwnersGui() {
        return playerClaims.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals("admin"))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    /**
     * Gets all the claim owners (excluding admin).
     *
     * @return a set of claim owners
     */
    public Set<String> getClaimsOwners() {
        return playerClaims.keySet()
                .parallelStream()
                .filter(key -> !key.equals("admin"))
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
    	return new HashSet<>(listClaims.values()).parallelStream()
    			.filter(c -> c.getOwner().equals("admin"))
    			.collect(Collectors.toSet()).size();
    }

    /**
     * Gets all members of all claims owned by the specified player.
     *
     * @param owner the owner of the claims
     * @return a set of all members of the owner's claims
     */
    public Set<String> getAllMembersOfAllPlayerClaim(String owner) {
        return listClaims.values().parallelStream()
                .filter(claim -> claim.getOwner().equals(owner))
                .flatMap(claim -> claim.getMembers().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all online claim owners and their claim counts.
     *
     * @return a map of online claim owners and their claim counts
     */
    public Map<String, Integer> getClaimsOnlineOwners() {
        return playerClaims.entrySet().stream()
                .filter(entry -> {
                    Player owner = Bukkit.getPlayer(entry.getKey());
                    return owner != null && owner.isOnline();
                })
                .filter(entry -> !entry.getKey().equals("admin"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }
    
    /**
     * Gets all claim owners with claims in sale and their claim counts.
     *
     * @return a map of claim owners with claims in sale and their claim counts
     */
    public Map<String, Integer> getClaimsOwnersWithSales() {
        return playerClaims.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("admin"))
                .filter(entry -> entry.getValue().stream().anyMatch(Claim::getSale))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (int) entry.getValue().stream().filter(Claim::getSale).count()
                ));
    }

    /**
     * Gets all offline claim owners and their claim counts.
     *
     * @return a map of offline claim owners and their claim counts
     */
    public Map<String, Integer> getClaimsOfflineOwners() {
        return playerClaims.entrySet().stream()
                .filter(entry -> Bukkit.getPlayer(entry.getKey()) == null)
                .filter(entry -> !entry.getKey().equals("admin"))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }
    
    /**
     * This method retrieves the set of members from a specific claim by its name for a given player.
     *
     * @param playerName the name of the player who owns the claim
     * @param name the name of the claim
     * @return a set of members who have access to the specified claim
     */
    public Set<String> getMembersFromClaimName(String playerName, String name) {
        return playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream()
                .filter(claim -> claim.getName().equalsIgnoreCase(name))
                .flatMap(claim -> claim.getMembers().stream())
                .collect(Collectors.toSet());
    }
    
    /**
     * This method retrieves the set of banned players from a specific claim by its name for a given player.
     *
     * @param playerName the name of the player who owns the claim
     * @param name the name of the claim
     * @return a set of banned players who have access to the specified claim
     */
    public Set<String> getBannedFromClaimName(String playerName, String name) {
        return playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream()
                .filter(claim -> claim.getName().equalsIgnoreCase(name))
                .flatMap(claim -> claim.getBans().stream())
                .collect(Collectors.toSet());
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
        return listClaims.entrySet().parallelStream()
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
        return listClaims.entrySet().parallelStream()
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
        return listClaims.values().parallelStream()
                .filter(claim -> claim.getMembers().contains(playerName))
                .flatMap(claim -> claim.getMembers().stream())
                .filter(member -> !member.equals(playerName))
                .distinct()
                .collect(Collectors.toList());
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

        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        int delay = cPlayer.getDelay();

        if (instance.getPlayerMain().checkPermPlayer(player, "scs.bypass") || delay == 0) {
            teleportPlayer(player, loc);
            player.sendMessage(instance.getLanguage().getMessage("teleportation-success"));
            return;
        }

        player.sendMessage(instance.getLanguage().getMessage("teleportation-in-progress").replaceAll("%delay%", String.valueOf(delay)));
        Location originalLocation = player.getLocation().clone();
        playerLocations.put(player, originalLocation);

        Runnable teleportTask = createTeleportTask(player, loc, originalLocation, delay);
        if (instance.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(instance.getPlugin(), task -> {
                teleportTask.run();
                if (!playerLocations.containsKey(player)) task.cancel();
            }, 0, 500, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                public void run() {
                    teleportTask.run();
                    if (!playerLocations.containsKey(player)) this.cancel();
                }
            }.runTaskTimer(instance.getPlugin(), 0L, 10L);
        }
    }

    /**
     * Teleports the player.
     *
     * @param player the player to teleport
     * @param loc    the location to teleport to
     */
    private void teleportPlayer(Player player, Location loc) {
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
     * @param owner the owner of the claim
     * @param name  the name of the claim
     * @return true if the name is already used, false otherwise
     */
    public boolean checkName(String owner, String name) {
        return playerClaims.getOrDefault(owner, new HashSet<>()).parallelStream()
                .noneMatch(claim -> claim.getName().equals(name) && claim.getOwner().equals(owner));
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
     * @param target the target owner of the claim
     * @return the next available ID
     */
    public int findFreeId(String target) {
        return playerClaims.getOrDefault(target, Collections.emptySet())
                .parallelStream()
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
    public static Location getCenterLocationOfChunk(Chunk chunk) {
        World world = chunk.getWorld();
        int centerX = (chunk.getX() << 4) + 8;
        int centerZ = (chunk.getZ() << 4) + 8;
        int maxY = world.getHighestBlockYAt(centerX, centerZ);
        return new Location(world, centerX, maxY, centerZ);
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
        		int id = findFreeId(owner);
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
        		
        		// Create X and Z list for chunks
                List<Integer> X = new ArrayList<>();
                List<Integer> Z = new ArrayList<>();
                
                chunks.forEach(c -> {
                    X.add(c.getX());
                    Z.add(c.getZ());
                });

                // Build X and Z strings
                StringBuilder sbX = new StringBuilder();
                for (Integer x : X) {
                    sbX.append(x).append(";");
                }
                if (sbX.length() > 0) {
                    sbX.setLength(sbX.length() - 1);
                }
                
                StringBuilder sbZ = new StringBuilder();
                for (Integer z : Z) {
                    sbZ.append(z).append(";");
                }
                if (sbZ.length() > 0) {
                    sbZ.setLength(sbZ.length() - 1);
                }
                
                // Update database
                try (Connection connection = instance.getDataSource().getConnection();
                        PreparedStatement stmt = connection.prepareStatement(
                                "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                       stmt.setInt(1, id);
                   	stmt.setString(2, uuid);
                       stmt.setString(3, owner);
                       stmt.setString(4, claim_name);
                       stmt.setString(5, instance.getLanguage().getMessage("default-description"));
                       stmt.setString(6, sbX.toString());
                       stmt.setString(7, sbZ.toString());
                       stmt.setString(8, world);
                       stmt.setString(9, getLocationString(loc));
                       stmt.setString(10, owner);
                       stmt.setString(11, instance.getSettings().getDefaultValuesCode());
                       stmt.executeUpdate();
                       i[0]++;
                   } catch (SQLException e) {
                       e.printStackTrace();
                   }
        	}
    		instance.executeSync(() -> {
    			sender.sendMessage(AdminGestionMainGui.getNumberSeparate(String.valueOf(i[0]))+" imported claims, reloading..");
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
            localConfig.setJdbcUrl("jdbc:sqlite:instance.getPlugin()/SimpleClaimSystem/claims.db");
            localConfig.setDriverClassName("org.sqlite.JDBC");
            try (HikariDataSource localDataSource = new HikariDataSource(localConfig);
                 Connection localConn = localDataSource.getConnection();
                 PreparedStatement selectStmt = localConn.prepareStatement("SELECT * FROM scs_claims");
                 ResultSet rs = selectStmt.executeQuery();
                 Connection remoteConn = instance.getDataSource().getConnection();
                 PreparedStatement insertStmt = remoteConn.prepareStatement(
                         "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions, isSale, SalePrice) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
                 )) {

                int count = 0;
                while (rs.next()) {
                    for (int i = 1; i <= 13; i++) {
                        insertStmt.setObject(i, rs.getObject(i));
                    }
                    insertStmt.addBatch();
                    count++;
                }
                insertStmt.executeBatch();
                instance.getPlugin().getLogger().info(count + " claims transferred");
                instance.getPlugin().getLogger().info("Safe reloading..");
                instance.executeSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "aclaim reload"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
    	});
    }

    /**
     * Loads claims from the database.
     */
    public void loadClaims() {
    	instance.info(" ");
    	instance.info(net.md_5.bungee.api.ChatColor.DARK_GREEN + "Loading claims..");
    	
        StringBuilder sb = new StringBuilder();
        for (String key : instance.getSettings().getDefaultValues().keySet()) {
            if (instance.getSettings().getDefaultValues().get(key)) {
                sb.append("1");
                continue;
            }
            sb.append("0");
        }
        instance.getSettings().setDefaultValuesCode(sb.toString());

        // Checking permissions (for update or new features)
        try (Connection connection = instance.getDataSource().getConnection()) {
            connection.setAutoCommit(false); // Start transaction
            String insertQuery = "UPDATE scs_claims SET Permissions = ? WHERE id_pk = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                String getQuery = "SELECT * FROM scs_claims";
                try (PreparedStatement stat = connection.prepareStatement(getQuery);
                     ResultSet resultSet = stat.executeQuery()) {

                    int batchCount = 0;

                    while (resultSet.next()) {
                        String perms = resultSet.getString("Permissions");
                        int id = resultSet.getInt("id_pk");
                        String perm = perms;
                        if (perm.length() != instance.getSettings().getDefaultValuesCode().length()) {
                            int diff = instance.getSettings().getDefaultValuesCode().length() - perm.length();
                            if (diff < 0) {
                                StringBuilder permCompleted = new StringBuilder(perm);
                                for (int i = 0; i < perm.length() - diff; i++) {
                                    permCompleted.append(instance.getSettings().getDefaultValuesCode().charAt(perm.length() + i));
                                }
                                String permFinal = permCompleted.toString();
                                preparedStatement.setString(1, permFinal);
                                preparedStatement.setInt(2, id);
                                preparedStatement.addBatch();
                                batchCount++;
                            } else {
                                StringBuilder permCompleted = new StringBuilder(perm);
                                for (int i = 0; i < diff; i++) {
                                    permCompleted.append(instance.getSettings().getDefaultValuesCode().charAt(perm.length() + i));
                                }
                                String permFinal = permCompleted.toString();
                                preparedStatement.setString(1, permFinal);
                                preparedStatement.setInt(2, id);
                                preparedStatement.addBatch();
                                batchCount++;
                            }
                        }
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
        int[] chunks_count = {0};
        int protected_areas_count = 0;
        try (Connection connection = instance.getDataSource().getConnection()) {
            String getQuery = "SELECT * FROM scs_claims";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        max_i++;
                        // General data
                        String permissions = resultSet.getString("Permissions");
                        String owner = resultSet.getString("name");
                        if(owner.equals("admin")) protected_areas_count++;
                        String name = resultSet.getString("claim_name");
                        String description = resultSet.getString("claim_description");
                        int id = resultSet.getInt("id");
                        
                        // World data
                        String world_name = resultSet.getString("World");
                        World check_world = Bukkit.getWorld(world_name);
                        World world = check_world == null ? Bukkit.createWorld(new WorldCreator(world_name)) : check_world;
                        if(world == null) continue;
                        
                        // Location data
                        String[] parts = resultSet.getString("Location").split(";");
                        double L_X = Double.parseDouble(parts[0]);
                        double L_Y = Double.parseDouble(parts[1]);
                        double L_Z = Double.parseDouble(parts[2]);
                        float L_Yaw = (float) Double.parseDouble(parts[3]);
                        float L_Pitch = (float) Double.parseDouble(parts[4]);
                        Location location = new Location(world, L_X, L_Y, L_Z, L_Yaw, L_Pitch);
                        
                        // Members data
                        String s_members = resultSet.getString("Members");
                        Set<String> members = new HashSet<>();
                        if (!s_members.isBlank()) {
                            parts = s_members.split(";");
                            for (String m : parts) {
                                members.add(m);
                            }
                        }
                        
                        // Banned players data
                        String s_bans = resultSet.getString("Bans");
                        Set<String> bans = new HashSet<>();
                        if (!s_bans.isBlank()) {
                            parts = s_bans.split(";");
                            for (String m : parts) {
                                bans.add(m);
                            }
                        }
                        
                        // Permissions data
                        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>();
                        int count_i = 0;
                        for (String perm_key : instance.getSettings().getDefaultValues().keySet()) {
                            char currentChar = permissions.charAt(count_i);
                            count_i++;
                            if (currentChar == '1') {
                                perms.put(perm_key, true);
                                continue;
                            }
                            perms.put(perm_key, false);
                        }
                        
                        // Economy data
                        boolean sale = resultSet.getBoolean("isSale");
                        Double price = resultSet.getDouble("SalePrice");
                        
                        // Chunks data
                        List<Integer> X = Arrays.stream(resultSet.getString("X").split(";"))
                                .map(String::trim)
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                        List<Integer> Z = Arrays.stream(resultSet.getString("Z").split(";"))
                                .map(String::trim)
                                .map(Integer::parseInt)
                                .collect(Collectors.toList());
                        if(X.size() != Z.size()) continue;

                        i[0]++;
                        chunks_count[0] += X.size();
                        Set<Chunk> chunks = ConcurrentHashMap.newKeySet();
                        
                        instance.getPlayerMain().loadOwner(owner);
                        
                        Runnable task = () -> {
                            Claim claim = new Claim(chunks, owner, members, location, name, description, perms, sale, price, bans, id);
                            chunks.parallelStream().forEach(c -> listClaims.put(c, claim));
                            if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createChunkZone(chunks, name, owner);
                            if (instance.getSettings().getBooleanSetting("preload-chunks")) {
                                if (instance.isFolia()) {
                                    chunks.parallelStream().forEach(c -> Bukkit.getRegionScheduler().execute(instance.getPlugin(), world, c.getX(), c.getZ(), () -> c.load(true)));
                                } else {
                                    List<CompletableFuture<Void>> loadFutures = chunks.parallelStream()
                                        .map(chunk -> CompletableFuture.runAsync(() -> {
                                            Bukkit.getScheduler().callSyncMethod(instance.getPlugin(), (Callable<Void>) () -> {
                                                chunk.load(true);
                                                return null;
                                            });
                                        }))
                                        .collect(Collectors.toList());

                                    CompletableFuture<Void> allLoaded = CompletableFuture.allOf(loadFutures.toArray(new CompletableFuture[0]));
                                    allLoaded.join();
                                }
                            }
                            if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
                                if (instance.isFolia()) {
                                    chunks.parallelStream().forEach(c -> Bukkit.getRegionScheduler().execute(instance.getPlugin(), world, c.getX(), c.getZ(), () -> c.setForceLoaded(true)));
                                } else {
                                    List<CompletableFuture<Void>> keepLoadedFutures = chunks.parallelStream()
                                        .map(chunk -> CompletableFuture.runAsync(() -> {
                                            Bukkit.getScheduler().callSyncMethod(instance.getPlugin(), (Callable<Void>) () -> {
                                                chunk.setForceLoaded(true);
                                                return null;
                                            });
                                        }))
                                        .collect(Collectors.toList());

                                    CompletableFuture<Void> allKeptLoaded = CompletableFuture.allOf(keepLoadedFutures.toArray(new CompletableFuture[0]));
                                    allKeptLoaded.join();
                                }
                            }
                            if (playerClaims.containsKey(owner)) {
                                playerClaims.get(owner).add(claim);
                            } else {
                                playerClaims.put(owner, new HashSet<>(Set.of(claim)));
                            }
                            instance.getBossBars().activateBossBar(chunks);
                        };

                        Iterator<Integer> xIterator = X.iterator();
                        Iterator<Integer> zIterator = Z.iterator();

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
        } catch (SQLException e) {
            e.printStackTrace();
        }
        instance.info(AdminGestionMainGui.getNumberSeparate(String.valueOf(i[0]))+"/"+AdminGestionMainGui.getNumberSeparate(String.valueOf(max_i))+" claims loaded.");
        instance.info("> including "+AdminGestionMainGui.getNumberSeparate(String.valueOf(protected_areas_count))+" protected areas.");
        instance.info("> including "+AdminGestionMainGui.getNumberSeparate(String.valueOf(chunks_count[0]))+" chunks" + (instance.getSettings().getBooleanSetting("preload-chunks") ? " (all preloaded)." : "."));
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
		        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
		
		        // Check if the chunk is already claimed
		        if (listClaims.containsKey(chunk)) {
		        	instance.executeEntitySync(player, () -> handleClaimConflict(player, chunk));
		            return false;
		        }
		
		        // Check if the player can claim
		        if (!cPlayer.canClaim()) {
		        	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore")));
		            return false;
		        }
		
		        // Check if the player can pay
		        if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
		            double price = instance.getSettings().getBooleanSetting("claim-cost-multiplier") ? cPlayer.getMultipliedCost() : cPlayer.getCost();
		            double balance = instance.getVault().getPlayerBalance(playerName);
		
		            if (balance < price) {
		            	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replaceAll("%missing-price%", String.valueOf(price - balance)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
		                return false;
		            }
		
		            instance.getVault().removePlayerBalance(playerName, price);
		            if (price > 0) instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replaceAll("%price%", String.valueOf(price)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
		        }
		
		        // Display particles, update player claims count and send message to player (success)
		        if (instance.getSettings().getBooleanSetting("claim-particles")) instance.executeSync(() -> displayChunks(player, Set.of(chunk), true, false));
		        cPlayer.setClaimsCount(cPlayer.getClaimsCount() + 1);
		        int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
		        instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("create-claim-success").replaceAll("%remaining-claims%", String.valueOf(remainingClaims))));
		
		        // Create default values, name, loc, perms and Claim
		        int id = findFreeId(playerName);
		        String uuid = player.getUniqueId().toString();
		        String claimName = "claim-" + String.valueOf(id);
		        String description = instance.getLanguage().getMessage("default-description");
		        String locationString = getLocationString(player.getLocation());
		        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
		        Claim newClaim = new Claim(Set.of(chunk), playerName, Set.of(playerName), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(),id);
		
		        // Add claim to claims list and player claims list
		        listClaims.put(chunk, newClaim);
		        playerClaims.computeIfAbsent(playerName, k -> new HashSet<>()).add(newClaim);
		        
		        // Create bossbars and maps
		        if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createChunkZone(Set.of(chunk), claimName, playerName);
		        if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createChunkZone(Set.of(chunk), claimName, playerName);
		        if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createChunkZone(Set.of(chunk), claimName, playerName);
		        if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		Bukkit.getRegionScheduler().execute(instance.getPlugin(), chunk.getWorld(), chunk.getX(), chunk.getZ(), () -> chunk.setForceLoaded(true));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance.getPlugin(), (Callable<Void>) () -> {
	            			chunk.setForceLoaded(true);
	            			return null;
	            		});
	            	}
		        }
		        instance.executeSync(() -> instance.getBossBars().activateBossBar(chunk));
		        
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
    private void handleClaimConflict(Player player, Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        String owner = claim.getOwner();
        if (owner.equals("admin")) {
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
	    		// Check if the chunk is already claimed
		        if (listClaims.containsKey(chunk)) {
		            instance.executeEntitySync(player, () -> handleClaimConflict(player, chunk));
		            return false;
		        }
		
		        // Display particles and send message to player (success)
		        if (instance.getSettings().getBooleanSetting("claim-particles")) instance.executeSync(() -> displayChunks(player, Set.of(chunk), true, false));
		        instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("create-protected-area-success")));
		
		        // Create default values, name, loc, perms and Claim
		        String uuid = "aucun";
		        int id = findFreeId("admin");
		        String claimName = "admin-" + String.valueOf(id);
		        String description = instance.getLanguage().getMessage("default-description");
		        String locationString = getLocationString(player.getLocation());
		        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
		        Claim newClaim = new Claim(Set.of(chunk), "admin", new HashSet<>(), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(),id);
		
		        // Add claim to claims list and protected areas list ("admin" in playerClaims)
		        listClaims.put(chunk, newClaim);
		        playerClaims.computeIfAbsent("admin", k -> new HashSet<>()).add(newClaim);
		
		        // Create bossbars and maps
		        if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createChunkZone(Set.of(chunk), claimName, "admin");
		        if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createChunkZone(Set.of(chunk), claimName, "admin");
		        if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createChunkZone(Set.of(chunk), claimName, "admin");
		        if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		Bukkit.getRegionScheduler().execute(instance.getPlugin(), chunk.getWorld(), chunk.getX(), chunk.getZ(), () -> chunk.setForceLoaded(true));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance.getPlugin(), (Callable<Void>) () -> {
	            			chunk.setForceLoaded(true);
	            			return null;
	            		});
	            	}
		        }
		        instance.executeSync(() -> instance.getBossBars().activateBossBar(chunk));
		
		        // Updata database
		        return insertClaimIntoDatabase(id, uuid, "admin", claimName, description, chunk, locationString);
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
     * @param playerName     the name of the player creating the claim
     * @param claimName      the name of the claim
     * @param description    the description of the claim
     * @param chunk          the chunk being claimed
     * @param locationString the location string of the claim
     */
    private boolean insertClaimIntoDatabase(int id, String uuid, String playerName, String claimName, String description, Chunk chunk, String locationString) {
        try (Connection connection = instance.getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions, Bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, id);
        	stmt.setString(2, uuid);
            stmt.setString(3, playerName);
            stmt.setString(4, claimName);
            stmt.setString(5, description);
            stmt.setString(6, String.valueOf(chunk.getX()));
            stmt.setString(7, String.valueOf(chunk.getZ()));
            stmt.setString(8, chunk.getWorld().getName());
            stmt.setString(9, locationString);
            stmt.setString(10, playerName.equals("admin") ? "" : playerName);
            stmt.setString(11, instance.getSettings().getDefaultValuesCode());
            stmt.setString(12, "");
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
	            Chunk chunk = player.getLocation().getChunk();
	            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
	
	            // Check if all claims are free to claim
	            Set<Chunk> chunksToClaim = chunks.stream()
	                    .filter(c -> !checkIfClaimExists(c))
	                    .collect(Collectors.toSet());
	
	            if (chunks.size() != chunksToClaim.size()) {
	                instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cant-radius-claim-already-claim")));
	                return false;
	            }
	
	            // Check if player can claim
	            if (!cPlayer.canClaim()) {
	                instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore")));
	                return false;
	            }
	
	            // Check if player can claim with all these chunks
	            if (!cPlayer.canClaimWithNumber(chunksToClaim.size())) {
	                instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks")));
	                return false;
	            }
	
	            // Check if player can pay
	            double price = calculateClaimPrice(cPlayer, chunksToClaim.size());
	            if (price > 0 && !processPayment(player, playerName, price)) {
	                return false;
	            }
	
	            // Get uuid of the player
	            String uuid = player.getUniqueId().toString();
	
	            // Create default values, name, loc, perms and Claim
	            int id = findFreeId(playerName);
	            String claimName = "claim-" + id;
	            String description = instance.getLanguage().getMessage("default-description");
	            String locationString = getLocationString(player.getLocation());
	            LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
	            Claim newClaim = new Claim(chunksToClaim, playerName, Set.of(playerName), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(), id);
	
	            // Add the claim to claims list of the player
	            playerClaims.computeIfAbsent(playerName, k -> ConcurrentHashMap.newKeySet()).add(newClaim);
	
	            // Display particles if enabled, send message to player (success) and update his claims count
	            if (instance.getSettings().getBooleanSetting("claim-particles")) {
	                instance.executeSync(() -> displayChunkBorderWithRadius(player, player.getLocation().getChunk(), radius));
	            }
	            cPlayer.setClaimsCount(cPlayer.getClaimsCount() + 1);
	            int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
	            instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("create-claim-radius-success").replace("%number%", AdminGestionMainGui.getNumberSeparate(String.valueOf(chunksToClaim.size()))).replace("%remaining-claims%", AdminGestionMainGui.getNumberSeparate(String.valueOf(remainingClaims))).replace("%claim-name%", claimName)));
	
	            // Create bossbars, maps
	            List<Integer> X = Collections.synchronizedList(new ArrayList<>());
	            List<Integer> Z = Collections.synchronizedList(new ArrayList<>());
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunksToClaim));
	            chunksToClaim.forEach(c -> {
	                listClaims.put(c, newClaim);
	                X.add(c.getX());
	                Z.add(c.getZ());
	            });
	            if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createChunkZone(chunksToClaim, claimName, playerName);
	            if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createChunkZone(chunksToClaim, claimName, playerName);
	            if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createChunkZone(chunksToClaim, claimName, playerName);
	            if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		chunksToClaim.parallelStream().forEach(c -> Bukkit.getRegionScheduler().execute(instance.getPlugin(), c.getWorld(), c.getX(), c.getZ(), () -> c.setForceLoaded(true)));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance.getPlugin(), (Callable<Void>) () -> {
	            			instance.executeSync(() -> chunksToClaim.parallelStream().forEach(c -> c.setForceLoaded(true)));
	            			return null;
	            		});
	            	}
	            }
	
	            // Create X and Z strings
	            String xString = String.join(";", X.stream().map(String::valueOf).collect(Collectors.toList()));
	            String zString = String.join(";", Z.stream().map(String::valueOf).collect(Collectors.toList()));
	
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection();
	                 PreparedStatement stmt = connection.prepareStatement(
	                         "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions, Bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
	                stmt.setInt(1, id);
	                stmt.setString(2, uuid);
	                stmt.setString(3, playerName);
	                stmt.setString(4, claimName);
	                stmt.setString(5, description);
	                stmt.setString(6, xString);
	                stmt.setString(7, zString);
	                stmt.setString(8, chunk.getWorld().getName());
	                stmt.setString(9, locationString);
	                stmt.setString(10, playerName.equals("admin") ? "" : playerName);
	                stmt.setString(11, instance.getSettings().getDefaultValuesCode());
	                stmt.setString(12, "");
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
		        String playerName = "admin";
		        Chunk chunk = player.getLocation().getChunk();
		
		        // Check if all claims are free to claim
		        Set<Chunk> chunksToClaim = chunks.stream()
		                .filter(c -> !checkIfClaimExists(c))
		                .collect(Collectors.toSet());
		        
		        if (chunks.size() != chunksToClaim.size()) {
		            instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cant-radius-claim-already-claim")));
		            return false;
		        }
		        
		        // Create default values, name, loc, perms and Claim
		        int id = findFreeId("admin");
		        String claimName = "admin-" + String.valueOf(id);
		        String description = instance.getLanguage().getMessage("default-description");
		        String locationString = getLocationString(player.getLocation());
		        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
		        Claim newClaim = new Claim(chunksToClaim, playerName, new HashSet<>(), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>(),id);
		
		        // Add the claim to protected areas list ("admin" in playerClaims)
		        playerClaims.computeIfAbsent("admin", k -> new HashSet<>()).add(newClaim);
		
		        // Display particles if enabled and send message to player (success)
		        if (instance.getSettings().getBooleanSetting("claim-particles")) instance.executeSync(() -> displayChunkBorderWithRadius(player, player.getLocation().getChunk(), radius));
		        instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("create-protected-area-radius-success").replace("%number%", String.valueOf(chunksToClaim.size())).replace("%claim-name%", claimName)));
		        
		        // Create bossbars, maps
		        List<Integer> X = Collections.synchronizedList(new ArrayList<>());
		        List<Integer> Z = Collections.synchronizedList(new ArrayList<>());
		        instance.executeSync(() -> instance.getBossBars().activateBossBar(chunksToClaim));
		        chunksToClaim.parallelStream().forEach(c -> {
		            listClaims.put(c, newClaim);
		            X.add(c.getX());
		            Z.add(c.getZ());
		        });
	            if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createChunkZone(chunksToClaim, claimName, "admin");
	            if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createChunkZone(chunksToClaim, claimName, "admin");
	            if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createChunkZone(chunksToClaim, claimName, "admin");
	            if (instance.getSettings().getBooleanSetting("keep-chunks-loaded")) {
	            	if(instance.isFolia()) {
	            		chunksToClaim.parallelStream().forEach(c -> Bukkit.getRegionScheduler().execute(instance.getPlugin(), c.getWorld(), c.getX(), c.getZ(), () -> c.setForceLoaded(true)));
	            	} else {
	            		Bukkit.getScheduler().callSyncMethod(instance.getPlugin(), (Callable<Void>) () -> {
	            			instance.executeSync(() -> chunksToClaim.parallelStream().forEach(c -> c.setForceLoaded(true)));
	            			return null;
	            		});
	            	}
	            }
	            
	            // Create X string
		        StringBuilder sb = new StringBuilder();
		        for (Integer x : X) {
		            sb.append(x).append(";");
		        }
		        if (sb.length() > 0) {
		            sb.setLength(sb.length() - 1);
		        }
		        
		        // Create Z String
		        StringBuilder sb2 = new StringBuilder();
		        for (Integer z : Z) {
		            sb2.append(z).append(";");
		        }
		        if (sb2.length() > 0) {
		            sb2.setLength(sb2.length() - 1);
		        }
		        
		        // Update database
	            try (Connection connection = instance.getDataSource().getConnection();
	                    PreparedStatement stmt = connection.prepareStatement(
	                            "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions, Bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
	                   stmt.setInt(1, id);
	            	   stmt.setString(2, "aucun");
	                   stmt.setString(3, "admin");
	                   stmt.setString(4, claimName);
	                   stmt.setString(5, description);
	                   stmt.setString(6, sb.toString());
	                   stmt.setString(7, sb2.toString());
	                   stmt.setString(8, chunk.getWorld().getName());
	                   stmt.setString(9, locationString);
	                   stmt.setString(10, playerName.equals("admin") ? "" : playerName);
	                   stmt.setString(11, instance.getSettings().getDefaultValuesCode());
	                   stmt.setString(12, "");
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
    private double calculateClaimPrice(CPlayer cPlayer, int numClaims) {
        if (!instance.getSettings().getBooleanSetting("economy") || !instance.getSettings().getBooleanSetting("claim-cost")) {
            return 0;
        }
        return instance.getSettings().getBooleanSetting("claim-cost-multiplier") ? cPlayer.getRadiusMultipliedCost(numClaims) : cPlayer.getCost() * numClaims;
    }

    /**
     * Processes the payment for creating claims.
     *
     * @param player    the player creating the claims
     * @param playerName the name of the player creating the claims
     * @param price     the total price for creating the claims
     * @return true if the payment was successful, false otherwise
     */
    private boolean processPayment(Player player, String playerName, double price) {
        double balance = instance.getVault().getPlayerBalance(playerName);
        if (balance < price) {
            player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", String.valueOf(price - balance)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
            return false;
        }
        instance.getVault().removePlayerBalance(playerName, price);
        player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replace("%price%", String.valueOf(price)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
        return true;
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
    public boolean canPermCheck(Chunk chunk, String perm) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getPermission(perm);
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
    	return claim == null ? false : claim.getMembers().contains(player.getName());
    }

    /**
     * Checks if a player name is a member of a claim.
     *
     * @param claim  the claim to check
     * @param targetName the name of the player to check
     * @return true if the player name is a member of the claim, false otherwise
     */
    public boolean checkMembre(Claim claim, String targetName) {
        return claim != null && claim.getMembers().stream().anyMatch(member -> member.equalsIgnoreCase(targetName));
    }
    
    /**
     * Checks if a player is banned from a claim.
     *
     * @param chunk  the chunk to check
     * @param player the player to check
     * @return true if the player is banned from the claim, false otherwise
     */
    public boolean checkBan(Chunk chunk, Player player) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getBans().contains(player.getName());
    }

    /**
     * Checks if a player name is banned from a claim.
     *
     * @param claim  the claim to check
     * @param targetName the name of the player to check
     * @return true if the player name is banned from the claim, false otherwise
     */
    public boolean checkBan(Claim claim, String targetName) {
        return claim != null && claim.getBans().stream().anyMatch(ban -> ban.equalsIgnoreCase(targetName));
    }

    /**
     * Gets the real name of a player from claim members.
     *
     * @param claim  the claim to check
     * @param targetName the name of the player to check
     * @return the real name of the player, or the target name if not found
     */
    public String getRealNameFromClaimMembers(Claim claim, String targetName) {
        return claim != null ? claim.getMembers().stream().filter(member -> member.equalsIgnoreCase(targetName)).findFirst().orElse(targetName) : targetName;
    }

    /**
     * Gets the real name of a player from claim bans.
     *
     * @param claim  the claim to check
     * @param targetName the name of the player to check
     * @return the real name of the player, or the target name if not found
     */
    public String getRealNameFromClaimBans(Claim claim, String targetName) {
        return claim != null ? claim.getBans().stream().filter(ban -> ban.equalsIgnoreCase(targetName)).findFirst().orElse(targetName) : targetName;
    }

    /**
     * Updates a claim's permission.
     *
     * @param player the player updating the permission
     * @param claim  the claim to update the permission for
     * @param perm   the permission to update
     * @param result the new value of the permission
     * @return true if the permission was updated successfully, false otherwise
     */
    public CompletableFuture<Boolean> updatePerm(Player player, Claim claim, String perm, boolean result) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
		        claim.getPermissions().put(perm, result);
		
		        if (perm.equals("Weather")) updateWeatherChunk(claim.getChunks(), result);
		        if (perm.equals("Fly")) updateFlyChunk(claim.getChunks(), result);
		
		        String permissions = claim.getPermissions().entrySet().stream()
		                .map(entry -> entry.getValue() ? "1" : "0")
		                .collect(Collectors.joining());
		
		        String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
		        try (Connection connection = instance.getDataSource().getConnection();
		             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
		            preparedStatement.setString(1, permissions);
		            preparedStatement.setString(2, player.getUniqueId().toString());
		            preparedStatement.setString(3, player.getName());
		            preparedStatement.setString(4, claim.getName());
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
     * Updates a protected area's permission.
     *
     * @param player the player updating the permission
     * @param claim  the claim to update the permission for
     * @param perm   the permission to update
     * @param result the new value of the permission
     * @return true if the permission was updated successfully, false otherwise
     */
    public CompletableFuture<Boolean> updateAdminPerm(Claim claim, String perm, boolean result) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
		        claim.getPermissions().put(perm, result);
		
		        if (perm.equals("Weather")) updateWeatherChunk(claim.getChunks(), result);
		        if (perm.equals("Fly")) updateFlyChunk(claim.getChunks(), result);
		
		        String permissions = claim.getPermissions().entrySet().stream()
		                .map(entry -> entry.getValue() ? "1" : "0")
		                .collect(Collectors.joining());
		
		        String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
		        try (Connection connection = instance.getDataSource().getConnection();
		             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
		            preparedStatement.setString(1, permissions);
		            preparedStatement.setString(2, "aucun");
		            preparedStatement.setString(3, "admin");
		            preparedStatement.setString(4, claim.getName());
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
     * Method to apply current settings to all admin claims.
     *
     * @param claim the claim from which to apply settings to all admin claims
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> applyAllSettingsAdmin(Claim c) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Update perms
	            LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(c.getPermissions());
	            playerClaims.getOrDefault("admin", new HashSet<>()).parallelStream().forEach(claim -> claim.setPermissions(perms));
	            
	            // Build the perms string
	            StringBuilder sb = new StringBuilder();
	            for (String key : perms.keySet()) {
	                sb.append(perms.get(key) ? "1" : "0");
	            }
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, sb.toString());
	                    preparedStatement.setString(2, "aucun");
	                    preparedStatement.setString(3, "admin");
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
     * Method to apply current settings to all player's claims.
     *
     * @param claim the claim from which to apply settings to all player's claims
     * @param player the player whose claims will be updated
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> applyAllSettings(Claim c, Player player) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String playerName = player.getName();
	        	
	        	// Update perms
	            LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(c.getPermissions());
	            playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> claim.setPermissions(perms));
	            
	            // Build the perms string
	            StringBuilder sb = new StringBuilder();
	            for (String key : perms.keySet()) {
	                sb.append(perms.get(key) ? "1" : "0");
	            }
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, sb.toString());
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, playerName);
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
     * Method to apply current settings to all player's claims.
     *
     * @param claim the claim from which to apply settings to all player's claims
     * @param owner the owner whose claims will be updated
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> applyAllSettings(Claim c, String owner) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Update perms
	            LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(c.getPermissions());
	            playerClaims.getOrDefault(owner, new HashSet<>()).parallelStream().forEach(claim -> claim.setPermissions(perms));
	            
	            // Get uuid of the owner
	        	String uuid = "";
	        	Player target = Bukkit.getPlayer(owner);
	        	if(target == null) {
	        		uuid = Bukkit.getOfflinePlayer(owner).getUniqueId().toString();
	        	} else {
	        		uuid = target.getUniqueId().toString();
	        	}
	        	
	        	// Build the perms string
	            StringBuilder sb = new StringBuilder();
	            for (String key : perms.keySet()) {
	                sb.append(perms.get(key) ? "1" : "0");
	            }
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, sb.toString());
	                    preparedStatement.setString(2, uuid);
	                    preparedStatement.setString(3, owner);
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
     * Method to ban a player from a player's claim.
     *
     * @param player the player who owns the claim
     * @param claim the claim representing the claim
     * @param name the name of the player to be banned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addClaimBan(Player player, Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String playerName = player.getName();
	        	String claimName = claim.getName();
	        	
	        	// Add banned and remove member
	        	claim.addBan(name);
	        	claim.removeMember(name);
	        	
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("banned-claim-player").replaceAll("%owner%", playerName).replaceAll("%claim-name%", claimName));
		        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replaceAll("%owner%", playerName).replaceAll("%claim-name%", claimName));
		        });
		        
		        // Update database
		        String banString = String.join(";", claim.getBans());
		        String memberString = String.join(";", claim.getMembers());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ?, Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, banString);
	                    preparedStatement.setString(2, memberString);
	                    preparedStatement.setString(3, player.getUniqueId().toString());
	                    preparedStatement.setString(4, playerName);
	                    preparedStatement.setString(5, claimName);
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
     * Method to ban a member from an admin claim.
     *
     * @param claim the claim representing the admin claim
     * @param name the name of the member to be banned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAdminClaimBan(Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String claimName = claim.getName();
	        	
	        	// Add banned and remove member
	        	claim.addBan(name);
	        	claim.removeMember(name);
	        	
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("banned-claim-protected-area-player").replaceAll("%claim-name%", claimName));
		        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-protected-area-player").replaceAll("%claim-name%", claimName));
		        });
		        
		        // Update database
		        String banString = String.join(";", claim.getBans());
		        String memberString = String.join(";", claim.getMembers());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ?, Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, banString);
	                    preparedStatement.setString(2, memberString);
	                    preparedStatement.setString(3, "aucun");
	                    preparedStatement.setString(4, "admin");
	                    preparedStatement.setString(5, claimName);
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
     * Method to unban a player from a player's claim.
     *
     * @param player the player who owns the claim
     * @param claim the claim representing the claim
     * @param name the name of the player to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeClaimBan(Player player, Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String playerName = player.getName();
	        	String claimName = claim.getName();
	        	
	        	// Remove banned
	            claim.removeBan(name);
	            
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-claim-player").replaceAll("%owner%", playerName).replaceAll("%claim-name%", claimName));
		        });
	            
	            // Update database
	            String banString = String.join(";", claim.getBans());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, banString);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, playerName);
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
     * Method to unban a member from an admin claim.
     *
     * @param claim the claim representing the admin claim
     * @param name the name of the member to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAdminClaimBan(Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Remove banned
	            claim.removeBan(name);
	            
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-all-claim-protected-area-player"));
		        });
	            
	            // Update database
	            String banString = String.join(";", claim.getBans());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, banString);
	                    preparedStatement.setString(2, "aucun");
	                    preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, claim.getName());
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
     * @param player the player who owns the claims
     * @param name the name of the member to be banned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAllClaimBan(Player player, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	            String playerName = player.getName();
	            
	            // Add banned and remove member
	            playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> {
	            	claim.addBan(name);
	            	claim.removeMember(name);
	            });
	            
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("banned-all-claim-player").replaceAll("%owner%", playerName));
		        	target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-player").replaceAll("%owner%", playerName));
		        });
	
		        // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ?, Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	String uuid = player.getUniqueId().toString();
	                    for (Claim claim : playerClaims.getOrDefault(playerName, new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getBans()));
	                        preparedStatement.setString(2, String.join(";", claim.getMembers()));
	                        preparedStatement.setString(3, uuid);
	                        preparedStatement.setString(4, playerName);
	                        preparedStatement.setString(5, claim.getName());
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
     * Method to unban a player from all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the player to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAllClaimBan(Player player, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	            String playerName = player.getName();
	            
	            // Remove banned
	            playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> claim.removeBan(name));
	            
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-all-claim-player").replaceAll("%owner%", playerName));
		        });
	            
	            // Updata database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	String uuid = player.getUniqueId().toString();
	                	for (Claim claim : playerClaims.getOrDefault(playerName, new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getBans()));
	                        preparedStatement.setString(2, uuid);
	                        preparedStatement.setString(3, playerName);
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
     * Method to ban a player to all admin claims.
     *
     * @param name the name of the player to be banned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAllAdminClaimBan(String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Add ban and remove member
	        	playerClaims.getOrDefault("admin", new HashSet<>()).parallelStream().forEach(claim -> {
	        		claim.addBan(name);
	        		claim.removeMember(name);
	        	});
	        	
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("banned-all-claim-protected-area-player"));
		        	target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-protected-area-player"));
		        });
	        	
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ?, Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	for (Claim claim : playerClaims.getOrDefault("admin", new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getBans()));
	                        preparedStatement.setString(2, String.join(";", claim.getMembers()));
	                        preparedStatement.setString(3, "aucun");
	                        preparedStatement.setString(4, "admin");
	                        preparedStatement.setString(5, claim.getName());
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
     * Method to unban a member from all admin claims.
     *
     * @param name the name of the member to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAllAdminClaimBan(String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Remove banned player
	        	playerClaims.getOrDefault("admin", new HashSet<>()).parallelStream().forEach(claim -> claim.removeBan(name));
	        	
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> {
		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-all-claim-protected-area-player"));
		        });
	
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	for (Claim claim : playerClaims.getOrDefault("admin", new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getBans()));
	                        preparedStatement.setString(2, "aucun");
	                        preparedStatement.setString(3, "admin");
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
     * Method to add a member to a player's claim.
     *
     * @param player the player who owns the claim
     * @param claim the claim
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addClaimMembers(Player player, Claim claim, String name) {
    	return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Add member
	            claim.addMember(name);
	            
	            // Notify him if online
	            Player target = Bukkit.getPlayer(name);
	            if (target != null) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("add-claim-player").replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", player.getName())));
	            
	            // Update database
	            String membersString = String.join(";", claim.getMembers());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, membersString);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
	                    preparedStatement.setString(4, claim.getName());
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
     * Method to add a member to all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAllClaimMembers(Player player, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	            String playerName = player.getName();
	            
	            // Add member
	            playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> claim.addMember(name));
	
	            // Notify the new member
	            Player target = Bukkit.getPlayer(name);
	            if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("add-all-claim-player").replaceAll("%owner%", playerName)));
	
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	String uuid = player.getUniqueId().toString();
	                	for (Claim claim : playerClaims.getOrDefault(playerName, new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getMembers()));
	                        preparedStatement.setString(2, uuid);
	                        preparedStatement.setString(3, playerName);
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
     * Method to add a member to an admin claim.
     *
     * @param claim the claim representing the admin claim
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAdminClaimMembers(Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Add member
	            claim.addMember(name);
	            
	            // Notify him if online
	            Player target = Bukkit.getPlayer(name);
	            if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("add-claim-protected-area-player").replaceAll("%claim-name%", claim.getName())));
	            
	            // Update database
	            String membersString = String.join(";", claim.getMembers());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, membersString);
	                    preparedStatement.setString(2, "aucun");
	                    preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, claim.getName());
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
     * Method to add a member to all admin claims.
     *
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> addAllAdminClaimMembers(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Remove member
	        	playerClaims.getOrDefault("admin", new HashSet<>()).parallelStream().forEach(claim -> claim.addMember(name));
	
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("add-all-claim-protected-area-player")));
		            
		        // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    for (Claim claim : playerClaims.getOrDefault("admin", new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getMembers()));
	                        preparedStatement.setString(2, "aucun");
	                        preparedStatement.setString(3, "admin");
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
     * Method to remove a member from a player's claim.
     *
     * @param player the player who owns the claim
     * @param claim the claim
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeClaimMembers(Player player, Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Add member
	            claim.removeMember(name);
	            
	            // Notify him if online
	            Player target = Bukkit.getPlayer(name);
	            if (target != null) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", player.getName())));
	            
	            // Update database
	            String membersString = String.join(";", claim.getMembers());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, membersString);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
	                    preparedStatement.setString(4, claim.getName());
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
     * Method to remove a member from a protected area.
     *
     * @param player the player who owns the claim
     * @param claim the claim
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAdminClaimMembers(Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Add member
	            claim.removeMember(name);
	            
	            // Notify him if online
	            Player target = Bukkit.getPlayer(name);
	            if (target != null) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-claim-protected-area-player").replaceAll("%claim-name%", claim.getName())));
	            
	            // Update database
	            String membersString = String.join(";", claim.getMembers());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, membersString);
	                    preparedStatement.setString(2, "");
	                    preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, claim.getName());
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
     * Method to remove a member from all admin claims.
     *
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAllAdminClaimMembers(String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Remove member
	        	playerClaims.getOrDefault("admin", new HashSet<>()).parallelStream().forEach(claim -> claim.removeMember(name));
	        	
	        	// Notify him if online
		        Player target = Bukkit.getPlayer(name);
		        if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-protected-area-player")));
	            
		        // Updata database
		        try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	for (Claim claim : playerClaims.getOrDefault("admin", new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getMembers()));
	                        preparedStatement.setString(2, "aucun");
	                        preparedStatement.setString(3, "admin");
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
     * Method to remove a member from all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> removeAllClaimMembers(Player player, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	            String playerName = player.getName();
	            
	            // Remove the member
	            playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> claim.removeMember(name));
	
	            // Notify him if online
	            Player target = Bukkit.getPlayer(name);
	            if (target != null && target.isOnline()) instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-player").replaceAll("%owner%", playerName)));
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	String uuid = player.getUniqueId().toString();
	                	for (Claim claim : playerClaims.getOrDefault(playerName, new HashSet<>())) {
	                        preparedStatement.setString(1, String.join(";", claim.getMembers()));
	                        preparedStatement.setString(2, uuid);
	                        preparedStatement.setString(3, playerName);
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
     * Method to change a player's claim's name.
     *
     * @param player the player who owns the claim
     * @param claim the claim
     * @param name the new name for the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setClaimName(Player player, Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data and update name
	            String old_name = claim.getName();
	            claim.setName(name);
	            
	            // Update name on bossbars and maps
	        	Set<Chunk> chunks = claim.getChunks();
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(chunks,claim);
	        	
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, name);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
	                    preparedStatement.setString(4, old_name);
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
     * Method to change an admin's claim's name.
     *
     * @param claim the claim representing the admin claim
     * @param name the new name for the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setAdminClaimName(Claim claim, String name) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data and update name
	            String old_name = claim.getName();
	            claim.setName(name);
	            
	            // Update name on bossbars and maps
	        	Set<Chunk> chunks = claim.getChunks();
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(chunks,claim);
	        	
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, name);
	                    preparedStatement.setString(2, "aucun");
	                    preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, old_name);
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
     * @param player the player who owns the claim
     * @param claim the claim representing the claim
     * @param loc the new location for the claim's spawn
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setClaimLocation(Player player, Claim claim, Location loc) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	claim.setLocation(loc);
	            String loc_string = String.valueOf(loc.getX()) + ";" + String.valueOf(loc.getY()) + ";" + String.valueOf(loc.getZ()) + ";" + String.valueOf(loc.getYaw()) + ";" + String.valueOf(loc.getPitch());
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Location = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, loc_string);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
	                    preparedStatement.setString(4, claim.getName());
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
     * @param player the player who owns the claim
     * @param claim the claim representing the claim to be deleted
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> deleteClaim(Player player, Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	            String owner = claim.getOwner();
	            
	        	// Delete all chunks and deactivate bossbars
	        	Set<Chunk> chunks = claim.getChunks();
	        	instance.executeSync(() -> instance.getBossBars().deactivateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
	        	chunks.parallelStream().forEach(c -> listClaims.remove(c));
	        	
	        	// Update owner claims count and remove the claim from his claims
	            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(owner);
	            if (cPlayer != null) cPlayer.setClaimsCount(cPlayer.getClaimsCount() - 1);
	            playerClaims.get(owner).remove(claim);
	            if (playerClaims.get(owner).isEmpty()) playerClaims.remove(owner);
	            
	            // Get the uuid of the owner
	            String uuid = player.getUniqueId().toString();
	            if (owner.equals("admin")) uuid = "aucun";
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid);
	                    preparedStatement.setString(2, owner);
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
     * Method to delete all player's claims.
     *
     * @param player the player whose claims will be deleted
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> deleteAllClaim(Player player) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	            String playerName = player.getName();
	            String uuid = player.getUniqueId().toString();
	            
	        	// Delete all claims of target player, and remove him from data
	        	int[] i = {0};
	        	playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> {
	            	Set<Chunk> chunks = claim.getChunks();
	                instance.executeSync(() -> instance.getBossBars().deactivateBossBar(chunks));
	            	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
	            	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
	            	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
	            	chunks.parallelStream().forEach(c -> listClaims.remove(c));
	            	i[0]++;
	        	});
	            playerClaims.remove(playerName);
	            
	            // Send the delete message
	            instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-all-claims-success").replaceAll("%number%", AdminGestionMainGui.getNumberSeparate(String.valueOf(i[0])))));
	            
	            // Update the claims count of target player
	            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
	            if(cPlayer != null) cPlayer.setClaimsCount(0);
	            
	            // Updata database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid);
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
     * Method to delete all player's claims.
     *
     * @param playerName the name of the player whose claims will be deleted
     * @return CompletableFuture<Boolean> indicating success or failure
     */
    public CompletableFuture<Boolean> deleteAllClaim(String playerName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Delete all claims of target player, and remove him from data
                playerClaims.getOrDefault(playerName, new HashSet<>()).parallelStream().forEach(claim -> {
                    Set<Chunk> chunks = claim.getChunks();
                    instance.executeSync(() -> instance.getBossBars().deactivateBossBar(chunks));
                    if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
                    if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
                    if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
                    chunks.parallelStream().forEach(c -> listClaims.remove(c));
                });
                playerClaims.remove(playerName);

                // Get the uuid of target player
                Player player = Bukkit.getPlayer(playerName);
                String uuid = "";
                if (player == null) {
                    uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
                } else {
                    uuid = player.getUniqueId().toString();
                }

                // Update the claims count of target player
                CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
                if (cPlayer != null) cPlayer.setClaimsCount(0);

                // Update database
                try (Connection connection = instance.getDataSource().getConnection()) {
                    String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                        preparedStatement.setString(1, uuid);
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
     * Method to delete all protected areas.
     *
     * @return CompletableFuture<Boolean> indicating success or failure
     */
    public CompletableFuture<Boolean> deleteAllAdminClaim() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                // Delete all claims of target player, and remove him from data
                playerClaims.getOrDefault("admin", new HashSet<>()).parallelStream().forEach(claim -> {
                    Set<Chunk> chunks = claim.getChunks();
                    instance.executeSync(() -> instance.getBossBars().deactivateBossBar(chunks));
                    if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
                    if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
                    if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
                    chunks.parallelStream().forEach(c -> listClaims.remove(c));
                });
                playerClaims.remove("admin");

                // Update database
                try (Connection connection = instance.getDataSource().getConnection()) {
                    String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                        preparedStatement.setString(1, "aucun");
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
     * Method to force the deletion of a claim.
     *
     * @param claim the claim to be deleted
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> forceDeleteClaim(Claim claim) {

    	Set<Chunk> chunks = claim.getChunks();
        instance.getBossBars().deactivateBossBar(chunks);

        return CompletableFuture.supplyAsync(() -> {
            try {
	    		// Update claims list and maps
	    		chunks.parallelStream().forEach(c -> listClaims.remove(c));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(chunks);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(chunks);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(chunks);
	        	
	        	// Update owner claims count and remove the claim from his claims
	            String owner = claim.getOwner();
	            playerClaims.get(owner).remove(claim);
	            if (playerClaims.get(owner).isEmpty()) playerClaims.remove(owner);
	            
	            // Get uuid of the owner
	            String uuid = "";
	            if(owner.equals("admin")) {
	            	uuid = "aucun";
	            } else {
		            Player player = Bukkit.getPlayer(owner);
		            if (player == null) {
		                uuid = Bukkit.getOfflinePlayer(owner).getUniqueId().toString();
		            } else {
		                uuid = player.getUniqueId().toString();
		            }
		            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(owner);
		            if (cPlayer != null) cPlayer.setClaimsCount(cPlayer.getClaimsCount() - 1);
	            }
	
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid);
	                    preparedStatement.setString(2, owner);
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
     * Method to change player's claim's description.
     *
     * @param player the player who owns the claim
     * @param claim the claim
     * @param description the new description for the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setChunkDescription(Player player, Claim claim, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	claim.setDescription(description);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, description);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
	                    preparedStatement.setString(4, claim.getName());
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
     * Method to change admin claim's description.
     *
     * @param claim the claim representing the admin claim
     * @param description the new description for the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setAdminChunkDescription(Claim claim, String description) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	claim.setDescription(description);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, description);
	                    preparedStatement.setString(2, "aucun");
	                    preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, claim.getName());
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
     * @param player the player who owns the claim
     * @param claim the claim
     * @param price the sale price of the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> setChunkSale(Player player, Claim claim, double price) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            claim.setSale(true);
	            claim.setPrice(price);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET isSale = true, SalePrice = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, String.valueOf(price));
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
	                    preparedStatement.setString(4, claim.getName());
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
     * @param player the player who owns the claim
     * @param claim the claim
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> delChunkSale(Player player, Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            claim.setSale(false);
	            claim.setPrice(0.0);
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, player.getUniqueId().toString());
	                    preparedStatement.setString(2, player.getName());
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
    public CompletableFuture<Boolean> resetAllClaimsSettings() {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	String defaultValue = instance.getSettings().getDefaultValuesCode();
	        	LinkedHashMap<String,Boolean> perm = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
	            listClaims.values().parallelStream().forEach(c -> {
	                if (!"admin".equals(c.getOwner())) {
	                    c.setPermissions(perm);
	                }
	            });
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid <> ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, defaultValue);
	                    preparedStatement.setString(2, "aucun");
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
     * Method to reset all admin claims settings
     * 
     * @return true if the operation was successful, false otherwise
     */
    public CompletableFuture<Boolean> resetAllAdminClaimsSettings() {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	String defaultValue = instance.getSettings().getDefaultValuesCode();
	        	LinkedHashMap<String,Boolean> perm = new LinkedHashMap<>(instance.getSettings().getDefaultValues());
	            listClaims.values().parallelStream().forEach(c -> {
	                if ("admin".equals(c.getOwner())) {
	                    c.setPermissions(perm);
	                }
	            });
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, defaultValue);
	                    preparedStatement.setString(2, "aucun");
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
     * @param player the player buying the claim
     * @param chunk the chunk representing the claim
     */
    public CompletableFuture<Boolean> sellChunk(Player player, Claim claim) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String old_name = claim.getName();
	            String playerName = player.getName();
	            String owner = claim.getOwner();
	            String claimName = claim.getName();
	            double price = claim.getPrice();
	            double balance = instance.getVault().getPlayerBalance(playerName);
	            
	            // Money checking and transfer
	            if (balance < price) {
	            	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money")));
	                return false;
	            }
	            instance.getVault().addPlayerBalance(owner, price);
	            instance.getVault().removePlayerBalance(playerName, price);
	            
	            // Send the buy message
	        	instance.executeEntitySync(player, () -> {
	                player.sendMessage(instance.getLanguage().getMessage("buy-claim-success").replaceAll("%name%", claimName).replaceAll("%price%", String.valueOf(price)).replaceAll("%owner%", owner).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
	                player.closeInventory();
	            });
	
	            // Set uuid of the old owner, and update his claims count and send the buy message if online
	            String uuid = "";
	            Player ownerP = Bukkit.getPlayer(owner);
	            if (ownerP == null) {
	                OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
	                uuid = ownerOP.getUniqueId().toString();
	            } else {
	                CPlayer cOwner = instance.getPlayerMain().getCPlayer(owner);
	                cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
	                uuid = ownerP.getUniqueId().toString();
	            	instance.executeEntitySync(ownerP, () -> ownerP.sendMessage(instance.getLanguage().getMessage("claim-was-sold").replaceAll("%name%", claimName).replaceAll("%buyer%", playerName).replaceAll("%price%", String.valueOf(price)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
	            }
	            
	            // Update new owner claims count, and set the new owner to him
	            CPlayer cTarget = instance.getPlayerMain().getCPlayer(playerName);
	            cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
	            claim.setOwner(playerName);
	            
	            // Delete old owner claim
	            playerClaims.get(owner).remove(claim);
	            if (playerClaims.get(owner).isEmpty()) playerClaims.remove(owner);
	            
	            // Set the new name of the bought claim
	            int id = findFreeId(playerName);
	            String new_name = "bought-claim-" + String.valueOf(id);
	            claim.setName(new_name);
	            
	            // Add the new owner to members if not member, and remove the old owner
	            Set<String> members = new HashSet<>(claim.getMembers());
	            if (!members.contains(playerName)) {
	                members.add(playerName);
	            }
	            members.remove(owner);
	            claim.setMembers(members);
	            String members_string = String.join(";", members);
	            
	            // Delete the sale and set the price to 0.0
	            claim.setSale(false);
	            claim.setPrice(0.0);
	            
	            // Add the claim to the new owner
	            playerClaims.getOrDefault(playerName, new HashSet<>()).add(claim);
	            
	            // Update the bossbars, and maps
	        	Set<Chunk> chunks = claim.getChunks();
	            instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(chunks,claim);
	            
	        	// Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	preparedStatement.setInt(1, id);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, playerName);
	                    preparedStatement.setString(4, members_string);
	                    preparedStatement.setString(5, new_name);
	                    preparedStatement.setString(6, uuid);
	                    preparedStatement.setString(7, owner);
	                    preparedStatement.setString(8, old_name);
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
    public CompletableFuture<Boolean> setOwner(Player sender, String playerName, Claim claim, boolean msg) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	        	// Get data
	        	String old_name = claim.getName();
	            String owner = claim.getOwner();
	            
	            // Set uuid of the old owner, and update his claims count if online
	            String uuid = "";
	            Player ownerP = Bukkit.getPlayer(owner);
	            if (ownerP == null) {
	                OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
	                uuid = ownerOP.getUniqueId().toString();
	            } else {
	                CPlayer cOwner = instance.getPlayerMain().getCPlayer(owner);
	                cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
	                uuid = ownerP.getUniqueId().toString();
	            }
	            
	        	// Send new owner message to sender
	            if (msg) instance.executeEntitySync(sender, () -> sender.sendMessage(instance.getLanguage().getMessage("setowner-success").replaceAll("%owner%", playerName)));
	            
	            // Update the claims count of new owner if online, and set the new owner to him
	            Player player = Bukkit.getPlayer(playerName);
	            if (player != null && player.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(playerName);
	                cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
	            }
	            claim.setOwner(playerName);
	            
	            // Set the new name of the bought claim
	            int id = findFreeId(playerName);
	            String new_name = "claim-" + String.valueOf(id);
	            claim.setName(new_name);
	            
	            // Delete old owner claim
	            playerClaims.get(owner).remove(claim);
	            if (playerClaims.get(owner).isEmpty()) playerClaims.remove(owner);
	            
	            // Add the new owner to members if not member, and remove the old owner
	            Set<String> members = new HashSet<>(claim.getMembers());
	            if (!members.contains(playerName)) {
	                members.add(playerName);
	            }
	            members.remove(owner);
	            claim.setMembers(members);
	            String members_string = String.join(";", members);
	            
	            // Add the claim to the new owner
	            playerClaims.getOrDefault(playerName, new HashSet<>()).add(claim);
	            
	            // Update the bossbars, and maps
	        	Set<Chunk> chunks = claim.getChunks();
	        	instance.executeSync(() -> instance.getBossBars().activateBossBar(chunks));
	        	if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(chunks,claim);
	        	if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(chunks,claim);
	            
	            // Updata database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                	preparedStatement.setInt(1, id);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, playerName);
	                    preparedStatement.setString(4, members_string);
	                    preparedStatement.setString(5, new_name);
	                    preparedStatement.setString(6, uuid);
	                    preparedStatement.setString(7, owner);
	                    preparedStatement.setString(8, old_name);
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
     * Remove a chunk from a claim
     * 
     * @param claim The target claim
     * @param chunk_default The target chunk in string format (world;x;z)
     * @return true if the merge process was initiated successfully
     */
    public CompletableFuture<Boolean> removeChunk(Claim claim, String chunk_default){
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
                    	
                    	instance.info("oui "+String.valueOf(chunk.getX())+" " + String.valueOf(chunk.getZ()));
                    	
                    	// Remove bossbar and maps
                        if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().deleteMarker(Set.of(chunk));
                        if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().deleteMarker(Set.of(chunk));
                        if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().deleteMarker(Set.of(chunk));
                    	instance.executeSync(() -> instance.getBossBars().deactivateBossBar(Set.of(chunk)));
                    	
        	            List<Integer> X = new ArrayList<>();
        	            List<Integer> Z = new ArrayList<>();
        	            
        	            // Collect chunks from claim1
        	            chunks.parallelStream().forEach(c -> {
        	                X.add(c.getX());
        	                Z.add(c.getZ());
        	            });
        	            
        	            // Build X and Z strings
        	            StringBuilder sbX = new StringBuilder();
        	            for (Integer x : X) {
        	                sbX.append(x).append(";");
        	            }
        	            if (sbX.length() > 0) {
        	                sbX.setLength(sbX.length() - 1);
        	            }
        	            
        	            StringBuilder sbZ = new StringBuilder();
        	            for (Integer z : Z) {
        	                sbZ.append(z).append(";");
        	            }
        	            if (sbZ.length() > 0) {
        	                sbZ.setLength(sbZ.length() - 1);
        	            }
        	            
        	            // Get uuid of the owner
        	            String uuid = "";
        	            String owner = claim.getOwner();
        	            if(owner.equals("admin")) {
        	            	uuid = "aucun";
        	            } else {
        		            Player player = Bukkit.getPlayer(owner);
        		            if (player == null) {
        		                uuid = Bukkit.getOfflinePlayer(owner).getUniqueId().toString();
        		            } else {
        		                uuid = player.getUniqueId().toString();
        		            }
        	            }
        	            
        	            // Update database
        	            try (Connection connection = instance.getDataSource().getConnection()) {
        	                String updateQuery = "UPDATE scs_claims SET X = ?, Z = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
        	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
        	                    preparedStatement.setString(1, sbX.toString());
        	                    preparedStatement.setString(2, sbZ.toString());
        	                    preparedStatement.setString(3, uuid);
        	                    preparedStatement.setString(4, owner);
        	                    preparedStatement.setString(5, claim.getName());
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
                	
    	            List<Integer> X = new ArrayList<>();
    	            List<Integer> Z = new ArrayList<>();
    	            
    	            // Collect chunks from claim1
    	            chunks.parallelStream().forEach(c -> {
    	                X.add(c.getX());
    	                Z.add(c.getZ());
    	            });
    	            
    	            // Build X and Z strings
    	            StringBuilder sbX = new StringBuilder();
    	            for (Integer x : X) {
    	                sbX.append(x).append(";");
    	            }
    	            if (sbX.length() > 0) {
    	                sbX.setLength(sbX.length() - 1);
    	            }
    	            
    	            StringBuilder sbZ = new StringBuilder();
    	            for (Integer z : Z) {
    	                sbZ.append(z).append(";");
    	            }
    	            if (sbZ.length() > 0) {
    	                sbZ.setLength(sbZ.length() - 1);
    	            }
    	            
    	            // Get uuid of the owner
    	            String uuid = "";
    	            String owner = claim.getOwner();
    	            if(owner.equals("admin")) {
    	            	uuid = "aucun";
    	            } else {
    		            Player player = Bukkit.getPlayer(owner);
    		            if (player == null) {
    		                uuid = Bukkit.getOfflinePlayer(owner).getUniqueId().toString();
    		            } else {
    		                uuid = player.getUniqueId().toString();
    		            }
    	            }
    	            
    	            // Update database
    	            try (Connection connection = instance.getDataSource().getConnection()) {
    	                String updateQuery = "UPDATE scs_claims SET X = ?, Z = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
    	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	                    preparedStatement.setString(1, sbX.toString());
    	                    preparedStatement.setString(2, sbZ.toString());
    	                    preparedStatement.setString(3, uuid);
    	                    preparedStatement.setString(4, owner);
    	                    preparedStatement.setString(5, claim.getName());
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
    public CompletableFuture<Boolean> addChunk(Claim claim, Chunk chunk){
        return CompletableFuture.supplyAsync(() -> {
            try {
    			// Add chunk
            	Set<Chunk> chunks = new HashSet<>(claim.getChunks());
            	if(chunks.contains(chunk)) return false;
            	chunks.add(chunk);
            	claim.setChunks(chunks);
            	listClaims.put(chunk,claim);
            	
            	// Add bossbar and maps
                if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().createChunkZone(Set.of(chunk), claim.getName(), claim.getOwner());
                if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().createChunkZone(Set.of(chunk), claim.getName(), claim.getOwner());
                if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().createChunkZone(Set.of(chunk), claim.getName(), claim.getOwner());
            	instance.executeSync(() -> instance.getBossBars().activateBossBar(Set.of(chunk)));
            	
	            List<Integer> X = new ArrayList<>();
	            List<Integer> Z = new ArrayList<>();
	            
	            // Collect chunks from claim1
	            chunks.parallelStream().forEach(c -> {
	                X.add(c.getX());
	                Z.add(c.getZ());
	            });
	            
	            // Build X and Z strings
	            StringBuilder sbX = new StringBuilder();
	            for (Integer x : X) {
	                sbX.append(x).append(";");
	            }
	            if (sbX.length() > 0) {
	                sbX.setLength(sbX.length() - 1);
	            }
	            
	            StringBuilder sbZ = new StringBuilder();
	            for (Integer z : Z) {
	                sbZ.append(z).append(";");
	            }
	            if (sbZ.length() > 0) {
	                sbZ.setLength(sbZ.length() - 1);
	            }
	            
	            // Get uuid of the owner
	            String uuid = "";
	            String owner = claim.getOwner();
	            if(owner.equals("admin")) {
	            	uuid = "aucun";
	            } else {
		            Player player = Bukkit.getPlayer(owner);
		            if (player == null) {
		                uuid = Bukkit.getOfflinePlayer(owner).getUniqueId().toString();
		            } else {
		                uuid = player.getUniqueId().toString();
		            }
	            }
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET X = ?, Z = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, sbX.toString());
	                    preparedStatement.setString(2, sbZ.toString());
	                    preparedStatement.setString(3, uuid);
	                    preparedStatement.setString(4, owner);
	                    preparedStatement.setString(5, claim.getName());
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
     * Merges two claims into one.
     *
     * This method combines the chunks of `claim2` into `claim1` and updates the database accordingly.
     * The merged claim will have all chunks from both claims, and `claim2` will be removed.
     * This method runs asynchronously.
     *
     * @param player the player initiating the merge
     * @param claim1 the primary claim to merge into
     * @param claim2 the secondary claim to be merged
     * @return true if the merge process was initiated successfully
     */
    public CompletableFuture<Boolean> mergeClaims(Player player, Claim claim1, Set<Claim> claims) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            String playerName = player.getName();
	            List<Integer> X = new ArrayList<>();
	            List<Integer> Z = new ArrayList<>();
	            
	            // Collect chunks from claim1
	            claim1.getChunks().parallelStream().forEach(c -> {
	                X.add(c.getX());
	                Z.add(c.getZ());
	            });
	            
	            // Collect chunks from claims and update listClaims map and add new chunks
	            claims.parallelStream().forEach(claim -> {
	            	claim1.addChunks(claim.getChunks());
	            	instance.executeSync(() -> instance.getBossBars().activateBossBar(claim.getChunks()));
	            	Set<Chunk> chunks = claim.getChunks();
	            	chunks.parallelStream().forEach(c -> {
	                    X.add(c.getX());
	                    Z.add(c.getZ());
	                    listClaims.put(c, claim1);
	                });
	                if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(chunks,claim1);
	                if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(chunks,claim1);
	                if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(chunks,claim1);
	            });
	            
	            // Remove 1 claim from player claims count
	            CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
	            cPlayer.setClaimsCount(cPlayer.getClaimsCount()-claims.size());
	            
	            // Remove claim2 from player's claims
	            playerClaims.getOrDefault(playerName, new HashSet<>()).removeAll(claims);
	            
	            // Build X and Z strings
	            StringBuilder sbX = new StringBuilder();
	            for (Integer x : X) {
	                sbX.append(x).append(";");
	            }
	            if (sbX.length() > 0) {
	                sbX.setLength(sbX.length() - 1);
	            }
	            
	            StringBuilder sbZ = new StringBuilder();
	            for (Integer z : Z) {
	                sbZ.append(z).append(";");
	            }
	            if (sbZ.length() > 0) {
	                sbZ.setLength(sbZ.length() - 1);
	            }
	            
	            // Display new chunks borders
	            if (instance.getSettings().getBooleanSetting("claim-particles")) instance.executeSync(() -> displayChunks(player, claim1.getChunks(), true, false));
	            
	            // Update database
	            String uuid = player.getUniqueId().toString();
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET X = ?, Z = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, sbX.toString());
	                    preparedStatement.setString(2, sbZ.toString());
	                    preparedStatement.setString(3, uuid);
	                    preparedStatement.setString(4, playerName);
	                    preparedStatement.setString(5, claim1.getName());
	                    preparedStatement.executeUpdate();
	                }
	                String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                	for(Claim claim : claims) {
	                        preparedStatement.setString(1, uuid);
	                        preparedStatement.setString(2, playerName);
	                        preparedStatement.setString(3, claim.getName());
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
     * Merges two claims into one (protected areas).
     *
     * This method combines the chunks of `claim2` into `claim1` and updates the database accordingly.
     * The merged claim will have all chunks from both claims, and `claim2` will be removed.
     * This method runs asynchronously.
     *
     * @param player the player initiating the merge
     * @param claim1 the primary claim to merge into
     * @param claim2 the secondary claim to be merged
     * @return true if the merge process was initiated successfully
     */
    public CompletableFuture<Boolean> mergeClaimsProtectedArea(Player player, Claim claim1, Set<Claim> claims) {
        return CompletableFuture.supplyAsync(() -> {
            try {
	            List<Integer> X = new ArrayList<>();
	            List<Integer> Z = new ArrayList<>();
	            
	            // Collect chunks from claim1
	            claim1.getChunks().parallelStream().forEach(c -> {
	                X.add(c.getX());
	                Z.add(c.getZ());
	            });
	            
	            // Collect chunks from claims and update listClaims map and add new chunks
	            claims.parallelStream().forEach(claim -> {
	            	claim1.addChunks(claim.getChunks());
	            	instance.executeSync(() -> instance.getBossBars().activateBossBar(claim.getChunks()));
	            	Set<Chunk> chunks = claim.getChunks();
	            	chunks.parallelStream().forEach(c -> {
	                    X.add(c.getX());
	                    Z.add(c.getZ());
	                    listClaims.put(c, claim1);
	                });
	                if (instance.getSettings().getBooleanSetting("dynmap")) instance.getDynmap().updateName(chunks,claim1);
	                if (instance.getSettings().getBooleanSetting("bluemap")) instance.getBluemap().updateName(chunks,claim1);
	                if (instance.getSettings().getBooleanSetting("pl3xmap")) instance.getPl3xMap().updateName(chunks,claim1);
	            });
	            
	            // Remove claim2 from player's claims
	            playerClaims.getOrDefault("admin", new HashSet<>()).removeAll(claims);
	            
	            // Build X and Z strings
	            StringBuilder sbX = new StringBuilder();
	            for (Integer x : X) {
	                sbX.append(x).append(";");
	            }
	            if (sbX.length() > 0) {
	                sbX.setLength(sbX.length() - 1);
	            }
	            
	            StringBuilder sbZ = new StringBuilder();
	            for (Integer z : Z) {
	                sbZ.append(z).append(";");
	            }
	            if (sbZ.length() > 0) {
	                sbZ.setLength(sbZ.length() - 1);
	            }
	            
	            // Display new chunks borders
	            if (instance.getSettings().getBooleanSetting("claim-particles")) instance.executeSync(() -> displayChunks(player, claim1.getChunks(), true, false));
	            
	            // Update database
	            try (Connection connection = instance.getDataSource().getConnection()) {
	                String updateQuery = "UPDATE scs_claims SET X = ?, Z = ? WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, sbX.toString());
	                    preparedStatement.setString(2, sbZ.toString());
	                    preparedStatement.setString(3, "aucun");
	                    preparedStatement.setString(4, "admin");
	                    preparedStatement.setString(5, claim1.getName());
	                    preparedStatement.executeUpdate();
	                }
	                String deleteQuery = "DELETE FROM scs_claims WHERE uuid = ? AND name = ? AND claim_name = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                	for(Claim claim : claims) {
	                        preparedStatement.setString(1, "aucun");
	                        preparedStatement.setString(2, "admin");
	                        preparedStatement.setString(3, claim.getName());
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
	            Bukkit.getAsyncScheduler().runAtFixedRate(instance.getPlugin(), task -> {
	                if (counter[0] >= 10) {
	                    task.cancel();
	                }
	                World world = player.getWorld();
	                particleLocations.parallelStream().forEach(location -> world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0, dustOptions));
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
	                    particleLocations.parallelStream().forEach(location -> world.spawnParticle(Particle.REDSTONE, location, 1, 0, 0, 0, 0, dustOptions));
	                    counter++;
	                }
	            }.runTaskTimerAsynchronously(instance.getPlugin(), 0, 10L);
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
            Bukkit.getAsyncScheduler().runAtFixedRate(instance.getPlugin(), task -> {
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
        }.runTaskTimerAsynchronously(instance.getPlugin(), 0, 10L);
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
            	player.sendMessage(instance.getLanguage().getMessage("available-args").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%args%", String.join(", ", commandArgsClaim)));
            } else if (cmd.equalsIgnoreCase("scs")) {
            	player.sendMessage(instance.getLanguage().getMessage("available-args").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%args%", String.join(", ", commandArgsScs)));
            } else if (cmd.equalsIgnoreCase("parea") || cmd.equalsIgnoreCase("protectedarea")) {
            	player.sendMessage(instance.getLanguage().getMessage("available-args").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%args%", String.join(", ", commandArgsParea)));
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
        	player.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgsClaim)));
        } else if (cmd.equalsIgnoreCase("scs")) {
        	player.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgsScs)));
        } else if (cmd.equalsIgnoreCase("parea") || cmd.equalsIgnoreCase("protectedarea")) {
        	player.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgsParea)));
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
            	sender.sendMessage(instance.getLanguage().getMessage("available-args").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%args%", String.join(", ", commandArgsClaim)));
            } else if (cmd.equalsIgnoreCase("scs")) {
            	sender.sendMessage(instance.getLanguage().getMessage("available-args").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%args%", String.join(", ", commandArgsScs)));
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
        	sender.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgsClaim)));
        } else if (cmd.equalsIgnoreCase("scs")) {
        	sender.sendMessage(instance.getLanguage().getMessage("sub-arg-not-found").replaceAll("%help-separator%", instance.getLanguage().getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgsScs)));
        }
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
                ? instance.getLanguage().getMessage("map-actual-claim-name-message").replaceAll("%name%", getClaimNameByChunk(centerChunk)) 
                : instance.getLanguage().getMessage("map-no-claim-name-message");
            String coords = instance.getLanguage().getMessage("map-coords-message").replaceAll("%coords%", centerX + "," + centerZ).replaceAll("%direction%", direction);
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
            legendMap.put(-3, "  " + name + (isClaimed ? " " + instance.getLanguage().getMessage("map-actual-claim-name-message-owner").replaceAll("%owner%", listClaims.get(centerChunk).getOwner()) : ""));
            legendMap.put(-2, "  " + coords);
            legendMap.put(0, "  " + instance.getLanguage().getMessage("map-legend-you").replaceAll("%cursor-color%", colorCursor));
            legendMap.put(1, "  " + instance.getLanguage().getMessage("map-legend-free").replaceAll("%no-claim-color%", colorRelationNoClaim));
            legendMap.put(2, "  " + instance.getLanguage().getMessage("map-legend-yours").replaceAll("%claim-relation-member%", instance.getLanguage().getMessage("map-claim-relation-member")));
            legendMap.put(3, "  " + instance.getLanguage().getMessage("map-legend-other").replaceAll("%claim-relation-visitor%", instance.getLanguage().getMessage("map-claim-relation-visitor")));

            if(instance.isFolia()) {
                Bukkit.getRegionScheduler().run(instance.getPlugin(), player.getLocation(), task -> {
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
                	instance.executeSync(() -> player.sendMessage(mapMessage.toString()));
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
     * Method to update the weather in chunks.
     *
     * @param chunks the chunks to be updated
     * @param result the new weather state
     */
    public void updateWeatherChunk(Set<Chunk> chunks, boolean result) {
    	if(result) {
    		Bukkit.getOnlinePlayers().parallelStream().forEach(p -> {
    			Chunk c = p.getLocation().getChunk();
    			if(chunks.contains(c)) {
    				p.resetPlayerWeather();
    			}
    		});
    	} else {
    		Bukkit.getOnlinePlayers().parallelStream().forEach(p -> {
    			Chunk c = p.getLocation().getChunk();
    			if(chunks.contains(c)) {
    				p.setPlayerWeather(WeatherType.CLEAR);
    			}
    		});
    	}
    }

    /**
     * Method to update the fly in the chunk.
     *
     * @param chunks the chunks to be updated
     * @param result the new fly state
     */
    public void updateFlyChunk(Set<Chunk> chunks, boolean result) {
    	if(result) {
        	Bukkit.getOnlinePlayers().parallelStream().forEach(p -> {
    			Chunk c = p.getLocation().getChunk();
    			if(chunks.contains(c)) {
                    CPlayer cPlayer = instance.getPlayerMain().getCPlayer(p.getName());
                    if (cPlayer.getClaimAutofly()) {
                        instance.getPlayerMain().activePlayerFly(p);
                    }
    			}
        	});
    	} else {
        	Bukkit.getOnlinePlayers().parallelStream().forEach(p -> {
    			Chunk c = p.getLocation().getChunk();
    			if(chunks.contains(c)) {
                    CPlayer cPlayer = instance.getPlayerMain().getCPlayer(p.getName());
                    if (cPlayer.getClaimFly()) {
                        instance.getPlayerMain().removePlayerFly(p);
                    }
    			}
        	});
    	}
    }
    
    /**
     * Handles for plugin settings in gui menu
     * 
     * @param player The target player
     * @param value The value of plugin setting
     */
    public void handleAdminGestionPlugin(Player player, String value) {
    	if(!playerAdminSetting.containsKey(player)) return;
    	String setting = playerAdminSetting.get(player);
    	String[] data;
    	playerAdminSetting.remove(player);
        File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		switch(setting) {
			case "group2":
				if(!config.isConfigurationSection("groups."+value)) {
					player.sendMessage(instance.getLanguage().getMessage("group-does-not-exist"));
					return;
				}
				if(value.equalsIgnoreCase("default")) {
					player.sendMessage(instance.getLanguage().getMessage("you-can-not-delete-default-group"));
					return;
				}
                config.set("groups."+value, null);
            	try {
					config.save(configFile);
					player.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replaceAll("%setting%", "Group").replaceAll("%value%", "'"+value+"' deleted"));
					instance.executeSync(() -> {
						if(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scs reload")) {
							new AdminGestionGui(player,instance);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
            	break;
			case "group":
				data = value.split(";");
    			if(!(data.length == 9)) {
    				player.sendMessage(instance.getLanguage().getMessage("group-must-be-nine-settings"));
    				return;
    			}
				for(int i = 2; i < data.length; i++) {
					try {
						int v = Integer.parseInt(data[i]);
	    			} catch (NumberFormatException e) {
	    	        	player.sendMessage(instance.getLanguage().getMessage("group-values-must-be-number"));
	    	        	return;
	    	        }
				}
				String permission = data[1];
				int max_claims = Integer.parseInt(data[2]);
				int max_radius_claim = Integer.parseInt(data[3]);
				int teleportation_delay = Integer.parseInt(data[4]);
				int max_members = Integer.parseInt(data[5]);
				int claim_cost = Integer.parseInt(data[6]);
				int claim_cost_multiplier = Integer.parseInt(data[7]);
				int max_chunks_per_claim = Integer.parseInt(data[8]);
                config.set("groups."+data[0]+".permission", permission);
                config.set("groups."+data[0]+".max-claims", max_claims);
                config.set("groups."+data[0]+".max-radius-claims", max_radius_claim);
                config.set("groups."+data[0]+".teleportation-delay", teleportation_delay);
                config.set("groups."+data[0]+".max-members", max_members);
                config.set("groups."+data[0]+".claim-cost", claim_cost);
                config.set("groups."+data[0]+".claim-cost-multiplier", claim_cost_multiplier);
                config.set("groups."+data[0]+".max-chunks-per-claim", max_chunks_per_claim);
            	try {
					config.save(configFile);
					player.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replaceAll("%setting%", "Group").replaceAll("%value%", data[0]));
					instance.executeSync(() -> {
						if(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scs reload")) {
							new AdminGestionGui(player,instance);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
            	break;
			case "player2":
				if(!config.isConfigurationSection("players."+value)) {
					player.sendMessage(instance.getLanguage().getMessage("player-does-not-exist"));
					return;
				}
                config.set("groups."+value, null);
            	try {
					config.save(configFile);
					player.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replaceAll("%setting%", "Player").replaceAll("%value%", "'"+value+"' deleted"));
					instance.executeSync(() -> {
						if(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scs reload")) {
							new AdminGestionGui(player,instance);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
            	break;
			case "player":
				data = value.split(";");
    			if(!(data.length == 8)) {
    				player.sendMessage(instance.getLanguage().getMessage("player-must-be-eight-settings"));
    				return;
    			}
				for(int i = 1; i < data.length; i++) {
					try {
						int v = Integer.parseInt(data[i]);
	    			} catch (NumberFormatException e) {
	    	        	player.sendMessage(instance.getLanguage().getMessage("player-values-must-be-number"));
	    	        	return;
	    	        }
				}
				max_claims = Integer.parseInt(data[1]);
				max_radius_claim = Integer.parseInt(data[2]);
				teleportation_delay = Integer.parseInt(data[3]);
				max_members = Integer.parseInt(data[4]);
				claim_cost = Integer.parseInt(data[5]);
				claim_cost_multiplier = Integer.parseInt(data[6]);
				max_chunks_per_claim = Integer.parseInt(data[7]);
                config.set("players."+data[0]+".max-claims", max_claims);
                config.set("players."+data[0]+".max-radius-claims", max_radius_claim);
                config.set("players."+data[0]+".teleportation-delay", teleportation_delay);
                config.set("players."+data[0]+".max-members", max_members);
                config.set("players."+data[0]+".claim-cost", claim_cost);
                config.set("players."+data[0]+".claim-cost-multiplier", claim_cost_multiplier);
                config.set("players."+data[0]+".max-chunks-per-claim", max_chunks_per_claim);
            	try {
					config.save(configFile);
					player.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replaceAll("%setting%", "Player").replaceAll("%value%", data[0]));
					instance.executeSync(() -> {
						if(Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "scs reload")) {
							new AdminGestionGui(player,instance);
						}
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
            	break;
    		case "database":
    			data = value.split(";");
    			if(!(data.length == 5)) {
    				player.sendMessage(instance.getLanguage().getMessage("database-must-be-five-settings"));
    				return;
    			}
    			String[] data_final = data;
    			instance.executeAsync(() -> {
    				// Create data source
                    HikariConfig configH = new HikariConfig();
                    configH.setJdbcUrl("jdbc:mysql://" + data_final[0] + ":" + data_final[1] + "/" + data_final[2]);
                    configH.setUsername(data_final[3]);
                    configH.setPassword(data_final[4]);
                    configH.addDataSourceProperty("cachePrepStmts", "true");
                    configH.addDataSourceProperty("prepStmtCacheSize", "250");
                    configH.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                    configH.setPoolName("MySQL");
                    configH.setMaximumPoolSize(10);
                    configH.setMinimumIdle(2);
                    configH.setIdleTimeout(60000);
                    configH.setMaxLifetime(600000);
                    HikariDataSource dataSource = new HikariDataSource(configH);
                    try (Connection connection = dataSource.getConnection()) {
                        config.set("database-settings.hostname", data_final[0]);
                        config.set("database-settings.port", data_final[1]);
                        config.set("database-settings.database_name", data_final[2]);
                    	config.set("database-settings.username", data_final[3]);
                    	config.set("database-settings.password", data_final[4]);
                    	config.set("database", true);
                    	try {
    						config.save(configFile);
    	                	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("database-connection-successful")));
    	                	String AnswerA = instance.getLanguage().getMessage("database-connection-successful-button");
    	                    TextComponent AnswerA_C = new TextComponent(AnswerA);
    	                    AnswerA_C.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(instance.getLanguage().getMessage("database-connection-successful-button")).create()));
    	                    AnswerA_C.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/aclaim reload"));
    	                    instance.executeEntitySync(player, () -> player.sendMessage(AnswerA_C));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
                        return;
                    } catch (SQLException e) {
                    	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("database-connection-error")));
                    	instance.executeSync(() -> new AdminGestionGui(player,instance));
                        return;
                    }
    			});
    			break;
    		case "auto-purge":
    			data = value.split(";");
    			if(!(data.length == 2)) {
    				player.sendMessage(instance.getLanguage().getMessage("auto-purge-must-be-two-settings"));
    				return;
    			}
    			break;
    		case "economy":
    			try {
    				Integer price = Integer.parseInt(value);
    				if(price < 1) {
        	        	player.sendMessage(instance.getLanguage().getMessage("max-sell-price-must-be-positive"));
        	        	return;
    				}
                    config.set("max-sell-price", price);
                	try {
						config.save(configFile);
						instance.getSettings().addSetting("max-sell-price", String.valueOf(price));
						player.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replaceAll("%setting%", "Max-sell-price").replaceAll("%value%", String.valueOf(price)));
						new AdminGestionGui(player,instance);
						return;
					} catch (IOException e) {
						e.printStackTrace();
					}
    			} catch (NumberFormatException e) {
    	        	player.sendMessage(instance.getLanguage().getMessage("max-sell-price-must-be-number"));
    	        	return;
    	        }
    			break;
    		case "dynmap":
    		case "bluemap":
    		case "pl3xmap":
    			data = value.split(";");
    			if(!(data.length == 3)) {
    				player.sendMessage(instance.getLanguage().getMessage("map-must-be-three-settings"));
    				return;
    			}
    			data[2] = data[2].replaceAll("&", "§");
                config.set(setting+"-settings.claim-border-color", data[0]);
                config.set(setting+"-settings.claim-fill-color", data[1]);
                config.set(setting+"-settings.claim-hover-text", data[2]);
            	try {
					config.save(configFile);
					instance.getSettings().addSetting(setting+"-claim-border-color", data[0]);
					instance.getSettings().addSetting(setting+"-claim-fill-color", data[1]);
					instance.getSettings().addSetting(setting+"-claim-hover-text", data[2]);
					player.sendMessage(instance.getLanguage().getMessage("map-new-settings").replaceAll("%settings%",
							"Claim-border-color: " + AdminGestionGui.fromHex(data[0])
							+ "\n§fClaim-fill-color: " + AdminGestionGui.fromHex(data[1])
							+ "\n§fClaim-hover-text: " + data[2]));
					new AdminGestionGui(player,instance);
					return;
				} catch (IOException e) {
					e.printStackTrace();
				}
            	break;
    	}
    }
    
    /**
     * Check if a player is in the map playerAdminSetting
     * 
     * @param player The target player
     */
    public boolean isPlayerAdminSetting(Player player) {
    	return playerAdminSetting.containsKey(player);
    }
    
    /**
     * Adds a player in the map playerAdminSetting
     * 
     * @param player The target player
     * @param value The setting value
     */
    public void addPlayerAdminSetting(Player player, String value) {
    	playerAdminSetting.put(player, value);
    }
}