package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
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
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimsOwnerGui implements InventoryHolder {

	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private Inventory inv;
    private static Map<Player,Map<Integer,Location>> claimsLoc = new HashMap<>();
    private static Map<Player,Map<Integer,Chunk>> claimsChunk = new HashMap<>();
    private static Map<Player,String> playerFilter = new HashMap<>();
    private static Map<Player,String> owner = new HashMap<>();
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimsOwnerGui(Player player, int page, String filter, String owner) {
    	String title = ClaimGuis.getGuiTitle("claims_owner").replaceAll("%page%", String.valueOf(page)).replaceAll("%owner%", owner);
    	if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims_owner")*9, title);
        initializeItems(player,page,filter,owner);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
    // Get the owner (by slot) for a player
    public static String getOwner(Player player) {
    	return owner.get(player);
    }
    
    // Remove the owner for a player
    public static void removeOwner(Player player) {
    	if(owner.containsKey(player)) {
    		owner.remove(player);
    	}
    }
    
    // Get the player's filter
    public static String getPlayerFilter(Player player) {
    	return playerFilter.get(player);
    }
    
    // Remove the player's filter
    public static void removePlayerFilter(Player player) {
    	if(playerFilter.containsKey(player)) {
    		playerFilter.remove(player);
    	}
    }
    
    // Get the claim location (by slot) for a player
    public static Location getClaimLoc(Player player, int slot) {
    	return claimsLoc.get(player).get(slot);
    }
    
    // Remove the claim location for a player
    public static void removeClaimsLoc(Player player) {
    	if(claimsLoc.containsKey(player)) {
    		claimsLoc.remove(player);
    	}
    }
    
    // Get the claim chunk (by slot) for a player
    public static Chunk getClaimChunk(Player player, int slot) {
    	return claimsChunk.get(player).get(slot);
    }
    
    // Remove the claim chunk for a player
    public static void removeClaimsChunk(Player player) {
    	if(claimsChunk.containsKey(player)) {
    		claimsChunk.remove(player);
    	}
    }

    // Method to initialize items for the gui
    public void initializeItems(Player player, int page, String filter, String owner) {
    	
    	this.owner.put(player, owner);
    	
		int min_member_slot = ClaimGuis.getGuiMinSlot("claims_owner");
    	int max_member_slot = ClaimGuis.getGuiMaxSlot("claims_owner");
    	int items_count = max_member_slot - min_member_slot + 1;
    	playerFilter.put(player, filter);
    	
        if(page > 1) {
        	inv.setItem(ClaimGuis.getItemSlot("claims_owner", "back-page-list"), backPage(page-1));
        } else {
        	inv.setItem(ClaimGuis.getItemSlot("claims_owner", "back-page-claims"), backPage2());
        }
        
        Set<Chunk> claims;
        if(filter.equals("sales")) {
        	claims = ClaimMain.getChunksinSaleFromOwner(owner);
        } else {
        	claims = ClaimMain.getChunksFromOwner(owner);
        }
        inv.setItem(ClaimGuis.getItemSlot("claims_owner", "filter"), filter(filter));
        
        List<String> lore = new ArrayList<>(getLore(ClaimLanguage.getMessage("access-all-claim-lore")));
        Map<Integer,Location> claims_loc = new HashMap<>();
        Map<Integer,Chunk> claims_chunk = new HashMap<>();
        int startItem = (page - 1) * items_count;
    	int i = min_member_slot;
    	int count = 0;
        for(Chunk c : claims) {
        	if (count++ < startItem) continue;
            if(i == max_member_slot+1) { 
            	inv.setItem(ClaimGuis.getItemSlot("claims_owner", "next-page-list"), nextPage(page+1));
            	break;
            }
            if(!ClaimMain.canPermCheck(c, "Visitors") && !ClaimSettings.getBooleanSetting("claims-visitors-off-visible")) continue;
            List<String> lore2 = new ArrayList<>();
            for(String s : lore) {
            	s = s.replaceAll("%description%", ClaimMain.getClaimDescription(c));
            	s = s.replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c)));
            	if(s.contains("%members%")) {
            		String members = getMembers(c);
            		if(members.contains("\n")) {
                    	String[] parts = members.split("\n");
                    	for(String ss : parts) {
                    		lore2.add(ss);
                    	}
                    } else {
                    	lore2.add(members);
                    }
            	} else {
            		lore2.add(s);
            	}
            }
            if(ClaimSettings.getBooleanSetting("economy")) {
	            if(ClaimMain.claimIsInSale(c)) {
	            	String[] m = ClaimLanguage.getMessageWP("all-claim-buyable-price",ClaimMain.getOwnerInClaim(c)).replaceAll("%price%", String.valueOf(ClaimMain.getClaimPrice(c))).split("\n");
	            	for(String part : m) {
	            		lore2.add(part);
	            	}
	            	lore2.add(ClaimLanguage.getMessage("all-claim-is-buyable"));
	            }
            }
            if(ClaimMain.canPermCheck(c, "Visitors") || ClaimMain.getOwnerInClaim(c).equals(player.getName())) {
            	lore2.add(ClaimLanguage.getMessage("access-all-claim-lore-allow-visitors"));
            } else {
            	lore2.add(ClaimLanguage.getMessage("access-all-claim-lore-deny-visitors"));
            }
            lore2 = getLoreWP(lore2,ClaimMain.getOwnerInClaim(c));
            claims_chunk.put(i, c);
            claims_loc.put(i, ClaimMain.getClaimLocationByChunk(c));

        	String displayName = ClaimLanguage.getMessageWP("access-all-claim-title",ClaimMain.getOwnerInClaim(c)).replaceAll("%owner%", ClaimMain.getOwnerInClaim(c)).replaceAll("%name%", ClaimMain.getClaimNameByChunk(c)).replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(c)));
            
            if(ClaimGuis.getItemCheckCustomModelData("claims_owner", "claim-item")) {
            	inv.setItem(i, createItemWMD(displayName,
						lore2,
						ClaimGuis.getItemMaterialMD("claims_owner", "claim-item"),
						ClaimGuis.getItemCustomModelData("claims_owner", "claim-item")));
            	i++;
            	continue;
            }
        	if(ClaimGuis.getItemMaterialMD("claims_owner", "claim-item").contains("PLAYER_HEAD")) {
        		ItemStack item = CacheGui.getPlayerHead(owner);
                SkullMeta meta = (SkullMeta) item.getItemMeta();
                meta.setDisplayName(displayName);
                meta.setLore(lore2);
                item.setItemMeta(meta);
                inv.setItem(i, item);
                i++;
                continue;
        	}
        	ItemStack item = new ItemStack(ClaimGuis.getItemMaterial("claims_owner", "claim-item"),1);
        	ItemMeta meta = item.getItemMeta();
        	meta.setDisplayName(displayName);
        	meta.setLore(lore2);
            item.setItemMeta(meta);
            inv.setItem(i, item);
            i++;
        }
        claimsChunk.put(player, claims_chunk);
        claimsLoc.put(player, claims_loc);
        
    	Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("claims_owner"));
    	for(String key : custom_items) {
    		lore = new ArrayList<>(getLoreP(ClaimGuis.getCustomItemLore("claims_owner", key),player));
    		String title = ClaimGuis.getCustomItemTitle("claims_owner", key);
    		if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    			title = PlaceholderAPI.setPlaceholders(player, title);
    		}
			if(ClaimGuis.getCustomItemCheckCustomModelData("claims_owner", key)) {
				inv.setItem(ClaimGuis.getCustomItemSlot("claims_owner", key), createItemWMD(title,
						lore,
						ClaimGuis.getCustomItemMaterialMD("claims_owner", key),
						ClaimGuis.getCustomItemCustomModelData("claims_owner", key)));
			} else {
				inv.setItem(ClaimGuis.getCustomItemSlot("claims_owner", key), createItem(ClaimGuis.getCustomItemMaterial("claims", key),
						title,
						lore));
			}
    	}
    }
    
    // Method to get members from a claim chunk
    public static String getMembers(Chunk chunk) {
    	Set<String> members = ClaimMain.getClaimMembers(chunk);
        if(!(members.size()>0)) {
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
    		OfflinePlayer o_offline = Bukkit.getOfflinePlayer(player);
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
    	if(ClaimGuis.getItemCheckCustomModelData("claims", "back-page-list")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", "back-page-list"));
            if(customStack == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("claims", "back-page-list"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("claims", "back-page-list");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check claims.yml");
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
    private ItemStack backPage2() {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("claims_owner", "back-page-claims")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("list", "back-page-claims"));
            if(customStack == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("claims_owner", "back-page-claims"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("claims_owner", "back-page-claims");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check list.yml");
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title"));
            meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-claims-lore")));

            // Masquer les attributs comme la défense d'armure
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

            item.setItemMeta(meta);
        }

        return item;
    }
    
    // Filter slot
    private ItemStack filter(String filter) {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("claims_owner", "filter")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims_owner", "filter"));
            if(customStack == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("claims_owner", "filter"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("claims_owner", "filter");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check claims.yml");
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String lore_filter = ClaimLanguage.getMessage("filter-owner-lore");
            if(filter.equals("sales")) {
            	lore_filter = lore_filter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter")).replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"));
            } else {
            	lore_filter = lore_filter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter")).replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(lore_filter));

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
    	if(ClaimGuis.getItemCheckCustomModelData("claims", "next-page-list")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", "next-page-list"));
            if(customStack == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("claims", "next-page-list"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("claims", "next-page-list");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check claims.yml");
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