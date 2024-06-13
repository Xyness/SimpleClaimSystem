package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************
	

    private Inventory inv;
    private static Map<Player,Chunk> chunks = new HashMap<>();
    
    
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
    
    
    // Method to get chunk for a player
    public static Chunk getChunk(Player player) {
    	if(chunks.containsKey(player)) {
    		return chunks.get(player);
    	}
    	return null;
    }
    
    // Method to remove chunk for a player
    public static void removeChunk(Player player) {
    	if(chunks.containsKey(player)) {
    		chunks.remove(player);
    	}
    }

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
    	
    	chunks.put(player, chunk);
    	Set<String> items = new HashSet<>(ClaimGuis.getItems("settings"));
    	for(String key : items) {
    		lower_name = key.toLowerCase();
    		lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP(lower_name+"-lore",playerName)));
    		if(ClaimGuis.isAPerm(key)) {
    	    	statut = default_statut_disabled;
    			choix = default_choix_disabled;
    			if(ClaimMain.canPermCheck(chunk, key)) {
    				statut = default_statut_enabled;
    				choix = default_choix_enabled;
    			}
    			if(ClaimSettings.isEnabled(key)) {
    				lore.add(choix);
    			} else {
    				lore.add(ClaimLanguage.getMessage("choice-setting-disabled"));
    			}
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
    		if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    			title = PlaceholderAPI.setPlaceholders(player, title);
    		}
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
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check list.yml");
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
        	item = new ItemStack(Material.STONE,1);
    	} else {
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Method to create custom item in the gui
    private ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if(customStack == null) {
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+name_custom_item);
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
        	item = new ItemStack(Material.STONE,1);
        } else {
        	item = customStack.getItemStack();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(model_data);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
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
