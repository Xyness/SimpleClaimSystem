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
import me.clip.placeholderapi.PlaceholderAPI;

public class ClaimsOwnerGui implements InventoryHolder {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private final Inventory inv;
	
	
	// ******************
	// *  Constructors  *
	// ******************
    
    
    // Main constructor
	public ClaimsOwnerGui(Player player, int page, String filter, String owner) {
		String title = getTitle(player, page, owner);
		inv = Bukkit.createInventory(this, ClaimGuis.getGuiRows("claims_owner") * 9, title);
		initializeItems(player, page, filter, owner);
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to get the title of the gui (replace placeholders)
	private String getTitle(Player player, int page, String owner) {
		String title = ClaimGuis.getGuiTitle("claims_owner")
			.replace("%page%", String.valueOf(page))
			.replace("%owner%", owner);
		if (ClaimSettings.getBooleanSetting("placeholderapi")) {
			title = PlaceholderAPI.setPlaceholders(player, title);
		}
		return title;
	}

	// Method to initialize items in async mode
	private void initializeItems(Player player, int page, String filter, String owner) {
		Runnable loadItemsTask = () -> loadItems(player, page, filter, owner);
		if (SimpleClaimSystem.isFolia()) {
			Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> loadItemsTask.run());
		} else {
			Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), loadItemsTask);
		}
	}

	// Method to load items
	private void loadItems(Player player, int page, String filter, String owner) {
		CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
		cPlayer.setOwner(owner);
		cPlayer.setFilter(filter);
		cPlayer.clearMapChunk();
		cPlayer.clearMapLoc();

		setNavigationItems(page);
		Map<Chunk, Claim> claims = getClaims(filter, owner);
		setFilterItem(filter);

		List<String> loreTemplate = getLore(ClaimLanguage.getMessage("access-all-claim-lore"));
		fillClaimsItems(player, cPlayer, page, loreTemplate, claims);
		setCustomItems(player);
	}

	// Method to get claims
	private Map<Chunk, Claim> getClaims(String filter, String owner) {
		return filter.equals("sales") ? ClaimMain.getChunksInSaleFromOwner(owner) : ClaimMain.getChunksFromOwnerGui(owner);
	}

	// Method to set the navigation items (back page)
	private void setNavigationItems(int page) {
		if (page > 1) {
			setItemAsync(ClaimGuis.getItemSlot("claims_owner", "back-page-list"), createNavigationItem("back-page-list", page - 1));
		} else {
			setItemAsync(ClaimGuis.getItemSlot("claims_owner", "back-page-claims"), createNavigationItem("back-page-claims", 0));
		}
	}

	// Method to set the filter item
	private void setFilterItem(String filter) {
		setItemAsync(ClaimGuis.getItemSlot("claims_owner", "filter"), createFilterItem(filter));
	}

	// Method to set the claims items
	private void fillClaimsItems(Player player, CPlayer cPlayer, int page, List<String> loreTemplate, Map<Chunk, Claim> claims) {
		int minSlot = ClaimGuis.getGuiMinSlot("claims_owner");
		int maxSlot = ClaimGuis.getGuiMaxSlot("claims_owner");
		int itemsPerPage = maxSlot - minSlot + 1;
		int startIndex = (page - 1) * itemsPerPage;
		int slotIndex = minSlot;
		int itemCount = 0;

		for (Map.Entry<Chunk, Claim> entry : claims.entrySet()) {
			if (itemCount++ < startIndex) continue;
			if (slotIndex > maxSlot) {
				setItemAsync(ClaimGuis.getItemSlot("claims_owner", "next-page-list"), createNavigationItem("next-page-list", page + 1));
				break;
			}
			Chunk chunk = entry.getKey();
			Claim claim = entry.getValue();
			if (claim.getPermission("Visitor") && !ClaimSettings.getBooleanSetting("claims-visitors-off-visible")) continue;

			List<String> lore = prepareLore(loreTemplate, claim, player);
			ItemStack item = createClaimItem(claim, player, lore);
			cPlayer.addMapChunk(slotIndex, chunk);
			cPlayer.addMapLoc(slotIndex, ClaimMain.getClaimLocationByChunk(chunk));
			setItemAsync(slotIndex++, item);
		}
	}

	// Method to set the custom items
	private void setCustomItems(Player player) {
		Set<String> customItems = new HashSet<>(ClaimGuis.getCustomItems("claims_owner"));
		for (String key : customItems) {
			List<String> lore = getLoreP(ClaimGuis.getCustomItemLore("claims_owner", key), player);
			String title = ClaimGuis.getCustomItemTitle("claims_owner", key);
			if (ClaimSettings.getBooleanSetting("placeholderapi")) {
				title = PlaceholderAPI.setPlaceholders(player, title);
			}
			setItemAsync(ClaimGuis.getCustomItemSlot("claims_owner", key), createCustomItem(key, title, lore));
		}
	}

	// Method to prepare the lore of items
	private List<String> prepareLore(List<String> template, Claim claim, Player player) {
		List<String> lore = new ArrayList<>();
		for (String line : template) {
			line = line.replace("%description%", claim.getDescription())
				.replace("%name%", claim.getName())
				.replace("%coords%", ClaimMain.getClaimCoords(claim));
			if (line.contains("%members%")) {
				lore.addAll(Arrays.asList(getMembers(claim).split("\n")));
			} else {
				lore.add(line);
			}
		}
		addEconomyLore(claim, lore);
		addVisitorLore(claim, lore, player);
		return getLoreWP(lore, claim.getOwner());
	}

	// Method to add the economy lore (if enabled)
	private void addEconomyLore(Claim claim, List<String> lore) {
		if (ClaimSettings.getBooleanSetting("economy") && claim.getSale()) {
			Collections.addAll(lore, ClaimLanguage.getMessageWP("all-claim-buyable-price", claim.getOwner())
				.replace("%price%", String.valueOf(claim.getPrice()))
				.split("\n"));
			lore.add(ClaimLanguage.getMessage("all-claim-is-buyable"));
		}
	}

	// Method to add the visitor lore (if enabled)
	private void addVisitorLore(Claim claim, List<String> lore, Player player) {
		String visitorMessage = ClaimMain.canPermCheck(claim.getChunk(), "Visitors") || claim.getOwner().equals(player.getName()) ? 
			ClaimLanguage.getMessage("access-all-claim-lore-allow-visitors") : 
			ClaimLanguage.getMessage("access-all-claim-lore-deny-visitors");
		lore.add(visitorMessage);
	}
	
	// Method to create claim item
	private ItemStack createClaimItem(Claim claim, Player player, List<String> lore) {
		String displayName = ClaimLanguage.getMessageWP("access-all-claim-title", claim.getOwner())
			.replace("%owner%", claim.getOwner())
			.replace("%name%", claim.getName())
			.replace("%coords%", ClaimMain.getClaimCoords(claim));

		if (ClaimGuis.getItemCheckCustomModelData("claims_owner", "claim-item")) {
			return createItemWMD(displayName, lore, ClaimGuis.getItemMaterialMD("claims_owner", "claim-item"), ClaimGuis.getItemCustomModelData("claims_owner", "claim-item"));
		}
		if (ClaimGuis.getItemMaterialMD("claims_owner", "claim-item").contains("PLAYER_HEAD")) {
			return createPlayerHeadItem(claim, displayName, lore);
		}
		return createItem(ClaimGuis.getItemMaterial("claims_owner", "claim-item"), displayName, lore);
	}

	// Method to create claim item (if PLAYER_HEAD)
	private ItemStack createPlayerHeadItem(Claim claim, String displayName, List<String> lore) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD);
		SkullMeta meta = (SkullMeta) item.getItemMeta();
		meta.setOwningPlayer(Bukkit.getOfflinePlayerIfCached(claim.getOwner()));
		meta.setDisplayName(displayName);
		meta.setLore(lore);
		item.setItemMeta(meta);
		return item;
	}

	// Method to create custom item
	private ItemStack createCustomItem(String key, String title, List<String> lore) {
		if (ClaimGuis.getCustomItemCheckCustomModelData("claims_owner", key)) {
			return createItemWMD(title, lore, ClaimGuis.getCustomItemMaterialMD("claims_owner", key), ClaimGuis.getCustomItemCustomModelData("claims_owner", key));
		} else {
			return createItem(ClaimGuis.getCustomItemMaterial("claims_owner", key), title, lore);
		}
	}

	// Method to create navigation item
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
			if (page == 0) {
				meta.setDisplayName(ClaimLanguage.getMessage("previous-page-title"));
				meta.setLore(getLore(ClaimLanguage.getMessage("previous-page-claims-lore")));
			} else {
				meta.setDisplayName(ClaimLanguage.getMessage(key.equals("next-page-list") ? "next-page-title" : "previous-page-title").replace("%page%", String.valueOf(page)));
				meta.setLore(getLore(ClaimLanguage.getMessage(key.equals("next-page-list") ? "next-page-lore" : "previous-page-lore").replace("%page%", String.valueOf(page))));
			}
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
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
			String loreFilter = getFilterLore(filter);
			meta.setDisplayName(ClaimLanguage.getMessage("filter-title"));
			meta.setLore(getLore(loreFilter));
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
			item.setItemMeta(meta);
		}
		return item;
	}

	// Method to get the lore of the filter
	private String getFilterLore(String filter) {
		String loreFilter = ClaimLanguage.getMessage("filter-owner-lore");
		if (filter.equals("sales")) {
			loreFilter = loreFilter.replace("%status_color_1%", ClaimLanguage.getMessage("status_color_inactive_filter"))
				.replace("%status_color_2%", ClaimLanguage.getMessage("status_color_active_filter"));
		} else {
			loreFilter = loreFilter.replace("%status_color_1%", ClaimLanguage.getMessage("status_color_active_filter"))
				.replace("%status_color_2%", ClaimLanguage.getMessage("status_color_inactive_filter"));
		}
		return loreFilter;
	}

	// Method to create item in the gui
	private ItemStack createItem(Material material, String name, List<String> lore) {
		ItemStack item = new ItemStack(material != null ? material : Material.STONE, 1);
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
	private ItemStack createItemWMD(String name, List<String> lore, String customItemName, int modelData) {
		CustomStack customStack = CustomStack.getInstance(customItemName);
		ItemStack item = customStack != null ? customStack.getItemStack() : new ItemStack(Material.STONE, 1);
		ItemMeta meta = item.getItemMeta();
		if (meta != null) {
			meta.setDisplayName(name);
			meta.setLore(lore);
			meta.setCustomModelData(modelData);
			meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
			item.setItemMeta(meta);
		}
		return item;
	}

	// Method to get members in lines
	public static String getMembers(Claim claim) {
		Set<String> members = claim.getMembers();
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

	// Method to split lines of lore in list
	public static List<String> getLore(String lore) {
		return Arrays.asList(lore.split("\n"));
	}

	// Method to split lines of lore in list (with placeholders)
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

	// Method to get lore with placeholders
	public static List<String> getLoreWP(List<String> lore, String playerName) {
		if (!ClaimSettings.getBooleanSetting("placeholderapi")) {
			return lore;
		}
		List<String> lores = new ArrayList<>();
		Player player = Bukkit.getPlayer(playerName);
		OfflinePlayer offlinePlayer = player != null ? player : Bukkit.getOfflinePlayerIfCached(playerName);
		for (String line : lore) {
			lores.add(PlaceholderAPI.setPlaceholders(offlinePlayer, line));
		}
		return lores;
	}

	// Method to set the item in the gui in sync mode
	private void setItemAsync(int slot, ItemStack item) {
		if (SimpleClaimSystem.isFolia()) {
			Bukkit.getGlobalRegionScheduler().execute(SimpleClaimSystem.getInstance(), () -> inv.setItem(slot, item));
		} else {
			Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), () -> inv.setItem(slot, item));
		}
	}

	@Override
	public Inventory getInventory() {
		return inv;
	}

	public void openInventory(Player player) {
		player.openInventory(inv);
	}
}
