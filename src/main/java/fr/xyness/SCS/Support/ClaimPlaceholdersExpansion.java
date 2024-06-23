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
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimLanguage;

/**
 * This class handles the integration with PlaceholderAPI for providing claim-related placeholders.
 */
public class ClaimPlaceholdersExpansion extends PlaceholderExpansion {
	
	// ***************
	// *  Variables  *
	// ***************
	
    /** The singleton instance of ClaimPlaceholdersExpansion. */
    private static ClaimPlaceholdersExpansion instance;

	// ******************
	// *  Constructors  *
	// ******************
    
    /**
     * Main constructor for ClaimPlaceholdersExpansion.
     * Sets the instance of this class.
     */
    public ClaimPlaceholdersExpansion() {
        instance = this;
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
        return instance;
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
        
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        	switch (identifier) {
            case "player_claims_count":
                return String.valueOf(cPlayer.getClaimsCount());
                
            case "claim_name":
                Chunk chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    return ClaimMain.getClaimNameByChunk(chunk);
                }
                return ClaimLanguage.getMessage("claim_name-if-no-claim");
                
            case "player_max_claims":
                int maxClaims = cPlayer.getMaxClaims();
                return maxClaims > 0 ? String.valueOf(maxClaims) : "∞";
                
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
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    return ClaimMain.getOwnerInClaim(chunk);
                }
                return ClaimLanguage.getMessage("claim_owner-if-no-claim");
                
            case "claim_description":
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    return ClaimMain.getClaimDescription(chunk);
                }
                return ClaimLanguage.getMessage("claim_description-if-no-claim");
                
            case "claim_is_in_sale":
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    return String.valueOf(ClaimMain.claimIsInSale(chunk));
                }
                return ClaimLanguage.getMessage("claim_is_in_sale-if-no-claim");
                
            case "claim_sale_price":
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (ClaimMain.claimIsInSale(chunk)) {
                        return String.valueOf(ClaimMain.getClaimPrice(chunk));
                    }
                    return ClaimLanguage.getMessage("claim_sale_price-if-not-in-sale");
                }
                return ClaimLanguage.getMessage("claim_sale_price-if-no-claim");
                
            case "claim_members_count":
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    return String.valueOf(ClaimMain.getClaimMembers(chunk).size());
                }
                return ClaimLanguage.getMessage("claim_members_count-if-no-claim");
                
            case "claim_members_online":
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    Set<String> members = ClaimMain.getClaimMembers(chunk);
                    int onlineMembers = 0;
                    for (String member : members) {
                        Player onlineMember = Bukkit.getPlayer(member);
                        if (onlineMember != null) onlineMembers++;
                    }
                    return String.valueOf(onlineMembers);
                }
                return ClaimLanguage.getMessage("claim_members_online-if-no-claim");
                
            case "claim_spawn":
                chunk = player.getChunk();
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    return String.valueOf(ClaimMain.getClaimCoords(chunk));
                }
                return ClaimLanguage.getMessage("claim_spawn-if-no-claim");
                
            default:
                if (identifier.startsWith("claim_setting_")) {
                    chunk = player.getChunk();
                    if (ClaimMain.checkIfClaimExists(chunk)) {
                        String setting = identifier.replaceFirst("claim_setting_", "");
                        return ClaimMain.canPermCheck(chunk, setting) ? 
                                ClaimLanguage.getMessage("status-enabled") : 
                                ClaimLanguage.getMessage("status-disabled");
                    }
                    return ClaimLanguage.getMessage("claim_setting-if-no-claim");
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
