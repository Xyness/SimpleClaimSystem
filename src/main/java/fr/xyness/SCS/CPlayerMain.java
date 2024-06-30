package fr.xyness.SCS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import fr.xyness.SCS.Config.ClaimSettings;

public class CPlayerMain {
    
    // ***************
    // *  Variables  *
    // ***************
    
    /** A map of player names to CPlayer instances */
    private static Map<String, CPlayer> players = new HashMap<>();
    
    /** A map of player names to their configuration settings */
    private static Map<String, Map<String, Double>> playersConfigSettings = new HashMap<>();
    
    /** Set of offline players */
    private static Map<String,OfflinePlayer> offlinePlayers = new HashMap<>();
    
    // ********************
    // *  Other Methods  *
    // ********************
    
    /**
     * Get the offline player by his name.
     *
     * @param meta The player name.
     * @return The OfflinePlayer object.
     */
    public static OfflinePlayer getOfflinePlayer(String playerName) {
    	OfflinePlayer player = offlinePlayers.get(playerName);
    	return player;
    }
    
    /**
     * Load all offline players
     */
    public static void loadOfflinePlayers() {
    	SimpleClaimSystem.executeAsync(() -> {
        	List<OfflinePlayer> op = new ArrayList<>(Arrays.asList(Bukkit.getOfflinePlayers()));
        	op.forEach(p -> {
        		offlinePlayers.put(p.getName(), p);
        	});
    	});
    }
    
    /**
     * Removes the CPlayer instance associated with the given player name.
     * 
     * @param playerName The name of the player
     */
    public static void removeCPlayer(String playerName) {
        players.remove(playerName);
    }
    
    /**
     * Gets the CPlayer instance associated with the given player name.
     * 
     * @param playerName The name of the player
     * @return The CPlayer instance, or null if not found
     */
    public static CPlayer getCPlayer(String playerName) {
        return players.get(playerName);
    }
    
    /**
     * Sets the configuration settings for all players.
     * 
     * @param p A map of player names to their configuration settings
     */
    public static void setPlayersConfigSettings(Map<String, Map<String, Double>> p) {
        playersConfigSettings = p;
    }
    
    /**
     * Checks if a player can add a member to their claim.
     * 
     * @param player The player
     * @param chunk The chunk
     * @return True if the player can add a member, false otherwise
     */
    public static boolean canAddMember(Player player, Chunk chunk) {
        if (player.hasPermission("scs.admin")) return true;
        CPlayer cPlayer = players.get(player.getName());
        int i = ClaimMain.getClaimMembers(chunk).size();
        int nb_members = cPlayer.getMaxMembers();
        return nb_members == 0 || nb_members > i;
    }
    
    /**
     * Checks if a player has a specific permission.
     * 
     * @param player The player
     * @param perm The permission to check
     * @return True if the player has the permission, false otherwise
     */
    public static boolean checkPermPlayer(Player player, String perm) {
    	return player.hasPermission("scs.admin") ? true : player.hasPermission(perm);
    }
    
    /**
     * Activates fly mode for the player.
     * 
     * @param player The player
     */
    public static void activePlayerFly(Player player) {
        CPlayer cPlayer = players.get(player.getName());
        if (!player.getAllowFlight()) {
            player.setAllowFlight(true);
        }
        player.setFlying(true);
        cPlayer.setClaimFly(true);
    }
    
    /**
     * Removes fly mode from the player.
     * 
     * @param player The player
     */
    public static void removePlayerFly(Player player) {
        CPlayer cPlayer = players.get(player.getName());
        if (cPlayer.getClaimFly()) {
            player.setFlying(false);
            player.setAllowFlight(false);
            cPlayer.setClaimFly(false);
        }
    }
    
