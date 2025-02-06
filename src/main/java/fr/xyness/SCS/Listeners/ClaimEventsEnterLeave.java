package fr.xyness.SCS.Listeners;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.WeatherType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;
import fr.xyness.SCS.Types.WorldMode;

/**
 * This class handles events related to players entering and leaving claims.
 * It implements the Listener interface to handle various player events.
 */
public class ClaimEventsEnterLeave implements Listener {

	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** Set of players when they are rejected from a claim */
	private Set<Player> playersRejected = new HashSet<>();
    
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
    public ClaimEventsEnterLeave(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
    
    // *******************
    // *  EventHandlers  *
    // *******************

    
    /**
     * Handles the player join event. Registers the player, updates the player's BossBar, 
     * and sends an update message if the player has admin permissions.
     *
     * @param event the player join event.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        instance.getPlayerMain().addPlayerPermSetting(player);
        instance.getPlayerMain().checkPlayer(player);
        if (player.hasPermission("scs.admin")) {
            if (instance.isUpdateAvailable()) {
                player.sendMessage(instance.getUpdateMessage());
            }
        }
        Chunk chunk = player.getLocation().getChunk();
        handleWeatherSettings(player, chunk, chunk);
        
        if (!instance.getMain().checkIfClaimExists(chunk)) return;
        
        Claim claim = instance.getMain().getClaim(chunk);
        if (instance.getMain().checkBan(claim, player) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.ban")) {
        	playersRejected.add(player);
            instance.getMain().teleportPlayer(player, Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
        }
        
        if (!claim.getPermissionForPlayer("Enter",player) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.enter")) {
        	playersRejected.add(player);
        	instance.getMain().teleportPlayer(player, Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
        }
        
        instance.getBossBars().activeBossBar(player,chunk);
    }

    /**
     * Handles the player quit event. Clears the player's data and removes their BossBar.
     *
     * @param event the player quit event.
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        player.resetPlayerWeather();
        instance.getPlayerMain().removeCPlayer(player.getUniqueId());
        instance.getMain().clearDataForPlayer(player);
        instance.getBossBars().removePlayer(player);
        if(playersRejected.contains(player)) playersRejected.remove(player);
    }

    /**
     * Handles the player teleport event.
     *
     * @param event The player teleport event.
     */
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Chunk to = event.getTo().getChunk();
        Chunk from = event.getFrom().getChunk();
        Player player = event.getPlayer();
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
	
	        if (isTeleportBlocked(event, player, claim)) {
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

    /**
     * Handles the player respawn event. Updates the player's BossBar and sends enabled messages on respawn.
     *
     * @param event the player respawn event.
     */
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        
        Chunk to = event.getRespawnLocation().getChunk();
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
    }

    /**
     * Handles the player move event. Updates the player's BossBar and sends enabled messages on changing chunk.
     *
     * @param event the player move event.
     */
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (!hasChangedChunk(event)) return;

        Chunk to = event.getTo().getChunk();
        Chunk from = event.getFrom().getChunk();
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerId);
        if(cPlayer == null) return;
        String ownerTO = instance.getMain().getOwnerInClaim(to);
        String ownerFROM = instance.getMain().getOwnerInClaim(from);

        Claim claim = instance.getMain().getClaim(to);
        if(claim != null) {
	        if (instance.getMain().checkBan(claim, player) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.ban")) {
	        	playersRejected.add(player);
	        	instance.getMain().teleportPlayer(player, event.getFrom());
	            instance.getMain().sendMessage(player, instance.getLanguage().getMessage("player-banned"), instance.getSettings().getSetting("protection-message"));
	        	if(instance.getSettings().getBooleanSetting("claim-particles-not-enter")) {
	        		instance.getMain().displayChunksNotEnter(player, new CustomSet<>(claim.getChunks()));
	        	}
	            return;
	        }
	        if (!claim.getPermissionForPlayer("Enter",player) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.enter")) {
	        	playersRejected.add(player);
	        	instance.getMain().teleportPlayer(player, event.getFrom());
	        	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("enter"), instance.getSettings().getSetting("protection-message"));
	        	if(instance.getSettings().getBooleanSetting("claim-particles-not-enter")) {
	        		instance.getMain().displayChunksNotEnter(player, new CustomSet<>(claim.getChunks()));
	        	}
	        	return;
	        }
	        
	        if (cPlayer.getClaimAutofly() && (ownerTO.equals(playerName) || claim.getPermissionForPlayer("Fly",player)) && !instance.isFolia()) {
	            instance.getPlayerMain().activePlayerFly(player);
	            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
	                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-enabled"), "CHAT");
	            }
	        } else if (!claim.getPermissionForPlayer("Fly",player) && !ownerTO.equals(playerName) && cPlayer.getClaimFly() && !instance.isFolia()) {
	            instance.getPlayerMain().removePlayerFly(player);
	            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
	                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-disabled"), "CHAT");
	            }
	        }
        } else {
        	instance.getPlayerMain().removePlayerFly(player);
        }

        handleWeatherSettings(player,to,from);

        instance.getBossBars().activeBossBar(player, to);

        String world = player.getWorld().getName();
        
        if (!ownerTO.equals(ownerFROM)) {
            handleEnterLeaveMessages(player, to, from, ownerTO, ownerFROM);
        }
        
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
    }

    
    // ********************
    // *  Others Methods  *
    // ********************
    
    
    /**
     * Cancels the teleport event and sends a message to the player.
     *
     * @param event   The player teleport event.
     * @param player  The player.
     * @param message The message key to send.
     */
    private void cancelTeleport(PlayerTeleportEvent event, Player player, String message) {
    	if(instance.isFolia()) {
    		instance.executeAsyncLater(() -> instance.getMain().teleportPlayer(player, event.getFrom()), 50);
    	} else {
    		event.setCancelled(true);
    	}
        instance.getMain().sendMessage(player, instance.getLanguage().getMessage(message), instance.getSettings().getSetting("protection-message"));
    }

    /**
     * Checks if the teleport is blocked based on permissions and teleport causes.
     *
     * @param event   The player teleport event.
     * @param player  The player.
     * @param toChunk The destination chunk.
     * @return True if the teleport is blocked, false otherwise.
     */
    private boolean isTeleportBlocked(PlayerTeleportEvent event, Player player, Claim claim) {
        if (!instance.getPlayerMain().checkPermPlayer(player, "scs.bypass") && !claim.getPermissionForPlayer("Teleportations",player)) {
            switch (event.getCause()) {
                case ENDER_PEARL:
                case CHORUS_FRUIT:
                    return true;
                default:
                    return false;
            }
        }
        return false;
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
     * Checks if the player has changed chunk.
     *
     * @param event the player move event.
     * @return true if the player has changed chunk, false otherwise.
     */
    private boolean hasChangedChunk(PlayerMoveEvent event) {
        int fromChunkX = event.getFrom().getChunk().getX();
        int fromChunkZ = event.getFrom().getChunk().getZ();
        int toChunkX = event.getTo().getChunk().getX();
        int toChunkZ = event.getTo().getChunk().getZ();
        return fromChunkX != toChunkX || fromChunkZ != toChunkZ;
    }
}
