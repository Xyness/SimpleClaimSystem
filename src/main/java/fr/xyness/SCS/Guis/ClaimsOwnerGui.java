package fr.xyness.SCS.Guis;

import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.*;
import fr.xyness.SCS.Config.*;
import fr.xyness.SCS.Others.CacheGui;
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimsOwnerGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private Inventory inv;
    private static final Map<Player, Map<Integer, Location>> claimsLoc = new HashMap<>();
    private static final Map<Player, Map<Integer, Chunk>> claimsChunk = new HashMap<>();
    private static final Map<Player, String> playerFilter = new HashMap<>();
    private static final Map<Player, String> owner = new HashMap<>();
    
    
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
    public ClaimsOwnerGui(Player player, int page, String filter, String owner) {
    	String title = ClaimGuis.getGuiTitle("claims_owner")
    		.replaceAll("%page%", String.valueOf(page))
    		.replaceAll("%owner%", owner);
    	if (ClaimSettings.getBooleanSetting("placeholderapi")) {
    		title = PlaceholderAPI.setPlaceholders(player, title);
    	}
        inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims_owner") * 9, title);
        initializeItems(player, page, filter, owner);
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
    // Method to get the targeted owner for a player
    public static String getOwner(Player player) {
    	return owner.get(player);
    }
    
    // Method to remove the targeted owner for a player
    public static void removeOwner(Player player) {
    	owner.remove(player);
    }
    
    // Method to get the filter of a player
    public static String getPlayerFilter(Player player) {
    	return playerFilter.get(player);
    }
    
    // Method to remove the filter of a player
    public static void removePlayerFilter(Player player) {
    	playerFilter.remove(player);
    }
    
    // Method to get the claim's location (by slot) for a player
    public static Location getClaimLoc(Player player, int slot) {
    	return claimsLoc.get(player).get(slot);
    }
    
    // Method to remove the claim's location for a player
    public static void removeClaimsLoc(Player player) {
    	claimsLoc.remove(player);
    }
    
    // Method to get the claim chunk (by slot) for a player
    public static Chunk getClaimChunk(Player player, int slot) {
    	return claimsChunk.get(player).get(slot);
    }
    
    // Method to remove the claim chunk for a player
    public static void removeClaimsChunk(Player player) {
    	claimsChunk.remove(player);
    }

    // Method to initialize the items in the gui
    private void initializeItems(Player player, int page, String filter, String owner) {
        if (SimpleClaimSystem.isFolia()) {
            Bukkit.getAsyncScheduler().runNow(ClaimMain.getPlugin(), task -> {
            	loadItems(player,page,filter,owner);
            });
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(ClaimMain.getPlugin(), task -> {
            	loadItems(player,page,filter,owner);
            });
        }
    }
    
    // Method to load items
    private void loadItems(Player player, int page, String filter, String owner) {
    	this.owner.put(player, owner);
        playerFilter.put(player, filter);

        int minSlot = ClaimGuis.getGuiMinSlot("claims_owner");
        int maxSlot = ClaimGuis.getGuiMaxSlot("claims_owner");
        int itemsPerPage = maxSlot - minSlot + 1;

        setNavigationItems(page);
        Set<Chunk> claims = filter.equals("sales") ? 
            ClaimMain.getChunksinSaleFromOwner(owner) : 
            ClaimMain.getChunksFromOwner(owner);

        inv.setItem(ClaimGuis.getItemSlot("claims_owner", "filter"), createFilterItem(filter));
        
        List<String> loreTemplate = getLore(ClaimLanguage.getMessage("access-all-claim-lore"));
        Map<Integer, Location> claimLocations = new HashMap<>();
        Map<Integer, Chunk> claimChunks = new HashMap<>();

        int startIndex = (page - 1) * itemsPerPage;
        int slotIndex = minSlot;
        int itemCount = 0;

        for (Chunk chunk : claims) {
            if (itemCount++ < startIndex) continue;
            if (slotIndex > maxSlot) {
                inv.setItem(ClaimGuis.getItemSlot("claims_owner", "next-page-list"), createNavigationItem("next-page-list", page + 1));
                break;
            }
            if (!ClaimMain.canPermCheck(chunk, "Visitors") && !ClaimSettings.getBooleanSetting("claims-visitors-off-visible")) continue;

            List<String> lore = prepareLore(loreTemplate, chunk, player);
            ItemStack item = createClaimItem(chunk, player, lore);

            claimChunks.put(slotIndex, chunk);
            claimLocations.put(slotIndex, ClaimMain.getClaimLocationByChunk(chunk));
            inv.setItem(slotIndex++, item);
        }

        claimsChunk.put(player, claimChunks);
        claimsLoc.put(player, claimLocations);

        setCustomItems(player);
    }

    // Method to set the navigation items (back page and next page)
    private void setNavigationItems(int page) {
        if (page > 1) {
            inv.setItem(ClaimGuis.getItemSlot("claims_owner", "back-page-list"), createNavigationItem("back-page-list", page - 1));
        } else {
            inv.setItem(ClaimGuis.getItemSlot("claims_owner", "back-page-claims"), createNavigationItem("back-page-claims", 0));
        }
    }

    // Method to set custom items
    private void setCustomItems(Player player) {
        Set<String> customItems = new HashSet<>(ClaimGuis.getCustomItems("claims_owner"));
        for (String key : customItems) {
            List<String> lore = getLoreP(ClaimGuis.getCustomItemLore("claims_owner", key), player);
            String title = ClaimGuis.getCustomItemTitle("claims_owner", key);
            if (ClaimSettings.getBooleanSetting("placeholderapi")) {
                title = PlaceholderAPI.setPlaceholders(player, title);
            }
            if (ClaimGuis.getCustomItemCheckCustomModelData("claims_owner", key)) {
                inv.setItem(ClaimGuis.getCustomItemSlot("claims_owner", key), createItemWMD(title, lore, ClaimGuis.getCustomItemMaterialMD("claims_owner", key), ClaimGuis.getCustomItemCustomModelData("claims_owner", key)));
            } else {
                inv.setItem(ClaimGuis.getCustomItemSlot("claims_owner", key), createItem(ClaimGuis.getCustomItemMaterial("claims", key), title, lore));
            }
        }
    }

    // Method to create the lore of items
    private List<String> prepareLore(List<String> template, Chunk chunk, Player player) {
        List<String> lore = new ArrayList<>();
        for (String line : template) {
            line = line.replaceAll("%description%", ClaimMain.getClaimDescription(chunk))
                .replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))
                .replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(chunk)));
            if (line.contains("%members%")) {
                lore.addAll(Arrays.asList(getMembers(chunk).split("\n")));
            } else {
                lore.add(line);
            }
        }

        if (ClaimSettings.getBooleanSetting("economy") && ClaimMain.claimIsInSale(chunk)) {
            String[] saleInfo = ClaimLanguage.getMessageWP("all-claim-buyable-price", ClaimMain.getOwnerInClaim(chunk))
                .replaceAll("%price%", String.valueOf(ClaimMain.getClaimPrice(chunk)))
                .split("\n");
            Collections.addAll(lore, saleInfo);
            lore.add(ClaimLanguage.getMessage("all-claim-is-buyable"));
        }

        lore.add(ClaimMain.canPermCheck(chunk, "Visitors") || ClaimMain.getOwnerInClaim(chunk).equals(player.getName()) ? 
            ClaimLanguage.getMessage("access-all-claim-lore-allow-visitors") : 
            ClaimLanguage.getMessage("access-all-claim-lore-deny-visitors"));

        return getLoreWP(lore, ClaimMain.getOwnerInClaim(chunk));
    }

    // Method to create the claim item
    private ItemStack createClaimItem(Chunk chunk, Player player, List<String> lore) {
        String displayName = ClaimLanguage.getMessageWP("access-all-claim-title", ClaimMain.getOwnerInClaim(chunk))
            .replaceAll("%owner%", ClaimMain.getOwnerInClaim(chunk))
            .replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk))
            .replaceAll("%coords%", String.valueOf(ClaimMain.getClaimCoords(chunk)));

        if (ClaimGuis.getItemCheckCustomModelData("claims_owner", "claim-item")) {
            return createItemWMD(displayName, lore, ClaimGuis.getItemMaterialMD("claims_owner", "claim-item"), ClaimGuis.getItemCustomModelData("claims_owner", "claim-item"));
        }
        if (ClaimGuis.getItemMaterialMD("claims_owner", "claim-item").contains("PLAYER_HEAD")) {
            ItemStack item = CacheGui.getPlayerHead(ClaimMain.getOwnerInClaim(chunk));
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            meta.setDisplayName(displayName);
            meta.setLore(lore);
            item.setItemMeta(meta);
            return item;
        }
        return createItem(ClaimGuis.getItemMaterial("claims_owner", "claim-item"), displayName, lore);
    }

    // Method to create the navigation item (back page and next page)
    private ItemStack createNavigationItem(String key, int page) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("claims_owner", key)) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims_owner", key));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("claims_owner", key);
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
        	if(page == 0) {
        		meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title"));
        		meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-claims-lore")));
        	} else {
                meta.setDisplayName(ClaimLanguage.getMessage(key.equals("next-page-list") ? "next-page-title" : "previous-page-title").replaceAll("%page%", String.valueOf(page)));
                meta.setLore(getLore(ClaimLanguage.getMessage(key.equals("next-page-list") ? "next-page-lore" : "previous-page-lore").replaceAll("%page%", String.valueOf(page))));
        	}
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Method to create the filter item
    private ItemStack createFilterItem(String filter) {
        ItemStack item;
        if (ClaimGuis.getItemCheckCustomModelData("claims_owner", "filter")) {
            CustomStack customStack = CustomStack.getInstance(ClaimGuis.getItemMaterialMD("claims_owner", "filter"));
            item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        } else {
            Material material = ClaimGuis.getItemMaterial("claims_owner", "filter");
            item = new ItemStack(material != null ? material : Material.STONE, 1);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = ClaimLanguage.getMessage("filter-owner-lore");
            if (filter.equals("sales")) {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
                    .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"));
            } else {
                loreFilter = loreFilter.replaceAll("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
                    .replaceAll("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
            meta.setLore(getLore(loreFilter));
            meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS, ItemFlag.HIDE_ATTRIBUTES);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Method to create an item
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material != null ? material : Material.STONE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Method to create a custom item
    private ItemStack createItemWMD(String name, List<String> lore, String customItemName, int modelData) {
        CustomStack customStack = CustomStack.getInstance(customItemName);
        ItemStack item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(name);
            meta.setLore(lore);
            meta.setCustomModelData(modelData);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ITEM_SPECIFICS);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    // Method to get members of a claim by chunk
    public static String getMembers(Chunk chunk) {
        Set<String> members = ClaimMain.getClaimMembers(chunk);
        if (members.isEmpty()) {
            return ClaimLanguage.getMessage("claim-list-no-member");
        }
        StringBuilder membersList = new StringBuilder();
        int i = 0;
        for (String member : members) {
            Player player = Bukkit.getPlayer(member);
            String memberName = player != null ? "§a" + member : "§c" + member;
            membersList.append(memberName);
            if (i < members.size() - 1) {
                membersList.append("§7, ");
            }
            if ((i + 1) % 4 == 0 && i < members.size() - 1) {
                membersList.append("\n");
            }
            i++;
        }
        return membersList.toString();
    }
    
    // Method to split the lore for lines
    public static List<String> getLore(String lore) {
        return Arrays.asList(lore.split("\n"));
    }
    
    // Method to split the lore for lines with placeholders from PlaceholderAPI
    public static List<String> getLoreP(String lore, Player player) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) {
            return getLore(lore);
        }
        List<String> lores = new ArrayList<>();
        for (String line : lore.split("\n")) {
            lores.add(PlaceholderAPI.setPlaceholders(player, line));
        }
        return lores;
    }
    
    // Method to apply placeholders to lines of a lore
    public static List<String> getLoreWP(List<String> lore, String playerName) {
        if (!ClaimSettings.getBooleanSetting("placeholderapi")) {
            return lore;
        }
        List<String> lores = new ArrayList<>();
        Player player = Bukkit.getPlayer(playerName);
        OfflinePlayer offlinePlayer = player != null ? player : Bukkit.getOfflinePlayer(playerName);
        for (String line : lore) {
            lores.add(PlaceholderAPI.setPlaceholders(offlinePlayer, line));
        }
        return lores;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    public void openInventory(Player player) {
        player.openInventory(inv);
    }
}
