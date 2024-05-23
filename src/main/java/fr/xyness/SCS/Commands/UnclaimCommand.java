package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Listeners.ClaimEventsEnterLeave;

public class UnclaimCommand implements CommandExecutor,TabCompleter {
	
	@Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if(sender instanceof Player) {
        	Player player = (Player) sender;
        	if(!player.hasPermission("scs.command.unclaim")) return completions;
	        if (args.length == 1) {
	        	completions.add("*");
	        	completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
	        	return completions;
	        }
        }
        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        
        if(!player.hasPermission("scs.command.unclaim")) {
        	sender.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
        	return false;
        }
        
        if(ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
        	player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
        	return true;
        }
        
        if (args.length > 1) {
        	player.sendMessage(ClaimLanguage.getMessage("help-unclaim").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")));
            return true;
        }
        
        if (args.length == 1) {
        	if(args[0].equals("*")) {
        		if(!player.hasPermission("scs.command.unclaim.*")) {
        			player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
        			return true;
        		}
        		if(cPlayer.getClaimsCount() == 0) {
        			player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim"));
        			return true;
        		}
        		ClaimMain.deleteAllClaim(player);
        		return true;
        	}
    		Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[0]);
    		if(chunk == null) {
    			if(!player.hasPermission("scs.command.claim.radius")) {
        			player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
        			return true;
        		}
        		try {
        			int radius = Integer.parseInt(args[0]);
        			if(!cPlayer.canRadiusClaim(radius)) {
        				player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim"));
        				return true;
        			}
        			List<Chunk> chunks = ClaimCommand.getChunksInRadius(player.getLocation(),radius);
        			Set<Chunk> toDeletePlayer = new HashSet<>();
        			Set<Chunk> toDeleteAdmin = new HashSet<>();
        			int i = 0;
        			for(Chunk c : chunks) {
        				if(ClaimMain.getOwnerInClaim(c).equals("admin") && player.hasPermission("csc.admin")) {
        					toDeleteAdmin.add(c);
        					for(Entity e : c.getEntities()) {
        						if(!(e instanceof Player)) continue;
        						Player p = (Player) e;
        						ClaimEventsEnterLeave.disableBossBar(p);
        					}
        					i++;
        					continue;
        				}
        				if(ClaimMain.getOwnerInClaim(c).equals(player.getName())) {
        					toDeletePlayer.add(c);
        					for(Entity e : c.getEntities()) {
        						if(!(e instanceof Player)) continue;
        						Player p = (Player) e;
        						ClaimEventsEnterLeave.disableBossBar(p);
        					}
        					i++;
        				}
        			}
        			if(player.hasPermission("scs.admin") && !toDeleteAdmin.isEmpty()) ClaimMain.deleteClaimRadius(player, toDeleteAdmin);
        			if(!toDeletePlayer.isEmpty()) ClaimMain.deleteClaimRadius(player, toDeletePlayer);
        			if(i == chunks.size()) {
            			player.sendMessage(ClaimLanguage.getMessage("territory-delete-radius-success").replace("%number%", String.valueOf(i)));
            			return true;
        			}
        			player.sendMessage(ClaimLanguage.getMessage("territory-delete-error").replace("%number%", String.valueOf(i)).replace("%number-max%", String.valueOf(chunks.size())));
        			return true;
        		} catch(NumberFormatException e){
        			player.sendMessage(ClaimLanguage.getMessage("help-command.unclaim-unclaim").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")));
                    return true;
        		}
    		}
			if(ClaimMain.deleteClaim(player, chunk)) {
				for(Entity e : chunk.getEntities()) {
					if(!(e instanceof Player)) continue;
					Player p = (Player) e;
					ClaimEventsEnterLeave.disableBossBar(p);
				}
				player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
				return true;
			}
			player.sendMessage(ClaimLanguage.getMessage("error"));
	        return true;
        }
        
		Chunk chunk = player.getLocation().getChunk();
		String owner = ClaimMain.getOwnerInClaim(chunk);
		
		if(!ClaimMain.checkIfClaimExists(chunk)) {
			player.sendMessage(ClaimLanguage.getMessage("free-territory"));
			return true;
		}
		
		if(owner.equals("admin") && player.hasPermission("csc.admin")) {
			if(ClaimMain.deleteClaim(player, chunk)) {
				for(Entity e : chunk.getEntities()) {
					if(!(e instanceof Player)) continue;
					Player p = (Player) e;
					ClaimEventsEnterLeave.disableBossBar(p);
				}
				player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
				return true;
			}
			player.sendMessage(ClaimLanguage.getMessage("error"));
	        return true;
		}
		
		if(!owner.equals(player.getName())) {
			player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
			return true;
		}
		
		if(ClaimMain.deleteClaim(player, chunk)) {
			for(Entity e : chunk.getEntities()) {
				if(!(e instanceof Player)) continue;
				Player p = (Player) e;
				ClaimEventsEnterLeave.disableBossBar(p);
			}
			player.sendMessage(ClaimLanguage.getMessage("territory-delete-success"));
			return true;
		}
		
		player.sendMessage(ClaimLanguage.getMessage("error"));
        return true;
    }
}
