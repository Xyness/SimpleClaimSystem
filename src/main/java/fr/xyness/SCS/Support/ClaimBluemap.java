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

public class ClaimBluemap {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private BlueMapAPI api;
	private static Map<World,MarkerSet> markerSets = new HashMap<>();
	
    
	// ******************
	// *  Constructors  *
	// ******************

	
	// Main constructor
	public ClaimBluemap(BlueMapAPI api, JavaPlugin plugin) {
		this.api = api;
		Set<Chunk> claims = ClaimMain.getAllClaimsChunk();
		
		for(World w : Bukkit.getWorlds()) {
			MarkerSet markerSet = MarkerSet.builder()
	                .label("Claims ("+w.getName()+")")
	                .build();
			markerSets.put(w, markerSet);
	    	if(SimpleClaimSystem.isFolia()) {
	    		Bukkit.getAsyncScheduler().runNow(plugin, task -> {
	    			for(Chunk c : claims) {
	    				Claim claim = ClaimMain.getClaimFromChunk(c);
	    				if(claim == null) continue;
	    				if(!claim.getLocation().getWorld().equals(w)) continue;
	    				createChunkZone(c,claim.getName(),claim.getOwner());
	    			}
	    			api.getWorld(w).ifPresent(world -> {
	    			    for (BlueMapMap map : world.getMaps()) {
	    			        map.getMarkerSets().put("Claims", markerSet);
	    			    }
	    			});
	    		});
	    	} else {
	    		Bukkit.getScheduler().runTaskAsynchronously(plugin, task -> {
	       			for(Chunk c : claims) {
	    				Claim claim = ClaimMain.getClaimFromChunk(c);
	    				if(claim == null) continue;
	    				if(!claim.getLocation().getWorld().equals(w)) continue;
	    				createChunkZone(c,claim.getName(),claim.getOwner());
	    			}
	    			api.getWorld(w).ifPresent(world -> {
	    			    for (BlueMapMap map : world.getMaps()) {
	    			        map.getMarkerSets().put("Claims", markerSet);
	    			    }
	    			});
	    		});
	    	}
		}
    	plugin.getLogger().info("Claims added to BlueMap.");
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to create the chunk area on the bluemap
	public static void createChunkZone(Chunk chunk, String name, String owner) {
	    String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
	    MarkerSet markerSet = markerSets.get(chunk.getWorld());
	    if (markerSet == null) return;

	    // Define the corners of the chunk at the ground level
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
	    
	    String hoverText = ClaimSettings.getSetting("bluemap-hover-text")
	            .replaceAll("%claim-name%", name)
	            .replaceAll("%owner%", owner);
	    
	    String fcolor = ClaimSettings.getSetting("bluemap-claim-fill-color");
	    String lcolor = ClaimSettings.getSetting("bluemap-claim-border-color");
	    Color fillColor = new Color((int) Long.parseLong(fcolor, 16));
	    Color strokeColor = new Color((int) Long.parseLong(lcolor, 16));

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
	}
	
	// Method to update the tooltip name of the given chunk on the bluemap
	public static void updateName(Chunk chunk) {
		String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		MarkerSet markerSet = markerSets.get(chunk.getWorld());
		if(markerSet == null) return;
    	Marker marker = markerSet.get(markerId);
    	if(marker != null) {
        	String t = ClaimSettings.getSetting("dynmap-hover-text");
        	t = t.replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
        	t = t.replaceAll("%owner%", ClaimMain.getOwnerInClaim(chunk));
    		marker.setLabel(t);
    	}
	}
	
	// Method to delete a chunk area from the bluemap
	public static void deleteMarker(Chunk chunk) {
		String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		MarkerSet markerSet = markerSets.get(chunk.getWorld());
		if(markerSet == null) return;
		markerSet.remove(markerId);
	}
}
