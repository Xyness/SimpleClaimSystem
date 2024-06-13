package fr.xyness.SCS.Others;

import java.lang.reflect.Field;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public class CacheGui {

	
	// ***************
	// *  Variables  *
	// ***************
	
	
	private static final ConcurrentHashMap<String, ItemStack> playerHeadCache = new ConcurrentHashMap<>();
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Method to add a player head to the cache
    public static void addPlayerHead(String playerName) {
        if (!playerHeadCache.containsKey(playerName)) {
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            SkullMeta meta = (SkullMeta) head.getItemMeta();
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
            playerHeadCache.put(playerName, head);
        }
    }
	
	// Method to get the player head from the cache or not
	public static ItemStack getPlayerHead(String playerName) {
		
        if (playerHeadCache.containsKey(playerName)) {
            return playerHeadCache.get(playerName);
        }
        
        // Create player head
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
    	OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
    	meta.setOwningPlayer(player);
        head.setItemMeta(meta);

        // Cache the head
        playerHeadCache.put(playerName, head);
        return head;
    }
	
	// Method to clear cache
    public static void clearCache() {
        playerHeadCache.clear();
    }

    // Method to remove a player from cache
    public static void removePlayerFromCache(UUID playerId) {
        playerHeadCache.remove(playerId);
    }
}
