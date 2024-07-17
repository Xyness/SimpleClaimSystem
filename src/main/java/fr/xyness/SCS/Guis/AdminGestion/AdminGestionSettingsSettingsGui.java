package fr.xyness.SCS.Guis.AdminGestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Class representing the Admin Gestion Claim GUI.
 */
public class AdminGestionSettingsSettingsGui implements InventoryHolder {

	
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
     * Main constructor for the AdminGestionClaimGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param filter  The filter to use on settings
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionSettingsSettingsGui(Player player, String filter, SimpleClaimSystem instance) {
    	this.instance = instance;
        inv = Bukkit.createInventory(this, 54, "§4[A]§r Plugin settings: "+(filter.equals("status") ? "Status settings" : "Default values"));
        instance.executeAsync(() -> loadItems(player, filter));
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
    public void loadItems(Player player, String filter) {

        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled");
        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled");
        String default_choix_disabled = instance.getLanguage().getMessage("choice-disabled");
        String default_choix_enabled = instance.getLanguage().getMessage("choice-enabled");

        String choix;

        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        cPlayer.setFilter(filter);
        Map<String,Boolean> map;
        if(filter.equals("status")) {
        	map = instance.getSettings().getStatusSettings();
        } else {
        	map = instance.getSettings().getDefaultValues();
        }
        
        for (String key : instance.getSettings().getDefaultValues().keySet()) {
            String lower_name = key.toLowerCase();
            List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP(lower_name + "-lore", (OfflinePlayer) player)));
            if (instance.getGuis().isAPerm(key)) {
                boolean permission = map.get(key);
                String statut = permission ? default_statut_enabled : default_statut_disabled;
                choix = permission ? default_choix_enabled : default_choix_disabled;
                lore.add(choix);
                if (instance.getGuis().getItemCheckCustomModelData("settings", key)) {
                    inv.setItem(instance.getGuis().getItemSlot("settings", key),
                    		instance.getGuis().createItemWMD(instance.getLanguage().getMessageWP(lower_name + "-title", (OfflinePlayer) player)
                                    .replace("%status%", statut), lore, instance.getGuis().getItemMaterialMD("settings", key),
                                    instance.getGuis().getItemCustomModelData("settings", key)));
                } else {
                    inv.setItem(instance.getGuis().getItemSlot("settings", key), instance.getGuis().createItem(
                            instance.getGuis().getItemMaterial("settings", key),
                            instance.getLanguage().getMessageWP(lower_name + "-title", (OfflinePlayer) player).replace("%status%", statut),
                            lore));
                }
            }
        }

        inv.setItem(49, instance.getGuis().createItem(Material.ARROW, "§cPrevious page", Arrays.asList("§7Go back to plugin settings","§7▸§f Click to access")));
        instance.executeEntitySync(player, () -> player.openInventory(inv));
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
