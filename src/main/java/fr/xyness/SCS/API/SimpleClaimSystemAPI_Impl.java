package fr.xyness.SCS.API;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;

/**
 * Implementation of the SimpleClaimSystemAPI interface.
 * Provides methods to interact with the claim system and player data.
 */
public class SimpleClaimSystemAPI_Impl implements SimpleClaimSystemAPI {

    private SimpleClaimSystem instance;

    public SimpleClaimSystemAPI_Impl(SimpleClaimSystem instance) {
        this.instance = instance;
    }

    // --- Player Methods ---

    @Override
    public Claim getPlayerClaim(Player player, String claimName) {
        return instance.getMain().getClaimByName(claimName, player);
    }

    @Override
    public Set<Claim> getPlayerClaims(Player player) {
        return instance.getMain().getPlayerClaims(player.getUniqueId());
    }

    @Override
    public Set<Claim> getPlayerClaimsWhereMember(Player player) {
        return instance.getMain().getClaimsWhereMemberNotOwner(player);
    }

    @Override
    public CPlayer getCPlayer(String playerName) {
        UUID uuid = instance.getPlayerMain().getPlayerUUID(playerName);
        if (uuid == null) return null;
        return instance.getPlayerMain().getCPlayer(uuid);
    }

    @Override
    public CPlayer getCPlayer(Player player) {
        return instance.getPlayerMain().getCPlayer(player.getUniqueId());
    }

    @Override
    public int getPlayerClaimsCount(UUID playerId) {
        return instance.getMain().getPlayerClaimsCount(playerId);
    }

    @Override
    public boolean isClaimMember(Claim claim, Player player) {
        return instance.getMain().checkMembre(claim, player);
    }

    @Override
    public boolean isClaimBanned(Claim claim, Player player) {
        return instance.getMain().checkBan(claim, player);
    }

    @Override
    public String getPlayerRelation(Player player, Chunk chunk) {
        return instance.getMain().getRelation(player, chunk);
    }

    // --- Claim Methods ---

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
    public Set<Claim> getAllClaims() {
        return instance.getMain().getAllClaims();
    }

    @Override
    public Set<Claim> getClaims(World targetWorld) {
        return instance.getMain().getAllClaims().parallelStream()
                .filter(claim -> claim.getLocation().getWorld().equals(targetWorld))
                .collect(Collectors.toSet());
    }

    @Override
    public int getAllClaimsCount() {
        return instance.getMain().getAllClaimsCount();
    }

    @Override
    public Set<Claim> getProtectedAreas() {
        return instance.getMain().getProtectedAreas();
    }

    @Override
    public int getProtectedAreasCount() {
        return instance.getMain().getProtectedAreasCount();
    }

    @Override
    public Claim getClaimByName(String claimName, UUID ownerUUID) {
        return instance.getMain().getClaimByName(claimName, ownerUUID);
    }

    @Override
    public String getClaimOwnerAt(Chunk chunk) {
        return instance.getMain().getOwnerInClaim(chunk);
    }

    @Override
    public Set<Chunk> getAllChunksFromOwner(String owner) {
        return instance.getMain().getAllChunksFromAllClaims(owner);
    }

    @Override
    public boolean isAreaClaimFree(Chunk centerChunk, int distance, String playerName) {
        return instance.getMain().isAreaClaimFreeSync(centerChunk, distance, playerName);
    }

    @Override
    public boolean areChunksConnected(Set<Chunk> chunks) {
        return instance.getMain().areChunksConnected(chunks);
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
        return instance.getMain().resetAllPlayerClaimsSettings().join();
    }

    @Override
    public boolean mergeMultipleClaims(Claim mainClaim, CustomSet<Claim> claimsToMerge) {
        return instance.getMain().mergeClaims(mainClaim, claimsToMerge).join();
    }

    @Override
    public void kickPlayerFromClaim(Claim claim, String targetPlayerName) {
        Player target = Bukkit.getPlayer(targetPlayerName);
        if (target != null && target.isOnline()) {
            if (claim.getChunks().contains(target.getLocation().getChunk())) {
                List<World> worlds = Bukkit.getWorlds();
                if (!worlds.isEmpty()) {
                    instance.getMain().teleportPlayer(target, worlds.get(0).getSpawnLocation());
                }
            }
        }
    }

