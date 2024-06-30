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
 * Class representing the Claims Owner GUI.
 */
public class ClaimsOwnerGui implements InventoryHolder {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** The inventory for this GUI. */
    private final Inventory inv;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Main constructor for ClaimsOwnerGui.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     */
    public ClaimsOwnerGui(Player player, int page, String filter, String owner) {
        String title = getTitle(player, page, owner);
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims_owner") * 9, title);
        initializeItems(player, page, filter, owner);
    }
    
    // ********************
    // *  Others Methods  *
    // ********************
    
    /**
     * Get the title of the GUI, replacing placeholders.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param owner  The owner of the claims.
     * @return The title of the GUI.
     */
    private String getTitle(Player player, int page, String owner) {
        String title = ClaimGuis.getGuiTitle("claims_owner")
            .replace("%page%", String.valueOf(page))
            .replace("%owner%", owner);
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        return title;
    }

    /**
     * Initialize items in the GUI asynchronously.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     */
    private void initializeItems(Player player, int page, String filter, String owner) {
        Runnable loadItemsTask = () -> loadItems(player, page, filter, owner);
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> loadItemsTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), loadItemsTask);
        }
    }

    /**
     * Load items into the inventory.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     */
    private void loadItems(Player player, int page, String filter, String owner) {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setOwner(owner);
        cPlayer.setFilter(filter);
        cPlayer.clearMapChunk();
        cPlayer.clearMapLoc();

        setNavigationItems(page);
        Map<Chunk, Claim> claims = getClaims(filter, owner);
        setFilterItem(filter);

        List<String> loreTemplate = getLore(ClaimLanguage.getMessage("access-all-claim-lore"));
        fillClaimsItems(player, cPlayer, page, loreTemplate, claims);
        setCustomItems(player);
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
    }

    /**
     * Get the claims based on the filter and owner.
     * 
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     * @return A map of claims and their corresponding chunks.
     */
    private Map<Chunk, Claim> getClaims(String filter, String owner) {
        return filter.equals("sales") ? ClaimMain.getChunksInSaleFromOwner(owner) : ClaimMain.getChunksFromOwnerGui(owner);
    }

    /**
     * Set the navigation items (back page).
     * 
     * @param page The current page of the GUI.
     */
    private void setNavigationItems(int page) {
        if (page > 1) {
            setItemAsync(ClaimGuis.getItemSlot("claims_owner", "back-page-list"), createNavigationItem("back-page-list", page - 1));
        } else {
            setItemAsync(ClaimGuis.getItemSlot("claims_owner", "back-page-claims"), createNavigationItem("back-page-claims", 0));
        }
    }

    /**
     * Set the filter item.
     * 
     * @param filter The filter applied to the claims.
     */
    private void setFilterItem(String filter) {
        setItemAsync(ClaimGuis.getItemSlot("claims_owner", "filter"), createFilterItem(filter));
    }

    /**
     * Fill the claims items in the GUI.
     * 
     * @param player       The player who opened the GUI.
     * @param cPlayer      The custom player object.
     * @param page         The current page of the GUI.
     * @param loreTemplate The lore template for the items.
     * @param claims       A map of claims and their corresponding chunks.
     */
    private void fillClaimsItems(Player player, CPlayer cPlayer, int page, List<String> loreTemplate, Map<Chunk, Claim> claims) {
        int minSlot = ClaimGuis.getGuiMinSlot("claims_owner");
        int maxSlot = ClaimGuis.getGuiMaxSlot("claims_owner");
        int itemsPerPage = maxSlot - minSlot + 1;
        int startIndex = (page - 1) * itemsPerPage;
        int slotIndex = minSlot;
        int itemCount = 0;

        for (Map.Entry<Chunk, Claim> entry : claims.entrySet()) {
            if (itemCount++ < startIndex) continue;
            if (slotIndex > maxSlot) {
                setItemAsync(ClaimGuis.getItemSlot("claims_owner", "next-page-list"), createNavigationItem("next-page-list", page + 1));
                break;
            }
            Chunk chunk = entry.getKey();
            Claim claim = entry.getValue();
            if (claim.getPermission("Visitor") && !ClaimSettings.getBooleanSetting("claims-visitors-off-visible")) continue;

            List<String> lore = prepareLore(loreTemplate, claim, player);
            ItemStack item = createClaimItem(claim, player, lore);
            cPlayer.addMapChunk(slotIndex, chunk);
            cPlayer.addMapLoc(slotIndex, ClaimMain.getClaimLocationByChunk(chunk));
            setItemAsync(slotIndex++, item);
        }
    }

    /**
     * Set the custom items in the GUI.
     * 
     * @param player The player who opened the GUI.
     */
    private void setCustomItems(Player player) {
        Set<String> customItems = new HashSet<>(ClaimGuis.getCustomItems("claims_owner"));
        for (String key : customItems) {
            List<String> lore = getLoreP(ClaimGuis.getCustomItemLore("claims_owner", key), player);
            String title = ClaimGuis.getCustomItemTitle("claims_owner", key);
            if (ClaimSettings.getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            setItemAsync(ClaimGuis.getCustomItemSlot("claims_owner", key), createCustomItem(key, title, lore));
        }
    }

    /**
     * Prepare the lore of the items.
     * 
     * @param template The lore template.
     * @param claim    The claim object.
     * @param player   The player who opened the GUI.
     * @return A list of lore lines.
     */
    private List<String> prepareLore(List<String> template, Claim claim, Player player) {
        List<String> lore = new ArrayList<>();
        for (String line : template) {
            line = line.replace("%description%", claim.getDescription())
                .replace("%name%", claim.getName())
                .replace("%coords%", ClaimMain.getClaimCoords(claim));
            if (line.contains("%members%")) {
                lore.addAll(Arrays.asList(getMembers(claim).split("\n")));
            } else {
                lore.add(line);
            }
        }
        addEconomyLore(player, claim, lore);
        addVisitorLore(claim, lore, player);
        return getLoreWP(lore, claim.getOwner());
    }

    /**
     * Add the economy lore if enabled.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim object.
     * @param lore   The lore list to modify.
     */
    private void addEconomyLore(Player player, Claim claim, List<String> lore) {
        if (ClaimSettings.getBooleanSetting("economy") && claim.getSale()) {
            Collections.addAll(lore, ClaimLanguage.getMessageWP("all-claim-buyable-price", claim.getOwner())
                .replace("%price%", String.valueOf(claim.getPrice()))
                .split("\n"));
            lore.add(CPlayerMain.checkPermPlayer(player, "scs.command.sclaim") ? ClaimLanguage.getMessage("all-claim-is-buyable") : ClaimLanguage.getMessage("gui-button-no-permission") + " to buy");
        }
    }

    /**
     * Add the visitor lore if enabled.
     * 
     * @param claim  The claim object.
     * @param lore   The lore list to modify.
     * @param player The player who opened the GUI.
     */
    private void addVisitorLore(Claim claim, List<String> lore, Player player) {
        String visitorMessage = ClaimMain.canPermCheck(claim.getChunk(), "Visitors") || claim.getOwner().equals(player.getName()) ? 
            ClaimLanguage.getMessage("access-all-claim-lore-allow-visitors") : 
            ClaimLanguage.getMessage("access-all-claim-lore-deny-visitors");
        lore.add(CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp") ? visitorMessage : ClaimLanguage.getMessage("gui-button-no-permission") + " to teleport");
    }
    
    /**
     * Create a claim item for the GUI.
     * 
     * @param claim      The claim object.
     * @param player     The player who opened the GUI.
     * @param lore       The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createClaimItem(Claim claim, Player player, List<String> lore) {
        String displayName = ClaimLanguage.getMessageWP("access-all-claim-title", claim.getOwner())
            .replace("%owner%", claim.getOwner())
            .replace("%name%", claim.getName())
            .replace("%coords%", ClaimMain.getClaimCoords(claim));

        if (ClaimGuis.getItemCheckCustomModelData("claims_owner", "claim-item")) {
            return createItemWMD(displayName, lore, ClaimGuis.getItemMaterialMD("claims_owner", "claim-item"), ClaimGuis.getItemCustomModelData("claims_owner", "claim-item"));
        }
        if (ClaimGuis.getItemMaterialMD("claims_owner", "claim-item").contains("PLAYER_HEAD")) {
            return createPlayerHeadItem(claim, displayName, lore);
        }
        return createItem(ClaimGuis.getItemMaterial("claims_owner", "claim-item"), displayName, lore);
    }

    /**
     * Create a claim item with a PLAYER_HEAD material.
     * 
     * @param claim        The claim object.
     * @param displayName  The display name for the item.
     * @param lore         The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createPlayerHeadItem(Claim claim, String displayName, List<String> lore) {
        ItemStack item = MinecraftSkinUtil.createPlayerHead(claim.getOwner());
        SkullMeta meta = (SkullMeta) item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create a custom item.
     * 
     * @param key    The key of the custom item.
     * @param title  The title of the custom item.
     * @param lore   The lore for the custom item.
     * @return The created ItemStack.
     */
    private ItemStack createCustomItem(String key, String title, List<String> lore) {
        if (ClaimGuis.getCustomItemCheckCustomModelData("claims_owner", key)) {
            return createItemWMD(title, lore, ClaimGuis.getCustomItemMaterialMD("claims_owner", key), ClaimGuis.getCustomItemCustomModelData("claims_owner", key));
        } else {
            return createItem(ClaimGuis.getCustomItemMaterial("claims_owner", key), title, lore);
        }
    }

    /**
     * Create a navigation item.
     * 
     * @param key  The key of the navigation item.
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack createNavigationItem(String key, int page) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("claims_owner", key)) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims_owner", key));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("claims_owner", key);
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (page == 0) {
                meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title"));
                meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-claims-lore")));
            } else {
                meta.setDisplayName(ClaimLanguage.getMessage(key.equals("next-page-list") ? "next-page-title" : "previous-page-title").replace("%page%", String.valueOf(page)));
                meta.setLore(getLore(ClaimLanguage.getMessage(key.equals("next-page-list") ? "next-page-lore" : "previous-page-lore").replace("%page%", String.valueOf(page))));
            }
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Create the filter item.
     * 
     * @param filter The filter applied to the claims.
     * @return The created ItemStack.
     */
    private ItemStack createFilterItem(String filter) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("claims_owner", "filter")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims_owner", "filter"));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("claims_owner", "filter");
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = getFilterLore(filter);
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(loreFilter));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get the lore of the filter item.
     * 
     * @param filter The filter applied to the claims.
     * @return The lore of the filter item.
     */
    private String getFilterLore(String filter) {
        String loreFilter = ClaimLanguage.getMessage("filter-owner-lore");
        if (filter.equals("sales")) {
            loreFilter = loreFilter.replace("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                .replace("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"));
        } else {
            loreFilter = loreFilter.replace("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
                .replace("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"));
        }
        return loreFilter;
    }

    /**
     * Create an item.
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
     * Get the members of a claim as a string with new lines.
     * 
     * @param claim The claim object.
     * @return A string representing the members of the claim.
     */
    public static String getMembers(Claim claim) {
        Set<String> members = claim.getMembers();
        if (members.isEmpty()) {
            return ClaimLanguage.getMessage("claim-list-no-member");
        }
        StringBuilder membersList = new StringBuilder();
        int i = 0;
        for (String member : members) {
            Player player = Bukkit.getPlayer(member);
            String memberName = player != null ? "§a" + member : "§c" + member;
            membersList.append(memberName);
            if (i < members.size() - 1) {
                membersList.append("§7, ");
            }
            if ((i + 1) % 4 == 0 && i < members.size() - 1) {
                membersList.append("\n");
            }
            i++;
        }
        return membersList.toString();
    }

    /**
     * Split a lore string into a list of strings.
     * 
     * @param lore The lore string.
     * @return A list of lore lines.
     */
    public static List<String> getLore(String lore) {
        return Arrays.asList(lore.split("\n"));
    }

    /**
     * Split a lore string into a list of strings with placeholders replaced.
     * 
     * @param lore   The lore string.
     * @param player The player for whom to replace placeholders.
     * @return A list of lore lines with placeholders replaced.
     */
    public static List<String> getLoreP(String lore, Player player) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) {
            return getLore(lore);
        }
        List<String> lores = new ArrayList<>();
        for (String line : lore.split("\n")) {
            lores.add(PlaceholderAPI.setPlaceholders(player, line));
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
     * Set an item in the inventory asynchronously.
     * 
     * @param slot The slot in the inventory.
     * @param item The item to set in the slot.
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
     * Open the inventory for a player.
     * 
     * @param player The player to open the inventory for.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}
