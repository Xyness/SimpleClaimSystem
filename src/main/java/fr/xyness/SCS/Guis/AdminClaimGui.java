package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashSet;
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
 * Class representing the Admin Claim GUI.
 */
public class AdminClaimGui implements InventoryHolder {

    // ***************
    // *  Variables  *
    // ***************

    /** Inventory for the GUI. */
    private Inventory inv;

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Main constructor for the AdminClaimGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param chunk  The chunk associated with the claim.
     */
    public AdminClaimGui(Player player, Chunk chunk) {
        String title = ClaimGuis.getGuiTitle("admin_settings").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("admin_settings") * 9, title);
        SimpleClaimSystem.executeAsync(() -> loadItems(player, chunk));
    }

    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param chunk  The chunk associated with the claim.
     */
    public void loadItems(Player player, Chunk chunk) {
        String default_statut_disabled = ClaimLanguage.getMessage("status-disabled");
        String default_statut_enabled = ClaimLanguage.getMessage("status-enabled");
        String default_choix_disabled = ClaimLanguage.getMessage("choice-disabled");
        String default_choix_enabled = ClaimLanguage.getMessage("choice-enabled");

        String choix;
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        cPlayer.setChunk(chunk);
        Set<String> items = new HashSet<>(ClaimGuis.getItems("admin_settings"));
        Claim claim = ClaimMain.getClaimFromChunk(chunk);
        for (String key : items) {
            String lower_name = key.toLowerCase();
            List<String> lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP(lower_name + "-lore", playerName)));
            if (ClaimGuis.isAPerm(key)) {
                boolean permission = claim.getPermission(key);
                String statut = permission ? default_statut_enabled : default_statut_disabled;
                choix = permission ? default_choix_enabled : default_choix_disabled;
                lore.add(ClaimSettings.isEnabled(key) ? choix : ClaimLanguage.getMessage("choice-setting-disabled"));
                if (ClaimGuis.getItemCheckCustomModelData("admin_settings", key)) {
                    inv.setItem(ClaimGuis.getItemSlot("admin_settings", key), createItemWMD(
                            ClaimLanguage.getMessageWP(lower_name + "-title", playerName).replaceAll("%status%", statut), lore,
                            ClaimGuis.getItemMaterialMD("admin_settings", key),
                            ClaimGuis.getItemCustomModelData("admin_settings", key)));
                } else {
                    inv.setItem(ClaimGuis.getItemSlot("admin_settings", key), createItem(
                            ClaimGuis.getItemMaterial("admin_settings", key),
                            ClaimLanguage.getMessageWP(lower_name + "-title", playerName).replaceAll("%status%", statut), lore));
                }
            } else {
                String title = ClaimLanguage.getMessageWP(lower_name + "-title", playerName)
                        .replaceAll("%coords%", ClaimMain.getClaimCoords(chunk)).replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
                lore.add(ClaimLanguage.getMessage("access-button"));
                if (ClaimGuis.getItemCheckCustomModelData("admin_settings", key)) {
                    inv.setItem(ClaimGuis.getItemSlot("admin_settings", key), createItemWMD(title, lore,
                            ClaimGuis.getItemMaterialMD("admin_settings", key),
                            ClaimGuis.getItemCustomModelData("admin_settings", key)));
                } else {
                    inv.setItem(ClaimGuis.getItemSlot("admin_settings", key), createItem(
                            ClaimGuis.getItemMaterial("admin_settings", key), title, lore));
                }
            }
        }

        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("admin_settings"));
        for (String key : custom_items) {
            List<String> lore = new ArrayList<>(getLoreWP(ClaimGuis.getCustomItemLore("admin_settings", key), player));
            String title = ClaimSettings.getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, ClaimGuis.getCustomItemTitle("admin_settings", key)) : ClaimGuis.getCustomItemTitle("admin_settings", key);
            if (ClaimGuis.getCustomItemCheckCustomModelData("admin_settings", key)) {
                inv.setItem(ClaimGuis.getCustomItemSlot("admin_settings", key), createItemWMD(title, lore,
                        ClaimGuis.getCustomItemMaterialMD("admin_settings", key),
                        ClaimGuis.getCustomItemCustomModelData("admin_settings", key)));
            } else {
                inv.setItem(ClaimGuis.getCustomItemSlot("admin_settings", key), createItem(
                        ClaimGuis.getCustomItemMaterial("list", key), title, lore));
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
     * @return The created ItemStack.
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
     * @param model_data       The custom model data value.
     * @return The created ItemStack.
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
