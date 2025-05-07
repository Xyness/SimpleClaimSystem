package fr.xyness.SCS.Guis.AdminGestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;

/**
 * The (non-Admin) Claim Chunks/Zones Management GUI
 * (Uses Zones mode if player is in a Zone).
 *
 * L'interface graphique de gestion des blocs/zones de réclamation (non administrateur)
 * (Utilise le mode Zones si le joueur se trouve dans une zone).
 */
public class AdminGestionClaimChunksGui implements InventoryHolder {
    
	
    // ***************
    // *  Variables  *
    // ***************
    
	
	/** Inventory for the GUI. */
    private Inventory inv;
    
    /** Instance of SimpleClaimSystem */
    private SimpleClaimSystem instance;
    
    
    // ******************
    // *  Constructors  *
    // ******************
    
    
    /**
     * Main constructor for AdminGestionClaimChunksGui.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The current page of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionClaimChunksGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
        final Zone zone = claim.setZoneOfGUIByLocation(player);
        // TODO: translate these strings
        String title = (zone != null)
                ? "§4[A]§r Zones: "+claim.getName()+" ("+claim.getOwner()+")"
                : "§4[A]§r Chunks: "+claim.getName()+" ("+claim.getOwner()+")";
        inv = Bukkit.createInventory(this, 54, title);
        loadItems(player, claim, page, zone).thenAccept(success -> {
        	if (success) {
        		instance.executeEntitySync(player, () -> player.openInventory(inv));
        	} else {
        		instance.executeEntitySync(player, () -> player.sendMessage(instance.getLanguage().getMessage("error", zone)));
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
     * Initializes the items for the GUI.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The current page of the GUI.
     * @return A CompletableFuture with a boolean to check if the gui is correctly initialized.
     */
    public CompletableFuture<Boolean> loadItems(Player player, Claim claim, int page, Zone zone) {
    	
    	return CompletableFuture.supplyAsync(() -> {
    	
	        CPlayer cPlayer = instance.getPlayerMain().getCPlayer(player.getUniqueId());
	        cPlayer.setClaim(claim);
	        cPlayer.clearMapString();
	        cPlayer.setGuiPage(page);
            cPlayer.setGuiZone(zone);
	        int min_member_slot = 0;
	        int max_member_slot = 44;
	        int items_count = max_member_slot - min_member_slot + 1;
	        if(page>1) inv.setItem(48, backPage(page - 1));
	        inv.setItem(49, backMainMenu(claim.getName()));
	        List<String> lore = new ArrayList<>(Arrays.asList(
                    "§7The chunk is part of the claim",
                    claim.getChunks().size() == 1 ? "§cYou can't remove the only remaining chunk" : "§c[Left-click]§7 to remove chunk"));
            if (zone != null) {
                lore.set(1, "§c[Left-click]§7 to remove zone");
            }
	        int startItem = (page - 1) * items_count;
	        int i = min_member_slot;
	        int count = 0;
	        int chunk_count = 0;
	        for (Chunk chunk : claim.getChunks()) {
	        	chunk_count++;
	            if (count++ < startItem) continue;
	            if (i == max_member_slot + 1) {
	            	inv.setItem(50, nextPage(page + 1));
	                break;
	            }
	            cPlayer.addMapString(i, String.valueOf(chunk.getWorld().getName()+";"+chunk.getX()+";"+chunk.getZ()));
	            ItemStack item = new ItemStack(Material.RED_MUSHROOM_BLOCK);
	            ItemMeta meta = item.getItemMeta();
	            meta.setDisplayName("§6Chunk-"+String.valueOf(chunk_count)+" §7("+String.valueOf(chunk.getWorld().getName()+", X:"+chunk.getX()+", Z:"+chunk.getZ())+")");
                if (zone != null) {
                    meta.setDisplayName("["+zone.getName()+"] overrides "+zone+" in "+claim.getName());
                }
	            meta.setLore(lore);
	            item.setItemMeta(meta);
	            inv.setItem(i, item);
	            i++;
	            continue;
	        }
	        
	        return true;
        
    	});
	        
    }
    
    /**
     * Creates an item for the back main menu slot.
     *
     * @param claim_name The name of the current claim.
     * @return The created back page item.
     */
    private ItemStack backMainMenu(String claim_name) {
        ItemStack item = new ItemStack(Material.DARK_OAK_DOOR);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cMain menu");
            meta.setLore(instance.getGuis().getLore("§7Go back to the main menu of "+claim_name+"\n§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }

        return item;
    }
    
    /**
     * Create a back page item.
     * 
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack backPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cPrevious page");
            meta.setLore(Arrays.asList("§7Go to the page "+String.valueOf(page),"§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    /**
     * Create a next page item.
     * 
     * @param page The page number.
     * @return The created ItemStack.
     */
    private ItemStack nextPage(int page) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§cNext page");
            meta.setLore(Arrays.asList("§7Go to the page "+String.valueOf(page),"§7▸ §fClick to access"));
            meta = instance.getGuis().setItemFlag(meta);
            item.setItemMeta(meta);
        }
        return item;
    }
    
    @Override
    public Inventory getInventory() {
        return inv;
    }
    
    /**
     * Opens the inventory for the player.
     * 
     * @param player The player.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }

}
