package fr.xyness.SCS.Guis.AdminGestion;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import dev.lone.itemsadder.api.CustomStack;
import fr.xyness.SCS.CPlayer;
import fr.xyness.SCS.CPlayerMain;
import fr.xyness.SCS.Claim;
import fr.xyness.SCS.ClaimMain;
import fr.xyness.SCS.SimpleClaimSystem;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import me.clip.placeholderapi.PlaceholderAPI;
import net.md_5.bungee.api.ChatColor;

/**
 * Class representing the Admin Gestion GUI.
 */
public class AdminGestionGui implements InventoryHolder {

	
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
     * Main constructor for the AdminGestionGui.
     *
     * @param player The player for whom the GUI is being created.
     * @param instance The instance of the SimpleClaimSystem plugin.
     */
    public AdminGestionGui(Player player, SimpleClaimSystem instance) {
    	this.instance = instance;
        inv = Bukkit.createInventory(this, 54, "§4[A] §rPlugin settings");
        instance.executeAsync(() -> loadItems(player));
    }

    
    // ********************
    // *  Others Methods  *
    // ********************

    
    /**
     * Initializes items for the GUI.
     *
     * @param player The player for whom the GUI is being initialized.
     */
    public void loadItems(Player player) {
        String default_statut_disabled = instance.getLanguage().getMessage("status-disabled");
        String default_statut_enabled = instance.getLanguage().getMessage("status-enabled");
        String default_choix_disabled = "§7▸ §fLeft-click to §nenable";
        String default_choix_enabled = "§7▸ §fLeft-click to §ndisable";
        String default_setup = "§7▸ §fRight-click to §nsetup";

        FileConfiguration config = instance.getPlugin().getConfig();
        
        inv.setItem(49, instance.getGuis().createItem(Material.ARROW, "§cPrevious page", Arrays.asList("§7Go back to admin main menu"," ","§7▸§f Click to access")));
        
        // Lang
        String param = config.getString("lang");
        List<String> lore = new ArrayList<>();
        lore.add("§7The language file for messages.");
        lore.add(" ");
        File guisDir = new File(instance.getPlugin().getDataFolder(), "langs");
        if (!guisDir.exists()) {
            guisDir.mkdirs();
        }
        FilenameFilter ymlFilter = (dir, name) -> name.toLowerCase().endsWith(".yml");
        File[] files = guisDir.listFiles(ymlFilter);
        for(File f : files) {
        	if(f.getName().equalsIgnoreCase(param)) {
        		lore.add("§b➲ §f"+f.getName());
        	} else {
        		lore.add("§8➲ "+f.getName());
        	}
        }
        lore.add(" ");
        lore.add("§7▸ §fClick to change");
        inv.setItem(0, instance.getGuis().createItem(Material.PAPER, "§3Lang §7(§f"+param+"§7)", lore));
        
        // Database
        Boolean setting_boolean = config.getBoolean("database");
        String statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        String choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.clear();
        lore.add("§7Are the claims stored in a distant");
        lore.add("§7database or local database?");
        lore.add("");
        lore.add("§c⚠ Reload required");
        lore.add("§7§oWhen you change this setting, a reload is executed");
        lore.add("§7Make sure it's the good connection informations before enable it.");
        lore.add("");
    	lore.add("§7Hostname: §b"+replaceHalfWithAsterisks(config.getString("database-settings.hostname")));
    	lore.add("§7Port: §b"+replaceHalfWithAsterisks(config.getString("database-settings.port")));
    	lore.add("§7Database: §b"+replaceHalfWithAsterisks(config.getString("database-settings.database_name")));
    	lore.add("§7Username: §b"+replaceHalfWithAsterisks(config.getString("database-settings.username")));
    	lore.add("§7Password: §b"+replaceHalfWithAsterisks(config.getString("database-settings.password")));
    	lore.add("");
    	lore.add(choice);
    	lore.add(default_setup);
        inv.setItem(1, instance.getGuis().createItem(Material.BOOKSHELF, "§3Distant database §7("+statut+"§7)", lore));
        
        // Purge
        setting_boolean = config.getBoolean("auto-purge");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.clear();
        lore.add("§7Are the claims deleted when too old?");
        lore.add(" ");
        if(setting_boolean) {
        	lore.add("§7Frequency: §b"+convertMinutes(config.getInt("auto-purge-checking")));
        	lore.add("§7Time without login: §b"+config.getString("auto-purge-time-without-login"));
        	lore.add(" ");
        	lore.add(choice);
        	lore.add(default_setup);
        } else {
        	lore.add(choice);
        }
        inv.setItem(2, instance.getGuis().createItem(Material.BLAZE_POWDER, "§3Auto-purge §7("+statut+"§7)", lore));
        
        // Protection message
        param = config.getString("protection-message");
        lore.clear();
        lore.add("§7Where the protection message is displayed?");
        lore.add(" ");
        lore.add(param.equalsIgnoreCase("action_bar") ? "§b➲ §fACTION_BAR" : "§8➲ ACTION_BAR");
        lore.add(param.equalsIgnoreCase("bossbar") ? "§b➲ §fBOSSBAR" : "§8➲ BOSSBAR");
        lore.add(param.equalsIgnoreCase("title") ? "§b➲ §fTITLE" : "§8➲ TITLE");
        lore.add(param.equalsIgnoreCase("subtitle") ? "§b➲ §fSUBTITLE" : "§8➲ SUBTITLE");
        lore.add(param.equalsIgnoreCase("chat") ? "§b➲ §fCHAT" : "§8➲ CHAT");
        lore.add(" ");
        lore.add("§7▸ §fClick to change");
        inv.setItem(3, instance.getGuis().createItem(Material.REDSTONE_BLOCK, "§3Protection message §7(§f"+param+"§7)", lore));
        
        // Disabled worlds
        List<String> worlds = config.getStringList("worlds-disabled");
        lore.clear();
        lore.add("§7The worlds where claims are disabled.");
        lore.add(" ");
        worlds.forEach(w -> lore.add("§7⁃ §b"+w));
        lore.add(" ");
        lore.add(default_setup);
        inv.setItem(4, instance.getGuis().createItem(Material.GRASS_BLOCK, "§3Disabled worlds", lore));
        
        // Preload chunks
        setting_boolean = config.getBoolean("preload-chunks");
        lore.clear();
        lore.add("§7Preload chunks when starting plugin?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(5, instance.getGuis().createItem(Material.WARPED_NYLIUM, "§3Preload chunks §7("+statut+"§7)", lore));
        
        // Keep chunks loaded
        setting_boolean = config.getBoolean("keep-chunks-loaded");
        lore.clear();
        lore.add("§7Keep the chunks loaded even if no players in?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(6, instance.getGuis().createItem(Material.CRIMSON_NYLIUM, "§3Keep chunks loaded §7("+statut+"§7)", lore));
                
        // Max claim name length
        param = config.getString("max-length-claim-name");
        lore.clear();
        lore.add("§7The max length for claim name.");
        lore.add(" ");
        lore.add("§7▸§f Left-click to increase");
        lore.add("§7▸§f Right-click to decrease");
        inv.setItem(7, instance.getGuis().createItem(Material.PAPER, "§3Max claim name length §7(§b"+param+"§7)", lore));
        
        // Max claim description length
        param = config.getString("max-length-claim-description");
        lore.clear();
        lore.add("§7The max length for claim description.");
        lore.add(" ");
        lore.add("§7▸§f Left-click to increase");
        lore.add("§7▸§f Right-click to decrease");
        inv.setItem(8, instance.getGuis().createItem(Material.PAPER, "§3Max claim name length §7(§b"+param+"§7)", lore));
        
        // Claim confirmation
        setting_boolean = config.getBoolean("claim-confirmation");
        lore.clear();
        lore.add("§7Do players need to confirm before claiming?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(9, instance.getGuis().createItem(Material.STONE_BUTTON, "§3Claim confirmation §7("+statut+"§7)", lore));
        
        // Claim particle
        setting_boolean = config.getBoolean("claim-particles");
        lore.clear();
        lore.add("§7Should particles be displayed when claiming?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(10, instance.getGuis().createItem(Material.NETHER_STAR, "§3Claim particles §7("+statut+"§7)", lore));
        
        // Fly disabled on damage
        setting_boolean = config.getBoolean("claim-fly-disabled-on-damage");
        lore.clear();
        lore.add("§7Should the fly be disabled on damage?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(11, instance.getGuis().createItem(Material.FEATHER, "§3Fly disabled on damage §7("+statut+"§7)", lore));
        
        // Auto-fly message
        setting_boolean = config.getBoolean("claim-fly-message-auto-fly");
        lore.clear();
        lore.add("§7Does the auto-fly send a message each time");
        lore.add("§7the fly is activated/deactivated?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(12, instance.getGuis().createItem(Material.ELYTRA, "§3Auto-fly message §7("+statut+"§7)", lore));
        
        // Enter/leave message actionbar
        setting_boolean = config.getBoolean("enter-leave-messages");
        lore.clear();
        lore.add("§7Claim entry/exit message in actionbar.");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(13, instance.getGuis().createItem(Material.GLOWSTONE_DUST, "§3Enter/Leave message ActionBar §7("+statut+"§7)", lore));
        
        // Enter/leave message title/subtitle
        setting_boolean = config.getBoolean("enter-leave-title-messages");
        lore.clear();
        lore.add("§7Claim entry/exit message in title/subtitle.");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(14, instance.getGuis().createItem(Material.SUGAR, "§3Enter/Leave message Title §7("+statut+"§7)", lore));
        
        // Enter/leave message chat
        setting_boolean = config.getBoolean("enter-leave-chat-messages");
        lore.clear();
        lore.add("§7Claim entry/exit message in chat.");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(15, instance.getGuis().createItem(Material.GUNPOWDER, "§3Enter/Leave message Chat §7("+statut+"§7)", lore));
        
        // Visibility of claims with "Visitors" disable
        setting_boolean = config.getBoolean("claims-visitors-off-visible");
        lore.clear();
        lore.add("§7Should claims with the Visitors setting");
        lore.add("§7disabled be displayed in /claims?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(16, instance.getGuis().createItem(Material.DARK_OAK_DOOR, "§3Visibility 'Visitor' disabled §7("+statut+"§7)", lore));
        
        // Economy
        setting_boolean = config.getBoolean("economy");
        lore.clear();
        lore.add("§7Is the claim economy activated (sell/buy)?");
        lore.add("");
    	lore.add("§c⚠ Vault required");
    	lore.add("§7§oYou need this plugin to use economy");
    	lore.add("");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        if(setting_boolean) {
        	lore.add("§7Max sell price : §b" + config.getString("max-sell-price"));
        	lore.add("§7Claim cost : " + (config.getBoolean("claim-cost") ? default_statut_enabled : default_statut_disabled));
        	lore.add("§7Claim cost multiplier : " + (config.getBoolean("claim-cost-multiplier") ? default_statut_enabled : default_statut_disabled));
        	lore.add(" ");
        	lore.add(choice);
        	lore.add("§7▸ §fRight-click to change §nmax sell price");
        	lore.add("§7▸ §fShift-left-click to §n" + (config.getBoolean("claim-cost") ? "disable" : "enable"));
        	lore.add("§7▸ §fShift-right-click to §n" + (config.getBoolean("claim-cost-multiplier") ? "disable" : "enable"));
        } else {
        	lore.add(choice);
        }
        inv.setItem(17, instance.getGuis().createItem(Material.GOLD_INGOT, "§3Economy §7("+statut+"§7)", lore));
        
        // Bossbar
        setting_boolean = config.getBoolean("bossbar");
        lore.clear();
        lore.add("§7Is the claim bossbar activated?");
        lore.add("");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        if(setting_boolean) {
        	String color = config.getString("bossbar-settings.color").toUpperCase();
        	lore.add("§7Color :");
            for (BarColor colors : BarColor.values()) {
                String colorName = colors.name().toUpperCase();
                lore.add((colorName.equalsIgnoreCase(color) ? "§b➲ " + getRightColor(colorName) : "§8➲ ") + colorName);
            }
            String style = config.getString("bossbar-settings.style").toUpperCase();
            lore.add(" ");
        	lore.add("§7Style : §b");
            for (BarStyle styles : BarStyle.values()) {
                String styleName = styles.name().toUpperCase();
                lore.add((styleName.equalsIgnoreCase(style) ? "§b➲ §f" : "§8➲ ") + styleName);
            }
        	lore.add(" ");
        	lore.add(choice);
        	lore.add("§7▸ §fShift-left-click to §nswitch color");
        	lore.add("§7▸ §fShift-right-click to §nswitch style");
        } else {
        	lore.add(choice);
        }
        inv.setItem(18, instance.getGuis().createItem(Material.DRAGON_HEAD, "§3BossBar §7("+statut+"§7)", lore));
        
        // Teleportation delay moving
        setting_boolean = config.getBoolean("teleportation-delay-moving");
        lore.clear();
        lore.add("§7Does the teleport cancel when the player moves?");
        lore.add(" ");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        lore.add(choice);
        inv.setItem(19, instance.getGuis().createItem(Material.ENDER_PEARL, "§3Teleportation delay moving §7("+statut+"§7)", lore));
        
        // Dynmap
        setting_boolean = config.getBoolean("dynmap");
        lore.clear();
        lore.add("§7Is the claims Dynmap activated?");
        lore.add("");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        if(setting_boolean) {
        	lore.add("§7Claim border color : " + fromHex(config.getString("dynmap-settings.claim-border-color")));
        	lore.add("§7Claim fill color : " + fromHex(config.getString("dynmap-settings.claim-fill-color")));
        	lore.add("§7Claim hover text : §b" + config.getString("dynmap-settings.claim-hover-text"));
        	lore.add(" ");
        	lore.add(choice);
        	lore.add(default_setup);
        } else {
        	lore.add(choice);
        }
        inv.setItem(20, instance.getGuis().createItem(Material.MAP, "§3Dynmap §7("+statut+"§7)", lore));
        
        // Bluemap
        setting_boolean = config.getBoolean("bluemap");
        lore.clear();
        lore.add("§7Is the claims Bluemap activated?");
        lore.add("");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        if(setting_boolean) {
        	lore.add("§7Claim border color : " + fromHex(config.getString("bluemap-settings.claim-border-color")));
        	lore.add("§7Claim fill color : " + fromHex(config.getString("bluemap-settings.claim-fill-color")));
        	lore.add("§7Claim hover text : §b" + config.getString("bluemap-settings.claim-hover-text"));
        	lore.add(" ");
        	lore.add(choice);
        	lore.add(default_setup);
        } else {
        	lore.add(choice);
        }
        inv.setItem(21, instance.getGuis().createItem(Material.MAP, "§3Bluemap §7("+statut+"§7)", lore));
        
        // Pl3xMap
        setting_boolean = config.getBoolean("pl3xmap");
        lore.clear();
        lore.add("§7Is the claims Pl3xmap activated?");
        lore.add("");
        statut = setting_boolean ? default_statut_enabled : default_statut_disabled;
        choice = setting_boolean ? default_choix_enabled : default_choix_disabled;
        if(setting_boolean) {
        	lore.add("§7Claim border color : " + fromHex(config.getString("pl3xmap-settings.claim-border-color")));
        	lore.add("§7Claim fill color : " + fromHex(config.getString("pl3xmap-settings.claim-fill-color")));
        	lore.add("§7Claim hover text : §b" + config.getString("pl3xmap-settings.claim-hover-text"));
        	lore.add(" ");
        	lore.add(choice);
        	lore.add(default_setup);
        } else {
        	lore.add(choice);
        }
        inv.setItem(22, instance.getGuis().createItem(Material.MAP, "§3Pl3xmap §7("+statut+"§7)", lore));
        
        // Groups
        lore.clear();
        lore.add("§7Group system for permissions.");
        lore.add(" ");
        lore.add("§c⚠ Reload required");
        lore.add("§7§oWhen you change this setting, a reload is executed");
        lore.add(" ");
        ConfigurationSection section = config.getConfigurationSection("groups");
        String path = "groups.";
        int i = 0;
        for(String key : section.getKeys(false)) {
        	String perm = config.getString(path+key+".permission");
        	String max_claims = config.getString(path+key+".max-claims");
        	String max_radius_claims = config.getString(path+key+".max-radius-claims");
        	String teleportation_delay = config.getString(path+key+".teleportation-delay");
        	String max_members = config.getString(path+key+".max-members");
        	String claim_cost = config.getString(path+key+".claim-cost");
        	String claim_cost_multiplier = config.getString(path+key+".claim-cost-multiplier");
        	String max_chunks_per_claim = config.getString(path+key+".max-chunks-per-claim");
        	lore.add("§7➣ §a"+key+"§7 ("+(perm == null ? "None" : perm)+"§7)");
        	lore.add(" §7⁃ max-claims : §b"+(max_claims.equals("0") ? "∞" : max_claims));
        	lore.add(" §7⁃ max-radius-claims : §b"+(max_radius_claims.equals("0") ? "∞" : max_radius_claims));
        	lore.add(" §7⁃ teleportation-delay : §b"+(teleportation_delay.equals("0") ? "∞" : teleportation_delay));
        	lore.add(" §7⁃ max-members : §b"+(max_members.equals("0") ? "∞" : max_members));
        	lore.add(" §7⁃ claim-cost : §b"+(claim_cost.equals("0") ? "∞" : claim_cost));
        	lore.add(" §7⁃ claim-cost-multiplier : §b"+(claim_cost_multiplier.equals("0") ? "∞" : claim_cost_multiplier));
        	lore.add(" §7⁃ max-chunks-per-claim : §b"+(max_chunks_per_claim.equals("0") ? "∞" : max_chunks_per_claim));
        	lore.add(" ");
        	i++;
        }
        lore.add("§7▸§f Left-click to remove a group");
        lore.add("§7▸§f Right-click to add/modify a group");
        inv.setItem(23, instance.getGuis().createItem(Material.CHEST, "§3Groups §7("+String.valueOf(i)+"§7)", lore));
        
        // Players
        lore.clear();
        lore.add("§7Player system for permissions.");
        lore.add(" ");
        lore.add("§c⚠ Reload required");
        lore.add("§7§oWhen you change this setting, a reload is executed");
        lore.add(" ");
        section = config.getConfigurationSection("players");
        path = "players.";
        i = 0;
        for(String key : section.getKeys(false)) {
        	String max_claims = config.getString(path+key+".max-claims");
        	String max_radius_claims = config.getString(path+key+".max-radius-claims");
        	String teleportation_delay = config.getString(path+key+".teleportation-delay");
        	String max_members = config.getString(path+key+".max-members");
        	String claim_cost = config.getString(path+key+".claim-cost");
        	String claim_cost_multiplier = config.getString(path+key+".claim-cost-multiplier");
        	String max_chunks_per_claim = config.getString(path+key+".max-chunks-per-claim");
        	lore.add("§7➣ §a"+key);
        	lore.add(" §7⁃ max-claims : §b"+(max_claims.equals("0") ? "∞" : max_claims));
        	lore.add(" §7⁃ max-radius-claims : §b"+(max_radius_claims.equals("0") ? "∞" : max_radius_claims));
        	lore.add(" §7⁃ teleportation-delay : §b"+(teleportation_delay.equals("0") ? "∞" : teleportation_delay));
        	lore.add(" §7⁃ max-members : §b"+(max_members.equals("0") ? "∞" : max_members));
        	lore.add(" §7⁃ claim-cost : §b"+(claim_cost.equals("0") ? "∞" : claim_cost));
        	lore.add(" §7⁃ claim-cost-multiplier : §b"+(claim_cost_multiplier.equals("0") ? "∞" : claim_cost_multiplier));
        	lore.add(" §7⁃ max-chunks-per-claim : §b"+(max_chunks_per_claim.equals("0") ? "∞" : max_chunks_per_claim));
        	lore.add(" ");
        	i++;
        }
        lore.add("§7▸§f Left-click to remove a player");
        lore.add("§7▸§f Right-click to add/modify a player");
        inv.setItem(24, instance.getGuis().createItem(Material.PLAYER_HEAD, "§3Players §7("+String.valueOf(i)+"§7)", lore));
        
        // Status setting
        lore.clear();
        lore.add("§7Can this setting be modified by players?");
        lore.add(" ");
        Map<String,Boolean> perms = new HashMap<>(instance.getSettings().getStatusSettings());
        for(String perm : perms.keySet()) {
        	setting_boolean = perms.get(perm);
        	lore.add("§7"+perm+": "+ (setting_boolean ? default_statut_enabled : default_statut_disabled));
        }
        lore.add(" ");
        lore.add(default_setup);
        inv.setItem(25, instance.getGuis().createItem(Material.REPEATER, "§3Status settings", lore));
        
        // Default values setting
        lore.clear();
        lore.add("§7The default values that the settings take when claiming.");
        lore.add(" ");
        perms = new HashMap<>(instance.getSettings().getDefaultValues());
        for(String perm : perms.keySet()) {
        	setting_boolean = perms.get(perm);
        	lore.add("§7"+perm+": "+ (setting_boolean ? default_statut_enabled : default_statut_disabled));
        }
        lore.add(" ");
        lore.add(default_setup);
        inv.setItem(26, instance.getGuis().createItem(Material.COMPARATOR, "§3Default values", lore));
        
        // Blocked interact blocks
        lore.clear();
        lore.add("§7Blocks that cannot be interacted with.");
        lore.add(" ");
        lore.add("§7▸ §fUse §a/scs add-blocked-interact-block <material>§f to add");
        lore.add("§7▸ §fUse §a/scs remove-blocked-interact-block <material>§f to remove");
        inv.setItem(27, instance.getGuis().createItem(Material.FURNACE, "§3Blocked interact blocks §7("+String.valueOf(instance.getSettings().getRestrictedContainers().size())+"§7)", lore));
        
        // Blocked items
        lore.clear();
        lore.add("§7Items that cannot be used.");
        lore.add(" ");
        lore.add("§7▸ §fUse §a/scs add-blocked-item <material>§f to add");
        lore.add("§7▸ §fUse §a/scs remove-blocked-item <material>§f to remove");
        inv.setItem(28, instance.getGuis().createItem(Material.FISHING_ROD, "§3Blocked items §7("+String.valueOf(instance.getSettings().getRestrictedItems().size())+"§7)", lore));
        
        // Blocked entities
        lore.clear();
        lore.add("§7Entities that cannot be interacted with.");
        lore.add(" ");
        lore.add("§7▸ §fUse §a/scs add-blocked-entity <entity_type>§f to add");
        lore.add("§7▸ §fUse §a/scs remove-blocked-entity <entity_type>§f to remove");
        inv.setItem(29, instance.getGuis().createItem(Material.OAK_BOAT, "§3Blocked entities §7("+String.valueOf(instance.getSettings().getRestrictedEntityType().size())+"§7)", lore));
        
        instance.executeEntitySync(player, () -> player.openInventory(inv));
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    /**
     * Opens the inventory for the player.
     *
     * @param player The player for whom the inventory is opened.
     */
    public void openInventory(Player player) {
        player.openInventory(inv);
    }
    
    /**
     * Converts a hex color string (RRGGBB) to a Minecraft ChatColor.
     * 
     * @param hexColor The hex color string (e.g., "FF5733").
     * @return The corresponding ChatColor with the string.
     */
    public static String fromHex(String hexColor) {
        if (hexColor == null || !hexColor.matches("^#?[0-9A-Fa-f]{6}$")) {
            return hexColor;
        }

        // Remove the '#' if present
        if (hexColor.startsWith("#")) {
            hexColor = hexColor.substring(1);
        }

        // Parse the hex string to an integer
        int rgb = Integer.parseInt(hexColor, 16);

        // Convert to ChatColor
        return ChatColor.of(new java.awt.Color(rgb))+hexColor;
    }
    
    /**
     * Gets the right color from String
     * @param input The color string
     * @return The right color string
     */
    public String getRightColor(String input) {
    	switch(input) {
    		case "YELLOW":
    			return "§e";
    		case "BLUE":
    			return "§b";
    		case "GREEN":
    			return "§a";
    		case "PINK":
    			return "§d";
    		case "PURPLE":
    			return "§5";
    		case "RED":
    			return "§c";
    		case "WHITE":
    			return "§f";
    	}
    	return input;
    }
    
    /**
     * Replaces the half string with asterisks
     * @param input The string
     * @return The string with asterisks
     */
    public String replaceHalfWithAsterisks(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        int length = input.length();
        int halfIndex = length / 2 - (length / 4);
        char[] result = input.toCharArray();

        for (int i = halfIndex; i < length; i++) {
            result[i] = '*';
        }

        return new String(result);
    }
    
    /**
     * Converts minutes in hours/days/weeks/months/years
     * @param minutes The minutes
     * @return The string of converted time
     */
    public String convertMinutes(long minutes) {
        final long MINUTES_IN_HOUR = 60;
        final long MINUTES_IN_DAY = 1440; // 60 * 24
        final long MINUTES_IN_WEEK = 10080; // 60 * 24 * 7
        final long MINUTES_IN_MONTH = 43200; // 60 * 24 * 30
        final long MINUTES_IN_YEAR = 525600; // 60 * 24 * 365

        long years = minutes / MINUTES_IN_YEAR;
        minutes %= MINUTES_IN_YEAR;

        long months = minutes / MINUTES_IN_MONTH;
        minutes %= MINUTES_IN_MONTH;

        long weeks = minutes / MINUTES_IN_WEEK;
        minutes %= MINUTES_IN_WEEK;

        long days = minutes / MINUTES_IN_DAY;
        minutes %= MINUTES_IN_DAY;

        long hours = minutes / MINUTES_IN_HOUR;
        minutes %= MINUTES_IN_HOUR;

        StringBuilder result = new StringBuilder();
        if (years > 0) {
            result.append(years).append(" year").append(years > 1 ? "s" : "").append(", ");
        }
        if (months > 0) {
            result.append(months).append(" month").append(months > 1 ? "s" : "").append(", ");
        }
        if (weeks > 0) {
            result.append(weeks).append(" week").append(weeks > 1 ? "s" : "").append(", ");
        }
        if (days > 0) {
            result.append(days).append(" day").append(days > 1 ? "s" : "").append(", ");
        }
        if (hours > 0) {
            result.append(hours).append(" hour").append(hours > 1 ? "s" : "").append(", ");
        }
        if (minutes > 0) {
            result.append(minutes).append(" minute").append(minutes > 1 ? "s" : "");
        } else {
            if (result.length() > 0) {
                result.setLength(result.length() - 2);
            }
        }

        return result.toString();
    }
}
