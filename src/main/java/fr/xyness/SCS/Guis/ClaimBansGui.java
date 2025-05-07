package fr.xyness.SCS.Guis;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * The Claim/Zone Bans GUI
 * (Goes to Zone mode if player is in a Zone).
 */
public class ClaimBansGui implements InventoryHolder {

	
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
     * Main constructor for the ClaimBansGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The page number of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimBansGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	final Zone zone = claim.setZoneOfGUIByLocation(player);
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.getGuiSettings("bans", zone);
    	String title = guiSettings.getTitle()  // if zone!=null,
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
     * Initializes items for the GUI.
     *
     * @param claim  The claim for which the GUI is being initialized.
     * @param page   The page number of the GUI.
	 * @param zone   The zone that was in effect when command was entered/clicked.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim, int page, Zone zone) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Get claim data
	        Set<String> bans = instance.getMain().convertUUIDSetToStringSet(claim.getBans());
	        List<String> bansList = new ArrayList<>(bans);
	        Collections.sort(bansList, (ban1, ban2) -> ban1.compareTo(ban2));
	        bans = new LinkedHashSet<>(bansList);
	        int bansCount = bans.size();
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        cPlayer.clearMapString();
	        cPlayer.setGuiPage(page);
			cPlayer.setGuiZone(zone);
	        
	        GuiSettings guiSettings = ClaimGuis.getGuiSettings("bans", zone);
	        int max = guiSettings.getEndSlot() - guiSettings.getStartSlot();
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.getGuiSlots(zone).get("bans"));
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
    				if(bansCount <= (page*max)) continue;
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
	
	        // Set template lore
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("player-banned-lore", zone)));
	        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban") ? instance.getLanguage().getMessage("unban-this-player-button", zone) : (instance.getLanguage().getMessage("gui-button-no-permission", zone) + instance.getLanguage().getMessage("to-unban", zone)));
	        
	        // Prepare count
	        int startItem = (page - 1) * max;
	        int i = guiSettings.getStartSlot();
	        int count = 0;
	        
	        // Start loop
	        for (String p : bans) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than max to not exceed
	            if (i == guiSettings.getEndSlot()) break;
	            
	            // Add the banned player to map string for gui clicking
	            cPlayer.addMapString(i, p);
	
	            // Set player head item
	        	ItemStack item = instance.getPlayerMain().getPlayerHead(p);
	        	if(item == null) {
	        		item = new ItemStack(Material.PLAYER_HEAD);
	        	}
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName(instance.getLanguage().getMessage("player-ban-title", zone).replace("%player%", p));
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
