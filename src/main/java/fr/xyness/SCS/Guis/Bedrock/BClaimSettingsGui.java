package fr.xyness.SCS.Guis.Bedrock;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import fr.xyness.SCS.Zone;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * Claim Settings GUI.
 */
public class BClaimSettingsGui {

	
    // ***************
    // *  Variables  *
    // ***************

    
    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;
    
    /** Floodgate Player */
    private final FloodgatePlayer floodgatePlayer;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the BClaimBansGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public BClaimSettingsGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
		Zone zone = claim.setZoneOfGUIByLocation(player);

    	// Get CPlayer
    	CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
    	if(cPlayer == null) return;
    	
        // Création d'un formulaire simple
		// Creating a simple form
		// zone: null since only applies to claim not chunk/zone
    	CustomForm.Builder form = CustomForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-settings-title", zone)
	    			.replace("%name%", claim.getName()))
	        .validResultHandler(response -> {
	        	Map<String,LinkedHashMap<String,Boolean>> perms = new HashMap<>();
	        	perms.put("visitors", new LinkedHashMap<>());
	        	int i = 3;
	        	for (String key : instance.getGuis().getPerms("visitors")) {
	        		if(!instance.getSettings().isEnabled(key) || !checkPermPerm(player,key)) continue;
	        		perms.get("visitors").put(key, response.asToggle(i));
	        		i++;
	        	}
	        	i += 3;
	        	perms.put("members", new LinkedHashMap<>());
	        	for (String key : instance.getGuis().getPerms("members")) {
	        		if(!instance.getSettings().isEnabled(key) || !checkPermPerm(player,key)) continue;
	        		perms.get("members").put(key, response.asToggle(i));
	        		i++;
	        	}
	        	i += 3;
	        	perms.put("natural", new LinkedHashMap<>());
	        	for (String key : instance.getGuis().getPerms("natural")) {
	        		if(!instance.getSettings().isEnabled(key) || !checkPermPerm(player,key)) continue;
	        		perms.get("natural").put(key, response.asToggle(i));
	        		i++;
	        	}
	        	String message = instance.getLanguage().getMessage("bedrock-perms-updated").replace("%claim-name%", claim.getName());
            	this.instance.getMain().updatePermsBedrock(claim, perms)
	        		.thenAccept(success -> {
	        			if (success) {
	        				instance.executeEntitySync(player, () -> player.sendMessage(message));
	        			} else {
	        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
	        			}
	        		})
	                .exceptionally(ex -> {
	                    ex.printStackTrace();
	                    return null;
	                });
            return;
	        });
        
        // Add buttons
    	form.label("-");
    	form.label(instance.getLanguage().getMessage("bedrock-gui-settings-role1"));
    	form.label("-");
        // Set settings items
        for (String key : instance.getGuis().getPerms("visitors")) {
        	if(!instance.getSettings().isEnabled(key) || !checkPermPerm(player,key)) continue;
            // Check setting status
            boolean permission = claim.getPermission(key,"visitors");
            form.toggle(instance.getLanguage().getMessage("bedrock-" + key.toLowerCase() + "-title"), permission);
        }
        form.label("-");
    	form.label(instance.getLanguage().getMessage("bedrock-gui-settings-role2"));
    	form.label("-");
        // Set settings items
        for (String key : instance.getGuis().getPerms("members")) {
        	if(!instance.getSettings().isEnabled(key) || !checkPermPerm(player,key)) continue;
            // Check setting status
            boolean permission = claim.getPermission(key,"members");
            form.toggle(instance.getLanguage().getMessage("bedrock-" + key.toLowerCase() + "-title"), permission);
        }
        form.label("-");
    	form.label(instance.getLanguage().getMessage("bedrock-gui-settings-role3"));
    	form.label("-");
    	// Set settings items
        for (String key : instance.getGuis().getPerms("natural")) {
        	if(!instance.getSettings().isEnabled(key) || !checkPermPerm(player,key)) continue;
            // Check setting status
            boolean permission = claim.getPermission(key,"natural");
            form.toggle(instance.getLanguage().getMessage("bedrock-" + key.toLowerCase() + "-title"), permission);
        }
        floodgatePlayer.sendForm(form.build());
    }
    
    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param perm    The perm to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermPerm(Player player, String perm) {
    	return instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+perm) || player.hasPermission("scs.setting.*");
    }

}
