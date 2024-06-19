package fr.xyness.SCS;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import de.bluecolored.bluemap.api.BlueMapAPI;

import fr.xyness.SCS.Commands.*;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Listeners.*;
import fr.xyness.SCS.Others.ClaimPurge;
import fr.xyness.SCS.Support.*;

public class SimpleClaimSystem extends JavaPlugin {
	
	
	// ***************
	// *  Variables  *
	// ***************
	
	
	public static JavaPlugin plugin;
	public static String Version = "1.9.0.1b9";
	public static HikariDataSource dataSource;
	private static boolean isFolia = false;
	private static boolean isUpdateAvailable;
	private static String updateMessage;
	
    public static HikariDataSource getDataSource() { return dataSource; } // Return data source for database
    public static boolean isFolia() { return isFolia; } // Return if the server use Folia
    public static String getUpdateMessage() { return updateMessage; } // Return the update message
    public static boolean isUpdateAvailable() { return isUpdateAvailable; } // Return if there is an update for the plugin
    
    
	// ******************
	// *  Main Methods  *
	// ******************
    
    
    // Enable plugin
	@Override
    public void onEnable() {
		getLogger().info("============================================================");
		plugin = this;
        if(loadConfig(this,false)) {
        	getLogger().info(" ");
        	getLogger().info("SimpleClaimSystem is enabled !");
        	getLogger().info("Discord for support : https://discord.gg/xyness");
        	getLogger().info("Documentation : https://xyness.gitbook.io/simpleclaimsystem");
        	getLogger().info("Developped by Xyness");
        } else {
        	Bukkit.getServer().getPluginManager().disablePlugin(this);
        }
        getLogger().info("============================================================");
    }
    
	// Disable plugin
    @Override
    public void onDisable() {
        if (dataSource != null) {
            dataSource.close();
        }
        getLogger().info("============================================================");
        getLogger().info("SimpleClaimSystem is disabled !");
    	getLogger().info("Discord for support : https://discord.gg/xyness");
    	getLogger().info("Documentation : https://xyness.gitbook.io/simpleclaimsystem");
    	getLogger().info("Developped by Xyness");
        getLogger().info("============================================================");
    }
	
