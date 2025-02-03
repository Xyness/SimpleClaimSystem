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

	
    // ***************
    // *  Variables  *
    // ***************
    
	
	/** The id associated with this claim */
	private int id;
	
	/** The UUID of the owner */
	private UUID uuid_owner;
	
    /** The chunks associated with this claim */
    private Set<Chunk> chunks;
    
    /** The owner of the claim */
    private String owner;
    
    /** Members who have access to the claim */
    private Set<UUID> members;
    
    /** Location of the claim */
    private Location location;
    
    /** Name of the claim */
    private String name;
    
    /** Description of the claim */
    private String description;
    
    /** Permissions associated with the claim */
    private Map<String,LinkedHashMap<String, Boolean>> permissions;
    
    /** Whether the claim is for sale */
    private boolean sale;
    
    /** Price of the claim if for sale */
    private long price;
    
    /** Banned members from the claim */
    private Set<UUID> bans;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Main constructor initializing all fields.
     * 
     * @param uuid_owner The UUID of the owner
     * @param chunks The chunks associated with this claim
     * @param owner The owner of the claim
     * @param members Members who have access to the claim
     * @param location Location of the claim
     * @param name Name of the claim
     * @param description Description of the claim
     * @param permissions Permissions associated with the claim
     * @param sale Whether the claim is for sale
     * @param price Price of the claim if for sale
     * @param bans Banned members from the claim
     */
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
    
    
    // *********************
    // *  Others Methods   *
    // *********************

    
    // Setters
    
    /**
     * Sets the id for this claim
     * 
     * @param id The new id
     */
    public void setId(int id) { this.id = id; }
    
    /**
     * Sets the UUID of the owner
     * 
     * @param uuid_owner The new UUID
     */
    public void setUUID(UUID uuid_owner) { this.uuid_owner = uuid_owner; }
    
    /**
     * Sets the chunk for this claim.
     * 
     * @param chunk The new chunk
     */
    public void setChunks(Set<Chunk> chunks) { this.chunks = chunks; }
    
    /**
     * Sets the owner of this claim.
     * 
     * @param owner The new owner
     */
    public void setOwner(String owner) { this.owner = owner; }
    
    /**
     * Sets the members who have access to this claim.
     * 
     * @param members The new set of members
     */
    public void setMembers(Set<UUID> members) { this.members = members; }
    
    /**
     * Sets the location of this claim.
     * 
     * @param location The new location
     */
    public void setLocation(Location location) { this.location = location; }
    
    /**
     * Sets the name of this claim.
     * 
     * @param name The new name
     */
    public void setName(String name) { this.name = name; }
    
    /**
     * Sets the description of this claim.
     * 
     * @param description The new description
     */
    public void setDescription(String description) { this.description = description; }
    
    /**
     * Sets the permissions associated with this claim.
     * 
     * @param permissions The new permissions
     */
    public void setPermissions(Map<String,LinkedHashMap<String, Boolean>> permissions) { this.permissions = permissions; }
    
    /**
     * Sets whether this claim is for sale.
     * 
     * @param sale The new sale status
     */
    public void setSale(boolean sale) { this.sale = sale; }
    
    /**
     * Sets the price of this claim if for sale.
     * 
     * @param price The new price
     */
    public void setPrice(long price) { this.price = price; }
    
    /**
     * Sets the members who are banned from this claim.
     * 
     * @param bans The new set of banned members
     */
    public void setBans(Set<UUID> bans) { this.bans = bans; }
    
    // Getters
    
    /**
     * Gets the id associated with this claim
     * 
     * @return The id
     */
    public int getId() { return this.id; }
    
    /**
     * Gets the UUID of the owner
     * 
     * @return The id
     */
    public UUID getUUID() { return this.uuid_owner; }
    
    /**
     * Gets the chunk associated with this claim.
     * 
     * @return The chunk
     */
    public Set<Chunk> getChunks() { return this.chunks; }
    
    /**
     * Gets the owner of this claim.
     * 
     * @return The owner
     */
    public String getOwner() { return this.owner; }
    
    /**
     * Gets the members who have access to this claim.
     * 
     * @return The members
     */
    public Set<UUID> getMembers() { return this.members; }
    
    /**
     * Gets the location of this claim.
     * 
     * @return The location
     */
    public Location getLocation() { return this.location; }
    
    /**
     * Gets the name of this claim.
     * 
     * @return The name
     */
    public String getName() { return this.name; }
    
    /**
     * Gets the description of this claim.
     * 
     * @return The description
     */
    public String getDescription() { return this.description; }
    
    /**
     * Gets the permissions associated with this claim.
     * 
     * @return The permissions
     */
    public Map<String,LinkedHashMap<String, Boolean>> getPermissions() { return this.permissions; }
    
    /**
     * Gets a specific permission associated with this claim.
     * 
     * @param permission The permission key
     * @param role The role of the player, can be null
     * @return The permission value
     */
    public boolean getPermission(String permission, String role) {
    	return this.permissions.getOrDefault(role == null ? "natural" : role.toLowerCase(), new LinkedHashMap<>()).getOrDefault(permission, false);
    }
    
    /**
     * Gets a specific permission for a player associated with this claim.
     * 
     * @param permission The permission key
     * @param player The target player
     * @return The permission value
     */
    public boolean getPermissionForPlayer(String permission, Player player) {
    	if (this.owner.equals(player.getName()) && !permission.equalsIgnoreCase("weather")) return true;
    	return this.permissions.getOrDefault(isMember(player.getUniqueId()) ? "members" : "visitors", new LinkedHashMap<>()).getOrDefault(permission, false);
    }
    
    /**
     * Gets whether this claim is for sale.
     * 
     * @return The sale status
     */
    public boolean getSale() { return this.sale; }
    
    /**
     * Gets the price of this claim if for sale.
     * 
     * @return The price
     */
    public long getPrice() { return this.price; }
    
    /**
     * Gets the members who are banned from this claim.
     * 
     * @return The banned members
     */
    public Set<UUID> getBans() { return this.bans; }
    
    // Modifications
    
    /**
     * Updates a specific permission for this claim.
     * 
     * @param permission The permission key
     * @param value The new permission value
     */
    public void updatePermission(String role, String permission, Boolean value) {
    	this.permissions.getOrDefault(role == null ? "natural" : role, new LinkedHashMap<>()).put(permission, value);
    }
    
    /**
     * Adds a member to the claim.
     * 
     * @param member The member to add
     */
    public void addMember(UUID member) { this.members.add(member); }
    
    /**
     * Removes a member from the claim.
     * 
     * @param member The member to remove
     */
    public void removeMember(UUID member) { this.members.remove(member); }
    
    /**
     * Adds a player to the banned list of this claim.
     * 
     * @param member The player to ban
     */
    public void addBan(UUID ban) { this.bans.add(ban); }
    
    /**
     * Removes a player from the banned list of this claim.
     * 
     * @param member The player to unban
     */
    public void removeBan(UUID ban) { this.bans.remove(ban); }
    
    /**
     * Adds a chunk to the claim.
     * 
     * @param chunk The chunk to add
     */
    public void addChunk(Chunk chunk) { this.chunks.add(chunk); }
    
    /**
     * Adds chunks to the claim.
     * 
     * @param chunks The chunks to add
     */
    public void addChunks(Set<Chunk> chunks) { 
        if (!(this.chunks instanceof HashSet)) {
            this.chunks = new HashSet<>(this.chunks);
        }
    	this.chunks.addAll(chunks); 
    }
    
    /**
     * Checks if a player is banned.
     *
     * @param targetUUID the uuid of the player to check
     * @return True if the player is banned, False otherwise
     */
    public boolean isBanned(UUID targetUUID) {
        return this.bans.contains(targetUUID);
    }

    /**
     * Checks if a player is a member.
     *
     * @param targetUUID the uuid of the player to check
     * @return True if the player is a member, False otherwise
     */
    public boolean isMember(UUID targetUUID) {
        return this.members.contains(targetUUID);
    }

}
