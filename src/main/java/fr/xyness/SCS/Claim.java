package fr.xyness.SCS;

import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class Claim {

    // ***************
    // *  Variables  *
    // ***************
    
    /** The chunk associated with this claim */
    private Chunk chunk;
    
    /** The owner of the claim */
    private String owner;
    
    /** Members who have access to the claim */
    private Set<String> members;
    
    /** Location of the claim */
    private Location location;
    
    /** Name of the claim */
    private String name;
    
    /** Description of the claim */
    private String description;
    
    /** Permissions associated with the claim */
    private LinkedHashMap<String, Boolean> permissions;
    
    /** Whether the claim is for sale */
    private boolean sale;
    
    /** Price of the claim if for sale */
    private Double price;
    
    /** Banned members from the claim */
    private Set<String> bans;
    
    // ******************
    // *  Constructors  *
    // ******************
    
    /**
     * Main constructor initializing all fields.
     * 
     * @param chunk The chunk associated with this claim
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
    public Claim(Chunk chunk, String owner, Set<String> members, Location location, String name, String description, LinkedHashMap<String, Boolean> permissions, boolean sale, Double price, Set<String> bans) {
        this.chunk = chunk;
        this.owner = owner;
        this.members = new HashSet<>(members);
        this.location = location;
        this.name = name;
        this.description = description;
        this.permissions = new LinkedHashMap<>(permissions);
        this.sale = sale;
        this.price = price;
        this.bans = new HashSet<>(bans);
    }
    
    // ********************
    // *  Other Methods   *
    // ********************
    
    // Setters
    
    /**
     * Sets the chunk for this claim.
     * 
     * @param chunk The new chunk
     */
    public void setChunk(Chunk chunk) { this.chunk = chunk; }
    
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
    public void setMembers(Set<String> members) { this.members = members; }
    
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
    public void setPermissions(LinkedHashMap<String, Boolean> permissions) { this.permissions = permissions; }
    
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
    public void setPrice(Double price) { this.price = price; }
    
    /**
     * Sets the members who are banned from this claim.
     * 
     * @param bans The new set of banned members
     */
    public void setBans(Set<String> bans) { this.bans = bans; }
    
    // Getters
    
    /**
     * Gets the chunk associated with this claim.
     * 
     * @return The chunk
     */
    public Chunk getChunk() { return this.chunk; }
    
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
    public Set<String> getMembers() { return this.members; }
    
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
    public LinkedHashMap<String, Boolean> getPermissions() { return this.permissions; }
    
    /**
     * Gets a specific permission associated with this claim.
     * 
     * @param permission The permission key
     * @return The permission value
     */
    public boolean getPermission(String permission) { return this.permissions.getOrDefault(permission, false); }
    
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
    public Double getPrice() { return this.price; }
    
    /**
     * Gets the members who are banned from this claim.
     * 
     * @return The banned members
     */
    public Set<String> getBans() { return this.bans; }
    
    // Modifications
    
    /**
     * Updates a specific permission for this claim.
     * 
     * @param permission The permission key
     * @param value The new permission value
     */
    public void updatePermission(String permission, Boolean value) { this.permissions.put(permission, value); }
    
    /**
     * Adds a member to the claim.
     * 
     * @param member The member to add
     */
    public void addMember(String member) { this.members.add(member); }
    
    /**
     * Removes a member from the claim.
     * 
     * @param member The member to remove
     */
    public void removeMember(String member) {
        SimpleClaimSystem.executeAsync(() -> {
        	Iterator<String> iterator = this.members.iterator();
            while (iterator.hasNext()) {
                String currentString = iterator.next();
                if (currentString.equalsIgnoreCase(member)) {
                    iterator.remove();
                }
            }
        });
    }
    
    /**
     * Adds a member to the banned list of this claim.
     * 
     * @param member The member to ban
     */
    public void addBan(String member) { this.bans.add(member); }
    
    /**
     * Removes a member from the banned list of this claim.
     * 
     * @param member The member to unban
     */
    public void removeBan(String member) {
    	SimpleClaimSystem.executeAsync(() -> {
            Iterator<String> iterator = this.bans.iterator();
            while (iterator.hasNext()) {
                String currentString = iterator.next();
                if (currentString.equalsIgnoreCase(member)) {
                    iterator.remove();
                }
            }
    	});
    }
}
