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
	
	public static boolean registered = false;
	public static final StateFlag SCS_CLAIM_FLAG = new StateFlag("scs-claim", true);
	
	public static void registerCustomFlag() {
		if(!registered) {
			FlagRegistry registry = WorldGuard.getInstance().getFlagRegistry();
		    registry.register(SCS_CLAIM_FLAG);
		    registered = true;
		}
	}
    
    public static boolean checkClaimFlagInRegion(ProtectedRegion region) {
        StateFlag.State flagValue = region.getFlag(SCS_CLAIM_FLAG);
        return flagValue == StateFlag.State.ALLOW;
    }
    
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
