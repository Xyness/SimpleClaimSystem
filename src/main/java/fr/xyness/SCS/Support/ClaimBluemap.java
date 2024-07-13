package fr.xyness.SCS.Support;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.flowpowered.math.vector.Vector2d;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimSettings;
import net.pl3x.map.core.util.Colors;

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
		Set<Chunk> claims = instance.getMain().getAllClaimsChunk();
		instance.executeAsync(() -> {
			for (World w : Bukkit.getWorlds()) {
				MarkerSet markerSet = MarkerSet.builder()
		                .label("Claims")
		                .build();
				markerSets.put(w, markerSet);
	   			for (Chunk c : claims) {
					Claim claim = instance.getMain().getClaimFromChunk(c);
					if (claim == null) continue;
					if (!claim.getLocation().getWorld().equals(w)) continue;
					createChunkZone(Set.of(c), claim.getName(), claim.getOwner());
				}
				api.getWorld(w).ifPresent(world -> {
				    for (BlueMapMap map : world.getMaps()) {
				        map.getMarkerSets().put("Claims", markerSet);
				    }
				});
			}
		});
		instance.getPlugin().getLogger().info("Claims added to BlueMap.");
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	/**
	 * Creates a marker on the BlueMap for the specified chunks.
	 *
	 * @param chunks the chunks to create the marker for.
	 * @param name  the name of the claim.
	 * @param owner the owner of the claim.
	 */
	public void createChunkZone(Set<Chunk> chunks, String name, String owner) {
	    String hoverText = instance.getSettings().getSetting("bluemap-claim-hover-text")
	            .replaceAll("%claim-name%", name)
	            .replaceAll("%owner%", owner);
	    
	    String fcolor = "80"+instance.getSettings().getSetting("bluemap-claim-fill-color");
	    String lcolor = "80"+instance.getSettings().getSetting("bluemap-claim-border-color");
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
		    
		    Shape shape = new Shape(Arrays.asList(
		            new Vector2d(loc1.getX(), loc1.getZ()),
		            new Vector2d(loc2.getX(), loc2.getZ()),
		            new Vector2d(loc3.getX(), loc3.getZ()),
		            new Vector2d(loc4.getX(), loc4.getZ())
		    ));
		    
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
	 * @param chunks the chunks to update the name for.
	 * @param claim the claim for chunks
	 */
	public void updateName(Set<Chunk> chunks, Claim claim) {
    	String t = instance.getSettings().getSetting("bluemap-hover-text")
    			.replaceAll("%claim-name%", claim.getName())
    			.replaceAll("%owner%", claim.getOwner());
		chunks.parallelStream().forEach(chunk -> {
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
	 * Deletes the marker for the specified chunk from the BlueMap.
	 *
	 * @param chunks the chunks to delete the marker for.
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
