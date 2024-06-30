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
 * Class representing the Claim GUI.
 */
public class ClaimGui implements InventoryHolder {

    // ***************
    // *  Variables  *
    // ***************

    /** Inventory for the GUI. */
    private Inventory inv;

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Main constructor for the ClaimGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param chunk  The chunk associated with the claim.
     */
    public ClaimGui(Player player, Chunk chunk) {
        String title = ClaimGuis.getGuiTitle("settings").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("settings") * 9, title);
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
        Set<String> items = new HashSet<>(ClaimGuis.getItems("settings"));
        Claim claim = ClaimMain.getClaimFromChunk(chunk);
        for (String key : items) {
            String lower_name = key.toLowerCase();
            List<String> lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP(lower_name + "-lore", playerName)));
            if (ClaimGuis.isAPerm(key)) {
                boolean permission = claim.getPermission(key);
                String statut = permission ? default_statut_enabled : default_statut_disabled;
                choix = permission ? default_choix_enabled : default_choix_disabled;
                lore.add(ClaimSettings.isEnabled(key) ? checkPermPerm(player,key) ? choix : ClaimLanguage.getMessage("gui-button-no-permission")+" to use this setting" : ClaimLanguage.getMessage("choice-setting-disabled"));
                if (ClaimGuis.getItemCheckCustomModelData("settings", key)) {
                    inv.setItem(ClaimGuis.getItemSlot("settings", key),
                            createItemWMD(ClaimLanguage.getMessageWP(lower_name + "-title", playerName)
                                    .replaceAll("%status%", statut), lore, ClaimGuis.getItemMaterialMD("settings", key),
                                    ClaimGuis.getItemCustomModelData("settings", key)));
                } else {
                    inv.setItem(ClaimGuis.getItemSlot("settings", key), createItem(
                            ClaimGuis.getItemMaterial("settings", key),
                            ClaimLanguage.getMessageWP(lower_name + "-title", playerName).replaceAll("%status%", statut),
                            lore));
                }
            } else {
                String title = ClaimLanguage.getMessageWP(lower_name + "-title", playerName)
                        .replaceAll("%coords%", ClaimMain.getClaimCoords(chunk))
                        .replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
                lore.add(!checkPermButton(player, key) ? ClaimLanguage.getMessage("gui-button-no-permission") : ClaimLanguage.getMessage("access-button"));
                if (ClaimGuis.getItemCheckCustomModelData("settings", key)) {
                    inv.setItem(ClaimGuis.getItemSlot("settings", key),
                            createItemWMD(title, lore, ClaimGuis.getItemMaterialMD("settings", key),
                                    ClaimGuis.getItemCustomModelData("settings", key)));
                } else {
                    inv.setItem(ClaimGuis.getItemSlot("settings", key), createItem(ClaimGuis.getItemMaterial("settings", key), title, lore));
                }
            }
        }

        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("settings"));
        for (String key : custom_items) {
            List<String> lore = new ArrayList<>(getLoreWP(ClaimGuis.getCustomItemLore("settings", key), player));
            String title = ClaimSettings.getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, ClaimGuis.getCustomItemTitle("settings", key)) : ClaimGuis.getCustomItemTitle("settings", key);
            if (ClaimGuis.getCustomItemCheckCustomModelData("settings", key)) {
                inv.setItem(ClaimGuis.getCustomItemSlot("settings", key),
                        createItemWMD(title, lore, ClaimGuis.getCustomItemMaterialMD("settings", key),
                                ClaimGuis.getCustomItemCustomModelData("settings", key)));
            } else {
                inv.setItem(ClaimGuis.getCustomItemSlot("settings", key),
                        createItem(ClaimGuis.getCustomItemMaterial("settings", key), title, lore));
            }
        }
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
    }

    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param key    The key to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public static boolean checkPermButton(Player player, String key) {
        switch (key) {
            case "define-loc":
                return CPlayerMain.checkPermPlayer(player, "scs.command.claim.setspawn");
            case "define-name":
                return CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname");
            case "manage-members":
                return CPlayerMain.checkPermPlayer(player, "scs.command.claim.members");
            case "manage-bans":
                return CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans");
            case "my-claims":
                return CPlayerMain.checkPermPlayer(player, "scs.command.claim.list");
            case "apply-all-claims":
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param perm    The perm to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public static boolean checkPermPerm(Player player, String perm) {
    	return CPlayerMain.checkPermPlayer(player, "scs.setting."+perm);
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
     * Gets the lore with placeholders from PAPI.
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
     * @return The created item.
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = null;
        if (material == null) {
            SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check settings.yml");
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
