package fr.xyness.SCS;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 * This class handles CPlayer object
 */
public class CPlayer {
    
	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** The player associated with this CPlayer instance */
    private Player player;
    
    /** The uuid of player */
    private UUID playerId;
    
    /** The name of the player */
    private String playerName;
    
    /** The number of claims the player has */
    private Integer claims_count;
    
    /** Whether the player has claim chat enabled */
    private Boolean claim_chat;
    
    /** Whether the player has claim automap enabled */
    private Boolean claim_automap;
    
    /** Whether the player has claim autoaddchunk enabled */
    private String claim_auto;
    
    /** The claim for autoaddchunk and autodelchunk */
    private Claim claim_chunk;
    
    /** Whether the player has claim autofly enabled */
    private Boolean claim_autofly;
    
    /** Whether the player has claim fly enabled */
    private Boolean claim_fly;
    
    /** The current GUI page */
    private Integer gui_page;
    
    /** The current chunk for the GUI */
    private Claim claim;
    
    /** A map of chunks for the GUI, indexed by slot */
    private Map<Integer, Claim> mapClaims = new HashMap<>();
    
    /** A map of locations for the GUI, indexed by slot */
    private Map<Integer, Location> mapLoc = new HashMap<>();
    
    /** A map of strings for the GUI, indexed by slot */
    private Map<Integer, String> mapString = new HashMap<>();
    
    /** The filter for the GUI */
    private String filter;
    
    /** The owner for the GUI */
    private String owner;
    
    /** The player's scoreboard */
    private CScoreboard scoreboard;
    
    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor initializing all fields.
     * 
     * @param player       The player associated with this CPlayer instance.
     * @param playerId     The UUID of the player.
     * @param claims_count The number of claims the player has.
     * @param instance     Instance of SimpleClaimSystem.
     */
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
    
    
    // ********************
    // *  Other methods   *
    // ********************
    
    
    // Setters
    
    /**
     * Sets the player.
     * 
     * @param player The new player
     */
    public void setPlayer(Player player) { this.player = player; }
    
    /**
     * Sets the player's name.
     * 
     * @param playerName The new player name
     */
    public void setName(String playerName) { this.playerName = playerName; }
    
    /**
     * Sets the player's claims count.
     * 
     * @param claims_count The new claims count
     */
    public void setClaimsCount(Integer claims_count) { this.claims_count = claims_count; }
    
    /**
     * Sets the player's GUI page.
     * 
     * @param page The new GUI page
     */
    public void setGuiPage(Integer page) { this.gui_page = page; }
    
    /**
     * Sets the player's chat mode.
     * 
     * @param setting The new chat mode
     */
    public void setClaimChat(Boolean setting) { this.claim_chat = setting; }
    
    /**
     * Sets the player's automap mode.
     * 
     * @param setting The new automap mode
     */
    public void setClaimAutomap(Boolean setting) { this.claim_automap = setting; }
    
    /**
     * Sets the player's auto mode.
     * 
     * @param setting The new auto mode
     */
    public void setClaimAuto(String setting) { this.claim_auto = setting; }
    
    /**
     * Sets the claim for the autoaddchunk and autodelchunk.
     * 
     * @param claim The new claim
     */
    public void setTargetClaimChunk(Claim claim) { this.claim_chunk = claim; }
    
    /**
     * Sets the claim for the GUI.
     * 
     * @param claim The new claim
     */
    public void setClaim(Claim claim) { this.claim = claim; }
    
    /**
     * Adds a chunk to the GUI map.
     * 
     * @param slot The slot index
     * @param chunk The chunk to add
     */
    public void addMapClaim(Integer slot, Claim claim) { this.mapClaims.put(slot, claim); }
    
    /**
     * Adds a location to the GUI map.
     * 
     * @param slot The slot index
     * @param loc The location to add
     */
    public void addMapLoc(Integer slot, Location loc) { this.mapLoc.put(slot, loc); }
    
    /**
     * Adds a string to the GUI map.
     * 
     * @param slot The slot index
     * @param s The string to add
     */
    public void addMapString(Integer slot, String s) { this.mapString.put(slot, s); }
    
    /**
     * Sets the filter for the GUI.
     * 
     * @param filter The new filter
     */
    public void setFilter(String filter) { this.filter = filter; }
    
    /**
     * Sets the owner for the GUI.
     * 
     * @param owner The new owner
     */
    public void setOwner(String owner) { this.owner = owner; }
    
    /**
     * Sets the player's autofly mode.
     * 
     * @param setting The new autofly mode
     */
    public void setClaimAutofly(Boolean setting) { this.claim_autofly = setting; }
    
