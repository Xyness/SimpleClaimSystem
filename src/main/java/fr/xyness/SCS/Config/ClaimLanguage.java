package fr.xyness.SCS.Config;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimLanguage {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	public static Map<String,String> lang = new HashMap<>();
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to set the language map
	public static boolean setLanguage(Map<String,String> messagesMap) {
		lang = messagesMap;
		return true;
	}
	
	// Method to get a message from its key
	public static String getMessage(String key) {
		if(lang.containsKey(key)) return lang.get(key);
		return "";
	}
	
	// Method to get a message with placeholders from its key and targeted player
	public static String getMessageWP(String key, String target) {
		if(!ClaimSettings.getBooleanSetting("placeholderapi")) {
			return getMessage(key);
		}
		if(!lang.containsKey(key)) return "";
		Player player = Bukkit.getPlayer(target);
		if(player == null) {
			OfflinePlayer player_offline = Bukkit.getOfflinePlayer(target);
			String txt = PlaceholderAPI.setPlaceholders(player_offline, lang.get(key));
			return txt;
		}
		String txt = PlaceholderAPI.setPlaceholders(player,lang.get(key));
		return txt;
	}
}
