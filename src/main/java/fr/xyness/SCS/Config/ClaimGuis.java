package fr.xyness.SCS.Config;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class ClaimGuis {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
    // Guis settings
    private static Map<String,Map<String,Map<String,String>>> guis_item_settings = new HashMap<>();
    private static Map<String,Map<String,Map<String,String>>> guis_custom_item_settings = new HashMap<>();
    private static Map<String,Map<Integer,String>> guis_custom_item_actions = new HashMap<>();
    private static Map<String,Map<String,String>> guis_settings = new HashMap<>();
    private static Map<Integer,String> guis_items_perms_clicked_slots = new HashMap<>();
    private static Set<String> settings_name = Set.of("Build", "Destroy", "Buttons", "Items", "InteractBlocks", "Levers", "Plates", "Doors", "Trapdoors",
    		"Fencegates","Tripwires","RepeatersComparators","Bells","Entities","Explosions","Liquids","Redstone","Frostwalker","Firespread",
    		"Teleportations","Damages","Visitors","Pvp","Monsters","Weather","Fly");
    
    
    // *****************
	// *  GUI Methods  *
 	// *****************
     
    
    // Clear all
    public static void clearAll() {
    	guis_item_settings.clear();
    	guis_custom_item_settings.clear();
    	guis_custom_item_actions.clear();
    	guis_settings.clear();
    	guis_items_perms_clicked_slots.clear();
    }
    
     // Customs items
     
     // Get custom items set
     public static Set<String> getCustomItems(String gui){
     	if(guis_custom_item_settings.containsKey(gui)) {
     		return guis_custom_item_settings.get(gui).keySet();
     	}
     	return null;
     }
     
     // Get custom item title
     public static String getCustomItemTitle(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return guis_custom_item_settings.get(gui).get(item).get("title");
     		}
     	}
     	return "";
     }
     
     // Get custom item lore
     public static String getCustomItemLore(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return guis_custom_item_settings.get(gui).get(item).get("lore");
     		}
     	}
     	return "";
     }
     
     // Get custom item material
     public static Material getCustomItemMaterial(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return Material.getMaterial(guis_custom_item_settings.get(gui).get(item).get("material"));
     		}
     	}
     	return null;
     }
     
     // Get custom item slot number
     public static int getCustomItemSlot(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return Integer.parseInt(guis_custom_item_settings.get(gui).get(item).get("slot"));
     		}
     	}
     	return 0;
     }
     
     // Check if custom item has a custom model data
     public static boolean getCustomItemCheckCustomModelData(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return Boolean.parseBoolean(guis_custom_item_settings.get(gui).get(item).get("custom_model_data"));
     		}
     	}
     	return false;
     }
     
     // Get custom item custom model data
     public static int getCustomItemCustomModelData(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return Integer.parseInt(guis_custom_item_settings.get(gui).get(item).get("custom_model_data_value"));
     		}
     	}
     	return 0;
     }
     
     // Get custom item material with custom model data
     public static String getCustomItemMaterialMD(String gui, String item) {
     	if(guis_custom_item_settings.containsKey(gui)) {
     		if(guis_custom_item_settings.get(gui).containsKey(item)) {
     			return guis_custom_item_settings.get(gui).get(item).get("material");
     		}
     	}
     	return null;
     }
     
     // Get custom item button action
     public static String getCustomItemAction(String gui, int slot) {
     	if(guis_custom_item_actions.containsKey(gui)) {
     		if(guis_custom_item_actions.get(gui).containsKey(slot)) {
     			return guis_custom_item_actions.get(gui).get(slot);
     		}
     	}
     	return null;
     }
     
     // Items classiques
     
     // Check if the button is a perm setting
     public static boolean isAPerm(String name) {
     	return settings_name.contains(name);
     }
     
     // Get the settings' name set
     public static Set<String> getPerms(){
     	return settings_name;
     }
     
     // Return the name of the setting with the given slot
     public static String getSlotPerm(int slot) {
     	if(guis_items_perms_clicked_slots.containsKey(slot)) {
     		return guis_items_perms_clicked_slots.get(slot);
     	}
     	return "";
     }
     
     // Get the items of a gui
     public static Set<String> getItems(String gui){
     	if(guis_item_settings.containsKey(gui)) {
     		return guis_item_settings.get(gui).keySet();
     	}
     	return null;
     }
     
     // Get the item material
     public static Material getItemMaterial(String gui, String item) {
     	if(guis_item_settings.containsKey(gui)) {
     		if(guis_item_settings.get(gui).containsKey(item)) {
     			return Material.getMaterial(guis_item_settings.get(gui).get(item).get("material"));
     		}
     	}
     	return null;
     }
     
     // Get the item string material
     public static String getItemMaterialMD(String gui, String item) {
     	if(guis_item_settings.containsKey(gui)) {
     		if(guis_item_settings.get(gui).containsKey(item)) {
     			return guis_item_settings.get(gui).get(item).get("material");
     		}
     	}
     	return null;
     }
     
     // Get item slot number
     public static int getItemSlot(String gui, String item) {
     	if(guis_item_settings.containsKey(gui)) {
     		if(guis_item_settings.get(gui).containsKey(item)) {
     			return Integer.parseInt(guis_item_settings.get(gui).get(item).get("slot"));
     		}
     	}
     	return 0;
     }
     
     // Check if the item has a custom model data
     public static boolean getItemCheckCustomModelData(String gui, String item) {
     	if(guis_item_settings.containsKey(gui)) {
     		if(guis_item_settings.get(gui).containsKey(item)) {
     			return Boolean.parseBoolean(guis_item_settings.get(gui).get(item).get("custom_model_data"));
     		}
     	}
     	return false;
     }
     
     // Get the item custom model data
     public static int getItemCustomModelData(String gui, String item) {
     	if(guis_item_settings.containsKey(gui)) {
     		if(guis_item_settings.get(gui).containsKey(item)) {
     			return Integer.parseInt(guis_item_settings.get(gui).get(item).get("custom_model_data_value"));
     		}
     	}
     	return 0;
     }
     
     // Get the number of rows of a gui
     public static int getGuiRows(String gui) {
     	if(guis_settings.containsKey(gui)) {
     		return Integer.parseInt(guis_settings.get(gui).get("rows"));
     	}
     	return 0;
     }
     
     // Get the number of max gui slot
     public static int getGuiMaxSlot(String gui) {
     	if(guis_settings.containsKey(gui)) {
     		return Integer.parseInt(guis_settings.get(gui).get("list-end-slot"));
     	}
     	return 0;
     }
     
     // Get the number of min gui slot
     public static int getGuiMinSlot(String gui) {
     	if(guis_settings.containsKey(gui)) {
     		return Integer.parseInt(guis_settings.get(gui).get("list-start-slot"));
     	}
     	return 0;
     }
     
     // Get the title of a gui
     public static String getGuiTitle(String gui) {
     	if(guis_settings.containsKey(gui)) {
     		return guis_settings.get(gui).get("gui-title");
     	}
     	return "";
     }
     
     // Check if the given slot is a clickable slot
     public static boolean isAllowedSlot(int clickedSlot) {
     	return guis_items_perms_clicked_slots.containsKey(clickedSlot);
     }
     
     // Method to load gui settings
     public static boolean loadGuiSettings(JavaPlugin plugin, boolean check_itemsadder) {
     	File guisDir = new File(plugin.getDataFolder(), "guis");
         
         if (!guisDir.exists()) {
             guisDir.mkdirs();
             return false;
         }

         FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase().endsWith(".yml");
         File[] guiFiles = guisDir.listFiles(ymlFilter);
         
         boolean model_data = false;

         if (guiFiles != null) {
             for (File file : guiFiles) {
             	if(!updateGuiWithDefaults(plugin,file.getName())) {
             		continue;
             	}
             	FileConfiguration config = YamlConfiguration.loadConfiguration(file);
             	
             	// Settings loading
             	Map<String,String> settings = new HashMap<>();
             	settings.put("rows", config.getString("rows"));
             	settings.put("gui-title", config.getString("gui-title"));
         		settings.put("list-start-slot", config.getString("list-start-slot"));
         		settings.put("list-end-slot", config.getString("list-end-slot"));
             	guis_settings.put(file.getName().replace(".yml", ""), settings);
             	
             	// Items loading
             	ConfigurationSection configSection = config.getConfigurationSection("items");
             	Map<String,Map<String,String>> items = new HashMap<>();
             	for (String key : configSection.getKeys(false)) {
             		Map<String,String> item_settings = new HashMap<>();
             		item_settings.put("slot", configSection.getString(key+".slot"));
             		if(configSection.getString(key+".custom_model_data").equals("true") && !check_itemsadder) {
             			model_data = true;
                 		item_settings.put("material", "STONE");
                 		item_settings.put("custom_model_data", "false");
                 		item_settings.put("custom_model_data_value", "0");
             		} else {
                 		item_settings.put("material", configSection.getString(key+".material"));
                 		item_settings.put("custom_model_data", configSection.getString(key+".custom_model_data"));
                 		item_settings.put("custom_model_data_value", configSection.getString(key+".custom_model_data_value"));
             		}
             		if(settings_name.contains(key)) {
             			guis_items_perms_clicked_slots.put(configSection.getInt(key+".slot"),key);
             		}
             		items.put(key, item_settings);
             	}
             	guis_item_settings.put(file.getName().replace(".yml", ""), items);
             	
             	// Custom items loading
             	configSection = config.getConfigurationSection("custom_items");
             	items = new HashMap<>();
             	Map<Integer,String> actions = new HashMap<>();
             	for (String key : configSection.getKeys(false)) {
             		Map<String,String> item_settings = new HashMap<>();
             		item_settings.put("title", configSection.getString(key+".title"));
             		item_settings.put("lore", configSection.getString(key+".lore"));
             		item_settings.put("slot", configSection.getString(key+".slot"));
             		item_settings.put("material", configSection.getString(key+".material"));
             		item_settings.put("custom_model_data", configSection.getString(key+".custom_model_data"));
             		item_settings.put("custom_model_data_value", configSection.getString(key+".custom_model_data_value"));
             		items.put(key, item_settings);
             		actions.put(configSection.getInt(key+".slot"), configSection.getString(key+".action"));
             	}
             	guis_custom_item_actions.put(file.getName().replace(".yml", ""), actions);
             	guis_custom_item_settings.put(file.getName().replace(".yml", ""), items);
             }
         }
         
         if(model_data && !check_itemsadder) plugin.getLogger().info("Custom model data will be replaced by STONE");
     	return true;
     }
     
     // Method to add new features in guis
     public static boolean updateGuiWithDefaults(JavaPlugin plugin, String fileName) {
         File configFile = new File(plugin.getDataFolder() + File.separator + "guis", fileName);
         if (!configFile.exists()) {
         	plugin.saveDefaultConfig();
         }

         FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
         InputStream defConfigStream = plugin.getResource("guis/"+fileName);
         if (defConfigStream == null) {
             return false;
         }
         YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));

         boolean changed = false;
         for (String key : defConfig.getKeys(true)) {
             if (!config.contains(key)) {
                 config.set(key, defConfig.get(key));
                 changed = true;
             }
         }

         if (changed) {
             try {
                 config.save(configFile);
                 return true;
             } catch (java.io.IOException e) {
                 e.printStackTrace();
                 return false;
             }
         }
         return true;
     }
     
     // Method to set item flags
     public static ItemMeta setItemFlag(ItemMeta meta) {
    	 if(Bukkit.getVersion().contains("1.20")) {
    		 meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ARMOR_TRIM);
    	 }
    	 meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
    	 return meta;
     }
     
     // Method to execute custom actions
     public static void executeAction(Player player, String gui, int clickedSlot, ClickType click) {
    	 
    	 String action = ClaimGuis.getCustomItemAction(gui, clickedSlot);
         if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
         String[] parts = action.split(":");
         
         if(parts[0].equalsIgnoreCase("left") && click == ClickType.LEFT) {
         	if(parts.length<2) return;
	            if(parts[1].equalsIgnoreCase("close_inventory")) {
	            	player.closeInventory();
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("cmd")) {
	            	if(parts.length<3) return;
	            	if(parts[2].equalsIgnoreCase("console")) {
	            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
	            		return;
	            	}
	            	if(parts[2].equalsIgnoreCase("player")) {
	            		Bukkit.dispatchCommand(player, parts[3]);
	            		return;
	            	}
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("msg")) {
	            	if(parts.length<3) return;
	            	player.sendMessage(parts[2]);
	            	return;
	            }
         	return;
         }
         if(parts[0].equalsIgnoreCase("right") && click == ClickType.RIGHT) {
         	if(parts.length<2) return;
	            if(parts[1].equalsIgnoreCase("close_inventory")) {
	            	player.closeInventory();
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("cmd")) {
	            	if(parts.length<3) return;
	            	if(parts[2].equalsIgnoreCase("console")) {
	            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
	            		return;
	            	}
	            	if(parts[2].equalsIgnoreCase("player")) {
	            		Bukkit.dispatchCommand(player, parts[3]);
	            		return;
	            	}
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("msg")) {
	            	if(parts.length<3) return;
	            	player.sendMessage(parts[2]);
	            	return;
	            }
         	return;
         }
         if(parts[0].equalsIgnoreCase("shift_left") && click == ClickType.SHIFT_LEFT) {
         	if(parts.length<2) return;
	            if(parts[1].equalsIgnoreCase("close_inventory")) {
	            	player.closeInventory();
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("cmd")) {
	            	if(parts.length<3) return;
	            	if(parts[2].equalsIgnoreCase("console")) {
	            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
	            		return;
	            	}
	            	if(parts[2].equalsIgnoreCase("player")) {
	            		Bukkit.dispatchCommand(player, parts[3]);
	            		return;
	            	}
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("msg")) {
	            	if(parts.length<3) return;
	            	player.sendMessage(parts[2]);
	            	return;
	            }
         	return;
         }
         if(parts[0].equalsIgnoreCase("shift_right") && click == ClickType.SHIFT_RIGHT) {
         	if(parts.length<2) return;
	            if(parts[1].equalsIgnoreCase("close_inventory")) {
	            	player.closeInventory();
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("cmd")) {
	            	if(parts.length<3) return;
	            	if(parts[2].equalsIgnoreCase("console")) {
	            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
	            		return;
	            	}
	            	if(parts[2].equalsIgnoreCase("player")) {
	            		Bukkit.dispatchCommand(player, parts[3]);
	            		return;
	            	}
	            	return;
	            }
	            if(parts[1].equalsIgnoreCase("msg")) {
	            	if(parts.length<3) return;
	            	player.sendMessage(parts[2]);
	            	return;
	            }
         	return;
         }
         return;
     }
}
