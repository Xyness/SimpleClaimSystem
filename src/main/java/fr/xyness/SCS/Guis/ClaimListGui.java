package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Others.MinecraftSkinUtil;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Class representing the Claim List GUI.
 */
public class ClaimListGui implements InventoryHolder {

    // ***************
    // *  Variables  *
    // ***************

    /** Inventory for the GUI. */
    private Inventory inv;

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Main constructor for the ClaimListGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the list.
     */
    public ClaimListGui(Player player, int page, String filter) {
        String title = ClaimGuis.getGuiTitle("list").replaceAll("%page%", String.valueOf(page));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("list") * 9, title);
        SimpleClaimSystem.executeAsync(() -> loadItems(player, page, filter));
    }

    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the list.
     */
    public void loadItems(Player player, int page, String filter) {

        int min_member_slot = ClaimGuis.getGuiMinSlot("list");
        int max_member_slot = ClaimGuis.getGuiMaxSlot("list");
        int items_count = max_member_slot - min_member_slot + 1;
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        cPlayer.setFilter(filter);
        cPlayer.clearMapChunk();
        cPlayer.clearMapLoc();

        String lore_tp = CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")
                ? ClaimLanguage.getMessage("access-claim-clickable-tp")
                : ClaimLanguage.getMessage("gui-button-no-permission") + " to teleport";
        String lore_remove = CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")
                ? ClaimLanguage.getMessage("access-claim-clickable-remove")
                : ClaimLanguage.getMessage("gui-button-no-permission") + " to remove";
        String lore_settings = CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")
                ? ClaimLanguage.getMessage("access-claim-clickable-settings")
                : ClaimLanguage.getMessage("gui-button-no-permission") + " to open settings";
        String lore_sale = CPlayerMain.checkPermPlayer(player, "scs.command.sclaim")
                ? ClaimLanguage.getMessage("access-claim-clickable-cancelsale")
                : ClaimLanguage.getMessage("gui-button-no-permission") + " to cancel sale";

        if (page > 1) {
            inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage(page - 1));
        } else if (cPlayer.getChunk() != null) {
            inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage2(cPlayer.getChunk()));
        }

        inv.setItem(ClaimGuis.getItemSlot("list", "filter"), createFilterItem(filter));

        Map<Chunk, Claim> claims;
        List<String> lore;
        if (filter.equals("owner")) {
            claims = new HashMap<>(ClaimMain.getChunksFromOwnerGui(playerName));
            lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-lore", playerName)));
        } else {
            claims = new HashMap<>(ClaimMain.getChunksWhereMemberNotOwner(playerName));
            lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-not-owner-lore", playerName)));
        }
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (Chunk c : claims.keySet()) {
            if (count++ < startItem)
                continue;
            if (i == max_member_slot + 1) {
                inv.setItem(ClaimGuis.getItemSlot("list", "next-page-list"), nextPage(page + 1));
                break;
            }
            Claim claim = claims.get(c);
            cPlayer.addMapChunk(i, c);
            cPlayer.addMapLoc(i, claim.getLocation());
            List<String> used_lore = new ArrayList<>();
            for (String s : lore) {
                s = s.replaceAll("%owner%", claim.getOwner());
                s = s.replaceAll("%description%", claim.getDescription());
                s = s.replaceAll("%name%", claim.getName()).replaceAll("%coords%", ClaimMain.getClaimCoords(claim));
                if (s.contains("%members%")) {
                    String members = getMembers(claim);
                    if (members.contains("\n")) {
                        String[] parts = members.split("\n");
                        for (String ss : parts) {
                            used_lore.add(ss);
                        }
                    } else {
                        used_lore.add(members);
                    }
                } else {
                    used_lore.add(s);
                }
            }
            if (filter.equals("owner")) {
                if (ClaimSettings.getBooleanSetting("economy")) {
                    if (ClaimMain.claimIsInSale(c)) {
                        String[] m = ClaimLanguage.getMessageWP("my-claims-buyable-price", playerName)
                                .replaceAll("%price%", String.valueOf(claim.getPrice())).split("\n");
                        for (String part : m) {
                            used_lore.add(part);
                        }
                        used_lore.addAll(Arrays.asList(lore_tp, lore_remove, lore_settings));
                        used_lore.add(lore_sale);
                    } else {
                        used_lore.addAll(Arrays.asList(lore_tp, lore_remove, lore_settings));
                    }
                } else {
                    used_lore.addAll(Arrays.asList(lore_tp, lore_remove, lore_settings));
                }
            } else {
                used_lore.add(lore_tp);
            }
            final int i_f = i;
            if (ClaimGuis.getItemCheckCustomModelData("list", "claim-item")) {
                inv.setItem(i_f,
                        createItemWMD(
                                ClaimLanguage.getMessageWP("access-claim-title", playerName)
                                        .replaceAll("%name%", claim.getName())
                                        .replaceAll("%coords%", ClaimMain.getClaimCoords(claim)),
                                used_lore, ClaimGuis.getItemMaterialMD("list", "claim-item"),
                                ClaimGuis.getItemCustomModelData("list", "claim-item")));
                i++;
                continue;
            }
            if (ClaimGuis.getItemMaterialMD("list", "claim-item").contains("PLAYER_HEAD")) {
                ItemStack item = MinecraftSkinUtil.createPlayerHead(playerName);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setDisplayName(ClaimLanguage.getMessageWP("access-claim-title", playerName).replaceAll("%name%", claim.getName()).replaceAll("%coords%", ClaimMain.getClaimCoords(claim)));
                meta.setLore(used_lore);
                item.setItemMeta(meta);
                inv.setItem(i_f, item);
                i++;
                continue;
            }
            inv.setItem(i_f,
                    createItem(ClaimGuis.getItemMaterial("list", "claim-item"),
                            ClaimLanguage.getMessageWP("access-claim-title", playerName)
                                    .replaceAll("%name%", claim.getName()).replaceAll("%coords%", ClaimMain.getClaimCoords(claim)),
                            used_lore));
            i++;
        }

        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("list"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(getLoreWP(ClaimGuis.getCustomItemLore("list", key), player));
            String title = ClaimSettings.getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, ClaimGuis.getCustomItemTitle("list", key)) : ClaimGuis.getCustomItemTitle("list", key);
            if (ClaimGuis.getCustomItemCheckCustomModelData("list", key)) {
                inv.setItem(ClaimGuis.getCustomItemSlot("list", key), createItemWMD(title, custom_lore,
                        ClaimGuis.getCustomItemMaterialMD("list", key), ClaimGuis.getCustomItemCustomModelData("list", key)));
            } else {
                inv.setItem(ClaimGuis.getCustomItemSlot("list", key), createItem(ClaimGuis.getCustomItemMaterial("list", key),
                        title, custom_lore));
            }
        }
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
    }

    /**
     * Gets members from a claim chunk.
     *
     * @param claim The claim chunk.
     * @return The members of the claim.
     */
    public static String getMembers(Claim claim) {
        Set<String> members = claim.getMembers();
        if (members.isEmpty()) {
            return ClaimLanguage.getMessage("claim-list-no-member");
        }
        StringBuilder factionsList = new StringBuilder();
        int i = 0;
        for (String membre : members) {
            Player p = Bukkit.getPlayer(membre);
            String fac = "§a" + membre;
            if (p == null) {
                fac = "§c" + membre;
            }
            factionsList.append(fac);
            if (i < members.size() - 1) {
                factionsList.append("§7, ");
            }
            if ((i + 1) % 4 == 0 && i < members.size() - 1) {
                factionsList.append("\n");
            }
            i++;
        }
        String result = factionsList.toString();
        return result;
    }

    /**
     * Splits the lore into lines.
     *
     * @param lore The lore string.
     * @return The list of lore lines.
     */
    public static List<String> getLore(String lore) {
        List<String> lores = new ArrayList<>();
        String[] parts = lore.split("\n");
        for (String s : parts) {
            lores.add(s);
        }
        return lores;
    }

    /**
     * Gets the lore with placeholders from PlaceholderAPI.
     *
     * @param lore   The lore string.
     * @param player The player.
     * @return The list of lore lines.
     */
    public static List<String> getLoreWP(String lore, Player player) {
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
     * Creates an item in the GUI.
     *
     * @param material The material of the item.
     * @param name     The name of the item.
     * @param lore     The lore of the item.
     * @return The created item.
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = null;
        if (material == null) {
            SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
            SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            item = new ItemStack(Material.STONE, 1);
        } else {
            item = new ItemStack(material, 1);
        }
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
     * Creates a custom item in the GUI.
     *
     * @param name             The name of the item.
     * @param lore             The lore of the item.
     * @param name_custom_item The custom item name.
     * @param model_data       The custom model data.
     * @return The created custom item.
     */
    private ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if (customStack == null) {
            SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : " + name_custom_item);
            SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            item = new ItemStack(Material.STONE, 1);
        } else {
            item = customStack.getItemStack();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(model_data);
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates the back page item.
     *
     * @param page The current page.
     * @return The back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("list", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "back-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "
                        + ClaimGuis.getItemMaterialMD("list", "back-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("list", "back-page-list");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-lore").replaceAll("%page%", String.valueOf(page))));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the back page 2 item.
     *
     * @param chunk The chunk.
     * @return The back page 2 item.
     */
    private ItemStack backPage2(Chunk chunk) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("list", "back-page-settings")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "back-page-settings"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "
                        + ClaimGuis.getItemMaterialMD("list", "back-page-settings"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("list", "back-page-settings");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-chunk-title"));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-chunk-lore").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the next page item.
     *
     * @param page The current page.
     * @return The next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("list", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "next-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "
                        + ClaimGuis.getItemMaterialMD("list", "next-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("list", "next-page-list");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("next-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("next-page-lore").replaceAll("%page%", String.valueOf(page))));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the filter item.
     *
     * @param filter The filter.
     * @return The filter item.
     */
    private ItemStack createFilterItem(String filter) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("list", "filter")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "filter"));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("list", "filter");
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = ClaimLanguage.getMessage("filter-list-lore");
            if (filter.equals("not_owner")) {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"));
            } else {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
                        .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(loreFilter));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    /**
     * Opens the inventory for the player.
     *
     * @param player The player.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
