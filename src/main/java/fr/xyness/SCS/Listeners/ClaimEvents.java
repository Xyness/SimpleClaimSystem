package fr.xyness.SCS.Listeners;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.projectiles.ProjectileSource;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.*;

public class ClaimEvents implements Listener {
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
		if(cPlayer.getClaimChat()) {
			event.setCancelled(true);
			String msg = ClaimLanguage.getMessage("chat-format").replaceAll("%player%",playerName).replaceAll("%message%", event.getMessage());
			player.sendMessage(msg);
			for(String p : ClaimMain.getAllMembersWithPlayerParallel(playerName)) {
				Player target = Bukkit.getPlayer(p);
				if(target != null) target.sendMessage(msg);
			}
		}
	}
	
	@EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
    	Player player = (Player) event.getWhoClicked();
        Inventory inv = event.getClickedInventory();
        Inventory openInventory = event.getView().getTopInventory();

        if (inv != null && inv.equals(openInventory)) {
        	
        	InventoryHolder holder = event.getInventory().getHolder();
        	CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        	
        	if (holder instanceof ClaimGui) {
        		event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
                int clickedSlot = event.getSlot();
                
                Chunk chunk = ClaimGui.getChunk(player);
                
                if(clickedSlot == ClaimGuis.getItemSlot("settings", "define-loc")) {
                	if (!player.hasPermission("scs.command.claim.setspawn")) {
                    	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    	return;
                	}
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
                	if (!player.hasPermission("scs.command.claim.members")) {
                    	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    	return;
                	}
                	cPlayer.setGuiPage(1);
                    ClaimMembersGui menu = new ClaimMembersGui(player,chunk,1);
                    menu.openInventory(player);
                    return;
                }
                
                if(clickedSlot == ClaimGuis.getItemSlot("settings", "define-name")) {
                	if (!player.hasPermission("scs.command.claim.setname")) {
                    	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    	return;
                	}
                	player.closeInventory();
                	player.sendMessage(ClaimLanguage.getMessage("name-change-ask"));
                	return;
                }
                
                if(clickedSlot == ClaimGuis.getItemSlot("settings", "my-claims")) {
                	if (!player.hasPermission("scs.command.claim.list")) {
                    	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    	return;
                	}
                	cPlayer.setGuiPage(1);
                	ClaimListGui.setLastChunk(player, chunk);
                    ClaimListGui menu = new ClaimListGui(player,1);
                    menu.openInventory(player);
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
                
	            String action = ClaimGuis.getCustomItemAction("claims", clickedSlot);
	            if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
	            String[] parts = action.split(":");
	            
	            if(parts[0].equalsIgnoreCase("left") && event.getClick() == ClickType.LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("right") && event.getClick() == ClickType.RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_left") && event.getClick() == ClickType.SHIFT_LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_right") && event.getClick() == ClickType.SHIFT_RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
                return;
        	}
        	
        	if (holder instanceof AdminClaimGui) {
        		event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
                int clickedSlot = event.getSlot();
                
                Chunk chunk = AdminClaimGui.getChunk(player);
                
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
                
                if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "define-name")) {
                	player.closeInventory();
                	player.sendMessage(ClaimLanguage.getMessage("name-change-ask"));
                	return;
                }
                
                if(clickedSlot == ClaimGuis.getItemSlot("admin_settings", "admin-claims")) {
                	cPlayer.setGuiPage(1);
                	AdminClaimListGui.setLastChunk(player, chunk);
                	AdminClaimListGui menu = new AdminClaimListGui(player,1);
                    menu.openInventory(player);
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
                
	            String action = ClaimGuis.getCustomItemAction("claims", clickedSlot);
	            if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
	            String[] parts = action.split(":");
	            
	            if(parts[0].equalsIgnoreCase("left") && event.getClick() == ClickType.LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("right") && event.getClick() == ClickType.RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_left") && event.getClick() == ClickType.SHIFT_LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_right") && event.getClick() == ClickType.SHIFT_RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
                return;
        	}
        	
        	if (holder instanceof ClaimMembersGui) {
        		event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
                int clickedSlot = event.getSlot();
                
                Chunk chunk = ClaimMembersGui.getChunk(player);
                
	            if (clickedSlot == ClaimGuis.getItemSlot("members", "back-page-list")) {
                	int page = cPlayer.getGuiPage();
                	if(ClaimGuis.getItemSlot("members", "back-page-list") == ClaimGuis.getItemSlot("members", "back-page-settings") && page == 1) {
    	            	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
    		                AdminClaimGui menu = new AdminClaimGui(player,chunk);
    		                menu.openInventory(player);
    		                return;
    	            	}
	                    ClaimGui menu = new ClaimGui(player,chunk);
	                    menu.openInventory(player);
	                    return;
                	}
                	cPlayer.setGuiPage(page-1);
                    ClaimMembersGui menu = new ClaimMembersGui(player,chunk,page-1);
                    menu.openInventory(player);
	                return;
	            }
	            
	            if (clickedSlot == ClaimGuis.getItemSlot("members", "back-page-settings")) {
	            	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
		                AdminClaimGui menu = new AdminClaimGui(player,chunk);
		                menu.openInventory(player);
		                return;
	            	}
	                ClaimGui menu = new ClaimGui(player,chunk);
	                menu.openInventory(player);
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
                	String owner = ClaimMembersGui.getClaimMember(player, clickedSlot);
                	if(owner.equals(player.getName())) return;
                	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
                		ClaimMain.removeAdminClaimMembers(chunk, owner);
                	} else {
                		ClaimMain.removeClaimMembers(player, chunk, owner);
                	}
                    int page = cPlayer.getGuiPage();
                	ClaimMembersGui menu = new ClaimMembersGui(player,chunk,page);
                	menu.openInventory(player);
                    return;
                }
                
	            String action = ClaimGuis.getCustomItemAction("claims", clickedSlot);
	            if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
	            String[] parts = action.split(":");
	            
	            if(parts[0].equalsIgnoreCase("left") && event.getClick() == ClickType.LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("right") && event.getClick() == ClickType.RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_left") && event.getClick() == ClickType.SHIFT_LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_right") && event.getClick() == ClickType.SHIFT_RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
                return;
        	}
        	
        	if (holder instanceof ClaimListGui) {
        		event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
                int clickedSlot = event.getSlot();
                
	            if (clickedSlot == ClaimGuis.getItemSlot("list", "back-page-list")) {
	            	int page = cPlayer.getGuiPage()-1;
	            	if(ClaimGuis.getItemSlot("list", "back-page-list") == ClaimGuis.getItemSlot("list", "back-page-settings") && page == 0) {
	            		if(!ClaimMain.checkIfClaimExists(ClaimListGui.getLastChunk(player))) {
	            			player.sendMessage(ClaimLanguage.getMessage("the-claim-does-not-exists-anymore"));
	            			return;
	            		}
	            		ClaimGui menu = new ClaimGui(player,ClaimListGui.getLastChunk(player));
	            		menu.openInventory(player);
	            		return;
	            	}
	            	cPlayer.setGuiPage(page);
                    ClaimListGui menu = new ClaimListGui(player,page);
                    menu.openInventory(player);
	                return;
	            }
                
	            if (clickedSlot == ClaimGuis.getItemSlot("list", "next-page-list")) {
	            	int page = cPlayer.getGuiPage()+1;
	            	cPlayer.setGuiPage(page);
                    ClaimListGui menu = new ClaimListGui(player,page);
                    menu.openInventory(player);
	                return;
	            }
                
	            if(clickedSlot >= ClaimGuis.getGuiMinSlot("list") && clickedSlot <= ClaimGuis.getGuiMaxSlot("list")) {
		            if(event.getClick() == ClickType.LEFT) {
		            	if(player.hasPermission("scs.command.claim.tp")) {
			            	player.closeInventory();
				        	ClaimMain.goClaim(player, ClaimListGui.getClaimLoc(player, clickedSlot));
				        	return;
		            	}
		            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
		            	return;
		            }
		            if(event.getClick() == ClickType.RIGHT) {
	                    ClaimGui menu = new ClaimGui(player,ClaimListGui.getClaimChunk(player, clickedSlot));
	                    menu.openInventory(player);
			        	return;
		            }
		            if(event.getClick() == ClickType.SHIFT_LEFT) {
		            	Chunk chunk = ClaimListGui.getClaimChunk(player, clickedSlot);
			        	if(ClaimMain.deleteClaim(player, chunk)) {
			        		for(Entity e : chunk.getEntities()) {
			    				if(!(e instanceof Player)) continue;
			    				Player p = (Player) e;
			    				ClaimEventsEnterLeave.disableBossBar(p);
			    			}
			    			player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
			            	int page = cPlayer.getGuiPage();
		                    ClaimListGui menu = new ClaimListGui(player,page);
		                    menu.openInventory(player);
			        	}
			        	return;
		            }
		            if(event.getClick() == ClickType.SHIFT_RIGHT) {
	            		if(ClaimSettings.getBooleanSetting("economy")) {
	            			Chunk chunk = ClaimListGui.getClaimChunk(player, clickedSlot);
	            			if(ClaimMain.claimIsInSale(chunk)) {
	                			if(ClaimMain.delChunkSale(player, chunk)) {
	                				player.sendMessage(ClaimLanguage.getMessage("claim-in-sale-cancel").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)));
	    			            	int page = cPlayer.getGuiPage();
	    		                    ClaimListGui menu = new ClaimListGui(player,page);
	    		                    menu.openInventory(player);
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
	            
	            String action = ClaimGuis.getCustomItemAction("claims", clickedSlot);
	            if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
	            String[] parts = action.split(":");
	            
	            if(parts[0].equalsIgnoreCase("left") && event.getClick() == ClickType.LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("right") && event.getClick() == ClickType.RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_left") && event.getClick() == ClickType.SHIFT_LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_right") && event.getClick() == ClickType.SHIFT_RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
                return;
        	}
        	
        	if (holder instanceof AdminClaimListGui) {
        		event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
                int clickedSlot = event.getSlot();
                
	            if (clickedSlot == ClaimGuis.getItemSlot("admin_list", "back-page-list")) {
	            	int page = cPlayer.getGuiPage()-1;
	            	if(ClaimGuis.getItemSlot("admin_list", "back-page-list") == ClaimGuis.getItemSlot("admin_list", "back-page-settings") && page == 0) {
	            		if(!ClaimMain.checkIfClaimExists(AdminClaimListGui.getLastChunk(player))) {
	            			player.sendMessage(ClaimLanguage.getMessage("the-claim-does-not-exists-anymore"));
	            			return;
	            		}
	            		AdminClaimGui menu = new AdminClaimGui(player,AdminClaimListGui.getLastChunk(player));
	            		menu.openInventory(player);
	            		return;
	            	}
	            	cPlayer.setGuiPage(page);
                    AdminClaimListGui menu = new AdminClaimListGui(player,page);
                    menu.openInventory(player);
	                return;
	            }
                
	            if (clickedSlot == ClaimGuis.getItemSlot("admin_list", "next-page-list")) {
	            	int page = cPlayer.getGuiPage()+1;
	            	cPlayer.setGuiPage(page);
                    AdminClaimListGui menu = new AdminClaimListGui(player,page);
                    menu.openInventory(player);
	                return;
	            }
                
	            if(clickedSlot >= ClaimGuis.getGuiMinSlot("admin_list") && clickedSlot <= ClaimGuis.getGuiMaxSlot("admin_list")) {
		            if(event.getClick() == ClickType.LEFT) {
		            	player.closeInventory();
			        	ClaimMain.goClaim(player, AdminClaimListGui.getClaimLoc(player, clickedSlot));
			        	return;
		            }
		            if(event.getClick() == ClickType.RIGHT) {
	                    AdminClaimGui menu = new AdminClaimGui(player,AdminClaimListGui.getClaimChunk(player, clickedSlot));
	                    menu.openInventory(player);
			        	return;
		            }
		            if(event.getClick() == ClickType.SHIFT_LEFT) {
		            	Chunk chunk = AdminClaimListGui.getClaimChunk(player, clickedSlot);
			        	if(ClaimMain.deleteClaim(player, chunk)) {
			        		for(Entity e : chunk.getEntities()) {
			    				if(!(e instanceof Player)) continue;
			    				Player p = (Player) e;
			    				ClaimEventsEnterLeave.disableBossBar(p);
			    			}
			    			player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
			            	int page = cPlayer.getGuiPage();
		                    AdminClaimListGui menu = new AdminClaimListGui(player,page);
		                    menu.openInventory(player);
			        	}
			        	return;
		            }
	            }
	            
	            String action = ClaimGuis.getCustomItemAction("claims", clickedSlot);
	            if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
	            String[] parts = action.split(":");
	            
	            if(parts[0].equalsIgnoreCase("left") && event.getClick() == ClickType.LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("right") && event.getClick() == ClickType.RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_left") && event.getClick() == ClickType.SHIFT_LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_right") && event.getClick() == ClickType.SHIFT_RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
                return;
        	}
        	
        	if (holder instanceof ClaimsGui) {
        		event.setCancelled(true);
                ItemStack clickedItem = event.getCurrentItem();
                if(clickedItem != null) { player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1f); } else { return; }
                int clickedSlot = event.getSlot();
                
                if (clickedSlot == ClaimGuis.getItemSlot("claims", "back-page-list")) {
	            	int page = cPlayer.getGuiPage()-1;
	            	cPlayer.setGuiPage(page);
                    ClaimsGui menu = new ClaimsGui(player,page,ClaimsGui.getPlayerFilter(player));
                    menu.openInventory(player);
	                return;
	            }
                
	            if (clickedSlot == ClaimGuis.getItemSlot("claims", "next-page-list")) {
	            	int page = cPlayer.getGuiPage()+1;
	            	cPlayer.setGuiPage(page);
                    ClaimsGui menu = new ClaimsGui(player,page,ClaimsGui.getPlayerFilter(player));
                    menu.openInventory(player);
	                return;
	            }
	            
	            if (clickedSlot == ClaimGuis.getItemSlot("claims", "filter")) {
	            	cPlayer.setGuiPage(1);
                	String filter = ClaimsGui.getPlayerFilter(player);
                	if(filter.equals("all")) {
                		filter = "sales";
                	} else {
                		filter = "all";
                	}
                    ClaimsGui menu = new ClaimsGui(player,1,filter);
                    menu.openInventory(player);
	                return;
	            }
                
	            if(clickedSlot >= ClaimGuis.getGuiMinSlot("claims") && clickedSlot <= ClaimGuis.getGuiMaxSlot("claims")) {
	            	Chunk chunk = ClaimsGui.getClaimChunk(player, clickedSlot);
	            	if(event.getClick() == ClickType.LEFT) {
	            		if(player.hasPermission("scs.command.claim.tp")) {
				            if(!ClaimMain.canPermCheck(chunk, "Visitors") && !ClaimMain.getOwnerInClaim(chunk).equals(player.getName())) {
				            	player.sendMessage(ClaimLanguage.getMessage("error-claim-visitors-deny"));
				            	return;
				            }
			            	player.closeInventory();
				        	ClaimMain.goClaim(player, ClaimsGui.getClaimLoc(player, clickedSlot));
				        	return;
	            		}
	            		player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
	            		return;
	            	}
	            	if(event.getClick() == ClickType.SHIFT_LEFT) {
	            		if(ClaimSettings.getBooleanSetting("economy")) {
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
	            
	            String action = ClaimGuis.getCustomItemAction("claims", clickedSlot);
	            if(action == null || action.isEmpty() || action.equalsIgnoreCase("none")) return;
	            String[] parts = action.split(":");
	            
	            if(parts[0].equalsIgnoreCase("left") && event.getClick() == ClickType.LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("right") && event.getClick() == ClickType.RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_left") && event.getClick() == ClickType.SHIFT_LEFT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
	            if(parts[0].equalsIgnoreCase("shift_right") && event.getClick() == ClickType.SHIFT_RIGHT) {
	            	if(parts.length<2) return;
		            if(parts[1].equalsIgnoreCase("close_inventory")) {
		            	player.closeInventory();
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("cmd")) {
		            	if(parts.length<3) return;
		            	if(parts[2].equalsIgnoreCase("console")) {
		            		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), parts[3]);
		            		return;
		            	}
		            	if(parts[2].equalsIgnoreCase("player")) {
		            		Bukkit.dispatchCommand(player, parts[3]);
		            		return;
		            	}
		            	return;
		            }
		            if(parts[1].equalsIgnoreCase("msg")) {
		            	if(parts.length<3) return;
		            	player.sendMessage(parts[2]);
		            	return;
		            }
	            	return;
	            }
                return;
        	}

        }
	}
	
    @EventHandler
    public void onPlayerHit(EntityDamageByEntityEvent event) {
    	if(event.isCancelled()) return;
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        Chunk chunk = player.getLocation().getChunk();
        if(ClaimMain.checkIfClaimExists(chunk)) {
        	if(ClaimMain.canPermCheck(chunk, "Pvp")) {
        		return;
        	}
            if (event.getDamager() instanceof Player) {
            	ClaimMain.sendActionBar((Player) event.getDamager(),ClaimLanguage.getMessage("pvp"));
            	event.setCancelled(true);
            } else if (event.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) event.getDamager();
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Player) {
                	ClaimMain.sendActionBar((Player) shooter,ClaimLanguage.getMessage("pvp"));
                    event.setCancelled(true);
                }
            }
        }
        return;
    }
    
    @EventHandler
    public void onCreatureSpawn(CreatureSpawnEvent event) {
		Chunk chunk = event.getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			Entity entity = event.getEntity();
			if(!(entity instanceof Monster)) return;
			if(!ClaimMain.canPermCheck(chunk, "Monsters")) {
				event.setCancelled(true);
				return;
			}
		}
    }
	
	@EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (ClaimMain.checkIfClaimExists(block.getLocation().getChunk()) && !ClaimMain.canPermCheck(block.getLocation().getChunk(), "Explosions")) {
                blockIterator.remove();
            }
        }
    }
	
	@EventHandler
	public void onPlayerBreak(BlockBreakEvent event){
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(player.hasPermission("scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Destroy")) {
				event.setCancelled(true);
				ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("destroy"));
				return;
			}
		}
	}
	
	@EventHandler
	public void onPlayerPlace(BlockPlaceEvent event){
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(player.hasPermission("scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("build"));
				return;
			}
		}
	}
	
	@EventHandler
	public void onHangingPlace(HangingPlaceEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(player.hasPermission("scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("build"));
				return;
			}
		}
	}
	
	@EventHandler
    public void onBucketUse(PlayerBucketEmptyEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(player.hasPermission("scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("build"));
				return;
			}
		}
    }
	
	@EventHandler
	public void onEntityPlace(EntityPlaceEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(player.hasPermission("scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("build"));
				return;
			}
		}
	}
	
	@EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if(player.hasPermission("scs.bypass")) return;
		Block block = event.getClickedBlock();
		Chunk chunk;
		if(block == null) {
			chunk = player.getLocation().getChunk();
		} else {
			chunk = block.getLocation().getChunk();
		}
		if(ClaimMain.checkIfClaimExists(chunk)) {
	        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && !ClaimMain.checkMembre(chunk, player)) {
	            Material mat = event.getClickedBlock().getType();
	            if (mat.name().contains("BUTTON") && !ClaimMain.canPermCheck(chunk, "Buttons")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("buttons"));
	                return;
	            }
	            if (mat.name().contains("TRAPDOOR") && !ClaimMain.canPermCheck(chunk, "Trapdoors")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("trapdoors"));
	                return;
	            }
	            if (mat.name().contains("DOOR") && !ClaimMain.canPermCheck(chunk, "Doors")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("doors"));
	                return;
	            }
	            if (mat.name().contains("FENCE_GATE") && !ClaimMain.canPermCheck(chunk, "Fencegates")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("fencegates"));
	                return;
	            }
	            if (mat.equals(Material.LEVER) && !ClaimMain.canPermCheck(chunk, "Levers")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("levers"));
	                return;
	            }
	            if (mat.equals(Material.REPEATER) && !ClaimMain.canPermCheck(chunk, "RepeatersComparators")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("repeaters"));
	                return;
	            }
	            if (mat.equals(Material.COMPARATOR) && !ClaimMain.canPermCheck(chunk, "RepeatersComparators")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("comparators"));
	                return;
	            }
	            if (mat.equals(Material.BELL) && !ClaimMain.canPermCheck(chunk, "Bells")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("bells"));
	                return;
	            }
	            if(!ClaimMain.canPermCheck(chunk, "Containers")) {
	            	Material item = block.getType();
	            	if(ClaimSettings.isRestrictedContainer(item)) {
                        event.setCancelled(true);
                        ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("containers"));
                        return;
	            	}
	            }
	            if(!ClaimMain.canPermCheck(chunk, "Items")) {
	                Material item = event.getMaterial();
	                if(ClaimSettings.isRestrictedItem(item)) {
                        event.setCancelled(true);
                        ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("items"));
                        return;
	                }
	            }
	            return;
	        }
	        if (event.getAction() == Action.PHYSICAL && !ClaimMain.checkMembre(chunk, player)) {
	        	if(block != null && block.getType().name().contains("PRESSURE_PLATE") && !ClaimMain.canPermCheck(chunk, "Plates")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("plates"));
	                return;
	        	}
	        	if (block.getType() == Material.TRIPWIRE && !ClaimMain.canPermCheck(chunk, "Tripwires")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("tripwires"));
	                return;
	            }
	        }
	        if(!ClaimMain.canPermCheck(chunk, "Items")) {
                Material item = event.getMaterial();
                if(ClaimSettings.isRestrictedItem(item)) {
                    event.setCancelled(true);
                    ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("items"));
                    return;
                }
	        }
	        return;
		}
    }
	
	@EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        if (event.getRightClicked() instanceof ArmorStand) {
            Chunk chunk = event.getRightClicked().getLocation().getChunk();
            if(ClaimMain.checkIfClaimExists(chunk)) {
            	Player player = event.getPlayer();
            	if(player.hasPermission("scs.bypass")) return;
            	if(ClaimMain.checkMembre(chunk, player)) return;
            	if(!ClaimMain.canPermCheck(chunk, "Armorstands")) {
            		event.setCancelled(true);
            		ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("armorstands"));
            	}
            }
            return;
        }
    }
	
	@EventHandler
    public void onPlayerInteractEntity2(PlayerInteractEntityEvent event) {
        if (event.getRightClicked().getType() == EntityType.ITEM_FRAME || event.getRightClicked().getType() == EntityType.GLOW_ITEM_FRAME) {
            Chunk chunk = event.getRightClicked().getLocation().getChunk();
            if(ClaimMain.checkIfClaimExists(chunk)) {
            	Player player = event.getPlayer();
            	if(player.hasPermission("scs.bypass")) return;
            	if(ClaimMain.checkMembre(chunk, player)) return;
            	if(!ClaimMain.canPermCheck(chunk, "Itemframes")) {
            		event.setCancelled(true);
            		ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("itemframes"));
            	}
            }
            return;
        }
    }
	
	@EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.PAINTING) {
            if (event.getRemover() instanceof Player) {
                Chunk chunk = event.getEntity().getLocation().getChunk();
                if(ClaimMain.checkIfClaimExists(chunk)) {
                	Player player = (Player) event.getRemover();
                	if(player.hasPermission("scs.bypass")) return;
                	if(ClaimMain.checkMembre(chunk, player)) return;
                	if(!ClaimMain.canPermCheck(chunk, "Paintings")) {
                		event.setCancelled(true);
                		ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("paintings"));
                	}
                }
            }
        }
        if (event.getEntity().getType() == EntityType.ITEM_FRAME || event.getEntity().getType() == EntityType.GLOW_ITEM_FRAME) {
            if (event.getRemover() instanceof Player) {
                Chunk chunk = event.getEntity().getLocation().getChunk();
                if(ClaimMain.checkIfClaimExists(chunk)) {
                	Player player = (Player) event.getRemover();
                	if(player.hasPermission("scs.bypass")) return;
                	if(ClaimMain.checkMembre(chunk, player)) return;
                	if(!ClaimMain.canPermCheck(chunk, "Itemframes")) {
                		event.setCancelled(true);
                		ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("itemframes"));
                	}
                }
            }
        }
    }
	
	
	// *****************************
	// *   PROTECTION DES CLAIMS   *
	// *****************************
	
	
	@EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
    	Block block = event.getBlock();
    	Block toBlock = event.getToBlock();
    	Chunk chunk = toBlock.getLocation().getChunk();
    	if(block.getLocation().getChunk().equals(chunk)) return;
    	if(ClaimMain.checkIfClaimExists(chunk)) {
    		if(ClaimMain.getOwnerInClaim(chunk).equals(ClaimMain.getOwnerInClaim(block.getLocation().getChunk()))) return;
            if (block.isLiquid() && toBlock.isEmpty() && !ClaimMain.canPermCheck(chunk, "Liquids")) {
                event.setCancelled(true);
            }
    	}
    }
    
	@EventHandler
    public void onDispense(BlockDispenseEvent event) {
    	Block block = event.getBlock();
    	Chunk targetChunk = block.getRelative(((Directional) event.getBlock().getBlockData()).getFacing()).getLocation().getChunk();
    	if(block.getLocation().getChunk().equals(targetChunk)) return;
    	if(ClaimMain.checkIfClaimExists(targetChunk)) {
    		if(ClaimMain.getOwnerInClaim(block.getLocation().getChunk()).equals(ClaimMain.getOwnerInClaim(targetChunk))) return;
    		if(!ClaimMain.canPermCheck(targetChunk, "Redstone")) {
    			event.setCancelled(true);
    		}
    	}
    }
    
	@EventHandler
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        List<Block> affectedBlocks = new ArrayList<>(event.getBlocks());
        BlockFace direction = event.getDirection();
        if(!affectedBlocks.isEmpty()) {
            affectedBlocks.add(piston.getRelative(direction));
        }
        if (!canPistonMoveBlock(affectedBlocks, direction, piston.getLocation().getChunk(),false)) {
            event.setCancelled(true);
        }
    }

	@EventHandler
    public void onPistonRetract(BlockPistonRetractEvent event) {
        Block piston = event.getBlock();
        List<Block> affectedBlocks = new ArrayList<>(event.getBlocks());
        BlockFace direction = event.getDirection();
        if (event.isSticky() && !affectedBlocks.isEmpty()) {
            affectedBlocks.add(piston.getRelative(direction));
        }
        if (!canPistonMoveBlock(affectedBlocks, direction, piston.getLocation().getChunk(),true)) {
            event.setCancelled(true);
        }
    }

    // Fonction qui vérifie si les blocs qui sont affectés par le piston ne sont pas dans un chunk claim
    private boolean canPistonMoveBlock(List<Block> blocks, BlockFace direction, Chunk pistonChunk, boolean retractOrNot) {
    	if(retractOrNot) {
	        for (Block block : blocks) {
	        	Chunk chunk = block.getLocation().getChunk();
	            if (!chunk.equals(pistonChunk)) {
	                if (ClaimMain.checkIfClaimExists(chunk)) {
	                	if(ClaimMain.getOwnerInClaim(pistonChunk).equals(ClaimMain.getOwnerInClaim(chunk))) return true;
	                	if(!ClaimMain.canPermCheck(chunk, "Redstone")) {
	                		return false;
	                	}
	                }
	            }
	        }
	        return true;
    	}
        for (Block block : blocks) {
            Chunk chunk = block.getRelative(direction).getLocation().getChunk();
            if (!chunk.equals(pistonChunk)) {
                if (ClaimMain.checkIfClaimExists(chunk)) {
                	if(ClaimMain.getOwnerInClaim(pistonChunk).equals(ClaimMain.getOwnerInClaim(chunk))) return true;
                	if(!ClaimMain.canPermCheck(chunk, "Redstone")) {
                		return false;
                	}
                }
            }
        }
        return true;
    }
    
    @EventHandler
    public void onFrostWalkerUse(EntityBlockFormEvent event) {
    	Chunk chunk = event.getBlock().getLocation().getChunk();
    	if(!ClaimMain.checkIfClaimExists(chunk)) return;
    	if(ClaimMain.canPermCheck(chunk, "Frostwalker")) return;
        if (event.getNewState().getType() == Material.FROSTED_ICE) {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if(player.hasPermission("scs.bypass")) return;
                ItemStack boots = player.getInventory().getBoots();
                if (boots != null && boots.containsEnchantment(Enchantment.FROST_WALKER)) {
                	if(ClaimMain.checkMembre(chunk, player)) return;
                    event.setCancelled(true);
                }
            }
        }
    }
    
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE) {
            Chunk chunk = event.getBlock().getLocation().getChunk();
            if(!ClaimMain.checkIfClaimExists(chunk)) return;
            if(ClaimMain.canPermCheck(chunk, "Firespread")) return;
            event.setCancelled(true);
        }
    }
    
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        if(!ClaimMain.checkIfClaimExists(chunk)) return;
        if(ClaimMain.canPermCheck(chunk, "Firespread")) return;
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        if(!ClaimMain.checkIfClaimExists(chunk)) return;
        if(ClaimMain.canPermCheck(chunk, "Firespread")) return;
        event.setCancelled(true);
    }
    
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();

        if(!ClaimMain.checkIfClaimExists(chunk)) return;

        if (!(entity instanceof Player)) {
            Entity damager = event.getDamager();

            if (damager instanceof Player) {
                processDamageByPlayer((Player) damager, chunk, event);
            } else if (damager instanceof Projectile) {
                Projectile projectile = (Projectile) damager;
                ProjectileSource shooter = projectile.getShooter();
                if (shooter instanceof Player) {
                    processDamageByPlayer((Player) shooter, chunk, event);
                }
            }
        }
    }

    private void processDamageByPlayer(Player player, Chunk chunk, EntityDamageByEntityEvent event) {
        if(player.hasPermission("scs.bypass")) return;
        if(ClaimMain.checkMembre(chunk, player)) return;
        if(!ClaimMain.canPermCheck(chunk, "Damages")) {
            event.setCancelled(true);
            ClaimMain.sendActionBar(player, ClaimLanguage.getMessage("damages"));
        }
    }
}
