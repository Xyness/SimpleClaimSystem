package fr.xyness.SCS.Support;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;
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

	

	
    // Store layers for each world
    private final Map<World, SimpleLayer> layers = new ConcurrentHashMap<>();
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    


    
    /**
     * Constructor for the ClaimPl3xMap class.
     * Registers the Pl3xMap event listener.
     *
     * @param instance the SimpleClaimSystem instance.
     */
    public ClaimPl3xMap(SimpleClaimSystem instance) {
    	this.instance = instance;
        Pl3xMap.api().getEventRegistry().register(this);
    }

    

    
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
        instance.executeAsync(() -> {
            Registry<net.pl3x.map.core.world.World> worldRegistry = Pl3xMap.api().getWorldRegistry();
            Set<Claim> claims = instance.getMain().getAllClaims();
            for (World world : Bukkit.getWorlds()) {
                String worldName = world.getName();
                String layerId = "claims_" + world.getName();
                net.pl3x.map.core.world.World mapWorld = worldRegistry.get(worldName);
                if (mapWorld != null) {
                    Layer layer = new SimpleLayer(layerId, () -> "Claims");
                    layer.setPriority(1);
                    layer.setZIndex(1);
                    try { layer.setLiveUpdate(true); } catch (NoSuchMethodError ignored) {}
                    mapWorld.getLayerRegistry().register(layer);
                    layers.put(world, (SimpleLayer) layer);
                    for (Claim claim : claims) {
                        if (claim.getLocation().getWorld().equals(world)) {
                        	createClaimZone(claim);
                        }
                    }
                }
            }
            instance.getLogger().info("Claims added to Pl3xMap.");
        });
    }

    


    /**
     * Clears all claim layers from the Pl3xMap.
     * Used during plugin reload to reset map state.
     */
    public void clearMarkers() {
        layers.values().forEach(layer -> layer.getMarkers().clear());
        layers.clear();
    }

    /**
     * Reloads all claims onto the Pl3xMap.
     * Re-initializes layers for each world and re-creates all markers.
     */
    public void reload() {
        clearMarkers();
        Registry<net.pl3x.map.core.world.World> worldRegistry = Pl3xMap.api().getWorldRegistry();
        Set<Claim> claims = instance.getMain().getAllClaims();
        for (World world : Bukkit.getWorlds()) {
            String worldName = world.getName();
            String layerId = "claims_" + world.getName();
            net.pl3x.map.core.world.World mapWorld = worldRegistry.get(worldName);
            if (mapWorld != null) {
                try {
                    mapWorld.getLayerRegistry().unregister(layerId);
                } catch (Exception ignored) {}
                Layer layer = new SimpleLayer(layerId, () -> "Claims");
                layer.setPriority(1);
                layer.setZIndex(1);
                layer.setLiveUpdate(true);
                mapWorld.getLayerRegistry().register(layer);
                layers.put(world, (SimpleLayer) layer);
                for (Claim claim : claims) {
                    if (claim.getLocation().getWorld().equals(world)) {
                        createClaimZone(claim);
                    }
                }
            }
        }
    }

    
    /**
     * Creates a marker on the Pl3xMap for the specified claim.
     *
     * @param claim The claim to create the marker for.
     */
    /**
     * Parses the alpha value from the config opacity setting (0-255).
     *
     * @return the alpha value as an int
     */
    private int getAlpha() {
        try {
            String opacity = instance.getSettings().getSetting("pl3xmap-claim-opacity");
            if (opacity != null && !opacity.isEmpty()) {
                int val = Integer.parseInt(opacity);
                return Math.max(0, Math.min(255, val));
            }
        } catch (NumberFormatException ignored) {}
        return 0x80; // default 50% opacity
    }

    public void createClaimZone(Claim claim) {
        String hoverText = instance.getSettings().getSetting("pl3xmap-claim-hover-text")
                .replace("%claim-name%", claim.getName())
                .replace("%owner%", claim.getOwner());

        String fillColor = instance.getSettings().getSetting("pl3xmap-claim-fill-color");
        String strokeColor = instance.getSettings().getSetting("pl3xmap-claim-border-color");
        int alpha = getAlpha();
        claim.getChunks().forEach(chunk -> {
            String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();

            Point point1 = Point.of(chunk.getX() * 16, chunk.getZ() * 16);
            Point point2 = Point.of((chunk.getX() * 16) + 16, (chunk.getZ() * 16) + 16);

            Rectangle rectangle = new Rectangle(markerId, point1, point2);

            Options options = Options.builder()
                    .tooltipContent(hoverText)
                    .fillColor(Colors.setAlpha(alpha, Integer.parseInt(fillColor, 16)))
                    .strokeColor(Colors.setAlpha(0xFF, Integer.parseInt(strokeColor, 16)))
                    .strokeWeight(2)
                    .fill(true)
                    .stroke(true)
                    .build();

            rectangle.setOptions(options);
            World world = chunk.getWorld();
            SimpleLayer targetLayer = layers.get(world);
            if(targetLayer == null) {
                String worldName = world.getName();
                String layerId = "claims_" + world.getName();
                Registry<net.pl3x.map.core.world.World> worldRegistry = Pl3xMap.api().getWorldRegistry();
                net.pl3x.map.core.world.World mapWorld = worldRegistry.get(worldName);
                if (mapWorld != null) {
                    Layer layer = new SimpleLayer(layerId, () -> "Claims");
                    layer.setPriority(1);
                    layer.setZIndex(1);
                    try { layer.setLiveUpdate(true); } catch (NoSuchMethodError ignored) {}
                    mapWorld.getLayerRegistry().register(layer);
                    layers.put(world, (SimpleLayer) layer);
                    layers.get(world).addMarker(rectangle);
                }
            } else {
            	layers.get(chunk.getWorld()).addMarker(rectangle);
            }
            
        });
    }

    /**
     * Updates the tooltip name of the specified chunk on the Pl3xMap.
     *
     * @param claim the claim to update the name for
     */
    public void updateName(Claim claim) {
        String hoverText = instance.getSettings().getSetting("pl3xmap-claim-hover-text")
                .replace("%claim-name%", claim.getName())
                .replace("%owner%", claim.getOwner());
        String fillColor = instance.getSettings().getSetting("pl3xmap-claim-fill-color");
        String strokeColor = instance.getSettings().getSetting("pl3xmap-claim-border-color");
        claim.getChunks().forEach(chunk -> {
            String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
            Collection<Marker<?>> markers = layers.get(chunk.getWorld()).getMarkers();
            for (Marker<?> marker : markers) {
                if (marker.getKey().equals(markerId)) {
                    Options newOptions = Options.builder()
                            .tooltipContent(hoverText)
                            .fillColor(Colors.setAlpha(getAlpha(), Integer.parseInt(fillColor, 16)))
                            .strokeColor(Colors.setAlpha(0xFF, Integer.parseInt(strokeColor, 16)))
                            .strokeWeight(2)
                            .fill(true)
                            .stroke(true)
                            .build();
                    marker.setOptions(newOptions);
                    break;
                }
            }
        });
    }

    /**
     * Deletes the marker for the specified chunks from the Pl3xMap.
     *
     * @param chunks The chunks to delete the marker for.
     */
    public void deleteMarker(Set<Chunk> chunks) {
        chunks.forEach(chunk -> {
            String markerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
            SimpleLayer layer = layers.get(chunk.getWorld());
            if (layer != null) {
            	layer.getMarkers().removeIf(marker -> marker.getKey().equals(markerId));
            }
        });
    }
}
