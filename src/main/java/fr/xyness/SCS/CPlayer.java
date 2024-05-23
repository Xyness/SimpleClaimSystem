package fr.xyness.SCS;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

public class CPlayer {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private Player player;
	private String playerName;
	private Integer claims_count;
	private Integer max_claims;
	private Integer max_radius_claims;
	private Integer teleportation_delay;
	private Integer max_members;
	private Double claim_cost;
	private Double claim_cost_multiplier;
	private Integer gui_page;
	private Boolean claim_chat;
	private Boolean claim_automap;
	private Boolean claim_autoclaim;
	
	private static final Pattern CLAIM_PATTERN = Pattern.compile("scs\\.claim\\.(\\d+)");
	private static final Pattern RADIUS_PATTERN = Pattern.compile("scs\\.radius\\.(\\d+)");
	private static final Pattern DELAY_PATTERN = Pattern.compile("scs\\.delay\\.(\\d+)");
	private static final Pattern COST_PATTERN = Pattern.compile("scs\\.cost\\.(\\d+)");
	private static final Pattern MULTIPLIER_PATTERN = Pattern.compile("scs\\.multiplier\\.(\\d+)");
	private static final Pattern MEMBERS_PATTERN = Pattern.compile("scs\\.members\\.(\\d+)");
    
    
	// ******************
	// *  Constructors  *
	// ******************
	
	
	public CPlayer(Player player, Integer claims_count, Integer max_claims, Integer max_radius_claims, Integer teleportation_delay, Integer max_members, Double claim_cost, Double claim_cost_multiplier) {
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
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Setters
	public void setPlayer(Player player) { this.player = player; } // Set the player
	public void setName(String playerName) { this.playerName = playerName; } // Set the player's name
	public void setClaimsCount(Integer claims_count) { this.claims_count = claims_count; } // Set the player's claims count
	public void setMaxClaims(Integer max_claims) { this.max_claims = max_claims; } // Set the player's max claims
	public void setMaxRadiusClaims(Integer max_radius_claims) { this.max_radius_claims = max_radius_claims; } // Set the player's max radius claims
	public void setTeleportationDelay(Integer teleportation_delay) { this.teleportation_delay = teleportation_delay; } // Set the player's teleportation delay
	public void setMaxMembers(Integer max_members) { this.max_members = max_members; } // Set the player's max members per player's claim
	public void setClaimCost(Double claim_cost) { this.claim_cost = claim_cost; } // Set the player's claim cost
	public void setClaimCostMultiplier(Double claim_cost_multiplier) { this.claim_cost_multiplier = claim_cost_multiplier; } // Set the player's claim cost multiplier
	public void setGuiPage(Integer page) { this.gui_page = page; } // Set the player's gui page
	public void setClaimChat(Boolean setting) { this.claim_chat = setting; } // Set the player's chat mode
	public void setClaimAutomap(Boolean setting) { this.claim_automap = setting; } // Set the player's automap mode
	public void setClaimAutoclaim(Boolean setting) { this.claim_autoclaim = setting; } // Set the player's autoclaim mode
	
	// Getters
	public Player getPlayer() { return this.player; } // Return the player
	public String getName() { return this.playerName; } // Return the player's name
	public Integer getClaimsCount() { return this.claims_count; } // Return the player's claims count
	public Integer getGuiPage() { return this.gui_page; } // Return the player's current gui page
	public Boolean getClaimChat() { return this.claim_chat; } // Return the player's chat mode (true = claim chat mode, false = public chat mode)
	public Boolean getClaimAutomap() { return this.claim_automap; } // Return the player's automap status
	public Boolean getClaimAutoclaim() { return this.claim_autoclaim; } // Return the player's autoclaim status
	
	// Get the player's max claims
	public Integer getMaxClaims() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.stream()
            .map(CLAIM_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if(maxClaims == -1) return max_claims;
        return maxClaims;
	}
	
	// Get the player's max radius claims
	public Integer getMaxRadiusClaims() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.stream()
            .map(RADIUS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if(maxClaims == -1) return max_radius_claims;
        return maxClaims;
	}
	
	// Check if the player can claim anymore
	public boolean canClaim() {
        if (player.hasPermission("scs.admin")) return true;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.stream()
            .map(CLAIM_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (maxClaims > claims_count) return true;
        if (maxClaims == -1) maxClaims = max_claims;

        return maxClaims > claims_count || maxClaims == 0;
    }
	
	// Check if the player can claim anymore (with a given amount of new claims)
	public boolean canClaimWithNumber(int n) {
        if (player.hasPermission("scs.admin")) return true;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int maxClaims = permissions.stream()
            .map(CLAIM_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (maxClaims > claims_count+n) return true;
        if (maxClaims == -1) maxClaims = max_claims;

        return maxClaims > claims_count+n || maxClaims == 0;
    }
	
	// Check if the player can use the given radius for radius claim
	public boolean canRadiusClaim(int r) {
        if (player.hasPermission("scs.admin")) return true;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int radius = permissions.stream()
            .map(RADIUS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (radius >= r) return true;
        if (radius == -1) radius = max_radius_claims;

        return radius > r || radius == 0;
    }
	
	// Get the teleportation delay of player
	public int getDelay() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int delay = permissions.stream()
            .map(DELAY_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (delay == -1) return teleportation_delay;
        return delay;
    }
	
	// Get the player's claim cost
	public double getCost() {
        if (player.hasPermission("scs.admin")) return 0.0;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        Double cost = permissions.stream()
            .map(COST_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .max().orElse(-1);
        
        if (cost == -1) return claim_cost;
        return cost;
    }
	
	// Get the player's claim cost multiplier
	public double getMultiplier() {
        if (player.hasPermission("scs.admin")) return 0.0;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        Double multiplier = permissions.stream()
            .map(MULTIPLIER_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToDouble(matcher -> Double.parseDouble(matcher.group(1)))
            .max().orElse(-1);
        
        if (multiplier == -1) return claim_cost_multiplier;
        return multiplier;
    }
	
	// Get the player's max members per player's claim
	public int getMaxMembers() {
        if (player.hasPermission("scs.admin")) return 0;

        Set<String> permissions = player.getEffectivePermissions().stream()
            .map(permissionAttachmentInfo -> permissionAttachmentInfo.getPermission())
            .collect(Collectors.toSet());

        int members = permissions.stream()
            .map(MEMBERS_PATTERN::matcher)
            .filter(Matcher::find)
            .mapToInt(matcher -> Integer.parseInt(matcher.group(1)))
            .max().orElse(-1);
        
        if (members == -1) return max_members;
        return members;
    }
	
    // Get the multiplied cost for a claim
    public Double getMultipliedCost() {
    	if(player.hasPermission("scs.admin")) return 0.0;
    	Double cost = getCost();
    	Double multiplier = getMultiplier();
    	return cost * Math.pow(multiplier, (claims_count - 1));
    }
    
    // Get the multiplied cost for a radius claim
    public Double getRadiusMultipliedCost(int r) {
    	if(player.hasPermission("scs.admin")) return 0.0;
    	int n = claims_count;
    	Double price = 0.0;
    	Double cost = getCost();
    	Double multiplier = getMultiplier();
    	for(int i = 0; i<r; i++) {
    		price += cost * Math.pow(multiplier, (n - 1));
    		n++;
    	}
    	return price;
    }
	
}
