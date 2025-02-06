package fr.xyness.SCS.Listeners;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.jetbrains.annotations.NotNull;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import fr.xyness.SCS.Types.WorldMode;

public class SpigotClaimEvents implements Listener {

	
    // ***************
    // *  Variables  *
    // ***************
	
	
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Constructor for ClaimEventsEnterLeave.
     *
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public SpigotClaimEvents(SimpleClaimSystem instance) {
    	this.instance = instance;
    }
    
    
    // *******************
    // *  EventHandlers  *
    // *******************
    
    
	/**
	 * Handles player chat events for claim chat.
	 * 
	 * @param event AsyncPlayerChatEvent event.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerChat(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		String playerName = player.getName();
		CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
		if(cPlayer == null) return;
		if(cPlayer.getClaimChat()) {
			event.setCancelled(true);
			String msg = instance.getLanguage().getMessage("chat-format").replace("%player%",playerName).replace("%message%", event.getMessage());
			player.sendMessage(msg);
			for(String p : instance.getMain().getAllMembersWithPlayerParallel(playerName)) {
				Player target = Bukkit.getPlayer(p);
				if(target != null && target.isOnline()) {
					target.sendMessage(msg);
				}
			}
		}
	}
	
    /**
     * Handles player pickup items events to prevent player pickuping in claims.
     * @param event the pickup items event.
     */
    @EventHandler
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
    	Chunk chunk = event.getItem().getLocation().getChunk();
    	Player player = event.getPlayer();
    	WorldMode mode = instance.getSettings().getWorldMode(player.getWorld().getName());
    	if(instance.getPlayerMain().checkPermPlayer(player, "scs.bypass")) return;
		if(instance.getMain().checkIfClaimExists(chunk)) {
			Claim claim = instance.getMain().getClaim(chunk);
			if(!claim.getPermissionForPlayer("ItemsPickup", player)) {
				event.setCancelled(true);
				instance.getMain().sendMessage(player,instance.getLanguage().getMessage("itemspickup"), instance.getSettings().getSetting("protection-message"));
				return;
			}
		} else if (mode == WorldMode.SURVIVAL_REQUIRING_CLAIMS && !instance.getSettings().getSettingSRC("ItemsPickup")) {
        	event.setCancelled(true);
        	instance.getMain().sendMessage(player,instance.getLanguage().getMessage("itemspickup-mode"), instance.getSettings().getSetting("protection-message"));
        }
    }

}
