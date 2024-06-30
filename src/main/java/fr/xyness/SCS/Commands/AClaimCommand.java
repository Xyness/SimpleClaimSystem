package fr.xyness.SCS.Commands;

import java.io.File;
import java.io.IOException;
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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.AdminClaimGui;
import fr.xyness.SCS.Guis.AdminClaimListGui;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Listeners.ClaimEventsEnterLeave;

/**
 * This class handles admin commands related to claims.
 */
public class AClaimCommand implements CommandExecutor, TabCompleter {
	
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
                completions.addAll(List.of("convert", "setdesc", "settings", "setname", "members", "tp", "ptp", "list", "player", "group",
                        "forceunclaim", "setowner", "set-lang", "set-actionbar", "set-auto-claim", "set-title-subtitle", "set-economy",
                        "set-claim-confirmation", "set-claim-particles", "set-max-sell-price", "set-bossbar", "set-bossbar-color",
                        "set-teleportation", "set-teleportation-moving", "add-blocked-interact-block", "add-blocked-entity", "add-blocked-item",
                        "remove-blocked-interact-block", "remove-blocked-item", "remove-blocked-entity", "add-disabled-world",
                        "remove-disabled-world", "set-status-setting", "set-default-value", "set-max-length-claim-description",
                        "set-max-length-claim-name", "set-claims-visitors-off-visible", "set-claim-cost", "set-claim-cost-multiplier", "ban", "unban",
                        "bans", "set-chat", "set-protection-message", "set-claim-fly-message-auto-fly", "set-claim-fly-disabled-on-damage"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("set-protection-message")) {
                completions.addAll(List.of("ACTION_BAR", "BOSSBAR", "TITLE", "SUBTITLE", "CHAT"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("ban")) {
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("bans")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("unban")) {
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("unban")) {
                Chunk chunk = ClaimMain.getChunkByClaimName("admin", args[1]);
                completions.addAll(ClaimMain.getClaimBans(chunk));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("ban")) {
                Player player = (Player) sender;
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(player.getName());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("add")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove") && !args[1].equals("*")) {
                Chunk chunk = ClaimMain.getChunkByClaimName("admin", args[1]);
                completions.addAll(ClaimMain.getClaimMembers(chunk));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
                completions.addAll(ClaimMain.getAllMembersOfAllPlayerClaim("admin"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
                Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                completions.addAll(ClaimMain.getClaimMembers(chunk));
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("setdesc") || args[0].equalsIgnoreCase("settings")
                    || args[0].equalsIgnoreCase("setname") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("members"))) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("setowner")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("set-actionbar") || args[0].equalsIgnoreCase("set-title-subtitle")
                    || args[0].equalsIgnoreCase("set-economy") || args[0].equalsIgnoreCase("set-claim-confirmation")
                    || args[0].equalsIgnoreCase("set-bossbar") || args[0].equalsIgnoreCase("set-teleportation")
                    || args[0].equalsIgnoreCase("set-teleportation-moving") || args[0].equalsIgnoreCase("autoclaim")
                    || args[0].equalsIgnoreCase("set-claims-visitors-off-visible") || args[0].equalsIgnoreCase("set-claim-cost")
                    || args[0].equalsIgnoreCase("set-claim-cost-multiplier") || args[0].equalsIgnoreCase("set-chat")
                    || args[0].equalsIgnoreCase("set-claim-particles") || args[0].equalsIgnoreCase("set-claim-fly-disabled-on-damage")
                    || args[0].equalsIgnoreCase("set-claim-fly-message-auto-fly"))) {
                completions.addAll(List.of("true", "false"));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("set-bossbar-color")) {
                completions.addAll(List.of(BarColor.values()).stream().map(BarColor::toString).collect(Collectors.toList()));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove-disabled-world")) {
                completions.addAll(ClaimSettings.getDisabledWorlds());
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("set-status-setting") || args[0].equalsIgnoreCase("set-default-value"))) {
                completions.addAll(ClaimGuis.getPerms());
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("add-blocked-interact-block") || args[0].equalsIgnoreCase("add-blocked-item"))) {
                completions.addAll(List.of(Material.values()).stream().map(Material::toString).collect(Collectors.toList()));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("add-blocked-entity")) {
                completions.addAll(List.of(EntityType.values()).stream().map(EntityType::toString).collect(Collectors.toList()));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove-blocked-interact-block")) {
                completions.addAll(ClaimSettings.getRestrictedContainersString());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove-blocked-entity")) {
                completions.addAll(ClaimSettings.getRestrictedEntitiesString());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove-blocked-item")) {
                completions.addAll(ClaimSettings.getRestrictedItemsString());
            }
            if (args.length == 2 && (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("player"))) {
                completions.addAll(List.of("add-limit", "add-radius", "add-members", "set-limit", "set-radius", "set-delay",
                        "set-members", "set-claim-cost", "set-claim-cost-multiplier", "tp", "unclaim"));
            }
            if (args.length == 3 && (args[0].equalsIgnoreCase("set-status-setting") || args[0].equalsIgnoreCase("set-default-value"))) {
                String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
                if (ClaimGuis.isAPerm(perm)) {
                    completions.addAll(List.of("true", "false"));
                }
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("player") && (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("unclaim"))) {
            	completions.addAll(new HashSet<>(ClaimMain.getClaimsOwners()));
            }
            if (args.length == 4 && args[0].equalsIgnoreCase("player") && (args[1].equalsIgnoreCase("tp") || args[1].equalsIgnoreCase("unclaim"))) {
            	completions.addAll(ClaimMain.getClaimsNameFromOwner(args[2]));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("group")) {
                completions.addAll(ClaimSettings.getGroups());
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
    	
    	CPlayer cPlayer = sender instanceof Player ? CPlayerMain.getCPlayer(sender.getName()) : null;
        
        if (args.length > 1 && args[0].equals("setdesc") && sender instanceof Player) {
        	SimpleClaimSystem.executeAsync(() -> {
            	Player player = (Player) sender;
            	if (!ClaimMain.checkName("admin",args[1])) {
            		String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                	if(ClaimMain.setAdminChunkDescription(chunk, description)) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-set-description-success").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)).replaceAll("%description%", description)));
                		return;
                	}
                	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
            		return;
            	}
            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
        		return;
        	});
        	
        	return true;
        }
        
