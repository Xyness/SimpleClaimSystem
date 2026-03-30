package fr.xyness.SCS.Types;

import org.bukkit.Material;

/**
 * Represents a single slot configuration in a GUI.
 */
public class GuiSlot {

	private final int id;
	private final String key;
	private final int slot;
	private final Material material;
	private final boolean custom_model_data;
	private final String custom_model_data_value;
	private final String title;
	private final String lore;
	private final String action;
	private final boolean custom_head;
	private final String custom_head_textures;

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

    public int getId() { return id; }
    public String getKey() { return key; }
    public int getSlot() { return slot; }
    public Material getMaterial() { return material; }
    public String getTitle() { return title; }
    public String getLore() { return lore; }
    public String getAction() { return action; }
    public boolean isCustomModel() { return custom_model_data; }
    public String getCustomModelData() { return custom_model_data_value; }
    public boolean isCustomHead() { return custom_head; }
    public String getCustomTextures() { return custom_head_textures; }
}
