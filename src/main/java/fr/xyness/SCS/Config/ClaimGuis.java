package fr.xyness.SCS.Config;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;
import net.md_5.bungee.api.ChatColor;

/**
 * Manages GUI settings and operations for the claim system.
 */
public class ClaimGuis {

	
    // ***************
    // *  Variables  *
    // ***************

    /** Stores permissions for clicked slots in GUIs */
    private static Map<String,Map<Integer, String>> guis_items_perms_clicked_slots_admin = new HashMap<>();
    
    static {
    	guis_items_perms_clicked_slots_admin.put("visitors", new HashMap<>());
        guis_items_perms_clicked_slots_admin.get("visitors").put(1, "Build");
        guis_items_perms_clicked_slots_admin.get("visitors").put(2, "Destroy");
        guis_items_perms_clicked_slots_admin.get("visitors").put(3, "Buttons");
        guis_items_perms_clicked_slots_admin.get("visitors").put(4, "Items");
        guis_items_perms_clicked_slots_admin.get("visitors").put(5, "InteractBlocks");
        guis_items_perms_clicked_slots_admin.get("visitors").put(6, "Levers");
        guis_items_perms_clicked_slots_admin.get("visitors").put(7, "Plates");
        guis_items_perms_clicked_slots_admin.get("visitors").put(10, "Doors");
        guis_items_perms_clicked_slots_admin.get("visitors").put(11, "Trapdoors");
        guis_items_perms_clicked_slots_admin.get("visitors").put(12, "Fencegates");
        guis_items_perms_clicked_slots_admin.get("visitors").put(13, "Tripwires");
        guis_items_perms_clicked_slots_admin.get("visitors").put(14, "RepeatersComparators");
        guis_items_perms_clicked_slots_admin.get("visitors").put(15, "Bells");
        guis_items_perms_clicked_slots_admin.get("visitors").put(16, "Entities");
        guis_items_perms_clicked_slots_admin.get("visitors").put(19, "Teleportations");
        guis_items_perms_clicked_slots_admin.get("visitors").put(20, "Damages");
        guis_items_perms_clicked_slots_admin.get("visitors").put(21, "Fly");
        guis_items_perms_clicked_slots_admin.get("visitors").put(22, "Frostwalker");
        guis_items_perms_clicked_slots_admin.get("visitors").put(23, "Weather");
        guis_items_perms_clicked_slots_admin.get("visitors").put(24, "GuiTeleport");
        guis_items_perms_clicked_slots_admin.get("visitors").put(25, "Portals");
        guis_items_perms_clicked_slots_admin.get("visitors").put(28, "Elytra");
        guis_items_perms_clicked_slots_admin.get("visitors").put(29, "Enter");
        guis_items_perms_clicked_slots_admin.get("visitors").put(30, "ItemsPickup");
        guis_items_perms_clicked_slots_admin.get("visitors").put(32, "ItemsDrop");
        guis_items_perms_clicked_slots_admin.get("visitors").put(33, "SpecialBlocks");
        guis_items_perms_clicked_slots_admin.get("visitors").put(34, "Windcharges");
        
        
    	guis_items_perms_clicked_slots_admin.put("members", new HashMap<>());
    	guis_items_perms_clicked_slots_admin.get("members").put(1, "Build");
    	guis_items_perms_clicked_slots_admin.get("members").put(2, "Destroy");
    	guis_items_perms_clicked_slots_admin.get("members").put(3, "Buttons");
    	guis_items_perms_clicked_slots_admin.get("members").put(4, "Items");
    	guis_items_perms_clicked_slots_admin.get("members").put(5, "InteractBlocks");
    	guis_items_perms_clicked_slots_admin.get("members").put(6, "Levers");
    	guis_items_perms_clicked_slots_admin.get("members").put(7, "Plates");
    	guis_items_perms_clicked_slots_admin.get("members").put(10, "Doors");
    	guis_items_perms_clicked_slots_admin.get("members").put(11, "Trapdoors");
    	guis_items_perms_clicked_slots_admin.get("members").put(12, "Fencegates");
    	guis_items_perms_clicked_slots_admin.get("members").put(13, "Tripwires");
    	guis_items_perms_clicked_slots_admin.get("members").put(14, "RepeatersComparators");
    	guis_items_perms_clicked_slots_admin.get("members").put(15, "Bells");
    	guis_items_perms_clicked_slots_admin.get("members").put(16, "Entities");
    	guis_items_perms_clicked_slots_admin.get("members").put(19, "Teleportations");
    	guis_items_perms_clicked_slots_admin.get("members").put(20, "Damages");
    	guis_items_perms_clicked_slots_admin.get("members").put(21, "Fly");
    	guis_items_perms_clicked_slots_admin.get("members").put(22, "Frostwalker");
    	guis_items_perms_clicked_slots_admin.get("members").put(23, "Weather");
    	guis_items_perms_clicked_slots_admin.get("members").put(24, "GuiTeleport");
    	guis_items_perms_clicked_slots_admin.get("members").put(25, "Portals");
    	guis_items_perms_clicked_slots_admin.get("members").put(28, "Elytra");
    	guis_items_perms_clicked_slots_admin.get("members").put(29, "Enter");
    	guis_items_perms_clicked_slots_admin.get("members").put(30, "ItemsPickup");
    	guis_items_perms_clicked_slots_admin.get("members").put(32, "ItemsDrop");
    	guis_items_perms_clicked_slots_admin.get("members").put(33, "SpecialBlocks");
    	guis_items_perms_clicked_slots_admin.get("members").put(34, "Windcharges");

        
        guis_items_perms_clicked_slots_admin.put("natural", new HashMap<>());
        guis_items_perms_clicked_slots_admin.get("natural").put(10, "Explosions");
        guis_items_perms_clicked_slots_admin.get("natural").put(11, "Liquids");
        guis_items_perms_clicked_slots_admin.get("natural").put(12, "Redstone");
        guis_items_perms_clicked_slots_admin.get("natural").put(14, "Firespread");
        guis_items_perms_clicked_slots_admin.get("natural").put(15, "Monsters");
        guis_items_perms_clicked_slots_admin.get("natural").put(16, "Pvp");
    }
    
