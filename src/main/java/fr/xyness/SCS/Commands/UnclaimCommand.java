package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;

/**
 * Handles the /unclaim command for unclaiming territory.
 */
public class UnclaimCommand implements CommandExecutor, TabCompleter {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for UnclaimCommand.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public UnclaimCommand(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
    
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
            
            if (args.length == 1) {
                completions.addAll(getPrimaryCompletions(player));
            }

            if (!player.hasPermission("scs.command.unclaim") && !player.hasPermission("scs.admin")) {
                return completions;
            }

            if (args.length == 1) {
                if (player.hasPermission("scs.command.unclaim.all") || player.hasPermission("scs.admin")) {
                    completions.add("*");
                }
                completions.addAll(instance.getMain().getClaimsNameFromOwner(player.getName()));
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
        
    	// If the sender is not a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(instance.getLanguage().getMessage("command-only-by-players"));
            return true;
        }

        // Get data
        Player player = (Player) sender;
        String playerName = player.getName();
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(playerName);
 
        if (instance.getSettings().isWorldDisabled(player.getWorld().getName())) {
        	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", player.getWorld().getName()));
            return false;
        }
        
        if (args.length > 1) {
        	player.sendMessage(instance.getLanguage().getMessage("help-unclaim").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")));
            return false;
        }
        
        if (args.length == 1) {
        	if (args[0].equals("*")) {
                if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim.all")) {
                	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                    return false;
                }
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return false;
                }
                instance.getMain().deleteAllClaims(playerName)
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
                return true;
            }
            Claim claim = instance.getMain().getClaimByName(args[0], playerName);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("help-command.unclaim-unclaim").replace("%help-separator%", instance.getLanguage().getMessage("help-separator")));
                return false;
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
            return true;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        
        if (!instance.getMain().checkIfClaimExists(chunk)) {
        	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
            return false;
        }
        
        Claim claim = instance.getMain().getClaim(chunk);
        String owner = claim.getOwner();
        
        if (owner.equals("admin") && player.hasPermission("scs.admin")) {
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
        	return true;
        }
        
        if (!owner.equals(player.getName())) {
        	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
            return false;
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
        
        return true;
    }
    
    
    // ********************
    // *  Other Methods  *
    // ********************
    
    
    /**
     * Gets the primary completions for the first argument.
     *
     * @param player the player executing the command
     * @return a list of primary completions
     */
    private List<String> getPrimaryCompletions(Player player) {
        List<String> completions = new ArrayList<>();
        if (instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim.all")) {
            completions.add("*");
        }
        completions.addAll(instance.getMain().getClaimsNameFromOwner(player.getName()));
        return completions;
    }
}
