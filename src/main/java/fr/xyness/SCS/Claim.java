package fr.xyness.SCS;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;

public class Claim {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private Chunk chunk;
	private String owner;
	private Set<String> members;
	private Location location;
	private String name;
	private String description;
	private LinkedHashMap<String, Boolean> permissions;
	private boolean sale;
	private Double price;
	private Set<String> bans;
    
    
	// ******************
	// *  Constructors  *
	// ******************
	
	
	// Main constructor
	public Claim(Chunk chunk, String owner, Set<String> members, Location location, String name, String description, LinkedHashMap<String, Boolean> permissions, boolean sale, Double price, Set<String> bans) {
		this.chunk = chunk;
		this.owner = owner;
		this.members = members;
		this.location = location;
		this.name = name;
		this.description = description;
		this.permissions = permissions;
		this.sale = sale;
		this.price = price;
		this.bans = bans;
	}
    
    
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Setters
	public void setChunk(Chunk chunk) { this.chunk = chunk; }
	public void setOwner(String owner) { this.owner = owner; }
	public void setMembers(Set<String> members) { this.members = members; }
	public void setLocation(Location location) { this.location = location; }
	public void setName(String name) { this.name = name; }
	public void setDescription(String description) { this.description = description; }
	public void setPermissions(LinkedHashMap<String, Boolean> permissions) { this.permissions = permissions; }
	public void setSale(boolean sale) { this.sale = sale; }
	public void setPrice(Double price) { this.price = price; }
	public void setBans(Set<String> bans) { this.bans = bans; }
	
	// Getters
	public Chunk getChunk() { return this.chunk; }
	public String getOwner() { return this.owner; }
	public Set<String> getMembers() { return this.members; }
	public Location getLocation() { return this.location; }
	public String getName() { return this.name; }
	public String getDescription() { return this.description; }
	public LinkedHashMap<String, Boolean> getPermissions() { return this.permissions; }
	public boolean getPermission(String permission) { return this.permissions.getOrDefault(permission, false); }
	public boolean getSale() { return this.sale; }
	public Double getPrice() { return this.price; }
	public Set<String> getBans() { return this.bans; }
	
	// Modifications
	public void updatePermission(String permission, Boolean value) { this.permissions.put(permission, value); }
	public void addMember(String member) { this.members.add(member); }
	public void removeMember(String member) {
        Iterator<String> iterator = this.members.iterator();
        while (iterator.hasNext()) {
            String currentString = iterator.next();
            if (currentString.equalsIgnoreCase(member)) {
                iterator.remove();
            }
        }
	}
	public void addBan(String member) { this.bans.add(member); }
	public void removeBan(String member) {
        Iterator<String> iterator = this.bans.iterator();
        while (iterator.hasNext()) {
            String currentString = iterator.next();
            if (currentString.equalsIgnoreCase(member)) {
                iterator.remove();
            }
        }
	}
	
}
