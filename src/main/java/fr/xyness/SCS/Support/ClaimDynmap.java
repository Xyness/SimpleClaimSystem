package fr.xyness.SCS.Support;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import fr.xyness.SCS.Claim;
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
    private DynmapAPI dynmapAPI;
    
    /** The Marker API instance from Dynmap. */
    private MarkerAPI markerAPI;
    
    /** The MarkerSet instance to manage markers on the Dynmap. */
    private MarkerSet markerSet;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
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
    public ClaimDynmap(DynmapAPI d, MarkerAPI m, MarkerSet m2, SimpleClaimSystem instance) {
    	this.dynmapAPI = d;
    	this.markerAPI = m;
    	this.markerSet = m2;
    	this.instance = instance;
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
	public void createChunkZone(Chunk chunk, String name, String owner) {
		instance.executeAsync(() -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    AreaMarker existingMarker = markerSet.findAreaMarker(markerId);
		    if (existingMarker != null) return;
		    World world = chunk.getWorld();
		    int x = chunk.getX() * 16;
		    int z = chunk.getZ() * 16;
		    double[] xCorners = {x, x + 16, x + 16, x};
		    double[] zCorners = {z, z, z + 16, z + 16};
	    	String t = instance.getSettings().getSetting("dynmap-claim-hover-text");
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
		    marker.setLineStyle(3, 1.0, Integer.parseInt(instance.getSettings().getSetting("dynmap-claim-border-color"), 16));
		    marker.setFillStyle(0.5, Integer.parseInt(instance.getSettings().getSetting("dynmap-claim-fill-color"), 16));
		});
	}
	
	/**
     * Updates the tooltip name of the given chunk on the Dynmap.
     *
     * @param chunk the chunk whose tooltip needs updating.
     */
	public void updateName(Chunk chunk) {
		instance.executeAsync(() -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
			AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		    	String t = instance.getSettings().getSetting("dynmap-hover-text");
	        	Claim claim = instance.getMain().getClaim(chunk);
	        	t = t.replaceAll("%claim-name%", claim.getName());
	        	t = t.replaceAll("%owner%", claim.getOwner());
		        marker.setLabel(t);
		    }
		});
	}
	
	/**
     * Deletes an area marker for a given chunk from the Dynmap.
     *
     * @param chunk the chunk whose marker needs deletion.
     */
	public void deleteMarker(Chunk chunk) {
		instance.executeAsync(() -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		        marker.deleteMarker();
		    }
		});
	}
	
}
