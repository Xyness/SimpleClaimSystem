package fr.xyness.SCS.Support;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimLanguage;

/**
 * This class handles the integration with PlaceholderAPI for providing claim-related placeholders.
 */
public class ClaimPlaceholdersExpansion extends PlaceholderExpansion {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
    /** The singleton instance of ClaimPlaceholdersExpansion. */
    private static ClaimPlaceholdersExpansion instancePAPI;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    

	// ******************
	// *  Constructors  *
	// ******************
    
    
    /**
     * Main constructor for ClaimPlaceholdersExpansion.
     * Sets the instance of this class.
     */
    public ClaimPlaceholdersExpansion(SimpleClaimSystem instance) {
        instancePAPI = this;
        this.instance = instance;
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
    /**
     * Gets the singleton instance of ClaimPlaceholdersExpansion.
     *
     * @return the singleton instance.
     */
    public static ClaimPlaceholdersExpansion getExpansionInstance() {
        return instancePAPI;
    }

    @Override
    public String getIdentifier() {
        return "scs";
    }

    @Override
    public String getAuthor() {
        return "Xyness";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onPlaceholderRequest(Player player, String identifier) {
        if (player == null) return "";
        
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        	switch (identifier) {
            case "player_claims_count":
                return String.valueOf(cPlayer.getClaimsCount());
                
            case "claim_name":
                Chunk chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                    return instance.getMain().getClaimNameByChunk(chunk);
                }
                return instance.getLanguage().getMessage("claim_name-if-no-claim");
                
            case "player_max_claims":
                int maxClaims = cPlayer.getMaxClaims();
                return maxClaims > 0 ? String.valueOf(maxClaims) : "∞";
                
            case "player_max_chunks_per_claim":
                int maxChunks = cPlayer.getMaxChunksPerClaim();
                return maxChunks > 0 ? String.valueOf(maxChunks) : "∞";
                
            case "player_max_chunks_total":
                int maxChunksTotal = cPlayer.getMaxChunksTotal();
                return maxChunksTotal > 0 ? String.valueOf(maxChunksTotal) : "∞";
                
            case "player_claim_distance":
                int distance = cPlayer.getClaimDistance();
                return distance > 0 ? String.valueOf(distance) : instance.getLanguage().getMessage("claim_distance-if-zero");
                
            case "player_remain_claims":
                int max = cPlayer.getMaxClaims();
                if (max == 0) return "∞";
                int remainingClaims = max - cPlayer.getClaimsCount();
                return remainingClaims >= 0 ? String.valueOf(remainingClaims) : "0";
                
            case "player_max_radius_claims":
                int maxRadiusClaims = cPlayer.getMaxRadiusClaims();
                return maxRadiusClaims > 0 ? String.valueOf(maxRadiusClaims) : "∞";
                
            case "player_teleportation_delay":
                return String.valueOf(cPlayer.getDelay());
                
            case "player_max_members":
                int maxMembers = cPlayer.getMaxMembers();
                return maxMembers > 0 ? String.valueOf(maxMembers) : "∞";
                
            case "player_claim_cost":
                return String.valueOf(cPlayer.getCost());
                
            case "player_claim_cost_multiplier":
                return String.valueOf(cPlayer.getMultiplier());
                
            case "claim_owner":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	Claim claim = instance.getMain().getClaim(chunk);
                    return claim.getOwner();
                }
                return instance.getLanguage().getMessage("claim_owner-if-no-claim");
                
            case "claim_description":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	Claim claim = instance.getMain().getClaim(chunk);
                    return claim.getDescription();
                }
                return instance.getLanguage().getMessage("claim_description-if-no-claim");
                
            case "claim_is_in_sale":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                    return String.valueOf(instance.getMain().getClaim(chunk).getSale());
                }
                return instance.getLanguage().getMessage("claim_is_in_sale-if-no-claim");
                
            case "claim_sale_price":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	Claim claim = instance.getMain().getClaim(chunk);
                    if (claim.getSale()) {
                        return String.valueOf(claim.getPrice());
                    }
                    return instance.getLanguage().getMessage("claim_sale_price-if-not-in-sale");
                }
                return instance.getLanguage().getMessage("claim_sale_price-if-no-claim");
                
            case "claim_members_count":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	Claim claim = instance.getMain().getClaim(chunk);
                    return String.valueOf(claim.getMembers().size());
                }
                return instance.getLanguage().getMessage("claim_members_count-if-no-claim");
                
            case "claim_members_online":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	Claim claim = instance.getMain().getClaim(chunk);
                	Set<String> members = claim.getMembers();
                	long onlineMembers = members.stream()
                	        .filter(member -> Bukkit.getPlayer(member) != null)
                	        .count();
                	return String.valueOf(onlineMembers);
                }
                return instance.getLanguage().getMessage("claim_members_online-if-no-claim");
                
            case "claim_spawn":
                chunk = player.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	Claim claim = instance.getMain().getClaim(chunk);
                    return String.valueOf(instance.getMain().getClaimCoords(claim));
                }
                return instance.getLanguage().getMessage("claim_spawn-if-no-claim");
                
            default:
                if (identifier.startsWith("claim_setting_")) {
                    chunk = player.getLocation().getChunk();
                    if (instance.getMain().checkIfClaimExists(chunk)) {
                    	Claim claim = instance.getMain().getClaim(chunk);
                        String setting = identifier.replaceFirst("claim_setting_", "");
                        return claim.getPermission(setting) ? 
                                instance.getLanguage().getMessage("status-enabled") : 
                                instance.getLanguage().getMessage("status-disabled");
                    }
                    return instance.getLanguage().getMessage("claim_setting-if-no-claim");
                }
                return null;
        	}
        });
        
        try {
            return future.get(); // Return the result from the CompletableFuture
        } catch (ExecutionException | InterruptedException e) {
            return "";
        }
    }
}
