package fr.xyness.SCS.Listeners;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
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
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimChunksGui;
import fr.xyness.SCS.Guis.ClaimMainGui;
import fr.xyness.SCS.Guis.ClaimListGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Guis.ClaimSettingsGui;
import fr.xyness.SCS.Guis.ClaimsGui;
import fr.xyness.SCS.Guis.ClaimsOwnerGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimBansGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimChunksGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMembersGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsOwnerGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsProtectedAreasGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionSettingWorldsGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionSettingsSettingsGui;

/**
 * Event listener for claim guis events.
 */
public class ClaimGuiEvents implements Listener {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimGuiEvents.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimGuiEvents(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
	
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
        	CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getName());
        	
        	if (holder instanceof ClaimMainGui) {
        		handleClaimMainGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimSettingsGui) {
        		handleClaimSettingsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimMembersGui) {
        		handleClaimMembersGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimChunksGui) {
        		handleClaimChunksGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimBansGui) {
        		handleClaimBansGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimListGui) {
        		handleClaimListGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimsGui) {
        		handleClaimsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimsOwnerGui) {
        		handleClaimsOwnerGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionGui) {
        		handleAdminGestionGuiClick(event, player);
        	} else if (holder instanceof AdminGestionMainGui) {
        		handleAdminGestionMainGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimsGui) {
        		handleAdminGestionClaimsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimsOwnerGui) {
        		handleAdminGestionClaimsOwnerGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimGui) {
        		handleAdminGestionClaimGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimMembersGui) {
        		handleAdminGestionClaimMembersGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimBansGui) {
        		handleAdminGestionClaimBansGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimsProtectedAreasGui) {
        		handleAdminGestionClaimsProtectedAreasGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionSettingWorldsGui) {
        		handleAdminGestionSettingWorldsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimMainGui) {
        		handleAdminGestionClaimMainGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimChunksGui) {
        		handleAdminGestionClaimChunksGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionSettingsSettingsGui) {
        		handleAdminGestionSettingsSettingsClick(event, player, cPlayer);
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
    private void handleClaimMainGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "manage-bans")) {
        	cPlayer.setGuiPage(1);
        	new ClaimBansGui(player,claim,1,instance);
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "manage-members")) {
        	cPlayer.setGuiPage(1);
        	new ClaimMembersGui(player,claim,1,instance);
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "manage-settings")) {
        	new ClaimSettingsGui(player,claim,instance);
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "manage-chunks")) {
        	cPlayer.setGuiPage(1);
        	new ClaimChunksGui(player,claim,1,instance);
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "unclaim")) {
        	player.closeInventory();
        	Bukkit.dispatchCommand(player, "unclaim "+claim.getName());
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "teleport-claim")) {
        	player.closeInventory();
        	instance.getMain().goClaim(player, claim.getLocation());
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("main", "claim-info")) {
        	cPlayer.setGuiPage(1);
        	new ClaimListGui(player,1,"owner",instance);
        	return;
        }
        
        instance.getGuis().executeAction(player, "main", clickedSlot, event.getClick());
        return;
    }
    
	/**
     * Handles claim GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimSettingsGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if(clickedSlot == instance.getGuis().getItemSlot("settings", "apply-all-claims")) {
        	player.closeInventory();
        	instance.getMain().applyAllSettings(claim, player)
        		.thenAccept(success -> {
        			if (success) {
        				player.sendMessage(instance.getLanguage().getMessage("apply-all-settings-success"));
        			} else {
        				player.sendMessage(instance.getLanguage().getMessage("error"));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        	return;
        }
        
        if(clickedSlot == instance.getGuis().getItemSlot("settings", "back-page-main")) {
        	new ClaimMainGui(player,claim,instance);
        	return;
        }
        
        if(instance.getGuis().isAllowedSlot(clickedSlot)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String check = lore.get(lore.size()-1);
                if(check.equals(instance.getLanguage().getMessage("choice-setting-disabled"))) return;
                String action = instance.getGuis().getSlotPerm(clickedSlot);
                if(!instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+action)) return;
                if(title.contains(instance.getLanguage().getMessage("status-enabled"))){
                	instance.getMain().updatePerm(player, claim, action, false)
                		.thenAccept(success -> {
                			if (success) {
                            	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-enabled"), instance.getLanguage().getMessage("status-disabled")));
                            	lore.remove(lore.size()-1);
                            	lore.add(instance.getLanguage().getMessage("choice-disabled"));
                            	meta.setLore(lore);
                            	clickedItem.setItemMeta(meta);
                			} else {
                                player.closeInventory();
                                player.sendMessage(instance.getLanguage().getMessage("error"));
                			}
                		})
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    return;
                }
                instance.getMain().updatePerm(player, claim, action, true)
                	.thenAccept(success -> {
                		if (success) {
                        	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-disabled"), instance.getLanguage().getMessage("status-enabled")));
                        	lore.remove(lore.size()-1);
                        	lore.add(instance.getLanguage().getMessage("choice-enabled"));
                        	meta.setLore(lore);
                        	clickedItem.setItemMeta(meta);
                		} else {
                            player.closeInventory();
                            player.sendMessage(instance.getLanguage().getMessage("error"));
                		}
                	})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
                return;
            }
        }
        
        instance.getGuis().executeAction(player, "settings", clickedSlot, event.getClick());
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
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if (clickedSlot == instance.getGuis().getItemSlot("members", "back-page-list")) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new ClaimMembersGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("members", "back-page-main")) {
            new ClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("members", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimMembersGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= instance.getGuis().getGuiMinSlot("members") && clickedSlot <= instance.getGuis().getGuiMaxSlot("members")) {
        	String owner = cPlayer.getMapString(clickedSlot);
        	String owner_claim = claim.getOwner();
        	if(owner.equals(player.getName()) && !owner_claim.equals("admin")) return;
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")) return;
        	String message = instance.getLanguage().getMessage("remove-member-success").replaceAll("%player%", owner).replaceAll("%claim-name%", claim.getName());
        	instance.getMain().removeClaimMembers(player, claim, owner)
        		.thenAccept(success -> {
        			if (success) {
        				player.sendMessage(message);
        	            int page = cPlayer.getGuiPage();
        	        	new ClaimMembersGui(player,claim,page,instance);
        			} else {
        				player.closeInventory();
        				player.sendMessage(instance.getLanguage().getMessage("error"));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            return;
        }
        
        instance.getGuis().executeAction(player, "members", clickedSlot, event.getClick());
        return;
    }
    
    /**
     * Handles claim chunks GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimChunksGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if (clickedSlot == instance.getGuis().getItemSlot("chunks", "back-page-list")) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new ClaimChunksGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("chunks", "back-page-main")) {
            new ClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("chunks", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimChunksGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= instance.getGuis().getGuiMinSlot("chunks") && clickedSlot <= instance.getGuis().getGuiMaxSlot("chunks")) {
        	String chunk = cPlayer.getMapString(clickedSlot);
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")) return;
        	if (claim.getChunks().size() == 1) return;
        	instance.getMain().removeChunk(claim, chunk)
    		.thenAccept(success -> {
    			if (success) {
    				player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replaceAll("%chunk%", "["+chunk+"]").replaceAll("%claim-name%", claim.getName()));
    	            int page = cPlayer.getGuiPage();
    	        	new ClaimChunksGui(player,claim,page,instance);
    			} else {
    				player.sendMessage(instance.getLanguage().getMessage("error"));
    			}
    		})
            .exceptionally(ex -> {
                ex.printStackTrace();
                return null;
            });
            return;
        }
        
        instance.getGuis().executeAction(player, "chunks", clickedSlot, event.getClick());
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
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if (clickedSlot == instance.getGuis().getItemSlot("bans", "back-page-list")) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new ClaimBansGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("bans", "back-page-main")) {
            new ClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("bans", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimBansGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= instance.getGuis().getGuiMinSlot("bans") && clickedSlot <= instance.getGuis().getGuiMaxSlot("bans")) {
        	String owner = cPlayer.getMapString(clickedSlot);
        	if(owner.equals(player.getName())) return;
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban")) return;
        	String message = instance.getLanguage().getMessage("remove-ban-success").replaceAll("%player%", owner).replaceAll("%claim-name%", claim.getName());
        	instance.getMain().removeClaimBan(player, claim, owner)
        		.thenAccept(success -> {
        			if (success) {
        				player.sendMessage(message);
        	            int page = cPlayer.getGuiPage();
        	        	new ClaimBansGui(player,claim,page,instance);
        			} else {
        				player.closeInventory();
        				player.sendMessage(instance.getLanguage().getMessage("error"));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            return;
        }
        
        instance.getGuis().executeAction(player, "bans", clickedSlot, event.getClick());
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
        
        if (clickedSlot == instance.getGuis().getItemSlot("list", "back-page-list")) {
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimListGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("list", "back-page-main")) {
    		if(!instance.getMain().checkIfClaimExists(cPlayer.getClaim())) {
    			player.sendMessage(instance.getLanguage().getMessage("the-claim-does-not-exists-anymore"));
    			player.closeInventory();
    			return;
    		}
    		new ClaimMainGui(player,cPlayer.getClaim(),instance);
    		return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("list", "filter")) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("owner")) {
        		filter = "not_owner";
        	} else {
        		filter = "owner";
        	}
            new ClaimListGui(player,1,filter,instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("list", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimListGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if(clickedSlot >= instance.getGuis().getGuiMinSlot("list") && clickedSlot <= instance.getGuis().getGuiMaxSlot("list")) {
            if(event.getClick() == ClickType.LEFT) {
            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
	            	player.closeInventory();
		        	instance.getMain().goClaim(player, cPlayer.getMapLoc(clickedSlot));
		        	return;
            	}
            	return;
            }
            if(cPlayer.getFilter().equals("not_owner")) return;
            if(event.getClick() == ClickType.RIGHT) {
            	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim")) return;
                new ClaimMainGui(player,cPlayer.getMapClaim(clickedSlot),instance);
	        	return;
            }
            if(event.getClick() == ClickType.SHIFT_LEFT) {
            	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim")) return;
            	Claim claim = cPlayer.getMapClaim(clickedSlot);
            	instance.getMain().deleteClaim(player, claim)
            		.thenAccept(success -> {
            			if (success) {
            				player.sendMessage(instance.getLanguage().getMessage("territory-delete-success"));
                        	int page = cPlayer.getGuiPage();
                            new ClaimListGui(player,page,cPlayer.getFilter(),instance);
            			} else {
            				player.closeInventory();
            				player.sendMessage(instance.getLanguage().getMessage("error"));
            			}
            		})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
	        	return;
            }
        }
        
        instance.getGuis().executeAction(player, "list", clickedSlot, event.getClick());
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
        
        if (clickedSlot == instance.getGuis().getItemSlot("claims", "back-page-list")) {
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimsGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("claims", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimsGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("claims", "filter")) {
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
            new ClaimsGui(player,1,filter,instance);
            return;
        }
        
        if(clickedSlot >= instance.getGuis().getGuiMinSlot("claims") && clickedSlot <= instance.getGuis().getGuiMaxSlot("claims")) {
        	String filter = cPlayer.getFilter();
        	cPlayer.setGuiPage(1);
        	if(filter.equals("sales")) {
        		new ClaimsOwnerGui(player,1,filter,cPlayer.getMapString(clickedSlot),instance);
        		return;
        	}
        	new ClaimsOwnerGui(player,1,"all",cPlayer.getMapString(clickedSlot),instance);
        	return;
        }
        
        instance.getGuis().executeAction(player, "claims", clickedSlot, event.getClick());
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
        
        if (clickedSlot == instance.getGuis().getItemSlot("claims_owner", "back-page-list")) {
        	if(cPlayer.getGuiPage() == 1) {
        		new ClaimsGui(player,1,"all",instance);
        		return;
        	}
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner(),instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("claims_owner", "next-page-list")) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
        	new ClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner(),instance);
            return;
        }
        
        if (clickedSlot == instance.getGuis().getItemSlot("claims_owner", "filter")) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("all")) {
        		filter = "sales";
        	} else {
        		filter = "all";
        	}
            new ClaimsOwnerGui(player,1,filter,cPlayer.getOwner(),instance);
            return;
        }
        
        if(clickedSlot >= instance.getGuis().getGuiMinSlot("claims_owner") && clickedSlot <= instance.getGuis().getGuiMaxSlot("claims_owner")) {
        	Claim claim = cPlayer.getMapClaim(clickedSlot);
        	if(event.getClick() == ClickType.LEFT) {
        		if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
		            if(!claim.getPermission("Visitors") && !claim.getOwner().equals(player.getName())) return;
	            	player.closeInventory();
		        	instance.getMain().goClaim(player, cPlayer.getMapLoc(clickedSlot));
		        	return;
        		}
        		return;
        	}
        	if(event.getClick() == ClickType.SHIFT_LEFT) {
        		if(instance.getSettings().getBooleanSetting("economy")) {
                	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.buy")) return;
        			if(claim.getOwner().equals(player.getName())) {
        				player.sendMessage(instance.getLanguage().getMessage("cant-buy-your-own-claim"));
        				return;
        			}
            		if(claim.getSale()) {
            			instance.getMain().sellChunk(player, claim);
            			return;
            		}
            		player.sendMessage(instance.getLanguage().getMessage("claim-is-not-in-sale"));
            		return;
        		}
        		player.sendMessage(instance.getLanguage().getMessage("economy-disabled"));
        		return;
        	}
        }
        
        instance.getGuis().executeAction(player, "claims_owner", clickedSlot, event.getClick());
        return;
    }
    
	/**
     * Handles admin gestion main claim GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionMainGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        } else {
            return;
        }

        int clickedSlot = event.getSlot();
        
        if(clickedSlot == 20) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimsGui(player,1,"all",instance);
        	return;
        }
        
        if(clickedSlot == 21) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimsProtectedAreasGui(player,1,"all",instance);
        	return;
        }
        
        if(clickedSlot == 23) {
        	new AdminGestionGui(player,instance);
        	return;
        }
        
        if(clickedSlot == 24) {
        	instance.getAutopurge().purgeClaims();
        	player.sendMessage(instance.getLanguage().getMessage("purge-started-manualy"));
        	player.closeInventory();
        	return;
        }
    }
    
	/**
     * Handles admin gestion setting worlds GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionSettingWorldsGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        } else {
            return;
        }

        int clickedSlot = event.getSlot();
        
        if(clickedSlot == 49) {
        	new AdminGestionGui(player,instance);
        	return;
        }
        
        String world = cPlayer.getMapString(clickedSlot);
        if(world == null) return;
        
        File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        Set<String> worlds = new HashSet<>(config.getStringList("worlds-disabled"));
        if(worlds.contains(world)) {
        	worlds.remove(world);
        } else {
        	worlds.add(world);
        }
        config.set("worlds-disabled", new ArrayList<>(worlds));
        try {
            config.save(configFile);
            instance.reloadConfig();
            instance.getSettings().addDisabledWorld(world);
            new AdminGestionSettingWorldsGui(player, instance);
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
    
	/**
     * Handles admin gestion claim GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionGuiClick(InventoryClickEvent event, Player player) {
        event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if (clickedItem != null) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f);
        } else {
            return;
        }

        int clickedSlot = event.getSlot();
        File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        
        if(clickedSlot == 49) {
        	new AdminGestionMainGui(player,instance);
        	return;
        }

        if (clickedSlot == 0) {
            File guisDir = new File(instance.getPlugin().getDataFolder(), "langs");
            if (!guisDir.exists()) {
                guisDir.mkdirs();
            }

            FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase().endsWith(".yml");
            File[] files = guisDir.listFiles(ymlFilter);
            if (files == null || files.length <= 1) {
                return;
            }

            String currentLang = config.getString("lang");
            int currentIndex = -1;

            for (int i = 0; i < files.length; i++) {
                if (files[i].getName().equalsIgnoreCase(currentLang)) {
                    currentIndex = i;
                    break;
                }
            }

            int nextIndex = (currentIndex + 1) % files.length;
            String newLang = files[nextIndex].getName();

            config.set("lang", newLang);

            try {
                config.save(configFile);
                instance.reloadLang(player, newLang);
                new AdminGestionGui(player, instance);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (clickedSlot == 1) {
        	Boolean param = config.getBoolean("database");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("database", new_param);
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "aclaim reload");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.RIGHT) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("database-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "database");
        	}
        	return;
        }
        
        if (clickedSlot == 2) {
        	Boolean param = config.getBoolean("auto-purge");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("auto-purge", new_param);
        		instance.getSettings().addSetting("auto-purge", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
            		if(new_param) {
            			instance.getAutopurge().stopPurge();
            		} else {
            			instance.getAutopurge().startPurge(config.getInt("auto-purge-checking"), config.getString("auto-purge-time-without-login"));
            		}
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.RIGHT && param) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("auto-purge-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "auto-purge");
        	}
        	return;
        }
        
        if (clickedSlot == 3) {
        	String param = config.getString("protection-message").toUpperCase();
        	switch(param) {
        		case "ACTION_BAR":
        			param = "BOSSBAR";
        			break;
        		case "BOSSBAR":
        			param = "TITLE";
        			break;
        		case "TITLE":
        			param = "SUBTITLE";
        			break;
        		case "SUBTITLE":
        			param = "CHAT";
        			break;
        		default:
        			param = "ACTION_BAR";
        			break;
        	}
        	config.set("protection-message", param);
        	instance.getSettings().addSetting("protection-message", param);
            try {
                config.save(configFile);
                instance.reloadConfig();
                new AdminGestionGui(player, instance);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (clickedSlot == 4) {
        	if(event.getClick() == ClickType.RIGHT) {
        		new AdminGestionSettingWorldsGui(player, instance);
        	}
        	return;
        }
        
        if (clickedSlot == 5) {
        	Boolean param = config.getBoolean("preload-chunks");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("preload-chunks", new_param);
        		instance.getSettings().addSetting("preload-chunks", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 6) {
        	Boolean param = config.getBoolean("keep-chunks-loaded");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("keep-chunks-loaded", new_param);
        		instance.getSettings().addSetting("keep-chunks-loaded", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 7) {
        	int param = config.getInt("max-length-claim-name");
        	if(event.getClick() == ClickType.LEFT) {
        		param++;
        	}
        	if(event.getClick() == ClickType.RIGHT) {
        		param--;
        	}
        	if(param < 1) param = 1;
        	config.set("max-length-claim-name", param);
    		instance.getSettings().addSetting("max-length-claim-name", String.valueOf(param));
            try {
                config.save(configFile);
                instance.reloadConfig();
                new AdminGestionGui(player, instance);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (clickedSlot == 8) {
        	int param = config.getInt("max-length-claim-description");
        	if(event.getClick() == ClickType.LEFT) {
        		param++;
        	}
        	if(event.getClick() == ClickType.RIGHT) {
        		param--;
        	}
        	if(param < 1) param = 1;
        	config.set("max-length-claim-description", param);
    		instance.getSettings().addSetting("max-length-claim-description", String.valueOf(param));
            try {
                config.save(configFile);
                instance.reloadConfig();
                new AdminGestionGui(player, instance);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }
        
        if (clickedSlot == 9) {
        	Boolean param = config.getBoolean("claim-confirmation");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("claim-confirmation", new_param);
        		instance.getSettings().addSetting("claim-confirmation", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 10) {
        	Boolean param = config.getBoolean("claim-particles");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("claim-particles", new_param);
        		instance.getSettings().addSetting("claim-particles", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 11) {
        	Boolean param = config.getBoolean("claim-fly-disabled-on-damage");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("claim-fly-disabled-on-damage", new_param);
        		instance.getSettings().addSetting("claim-fly-disabled-on-damage", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 12) {
        	Boolean param = config.getBoolean("claim-fly-message-auto-fly");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("claim-fly-message-auto-fly", new_param);
        		instance.getSettings().addSetting("claim-fly-message-auto-fly", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 13) {
        	Boolean param = config.getBoolean("enter-leave-messages");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("enter-leave-messages", new_param);
        		instance.getSettings().addSetting("enter-leave-messages", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 14) {
        	Boolean param = config.getBoolean("enter-leave-title-messages");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("enter-leave-title-messages", new_param);
        		instance.getSettings().addSetting("enter-leave-title-messages", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 15) {
        	Boolean param = config.getBoolean("enter-leave-chat-messages");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("enter-leave-chat-messages", new_param);
        		instance.getSettings().addSetting("enter-leave-chat-messages", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 16) {
        	Boolean param = config.getBoolean("claims-visitors-off-visible");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("claims-visitors-off-visible", new_param);
        		instance.getSettings().addSetting("claims-visitors-off-visible", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 17) {
        	if(!instance.getSettings().getBooleanSetting("vault")) {
        		player.sendMessage(instance.getLanguage().getMessage("vault-required"));
        		return;
        	}
        	Boolean param = config.getBoolean("economy");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("economy", new_param);
        		instance.getSettings().addSetting("economy", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.RIGHT && param) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("economy-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "economy");
        		return;
        	}
        	if(event.getClick() == ClickType.SHIFT_LEFT && param) {
            	param = config.getBoolean("claim-cost");
        		Boolean new_param = param ? false : true;
        		config.set("claim-cost", new_param);
        		instance.getSettings().addSetting("claim-cost", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.SHIFT_RIGHT && param) {
            	param = config.getBoolean("claim-cost-multiplier");
        		Boolean new_param = param ? false : true;
        		config.set("claim-cost-multiplier", new_param);
        		instance.getSettings().addSetting("claim-cost-multiplier", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 18) {
        	Boolean param = config.getBoolean("bossbar");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("bossbar", new_param);
        		instance.getSettings().addSetting("bossbar", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                    Bukkit.getOnlinePlayers().forEach(p -> instance.getBossBars().activeBossBar(p, p.getLocation().getChunk()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if (event.getClick() == ClickType.SHIFT_LEFT && param) {
        	    String param2 = config.getString("bossbar-settings.color");
        	    int i = 0;
        	    BarColor[] values = BarColor.values();

        	    for (BarColor c : values) {
        	        if (c.name().equalsIgnoreCase(param2)) {
        	            break;
        	        }
        	        i++;
        	    }
        	    i = (i + 1) % values.length;
        	    String new_param = values[i].name().toUpperCase();
        	    BarColor new_color = values[i];
        	    config.set("bossbar-settings.color", new_param);
        	    instance.getSettings().addSetting("bossbar-color", new_param);
        	    try {
        	        config.save(configFile);
        	        instance.reloadConfig();
        	        new AdminGestionGui(player, instance);
        	        instance.getBossBars().setBossBarColor(new_color);
        	    } catch (IOException e) {
        	        e.printStackTrace();
        	    }
        	    return;
        	}
        	if (event.getClick() == ClickType.SHIFT_RIGHT && param) {
        	    String param2 = config.getString("bossbar-settings.style");
        	    int i = 0;
        	    BarStyle[] values = BarStyle.values();
        	    for (BarStyle c : values) {
        	        if (c.name().equalsIgnoreCase(param2)) {
        	            break;
        	        }
        	        i++;
        	    }
        	    i = (i + 1) % values.length;
        	    String new_param = values[i].name().toUpperCase();
        	    BarStyle new_style = values[i];
        	    config.set("bossbar-settings.style", new_param);
        	    instance.getSettings().addSetting("bossbar-style", new_param);
        	    try {
        	        config.save(configFile);
        	        instance.reloadConfig();
        	        new AdminGestionGui(player, instance);
        	        instance.getBossBars().setBossBarStyle(new_style);
        	    } catch (IOException e) {
        	        e.printStackTrace();
        	    }
        	    return;
        	}
        }
        
        if (clickedSlot == 19) {
        	Boolean param = config.getBoolean("teleportation-delay-moving");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("teleportation-delay-moving", new_param);
        		instance.getSettings().addSetting("teleportation-delay-moving", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	return;
        }
        
        if (clickedSlot == 20) {
        	Boolean param = config.getBoolean("dynmap");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("dynmap", new_param);
        		instance.getSettings().addSetting("dynmap", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.RIGHT && param) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("map-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "dynmap");
        		return;
        	}
        	return;
        }
        
        if (clickedSlot == 21) {
        	Boolean param = config.getBoolean("bluemap");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("bluemap", new_param);
        		instance.getSettings().addSetting("bluemap", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.RIGHT && param) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("map-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "bluemap");
        		return;
        	}
        	return;
        }
        
        if (clickedSlot == 22) {
        	Boolean param = config.getBoolean("pl3xmap");
        	if(event.getClick() == ClickType.LEFT) {
        		Boolean new_param = param ? false : true;
        		config.set("pl3xmap", new_param);
        		instance.getSettings().addSetting("pl3xmap", String.valueOf(new_param));
                try {
                    config.save(configFile);
                    instance.reloadConfig();
                    new AdminGestionGui(player, instance);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
        	}
        	if(event.getClick() == ClickType.RIGHT && param) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("map-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "pl3xmap");
        		return;
        	}
        	return;
        }
        
        if (clickedSlot == 23) {
        	if(event.getClick() == ClickType.LEFT) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("group2-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "group2");
        		return;
        	}
        	if(event.getClick() == ClickType.RIGHT) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("group-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "group");
        		return;
        	}
        }
        
        if (clickedSlot == 24) {
        	if(event.getClick() == ClickType.LEFT) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("player2-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "player2");
        		return;
        	}
        	if(event.getClick() == ClickType.RIGHT) {
        		player.closeInventory();
        		player.sendMessage(instance.getLanguage().getMessage("player-setup-how-to"));
        		instance.getMain().addPlayerAdminSetting(player, "player");
        		return;
        	}
        }
        
        if (clickedSlot == 25) {
        	if(event.getClick() == ClickType.RIGHT) {
        		new AdminGestionSettingsSettingsGui(player,"status",instance);
        		return;
        	}
        }
        
        if (clickedSlot == 26) {
        	if(event.getClick() == ClickType.RIGHT) {
        		new AdminGestionSettingsSettingsGui(player,"default",instance);
        		return;
        	}
        }
    }
    
    /**
     * Handles admin gestion claims GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimsGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	if(page == 1) {
        		new AdminGestionMainGui(player,instance);
        		return;
        	}
        	page--;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimsGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimsGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == 49) {
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
            new AdminGestionClaimsGui(player,1,filter,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String filter = cPlayer.getFilter();
        	cPlayer.setGuiPage(1);
    		if(event.getClick() == ClickType.SHIFT_LEFT) {
    			String target = cPlayer.getMapString(clickedSlot);
    			instance.getMain().deleteAllClaim(target)
	    			.thenAccept(success -> {
	    				if(success) {
	        				new AdminGestionClaimsGui(player,1,filter,instance);
	        				player.sendMessage(instance.getLanguage().getMessage("player-unclaim-other-all-claim-aclaim").replaceAll("%player%", target));
	        				Player pTarget = Bukkit.getPlayer(target);
	        				if(pTarget != null) {
	        					pTarget.sendMessage(instance.getLanguage().getMessage("player-all-claim-unclaimed-by-admin").replaceAll("%player%", player.getName()));
	        				}
	    				} else {
	    					player.sendMessage(instance.getLanguage().getMessage("error"));
	    				}
	    			})
	    			.exceptionally(ex -> {
	    				ex.printStackTrace();
	    				return null;
	    			});
    			return;
    		}
    		if(event.getClick() == ClickType.LEFT) {
            	new AdminGestionClaimsOwnerGui(player,1,filter,cPlayer.getMapString(clickedSlot),instance);
            	return;
    		}
    		return;
        }
        return;
    }

    /**
     * Handles admin gestion claims owner GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimsOwnerGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == 48) {
        	if(cPlayer.getGuiPage() == 1) {
        		new AdminGestionClaimsGui(player,1,"all",instance);
        		return;
        	}
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner(),instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
        	new AdminGestionClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner(),instance);
            return;
        }
        
        if (clickedSlot == 49) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("all")) {
        		filter = "sales";
        	} else {
        		filter = "all";
        	}
            new AdminGestionClaimsOwnerGui(player,1,filter,cPlayer.getOwner(),instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	Claim claim = cPlayer.getMapClaim(clickedSlot);
        	if(event.getClick() == ClickType.LEFT) {
            	new AdminGestionClaimMainGui(player,claim,instance);
	        	return;
        	}
        	if(event.getClick() == ClickType.SHIFT_LEFT) {
        		String owner = claim.getOwner();
        		String claim_name = claim.getName();
        		instance.getMain().deleteClaim(player, claim)
				.thenAccept(success -> {
					if (success) {
        				player.sendMessage(instance.getLanguage().getMessage("player-unclaim-other-claim-aclaim").replaceAll("%name%", claim_name).replaceAll("%player%", owner));
        				Player target = Bukkit.getPlayer(owner);
        				if(target != null) {
        					target.sendMessage(instance.getLanguage().getMessage("player-claim-unclaimed-by-admin").replaceAll("%name%", claim_name).replaceAll("%player%", player.getName()));
        				}
        				new AdminGestionClaimsOwnerGui(player,cPlayer.getGuiPage(),cPlayer.getFilter(),owner,instance);
					} else {
						player.sendMessage(instance.getLanguage().getMessage("error"));
					}
				})
		        .exceptionally(ex -> {
		            ex.printStackTrace();
		            return null;
		        });
			return;
        	}
        }
        return;
    }
    
	/**
     * Handles admin gestion claim GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if(clickedSlot == 49) {
        	new AdminGestionClaimMainGui(player,claim,instance);
        	return;
        }
        
        if(clickedSlot == 50) {
        	instance.getMain().applyAllSettings(claim, claim.getOwner())
        		.thenAccept(success -> {
        			if (success) {
                		player.sendMessage(instance.getLanguage().getMessage("apply-all-settings-success-aclaim").replaceAll("%player%", claim.getOwner()));
                		String target = cPlayer.getOwner();
                    	if(target.equals("admin")) {
                    		new AdminGestionClaimsProtectedAreasGui(player,1,cPlayer.getFilter(), instance);
                    		return;
                    	}
                    	new AdminGestionClaimsOwnerGui(player,1,cPlayer.getFilter(),target, instance);
        			} else {
        				player.closeInventory();
        				player.sendMessage(instance.getLanguage().getMessage("error"));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        	return;
        }
        
        if(instance.getGuis().isAllowedSlot(clickedSlot)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String check = lore.get(lore.size()-1);
                if(check.equals(instance.getLanguage().getMessage("choice-setting-disabled"))) return;
                String action = instance.getGuis().getSlotPerm(clickedSlot);
                if(!instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+action)) return;
                if(title.contains(instance.getLanguage().getMessage("status-enabled"))){
                	instance.getMain().updatePerm(player, claim, action, false)
                		.thenAccept(success -> {
                			if (success) {
                            	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-enabled"), instance.getLanguage().getMessage("status-disabled")));
                            	lore.remove(lore.size()-1);
                            	lore.add(instance.getLanguage().getMessage("choice-disabled"));
                            	meta.setLore(lore);
                            	clickedItem.setItemMeta(meta);
                			} else {
                                player.closeInventory();
                                player.sendMessage(instance.getLanguage().getMessage("error"));
                			}
                		})
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    return;
                }
                instance.getMain().updatePerm(player, claim, action, true)
                	.thenAccept(success -> {
                		if (success) {
                        	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-disabled"), instance.getLanguage().getMessage("status-enabled")));
                        	lore.remove(lore.size()-1);
                        	lore.add(instance.getLanguage().getMessage("choice-enabled"));
                        	meta.setLore(lore);
                        	clickedItem.setItemMeta(meta);
                		} else {
                            player.closeInventory();
                            player.sendMessage(instance.getLanguage().getMessage("error"));
                		}
                	})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
                return;
            }
        }
        return;
    }
    
    /**
     * Handles admin gestion settings settings GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionSettingsSettingsClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if(clickedSlot == 49) {
        	new AdminGestionGui(player,instance);
        	return;
        }
        
        File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        String filter = cPlayer.getFilter();
        
        if(instance.getGuis().isAllowedSlot(clickedSlot)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String action = instance.getGuis().getSlotPerm(clickedSlot);
                if(title.contains(instance.getLanguage().getMessage("status-enabled"))){
                	if(filter.equals("status")) {
                		config.set("status-settings."+action, false);
                		instance.getSettings().getStatusSettings().put(action, false);
                	} else {
                		config.set("default-values-settings."+action, false);
                		instance.getSettings().getDefaultValues().put(action, false);
                	}
                    try {
                        config.save(configFile);
                        instance.reloadConfig();
                    	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-enabled"), instance.getLanguage().getMessage("status-disabled")));
                    	lore.remove(lore.size()-1);
                    	lore.add(instance.getLanguage().getMessage("choice-disabled"));
                    	meta.setLore(lore);
                    	clickedItem.setItemMeta(meta);
                        return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            	if(filter.equals("status")) {
            		config.set("status-settings."+action, true);
            		instance.getSettings().getStatusSettings().put(action, true);
            	} else {
            		config.set("default-values-settings."+action, true);
            		instance.getSettings().getDefaultValues().put(action, true);
            	}
                try {
                    config.save(configFile);
                    instance.reloadConfig();
	            	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-disabled"), instance.getLanguage().getMessage("status-enabled")));
	            	lore.remove(lore.size()-1);
	            	lore.add(instance.getLanguage().getMessage("choice-enabled"));
	            	meta.setLore(lore);
	            	clickedItem.setItemMeta(meta);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }
        }
        return;
    }
    
    /**
     * Handles admin gestion claim members GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimMembersGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new AdminGestionClaimMembersGui(player,claim,page-1,instance);
            return;
        }
        
        if(clickedSlot == 49) {
        	new AdminGestionClaimMainGui(player,claim,instance);
        	return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimMembersGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String owner = cPlayer.getMapString(clickedSlot);
        	String owner_claim = claim.getOwner();
        	if(owner.equals(player.getName()) && !owner_claim.equals("admin")) return;
        	if(owner_claim.equals("admin")) {
        		String message = instance.getLanguage().getMessage("remove-member-success-aclaim").replaceAll("%player%", owner).replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", instance.getLanguage().getMessage("protected-area-title"));
        		instance.getMain().removeAdminClaimMembers(claim, owner)
        			.thenAccept(success -> {
        				if (success) {
                			player.sendMessage(message);
                            int page = cPlayer.getGuiPage();
                        	new AdminGestionClaimMembersGui(player,claim,page,instance);
        				} else {
        					player.closeInventory();
        					player.sendMessage(instance.getLanguage().getMessage("error"));
        				}
        			})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        	} else {
        		String message = instance.getLanguage().getMessage("remove-member-success-aclaim").replaceAll("%player%", owner).replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", owner_claim);
        		instance.getMain().removeClaimMembers(player, claim, owner)
        			.thenAccept(success -> {
        				if (success) {
        					player.sendMessage(message);
                            int page = cPlayer.getGuiPage();
                        	new AdminGestionClaimMembersGui(player,claim,page,instance);
        				} else {
        					player.closeInventory();
        					player.sendMessage(instance.getLanguage().getMessage("error"));
        				}
        			})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        	}
            return;
        }
        return;
    }
    
    /**
     * Handles admin gestion claim bans GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimBansGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new AdminGestionClaimBansGui(player,claim,page-1,instance);
            return;
        }
        
        if(clickedSlot == 49) {
        	new AdminGestionClaimMainGui(player,claim,instance);
        	return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimBansGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String owner = cPlayer.getMapString(clickedSlot);
        	if(owner.equals(player.getName())) return;
        	if(claim.getOwner().equals("admin")) {
        		String message = instance.getLanguage().getMessage("remove-ban-success-aclaim").replaceAll("%player%", owner).replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", instance.getLanguage().getMessage("protected-area-title"));
        		instance.getMain().removeAdminClaimBan(claim, owner)
        			.thenAccept(success -> {
        				if (success) {
                			player.sendMessage(message);
                            int page = cPlayer.getGuiPage();
                        	new AdminGestionClaimBansGui(player,claim,page,instance);
        				} else {
        					player.closeInventory();
        					player.sendMessage(instance.getLanguage().getMessage("error"));
        				}
        			})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        	} else {
        		String message = instance.getLanguage().getMessage("remove-ban-success-aclaim").replaceAll("%player%", owner).replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", claim.getOwner());
        		instance.getMain().removeClaimBan(player, claim, owner)
        			.thenAccept(success -> {
        				if (success) {
        					player.sendMessage(message);
                            int page = cPlayer.getGuiPage();
                        	new AdminGestionClaimBansGui(player,claim,page,instance);
        				} else {
        					player.closeInventory();
        					player.sendMessage(instance.getLanguage().getMessage("error"));
        				}
        			})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        	}
            return;
        }
        return;
    }
    
    /**
     * Handles admin gestion claims protected areas GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimsProtectedAreasGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        if (clickedSlot == 48) {
        	if(cPlayer.getGuiPage() == 1) {
        		new AdminGestionMainGui(player,instance);
        		return;
        	}
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimsProtectedAreasGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
        	new AdminGestionClaimsProtectedAreasGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == 49) {
        	cPlayer.setGuiPage(1);
        	String filter = cPlayer.getFilter();
        	if(filter.equals("all")) {
        		filter = "sales";
        	} else {
        		filter = "all";
        	}
            new AdminGestionClaimsProtectedAreasGui(player,1,filter,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	Claim claim = cPlayer.getMapClaim(clickedSlot);
        	if(event.getClick() == ClickType.LEFT) {
            	new AdminGestionClaimMainGui(player,claim,instance);
	        	return;
        	}
        	if(event.getClick() == ClickType.SHIFT_LEFT) {
        		instance.getMain().forceDeleteClaim(claim)
        			.thenAccept(success -> {
        				if (success) {
        					player.sendMessage(instance.getLanguage().getMessage("delete-claim-protected-area"));
        					new AdminGestionClaimsProtectedAreasGui(player,cPlayer.getGuiPage(),cPlayer.getFilter(),instance);
        				} else {
        					player.sendMessage(instance.getLanguage().getMessage("error"));
        				}
        			})
        	        .exceptionally(ex -> {
        	            ex.printStackTrace();
        	            return null;
        	        });
        	}
        	return;
        }
        return;
    }
    
	/**
     * Handles admin gestion claim main GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimMainGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if(clickedSlot == 20) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimBansGui(player,claim,1,instance);
        	return;
        }
        
        if(clickedSlot == 30) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimMembersGui(player,claim,1,instance);
        	return;
        }
        
        if(clickedSlot == 29) {
        	new AdminGestionClaimGui(player,claim,instance);
        	return;
        }
        
        if(clickedSlot == 32) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimChunksGui(player,claim,1,instance);
        	return;
        }
        
        if(clickedSlot == 33) {
        	player.closeInventory();
        	instance.getMain().goClaim(player, claim.getLocation());
        	return;
        }
        
        if(clickedSlot == 24) {
        	player.closeInventory();
        	if(claim.getOwner().equalsIgnoreCase("admin")) {
        		Bukkit.dispatchCommand(player, "parea unclaim "+claim.getName());
        	} else {
        		Bukkit.dispatchCommand(player, "scs player unclaim "+claim.getOwner()+" " +claim.getName());
        	}
        	return;
        }
        
        if(clickedSlot == 49) {
        	cPlayer.setGuiPage(1);
        	String owner = claim.getOwner();
        	if(owner.equals("admin")) {
        		new AdminGestionClaimsProtectedAreasGui(player,1,"all",instance);
        	} else {
        		new AdminGestionClaimsOwnerGui(player,1,"all",claim.getOwner(),instance);
        	}
        	return;
        }
        
        return;
    }
    
    /**
     * Handles admin gestion claim chunks GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleAdminGestionClaimChunksGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        
        Claim claim = cPlayer.getClaim();
        if(claim == null) return;
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new AdminGestionClaimChunksGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == 49) {
            new AdminGestionClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new AdminGestionClaimChunksGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String chunk = cPlayer.getMapString(clickedSlot);
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")) return;
        	if (claim.getChunks().size() == 1) return;
        	instance.getMain().removeChunk(claim, chunk)
        		.thenAccept(success -> {
        			if (success) {
        				player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replaceAll("%chunk%", "["+chunk+"]").replaceAll("%claim-name%", claim.getName()));
        	            int page = cPlayer.getGuiPage();
        	        	new AdminGestionClaimChunksGui(player,claim,page,instance);
        			} else {
        				player.sendMessage(instance.getLanguage().getMessage("error"));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            return;
        }
        return;
    }
}
