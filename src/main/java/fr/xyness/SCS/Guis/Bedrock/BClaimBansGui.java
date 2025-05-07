package fr.xyness.SCS.Guis.Bedrock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;

/**
 * Claim (or Zone, conditionally) bans GUI for Bedrock.
 */
public class BClaimBansGui {

	
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
    public BClaimBansGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	
        // Création d'un formulaire simple
    	SimpleForm.Builder form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-bans-title")
	    			.replace("%name%", claim.getName()))
	        .button(instance.getLanguage().getMessage("bedrock-back-page-main"))
	        .content(instance.getLanguage().getMessage("bedrock-gui-bans-click"))
	        .validResultHandler(response -> {
	        	if(response.clickedButtonId() == 0) {
	        		new BClaimMainGui(player,claim,instance);
	        		return;
	        	}
	        	String banned = response.clickedButton().text();
	        	if(player.getName().equals(banned)) return;
	        	if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban")){
	        		String message = instance.getLanguage().getMessage("remove-ban-success").replace("%player%", banned).replace("%claim-name%", claim.getName());
	            	this.instance.getMain().removeClaimBan(claim, banned)
	            		.thenAccept(success -> {
	            			if (success) {
	            				instance.executeEntitySync(player, () -> player.sendMessage(message));
	                            Player target = Bukkit.getPlayer(banned);
	            		        if (target != null && target.isOnline()) {
	            		        	instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("unbanned-claim-player").replace("%owner%", player.getName()).replace("%claim-name%", claim.getName())));
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
        Set<String> bans = instance.getMain().convertUUIDSetToStringSet(claim.getBans());
        List<String> bansList = new ArrayList<>(bans);
        Collections.sort(bansList, (ban1, ban2) -> ban1.compareTo(ban2));
        bans = new LinkedHashSet<>(bansList);
        
        // Add buttons
        for (String ban : bans) {
            String banHeadUrl = "https://mc-heads.net/avatar/" + ban + "/150";
            form.button(ban, Type.URL, banHeadUrl);
        }
        
        floodgatePlayer.sendForm(form.build());
    }

}
