package fr.xyness.SCS.Guis.Bedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
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
 * Bedrock Claim List GUI.
 */
public class BClaimListGui {

	
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
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public BClaimListGui(Player player, SimpleClaimSystem instance, String filter) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	
    	// Get CPlayer
    	CPlayer cPlayer = this.instance.getPlayerMain().getCPlayer(player.getUniqueId());
    	if(cPlayer == null) return;
    	String playerName = player.getName();
    	
        // Création d'un formulaire simple (zone: null puisque nous sommes dans le périmètre des revendications)
		// Creating a simple form (zone: null since we are in claims scope)
    	SimpleForm.Builder form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-list-title", null))
	        .content(filter.equals("owner") ? instance.getLanguage().getMessage("bedrock-gui-list-click-1", null) : instance.getLanguage().getMessage("bedrock-gui-list-click-2", null))
	        .button(instance.getLanguage().getMessage("bedrock-gui-list-filter", null))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	        		new BClaimListGui(player,instance,filter.equals("owner") ? "not_owner" : "owner");
	        		return;
	        	}
	        	if(!filter.equals("not_owner")) {
	        		if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.main")) {
		        		Claim claim = cPlayer.getMapClaim(clickedSlot);
		        		if(claim == null) return;
			        	new BClaimMainGui(player,claim,instance);
	        		}
	        	} else {
	        		Claim claim = cPlayer.getMapClaim(clickedSlot);
	            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
	            		if(claim == null) return;
	            		if(!claim.getPermissionForPlayer("GuiTeleport",player) && !claim.getOwner().equals(player.getName())) return;
			        	instance.getMain().goClaim(player, cPlayer.getMapLoc(clickedSlot));
			        	return;
	            	}
	        	}
	        	return;
	        });
    	
        // Get claims
        Set<Claim> claims = new HashSet<>(filter.equals("owner") ? instance.getMain().getPlayerClaims(playerName) : instance.getMain().getClaimsWhereMemberNotOwner(player));
        List<Claim> claimList = new ArrayList<>(claims);
        Collections.sort(claimList, (claim1, claim2) -> claim1.getName().compareTo(claim2.getName()));
        claims = new LinkedHashSet<>(claimList);
    	cPlayer.clearMapClaim();
        
        // Add buttons
    	String chunkHeadUrl = "https://i.ibb.co/kg1gN8V3/chunks.png";
    	int i = 1;
        for (Claim claim : claims) {
        	cPlayer.addMapClaim(i, claim);
            form.button(claim.getName(), Type.URL, chunkHeadUrl);
            i++;
        }
        
        floodgatePlayer.sendForm(form.build());
    }

}
