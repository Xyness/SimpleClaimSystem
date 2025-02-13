package fr.xyness.SCS.Guis.Bedrock;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Commands.UnclaimCommand;

/**
 * Class representing the Claim GUI.
 */
public class BUnclaimConfirmationGui {

	
    // ***************
    // *  Variables  *
    // ***************

    
    /** Floodgate Player */
    private final FloodgatePlayer floodgatePlayer;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the BUnclaimConfirmationGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public BUnclaimConfirmationGui(Player player, SimpleClaimSystem instance) {
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	
    	String lore = instance.getLanguage().getMessage("bedrock-unclaim-confirm-info-lore");
    	
        // Création d'un formulaire simple
    	ModalForm form = ModalForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-unclaim-confirm-title"))
	        .content(lore)
	        .button1(instance.getLanguage().getMessage("bedrock-confirm-title"))
	        .button2(instance.getLanguage().getMessage("bedrock-cancel-title"))
	        .invalidResultHandler(() -> UnclaimCommand.isOnDelete.remove(player))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	            	String claimName = UnclaimCommand.isOnDelete.get(player);
	            	Bukkit.dispatchCommand(player, "unclaim "+claimName);
	            	return;
	        	}
	        	UnclaimCommand.isOnDelete.remove(player);
	        	return;
	        })
	        .build();
        
        floodgatePlayer.sendForm(form);
    }

}