    /** Stores permissions for clicked slots in GUIs */
    private Map<String,Map<Integer, String>> guis_items_perms_clicked_slots = new HashMap<>();
    
    /**
     * A map containing the key-to-slot mappings.
     */
    public static final Map<String, Integer> keyToSlotMap = new HashMap<>();

    /**
     * A map containing the key-to-material mappings.
     */
    public static final Map<String, Material> keyToMaterialMap = new HashMap<>();
    
    /** Set of settings names */
    private Map<String,LinkedHashSet<String>> settings_name = new HashMap<>();
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    public static Map<String,List<GuiSlot>> gui_slots = new HashMap<>();
    public static Map<String,GuiSettings> gui_settings = new HashMap<>();
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimGuis.
     *
     * @param instance The instance of the SimpleClaimSystem instance.
     */
    public ClaimGuis(SimpleClaimSystem instance) {
    	this.instance = instance;
    	
    	// Set settings_name
    	LinkedHashSet<String> permissions = new LinkedHashSet<>();
    	permissions.addAll(List.of("Build", "Destroy", "Buttons", "Items", "InteractBlocks", "Levers", "Plates", "Doors", 
    	    "Trapdoors", "Fencegates", "Tripwires", "RepeatersComparators", "Bells", "Entities", "Frostwalker", "Teleportations", 
    	    "Damages", "Enter", "GuiTeleport", "Weather", "Fly", "Portals", "ItemsPickup", "ItemsDrop", "SpecialBlocks", "Elytra", "Windcharges"));
    	settings_name.put("members", permissions);
    	
    	permissions = new LinkedHashSet<>();
    	permissions.addAll(List.of("Build", "Destroy", "Buttons", "Items", "InteractBlocks", "Levers", "Plates", "Doors", 
    	    "Trapdoors", "Fencegates", "Tripwires", "RepeatersComparators", "Bells", "Entities", "Frostwalker", "Teleportations", 
    	    "Damages", "Enter", "GuiTeleport", "Weather", "Fly", "Portals", "ItemsPickup", "ItemsDrop", "SpecialBlocks", "Elytra", "Windcharges"));
    	settings_name.put("visitors", permissions);

    	permissions = new LinkedHashSet<>();
    	permissions.addAll(List.of("Explosions","Liquids","Redstone","Firespread","Monsters","Pvp"));
    	settings_name.put("natural", permissions);
        
        // Admin
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
        keyToSlotMap.put("Elytra", 28);
        keyToSlotMap.put("Enter", 29);
        keyToSlotMap.put("ItemsPickup", 30);
        keyToSlotMap.put("ItemsDrop", 32);
        keyToSlotMap.put("SpecialBlocks", 33);
        keyToSlotMap.put("Windcharges", 34);
        
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
        keyToMaterialMap.put("Fly", Material.PRISMARINE_SHARD);
        keyToMaterialMap.put("Pvp", Material.DIAMOND_SWORD);
        keyToMaterialMap.put("Weather", Material.SNOWBALL);
        keyToMaterialMap.put("Portals", Material.END_PORTAL_FRAME);
        keyToMaterialMap.put("Enter", Material.BIRCH_DOOR);
        keyToMaterialMap.put("ItemsPickup", Material.HOPPER);
        keyToMaterialMap.put("ItemsDrop", Material.FEATHER);
        keyToMaterialMap.put("Elytra", Material.ELYTRA);
        keyToMaterialMap.put("SpecialBlocks", Material.SPAWNER);
        keyToMaterialMap.put("Windcharges", Material.WHITE_DYE);
        
    }
    

