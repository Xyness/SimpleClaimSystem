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

    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the ClaimBansGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The page number of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimBansGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = instance.getGuis().getGuiTitle("bans")
                .replaceAll("%name%", claim.getName())
                .replaceAll("%page%", String.valueOf(page));
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("bans") * 9, title);
        instance.executeAsync(() -> loadItems(player, claim, page));
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
    public void loadItems(Player player, Claim claim, int page) {
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        cPlayer.setClaim(claim);
        cPlayer.clearMapString();
        int min_member_slot = instance.getGuis().getGuiMinSlot("bans");
        int max_member_slot = instance.getGuis().getGuiMaxSlot("bans");
        int items_count = max_member_slot - min_member_slot + 1;

    	if (page > 1) {
            inv.setItem(instance.getGuis().getItemSlot("bans", "back-page-list"), backPage(page - 1));
        }
        inv.setItem(instance.getGuis().getItemSlot("bans", "back-page-main"), backPage2(claim));

        List<String> lore = new ArrayList<>();
        String owner = claim.getOwner();
        if (owner.equals("admin")) {
            lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("player-banned-protected-area-lore")));
        } else {
            lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("player-banned-lore")));
        }
        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban") ? instance.getLanguage().getMessage("unban-this-player-button") : (instance.getLanguage().getMessage("gui-button-no-permission") + " to unban"));
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        for (String p : claim.getBans()) {
            if (count++ < startItem)
                continue;
            if (i == max_member_slot + 1) {
                inv.setItem(instance.getGuis().getItemSlot("bans", "next-page-list"), nextPage(page + 1));
                break;
            }
            OfflinePlayer target = instance.getPlayerMain().getOfflinePlayer(p);
            List<String> lore2 = new ArrayList<>(instance.getGuis().getLoreWP(lore, p, target));
            cPlayer.addMapString(i, p);
            if (instance.getGuis().getItemCheckCustomModelData("bans", "player-item")) {
                inv.setItem(i, instance.getGuis().createItemWMD(
                        instance.getLanguage().getMessageWP("player-ban-title", target).replace("%player%", p), lore2,
                        instance.getGuis().getItemMaterialMD("bans", "player-item"),
                        instance.getGuis().getItemCustomModelData("bans", "player-item")));
                i++;
                continue;
            }
            if (instance.getGuis().getItemMaterialMD("bans", "player-item").contains("PLAYER_HEAD")) {
            	ItemStack item = instance.getPlayerMain().getPlayerHead(target);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setDisplayName(instance.getLanguage().getMessageWP("player-ban-title", target).replace("%player%", p));
                meta.setLore(lore2);
                item.setItemMeta(meta);
                inv.setItem(i, item);
                i++;
                continue;
            }
            ItemStack item = new ItemStack(instance.getGuis().getItemMaterial("bans", "player-item"), 1);
            ItemMeta meta = item.getItemMeta();
            meta.setDisplayName(instance.getLanguage().getMessageWP("player-ban-title", target).replace("%player%", p));
            meta.setLore(lore2);
            item.setItemMeta(meta);
            inv.setItem(i, item);
            i++;
        }

        Set<String> custom_items = new HashSet<>(instance.getGuis().getCustomItems("bans"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("bans", key), player));
            String title = instance.getSettings().getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, instance.getGuis().getCustomItemTitle("bans", key)) : instance.getGuis().getCustomItemTitle("bans", key);
            if (instance.getGuis().getCustomItemCheckCustomModelData("bans", key)) {
                inv.setItem(instance.getGuis().getCustomItemSlot("bans", key), instance.getGuis().createItemWMD(title, custom_lore,
                        instance.getGuis().getCustomItemMaterialMD("bans", key),
                        instance.getGuis().getCustomItemCustomModelData("bans", key)));
            } else {
                inv.setItem(instance.getGuis().getCustomItemSlot("bans", key), instance.getGuis().createItem(
                        instance.getGuis().getCustomItemMaterial("bans", key), title, custom_lore));
            }
        }
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }

    /**
     * Creates an item for the back page slot.
     *
     * @param page The page number.
     * @return The created back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = null;
        if (instance.getGuis().getItemCheckCustomModelData("bans", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("bans", "back-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger()
                        .info("Error custom item loading : " + instance.getGuis().getItemMaterialMD("bans", "back-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("bans", "back-page-list");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check members.yml");
                instance.getPlugin().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("previous-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("previous-page-lore").replaceAll("%page%", String.valueOf(page))));
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
        if (instance.getGuis().getItemCheckCustomModelData("bans", "back-page-main")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("bans", "back-page-main"));
            if (customStack == null) {
                instance.getPlugin().getLogger()
                        .info("Error custom item loading : " + instance.getGuis().getItemMaterialMD("bans", "back-page-main"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("bans", "back-page-main");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check bans.yml");
                instance.getPlugin().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("back-page-main-title"));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("back-page-main-lore").replaceAll("%claim-name%", claim.getName())));
            meta = instance.getGuis().setItemFlag(meta);
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
        if (instance.getGuis().getItemCheckCustomModelData("bans", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("bans", "next-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger()
                        .info("Error custom item loading : " + instance.getGuis().getItemMaterialMD("bans", "next-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("bans", "next-page-list");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check members.yml");
                instance.getPlugin().getLogger().info("Using STONE instead");
                material = Material.STONE;
            }
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("next-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("next-page-lore").replaceAll("%page%", String.valueOf(page))));
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
     * @param player The player for whom the inventory is opened.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}
