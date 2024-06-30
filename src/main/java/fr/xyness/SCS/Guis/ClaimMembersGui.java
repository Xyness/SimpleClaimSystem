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
import fr.xyness.SCS.Others.MinecraftSkinUtil;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Class representing the Claim Members GUI.
 */
public class ClaimMembersGui implements InventoryHolder {
    
    // ***************
    // *  Variables  *
    // ***************
    
	/** Inventory for the GUI. */
    private Inventory inv;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Main constructor for ClaimMembersGui.
     * 
     * @param player The player who opened the GUI.
     * @param chunk  The chunk for which the GUI is displayed.
     * @param page   The current page of the GUI.
     */
    public ClaimMembersGui(Player player, Chunk chunk, int page) {
        String title = ClaimGuis.getGuiTitle("members")
                .replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))
                .replaceAll("%page%", String.valueOf(page));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("members") * 9, title);
        SimpleClaimSystem.executeAsync(() -> initializeItems(player, chunk, page));
    }
    
    // ********************
    // *  Others Methods  *
    // ********************
    
    /**
     * Initializes the items for the GUI.
     * 
     * @param player The player who opened the GUI.
     * @param chunk  The chunk for which the GUI is displayed.
     * @param page   The current page of the GUI.
     */
    public void initializeItems(Player player, Chunk chunk, int page) {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setChunk(chunk);
        cPlayer.clearMapString();
        Claim claim = ClaimMain.getClaimFromChunk(chunk);
        int min_member_slot = ClaimGuis.getGuiMinSlot("members");
        int max_member_slot = ClaimGuis.getGuiMaxSlot("members");
        int items_count = max_member_slot - min_member_slot + 1;
        
        SimpleClaimSystem.executeSync(() -> {
            if (page > 1) {
                inv.setItem(ClaimGuis.getItemSlot("members", "back-page-list"), backPage(page - 1));
            } else {
                inv.setItem(ClaimGuis.getItemSlot("members", "back-page-settings"), backPage2());
            }
        });
        
        List<String> lore = new ArrayList<>();
        String owner = claim.getOwner();
        if (owner.equals("admin")) {
            lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("protected-area-access-lore-new")));
        } else {
            lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("territory-access-lore-new")));
        }
        lore.add(CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")
                ? ClaimLanguage.getMessage("access-claim-clickable-removemember")
                : ClaimLanguage.getMessage("gui-button-no-permission") + " to remove this player");
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (String p : claim.getMembers()) {
            if (count++ < startItem) continue;
            if (i == max_member_slot + 1) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getItemSlot("members", "next-page-list"), nextPage(page + 1)));
                break;
            }
            List<String> lore2 = new ArrayList<>(getLoreWP(lore, p));
            cPlayer.addMapString(i, p);
            final int i_f = i;
            if (ClaimGuis.getItemCheckCustomModelData("members", "player-item")) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, createItemWMD(ClaimLanguage.getMessageWP("player-member-title", p).replace("%player%", p),
                        lore2,
                        ClaimGuis.getItemMaterialMD("members", "player-item"),
                        ClaimGuis.getItemCustomModelData("members", "player-item"))));
                i++;
                continue;
            }
            if (ClaimGuis.getItemMaterialMD("members", "player-item").contains("PLAYER_HEAD")) {
                ItemStack item = MinecraftSkinUtil.createPlayerHead(p);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setDisplayName(ClaimLanguage.getMessageWP("player-member-title", p).replace("%player%", p));
                if (owner.equals(p)) {
                    List<String> lore_chef = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("owner-territory-lore", p)));
                    meta.setLore(lore_chef);
                } else {
                    meta.setLore(lore2);
                }
                item.setItemMeta(meta);
                SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, item));
                i++;
                continue;
            }
            ItemStack item = new ItemStack(ClaimGuis.getItemMaterial("members", "player-item"), 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(ClaimLanguage.getMessageWP("player-member-title", p).replace("%player%", p));
            if (owner.equals(p)) {
                List<String> lore_chef = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("owner-territory-lore", p)));
                meta.setLore(lore_chef);
            } else {
                meta.setLore(lore2);
            }
            item.setItemMeta(meta);
            SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, item));
            i++;
        }
        
        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("members"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(getLoreP(ClaimGuis.getCustomItemLore("members", key), player));
            String title = ClaimSettings.getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, ClaimGuis.getCustomItemTitle("members", key)) : ClaimGuis.getCustomItemTitle("members", key);
            if (ClaimGuis.getCustomItemCheckCustomModelData("members", key)) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getCustomItemSlot("members", key), createItemWMD(title,
                        custom_lore,
                        ClaimGuis.getCustomItemMaterialMD("members", key),
                        ClaimGuis.getCustomItemCustomModelData("members", key))));
            } else {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getCustomItemSlot("members", key), createItem(ClaimGuis.getCustomItemMaterial("members", key),
                        title,
                        custom_lore)));
            }
        }
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
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
     * Splits the lore into lines with placeholders from PlaceholderAPI.
     * 
     * @param lore   The lore string.
     * @param player The player.
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
     * @param name           The name of the item.
     * @param lore           The lore of the item.
     * @param name_custom_item The custom item name.
     * @param model_data     The model data.
     * @return The created custom item.
     */
    private ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if (customStack == null) {
            SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading: " + name_custom_item);
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
     * @param page The page number.
     * @return The back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("members", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("members", "back-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading: " + ClaimGuis.getItemMaterialMD("members", "back-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("members", "back-page-list");
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
     * Creates the back page settings item.
     * 
     * @return The back page settings item.
     */
    private ItemStack backPage2() {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("members", "back-page-settings")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("members", "back-page-settings"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading: " + ClaimGuis.getItemMaterialMD("members", "back-page-settings"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("members", "back-page-settings");
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
     * Creates the next page item.
     * 
     * @param page The page number.
     * @return The next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("members", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("members", "next-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading: " + ClaimGuis.getItemMaterialMD("members", "next-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("members", "next-page-list");
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
     * @param player The player.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
