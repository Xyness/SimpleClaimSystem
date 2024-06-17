package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
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
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimListGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************
	

    private Inventory inv;
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimListGui(Player player, int page, String filter) {
    	String title = ClaimGuis.getGuiTitle("list").replaceAll("%page%", String.valueOf(page));
    	if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("list")*9, title);
        initializeItems(player,page,filter);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    

    // Method to initialize items for the gui
    public void initializeItems(Player player, int page, String filter) {

		int min_member_slot = ClaimGuis.getGuiMinSlot("list");
    	int max_member_slot = ClaimGuis.getGuiMaxSlot("list");
    	int items_count = max_member_slot - min_member_slot + 1;
    	String playerName = player.getName();
    	CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
    	cPlayer.setFilter(filter);
    	cPlayer.clearMapChunk();
    	cPlayer.clearMapLoc();
    	
        if(page > 1) {
        	inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage(page-1));
        } else if (cPlayer.getChunk() != null) {
        	inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage2(cPlayer.getChunk()));
        }
        
        inv.setItem(ClaimGuis.getItemSlot("list", "filter"), createFilterItem(filter));
        
        Set<Chunk> claims;
        List<String> lore;
        if(filter.equals("owner")) {
        	claims = new HashSet<>(ClaimMain.getChunksFromOwner(playerName));
        	lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-lore",playerName)));
        } else {
        	claims = new HashSet<>(ClaimMain.getChunksWhereMemberNotOwner(playerName));
        	lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-not-owner-lore",playerName)));
        } 
        int startItem = (page - 1) * items_count;
    	int i = min_member_slot;
    	int count = 0;
        for(Chunk c : claims) {
        	if (count++ < startItem) continue;
            if(i == max_member_slot+1) { 
            	inv.setItem(ClaimGuis.getItemSlot("list", "next-page-list"), nextPage(page+1));
            	break;
            }
            cPlayer.addMapChunk(i, c);
            cPlayer.addMapLoc(i, ClaimMain.getClaimLocationByChunk(c));
            List<String> used_lore = new ArrayList<>();
            for(String s : lore) {
            	s = s.replaceAll("%owner%", ClaimMain.getOwnerInClaim(c));
            	s = s.replaceAll("%description%", ClaimMain.getClaimDescription(c));
            	s = s.replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c)));
            	if(s.contains("%members%")) {
            		String members = getMembers(c);
            		if(members.contains("\n")) {
                    	String[] parts = members.split("\n");
                    	for(String ss : parts) {
                    		used_lore.add(ss);
                    	}
                    } else {
                    	used_lore.add(members);
                    }
            	} else {
            		used_lore.add(s);
            	}
            }
            if(filter.equals("owner")) {
	            if(ClaimSettings.getBooleanSetting("economy")) {
		            if(ClaimMain.claimIsInSale(c)) {
		            	String[] m = ClaimLanguage.getMessageWP("my-claims-buyable-price",playerName).replaceAll("%price%", String.valueOf(ClaimMain.getClaimPrice(c))).split("\n");
		            	for(String part : m) {
		            		used_lore.add(part);
		            	}
		            	used_lore.addAll(getLore(ClaimLanguage.getMessage("access-claim-clickable")));
		            	used_lore.add(ClaimLanguage.getMessage("my-claims-clickable-cancel-sale"));
		            } else {
		            	used_lore.addAll(getLore(ClaimLanguage.getMessage("access-claim-clickable")));
		            }
	            } else {
	            	used_lore.addAll(getLore(ClaimLanguage.getMessage("access-claim-clickable")));
	            }
            } else {
            	used_lore.addAll(getLore(ClaimLanguage.getMessage("access-claim-clickable-not-owner")));
            }
            
        	final int i_final = i;
        	final List<String> used_lore_final = used_lore;
            
            if(ClaimGuis.getItemCheckCustomModelData("list", "claim-item")) {
            	inv.setItem(i_final, createItemWMD(ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))),
            			used_lore_final,
            			ClaimGuis.getItemMaterialMD("list", "claim-item"),
            			ClaimGuis.getItemCustomModelData("list", "claim-item")));
            	i++;
            	continue;
            }
            if(ClaimGuis.getItemMaterialMD("list", "claim-item").contains("PLAYER_HEAD")) {
            	ItemStack item = new ItemStack(Material.PLAYER_HEAD);
    	        SkullMeta meta = (SkullMeta) item.getItemMeta();
    	        meta.setOwningPlayer(player);
                meta.setDisplayName(ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))));
                meta.setLore(used_lore_final);
                item.setItemMeta(meta);
                inv.setItem(i_final, item);
                i++;
                continue;
            }
            inv.setItem(i_final, createItem(ClaimGuis.getItemMaterial("list", "claim-item"), ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))),used_lore_final));
            i++;
        }
        
    	Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("list"));
    	for(String key : custom_items) {
    		lore = new ArrayList<>(getLoreWP(ClaimGuis.getCustomItemLore("list", key),player));
    		String title = ClaimGuis.getCustomItemTitle("list", key);
    		if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    			title = PlaceholderAPI.setPlaceholders(player, title);
    		}
			if(ClaimGuis.getCustomItemCheckCustomModelData("list", key)) {
				inv.setItem(ClaimGuis.getCustomItemSlot("list", key), createItemWMD(title,
						lore,
						ClaimGuis.getCustomItemMaterialMD("list", key),
						ClaimGuis.getCustomItemCustomModelData("list", key)));
			} else {
				inv.setItem(ClaimGuis.getCustomItemSlot("list", key), createItem(ClaimGuis.getCustomItemMaterial("list", key),
						title,
						lore));
			}
    	}
    }
    
    // Method to get members from a claim chunk
    public static String getMembers(Chunk chunk) {
    	Set<String> members = ClaimMain.getClaimMembers(chunk);
        if(members.isEmpty()) {
        	return ClaimLanguage.getMessage("claim-list-no-member");
        }
        StringBuilder factionsList = new StringBuilder();
        int i = 0;
    	for(String membre : ClaimMain.getClaimMembers(chunk)) {
    		Player p = Bukkit.getPlayer(membre);
    		String fac = "§a"+membre;
    		if(p == null) {
    			fac = "§c"+membre;
    		}
    		factionsList.append(fac);
            if (i < members.size() - 1) {
            	factionsList.append("§7, ");
            }
            if ((i + 1) % 4 == 0 && i < members.size() - 1) {
                factionsList.append("\n");
            }
            i++;
    	}
    	String result = factionsList.toString();
    	return result;
    }
    
    // Method to split the lore for lines
    public static List<String> getLore(String lore){
    	List<String> lores = new ArrayList<>();
    	String[] parts = lore.split("\n");
    	for(String s : parts) {
    		lores.add(s);
    	}
    	return lores;
    }
    
    // Method to get the lore with placeholders from PAPI
    public static List<String> getLoreWP(String lore, Player player){
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

    // Method to create item in the gui
    private ItemStack createItem(Material material, String name, List<String> lore) {
    	ItemStack item = null;
    	if(material == null) {
        	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
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
    	if(ClaimGuis.getItemCheckCustomModelData("list", "back-page-list")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "back-page-list"));
            if(customStack == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+ClaimGuis.getItemMaterialMD("list", "back-page-list"));
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("list", "back-page-list");
    		if(material == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
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
    private ItemStack backPage2(Chunk chunk) {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("list", "back-page-settings")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "back-page-settings"));
            if(customStack == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+ClaimGuis.getItemMaterialMD("list", "back-page-settings"));
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("list", "back-page-settings");
    		if(material == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-chunk-title"));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-chunk-lore").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    // Next page slot
    private ItemStack nextPage(int page) {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("list", "next-page-list")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "next-page-list"));
            if(customStack == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error custom item loading : "+ClaimGuis.getItemMaterialMD("list", "next-page-list"));
            	SimpleClaimSystem.getInstance().getLogger().info("Using STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("list", "next-page-list");
    		if(material == null) {
            	SimpleClaimSystem.getInstance().getLogger().info("Error material loading, check list.yml");
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
    
    // Method to create the filter item
    private ItemStack createFilterItem(String filter) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("list", "filter")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "filter"));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("list", "filter");
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = ClaimLanguage.getMessage("filter-list-lore");
            if (filter.equals("not_owner")) {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                    .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"));
            } else {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
                    .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(loreFilter));
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
