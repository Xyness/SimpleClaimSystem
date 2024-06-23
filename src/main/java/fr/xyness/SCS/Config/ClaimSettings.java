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
    private static List<Material> restrictedItems = new ArrayList<>();
    
    /** List of restricted interactable blocks. */
    private static List<Material> restrictedInteractBlocks = new ArrayList<>();
    
    /** List of restricted entity types. */
    private static List<EntityType> restrictedEntityType = new ArrayList<>();
    
    /** Default values for settings. */
    private static LinkedHashMap<String, Boolean> defaultValues = new LinkedHashMap<>();
    
    /** Code for default values. */
    private static String defaultValuesCode;
    
    /** Enabled settings map. */
    private static Map<String, Boolean> enabledSettings = new HashMap<>();
    
    /** General settings map. */
    private static Map<String, String> settings = new HashMap<>();
    
    /** Groups and their corresponding values. */
    private static LinkedHashMap<String, String> groups = new LinkedHashMap<>();
    
    /** Group settings map. */
    private static Map<String, Map<String, Double>> groupsSettings = new HashMap<>();
    
    /** Set of disabled worlds. */
    private static Set<String> disabledWorlds = new HashSet<>();

    // **********************
    // *  SETTINGS Methods  *
    // **********************

    /**
     * Clears all settings and configurations.
     */
    public static void clearAll() {
        restrictedItems.clear();
        restrictedInteractBlocks.clear();
        restrictedEntityType.clear();
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
     * @return The default values code.
     */
    public static String getDefaultValuesCode() {
        return defaultValuesCode;
    }

    /**
     * Sets the default values code.
     *
     * @param s The default values code to set.
     */
    public static void setDefaultValuesCode(String s) {
        defaultValuesCode = s;
    }

    /**
     * Checks if the material is a restricted container.
     *
     * @param item The material to check.
     * @return true if the material is a restricted container, false otherwise.
     */
    public static boolean isRestrictedContainer(Material item) {
        return restrictedInteractBlocks.contains(item);
    }

    /**
     * Checks if the entity type is restricted.
     *
     * @param e The entity type to check.
     * @return true if the entity type is restricted, false otherwise.
     */
    public static boolean isRestrictedEntityType(EntityType e) {
        return restrictedEntityType.contains(e);
    }

    /**
     * Checks if the material is a restricted item.
     *
     * @param item The material to check.
     * @return true if the material is a restricted item, false otherwise.
     */
    public static boolean isRestrictedItem(Material item) {
        return restrictedItems.contains(item);
    }

    /**
     * Gets the set of disabled worlds.
     *
     * @return The set of disabled worlds.
     */
    public static Set<String> getDisabledWorlds() {
        return disabledWorlds;
    }

    /**
     * Gets the set of restricted containers as strings.
     *
     * @return The set of restricted containers as strings.
     */
    public static Set<String> getRestrictedContainersString() {
        return restrictedInteractBlocks.stream()
                                       .map(Material::toString)
                                       .collect(Collectors.toSet());
    }

    /**
     * Gets the set of restricted entities as strings.
     *
     * @return The set of restricted entities as strings.
     */
    public static Set<String> getRestrictedEntitiesString() {
        return restrictedEntityType.stream()
                                   .map(EntityType::toString)
                                   .collect(Collectors.toSet());
    }

    /**
     * Gets the set of restricted items as strings.
     *
     * @return The set of restricted items as strings.
     */
    public static Set<String> getRestrictedItemsString() {
        return restrictedItems.stream()
                              .map(Material::toString)
                              .collect(Collectors.toSet());
    }

    /**
     * Sets the disabled worlds.
     *
     * @param w The set of worlds to disable.
     */
    public static void setDisabledWorlds(Set<String> w) {
        disabledWorlds = w;
    }

    /**
     * Checks if a world is disabled.
     *
     * @param w The world to check.
     * @return true if the world is disabled, false otherwise.
     */
    public static boolean isWorldDisabled(String w) {
        return disabledWorlds.contains(w);
    }

    /**
     * Sets the groups from the configuration.
     *
     * @param g The groups to set.
     */
    public static void setGroups(LinkedHashMap<String, String> g) {
        groups = g;
    }

    /**
     * Sets the groups' settings from the configuration.
     *
     * @param g The groups' settings to set.
     */
    public static void setGroupsSettings(Map<String, Map<String, Double>> g) {
        groupsSettings = g;
    }

    /**
     * Gets the groups' settings.
     *
     * @return The groups' settings.
     */
    public static Map<String, Map<String, Double>> getGroupsSettings() {
        return groupsSettings;
    }

    /**
     * Gets the groups.
     *
     * @return The set of groups.
     */
    public static Set<String> getGroups() {
        return groupsSettings.keySet();
    }

    /**
     * Gets the groups' values.
     *
     * @return The groups' values.
     */
    public static LinkedHashMap<String, String> getGroupsValues() {
        return groups;
    }

    /**
     * Gets the default values for permissions (claim settings).
     *
     * @return The default values map.
     */
    public static Map<String, Boolean> getDefaultValues() {
        return defaultValues;
    }

    /**
     * Checks if a setting is enabled.
     *
     * @param p The setting to check.
     * @return true if the setting is enabled, false otherwise.
     */
    public static boolean isEnabled(String p) {
        return enabledSettings.get(p);
    }

    /**
     * Adds a new setting.
     *
     * @param s The setting key.
     * @param p The setting value.
     */
    public static void addSetting(String s, String p) {
        settings.put(s, p);
    }

    /**
     * Gets a boolean value from the setting name.
     *
     * @param s The setting name.
     * @return The boolean value of the setting.
     */
    public static boolean getBooleanSetting(String s) {
    	return Boolean.parseBoolean(settings.get(s));
    }

    /**
     * Gets the value of a setting.
     *
     * @param s The setting name.
     * @return The value of the setting.
     */
    public static String getSetting(String s) {
    	return settings.containsKey(s) ? settings.get(s) : "";
    }

    /**
     * Gets the list of restricted items.
     *
     * @return The list of restricted items.
     */
    public static List<Material> getRestrictedItems() {
        return restrictedItems;
    }

    /**
     * Gets the list of restricted containers.
     *
     * @return The list of restricted containers.
     */
    public static List<Material> getRestrictedContainers() {
        return restrictedInteractBlocks;
    }

    /**
     * Gets the list of restricted entity types.
     *
     * @return The list of restricted entity types.
     */
    public static List<EntityType> getRestrictedEntityType() {
        return restrictedEntityType;
    }

    /**
     * Sets the default values.
     *
     * @param v The default values to set.
     */
    public static void setDefaultValues(LinkedHashMap<String, Boolean> v) {
        defaultValues = v;
    }

    /**
     * Sets the enabled settings.
     *
     * @param v The enabled settings to set.
     */
    public static void setEnabledSettings(Map<String, Boolean> v) {
        enabledSettings = v;
    }

    /**
     * Sets the restricted items.
     *
     * @param mat The list of material names to restrict.
     * @return The number of restricted items added.
     */
    public static int setRestrictedItems(List<String> mat) {
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
    public static int setRestrictedContainers(List<String> mat) {
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
    public static int setRestrictedEntityType(List<String> mat) {
        return (int) mat.stream()
                        .map(EntityType::fromName)
                        .filter(Objects::nonNull)
                        .peek(restrictedEntityType::add)
                        .count();
    }

    /**
     * Gets the status of enabled/disabled settings.
     *
     * @return The map of enabled/disabled settings.
     */
    public static Map<String, Boolean> getStatusSettings() {
        return enabledSettings;
    }
}
