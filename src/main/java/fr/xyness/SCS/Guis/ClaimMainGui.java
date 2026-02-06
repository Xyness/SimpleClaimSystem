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
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Class representing the Claim Main GUI.
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
     * Main constructor for the ClaimMainGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMainGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.gui_settings.get("main");
    	String title = guiSettings.getTitle()
    			.replace("%name%", claim.getName());
    	
    	// Create the inventory
    	inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(claim).thenAccept(success -> {
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
     * @param claim  The claim for which the GUI is displayed.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);

	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.gui_slots.get("main"));
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
    						.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
    						.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
    				lore_string = lore_string.replace("%description%", claim.getDescription())
    			    		.replace("%claim-name%", claim.getName())
    			    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
    							.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
    							.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
    			    		.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
    						.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
    						.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
    			}
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(title.isBlank()) title = null;
    			if(lore.isEmpty()) lore = null;
    			if(slot.isCustomModel()) {
    				Material mat = slot.getMaterial();
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
