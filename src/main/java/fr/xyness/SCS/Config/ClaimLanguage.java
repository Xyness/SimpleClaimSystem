package fr.xyness.SCS.Config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import fr.xyness.SCS.Zone;
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

    public static final Map<String, String> zoneFields = Collections.unmodifiableMap(new HashMap<String, String>() {{
        // For yml files in SimpleClaimsSystem/src/main/resources/langs/:
        // For yml files in SimpleClaimsSystem/langs/:
        put("apply-all-claims-title", "apply-all-zones-title");
        put("apply-all-claims-lore", "apply-all-zones-lore");
        put("manage-bans-title", "manage-zone-bans-title");
        put("manage-bans-lore", "manage-zone-bans-lore");
        put("manage-chunks-title", "manage-zones-title");
        put("manage-chunks-lore", "manage-zones-lore");
        put("chunk-title", "zone-title");
        put("chunk-lore", "zone-lore");
        // FIXME: yml files in both langs folders (See https://github.com/Xyness/SimpleClaimSystem/issues/51):
        put("remove-ban-success", "zone-remove-ban-success");
        // FIXME: not yet added to translation files:
        put("manage-members-title", "manage-zone-members-title");
        put("manage-members-lore", "manage-zone-members-lore");
        put("gui-members-title", "gui-zone-members-title"); // no lore
        put("gui-chunks-title", "gui-zones-title"); // no lore
        put("already-banned", "already-banned-from-zone");
        put("add-ban-success", "add-zone-ban-success");
        // TODO: add bedrock ones!
        // NOTE: The following are not for Zone (See remove chunk/zone instead): unclaim-title unclaim-lore
    }});


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
     * Gets a message corresponding to the provided languageStringId.
     *
     * @param languageStringId The ID of the message to retrieve from the language loaded from yml file of current language.
     * @return The message corresponding to the languageStringId, or an empty string if the languageStringId is not found.
     */
    public String getMessage(String languageStringId, Zone zone) {
        // Do not remap translation string id if zone is null, but if zone is not null but there is no special one in
        // zoneFields, just use the original languageStringId (Add more to zoneFields if more separate zone strings are added to the
        // translation yml files).
        String originalId = languageStringId;
        String zoneStringId = ClaimLanguage.zoneFields.getOrDefault(languageStringId, languageStringId);
        if (zone != null) {
            if (!ClaimLanguage.zoneFields.containsKey(languageStringId)) {
                // throw NoSuchFieldException
                instance.getLogger().warning( "[ClaimLanguage] No " + languageStringId + " in zoneFields, but was used for a zone. If differs for zone, it needs to be added to zoneFields. Then add the mapped value in zoneFields to langs/ file(s) in repo root (or in src/main/resources/langs in some cases), or it won't be translated!");
            }
        }
        languageStringId = (zone != null) ? zoneStringId : languageStringId;
    	String value = lang.getOrDefault(languageStringId, "");
        if (value.isEmpty()) {
            String message = "There is no id " + languageStringId;
            if (zone != null) message += " (remapped from id \"" + originalId + "\" using zoneFields since editing a zone)";
            instance.getLogger().warning(message + " in current language file.");
        }
        if (zone != null) {
            return value
                    .replace("%zone-name%", zone.getName())
                    .replace("%zone-boundingbox%", zone.getBoundingBox().toString());
        }
        // FIXME: ^ Change to the gettext template way (How does %claim-name% get replaced?) -Poikilos
        return value;
    }
    
    /**
     * Gets a message with placeholders for the targeted player.
     *
     * @param key The key of the message to retrieve.
     * @param target The targeted player's name.
     * @return The message with placeholders replaced, or an empty string if the key is not found.
     */
    public String getMessage(String key, OfflinePlayer target, Zone zone) {
        if (!instance.getSettings().getBooleanSetting("placeholderapi") || !lang.containsKey(key)) {
            return lang.get(key);
        }
        return PlaceholderAPI.setPlaceholders(target, lang.get(key));
    }

}
