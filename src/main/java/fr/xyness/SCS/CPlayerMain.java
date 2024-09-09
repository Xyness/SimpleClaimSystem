package fr.xyness.SCS;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.xyness.SCS.API.SimpleClaimSystemAPI;

/**
 * This class handles CPlayer management and methods
 */
public class CPlayerMain {
    
	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** A map of player uuid to CPlayer instances */
    private Map<UUID, CPlayer> players = new HashMap<>();
    
    /** A map of player uuid to players name instances */
    private Map<UUID, String> playersName = new HashMap<>();
    
    /** A map of players name to players uuid instances */
    private Map<String, UUID> playersUUID = new HashMap<>();
    
    /** A set of players in DB */
    private Set<UUID> playersRegistered = new HashSet<>();
    
    /** A map of player names to their configuration settings */
    private Map<UUID, Map<String, Double>> playersConfigSettings = new HashMap<>();
    
    /** Map of ItemStacks for players head */
    private ConcurrentHashMap<String,ItemStack> playersHead = new ConcurrentHashMap<>();
    
    /** Map of players head hashed texture */
    private ConcurrentHashMap<String,String> playersHashedTexture = new ConcurrentHashMap<>();
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    /** Link of the mojang API */
    private final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    
    /** Link of the second mojang API */
    private final String MOJANG_API_URL_2 = "https://api.mojang.com/profiles/minecraft";
    
    /** Link of the mojang profile API */
    private final String MOJANG_PROFILE_API_URL = "https://sessionserver.mojang.com/session/minecraft/profile/";
    
    /** Defines the rate limit for requests in milliseconds */
    private static final int RATE_LIMIT = 50;

    /** Schedules tasks to run after a specified delay using a single-threaded executor */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /** Tracks the number of requests sent to calculate the scheduling delay for the next request */
    private int requestCount = 0;
    
    /** Pattern for matching claim permissions */
    public static final Pattern CLAIM_PATTERN = Pattern.compile("scs\\.claim\\.(\\d+)");
    
    /** Pattern for matching radius permissions */
    public static final Pattern RADIUS_PATTERN = Pattern.compile("scs\\.radius\\.(\\d+)");
    
    /** Pattern for matching delay permissions */
    public static final Pattern DELAY_PATTERN = Pattern.compile("scs\\.delay\\.(\\d+)");
    
    /** Pattern for matching cost permissions */
    public static final Pattern COST_PATTERN = Pattern.compile("scs\\.cost\\.(\\d+)");
    
    /** Pattern for matching multiplier permissions */
    public static final Pattern MULTIPLIER_PATTERN = Pattern.compile("scs\\.multiplier\\.(\\d+)");
    
    /** Pattern for matching member permissions */
    public static final Pattern MEMBERS_PATTERN = Pattern.compile("scs\\.members\\.(\\d+)");
    
    /** Pattern for matching chunks permissions */
    public static final Pattern CHUNKS_PATTERN = Pattern.compile("scs\\.chunks\\.(\\d+)");
    
    /** Pattern for matching distance permissions */
    public static final Pattern DISTANCE_PATTERN = Pattern.compile("scs\\.distance\\.(\\d+)");
    
