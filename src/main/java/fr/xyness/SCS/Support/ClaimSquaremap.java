package fr.xyness.SCS.Support;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.Claim;
import xyz.jpenilla.squaremap.api.BukkitAdapter;
import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.Point;
import xyz.jpenilla.squaremap.api.SimpleLayerProvider;
import xyz.jpenilla.squaremap.api.Squaremap;
import xyz.jpenilla.squaremap.api.SquaremapProvider;
import xyz.jpenilla.squaremap.api.WorldIdentifier;
import xyz.jpenilla.squaremap.api.marker.Marker;
import xyz.jpenilla.squaremap.api.marker.MarkerOptions;
import xyz.jpenilla.squaremap.api.marker.Polygon;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class integrates claims with the Squaremap plugin, allowing claims to be displayed as markers on the Squaremap.
 */
public class ClaimSquaremap {


    // ***************
    // *  Variables  *
    // ***************


    // Store layer providers for each world
    private final Map<World, SimpleLayerProvider> layerProviders = new HashMap<>();

    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;

    /** Instance of Squaremap API */
    private final Squaremap squaremapApi;


    // ******************
    // *  Constructors  *
    // ******************


    /**
     * Constructor for the ClaimSquaremap class.
     * Initializes the layers for each world.
     */
    public ClaimSquaremap(SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.squaremapApi = SquaremapProvider.get();
    	initialize();
    }


    // ********************
    // *  Other Methods   *
    // ********************


    /**
     * Initializes the layers for each world and creates markers for all existing claims.
     */
    private void initialize() {
        instance.executeAsync(() -> {
            Set<Claim> claims = instance.getMain().getAllClaims();
            for (World world : Bukkit.getWorlds()) {
                WorldIdentifier worldId = BukkitAdapter.worldIdentifier(world);
                xyz.jpenilla.squaremap.api.MapWorld mapWorld = squaremapApi.getWorldIfEnabled(worldId).orElse(null);

                if (mapWorld != null) {
                    Key layerKey = Key.of("simpleclaimsystem_claims");
                    SimpleLayerProvider provider = SimpleLayerProvider.builder("Claims")
                        .showControls(true)
                        .defaultHidden(false)
                        .layerPriority(1)
                        .zIndex(1)
                        .build();

                    mapWorld.layerRegistry().register(layerKey, provider);
                    layerProviders.put(world, provider);

                    // Add existing claims to the map
                    for (Claim claim : claims) {
                        if (claim.getLocation().getWorld().equals(world)) {
                        	createClaimZone(claim);
                        }
                    }
                }
            }
            instance.getLogger().info("Claims added to Squaremap.");
        });
    }


    /**
     * Creates a marker on the Squaremap for the specified claim.
     * Uses rectangles without borders to avoid grid pattern.
     *
     * @param claim The claim to create the marker for.
     */
    public void createClaimZone(Claim claim) {
        if (claim.getChunks().isEmpty()) return;

        String hoverText = instance.getSettings().getSetting("squaremap-claim-hover-text")
                .replace("%claim-name%", claim.getName())
                .replace("%owner%", claim.getOwner());

        String fillColorHex = instance.getSettings().getSetting("squaremap-claim-fill-color");
        String strokeColorHex = instance.getSettings().getSetting("squaremap-claim-border-color");

        // Parse hex colors to RGB integers
        int fillColor = Integer.parseInt(fillColorHex, 16);
        int strokeColor = Integer.parseInt(strokeColorHex, 16);

        // Get world from first chunk
        World world = claim.getChunks().iterator().next().getWorld();
        SimpleLayerProvider layerProvider = layerProviders.get(world);

        if (layerProvider == null) {
            // Create layer if it doesn't exist
            WorldIdentifier worldId = BukkitAdapter.worldIdentifier(world);
            xyz.jpenilla.squaremap.api.MapWorld mapWorld = squaremapApi.getWorldIfEnabled(worldId).orElse(null);

            if (mapWorld != null) {
                Key layerKey = Key.of("simpleclaimsystem_claims");
                SimpleLayerProvider newProvider = SimpleLayerProvider.builder("Claims")
                    .showControls(true)
                    .defaultHidden(false)
                    .layerPriority(1)
                    .zIndex(1)
                    .build();

                mapWorld.layerRegistry().register(layerKey, newProvider);
                layerProviders.put(world, newProvider);
                layerProvider = newProvider;
            }
        }

        if (layerProvider == null) return;

        final SimpleLayerProvider provider = layerProvider;

        // Remove old markers for this claim first
        String claimMarkerId = "claim_" + claim.getId();
        provider.removeMarker(Key.of(claimMarkerId));

        // Also remove old chunk-based markers for backwards compatibility
        claim.getChunks().forEach(chunk -> {
            String oldMarkerId = "chunk_" + chunk.getX() + "_" + chunk.getZ();
            provider.removeMarker(Key.of(oldMarkerId));
            String oldClaimChunkId = "claim_" + claim.getId() + "_chunk_" + chunk.getX() + "_" + chunk.getZ();
            provider.removeMarker(Key.of(oldClaimChunkId));
        });

        // Create a merged polygon for all chunks with single outer border
        List<Point> outerBoundary = calculateOuterBoundary(claim.getChunks());

        if (!outerBoundary.isEmpty()) {
            Marker marker = Polygon.polygon(outerBoundary)
                .markerOptions(
                    MarkerOptions.builder()
                        .strokeColor(new Color(strokeColor))
                        .strokeWeight(3)
                        .fillColor(new Color(fillColor))
                        .fillOpacity(0.35)
                        .clickTooltip(hoverText)
                        .build()
                );

            provider.addMarker(Key.of(claimMarkerId), marker);
        }
    }

