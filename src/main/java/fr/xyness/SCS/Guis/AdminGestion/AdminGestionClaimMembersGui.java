package fr.xyness.SCS.Guis.AdminGestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import fr.xyness.SCS.Zone;
import org.bukkit.Bukkit;
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
 * The Admin Claim/Zone Members Management GUI.
 *
 * L'interface graphique de gestion des membres des réclamations administratives.
 */
public class AdminGestionClaimMembersGui implements InventoryHolder {
    
	
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
     * Main constructor for AdminGestionClaimMembersGui.
     * 
     * @param player The player who opened the GUI.
     * @param claim  The claim for which the GUI is displayed.
     * @param page   The current page of the GUI.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionClaimMembersGui(Player player, Claim claim, int page, SimpleClaimSystem instance) {
    	this.instance = instance;
        final Zone zone = claim.setZoneOfGUIByLocation(player);
        // TODO: Translate these strings
        String title = (zone != null)
                ? String.format("§4[A]§r Members: [%s] in %s (%s)", zone.getName(), claim.getName(), claim.getOwner())
                : String.format("§4[A]§r Members: %s (%s)", claim.getName(), claim.getOwner());
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
	        String owner = claim.getOwner();
	        if(page>1) inv.setItem(48, backPage(page - 1));
	        inv.setItem(49, backMainMenu(claim.getName()));
            // TODO: translate these phrases
            String hasWhat = (zone != null)
                    ? "Has access to this zone of claim"
                    : "Has access to this claim";
	        List<String> lore = new ArrayList<>(Arrays.asList("§7"+hasWhat," ","§c[Left-click]§7 to remove member"));
	        int startItem = (page - 1) * items_count;
	        int i = min_member_slot;
	        int count = 0;
	        Set<String> members = instance.getMain().convertUUIDSetToStringSet(claim.getMembers());
	        List<String> membersList = new ArrayList<>(members);
	        Collections.sort(membersList, (member1, member2) -> member1.compareTo(member2));
	        members = new LinkedHashSet<>(membersList);
	        for (String p : members) {
	            if (count++ < startItem) continue;
	            if (i == max_member_slot + 1) {
	            	inv.setItem(50, nextPage(page + 1));
	                break;
	            }
	            cPlayer.addMapString(i, p);
	            ItemStack item = instance.getPlayerMain().getPlayerHead(p);
	            SkullMeta meta = (SkullMeta) item.getItemMeta();
	            meta.setDisplayName("§e"+p);
	            if (owner.equals(p)) {
	                meta.setLore(Arrays.asList("§dOwner of the claim"));
	            } else {
	                meta.setLore(lore);
	            }
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
