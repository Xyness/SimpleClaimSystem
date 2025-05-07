package fr.xyness.SCS.Guis.Bedrock;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.ModalForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Commands.UnclaimCommand;

/**
 * Bedrock unclaim confirmation GUI.
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
		// zone: null since unclaim is not for Zone (for Zone see delete chunk/zone instead).
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	String lore = instance.getLanguage().getMessage("bedrock-unclaim-confirm-info-lore", null);
    	
        // Création d'un formulaire simple
		// Creating a simple form
    	ModalForm form = ModalForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-unclaim-confirm-title", null))
	        .content(lore)
	        .button1(instance.getLanguage().getMessage("bedrock-confirm-title", null))
	        .button2(instance.getLanguage().getMessage("bedrock-cancel-title", null))
	        .invalidResultHandler(() -> UnclaimCommand.isOnDelete.remove(player))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	            	String claimName = UnclaimCommand.isOnDelete.get(player);
					Bukkit.dispatchCommand(player, "unclaim " + claimName);
	            	return;
	        	}
	        	UnclaimCommand.isOnDelete.remove(player);
	        	return;
	        })
	        .build();
        
        floodgatePlayer.sendForm(form);
    }

}