    // ********************
    // *  Others Methods  *
    // ********************
    
    
    public GuiSlot getGuiSlotByClickedSlot(String menu, int clickedSlot) {
        return gui_slots.get(menu).stream()
            .filter(slot -> slot.getSlot() == clickedSlot)
            .findFirst()
            .orElse(null);
    }
    
    public void loadGuiSettings(boolean check_itemsadder) {
    	guis_items_perms_clicked_slots.clear();
        guis_items_perms_clicked_slots.put("visitors", new HashMap<>());
    	guis_items_perms_clicked_slots.put("members", new HashMap<>());
        guis_items_perms_clicked_slots.put("natural", new HashMap<>());
        gui_slots.clear();
        gui_settings.clear();
        File guisDir = new File(instance.getDataFolder(), "guis");
        if (!guisDir.exists()) {
            guisDir.mkdirs();
        }
        FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase().endsWith(".yml");
        File[] guiFiles = guisDir.listFiles(ymlFilter);
        if (guiFiles != null) {
        	int id_settings = 0;
        	int id_slot = 0;
            for (File file : guiFiles) {
                YamlConfiguration config = YamlConfiguration.loadConfiguration((File)file);
                
                // Get GUI settings
                String gui_name = file.getName().replace(".yml", "");
                int rows = config.getInt("rows");
                String title = instance.getLanguage().getMessage(config.getString("gui-title"));
                int list_start_slot = config.getInt("list-start-slot");
                int list_end_slot = config.getInt("list-end-slot");
                GuiSettings guiSettings = new GuiSettings(id_settings++,rows,title,list_start_slot,list_end_slot);
                gui_settings.put(gui_name, guiSettings);
                
                // Get GUI items
                ConfigurationSection configSection = config.getConfigurationSection("items");
                for (String key : configSection.getKeys(false)) {
                	int slot = configSection.getInt(key + ".slot");
                	boolean custom_head = false;
                	String textures = "";
                	Material mat = null;
                	try {
                		String mat_string = configSection.getString(key + ".material");
                		if(mat_string.contains("PLAYER_HEAD:")) {
                			String[] parts = mat_string.split(":");
                			if(parts.length == 2) {
                    			mat_string = parts[0];
                    			textures = parts[1];
                    			custom_head = true;
                			}
                		}
                		mat = Material.valueOf(mat_string);
                    } catch (IllegalArgumentException e) {
                        instance.info(ChatColor.RED + "Invalid Material for '"+key+"', using STONE.");
                        mat = Material.STONE;
                    }
                	boolean custom_model_data = configSection.getBoolean(key + ".custom_model_data");
                	String custom_model_data_value = configSection.getString(key + ".custom_model_data_value");
                	String target_title = instance.getLanguage().getMessage(configSection.getString(key + ".target-title"));
                	String target_lore = instance.getLanguage().getMessage(configSection.getString(key + ".target-lore"));
                	String action = configSection.getString(key + ".action");
                	if(gui_name.equals("settings")) {
                        if (settings_name.get("visitors").contains(key)) {
                            guis_items_perms_clicked_slots.get("visitors").put(configSection.getInt(key + ".slot"), key);
                        }
                        if (settings_name.get("members").contains(key)) {
                            guis_items_perms_clicked_slots.get("members").put(configSection.getInt(key + ".slot"), key);
                        }
                        if (settings_name.get("natural").contains(key)) {
                            guis_items_perms_clicked_slots.get("natural").put(configSection.getInt(key + ".slot"), key);
                        }
                	}
                    GuiSlot guiSlot = new GuiSlot(id_slot++, key, slot, mat, custom_model_data, custom_model_data_value, target_title, target_lore, action, custom_head, textures);
                    gui_slots.computeIfAbsent(gui_name, k -> new ArrayList<>()).add(guiSlot);
                }
            }
        }
    }

