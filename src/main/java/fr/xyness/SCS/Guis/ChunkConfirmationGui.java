package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Class representing the Claim Bans GUI.
 */
public class ChunkConfirmationGui implements InventoryHolder {

	
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
     * Main constructor for the ChunkConfirmationGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     * @param price The price of the future chunk.
     */
    public ChunkConfirmationGui(Player player, SimpleClaimSystem instance, double price) {
    	this.instance = instance;
    	this.player = player;
    	
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.gui_settings.get("chunk_confirmation");
    	String title = guiSettings.getTitle();
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(price).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
     * @param price The price of the future chunk.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(double price) {
    	
    	return CompletableFuture.supplyAsync(() -> {
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.gui_slots.get("chunk_confirmation"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String title = slot.getTitle();
    			String lore_string = replaceLore(slot.getKey(), slot.getLore(), price);
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(title.isBlank()) title = null;
    			if(lore.isEmpty()) lore = null;
    			if(slot.isCustomModel()) {
    				CustomStack customItem = CustomStack.getInstance(slot.getCustomModelData());
    				if(customItem != null) {
    					Material mat = customItem.getItemStack().getType();
    					
    					inv.setItem(slot_int, instance.getGuis().createItem(mat, title, lore));
    				}
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
    
    /**
     * Replace lore.
     * 
     * @param key The key of the slot.
     * @param lore The lore to replace.
     * @param price The price.
     * @return The replaced lore.
     */
    public String replaceLore(String key, String lore, double price) {
		if(key.equalsIgnoreCase("main-item")) {
			return lore.replace("%price%", instance.getMain().getPrice(String.valueOf(price)))
    				.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
		}
    	return lore;
    }
}
