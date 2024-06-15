package fr.xyness.SCS.Commands;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import org.bukkit.plugin.java.JavaPlugin;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.AdminClaimGui;
import fr.xyness.SCS.Guis.AdminClaimListGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Listeners.ClaimEventsEnterLeave;

public class AClaimCommand implements CommandExecutor, TabCompleter {
	
	private JavaPlugin plugin;
	
	public AClaimCommand(JavaPlugin plugin) {
		this.plugin = plugin;
	}
	
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        List<String> completions = new ArrayList<>();

        if(sender instanceof Player) {
        	if (args.length == 1) {
        		completions.add("convert");
        		completions.add("setdesc");
        		completions.add("settings");
        		completions.add("setname");
        		completions.add("members");
        		completions.add("tp");
        		completions.add("ptp");
        		completions.add("list");
        		completions.add("player");
        		completions.add("group");
        		completions.add("forceunclaim");
        		completions.add("setowner");
        		completions.add("set-lang");
        		completions.add("set-actionbar");
        		completions.add("set-auto-claim");
        		completions.add("set-title-subtitle");
        		completions.add("set-economy");
        		completions.add("set-claim-confirmation");
        		completions.add("set-max-sell-price");
        		completions.add("set-bossbar");
        		completions.add("set-bossbar-color");
        		completions.add("set-teleportation");
        		completions.add("set-teleportation-moving");
        		completions.add("add-blocked-interact-block");
        		completions.add("add-blocked-entity");
        		completions.add("add-blocked-item");
        		completions.add("remove-blocked-interact-block");
        		completions.add("remove-blocked-item");
        		completions.add("remove-blocked-entity");
        		completions.add("add-disabled-world");
        		completions.add("remove-disabled-world");
        		completions.add("set-status-setting");
        		completions.add("set-default-value");
        		completions.add("set-max-length-claim-description");
        		completions.add("set-max-length-claim-name");
        		completions.add("set-claims-visitors-off-visible");
        		completions.add("set-claim-cost");
        		completions.add("set-claim-cost-multiplier");
        		return completions;
        	}
	        if (args.length == 2 && args[0].equalsIgnoreCase("ptp")) {
	        	completions.addAll(new HashSet<>(ClaimMain.getClaimsOwners()));
	        	return completions;
	        }
	        if (args.length == 3 && args[0].equalsIgnoreCase("ptp")) {
	        	completions.addAll(ClaimMain.getClaimsNameFromOwner(args[1]));
	        	return completions;
	        }
	        if (args.length == 2 && args[0].equalsIgnoreCase("add")) {
	        	completions.add("*");
        		completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
	        	return completions;
	        }
	        if (args.length == 2 && args[0].equalsIgnoreCase("remove")) {
	        	completions.add("*");
	        	completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
	        	return completions;
	        }
	        if (args.length == 3 && args[0].equalsIgnoreCase("remove")) {
	        	Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
	        	completions.addAll(ClaimMain.getClaimMembers(chunk));
	        	return completions;
	        }
        	if (args.length == 2 && (args[0].equalsIgnoreCase("setdesc") || args[0].equalsIgnoreCase("settings")
        			|| args[0].equalsIgnoreCase("setname") || args[0].equalsIgnoreCase("tp") || args[0].equalsIgnoreCase("members"))) {
        		completions.addAll(ClaimMain.getClaimsNameFromOwner("admin"));
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("setowner")) {
        		for(Player p : Bukkit.getOnlinePlayers()) {
        			completions.add(p.getName());
        		}
        		return completions;
        	}
        	if (args.length == 2 && (args[0].equalsIgnoreCase("set-actionbar") || args[0].equalsIgnoreCase("set-title-subtitle")
        			|| args[0].equalsIgnoreCase("set-economy") || args[0].equalsIgnoreCase("set-claim-confirmation")
        			|| args[0].equalsIgnoreCase("set-bossbar") || args[0].equalsIgnoreCase("set-teleportation")
        			|| args[0].equalsIgnoreCase("set-teleportation-moving") || args[0].equalsIgnoreCase("autoclaim")
        			|| args[0].equalsIgnoreCase("set-claims-visitors-off-visible") || args[0].equalsIgnoreCase("set-claim-cost")
        			|| args[0].equalsIgnoreCase("set-claim-cost-multiplier"))) {
        		completions.add("true");
        		completions.add("false");
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("set-bossbar-color")) {
        		for(BarColor c : BarColor.values()) {
        			completions.add(c.toString());
        		}
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("remove-disabled-world")) {
        		completions.addAll(ClaimSettings.getDisabledWorlds());
        		return completions;
        	}
        	if (args.length == 2 && (args[0].equalsIgnoreCase("set-status-setting") || args[0].equalsIgnoreCase("set-default-value"))) {
        		completions.addAll(ClaimGuis.getPerms());
        		return completions;
        	}
        	if (args.length == 2 && (args[0].equalsIgnoreCase("add-blocked-interact-block") || args[0].equalsIgnoreCase("add-blocked-item"))) {
        		for(Material mat : Material.values()) {
        			completions.add(mat.toString());
        		}
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("add-blocked-entity")) {
        		for(EntityType e : EntityType.values()) {
        			completions.add(e.toString());
        		}
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("remove-blocked-interact-block")) {
        		completions.addAll(ClaimSettings.getRestrictedContainersString());
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("remove-blocked-entity")) {
        		completions.addAll(ClaimSettings.getRestrictedEntitiesString());
        		return completions;
        	}
        	if (args.length == 2 && args[0].equalsIgnoreCase("remove-blocked-item")) {
        		completions.addAll(ClaimSettings.getRestrictedItemsString());
        		return completions;
        	}
	        if (args.length == 2 && (args[0].equalsIgnoreCase("group") || args[0].equalsIgnoreCase("player"))) {
	        	completions.add("add-limit");
	        	completions.add("add-radius");
	        	completions.add("add-members");
	        	completions.add("set-limit");
	        	completions.add("set-radius");
	        	completions.add("set-delay");
	        	completions.add("set-members");
	        	completions.add("set-claim-cost");
	        	completions.add("set-claim-cost-multiplier");
	        	return completions;
	        }
	        if (args.length == 3 && (args[0].equalsIgnoreCase("set-status-setting") || args[0].equalsIgnoreCase("set-default-value"))) {
	        	String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
	        	if(ClaimGuis.isAPerm(perm)) {
	        		completions.add("true");
	        		completions.add("false");
	        		return completions;
	        	}
	        }
	        if (args.length == 3 && args[0].equalsIgnoreCase("player")) {
	        	for(OfflinePlayer p : Bukkit.getOfflinePlayers()) {
	        		completions.add(p.getName());
	        	}
	        	return completions;
	        }
	        if (args.length == 3 && args[0].equalsIgnoreCase("group")) {
	        	completions.addAll(ClaimSettings.getGroups());
	        	return completions;
	        }
        }