    /**
     * Executes the action of the slot.
     * 
     * @param player The player.
     * @param slot The slot.
     * @param click The click.
     */
    public void executeAction(Player player, GuiSlot slot, ClickType click) {
        String action = slot.getAction();
        if (action == null || action.isBlank() || action.equalsIgnoreCase("none")) {
            return;
        }
        String[] parts = action.split(":");
        if (parts[0].equalsIgnoreCase("left") && click == ClickType.LEFT) {
            if (parts.length < 2) {
                return;
            }
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) {
                    return;
                }
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), (String)parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand((CommandSender) player, (String)parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) {
                    return;
                }
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
        if (parts[0].equalsIgnoreCase("right") && click == ClickType.RIGHT) {
            if (parts.length < 2) {
                return;
            }
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) {
                    return;
                }
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand((CommandSender) Bukkit.getConsoleSender(), (String) parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand((CommandSender) player, (String) parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) {
                    return;
                }
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
        if (parts[0].equalsIgnoreCase("shift_left") && click == ClickType.SHIFT_LEFT) {
            if (parts.length < 2) {
                return;
            }
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) {
                    return;
                }
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand((CommandSender)player, (String)parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) {
                    return;
                }
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
        if (parts[0].equalsIgnoreCase("shift_right") && click == ClickType.SHIFT_RIGHT) {
            if (parts.length < 2) {
                return;
            }
            if (parts[1].equalsIgnoreCase("close_inventory")) {
                player.closeInventory();
                return;
            }
            if (parts[1].equalsIgnoreCase("cmd")) {
                if (parts.length < 3) {
                    return;
                }
                if (parts[2].equalsIgnoreCase("console")) {
                    Bukkit.dispatchCommand((CommandSender)Bukkit.getConsoleSender(), (String)parts[3]);
                    return;
                }
                if (parts[2].equalsIgnoreCase("player")) {
                    Bukkit.dispatchCommand((CommandSender)player, (String)parts[3]);
                    return;
                }
                return;
            }
            if (parts[1].equalsIgnoreCase("msg")) {
                if (parts.length < 3) {
                    return;
                }
                player.sendMessage(parts[2]);
                return;
            }
            return;
        }
    }
    
