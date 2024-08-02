package fr.xyness.SCS.Config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.OfflinePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * ClaimLanguage class handles the language settings and messages for the plugin.
 */
public class ClaimLanguage {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** A map to store language keys and their corresponding messages. */
    private Map<String, String> lang = new HashMap<>();
    
    /** Instance of SimpleClaimSystem. */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimLanguage.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimLanguage(SimpleClaimSystem instance) {
    	this.instance = instance;
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Sets the language map with the provided messages map.
     *
     * @param messagesMap A map containing message keys and their corresponding messages.
     * @return true if the language map is successfully set.
     */
    public boolean setLanguage(Map<String, String> messagesMap) {
        lang = messagesMap;
        return true;
    }

    /**
     * Gets a message corresponding to the provided key.
     *
     * @param key The key of the message to retrieve.
     * @return The message corresponding to the key, or an empty string if the key is not found.
     */
    public String getMessage(String key) {
    	return lang.containsKey(key) ? lang.get(key) : "";
    }
    
    /**
     * Gets a message with placeholders for the targeted player.
     *
     * @param key The key of the message to retrieve.
     * @param target The targeted player's name.
     * @return The message with placeholders replaced, or an empty string if the key is not found.
     */
    public String getMessage(String key, OfflinePlayer target) {
        if (!instance.getSettings().getBooleanSetting("placeholderapi") || !lang.containsKey(key)) {
            return lang.get(key);
        }
        return PlaceholderAPI.setPlaceholders(target, lang.get(key));
    }

}
