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
 * Class representing the Claim Members GUI.
 */
public class ClaimMembersGui implements InventoryHolder {
    
	
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
     * Main constructor for ClaimMembersGui.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The current page of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMembersGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = instance.getGuis().getGuiTitle("members")
                .replace("%name%", claim.getName())
                .replace("%page%", String.valueOf(page));
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("members") * 9, title);
        instance.executeAsync(() -> loadItems(player, claim, page));
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
    public void loadItems(Player player, Claim claim, int page) {
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        cPlayer.setClaim(claim);
        cPlayer.clearMapString();
        int min_member_slot = instance.getGuis().getGuiMinSlot("members");
        int max_member_slot = instance.getGuis().getGuiMaxSlot("members");
        int items_count = max_member_slot - min_member_slot + 1;
        
        if (page > 1) {
            inv.setItem(instance.getGuis().getItemSlot("members", "back-page-list"), backPage(page - 1));
        }
        inv.setItem(instance.getGuis().getItemSlot("members", "back-page-main"), backPage2(claim));
        
        List<String> lore = new ArrayList<>();
        String owner = claim.getOwner();
        lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("territory-access-lore-new")));
        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")
                ? instance.getLanguage().getMessage("access-claim-clickable-removemember")
                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-remove-member"));
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (String p : claim.getMembers()) {
            if (count++ < startItem) continue;
            if (i == max_member_slot + 1) {
                inv.setItem(instance.getGuis().getItemSlot("members", "next-page-list"), nextPage(page + 1));
                break;
            }
            OfflinePlayer target = instance.getPlayerMain().getOfflinePlayer(p);
            List<String> lore2 = new ArrayList<>(instance.getGuis().getLoreWP(lore, p, target));
            cPlayer.addMapString(i, p);
            if (instance.getGuis().getItemCheckCustomModelData("members", "player-item")) {
                inv.setItem(i, instance.getGuis().createItemWMD(instance.getLanguage().getMessageWP("player-member-title", target).replace("%player%", p),
                        lore2,
                        instance.getGuis().getItemMaterialMD("members", "player-item"),
                        instance.getGuis().getItemCustomModelData("members", "player-item")));
                i++;
                continue;
            }
            if (instance.getGuis().getItemMaterialMD("members", "player-item").contains("PLAYER_HEAD")) {
                ItemStack item = instance.getPlayerMain().getPlayerHead(target);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setDisplayName(instance.getLanguage().getMessageWP("player-member-title", target).replace("%player%", p));
                if (owner.equals(p)) {
                    List<String> lore_chef = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP("owner-territory-lore", target)));
                    meta.setLore(lore_chef);
                } else {
                    meta.setLore(lore2);
                }
                item.setItemMeta(meta);
                inv.setItem(i, item);
                i++;
                continue;
            }
            ItemStack item = new ItemStack(instance.getGuis().getItemMaterial("members", "player-item"), 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(instance.getLanguage().getMessageWP("player-member-title", target).replace("%player%", p));
            if (owner.equals(p)) {
                List<String> lore_chef = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP("owner-territory-lore", target)));
                meta.setLore(lore_chef);
            } else {
                meta.setLore(lore2);
            }
            item.setItemMeta(meta);
            inv.setItem(i, item);
            i++;
        }
        
        Set<String> custom_items = new HashSet<>(instance.getGuis().getCustomItems("members"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("members", key), player));
            String title = instance.getSettings().getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, instance.getGuis().getCustomItemTitle("members", key)) : instance.getGuis().getCustomItemTitle("members", key);
            if (instance.getGuis().getCustomItemCheckCustomModelData("members", key)) {
                inv.setItem(instance.getGuis().getCustomItemSlot("members", key), instance.getGuis().createItemWMD(title,
                        custom_lore,
                        instance.getGuis().getCustomItemMaterialMD("members", key),
                        instance.getGuis().getCustomItemCustomModelData("members", key)));
            } else {
                inv.setItem(instance.getGuis().getCustomItemSlot("members", key), instance.getGuis().createItem(instance.getGuis().getCustomItemMaterial("members", key),
                        title,
                        custom_lore));
            }
        }
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }
    
    /**
     * Creates the back page item.
     * 
     * @param page The page number.
     * @return The back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (instance.getGuis().getItemCheckCustomModelData("members", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("members", "back-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("members", "back-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("members", "back-page-list");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check members.yml");
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
        if (instance.getGuis().getItemCheckCustomModelData("members", "back-page-main")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("members", "back-page-main"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("members", "back-page-main"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("members", "back-page-main");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check members.yml");
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
     * @param page The page number.
     * @return The next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = null;
        if (instance.getGuis().getItemCheckCustomModelData("members", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("members", "next-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("members", "next-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("members", "next-page-list");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check members.yml");
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
