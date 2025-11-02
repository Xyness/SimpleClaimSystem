package fr.xyness.SCS.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import fr.xyness.SCS.SimpleClaimSystem;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import fr.xyness.SCS.Types.WorldMode;

/**
 * ClaimSettings class handles various settings and configurations for the plugin.
 */
public class ClaimSettings {

    private final SimpleClaimSystem instance;
    public ClaimSettings(SimpleClaimSystem instance) {
        this.instance = instance;
    }
	
    // ***************
    // *  Variables  *
    // ***************

	
    /** List of restricted items. */
    private List<Material> restrictedItems = new ArrayList<>();
    
    /** List of restricted interactable blocks. */
    private List<Material> restrictedInteractBlocks = new ArrayList<>();
    
    /** List of restricted entity types. */
    private List<EntityType> restrictedEntityType = new ArrayList<>();
    
    /** List of special blocks. */
    private List<Material> specialBlocks = new ArrayList<>();
    
    /** List of ignored break blocks. */
    private List<Material> BreakBlocksIgnore = new ArrayList<>();
    
    /** List of ignored place blocks. */
    private List<Material> PlaceBlocksIgnore = new ArrayList<>();
    
    /** Map of aliases, key for aliase, value for real command */
    private Map<String,String> aliases = new HashMap<>();
    
    /** Default values for settings. */
    private Map<String,LinkedHashMap<String, Boolean>> defaultValues = new HashMap<>();
    
    /** Code for default values for natural. */
    private String defaultValuesCode_Natural;
    
    /** Code for default values for visitors. */
    private String defaultValuesCode_Visitors;
    
    /** Code for default values for members. */
    private String defaultValuesCode_Members;
    
    /** Enabled settings map. */
    private Map<String, Boolean> enabledSettings = new HashMap<>();
    
    /** SurvivalRequiringClaims settings map. */
    private Map<String, Boolean> SurvivalRequiringClaimsSettings = new HashMap<>();
    
    /** General settings map. */
    private Map<String, String> settings = new HashMap<>();
    
    /** Groups and their corresponding values. */
    private LinkedHashMap<String, String> groups = new LinkedHashMap<>();
    
    /** Group settings map. */
    private Map<String, Map<String, Double>> groupsSettings = new HashMap<>();
    
    /** Map of mode of worlds. */
    private Map<String,WorldMode> worlds = new HashMap<>();
    
    /** Map of worlds aliases. */
    private Map<String,String> worldsAliases = new HashMap<>();
    
    /** Location of the expulsion */
    private Location expulsionLocation;

    /** Regex for the description of player claims. */
    private Pattern descriptionRegexClaims;

    /** Same, but for the protected areas. */
    private Pattern descriptionRegexProtected;

    /** Default description pattern. Used if the user regex is not valid. */
    private static final Pattern DEFAULT_DESCRIPTION_PATTERN = Pattern.compile("^[a-zA-Z0-9\\s]+$");

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Clears all settings and configurations.
     */
    public void clearAll() {
        restrictedItems.clear();
        restrictedInteractBlocks.clear();
        restrictedEntityType.clear();
        specialBlocks.clear();
        BreakBlocksIgnore.clear();
        PlaceBlocksIgnore.clear();
        defaultValues.clear();
        enabledSettings.clear();
        settings.clear();
        groups.clear();
        groupsSettings.clear();
        worlds.clear();
        aliases.clear();
        descriptionRegexClaims = null;
        descriptionRegexProtected = null;
        worldsAliases.clear();
    }
    
    /**
     * Gets the world aliase.
     * 
     * @param world The world name.
     * @return The world aliase.
     */
    public String getWorldAliase(String world) {
    	return worldsAliases.getOrDefault(world, world);
    }
    
    /**
     * Sets the worlds aliases.
     * 
     * @param worldsAliases
     */
    public void setWorldAliases(Map<String,String> worldsAliases) {
    	this.worldsAliases = worldsAliases;
    }

