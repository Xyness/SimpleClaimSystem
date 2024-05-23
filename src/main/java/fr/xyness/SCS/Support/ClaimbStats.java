package fr.xyness.SCS.Support;

import org.bukkit.plugin.java.JavaPlugin;

public class ClaimbStats {
	
	public static Metrics metrics;
	
	public static void enableMetrics(JavaPlugin plugin) {
	    int pluginId = 21435;
	    metrics = new Metrics(plugin, pluginId);
	}
}
