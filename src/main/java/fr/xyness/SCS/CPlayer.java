package fr.xyness.SCS;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

/**
 * This class handles CPlayer object
 */
public class CPlayer {
    
	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** The player associated with this CPlayer instance */
    private Player player;
    
    /** The name of the player */
    private String playerName;
    
    /** The number of claims the player has */
    private Integer claims_count;
    
    /** The maximum number of claims the player can have */
    private Integer max_claims;
    
    /** The maximum radius for claims */
    private Integer max_radius_claims;
    
    /** The teleportation delay for the player */
    private Integer teleportation_delay;
    
    /** The maximum number of members per player's claim */
    private Integer max_members;
    
    /** The cost of a claim */
    private Double claim_cost;
    
    /** The multiplier for the claim cost */
    private Double claim_cost_multiplier;
    
    /** The max chunks per claim */
    private Integer max_chunks_per_claim;
    
    /** The distance for claim */
    private Integer claim_distance;
    
    /** The max chunks total */
    private Integer max_chunks_total;
    
    /** Whether the player has claim chat enabled */
    private Boolean claim_chat;
    
    /** Whether the player has claim automap enabled */
    private Boolean claim_automap;
    
    /** Whether the player has claim autoclaim enabled */
    private Boolean claim_autoclaim;
    
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
    
    /** Pattern for matching claim permissions */
    private static final Pattern CLAIM_PATTERN = Pattern.compile("scs\\.claim\\.(\\d+)");
    
    /** Pattern for matching radius permissions */
    private static final Pattern RADIUS_PATTERN = Pattern.compile("scs\\.radius\\.(\\d+)");
    
    /** Pattern for matching delay permissions */
    private static final Pattern DELAY_PATTERN = Pattern.compile("scs\\.delay\\.(\\d+)");
    
    /** Pattern for matching cost permissions */
    private static final Pattern COST_PATTERN = Pattern.compile("scs\\.cost\\.(\\d+)");
    
    /** Pattern for matching multiplier permissions */
    private static final Pattern MULTIPLIER_PATTERN = Pattern.compile("scs\\.multiplier\\.(\\d+)");
    
    /** Pattern for matching member permissions */
    private static final Pattern MEMBERS_PATTERN = Pattern.compile("scs\\.members\\.(\\d+)");
    
    /** Pattern for matching chunks permissions */
    private static final Pattern CHUNKS_PATTERN = Pattern.compile("scs\\.chunks\\.(\\d+)");
    
    /** Pattern for matching distance permissions */
    private static final Pattern DISTANCE_PATTERN = Pattern.compile("scs\\.distance\\.(\\d+)");
    
    /** Pattern for matching chunks total permissions */
    private static final Pattern CHUNKS_TOTAL_PATTERN = Pattern.compile("scs\\.chunks-total\\.(\\d+)");
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor initializing all fields.
     * 
     * @param player The player associated with this CPlayer instance
     * @param claims_count The number of claims the player has
     * @param max_claims The maximum number of claims the player can have
     * @param max_radius_claims The maximum radius for claims
     * @param teleportation_delay The teleportation delay for the player
     * @param max_members The maximum number of members per player's claim
     * @param claim_cost The cost of a claim
     * @param claim_cost_multiplier The multiplier for the claim cost
     */
    public CPlayer(Player player, Integer claims_count, Integer max_claims, Integer max_radius_claims, Integer teleportation_delay, Integer max_members, Double claim_cost, Double claim_cost_multiplier, Integer max_chunks_per_claim, Integer claim_distance, Integer max_chunks_total) {
        this.player = player;
        this.playerName = player.getName();
        this.claims_count = claims_count;
        this.max_claims = max_claims;
        this.max_radius_claims = max_radius_claims;
        this.teleportation_delay = teleportation_delay;
        this.max_members = max_members;
        this.claim_cost = claim_cost;
        this.claim_cost_multiplier = claim_cost_multiplier;
        this.gui_page = 0;
        this.claim_chat = false;
        this.claim_automap = false;
        this.claim_autoclaim = false;
        this.claim_autofly = false;
        this.claim_fly = false;
        this.max_chunks_per_claim = max_chunks_per_claim;
        this.claim_distance = claim_distance;
        this.max_chunks_total = max_chunks_total;
    }
    
    
    // ********************
    // *  Other Methods   *
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
     * Sets the player's max claims.
     * 
     * @param max_claims The new max claims
     */
    public void setMaxClaims(Integer max_claims) { this.max_claims = max_claims; }
    
    /**
     * Sets the player's max radius claims.
     * 
     * @param max_radius_claims The new max radius claims
     */
    public void setMaxRadiusClaims(Integer max_radius_claims) { this.max_radius_claims = max_radius_claims; }
    
    /**
     * Sets the player's teleportation delay.
     * 
     * @param teleportation_delay The new teleportation delay
     */
    public void setTeleportationDelay(Integer teleportation_delay) { this.teleportation_delay = teleportation_delay; }
    
    /**
     * Sets the player's max members per claim.
     * 
     * @param max_members The new max members
     */
    public void setMaxMembers(Integer max_members) { this.max_members = max_members; }
    
    /**
     * Sets the player's claim cost.
     * 
     * @param claim_cost The new claim cost
     */
    public void setClaimCost(Double claim_cost) { this.claim_cost = claim_cost; }
    