    /**
     * Calculates the outer boundary of a set of chunks as a polygon.
     * This creates a single outline around all connected chunks.
     */
    private List<Point> calculateOuterBoundary(Set<Chunk> chunks) {
        if (chunks.isEmpty()) return new ArrayList<>();

        // Convert chunks to a set of coordinate pairs for easy lookup
        java.util.Set<String> chunkCoords = chunks.stream()
            .map(c -> c.getX() + "," + c.getZ())
            .collect(Collectors.toSet());

        // Collect all outer edges (edges that don't have an adjacent chunk)
        List<Edge> outerEdges = new ArrayList<>();

        for (Chunk chunk : chunks) {
            int cx = chunk.getX();
            int cz = chunk.getZ();
            int minX = cx * 16;
            int minZ = cz * 16;
            int maxX = minX + 16;
            int maxZ = minZ + 16;

            // Check each of the 4 edges
            // North edge (top)
            if (!chunkCoords.contains((cx) + "," + (cz - 1))) {
                outerEdges.add(new Edge(minX, minZ, maxX, minZ));
            }
            // South edge (bottom)
            if (!chunkCoords.contains((cx) + "," + (cz + 1))) {
                outerEdges.add(new Edge(minX, maxZ, maxX, maxZ));
            }
            // West edge (left)
            if (!chunkCoords.contains((cx - 1) + "," + (cz))) {
                outerEdges.add(new Edge(minX, minZ, minX, maxZ));
            }
            // East edge (right)
            if (!chunkCoords.contains((cx + 1) + "," + (cz))) {
                outerEdges.add(new Edge(maxX, minZ, maxX, maxZ));
            }
        }

        // Convert edges to points in order (trace the outline)
        return traceOutline(outerEdges);
    }

    /**
     * Traces the outline from a collection of edges.
     */
    private List<Point> traceOutline(List<Edge> edges) {
        if (edges.isEmpty()) return new ArrayList<>();

        List<Point> points = new ArrayList<>();
        java.util.Set<Edge> usedEdges = new HashSet<>();

        // Start with the first edge
        Edge currentEdge = edges.getFirst();
        points.add(Point.of(currentEdge.x1, currentEdge.z1));
        points.add(Point.of(currentEdge.x2, currentEdge.z2));
        usedEdges.add(currentEdge);

        // Find connected edges
        while (usedEdges.size() < edges.size()) {
            Point lastPoint = points.getLast();
            boolean found = false;

            for (Edge edge : edges) {
                if (usedEdges.contains(edge)) continue;

                // Check if this edge connects to our current point
                if (edge.x1 == lastPoint.x() && edge.z1 == lastPoint.z()) {
                    points.add(Point.of(edge.x2, edge.z2));
                    usedEdges.add(edge);
                    found = true;
                    break;
                } else if (edge.x2 == lastPoint.x() && edge.z2 == lastPoint.z()) {
                    points.add(Point.of(edge.x1, edge.z1));
                    usedEdges.add(edge);
                    found = true;
                    break;
                }
            }

            if (!found) break; // Can't find more connected edges
        }

        return points;
    }

    /**
         * Helper class to represent an edge.
         */
        private record Edge(int x1, int z1, int x2, int z2) {

        @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (!(o instanceof Edge edge)) return false;
                return x1 == edge.x1 && z1 == edge.z1 && x2 == edge.x2 && z2 == edge.z2;
            }

    }

    /**
     * Updates the tooltip name of the specified claim on the Squaremap.
     *
     * @param claim the claim to update the name for
     */
    public void updateName(Claim claim) {
        // Simply recreate the claim zone with updated information
        createClaimZone(claim);
    }

    /**
     * Deletes the marker for the specified claim from the Squaremap.
     *
     * @param claim The claim to delete the marker for.
     */
    public void deleteMarker(Claim claim) {
        if (claim.getChunks().isEmpty()) return;

        World world = claim.getChunks().iterator().next().getWorld();
        SimpleLayerProvider provider = layerProviders.get(world);

        if (provider != null) {
            // Remove all chunk markers for this claim
            claim.getChunks().forEach(chunk -> {
                String markerId = "claim_" + claim.getId() + "_chunk_" + chunk.getX() + "_" + chunk.getZ();
                provider.removeMarker(Key.of(markerId));
            });
        }
    }

}
