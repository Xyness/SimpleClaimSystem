package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimChunksGui;
import fr.xyness.SCS.Guis.ClaimSettingsGui;
import fr.xyness.SCS.Guis.ClaimListGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Guis.ClaimMainGui;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

/**
 * Command executor and tab completer for the /claim command.
 */
public class ClaimCommand implements CommandExecutor, TabCompleter {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** A set of players currently in the process of creating a claim. */
    private static Set<Player> isOnCreate = new HashSet<>();
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimCommand.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimCommand(SimpleClaimSystem instance) {
    	this.instance = instance;
    }

    
    // ******************
    // *  Tab Complete  *
    // ******************

    
    /**
     * Handles tab completion for the /claim command.
     *
     * @param sender the sender of the command
     * @param cmd the command
     * @param alias the alias of the command
     * @param args the arguments provided to the command
     * @return a list of possible completions for the final argument, or an empty list if none are applicable
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim")) {
            return Collections.emptyList();
        }

        String playerName = player.getName();
        Chunk chunk = player.getLocation().getChunk();

        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                completions.addAll(getPrimaryCompletions(player));
            } else if (args.length == 2) {
                completions.addAll(getSecondaryCompletions(player, args[0], playerName, chunk));
            } else if (args.length == 3) {
                completions.addAll(getTertiaryCompletions(player, args[0], args[1], playerName));
            }

            return completions;
        });

        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return Collections.emptyList();
        }
    }
    
    
    // ******************
    // *  Main command  *
    // ******************

    
    /**
     * Handles the execution of the /claim command.
     *
     * @param sender  the command sender
     * @param command the command
     * @param label   the command label
     * @param args    the command arguments
     * @return true if the command was successful, false otherwise
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

        // Check if for desc (so there are many arguments)
        if (args.length > 1 && args[0].equals("setdesc")) {
        	handleDesc(player, playerName, args);
            return true;
        }
        
        // Switch for args
        switch(args.length) {
        	case 0:
        		handleArgZero(player,playerName,cPlayer);
        		break;
        	case 1:
        		handleArgOne(player,playerName,cPlayer,args);
        		break;
        	case 2:
        		handleArgTwo(player,playerName,cPlayer,args);
        		break;
        	case 3:
        		handleArgThree(player,playerName,cPlayer,args);
        		break;
        	default:
        		instance.getMain().getHelp(player, args[0], "claim");
        		break;
        }
        return true;
    }
    
    
    // ********************
    // *  Other Methods  *
    // ********************
    
    
    /**
     * Handles the command for description
     */
    private void handleDesc(Player player, String playerName, String[] args) {
        if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.setdesc")) {
        	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
            return;
        }
        if (instance.getMain().getClaimsNameFromOwner(playerName).contains(args[1])) {
            String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (description.length() > Integer.parseInt(instance.getSettings().getSetting("max-length-claim-description"))) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-description-too-long"));
                return;
            }
            if (!description.matches("^[a-zA-Z0-9]+$")) {
            	player.sendMessage(instance.getLanguage().getMessage("incorrect-characters-description"));
            	return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            instance.getMain().setClaimDescription(claim, description)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("claim-set-description-success").replace("%name%", args[1]).replace("%description%", description));
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
        player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
        return;
    }
    
    /**
     * Handles the command with three arguments for the given player.
     *
     * @param player the player executing the command
     * @param playerName The name of the player
     * @param cPlayer The cPlayer for player
     * @param args The args for the command
     */
    private void handleArgThree(Player player, String playerName, CPlayer cPlayer, String[] args) {
    	if (args[0].equalsIgnoreCase("delchunk")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.delchunk")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            if(claim.getChunks().size() == 1) {
            	player.sendMessage(instance.getLanguage().getMessage("cannot-remove-only-remaining-chunk"));
            	return;
            }
            String[] parts = args[2].split(";");
            World world = Bukkit.getWorld(parts[0]);
            if(world == null) {
            	player.sendMessage(instance.getLanguage().getMessage("world-does-not-exist"));
            	return;
            }
            instance.getMain().removeClaimChunk(claim, parts[0]+";"+parts[1]+";"+parts[2])
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+args[2]+"]").replace("%claim-name%", claim.getName()));
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("error-delete-chunk"));
            		}
            	})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            return;
    	}
    	if (args[0].equalsIgnoreCase("merge")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.merge")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Set<String> claimsName = instance.getMain().getClaimsNameFromOwner(playerName);
            if (!claimsName.contains(args[1])) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
            	return;
            }
            Set<Claim> claims = new HashSet<>();
            if(args[2].contains(";")) {
            	for(String c : args[2].split(";")) {
            		if(!claimsName.contains(c)) {
                    	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                    	return;
            		}
            		claims.add(instance.getMain().getClaimByName(c, player));
            	}
            } else {
                if (!claimsName.contains(args[2])) {
                	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                	return;
                }
                Claim claim2 = instance.getMain().getClaimByName(args[2], player);
                claims.add(claim2);
            }
            Claim claim1 = instance.getMain().getClaimByName(args[1], player);
            if(claims.contains(claim1)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-merge-same-claim"));
            	return;
            }
            for(Claim claim : claims) {
            	if(!instance.getMain().isAnyChunkAdjacentBetweenSets(claim1.getChunks(), claim.getChunks())) {
            		player.sendMessage(instance.getLanguage().getMessage("one-chunk-of-claim-must-be-adjacent"));
            		return;
            	}
            }
            Set<Chunk> chunks = new HashSet<>(claim1.getChunks());
            claims.forEach(c -> chunks.addAll(c.getChunks()));
            if(!cPlayer.canClaimWithNumber(chunks.size())) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
            	return;
            }
            if(!instance.getMain().areChunksInSameWorld(chunks)) {
            	player.sendMessage(instance.getLanguage().getMessage("chunks-must-be-from-same-world"));
            	return;
            }
            instance.getMain().mergeClaims(claim1, claims)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("claims-are-now-merged").replace("%claim-name%", claim1.getName()));
            			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, claim1.getChunks(), true, false);
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
        if (args[0].equalsIgnoreCase("setname")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.setname")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (instance.getMain().getClaimsNameFromOwner(playerName).contains(args[1])) {
                if (args[2].length() > Integer.parseInt(instance.getSettings().getSetting("max-length-claim-name"))) {
                	player.sendMessage(instance.getLanguage().getMessage("claim-name-too-long"));
                    return;
                }
                if (args[2].contains("claim-")) {
                	player.sendMessage(instance.getLanguage().getMessage("you-cannot-use-this-name"));
                    return;
                }
                if (!args[2].matches("^[a-zA-Z0-9]+$")) {
                	player.sendMessage(instance.getLanguage().getMessage("incorrect-characters-name"));
                	return;
                }
                if (instance.getMain().checkName(player.getUniqueId(), args[2])) {
                	Claim claim = instance.getMain().getClaimByName(args[1], player);
                	instance.getMain().setClaimName(claim, args[2])
                		.thenAccept(success -> {
                			if (success) {
                				player.sendMessage(instance.getLanguage().getMessage("name-change-success").replace("%name%", args[2]));
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
                player.sendMessage(instance.getLanguage().getMessage("error-name-exists").replace("%name%", args[1]));
                return;
            }
            player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
            return;
        }
    	if (args[0].equalsIgnoreCase("kick")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.kick")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (args[1].equalsIgnoreCase("*")) {
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                if(target == null) {
                	player.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
                	return;
                }
	        	if(!instance.getMain().getAllChunksFromAllClaims(playerName).contains(target.getLocation().getChunk())) {
	            	player.sendMessage(instance.getLanguage().getMessage("player-not-in-any-claim").replace("%player%", target.getName()));
	            	return;
	        	}
	            player.sendMessage(instance.getLanguage().getMessage("kick-success-all-claims").replace("%player%", target.getName()));
	            target.sendMessage(instance.getLanguage().getMessage("kicked-from-all-claims").replace("%player%", playerName));
	        	instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
	        	return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if(target == null) {
            	player.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
            	return;
            }
            if(!claim.getChunks().contains(target.getLocation().getChunk())) {
            	player.sendMessage(instance.getLanguage().getMessage("player-not-in-the-claim").replace("%player%", target.getName()).replace("%claim-name%", claim.getName()));
            	return;
            }
            String claimName = claim.getName();
            player.sendMessage(instance.getLanguage().getMessage("kick-success").replace("%player%", target.getName()).replace("%claim-name%", claimName));
            target.sendMessage(instance.getLanguage().getMessage("kicked-from-claim").replace("%player%", playerName).replace("%claim-name%", claimName));
            instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
    	}
        if (args[0].equalsIgnoreCase("ban")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.ban")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (args[1].equalsIgnoreCase("*")) {
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                	player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
                    return;
                }
                String message = instance.getLanguage().getMessage("add-ban-all-success").replace("%player%", targetName);
                instance.getMain().addAllClaimBan(playerName, targetName)
                	.thenAccept(success -> {
                		if (success) {
                            player.sendMessage(message);
            		        if (target != null && target.isOnline()) {
            		        	if(instance.getMain().getAllChunksFromAllClaims(playerName).contains(target.getLocation().getChunk())) {
            		        		instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
            		        	}
            		        	target.sendMessage(instance.getLanguage().getMessage("banned-all-claim-player").replace("%owner%", playerName));
            		        	target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-player").replace("%owner%", playerName));
            		        }
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
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
                return;
            }
            String message = instance.getLanguage().getMessage("add-ban-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().addClaimBan(claim, targetName)
            	.thenAccept(success -> {
            		if (success) {
                        player.sendMessage(message);
        		        if (target != null && target.isOnline()) {
        		        	String claimName = claim.getName();
        		        	if(claim.getChunks().contains(target.getLocation().getChunk())) {
        		        		instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
        		        	}
        		        	target.sendMessage(instance.getLanguage().getMessage("banned-claim-player").replace("%owner%", playerName).replace("%claim-name%", claimName));
        		        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replace("%owner%", playerName).replace("%claim-name%", claimName));
        		        }
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
        if (args[0].equalsIgnoreCase("unban")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (args[1].equalsIgnoreCase("*")) {
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    targetName = otarget == null ? args[2] : otarget.getName();
                } else {
                    targetName = target.getName();
                }
                String message = instance.getLanguage().getMessage("remove-ban-all-success").replace("%player%", targetName);
                instance.getMain().removeAllClaimBan(playerName, targetName)
                	.thenAccept(success -> {
                		if (success) {
                            player.sendMessage(message);
            		        if (target != null && target.isOnline()) {
            		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-all-claim-player").replace("%owner%", playerName));
            		        }
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
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            if (!instance.getMain().checkBan(claim, args[2])) {
                String message = instance.getLanguage().getMessage("not-banned").replace("%player%", args[2]);
                player.sendMessage(message);
                return;
            }
            String targetName = instance.getMain().getRealNameFromClaimBans(claim, args[2]);
            instance.getMain().removeClaimBan(claim, targetName)
            	.thenAccept(success -> {
            		if (success) {
                        String message = instance.getLanguage().getMessage("remove-ban-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
                        player.sendMessage(message);
                        Player target = Bukkit.getPlayer(targetName);
        		        if (target != null && target.isOnline()) {
        		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-claim-player").replace("%owner%", playerName).replace("%claim-name%", claim.getName()));
        		        }
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
        if (args[0].equalsIgnoreCase("add")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.add")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (args[1].equalsIgnoreCase("*")) {
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return;
                }
                for (Claim claim : instance.getMain().getPlayerClaims(playerName)) {
                    if (!instance.getPlayerMain().canAddMember(player, claim)) {
                    	player.sendMessage(instance.getLanguage().getMessage("cant-add-member-anymore"));
                        return;
                    }
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                	player.sendMessage(instance.getLanguage().getMessage("cant-add-yourself"));
                    return;
                }
                String message = instance.getLanguage().getMessage("add-member-success").replace("%player%", targetName).replace("%claim-name%", instance.getLanguage().getMessage("all-your-claims-title"));
                instance.getMain().addAllClaimsMember(playerName, targetName)
                	.thenAccept(success -> {
                		if (success) {
                            player.sendMessage(message);
                            if(target != null && target.isOnline()) {
                            	target.sendMessage(instance.getLanguage().getMessage("add-all-claim-player").replace("%owner%", playerName));
                            }
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
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            if (!instance.getPlayerMain().canAddMember(player, claim)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-add-member-anymore"));
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-add-yourself"));
                return;
            }
            if (instance.getMain().checkMembre(claim, targetName)) {
                String message = instance.getLanguage().getMessage("already-member").replace("%player%", targetName);
                player.sendMessage(message);
                return;
            }
            String message = instance.getLanguage().getMessage("add-member-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().addClaimMember(claim, targetName)
            	.thenAccept(success -> {
            		if (success) {
                        player.sendMessage(message);
                        if(target != null && target.isOnline()) {
                        	target.sendMessage(instance.getLanguage().getMessage("add-claim-player").replace("%claim-name%", claim.getName()).replace("%owner%", playerName));
                        }
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
        if (args[0].equalsIgnoreCase("remove")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (args[1].equalsIgnoreCase("*")) {
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return;
                }
                if (args[2].equals(playerName)) {
                	player.sendMessage(instance.getLanguage().getMessage("cant-remove-owner"));
                    return;
                }
                String targetName = args[2];
                instance.getMain().removeAllClaimsMember(playerName, targetName)
                	.thenAccept(success -> {
                		if (success) {
                            String message = instance.getLanguage().getMessage("remove-member-success").replace("%player%", targetName).replace("%claim-name%", instance.getLanguage().getMessage("all-your-claims-title"));
                            player.sendMessage(message);
                            Player target = Bukkit.getPlayer(targetName);
                            if(target != null && target.isOnline()) {
                            	target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-player").replace("%owner%", playerName));
                            }
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
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            String targetName = args[2];
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-remove-owner"));
                return;
            }
            if (!instance.getMain().checkMembre(claim, targetName)) {
                String message = instance.getLanguage().getMessage("not-member").replace("%player%", targetName);
                player.sendMessage(message);
                return;
            }
            String realName = instance.getMain().getRealNameFromClaimMembers(claim, targetName);
            instance.getMain().removeClaimMember(claim, realName)
            	.thenAccept(success -> {
            		if (success) {
                        String message = instance.getLanguage().getMessage("remove-member-success").replace("%player%", realName).replace("%claim-name%", claim.getName());
                        player.sendMessage(message);
                        Player target = Bukkit.getPlayer(realName);
                        if(target != null && target.isOnline()) {
                        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replace("%claim-name%", claim.getName()).replace("%owner%", playerName));
                        }
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
        if (args[0].equalsIgnoreCase("owner")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.owner")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (args[1].equalsIgnoreCase("*")) {
                if (cPlayer.getClaimsCount() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("player-has-no-claim"));
                    return;
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                	player.sendMessage(instance.getLanguage().getMessage("cant-transfer-ownership-yourself"));
                    return;
                }
                String message = instance.getLanguage().getMessage("setowner-all-success").replace("%owner%", targetName);
            	instance.getMain().setOwner(targetName, instance.getMain().getPlayerClaims(playerName), playerName)
	            	.thenAccept(success -> {
	            		if (success) {
	            			player.sendMessage(message);
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
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-transfer-ownership-yourself"));
                return;
            }
            String message = instance.getLanguage().getMessage("setowner-claim-success").replace("%owner%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().setOwner(targetName, claim)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(message);
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
        if (args[0].equalsIgnoreCase("sell")) {
            if (!instance.getSettings().getBooleanSetting("economy")) {
            	player.sendMessage(instance.getLanguage().getMessage("economy-disabled"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            if (claim.getSale()) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-already-in-sale"));
            	return;
            }
            try {
                Double price = Double.parseDouble(args[2]);
                Double max_price = Double.parseDouble(instance.getSettings().getSetting("max-sell-price"));
                if (price > max_price || price <= 0) {
                	player.sendMessage(instance.getLanguage().getMessage("sell-claim-price-syntax").replace("%max-price%", instance.getMain().getNumberSeparate(instance.getSettings().getSetting("max-sell-price"))));
                    return;
                }
                instance.getMain().setChunkSale(claim, price)
                	.thenAccept(success -> {
                		if (success) {
                            player.sendMessage(instance.getLanguage().getMessage("claim-for-sale-success").replace("%name%", args[1]).replace("%price%", instance.getMain().getNumberSeparate(args[2])).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
                            Bukkit.getOnlinePlayers().stream().forEach(p -> p.sendMessage(instance.getLanguage().getMessage("claim-for-sale-success-broadcast").replace("%name%", args[1]).replace("%price%", instance.getMain().getNumberSeparate(args[2])).replace("%player%", playerName).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))));
                            instance.getBossBars().activateBossBar(claim.getChunks());
                		} else {
                			player.sendMessage(instance.getLanguage().getMessage("error"));
                		}
                	})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
                return;
            } catch (NumberFormatException e) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-price-must-be-number"));
            }
            return;
        }
        instance.getMain().getHelp(player, args[0], "claim");
    }
    
    /**
     * Handles the command with two arguments for the given player.
     *
     * @param player the player executing the command
     * @param playerName The name of the player
     * @param cPlayer The cPlayer for player
     * @param args The args for the command
     */
    private void handleArgTwo(Player player, String playerName, CPlayer cPlayer, String[] args) {
    	if (args[0].equalsIgnoreCase("main")) {
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            new ClaimMainGui(player,claim,instance);
            return;
    	}
    	if (args[0].equalsIgnoreCase("addchunk")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.addchunk")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
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
            instance.getMain().addClaimChunk(claim, chunk)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-successful")
            					.replace("%chunk%", "["+chunk.getWorld().getName()+";"+String.valueOf(chunk.getX())+";"+String.valueOf(chunk.getZ())+"]")
            					.replace("%claim-name%", claim.getName()));
            			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, claim.getChunks(), true, false);
            			return;
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
    	if (args[0].equalsIgnoreCase("kick")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.kick")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if(target == null) {
            	player.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[1]));
            	return;
            }
            if(!claim.getChunks().contains(target.getLocation().getChunk())) {
            	player.sendMessage(instance.getLanguage().getMessage("player-not-in-the-claim").replace("%player%", target.getName()).replace("%claim-name%", claim.getName()));
            	return;
            }
            String claimName = claim.getName();
            player.sendMessage(instance.getLanguage().getMessage("kick-success").replace("%player%", target.getName()).replace("%claim-name%", claimName));
            target.sendMessage(instance.getLanguage().getMessage("kicked-from-claim").replace("%player%", playerName).replace("%claim-name%", claimName));
            instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
            return;
    	}
        if (args[0].equalsIgnoreCase("ban")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.ban")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[1]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
                return;
            }
            String message = instance.getLanguage().getMessage("add-ban-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().addClaimBan(claim, targetName)
            	.thenAccept(success -> {
            		if (success) {
                        player.sendMessage(message);
        		        if (target != null && target.isOnline()) {
        		        	String claimName = claim.getName();
        		        	if(claim.getChunks().contains(target.getLocation().getChunk())) {
        		        		instance.getMain().teleportPlayer(target, Bukkit.getWorlds().get(0).getSpawnLocation());
        		        	}
        		        	target.sendMessage(instance.getLanguage().getMessage("banned-claim-player").replace("%owner%", playerName).replace("%claim-name%", claimName));
        		        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replace("%owner%", playerName).replace("%claim-name%", claimName));
        		        }
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
        if (args[0].equalsIgnoreCase("unban")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.unban")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            if (!instance.getMain().checkBan(claim, args[1])) {
                String message = instance.getLanguage().getMessage("not-banned").replace("%player%", args[1]);
                player.sendMessage(message);
                return;
            }
            String targetName = instance.getMain().getRealNameFromClaimBans(claim, args[1]);
            String message = instance.getLanguage().getMessage("remove-ban-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().removeClaimBan(claim, targetName)
            	.thenAccept(success -> {
            		if (success) {
                        player.sendMessage(message);
                        Player target = Bukkit.getPlayer(targetName);
        		        if (target != null && target.isOnline()) {
        		        	target.sendMessage(instance.getLanguage().getMessage("unbanned-claim-player").replace("%owner%", playerName).replace("%claim-name%", claim.getName()));
        		        }
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
        if (args[0].equalsIgnoreCase("owner")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.owner")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[1]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-transfer-ownership-yourself"));
                return;
            }
            String message = instance.getLanguage().getMessage("setowner-claim-success").replace("%owner%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().setOwner(targetName, claim)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(message);
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("error"));
            		}
            	})
                .exceptionally(ex -> {
                    return null;
                });
            return;
        }
        if (args[0].equalsIgnoreCase("remove")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.remove")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            String targetName = args[1];
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-remove-owner"));
                return;
            }
            if (!instance.getMain().checkMembre(claim, targetName)) {
                String message = instance.getLanguage().getMessage("not-member").replace("%player%", targetName);
                player.sendMessage(message);
                return;
            }
            String realName = instance.getMain().getRealNameFromClaimMembers(claim, targetName);
            String message = instance.getLanguage().getMessage("remove-member-success").replace("%player%", realName).replace("%claim-name%", claim.getName());
            instance.getMain().removeClaimMember(claim, realName)
            	.thenAccept(success -> {
            		if (success) {
                        player.sendMessage(message);
                        Player target = Bukkit.getPlayer(realName);
                        if(target != null && target.isOnline()) {
                        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-player").replace("%claim-name%", claim.getName()).replace("%owner%", playerName));
                        }
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
        if (args[0].equalsIgnoreCase("add")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.add")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            if (!instance.getPlayerMain().canAddMember(player, claim)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-add-member-anymore"));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[1]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
            if (targetName.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-add-yourself"));
                return;
            }
            if (instance.getMain().checkMembre(claim, targetName)) {
                String message = instance.getLanguage().getMessage("already-member").replace("%player%", targetName);
                player.sendMessage(message);
                return;
            }
            String message = instance.getLanguage().getMessage("add-member-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
            instance.getMain().addClaimMember(claim, targetName)
            	.thenAccept(success -> {
            		if (success) {
                        player.sendMessage(message);
                        if(target != null && target.isOnline()) {
                        	target.sendMessage(instance.getLanguage().getMessage("add-claim-player").replace("%claim-name%", claim.getName()).replace("%owner%", playerName));
                        }
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
        if (args[0].equalsIgnoreCase("see")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.see.others")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            String world = player.getWorld().getName();
            if (instance.getSettings().isWorldDisabled(world)) {
            	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", world));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            UUID uuid = null;
            if (target == null) {
                OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[1]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                    return;
                }
                uuid = otarget.getUniqueId();
            } else {
                uuid = target.getUniqueId();
            }
            if (instance.getMain().getPlayerClaimsCount(uuid) == 0) {
            	player.sendMessage(instance.getLanguage().getMessage("target-does-not-have-claim").replace("%name%", args[1]));
                return;
            }
            Set<Chunk> chunks = new HashSet<>();
            instance.getMain().getPlayerClaims(playerName).forEach(c -> c.getChunks().forEach(chunk -> chunks.add(chunk)));
            instance.getMain().displayChunks(player, chunks, false, true);
            return;
        }
        if (args[0].equalsIgnoreCase("settings")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.settings")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            new ClaimSettingsGui(player, claim, instance, "visitors");
            return;
        }
        if (args[0].equalsIgnoreCase("chunks")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.chunks")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            new ClaimChunksGui(player, claim, 1, instance);
            return;
        }
        if (args[0].equalsIgnoreCase("members")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.members")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            cPlayer.setGuiPage(1);
            new ClaimMembersGui(player, claim, 1, instance);
            return;
        }
        if (args[0].equalsIgnoreCase("bans")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.bans")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            cPlayer.setGuiPage(1);
            new ClaimBansGui(player, claim, 1, instance);
            return;
        }
        if (args[0].equalsIgnoreCase("tp")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            instance.getMain().goClaim(player, claim.getLocation());
            return;
        }
        if (args[0].equalsIgnoreCase("cancel")) {
            Claim claim = instance.getMain().getClaimByName(args[1], player);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            if (claim.getSale()) {
            	instance.getMain().delChunkSale(claim)
            		.thenAccept(success -> {
            			if (success) {
            				instance.getBossBars().activateBossBar(claim.getChunks());
            				player.sendMessage(instance.getLanguage().getMessage("claim-in-sale-cancel").replace("%name%", args[1]));
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
            player.sendMessage(instance.getLanguage().getMessage("claim-is-not-in-sale"));
            return;
        }
        instance.getMain().getHelp(player, args[0], "claim");
    }
    
    /**
     * Handles the command with only one argument for the given player.
     *
     * @param player the player executing the command
     * @param playerName The name of the player
     * @param cPlayer The cPlayer for player
     * @param args The args for the command
     */
    private void handleArgOne(Player player, String playerName, CPlayer cPlayer, String[] args) {
    	if (args[0].equalsIgnoreCase("buy")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.buy")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaimFromChunk(chunk);
            if(claim.getOwner().equalsIgnoreCase(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("can-not-buy-your-own-claim"));
            	return;
            }
    		if(claim.getSale()) {
    			String old_owner = claim.getOwner();
    			String old_name = claim.getName();
	            double price = claim.getPrice();
	            double balance = instance.getVault().getPlayerBalance(playerName);
	            // Money checking
	            if (balance < price) {
	            	player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money"));
	                return;
	            }
    			instance.getMain().sellChunk(player, claim)
    				.thenAccept(success -> {
    					if (success) {
        	                player.sendMessage(instance.getLanguage().getMessage("buy-claim-success").replace("%name%", old_name).replace("%price%", String.valueOf(price)).replace("%owner%", old_owner.equalsIgnoreCase("*") ? "protected areas" : old_owner).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
        	                if(!old_owner.equalsIgnoreCase("*")) {
            	                Player target = Bukkit.getPlayer(old_owner);
            	                if(target != null && target.isOnline()) {
            	                	target.sendMessage(instance.getLanguage().getMessage("claim-was-sold").replace("%name%", old_name).replace("%buyer%", playerName).replace("%price%", String.valueOf(price)).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
            	                }
        	                }
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
    		player.sendMessage(instance.getLanguage().getMessage("claim-is-not-in-sale"));
    		return;
    	}
        if (args[0].equalsIgnoreCase("fly")) {
        	if (instance.isFolia()) {
        		player.sendMessage(instance.getLanguage().getMessage("fly-disabled-on-this-server"));
                return;
            }
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.fly")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaimFromChunk(chunk);
            if (claim.getOwner().equals(playerName)) {
                if (cPlayer.getClaimFly()) {
            		instance.getPlayerMain().removePlayerFly(player);
            		player.sendMessage(instance.getLanguage().getMessage("fly-disabled"));
                    return;
                }
                instance.getPlayerMain().activePlayerFly(player);
                player.sendMessage(instance.getLanguage().getMessage("fly-enabled"));
                return;
            }
            if (!claim.getPermissionForPlayer("Fly",player)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-fly-in-this-claim"));
                return;
            }
            if (cPlayer.getClaimFly()) {
        		instance.getPlayerMain().removePlayerFly(player);
        		player.sendMessage(instance.getLanguage().getMessage("fly-disabled"));
                return;
            }
            instance.getPlayerMain().activePlayerFly(player);
            player.sendMessage(instance.getLanguage().getMessage("fly-enabled"));
            return;
        }
        if (args[0].equalsIgnoreCase("autofly")) {
        	if (instance.isFolia()) {
        		player.sendMessage(instance.getLanguage().getMessage("fly-disabled-on-this-server"));
                return;
            }
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.autofly")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (cPlayer.getClaimAutofly()) {
                cPlayer.setClaimAutofly(false);
                player.sendMessage(instance.getLanguage().getMessage("autofly-disabled"));
                return;
            }
            cPlayer.setClaimAutofly(true);
            player.sendMessage(instance.getLanguage().getMessage("autofly-enabled"));
            Chunk chunk = player.getLocation().getChunk();
            if (instance.getMain().checkIfClaimExists(chunk)) {
                Claim claim = instance.getMain().getClaimFromChunk(chunk);
                if (claim.getOwner().equals(playerName) || claim.getPermissionForPlayer("Fly",player)) {
                	if(cPlayer.getClaimFly()) return;
                    instance.getPlayerMain().activePlayerFly(player);
                    player.sendMessage(instance.getLanguage().getMessage("fly-enabled"));	
                    return;
                }
            }
            return;
        }
        if (args[0].equalsIgnoreCase("chat")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.chat")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (cPlayer.getClaimChat()) {
                cPlayer.setClaimChat(false);
                player.sendMessage(instance.getLanguage().getMessage("talking-now-in-public"));
                return;
            }
            cPlayer.setClaimChat(true);
            player.sendMessage(instance.getLanguage().getMessage("talking-now-in-claim"));
            return;
        }
        if (args[0].equalsIgnoreCase("automap")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.automap")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            String world = player.getWorld().getName();
            if (instance.getSettings().isWorldDisabled(world)) {
            	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", world));
                return;
            }
            if (cPlayer.getClaimAutomap()) {
                cPlayer.setClaimAutomap(false);
                player.sendMessage(instance.getLanguage().getMessage("automap-off"));
                return;
            }
            cPlayer.setClaimAutomap(true);
            player.sendMessage(instance.getLanguage().getMessage("automap-on"));
            return;
        }
        if (args[0].equalsIgnoreCase("map")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.map")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            String world = player.getWorld().getName();
            if (instance.getSettings().isWorldDisabled(world)) {
            	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", world));
                return;
            }
            instance.getMain().getMap(player, player.getLocation().getChunk());
            return;
        }
        if (args[0].equalsIgnoreCase("autoclaim")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.autoclaim")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            String world = player.getWorld().getName();
            if (instance.getSettings().isWorldDisabled(world)) {
            	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", world));
                return;
            }
            if (cPlayer.getClaimAutoclaim()) {
                cPlayer.setClaimAutoclaim(false);
                player.sendMessage(instance.getLanguage().getMessage("autoclaim-off"));
                return;
            }
            cPlayer.setClaimAutoclaim(true);
            player.sendMessage(instance.getLanguage().getMessage("autoclaim-on"));
            return;
        }
        if (args[0].equalsIgnoreCase("setspawn")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.setspawn")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            Location l = player.getLocation();
        	instance.getMain().setClaimLocation(claim, l)
        		.thenAccept(success -> {
        			if (success) {
        				player.sendMessage(instance.getLanguage().getMessage("loc-change-success").replace("%coords%", instance.getMain().getClaimCoords(claim)));
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
        if (args[0].equalsIgnoreCase("settings")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.settings")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            new ClaimSettingsGui(player, claim, instance, "visitors");
            return;
        }
        if (args[0].equalsIgnoreCase("chunks")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.chunks")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            new ClaimChunksGui(player, claim, 1, instance);
            return;
        }
        if (args[0].equalsIgnoreCase("members")) {
            if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.members")) {
            	player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            cPlayer.setGuiPage(1);
            new ClaimMembersGui(player, claim, 1, instance);
            return;
        }
        if (args[0].equalsIgnoreCase("bans")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.bans")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if (!instance.getMain().checkIfClaimExists(chunk)) {
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
            Claim claim = instance.getMain().getClaim(chunk);
            String owner = claim.getOwner();
            if (!owner.equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("territory-not-yours"));
                return;
            }
            cPlayer.setGuiPage(1);
            new ClaimBansGui(player, claim, 1, instance);
            return;
        }
        if (args[0].equalsIgnoreCase("list")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.list")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            cPlayer.setGuiPage(1);
            cPlayer.setClaim(null);
            new ClaimListGui(player, 1, "owner", instance);
            return;
        }
        if (args[0].equalsIgnoreCase("see")) {
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.see")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            String world = player.getWorld().getName();
            if (instance.getSettings().isWorldDisabled(world)) {
            	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", world));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            Claim claim = instance.getMain().getClaim(chunk);
            instance.getMain().displayChunks(player, claim == null ? Set.of(player.getLocation().getChunk()) : claim.getChunks(), false, false);
            return;
        }
        try {
            int radius = Integer.parseInt(args[0]);
        	if (!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.radius")) {
        		player.sendMessage(instance.getLanguage().getMessage("cmd-no-permission"));
                return;
            }
            if (instance.getSettings().isWorldDisabled(player.getWorld().getName())) {
            	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", player.getWorld().getName()));
                return;
            }
            if (!cPlayer.canRadiusClaim(radius)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-radius-claim"));
                return;
            }
            getChunksInRadius(player, player.getLocation(), radius, instance).thenAccept(chunks -> {
            	
	            // Check if all claims are free to claim
	            Set<Chunk> chunksToClaim = chunks.stream()
	                    .filter(c -> !instance.getMain().checkIfClaimExists(c))
	                    .collect(Collectors.toSet());
	            if (chunks.size() != chunksToClaim.size()) {
	                player.sendMessage(instance.getLanguage().getMessage("cant-radius-claim-already-claim"));
	                return;
	            }
	            
	            // Check if there is chunk near
	            instance.getMain().isAreaClaimFree(player.getLocation().getChunk(), cPlayer.getClaimDistance()+radius, playerName)
	            	.thenAccept(successs -> {
	            		if (successs) {
	            			// Check if player can claim
	        	            if (!cPlayer.canClaim()) {
	        	                player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore"));
	        	                return;
	        	            }
	        	            
	        	            // Check if player can claim with all these chunks (total)
	        	            if (!cPlayer.canClaimTotalWithNumber(instance.getMain().getAllChunksFromAllClaims(playerName).size()+chunksToClaim.size())) {
	        	                player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
	        	                return;
	        	            }
	        	
	        	            // Check if player can claim with all these chunks
	        	            if (!cPlayer.canClaimWithNumber(chunksToClaim.size())) {
	        	                player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
	        	                return;
	        	            }
	        	
	        	            // Check if player can pay
	        	            double price = 0;
	        	            if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
		        	            price = instance.getMain().calculateClaimPrice(cPlayer, chunksToClaim.size());
		        	            double balance = instance.getVault().getPlayerBalance(playerName);
		        	            if (balance < price) {
		        	                player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round((price - balance)*100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
		        	                return;
		        	            }
	        	            }
	        	            
	                        if (instance.getSettings().getBooleanSetting("claim-confirmation")) {
	                            if (isOnCreate.contains(player)) {
	                                isOnCreate.remove(player);
	                                
	                                // Make player pay
	                                if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
		                	            instance.getVault().removePlayerBalance(playerName, price);
		                	            player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replace("%price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round(price * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
	                                }
		                	            
	                	            // Create claim
	                	            instance.getMain().createClaimRadius(player, chunks, radius)
	        	                    	.thenAccept(success -> {
	        	                    		if (success) {
	        	                	            if (instance.getSettings().getBooleanSetting("claim-particles")) {
	        	                	                instance.getMain().displayChunkBorderWithRadius(player, player.getLocation().getChunk(), radius);
	        	                	            }
	        	                	            Claim claim = instance.getMain().getClaim(player.getLocation().getChunk());
	        	                	            int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
	        	                	            player.sendMessage(instance.getLanguage().getMessage("create-claim-radius-success").replace("%number%", instance.getMain().getNumberSeparate(String.valueOf(chunks.size()))).replace("%remaining-claims%", instance.getMain().getNumberSeparate(String.valueOf(remainingClaims))).replace("%claim-name%", claim.getName()));
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
	                            isOnCreate.add(player);
	                            String AnswerA = instance.getLanguage().getMessage("claim-confirmation-button");
	                            TextComponent AnswerA_C = new TextComponent(AnswerA);
	                            AnswerA_C.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(instance.getLanguage().getMessage("claim-confirmation-button")).create()));
	                            AnswerA_C.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim " + String.valueOf(radius)));
	                            TextComponent finale = new TextComponent(instance.getLanguage().getMessage("claim-confirmation-ask"));
	                            finale.addExtra(AnswerA_C);
	                            player.sendMessage(finale);
	                            return;
	                        }
	                        // Make player pay
	                        if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
		        	            instance.getVault().removePlayerBalance(playerName, price);
		        	            player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replace("%price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round(price * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
	                        }
		        	            
	        	            // Create claim
	        	            instance.getMain().createClaimRadius(player, chunks, radius)
	                        	.thenAccept(success -> {
	                        		if (success) {
	                    	            if (instance.getSettings().getBooleanSetting("claim-particles")) {
	                    	                instance.getMain().displayChunkBorderWithRadius(player, player.getLocation().getChunk(), radius);
	                    	            }
	                    	            Claim claim = instance.getMain().getClaim(player.getLocation().getChunk());
	                    	            int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
	                    	            player.sendMessage(instance.getLanguage().getMessage("create-claim-radius-success").replace("%number%", instance.getMain().getNumberSeparate(String.valueOf(chunks.size()))).replace("%remaining-claims%", instance.getMain().getNumberSeparate(String.valueOf(remainingClaims))).replace("%claim-name%", claim.getName()));
	                        		} else {
	                        			player.sendMessage(instance.getLanguage().getMessage("error"));
	                        		}
	                        	})
	        	                .exceptionally(ex -> {
	        	                    ex.printStackTrace();
	        	                    return null;
	        	                });
	            		} else {
	    	            	player.sendMessage(instance.getLanguage().getMessage("cannot-claim-because-claim-near"));
	    	            	return;
	            		}
	            	})
	                .exceptionally(ex -> {
	                    ex.printStackTrace();
	                    return null;
	                });
            });
            return;
        } catch (NumberFormatException e) {
        	instance.getMain().getHelp(player, args[0], "claim");
        }
    }
    
    /**
     * Handles the command with no arguments for the given player.
     *
     * @param player the player executing the command
     */
    private void handleArgZero(Player player, String playerName, CPlayer cPlayer) {
    	Chunk chunk = player.getLocation().getChunk();
    	if(instance.getMain().checkIfClaimExists(chunk)) {
    		Claim claim = instance.getMain().getClaim(chunk);
    		String owner = claim.getOwner();
            if (owner.equals("*")) {
                player.sendMessage(instance.getLanguage().getMessage("create-error-protected-area"));
            } else if (owner.equals(player.getName())) {
            	if(instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.main")) {
            		new ClaimMainGui(player,claim,instance);
            	} else {
            		player.sendMessage(instance.getLanguage().getMessage("create-already-yours"));
            	}
                
            } else {
                player.sendMessage(instance.getLanguage().getMessage("create-already-claim").replace("%player%", owner));
            }
    		return;
    	}
    	
        String world = player.getWorld().getName();

        if (instance.getSettings().isWorldDisabled(world)) {
            player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", world));
            return;
        }

        if (instance.getSettings().getBooleanSetting("worldguard") && !instance.getWorldGuard().checkFlagClaim(player)) {
            player.sendMessage(instance.getLanguage().getMessage("worldguard-cannot-claim-in-region"));
            return;
        }

        if (instance.getSettings().getBooleanSetting("claim-confirmation")) {
            
            // Check if there is chunk near
            instance.getMain().isAreaClaimFree(chunk, cPlayer.getClaimDistance(), playerName)
            	.thenAccept(successs -> {
            		if (successs) {
            			// Check if the player can claim
                        if (!cPlayer.canClaim()) {
                        	player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore"));
                            return;
                        }
                        
                        // Check if player can claim with all these chunks (total)
                        if (!cPlayer.canClaimTotalWithNumber(instance.getMain().getAllChunksFromAllClaims(playerName).size()+1)) {
                            player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
                            return;
                        }
                        
                        // Check if the player can pay
                        double price = 0;
                        if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
                            price = instance.getSettings().getBooleanSetting("claim-cost-multiplier") ? cPlayer.getMultipliedCost() : cPlayer.getCost();
                            double balance = instance.getVault().getPlayerBalance(playerName);

                            if (balance < price) {
                            	player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round((price - balance)*100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
                                return;
                            }
                        }
                        
                        if (isOnCreate.contains(player)) {
                            isOnCreate.remove(player);
                            
                            // Make the player pay
                            if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
                                instance.getVault().removePlayerBalance(playerName, price);
                                if (price > 0) player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replace("%price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round(price * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
                            }
                            
                            // Create claim
                            instance.getMain().createClaim(player, chunk)
            	            	.thenAccept(success -> {
            	            		if (success) {
            	            			int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
            	            			player.sendMessage(instance.getLanguage().getMessage("create-claim-success").replace("%remaining-claims%", instance.getMain().getNumberSeparate(String.valueOf(remainingClaims))));
            	            			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, Set.of(chunk), true, false);
            	            		} else {
            	            			player.sendMessage(instance.getLanguage().getMessage("error"));
            	            		}
            	            	})
            	                .exceptionally(ex -> {
            	                    ex.printStackTrace();
            	                    return null;
            	                });
                        } else {
                            isOnCreate.add(player);
                            if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, Set.of(player.getLocation().getChunk()), false, false);
                            String confirmationMessage = instance.getLanguage().getMessage("claim-confirmation-ask");
                            TextComponent confirmationComponent = new TextComponent(confirmationMessage);

                            String buttonText = instance.getLanguage().getMessage("claim-confirmation-button");
                            TextComponent buttonComponent = new TextComponent(buttonText);
                            buttonComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(buttonText).create()));
                            buttonComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim"));

                            confirmationComponent.addExtra(buttonComponent);
                            player.sendMessage(confirmationComponent);
                        }
            		} else {
                    	player.sendMessage(instance.getLanguage().getMessage("cannot-claim-because-claim-near"));
                    	return;
            		}
            	})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            return;
        }
        
        // Check if there is chunk near
        instance.getMain().isAreaClaimFree(chunk, cPlayer.getClaimDistance(), playerName)
        		.thenAccept(successs -> {
        			if (successs) {
        		        // Check if the player can claim
        		        if (!cPlayer.canClaim()) {
        		        	player.sendMessage(instance.getLanguage().getMessage("cant-claim-anymore"));
        		            return;
        		        }
        		        
                        // Check if player can claim with all these chunks (total)
                        if (!cPlayer.canClaimTotalWithNumber(instance.getMain().getAllChunksFromAllClaims(playerName).size()+1)) {
                            player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
                            return;
                        }
        		        
        		        // Check if the player can pay
        		        if (instance.getSettings().getBooleanSetting("economy") && instance.getSettings().getBooleanSetting("claim-cost")) {
        		            double price = instance.getSettings().getBooleanSetting("claim-cost-multiplier") ? cPlayer.getMultipliedCost() : cPlayer.getCost();
        		            double balance = instance.getVault().getPlayerBalance(playerName);

        		            if (balance < price) {
        		            	player.sendMessage(instance.getLanguage().getMessage("buy-but-not-enough-money-claim").replace("%missing-price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round((price - balance)*100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
        		                return;
        		            }

        		            instance.getVault().removePlayerBalance(playerName, price);
        		            if (price > 0) player.sendMessage(instance.getLanguage().getMessage("you-paid-claim").replace("%price%", instance.getMain().getNumberSeparate(String.valueOf((double) Math.round(price * 100.0)/100.0))).replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol")));
        		        }
        		        
        		        // Create claim
        		        instance.getMain().createClaim(player, chunk)
        		        	.thenAccept(success -> {
        		        		if (success) {
        		        			int remainingClaims = cPlayer.getMaxClaims() - cPlayer.getClaimsCount();
        		        			player.sendMessage(instance.getLanguage().getMessage("create-claim-success").replace("%remaining-claims%", String.valueOf(remainingClaims)));
        		        			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, Set.of(chunk), true, false);
        		        		} else {
        		        			player.sendMessage(instance.getLanguage().getMessage("error"));
        		        		}
        		        	})
        		            .exceptionally(ex -> {
        		                ex.printStackTrace();
        		                return null;
        		            });
        			} else {
        	        	player.sendMessage(instance.getLanguage().getMessage("cannot-claim-because-claim-near"));
        	        	return;
        			}
        		})
				.exceptionally(ex -> {
				    ex.printStackTrace();
				    return null;
				});
    }
    
    /**
     * Gets the primary completions for the first argument.
     *
     * @param player the player executing the command
     * @return a list of primary completions
     */
    private List<String> getPrimaryCompletions(Player player) {
        List<String> completions = new ArrayList<>();
        String[] commands = {"settings", "add", "remove", "list", "setspawn", "setname", "members", "setdesc",
                "chat", "map", "autoclaim", "automap", "see", "tp", "ban", "unban", "bans", "fly", "autofly", "owner", "merge", "sell", "cancel",
                "main", "delchunk", "addchunk", "chunks", "kick", "buy"};

        for (String command : commands) {
            if (instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim." + command)) {
                completions.add(command);
            }
        }

        return completions;
    }

    /**
     * Gets the secondary completions for the second argument.
     *
     * @param player the player executing the command
     * @param arg the first argument provided to the command
     * @param playerName the name of the player
     * @param chunk the chunk the player is currently in
     * @return a list of secondary completions
     */
    private List<String> getSecondaryCompletions(Player player, String arg, String playerName, Chunk chunk) {
        List<String> completions = new ArrayList<>();
        ClaimMain main = instance.getMain();
        
        if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim."+arg)) return completions;

        switch (arg.toLowerCase()) {
            case "see":
                if (instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.see.others")) {
                    completions.addAll(main.getClaimsOwners());
                }
                break;
            case "merge":
            case "setname":
            case "chat":
            case "setdesc":
            case "settings":
            case "tp":
            case "sell":
            case "main":
            case "addchunk":
            case "delchunk":
            case "chunks":
            case "members":
            case "bans":
                completions.addAll(main.getClaimsNameFromOwner(playerName));
                break;
            case "cancel":
            	completions.addAll(instance.getMain().getClaimsNameInSaleFromOwner(playerName));
            	break;
            case "add":
                if (main.checkIfClaimExists(chunk) && main.getClaim(chunk).getOwner().equals(playerName)) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    completions.remove(playerName);
                }
                completions.add("*");
                completions.addAll(main.getClaimsNameFromOwner(playerName));
                break;
            case "ban":
                if (main.checkIfClaimExists(chunk) && main.getClaim(chunk).getOwner().equals(playerName)) {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                    completions.remove(playerName);
                }
                completions.add("*");
                completions.addAll(main.getClaimsNameFromOwner(playerName));
                break;
            case "unban":
                if (main.checkIfClaimExists(chunk)) {
                    Claim claim = main.getClaim(chunk);
                    if (claim.getOwner().equals(playerName)) {
                        completions.addAll(instance.getMain().convertUUIDSetToStringSet(claim.getBans()));
                    }
                }
                completions.add("*");
                completions.addAll(main.getClaimsNameFromOwner(playerName));
                break;
            case "remove":
                if (main.checkIfClaimExists(chunk)) {
                    Claim claim = main.getClaim(chunk);
                    if (claim.getOwner().equals(playerName)) {
                        completions.addAll(instance.getMain().convertUUIDSetToStringSet(claim.getMembers()));
                        completions.remove(playerName);
                    }
                }
                completions.add("*");
                completions.addAll(main.getClaimsNameFromOwner(playerName));
                break;
            case "owner":
            case "kick":
            	if (main.checkIfClaimExists(chunk) && main.getClaim(chunk).getOwner().equals(playerName)) {
            		completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            		completions.remove(playerName);
            	}
            	completions.addAll(main.getClaimsNameFromOwner(playerName));
            	completions.add("*");
            	break;
            default:
                break;
        }

        return completions;
    }

    /**
     * Gets the tertiary completions for the third argument.
     *
     * @param player the player executing the command
     * @param arg the first argument provided to the command
     * @param arg1 the second argument provided to the command
     * @param playerName the name of the player
     * @return a list of tertiary completions
     */
    private List<String> getTertiaryCompletions(Player player, String arg, String arg1, String playerName) {
        List<String> completions = new ArrayList<>();
        ClaimMain main = instance.getMain();
        
        if(!instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim."+arg)) return completions;
        Claim claim = instance.getMain().getClaimByName(arg1, player);

        switch (arg.toLowerCase()) {
            case "remove":
                if (!arg1.equals("*") && claim != null) {
                    completions.addAll(instance.getMain().convertUUIDSetToStringSet(claim.getMembers()));
                }
                completions.remove(playerName);
                break;
            case "merge":
                completions.addAll(main.getClaimsNameFromOwner(playerName));
                completions.remove(arg1);
                break;
            case "unban":
            	if(claim != null) {
            		completions.addAll(instance.getMain().convertUUIDSetToStringSet(claim.getBans()));
            	}
                break;
            case "add":
            case "ban":
            case "owner":
            case "kick":
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(playerName);
                break;
            case "delchunk":
            	claim = main.getClaimByName(arg1, player);
            	if(claim != null) {
            		completions.addAll(instance.getMain().getStringChunkFromClaim(claim));
            	}
            	break;
            default:
                break;
        }

        return completions;
    }
    
    /**
     * Gets the chunks in a radius around a center location.
     *
     * @param player    the target player
     * @param center    the center location
     * @param radius    the radius in chunks
     * @param instance  the instance of SimpleClaimSystem
     * @return a CompletableFuture containing the set of chunks within the radius
     */
    public static CompletableFuture<Set<Chunk>> getChunksInRadius(Player player, Location center, int radius, SimpleClaimSystem instance) {
        
        player.sendMessage(instance.getLanguage().getMessage("chunk-are-loading")
                .replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(calculateNumberOfChunks(radius)))));

        Set<Chunk> chunks = ConcurrentHashMap.newKeySet(); // Thread-safe set
        World world = center.getWorld();
        Chunk centerChunk = center.getChunk();
        int centerX = centerChunk.getX();
        int centerZ = centerChunk.getZ();
        AtomicInteger loadedChunks = new AtomicInteger(0);
        int totalChunks = calculateNumberOfChunks(radius);

        if (instance.isFolia()) {
            List<CompletableFuture<Void>> futures = IntStream.rangeClosed(centerX - radius, centerX + radius)
                    .parallel()
                    .boxed()
                    .flatMap(x -> IntStream.rangeClosed(centerZ - radius, centerZ + radius)
                            .mapToObj(z -> world.getChunkAtAsync(x, z)
                                    .thenAccept(chunk -> {
                                        chunks.add(chunk);
                                        int count = loadedChunks.incrementAndGet();
                                        if (count % 1000 == 0) {
                                            instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("chunk-while-loading")
                                                    .replace("%loaded-chunks%", instance.getMain().getNumberSeparate(String.valueOf(count)))
                                                    .replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(totalChunks)))));
                                        }
                                    })
                                    .exceptionally(ex -> {
                                        ex.printStackTrace();
                                        return null;
                                    })))
                    .collect(Collectors.toList());

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> chunks);
        } else {
            return CompletableFuture.runAsync(() -> {
                IntStream.rangeClosed(centerX - radius, centerX + radius)
                	.parallel()
                    .boxed()
                    .flatMap(x -> IntStream.rangeClosed(centerZ - radius, centerZ + radius)
                            .mapToObj(z -> {
                                Chunk chunk = world.getChunkAt(x, z);
                                chunks.add(chunk);
                                int count = loadedChunks.incrementAndGet();
                                if (count % 1000 == 0) {
                                    instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("chunk-while-loading")
                                            .replace("%loaded-chunks%", instance.getMain().getNumberSeparate(String.valueOf(count)))
                                            .replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(totalChunks)))));
                                }
                                return chunk;
                            }))
                    .collect(Collectors.toList());
            }).thenApply(v -> chunks);
        }
    }

    
    /**
     * Calculates the number of chunks in a square area given a radius.
     *
     * @param radius the radius around the center chunk
     * @return the total number of chunks in the square area
     */
    public static int calculateNumberOfChunks(int radius) {
        int sideLength = 2 * radius + 1;
        return sideLength * sideLength;
    }
}