        return completions;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
    	CPlayer cPlayer = null;
        if (sender instanceof Player) {
        	Player player = (Player) sender;
        	cPlayer = CPlayerMain.getCPlayer(player.getName());
            if(!player.hasPermission("scs.admin")) {
            	sender.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            	return true;
            }
        }
        
        if (args.length > 1 && args[0].equals("setdesc") && sender instanceof Player) {
        	Player player = (Player) sender;
        	if (!ClaimMain.checkName("owner",args[1])) {
        		String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
            	if(ClaimMain.setAdminChunkDescription(chunk, description)) {
            		player.sendMessage(ClaimLanguage.getMessage("claim-set-description-success").replaceAll("%name%", ClaimMain.getClaimNameByChunk(chunk)).replaceAll("%description%", description));
            		return true;
            	}
        		player.sendMessage(ClaimLanguage.getMessage("error"));
        		return true;
        	}
    		player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
    		return true;
        }
        
        if(args.length == 1) {
        	if(args[0].equalsIgnoreCase("reload")) {
        		if(SimpleClaimSystem.loadConfig(plugin,true)) {
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
            	if(args[0].equalsIgnoreCase("settings")) {
            		Chunk chunk = player.getLocation().getChunk();
            		String owner = ClaimMain.getOwnerInClaim(chunk);
            		if(owner.equals("admin")) {
            			AdminClaimGui menu = new AdminClaimGui(player, chunk);
            			menu.openInventory(player);
            			return true;
            		}
            		player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim"));
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("list")) {
            		cPlayer.setGuiPage(1);
            		AdminClaimListGui menu = new AdminClaimListGui(player,1);
            		menu.openInventory(player);
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("members")) {
            		Chunk chunk = player.getLocation().getChunk();
            		String owner = ClaimMain.getOwnerInClaim(chunk);
            		if(!ClaimMain.checkIfClaimExists(chunk)) {
            			player.sendMessage(ClaimLanguage.getMessage("free-territory"));
            			return true;
            		}
            		if(!owner.equals("admin")) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim"));
            			return true;
            		}
            		cPlayer.setGuiPage(1);
                    ClaimMembersGui menu = new ClaimMembersGui(player,chunk,1);
                    menu.openInventory(player);
                    return true;
            	}
	        	if(args[0].equalsIgnoreCase("forceunclaim")) {
	        		Chunk chunk = player.getLocation().getChunk();
	        		if(!ClaimMain.checkIfClaimExists(chunk)) {
	        			player.sendMessage(ClaimLanguage.getMessage("free-territory"));
	        			return true;
	        		}
	        		String owner = ClaimMain.getOwnerInClaim(chunk);
	        		if(ClaimMain.forceDeleteClaim(chunk)) {
	        			player.sendMessage(ClaimLanguage.getMessage("forceunclaim-success").replaceAll("%owner%", owner));
	    				for(Entity e : chunk.getEntities()) {
	    					if(!(e instanceof Player)) continue;
	    					Player p = (Player) e;
	    					ClaimEventsEnterLeave.disableBossBar(p);
	    				}
	        			return true;
	        		}
	        		player.sendMessage(ClaimLanguage.getMessage("error"));
	        		return true;
	        	}
	            if(ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
	            	player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
	            	return true;
	            }
	        	try {
        			int radius = Integer.parseInt(args[0]);
        			Set<Chunk> chunks = new HashSet<>(ClaimCommand.getChunksInRadius(player.getLocation(),radius));
        			ClaimMain.createAdminClaimRadius(player, chunks, radius);
        			return true;
        		} catch(NumberFormatException e){
            		player.sendMessage(ClaimLanguage.getMessage("syntax-admin"));
                    return true;
        		}
        	}
        	return true;
        }
        
