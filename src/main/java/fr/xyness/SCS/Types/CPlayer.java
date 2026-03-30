package fr.xyness.SCS.Types;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.CScoreboard;
import fr.xyness.SCS.SimpleClaimSystem;

/**
 * This class handles CPlayer object
 */
public class CPlayer {

    private Player player;
    private UUID playerId;
    private String playerName;
    private Integer claims_count;
    private Boolean claim_chat;
    private Boolean claim_automap;
    private String claim_auto;
    private Claim claim_chunk;
    private Boolean claim_autofly;
    private Boolean claim_fly;
    private Integer gui_page;
    private Claim claim;
    private Map<Integer, Claim> mapClaims = new ConcurrentHashMap<>();
    private Map<Integer, Location> mapLoc = new ConcurrentHashMap<>();
    private Map<Integer, String> mapString = new ConcurrentHashMap<>();
    private String filter;
    private String owner;
    private CScoreboard scoreboard;
    private final SimpleClaimSystem instance;

    public CPlayer(Player player, UUID playerId, Integer claims_count, SimpleClaimSystem instance) {
        this.player = player;
        this.playerId = playerId;
        this.playerName = player.getName();
        this.claims_count = claims_count;
        this.gui_page = 0;
        this.claim_chat = false;
        this.claim_automap = false;
        this.claim_autofly = false;
        this.claim_auto = "";
        this.claim_fly = false;
        this.instance = instance;
    }

    // --- Setters ---

