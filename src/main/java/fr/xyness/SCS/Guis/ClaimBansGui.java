package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Class representing the Claim Bans GUI.
 */
public class ClaimBansGui implements InventoryHolder {

    // ***************
    // *  Variables  *
    // ***************

    /** Inventory for the GUI. */
    private Inventory inv;

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Main constructor for the ClaimBansGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param chunk  The chunk associated with the claim.
     * @param page   The page number of the GUI.
     */
    public ClaimBansGui(Player player, Chunk chunk, int page) {
        String title = ClaimGuis.getGuiTitle("bans")
                .replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))
                .replaceAll("%page%", String.valueOf(page));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("bans") * 9, title);
        SimpleClaimSystem.executeAsync(() -> initializeItems(player, chunk, page));
    }

    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param chunk  The chunk associated with the claim.
     * @param page   The page number of the GUI.
     */
    public void initializeItems(Player player, Chunk chunk, int page) {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setChunk(chunk);
        cPlayer.clearMapString();
        Claim claim = ClaimMain.getClaimFromChunk(chunk);
        int min_member_slot = ClaimGuis.getGuiMinSlot("bans");
        int max_member_slot = ClaimGuis.getGuiMaxSlot("bans");
        int items_count = max_member_slot - min_member_slot + 1;

        SimpleClaimSystem.executeSync(() -> {
        	if (page > 1) {
                inv.setItem(ClaimGuis.getItemSlot("bans", "back-page-list"), backPage(page - 1));
            } else {
                inv.setItem(ClaimGuis.getItemSlot("bans", "back-page-settings"), backPage2());
            }
        });

        List<String> lore = new ArrayList<>();
        String owner = claim.getOwner();
        if (owner.equals("admin")) {
            lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("player-banned-protected-area-lore")));
        } else {
            lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("player-banned-lore")));
        }
        if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
            lore.remove(lore.size() - 1);
            lore.add(ClaimLanguage.getMessage("gui-button-no-permission") + " to unban");
        }
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (String p : claim.getBans()) {
            if (count++ < startItem)
                continue;
            if (i == max_member_slot + 1) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getItemSlot("bans", "next-page-list"), nextPage(page + 1)));
                break;
            }
            List<String> lore2 = new ArrayList<>(getLoreWP(lore, p));
            cPlayer.addMapString(i, p);
            final int i_f = i;
            if (ClaimGuis.getItemCheckCustomModelData("bans", "player-item")) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, createItemWMD(
                        ClaimLanguage.getMessageWP("player-ban-title", p).replace("%player%", p), lore2,
                        ClaimGuis.getItemMaterialMD("bans", "player-item"),
                        ClaimGuis.getItemCustomModelData("bans", "player-item"))));
                i++;
                continue;
            }
            if (ClaimGuis.getItemMaterialMD("bans", "player-item").contains("PLAYER_HEAD")) {
                ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setOwningPlayer(Bukkit.getOfflinePlayerIfCached(p));
                meta.setDisplayName(ClaimLanguage.getMessageWP("player-ban-title", p).replace("%player%", p));
                meta.setLore(lore2);
                item.setItemMeta(meta);
                SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, item));
                i++;
                continue;
            }
            ItemStack item = new ItemStack(ClaimGuis.getItemMaterial("bans", "player-item"), 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ClaimLanguage.getMessageWP("player-ban-title", p).replace("%player%", p));
            meta.setLore(lore2);
            item.setItemMeta(meta);
            SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, item));
            i++;
        }

        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("bans"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(getLoreP(ClaimGuis.getCustomItemLore("bans", key), player));
            String title = ClaimSettings.getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, ClaimGuis.getCustomItemTitle("bans", key)) : ClaimGuis.getCustomItemTitle("bans", key);
            if (ClaimGuis.getCustomItemCheckCustomModelData("bans", key)) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getCustomItemSlot("bans", key), createItemWMD(title, custom_lore,
                        ClaimGuis.getCustomItemMaterialMD("bans", key),
                        ClaimGuis.getCustomItemCustomModelData("bans", key))));
            } else {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getCustomItemSlot("bans", key), createItem(
                        ClaimGuis.getCustomItemMaterial("bans", key), title, custom_lore)));
            }
        }
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
    }

    /**
     * Splits the lore into lines.
     *
     * @param lore The lore to split.
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
     * Splits the lore into lines with placeholders from PAPI.
     *
     * @param lore   The lore to split.
     * @param player The player for whom the placeholders are set.
     * @return The list of lore lines.
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
     * Gets the lore with placeholders from PAPI.
     *
     * @param lore   The lore to split.
     * @param player The player name for whom the placeholders are set.
     * @return The list of lore lines.
     */
    public static List<String> getLoreWP(List<String> lore, String player) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi"))
            return lore;
        List<String> lores = new ArrayList<>();
        Player p = Bukkit.getPlayer(player);
        if (p == null) {
            OfflinePlayer o_offline = Bukkit.getOfflinePlayerIfCached(player);
            for (String s : lore) {
                lores.add(PlaceholderAPI.setPlaceholders(o_offline, s));
            }
            return lores;
        }
        for (String s : lore) {
            lores.add(PlaceholderAPI.setPlaceholders(p, s));
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
            SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
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
     * Creates an item for the back page slot.
     *
     * @param page The page number.
     * @return The created back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("bans", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("bans", "back-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger()
                        .info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("bans", "back-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("bans", "back-page-list");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
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
     * Creates an item for the back page 2 slot.
     *
     * @return The created back page 2 item.
     */
    private ItemStack backPage2() {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("bans", "back-page-settings")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("bans", "back-page-settings"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger()
                        .info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("bans", "back-page-settings"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("bans", "back-page-settings");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-settings-title"));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-settings-lore")));
            meta = ClaimGuis.setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates an item for the next page slot.
     *
     * @param page The page number.
     * @return The created next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("bans", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("bans", "next-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger()
                        .info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("bans", "next-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("bans", "next-page-list");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
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