        if(args.length == 2) {
        	if(args[0].equalsIgnoreCase("set-max-length-claim-name")) {
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                    if(amount < 1) {
                        sender.sendMessage(ClaimLanguage.getMessage("claim-name-length-must-be-positive"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ClaimLanguage.getMessage("claim-name-length-must-be-number"));
                    return true;
                }
                
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("max-length-claim-name", amount);
                ClaimSettings.addSetting("max-length-claim-name", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("set-max-length-claim-name-success").replaceAll("%amount%", String.valueOf(args[1])));
                	return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-max-length-claim-description")) {
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                    if(amount < 1) {
                        sender.sendMessage(ClaimLanguage.getMessage("claim-description-length-must-be-positive"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ClaimLanguage.getMessage("claim-description-length-must-be-number"));
                    return true;
                }
                
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("max-length-claim-description", amount);
                ClaimSettings.addSetting("max-length-claim-description", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("set-max-length-claim-description-success").replaceAll("%amount%", String.valueOf(args[1])));
                	return true;
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-lang")) {
        		SimpleClaimSystem.reloadLang(plugin, sender, args[1]);
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claims-visitors-off-visible")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("claims-visitors-off-visible", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("claims-visitors-off-visible", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claims visible (w Visitors setting off)").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-cost")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("claim-cost", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("claim-cost", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim cost").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-cost-multiplier")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("claim-cost-multiplier", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("claim-cost-multiplier", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim cost multiplier").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-actionbar")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("enter-leave-messages", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("enter-leave-messages", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "ActionBar").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-auto-claim")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("auto-claim", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("auto-claim", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "ActionBar").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-title-subtitle")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("enter-leave-title-messages", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("enter-leave-title-messages", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Title/Subtitle").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-economy")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("economy", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("economy", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Economy").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-claim-confirmation")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("claim-confirmation", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("claim-confirmation", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Claim confirmation").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-max-sell-price")) {
                int amount;
                try {
                    amount = Integer.parseInt(args[1]);
                    if(amount < 1) {
                        sender.sendMessage(ClaimLanguage.getMessage("max-sell-price-must-be-positive"));
                        return true;
                    }
                } catch (NumberFormatException e) {
                    sender.sendMessage(ClaimLanguage.getMessage("max-sell-price-must-be-number"));
                    return true;
                }
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("max-sell-price", args[1]);
                ClaimSettings.addSetting("max-sell-price", args[1]);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Max sell price").replaceAll("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-bossbar")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("bossbar", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("bossbar", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "BossBar").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-bossbar-color")) {
        		String bcolor = args[1].toUpperCase();
        		BarColor color = BarColor.valueOf(bcolor);
        		if(color == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("bossbar-color-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                config.set("bossbar-settings.color", bcolor);
                ClaimSettings.addSetting("bossbar-color", bcolor);
                ClaimEventsEnterLeave.setBossBarColor(color);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "BossBar color").replaceAll("%value%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("set-teleportation")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("teleportation", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("teleportation", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Teleportation").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-teleportation-moving")) {
        		if(args[1].equalsIgnoreCase("true") || args[1].equalsIgnoreCase("false")) {
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("teleportation-delay-moving", Boolean.parseBoolean(args[1]));
	                ClaimSettings.addSetting("teleportation-delay-moving", args[1]);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Teleportation moving").replaceAll("%value%", args[1]));
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("add-disabled-world")) {
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> worlds = config.getStringList("worlds-disabled");
                if(worlds.contains(args[1])) {
                	sender.sendMessage(ClaimLanguage.getMessage("world-already-in-list"));
                	return true;
                }
                worlds.add(args[1]);
                config.set("worlds-disabled", worlds);
                ClaimSettings.setDisabledWorlds(new HashSet<>(worlds));
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("world-list-changed-via-command").replaceAll("%name%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-disabled-world")) {
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> worlds = config.getStringList("worlds-disabled");
                if(!worlds.contains(args[1])) {
                	sender.sendMessage(ClaimLanguage.getMessage("world-not-in-list"));
                	return true;
                }
                worlds.remove(args[1]);
                config.set("worlds-disabled", worlds);
                ClaimSettings.setDisabledWorlds(new HashSet<>(worlds));
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("world-list-changeda-via-command").replaceAll("%name%", args[1]));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("add-blocked-interact-block")) {
        		String material = args[1].toUpperCase();
        		Material mat = Material.getMaterial(material);
        		if(mat == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> containers = config.getStringList("blocked-interact-blocks");
                if(containers.contains(material.toUpperCase())) {
                	sender.sendMessage(ClaimLanguage.getMessage("material-already-in-list"));
                	return true;
                }
                containers.add(material.toUpperCase());
                config.set("blocked-interact-blocks", containers);
                ClaimSettings.setRestrictedContainers(containers);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-list-changed-via-command").replaceAll("%setting%", "Blocked interact blocks").replaceAll("%material%", material.toUpperCase()));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("add-blocked-entity")) {
        		String material = args[1].toUpperCase();
        		EntityType e = EntityType.fromName(material);
        		if(e == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("entity-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> containers = config.getStringList("blocked-entities");
                if(containers.contains(material.toUpperCase())) {
                	sender.sendMessage(ClaimLanguage.getMessage("entity-already-in-list"));
                	return true;
                }
                containers.add(material.toUpperCase());
                config.set("blocked-entities", containers);
                ClaimSettings.setRestrictedEntityType(containers);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-list-changed-via-command-entity").replaceAll("%setting%", "Blocked entities").replaceAll("%entity%", material.toUpperCase()));
				} catch (IOException ee) {
					ee.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("add-blocked-item")) {
        		String material = args[1].toUpperCase();
        		Material mat = Material.getMaterial(material);
        		if(mat == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> items = config.getStringList("blocked-items");
                if(items.contains(material.toUpperCase())) {
                	sender.sendMessage(ClaimLanguage.getMessage("material-already-in-list"));
                	return true;
                }
                items.add(material.toUpperCase());
                config.set("blocked-items", items);
                ClaimSettings.setRestrictedItems(items);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-list-changed-via-command").replaceAll("%setting%", "Blocked items").replaceAll("%material%", material.toUpperCase()));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-blocked-item")) {
        		String material = args[1].toUpperCase();
        		Material mat = Material.getMaterial(material);
        		if(mat == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> items = config.getStringList("blocked-items");
                if(!items.contains(material.toUpperCase())) {
                	sender.sendMessage(ClaimLanguage.getMessage("material-not-in-list"));
                	return true;
                }
                items.remove(material.toUpperCase());
                config.set("blocked-items", items);
                ClaimSettings.setRestrictedItems(items);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-list-changeda-via-command").replaceAll("%setting%", "Blocked items").replaceAll("%material%", material.toUpperCase()));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-blocked-interact-block")) {
        		String material = args[1].toUpperCase();
        		Material mat = Material.getMaterial(material);
        		if(mat == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("mat-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> containers = config.getStringList("blocked-interact-blocks");
                if(!containers.contains(material.toUpperCase())) {
                	sender.sendMessage(ClaimLanguage.getMessage("material-not-in-list"));
                	return true;
                }
                containers.remove(material.toUpperCase());
                config.set("blocked-interact-blocks", containers);
                ClaimSettings.setRestrictedContainers(containers);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-list-changeda-via-command").replaceAll("%setting%", "Blocked interact blocks").replaceAll("%material%", material.toUpperCase()));
				} catch (IOException e) {
					e.printStackTrace();
				}
                return true;
        	}
        	if(args[0].equalsIgnoreCase("remove-blocked-entity")) {
        		String material = args[1].toUpperCase();
        		EntityType e = EntityType.fromName(material);
        		if(e == null) {
        			sender.sendMessage(ClaimLanguage.getMessage("entity-incorrect"));
        			return true;
        		}
                File configFile = new File(plugin.getDataFolder(), "config.yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
                List<String> containers = config.getStringList("blocked-entities");
                if(!containers.contains(material.toUpperCase())) {
                	sender.sendMessage(ClaimLanguage.getMessage("entity-not-in-list"));
                	return true;
                }
                containers.remove(material.toUpperCase());
                config.set("blocked-entities", containers);
                ClaimSettings.setRestrictedEntityType(containers);
                try {
                	config.save(configFile);
                	sender.sendMessage(ClaimLanguage.getMessage("setting-list-changeda-via-command-entity").replaceAll("%setting%", "Blocked entities").replaceAll("%entity%", material.toUpperCase()));
				} catch (IOException ee) {
					ee.printStackTrace();
				}
                return true;
        	}
        	if(sender instanceof Player) {
        		Player player = (Player) sender;
            	if(args[0].equalsIgnoreCase("tp")) {
            		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
            		if(chunk == null) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
            			return true;
            		}
            		ClaimMain.goClaim(player, ClaimMain.getClaimLocationByChunk(chunk));
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("members")) {
            		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
            		if(chunk == null) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
            			return true;
            		}
            		cPlayer.setGuiPage(1);
            		ClaimMembersGui menu = new ClaimMembersGui(player,chunk,1);
            		menu.openInventory(player);
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("settings")) {
            		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
            		if(chunk == null) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
            			return true;
            		}
            		AdminClaimGui menu = new AdminClaimGui(player,chunk);
            		menu.openInventory(player);
            		return true;
            	}
	        	if(args[0].equalsIgnoreCase("setowner")) {
	        		Chunk chunk = player.getLocation().getChunk();
	        		ClaimMain.setOwner(player, args[1], chunk);
	        		return true;
	        	}
        	}
        }
        
        if(args.length == 3) {
        	if(sender instanceof Player) {
        		Player player = (Player) sender;
        		if(args[0].equalsIgnoreCase("ptp")) {
        			if(!ClaimMain.getClaimsOwners().contains(args[1])) {
        				player.sendMessage(ClaimLanguage.getMessage("player-does-not-have-claim"));
        				return false;
        			}
        			if(!ClaimMain.getClaimsNameFromOwner(args[1]).contains(args[2])) {
        				player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
        				return false;
        			}
        			Chunk c = ClaimMain.getChunkByClaimName(args[1], args[2]);
        			if(c == null) return false;
        			Location loc = ClaimMain.getClaimLocationByChunk(c);
            		if(SimpleClaimSystem.isFolia()) {
            			player.teleportAsync(loc);
            		} else {
            			player.teleport(loc);
            		}
        			player.sendMessage(ClaimLanguage.getMessage("player-teleport-to-other-claim-aclaim").replaceAll("%name%", args[2]).replaceAll("%player%", args[1]));
        			return true;
        		}
            	if(args[0].equalsIgnoreCase("setname")) {
            		if (!ClaimMain.checkName("admin",args[1])) {
            			if(args[2].contains("claim-")) {
            				player.sendMessage(ClaimLanguage.getMessage("you-cannot-use-this-name"));
            				return true;
            			}
                		if(ClaimMain.checkName("admin",args[2])) {
                			Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
                        	ClaimMain.setAdminClaimName(chunk, args[2]);
                        	player.sendMessage(ClaimLanguage.getMessage("name-change-success").replaceAll("%name%", args[2]));
                        	return true;
                		}
                		player.sendMessage(ClaimLanguage.getMessage("error-name-exists").replaceAll("%name%", args[1]));
                    	return true;
            		}
            		Chunk chunk = player.getLocation().getChunk();
            		if(!ClaimMain.checkIfClaimExists(chunk)) {
            			player.sendMessage(ClaimLanguage.getMessage("free-territory"));
            			return true;
            		}
            		String owner = ClaimMain.getOwnerInClaim(chunk);
            		if(!owner.equals("admin")) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-not-an-admin-claim"));
            			return true;
            		}
        			if(args[1].contains("claim-")) {
        				player.sendMessage(ClaimLanguage.getMessage("you-cannot-use-this-name"));
        				return true;
        			}
            		if(ClaimMain.checkName("admin",args[1])) {
                    	ClaimMain.setAdminClaimName(chunk, args[1]);
                    	player.sendMessage(ClaimLanguage.getMessage("name-change-success").replaceAll("%name%", args[1]));
                    	return true;
            		}
            		player.sendMessage(ClaimLanguage.getMessage("error-name-exists").replaceAll("%name%", args[1]));
                	return true;
            	}
            	if(args[0].equalsIgnoreCase("add")) {
            		if(args[1].equalsIgnoreCase("*")) {
    	        		if(ClaimMain.addAllAdminClaimMembers(args[2])) {
    	        			String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", args[2]);
    	        			player.sendMessage(message);
    	        			return true;
    	        		}
    	        		player.sendMessage(ClaimLanguage.getMessage("error"));
    	        		return true;
            		}
            		OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            		if(!target.hasPlayedBefore() && !target.isOnline()) {
            			player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
            			return true;
            		}
            		String targetName = target.getName();
            		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
            		if(chunk == null) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
            			return true;
            		}
            		if(ClaimMain.addAdminClaimMembers(chunk, targetName)) {
            			String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName);
            			player.sendMessage(message);
            			return true;
            		}
            		player.sendMessage(ClaimLanguage.getMessage("error"));
            		return true;
            	}
            	if(args[0].equalsIgnoreCase("remove")) {
            		if(args[1].equalsIgnoreCase("*")) {
    	        		if(ClaimMain.removeAllAdminClaimMembers(args[2])) {
    	        			String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", args[2]);
    	        			player.sendMessage(message);
    	        			return true;
    	        		}
    	        		player.sendMessage(ClaimLanguage.getMessage("error"));
    	        		return true;
            		}
            		Chunk chunk = ClaimMain.getAdminChunkByName(args[1]);
            		if(chunk == null) {
            			player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
            			return true;
            		}
            		OfflinePlayer target = Bukkit.getOfflinePlayer(args[2]);
            		if(!target.hasPlayedBefore() && !target.isOnline()) {
            			player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
            			return true;
            		}
            		String targetName = target.getName();
            		if(!ClaimMain.getClaimMembers(chunk).contains(targetName)) {
            			String message = ClaimLanguage.getMessage("not-member").replaceAll("%player%", targetName);
            			player.sendMessage(message);
            			return true;
            		}
            		if(ClaimMain.removeAdminClaimMembers(chunk, args[2])) {
            			String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", args[2]);
            			player.sendMessage(message);
            			return true;
            		}
            		player.sendMessage(ClaimLanguage.getMessage("error"));
            		return true;
            	}
        	}
        	if(args[0].equalsIgnoreCase("set-status-setting")) {
        		String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
        		if(ClaimGuis.isAPerm(perm)) {
            		if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
    	                File configFile = new File(plugin.getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("status-settings."+perm, Boolean.parseBoolean(args[2]));
    	                ClaimSettings.getStatusSettings().put(perm, Boolean.parseBoolean(args[2]));
    	                try {
    	                	config.save(configFile);
    	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Status Setting '"+perm+"'").replaceAll("%value%", args[2]));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return true;
            		}
            		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
            		return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-incorrect"));
        		return true;
        	}
        	if(args[0].equalsIgnoreCase("set-default-value")) {
        		String perm = args[1].substring(0, 1).toUpperCase() + args[1].substring(1).toLowerCase();
        		if(ClaimGuis.isAPerm(perm)) {
            		if(args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("false")) {
    	                File configFile = new File(plugin.getDataFolder(), "config.yml");
    	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
    	                config.set("default-values-settings."+perm, Boolean.parseBoolean(args[2]));
    	                ClaimSettings.getDefaultValues().put(perm, Boolean.parseBoolean(args[2]));
    	                try {
    	                	config.save(configFile);
    	                	sender.sendMessage(ClaimLanguage.getMessage("setting-changed-via-command").replaceAll("%setting%", "Default Values Setting '"+perm+"'").replaceAll("%value%", args[2]));
    					} catch (IOException e) {
    						e.printStackTrace();
    					}
    	                return true;
            		}
            		sender.sendMessage(ClaimLanguage.getMessage("setting-must-be-boolean"));
            		return true;
        		}
        		sender.sendMessage(ClaimLanguage.getMessage("setting-incorrect"));
        		return true;
        	}
        }
        
        if(args.length == 4) {
        	if(args[0].equalsIgnoreCase("player")) {
        		if(args[1].equalsIgnoreCase("set-claim-cost")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("players."+args[2]+".claim-cost", amount);
	                cTarget.setClaimCost(amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-claim-cost-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("players."+args[2]+".claim-cost-multiplier", amount);
	                cTarget.setClaimCostMultiplier(amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-claim-cost-multiplier-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-members")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("players."+args[2]+".max-members", amount);
	                cTarget.setMaxMembers(amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-member-limit-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-limit")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("players."+args[2]+".max-claims", amount);
	                cTarget.setMaxClaims(amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-max-claim-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("add-limit")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                int new_amount = cTarget.getMaxClaims()+amount;
	                config.set("players."+name+".max-claims", new_amount);
	                cTarget.setMaxClaims(new_amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-max-claim-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(new_amount)));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-radius")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("players."+args[2]+".max-radius-claims", amount);
	                cTarget.setMaxRadiusClaims(amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-max-radius-claim-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("add-radius")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-number"));
	                    return true;
	                }

	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                int new_amount = cTarget.getMaxRadiusClaims()+amount;
	                config.set("players."+name+".max-radius-claims", new_amount);
	                cTarget.setMaxClaims(new_amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-max-radius-claim-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(new_amount)));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("add-members")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("member-limit-radius-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("member-limit-radius-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                int new_amount = cTarget.getMaxRadiusClaims()+amount;
	                config.set("players."+name+".max-members", new_amount);
	                cTarget.setMaxClaims(new_amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-member-limit-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(new_amount)));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-delay")) {
        			Player target = Bukkit.getPlayer(args[2]);
        			if(target == null) {
        				sender.sendMessage(ClaimLanguage.getMessage("player-not-online"));
        				return true;
        			}
        			String name = target.getName();
        			CPlayer cTarget = CPlayerMain.getCPlayer(name);
	                int amount;
	                try {
	                    amount = Integer.parseInt(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("players."+args[2]+".teleportation-delay", amount);
	                cTarget.setTeleportationDelay(amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-player-teleportation-delay-success").replaceAll("%player%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
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
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-cost-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("groups."+args[2]+".claim-cost", amount);
	                ClaimSettings.getGroupsSettings().get(args[2]).put("claim-cost", amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-group-claim-cost-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-claim-cost-multiplier")) {
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-cost-multiplier-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("groups."+args[2]+".claim-cost-multiplier", amount);
	                ClaimSettings.getGroupsSettings().get(args[2]).put("claim-cost-multiplier", amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-group-claim-cost-multiplier-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
        		if(args[1].equalsIgnoreCase("set-members")) {
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("member-limit-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("groups."+args[2]+".max-members", amount);
	                ClaimSettings.getGroupsSettings().get(args[2]).put("max-members", amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-group-member-limit-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-limit")) {
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-limit-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("groups."+args[2]+".max-claims", amount);
	                ClaimSettings.getGroupsSettings().get(args[2]).put("max-claims", amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-group-max-claim-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-radius")) {
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("claim-limit-radius-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("groups."+args[2]+".max-radius-claims", amount);
	                ClaimSettings.getGroupsSettings().get(args[2]).put("max-radius-claims", amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-group-max-radius-claim-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	if(args[1].equalsIgnoreCase("set-delay")) {
	                Double amount;
	                try {
	                    amount = Double.parseDouble(args[3]);
	                    if(amount < 0) {
	                        sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-positive"));
	                        return true;
	                    }
	                } catch (NumberFormatException e) {
	                    sender.sendMessage(ClaimLanguage.getMessage("teleportation-delay-must-be-number"));
	                    return true;
	                }
	                
	                File configFile = new File(plugin.getDataFolder(), "config.yml");
	                FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
	                config.set("groups."+args[2]+".teleportation-delay", amount);
	                ClaimSettings.getGroupsSettings().get(args[2]).put("teleportation-delay", amount);
	                try {
	                	config.save(configFile);
	                	sender.sendMessage(ClaimLanguage.getMessage("set-group-teleportation-delay-success").replaceAll("%group%", args[2]).replaceAll("%amount%", String.valueOf(args[3])));
	                	return true;
					} catch (IOException e) {
						e.printStackTrace();
					}
	                return true;
	        	}
	        	sender.sendMessage(ClaimLanguage.getMessage("syntax-aclaim"));
	        	return true;
        	}
        }
        
        if(sender instanceof Player) {
        	Player player = (Player) sender;
        	
            if(ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
            	player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
            	return true;
            }
        	
	        Chunk chunk = player.getLocation().getChunk();
	        ClaimMain.createAdminClaim(player, chunk);
	        return true;
        }

        return true;
    }
}