    /**
     * Sets the player's claim cost multiplier.
     * 
     * @param claim_cost_multiplier The new claim cost multiplier
     */
    public void setClaimCostMultiplier(Double claim_cost_multiplier) { this.claim_cost_multiplier = claim_cost_multiplier; }
    
    /**
     * Sets the player's max chunks per claim.
     * 
     * @param max_chunks_per_claim The new max chunks per claim
     */
    public void setMaxChunksPerClaim(Integer max_chunks_per_claim) { this.max_chunks_per_claim = max_chunks_per_claim; }
    
    /**
     * Sets the player's claim distance.
     * 
     * @param claim_distance The new claim distance
     */
    public void setClaimDistance(Integer claim_distance) { this.claim_distance = claim_distance; }
    
    /**
     * Sets the player's max chunks total.
     * 
     * @param max_chunks_total The new max chunks total
     */
    public void setMaxChunksTotal(Integer max_chunks_total) { this.max_chunks_total = max_chunks_total; }
    
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
     * Sets the player's autoclaim mode.
     * 
     * @param setting The new autoclaim mode
     */
    public void setClaimAutoclaim(Boolean setting) { this.claim_autoclaim = setting; }
    
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
     * @return True if autoclaim is enabled, false otherwise
     */
    public Boolean getClaimAutoclaim() { return this.claim_autoclaim; }
    
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
     * Gets the player's max claims.
     * 
     * @return The maximum number of claims
     */
    public Integer getMaxClaims() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.parallelStream()
            .map(CLAIM_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if(maxClaims == -1) return max_claims;
        return maxClaims;
    }
    
    /**
     * Gets the player's max radius claims.
     * 
     * @return The maximum radius claims
     */
    public Integer getMaxRadiusClaims() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.parallelStream()
            .map(RADIUS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if(maxClaims == -1) return max_radius_claims;
        return maxClaims;
    }
    
    /**
     * Checks if the player can claim more chunks.
     * 
     * @return True if the player can claim more, false otherwise
     */
    public boolean canClaim() {
        if (player.hasPermission("scs.admin")) return true;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.parallelStream()
            .map(CLAIM_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (maxClaims > claims_count) return true;
        if (maxClaims == -1) maxClaims = max_claims;

        return maxClaims > claims_count || maxClaims == 0;
    }
    
    /**
     * Checks if the player can claim more chunks with a given amount of new chunks.
     * 
     * @param n The number of new chunks
     * @return True if the player can claim the specified number of chunks, false otherwise
     */
    public boolean canClaimWithNumber(int n) {
        if (player.hasPermission("scs.admin")) return true;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxChunks = permissions.parallelStream()
            .map(CHUNKS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (maxChunks >= n) return true;
        if (maxChunks == -1) maxChunks = max_chunks_per_claim;

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

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxChunks = permissions.parallelStream()
            .map(CHUNKS_TOTAL_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (maxChunks == -1) maxChunks = max_chunks_total;

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

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int radius = permissions.parallelStream()
            .map(RADIUS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (radius >= r) return true;
        if (radius == -1) radius = max_radius_claims;

        return radius >= r || radius == 0;
    }
    
    /**
     * Gets the teleportation delay for the player.
     * 
     * @return The teleportation delay
     */
    public int getDelay() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int delay = permissions.parallelStream()
            .map(DELAY_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (delay == -1) return teleportation_delay;
        return delay;
    }
    
    /**
     * Gets the cost of a claim for the player.
     * 
     * @return The claim cost
     */
    public double getCost() {
        if (player.hasPermission("scs.admin")) return 0.0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        Double cost = permissions.parallelStream()
            .map(COST_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .max().orElse(-1);
        
        if (cost == -1) return claim_cost;
        return cost;
    }
    
    /**
     * Gets the claim cost multiplier for the player.
     * 
     * @return The claim cost multiplier
     */
    public double getMultiplier() {
        if (player.hasPermission("scs.admin")) return 0.0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        Double multiplier = permissions.parallelStream()
            .map(MULTIPLIER_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .max().orElse(-1);
        
        if (multiplier == -1) return claim_cost_multiplier;
        return multiplier;
    }
    
    /**
     * Gets the maximum number of members per claim for the player.
     * 
     * @return The maximum number of members
     */
    public int getMaxMembers() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int members = permissions.parallelStream()
            .map(MEMBERS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (members == -1) return max_members;
        return members;
    }
    
    /**
     * Gets the maximum number of chunks per claim for the player
     * 
     * @return The maximum number of chunks
     */
    public int getMaxChunksPerClaim() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int chunks = permissions.parallelStream()
            .map(CHUNKS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (chunks == -1) return max_chunks_per_claim;
        return chunks;
    }
    
    /**
     * Gets the claim distance
     * 
     * @return The claim distance
     */
    public int getClaimDistance() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int distance = permissions.parallelStream()
            .map(DISTANCE_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (distance == -1) return claim_distance;
        return distance;
    }
    
    /**
     * Gets the max chunks total
     * 
     * @return The max chunks total
     */
    public int getMaxChunksTotal() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().parallelStream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int chunks = permissions.parallelStream()
            .map(CHUNKS_TOTAL_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (chunks == -1) return max_chunks_total;
        return chunks;
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
