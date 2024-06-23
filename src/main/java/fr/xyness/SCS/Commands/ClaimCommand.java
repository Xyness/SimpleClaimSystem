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

        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            List<String> completions = new ArrayList<>();

            if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim")) return completions;

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
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("see") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.see.others")) {
                completions.addAll(ClaimMain.getClaimsOwners());
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("setname") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("chat") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.chat")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("setdesc") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.setdesc")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("settings") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("tp") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("add") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(player.getName())) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        completions.remove(player.getName());
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("ban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(player.getName())) {
                        completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                        completions.remove(player.getName());
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("unban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(player.getName())) {
                        completions.addAll(ClaimMain.getClaimBans(chunk));
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("remove") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (ClaimMain.checkIfClaimExists(chunk)) {
                    if (owner.equals(player.getName())) {
                        completions.addAll(ClaimMain.getClaimMembers(chunk));
                        completions.remove(player.getName());
                    }
                }
                completions.add("*");
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("members") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("bans") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }
            if (args.length == 2 && args[0].equalsIgnoreCase("owner") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(player.getName());
                return completions;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove") && !args[1].equals("*")) {
                Chunk chunk = ClaimMain.getChunkByClaimName(player.getName(), args[1]);
                completions.addAll(ClaimMain.getClaimMembers(chunk));
                completions.remove(player.getName());
                return completions;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("unban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                Chunk chunk = ClaimMain.getChunkByClaimName(player.getName(), args[1]);
                completions.addAll(ClaimMain.getClaimBans(chunk));
                return completions;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("add") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(player.getName());
                return completions;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("remove") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                completions.addAll(ClaimMain.getAllMembersOfAllPlayerClaim(player.getName()));
                completions.remove(player.getName());
                return completions;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("ban") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(player.getName());
                return completions;
            }
            if (args.length == 3 && args[0].equalsIgnoreCase("owner") && CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                completions.addAll(Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()));
                completions.remove(player.getName());
                return completions;
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

        if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim")) {
            sender.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            return false;
        }

        if (args.length > 1 && args[0].equals("setdesc")) {
            if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setdesc")) {
                player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                return false;
            }
            if (ClaimMain.getClaimsNameFromOwner(playerName).contains(args[1])) {
                String description = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                if (description.length() > Integer.parseInt(ClaimSettings.getSetting("max-length-claim-description"))) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-description-too-long"));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (ClaimMain.setChunkDescription(player, chunk, description)) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-set-description-success").replaceAll("%name%", args[1]).replaceAll("%description%", description));
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
            return true;
        }

        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("setname")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setname")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (ClaimMain.getClaimsNameFromOwner(playerName).contains(args[1])) {
                    if (args[2].length() > Integer.parseInt(ClaimSettings.getSetting("max-length-claim-name"))) {
                        player.sendMessage(ClaimLanguage.getMessage("claim-name-too-long"));
                        return true;
                    }
                    if (args[2].contains("claim-")) {
                        player.sendMessage(ClaimLanguage.getMessage("you-cannot-use-this-name"));
                        return true;
                    }
                    if (ClaimMain.checkName(playerName, args[2])) {
                        Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                        ClaimMain.setClaimName(player, chunk, args[2]);
                        player.sendMessage(ClaimLanguage.getMessage("name-change-success").replaceAll("%name%", args[2]));
                        return true;
                    }
                    player.sendMessage(ClaimLanguage.getMessage("error-name-exists").replaceAll("%name%", args[1]));
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                return true;
            }
            if (args[0].equalsIgnoreCase("ban")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (args[1].equalsIgnoreCase("*")) {
                    if (cPlayer.getClaimsCount() == 0) {
                        player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim"));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                        if (otarget == null) {
                            player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
                            return true;
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (targetName.equals(playerName)) {
                        player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself"));
                        return true;
                    }
                    if (ClaimMain.addAllClaimBan(player, targetName)) {
                        String message = ClaimLanguage.getMessage("add-ban-all-success").replaceAll("%player%", targetName);
                        player.sendMessage(message);
                        return true;
                    }
                    player.sendMessage(ClaimLanguage.getMessage("error"));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                    if (otarget == null) {
                        player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
                        return true;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself"));
                    return true;
                }
                if (ClaimMain.addClaimBan(player, chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("add-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("unban")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (args[1].equalsIgnoreCase("*")) {
                    if (cPlayer.getClaimsCount() == 0) {
                        player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim"));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                        if (otarget == null) {
                            targetName = args[2];
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (ClaimMain.removeAllClaimBan(player, targetName)) {
                        String message = ClaimLanguage.getMessage("remove-ban-all-success").replaceAll("%player%", targetName);
                        player.sendMessage(message);
                        return true;
                    }
                    player.sendMessage(ClaimLanguage.getMessage("error"));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                if (!ClaimMain.checkBan(chunk, args[2])) {
                    String message = ClaimLanguage.getMessage("not-banned").replaceAll("%player%", args[2]);
                    player.sendMessage(message);
                    return true;
                }
                String targetName = ClaimMain.getRealNameFromClaimBans(chunk, args[2]);
                if (ClaimMain.removeClaimBan(player, chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (args[1].equalsIgnoreCase("*")) {
                    if (cPlayer.getClaimsCount() == 0) {
                        player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim"));
                        return true;
                    }
                    Set<Chunk> chunks = ClaimMain.getChunksFromOwner(playerName);
                    for (Chunk c : chunks) {
                        if (!CPlayerMain.canAddMember(player, c)) {
                            player.sendMessage(ClaimLanguage.getMessage("cant-add-member-anymore"));
                            return true;
                        }
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                        if (otarget == null) {
                            player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
                            return true;
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (targetName.equals(playerName)) {
                        player.sendMessage(ClaimLanguage.getMessage("cant-add-yourself"));
                        return true;
                    }
                    if (ClaimMain.addAllClaimMembers(player, targetName)) {
                        String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", "all your claims");
                        player.sendMessage(message);
                        return true;
                    }
                    player.sendMessage(ClaimLanguage.getMessage("error"));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                if (!CPlayerMain.canAddMember(player, chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-add-member-anymore"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                    if (otarget == null) {
                        player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
                        return true;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-add-yourself"));
                    return true;
                }
                if (ClaimMain.checkMembre(chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("already-member").replaceAll("%player%", targetName);
                    player.sendMessage(message);
                    return true;
                }
                if (ClaimMain.addClaimMembers(player, chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (args[1].equalsIgnoreCase("*")) {
                    if (cPlayer.getClaimsCount() == 0) {
                        player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim"));
                        return true;
                    }
                    if (args[2].equals(playerName)) {
                        player.sendMessage(ClaimLanguage.getMessage("cant-remove-owner"));
                        return true;
                    }
                    String targetName = args[2];
                    if (ClaimMain.removeAllClaimMembers(player, targetName)) {
                        String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", "all your claims");
                        player.sendMessage(message);
                        return true;
                    }
                    player.sendMessage(ClaimLanguage.getMessage("error"));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                String targetName = args[2];
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-remove-owner"));
                    return true;
                }
                if (!ClaimMain.checkMembre(chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("not-member").replaceAll("%player%", targetName);
                    player.sendMessage(message);
                    return true;
                }
                String realName = ClaimMain.getRealNameFromClaimMembers(chunk, targetName);
                if (ClaimMain.removeClaimMembers(player, chunk, realName)) {
                    String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", realName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("owner")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (args[1].equalsIgnoreCase("*")) {
                    if (cPlayer.getClaimsCount() == 0) {
                        player.sendMessage(ClaimLanguage.getMessage("player-has-no-claim"));
                        return true;
                    }
                    Player target = Bukkit.getPlayer(args[2]);
                    String targetName = "";
                    if (target == null) {
                        OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                        if (otarget == null) {
                            player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
                            return true;
                        }
                        targetName = otarget.getName();
                    } else {
                        targetName = target.getName();
                    }
                    if (targetName.equals(playerName)) {
                        player.sendMessage(ClaimLanguage.getMessage("cant-transfer-ownership-yourself"));
                        return true;
                    }
                    Set<Chunk> chunks = ClaimMain.getChunksFromOwner(playerName);
                    for (Chunk c : chunks) {
                        ClaimMain.setOwner(player, targetName, c, false);
                    }
                    player.sendMessage(ClaimLanguage.getMessage("setowner-all-success").replaceAll("%owner%", targetName));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[2]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[2]);
                    if (otarget == null) {
                        player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[2]));
                        return true;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-transfer-ownership-yourself"));
                    return true;
                }
                ClaimMain.setOwner(player, targetName, chunk, false);
                player.sendMessage(ClaimLanguage.getMessage("setowner-claim-success").replaceAll("%owner%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk)));
                return true;
            }
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("ban")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.ban")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[1]);
                    if (otarget == null) {
                        player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[1]));
                        return true;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-ban-yourself"));
                    return true;
                }
                if (ClaimMain.addClaimBan(player, chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("add-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("unban")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.unban")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                if (!ClaimMain.checkBan(chunk, args[1])) {
                    String message = ClaimLanguage.getMessage("not-banned").replaceAll("%player%", args[1]);
                    player.sendMessage(message);
                    return true;
                }
                String targetName = ClaimMain.getRealNameFromClaimBans(chunk, args[1]);
                if (ClaimMain.removeClaimBan(player, chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("remove-ban-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("owner")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.owner")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[1]);
                    if (otarget == null) {
                        player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[1]));
                        return true;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-transfer-ownership-yourself"));
                    return true;
                }
                ClaimMain.setOwner(player, targetName, chunk, false);
                player.sendMessage(ClaimLanguage.getMessage("setowner-claim-success").replaceAll("%owner%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk)));
                return true;
            }
            if (args[0].equalsIgnoreCase("remove")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.remove")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                String targetName = args[1];
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-remove-owner"));
                    return true;
                }
                if (!ClaimMain.checkMembre(chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("not-member").replaceAll("%player%", targetName);
                    player.sendMessage(message);
                    return true;
                }
                String realName = ClaimMain.getRealNameFromClaimMembers(chunk, targetName);
                if (ClaimMain.removeClaimMembers(player, chunk, realName)) {
                    String message = ClaimLanguage.getMessage("remove-member-success").replaceAll("%player%", realName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("add")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.add")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                if (!CPlayerMain.canAddMember(player, chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-add-member-anymore"));
                    return true;
                }
                Player target = Bukkit.getPlayer(args[1]);
                String targetName = "";
                if (target == null) {
                    OfflinePlayer otarget = Bukkit.getOfflinePlayerIfCached(args[1]);
                    if (otarget == null) {
                        player.sendMessage(ClaimLanguage.getMessage("player-never-played").replaceAll("%player%", args[1]));
                        return true;
                    }
                    targetName = otarget.getName();
                } else {
                    targetName = target.getName();
                }
                if (targetName.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-add-yourself"));
                    return true;
                }
                if (ClaimMain.checkMembre(chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("already-member").replaceAll("%player%", targetName);
                    player.sendMessage(message);
                    return true;
                }
                if (ClaimMain.addClaimMembers(player, chunk, targetName)) {
                    String message = ClaimLanguage.getMessage("add-member-success").replaceAll("%player%", targetName).replaceAll("%claim-name%", ClaimMain.getClaimNameByChunk(chunk));
                    player.sendMessage(message);
                    return true;
                }
                player.sendMessage(ClaimLanguage.getMessage("error"));
                return true;
            }
            if (args[0].equalsIgnoreCase("see")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.see.others")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return true;
                }
                String world = player.getWorld().getName();
                if (ClaimSettings.isWorldDisabled(world)) {
                    player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world));
                    return true;
                }
                if (ClaimMain.getPlayerClaimsCount(args[1]) == 0) {
                    player.sendMessage(ClaimLanguage.getMessage("target-does-not-have-claim").replaceAll("%name%", args[1]));
                    return true;
                }
                Set<Chunk> chunks = ClaimMain.getChunksFromOwner(args[1]);
                for (Chunk c : chunks) {
                    ClaimMain.displayChunk(player, c, false);
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("settings")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                new ClaimGui(player, chunk);
                return true;
            }
            if (args[0].equalsIgnoreCase("members")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                cPlayer.setGuiPage(1);
                new ClaimMembersGui(player, chunk, 1);
                return true;
            }
            if (args[0].equalsIgnoreCase("bans")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                cPlayer.setGuiPage(1);
                new ClaimBansGui(player, chunk, 1);
                return true;
            }
            if (args[0].equalsIgnoreCase("tp")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.tp")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return true;
                }
                Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                if (chunk == null) {
                    player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found"));
                    return true;
                }
                ClaimMain.goClaim(player, ClaimMain.getClaimLocationByChunk(chunk));
                return true;
            }
        }

        if (args.length == 1) {
            if (args[0].equalsIgnoreCase("fly")) {
                if (SimpleClaimSystem.isFolia()) {
                    player.sendMessage(ClaimLanguage.getMessage("fly-disabled-on-this-server"));
                    return true;
                }
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.fly")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                Claim claim = ClaimMain.getClaimFromChunk(chunk);
                if (claim.getOwner().equals(playerName)) {
                    if (cPlayer.getClaimFly()) {
                        CPlayerMain.removePlayerFly(player);
                        player.sendMessage(ClaimLanguage.getMessage("fly-disabled"));
                        return true;
                    }
                    CPlayerMain.activePlayerFly(player);
                    player.sendMessage(ClaimLanguage.getMessage("fly-enabled"));
                    return true;
                }
                if (!claim.getPermission("Fly")) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-fly-in-this-claim"));
                    return true;
                }
                if (cPlayer.getClaimFly()) {
                    CPlayerMain.removePlayerFly(player);
                    player.sendMessage(ClaimLanguage.getMessage("fly-disabled"));
                    return true;
                }
                CPlayerMain.activePlayerFly(player);
                player.sendMessage(ClaimLanguage.getMessage("fly-enabled"));
                return true;
            }
            if (args[0].equalsIgnoreCase("autofly")) {
                if (SimpleClaimSystem.isFolia()) {
                    player.sendMessage(ClaimLanguage.getMessage("fly-disabled-on-this-server"));
                    return true;
                }
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.autofly")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (cPlayer.getClaimAutofly()) {
                    cPlayer.setClaimAutofly(false);
                    player.sendMessage(ClaimLanguage.getMessage("autofly-disabled"));
                    return true;
                }
                cPlayer.setClaimAutofly(true);
                player.sendMessage(ClaimLanguage.getMessage("autofly-enabled"));
                Chunk chunk = player.getLocation().getChunk();

                if (ClaimMain.checkIfClaimExists(chunk)) {
                    Claim claim = ClaimMain.getClaimFromChunk(chunk);
                    if (claim.getOwner().equals(playerName) || claim.getPermission("Fly")) {
                    	if(cPlayer.getClaimFly()) return true;
                        CPlayerMain.activePlayerFly(player);
                        player.sendMessage(ClaimLanguage.getMessage("fly-enabled"));
                        return true;
                    }
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("chat")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.chat")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                if (cPlayer.getClaimChat()) {
                    cPlayer.setClaimChat(false);
                    player.sendMessage(ClaimLanguage.getMessage("talking-now-in-public"));
                    return true;
                }
                cPlayer.setClaimChat(true);
                player.sendMessage(ClaimLanguage.getMessage("talking-now-in-claim"));
                return true;
            }
            if (args[0].equalsIgnoreCase("automap")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.automap")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return true;
                }
                String world = player.getWorld().getName();
                if (ClaimSettings.isWorldDisabled(world)) {
                    player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world));
                    return true;
                }
                if (cPlayer.getClaimAutomap()) {
                    cPlayer.setClaimAutomap(false);
                    player.sendMessage(ClaimLanguage.getMessage("automap-off"));
                    return true;
                }
                cPlayer.setClaimAutomap(true);
                player.sendMessage(ClaimLanguage.getMessage("automap-on"));
                return true;
            }
            if (args[0].equalsIgnoreCase("map")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.map")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return true;
                }
                String world = player.getWorld().getName();
                if (ClaimSettings.isWorldDisabled(world)) {
                    player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world));
                    return true;
                }
                ClaimMain.getMap(player, player.getLocation().getChunk());
                return true;
            }
            if (args[0].equalsIgnoreCase("autoclaim")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.autoclaim")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return true;
                }
                String world = player.getWorld().getName();
                if (ClaimSettings.isWorldDisabled(world)) {
                    player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world));
                    return true;
                }
                if (cPlayer.getClaimAutoclaim()) {
                    cPlayer.setClaimAutoclaim(false);
                    player.sendMessage(ClaimLanguage.getMessage("autoclaim-off"));
                    return true;
                }
                cPlayer.setClaimAutoclaim(true);
                player.sendMessage(ClaimLanguage.getMessage("autoclaim-on"));
                return true;
            }
            if (args[0].equalsIgnoreCase("setspawn")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.setspawn")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                Location l = player.getLocation();
                ClaimMain.setClaimLocation(player, chunk, l);
                player.sendMessage(ClaimLanguage.getMessage("loc-change-success").replaceAll("%coords%", ClaimMain.getClaimCoords(chunk)));
                return true;
            }
            if (args[0].equalsIgnoreCase("settings")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.settings")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                new ClaimGui(player, chunk);
                return true;
            }
            if (args[0].equalsIgnoreCase("members")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.members")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                cPlayer.setGuiPage(1);
                new ClaimMembersGui(player, chunk, 1);
                return true;
            }
            if (args[0].equalsIgnoreCase("bans")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.bans")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                Chunk chunk = player.getLocation().getChunk();
                String owner = ClaimMain.getOwnerInClaim(chunk);
                if (!ClaimMain.checkIfClaimExists(chunk)) {
                    player.sendMessage(ClaimLanguage.getMessage("free-territory"));
                    return true;
                }
                if (!owner.equals(playerName)) {
                    player.sendMessage(ClaimLanguage.getMessage("territory-not-yours"));
                    return true;
                }
                cPlayer.setGuiPage(1);
                new ClaimBansGui(player, chunk, 1);
                return true;
            }
            if (args[0].equalsIgnoreCase("list")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.list")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return false;
                }
                cPlayer.setGuiPage(1);
                cPlayer.setChunk(null);
                new ClaimListGui(player, 1, "owner");
                return true;
            }
            if (args[0].equalsIgnoreCase("see")) {
                if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.see")) {
                    player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                    return true;
                }
                String world = player.getWorld().getName();
                if (ClaimSettings.isWorldDisabled(world)) {
                    player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world));
                    return true;
                }
                ClaimMain.displayChunk(player, player.getLocation().getChunk(), false);
                return true;
            }
            if (!CPlayerMain.checkPermPlayer(player, "scs.command.claim.radius")) {
                player.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
                return true;
            }
            if (ClaimSettings.isWorldDisabled(player.getWorld().getName())) {
                player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", player.getWorld().getName()));
                return true;
            }
            try {
                int radius = Integer.parseInt(args[0]);
                if (!cPlayer.canRadiusClaim(radius)) {
                    player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim"));
                    return true;
                }
                Set<Chunk> chunks = new HashSet<>(getChunksInRadius(player.getLocation(), radius));
                if (ClaimSettings.getBooleanSetting("claim-confirmation")) {
                    if (isOnCreate.contains(player)) {
                        isOnCreate.remove(player);
                        ClaimMain.createClaimRadius(player, chunks, radius);
                        return true;
                    }
                    isOnCreate.add(player);
                    String AnswerA = ClaimLanguage.getMessage("claim-confirmation-button");
                    TextComponent AnswerA_C = new TextComponent(AnswerA);
                    AnswerA_C.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ClaimLanguage.getMessage("claim-confirmation-button")).create()));
                    AnswerA_C.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim " + String.valueOf(radius)));
                    TextComponent finale = new TextComponent(ClaimLanguage.getMessage("claim-confirmation-ask"));
                    finale.addExtra(AnswerA_C);
                    player.sendMessage(finale);
                    return true;
                }
                ClaimMain.createClaimRadius(player, chunks, radius);
                return true;
            } catch (NumberFormatException e) {
                ClaimMain.getHelp(player, args[0], "claim");
                return true;
            }
        }

        if (args.length > 0) {
            ClaimMain.getHelp(player, args[0], "claim");
            return true;
        }

        String world = player.getWorld().getName();
        if (ClaimSettings.isWorldDisabled(world)) {
            player.sendMessage(ClaimLanguage.getMessage("world-disabled").replaceAll("%world%", world));
            return true;
        }

        if (ClaimSettings.getBooleanSetting("worldguard")) {
            if (!ClaimWorldGuard.checkFlagClaim(player)) {
                player.sendMessage(ClaimLanguage.getMessage("worldguard-cannot-claim-in-region"));
                return true;
            }
        }

        if (ClaimSettings.getBooleanSetting("claim-confirmation")) {
            if (isOnCreate.contains(player)) {
                isOnCreate.remove(player);
                Chunk chunk = player.getLocation().getChunk();
                ClaimMain.createClaim(player, chunk);
                return true;
            }
            isOnCreate.add(player);
            ClaimMain.displayChunk(player, player.getChunk(), false);
            String AnswerA = ClaimLanguage.getMessage("claim-confirmation-button");
            TextComponent AnswerA_C = new TextComponent(AnswerA);
            AnswerA_C.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ClaimLanguage.getMessage("claim-confirmation-button")).create()));
            AnswerA_C.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/claim"));
            TextComponent finale = new TextComponent(ClaimLanguage.getMessage("claim-confirmation-ask"));
            finale.addExtra(AnswerA_C);
            player.sendMessage(finale);
            return true;
        }

        Chunk chunk = player.getLocation().getChunk();
        ClaimMain.createClaim(player, chunk);

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
