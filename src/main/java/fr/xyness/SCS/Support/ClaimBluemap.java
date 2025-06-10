package fr.xyness.SCS.Support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.flowpowered.math.vector.Vector2i;
import com.technicjelle.BMUtils.Cheese;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Shape;
import org.bukkit.Bukkit;
import org.bukkit.World;


import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
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
	private final BlueMapAPI api;
	
	/** A map storing the MarkerSets for each world. */
	private final Map<World, MarkerSet> markerSets = new HashMap<>();
	
    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;
	
    
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
		load();
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
	    // Get info from the claim
		String hoverText = instance.getSettings().getSetting("bluemap-claim-hover-text")
				.replace("%claim-name%", claim.getName())
				.replace("%owner%", claim.getOwner());
		String markerId = "claim_" + claim.getId();
		MarkerSet markerSet = markerSets.get(claim.getLocation().getWorld());
		if (markerSet == null) return;
		markerSet.getMarkers().keySet().removeIf(key -> key.startsWith(markerId));

		// Get claim coordinates
		Vector2i[] chunkCoordinates = claim.getChunks().stream()
				.map(chunk -> new Vector2i(chunk.getX(), chunk.getZ()))
				.toArray(Vector2i[]::new);
		Collection<Cheese> cheeses = Cheese.createPlatterFromChunks(chunkCoordinates);

		// Get the claim color
	    Color fillColor = new Color((int) Long.parseLong("80" + instance.getSettings().getSetting("bluemap-claim-fill-color"), 16));
	    Color strokeColor = new Color((int) Long.parseLong("80" + instance.getSettings().getSetting("bluemap-claim-border-color"), 16));

		// Create the marker
		AtomicInteger index = new AtomicInteger();
		cheeses.forEach( cheese -> {
			ShapeMarker marker = ShapeMarker.builder()
					.label(hoverText)
					.detail(hoverText)
					.depthTestEnabled(false)
					.shape(cheese.getShape(), 64)
					.holes(cheese.getHoles().toArray(Shape[]::new))
					.fillColor(fillColor)
					.lineColor(strokeColor)
					.lineWidth(5)
					.build();

			markerSet.getMarkers().put(markerId + "_" + index.getAndIncrement(), marker);
		});
	}
	
	/**
	 * Updates the tooltip name of the specified chunks on the BlueMap.
	 *
	 * @param claim The claim to update the name for
	 */
	public void updateName(Claim claim) {
    	// Get new name
		String t = instance.getSettings().getSetting("bluemap-claim-hover-text")
    			.replace("%claim-name%", claim.getName())
    			.replace("%owner%", claim.getOwner());

		// Get the marker
		String markerId = "claim_" + claim.getId();
		MarkerSet markerSet = markerSets.get(claim.getLocation().getWorld());
		if (markerSet == null) return;
		markerSet.getMarkers().entrySet().stream()
				.filter(ent -> ent.getKey().startsWith(markerId))
				.forEach(ent -> {
					ShapeMarker marker = (ShapeMarker) ent.getValue();
					marker.setLabel(t);
					marker.setDetail(t);
				});
	}

	/**
	 * Deletes the marker for the specified chunks from the BlueMap.
	 *
	 * @param claim The chunks to delete the marker for.
	 */
	public void deleteMarker(Claim claim) {
		String markerId = "claim_" + claim.getId();
		MarkerSet markerSet = markerSets.get(claim.getLocation().getWorld());
		if (markerSet == null) return;
		markerSet.getMarkers().keySet().removeIf(key -> key.startsWith(markerId));
	}
	
}