    /**
     * Gets the default values code.
     *
     * @param role The role to get default values for
     * @return The default values code.
     */
    public String getDefaultValuesCode(String role) {
        switch(role) {
	        case "natural":
	        	return defaultValuesCode_Natural;
	        case "visitors":
	        	return defaultValuesCode_Visitors;
	        case "members":
	        	return defaultValuesCode_Members;
	        case "all":
	        	return "natural:"+defaultValuesCode_Natural+";visitors:"+defaultValuesCode_Visitors+";members:"+defaultValuesCode_Members;
	    	default:
	    		return "";
        }
    }

    /**
     * Sets the default values code.
     *
     * @param s The default values code to set.
     * @param role The role to set default values for
     */
    public void setDefaultValuesCode(String s, String role) {
        switch(role) {
	        case "natural":
	        	this.defaultValuesCode_Natural = s;
	        case "visitors":
	        	this.defaultValuesCode_Visitors = s;
	        case "members":
	        	this.defaultValuesCode_Members = s;
	    	default:
	    		return;
        }
    }
    
    /**
     * Add a new aliase.
     * 
     * @param key The aliase.
     * @param value The real command.
     */
    public void addAliase(String key, String value) {
    	aliases.put(key, value);
    }
    
    /**
     * Gets an aliase.
     * 
     * @param key The aliase to check.
     * @return The aliase.
     */
    public String getAliase(String key) {
    	return aliases.get(key);
    }
    
    /**
     * Gets the aliases of a command.
     * 
     * @param value The real command.
     * @return The aliases.
     */
    public Set<String> getAliasesFromCommand(String value){
    	return aliases.values().stream()
    			.filter(s -> s.equals(value))
    			.collect(Collectors.toSet());
    }

    /**
     * Checks if the material is a restricted container.
     *
     * @param item The material to check.
     * @return true if the material is a restricted container, false otherwise.
     */
    public boolean isRestrictedContainer(Material item) {
        return restrictedInteractBlocks.contains(item);
    }

    /**
     * Checks if the entity type is restricted.
     *
     * @param e The entity type to check.
     * @return true if the entity type is restricted, false otherwise.
     */
    public boolean isRestrictedEntityType(EntityType e) {
        return restrictedEntityType.contains(e);
    }

    /**
     * Checks if the material is a restricted item.
     *
     * @param item The material to check.
     * @return true if the material is a restricted item, false otherwise.
     */
    public boolean isRestrictedItem(Material item) {
        return restrictedItems.contains(item);
    }
    
    /**
     * Checks if the material is a ignored break block.
     *
     * @param item The material to check.
     * @return true if the material is a ignored break block, false otherwise.
     */
    public boolean isBreakBlockIgnore(Material item) {
        return BreakBlocksIgnore.contains(item);
    }
    
    /**
     * Checks if the material is a ignored place block.
     *
     * @param item The material to check.
     * @return true if the material is a ignored place block, false otherwise.
     */
    public boolean isPlaceBlockIgnore(Material item) {
        return PlaceBlocksIgnore.contains(item);
    }
    
    /**
     * Checks if the material is a special block.
     *
     * @param item The material to check.
     * @return true if the material is a special block, false otherwise.
     */
    public boolean isSpecialBlock(Material item) {
        return specialBlocks.contains(item);
    }

    /**
     * Sets the disabled worlds.
     *
     * @param w The set of worlds to disable.
     */
    public void setWorlds(Map<String,WorldMode> w) {
        worlds = w;
    }
    
    /**
     * Gets a WorldMode.
     * 
     * @param world The world.
     * @return The world mode.
     */
    public WorldMode getWorldMode(String world) {
    	WorldMode mode = worlds.get(world);
    	return mode == null ? WorldMode.SURVIVAL : mode;
    }
    
    /**
     * Set the expulsion location.
     * 
     * @param expulsionLocation The expulsion location.
     */
    public void setExpulsionLocation(Location expulsionLocation) {
    	this.expulsionLocation = expulsionLocation;
    }
    
