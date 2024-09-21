package fr.xyness.SCS.API.Listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.xyness.SCS.Claim;

/**
 * Event that is triggered when a claim is removed.
 * This event allows other parts of the plugin to respond to the deletion of a claim.
 */
public class UnclaimEvent extends Event {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
	/** Handlers list */
    private static final HandlerList HANDLERS = new HandlerList();
    
    /** The claim */
    private final Claim claim;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Constructor for UnclaimEvent.
     *
     * @param claim The claim that has been deleted.
     */
    public UnclaimEvent(Claim claim) {
        this.claim = claim;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim that has been deleted.
     *
     * @return The deleted claim.
     */
    public Claim getClaim() {
        return claim;
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

