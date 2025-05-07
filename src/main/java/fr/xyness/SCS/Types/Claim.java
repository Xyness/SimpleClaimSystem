package fr.xyness.SCS.Types;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import fr.xyness.SCS.Zone;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.sql.DataSource;

/**
 * This class handles claim object.
 */
public class Claim {

	
    // ***************
    // *  Variables  *
    // ***************
    
	
	/** The id associated with this claim */
	protected int id;
	
	/** The UUID of the owner */
    protected UUID uuid_owner;
	
    /** The chunks associated with this claim */
    private Set<Chunk> chunks;
    
    /** The owner of the claim */
    protected String owner;
    
    /** Members who have access to the claim */
    protected Set<UUID> members;

    private Map<String, Zone> zones = new ConcurrentHashMap<>();
    
    /** Location of the claim */
    private Location location;
    
    /** Name of the claim */
    protected String name;
    
    /** Description of the claim */
    protected String description;
    
    /** Permissions associated with the claim */
    protected Map<String,LinkedHashMap<String, Boolean>> permissions;
    
    /** Whether the claim is for sale */
    protected boolean sale;
    
    /** Price of the claim if for sale */
    protected long price;
    
    /** Banned members from the claim */
    protected Set<UUID> bans;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    public Claim(Boolean isZone) {
        // A Boolean argument was added since Claim() runs regardless of whether the subclass calls super() explicitly,
        //   and we don't want Claim attributes set to null for anything except a Zone.
        if (!isZone) {
            // throw new UnsupportedOperationException(
            System.err.println("ERROR: Default Claim constructor sets Claim values to null" +
                    " (So it should not be called from anywhere other than the `Zone` constructor, like: `super(true)`).");
            return;
        }
        this.chunks = null;
        this.zones = null;
        this.location = null;
    }
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
     * Sets the chunks for this claim.
     * 
     * @param chunks The new chunks
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


    /**
     * Put the zone into zones if the name isn't used yet.
     * @param zone A newly created/loaded zone with "name" set to a unique value (otherwise won't be added)
     * @return true if added to zones, false if zoneName already in zones (no change occurred)
     */
    public boolean putNewZone(Zone zone) {
        if (zones.containsKey(zone.getName())) return false;
        zones.put(zone.getName(), zone);
        return true;
    }

    /**
     * Add a zone, changing its name if necessary to make it unique to this claim.
     * (No database change is made. Use appropriate dbUpdate* method of {@link Zone} if name changes).
     *
     * @param zone Any zone.
     * @param prefix String to place before the name *only if* name is already used (Can be blank. A unique numbered
     *               name will be generated if there is no unique result using prefix+name).
     * @return The given zone, with the name changed if it wasn't unique to the claim.
     */
    public Zone mergeAsUniqueName(Zone zone, String prefix) {
        String originalName = zone.getName();
        String newName = zone.getName();
        int num = 0;
        String tryName = prefix + originalName;
        while (zones.containsKey(newName)) {
            num++;
            if (num == 1) {
                newName = tryName;
            }
            else {
                newName = tryName + Integer.toString(num);
            }
        }
        zone.name = newName;
        zones.put(newName, zone);
        return zone;
    }

    public void clearZones() {
        zones.clear();
    }

    public Zone removeZone(String zoneName) {
        return zones.remove(zoneName);
    }


    // Getters

    public Zone getZoneById(Integer zoneID) {
        for (Map.Entry<String, Zone> entry : zones.entrySet()) {
            Zone zone = entry.getValue();
            if (zone.getId() == zoneID) {
                return zone;
            }
        }
        return null;
    }
    public Zone getZoneAt(Location location) {
        for (Map.Entry<String, Zone> entry : zones.entrySet()) {
            Zone zone = entry.getValue();
            if (zone.contains(location)) {
                return zone;
            }
        }
        return null;
    }
    /**
     * Set the zone ID for the player's GUI session
     * @param player whose location to use for finding a zone.
     * @return
     */
    public Zone getZoneAt(Player player) {
        return getZoneAt(player.getLocation());
    }

    public int getZoneId(String zoneName) {
        Zone zone = zones.get(zoneName);
        if (zone == null) return -1;
        return zone.getId();
    }

    public Zone getZone(String zoneName) { return zones.get(zoneName); }

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

    public String getMembersString() {
        return getMembers().stream()
                .map(UUID::toString)
                .collect(Collectors.joining(";"));
    }
    
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
     * Gets a specific permission for a player associated with this claim
     * (If player is in a zone, the zone's permissions are used instead).
     * 
     * @param permission The permission key
     * @param player The target player
     * @return The permission value
     */
    public boolean getPermissionForPlayer(String permission, Player player) {
    	if (this.owner.equals(player.getName()) && !permission.equalsIgnoreCase("weather")) return true;
        Zone zone = getZoneAt(player.getLocation());
        if (zone != null) {
            zone.permissions.getOrDefault(isMember(player.getUniqueId()) ? "members" : "visitors", new LinkedHashMap<>()).getOrDefault(permission, false);
        }
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
     * @param ban The player to ban
     */
    public void addBan(UUID ban) { this.bans.add(ban); }
    
    /**
     * Removes a player from the banned list of this claim.
     * 
     * @param ban The player to unban
     */
    public void removeBan(UUID ban) { this.bans.remove(ban); }
    
    /**
     * Adds a chunk to the claim.
     * 
     * @param chunk The chunk to add
     */
    public void addChunk(Chunk chunk) { this.chunks.add(chunk); }
    
    /**
     * Adds chunks to the claim
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

    public Map<String, Zone> getZones() {
        return zones;
    }

    /**
     *Delete zones from *this* claim from the database.
     * @param datasource     Such as instance.getDataSource() (HikariDataSource or other that implements
     *                       javax.sql.DataSource and java.io.CLoseable)
     */
    public void dbDeleteZones(DataSource datasource) {
        // Update database
        try (Connection connection = datasource.getConnection()) {
            String deleteZonesQuery = "DELETE FROM scs_zones WHERE parent_claim_id = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteZonesQuery)) {
                preparedStatement.setInt(1, getId());  // *all* zones with this parent claim id
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(String.format("Failed to delete zones with parent_claim_id: %s", getId()));
            e.printStackTrace();
        }
    }
    /**
     *Delete named Zone from *this* claim from the database.
     * @param datasource     Such as instance.getDataSource() (HikariDataSource or other that implements
     *                       javax.sql.DataSource and java.io.CLoseable)
     */
    public void dbDeleteZone(DataSource datasource, String zoneName) {
        // Update database
        try (Connection connection = datasource.getConnection()) {
            String deleteZonesQuery = "DELETE FROM scs_zones WHERE parent_claim_id = ? AND name = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(deleteZonesQuery)) {
                preparedStatement.setInt(1, getId());
                preparedStatement.setString(2, zoneName);
                preparedStatement.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println(String.format("Failed to delete zones with parent_claim_id: %s", getId()));
            e.printStackTrace();
            return;
        }
    }
}
