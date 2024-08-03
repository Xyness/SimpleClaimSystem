package fr.xyness.SCS.Guis.AdminGestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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
 * Class representing the Admin Gestion Claim GUI.
 */
public class AdminGestionClaimGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /**
     * A map containing the key-to-slot mappings.
     */
    private static final Map<String, Integer> keyToSlotMap = new HashMap<>();

    /**
     * A map containing the key-to-material mappings.
     */
    private static final Map<String, Material> keyToMaterialMap = new HashMap<>();
    
    static {
        keyToSlotMap.put("Build", 1);
        keyToSlotMap.put("Destroy", 2);
        keyToSlotMap.put("Buttons", 3);
        keyToSlotMap.put("Items", 4);
        keyToSlotMap.put("InteractBlocks", 5);
        keyToSlotMap.put("Levers", 6);
        keyToSlotMap.put("Plates", 7);
        keyToSlotMap.put("Doors", 10);
        keyToSlotMap.put("Trapdoors", 11);
        keyToSlotMap.put("Fencegates", 12);
        keyToSlotMap.put("Tripwires", 13);
        keyToSlotMap.put("RepeatersComparators", 14);
        keyToSlotMap.put("Bells", 15);
        keyToSlotMap.put("Entities", 16);
        keyToSlotMap.put("Teleportations", 19);
        keyToSlotMap.put("Damages", 20);
        keyToSlotMap.put("Fly", 21);
        keyToSlotMap.put("Frostwalker", 22);
        keyToSlotMap.put("Weather", 23);
        keyToSlotMap.put("GuiTeleport", 24);
        keyToSlotMap.put("Portals", 25);
        keyToSlotMap.put("Enter", 30);
        keyToSlotMap.put("ItemsPickup", 31);
        keyToSlotMap.put("ItemsDrop", 32);
        
        keyToSlotMap.put("Explosions", 10);
        keyToSlotMap.put("Liquids", 11);
        keyToSlotMap.put("Redstone", 12);
        keyToSlotMap.put("Firespread", 14);
        keyToSlotMap.put("Monsters", 15);
        keyToSlotMap.put("Pvp", 16);

        keyToMaterialMap.put("Build", Material.OAK_STAIRS);
        keyToMaterialMap.put("Destroy", Material.IRON_PICKAXE);
        keyToMaterialMap.put("Buttons", Material.STONE_BUTTON);
        keyToMaterialMap.put("Items", Material.BOW);
        keyToMaterialMap.put("InteractBlocks", Material.RED_SHULKER_BOX);
        keyToMaterialMap.put("Levers", Material.LEVER);
        keyToMaterialMap.put("Plates", Material.STONE_PRESSURE_PLATE);
        keyToMaterialMap.put("Doors", Material.OAK_DOOR);
        keyToMaterialMap.put("Trapdoors", Material.OAK_TRAPDOOR);
        keyToMaterialMap.put("Fencegates", Material.OAK_FENCE_GATE);
        keyToMaterialMap.put("Tripwires", Material.TRIPWIRE_HOOK);
        keyToMaterialMap.put("RepeatersComparators", Material.REPEATER);
        keyToMaterialMap.put("Bells", Material.BELL);
        keyToMaterialMap.put("Entities", Material.ARMOR_STAND);
        keyToMaterialMap.put("Explosions", Material.TNT);
        keyToMaterialMap.put("Liquids", Material.WATER_BUCKET);
        keyToMaterialMap.put("Redstone", Material.REDSTONE);
        keyToMaterialMap.put("Frostwalker", Material.DIAMOND_BOOTS);
        keyToMaterialMap.put("Firespread", Material.CAMPFIRE);
        keyToMaterialMap.put("Teleportations", Material.ENDER_PEARL);
        keyToMaterialMap.put("Damages", Material.APPLE);
        keyToMaterialMap.put("Monsters", Material.STRING);
        keyToMaterialMap.put("GuiTeleport", Material.COMPASS);
        keyToMaterialMap.put("Fly", Material.ELYTRA);
        keyToMaterialMap.put("Pvp", Material.DIAMOND_SWORD);
        keyToMaterialMap.put("Weather", Material.SNOWBALL);
        keyToMaterialMap.put("Portals", Material.END_PORTAL_FRAME);
        keyToMaterialMap.put("Enter", Material.BIRCH_DOOR);
        keyToMaterialMap.put("ItemsPickup", Material.HOPPER);
        keyToMaterialMap.put("ItemsDrop", Material.FEATHER);
    }
    
    /** Inventory for the GUI. */
    private Inventory inv;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the AdminGestionClaimGui.
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
    	
        inv = Bukkit.createInventory(this, 54, "§4[A]§r Settings: "+claim.getName()+" ("+role_displayed+" - " + claim.getOwner() + ")");
        loadItems(player, claim, role).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
    public CompletableFuture<Boolean> loadItems(Player player, Claim claim, String role) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled");
	        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled");
	        String default_choix_disabled = instance.getLanguage().getMessage("choice-disabled");
	        String default_choix_enabled = instance.getLanguage().getMessage("choice-enabled");
	
	        String choix;
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        cPlayer.setClaim(claim);
	        cPlayer.setFilter(role);
	        inv.setItem(48, role(role));
	        for (String key : instance.getGuis().getPerms(role)) {
	            String lower_name = key.toLowerCase();
	            List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage(lower_name + "-lore")));
	            if (instance.getGuis().isAPerm(key,role)) {
	                boolean permission = claim.getPermission(key,role);
	                String statut = permission ? default_statut_enabled : default_statut_disabled;
	                choix = permission ? default_choix_enabled : default_choix_disabled;
	                lore.add(instance.getSettings().isEnabled(key) ? choix : instance.getLanguage().getMessage("choice-setting-disabled"));
	                inv.setItem(getSlotByKey(key), instance.getGuis().createItem(
	                        getMaterialByKey(key),
	                        instance.getLanguage().getMessage(lower_name + "-title").replace("%status%", statut),
	                        lore));
	            }
	        }
	        inv.setItem(49, backMainMenu(claim.getName()));
	        inv.setItem(50, instance.getGuis().createItem(Material.GREEN_CONCRETE, "§cApply settings to all his claims", Arrays.asList("§7Apply these settings to all claims","§7of "+claim.getOwner()+"."," ","§7▸§f Click to apply")));
        
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
        return keyToSlotMap.getOrDefault(key, -1);
    }

    /**
     * Gets the material associated with the given key.
     *
     * @param key The key to lookup.
     * @return The material associated with the key, or null if the key is not found.
     */
    public static Material getMaterialByKey(String key) {
        return keyToMaterialMap.getOrDefault(key, null);
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
                    .replaceAll("%status_color_" + getStatusIndex(role) + "%", instance.getLanguage().getMessage("status_color_active_filter"))
                    .replaceAll("%status_color_[^" + getStatusIndex(role) + "]%", instance.getLanguage().getMessage("status_color_inactive_filter"));
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
