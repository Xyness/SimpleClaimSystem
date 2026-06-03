package fr.xyness.SCS;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Chunk;

import fr.xyness.SCS.Types.Claim;

/**
 * Chunk-keyed claim map that also maintains a parallel coordinate index ("world;x;z").
 * This lets claims be looked up by coordinates without touching the chunk system,
 * so lookups are safe from any thread (including off the region thread on Folia).
 */
class ClaimChunkMap extends ConcurrentHashMap<Chunk, Claim> {

    private static final long serialVersionUID = 1L;

    /** Parallel index: "world;x;z" -> claim. */
    private final Map<String, Claim> byCoords = new ConcurrentHashMap<>();

    private static String coordKey(String world, int x, int z) {
        return world + ";" + x + ";" + z;
    }

    @Override
    public Claim put(Chunk chunk, Claim claim) {
        byCoords.put(coordKey(chunk.getWorld().getName(), chunk.getX(), chunk.getZ()), claim);
        return super.put(chunk, claim);
    }

    @Override
    public Claim remove(Object chunk) {
        if (chunk instanceof Chunk) {
            Chunk c = (Chunk) chunk;
            byCoords.remove(coordKey(c.getWorld().getName(), c.getX(), c.getZ()));
        }
        return super.remove(chunk);
    }

    @Override
    public void clear() {
        byCoords.clear();
        super.clear();
    }

    /**
     * Gets the claim at the given chunk coordinates.
     *
     * @param world the world name
     * @param x     the chunk X coordinate
     * @param z     the chunk Z coordinate
     * @return the claim, or null if none
     */
    Claim getByCoords(String world, int x, int z) {
        return byCoords.get(coordKey(world, x, z));
    }

    /**
     * Whether a claim exists at the given chunk coordinates.
     *
     * @param world the world name
     * @param x     the chunk X coordinate
     * @param z     the chunk Z coordinate
     * @return true if a claim exists there
     */
    boolean containsCoords(String world, int x, int z) {
        return byCoords.containsKey(coordKey(world, x, z));
    }
}
