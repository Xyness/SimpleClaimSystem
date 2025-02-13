package fr.xyness.SCS.Commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.geysermc.floodgate.api.FloodgateApi;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Guis.ClaimsGui;
import fr.xyness.SCS.Guis.Bedrock.BClaimsGui;
import fr.xyness.SCS.Types.CPlayer;

/**
 * Handles the /claims command, which opens a GUI for the player to view their claims.
 */
public class ClaimsCommand implements CommandExecutor {
	
	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimsCommand.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimsCommand(SimpleClaimSystem instance) {
    	this.instance = instance;
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
    	
        // Check if the sender is a player
        if (!(sender instanceof Player)) {
        	sender.sendMessage(instance.getLanguage().getMessage("command-only-by-players"));
            return false;
        }

        // Get data
        Player player = (Player) sender;
        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
        
        // Open the claims GUI for the player
        cPlayer.setGuiPage(1);
        if(instance.getSettings().getBooleanSetting("floodgate")) {
        	if(FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId())) {
        		new BClaimsGui(player,instance,"all");
        		return true;
        	}
        }
        new ClaimsGui(player, 1, "all",instance);
        
        return true;
    }

}
