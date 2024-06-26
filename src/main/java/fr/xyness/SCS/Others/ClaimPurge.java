package fr.xyness.SCS.Others;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;

/**
 * This class handles the automatic purging of claims for players who have been offline for a set amount of time.
 */
public class ClaimPurge {

    // ***************
    // *  Variables  *
    // ***************

    /** The time threshold for purging claims, in milliseconds. */
    private static long offlineTime;

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
    public static boolean hasBeenOfflineFor(String playerName) {
        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            OfflinePlayer target = Bukkit.getOfflinePlayer(playerName);
            if (!target.hasPlayedBefore()) return false;
            long lastPlayed = target.getLastPlayed();
            if (lastPlayed <= 0) return false;
            long currentTime = System.currentTimeMillis();
            long timeElapsed = currentTime - lastPlayed;
            long minutesInMillis = offlineTime;
            return timeElapsed > minutesInMillis;
        }
        return false;
    }

    /**
     * Starts the claim purge process.
     *
     * @param minutes the interval in minutes at which the purge should run.
     * @param time the time threshold for purging claims, as specified in the configuration.
     */
    public static void startPurge(int minutes, String time) {
        int interval = convertTimeToSeconds(time);
        offlineTime = interval * 1000;
        int ticks = minutes * 60 * 20;
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
                Map<String, String> players = new HashMap<>();
                for (String owner : ClaimMain.getClaimsOwners()) {
                    if (hasBeenOfflineFor(owner)) {
                        int nb = ClaimMain.getPlayerClaimsCount(owner);
                        players.put(owner, String.valueOf(nb));
                        ClaimMain.deleteAllClaim(owner);
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
                SimpleClaimSystem.getInstance().getLogger().info("Auto-purge: " + sb.toString());
            }, minutes, minutes, TimeUnit.MINUTES);
        } else {
            Bukkit.getScheduler().runTaskTimerAsynchronously(SimpleClaimSystem.getInstance(), new Runnable() {
                @Override
                public void run() {
                    Map<String, String> players = new HashMap<>();
                    for (String owner : ClaimMain.getClaimsOwners()) {
                        if (hasBeenOfflineFor(owner)) {
                            int nb = ClaimMain.getPlayerClaimsCount(owner);
                            players.put(owner, String.valueOf(nb));
                            ClaimMain.deleteAllClaim(owner);
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
                    }
                    SimpleClaimSystem.getInstance().getLogger().info("Auto-purge: " + sb.toString());
                }
            }, ticks, ticks);
        }
    }

    /**
     * Converts a time string from the configuration to seconds.
     *
     * @param time the time string to convert.
     * @return the equivalent time in seconds.
     */
    public static int convertTimeToSeconds(String time) {
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
