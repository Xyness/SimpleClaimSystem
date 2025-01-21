package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * Class representing the Claim List GUI.
 */
public class ClaimListGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** Inventory for the GUI. */
    private Inventory inv;

    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the ClaimListGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param page   The page number of the GUI.
     * @param filter The filter applied to the list.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimListGui(Player player, int page, String filter, SimpleClaimSystem instance) {
    	this.instance = instance;
    	
    	// Get title
    	String title = instance.getLanguage().getMessage("gui-list-title")
    			.replace("%page%", String.valueOf(page));
    	
    	// Create the inventory
        inv = Bukkit.createInventory(this, 54, title);
        
        // Load the items asynchronously
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
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the list.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Player player, int page, String filter) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        String playerName = player.getName();
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        int claimsCount = cPlayer.getClaimsCount();
	        
	        // Update player data (gui)
	        cPlayer.setFilter(filter);
	        cPlayer.clearMapClaim();
	        cPlayer.clearMapLoc();
	        
	        // Set bottom items
	        if (page > 1) inv.setItem(48, backPage(page - 1));
	        if (cPlayer.getClaim() != null) inv.setItem(49, backPageMain(cPlayer.getClaim()));
	        if (claimsCount > (page*45)) inv.setItem(50, nextPage(page + 1));
	        inv.setItem(53, filter(filter));
	
	        // Prepare lore
	        Set<Claim> claims = new HashSet<>(filter.equals("owner") ? instance.getMain().getPlayerClaims(playerName) : instance.getMain().getClaimsWhereMemberNotOwner(player));
	        List<Claim> claimList = new ArrayList<>(claims);
	        Collections.sort(claimList, (claim1, claim2) -> claim1.getName().compareTo(claim2.getName()));
	        claims = new LinkedHashSet<>(claimList);
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(filter.equals("owner") ? instance.getLanguage().getMessage("access-claim-lore") : instance.getLanguage().getMessage("access-claim-not-owner-lore")));
	        String lore_tp = instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")
	                ? instance.getLanguage().getMessage("access-claim-clickable-tp")
	                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-teleport");
	        String lore_remove = instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")
	                ? instance.getLanguage().getMessage("access-claim-clickable-remove")
	                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-remove");
	        String lore_settings = instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.main")
	                ? instance.getLanguage().getMessage("access-claim-clickable-manage")
	                : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-manage");
	
	        // Prepare count
	        int startItem = (page - 1) * 45;
	        int i = 0;
	        int count = 0;
	        
	        // Start loop
	        for (Claim claim : claims) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than 45 to not exceed
	            if (i == 45) break;
	            
	            // Add the claim and the claim location to map for gui clicking
	            cPlayer.addMapClaim(i, claim);
	            cPlayer.addMapLoc(i, claim.getLocation());
	            
	            // Prepare the lore per claim
	            List<String> used_lore = new ArrayList<>(processLore(lore,claim));
	            if (filter.equals("owner")) {
	                used_lore.addAll(Arrays.asList(lore_tp, lore_remove, lore_settings));
	            } else {
	                used_lore.add(lore_tp);
	            }
	            
	            // Set the claim item
	        	ItemStack item = instance.getPlayerMain().getPlayerHead(playerName);
	        	if(item == null) {
	        		item = new ItemStack(Material.PLAYER_HEAD);
	        	}
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName(instance.getLanguage().getMessage("access-claim-title").replace("%name%", claim.getName()).replace("%coords%", instance.getMain().getClaimCoords(claim)));
	            meta.setLore(used_lore);
	            item.setItemMeta(meta);
	            inv.setItem(i, item);
	            i++;
	
	        }
	        
	        return true;
        
    	});
	        
    }
    
    /**
     * Processes a list of lore strings by replacing placeholders with claim information.
     *
     * @param lore The list of lore strings containing placeholders.
     * @param claim The claim object containing the information to replace in the lore.
     * @return A new list of lore strings with placeholders replaced by claim information.
     */
    public List<String> processLore(List<String> lore, Claim claim) {
        List<String> used_lore = new ArrayList<>();
        String owner = claim.getOwner();
        String description = claim.getDescription();
        String name = claim.getName();
        String coords = instance.getMain().getClaimCoords(claim);
        String members = getMembers(claim);
        String bans = getBans(claim);

        for (String s : lore) {
            s = s.replace("%owner%", owner)
                 .replace("%description%", description)
                 .replace("%name%", name)
                 .replace("%coords%", coords);

            if (s.contains("%members%")) {
                if (members.contains("\n")) {
                    for (String member : members.split("\n")) {
                        used_lore.add(s.replace("%members%", member));
                    }
                } else {
                    used_lore.add(s.replace("%members%", members));
                }
            } else if (s.contains("%bans%")) {
                if (bans.contains("\n")) {
                    for (String ban : bans.split("\n")) {
                        used_lore.add(s.replace("%bans%", ban));
                    }
                } else {
                    used_lore.add(s.replace("%bans%", bans));
                }
            } else {
                used_lore.add(s);
            }
        }
        return used_lore;
    }

    /**
     * Gets members from a claim chunk.
     *
     * @param claim The claim chunk.
     * @return The members of the claim.
     */
    public String getMembers(Claim claim) {
        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getMembers());
        if (members.isEmpty()) {
            return instance.getLanguage().getMessage("claim-list-no-member");
        }
        StringBuilder membersList = new StringBuilder();
        int i = 0;
        for (String membre : members) {
        	if(membre == null) continue;
            Player p = Bukkit.getPlayer(membre);
            String fac = p != null ? "§a" + membre : "§c" + membre;
            membersList.append(fac);
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
        Set<String> bans = instance.getMain().convertUUIDSetToStringSet(claim.getBans());
        if (bans.isEmpty()) {
            return instance.getLanguage().getMessage("claim-list-no-ban");
        }
        StringBuilder bansList = new StringBuilder();
        int i = 0;
        for (String ban : bans) {
        	if(ban == null) continue;
            Player player = Bukkit.getPlayer(ban);
            String banName = player != null ? "§a" + ban : "§c" + ban;
            bansList.append(banName);
            if (i < bans.size() - 1) {
            	bansList.append("§7, ");
            }
            if ((i + 1) % 4 == 0 && i < bans.size() - 1) {
            	bansList.append("\n");
            }
            i++;
        }
        return bansList.toString();
    }

    /**
     * Creates an item for the back page slot.
     *
     * @param page The page number.
     * @return The created back page item.
     */
    private ItemStack backPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("previous-page-title").replace("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("previous-page-lore").replace("%page%", String.valueOf(page))));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the back page main item.
     * 
     * @param claim The target claim
     * @return The back page main item.
     */
    private ItemStack backPageMain(Claim claim) {
        ItemStack item = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("back-page-main-title"));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("back-page-main-lore").replace("%claim-name%", claim.getName())));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates an item for the next page slot.
     *
     * @param page The page number.
     * @return The created next page item.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(instance.getLanguage().getMessage("next-page-title").replace("%page%", String.valueOf(page)));
            meta.setLore(instance.getGuis().getLore(instance.getLanguage().getMessage("next-page-lore").replace("%page%", String.valueOf(page))));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }

    /**
     * Creates the filter item.
     *
     * @param filter The filter.
     * @return The filter item.
     */
    private ItemStack filter(String filter) {
        ItemStack item = new ItemStack(Material.END_CRYSTAL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String loreFilter = instance.getLanguage().getMessage("filter-list-lore");
            if (filter.equals("not_owner")) {
                loreFilter = loreFilter.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_inactive_filter"))
                        .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_active_filter"));
            } else {
                loreFilter = loreFilter.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_active_filter"))
                        .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_inactive_filter"));
            }
            meta.setDisplayName(instance.getLanguage().getMessage("filter-title"));
            meta.setLore(instance.getGuis().getLore(loreFilter));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    /**
     * Opens the inventory for the player.
     *
     * @param player The player.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
