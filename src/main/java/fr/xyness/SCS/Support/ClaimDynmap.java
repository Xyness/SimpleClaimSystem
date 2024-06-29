package fr.xyness.SCS.Support;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimSettings;

/**
 * This class handles the integration with Dynmap for visualizing claims on the map.
 */
public class ClaimDynmap {
	
	// ***************
	// *  Variables  *
	// ***************
	
    /** The Dynmap API instance. */
    private static DynmapAPI dynmapAPI;
    
    /** The Marker API instance from Dynmap. */
    private static MarkerAPI markerAPI;
    
    /** The MarkerSet instance to manage markers on the Dynmap. */
    private static MarkerSet markerSet;
    
	// ******************
	// *  Constructors  *
	// ******************
    
    /**
     * Constructor to initialize the ClaimDynmap class with Dynmap and Marker API instances.
     *
     * @param d  the Dynmap API instance.
     * @param m  the Marker API instance.
     * @param m2 the MarkerSet instance.
     */
    public ClaimDynmap(DynmapAPI d, MarkerAPI m, MarkerSet m2) {
    	this.dynmapAPI = d;
    	this.markerAPI = m;
    	this.markerSet = m2;
    }
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    /**
     * Creates an area marker on the Dynmap for a given chunk.
     *
     * @param chunk the chunk to be marked.
     * @param name  the name of the claim.
     * @param owner the owner of the claim.
     */
	public static void createChunkZone(Chunk chunk, String name, String owner) {
		Runnable task = () -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    AreaMarker existingMarker = markerSet.findAreaMarker(markerId);
		    if (existingMarker != null) return;
		    World world = chunk.getWorld();
		    int x = chunk.getX() * 16;
		    int z = chunk.getZ() * 16;
		    double[] xCorners = {x, x + 16, x + 16, x};
		    double[] zCorners = {z, z, z + 16, z + 16};
	    	String t = ClaimSettings.getSetting("dynmap-hover-text");
	    	t = t.replaceAll("%claim-name%", name);
	    	t = t.replaceAll("%owner%", owner);
		    AreaMarker marker = markerSet.createAreaMarker(
		        markerId,
		        t,
		        false,
		        world.getName(),
		        xCorners,
		        zCorners,
		        false
		    );
		    marker.setLineStyle(3, 1.0, Integer.parseInt(ClaimSettings.getSetting("dynmap-claim-border-color"), 16));
		    marker.setFillStyle(0.5, Integer.parseInt(ClaimSettings.getSetting("dynmap-claim-fill-color"), 16));
		};
		SimpleClaimSystem.executeAsync(task);
	}
	
	/**
     * Updates the tooltip name of the given chunk on the Dynmap.
     *
     * @param chunk the chunk whose tooltip needs updating.
     */
	public static void updateName(Chunk chunk) {
		Runnable task = () -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
			AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		    	String t = ClaimSettings.getSetting("dynmap-hover-text");
		    	t = t.replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
		    	t = t.replaceAll("%owner%", ClaimMain.getOwnerInClaim(chunk));
		        marker.setLabel(t);
		    }
		};
		SimpleClaimSystem.executeAsync(task);
	}
	
	/**
     * Deletes an area marker for a given chunk from the Dynmap.
     *
     * @param chunk the chunk whose marker needs deletion.
     */
	public static void deleteMarker(Chunk chunk) {
		Runnable task = () -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		        marker.deleteMarker();
		    }
		};
		SimpleClaimSystem.executeAsync(task);
	}
	
}
