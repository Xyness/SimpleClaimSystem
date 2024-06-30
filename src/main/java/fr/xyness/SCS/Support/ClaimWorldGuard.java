package fr.xyness.SCS.Support;

import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import org.bukkit.entity.Player;

/**
 * This class handles the integration with WorldGuard for managing claim flags.
 */
public class ClaimWorldGuard {
	
	// ***************
	// *  Variables  *
	// ***************
	
	/** Indicates whether the custom flag has been registered. */
	public static boolean registered = false;
	
	/** The custom flag for claims in WorldGuard. */
	public static final StateFlag SCS_CLAIM_FLAG = new StateFlag("scs-claim", true);
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	/**
	 * Registers the custom flag "scs-claim" with WorldGuard.
	 */
	public static void registerCustomFlag() {
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
    public static boolean checkClaimFlagInRegion(ProtectedRegion region) {
        StateFlag.State flagValue = region.getFlag(SCS_CLAIM_FLAG);
        return flagValue == StateFlag.State.ALLOW;
    }
    
    /**
     * Checks if the "scs-claim" flag is enabled at the player's location.
     *
     * @param player the player whose location is to be checked.
     * @return true if the flag is enabled at the player's location, false otherwise.
     */
    public static boolean checkFlagClaim(Player player) {
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionQuery query = container.createQuery();
        ApplicableRegionSet regions = query.getApplicableRegions(BukkitAdapter.adapt(player.getLocation()));

        for (ProtectedRegion region : regions) {
            if(!checkClaimFlagInRegion(region)) {
            	return false;
            }
        }
        return true;
    }
}
