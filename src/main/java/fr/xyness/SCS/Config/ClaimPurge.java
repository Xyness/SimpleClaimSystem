package fr.xyness.SCS.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import fr.xyness.SCS.SimpleClaimSystem;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * This class handles the automatic purging of claims for players who have been offline for a set amount of time.
 */
public class ClaimPurge {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** The time threshold for purging claims, in milliseconds. */
    private long offlineTime;
    
    /** BukkitTask of the purge system. */
    private BukkitTask BukkitTaskPurge = null;

    /** ScheduledTask of the purge system. */
    private ScheduledTask ScheduledTaskPurge = null;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimPurge.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimPurge(SimpleClaimSystem instance) {
        this.instance = instance;
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Checks if a player has been offline for the duration set in the configuration.
     * If true, the system will delete all their claims.
     *
     * @param playerName the name of the player.
     * @return true if the player has been offline for the specified time, false otherwise.
     */
    public boolean hasBeenOfflineFor(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (!target.hasPlayedBefore()) return false;
            long lastPlayed = target.getLastPlayed();
            if (lastPlayed <= 0) return false;
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastPlayed;
            return timeElapsed > offlineTime;
        }
        return false;
    }
    
    /**
     * Stops the claim purge process.
     */
    public void stopPurge() {
        if (BukkitTaskPurge != null) {
            BukkitTaskPurge.cancel();
        }
        if (ScheduledTaskPurge != null) {
            ScheduledTaskPurge.cancel();
        }
    }

    /**
     * Starts the claim purge process.
     *
     * @param minutes the interval in minutes at which the purge should run.
     * @param time the time threshold for purging claims, as specified in the configuration.
     */
    public void startPurge(int minutes, String time) {
        int interval = convertTimeToSeconds(time);
        offlineTime = interval * 1000L;
        int ticks = minutes * 60 * 20;

        if (instance.isFolia()) {
            ScheduledTaskPurge = Bukkit.getAsyncScheduler().runAtFixedRate(instance.getPlugin(), task -> {
                purgeClaims();
            }, minutes, minutes, TimeUnit.MINUTES);
        } else {
            BukkitTaskPurge = Bukkit.getScheduler().runTaskTimerAsynchronously(instance.getPlugin(), new Runnable() {
                @Override
                public void run() {
                    purgeClaims();
                }
            }, ticks, ticks);
        }
    }

    /**
     * Purge claims of offline players.
     */
    public void purgeClaims() {
        Map<String, String> players = new HashMap<>();
        for (String owner : instance.getMain().getClaimsOwners()) {
            if (hasBeenOfflineFor(owner)) {
                int nb = instance.getMain().getPlayerClaimsCount(owner);
                players.put(owner, String.valueOf(nb));
                instance.getMain().deleteAllClaims(owner);
            }
        }
        StringBuilder sb = new StringBuilder();
        if (players.isEmpty()) {
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
        instance.getPlugin().getLogger().info("Auto-purge: " + sb.toString());
    }

    /**
     * Converts a time string from the configuration to seconds.
     *
     * @param time the time string to convert.
     * @return the equivalent time in seconds.
     */
    public int convertTimeToSeconds(String time) {
        int totalSeconds = 0;
        Pattern pattern = Pattern.compile("(\\d+)([smhdwMy])");
        Matcher matcher = pattern.matcher(time);

        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            char unit = matcher.group(2).charAt(0);

            switch (unit) {
                case 's':
                    totalSeconds += value;
                    break;
                case 'm':
                    totalSeconds += value * 60;
                    break;
                case 'h':
                    totalSeconds += value * 60 * 60;
                    break;
                case 'd':
                    totalSeconds += value * 60 * 60 * 24;
                    break;
                case 'w':
                    totalSeconds += value * 60 * 60 * 24 * 7;
                    break;
                case 'M':
                    totalSeconds += value * 60 * 60 * 24 * 30;
                    break;
                case 'y':
                    totalSeconds += value * 60 * 60 * 24 * 365;
                    break;
                default:
                    break;
            }
        }

        return totalSeconds;
    }
}
