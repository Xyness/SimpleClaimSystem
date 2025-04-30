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
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimBansGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimChunksGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMembersGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsProtectedAreasGui;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.CustomSet;
import fr.xyness.SCS.Types.WorldMode;

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
                completions.addAll(getPrimaryCompletions(args));
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
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
        
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
        if (instance.getMain().getClaimsNameFromOwner("*").contains(args[1])) {
            String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            if (description.length() > Integer.parseInt(instance.getSettings().getSetting("max-length-claim-description"))) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-description-too-long"));
                return;
            }
			if (!instance.getSettings().getDescriptionPatternProtected().matcher(description).find()) {
				player.sendMessage(instance.getLanguage().getMessage("incorrect-characters-description"));
				return;
			}
            Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
            instance.getMain().setClaimDescription(claim, description)
            	.thenAccept(success -> {
            		if (success) {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("claim-set-description-success").replace("%name%", args[1]).replace("%description%", description)));
            		} else {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
            Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
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
        	int X_;
        	int Z_;
        	try {
        		X_ = Integer.parseInt(parts[1]);
        	} catch (NumberFormatException e) {
        		player.sendMessage(instance.getLanguage().getMessage("x-z-must-be-integer"));
        		return;
        	}
        	try {
        		Z_ = Integer.parseInt(parts[2]);
        	} catch (NumberFormatException e) {
        		player.sendMessage(instance.getLanguage().getMessage("x-z-must-be-integer"));
        		return;
        	}
        	if(instance.isFolia()) {
        		world.getChunkAtAsync(X_, Z_).thenAccept(chunk -> {
        			Set<Chunk> chunks = new HashSet<>(claim.getChunks());
        			if(!chunks.contains(chunk)) {
        				player.sendMessage(instance.getLanguage().getMessage("chunk-not-in-claim"));
        				return;
        			}
        			chunks.remove(chunk);
                    if(!instance.getMain().areChunksConnected(chunks)) {
                    	player.sendMessage(instance.getLanguage().getMessage("chunks-are-not-connected-delchunk"));
                    	return;
                    }
        			instance.getMain().removeClaimChunk(claim, chunk)
	                	.thenAccept(success -> {
	                		if (success) {
	                			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+args[2]+"]").replace("%claim-name%", claim.getName())));
	                		} else {
	                			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error-delete-chunk")));
	                		}
	                	})
	                    .exceptionally(ex -> {
	                        ex.printStackTrace();
	                        return null;
	                    });
        		});
        	} else {
        		Chunk chunk = world.getChunkAt(X_, Z_);
    			Set<Chunk> chunks = new HashSet<>(claim.getChunks());
    			if(!chunks.contains(chunk)) {
    				player.sendMessage(instance.getLanguage().getMessage("chunk-not-in-claim"));
    				return;
    			}
    			chunks.remove(chunk);
                if(!instance.getMain().areChunksConnected(chunks)) {
                	player.sendMessage(instance.getLanguage().getMessage("chunks-are-not-connected-delchunk"));
                	return;
                }
    			instance.getMain().removeClaimChunk(claim, chunk)
                	.thenAccept(success -> {
                		if (success) {
                			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-chunk-success").replace("%chunk%", "["+args[2]+"]").replace("%claim-name%", claim.getName())));
                		} else {
                			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error-delete-chunk")));
                		}
                	})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        	}
            return;
    	}
    	if (args[0].equalsIgnoreCase("merge")) {
            Set<String> claimsName = instance.getMain().getClaimsNameFromOwner("*");
            if (!claimsName.contains(args[1])) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
            	return;
            }
            Claim claim1 = instance.getMain().getProtectedAreaByName(args[1]);
            Set<Claim> claims = new HashSet<>();
            if(args[2].equals("*")) {
            	claims.addAll(instance.getMain().getProtectedAreas());
            	claims.remove(claim1);
            	if(claims.size() == 0) {
                	player.sendMessage(instance.getLanguage().getMessage("no-claim-can-be-merged"));
                    return;
            	}
            } else if(args[2].contains(";")) {
            	for(String c : args[2].split(";")) {
            		if(!claimsName.contains(c)) {
                    	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                    	return;
            		}
            		claims.add(instance.getMain().getProtectedAreaByName(c));
            	}
            } else {
                if (!claimsName.contains(args[2])) {
                	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                	return;
                }
                Claim claim2 = instance.getMain().getProtectedAreaByName(args[2]);
                claims.add(claim2);
            }
            if(claims.contains(claim1)) {
            	player.sendMessage(instance.getLanguage().getMessage("cant-merge-same-claim"));
            	return;
            }
            for(Claim claim : claims) {
            	if(!instance.getMain().isAnyChunkAdjacentBetweenSets(new CustomSet<>(claim1.getChunks()), new CustomSet<>(claim.getChunks()))) {
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
            instance.getMain().mergeClaims(claim1, new CustomSet<>(claims))
            	.thenAccept(success -> {
            		if (success) {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("claims-are-now-merged").replace("%claim-name%", claim1.getName())));
            			if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, new CustomSet<>(claim1.getChunks()), true, false);
            		} else {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
            		}
            	})
                .exceptionally(ex -> {
                    ex.printStackTrace();
                    return null;
                });
            return;
    	}
    	if (args[0].equalsIgnoreCase("kick")) {
            if (args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getProtectedAreasCount() == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
                Player target = Bukkit.getPlayer(args[2]);
                if(target == null) {
                	player.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
                	return;
                }
                if(target.getName().equals(playerName)) {
                	player.sendMessage(instance.getLanguage().getMessage("can-not-kick-yourself"));
                	return;
                }
	        	if(!instance.getMain().getAllChunksFromAllClaims("*").contains(target.getLocation().getChunk())) {
	            	player.sendMessage(instance.getLanguage().getMessage("player-not-in-any-claim").replace("%player%", target.getName()));
	            	return;
	        	}
	            player.sendMessage(instance.getLanguage().getMessage("kick-success-all-protected-areas").replace("%player%", target.getName()));
	            target.sendMessage(instance.getLanguage().getMessage("kicked-from-all-protected-areas").replace("%player%", playerName));
	        	instance.getMain().teleportPlayerToExpulsion(target);
	        	return;
            }
            Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Player target = Bukkit.getPlayer(args[2]);
            if(target == null) {
            	player.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
            	return;
            }
            if(target.getName().equals(playerName)) {
            	player.sendMessage(instance.getLanguage().getMessage("can-not-kick-yourself"));
            	return;
            }
            if(!claim.getChunks().contains(target.getLocation().getChunk())) {
            	player.sendMessage(instance.getLanguage().getMessage("player-not-in-the-protected-area").replace("%player%", target.getName()).replace("%claim-name%", claim.getName()));
            	return;
            }
            String claimName = claim.getName();
            player.sendMessage(instance.getLanguage().getMessage("kick-success-protected-area").replace("%player%", target.getName()).replace("%claim-name%", claimName));
            target.sendMessage(instance.getLanguage().getMessage("kicked-from-protected-area").replace("%player%", playerName).replace("%claim-name%", claimName));
            instance.getMain().teleportPlayerToExpulsion(target);
            return;
    	}
    	if(args[0].equalsIgnoreCase("ban")) {
			if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getProtectedAreasCount() == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			Player target = Bukkit.getPlayer(args[2]);
    			String[] targetName = {""};
    			
    			// Create runnable
    			Runnable task = () -> {
    				instance.executeSync(() -> {
    	        		if(targetName[0].equals(playerName)) {
    	        			player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
    	        			return;
    	        		}
    	        		String message = instance.getLanguage().getMessage("add-ban-all-success").replace("%player%", targetName[0]);
    	        		instance.getMain().addAllClaimBan("*",targetName[0])
    	        			.thenAccept(success -> {
    	        				if (success) {
    	        					instance.executeEntitySync(player, () -> player.sendMessage(message));
    	    	        			if (target != null && target.isOnline()) {
    	    	        				if(instance.getMain().getAllChunksFromAllProtectedAreas().contains(target.getLocation().getChunk())) {
    	            		        		instance.executeEntitySync(target, () -> instance.getMain().teleportPlayerToExpulsion(target));
    	            		        	}
    	            		        	instance.executeEntitySync(target, () -> {
    			    			        	target.sendMessage(instance.getLanguage().getMessage("banned-all-claim-protected-area-player"));
    			    			        	target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-protected-area-player"));
    	            		        	});
    	    	        			}
    	        				} else {
    	        					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
    	        				}
    	        			})
    	                    .exceptionally(ex -> {
    	                        ex.printStackTrace();
    	                        return null;
    	                    });
    				});
    			};

                if (target == null) {
                	instance.getOfflinePlayer(args[2], otarget -> {
                        if (otarget == null || !otarget.hasPlayedBefore()) {
                        	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        targetName[0] = otarget.getName();
                        task.run();
                	});
                } else {
                    targetName[0] = target.getName();
                    task.run();
                }
        		return;
    		}
    		Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
			Player target = Bukkit.getPlayer(args[2]);
			String[] targetName = {""};
			
			Runnable task = () -> {
				instance.executeSync(() -> {
		    		if(targetName[0].equals(playerName)) {
		    			player.sendMessage(instance.getLanguage().getMessage("cant-ban-yourself"));
		    			return;
		    		}
                    if (instance.getMain().checkBan(claim, targetName[0])) {
                        String message = instance.getLanguage().getMessage("already-banned").replace("%player%", targetName[0]);
                        player.sendMessage(message);
                        return;
                    }
		    		String message = instance.getLanguage().getMessage("add-ban-success").replace("%player%", targetName[0]).replace("%claim-name%", claim.getName());
		    		instance.getMain().addClaimBan(claim, targetName[0])
		    			.thenAccept(success -> {
		    				if (success) {
		    					instance.executeEntitySync(player, () -> player.sendMessage(message));
		    			        if (target != null && target.isOnline()) {
		    			        	String claimName = claim.getName();
		    			        	if(claim.getChunks().contains(target.getLocation().getChunk())) {
		        		        		instance.executeEntitySync(target, () -> instance.getMain().teleportPlayerToExpulsion(target));
		        		        	}
		        		        	instance.executeEntitySync(target, () -> {
			    			        	target.sendMessage(instance.getLanguage().getMessage("banned-claim-protected-area-player").replace("%claim-name%", claimName));
			    			        	target.sendMessage(instance.getLanguage().getMessage("remove-claim-protected-area-player").replace("%claim-name%", claimName));
		        		        	});
		    			        }
		    				} else {
		    					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
		    				}
		    			})
		                .exceptionally(ex -> {
		                    ex.printStackTrace();
		                    return null;
		                });
				});
			};
			
            if (target == null) {
            	instance.getOfflinePlayer(args[2], otarget -> {
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName[0] = otarget.getName();
                    task.run();
            	});
            } else {
                targetName[0] = target.getName();
                task.run();
            }
    		return;
    	}
    	if(args[0].equalsIgnoreCase("unban")) {
			if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getProtectedAreasCount() == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
			}
    		Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		if(!instance.getMain().checkBan(claim, args[2])) {
    			String message = instance.getLanguage().getMessage("not-banned").replace("%player%", args[2]);
    			player.sendMessage(message);
    			return;
    		}
    		String targetName = instance.getMain().getRealNameFromClaimBans(claim, args[2]);
    		String message = instance.getLanguage().getMessage("remove-ban-success").replace("%player%", targetName).replace("%claim-name%", claim.getName());
    		instance.getMain().removeClaimBan(claim, targetName)
    			.thenAccept(success -> {
    				if (success) {
    					instance.executeEntitySync(player, () -> player.sendMessage(message));
                        Player target = Bukkit.getPlayer(targetName);
        		        if (target != null && target.isOnline()) {
        		        	instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("unbanned-claim-protected-area-player").replace("%claim-name%", claim.getName())));
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
    	if(args[0].equalsIgnoreCase("setname")) {
			if (!instance.getMain().checkName(ClaimMain.SERVER_UUID,args[1])) {
                if (args[2].contains("claim-") || !args[2].matches("^[a-zA-Z0-9]+$")) {
                	player.sendMessage(instance.getLanguage().getMessage("you-cannot-use-this-name"));
                    return;
                }
        		if(instance.getMain().checkName(ClaimMain.SERVER_UUID,args[2])) {
        			Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
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
        		player.sendMessage(instance.getLanguage().getMessage("error-name-exists").replace("%name%", args[2]));
            	return;
    		}
    		Chunk chunk = player.getLocation().getChunk();
    		if(!instance.getMain().checkIfClaimExists(chunk)) {
    			player.sendMessage(instance.getLanguage().getMessage("free-territory"));
    			return;
    		}
    		Claim claim = instance.getMain().getClaim(chunk);
    		String owner = claim.getOwner();
    		if(!owner.equals("*")) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    			return;
    		}
			if(args[1].contains("claim-")) {
				player.sendMessage(instance.getLanguage().getMessage("you-cannot-use-this-name"));
				return;
			}
    		if(instance.getMain().checkName(ClaimMain.SERVER_UUID,args[1])) {
            	instance.getMain().setClaimName(claim, args[1])
        		.thenAccept(success -> {
        			if (success) {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("name-change-success").replace("%name%", args[1])));
        			} else {
        				instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
    	if(args[0].equalsIgnoreCase("add")) {
			if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getProtectedAreasCount() == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			Player target = Bukkit.getPlayer(args[2]);
    			String[] targetName = {""};
    			
    			// Create runnable
    			Runnable task = () -> {
    				instance.executeSync(() -> {
    	    			String message = instance.getLanguage().getMessage("add-member-success").replace("%player%", targetName[0]).replace("%claim-name%", instance.getLanguage().getMessage("protected-area-title"));
    	    			instance.getMain().addAllClaimsMember("*",targetName[0])
    	    				.thenAccept(success -> {
    	    					if (success) {
    	    						instance.executeEntitySync(player, () -> player.sendMessage(message));
    	    	        			if(target != null && target.isOnline()) {
    	    	        				instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("add-all-claim-protected-area-player")));
    	    	        			}
    	    					} else {
    	    						instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
    	    					}
    	    				})
    	                    .exceptionally(ex -> {
    	                        ex.printStackTrace();
    	                        return null;
    	                    });
    				});
    			};
    			
                if (target == null) {
                	instance.getOfflinePlayer(args[2], otarget -> {
                        if (otarget == null || !otarget.hasPlayedBefore()) {
                        	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        targetName[0] = otarget.getName();
                        task.run();
                	});
                } else {
                    targetName[0] = target.getName();
                    task.run();
                }
        		return;
    		}
    		Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
            Player target = Bukkit.getPlayer(args[2]);
            String[] targetName = {""};
            
            // Create runnable
            Runnable task = () -> {
            	instance.executeSync(() -> {
                    if (instance.getMain().checkMembre(claim, targetName[0])) {
                        String message = instance.getLanguage().getMessage("already-member").replace("%player%", targetName[0]);
                        player.sendMessage(message);
                        return;
                    }
            		String message = instance.getLanguage().getMessage("add-member-success").replace("%player%", targetName[0]).replace("%claim-name%", claim.getName());
            		instance.getMain().addClaimMember(claim, targetName[0])
            			.thenAccept(success -> {
            				if (success) {
            					instance.executeEntitySync(player, () -> player.sendMessage(message));
            					if(target != null && target.isOnline()) {
            						instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("add-claim-protected-area-player").replace("%claim-name%", claim.getName())));
            					}
            				} else {
            					instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
            				}
            			})
                        .exceptionally(ex -> {
                            ex.printStackTrace();
                            return null;
                        });
            	});
            };
            
            if (target == null) {
            	instance.getOfflinePlayer(args[2], otarget -> {
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName[0] = otarget.getName();
                    task.run();
            	});
            } else {
                targetName[0] = target.getName();
                task.run();
            }
    		return;
    	}
    	if(args[0].equalsIgnoreCase("remove")) {
    		if(args[1].equalsIgnoreCase("*")) {
        		if(instance.getMain().getProtectedAreasCount() == 0) {
        			player.sendMessage(instance.getLanguage().getMessage("no-admin-claim"));
        			return;
        		}
    			String targetName = args[2];
    			String message = instance.getLanguage().getMessage("remove-member-success").replace("%player%", targetName).replace("%claim-name%", instance.getLanguage().getMessage("protected-area-title"));
    			instance.getMain().removeAllClaimsMember("*",targetName)
    				.thenAccept(success -> {
    					if (success) {
    						instance.executeEntitySync(player, () -> player.sendMessage(message));
    						Player target = Bukkit.getPlayer(targetName);
    						if(target != null && target.isOnline()) {
    							instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-all-claim-protected-area-player")));
    						}
    					} else {
    						instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
    					}
    				})
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        return null;
                    });
        		return;
    		}
    		Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
    		if(claim == null) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
    			return;
    		}
    		String targetName = args[2];
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
            			instance.executeEntitySync(player, () -> player.sendMessage(message));
            			Player target = Bukkit.getPlayer(realName);
            			if(target != null && target.isOnline()) {
            				instance.executeEntitySync(target, () -> target.sendMessage(instance.getLanguage().getMessage("remove-claim-protected-area-player").replace("%claim-name%", claim.getName())));
            			}
            		} else {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
    	Claim claim = instance.getMain().getProtectedAreaByName(args[1]);
    	if (args[0].equalsIgnoreCase("addchunk")) {
            if (claim == null) {
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            Chunk chunk = player.getLocation().getChunk();
            if(instance.getMain().checkIfClaimExists(chunk)) {
            	Claim claim_target = instance.getMain().getClaim(chunk);
            	if(claim_target.getOwner().equalsIgnoreCase("*")) {
            		if(claim_target.equals(claim)) {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-already-in-claim-protected-area")
            					.replace("%claim-name%", claim.getName()));
            			return;
            		} else {
            			player.sendMessage(instance.getLanguage().getMessage("add-chunk-already-owner-protected-area")
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
            return;
    	}
    	if (args[0].equalsIgnoreCase("unclaim")) {
            if (claim == null) {
            	if (args[1].equalsIgnoreCase("*")) {
                    instance.getMain().deleteAllClaims("*")
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
                    return;
            	}
            	player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                return;
            }
            instance.getMain().deleteClaim(claim)
            	.thenAccept(success -> {
            		if (success) {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("delete-claim-protected-area")));
            		} else {
            			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
    		new AdminGestionClaimGui(player,claim,instance,"visitors");
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
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
    		String owner = claim.getOwner();
    		if(owner.equals("*")) {
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
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
    		String owner = claim.getOwner();
    		if(owner.equals("*")) {
    			new AdminGestionClaimGui(player, claim, instance,"visitors");
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
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
    		String owner = claim.getOwner();
    		if(!owner.equals("*")) {
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
            	player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                return;
            }
    		String owner = claim.getOwner();
    		if(!owner.equals("*")) {
    			player.sendMessage(instance.getLanguage().getMessage("claim-not-an-admin-claim"));
    			return;
    		}
    		cPlayer.setGuiPage(1);
    		new AdminGestionClaimChunksGui(player,claim,1,instance);
            return;
    	}
    	String world = player.getWorld().getName();
    	if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
        	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", player.getWorld().getName()));
        	return;
        }
    	try {
			int radius = Integer.parseInt(args[0]);
            ClaimCommand.getChunksInRadius(player, player.getLocation(), radius, instance).thenAccept(chunks -> {
		        // Check if all claims are free to claim
		        Set<Chunk> chunksToClaim = chunks.stream()
		                .filter(c -> !instance.getMain().checkIfClaimExists(c))
		                .collect(Collectors.toSet());
		        
		        if (chunks.size() != chunksToClaim.size()) {
		            instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("cant-radius-claim-already-claim")));
		            return;
		        }
		        
		        instance.getMain().createAdminClaimRadius(player, new CustomSet<>(chunks), radius)
		        	.thenAccept(success -> {
		        		if (success) {
		    		        if (instance.getSettings().getBooleanSetting("claim-particles")) instance.executeSync(() -> instance.getMain().displayChunkBorderWithRadius(player, radius));
		    		        instance.executeAsyncLocation(() -> {
			    		        Claim claim = instance.getMain().getClaim(player.getLocation().getChunk());
			    		        instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("create-protected-area-radius-success").replace("%number%", instance.getMain().getNumberSeparate(String.valueOf(chunks.size()))).replace("%claim-name%", claim.getName())));
		    		        }, player.getLocation());
		        		} else {
		        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
		        		}
		        	})
		            .exceptionally(ex -> {
		                ex.printStackTrace();
		                return null;
		            });
            });
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
    		if(!owner.equals("*")) {
    			player.sendMessage(instance.getLanguage().getMessage("create-already-claim").replace("%player%", owner));
    			return;
    		}
    		new AdminGestionClaimMainGui(player,claim,instance);
    		return;
    	}
    	String world = player.getWorld().getName();
    	if (instance.getSettings().getWorldMode(world) == WorldMode.DISABLED) {
        	player.sendMessage(instance.getLanguage().getMessage("world-disabled").replace("%world%", player.getWorld().getName()));
        	return;
        }
        
        // Check if the chunk is already claimed
        if (instance.getMain().checkIfClaimExists(chunk)) {
        	instance.getMain().handleClaimConflict(player, chunk);
        	return;
        }
        
        instance.getMain().createAdminClaim(player, chunk)
        	.thenAccept(success -> {
        		if (success) {
        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("create-protected-area-success")));
    		        if (instance.getSettings().getBooleanSetting("claim-particles")) instance.getMain().displayChunks(player, new CustomSet<>(Set.of(chunk)), true, false);
        		} else {
        			instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
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
     * @param args the args of the command
     * @return a list of primary completions
     */
    private List<String> getPrimaryCompletions(String[] args) {
    	String partialInput = args.length > 0 ? args[0].toLowerCase() : "";
        List<String> completions = List.of("setdesc", "settings", "setname", "members", "tp", "list", "ban", "unban", "bans", "add", "remove", "unclaim", "main",
        		"merge", "addchunk", "delchunk", "chunks", "kick");
        return completions.stream()
	        .filter(c -> c.toLowerCase().startsWith(partialInput))
	        .collect(Collectors.toList());
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
        String partialInput = args.length > 1 ? args[1].toLowerCase() : "";
        switch (command) {
            case "ban":
            case "unban":
            case "add":
                completions.add("*");
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
                completions.addAll(instance.getMain().getClaimsNameFromOwner("*"));
                break;
            case "remove":
            case "unclaim":
            case "kick":
                completions.add("*");
                completions.addAll(instance.getMain().getClaimsNameFromOwner("*"));
                break;
            default:
                break;
        }
        return completions.stream()
	        .filter(c -> c.toLowerCase().startsWith(partialInput))
	        .collect(Collectors.toList());
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
        
        Claim claim = instance.getMain().getProtectedAreaByName(claimName);
        if(claim == null) return completions;

        String partialInput = args.length > 2 ? args[2].toLowerCase() : "";
        
        switch (command) {
            case "unban":
                completions.addAll(instance.getMain().convertUUIDSetToStringSet(claim.getBans()));
                break;
            case "ban":
            case "add":
            case "kick":
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                break;
            case "remove":
                if (!claimName.equals("*")) {
                    completions.addAll(instance.getMain().convertUUIDSetToStringSet(claim.getMembers()));
                } else {
                    completions.addAll(instance.getMain().getAllMembersOfAllPlayerClaim("*"));
                }
                break;
            case "merge":
            	completions.addAll(instance.getMain().getClaimsNameFromOwner("*"));
            	completions.remove(args[1]);
            	break;
            case "delchunk":
            	if(claim != null) {
            		completions.addAll(instance.getMain().getStringChunkFromClaim(claim));
            	}
            	break;
            default:
                break;
        }
        return completions.stream()
    	        .filter(c -> c.toLowerCase().startsWith(partialInput))
    	        .collect(Collectors.toList());
    }
}
