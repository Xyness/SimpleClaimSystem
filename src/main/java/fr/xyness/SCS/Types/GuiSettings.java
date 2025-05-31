package fr.xyness.SCS.Types;

import java.util.List;

import javax.annotation.Nullable;

/**
 * This class handles slot object.
 */
public class GuiSettings {

	
    // ***************
    // *  Variables  *
    // ***************
    
	
	/** The id associated with this slot */
	private final int id;
	
	/** The rows */
	private final int rows;
	
	/** The title */
	private final String title;
	
	/** The list of slots */
	@Nullable private final List<Integer> slots;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Main constructor initializing all fields.
     */
    public GuiSettings(int id, int rows, String title, List<Integer> slots) {
    	this.id = id;
    	this.rows = rows;
    	this.title = title;
    	this.slots = slots;
    }
    
    
    // ********************
    // *  Other methods   *
    // ********************

    
    // Getters
    
    /**
     * Gets the id.
     * 
     * @return The id.
     */
    public int getId() {
    	return id;
    }
    
    /**
     * Gets the rows.
     * 
     * @return The rows.
     */
    public int getRows() {
    	return rows;
    }
    
    /**
     * Gets the title.
     * 
     * @return The title.
     */
    public String getTitle() {
    	return title;
    }
    
    /**
     * Gets the slots.
     * 
     * @return The slots.
     */
    public List<Integer> getSlots() {
    	return slots;
    }
}