    @Override
    public void kickPlayerFromAllClaims(String owner, String targetPlayerName) {
        Player target = Bukkit.getPlayer(targetPlayerName);
        if (target != null && target.isOnline()) {
            Set<Chunk> chunks = instance.getMain().getAllChunksFromAllClaims(owner);
            if (chunks.contains(target.getLocation().getChunk())) {
                List<World> worlds = Bukkit.getWorlds();
                if (!worlds.isEmpty()) {
                    instance.getMain().teleportPlayer(target, worlds.get(0).getSpawnLocation());
                }
            }
        }
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
    public boolean setClaimPerm(Claim claim, String permission, boolean value, String role) {
        return instance.getMain().updatePerm(claim, permission, value, role).join();
    }

    @Override
    public boolean checkClaimPermission(Chunk chunk, String permission, String role) {
        return instance.getMain().canPermCheck(chunk, permission, role);
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
    public boolean addClaimSale(Claim claim, long claimPrice) {
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
        return instance.getMain().removeClaimChunk(claim, chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ()).join();
    }

    // --- Async Claims Methods ---

    @Override
    public CompletableFuture<Boolean> unclaimAsync(Claim claim) {
        return instance.getMain().deleteClaim(claim);
    }

    @Override
    public CompletableFuture<Boolean> unclaimAllAsync(String owner) {
        return instance.getMain().deleteAllClaims(owner);
    }

    @Override
    public CompletableFuture<Boolean> setClaimNameAsync(Claim claim, String newName) {
        return instance.getMain().setClaimName(claim, newName);
    }

    @Override
    public CompletableFuture<Boolean> setClaimDescriptionAsync(Claim claim, String newDesc) {
        return instance.getMain().setClaimDescription(claim, newDesc);
    }

    @Override
    public CompletableFuture<Boolean> setClaimOwnerAsync(Claim claim, String newOwner) {
        return instance.getMain().setOwner(newOwner, claim);
    }

    @Override
    public CompletableFuture<Boolean> addPlayerToClaimAsync(Claim claim, String targetPlayerName) {
        return instance.getMain().addClaimMember(claim, targetPlayerName);
    }

    @Override
    public CompletableFuture<Boolean> removePlayerFromClaimAsync(Claim claim, String targetPlayerName) {
        return instance.getMain().removeClaimMember(claim, targetPlayerName);
    }

    @Override
    public CompletableFuture<Boolean> banPlayerFromClaimAsync(Claim claim, String targetPlayerName) {
        return instance.getMain().addClaimBan(claim, targetPlayerName);
    }

    @Override
    public CompletableFuture<Boolean> unbanPlayerFromClaimAsync(Claim claim, String targetPlayerName) {
        return instance.getMain().removeClaimBan(claim, targetPlayerName);
    }

    @Override
    public CompletableFuture<Boolean> setClaimPermAsync(Claim claim, String permission, boolean value, String role) {
        return instance.getMain().updatePerm(claim, permission, value, role);
    }

    @Override
    public CompletableFuture<Boolean> addClaimChunkAsync(Claim claim, Chunk chunk) {
        return instance.getMain().addClaimChunk(claim, chunk);
    }

    @Override
    public CompletableFuture<Boolean> removeClaimChunkAsync(Claim claim, Chunk chunk) {
        return instance.getMain().removeClaimChunk(claim, chunk.getWorld().getName() + ";" + chunk.getX() + ";" + chunk.getZ());
    }

    // --- Other Methods ---

    @Override
    public void getMap(Player player, Chunk chunk) {
        instance.getMain().getMap(player, chunk, false);
    }

    @Override
    public void updateBossBar(Player player, Chunk chunk) {
        instance.getBossBars().activeBossBar(player, chunk);
    }

    @Override
    public String getClaimNameAt(Chunk chunk) {
        return instance.getMain().getClaimNameByChunk(chunk);
    }

    @Override
    public String getVersion() {
        return instance.getDescription().getVersion();
    }
}
