package fr.xyness.SCS.Listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAttemptPickupItemEvent;

import com.destroystokyo.paper.event.player.PlayerPostRespawnEvent;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;
import fr.xyness.SCS.Types.WorldMode;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class PaperClaimEvents implements Listener {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimEventsEnterLeave.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public PaperClaimEvents(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
    
    // *******************
    // *  EventHandlers  *
    // *******************
    
    
    /**
     * Handles the player post respawn event.
     * 
     * @param event The PlayerPostRespawnEvent event.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerPostRespawnEvent event) {
    	if(instance.isFolia()) {
        	Player player = event.getPlayer();
            
        	Bukkit.getRegionScheduler().run(instance, event.getRespawnedLocation(), task -> {
                Chunk to = event.getRespawnedLocation().getChunk();
                
                instance.executeSync(() -> {
                    String ownerTO = instance.getMain().getOwnerInClaim(to);
                    
                    CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
                    if(cPlayer == null) return;
                    
                    String world = player.getWorld().getName();
                    
                    handleWeatherSettings(player, to, null);
                    instance.getBossBars().activeBossBar(player, to);
                    handleAutoFly(player, cPlayer, to, ownerTO);

                    if (cPlayer.getClaimAuto().equals("addchunk")) {
                        handleAutoAddChunk(player, cPlayer, to, world);
                    } else if (cPlayer.getClaimAuto().equals("delchunk")) {
                        handleAutoDelChunk(player, cPlayer, to, world);
                    } else if (cPlayer.getClaimAuto().equals("claim")) {
                        handleAutoClaim(player, cPlayer, to, world);
                    } else if (cPlayer.getClaimAuto().equals("unclaim")) {
                        handleAutoUnclaim(player, cPlayer, to, world);
                    }

                    if (cPlayer.getClaimAutomap()) {
                        handleAutoMap(player, cPlayer, to, world);
                    }
                });

        	});

    	}
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
    	if(instance.isFolia()) {
    		Player player = event.getPlayer();
    		Location respawnLocation = player.getRespawnLocation() == null ? player.getWorld().getSpawnLocation() : player.getRespawnLocation();
    		Bukkit.getAsyncScheduler().runAtFixedRate(instance, task -> {
				if(player != null && player.isOnline()) {
					if(!player.isDead()) {
						instance.executeSync(() -> {
				    		PlayerPostRespawnEvent e = new PlayerPostRespawnEvent(player,respawnLocation, false);
				    		Bukkit.getPluginManager().callEvent(e);
						});
						task.cancel();
					}
				} else {
					task.cancel();
				}
    		}, 250, 250, TimeUnit.MILLISECONDS);
    	}
    }
    
	/**
	 * Handles player chat events for claim chat.
	 * 
	 * @param event AsyncPlayerChatEvent event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
		if(cPlayer == null) return;
		if(cPlayer.getClaimChat()) {
			event.setCancelled(true);
			String msg = instance.getLanguage().getMessage("chat-format").replace("%player%",playerName).replace("%message%", PlainTextComponentSerializer.plainText().serialize(event.originalMessage()));
			player.sendMessage(msg);
			for(String p : instance.getMain().getAllMembersWithPlayerParallel(playerName)) {
				Player target = Bukkit.getPlayer(p);
				if(target != null && target.isOnline()) {
					target.sendMessage(msg);
				}
			}
		}
	}
	
    /**
     * Handles player pickup items events to prevent player pickuping in claims.
     * 
     * @param event The PlayerAttemptPickupItemEvent event.
     */
    @EventHandler
    public void onPlayerPickupItem(PlayerAttemptPickupItemEvent event) {
    	Chunk chunk = event.getItem().getLocation().getChunk();
    	Player player = event.getPlayer();
    	WorldMode mode = instance.getSettings().getWorldMode(player.getWorld().getName());
    	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("ItemsPickup", player)) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("itemspickup"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("ItemsPickup")) {
        	event.setCancelled(true);
        	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("itemspickup-mode"), instance.getSettings().getSetting("protection-message"));
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
            double[] price = {0};
            if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("chunk-cost")) {
                price[0] = instance.getSettings().getBooleanSetting("chunk-cost-multiplier") ? cPlayer.getChunkMultipliedCost(chunks.size()) : cPlayer.getChunkCost();
                double balance = instance.getVault().getPlayerBalance(playerName);

                if (balance < price[0]) {
                	instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", instance.getMain().getPrice(String.valueOf((double) Math.round((price[0] - balance)*100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
                    return;
                }
            }
            instance.getVault().removePlayerBalance(playerName, price[0]);
            if (price[0] > 0) instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("you-paid-chunk").replace("%price%", instance.getMain().getPrice(String.valueOf((double) Math.round(price[0] * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
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

}
