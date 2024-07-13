package fr.xyness.SCS.Guis.AdminGestion;

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
public class AdminGestionClaimMainGui implements InventoryHolder {

	
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
     * Main constructor for the AdminGestionClaimMainGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionClaimMainGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = "§4[A]§r "+claim.getName()+" ("+claim.getOwner()+")";
        inv = Bukkit.createInventory(this, 54, title);
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
        
        List<String> lore = new ArrayList<>();
        lore.add("§7Chunks: §b"+AdminGestionMainGui.getNumberSeparate(String.valueOf(claim.getChunks().size())));
        lore.add(" ");
        lore.add("§7Members: §a"+AdminGestionMainGui.getNumberSeparate(String.valueOf(claim.getMembers().size())));
        lore.add("§7Bans: §c"+AdminGestionMainGui.getNumberSeparate(String.valueOf(claim.getBans().size())));
        lore.add(" ");
        lore.add(claim.getSale() ? ("§a✔ Claim in sale §7("+AdminGestionMainGui.getNumberSeparate(String.valueOf(claim.getPrice()))+instance.getLanguage().getMessage("money-symbol")+"§7)") : "§c✘ Claim not in sale");
        inv.setItem(13, instance.getGuis().createItem(Material.PAINTING, "§6"+claim.getName(), lore));
        
        lore.clear();
        lore.add("§7Manage banned players");
        lore.add("§7▸ §fClick to perform");
        inv.setItem(20, instance.getGuis().createItem(Material.LECTERN, "§cBans", lore));
        
        lore.clear();
        lore.add("§7Delete this claim?");
        lore.add("§7▸ §fClick to perform");
        inv.setItem(24, instance.getGuis().createItem(Material.RED_CONCRETE, "§4Unclaim", lore));
        
        lore.clear();
        lore.add("§7Manage settings");
        lore.add("§7▸ §fClick to perform");
        inv.setItem(29, instance.getGuis().createItem(Material.REPEATER, "§3Settings", lore));
        
        lore.clear();
        lore.add("§7Manage members");
        lore.add("§7▸ §fClick to perform");
        inv.setItem(30, instance.getGuis().createItem(Material.TOTEM_OF_UNDYING, "§aMembers", lore));
        
        lore.clear();
        lore.add("§7Manage chunks");
        lore.add("§7▸ §fClick to perform");
        inv.setItem(32, instance.getGuis().createItem(Material.RED_MUSHROOM_BLOCK, "§6Chunks", lore));
        
        lore.clear();
        lore.add("§7Teleport to the claim's spawn");
        lore.add("§7▸ §fClick to perform");
        inv.setItem(33, instance.getGuis().createItem(Material.ENDER_PEARL, "§5Teleport", lore));
        
        lore.clear();
        lore.add("§7Go back to claims list of "+(claim.getOwner().equals("admin") ? "protected areas" : claim.getOwner()));
        lore.add("§7▸ §fClick to access");
        inv.setItem(49, instance.getGuis().createItem(Material.DARK_OAK_DOOR, "§cPrevious page", lore));
        
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
