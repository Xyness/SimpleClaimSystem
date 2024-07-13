package fr.xyness.SCS;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

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
    
    /** The plugin instance */
    private JavaPlugin plugin;
    
    /** The version of the plugin */
    private String Version = "1.10.0.1";
    
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
    	info("==========================================================================");
        info(ChatColor.AQUA + "  ___    ___   ___ ");
        info(ChatColor.AQUA + " / __|  / __| / __|  " + ChatColor.DARK_GREEN + "SimpleClaimSystem " + ChatColor.AQUA + "v" + Version);
        info(ChatColor.AQUA + " \\__ \\ | (__  \\__ \\  " + ChatColor.GRAY + checkForUpdates());
        info(ChatColor.AQUA + " |___/  \\___| |___/  " + ChatColor.DARK_GRAY + "Running on " + Bukkit.getVersion());
        info(" ");
        plugin = this;
        if (loadConfig( false)) {
            info(" ");
            info("SimpleClaimSystem is enabled !");
            info("Discord for support : https://discord.gg/xyness");
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
        info("Discord for support : https://discord.gg/xyness");
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
    // *  Other Methods  *
    // ********************
    
    
    /**
     * Loads or reloads the plugin configuration.
     * 
     * @param plugin The plugin instance
     * @param reload Whether to reload the configuration
     * @return True if the configuration was loaded successfully, false otherwise
     */
    public boolean loadConfig(boolean reload) {
        if (reload) info("==========================================================================");
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        
        // Unregister all handlers
        if(reload) {
            HandlerList.unregisterAll(plugin);
            claimInstance.clearAll();
            claimGuisInstance.clearAll();
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
        	bStatsInstance.enableMetrics(plugin);
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
            new ClaimPlaceholdersExpansion(this).register();
        } else {
            claimSettingsInstance.addSetting("placeholderapi", "false");
        }
        
        // Check ItemsAdder
        boolean check_itemsadder = false;
        if (Bukkit.getPluginManager().getPlugin("ItemsAdder") != null) {
            check_itemsadder = true;
        }
        
        // Check WorldGuard
        if (Bukkit.getPluginManager().getPlugin("WorldGuard") != null) {
            claimSettingsInstance.addSetting("worldguard", "true");
        } else {
            claimSettingsInstance.addSetting("worldguard", "false");
        }
        
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
        
        // Check "guis" folder
        File dossier = new File(plugin.getDataFolder(), "guis");
        if (!dossier.exists()) {
            dossier.mkdirs();
        }
        
        // Check GUI files
        checkAndSaveResource("guis/main.yml");
        checkAndSaveResource("guis/settings.yml");
        checkAndSaveResource("guis/members.yml");
        checkAndSaveResource("guis/list.yml");
        checkAndSaveResource("guis/claims.yml");
        checkAndSaveResource("guis/claims_owner.yml");
        checkAndSaveResource("guis/bans.yml");
        checkAndSaveResource("guis/chunks.yml");
        claimGuisInstance.loadGuiSettings(plugin, check_itemsadder);

        // Check "langs" folder
        dossier = new File(plugin.getDataFolder(), "langs");
        if (!dossier.exists()) {
            dossier.mkdirs();
        }
        
        // Check default language file for additions
        checkAndSaveResource("langs/en_US.yml");
        updateLangFileWithMissingKeys("en_US.yml");
        
        // Check custom language file
        String lang = plugin.getConfig().getString("lang");
        File custom = new File(plugin.getDataFolder() + File.separator + "langs", lang);
        if (!custom.exists()) {
            info(ChatColor.RED + "File '" + lang + "' not found, using en_US.yml");
            lang = "en_US.yml";
        } else {
            updateLangFileWithMissingKeys(lang);
        }
        claimSettingsInstance.addSetting("lang", lang);
        
        // Load selected language file
        File lang_final = new File(plugin.getDataFolder() + File.separator + "langs", lang);
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
        
        // Check database
        String configC = plugin.getConfig().getString("database");
        if (configC.equalsIgnoreCase("true")) {
            // Create data source
            HikariConfig configH = new HikariConfig();
            configH.setJdbcUrl("jdbc:mysql://" + plugin.getConfig().getString("database-settings.hostname") + ":" + plugin.getConfig().getString("database-settings.port") + "/" + plugin.getConfig().getString("database-settings.database_name"));
            configH.setUsername(plugin.getConfig().getString("database-settings.username"));
            configH.setPassword(plugin.getConfig().getString("database-settings.password"));
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
                    String sql = "CREATE TABLE IF NOT EXISTS scs_claims " +
                            "(id_pk INT AUTO_INCREMENT PRIMARY KEY, " +
                            "id INT, " +
                            "uuid VARCHAR(36), " +
                            "name VARCHAR(36), " +
                            "claim_name VARCHAR(255), " +
                            "claim_description VARCHAR(255), " +
                            "X VARCHAR(1000000000), " +
                            "Z VARCHAR(1000000000), " +
                            "World VARCHAR(255), " +
                            "Location VARCHAR(255), " +
                            "Members VARCHAR(1000000000), " +
                            "Permissions VARCHAR(510), " +
                            "isSale TINYINT(1) DEFAULT 0, " +
                            "SalePrice DOUBLE DEFAULT 0, " +
                            "Bans VARCHAR(1000000000) DEFAULT '')";
                    stmt.executeUpdate(sql);
                    sql = "ALTER TABLE scs_claims MODIFY COLUMN SalePrice DOUBLE;";
                    stmt.executeUpdate(sql);
                    sql = "ALTER TABLE scs_claims MODIFY COLUMN X VARCHAR(1000000000), MODIFY COLUMN Z VARCHAR(1000000000);";
                    stmt.executeUpdate(sql);
                    String checkColumnSQL = String.format(
                            "SELECT COUNT(*) AS column_count FROM information_schema.columns " +
                            "WHERE table_name = '%s' AND column_name = '%s'",
                            "scs_claims", "Bans");
                    ResultSet rs = stmt.executeQuery(checkColumnSQL);
                    if (rs.next() && rs.getInt("column_count") == 0) {
                        sql = "ALTER TABLE scs_claims ADD COLUMN Bans VARCHAR(1020) DEFAULT '';";
                        stmt.executeUpdate(sql);
                    }
                } catch (SQLException e) {
                    info(ChatColor.RED + "Error creating tables, using local db.");
                    configC = "false";
                }
            } catch (SQLException e) {
                info(ChatColor.RED + "Error connecting to database, using local db.");
                configC = "false";
            }
        }
        if (configC.equals("false")) {
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
                            "X VARCHAR(1000000000), " +
                            "Z VARCHAR(1000000000), " +
                            "World VARCHAR(255), " +
                            "Location VARCHAR(255), " +
                            "Members VARCHAR(1000000000), " +
                            "Permissions VARCHAR(510), " +
                            "isSale TINYINT(1) DEFAULT 0, " +
                            "SalePrice DOUBLE DEFAULT 0, " +
                            "Bans VARCHAR(1000000000) DEFAULT '')";
                    stmt.executeUpdate(sql);
                } catch (SQLException e) {
                    info(ChatColor.RED + "Error creating tables, disabling plugin.");
                    return false;
                }
            } catch (SQLException e) {
                info(ChatColor.RED + "Error creating tables, disabling plugin.");
                return false;
            }
        }
        claimSettingsInstance.addSetting("database", configC);
        
        // Auto-purge settings
        configC = plugin.getConfig().getString("auto-purge");
        claimSettingsInstance.addSetting("auto-purge", configC);
        if (configC.equals("true")) {
            configC = plugin.getConfig().getString("auto-purge-checking");
            claimSettingsInstance.addSetting("auto-purge-checking", configC);
            try {
                int minutes = Integer.parseInt(configC);
                if (minutes < 1) {
                    info(ChatColor.RED + "'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
                    minutes = 60;
                }
                configC = plugin.getConfig().getString("auto-purge-time-without-login");
                claimSettingsInstance.addSetting("auto-purge-time-without-login", configC);
                claimPurgeInstance = new ClaimPurge(this);
                claimPurgeInstance.startPurge(minutes, configC);
            } catch (NumberFormatException e) {
                info(ChatColor.RED + "'auto-purge-checking' must be a correct number (integer and > 0). Using default value.");
                int minutes = 60;
                configC = plugin.getConfig().getString("auto-purge-time-without-login");
                claimSettingsInstance.addSetting("auto-purge-time-without-login", configC);
                claimPurgeInstance = new ClaimPurge(this);
                claimPurgeInstance.startPurge(minutes, configC);
            }
        }
        
        // Add Dynmap settings
        configC = plugin.getConfig().getString("dynmap");
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
        claimSettingsInstance.addSetting("dynmap-claim-border-color", plugin.getConfig().getString("dynmap-settings.claim-border-color"));
        claimSettingsInstance.addSetting("dynmap-claim-fill-color", plugin.getConfig().getString("dynmap-settings.claim-fill-color"));
        claimSettingsInstance.addSetting("dynmap-claim-hover-text", plugin.getConfig().getString("dynmap-settings.claim-hover-text"));
        
        // Add Bluemap settings
        configC = plugin.getConfig().getString("bluemap");
        if(configC.equalsIgnoreCase("true") && claimSettingsInstance.getBooleanSetting("bluemap")) {
            BlueMapAPI.onEnable(api -> {
                // Register marker set
                bluemapInstance = new ClaimBluemap(api,this);
            });
        } else {
        	claimSettingsInstance.addSetting("bluemap", "false");
        }
        claimSettingsInstance.addSetting("bluemap-claim-border-color", plugin.getConfig().getString("bluemap-settings.claim-border-color"));
        claimSettingsInstance.addSetting("bluemap-claim-fill-color", plugin.getConfig().getString("bluemap-settings.claim-fill-color"));
        claimSettingsInstance.addSetting("bluemap-claim-hover-text", plugin.getConfig().getString("bluemap-settings.claim-hover-text"));
        
        // Add Pl3xmap settings
        configC = plugin.getConfig().getString("pl3xmap");
        if(configC.equalsIgnoreCase("true") && claimSettingsInstance.getBooleanSetting("pl3xmap")) {
        	if (!reload) pl3xmapInstance = new ClaimPl3xMap(this);
        } else {
        	claimSettingsInstance.addSetting("pl3xmap", "false");
        }
        claimSettingsInstance.addSetting("pl3xmap-claim-border-color", plugin.getConfig().getString("pl3xmap-settings.claim-border-color"));
        claimSettingsInstance.addSetting("pl3xmap-claim-fill-color", plugin.getConfig().getString("pl3xmap-settings.claim-fill-color"));
        claimSettingsInstance.addSetting("pl3xmap-claim-hover-text", plugin.getConfig().getString("pl3xmap-settings.claim-hover-text"));
        
        // Add the message type for protection
        configC = plugin.getConfig().getString("protection-message");
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
        Set<String> worlds = new HashSet<>(plugin.getConfig().getStringList("worlds-disabled"));
        claimSettingsInstance.setDisabledWorlds(worlds);
        
        // Check the preload chunks
        claimSettingsInstance.addSetting("preload-chunks", plugin.getConfig().getString("preload-chunks"));
        
        // Check the keep chunks loaded
        claimSettingsInstance.addSetting("keep-chunks-loaded", plugin.getConfig().getString("keep-chunks-loaded"));
        
        // Check the max length of the claim name
        claimSettingsInstance.addSetting("max-length-claim-name", plugin.getConfig().getString("max-length-claim-name"));
        
        // Check the max length of the claim description
        claimSettingsInstance.addSetting("max-length-claim-description", plugin.getConfig().getString("max-length-claim-description"));
        
        // Add confirmation check setting
        claimSettingsInstance.addSetting("claim-confirmation", plugin.getConfig().getString("claim-confirmation"));
        
        // Add claim particles setting
        claimSettingsInstance.addSetting("claim-particles", plugin.getConfig().getString("claim-particles"));
        
        // Add claim fly disabled on damage setting
        claimSettingsInstance.addSetting("claim-fly-disabled-on-damage", plugin.getConfig().getString("claim-fly-disabled-on-damage"));
        
        // Add claim fly message setting
        claimSettingsInstance.addSetting("claim-fly-message-auto-fly", plugin.getConfig().getString("claim-fly-message-auto-fly"));
        
        // Check if enter/leave messages in a claim in the action bar are enabled
        claimSettingsInstance.addSetting("enter-leave-messages", plugin.getConfig().getString("enter-leave-messages"));
        
        // Check if enter/leave messages in a claim in the title/subtitle are enabled
        claimSettingsInstance.addSetting("enter-leave-title-messages", plugin.getConfig().getString("enter-leave-title-messages"));
        
        // Check if enter/leave messages in a claim in the chat are enabled
        claimSettingsInstance.addSetting("enter-leave-chat-messages", plugin.getConfig().getString("enter-leave-chat-messages"));
        
        // Check if claims where Visitors is false are displayed in the /claims GUI
        claimSettingsInstance.addSetting("claims-visitors-off-visible", plugin.getConfig().getString("claims-visitors-off-visible"));
        
        // Add economy settings
        if (check_vault) {
            claimSettingsInstance.addSetting("economy", plugin.getConfig().getString("economy"));
            claimSettingsInstance.addSetting("max-sell-price", plugin.getConfig().getString("max-sell-price"));
            claimSettingsInstance.addSetting("claim-cost", plugin.getConfig().getString("claim-cost"));
            claimSettingsInstance.addSetting("claim-cost-multiplier", plugin.getConfig().getString("claim-cost-multiplier"));
        } else {
            claimSettingsInstance.addSetting("economy", "false");
        }
        
        // Add bossbar settings
        configC = plugin.getConfig().getString("bossbar");
        claimSettingsInstance.addSetting("bossbar", configC);
        // Load bossbar settings
        String barColor = getConfig().getString("bossbar-settings.color").toUpperCase();
        try {
        	BarColor color = BarColor.valueOf(barColor);
        } catch (IllegalArgumentException e) {
            info(ChatColor.RED + "Invalid bossbar color, using default color YELLOW.");
            barColor = "YELLOW";
        }
        String barStyle = plugin.getConfig().getString("bossbar-settings.style").toUpperCase();
        try {
        	BarStyle style = BarStyle.valueOf(barStyle);
        } catch (IllegalArgumentException e) {
        	info(ChatColor.RED + "Invalid bossbar style, using default style SOLID.");
        	barStyle = "SOLID";
        }
        claimSettingsInstance.addSetting("bossbar-color", barColor);
        claimSettingsInstance.addSetting("bossbar-style", barStyle);
        
        // Add teleportation delay moving setting
        claimSettingsInstance.addSetting("teleportation-delay-moving", plugin.getConfig().getString("teleportation-delay-moving"));
        
        // Add group settings
        ConfigurationSection groupsSection = plugin.getConfig().getConfigurationSection("groups");
        LinkedHashMap<String, String> groups = new LinkedHashMap<>();
        Map<String, Map<String, Double>> groupsSettings = new HashMap<>();
        for (String key : groupsSection.getKeys(false)) {
            if (!key.equalsIgnoreCase("default")) groups.put(key, plugin.getConfig().getString("groups." + key + ".permission"));
            Map<String, Double> settings = new HashMap<>();
            settings.put("max-claims", plugin.getConfig().getDouble("groups." + key + ".max-claims"));
            settings.put("max-radius-claims", plugin.getConfig().getDouble("groups." + key + ".max-radius-claims"));
            settings.put("teleportation-delay", plugin.getConfig().getDouble("groups." + key + ".teleportation-delay"));
            settings.put("max-members", plugin.getConfig().getDouble("groups." + key + ".max-members"));
            settings.put("claim-cost", plugin.getConfig().getDouble("groups." + key + ".claim-cost"));
            settings.put("claim-cost-multiplier", plugin.getConfig().getDouble("groups." + key + ".claim-cost-multiplier"));
            settings.put("max-chunks-per-claim", plugin.getConfig().getDouble("groups." + key + ".max-chunks-per-claim"));
            groupsSettings.put(key, settings);
        }
        claimSettingsInstance.setGroups(groups);
        claimSettingsInstance.setGroupsSettings(groupsSettings);
        
        // Add player settings
        ConfigurationSection playersSection = plugin.getConfig().getConfigurationSection("players");
        Map<String, Map<String, Double>> playersSettings = new HashMap<>();
        for (String key : playersSection.getKeys(false)) {
            Map<String, Double> settings = new HashMap<>();
            if (plugin.getConfig().isSet("players." + key + ".max-claims")) settings.put("max-claims", plugin.getConfig().getDouble("players." + key + ".max-claims"));
            if (plugin.getConfig().isSet("players." + key + ".max-radius-claims")) settings.put("max-radius-claims", plugin.getConfig().getDouble("players." + key + ".max-radius-claims"));
            if (plugin.getConfig().isSet("players." + key + ".teleportation-delay")) settings.put("teleportation-delay", plugin.getConfig().getDouble("players." + key + ".teleportation-delay"));
            if (plugin.getConfig().isSet("players." + key + ".claim-cost")) settings.put("claim-cost", plugin.getConfig().getDouble("players." + key + ".claim-cost"));
            if (plugin.getConfig().isSet("players." + key + ".claim-cost-multiplier")) settings.put("claim-cost-multiplier", plugin.getConfig().getDouble("players." + key + ".claim-cost-multiplier"));
            if (plugin.getConfig().isSet("players." + key + ".max-chunks-per-claim")) settings.put("max-chunks-per-claim", plugin.getConfig().getDouble("players." + key + ".max-chunks-per-claim"));
            if (!settings.isEmpty()) playersSettings.put(key, settings);
        }
        cPlayerMainInstance.setPlayersConfigSettings(playersSettings);
        
        // Register listener for entering/leaving claims
        plugin.getServer().getPluginManager().registerEvents(new ClaimEventsEnterLeave(this), plugin);
        
        // Register listener for guis
        plugin.getServer().getPluginManager().registerEvents(new ClaimGuiEvents(this), plugin);
        
        // Add enabled/disabled settings
        LinkedHashMap<String, Boolean> v = new LinkedHashMap<>();
        ConfigurationSection statusSettings = plugin.getConfig().getConfigurationSection("status-settings");
        for (String key : statusSettings.getKeys(false)) {
            v.put(key, statusSettings.getBoolean(key));
        }
        claimSettingsInstance.setEnabledSettings(v);
        
        // Add default settings
        v = new LinkedHashMap<>();
        statusSettings = plugin.getConfig().getConfigurationSection("default-values-settings");
        for (String key : statusSettings.getKeys(false)) {
            v.put(key, statusSettings.getBoolean(key));
        }
        claimSettingsInstance.setDefaultValues(v);
        
        // Add blocked items
        claimSettingsInstance.setRestrictedItems(plugin.getConfig().getStringList("blocked-items"));
        
        // Add blocked containers
        claimSettingsInstance.setRestrictedContainers(plugin.getConfig().getStringList("blocked-interact-blocks"));
        
        // Add blocked entities
        claimSettingsInstance.setRestrictedEntityType(plugin.getConfig().getStringList("blocked-entities"));
        
        // Register protection listener
        plugin.getServer().getPluginManager().registerEvents(new ClaimEvents(this), plugin);
        
        // Register commands
        plugin.getCommand("claim").setExecutor(new ClaimCommand(this));
        plugin.getCommand("unclaim").setExecutor(new UnclaimCommand(this));
        plugin.getCommand("scs").setExecutor(new ScsCommand(this));
        plugin.getCommand("claims").setExecutor(new ClaimsCommand(this));
        plugin.getCommand("protectedarea").setExecutor(new ProtectedAreaCommand(this));
        
        plugin.saveConfig();

        // Load claims system
        claimInstance.loadClaims();
        
        // Add players setting and active their bossbar (/reload prevention)
        Bukkit.getOnlinePlayers().forEach(p -> {
        	cPlayerMainInstance.addPlayerPermSetting(p);
        	claimBossBarInstance.activeBossBar(p, p.getLocation().getChunk());
        });
        if(reload) info("==========================================================================");
        return true;
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
     * Checks if an update is available for the plugin.
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
            Bukkit.getAsyncScheduler().runNow(plugin, task -> gTask.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(plugin, gTask);
        }
    }
    
    /**
     * Executes a task synchronously.
     * 
     * @param gTask The task to execute
     */
    public void executeSync(Runnable gTask) {
        if (isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(plugin, () -> gTask.run());
        } else {
            Bukkit.getScheduler().runTask(plugin, gTask);
        }
    }
    
    /**
     * Executes a task synchronously (for entities).
     * 
     * @param gTask The task to execute
     */
    public void executeEntitySync(Player player, Runnable gTask) {
        if (isFolia) {
        	player.getScheduler().execute(plugin, gTask, null, 0);
        } else {
            Bukkit.getScheduler().runTask(plugin, gTask);
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
        File custom = new File(plugin.getDataFolder() + File.separator + "langs", lang);
        final boolean check = !custom.exists() ? true : false;
        if (!custom.exists()) {
            lang = "en_US.yml";
        } else {
            updateLangFileWithMissingKeys(lang);
        }
        claimSettingsInstance.addSetting("lang", lang);
        
        // Load selected language file
        File lang_final = new File(plugin.getDataFolder() + File.separator + "langs", lang);
        FileConfiguration config = YamlConfiguration.loadConfiguration(lang_final);
        Map<String, String> messages = new HashMap<>();
        for (String key : config.getKeys(false)) {
            String value = config.getString(key);
            messages.put(key, value);
        }
        claimLanguageInstance.setLanguage(messages);
        
        plugin.getConfig().set("lang", lang);
        plugin.saveConfig();
        plugin.reloadConfig();
        
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
            InputStream defLangStream = plugin.getClass().getClassLoader().getResourceAsStream("langs/en_US.yml");
            if (defLangStream == null) return;
            FileConfiguration defConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(defLangStream, StandardCharsets.UTF_8));
            File langFile = new File(plugin.getDataFolder() + File.separator + "langs", file);
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
        File configFile = new File(plugin.getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            plugin.saveDefaultConfig();
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        InputStream defConfigStream = plugin.getResource("config.yml");
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

        if (changed) {
            try {
                config.save(configFile);
                plugin.reloadConfig();
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
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
     * Checks for updates for the plugin.
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
                    updateMessage = "§b[SCS] §dA new update is available : §b" + response + " §c(You have "+Version+")";
                    isUpdateAvailable = true;
                    return "Update available : "+response;
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
        File file = new File(plugin.getDataFolder() + File.separator + resource);
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource(resource, false);
        }
    }
    
    /**
     * Returns the plugin instance.
     * 
     * @return The plugin instance
     */
    public JavaPlugin getPlugin() {
        return plugin;
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
