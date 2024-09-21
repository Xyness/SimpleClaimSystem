package fr.xyness.SCS.API.Listeners;

import java.util.Set;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.xyness.SCS.Claim;

/**
 * Event that is triggered when a claim is removed.
 * This event allows other parts of the plugin to respond to the deletion of claims.
 */
public class UnclaimallEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claims set */
    private final Set<Claim> claims;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for UnclaimEvent.
     *
     * @param claims The claims that has been deleted.
     */
    public UnclaimallEvent(Set<Claim> claims) {
        this.claims = claims;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claims that has been deleted.
     *
     * @return The deleted claim.
     */
    public Set<Claim> getClaims() {
        return claims;
    }

    /**
     * Gets the list of handlers for this event.
     *
     * @return The handler list.
     */
    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    /**
     * Gets the static handler list for this event.
     *
     * @return The static handler list.
     */
    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

