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

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Claim List GUI.
 */
public class ClaimListGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** Inventory for the GUI. */
    private final Inventory inv;
    
    /** Player */
    private final Player player;

    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;
    
    
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
    	this.player = player;
    	// zone: null since listing claims (For Zone see chunks/zones list GUI)
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.getGuiSettings("list", null);
    	String title = guiSettings.getTitle()
    			.replace("%page%", String.valueOf(page));
    	
    	// Create the inventory
    	inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(page, filter).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error", null)));
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
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the list.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(int page, String filter) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        String playerName = player.getName();
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());

	        // Update player data (gui)
	        cPlayer.setFilter(filter);
	        cPlayer.clearMapClaim();
	        cPlayer.clearMapLoc();
	        cPlayer.setGuiPage(page);
			cPlayer.clearGuiZone();
	        
	        // Get claims
	        Set<Claim> claims = new HashSet<>(filter.equals("owner") ? instance.getMain().getPlayerClaims(playerName) : instance.getMain().getClaimsWhereMemberNotOwner(player));
	        List<Claim> claimList = new ArrayList<>(claims);
	        Collections.sort(claimList, (claim1, claim2) -> claim1.getName().compareTo(claim2.getName()));
	        claims = new LinkedHashSet<>(claimList);
	        int claimsCount = claims.size();
	        
    		GuiSettings guiSettings = ClaimGuis.gui_settings.get("list");
	        int max = guiSettings.getEndSlot() - guiSettings.getStartSlot();
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.getGuiSlots(zone).get("list"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String key = slot.getKey();
    			String title = slot.getTitle();
    			String lore_string = slot.getLore();
    			if(key.equals("BackPage")) {
    				if(page == 1) continue;
    				title = title.replace("%page%", String.valueOf(page-1));
    				lore_string = lore_string.replace("%page%", String.valueOf(page-1));
    			}
    			if(key.equals("NextPage")) {
    				if(claimsCount <= (page*max)) continue;
    				title = title.replace("%page%", String.valueOf(page+1));
    				lore_string = lore_string.replace("%page%", String.valueOf(page+1));
    			}
    			if (key.equals("Filter")) {
    	            if (filter.equals("not_owner")) {
    	                lore_string = lore_string.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_inactive_filter"))
    	                        .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_active_filter"));
    	            } else {
    	                lore_string = lore_string.replace("%status_color_1%", instance.getLanguage().getMessage("status_color_active_filter"))
    	                        .replace("%status_color_2%", instance.getLanguage().getMessage("status_color_inactive_filter"));
    	            }
    			}
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(title.isBlank()) title = null;
    			if(lore.isEmpty()) lore = null;
    			if(slot.isCustomModel()) {
    				CustomStack customItem = CustomStack.getInstance(slot.getCustomModelData());
    				if(customItem != null) {
    					Material mat = customItem.getItemStack().getType();
    					inv.setItem(slot_int, instance.getGuis().createItem(mat, title, lore));
    				}
    			} else if (slot.isCustomHead()) {
    				inv.setItem(slot_int, instance.getPlayerMain().createPlayerHeadWithTexture(slot.getCustomTextures(), title, lore));
    			} else {
					Material mat = slot.getMaterial();
					inv.setItem(slot_int, instance.getGuis().createItem(mat, title, lore));
    			}
    		}
	
	        // Prepare lore
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
	        int startItem = (page - 1) * max;
	        int i = guiSettings.getStartSlot();
	        int count = 0;
	        
	        // Start loop
	        for (Claim claim : claims) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than end slot to not exceed
	            if (i == guiSettings.getEndSlot()+1) break;
	            
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
	        	if(item.hasItemMeta() && item.getItemMeta() != null) {
	        		if(item.getItemMeta() instanceof SkullMeta meta) {
	    	            meta.setDisplayName(instance.getLanguage().getMessage("access-claim-title").replace("%name%", claim.getName()).replace("%coords%", instance.getMain().getClaimCoords(claim)));
	    	            meta.setLore(used_lore);
	    	            item.setItemMeta(meta);
	        		} else {
	        			ItemMeta meta = item.getItemMeta();
	    	            meta.setDisplayName(instance.getLanguage().getMessage("access-claim-title").replace("%name%", claim.getName()).replace("%coords%", instance.getMain().getClaimCoords(claim)));
	    	            meta.setLore(used_lore);
	    	            item.setItemMeta(meta);
	        		}
	        	}
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

    @Override
    public Inventory getInventory() {
        return inv;
    }

}