    /**
     * Gets the expulsion location.
     * 
     * @return The expulsion location.
     */
    public Location getExpulsionLocation() {
    	return expulsionLocation;
    }

    /**
     * Sets the groups from the configuration.
     *
     * @param g The groups to set.
     */
    public void setGroups(LinkedHashMap<String, String> g) {
        groups = g;
    }

    /**
     * Sets the groups' settings from the configuration.
     *
     * @param g The groups' settings to set.
     */
    public void setGroupsSettings(Map<String, Map<String, Double>> g) {
        groupsSettings = g;
    }

    /**
     * Gets the groups' settings.
     *
     * @return The groups' settings.
     */
    public Map<String, Map<String, Double>> getGroupsSettings() {
        return groupsSettings;
    }

    /**
     * Gets the groups.
     *
     * @return The set of groups.
     */
    public Set<String> getGroups() {
        return groupsSettings.keySet();
    }

    /**
     * Gets the groups' values.
     *
     * @return The groups' values.
     */
    public LinkedHashMap<String, String> getGroupsValues() {
        return groups;
    }

    /**
     * Gets the default values for permissions (claim settings).
     *
     * @return The default values map.
     */
    public Map<String,LinkedHashMap<String, Boolean>> getDefaultValues() {
        return defaultValues;
    }

    /**
     * Checks if a setting is enabled.
     *
     * @param p The setting to check.
     * @return true if the setting is enabled, false otherwise.
     */
    public boolean isEnabled(String p) {
        return enabledSettings.get(p);
    }

    /**
     * Adds a new setting.
     *
     * @param s The setting key.
     * @param p The setting value.
     */
    public void addSetting(String s, String p) {
        settings.put(s, p);
    }

    /**
     * Gets a boolean value from the setting name.
     *
     * @param s The setting name.
     * @return The boolean value of the setting.
     */
    public boolean getBooleanSetting(String s) {
    	return Boolean.parseBoolean(settings.get(s));
    }

    /**
     * Gets the value of a setting.
     *
     * @param s The setting name.
     * @return The value of the setting.
     */
    public String getSetting(String s) {
    	return settings.containsKey(s) ? settings.get(s) : "";
    }

    /**
     * Gets the list of restricted items.
     *
     * @return The list of restricted items.
     */
    public List<Material> getRestrictedItems() {
        return restrictedItems;
    }

    /**
     * Gets the list of restricted containers.
     *
     * @return The list of restricted containers.
     */
    public List<Material> getRestrictedContainers() {
        return restrictedInteractBlocks;
    }

    /**
     * Gets the list of restricted entity types.
     *
     * @return The list of restricted entity types.
     */
    public List<EntityType> getRestrictedEntityType() {
        return restrictedEntityType;
    }
    
    /**
     * Gets the list of special blocks.
     *
     * @return The list of special blocks.
     */
    public List<Material> getSpecialBlocks() {
        return specialBlocks;
    }
    
    /**
     * Gets the list of ignored break blocks.
     *
     * @return The list of ignored break blocks.
     */
    public List<Material> getBreakBlocksIgnore() {
        return BreakBlocksIgnore;
    }
    
    /**
     * Gets the list of ignored place blocks.
     *
     * @return The list of ignored place blocks.
     */
    public List<Material> getPlaceBlocksIgnore() {
        return PlaceBlocksIgnore;
    }

    /**
     * Sets the default values.
     *
     * @param v The default values to set.
     */
    public void setDefaultValues(Map<String,LinkedHashMap<String, Boolean>> v) {
        defaultValues = v;
    }

    /**
     * Sets the enabled settings.
     *
     * @param v The enabled settings to set.
     */
    public void setEnabledSettings(Map<String, Boolean> v) {
        enabledSettings = v;
    }
    
    /**
     * Sets the settings for SurvivalRequiringClaims mode.
     * 
     * @param v The settings.
     */
    public void setSurvivalRequiringClaimsSettings(Map<String, Boolean> v) {
    	SurvivalRequiringClaimsSettings = v;
    }
    
