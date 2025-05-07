package fr.xyness.SCS.Types;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Inventory slots used as a GUI.
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
	
	/** The start slot */
	@Nullable private final int list_start_slot;
	
	/** The end slot */
	@Nullable private final int list_end_slot;


	// ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Main constructor initializing all fields.
     */
    public GuiSettings(int id, int rows, String title, int list_start_slot, int list_end_slot) {
    	this.id = id;
    	this.rows = rows;
    	this.title = title;
    	this.list_start_slot = list_start_slot;
    	this.list_end_slot = list_end_slot;
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
     * Gets the start slot.
     * 
     * @return The start slot.
     */
    public int getStartSlot() {
    	return list_start_slot;
    }
    
    /**
     * Gets the end slot.
     * 
     * @return The end slot.
     */
    public int getEndSlot() {
    	return list_end_slot;
    }
}
