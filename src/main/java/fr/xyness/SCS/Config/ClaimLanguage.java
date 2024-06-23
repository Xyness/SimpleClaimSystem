package fr.xyness.SCS.Config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

/**
 * ClaimLanguage class handles the language settings and messages for the plugin.
 */
public class ClaimLanguage {

    // ***************
    // *  Variables  *
    // ***************

    /** A map to store language keys and their corresponding messages. */
    public static Map<String, String> lang = new HashMap<>();

    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Sets the language map with the provided messages map.
     *
     * @param messagesMap A map containing message keys and their corresponding messages.
     * @return true if the language map is successfully set.
     */
    public static boolean setLanguage(Map<String, String> messagesMap) {
        lang = messagesMap;
        return true;
    }

    /**
     * Gets a message corresponding to the provided key.
     *
     * @param key The key of the message to retrieve.
     * @return The message corresponding to the key, or an empty string if the key is not found.
     */
    public static String getMessage(String key) {
    	return lang.containsKey(key) ? lang.get(key) : "";
    }

    /**
     * Gets a message with placeholders for the targeted player.
     *
     * @param key The key of the message to retrieve.
     * @param target The targeted player's name.
     * @return The message with placeholders replaced, or an empty string if the key is not found.
     */
    public static String getMessageWP(String key, String target) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi") || !lang.containsKey(key)) {
            return getMessage(key);
        }
        
        String message = lang.get(key);
        Player player = Bukkit.getPlayer(target);
        return player == null 
            ? PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(target), message) 
            : PlaceholderAPI.setPlaceholders(player, message);
    }

}
