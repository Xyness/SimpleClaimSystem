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
 * Claim (or Zone conditionally) Settings GUI.
 */
public class ClaimSettingsGui implements InventoryHolder {

	
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
     * Main constructor for the ClaimSettingsGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     * @param role   The role associated with permissions.
     */
    public ClaimSettingsGui(Player player, Claim claim, SimpleClaimSystem instance, String role) {
    	this.instance = instance;
    	this.player = player;
		final Zone zone = claim.setZoneOfGUIByLocation(player);

    	String role_displayed;
    	switch(role) {
	    	case "members":
	    		role_displayed = instance.getLanguage().getMessage("role-members", zone);
	    		break;
	    	case "natural":
	    		role_displayed = instance.getLanguage().getMessage("role-natural", zone);
	    		break;
    		default:
	    		role_displayed = instance.getLanguage().getMessage("role-visitors", zone);
	    		break;
    	}
    	// Get title (Add brackets if it is a zone)
		String name = (zone != null) ? String.format("[%s]", zone.getName()) : claim.getName();
    	GuiSettings guiSettings = ClaimGuis.getGuiSettings("settings", zone);
    	String title = guiSettings.getTitle()
    			.replace("%name%", name)
    			.replace("%role%", role_displayed);;
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(claim, role, zone).thenAccept(success -> {
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
	 * @param role   The role associated with permissions.
	 * @param zone   Zone to edit, or null to affect whole claim.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim, String role, Zone zone) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        cPlayer.setFilter(role);
			cPlayer.setGuiZone(zone);
	
	    	// Get data
	        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled", zone);
	        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled", zone);
	        String default_choix_disabled = instance.getLanguage().getMessage("choice-disabled", zone);
	        String default_choix_enabled = instance.getLanguage().getMessage("choice-enabled", zone);
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.getGuiSlots(zone).get("settings"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String key = slot.getKey();
    			String title = slot.getTitle();
    			String lore_string = slot.getLore();
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(instance.getGuis().isAPerm(key)) {
    				if(!instance.getGuis().isAPerm(key, role)) {
    					continue;
    				}
    	            boolean permission = claim.getPermission(key,role);
    	            String statut = permission ? default_statut_enabled : default_statut_disabled;
    	            String choix = permission ? default_choix_enabled : default_choix_disabled;
    	            lore.add(instance.getSettings().isEnabled(key) ? checkPermPerm(player,key) ? choix : instance.getLanguage().getMessage("gui-button-no-permission")+instance.getLanguage().getMessage("to-use-setting") : instance.getLanguage().getMessage("choice-setting-disabled"));
    	            title = title.replace("%status%", statut);
    			} else if (key.equals("Filter")) {
    	            String loreFilter = instance.getLanguage().getMessage("role-lore", zone)
    	                    .replaceAll("%status_color_" + getStatusIndex(role) + "%", instance.getLanguage().getMessage("status_color_active_filter"))
    	                    .replaceAll("%status_color_[^" + getStatusIndex(role) + "]%", instance.getLanguage().getMessage("status_color_inactive_filter"));
    	            lore = instance.getGuis().getLore(loreFilter);
    			}
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
    
    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param perm    The perm to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermPerm(Player player, String perm) {
    	return instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+perm) || player.hasPermission("scs.setting.*");
    }
    
    /**
     * Get the index of the current role.
     * 
     * @param filter The current role.
     * @return The index of the role.
     */
    private int getStatusIndex(String role) {
        switch (role) {
            case "members":
                return 2;
            case "natural":
                return 3;
            default:
                return 1;
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

}