        if(args.length == 1) {
        	if(args[0].equalsIgnoreCase("reload")) {
        		if(SimpleClaimSystem.loadConfig(SimpleClaimSystem.getInstance(),true)) {
        			sender.sendMessage(ClaimLanguage.getMessage("reload-complete"));
        		}
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("convert")) {
        		if(!ClaimSettings.getBooleanSetting("database")) {
        			sender.sendMessage(ClaimLanguage.getMessage("not-using-database"));
        			return true;
        		}
        		ClaimMain.transferClaims();
        		return true;
        	}
        	if(sender instanceof Player) {
        		Player player = (Player) sender;
            	if(args[0].equalsIgnoreCase("bans")) {
            		SimpleClaimSystem.executeAsync(() -> {
                		Chunk chunk = player.getLocation().getChunk();
                		String owner = ClaimMain.getOwnerInClaim(chunk);
                		if(owner.equals("admin")) {
                			cPlayer.setGuiPage(1);
                			SimpleClaimSystem.executeSync(() -> new ClaimBansGui(player, chunk, 1));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim")));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("settings")) {
            		SimpleClaimSystem.executeAsync(() -> {
                		Chunk chunk = player.getLocation().getChunk();
                		String owner = ClaimMain.getOwnerInClaim(chunk);
                		if(owner.equals("admin")) {
                			SimpleClaimSystem.executeSync(() -> new AdminClaimGui(player, chunk));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim")));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("list")) {
            		SimpleClaimSystem.executeAsync(() -> {
                		cPlayer.setGuiPage(1);
                		SimpleClaimSystem.executeSync(() -> new AdminClaimListGui(player,1));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("members")) {
            		SimpleClaimSystem.executeAsync(() -> {
                		Chunk chunk = player.getLocation().getChunk();
                		String owner = ClaimMain.getOwnerInClaim(chunk);
                		if(!ClaimMain.checkIfClaimExists(chunk)) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                			return;
                		}
                		if(!owner.equals("admin")) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim")));
                			return;
                		}
                		cPlayer.setGuiPage(1);
                		SimpleClaimSystem.executeSync(() -> new ClaimMembersGui(player,chunk,1));
                        return;
            		});
            		
            		return true;
            	}
	        	if(args[0].equalsIgnoreCase("forceunclaim")) {
	        		SimpleClaimSystem.executeAsync(() -> {
		        		Chunk chunk = player.getLocation().getChunk();
		        		if(!ClaimMain.checkIfClaimExists(chunk)) {
		        			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
		        			return;
		        		}
		        		String owner = ClaimMain.getOwnerInClaim(chunk);
		        		if(ClaimMain.forceDeleteClaim(chunk)) {
		        			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("forceunclaim-success").replaceAll("%owner%", owner)));
		        	        if (SimpleClaimSystem.isFolia()) {
		        	            Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
		        	                for (Entity e : chunk.getEntities()) {
		        	                    if (!(e instanceof Player)) continue;
		        	                    Player p = (Player) e;
		        	                    ClaimEventsEnterLeave.disableBossBar(p);
		        	                }
		        	            });
		        	        } else {
		        	        	SimpleClaimSystem.executeSync(() -> {
			        	            for (Entity e : chunk.getEntities()) {
			        	                if (!(e instanceof Player)) continue;
			        	                Player p = (Player) e;
			        	                ClaimEventsEnterLeave.disableBossBar(p);
			        	            }
		        	        	});
		        	        }
		        			return;
		        		}
		        		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
		        		return;
	        		});
	        		
	        		return true;
	        	}
	        	SimpleClaimSystem.executeAsync(() -> {
		            if(ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
		            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName())));
		            	return;
		            }
		        	try {
	        			int radius = Integer.parseInt(args[0]);
	        			Set<Chunk> chunks = new HashSet<>(ClaimCommand.getChunksInRadius(player.getLocation(),radius));
	        			SimpleClaimSystem.executeSync(() -> ClaimMain.createAdminClaimRadius(player, chunks, radius));
	        		} catch(NumberFormatException e){
	        			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("syntax-admin")));
	        		}
		        	return;
	        	});
	        	
	        	return true;
        	}
        	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
        	return true;
        }
        
        if(args.length == 2) {
        	if(args[0].equalsIgnoreCase("set-max-length-claim-name")) {
        		SimpleClaimSystem.executeAsync(() -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(args[1]);
                        if(amount < 1) {
                        	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-name-length-must-be-positive")));
                            return;
                        }
                    } catch (NumberFormatException e) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-name-length-must-be-number")));
                        return;
                    }
                    
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    config.set("max-length-claim-name", amount);
                    ClaimSettings.addSetting("max-length-claim-name", args[1]);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-max-length-claim-name-success").replaceAll("%amount%", String.valueOf(args[1]))));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-max-length-claim-description")) {
        		SimpleClaimSystem.executeAsync(() -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(args[1]);
                        if(amount < 1) {
                        	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-description-length-must-be-positive")));
                            return;
                        }
                    } catch (NumberFormatException e) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-description-length-must-be-number")));
                        return;
                    }
                    
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    config.set("max-length-claim-description", amount);
                    ClaimSettings.addSetting("max-length-claim-description", args[1]);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-max-length-claim-description-success").replaceAll("%amount%", String.valueOf(args[1]))));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-lang")) {
        		SimpleClaimSystem.reloadLang(sender, args[1]);
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claims-visitors-off-visible")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claims-visitors-off-visible", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claims-visitors-off-visible", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claims visible (w Visitors setting off)").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-cost")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claim-cost", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claim-cost", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim cost").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-cost-multiplier")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claim-cost-multiplier", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claim-cost-multiplier", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim cost multiplier").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-actionbar")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("enter-leave-messages", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("enter-leave-messages", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "ActionBar").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-protection-message")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("action_bar") || args[1].equalsIgnoreCase("title") || args[1].equalsIgnoreCase("subtitle") || args[1].equalsIgnoreCase("chat") || args[1].equalsIgnoreCase("bossbar")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("protection-message", args[1].toUpperCase());
    	                ClaimSettings.addSetting("protection-message", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Protection message").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-protection-message")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-chat")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("enter-leave-chat-messages", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("enter-leave-chat-messages", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Chat").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-fly-disabled-on-damage")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claim-fly-disabled-on-damage", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claim-fly-disabled-on-damage", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim fly disabled on damage").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-fly-message-auto-fly")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claim-fly-message-auto-fly", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claim-fly-message-auto-fly", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim fly message auto-fly").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-auto-claim")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("auto-claim", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("auto-claim", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "ActionBar").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-title-subtitle")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("enter-leave-title-messages", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("enter-leave-title-messages", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Title/Subtitle").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-economy")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("economy", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("economy", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Economy").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-confirmation")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claim-confirmation", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claim-confirmation", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim confirmation").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-particles")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("claim-particles", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("claim-particles", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim particles").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-max-sell-price")) {
        		SimpleClaimSystem.executeAsync(() -> {
                    int amount;
                    try {
                        amount = Integer.parseInt(args[1]);
                        if(amount < 1) {
                        	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("max-sell-price-must-be-positive")));
                            return;
                        }
                    } catch (NumberFormatException e) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("max-sell-price-must-be-number")));
                        return;
                    }
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    config.set("max-sell-price", args[1]);
                    ClaimSettings.addSetting("max-sell-price", args[1]);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Max sell price").replaceAll("%value%", args[1])));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-bossbar")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("bossbar", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("bossbar", args[1]);
    	                if(args[1].equalsIgnoreCase("false")) {
    	                	SimpleClaimSystem.executeSync(() -> Bukkit.getOnlinePlayers().forEach(p -> ClaimEventsEnterLeave.checkBossBar(p).setVisible(false)));
    	                } else {
    	                	SimpleClaimSystem.executeSync(() -> Bukkit.getOnlinePlayers().forEach(p -> ClaimEventsEnterLeave.activeBossBar(p, p.getLocation().getChunk())));
    	                }
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "BossBar").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-bossbar-color")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		String bcolor = args[1].toUpperCase();
            		BarColor color = BarColor.valueOf(bcolor);
            		if(color == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("bossbar-color-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    config.set("bossbar-settings.color", bcolor);
                    ClaimSettings.addSetting("bossbar-color", bcolor);
                    SimpleClaimSystem.executeSync(() -> ClaimEventsEnterLeave.setBossBarColor(color));
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "BossBar color").replaceAll("%value%", args[1])));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-teleportation")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("teleportation", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("teleportation", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Teleportation").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-teleportation-moving")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("teleportation-delay-moving", Boolean.parseBoolean(args[1]));
    	                ClaimSettings.addSetting("teleportation-delay-moving", args[1]);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Teleportation moving").replaceAll("%value%", args[1])));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("add-disabled-world")) {
        		SimpleClaimSystem.executeAsync(() -> {
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> worlds = config.getStringList("worlds-disabled");
                    if(worlds.contains(args[1])) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("world-already-in-list")));
                    	return;
                    }
                    worlds.add(args[1]);
                    config.set("worlds-disabled", worlds);
                    ClaimSettings.setDisabledWorlds(new HashSet<>(worlds));
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("world-list-changed-via-command").replaceAll("%name%", args[1])));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-disabled-world")) {
        		SimpleClaimSystem.executeAsync(() -> {
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> worlds = config.getStringList("worlds-disabled");
                    if(!worlds.contains(args[1])) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("world-not-in-list")));
                    	return;
                    }
                    worlds.remove(args[1]);
                    config.set("worlds-disabled", worlds);
                    ClaimSettings.setDisabledWorlds(new HashSet<>(worlds));
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("world-list-changeda-via-command").replaceAll("%name%", args[1])));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("add-blocked-interact-block")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		String material = args[1].toUpperCase();
            		Material mat = Material.getMaterial(material);
            		if(mat == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> containers = config.getStringList("blocked-interact-blocks");
                    if(containers.contains(material.toUpperCase())) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("material-already-in-list")));
                    	return;
                    }
                    containers.add(material.toUpperCase());
                    config.set("blocked-interact-blocks", containers);
                    ClaimSettings.setRestrictedContainers(containers);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-list-changed-via-command").replaceAll("%setting%", "Blocked interact blocks").replaceAll("%material%", material.toUpperCase())));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("add-blocked-entity")) {
        		SimpleClaimSystem.executeAsync(() -> {
        			String material = args[1].toUpperCase();
            		EntityType e = EntityType.fromName(material);
            		if(e == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("entity-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> containers = config.getStringList("blocked-entities");
                    if(containers.contains(material.toUpperCase())) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("entity-already-in-list")));
                    	return;
                    }
                    containers.add(material.toUpperCase());
                    config.set("blocked-entities", containers);
                    ClaimSettings.setRestrictedEntityType(containers);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-list-changed-via-command-entity").replaceAll("%setting%", "Blocked entities").replaceAll("%entity%", material.toUpperCase())));
    				} catch (IOException ee) {
    					ee.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("add-blocked-item")) {
        		SimpleClaimSystem.executeAsync(() -> {
        			String material = args[1].toUpperCase();
            		Material mat = Material.getMaterial(material);
            		if(mat == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> items = config.getStringList("blocked-items");
                    if(items.contains(material.toUpperCase())) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("material-already-in-list")));
                    	return;
                    }
                    items.add(material.toUpperCase());
                    config.set("blocked-items", items);
                    ClaimSettings.setRestrictedItems(items);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-list-changed-via-command").replaceAll("%setting%", "Blocked items").replaceAll("%material%", material.toUpperCase())));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-blocked-item")) {
        		SimpleClaimSystem.executeAsync(() -> {
        			String material = args[1].toUpperCase();
            		Material mat = Material.getMaterial(material);
            		if(mat == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> items = config.getStringList("blocked-items");
                    if(!items.contains(material.toUpperCase())) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("material-not-in-list")));
                    	return;
                    }
                    items.remove(material.toUpperCase());
                    config.set("blocked-items", items);
                    ClaimSettings.setRestrictedItems(items);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-list-changeda-via-command").replaceAll("%setting%", "Blocked items").replaceAll("%material%", material.toUpperCase())));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-blocked-interact-block")) {
        		SimpleClaimSystem.executeAsync(() -> {
        			String material = args[1].toUpperCase();
            		Material mat = Material.getMaterial(material);
            		if(mat == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> containers = config.getStringList("blocked-interact-blocks");
                    if(!containers.contains(material.toUpperCase())) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("material-not-in-list")));
                    	return;
                    }
                    containers.remove(material.toUpperCase());
                    config.set("blocked-interact-blocks", containers);
                    ClaimSettings.setRestrictedContainers(containers);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-list-changeda-via-command").replaceAll("%setting%", "Blocked interact blocks").replaceAll("%material%", material.toUpperCase())));
    				} catch (IOException e) {
    					e.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-blocked-entity")) {
        		SimpleClaimSystem.executeAsync(() -> {
            		String material = args[1].toUpperCase();
            		EntityType e = EntityType.fromName(material);
            		if(e == null) {
            			SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("entity-incorrect")));
            			return;
            		}
                    File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
                    FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                    List<String> containers = config.getStringList("blocked-entities");
                    if(!containers.contains(material.toUpperCase())) {
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("entity-not-in-list")));
                    	return;
                    }
                    containers.remove(material.toUpperCase());
                    config.set("blocked-entities", containers);
                    ClaimSettings.setRestrictedEntityType(containers);
                    try {
                    	config.save(configFile);
                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-list-changeda-via-command-entity").replaceAll("%setting%", "Blocked entities").replaceAll("%entity%", material.toUpperCase())));
    				} catch (IOException ee) {
    					ee.printStackTrace();
    				}
                    return;
        		});
        		
                return true;
        	}
        	if(sender instanceof Player) {
        		Player player = (Player) sender;
            	if(args[0].equalsIgnoreCase("bans")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		cPlayer.setGuiPage(1);
                		SimpleClaimSystem.executeSync(() -> new ClaimBansGui(player,chunk,1));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("tp")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> ClaimMain.goClaim(player, ClaimMain.getClaimLocationByChunk(chunk)));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("members")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		cPlayer.setGuiPage(1);
                		SimpleClaimSystem.executeSync(() -> new ClaimMembersGui(player,chunk,1));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("settings")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> new AdminClaimGui(player,chunk));
                		return;
            		});
            		
            		return true;
            	}
	        	if(args[0].equalsIgnoreCase("setowner")) {
	        		SimpleClaimSystem.executeAsync(() -> {
		        		Chunk chunk = player.getLocation().getChunk();
		        		SimpleClaimSystem.executeSync(() -> ClaimMain.setOwner(player, args[1], chunk, true));
		        		return;
	        		});
	        		
	        		return true;
	        	}
        	}
        	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
        	return true;
        }
        
        if(args.length == 3) {
        	if(sender instanceof Player) {
        		Player player = (Player) sender;
        		String playerName = player.getName();
            	if(args[0].equalsIgnoreCase("ban")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			if(args[1].equalsIgnoreCase("*")) {
                    		if(ClaimMain.getPlayerClaimsCount("admin") == 0) {
                    			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("no-admin-claim")));
                    			return;
                    		}
                			Player target = Bukkit.getPlayer(args[2]);
                			String targetName = "";
                			if(target == null) {
                				OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                				if(otarget == null) {
                					SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
                    				return;
                				}
                				targetName = otarget.getName();
                			} else {
                				targetName = target.getName();
                			}
                    		if(targetName.equals(playerName)) {
                    			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself")));
                    			return;
                    		}
        	        		if(ClaimMain.addAllAdminClaimBan(targetName)) {
        	        			String message = ClaimLanguage.getMessage("add-ban-all-success").replaceAll("%player%", targetName);
        	        			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
        	        			return;
        	        		}
        	        		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
        	        		return;
                		}
                		Chunk chunk = ClaimMain.getChunkByClaimName("admin", args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
            			Player target = Bukkit.getPlayer(args[2]);
            			String targetName = "";
            			if(target == null) {
            				OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
            				if(otarget == null) {
            					SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
                				return;
            				}
            				targetName = otarget.getName();
            			} else {
            				targetName = target.getName();
            			}
                		if(targetName.equals(playerName)) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself")));
                			return;
                		}
                		if(ClaimMain.addAdminClaimBan(chunk, targetName)) {
                			String message = ClaimLanguage.getMessage("add-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("unban")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			if(args[1].equalsIgnoreCase("*")) {
                    		if(ClaimMain.getPlayerClaimsCount("admin") == 0) {
                    			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("no-admin-claim")));
                    			return;
                    		}
                			Player target = Bukkit.getPlayer(args[2]);
                			String targetName = "";
                			if(target == null) {
                				OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                				targetName = otarget == null ? args[2] : otarget.getName();
                			} else {
                				targetName = target.getName();
                			}
        	        		if(ClaimMain.removeAllAdminClaimBan(targetName)) {
        	        			String message = ClaimLanguage.getMessage("remove-ban-all-success").replaceAll("%player%", targetName);
        	        			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
        	        			return;
        	        		}
        	        		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
        	        		return;
                		}
                		Chunk chunk = ClaimMain.getChunkByClaimName("admin", args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		if(!ClaimMain.checkBan(chunk, args[2])) {
                			String message = ClaimLanguage.getMessage("not-banned").replaceAll("%player%", args[2]);
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                			return;
                		}
                		String targetName = ClaimMain.getRealNameFromClaimBans(chunk, args[2]);
                		if(ClaimMain.removeAdminClaimBan(chunk, targetName)) {
                			String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("setname")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			if (!ClaimMain.checkName("admin",args[1])) {
                			if(args[2].contains("claim-")) {
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("you-cannot-use-this-name")));
                				return;
                			}
                    		if(ClaimMain.checkName("admin",args[2])) {
                    			Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                            	ClaimMain.setAdminClaimName(chunk, args[2]);
                            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("name-change-success").replaceAll("%name%", args[2])));
                            	return;
                    		}
                    		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error-name-exists").replaceAll("%name%", args[1])));
                        	return;
                		}
                		Chunk chunk = player.getLocation().getChunk();
                		if(!ClaimMain.checkIfClaimExists(chunk)) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                			return;
                		}
                		String owner = ClaimMain.getOwnerInClaim(chunk);
                		if(!owner.equals("admin")) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim")));
                			return;
                		}
            			if(args[1].contains("claim-")) {
            				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("you-cannot-use-this-name")));
            				return;
            			}
                		if(ClaimMain.checkName("admin",args[1])) {
                        	ClaimMain.setAdminClaimName(chunk, args[1]);
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("name-change-success").replaceAll("%name%", args[1])));
                        	return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error-name-exists").replaceAll("%name%", args[1])));
                		return;
            		});
            		
                	return true;
            	}
            	if(args[0].equalsIgnoreCase("add")) {
            		SimpleClaimSystem.executeAsync(() -> {
            			if(args[1].equalsIgnoreCase("*")) {
                    		if(ClaimMain.getPlayerClaimsCount("admin") == 0) {
                    			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("no-admin-claim")));
                    			return;
                    		}
                			Player target = Bukkit.getPlayer(args[2]);
                			String targetName = "";
                			if(target == null) {
                				OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                				if(otarget == null) {
                					SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
                    				return;
                				}
                				targetName = otarget.getName();
                			} else {
                				targetName = target.getName();
                			}
        	        		if(ClaimMain.addAllAdminClaimMembers(targetName)) {
        	        			String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", "all protected areas");
        	        			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
        	        			return;
        	        		}
        	        		player.sendMessage(ClaimLanguage.getMessage("error"));
        	        		return;
                		}
                        Player target = Bukkit.getPlayer(args[2]);
                        String targetName = "";
                        if (target == null) {
                            OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                            if (otarget == null) {
                            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
                                return;
                            }
                            targetName = otarget.getName();
                        } else {
                            targetName = target.getName();
                        }
                		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		if(ClaimMain.addAdminClaimMembers(chunk, targetName)) {
                			String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                		return;
            		});
            		
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("remove")) {
            		SimpleClaimSystem.executeAsync(() -> {
                		if(args[1].equalsIgnoreCase("*")) {
                    		if(ClaimMain.getPlayerClaimsCount("admin") == 0) {
                    			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("no-admin-claim")));
                    			return;
                    		}
                			String targetName = args[2];
        	        		if(ClaimMain.removeAllAdminClaimMembers(targetName)) {
        	        			String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", "all protected areas");
        	        			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
        	        			return;
        	        		}
        	        		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
        	        		return;
                		}
                		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                		if(chunk == null) {
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                			return;
                		}
                		String targetName = args[2];
                        if (!ClaimMain.checkMembre(chunk, targetName)) {
                            String message = ClaimLanguage.getMessage("not-member").replaceAll("%player%", targetName);
                            SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                            return;
                        }
                        String realName = ClaimMain.getRealNameFromClaimMembers(chunk, targetName);
                		if(ClaimMain.removeAdminClaimMembers(chunk, realName)) {
                			String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", realName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                			return;
                		}
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                		return;
            		});
            		
            		return true;
            	}
        	}
        	if(args[0].equalsIgnoreCase("set-status-setting")) {
        		SimpleClaimSystem.executeAsync(() -> {
        			String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
            		if(ClaimGuis.isAPerm(perm)) {
                		if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
        	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
        	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        	                config.set("status-settings."+perm, Boolean.parseBoolean(args[2]));
        	                ClaimSettings.getStatusSettings().put(perm, Boolean.parseBoolean(args[2]));
        	                try {
        	                	config.save(configFile);
        	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Status Setting '"+perm+"'").replaceAll("%value%", args[2])));
        					} catch (IOException e) {
        						e.printStackTrace();
        					}
        	                return;
                		}
                		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
                		return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-incorrect")));
            		return;
        		});
        		
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-default-value")) {
        		SimpleClaimSystem.executeAsync(() -> {
        			String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
            		if(ClaimGuis.isAPerm(perm)) {
                		if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
        	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
        	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        	                config.set("default-values-settings."+perm, Boolean.parseBoolean(args[2]));
        	                ClaimSettings.getDefaultValues().put(perm, Boolean.parseBoolean(args[2]));
        	                try {
        	                	config.save(configFile);
        	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Default Values Setting '"+perm+"'").replaceAll("%value%", args[2])));
        					} catch (IOException e) {
        						e.printStackTrace();
        					}
        	                return;
                		}
                		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean")));
                		return;
            		}
            		SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("setting-incorrect")));
            		return;
        		});
        		
        		return true;
        	}
        	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
        	return true;
        }
        
        if(args.length == 4) {
        	if(args[0].equalsIgnoreCase("player")) {
        		if(sender instanceof Player) {
        			Player player = (Player) sender;
            		if(args[1].equalsIgnoreCase("tp")) {
            			SimpleClaimSystem.executeAsync(() -> {
                			if(!ClaimMain.getClaimsOwners().contains(args[2])) {
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-does-not-have-claim")));
                				return;
                			}
                			if(!ClaimMain.getClaimsNameFromOwner(args[2]).contains(args[3])) {
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                				return;
                			}
                			Chunk c = ClaimMain.getChunkByClaimName(args[2], args[3]);
                			if(c == null) return;
                			Location loc = ClaimMain.getClaimLocationByChunk(c);
                			SimpleClaimSystem.executeSync(() -> {
                        		if(SimpleClaimSystem.isFolia()) {
                        			player.teleportAsync(loc);
                        		} else {
                        			player.teleport(loc);
                        		}
                			});
                    		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-teleport-to-other-claim-aclaim").replaceAll("%name%", args[3]).replaceAll("%player%", args[2])));
                    		return;
            			});
            			
            			return true;
            		}
            		if(args[1].equalsIgnoreCase("unclaim")) {
            			SimpleClaimSystem.executeAsync(() -> {
                			if(!ClaimMain.getClaimsOwners().contains(args[2])) {
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-does-not-have-claim")));
                				return;
                			}
                			if(args[3].equalsIgnoreCase("*")) {
                				if(ClaimMain.deleteAllClaim(args[2])) {
                					SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-unclaim-other-all-claim-aclaim").replaceAll("%player%", args[2])));
                    				Player target = Bukkit.getPlayer(args[2]);
                    				if(target != null) {
                    					SimpleClaimSystem.executeSync(() -> target.sendMessage(ClaimLanguage.getMessage("player-all-claim-unclaimed-by-admin").replaceAll("%player%", player.getName())));
                    				}
                    				return;
                				}
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    			return;
                			}
                			if(!ClaimMain.getClaimsNameFromOwner(args[2]).contains(args[3])) {
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                				return;
                			}
                			Chunk c = ClaimMain.getChunkByClaimName(args[2], args[3]);
                			if(c == null) return;
                			if(ClaimMain.deleteClaim(player, c)) {
                				SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-unclaim-other-claim-aclaim").replaceAll("%name%", args[3]).replaceAll("%player%", args[2])));
                				Player target = Bukkit.getPlayer(args[2]);
                				if(target != null) {
                					SimpleClaimSystem.executeSync(() -> target.sendMessage(ClaimLanguage.getMessage("player-claim-unclaimed-by-admin").replaceAll("%name%", args[3]).replaceAll("%player%", player.getName())));
                				}
                				return;
                			}
                			SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                			return;
            			});
            			
            			return true;
            		}
        		}
        		if(args[1].equalsIgnoreCase("set-claim-cost")) {
        			SimpleClaimSystem.executeAsync(() -> {
            			Player target = Bukkit.getPlayer(args[2]);
                        String targetName = "";
                        if (target == null) {
                            OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                            if (otarget == null) {
                            	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
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
    	                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-positive")));
    	                        return;
    	                    }
    	                } catch (NumberFormatException e) {
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-number")));
    	                    return;
    	                }
    	                
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("players."+targetName+".claim-cost", amount);
    	                if(target != null && target.isOnline()) {
    		                CPlayer cTarget = CPlayerMain.getCPlayer(targetName);
    		                cTarget.setClaimCost(amount);
    	                }
    	                try {
    	                	config.save(configFile);
    	                	final String tName = targetName;
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-claim-cost-success").replaceAll("%player%", tName).replaceAll("%amount%", String.valueOf(args[3]))));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
        			});
        			
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
        			SimpleClaimSystem.executeAsync(() -> {
            			Player target = Bukkit.getPlayer(args[2]);
                        String targetName = "";
                        if (target == null) {
                            OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                            if (otarget == null) {
                            	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
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
    	                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-positive")));
    	                        return;
    	                    }
    	                } catch (NumberFormatException e) {
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-number")));
    	                    return;
    	                }
    	                
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("players."+targetName+".claim-cost-multiplier", amount);
    	                if(target != null && target.isOnline()) {
    	                	CPlayer cTarget = CPlayerMain.getCPlayer(targetName);
    	                	cTarget.setClaimCostMultiplier(amount);
    	                }
    	                try {
    	                	config.save(configFile);
    	                	final String tName = targetName;
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-claim-cost-multiplier-success").replaceAll("%player%", tName).replaceAll("%amount%", String.valueOf(args[3]))));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
        			});
        			
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-members")) {
        			SimpleClaimSystem.executeAsync(() -> {
            			Player target = Bukkit.getPlayer(args[2]);
                        String targetName = "";
                        if (target == null) {
                            OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                            if (otarget == null) {
                            	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
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
    	                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-positive")));
    	                        return;
    	                    }
    	                } catch (NumberFormatException e) {
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-number")));
    	                    return;
    	                }
    	                
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("players."+targetName+".max-members", amount);
    	                if(target != null && target.isOnline()) {
    	                	CPlayer cTarget = CPlayerMain.getCPlayer(targetName);
    	                	cTarget.setMaxMembers(amount);
    	                }
    	                try {
    	                	config.save(configFile);
    	                	final String tName = targetName;
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-member-limit-success").replaceAll("%player%", tName).replaceAll("%amount%", String.valueOf(args[3]))));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
        			});
        			
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-limit")) {
	        		SimpleClaimSystem.executeAsync(() -> {
	        			Player target = Bukkit.getPlayer(args[2]);
	                    String targetName = "";
	                    if (target == null) {
	                        OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
	                        if (otarget == null) {
	                        	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
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
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                config.set("players."+targetName+".max-claims", amount);
		                if(target != null && target.isOnline()) {
		                	CPlayer cTarget = CPlayerMain.getCPlayer(targetName);
		                	cTarget.setMaxClaims(amount);
		                }
		                try {
		                	config.save(configFile);
		                	final String tName = targetName;
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-max-claim-success").replaceAll("%player%", tName).replaceAll("%amount%", String.valueOf(args[3]))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-radius")) {
	        		SimpleClaimSystem.executeAsync(() -> {
	        			Player target = Bukkit.getPlayer(args[2]);
	                    String targetName = "";
	                    if (target == null) {
	                        OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
	                        if (otarget == null) {
	                        	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
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
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                config.set("players."+targetName+".max-radius-claims", amount);
		                if(target != null && target.isOnline()) {
		                	CPlayer cTarget = CPlayerMain.getCPlayer(targetName);
		                	cTarget.setMaxRadiusClaims(amount);
		                }
		                
		                try {
		                	config.save(configFile);
		                	final String tName = targetName;
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-max-radius-claim-success").replaceAll("%player%", tName).replaceAll("%amount%", String.valueOf(args[3]))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-delay")) {
	        		SimpleClaimSystem.executeAsync(() -> {
	        			Player target = Bukkit.getPlayer(args[2]);
	                    String targetName = "";
	                    if (target == null) {
	                        OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
	                        if (otarget == null) {
	                        	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2])));
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
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                config.set("players."+targetName+".teleportation-delay", amount);
		                if(target != null && target.isOnline()) {
		                	CPlayer cTarget = CPlayerMain.getCPlayer(targetName);
		                	cTarget.setTeleportationDelay(amount);
		                }
		                
		                try {
		                	config.save(configFile);
		                	final String tName = targetName;
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-teleportation-delay-success").replaceAll("%player%", tName).replaceAll("%amount%", String.valueOf(args[3]))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("add-limit")) {
	        		SimpleClaimSystem.executeAsync(() -> {
	        			Player target = Bukkit.getPlayer(args[2]);
	        			if(target == null) {
	        				SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-not-online")));
	        				return;
	        			}
	        			String name = target.getName();
	        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
		                int amount;
		                try {
		                    amount = Integer.parseInt(args[3]);
		                    if(amount < 0) {
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                int new_amount = cTarget.getMaxClaims()+amount;
		                config.set("players."+name+".max-claims", new_amount);
		                cTarget.setMaxClaims(new_amount);
		                try {
		                	config.save(configFile);
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-max-claim-success").replaceAll("%player%", name).replaceAll("%amount%", String.valueOf(new_amount))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("add-radius")) {
	        		SimpleClaimSystem.executeAsync(() -> {
	        			Player target = Bukkit.getPlayer(args[2]);
	        			if(target == null) {
	        				SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-not-online")));
	        				return;
	        			}
	        			String name = target.getName();
	        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
		                int amount;
		                try {
		                    amount = Integer.parseInt(args[3]);
		                    if(amount < 0) {
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-number")));
		                    return;
		                }

		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                int new_amount = cTarget.getMaxRadiusClaims()+amount;
		                config.set("players."+name+".max-radius-claims", new_amount);
		                cTarget.setMaxClaims(new_amount);
		                try {
		                	config.save(configFile);
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-max-radius-claim-success").replaceAll("%player%", name).replaceAll("%amount%", String.valueOf(new_amount))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("add-members")) {
	        		SimpleClaimSystem.executeAsync(() -> {
	        			Player target = Bukkit.getPlayer(args[2]);
	        			if(target == null) {
	        				SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("player-not-online")));
	        				return;
	        			}
	        			String name = target.getName();
	        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
		                int amount;
		                try {
		                    amount = Integer.parseInt(args[3]);
		                    if(amount < 0) {
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("member-limit-radius-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("member-limit-radius-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                int new_amount = cTarget.getMaxRadiusClaims()+amount;
		                config.set("players."+name+".max-members", new_amount);
		                cTarget.setMaxClaims(new_amount);
		                try {
		                	config.save(configFile);
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-player-member-limit-success").replaceAll("%player%", name).replaceAll("%amount%", String.valueOf(new_amount))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
	        	return true;
        	}
        	if(args[0].equalsIgnoreCase("group")) {
        		
        		if(!ClaimSettings.getGroups().contains(args[2])) {
        			sender.sendMessage(ClaimLanguage.getMessage("group-does-not-exists"));
        			return true;
        		}
        		
        		if(args[1].equalsIgnoreCase("set-claim-cost")) {
        			SimpleClaimSystem.executeAsync(() -> {
    	                Double amount;
    	                try {
    	                    amount = Double.parseDouble(args[3]);
    	                    if(amount < 0) {
    	                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-positive")));
    	                        return;
    	                    }
    	                } catch (NumberFormatException e) {
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-number")));
    	                    return;
    	                }
    	                
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("groups."+args[2]+".claim-cost", amount);
    	                ClaimSettings.getGroupsSettings().get(args[2]).put("claim-cost", amount);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-group-claim-cost-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3]))));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
        			});
        			
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
        			SimpleClaimSystem.executeAsync(() -> {
    	                Double amount;
    	                try {
    	                    amount = Double.parseDouble(args[3]);
    	                    if(amount < 0) {
    	                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-positive")));
    	                        return;
    	                    }
    	                } catch (NumberFormatException e) {
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-number")));
    	                    return;
    	                }
    	                
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("groups."+args[2]+".claim-cost-multiplier", amount);
    	                ClaimSettings.getGroupsSettings().get(args[2]).put("claim-cost-multiplier", amount);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-group-claim-cost-multiplier-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3]))));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
        			});
        			
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-members")) {
        			SimpleClaimSystem.executeAsync(() -> {
    	                Double amount;
    	                try {
    	                    amount = Double.parseDouble(args[3]);
    	                    if(amount < 0) {
    	                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-positive")));
    	                        return;
    	                    }
    	                } catch (NumberFormatException e) {
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-number")));
    	                    return;
    	                }
    	                
    	                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("groups."+args[2]+".max-members", amount);
    	                ClaimSettings.getGroupsSettings().get(args[2]).put("max-members", amount);
    	                try {
    	                	config.save(configFile);
    	                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-group-member-limit-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3]))));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return;
        			});
        			
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-limit")) {
	        		SimpleClaimSystem.executeAsync(() -> {
		                Double amount;
		                try {
		                    amount = Double.parseDouble(args[3]);
		                    if(amount < 0) {
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                config.set("groups."+args[2]+".max-claims", amount);
		                ClaimSettings.getGroupsSettings().get(args[2]).put("max-claims", amount);
		                try {
		                	config.save(configFile);
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-group-max-claim-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3]))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-radius")) {
	        		SimpleClaimSystem.executeAsync(() -> {
		                Double amount;
		                try {
		                    amount = Double.parseDouble(args[3]);
		                    if(amount < 0) {
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                config.set("groups."+args[2]+".max-radius-claims", amount);
		                ClaimSettings.getGroupsSettings().get(args[2]).put("max-radius-claims", amount);
		                try {
		                	config.save(configFile);
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-group-max-radius-claim-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3]))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-delay")) {
	        		SimpleClaimSystem.executeAsync(() -> {
		                Double amount;
		                try {
		                    amount = Double.parseDouble(args[3]);
		                    if(amount < 0) {
		                    	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-positive")));
		                        return;
		                    }
		                } catch (NumberFormatException e) {
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-number")));
		                    return;
		                }
		                
		                File configFile = new File(SimpleClaimSystem.getInstance().getDataFolder(), "config.yml");
		                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		                config.set("groups."+args[2]+".teleportation-delay", amount);
		                ClaimSettings.getGroupsSettings().get(args[2]).put("teleportation-delay", amount);
		                try {
		                	config.save(configFile);
		                	SimpleClaimSystem.executeSync(() -> sender.sendMessage(ClaimLanguage.getMessage("set-group-teleportation-delay-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3]))));
						} catch (IOException e) {
							e.printStackTrace();
						}
		                return;
	        		});
	        		
	                return true;
	        	}
	        	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
	        	return true;
        	}
        }
        
        if(sender instanceof Player) {
        	SimpleClaimSystem.executeAsync(() -> {
            	Player player = (Player) sender;
                if(ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
                	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName())));
                	return;
                }
    	        Chunk chunk = player.getLocation().getChunk();
    	        SimpleClaimSystem.executeSync(() -> ClaimMain.createAdminClaim(player, chunk));
        	});
        	
	        return true;
        }

    	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
        return true;
    }
}
