package fr.xyness.SCS.Others;

import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
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
		offlineTime = minutes * 60 * 1000;
		startPurge(time);
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to check if a player has been offline for the time set in config.yml (if true = the plugin delete all his claims)
    public static boolean hasBeenOfflineFor(String playerName) {
        OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
        if(!target.hasPlayedBefore()) return false;
        long lastPlayed = target.getLastPlayed();
        if (lastPlayed <= 0) return false;
        long currentTime = System.currentTimeMillis();
        long timeElapsed = currentTime - lastPlayed;
        long minutesInMillis = offlineTime;
        return timeElapsed > minutesInMillis;
    }
    
    // Method to start purge
    public void startPurge(String time) {
    	int interval = convertTimeToMinutes(time);
    	int intervalTicks = interval * 60 * 20;
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runAtFixedRate(plugin, task -> {
    			for(String owner : ClaimMain.getClaimsOwners()) {
    				if(hasBeenOfflineFor(owner)) {
    					ClaimMain.deleteAllClaim(owner);
    				}
    			}
    		}, interval, interval, TimeUnit.MINUTES);
    	} else {
    		Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, new Runnable() {
    			@Override
    	        public void run() {
	    			for(String owner : ClaimMain.getClaimsOwners()) {
	    				if(hasBeenOfflineFor(owner)) {
	    					ClaimMain.deleteAllClaim(owner);
	    				}
	    			}
    			}
    		}, intervalTicks, intervalTicks);
    	}
    }
    
    // Method to convert time from config.yml to minutes
    public static int convertTimeToMinutes(String time) {
        int totalMinutes = 0;
        StringBuilder number = new StringBuilder();
        for (char c : time.toCharArray()) {
            if (Character.isDigit(c)) {
                number.append(c);
            } else {
                int value = Integer.parseInt(number.toString());
                switch (c) {
                    case 'm':
                        totalMinutes += value;
                        break;
                    case 'h':
                        totalMinutes += value * 60;
                        break;
                    case 'd':
                        totalMinutes += value * 60 * 24;
                        break;
                    case 'w':
                        totalMinutes += value * 60 * 24 * 7;
                        break;
                    case 'M':
                        totalMinutes += value * 60 * 24 * 30;
                        break;
                    case 'y':
                        totalMinutes += value * 60 * 24 * 365;
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
