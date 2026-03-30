package fr.xyness.SCS.Types;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * This class handles claim object.
 */
public class Claim {

	private int id;
	private UUID uuid_owner;
    private Set<Chunk> chunks;
    private String owner;
    private Set<UUID> members;
    private Location location;
    private String name;
    private String description;
    private Map<String,LinkedHashMap<String, Boolean>> permissions;
    private boolean sale;
    private long price;
    private Set<UUID> bans;

    public Claim(UUID uuid_owner, Set<Chunk> chunks, String owner, Set<UUID> members, Location location, String name, String description, Map<String,LinkedHashMap<String, Boolean>> permissions, boolean sale, long price, Set<UUID> bans, int id) {
    	this.uuid_owner = uuid_owner;
    	this.chunks = chunks;
        this.owner = owner;
        this.members = new HashSet<>(members);
        this.location = location;
        this.name = name;
        this.description = description;
        this.permissions = new HashMap<>(permissions);
        this.sale = sale;
        this.price = price;
        this.bans = new HashSet<>(bans);
        this.id = id;
    }

    // --- Setters ---

    public void setId(int id) { this.id = id; }
    public void setUUID(UUID uuid_owner) { this.uuid_owner = uuid_owner; }
    public void setChunks(Set<Chunk> chunks) { this.chunks = chunks; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setMembers(Set<UUID> members) { this.members = members; }
    public void setLocation(Location location) { this.location = location; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setPermissions(Map<String,LinkedHashMap<String, Boolean>> permissions) { this.permissions = permissions; }
    public void setSale(boolean sale) { this.sale = sale; }
    public void setPrice(long price) { this.price = price; }
    public void setBans(Set<UUID> bans) { this.bans = bans; }

    // --- Getters ---

    public int getId() { return this.id; }
    public UUID getUUID() { return this.uuid_owner; }
    public Set<Chunk> getChunks() { return this.chunks; }
    public String getOwner() { return this.owner; }
    public Set<UUID> getMembers() { return this.members; }
    public Location getLocation() { return this.location; }
    public String getName() { return this.name; }
    public String getDescription() { return this.description; }
    public Map<String,LinkedHashMap<String, Boolean>> getPermissions() { return this.permissions; }
    public boolean getSale() { return this.sale; }
    public long getPrice() { return this.price; }
    public Set<UUID> getBans() { return this.bans; }

    // --- Permission checks ---

    /**
     * Gets a specific permission for this claim.
     *
     * @param permission the permission key
     * @param role the role of the player, can be null (defaults to "natural")
     * @return the permission value
     */
    public boolean getPermission(String permission, String role) {
    	return this.permissions.getOrDefault(role == null ? "natural" : role.toLowerCase(), new LinkedHashMap<>()).getOrDefault(permission, false);
    }

    /**
     * Gets a specific permission for a player, considering ownership and membership.
     * Owners always have all permissions except "weather".
     *
     * @param permission the permission key
     * @param player the target player
     * @return the permission value
     */
    public boolean getPermissionForPlayer(String permission, Player player) {
    	if (this.owner.equals(player.getName()) && !permission.equalsIgnoreCase("weather")) return true;
    	return this.permissions.getOrDefault(isMember(player.getUniqueId()) ? "members" : "visitors", new LinkedHashMap<>()).getOrDefault(permission, false);
    }

    // --- Modifications ---

    public void updatePermission(String role, String permission, Boolean value) {
    	this.permissions.getOrDefault(role == null ? "natural" : role, new LinkedHashMap<>()).put(permission, value);
    }

    public void addMember(UUID member) { this.members.add(member); }
    public void removeMember(UUID member) { this.members.remove(member); }
    public void addBan(UUID ban) { this.bans.add(ban); }
    public void removeBan(UUID ban) { this.bans.remove(ban); }
    public void addChunk(Chunk chunk) { this.chunks.add(chunk); }

    public void addChunks(Set<Chunk> chunks) {
        if (!(this.chunks instanceof HashSet)) {
            this.chunks = new HashSet<>(this.chunks);
        }
    	this.chunks.addAll(chunks);
    }

    public boolean isBanned(UUID targetUUID) {
        return this.bans.contains(targetUUID);
    }

    public boolean isMember(UUID targetUUID) {
        return this.members.contains(targetUUID);
    }
}
