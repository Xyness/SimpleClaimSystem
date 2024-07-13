package fr.xyness.SCS.Support;

import java.util.Set;

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
     * Creates an area marker on the Dynmap for given chunks.
     *
     * @param chunks the chunks to be marked.
     * @param name  the name of the claim.
     * @param owner the owner of the claim.
     */
	public void createChunkZone(Set<Chunk> chunks, String name, String owner) {
    	String t = instance.getSettings().getSetting("dynmap-claim-hover-text")
    			.replaceAll("%claim-name%", name)
    			.replaceAll("%owner%", owner);
    	int linestyle = Integer.parseInt(instance.getSettings().getSetting("dynmap-claim-border-color"), 16);
    	int fillstyle = Integer.parseInt(instance.getSettings().getSetting("dynmap-claim-fill-color"), 16);
    	chunks.parallelStream().forEach(chunk -> {
    		String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
    	    AreaMarker existingMarker = markerSet.findAreaMarker(markerId);
    	    if (existingMarker != null) return;
    	    World world = chunk.getWorld();
    	    int x = chunk.getX() * 16;
    	    int z = chunk.getZ() * 16;
    	    double[] xCorners = {x, x + 16, x + 16, x};
    	    double[] zCorners = {z, z, z + 16, z + 16};
    	    AreaMarker marker = markerSet.createAreaMarker(
    	        markerId,
    	        t,
    	        false,
    	        world.getName(),
    	        xCorners,
    	        zCorners,
    	        false
    	    );
    	    marker.setLineStyle(3, 1.0, linestyle);
    	    marker.setFillStyle(0.5, fillstyle);
    	});
	}
	
	/**
     * Updates the tooltip name of given chunks on the Dynmap.
     *
     * @param chunks the chunks whose tooltip needs updating.
     * @param claim the claim of chunks
     */
	public void updateName(Set<Chunk> chunks, Claim claim) {
    	String t = instance.getSettings().getSetting("dynmap-hover-text")
    			.replaceAll("%claim-name%", claim.getName())
    			.replaceAll("%owner%", claim.getOwner());
		chunks.parallelStream().forEach(chunk -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
			AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		        marker.setLabel(t);
		    }
		});
	}
	
	/**
     * Deletes an area marker for given chunks from the Dynmap.
     *
     * @param chunk the chunks whose marker needs deletion.
     */
	public void deleteMarker(Set<Chunk> chunks) {
		chunks.parallelStream().forEach(chunk -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		        marker.deleteMarker();
		    }
		});
	}
	
}
