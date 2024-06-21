package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashSet;
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

public class ClaimGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************
	

    private Inventory inv;
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimGui(Player player, Chunk chunk) {
    	String title = ClaimGuis.getGuiTitle("settings").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
    	if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
    	inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("settings")*9, title);
        initializeItems(player,chunk);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    

    // Method to initialize items for the gui
    public void initializeItems(Player player, Chunk chunk) {

		String default_statut_disabled = ClaimLanguage.getMessage("status-disabled");
    	String default_statut_enabled = ClaimLanguage.getMessage("status-enabled");
    	String default_choix_disabled = ClaimLanguage.getMessage("choice-disabled");
    	String default_choix_enabled = ClaimLanguage.getMessage("choice-enabled");
    	
    	String statut;
    	String choix;
    	List<String> lore;
    	String lower_name;
    	String playerName = player.getName();
    	CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
    	cPlayer.setChunk(chunk);
    	Set<String> items = new HashSet<>(ClaimGuis.getItems("settings"));
    	Claim claim = ClaimMain.getClaimFromChunk(chunk);
    	for(String key : items) {
    		lower_name = key.toLowerCase();
    		lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP(lower_name+"-lore",playerName)));
    		if(ClaimGuis.isAPerm(key)) {
    			boolean permission = claim.getPermission(key);
    			statut = permission ? default_statut_enabled : default_statut_disabled;
    			choix = permission ? default_choix_enabled : default_choix_disabled;
    			lore.add(ClaimSettings.isEnabled(key) ? choix : ClaimLanguage.getMessage("choice-setting-disabled"));
    			if(ClaimGuis.getItemCheckCustomModelData("settings", key)) {
    				inv.setItem(ClaimGuis.getItemSlot("settings", key), createItemWMD(ClaimLanguage.getMessageWP(lower_name+"-title",playerName).replaceAll("%status%", statut),
    						lore,
    						ClaimGuis.getItemMaterialMD("settings", key),
    						ClaimGuis.getItemCustomModelData("settings", key)));
    			} else {
    				inv.setItem(ClaimGuis.getItemSlot("settings", key), createItem(ClaimGuis.getItemMaterial("settings", key),
    						ClaimLanguage.getMessageWP(lower_name+"-title",playerName).replaceAll("%status%", statut),
    						lore));
    			}
    		} else {
    			String title = ClaimLanguage.getMessageWP(lower_name+"-title",playerName).replaceAll("%coords%", ClaimMain.getClaimCoords(chunk)).replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
    			if(!checkPermButton(player,key)) {
    				lore.remove(lore.size()-1);
    				lore.add(ClaimLanguage.getMessage("gui-button-no-permission"));
    			}
    			if(ClaimGuis.getItemCheckCustomModelData("settings", key)) {
    				inv.setItem(ClaimGuis.getItemSlot("settings", key), createItemWMD(title,
    						lore,
    						ClaimGuis.getItemMaterialMD("settings", key),
    						ClaimGuis.getItemCustomModelData("settings", key)));
    			} else {
    				inv.setItem(ClaimGuis.getItemSlot("settings", key), createItem(ClaimGuis.getItemMaterial("settings", key),
    						title,
    						lore));
    			}
    		}
    	}
    	
    	Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("settings"));
    	for(String key : custom_items) {
    		lore = new ArrayList<>(getLoreWP(ClaimGuis.getCustomItemLore("settings", key),player));
    		String title = ClaimGuis.getCustomItemTitle("settings", key);
    		if(ClaimSettings.getBooleanSetting("placeholderapi")) title = PlaceholderAPI.setPlaceholders(player, title);
			if(ClaimGuis.getCustomItemCheckCustomModelData("settings", key)) {
				inv.setItem(ClaimGuis.getCustomItemSlot("settings", key), createItemWMD(title,
						lore,
						ClaimGuis.getCustomItemMaterialMD("settings", key),
						ClaimGuis.getCustomItemCustomModelData("settings", key)));
			} else {
				inv.setItem(ClaimGuis.getCustomItemSlot("settings", key), createItem(ClaimGuis.getCustomItemMaterial("settings", key),
						title,
						lore));
			}
    	}
    }
    
    // Method to check if they have perm with the key
    public static boolean checkPermButton(Player player, String key) {
    	switch(key) {
    	case "define-loc":
    		return player.hasPermission("scs.command.claim.setspawn");
    	case "define-name":
    		return player.hasPermission("scs.command.claim.setname");
    	case "manage-members":
    		return player.hasPermission("scs.command.claim.members");
    	case "manage-bans":
    		return player.hasPermission("scs.command.claim.bans");
    	case "my-claims":
    		return player.hasPermission("scs.command.claim.list");
    	case "apply-all-claims":
    		return true;
    	default:
    		return false;
    	}
    }
    
    // Method to split the lore for lines
    public static List<String> getLore(String lore){
    	List<String> lores = new ArrayList<>();
    	String[] parts = lore.split("\n");
    	for(String s : parts) {
    		lores.add(s);
    	}
    	return lores;
    }
    
    // Method to get the lore with placeholders from PAPI
    public static List<String> getLoreWP(String lore, Player player){
    	if(!ClaimSettings.getBooleanSetting("placeholderapi")) {
    		return getLore(lore);
    	}
    	List<String> lores = new ArrayList<>();
    	String[] parts = lore.split("\n");
    	for(String s : parts) {
    		lores.add(PlaceholderAPI.setPlaceholders(player, s));
    	}
    	return lores;
    }

    // Method to create item in the gui
    private ItemStack createItem(Material material, String name, List<String> lore) {
    	ItemStack item = null;
    	if(material == null) {
        	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check settings.yml");
        	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
        	item = new ItemStack(Material.STONE,1);
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
    
    // Method to create custom item in the gui
    private ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if(customStack == null) {
        	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+name_custom_item);
        	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
        	item = new ItemStack(Material.STONE,1);
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
    
    @Override
    public Inventory getInventory() {
        return inv;
    }
    
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
