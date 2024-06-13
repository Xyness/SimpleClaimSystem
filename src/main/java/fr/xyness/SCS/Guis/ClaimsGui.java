package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
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
import fr.xyness.SCS.Others.CacheGui;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimsGui implements InventoryHolder {

	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private Inventory inv;
    private static Map<Player,Map<Integer,String>> players = new HashMap<>();
    private static Map<Player,String> playerFilter = new HashMap<>();
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimsGui(Player player, int page, String filter) {
    	String title = ClaimGuis.getGuiTitle("claims").replaceAll("%page%", String.valueOf(page));
    	if(ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims")*9, title);
        initializeItems(player,page,filter);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
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
    
    // Get the owner (by slot) for a player
    public static String getOwner(Player player, int slot) {
    	return players.get(player).get(slot);
    }
    
    // Remove the owner for a player
    public static void removeOwner(Player player) {
    	if(players.containsKey(player)) {
    		players.remove(player);
    	}
    }

    // Method to initialize items for the GUI
    public void initializeItems(Player player, int page, String filter) {
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(ClaimMain.getPlugin(), task -> {
                loadItems(player, page, filter);
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(ClaimMain.getPlugin(), task -> {
                loadItems(player, page, filter);
            });
        }
    }

    // Method to load items
    private void loadItems(Player player, int page, String filter) {
        int min_member_slot = ClaimGuis.getGuiMinSlot("claims");
        int max_member_slot = ClaimGuis.getGuiMaxSlot("claims");
        int items_count = max_member_slot - min_member_slot + 1;
        playerFilter.put(player, filter);

        if (page > 1) {
            inv.setItem(ClaimGuis.getItemSlot("claims", "back-page-list"), backPage(page - 1));
        }

        List<String> loreTemplate = new ArrayList<>(getLore(ClaimLanguage.getMessage("owner-claim-lore")));
        Map<Integer, String> owners = new HashMap<>();

        Set<String> powners = getPownersByFilter(filter);

        inv.setItem(ClaimGuis.getItemSlot("claims", "filter"), filter(filter));

        int startItem = (page - 1) * items_count;
        int i = min_member_slot;
        int count = 0;

        for (String owner : powners) {
            if (count++ < startItem) continue;
            if (i == max_member_slot + 1) {
                inv.setItem(ClaimGuis.getItemSlot("claims", "next-page-list"), nextPage(page + 1));
                break;
            }

            List<String> lore = new ArrayList<>();
            for (String s : loreTemplate) {
                lore.add(s.replaceAll("%claim-amount%", String.valueOf(ClaimMain.getPlayerClaimsCount(owner))));
            }
            lore.add(ClaimLanguage.getMessage("owner-claim-access"));
            lore = getLoreWP(lore, owner);
            owners.put(i, owner);

            ItemStack item = createOwnerClaimItem(owner, lore);
            inv.setItem(i, item);
            i++;
        }

        players.put(player, owners);

        Set<String> custom_items = new HashSet<>(ClaimGuis.getCustomItems("claims"));
        for (String key : custom_items) {
            List<String> lore = new ArrayList<>(getLoreP(ClaimGuis.getCustomItemLore("claims", key), player));
            String title = ClaimGuis.getCustomItemTitle("claims", key);
            if (ClaimSettings.getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            inv.setItem(ClaimGuis.getCustomItemSlot("claims", key), createCustomItem(key, title, lore));
        }
    }

    // Method to get the owners by filter
    private Set<String> getPownersByFilter(String filter) {
        switch (filter) {
            case "sales":
                return ClaimMain.getClaimsOwnersWithSales();
            case "online":
                return ClaimMain.getClaimsOnlineOwners();
            case "offline":
                return ClaimMain.getClaimsOfflineOwners();
            default:
                return ClaimMain.getClaimsOwners();
        }
    }

    // Method to create the item
    private ItemStack createOwnerClaimItem(String owner, List<String> lore) {
        if (ClaimGuis.getItemCheckCustomModelData("claims", "claim-item")) {
            return createItemWMD(ClaimLanguage.getMessageWP("owner-claim-title", owner).replaceAll("%owner%", owner),
                    lore,
                    ClaimGuis.getItemMaterialMD("claims", "claim-item"),
                    ClaimGuis.getItemCustomModelData("claims", "claim-item"));
        }
        if (ClaimGuis.getItemMaterialMD("claims", "claim-item").contains("PLAYER_HEAD")) {
            ItemStack item = CacheGui.getPlayerHead(owner);
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setDisplayName(ClaimLanguage.getMessageWP("owner-claim-title", owner).replaceAll("%owner%", owner));
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
        ItemStack item = new ItemStack(ClaimGuis.getItemMaterial("claims", "claim-item"), 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ClaimLanguage.getMessageWP("owner-claim-title", owner).replaceAll("%owner%", owner));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    // Method to create custom item
    private ItemStack createCustomItem(String key, String title, List<String> lore) {
        if (ClaimGuis.getCustomItemCheckCustomModelData("claims", key)) {
            return createItemWMD(title,
                    lore,
                    ClaimGuis.getCustomItemMaterialMD("claims", key),
                    ClaimGuis.getCustomItemCustomModelData("claims", key));
        } else {
            return createItem(ClaimGuis.getCustomItemMaterial("claims", key),
                    title,
                    lore);
        }
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
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    // Filter slot
    private ItemStack filter(String filter) {
    	ItemStack item = null;
    	if(ClaimGuis.getItemCheckCustomModelData("claims", "filter")) {
    		CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims", "filter"));
            if(customStack == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError custom item loading : "+ClaimGuis.getItemMaterialMD("claims", "filter"));
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
            	item = new ItemStack(Material.STONE,1);
            } else {
            	item = customStack.getItemStack();
            }
    	} else {
    		Material material = ClaimGuis.getItemMaterial("claims", "filter");
    		if(material == null) {
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cError material loading, check claims.yml");
            	Bukkit.getServer().getConsoleSender().sendMessage("§4[SimpleClaimSystem] §cUsing STONE instead");
    			material = Material.STONE;
    		}
    		item = new ItemStack(material, 1);
    	}
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            String lore_filter = ClaimLanguage.getMessage("filter-new-lore");
            if(filter.equals("sales")) {
            	lore_filter = lore_filter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"))
            			.replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            } else if (filter.equals("online")) {
            	lore_filter = lore_filter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_active_filter"))
            			.replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            } else if (filter.equals("offline")) {
            	lore_filter = lore_filter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_active_filter"));
            } else {
            	lore_filter = lore_filter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
            			.replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_3%", ClaimLanguage.getMessage("status_color_inactive_filter"))
            			.replaceAll("%status_color_4%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(lore_filter));
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
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
