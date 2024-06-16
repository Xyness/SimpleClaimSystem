package fr.xyness.SCS.Others;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;

public class ClaimPurge {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private static long offlineTime;
	private JavaPlugin plugin;
	
	
	// ******************
	// *  Constructors  *
	// ******************
	
	
	// Main constructor
	public ClaimPurge(JavaPlugin plugin, int minutes, String time) {
		this.plugin = plugin;
    	int interval = convertTimeToSeconds(time);
    	offlineTime = interval * 1000;
		startPurge(minutes);
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to check if a player has been offline for the time set in config.yml (if true = the plugin delete all his claims)
    public static boolean hasBeenOfflineFor(String playerName) {
    	Player player = Bukkit.getPlayer(playerName);
    	if(player == null) {
    		OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
    		if(!target.hasPlayedBefore()) return false;
            long lastPlayed = target.getLastPlayed();
            if (lastPlayed <= 0) return false;
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastPlayed;
            long minutesInMillis = offlineTime;
            return timeElapsed > minutesInMillis;
    	}
        return false;
    }
    
    // Method to start purge
    public void startPurge(int minutes) {
    	int ticks = minutes * 60 * 20;
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
    			Map<String,String> players = new HashMap<>();
    			for(String owner : ClaimMain.getClaimsOwners()) {
    				if(hasBeenOfflineFor(owner)) {
    					int nb = ClaimMain.getPlayerClaimsCount(owner);
    					players.put(owner, String.valueOf(nb));
    					ClaimMain.deleteAllClaim(owner);
    				}
    			}
    	        StringBuilder sb = new StringBuilder();
    	        if(players.isEmpty()) {
    	        	sb.append("no claims removed.");
    	        } else {
	    	        for (Map.Entry<String, String> entry : players.entrySet()) {
	    	            String key = entry.getKey();
	    	            String value = entry.getValue();
	    	            sb.append(key).append(" (").append(value).append(" claims), ");
	    	        }
	    	        if (sb.length() > 0) {
	    	            sb.setLength(sb.length() - 2);
	    	        }
	    	        sb.append(".");
    	        }
    	        plugin.getLogger().info("Auto-purge: "+sb.toString());
    		}, minutes, minutes, TimeUnit.MINUTES);
    	} else {
    		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
    			@Override
    	        public void run() {
        			Map<String,String> players = new HashMap<>();
        			for(String owner : ClaimMain.getClaimsOwners()) {
        				if(hasBeenOfflineFor(owner)) {
        					int nb = ClaimMain.getPlayerClaimsCount(owner);
        					players.put(owner, String.valueOf(nb));
        					ClaimMain.deleteAllClaim(owner);
        				}
        			}
        	        StringBuilder sb = new StringBuilder();
        	        if(players.isEmpty()) {
        	        	sb.append("no claims removed.");
        	        } else {
    	    	        for (Map.Entry<String, String> entry : players.entrySet()) {
    	    	            String key = entry.getKey();
    	    	            String value = entry.getValue();
    	    	            sb.append(key).append(" (").append(value).append(" claims), ");
    	    	        }
    	    	        if (sb.length() > 0) {
    	    	            sb.setLength(sb.length() - 2);
    	    	        }
        	        }
        	        plugin.getLogger().info("Auto-purge: "+sb.toString());
    			}
    		}, ticks, ticks);
    	}
    }
    
    // Method to convert time from config.yml to minutes
    public static int convertTimeToSeconds(String time) {
        int totalMinutes = 0;
        StringBuilder number = new StringBuilder();
        for (char c : time.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                int value = Integer.parseInt(number.toString());
                switch (c) {
	                case 's':
	                    totalMinutes += value;
	                    break;
                    case 'm':
                        totalMinutes += value * 60;
                        break;
                    case 'h':
                        totalMinutes += value * 60 * 60;
                        break;
                    case 'd':
                        totalMinutes += value * 60 * 60 * 24;
                        break;
                    case 'w':
                        totalMinutes += value * 60 * 60 * 24 * 7;
                        break;
                    case 'M':
                        totalMinutes += value * 60 * 60 * 24 * 30;
                        break;
                    case 'y':
                        totalMinutes += value * 60 * 60 * 24 * 365;
                        break;
                    default:
                        return 60;
                }
                number.setLength(0);
            }
        }
        return totalMinutes;
    }
}
