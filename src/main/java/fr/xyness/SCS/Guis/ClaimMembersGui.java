package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimMembersGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************

	
    private Inventory inv;
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimMembersGui(Player player, Chunk chunk, int page) {
    	String title = ClaimGuis.getGuiTitle("members").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)).replaceAll("%page%", String.valueOf(page));
    	if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("members")*9, title);
        initializeItems(player,chunk,page);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    

    // Method to initialize items for the gui
    public void initializeItems(Player player, Chunk chunk, int page) {
    	CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
    	cPlayer.setChunk(chunk);
    	cPlayer.clearMapString();
    	Claim claim = ClaimMain.getClaimFromChunk(chunk);
    	Runnable loadingtask = () -> {
	    	int min_member_slot = ClaimGuis.getGuiMinSlot("members");
	    	int max_member_slot = ClaimGuis.getGuiMaxSlot("members");
	    	int items_count = max_member_slot - min_member_slot + 1;
	    	
	        if(page > 1) {
	        	inv.setItem(ClaimGuis.getItemSlot("members", "back-page-list"), backPage(page-1));
	        } else {
	        	inv.setItem(ClaimGuis.getItemSlot("members", "back-page-settings"), backPage2());
	        }
	        
	        List<String> lore = new ArrayList<>();
	        String owner = claim.getOwner();
	        if(owner.equals("admin")) {
	        	lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("protected-area-access-lore")));
	        } else {
	        	lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("territory-access-lore")));
	        }
	        int startItem = (page - 1) * items_count;
	    	int i = min_member_slot;
	    	int count = 0;
	        for(String p : claim.getMembers()) {
	        	if (count++ < startItem) continue;
	            if(i == max_member_slot+1) { 
	            	inv.setItem(ClaimGuis.getItemSlot("members", "next-page-list"), nextPage(page+1));
	            	break;
	            }
	            List<String> lore2 = new ArrayList<>(getLoreWP(lore,p));
	            cPlayer.addMapString(i, p);
	            if(ClaimGuis.getItemCheckCustomModelData("members", "player-item")) {
	            	inv.setItem(i, createItemWMD(ClaimLanguage.getMessageWP("player-member-title",p).replace("%player%", p),
							lore2,
							ClaimGuis.getItemMaterialMD("members", "player-item"),
							ClaimGuis.getItemCustomModelData("members", "player-item")));
	            	i++;
	            	continue;
	            }
	        	if(ClaimGuis.getItemMaterialMD("members", "player-item").contains("PLAYER_HEAD")) {
	            	ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
	    	        SkullMeta meta = (SkullMeta) item.getItemMeta();
	    	        meta.setOwningPlayer(Bukkit.getOfflinePlayerIfCached(p));
	                meta.setDisplayName(ClaimLanguage.getMessageWP("player-member-title",p).replace("%player%", p));
	                if(owner.equals(p)) {
	                	List<String> lore_chef = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("owner-territory-lore",p)));
	                    meta.setLore(lore_chef);
	                } else {
	                	meta.setLore(lore2);
	                }
	                item.setItemMeta(meta);
	                inv.setItem(i, item);
	                i++;
	                continue;
	        	}
	        	ItemStack item = new ItemStack(ClaimGuis.getItemMaterial("members", "player-item"),1);
	        	ItemMeta meta = item.getItemMeta();
	        	meta.setDisplayName(ClaimLanguage.getMessageWP("player-member-title",p).replace("%player%", p));
	            if(owner.equals(p)) {
	            	List<String> lore_chef = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("owner-territory-lore",p)));
	                meta.setLore(lore_chef);
	            } else {
	            	meta.setLore(lore2);
	            }
	            item.setItemMeta(meta);
	            inv.setItem(i, item);
	            i++;
	        }
	        
	    	Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("members"));
	    	for(String key : custom_items) {
	    		lore = new ArrayList<>(getLoreP(ClaimGuis.getCustomItemLore("members", key),player));
	    		String title = ClaimGuis.getCustomItemTitle("members", key);
	    		if(ClaimSettings.getBooleanSetting("placeholderapi")) {
	    			title = PlaceholderAPI.setPlaceholders(player, title);
	    		}
				if(ClaimGuis.getCustomItemCheckCustomModelData("members", key)) {
					inv.setItem(ClaimGuis.getCustomItemSlot("members", key), createItemWMD(title,
							lore,
							ClaimGuis.getCustomItemMaterialMD("members", key),
							ClaimGuis.getCustomItemCustomModelData("members", key)));
				} else {
					inv.setItem(ClaimGuis.getCustomItemSlot("members", key), createItem(ClaimGuis.getCustomItemMaterial("members", key),
							title,
							lore));
				}
	    	}
    	};
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> loadingtask.run());
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), loadingtask);
    	}

    }
    
    // Method to split lore for lines
    public static List<String> getLore(String lore){
    	List<String> lores = new ArrayList<>();
    	String[] parts = lore.split("\n");
    	for(String s : parts) {
    		lores.add(s);
    	}
    	return lores;
    }
    
    // Method to split the lore for lines with placeholders from PAPI
    public static List<String> getLoreP(String lore, Player player){
    	if(!ClaimSettings.getBooleanSetting("placeholderapi")) {
    		return getLore(lore);
    	}
    	List<String> lores = new ArrayList<>();
    	String[] parts = lore.split("\n");
    	for(String s : parts) {
    		lores.add(PlaceholderAPI.setPlaceholders(player, s));
    	}
    	return lores;
    }
    
    // Method to get the lore with placeholders from PAPI
    public static List<String> getLoreWP(List<String> lore, String player){
    	if(!ClaimSettings.getBooleanSetting("placeholderapi")) return lore;
    	List<String> lores = new ArrayList<>();
    	Player p = Bukkit.getPlayer(player);
    	if(p == null) {
    		OfflinePlayer o_offline = Bukkit.getOfflinePlayerIfCached(player);
        	for(String s : lore) {
        		lores.add(PlaceholderAPI.setPlaceholders(o_offline, s));
        	}
        	return lores;
    	}
    	for(String s : lore) {
    		lores.add(PlaceholderAPI.setPlaceholders(p, s));
    	}

    	return lores;
    }
    
    // Method to create item in the gui
    private ItemStack createItem(Material material, String name, List<String> lore) {
    	ItemStack item = null;
    	if(material == null) {
        	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
        	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
        	item = new ItemStack(Material.STONE,1);
    	} else {
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Method to create custom item in the gui
    private ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if(customStack == null) {
        	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+name_custom_item);
        	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
        	item = new ItemStack(Material.STONE,1);
        } else {
        	item = customStack.getItemStack();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(model_data);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Back page slot
    private ItemStack backPage(int page) {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("members", "back-page-list")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("members", "back-page-list"));
            if(customStack == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+ClaimGuis.getItemMaterialMD("members", "back-page-list"));
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("members", "back-page-list");
    		if(material == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-lore").replaceAll("%page%", String.valueOf(page))));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    // Back page 2 slot
    private ItemStack backPage2() {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("members", "back-page-settings")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("members", "back-page-settings"));
            if(customStack == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+ClaimGuis.getItemMaterialMD("members", "back-page-settings"));
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("members", "back-page-settings");
    		if(material == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-settings-title"));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-settings-lore")));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    // Next page slot
    private ItemStack nextPage(int page) {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("members", "next-page-list")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("members", "next-page-list"));
            if(customStack == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+ClaimGuis.getItemMaterialMD("members", "next-page-list"));
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("members", "next-page-list");
    		if(material == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check members.yml");
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("next-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("next-page-lore").replaceAll("%page%", String.valueOf(page))));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    @Override
    public Inventory getInventory() {
        return inv;
    }
    
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
