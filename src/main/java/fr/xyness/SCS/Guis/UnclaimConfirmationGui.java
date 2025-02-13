package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import fr.xyness.SCS.SimpleClaimSystem;

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
    
    /** Slots of confirm and cancel buttons */
    public static Set<Integer> confirm_int = Set.of(0,1,2,9,10,11,18,19,20);
    public static Set<Integer> cancel_int = Set.of(6,7,8,15,16,17,24,25,26);
    
    
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
    	String title = instance.getLanguage().getMessage("gui-unclaim-confirm-title");
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, 27, title);
        
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
	        ItemStack confirm = instance.getGuis().createItem(Material.GREEN_STAINED_GLASS_PANE, instance.getLanguage().getMessage("confirm-title"), null);
	        ItemStack deny = instance.getGuis().createItem(Material.RED_STAINED_GLASS_PANE, instance.getLanguage().getMessage("cancel-title"), null);
	        for(int i : confirm_int) {
        		inv.setItem(i, confirm);
        	}
        	for(int i : cancel_int) {
        		inv.setItem(i, deny);
        	}
        	
        	String title = instance.getLanguage().getMessage("unclaim-confirm-info-title");
        	List<String> lore = new ArrayList<>();
        	lore.addAll(instance.getGuis().getLore(instance.getLanguage().getMessage("unclaim-confirm-info-lore")));
        	ItemStack info = instance.getGuis().createItem(Material.BARRIER, title, lore);
        	inv.setItem(13, info);
	        
	        return true;
	        
    	});
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
