package fr.xyness.SCS.Listeners;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Commands.ClaimCommand;
import fr.xyness.SCS.Guis.ChunkConfirmationGui;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimChunksGui;
import fr.xyness.SCS.Guis.ClaimConfirmationGui;
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
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

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
	 * Handles inventory close events.
	 * 
	 * @param event The InventoryCloseEvent event.
	 */
    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
    	Player player = (Player) event.getPlayer();
    	Inventory inv = event.getInventory();
    	InventoryHolder holder = inv.getHolder();
    	if(holder instanceof ClaimConfirmationGui) {
    		if(instance.isFolia()) {
    			Bukkit.getAsyncScheduler().runDelayed(instance, task -> ClaimCommand.isOnCreate.remove(player), 500, TimeUnit.MILLISECONDS);
    		} else {
    			Bukkit.getScheduler().runTaskLaterAsynchronously(instance, () -> ClaimCommand.isOnCreate.remove(player), 10L);
    		}
    	}
    }
    
	/**
	 * Handles inventory click events in the claim GUIs.
	 * 
	 * @param event the inventory click event.
	 */
	@EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
    	Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        Inventory openInventory = event.getView().getTopInventory();

        if (inv != null && inv.equals(openInventory)) {
        	
        	InventoryHolder holder = event.getInventory().getHolder();
        	CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
        	
        	if (holder instanceof ClaimMainGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimMainGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimSettingsGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimSettingsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimMembersGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimMembersGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimChunksGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimChunksGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimBansGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimBansGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimListGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimListGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimsGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimsOwnerGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimsOwnerGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ClaimConfirmationGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleClaimConfirmationGuiClick(event, player, cPlayer);
        	} else if (holder instanceof ChunkConfirmationGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleChunkConfirmationGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionMainGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionMainGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimsGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimsGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimsOwnerGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimsOwnerGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimMembersGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimMembersGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimBansGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimBansGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimsProtectedAreasGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimsProtectedAreasGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimMainGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimMainGuiClick(event, player, cPlayer);
        	} else if (holder instanceof AdminGestionClaimChunksGui) {
                if (!inv.equals(openInventory)) {
                    event.setCancelled(true);
                    return;
                }
        		handleAdminGestionClaimChunksGuiClick(event, player, cPlayer);
        	}
        }
	}
	
	
    // ********************
    // *  Others Methods  *
    // ********************
	
	
	/**
     * Handles claim confirmation GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleClaimConfirmationGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        if(ClaimConfirmationGui.confirm_int.contains(clickedSlot)) {
        	player.closeInventory();
        	int radius = ClaimCommand.isOnCreate.get(player);
        	if(radius == 0) {
        		Bukkit.dispatchCommand(player, "claim");
        	} else {
        		Bukkit.dispatchCommand(player, "claim "+String.valueOf(radius));
        	}
        	return;
        }
        if(ClaimConfirmationGui.cancel_int.contains(clickedSlot)) {
        	player.closeInventory();
        	ClaimCommand.isOnCreate.remove(player);
        	return;
        }
        return;
    }
	
	/**
     * Handles chunk confirmation GUI click events.
     * @param event the inventory click event.
     * @param player the player clicking in the inventory.
     * @param cPlayer the CPlayer object for the player.
     */
    private void handleChunkConfirmationGuiClick(InventoryClickEvent event, Player player, CPlayer cPlayer) {
    	event.setCancelled(true);
        ItemStack clickedItem = event.getCurrentItem();
        if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
        int clickedSlot = event.getSlot();
        if(ChunkConfirmationGui.confirm_int.contains(clickedSlot)) {
        	player.closeInventory();
        	String claimName = ClaimCommand.isOnAdd.get(player);
        	Bukkit.dispatchCommand(player, "claim addchunk "+claimName);
        	return;
        }
        if(ChunkConfirmationGui.cancel_int.contains(clickedSlot)) {
        	player.closeInventory();
        	ClaimCommand.isOnAdd.remove(player);
        	return;
        }
        return;
    }
    
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
        
        switch(clickedSlot) {
        	case 13:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.list")) return;
            	cPlayer.setGuiPage(1);
            	new ClaimListGui(player,1,"owner",instance);
            	break;
        	case 20:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.bans")) return;
            	cPlayer.setGuiPage(1);
            	new ClaimBansGui(player,claim,1,instance);
            	break;
        	case 29:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.settings")) return;
            	new ClaimSettingsGui(player,claim,instance,"visitors");
            	break;
        	case 30:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.members")) return;
            	cPlayer.setGuiPage(1);
            	new ClaimMembersGui(player,claim,1,instance);
            	break;
        	case 32:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.chunks")) return;
            	cPlayer.setGuiPage(1);
            	new ClaimChunksGui(player,claim,1,instance);
            	break;
        	case 33:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) return;
            	player.closeInventory();
            	instance.getMain().goClaim(player, claim.getLocation());
            	break;
        	case 24:
            	if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim")) return;
            	player.closeInventory();
            	Bukkit.dispatchCommand(player, "unclaim "+claim.getName());
            	break;
        }
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
        String role = cPlayer.getFilter();
        if(claim == null || role == null) return;
        
        if(clickedSlot == 50) {
        	instance.getMain().applyAllSettings(claim)
        		.thenAccept(success -> {
        			if (success) {
        				instance.executeEntitySync(player, () -> {
            				player.closeInventory();
            				player.sendMessage(instance.getLanguage().getMessage("apply-all-settings-success"));
        				});
        			} else {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        	return;
        }
        
        if(clickedSlot == 49) {
        	new ClaimMainGui(player,claim,instance);
        	return;
        }
        
        if (clickedSlot == 48) {
        	if(role.equals("visitors")) {
        		role = "members";
        	} else if (role.equals("members")) {
        		role = "natural";
        	} else {
        		role = "visitors";
        	}
            new ClaimSettingsGui(player,claim,instance,role);
            return;
        }
        
        if(instance.getGuis().isAllowedSlot(clickedSlot,role)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String check = lore.get(lore.size()-1);
                if(check.equals(instance.getLanguage().getMessage("choice-setting-disabled"))) return;
                String action = instance.getGuis().getSlotPerm(clickedSlot,role);
                if(!instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+action) && !instance.getPlayerMain().checkPermPlayer(player, "scs.setting.*")) return;
                if(title.contains(instance.getLanguage().getMessage("status-enabled"))){
                	instance.getMain().updatePerm(claim, action, false, role)
                		.thenAccept(success -> {
                			if (success) {
                            	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-enabled"), instance.getLanguage().getMessage("status-disabled")));
                            	lore.remove(lore.size()-1);
                            	lore.add(instance.getLanguage().getMessage("choice-disabled"));
                            	meta.setLore(lore);
                            	clickedItem.setItemMeta(meta);
                			} else {
                				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
                			}
                		})
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    return;
                }
                instance.getMain().updatePerm(claim, action, true, role)
                	.thenAccept(success -> {
                		if (success) {
                        	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-disabled"), instance.getLanguage().getMessage("status-enabled")));
                        	lore.remove(lore.size()-1);
                        	lore.add(instance.getLanguage().getMessage("choice-enabled"));
                        	meta.setLore(lore);
                        	clickedItem.setItemMeta(meta);
                		} else {
                			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new ClaimMembersGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == 49) {
            new ClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimMembersGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String targetName = cPlayer.getMapString(clickedSlot);
        	String owner_claim = claim.getOwner();
        	String playerName = player.getName();
        	if(targetName.equals(playerName) && !owner_claim.equals("*")) return;
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")) return;
        	String message = instance.getLanguage().getMessage("remove-member-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
        	instance.getMain().removeClaimMember(claim, targetName)
        		.thenAccept(success -> {
        			if (success) {
        	            int page = cPlayer.getGuiPage();
        	        	new ClaimMembersGui(player,claim,page,instance);
        	        	instance.executeEntitySync(player, () -> player.sendMessage(message));
                        Player target = Bukkit.getPlayer(targetName);
                        if(target != null && target.isOnline()) {
                        	instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replace("%claim-name%", claim.getName()).replace("%owner%", playerName)));
                        }
        			} else {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new ClaimChunksGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == 49) {
            new ClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimChunksGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String chunk = cPlayer.getMapString(clickedSlot);
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")) return;
        	if (claim.getChunks().size() == 1) return;
        	instance.getMain().removeClaimChunk(claim, chunk)
    		.thenAccept(success -> {
    			if (success) {
    				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+chunk+"]").replace("%claim-name%", claim.getName())));
    	            int page = cPlayer.getGuiPage();
    	        	new ClaimChunksGui(player,claim,page,instance);
    			} else {
    				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage();
        	cPlayer.setGuiPage(page-1);
            new ClaimBansGui(player,claim,page-1,instance);
            return;
        }
        
        if (clickedSlot == 49) {
            new ClaimMainGui(player,claim,instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimBansGui(player,claim,page,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String targetName = cPlayer.getMapString(clickedSlot);
        	String playerName = player.getName();
        	if(targetName.equals(playerName)) return;
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban")) return;
        	String message = instance.getLanguage().getMessage("remove-ban-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
        	instance.getMain().removeClaimBan(claim, targetName)
        		.thenAccept(success -> {
        			if (success) {
        				instance.executeEntitySync(player, () -> player.sendMessage(message));
        	            int page = cPlayer.getGuiPage();
        	        	new ClaimBansGui(player,claim,page,instance);
                        Player target = Bukkit.getPlayer(targetName);
        		        if (target != null && target.isOnline()) {
        		        	instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("unbanned-claim-player").replace("%owner%", playerName).replace("%claim-name%", claim.getName())));
        		        }
        			} else {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimListGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == 49) {
    		if(!instance.getMain().checkIfClaimExists(cPlayer.getClaim())) {
    			player.sendMessage(instance.getLanguage().getMessage("the-claim-does-not-exists-anymore"));
    			player.closeInventory();
    			return;
    		}
    		new ClaimMainGui(player,cPlayer.getClaim(),instance);
    		return;
        }
        
        if (clickedSlot == 53) {
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
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimListGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
            if(event.getClick() == ClickType.LEFT) {
            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
            		Claim claim = cPlayer.getMapClaim(clickedSlot);
            		if(claim == null) return;
            		if(!claim.getPermissionForPlayer("GuiTeleport",player) && !claim.getOwner().equals(player.getName())) return;
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
            	instance.getMain().deleteClaim(claim)
            		.thenAccept(success -> {
            			if (success) {
            				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("territory-delete-success")));
                        	int page = cPlayer.getGuiPage();
                            new ClaimListGui(player,page,cPlayer.getFilter(),instance);
            			} else {
            				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        
        if (clickedSlot == 48) {
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimsGui(player,page,cPlayer.getFilter(),instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
            new ClaimsGui(player,page,cPlayer.getFilter(),instance);
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
            new ClaimsGui(player,1,filter,instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	String filter = cPlayer.getFilter();
        	cPlayer.setGuiPage(1);
        	if(filter.equals("sales")) {
        		new ClaimsOwnerGui(player,1,filter,cPlayer.getMapString(clickedSlot),instance);
        		return;
        	}
        	new ClaimsOwnerGui(player,1,"all",cPlayer.getMapString(clickedSlot),instance);
        	return;
        }
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
        
        if (clickedSlot == 48) {
        	if(cPlayer.getGuiPage() == 1) {
        		new ClaimsGui(player,1,"all",instance);
        		return;
        	}
        	int page = cPlayer.getGuiPage()-1;
        	cPlayer.setGuiPage(page);
            new ClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner(),instance);
            return;
        }
        
        if (clickedSlot == 50) {
        	int page = cPlayer.getGuiPage()+1;
        	cPlayer.setGuiPage(page);
        	new ClaimsOwnerGui(player,page,cPlayer.getFilter(),cPlayer.getOwner(),instance);
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
            new ClaimsOwnerGui(player,1,filter,cPlayer.getOwner(),instance);
            return;
        }
        
        if(clickedSlot >= 0 && clickedSlot <= 44) {
        	Claim claim = cPlayer.getMapClaim(clickedSlot);
        	if(event.getClick() == ClickType.LEFT) {
        		if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
		            if(!claim.getPermissionForPlayer("GuiTeleport",player) && !claim.getOwner().equals(player.getName())) return;
		            instance.executeEntitySync(player, () -> player.closeInventory());
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
            			String playerName = player.getName();
            			String old_owner = claim.getOwner();
            			String old_name = claim.getName();
        	            double price = claim.getPrice();
        	            double balance = instance.getVault().getPlayerBalance(playerName);
        	            // Money checking
        	            if (balance < price) {
        	            	player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money"));
        	                return;
        	            }
        		        // Check if the player can claim
        		        if (!cPlayer.canClaim()) {
        		        	player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore"));
        		            return;
        		        }
                        // Check if player can claim with all these chunks (total)
                        if (!cPlayer.canClaimTotalWithNumber(instance.getMain().getAllChunksFromAllClaims(playerName).size()+claim.getChunks().size())) {
                        	player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
                            return;
                        }
            			instance.getMain().sellChunk(player, claim)
            				.thenAccept(success -> {
            					if (success) {
            						instance.executeEntitySync(player, () -> {
    	            	                player.sendMessage(instance.getLanguage().getMessage("buy-claim-success").replace("%name%", old_name).replace("%price%", String.valueOf(price)).replace("%owner%", old_owner.equalsIgnoreCase("*") ? "protected areas" : old_owner).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
    	            	                player.closeInventory();
            						});
	            	                if(!old_owner.equalsIgnoreCase("*")) {
	                	                Player target = Bukkit.getPlayer(old_owner);
	                	                if(target != null && target.isOnline()) {
	                	                	instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("claim-was-sold").replace("%name%", old_name).replace("%buyer%", playerName).replace("%price%", String.valueOf(price)).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
	                	                }
	            	                }
            					} else {
            						instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
            					}
            				})
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });
            			return;
            		}
            		player.sendMessage(instance.getLanguage().getMessage("claim-is-not-in-sale"));
            		return;
        		}
        		player.sendMessage(instance.getLanguage().getMessage("economy-disabled"));
        		return;
        	}
        }
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
        
        if(clickedSlot == 21) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimsGui(player,1,"all",instance);
        	return;
        }
        
        if(clickedSlot == 22) {
        	cPlayer.setGuiPage(1);
        	new AdminGestionClaimsProtectedAreasGui(player,1,"all",instance);
        	return;
        }
        
        if(clickedSlot == 23) {
        	instance.getAutopurge().purgeClaims(player);
        	player.closeInventory();
        	return;
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
    			instance.getMain().deleteAllClaims(target)
	    			.thenAccept(success -> {
	    				if(success) {
	        				new AdminGestionClaimsGui(player,1,filter,instance);
	        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("player-unclaim-other-all-claim-aclaim").replace("%player%", target)));
	        				Player pTarget = Bukkit.getPlayer(target);
	        				if(pTarget != null) {
	        					instance.executeEntitySync(pTarget, () -> pTarget.sendMessage(instance.getLanguage().getMessage("player-all-claim-unclaimed-by-admin").replace("%player%", player.getName())));
	        				}
	    				} else {
	    					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        		instance.getMain().deleteClaim(claim)
				.thenAccept(success -> {
					if (success) {
						instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("player-unclaim-other-claim-aclaim").replace("%name%", claim_name).replace("%player%", owner)));
        				Player target = Bukkit.getPlayer(owner);
        				if(target != null) {
        					instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("player-claim-unclaimed-by-admin").replace("%name%", claim_name).replace("%player%", player.getName())));
        				}
        				new AdminGestionClaimsOwnerGui(player,cPlayer.getGuiPage(),cPlayer.getFilter(),owner,instance);
					} else {
						instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        String role = cPlayer.getFilter();
        if(claim == null || role == null) return;
        
        if(clickedSlot == 49) {
        	new AdminGestionClaimMainGui(player,claim,instance);
        	return;
        }
        
        if(clickedSlot == 50) {
        	instance.getMain().applyAllSettings(claim)
        		.thenAccept(success -> {
        			if (success) {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("apply-all-settings-success-aclaim").replace("%player%", claim.getOwner())));
                		String target = cPlayer.getOwner();
                    	if(target.equals("*")) {
                    		new AdminGestionClaimsProtectedAreasGui(player,1,cPlayer.getFilter(), instance);
                    		return;
                    	}
                    	new AdminGestionClaimsOwnerGui(player,1,cPlayer.getFilter(),target, instance);
        			} else {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
        			}
        		})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        	return;
        }
        
        if (clickedSlot == 48) {
        	if(role.equals("visitors")) {
        		role = "members";
        	} else if (role.equals("members")) {
        		role = "natural";
        	} else {
        		role = "visitors";
        	}
            new AdminGestionClaimGui(player,claim,instance,role);
            return;
        }
        
        if(instance.getGuis().isAllowedSlot(clickedSlot,role)) {
        	ItemMeta meta = clickedItem.getItemMeta();
            if (meta != null && meta.hasLore()) {
            	String title = meta.getDisplayName();
                List<String> lore = meta.getLore();
                String check = lore.get(lore.size()-1);
                if(check.equals(instance.getLanguage().getMessage("choice-setting-disabled"))) return;
                String action = instance.getGuis().getSlotPerm(clickedSlot,role);
                if(!instance.getPlayerMain().checkPermPlayer(player, "scs.setting."+action)) return;
                if(title.contains(instance.getLanguage().getMessage("status-enabled"))){
                	instance.getMain().updatePerm(claim, action, false, role)
                		.thenAccept(success -> {
                			if (success) {
                            	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-enabled"), instance.getLanguage().getMessage("status-disabled")));
                            	lore.remove(lore.size()-1);
                            	lore.add(instance.getLanguage().getMessage("choice-disabled"));
                            	meta.setLore(lore);
                            	clickedItem.setItemMeta(meta);
                			} else {
                				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
                			}
                		})
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
                    return;
                }
                instance.getMain().updatePerm(claim, action, true, role)
                	.thenAccept(success -> {
                		if (success) {
                        	meta.setDisplayName(title.replace(instance.getLanguage().getMessage("status-disabled"), instance.getLanguage().getMessage("status-enabled")));
                        	lore.remove(lore.size()-1);
                        	lore.add(instance.getLanguage().getMessage("choice-enabled"));
                        	meta.setLore(lore);
                        	clickedItem.setItemMeta(meta);
                		} else {
                			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        	String targetName = cPlayer.getMapString(clickedSlot);
        	String owner_claim = claim.getOwner();
        	String claimName = claim.getName();
        	if(targetName.equals(owner_claim) && !owner_claim.equals("*")) return;
    		String message = instance.getLanguage().getMessage("remove-member-success-aclaim").replace("%player%", targetName).replace("%claim-name%", claimName).replace("%owner%", owner_claim);
    		String targetMessage = owner_claim.equalsIgnoreCase("*") ?
    				instance.getLanguage().getMessage("remove-claim-protected-area-player").replace("%claim-name%", claimName) :
    				instance.getLanguage().getMessage("remove-claim-player").replace("%claim-name%", claimName).replace("%owner%", owner_claim);
    		instance.getMain().removeClaimMember(claim, targetName)
    			.thenAccept(success -> {
    				if (success) {
    					instance.executeEntitySync(player, () -> player.sendMessage(message));
                        int page = cPlayer.getGuiPage();
                    	new AdminGestionClaimMembersGui(player,claim,page,instance);
            			Player target = Bukkit.getPlayer(targetName);
            			if(target != null && target.isOnline()) {
            				instance.executeEntitySync(target, () -> target.sendMessage(targetMessage));
            			}
    				} else {
    					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        	String targetName = cPlayer.getMapString(clickedSlot);
        	String owner_claim = claim.getOwner();
        	String claimName = claim.getName();
        	if(targetName.equals(player.getName())) return;
    		String message = instance.getLanguage().getMessage("remove-ban-success-aclaim").replace("%player%", targetName).replace("%claim-name%", claim.getName()).replace("%owner%", owner_claim);
    		String targetMessage = owner_claim.equalsIgnoreCase("*") ?
    				instance.getLanguage().getMessage("unbanned-claim-protected-area-player").replace("%claim-name%", claimName) :
    				instance.getLanguage().getMessage("unbanned-claim-player").replace("%claim-name%", claimName).replace("%owner%", owner_claim);
    		instance.getMain().removeClaimBan(claim, targetName)
    			.thenAccept(success -> {
    				if (success) {
    					instance.executeEntitySync(player, () -> player.sendMessage(message));
                        int page = cPlayer.getGuiPage();
                    	new AdminGestionClaimBansGui(player,claim,page,instance);
            			Player target = Bukkit.getPlayer(targetName);
            			if(target != null && target.isOnline()) {
            				instance.executeEntitySync(target, () -> target.sendMessage(targetMessage));
            			}
    				} else {
    					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        		instance.getMain().deleteClaim(claim)
        			.thenAccept(success -> {
        				if (success) {
        					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-claim-protected-area")));
        					new AdminGestionClaimsProtectedAreasGui(player,cPlayer.getGuiPage(),cPlayer.getFilter(),instance);
        				} else {
        					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
        	new AdminGestionClaimGui(player,claim,instance,"visitors");
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
        	if(claim.getOwner().equalsIgnoreCase("*")) {
        		Bukkit.dispatchCommand(player, "parea unclaim "+claim.getName());
        	} else {
        		Bukkit.dispatchCommand(player, "scs player unclaim "+claim.getOwner()+" " +claim.getName());
        	}
        	return;
        }
        
        if(clickedSlot == 49) {
        	cPlayer.setGuiPage(1);
        	String owner = claim.getOwner();
        	if(owner.equals("*")) {
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
        	instance.getMain().removeClaimChunk(claim, chunk)
        		.thenAccept(success -> {
        			if (success) {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+chunk+"]").replace("%claim-name%", claim.getName())));
        	            int page = cPlayer.getGuiPage();
        	        	new AdminGestionClaimChunksGui(player,claim,page,instance);
        			} else {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
