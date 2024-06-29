package fr.xyness.SCS.Listeners;

import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.AdminClaimGui;
import fr.xyness.SCS.Guis.AdminClaimListGui;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimGui;
import fr.xyness.SCS.Guis.ClaimListGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Guis.ClaimsGui;
import fr.xyness.SCS.Guis.ClaimsOwnerGui;

/**
 * Event listener for claim guis events.
 */
public class ClaimGuiEvents implements Listener {
	
	// ******************
	// *  EventHandler  *
	// ******************

	/**
	 * Handles inventory click events in the claim GUIs.
	 * @param event the inventory click event.
	 */
	@EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
    	Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        Inventory openInventory = event.getView().getTopInventory();

        if (inv != null && inv.equals(openInventory)) {
        	
        	InventoryHolder holder = event.getInventory().getHolder();
        	CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        	
        	if (holder instanceof ClaimGui) {
        		handleClaimGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminClaimGui) {
        		handleAdminClaimGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimMembersGui) {
        		handleClaimMembersGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimBansGui) {
        		handleClaimBansGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimListGui) {
        		handleClaimListGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminClaimListGui) {
        		handleAdminClaimListGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimsGui) {
        		handleClaimsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimsOwnerGui) {
        		handleClaimsOwnerGuiClick(event, player, cPlayer);
        	}
        }
	}
	
    // ********************
    // *  Others Methods  *
    // ********************
	
	/**
     * Handles claim GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Chunk chunk = cPlayer.getChunk();
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "define-loc")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setspawn")) return;
        	if(chunk.equals(player.getLocation().getChunk())) {
            	player.closeInventory();
            	Location l = player.getLocation();
            	ClaimMain.setClaimLocation(player, chunk, l);
            	player.sendMessage(ClaimLanguage.getMessage("loc-change-success").replaceAll("%coords%", ClaimMain.getClaimCoords(chunk)));
            	return;
        	}
        	player.sendMessage(ClaimLanguage.getMessage("error-not-right-claim"));
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "manage-members")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) return;
        	cPlayer.setGuiPage(1);
            ClaimMembersGui menu = new ClaimMembersGui(player,chunk,1);
            menu.openInventory(player);
            return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "manage-bans")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) return;
        	cPlayer.setGuiPage(1);
            new ClaimBansGui(player,chunk,1);
            return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "define-name")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) return;
        	player.closeInventory();
        	player.sendMessage(ClaimLanguage.getMessage("name-change-ask"));
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "my-claims")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.list")) return;
        	cPlayer.setGuiPage(1);
        	cPlayer.setChunk(chunk);
            new ClaimListGui(player,1,"owner");
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "apply-all-claims")) {
        	player.closeInventory();
        	if(ClaimMain.applyAllSettings(chunk, player)) {
        		player.sendMessage(ClaimLanguage.getMessage("apply-all-settings-success"));
        	}
        	return;
        }
        
        if(ClaimGuis.isAllowedSlot(clickedSlot)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String check = lore.get(lore.size()-1);
                if(check.equals(ClaimLanguage.getMessage("choice-setting-disabled"))) return;
                String action = ClaimGuis.getSlotPerm(clickedSlot);
                if(!CPlayerMain.checkPermPlayer(player, "scs.setting."+action)) return;
                if(title.contains(ClaimLanguage.getMessage("status-enabled"))){
                    if(ClaimMain.updatePerm(player, chunk, action, false)) {
                    	title = title.replace(ClaimLanguage.getMessage("status-enabled"), ClaimLanguage.getMessage("status-disabled"));
                    	meta.setDisplayName(title);
                    	lore.remove(lore.size()-1);
                    	lore.add(ClaimLanguage.getMessage("choice-disabled"));
                    	meta.setLore(lore);
                    	clickedItem.setItemMeta(meta);
                    	return;
                    }
                    player.closeInventory();
                    player.sendMessage(ClaimLanguage.getMessage("error"));
                    return;
                }
                if(ClaimMain.updatePerm(player, chunk, action, true)) {
                	title = title.replace(ClaimLanguage.getMessage("status-disabled"), ClaimLanguage.getMessage("status-enabled"));
                	meta.setDisplayName(title);
                	lore.remove(lore.size()-1);
                	lore.add(ClaimLanguage.getMessage("choice-enabled"));
                	meta.setLore(lore);
                	clickedItem.setItemMeta(meta);
                	return;
                }
                player.closeInventory();
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return;
            }
        }
        
        ClaimGuis.executeAction(player, "settings", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles admin claim GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminClaimGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Chunk chunk = cPlayer.getChunk();
        
        if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "define-loc")) {
        	if(chunk.equals(player.getLocation().getChunk())) {
            	player.closeInventory();
            	Location l = player.getLocation();
            	ClaimMain.setClaimLocation(player, chunk, l);
            	player.sendMessage(ClaimLanguage.getMessage("loc-change-success").replaceAll("%coords%", ClaimMain.getClaimCoords(chunk)));
            	return;
        	}
        	player.sendMessage(ClaimLanguage.getMessage("error-not-right-claim"));
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "manage-members")) {
        	cPlayer.setGuiPage(1);
            ClaimMembersGui menu = new ClaimMembersGui(player,chunk,1);
            menu.openInventory(player);
            return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "manage-bans")) {
        	cPlayer.setGuiPage(1);
            new ClaimBansGui(player,chunk,1);
            return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "define-name")) {
        	player.closeInventory();
        	player.sendMessage(ClaimLanguage.getMessage("name-change-ask"));
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "admin-claims")) {
        	cPlayer.setGuiPage(1);
        	cPlayer.setChunk(chunk);
        	new AdminClaimListGui(player,1);
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "apply-all-admin-claims")) {
        	player.closeInventory();
        	if(ClaimMain.applyAllSettingsAdmin(chunk)) {
        		player.sendMessage(ClaimLanguage.getMessage("apply-all-admin-settings-success"));
        	}
        	return;
        }
        
        if(ClaimGuis.isAllowedSlot(clickedSlot)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String check = lore.get(lore.size()-1);
                if(check.equals(ClaimLanguage.getMessage("choice-setting-disabled"))) return;
                String action = ClaimGuis.getSlotPerm(clickedSlot);
                if(title.contains(ClaimLanguage.getMessage("status-enabled"))){
                    if(ClaimMain.updateAdminPerm(chunk, action, false)) {
                    	title = title.replace(ClaimLanguage.getMessage("status-enabled"), ClaimLanguage.getMessage("status-disabled"));
                    	meta.setDisplayName(title);
                    	lore.remove(lore.size()-1);
                    	lore.add(ClaimLanguage.getMessage("choice-disabled"));
                    	meta.setLore(lore);
                    	clickedItem.setItemMeta(meta);
                    	return;
                    }
                    player.closeInventory();
                    player.sendMessage(ClaimLanguage.getMessage("error"));
                    return;
                }
                if(ClaimMain.updateAdminPerm(chunk, action, true)) {
                	title = title.replace(ClaimLanguage.getMessage("status-disabled"), ClaimLanguage.getMessage("status-enabled"));
                	meta.setDisplayName(title);
                	lore.remove(lore.size()-1);
                	lore.add(ClaimLanguage.getMessage("choice-enabled"));
                	meta.setLore(lore);
                	clickedItem.setItemMeta(meta);
                	return;
                }
                player.closeInventory();
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return;
            }
        }
        
        ClaimGuis.executeAction(player, "admin_settings", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles claim members GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimMembersGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Chunk chunk = cPlayer.getChunk();
        
        if (clickedSlot == ClaimGuis.getItemSlot("members", "back-page-list")) {
        	int page = cPlayer.getGuiPage();
        	if(ClaimGuis.getItemSlot("members", "back-page-list") == ClaimGuis.getItemSlot("members", "back-page-settings") && page == 1) {
            	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
	                new AdminClaimGui(player,chunk);
	                return;
            	}
                new ClaimGui(player,chunk);
                return;
        	}
        	cPlayer.setGuiPage(page-1);
            ClaimMembersGui menu = new ClaimMembersGui(player,chunk,page-1);
            menu.openInventory(player);
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("members", "back-page-settings")) {
        	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
                new AdminClaimGui(player,chunk);
                return;
        	}
            new ClaimGui(player,chunk);
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("members", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            ClaimMembersGui menu = new ClaimMembersGui(player,chunk,page);
            menu.openInventory(player);
            return;
        }
        
        if(clickedSlot >= ClaimGuis.getGuiMinSlot("members") && clickedSlot <= ClaimGuis.getGuiMaxSlot("members")) {
        	String owner = cPlayer.getMapString(clickedSlot);
        	if(owner.equals(player.getName())) return;
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) return;
        	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
        		if(ClaimMain.removeAdminClaimMembers(chunk, owner)) {
        			String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", owner).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
        			player.sendMessage(message);
        		}
        	} else {
        		if(ClaimMain.removeClaimMembers(player, chunk, owner)) {
        			String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", owner).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
        			player.sendMessage(message);
        		}
        	}
            int page = cPlayer.getGuiPage();
        	ClaimMembersGui menu = new ClaimMembersGui(player,chunk,page);
        	menu.openInventory(player);
            return;
        }
        
        ClaimGuis.executeAction(player, "members", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles claim bans GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimBansGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Chunk chunk = cPlayer.getChunk();
        
        if (clickedSlot == ClaimGuis.getItemSlot("bans", "back-page-list")) {
        	int page = cPlayer.getGuiPage();
        	if(ClaimGuis.getItemSlot("bans", "back-page-list") == ClaimGuis.getItemSlot("bans", "back-page-settings") && page == 1) {
            	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
	                new AdminClaimGui(player,chunk);
	                return;
            	}
                new ClaimGui(player,chunk);
                return;
        	}
        	cPlayer.setGuiPage(page-1);
            new ClaimBansGui(player,chunk,page-1);
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("bans", "back-page-settings")) {
        	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
                new AdminClaimGui(player,chunk);
                return;
        	}
            new ClaimGui(player,chunk);
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("bans", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimBansGui(player,chunk,page);
            return;
        }
        
        if(clickedSlot >= ClaimGuis.getGuiMinSlot("bans") && clickedSlot <= ClaimGuis.getGuiMaxSlot("bans")) {
        	String owner = cPlayer.getMapString(clickedSlot);
        	if(owner.equals(player.getName())) return;
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) return;
        	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
        		if(ClaimMain.removeAdminClaimBan(chunk, owner)) {
        			String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", owner).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
        			player.sendMessage(message);
        		}
        	} else {
        		if(ClaimMain.removeClaimBan(player, chunk, owner)) {
        			String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", owner).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
        			player.sendMessage(message);
        		}
        	}
            int page = cPlayer.getGuiPage();
        	new ClaimBansGui(player,chunk,page);
            return;
        }
        
        ClaimGuis.executeAction(player, "bans", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles claim list GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimListGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == ClaimGuis.getItemSlot("list", "back-page-list")) {
        	int page = cPlayer.getGuiPage()-1;
        	if(ClaimGuis.getItemSlot("list", "back-page-list") == ClaimGuis.getItemSlot("list", "back-page-settings") && page == 0) {
        		if(!ClaimMain.checkIfClaimExists(cPlayer.getChunk())) {
        			player.sendMessage(ClaimLanguage.getMessage("the-claim-does-not-exists-anymore"));
        			return;
        		}
        		new ClaimGui(player,cPlayer.getChunk());
        		return;
        	}
        	cPlayer.setGuiPage(page);
            new ClaimListGui(player,page,cPlayer.getFilter());
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("list", "filter")) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("owner")) {
        		filter = "not_owner";
        	} else {
        		filter = "owner";
        	}
            new ClaimListGui(player,1,filter);
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("list", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimListGui(player,page,cPlayer.getFilter());
            return;
        }
        
        if(clickedSlot >= ClaimGuis.getGuiMinSlot("list") && clickedSlot <= ClaimGuis.getGuiMaxSlot("list")) {
            if(event.getClick() == ClickType.LEFT) {
            	if(CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) {
	            	player.closeInventory();
		        	ClaimMain.goClaim(player, cPlayer.getMapLoc(clickedSlot));
		        	return;
            	}
            	return;
            }
            if(cPlayer.getFilter().equals("not_owner")) return;
            if(event.getClick() == ClickType.RIGHT) {
            	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) return;
                new ClaimGui(player,cPlayer.getMapChunk(clickedSlot));
	        	return;
            }
            if(event.getClick() == ClickType.SHIFT_LEFT) {
            	if (!CPlayerMain.checkPermPlayer(player, "scs.command.unclaim")) return;
            	Chunk chunk = cPlayer.getMapChunk(clickedSlot);
	        	if(ClaimMain.deleteClaim(player, chunk)) {
	    			player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
	            	int page = cPlayer.getGuiPage();
                    new ClaimListGui(player,page,cPlayer.getFilter());
	        	}
	        	return;
            }
            if(event.getClick() == ClickType.SHIFT_RIGHT) {
        		if(ClaimSettings.getBooleanSetting("economy")) {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.sclaim")) return;
        			Chunk chunk = cPlayer.getMapChunk(clickedSlot);
        			if(ClaimMain.claimIsInSale(chunk)) {
            			if(ClaimMain.delChunkSale(player, chunk)) {
            				player.sendMessage(ClaimLanguage.getMessage("claim-in-sale-cancel").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)));
    			            	int page = cPlayer.getGuiPage();
    		                    new ClaimListGui(player,page,cPlayer.getFilter());
            				return;
            			}
            			player.sendMessage(ClaimLanguage.getMessage("error"));
            			return;
            		}
        			player.sendMessage(ClaimLanguage.getMessage("claim-is-not-in-sale"));
        			return;
        		}
        		player.sendMessage(ClaimLanguage.getMessage("economy-disabled"));
        		return;
            }
        }
        
        ClaimGuis.executeAction(player, "list", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles admin claim list GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminClaimListGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == ClaimGuis.getItemSlot("admin_list", "back-page-list")) {
        	int page = cPlayer.getGuiPage()-1;
        	if(ClaimGuis.getItemSlot("admin_list", "back-page-list") == ClaimGuis.getItemSlot("admin_list", "back-page-settings") && page == 0) {
        		if(!ClaimMain.checkIfClaimExists(cPlayer.getChunk())) {
        			player.sendMessage(ClaimLanguage.getMessage("the-claim-does-not-exists-anymore"));
        			return;
        		}
        		new AdminClaimGui(player,cPlayer.getChunk());
        		return;
        	}
        	cPlayer.setGuiPage(page);
            new AdminClaimListGui(player,page);
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("admin_list", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new AdminClaimListGui(player,page);
            return;
        }
        
        if(clickedSlot >= ClaimGuis.getGuiMinSlot("admin_list") && clickedSlot <= ClaimGuis.getGuiMaxSlot("admin_list")) {
            if(event.getClick() == ClickType.LEFT) {
            	player.closeInventory();
	        	ClaimMain.goClaim(player, cPlayer.getMapLoc(clickedSlot));
	        	return;
            }
            if(event.getClick() == ClickType.RIGHT) {
                new AdminClaimGui(player,cPlayer.getMapChunk(clickedSlot));
	        	return;
            }
            if(event.getClick() == ClickType.SHIFT_LEFT) {
            	Chunk chunk = cPlayer.getMapChunk(clickedSlot);
	        	if(ClaimMain.deleteClaim(player, chunk)) {
	    			player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
	            	int page = cPlayer.getGuiPage();
                    new AdminClaimListGui(player,page);
	        	}
	        	return;
            }
        }
        
        ClaimGuis.executeAction(player, "admin_list", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles claims GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimsGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == ClaimGuis.getItemSlot("claims", "back-page-list")) {
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimsGui(player,page,cPlayer.getFilter());
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("claims", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimsGui(player,page,cPlayer.getFilter());
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("claims", "filter")) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("all")) {
        		filter = "sales";
        	} else if (filter.equals("sales")) {
        		filter = "online";
        	} else if (filter.equals("online")) {
        		filter = "offline";
        	} else {
        		filter = "all";
        	}
            new ClaimsGui(player,1,filter);
            return;
        }
        
        if(clickedSlot >= ClaimGuis.getGuiMinSlot("claims") && clickedSlot <= ClaimGuis.getGuiMaxSlot("claims")) {
        	String filter = cPlayer.getFilter();
        	cPlayer.setGuiPage(1);
        	if(filter.equals("sales")) {
        		new ClaimsOwnerGui(player,1,filter,cPlayer.getMapString(clickedSlot));
        		return;
        	}
        	new ClaimsOwnerGui(player,1,"all",cPlayer.getMapString(clickedSlot));
        	return;
        }
        
        ClaimGuis.executeAction(player, "claims", clickedSlot, event.getClick());
        return;
    }

    /**
     * Handles claims owner GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimsOwnerGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == ClaimGuis.getItemSlot("claims_owner", "back-page-list")) {
        	if(cPlayer.getGuiPage() == 1) {
        		new ClaimsGui(player,1,"all");
        		return;
        	}
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner());
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("claims_owner", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
        	new ClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner());
            return;
        }
        
        if (clickedSlot == ClaimGuis.getItemSlot("claims_owner", "filter")) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("all")) {
        		filter = "sales";
        	} else {
        		filter = "all";
        	}
            new ClaimsOwnerGui(player,1,filter,cPlayer.getOwner());
            return;
        }
        
        if(clickedSlot >= ClaimGuis.getGuiMinSlot("claims_owner") && clickedSlot <= ClaimGuis.getGuiMaxSlot("claims_owner")) {
        	Chunk chunk = cPlayer.getMapChunk(clickedSlot);
        	if(event.getClick() == ClickType.LEFT) {
        		if(CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) {
		            if(!ClaimMain.canPermCheck(chunk, "Visitors") && !ClaimMain.getOwnerInClaim(chunk).equals(player.getName())) return;
	            	player.closeInventory();
		        	ClaimMain.goClaim(player, cPlayer.getMapLoc(clickedSlot));
		        	return;
        		}
        		return;
        	}
        	if(event.getClick() == ClickType.SHIFT_LEFT) {
        		if(ClaimSettings.getBooleanSetting("economy")) {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.sclaim")) return;
        			if(ClaimMain.getOwnerInClaim(chunk).equals(player.getName())) {
        				player.sendMessage(ClaimLanguage.getMessage("cant-buy-your-own-claim"));
        				return;
        			}
            		if(ClaimMain.claimIsInSale(chunk)) {
            			ClaimMain.sellChunk(player, chunk);
            			return;
            		}
            		player.sendMessage(ClaimLanguage.getMessage("claim-is-not-in-sale"));
            		return;
        		}
        		player.sendMessage(ClaimLanguage.getMessage("economy-disabled"));
        		return;
        	}
        }
        
        ClaimGuis.executeAction(player, "claims_owner", clickedSlot, event.getClick());
        return;
    }
	
}
