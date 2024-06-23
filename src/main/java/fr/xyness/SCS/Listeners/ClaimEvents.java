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
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
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
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.EntityBlockFormEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityPlaceEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.event.weather.LightningStrikeEvent;
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

/**
 * Event listener for claim-related events.
 */
public class ClaimEvents implements Listener {
	
	// ******************
	// *  EventHandler  *
	// ******************
	
	/**
	 * Handles player chat events for claim chat.
	 * @param event the player chat event.
	 */
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
	
	/**
	 * Handles player damage events to disable claim fly on damage.
	 * @param event the player damage event.
	 */
	@EventHandler
	public void onPlayerDamage(EntityDamageEvent event) {
		if(event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        	if(cPlayer.getClaimFly()) {
        		CPlayerMain.removePlayerFly(player);
        		ClaimMain.sendMessage(player, ClaimLanguage.getMessage("claim-fly-disabled-on-damage"), ClaimSettings.getSetting("protection-message"));
        	}
		}
	}
	
	/**
	 * Handles PvP settings in claims.
	 * @param event the player hit event.
	 */
	@EventHandler
	public void onPlayerHit(EntityDamageByEntityEvent event) {
	    if(event.isCancelled()) return;
	    if (!(event.getEntity() instanceof Player)) return;

	    Player player = (Player) event.getEntity();
	    Chunk chunk = player.getLocation().getChunk();
	    
	    if(ClaimMain.checkIfClaimExists(chunk)) {
	        if (event.getDamager() instanceof Player) {
	            Player damager = (Player) event.getDamager();
	            if(damager.hasPermission("scs.bypass")) return;
	            if(!ClaimMain.canPermCheck(chunk, "Pvp")) {
	                ClaimMain.sendMessage(damager, ClaimLanguage.getMessage("pvp"), ClaimSettings.getSetting("protection-message"));
	                event.setCancelled(true);
	            }
	        } else if (event.getDamager() instanceof Projectile) {
	            Projectile projectile = (Projectile) event.getDamager();
	            ProjectileSource shooter = projectile.getShooter();
	            if (shooter instanceof Player) {
	                Player damager = (Player) shooter;
	                if(damager.hasPermission("scs.bypass")) return;
	                if(!ClaimMain.canPermCheck(chunk, "Pvp")) {
	                    ClaimMain.sendMessage(damager, ClaimLanguage.getMessage("pvp"), ClaimSettings.getSetting("protection-message"));
	                    event.setCancelled(true);
	                }
	            }
	        }
	    }
	}

    /**
     * Handles creature spawn events to prevent monster spawning in claims.
     * @param event the creature spawn event.
     */
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
	
