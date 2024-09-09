package fr.xyness.SCS;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
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
import fr.xyness.SCS.API.SimpleClaimSystemAPI_Provider;
import fr.xyness.SCS.Commands.*;
import fr.xyness.SCS.Config.ClaimGuis;
import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimPurge;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Listeners.*;
import fr.xyness.SCS.Support.*;
import net.md_5.bungee.api.ChatColor;

/**
 * Main class to enable SimpleClaimSystem
 * This class provides some useful methods
 */
public class SimpleClaimSystem extends JavaPlugin {
    
	
    // ***************
    // *  Variables  *
    // ***************
    
	
    /** Instance of ClaimDynmap for dynmap integration */
    private ClaimDynmap dynmapInstance;
    
    /** Instance of ClaimBluemap for Bluemap integration */
    private ClaimBluemap bluemapInstance;
    
    /** Instance of ClaimPl3xMap for Pl3xmap integration */
    private ClaimPl3xMap pl3xmapInstance;
    
    /** Instance of ClaimWorldguard for WorldGuard integration */
    private ClaimWorldGuard claimWorldguardInstance;
    
    /** Instance of ClaimVault for Vault integration */
    private ClaimVault claimVaultInstance;
    
    /** Instance of ClaimbStats for bStats integration */
    private ClaimbStats bStatsInstance;
    
    /** Instance of ClaimMain for claim data */
    private ClaimMain claimInstance;
    
    /** Instance of ClaimGuis for claim gui data */
    private ClaimGuis claimGuisInstance;
    
    /** Instance of ClaimSettings for plugin settings */
    private ClaimSettings claimSettingsInstance;
    
    /** Instance of ClaimLanguage for custom messages */
    private ClaimLanguage claimLanguageInstance;
    
    /** Instance of ClaimPurge for purge system */
    private ClaimPurge claimPurgeInstance;
    
    /** Instance of CPlayerMain for players data */
    private CPlayerMain cPlayerMainInstance;
    
    /** Instance of ClaimBossBar for players bossbar */
    private ClaimBossBar claimBossBarInstance;
    
    /** Instance of SimpleClaimSystem for useful methods */
    private SimpleClaimSystem instance;
    
    /** The version of the plugin */
    private String Version = "1.11.5.3";
    
    /** Data source for database connections */
    private HikariDataSource dataSource;
    
    /** Whether the server is using Folia */
    private boolean isFolia = false;
    
    /** Whether an update is available for the plugin */
    private boolean isUpdateAvailable;
    
    /** The update message */
    private String updateMessage;
    
    /** Console sender */
    private ConsoleCommandSender logger = Bukkit.getConsoleSender();
    
    
    // ******************
    // *  Main Methods  *
    // ******************
    
    
    /**
     * Called when the plugin is enabled.
     */
    @Override
    public void onEnable() {
    	// Message for console
    	info("==========================================================================");
        info(ChatColor.AQUA + "  ___   ___   ___ ");
        info(ChatColor.AQUA + " / __| / __| / __|  " + ChatColor.DARK_GREEN + "SimpleClaimSystem " + ChatColor.AQUA + "v" + Version);
        info(ChatColor.AQUA + " \\__ \\ ∣(__  \\__ \\  " + ChatColor.GRAY + checkForUpdates());
        info(ChatColor.AQUA + " |___/ \\___| |___/  " + ChatColor.DARK_GRAY + "Running on " + Bukkit.getVersion());
        info(" ");
        
        // Register plugin instance
        this.instance = this;
        
        // Initialize the API
        SimpleClaimSystemAPI_Provider.initialize(this);
        
        // Load config and send finale message
        if (loadConfig(false, Bukkit.getConsoleSender())) {
            info(" ");
            info("SimpleClaimSystem is enabled !");
            info("Discord for support : https://discord.gg/6sRTGprM95");
            info("Documentation : https://xyness.gitbook.io/simpleclaimsystem");
            info("Developped by Xyness");
        } else {
            Bukkit.getServer().getPluginManager().disablePlugin(this);
        }
        info("==========================================================================");
    }
    
    /**
     * Called when the plugin is disabled.
     */
    @Override
    public void onDisable() {
        if (dataSource != null) {
            dataSource.close();
        }
        // Disable players bossbar (prevent for /reload)
        Bukkit.getOnlinePlayers().forEach(p -> claimBossBarInstance.disableBossBar(p));
        info("==========================================================================");
        info("SimpleClaimSystem is disabled !");
        info("Discord for support : https://discord.gg/6sRTGprM95");
        info("Documentation : https://xyness.gitbook.io/simpleclaimsystem");
        info("Developped by Xyness");
        info("==========================================================================");
    }
    
