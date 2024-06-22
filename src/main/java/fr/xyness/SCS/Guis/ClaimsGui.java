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

public class ClaimsGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************
	

    private final Inventory inv;
    private final Player player;
    private final int page;
    private final String filter;
    private final int itemsPerPage;
    private final int minMemberSlot;
    private final int maxMemberSlot;
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimsGui(Player player, int page, String filter) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.minMemberSlot = ClaimGuis.getGuiMinSlot("claims");
        this.maxMemberSlot = ClaimGuis.getGuiMaxSlot("claims");
        this.itemsPerPage = maxMemberSlot - minMemberSlot + 1;

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

    
    // Method to initialize items (in async mode)
    private void initializeItems() {
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> loadItems());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> loadItems());
        }
    }

    // Method to load items
    private void loadItems() {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setFilter(filter);
        cPlayer.clearMapString();

        setItemAsync(ClaimGuis.getItemSlot("claims", "back-page-list"), page > 1, backPage(page - 1));

        List<String> loreTemplate = getLore(ClaimLanguage.getMessage("owner-claim-lore"));
        Map<String, Integer> owners = getOwnersByFilter(filter);
        setItemAsync(ClaimGuis.getItemSlot("claims", "filter"), true, filterItem(filter));

        int startItem = (page - 1) * itemsPerPage;
        int i = minMemberSlot;
        int count = 0;

        for (Map.Entry<String, Integer> entry : owners.entrySet()) {
            String owner = entry.getKey();
            int claimAmount = entry.getValue();

            if (count++ < startItem) continue;
            if (i > maxMemberSlot) {
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
    }

    // Method to get the owners with the filter
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

    // Method to create the item for the gui
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

    // Method to create the item (if PLAYER_HEAD) for the gui
    private ItemStack createPlayerHeadItem(String owner, String title, List<String> lore) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setOwningPlayer(Bukkit.getOfflinePlayerIfCached(owner));
        meta.setDisplayName(title);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Method to create custom item (ItemsAdder)
    private ItemStack createCustomItem(String key, String title, List<String> lore) {
        if (ClaimGuis.getCustomItemCheckCustomModelData("claims", key)) {
            return createItemWMD(title, lore, ClaimGuis.getCustomItemMaterialMD("claims", key), ClaimGuis.getCustomItemCustomModelData("claims", key));
        } else {
            return createItem(ClaimGuis.getCustomItemMaterial("claims", key), title, lore);
        }
    }

    // Method to get split the lore in list
    public static List<String> getLore(String lore) {
        return Arrays.asList(lore.split("\n"));
    }

    // Method to get lore with placeholders (lore in string)
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

    // Method to get lore with placeholders (lore in list)
    public static List<String> getLoreWP(List<String> lore, String player) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) return lore;
        Player p = Bukkit.getPlayer(player);
        return p == null ? replaceOfflinePlaceholders(lore, Bukkit.getOfflinePlayerIfCached(player)) : replacePlaceholders(lore, p);
    }

    // Method to apply placeholders (for online player)
    private static List<String> replacePlaceholders(List<String> lore, Player player) {
        List<String> result = new ArrayList<>();
        for (String s : lore) {
            result.add(PlaceholderAPI.setPlaceholders(player, s));
        }
        return result;
    }

    // Method to apply placeholders (for offline player)
    private static List<String> replaceOfflinePlaceholders(List<String> lore, OfflinePlayer player) {
        List<String> result = new ArrayList<>();
        for (String s : lore) {
            result.add(PlaceholderAPI.setPlaceholders(player, s));
        }
        return result;
    }

    // Method to create item
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

    // Method to create custom item
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

    // Method to get the back page item
    private ItemStack backPage(int page) {
        return createNavigationItem("back-page-list", page);
    }

    // Method to get the next page item
    private ItemStack nextPage(int page) {
        return createNavigationItem("next-page-list", page);
    }

    // Method to get the filter item
    private ItemStack filterItem(String filter) {
        String loreFilter = ClaimLanguage.getMessage("filter-new-lore");
        loreFilter = loreFilter.replaceAll("%status_color_" + getStatusIndex(filter) + "%", ClaimLanguage.getMessage("status_color_active_filter"));
        loreFilter = loreFilter.replaceAll("%status_color_[^" + getStatusIndex(filter) + "]%", ClaimLanguage.getMessage("status_color_inactive_filter"));

        return createItem(ClaimGuis.getItemMaterial("claims", "filter"),
                ClaimLanguage.getMessage("filter-title"),
                getLore(loreFilter));
    }

    // Method to get index of filter
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

    // Method to create the navigation item
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

    // Method to set the item in sync mode
    private void setItemAsync(int slot, boolean condition, ItemStack item) {
        if (condition) {
            setItemAsync(slot, item);
        }
    }

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

    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}
