package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Listeners.ClaimEventsEnterLeave;

/**
 * Handles the /unclaim command for unclaiming territory.
 */
public class UnclaimCommand implements CommandExecutor, TabCompleter {

    // ******************
    // *  Tab Complete  *
    // ******************

    /**
     * Provides tab completion suggestions for the /unclaim command.
     *
     * @param sender Source of the command
     * @param cmd Command which was executed
     * @param alias Alias of the command which was used
     * @param args Passed command arguments
     * @return A list of tab completion suggestions
     */
	@Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            List<String> completions = new ArrayList<>();

            if (!player.hasPermission("scs.command.unclaim") && !player.hasPermission("scs.admin")) {
                return completions;
            }

            if (args.length == 1) {
                if (player.hasPermission("scs.command.unclaim.all") || player.hasPermission("scs.admin")) {
                    completions.add("*");
                }
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
            }

            return completions;
        });

        try {
            return future.get(); // Return the result from the CompletableFuture
        } catch (ExecutionException | InterruptedException e) {
            return new ArrayList<>();
        }
    }

    // ******************
    // *  Main command  *
    // ******************

    /**
     * Executes the given command, returning its success.
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
        
        if (ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
            player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
            return true;
        }
        
        if (args.length > 1) {
            player.sendMessage(ClaimLanguage.getMessage("help-unclaim").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")));
            return true;
        }
        
        if (args.length == 1) {
            SimpleClaimSystem.executeAsync(() -> {
            	if (args[0].equals("*")) {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.unclaim.all")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (cPlayer.getClaimsCount() == 0) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> ClaimMain.deleteAllClaim(player));
                    return;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[0]);
                if (chunk == null) {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.unclaim.radius")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    try {
                        int radius = Integer.parseInt(args[0]);
                        if (!cPlayer.canRadiusClaim(radius)) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim")));
                            return;
                        }
                        List<Chunk> chunks = ClaimCommand.getChunksInRadius(player.getLocation(), radius);
                        Set<Chunk> toDeletePlayer = new HashSet<>();
                        Set<Chunk> toDeleteAdmin = new HashSet<>();
                        int i = 0;
                        for (Chunk c : chunks) {
                            if (ClaimMain.getOwnerInClaim(c).equals("admin") && player.hasPermission("scs.admin")) {
                                toDeleteAdmin.add(c);
                                i++;
                                continue;
                            }
                            if (ClaimMain.getOwnerInClaim(c).equals(player.getName())) {
                                toDeletePlayer.add(c);
                                i++;
                            }
                        }
                        if (player.hasPermission("scs.admin") && !toDeleteAdmin.isEmpty()) ClaimMain.deleteClaimRadius(player, toDeleteAdmin);
                        if (!toDeletePlayer.isEmpty()) ClaimMain.deleteClaimRadius(player, toDeletePlayer);
                        final String i_f = String.valueOf(i);
                        if (i == chunks.size()) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-delete-radius-success").replace("%number%", i_f)));
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-delete-error").replace("%number%", i_f).replace("%number-max%", String.valueOf(chunks.size()))));
                        return;
                    } catch (NumberFormatException e) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("help-command.unclaim-unclaim").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator"))));
                        return;
                    }
                }
                if (ClaimMain.deleteClaim(player, chunk)) {
                	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-delete-success")));
                    return;
                }
                SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
            });
            
            return true;
        }
        
        SimpleClaimSystem.executeAsync(() -> {
	        Chunk chunk = player.getLocation().getChunk();
	        String owner = ClaimMain.getOwnerInClaim(chunk);
	        
	        if (!ClaimMain.checkIfClaimExists(chunk)) {
	        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
	            return;
	        }
	        
	        if (owner.equals("admin") && player.hasPermission("scs.admin")) {
	            if (ClaimMain.deleteClaim(player, chunk)) {
	            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-delete-success")));
	                return;
	            }
	            SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
	            return;
	        }
	        
	        if (!owner.equals(player.getName())) {
	        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
	            return;
	        }
	        
	        if (ClaimMain.deleteClaim(player, chunk)) {
	        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-delete-success")));
	            return;
	        }
	        
	        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
        });
        
        return true;
    }
}
