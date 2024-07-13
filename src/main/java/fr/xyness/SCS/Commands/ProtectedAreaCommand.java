package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.ClaimMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimBansGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimChunksGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMembersGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsProtectedAreasGui;

/**
 * This class handles admin commands related to claims.
 */
public class ProtectedAreaCommand implements CommandExecutor, TabCompleter {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ScsCommand.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ProtectedAreaCommand(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
	
	// ******************
	// *  Tab Complete  *
	// ******************
	
	
    /**
     * Provides tab completion for the command.
     *
     * @param sender the sender of the command
     * @param cmd the command
     * @param alias the alias used for the command
     * @param args the arguments of the command
     * @return a list of possible completions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            List<String> completions = new ArrayList<>();
            if (args.length == 1) {
                completions.addAll(getPrimaryCompletions());
            } else if (args.length == 2) {
                completions.addAll(getSecondaryCompletions(sender, args));
            } else if (args.length == 3) {
                completions.addAll(getTertiaryCompletions(sender, args));
            }
            return completions;
        });

        try {
            return future.get();
        } catch (ExecutionException | InterruptedException e) {
            return new ArrayList<>();
        }
    }
	
	
	// ******************
	// *  Main command  *
	// ******************
    
	
    /**
     * Handles the command execution.
     *
     * @param sender the sender of the command
     * @param command the command
     * @param label the alias of the command
     * @param args the arguments of the command
     * @return true if the command was successfully executed, false otherwise
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
        
        // Check if for desc (so there are many arguments)
        if (args.length > 1 && args[0].equals("setdesc")) {
        	handleDesc(player, args);
            return true;
        }
        
        // Switch for args
        switch(args.length) {
        	case 0:
        		handleArgZero(player);
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
        		instance.getMain().getHelp(player, args[0], "parea");
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
    private void handleDesc(Player player, String[] args) {
        if (instance.getMain().getClaimsNameFromOwner("admin").contains(args[1])) {
            String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (description.length() > Integer.parseInt(instance.getSettings().getSetting("max-length-claim-description"))) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-description-too-long"));
                return;
            }
            if (!description.matches("^[a-zA-Z0-9]+$")) {
            	player.sendMessage(instance.getLanguage().getMessage("incorrect-characters-description"));
            	return;
            }
            Claim claim = instance.getMain().getClaimByName(args[1], "admin");
            instance.getMain().setAdminChunkDescription(claim, description)
            	.thenAccept(success -> {
            		if (success) {
                    	player.sendMessage(instance.getLanguage().getMessage("claim-set-description-success").replaceAll("%name%", args[1]).replaceAll("%description%", description));
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
            Claim claim = instance.getMain().getClaimByName(args[1], "admin");
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
            instance.getMain().removeChunk(claim, parts[0]+";"+parts[1]+";"+parts[2])
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replaceAll("%chunk%", "["+args[2]+"]").replaceAll("%claim-name%", claim.getName()));
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
            Set<String> claimsName = instance.getMain().getClaimsNameFromOwner("admin");
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
            		claims.add(instance.getMain().getClaimByName(c, "admin"));
            	}
            } else {
                if (!claimsName.contains(args[2])) {
                	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                	return;
                }
                Claim claim2 = instance.getMain().getClaimByName(args[2], "admin");
                claims.add(claim2);
            }
            Claim claim1 = instance.getMain().getClaimByName(args[1], "admin");
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
            if(!cPlayer.canClaimWithNumber(chunks.size()+claim1.getChunks().size())) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-claim-with-so-many-chunks"));
            	return;
            }
            if(!instance.getMain().areChunksInSameWorld(chunks)) {
            	player.sendMessage(instance.getLanguage().getMessage("chunks-must-be-from-same-world"));
            	return;
            }
            instance.getMain().mergeClaimsProtectedArea(player, claim1, claims)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("claims-are-now-merged").replaceAll("%claim-name%", claim1.getName()));
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
    	if(args[0].equalsIgnoreCase("ban")) {
			if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getPlayerClaimsCount("admin") == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			Player target = Bukkit.getPlayer(args[2]);
    			String targetName = "";
    			if(target == null) {
    				OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
    				if(otarget == null || !otarget.hasPlayedBefore()) {
    					player.sendMessage(instance.getLanguage().getMessage("player-never-played").replaceAll("%player%", args[2]));
        				return;
    				}
    				targetName = otarget.getName();
    			} else {
    				targetName = target.getName();
    			}
        		if(targetName.equals(playerName)) {
        			player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
        			return;
        		}
        		String message = instance.getLanguage().getMessage("add-ban-all-success").replaceAll("%player%", targetName);
        		instance.getMain().addAllAdminClaimBan(targetName)
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
    		Claim claim = instance.getMain().getClaimByName(args[1], "admin");
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
			Player target = Bukkit.getPlayer(args[2]);
			String targetName = "";
			if(target == null) {
				OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
				if(otarget == null || !otarget.hasPlayedBefore()) {
					player.sendMessage(instance.getLanguage().getMessage("player-never-played").replaceAll("%player%", args[2]));
    				return;
				}
				targetName = otarget.getName();
			} else {
				targetName = target.getName();
			}
    		if(targetName.equals(playerName)) {
    			player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
    			return;
    		}
    		String message = instance.getLanguage().getMessage("add-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", claim.getName());
    		instance.getMain().addAdminClaimBan(claim, targetName)
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
    	if(args[0].equalsIgnoreCase("unban")) {
			if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getPlayerClaimsCount("admin") == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			Player target = Bukkit.getPlayer(args[2]);
    			String targetName = "";
    			if(target == null) {
    				OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
    				targetName = otarget == null || !otarget.hasPlayedBefore() ? args[2] : otarget.getName();
    			} else {
    				targetName = target.getName();
    			}
    			String message = instance.getLanguage().getMessage("remove-ban-all-success").replaceAll("%player%", targetName);
    			instance.getMain().removeAllAdminClaimBan(targetName)
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
    		Claim claim = instance.getMain().getClaimByName(args[1], "admin");
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		if(!instance.getMain().checkBan(claim, args[2])) {
    			String message = instance.getLanguage().getMessage("not-banned").replaceAll("%player%", args[2]);
    			player.sendMessage(message);
    			return;
    		}
    		String targetName = instance.getMain().getRealNameFromClaimBans(claim, args[2]);
    		String message = instance.getLanguage().getMessage("remove-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", claim.getName());
    		instance.getMain().removeAdminClaimBan(claim, targetName)
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
    	if(args[0].equalsIgnoreCase("setname")) {
			if (!instance.getMain().checkName("admin",args[1])) {
                if (args[2].contains("claim-") || !args[2].matches("^[a-zA-Z0-9]+$")) {
                	player.sendMessage(instance.getLanguage().getMessage("you-cannot-use-this-name"));
                    return;
                }
        		if(instance.getMain().checkName("admin",args[2])) {
        			Claim claim = instance.getMain().getClaimByName(args[1], "admin");
                	instance.getMain().setAdminClaimName(claim, args[2]);
                	player.sendMessage(instance.getLanguage().getMessage("name-change-success").replaceAll("%name%", args[2]));
                	return;
        		}
        		player.sendMessage(instance.getLanguage().getMessage("error-name-exists").replaceAll("%name%", args[1]));
            	return;
    		}
    		Chunk chunk = player.getLocation().getChunk();
    		if(!instance.getMain().checkIfClaimExists(chunk)) {
    			player.sendMessage(instance.getLanguage().getMessage("free-territory"));
    			return;
    		}
    		Claim claim = instance.getMain().getClaim(chunk);
    		String owner = claim.getOwner();
    		if(!owner.equals("admin")) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    			return;
    		}
			if(args[1].contains("claim-")) {
				player.sendMessage(instance.getLanguage().getMessage("you-cannot-use-this-name"));
				return;
			}
    		if(instance.getMain().checkName("admin",args[1])) {
            	instance.getMain().setAdminClaimName(claim, args[1]);
            	player.sendMessage(instance.getLanguage().getMessage("name-change-success").replaceAll("%name%", args[1]));
            	return;
    		}
    		player.sendMessage(instance.getLanguage().getMessage("error-name-exists").replaceAll("%name%", args[1]));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("add")) {
			if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getPlayerClaimsCount("admin") == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			Player target = Bukkit.getPlayer(args[2]);
    			String targetName = "";
    			if(target == null) {
    				OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
    				if(otarget == null || !otarget.hasPlayedBefore()) {
    					player.sendMessage(instance.getLanguage().getMessage("player-never-played").replaceAll("%player%", args[2]));
        				return;
    				}
    				targetName = otarget.getName();
    			} else {
    				targetName = target.getName();
    			}
    			String message = instance.getLanguage().getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", instance.getLanguage().getMessage("protected-area-title"));
    			instance.getMain().addAllAdminClaimMembers(targetName)
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
            Player target = Bukkit.getPlayer(args[2]);
            String targetName = "";
            if (target == null) {
                OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
                if (otarget == null || !otarget.hasPlayedBefore()) {
                	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replaceAll("%player%", args[2]));
                    return;
                }
                targetName = otarget.getName();
            } else {
                targetName = target.getName();
            }
    		Claim claim = instance.getMain().getClaimByName(args[1], "admin");
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		String message = instance.getLanguage().getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", claim.getName());
    		instance.getMain().addAdminClaimMembers(claim, targetName)
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
    	if(args[0].equalsIgnoreCase("remove")) {
    		if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getPlayerClaimsCount("admin") == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			String targetName = args[2];
    			String message = instance.getLanguage().getMessage("remove-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", instance.getLanguage().getMessage("protected-area-title"));
    			instance.getMain().removeAllAdminClaimMembers(targetName)
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
    		Claim claim = instance.getMain().getClaimByName(args[1], "admin");
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		String targetName = args[2];
            if (!instance.getMain().checkMembre(claim, targetName)) {
                String message = instance.getLanguage().getMessage("not-member").replaceAll("%player%", targetName);
                player.sendMessage(message);
                return;
            }
            String realName = instance.getMain().getRealNameFromClaimMembers(claim, targetName);
            String message = instance.getLanguage().getMessage("remove-member-success").replaceAll("%player%", realName).replaceAll("%claim-name%", claim.getName());
            instance.getMain().removeAdminClaimMembers(claim, realName)
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
    	instance.getMain().getHelp(player, args[0], "parea");
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
    	Claim claim = instance.getMain().getClaimByName(args[1], "admin");
    	if (args[0].equalsIgnoreCase("addchunk")) {
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if(instance.getMain().checkIfClaimExists(chunk)) {
            	Claim claim_target = instance.getMain().getClaim(chunk);
            	if(claim_target.getOwner().equalsIgnoreCase("admin")) {
            		if(claim_target.equals(claim)) {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-already-in-claim-protected-area")
            					.replaceAll("%claim-name%", claim.getName()));
            			return;
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-already-owner-protected-area")
            					.replaceAll("%claim-name%", claim.getName())
            					.replaceAll("%claim-name-1%", claim_target.getName()));
            			return;
            		}
            	} else {
            		player.sendMessage(instance.getLanguage().getMessage("add-chunk-not-owner"));
            		return;
            	}
            }
            Set<Chunk> chunks = new HashSet<>(claim.getChunks());
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
            instance.getMain().addChunk(claim, chunk)
            	.thenAccept(success -> {
            		if (success) {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-successful")
            					.replaceAll("%chunk%", "["+chunk.getWorld().getName()+";"+String.valueOf(chunk.getX())+";"+String.valueOf(chunk.getZ())+"]")
            					.replaceAll("%claim-name%", claim.getName()));
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
    	if (args[0].equalsIgnoreCase("unclaim")) {
            if (claim == null) {
            	if (args[1].equalsIgnoreCase("*")) {
                    instance.getMain().deleteAllAdminClaim()
	                	.thenAccept(success -> {
	                		if (success) {
	                			player.sendMessage(instance.getLanguage().getMessage("delete-claim-all-protected-area"));
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
            instance.getMain().forceDeleteClaim(claim)
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
    	if (args[0].equalsIgnoreCase("main")) {
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            new AdminGestionClaimMainGui(player,claim,instance);
    	}
    	if(args[0].equalsIgnoreCase("bans")) {
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimBansGui(player,claim,1,instance);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("tp")) {
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		instance.getMain().goClaim(player, claim.getLocation());
    		return;
    	}
    	if(args[0].equalsIgnoreCase("members")) {
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimMembersGui(player,claim,1,instance);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("chunks")) {
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimChunksGui(player,claim,1,instance);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("settings")) {
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		new AdminGestionClaimGui(player,claim,instance);
    		return;
    	}
    	instance.getMain().getHelp(player, args[0], "parea");
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
    	if(args[0].equalsIgnoreCase("bans")) {
    		Chunk chunk = player.getLocation().getChunk();
    		Claim claim = instance.getMain().getClaim(chunk);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
    		String owner = claim.getOwner();
    		if(owner.equals("admin")) {
    			cPlayer.setGuiPage(1);
    			new AdminGestionClaimBansGui(player, claim, 1, instance);
    			return;
    		}
    		player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("settings")) {
    		Chunk chunk = player.getLocation().getChunk();
    		Claim claim = instance.getMain().getClaim(chunk);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
    		String owner = claim.getOwner();
    		if(owner.equals("admin")) {
    			new AdminGestionClaimGui(player, claim, instance);
    			return;
    		}
    		player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("list")) {
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimsProtectedAreasGui(player, 1, "all", instance);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("members")) {
    		Chunk chunk = player.getLocation().getChunk();
    		Claim claim = instance.getMain().getClaim(chunk);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
    		String owner = claim.getOwner();
    		if(!owner.equals("admin")) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    			return;
    		}
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimMembersGui(player,claim,1,instance);
            return;
    	}
    	if(args[0].equalsIgnoreCase("chunks")) {
    		Chunk chunk = player.getLocation().getChunk();
    		Claim claim = instance.getMain().getClaim(chunk);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
    		String owner = claim.getOwner();
    		if(!owner.equals("admin")) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    			return;
    		}
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimChunksGui(player,claim,1,instance);
            return;
    	}
        if(instance.getSettings().isWorldDisabled(player.getWorld().getName())) {
        	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
        	return;
        }
    	try {
			int radius = Integer.parseInt(args[0]);
            ClaimCommand.getChunksInRadius(player, player.getLocation(), radius, instance).thenAccept(chunks -> instance.getMain().createAdminClaimRadius(player, chunks, radius));
		} catch(NumberFormatException e){
			instance.getMain().getHelp(player, args[0], "parea");
		}
    	return;
    }
    
    /**
     * Handles the command with no arguments for the given player.
     *
     * @param player the player executing the command
     */
    private void handleArgZero(Player player) {
    	Chunk chunk = player.getLocation().getChunk();
    	if(instance.getMain().checkIfClaimExists(chunk)) {
    		Claim claim = instance.getMain().getClaim(chunk);
    		String owner = claim.getOwner();
    		if(!owner.equals("admin")) {
    			player.sendMessage(instance.getLanguage().getMessage("create-already-claim").replace("%player%", owner));
    			return;
    		}
    		new AdminGestionClaimMainGui(player,claim,instance);
    		return;
    	}
        if(instance.getSettings().isWorldDisabled(player.getWorld().getName())) {
        	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
        	return;
        }
        instance.getMain().createAdminClaim(player, chunk);
    }
    
    /**
     * Gets the primary completions for the first argument.
     *
     * @return a list of primary completions
     */
    private List<String> getPrimaryCompletions() {
        return List.of("setdesc", "settings", "setname", "members", "tp", "list", "ban", "unban", "bans", "add", "remove", "unclaim", "main",
        		"merge", "addchunk", "delchunk", "chunks");
    }

    /**
     * Gets the secondary completions for the second argument.
     *
     * @param sender The command sender
     * @param args the args of the tab
     * @return a list of secondary completions
     */
    private List<String> getSecondaryCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String command = args[0].toLowerCase();

        switch (command) {
            case "ban":
            case "unban":
            case "add":
                completions.add("*");
                // Fall through to next case
            case "bans":
            case "setdesc":
            case "settings":
            case "setname":
            case "tp":
            case "members":
            case "merge":
            case "addchunk":
            case "delchunk":
            case "chunks":
                completions.addAll(instance.getMain().getClaimsNameFromOwner("admin"));
                break;
            case "remove":
            case "unclaim":
                completions.add("*");
                completions.addAll(instance.getMain().getClaimsNameFromOwner("admin"));
                break;
            default:
                break;
        }
        return completions;
    }

    /**
     * Gets the tertiary completions for the third argument.
     *
     * @param sender The command sender
     * @param args the args of the tab
     * @return a list of tertiary completions
     */
    private List<String> getTertiaryCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String command = args[0].toLowerCase();
        String claimName = args[1];

        switch (command) {
            case "unban":
                completions.addAll(instance.getMain().getBannedFromClaimName("admin", claimName));
                break;
            case "ban":
            case "add":
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                break;
            case "remove":
                if (!claimName.equals("*")) {
                    completions.addAll(instance.getMain().getMembersFromClaimName("admin", claimName));
                } else {
                    completions.addAll(instance.getMain().getAllMembersOfAllPlayerClaim("admin"));
                }
                break;
            case "merge":
            	completions.addAll(instance.getMain().getClaimsNameFromOwner("admin"));
            	completions.remove(args[1]);
            	break;
            case "delchunk":
            	Claim claim = instance.getMain().getClaimByName(claimName, "admin");
            	if(claim != null) {
            		completions.addAll(instance.getMain().getStringChunkFromClaim(claim));
            	}
            	break;
            default:
                break;
        }
        return completions;
    }
}
