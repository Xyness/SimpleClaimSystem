package fr.xyness.SCS.Listeners;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

/**
 * This class handles events related to players entering and leaving claims.
 * It implements the Listener interface to handle various player events.
 */
public class ClaimEventsEnterLeave implements Listener {

	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** A map to store the BossBars for each player. */
    private Map<Player, BossBar> bossBars = new HashMap<>();
    
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
        instance.getPlayerMain().refreshPlayerHead(player);
        if (player.hasPermission("scs.admin")) {
            if (instance.isUpdateAvailable()) {
                player.sendMessage(instance.getUpdateMessage());
            }
        }
        Chunk chunk = player.getLocation().getChunk();
        handleWeatherSettings(player, chunk, chunk);
        
        if (!instance.getMain().checkIfClaimExists(chunk)) return;

        String playerName = player.getName();
        Claim claim = instance.getMain().getClaim(chunk);
        if (instance.getMain().checkBan(claim, playerName) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.ban")) {
            instance.getMain().teleportPlayer(player, Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
        }
        
        if (!claim.isMember(playerName) && !claim.getPermission("Visitors") && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.visitors")) {
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
        instance.getPlayerMain().removeCPlayer(player.getName());
        if (bossBars.containsKey(player)) bossBars.remove(player);
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
        if (!instance.getMain().checkIfClaimExists(to)) return;

        Player player = event.getPlayer();
        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
        if(cPlayer == null) return;
        
        String ownerTO = instance.getMain().getOwnerInClaim(to);
        String ownerFROM = instance.getMain().getOwnerInClaim(from);
        
        instance.getBossBars().activeBossBar(player, to);
        handleAutoFly(player, cPlayer, to, ownerTO);
        handleWeatherSettings(player, to, from);
        
        String world = player.getWorld().getName();

        if (!ownerTO.equals(ownerFROM)) {
            handleEnterLeaveMessages(player, to, from, ownerTO, ownerFROM);
            if (cPlayer.getClaimAutoclaim()) {
                handleAutoClaim(player, cPlayer, to, world);
            }
        }

        if (cPlayer.getClaimAutomap()) {
        	handleAutoMap(player, cPlayer, to, world);
        }
        
        if (!instance.getMain().checkIfClaimExists(to)) return;

        Claim claim = instance.getMain().getClaim(to);
        if (instance.getMain().checkBan(claim, playerName) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.ban")) {
            cancelTeleport(event, player, "player-banned");
            return;
        }
        
        if (!claim.isMember(playerName) && !claim.getPermission("Visitors") && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.visitors")) {
            cancelTeleport(event, player, "visitors");
            return;
        }

        if (isTeleportBlocked(event, player, claim)) {
            cancelTeleport(event, player, "teleportations");
            return;
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
        String playerName = player.getName();
        
        Chunk to = event.getRespawnLocation().getChunk();
        String ownerTO = instance.getMain().getOwnerInClaim(to);
        
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
        if(cPlayer == null) return;
        
        String world = player.getWorld().getName();
        
        handleWeatherSettings(player, to, null);
        instance.getBossBars().activeBossBar(player, to);
        handleAutoFly(player, cPlayer, to, ownerTO);

        if (cPlayer.getClaimAutoclaim()) {
            handleAutoClaim(player, cPlayer, to, world);
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
        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
        if(cPlayer == null) return;
        String ownerTO = instance.getMain().getOwnerInClaim(to);
        String ownerFROM = instance.getMain().getOwnerInClaim(from);

        Claim claim = instance.getMain().getClaim(to);
        if(claim != null) {
	        if (instance.getMain().checkBan(claim, playerName) && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.ban")) {
	        	instance.getMain().teleportPlayer(player, event.getFrom());
	            instance.getMain().sendMessage(player, instance.getLanguage().getMessage("player-banned"), instance.getSettings().getSetting("protection-message"));
	            return;
	        }
	        if (!claim.isMember(playerName) && !claim.getPermission("Visitors") && !instance.getPlayerMain().checkPermPlayer(player, "scs.bypass.visitors")) {
	        	instance.getMain().teleportPlayer(player, event.getFrom());
	        	instance.getMain().sendMessage(player, instance.getLanguage().getMessage("visitors"), instance.getSettings().getSetting("protection-message"));
	            return;
	        }
        }

        handleWeatherSettings(player,to,from);

        if (cPlayer.getClaimAutofly() && (ownerTO.equals(playerName) || instance.getMain().canPermCheck(to, "Fly")) && !instance.isFolia()) {
            instance.getPlayerMain().activePlayerFly(player);
            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-enabled"), "CHAT");
            }
        } else if (!instance.getMain().canPermCheck(to, "Fly") && !ownerTO.equals(playerName) && cPlayer.getClaimFly() && !instance.isFolia()) {
            instance.getPlayerMain().removePlayerFly(player);
            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-disabled"), "CHAT");
            }
        }

        instance.getBossBars().activeBossBar(player, to);

        String world = player.getWorld().getName();

        if (cPlayer.getClaimAutoclaim()) {
            handleAutoClaim(player, cPlayer, to, world);
        }

        if (cPlayer.getClaimAutomap()) {
            handleAutoMap(player, cPlayer, to, world);
        }

        if (!ownerTO.equals(ownerFROM)) {
            handleEnterLeaveMessages(player, to, from, ownerTO, ownerFROM);
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
        event.setCancelled(true);
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
        if (!instance.getPlayerMain().checkPermPlayer(player, "scs.bypass") && !instance.getMain().checkMembre(claim, player) && !claim.getPermission("Teleportations")) {
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
        if (instance.getMain().checkIfClaimExists(to) && !instance.getMain().canPermCheck(to, "Weather")) {
            player.setPlayerWeather(WeatherType.CLEAR);
        } else if (instance.getMain().checkIfClaimExists(from) && !instance.getMain().canPermCheck(from, "Weather")) {
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
        if (cPlayer.getClaimAutofly() && (owner.equals(player.getName()) || instance.getMain().canPermCheck(chunk, "Fly")) && !instance.isFolia()) {
            instance.getPlayerMain().activePlayerFly(player);
            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-enabled"), "CHAT");
            }
        } else if (!instance.getMain().canPermCheck(chunk, "Fly") && !owner.equals(player.getName()) && cPlayer.getClaimFly() && !instance.isFolia()) {
            instance.getPlayerMain().removePlayerFly(player);
            if (instance.getSettings().getBooleanSetting("claim-fly-message-auto-fly")) {
                instance.getMain().sendMessage(player, instance.getLanguage().getMessage("fly-disabled"), "CHAT");
            }
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
        if (instance.getSettings().isWorldDisabled(world)) {
            player.sendMessage(instance.getLanguage().getMessage("autoclaim-world-disabled").replaceAll("%world%", world));
            cPlayer.setClaimAutoclaim(false);
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
                	player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replaceAll("%missing-price%", String.valueOf(price - balance)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
                    return;
                }

                instance.getVault().removePlayerBalance(playerName, price);
                if (price > 0) player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replaceAll("%price%", String.valueOf(price)).replaceAll("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
            }
            
            // Create claim
            instance.getMain().createClaim(player, chunk)
            	.thenAccept(success -> {
            		if (success) {
            			int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
            			player.sendMessage(instance.getLanguage().getMessage("create-claim-success").replaceAll("%remaining-claims%", String.valueOf(remainingClaims)));
            			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, Set.of(chunk), true, false);
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
        if (instance.getSettings().isWorldDisabled(world)) {
            player.sendMessage(instance.getLanguage().getMessage("automap-world-disabled").replaceAll("%world%", world));
            cPlayer.setClaimAutomap(false);
        } else {
            instance.getMain().getMap(player, chunk);
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
    	instance.executeAsync(() -> {
            String playerName = player.getName();
            String toName = instance.getMain().getClaimNameByChunk(to);
            String fromName = instance.getMain().getClaimNameByChunk(from);

            if (instance.getMain().checkIfClaimExists(to)) {
                String message = ownerTO.equals("admin")
                        ? instance.getLanguage().getMessage("enter-protected-area").replace("%name%", toName)
                        : instance.getLanguage().getMessage("enter-territory")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName);
                instance.executeEntitySync(player, () -> player.sendMessage(message));
                return;
            }

            if (instance.getMain().checkIfClaimExists(from)) {
                String message = ownerFROM.equals("admin")
                        ? instance.getLanguage().getMessage("leave-protected-area").replace("%name%", fromName)
                        : instance.getLanguage().getMessage("leave-territory")
                          .replace("%owner%", ownerFROM)
                          .replace("%player%", playerName)
                          .replace("%name%", fromName);
                instance.executeEntitySync(player, () -> player.sendMessage(message));
            }
    	});
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
    	instance.executeAsync(() -> {
            String playerName = player.getName();
            String toName = instance.getMain().getClaimNameByChunk(to);
            String fromName = instance.getMain().getClaimNameByChunk(from);

            if (instance.getMain().checkIfClaimExists(to)) {
                String message = ownerTO.equals("admin")
                        ? instance.getLanguage().getMessage("enter-protected-area").replace("%name%", toName)
                        : instance.getLanguage().getMessage("enter-territory")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName);
                instance.executeEntitySync(player, () -> instance.getMain().sendMessage(player, message, "ACTION_BAR"));
                return;
            }

            if (instance.getMain().checkIfClaimExists(from)) {
                String message = ownerFROM.equals("admin")
                        ? instance.getLanguage().getMessage("leave-protected-area").replace("%name%", fromName)
                        : instance.getLanguage().getMessage("leave-territory")
                          .replace("%owner%", ownerFROM)
                          .replace("%player%", playerName)
                          .replace("%name%", fromName);
                instance.executeEntitySync(player, () -> instance.getMain().sendMessage(player, message, "ACTION_BAR"));
            }
    	});
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
    	instance.executeAsync(() -> {
            String toName = instance.getMain().getClaimNameByChunk(to);
            String fromName = instance.getMain().getClaimNameByChunk(from);
            String playerName = player.getName();
            
            if (instance.getMain().checkIfClaimExists(to)) {
            	String toTitleKey = ownerTO.equals("admin") ? "enter-protected-area-title" : "enter-territory-title";
            	String toSubtitleKey = ownerTO.equals("admin") ? "enter-protected-area-subtitle" : "enter-territory-subtitle";

            	String title = instance.getLanguage().getMessage(toTitleKey)
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName);
            	String subtitle = instance.getLanguage().getMessage(toSubtitleKey)
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName);
            	instance.executeEntitySync(player, () -> player.sendTitle(title, subtitle, 5, 25, 5));
                return;
            }
            
            if (instance.getMain().checkIfClaimExists(from)) {
            	String fromTitleKey = ownerFROM.equals("admin") ? "leave-protected-area-title" : "leave-territory-title";
            	String fromSubtitleKey = ownerFROM.equals("admin") ? "leave-protected-area-subtitle" : "leave-territory-subtitle";

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
    	});
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