    /**
     * Called when the plugin is loaded (only for WorldGuard support).
     */
    @Override
    public void onLoad() {
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
        	claimWorldguardInstance = new ClaimWorldGuard();
            claimWorldguardInstance.registerCustomFlag();
        }
    }
    
    
    // ********************
    // *  Other Methods   *
    // ********************
    
    
    /**
     * Loads or reloads the plugin configuration.
     * 
     * @param reload Whether to reload the configuration
     * @return True if the configuration was loaded successfully, false otherwise
     */
    public boolean loadConfig(boolean reload, CommandSender sender) {
    	
    	boolean[] status = {true};
    	
    	Runnable reloadTask = () -> {
    		
            if (reload) {
            	sender.sendMessage(getLanguage().getMessage("reload-attempt"));
            	info("==========================================================================");
            }
            
            // Save and reload config
            saveDefaultConfig();
            reloadConfig();
            
            // Unregister all handlers
            if(reload) {
                HandlerList.unregisterAll(this);
                claimInstance.clearAll();
                claimSettingsInstance.clearAll();
                cPlayerMainInstance.clearAll();
                claimBossBarInstance.clearAll();
            } else {
            	claimInstance = new ClaimMain(this);
            	claimGuisInstance = new ClaimGuis(this);
            	claimSettingsInstance = new ClaimSettings();
            	cPlayerMainInstance = new CPlayerMain(this);
            	claimLanguageInstance = new ClaimLanguage(this);
            	claimBossBarInstance = new ClaimBossBar(this);
            	bStatsInstance = new ClaimbStats();
            	bStatsInstance.enableMetrics(this);
            }
            
            // Update config if necessary
            updateConfigWithDefaults();
            // Check Folia
            checkFolia();
            
            // Check GriefPrevention
            if (Bukkit.getPluginManager().getPlugin("GriefPrevention") != null) {
                claimSettingsInstance.addSetting("griefprevention", "true");
            } else {
                claimSettingsInstance.addSetting("griefprevention", "false");
            }
            
            // Check PlaceholderAPI
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                claimSettingsInstance.addSetting("placeholderapi", "true");
                if(!reload) new ClaimPlaceholdersExpansion(this).register();
            } else {
                claimSettingsInstance.addSetting("placeholderapi", "false");
            }
            
            // Check WorldGuard
            if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
                claimSettingsInstance.addSetting("worldguard", "true");
            } else {
                claimSettingsInstance.addSetting("worldguard", "false");
            }
            
            // Check Vault
            boolean[] check_vault = {false};
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            	claimSettingsInstance.addSetting("vault", "true");
            	if(!reload) {
                	claimVaultInstance = new ClaimVault();
                    if (claimVaultInstance.setupEconomy()) {
                        claimSettingsInstance.addSetting("vault", "true");
                        check_vault[0] = true;
                    } else {
                        claimSettingsInstance.addSetting("vault", "false");
                    }
            	}
            } else {
                claimSettingsInstance.addSetting("vault", "false");
            }
            
            // Check Dynmap
            Plugin dynmap = Bukkit.getPluginManager().getPlugin("dynmap");
            if (dynmap != null) {
                claimSettingsInstance.addSetting("dynmap", "true");
            } else {
                claimSettingsInstance.addSetting("dynmap", "false");
            }
            
            // Check Bluemap
            Plugin bluemap = Bukkit.getPluginManager().getPlugin("bluemap");
            if (bluemap != null) {
                claimSettingsInstance.addSetting("bluemap", "true");
            } else {
                claimSettingsInstance.addSetting("bluemap", "false");
            }
            
            // Check Pl3xmap
            Plugin pl3xmap = Bukkit.getPluginManager().getPlugin("pl3xmap");
            if (pl3xmap != null) {
                claimSettingsInstance.addSetting("pl3xmap", "true");
            } else {
                claimSettingsInstance.addSetting("pl3xmap", "false");
            }

            // Check "langs" folder
            File dossier = new File(getDataFolder(), "langs");
            if (!dossier.exists()) {
                dossier.mkdirs();
            }
            
            // Check default language file for additions
            checkAndSaveResource("langs/en_US.yml");
            updateLangFileWithMissingKeys("en_US.yml");
            
            // Check custom language file
            String lang = getConfig().getString("lang");
            File custom = new File(getDataFolder() + File.separator + "langs", lang);
            if (!custom.exists()) {
                info(ChatColor.RED + "File '" + lang + "' not found, using en_US.yml");
                lang = "en_US.yml";
            } else {
                updateLangFileWithMissingKeys(lang);
            }
            claimSettingsInstance.addSetting("lang", lang);
            
            // Load selected language file
            File lang_final = new File(getDataFolder() + File.separator + "langs", lang);
            FileConfiguration config = YamlConfiguration.loadConfiguration(lang_final);
            Map<String, String> messages = new HashMap<>();
            for (String key : config.getKeys(false)) {
                if (key.equals("help-command")) {
                    ConfigurationSection configHelp = config.getConfigurationSection("help-command");
                    for (String help : configHelp.getKeys(false)) {
                        messages.put("help-command." + help, configHelp.getString(help));
                    }
                    continue;
                }
                String value = config.getString(key);
                messages.put(key, value);
            }
            claimLanguageInstance.setLanguage(messages);
            
            // Add default settings (before loading DB)
            Map<String,LinkedHashMap<String, Boolean>> defaultSettings = new HashMap<>();
            LinkedHashMap<String, Boolean> v = new LinkedHashMap<>();
            ConfigurationSection statusSettings = getConfig().getConfigurationSection("default-values-settings");
            for (String key : statusSettings.getKeys(false)) {
            	ConfigurationSection statusSettingsSub = statusSettings.getConfigurationSection(key);
            	v = new LinkedHashMap<>();
            	for (String subkey : statusSettingsSub.getKeys(false)) {
            		v.put(subkey, statusSettingsSub.getBoolean(subkey));
            	}
            	defaultSettings.put(key.toLowerCase(), v);
            }
            claimSettingsInstance.setDefaultValues(defaultSettings);
            
            // Check database
            String configC = getConfig().getString("database");
            if (configC.equalsIgnoreCase("true")) {
                // Create data source
                HikariConfig configH = new HikariConfig();
                configH.setJdbcUrl("jdbc:mysql://" + getConfig().getString("database-settings.hostname") + ":" + getConfig().getString("database-settings.port") + "/" + getConfig().getString("database-settings.database_name"));
                configH.setUsername(getConfig().getString("database-settings.username"));
                configH.setPassword(getConfig().getString("database-settings.password"));
                configH.addDataSourceProperty("cachePrepStmts", "true");
                configH.addDataSourceProperty("prepStmtCacheSize", "250");
                configH.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                configH.addDataSourceProperty("useServerPrepStmts", "true");
                configH.setPoolName("MySQL");
                configH.setMaximumPoolSize(10);
                configH.setMinimumIdle(2);
                configH.setIdleTimeout(60000);
                configH.setMaxLifetime(600000);
                dataSource = new HikariDataSource(configH);
                try (Connection connection = dataSource.getConnection()) {
                    info("Database connection successful.");
                    try (Statement stmt = connection.createStatement()) {
                    	String sql = "CREATE TABLE IF NOT EXISTS scs_claims_1 ("
                    		    + "id INT AUTO_INCREMENT PRIMARY KEY, "
                    		    + "id_claim INT NOT NULL, "
                    		    + "owner_uuid VARCHAR(36) NOT NULL, "
                    		    + "owner_name VARCHAR(36) NOT NULL, "
                    		    + "claim_name VARCHAR(255) NOT NULL, "
                    		    + "claim_description VARCHAR(255) NOT NULL, "
                    		    + "chunks TEXT NOT NULL, "
                    		    + "world_name VARCHAR(255) NOT NULL, "
                    		    + "location VARCHAR(255) NOT NULL, "
                    		    + "members TEXT NOT NULL, "
                    		    + "permissions VARCHAR(510) NOT NULL, "
                    		    + "for_sale TINYINT(1) NOT NULL DEFAULT 0, "
                    		    + "sale_price DOUBLE NOT NULL DEFAULT 0, "
                    		    + "bans TEXT NOT NULL)";
                    		stmt.executeUpdate(sql);

                    		sql = "CREATE TABLE IF NOT EXISTS scs_players ("
                    		    + "id INT AUTO_INCREMENT PRIMARY KEY, " 
                    		    + "uuid_server VARCHAR(36) NOT NULL UNIQUE, "
                    		    + "uuid_mojang VARCHAR(36) NOT NULL, "
                    		    + "player_name VARCHAR(36) NOT NULL, "
                    		    + "player_head TEXT NOT NULL, "
                    		    + "player_textures TEXT NOT NULL)";
                    		stmt.executeUpdate(sql);
                    		
                    		sql = "UPDATE scs_claims_1 SET owner_uuid = '" + ClaimMain.SERVER_UUID.toString() + "' WHERE owner_uuid = 'none';";
                    		stmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        info(ChatColor.RED + "Error creating tables, using local db.");
                        configC = "false";
                    }
                    
                    DatabaseMetaData metaData = connection.getMetaData();
                    try (ResultSet resultSet = metaData.getTables(null, null, "scs_claims", new String[]{"TABLE"})) {
                        if(resultSet.next()) {
                        	claimInstance.convertDistantToNewDistant();
                        }
                    }
                } catch (SQLException e) {
                    info(ChatColor.RED + "Error connecting to database, using local db.");
                    configC = "false";
                }
            }
            if (configC.equals("false")) {
                HikariConfig configH = new HikariConfig();
                configH.setJdbcUrl("jdbc:sqlite:plugins/SimpleClaimSystem/storage.db");
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
                        String sql = "CREATE TABLE IF NOT EXISTS scs_claims_1 " +
                                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    		    "id_claim INT NOT NULL, " +
                    		    "owner_uuid VARCHAR(36) NOT NULL, " +
                    		    "owner_name VARCHAR(36) NOT NULL, " +
                    		    "claim_name VARCHAR(255) NOT NULL, " +
                    		    "claim_description VARCHAR(255) NOT NULL, " +
                    		    "chunks TEXT NOT NULL, " +
                    		    "world_name VARCHAR(255) NOT NULL, " +
                    		    "location VARCHAR(255) NOT NULL, " +
                    		    "members TEXT NOT NULL, " +
                    		    "permissions VARCHAR(510) NOT NULL, " +
                    		    "for_sale TINYINT(1) NOT NULL DEFAULT 0, " +
                    		    "sale_price DOUBLE NOT NULL DEFAULT 0, " +
                    		    "bans TEXT NOT NULL DEFAULT '')";
                        stmt.executeUpdate(sql);
                    	sql = "CREATE TABLE IF NOT EXISTS scs_players " +
                    		    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    		    "uuid_server VARCHAR(36) NOT NULL UNIQUE, " +
                    		    "uuid_mojang VARCHAR(36) NOT NULL, " + 
                    		    "player_name VARCHAR(36) NOT NULL, " +
                    		    "player_head TEXT NOT NULL, " +
                    		    "player_textures TEXT NOT NULL)";
                        stmt.executeUpdate(sql);
                		sql = "UPDATE scs_claims_1 SET owner_uuid = '" + ClaimMain.SERVER_UUID.toString() + "' WHERE owner_uuid = 'none';";
                		stmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        info(ChatColor.RED + "Error creating tables, disabling ");
                        status[0] = false;
                        return;
                    }
                    
                } catch (SQLException e) {
                    info(ChatColor.RED + "Error creating tables, disabling ");
                    status[0] = false;
                    return;
                }
                
                // Check new DB
                String databasePath = "plugins/SimpleClaimSystem/claims.db";
                File databaseFile = new File(databasePath);

                if (databaseFile.exists()) {
                	claimInstance.convertLocalToNewLocal();
                }
            }
            claimSettingsInstance.addSetting("database", configC);
            
            // Auto-purge settings
            configC = getConfig().getString("auto-purge");
            claimSettingsInstance.addSetting("auto-purge", configC);
            if (configC.equals("true")) {
                configC = getConfig().getString("auto-purge-checking");
                claimSettingsInstance.addSetting("auto-purge-checking", configC);
                try {
                    int minutes = Integer.parseInt(configC);
                    if (minutes < 1) {
                        info(ChatColor.RED + "'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
                        minutes = 60;
                    }
                    configC = getConfig().getString("auto-purge-time-without-login");
                    claimSettingsInstance.addSetting("auto-purge-time-without-login", configC);
                    claimPurgeInstance = new ClaimPurge(this);
                    claimPurgeInstance.startPurge(minutes, configC);
                } catch (NumberFormatException e) {
                    info(ChatColor.RED + "'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
                    int minutes = 60;
                    configC = getConfig().getString("auto-purge-time-without-login");
                    claimSettingsInstance.addSetting("auto-purge-time-without-login", configC);
                    claimPurgeInstance = new ClaimPurge(this);
                    claimPurgeInstance.startPurge(minutes, configC);
                }
            }
            
            // Aliases settings
            List<String> aliases_claim = getConfig().getStringList("command-aliases.claim");
            List<String> aliases_unclaim = getConfig().getStringList("command-aliases.unclaim");
            List<String> aliases_claims = getConfig().getStringList("command-aliases.claims");
            if (aliases_claim == null) {
                info(ChatColor.RED + "'aliases_claim' is missing in config. Using default value.");
                aliases_claim = new ArrayList<>();
                aliases_claim.add("/territory");
            }
            if (aliases_unclaim == null) {
                info(ChatColor.RED + "'aliases_unclaim' is missing in config. Using default value.");
                aliases_unclaim = new ArrayList<>();
                aliases_unclaim.add("/unterritory");
            }
            if (aliases_claims == null) {
                info(ChatColor.RED + "'aliases_claims' is missing in config. Using default value.");
                aliases_claims = new ArrayList<>();
                aliases_claims.add("/territories");
            }
            for(String command : aliases_claim) {
            	claimSettingsInstance.addAliase(command, "/claim");
            }
            for(String command : aliases_unclaim) {
            	claimSettingsInstance.addAliase(command, "/unclaim");
            }
            for(String command : aliases_claims) {
            	claimSettingsInstance.addAliase(command, "/claims");
            }
            
            // Add Dynmap settings
            configC = getConfig().getString("dynmap");
            if(configC.equalsIgnoreCase("true") && claimSettingsInstance.getBooleanSetting("dynmap")) {
                if (!reload) {
                    DynmapAPI dynmapAPI = (DynmapAPI) dynmap;
                    MarkerAPI markerAPI = dynmapAPI.getMarkerAPI();
                    if (markerAPI != null) {
                        MarkerSet markerSet = markerAPI.createMarkerSet("SimpleClaimSystem", "Claims", null, false);
                        dynmapInstance = new ClaimDynmap(dynmapAPI, markerAPI, markerSet, this);
                    }
                }
            } else {
            	claimSettingsInstance.addSetting("dynmap", "false");
            }
            claimSettingsInstance.addSetting("dynmap-claim-border-color", getConfig().getString("dynmap-settings.claim-border-color"));
            claimSettingsInstance.addSetting("dynmap-claim-fill-color", getConfig().getString("dynmap-settings.claim-fill-color"));
            claimSettingsInstance.addSetting("dynmap-claim-hover-text", getConfig().getString("dynmap-settings.claim-hover-text"));
            
            // Add Bluemap settings
            configC = getConfig().getString("bluemap");
            if(configC.equalsIgnoreCase("true") && claimSettingsInstance.getBooleanSetting("bluemap")) {
            	if(!reload) {
                    BlueMapAPI.onEnable(api -> {
                        // Register marker set
                        bluemapInstance = new ClaimBluemap(api,this);
                    });
            	}
            } else {
            	claimSettingsInstance.addSetting("bluemap", "false");
            }
            claimSettingsInstance.addSetting("bluemap-claim-border-color", getConfig().getString("bluemap-settings.claim-border-color"));
            claimSettingsInstance.addSetting("bluemap-claim-fill-color", getConfig().getString("bluemap-settings.claim-fill-color"));
            claimSettingsInstance.addSetting("bluemap-claim-hover-text", getConfig().getString("bluemap-settings.claim-hover-text"));
            
            // Add Pl3xmap settings
            configC = getConfig().getString("pl3xmap");
            if(configC.equalsIgnoreCase("true") && claimSettingsInstance.getBooleanSetting("pl3xmap")) {
            	if (!reload) pl3xmapInstance = new ClaimPl3xMap(this);
            } else {
            	claimSettingsInstance.addSetting("pl3xmap", "false");
            }
            claimSettingsInstance.addSetting("pl3xmap-claim-border-color", getConfig().getString("pl3xmap-settings.claim-border-color"));
            claimSettingsInstance.addSetting("pl3xmap-claim-fill-color", getConfig().getString("pl3xmap-settings.claim-fill-color"));
            claimSettingsInstance.addSetting("pl3xmap-claim-hover-text", getConfig().getString("pl3xmap-settings.claim-hover-text"));
            
            // Add the message type for protection
            configC = getConfig().getString("protection-message");
            if (configC.equalsIgnoreCase("action_bar") || 
                    configC.equalsIgnoreCase("title") ||
                    configC.equalsIgnoreCase("subtitle") ||
                    configC.equalsIgnoreCase("chat") ||
                    configC.equalsIgnoreCase("bossbar")) {
                claimSettingsInstance.addSetting("protection-message", configC);
            } else {
                info(ChatColor.RED + "'protection-message' must be 'ACTION_BAR', 'TITLE', 'SUBTITLE', 'CHAT' or 'BOSSBAR'. Using default value.");
                claimSettingsInstance.addSetting("protection-message", "ACTION_BAR");
            }
            
            // Add disabled worlds
            Set<String> worlds = new HashSet<>(getConfig().getStringList("worlds-disabled"));
            claimSettingsInstance.setDisabledWorlds(worlds);
            
            // Check the preload chunks
            claimSettingsInstance.addSetting("preload-chunks", getConfig().getString("preload-chunks"));
            
            // Check the keep chunks loaded
            claimSettingsInstance.addSetting("keep-chunks-loaded", getConfig().getString("keep-chunks-loaded"));
            
            // Check the max length of the claim name
            claimSettingsInstance.addSetting("max-length-claim-name", getConfig().getString("max-length-claim-name"));
            
            // Check the max length of the claim description
            claimSettingsInstance.addSetting("max-length-claim-description", getConfig().getString("max-length-claim-description"));
            
            // Add confirmation check setting
            claimSettingsInstance.addSetting("claim-confirmation", getConfig().getString("claim-confirmation"));
            
            // Add claim particles setting
            claimSettingsInstance.addSetting("claim-particles", getConfig().getString("claim-particles"));
            
            // Add claim fly disabled on damage setting
            claimSettingsInstance.addSetting("claim-fly-disabled-on-damage", getConfig().getString("claim-fly-disabled-on-damage"));
            
            // Add claim fly message setting
            claimSettingsInstance.addSetting("claim-fly-message-auto-fly", getConfig().getString("claim-fly-message-auto-fly"));
            
            // Check if enter/leave messages in a claim in the action bar are enabled
            claimSettingsInstance.addSetting("enter-leave-messages", getConfig().getString("enter-leave-messages"));
            
            // Check if enter/leave messages in a claim in the title/subtitle are enabled
            claimSettingsInstance.addSetting("enter-leave-title-messages", getConfig().getString("enter-leave-title-messages"));
            
            // Check if enter/leave messages in a claim in the chat are enabled
            claimSettingsInstance.addSetting("enter-leave-chat-messages", getConfig().getString("enter-leave-chat-messages"));
            
            // Check if claims where Visitors is false are displayed in the /claims GUI
            claimSettingsInstance.addSetting("claims-visitors-off-visible", getConfig().getString("claims-visitors-off-visible"));
            
            // Check where the auto-map is sent
            configC = getConfig().getString("map-type").toLowerCase();
            if(configC.equals("scoreboard") && isFolia) {
            	info(ChatColor.RED + "'map-type' set to 'CHAT' because the server is running on Folia.");
            	configC = "chat";
            }
            claimSettingsInstance.addSetting("map-type", configC);
            
            // Add economy settings
            if (check_vault[0]) {
                claimSettingsInstance.addSetting("economy", getConfig().getString("economy"));
                claimSettingsInstance.addSetting("max-sell-price", getConfig().getString("max-sell-price"));
                claimSettingsInstance.addSetting("claim-cost", getConfig().getString("claim-cost"));
                claimSettingsInstance.addSetting("claim-cost-multiplier", getConfig().getString("claim-cost-multiplier"));
            } else {
                claimSettingsInstance.addSetting("economy", "false");
            }
            
            // Add announce sale settings
            claimSettingsInstance.addSetting("announce-sale.bossbar", getConfig().getString("announce-sale.bossbar"));
            String barColor = getConfig().getString("announce-sale.bossbar-settings.color").toUpperCase();
            try {
            	BarColor color = BarColor.valueOf(barColor);
            	if(color == null) {
                    info(ChatColor.RED + "Invalid bossbar color, using default color RED.");
                    barColor = "YELLOW";
            	}
            } catch (IllegalArgumentException e) {
                info(ChatColor.RED + "Invalid bossbar color, using default color RED.");
                barColor = "YELLOW";
            }
            String barStyle = getConfig().getString("announce-sale.bossbar-settings.style").toUpperCase();
            try {
            	BarStyle style = BarStyle.valueOf(barStyle);
            	if(style == null) {
                	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
                	barStyle = "SOLID";
            	}
            } catch (IllegalArgumentException e) {
            	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
            	barStyle = "SOLID";
            }
            claimSettingsInstance.addSetting("announce-sale.bossbar-settings.color", barColor);
            claimSettingsInstance.addSetting("announce-sale.bossbar-settings.style", barStyle);
            claimSettingsInstance.addSetting("announce-sale.chat", getConfig().getString("announce-sale.chat"));
            claimSettingsInstance.addSetting("announce-sale.title", getConfig().getString("announce-sale.title"));
            claimSettingsInstance.addSetting("announce-sale.actionbar", getConfig().getString("announce-sale.actionbar"));
            
            // Add bossbar settings
            configC = getConfig().getString("bossbar");
            claimSettingsInstance.addSetting("bossbar", configC);
            // Load bossbar settings
            barColor = getConfig().getString("bossbar-settings.color").toUpperCase();
            try {
            	BarColor color = BarColor.valueOf(barColor);
            	if(color == null) {
                    info(ChatColor.RED + "Invalid bossbar color, using default color YELLOW.");
                    barColor = "YELLOW";
            	}
            } catch (IllegalArgumentException e) {
                info(ChatColor.RED + "Invalid bossbar color, using default color YELLOW.");
                barColor = "YELLOW";
            }
            barStyle = getConfig().getString("bossbar-settings.style").toUpperCase();
            try {
            	BarStyle style = BarStyle.valueOf(barStyle);
            	if(style == null) {
                	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
                	barStyle = "SOLID";
            	}
            } catch (IllegalArgumentException e) {
            	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
            	barStyle = "SOLID";
            }
            claimSettingsInstance.addSetting("bossbar-color", barColor);
            claimSettingsInstance.addSetting("bossbar-style", barStyle);
            
            // Add teleportation delay moving setting
            claimSettingsInstance.addSetting("teleportation-delay-moving", getConfig().getString("teleportation-delay-moving"));
            
            // Add group settings
            ConfigurationSection groupsSection = getConfig().getConfigurationSection("groups");
            LinkedHashMap<String, String> groups = new LinkedHashMap<>();
            Map<String, Map<String, Double>> groupsSettings = new HashMap<>();
            for (String key : groupsSection.getKeys(false)) {
                if (!key.equalsIgnoreCase("default")) groups.put(key, getConfig().getString("groups." + key + ".permission"));
                Map<String, Double> settings = new HashMap<>();
                settings.put("max-claims", getConfig().getDouble("groups." + key + ".max-claims"));
                settings.put("max-radius-claims", getConfig().getDouble("groups." + key + ".max-radius-claims"));
                settings.put("teleportation-delay", getConfig().getDouble("groups." + key + ".teleportation-delay"));
                settings.put("max-members", getConfig().getDouble("groups." + key + ".max-members"));
                settings.put("claim-cost", getConfig().getDouble("groups." + key + ".claim-cost"));
                settings.put("claim-cost-multiplier", getConfig().getDouble("groups." + key + ".claim-cost-multiplier"));
                settings.put("max-chunks-per-claim", getConfig().getDouble("groups." + key + ".max-chunks-per-claim"));
                settings.put("claim-distance", getConfig().getDouble("groups." + key + ".claim-distance"));
                settings.put("max-chunks-total", getConfig().getDouble("groups." + key + ".max-chunks-total"));
                groupsSettings.put(key, settings);
            }
            claimSettingsInstance.setGroups(groups);
            claimSettingsInstance.setGroupsSettings(groupsSettings);
            
            // Add player settings
            ConfigurationSection playersSection = getConfig().getConfigurationSection("players");
            Map<UUID, Map<String, Double>> playersSettings = new HashMap<>();
            for (String key : playersSection.getKeys(false)) {
                Map<String, Double> settings = new HashMap<>();
                if (getConfig().isSet("players." + key + ".max-claims")) settings.put("max-claims", getConfig().getDouble("players." + key + ".max-claims"));
                if (getConfig().isSet("players." + key + ".max-radius-claims")) settings.put("max-radius-claims", getConfig().getDouble("players." + key + ".max-radius-claims"));
                if (getConfig().isSet("players." + key + ".teleportation-delay")) settings.put("teleportation-delay", getConfig().getDouble("players." + key + ".teleportation-delay"));
                if (getConfig().isSet("players." + key + ".claim-cost")) settings.put("claim-cost", getConfig().getDouble("players." + key + ".claim-cost"));
                if (getConfig().isSet("players." + key + ".claim-cost-multiplier")) settings.put("claim-cost-multiplier", getConfig().getDouble("players." + key + ".claim-cost-multiplier"));
                if (getConfig().isSet("players." + key + ".max-chunks-per-claim")) settings.put("max-chunks-per-claim", getConfig().getDouble("players." + key + ".max-chunks-per-claim"));
                if (getConfig().isSet("players." + key + ".claim-distance")) settings.put("claim-distance", getConfig().getDouble("players." + key + ".claim-distance"));
                if (getConfig().isSet("players." + key + ".max-chunks-total")) settings.put("max-chunks-total", getConfig().getDouble("players." + key + ".max-chunks-total"));
                if (!settings.isEmpty()) playersSettings.put(Bukkit.getOfflinePlayer(key).getUniqueId(), settings);
            }
            cPlayerMainInstance.setPlayersConfigSettings(playersSettings);
            
            // Register listener for entering/leaving claims
            getServer().getPluginManager().registerEvents(new ClaimEventsEnterLeave(this), this);
            
            // Register listener for guis
            getServer().getPluginManager().registerEvents(new ClaimGuiEvents(this), this);
            
            // Add enabled/disabled settings
            v = new LinkedHashMap<>();
            statusSettings = getConfig().getConfigurationSection("status-settings");
            for (String key : statusSettings.getKeys(false)) {
                v.put(key, statusSettings.getBoolean(key));
            }
            claimSettingsInstance.setEnabledSettings(v);
            
            // Add blocked items
            claimSettingsInstance.setRestrictedItems(getConfig().getStringList("blocked-items"));
            
            // Add blocked containers
            claimSettingsInstance.setRestrictedContainers(getConfig().getStringList("blocked-interact-blocks"));
            
            // Add blocked entities
            claimSettingsInstance.setRestrictedEntityType(getConfig().getStringList("blocked-entities"));
            
            // Add special blocks
            claimSettingsInstance.setSpecialBlocks(getConfig().getStringList("special-blocks"));
            
            // Add ignored break blocks
            claimSettingsInstance.setBreakBlocksIgnore(getConfig().getStringList("ignored-break-blocks"));
            
            // Add ignored place blocks
            claimSettingsInstance.setPlaceBlocksIgnore(getConfig().getStringList("ignored-place-blocks"));
            
            // Register protection listener
            getServer().getPluginManager().registerEvents(new ClaimEvents(this), this);
            
            // Register commands
            getCommand("claim").setExecutor(new ClaimCommand(this));
            getCommand("unclaim").setExecutor(new UnclaimCommand(this));
            getCommand("scs").setExecutor(new ScsCommand(this));
            getCommand("claims").setExecutor(new ClaimsCommand(this));
            getCommand("protectedarea").setExecutor(new ProtectedAreaCommand(this));
            
            // Save config
            saveConfig();
            
            // Load bossbar default settings
            claimBossBarInstance.loadBossbarSettings();

            // Load claims system
            claimInstance.loadClaims();
            
            // Load players
            cPlayerMainInstance.loadPlayers();
            
            // Add players setting and active their bossbar (/reload prevention)
            Bukkit.getOnlinePlayers().forEach(p -> {
            	cPlayerMainInstance.addPlayerPermSetting(p);
            	claimBossBarInstance.activeBossBar(p, p.getLocation().getChunk());
            });
            if(reload) {
            	info("==========================================================================");
                if(status[0]) {
                	sender.sendMessage(getLanguage().getMessage("reload-complete"));
                } else {
                	sender.sendMessage(getLanguage().getMessage("reload-not-complete"));
                }
            }
    	};
    	
    	if(reload) {
    		executeAsync(() -> reloadTask.run());
    	} else {
    		reloadTask.run();
    	}
        
        return status[0];
    }
    
    /**
     * Loads or reloads the plugin configuration.
     * 
     * @return True if the configuration was loaded successfully, false otherwise
     */
    public boolean reloadOnlyConfig(CommandSender sender) {
    	
    	boolean[] status = {true};
    	
    	executeAsync(() -> {
    		
    		sender.sendMessage(getLanguage().getMessage("config-reload-attempt"));
    		info("==========================================================================");
    		
    		// Save and reload config
            saveDefaultConfig();
            reloadConfig();
            
            // Clear bossbars
            claimBossBarInstance.clearAll();
            
            // Update config if necessary
            updateConfigWithDefaults();
            // Check Folia
            checkFolia();
            
            // Check Vault
            boolean check_vault = false;
            if (Bukkit.getPluginManager().getPlugin("Vault") != null) {
            	claimVaultInstance = new ClaimVault();
                if (claimVaultInstance.setupEconomy()) {
                    claimSettingsInstance.addSetting("vault", "true");
                    check_vault = true;
                } else {
                    claimSettingsInstance.addSetting("vault", "false");
                }
            } else {
                claimSettingsInstance.addSetting("vault", "false");
            }

            // Check "langs" folder
            File dossier = new File(getDataFolder(), "langs");
            if (!dossier.exists()) {
                dossier.mkdirs();
            }
            
            // Check default language file for additions
            checkAndSaveResource("langs/en_US.yml");
            updateLangFileWithMissingKeys("en_US.yml");
            
            // Check custom language file
            String lang = getConfig().getString("lang");
            File custom = new File(getDataFolder() + File.separator + "langs", lang);
            if (!custom.exists()) {
                info(ChatColor.RED + "File '" + lang + "' not found, using en_US.yml");
                lang = "en_US.yml";
            } else {
                updateLangFileWithMissingKeys(lang);
            }
            claimSettingsInstance.addSetting("lang", lang);
            
            // Load selected language file
            File lang_final = new File(getDataFolder() + File.separator + "langs", lang);
            FileConfiguration config = YamlConfiguration.loadConfiguration(lang_final);
            Map<String, String> messages = new HashMap<>();
            for (String key : config.getKeys(false)) {
                if (key.equals("help-command")) {
                    ConfigurationSection configHelp = config.getConfigurationSection("help-command");
                    for (String help : configHelp.getKeys(false)) {
                        messages.put("help-command." + help, configHelp.getString(help));
                    }
                    continue;
                }
                String value = config.getString(key);
                messages.put(key, value);
            }
            claimLanguageInstance.setLanguage(messages);
            
            // Add default settings (before loading DB)
            Map<String,LinkedHashMap<String, Boolean>> defaultSettings = new HashMap<>();
            LinkedHashMap<String, Boolean> v = new LinkedHashMap<>();
            ConfigurationSection statusSettings = getConfig().getConfigurationSection("default-values-settings");
            for (String key : statusSettings.getKeys(false)) {
            	ConfigurationSection statusSettingsSub = statusSettings.getConfigurationSection(key);
            	v = new LinkedHashMap<>();
            	for (String subkey : statusSettingsSub.getKeys(false)) {
            		v.put(subkey, statusSettingsSub.getBoolean(subkey));
            	}
            	defaultSettings.put(key.toLowerCase(), v);
            }
            claimSettingsInstance.setDefaultValues(defaultSettings);
            
            // Check database
            String configC = getConfig().getString("database");
            if (configC.equalsIgnoreCase("true")) {
                // Create data source
                HikariConfig configH = new HikariConfig();
                configH.setJdbcUrl("jdbc:mysql://" + getConfig().getString("database-settings.hostname") + ":" + getConfig().getString("database-settings.port") + "/" + getConfig().getString("database-settings.database_name"));
                configH.setUsername(getConfig().getString("database-settings.username"));
                configH.setPassword(getConfig().getString("database-settings.password"));
                configH.addDataSourceProperty("cachePrepStmts", "true");
                configH.addDataSourceProperty("prepStmtCacheSize", "250");
                configH.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
                configH.addDataSourceProperty("useServerPrepStmts", "true");
                configH.setPoolName("MySQL");
                configH.setMaximumPoolSize(10);
                configH.setMinimumIdle(2);
                configH.setIdleTimeout(60000);
                configH.setMaxLifetime(600000);
                dataSource = new HikariDataSource(configH);
                try (Connection connection = dataSource.getConnection()) {
                    info("Database connection successful.");
                    try (Statement stmt = connection.createStatement()) {
                    	String sql = "CREATE TABLE IF NOT EXISTS scs_claims_1 " +
                    		    "(id INT AUTO_INCREMENT PRIMARY KEY, " +
                    		    "id_claim INT NOT NULL, " +
                    		    "owner_uuid VARCHAR(36) NOT NULL, " +
                    		    "owner_name VARCHAR(36) NOT NULL, " +
                    		    "claim_name VARCHAR(255) NOT NULL, " +
                    		    "claim_description VARCHAR(255) NOT NULL, " +
                    		    "chunks TEXT NOT NULL, " +
                    		    "world_name VARCHAR(255) NOT NULL, " +
                    		    "location VARCHAR(255) NOT NULL, " +
                    		    "members TEXT NOT NULL, " +
                    		    "permissions VARCHAR(510) NOT NULL, " +
                    		    "for_sale TINYINT(1) NOT NULL DEFAULT 0, " +
                    		    "sale_price DOUBLE NOT NULL DEFAULT 0, " +
                    		    "bans TEXT NOT NULL DEFAULT '')";
                        stmt.executeUpdate(sql);
                    	sql = "CREATE TABLE IF NOT EXISTS scs_players " +
                    		    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    		    "uuid_server VARCHAR(36) NOT NULL UNIQUE, " +
                    		    "uuid_mojang VARCHAR(36) NOT NULL, " + 
                    		    "player_name VARCHAR(36) NOT NULL, " +
                    		    "player_head TEXT NOT NULL, " +
                    		    "player_textures TEXT NOT NULL)";
                        stmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        info(ChatColor.RED + "Error creating tables, using local db.");
                        configC = "false";
                    }
                    
                    DatabaseMetaData metaData = connection.getMetaData();
                    try (ResultSet resultSet = metaData.getTables(null, null, "scs_claims", new String[]{"TABLE"})) {
                        if(resultSet.next()) {
                        	claimInstance.convertDistantToNewDistant();
                        }
                    }
                } catch (SQLException e) {
                    info(ChatColor.RED + "Error connecting to database, using local db.");
                    configC = "false";
                }
            }
            if (configC.equals("false")) {
                HikariConfig configH = new HikariConfig();
                configH.setJdbcUrl("jdbc:sqlite:plugins/SimpleClaimSystem/storage.db");
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
                        String sql = "CREATE TABLE IF NOT EXISTS scs_claims_1 " +
                                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    		    "id_claim INT NOT NULL, " +
                    		    "owner_uuid VARCHAR(36) NOT NULL, " +
                    		    "owner_name VARCHAR(36) NOT NULL, " +
                    		    "claim_name VARCHAR(255) NOT NULL, " +
                    		    "claim_description VARCHAR(255) NOT NULL, " +
                    		    "chunks TEXT NOT NULL, " +
                    		    "world_name VARCHAR(255) NOT NULL, " +
                    		    "location VARCHAR(255) NOT NULL, " +
                    		    "members TEXT NOT NULL, " +
                    		    "permissions VARCHAR(510) NOT NULL, " +
                    		    "for_sale TINYINT(1) NOT NULL DEFAULT 0, " +
                    		    "sale_price DOUBLE NOT NULL DEFAULT 0, " +
                    		    "bans TEXT NOT NULL DEFAULT '')";
                        stmt.executeUpdate(sql);
                    	sql = "CREATE TABLE IF NOT EXISTS scs_players " +
                    		    "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    		    "uuid_server VARCHAR(36) NOT NULL UNIQUE, " +
                    		    "uuid_mojang VARCHAR(36) NOT NULL, " + 
                    		    "player_name VARCHAR(36) NOT NULL, " +
                    		    "player_head TEXT NOT NULL, " +
                    		    "player_textures TEXT NOT NULL)";
                        stmt.executeUpdate(sql);
                    } catch (SQLException e) {
                        info(ChatColor.RED + "Error creating tables.");
                        status[0] = false;
                        return;
                    }
                    
                } catch (SQLException e) {
                    info(ChatColor.RED + "Error creating tables.");
                    status[0] = false;
                    return;
                }
                
                // Check new DB
                String databasePath = "plugins/SimpleClaimSystem/claims.db";
                File databaseFile = new File(databasePath);

                if (databaseFile.exists()) {
                	claimInstance.convertLocalToNewLocal();
                }
            }
            claimSettingsInstance.addSetting("database", configC);
            
            // Auto-purge settings
            configC = getConfig().getString("auto-purge");
            claimSettingsInstance.addSetting("auto-purge", configC);
            if (configC.equals("true")) {
                configC = getConfig().getString("auto-purge-checking");
                claimSettingsInstance.addSetting("auto-purge-checking", configC);
                try {
                    int minutes = Integer.parseInt(configC);
                    if (minutes < 1) {
                        info(ChatColor.RED + "'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
                        minutes = 60;
                    }
                    configC = getConfig().getString("auto-purge-time-without-login");
                    claimSettingsInstance.addSetting("auto-purge-time-without-login", configC);
                    claimPurgeInstance = new ClaimPurge(this);
                    claimPurgeInstance.startPurge(minutes, configC);
                } catch (NumberFormatException e) {
                    info(ChatColor.RED + "'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
                    int minutes = 60;
                    configC = getConfig().getString("auto-purge-time-without-login");
                    claimSettingsInstance.addSetting("auto-purge-time-without-login", configC);
                    claimPurgeInstance = new ClaimPurge(this);
                    claimPurgeInstance.startPurge(minutes, configC);
                }
            }
            
            // Aliases settings
            List<String> aliases_claim = getConfig().getStringList("command-aliases.claim");
            List<String> aliases_unclaim = getConfig().getStringList("command-aliases.unclaim");
            List<String> aliases_claims = getConfig().getStringList("command-aliases.claims");
            if (aliases_claim == null || aliases_claim.isEmpty()) {
                info(ChatColor.RED + "'aliases_claim' is missing or empty in config. Using default value.");
                aliases_claim = new ArrayList<>();
                aliases_claim.add("/territory");
            }
            if (aliases_unclaim == null || aliases_unclaim.isEmpty()) {
                info(ChatColor.RED + "'aliases_unclaim' is missing or empty in config. Using default value.");
                aliases_unclaim = new ArrayList<>();
                aliases_unclaim.add("/unterritory");
            }
            if (aliases_claims == null || aliases_claims.isEmpty()) {
                info(ChatColor.RED + "'aliases_claims' is missing or empty in config. Using default value.");
                aliases_claims = new ArrayList<>();
                aliases_claims.add("/territories");
            }
            for(String command : aliases_claim) {
            	claimSettingsInstance.addAliase(command, "/claim");
            }
            for(String command : aliases_unclaim) {
            	claimSettingsInstance.addAliase(command, "/unclaim");
            }
            for(String command : aliases_claims) {
            	claimSettingsInstance.addAliase(command, "/claims");
            }
            
            // Add Dynmap settings
            claimSettingsInstance.addSetting("dynmap-claim-border-color", getConfig().getString("dynmap-settings.claim-border-color"));
            claimSettingsInstance.addSetting("dynmap-claim-fill-color", getConfig().getString("dynmap-settings.claim-fill-color"));
            claimSettingsInstance.addSetting("dynmap-claim-hover-text", getConfig().getString("dynmap-settings.claim-hover-text"));
            
            // Add Bluemap settings
            claimSettingsInstance.addSetting("bluemap-claim-border-color", getConfig().getString("bluemap-settings.claim-border-color"));
            claimSettingsInstance.addSetting("bluemap-claim-fill-color", getConfig().getString("bluemap-settings.claim-fill-color"));
            claimSettingsInstance.addSetting("bluemap-claim-hover-text", getConfig().getString("bluemap-settings.claim-hover-text"));
            
            // Add Pl3xmap settings
            claimSettingsInstance.addSetting("pl3xmap-claim-border-color", getConfig().getString("pl3xmap-settings.claim-border-color"));
            claimSettingsInstance.addSetting("pl3xmap-claim-fill-color", getConfig().getString("pl3xmap-settings.claim-fill-color"));
            claimSettingsInstance.addSetting("pl3xmap-claim-hover-text", getConfig().getString("pl3xmap-settings.claim-hover-text"));
            
            // Add the message type for protection
            configC = getConfig().getString("protection-message");
            if (configC.equalsIgnoreCase("action_bar") || 
                    configC.equalsIgnoreCase("title") ||
                    configC.equalsIgnoreCase("subtitle") ||
                    configC.equalsIgnoreCase("chat") ||
                    configC.equalsIgnoreCase("bossbar")) {
                claimSettingsInstance.addSetting("protection-message", configC);
            } else {
                info(ChatColor.RED + "'protection-message' must be 'ACTION_BAR', 'TITLE', 'SUBTITLE', 'CHAT' or 'BOSSBAR'. Using default value.");
                claimSettingsInstance.addSetting("protection-message", "ACTION_BAR");
            }
            
            // Add disabled worlds
            Set<String> worlds = new HashSet<>(getConfig().getStringList("worlds-disabled"));
            claimSettingsInstance.setDisabledWorlds(worlds);
            
            // Check the preload chunks
            claimSettingsInstance.addSetting("preload-chunks", getConfig().getString("preload-chunks"));
            
            // Check the keep chunks loaded
            claimSettingsInstance.addSetting("keep-chunks-loaded", getConfig().getString("keep-chunks-loaded"));
            
            // Check the max length of the claim name
            claimSettingsInstance.addSetting("max-length-claim-name", getConfig().getString("max-length-claim-name"));
            
            // Check the max length of the claim description
            claimSettingsInstance.addSetting("max-length-claim-description", getConfig().getString("max-length-claim-description"));
            
            // Add confirmation check setting
            claimSettingsInstance.addSetting("claim-confirmation", getConfig().getString("claim-confirmation"));
            
            // Add claim particles setting
            claimSettingsInstance.addSetting("claim-particles", getConfig().getString("claim-particles"));
            
            // Add claim fly disabled on damage setting
            claimSettingsInstance.addSetting("claim-fly-disabled-on-damage", getConfig().getString("claim-fly-disabled-on-damage"));
            
            // Add claim fly message setting
            claimSettingsInstance.addSetting("claim-fly-message-auto-fly", getConfig().getString("claim-fly-message-auto-fly"));
            
            // Check if enter/leave messages in a claim in the action bar are enabled
            claimSettingsInstance.addSetting("enter-leave-messages", getConfig().getString("enter-leave-messages"));
            
            // Check if enter/leave messages in a claim in the title/subtitle are enabled
            claimSettingsInstance.addSetting("enter-leave-title-messages", getConfig().getString("enter-leave-title-messages"));
            
            // Check if enter/leave messages in a claim in the chat are enabled
            claimSettingsInstance.addSetting("enter-leave-chat-messages", getConfig().getString("enter-leave-chat-messages"));
            
            // Check if claims where Visitors is false are displayed in the /claims GUI
            claimSettingsInstance.addSetting("claims-visitors-off-visible", getConfig().getString("claims-visitors-off-visible"));
            
            // Check where the auto-map is sent
            configC = getConfig().getString("map-type").toLowerCase();
            if(configC.equals("scoreboard") && isFolia) {
            	info(ChatColor.RED + "'map-type' set to 'CHAT' because the server is running on Folia.");
            	configC = "chat";
            }
            claimSettingsInstance.addSetting("map-type", configC);
            
            // Add economy settings
            if (check_vault) {
                claimSettingsInstance.addSetting("economy", getConfig().getString("economy"));
                claimSettingsInstance.addSetting("max-sell-price", getConfig().getString("max-sell-price"));
                claimSettingsInstance.addSetting("claim-cost", getConfig().getString("claim-cost"));
                claimSettingsInstance.addSetting("claim-cost-multiplier", getConfig().getString("claim-cost-multiplier"));
            } else {
                claimSettingsInstance.addSetting("economy", "false");
            }
            
            // Add announce sale settings
            claimSettingsInstance.addSetting("announce-sale.bossbar", getConfig().getString("announce-sale.bossbar"));
            String barColor = getConfig().getString("announce-sale.bossbar-settings.color").toUpperCase();
            try {
            	BarColor color = BarColor.valueOf(barColor);
            	if(color == null) {
                    info(ChatColor.RED + "Invalid bossbar color, using default color RED.");
                    barColor = "YELLOW";
            	}
            } catch (IllegalArgumentException e) {
                info(ChatColor.RED + "Invalid bossbar color, using default color RED.");
                barColor = "YELLOW";
            }
            String barStyle = getConfig().getString("announce-sale.bossbar-settings.style").toUpperCase();
            try {
            	BarStyle style = BarStyle.valueOf(barStyle);
            	if(style == null) {
                	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
                	barStyle = "SOLID";
            	}
            } catch (IllegalArgumentException e) {
            	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
            	barStyle = "SOLID";
            }
            claimSettingsInstance.addSetting("announce-sale.bossbar-settings.color", barColor);
            claimSettingsInstance.addSetting("announce-sale.bossbar-settings.style", barStyle);
            claimSettingsInstance.addSetting("announce-sale.chat", getConfig().getString("announce-sale.chat"));
            claimSettingsInstance.addSetting("announce-sale.title", getConfig().getString("announce-sale.title"));
            claimSettingsInstance.addSetting("announce-sale.actionbar", getConfig().getString("announce-sale.actionbar"));
            
            // Add bossbar settings
            configC = getConfig().getString("bossbar");
            claimSettingsInstance.addSetting("bossbar", configC);
            // Load bossbar settings
            barColor = getConfig().getString("bossbar-settings.color").toUpperCase();
            try {
            	BarColor color = BarColor.valueOf(barColor);
            	if(color == null) {
                    info(ChatColor.RED + "Invalid bossbar color, using default color YELLOW.");
                    barColor = "YELLOW";
            	}
            } catch (IllegalArgumentException e) {
                info(ChatColor.RED + "Invalid bossbar color, using default color YELLOW.");
                barColor = "YELLOW";
            }
            barStyle = getConfig().getString("bossbar-settings.style").toUpperCase();
            try {
            	BarStyle style = BarStyle.valueOf(barStyle);
            	if(style == null) {
                    info(ChatColor.RED + "Invalid bossbar color, using default color YELLOW.");
                    barColor = "YELLOW";
            	}
            } catch (IllegalArgumentException e) {
            	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
            	barStyle = "SOLID";
            }
            claimSettingsInstance.addSetting("bossbar-color", barColor);
            claimSettingsInstance.addSetting("bossbar-style", barStyle);
            
            // Add teleportation delay moving setting
            claimSettingsInstance.addSetting("teleportation-delay-moving", getConfig().getString("teleportation-delay-moving"));
            
            // Add group settings
            ConfigurationSection groupsSection = getConfig().getConfigurationSection("groups");
            LinkedHashMap<String, String> groups = new LinkedHashMap<>();
            Map<String, Map<String, Double>> groupsSettings = new HashMap<>();
            for (String key : groupsSection.getKeys(false)) {
                if (!key.equalsIgnoreCase("default")) groups.put(key, getConfig().getString("groups." + key + ".permission"));
                Map<String, Double> settings = new HashMap<>();
                settings.put("max-claims", getConfig().getDouble("groups." + key + ".max-claims"));
                settings.put("max-radius-claims", getConfig().getDouble("groups." + key + ".max-radius-claims"));
                settings.put("teleportation-delay", getConfig().getDouble("groups." + key + ".teleportation-delay"));
                settings.put("max-members", getConfig().getDouble("groups." + key + ".max-members"));
                settings.put("claim-cost", getConfig().getDouble("groups." + key + ".claim-cost"));
                settings.put("claim-cost-multiplier", getConfig().getDouble("groups." + key + ".claim-cost-multiplier"));
                settings.put("max-chunks-per-claim", getConfig().getDouble("groups." + key + ".max-chunks-per-claim"));
                settings.put("claim-distance", getConfig().getDouble("groups." + key + ".claim-distance"));
                settings.put("max-chunks-total", getConfig().getDouble("groups." + key + ".max-chunks-total"));
                groupsSettings.put(key, settings);
            }
            claimSettingsInstance.setGroups(groups);
            claimSettingsInstance.setGroupsSettings(groupsSettings);
            
            // Add player settings
            ConfigurationSection playersSection = getConfig().getConfigurationSection("players");
            Map<UUID, Map<String, Double>> playersSettings = new HashMap<>();
            for (String key : playersSection.getKeys(false)) {
                Map<String, Double> settings = new HashMap<>();
                if (getConfig().isSet("players." + key + ".max-claims")) settings.put("max-claims", getConfig().getDouble("players." + key + ".max-claims"));
                if (getConfig().isSet("players." + key + ".max-radius-claims")) settings.put("max-radius-claims", getConfig().getDouble("players." + key + ".max-radius-claims"));
                if (getConfig().isSet("players." + key + ".teleportation-delay")) settings.put("teleportation-delay", getConfig().getDouble("players." + key + ".teleportation-delay"));
                if (getConfig().isSet("players." + key + ".claim-cost")) settings.put("claim-cost", getConfig().getDouble("players." + key + ".claim-cost"));
                if (getConfig().isSet("players." + key + ".claim-cost-multiplier")) settings.put("claim-cost-multiplier", getConfig().getDouble("players." + key + ".claim-cost-multiplier"));
                if (getConfig().isSet("players." + key + ".max-chunks-per-claim")) settings.put("max-chunks-per-claim", getConfig().getDouble("players." + key + ".max-chunks-per-claim"));
                if (getConfig().isSet("players." + key + ".claim-distance")) settings.put("claim-distance", getConfig().getDouble("players." + key + ".claim-distance"));
                if (getConfig().isSet("players." + key + ".max-chunks-total")) settings.put("max-chunks-total", getConfig().getDouble("players." + key + ".max-chunks-total"));
                if (!settings.isEmpty()) playersSettings.put(Bukkit.getOfflinePlayer(key).getUniqueId(), settings);
            }
            cPlayerMainInstance.setPlayersConfigSettings(playersSettings);
            
            // Add enabled/disabled settings
            v = new LinkedHashMap<>();
            statusSettings = getConfig().getConfigurationSection("status-settings");
            for (String key : statusSettings.getKeys(false)) {
                v.put(key, statusSettings.getBoolean(key));
            }
            claimSettingsInstance.setEnabledSettings(v);
            
            // Add blocked items
            claimSettingsInstance.setRestrictedItems(getConfig().getStringList("blocked-items"));
            
            // Add blocked containers
            claimSettingsInstance.setRestrictedContainers(getConfig().getStringList("blocked-interact-blocks"));
            
            // Add blocked entities
            claimSettingsInstance.setRestrictedEntityType(getConfig().getStringList("blocked-entities"));
            
            // Add special blocks
            claimSettingsInstance.setSpecialBlocks(getConfig().getStringList("special-blocks"));
            
            // Add ignored break blocks
            claimSettingsInstance.setBreakBlocksIgnore(getConfig().getStringList("ignored-break-blocks"));
            
            // Add ignored place blocks
            claimSettingsInstance.setPlaceBlocksIgnore(getConfig().getStringList("ignored-place-blocks"));
            
            // Register protection listener
            getServer().getPluginManager().registerEvents(new ClaimEvents(this), this);

            // Save config
            saveConfig();
            
            // Load bossbar default settings
            claimBossBarInstance.loadBossbarSettings();
            
            // Add players setting and active their bossbar (/reload prevention)
            Bukkit.getOnlinePlayers().forEach(p -> {
            	cPlayerMainInstance.addPlayerPermSetting(p);
            	claimBossBarInstance.activeBossBar(p, p.getLocation().getChunk());
            });

            info("==========================================================================");
            
            if(status[0]) {
            	sender.sendMessage(getLanguage().getMessage("config-reload-complete"));
            } else {
            	sender.sendMessage(getLanguage().getMessage("config-reload-not-complete"));
            }
    	});
        
        return status[0];
    }
    
    /**
     * Returns the data source for database connections.
     * 
     * @return The data source
     */
    public HikariDataSource getDataSource() { return dataSource; }
    
    /**
     * Send a log
     * 
     * @param msg The log to send
     */
    public void info(String msg) {
    	logger.sendMessage(msg);
    }
    
    /**
     * Checks if the server is using Folia.
     * 
     * @return True if the server is using Folia, false otherwise
     */
    public boolean isFolia() { return isFolia; }
    
    /**
     * Returns the update message.
     * 
     * @return The update message
     */
    public String getUpdateMessage() { return updateMessage; }
    
    /**
     * Checks if an update is available for the 
     * 
     * @return True if an update is available, false otherwise
     */
    public boolean isUpdateAvailable() { return isUpdateAvailable; }
    
    /**
     * Executes a task asynchronously.
     * 
     * @param gTask The task to execute
     */
    public void executeAsync(Runnable gTask) {
        if (isFolia) {
            Bukkit.getAsyncScheduler().runNow(this, task -> gTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(this, gTask);
        }
    }
    
    /**
     * Executes a task synchronously.
     * 
     * @param gTask The task to execute
     */
    public void executeSync(Runnable gTask) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(this, () -> gTask.run());
        } else {
            Bukkit.getScheduler().runTask(this, gTask);
        }
    }
    
    /**
     * Executes a task synchronously (for entities).
     * 
     * @param player The target player for who execute the task
     * @param gTask The task to execute
     */
    public void executeEntitySync(Player player, Runnable gTask) {
        if (isFolia) {
        	player.getScheduler().execute(this, gTask, null, 0);
        } else {
            Bukkit.getScheduler().runTask(this, gTask);
        }
    }
    
    /**
     * Checks if the server is using Folia.
     */
    public void checkFolia() {
        if (Bukkit.getVersion().contains("folia")) {
            isFolia = true;
            return;
        }
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServerInitEvent");
            isFolia = true;
        } catch (ClassNotFoundException e) {
            isFolia = false;
        }
    }
    
    /**
     * Reloads the language file.
     * 
     * @param plugin The plugin instance
     * @param sender The command sender
     * @param lang The language file to reload
     */
    public void reloadLang(CommandSender sender, String l) {
		String lang = l;
        
        // Check default language file for additions
        File custom = new File(getDataFolder() + File.separator + "langs", lang);
        final boolean check = !custom.exists() ? true : false;
        if (!custom.exists()) {
            lang = "en_US.yml";
        } else {
            updateLangFileWithMissingKeys(lang);
        }
        claimSettingsInstance.addSetting("lang", lang);
        
        // Load selected language file
        File lang_final = new File(getDataFolder() + File.separator + "langs", lang);
        FileConfiguration config = YamlConfiguration.loadConfiguration(lang_final);
        Map<String, String> messages = new HashMap<>();
        for (String key : config.getKeys(false)) {
            String value = config.getString(key);
            messages.put(key, value);
        }
        claimLanguageInstance.setLanguage(messages);
        
        getConfig().set("lang", lang);
        saveConfig();
        reloadConfig();
        
    	if(check) sender.sendMessage("The file you indicate doesn't exists. Using default file.");
    	sender.sendMessage("Language file loaded §7("+l+"§7)§f.");
    	
    	executeAsync(() -> {
    		Bukkit.getOnlinePlayers().forEach(p -> claimBossBarInstance.activeBossBar(p, p.getLocation().getChunk()));
    	});
    }
    
    /**
     * Updates the language file with missing keys and removes obsolete keys.
     * 
     * @param plugin The plugin instance
     * @param file The language file to update
     */
    private void updateLangFileWithMissingKeys(String file) {
        try {
            InputStream defLangStream = getClass().getClassLoader().getResourceAsStream("langs/en_US.yml");
            if (defLangStream == null) return;
            FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
            File langFile = new File(getDataFolder() + File.separator + "langs", file);
            if (!langFile.exists()) return;
            FileConfiguration customConfig = YamlConfiguration.loadConfiguration(langFile);
            boolean needSave = false;

            // Add missing keys
            for (String key : defConfig.getKeys(true)) {
                if (!customConfig.contains(key)) {
                    customConfig.set(key, defConfig.get(key));
                    needSave = true;
                }
            }

            // Remove obsolete keys
            Set<String> customConfigKeys = new HashSet<>(customConfig.getKeys(true));
            for (String key : customConfigKeys) {
                if (!defConfig.contains(key)) {
                    customConfig.set(key, null);
                    needSave = true;
                }
            }

            if (needSave) customConfig.save(langFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Updates the configuration file with default values, adding missing keys and removing obsolete ones.
     *
     * @param plugin The plugin instance
     */
    public void updateConfigWithDefaults() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveDefaultConfig();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = getResource("config.yml");
        if (defConfigStream == null) return;
        YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream));

        boolean changed = false;

        // Add missing keys
        for (String key : defConfig.getKeys(true)) {
            if (!config.contains(key)) {
                config.set(key, defConfig.get(key));
                changed = true;
            }
        }

        // Remove obsolete keys
        Set<String> configKeys = new HashSet<>(config.getKeys(true));
        for (String key : configKeys) {
            if (!defConfig.contains(key) && !checkKey(key)) {
                config.set(key, null);
                changed = true;
            }
        }
        
        ConfigurationSection defaultGroup = defConfig.getConfigurationSection("groups.default");
        ConfigurationSection groups = defConfig.getConfigurationSection("groups");
        if(addMissingKeysFromDefault(defaultGroup, groups)) changed = true;

        if (changed) {
            try {
                config.save(configFile);
                reloadConfig();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Adds missing keys from the default group to all other groups.
     *
     * @param defaultGroup the ConfigurationSection of the default group
     * @param groups       the ConfigurationSection containing all groups
     */
    private boolean addMissingKeysFromDefault(ConfigurationSection defaultGroup, ConfigurationSection groups) {
    	boolean changed = false;
        for (String groupName : groups.getKeys(false)) {
            if (!groupName.equals("default")) {
                ConfigurationSection group = groups.getConfigurationSection(groupName);
                if (group != null) {
                    changed = addMissingKeys(defaultGroup, group);
                }
            }
        }
        return changed;
    }

    /**
     * Adds missing keys from the default group to the specified group.
     *
     * @param defaultGroup the ConfigurationSection of the default group
     * @param group        the ConfigurationSection of the group to update
     */
    private boolean addMissingKeys(ConfigurationSection defaultGroup, ConfigurationSection group) {
    	boolean changed = false;
        for (String key : defaultGroup.getKeys(false)) {
            if (!group.contains(key)) {
                Object value = defaultGroup.get(key);
                group.set(key, value);
                changed = true;
            }
        }
        return changed;
    }
    
    /**
     * Check the key
     * 
     * @param key The target key
     * @return True if the key must be deleted
     */
    private boolean checkKey(String key) {
    	return key.startsWith("groups.") || key.startsWith("players.");
    }
    
    /**
     * Checks for updates for the 
     * 
     * @param plugin The plugin instance
     * @return True if an update is available, false otherwise
     */
    public String checkForUpdates() {
        try {
            URL url = new URL("https://raw.githubusercontent.com/Xyness/SimpleClaimSystem/main/version.yml");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String response = reader.readLine();
                if (!Version.equalsIgnoreCase(response)) {
                    updateMessage = "§4[SCS] §cUpdate available : §l" + response + " §7(You have "+Version+")";
                    isUpdateAvailable = true;
                    return "§cUpdate available : §4"+response;
                } else {
                    isUpdateAvailable = false;
                    return "You are using the latest version";
                }
            }
        } catch (Exception e) {
            return "Error when checking new update";
        }
    }
    
    /**
     * Checks and saves a resource file if it does not exist.
     * 
     * @param plugin The plugin instance
     * @param resource The resource file to check and save
     */
    private void checkAndSaveResource(String resource) {
        File file = new File(getDataFolder() + File.separator + resource);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            saveResource(resource, false);
        }
    }
    
    /**
     * Returns the SimpleClaimSystem instance.
     * 
     * @return The SimpleClaimSystem instance
     */
    public SimpleClaimSystem getInstance() {
        return instance;
    }
    
    /**
     * Returns the ClaimMain instance.
     * 
     * @return The ClaimMain instance
     */
    public ClaimMain getMain() {
        return claimInstance;
    }
    
    /**
     * Returns the ClaimGuis instance.
     * 
     * @return The ClaimGuis instance
     */
    public ClaimGuis getGuis() {
        return claimGuisInstance;
    }
    
    /**
     * Returns the ClaimSettings instance.
     * 
     * @return The ClaimSettings instance
     */
    public ClaimSettings getSettings() {
        return claimSettingsInstance;
    }
    
    /**
     * Returns the ClaimLanguage instance.
     * 
     * @return The ClaimLanguage instance
     */
    public ClaimLanguage getLanguage() {
        return claimLanguageInstance;
    }
    
    /**
     * Returns the ClaimWorldGuard instance.
     * 
     * @return The ClaimWorldGuard instance
     */
    public ClaimWorldGuard getWorldGuard() {
        return claimWorldguardInstance;
    }
    
    /**
     * Returns the ClaimVault instance.
     * 
     * @return The ClaimVault instance
     */
    public ClaimVault getVault() {
        return claimVaultInstance;
    }
    
    /**
     * Returns the CPlayerMain instance.
     * 
     * @return The CPlayerMain instance
     */
    public CPlayerMain getPlayerMain() {
        return cPlayerMainInstance;
    }
    
    /**
     * Returns the ClaimBossBar instance.
     * 
     * @return The ClaimBossBar instance
     */
    public ClaimBossBar getBossBars() {
        return claimBossBarInstance;
    }
    
    /**
     * Returns the ClaimDynmap instance.
     * 
     * @return The ClaimDynmap instance
     */
    public ClaimDynmap getDynmap() {
        return dynmapInstance;
    }
    
    /**
     * Returns the ClaimBluemap instance.
     * 
     * @return The ClaimBluemap instance
     */
    public ClaimBluemap getBluemap() {
        return bluemapInstance;
    }
    
    /**
     * Returns the ClaimPl3xMap instance.
     * 
     * @return The ClaimPl3xMap instance
     */
    public ClaimPl3xMap getPl3xMap() {
        return pl3xmapInstance;
    }
    
    /**
     * Returns the ClaimPurge instance.
     * 
     * @return The ClaimPurge instance
     */
    public ClaimPurge getAutopurge() {
        return claimPurgeInstance;
    }
}
