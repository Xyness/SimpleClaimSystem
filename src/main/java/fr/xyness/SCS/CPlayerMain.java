package fr.xyness.SCS;

import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;

/**
 * This class handles CPlayer management and methods
 */
public class CPlayerMain {
    
	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** A map of player names to CPlayer instances */
    private Map<String, CPlayer> players = new HashMap<>();
    
    /** A map of player names to their configuration settings */
    private Map<String, Map<String, Double>> playersConfigSettings = new HashMap<>();
    
    /** Set of offline players */
    private Map<String,OfflinePlayer> offlinePlayers = new HashMap<>();
    
    /** Set of ItemStacks for players head */
    private Map<OfflinePlayer,ItemStack> playersHead = new HashMap<>();
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    /** Link of the mojang API */
    private final String MOJANG_API_URL = "https://api.mojang.com/users/profiles/minecraft/";
    
    /** Link of the mineskin API */
    private final String MINESKIN_API_URL = "https://api.mineskin.org/generate/user/";
    
    
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
    
    
    // ********************
    // *  Others Methods  *
    // ********************
    
    
    /**
     * Clears all maps and variables.
     */
    public void clearAll() {
        players.clear();
        playersConfigSettings.clear();
        offlinePlayers.clear();
    }
    
    /**
     * Get the offline player by his name.
     *
     * @param playerName The player name.
     * @return The OfflinePlayer object.
     */
    public OfflinePlayer getOfflinePlayer(String playerName) {
    	return offlinePlayers.get(playerName);
    }
    
    /**
     * Load all offline players.
     */
    public void loadOfflinePlayers() {
    	instance.executeAsync(() -> {
	        Arrays.asList(Bukkit.getOfflinePlayers()).forEach(p -> {
	            offlinePlayers.put(p.getName(), p);
	            String uuid = getUUIDFromMojang(p.getName());
	            if (uuid != null) {
	                String texture = getTextureFromMineskin(uuid);
	                if (texture != null) {
	                    ItemStack head = createPlayerHeadWithTexture(texture,p);
	                    playersHead.put(p, head);
	                } else {
	    	        	ItemStack head = new ItemStack(Material.PLAYER_HEAD);
	    	        	playersHead.put(p, head);
	                }
	            } else {
		        	ItemStack head = new ItemStack(Material.PLAYER_HEAD);
		        	playersHead.put(p, head);
	            }
	        });
    	});
    }
    
    /**
     * Refresh the player head
     * 
     * @param player The target player
     */
    public void refreshPlayerHead(Player player) {
    	instance.executeAsync(() -> {
        	String playerName = player.getName();
        	if(!offlinePlayers.containsKey(playerName)) {
        		offlinePlayers.put(playerName, (OfflinePlayer) player);
        	}
        	String uuid = getUUIDFromMojang(playerName);
            if (uuid != null) {
                String texture = getTextureFromMineskin(uuid);
                if (texture != null) {
                    ItemStack head = createPlayerHeadWithTexture(texture,player);
                    playersHead.put(player, head);
                } else {
    	        	ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    	        	playersHead.put(player, head);
                }
            } else {
            	ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            	playersHead.put(player, head);
            }
    	});
    }

    /**
     * Get or create a player head with the correct texture.
     *
     * @param player The OfflinePlayer object.
     * @return The ItemStack representing the player's head.
     */
    public ItemStack getPlayerHead(OfflinePlayer player) {
        if (playersHead.containsKey(player)) {
            return playersHead.get(player);
        }
        String uuid = getUUIDFromMojang(player.getName());
        if (uuid != null) {
	        String texture = getTextureFromMineskin(uuid);
	        if (texture != null) {
	            ItemStack head = createPlayerHeadWithTexture(texture,player);
	            playersHead.put(player, head);
	            return head;
	        } else {
	        	ItemStack head = new ItemStack(Material.PLAYER_HEAD);
	        	playersHead.put(player, head);
	            return head;
	        }
        } else {
        	ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        	playersHead.put(player, head);
            return head;
        }
    }

