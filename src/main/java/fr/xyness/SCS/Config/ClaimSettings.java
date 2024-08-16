package fr.xyness.SCS.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * ClaimSettings class handles various settings and configurations for the plugin.
 */
public class ClaimSettings {

	
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
    
    /** General settings map. */
    private Map<String, String> settings = new HashMap<>();
    
    /** Groups and their corresponding values. */
    private LinkedHashMap<String, String> groups = new LinkedHashMap<>();
    
    /** Group settings map. */
    private Map<String, Map<String, Double>> groupsSettings = new HashMap<>();
    
    /** Set of disabled worlds. */
    private Set<String> disabledWorlds = new HashSet<>();

    
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
        defaultValues.clear();
        enabledSettings.clear();
        settings.clear();
        groups.clear();
        groupsSettings.clear();
        disabledWorlds.clear();
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
     * Gets the set of disabled worlds.
     *
     * @return The set of disabled worlds.
     */
    public Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }
    
    /**
     * Adds a disabled world
     * 
     * @param world The world to add
     */
    public void addDisabledWorld(String world) {
    	disabledWorlds.add(world);
    }

    /**
     * Sets the disabled worlds.
     *
     * @param w The set of worlds to disable.
     */
    public void setDisabledWorlds(Set<String> w) {
        disabledWorlds = w;
    }

    /**
     * Checks if a world is disabled.
     *
     * @param w The world to check.
     * @return true if the world is disabled, false otherwise.
     */
    public boolean isWorldDisabled(String w) {
        return disabledWorlds.contains(w);
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
     * Sets the restricted items.
     *
     * @param mat The list of material names to restrict.
     * @return The number of restricted items added.
     */
    public int setRestrictedItems(List<String> mat) {
        return (int) mat.stream()
                        .map(Material::matchMaterial)
                        .filter(Objects::nonNull)
                        .peek(restrictedItems::add)
                        .count();
    }

    /**
     * Sets the restricted containers.
     *
     * @param mat The list of material names to restrict as containers.
     * @return The number of restricted containers added.
     */
    public int setRestrictedContainers(List<String> mat) {
        return (int) mat.stream()
                        .map(Material::matchMaterial)
                        .filter(Objects::nonNull)
                        .peek(restrictedInteractBlocks::add)
                        .count();
    }

    /**
     * Sets the restricted entity types.
     *
     * @param mat The list of entity type names to restrict.
     * @return The number of restricted entity types added.
     */
    public int setRestrictedEntityType(List<String> mat) {
        return (int) mat.stream()
                        .map(EntityType::fromName)
                        .filter(Objects::nonNull)
                        .peek(restrictedEntityType::add)
                        .count();
    }
    
    /**
     * Sets the special blocks.
     *
     * @param mat The list of material names to restrict.
     * @return The number of restricted blocks added.
     */
    public int setSpecialBlocks(List<String> mat) {
        return (int) mat.stream()
                        .map(Material::matchMaterial)
                        .filter(Objects::nonNull)
                        .peek(specialBlocks::add)
                        .count();
    }

    /**
     * Gets the status of enabled/disabled settings.
     *
     * @return The map of enabled/disabled settings.
     */
    public Map<String, Boolean> getStatusSettings() {
        return enabledSettings;
    }
}
