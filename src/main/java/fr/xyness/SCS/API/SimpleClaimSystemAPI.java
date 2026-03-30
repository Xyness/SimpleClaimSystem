package fr.xyness.SCS.API;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;

/**
 * Public API for the SimpleClaimSystem plugin.
 * <p>
 * Obtain an instance via {@link SimpleClaimSystemAPI_Provider#getAPI()}.
 */
public interface SimpleClaimSystemAPI {

    // --- Player Methods ---

    /**
     * Retrieves a player's claim by name.
     *
     * @param player the player
     * @param claimName the name of the claim
     * @return the player's claim, or null if not found
     */
    Claim getPlayerClaim(Player player, String claimName);

    /**
     * Retrieves all claims owned by a player.
     *
     * @param player the player
     * @return a set of the player's claims
     */
    Set<Claim> getPlayerClaims(Player player);

    /**
     * Retrieves all claims where the player is a member but not the owner.
     *
     * @param player the player
     * @return a set of claims where the player is a member
     */
    Set<Claim> getPlayerClaimsWhereMember(Player player);

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

    /**
     * Gets the number of claims owned by a player.
     *
     * @param playerId the UUID of the player
     * @return the number of claims
     */
    int getPlayerClaimsCount(UUID playerId);

    /**
     * Checks if a player is a member of a claim.
     *
     * @param claim the claim
     * @param player the player to check
     * @return true if the player is a member
     */
    boolean isClaimMember(Claim claim, Player player);

    /**
     * Checks if a player is banned from a claim.
     *
     * @param claim the claim
     * @param player the player to check
     * @return true if the player is banned
     */
    boolean isClaimBanned(Claim claim, Player player);

    /**
     * Gets the relationship between a player and a chunk.
     *
     * @param player the player
     * @param chunk the chunk
     * @return the relation string (e.g., "owner", "member", "visitor")
     */
    String getPlayerRelation(Player player, Chunk chunk);

    // --- Claims Methods ---

    /**
     * Unclaims the specified claim.
     *
     * @param claim the claim to unclaim
     * @return true if the claim was successfully unclaimed
     */
    boolean unclaim(Claim claim);

    /**
     * Unclaims all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @return true if the claims were successfully unclaimed
     */
    boolean unclaimAll(String owner);

    /**
     * Checks if a chunk is claimed.
     *
     * @param chunk the chunk to check
     * @return true if the chunk is claimed
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
     * Gets all claims in the system.
     *
     * @return a set of all claims
     */
    Set<Claim> getAllClaims();

    /**
     * Gets all claims in a specific world.
     *
     * @param targetWorld the world to filter by
     * @return a set of claims in the specified world
     */
    Set<Claim> getClaims(World targetWorld);

    /**
     * Gets the total number of claims in the system.
     *
     * @return the total claim count
     */
    int getAllClaimsCount();

    /**
     * Gets all protected areas (admin claims).
     *
     * @return a set of protected area claims
     */
    Set<Claim> getProtectedAreas();

    /**
     * Gets the number of protected areas.
     *
     * @return the protected area count
     */
    int getProtectedAreasCount();

    /**
     * Gets a claim by its name and owner UUID.
     *
     * @param claimName the name of the claim
     * @param ownerUUID the UUID of the owner
     * @return the claim, or null if not found
     */
    Claim getClaimByName(String claimName, UUID ownerUUID);

    /**
     * Gets the owner name of the claim at a specific chunk.
     *
     * @param chunk the chunk
     * @return the owner name, or null if not claimed
     */
    String getClaimOwnerAt(Chunk chunk);

    /**
     * Gets all chunks from all claims owned by a player.
     *
     * @param owner the owner name
     * @return a set of all owned chunks
     */
    Set<Chunk> getAllChunksFromOwner(String owner);

    /**
     * Checks if the area around a chunk is free of other players' claims.
     *
     * @param centerChunk the center chunk
     * @param distance the distance in chunks to check
     * @param playerName the player name to exclude
     * @return true if the area is free
     */
    boolean isAreaClaimFree(Chunk centerChunk, int distance, String playerName);

