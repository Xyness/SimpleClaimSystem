package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
 * Claim/Zone Members GUI.
 */
public class ClaimMembersGui implements InventoryHolder {
    
	
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
     * Main constructor for ClaimMembersGui.
     * 
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The page number of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMembersGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	Zone zone = claim.getZoneOfPlayerGUI(player);
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.getGuiSettings("members", zone);
		// NOTE: ^ Fields in members.yml may be different based on zoneFields--for example:

    	String title = guiSettings.getTitle()  // "gui-members-title" becomes "gui-zone-members-title" if zone!=null above
    			.replace("%name%", claim.getName())
    			.replace("%page%", String.valueOf(page))
				.replace("%zone-name%", zone.getName());
    	
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
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The page number of the GUI.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim, int page, Zone zone) {
    	
    	return CompletableFuture.supplyAsync(() -> {
	    	// Get player data
	    	String playerName = player.getName();
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        // Get claim data
	        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getMembers());
	        List<String> membersList = new ArrayList<>(members);
	        Collections.sort(membersList, (member1, member2) -> member1.compareTo(member2));
	        members = new LinkedHashSet<>(membersList);
	        int membersCount = members.size();
	        members.remove(playerName);
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        cPlayer.clearMapString();
	        cPlayer.setGuiPage(page);
			cPlayer.setGuiZone(zone);
	        
    		GuiSettings guiSettings = ClaimGuis.getGuiSettings("members", zone);
	        int max = guiSettings.getEndSlot() - guiSettings.getStartSlot();
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.getGuiSlots(zone).get("members"));
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
    				if(membersCount <= (page*max)) continue;
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
	        
	        // Set owner item
	        if(page == 1) {
	        	int n = guiSettings.getStartSlot();
	            ItemStack ownerHead = instance.getPlayerMain().getPlayerHead(playerName);
	            SkullMeta metaHead = (SkullMeta) ownerHead.getItemMeta();
	            metaHead.setDisplayName(instance.getLanguage().getMessage("player-member-title", zone).replace("%player%", playerName));
	            metaHead.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("owner-territory-lore", zone)));
	            ownerHead.setItemMeta(metaHead);
	            inv.setItem(n, ownerHead);
	            cPlayer.addMapString(n, playerName);
	        }
	        
	        // Prepare lore
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("territory-access-lore-new", zone)));
	        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove", zone)
	                ? instance.getLanguage().getMessage("access-claim-clickable-removemember", zone)
	                : instance.getLanguage().getMessage("gui-button-no-permission", zone) + instance.getLanguage().getMessage("to-remove-member", zone));
	        
	        // Prepare count
	        int startItem = (page - 1) * max;
	        int i = guiSettings.getStartSlot()+1;
	        int count = 0;
	        
	        // Start loop
	        for (String p : members) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than max to not exceed
	            if (i == guiSettings.getEndSlot()+1) break;
	
	            // Add the member to map string for gui clicking
	            cPlayer.addMapString(i, p);
	
	            // Set member head
	            ItemStack item = instance.getPlayerMain().getPlayerHead(p);
	        	if(item == null) {
	        		item = new ItemStack(Material.PLAYER_HEAD);
	        	}
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName(instance.getLanguage().getMessage("player-member-title", zone).replace("%player%", p));
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