    /**
     * Updates the permissions of a player when /aclaim reload is executed.
     * 
     * @param player The player
     */
    public static void updatePlayerPermSetting(Player player) {
        Runnable task = () -> {
            Map<String, Map<String, Double>> groupsSettings = ClaimSettings.getGroupsSettings();
            LinkedHashMap<String, String> groups = ClaimSettings.getGroupsValues();
            Map<String, Double> groupPlayerSettings = new HashMap<>();
            groupPlayerSettings.put("max-claims", groupsSettings.get("default").get("max-claims"));
            groupPlayerSettings.put("max-radius-claims", groupsSettings.get("default").get("max-radius-claims"));
            groupPlayerSettings.put("teleportation-delay", groupsSettings.get("default").get("teleportation-delay"));
            groupPlayerSettings.put("max-members", groupsSettings.get("default").get("max-members"));
            groupPlayerSettings.put("claim-cost", groupsSettings.get("default").get("claim-cost"));
            groupPlayerSettings.put("claim-cost-multiplier", groupsSettings.get("default").get("claim-cost-multiplier"));
            for (String group : groups.keySet()) {
                if (CPlayerMain.checkPermPlayer(player, groups.get(group))) {
                    groupPlayerSettings.put("max-claims", groupsSettings.get(group).get("max-claims"));
                    groupPlayerSettings.put("max-radius-claims", groupsSettings.get(group).get("max-radius-claims"));
                    groupPlayerSettings.put("teleportation-delay", groupsSettings.get(group).get("teleportation-delay"));
                    groupPlayerSettings.put("max-members", groupsSettings.get(group).get("max-members"));
                    groupPlayerSettings.put("claim-cost", groupsSettings.get(group).get("claim-cost"));
                    groupPlayerSettings.put("claim-cost-multiplier", groupsSettings.get(group).get("claim-cost-multiplier"));
                    break;
                }
            }
            
            String playerName = player.getName();
            if (!playersConfigSettings.containsKey(playerName)) {
                players.get(playerName).setMaxClaims((int) Math.round(groupPlayerSettings.get("max-claims")));
                players.get(playerName).setMaxRadiusClaims((int) Math.round(groupPlayerSettings.get("max-radius-claims")));
                players.get(playerName).setTeleportationDelay((int) Math.round(groupPlayerSettings.get("teleportation-delay")));
                players.get(playerName).setMaxMembers((int) Math.round(groupPlayerSettings.get("max-members")));
                players.get(playerName).setClaimCost(groupPlayerSettings.get("claim-cost"));
                players.get(playerName).setClaimCostMultiplier(groupPlayerSettings.get("claim-cost-multiplier"));
            } else {
                players.get(playerName).setMaxClaims((int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-claims", groupPlayerSettings.get("max-claims"))));
                players.get(playerName).setMaxRadiusClaims((int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-radius-claims", groupPlayerSettings.get("max-radius-claims"))));
                players.get(playerName).setTeleportationDelay((int) Math.round(playersConfigSettings.get(playerName).getOrDefault("teleportation-delay", groupPlayerSettings.get("teleportation-delay"))));
                players.get(playerName).setMaxMembers((int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-members", groupPlayerSettings.get("max-members"))));
                players.get(playerName).setClaimCost(playersConfigSettings.get(playerName).getOrDefault("claim-cost", groupPlayerSettings.get("claim-cost")));
                players.get(playerName).setClaimCostMultiplier(playersConfigSettings.get(playerName).getOrDefault("claim-cost-multiplier", groupPlayerSettings.get("claim-cost-multiplier")));
            }
        };
        SimpleClaimSystem.executeAsync(task);
    }
    
    /**
     * Sets the permissions of a player when he joins the server.
     * 
     * @param player The player
     */
    public static void addPlayerPermSetting(Player player) {
        Runnable task = () -> {
            Map<String, Map<String, Double>> groupsSettings = ClaimSettings.getGroupsSettings();
            LinkedHashMap<String, String> groups = ClaimSettings.getGroupsValues();
            Map<String, Double> groupPlayerSettings = new HashMap<>();
            groupPlayerSettings.put("max-claims", groupsSettings.get("default").get("max-claims"));
            groupPlayerSettings.put("max-radius-claims", groupsSettings.get("default").get("max-radius-claims"));
            groupPlayerSettings.put("teleportation-delay", groupsSettings.get("default").get("teleportation-delay"));
            groupPlayerSettings.put("max-members", groupsSettings.get("default").get("max-members"));
            groupPlayerSettings.put("claim-cost", groupsSettings.get("default").get("claim-cost"));
            groupPlayerSettings.put("claim-cost-multiplier", groupsSettings.get("default").get("claim-cost-multiplier"));
            for (String group : groups.keySet()) {
                if (CPlayerMain.checkPermPlayer(player, groups.get(group))) {
                    groupPlayerSettings.put("max-claims", groupsSettings.get(group).get("max-claims"));
                    groupPlayerSettings.put("max-radius-claims", groupsSettings.get(group).get("max-radius-claims"));
                    groupPlayerSettings.put("teleportation-delay", groupsSettings.get(group).get("teleportation-delay"));
                    groupPlayerSettings.put("max-members", groupsSettings.get(group).get("max-members"));
                    groupPlayerSettings.put("claim-cost", groupsSettings.get(group).get("claim-cost"));
                    groupPlayerSettings.put("claim-cost-multiplier", groupsSettings.get(group).get("claim-cost-multiplier"));
                    break;
                }
            }
            
            String playerName = player.getName();
            if (!playersConfigSettings.containsKey(playerName)) {
                players.put(playerName, new CPlayer(player, ClaimMain.getPlayerClaimsCount(playerName),
                    (int) Math.round(groupPlayerSettings.get("max-claims")),
                    (int) Math.round(groupPlayerSettings.get("max-radius-claims")),
                    (int) Math.round(groupPlayerSettings.get("teleportation-delay")),
                    (int) Math.round(groupPlayerSettings.get("max-members")),
                    groupPlayerSettings.get("claim-cost"),
                    groupPlayerSettings.get("claim-cost-multiplier")));
            } else {
                players.put(playerName, new CPlayer(player, ClaimMain.getPlayerClaimsCount(playerName),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-claims", groupPlayerSettings.get("max-claims"))),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-radius-claims", groupPlayerSettings.get("max-radius-claims"))),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("teleportation-delay", groupPlayerSettings.get("teleportation-delay"))),
                    (int) Math.round(playersConfigSettings.get(playerName).getOrDefault("max-members", groupPlayerSettings.get("max-members"))),
                    playersConfigSettings.get(playerName).getOrDefault("claim-cost", groupPlayerSettings.get("claim-cost")),
                    playersConfigSettings.get(playerName).getOrDefault("claim-cost-multiplier", groupPlayerSettings.get("claim-cost-multiplier"))));
            }
        };
        SimpleClaimSystem.executeAsync(task);
    }
}
