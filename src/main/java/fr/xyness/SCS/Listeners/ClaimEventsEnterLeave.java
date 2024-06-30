package fr.xyness.SCS.Listeners;

import java.util.HashMap;
import java.util.Map;

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
    private static Map<Player, BossBar> bossBars = new HashMap<>();

    // ******************
    // *  EventHandler  *
    // ******************

    /**
     * Handles the player join event. Registers the player, updates the player's BossBar, 
     * and sends an update message if the player has admin permissions.
     *
     * @param event the player join event.
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        CPlayerMain.addPlayerPermSetting(player);
        if (player.hasPermission("scs.admin")) {
            if (SimpleClaimSystem.isUpdateAvailable()) {
                player.sendMessage(SimpleClaimSystem.getUpdateMessage());
            }
        }
        Chunk chunk = player.getLocation().getChunk();
        handleWeatherSettings(player, chunk, chunk);
        activeBossBar(player,chunk);
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
        CPlayerMain.removeCPlayer(player.getName());
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
        if (!ClaimMain.checkIfClaimExists(to)) return;

        Player player = event.getPlayer();
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        if(cPlayer == null) return;
        
        String ownerTO = ClaimMain.getOwnerInClaim(to);
        String ownerFROM = ClaimMain.getOwnerInClaim(from);
        
        activeBossBar(player, to);
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
        
        if (!ClaimMain.checkIfClaimExists(to)) return;

        if (ClaimMain.checkBan(to, playerName)) {
            cancelTeleport(event, player, "player-banned");
            return;
        }

        if (isTeleportBlocked(event, player, to)) {
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
        Chunk to = event.getRespawnLocation().getChunk();
        String ownerTO = ClaimMain.getOwnerInClaim(to);
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        if(cPlayer == null) return;
        String world = player.getWorld().getName();
        
        handleWeatherSettings(player, to, null);
        activeBossBar(player, to);
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
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        if(cPlayer == null) return;
        String ownerTO = ClaimMain.getOwnerInClaim(to);
        String ownerFROM = ClaimMain.getOwnerInClaim(from);

        if (ClaimMain.checkBan(to, playerName)) {
            player.teleport(event.getFrom());
            ClaimMain.sendMessage(player, ClaimLanguage.getMessage("player-banned"), "ACTION_BAR");
            return;
        }

        handleWeatherSettings(player,to,from);

        if (cPlayer.getClaimAutofly() && (ownerTO.equals(playerName) || ClaimMain.canPermCheck(to, "Fly")) && !SimpleClaimSystem.isFolia()) {
            CPlayerMain.activePlayerFly(player);
            if (ClaimSettings.getBooleanSetting("claim-fly-message-auto-fly")) {
                ClaimMain.sendMessage(player, ClaimLanguage.getMessage("fly-enabled"), "CHAT");
            }
        } else if (!ClaimMain.canPermCheck(to, "Fly") && !ownerTO.equals(playerName) && cPlayer.getClaimFly() && !SimpleClaimSystem.isFolia()) {
            CPlayerMain.removePlayerFly(player);
            if (ClaimSettings.getBooleanSetting("claim-fly-message-auto-fly")) {
                ClaimMain.sendMessage(player, ClaimLanguage.getMessage("fly-disabled"), "CHAT");
            }
        }

        activeBossBar(player, to);

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
        ClaimMain.sendMessage(player, ClaimLanguage.getMessage(message), ClaimSettings.getSetting("protection-message"));
    }

    /**
     * Checks if the teleport is blocked based on permissions and teleport causes.
     *
     * @param event   The player teleport event.
     * @param player  The player.
     * @param toChunk The destination chunk.
     * @return True if the teleport is blocked, false otherwise.
     */
    private boolean isTeleportBlocked(PlayerTeleportEvent event, Player player, Chunk toChunk) {
        if (!CPlayerMain.checkPermPlayer(player, "scs.bypass") && !ClaimMain.checkMembre(toChunk, player) && !ClaimMain.canPermCheck(toChunk, "Teleportations")) {
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
        if (ClaimMain.checkIfClaimExists(to) && !ClaimMain.canPermCheck(to, "Weather")) {
            player.setPlayerWeather(WeatherType.CLEAR);
        } else if (ClaimMain.checkIfClaimExists(from) && !ClaimMain.canPermCheck(from, "Weather")) {
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
        if (cPlayer.getClaimAutofly() && (owner.equals(player.getName()) || ClaimMain.canPermCheck(chunk, "Fly")) && !SimpleClaimSystem.isFolia()) {
            CPlayerMain.activePlayerFly(player);
            if (ClaimSettings.getBooleanSetting("claim-fly-message-auto-fly")) {
                ClaimMain.sendMessage(player, ClaimLanguage.getMessage("fly-enabled"), "CHAT");
            }
        } else if (!ClaimMain.canPermCheck(chunk, "Fly") && !owner.equals(player.getName()) && cPlayer.getClaimFly() && !SimpleClaimSystem.isFolia()) {
            CPlayerMain.removePlayerFly(player);
            if (ClaimSettings.getBooleanSetting("claim-fly-message-auto-fly")) {
                ClaimMain.sendMessage(player, ClaimLanguage.getMessage("fly-disabled"), "CHAT");
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
        if (ClaimSettings.isWorldDisabled(world)) {
            player.sendMessage(ClaimLanguage.getMessage("autoclaim-world-disabled").replaceAll("%world%", world));
            cPlayer.setClaimAutoclaim(false);
        } else {
            ClaimMain.createClaim(player, chunk);
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
        if (ClaimSettings.isWorldDisabled(world)) {
            player.sendMessage(ClaimLanguage.getMessage("automap-world-disabled").replaceAll("%world%", world));
            cPlayer.setClaimAutomap(false);
        } else {
            ClaimMain.getMap(player, chunk);
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
        if (ClaimSettings.getBooleanSetting("enter-leave-messages")) {
            enterleaveMessages(player, to, from, ownerTO, ownerFROM);
        }
        if (ClaimSettings.getBooleanSetting("enter-leave-chat-messages")) {
            enterleaveChatMessages(player, to, from, ownerTO, ownerFROM);
        }
        if (ClaimSettings.getBooleanSetting("enter-leave-title-messages")) {
            enterleavetitleMessages(player, to, from, ownerTO, ownerFROM);
        }
    }

    /**
     * Updates the color of all BossBars.
     *
     * @param color the new color for the BossBars.
     */
    public static void setBossBarColor(BarColor color) {
        bossBars.values().forEach(b -> b.setColor(color));
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
    	Runnable task = () -> {
            String playerName = player.getName();
            String toName = ClaimMain.getClaimNameByChunk(to);
            String fromName = ClaimMain.getClaimNameByChunk(from);

            if (ClaimMain.checkIfClaimExists(to)) {
                String message = ownerTO.equals("admin")
                        ? ClaimLanguage.getMessage("enter-protected-area").replace("%name%", toName)
                        : ClaimLanguage.getMessage("enter-territory")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName);
                Runnable subtask = () -> player.sendMessage(message);
                SimpleClaimSystem.executeEntitySync(player, subtask);
                return;
            }

            if (ClaimMain.checkIfClaimExists(from)) {
                String message = ownerFROM.equals("admin")
                        ? ClaimLanguage.getMessage("leave-protected-area").replace("%name%", fromName)
                        : ClaimLanguage.getMessage("leave-territory")
                          .replace("%owner%", ownerFROM)
                          .replace("%player%", playerName)
                          .replace("%name%", fromName);
                Runnable subtask = () -> player.sendMessage(message);
                SimpleClaimSystem.executeEntitySync(player, subtask);
            }
    	};
    	SimpleClaimSystem.executeAsync(task);
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
    	Runnable task = () -> {
            String playerName = player.getName();
            String toName = ClaimMain.getClaimNameByChunk(to);
            String fromName = ClaimMain.getClaimNameByChunk(from);

            if (ClaimMain.checkIfClaimExists(to)) {
                String message = ownerTO.equals("admin")
                        ? ClaimLanguage.getMessage("enter-protected-area").replace("%name%", toName)
                        : ClaimLanguage.getMessage("enter-territory")
                          .replace("%owner%", ownerTO)
                          .replace("%player%", playerName)
                          .replace("%name%", toName);
                Runnable subtask = () -> ClaimMain.sendMessage(player, message, "ACTION_BAR");
                SimpleClaimSystem.executeEntitySync(player, subtask);
                return;
            }

            if (ClaimMain.checkIfClaimExists(from)) {
                String message = ownerFROM.equals("admin")
                        ? ClaimLanguage.getMessage("leave-protected-area").replace("%name%", fromName)
                        : ClaimLanguage.getMessage("leave-territory")
                          .replace("%owner%", ownerFROM)
                          .replace("%player%", playerName)
                          .replace("%name%", fromName);
                Runnable subtask = () -> ClaimMain.sendMessage(player, message, "ACTION_BAR");
                SimpleClaimSystem.executeEntitySync(player, subtask);
            }
    	};
    	SimpleClaimSystem.executeAsync(task);
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
    	Runnable task = () -> {
            String toName = ClaimMain.getClaimNameByChunk(to);
            String fromName = ClaimMain.getClaimNameByChunk(from);
            String playerName = player.getName();
            
            if (ClaimMain.checkIfClaimExists(to)) {
            	String toTitleKey = ownerTO.equals("admin") ? "enter-protected-area-title" : "enter-territory-title";
            	String toSubtitleKey = ownerTO.equals("admin") ? "enter-protected-area-subtitle" : "enter-territory-subtitle";

            	String title = ClaimLanguage.getMessage(toTitleKey)
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName);
            	String subtitle = ClaimLanguage.getMessage(toSubtitleKey)
            	        .replace("%name%", toName)
            	        .replace("%owner%", ownerTO)
            	        .replace("%player%", playerName);

            	Runnable subtask = () -> player.sendTitle(title, subtitle, 5, 25, 5);
            	SimpleClaimSystem.executeEntitySync(player, subtask);
                return;
            }
            
            if (ClaimMain.checkIfClaimExists(from)) {
            	String fromTitleKey = ownerFROM.equals("admin") ? "leave-protected-area-title" : "leave-territory-title";
            	String fromSubtitleKey = ownerFROM.equals("admin") ? "leave-protected-area-subtitle" : "leave-territory-subtitle";

            	String title = ClaimLanguage.getMessage(fromTitleKey)
            	        .replace("%name%", fromName)
            	        .replace("%owner%", ownerFROM)
            	        .replace("%player%", playerName);
            	String subtitle = ClaimLanguage.getMessage(fromSubtitleKey)
            	        .replace("%name%", fromName)
            	        .replace("%owner%", ownerFROM)
            	        .replace("%player%", playerName);

            	Runnable subtask = () -> player.sendTitle(title, subtitle, 5, 25, 5);
            	SimpleClaimSystem.executeEntitySync(player, subtask);
            }
    	};
    	SimpleClaimSystem.executeAsync(task);
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
    
    /**
     * Checks if the player has a BossBar and returns it.
     *
     * @param player the player.
     * @return the player's BossBar.
     */
    public static BossBar checkBossBar(Player player) {
        return bossBars.computeIfAbsent(player, p -> {
            BossBar b = Bukkit.getServer().createBossBar("", BarColor.valueOf(ClaimSettings.getSetting("bossbar-color")), BarStyle.SOLID);
            b.addPlayer(p);
            return b;
        });
    }

    /**
     * Activates the BossBar for the player.
     *
     * @param player the player.
     * @param chunk the chunk the player is in.
     */
    public static void activeBossBar(Player player, Chunk chunk) {
        if (player == null) return;
        Runnable task = () -> {
            BossBar b = checkBossBar(player);

            if (!ClaimSettings.getBooleanSetting("bossbar")) {
                b.setVisible(false);
                return;
            }

            if (!ClaimMain.checkIfClaimExists(chunk)) {
                b.setVisible(false);
                return;
            }

            String owner = ClaimMain.getOwnerInClaim(chunk);
            String chunkName = ClaimMain.getClaimNameByChunk(chunk);
            String title;

            if (owner.equals("admin")) {
                title = ClaimSettings.getSetting("bossbar-protected-area-message").replace("%name%", chunkName);
            } else if (owner.equals(player.getName())) {
                title = ClaimSettings.getSetting("bossbar-owner-message").replace("%owner%", owner).replace("%name%", chunkName);
            } else if (ClaimMain.checkMembre(player.getLocation().getChunk(), player)) {
                title = ClaimSettings.getSetting("bossbar-member-message")
                        .replace("%player%", player.getName())
                        .replace("%owner%", owner)
                        .replace("%name%", chunkName);
            } else {
                title = ClaimSettings.getSetting("bossbar-visitor-message")
                        .replace("%player%", player.getName())
                        .replace("%owner%", owner)
                        .replace("%name%", chunkName);
            }

            b.setTitle(title);
            b.setVisible(true);
        };
        SimpleClaimSystem.executeAsync(task);
    }

    /**
     * Disables the BossBar for the player.
     *
     * @param player the player.
     */
    public static void disableBossBar(Player player) {
        if (!ClaimSettings.getBooleanSetting("bossbar")) return;
        if (player == null) return;
        BossBar b = checkBossBar(player);
        b.setVisible(false);
    }
}
