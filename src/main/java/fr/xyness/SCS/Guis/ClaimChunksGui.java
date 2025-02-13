package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * Class representing the Claim Members GUI.
 */
public class ClaimChunksGui implements InventoryHolder {
    
	
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
     * Main constructor for ClaimChunksGui.
     * 
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The page number of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimChunksGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	
    	// Get title
    	String title = instance.getLanguage().getMessage("gui-chunks-title")
    			.replace("%name%", claim.getName())
    			.replace("%page%", String.valueOf(page));
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, 54, title);
        
        // Load the items asynchronously
        loadItems(claim, page).thenAccept(success -> {
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
     * Initializes the items for the GUI.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim, int page) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Get claim data
	        Set<Chunk> chunks = claim.getChunks();
	        int chunksCount = chunks.size();
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        cPlayer.clearMapString();
	        
	        // Set bottom items
	        if (page > 1) inv.setItem(48, backPage(page - 1));
	        inv.setItem(49, backPageMain(claim));
	        if (chunksCount > (page*45)) inv.setItem(50, nextPage(page + 1));
	        
	        // Prepare template lore
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("chunk-lore")));
	        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")
	                ? (claim.getChunks().size() == 1 ? instance.getLanguage().getMessage("cannot-remove-only-remaining-chunk-gui") : instance.getLanguage().getMessage("access-claim-clickable-removechunk"))
	                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-remove-chunk"));
	        
	        // Prepare count
	        int startItem = (page - 1) * 45;
	        int i = 0;
	        int count = 0;
	        
	        // Start loop
	        for (Chunk chunk : chunks) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than 45 to not exceed
	            if (i == 45) break;
	
	            // Add the chunk to map string for gui clicking
	            cPlayer.addMapString(i, String.valueOf(chunk.getWorld().getName()+";"+chunk.getX()+";"+chunk.getZ()));
	            
	            // Prepare title for current chunk
	            String title = instance.getLanguage().getMessage("chunk-title").replace("%coords%", String.valueOf(chunk.getWorld().getName()+", X:"+chunk.getX()+", Z:"+chunk.getZ()));
	            
	            // Set chunk item
	            ItemStack item = new ItemStack(Material.RED_MUSHROOM_BLOCK, 1);
	            ItemMeta meta = item.getItemMeta();
	            meta.setDisplayName(title);
	            meta.setLore(lore);
	            item.setItemMeta(meta);
	            inv.setItem(i, item);
	            i++;
	            
	        }
	        
	        return true;
        
    	});
	        
    }
    
    /**
     * Creates an item for the back page slot.
     *
     * @param page The page number.
     * @return The created back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("previous-page-title").replace("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("previous-page-lore").replace("%page%", String.valueOf(page))));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the back page main item.
     * 
     * @param claim The target claim
     * @return The back page main item.
     */
    private ItemStack backPageMain(Claim claim) {
        ItemStack item = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("back-page-main-title"));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("back-page-main-lore").replace("%claim-name%", claim.getName())));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates an item for the next page slot.
     *
     * @param page The page number.
     * @return The created next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("next-page-title").replace("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("next-page-lore").replace("%page%", String.valueOf(page))));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    @Override
    public Inventory getInventory() {
        return inv;
    }

}
