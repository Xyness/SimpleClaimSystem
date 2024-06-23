package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.Arrays;
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
import org.bukkit.inventory.ItemFlag;
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
 * Class representing the Admin Claim List GUI.
 */
public class AdminClaimListGui implements InventoryHolder {

    // ***************
    // *  Variables  *
    // ***************

    /** Inventory for the GUI. */
    private Inventory inv;

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Main constructor for the AdminClaimListGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param page   The page number of the GUI.
     */
    public AdminClaimListGui(Player player, int page) {
        String title = ClaimGuis.getGuiTitle("admin_list").replaceAll("%page%", String.valueOf(page));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("admin_list") * 9, title);
        SimpleClaimSystem.executeAsync(() -> initializeItems(player, page));
    }

    // ********************
    // *  Others Methods  *
    // ********************

    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param page   The page number of the GUI.
     */
    public void initializeItems(Player player, int page) {
        int min_member_slot = ClaimGuis.getGuiMinSlot("admin_list");
        int max_member_slot = ClaimGuis.getGuiMaxSlot("admin_list");
        int items_count = max_member_slot - min_member_slot + 1;
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        cPlayer.clearMapChunk();
        cPlayer.clearMapLoc();
        
        String lore_tp = ClaimLanguage.getMessage("access-claim-clickable-tp");
        String lore_remove = ClaimLanguage.getMessage("access-claim-clickable-remove");
        String lore_settings = ClaimLanguage.getMessage("access-claim-clickable-settings");

        SimpleClaimSystem.executeSync(() -> {
        	if (page > 1) {
                inv.setItem(ClaimGuis.getItemSlot("admin_list", "back-page-list"), backPage(page - 1));
            } else if (cPlayer.getChunk() != null) {
                inv.setItem(ClaimGuis.getItemSlot("admin_list", "back-page-list"), backPage2(cPlayer.getChunk()));
            }
        });

        Map<Chunk, Claim> claims = ClaimMain.getChunksFromOwnerGui("admin");
        List<String> lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-lore", playerName)));
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (Chunk c : claims.keySet()) {
            if (count++ < startItem)
                continue;
            if (i == max_member_slot + 1) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getItemSlot("admin_list", "next-page-list"), nextPage(page + 1)));
                break;
            }
            Claim claim = claims.get(c);
            cPlayer.addMapChunk(i, c);
            cPlayer.addMapLoc(i, claim.getLocation());
            List<String> used_lore = new ArrayList<>();
            for (String s : lore) {
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
            used_lore.addAll(Arrays.asList(lore_tp,lore_remove,lore_settings));
            final int i_f = i;
            if (ClaimGuis.getItemCheckCustomModelData("admin_list", "claim-item")) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, createItemWMD(
                        ClaimLanguage.getMessageWP("access-claim-title", playerName).replaceAll("%name%", claim.getName())
                                .replaceAll("%coords%", ClaimMain.getClaimCoords(claim)),
                        used_lore, ClaimGuis.getItemMaterialMD("admin_list", "claim-item"),
                        ClaimGuis.getItemCustomModelData("admin_list", "claim-item"))));
                i++;
                continue;
            }
            if (ClaimGuis.getItemMaterialMD("admin_list", "claim-item").contains("PLAYER_HEAD")) {
                ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setOwningPlayer(player);
                meta.setDisplayName(ClaimLanguage.getMessageWP("access-claim-title", playerName).replaceAll("%name%", claim.getName()).replaceAll("%coords%", ClaimMain.getClaimCoords(claim)));
                meta.setLore(used_lore);
                item.setItemMeta(meta);
                SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, item));;
                i++;
                continue;
            }
            SimpleClaimSystem.executeSync(() -> inv.setItem(i_f, createItem(ClaimGuis.getItemMaterial("admin_list", "claim-item"),
                    ClaimLanguage.getMessageWP("access-claim-title", playerName).replaceAll("%name%", claim.getName())
                            .replaceAll("%coords%", ClaimMain.getClaimCoords(claim)),
                    used_lore)));
            i++;
        }

        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("admin_list"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(getLoreWP(ClaimGuis.getCustomItemLore("admin_list", key), player));
            String title = ClaimSettings.getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, ClaimGuis.getCustomItemTitle("admin_list", key)) : ClaimGuis.getCustomItemTitle("admin_list", key);
            if (ClaimGuis.getCustomItemCheckCustomModelData("admin_list", key)) {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getCustomItemSlot("admin_list", key),
                        createItemWMD(title, custom_lore, ClaimGuis.getCustomItemMaterialMD("admin_list", key),
                                ClaimGuis.getCustomItemCustomModelData("admin_list", key))));
            } else {
                SimpleClaimSystem.executeSync(() -> inv.setItem(ClaimGuis.getCustomItemSlot("admin_list", key), createItem(
                        ClaimGuis.getCustomItemMaterial("admin_list", key), title, custom_lore)));
            }
        }
        
        SimpleClaimSystem.executeSync(() -> openInventory(player));
    }

    /**
     * Retrieves members from a claim chunk.
     *
     * @param claim The claim from which to retrieve members.
     * @return A string representing the members of the claim.
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
     * Creates an item for the back page slot.
     *
     * @param page The page number.
     * @return The created back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("admin_list", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("admin_list", "back-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger()
                        .info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("admin_list", "back-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("admin_list", "back-page-list");
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
     * Creates an item for the back page 2 slot.
     *
     * @param chunk The chunk associated with the back page.
     * @return The created back page 2 item.
     */
    private ItemStack backPage2(Chunk chunk) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("admin_list", "back-page-settings")) {
            CustomStack customStack = CustomStack
                    .getInstance(ClaimGuis.getItemMaterialMD("admin_list", "back-page-settings"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger()
                        .info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("admin_list", "back-page-settings"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("admin_list", "back-page-settings");
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
     * Creates an item for the next page slot.
     *
     * @param page The page number.
     * @return The created next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("admin_list", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("admin_list", "next-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger()
                        .info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("admin_list", "next-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("admin_list", "next-page-list");
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
