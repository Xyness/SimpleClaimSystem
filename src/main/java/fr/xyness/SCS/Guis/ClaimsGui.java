package fr.xyness.SCS.Guis;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.*;
import fr.xyness.SCS.Config.*;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Class representing the Claims GUI.
 */
public class ClaimsGui implements InventoryHolder {
    
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
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Main constructor for ClaimsGui.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     */
    public ClaimsGui(Player player, int page, String filter) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.minSlot = ClaimGuis.getGuiMinSlot("claims");
        this.maxSlot = ClaimGuis.getGuiMaxSlot("claims");
        this.itemsPerPage = maxSlot - minSlot + 1;

        String title = ClaimGuis.getGuiTitle("claims").replace("%page%", String.valueOf(page));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims") * 9, title);
        initializeItems();
    }
    
    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Initialize items in the GUI asynchronously.
     */
    private void initializeItems() {
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> loadItems());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> loadItems());
        }
    }

    /**
     * Load items into the inventory.
     */
    private void loadItems() {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setFilter(filter);
        cPlayer.clearMapString();

        setItemAsync(ClaimGuis.getItemSlot("claims", "back-page-list"), page > 1, backPage(page - 1));

        List<String> loreTemplate = getLore(ClaimLanguage.getMessage("owner-claim-lore"));
        Map<String, Integer> owners = getOwnersByFilter(filter);
        setItemAsync(ClaimGuis.getItemSlot("claims", "filter"), true, filterItem(filter));

        int startItem = (page - 1) * itemsPerPage;
        int i = minSlot;
        int count = 0;

        for (Map.Entry<String, Integer> entry : owners.entrySet()) {
            String owner = entry.getKey();
            int claimAmount = entry.getValue();

            if (count++ < startItem) continue;
            if (i > maxSlot) {
                setItemAsync(ClaimGuis.getItemSlot("claims", "next-page-list"), true, nextPage(page + 1));
                break;
            }

            List<String> lore = new ArrayList<>();
            for (String s : loreTemplate) {
                lore.add(s.replace("%claim-amount%", String.valueOf(claimAmount)));
            }
            lore.add(ClaimLanguage.getMessage("owner-claim-access"));
            lore = getLoreWP(lore, owner);

            cPlayer.addMapString(i, owner);
            setItemAsync(i, createOwnerClaimItem(owner, lore));
            i++;
        }

        Set<String> customItems = new HashSet<>(ClaimGuis.getCustomItems("claims"));
        for (String key : customItems) {
            List<String> lore = getLoreP(ClaimGuis.getCustomItemLore("claims", key), player);
            String title = ClaimGuis.getCustomItemTitle("claims", key);
            if (ClaimSettings.getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            setItemAsync(ClaimGuis.getCustomItemSlot("claims", key), createCustomItem(key, title, lore));
        }
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
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
                return ClaimMain.getClaimsOwnersWithSales();
            case "online":
                return ClaimMain.getClaimsOnlineOwners();
            case "offline":
                return ClaimMain.getClaimsOfflineOwners();
            default:
                return ClaimMain.getClaimsOwnersGui();
        }
    }

    /**
     * Create an item representing an owner's claim.
     * 
     * @param owner The owner of the claim.
     * @param lore  The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createOwnerClaimItem(String owner, List<String> lore) {
        String title = ClaimLanguage.getMessageWP("owner-claim-title", owner).replace("%owner%", owner);
        if (ClaimGuis.getItemCheckCustomModelData("claims", "claim-item")) {
            return createItemWMD(title, lore, ClaimGuis.getItemMaterialMD("claims", "claim-item"), ClaimGuis.getItemCustomModelData("claims", "claim-item"));
        }
        if (ClaimGuis.getItemMaterialMD("claims", "claim-item").contains("PLAYER_HEAD")) {
            return createPlayerHeadItem(owner, title, lore);
        }
        return createItem(ClaimGuis.getItemMaterial("claims", "claim-item"), title, lore);
    }

    /**
     * Create a player head item representing an owner's claim.
     * 
     * @param owner The owner of the claim.
     * @param title The title for the item.
     * @param lore  The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createPlayerHeadItem(String owner, String title, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
        meta.setDisplayName(title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a custom item.
     * 
     * @param key   The key of the custom item.
     * @param title The title for the item.
     * @param lore  The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createCustomItem(String key, String title, List<String> lore) {
        if (ClaimGuis.getCustomItemCheckCustomModelData("claims", key)) {
            return createItemWMD(title, lore, ClaimGuis.getCustomItemMaterialMD("claims", key), ClaimGuis.getCustomItemCustomModelData("claims", key));
        } else {
            return createItem(ClaimGuis.getCustomItemMaterial("claims", key), title, lore);
        }
    }

    /**
     * Split a lore string into a list of strings.
     * 
     * @param lore The lore string to split.
     * @return A list of lore lines.
     */
    public static List<String> getLore(String lore) {
        return Arrays.asList(lore.split("\n"));
    }

    /**
     * Apply placeholders to a lore string for a player.
     * 
     * @param lore   The lore string.
     * @param player The player to apply placeholders for.
     * @return A list of lore lines with placeholders applied.
     */
    public static List<String> getLoreP(String lore, Player player) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) {
            return getLore(lore);
        }
        List<String> lores = new ArrayList<>();
        String[] parts = lore.split("\n");
        for (String s : parts) {
            lores.add(PlaceholderAPI.setPlaceholders(player, s));
        }
        return lores;
    }

    /**
     * Get a list of lore lines with placeholders replaced.
     * 
     * @param lore       The list of lore lines.
     * @param playerName The name of the player for whom to replace placeholders.
     * @return A list of lore lines with placeholders replaced.
     */
    public static List<String> getLoreWP(List<String> lore, String playerName) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) {
            return lore;
        }
        List<String> lores = new ArrayList<>();
        Player player = Bukkit.getPlayer(playerName);
        OfflinePlayer offlinePlayer = player != null ? player : CPlayerMain.getOfflinePlayer(playerName);
        if(offlinePlayer == null) return lores;
        for (String line : lore) {
            lores.add(PlaceholderAPI.setPlaceholders(offlinePlayer, line));
        }
        return lores;
    }

    /**
     * Replace placeholders in lore lines for an online player.
     * 
     * @param lore   The list of lore lines.
     * @param player The online player.
     * @return A list of lore lines with placeholders applied.
     */
    private static List<String> replacePlaceholders(List<String> lore, Player player) {
        List<String> result = new ArrayList<>();
        for (String s : lore) {
            result.add(PlaceholderAPI.setPlaceholders(player, s));
        }
        return result;
    }

    /**
     * Replace placeholders in lore lines for an offline player.
     * 
     * @param lore   The list of lore lines.
     * @param player The offline player.
     * @return A list of lore lines with placeholders applied.
     */
    private static List<String> replaceOfflinePlaceholders(List<String> lore, OfflinePlayer player) {
        List<String> result = new ArrayList<>();
        for (String s : lore) {
            result.add(PlaceholderAPI.setPlaceholders(player, s));
        }
        return result;
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
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create a custom item with model data.
     * 
     * @param name          The display name of the item.
     * @param lore          The lore of the item.
     * @param customItemName The custom item name.
     * @param modelData     The custom model data.
     * @return The created ItemStack.
     */
    private ItemStack createItemWMD(String name, List<String> lore, String customItemName, int modelData) {
        CustomStack customStack = CustomStack.getInstance(customItemName);
        ItemStack item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(modelData);
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create an item for navigating to the previous page.
     * 
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack backPage(int page) {
        return createNavigationItem("back-page-list", page);
    }

    /**
     * Create an item for navigating to the next page.
     * 
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack nextPage(int page) {
        return createNavigationItem("next-page-list", page);
    }

    /**
     * Create the filter item.
     * 
     * @param filter The current filter.
     * @return The created ItemStack.
     */
    private ItemStack filterItem(String filter) {
        String loreFilter = ClaimLanguage.getMessage("filter-new-lore");
        loreFilter = loreFilter.replaceAll("%status_color_" + getStatusIndex(filter) + "%", ClaimLanguage.getMessage("status_color_active_filter"));
        loreFilter = loreFilter.replaceAll("%status_color_[^" + getStatusIndex(filter) + "]%", ClaimLanguage.getMessage("status_color_inactive_filter"));

        return createItem(ClaimGuis.getItemMaterial("claims", "filter"),
                ClaimLanguage.getMessage("filter-title"),
                getLore(loreFilter));
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
     * Create a navigation item.
     * 
     * @param key  The key for the item.
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack createNavigationItem(String key, int page) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("claims", key)) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", key));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("claims", key);
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage(key + "-title").replace("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage(key + "-lore").replace("%page%", String.valueOf(page))));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Set an item in the inventory asynchronously.
     * 
     * @param slot     The slot index.
     * @param condition The condition to check.
     * @param item     The item to set.
     */
    private void setItemAsync(int slot, boolean condition, ItemStack item) {
        if (condition) {
            setItemAsync(slot, item);
        }
    }

    /**
     * Set an item in the inventory asynchronously.
     * 
     * @param slot The slot index.
     * @param item The item to set.
     */
    private void setItemAsync(int slot, ItemStack item) {
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(slot, item));
        } else {
            Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(slot, item));
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    /**
     * Open the inventory for the player.
     * 
     * @param player The player to open the inventory for.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}
