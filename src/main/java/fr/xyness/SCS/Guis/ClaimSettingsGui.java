package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;

/**
 * Class representing the Claim GUI.
 */
public class ClaimSettingsGui implements InventoryHolder {

	
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
        keyToSlotMap.put("Build", 10);
        keyToSlotMap.put("Destroy", 11);
        keyToSlotMap.put("Buttons", 12);
        keyToSlotMap.put("Items", 13);
        keyToSlotMap.put("InteractBlocks", 14);
        keyToSlotMap.put("Levers", 15);
        keyToSlotMap.put("Plates", 16);
        keyToSlotMap.put("Doors", 19);
        keyToSlotMap.put("Trapdoors", 20);
        keyToSlotMap.put("Fencegates", 21);
        keyToSlotMap.put("Tripwires", 22);
        keyToSlotMap.put("RepeatersComparators", 23);
        keyToSlotMap.put("Bells", 24);
        keyToSlotMap.put("Entities", 25);
        keyToSlotMap.put("Frostwalker", 28);
        keyToSlotMap.put("Teleportations", 29);
        keyToSlotMap.put("Damages", 30);
        keyToSlotMap.put("EnterTeleport", 32);
        keyToSlotMap.put("Fly", 33);
        keyToSlotMap.put("Weather", 34);
        
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
        keyToMaterialMap.put("EnterTeleport", Material.COMPASS);
        keyToMaterialMap.put("Fly", Material.ELYTRA);
        keyToMaterialMap.put("Pvp", Material.DIAMOND_SWORD);
        keyToMaterialMap.put("Weather", Material.SNOWBALL);
    }
    
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
     * @param role   The role associated with permissions.
     */
    public ClaimSettingsGui(Player player, Claim claim, SimpleClaimSystem instance, String role) {
    	this.instance = instance;
    	
    	String role_displayed;
    	switch(role) {
	    	case "members":
	    		role_displayed = instance.getLanguage().getMessage("role-members");
	    		break;
	    	case "natural":
	    		role_displayed = instance.getLanguage().getMessage("role-natural");
	    		break;
    		default:
	    		role_displayed = instance.getLanguage().getMessage("role-visitors");
	    		break;
    	}
    	
    	// Get title
    	String title = instance.getLanguage().getMessage("gui-settings-new-title")
    			.replace("%name%", claim.getName())
    			.replace("%role%", role_displayed);
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, 54, title);
        
        // Load the items asynchronously
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
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        cPlayer.setFilter(role);
	
	    	// Get data
	        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled");
	        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled");
	        String default_choix_disabled = instance.getLanguage().getMessage("choice-disabled");
	        String default_choix_enabled = instance.getLanguage().getMessage("choice-enabled");
	        
	        // Set role item
	        inv.setItem(48, role(role));
	
	        // Set settings items
	        for (String key : instance.getGuis().getPerms(role)) {
	        	
	        	// Get lower name
	            String lower_name = key.toLowerCase();
	            
	            // Get lore of setting
	            List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage(lower_name + "-lore")));
	            
	            // Check setting status
	            boolean permission = claim.getPermission(key,role);
	            
	            // Set status and choice message depending on permission variable
	            String statut = permission ? default_statut_enabled : default_statut_disabled;
	            String choix = permission ? default_choix_enabled : default_choix_disabled;
	            
	            // Add the click button depending on permission of player
	            lore.add(instance.getSettings().isEnabled(key) ? checkPermPerm(player,key) ? choix : instance.getLanguage().getMessage("gui-button-no-permission")+instance.getLanguage().getMessage("to-use-setting") : instance.getLanguage().getMessage("choice-setting-disabled"));
	
	            // Set the item
	            inv.setItem(getSlotByKey(key), instance.getGuis().createItem(
	                    getMaterialByKey(key),
	                    instance.getLanguage().getMessage(lower_name + "-title").replace("%status%", statut),
	                    lore));
	        }
	        
	        // Back main page
	        inv.setItem(49, backPageMain(claim));
	        
	        // Apply all settings
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(instance.getLanguage().getMessage("apply-all-claims-lore")));
	        inv.setItem(50, instance.getGuis().createItem(
	                Material.GREEN_CONCRETE,
	                instance.getLanguage().getMessage("apply-all-claims-title"),lore));
        
	        return true;
	        
    	});
	        
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
    private ItemStack backPageMain(Claim claim) {
        ItemStack item = new ItemStack(Material.DARK_OAK_DOOR);
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
     * Create a role item.
     * 
     * @param role The current role.
     * @return The created ItemStack.
     */
    private ItemStack role(String role) {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = instance.getLanguage().getMessage("role-lore")
                    .replaceAll("%status_color_" + getStatusIndex(role) + "%", instance.getLanguage().getMessage("status_color_active_filter"))
                    .replaceAll("%status_color_[^" + getStatusIndex(role) + "]%", instance.getLanguage().getMessage("status_color_inactive_filter"));
            meta.setDisplayName(instance.getLanguage().getMessage("role-title"));
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