	// Loading plugin (only for WorldGuard support)
	@Override
	public void onLoad() {
		if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
			ClaimWorldGuard.registerCustomFlag();
		}
	}
	
	
	// ********************
	// *  Others Methods  *
	// ********************
	
	
	// Execute task in async thread
	public static void executeAsync(Runnable gTask) {
		if (SimpleClaimSystem.isFolia()) {
			Bukkit.getAsyncScheduler().runNow(plugin, task -> gTask.run());
		} else {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, gTask);
		}
	}
	
	// Execute task in sync thread
	public static void executeSync(Runnable gTask) {
		if (SimpleClaimSystem.isFolia()) {
			Bukkit.getGlobalRegionScheduler().execute(plugin, () -> gTask.run());
		} else {
			Bukkit.getScheduler().runTask(plugin, gTask);
		}
	}
	
	// Set the folia check
	public static void checkFolia() {
		if(Bukkit.getVersion().contains("folia")) {
			isFolia = true;
			return;
		}
		try {
			Class.forName("io.papermc.paper.threadedregions.scheduler.AsyncScheduler");
			isFolia = true;
			return;
		} catch (ClassNotFoundException e) {
		    isFolia = false;
		    return;
		}
	}
	
    // Function to load/reload config
    public static boolean loadConfig(JavaPlugin plugin, boolean reload) {
    	if(reload) plugin.getLogger().info("============================================================");
    	plugin.saveDefaultConfig();
    	plugin.reloadConfig();
        updateConfigWithDefaults(plugin);
        checkFolia();
        
        // Unregister of all
        HandlerList.unregisterAll(plugin);
        ClaimMain.clearAll();
        ClaimGuis.clearAll();
        ClaimSettings.clearAll();
        
        // Checking for update
        isUpdateAvailable = checkForUpdates(plugin);
        
        // Register bStats
        ClaimbStats.enableMetrics(plugin);
        
        // Checking "guis" folder
        File dossier = new File(plugin.getDataFolder(), "guis");
        if (!dossier.exists()) {
        	dossier.mkdirs();
        }
        
    	// Checking PlaceholderAPI
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
        	ClaimSettings.addSetting("placeholderapi", "true");
            new ClaimPlaceholdersExpansion().register();
        } else {
        	ClaimSettings.addSetting("placeholderapi", "false");
        }
        
    	// Checking ItemsAdder
        boolean check_itemsadder = false;
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
        	check_itemsadder = true;
        }
        
    	// Checking WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
        	ClaimSettings.addSetting("worldguard", "true");
        } else {
        	ClaimSettings.addSetting("worldguard", "false");
        }
        
    	// Checking Vault
        boolean check_vault = false;
        if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
        	if(ClaimVault.setupEconomy()) {
            	ClaimSettings.addSetting("vault", "true");
            	check_vault = true;
        	} else {
        		ClaimSettings.addSetting("vault", "false");
        	}

        } else {
        	ClaimSettings.addSetting("vault", "false");
        }
        
    	// Checking Dynmap
        Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
        if (dynmap != null) {
        	ClaimSettings.addSetting("dynmap", "true");
        	if(!reload) {
            	DynmapAPI dynmapAPI = (DynmapAPI) dynmap;
            	MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
                if (markerAPI != null) {
                	MarkerSet markerSet = markerAPI.createMarkerSet("SimpleClaimSystem", "Claims", null, false);
                	new ClaimDynmap(dynmapAPI,markerAPI,markerSet);
                }
        	}
        } else {
        	ClaimSettings.addSetting("dynmap", "false");
        }
        
        // Checking Bluemap
        Plugin bluemap = Bukkit.getPluginManager().getPlugin("bluemap");
        if(bluemap != null) {
        	ClaimSettings.addSetting("bluemap", "true");
	        BlueMapAPI.onEnable(api -> {
	            // Register marker set
	        	new ClaimBluemap(api,plugin);
	        });
        } else {
        	ClaimSettings.addSetting("dynmap", "false");
        }
        
        // Checking guis files
        File check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "settings.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/settings.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "members.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/members.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "list.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/list.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "claims.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/claims.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "admin_settings.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/admin_settings.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "admin_list.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/admin_list.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "claims_owner.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/claims_owner.yml", false);
        }
        check_gui = new File(plugin.getDataFolder()+File.separator+"guis", "bans.yml");
        if (!check_gui.exists()) {
        	check_gui.getParentFile().mkdirs();
        	plugin.saveResource("guis/bans.yml", false);
        }
        ClaimGuis.loadGuiSettings(plugin,check_itemsadder);

        // Checking "langs" folder
        dossier = new File(plugin.getDataFolder(), "langs");
        if (!dossier.exists()) {
        	dossier.mkdirs();
        }
        
        // Checking default language file for some adds
        File en_US = new File(plugin.getDataFolder()+File.separator+"langs", "en_US.yml");
        if (!en_US.exists()) {
        	en_US.getParentFile().mkdirs();
        	plugin.saveResource("langs/en_US.yml", false);
        } else {
        	updateLangFileWithMissingKeys(plugin,"en_US.yml");
        }
        
        // Checking custom language file
        String lang = plugin.getConfig().getString("lang");
        File custom = new File(plugin.getDataFolder()+File.separator+"langs", lang);
        if (!custom.exists()) {
        	plugin.getLogger().info("File '"+lang+"' not found, using en_US.yml");
        	lang = "en_US.yml";
        } else {
        	updateLangFileWithMissingKeys(plugin,lang);
        }
        ClaimSettings.addSetting("lang", lang);
        
        // Loading selected language file
        File lang_final = new File(plugin.getDataFolder()+File.separator+"langs", lang);
        FileConfiguration config = YamlConfiguration.loadConfiguration(lang_final);
        Map<String,String> messages = new HashMap<>();
        for (String key : config.getKeys(false)) {
        	if(key.equals("help-command")) {
        		ConfigurationSection configHelp = config.getConfigurationSection("help-command");
        		for (String help : configHelp.getKeys(false)) {
        			messages.put("help-command."+help, configHelp.getString(help));
        		}
        		continue;
        	}
            String value = config.getString(key);
            messages.put(key, value);
        }
        ClaimLanguage.setLanguage(messages);
        
        // Checking database
        String configC = plugin.getConfig().getString("database");
        if(configC.equalsIgnoreCase("true")) {
            // Création d'une source de données
            HikariConfig configH = new HikariConfig();
            configH.setJdbcUrl("jdbc:mysql://"+plugin.getConfig().getString("database-settings.hostname")+":"+plugin.getConfig().getString("database-settings.port")+"/"+plugin.getConfig().getString("database-settings.database_name"));
            configH.setUsername(plugin.getConfig().getString("database-settings.username"));
            configH.setPassword(plugin.getConfig().getString("database-settings.password"));
            configH.addDataSourceProperty("cachePrepStmts", "true");
            configH.addDataSourceProperty("prepStmtCacheSize", "250");
            configH.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            configH.setPoolName("MySQL");
            configH.setMaximumPoolSize(10);
            configH.setMinimumIdle(2);
            configH.setIdleTimeout(60000);
            configH.setMaxLifetime(600000);
            dataSource = new HikariDataSource(configH);
        	try (Connection connection = dataSource.getConnection()) {
        		plugin.getLogger().info("✓ Database connection successful");
                try (Statement stmt = connection.createStatement()) {
                    String sql = "CREATE TABLE IF NOT EXISTS scs_claims " +
                            "(id_pk INT AUTO_INCREMENT PRIMARY KEY, " +
                            "id INT, " +
                            "uuid VARCHAR(36), " +
                            "name VARCHAR(36), " +
                            "claim_name VARCHAR(255), " +
                            "claim_description VARCHAR(255), " +
                            "X INT, " +
                            "Z INT, " +
                            "World VARCHAR(255), " +
                            "Location VARCHAR(255), " +
                            "Members VARCHAR(1020), " +
                            "Permissions VARCHAR(510), " +
                            "isSale TINYINT(1) DEFAULT 0, " +
                            "SalePrice DOUBLE DEFAULT 0, " +
                            "Bans VARCHAR(1020) DEFAULT '')";
                    stmt.executeUpdate(sql);
                    sql = "ALTER TABLE scs_claims\r\n"
                    		+ "MODIFY COLUMN SalePrice DOUBLE;";
                    stmt.executeUpdate(sql);
                    String checkColumnSQL = String.format(
                            "SELECT COUNT(*) AS column_count FROM information_schema.columns " +
                            "WHERE table_name = '%s' AND column_name = '%s'",
                            "scs_claims", "Bans");
                        ResultSet rs = stmt.executeQuery(checkColumnSQL);
                        if (rs.next() && rs.getInt("column_count") == 0) {
                            sql = "ALTER TABLE scs_claims\r\n"
                            		+ "ADD COLUMN Bans VARCHAR(1020) DEFAULT '';";
                            stmt.executeUpdate(sql);
                        }
                } catch (SQLException e) {
                	plugin.getLogger().info("Error creating tables, using local db.");
            		e.printStackTrace();
            		configC = "false";
    			}
        	} catch (SQLException e) {
        		plugin.getLogger().info("Error connecting to database, check the connection informations, using local db.");
        		configC = "false";
			}
        }
        if(configC.equals("false")) {
        	HikariConfig configH = new HikariConfig();
            configH.setJdbcUrl("jdbc:sqlite:plugins/SimpleClaimSystem/claims.db");
            configH.addDataSourceProperty("cachePrepStmts", "true");
            configH.addDataSourceProperty("prepStmtCacheSize", "250");
            configH.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            configH.setPoolName("SQLitePool");
            configH.setMaximumPoolSize(10);
            configH.setMinimumIdle(2);
            configH.setIdleTimeout(60000);
            configH.setMaxLifetime(600000);
            dataSource = new HikariDataSource(configH);
        	try (Connection connection = dataSource.getConnection()) {
        		try (Statement stmt = connection.createStatement()) {
                    String sql = "CREATE TABLE IF NOT EXISTS scs_claims " +
                            "(id_pk INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "id INT, " +
                            "uuid VARCHAR(36), " +
                            "name VARCHAR(36), " +
                            "claim_name VARCHAR(255), " +
                            "claim_description VARCHAR(255), " +
                            "X INT, " +
                            "Z INT, " +
                            "World VARCHAR(255), " +
                            "Location VARCHAR(255), " +
                            "Members VARCHAR(1020), " +
                            "Permissions VARCHAR(510), " +
                            "isSale TINYINT(1) DEFAULT 0, " +
                            "SalePrice DOUBLE DEFAULT 0, " +
                            "Bans VARCHAR(1020) DEFAULT '')";
                    stmt.executeUpdate(sql);
        		} catch (SQLException e) {
        			plugin.getLogger().info("Error creating tables, disabling plugin.");
        			return false;
    			}
        	} catch (SQLException e) {
        		plugin.getLogger().info("Error creating tables, disabling plugin.");
        		return false;
    		}
        }
        ClaimSettings.addSetting("database", configC);
        
        // Auto-purge settings
        configC = plugin.getConfig().getString("auto-purge");
        ClaimSettings.addSetting("auto-purge", configC);
        if(configC.equals("true")) {
            configC = plugin.getConfig().getString("auto-purge-checking");
            ClaimSettings.addSetting("auto-purge-checking", configC);
    		try {
    			int minutes = Integer.parseInt(configC);
    			if(minutes < 1) {
    				plugin.getLogger().info("'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
    				minutes = 60;
    			}
                configC = plugin.getConfig().getString("auto-purge-time-without-login");
                ClaimSettings.addSetting("auto-purge-time-without-login", configC);
                ClaimPurge.startPurge(minutes, configC);
    		} catch(NumberFormatException e){
    			plugin.getLogger().info("'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
    			int minutes = 60;
                configC = plugin.getConfig().getString("auto-purge-time-without-login");
                ClaimSettings.addSetting("auto-purge-time-without-login", configC);
                ClaimPurge.startPurge(minutes, configC);
    		}
        }
        
        // Add Dynmap settings
        configC = plugin.getConfig().getString("dynmap-claim-border-color");
        ClaimSettings.addSetting("dynmap-claim-border-color", configC);
        configC = plugin.getConfig().getString("dynmap-claim-fill-color");
        ClaimSettings.addSetting("dynmap-claim-fill-color", configC);
        configC = plugin.getConfig().getString("dynmap-hover-text");
        ClaimSettings.addSetting("dynmap-hover-text", configC);
        
        // Add Bluemap settings
        configC = plugin.getConfig().getString("bluemap-claim-border-color");
        ClaimSettings.addSetting("bluemap-claim-border-color", configC);
        configC = plugin.getConfig().getString("bluemap-claim-fill-color");
        ClaimSettings.addSetting("bluemap-claim-fill-color", configC);
        configC = plugin.getConfig().getString("bluemap-hover-text");
        ClaimSettings.addSetting("bluemap-hover-text", configC);
        
        // Add the message type for protection
        configC = plugin.getConfig().getString("protection-message");
        if(configC.equalsIgnoreCase("action_bar") || 
        		configC.equalsIgnoreCase("title") ||
        		configC.equalsIgnoreCase("subtitle") ||
        		configC.equalsIgnoreCase("chat")) {
        	ClaimSettings.addSetting("protection-message", configC);
        } else {
        	plugin.getLogger().info("'protection-message' must be 'ACTION_BAR', 'TITLE', 'SUBTITLE' or 'CHAT'. Using default value.");
        	ClaimSettings.addSetting("protection-message", "ACTION_BAR");
        }
        
        
        // Add disabled worlds
        Set<String> worlds = new HashSet<>(plugin.getConfig().getStringList("worlds-disabled"));
        ClaimSettings.setDisabledWorlds(worlds);
        
        // Checking the max length of the claim name
        configC = plugin.getConfig().getString("max-length-claim-name");
        ClaimSettings.addSetting("max-length-claim-name", configC);
        
        // Checking the max length of the claim description
        configC = plugin.getConfig().getString("max-length-claim-description");
        ClaimSettings.addSetting("max-length-claim-description", configC);
        
        // Add confirmation check setting
        configC = plugin.getConfig().getString("claim-confirmation");
        ClaimSettings.addSetting("claim-confirmation", configC);
        
        // Checking if enter/leave messages in a claim in the action bar are enabled
        configC = plugin.getConfig().getString("enter-leave-messages");
        ClaimSettings.addSetting("enter-leave-messages", configC);

        // Checking if enter/leave messages in a claim in the title/subtitle are enabled
        configC = plugin.getConfig().getString("enter-leave-title-messages");
        ClaimSettings.addSetting("enter-leave-title-messages", configC);
        
        // Checking if enter/leave messages in a claim in the chat are enabled
        configC = plugin.getConfig().getString("enter-leave-chat-messages");
        ClaimSettings.addSetting("enter-leave-chat-messages", configC);
        
        // Checking if claims where Visitors is false are displayed in the /claims gui
        configC = plugin.getConfig().getString("claims-visitors-off-visible");
        ClaimSettings.addSetting("claims-visitors-off-visible", configC);
        
        // Add economy settings
        if(check_vault) {
            configC = plugin.getConfig().getString("economy");
            String price = plugin.getConfig().getString("max-sell-price");
            String claim_cost = plugin.getConfig().getString("claim-cost");
            String claim_cost_multiplier = plugin.getConfig().getString("claim-cost-multiplier");
            ClaimSettings.addSetting("economy", configC);
            ClaimSettings.addSetting("max-sell-price", price);
            ClaimSettings.addSetting("claim-cost", claim_cost);
            ClaimSettings.addSetting("claim-cost-multiplier", claim_cost_multiplier);
        } else {
        	ClaimSettings.addSetting("economy", "false");
        }
        
        configC = plugin.getConfig().getString("bossbar");
        // Add bossbar settings
        ClaimSettings.addSetting("bossbar", plugin.getConfig().getString("bossbar"));
        if(configC.equalsIgnoreCase("true")) {
            // Chargement des paramètres de la bossbar
            String barColor = plugin.getConfig().getString("bossbar-settings.color").toUpperCase();
            BarColor color = BarColor.valueOf(barColor);
            if(color == null) {
            	plugin.getLogger().info("Invalid bossbar color, using default color YELLOW");
            	barColor = "YELLOW";
            }
            ClaimSettings.addSetting("bossbar-color", barColor);
            ClaimSettings.addSetting("bossbar-protected-area-message", plugin.getConfig().getString("bossbar-settings.protected-area-message"));
            ClaimSettings.addSetting("bossbar-owner-message", plugin.getConfig().getString("bossbar-settings.owner-message"));
            ClaimSettings.addSetting("bossbar-member-message", plugin.getConfig().getString("bossbar-settings.member-message"));
            ClaimSettings.addSetting("bossbar-visitor-message", plugin.getConfig().getString("bossbar-settings.visitor-message"));
        }
        
        // Add teleportation delay moving setting
        configC = plugin.getConfig().getString("teleportation-delay-moving");
        ClaimSettings.addSetting("teleportation-delay-moving", configC);
        
        // Add groups settings
        ConfigurationSection groupsSection = plugin.getConfig().getConfigurationSection("groups");
        LinkedHashMap<String,String> groups = new LinkedHashMap<>();
        Map<String,Map<String,Double>> groupsSettings = new HashMap<>();
        for(String key : groupsSection.getKeys(false)) {
        	if(!key.equalsIgnoreCase("default")) groups.put(key, plugin.getConfig().getString("groups."+key+".permission"));
        	Map<String,Double> settings = new HashMap<>();
        	settings.put("max-claims", plugin.getConfig().getDouble("groups."+key+".max-claims"));
        	settings.put("max-radius-claims", plugin.getConfig().getDouble("groups."+key+".max-radius-claims"));
        	settings.put("teleportation-delay", plugin.getConfig().getDouble("groups."+key+".teleportation-delay"));
        	settings.put("max-members", plugin.getConfig().getDouble("groups."+key+".max-members"));
        	settings.put("claim-cost", plugin.getConfig().getDouble("groups."+key+".claim-cost"));
        	settings.put("claim-cost-multiplier", plugin.getConfig().getDouble("groups."+key+".claim-cost-multiplier"));
        	groupsSettings.put(key, settings);
        }
        ClaimSettings.setGroups(groups);
        ClaimSettings.setGroupsSettings(groupsSettings);
        
        // Add players settings
        ConfigurationSection playersSection = plugin.getConfig().getConfigurationSection("players");
        Map<String,Map<String,Double>> playersSettings = new HashMap<>();
        for(String key : playersSection.getKeys(false)) {
        	Map<String,Double> settings = new HashMap<>();
        	if(plugin.getConfig().isSet("players."+key+".max-claims")) settings.put("max-claims", plugin.getConfig().getDouble("players."+key+".max-claims"));
        	if(plugin.getConfig().isSet("players."+key+".max-radius-claims")) settings.put("max-radius-claims", plugin.getConfig().getDouble("players."+key+".max-radius-claims"));
        	if(plugin.getConfig().isSet("players."+key+".teleportation-delay")) settings.put("teleportation-delay", plugin.getConfig().getDouble("players."+key+".teleportation-delay"));
        	if(plugin.getConfig().isSet("players."+key+".claim-cost")) settings.put("claim-cost", plugin.getConfig().getDouble("players."+key+".claim-cost"));
        	if(plugin.getConfig().isSet("players."+key+".claim-cost-multiplier")) settings.put("claim-cost-multiplier", plugin.getConfig().getDouble("players."+key+".claim-cost-multiplier"));
        	if(!settings.isEmpty()) playersSettings.put(key, settings);
        }
        CPlayerMain.setPlayersConfigSettings(playersSettings);
        
        // Register listener of enter/leave claim
        plugin.getServer().getPluginManager().registerEvents(new ClaimEventsEnterLeave(), plugin);
        
        // Add of enabled/disabled settings
        LinkedHashMap<String,Boolean> v = new LinkedHashMap<>();
        ConfigurationSection statusSettings = plugin.getConfig().getConfigurationSection("status-settings");
        for(String key : statusSettings.getKeys(false)) {
        	v.put(key, statusSettings.getBoolean(key));
        }
        ClaimSettings.setEnabledSettings(v);
        
        // Add of default settings
        v = new LinkedHashMap<>();
        statusSettings = plugin.getConfig().getConfigurationSection("default-values-settings");
        for(String key : statusSettings.getKeys(false)) {
        	v.put(key, statusSettings.getBoolean(key));
        }
        ClaimSettings.setDefaultValues(v);
        
        // Add of blocked items
        List<String> mat = plugin.getConfig().getStringList("blocked-items");
        ClaimSettings.setRestrictedItems(mat);
        
        // Add of blocked containers
        mat = plugin.getConfig().getStringList("blocked-interact-blocks");
        ClaimSettings.setRestrictedContainers(mat);
        
        // Add of blocked entities
        mat = plugin.getConfig().getStringList("blocked-entities");
        ClaimSettings.setRestrictedEntityType(mat);
        
        // Protection listener register
        plugin.getServer().getPluginManager().registerEvents(new ClaimEvents(), plugin);
        
        // Commands register
        plugin.getCommand("claim").setExecutor(new ClaimCommand());
		plugin.getCommand("unclaim").setExecutor(new UnclaimCommand());
		plugin.getCommand("aclaim").setExecutor(new AClaimCommand());
		plugin.getCommand("claims").setExecutor(new ClaimsCommand());
		plugin.getCommand("sclaim").setExecutor(new SClaimCommand());
        
        plugin.saveConfig();
		
		// Claim system register
		ClaimMain.loadClaims();

		if(reload) {
			Bukkit.getOnlinePlayers().forEach(p -> CPlayerMain.updatePlayerPermSetting(p));
			plugin.getLogger().info("============================================================");
		}
        return true;
    }
    
    // Method to reload language file
    public static void reloadLang(JavaPlugin plugin, CommandSender sender, String lang) {
    	sender.sendMessage("Loading language file [...]");
    	
    	// Checking default language file for some adds
        File custom = new File(plugin.getDataFolder()+File.separator+"langs", lang);
        if (!custom.exists()) {
        	sender.sendMessage("File '"+lang+"' not found, using en_US.yml");
        	lang = "en_US.yml";
        } else {
        	updateLangFileWithMissingKeys(plugin,lang);
        }
        ClaimSettings.addSetting("lang", lang);
        
        // Checking custom language file
        File lang_final = new File(plugin.getDataFolder()+File.separator+"langs", lang);
        FileConfiguration config = YamlConfiguration.loadConfiguration(lang_final);
        Map<String,String> messages = new HashMap<>();
        for (String key : config.getKeys(false)) {
            String value = config.getString(key);
            messages.put(key, value);
        }
        ClaimLanguage.setLanguage(messages);
        
        plugin.getConfig().set("lang", lang);
        plugin.saveConfig();
        
        sender.sendMessage("'"+lang+"' language file loaded");
    }
    
    // Method to update language file
    private static void updateLangFileWithMissingKeys(JavaPlugin plugin, String file) {
        try {
            InputStream defLangStream = plugin.getClass().getClassLoader().getResourceAsStream("langs/"+"en_US.yml");
            if (defLangStream == null) return;
            FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
            File langFile = new File(plugin.getDataFolder() + File.separator + "langs", file);
            if (!langFile.exists()) return;
            FileConfiguration customConfig = YamlConfiguration.loadConfiguration(langFile);
            boolean needSave = false;
            for (String key : defConfig.getKeys(true)) {
                if (!customConfig.contains(key)) {
                    customConfig.set(key, defConfig.get(key));
                    needSave = true;
                }
            }
            if (needSave) customConfig.save(langFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // Method to update config.yml file
    public static void updateConfigWithDefaults(JavaPlugin plugin) {
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
        	plugin.saveDefaultConfig();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource("config.yml");
        if (defConfigStream == null) return;
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));
        
        boolean changed = false;
        for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
                changed = true;
            }
        }

        if (changed) {
            try {
                config.save(configFile);
                plugin.reloadConfig();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    // Method to check if an update is available
    public static boolean checkForUpdates(JavaPlugin plugin) {
        try {
            URL url = new URL("https://api.spigotmc.org/legacy/update.php?resource=115568");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String response = reader.readLine();
                if (!Version.equalsIgnoreCase(response)) {
                	plugin.getLogger().info("A new update is available : "+response);
                	updateMessage = "§b[SimpleClaimSystem] §dA new update is available : §b"+response;
                	return true;
                } else {
                	plugin.getLogger().info("You are using the latest version : "+response);
                	return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }
    
    // Method to get instance
    public static JavaPlugin getInstance() {
    	return plugin;
    }

}