    /**
     * Splits the lore into lines.
     *
     * @param lore The lore to split.
     * @return The list of lore lines.
     */
    public List<String> getLore(String lore) {
        List<String> lores = new ArrayList<>();
        if(lore.isBlank()) return lores;
        String[] parts = lore.split("\n");
        for (String s : parts) {
            lores.add(s);
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
        ItemStack item = new ItemStack(material == null ? Material.STONE : material, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if(name != null) meta.setDisplayName(name);
            if(lore != null) meta.setLore(lore);
            meta = instance.getGuis().setItemFlag(meta);
            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", 0, AttributeModifier.Operation.ADD_NUMBER);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Checks if a given name is a permission setting.
     *
     * @param name The name to check.
     * @param role The role to check for.
     * @return true if the name is a permission setting, false otherwise.
     */
    public boolean isAPerm(String name, String role) {
        return settings_name.get(role).contains(name);
    }
    
    /**
     * Checks if a given name is a permission setting.
     * 
     * @param name The name to check.
     * @return True if the name is a permission setting, false otherwise.
     */
    public boolean isAPerm(String name) {
    	return settings_name.get("visitors").contains(name) || settings_name.get("members").contains(name)
    			|| settings_name.get("natural").contains(name);
    }

    /**
     * Gets the set of permission settings names.
     *
     * @param role The role to get permissions for.
     * @return The set of permission settings names.
     */
    public LinkedHashSet<String> getPerms(String role) {
        return settings_name.get(role);
    }

    /**
     * Gets the name of the setting associated with a specific slot.
     *
     * @param slot The slot number.
     * @param role The target role.
     * @return The name of the setting, or an empty string if not found.
     */
    public String getSlotPerm(int slot, String role) {
        return guis_items_perms_clicked_slots.getOrDefault(role, new HashMap<>()).getOrDefault(slot, "");
    }

    /**
     * Checks if a slot is a clickable slot.
     *
     * @param clickedSlot The slot number.
     * @param role The role to check for.
     * @return true if the slot is clickable, false otherwise.
     */
    public boolean isAllowedSlot(int clickedSlot, String role) {
        return guis_items_perms_clicked_slots.getOrDefault(role, new HashMap<>()).containsKey(clickedSlot);
    }
    
    /**
     * Checks if a slot is a clickable slot.
     *
     * @param clickedSlot The slot number.
     * @param role The role to check for.
     * @return true if the slot is clickable, false otherwise.
     */
    public boolean isAdminAllowedSlot(int clickedSlot, String role) {
        return guis_items_perms_clicked_slots_admin.getOrDefault(role, new HashMap<>()).containsKey(clickedSlot);
    }
    
    /**
     * Gets the name of the setting associated with a specific slot.
     *
     * @param slot The slot number.
     * @param role The target role.
     * @return The name of the setting, or an empty string if not found.
     */
    public String getAdminSlotPerm(int slot, String role) {
        return guis_items_perms_clicked_slots_admin.getOrDefault(role, new HashMap<>()).getOrDefault(slot, "");
    }

    /**
     * Sets item flags for an item meta.
     *
     * @param meta The ItemMeta to set flags on.
     * @return The ItemMeta with the flags set.
     */
    public ItemMeta setItemFlag(ItemMeta meta) {
    	String version = Bukkit.getVersion();
        if (!version.contains("1.21")) {
            meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
        } else if ((version.contains("1.20") || version.contains("1.21")) && !version.contains("Spigot")) {
        	meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
        }
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_DYE, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_UNBREAKABLE);
        return meta;
    }

}