    /**
     * Handles entity explosion events to prevent explosions in claims.
     * @param event the entity explosion event.
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (ClaimMain.checkIfClaimExists(block.getLocation().getChunk()) &&
                !ClaimMain.canPermCheck(block.getLocation().getChunk(), "Explosions")) {
                blockIterator.remove();
            }
        }
    }
    
    /**
     * Handles projectile hit events to prevent explosions in claims.
     * @param event the projectile hit event.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() == EntityType.WITHER_SKULL) {
            if (event.getHitBlock() != null) {
            	Block block = event.getHitBlock();
                if (ClaimMain.checkIfClaimExists(block.getLocation().getChunk()) && !ClaimMain.canPermCheck(block.getLocation().getChunk(), "Explosions")) {
                	event.setCancelled(true);
                }
            }
            if (event.getHitEntity() != null) {
        		Chunk chunk = event.getHitEntity().getLocation().getChunk();
        		if(ClaimMain.checkIfClaimExists(chunk) && !ClaimMain.canPermCheck(chunk, "Explosions")) {
        			event.setCancelled(true);
        		}
            }
        }
    }
    
    /**
     * Handles block explosion events to prevent explosions in claims.
     * @param event the block explosion event.
     */
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent event) {
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            if (ClaimMain.checkIfClaimExists(block.getLocation().getChunk()) &&
                !ClaimMain.canPermCheck(block.getLocation().getChunk(), "Explosions")) {
                blockIterator.remove();
            }
        }
    }
	
    /**
     * Handles entity change block events to prevent wither or wither skulls from changing blocks in claims.
     * @param event the entity change block event.
     */
    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL) {
            Block block = event.getBlock();
            if (ClaimMain.checkIfClaimExists(block.getLocation().getChunk()) && !ClaimMain.canPermCheck(block.getLocation().getChunk(), "Explosions")) {
            	event.setCancelled(true);
            }
        }
    }
	
    /**
     * Handles block break events to prevent destruction in claims.
     * @param event the block break event.
     */
    @EventHandler(priority = EventPriority.LOW)
	public void onPlayerBreak(BlockBreakEvent event){
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Destroy")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
	}
	
    /**
     * Handles vehicle damage events to prevent destruction of vehicles in claims.
     * @param event the vehicle damage event.
     */
	@EventHandler
	public void onVehicleDamage(VehicleDamageEvent event){
		Entity damager = event.getAttacker();
		Chunk chunk = event.getVehicle().getLocation().getChunk();
		if(!ClaimMain.checkIfClaimExists(chunk)) return;
		if(damager instanceof Player) {
			Player player = (Player) damager;
			if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
			if(ClaimMain.checkMembre(chunk, player)) return;
			if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
		if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
			event.setCancelled(true);
			return;
		}
	}
	
    /**
     * Handles lightning strike events to prevent weather-related damage in claims.
     * @param event the lightning strike event.
     */
    @EventHandler
    public void onLightningStrike(LightningStrikeEvent event) {
        Location strikeLocation = event.getLightning().getLocation();
        Chunk chunk = strikeLocation.getChunk();
        if (ClaimMain.checkIfClaimExists(chunk) && !ClaimMain.canPermCheck(chunk, "Weather")) {
            event.setCancelled(true);
        }
    }
	
    /**
     * Handles block place events to prevent building in claims.
     * @param event the block place event.
     */
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerPlace(BlockPlaceEvent event){
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
		Block block = event.getBlock();
		Chunk chunk = block.getLocation().getChunk();
		
		if(block.getType().toString().contains("BED")) {
	        Bed bed = (Bed) block.getBlockData();
	        BlockFace facing = bed.getFacing();
	        Block adjacentBlock = block.getRelative(facing);
	        Chunk adjacentChunk = adjacentBlock.getChunk();
	
	        if (!chunk.equals(adjacentChunk)) {
	            if (ClaimMain.checkIfClaimExists(adjacentChunk) &&
	                !ClaimMain.getOwnerInClaim(chunk).equals(ClaimMain.getOwnerInClaim(adjacentChunk))) {
	                if (!ClaimMain.canPermCheck(adjacentChunk, "Build")) {
	                    event.setCancelled(true);
	                    ClaimMain.sendMessage(player,ClaimLanguage.getMessage("build"), ClaimSettings.getSetting("protection-message"));
	                    return;
	                }
	            }
	        }
		}
		
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("build"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
	}
	
    /**
     * Handles hanging place events to prevent hanging items in claims.
     * @param event the hanging place event.
     */
	@EventHandler
	public void onHangingPlace(HangingPlaceEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("build"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
	}
	
    /**
     * Handles hanging break events to prevent hanging items from being broken by physics in claims.
     * @param event the hanging break event.
     */
	@EventHandler
	public void onHangingBreak(HangingBreakEvent event) {
		Chunk chunk = event.getEntity().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS && !ClaimMain.canPermCheck(chunk, "Destroy")) {
				event.setCancelled(true);
			}
		}
	}
	
    /**
     * Handles hanging break by entity events to prevent hanging items from being broken by players in claims.
     * @param event the hanging break by entity event.
     */
	@EventHandler
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
		if(event.isCancelled()) return;
        if (event.getEntity().getType() == EntityType.PAINTING) {
        	Chunk chunk = event.getEntity().getLocation().getChunk();
            if (event.getRemover() instanceof Player) {
                if(ClaimMain.checkIfClaimExists(chunk)) {
                	Player player = (Player) event.getRemover();
                	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
                	if(ClaimMain.checkMembre(chunk, player)) return;
                	if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
                		event.setCancelled(true);
                		ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
                		return;
                	}
                }
                return;
            }
           	if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
        		event.setCancelled(true);
        		return;
        	}
        }
        if (event.getEntity().getType() == EntityType.ITEM_FRAME || event.getEntity().getType() == EntityType.GLOW_ITEM_FRAME) {
        	Chunk chunk = event.getEntity().getLocation().getChunk();
            if (event.getRemover() instanceof Player) {
                if(ClaimMain.checkIfClaimExists(chunk)) {
                	Player player = (Player) event.getRemover();
                	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
                	if(ClaimMain.checkMembre(chunk, player)) return;
                	if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
                		event.setCancelled(true);
                		ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
                		return;
                	}
                }
                return;
            }
           	if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
        		event.setCancelled(true);
        		return;
        	}
        }
    }
	
    /**
     * Handles bucket empty events to prevent liquid placement in claims.
     * @param event the bucket empty event.
     */
	@EventHandler
    public void onBucketUse(PlayerBucketEmptyEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("build"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
    }
	
    /**
     * Handles bucket fill events to prevent liquid removal in claims.
     * @param event the bucket fill event.
     */
	@EventHandler
    public void onBucketUse(PlayerBucketFillEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Destroy")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
    }
	
    /**
     * Handles entity place events to prevent entity placement in claims.
     * @param event the entity place event.
     */
	@EventHandler
	public void onEntityPlace(EntityPlaceEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(ClaimMain.checkIfClaimExists(chunk)) {
			if(!ClaimMain.checkMembre(chunk, player) && !ClaimMain.canPermCheck(chunk, "Build")) {
				event.setCancelled(true);
				ClaimMain.sendMessage(player,ClaimLanguage.getMessage("build"), ClaimSettings.getSetting("protection-message"));
				return;
			}
		}
	}
	
    /**
     * Handles player interact events to prevent interactions with blocks and items in claims.
     * @param event the player interact event.
     */
	@EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
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
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("buttons"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("TRAPDOOR") && !ClaimMain.canPermCheck(chunk, "Trapdoors")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("trapdoors"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("DOOR") && !ClaimMain.canPermCheck(chunk, "Doors")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("doors"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("FENCE_GATE") && !ClaimMain.canPermCheck(chunk, "Fencegates")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("fencegates"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.LEVER) && !ClaimMain.canPermCheck(chunk, "Levers")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("levers"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.REPEATER) && !ClaimMain.canPermCheck(chunk, "RepeatersComparators")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("repeaters"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.COMPARATOR) && !ClaimMain.canPermCheck(chunk, "RepeatersComparators")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("comparators"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.BELL) && !ClaimMain.canPermCheck(chunk, "Bells")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("bells"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	            if(!ClaimMain.canPermCheck(chunk, "InteractBlocks")) {
	            	Material item = block.getType();
	            	if(ClaimSettings.isRestrictedContainer(item)) {
                        event.setCancelled(true);
                        ClaimMain.sendMessage(player,ClaimLanguage.getMessage("interactblocks"), ClaimSettings.getSetting("protection-message"));
                        return;
	            	}
	            }
	            if(!ClaimMain.canPermCheck(chunk, "Items")) {
	                Material item = event.getMaterial();
	                if(ClaimSettings.isRestrictedItem(item)) {
                        event.setCancelled(true);
                        ClaimMain.sendMessage(player,ClaimLanguage.getMessage("items"), ClaimSettings.getSetting("protection-message"));
                        return;
	                }
	            }
	            return;
	        }
	        if (event.getAction() == Action.PHYSICAL && !ClaimMain.checkMembre(chunk, player)) {
	        	if(block != null && block.getType().name().contains("PRESSURE_PLATE") && !ClaimMain.canPermCheck(chunk, "Plates")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("plates"), ClaimSettings.getSetting("protection-message"));
	                return;
	        	}
	        	if (block.getType() == Material.TRIPWIRE && !ClaimMain.canPermCheck(chunk, "Tripwires")) {
	            	event.setCancelled(true);
	            	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("tripwires"), ClaimSettings.getSetting("protection-message"));
	                return;
	            }
	        }
	        if(!ClaimMain.canPermCheck(chunk, "Items") && !ClaimMain.checkMembre(chunk, player)) {
                Material item = event.getMaterial();
                if(ClaimSettings.isRestrictedItem(item)) {
                    event.setCancelled(true);
                    ClaimMain.sendMessage(player,ClaimLanguage.getMessage("items"), ClaimSettings.getSetting("protection-message"));
                    return;
                }
	        }
	        return;
		}
    }
	
    /**
     * Handles player interact entity events to prevent entity interactions in claims.
     * @param event the player interact entity event.
     */
	@EventHandler
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if(ClaimMain.checkIfClaimExists(chunk)) {
        	Player player = event.getPlayer();
        	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
        	if(ClaimMain.checkMembre(chunk, player)) return;
        	Entity entity = event.getRightClicked();
        	
        	EntityType e = event.getRightClicked().getType();
        	if(!ClaimSettings.isRestrictedEntityType(e)) return;
        	if(!ClaimMain.canPermCheck(chunk, "Entities")) {
        		event.setCancelled(true);
        		ClaimMain.sendMessage(player,ClaimLanguage.getMessage("entities"), ClaimSettings.getSetting("protection-message"));
        		return;
        	}
        	
            ItemStack itemInHand = player.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
            	if(!ClaimSettings.isRestrictedItem(itemInHand.getType())) return;
                if (!ClaimMain.canPermCheck(entity.getLocation().getChunk(), "Items")) {
                    event.setCancelled(true);
                    ClaimMain.sendMessage(player,ClaimLanguage.getMessage("items"), ClaimSettings.getSetting("protection-message"));
                    return;
                }
            }
        }
        return;
    }
	
    /**
     * Handles secondary player interact entity events to prevent entity interactions in claims.
     * @param event the player interact entity event.
     */
	@EventHandler
    public void onPlayerInteractEntity2(PlayerInteractEntityEvent event) {
        Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if(ClaimMain.checkIfClaimExists(chunk)) {
        	Player player = event.getPlayer();
        	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
        	if(ClaimMain.checkMembre(chunk, player)) return;
        	Entity entity = event.getRightClicked();
        	
        	EntityType e = event.getRightClicked().getType();
        	if(!ClaimSettings.isRestrictedEntityType(e)) return;
        	if(!ClaimMain.canPermCheck(chunk, "Entities")) {
        		event.setCancelled(true);
        		ClaimMain.sendMessage(player,ClaimLanguage.getMessage("entities"), ClaimSettings.getSetting("protection-message"));
        		return;
        	}
        	
            ItemStack itemInHand = player.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
            	if(!ClaimSettings.isRestrictedItem(itemInHand.getType())) return;
                if (!ClaimMain.canPermCheck(entity.getLocation().getChunk(), "Items")) {
                    event.setCancelled(true);
                    ClaimMain.sendMessage(player,ClaimLanguage.getMessage("items"), ClaimSettings.getSetting("protection-message"));
                    return;
                }
            }
        }
        return;
    }
	
    /**
     * Handles liquid flow events to prevent liquids from flowing into claims.
     * @param event the block from-to event.
     */
	@EventHandler
    public void onLiquidFlow(BlockFromToEvent event) {
    	Block block = event.getBlock();
    	Block toBlock = event.getToBlock();
    	Chunk chunk = toBlock.getLocation().getChunk();
    	if(block.getLocation().getChunk().equals(chunk)) return;
    	if(ClaimMain.checkIfClaimExists(chunk)) {
    		if(ClaimMain.getOwnerInClaim(chunk).equals(ClaimMain.getOwnerInClaim(block.getLocation().getChunk()))) return;
    		if(ClaimMain.canPermCheck(chunk, "Liquids")) return;
            if (block.isLiquid()) {
                if (toBlock.getBlockData() instanceof Waterlogged) {
                    Waterlogged waterlogged = (Waterlogged) toBlock.getBlockData();
                    if (waterlogged.isWaterlogged()) {
                        event.setCancelled(true);
                        return;
                    }
                }
                if (toBlock.isEmpty() || toBlock.isPassable()) {
                    event.setCancelled(true);
                }
            } else {
                if (block.getBlockData() instanceof Waterlogged) {
                    Waterlogged waterlogged = (Waterlogged) block.getBlockData();
                    if (waterlogged.isWaterlogged()) {
                        event.setCancelled(true);
                        return;
                    }
                }
                if (toBlock.isEmpty() || toBlock.isPassable()) {
                    event.setCancelled(true);
                }
            }
    	}
    }
    
    /**
     * Handles block dispense events to prevent redstone interactions across claim boundaries.
     * @param event the block dispense event.
     */
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
    
    /**
     * Handles piston extend events to prevent pistons from moving blocks across claim boundaries.
     * @param event the block piston extend event.
     */
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

    /**
     * Handles piston retract events to prevent pistons from moving blocks across claim boundaries.
     * @param event the block piston retract event.
     */
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
    
    /**
     * Handles frost walker events to prevent frost walker enchantment from creating frosted ice in claims.
     * @param event the entity block form event.
     */
    @EventHandler
    public void onFrostWalkerUse(EntityBlockFormEvent event) {
    	Chunk chunk = event.getBlock().getLocation().getChunk();
    	if(!ClaimMain.checkIfClaimExists(chunk)) return;
    	if(ClaimMain.canPermCheck(chunk, "Frostwalker")) return;
        if (event.getNewState().getType() == Material.FROSTED_ICE) {
            Entity entity = event.getEntity();
            if (entity instanceof Player) {
                Player player = (Player) entity;
                if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
                ItemStack boots = player.getInventory().getBoots();
                if (boots != null && boots.containsEnchantment(Enchantment.FROST_WALKER)) {
                	if(ClaimMain.checkMembre(chunk, player)) return;
                    event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * Handles block spread events to prevent fire spread in claims.
     * @param event the block spread event.
     */
    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        if (event.getNewState().getType() == Material.FIRE) {
            Chunk chunk = event.getBlock().getLocation().getChunk();
            if(!ClaimMain.checkIfClaimExists(chunk)) return;
            if(ClaimMain.canPermCheck(chunk, "Firespread")) return;
            event.setCancelled(true);
        }
    }
    
    /**
     * Handles block ignite events to prevent fire spread in claims.
     * @param event the block ignite event.
     */
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        if(!ClaimMain.checkIfClaimExists(chunk)) return;
        Player player = event.getPlayer();
        if(player != null) {
        	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
        	if(ClaimMain.checkMembre(chunk, player)) return;
        }
        if(ClaimMain.canPermCheck(chunk, "Firespread")) return;
        event.setCancelled(true);
    }
    
    /**
     * Handles block burn events to prevent fire spread in claims.
     * @param event the block burn event.
     */
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        if(!ClaimMain.checkIfClaimExists(chunk)) return;
        if(ClaimMain.canPermCheck(chunk, "Firespread")) return;
        event.setCancelled(true);
    }
    
    /**
     * Handles entity damage by entity events to prevent damage to armor stands, item frames, and glow item frames in claims.
     * @param event the entity damage by entity event.
     */
    @EventHandler
    public void onEntityDamageByEntity2(EntityDamageByEntityEvent event) {
    	Entity entity = event.getEntity();
    	if(entity instanceof ArmorStand || entity instanceof ItemFrame || entity instanceof GlowItemFrame) {
            Entity damager = event.getDamager();
            Chunk chunk = entity.getLocation().getChunk();
            if (!ClaimMain.checkIfClaimExists(chunk)) return;
            if (damager instanceof Player) {
            	Player player = (Player) damager;
            	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
            	if(ClaimMain.checkMembre(chunk, player)) return;
                if (!ClaimMain.canPermCheck(chunk, "Destroy")) {
                	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
                    event.setCancelled(true);
                }
            	return;
            }
            
            if (!ClaimMain.canPermCheck(chunk, "Destroy")) {
            	event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handles entity damage by entity events to prevent damage to non-player, non-monster, non-armor stand, non-item frame entities in claims.
     * @param event the entity damage by entity event.
     */
    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Chunk chunk = entity.getLocation().getChunk();

        if(!ClaimMain.checkIfClaimExists(chunk)) return;

        if (!(entity instanceof Player) && !(entity instanceof Monster) && !(entity instanceof ArmorStand) && !(entity instanceof ItemFrame) ) {
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
    
    /**
     * Handles vehicle enter events to prevent players from entering restricted vehicles in claims.
     * @param event the vehicle enter event.
     */
    @EventHandler
    public void onVehicleEnter(VehicleEnterEvent event) {
        Entity entity = event.getEntered();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            Entity vehicle = event.getVehicle();
            EntityType vehicleType = vehicle.getType();
            if(!ClaimSettings.isRestrictedEntityType(vehicleType)) return;
        	Chunk chunk = vehicle.getLocation().getChunk();
            if (ClaimMain.checkIfClaimExists(chunk) &&
                !ClaimMain.canPermCheck(chunk, "Entities")) {
            	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
            	if(ClaimMain.checkMembre(chunk, player)) return;
                event.setCancelled(true);
                ClaimMain.sendMessage(player,ClaimLanguage.getMessage("entities"), ClaimSettings.getSetting("protection-message"));
            }
        }
    }
    
    /**
     * Handles block change events to prevent trampling of farmland in claims.
     * @param event the entity change block event.
     */
    @EventHandler
    public void onBlockChange(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        Block block = event.getBlock();

        if (entity.getType() == EntityType.PLAYER && block.getType() == Material.FARMLAND) {
            Player player = (Player) entity;
            Chunk chunk = block.getLocation().getChunk();
            if (ClaimMain.checkIfClaimExists(chunk)) {
            	if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
            	if(ClaimMain.checkMembre(chunk, player)) return;
                if(!ClaimMain.canPermCheck(chunk, "Destroy")) {
                	ClaimMain.sendMessage(player,ClaimLanguage.getMessage("destroy"), ClaimSettings.getSetting("protection-message"));
                    event.setCancelled(true);
                }

            }
        }
    }
    
    // ********************
    // *  Others Methods  *
    // ********************
    
    /**
     * Handles piston movement checks across claim boundaries.
     * @param blocks the list of blocks affected by the piston.
     * @param direction the direction of piston movement.
     * @param pistonChunk the chunk where the piston is located.
     * @param retractOrNot flag indicating whether the piston is retracting.
     * @return true if the piston can move the blocks, false otherwise.
     */
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
    
    /**
     * Processes damage by a player to prevent unauthorized damage in claims.
     * @param player the player causing the damage.
     * @param chunk the chunk where the damage occurs.
     * @param event the entity damage by entity event.
     */
    private void processDamageByPlayer(Player player, Chunk chunk, EntityDamageByEntityEvent event) {
        if(CPlayerMain.checkPermPlayer(player, "scs.bypass")) return;
        if(ClaimMain.checkMembre(chunk, player)) return;
        if(!ClaimMain.canPermCheck(chunk, "Damages")) {
            event.setCancelled(true);
            ClaimMain.sendMessage(player, ClaimLanguage.getMessage("damages"), ClaimSettings.getSetting("protection-message"));
        }
    }
    
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
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setspawn")) {
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
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
        	}
        	cPlayer.setGuiPage(1);
            ClaimMembersGui menu = new ClaimMembersGui(player,chunk,1);
            menu.openInventory(player);
            return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "manage-bans")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
        	}
        	cPlayer.setGuiPage(1);
            new ClaimBansGui(player,chunk,1);
            return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "define-name")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) {
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
        	}
        	player.closeInventory();
        	player.sendMessage(ClaimLanguage.getMessage("name-change-ask"));
        	return;
        }
        
        if(clickedSlot == ClaimGuis.getItemSlot("settings", "my-claims")) {
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.list")) {
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
        	}
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
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
        	}
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
        	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
        	}
        	if(ClaimMain.getOwnerInClaim(chunk).equals("admin")) {
        		ClaimMain.removeAdminClaimBan(chunk, owner);
        	} else {
        		ClaimMain.removeClaimBan(player, chunk, owner);
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
            	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return;
            }
            if(cPlayer.getFilter().equals("not_owner")) return;
            if(event.getClick() == ClickType.RIGHT) {
            	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                	return;
            	}
                new ClaimGui(player,cPlayer.getMapChunk(clickedSlot));
	        	return;
            }
            if(event.getClick() == ClickType.SHIFT_LEFT) {
            	if (!CPlayerMain.checkPermPlayer(player, "scs.command.unclaim")) {
                	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                	return;
            	}
            	Chunk chunk = cPlayer.getMapChunk(clickedSlot);
	        	if(ClaimMain.deleteClaim(player, chunk)) {
	        		for(Entity e : chunk.getEntities()) {
	    				if(!(e instanceof Player)) continue;
	    				Player p = (Player) e;
	    				ClaimEventsEnterLeave.disableBossBar(p);
	    			}
	    			player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
	            	int page = cPlayer.getGuiPage();
                    new ClaimListGui(player,page,cPlayer.getFilter());
	        	}
	        	return;
            }
            if(event.getClick() == ClickType.SHIFT_RIGHT) {
        		if(ClaimSettings.getBooleanSetting("economy")) {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.sclaim")) {
                    	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    	return;
                	}
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
	        		for(Entity e : chunk.getEntities()) {
	    				if(!(e instanceof Player)) continue;
	    				Player p = (Player) e;
	    				ClaimEventsEnterLeave.disableBossBar(p);
	    			}
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
		            if(!ClaimMain.canPermCheck(chunk, "Visitors") && !ClaimMain.getOwnerInClaim(chunk).equals(player.getName())) {
		            	player.sendMessage(ClaimLanguage.getMessage("error-claim-visitors-deny"));
		            	return;
		            }
	            	player.closeInventory();
		        	ClaimMain.goClaim(player, cPlayer.getMapLoc(clickedSlot));
		        	return;
        		}
        		player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
        		return;
        	}
        	if(event.getClick() == ClickType.SHIFT_LEFT) {
        		if(ClaimSettings.getBooleanSetting("economy")) {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.sclaim")) {
                    	player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    	return;
                	}
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
