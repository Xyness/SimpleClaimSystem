package fr.xyness.SCS.Commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimMainGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsOwnerGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionClaimsProtectedAreasGui;
import fr.xyness.SCS.Guis.AdminGestion.AdminGestionMainGui;

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
                completions.addAll(getPrimaryCompletions());
            } else if (args.length == 2) {
                completions.addAll(getSecondaryCompletions(sender, args));
            } else if (args.length == 3) {
                completions.addAll(getTertiaryCompletions(sender, args));
            } else if (args.length == 4 && args[0].equalsIgnoreCase("player") && (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("unclaim"))) {
                completions.addAll(instance.getMain().getClaimsNameFromOwner(args[2]));
                if (args[1].equalsIgnoreCase("unclaim")) {
                    completions.add("*");
                }
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
    		if(instance.loadConfig(true)) {
    			sender.sendMessage(instance.getLanguage().getMessage("reload-complete"));
    		}
    		return;
    	}
    	if(args[0].equalsIgnoreCase("config-reload")) {
    		if(instance.reloadOnlyConfig()) {
    			sender.sendMessage(instance.getLanguage().getMessage("reload-complete"));
    		}
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
        		Chunk chunk = player.getLocation().getChunk();
        		if(!instance.getMain().checkIfClaimExists(chunk)) {
        			player.sendMessage(instance.getLanguage().getMessage("free-territory"));
        			return;
        		}
        		Claim claim = instance.getMain().getClaim(chunk);
        		final String tName = targetName;
        		instance.getMain().setOwner(tName, claim)
        			.thenAccept(success -> {
        				if (success) {
        					sender.sendMessage(instance.getLanguage().getMessage("setowner-success").replace("%owner%", tName));
        				}
        			})
                    .exceptionally(ex -> {
                        return null;
                    });
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
        			OfflinePlayer p = Bukkit.getOfflinePlayer(args[2]);
        			Claim claim = instance.getMain().getClaimByName(args[3], p.getUniqueId());
        			new AdminGestionClaimMainGui(player,claim,instance);
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
        			OfflinePlayer p = Bukkit.getOfflinePlayer(args[2]);
        			Claim claim = instance.getMain().getClaimByName(args[3], p.getUniqueId());
        			Location loc = claim.getLocation();
            		if(instance.isFolia()) {
            			player.teleportAsync(loc);
            		} else {
            			player.teleport(loc);
            		}
            		player.sendMessage(instance.getLanguage().getMessage("player-teleport-to-other-claim-aclaim").replace("%name%", args[3]).replace("%player%", args[2]));
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
        			OfflinePlayer p = Bukkit.getOfflinePlayer(args[2]);
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
        			return;
        		}
			}
    	}
    	if(args[0].equalsIgnoreCase("cplayer")) {
    		if(args[1].equalsIgnoreCase("set-claim-distance")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".claim-distance", amount);
                if(target != null && target.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
	                int nb = (int) Math.round(amount);
	                cTarget.setClaimDistance(nb);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-claim-distance-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-max-chunks-total")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".max-chunks-total", amount);
                if(target != null && target.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
	                int nb = (int) Math.round(amount);
	                cTarget.setMaxChunksTotal(nb);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-chunks-total-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-max-chunks-per-claim")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".max-chunks-per-claim", amount);
                if(target != null && target.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
	                int nb = (int) Math.round(amount);
	                cTarget.setMaxChunksPerClaim(nb);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-chunks-per-claim-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-claim-cost")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".claim-cost", amount);
                if(target != null && target.isOnline()) {
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
	                cTarget.setClaimCost(amount);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-claim-cost-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".claim-cost-multiplier", amount);
                if(target != null && target.isOnline()) {
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
                	cTarget.setClaimCostMultiplier(amount);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-claim-cost-multiplier-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
    		if(args[1].equalsIgnoreCase("set-members")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".max-members", amount);
                if(target != null && target.isOnline()) {
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
                	cTarget.setMaxMembers(amount);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-member-limit-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-limit")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".max-claims", amount);
                if(target != null && target.isOnline()) {
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
                	cTarget.setMaxClaims(amount);
                }
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-claim-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-radius")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".max-radius-claims", amount);
                if(target != null && target.isOnline()) {
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
                	cTarget.setMaxRadiusClaims(amount);
                }
                
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-max-radius-claim-success").replace("%player%", tName).replace("%amount%", instance.getMain().getNumberSeparate(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("set-delay")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	sender.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
                        return;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("players."+targetName+".teleportation-delay", amount);
                if(target != null && target.isOnline()) {
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
                	cTarget.setTeleportationDelay(amount);
                }
                
                try {
                	config.save(configFile);
                	final String tName = targetName;
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-teleportation-delay-success").replace("%player%", tName).replace("%amount%", String.valueOf(args[3])));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
        	}
        	if(args[1].equalsIgnoreCase("add-limit")) {
    			Player target = Bukkit.getPlayer(args[2]);
    			if(target == null) {
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online"));
    				return;
    			}
    			String name = target.getName();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxClaims()+amount;
                config.set("players."+name+".max-claims", new_amount);
                cTarget.setMaxClaims(new_amount);
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
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online"));
    				return;
    			}
    			String name = target.getName();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
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

                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxRadiusClaims()+amount;
                config.set("players."+name+".max-radius-claims", new_amount);
                cTarget.setMaxRadiusClaims(new_amount);
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
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online"));
    				return;
    			}
    			String name = target.getName();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxRadiusClaims()+amount;
                config.set("players."+name+".max-members", new_amount);
                cTarget.setMaxMembers(new_amount);
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
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online"));
    				return;
    			}
    			String name = target.getName();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxRadiusClaims()+amount;
                config.set("players."+name+".max-chunks-per-claim", new_amount);
                cTarget.setMaxChunksPerClaim(new_amount);
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
    				sender.sendMessage(instance.getLanguage().getMessage("player-not-online"));
    				return;
    			}
    			String name = target.getName();
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(target.getUniqueId());
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxRadiusClaims()+amount;
                config.set("players."+name+".max-chunks-total", new_amount);
                cTarget.setMaxChunksTotal(new_amount);
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
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
     * @return a list of possible primary completions
     */
    private List<String> getPrimaryCompletions() {
        return List.of("reload", "config-reload", "transfer", "player", "cplayer", "group", "forceunclaim", "setowner", "set-lang", 
                "reset-all-player-claims-settings", "reset-all-admin-claims-settings","admin","import-griefprevention");
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

        switch (command) {
            case "setowner":
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                break;
            case "group":
            case "cplayer":
                completions.addAll(List.of("add-limit", "add-radius", "add-members", "add-chunks-per-claim", "add-chunks-total", "set-limit", "set-radius", "set-delay",
                        "set-members", "set-claim-cost", "set-claim-cost-multiplier", "set-max-chunks-per-claim",
                        "set-claim-distance", "set-max-chunks-total"));
                break;
            case "player":
            	completions.addAll(List.of("tp", "unclaim", "main", "list"));
            	break;
            default:
                break;
        }
        return completions;
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

        switch (command) {
            case "player":
                if (secondArg.equals("tp") || secondArg.equals("unclaim") || secondArg.equals("main") || secondArg.equals("list")) {
                    completions.addAll(new HashSet<>(instance.getMain().getClaimsOwners()));
                }
                break;
            case "group":
                completions.addAll(instance.getSettings().getGroups());
                break;
            default:
                break;
        }
        return completions;
    }
}
