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
public class ClaimChunksGui implements InventoryHolder {
    
	
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
     * Main constructor for ClaimChunksGui.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The current page of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimChunksGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = instance.getGuis().getGuiTitle("chunks")
                .replaceAll("%name%", claim.getName())
                .replaceAll("%page%", String.valueOf(page));
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("chunks") * 9, title);
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
        int min_member_slot = instance.getGuis().getGuiMinSlot("chunks");
        int max_member_slot = instance.getGuis().getGuiMaxSlot("chunks");
        int items_count = max_member_slot - min_member_slot + 1;
        
        if (page > 1) {
            inv.setItem(instance.getGuis().getItemSlot("chunks", "back-page-list"), backPage(page - 1));
        }
        inv.setItem(instance.getGuis().getItemSlot("chunks", "back-page-main"), backPage2(claim));
        
        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("chunk-lore")));
        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")
                ? (claim.getChunks().size() == 1 ? instance.getLanguage().getMessage("cannot-remove-only-remaining-chunk-gui") : instance.getLanguage().getMessage("access-claim-clickable-removechunk"))
                : instance.getLanguage().getMessage("gui-button-no-permission") + " to remove this chunk");
        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;
        int chunk_count = 0;
        for (Chunk chunk : claim.getChunks()) {
        	chunk_count++;
            if (count++ < startItem) continue;
            if (i == max_member_slot + 1) {
                inv.setItem(instance.getGuis().getItemSlot("chunks", "next-page-list"), nextPage(page + 1));
                break;
            }
            List<String> lore2 = new ArrayList<>(instance.getGuis().getLoreWP(lore, player.getName(), (OfflinePlayer) player));
            cPlayer.addMapString(i, String.valueOf(chunk.getWorld().getName()+";"+chunk.getX()+";"+chunk.getZ()));
            String title = instance.getLanguage().getMessageWP("chunk-title", (OfflinePlayer) player)
            		.replaceAll("%number%", String.valueOf(chunk_count))
            		.replaceAll("%coords%", String.valueOf(chunk.getWorld().getName()+", X:"+chunk.getX()+", Z:"+chunk.getZ()));
            if (instance.getGuis().getItemCheckCustomModelData("chunks", "chunk-item")) {
                inv.setItem(i, instance.getGuis().createItemWMD(title,
                        lore2,
                        instance.getGuis().getItemMaterialMD("chunks", "chunk-item"),
                        instance.getGuis().getItemCustomModelData("chunks", "chunk-item")));
                i++;
                continue;
            } else {
                ItemStack item = new ItemStack(instance.getGuis().getItemMaterial("chunks", "chunk-item"), 1);
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(title);
                meta.setLore(lore2);
                item.setItemMeta(meta);
                inv.setItem(i, item);
                i++;
            }
        }
        
        Set<String> custom_items = new HashSet<>(instance.getGuis().getCustomItems("chunks"));
        for (String key : custom_items) {
            List<String> custom_lore = new ArrayList<>(instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("chunks", key), player));
            String title = instance.getSettings().getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, instance.getGuis().getCustomItemTitle("chunks", key)) : instance.getGuis().getCustomItemTitle("chunks", key);
            if (instance.getGuis().getCustomItemCheckCustomModelData("chunks", key)) {
                inv.setItem(instance.getGuis().getCustomItemSlot("chunks", key), instance.getGuis().createItemWMD(title,
                        custom_lore,
                        instance.getGuis().getCustomItemMaterialMD("chunks", key),
                        instance.getGuis().getCustomItemCustomModelData("chunks", key)));
            } else {
                inv.setItem(instance.getGuis().getCustomItemSlot("chunks", key), instance.getGuis().createItem(instance.getGuis().getCustomItemMaterial("chunks", key),
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
        if (instance.getGuis().getItemCheckCustomModelData("chunks", "back-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("chunks", "back-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("chunks", "back-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("chunks", "back-page-list");
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
        if (instance.getGuis().getItemCheckCustomModelData("chunks", "back-page-main")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("chunks", "back-page-main"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("chunks", "back-page-main"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("chunks", "back-page-main");
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
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("back-page-main-lore").replaceAll("%claim-name%", claim.getName())));
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
        if (instance.getGuis().getItemCheckCustomModelData("chunks", "next-page-list")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("chunks", "next-page-list"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("chunks", "next-page-list"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("chunks", "next-page-list");
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
     * @param player The player.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