    /**
     * Retrieves the texture value from Mineskin using the player's UUID.
     *
     * @param uuid The UUID of the player.
     * @return The texture value as a string, or null if an error occurs.
     */
    private String getTextureFromMineskin(String uuid) {
        try {
            URL url = new URL(MINESKIN_API_URL + uuid);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            if (connection.getResponseCode() == 200) {
                try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                    JsonObject responseJson = JsonParser.parseReader(reader).getAsJsonObject();
                    JsonObject data = responseJson.getAsJsonObject("data");
                    if (data != null) {
                        JsonObject texture = data.getAsJsonObject("texture");
                        if (texture != null) {
                            return texture.get("value").getAsString();
                        }
                    }
                }
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    /**
     * Creates an ItemStack of a player head with the specified texture.
     *
     * @param texture The texture value to apply to the player head.
     * @param player The OfflinePlayer for whom the head is being created.
     * @return An ItemStack representing the player's head with the applied texture.
     */
    private ItemStack createPlayerHeadWithTexture(String texture, OfflinePlayer player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        if (meta != null && texture != null) {
            GameProfile profile = new GameProfile(UUID.randomUUID(), player.getName());
            profile.getProperties().put("textures", new Property("textures", texture));

            try {
                Field profileField = meta.getClass().getDeclaredField("profile");
                profileField.setAccessible(true);
                profileField.set(meta, profile);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }

            head.setItemMeta(meta);
        }

        return head;
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
                    return responseJson.get("id").getAsString();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    
    /**
     * Removes the CPlayer instance associated with the given player name.
     * 
     * @param playerName The name of the player
     */
    public void removeCPlayer(String playerName) {
        players.remove(playerName);
    }
    
    /**
     * Gets the CPlayer instance associated with the given player name.
     * 
     * @param playerName The name of the player
     * @return The CPlayer instance, or null if not found
     */
    public CPlayer getCPlayer(String playerName) {
        return players.get(playerName);
    }
    
    /**
     * Sets the configuration settings for all players.
     * 
     * @param p A map of player names to their configuration settings
     */
    public void setPlayersConfigSettings(Map<String, Map<String, Double>> p) {
        playersConfigSettings = p;
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
        CPlayer cPlayer = players.get(player.getName());
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
        CPlayer cPlayer = players.get(player.getName());
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
        CPlayer cPlayer = players.get(player.getName());
        if (cPlayer.getClaimFly()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            cPlayer.setClaimFly(false);
        }
    }
    
    /**
     * Sets the permissions of a player when he joins the server.
     * 
     * @param player The player
     */
    public void addPlayerPermSetting(Player player) {
        instance.executeAsync(() -> {
        	String playerName = player.getName();
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            Map<String, Double> groupPlayerSettings = new HashMap<>();
            groupPlayerSettings.put("max-claims", groupsSettings.get("default").get("max-claims"));
            groupPlayerSettings.put("max-radius-claims", groupsSettings.get("default").get("max-radius-claims"));
            groupPlayerSettings.put("teleportation-delay", groupsSettings.get("default").get("teleportation-delay"));
            groupPlayerSettings.put("max-members", groupsSettings.get("default").get("max-members"));
            groupPlayerSettings.put("claim-cost", groupsSettings.get("default").get("claim-cost"));
            groupPlayerSettings.put("claim-cost-multiplier", groupsSettings.get("default").get("claim-cost-multiplier"));
            groupPlayerSettings.put("max-chunks-per-claim", groupsSettings.get("default").get("max-chunks-per-claim"));
            for (String group : groups.keySet()) {
                if (checkPermPlayer(player, groups.get(group))) {
                    groupPlayerSettings.put("max-claims", groupsSettings.get(group).get("max-claims"));
                    groupPlayerSettings.put("max-radius-claims", groupsSettings.get(group).get("max-radius-claims"));
                    groupPlayerSettings.put("teleportation-delay", groupsSettings.get(group).get("teleportation-delay"));
                    groupPlayerSettings.put("max-members", groupsSettings.get(group).get("max-members"));
                    groupPlayerSettings.put("claim-cost", groupsSettings.get(group).get("claim-cost"));
                    groupPlayerSettings.put("claim-cost-multiplier", groupsSettings.get(group).get("claim-cost-multiplier"));
                    groupPlayerSettings.put("max-chunks-per-claim", groupsSettings.get("default").get("max-chunks-per-claim"));
                    break;
                }
            }

            if (!playersConfigSettings.containsKey(playerName)) {
                players.put(playerName, new CPlayer(player, instance.getMain().getPlayerClaimsCount(playerName),
                    (int) Math.round(groupPlayerSettings.get("max-claims")),
                    (int) Math.round(groupPlayerSettings.get("max-radius-claims")),
                    (int) Math.round(groupPlayerSettings.get("teleportation-delay")),
                    (int) Math.round(groupPlayerSettings.get("max-members")),
                    groupPlayerSettings.get("claim-cost"),
                    groupPlayerSettings.get("claim-cost-multiplier"),
                    (int) Math.round(groupPlayerSettings.get("max-chunks-per-claim"))));
            } else {
                players.put(playerName, new CPlayer(player, instance.getMain().getPlayerClaimsCount(playerName),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-claims", groupPlayerSettings.get("max-claims"))),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-radius-claims", groupPlayerSettings.get("max-radius-claims"))),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("teleportation-delay", groupPlayerSettings.get("teleportation-delay"))),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-members", groupPlayerSettings.get("max-members"))),
                    playersConfigSettings.get(playerName).getOrDefault("claim-cost", groupPlayerSettings.get("claim-cost")),
                    playersConfigSettings.get(playerName).getOrDefault("claim-cost-multiplier", groupPlayerSettings.get("claim-cost-multiplier")),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-chunks-per-claim", groupPlayerSettings.get("max-chunks-per-claim")))));
            }
        });
    }
}
