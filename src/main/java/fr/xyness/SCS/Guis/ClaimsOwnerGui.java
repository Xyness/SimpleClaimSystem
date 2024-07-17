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
 * Class representing the Claims Owner GUI.
 */
public class ClaimsOwnerGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** The inventory for this GUI. */
    private final Inventory inv;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
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
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimsOwnerGui(Player player, int page, String filter, String owner, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = getTitle(player, page, owner);
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("claims_owner") * 9, title);
        instance.executeAsync(() -> loadItems(player, page, filter, owner));
    }

    /**
     * Get the title of the GUI, replacing placeholders.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param owner  The owner of the claims.
     * @return The title of the GUI.
     */
    private String getTitle(Player player, int page, String owner) {
        String title = instance.getGuis().getGuiTitle("claims_owner")
            .replace("%page%", String.valueOf(page))
            .replace("%owner%", owner);
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        return title;
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
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        cPlayer.setOwner(owner);
        cPlayer.setFilter(filter);
        cPlayer.clearMapClaim();
        cPlayer.clearMapLoc();

        setNavigationItems(page);
        Set<Claim> claims = getClaims(filter, owner);
        setFilterItem(filter);

        List<String> loreTemplate = instance.getGuis().getLore(instance.getLanguage().getMessage("access-all-claim-lore"));
        fillClaimsItems(player, cPlayer, page, loreTemplate, claims);
        setCustomItems(player);
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }

    /**
     * Get the claims based on the filter and owner.
     * 
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     * @return A map of claims and their corresponding chunks.
     */
    private Set<Claim> getClaims(String filter, String owner) {
        return "sales".equals(filter) ? instance.getMain().getClaimsInSale(owner) : instance.getMain().getPlayerClaims(owner);
    }

    /**
     * Set the navigation items (back page).
     * 
     * @param page The current page of the GUI.
     */
    private void setNavigationItems(int page) {
        inv.setItem(instance.getGuis().getItemSlot("claims_owner", "back-page-list"), createNavigationItem("back-page-list", page > 1 ? page - 1 : 0));
        if (page > 1) {
            inv.setItem(instance.getGuis().getItemSlot("claims_owner", "back-page-claims"), createNavigationItem("back-page-claims", 0));
        }
    }

    /**
     * Set the filter item.
     * 
     * @param filter The filter applied to the claims.
     */
    private void setFilterItem(String filter) {
        inv.setItem(instance.getGuis().getItemSlot("claims_owner", "filter"), createFilterItem(filter));
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
    private void fillClaimsItems(Player player, CPlayer cPlayer, int page, List<String> loreTemplate, Set<Claim> claims) {
        int minSlot = instance.getGuis().getGuiMinSlot("claims_owner");
        int maxSlot = instance.getGuis().getGuiMaxSlot("claims_owner");
        int itemsPerPage = maxSlot - minSlot + 1;
        int startIndex = (page - 1) * itemsPerPage;
        int slotIndex = minSlot;
        int itemCount = 0;

        for (Claim claim : claims) {
            if (itemCount++ < startIndex) continue;
            if (slotIndex > maxSlot) {
                inv.setItem(instance.getGuis().getItemSlot("claims_owner", "next-page-list"), createNavigationItem("next-page-list", page + 1));
                break;
            }
            if (claim.getPermission("Visitors") && !instance.getSettings().getBooleanSetting("claims-visitors-off-visible")) continue;
            OfflinePlayer target = instance.getPlayerMain().getOfflinePlayer(claim.getOwner());
            List<String> lore = prepareLore(loreTemplate, claim, player, target);
            ItemStack item = createClaimItem(claim, player, lore, target);
            cPlayer.addMapClaim(slotIndex, claim);
            cPlayer.addMapLoc(slotIndex, claim.getLocation());
            inv.setItem(slotIndex++, item);
        }
    }

    /**
     * Set the custom items in the GUI.
     * 
     * @param player The player who opened the GUI.
     */
    private void setCustomItems(Player player) {
        for (String key : instance.getGuis().getCustomItems("claims_owner")) {
            List<String> lore = instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("claims_owner", key), player);
            String title = instance.getGuis().getCustomItemTitle("claims_owner", key);
            if (instance.getSettings().getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            inv.setItem(instance.getGuis().getCustomItemSlot("claims_owner", key), createCustomItem(key, title, lore));
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
    private List<String> prepareLore(List<String> template, Claim claim, Player player, OfflinePlayer target) {
        List<String> lore = new ArrayList<>();
        for (String line : template) {
            line = line.replace("%description%", claim.getDescription())
                .replace("%name%", claim.getName())
                .replace("%coords%", instance.getMain().getClaimCoords(claim));
            if (line.contains("%members%")) {
                lore.addAll(Arrays.asList(getMembers(claim).split("\n")));
            } else {
                lore.add(line);
            }
        }
        addEconomyLore(player, claim, lore, target);
        addVisitorLore(claim, lore, player);
        return instance.getGuis().getLoreWP(lore, claim.getOwner(), target);
    }

    /**
     * Add the economy lore if enabled.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim object.
     * @param lore   The lore list to modify.
     */
    private void addEconomyLore(Player player, Claim claim, List<String> lore, OfflinePlayer target) {
        if (instance.getSettings().getBooleanSetting("economy") && claim.getSale()) {
            Collections.addAll(lore, instance.getLanguage().getMessageWP("all-claim-buyable-price", target)
                .replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
                .replace("%money-symbol%",instance.getLanguage().getMessage("money-symbol"))
                .split("\n"));
            lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.buy") ? instance.getLanguage().getMessage("all-claim-is-buyable") : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-buy"));
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
        String visitorMessage = claim.getPermission("Visitors") || claim.getOwner().equals(player.getName()) ? 
            instance.getLanguage().getMessage("access-all-claim-lore-allow-visitors") : 
            instance.getLanguage().getMessage("access-all-claim-lore-deny-visitors");
        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp") ? visitorMessage : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-teleport"));
    }
    
    /**
     * Create a claim item for the GUI.
     * 
     * @param claim      The claim object.
     * @param player     The player who opened the GUI.
     * @param lore       The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createClaimItem(Claim claim, Player player, List<String> lore, OfflinePlayer target) {
        String displayName = instance.getLanguage().getMessageWP("access-all-claim-title", target)
            .replace("%owner%", claim.getOwner())
            .replace("%name%", claim.getName())
            .replace("%coords%", instance.getMain().getClaimCoords(claim));

        if (instance.getGuis().getItemCheckCustomModelData("claims_owner", "claim-item")) {
            return createItemWMD(displayName, lore, instance.getGuis().getItemMaterialMD("claims_owner", "claim-item"), instance.getGuis().getItemCustomModelData("claims_owner", "claim-item"));
        }
        if (instance.getGuis().getItemMaterialMD("claims_owner", "claim-item").contains("PLAYER_HEAD")) {
            return createPlayerHeadItem(claim, displayName, lore, target);
        }
        return createItem(instance.getGuis().getItemMaterial("claims_owner", "claim-item"), displayName, lore);
    }

    /**
     * Create a claim item with a PLAYER_HEAD material.
     * 
     * @param claim        The claim object.
     * @param displayName  The display name for the item.
     * @param lore         The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createPlayerHeadItem(Claim claim, String displayName, List<String> lore, OfflinePlayer target) {
    	ItemStack item = instance.getPlayerMain().getPlayerHead(target);
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
        if (instance.getGuis().getCustomItemCheckCustomModelData("claims_owner", key)) {
            return createItemWMD(title, lore, instance.getGuis().getCustomItemMaterialMD("claims_owner", key), instance.getGuis().getCustomItemCustomModelData("claims_owner", key));
        } else {
            return createItem(instance.getGuis().getCustomItemMaterial("claims_owner", key), title, lore);
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
        ItemStack item = createItem(instance.getGuis().getItemMaterial("claims_owner", key), null, null);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (page == 0) {
                meta.setDisplayName(instance.getLanguage().getMessage("previous-page-title"));
                meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("previous-page-claims-lore")));
            } else {
                meta.setDisplayName(instance.getLanguage().getMessage(key.equals("next-page-list") ? "next-page-title" : "previous-page-title").replace("%page%", String.valueOf(page)));
                meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage(key.equals("next-page-list") ? "next-page-lore" : "previous-page-lore").replace("%page%", String.valueOf(page))));
            }
            meta = instance.getGuis().setItemFlag(meta);
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
        String loreFilter = getFilterLore(filter);
        return createItem(instance.getGuis().getItemMaterial("claims_owner", "filter"),
                instance.getLanguage().getMessage("filter-title"),
                instance.getGuis().getLore(loreFilter));
    }

    /**
     * Get the lore of the filter item.
     * 
     * @param filter The filter applied to the claims.
     * @return The lore of the filter item.
     */
    private String getFilterLore(String filter) {
        String loreFilter = instance.getLanguage().getMessage("filter-owner-lore");
        if ("sales".equals(filter)) {
            loreFilter = loreFilter.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_inactive_filter"))
                .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_active_filter"));
        } else {
            loreFilter = loreFilter.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_active_filter"))
                .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_inactive_filter"));
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
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta = instance.getGuis().setItemFlag(meta);
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
            meta = instance.getGuis().setItemFlag(meta);
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
    public String getMembers(Claim claim) {
        Set<String> members = claim.getMembers();
        if (members.isEmpty()) {
            return instance.getLanguage().getMessage("claim-list-no-member");
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

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
