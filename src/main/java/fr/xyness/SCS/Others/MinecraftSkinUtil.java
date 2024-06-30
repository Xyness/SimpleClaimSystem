package fr.xyness.SCS.Others;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;

import fr.xyness.SCS.SimpleClaimSystem;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Utility class for managing Minecraft skins and player heads.
 * This class provides methods to retrieve UUID and skin data for players and create custom player heads with the retrieved data.
 */
public class MinecraftSkinUtil {
	
    // ***************
    // *  Variables  *
    // ***************
    
    /** A cache to store player heads. */
    private static Map<String, ItemStack> playerHeads = new HashMap<>();

    /** A cache to store skin data. */
    private static final Map<String, String> skinDataCache = new HashMap<>();
    
    // ********************
    // *  Other Methods   *
    // ********************

    /**
     * Retrieves the UUID for a given player name.
     * 
     * @param playerName the name of the player whose UUID is to be retrieved.
     * @return the UUID of the player as a string.
     * @throws Exception if no UUID is found for the player or if an error occurs during the process.
     */
    public static String getUUID(String playerName) throws Exception {
        String url = "https://api.mojang.com/users/profiles/minecraft/" + playerName;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (response.toString().isEmpty()) {
            throw new Exception("No UUID found for player: " + playerName);
        }

        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
        return jsonObject.get("id").getAsString();
    }

    /**
     * Retrieves the skin data for a given UUID.
     * 
     * @param uuid the UUID of the player whose skin data is to be retrieved.
     * @return the skin data of the player as a string.
     * @throws Exception if no skin data is found for the UUID or if an error occurs during the process.
     */
    public static String getSkinData(String uuid) throws Exception {
        if (skinDataCache.containsKey(uuid)) {
            return skinDataCache.get(uuid);
        }
        String url = "https://sessionserver.mojang.com/session/minecraft/profile/" + uuid;
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");

        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();

        if (response.toString().isEmpty()) {
            throw new Exception("No skin data found for UUID: " + uuid);
        }

        JsonObject jsonObject = JsonParser.parseString(response.toString()).getAsJsonObject();
        JsonObject properties = jsonObject.getAsJsonArray("properties").get(0).getAsJsonObject();
        String p = properties.get("value").getAsString();
        skinDataCache.put(uuid, p);
        return p;
    }

    /**
     * Creates a custom player head ItemStack for a given player name.
     * 
     * @param playerName the name of the player whose head is to be created.
     * @return the created ItemStack representing the player's head.
     */
    public static ItemStack createPlayerHead(String playerName) {
        if (playerHeads.containsKey(playerName)) {
            return playerHeads.get(playerName);
        }

        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();

        try {
            String uuid = getUUID(playerName);
            if (uuid == null || uuid.isEmpty()) {
                throw new Exception("UUID is null or empty for player: " + playerName);
            }

            String skinData = getSkinData(uuid);
            if (skinData == null || skinData.isEmpty()) {
                throw new Exception("Skin data is null or empty for UUID: " + uuid);
            }

            GameProfile profile = new GameProfile(UUID.randomUUID(), playerName);
            profile.getProperties().put("textures", new Property("textures", skinData));

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

        } catch (Exception e) {
            e.printStackTrace();
        }

        head.setItemMeta(meta);
        playerHeads.put(playerName, head);
        return head;
    }
    
    /**
     * Loads player heads for all offline players asynchronously.
     */
    public static void loadPlayersHead() {
    	SimpleClaimSystem.executeAsync(() -> {
    		Set<OfflinePlayer> offlinePlayers = new HashSet<>(Arrays.asList(Bukkit.getOfflinePlayers()));
    		offlinePlayers.forEach(p -> createPlayerHead(p.getName()));
    	});
    }
}
