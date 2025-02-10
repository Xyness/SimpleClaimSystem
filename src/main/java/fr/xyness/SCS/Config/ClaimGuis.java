package fr.xyness.SCS.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.xyness.SCS.SimpleClaimSystem;

/**
 * Manages GUI settings and operations for the claim system.
 */
public class ClaimGuis {

	
    // ***************
    // *  Variables  *
    // ***************

    
    /** Stores permissions for clicked slots in GUIs */
    private static Map<String,Map<Integer, String>> guis_items_perms_clicked_slots = new HashMap<>();
    
    static {
    	guis_items_perms_clicked_slots.put("visitors", new HashMap<>());
        guis_items_perms_clicked_slots.get("visitors").put(1, "Build");
        guis_items_perms_clicked_slots.get("visitors").put(2, "Destroy");
        guis_items_perms_clicked_slots.get("visitors").put(3, "Buttons");
        guis_items_perms_clicked_slots.get("visitors").put(4, "Items");
        guis_items_perms_clicked_slots.get("visitors").put(5, "InteractBlocks");
        guis_items_perms_clicked_slots.get("visitors").put(6, "Levers");
        guis_items_perms_clicked_slots.get("visitors").put(7, "Plates");
        guis_items_perms_clicked_slots.get("visitors").put(10, "Doors");
        guis_items_perms_clicked_slots.get("visitors").put(11, "Trapdoors");
        guis_items_perms_clicked_slots.get("visitors").put(12, "Fencegates");
        guis_items_perms_clicked_slots.get("visitors").put(13, "Tripwires");
        guis_items_perms_clicked_slots.get("visitors").put(14, "RepeatersComparators");
        guis_items_perms_clicked_slots.get("visitors").put(15, "Bells");
        guis_items_perms_clicked_slots.get("visitors").put(16, "Entities");
        guis_items_perms_clicked_slots.get("visitors").put(19, "Teleportations");
        guis_items_perms_clicked_slots.get("visitors").put(20, "Damages");
        guis_items_perms_clicked_slots.get("visitors").put(21, "Fly");
        guis_items_perms_clicked_slots.get("visitors").put(22, "Frostwalker");
        guis_items_perms_clicked_slots.get("visitors").put(23, "Weather");
        guis_items_perms_clicked_slots.get("visitors").put(24, "GuiTeleport");
        guis_items_perms_clicked_slots.get("visitors").put(25, "Portals");
        guis_items_perms_clicked_slots.get("visitors").put(28, "Elytra");
        guis_items_perms_clicked_slots.get("visitors").put(29, "Enter");
        guis_items_perms_clicked_slots.get("visitors").put(30, "ItemsPickup");
        guis_items_perms_clicked_slots.get("visitors").put(32, "ItemsDrop");
        guis_items_perms_clicked_slots.get("visitors").put(33, "SpecialBlocks");
        guis_items_perms_clicked_slots.get("visitors").put(34, "Windcharges");
        
        
    	guis_items_perms_clicked_slots.put("members", new HashMap<>());
    	guis_items_perms_clicked_slots.get("members").put(1, "Build");
    	guis_items_perms_clicked_slots.get("members").put(2, "Destroy");
    	guis_items_perms_clicked_slots.get("members").put(3, "Buttons");
    	guis_items_perms_clicked_slots.get("members").put(4, "Items");
    	guis_items_perms_clicked_slots.get("members").put(5, "InteractBlocks");
    	guis_items_perms_clicked_slots.get("members").put(6, "Levers");
    	guis_items_perms_clicked_slots.get("members").put(7, "Plates");
    	guis_items_perms_clicked_slots.get("members").put(10, "Doors");
    	guis_items_perms_clicked_slots.get("members").put(11, "Trapdoors");
    	guis_items_perms_clicked_slots.get("members").put(12, "Fencegates");
    	guis_items_perms_clicked_slots.get("members").put(13, "Tripwires");
    	guis_items_perms_clicked_slots.get("members").put(14, "RepeatersComparators");
    	guis_items_perms_clicked_slots.get("members").put(15, "Bells");
    	guis_items_perms_clicked_slots.get("members").put(16, "Entities");
    	guis_items_perms_clicked_slots.get("members").put(19, "Teleportations");
    	guis_items_perms_clicked_slots.get("members").put(20, "Damages");
    	guis_items_perms_clicked_slots.get("members").put(21, "Fly");
    	guis_items_perms_clicked_slots.get("members").put(22, "Frostwalker");
    	guis_items_perms_clicked_slots.get("members").put(23, "Weather");
    	guis_items_perms_clicked_slots.get("members").put(24, "GuiTeleport");
    	guis_items_perms_clicked_slots.get("members").put(25, "Portals");
    	guis_items_perms_clicked_slots.get("members").put(28, "Elytra");
    	guis_items_perms_clicked_slots.get("members").put(29, "Enter");
    	guis_items_perms_clicked_slots.get("members").put(30, "ItemsPickup");
    	guis_items_perms_clicked_slots.get("members").put(32, "ItemsDrop");
    	guis_items_perms_clicked_slots.get("members").put(33, "SpecialBlocks");
    	guis_items_perms_clicked_slots.get("members").put(34, "Windcharges");

        
        guis_items_perms_clicked_slots.put("natural", new HashMap<>());
        guis_items_perms_clicked_slots.get("natural").put(10, "Explosions");
        guis_items_perms_clicked_slots.get("natural").put(11, "Liquids");
        guis_items_perms_clicked_slots.get("natural").put(12, "Redstone");
        guis_items_perms_clicked_slots.get("natural").put(14, "Firespread");
        guis_items_perms_clicked_slots.get("natural").put(15, "Monsters");
        guis_items_perms_clicked_slots.get("natural").put(16, "Pvp");
    }
    
    /**
     * A map containing the key-to-slot mappings.
     */
    public static final Map<String, Integer> keyToSlotMap = new HashMap<>();

    /**
     * A map containing the key-to-material mappings.
     */
    public static final Map<String, Material> keyToMaterialMap = new HashMap<>();
    
    /** Set of settings names */
    private Map<String,Set<String>> settings_name = new HashMap<>();
    
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
    	
    	// Set settings_name
    	settings_name.put("members", Set.of("Build", "Destroy", "Buttons", "Items", "InteractBlocks", "Levers", "Plates", "Doors", "Trapdoors",
            "Fencegates","Tripwires","RepeatersComparators","Bells","Entities","Frostwalker","Teleportations","Damages","Enter", "GuiTeleport",
            "Weather","Fly", "Portals", "ItemsPickup", "ItemsDrop", "SpecialBlocks", "Elytra", "Windcharges"));
    	
    	settings_name.put("visitors", Set.of("Build", "Destroy", "Buttons", "Items", "InteractBlocks", "Levers", "Plates", "Doors", "Trapdoors",
                "Fencegates","Tripwires","RepeatersComparators","Bells","Entities","Frostwalker","Teleportations","Damages","Enter", "GuiTeleport",
                "Weather","Fly", "Portals", "ItemsPickup", "ItemsDrop", "SpecialBlocks", "Elytra", "Windcharges"));
    	
    	settings_name.put("natural", Set.of("Explosions","Liquids","Redstone","Firespread","Monsters","Pvp"));
    
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
            meta.setDisplayName(name);
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
     * Gets the set of permission settings names.
     *
     * @param role The role to get permissions for.
     * @return The set of permission settings names.
     */
    public Set<String> getPerms(String role) {
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
