package fr.xyness.SCS.Guis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * Class representing the Claim GUI.
 */
public class ClaimMainGui implements InventoryHolder {

	
    // ***************
    // *  Variables  *
    // ***************

	
    /** Inventory for the GUI. */
    private final Inventory inv;
    
    /** Player */
    private final Player player;
    
    /** Instance of SimpleClaimSystem */
    private final SimpleClaimSystem instance;

    
    // ******************
    // *  Constructors  *
    // ******************

    
    /**
     * Main constructor for the ClaimMainGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param claim  The claim for which the GUI is displayed.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public ClaimMainGui(Player player, Claim claim, SimpleClaimSystem instance) {
    	this.instance = instance;
    	this.player = player;
    	
    	// Get title
    	String title = instance.getLanguage().getMessage("gui-main-title")
    			.replace("%name%", claim.getName());

    	// Create inventory
        inv = Bukkit.createInventory(this, 54, title);
        
        // Load the items asynchronously
        loadItems(claim).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error")));
        	}
        })
        .exceptionally(ex -> {
            ex.printStackTrace();
            return null;
        });
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Initializes items for the GUI.
     *
     * @param claim  The claim for which the GUI is displayed.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Claim claim) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	    	// Get player data
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        
	        // Update player data (gui)
	        cPlayer.setClaim(claim);
	        
	        // Set items
	        String lore_string = instance.getLanguage().getMessage("claim-info-lore")
	            	.replace("%description%", claim.getDescription())
		    		.replace("%claim-name%", claim.getName())
		    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
						.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
						.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
		    		.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
					.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
					.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
	        String title = instance.getLanguage().getMessage("claim-info-title")
	            	.replace("%description%", claim.getDescription())
		    		.replace("%claim-name%", claim.getName())
		    		.replace("%sale-status%", claim.getSale() ? (instance.getLanguage().getMessage("claim-info-lore-sale-status-true")
						.replace("%price%", instance.getMain().getNumberSeparate(String.valueOf(claim.getPrice())))
						.replace("%money-symbol%", instance.getLanguage().getMessage("money-symbol"))) : instance.getLanguage().getMessage("claim-info-lore-sale-status-false"))
		    		.replace("%chunks-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getChunks().size())))
					.replace("%members-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getMembers().size())))
					.replace("%bans-count%", instance.getMain().getNumberSeparate(String.valueOf(claim.getBans().size())));
	        List<String> lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "claim-info") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(13, instance.getGuis().createItem(Material.PAINTING, title, lore));
	        
	        lore_string = instance.getLanguage().getMessage("manage-bans-lore");
	        title = instance.getLanguage().getMessage("manage-bans-title");
	        lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "manage-bans") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(20, instance.getGuis().createItem(Material.LECTERN, title, lore));
	        
	        lore_string = instance.getLanguage().getMessage("manage-settings-lore");
	        title = instance.getLanguage().getMessage("manage-settings-title");
	        lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "manage-settings") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(29, instance.getGuis().createItem(Material.REPEATER, title, lore));
	        
	        lore_string = instance.getLanguage().getMessage("manage-members-lore");
	        title = instance.getLanguage().getMessage("manage-members-title");
	        lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "manage-members") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(30, instance.getGuis().createItem(Material.TOTEM_OF_UNDYING, title, lore));
	        
	        lore_string = instance.getLanguage().getMessage("manage-chunks-lore");
	        title = instance.getLanguage().getMessage("manage-chunks-title");
	        lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "manage-chunks") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(32, instance.getGuis().createItem(Material.RED_MUSHROOM_BLOCK, title, lore));
	        
	        lore_string = instance.getLanguage().getMessage("teleport-claim-lore");
	        title = instance.getLanguage().getMessage("teleport-claim-title");
	        lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "teleport-claim") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(33, instance.getGuis().createItem(Material.ENDER_PEARL, title, lore));
	        
	        lore_string = instance.getLanguage().getMessage("unclaim-lore");
	        title = instance.getLanguage().getMessage("unclaim-title");
	        lore = new ArrayList<>(instance.getGuis().getLore(lore_string));
	        lore.add(!checkPermButton(player, "unclaim") ? instance.getLanguage().getMessage("gui-button-no-permission") : instance.getLanguage().getMessage("access-button"));
	        inv.setItem(24, instance.getGuis().createItem(Material.RED_CONCRETE, title, lore));
        
	        return true;
	        
    	});
    }

    /**
     * Checks if the player has the permission for the specified key.
     *
     * @param player The player to check.
     * @param key    The key to check permission for.
     * @return True if the player has the permission, otherwise false.
     */
    public boolean checkPermButton(Player player, String key) {
        switch (key) {
        	case "unclaim":
        		return instance.getPlayerMain().checkPermPlayer(player, "scs.command.unclaim");
            case "manage-members":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.members");
            case "manage-bans":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.bans");
            case "manage-settings":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.settings");
            case "manage-chunks":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.chunks");
            case "claim-info":
                return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.list");
            case "teleport-claim":
            	return instance.getPlayerMain().checkPermPlayer(player, "scs.command.claim.tp");
            default:
                return false;
        }
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

}
