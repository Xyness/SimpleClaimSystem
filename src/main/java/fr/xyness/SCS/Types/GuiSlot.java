package fr.xyness.SCS.Types;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import dev.lone.itemsadder.api.CustomStack;

/**
 * This class handles gui slot object.
 */
public class GuiSlot {

	
    // ***************
    // *  Variables  *
    // ***************
    
	
	/** The id associated with this slot */
	private final int id;
	
	/** The key of the slot */
	private final String key;
	
	/** The slot */
	private final int slot;
	
	/** The material of the slot */
	private final Material material;
	
	/** Is the slot a custom model data */
	private final boolean custom_model_data;
	
	/** The value of the custom model data */
	private final String custom_model_data_value;
	
	/** The title */
	private final String title;
	
	/** The lore in list */
	private final String lore;
	
	/** The action of the slot */
	private final String action;
	
	/** If the material is a custom head */
	private final boolean custom_head;
	
	/** The textures of the custom head */
	private final String custom_head_textures;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Main constructor initializing all fields.
     */
    public GuiSlot(int id, String key, int slot, Material material, boolean custom_model_data, String custom_model_data_value, 
    		String title, String lore, String action, boolean custom_head, String custom_head_textures) {
    	this.id = id;
    	this.key = key;
    	this.slot = slot;
    	this.material = material;
    	this.custom_model_data = custom_model_data;
    	this.custom_model_data_value = custom_model_data_value;
    	this.title = title;
    	this.lore = lore;
    	this.action = action;
    	this.custom_head = custom_head;
    	this.custom_head_textures = custom_head_textures;
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
     * Gets the key.
     * 
     * @return The key.
     */
    public String getKey() {
    	return key;
    }
    
    /**
     * Gets the slot.
     * 
     * @return The slot.
     */
    public int getSlot() {
    	return slot;
    }

    /**
     * Gets the item.
     * 
     * @return The item.
     */
    public Object getItem() {
    	return custom_model_data ? CustomStack.getInstance(custom_model_data_value) : new ItemStack(material);
    }

    /**
     * Gets the material.
     * 
     * @return The material.
     */
    public Material getMaterial() {
    	return material;
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
     * Gets the lore.
     * 
     * @return The lore.
     */
    public String getLore() {
    	return lore;
    }
    
    /**
     * Gets the action.
     * 
     * @return The action.
     */
    public String getAction() {
    	return action;
    }
    
    /**
     * Checks if the slot is a custom model data.
     * 
     * @return True if custom model data, false otherwise.
     */
    public boolean isCustomModel() {
    	return custom_model_data;
    }
    
    /**
     * Gets the custom model data.
     * 
     * @return The custom model data.
     */
    public String getCustomModelData() {
    	return custom_model_data_value;
    }
    
    /**
     * Checks if the slot is a custom head.
     * 
     * @return True if custom head, false otherwise.
     */
    public boolean isCustomHead() {
    	return custom_head;
    }
    
    /**
     * Gets the custom textures.
     * 
     * @return The custom textures.
     */
    public String getCustomTextures() {
    	return custom_head_textures;
    }
}
