package fr.xyness.SCS.API.Listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.xyness.SCS.Types.Claim;

/**
 * Event that is triggered when a new claim is created.
 */
public class ClaimCreateEvent extends Event {

    private static final HandlerList HANDLERS = new HandlerList();
    private final Claim claim;

    public ClaimCreateEvent(Claim claim) {
        this.claim = claim;
    }

    public Claim getClaim() {
        return claim;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