    /**
     * Sets the player's fly mode.
     * 
     * @param setting The new fly mode
     */
    public void setClaimFly(Boolean setting) { this.claim_fly = setting; }
    
    /**
     * Sets the player's scoreboard.
     * 
     * @param scoreboard The new scoreboard
     */
    public void setScoreboard(CScoreboard scoreboard) { this.scoreboard = scoreboard; }
    
    // Getters
    
    /**
     * Gets the player.
     * 
     * @return The player
     */
    public Player getPlayer() { return this.player; }
    
    /**
     * Gets the player's name.
     * 
     * @return The player's name
     */
    public String getName() { return this.playerName; }
    
    /**
     * Gets the player's claims count.
     * 
     * @return The claims count
     */
    public Integer getClaimsCount() { return this.claims_count; }
    
    /**
     * Gets the player's current GUI page.
     * 
     * @return The current GUI page
     */
    public Integer getGuiPage() { return this.gui_page; }
    
    /**
     * Gets the player's chat mode.
     * 
     * @return True if claim chat mode is enabled, false otherwise
     */
    public Boolean getClaimChat() { return this.claim_chat; }
    
    /**
     * Gets the player's automap status.
     * 
     * @return True if automap is enabled, false otherwise
     */
    public Boolean getClaimAutomap() { return this.claim_automap; }
    
    /**
     * Gets the player's autoclaim status.
     * 
     * @return The autoclaim status
     */
    public String getClaimAuto() { return this.claim_auto; }
    
    /**
     * Gets the current claim for the autochunk.
     * 
     * @return The current claim
     */
    public Claim getTargetClaimChunk() { return this.claim_chunk; }
    
    /**
     * Gets the current claim for the GUI.
     * 
     * @return The current claim
     */
    public Claim getClaim() { return this.claim; }
    
    /**
     * Gets a chunk by its slot.
     * 
     * @param slot The slot index
     * @return The chunk at the specified slot
     */
    public Claim getMapClaim(Integer slot) { return this.mapClaims.get(slot); }
    
    /**
     * Gets a location by its slot.
     * 
     * @param slot The slot index
     * @return The location at the specified slot
     */
    public Location getMapLoc(Integer slot) { return this.mapLoc.get(slot); }
    
    /**
     * Gets a string by its slot.
     * 
     * @param slot The slot index
     * @return The string at the specified slot
     */
    public String getMapString(Integer slot) { return this.mapString.get(slot); }
    
    /**
     * Gets the filter for the GUI.
     * 
     * @return The filter
     */
    public String getFilter() { return this.filter; }
    
    /**
     * Gets the owner for the GUI.
     * 
     * @return The owner
     */
    public String getOwner() { return this.owner; }
    
    /**
     * Gets the player's autofly status.
     * 
     * @return True if autofly is enabled, false otherwise
     */
    public Boolean getClaimAutofly() { return this.claim_autofly; }
    
    /**
     * Gets the player's fly status.
     * 
     * @return True if fly is enabled, false otherwise
     */
    public Boolean getClaimFly() { return this.claim_fly; }
    
    /**
     * Gets the player's scoreboard.
     * 
     * @return The scoreboard of the player.
     */
    public CScoreboard getScoreboard() { return this.scoreboard; }
    
