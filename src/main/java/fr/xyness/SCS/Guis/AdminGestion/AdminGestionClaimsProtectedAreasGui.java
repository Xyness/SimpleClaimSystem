package fr.xyness.SCS.Guis.AdminGestion;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.*;
import fr.xyness.SCS.Config.*;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * The Admin Claims Protected Areas Management GUI.
 *
 * L'interface utilisateur graphique de gestion des zones protégées des revendications administratives.
 */
public class AdminGestionClaimsProtectedAreasGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** The inventory for this GUI. */
    private final Inventory inv;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * The Admin Claims Protected Areas Management GUI constructor.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionClaimsProtectedAreasGui(Player player, int page, String filter, SimpleClaimSystem instance) {
    	this.instance = instance;
        // TODO: translate this string
        // zone: null since this is a list of claims (See Chunk/Zone list for zones)
        inv = Bukkit.createInventory(this, 54, "§4[A]§r Protected areas (Page "+String.valueOf(page)+")");
        loadItems(player, page, filter).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
        	}
        })
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }
    
    
    // ********************
    // *  Others Methods  *
    // ********************
    

    /**
     * Load items into the inventory.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    private CompletableFuture<Boolean> loadItems(Player player, int page, String filter) {
    	return CompletableFuture.supplyAsync(() -> {
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        cPlayer.setFilter(filter);
	        cPlayer.clearMapClaim();
	        cPlayer.clearMapLoc();
	        cPlayer.setGuiPage(page);
            cPlayer.clearGuiZone();
            // zone: null since we are in the list of claims (admin-protected areas in this case)
	        inv.setItem(48, backPage(page - 1,!(page > 1)));
	        
	        Set<Claim> claims = getClaims(filter, "*");
	        List<Claim> claimList = new ArrayList<>(claims);
	        Collections.sort(claimList, (claim1, claim2) -> claim1.getName().compareTo(claim2.getName()));
	        claims = new LinkedHashSet<>(claimList);
	        setFilterItem(filter);
	
	        List<String> loreTemplate = instance.getGuis().getLore("§7Chunks: §e%chunks_count%\n§7Spawn location: §b%location%\n§7%sale-status%\n \n§7Members:\n%members%\n \n§7Banned players:\n%bans%\n \n");
	        fillClaimsItems(player, cPlayer, page, loreTemplate, claims);
	        
	        return true;
	        
    	});
    	
    }

    /**
     * Get the claims based on the filter and owner.
     * 
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     * @return A map of claims and their corresponding chunks.
     */
    private Set<Claim> getClaims(String filter, String owner) {
        return "sales".equals(filter) ? instance.getMain().getClaimsInSale(owner) : instance.getMain().getPlayerClaims(owner);
    }

    /**
     * Create a back page item.
     * 
     * @param page The page number.
     * @param back If the backPage is to a other menu
     * @return The created ItemStack.
     */
    private ItemStack backPage(int page, boolean back) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cPrevious page");
            meta.setLore(Arrays.asList(back ? "§7Go back to admin main menu" : "§7Go to the page "+String.valueOf(page),"§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create a next page item.
     * 
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNext page");
            meta.setLore(Arrays.asList("§7Go to the page "+String.valueOf(page),"§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Set the filter item.
     * 
     * @param filter The filter applied to the claims.
     */
    private void setFilterItem(String filter) {
        inv.setItem(49, createFilterItem(filter));
    }

    /**
     * Fill the claims items in the GUI.
     * 
     * @param player       The player who opened the GUI.
     * @param cPlayer      The custom player object.
     * @param page         The current page of the GUI.
     * @param loreTemplate The lore template for the items.
     * @param claims       A map of claims and their corresponding chunks.
     */
    private void fillClaimsItems(Player player, CPlayer cPlayer, int page, List<String> loreTemplate, Set<Claim> claims) {
        int minSlot = 0;
        int maxSlot = 44;
        int itemsPerPage = maxSlot - minSlot + 1;
        int startIndex = (page - 1) * itemsPerPage;
        int slotIndex = minSlot;
        int itemCount = 0;

        for (Claim claim : claims) {
            if (itemCount++ < startIndex) continue;
            if (slotIndex > maxSlot) {
                inv.setItem(50, nextPage(page + 1));
                break;
            }
            List<String> lore = prepareLore(loreTemplate, claim, player);
            ItemStack item = createClaimItem(claim, player, lore);
            cPlayer.addMapClaim(slotIndex, claim);
            cPlayer.addMapLoc(slotIndex, claim.getLocation());
            inv.setItem(slotIndex++, item);
        }
    }

    /**
     * Prepare the lore of the items.
     * 
     * @param template The lore template.
     * @param claim    The claim object.
     * @param player   The player who opened the GUI.
     * @return A list of lore lines.
     */
    private List<String> prepareLore(List<String> template, Claim claim, Player player) {
        List<String> lore = new ArrayList<>();
        for (String line : template) {
            line = line.replace("%name%", claim.getName())
                .replace("%chunks_count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
                .replace("%location%", instance.getMain().getClaimCoords(claim))
	    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
					.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
					.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"));
            if (line.contains("%members%")) {
                lore.addAll(Arrays.asList(getMembers(claim).split("\n")));
            } else if (line.contains("%bans%")) {
                lore.addAll(Arrays.asList(getBans(claim).split("\n")));
            } else {
                lore.add(line);
            }
        }
        lore.add("§c[Left-click]§7 to manage");
        lore.add("§c[Shift-left-click]§7 to quick delete");
        return lore;
    }
    
    /**
     * Create a claim item for the GUI.
     * 
     * @param claim      The claim object.
     * @param player     The player who opened the GUI.
     * @param lore       The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createClaimItem(Claim claim, Player player, List<String> lore) {
        String displayName = "§6"+claim.getName()+" §7("+claim.getDescription()+")";
        return createPlayerHeadItem(claim, displayName, lore);
    }

    /**
     * Create a claim item with a PLAYER_HEAD material.
     * 
     * @param claim        The claim object.
     * @param displayName  The display name for the item.
     * @param lore         The lore for the item.
     * @return The created ItemStack.
     */
    private ItemStack createPlayerHeadItem(Claim claim, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(Material.FARMLAND);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(displayName);
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Create the filter item.
     * 
     * @param filter The filter applied to the claims.
     * @return The created ItemStack.
     */
    private ItemStack createFilterItem(String filter) {
        String loreFilter = getFilterLore(filter);
        return createItem(Material.END_CRYSTAL,
                "§eFilter",
                instance.getGuis().getLore(loreFilter));
    }

    /**
     * Get the lore of the filter item.
     * 
     * @param filter The filter applied to the claims.
     * @return The lore of the filter item.
     */
    private String getFilterLore(String filter) {
        String loreFilter = "§7Change filter\n%status_color_1%➲ All claims\n%status_color_2%➲ Claims in sale\n§7▸ §fClick to change";
        if ("sales".equals(filter)) {
            loreFilter = loreFilter.replace("%status_color_1%", "§8")
                .replace("%status_color_2%", "§a");
        } else {
            loreFilter = loreFilter.replace("%status_color_1%", "§a")
                .replace("%status_color_2%", "§8");
        }
        return loreFilter;
    }

    /**
     * Create an item.
     * 
     * @param material The material of the item.
     * @param name     The display name of the item.
     * @param lore     The lore of the item.
     * @return The created ItemStack.
     */
    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material != null ? material : Material.STONE, 1);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (name != null) meta.setDisplayName(name);
            if (lore != null) meta.setLore(lore);
            meta = instance.getGuis().setItemFlag(meta);
            AttributeModifier modifier = new AttributeModifier(UUID.randomUUID(), "generic.armor", 0, AttributeModifier.Operation.ADD_NUMBER);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR, modifier);
            item.setItemMeta(meta);
        }
        return item;
    }

    /**
     * Get the members of a claim as a string with new lines.
     * 
     * @param claim The claim object.
     * @return A string representing the members of the claim.
     */
    public String getMembers(Claim claim) {
        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getMembers());
        if (members.isEmpty()) {
            return "§8no members";
        }
        StringBuilder membersList = new StringBuilder();
        int i = 0;
        for (String member : members) {
        	if(member == null) continue;
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
    
    /**
     * Get the bans of a claim as a string with new lines.
     * 
     * @param claim The claim object.
     * @return A string representing the bans of the claim.
     */
    public String getBans(Claim claim) {
        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getBans());
        if (members.isEmpty()) {
            return "§8no banned players";
        }
        StringBuilder membersList = new StringBuilder();
        int i = 0;
        for (String member : members) {
        	if(member == null) continue;
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

    @Override
    public Inventory getInventory() {
        return inv;
    }
}