    /** Pattern for matching chunks total permissions */
    public static final Pattern CHUNKS_TOTAL_PATTERN = Pattern.compile("scs\\.chunks-total\\.(\\d+)");
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for CPlayerMain
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public CPlayerMain(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    
    
    /**
     * Clears all maps and variables.
     */
    public void clearAll() {
        players.clear();
        playersConfigSettings.clear();
    }

    /**
     * Load owner player.
     */
    public void loadOwners(Map<String, String> owners) {

        // Return if map is empty
        if (owners.isEmpty()) return;

        // Get Mojang UUID for players
        Map<String, String> uuids = getUUIDsFromMojang(new ArrayList<>(owners.keySet()));
        if (uuids.keySet().size() != owners.size()) {
            owners.keySet().forEach(o -> {
                if (!uuids.containsKey(o)) {
                    uuids.put(o, null);
                }
            });
        }

        // Add players
        for (Map.Entry<String, String> entry : uuids.entrySet()) {
            String name = entry.getKey();
            String uuid_mojang = entry.getValue();
            String uuid_server = owners.get(name);
            
            UUID uuid_server_real = UUID.fromString(uuid_server);
            playersName.put(uuid_server_real, name);
            playersUUID.put(name, uuid_server_real);

            if (uuid_server != null) {

                getSkinURL(uuid_mojang).thenAccept(textures -> {
                    ItemStack playerHead;
                    if (uuid_mojang != null && textures != null) {
                        playerHead = createPlayerHeadWithTexture(uuid_mojang, textures);
                    } else {
                        playerHead = new ItemStack(Material.PLAYER_HEAD);
                        playersHashedTexture.put(name, "none");
                    }
                    playersHead.put(name, playerHead == null ? new ItemStack(Material.PLAYER_HEAD) : playerHead);
                    playersHashedTexture.put(name, textures == null ? "none" : textures);
                }).exceptionally(ex -> {
                    return null;
                });
            } else {
            	playersHashedTexture.put(name, "none");
                playersHead.put(name, new ItemStack(Material.PLAYER_HEAD));
            }
        }
       
    }
    
    /**
     * Checks if the player's data has changed.
     *
     * @param player The player to check and update claims for.
     */
    public void checkPlayer(Player player) {
        instance.executeAsync(() -> {
            UUID uuid = player.getUniqueId();
            String playerName = player.getName();
            String oldName = playersName.get(uuid);

            // Check if the player is registered
            if (!playersRegistered.contains(uuid) || oldName == null) {
            	playersRegistered.add(uuid);
                playersName.put(uuid, playerName);
                playersUUID.put(playerName, uuid);

                String uuid_mojang = getUUIDFromMojang(playerName);

                String textures = getSkinURLWithoutDelay(uuid_mojang);
                ItemStack playerHead = createPlayerHeadWithTexture(uuid_mojang, textures);
                playersHead.put(playerName, playerHead);
                playersHashedTexture.put(playerName, textures == null ? "none" : textures);

                // Update database
                try (Connection connection = instance.getDataSource().getConnection()) {
                    String dbProductName = connection.getMetaData().getDatabaseProductName().toLowerCase();
                    String updateQuery;

                    if (dbProductName.contains("sqlite")) {
                        updateQuery = "INSERT INTO scs_players(uuid_server, uuid_mojang, player_name, player_head, player_textures) VALUES(?, ?, ?, ?, ?) ON CONFLICT(uuid_server) DO UPDATE SET player_name = excluded.player_name";
                    } else if (dbProductName.contains("mysql")) {
                        updateQuery = "INSERT INTO scs_players(uuid_server, uuid_mojang, player_name, player_head, player_textures) VALUES(?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)";
                    } else {
                        throw new UnsupportedOperationException("Unsupported database: " + dbProductName);
                    }

                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, uuid.toString());
                        preparedStatement.setString(2, uuid_mojang == null ? "none" : uuid_mojang);
                        preparedStatement.setString(3, playerName);
                        preparedStatement.setString(4, "");
                        preparedStatement.setString(5, textures == null ? "none" : textures);
                        preparedStatement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }

                // Log this
                instance.getLogger().info(playerName + " is now registered (" + uuid.toString() + ").");
                return;
            }

            // Check if the player has changed name (premium players)
            if (!oldName.equals(playerName)) {
            	
                // Log this
                instance.getLogger().info(oldName + " changed his name to " + playerName + " (" + uuid.toString() + "), new name saved.");
                playersUUID.remove(oldName);
                playersName.put(uuid, playerName);
                playersUUID.put(playerName, uuid);

                Set<Claim> claims = instance.getMain().getPlayerClaims(uuid);
                claims.forEach(c -> {
                    c.setOwner(playerName);
                    instance.getBossBars().activateBossBar(c.getChunks());
                });
                instance.getMain().setPlayerClaims(uuid, claims);
                
                try (Connection connection = instance.getDataSource().getConnection()) {

	                // Update database
	                String updateQuery = "UPDATE scs_players SET player_name = ? WHERE uuid_server = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, playerName);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.executeUpdate();
	                } catch (SQLException e) {
	                    e.printStackTrace();
	                }
	
	                // Update database
	                updateQuery = "UPDATE scs_claims SET owner_name = ? WHERE owner_uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
	                    preparedStatement.setString(1, playerName);
	                    preparedStatement.setString(2, uuid.toString());
	                    preparedStatement.executeUpdate();
	                } catch (SQLException e) {
	                    e.printStackTrace();
	                }
	                
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }

            // Refresh player head texture
            String uuid_mojang = getUUIDFromMojang(playerName);
            if (uuid_mojang != null) {
                String textures = getSkinURLWithoutDelay(uuid_mojang);
                if (textures == null) return;
                
                // Check if the texture is the same
                if (textures.equals(playersHashedTexture.getOrDefault(playerName, ""))) return;

                // Log this
                instance.getLogger().info(playerName + " changed his skin (" + uuid.toString() + "), new textures saved.");

                ItemStack head = createPlayerHeadWithTexture(uuid_mojang, textures);
                playersHead.put(playerName, head);
                playersHashedTexture.put(playerName, textures);
                
                try (Connection connection = instance.getDataSource().getConnection()) {

                    // Update database
                    String updateQuery = "UPDATE scs_players SET player_textures = ? WHERE uuid_server = ?";
                    try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
                        preparedStatement.setString(1, textures);
                        preparedStatement.setString(2, uuid.toString());
                        preparedStatement.executeUpdate();
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                    
                } catch (SQLException e) {
                    e.printStackTrace();
                }

            } else if (!playersHead.containsKey(playerName)) {
                playersHead.put(playerName, new ItemStack(Material.PLAYER_HEAD));
            }

        });
    }
    
    /**
     * Loads player data from Bukkit and inserts it into the database.
     * If a player already exists in the database, their name is updated.
     */
    public void loadPlayers() {
        instance.info(" ");
        instance.info(net.md_5.bungee.api.ChatColor.DARK_GREEN + "Loading players..");
        int i = 0;

        try (Connection connection = instance.getDataSource().getConnection()) {
        	
            String getQuery = "SELECT * FROM scs_players";
            try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery)) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                    	UUID uuid = UUID.fromString(resultSet.getString("uuid_server"));
                    	String uuid_mojang = resultSet.getString("uuid_mojang");
                    	String playerName = resultSet.getString("player_name");
                    	String textures = resultSet.getString("player_textures");
                        ItemStack playerHead = createPlayerHeadWithTexture(uuid_mojang,textures);
                        playersHead.put(playerName, playerHead);
                    	playersHashedTexture.put(playerName, textures);
                    	playersName.put(uuid, playerName);
                    	playersUUID.put(playerName, uuid);
                    	playersRegistered.add(uuid);
                    	i++;
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        instance.info(instance.getMain().getNumberSeparate(String.valueOf(i)) + " players loaded.");
    }

    /**
     * Get or create a player head with the correct texture.
     *
     * @param player The OfflinePlayer object.
     * @return The ItemStack representing the player's head.
     */
    public ItemStack getPlayerHead(String playerName) {
        return playersHead.computeIfAbsent(playerName, p -> new ItemStack(Material.PLAYER_HEAD));
    }
    
    /**
     * Adds dashes to a UUID string if they are missing.
     *
     * @param uuid The UUID string without dashes.
     * @return The UUID string with dashes.
     */
    private String addDashesToUUID(String uuid) {
        if (uuid.length() == 32) {
            return uuid.replaceFirst(
                    "([0-9a-fA-F]{8})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{4})([0-9a-fA-F]{12})",
                    "$1-$2-$3-$4-$5"
            );
        }
        return uuid;
    }
    
    /**
     * Creates an ItemStack of a player head with the specified texture.
     *
     * @param uuid The UUID of the player.
     * @param texture The texture of the player's head
     * @return An ItemStack representing the player's head with the applied texture.
     */
    public ItemStack createPlayerHeadWithTexture(String uuid, String texture) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null && uuid != null && !uuid.isBlank() && texture != null && !texture.isBlank() && !texture.equals("none") && !uuid.equals("none")) {
            PlayerProfile profile = Bukkit.createPlayerProfile(UUID.fromString(uuid));
            if(texture != null) {
                try {
                    URL url = new URL(texture);
                    PlayerTextures textures = profile.getTextures();
                    textures.setSkin(url);
                    profile.setTextures(textures);
                    meta.setOwnerProfile(profile);
                    head.setItemMeta(meta);
                } catch (MalformedURLException e) {
                    return head;
                }
            }
        }
        return head;
    }
    
    /**
     * Retrieves the URL of a Minecraft player's skin texture from Mojang using the player's UUID.
     *
     * @param uuid The UUID of the player whose skin texture URL is to be retrieved.
     * @return A CompletableFuture that resolves to a string representing the URL of the player's skin texture, or null if an error occurs or the texture is not found.
     */
    public CompletableFuture<String> getSkinURL(String uuid) {
        CompletableFuture<String> future = new CompletableFuture<>();
        scheduler.schedule(() -> {
            try {
                URL url = new URL(MOJANG_PROFILE_API_URL + uuid);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                    JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject properties = response.getAsJsonArray("properties").get(0).getAsJsonObject();
                    String value = properties.get("value").getAsString();
                    String decodedValue = new String(Base64.getDecoder().decode(value));
                    JsonObject textureProperty = JsonParser.parseString(decodedValue).getAsJsonObject();
                    future.complete(textureProperty.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString());
                }
                
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, requestCount++ * RATE_LIMIT, TimeUnit.MILLISECONDS);
        return future;
    }
    
    /**
     * Retrieves the URL of a Minecraft player's skin texture from Mojang using the player's UUID.
     *
     * @param uuid The UUID of the player whose skin texture URL is to be retrieved.
     * @return A String representing the URL of the player's skin texture, or null if an error occurs or the texture is not found.
     */
    public String getSkinURLWithoutDelay(String uuid) {
        try {
            URL url = new URL(MOJANG_PROFILE_API_URL + uuid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                InputStreamReader reader = new InputStreamReader(connection.getInputStream());
                JsonObject response = JsonParser.parseReader(reader).getAsJsonObject();
                JsonObject properties = response.getAsJsonArray("properties").get(0).getAsJsonObject();
                String value = properties.get("value").getAsString();
                String decodedValue = new String(Base64.getDecoder().decode(value));
                JsonObject textureProperty = JsonParser.parseString(decodedValue).getAsJsonObject();
                return textureProperty.getAsJsonObject("textures").getAsJsonObject("SKIN").get("url").getAsString();
            }
            
            return null;
            
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Retrieves the URLs of Minecraft players' skin textures from Mineskin using their UUIDs.
     *
     * @param uuids The UUIDs of the players whose skin texture URLs are to be retrieved.
     * @return A CompletableFuture that resolves to a map of UUIDs to their corresponding skin texture URLs.
     */
    public CompletableFuture<Map<String, String>> getSkinURLs(List<String> uuids) {
        List<CompletableFuture<Map.Entry<String, String>>> futures = uuids.stream()
                .map(uuid -> getSkinURL(uuid).thenApply(url -> Map.entry(uuid, url)))
                .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue)));
    }

    /**
     * Retrieves the UUID of a player from Mojang's API using the player's name.
     *
     * @param playerName The name of the player.
     * @return The UUID of the player as a string, or null if an error occurs.
     */
    private String getUUIDFromMojang(String playerName) {
        try {
            URL url = new URL(MOJANG_API_URL + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject responseJson = JsonParser.parseReader(reader).getAsJsonObject();
                    return addDashesToUUID(responseJson.get("id").getAsString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Retrieves the UUIDs of players from Mojang's API using a list of player names.
     *
     * @param playerNames The list of player names.
     * @return A map of player names to their UUIDs.
     */
    public Map<String, String> getUUIDsFromMojang(List<String> playerNames) {
        Map<String, String> result = new HashMap<>();
        List<List<String>> batches = splitList(playerNames, 10);

        for (List<String> batch : batches) {
            result.putAll(getUUIDsFromMojangBatch(batch));
        }

        return result;
    }
    
    /**
     * Retrieves the UUIDs of players from Mojang's API using a list of player names.
     *
     * @param playerNames The list of player names.
     * @return A map of player names to their UUIDs.
     */
    public Map<String, String> getUUIDsFromMojangBatch(List<String> playerNames) {
        Map<String, String> result = new HashMap<>();
        try {
            URL url = new URL(MOJANG_API_URL_2);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            // Create the JSON array for the request
            JsonArray jsonArray = new JsonArray();
            for (String playerName : playerNames) {
                jsonArray.add(playerName);
            }

            // Send the request
            try (OutputStream os = connection.getOutputStream()) {
                os.write(jsonArray.toString().getBytes("utf-8"));
            }

            // Process the response
            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonArray responseJson = JsonParser.parseReader(reader).getAsJsonArray();
                    for (int i = 0; i < responseJson.size(); i++) {
                        JsonObject playerData = responseJson.get(i).getAsJsonObject();
                        String id = playerData.get("id").getAsString();
                        String name = playerNames.get(i);
                        result.put(name, addDashesToUUID(id));
                    }
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }
    
    /**
     * Splits a list into smaller sublists of a specified maximum size.
     *
     * @param list The list to split.
     * @param size The maximum size of each sublist.
     * @return A list of sublists.
     */
    private <T> List<List<T>> splitList(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return result;
    }
    
    /**
     * Removes the CPlayer instance associated with the given player uuid.
     * 
     * @param targetUUID The uuid of the player
     */
    public void removeCPlayer(UUID targetUUID) {
        players.remove(targetUUID);
    }
    
    /**
     * Gets the CPlayer instance associated with the given player uuid.
     * 
     * @param targetUUID The uuid of the player
     * @return The CPlayer instance, or null if not found
     */
    public CPlayer getCPlayer(UUID targetUUID) {
        return players.get(targetUUID);
    }
    
    /**
     * Gets the player name associated with the given player uuid.
     * 
     * @param targetUUID The uuid of the player
     * @return The player name
     */
    public String getPlayerName(UUID targetUUID) {
        String name = playersName.get(targetUUID);
        return name == null ? Bukkit.getOfflinePlayer(targetUUID).getName() : name;
    }
    
    /**
     * Gets the player uuid associated with the given player name
     * 
     * @param targetName The name of the player
     * @return The player uuid
     */
    public UUID getPlayerUUID(String targetName) {
    	UUID uuid = playersUUID.get(targetName);
    	return uuid == null ? Bukkit.getOfflinePlayer(targetName).getUniqueId() : uuid;
    }
    
    /**
     * Sets the configuration settings for all players.
     * 
     * @param p A map of player names to their configuration settings
     */
    public void setPlayersConfigSettings(Map<UUID, Map<String, Double>> p) {
        playersConfigSettings = p;
    }
    
    /**
     * Update a player setting ("players" in config.yml)
     * 
     * @param playerId The UUID of player
     * @param key The key of the setting
     * @param value The value of the setting
     */
    public void updatePlayerConfigSettings(UUID playerId, String key, Double value) {
    	playersConfigSettings.computeIfAbsent(playerId, k -> new HashMap<>()).put(key, value);
    }
    
    /**
     * Checks if a player can add a member to their claim.
     * 
     * @param player The player
     * @param chunk The chunk
     * @return True if the player can add a member, false otherwise
     */
    public boolean canAddMember(Player player, Claim claim) {
        if (player.hasPermission("scs.admin")) return true;
        CPlayer cPlayer = players.get(player.getUniqueId());
        int i = claim.getMembers().size();
        int nb_members = cPlayer.getMaxMembers();
        return nb_members == 0 || nb_members > i;
    }
    
    /**
     * Checks if a player has a specific permission.
     * 
     * @param player The player
     * @param perm The permission to check
     * @return True if the player has the permission, false otherwise
     */
    public boolean checkPermPlayer(Player player, String perm) {
    	return player.hasPermission("scs.admin") ? true : player.hasPermission(perm);
    }
    
    /**
     * Activates fly mode for the player.
     * 
     * @param player The player
     */
    public void activePlayerFly(Player player) {
        CPlayer cPlayer = players.get(player.getUniqueId());
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        player.setFlying(true);
        cPlayer.setClaimFly(true);
    }
    
    /**
     * Removes fly mode from the player.
     * 
     * @param player The player
     */
    public void removePlayerFly(Player player) {
        CPlayer cPlayer = players.get(player.getUniqueId());
        if (cPlayer.getClaimFly()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            cPlayer.setClaimFly(false);
        }
    }
    
    /**
     * Returns the player config from "players" section in config.yml
     * 
     * @param uuid The target uuid
     * @return The player config
     */
    public Map<String,Double> getPlayerConfig(UUID uuid){
    	return playersConfigSettings.get(uuid);
    }
    
    /**
     * Sets the permissions of a player when he joins the server.
     * 
     * @param player The player
     */
    public void addPlayerPermSetting(Player player) {
        instance.executeAsync(() -> {
        	UUID playerId = player.getUniqueId();
            players.put(playerId, new CPlayer(player, playerId, instance.getMain().getPlayerClaimsCount(playerId),instance));
        });
    }
}
