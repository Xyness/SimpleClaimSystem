package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.UnclaimConfirmationGui;
import fr.xyness.SCS.Guis.Bedrock.BUnclaimConfirmationGui;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.WorldMode;

/**
 * Handles the /unclaim command to unclaim territory.
 */
public class UnclaimCommand implements CommandExecutor, TabCompleter {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** A map of players currently in the process of deleting a claim. */
    public static Map<Player, String> isOnDelete = new HashMap<>();
	
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
                completions.addAll(getPrimaryCompletions(player, args));
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
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
 
        String world = player.getWorld().getName();
        if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
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
                if (instance.getSettings().getBooleanSetting("claim-confirmation")) {
                    if (isOnDelete.containsKey(player)) {
                        isOnDelete.remove(player);
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
                    } else {
                        isOnDelete.put(player,"*");
                        if(instance.getSettings().getBooleanSetting("floodgate")) {
                        	if(FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                        		new BUnclaimConfirmationGui(player, instance);
                        		return true;
                        	}
                        }
                        new UnclaimConfirmationGui(player, instance);
                    }
                } else {
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
                }
                return true;
            }
            Claim claim = instance.getMain().getClaimByName(args[0], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return false;
            }
            if (instance.getSettings().getBooleanSetting("claim-confirmation")) {
                if (isOnDelete.containsKey(player)) {
                    isOnDelete.remove(player);
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
                } else {
                    isOnDelete.put(player,claim.getName());
                    if(instance.getSettings().getBooleanSetting("floodgate")) {
                    	if(FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                    		new BUnclaimConfirmationGui(player, instance);
                    		return true;
                    	}
                    }
                    new UnclaimConfirmationGui(player, instance);
                }
            } else {
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
            return true;
        }
        
        Chunk chunk = player.getLocation().getChunk();
        
        if (!instance.getMain().checkIfClaimExists(chunk)) {
        	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
            return false;
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
        	return true;
        }
        
        if (!owner.equals(player.getName())) {
        	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
            return false;
        }
        
        if (instance.getSettings().getBooleanSetting("claim-confirmation")) {
            if (isOnDelete.containsKey(player)) {
                isOnDelete.remove(player);
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
            } else {
                isOnDelete.put(player,claim.getName());
                if(instance.getSettings().getBooleanSetting("floodgate")) {
                	if(FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
                		new BUnclaimConfirmationGui(player, instance);
                		return true;
                	}
                }
                new UnclaimConfirmationGui(player, instance);
            }
        } else {
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

        
        return true;
    }
    
    
    // ********************
    // *  Other Methods  *
    // ********************
    
    
    /**
     * Gets the primary completions for the first argument.
     *
     * @param player the player executing the command
     * @param args the args of the command
     * @return a list of primary completions
     */
    private List<String> getPrimaryCompletions(Player player, String[] args) {
    	String partialInput = args.length > 0 ? args[0].toLowerCase() : "";
        List<String> completions = new ArrayList<>();
        if (instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim.all")) {
            completions.add("*");
        }
        completions.addAll(instance.getMain().getClaimsNameFromOwner(player.getName()));
        return completions.stream()
    	        .filter(c -> c.toLowerCase().startsWith(partialInput))
    	        .collect(Collectors.toList());
    }
}
