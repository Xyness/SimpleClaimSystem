package fr.xyness.SCS.Guis.Bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Commands.ClaimCommand;

/**
 * Bedrock confirmation GUI for adding Chunk/Zone to claim.
 */
public class BChunkConfirmationGui {

	
    // ***************
    // *  Variables  *
    // ***************

    
    /** Floodgate Player */
    private final FloodgatePlayer floodgatePlayer;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the BChunkConfirmationGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     * @param price The price.
     */
    public BChunkConfirmationGui(Player player, SimpleClaimSystem instance, double price) {
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	
    	String lore = "";
    	if(instance.getSettings().getBooleanSetting("economy") && price > 0) {
    		lore += instance.getLanguage().getMessage("bedrock-chunk-confirm-info-lore-economy")
    				.replace("%price%", instance.getMain().getPrice(String.valueOf(price)))
    				.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))+"\n";
    	}
    	lore += instance.getLanguage().getMessage("bedrock-chunk-confirm-info-lore");
    	
        // Création d'un formulaire simple
    	ModalForm form = ModalForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-chunk-confirm-title"))  // "Confirm adding chunk?"
	        .content(lore)
	        .button1(instance.getLanguage().getMessage("bedrock-confirm-title"))
	        .button2(instance.getLanguage().getMessage("bedrock-cancel-title"))
	        .invalidResultHandler(() -> ClaimCommand.isOnAdd.remove(player))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	            	String claimName = ClaimCommand.isOnAdd.get(player);
	            	Bukkit.dispatchCommand(player, "claim addchunk "+claimName);
	            	return;
	        	}
	        	ClaimCommand.isOnAdd.remove(player);
	        	return;
	        })
	        .build();
        
        floodgatePlayer.sendForm(form);
    }

}
