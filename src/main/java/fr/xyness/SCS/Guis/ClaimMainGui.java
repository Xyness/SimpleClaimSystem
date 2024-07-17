package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Class representing the Claim GUI.
 */
public class ClaimMainGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** Inventory for the GUI. */
    private Inventory inv;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the ClaimGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMainGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = instance.getGuis().getGuiTitle("main").replace("%name%", claim.getName());
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("main") * 9, title);
        instance.executeAsync(() -> loadItems(player, claim));
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param chunk  The chunk associated with the claim.
     */
    public void loadItems(Player player, Claim claim) {

        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
        cPlayer.setClaim(claim);
        Set<String> items = new HashSet<>(instance.getGuis().getItems("main"));
        for (String key : items) {
            String lower_name = key.toLowerCase();
            String lore_template = instance.getLanguage().getMessageWP(lower_name + "-lore", (OfflinePlayer) player)
            	.replace("%description%", claim.getDescription())
	    		.replace("%claim-name%", claim.getName())
	    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
					.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
					.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
	    		.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
				.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
				.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
            List<String> lore = new ArrayList<>(instance.getGuis().getLore(lore_template));
            String title = instance.getLanguage().getMessageWP(lower_name + "-title", (OfflinePlayer) player)
            	.replace("%description%", claim.getDescription())
	    		.replace("%claim-name%", claim.getName())
	    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
					.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
					.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
	    		.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
				.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
				.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
            if(isClickableSlot(key)) {
            	lore.add(!checkPermButton(player, key) ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
            }
            if (instance.getGuis().getItemCheckCustomModelData("main", key)) {
                inv.setItem(instance.getGuis().getItemSlot("main", key),
                		instance.getGuis().createItemWMD(title, lore, instance.getGuis().getItemMaterialMD("main", key),
                                instance.getGuis().getItemCustomModelData("main", key)));
            } else {
            	Material mat = instance.getGuis().getItemMaterial("main", key);
            	ItemStack item = new ItemStack(mat);
            	if(mat.equals(Material.PLAYER_HEAD)) {
            		item = instance.getPlayerMain().getPlayerHead((OfflinePlayer) player);
            	}
        		ItemMeta meta = item.getItemMeta();
        		meta.setDisplayName(title);
        		meta.setLore(lore);
        		item.setItemMeta(meta);
                inv.setItem(instance.getGuis().getItemSlot("main", key), item);
            }
        }

        Set<String> custom_items = new HashSet<>(instance.getGuis().getCustomItems("main"));
        for (String key : custom_items) {
            List<String> lore = new ArrayList<>(instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("main", key), player));
            String title = instance.getSettings().getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, instance.getGuis().getCustomItemTitle("main", key)) : instance.getGuis().getCustomItemTitle("main", key);
            if (instance.getGuis().getCustomItemCheckCustomModelData("main", key)) {
                inv.setItem(instance.getGuis().getCustomItemSlot("main", key),
                		instance.getGuis().createItemWMD(title, lore, instance.getGuis().getCustomItemMaterialMD("main", key),
                                instance.getGuis().getCustomItemCustomModelData("main", key)));
            } else {
                inv.setItem(instance.getGuis().getCustomItemSlot("main", key),
                		instance.getGuis().createItem(instance.getGuis().getCustomItemMaterial("main", key), title, lore));
            }
        }
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }
    
    /**
     * Checks if the specified key is a clickable slot
     * 
     * @param key The key to check for
     * @return True if the key is a clickable slot
     */
    public boolean isClickableSlot(String key) {
    	switch(key) {
    		default:
    			return true;
    	}
    }

    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param key    The key to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermButton(Player player, String key) {
        switch (key) {
        	case "unclaim":
        		return instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim");
            case "manage-members":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.members");
            case "manage-bans":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.bans");
            case "manage-settings":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.settings");
            case "manage-chunks":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.chunks");
            case "claim-info":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.list");
            case "teleport-claim":
            	return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp");
            default:
                return false;
        }
    }
    
    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param perm    The perm to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermPerm(Player player, String perm) {
    	return instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+perm);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    /**
     * Opens the inventory for the player.
     *
     * @param player The player for whom the inventory is opened.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
