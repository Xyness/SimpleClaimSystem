package fr.xyness.SCS.Support;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimLanguage;

public class ClaimPlaceholdersExpansion extends PlaceholderExpansion {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
    private static ClaimPlaceholdersExpansion instance;

    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimPlaceholdersExpansion() {
        instance = this;
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
    // Method to get the instance
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
        
        if (identifier.equals("player_claims_count")) {
            return String.valueOf(cPlayer.getClaimsCount());
        }
        
        if (identifier.equals("claim_name")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		return ClaimMain.getClaimNameByChunk(chunk);
        	}
        	return ClaimLanguage.getMessage("claim_name-if-no-claim");
        }
        
        if (identifier.equals("player_max_claims")) {
        	int i = cPlayer.getMaxClaims();
        	if(i>0) return String.valueOf(i);
        	return "∞";
        }
        
        if (identifier.equals("player_remain_claims")) {
        	int max = cPlayer.getMaxClaims();
        	if(max==0) return "∞";
        	int i = max - cPlayer.getClaimsCount();
        	if(i>=0) return String.valueOf(i);
        	return "0";
        }
        
        if (identifier.equals("player_max_radius_claims")) {
        	int i = cPlayer.getMaxRadiusClaims();
        	if(i>0) return String.valueOf(i);
        	return "∞";
        }
        
        if (identifier.equals("player_teleportation_delay")) {
        	return String.valueOf(cPlayer.getDelay());
        }
        
        if (identifier.equals("player_max_members")) {
        	int i = cPlayer.getMaxMembers();
        	if(i>0) return String.valueOf(i);
        	return "∞";
        }
        
        if (identifier.equals("player_claim_cost")) {
        	return String.valueOf(cPlayer.getCost());
        }
        
        if (identifier.equals("player_claim_cost_multiplier")) {
        	return String.valueOf(cPlayer.getMultiplier());
        }
        
        if (identifier.equals("claim_owner")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		return ClaimMain.getOwnerInClaim(chunk);
        	}
        	return ClaimLanguage.getMessage("claim_owner-if-no-claim");
        }
        
        if (identifier.equals("claim_description")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		return ClaimMain.getClaimDescription(chunk);
        	}
        	return ClaimLanguage.getMessage("claim_description-if-no-claim");
        }
        
        if (identifier.equals("claim_is_in_sale")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		return String.valueOf(ClaimMain.claimIsInSale(chunk));
        	}
        	return ClaimLanguage.getMessage("claim_is_in_sale-if-no-claim");
        }
        
        if (identifier.equals("claim_sale_price")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		if(ClaimMain.claimIsInSale(chunk)) {
        			return String.valueOf(ClaimMain.getClaimPrice(chunk));
        		}
        		return ClaimLanguage.getMessage("claim_sale_price-if-not-in-sale");
        	}
        	return ClaimLanguage.getMessage("claim_sale_price-if-no-claim");
        }
        
        if (identifier.equals("claim_members_count")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		return String.valueOf(ClaimMain.getClaimMembers(chunk).size());
        	}
        	return ClaimLanguage.getMessage("claim_members_count-if-no-claim");
        }
        
        if (identifier.equals("claim_members_online")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		Set<String> members = ClaimMain.getClaimMembers(chunk);
        		int i = 0;
        		for(String p : members) {
        			Player pmember = Bukkit.getPlayer(p);
        			if(pmember != null) i++;
        		}
        		return String.valueOf(i);
        	}
        	return ClaimLanguage.getMessage("claim_members_online-if-no-claim");
        }
        
        if (identifier.equals("claim_spawn")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		return String.valueOf(ClaimMain.getClaimCoords(chunk));
        	}
        	return ClaimLanguage.getMessage("claim_spawn-if-no-claim");
        }
        
        if (identifier.contains("claim_setting_")) {
        	Chunk chunk = player.getChunk();
        	if(ClaimMain.checkIfClaimExists(chunk)) {
        		identifier = identifier.replaceFirst("claim_setting_", "");
        		if(ClaimMain.canPermCheck(chunk, identifier)) {
        			return ClaimLanguage.getMessage("status-enabled");
        		}
        		return ClaimLanguage.getMessage("status-disabled");
        	}
        	return ClaimLanguage.getMessage("claim_setting-if-no-claim");
        }
        
        return null;
    }

}
