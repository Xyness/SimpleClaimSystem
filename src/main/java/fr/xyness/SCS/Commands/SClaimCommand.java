package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;

/**
 * Handles the /sclaim command for selling and canceling claim sales.
 */
public class SClaimCommand implements CommandExecutor, TabCompleter {
    
    // ******************
    // *  Tab Complete  *
    // ******************

    /**
     * Provides tab completion suggestions for the /sclaim command.
     *
     * @param sender Source of the command
     * @param cmd Command which was executed
     * @param alias Alias of the command which was used
     * @param args Passed command arguments
     * @return A list of tab completion suggestions
     */
	@Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        CompletableFuture<List<String>> future = CompletableFuture.supplyAsync(() -> {
            List<String> completions = new ArrayList<>();

            if (!CPlayerMain.checkPermPlayer(player, "scs.command.sclaim")) return completions;

            if (args.length == 1) {
                completions.add("sell");
                completions.add("cancel");
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
                completions.addAll(ClaimMain.getClaimsNameFromOwner(player.getName()));
                return completions;
            }

            if (args.length == 2 && args[0].equalsIgnoreCase("cancel")) {
                Set<String> names = ClaimMain.getClaimsNameInSaleFromOwner(player.getName());
                if (names != null) completions.addAll(names);
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
     * Executes the given command, returning its success.
     *
     * @param sender Source of the command
     * @param command Command which was executed
     * @param label Alias of the command which was used
     * @param args Passed command arguments
     * @return true if a valid command, otherwise false
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        
        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        String playerName = player.getName();
        
        if (!ClaimSettings.getBooleanSetting("economy")) {
            player.sendMessage(ClaimLanguage.getMessage("economy-disabled"));
            return true;
        }
        
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("sell")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (chunk == null) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-player-not-found")));
                        return;
                    }
                    try {
                        Double price = Double.parseDouble(args[2]);
                        Double max_price = Double.parseDouble(ClaimSettings.getSetting("max-sell-price"));
                        if (price > max_price || price <= 0) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("sell-claim-price-syntax").replaceAll("%max-price%", ClaimSettings.getSetting("max-sell-price"))));
                            return;
                        }
                        if (ClaimMain.setChunkSale(player, chunk, price)) {
                        	SimpleClaimSystem.executeSync(() -> {
	                            player.sendMessage(ClaimLanguage.getMessage("claim-for-sale-success").replaceAll("%name%", args[1]).replaceAll("%price%", args[2]));
	                            for (Player p : Bukkit.getOnlinePlayers()) {
	                                p.sendMessage(ClaimLanguage.getMessage("claim-for-sale-success-broadcast").replaceAll("%name%", args[1]).replaceAll("%price%", args[2]).replaceAll("%player%", playerName));
	                            }
                        	});
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                    } catch (NumberFormatException e) {
                    	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-price-must-be-number")));
                    }
                    return;
            	});
            	return true;
            }
            player.sendMessage(ClaimLanguage.getMessage("help-command.sclaim-sclaim").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")));
            return true;
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("cancel")) {
            	SimpleClaimSystem.executeAsync(() -> {
                    Chunk chunk = ClaimMain.getChunkByClaimName(playerName, args[1]);
                    if (ClaimMain.claimIsInSale(chunk)) {
                        if (ClaimMain.delChunkSale(player, chunk)) {
                        	SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-in-sale-cancel").replaceAll("%name%", args[1])));
                            return;
                        }
                        SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("error")));
                        return;
                    }
                    SimpleClaimSystem.executeSync(() -> player.sendMessage(ClaimLanguage.getMessage("claim-is-not-in-sale")));
            	});
                return true;
            }
        }
        
        player.sendMessage(ClaimLanguage.getMessage("help-command.sclaim-sclaim").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")));
        return true;
    }
}
