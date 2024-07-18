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
 * Class representing the Claim List GUI.
 */
public class ClaimListGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** Inventory for the GUI. */
    private Inventory inv;

    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the ClaimListGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the list.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimListGui(Player player, int page, String filter, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = instance.getGuis().getGuiTitle("list").replace("%page%", String.valueOf(page));
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("list") * 9, title);
        instance.executeAsync(() -> loadItems(player, page, filter));
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

        int min_member_slot = instance.getGuis().getGuiMinSlot("list");
        int max_member_slot = instance.getGuis().getGuiMaxSlot("list");
        int items_count = max_member_slot - min_member_slot + 1;
        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
        cPlayer.setFilter(filter);
        cPlayer.clearMapClaim();
        cPlayer.clearMapLoc();

        String lore_tp = instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")
                ? instance.getLanguage().getMessage("access-claim-clickable-tp")
                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-teleport");
        String lore_remove = instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")
                ? instance.getLanguage().getMessage("access-claim-clickable-remove")
                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-remove");
        String lore_settings = instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.main")
                ? instance.getLanguage().getMessage("access-claim-clickable-manage")
                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-manage");

        if (page > 1) {
            inv.setItem(instance.getGuis().getItemSlot("list", "back-page-list"), backPage(page - 1));
        }
        if (cPlayer.getClaim() != null) {
            inv.setItem(instance.getGuis().getItemSlot("list", "back-page-main"), backPage2(cPlayer.getClaim()));
        }

        inv.setItem(instance.getGuis().getItemSlot("list", "filter"), createFilterItem(filter));

        Set<Claim> claims;
        List<String> lore;
        if (filter.equals("owner")) {
            claims = new HashSet<>(instance.getMain().getPlayerClaims(playerName));
            lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP("access-claim-lore", (OfflinePlayer) player)));
        } else {
            claims = new HashSet<>(instance.getMain().getClaimsWhereMemberNotOwner(playerName));
            lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP("access-claim-not-owner-lore", (OfflinePlayer) player)));
        }
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (Claim claim : claims) {
            if (count++ < startItem)
                continue;
            if (i == max_member_slot + 1) {
                inv.setItem(instance.getGuis().getItemSlot("list", "next-page-list"), nextPage(page + 1));
                break;
            }
            cPlayer.addMapClaim(i, claim);
            cPlayer.addMapLoc(i, claim.getLocation());
            List<String> used_lore = new ArrayList<>();
            for (String s : lore) {
                s = s.replace("%owner%", claim.getOwner());
                s = s.replace("%description%", claim.getDescription());
                s = s.replace("%name%", claim.getName()).replace("%coords%", instance.getMain().getClaimCoords(claim));
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
                } else if (s.contains("%bans%")) {
                    String bans = getBans(claim);
                    if (bans.contains("\n")) {
                        String[] parts = bans.split("\n");
                        for (String ss : parts) {
                            used_lore.add(ss);
                        }
                    } else {
                        used_lore.add(bans);
                    }
                } else {
                    used_lore.add(s);
                }
            }
            if (filter.equals("owner")) {
                used_lore.addAll(Arrays.asList(lore_tp, lore_remove, lore_settings));
            } else {
                used_lore.add(lore_tp);
            }
            final int i_f = i;
            if (instance.getGuis().getItemCheckCustomModelData("list", "claim-item")) {
                inv.setItem(i_f,
                		instance.getGuis().createItemWMD(
                                instance.getLanguage().getMessageWP("access-claim-title", (OfflinePlayer) player)
                                        .replace("%name%", claim.getName())
                                        .replace("%coords%", instance.getMain().getClaimCoords(claim)),
                                used_lore, instance.getGuis().getItemMaterialMD("list", "claim-item"),
                                instance.getGuis().getItemCustomModelData("list", "claim-item")));
                i++;
                continue;
            }
            if (instance.getGuis().getItemMaterialMD("list", "claim-item").contains("PLAYER_HEAD")) {
            	ItemStack item = instance.getPlayerMain().getPlayerHead((OfflinePlayer) player);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setDisplayName(instance.getLanguage().getMessageWP("access-claim-title", (OfflinePlayer) player).replace("%name%", claim.getName()).replace("%coords%", instance.getMain().getClaimCoords(claim)));
                meta.setLore(used_lore);
                item.setItemMeta(meta);
                inv.setItem(i_f, item);
                i++;
                continue;
            }
            inv.setItem(i_f,
            		instance.getGuis().createItem(instance.getGuis().getItemMaterial("list", "claim-item"),
                            instance.getLanguage().getMessageWP("access-claim-title", (OfflinePlayer) player)
                                    .replace("%name%", claim.getName()).replace("%coords%", instance.getMain().getClaimCoords(claim)),
                            used_lore));
            i++;
        }

        Set<String> custom_items = new HashSet<>(instance.getGuis().getCustomItems("list"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("list", key), player));
            String title = instance.getSettings().getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, instance.getGuis().getCustomItemTitle("list", key)) : instance.getGuis().getCustomItemTitle("list", key);
            if (instance.getGuis().getCustomItemCheckCustomModelData("list", key)) {
                inv.setItem(instance.getGuis().getCustomItemSlot("list", key), instance.getGuis().createItemWMD(title, custom_lore,
                        instance.getGuis().getCustomItemMaterialMD("list", key), instance.getGuis().getCustomItemCustomModelData("list", key)));
            } else {
                inv.setItem(instance.getGuis().getCustomItemSlot("list", key), instance.getGuis().createItem(instance.getGuis().getCustomItemMaterial("list", key),
                        title, custom_lore));
            }
        }
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }

    /**
     * Gets members from a claim chunk.
     *
     * @param claim The claim chunk.
     * @return The members of the claim.
     */
    public String getMembers(Claim claim) {
        Set<String> members = claim.getMembers();
        if (members.isEmpty()) {
            return instance.getLanguage().getMessage("claim-list-no-member");
        }
        StringBuilder factionsList = new StringBuilder();
        int i = 0;
        for (String membre : members) {
            Player p = Bukkit.getPlayer(membre);
            String fac = p != null ? "§a" + membre : "§c" + membre;
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
     * Get the bans of a claim as a string with new lines.
     * 
     * @param claim The claim object.
     * @return A string representing the bans of the claim.
     */
    public String getBans(Claim claim) {
        Set<String> members = claim.getBans();
        if (members.isEmpty()) {
            return instance.getLanguage().getMessage("claim-list-no-ban");
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
     * Creates the back page item.
     *
     * @param page The current page.
     * @return The back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (instance.getGuis().getItemCheckCustomModelData("list", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("list", "back-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading : "
                        + instance.getGuis().getItemMaterialMD("list", "back-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("list", "back-page-list");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check list.yml");
                instance.getPlugin().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("previous-page-title").replace("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("previous-page-lore").replace("%page%", String.valueOf(page))));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the back page main item.
     * 
     * @param claim The target claim
     * @return The back page main item.
     */
    private ItemStack backPage2(Claim claim) {
        ItemStack item = null;
        if (instance.getGuis().getItemCheckCustomModelData("list", "back-page-main")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("list", "back-page-main"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading : "
                        + instance.getGuis().getItemMaterialMD("list", "back-page-main"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("list", "back-page-main");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check list.yml");
                instance.getPlugin().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("back-page-main-title"));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("back-page-main-lore").replace("%claim-name%", claim.getName())));
            meta = instance.getGuis().setItemFlag(meta);
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
        if (instance.getGuis().getItemCheckCustomModelData("list", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("list", "next-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading : "
                        + instance.getGuis().getItemMaterialMD("list", "next-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("list", "next-page-list");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check list.yml");
                instance.getPlugin().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("next-page-title").replace("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("next-page-lore").replace("%page%", String.valueOf(page))));
            meta = instance.getGuis().setItemFlag(meta);
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
        if (instance.getGuis().getItemCheckCustomModelData("list", "filter")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("list", "filter"));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = instance.getGuis().getItemMaterial("list", "filter");
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = instance.getLanguage().getMessage("filter-list-lore");
            if (filter.equals("not_owner")) {
                loreFilter = loreFilter.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_inactive_filter"))
                        .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_active_filter"));
            } else {
                loreFilter = loreFilter.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_active_filter"))
                        .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(instance.getLanguage().getMessage("filter-title"));
            meta.setLore(instance.getGuis().getLore(loreFilter));
            meta = instance.getGuis().setItemFlag(meta);
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
