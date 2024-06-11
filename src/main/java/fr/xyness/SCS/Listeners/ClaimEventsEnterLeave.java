package fr.xyness.SCS.Listeners;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.*;

import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;

public class ClaimEventsEnterLeave implements Listener {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private static Map<Player,BossBar> bossBars = new HashMap<>();
	
	
	// ******************
	// *  EventHandler  *
	// ******************
	
	
	// Register the player and update his bossbar
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		CPlayerMain.addPlayerPermSetting(player);
		if(player.hasPermission("scs.admin")) {
			if(SimpleClaimSystem.isUpdateAvailable()) {
				player.sendMessage(SimpleClaimSystem.getUpdateMessage());
			}
		}
		if(!ClaimSettings.getBooleanSetting("bossbar")) return;
		BossBar b = Bukkit.getServer().createBossBar("", BarColor.valueOf(ClaimSettings.getSetting("bossbar-color")), BarStyle.SOLID);
		bossBars.put(player, b);
		b.setVisible(false);
		b.addPlayer(player);
		if(ClaimMain.checkIfClaimExists(player.getLocation().getChunk())){
			String owner = ClaimMain.getOwnerInClaim(player.getLocation().getChunk());
        	if(owner.equals("admin")) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-protected-area-message").replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(player.getLocation().getChunk())));
        		b.setVisible(true);
            	return;
        	}
        	if(owner.equals(player.getName())) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-owner-message").replaceAll("%owner%", owner).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(player.getLocation().getChunk())));
        		b.setVisible(true);
            	return;
        	}
        	if(ClaimMain.checkMembre(player.getLocation().getChunk(), player)) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-member-message").replaceAll("%player%", player.getName()).replaceAll("%owner%", owner).replaceAll("%name%", ClaimMain.getClaimNameByChunk(player.getLocation().getChunk())));
        		b.setVisible(true);
            	return;
        	}
        	String message = ClaimSettings.getSetting("bossbar-visitor-message").replaceAll("%player%", player.getName()).replaceAll("%owner%", owner).replaceAll("%name%", ClaimMain.getClaimNameByChunk(player.getLocation().getChunk()));
        	b.setTitle(message);
        	b.setVisible(true);
        	return;
		}
	}
	
	// Delete the player's bossbar and clear his data
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if(bossBars.containsKey(player)) bossBars.remove(player);
		ClaimGui.removeChunk(player);
		ClaimListGui.removeClaimsChunk(player);
		ClaimListGui.removeClaimsLoc(player);
		ClaimListGui.removeLastChunk(player);
		ClaimMembersGui.removeChunk(player);
		ClaimMembersGui.removeClaimMember(player);
		ClaimsOwnerGui.removeClaimsChunk(player);
		ClaimsOwnerGui.removeClaimsLoc(player);
		ClaimsOwnerGui.removeOwner(player);
		ClaimsOwnerGui.removePlayerFilter(player);
		ClaimsGui.removeOwner(player);
		ClaimsGui.removePlayerFilter(player);
		AdminClaimGui.removeChunk(player);
		AdminClaimListGui.removeClaimsChunk(player);
		AdminClaimListGui.removeClaimsLoc(player);
		AdminClaimListGui.removeLastChunk(player);
	}
	
	// Update his bossbar and send enabled messages on teleport
	@EventHandler
	public void onPlayerTeleport(PlayerTeleportEvent event) {
    	Chunk to = event.getTo().getChunk();
    	if(!ClaimMain.checkIfClaimExists(to)) return;
    	Player player = event.getPlayer();
    	if(ClaimMain.checkBan(to, player)) {
    		event.setCancelled(true);
    		ClaimMain.sendActionBar(player, ClaimLanguage.getMessage("player-banned"));
    		return;
    	}
    	if(!player.hasPermission("csc.bypass") && !ClaimMain.checkMembre(to, player) && !ClaimMain.canPermCheck(to, "Teleportations")) {
    		PlayerTeleportEvent.TeleportCause cause = event.getCause();
            switch (cause) {
                case ENDER_PEARL:
                case CHORUS_FRUIT:
                	ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("teleportations"));
                    event.setCancelled(true);
                    return;
            }
    	}
		Chunk from = event.getFrom().getChunk();
    	String ownerTO = ClaimMain.getOwnerInClaim(to);
    	String ownerFROM = ClaimMain.getOwnerInClaim(from);
    	CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
    	if(cPlayer.getClaimAutomap()) ClaimMain.getMap(player,to);
    	if(ClaimSettings.getBooleanSetting("bossbar")) bossbarMessages(player,to,ownerTO);
		if(!ownerTO.equals(ownerFROM)) {
	    	if(ClaimSettings.getBooleanSetting("enter-leave-messages")) enterleaveMessages(player,to,from,ownerTO,ownerFROM);
	    	if(cPlayer.getClaimAutoclaim()) ClaimMain.createClaim(player, to);
		}
    	
	}
	
	// Update his bossbar and send enabled messages on respawn
	@EventHandler
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Player player = event.getPlayer();
		Chunk to = event.getRespawnLocation().getChunk();
    	String ownerTO = ClaimMain.getOwnerInClaim(to);
    	if(ClaimSettings.getBooleanSetting("bossbar")) bossbarMessages(player,to,ownerTO);
    	CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
    	if(cPlayer.getClaimAutoclaim()) ClaimMain.createClaim(player, to);
    	if(cPlayer.getClaimAutomap()) ClaimMain.getMap(player,to);
	}
	
	// Update his bossbar and send enabled messages on changing chunk
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (hasChangedChunk(event)) {
        	Chunk to = event.getTo().getChunk();
        	Chunk from = event.getFrom().getChunk();
        	String ownerTO = ClaimMain.getOwnerInClaim(to);
        	String ownerFROM = ClaimMain.getOwnerInClaim(from);
        	Player player = event.getPlayer();
        	CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        	if(ClaimMain.checkBan(to, player)) {
        		player.teleport(event.getFrom());
        		ClaimMain.sendActionBar(player, ClaimLanguage.getMessage("player-banned"));
        		return;
        	}
        	if(ClaimSettings.getBooleanSetting("bossbar")) bossbarMessages(player,to,ownerTO);
        	if(cPlayer.getClaimAutoclaim()) ClaimMain.createClaim(player, to);
        	if(cPlayer.getClaimAutomap()) ClaimMain.getMap(player,to);
        	if(ownerTO.equals(ownerFROM)) return;
        	if(ClaimSettings.getBooleanSetting("enter-leave-messages")) enterleaveMessages(player,to,from,ownerTO,ownerFROM);
        	if(ClaimSettings.getBooleanSetting("enter-leave-chat-messages")) enterleaveChatMessages(player,to,from,ownerTO,ownerFROM);
        	if(ClaimSettings.getBooleanSetting("enter-leave-title-messages")) enterleavetitleMessages(player,to,from,ownerTO,ownerFROM);
        }
    }
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
	// Update the color of all bossbars
	public static void setBossBarColor(BarColor color){
		for(BossBar b : bossBars.values()) {
			b.setColor(color);
		}
	}
	
	// Send the claim enter message to the player (chat)
    private void enterleaveChatMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
    	if(ClaimMain.checkIfClaimExists(to)) {
        	if(ownerTO.equals("admin")) {
        		player.sendMessage(ClaimLanguage.getMessage("enter-protected-area-chat").replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)));
            	return;
        	}
        	String message = ClaimLanguage.getMessage("enter-territory-chat").replaceAll("%owner%", ownerTO).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to));
        	player.sendMessage(message);
        	return;
        }
        if(ClaimMain.checkIfClaimExists(from)) {
        	if(ownerFROM.equals("admin")) {
        		player.sendMessage(ClaimLanguage.getMessage("leave-protected-area-chat").replaceAll("%name%", ClaimMain.getClaimNameByChunk(from)));
            	return;
        	}
        	String message = ClaimLanguage.getMessage("leave-territory-chat").replaceAll("%owner%", ownerFROM).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(from));
        	player.sendMessage(message);
        	return;
        }
    }
    
    // Send the claim enter message to the player (action bar)
    private void enterleaveMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
    	if(ClaimMain.checkIfClaimExists(to)) {
        	if(ownerTO.equals("admin")) {
        		ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("enter-protected-area").replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)));
            	return;
        	}
        	String message = ClaimLanguage.getMessage("enter-territory").replaceAll("%owner%", ownerTO).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to));
        	ClaimMain.sendActionBar(player,message);
        	return;
        }
        if(ClaimMain.checkIfClaimExists(from)) {
        	if(ownerFROM.equals("admin")) {
        		ClaimMain.sendActionBar(player,ClaimLanguage.getMessage("leave-protected-area").replaceAll("%name%", ClaimMain.getClaimNameByChunk(from)));
            	return;
        	}
        	String message = ClaimLanguage.getMessage("leave-territory").replaceAll("%owner%", ownerFROM).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(from));
        	ClaimMain.sendActionBar(player,message);
        	return;
        }
    }
    
    // Send the claim enter message to the player (title)
    private void enterleavetitleMessages(Player player, Chunk to, Chunk from, String ownerTO, String ownerFROM) {
    	if(ClaimMain.checkIfClaimExists(to)) {
        	if(ownerTO.equals("admin")) {
        		player.sendTitle(ClaimLanguage.getMessage("enter-protected-area-title").replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)), ClaimLanguage.getMessage("enter-protected-area-subtitle").replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)), 5, 25, 5);
            	return;
        	}
        	player.sendTitle(ClaimLanguage.getMessage("enter-territory-title").replaceAll("%owner%", ownerTO).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)), ClaimLanguage.getMessage("enter-territory-subtitle").replaceAll("%owner%", ownerTO).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)), 5, 25, 5);
        	return;
        }
        if(ClaimMain.checkIfClaimExists(from)) {
        	if(ownerFROM.equals("admin")) {
        		player.sendTitle(ClaimLanguage.getMessage("leave-protected-area-title").replaceAll("%name%", ClaimMain.getClaimNameByChunk(from)), ClaimLanguage.getMessage("leave-protected-area-subtitle").replaceAll("%name%", ClaimMain.getClaimNameByChunk(from)), 5, 25, 5);
            	return;
        	}
        	player.sendTitle(ClaimLanguage.getMessage("leave-territory-title").replaceAll("%owner%", ownerFROM).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(from)), ClaimLanguage.getMessage("leave-territory-subtitle").replaceAll("%owner%", ownerFROM).replaceAll("%player%", player.getName()).replaceAll("%name%", ClaimMain.getClaimNameByChunk(from)), 5, 25, 5);
        	return;
        }
    }
    
    // Method to check if the player has a bossbar
    public static BossBar checkBossBar(Player player) {
		BossBar b;
		if(bossBars.containsKey(player)) {
			b = bossBars.get(player);
			return b;
		}
		b = Bukkit.getServer().createBossBar("", BarColor.valueOf(ClaimSettings.getSetting("bossbar-color")), BarStyle.SOLID);
		bossBars.put(player, b);
		b.addPlayer(player);
		return b;
    }
    
    // Update the bossbar message
    public static void bossbarMessages(Player player, Chunk to, String ownerTO) {
    	if(!ClaimSettings.getBooleanSetting("bossbar")) return;
    	if(ClaimMain.checkIfClaimExists(to)) {
    		BossBar b = checkBossBar(player);
        	if(ownerTO.equals("admin")) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-protected-area-message").replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)));
        		b.setVisible(true);
            	return;
        	}
        	if(ownerTO.equals(player.getName())) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-owner-message").replaceAll("%owner%", ownerTO).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)));
        		b.setVisible(true);
            	return;
        	}
        	if(ClaimMain.checkMembre(to, player)) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-member-message").replaceAll("%player%", player.getName()).replaceAll("%owner%", ownerTO).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to)));
        		b.setVisible(true);
            	return;
        	}
        	String message = ClaimSettings.getSetting("bossbar-visitor-message").replaceAll("%player%", player.getName()).replaceAll("%owner%", ownerTO).replaceAll("%name%", ClaimMain.getClaimNameByChunk(to));
        	b.setTitle(message);
        	b.setVisible(true);
        	return;
        }
    	bossBars.get(player).setVisible(false);;
    	return;
    }

    // Check if the player has changed chunk
    private boolean hasChangedChunk(PlayerMoveEvent event) {
        int fromChunkX = event.getFrom().getChunk().getX();
        int fromChunkZ = event.getFrom().getChunk().getZ();
        int toChunkX = event.getTo().getChunk().getX();
        int toChunkZ = event.getTo().getChunk().getZ();
        return fromChunkX != toChunkX || fromChunkZ != toChunkZ;
    }
    
    // Method to active the bossbar of a player
    public static void activeBossBar(Player player, Chunk chunk) {
    	if(!ClaimSettings.getBooleanSetting("bossbar")) return;
    	if(player == null) return;
    	if(ClaimMain.checkIfClaimExists(chunk)) {
    		BossBar b = checkBossBar(player);
    		String owner = ClaimMain.getOwnerInClaim(chunk);
        	if(owner.equals("admin")) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-protected-area-message").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)));
        		b.setVisible(true);
            	return;
        	}
        	if(owner.equals(player.getName())) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-owner-message").replaceAll("%owner%", owner).replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)));
        		b.setVisible(true);
            	return;
        	}
        	if(ClaimMain.checkMembre(player.getLocation().getChunk(), player)) {
        		b.setTitle(ClaimSettings.getSetting("bossbar-member-message").replaceAll("%player%", player.getName()).replaceAll("%owner%", owner).replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)));
        		b.setVisible(true);
            	return;
        	}
        	String message = ClaimSettings.getSetting("bossbar-visitor-message").replaceAll("%player%", player.getName()).replaceAll("%owner%", owner).replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk));
        	b.setTitle(message);
        	b.setVisible(true);
        	return;
        }
    }
    
    // Method to disable the bossbar of a player
    public static void disableBossBar(Player player) {
    	if(!ClaimSettings.getBooleanSetting("bossbar")) return;
    	if(player == null) return;
    	BossBar b = checkBossBar(player);
    	b.setVisible(false);
    }
	
}