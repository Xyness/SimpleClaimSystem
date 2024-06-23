package fr.xyness.SCS.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Guis.ClaimsGui;

/**
 * Handles the /claims command, which opens a GUI for the player to view their claims.
 */
public class ClaimsCommand implements CommandExecutor {
	
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
    	
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        
        // Check if the player has the required permission
        if(!CPlayerMain.checkPermPlayer(player, "scs.command.claims")) {
            sender.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
            return false;
        }
        
        // Open the claims GUI for the player
        cPlayer.setGuiPage(1);
        new ClaimsGui(player, 1, "all");
        return true;
    }

}
