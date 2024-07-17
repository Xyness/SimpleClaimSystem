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
public class ClaimSettingsGui implements InventoryHolder {

	
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
     * Main constructor for the ClaimSettingsGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimSettingsGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
        String title = instance.getGuis().getGuiTitle("settings").replace("%name%", claim.getName());
        if (instance.getSettings().getBooleanSetting("placeholderapi")) {
            title = PlaceholderAPI.setPlaceholders(player, title);
        }
        inv = Bukkit.createInventory(this, instance.getGuis().getGuiRows("settings") * 9, title);
        instance.executeAsync(() -> loadItems(player, claim));
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
    public void loadItems(Player player, Claim claim) {

        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled");
        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled");
        String default_choix_disabled = instance.getLanguage().getMessage("choice-disabled");
        String default_choix_enabled = instance.getLanguage().getMessage("choice-enabled");

        String choix;
        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
        cPlayer.setClaim(claim);
        Set<String> items = new HashSet<>(instance.getGuis().getItems("settings"));
        for (String key : items) {
            String lower_name = key.toLowerCase();
            List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP(lower_name + "-lore", (OfflinePlayer) player)));
            if (instance.getGuis().isAPerm(key)) {
                boolean permission = claim.getPermission(key);
                String statut = permission ? default_statut_enabled : default_statut_disabled;
                choix = permission ? default_choix_enabled : default_choix_disabled;
                lore.add(instance.getSettings().isEnabled(key) ? checkPermPerm(player,key) ? choix : instance.getLanguage().getMessage("gui-button-no-permission")+instance.getLanguage().getMessage("to-use-setting") : instance.getLanguage().getMessage("choice-setting-disabled"));
                if (instance.getGuis().getItemCheckCustomModelData("settings", key)) {
                    inv.setItem(instance.getGuis().getItemSlot("settings", key),
                    		instance.getGuis().createItemWMD(instance.getLanguage().getMessageWP(lower_name + "-title", (OfflinePlayer) player)
                                    .replace("%status%", statut), lore, instance.getGuis().getItemMaterialMD("settings", key),
                                    instance.getGuis().getItemCustomModelData("settings", key)));
                } else {
                    inv.setItem(instance.getGuis().getItemSlot("settings", key), instance.getGuis().createItem(
                            instance.getGuis().getItemMaterial("settings", key),
                            instance.getLanguage().getMessageWP(lower_name + "-title", (OfflinePlayer) player).replace("%status%", statut),
                            lore));
                }
            }
        }
        
        inv.setItem(instance.getGuis().getItemSlot("settings", "back-page-main"), backPage2(claim));
        
        // Apply all settings
        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessageWP("apply-all-claims-lore", (OfflinePlayer) player)));
        if (instance.getGuis().getItemCheckCustomModelData("settings", "apply-all-claims")) {
            inv.setItem(instance.getGuis().getItemSlot("settings", "apply-all-claims"),
            		instance.getGuis().createItemWMD(instance.getLanguage().getMessageWP("apply-all-claims-title", (OfflinePlayer) player),
            				lore, instance.getGuis().getItemMaterialMD("settings", "apply-all-claims"),
                            instance.getGuis().getItemCustomModelData("settings", "apply-all-claims")));
        } else {
            inv.setItem(instance.getGuis().getItemSlot("settings", "apply-all-claims"), instance.getGuis().createItem(
                    instance.getGuis().getItemMaterial("settings", "apply-all-claims"),
                    instance.getLanguage().getMessageWP("apply-all-claims-title", (OfflinePlayer) player),lore));
        }

        Set<String> custom_items = new HashSet<>(instance.getGuis().getCustomItems("settings"));
        for (String key : custom_items) {
            lore = new ArrayList<>(instance.getGuis().getLoreWP(instance.getGuis().getCustomItemLore("settings", key), player));
            String title = instance.getSettings().getBooleanSetting("placeholderapi") ? PlaceholderAPI.setPlaceholders(player, instance.getGuis().getCustomItemTitle("settings", key)) : instance.getGuis().getCustomItemTitle("settings", key);
            if (instance.getGuis().getCustomItemCheckCustomModelData("settings", key)) {
                inv.setItem(instance.getGuis().getCustomItemSlot("settings", key),
                		instance.getGuis().createItemWMD(title, lore, instance.getGuis().getCustomItemMaterialMD("settings", key),
                                instance.getGuis().getCustomItemCustomModelData("settings", key)));
            } else {
                inv.setItem(instance.getGuis().getCustomItemSlot("settings", key),
                		instance.getGuis().createItem(instance.getGuis().getCustomItemMaterial("settings", key), title, lore));
            }
        }
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }
    
    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param perm    The perm to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermPerm(Player player, String perm) {
    	return instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+perm) || player.hasPermission("scs.setting.*");
    }
    
    /**
     * Creates the back page main item.
     * 
     * @param claim The target claim
     * @return The back page main item.
     */
    private ItemStack backPage2(Claim claim) {
        ItemStack item = null;
        if (instance.getGuis().getItemCheckCustomModelData("settings", "back-page-main")) {
            CustomStack customStack = CustomStack.getInstance(instance.getGuis().getItemMaterialMD("settings", "back-page-main"));
            if (customStack == null) {
                instance.getPlugin().getLogger().info("Error custom item loading: " + instance.getGuis().getItemMaterialMD("settings", "back-page-main"));
                instance.getPlugin().getLogger().info("Using STONE instead");
                item = new ItemStack(Material.STONE, 1);
            } else {
                item = customStack.getItemStack();
            }
        } else {
            Material material = instance.getGuis().getItemMaterial("settings", "back-page-main");
            if (material == null) {
                instance.getPlugin().getLogger().info("Error material loading, check settings.yml");
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
