package fr.xyness.SCS.Support;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimSettings;

public class ClaimDynmap {
	
    private static DynmapAPI dynmapAPI;
    private static MarkerAPI markerAPI;
    private static MarkerSet markerSet;
    
    public ClaimDynmap(DynmapAPI d, MarkerAPI m, MarkerSet m2) {
    	this.dynmapAPI = d;
    	this.markerAPI = m;
    	this.markerSet = m2;
    }

	public static void createChunkZone(Chunk chunk, String name, String owner) {
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
	        name + " - Owner: " + owner,
	        false,
	        world.getName(),
	        xCorners,
	        zCorners,
	        false
	    );
	    marker.setLineStyle(3, 1.0, Integer.parseInt(ClaimSettings.getSetting("dynmap-claim-border-color"), 16));
	    marker.setFillStyle(0.5, Integer.parseInt(ClaimSettings.getSetting("dynmap-claim-fill-color"), 16));
	}
	
	public static void updateName(Chunk chunk) {
		String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
		AreaMarker marker = markerSet.findAreaMarker(markerId);
	    if (marker != null) {
	    	String t = ClaimSettings.getSetting("dynmap-hover-text");
	    	t = t.replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
	    	t = t.replaceAll("%owner%", ClaimMain.getOwnerInClaim(chunk));
	        marker.setLabel(t);
	        Bukkit.getConsoleSender().sendMessage(t);
	    }
	    Bukkit.getConsoleSender().sendMessage("lancé");
	}
	
	public static void deleteMarker(Chunk chunk) {
		String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
	    AreaMarker marker = markerSet.findAreaMarker(markerId);
	    if (marker != null) {
	        marker.deleteMarker();
	    }
	}
	
}
