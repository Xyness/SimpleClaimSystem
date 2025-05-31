package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Class representing the Claim Bans GUI.
 */
public class UnclaimConfirmationGui implements InventoryHolder {

	
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
     * Main constructor for the ClaimConfirmationGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public UnclaimConfirmationGui(Player player, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.gui_settings.get("unclaim_confirmation");
    	String title = guiSettings.getTitle();
    	
    	// Create the inventory
    	inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems().thenAccept(success -> {
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
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems() {
    	
    	return CompletableFuture.supplyAsync(() -> {
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.gui_slots.get("unclaim_confirmation"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String title = slot.getTitle();
    			String lore_string = slot.getLore();
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(title.isBlank()) title = null;
    			if(lore.isEmpty()) lore = null;
    			Material mat = slot.getMaterial();
    			if(slot.isCustomModel()) {
    				inv.setItem(slot_int, instance.getGuis().createCustomItem(mat, title, lore, slot.getCustomModelData()));
    			} else if (slot.isCustomHead()) {
    				if(slot.getCustomTextures().equalsIgnoreCase("%player%")) {
    					ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    					SkullMeta meta = (SkullMeta) head.getItemMeta();
    					meta.setOwningPlayer(player);
    					meta.setDisplayName(title);
    					meta.setLore(lore);
    					head.setItemMeta(meta);
    					inv.setItem(slot_int, head);
    				} else if (slot.getCustomTextures().length() < 17) {
    					ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    					SkullMeta meta = (SkullMeta) head.getItemMeta();
    					OfflinePlayer targetP = Bukkit.getOfflinePlayer(slot.getCustomTextures());
    					if(targetP != null) {
    						meta.setOwningPlayer(targetP);
    					}
    					meta.setDisplayName(title);
    					meta.setLore(lore);
    					head.setItemMeta(meta);
    					inv.setItem(slot_int, head);
    				} else {
    					inv.setItem(slot_int, instance.getPlayerMain().createPlayerHeadWithTexture(slot.getCustomTextures(), title, lore));
    				}
    			} else {
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
