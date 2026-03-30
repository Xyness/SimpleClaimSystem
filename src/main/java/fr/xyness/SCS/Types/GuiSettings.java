package fr.xyness.SCS.Types;

import java.util.List;

import javax.annotation.Nullable;

/**
 * Represents the settings for a GUI screen (rows, title, item slots).
 */
public class GuiSettings {

	private final int id;
	private final int rows;
	private final String title;
	@Nullable private final List<Integer> slots;

    public GuiSettings(int id, int rows, String title, List<Integer> slots) {
    	this.id = id;
    	this.rows = rows;
    	this.title = title;
    	this.slots = slots;
    }

    public int getId() { return id; }
    public int getRows() { return rows; }
    public String getTitle() { return title; }
    public List<Integer> getSlots() { return slots; }
}
