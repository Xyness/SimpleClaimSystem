package fr.xyness.SCS.API;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;

/**
 * Interface providing API methods for the Simple Claim System.
 */
public interface SimpleClaimSystemAPI {
    
	
    // *********************
    // *  Players Methods  *
    // *********************


    /**
     * Retrieves a player's claim by name.
     *
     * @param playerName the name of the player
     * @param claimName the name of the claim
     * @return the player's claim, or null if not found
     */
    Claim getPlayerClaim(String playerName, String claimName);

    /**
     * Retrieves a player's claim by name.
     *
     * @param player the player
     * @param claimName the name of the claim
     * @return the player's claim, or null if not found
     */
    Claim getPlayerClaim(Player player, String claimName);

    /**
     * Retrieves a CPlayer object by player name.
     *
     * @param playerName the name of the player
     * @return the CPlayer object, or null if not found
     */
    CPlayer getCPlayer(String playerName);

    /**
     * Retrieves a CPlayer object by Player instance.
     *
     * @param player the player
     * @return the CPlayer object, or null if not found
     */
    CPlayer getCPlayer(Player player);
    

    // ********************
    // *  Claims Methods  *
    // ********************
    

    /**
     * Unclaims the specified claim.
     *
     * @param claim the claim to unclaim
     * @return true if the claim was successfully unclaimed, false otherwise
     */
    boolean unclaim(Claim claim);

    /**
     * Unclaims all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @return true if the claims were successfully unclaimed, false otherwise
     */
    boolean unclaimAll(String owner);

    /**
     * Checks if a chunk is claimed.
     *
     * @param chunk the chunk to check
     * @return true if the chunk is claimed, false otherwise
     */
    boolean isClaimed(Chunk chunk);

    /**
     * Retrieves the claim at the specified chunk.
     *
     * @param chunk the chunk to check
     * @return the claim at the chunk, or null if not found
     */
    Claim getClaimAtChunk(Chunk chunk);
    
    /**
     * Gets all the claims
     * @return A set of all the claims
     */
    Set<Claim> getAllClaims();
    
    /**
     * Gets the claims in a target world
     * @return A set of all the claims in a specific world
     */
    Set<Claim> getClaims(World targetWorld);

    /**
     * Applies settings from the specified claim to all claims.
     *
     * @param claim the claim with settings to apply
     * @return true if the settings were successfully applied, false otherwise
     */
    boolean applySettingsToAllClaims(Claim claim);

    /**
     * Resets settings for all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @return true if the settings were successfully reset, false otherwise
     */
    boolean resetClaimsSettings(String owner);

    /**
     * Resets settings for all claims.
     *
     * @return true if the settings were successfully reset, false otherwise
     */
    boolean resetAllClaimsSettings();

    /**
     * Merges multiple claims into the main claim.
     *
     * @param mainClaim the main claim
     * @param claimsToMerge the set of claims to merge
     * @return true if the claims were successfully merged, false otherwise
     */
    boolean mergeMultipleClaims(Claim mainClaim, Set<Claim> claimsToMerge);

    /**
     * Bans a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully banned, false otherwise
     */
    boolean banPlayerFromClaim(Claim claim, String targetPlayer);

    /**
     * Unbans a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully unbanned, false otherwise
     */
    boolean unbanPlayerFromClaim(Claim claim, String targetPlayer);

    /**
     * Bans a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully banned, false otherwise
     */
    boolean banPlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Unbans a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully unbanned, false otherwise
     */
    boolean unbanPlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Adds a player to a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully added, false otherwise
     */
    boolean addPlayerToClaim(Claim claim, String targetPlayer);

    /**
     * Removes a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayer the target player
     * @return true if the player was successfully removed, false otherwise
     */
    boolean removePlayerFromClaim(Claim claim, String targetPlayer);

    /**
     * Adds a player to all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully added, false otherwise
     */
    boolean addPlayerToAllClaims(String owner, String targetPlayer);

    /**
     * Removes a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayer the target player
     * @return true if the player was successfully removed, false otherwise
     */
    boolean removePlayerFromAllClaims(String owner, String targetPlayer);

    /**
     * Resets the permissions of a specific claim.
     *
     * @param claim the claim
     * @return true if the permissions were successfully reset, false otherwise
     */
    boolean resetClaimPerm(Claim claim);

    /**
     * Sets a permission for a specific claim.
     *
     * @param claim the claim
     * @param permission the permission to set
     * @param value the value of the permission
     * @return true if the permission was successfully set, false otherwise
     */
    boolean setClaimPerm(Claim claim, String permission, boolean value);

    /**
     * Sets the name of a specific claim.
     *
     * @param claim the claim
     * @param newName the new name
     * @return true if the name was successfully set, false otherwise
     */
    boolean setClaimName(Claim claim, String newName);

    /**
     * Sets the location of a specific claim.
     *
     * @param claim the claim
     * @param newLoc the new location
     * @return true if the location was successfully set, false otherwise
     */
    boolean setClaimLocation(Claim claim, Location newLoc);

    /**
     * Sets the description of a specific claim.
     *
     * @param claim the claim
     * @param newDesc the new description
     * @return true if the description was successfully set, false otherwise
     */
    boolean setClaimDescription(Claim claim, String newDesc);

    /**
     * Adds a claim for sale.
     *
     * @param claim the claim
     * @param claimPrice the price of the claim
     * @return true if the claim was successfully added for sale, false otherwise
     */
    boolean addClaimSale(Claim claim, double claimPrice);

    /**
     * Removes a claim from sale.
     *
     * @param claim the claim
     * @return true if the claim was successfully removed from sale, false otherwise
     */
    boolean removeClaimSale(Claim claim);

    /**
     * Sets the owner of a specific claim.
     *
     * @param claim the claim
     * @param newOwner the new owner
     * @return true if the owner was successfully set, false otherwise
     */
    boolean setClaimOwner(Claim claim, String newOwner);

    /**
     * Adds a chunk to a specific claim.
     *
     * @param claim the claim
     * @param chunk the chunk to add
     * @return true if the chunk was successfully added, false otherwise
     */
    boolean addClaimChunk(Claim claim, Chunk chunk);

    /**
     * Removes a chunk from a specific claim.
     *
     * @param claim the claim
     * @param chunk the chunk to remove
     * @return true if the chunk was successfully removed, false otherwise
     */
    boolean removeClaimChunk(Claim claim, Chunk chunk);
    
    
    // *******************
    // *  Other Methods  *
    // *******************
    

    /**
     * Retrieves the map for a player at a specific chunk.
     *
     * @param player the player
     * @param chunk the chunk
     */
    void getMap(Player player, Chunk chunk);
}
