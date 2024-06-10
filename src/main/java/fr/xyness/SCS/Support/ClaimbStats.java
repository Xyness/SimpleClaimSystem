package fr.xyness.SCS.Support;

import org.bukkit.plugin.java.JavaPlugin;

public class ClaimbStats {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	public static Metrics metrics;
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to enable metrics of bStats
	public static void enableMetrics(JavaPlugin plugin) {
	    int pluginId = 21435;
	    metrics = new Metrics(plugin, pluginId);
	}
}
