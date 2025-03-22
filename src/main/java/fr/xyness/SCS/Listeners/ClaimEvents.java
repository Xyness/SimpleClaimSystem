package fr.xyness.SCS.Listeners;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.WeatherType;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.block.data.type.Bed;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Phantom;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
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
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;
import fr.xyness.SCS.Types.WorldMode;

/**
 * Event listener for claim-related events.
 */
public class ClaimEvents implements Listener {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    // Potions effect to cancel
    private final List<PotionEffectType> NEGATIVE_EFFECTS = new ArrayList<>();
    
    /** Bukkit version */
    private final String bukkitVersion = Bukkit.getVersion();
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimEvents.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimEvents(SimpleClaimSystem instance) {
    	this.instance = instance;
    	
    	NEGATIVE_EFFECTS.add(PotionEffectType.HARM);
    	NEGATIVE_EFFECTS.add(PotionEffectType.POISON);
    	NEGATIVE_EFFECTS.add(PotionEffectType.WITHER);
    	NEGATIVE_EFFECTS.add(PotionEffectType.SLOW);
    	NEGATIVE_EFFECTS.add(PotionEffectType.WEAKNESS);
    	NEGATIVE_EFFECTS.add(PotionEffectType.BLINDNESS);
    	NEGATIVE_EFFECTS.add(PotionEffectType.HUNGER);
    	NEGATIVE_EFFECTS.add(PotionEffectType.SLOW_DIGGING);
    	NEGATIVE_EFFECTS.add(PotionEffectType.LEVITATION);
    	NEGATIVE_EFFECTS.add(PotionEffectType.CONFUSION);
    	
    	if(!bukkitVersion.contains("1.18")) {
    		NEGATIVE_EFFECTS.add(PotionEffectType.DARKNESS);
    	}
    }
	
    
	// *******************
	// *  EventHandlers  *
	// *******************
	
	
    /**
     * Handles player command pre process.
     * 
     * @param event The PlayerCommandPreprocessEvent event.
     */
    @EventHandler
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String message = event.getMessage().toLowerCase();
        String command = message.split(" ")[0];
        String aliase = instance.getSettings().getAliase(command);
        if(aliase != null) {
            String newCommand = message.replaceFirst(command, aliase);
            event.setMessage(newCommand);
        }
    }
    
    
	
	/**
	 * Handles player glide event for Elytra.
	 * @param event the EntityToggleGlideEvent.
	 */
    @EventHandler
    public void onPlayerToggleGlide(EntityToggleGlideEvent event) {
    	WorldMode mode = instance.getSettings().getWorldMode(event.getEntity().getWorld().getName());
        if (event.getEntity() instanceof Player) {
            if (event.isGliding()) {
            	Player player = (Player) event.getEntity();
            	if (player.hasPermission("scs.bypass")) return;
            	Chunk chunk = player.getLocation().getChunk();
                Claim claim = instance.getMain().getClaim(chunk);
                if (claim != null) {
                    if (!claim.getPermissionForPlayer("Elytra", player)) {
                    	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("elytra"), instance.getSettings().getSetting("protection-message"));
                    	event.setCancelled(true);
                    }
                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Elytra")) {
                	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("elytra-mode"), instance.getSettings().getSetting("protection-message"));
                	event.setCancelled(true);
                }
            }
        }
    }
    
    /**
     * Handles the projectile launch event.
     * 
     * @param event The ProjectileLaunchEvent event.
     */
    @EventHandler
    public void onFireworkLaunch(ProjectileLaunchEvent event) {
    	WorldMode mode = instance.getSettings().getWorldMode(event.getEntity().getWorld().getName());
        if (event.getEntity() instanceof Firework) {
            Firework firework = (Firework) event.getEntity();
            if(firework.getShooter() instanceof Player) {
                Player player = (Player) firework.getShooter();
                if (player == null) return;
                if (player.hasPermission("scs.bypass")) return;
                if (player.isGliding() || player.getTargetBlockExact(5) == null) {
                	Chunk chunk = player.getLocation().getChunk();
                    Claim claim = instance.getMain().getClaim(chunk);
                    if (claim != null) {
                        if (!claim.getPermissionForPlayer("Elytra", player)) {
                        	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("elytra"), instance.getSettings().getSetting("protection-message"));
                        	event.setCancelled(true);
                        }
                    } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Elytra")) {
                    	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("elytra-mode"), instance.getSettings().getSetting("protection-message"));
                    	event.setCancelled(true);
                    }
                }
            }
        }
    }
	
	/**
	 * Handles player damage events by splash potion (to prevent pvp)
	 * @param event the potion splash event
	 */
	@EventHandler
	public void onPotionSplash(PotionSplashEvent event) {
		
		WorldMode mode = instance.getSettings().getWorldMode(event.getEntity().getWorld().getName());

	    if (event.getEntity() instanceof ThrownPotion) {
	        ThrownPotion thrownPotion = (ThrownPotion) event.getEntity();

	        if (thrownPotion.getShooter() instanceof Player) {
	            Player damager = (Player) thrownPotion.getShooter();

	            if (damager.hasPermission("scs.bypass")) return;

	            for (PotionEffect effect : thrownPotion.getEffects()) {
	                if (NEGATIVE_EFFECTS.contains(effect.getType())) {
	                    for (Entity entity : event.getAffectedEntities()) {
	                        if (entity.getType() == EntityType.PLAYER) {
	                            Player player = (Player) entity;
	                            if(player == damager) return;
	                            Chunk chunk = player.getLocation().getChunk();
	                            Claim claim = instance.getMain().getClaim(chunk);

	                            if (claim != null) {
	                                if (!claim.getPermission("Pvp", "Natural")) {
	                                    instance.getMain().sendMessage(damager, instance.getLanguage().getMessage("pvp"), instance.getSettings().getSetting("protection-message"));
	                                    event.setIntensity(player, 0.0);
	                                }
	                            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Pvp")) {
	                            	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("pvp-mode"), instance.getSettings().getSetting("protection-message"));
	                            	event.setCancelled(true);
	                            }
	                        }
	                    }
	                }
	            }
	        }
	    }
	}
	
	/**
	 * Handles player damage events to disable claim fly on damage.
	 * @param event the player damage event.
	 */
	@EventHandler
	public void onPlayerDamage(EntityDamageEvent event) {
		if(!(event.getEntity() instanceof Player)) return;
		if(!instance.getSettings().getBooleanSetting("claim-fly-disabled-on-damage")) return;
		Player player = (Player) event.getEntity();
		CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
    	if(cPlayer != null && cPlayer.getClaimFly()) {
    		instance.getPlayerMain().removePlayerFly(player);
    		instance.getMain().sendMessage(player, instance.getLanguage().getMessage("claim-fly-disabled-on-damage"), instance.getSettings().getSetting("protection-message"));
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
	    WorldMode mode = instance.getSettings().getWorldMode(player.getWorld().getName());
	    
	    if(instance.getMain().checkIfClaimExists(chunk)) {
	    	Claim claim = instance.getMain().getClaim(chunk);
	        if (event.getDamager() instanceof Player) {
	            Player damager = (Player) event.getDamager();
	            if(player == damager) return;
	            if(damager.hasPermission("scs.bypass")) return;
	            if(!claim.getPermission("Pvp", "Natural")) {
	                instance.getMain().sendMessage(damager, instance.getLanguage().getMessage("pvp"), instance.getSettings().getSetting("protection-message"));
	                event.setCancelled(true);
	            }
	        } else if (event.getDamager() instanceof Projectile) {
	            Projectile projectile = (Projectile) event.getDamager();
	            ProjectileSource shooter = projectile.getShooter();
	            if (shooter instanceof Player) {
	                Player damager = (Player) shooter;
	                if(player == damager) return;
	                if(damager.hasPermission("scs.bypass")) return;
	                if(!claim.getPermission("Pvp", "Natural")) {
	                    instance.getMain().sendMessage(damager, instance.getLanguage().getMessage("pvp"), instance.getSettings().getSetting("protection-message"));
	                    event.setCancelled(true);
	                }
	            }
	        }
	    } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Pvp")) {
	    	if (event.getDamager() instanceof Player damager) {
    	    	instance.getMain().sendMessage(damager, instance.getLanguage().getMessage("pvp-mode"), instance.getSettings().getSetting("protection-message"));
            	event.setCancelled(true);
	        } else if (event.getDamager() instanceof Projectile) {
	            Projectile projectile = (Projectile) event.getDamager();
	            ProjectileSource shooter = projectile.getShooter();
	            if (shooter instanceof Player damager) {
	    	    	instance.getMain().sendMessage(damager, instance.getLanguage().getMessage("pvp-mode"), instance.getSettings().getSetting("protection-message"));
	            	event.setCancelled(true);
	            }
	        }

        }
	}

    /**
     * Handles creature spawn events to prevent monster spawning in claims.
     * @param event the creature spawn event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
		WorldMode mode = instance.getSettings().getWorldMode(event.getLocation().getWorld().getName());
		Chunk chunk = event.getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			Entity entity = event.getEntity();
			if(entity instanceof Monster || entity instanceof Phantom) {
				if(!claim.getPermission("Monsters", "Natural")) {
					event.setCancelled(true);
					return;
				}
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Monsters")) {
			Entity entity = event.getEntity();
			if(entity instanceof Monster || entity instanceof Phantom) {
				event.setCancelled(true);
			}
        }
    }
    
    /**
     * Handles player drop items events to prevent player dropping in claims.
     * @param event the drop items event.
     */
    @EventHandler
    public void onPlayerDropItem(PlayerDropItemEvent event) {
    	Chunk chunk = event.getItemDrop().getLocation().getChunk();
    	Player player = event.getPlayer();
    	WorldMode mode = instance.getSettings().getWorldMode(player.getWorld().getName());
    	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("ItemsDrop", player)) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("itemsdrop"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("ItemsDrop")) {
        	event.setCancelled(true);
        	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("itemsdrop-mode"), instance.getSettings().getSetting("protection-message"));
        }
    }
    
    /**
     * Handles player portal events to prevent player using portals in claims.
     * 
     * @param event The PlayerPortalEvent event.
     */
    @EventHandler
    public void onPlayerUsePortal(PlayerPortalEvent event) {
    	Chunk chunk = event.getFrom().getChunk();
    	Player player = event.getPlayer();
    	WorldMode mode = instance.getSettings().getWorldMode(player.getWorld().getName());
    	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Portals", player)) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("portals"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Portals")) {
        	event.setCancelled(true);
        	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("portals-mode"), instance.getSettings().getSetting("protection-message"));
        }
    }
	
    /**
     * Handles entity explosion events to prevent explosions in claims.
     * 
     * @param event The EntityExplodeEvent event.
     */
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
    	WorldMode mode = instance.getSettings().getWorldMode(event.getLocation().getWorld().getName());
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            Chunk chunk = block.getLocation().getChunk();
            if (instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
                blockIterator.remove();
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
            	blockIterator.remove();
            }
        }
        if (instance.getMinecraftVersion().contains("1.21") && event.getEntityType() == EntityType.WIND_CHARGE) {
        	Projectile wind = (Projectile) event.getEntity();
        	if(wind.getShooter() instanceof Player player) {
        		Chunk chunk = event.getEntity().getLocation().getChunk();
        		if(instance.getMain().checkIfClaimExists(chunk)) {
        			Claim claim = instance.getMain().getClaim(chunk);
        			if(!claim.getPermissionForPlayer("Windcharges", player)) {
        				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("windcharges"), instance.getSettings().getSetting("protection-message"));
                        event.getEntity().getNearbyEntities(5, 5, 5).forEach(entity -> {
                        	entity.setVelocity(new Vector(0, 0, 0));
                        });
        			}
        		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Windcharges")) {
    				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("windcharges-mode"), instance.getSettings().getSetting("protection-message"));
                    event.getEntity().getNearbyEntities(5, 5, 5).forEach(entity -> {
                    	entity.setVelocity(new Vector(0, 0, 0));
                    });
        		}
        		return;
        	}
    		return;
        }
    }
    
    /**
     * Handles projectile hit events to prevent explosions in claims.
     * @param event the projectile hit event.
     */
    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
    	WorldMode mode = instance.getSettings().getWorldMode(event.getEntity().getWorld().getName());
		if (event.getEntityType() == EntityType.WITHER_SKULL) {
            if (event.getHitBlock() != null) {
            	Block block = event.getHitBlock();
            	Chunk chunk = block.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
                	event.getEntity().remove();
                	event.setCancelled(true);
                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
                	event.getEntity().remove();
                	event.setCancelled(true);
                }
            }
            if (event.getHitEntity() != null) {
        		Chunk chunk = event.getHitEntity().getLocation().getChunk();
        		if(instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
        			event.getEntity().remove();
        			event.setCancelled(true);
        		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
        			event.getEntity().remove();
                	event.setCancelled(true);
                }
            }
            event.getEntity().getNearbyEntities(5, 5, 5).forEach(entity -> {
            	Chunk chunk = entity.getLocation().getChunk();
            	if (instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
                    entity.setVelocity(new Vector(0, 0, 0));
                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
                	entity.setVelocity(new Vector(0, 0, 0));
                }
            });
        } else if (instance.getMinecraftVersion().contains("1.21") && event.getEntityType() == EntityType.WIND_CHARGE) {
            if (event.getHitBlock() != null) {
            	Block block = event.getHitBlock();
            	Chunk chunk = block.getLocation().getChunk();
                if (instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
                	event.setCancelled(true);
                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
                	event.setCancelled(true);
                }
            }
            if (event.getHitEntity() != null) {
        		Chunk chunk = event.getHitEntity().getLocation().getChunk();
        		if(instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
        			event.setCancelled(true);
        		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
                	event.setCancelled(true);
                }
            }
        } else if (event.getEntityType() == EntityType.ENDER_PEARL && instance.isFolia()) {
            if (event.getEntity().getShooter() instanceof Player player) {
            	EnderPearl pearl = (EnderPearl) event.getEntity();
            	Location playerLocation = player.getLocation();
                Location pearlLocation = pearl.getLocation();
                
                Chunk to = pearlLocation.getChunk();
                Chunk from = playerLocation.getChunk();
                if (!instance.getMain().checkIfClaimExists(to)) {
                	instance.getBossBars().disableBossBar(player);
                	return;
                }

                UUID playerId = player.getUniqueId();
                CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerId);
                if(cPlayer == null) return;
                
                String ownerTO = instance.getMain().getOwnerInClaim(to);
                String ownerFROM = instance.getMain().getOwnerInClaim(from);
                
                Claim claim = instance.getMain().getClaim(to);
                if(claim != null) {
        	        if (instance.getMain().checkBan(claim, player) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.ban")) {
        	            cancelTeleport(event, player, "player-banned");
        	            return;
        	        }
        	        
        	        if (!claim.getPermissionForPlayer("Enter",player) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.enter")) {
        	            cancelTeleport(event, player, "enter");
        	            return;
        	        }
        	
        	        if (!instance.getPlayerMain().checkPermPlayer(player, "scs.bypass") && !claim.getPermissionForPlayer("Teleportations",player)) {
        	            cancelTeleport(event, player, "teleportations");
        	            return;
        	        }
                }
                
                instance.getBossBars().activeBossBar(player, to);
                handleAutoFly(player, cPlayer, to, ownerTO);
                handleWeatherSettings(player, to, from);
                
                String world = player.getWorld().getName();

                if (!ownerTO.equals(ownerFROM)) {
                    handleEnterLeaveMessages(player, to, from, ownerTO, ownerFROM);
                    if (cPlayer.getClaimAuto().equals("addchunk")) {
                        handleAutoAddChunk(player, cPlayer, to, world);
                    } else if (cPlayer.getClaimAuto().equals("delchunk")) {
                        handleAutoDelChunk(player, cPlayer, to, world);
                    } else if (cPlayer.getClaimAuto().equals("claim")) {
                        handleAutoClaim(player, cPlayer, to, world);
                    } else if (cPlayer.getClaimAuto().equals("unclaim")) {
                        handleAutoUnclaim(player, cPlayer, to, world);
                    }
                }

                if (cPlayer.getClaimAutomap()) {
                	handleAutoMap(player, cPlayer, to, world);
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
    	WorldMode mode = instance.getSettings().getWorldMode(event.getBlock().getWorld().getName());
        Iterator<Block> blockIterator = event.blockList().iterator();
        while (blockIterator.hasNext()) {
            Block block = blockIterator.next();
            Chunk chunk = block.getLocation().getChunk();
            if (instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
                blockIterator.remove();
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
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
    	WorldMode mode = instance.getSettings().getWorldMode(event.getBlock().getLocation().getWorld().getName());
        if (event.getEntityType() == EntityType.WITHER || event.getEntityType() == EntityType.WITHER_SKULL) {
            Block block = event.getBlock();
            Chunk chunk = block.getLocation().getChunk();
            if (instance.getMain().checkIfClaimExists(chunk) && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
            	event.setCancelled(true);
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Explosions")) {
            	event.setCancelled(true);
            }
        }

    }
	
    /**
     * Handles block break events to prevent destruction in claims.
     * @param event the block break event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerBreak(BlockBreakEvent event){
		Player player = event.getPlayer();
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Destroy", player) && !instance.getSettings().isBreakBlockIgnore(event.getBlock().getType())) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy"), instance.getSettings().getSetting("protection-message"));
				return;
			}
			if(instance.getSettings().isSpecialBlock(event.getBlock().getType()) && !claim.getPermissionForPlayer("SpecialBlocks", player)) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("specialblocks"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
			if(!instance.getSettings().getSettingSRC("Destroy")) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy-mode"), instance.getSettings().getSetting("protection-message"));
				return;
			}
			if(instance.getSettings().isSpecialBlock(event.getBlock().getType()) && !instance.getSettings().getSettingSRC("SpecialBlocks")) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("specialblocks-mode"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		}
	}
	
    /**
     * Handles vehicle damage events to prevent destruction of vehicles in claims.
     * @param event the vehicle damage event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
	public void onVehicleDamage(VehicleDamageEvent event){
		Entity damager = event.getAttacker();
		if(damager == null) return;
		WorldMode mode = instance.getSettings().getWorldMode(damager.getLocation().getWorld().getName());
		Chunk chunk = event.getVehicle().getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			if(damager instanceof Player) {
				Player player = (Player) damager;
				if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
				Claim claim = instance.getMain().getClaim(chunk);
				if(!claim.getPermissionForPlayer("Destroy", player)) {
					event.setCancelled(true);
					instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy"), instance.getSettings().getSetting("protection-message"));
					return;
				}
				return;
			}
			if(!instance.getMain().canPermCheck(chunk, "Destroy", "Visitors")) {
				event.setCancelled(true);
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Destroy")) {
			if(damager instanceof Player player) {
				if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy-mode"), instance.getSettings().getSetting("protection-message"));
				return;
			}
			event.setCancelled(true);
		}

	}
	
    /**
     * Handles block place events to prevent building in claims.
     * @param event the block place event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPlace(BlockPlaceEvent event){
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		Block block = event.getBlock();
		Chunk chunk = block.getLocation().getChunk();
		
	    if (block.getBlockData() instanceof Bed bed) {
	        if (!instance.getSettings().isPlaceBlockIgnore(block.getType())) {
	            BlockFace facing = bed.getFacing();
	            Block adjacentBlock = block.getRelative(facing);
	            Chunk adjacentChunk = adjacentBlock.getChunk();

	            if (!chunk.equals(adjacentChunk)) {
	                if (instance.getMain().checkIfClaimExists(adjacentChunk) &&
	                    !instance.getMain().getOwnerInClaim(chunk).equals(instance.getMain().getOwnerInClaim(adjacentChunk))) {
	                    Claim claim = instance.getMain().getClaim(adjacentChunk);
	                    if (!claim.getPermissionForPlayer("Build", player)) {
	                        event.setCancelled(true);
	                        instance.getMain().sendMessage(player, instance.getLanguage().getMessage("build"), instance.getSettings().getSetting("protection-message"));
	                        return;
	                    }
	                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Build")) {
                        event.setCancelled(true);
                        instance.getMain().sendMessage(player, instance.getLanguage().getMessage("build-mode"), instance.getSettings().getSetting("protection-message"));
                        return;
	                }
	            }
	        }
	    }
		
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Build", player) && !instance.getSettings().isPlaceBlockIgnore(block.getType())) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Build")) {
            event.setCancelled(true);
            instance.getMain().sendMessage(player, instance.getLanguage().getMessage("build-mode"), instance.getSettings().getSetting("protection-message"));
            return;
		}
	}
	
    /**
     * Handles hanging place events to prevent hanging items in claims.
     * @param event the hanging place event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onHangingPlace(HangingPlaceEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Build", player) && !instance.getSettings().isPlaceBlockIgnore(event.getBlock().getType())) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Build")) {
			event.setCancelled(true);
			instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build-mode"), instance.getSettings().getSetting("protection-message"));
		}
	}
	
    /**
     * Handles hanging break events to prevent hanging items from being broken by physics in claims.
     * @param event the hanging break event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onHangingBreak(HangingBreakEvent event) {
		Chunk chunk = event.getEntity().getLocation().getChunk();
		WorldMode mode = instance.getSettings().getWorldMode(event.getEntity().getLocation().getWorld().getName());
		if(instance.getMain().checkIfClaimExists(chunk)) {
			if(event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS && !instance.getMain().canPermCheck(chunk, "Destroy", "Visitors")) {
				event.setCancelled(true);
			} else if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && !instance.getMain().canPermCheck(chunk, "Explosions", "Natural")) {
				event.setCancelled(true);
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
			if(event.getCause() == HangingBreakEvent.RemoveCause.PHYSICS && !instance.getSettings().getSettingSRC("Destroy")) {
				event.setCancelled(true);
			} else if (event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION && !instance.getSettings().getSettingSRC("Explosions")) {
				event.setCancelled(true);
			}
		}

	}
	
    /**
     * Handles hanging break by entity events to prevent hanging items from being broken by players in claims.
     * @param event the hanging break by entity event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        if (event.getEntity().getType() == EntityType.PAINTING
        		|| event.getEntity().getType() == EntityType.ITEM_FRAME 
        		|| event.getEntity().getType() == EntityType.GLOW_ITEM_FRAME) {
        	WorldMode mode = instance.getSettings().getWorldMode(event.getEntity().getLocation().getWorld().getName());
        	Chunk chunk = event.getEntity().getLocation().getChunk();
        	if(instance.getMain().checkIfClaimExists(chunk)) {
                if (event.getRemover() instanceof Player player) {
                	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                	Claim claim = instance.getMain().getClaim(chunk);
                	if(!claim.getPermissionForPlayer("Destroy", player)) {
                		event.setCancelled(true);
                		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy"), instance.getSettings().getSetting("protection-message"));
                		return;
                	}
                    return;
                }
               	if(!instance.getMain().canPermCheck(chunk, "Destroy", "Visitors")) {
            		event.setCancelled(true);
            		return;
            	}
        	} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Destroy")) {
        		if (event.getRemover() instanceof Player player) {
        			if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
            		event.setCancelled(true);
            		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy-mode"), instance.getSettings().getSetting("protection-message"));
            		return;
        		}
        		event.setCancelled(true);
        	}
        }
    }
	
    /**
     * Handles bucket empty events to prevent liquid placement in claims.
     * @param event the bucket empty event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
    public void onBucketUse(PlayerBucketEmptyEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Build", player) && !instance.getSettings().isPlaceBlockIgnore(event.getBlock().getType())) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Build")) {
			event.setCancelled(true);
			instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build-mode"), instance.getSettings().getSetting("protection-message"));
		}
    }
	
    /**
     * Handles bucket fill events to prevent liquid removal in claims.
     * @param event the bucket fill event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
    public void onBucketUse(PlayerBucketFillEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Destroy", player) && !instance.getSettings().isBreakBlockIgnore(event.getBlock().getType())) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Destroy")) {
			event.setCancelled(true);
			instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy-mode"), instance.getSettings().getSetting("protection-message"));
		}

    }
	
	/**
	 * Handles the player fish event.
	 * 
	 * @param event the player fish event.
	 */
	@EventHandler
	public void onPlayerFish(PlayerFishEvent event) {
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		if(event.getCaught() instanceof Entity) {
			Entity entity = event.getCaught();
			if(entity != null) {
				Chunk chunk = entity.getLocation().getChunk();
				if(instance.getMain().checkIfClaimExists(chunk)) {
					Claim claim = instance.getMain().getClaim(chunk);
					if(!claim.getPermissionForPlayer("Entities", player)) {
						event.setCancelled(true);
		        		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities"), instance.getSettings().getSetting("protection-message"));
		        		return;
					}
				} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Entities")) {
					event.setCancelled(true);
					instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities-mode"), instance.getSettings().getSetting("protection-message"));
				}
			}
		}

	}
	
    /**
     * Handles entity place events to prevent entity placement in claims.
     * @param event the entity place event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityPlace(EntityPlaceEvent event) {
		if(event.isCancelled()) return;
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		Chunk chunk = event.getBlock().getLocation().getChunk();
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("Build", player)) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Build")) {
			event.setCancelled(true);
			instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build-mode"), instance.getSettings().getSetting("protection-message"));
		}
	}
	
    /**
     * Handles player interact events to prevent interactions with blocks and items in claims.
     * @param event the player interact event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
		Block block = event.getClickedBlock();
		Chunk chunk;
		if(block == null) {
			chunk = player.getLocation().getChunk();
		} else {
			chunk = block.getLocation().getChunk();
		}
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
	        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
	            Material mat = event.getClickedBlock().getType();
	            if (mat.name().contains("BUTTON") && !claim.getPermissionForPlayer("Buttons", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("buttons"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("TRAPDOOR") && !claim.getPermissionForPlayer("Trapdoors", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("trapdoors"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("DOOR") && !claim.getPermissionForPlayer("Doors", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("doors"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("FENCE_GATE") && !claim.getPermissionForPlayer("Fencegates", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("fencegates"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.LEVER) && !claim.getPermissionForPlayer("Levers", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("levers"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.REPEATER) && !claim.getPermissionForPlayer("RepeatersComparators", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("repeaters"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.COMPARATOR) && !claim.getPermissionForPlayer("RepeatersComparators", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("comparators"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.BELL) && !claim.getPermissionForPlayer("Bells", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("bells"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if(!claim.getPermissionForPlayer("InteractBlocks", player)) {
	            	Material item = block.getType();
	            	if(instance.getSettings().isRestrictedContainer(item)) {
                        event.setCancelled(true);
                        instance.getMain().sendMessage(player,instance.getLanguage().getMessage("interactblocks"), instance.getSettings().getSetting("protection-message"));
                        return;
	            	}
	            }
	            if(!claim.getPermissionForPlayer("Items", player)) {
	                Material item = event.getMaterial();
	                if(instance.getSettings().isRestrictedItem(item)) {
                        event.setCancelled(true);
                        instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items"), instance.getSettings().getSetting("protection-message"));
                        return;
	                }
	            }
	            return;
	        }
	        if (event.getAction() == Action.PHYSICAL) {
	        	if(block != null && block.getType().name().contains("PRESSURE_PLATE") && !claim.getPermissionForPlayer("Plates", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("plates"), instance.getSettings().getSetting("protection-message"));
	                return;
	        	}
	        	if (block.getType() == Material.TRIPWIRE && !claim.getPermissionForPlayer("Tripwires", player)) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("tripwires"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	        }
	        if(!claim.getPermissionForPlayer("Items", player)) {
                Material item = event.getMaterial();
                if(instance.getSettings().isRestrictedItem(item)) {
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items"), instance.getSettings().getSetting("protection-message"));
                    return;
                }
	        }
	        return;
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
	        if ((event.getAction() == Action.RIGHT_CLICK_BLOCK || event.getAction() == Action.LEFT_CLICK_BLOCK)) {
	            Material mat = event.getClickedBlock().getType();
	            if (mat.name().contains("BUTTON") && !instance.getSettings().getSettingSRC("Buttons")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("buttons-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("TRAPDOOR") && !instance.getSettings().getSettingSRC("Trapdoors")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("trapdoors-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("DOOR") && !instance.getSettings().getSettingSRC("Doors")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("doors-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.name().contains("FENCE_GATE") && !instance.getSettings().getSettingSRC("Fencegates")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("fencegates-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.LEVER) && !instance.getSettings().getSettingSRC("Levers")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("levers-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.REPEATER) && !instance.getSettings().getSettingSRC("RepeatersComparators")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("repeaters-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.COMPARATOR) && !instance.getSettings().getSettingSRC("RepeatersComparators")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("comparators-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if (mat.equals(Material.BELL) && !instance.getSettings().getSettingSRC("Bells")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("bells-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	            if(!instance.getSettings().getSettingSRC("InteractBlocks")) {
	            	Material item = block.getType();
	            	if(instance.getSettings().isRestrictedContainer(item)) {
                        event.setCancelled(true);
                        instance.getMain().sendMessage(player,instance.getLanguage().getMessage("interactblocks-mode"), instance.getSettings().getSetting("protection-message"));
                        return;
	            	}
	            }
	            if(!instance.getSettings().getSettingSRC("Items")) {
	                Material item = event.getMaterial();
	                if(instance.getSettings().isRestrictedItem(item)) {
                        event.setCancelled(true);
                        instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items-mode"), instance.getSettings().getSetting("protection-message"));
                        return;
	                }
	            }
	            return;
	        }
	        if (event.getAction() == Action.PHYSICAL) {
	        	if(block != null && block.getType().name().contains("PRESSURE_PLATE") && !instance.getSettings().getSettingSRC("Plates")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("plates-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	        	}
	        	if (block.getType() == Material.TRIPWIRE && !instance.getSettings().getSettingSRC("Tripwires")) {
	            	event.setCancelled(true);
	            	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("tripwires-mode"), instance.getSettings().getSetting("protection-message"));
	                return;
	            }
	        }
	        if(!instance.getSettings().getSettingSRC("Items")) {
                Material item = event.getMaterial();
                if(instance.getSettings().isRestrictedItem(item)) {
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items-mode"), instance.getSettings().getSetting("protection-message"));
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
	@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractAtEntityEvent event) {
    	Player player = event.getPlayer();
    	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(event.getRightClicked().getLocation().getWorld().getName());
		Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if(instance.getMain().checkIfClaimExists(chunk)) {
        	Claim claim = instance.getMain().getClaim(chunk);
        	Entity entity = event.getRightClicked();
        	EntityType e = event.getRightClicked().getType();
        	if(!instance.getSettings().isRestrictedEntityType(e)) return;
        	if(!claim.getPermissionForPlayer("Entities", player)) {
        		event.setCancelled(true);
        		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities"), instance.getSettings().getSetting("protection-message"));
        		return;
        	}
        	
            ItemStack itemInHand = player.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
            	if(!instance.getSettings().isRestrictedItem(itemInHand.getType())) return;
            	Claim claim2 = instance.getMain().getClaim(entity.getLocation().getChunk());
            	if(claim2 == null) return;
                if (!claim.getPermissionForPlayer("Items", player)) {
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items"), instance.getSettings().getSetting("protection-message"));
                    return;
                }
            }
        } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
        	EntityType e = event.getRightClicked().getType();
        	if(!instance.getSettings().isRestrictedEntityType(e)) return;
        	if(!instance.getSettings().getSettingSRC("Entities")) {
        		event.setCancelled(true);
        		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities-mode"), instance.getSettings().getSetting("protection-message"));
        		return;
        	}
        	
            ItemStack itemInHand = player.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
            	if(!instance.getSettings().isRestrictedItem(itemInHand.getType())) return;
                if (!instance.getSettings().getSettingSRC("Items")) {
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items-mode"), instance.getSettings().getSetting("protection-message"));
                    return;
                }
            }
        }
    }
	
    /**
     * Handles secondary player interact entity events to prevent entity interactions in claims.
     * @param event the player interact entity event.
     */
	@EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity2(PlayerInteractEntityEvent event) {
    	Player player = event.getPlayer();
    	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		WorldMode mode = instance.getSettings().getWorldMode(event.getRightClicked().getLocation().getWorld().getName());
		Chunk chunk = event.getRightClicked().getLocation().getChunk();
        if(instance.getMain().checkIfClaimExists(chunk)) {
        	Claim claim = instance.getMain().getClaim(chunk);
        	Entity entity = event.getRightClicked();
        	EntityType e = event.getRightClicked().getType();
        	if(!instance.getSettings().isRestrictedEntityType(e)) return;
        	if(!claim.getPermissionForPlayer("Entities", player)) {
        		event.setCancelled(true);
        		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities"), instance.getSettings().getSetting("protection-message"));
        		return;
        	}
        	
            ItemStack itemInHand = player.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
            	if(!instance.getSettings().isRestrictedItem(itemInHand.getType())) return;
            	Claim claim2 = instance.getMain().getClaim(entity.getLocation().getChunk());
            	if(claim2 == null) return;
                if (!claim.getPermissionForPlayer("Items", player)) {
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items"), instance.getSettings().getSetting("protection-message"));
                    return;
                }
            }
        } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
        	EntityType e = event.getRightClicked().getType();
        	if(!instance.getSettings().isRestrictedEntityType(e)) return;
        	if(!instance.getSettings().getSettingSRC("Entities")) {
        		event.setCancelled(true);
        		instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities-mode"), instance.getSettings().getSetting("protection-message"));
        		return;
        	}
        	
            ItemStack itemInHand = player.getInventory().getItem(event.getHand());
            if (itemInHand != null) {
            	if(!instance.getSettings().isRestrictedItem(itemInHand.getType())) return;
                if (!instance.getSettings().getSettingSRC("Items")) {
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player,instance.getLanguage().getMessage("items-mode"), instance.getSettings().getSetting("protection-message"));
                    return;
                }
            }
        }
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
    	WorldMode mode = instance.getSettings().getWorldMode(toBlock.getLocation().getWorld().getName());
    	if(block.getLocation().getChunk().equals(chunk)) return;
    	if(instance.getMain().checkIfClaimExists(chunk)) {
    		if(instance.getMain().getOwnerInClaim(chunk).equals(instance.getMain().getOwnerInClaim(block.getLocation().getChunk()))) return;
    		if(instance.getMain().canPermCheck(chunk, "Liquids", "Natural")) return;
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
    	} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Liquids")) {
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
    	WorldMode mode = instance.getSettings().getWorldMode(block.getLocation().getWorld().getName());
    	if(instance.getMain().checkIfClaimExists(targetChunk)) {
    		if(instance.getMain().getOwnerInClaim(block.getLocation().getChunk()).equals(instance.getMain().getOwnerInClaim(targetChunk))) return;
    		if(!instance.getMain().canPermCheck(targetChunk, "Redstone", "Natural")) {
    			event.setCancelled(true);
    		}
    	} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Redstone")) {
    		event.setCancelled(true);
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
    	WorldMode mode = instance.getSettings().getWorldMode(event.getBlock().getLocation().getWorld().getName());
    	if(instance.getMain().checkIfClaimExists(chunk)) {
        	Claim claim = instance.getMain().getClaim(chunk);
            if (event.getNewState().getType() == Material.FROSTED_ICE) {
                Entity entity = event.getEntity();
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                    if(claim.getPermissionForPlayer("FrostWalker", player)) return;
                    ItemStack boots = player.getInventory().getBoots();
                    if (boots != null && boots.containsEnchantment(Enchantment.FROST_WALKER)) {
                    	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("frostwalker"), instance.getSettings().getSetting("protection-message"));
                        event.setCancelled(true);
                    }
                }
            }
    	} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Frostwalker")) {
            if (event.getNewState().getType() == Material.FROSTED_ICE) {
                Entity entity = event.getEntity();
                if (entity instanceof Player) {
                    Player player = (Player) entity;
                    if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                    ItemStack boots = player.getInventory().getBoots();
                    if (boots != null && boots.containsEnchantment(Enchantment.FROST_WALKER)) {
                    	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("frostwalker-mode"), instance.getSettings().getSetting("protection-message"));
                        event.setCancelled(true);
                    }
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
            WorldMode mode = instance.getSettings().getWorldMode(event.getBlock().getLocation().getWorld().getName());
            if(instance.getMain().checkIfClaimExists(chunk)) {
                if(instance.getMain().canPermCheck(chunk, "Firespread", "Natural")) return;
                event.setCancelled(true);
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Firespread")) {
            	event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handles block ignite events to prevent fire spread in claims.
     * @param event the block ignite event.
     */
    @EventHandler
    public void onBlockIgnite(BlockIgniteEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        WorldMode mode = instance.getSettings().getWorldMode(event.getBlock().getLocation().getWorld().getName());
        if(instance.getMain().checkIfClaimExists(chunk)) {
            Claim claim = instance.getMain().getClaim(chunk);
            Player player = event.getPlayer();
            if(player != null) {
            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
    			if(!claim.getPermissionForPlayer("Build", player)) {
    				event.setCancelled(true);
    				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build"), instance.getSettings().getSetting("protection-message"));
    				return;
    			}
    			return;
            }
            if(instance.getMain().canPermCheck(chunk, "Firespread", "Natural")) return;
            event.setCancelled(true);
        } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
            Player player = event.getPlayer();
            if(player != null) {
            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
    			if(!instance.getSettings().getSettingSRC("Build")) {
    				event.setCancelled(true);
    				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("build-mode"), instance.getSettings().getSetting("protection-message"));
    				return;
    			}
    			return;
            }
            if(!instance.getSettings().getSettingSRC("Firespread")) {
            	event.setCancelled(true);
            }
        }
    }
    
    /**
     * Handles block burn events to prevent fire spread in claims.
     * @param event the block burn event.
     */
    @EventHandler
    public void onBlockBurn(BlockBurnEvent event) {
        Chunk chunk = event.getBlock().getLocation().getChunk();
        WorldMode mode = instance.getSettings().getWorldMode(event.getBlock().getLocation().getWorld().getName());
        if(instance.getMain().checkIfClaimExists(chunk)) {
            if(instance.getMain().canPermCheck(chunk, "Firespread", "Natural")) return;
            event.setCancelled(true);
        } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Firespread")) {
        	event.setCancelled(true);
        }

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
            WorldMode mode = instance.getSettings().getWorldMode(damager.getLocation().getWorld().getName());
            Chunk chunk = entity.getLocation().getChunk();
            if (instance.getMain().checkIfClaimExists(chunk)) {
                if (damager instanceof Player) {
                	Claim claim = instance.getMain().getClaim(chunk);
                	Player player = (Player) damager;
                	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                    if (!claim.getPermissionForPlayer("Destroy", player)) {
                    	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy"), instance.getSettings().getSetting("protection-message"));
                        event.setCancelled(true);
                    }
                	return;
                }
                if (!instance.getMain().canPermCheck(chunk, "Destroy", "Visitors")) {
                	event.setCancelled(true);
                }
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS) {
                if (damager instanceof Player) {
                	Player player = (Player) damager;
                	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                    if (!instance.getSettings().getSettingSRC("Destroy")) {
                    	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy-mode"), instance.getSettings().getSetting("protection-message"));
                        event.setCancelled(true);
                    }
                	return;
                }
                if (!instance.getSettings().getSettingSRC("Destroy")) {
                	event.setCancelled(true);
                }
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
        WorldMode mode = instance.getSettings().getWorldMode(entity.getLocation().getWorld().getName());
        if(instance.getMain().checkIfClaimExists(chunk)) {
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
        } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Damages")) {
            if (!(entity instanceof Player) && !(entity instanceof Monster) && !(entity instanceof ArmorStand) && !(entity instanceof ItemFrame) ) {
                Entity damager = event.getDamager();
                if (damager instanceof Player player) {
                	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                    event.setCancelled(true);
                    instance.getMain().sendMessage(player, instance.getLanguage().getMessage("damages-mode"), instance.getSettings().getSetting("protection-message"));
                } else if (damager instanceof Projectile) {
                    Projectile projectile = (Projectile) damager;
                    ProjectileSource shooter = projectile.getShooter();
                    if (shooter instanceof Player player) {
                        event.setCancelled(true);
                        instance.getMain().sendMessage(player, instance.getLanguage().getMessage("damages-mode"), instance.getSettings().getSetting("protection-message"));
                    }
                }
            }
        }

    }
    
    /**
     * Handles vehicle enter events to prevent players from entering restricted vehicles in claims.
     * @param event the vehicle enter event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onVehicleEnter(VehicleEnterEvent event) {
        Entity entity = event.getEntered();
        if (entity instanceof Player) {
            Player player = (Player) entity;
            if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
            WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
            Entity vehicle = event.getVehicle();
            EntityType vehicleType = vehicle.getType();
            if(!instance.getSettings().isRestrictedEntityType(vehicleType)) return;
        	Chunk chunk = vehicle.getLocation().getChunk();
            if (instance.getMain().checkIfClaimExists(chunk)) {
            	Claim claim = instance.getMain().getClaim(chunk);
            	if(claim.getPermissionForPlayer("Entities", player)) return;
                event.setCancelled(true);
                instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities"), instance.getSettings().getSetting("protection-message"));
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Entities")) {
                event.setCancelled(true);
                instance.getMain().sendMessage(player,instance.getLanguage().getMessage("entities-mode"), instance.getSettings().getSetting("protection-message"));
            }
        }
    }
    
    /**
     * Handles block change events to prevent trampling of farmland in claims.
     * @param event the entity change block event.
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onBlockChange(EntityChangeBlockEvent event) {
        Entity entity = event.getEntity();
        Block block = event.getBlock();

        if (entity.getType() == EntityType.PLAYER && block.getType() == Material.FARMLAND) {
            Player player = (Player) entity;
            Chunk chunk = block.getLocation().getChunk();
            WorldMode mode = instance.getSettings().getWorldMode(player.getLocation().getWorld().getName());
            if (instance.getMain().checkIfClaimExists(chunk)) {
            	Claim claim = instance.getMain().getClaim(chunk);
            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
                if(!claim.getPermissionForPlayer("Destroy", player)) {
                	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy"), instance.getSettings().getSetting("protection-message"));
                    event.setCancelled(true);
                }
            } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Destroy")) {
                event.setCancelled(true);
                instance.getMain().sendMessage(player,instance.getLanguage().getMessage("destroy-mode"), instance.getSettings().getSetting("protection-message"));
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
    	WorldMode mode = instance.getSettings().getWorldMode(pistonChunk.getWorld().getName());
    	if(retractOrNot) {
	        for (Block block : blocks) {
	        	Chunk chunk = block.getLocation().getChunk();
	            if (!chunk.equals(pistonChunk)) {
	                if (instance.getMain().checkIfClaimExists(chunk)) {
	                	if(instance.getMain().getOwnerInClaim(pistonChunk).equals(instance.getMain().getOwnerInClaim(chunk))) return true;
	                	if(!instance.getMain().canPermCheck(chunk, "Redstone", "Natural")) {
	                		return false;
	                	}
	                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Redstone")) {
	                	return false;
	                }
	            }
	        }
	        return true;
    	}
        for (Block block : blocks) {
            Chunk chunk = block.getRelative(direction).getLocation().getChunk();
            if (!chunk.equals(pistonChunk)) {
                if (instance.getMain().checkIfClaimExists(chunk)) {
                	if(instance.getMain().getOwnerInClaim(pistonChunk).equals(instance.getMain().getOwnerInClaim(chunk))) return true;
                	if(!instance.getMain().canPermCheck(chunk, "Redstone", "Natural")) {
                		return false;
                	}
                } else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("Redstone")) {
                	return false;
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
        if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
        Claim claim = instance.getMain().getClaim(chunk);
        if(!claim.getPermissionForPlayer("Damages", player)) {
            event.setCancelled(true);
            instance.getMain().sendMessage(player, instance.getLanguage().getMessage("damages"), instance.getSettings().getSetting("protection-message"));
        }
    }
    
    /**
     * Handles weather settings for the player.
     *
     * @param player The player.
     * @param chunk  The chunk.
     */
    private void handleWeatherSettings(Player player, Chunk to, Chunk from) {
    	Claim claimTo = instance.getMain().getClaim(to);
    	Claim claimFrom = instance.getMain().getClaim(from);
        if (instance.getMain().checkIfClaimExists(to) && !claimTo.getPermissionForPlayer("Weather",player)) {
            player.setPlayerWeather(WeatherType.CLEAR);
        } else if (instance.getMain().checkIfClaimExists(from) && !claimFrom.getPermissionForPlayer("Weather",player)) {
            player.resetPlayerWeather();
        }
    }
    
    /**
     * Handles auto fly functionality for the player.
     *
     * @param player  The player.
     * @param cPlayer The custom player object.
     * @param chunk   The chunk.
     * @param owner   The owner of the chunk.
     */
    private void handleAutoFly(Player player, CPlayer cPlayer, Chunk chunk, String owner) {
    	Claim claim = instance.getMain().getClaim(chunk);
        if (cPlayer.getClaimAutofly() && (owner.equals(player.getName()) || claim != null && claim.getPermissionForPlayer("Fly", player)) && !instance.isFolia()) {
            instance.getPlayerMain().activePlayerFly(player);
            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-enabled"), "CHAT");
            }
        } else if (claim != null && !claim.getPermissionForPlayer("Fly", player) && !owner.equals(player.getName()) && cPlayer.getClaimFly() && !instance.isFolia()) {
            instance.getPlayerMain().removePlayerFly(player);
            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-disabled"), "CHAT");
            }
        }
    }
    
    /**
     * Handles auto del chunk functionality.
     *
     * @param player The player.
     * @param cPlayer The custom player object.
     * @param chunk The chunk.
     * @param world The world name.
     */
    private void handleAutoDelChunk(Player player, CPlayer cPlayer, Chunk chunk, String world) {
        if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
            player.sendMessage(instance.getLanguage().getMessage("autodelchunk-world-disabled").replace("%world%", world));
            cPlayer.setClaimAuto("");
        } else {
        	Claim claim = cPlayer.getTargetClaimChunk();
        	if(claim == null) return;
            if(claim.getChunks().size() == 1) {
            	player.sendMessage(instance.getLanguage().getMessage("cannot-remove-only-remaining-chunk"));
            	return;
            }
			Set<Chunk> chunks = new HashSet<>(claim.getChunks());
			if(!chunks.contains(chunk)) {
				player.sendMessage(instance.getLanguage().getMessage("chunk-not-in-claim"));
				return;
			}
			chunks.remove(chunk);
            if(!instance.getMain().isAnyChunkAdjacent(chunks, chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("one-chunk-must-be-adjacent-delchunk"));
            	return;
            }
            instance.getMain().removeClaimChunk(claim, chunk)
	        	.thenAccept(success -> {
	        		if (success) {
	        			String chunk_string = world+";"+String.valueOf(chunk.getX())+";"+String.valueOf(chunk.getZ());
	        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+chunk_string+"]").replace("%claim-name%", claim.getName())));
	        		} else {
	        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error-delete-chunk")));
	        		}
	        	})
	            .exceptionally(ex -> {
	                ex.printStackTrace();
	                return null;
	            });
        }
    }
    
    /**
     * Handles auto add chunk functionality.
     *
     * @param player The player.
     * @param cPlayer The custom player object.
     * @param chunk The chunk.
     * @param world The world name.
     */
    private void handleAutoAddChunk(Player player, CPlayer cPlayer, Chunk chunk, String world) {
        if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
            player.sendMessage(instance.getLanguage().getMessage("autoaddchunk-world-disabled").replace("%world%", world));
            cPlayer.setClaimAuto("");
        } else {
        	String playerName = player.getName();
        	Claim claim = cPlayer.getTargetClaimChunk();
        	if(claim == null) return;
            if(instance.getMain().checkIfClaimExists(chunk)) {
            	Claim claim_target = instance.getMain().getClaim(chunk);
            	if(claim_target.getOwner().equalsIgnoreCase(playerName)) {
            		if(claim_target.equals(claim)) {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-already-in-claim")
            					.replace("%claim-name%", claim.getName()));
            			return;
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-already-owner")
            					.replace("%claim-name%", claim.getName())
            					.replace("%claim-name-1%", claim_target.getName()));
            			return;
            		}
            	} else {
            		player.sendMessage(instance.getLanguage().getMessage("add-chunk-not-owner"));
            		return;
            	}
            }
            Set<Chunk> chunks = new HashSet<>(claim.getChunks());
            if(!cPlayer.canClaimTotalWithNumber(instance.getMain().getAllChunksFromAllClaims(playerName).size()+1)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
            	return;
            }
            if(!cPlayer.canClaimWithNumber(chunks.size()+1)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
            	return;
            }
            chunks.add(chunk);
            if(!instance.getMain().areChunksInSameWorld(chunks)) {
            	player.sendMessage(instance.getLanguage().getMessage("chunks-must-be-from-same-world"));
            	return;
            }
            chunks.remove(chunk);
            if(!instance.getMain().isAnyChunkAdjacent(chunks, chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("one-chunk-must-be-adjacent"));
            	return;
            }
            // Check if there is chunk near
            instance.getMain().isAreaClaimFree(chunk, cPlayer.getClaimDistance(), playerName)
            	.thenAccept(successs -> {
            		if (successs) {
            			double[] price = {0};
                        if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("chunk-cost")) {
                            price[0] = instance.getSettings().getBooleanSetting("chunk-cost-multiplier") ? cPlayer.getChunkMultipliedCost(chunks.size()) : cPlayer.getChunkCost();
                            double balance = instance.getVault().getPlayerBalance(playerName);

                            if (balance < price[0]) {
                            	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", instance.getMain().getPrice(String.valueOf((double) Math.round((price[0] - balance)*100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
                                return;
                            }
                            instance.getVault().removePlayerBalance(playerName, price[0]);
                            if (price[0] > 0) instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("you-paid-chunk").replace("%price%", instance.getMain().getPrice(String.valueOf((double) Math.round(price[0] * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
                        }
                        instance.getMain().addClaimChunk(claim, chunk)
                        	.thenAccept(success -> {
                        		if (success) {
                        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("add-chunk-successful")
                        					.replace("%chunk%", "["+chunk.getWorld().getName()+";"+String.valueOf(chunk.getX())+";"+String.valueOf(chunk.getZ())+"]")
                        					.replace("%claim-name%", claim.getName())));
                        			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, new CustomSet<>(claim.getChunks()), true, false);
                        			return;
                        		} else {
                        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
                        		}
                        	})
                            .exceptionally(ex -> {
                                ex.printStackTrace();
                                return null;
                            });
            		} else {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cannot-claim-because-claim-near")));
                    	return;
            		}
            	})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        }
    }
    
    /**
     * Handles auto unclaim functionality.
     *
     * @param player The player.
     * @param cPlayer The custom player object.
     * @param chunk The chunk.
     * @param world The world name.
     */
    private void handleAutoUnclaim(Player player, CPlayer cPlayer, Chunk chunk, String world) {
        if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
            player.sendMessage(instance.getLanguage().getMessage("autounclaim-world-disabled").replace("%world%", world));
            cPlayer.setClaimAuto("");
        } else {
        	
        	if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            
            if (owner.equals("*") && player.hasPermission("scs.admin")) {
            	instance.getMain().deleteClaim(claim)
            		.thenAccept(success -> {
            			if (success) {
            				player.sendMessage(instance.getLanguage().getMessage("delete-claim-protected-area"));
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
            
            if (!owner.equals(player.getName())) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            
            instance.getMain().deleteClaim(claim)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("territory-delete-success"));
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("error"));
            		}
            	})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        }
    }
    
    /**
     * Handles auto claim functionality.
     *
     * @param player The player.
     * @param cPlayer The custom player object.
     * @param chunk The chunk.
     * @param world The world name.
     */
    private void handleAutoClaim(Player player, CPlayer cPlayer, Chunk chunk, String world) {
        if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
            player.sendMessage(instance.getLanguage().getMessage("autoclaim-world-disabled").replace("%world%", world));
            cPlayer.setClaimAuto("");
        } else {
        	String playerName = player.getName();
        	// Check if the chunk is already claimed
            if (instance.getMain().checkIfClaimExists(chunk)) {
            	instance.getMain().handleClaimConflict(player, chunk);
            	return;
            }
            
            // Check if there is chunk near
            if(!instance.getMain().isAreaClaimFree(chunk, cPlayer.getClaimDistance(), playerName).join()) {
            	player.sendMessage(instance.getLanguage().getMessage("cannot-claim-because-claim-near"));
            	return;
            }
            
            // Check if the player can claim
            if (!cPlayer.canClaim()) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore"));
                return;
            }
            
            // Check if the player can pay
            if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
                double price = instance.getSettings().getBooleanSetting("claim-cost-multiplier") ? cPlayer.getMultipliedCost() : cPlayer.getCost();
                double balance = instance.getVault().getPlayerBalance(playerName);

                if (balance < price) {
                	player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", instance.getMain().getPrice(String.valueOf((double) Math.round((price - balance)*100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
                    return;
                }

                instance.getVault().removePlayerBalance(playerName, price);
                if (price > 0) player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replace("%price%", instance.getMain().getPrice(String.valueOf((double) Math.round(price * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
            }
            
            // Create claim
            instance.getMain().createClaim(player, chunk)
            	.thenAccept(success -> {
            		if (success) {
            			int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
            			player.sendMessage(instance.getLanguage().getMessage("create-claim-success").replace("%remaining-claims%", instance.getMain().getNumberSeparate(String.valueOf(remainingClaims))));
            			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, new CustomSet<>(Set.of(chunk)), true, false);
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("error"));
            		}
            	})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
        }
    }

    /**
     * Handles auto map functionality.
     *
     * @param player The player.
     * @param cPlayer The custom player object.
     * @param chunk The chunk.
     * @param world The world name.
     */
    private void handleAutoMap(Player player, CPlayer cPlayer, Chunk chunk, String world) {
        if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
            player.sendMessage(instance.getLanguage().getMessage("automap-world-disabled").replace("%world%", world));
            cPlayer.setClaimAutomap(false);
        } else {
            instance.getMain().getMap(player, chunk, true);
        }
    }
    
    /**
     * Handles enter and leave messages.
     *
     * @param player The player.
     * @param to The chunk the player is moving to.
     * @param from The chunk the player is moving from.
     * @param ownerTO The owner of the chunk the player is moving to.
     * @param ownerFROM The owner of the chunk the player is moving from.
     */
    private void handleEnterLeaveMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
        if (instance.getSettings().getBooleanSetting("enter-leave-messages")) {
            enterleaveMessages(player, to, from, ownerTO, ownerFROM);
        }
        if (instance.getSettings().getBooleanSetting("enter-leave-chat-messages")) {
            enterleaveChatMessages(player, to, from, ownerTO, ownerFROM);
        }
        if (instance.getSettings().getBooleanSetting("enter-leave-title-messages")) {
            enterleavetitleMessages(player, to, from, ownerTO, ownerFROM);
        }
    }

    /**
     * Sends the claim enter message to the player (chat).
     *
     * @param player the player.
     * @param to the chunk the player is entering.
     * @param from the chunk the player is leaving.
     * @param ownerTO the owner of the chunk the player is entering.
     * @param ownerFROM the owner of the chunk the player is leaving.
     */
    private void enterleaveChatMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
        String playerName = player.getName();
        String toName = instance.getMain().getClaimNameByChunk(to);
        String fromName = instance.getMain().getClaimNameByChunk(from);

        if (instance.getMain().checkIfClaimExists(to)) {
        	Claim claim = instance.getMain().getClaim(to);
        	String message;
        	if(claim.getSale() && instance.getSettings().getBooleanSetting("announce-sale.chat")) {
                message = ownerTO.equals("*")
                        ? instance.getLanguage().getMessage("enter-protected-area-for-sale-chat")
                        		.replace("%name%", toName)
                        		.replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                        		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))
                        : instance.getLanguage().getMessage("enter-territory-for-sale-chat")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName)
                  		  .replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                  		  .replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
        	} else {
                message = ownerTO.equals("*")
                        ? instance.getLanguage().getMessage("enter-protected-area-chat").replace("%name%", toName)
                        : instance.getLanguage().getMessage("enter-territory-chat")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName);
        	}

            instance.executeEntitySync(player, () -> player.sendMessage(message));
            return;
        }

        if (instance.getMain().checkIfClaimExists(from)) {
            String message = ownerFROM.equals("*")
                    ? instance.getLanguage().getMessage("leave-protected-area").replace("%name%", fromName)
                    : instance.getLanguage().getMessage("leave-territory")
                      .replace("%owner%", ownerFROM)
                      .replace("%player%", playerName)
                      .replace("%name%", fromName);
            instance.executeEntitySync(player, () -> player.sendMessage(message));
        }
    }


    /**
     * Sends the claim enter message to the player (action bar).
     *
     * @param player the player.
     * @param to the chunk the player is entering.
     * @param from the chunk the player is leaving.
     * @param ownerTO the owner of the chunk the player is entering.
     * @param ownerFROM the owner of the chunk the player is leaving.
     */
    private void enterleaveMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
        String playerName = player.getName();
        String toName = instance.getMain().getClaimNameByChunk(to);
        String fromName = instance.getMain().getClaimNameByChunk(from);

        if (instance.getMain().checkIfClaimExists(to)) {
        	Claim claim = instance.getMain().getClaim(to);
        	String message;
        	if(claim.getSale() && instance.getSettings().getBooleanSetting("announce-sale.actionbar")) {
        		message = ownerTO.equals("*")
                    ? instance.getLanguage().getMessage("enter-protected-area-for-sale")
                    		.replace("%name%", toName)
                    		.replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                    		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))
                    : instance.getLanguage().getMessage("enter-territory-for-sale")
                      .replace("%owner%", ownerTO)
                      .replace("%player%", playerName)
                      .replace("%name%", toName)
              		  .replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
              		  .replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
        	} else {
        		message = ownerTO.equals("*")
                        ? instance.getLanguage().getMessage("enter-protected-area").replace("%name%", toName)
                        : instance.getLanguage().getMessage("enter-territory")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName);
        	}
            instance.executeEntitySync(player, () -> instance.getMain().sendMessage(player, message, "ACTION_BAR"));
            return;
        }

        if (instance.getMain().checkIfClaimExists(from)) {
            String message = ownerFROM.equals("*")
                    ? instance.getLanguage().getMessage("leave-protected-area").replace("%name%", fromName)
                    : instance.getLanguage().getMessage("leave-territory")
                      .replace("%owner%", ownerFROM)
                      .replace("%player%", playerName)
                      .replace("%name%", fromName);
            instance.executeEntitySync(player, () -> instance.getMain().sendMessage(player, message, "ACTION_BAR"));
        }
    }

    /**
     * Sends the claim enter message to the player (title).
     *
     * @param player the player.
     * @param to the chunk the player is entering.
     * @param from the chunk the player is leaving.
     * @param ownerTO the owner of the chunk the player is entering.
     * @param ownerFROM the owner of the chunk the player is leaving.
     */
    private void enterleavetitleMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
        String toName = instance.getMain().getClaimNameByChunk(to);
        String fromName = instance.getMain().getClaimNameByChunk(from);
        String playerName = player.getName();
        
        if (instance.getMain().checkIfClaimExists(to)) {
        	Claim claim = instance.getMain().getClaim(to);
        	String toTitleKey;
        	String toSubtitleKey;
        	if(claim.getSale() && instance.getSettings().getBooleanSetting("announce-sale.title")) {
            	toTitleKey = ownerTO.equals("*") ? instance.getLanguage().getMessage("enter-protected-area-for-sale-title")
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName)
                		.replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                  		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))
            			: instance.getLanguage().getMessage("enter-territory-for-sale-title")
                	        .replace("%name%", toName)
                	        .replace("%owner%", ownerTO)
                	        .replace("%player%", playerName)
                    		.replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                      		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
            	toSubtitleKey = ownerTO.equals("*") ? instance.getLanguage().getMessage("enter-protected-area-for-sale-subtitle")
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName)
                		.replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                  		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))
            	        : instance.getLanguage().getMessage("enter-territory-for-sale-subtitle")
                	        .replace("%name%", toName)
                	        .replace("%owner%", ownerTO)
                	        .replace("%player%", playerName)
                    		.replace("%price%", instance.getMain().getPrice(String.valueOf(claim.getPrice())))
                      		.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"));
        	} else {
            	toTitleKey = ownerTO.equals("*") ? instance.getLanguage().getMessage("enter-protected-area-title")
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName)
            			: instance.getLanguage().getMessage("enter-territory-title")
                	        .replace("%name%", toName)
                	        .replace("%owner%", ownerTO)
                	        .replace("%player%", playerName);;
            	toSubtitleKey = ownerTO.equals("*") ? instance.getLanguage().getMessage("enter-protected-area-subtitle")
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName)
            	        : instance.getLanguage().getMessage("enter-territory-subtitle")
                	        .replace("%name%", toName)
                	        .replace("%owner%", ownerTO)
                	        .replace("%player%", playerName);
        	}

        	instance.executeEntitySync(player, () -> player.sendTitle(toTitleKey, toSubtitleKey, 5, 25, 5));
            return;
        }
        
        if (instance.getMain().checkIfClaimExists(from)) {
        	String fromTitleKey = ownerFROM.equals("*") ? "leave-protected-area-title" : "leave-territory-title";
        	String fromSubtitleKey = ownerFROM.equals("*") ? "leave-protected-area-subtitle" : "leave-territory-subtitle";

        	String title = instance.getLanguage().getMessage(fromTitleKey)
        	        .replace("%name%", fromName)
        	        .replace("%owner%", ownerFROM)
        	        .replace("%player%", playerName);
        	String subtitle = instance.getLanguage().getMessage(fromSubtitleKey)
        	        .replace("%name%", fromName)
        	        .replace("%owner%", ownerFROM)
        	        .replace("%player%", playerName);
        	instance.executeEntitySync(player, () -> player.sendTitle(title, subtitle, 5, 25, 5));
        }
    }
    
    /**
     * Cancels the teleport event and sends a message to the player.
     *
     * @param event   The player teleport event.
     * @param player  The player.
     * @param message The message key to send.
     */
    private void cancelTeleport(ProjectileHitEvent event, Player player, String message) {
    	event.getEntity().remove();
    	event.setCancelled(true);
        instance.getMain().sendMessage(player, instance.getLanguage().getMessage(message), instance.getSettings().getSetting("protection-message"));
    }
    
}
