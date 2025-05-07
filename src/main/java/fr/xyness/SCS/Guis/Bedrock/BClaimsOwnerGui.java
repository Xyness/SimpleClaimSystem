package fr.xyness.SCS.Guis.Bedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * Claim Owners GUI.
 */
public class BClaimsOwnerGui {

	
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
     * Main constructor for the BClaimsOwnerGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public BClaimsOwnerGui(Player player, SimpleClaimSystem instance, String owner, String filter) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	// zone: null since only claims have owners (zones/claims have members)
    	// Get CPlayer
    	CPlayer cPlayer = this.instance.getPlayerMain().getCPlayer(player.getUniqueId());
    	if(cPlayer == null) return;
    	
        // Création d'un formulaire simple
    	SimpleForm.Builder form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-claims-owner-title", null).replace("%owner%", owner))
	        .content(getContent(filter).replace("%owner%", owner))
	        .button(instance.getLanguage().getMessage("bedrock-back-page-main", null))
	        .button(instance.getLanguage().getMessage("bedrock-gui-claims-owner-filter", null))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	        		new BClaimsGui(player,instance,filter);
	        		return;
	        	}
	        	if(clickedSlot == 1) {
	        		new BClaimsOwnerGui(player,instance,owner,getNextFilter(filter));
	        		return;
	        	}
        		Claim claim = cPlayer.getMapClaim(clickedSlot);
        		if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
		            if(!claim.getPermissionForPlayer("GuiTeleport",player) && !claim.getOwner().equals(player.getName()) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.guiteleport")) return;
		            instance.executeEntitySync(player, () -> player.closeInventory());
		        	instance.getMain().goClaim(player, claim.getLocation());
		        	return;
        		}
	        	return;
	        });
    	
        // Get claims
        Set<Claim> claims = getClaims(filter, owner);
        List<Claim> claimList = new ArrayList<>(claims);
        Collections.sort(claimList, (claim1, claim2) -> claim1.getName().compareTo(claim2.getName()));
        claims = new LinkedHashSet<>(claimList);
    	cPlayer.clearMapClaim();
        
        // Add buttons
    	int i = 2;
    	for (Claim claim : claims) {
            // Get owner data
        	cPlayer.addMapClaim(i, claim);
        	String ownerHeadUrl = "https://mc-heads.net/avatar/" + owner + "/150";
            form.button(claim.getName(), Type.URL, ownerHeadUrl);
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
    		return instance.getLanguage().getMessage("bedrock-gui-claims-owner-click-1");
    	} else {
    		return instance.getLanguage().getMessage("bedrock-gui-claims-owner-click-2");
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
    	} else {
    		filter = "all";
    	}
    	return filter;
    }
    
    /**
     * Get the claims based on the filter and owner.
     * 
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     * @return A map of claims and their corresponding chunks.
     */
    private Set<Claim> getClaims(String filter, String owner) {
        return "sales".equals(filter) ? instance.getMain().getClaimsInSale(owner) : instance.getMain().getPlayerClaims(owner);
    }

}
