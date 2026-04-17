package fr.xyness.SCS.Support;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;

import org.bukkit.Chunk;
import org.bukkit.World;

/**
 * This class handles the integration with WorldGuard for managing claim flags.
 */
public class ClaimWorldGuard {




	/** Indicates whether the custom flag has been registered. */
	public boolean registered = false;

	/** The custom flag for claims in WorldGuard. */
	public final StateFlag SCS_CLAIM_FLAG = new StateFlag("scs-claim", true);




	/**
	 * Registers the custom flag "scs-claim" with WorldGuard.
	 */
	public void registerCustomFlag() {
		if(!registered) {
			FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		    registry.register(SCS_CLAIM_FLAG);
		    registered = true;
		}
	}

	/**
	 * Checks if the "scs-claim" flag is enabled in a specific region.
	 *
	 * @param region the region to check.
	 * @return true if the flag is enabled, false otherwise.
	 */
    public boolean checkClaimFlagInRegion(ProtectedRegion region) {
        StateFlag.State flagValue = region.getFlag(SCS_CLAIM_FLAG);
        return flagValue == StateFlag.State.ALLOW;
    }

    /**
     * Checks if the "scs-claim" flag is enabled for every WorldGuard region overlapping a chunk.
     *
     * @param chunk the chunk to check.
     * @return true if the flag is enabled on all overlapping regions, false otherwise.
     */
    public boolean checkFlagClaimInChunk(Chunk chunk) {
        World world = chunk.getWorld();
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager manager = container.get(BukkitAdapter.adapt(world));
        if (manager == null) return true;

        int minX = chunk.getX() << 4;
        int minZ = chunk.getZ() << 4;
        int maxX = minX + 15;
        int maxZ = minZ + 15;

        BlockVector3 min = BlockVector3.at(minX, world.getMinHeight(), minZ);
        BlockVector3 max = BlockVector3.at(maxX, world.getMaxHeight(), maxZ);
        ProtectedCuboidRegion chunkRegion = new ProtectedCuboidRegion("__scs_chunk_check__", true, min, max);

        ApplicableRegionSet regions = manager.getApplicableRegions(chunkRegion);
        for (ProtectedRegion region : regions) {
            if (!checkClaimFlagInRegion(region)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the "scs-claim" flag is enabled for every chunk in the provided set.
     *
     * @param chunks the chunks to check.
     * @return true if the flag is enabled on all overlapping regions for all chunks, false otherwise.
     */
    public boolean checkFlagClaimInChunks(Iterable<Chunk> chunks) {
        for (Chunk chunk : chunks) {
            if (!checkFlagClaimInChunk(chunk)) {
                return false;
            }
        }
        return true;
    }
}
