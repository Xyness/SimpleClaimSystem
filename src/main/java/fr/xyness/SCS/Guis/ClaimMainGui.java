package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Main GUI for Claim (Provides navigation to other GUIs)
 */
public class ClaimMainGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** Inventory for the GUI. */
    private final Inventory inv;
    
    /** Player */
    private final Player player;
    
    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
	 * Main GUI for Claim (Provides navigation to other GUIs) constructor.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMainGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
		Zone zone = claim.getZoneOfPlayerGUI(player);
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.getGuiSettings("main", zone);
    	String title = guiSettings.getTitle()
    			.replace("%name%", claim.getName());
    	
    	// Create the inventory
    	inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(claim, zone).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error", zone)));
        	}
        })
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Initializes items for the GUI.
     *
     * @param claim  The claim for which the GUI is displayed.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim, Zone zone) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
			cPlayer.setGuiZone(zone);

	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.getGuiSlots(zone).get("main"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String key = slot.getKey();
    			String title = slot.getTitle();
    			String lore_string = slot.getLore();
    			if(key.equals("Info")) {
    				title = title.replace("%description%", claim.getDescription())
    			    		.replace("%claim-name%", claim.getName())
    			    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
    							.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
    							.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
							.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
							.replace("%zones-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getZones().size())))
    						.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
    						.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
    				lore_string = lore_string.replace("%description%", claim.getDescription())
    			    		.replace("%claim-name%", claim.getName())
    			    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
    							.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
    							.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
							.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
							.replace("%zones-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getZones().size())))
    						.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
    						.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
    			}
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(title.isBlank()) title = null;
    			if(lore.isEmpty()) lore = null;
    			if(slot.isCustomModel()) {
    				CustomStack customItem = CustomStack.getInstance(slot.getCustomModelData());
    				if(customItem != null) {
    					Material mat = customItem.getItemStack().getType();
    					inv.setItem(slot_int, instance.getGuis().createItem(mat, title, lore));
    				}
    			} else if (slot.isCustomHead()) {
    				inv.setItem(slot_int, instance.getPlayerMain().createPlayerHeadWithTexture(slot.getCustomTextures(), title, lore));
    			} else {
					Material mat = slot.getMaterial();
					inv.setItem(slot_int, instance.getGuis().createItem(mat, title, lore));
    			}
    		}
	        return true;
	        
    	});
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

}
