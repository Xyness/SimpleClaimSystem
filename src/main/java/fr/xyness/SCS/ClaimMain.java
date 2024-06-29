package fr.xyness.SCS;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Listeners.ClaimEventsEnterLeave;
import fr.xyness.SCS.Support.ClaimBluemap;
import fr.xyness.SCS.Support.ClaimDynmap;
import fr.xyness.SCS.Support.ClaimPl3xMap;
import fr.xyness.SCS.Support.ClaimVault;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ClaimMain {

    // ***************
    // *  Variables  *
    // ***************

    /** List of claims by chunk. */
    private static Map<Chunk, Claim> listClaims = new HashMap<>();

    /** Mapping of player names to their claims. */
    private static Map<String, Map<Chunk, String>> claimsId = new HashMap<>();

    /** Mapping of players to their original locations. */
    private final static Map<Player, Location> playerLocations = new HashMap<>();

    /** Mapping of players to their active Bukkit tasks. */
    private static final Map<Player, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    /** Mapping of players to their active Folia tasks. */
    private static final Map<Player, ScheduledTask> activeFoliaTasks = new ConcurrentHashMap<>();

    /** Set of command arguments. */
    private static Set<String> commandArgs = Set.of("add", "autoclaim", "automap", "list",
            "map", "members", "remove", "see", "setdesc", "setname", "setspawn", "settings", "tp", "chat", "ban", "unban", "bans", "owner", "autofly", "fly");

    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Clears all maps and variables.
     */
    public static void clearAll() {
        claimsId.clear();
        playerLocations.clear();
        listClaims.clear();
    }

    /**
     * Sends a message to a player.
     *
     * @param player  the player to send the message to
     * @param message the message to send
     * @param type    the type of message (ACTION_BAR, SUBTITLE, TITLE, CHAT, BOSSBAR)
     */
    public static void sendMessage(Player player, String message, String type) {
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
    public static void sendBossbarMessage(Player player, String message) {
        BossBar b = ClaimEventsEnterLeave.checkBossBar(player);
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
                    b.setColor(BarColor.valueOf(ClaimSettings.getSetting("bossbar-color")));
                    b.setProgress(1);
                    ClaimEventsEnterLeave.activeBossBar(player, player.getLocation().getChunk());
                } else {
                    counter[0]--;
                    b.setProgress(counter[0] / 20.0);
                }
            };

            if (SimpleClaimSystem.isFolia()) {
                if (activeFoliaTasks.containsKey(player)) {
                    activeFoliaTasks.get(player).cancel();
                }
                ScheduledTask task = Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), subtask -> {
                    countdownTask.run();
                    if (!player.isOnline()) {
                        subtask.cancel();
                    }
                    if (counter[0] <= 0) {
                        subtask.cancel();
                        b.setColor(BarColor.valueOf(ClaimSettings.getSetting("bossbar-color")));
                        b.setProgress(1);
                        ClaimEventsEnterLeave.activeBossBar(player, player.getLocation().getChunk());
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
                            b.setColor(BarColor.valueOf(ClaimSettings.getSetting("bossbar-color")));
                            b.setProgress(1);
                            ClaimEventsEnterLeave.activeBossBar(player, player.getLocation().getChunk());
                        }
                    }
                }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 2L);
                activeTasks.put(player, task);
            }
        };
        updateTask.run();
    }

    // ********************
    // *  CLAIMS Methods  *
    // ********************

    /**
     * Gets the claim from the given chunk.
     *
     * @param chunk the chunk to get the claim from
     * @return the claim, or null if no claim exists for the chunk
     */
    public static Claim getClaimFromChunk(Chunk chunk) {
        return listClaims.get(chunk);
    }

    /**
     * Gets the number of claims a player has.
     *
     * @param playerName the name of the player
     * @return the number of claims the player has
     */
    public static int getPlayerClaimsCount(String playerName) {
        return Optional.ofNullable(claimsId.get(playerName)).map(Map::size).orElse(0);
    }

    /**
     * Gets the chunk by the admin claim name.
     *
     * @param name the name of the admin claim
     * @return the chunk, or null if no chunk with the given claim name exists
     */
    public static Chunk getAdminChunkByName(String name) {
        return claimsId.getOrDefault("admin", Collections.emptyMap())
                .keySet()
                .stream()
                .filter(c -> name.equals(listClaims.get(c).getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a free ID for a new claim.
     *
     * @param target the target owner of the claim
     * @return the next available ID
     */
    public static int findFreeId(String target) {
        return claimsId.getOrDefault(target, Collections.emptyMap())
                .values()
                .stream()
                .mapToInt(Integer::parseInt)
                .max()
                .orElse(-1) + 1;
    }

    /**
     * Gets all the claim owners (excluding admin).
     *
     * @return a map of claim owners and their claim counts
     */
    public static Map<String, Integer> getClaimsOwnersGui() {
        return claimsId.entrySet()
                .stream()
                .filter(entry -> !entry.getKey().equals("admin"))
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().size()));
    }

    /**
     * Gets all the claim owners (excluding admin).
     *
     * @return a set of claim owners
     */
    public static Set<String> getClaimsOwners() {
        return claimsId.keySet()
                .stream()
                .filter(key -> !key.equals("admin"))
                .collect(Collectors.toSet());
    }

    /**
     * Gets the chunks owned by the specified owner.
     *
     * @param owner the owner of the chunks
     * @return a set of chunks owned by the owner
     */
    public static Set<Chunk> getChunksFromOwner(String owner) {
        return Optional.ofNullable(claimsId.get(owner))
                .map(Map::keySet)
                .orElseGet(HashSet::new);
    }

    /**
     * Gets the chunks owned by the specified owner for GUIs.
     *
     * @param owner the owner of the chunks
     * @return a map of chunks and their associated claims
     */
    public static Map<Chunk, Claim> getChunksFromOwnerGui(String owner) {
        return Optional.ofNullable(claimsId.get(owner))
                .orElseGet(Collections::emptyMap)
                .keySet()
                .stream()
                .map(chunk -> Map.entry(chunk, listClaims.get(chunk)))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets the chunks in sale owned by the specified owner.
     *
     * @param owner the owner of the chunks
     * @return a map of chunks and their associated claims in sale
     */
    public static Map<Chunk, Claim> getChunksInSaleFromOwner(String owner) {
        return Optional.ofNullable(claimsId.get(owner))
                .orElseGet(Collections::emptyMap)
                .keySet()
                .stream()
                .map(chunk -> Map.entry(chunk, listClaims.get(chunk)))
                .filter(entry -> entry.getValue() != null && entry.getValue().getSale())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets all claimed chunks.
     *
     * @return a set of all claimed chunks
     */
    public static Set<Chunk> getAllClaimsChunk() {
        return listClaims.keySet();
    }

    /**
     * Gets all members of all claims owned by the specified player.
     *
     * @param owner the owner of the claims
     * @return a set of all members of the owner's claims
     */
    public static Set<String> getAllMembersOfAllPlayerClaim(String owner) {
        return listClaims.values().stream()
                .filter(claim -> claim.getOwner().equals(owner))
                .flatMap(claim -> claim.getMembers().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all claimed chunks where the owner is online.
     *
     * @return a set of claimed chunks where the owner is online
     */
    public static Set<Chunk> getAllClaimsChunkOwnerOnline() {
        return listClaims.values().stream()
                .filter(claim -> {
                    Player owner = Bukkit.getPlayer(claim.getOwner());
                    return owner != null && owner.isOnline();
                })
                .map(Claim::getChunk)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all claimed chunks where the owner is offline.
     *
     * @return a set of claimed chunks where the owner is offline
     */
    public static Set<Chunk> getAllClaimsChunkOwnerOffline() {
        return listClaims.values().stream()
                .filter(claim -> Bukkit.getPlayer(claim.getOwner()) == null)
                .map(Claim::getChunk)
                .collect(Collectors.toSet());
    }

    /**
     * Gets all online claim owners and their claim counts.
     *
     * @return a map of online claim owners and their claim counts
     */
    public static Map<String, Integer> getClaimsOnlineOwners() {
        return claimsId.entrySet().stream()
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
     * Gets all offline claim owners and their claim counts.
     *
     * @return a map of offline claim owners and their claim counts
     */
    public static Map<String, Integer> getClaimsOfflineOwners() {
        return claimsId.entrySet().stream()
                .filter(entry -> Bukkit.getPlayer(entry.getKey()) == null)
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
    public static Map<String, Integer> getClaimsOwnersWithSales() {
        return claimsId.entrySet().stream()
                .filter(entry -> !entry.getKey().equals("admin"))
                .filter(entry -> entry.getValue().keySet().stream().anyMatch(chunk -> listClaims.get(chunk).getSale()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()
                ));
    }

    /**
     * Gets the chunk associated with the specified claim name and owner.
     *
     * @param playerName the owner of the claim
     * @param name       the name of the claim
     * @return the chunk, or null if no chunk with the given claim name exists for the owner
     */
    public static Chunk getChunkByClaimName(String playerName, String name) {
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(playerName) && entry.getValue().getName().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the chunks where the specified player is a member but not the owner.
     *
     * @param playerName the name of the player
     * @return a map of chunks and their associated claims where the player is a member but not the owner
     */
    public static Map<Chunk, Claim> getChunksWhereMemberNotOwner(String playerName) {
        return listClaims.entrySet().stream()
                .filter(entry -> !entry.getValue().getOwner().equals(playerName) && entry.getValue().getMembers().contains(playerName))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    /**
     * Gets the names of all claims owned by the specified owner.
     *
     * @param owner the owner of the claims
     * @return a set of claim names owned by the owner
     */
    public static Set<String> getClaimsNameFromOwner(String owner) {
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
    public static Set<String> getClaimsNameInSaleFromOwner(String owner) {
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner) && entry.getValue().getSale())
                .map(entry -> entry.getValue().getName())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all chunks of the claims in sale.
     *
     * @return a set of chunks of the claims in sale
     */
    public static Set<Chunk> getClaimsChunkInSale() {
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getSale())
                .map(entry -> entry.getValue().getChunk())
                .collect(Collectors.toSet());
    }

    /**
     * Gets all members in claim chat mode for the specified player.
     *
     * @param playerName the name of the player
     * @return a list of all members in claim chat mode for the player
     */
    public static List<String> getAllMembersWithPlayerParallel(String playerName) {
        return listClaims.values().parallelStream()
                .filter(claim -> claim.getMembers().contains(playerName))
                .flatMap(claim -> claim.getMembers().stream())
                .filter(member -> !member.equals(playerName))
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Teleports the player to the specified location.
     *
     * @param player the player to teleport
     * @param loc    the location to teleport to
     */
    public static void goClaim(Player player, Location loc) {
        if (loc == null) return;

        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        int delay = cPlayer.getDelay();

        if (CPlayerMain.checkPermPlayer(player, "scs.bypass") || delay == 0) {
            teleportPlayer(player, loc);
            player.sendMessage(ClaimLanguage.getMessage("teleportation-success"));
            return;
        }

        player.sendMessage(ClaimLanguage.getMessage("teleportation-in-progress").replaceAll("%delay%", String.valueOf(delay)));
        Location originalLocation = player.getLocation().clone();
        playerLocations.put(player, originalLocation);

        Runnable teleportTask = createTeleportTask(player, loc, originalLocation, delay);
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
                teleportTask.run();
                if (!playerLocations.containsKey(player)) task.cancel();
            }, 0, 500, TimeUnit.MILLISECONDS);
        } else {
            new BukkitRunnable() {
                public void run() {
                    teleportTask.run();
                    if (!playerLocations.containsKey(player)) this.cancel();
                }
            }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 10L);
        }
    }

    /**
     * Teleports the player.
     *
     * @param player the player to teleport
     * @param loc    the location to teleport to
     */
    private static void teleportPlayer(Player player, Location loc) {
        if (SimpleClaimSystem.isFolia()) {
            player.teleportAsync(loc);
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
    private static Runnable createTeleportTask(Player player, Location loc, Location originalLocation, int delay) {
        return new Runnable() {
            int countdown = delay * 2;

            @Override
            public void run() {
                if (!player.isOnline() || !playerLocations.containsKey(player)) {
                    playerLocations.remove(player);
                    return;
                }

                if (!ClaimSettings.getBooleanSetting("teleportation-delay-moving")) {
                    Location currentLocation = player.getLocation();
                    if (!currentLocation.equals(originalLocation)) {
                        player.sendMessage(ClaimLanguage.getMessage("teleportation-canceled-moving"));
                        playerLocations.remove(player);
                        return;
                    }
                }

                if (countdown <= 0) {
                    teleportPlayer(player, loc);
                    player.sendMessage(ClaimLanguage.getMessage("teleportation-success"));
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
    public static boolean checkName(String owner, String name) {
        return listClaims.values().stream()
                .noneMatch(claim -> claim.getName().equals(name) && claim.getOwner().equals(owner));
    }

    /**
     * Checks if a claim is in sale.
     *
     * @param chunk the chunk to check
     * @return true if the claim is in sale, false otherwise
     */
    public static boolean claimIsInSale(Chunk chunk) {
        return listClaims.getOrDefault(chunk, null).getSale();
    }

    /**
     * Gets the claim description by the chunk.
     *
     * @param chunk the chunk to get the description from
     * @return the claim description
     */
    public static String getClaimDescription(Chunk chunk) {
        Claim claim = listClaims.getOrDefault(chunk, null);
        return claim == null ? "" : claim.getDescription();
    }

    /**
     * Gets the claim price by the chunk.
     *
     * @param chunk the chunk to get the price from
     * @return the claim price
     */
    public static Double getClaimPrice(Chunk chunk) {
        Claim claim = listClaims.getOrDefault(chunk, null);
        return claim == null ? 0.0 : claim.getPrice();
    }

    /**
     * Gets the claim name by the chunk.
     *
     * @param chunk the chunk to get the name from
     * @return the claim name
     */
    public static String getClaimNameByChunk(Chunk chunk) {
        Claim claim = listClaims.getOrDefault(chunk, null);
        return claim == null ? "" : claim.getName();
    }

    /**
     * Gets the claim location by the chunk.
     *
     * @param chunk the chunk to get the location from
     * @return the claim location
     */
    public static Location getClaimLocationByChunk(Chunk chunk) {
        Claim claim = listClaims.getOrDefault(chunk, null);
        return claim == null ? null : claim.getLocation();
    }

    /**
     * Gets the claim coordinates as a string by the chunk.
     *
     * @param chunk the chunk to get the coordinates from
     * @return the claim coordinates as a string
     */
    public static String getClaimCoords(Chunk chunk) {
        if (listClaims.containsKey(chunk)) {
            Claim claim = listClaims.get(chunk);
            Location loc = claim.getLocation();
            String world = loc.getWorld().getName();
            String x = String.valueOf(Math.round(loc.getX() * 10.0 / 10.0));
            String y = String.valueOf(Math.round(loc.getY() * 10.0 / 10.0));
            String z = String.valueOf(Math.round(loc.getZ() * 10.0 / 10.0));
            return world + ", " + x + ", " + y + ", " + z;
        }
        return "";
    }

    /**
     * Gets the claim coordinates as a string by the claim.
     *
     * @param claim the claim to get the coordinates from
     * @return the claim coordinates as a string
     */
    public static String getClaimCoords(Claim claim) {
        Location loc = claim.getLocation();
        String world = loc.getWorld().getName();
        String x = String.valueOf(Math.round(loc.getX() * 10.0 / 10.0));
        String y = String.valueOf(Math.round(loc.getY() * 10.0 / 10.0));
        String z = String.valueOf(Math.round(loc.getZ() * 10.0 / 10.0));
        return world + ", " + x + ", " + y + ", " + z;
    }

    /**
     * Gets the center X coordinate of a chunk.
     *
     * @param chunk the chunk to get the center X coordinate from
     * @return the center X coordinate
     */
    public static int getChunkCenterX(Chunk chunk) {
        int centerX = (chunk.getX() << 4) + 8;
        return centerX;
    }

    /**
     * Gets the center Y coordinate of a chunk.
     *
     * @param chunk the chunk to get the center Y coordinate from
     * @return the center Y coordinate
     */
    public static int getChunkCenterY(Chunk chunk) {
        World world = chunk.getWorld();
        int centerX = (chunk.getX() << 4) + 8;
        int centerZ = (chunk.getZ() << 4) + 8;
        int highestY = world.getHighestBlockYAt(centerX, centerZ) + 1;
        return highestY;
    }

    /**
     * Gets the center Z coordinate of a chunk.
     *
     * @param chunk the chunk to get the center Z coordinate from
     * @return the center Z coordinate
     */
    public static int getChunkCenterZ(Chunk chunk) {
        int centerZ = (chunk.getZ() << 4) + 8;
        return centerZ;
    }

    /**
     * Transfers local claims database to a distant database.
     */
    public static void transferClaims() {
    	Runnable task = () -> {
            HikariConfig localConfig = new HikariConfig();
            localConfig.setJdbcUrl("jdbc:sqlite:SimpleClaimSystem.getInstance()/SimpleClaimSystem/claims.db");
            localConfig.setDriverClassName("org.sqlite.JDBC");
            try (HikariDataSource localDataSource = new HikariDataSource(localConfig);
                 Connection localConn = localDataSource.getConnection();
                 PreparedStatement selectStmt = localConn.prepareStatement("SELECT * FROM scs_claims");
                 ResultSet rs = selectStmt.executeQuery();
                 Connection remoteConn = SimpleClaimSystem.getDataSource().getConnection();
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
                SimpleClaimSystem.getInstance().getLogger().info(count + " claims transferred");
                SimpleClaimSystem.getInstance().getLogger().info("Safe reloading..");
                SimpleClaimSystem.executeSync(() -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "aclaim reload"));
            } catch (SQLException e) {
                e.printStackTrace();
            }
    	};
    	SimpleClaimSystem.executeAsync(task);
    }

    /**
     * Loads claims from the database.
     */
    public static void loadClaims() {
        StringBuilder sb = new StringBuilder();
        for (String key : ClaimSettings.getDefaultValues().keySet()) {
            if (ClaimSettings.getDefaultValues().get(key)) {
                sb.append("1");
                continue;
            }
            sb.append("0");
        }
        ClaimSettings.setDefaultValuesCode(sb.toString());

        // Checking permissions (for update or new features)
        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
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
                        if (perm.length() != ClaimSettings.getDefaultValuesCode().length()) {
                            int diff = ClaimSettings.getDefaultValuesCode().length() - perm.length();
                            if (diff < 0) {
                                StringBuilder permCompleted = new StringBuilder(perm);
                                for (int i = 0; i < perm.length() - diff; i++) {
                                    permCompleted.append(ClaimSettings.getDefaultValuesCode().charAt(perm.length() + i));
                                }
                                String permFinal = permCompleted.toString();
                                preparedStatement.setString(1, permFinal);
                                preparedStatement.setInt(2, id);
                                preparedStatement.addBatch();
                                batchCount++;
                            } else {
                                StringBuilder permCompleted = new StringBuilder(perm);
                                for (int i = 0; i < diff; i++) {
                                    permCompleted.append(ClaimSettings.getDefaultValuesCode().charAt(perm.length() + i));
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

        int i = 0;
        int max_i = 0;
        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
            String getQuery = "SELECT * FROM scs_claims";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        max_i++;
                        String permissions = resultSet.getString("Permissions");
                        String id = resultSet.getString("id");
                        String owner = resultSet.getString("name");
                        String name = resultSet.getString("claim_name");
                        String description = resultSet.getString("claim_description");
                        int X = resultSet.getInt("X");
                        int Z = resultSet.getInt("Z");
                        String world_name = resultSet.getString("World");
                        World world = Bukkit.getWorld(world_name);
                        if (world == null) continue;
                        String[] parts = resultSet.getString("Location").split(";");
                        double L_X = Double.parseDouble(parts[0]);
                        double L_Y = Double.parseDouble(parts[1]);
                        double L_Z = Double.parseDouble(parts[2]);
                        float L_Yaw = (float) Double.parseDouble(parts[3]);
                        float L_Pitch = (float) Double.parseDouble(parts[4]);
                        String s_members = resultSet.getString("Members");
                        Set<String> members = new HashSet<>();
                        if (!s_members.isBlank()) {
                            parts = s_members.split(";");
                            for (String m : parts) {
                                members.add(m);
                            }
                        }
                        String s_bans = resultSet.getString("Bans");
                        Set<String> bans = new HashSet<>();
                        if (!s_bans.isBlank()) {
                            parts = s_bans.split(";");
                            for (String m : parts) {
                                bans.add(m);
                            }
                        }
                        boolean sale = resultSet.getBoolean("isSale");
                        Double price = resultSet.getDouble("SalePrice");
                        if (SimpleClaimSystem.isFolia()) {
                            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), world, X, Z, task -> {
                                Chunk chunk = world.getChunkAt(X, Z);
                                if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, name, owner);
                                if (claimsId.containsKey(owner)) {
                                    claimsId.get(owner).put(chunk, id);
                                } else {
                                    Map<Chunk, String> ids = new HashMap<>();
                                    ids.put(chunk, id);
                                    claimsId.put(owner, ids);
                                }
                                LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>();
                                int count_i = 0;
                                for (String perm_key : ClaimSettings.getDefaultValues().keySet()) {
                                    char currentChar = permissions.charAt(count_i);
                                    count_i++;
                                    if (currentChar == '1') {
                                        perms.put(perm_key, true);
                                        continue;
                                    }
                                    perms.put(perm_key, false);
                                }
                                Location location = new Location(world, L_X, L_Y, L_Z, L_Yaw, L_Pitch);
                                listClaims.put(chunk, new Claim(chunk, owner, members, location, name, description, perms, sale, price, bans));
                            });
                        } else {
                            Chunk chunk = world.getChunkAt(X, Z);
                            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, name, owner);
                            if (claimsId.containsKey(owner)) {
                                claimsId.get(owner).put(chunk, id);
                            } else {
                                Map<Chunk, String> ids = new HashMap<>();
                                ids.put(chunk, id);
                                claimsId.put(owner, ids);
                            }
                            LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>();
                            int count_i = 0;
                            for (String perm_key : ClaimSettings.getDefaultValues().keySet()) {
                                char currentChar = permissions.charAt(count_i);
                                count_i++;
                                if (currentChar == '1') {
                                    perms.put(perm_key, true);
                                    continue;
                                }
                                perms.put(perm_key, false);
                            }
                            Location location = new Location(world, L_X, L_Y, L_Z, L_Yaw, L_Pitch);
                            listClaims.put(chunk, new Claim(chunk, owner, members, location, name, description, perms, sale, price, bans));
                        }
                        i++;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        SimpleClaimSystem.getInstance().getLogger().info(String.valueOf(i) + "/" + String.valueOf(max_i) + " claims loaded");
        return;
    }

    /**
     * Creates a new claim for the player.
     *
     * @param player the player creating the claim
     * @param chunk  the chunk to claim
     */
    public static void createClaim(Player player, Chunk chunk) {
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);

        if (listClaims.containsKey(chunk)) {
            handleClaimConflict(player, chunk);
            return;
        }

        if (!cPlayer.canClaim()) {
            player.sendMessage(ClaimLanguage.getMessage("cant-claim-anymore"));
            return;
        }

        if (ClaimSettings.getBooleanSetting("economy") && ClaimSettings.getBooleanSetting("claim-cost")) {
            double price = ClaimSettings.getBooleanSetting("claim-cost-multiplier") ? cPlayer.getMultipliedCost() : cPlayer.getCost();
            double balance = ClaimVault.getPlayerBalance(playerName);

            if (balance < price) {
                player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money-claim").replaceAll("%missing-price%", String.valueOf(price - balance)));
                return;
            }

            ClaimVault.removePlayerBalance(playerName, price);
            if (price > 0) player.sendMessage(ClaimLanguage.getMessage("you-paid-claim").replaceAll("%price%", String.valueOf(price)));
        }

        if (ClaimSettings.getBooleanSetting("claim-particles")) displayChunk(player, chunk, true);
        cPlayer.setClaimsCount(cPlayer.getClaimsCount() + 1);
        int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
        player.sendMessage(ClaimLanguage.getMessage("create-claim-success").replaceAll("%remaining-claims%", String.valueOf(remainingClaims)));

        String uuid = player.getUniqueId().toString();
        int id = findFreeId(playerName);
        String claimName = "claim-" + id;
        String description = ClaimLanguage.getMessage("default-description");
        String locationString = getLocationString(player.getLocation());

        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
        Claim newClaim = new Claim(chunk, playerName, Set.of(playerName), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>());

        listClaims.put(chunk, newClaim);
        claimsId.computeIfAbsent(playerName, k -> new HashMap<>()).put(chunk, String.valueOf(id));

        if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claimName, playerName);
        if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.createChunkZone(chunk, claimName, playerName);
        if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.createChunkZone(chunk, claimName, playerName);

        activateBossBar(chunk);

        SimpleClaimSystem.executeAsync(() -> insertClaimIntoDatabase(id, uuid, playerName, claimName, description, chunk, locationString));
    }

    /**
     * Handles the case where a claim conflict occurs when creating a claim.
     *
     * @param player the player attempting to create the claim
     * @param chunk  the chunk to claim
     */
    private static void handleClaimConflict(Player player, Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        String owner = claim.getOwner();

        if (owner.equals("admin")) {
            player.sendMessage(ClaimLanguage.getMessage("create-error-protected-area"));
        } else if (owner.equals(player.getName())) {
            player.sendMessage(ClaimLanguage.getMessage("create-already-yours"));
        } else {
            player.sendMessage(ClaimLanguage.getMessage("create-already-claim").replace("%player%", owner));
        }
    }

    /**
     * Gets the location string for the specified location.
     *
     * @param location the location to get the string for
     * @return the location string
     */
    private static String getLocationString(Location location) {
        return String.format("%s;%s;%s;%s;%s", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
    }

    /**
     * Activates the boss bar for players in the specified chunk.
     *
     * @param chunk the chunk to activate the boss bar in
     */
    private static void activateBossBar(Chunk chunk) {
        Runnable bossBarTask = () -> {
            for (Entity entity : chunk.getEntities()) {
                if (entity instanceof Player) {
                    ClaimEventsEnterLeave.activeBossBar((Player) entity, chunk);
                }
            }
        };

        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), task -> bossBarTask.run());
        } else {
            bossBarTask.run();
        }
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
    private static void insertClaimIntoDatabase(int id, String uuid, String playerName, String claimName, String description, Chunk chunk, String locationString) {
        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection();
             PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
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
            stmt.setString(11, ClaimSettings.getDefaultValuesCode());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates a new protected area (admin claim).
     *
     * @param player the player creating the claim
     * @param chunk  the chunk to claim
     */
    public static void createAdminClaim(Player player, Chunk chunk) {
        String playerName = player.getName();

        if (listClaims.containsKey(chunk)) {
            handleClaimConflict(player, chunk);
            return;
        }

        if (ClaimSettings.getBooleanSetting("claim-particles")) displayChunk(player, chunk, true);
        player.sendMessage(ClaimLanguage.getMessage("create-protected-area-success"));

        String uuid = "aucun";
        int id = findFreeId("admin");
        String claimName = "admin-" + id;
        String description = ClaimLanguage.getMessage("default-description");
        String locationString = getLocationString(player.getLocation());

        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
        Claim newClaim = new Claim(chunk, "admin", new HashSet<>(), player.getLocation(), claimName, description, perms, false, 0.0, new HashSet<>());

        listClaims.put(chunk, newClaim);
        claimsId.computeIfAbsent("admin", k -> new HashMap<>()).put(chunk, String.valueOf(id));

        if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claimName, "admin");
        if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.createChunkZone(chunk, claimName, "admin");
        if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.createChunkZone(chunk, claimName, "admin");

        activateBossBar(chunk);

        SimpleClaimSystem.executeAsync(() -> insertClaimIntoDatabase(id, uuid, "admin", claimName, description, chunk, locationString));
    }

    /**
     * Creates multiple claims within a radius.
     *
     * @param player the player creating the claims
     * @param chunks the chunks to claim
     * @param radius the radius within which to claim chunks
     * @return true if the claims were created successfully, false otherwise
     */
    public static boolean createClaimRadius(Player player, Set<Chunk> chunks, int radius) {
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);

        Set<Chunk> chunksToClaim = chunks.stream()
                .filter(chunk -> !checkIfClaimExists(chunk))
                .collect(Collectors.toSet());

        if (chunks.size() != chunksToClaim.size()) {
            player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim-already-claim"));
            return false;
        }

        if (!cPlayer.canClaimWithNumber(chunksToClaim.size())) {
            player.sendMessage(ClaimLanguage.getMessage("cant-claim-anymore"));
            return false;
        }

        double price = calculateClaimPrice(cPlayer, chunksToClaim.size());
        if (price > 0 && !processPayment(player, playerName, price)) {
            return true;
        }

        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
        Map<Chunk, Map<String, String>> values = new HashMap<>();

        for (Chunk chunk : chunksToClaim) {
            int id = findFreeId(playerName);
            String claimName = "claim-" + id;
            String locationString = getLocationString(player, chunk);
            Map<String, String> claimValues = createClaimValues(id, playerName, claimName, locationString, chunk);

            values.put(chunk, claimValues);
            listClaims.put(chunk, new Claim(chunk, playerName, Set.of(playerName), getSpawnLocation(player, chunk), claimName, ClaimLanguage.getMessage("default-description"), perms, false, 0.0, new HashSet<>()));
            claimsId.computeIfAbsent(playerName, k -> new HashMap<>()).put(chunk, String.valueOf(id));

            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claimName, playerName);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.createChunkZone(chunk, claimName, playerName);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.createChunkZone(chunk, claimName, playerName);

            activateBossBar(chunk);
        }

        if (ClaimSettings.getBooleanSetting("claim-particles")) {
            displayChunkBorderWithRadius(player, player.getLocation().getChunk(), radius);
        }

        cPlayer.setClaimsCount(cPlayer.getClaimsCount() + chunksToClaim.size());
        insertClaimsIntoDatabase(values);

        int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
        player.sendMessage(ClaimLanguage.getMessage("create-claim-radius-success").replace("%number%", String.valueOf(chunksToClaim.size())).replace("%remaining-claims%", String.valueOf(remainingClaims)));

        return true;
    }

    /**
     * Creates multiple protected areas (admin claims) within a radius.
     *
     * @param player the player creating the claims
     * @param chunks the chunks to claim
     * @param radius the radius within which to claim chunks
     * @return true if the claims were created successfully, false otherwise
     */
    public static boolean createAdminClaimRadius(Player player, Set<Chunk> chunks, int radius) {
        Set<Chunk> chunksToClaim = chunks.stream()
                .filter(chunk -> !checkIfClaimExists(chunk))
                .collect(Collectors.toSet());

        if (chunks.size() != chunksToClaim.size()) {
            player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim-already-claim"));
            return false;
        }

        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
        Map<Chunk, Map<String, String>> values = new HashMap<>();

        for (Chunk chunk : chunksToClaim) {
            int id = findFreeId("admin");
            String claimName = "admin-" + id;
            String locationString = getLocationString(player, chunk);
            Map<String, String> claimValues = createClaimValues(id, "admin", claimName, locationString, chunk);

            values.put(chunk, claimValues);
            listClaims.put(chunk, new Claim(chunk, "admin", new HashSet<>(), getSpawnLocation(player, chunk), claimName, ClaimLanguage.getMessage("default-description"), perms, false, 0.0, new HashSet<>()));
            claimsId.computeIfAbsent("admin", k -> new HashMap<>()).put(chunk, String.valueOf(id));

            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claimName, "admin");
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.createChunkZone(chunk, claimName, "admin");
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.createChunkZone(chunk, claimName, "admin");

            activateBossBar(chunk);
        }

        if (ClaimSettings.getBooleanSetting("claim-particles")) {
            displayChunkBorderWithRadius(player, player.getLocation().getChunk(), radius);
        }

        insertClaimsIntoDatabase(values);

        player.sendMessage(ClaimLanguage.getMessage("create-protected-area-radius-success").replace("%number%", String.valueOf(chunksToClaim.size())));

        return true;
    }

    /**
     * Calculates the price for creating multiple claims.
     *
     * @param cPlayer  the player creating the claims
     * @param numClaims the number of claims being created
     * @return the total price for creating the claims
     */
    private static double calculateClaimPrice(CPlayer cPlayer, int numClaims) {
        if (!ClaimSettings.getBooleanSetting("economy") || !ClaimSettings.getBooleanSetting("claim-cost")) {
            return 0;
        }
        return ClaimSettings.getBooleanSetting("claim-cost-multiplier") ? cPlayer.getRadiusMultipliedCost(numClaims) : cPlayer.getCost() * numClaims;
    }

    /**
     * Processes the payment for creating claims.
     *
     * @param player    the player creating the claims
     * @param playerName the name of the player creating the claims
     * @param price     the total price for creating the claims
     * @return true if the payment was successful, false otherwise
     */
    private static boolean processPayment(Player player, String playerName, double price) {
        double balance = ClaimVault.getPlayerBalance(playerName);
        if (balance < price) {
            player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", String.valueOf(price - balance)));
            return false;
        }
        ClaimVault.removePlayerBalance(playerName, price);
        player.sendMessage(ClaimLanguage.getMessage("you-paid-claim").replace("%price%", String.valueOf(price)));
        return true;
    }

    /**
     * Gets the location string for the specified player and chunk.
     *
     * @param player the player
     * @param chunk  the chunk
     * @return the location string
     */
    private static String getLocationString(Player player, Chunk chunk) {
        Location location = player.getLocation();
        if (player.getLocation().getChunk().equals(chunk)) {
            return String.format("%s;%s;%s;%s;%s", location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
        } else {
            int locX = getChunkCenterX(chunk);
            int locY = getChunkCenterY(chunk);
            int locZ = getChunkCenterZ(chunk);
            return String.format("%s;%s;%s;%s;%s", locX, locY, locZ, location.getYaw(), location.getPitch());
        }
    }

    /**
     * Gets the spawn location for the specified player and chunk.
     *
     * @param player the player
     * @param chunk  the chunk
     * @return the spawn location
     */
    private static Location getSpawnLocation(Player player, Chunk chunk) {
        Location location = player.getLocation();
        if (!player.getLocation().getChunk().equals(chunk)) {
            int locX = getChunkCenterX(chunk);
            int locY = getChunkCenterY(chunk);
            int locZ = getChunkCenterZ(chunk);
            return new Location(location.getWorld(), locX, locY, locZ);
        }
        return location;
    }

    /**
     * Creates the values for a new claim.
     *
     * @param id          the ID of the claim
     * @param owner       the owner of the claim
     * @param claimName   the name of the claim
     * @param location    the location string of the claim
     * @param chunk       the chunk being claimed
     * @return a map of claim values
     */
    private static Map<String, String> createClaimValues(int id, String owner, String claimName, String location, Chunk chunk) {
        Map<String, String> values = new HashMap<>();
        values.put("id", String.valueOf(id));
        values.put("uuid", owner.equals("admin") ? "aucun" : Bukkit.getPlayer(owner).getUniqueId().toString());
        values.put("owner", owner);
        values.put("claim_name", claimName);
        values.put("description", ClaimLanguage.getMessage("default-description"));
        values.put("X", String.valueOf(chunk.getX()));
        values.put("Z", String.valueOf(chunk.getZ()));
        values.put("World", chunk.getWorld().getName());
        values.put("Location", location);
        values.put("Members", owner.equals("admin") ? "" : owner);
        values.put("Permissions", ClaimSettings.getDefaultValuesCode());
        return values;
    }

    /**
     * Inserts multiple claims into the database.
     *
     * @param values the values of the claims to insert
     */
    private static void insertClaimsIntoDatabase(Map<Chunk, Map<String, String>> values) {
        SimpleClaimSystem.executeAsync(() -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection();
                 PreparedStatement stmt = connection.prepareStatement(
                         "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                for (Map.Entry<Chunk, Map<String, String>> entry : values.entrySet()) {
                    Map<String, String> claimValues = entry.getValue();
                    stmt.setString(1, claimValues.get("id"));
                    stmt.setString(2, claimValues.get("uuid"));
                    stmt.setString(3, claimValues.get("owner"));
                    stmt.setString(4, claimValues.get("claim_name"));
                    stmt.setString(5, claimValues.get("description"));
                    stmt.setString(6, claimValues.get("X"));
                    stmt.setString(7, claimValues.get("Z"));
                    stmt.setString(8, claimValues.get("World"));
                    stmt.setString(9, claimValues.get("Location"));
                    stmt.setString(10, claimValues.get("Members"));
                    stmt.setString(11, claimValues.get("Permissions"));
                    stmt.addBatch();
                }
                stmt.executeBatch();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Checks if a claim exists in the given chunk.
     *
     * @param chunk the chunk to check
     * @return true if a claim exists in the chunk, false otherwise
     */
    public static boolean checkIfClaimExists(Chunk chunk) {
        return listClaims.containsKey(chunk);
    }

    /**
     * Gets the owner of a claim by the chunk.
     *
     * @param chunk the chunk to get the owner from
     * @return the owner of the claim, or an empty string if no claim exists for the chunk
     */
    public static String getOwnerInClaim(Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        return claim == null ? "" : claim.getOwner();
    }

    /**
     * Checks if a permission is allowed for the given chunk.
     *
     * @param chunk the chunk to check
     * @param perm  the permission to check
     * @return true if the permission is allowed, false otherwise
     */
    public static boolean canPermCheck(Chunk chunk, String perm) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getPermission(perm);
    }

    /**
     * Checks if a player is a member of a claim.
     *
     * @param chunk  the chunk to check
     * @param player the player to check
     * @return true if the player is a member of the claim, false otherwise
     */
    public static boolean checkMembre(Chunk chunk, Player player) {
    	Claim claim = listClaims.get(chunk);
    	return claim == null ? false : claim.getMembers().contains(player.getName());
    }

    /**
     * Checks if a player name is a member of a claim.
     *
     * @param chunk      the chunk to check
     * @param targetName the name of the player to check
     * @return true if the player name is a member of the claim, false otherwise
     */
    public static boolean checkMembre(Chunk chunk, String targetName) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getMembers().stream().anyMatch(member -> member.equalsIgnoreCase(targetName));
    }

    /**
     * Checks if a player name is banned from a claim.
     *
     * @param chunk      the chunk to check
     * @param targetName the name of the player to check
     * @return true if the player name is banned from the claim, false otherwise
     */
    public static boolean checkBan(Chunk chunk, String targetName) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getBans().stream().anyMatch(ban -> ban.equalsIgnoreCase(targetName));
    }

    /**
     * Gets the real name of a player from claim members.
     *
     * @param chunk      the chunk to check
     * @param targetName the name of the player to check
     * @return the real name of the player, or the target name if not found
     */
    public static String getRealNameFromClaimMembers(Chunk chunk, String targetName) {
        Claim claim = listClaims.get(chunk);
        return claim != null ? claim.getMembers().stream().filter(member -> member.equalsIgnoreCase(targetName)).findFirst().orElse(targetName) : targetName;
    }

    /**
     * Gets the real name of a player from claim bans.
     *
     * @param chunk      the chunk to check
     * @param targetName the name of the player to check
     * @return the real name of the player, or the target name if not found
     */
    public static String getRealNameFromClaimBans(Chunk chunk, String targetName) {
        Claim claim = listClaims.get(chunk);
        return claim != null ? claim.getBans().stream().filter(ban -> ban.equalsIgnoreCase(targetName)).findFirst().orElse(targetName) : targetName;
    }

    /**
     * Checks if a player is banned from a claim.
     *
     * @param chunk  the chunk to check
     * @param player the player to check
     * @return true if the player is banned from the claim, false otherwise
     */
    public static boolean checkBan(Chunk chunk, Player player) {
        Claim claim = listClaims.get(chunk);
        return claim != null && claim.getBans().contains(player.getName());
    }

    /**
     * Gets the members of a claim.
     *
     * @param chunk the chunk to get the members from
     * @return a set of members of the claim
     */
    public static Set<String> getClaimMembers(Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        return claim != null ? claim.getMembers() : new HashSet<>();
    }

    /**
     * Gets the bans of a claim.
     *
     * @param chunk the chunk to get the bans from
     * @return a set of bans of the claim
     */
    public static Set<String> getClaimBans(Chunk chunk) {
        Claim claim = listClaims.get(chunk);
        return claim != null ? claim.getBans() : new HashSet<>();
    }

    /**
     * Updates a claim's permission.
     *
     * @param player the player updating the permission
     * @param chunk  the chunk to update the permission for
     * @param perm   the permission to update
     * @param result the new value of the permission
     * @return true if the permission was updated successfully, false otherwise
     */
    public static boolean updatePerm(Player player, Chunk chunk, String perm, boolean result) {
        return updateClaimPermission(chunk, perm, result, player.getUniqueId().toString(), player.getName());
    }

    /**
     * Updates an admin claim's permission.
     *
     * @param chunk  the chunk to update the permission for
     * @param perm   the permission to update
     * @param result the new value of the permission
     * @return true if the permission was updated successfully, false otherwise
     */
    public static boolean updateAdminPerm(Chunk chunk, String perm, boolean result) {
        return updateClaimPermission(chunk, perm, result, "aucun", "admin");
    }

    /**
     * Updates the permission of a claim.
     *
     * @param chunk  the chunk to update the permission for
     * @param perm   the permission to update
     * @param result the new value of the permission
     * @param uuid   the UUID of the owner of the claim
     * @param name   the name of the owner of the claim
     * @return true if the permission was updated successfully, false otherwise
     */
    private static boolean updateClaimPermission(Chunk chunk, String perm, boolean result, String uuid, String name) {
        Claim claim = listClaims.get(chunk);
        if (claim == null) return false;

        claim.getPermissions().put(perm, result);

        if (perm.equals("Weather")) updateWeatherChunk(chunk, result);
        if (perm.equals("Fly")) updateFlyChunk(chunk, result);

        String permissions = claim.getPermissions().entrySet().stream()
                .map(entry -> entry.getValue() ? "1" : "0")
                .collect(Collectors.joining());

        Runnable task = () -> updateClaimPermissionsInDB(uuid, name, chunk, permissions);
        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Updates the permissions of a claim in the database.
     *
     * @param uuid        the UUID of the owner of the claim
     * @param name        the name of the owner of the claim
     * @param chunk       the chunk to update the permissions for
     * @param permissions the new permissions of the claim
     */
    private static void updateClaimPermissionsInDB(String uuid, String name, Chunk chunk, String permissions) {
        String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, permissions);
            preparedStatement.setString(2, uuid);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, String.valueOf(chunk.getX()));
            preparedStatement.setString(5, String.valueOf(chunk.getZ()));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Removes a member from a claim.
     *
     * @param player the player removing the member
     * @param chunk  the chunk to remove the member from
     * @param name   the name of the member to remove
     * @return true if the member was removed successfully, false otherwise
     */
    public static boolean removeClaimMembers(Player player, Chunk chunk, String name) {
        return removeMemberFromClaim(chunk, name, player.getUniqueId().toString(), player.getName(), player);
    }

    /**
     * Removes a member from an admin claim.
     *
     * @param chunk the chunk to remove the member from
     * @param name  the name of the member to remove
     * @return true if the member was removed successfully, false otherwise
     */
    public static boolean removeAdminClaimMembers(Chunk chunk, String name) {
        return removeMemberFromClaim(chunk, name, "aucun", "admin", null);
    }

    /**
     * Removes a member from a claim.
     *
     * @param chunk    the chunk to remove the member from
     * @param name     the name of the member to remove
     * @param uuid     the UUID of the owner of the claim
     * @param ownerName the name of the owner of the claim
     * @param player   the player removing the member (if applicable)
     * @return true if the member was removed successfully, false otherwise
     */
    private static boolean removeMemberFromClaim(Chunk chunk, String name, String uuid, String ownerName, Player player) {
        Claim claim = listClaims.get(chunk);
        if (claim == null) return false;

        claim.removeMember(name);
        String membersString = String.join(";", claim.getMembers());

        if (player != null) {
            Player target = Bukkit.getPlayer(name);
            if (target != null) {
                target.sendMessage(ClaimLanguage.getMessage("remove-claim-player")
                        .replace("%claim-name%", claim.getName())
                        .replace("%owner%", player.getName()));
            }
        }

        Runnable task = () -> updateClaimMembersInDB(uuid, ownerName, chunk, membersString);
        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Updates the members of a claim in the database.
     *
     * @param uuid     the UUID of the owner of the claim
     * @param name     the name of the owner of the claim
     * @param chunk    the chunk to update the members for
     * @param members  the new members of the claim
     */
    private static void updateClaimMembersInDB(String uuid, String name, Chunk chunk, String members) {
        String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, members);
            preparedStatement.setString(2, uuid);
            preparedStatement.setString(3, name);
            preparedStatement.setString(4, String.valueOf(chunk.getX()));
            preparedStatement.setString(5, String.valueOf(chunk.getZ()));
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


    /**
     * Method to apply current settings to all admin claims.
     *
     * @param chunk the chunk from which to apply settings to all admin claims
     * @return true if the operation was successful, false otherwise
     */
    public static boolean applyAllSettingsAdmin(Chunk chunk) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim c = listClaims.get(chunk);
        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(c.getPermissions());
        listClaims.values().stream()
            .filter(claim -> "admin".equals(claim.getOwner()))
            .forEach(claim -> claim.setPermissions(perms));

        Runnable task = () -> {
            StringBuilder sb = new StringBuilder();
            for (String key : perms.keySet()) {
                sb.append(perms.get(key) ? "1" : "0");
            }
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, sb.toString());
                    preparedStatement.setString(2, "aucun");
                    preparedStatement.setString(3, "admin");
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to apply current settings to all player's claims.
     *
     * @param chunk the chunk from which to apply settings to all player's claims
     * @param player the player whose claims will be updated
     * @return true if the operation was successful, false otherwise
     */
    public static boolean applyAllSettings(Chunk chunk, Player player) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim c = listClaims.get(chunk);
        LinkedHashMap<String, Boolean> perms = new LinkedHashMap<>(c.getPermissions());
        listClaims.values().stream()
            .filter(claim -> player.getName().equals(claim.getOwner()))
            .forEach(claim -> claim.setPermissions(perms));

        Runnable task = () -> {
            StringBuilder sb = new StringBuilder();
            for (String key : perms.keySet()) {
                sb.append(perms.get(key) ? "1" : "0");
            }
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, sb.toString());
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to ban a player from a player's claim.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param name the name of the player to be banned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addClaimBan(Player player, Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.addBan(name);
        String banString = String.join(";", claim.getBans());
        if (claim.getMembers().contains(name)) removeClaimMembers(player, chunk, name);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, banString);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to ban a member from an admin claim.
     *
     * @param chunk the chunk representing the admin claim
     * @param name the name of the member to be banned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addAdminClaimBan(Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.addBan(name);
        String banString = String.join(";", claim.getBans());
        if (claim.getMembers().contains(name)) removeAdminClaimMembers(chunk, name);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, banString);
                    preparedStatement.setString(2, "aucun");
                    preparedStatement.setString(3, "admin");
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to unban a player from a player's claim.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param name the name of the player to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean removeClaimBan(Player player, Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.removeBan(name);
        String banString = String.join(";", claim.getBans());

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, banString);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to unban a member from an admin claim.
     *
     * @param chunk the chunk representing the admin claim
     * @param name the name of the member to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean removeAdminClaimBan(Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.removeBan(name);
        String banString = String.join(";", claim.getBans());

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, banString);
                    preparedStatement.setString(2, "aucun");
                    preparedStatement.setString(3, "admin");
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to add a member to a player's claim.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addClaimMembers(Player player, Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.addMember(name);
        String membersString = String.join(";", claim.getMembers());
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            target.sendMessage(ClaimLanguage.getMessage("add-claim-player").replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", player.getName()));
        }

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, membersString);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to add a member to an admin claim.
     *
     * @param chunk the chunk representing the admin claim
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addAdminClaimMembers(Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.addMember(name);
        String membersString = String.join(";", claim.getMembers());
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            target.sendMessage(ClaimLanguage.getMessage("add-claim-protected-area-player").replaceAll("%claim-name%", claim.getName()));
        }

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, membersString);
                    preparedStatement.setString(2, "aucun");
                    preparedStatement.setString(3, "admin");
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to add a member to all admin claims.
     *
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addAllAdminClaimMembers(String name) {
        listClaims.values().stream()
            .filter(claim -> "admin".equals(claim.getOwner()))
            .forEach(claim -> claim.addMember(name));
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            target.sendMessage(ClaimLanguage.getMessage("add-all-claim-protected-area-player"));
        }

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner("admin")) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getMembers());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, "aucun");
                        preparedStatement.setString(3, "admin");
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to ban a member from all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the member to be banned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addAllClaimBan(Player player, String name) {
        String playerName = player.getName();
        listClaims.values().stream()
            .filter(claim -> playerName.equals(claim.getOwner()))
            .forEach(claim -> claim.addBan(name));
        removeAllClaimMembers(player, name);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner(playerName)) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getBans());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, player.getName());
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to unban a player from all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the player to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean removeAllClaimBan(Player player, String name) {
        String playerName = player.getName();
        listClaims.values().stream()
            .filter(claim -> playerName.equals(claim.getOwner()))
            .forEach(claim -> claim.removeBan(name));

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner(playerName)) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getBans());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, player.getName());
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to ban a player to all admin claims.
     *
     * @param name the name of the player to be banned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addAllAdminClaimBan(String name) {
        listClaims.values().stream()
            .filter(claim -> "admin".equals(claim.getOwner()))
            .forEach(claim -> claim.addBan(name));
        removeAllAdminClaimMembers(name);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner("admin")) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getBans());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, "aucun");
                        preparedStatement.setString(3, "admin");
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to unban a member from all admin claims.
     *
     * @param name the name of the member to be unbanned
     * @return true if the operation was successful, false otherwise
     */
    public static boolean removeAllAdminClaimBan(String name) {
        listClaims.values().stream()
            .filter(claim -> "admin".equals(claim.getOwner()))
            .forEach(claim -> claim.removeBan(name));

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner("admin")) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getBans());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, "aucun");
                        preparedStatement.setString(3, "admin");
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to add a member to all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the member to be added
     * @return true if the operation was successful, false otherwise
     */
    public static boolean addAllClaimMembers(Player player, String name) {
        String playerName = player.getName();
        listClaims.values().stream()
            .filter(claim -> playerName.equals(claim.getOwner()))
            .forEach(claim -> claim.addMember(name));
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            target.sendMessage(ClaimLanguage.getMessage("add-all-claim-player").replaceAll("%owner%", playerName));
        }

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner(playerName)) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getMembers());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to remove a member from all admin claims.
     *
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public static boolean removeAllAdminClaimMembers(String name) {
        listClaims.values().stream()
            .filter(claim -> "admin".equals(claim.getOwner()))
            .forEach(claim -> claim.removeMember(name));
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            target.sendMessage(ClaimLanguage.getMessage("remove-all-claim-protected-area-player"));
        }

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner("admin")) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getMembers());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, "aucun");
                        preparedStatement.setString(3, "admin");
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to remove a member from all player's claims.
     *
     * @param player the player who owns the claims
     * @param name the name of the member to be removed
     * @return true if the operation was successful, false otherwise
     */
    public static boolean removeAllClaimMembers(Player player, String name) {
        String playerName = player.getName();
        listClaims.values().stream()
            .filter(claim -> playerName.equals(claim.getOwner()))
            .forEach(claim -> claim.removeMember(name));
        Player target = Bukkit.getPlayer(name);
        if (target != null) {
            target.sendMessage(ClaimLanguage.getMessage("remove-all-claim-player").replaceAll("%owner%", playerName));
        }

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    for (Chunk chunk : getChunksFromOwner(playerName)) {
                        Claim claim = listClaims.get(chunk);
                        String banString = String.join(";", claim.getMembers());
                        preparedStatement.setString(1, banString);
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, String.valueOf(chunk.getX()));
                        preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                        preparedStatement.addBatch();
                    }
                    preparedStatement.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to change a player's claim's name.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param name the new name for the claim
     * @return true if the operation was successful, false otherwise
     */
    public static boolean setClaimName(Player player, Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setName(name);
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    ClaimEventsEnterLeave.activeBossBar(p, chunk);
                }
            });
        } else {
            for (Entity e : chunk.getEntities()) {
                if (!(e instanceof Player)) continue;
                Player p = (Player) e;
                ClaimEventsEnterLeave.activeBossBar(p, chunk);
            }
        }

        Runnable task = () -> {
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.updateName(chunk);
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to change an admin's claim's name.
     *
     * @param chunk the chunk representing the admin claim
     * @param name the new name for the claim
     * @return true if the operation was successful, false otherwise
     */
    public static boolean setAdminClaimName(Chunk chunk, String name) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setName(name);
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    ClaimEventsEnterLeave.activeBossBar(p, chunk);
                }
            });
        } else {
            for (Entity e : chunk.getEntities()) {
                if (!(e instanceof Player)) continue;
                Player p = (Player) e;
                ClaimEventsEnterLeave.activeBossBar(p, chunk);
            }
        }

        Runnable task = () -> {
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.updateName(chunk);
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, name);
                    preparedStatement.setString(2, "aucun");
                    preparedStatement.setString(3, "admin");
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to change the claim's spawn location.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param loc the new location for the claim's spawn
     * @return true if the operation was successful, false otherwise
     */
    public static boolean setClaimLocation(Player player, Chunk chunk, Location loc) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setLocation(loc);

        Runnable task = () -> {
            String loc_string = String.valueOf(loc.getX()) + ";" + String.valueOf(loc.getY()) + ";" + String.valueOf(loc.getZ()) + ";" + String.valueOf(loc.getYaw()) + ";" + String.valueOf(loc.getPitch());
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET Location = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, loc_string);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to delete a claim with radius.
     *
     * @param player the player who owns the claims
     * @param chunks the set of chunks representing the claims to be deleted
     * @return true if the operation was successful, false otherwise
     */
    public static boolean deleteClaimRadius(Player player, Set<Chunk> chunks) {
        String owner = "";
        String uuid = "";
        List<Integer> ids = new ArrayList<>();
        for (Chunk chunk : chunks) {
            if (!listClaims.containsKey(chunk)) continue;
            Claim claim = listClaims.get(chunk);
            owner = claim.getOwner();
            CPlayer cPlayer = CPlayerMain.getCPlayer(owner);
            if (cPlayer != null) {
                cPlayer.setClaimsCount(cPlayer.getClaimsCount() - 1);
            }
            uuid = "";
            if (owner.equals("admin")) {
                ids.add(Integer.parseInt(claimsId.get("admin").get(chunk)));
                uuid = "aucun";
            } else {
                ids.add(Integer.parseInt(claimsId.get(owner).get(chunk)));
                uuid = player.getUniqueId().toString();
            }
            claimsId.get(owner).remove(chunk);
            if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
            listClaims.remove(chunk);
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.deleteMarker(chunk);
            if (SimpleClaimSystem.isFolia()) {
                Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.disableBossBar(p);
                    }
                });
            } else {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    ClaimEventsEnterLeave.disableBossBar(p);
                }
            }
        }
        final String final_uuid = uuid;
        String idsString = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                    preparedStatement.setString(1, final_uuid);
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to delete a claim.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim to be deleted
     * @return true if the operation was successful, false otherwise
     */
    public static boolean deleteClaim(Player player, Chunk chunk) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        String owner = claim.getOwner();
        listClaims.remove(chunk);

        CPlayer cPlayer = CPlayerMain.getCPlayer(owner);
        if (cPlayer != null) {
            cPlayer.setClaimsCount(cPlayer.getClaimsCount() - 1);
        }

        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    ClaimEventsEnterLeave.disableBossBar(p);
                }
            });
        } else {
            for (Entity e : chunk.getEntities()) {
                if (!(e instanceof Player)) continue;
                Player p = (Player) e;
                ClaimEventsEnterLeave.disableBossBar(p);
            }
        }

        Runnable task = () -> {
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.deleteMarker(chunk);
            String id = claimsId.get(owner).get(chunk);
            String uuid = player.getUniqueId().toString();
            if (owner.equals("admin")) uuid = "aucun";
            claimsId.get(owner).remove(chunk);
            if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
            listClaims.remove(chunk);
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String deleteQuery = "DELETE FROM scs_claims WHERE id = ? AND uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                    preparedStatement.setString(1, id);
                    preparedStatement.setString(2, uuid);
                    preparedStatement.setString(3, owner);
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to delete all player's claims.
     *
     * @param player the player whose claims will be deleted
     * @return true if the operation was successful, false otherwise
     */
    public static boolean deleteAllClaim(Player player) {
        String playerName = player.getName();
        Set<Chunk> chunks = new HashSet<>(getChunksFromOwner(playerName));
        String uuid = player.getUniqueId().toString();
        List<Integer> ids = new ArrayList<>();
        int i = 0;
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        cPlayer.setClaimsCount(0);
        for (Chunk chunk : chunks) {
            if (!listClaims.containsKey(chunk)) continue;
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.deleteMarker(chunk);
            ids.add(Integer.parseInt(claimsId.get(player.getName()).get(chunk)));
            claimsId.get(playerName).remove(chunk);
            if (claimsId.get(playerName).isEmpty()) claimsId.remove(playerName);
            listClaims.remove(chunk);
            if (SimpleClaimSystem.isFolia()) {
                Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.disableBossBar(p);
                    }
                });
            } else {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    ClaimEventsEnterLeave.disableBossBar(p);
                }
            }
            i++;
        }
        String idsString = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
        player.sendMessage(ClaimLanguage.getMessage("territory-delete-radius-success").replaceAll("%number%", String.valueOf(i)));

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                    preparedStatement.setString(1, uuid);
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to delete all player's claims.
     *
     * @param playerName the name of the player whose claims will be deleted
     * @return true if the operation was successful, false otherwise
     */
    public static boolean deleteAllClaim(String playerName) {
        Set<Chunk> chunks = new HashSet<>(getChunksFromOwner(playerName));
        Player player = Bukkit.getPlayer(playerName);
        String uuid = "";
        if (player == null) {
            uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
        } else {
            uuid = player.getUniqueId().toString();
        }
        List<Integer> ids = new ArrayList<>();
        int i = 0;
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        if (cPlayer != null) cPlayer.setClaimsCount(0);
        for (Chunk chunk : chunks) {
            if (!listClaims.containsKey(chunk)) continue;
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.deleteMarker(chunk);
            ids.add(Integer.parseInt(claimsId.get(playerName).get(chunk)));
            claimsId.get(playerName).remove(chunk);
            if (claimsId.get(playerName).isEmpty()) claimsId.remove(playerName);
            listClaims.remove(chunk);
            if (SimpleClaimSystem.isFolia()) {
                Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.disableBossBar(p);
                    }
                });
            } else {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    ClaimEventsEnterLeave.disableBossBar(p);
                }
            }
            i++;
        }
        String idsString = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
        final String uuid_final = uuid;

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                    preparedStatement.setString(1, uuid_final);
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to force the deletion of a claim.
     *
     * @param chunk the chunk representing the claim to be deleted
     * @return true if the operation was successful, false otherwise
     */
    public static boolean forceDeleteClaim(Chunk chunk) {
        if (!listClaims.containsKey(chunk)) return false;

        Claim claim = listClaims.get(chunk);
        String owner = claim.getOwner();
        if (owner.equals("admin")) {
            return deleteClaim(null, chunk);
        }
        OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
        if (player.isOnline()) {
            CPlayer cTarget = CPlayerMain.getCPlayer(owner);
            cTarget.setClaimsCount(cTarget.getClaimsCount() - 1);
        }
        String id = claimsId.get(player.getName()).get(chunk);
        claimsId.get(owner).remove(chunk);
        if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
        listClaims.remove(chunk);

        Runnable task = () -> {
            if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
            if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.deleteMarker(chunk);
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String deleteQuery = "DELETE FROM scs_claims WHERE id = ? AND uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
                    preparedStatement.setString(1, id);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to change player's claim's description.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param description the new description for the claim
     * @return true if the operation was successful, false otherwise
     */
    public static boolean setChunkDescription(Player player, Chunk chunk, String description) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setDescription(description);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, description);
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to change admin claim's description.
     *
     * @param chunk the chunk representing the admin claim
     * @param description the new description for the claim
     * @return true if the operation was successful, false otherwise
     */
    public static boolean setAdminChunkDescription(Chunk chunk, String description) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setDescription(description);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, description);
                    preparedStatement.setString(2, "aucun");
                    preparedStatement.setString(3, "admin");
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to put a claim on sale.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @param price the sale price of the claim
     * @return true if the operation was successful, false otherwise
     */
    public static boolean setChunkSale(Player player, Chunk chunk, double price) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setSale(true);
        claim.setPrice(price);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET isSale = true, SalePrice = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, String.valueOf(price));
                    preparedStatement.setString(2, player.getUniqueId().toString());
                    preparedStatement.setString(3, player.getName());
                    preparedStatement.setString(4, String.valueOf(chunk.getX()));
                    preparedStatement.setString(5, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method to remove a claim from sales.
     *
     * @param player the player who owns the claim
     * @param chunk the chunk representing the claim
     * @return true if the operation was successful, false otherwise
     */
    public static boolean delChunkSale(Player player, Chunk chunk) {
        if (!listClaims.containsKey(chunk)) return false;
        Claim claim = listClaims.get(chunk);
        claim.setSale(false);
        claim.setPrice(0.0);

        Runnable task = () -> {
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String updateQuery = "UPDATE scs_claims SET isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                    preparedStatement.setString(1, player.getUniqueId().toString());
                    preparedStatement.setString(2, player.getName());
                    preparedStatement.setString(3, String.valueOf(chunk.getX()));
                    preparedStatement.setString(4, String.valueOf(chunk.getZ()));
                    preparedStatement.executeUpdate();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        };

        SimpleClaimSystem.executeAsync(task);

        return true;
    }

    /**
     * Method when a claim is sold.
     *
     * @param player the player buying the claim
     * @param chunk the chunk representing the claim
     */
    public static void sellChunk(Player player, Chunk chunk) {
        if (!listClaims.containsKey(chunk)) return;
        Claim claim = listClaims.get(chunk);
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
                String playerName = player.getName();
                String owner = claim.getOwner();
                String claimName = claim.getName();
                double price = claim.getPrice();
                double balance = ClaimVault.getPlayerBalance(playerName);
                if (balance < price) {
                    player.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
                        player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money"));
                    }, null);
                    return;
                }
                ClaimVault.addPlayerBalance(owner, price);
                ClaimVault.removePlayerBalance(playerName, price);

                String uuid = "";
                Player ownerP = Bukkit.getPlayer(owner);
                if (ownerP == null) {
                    OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
                    uuid = ownerOP.getUniqueId().toString();
                } else {
                    CPlayer cOwner = CPlayerMain.getCPlayer(owner);
                    cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
                    uuid = ownerP.getUniqueId().toString();
                }
                CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
                cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
                int nextKey = findFreeId(playerName);
                Map<Chunk, String> newid = new HashMap<>();
                if (claimsId.get(playerName) != null) {
                    newid = new HashMap<>(claimsId.get(playerName));
                }
                newid.put(chunk, String.valueOf(nextKey));
                claimsId.put(playerName, newid);
                claimsId.get(owner).remove(chunk);
                if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
                claim.setOwner(playerName);
                claim.setName("bought-" + claimName + "-" + nextKey);
                Set<String> members = new HashSet<>(claim.getMembers());
                if (!members.contains(playerName)) {
                    members.add(playerName);
                }
                members.remove(owner);
                claim.setMembers(members);
                claim.setSale(false);
                claim.setPrice(0.0);
                String members_string = String.join(";", members);
                if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.updateName(chunk);
                Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.activeBossBar(p, chunk);
                    }
                });
                player.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
                    player.sendMessage(ClaimLanguage.getMessage("buy-claim-success").replaceAll("%name%", claimName).replaceAll("%price%", String.valueOf(price)).replaceAll("%owner%", owner));
                    player.closeInventory();
                }, null);
                if (ownerP != null) {
                    ownerP.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
                        ownerP.sendMessage(ClaimLanguage.getMessage("claim-was-sold").replaceAll("%name%", claimName).replaceAll("%buyer%", playerName).replaceAll("%price%", String.valueOf(price)));
                    }, null);
                }
                try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                    String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, String.valueOf(nextKey));
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, members_string);
                        preparedStatement.setString(5, "bought-" + claimName + "-" + nextKey);
                        preparedStatement.setString(6, uuid);
                        preparedStatement.setString(7, owner);
                        preparedStatement.setString(8, String.valueOf(chunk.getX()));
                        preparedStatement.setString(9, String.valueOf(chunk.getZ()));
                        preparedStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
                String playerName = player.getName();
                String owner = claim.getOwner();
                String claimName = claim.getName();
                double price = claim.getPrice();
                double balance = ClaimVault.getPlayerBalance(playerName);
                if (balance < price) {
                    player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money"));
                    return;
                }
                ClaimVault.addPlayerBalance(owner, price);
                ClaimVault.removePlayerBalance(playerName, price);

                String uuid = "";
                Player ownerP = Bukkit.getPlayer(owner);
                if (ownerP == null) {
                    OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
                    uuid = ownerOP.getUniqueId().toString();
                } else {
                    CPlayer cOwner = CPlayerMain.getCPlayer(owner);
                    cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
                    uuid = ownerP.getUniqueId().toString();
                }
                CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
                cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
                int nextKey = findFreeId(playerName);
                Map<Chunk, String> newid = new HashMap<>();
                if (claimsId.get(playerName) != null) {
                    newid = new HashMap<>(claimsId.get(playerName));
                }
                newid.put(chunk, String.valueOf(nextKey));
                claimsId.put(playerName, newid);
                claimsId.get(owner).remove(chunk);
                if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
                claim.setOwner(playerName);
                claim.setName("bought-" + claimName + "-" + String.valueOf(nextKey));
                Set<String> members = new HashSet<>(claim.getMembers());
                if (!members.contains(playerName)) {
                    members.add(playerName);
                }
                members.remove(owner);
                claim.setMembers(members);
                claim.setSale(false);
                claim.setPrice(0.0);
                String members_string = String.join(";", members);
                if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.updateName(chunk);
                Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), stask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.activeBossBar(p, chunk);
                    }
                    player.sendMessage(ClaimLanguage.getMessage("buy-claim-success").replaceAll("%name%", claimName).replaceAll("%price%", String.valueOf(price)).replaceAll("%owner%", owner));
                    player.closeInventory();
                    if (ownerP != null) {
                        ownerP.sendMessage(ClaimLanguage.getMessage("claim-was-sold").replaceAll("%name%", claimName).replaceAll("%buyer%", playerName).replaceAll("%price%", String.valueOf(price)));
                    }
                });
                try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                    String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, String.valueOf(nextKey));
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, members_string);
                        preparedStatement.setString(5, "bought-" + claimName + "-" + String.valueOf(nextKey));
                        preparedStatement.setString(6, uuid);
                        preparedStatement.setString(7, owner);
                        preparedStatement.setString(8, String.valueOf(chunk.getX()));
                        preparedStatement.setString(9, String.valueOf(chunk.getZ()));
                        preparedStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            });
        }
    }

    /**
     * Method to change the owner of a claim.
     *
     * @param sender the player sending the request
     * @param playerName the name of the new owner
     * @param chunk the chunk representing the claim
     * @param msg whether to send a message to the sender
     */
    public static void setOwner(Player sender, String playerName, Chunk chunk, boolean msg) {
        if (!listClaims.containsKey(chunk)) return;
        Claim claim = listClaims.get(chunk);
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
                String owner = claim.getOwner();
                String uuid = "";
                Player ownerP = Bukkit.getPlayer(owner);
                if (ownerP == null) {
                    OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
                    uuid = ownerOP.getUniqueId().toString();
                } else {
                    CPlayer cOwner = CPlayerMain.getCPlayer(owner);
                    cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
                    uuid = ownerP.getUniqueId().toString();
                }
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (player.isOnline()) {
                    CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
                    cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
                }
                int nextKey = findFreeId(playerName);
                claim.setOwner(playerName);
                if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.updateName(chunk);
                claim.setName("claim-" + nextKey);
                Map<Chunk, String> newid = new HashMap<>();
                if (claimsId.get(playerName) != null) {
                    newid = new HashMap<>(claimsId.get(playerName));
                }
                newid.put(chunk, String.valueOf(nextKey));
                claimsId.put(playerName, newid);
                claimsId.get(owner).remove(chunk);
                if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
                Set<String> members = new HashSet<>(claim.getMembers());
                if (!members.contains(playerName)) {
                    members.add(playerName);
                }
                members.remove(owner);
                claim.setMembers(members);
                String members_string = String.join(";", members);
                Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.activeBossBar(p, chunk);
                    }
                });
                if (msg) {
                    sender.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
                        sender.sendMessage(ClaimLanguage.getMessage("setowner-success").replaceAll("%owner%", playerName));
                    }, null);
                }
                try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                    String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, String.valueOf(nextKey));
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, members_string);
                        preparedStatement.setString(5, "claim-" + nextKey);
                        preparedStatement.setString(6, uuid);
                        preparedStatement.setString(7, owner);
                        preparedStatement.setString(8, String.valueOf(chunk.getX()));
                        preparedStatement.setString(9, String.valueOf(chunk.getZ()));
                        preparedStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
                String owner = claim.getOwner();
                String uuid = "";
                Player ownerP = Bukkit.getPlayer(owner);
                if (ownerP == null) {
                    OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
                    uuid = ownerOP.getUniqueId().toString();
                } else {
                    CPlayer cOwner = CPlayerMain.getCPlayer(owner);
                    cOwner.setClaimsCount(cOwner.getClaimsCount() - 1);
                    uuid = ownerP.getUniqueId().toString();
                }
                OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
                if (player.isOnline()) {
                    CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
                    cTarget.setClaimsCount(cTarget.getClaimsCount() + 1);
                }
                int nextKey = findFreeId(playerName);
                claim.setOwner(playerName);
                if (ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
                if (ClaimSettings.getBooleanSetting("pl3xmap")) ClaimPl3xMap.updateName(chunk);
                claim.setName("claim-" + nextKey);
                Map<Chunk, String> newid = new HashMap<>();
                if (claimsId.get(playerName) != null) {
                    newid = new HashMap<>(claimsId.get(playerName));
                }
                newid.put(chunk, String.valueOf(nextKey));
                claimsId.put(playerName, newid);
                claimsId.get(owner).remove(chunk);
                if (claimsId.get(owner).isEmpty()) claimsId.remove(owner);
                Set<String> members = new HashSet<>(claim.getMembers());
                if (!members.contains(playerName)) {
                    members.add(playerName);
                }
                members.remove(owner);
                claim.setMembers(members);
                String members_string = String.join(";", members);
                Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), stask -> {
                    for (Entity e : chunk.getEntities()) {
                        if (!(e instanceof Player)) continue;
                        Player p = (Player) e;
                        ClaimEventsEnterLeave.activeBossBar(p, chunk);
                    }
                    if (msg) sender.sendMessage(ClaimLanguage.getMessage("setowner-success").replaceAll("%owner%", playerName));
                });
                try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                    String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0 WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, String.valueOf(nextKey));
                        preparedStatement.setString(2, player.getUniqueId().toString());
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, members_string);
                        preparedStatement.setString(5, "claim-" + nextKey);
                        preparedStatement.setString(6, uuid);
                        preparedStatement.setString(7, owner);
                        preparedStatement.setString(8, String.valueOf(chunk.getX()));
                        preparedStatement.setString(9, String.valueOf(chunk.getZ()));
                        preparedStatement.executeUpdate();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                return;
            });
        }
    }

    /**
     * Method to display a chunk when claiming.
     *
     * @param player the player claiming the chunk
     * @param chunk the chunk to be displayed
     * @param claim whether the chunk is being claimed or not
     */
    public static void displayChunk(Player player, Chunk chunk, boolean claim) {
        Particle.DustOptions dustOptions;
        if (!claim) {
            if (checkIfClaimExists(chunk)) {
                String playerName = player.getName();
                if (getOwnerInClaim(chunk).equals(playerName)) {
                    dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);
                } else {
                    dustOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 1.5f);
                }
            } else {
                dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.5f);
            }
        } else {
            dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);
        }
        if (SimpleClaimSystem.isFolia()) {
            final int[] counter = {0};
            Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
                if (counter[0] >= 10) {
                    task.cancel();
                }
                World world = player.getWorld();
                int xStart = chunk.getX() << 4;
                int zStart = chunk.getZ() << 4;
                int xEnd = xStart + 15;
                int zEnd = zStart + 15;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;

                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x <= xEnd; x++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd + 1), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z <= zEnd; z++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd + 1, y, z), 1, 0, 0, 0, 0, dustOptions);
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
                int xStart = chunk.getX() << 4;
                int zStart = chunk.getZ() << 4;
                int xEnd = xStart + 15;
                int zEnd = zStart + 15;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;
                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x <= xEnd; x++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd + 1), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z <= zEnd; z++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd + 1, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }
                counter++;
            }
        }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 10L);
    }

    /**
     * Method to display a chunk when radius claiming.
     *
     * @param player the player claiming the chunk
     * @param centralChunk the central chunk to be displayed
     * @param radius the radius around the central chunk to be displayed
     */
    public static void displayChunkBorderWithRadius(Player player, Chunk centralChunk, int radius) {
        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 1.5f);
        if (SimpleClaimSystem.isFolia()) {
            final int[] counter = {0};
            Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
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
                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z < zEnd; z++) {
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
                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z < zEnd; z++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }

                counter++;
            }
        }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 10L);
    }

    /**
     * Method to send help for commands.
     *
     * @param player the player requesting help
     * @param help the help message
     * @param cmd the command for which help is requested
     */
    public static void getHelp(Player player, String help, String cmd) {
        String help_msg = ClaimLanguage.getMessage("help-command." + cmd + "-" + help.toLowerCase());
        if (!help_msg.isEmpty()) {
            player.sendMessage(ClaimLanguage.getMessage("help-separator"));
            player.sendMessage(help_msg);
            player.sendMessage(ClaimLanguage.getMessage("help-separator"));
            return;
        }
        player.sendMessage(ClaimLanguage.getMessage("sub-arg-not-found").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgs)));
    }

    /**
     * Method to get the direction (north, south, east or west).
     *
     * @param yaw the yaw angle
     * @return the direction as a string
     */
    private static String getDirection(float yaw) {
        yaw = yaw % 360;
        if (yaw < 0) yaw += 360;
        if (0 <= yaw && yaw < 45) return ClaimLanguage.getMessage("map-direction-south");
        else if (45 <= yaw && yaw < 135) return ClaimLanguage.getMessage("map-direction-west");
        else if (135 <= yaw && yaw < 225) return ClaimLanguage.getMessage("map-direction-north");
        else if (225 <= yaw && yaw < 315) return ClaimLanguage.getMessage("map-direction-east");
        else if (315 <= yaw && yaw < 360.0) return ClaimLanguage.getMessage("map-direction-south");
        else return "Unknown";
    }

    /**
     * Method to get the map for a player.
     *
     * @param player the player requesting the map
     * @param to the chunk to be displayed on the map
     */
    public static void getMap(Player player, Chunk to) {
        SimpleClaimSystem.executeAsync(() -> {
            String direction = getDirection(player.getLocation().getYaw());
            Chunk centerChunk = to;
            int centerX = centerChunk.getX();
            int centerZ = centerChunk.getZ();
            boolean isClaimed = checkIfClaimExists(centerChunk);
            
            String name = isClaimed 
                ? ClaimLanguage.getMessage("map-actual-claim-name-message").replaceAll("%name%", getClaimNameByChunk(centerChunk)) 
                : ClaimLanguage.getMessage("map-no-claim-name-message");
            String coords = ClaimLanguage.getMessage("map-coords-message").replaceAll("%coords%", centerX + "," + centerZ).replaceAll("%direction%", direction);
            String colorRelationNoClaim = ClaimLanguage.getMessage("map-no-claim-color");
            String colorCursor = ClaimLanguage.getMessage("map-cursor-color");
            String symbolNoClaim = ClaimLanguage.getMessage("map-symbol-no-claim");
            String symbolClaim = ClaimLanguage.getMessage("map-symbol-claim");
            String mapCursor = ClaimLanguage.getMessage("map-cursor");
            World world = player.getWorld();

            StringBuilder mapMessage = new StringBuilder(colorRelationNoClaim);
            Function<Chunk, String> getChunkSymbol = chunk -> chunk.equals(centerChunk) 
                ? colorCursor + mapCursor + colorRelationNoClaim
                : checkIfClaimExists(chunk) 
                    ? getRelation(player, chunk) + symbolClaim + colorRelationNoClaim
                    : colorRelationNoClaim + symbolNoClaim;

            Map<Integer, String> legendMap = new HashMap<>();
            legendMap.put(-3, "  " + name + (isClaimed ? " " + ClaimLanguage.getMessage("map-actual-claim-name-message-owner").replaceAll("%owner%", getOwnerInClaim(centerChunk)) : ""));
            legendMap.put(-2, "  " + coords);
            legendMap.put(0, "  " + ClaimLanguage.getMessage("map-legend-you").replaceAll("%cursor-color%", colorCursor));
            legendMap.put(1, "  " + ClaimLanguage.getMessage("map-legend-free").replaceAll("%no-claim-color%", colorRelationNoClaim));
            legendMap.put(2, "  " + ClaimLanguage.getMessage("map-legend-yours").replaceAll("%claim-relation-member%", ClaimLanguage.getMessage("map-claim-relation-member")));
            legendMap.put(3, "  " + ClaimLanguage.getMessage("map-legend-other").replaceAll("%claim-relation-visitor%", ClaimLanguage.getMessage("map-claim-relation-visitor")));

            IntStream.rangeClosed(-4, 4).forEach(dz -> {
                IntStream.rangeClosed(-10, 10).forEach(dx -> {
                    int[] offset = adjustDirection(dx, dz, direction);
                    Chunk chunk = world.getChunkAt(centerX + offset[0], centerZ + offset[1]);
                    mapMessage.append(getChunkSymbol.apply(chunk));
                });
                if (legendMap.containsKey(dz)) {
                    mapMessage.append(legendMap.get(dz));
                }
                mapMessage.append("\n");
            });

            SimpleClaimSystem.executeSync(() -> player.sendMessage(mapMessage.toString()));
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
    private static int[] adjustDirection(int dx, int dz, String direction) {
        int relX = dx, relZ = dz;
        if (direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-north"))) return new int[]{relX, relZ};
        if (direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-south"))) {
            relX = -dx;
            relZ = -dz;
            return new int[]{relX, relZ};
        }
        if (direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-east"))) {
            relX = -dz;
            relZ = dx;
            return new int[]{relX, relZ};
        }
        if (direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-west"))) {
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
    public static String getRelation(Player player, Chunk chunk) {
    	return ClaimMain.checkMembre(chunk, player) ? ClaimLanguage.getMessage("map-claim-relation-member") : ClaimLanguage.getMessage("map-claim-relation-visitor");
    }

    /**
     * Method to update the weather in the chunk.
     *
     * @param chunk the chunk to be updated
     * @param result the new weather state
     */
    public static void updateWeatherChunk(Chunk chunk, boolean result) {
        Runnable task = () -> {
            if (result) {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    p.resetPlayerWeather();
                }
            } else {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    p.setPlayerWeather(WeatherType.CLEAR);
                }
            }
        };
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> task.run());
        } else {
            task.run();
        }
    }

    /**
     * Method to update the fly in the chunk.
     *
     * @param chunk the chunk to be updated
     * @param result the new fly state
     */
    public static void updateFlyChunk(Chunk chunk, boolean result) {
        Runnable task = () -> {
            if (result) {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    CPlayer cPlayer = CPlayerMain.getCPlayer(p.getName());
                    if (cPlayer.getClaimAutofly()) {
                        CPlayerMain.activePlayerFly(p);
                    }
                }
            } else {
                for (Entity e : chunk.getEntities()) {
                    if (!(e instanceof Player)) continue;
                    Player p = (Player) e;
                    CPlayer cPlayer = CPlayerMain.getCPlayer(p.getName());
                    if (cPlayer.getClaimFly()) {
                        CPlayerMain.removePlayerFly(p);
                    }
                }
            }
        };
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> task.run());
        } else {
            task.run();
        }
    }
}