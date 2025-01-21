package fr.xyness.SCS.Support;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

import com.flowpowered.math.vector.Vector2d;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;

/**
 * This class integrates claims with the BlueMap plugin, allowing claims to be displayed as markers on the BlueMap.
 */
public class ClaimBluemap {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	/** The BlueMap API instance. */
	private BlueMapAPI api;
	
	/** A map storing the MarkerSets for each world. */
	private Map<World, MarkerSet> markerSets = new HashMap<>();
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
	
    
	// ******************
	// *  Constructors  *
	// ******************

    
	/**
	 * Main constructor for the ClaimBluemap class.
	 * Initializes the markers on the BlueMap for all claims in all worlds.
	 *
	 * @param api    the BlueMap API instance.
	 * @param instance the SimpleClaimSystem instance.
	 */
	public ClaimBluemap(BlueMapAPI api, SimpleClaimSystem instance) {
		this.api = api;
		this.instance = instance;
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	/**
	 * Loads the claims into Bluemap
	 */
	public void load() {
		Set<Claim> claims = instance.getMain().getAllClaims();
		instance.executeAsync(() -> {
			for (World w : Bukkit.getWorlds()) {
				MarkerSet markerSet = MarkerSet.builder()
		                .label("Claims")
		                .build();
				markerSets.put(w, markerSet);
				for(Claim claim : claims) {
					if (claim.getLocation().getWorld().equals(w)) {
						createClaimZone(claim);
					}
				}
				api.getWorld(w).ifPresent(world -> {
				    for (BlueMapMap map : world.getMaps()) {
				        map.getMarkerSets().put("Claims", markerSet);
				    }
				});
			}
		});
		instance.getLogger().info("Claims added to BlueMap.");
	}
	
	/**
	 * Creates a marker on the BlueMap for the specified chunks.
	 *
	 * @param claim The claim to create the marker for.
	 */
	public void createClaimZone(Claim claim) {
	    // Get data
	    String name = claim.getName();
	    String owner = claim.getOwner();
	    Set<Chunk> chunks = claim.getChunks();
	    String hoverText = instance.getSettings().getSetting("bluemap-claim-hover-text")
	            .replace("%claim-name%", name)
	            .replace("%owner%", owner);
	    String fcolor = "80" + instance.getSettings().getSetting("bluemap-claim-fill-color");
	    String lcolor = "80" + instance.getSettings().getSetting("bluemap-claim-border-color");
	    Color fillColor = new Color((int) Long.parseLong(fcolor, 16));
	    Color strokeColor = new Color((int) Long.parseLong(lcolor, 16));

	    chunks.parallelStream().forEach(chunk -> {
	    	String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    MarkerSet markerSet = markerSets.get(chunk.getWorld());
		    if (markerSet == null) return;
		    
		    Location loc1 = new Location(chunk.getWorld(), chunk.getX() * 16, 0, chunk.getZ() * 16);
		    Location loc2 = new Location(chunk.getWorld(), (chunk.getX() * 16) + 16, 0, chunk.getZ() * 16);
		    Location loc3 = new Location(chunk.getWorld(), (chunk.getX() * 16) + 16, 0, (chunk.getZ() * 16) + 16);
		    Location loc4 = new Location(chunk.getWorld(), chunk.getX() * 16, 0, (chunk.getZ() * 16) + 16);
		    
		    Shape shape = new Shape(new Vector2d[] {
		    	    new Vector2d(loc1.getX(), loc1.getZ()),
		    	    new Vector2d(loc2.getX(), loc2.getZ()),
		    	    new Vector2d(loc3.getX(), loc3.getZ()),
		    	    new Vector2d(loc4.getX(), loc4.getZ())
		    	});
		    
		    ExtrudeMarker marker = ExtrudeMarker.builder()
		            .label(hoverText)
		            .detail(hoverText)
		            .depthTestEnabled(false)
		            .shape(shape, -64, 320)
		            .position(loc1.getX(), -64, loc1.getZ())
		            .fillColor(fillColor)
		            .lineColor(strokeColor)
		            .lineWidth(5)
		            .build();

		    markerSet.getMarkers().put(markerId, marker);
	    });
	}
	
	/**
	 * Updates the tooltip name of the specified chunks on the BlueMap.
	 *
	 * @param claim The claim to update the name for
	 */
	public void updateName(Claim claim) {
    	String t = instance.getSettings().getSetting("bluemap-claim-hover-text")
    			.replace("%claim-name%", claim.getName())
    			.replace("%owner%", claim.getOwner());
		claim.getChunks().forEach(chunk -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
			MarkerSet markerSet = markerSets.get(chunk.getWorld());
			if (markerSet == null) return;
	    	ExtrudeMarker marker = (ExtrudeMarker) markerSet.get(markerId);
	    	if (marker != null) {
	    		marker.setLabel(t);
	    		marker.setDetail(t);
	    	}
		});
	}
	
	/**
	 * Deletes the marker for the specified chunks from the BlueMap.
	 *
	 * @param chunks The chunks to delete the marker for.
	 */
	public void deleteMarker(Set<Chunk> chunks) {
		chunks.parallelStream().forEach(chunk -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
			MarkerSet markerSet = markerSets.get(chunk.getWorld());
			if (markerSet == null) return;
			markerSet.remove(markerId);
		});
	}
	
}
