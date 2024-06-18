package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
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
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimsGui implements InventoryHolder {

    private Inventory inv;
    private Player player;
    private int page;
    private String filter;
    private int itemsPerPage;
    private int minMemberSlot;
    private int maxMemberSlot;

    public ClaimsGui(Player player, int page, String filter) {
        this.player = player;
        this.page = page;
        this.filter = filter;
        this.minMemberSlot = ClaimGuis.getGuiMinSlot("claims");
        this.maxMemberSlot = ClaimGuis.getGuiMaxSlot("claims");
        this.itemsPerPage = maxMemberSlot - minMemberSlot + 1;
        String title = ClaimGuis.getGuiTitle("claims").replaceAll("%page%", String.valueOf(page));
        if (ClaimSettings.getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims") * 9, title);
        initializeItems();
    }

    private void initializeItems() {
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> loadItems());
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), this::loadItems);
    	}
    }

    private void loadItems() {
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        cPlayer.setFilter(filter);
        cPlayer.clearMapString();

        if (page > 1) {
            if (SimpleClaimSystem.isFolia()) {
                Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getItemSlot("claims", "back-page-list"), backPage(page - 1)));
            } else {
                Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getItemSlot("claims", "back-page-list"), backPage(page - 1)));
            }
        }

        List<String> loreTemplate = new ArrayList<>(getLore(ClaimLanguage.getMessage("owner-claim-lore")));
        Set<String> powners = getPownersByFilter(filter);
        Map<String, Integer> ownerClaimCount = ClaimMain.getPlayerlistClaimsCount(powners);

        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getItemSlot("claims", "filter"), filter(filter)));
        } else {
            Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getItemSlot("claims", "filter"), filter(filter)));
        }

        int startItem = (page - 1) * itemsPerPage;
        int i = minMemberSlot;
        int count = 0;

        for (String owner : powners) {
            if (count++ < startItem) continue;
            if (i == maxMemberSlot + 1) {
                if (SimpleClaimSystem.isFolia()) {
                    Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getItemSlot("claims", "next-page-list"), nextPage(page + 1)));
                } else {
                    Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getItemSlot("claims", "next-page-list"), nextPage(page + 1)));
                }
                break;
            }

            List<String> lore = new ArrayList<>();
            for (String s : loreTemplate) {
                lore.add(s.replaceAll("%claim-amount%", String.valueOf(ownerClaimCount.get(owner))));
            }
            lore.add(ClaimLanguage.getMessage("owner-claim-access"));
            lore = getLoreWP(lore, owner);
            cPlayer.addMapString(i, owner);
            ItemStack item = createOwnerClaimItem(owner, lore);
            int finalI = i;

            if (SimpleClaimSystem.isFolia()) {
                Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(finalI, item));
            } else {
                Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(finalI, item));
            }
            i++;
        }

        Set<String> customItems = new HashSet<>(ClaimGuis.getCustomItems("claims"));
        for (String key : customItems) {
            List<String> lore = new ArrayList<>(getLoreP(ClaimGuis.getCustomItemLore("claims", key), player));
            String title = ClaimGuis.getCustomItemTitle("claims", key);
            if (ClaimSettings.getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            final String title_f = title;
            if (SimpleClaimSystem.isFolia()) {
                Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getCustomItemSlot("claims", key), createCustomItem(key, title_f, lore)));
            } else {
                Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(ClaimGuis.getCustomItemSlot("claims", key), createCustomItem(key, title_f, lore)));
            }
        }
    }

    private Set<String> getPownersByFilter(String filter) {
        switch (filter) {
            case "sales":
                return ClaimMain.getClaimsOwnersWithSales();
            case "online":
                return ClaimMain.getClaimsOnlineOwners();
            case "offline":
                return ClaimMain.getClaimsOfflineOwners();
            default:
                return ClaimMain.getClaimsOwners();
        }
    }

    private ItemStack createOwnerClaimItem(String owner, List<String> lore) {
        if (ClaimGuis.getItemCheckCustomModelData("claims", "claim-item")) {
            return createItemWMD(ClaimLanguage.getMessageWP("owner-claim-title", owner).replaceAll("%owner%", owner),
                    lore,
                    ClaimGuis.getItemMaterialMD("claims", "claim-item"),
                    ClaimGuis.getItemCustomModelData("claims", "claim-item"));
        }
        if (ClaimGuis.getItemMaterialMD("claims", "claim-item").contains("PLAYER_HEAD")) {
            ItemStack item = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(owner));
            meta.setDisplayName(ClaimLanguage.getMessageWP("owner-claim-title", owner).replaceAll("%owner%", owner));
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
        ItemStack item = new ItemStack(ClaimGuis.getItemMaterial("claims", "claim-item"), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ClaimLanguage.getMessageWP("owner-claim-title", owner).replaceAll("%owner%", owner));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCustomItem(String key, String title, List<String> lore) {
        if (ClaimGuis.getCustomItemCheckCustomModelData("claims", key)) {
            return createItemWMD(title,
                    lore,
                    ClaimGuis.getCustomItemMaterialMD("claims", key),
                    ClaimGuis.getCustomItemCustomModelData("claims", key));
        } else {
            return createItem(ClaimGuis.getCustomItemMaterial("claims", key),
                    title,
                    lore);
        }
    }

    public static List<String> getLore(String lore) {
        List<String> lores = new ArrayList<>();
        String[] parts = lore.split("\n");
        for (String s : parts) {
            lores.add(s);
        }
        return lores;
    }

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

    public static List<String> getLoreWP(List<String> lore, String player) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) return lore;
        List<String> lores = new ArrayList<>();
        Player p = Bukkit.getPlayer(player);
        if (p == null) {
            OfflinePlayer o_offline = Bukkit.getOfflinePlayer(player);
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
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

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
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("claims", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", "back-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("claims", "back-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("claims", "back-page-list");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check claims.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-lore").replaceAll("%page%", String.valueOf(page))));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack filter(String filter) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("claims", "filter")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", "filter"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("claims", "filter"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("claims", "filter");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check claims.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String loreFilter = ClaimLanguage.getMessage("filter-new-lore");
            if (filter.equals("sales")) {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"))
                        .replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            } else if (filter.equals("online")) {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_active_filter"))
                        .replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            } else if (filter.equals("offline")) {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_active_filter"));
            } else {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
                        .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                        .replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(loreFilter));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack nextPage(int page) {
        ItemStack item = null;
        if (ClaimGuis.getItemCheckCustomModelData("claims", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", "next-page-list"));
            if (customStack == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : " + ClaimGuis.getItemMaterialMD("claims", "next-page-list"));
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = ClaimGuis.getItemMaterial("claims", "next-page-list");
            if (material == null) {
                SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check claims.yml");
                SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("next-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("next-page-lore").replaceAll("%page%", String.valueOf(page))));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
