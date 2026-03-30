package fr.xyness.SCS.API.Listeners;

import java.util.Set;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.xyness.SCS.Types.Claim;

/**
 * Event that is triggered when all claims from an owner are removed.
 */
public class UnclaimallEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Set<Claim> claims;

    public UnclaimallEvent(Set<Claim> claims) {
        this.claims = claims;
    }

    public Set<Claim> getClaims() {
        return claims;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
