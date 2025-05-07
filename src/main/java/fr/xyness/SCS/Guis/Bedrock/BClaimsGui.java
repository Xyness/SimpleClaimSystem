package fr.xyness.SCS.Guis.Bedrock;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;

/**
 * Bedrock Claims List GUI.
 */
public class BClaimsGui {

	
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
     * Main constructor for the BClaimsGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public BClaimsGui(Player player, SimpleClaimSystem instance, String filter) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());

    	// Get CPlayer
    	CPlayer cPlayer = this.instance.getPlayerMain().getCPlayer(player.getUniqueId());
    	if(cPlayer == null) return;
    	
        // Création d'un formulaire simple
		// Creating a simple form
		// zone: null since listing claims (see chunks/zones list GUI for zones)
    	SimpleForm.Builder form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-claims-title", null))
	        .content(getContent(filter))
	        .button(instance.getLanguage().getMessage("bedrock-gui-claims-filter", null))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	        		new BClaimsGui(player,instance,getNextFilter(filter));
	        		return;
	        	}
        		String owner = cPlayer.getMapString(clickedSlot);
        		if(filter.equals("sales")) {
        			new BClaimsOwnerGui(player, instance, owner, filter);
        			return;
        		}
        		new BClaimsOwnerGui(player, instance, owner, "all");
	        	return;
	        });
    	
        // Get claims
        Map<String, Integer> owners = getOwnersByFilter(filter);
        LinkedHashMap<String, Integer> sortedOwners = owners.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    	cPlayer.clearMapString();
        
        // Add buttons
    	int i = 1;
    	for (Map.Entry<String, Integer> entry : sortedOwners.entrySet()) {
            // Get owner data
            String owner = entry.getKey();
            int claimAmount = entry.getValue();
        	cPlayer.addMapString(i, owner);
        	String ownerHeadUrl = "https://mc-heads.net/avatar/" + owner + "/150";
            form.button(owner + " (Claims: "+instance.getMain().getNumberSeparate(String.valueOf(claimAmount))+")", Type.URL, ownerHeadUrl);
            i++;
        }
        
        floodgatePlayer.sendForm(form.build());
    }
    
    /**
     * Gets the content of the filter.
     * 
     * @param filter The filter.
     * @return The content of the filter.
     */
    public String getContent(String filter) {
    	if(filter.equals("all")) {
    		return instance.getLanguage().getMessage("bedrock-gui-claims-click-1");
    	} else if (filter.equals("sales")) {
    		return instance.getLanguage().getMessage("bedrock-gui-claims-click-2");
    	} else if (filter.equals("online")) {
    		return instance.getLanguage().getMessage("bedrock-gui-claims-click-3");
    	} else if (filter.equals("offline")) {
    		return instance.getLanguage().getMessage("bedrock-gui-claims-click-4");
    	} else {
    		return instance.getLanguage().getMessage("bedrock-gui-claims-click-1");
    	}
    }
    
    /**
     * Gets the next filter.
     * 
     * @param filter The filter.
     * @return The next filter
     */
    public String getNextFilter(String filter) {
    	if(filter.equals("all")) {
    		filter = "sales";
    	} else if (filter.equals("sales")) {
    		filter = "online";
    	} else if (filter.equals("online")) {
    		filter = "offline";
    	} else {
    		filter = "all";
    	}
    	return filter;
    }
    
    /**
     * Get the owners based on the filter.
     * 
     * @param filter The filter to apply.
     * @return A map of owners and their claim count.
     */
    private Map<String, Integer> getOwnersByFilter(String filter) {
        switch (filter) {
            case "sales":
                return instance.getMain().getClaimsOwnersWithSales();
            case "online":
                return instance.getMain().getClaimsOnlineOwners();
            case "offline":
                return instance.getMain().getClaimsOfflineOwners();
            default:
                return instance.getMain().getClaimsOwnersGui();
        }
    }

}
