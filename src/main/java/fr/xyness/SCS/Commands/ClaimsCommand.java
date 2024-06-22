package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Guis.ClaimsGui;

public class ClaimsCommand implements CommandExecutor {
	
	
	// ******************
	// *  Main command  *
	// ******************

	
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        
        if(!CPlayerMain.checkPermPlayer(player, "scs.command.claims")) {
        	sender.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
        	return false;
        }
        
        cPlayer.setGuiPage(1);
        ClaimsGui menu = new ClaimsGui(player,1,"all");
        menu.openInventory(player);
        return true;
    }
    
    
	// *******************
	// *  Others methods *
	// *******************
    
    
    // Method to get chunks in radius
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