    /**
     * Checks if a set of chunks are connected (adjacent).
     *
     * @param chunks the chunks to check
     * @return true if all chunks are connected
     */
    boolean areChunksConnected(Set<Chunk> chunks);

    /**
     * Applies settings from the specified claim to all claims.
     *
     * @param claim the claim with settings to apply
     * @return true if successful
     */
    boolean applySettingsToAllClaims(Claim claim);

    /**
     * Resets settings for all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @return true if successful
     */
    boolean resetClaimsSettings(String owner);

    /**
     * Resets settings for all claims.
     *
     * @return true if successful
     */
    boolean resetAllClaimsSettings();

    /**
     * Merges multiple claims into the main claim.
     *
     * @param mainClaim the main claim
     * @param claimsToMerge the set of claims to merge
     * @return true if successful
     */
    boolean mergeMultipleClaims(Claim mainClaim, CustomSet<Claim> claimsToMerge);

    /**
     * Kicks a player from a specific claim by teleporting them to spawn.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     */
    void kickPlayerFromClaim(Claim claim, String targetPlayerName);

    /**
     * Kicks a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayerName the target player name
     */
    void kickPlayerFromAllClaims(String owner, String targetPlayerName);

    /**
     * Bans a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean banPlayerFromClaim(Claim claim, String targetPlayerName);

    /**
     * Unbans a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean unbanPlayerFromClaim(Claim claim, String targetPlayerName);

    /**
     * Bans a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean banPlayerFromAllClaims(String owner, String targetPlayerName);

    /**
     * Unbans a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean unbanPlayerFromAllClaims(String owner, String targetPlayerName);

    /**
     * Adds a player as a member to a specific claim.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean addPlayerToClaim(Claim claim, String targetPlayerName);

    /**
     * Removes a player from a specific claim.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean removePlayerFromClaim(Claim claim, String targetPlayerName);

    /**
     * Adds a player to all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean addPlayerToAllClaims(String owner, String targetPlayerName);

    /**
     * Removes a player from all claims owned by the specified owner.
     *
     * @param owner the name of the owner
     * @param targetPlayerName the target player name
     * @return true if successful
     */
    boolean removePlayerFromAllClaims(String owner, String targetPlayerName);

    /**
     * Resets the permissions of a specific claim.
     *
     * @param claim the claim
     * @return true if successful
     */
    boolean resetClaimPerm(Claim claim);

    /**
     * Sets a permission for a specific claim.
     *
     * @param claim the claim
     * @param permission the permission name
     * @param value the value to set
     * @param role the role (e.g., "visitors", "members", "natural")
     * @return true if successful
     */
    boolean setClaimPerm(Claim claim, String permission, boolean value, String role);

    /**
     * Checks a permission value for a specific chunk and role.
     *
     * @param chunk the chunk
     * @param permission the permission name
     * @param role the role to check
     * @return true if the permission is enabled
     */
    boolean checkClaimPermission(Chunk chunk, String permission, String role);

    /**
     * Sets the name of a specific claim.
     *
     * @param claim the claim
     * @param newName the new name
     * @return true if successful
     */
    boolean setClaimName(Claim claim, String newName);

    /**
     * Sets the teleport location of a specific claim.
     *
     * @param claim the claim
     * @param newLoc the new location
     * @return true if successful
     */
    boolean setClaimLocation(Claim claim, Location newLoc);

    /**
     * Sets the description of a specific claim.
     *
     * @param claim the claim
     * @param newDesc the new description
     * @return true if successful
     */
    boolean setClaimDescription(Claim claim, String newDesc);

    /**
     * Adds a claim for sale at the specified price.
     *
     * @param claim the claim
     * @param claimPrice the price
     * @return true if successful
     */
    boolean addClaimSale(Claim claim, long claimPrice);

