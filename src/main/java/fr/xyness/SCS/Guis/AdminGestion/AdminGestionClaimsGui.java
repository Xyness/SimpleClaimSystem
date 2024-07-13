package fr.xyness.SCS.Guis.AdminGestion;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import fr.xyness.SCS.*;

/**
 * Class representing the Admin Gestion Claims GUI.
 */
public class AdminGestionClaimsGui implements InventoryHolder {
	
	
    // ***************
    // *  Variables  *
    // ***************

	
    /** The inventory for this GUI. */
    private final Inventory inv;
    
    /** The player who opened the GUI. */
    private final Player player;
    
    /** The current page of the GUI. */
    private final int page;
    
    /** The filter applied to the claims. */
    private final String filter;
    
    /** The number of items per page. */
    private final int itemsPerPage;
    
    /** The minimum slot index for owner items. */
    private final int minSlot;
    
    /** The maximum slot index for owner items. */
    private final int maxSlot;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for AdminGestionClaimsGui.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionClaimsGui(Player player, int page, String filter, SimpleClaimSystem instance) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.minSlot = 0;
        this.maxSlot = 44;
        this.itemsPerPage = maxSlot - minSlot + 1;
        this.instance = instance;

        inv = Bukkit.createInventory(this, 54, "§4[A]§r Claims (Page "+String.valueOf(page)+")");
        instance.executeAsync(this::loadItems);
    }
    
    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Load items into the inventory.
     */
    private void loadItems() {
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        cPlayer.setFilter(filter);
        cPlayer.clearMapString();

        inv.setItem(48, backPage(page - 1,!(page > 1)));

        Map<String, Integer> owners = getOwnersByFilter(filter);
        inv.setItem(49, createFilterItem(filter));

        int startItem = (page - 1) * itemsPerPage;
        int i = minSlot;
        int count = 0;

        for (Map.Entry<String, Integer> entry : owners.entrySet()) {
            if (count++ < startItem) continue;
            if (i > maxSlot) {
                inv.setItem(50, nextPage(page + 1));
                break;
            }

            String owner = entry.getKey();
            int claimAmount = entry.getValue();
            OfflinePlayer target = instance.getPlayerMain().getOfflinePlayer(owner);
            List<String> lore = new ArrayList<>(getLore("§7Claims: §b"+String.valueOf(claimAmount)+"\n \n§c[Left-click]§7 to display his claims\n§c[Shift-left-click]§7 to remove all his claims"));
            cPlayer.addMapString(i, owner);
            inv.setItem(i, createOwnerClaimItem(owner, lore, target));
            i++;
        }

        instance.executeEntitySync(player, () -> player.openInventory(inv));
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
     * Create an item representing an owner's claim.
     * 
     * @param owner The owner of the claim.
     * @param lore  The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createOwnerClaimItem(String owner, List<String> lore, OfflinePlayer target) {
        String title = "§e"+owner;
        return createPlayerHeadItem(owner, title, lore, target);
    }

    /**
     * Create a player head item representing an owner's claim.
     * 
     * @param owner The owner of the claim.
     * @param title The title for the item.
     * @param lore  The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createPlayerHeadItem(String owner, String title, List<String> lore, OfflinePlayer target) {
    	ItemStack item = instance.getPlayerMain().getPlayerHead(target);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a standard item.
     * 
     * @param material The material of the item.
     * @param name     The display name of the item.
     * @param lore     The lore of the item.
     * @return The created ItemStack.
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material != null ? material : Material.STONE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a filter item.
     * 
     * @param filter The current filter.
     * @return The created ItemStack.
     */
    private ItemStack createFilterItem(String filter) {
        String loreFilter = "§7Change filter\n%status_color_1%➲ All owners\n%status_color_2%➲ Owners with claims in sale\n%status_color_3%➲ Online owners\n%status_color_4%➲ Offline owners\n§7▸ §fClick to change"
            .replaceAll("%status_color_" + getStatusIndex(filter) + "%", "§a")
            .replaceAll("%status_color_[^" + getStatusIndex(filter) + "]%", "§8");
        return createItem(Material.END_CRYSTAL, "§eFilter", getLore(loreFilter));
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

    /**
     * Create a back page item.
     * 
     * @param page The page number.
     * @param back If the backPage is to a other menu
     * @return The created ItemStack.
     */
    private ItemStack backPage(int page, boolean back) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cPrevious page");
            meta.setLore(Arrays.asList(back ? "§7Go back to admin main menu" : "§7Go to the page "+String.valueOf(page),"§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create a next page item.
     * 
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNext page");
            meta.setLore(Arrays.asList("§7Go to the page "+String.valueOf(page),"§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Split a lore string into a list of strings.
     * 
     * @param lore The lore string to split.
     * @return A list of lore lines.
     */
    public List<String> getLore(String lore) {
        return Arrays.asList(lore.split("\n"));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
