package fr.xyness.SCS.Commands;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Guis.ClaimGui;
import fr.xyness.SCS.Guis.ClaimListGui;
import fr.xyness.SCS.Guis.ClaimsGui;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class ClaimsCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
    	
        if (!(sender instanceof Player)) {
            sender.sendMessage(ClaimLanguage.getMessage("command-only-by-players"));
            return true;
        }

        Player player = (Player) sender;
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        
        if(!player.hasPermission("scs.command.claims")) {
        	sender.sendMessage(ClaimLanguage.getMessage("cmd-no-permission"));
        	return false;
        }
        
        cPlayer.setGuiPage(1);
        ClaimsGui menu = new ClaimsGui(player,1,"all");
        menu.openInventory(player);

        return true;
    }
    
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
