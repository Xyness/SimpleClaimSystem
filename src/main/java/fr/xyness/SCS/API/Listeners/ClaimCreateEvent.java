package fr.xyness.SCS.API.Listeners;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import fr.xyness.SCS.Types.Claim;

/**
 * Event that is triggered when a new claim is created.
 * This event allows other parts of the plugin to respond to the creation of a new claim.
 */
public class ClaimCreateEvent extends Event {

	
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
     * Constructor for ClaimCreateEvent.
     *
     * @param claim The claim that has been created.
     */
    public ClaimCreateEvent(Claim claim) {
        this.claim = claim;
    }
    
    
    // *******************
    // *  Other methods  *
    // *******************
    

    /**
     * Gets the claim that has been created.
     *
     * @return The created claim.
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