    /**
     * Gets a SurvivalRequiringClaims setting.
     * 
     * @param setting The setting to get.
     * @return True if enabled, false otherwise.
     */
    public boolean getSettingSRC(String setting) {
    	return SurvivalRequiringClaimsSettings.get(setting);
    }

    /**
     * Sets the restricted items.
     *
     * @param mat The list of material names to restrict.
     */
    public void setRestrictedItems(List<String> mat) {
        restrictedItems.clear();
        mat.stream()
            .map(Material::matchMaterial)
            .filter(Objects::nonNull)
            .forEach(restrictedItems::add); // Utilisation de forEach ici
    }

    /**
     * Sets the restricted containers.
     *
     * @param mat The list of material names to restrict as containers.
     */
    public void setRestrictedContainers(List<String> mat) {
        restrictedInteractBlocks.clear();
        mat.stream()
            .map(Material::matchMaterial)
            .filter(Objects::nonNull)
            .forEach(restrictedInteractBlocks::add); // Utilisation de forEach ici
    }

    /**
     * Sets the restricted entity types.
     *
     * @param mat The list of entity type names to restrict.
     */
    public void setRestrictedEntityType(List<String> mat) {
        restrictedEntityType.clear();
        mat.stream()
            .map(EntityType::fromName)
            .filter(Objects::nonNull)
            .forEach(restrictedEntityType::add); // Utilisation de forEach ici
    }

    /**
     * Sets the special blocks.
     *
     * @param mat The list of material names to restrict.
     */
    public void setSpecialBlocks(List<String> mat) {
        specialBlocks.clear();
        mat.stream()
            .map(Material::matchMaterial)
            .filter(Objects::nonNull)
            .forEach(specialBlocks::add); // Utilisation de forEach ici
    }

    /**
     * Sets the ignored break blocks.
     *
     * @param mat The list of ignored break blocks.
     */
    public void setBreakBlocksIgnore(List<String> mat) {
        BreakBlocksIgnore.clear();
        mat.stream()
            .map(Material::matchMaterial)
            .filter(Objects::nonNull)
            .forEach(BreakBlocksIgnore::add); // Utilisation de forEach ici
    }

    /**
     * Sets the ignored place blocks.
     *
     * @param mat The list of ignored place blocks.
     */
    public void setPlaceBlocksIgnore(List<String> mat) {
        PlaceBlocksIgnore.clear();
        mat.stream()
            .map(Material::matchMaterial)
            .filter(Objects::nonNull)
            .forEach(PlaceBlocksIgnore::add); // Utilisation de forEach ici
    }

    /**
     * Gets the status of enabled/disabled settings.
     *
     * @return The map of enabled/disabled settings.
     */
    public Map<String, Boolean> getStatusSettings() {
        return enabledSettings;
    }

    /**
     * Get the pattern for the description of claims.
     * @return a non-null instance of a pattern. If the provided one is not valid, will use a default, safe one.
     */
    public Pattern getDescriptionPatternClaims() {
        if(descriptionRegexClaims == null) {
            descriptionRegexClaims = computeOrDefault("description-regex.claims");
        }
        return descriptionRegexClaims;
    }

    /**
     * Get the pattern for the description of protected-areas.
     * @return a non-null instance of a pattern. If the provided one is not valid, will use a default, safe one.
     */
    public Pattern getDescriptionPatternProtected() {
        if(descriptionRegexProtected == null) {
            descriptionRegexProtected = computeOrDefault("description-regex.protected-areas");
        }
        return descriptionRegexProtected;
    }

    private Pattern computeOrDefault(String key) {
        String regex = getSetting(key);
        try {
            return Pattern.compile(regex);
        } catch(PatternSyntaxException e) {
            instance.info(ChatColor.RED + "[ERROR] The property "+key+" (\""+regex+"\") is not valid: " + e.getMessage());
            return DEFAULT_DESCRIPTION_PATTERN;
        }
    }
}
