package fr.xyness.SCS.Config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Material;

public class ClaimSettings {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	// Settings of config.yml
    private static List<Material> restrictedItems = new ArrayList<>();
    private static List<Material> restrictedContainers = new ArrayList<>();
    private static LinkedHashMap<String,Boolean> defaultValues = new LinkedHashMap<>();
    private static String defaultValuesCode;
    private static Map<String,Boolean> enabledSettings = new HashMap<>();
    private static Map<String,String> settings = new HashMap<>();
    private static LinkedHashMap<String,String> groups = new LinkedHashMap<>();
    private static Map<String,Map<String,Double>> groupsSettings = new HashMap<>();
    private static Set<String> disabledWorlds = new HashSet<>();
    
    
    // **********************
 	// *  SETTINGS Methods  *
 	// **********************
     
     
    // Get the default values code
    public static String getDefaultValuesCode() {
    	return defaultValuesCode;
    }
    
    // Set the default values code
    public static void setDefaultValuesCode(String s) {
    	defaultValuesCode = s;
    }
    
	// Check if the material is a restricted container
	public static boolean isRestrictedContainer(Material item) {
		return restrictedContainers.contains(item);
	}
     
	// Check if the material is a restricted item
	public static boolean isRestrictedItem(Material item) {
		return restrictedItems.contains(item);
	}
     
	// Get the disabled worlds
	public static Set<String> getDisabledWorlds(){
		return disabledWorlds;
	}
     
	// Get the restricted containers set
	public static Set<String> getRestrictedContainersString(){
		Set<String> containers = new HashSet<>();
		for(Material mat : restrictedContainers) {
			containers.add(mat.toString());
		}
		return containers;
	}
     
	// Get the restricted items set
	public static Set<String> getRestrictedItemsString(){
		Set<String> items = new HashSet<>();
		for(Material mat : restrictedContainers) {
			items.add(mat.toString());
		}
		return items;
	}
     
	// Set the disabled worlds
	public static void setDisabledWorlds(Set<String> w) {
		disabledWorlds = w;
	}
     
	// Check if a world is disabled
	public static boolean isWorldDisabled(String w) {
		return disabledWorlds.contains(w);
	}
     
	// Set the groups (config.yml)
	public static void setGroups(LinkedHashMap<String,String> g) {
		groups = g;
	}
     
	// Set the groups' settings (config.yml)
	public static void setGroupsSettings(Map<String,Map<String,Double>> g) {
		groupsSettings = g;
	}
     
	// Get the groups' settings
	public static Map<String,Map<String,Double>> getGroupsSettings(){
		return groupsSettings;
	}
     
	// Get the groups
	public static Set<String> getGroups(){
		return groupsSettings.keySet();
	}
     
	// Get the groups values
	public static LinkedHashMap<String,String> getGroupsValues(){
		return groups;
	}
     
	// Get the default values of perms (claim settings)
	public static Map<String,Boolean> getDefaultValues(){
		return defaultValues;
	}
     
	// Check if a setting is enabled
	public static boolean isEnabled(String p) {
		return enabledSettings.get(p);
	}
     
	// Add a new setting
	public static void addSetting(String s, String p) {
		settings.put(s, p);
	}
     
	// Return a boolean from setting name
	public static boolean getBooleanSetting(String s) {
		if(settings.containsKey(s)) {
			return Boolean.parseBoolean(settings.get(s));
		}
		return false;
	}
     
	// Get the value of a setting
	public static String getSetting(String s) {
		if(settings.containsKey(s)) return settings.get(s);
		return "";
	}
     
	// Get the restricted items material list
	public static List<Material> getRestrictedItems(){
		return restrictedItems;
	}
     
	// Get the restricted containers material list
	public static List<Material> getRestrictedContainers(){
		return restrictedContainers;
	}
     
	// Set the default values
	public static void setDefaultValues(LinkedHashMap<String,Boolean> v) {
		defaultValues = v;
	}
     
	// Set the enabled settings
	public static void setEnabledSettings(Map<String,Boolean> v) {
		enabledSettings = v;
	}
     
	// Set the restricted items
	public static int setRestrictedItems(List<String> mat) {
		int i = 0;
		for(String s : mat) {
			Material matt = Material.matchMaterial(s);
			if(matt != null) {
     			restrictedItems.add(matt);
     			i++;
     		}
		}
		return i;
	}
     
	// Set the restricted containers
	public static int setRestrictedContainers(List<String> mat) {
		int i = 0;
		for(String s : mat) {
			Material matt = Material.matchMaterial(s);
			if(matt != null) {
     			restrictedContainers.add(matt);
     			i++;
     		}
     	}
		return i;
	}
    
	// Get the enabled/disabled settings
	public static Map<String,Boolean> getStatusSettings(){
    	return enabledSettings;
    }
}
