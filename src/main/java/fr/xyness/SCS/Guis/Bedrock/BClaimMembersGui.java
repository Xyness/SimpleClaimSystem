package fr.xyness.SCS.Guis.Bedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;

/**
 * Bedrock Claim/Zone Members GUI.
 */
public class BClaimMembersGui {

	
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
     * Main constructor for the BClaimMainGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public BClaimMembersGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
		Zone zone = claim.getZoneOfPlayerGUI(player);
        // Création d'un formulaire simple
		// Creating a simple form
		// zone: null for buttons applying only to claim rather than zone/chunk
    	SimpleForm.Builder form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-members-title", zone)
	    			.replace("%name%", claim.getName()))
	        .button(instance.getLanguage().getMessage("bedrock-back-page-main", null))
	        .content(instance.getLanguage().getMessage("bedrock-gui-members-click", zone))
	        .validResultHandler(response -> {
	        	if(response.clickedButtonId() == 0) {
	        		new BClaimMainGui(player,claim,instance);
	        		return;
	        	}
	        	String member = response.clickedButton().text();
	        	if(player.getName().equals(member)) return;
	        	if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")) {
	        		String message = instance.getLanguage().getMessage("remove-member-success", zone).replace("%player%", member).replace("%claim-name%", claim.getName());
	            	this.instance.getMain().removeClaimMember(claim, member)
	            		.thenAccept(success -> {
	            			if (success) {
	            	        	instance.executeEntitySync(player, () -> player.sendMessage(message));
	                            Player target = Bukkit.getPlayer(member);
	                            if(target != null && target.isOnline()) {
	                            	instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replace("%claim-name%", claim.getName()).replace("%owner%", member)));
	                            }
	            			} else {
	            				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
	            			}
	            		})
	                    .exceptionally(ex -> {
	                        ex.printStackTrace();
	                        return null;
	                    });
	                return;
	        	}
	        });
    	
        // Get claim data
        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getMembers());
        List<String> membersList = new ArrayList<>(members);
        Collections.sort(membersList, (member1, member2) -> member1.compareTo(member2));
        members = new LinkedHashSet<>(membersList);
        
        // Add buttons
        for (String member : members) {
            String memberHeadUrl = "https://mc-heads.net/avatar/" + member + "/150";
            form.button(member, Type.URL, memberHeadUrl);
        }
        
        floodgatePlayer.sendForm(form.build());
    }

}
