package fr.xyness.SCS.Support;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimSettings;
import net.pl3x.map.core.Pl3xMap;
import net.pl3x.map.core.event.EventHandler;
import net.pl3x.map.core.event.EventListener;
import net.pl3x.map.core.event.server.Pl3xMapEnabledEvent;
import net.pl3x.map.core.markers.Point;
import net.pl3x.map.core.markers.layer.Layer;
import net.pl3x.map.core.markers.layer.SimpleLayer;
import net.pl3x.map.core.markers.marker.Marker;
import net.pl3x.map.core.markers.marker.Rectangle;
import net.pl3x.map.core.markers.option.Options;
import net.pl3x.map.core.registry.Registry;
import net.pl3x.map.core.util.Colors;

/**
 * This class integrates claims with the Pl3xMap plugin, allowing claims to be displayed as markers on the Pl3xMap.
 */
public class ClaimPl3xMap implements EventListener {

    // ***************
    // *  Variables  *
    // ***************

    // Store layers for each world
    private static final Map<World, SimpleLayer> layers = new HashMap<>();

    // ******************
    // *  Constructors  *
    // ******************

    /**
     * Constructor for the ClaimPl3xMap class.
     * Initializes the layers for each world.
     */
    public ClaimPl3xMap() {
        Pl3xMap.api().getEventRegistry().register(this);
    }

    // ******************
    // *  EventHandler  *
    // ******************

    /**
     * Event handler for the Pl3xMapEnabledEvent.
     * 
     * This method is triggered when Pl3xMap is enabled. It initializes the layers for each world
     * and creates markers for all existing claims.
     * 
     * @param event The Pl3xMapEnabledEvent that triggers this handler.
     */
    @EventHandler
    public void onPl3xMapEnabled(Pl3xMapEnabledEvent event) {
        Runnable task = () -> {
            Registry<net.pl3x.map.core.world.World> worldRegistry = Pl3xMap.api().getWorldRegistry();
            Set<Chunk> claims = ClaimMain.getAllClaimsChunk();
            for (World world : Bukkit.getWorlds()) {
                String worldName = world.getName();
                String layerId = "claims_" + world.getName();
                net.pl3x.map.core.world.World mapWorld = worldRegistry.get(worldName);
                if (mapWorld != null) {
                    Layer layer = new SimpleLayer(layerId, () -> "Claims");
                    layer.setPriority(1);
                    layer.setZIndex(1);
                    layer.setLiveUpdate(true);
                    mapWorld.getLayerRegistry().register(layer);
                    layers.put(world, (SimpleLayer) layer);
                    for (Chunk c : claims) {
                        Claim claim = ClaimMain.getClaimFromChunk(c);
                        if (claim == null) continue;
                        if (!claim.getLocation().getWorld().equals(world)) continue;
                        createChunkZone(c, claim.getName(), claim.getOwner());
                    }
                }
            }
            SimpleClaimSystem.getInstance().getLogger().info("Claims added to Pl3xMap.");
        };
        SimpleClaimSystem.executeAsync(task);
    }

    // ********************
    // *  Other Methods   *
    // ********************

    /**
     * Creates a marker on the Pl3xMap for the specified chunk.
     *
     * @param chunk the chunk to create the marker for.
     * @param name  the name of the claim.
     * @param owner the owner of the claim.
     */
    public static void createChunkZone(Chunk chunk, String name, String owner) {
        Runnable task = () -> {
            String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();

            Point point1 = Point.of(chunk.getX() * 16, chunk.getZ() * 16);
            Point point2 = Point.of((chunk.getX() * 16) + 16, (chunk.getZ() * 16) + 16);

            Rectangle rectangle = new Rectangle(markerId, point1, point2);

            String hoverText = ClaimSettings.getSetting("pl3xmap-hover-text")
                    .replace("%claim-name%", name)
                    .replace("%owner%", owner);

            String fillColor = ClaimSettings.getSetting("pl3xmap-claim-fill-color");
            String strokeColor = ClaimSettings.getSetting("pl3xmap-claim-border-color");

            Options options = Options.builder()
                    .tooltipContent(hoverText)
                    .fillColor(Colors.setAlpha(0xFF, Integer.parseInt(fillColor, 16)))
                    .strokeColor(Colors.setAlpha(0xFF, Integer.parseInt(strokeColor, 16)))
                    .strokeWeight(2)
                    .fill(true)
                    .stroke(true)
                    .build();

            rectangle.setOptions(options);
            layers.get(chunk.getWorld()).addMarker(rectangle);
        };
        SimpleClaimSystem.executeAsync(task);
    }

    /**
     * Updates the tooltip name of the specified chunk on the Pl3xMap.
     *
     * @param chunk the chunk to update the name for.
     */
    public static void updateName(Chunk chunk) {
        Runnable task = () -> {
            String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
            Collection<Marker<?>> markers = layers.get(chunk.getWorld()).getMarkers();
            for (Marker<?> marker : markers) {
                if (marker.getKey().equals(markerId)) {
                    String hoverText = ClaimSettings.getSetting("pl3xmap-hover-text")
                            .replace("%claim-name%", ClaimMain.getClaimNameByChunk(chunk))
                            .replace("%owner%", ClaimMain.getOwnerInClaim(chunk));
                    String fillColor = ClaimSettings.getSetting("pl3xmap-claim-fill-color");
                    String strokeColor = ClaimSettings.getSetting("pl3xmap-claim-border-color");
                    Options newOptions = Options.builder()
                            .tooltipContent(hoverText)
                            .fillColor(Colors.setAlpha(0xFF, Integer.parseInt(fillColor, 16)))
                            .strokeColor(Colors.setAlpha(0xFF, Integer.parseInt(strokeColor, 16)))
                            .strokeWeight(2)
                            .fill(true)
                            .stroke(true)
                            .build();
                    marker.setOptions(newOptions);
                    break;
                }
            }
        };
        SimpleClaimSystem.executeAsync(task);
    }

    /**
     * Deletes the marker for the specified chunk from the Pl3xMap.
     *
     * @param chunk the chunk to delete the marker for.
     */
    public static void deleteMarker(Chunk chunk) {
        Runnable task = () -> {
            String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
            layers.get(chunk.getWorld()).getMarkers().removeIf(marker -> marker.getKey().equals(markerId));
        };
        SimpleClaimSystem.executeAsync(task);
    }
}
