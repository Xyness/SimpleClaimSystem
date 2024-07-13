package fr.xyness.SCS.Guis.AdminGestion;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
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
import net.md_5.bungee.api.ChatColor;

/**
 * Class representing the Admin Gestion GUI.
 */
public class AdminGestionSettingWorldsGui implements InventoryHolder {

	
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
     * Main constructor for the AdminGestionSettingWorldsGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionSettingWorldsGui(Player player, SimpleClaimSystem instance) {
    	this.instance = instance;
        inv = Bukkit.createInventory(this, 54, "§4[A]§r Plugin settings: Disabled worlds");
        instance.executeAsync(() -> loadItems(player));
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     */
    public void loadItems(Player player) {
        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled");
        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled");
        String default_choix_disabled = "§7▸ §fLeft-click to §nenable";
        String default_choix_enabled = "§7▸ §fLeft-click to §ndisable";

        FileConfiguration config = instance.getPlugin().getConfig();
        
        inv.setItem(49, instance.getGuis().createItem(Material.ARROW, "§cPrevious page", Arrays.asList("§7Go back to plugin settings","§7▸§f Click to access")));
        
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        cPlayer.clearMapString();
        
        Map<String,Boolean> worlds = new HashMap<>();
        Bukkit.getWorlds().forEach(w -> worlds.put(w.getName(),true));
        config.getStringList("worlds-disabled").forEach(w -> worlds.put(w, false));
        int i = 0;
        for(String world : worlds.keySet()) {
        	cPlayer.addMapString(i, world);
        	Boolean param = worlds.get(world);
        	List<String> lore = new ArrayList<>();
        	lore.add("§7Status of this world");
        	lore.add(param ? default_choix_enabled : default_choix_disabled);
        	inv.setItem(i, instance.getGuis().createItem(Material.PAPER, "§3"+world+" §7(§f"+(param ? default_statut_enabled : default_statut_disabled)+"§7)", lore));
        	i++;
        }

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
