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
    private static Map<Player,Map<Integer,Location>> claimsLoc = new HashMap<>();
    private static Map<Player,Map<Integer,Chunk>> claimsChunk = new HashMap<>();
    private static Map<Player,Chunk> lastChunkSetting = new HashMap<>();
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimListGui(Player player, int page) {
    	String title = ClaimGuis.getGuiTitle("list").replaceAll("%page%", String.valueOf(page));
    	if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("list")*9, title);
        initializeItems(player,page);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
    // Method to set last chunk for a player
    public static void setLastChunk(Player player, Chunk chunk) {
    	lastChunkSetting.put(player, chunk);
    }
    
    // Method to get the last chunk for a player
    public static Chunk getLastChunk(Player player) {
    	return lastChunkSetting.get(player);
    }
    
    // Method to remove the last chunk for a player
    public static void removeLastChunk(Player player) {
    	if(lastChunkSetting.containsKey(player)) {
    		lastChunkSetting.remove(player);
    	}
    }
    
    // Method to get the claim location (by slot) for a player
    public static Location getClaimLoc(Player player, int slot) {
    	return claimsLoc.get(player).get(slot);
    }
    
    // Method to remove the claim location for a player
    public static void removeClaimsLoc(Player player) {
    	if(claimsLoc.containsKey(player)) {
    		claimsLoc.remove(player);
    	}
    }
    
    // Method to get the claim chunk (by slot) for a player
    public static Chunk getClaimChunk(Player player, int slot) {
    	return claimsChunk.get(player).get(slot);
    }
    
    // Method to remove the claim chunk for a player
    public static void removeClaimsChunk(Player player) {
    	if(claimsChunk.containsKey(player)) {
    		claimsChunk.remove(player);
    	}
    }

    // Method to initialize items for the gui
    public void initializeItems(Player player, int page) {
    	
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(ClaimMain.getPlugin(), task -> {
    			int min_member_slot = ClaimGuis.getGuiMinSlot("list");
    	    	int max_member_slot = ClaimGuis.getGuiMaxSlot("list");
    	    	int items_count = max_member_slot - min_member_slot + 1;
    	    	String playerName = player.getName();
    	    	
    	        if(page > 1) {
    	        	inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage(page-1));
    	        } else if (lastChunkSetting.containsKey(player)) {
    	        	inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage2(lastChunkSetting.get(player)));
    	        }
    	        
    	        Set<Chunk> claims = ClaimMain.getChunksFromOwner(player.getName());
    	        List<String> lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-lore",playerName)));
    	        Map<Integer,Location> claims_loc = new HashMap<>();
    	        Map<Integer,Chunk> claims_chunk = new HashMap<>();
    	        int startItem = (page - 1) * items_count;
    	    	int i = min_member_slot;
    	    	int count = 0;
    	        for(Chunk c : claims) {
    	        	if (count++ < startItem) continue;
    	            if(i == max_member_slot+1) { 
    	            	inv.setItem(ClaimGuis.getItemSlot("list", "next-page-list"), nextPage(page+1));
    	            	break;
    	            }
    	            claims_chunk.put(i, c);
    	            claims_loc.put(i, ClaimMain.getClaimLocationByChunk(c));
    	            List<String> used_lore = new ArrayList<>();
    	            for(String s : lore) {
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
    	            
    	            
    	            if(ClaimGuis.getItemCheckCustomModelData("list", "claim-item")) {
    	            	inv.setItem(i, createItemWMD(ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))),
    	            			used_lore,
    	            			ClaimGuis.getItemMaterialMD("list", "claim-item"),
    	            			ClaimGuis.getItemCustomModelData("list", "claim-item")));
    	            	i++;
    	            	continue;
    	            }
    	            if(ClaimGuis.getItemMaterialMD("list", "claim-item").contains("PLAYER_HEAD")) {
    	            	ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
    	    	        SkullMeta meta = (SkullMeta) item.getItemMeta();
    	                meta.setOwner(player.getName());
    	                meta.setDisplayName(ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))));
    	                meta.setLore(used_lore);
    	                item.setItemMeta(meta);
    	                inv.setItem(i, item);
    	                i++;
    	                continue;
    	            }
    	            inv.setItem(i, createItem(ClaimGuis.getItemMaterial("list", "claim-item"), ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))),used_lore));
    	            i++;
    	        }
    	        claimsChunk.put(player, claims_chunk);
    	        claimsLoc.put(player, claims_loc);
    	        
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
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(ClaimMain.getPlugin(), task -> {
    			int min_member_slot = ClaimGuis.getGuiMinSlot("list");
    	    	int max_member_slot = ClaimGuis.getGuiMaxSlot("list");
    	    	int items_count = max_member_slot - min_member_slot + 1;
    	    	String playerName = player.getName();
    	    	
    	        if(page > 1) {
    	        	inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage(page-1));
    	        } else if (lastChunkSetting.containsKey(player)) {
    	        	inv.setItem(ClaimGuis.getItemSlot("list", "back-page-list"), backPage2(lastChunkSetting.get(player)));
    	        }
    	        
    	        Set<Chunk> claims = ClaimMain.getChunksFromOwner(player.getName());
    	        List<String> lore = new ArrayList<>(getLore(ClaimLanguage.getMessageWP("access-claim-lore",playerName)));
    	        Map<Integer,Location> claims_loc = new HashMap<>();
    	        Map<Integer,Chunk> claims_chunk = new HashMap<>();
    	        int startItem = (page - 1) * items_count;
    	    	int i = min_member_slot;
    	    	int count = 0;
    	        for(Chunk c : claims) {
    	        	if (count++ < startItem) continue;
    	            if(i == max_member_slot+1) { 
    	            	inv.setItem(ClaimGuis.getItemSlot("list", "next-page-list"), nextPage(page+1));
    	            	break;
    	            }
    	            claims_chunk.put(i, c);
    	            claims_loc.put(i, ClaimMain.getClaimLocationByChunk(c));
    	            List<String> used_lore = new ArrayList<>();
    	            for(String s : lore) {
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
    	            
    	            
    	            if(ClaimGuis.getItemCheckCustomModelData("list", "claim-item")) {
    	            	inv.setItem(i, createItemWMD(ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))),
    	            			used_lore,
    	            			ClaimGuis.getItemMaterialMD("list", "claim-item"),
    	            			ClaimGuis.getItemCustomModelData("list", "claim-item")));
    	            	i++;
    	            	continue;
    	            }
    	            if(ClaimGuis.getItemMaterialMD("list", "claim-item").contains("PLAYER_HEAD")) {
    	            	ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
    	    	        SkullMeta meta = (SkullMeta) item.getItemMeta();
    	                meta.setOwner(player.getName());
    	                meta.setDisplayName(ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))));
    	                meta.setLore(used_lore);
    	                item.setItemMeta(meta);
    	                inv.setItem(i, item);
    	                i++;
    	                continue;
    	            }
    	            inv.setItem(i, createItem(ClaimGuis.getItemMaterial("list", "claim-item"), ClaimLanguage.getMessageWP("access-claim-title",playerName).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c))),used_lore));
    	            i++;
    	        }
    	        claimsChunk.put(player, claims_chunk);
    	        claimsLoc.put(player, claims_loc);
    	        
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
    		});
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
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check list.yml");
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
        	item = new ItemStack(Material.STONE,1);
    	} else {
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Method to create custom item in the gui
    private ItemStack createItemWMD(String name, List<String> lore, String name_custom_item, int model_data) {
        CustomStack customStack = CustomStack.getInstance(name_custom_item);
        ItemStack item = null;
        if(customStack == null) {
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+name_custom_item);
        	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
        	item = new ItemStack(Material.STONE,1);
        } else {
        	item = customStack.getItemStack();
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(model_data);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
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
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("list", "back-page-list"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("list", "back-page-list");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check list.yml");
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-lore").replaceAll("%page%", String.valueOf(page))));

            // Masquer les attributs comme la défense d'armure
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

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
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("list", "back-page-settings"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("list", "back-page-settings");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check list.yml");
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-chunk-title"));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-chunk-lore").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))));

            // Masquer les attributs comme la défense d'armure
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

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
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("list", "next-page-list"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("list", "next-page-list");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check list.yml");
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("next-page-title").replaceAll("%page%", String.valueOf(page)));
            meta.setLore(getLore(ClaimLanguage.getMessage("next-page-lore").replaceAll("%page%", String.valueOf(page))));

            // Masquer les attributs comme la défense d'armure
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

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
