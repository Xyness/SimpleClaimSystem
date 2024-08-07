package fr.xyness.SCS.Guis.AdminGestion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import net.md_5.bungee.api.ChatColor;

/**
 * Class representing the Admin Gestion Main GUI.
 */
public class AdminGestionMainGui implements InventoryHolder {

	
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
     * Main constructor for the AdminGestionMainGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionMainGui(Player player, SimpleClaimSystem instance) {
    	this.instance = instance;
        inv = Bukkit.createInventory(this, 54, "§4[A]§r Menu");
        loadItems(player).thenAccept(success -> {
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
     * @param player The player for whom the GUI is being initialized.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Player player) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	        List<String> lore = new ArrayList<>();
	        lore.add("§7All owners of the server");
	        lore.add(" ");
	        lore.add("§7▸ §fClick to access");
	        ItemStack item = instance.getPlayerMain().getPlayerHead(player.getName());
	        SkullMeta meta = (SkullMeta) item.getItemMeta();
	        meta.setDisplayName("§cClaims owners");
	        meta.setLore(lore);
	        item.setItemMeta(meta);
	        inv.setItem(21, item);
	        
	        lore.clear();
	        lore.add("§7All protected areas of the server");
	        lore.add(" ");
	        lore.add("§7▸ §fClick to access");
	        inv.setItem(22, instance.getGuis().createItem(Material.FARMLAND, "§cProtected areas", lore));
	        
	        lore.clear();
	        lore.add("§7Start a purge right now");
	        lore.add(" ");
	        lore.add("§7▸ §fClick to run");
	        inv.setItem(23, instance.getGuis().createItem(Material.BLAZE_POWDER, "§cStart a purge", lore));
	        
	        lore.clear();
	        lore.add("§7Claims statistics");
	        lore.add(" ");
	        lore.add("§7➣ Claims count: §b"+instance.getMain().getNumberSeparate(String.valueOf(instance.getMain().getAllClaimsCount())));
	        lore.add("§7   ⁃ §a"+instance.getMain().getNumberSeparate(String.valueOf(instance.getMain().getProtectedAreasCount()))+" protected areas§7.");
	        lore.add("§7   ⁃ A total of §d"+instance.getMain().getNumberSeparate(String.valueOf(instance.getMain().getAllClaimsChunk().size()))+" chunks§7.");
	        lore.add(" ");
	        lore.add("§7➣ Owners count: §b"+instance.getMain().getNumberSeparate(String.valueOf(instance.getMain().getClaimsOwners().size())));
	        lore.add("§7   ⁃ §a"+instance.getMain().getNumberSeparate(String.valueOf(instance.getMain().getClaimsOnlineOwners().size()))+" online owners§7.");
	        lore.add(" ");
	        lore.add("§7Information updated now");
	        inv.setItem(13, instance.getGuis().createItem(Material.BOOK, "§cClaims informations", lore));
        
	        return true;
	        
    	});
        
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
