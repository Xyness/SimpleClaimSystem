package fr.xyness.SCS.Commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsOwnerGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * This class handles admin commands related to claims.
 */
public class ScsCommand implements CommandExecutor, TabCompleter {
	
	
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
    public ScsCommand(SimpleClaimSystem instance) {
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
            } else if (args.length == 4 && args[0].equalsIgnoreCase("player") && (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("unclaim") || args[1].equalsIgnoreCase("main"))) {
            	String partialInput = args.length > 3 ? args[3].toLowerCase() : "";
            	completions.addAll(instance.getMain().getClaimsNameFromOwner(args[2]));
                if (args[1].equalsIgnoreCase("unclaim")) {
                    completions.add("*");
                }
                return completions.stream()
            	        .filter(c -> c.toLowerCase().startsWith(partialInput))
            	        .collect(Collectors.toList());
            } else if (args.length == 4) {
            	completions.addAll(getFourCompletions(sender, args));
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

    	// Switch for args
        switch(args.length) {
        	case 1:
        		handleArgOne(sender,args);
        		break;
        	case 2:
        		handleArgTwo(sender,args);
        		break;
        	case 3:
        		handleArgThree(sender,args);
        		break;
        	case 4:
        		handleArgFour(sender,args);
        		break;
        	default:
        		instance.getMain().getHelp(sender, "no arg", "scs");
        		break;
        }
        return true;
    }
    
    
    // ********************
    // *  Other Methods  *
    // ********************
    
    
    /**
     * Handles the command with only one argument for the given command sender.
     *
     * @param sender the command sender
     * @param args The args for the command
     */
    private void handleArgOne(CommandSender sender, String[] args) {
    	if(args[0].equalsIgnoreCase("reload")) {
    		instance.loadConfig(true,sender);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("config-reload")) {
    		instance.reloadOnlyConfig(sender);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("import-griefprevention")) {
    		if(!instance.getSettings().getBooleanSetting("griefprevention")) {
    			sender.sendMessage(instance.getLanguage().getMessage("griefprevention-needed"));
    			return;
    		}
    		instance.getMain().importFromGriefPrevention(sender);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("transfer")) {
    		if(!instance.getSettings().getBooleanSetting("database")) {
    			sender.sendMessage(instance.getLanguage().getMessage("not-using-database"));
    			return;
    		}
    		instance.getMain().transferClaims();
    		return;
    	}
    	if(args[0].equalsIgnoreCase("reset-all-player-claims-settings")) {
    		instance.getMain().resetAllPlayerClaimsSettings()
    			.thenAccept(success -> {
    				if (success) {
    					sender.sendMessage(instance.getLanguage().getMessage("reset-of-player-claims-settings-successful"));
    				} else {
    					sender.sendMessage(instance.getLanguage().getMessage("error"));
    				}
    			})
    	        .exceptionally(ex -> {
    	            ex.printStackTrace();
    	            return null;
    	        });
    		return;
    	}
    	if(args[0].equalsIgnoreCase("reset-all-admin-claims-settings")) {
    		instance.getMain().resetAllOwnerClaimsSettings("*")
    			.thenAccept(success ->{
    				if (success) {
    					sender.sendMessage(instance.getLanguage().getMessage("reset-of-admin-claims-settings-successful"));
    				} else {
    					sender.sendMessage(instance.getLanguage().getMessage("error"));
    				}
    			})
    	        .exceptionally(ex -> {
    	            ex.printStackTrace();
    	            return null;
    	        });
    		return;
    	}
    	if(sender instanceof Player) {
    		Player player = (Player) sender;
    		if(args[0].equalsIgnoreCase("admin")) {
    			new AdminGestionMainGui(player,instance);
    			return;
    		}
        	if(args[0].equalsIgnoreCase("setexpulsionlocation")) {
        		Location loc = player.getLocation();
        		instance.setExpulsionLocation(loc);
        		player.sendMessage(instance.getLanguage().getMessage("expulsion-set"));
        		return;
        	}
        	if(args[0].equalsIgnoreCase("forceunclaim")) {
        		Chunk chunk = player.getLocation().getChunk();
        		if(!instance.getMain().checkIfClaimExists(chunk)) {
        			player.sendMessage(instance.getLanguage().getMessage("free-territory"));
        			return;
        		}
        		Claim claim = instance.getMain().getClaim(chunk);
        		String owner = claim.getOwner();
        		instance.getMain().deleteClaim(claim)
        			.thenAccept(success -> {
        				if (success) {
        					player.sendMessage(instance.getLanguage().getMessage("forceunclaim-success").replace("%owner%", owner));
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
    	}
    	instance.getMain().getHelp(sender, args[0], "scs");
    }
    
    /**
     * Handles the command with two arguments for the given command sender.
     *
     * @param sender the command sender
     * @param args The args for the command
     */
    private void handleArgTwo(CommandSender sender, String[] args) {
    	if(sender instanceof Player) {
    		Player player = (Player) sender;
    		if(args[0].equalsIgnoreCase("setowner")) {
                Player target = Bukkit.getPlayer(args[1]);
                String[] targetName = {""};
                
                // Create runnable
                Runnable task = () -> {
                	instance.executeSync(() -> {
                		Chunk chunk = player.getLocation().getChunk();
                		if(!instance.getMain().checkIfClaimExists(chunk)) {
                			player.sendMessage(instance.getLanguage().getMessage("free-territory"));
                			return;
                		}
                		Claim claim = instance.getMain().getClaim(chunk);
                		instance.getMain().setOwner(targetName[0], claim)
                			.thenAccept(success -> {
                				if (success) {
                					sender.sendMessage(instance.getLanguage().getMessage("setowner-success").replace("%owner%", targetName[0]));
                				}
                			})
                            .exceptionally(ex -> {
                                return null;
                            });
                	});
                };
                
                if (target == null) {
                	instance.getOfflinePlayer(args[1], otarget -> {
                        if (otarget == null || !otarget.hasPlayedBefore()) {
                        	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
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
    	}
    	if(args[0].equalsIgnoreCase("set-lang")) {
    		instance.reloadLang(sender, args[1]);
    		return;
    	}
    	instance.getMain().getHelp(sender, args[0], "scs");
    }
    
    /**
     * Handles the command with three arguments for the given command sender.
     *
     * @param sender the command sender
     * @param args The args for the command
     */
    private void handleArgThree(CommandSender sender, String[] args) {
    	if(sender instanceof Player) {
    		Player player = (Player) sender;
    		if(args[0].equalsIgnoreCase("player")) {
    			if(args[1].equalsIgnoreCase("list")) {
        			if(!instance.getMain().getClaimsOwners().contains(args[2])) {
        				player.sendMessage(instance.getLanguage().getMessage("player-does-not-have-claim"));
        				return;
        			}
        			new AdminGestionClaimsOwnerGui(player,1,"all",args[2],instance);
            		return;
    			}
    		}
    	}
    	instance.getMain().getHelp(sender, args[0], "scs");
    }
    
    /**
     * Handles the command with four arguments for the given command sender.
     *
     * @param sender the command sender
     * @param args The args for the command
     */
    private void handleArgFour(CommandSender sender, String[] args) {
    	if(sender instanceof Player) {
    		Player player = (Player) sender;
    		if(args[0].equalsIgnoreCase("setowner")) {
                Player target = Bukkit.getPlayer(args[1]);
                String[] targetName = {""};
                UUID[] uuid = {null};
                
                // Create runnable
                Runnable task = () -> {
                	instance.executeSync(() -> {
                		if(!instance.getMain().getClaimsOwners().contains(targetName[0])) {
            				player.sendMessage(instance.getLanguage().getMessage("player-does-not-have-claim"));
            				return;
            			}
            			if(args[2].equals("*")) {
            				
                			// Check new owner
                            Player newOwner = Bukkit.getPlayer(args[3]);
                            String[] ownerName = {""};
                            
                            Runnable subtask = () -> {
                            	instance.executeSync(() -> {
                                    // Set the new owner
                            		instance.getMain().setOwner(ownerName[0], instance.getMain().getPlayerClaims(targetName[0]), targetName[0])
                	        			.thenAccept(success -> {
                	        				if (success) {
                	        					sender.sendMessage(instance.getLanguage().getMessage("setowner-all-other-success").replace("%owner%", ownerName[0]).replace("%old-owner%", targetName[0]));
                	        				}
                	        			})
                	                    .exceptionally(ex -> {
                	                        return null;
                	                    });
                            	});
                            };
                            
                            if (newOwner == null) {
                            	instance.getOfflinePlayer(args[1], otarget -> {
                                    if (otarget == null || !otarget.hasPlayedBefore()) {
                                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                                        return;
                                    }
                                    ownerName[0] = otarget.getName();
                                    subtask.run();
                            	});
                            } else {
                            	ownerName[0] = newOwner.getName();
                                subtask.run();
                            }
            				
            			} else {
            				
            				// Check if the claim exists
                			if(!instance.getMain().getClaimsNameFromOwner(targetName[0]).contains(args[2])) {
                				player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
                				return;
                			}
                			
                			// Check new owner
                            Player newOwner = Bukkit.getPlayer(args[3]);
                            String[] ownerName = {""};
                            
                            // Create runnable
                            Runnable subtask = () -> {
                            	instance.executeSync(() -> {
                        			Claim claim = instance.getMain().getClaimByName(args[2], uuid[0]);
                            		instance.getMain().setOwner(ownerName[0], claim)
                	        			.thenAccept(success -> {
                	        				if (success) {
                	        					sender.sendMessage(instance.getLanguage().getMessage("setowner-claim-other-success").replace("%owner%", ownerName[0])
                	        							.replace("%old-owner%", targetName[0])
                	        							.replace("%claim-name%", claim.getName()));
                	        				}
                	        			})
                	                    .exceptionally(ex -> {
                	                        return null;
                	                    });
                            	});
                            };
                            
                            if (newOwner == null) {
                            	instance.getOfflinePlayer(args[1], otarget -> {
                                    if (otarget == null || !otarget.hasPlayedBefore()) {
                                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                                        return;
                                    }
                                    ownerName[0] = otarget.getName();
                                    subtask.run();
                            	});
                            } else {
                            	ownerName[0] = newOwner.getName();
                                subtask.run();
                            }
            			}
                	});
                };
                
                if (target == null) {
                	instance.getOfflinePlayer(args[1], otarget -> {
                        if (otarget == null || !otarget.hasPlayedBefore()) {
                        	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[1]));
                            return;
                        }
                        targetName[0] = otarget.getName();
                        uuid[0] = otarget.getUniqueId();
                        task.run();
                	});
                } else {
                    targetName[0] = target.getName();
                    uuid[0] = target.getUniqueId();
                    task.run();
                }
    			return;
    		}
    		if(args[0].equalsIgnoreCase("player")) {
    			if(args[1].equalsIgnoreCase("main")) {
        			if(!instance.getMain().getClaimsOwners().contains(args[2])) {
        				player.sendMessage(instance.getLanguage().getMessage("player-does-not-have-claim"));
        				return;
        			}
        			if(!instance.getMain().getClaimsNameFromOwner(args[2]).contains(args[3])) {
        				player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
        				return;
        			}
                	instance.getOfflinePlayer(args[2], p -> {
                		instance.executeSync(() -> {
                			Claim claim = instance.getMain().getClaimByName(args[3], p.getUniqueId());
                			new AdminGestionClaimMainGui(player,claim,instance);
                		});
                	});
            		return;
    			}
    			if(args[1].equalsIgnoreCase("tp")) {
        			if(!instance.getMain().getClaimsOwners().contains(args[2])) {
        				player.sendMessage(instance.getLanguage().getMessage("player-does-not-have-claim"));
        				return;
        			}
        			if(!instance.getMain().getClaimsNameFromOwner(args[2]).contains(args[3])) {
        				player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
        				return;
        			}
        			instance.getOfflinePlayer(args[2], p -> {
        				instance.executeSync(() -> {
                			Claim claim = instance.getMain().getClaimByName(args[3], p.getUniqueId());
                			Location loc = claim.getLocation();
                    		if(instance.isFolia()) {
                    			player.teleportAsync(loc);
                    		} else {
                    			player.teleport(loc);
                    		}
                    		player.sendMessage(instance.getLanguage().getMessage("player-teleport-to-other-claim-aclaim").replace("%name%", args[3]).replace("%player%", args[2]));
        				});
        			});
            		return;
        		}
        		if(args[1].equalsIgnoreCase("unclaim")) {
        			if(!instance.getMain().getClaimsOwners().contains(args[2])) {
        				player.sendMessage(instance.getLanguage().getMessage("player-does-not-have-claim"));
        				return;
        			}
        			if(args[3].equalsIgnoreCase("*")) {
        				instance.getMain().deleteAllClaims(args[2])
        					.thenAccept(success -> {
        						if(success) {
                					player.sendMessage(instance.getLanguage().getMessage("player-unclaim-other-all-claim-aclaim").replace("%player%", args[2]));
                    				Player target = Bukkit.getPlayer(args[2]);
                    				if(target != null) {
                    					target.sendMessage(instance.getLanguage().getMessage("player-all-claim-unclaimed-by-admin").replace("%player%", player.getName()));
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
        			if(!instance.getMain().getClaimsNameFromOwner(args[2]).contains(args[3])) {
        				player.sendMessage(instance.getLanguage().getMessage("claim-player-not-found"));
        				return;
        			}
        			instance.getOfflinePlayer(args[2], p -> {
        				instance.executeSync(() -> {
                			Claim claim = instance.getMain().getClaimByName(args[3], p.getUniqueId());
                			instance.getMain().deleteClaim(claim)
                				.thenAccept(success -> {
                					if (success) {
                        				player.sendMessage(instance.getLanguage().getMessage("player-unclaim-other-claim-aclaim").replace("%name%", args[3]).replace("%player%", args[2]));
                        				Player target = Bukkit.getPlayer(args[2]);
                        				if(target != null) {
                        					target.sendMessage(instance.getLanguage().getMessage("player-claim-unclaimed-by-admin").replace("%name%", args[3]).replace("%player%", player.getName()));
                        				}
                					} else {
                						player.sendMessage(instance.getLanguage().getMessage("error"));
                					}
                				})
                		        .exceptionally(ex -> {
                		            ex.printStackTrace();
                		            return null;
                		        });
        				});
        			});
        			return;
        		}
			}
    	}
    	if(args[0].equalsIgnoreCase("cplayer")) {
    		if(args[1].equalsIgnoreCase("set-claim-distance")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("claim-distance-must-be-positive")); // Same message
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("claim-distance-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".claim-distance", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "claim-distance", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-claim-distance-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-max-chunks-total")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-total-must-be-positive")); // Same message
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-total-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".max-chunks-total", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-chunks-total", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-chunks-total-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-max-chunks-per-claim")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-per-claim-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-per-claim-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".max-chunks-per-claim", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-chunks-per-claim", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-chunks-per-claim-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-claim-cost")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".claim-cost", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "claim-cost", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-claim-cost-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-chunk-cost")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".chunk-cost", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "chunk-cost", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-chunk-cost-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-chunk-cost-multiplier")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-multiplier-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-multiplier-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".chunk-cost-multiplier", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "chunk-cost-multiplier", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-chunk-cost-multiplier-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        Double amount;
                        try {
                            amount = Double.parseDouble(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-multiplier-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-multiplier-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".claim-cost-multiplier", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "claim-cost-multiplier", amount);
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-claim-cost-multiplier-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-members")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        int amount;
                        try {
                            amount = Integer.parseInt(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("member-limit-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("member-limit-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".max-members", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-members", Double.valueOf(amount));
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-member-limit-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-limit")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        int amount;
                        try {
                            amount = Integer.parseInt(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".max-claims", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-claims", Double.valueOf(amount));
                        }
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-claim-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-radius")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        int amount;
                        try {
                            amount = Integer.parseInt(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-radius-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-radius-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".max-radius-claims", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-radius-claims", Double.valueOf(amount));
                        }
                        
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-radius-claim-success").replace("%player%", targetName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-delay")) {
    			instance.getOfflinePlayer(args[2], p -> {
    				instance.executeSync(() -> {
                        if (p == null || !p.hasPlayedBefore()) {
                        	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                            return;
                        }
                        String targetName = p.getName();
                        int amount;
                        try {
                            amount = Integer.parseInt(args[3]);
                            if(amount < 0) {
                            	sender.sendMessage(instance.getLanguage().getMessage("teleportation-delay-must-be-positive"));
                                return;
                            }
                        } catch (NumberFormatException e) {
                        	sender.sendMessage(instance.getLanguage().getMessage("teleportation-delay-must-be-number"));
                            return;
                        }
                        
                        File configFile = new File(instance.getDataFolder(), "config.yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                        config.set("players."+targetName+".teleportation-delay", amount);
                        if(p.isOnline()) {
                        	UUID targetId = p.getUniqueId();
        	                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "teleportation-delay", Double.valueOf(amount));
                        }
                        
                        try {
                        	config.save(configFile);
                        	sender.sendMessage(instance.getLanguage().getMessage("set-player-teleportation-delay-success").replace("%player%", targetName).replace("%amount%", String.valueOf(args[3])));
        				} catch (IOException e) {
        					e.printStackTrace();
        				}
    				});
    			});
                return;
        	}
        	if(args[1].equalsIgnoreCase("add-limit")) {
    			Player target = Bukkit.getPlayer(args[2]);
    			if(target == null) {
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
    				return;
    			}
    			String name = target.getName();
    			UUID targetId = target.getUniqueId();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetId);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxClaims()+amount;
                config.set("players."+name+".max-claims", new_amount);
                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-claims", Double.valueOf(new_amount));
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-claim-success").replace("%player%", name).replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(new_amount))));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("add-radius")) {
    			Player target = Bukkit.getPlayer(args[2]);
    			if(target == null) {
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
    				return;
    			}
    			String name = target.getName();
    			UUID targetId = target.getUniqueId();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetId);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-radius-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-radius-must-be-number"));
                    return;
                }

                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxRadiusClaims()+amount;
                config.set("players."+name+".max-radius-claims", new_amount);
                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-radius-claims", Double.valueOf(new_amount));
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-radius-claim-success").replace("%player%", name).replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(new_amount))));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("add-members")) {
    			Player target = Bukkit.getPlayer(args[2]);
    			if(target == null) {
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
    				return;
    			}
    			String name = target.getName();
    			UUID targetId = target.getUniqueId();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetId);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("member-limit-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("member-limit-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxMembers()+amount;
                config.set("players."+name+".max-members", new_amount);
                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-members", Double.valueOf(new_amount));
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-member-limit-success").replace("%player%", name).replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(new_amount))));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("add-chunks-per-claim")) {
    			Player target = Bukkit.getPlayer(args[2]);
    			if(target == null) {
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
    				return;
    			}
    			String name = target.getName();
    			UUID targetId = target.getUniqueId();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetId);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-per-claim-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-per-claim-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxChunksPerClaim()+amount;
                config.set("players."+name+".max-chunks-per-claim", new_amount);
                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-chunks-per-claim", Double.valueOf(new_amount));
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-chunks-per-claim-success").replace("%player%", name).replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(new_amount))));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("add-chunks-total")) {
    			Player target = Bukkit.getPlayer(args[2]);
    			if(target == null) {
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online").replace("%player%", args[2]));
    				return;
    			}
    			String name = target.getName();
    			UUID targetId = target.getUniqueId();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetId);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-total-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-total-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxChunksTotal()+amount;
                config.set("players."+name+".max-chunks-total", new_amount);
                instance.getPlayerMain().updatePlayerConfigSettings(targetId, "max-chunks-total", Double.valueOf(new_amount));
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-chunks-total-success").replace("%player%", name).replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(new_amount))));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	return;
    	}
    	if(args[0].equalsIgnoreCase("group")) {
    		
    		if(!instance.getSettings().getGroups().contains(args[2])) {
    			sender.sendMessage(instance.getLanguage().getMessage("group-does-not-exists"));
    			return;
    		}
    		
    		if(args[1].equalsIgnoreCase("set-claim-distance")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-distance-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-distance-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".claim-distance", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("claim-distance", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-claim-distance-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-max-chunks-total")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-total-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-total-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".max-chunks-total", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("max-chunks-total", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-max-chunks-total-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-max-chunks-per-claim")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-per-claim-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("max-chunks-per-claim-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".max-chunks-per-claim", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("max-chunks-per-claim", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-max-chunks-per-claim-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-claim-cost")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".claim-cost", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("claim-cost", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-claim-cost-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-chunk-cost")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".chunk-cost", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("chunk-cost", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-chunk-cost-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-multiplier-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-cost-multiplier-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".claim-cost-multiplier", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("claim-cost-multiplier", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-claim-cost-multiplier-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-chunk-cost-multiplier")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-multiplier-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("chunk-cost-multiplier-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".chunk-cost-multiplier", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("chunk-cost-multiplier", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-chunk-cost-multiplier-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-members")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("member-limit-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("member-limit-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".max-members", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("max-members", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-member-limit-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-limit")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".max-claims", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("max-claims", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-max-claim-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-radius")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-radius-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-limit-radius-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".max-radius-claims", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("max-radius-claims", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-max-radius-claim-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-delay")) {
                Double amount;
                try {
                    amount = Double.parseDouble(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("teleportation-delay-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("teleportation-delay-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("groups."+args[2]+".teleportation-delay", amount);
                instance.getSettings().getGroupsSettings().get(args[2]).put("teleportation-delay", amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-group-teleportation-delay-success").replace("%group%", args[2]).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    	}
    	instance.getMain().getHelp(sender, args[0], "scs");
    }
    
    /**
     * Provides primary completions for the first argument.
     *
     * @param args the args of the command
     * @return a list of possible primary completions
     */
    private List<String> getPrimaryCompletions(String[] args) {
    	String partialInput = args.length > 0 ? args[0].toLowerCase() : "";
        List<String> completions = List.of("reload", "config-reload", "transfer", "player", "cplayer", "group", "forceunclaim", "setowner", "set-lang", 
                "reset-all-player-claims-settings", "reset-all-admin-claims-settings","admin","import-griefprevention","setexpulsionlocation");
        return completions.stream()
    	        .filter(c -> c.toLowerCase().startsWith(partialInput))
    	        .collect(Collectors.toList());
    }

    /**
     * Provides secondary completions for the second argument based on the first argument.
     *
     * @param sender the sender of the command
     * @param args the arguments of the command
     * @return a list of possible secondary completions
     */
    private List<String> getSecondaryCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String command = args[0].toLowerCase();
        String partialInput = args.length > 1 ? args[1].toLowerCase() : "";
        switch (command) {
            case "setowner":
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                break;
            case "group":
            case "cplayer":
                completions.addAll(List.of("add-limit", "add-radius", "add-members", "add-chunks-per-claim", "add-chunks-total", "set-limit", "set-radius", "set-delay",
                        "set-members", "set-claim-cost", "set-claim-cost-multiplier", "set-max-chunks-per-claim",
                        "set-claim-distance", "set-max-chunks-total", "set-chunk-cost", "set-chunk-cost-multiplier"));
                break;
            case "player":
            	completions.addAll(List.of("tp", "unclaim", "main", "list"));
            	break;
            default:
                break;
        }
        return completions.stream()
    	        .filter(c -> c.toLowerCase().startsWith(partialInput))
    	        .collect(Collectors.toList());
    }

    /**
     * Provides tertiary completions for the third argument based on the first and second arguments.
     *
     * @param sender the sender of the command
     * @param args the arguments of the command
     * @return a list of possible tertiary completions
     */
    private List<String> getTertiaryCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String command = args[0].toLowerCase();
        String secondArg = args[1].toLowerCase();
        String partialInput = args.length > 2 ? args[2].toLowerCase() : "";
        switch (command) {
            case "player":
                if (secondArg.equals("tp") || secondArg.equals("unclaim") || secondArg.equals("main") || secondArg.equals("list")) {
                    completions.addAll(new HashSet<>(instance.getMain().getClaimsOwners()));
                }
                break;
            case "cplayer":
            	completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            	break;
            case "group":
                completions.addAll(instance.getSettings().getGroups());
                break;
            case "setowner":
            	completions.add("*");
            	completions.addAll(instance.getMain().getClaimsNameFromOwner(args[1]));
            	break;
            default:
                break;
        }
        return completions.stream()
    	        .filter(c -> c.toLowerCase().startsWith(partialInput))
    	        .collect(Collectors.toList());
    }
    
    /**
     * Provides four completions for the third argument based on the first, second and third arguments.
     *
     * @param sender the sender of the command
     * @param args the arguments of the command
     * @return a list of possible tertiary completions
     */
    private List<String> getFourCompletions(CommandSender sender, String[] args) {
        List<String> completions = new ArrayList<>();
        String command = args[0].toLowerCase();
        String partialInput = args.length > 3 ? args[3].toLowerCase() : "";
        switch (command) {
            case "setowner":
            	completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            	break;
            default:
                break;
        }
        return completions.stream()
    	        .filter(c -> c.toLowerCase().startsWith(partialInput))
    	        .collect(Collectors.toList());
    }
}
