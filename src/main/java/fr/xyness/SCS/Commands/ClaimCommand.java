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
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.WorldBorder;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Guis.ClaimBansGui;
import fr.xyness.SCS.Guis.ClaimGui;
import fr.xyness.SCS.Guis.ClaimListGui;
import fr.xyness.SCS.Guis.ClaimMembersGui;
import fr.xyness.SCS.Support.ClaimWorldGuard;
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
    
    // ******************
    // *  Tab Complete  *
    // ******************

    /**
     * Handles tab completion for the /claim command.
     *
     * @param sender the command sender
     * @param cmd    the command
     * @param alias  the command alias
     * @param args   the command arguments
     * @return a list of tab completions
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim")) return new ArrayList<>();
        
        Chunk chunk = player.getLocation().getChunk();
        String playerName = player.getName();

        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            List<String> completions = new ArrayList<>();

            if (args.length == 1) {
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) completions.add("settings");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) completions.add("add");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) completions.add("remove");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.list")) completions.add("list");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.setspawn")) completions.add("setspawn");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) completions.add("setname");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) completions.add("members");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.setdesc")) completions.add("setdesc");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.chat")) completions.add("chat");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.map")) completions.add("map");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.autoclaim")) completions.add("autoclaim");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.automap")) completions.add("automap");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.see")) completions.add("see");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) completions.add("tp");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) completions.add("ban");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) completions.add("unban");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) completions.add("bans");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.fly")) completions.add("fly");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.autofly")) completions.add("autofly");
                if (CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) completions.add("owner");
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("see") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.see.others")) {
                completions.addAll(ClaimMain.getClaimsOwners());
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("setname") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("chat") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.chat")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("setdesc") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.setdesc")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("settings") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("tp") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("add") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(playerName)) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        completions.remove(playerName);
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("ban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(playerName)) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        completions.remove(playerName);
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("unban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(playerName)) {
                        completions.addAll(ClaimMain.getClaimBans(chunk));
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(playerName)) {
                        completions.addAll(ClaimMain.getClaimMembers(chunk));
                        completions.remove(playerName);
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("members") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("bans") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("owner") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(playerName));
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(playerName);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove") && !args[1].equals("*")) {
                Chunk c = ClaimMain.getChunkByClaimName(playerName, args[1]);
                completions.addAll(ClaimMain.getClaimMembers(c));
                completions.remove(playerName);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("unban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                Chunk c = ClaimMain.getChunkByClaimName(playerName, args[1]);
                completions.addAll(ClaimMain.getClaimBans(c));
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("add") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(playerName);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                completions.addAll(ClaimMain.getAllMembersOfAllPlayerClaim(playerName));
                completions.remove(playerName);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("ban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(playerName);
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("owner") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(playerName);
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

        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);

        if (args.length > 1 && args[0].equals("setdesc")) {
        	SimpleClaimSystem.executeAsync(() -> {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setdesc")) {
                	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                    return;
                }
                if (ClaimMain.getClaimsNameFromOwner(playerName).contains(args[1])) {
                    String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                    if (description.length() > Integer.parseInt(ClaimSettings.getSetting("max-length-claim-description"))) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-description-too-long")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (ClaimMain.setChunkDescription(player, chunk, description)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-set-description-success").replaceAll("%name%", args[1]).replaceAll("%description%", description)));
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

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setname")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (ClaimMain.getClaimsNameFromOwner(playerName).contains(args[1])) {
                        if (args[2].length() > Integer.parseInt(ClaimSettings.getSetting("max-length-claim-name"))) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-name-too-long")));
                            return;
                        }
                        if (args[2].contains("claim-")) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("you-cannot-use-this-name")));
                            return;
                        }
                        if (ClaimMain.checkName(playerName, args[2])) {
                            Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                            SimpleClaimSystem.executeSync(() -> {
                            	ClaimMain.setClaimName(player, chunk, args[2]);
                            	player.sendMessage(ClaimLanguage.getMessage("name-change-success").replaceAll("%name%", args[2]));
                            });
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error-name-exists").replaceAll("%name%", args[1])));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                    return;
            	});
            	
                return true;
            }
            if (args[0].equalsIgnoreCase("ban")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (args[1].equalsIgnoreCase("*")) {
                        if (cPlayer.getClaimsCount() == 0) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim")));
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
                        if (targetName.equals(playerName)) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself")));
                            return;
                        }
                        if (ClaimMain.addAllClaimBan(player, targetName)) {
                            String message = ClaimLanguage.getMessage("add-ban-all-success").replaceAll("%player%", targetName);
                            SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
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
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself")));
                        return;
                    }
                    if (ClaimMain.addClaimBan(player, chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("add-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("unban")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (args[1].equalsIgnoreCase("*")) {
                        if (cPlayer.getClaimsCount() == 0) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim")));
                            return;
                        }
                        Player target = Bukkit.getPlayer(args[2]);
                        String targetName = "";
                        if (target == null) {
                            OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[2]);
                            targetName = otarget == null ? args[2] : otarget.getName();
                        } else {
                            targetName = target.getName();
                        }
                        if (ClaimMain.removeAllClaimBan(player, targetName)) {
                            String message = ClaimLanguage.getMessage("remove-ban-all-success").replaceAll("%player%", targetName);
                            SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    if (!ClaimMain.checkBan(chunk, args[2])) {
                        String message = ClaimLanguage.getMessage("not-banned").replaceAll("%player%", args[2]);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    String targetName = ClaimMain.getRealNameFromClaimBans(chunk, args[2]);
                    if (ClaimMain.removeClaimBan(player, chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (args[1].equalsIgnoreCase("*")) {
                        if (cPlayer.getClaimsCount() == 0) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim")));
                            return;
                        }
                        Set<Chunk> chunks = ClaimMain.getChunksFromOwner(playerName);
                        for (Chunk c : chunks) {
                            if (!CPlayerMain.canAddMember(player, c)) {
                            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-add-member-anymore")));
                                return;
                            }
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
                        if (targetName.equals(playerName)) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-add-yourself")));
                            return;
                        }
                        if (ClaimMain.addAllClaimMembers(player, targetName)) {
                            String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", "all your claims");
                            SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    if (!CPlayerMain.canAddMember(player, chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-add-member-anymore")));
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
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-add-yourself")));
                        return;
                    }
                    if (ClaimMain.checkMembre(chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("already-member").replaceAll("%player%", targetName);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    if (ClaimMain.addClaimMembers(player, chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (args[1].equalsIgnoreCase("*")) {
                        if (cPlayer.getClaimsCount() == 0) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim")));
                            return;
                        }
                        if (args[2].equals(playerName)) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-remove-owner")));
                            return;
                        }
                        String targetName = args[2];
                        if (ClaimMain.removeAllClaimMembers(player, targetName)) {
                            String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", "all your claims");
                            SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    String targetName = args[2];
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-remove-owner")));
                        return;
                    }
                    if (!ClaimMain.checkMembre(chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("not-member").replaceAll("%player%", targetName);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    String realName = ClaimMain.getRealNameFromClaimMembers(chunk, targetName);
                    if (ClaimMain.removeClaimMembers(player, chunk, realName)) {
                        String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", realName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("owner")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (args[1].equalsIgnoreCase("*")) {
                        if (cPlayer.getClaimsCount() == 0) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim")));
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
                        if (targetName.equals(playerName)) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-transfer-ownership-yourself")));
                            return;
                        }
                        Set<Chunk> chunks = ClaimMain.getChunksFromOwner(playerName);
                        final String tName = targetName;
                        SimpleClaimSystem.executeSync(() -> {
                        	chunks.forEach(c -> ClaimMain.setOwner(player, tName, c, false));
                        	player.sendMessage(ClaimLanguage.getMessage("setowner-all-success").replaceAll("%owner%", tName));
                        });
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
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
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-transfer-ownership-yourself")));
                        return;
                    }
                    final String tName = targetName;
                    SimpleClaimSystem.executeSync(() -> {
                    	ClaimMain.setOwner(player, tName, chunk, false);
                    	player.sendMessage(ClaimLanguage.getMessage("setowner-claim-success").replaceAll("%owner%", tName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk)));
                    });
                    return;
                });
                
                return true;
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("ban")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[1]);
                        if (otarget == null) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[1])));
                            return;
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself")));
                        return;
                    }
                    if (ClaimMain.addClaimBan(player, chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("add-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("unban")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    if (!ClaimMain.checkBan(chunk, args[1])) {
                        String message = ClaimLanguage.getMessage("not-banned").replaceAll("%player%", args[1]);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    String targetName = ClaimMain.getRealNameFromClaimBans(chunk, args[1]);
                    if (ClaimMain.removeClaimBan(player, chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("owner")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[1]);
                        if (otarget == null) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[1])));
                            return;
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-transfer-ownership-yourself")));
                        return;
                    }
                    final String tName = targetName;
                    SimpleClaimSystem.executeSync(() -> {
                    	ClaimMain.setOwner(player, tName, chunk, false);
                    	player.sendMessage(ClaimLanguage.getMessage("setowner-claim-success").replaceAll("%owner%", tName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk)));
                    });
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    String targetName = args[1];
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-remove-owner")));
                        return;
                    }
                    if (!ClaimMain.checkMembre(chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("not-member").replaceAll("%player%", targetName);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    String realName = ClaimMain.getRealNameFromClaimMembers(chunk, targetName);
                    if (ClaimMain.removeClaimMembers(player, chunk, realName)) {
                        String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", realName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    if (!CPlayerMain.canAddMember(player, chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-add-member-anymore")));
                        return;
                    }
                    Player target = Bukkit.getPlayer(args[1]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = CPlayerMain.getOfflinePlayer(args[1]);
                        if (otarget == null) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[1])));
                            return;
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (targetName.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-add-yourself")));
                        return;
                    }
                    if (ClaimMain.checkMembre(chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("already-member").replaceAll("%player%", targetName);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    if (ClaimMain.addClaimMembers(player, chunk, targetName)) {
                        String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(message));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("see")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.see.others")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    String world = player.getWorld().getName();
                    if (ClaimSettings.isWorldDisabled(world)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world)));
                        return;
                    }
                    if (ClaimMain.getPlayerClaimsCount(args[1]) == 0) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("target-does-not-have-claim").replaceAll("%name%", args[1])));
                        return;
                    }
                    Set<Chunk> chunks = ClaimMain.getChunksFromOwner(args[1]);
                    SimpleClaimSystem.executeSync(() -> chunks.forEach(c -> ClaimMain.displayChunk(player, c, false)));
                    return;
            	});
            	
                return true;
            }
            if (args[0].equalsIgnoreCase("settings")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> new ClaimGui(player, chunk));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("members")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    cPlayer.setGuiPage(1);
                    SimpleClaimSystem.executeSync(() -> new ClaimMembersGui(player, chunk, 1));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("bans")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    cPlayer.setGuiPage(1);
                    SimpleClaimSystem.executeSync(() -> new ClaimBansGui(player, chunk, 1));
                    return;
            	});
            	
                return true;
            }
            if (args[0].equalsIgnoreCase("tp")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> ClaimMain.goClaim(player, ClaimMain.getClaimLocationByChunk(chunk)));
                    return;
            	});
            	
                return true;
            }
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("fly")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (SimpleClaimSystem.isFolia()) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("fly-disabled-on-this-server")));
                        return;
                    }
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.fly")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    Claim claim = ClaimMain.getClaimFromChunk(chunk);
                    if (claim.getOwner().equals(playerName)) {
                        if (cPlayer.getClaimFly()) {
                            CPlayerMain.removePlayerFly(player);
                            SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("fly-disabled")));
                            return;
                        }
                        CPlayerMain.activePlayerFly(player);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("fly-enabled")));
                        return;
                    }
                    if (!claim.getPermission("Fly")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-fly-in-this-claim")));
                        return;
                    }
                    if (cPlayer.getClaimFly()) {
                        CPlayerMain.removePlayerFly(player);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("fly-disabled")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> {
                        CPlayerMain.activePlayerFly(player);
                        player.sendMessage(ClaimLanguage.getMessage("fly-enabled"));
                    });
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("autofly")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (SimpleClaimSystem.isFolia()) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("fly-disabled-on-this-server")));
                        return;
                    }
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.autofly")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (cPlayer.getClaimAutofly()) {
                        cPlayer.setClaimAutofly(false);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("autofly-disabled")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> {
                        cPlayer.setClaimAutofly(true);
                        player.sendMessage(ClaimLanguage.getMessage("autofly-enabled"));
                    });
                    Chunk chunk = player.getLocation().getChunk();
                    if (ClaimMain.checkIfClaimExists(chunk)) {
                        Claim claim = ClaimMain.getClaimFromChunk(chunk);
                        if (claim.getOwner().equals(playerName) || claim.getPermission("Fly")) {
                        	if(cPlayer.getClaimFly()) return;
                        	SimpleClaimSystem.executeSync(() -> {
                                CPlayerMain.activePlayerFly(player);
                                player.sendMessage(ClaimLanguage.getMessage("fly-enabled"));	
                        	});
                            return;
                        }
                    }
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("chat")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.chat")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    if (cPlayer.getClaimChat()) {
                    	SimpleClaimSystem.executeSync(() -> {
                            cPlayer.setClaimChat(false);
                            player.sendMessage(ClaimLanguage.getMessage("talking-now-in-public"));
                    	});
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> {
                        cPlayer.setClaimChat(true);
                        player.sendMessage(ClaimLanguage.getMessage("talking-now-in-claim"));
                    });
                    return;
            	});
            	
                return true;
            }
            if (args[0].equalsIgnoreCase("automap")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.automap")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    String world = player.getWorld().getName();
                    if (ClaimSettings.isWorldDisabled(world)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world)));
                        return;
                    }
                    if (cPlayer.getClaimAutomap()) {
                    	SimpleClaimSystem.executeSync(() -> {
                            cPlayer.setClaimAutomap(false);
                            player.sendMessage(ClaimLanguage.getMessage("automap-off"));
                    	});
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> {
                        cPlayer.setClaimAutomap(true);
                        player.sendMessage(ClaimLanguage.getMessage("automap-on"));
                    });
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("map")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.map")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    String world = player.getWorld().getName();
                    if (ClaimSettings.isWorldDisabled(world)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world)));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> ClaimMain.getMap(player, player.getLocation().getChunk()));
                    return;
            	});
            	
                return true;
            }
            if (args[0].equalsIgnoreCase("autoclaim")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.autoclaim")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    String world = player.getWorld().getName();
                    if (ClaimSettings.isWorldDisabled(world)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world)));
                        return;
                    }
                    if (cPlayer.getClaimAutoclaim()) {
                    	SimpleClaimSystem.executeSync(() -> {
                            cPlayer.setClaimAutoclaim(false);
                            player.sendMessage(ClaimLanguage.getMessage("autoclaim-off"));
                    	});
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> {
                        cPlayer.setClaimAutoclaim(true);
                        player.sendMessage(ClaimLanguage.getMessage("autoclaim-on"));
                    });
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("setspawn")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setspawn")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    Location l = player.getLocation();
                    SimpleClaimSystem.executeSync(() -> {
                    	ClaimMain.setClaimLocation(player, chunk, l);
                    	player.sendMessage(ClaimLanguage.getMessage("loc-change-success").replaceAll("%coords%", ClaimMain.getClaimCoords(chunk)));
                    });
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("settings")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> new ClaimGui(player, chunk));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("members")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    cPlayer.setGuiPage(1);
                    SimpleClaimSystem.executeSync(() -> new ClaimMembersGui(player, chunk, 1));
                    return;
            	});
            	
                return true;
            }
            if (args[0].equalsIgnoreCase("bans")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    Chunk chunk = player.getLocation().getChunk();
                    String owner = ClaimMain.getOwnerInClaim(chunk);
                    if (!ClaimMain.checkIfClaimExists(chunk)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("free-territory")));
                        return;
                    }
                    if (!owner.equals(playerName)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("territory-not-yours")));
                        return;
                    }
                    cPlayer.setGuiPage(1);
                    SimpleClaimSystem.executeSync(() -> new ClaimBansGui(player, chunk, 1));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("list")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.list")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    cPlayer.setGuiPage(1);
                    cPlayer.setChunk(null);
                    SimpleClaimSystem.executeSync(() -> new ClaimListGui(player, 1, "owner"));
                    return;
                });
                
                return true;
            }
            if (args[0].equalsIgnoreCase("see")) {
                SimpleClaimSystem.executeAsync(() -> {
                	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.see")) {
                		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                        return;
                    }
                    String world = player.getWorld().getName();
                    if (ClaimSettings.isWorldDisabled(world)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world)));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> ClaimMain.displayChunk(player, player.getLocation().getChunk(), false));
                    return;
                });
                
                return true;
            }
            SimpleClaimSystem.executeAsync(() -> {
            	if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.radius")) {
            		SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission")));
                    return;
                }
                if (ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
                	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName())));
                    return;
                }
                try {
                    int radius = Integer.parseInt(args[0]);
                    if (!cPlayer.canRadiusClaim(radius)) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim")));
                        return;
                    }
                    Set<Chunk> chunks = new HashSet<>(getChunksInRadius(player.getLocation(), radius));
                    if (ClaimSettings.getBooleanSetting("claim-confirmation")) {
                        if (isOnCreate.contains(player)) {
                            isOnCreate.remove(player);
                            SimpleClaimSystem.executeSync(() -> ClaimMain.createClaimRadius(player, chunks, radius));
                            return;
                        }
                        isOnCreate.add(player);
                        String AnswerA = ClaimLanguage.getMessage("claim-confirmation-button");
                        TextComponent AnswerA_C = new TextComponent(AnswerA);
                        AnswerA_C.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ClaimLanguage.getMessage("claim-confirmation-button")).create()));
                        AnswerA_C.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim " + String.valueOf(radius)));
                        TextComponent finale = new TextComponent(ClaimLanguage.getMessage("claim-confirmation-ask"));
                        finale.addExtra(AnswerA_C);
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(finale));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> ClaimMain.createClaimRadius(player, chunks, radius));
                } catch (NumberFormatException e) {
                	SimpleClaimSystem.executeSync(() -> ClaimMain.getHelp(player, args[0], "claim"));
                }
                return;
            });
            
            return true;
        }

        SimpleClaimSystem.executeAsync(() -> {
        	if (args.length > 0) {
        		SimpleClaimSystem.executeSync(() -> ClaimMain.getHelp(player, args[0], "claim"));
                return;
            }

            String world = player.getWorld().getName();
            if (ClaimSettings.isWorldDisabled(world)) {
            	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world)));
                return;
            }

            if (ClaimSettings.getBooleanSetting("worldguard")) {
                if (!ClaimWorldGuard.checkFlagClaim(player)) {
                	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("worldguard-cannot-claim-in-region")));
                    return;
                }
            }

            if (ClaimSettings.getBooleanSetting("claim-confirmation")) {
                if (isOnCreate.contains(player)) {
                    isOnCreate.remove(player);
                    Chunk chunk = player.getLocation().getChunk();
                    SimpleClaimSystem.executeSync(() -> ClaimMain.createClaim(player, chunk));
                    return;
                }
                isOnCreate.add(player);
                ClaimMain.displayChunk(player, player.getChunk(), false);
                String AnswerA = ClaimLanguage.getMessage("claim-confirmation-button");
                TextComponent AnswerA_C = new TextComponent(AnswerA);
                AnswerA_C.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ClaimLanguage.getMessage("claim-confirmation-button")).create()));
                AnswerA_C.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim"));
                TextComponent finale = new TextComponent(ClaimLanguage.getMessage("claim-confirmation-ask"));
                finale.addExtra(AnswerA_C);
                SimpleClaimSystem.executeSync(() -> player.sendMessage(finale));
                return;
            }

            Chunk chunk = player.getLocation().getChunk();
            SimpleClaimSystem.executeSync(() -> ClaimMain.createClaim(player, chunk));
        });
        
        return true;
    }
    
    // ********************
    // *  Other Methods  *
    // ********************

    /**
     * Method to get chunks in radius.
     *
     * @param center the center location
     * @param radius the radius in chunks
     * @return list of chunks within the radius
     */
    public static List<Chunk> getChunksInRadius(Location center, int radius) {
        List<Chunk> chunks = new ArrayList<>();
        Chunk centerChunk = center.getChunk();
        int startX = centerChunk.getX() - radius;
        int startZ = centerChunk.getZ() - radius;
        int endX = centerChunk.getX() + radius;
        int endZ = centerChunk.getZ() + radius;

        for (int x = startX; x <= endX; x++) {
            for (int z = startZ; z <= endZ; z++) {
                chunks.add(center.getWorld().getChunkAt(x, z));
            }
        }
        return chunks;
    }
}
