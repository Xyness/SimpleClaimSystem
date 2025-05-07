package fr.xyness.SCS.Guis.Bedrock;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.util.FormImage.Type;
import org.geysermc.floodgate.api.FloodgateApi;
import org.geysermc.floodgate.api.player.FloodgatePlayer;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * Claim Chunks (or Zones conditionally).
 */
public class BClaimChunksGui {

	
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
    public BClaimChunksGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.floodgatePlayer = FloodgateApi.getInstance().getPlayer(player.getUniqueId());
    	
    	// Get CPlayer
    	CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
    	if(cPlayer == null) return;
    	
        // Création d'un formulaire simple
    	SimpleForm.Builder form = SimpleForm.builder()
	        .title(instance.getLanguage().getMessage("bedrock-gui-chunks-title")
	    			.replace("%name%", claim.getName()))
	        .button(instance.getLanguage().getMessage("bedrock-back-page-main"))
	        .content(instance.getLanguage().getMessage("bedrock-gui-chunks-click"))
	        .validResultHandler(response -> {
	        	int clickedSlot = response.clickedButtonId();
	        	if(clickedSlot == 0) {
	        		new BClaimMainGui(player,claim,instance);
	        		return;
	        	}
	        	String chunk = cPlayer.getMapString(clickedSlot);
	        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")) return;
	        	if (claim.getChunks().size() == 1) return;
	        	this.instance.getMain().removeClaimChunk(claim, chunk)
		    		.thenAccept(success -> {
		    			if (success) {
		    				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+chunk+"]").replace("%claim-name%", claim.getName())));
		    			} else {
		    				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
		    			}
		    		})
		            .exceptionally(ex -> {
		                ex.printStackTrace();
		                return null;
		            });
	        });
    	
        // Get claim data
    	Set<Chunk> chunks = claim.getChunks();
    	cPlayer.clearMapString();
        
        // Add buttons
    	String chunkHeadUrl = "https://i.ibb.co/kg1gN8V3/chunks.png";
    	int i = 1;
        for (Chunk chunk : chunks) {
        	cPlayer.addMapString(i, String.valueOf(chunk.getWorld().getName()+";"+chunk.getX()+";"+chunk.getZ()));
            form.button(String.valueOf(chunk.getWorld().getName()+", "+chunk.getX()+", "+chunk.getZ()), Type.URL, chunkHeadUrl);
            i++;
        }
        
        floodgatePlayer.sendForm(form.build());
    }

}
