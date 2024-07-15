package fr.xyness.SCS.API;

import java.util.Set;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;

/**
 * Implementation of the SimpleClaimSystemAPI interface.
 * Provides methods to interact with the claim system and player data.
 */
public class SimpleClaimSystemAPI_Impl implements SimpleClaimSystemAPI {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Instance of SimpleClaimSystem */
	private SimpleClaimSystem instance;
	
	
    // ******************
    // *  Constructors  *
    // ******************
	
	
    /**
     * Constructs a new SimpleClaimSystemAPI_Impl.
     *
     * @param instance the instance of the SimpleClaimSystem
     */
	public SimpleClaimSystemAPI_Impl(SimpleClaimSystem instance) {
		this.instance = instance;
	}
	
	
    // *******************
    // *  Other Methods  *
    // *******************
	

	// Player methods
	
	@Override
	public Claim getPlayerClaim(String playerName, String claimName) {
		return instance.getMain().getClaimByName(claimName, playerName);
	}

	@Override
	public Claim getPlayerClaim(Player player, String claimName) {
		return instance.getMain().getClaimByName(claimName, player.getName());
	}

	@Override
	public CPlayer getCPlayer(String playerName) {
		return instance.getPlayerMain().getCPlayer(playerName);
	}

	@Override
	public CPlayer getCPlayer(Player player) {
		return instance.getPlayerMain().getCPlayer(player.getName());
	}
	
	// Claim methods

	@Override
	public boolean unclaim(Claim claim) {
		return instance.getMain().deleteClaim(claim).join();
	}

	@Override
	public boolean unclaimAll(String owner) {
		return instance.getMain().deleteAllClaims(owner).join();
	}

	@Override
	public boolean isClaimed(Chunk chunk) {
		return instance.getMain().checkIfClaimExists(chunk);
	}

	@Override
	public Claim getClaimAtChunk(Chunk chunk) {
		return instance.getMain().getClaim(chunk);
	}
	
	@Override
	public Set<Claim> getAllClaims(){
		return instance.getMain().getAllClaims();
	}
	
	@Override
	public Set<Claim> getClaims(World targetWorld){
		return instance.getMain().getAllClaims().parallelStream()
				.filter(claim -> claim.getLocation().getWorld().equals(targetWorld))
				.collect(Collectors.toSet());
	}

	@Override
	public boolean applySettingsToAllClaims(Claim claim) {
		return instance.getMain().applyAllSettings(claim).join();
	}

	@Override
	public boolean resetClaimsSettings(String owner) {
		return instance.getMain().resetAllOwnerClaimsSettings(owner).join();
	}

	@Override
	public boolean resetAllClaimsSettings() {
		return instance.getMain().resetAllClaimsSettings().join();
	}

	@Override
	public boolean mergeMultipleClaims(Claim mainClaim, Set<Claim> claimsToMerge) {
		return instance.getMain().mergeClaims(mainClaim, claimsToMerge).join();
	}

	@Override
	public boolean banPlayerFromClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().addClaimBan(claim, targetPlayerName).join();
	}

	@Override
	public boolean unbanPlayerFromClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().removeClaimBan(claim, targetPlayerName).join();
	}

	@Override
	public boolean banPlayerFromAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().addAllClaimBan(owner, targetPlayerName).join();
	}

	@Override
	public boolean unbanPlayerFromAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().removeAllClaimBan(owner, targetPlayerName).join();
	}

	@Override
	public boolean addPlayerToClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().addClaimMember(claim, targetPlayerName).join();
	}

	@Override
	public boolean removePlayerFromClaim(Claim claim, String targetPlayerName) {
		return instance.getMain().removeClaimMember(claim, targetPlayerName).join();
	}

	@Override
	public boolean addPlayerToAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().addAllClaimsMember(owner, targetPlayerName).join();
	}

	@Override
	public boolean removePlayerFromAllClaims(String owner, String targetPlayerName) {
		return instance.getMain().removeAllClaimsMember(owner, targetPlayerName).join();
	}

	@Override
	public boolean resetClaimPerm(Claim claim) {
		return instance.getMain().resetClaimSettings(claim).join();
	}

	@Override
	public boolean setClaimPerm(Claim claim, String permission, boolean value) {
		return instance.getMain().updatePerm(claim, permission, value).join();
	}

	@Override
	public boolean setClaimName(Claim claim, String newName) {
		return instance.getMain().setClaimName(claim, newName).join();
	}

	@Override
	public boolean setClaimLocation(Claim claim, Location newLoc) {
		return instance.getMain().setClaimLocation(claim, newLoc).join();
	}

	@Override
	public boolean setClaimDescription(Claim claim, String newDesc) {
		return instance.getMain().setClaimDescription(claim, newDesc).join();
	}

	@Override
	public boolean addClaimSale(Claim claim, double claimPrice) {
		return instance.getMain().setChunkSale(claim, claimPrice).join();
	}

	@Override
	public boolean removeClaimSale(Claim claim) {
		return instance.getMain().delChunkSale(claim).join();
	}

	@Override
	public boolean setClaimOwner(Claim claim, String newOwner) {
		return instance.getMain().setOwner(newOwner, claim).join();
	}

	@Override
	public boolean addClaimChunk(Claim claim, Chunk chunk) {
		return instance.getMain().addClaimChunk(claim, chunk).join();
	}

	@Override
	public boolean removeClaimChunk(Claim claim, Chunk chunk) {
		return instance.getMain().removeClaimChunk(claim, String.valueOf(chunk.getWorld().getName()+";"+chunk.getX()+";"+chunk.getZ())).join();
	}
	
	// Other methods

	@Override
	public void getMap(Player player, Chunk chunk) {
		instance.getMain().getMap(player, chunk);
	}
	
}
