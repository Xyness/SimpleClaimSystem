package fr.xyness.SCS.Guis;

import java.util.*;
import java.util.concurrent.CompletableFuture;

import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.SkullMeta;

import fr.xyness.SCS.*;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.GuiSettings;
import fr.xyness.SCS.Types.GuiSlot;

/**
 * Class representing the Claims Owner GUI.
 */
public class ClaimsOwnerGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** The inventory for this GUI. */
    private final Inventory inv;
    
    /** Player */
    private final Player player;
    
    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    

    /**
     * Main constructor for ClaimsOwnerGui.
     * 
     * @param player The player who opened the GUI.
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimsOwnerGui(Player player, int page, String filter, String owner, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	
    	// Get title
    	GuiSettings guiSettings = ClaimGuis.gui_settings.get("claims_owner");
    	String title = guiSettings.getTitle()
    			.replace("%page%", String.valueOf(page))
    			.replace("%owner%", owner);
    	
    	// Create the inventory
    	inv = Bukkit.createInventory(this, guiSettings.getRows()*9, title);
        
        // Load the items asynchronously
        loadItems(page, filter, owner).thenAccept(success -> {
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

    /**
     * Load items into the inventory.
     * 
     * @param page   The current page of the GUI.
     * @param filter The filter applied to the claims.
     * @param owner  The owner of the claims.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    private CompletableFuture<Boolean> loadItems(int page, String filter, String owner) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        Set<Claim> claims = getClaims(filter, owner);
	        List<Claim> claimList = new ArrayList<>(claims);
	        Collections.sort(claimList, (claim1, claim2) -> claim1.getName().compareTo(claim2.getName()));
	        claims = new LinkedHashSet<>(claimList);
	        int claimsCount = claims.size();
	        
	        // Update player data (gui)
	        cPlayer.setOwner(owner);
	        cPlayer.setFilter(filter);
	        cPlayer.clearMapClaim();
	        cPlayer.clearMapLoc();
	        cPlayer.setGuiPage(page);
	        
	        // Set bottom items
    		GuiSettings guiSettings = ClaimGuis.gui_settings.get("claims_owner");
    		int max = guiSettings.getSlots().size();
	        
	        // Items
    		List<GuiSlot> slots = new ArrayList<>(ClaimGuis.gui_slots.get("claims_owner"));
    		for(GuiSlot slot : slots) {
    			int slot_int = slot.getSlot();
    			String key = slot.getKey();
    			String title = slot.getTitle();
    			String lore_string = slot.getLore();
    			if(key.equals("BackPageOther")) {
    				if(page > 1) continue;
    			}
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
    	            lore_string = lore_string
    	                    .replaceAll("%status_color_" + getStatusIndex(filter) + "%", instance.getLanguage().getMessage("status_color_active_filter"))
    	                    .replaceAll("%status_color_[^" + getStatusIndex(filter) + "]%", instance.getLanguage().getMessage("status_color_inactive_filter"));
    			}
    			List<String> lore = instance.getGuis().getLore(lore_string);
    			if(title.isBlank()) title = null;
    			if(lore.isEmpty()) lore = null;
    			if(slot.isCustomModel()) {
    				Material mat = slot.getMaterial();
    				inv.setItem(slot_int, instance.getGuis().createCustomItem(mat, title, lore, slot.getCustomModelData()));
    			} else if (slot.isCustomHead()) {
    				if(slot.getCustomTextures().equalsIgnoreCase("%player%")) {
    					ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    					SkullMeta meta = (SkullMeta) head.getItemMeta();
    					meta.setOwningPlayer(player);
    					meta.setDisplayName(title);
    					meta.setLore(lore);
    					head.setItemMeta(meta);
    					inv.setItem(slot_int, head);
    				} else if (slot.getCustomTextures().length() < 17) {
    					ItemStack head = new ItemStack(Material.PLAYER_HEAD);
    					SkullMeta meta = (SkullMeta) head.getItemMeta();
    					OfflinePlayer targetP = Bukkit.getOfflinePlayer(slot.getCustomTextures());
    					if(targetP != null) {
    						meta.setOwningPlayer(targetP);
    					}
    					meta.setDisplayName(title);
    					meta.setLore(lore);
    					head.setItemMeta(meta);
    					inv.setItem(slot_int, head);
    				} else {
    					inv.setItem(slot_int, instance.getPlayerMain().createPlayerHeadWithTexture(slot.getCustomTextures(), title, lore));
    				}
    			} else {
					Material mat = slot.getMaterial();
					inv.setItem(slot_int, instance.getGuis().createItem(mat, title, lore));
    			}
    		}
	
	        // Prepare lore
	        List<String> loreTemplate = instance.getGuis().getLore(instance.getLanguage().getMessage("access-all-claim-lore"));
	        
	        // Prepare count
	        int startItem = (page - 1) * max;
	        List<Integer> slots_i = guiSettings.getSlots();
	        int i = slots_i.get(0);
	        int count = 0;
	        int count2 = 0;
	        
	        // Start loop
	        for (Claim claim : claims) {
	        	
	        	// Continue if not in the page
	            if (count++ < startItem) continue;
	            
	            // Break if bigger than end slot to not exceed
	            if (count2 > max-1) break;
	            
	            // Set new i
	            i = slots_i.get(count2);
	            count2++;
	            
	            // Hide claim if setting "GuiTeleport" disabled (if enabled in config.yml)
	            if (!claim.getPermissionForPlayer("GuiTeleport",player) && !instance.getSettings().getBooleanSetting("claims-visitors-off-visible")) continue;
	            
	            // Prepare lore and title for claim
	            List<String> lore = prepareLore(loreTemplate, claim, player);
	            String title = instance.getLanguage().getMessage("access-all-claim-title")
	                    .replace("%owner%", owner)
	                    .replace("%name%", claim.getName())
	                    .replace("%coords%", instance.getMain().getClaimCoords(claim));
	            
	            // Add the claim and claim location to map for gui clicking
	            cPlayer.addMapClaim(i, claim);
	            cPlayer.addMapLoc(i, claim.getLocation());
	            
	            // Set owner item
	        	ItemStack item = instance.getPlayerMain().getPlayerHead(owner);
	        	if(item == null) {
	        		item = new ItemStack(Material.PLAYER_HEAD);
	        	}
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName(title);
	            meta.setLore(lore);
	            item.setItemMeta(meta);
	            inv.setItem(i, item);
	        }
	        
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
            line = line.replace("%description%", claim.getDescription())
                .replace("%name%", claim.getName())
                .replace("%coords%", instance.getMain().getClaimCoords(claim));
            if (line.contains("%members%")) {
                lore.addAll(Arrays.asList(getMembers(claim).split("\n")));
            } else {
                lore.add(line);
            }
        }
        addEconomyLore(player, claim, lore);
        addVisitorLore(claim, lore, player);
        return lore;
    }

    /**
     * Add the economy lore if enabled.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim object.
     * @param lore   The lore list to modify.
     */
    private void addEconomyLore(Player player, Claim claim, List<String> lore) {
        if (instance.getSettings().getBooleanSetting("economy") && claim.getSale()) {
            Collections.addAll(lore, instance.getLanguage().getMessage("all-claim-buyable-price")
                .replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
                .replace("%money-symbol%",instance.getLanguage().getMessage("money-symbol"))
                .split("\n"));
            lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.buy") ? instance.getLanguage().getMessage("all-claim-is-buyable") : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-buy"));
        }
    }

    /**
     * Add the visitor lore if enabled.
     * 
     * @param claim  The claim object.
     * @param lore   The lore list to modify.
     * @param player The player who opened the GUI.
     */
    private void addVisitorLore(Claim claim, List<String> lore, Player player) {
        String visitorMessage = claim.getPermissionForPlayer("GuiTeleport",player) || claim.getOwner().equals(player.getName()) || instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.guiteleport") ? 
            instance.getLanguage().getMessage("access-all-claim-lore-allow-visitors") : 
            instance.getLanguage().getMessage("access-all-claim-lore-deny-visitors");
        lore.add(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp") ? visitorMessage : instance.getLanguage().getMessage("gui-button-no-permission") + instance.getLanguage().getMessage("to-teleport"));
    }

    /**
     * Get the index of the current filter.
     * 
     * @param filter The current filter.
     * @return The index of the filter.
     */
    private int getStatusIndex(String filter) {
        switch (filter) {
            case "sales":
                return 2;
            default:
                return 1;
        }
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
            return instance.getLanguage().getMessage("claim-list-no-member");
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
