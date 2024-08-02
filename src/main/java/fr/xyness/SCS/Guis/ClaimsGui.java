package fr.xyness.SCS.Guis;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import fr.xyness.SCS.*;

/**
 * Class representing the Claims GUI.
 */
public class ClaimsGui implements InventoryHolder {
	
	
    // ***************
    // *  Variables  *
    // ***************

	
    /** The inventory for this GUI. */
    private Inventory inv;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for ClaimsGui.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimsGui(Player player, int page, String filter, SimpleClaimSystem instance) {
    	this.instance = instance;
    	
    	// Get title
    	String title = instance.getLanguage().getMessage("gui-claims-title")
    			.replace("%page%", String.valueOf(page));
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, 54, title);
        
        // Load the items asynchronously
        loadItems(player, page, filter).thenAccept(success -> {
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
     * Load items into the inventory.
     * 
     * @param player The player for whom the GUI is being initialized.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the list.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    private CompletableFuture<Boolean> loadItems(Player player, int page, String filter) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Get claims data
	        Map<String, Integer> owners = getOwnersByFilter(filter);
	        int ownersCount = owners.size();
	        
	        // Update player data (gui)
	        cPlayer.setFilter(filter);
	        cPlayer.clearMapString();
	
	        // Set bottom items
	        if (page > 1) inv.setItem(48, backPage(page - 1));
	        inv.setItem(49, filter(filter));
	        if (ownersCount > (page*45)) inv.setItem(50, nextPage(page + 1));
	
	        // Prepare lore
	        List<String> loreTemplate = instance.getGuis().getLore(instance.getLanguage().getMessage("owner-claim-lore"));
	        
	        // Prepare count
	        int startItem = (page - 1) * 45;
	        int i = 0;
	        int count = 0;
	
	        // Start loop
	        for (Map.Entry<String, Integer> entry : owners.entrySet()) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than 45 to not exceed
	            if (i == 45) break;
	
	            // Get owner data
	            String owner = entry.getKey();
	            int claimAmount = entry.getValue();
	            
	            // Set lore for owner
	            List<String> lore = new ArrayList<>();
	            loreTemplate.forEach(s -> {
	            	String l = s.replace("%claim-amount%", instance.getMain().getNumberSeparate(String.valueOf(claimAmount)));
	            	lore.add(l);
	            });
	            lore.add(instance.getLanguage().getMessage("owner-claim-access"));
	            
	            // Add the owner to map string for gui clicking
	            cPlayer.addMapString(i, owner);
	            
	            // Set owner head item
	        	ItemStack item = instance.getPlayerMain().getPlayerHead(owner);
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName(instance.getLanguage().getMessage("owner-claim-title").replace("%owner%", owner));
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

    /**
     * Get the owners based on the filter.
     * 
     * @param filter The filter to apply.
     * @return A map of owners and their claim count.
     */
    private Map<String, Integer> getOwnersByFilter(String filter) {
        switch (filter) {
            case "sales":
                return instance.getMain().getClaimsOwnersWithSales();
            case "online":
                return instance.getMain().getClaimsOnlineOwners();
            case "offline":
                return instance.getMain().getClaimsOfflineOwners();
            default:
                return instance.getMain().getClaimsOwnersGui();
        }
    }

    /**
     * Create a filter item.
     * 
     * @param filter The current filter.
     * @return The created ItemStack.
     */
    private ItemStack filter(String filter) {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = instance.getLanguage().getMessage("filter-new-lore")
                    .replaceAll("%status_color_" + getStatusIndex(filter) + "%", instance.getLanguage().getMessage("status_color_active_filter"))
                    .replaceAll("%status_color_[^" + getStatusIndex(filter) + "]%", instance.getLanguage().getMessage("status_color_inactive_filter"));
            meta.setDisplayName(instance.getLanguage().getMessage("filter-title"));
            meta.setLore(instance.getGuis().getLore(loreFilter));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get the index of the current filter.
     * 
     * @param filter The current filter.
     * @return The index of the filter.
     */
    private int getStatusIndex(String filter) {
        switch (filter) {
            case "sales":
                return 2;
            case "online":
                return 3;
            case "offline":
                return 4;
            default:
                return 1;
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