    /**
     * Removes a claim from sale.
     *
     * @param claim the claim
     * @return true if successful
     */
    boolean removeClaimSale(Claim claim);

    /**
     * Sets the owner of a specific claim.
     *
     * @param claim the claim
     * @param newOwner the new owner name
     * @return true if successful
     */
    boolean setClaimOwner(Claim claim, String newOwner);

    /**
     * Adds a chunk to a specific claim.
     *
     * @param claim the claim
     * @param chunk the chunk to add
     * @return true if successful
     */
    boolean addClaimChunk(Claim claim, Chunk chunk);

    /**
     * Removes a chunk from a specific claim.
     *
     * @param claim the claim
     * @param chunk the chunk to remove
     * @return true if successful
     */
    boolean removeClaimChunk(Claim claim, Chunk chunk);

    // --- Async Claims Methods ---

    /**
     * Async version of {@link #unclaim(Claim)}.
     *
     * @param claim the claim to unclaim
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> unclaimAsync(Claim claim);

    /**
     * Async version of {@link #unclaimAll(String)}.
     *
     * @param owner the name of the owner
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> unclaimAllAsync(String owner);

    /**
     * Async version of {@link #setClaimName(Claim, String)}.
     *
     * @param claim the claim
     * @param newName the new name
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> setClaimNameAsync(Claim claim, String newName);

    /**
     * Async version of {@link #setClaimDescription(Claim, String)}.
     *
     * @param claim the claim
     * @param newDesc the new description
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> setClaimDescriptionAsync(Claim claim, String newDesc);

    /**
     * Async version of {@link #setClaimOwner(Claim, String)}.
     *
     * @param claim the claim
     * @param newOwner the new owner name
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> setClaimOwnerAsync(Claim claim, String newOwner);

    /**
     * Async version of {@link #addPlayerToClaim(Claim, String)}.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> addPlayerToClaimAsync(Claim claim, String targetPlayerName);

    /**
     * Async version of {@link #removePlayerFromClaim(Claim, String)}.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> removePlayerFromClaimAsync(Claim claim, String targetPlayerName);

    /**
     * Async version of {@link #banPlayerFromClaim(Claim, String)}.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> banPlayerFromClaimAsync(Claim claim, String targetPlayerName);

    /**
     * Async version of {@link #unbanPlayerFromClaim(Claim, String)}.
     *
     * @param claim the claim
     * @param targetPlayerName the target player name
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> unbanPlayerFromClaimAsync(Claim claim, String targetPlayerName);

    /**
     * Async version of {@link #setClaimPerm(Claim, String, boolean, String)}.
     *
     * @param claim the claim
     * @param permission the permission name
     * @param value the value to set
     * @param role the role
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> setClaimPermAsync(Claim claim, String permission, boolean value, String role);

    /**
     * Async version of {@link #addClaimChunk(Claim, Chunk)}.
     *
     * @param claim the claim
     * @param chunk the chunk to add
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> addClaimChunkAsync(Claim claim, Chunk chunk);

    /**
     * Async version of {@link #removeClaimChunk(Claim, Chunk)}.
     *
     * @param claim the claim
     * @param chunk the chunk to remove
     * @return a future that completes with true if successful
     */
    CompletableFuture<Boolean> removeClaimChunkAsync(Claim claim, Chunk chunk);

    // --- Other Methods ---

    /**
     * Displays the claim map to a player at a specific chunk.
     *
     * @param player the player
     * @param chunk the chunk
     */
    void getMap(Player player, Chunk chunk);

    /**
     * Updates the boss bar display for a player at a specific chunk.
     *
     * @param player the player
     * @param chunk the chunk
     */
    void updateBossBar(Player player, Chunk chunk);

    /**
     * Gets the claim name at a specific chunk.
     *
     * @param chunk the chunk
     * @return the claim name, or null if not claimed
     */
    String getClaimNameAt(Chunk chunk);

    /**
     * Gets the plugin version.
     *
     * @return the version string
     */
    String getVersion();
}