    /**
     * Gets the player's max claims.
     * 
     * @return The maximum number of claims
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-claims")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the player's max radius claims.
     * 
     * @return The maximum radius claims
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-radius-claims")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the teleportation delay for the player.
     * 
     * @return The teleportation delay
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("teleportation-delay")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the maximum number of members per claim for the player.
     * 
     * @return The maximum number of members
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-members")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the cost of a claim for the player.
     * 
     * @return The claim cost
     */
    public double getCost() {
        if (player.hasPermission("scs.admin")) return 0;
        
        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-cost")) {
            return (int) Math.round(playerConfig.get("claim-cost"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.COST_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {

            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("claim-cost"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("claim-cost")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the cost of a chunk for the player.
     * 
     * @return The chunk cost
     */
    public double getChunkCost() {
        if (player.hasPermission("scs.admin")) return 0;
        
        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("chunk-cost")) {
            return (int) Math.round(playerConfig.get("chunk-cost"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CHUNK_COST_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {

            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("chunk-cost"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("chunk-cost")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the claim cost multiplier for the player.
     * 
     * @return The claim cost multiplier
     */
    public double getMultiplier() {
        if (player.hasPermission("scs.admin")) return 0;
        
        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("claim-cost-multiplier")) {
            return (int) Math.round(playerConfig.get("claim-cost-multiplier"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.MULTIPLIER_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {

            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("claim-cost-multiplier"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("claim-cost-multiplier")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the chunk cost multiplier for the player.
     * 
     * @return The chunk cost multiplier
     */
    public double getChunkMultiplier() {
        if (player.hasPermission("scs.admin")) return 0;
        
        Map<String, Double> playerConfig = instance.getPlayerMain().getPlayerConfig(playerId);
        if (playerConfig != null && playerConfig.containsKey("chunk-cost-multiplier")) {
            return (int) Math.round(playerConfig.get("chunk-cost-multiplier"));
        }

        int n = player.getEffectivePermissions().stream()
            .map(PermissionAttachmentInfo::getPermission)
            .map(CPlayerMain.CHUNK_MULTIPLIER_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);

        if (n == -1) {

            Map<String, Map<String, Double>> groupsSettings = instance.getSettings().getGroupsSettings();
            LinkedHashMap<String, String> groups = instance.getSettings().getGroupsValues();
            n = (int) Math.round(groupsSettings.get("default").get("chunk-cost-multiplier"));
            for (Map.Entry<String, String> entry : groups.entrySet()) {
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("chunk-cost-multiplier")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the maximum number of chunks per claim for the player
     * 
     * @return The maximum number of chunks
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-chunks-per-claim")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the claim distance
     * 
     * @return The claim distance
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("claim-distance")));
                }
            }
        }

        return n;
    }
    
    /**
     * Gets the max chunks total
     * 
     * @return The max chunks total
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
                if (instance.getPlayerMain().checkPermPlayer(player, entry.getValue())) {
                    n = Math.max(n, (int) Math.round(groupsSettings.get(entry.getKey()).get("max-chunks-total")));
                }
            }
        }

        return n;
    }
    
    /**
     * Checks if the player can claim more chunks.
     * 
     * @return True if the player can claim more, false otherwise
     */
    public boolean canClaim() {
        if (player.hasPermission("scs.admin")) return true;
        int maxClaims = getMaxClaims();
        return maxClaims > claims_count || maxClaims == 0;
    }
    
    /**
     * Checks if the player can claim more chunks.
     * 
     * @param n The number of new claims
     * @return True if the player can claim more, false otherwise
     */
    public boolean canClaimX(int n) {
        if (player.hasPermission("scs.admin")) return true;
        int maxClaims = getMaxClaims();
        return maxClaims > claims_count+n || maxClaims == 0;
    }
    
    /**
     * Checks if the player can claim more chunks with a given amount of new chunks.
     * 
     * @param n The number of new chunks
     * @return True if the player can claim the specified number of chunks, false otherwise
     */
    public boolean canClaimWithNumber(int n) {
        if (player.hasPermission("scs.admin")) return true;
        int maxChunks = getMaxChunksPerClaim();
        return maxChunks >= n || maxChunks == 0;
    }
    
    /**
     * Checks if the player can claim more chunks with a given amount of new chunks.
     * 
     * @param total The new total to check
     * @return True if the player can claim the specified number of chunks, false otherwise
     */
    public boolean canClaimTotalWithNumber(int total) {
        if (player.hasPermission("scs.admin")) return true;
        int maxChunks = getMaxChunksTotal();
        return maxChunks >= total || maxChunks == 0;
    }
    
    /**
     * Checks if the player can use the given radius for a radius claim.
     * 
     * @param r The radius
     * @return True if the player can use the radius, false otherwise
     */
    public boolean canRadiusClaim(int r) {
        if (player.hasPermission("scs.admin")) return true;
        int radius = getMaxRadiusClaims();
        return radius >= r || radius == 0;
    }
    
    /**
     * Gets the multiplied cost for a claim.
     * 
     * @return The multiplied claim cost
     */
    public Double getMultipliedCost() {
        if (player.hasPermission("scs.admin")) return 0.0;
        Double cost = getCost();
        Double multiplier = getMultiplier();
        return cost * Math.pow(multiplier, (claims_count - 1));
    }
    
    /**
     * Gets the multiplied cost for a chunk.
     * 
     * @return The multiplied chunk cost
     */
    public Double getChunkMultipliedCost(int nb_chunks) {
        if (player.hasPermission("scs.admin")) return 0.0;
        Double cost = getChunkCost();
        Double multiplier = getChunkMultiplier();
        return cost * Math.pow(multiplier, (nb_chunks - 1));
    }
    
    /**
     * Gets the multiplied cost for a radius claim.
     * 
     * @param r The radius
     * @return The multiplied radius claim cost
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
        return price;
    }
    
    /**
     * Clears the chunk map.
     */
    public void clearMapClaim() { mapClaims.clear(); }
    
    /**
     * Clears the location map.
     */
    public void clearMapLoc() { mapLoc.clear(); }
    
    /**
     * Clears the string map.
     */
    public void clearMapString() { mapString.clear(); }
}
