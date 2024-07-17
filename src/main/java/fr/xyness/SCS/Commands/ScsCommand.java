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
    		instance.getMain().resetAllClaimsSettings()
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
    		instance.getMain().resetAllOwnerClaimsSettings("admin")
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
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
                    if (otarget == null || !otarget.hasPlayedBefore()) {
                    	player.sendMessage(instance.getLanguage().getMessage("player-never-played").replace("%player%", args[2]));
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
    	if(args[0].equalsIgnoreCase("set-max-length-claim-name")) {
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if(amount < 1) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-name-length-must-be-positive"));
                    return;
                }
            } catch (NumberFormatException e) {
            	sender.sendMessage(instance.getLanguage().getMessage("claim-name-length-must-be-number"));
                return;
            }
            
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("max-length-claim-name", amount);
            instance.getSettings().addSetting("max-length-claim-name", args[1]);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("set-max-length-claim-name-success").replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(args[1]))));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("set-max-length-claim-description")) {
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if(amount < 1) {
                	sender.sendMessage(instance.getLanguage().getMessage("claim-description-length-must-be-positive"));
                    return;
                }
            } catch (NumberFormatException e) {
            	sender.sendMessage(instance.getLanguage().getMessage("claim-description-length-must-be-number"));
                return;
            }
            
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("max-length-claim-description", amount);
            instance.getSettings().addSetting("max-length-claim-description", args[1]);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("set-max-length-claim-description-success").replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(args[1]))));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("set-lang")) {
    		instance.reloadLang(sender, args[1]);
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claims-visitors-off-visible")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claims-visitors-off-visible", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claims-visitors-off-visible", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claims visible (w Visitors setting off)").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claim-cost")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claim-cost", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claim-cost", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claim cost").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-preload-chunks")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("preload-chunks", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("preload-chunks", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Preload chunks").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-keep-chunks-loaded")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("keep-chunks-loaded", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("keep-chunks-loaded", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Keep chunks loaded").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claim-cost-multiplier")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claim-cost-multiplier", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claim-cost-multiplier", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claim cost multiplier").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-actionbar")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("enter-leave-messages", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("enter-leave-messages", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "ActionBar").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-protection-message")) {
    		if(args[1].equalsIgnoreCase("action_bar") || args[1].equalsIgnoreCase("title") || args[1].equalsIgnoreCase("subtitle") || args[1].equalsIgnoreCase("chat") || args[1].equalsIgnoreCase("bossbar")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("protection-message", args[1].toUpperCase());
                instance.getSettings().addSetting("protection-message", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Protection message").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-protection-message"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-chat")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("enter-leave-chat-messages", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("enter-leave-chat-messages", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Chat").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claim-fly-disabled-on-damage")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claim-fly-disabled-on-damage", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claim-fly-disabled-on-damage", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claim fly disabled on damage").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claim-fly-message-auto-fly")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claim-fly-message-auto-fly", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claim-fly-message-auto-fly", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claim fly message auto-fly").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-auto-claim")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("auto-claim", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("auto-claim", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "ActionBar").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-title-subtitle")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("enter-leave-title-messages", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("enter-leave-title-messages", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Title/Subtitle").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-economy")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("economy", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("economy", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Economy").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claim-confirmation")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claim-confirmation", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claim-confirmation", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claim confirmation").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-claim-particles")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("claim-particles", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("claim-particles", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Claim particles").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-max-sell-price")) {
            int amount;
            try {
                amount = Integer.parseInt(args[1]);
                if(amount < 1) {
                	sender.sendMessage(instance.getLanguage().getMessage("max-sell-price-must-be-positive"));
                    return;
                }
            } catch (NumberFormatException e) {
            	sender.sendMessage(instance.getLanguage().getMessage("max-sell-price-must-be-number"));
                return;
            }
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("max-sell-price", args[1]);
            instance.getSettings().addSetting("max-sell-price", args[1]);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Max sell price").replace("%value%", instance.getMain().getNumberSeparate(args[1])));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("set-bossbar")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("bossbar", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("bossbar", args[1]);
                if(args[1].equalsIgnoreCase("false")) {
                	Bukkit.getOnlinePlayers().forEach(p -> instance.getBossBars().checkBossBar(p).setVisible(false));
                } else {
                	Bukkit.getOnlinePlayers().forEach(p -> instance.getBossBars().activeBossBar(p, p.getLocation().getChunk()));
                }
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "BossBar").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-bossbar-color")) {
    		String bcolor = args[1].toUpperCase();
    		BarColor color;
            try {
            	color = BarColor.valueOf(bcolor);
            } catch (IllegalArgumentException e) {
    			sender.sendMessage(instance.getLanguage().getMessage("bossbar-color-incorrect"));
    			return;
            }
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("bossbar-settings.color", bcolor);
            instance.getSettings().addSetting("bossbar-color", bcolor);
            instance.getBossBars().setBossBarColor(color);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "BossBar color").replace("%value%", args[1].toUpperCase()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("set-bossbar-style")) {
    		String bstyle = args[1].toUpperCase();
    		BarStyle style;
            try {
            	style = BarStyle.valueOf(bstyle);
            } catch (IllegalArgumentException e) {
    			sender.sendMessage(instance.getLanguage().getMessage("bossbar-style-incorrect"));
    			return;
            }
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            config.set("bossbar-settings.style", bstyle);
            instance.getSettings().addSetting("bossbar-style", bstyle);
            instance.getBossBars().setBossBarStyle(style);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "BossBar style").replace("%value%", args[1].toUpperCase()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("set-teleportation")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("teleportation", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("teleportation", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Teleportation").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-teleportation-moving")) {
    		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("teleportation-delay-moving", Boolean.parseBoolean(args[1]));
                instance.getSettings().addSetting("teleportation-delay-moving", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Teleportation moving").replace("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("add-disabled-world")) {
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> worlds = config.getStringList("worlds-disabled");
            if(worlds.contains(args[1])) {
            	sender.sendMessage(instance.getLanguage().getMessage("world-already-in-list"));
            	return;
            }
            worlds.add(args[1]);
            config.set("worlds-disabled", worlds);
            instance.getSettings().setDisabledWorlds(new HashSet<>(worlds));
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("world-list-changed-via-command").replace("%name%", args[1]));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("remove-disabled-world")) {
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> worlds = config.getStringList("worlds-disabled");
            if(!worlds.contains(args[1])) {
            	sender.sendMessage(instance.getLanguage().getMessage("world-not-in-list"));
            	return;
            }
            worlds.remove(args[1]);
            config.set("worlds-disabled", worlds);
            instance.getSettings().setDisabledWorlds(new HashSet<>(worlds));
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("world-list-changeda-via-command").replace("%name%", args[1]));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("add-blocked-interact-block")) {
    		String material = args[1].toUpperCase();
    		Material mat = Material.getMaterial(material);
    		if(mat == null) {
    			sender.sendMessage(instance.getLanguage().getMessage("mat-incorrect"));
    			return;
    		}
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> containers = config.getStringList("blocked-interact-blocks");
            if(containers.contains(material.toUpperCase())) {
            	sender.sendMessage(instance.getLanguage().getMessage("material-already-in-list"));
            	return;
            }
            containers.add(material.toUpperCase());
            config.set("blocked-interact-blocks", containers);
            instance.getSettings().setRestrictedContainers(containers);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-list-changed-via-command").replace("%setting%", "Blocked interact blocks").replace("%material%", material.toUpperCase()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("add-blocked-entity")) {
			String material = args[1].toUpperCase();
    		EntityType e = EntityType.fromName(material);
    		if(e == null) {
    			sender.sendMessage(instance.getLanguage().getMessage("entity-incorrect"));
    			return;
    		}
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> containers = config.getStringList("blocked-entities");
            if(containers.contains(material.toUpperCase())) {
            	sender.sendMessage(instance.getLanguage().getMessage("entity-already-in-list"));
            	return;
            }
            containers.add(material.toUpperCase());
            config.set("blocked-entities", containers);
            instance.getSettings().setRestrictedEntityType(containers);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-list-changed-via-command-entity").replace("%setting%", "Blocked entities").replace("%entity%", material.toUpperCase()));
			} catch (IOException ee) {
				ee.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("add-blocked-item")) {
			String material = args[1].toUpperCase();
    		Material mat = Material.getMaterial(material);
    		if(mat == null) {
    			sender.sendMessage(instance.getLanguage().getMessage("mat-incorrect"));
    			return;
    		}
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> items = config.getStringList("blocked-items");
            if(items.contains(material.toUpperCase())) {
            	sender.sendMessage(instance.getLanguage().getMessage("material-already-in-list"));
            	return;
            }
            items.add(material.toUpperCase());
            config.set("blocked-items", items);
            instance.getSettings().setRestrictedItems(items);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-list-changed-via-command").replace("%setting%", "Blocked items").replace("%material%", material.toUpperCase()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("remove-blocked-item")) {
			String material = args[1].toUpperCase();
    		Material mat = Material.getMaterial(material);
    		if(mat == null) {
    			sender.sendMessage(instance.getLanguage().getMessage("mat-incorrect"));
    			return;
    		}
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> items = config.getStringList("blocked-items");
            if(!items.contains(material.toUpperCase())) {
            	sender.sendMessage(instance.getLanguage().getMessage("material-not-in-list"));
            	return;
            }
            items.remove(material.toUpperCase());
            config.set("blocked-items", items);
            instance.getSettings().setRestrictedItems(items);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-list-changeda-via-command").replace("%setting%", "Blocked items").replace("%material%", material.toUpperCase()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("remove-blocked-interact-block")) {
			String material = args[1].toUpperCase();
    		Material mat = Material.getMaterial(material);
    		if(mat == null) {
    			sender.sendMessage(instance.getLanguage().getMessage("mat-incorrect"));
    			return;
    		}
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> containers = config.getStringList("blocked-interact-blocks");
            if(!containers.contains(material.toUpperCase())) {
            	sender.sendMessage(instance.getLanguage().getMessage("material-not-in-list"));
            	return;
            }
            containers.remove(material.toUpperCase());
            config.set("blocked-interact-blocks", containers);
            instance.getSettings().setRestrictedContainers(containers);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-list-changeda-via-command").replace("%setting%", "Blocked interact blocks").replace("%material%", material.toUpperCase()));
			} catch (IOException e) {
				e.printStackTrace();
			}
            return;
    	}
    	if(args[0].equalsIgnoreCase("remove-blocked-entity")) {
    		String material = args[1].toUpperCase();
    		EntityType e = EntityType.fromName(material);
    		if(e == null) {
    			sender.sendMessage(instance.getLanguage().getMessage("entity-incorrect"));
    			return;
    		}
            File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
            List<String> containers = config.getStringList("blocked-entities");
            if(!containers.contains(material.toUpperCase())) {
            	sender.sendMessage(instance.getLanguage().getMessage("entity-not-in-list"));
            	return;
            }
            containers.remove(material.toUpperCase());
            config.set("blocked-entities", containers);
            instance.getSettings().setRestrictedEntityType(containers);
            try {
            	config.save(configFile);
            	sender.sendMessage(instance.getLanguage().getMessage("setting-list-changeda-via-command-entity").replace("%setting%", "Blocked entities").replace("%entity%", material.toUpperCase()));
			} catch (IOException ee) {
				ee.printStackTrace();
			}
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
    	if(args[0].equalsIgnoreCase("set-status-setting")) {
			String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
    		if(instance.getGuis().isAPerm(perm)) {
        		if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
	                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("status-settings."+perm, Boolean.parseBoolean(args[2]));
	                instance.getSettings().getStatusSettings().put(perm, Boolean.parseBoolean(args[2]));
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Status Setting '"+perm+"'").replace("%value%", args[2]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return;
        		}
        		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
        		return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-incorrect"));
    		return;
    	}
    	if(args[0].equalsIgnoreCase("set-default-value")) {
			String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
    		if(instance.getGuis().isAPerm(perm)) {
        		if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
	                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("default-values-settings."+perm, Boolean.parseBoolean(args[2]));
	                instance.getSettings().getDefaultValues().put(perm, Boolean.parseBoolean(args[2]));
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(instance.getLanguage().getMessage("setting-changed-via-command").replace("%setting%", "Default Values Setting '"+perm+"'").replace("%value%", args[2]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return;
        		}
        		sender.sendMessage(instance.getLanguage().getMessage("setting-must-be-boolean"));
        		return;
    		}
    		sender.sendMessage(instance.getLanguage().getMessage("setting-incorrect"));
    		return;
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
        			Claim claim = instance.getMain().getClaimByName(args[3], args[2]);
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
        			Claim claim = instance.getMain().getClaimByName(args[3], args[2]);
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
        			Claim claim = instance.getMain().getClaimByName(args[3], args[2]);
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
    	if(args[0].equalsIgnoreCase("player")) {
    		if(args[1].equalsIgnoreCase("set-claim-distance")) {
    			Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
	                CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
                    OfflinePlayer otarget = instance.getPlayerMain().getOfflinePlayer(args[2]);
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
                	CPlayer cTarget = instance.getPlayerMain().getCPlayer(targetName);
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
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(name);
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
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(name);
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
                cTarget.setMaxClaims(new_amount);
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
    			CPlayer cTarget = instance.getPlayerMain().getCPlayer(name);
                int amount;
                try {
                    amount = Integer.parseInt(args[3]);
                    if(amount < 0) {
                    	sender.sendMessage(instance.getLanguage().getMessage("member-limit-radius-must-be-positive"));
                        return;
                    }
                } catch (NumberFormatException e) {
                	sender.sendMessage(instance.getLanguage().getMessage("member-limit-radius-must-be-number"));
                    return;
                }
                
                File configFile = new File(instance.getPlugin().getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                int new_amount = cTarget.getMaxRadiusClaims()+amount;
                config.set("players."+name+".max-members", new_amount);
                cTarget.setMaxClaims(new_amount);
                try {
                	config.save(configFile);
                	sender.sendMessage(instance.getLanguage().getMessage("set-player-member-limit-success").replace("%player%", name).replace("%amount%", instance.getMain().getNumberSeparate(String.valueOf(new_amount))));
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
        return List.of("transfer", "player", "group", "forceunclaim", "setowner", "set-lang", "set-actionbar", "set-auto-claim", 
        		"set-title-subtitle", "set-economy", "set-claim-confirmation", "set-claim-particles", "set-max-sell-price", "set-bossbar", "set-bossbar-color",
        		"set-bossbar-style", "set-teleportation", "set-teleportation-moving", "add-blocked-interact-block", "add-blocked-entity", "add-blocked-item",
                "remove-blocked-interact-block", "remove-blocked-item", "remove-blocked-entity", "add-disabled-world", "remove-disabled-world", "set-status-setting", 
                "set-default-value", "set-max-length-claim-description", "set-max-length-claim-name", "set-claims-visitors-off-visible", "set-claim-cost", 
                "set-claim-cost-multiplier", "set-chat", "set-protection-message", "set-claim-fly-message-auto-fly", "set-claim-fly-disabled-on-damage",
                "reset-all-player-claims-settings", "reset-all-admin-claims-settings","admin", "set-keep-chunks-loaded", "set-preload-chunks","import-griefprevention");
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
            case "set-protection-message":
                completions.addAll(List.of("ACTION_BAR", "BOSSBAR", "TITLE", "SUBTITLE", "CHAT"));
                break;
            case "setowner":
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                break;
            case "set-actionbar":
            case "set-title-subtitle":
            case "set-economy":
            case "set-claim-confirmation":
            case "set-bossbar":
            case "set-teleportation":
            case "set-teleportation-moving":
            case "autoclaim":
            case "set-claims-visitors-off-visible":
            case "set-claim-cost":
            case "set-claim-cost-multiplier":
            case "set-chat":
            case "set-claim-particles":
            case "set-claim-fly-disabled-on-damage":
            case "set-claim-fly-message-auto-fly":
                completions.addAll(List.of("true", "false"));
                break;
            case "set-bossbar-color":
                completions.addAll(List.of(BarColor.values()).stream().map(BarColor::toString).collect(Collectors.toList()));
                break;
            case "set-bossbar-style":
                completions.addAll(List.of(BarStyle.values()).stream().map(BarStyle::toString).collect(Collectors.toList()));
                break;
            case "remove-disabled-world":
                completions.addAll(instance.getSettings().getDisabledWorlds());
                break;
            case "set-status-setting":
            case "set-default-value":
                completions.addAll(instance.getGuis().getPerms());
                break;
            case "add-blocked-interact-block":
            case "add-blocked-item":
                completions.addAll(List.of(Material.values()).stream().map(Material::toString).collect(Collectors.toList()));
                break;
            case "add-blocked-entity":
                completions.addAll(List.of(EntityType.values()).stream().map(EntityType::toString).collect(Collectors.toList()));
                break;
            case "remove-blocked-interact-block":
                completions.addAll(instance.getSettings().getRestrictedContainersString());
                break;
            case "remove-blocked-entity":
                completions.addAll(instance.getSettings().getRestrictedEntitiesString());
                break;
            case "remove-blocked-item":
                completions.addAll(instance.getSettings().getRestrictedItemsString());
                break;
            case "group":
            case "player":
                completions.addAll(List.of("add-limit", "add-radius", "add-members", "set-limit", "set-radius", "set-delay",
                        "set-members", "set-claim-cost", "set-claim-cost-multiplier", "set-max-chunks-per-claim", "tp", "unclaim", "main", "list",
                        "set-claim-distance", "set-max-chunks-total"));
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
            case "set-status-setting":
            case "set-default-value":
                String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
                if (instance.getGuis().isAPerm(perm)) {
                    completions.addAll(List.of("true", "false"));
                }
                break;
            case "player":
                if (secondArg.equals("tp") || secondArg.equals("unclaim") || secondArg.equals("main")) {
                    completions.addAll(new HashSet<>(instance.getMain().getClaimsOwners()));
                } else {
                    completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
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
