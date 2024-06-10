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

public class ClaimWorldGuard {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	public static boolean registered = false;
	public static final StateFlag SCS_CLAIM_FLAG = new StateFlag("scs-claim", true);
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to register the flag "scs-claim"
	public static void registerCustomFlag() {
		if(!registered) {
			FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		    registry.register(SCS_CLAIM_FLAG);
		    registered = true;
		}
	}
    
	// Method to check if the flag "scs-claim" is enable/disable in a region
    public static boolean checkClaimFlagInRegion(ProtectedRegion region) {
        StateFlag.State flagValue = region.getFlag(SCS_CLAIM_FLAG);
        return flagValue == StateFlag.State.ALLOW;
    }
    
    // Method to check if the flag "scs-claim" is enable/disable at the player's location
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
