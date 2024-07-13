package fr.xyness.SCS.Config;

import java.io.File;
import java.io.FilenameFilter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.SimpleClaimSystem;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * Manages GUI settings and operations for the claim system.
 */
public class ClaimGuis {

	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** Stores settings for GUI items */
    private Map<String, Map<String, Map<String, String>>> guis_item_settings = new HashMap<>();
    
    /** Stores settings for custom GUI items */
    private Map<String, Map<String, Map<String, String>>> guis_custom_item_settings = new HashMap<>();
    
    /** Stores actions for custom GUI items */
    private Map<String, Map<Integer, String>> guis_custom_item_actions = new HashMap<>();
    
    /** Stores general GUI settings */
    private Map<String, Map<String, String>> guis_settings = new HashMap<>();
    
    /** Stores permissions for clicked slots in GUIs */
    private Map<Integer, String> guis_items_perms_clicked_slots = new HashMap<>();
    
    /** Set of settings names */
    private Set<String> settings_name = Set.of("Build", "Destroy", "Buttons", "Items", "InteractBlocks", "Levers", "Plates", "Doors", "Trapdoors",
            "Fencegates","Tripwires","RepeatersComparators","Bells","Entities","Explosions","Liquids","Redstone","Frostwalker","Firespread",
            "Teleportations","Damages","Visitors","Pvp","Monsters","Weather","Fly");
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimGuis.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimGuis(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    

    // ********************
    // *  Others Methods  *
    // ********************
    
    
    /**
     * Clears all stored GUI settings and actions.
     */
    public void clearAll() {
        guis_item_settings.clear();
        guis_custom_item_settings.clear();
        guis_custom_item_actions.clear();
        guis_settings.clear();
        guis_items_perms_clicked_slots.clear();
    }
    
    /**
     * Splits the lore into lines.
     *
     * @param lore The lore to split.
     * @return The list of lore lines.
     */
    public List<String> getLore(String lore) {
        List<String> lores = new ArrayList<>();
        String[] parts = lore.split("\n");
        for (String s : parts) {
            lores.add(s);
        }
        return lores;
    }

    /**
     * Splits the lore into lines with placeholders from PAPI.
     *
     * @param lore   The lore to split.
     * @param player The player for whom the placeholders are set.
     * @return The list of lore lines.
     */
    public List<String> getLoreWP(String lore, Player player) {
        if (!instance.getSettings().getBooleanSetting("placeholderapi")) {
            return getLore(lore);
        }
        List<String> lores = new ArrayList<>();
        String[] parts = lore.split("\n");
        for (String s : parts) {
            lores.add(PlaceholderAPI.setPlaceholders(player, s));
        }
        return lores;
    }
    
    /**
     * Get a list of lore lines with placeholders replaced.
     * 
     * @param lore       The list of lore lines.
     * @param playerName The name of the player for whom to replace placeholders.
     * @param target The targeted player for whom to replace placeholders.
     * @return A list of lore lines with placeholders replaced.
     */
    public List<String> getLoreWP(List<String> lore, String playerName, OfflinePlayer target) {
        if (!instance.getSettings().getBooleanSetting("placeholderapi")) {
            return lore;
        }
        List<String> lores = new ArrayList<>();
        for (String line : lore) {
            lores.add(PlaceholderAPI.setPlaceholders(target, line));
        }
        return lores;
    }

    /**
     * Creates an item in the GUI.
     *
     * @param material The material of the item.
     * @param name     The name of the item.
     * @param lore     The lore of the item.
     * @return The created ItemStack.
     */
    public ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = null;
        if (material == null) {
            instance.getPlugin().getLogger().info("Error material loading, check list.yml");
            instance.getPlugin().getLogger().info("Using STONE instead");
            item = new ItemStack(Material.STONE, 1);
        } else {
            item = new ItemStack(material, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Creates a custom item in the GUI.
     *
     * @param name             The name of the item.
     * @param lore             The lore of the item.
     * @param name_custom_item The custom item name.
     * @param model_data       The custom model data value.
     * @return The created ItemStack.
     */
    public ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if (customStack == null) {
            instance.getPlugin().getLogger().info("Error custom item loading : " + name_custom_item);
            instance.getPlugin().getLogger().info("Using STONE instead");
            item = new ItemStack(Material.STONE, 1);
        } else {
            item = customStack.getItemStack();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(model_data);
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Custom items

    /**
     * Gets the set of custom items for a specific GUI.
     *
     * @param gui The GUI identifier.
     * @return The set of custom items, or null if none exist.
     */
    public Set<String> getCustomItems(String gui) {
        return guis_custom_item_settings.getOrDefault(gui, Map.of()).keySet();
    }

    /**
     * Gets the title of a custom item.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return The title of the custom item, or an empty string if not found.
     */
    public String getCustomItemTitle(String gui, String item) {
        return getCustomItemProperty(gui, item, "title", "");
    }

    /**
     * Gets the lore of a custom item.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return The lore of the custom item, or an empty string if not found.
     */
    public String getCustomItemLore(String gui, String item) {
        return getCustomItemProperty(gui, item, "lore", "");
    }

    /**
     * Gets the material of a custom item.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return The material of the custom item, or null if not found.
     */
    public Material getCustomItemMaterial(String gui, String item) {
        return Material.getMaterial(getCustomItemProperty(gui, item, "material", null));
    }

    /**
     * Gets the slot number of a custom item.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return The slot number of the custom item, or 0 if not found.
     */
    public int getCustomItemSlot(String gui, String item) {
        return Integer.parseInt(getCustomItemProperty(gui, item, "slot", "0"));
    }

    /**
     * Checks if a custom item has custom model data.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return true if the custom item has custom model data, false otherwise.
     */
    public boolean getCustomItemCheckCustomModelData(String gui, String item) {
        return Boolean.parseBoolean(getCustomItemProperty(gui, item, "custom_model_data", "false"));
    }

    /**
     * Gets the custom model data value of a custom item.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return The custom model data value, or 0 if not found.
     */
    public int getCustomItemCustomModelData(String gui, String item) {
        return Integer.parseInt(getCustomItemProperty(gui, item, "custom_model_data_value", "0"));
    }

    /**
     * Gets the material of a custom item including custom model data.
     *
     * @param gui  The GUI identifier.
     * @param item The custom item identifier.
     * @return The material string of the custom item, or null if not found.
     */
    public String getCustomItemMaterialMD(String gui, String item) {
        return getCustomItemProperty(gui, item, "material", null);
    }

    /**
     * Gets the action associated with a custom item at a specific slot.
     *
     * @param gui  The GUI identifier.
     * @param slot The slot number.
     * @return The action string, or null if not found.
     */
    public String getCustomItemAction(String gui, int slot) {
        return guis_custom_item_actions.getOrDefault(gui, Map.of()).get(slot);
    }

    // Standard items

    /**
     * Checks if a given name is a permission setting.
     *
     * @param name The name to check.
     * @return true if the name is a permission setting, false otherwise.
     */
    public boolean isAPerm(String name) {
        return settings_name.contains(name);
    }

    /**
     * Gets the set of permission settings names.
     *
     * @return The set of permission settings names.
     */
    public Set<String> getPerms() {
        return settings_name;
    }

    /**
     * Gets the name of the setting associated with a specific slot.
     *
     * @param slot The slot number.
     * @return The name of the setting, or an empty string if not found.
     */
    public String getSlotPerm(int slot) {
        return guis_items_perms_clicked_slots.getOrDefault(slot, "");
    }

    /**
     * Gets the set of items for a specific GUI.
     *
     * @param gui The GUI identifier.
     * @return The set of items, or null if none exist.
     */
    public Set<String> getItems(String gui) {
        return guis_item_settings.getOrDefault(gui, Map.of()).keySet();
    }

    /**
     * Gets the material of an item in a specific GUI.
     *
     * @param gui  The GUI identifier.
     * @param item The item identifier.
     * @return The material of the item, or null if not found.
     */
    public Material getItemMaterial(String gui, String item) {
        return Material.getMaterial(getItemProperty(gui, item, "material", null));
    }

    /**
     * Gets the material string of an item in a specific GUI.
     *
     * @param gui  The GUI identifier.
     * @param item The item identifier.
     * @return The material string of the item, or null if not found.
     */
    public String getItemMaterialMD(String gui, String item) {
        return getItemProperty(gui, item, "material", null);
    }

    /**
     * Gets the slot number of an item in a specific GUI.
     *
     * @param gui  The GUI identifier.
     * @param item The item identifier.
     * @return The slot number of the item, or 0 if not found.
     */
    public int getItemSlot(String gui, String item) {
        return Integer.parseInt(getItemProperty(gui, item, "slot", "0"));
    }

    /**
     * Checks if an item has custom model data.
     *
     * @param gui  The GUI identifier.
     * @param item The item identifier.
     * @return true if the item has custom model data, false otherwise.
     */
    public boolean getItemCheckCustomModelData(String gui, String item) {
        return Boolean.parseBoolean(getItemProperty(gui, item, "custom_model_data", "false"));
    }

    /**
     * Gets the custom model data value of an item.
     *
     * @param gui  The GUI identifier.
     * @param item The item identifier.
     * @return The custom model data value, or 0 if not found.
     */
    public int getItemCustomModelData(String gui, String item) {
        return Integer.parseInt(getItemProperty(gui, item, "custom_model_data_value", "0"));
    }

    /**
     * Gets the number of rows in a specific GUI.
     *
     * @param gui The GUI identifier.
     * @return The number of rows, or 0 if not found.
     */
    public int getGuiRows(String gui) {
        return Integer.parseInt(getGuiProperty(gui, "rows", "0"));
    }

    /**
     * Gets the maximum slot number for a specific GUI.
     *
     * @param gui The GUI identifier.
     * @return The maximum slot number, or 0 if not found.
     */
    public int getGuiMaxSlot(String gui) {
        return Integer.parseInt(getGuiProperty(gui, "list-end-slot", "0"));
    }

    /**
     * Gets the minimum slot number for a specific GUI.
     *
     * @param gui The GUI identifier.
     * @return The minimum slot number, or 0 if not found.
     */
    public int getGuiMinSlot(String gui) {
        return Integer.parseInt(getGuiProperty(gui, "list-start-slot", "0"));
    }

    /**
     * Gets the title of a specific GUI.
     *
     * @param gui The GUI identifier.
     * @return The title of the GUI, or an empty string if not found.
     */
    public String getGuiTitle(String gui) {
        return getGuiProperty(gui, "gui-title", "");
    }

    /**
     * Checks if a slot is a clickable slot.
     *
     * @param clickedSlot The slot number.
     * @return true if the slot is clickable, false otherwise.
     */
    public boolean isAllowedSlot(int clickedSlot) {
        return guis_items_perms_clicked_slots.containsKey(clickedSlot);
    }

    /**
     * Gets the property of a custom item.
     *
     * @param gui    The GUI identifier.
     * @param item   The custom item identifier.
     * @param key    The property key.
     * @param defVal The default value if not found.
     * @return The property value, or the default value if not found.
     */
    private String getCustomItemProperty(String gui, String item, String key, String defVal) {
        return guis_custom_item_settings.getOrDefault(gui, Map.of()).getOrDefault(item, Map.of()).getOrDefault(key, defVal);
    }

    /**
     * Gets the property of an item.
     *
     * @param gui    The GUI identifier.
     * @param item   The item identifier.
     * @param key    The property key.
     * @param defVal The default value if not found.
     * @return The property value, or the default value if not found.
     */
    private String getItemProperty(String gui, String item, String key, String defVal) {
        return guis_item_settings.getOrDefault(gui, Map.of()).getOrDefault(item, Map.of()).getOrDefault(key, defVal);
    }

    /**
     * Gets the property of a GUI.
     *
     * @param gui    The GUI identifier.
     * @param key    The property key.
     * @param defVal The default value if not found.
     * @return The property value, or the default value if not found.
     */
    private String getGuiProperty(String gui, String key, String defVal) {
        return guis_settings.getOrDefault(gui, Map.of()).getOrDefault(key, defVal);
    }

    /**
     * Loads GUI settings from configuration files.
     *
     * @param plugin The JavaPlugin instance.
     * @param check_itemsadder Flag to check itemsadder.
     * @return true if settings were loaded successfully, false otherwise.
     */
    public boolean loadGuiSettings(JavaPlugin plugin, boolean check_itemsadder) {
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
                if (!updateGuiWithDefaults(plugin, file.getName())) {
                    continue;
                }
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                // Settings loading
                Map<String, String> settings = new HashMap<>();
                settings.put("rows", config.getString("rows"));
                settings.put("gui-title", config.getString("gui-title"));
                settings.put("list-start-slot", config.getString("list-start-slot"));
                settings.put("list-end-slot", config.getString("list-end-slot"));
                guis_settings.put(file.getName().replace(".yml", ""), settings);
                
                // Items loading
                ConfigurationSection configSection = config.getConfigurationSection("items");
                Map<String, Map<String, String>> items = new HashMap<>();
                for (String key : configSection.getKeys(false)) {
                    Map<String, String> item_settings = new HashMap<>();
                    item_settings.put("slot", configSection.getString(key + ".slot"));
                    if (configSection.getString(key + ".custom_model_data").equals("true") && !check_itemsadder) {
                        model_data = true;
                        item_settings.put("material", "STONE");
                        item_settings.put("custom_model_data", "false");
                        item_settings.put("custom_model_data_value", "0");
                    } else {
                        item_settings.put("material", configSection.getString(key + ".material"));
                        item_settings.put("custom_model_data", configSection.getString(key + ".custom_model_data"));
                        item_settings.put("custom_model_data_value", configSection.getString(key + ".custom_model_data_value"));
                    }
                    if (settings_name.contains(key)) {
                        guis_items_perms_clicked_slots.put(configSection.getInt(key + ".slot"), key);
                    }
                    items.put(key, item_settings);
                }
                guis_item_settings.put(file.getName().replace(".yml", ""), items);
                
                // Custom items loading
                configSection = config.getConfigurationSection("custom_items");
                items = new HashMap<>();
                Map<Integer, String> actions = new HashMap<>();
                for (String key : configSection.getKeys(false)) {
                    Map<String, String> item_settings = new HashMap<>();
                    item_settings.put("title", configSection.getString(key + ".title"));
                    item_settings.put("lore", configSection.getString(key + ".lore"));
                    item_settings.put("slot", configSection.getString(key + ".slot"));
                    item_settings.put("material", configSection.getString(key + ".material"));
                    item_settings.put("custom_model_data", configSection.getString(key + ".custom_model_data"));
                    item_settings.put("custom_model_data_value", configSection.getString(key + ".custom_model_data_value"));
                    items.put(key, item_settings);
                    actions.put(configSection.getInt(key + ".slot"), configSection.getString(key + ".action"));
                }
                guis_custom_item_actions.put(file.getName().replace(".yml", ""), actions);
                guis_custom_item_settings.put(file.getName().replace(".yml", ""), items);
            }
        }
        
        if (model_data && !check_itemsadder) plugin.getLogger().info("Custom model data will be replaced by STONE");
        return true;
    }

    /**
     * Updates a GUI configuration file with default values and removes obsolete keys in the "items:" section.
     *
     * @param plugin The JavaPlugin instance.
     * @param fileName The name of the configuration file.
     * @return true if the configuration was updated successfully, false otherwise.
     */
    public boolean updateGuiWithDefaults(JavaPlugin plugin, String fileName) {
        File configFile = new File(plugin.getDataFolder() + File.separator + "guis", fileName);
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource("guis/" + fileName);
        if (defConfigStream == null) {
            return false;
        }
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));

        boolean changed = false;

        // Add missing keys
        for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
                changed = true;
            }
        }

        // Remove obsolete keys in the "items:" section
        if (config.contains("items")) {
            Set<String> defaultItemKeys = defConfig.getConfigurationSection("items").getKeys(true);
            Set<String> currentItemKeys = config.getConfigurationSection("items").getKeys(true);

            for (String key : currentItemKeys) {
                if (!defaultItemKeys.contains(key)) {
                    config.set("items." + key, null);
                    changed = true;
                }
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


    /**
     * Sets item flags for an item meta.
     *
     * @param meta The ItemMeta to set flags on.
     * @return The ItemMeta with the flags set.
     */
    public ItemMeta setItemFlag(ItemMeta meta) {
        if (!Bukkit.getVersion().contains("1.21")) {
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
        return meta;
    }

    /**
     * Executes a custom action when a GUI item is clicked.
     *
     * @param player The player clicking the item.
     * @param gui The GUI identifier.
     * @param clickedSlot The slot that was clicked.
     * @param click The type of click.
     */
    public void executeAction(Player player, String gui, int clickedSlot, ClickType click) {
        String action = getCustomItemAction(gui, clickedSlot);
        if (action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
        String[] parts = action.split(":");

        if (parts[0].equalsIgnoreCase("left") && click == ClickType.LEFT) {
            if (parts.length < 2) return;
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) return;
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand(player, parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) return;
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
        if (parts[0].equalsIgnoreCase("right") && click == ClickType.RIGHT) {
            if (parts.length < 2) return;
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) return;
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand(player, parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) return;
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
        if (parts[0].equalsIgnoreCase("shift_left") && click == ClickType.SHIFT_LEFT) {
            if (parts.length < 2) return;
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) return;
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand(player, parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) return;
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
        if (parts[0].equalsIgnoreCase("shift_right") && click == ClickType.SHIFT_RIGHT) {
            if (parts.length < 2) return;
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) return;
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand(player, parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) return;
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
    }
}
