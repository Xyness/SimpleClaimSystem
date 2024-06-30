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
import fr.xyness.SCS.Others.MinecraftSkinUtil;
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
        SimpleClaimSystem.executeAsync(this::loadItems);
    }
    
    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Load items into the inventory.
     */
    private void loadItems() {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setFilter(filter);
        cPlayer.clearMapString();

        if (page > 1) {
            inv.setItem(ClaimGuis.getItemSlot("claims", "back-page-list"), createNavigationItem("back-page-list", page - 1));
        }

        List<String> loreTemplate = getLore(ClaimLanguage.getMessage("owner-claim-lore"));
        Map<String, Integer> owners = getOwnersByFilter(filter);
        inv.setItem(ClaimGuis.getItemSlot("claims", "filter"), createFilterItem(filter));

        int startItem = (page - 1) * itemsPerPage;
        int i = minSlot;
        int count = 0;

        for (Map.Entry<String, Integer> entry : owners.entrySet()) {
            if (count++ < startItem) continue;
            if (i > maxSlot) {
                inv.setItem(ClaimGuis.getItemSlot("claims", "next-page-list"), createNavigationItem("next-page-list", page + 1));
                break;
            }

            String owner = entry.getKey();
            int claimAmount = entry.getValue();

            List<String> lore = getLoreWithPlaceholders(loreTemplate, owner, claimAmount);
            cPlayer.addMapString(i, owner);
            inv.setItem(i, createOwnerClaimItem(owner, lore));
            i++;
        }

        setCustomItems();

        SimpleClaimSystem.executeSync(() -> player.openInventory(inv));
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
            return createItemWithModelData(title, lore, ClaimGuis.getItemMaterialMD("claims", "claim-item"), ClaimGuis.getItemCustomModelData("claims", "claim-item"));
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
        ItemStack item = MinecraftSkinUtil.createPlayerHead(owner);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
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
            return createItemWithModelData(title, lore, ClaimGuis.getCustomItemMaterialMD("claims", key), ClaimGuis.getCustomItemCustomModelData("claims", key));
        } else {
            return createItem(ClaimGuis.getCustomItemMaterial("claims", key), title, lore);
        }
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
     * Create an item with model data.
     * 
     * @param name          The display name of the item.
     * @param lore          The lore of the item.
     * @param customItemName The custom item name.
     * @param modelData     The custom model data.
     * @return The created ItemStack.
     */
    private ItemStack createItemWithModelData(String name, List<String> lore, String customItemName, int modelData) {
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
     * Create a filter item.
     * 
     * @param filter The current filter.
     * @return The created ItemStack.
     */
    private ItemStack createFilterItem(String filter) {
        String loreFilter = ClaimLanguage.getMessage("filter-new-lore")
            .replaceAll("%status_color_" + getStatusIndex(filter) + "%", ClaimLanguage.getMessage("status_color_active_filter"))
            .replaceAll("%status_color_[^" + getStatusIndex(filter) + "]%", ClaimLanguage.getMessage("status_color_inactive_filter"));
        return createItem(ClaimGuis.getItemMaterial("claims", "filter"), ClaimLanguage.getMessage("filter-title"), getLore(loreFilter));
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
     * Set custom items in the inventory.
     */
    private void setCustomItems() {
        Set<String> customItems = new HashSet<>(ClaimGuis.getCustomItems("claims"));
        for (String key : customItems) {
            List<String> lore = getLoreP(ClaimGuis.getCustomItemLore("claims", key), player);
            String title = ClaimGuis.getCustomItemTitle("claims", key);
            if (ClaimSettings.getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            inv.setItem(ClaimGuis.getCustomItemSlot("claims", key), createCustomItem(key, title, lore));
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
        for (String s : lore.split("\n")) {
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
        if (offlinePlayer == null) return lores;
        for (String line : lore) {
            lores.add(PlaceholderAPI.setPlaceholders(offlinePlayer, line));
        }
        return lores;
    }

    /**
     * Get lore with placeholders replaced.
     * 
     * @param template    The lore template.
     * @param owner       The owner of the claim.
     * @param claimAmount The claim amount.
     * @return A list of lore lines with placeholders replaced.
     */
    private List<String> getLoreWithPlaceholders(List<String> template, String owner, int claimAmount) {
        List<String> lore = new ArrayList<>();
        for (String s : template) {
            lore.add(s.replace("%claim-amount%", String.valueOf(claimAmount)));
        }
        lore.add(ClaimLanguage.getMessage("owner-claim-access"));
        return getLoreWP(lore, owner);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
