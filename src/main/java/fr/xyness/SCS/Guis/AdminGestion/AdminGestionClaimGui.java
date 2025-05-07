package fr.xyness.SCS.Guis.AdminGestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import fr.xyness.SCS.Zone;
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
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * The Admin Claim/Zone Settings Management GUI
 * (edits permissions).
 *
 * L'interface utilisateur de gestion des réclamations/paramètres de zone d'administration
 * (permet la navigation vers les autres écrans de l'interface utilisateur).
 */
public class AdminGestionClaimGui implements InventoryHolder {

	
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
     * The Admin Claim/Zone Settings Management GUI constructor
     *  (edits permissions).
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     * @param role   The role associated with permissions.
     */
    public AdminGestionClaimGui(Player player, Claim claim, SimpleClaimSystem instance, String role) {
    	this.instance = instance;
    	
    	String role_displayed;
    	switch(role) {
	    	case "members":
	    		role_displayed = "Members";
	    		break;
	    	case "natural":
	    		role_displayed = "Natural";
	    		break;
    		default:
	    		role_displayed = "Visitors";
	    		break;
    	}
        final Zone zone = claim.setZoneOfGUIByLocation(player);
        // TODO: translate these strings
    	String title = (zone != null)
                ? String.format("§4[A]§r Settings: [%s] in %s (%s - %s)", zone.getName(), claim.getName(), role_displayed, claim.getOwner())
                : String.format("§4[A]§r Settings: %s (%s - %s)", claim.getName(), role_displayed, claim.getOwner());
        inv = Bukkit.createInventory(this, 54, title);
        // This implementation of loadItems calls setGuiZone:
        loadItems(player, claim, role, zone).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error", zone)));
        	}
        })
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param claim  The claim for which the GUI is displayed.
     * @param role   The role associated with permissions.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Player player, Claim claim, String role, Zone zone) {
        // Owner only applies to Claim, so do not check Zone.
    	return CompletableFuture.supplyAsync(() -> {
    	
	        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled", zone);
	        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled", zone);
	        String default_choix_disabled = instance.getLanguage().getMessage("choice-disabled", zone);
	        String default_choix_enabled = instance.getLanguage().getMessage("choice-enabled", zone);
	
	        String choix;
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        cPlayer.setClaim(claim);
	        cPlayer.setFilter(role);
	        cPlayer.setOwner(claim.getOwner());
            cPlayer.setGuiZone(zone);
	        inv.setItem(48, role(role));
	        for (String key : instance.getGuis().getPerms(role)) {
	            String lower_name = key.toLowerCase();
	            List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage(lower_name + "-lore", zone)));
	            if (instance.getGuis().isAPerm(key,role)) {
	                boolean permission = claim.getPermission(key,role);
	                String statut = permission ? default_statut_enabled : default_statut_disabled;
	                choix = permission ? default_choix_enabled : default_choix_disabled;
	                lore.add(instance.getSettings().isEnabled(key) ? choix : instance.getLanguage().getMessage("choice-setting-disabled", zone));
	                inv.setItem(getSlotByKey(key), instance.getGuis().createItem(
	                        getMaterialByKey(key),
	                        instance.getLanguage().getMessage(lower_name + "-title", zone).replace("%status%", statut),
	                        lore));
	            }
	        }
	        inv.setItem(49, backMainMenu(claim.getName()));
            // TODO: translate phrases below
            if (zone != null) {
                inv.setItem(50, instance.getGuis().createItem(Material.GREEN_CONCRETE,
                        "§cApply settings to all zones in claim",
                        Arrays.asList("§7Apply these settings to all zones", String.format("§7in %s (%s).", claim.getName(), claim.getOwner()), " ", "§7▸§f Click to apply")));
            }
            else {
                inv.setItem(50, instance.getGuis().createItem(Material.GREEN_CONCRETE,
                        "§cApply settings to all of owner's claims",
                        Arrays.asList("§7Apply these settings to all claims", "§7of " + claim.getOwner() + ".", " ", "§7▸§f Click to apply")));
            }
	        return true;
	        
    	});
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
    
    /**
     * Gets the slot number associated with the given key.
     *
     * @param key The key to lookup.
     * @return The slot number associated with the key, or -1 if the key is not found.
     */
    public static int getSlotByKey(String key) {
        return ClaimGuis.keyToSlotMap.getOrDefault(key, -1);
    }

    /**
     * Gets the material associated with the given key.
     *
     * @param key The key to lookup.
     * @return The material associated with the key, or null if the key is not found.
     */
    public static Material getMaterialByKey(String key) {
        return ClaimGuis.keyToMaterialMap.getOrDefault(key, null);
    }

    /**
     * Creates an item for the back main menu slot.
     *
     * @param claim_name The name of the current claim.
     * @return The created back page item.
     */
    private ItemStack backMainMenu(String claim_name) {
        ItemStack item = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cMain menu");
            meta.setLore(instance.getGuis().getLore("§7Go back to the main menu of "+claim_name+"\n§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    /**
     * Create a role item.
     * 
     * @param role The current role.
     * @return The created ItemStack.
     */
    private ItemStack role(String role) {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = "§7Change the target role\n%status_color_1%➲ Visitors\n%status_color_2%➲ Members\n%status_color_3%➲ Natural\n§7▸ §fClick to change"
                    .replaceAll("%status_color_" + getStatusIndex(role) + "%", instance.getLanguage().getMessage("status_color_active_filter", zone))
                    .replaceAll("%status_color_[^" + getStatusIndex(role) + "]%", instance.getLanguage().getMessage("status_color_inactive_filter", zone));
            meta.setDisplayName("§eRole");
            meta.setLore(instance.getGuis().getLore(loreFilter));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Get the index of the current role.
     * 
     * @param filter The current role.
     * @return The index of the role.
     */
    private int getStatusIndex(String role) {
        switch (role) {
            case "members":
                return 2;
            case "natural":
                return 3;
            default:
                return 1;
        }
    }
}
