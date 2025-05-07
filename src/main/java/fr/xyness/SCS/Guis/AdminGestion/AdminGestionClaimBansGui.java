package fr.xyness.SCS.Guis.AdminGestion;

import java.util.*;
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

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Types.CPlayer;
import fr.xyness.SCS.Types.Claim;
import me.clip.placeholderapi.PlaceholderAPI;

/**
 * The Admin Claim/Zone Bans Management GUI.
 *
 * La réclamation de gestion administrative interdit l'interface graphique.
 */
public class AdminGestionClaimBansGui implements InventoryHolder {
    
	
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
     * Main constructor for AdminGestionClaimBansGui.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The current page of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionClaimBansGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
        final Zone zone = claim.setZoneOfGUIByLocation(player);
        // TODO: Translate these strings
        String title = (zone != null)
                ? String.format("§4[A]§r Bans: [%s] in %s (%s)", zone.getName(), claim.getName(), claim.getOwner())
                : String.format("§4[A]§r Bans: %s (%s)", claim.getName(), claim.getOwner());
        inv = Bukkit.createInventory(this, 54, title);
        loadItems(player, claim, page, zone).thenAccept(success -> {
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
            String scope = (zone != null) ? "zone" : "claim";
	        int min_member_slot = 0;
	        int max_member_slot = 44;
	        int items_count = max_member_slot - min_member_slot + 1;
	        if(page>1) inv.setItem(48, backPage(page - 1));
	        inv.setItem(49, backMainMenu(claim.getName()));
            // TODO: translate phrases below, and scope above
	        List<String> lore = new ArrayList<>(Arrays.asList(
                    String.format("§7Is banned from this %s", scope),
                    " ", "§c[Left-click]§7 to unban this player"));
	        int startItem = (page - 1) * items_count;
	        int i = min_member_slot;
	        int count = 0;
            Set<UUID> banUUIDs = (zone != null) ? zone.getBans() : claim.getBans();
	        Set<String> bans = instance.getMain().convertUUIDSetToStringSet(banUUIDs);
	        List<String> bansList = new ArrayList<>(bans);
	        Collections.sort(bansList, (ban1, ban2) -> ban1.compareTo(ban2));
	        bans = new LinkedHashSet<>(bansList);
	        for (String p : bans) {
	            if (count++ < startItem) continue;
	            if (i == max_member_slot + 1) {
	            	inv.setItem(50, nextPage(page + 1));
	                break;
	            }
	            cPlayer.addMapString(i, p);
	            ItemStack item = instance.getPlayerMain().getPlayerHead(p);
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName("§e"+p);
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