    public void setPlayer(Player player) { this.player = player; }
    public void setName(String playerName) { this.playerName = playerName; }
    public void setClaimsCount(Integer claims_count) { this.claims_count = claims_count; }
    public void setGuiPage(Integer page) { this.gui_page = page; }
    public void setClaimChat(Boolean setting) { this.claim_chat = setting; }
    public void setClaimAutomap(Boolean setting) { this.claim_automap = setting; }
    public void setClaimAuto(String setting) { this.claim_auto = setting; }
    public void setTargetClaimChunk(Claim claim) { this.claim_chunk = claim; }
    public void setClaim(Claim claim) { this.claim = claim; }
    public void addMapClaim(Integer slot, Claim claim) { this.mapClaims.put(slot, claim); }
    public void addMapLoc(Integer slot, Location loc) { this.mapLoc.put(slot, loc); }
    public void addMapString(Integer slot, String s) { this.mapString.put(slot, s); }
    public void setFilter(String filter) { this.filter = filter; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setClaimAutofly(Boolean setting) { this.claim_autofly = setting; }
    public void setClaimFly(Boolean setting) { this.claim_fly = setting; }
    public void setScoreboard(CScoreboard scoreboard) { this.scoreboard = scoreboard; }

    // --- Getters ---

    public Player getPlayer() { return this.player; }
    public String getName() { return this.playerName; }
    public Integer getClaimsCount() { return this.claims_count; }
    public Integer getGuiPage() { return this.gui_page; }
    public Boolean getClaimChat() { return this.claim_chat; }
    public Boolean getClaimAutomap() { return this.claim_automap; }
    public String getClaimAuto() { return this.claim_auto; }
    public Claim getTargetClaimChunk() { return this.claim_chunk; }
    public Claim getClaim() { return this.claim; }
    public Claim getMapClaim(Integer slot) { return this.mapClaims.get(slot); }
    public Location getMapLoc(Integer slot) { return this.mapLoc.get(slot); }
    public String getMapString(Integer slot) { return this.mapString.get(slot); }
    public String getFilter() { return this.filter; }
    public String getOwner() { return this.owner; }
    public Boolean getClaimAutofly() { return this.claim_autofly; }
    public Boolean getClaimFly() { return this.claim_fly; }
    public CScoreboard getScoreboard() { return this.scoreboard; }

    // --- Permission-based limit getters ---

    /**
     * Gets the player's max claims based on permissions, player config, or group settings.
     * Returns 0 for admins (unlimited).
     */
    public Integer getMaxClaims() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-claims")) {
            return (int) Math.round(playerConfig.get("max-claims"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CLAIM_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("max-claims"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-claims")));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the player's max radius claims.
     * Returns 0 for admins (unlimited).
     */
    public Integer getMaxRadiusClaims() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-radius-claims")) {
            return (int) Math.round(playerConfig.get("max-radius-claims"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.RADIUS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("max-radius-claims"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-radius-claims")));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the teleportation delay for the player.
     * Returns 0 for admins (no delay).
     */
    public int getDelay() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("teleportation-delay")) {
            return (int) Math.round(playerConfig.get("teleportation-delay"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.DELAY_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("teleportation-delay"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.min(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("teleportation-delay")));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the maximum number of members per claim for the player.
     * Returns 0 for admins (unlimited).
     */
    public int getMaxMembers() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-members")) {
            return (int) Math.round(playerConfig.get("max-members"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.MEMBERS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("max-members"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-members")));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the cost of a claim for the player.
     * Returns 0 for admins (free).
     */
    public double getCost() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-cost")) {
            return playerConfig.get("claim-cost");
        }

        double n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.COST_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("claim-cost"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("claim-cost"));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the cost of adding a chunk for the player.
     * Returns 0 for admins (free).
     */
    public double getChunkCost() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("chunk-cost")) {
            return playerConfig.get("chunk-cost");
        }

        double n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CHUNK_COST_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("chunk-cost"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("chunk-cost"));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the claim cost multiplier for the player.
     * Returns 0 for admins.
     */
    public double getMultiplier() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-cost-multiplier")) {
            return playerConfig.get("claim-cost-multiplier");
        }

        double n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.MULTIPLIER_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = groupsSettings.get("default").get("claim-cost-multiplier");
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("claim-cost-multiplier"));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the chunk cost multiplier for the player.
     * Returns 0 for admins.
     */
    public double getChunkMultiplier() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("chunk-cost-multiplier")) {
            return playerConfig.get("chunk-cost-multiplier");
        }

        double n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CHUNK_MULTIPLIER_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = groupsSettings.get("default").get("chunk-cost-multiplier");
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.min(n, groupsSettings.get(entry.getKey()).get("chunk-cost-multiplier"));
	                }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the maximum number of chunks per claim for the player.
     * Returns 0 for admins (unlimited).
     */
    public int getMaxChunksPerClaim() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-chunks-per-claim")) {
            return (int) Math.round(playerConfig.get("max-chunks-per-claim"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CHUNKS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("max-chunks-per-claim"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
                    if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                        n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-chunks-per-claim")));
                    }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the minimum required distance between claims for the player.
     * Returns 0 for admins (no restriction).
     */
    public int getClaimDistance() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-distance")) {
            return (int) Math.round(playerConfig.get("claim-distance"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.DISTANCE_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .min().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("claim-distance"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
                    if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                        n = Math.min(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("claim-distance")));
                    }
            	}
            }
        }

        return n;
    }

    /**
     * Gets the max total chunks across all claims for the player.
     * Returns 0 for admins (unlimited).
     */
    public int getMaxChunksTotal() {
        if (player.hasPermission("scs.admin")) return 0;

        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("max-chunks-total")) {
            return (int) Math.round(playerConfig.get("max-chunks-total"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CHUNKS_TOTAL_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {
            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("max-chunks-total"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
            	if(entry.getValue() != null) {
	                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
	                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-chunks-total")));
	                }
            	}
            }
        }

        return n;
    }

    // --- Claim eligibility checks ---

    /**
     * Checks if the player can create one more claim.
     */
    public boolean canClaim() {
        if (player.hasPermission("scs.admin")) return true;
        int maxClaims = getMaxClaims();
        return maxClaims > claims_count || maxClaims == 0;
    }

    /**
     * Checks if the player can create n more claims.
     */
    public boolean canClaimX(int n) {
        if (player.hasPermission("scs.admin")) return true;
        int maxClaims = getMaxClaims();
        return maxClaims > claims_count+n || maxClaims == 0;
    }

    /**
     * Checks if the player can have a claim with the given number of chunks.
     */
    public boolean canClaimWithNumber(int n) {
        if (player.hasPermission("scs.admin")) return true;
        int maxChunks = getMaxChunksPerClaim();
        return maxChunks >= n || maxChunks == 0;
    }

    /**
     * Checks if the player can have the given total number of chunks across all claims.
     */
    public boolean canClaimTotalWithNumber(int total) {
        if (player.hasPermission("scs.admin")) return true;
        int maxChunks = getMaxChunksTotal();
        return maxChunks >= total || maxChunks == 0;
    }

    /**
     * Checks if the player can use the given radius for a radius claim.
     */
    public boolean canRadiusClaim(int r) {
        if (player.hasPermission("scs.admin")) return true;
        int radius = getMaxRadiusClaims();
        return radius >= r || radius == 0;
    }

    // --- Cost calculations ---

    /**
     * Gets the multiplied cost for creating a new claim, applying the cost multiplier
     * based on current claim count.
     */
    public Double getMultipliedCost() {
        if (player.hasPermission("scs.admin")) return 0.0;
        Double cost = getCost();
        Double multiplier = getMultiplier();
        Double result = cost * Math.pow(multiplier, claims_count);
        return Math.round(result * 100.0) / 100.0;
    }

    /**
     * Gets the multiplied cost for adding a chunk, applying the chunk cost multiplier.
     */
    public Double getChunkMultipliedCost(int nb_chunks) {
        if (player.hasPermission("scs.admin")) return 0.0;
        Double cost = getChunkCost();
        Double multiplier = getChunkMultiplier();
        Double result = cost * Math.pow(multiplier, (nb_chunks - 1));
        return Math.round(result * 100.0) / 100.0;
    }

    /**
     * Gets the total multiplied cost for a radius claim (sum of each individual claim cost).
     */
    public Double getRadiusMultipliedCost(int r) {
        if (player.hasPermission("scs.admin")) return 0.0;
        int n = claims_count;
        Double price = 0.0;
        Double cost = getCost();
        Double multiplier = getMultiplier();
        for(int i = 0; i < r; i++) {
            price += cost * Math.pow(multiplier, (n - 1));
            n++;
        }
        return Math.round(price * 100.0) / 100.0;
    }

    // --- Map clearing ---

    public void clearMapClaim() { mapClaims.clear(); }
    public void clearMapLoc() { mapLoc.clear(); }
    public void clearMapString() { mapString.clear(); }
}
