package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Claim Chunks/Zones GUI.
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

    	Zone zone = claim.getZoneOfPlayerGUI(player);

    	// Get title
    	GuiSettings guiSettings = ClaimGuis.getGuiSettings("chunks", zone);
    	String title = guiSettings.getTitle()
    			.replace("%name%", claim.getName())
    			.replace("%page%", String.valueOf(page));
    	
    	// Create the inventory
    	inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(claim, page, zone).thenAccept(success -> {
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
     * Initializes the items for the GUI.
     * 
     * @param claim The claim the GUI should use.
     * @param page   The current page of the GUI.
	 * @param zone   The current zone (null operates on chunk instead).
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim, int page, Zone zone) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Get claim data
	        Set<Chunk> chunks = claim.getChunks();
	        int chunksCount = chunks.size();
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        cPlayer.clearMapString();
	        cPlayer.setGuiPage(page);
			cPlayer.setGuiZone(zone);
	        
	        GuiSettings guiSettings = ClaimGuis.getGuiSettings("chunks", zone);
	        int max = guiSettings.getEndSlot() - guiSettings.getStartSlot();
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.getGuiSlots(zone).get("chunks"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String key = slot.getKey();
    			String title = slot.getTitle();
    			String lore_string = slot.getLore();
    			if(key.equals("BackPage")) {
    				if(page == 1) continue;
    				title = title.replace("%page%", String.valueOf(page-1));
    				lore_string = lore_string.replace("%page%", String.valueOf(page-1));
    			}
    			if(key.equals("NextPage")) {
    				if(chunksCount <= (page*max)) continue;
    				title = title.replace("%page%", String.valueOf(page+1));
    				lore_string = lore_string.replace("%page%", String.valueOf(page+1));
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
	        
	        // Prepare template lore
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("chunk-lore")));
	        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")
	                ? (claim.getChunks().size() == 1 ? instance.getLanguage().getMessage("cannot-remove-only-remaining-chunk-gui") : instance.getLanguage().getMessage("access-claim-clickable-removechunk"))
	                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-remove-chunk"));
	        
	        // Prepare count
	        int startItem = (page - 1) * max;
	        int i = guiSettings.getStartSlot();
	        int count = 0;
	        
	        // Start loop
	        for (Chunk chunk : chunks) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than max to not exceed
	            if (i == guiSettings.getEndSlot()+1) break;
	
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
    
    @Override
    public Inventory getInventory() {
        return inv;
    }

}
