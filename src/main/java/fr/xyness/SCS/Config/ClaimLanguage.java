package fr.xyness.SCS.Config;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.OfflinePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Handles language settings and messages for the plugin.
 */
public class ClaimLanguage {

    private Map<String, String> lang = new ConcurrentHashMap<>();
    private SimpleClaimSystem instance;

    public ClaimLanguage(SimpleClaimSystem instance) {
        this.instance = instance;
    }

    /**
     * Sets the language map with the provided messages.
     *
     * @param messagesMap A map containing message keys and their corresponding messages.
     * @return true if the language map is successfully set.
     */
    public boolean setLanguage(Map<String, String> messagesMap) {
        lang = new ConcurrentHashMap<>(messagesMap);
        return true;
    }

    /**
     * Gets a message corresponding to the provided key.
     *
     * @param key The key of the message to retrieve.
     * @return The message string, or an empty string if the key is not found.
     */
    public String getMessage(String key) {
        return lang.getOrDefault(key, "");
    }

    /**
     * Gets a message with PlaceholderAPI placeholders resolved for the targeted player.
     *
     * @param key    The key of the message to retrieve.
     * @param target The targeted player.
     * @return The message with placeholders replaced.
     */
    public String getMessage(String key, OfflinePlayer target) {
        String msg = lang.getOrDefault(key, "");
        if (msg.isEmpty() || !instance.getSettings().getBooleanSetting("placeholderapi")) {
            return msg;
        }
        return PlaceholderAPI.setPlaceholders(target, msg);
    }
}
