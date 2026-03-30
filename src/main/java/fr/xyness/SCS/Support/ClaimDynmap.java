package fr.xyness.SCS.Support;

import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.World;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerSet;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;

/**
 * This class handles the integration with Dynmap for visualizing claims on the map.
 */
public class ClaimDynmap {

    private MarkerSet markerSet;
    private SimpleClaimSystem instance;

    /**
     * Constructor to initialize the ClaimDynmap class.
     *
     * @param m2       the MarkerSet instance.
     * @param instance the SimpleClaimSystem instance.
     */
    public ClaimDynmap(MarkerSet m2, SimpleClaimSystem instance) {
    	this.markerSet = m2;
    	this.instance = instance;
    }

    /**
     * Clears all claim markers from the Dynmap.
     * Used during plugin reload to reset map state.
     */
    public void clearMarkers() {
        if (markerSet != null) {
            markerSet.getAreaMarkers().forEach(marker -> marker.deleteMarker());
        }
    }

    /**
     * Creates an area marker on the Dynmap for given claim.
     *
     * @param claim The claim to be marked
     */
	public void createClaimZone(Claim claim) {
		if(markerSet == null) return;
    	String t = instance.getSettings().getSetting("dynmap-claim-hover-text")
    			.replace("%claim-name%", claim.getName())
    			.replace("%owner%", claim.getOwner());
    	int linestyle = Integer.parseInt(instance.getSettings().getSetting("dynmap-claim-border-color"), 16);
    	int fillstyle = Integer.parseInt(instance.getSettings().getSetting("dynmap-claim-fill-color"), 16);
    	double opacity = Double.parseDouble(instance.getSettings().getSetting("dynmap-claim-opacity"));
    	claim.getChunks().forEach(chunk -> {
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
    	    marker.setFillStyle(opacity, fillstyle);
    	});
	}
	
	/**
     * Updates the tooltip name of given claim on the Dynmap.
     *
     * @param claim The claim to update the name for
     */
	public void updateName(Claim claim) {
		if(markerSet == null) return;
    	String t = instance.getSettings().getSetting("dynmap-claim-hover-text")
    			.replace("%claim-name%", claim.getName())
    			.replace("%owner%", claim.getOwner());
		claim.getChunks().forEach(chunk -> {
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
     * @param chunks The chunks whose marker needs deletion.
     */
	public void deleteMarker(Set<Chunk> chunks) {
		if(markerSet == null) return;
		chunks.forEach(chunk -> {
			String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		    AreaMarker marker = markerSet.findAreaMarker(markerId);
		    if (marker != null) {
		        marker.deleteMarker();
		    }
		});
	}
	
}
