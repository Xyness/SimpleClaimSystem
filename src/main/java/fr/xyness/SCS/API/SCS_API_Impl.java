package fr.xyness.SCS.API;

import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;

public class SCS_API_Impl implements SimpleClaimSystemAPI {
	
	private SimpleClaimSystem instance;
	
	public SCS_API_Impl(SimpleClaimSystem instance) {
		this.instance = instance;
	}

	@Override
	public Claim getPlayerClaim(String playerName, String claim_name) {
		return instance.getMain().getClaimByName(claim_name, playerName);
	}
	
}
