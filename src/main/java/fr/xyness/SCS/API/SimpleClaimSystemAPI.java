package fr.xyness.SCS.API;

import fr.xyness.SCS.Claim;

public interface SimpleClaimSystemAPI {
	Claim getPlayerClaim(String player, String claim_name);
}
