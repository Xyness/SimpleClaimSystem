package fr.xyness.SCS;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import fr.xyness.SCS.Config.ClaimLanguage;
import fr.xyness.SCS.Config.ClaimSettings;
import fr.xyness.SCS.Listeners.ClaimEventsEnterLeave;
import fr.xyness.SCS.Support.ClaimBluemap;
import fr.xyness.SCS.Support.ClaimDynmap;
import fr.xyness.SCS.Support.ClaimVault;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;

public class ClaimMain {
	
	
	// ***************
	// *  Variables  *
	// ***************

	
	// Claims
	private static Map<Chunk, Claim> listClaims = new HashMap<>();
	private static Map<String,Map<Chunk,String>> claimsId = new HashMap<>();
    
    private final static Map<Player, Location> playerLocations = new HashMap<>();
    private static Set<String> commandArgs = Set.of("add","autoclaim","automap","list","map","members","remove","see","setdesc","setname","setspawn","settings","tp","chat","ban","unban","bans","owner");
    
    
	// ********************
	// *  Others Methods  *
	// ********************
    
    
    // Method to clear all maps and variables
    public static void clearAll() {
    	claimsId.clear();
    	playerLocations.clear();
    	listClaims.clear();
    }
    
    // Method to send action bar message to a player
    public static void sendActionBar(Player player, String message) {
    	player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
    }
    
    
	// ********************
	// *  CLAIMS Methods  *
	// ********************
    
    
    // Get claim from chunk
    public static Claim getClaimFromChunk(Chunk chunk) {
    	if(listClaims.containsKey(chunk)) {
    		return listClaims.get(chunk);
    	}
    	return null;
    }
    
    // Get player's claims count from his name
    public static int getPlayerClaimsCount(String playerName) {
    	return getChunksFromOwner(playerName).size();
    }
    
    // Get the chunk by the admin claim name
    public static Chunk getAdminChunkByName(String name) {
    	for(Chunk c : listClaims.keySet()) {
    		Claim claim = listClaims.get(c);
    		if(claim.getName().equals(name) && claim.getOwner().equals("admin")) {
    			return c;
    		}
    	}
    	return null;
    }
    
    // Find a free id for a new claim
    public static int findFreeId(String target) {
    	if(!claimsId.containsKey(target)) return 0;
    	int nextKey = 0;
    	Set<String> keys = new HashSet<>(claimsId.get(target).values());
	    if (keys.isEmpty()) return 0;
        OptionalInt maxKey = keys.stream()
                                 .mapToInt(Integer::parseInt)
                                 .max();          
        if (maxKey.isPresent()) return maxKey.getAsInt() + 1;
        return nextKey;
    }
    
    // Get all the owners of the server
    public static Set<String> getClaimsOwners(){
        return listClaims.values().stream()
        		.filter(entry -> !entry.getOwner().equals("admin"))
                .map(Claim::getOwner)
                .collect(Collectors.toSet());
    }
    
    // Get chunks from owner
    public static Set<Chunk> getChunksFromOwner(String owner){
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    public static Map<String, Integer> getPlayerlistClaimsCount(Set<String> players) {
        Map<String, Integer> playerClaimsCount = players.stream()
                .collect(Collectors.toMap(player -> player, player -> 0));
        for (Map.Entry<Chunk, Claim> entry : listClaims.entrySet()) {
            String owner = entry.getValue().getOwner();
            if (playerClaimsCount.containsKey(owner)) {
                playerClaimsCount.put(owner, playerClaimsCount.get(owner) + 1);
            }
        }
        return playerClaimsCount;
    }
    
    // Get chunks in sale from owner
    public static Set<Chunk> getChunksinSaleFromOwner(String owner){
        return listClaims.entrySet().stream()
                .filter(entry -> {
                	return entry.getValue().getOwner().equals(owner) && entry.getValue().getSale();
                })
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }
    
    // Get all chunks of the server (claimed chunks)
    public static Set<Chunk> getAllClaimsChunk(){
    	return listClaims.keySet();
    }
    
    // Get all the members reunited
    public static Set<String> getAllMembersOfAllPlayerClaim(String owner) {
        return listClaims.values().stream()
                .filter(claim -> claim.getOwner().equals(owner))
                .flatMap(claim -> claim.getMembers().stream())
                .collect(Collectors.toSet());
    }
    
    // Get all chunks of the server (claimed chunks) where owner is online
    public static Set<Chunk> getAllClaimsChunkOwnerOnline(){
        return listClaims.values().stream()
                .filter(entry -> {
                    Player owner = Bukkit.getPlayer(entry.getOwner());
                    return owner != null && owner.isOnline();
                })
                .map(entry -> entry.getChunk())
                .collect(Collectors.toSet());
    }
    
    // Get all chunks of the server (claimed chunks) where owner is offline
    public static Set<Chunk> getAllClaimsChunkOwnerOffline(){
        return listClaims.values().stream()
                .filter(entry -> {
                    Player owner = Bukkit.getPlayer(entry.getOwner());
                    return owner == null;
                })
                .map(entry -> entry.getChunk())
                .collect(Collectors.toSet());
    }
    
    // Get all the online owners of the server
    public static Set<String> getClaimsOnlineOwners(){
        return listClaims.values().stream()
                .filter(entry -> {
                    Player owner = Bukkit.getPlayer(entry.getOwner());
                    return owner != null && owner.isOnline() && !entry.getOwner().equals("admin");
                })
                .map(Claim::getOwner)
                .collect(Collectors.toSet());
    }
    
    // Get all the offline owners of the server
    public static Set<String> getClaimsOfflineOwners(){
        return listClaims.values().stream()
                .filter(entry -> {
                    Player owner = Bukkit.getPlayer(entry.getOwner());
                    return owner == null && !entry.getOwner().equals("admin");
                })
                .map(Claim::getOwner)
                .collect(Collectors.toSet());
    }
    
    // Get all the owners with claims in sale
    public static Set<String> getClaimsOwnersWithSales(){
        return listClaims.values().stream()
                .filter(entry -> {
                    return entry.getSale() && !entry.getOwner().equals("admin");
                })
                .map(Claim::getOwner)
                .collect(Collectors.toSet());
    }
    
    // Get the chunk from the claim name
    public static Chunk getChunkByClaimName(String playerName, String name) {
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(playerName) && entry.getValue().getName().equals(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }
    
    // Get the claims chunk where the player is not owner but member
    public static Set<Chunk> getChunksWhereMemberNotOwner(String playerName){
    	return listClaims.entrySet().stream()
    			.filter(entry -> !entry.getValue().getOwner().equals(playerName) && entry.getValue().getMembers().contains(playerName))
    			.map(entry -> entry.getValue().getChunk())
    			.collect(Collectors.toSet());
    }
    
    // Get the claims name from owner
    public static Set<String> getClaimsNameFromOwner(String owner){
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner))
                .map(entry -> entry.getValue().getName())
                .collect(Collectors.toSet());
    }
    
    // Get the claims in sale from owner
    public static Set<String> getClaimsNameInSaleFromOwner(String owner){
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getOwner().equals(owner) && entry.getValue().getSale())
                .map(entry -> entry.getValue().getName())
                .collect(Collectors.toSet());
    }
    
    // Get the chunks of the claims in sale
    public static Set<Chunk> getClaimsChunkInSale(){
        return listClaims.entrySet().stream()
                .filter(entry -> entry.getValue().getSale())
                .map(entry -> entry.getValue().getChunk())
                .collect(Collectors.toSet());
    }
    
    // Get all the claims where the player is for the claim chat mode
    public static List<String> getAllMembersWithPlayerParallel(String playerName) {
        Set<String> allMembers = listClaims.values().parallelStream()
                .filter(claim -> claim.getMembers().contains(playerName))
                .flatMap(claim -> claim.getMembers().stream())
                .filter(member -> !member.equals(playerName))
                .collect(Collectors.toSet());
        return new ArrayList<>(allMembers);
    }
    
    // Teleport to a claim
    public static void goClaim(Player player, Location loc) {
        if(loc == null) return;
        CPlayer cPlayer = CPlayerMain.getCPlayer(player.getName());
        int delay = cPlayer.getDelay();
        
    	if (player.hasPermission("scs.bypass") || delay == 0) {
    		if(SimpleClaimSystem.isFolia()) {
    			player.teleportAsync(loc);
    		} else {
    			player.teleport(loc);
    		}
    		player.sendMessage(ClaimLanguage.getMessage("teleportation-success"));
            return;
    	}
    	
    	player.sendMessage(ClaimLanguage.getMessage("teleportation-in-progress").replaceAll("%delay%", String.valueOf(delay)));
        Location originalLocation = player.getLocation().clone();
        playerLocations.put(player, originalLocation);
        
        if(SimpleClaimSystem.isFolia()) {
    	    final int[] counter = {delay*2};
    	    Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
    	    	if (!player.isOnline() || !playerLocations.containsKey(player)) {
                    task.cancel();
                    return;
                }

                if(!ClaimSettings.getBooleanSetting("teleportation-delay-moving")) {
	                Location currentLocation = player.getLocation();
	                if (!currentLocation.equals(originalLocation) && 
	                    (currentLocation.getX() != originalLocation.getX() || 
	                     currentLocation.getY() != originalLocation.getY() || 
	                     currentLocation.getZ() != originalLocation.getZ())) {
	                	player.sendMessage(ClaimLanguage.getMessage("teleportation-canceled-moving"));
	                    playerLocations.remove(player);
	                    task.cancel();
	                    return;
	                }
                }

                if (counter[0] <= 0) {
                	player.teleportAsync(loc);
                    player.sendMessage(ClaimLanguage.getMessage("teleportation-success"));
                    playerLocations.remove(player);
                    task.cancel();
                } else {
                    counter[0]--;
                }
    	    }, 0, 500, TimeUnit.MILLISECONDS);
    	    return;
        } else {
        	new BukkitRunnable() {
                int countdown = delay*2;

                public void run() {
                    if (!player.isOnline() || !playerLocations.containsKey(player)) {
                        this.cancel();
                        return;
                    }

                    if(!ClaimSettings.getBooleanSetting("teleportation-delay-moving")) {
    	                Location currentLocation = player.getLocation();
    	                if (!currentLocation.equals(originalLocation) && 
    	                    (currentLocation.getX() != originalLocation.getX() || 
    	                     currentLocation.getY() != originalLocation.getY() || 
    	                     currentLocation.getZ() != originalLocation.getZ())) {
    	                	player.sendMessage(ClaimLanguage.getMessage("teleportation-canceled-moving"));
    	                    playerLocations.remove(player);
    	                    this.cancel();
    	                    return;
    	                }
                    }

                    if (countdown <= 0) {
                        player.teleport(loc);
                        player.sendMessage(ClaimLanguage.getMessage("teleportation-success"));
                        playerLocations.remove(player);
                        this.cancel();
                    } else {
                        countdown--;
                    }
                }
            }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 10L);
        }
    }
    
    // Check if the given claim name is already used
    public static boolean checkName(String owner, String name) {
        return listClaims.values().stream()
                .noneMatch(claim -> claim.getName().equals(name) && claim.getOwner().equals(owner));
    }
    
    // Check if a claim is in sale
    public static boolean claimIsInSale(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	return claim.getSale();
    }
    
    // Get the claim description by the chunk
    public static String getClaimDescription(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return "";
    	Claim claim = listClaims.get(chunk);
    	return claim.getDescription();
    }
    
    // Get the claim price by the chunk
    public static Double getClaimPrice(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return 0.0;
    	Claim claim = listClaims.get(chunk);
    	return claim.getPrice();
    }
    
    // Get the claim name by the chunk
    public static String getClaimNameByChunk(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return "";
    	Claim claim = listClaims.get(chunk);
    	return claim.getName();
    }
    
    // Get the claim location by the chunk
    public static Location getClaimLocationByChunk(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return null;
    	Claim claim = listClaims.get(chunk);
    	return claim.getLocation();
    }
    
    // Get the claim string coords by the chunk
    public static String getClaimCoords(Chunk chunk) {
    	if(listClaims.containsKey(chunk)) {
    		Claim claim = listClaims.get(chunk);
    		Location loc = claim.getLocation();
    		String world = loc.getWorld().getName();
    		String x = String.valueOf(Math.round(loc.getX() * 10.0 / 10.0));
    		String y = String.valueOf(Math.round(loc.getY() * 10.0 / 10.0));
    		String z = String.valueOf(Math.round(loc.getZ() * 10.0 / 10.0));
    		return world+", "+x+", "+y+", "+z;
    	}
    	return "";
    }
    
    // Get the center X of a chunk
    public static int getChunkCenterX(Chunk chunk) {
        int centerX = (chunk.getX() << 4) + 8;
        return centerX;
    }
    
    // Get the center Y of a chunk
    public static int getChunkCenterY(Chunk chunk) {
        World world = chunk.getWorld();
        int centerX = (chunk.getX() << 4) + 8;
        int centerZ = (chunk.getZ() << 4) + 8;
        int highestY = world.getHighestBlockYAt(centerX, centerZ)+1;
        return highestY;
    }
    
    // Get the center Z of a chunk
    public static int getChunkCenterZ(Chunk chunk) {
        int centerZ = (chunk.getZ() << 4) + 8;
        return centerZ;
    }
    
    // Method to transfer local db to distant db
    public static void transferClaims() {
        HikariConfig localConfig = new HikariConfig();
        localConfig.setJdbcUrl("jdbc:sqlite:SimpleClaimSystem.getInstance()s/SimpleClaimSystem/claims.db");
        localConfig.setDriverClassName("org.sqlite.JDBC");
        HikariDataSource localDataSource = new HikariDataSource(localConfig);
        
        try (Connection localConn = localDataSource.getConnection()) {
	        PreparedStatement selectStmt = localConn.prepareStatement("SELECT * FROM scs_claims");
	        ResultSet rs = selectStmt.executeQuery();
	        Connection connection = SimpleClaimSystem.getDataSource().getConnection();
	        String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions, isSale, SalePrice) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	        PreparedStatement preparedStatement = connection.prepareStatement(insertQuery);
	        int i = 0;
	        while (rs.next()) {
	        	i++;
            	preparedStatement.setString(1, rs.getString("id"));
            	preparedStatement.setString(2, rs.getString("uuid"));
            	preparedStatement.setString(3, rs.getString("name"));
                preparedStatement.setString(4, rs.getString("claim_name"));
                preparedStatement.setString(5, rs.getString("claim_description"));
                preparedStatement.setString(6, rs.getString("X"));
                preparedStatement.setString(7, rs.getString("Z"));
                preparedStatement.setString(8, rs.getString("World"));
                preparedStatement.setString(9, rs.getString("Location"));
                preparedStatement.setString(10, rs.getString("Members"));
                preparedStatement.setString(11, rs.getString("Permissions"));
                preparedStatement.setBoolean(12, rs.getBoolean("isSale"));
                preparedStatement.setDouble(13, rs.getDouble("SalePrice"));
                preparedStatement.addBatch();
	        }
	        preparedStatement.executeBatch();
    		SimpleClaimSystem.getInstance().getLogger().info(""+String.valueOf(i)+" claims transfered");
    		SimpleClaimSystem.getInstance().getLogger().info("Safe reloading..");
    		Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "aclaim reload");
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        if (localDataSource != null && !localDataSource.isClosed()) {
            localDataSource.close();
        }
    }
    
    // Method to convert claims (from flat files to database)
    public static void convertClaims() {
        File playerDataFolder = new File(SimpleClaimSystem.getInstance().getDataFolder(), "claims");
        if (playerDataFolder.exists()) {
            File[] files = playerDataFolder.listFiles();
            if (files == null) {
                SimpleClaimSystem.getInstance().getLogger().info("0 claims converted");
                return;
            }

            World world;
            Chunk chunk;
            int i = 0;
            try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions, isSale, SalePrice) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                    for (File file : files) {
                        if (file.isFile() && file.getName().endsWith(".yml")) {
                            FileConfiguration playerConfig = YamlConfiguration.loadConfiguration(file);
                            String owner = playerConfig.getString("a_name");
                            String uuid = "";
                            if (file.getName().contains("admin.yml")) {
                                uuid = "aucun";
                                owner = "admin";
                            } else {
                                OfflinePlayer op = Bukkit.getOfflinePlayer(owner);
                                uuid = op.getUniqueId().toString();
                            }
                            String final_uuid = uuid;
                            String owner_final = owner;
                            for (String key : playerConfig.getConfigurationSection("claims").getKeys(false)) {
                                if (!playerConfig.isSet("claims." + key + ".X")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Z")) continue;
                                if (!playerConfig.isSet("claims." + key + ".World")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Location.Yaw")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Location.Pitch")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Location.X")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Location.Y")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Location.Z")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Name")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Description")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Members")) continue;
                                if (!playerConfig.isSet("claims." + key + ".Permissions")) continue;

                                int x = playerConfig.getInt("claims." + key + ".X");
                                int z = playerConfig.getInt("claims." + key + ".Z");
                                world = Bukkit.getServer().getWorld(playerConfig.getString("claims." + key + ".World"));
                                chunk = world.getChunkAt(x, z);

                                double loc_x = playerConfig.getDouble("claims." + key + ".Location.X");
                                double loc_y = playerConfig.getDouble("claims." + key + ".Location.Y");
                                double loc_z = playerConfig.getDouble("claims." + key + ".Location.Z");
                                float yaw = (float) playerConfig.getDouble("claims." + key + ".Location.Yaw");
                                float pitch = (float) playerConfig.getDouble("claims." + key + ".Location.Pitch");
                                Location spawn_loc = new Location(world, loc_x, loc_y, loc_z, yaw, pitch);
                                String Location = spawn_loc.getX() + ";" + spawn_loc.getY() + ";" + spawn_loc.getZ() + ";" + spawn_loc.getYaw() + ";" + spawn_loc.getPitch();
                                String name = playerConfig.getString("claims." + key + ".Name");
                                String description = playerConfig.getString("claims." + key + ".Description");
                                String members_final = playerConfig.getString("claims." + key + ".Members");
                                String Permissions = playerConfig.getString("claims." + key + ".Permissions");
                                boolean isSale = false;
                                if (playerConfig.isSet("claims." + key + ".isSale")) {
                                    isSale = playerConfig.getBoolean("claims." + key + ".isSale");
                                }
                                double saleprice = 0;
                                if (playerConfig.isSet("claims." + key + ".SalePrice")) {
                                    saleprice = playerConfig.getDouble("claims." + key + ".SalePrice");
                                }

                                preparedStatement.setInt(1, Integer.parseInt(key)); // Assuming key is a string representing an integer
                                preparedStatement.setString(2, final_uuid);
                                preparedStatement.setString(3, owner_final);
                                preparedStatement.setString(4, name);
                                preparedStatement.setString(5, description);
                                preparedStatement.setInt(6, x);
                                preparedStatement.setInt(7, z);
                                preparedStatement.setString(8, playerConfig.getString("claims." + key + ".World"));
                                preparedStatement.setString(9, Location);
                                preparedStatement.setString(10, members_final);
                                preparedStatement.setString(11, Permissions);
                                preparedStatement.setBoolean(12, isSale);
                                preparedStatement.setDouble(13, saleprice);
                                preparedStatement.executeUpdate();
                                i++;
                            }
                            try {
                                playerConfig.save(file);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                SimpleClaimSystem.getInstance().getLogger().info(i + " claims converted");
                // Delete the claims folder after conversion
                if (deleteFolder(playerDataFolder)) {
                    SimpleClaimSystem.getInstance().getLogger().info("Old claims folder deleted successfully.");
                } else {
                    SimpleClaimSystem.getInstance().getLogger().info("Failed to delete old claims folder.");
                }
            } catch (SQLException e1) {
				e1.printStackTrace();
			}
        }
    }
    
    // Delete a folder
    public static boolean deleteFolder(File folder) {
        File[] files = folder.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteFolder(file);
                } else {
                    file.delete();
                }
            }
        }
        return folder.delete();
    }
    
    // Method to load claims
    public static void loadClaims() {
    	
    	if(!ClaimSettings.getBooleanSetting("database")) {
        	File playerDataFolder = new File(SimpleClaimSystem.getInstance().getDataFolder(), "claims");
        	if(playerDataFolder.exists()) {
        		convertClaims();
        	}
    	}
    	
        StringBuilder sb = new StringBuilder();
        for(String key : ClaimSettings.getDefaultValues().keySet()) {
        	if(ClaimSettings.getDefaultValues().get(key)) {
        		sb.append("1");
        		continue;
        	}
        	sb.append("0");
        }
        ClaimSettings.setDefaultValuesCode(sb.toString());
        
        // Checking perms (for update or new features)
        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
            connection.setAutoCommit(false); // Start transaction
            String insertQuery = "UPDATE scs_claims SET Permissions = ? WHERE id_pk = ?";
            try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
                String getQuery = "SELECT * FROM scs_claims";
                try (PreparedStatement stat = connection.prepareStatement(getQuery);
                    ResultSet resultSet = stat.executeQuery()) {

                    int batchCount = 0;

                    while (resultSet.next()) {
                        String perms = resultSet.getString("Permissions");
                        int id = resultSet.getInt("id_pk");
                        if (perms.contains(":") || perms.contains(";")) {
                            Map<String, Boolean> settings = new HashMap<>();
                            String[] parts = perms.split(";");
                            for (String keys : parts) {
                                String[] values = keys.split(":");
                                settings.put(values[0], Boolean.parseBoolean(values[1]));
                            }
                            sb = new StringBuilder();
                            for (String perm_key : ClaimSettings.getDefaultValues().keySet()) {
                                sb.append(settings.getOrDefault(perm_key, false) ? "1" : "0");
                            }
                            preparedStatement.setString(1, sb.toString());
                            preparedStatement.setInt(2, id);
                            preparedStatement.addBatch();
                            batchCount++;
                        } else {
                            String perm = perms;
                            if (perm.length() != ClaimSettings.getDefaultValuesCode().length()) {
                                int diff = ClaimSettings.getDefaultValuesCode().length() - perm.length();
                                if(diff < 0) {
                                	StringBuilder permCompleted = new StringBuilder(perm);
                                	for(int i = 0; i < perm.length()-diff; i++) {
                                		permCompleted.append(ClaimSettings.getDefaultValuesCode().charAt(perm.length() + i));
                                	}
                                    String permFinal = permCompleted.toString();
                                    preparedStatement.setString(1, permFinal);
                                    preparedStatement.setInt(2, id);
                                    preparedStatement.addBatch();
                                    batchCount++;
                                } else {
                                    StringBuilder permCompleted = new StringBuilder(perm);
                                    for (int i = 0; i < diff; i++) {
                                        permCompleted.append(ClaimSettings.getDefaultValuesCode().charAt(perm.length() + i));
                                    }
                                    String permFinal = permCompleted.toString();
                                    preparedStatement.setString(1, permFinal);
                                    preparedStatement.setInt(2, id);
                                    preparedStatement.addBatch();
                                    batchCount++;
                                }
                            }
                        }
                    }

                    if (batchCount > 0) {
                        preparedStatement.executeBatch();
                        connection.commit(); // Commit transaction
                    }
                }
            } catch (SQLException e) {
                connection.rollback(); // Rollback transaction in case of error
                e.printStackTrace();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
		int i = 0;
		int max_i = 0;
		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
		    String getQuery = "SELECT * FROM scs_claims";
		    try (PreparedStatement preparedStatement = connection.prepareStatement(getQuery)) {
		        try (ResultSet resultSet = preparedStatement.executeQuery()) {
		            while (resultSet.next()) {
		            	max_i++;
		            	String permissions = resultSet.getString("Permissions");
		            	String id = resultSet.getString("id");
		            	String owner = resultSet.getString("name");
		            	String name = resultSet.getString("claim_name");
		            	String description = resultSet.getString("claim_description");
		            	int X = resultSet.getInt("X");
		            	int Z = resultSet.getInt("Z");
		            	String world_name = resultSet.getString("World");
		            	World world = Bukkit.getWorld(world_name);
		            	if(world == null) continue;
		            	String[] parts = resultSet.getString("Location").split(";");
		            	double L_X = Double.parseDouble(parts[0]);
		            	double L_Y = Double.parseDouble(parts[1]);
		            	double L_Z = Double.parseDouble(parts[2]);
		            	float L_Yaw = (float) Double.parseDouble(parts[3]);
		            	float L_Pitch = (float) Double.parseDouble(parts[4]);
		            	String s_members = resultSet.getString("Members");
		            	Set<String> members = new HashSet<>();
		            	if(!s_members.isBlank()) {
		            		parts = s_members.split(";");
			            	for(String m : parts) {
    		            		members.add(m);
    		            	}
		            	}
		            	String s_bans = resultSet.getString("Bans");
		            	Set<String> bans = new HashSet<>();
		            	if(!s_bans.isBlank()) {
		            		parts = s_bans.split(";");
			            	for(String m : parts) {
    		            		bans.add(m);
    		            	}
		            	}
		            	boolean sale = resultSet.getBoolean("isSale");
		            	Double price = resultSet.getDouble("SalePrice");
	                    if(SimpleClaimSystem.isFolia()) {
	                    	Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), world, X, Z, task -> {
	                    		Chunk chunk = world.getChunkAt(X,Z);
	                    		if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, name, owner);
	    		            	if(claimsId.containsKey(owner)) {
	    		            		claimsId.get(owner).put(chunk, id);
	    		            	} else {
	    		            		Map<Chunk,String> ids = new HashMap<>();
	    		            		ids.put(chunk, id);
	    		            		claimsId.put(owner, ids);
	    		            	}
        	                    LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>();
        	                    int count_i = 0;
        	                    for(String perm_key : ClaimSettings.getDefaultValues().keySet()) {
        	                    	char currentChar = permissions.charAt(count_i);
        	                    	count_i++;
        	                    	if(currentChar == '1') {
        	                    		perms.put(perm_key, true);
        	                    		continue;
        	                    	}
        	                    	perms.put(perm_key, false);
        	                    }
        	                    Location location = new Location(world,L_X,L_Y,L_Z,L_Yaw,L_Pitch);
        	                    listClaims.put(chunk, new Claim(chunk,owner,members,location,name,description,perms,sale,price,bans));
	                    	});
		            	} else {
                    		Chunk chunk = world.getChunkAt(X,Z);
                    		if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, name, owner);
    		            	if(claimsId.containsKey(owner)) {
    		            		claimsId.get(owner).put(chunk, id);
    		            	} else {
    		            		Map<Chunk,String> ids = new HashMap<>();
    		            		ids.put(chunk, id);
    		            		claimsId.put(owner, ids);
    		            	}
    	                    LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>();
    	                    int count_i = 0;
    	                    for(String perm_key : ClaimSettings.getDefaultValues().keySet()) {
    	                    	char currentChar = permissions.charAt(count_i);
    	                    	count_i++;
    	                    	if(currentChar == '1') {
    	                    		perms.put(perm_key, true);
    	                    		continue;
    	                    	}
    	                    	perms.put(perm_key, false);
    	                    }
    	                    Location location = new Location(world,L_X,L_Y,L_Z,L_Yaw,L_Pitch);
    	                    listClaims.put(chunk, new Claim(chunk,owner,members,location,name,description,perms,sale,price,bans));
		            	}
	                    i++;
		            }
		        }
		    }
		} catch (SQLException e) {
		    e.printStackTrace();
		}
		SimpleClaimSystem.getInstance().getLogger().info(String.valueOf(i)+"/"+String.valueOf(max_i)+" claims loaded");
		return;
    }
    
    // Method to create a claim
    public static void createClaim(Player player, Chunk chunk) {
    	String playerName = player.getName();
    	CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
    	
    	if(listClaims.containsKey(chunk)) {
    		Claim claim = listClaims.get(chunk);
    		String proprietaire = claim.getOwner();
    		if(proprietaire.equals("admin")) {
    			player.sendMessage(ClaimLanguage.getMessage("create-error-protected-area"));
    		} else if (proprietaire.equals(playerName)){
    			player.sendMessage(ClaimLanguage.getMessage("create-already-yours"));
    		} else {
    			player.sendMessage(ClaimLanguage.getMessage("create-already-claim").replace("%player%", proprietaire));
    		}
    		return;
    	}
    	
    	if(!cPlayer.canClaim()) {
    		player.sendMessage(ClaimLanguage.getMessage("cant-claim-anymore"));
    		return;
    	}
    	
    	if(ClaimSettings.getBooleanSetting("economy")) {
	    	if(ClaimSettings.getBooleanSetting("claim-cost")) {
	    		double price = 0;
	    		if(ClaimSettings.getBooleanSetting("claim-cost-multiplier")) {
	    			price = cPlayer.getMultipliedCost();
	    		} else {
	    			price = cPlayer.getCost();
	    		}
    	    	double balance = ClaimVault.getPlayerBalance(playerName);
    	    	if(balance < price) {
    	    		double diff = price-balance;
    	    		player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money-claim").replaceAll("%missing-price%", String.valueOf(diff)));
    	    		return;
    	    	}
    	    	ClaimVault.removePlayerBalance(playerName, price);
    	    	if(price>0) player.sendMessage(ClaimLanguage.getMessage("you-paid-claim").replaceAll("%price%", String.valueOf(price)));
	    	}
    	}
    	
    	displayChunk(player,chunk,true);
    	cPlayer.setClaimsCount(cPlayer.getClaimsCount()+1);
    	int nb = cPlayer.getMaxClaims()-cPlayer.getClaimsCount();
		player.sendMessage(ClaimLanguage.getMessage("create-claim-success").replaceAll("%remaining-claims%", String.valueOf(nb)));
		
    	String uuid = player.getUniqueId().toString();
    	int id = findFreeId(playerName);
    	String claim_name = "claim-"+String.valueOf(id);
        String description = ClaimLanguage.getMessage("default-description");
		String X = String.valueOf(chunk.getX());
		String Z = String.valueOf(chunk.getZ());
		String World = chunk.getWorld().getName();
        Location location = player.getLocation();
        String location_string = String.valueOf(location.getX()+";"+location.getY()+";"+location.getZ()+";"+location.getYaw()+";"+location.getPitch());
        LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
        listClaims.put(chunk, new Claim(chunk,playerName,Set.of(playerName),location,claim_name,description,perms,false,0.0,new HashSet<>()));
        if(claimsId.containsKey(playerName)) {
        	claimsId.get(playerName).put(chunk, String.valueOf(id));
        } else {
        	Map<Chunk,String> ids = new HashMap<>();
        	ids.put(chunk, String.valueOf(id));
        	claimsId.put(playerName, ids);
        }
        
        if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claim_name, playerName);
        if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.createChunkZone(chunk, claim_name, playerName);
    	
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.activeBossBar(p,chunk);
    			}
    		});
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	preparedStatement.setInt(1, id);
	                	preparedStatement.setString(2, uuid);
	                	preparedStatement.setString(3, playerName);
	                    preparedStatement.setString(4, claim_name);
	                    preparedStatement.setString(5, description);
	                    preparedStatement.setString(6, X);
	                    preparedStatement.setString(7, Z);
	                    preparedStatement.setString(8, World);
	                    preparedStatement.setString(9, location_string);
	                    preparedStatement.setString(10, playerName);
	                    preparedStatement.setString(11, ClaimSettings.getDefaultValuesCode());
	                    preparedStatement.executeUpdate();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	} else {
    		for(Entity e : chunk.getEntities()) {
    			if(!(e instanceof Player)) continue;
    			Player p = (Player) e;
    			ClaimEventsEnterLeave.activeBossBar(p,chunk);
    		}
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	preparedStatement.setInt(1, id);
	                	preparedStatement.setString(2, uuid);
	                	preparedStatement.setString(3, playerName);
	                    preparedStatement.setString(4, claim_name);
	                    preparedStatement.setString(5, description);
	                    preparedStatement.setString(6, X);
	                    preparedStatement.setString(7, Z);
	                    preparedStatement.setString(8, World);
	                    preparedStatement.setString(9, location_string);
	                    preparedStatement.setString(10, playerName);
	                    preparedStatement.setString(11, ClaimSettings.getDefaultValuesCode());
	                    preparedStatement.executeUpdate();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	}
		return;
    }
    
    // Method to create a protected area (admin claim)
    public static void createAdminClaim(Player player, Chunk chunk) {
    	String playerName = player.getName();
    	
    	if(listClaims.containsKey(chunk)) {
    		Claim claim = listClaims.get(chunk);
    		String proprietaire = claim.getOwner();
    		if(proprietaire.equals("admin")) {
    			player.sendMessage(ClaimLanguage.getMessage("create-error-protected-area"));
    		} else if (proprietaire.equals(playerName)){
    			player.sendMessage(ClaimLanguage.getMessage("create-already-yours"));
    		} else {
    			player.sendMessage(ClaimLanguage.getMessage("create-already-claim").replace("%player%", proprietaire));
    		}
    		return;
    	}
    	
    	displayChunk(player,chunk,true);
		player.sendMessage(ClaimLanguage.getMessage("create-protected-area-success"));
		
    	String uuid = "aucun";
    	int id = findFreeId("admin");
    	String claim_name = "admin-"+String.valueOf(id);
        String description = ClaimLanguage.getMessage("default-description");
		String X = String.valueOf(chunk.getX());
		String Z = String.valueOf(chunk.getZ());
		String World = chunk.getWorld().getName();
        Location location = player.getLocation();
        String location_string = String.valueOf(location.getX()+";"+location.getY()+";"+location.getZ()+";"+location.getYaw()+";"+location.getPitch());
        LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
        listClaims.put(chunk, new Claim(chunk,"admin",new HashSet<>(),location,claim_name,description,perms,false,0.0,new HashSet<>()));
        if(claimsId.containsKey("admin")) {
        	claimsId.get("admin").put(chunk, String.valueOf(id));
        } else {
        	Map<Chunk,String> ids = new HashMap<>();
        	ids.put(chunk, String.valueOf(id));
        	claimsId.put("admin", ids);
        }
        
        if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claim_name, "admin");
        
    	if(SimpleClaimSystem.isFolia()) {
      		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.activeBossBar(p,chunk);
    			}
    		});
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	preparedStatement.setInt(1, id);
	                	preparedStatement.setString(2, uuid);
	                	preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, claim_name);
	                    preparedStatement.setString(5, description);
	                    preparedStatement.setString(6, X);
	                    preparedStatement.setString(7, Z);
	                    preparedStatement.setString(8, World);
	                    preparedStatement.setString(9, location_string);
	                    preparedStatement.setString(10, "");
	                    preparedStatement.setString(11, ClaimSettings.getDefaultValuesCode());
	                    preparedStatement.executeUpdate();
	                }
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	} else {
			for(Entity e : chunk.getEntities()) {
				if(!(e instanceof Player)) continue;
				Player p = (Player) e;
				ClaimEventsEnterLeave.activeBossBar(p,chunk);
			}
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	preparedStatement.setInt(1, id);
	                	preparedStatement.setString(2, uuid);
	                	preparedStatement.setString(3, "admin");
	                    preparedStatement.setString(4, claim_name);
	                    preparedStatement.setString(5, description);
	                    preparedStatement.setString(6, X);
	                    preparedStatement.setString(7, Z);
	                    preparedStatement.setString(8, World);
	                    preparedStatement.setString(9, location_string);
	                    preparedStatement.setString(10, "");
	                    preparedStatement.setString(11, ClaimSettings.getDefaultValuesCode());
	                    preparedStatement.executeUpdate();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	}
		return;
    }
    
    // Method to create some claims with radius
    public static boolean createClaimRadius(Player player, Set<Chunk> chunks, int radius) {
    	Set<Chunk> chunksToClaim = new HashSet<>();
    	String playerName = player.getName();
    	CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
    	
    	int i = 0;
    	for(Chunk chunk : chunks) {
        	if(!checkIfClaimExists(chunk)) {
        		chunksToClaim.add(chunk);
        		i++;
        	}
    	}
    	
    	if(i != chunksToClaim.size()) {
    		player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim-already-claim"));
    		return false;
    	}
    	
    	if(!cPlayer.canClaimWithNumber(i)) {
    		player.sendMessage(ClaimLanguage.getMessage("cant-claim-anymore"));
    		return false;
    	}
    	
    	double price = 0;
		if(ClaimSettings.getBooleanSetting("economy")) {
	    	if(ClaimSettings.getBooleanSetting("claim-cost")) {
	    		if(ClaimSettings.getBooleanSetting("claim-cost-multiplier")) {
	    			price = cPlayer.getRadiusMultipliedCost(i);
	    		} else {
	    			price = cPlayer.getCost() * i;
	    		}
	    	}
    	}
    	
    	if(price>0) {
    		double balance = ClaimVault.getPlayerBalance(playerName);
	    	if(balance < price) {
	    		double diff = price-balance;
	    		player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money-claim").replaceAll("%missing-price%", String.valueOf(diff)));
	    		return true;
	    	}
	    	ClaimVault.removePlayerBalance(playerName, price);
	    	if(price>0) player.sendMessage(ClaimLanguage.getMessage("you-paid-claim").replaceAll("%price%", String.valueOf(price)));
    	}

		Map<Chunk,Map<String,String>> values = new HashMap<>();
		LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
		for(Chunk chunk : chunksToClaim) {
            Set<String> members = new HashSet<>();
            members.add(playerName);
    		int id = findFreeId(playerName);
        	String uuid = player.getUniqueId().toString();
        	String claim_name = "claim-"+String.valueOf(id);
            Map<Chunk,String> newid = new HashMap<>();
            if(claimsId.get(playerName) != null) {
            	newid = new HashMap<>(claimsId.get(playerName));
            }
            newid.put(chunk, String.valueOf(id));
            claimsId.put(playerName, newid);
            String id_final = String.valueOf(id);
    		String description = ClaimLanguage.getMessage("default-description");
    		String X = String.valueOf(chunk.getX());
    		String Z = String.valueOf(chunk.getZ());
    		String World = chunk.getWorld().getName();
            Location spawn_loc = player.getLocation();
            String Location = "";
            if(player.getLocation().getChunk().equals(chunk)) {
            	Location = String.valueOf(spawn_loc.getX())+";"+String.valueOf(spawn_loc.getY())+";"+String.valueOf(spawn_loc.getZ())+";"+String.valueOf(spawn_loc.getYaw())+";"+String.valueOf(spawn_loc.getPitch());
            } else {
            	int loc_x = getChunkCenterX(chunk);
            	int loc_y = getChunkCenterY(chunk);
            	int loc_z = getChunkCenterZ(chunk);
            	Location = String.valueOf(loc_x)+";"+String.valueOf(loc_y)+";"+String.valueOf(loc_z)+";"+String.valueOf(spawn_loc.getYaw())+";"+String.valueOf(spawn_loc.getPitch());
                spawn_loc = new Location(spawn_loc.getWorld(),loc_x,loc_y,loc_z);
            }
            String Location_final = Location;
            
            Map<String,String> sub_values = new HashMap<>();
            sub_values.put("id", id_final);
            sub_values.put("uuid", uuid);
            sub_values.put("owner", playerName);
            sub_values.put("claim_name", claim_name);
            sub_values.put("description", description);
            sub_values.put("X", X);
            sub_values.put("Z", Z);
            sub_values.put("World", World);
            sub_values.put("Location", Location_final);
            sub_values.put("Members", String.join(";", members));
            sub_values.put("Permissions", ClaimSettings.getDefaultValuesCode());
            values.put(chunk, sub_values);
            
            listClaims.put(chunk, new Claim(chunk,playerName,Set.of(playerName),spawn_loc,claim_name,description,perms,false,0.0,new HashSet<>()));
            
            if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claim_name, playerName);

            if(SimpleClaimSystem.isFolia()) {
	    		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
	    			for(Entity e : chunk.getEntities()) {
	    				if(!(e instanceof Player)) continue;
	    				Player p = (Player) e;
	    				ClaimEventsEnterLeave.activeBossBar(p,chunk);
	    			}
	    		});
            } else {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.activeBossBar(p,chunk);
    			}
            }
		}
		
		displayChunkBorderWithRadius(player,player.getLocation().getChunk(),radius);
		cPlayer.setClaimsCount(cPlayer.getClaimsCount()+i);
		
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	for(Chunk chunk : chunksToClaim) {
		                	preparedStatement.setString(1, values.get(chunk).get("id"));
		                	preparedStatement.setString(2, values.get(chunk).get("uuid"));
		                	preparedStatement.setString(3, values.get(chunk).get("owner"));
		                    preparedStatement.setString(4, values.get(chunk).get("claim_name"));
		                    preparedStatement.setString(5, values.get(chunk).get("description"));
		                    preparedStatement.setString(6, values.get(chunk).get("X"));
		                    preparedStatement.setString(7, values.get(chunk).get("Z"));
		                    preparedStatement.setString(8, values.get(chunk).get("World"));
		                    preparedStatement.setString(9, values.get(chunk).get("Location"));
		                    preparedStatement.setString(10, values.get(chunk).get("Members"));
		                    preparedStatement.setString(11, values.get(chunk).get("Permissions"));
		                    preparedStatement.addBatch();
	                	}
	                	preparedStatement.executeBatch();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	for(Chunk chunk : chunksToClaim) {
		                	preparedStatement.setString(1, values.get(chunk).get("id"));
		                	preparedStatement.setString(2, values.get(chunk).get("uuid"));
		                	preparedStatement.setString(3, values.get(chunk).get("owner"));
		                    preparedStatement.setString(4, values.get(chunk).get("claim_name"));
		                    preparedStatement.setString(5, values.get(chunk).get("description"));
		                    preparedStatement.setString(6, values.get(chunk).get("X"));
		                    preparedStatement.setString(7, values.get(chunk).get("Z"));
		                    preparedStatement.setString(8, values.get(chunk).get("World"));
		                    preparedStatement.setString(9, values.get(chunk).get("Location"));
		                    preparedStatement.setString(10, values.get(chunk).get("Members"));
		                    preparedStatement.setString(11, values.get(chunk).get("Permissions"));
		                    preparedStatement.addBatch();
	                	}
	                	preparedStatement.executeBatch();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	}
    	int nb = cPlayer.getMaxClaims()-cPlayer.getClaimsCount();
    	player.sendMessage(ClaimLanguage.getMessage("create-claim-radius-success").replaceAll("%number%", String.valueOf(chunksToClaim.size())).replaceAll("%remaining-claims%", String.valueOf(nb)));
    	return true;
    }
    
    // Methods to create some protected areas (admin claims) with radius
    public static boolean createAdminClaimRadius(Player player, Set<Chunk> chunks, int radius) {
    	Set<Chunk> chunksToClaim = new HashSet<>();
    	int i = 0;
    	for(Chunk chunk : chunks) {
        	if(!checkIfClaimExists(chunk)) {
        		chunksToClaim.add(chunk);
        		i++;
        	}
    	}
    	
    	if(i != chunksToClaim.size()) {
    		player.sendMessage(ClaimLanguage.getMessage("cant-radius-claim-already-claim"));
    		return false;
    	}

		Map<Chunk,Map<String,String>> values = new HashMap<>();
		LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>(ClaimSettings.getDefaultValues());
		for(Chunk chunk : chunksToClaim) {
    		int id = findFreeId("admin");
        	String uuid = "aucun";
        	String claim_name = "admin-"+String.valueOf(id);
            Map<Chunk,String> newid = new HashMap<>();
            if(claimsId.get("admin") != null) {
            	newid = new HashMap<>(claimsId.get("admin"));
            }
            newid.put(chunk, String.valueOf(id));
            claimsId.put("admin", newid);
            String id_final = String.valueOf(id);
    		String description = ClaimLanguage.getMessage("default-description");
    		String X = String.valueOf(chunk.getX());
    		String Z = String.valueOf(chunk.getZ());
    		String World = chunk.getWorld().getName();
            Location spawn_loc = player.getLocation();
            String Location = "";
            if(player.getLocation().getChunk().equals(chunk)) {
            	Location = String.valueOf(spawn_loc.getX())+";"+String.valueOf(spawn_loc.getY())+";"+String.valueOf(spawn_loc.getZ())+";"+String.valueOf(spawn_loc.getYaw())+";"+String.valueOf(spawn_loc.getPitch());
            } else {
            	int loc_x = getChunkCenterX(chunk);
            	int loc_y = getChunkCenterY(chunk);
            	int loc_z = getChunkCenterZ(chunk);
            	Location = String.valueOf(loc_x)+";"+String.valueOf(loc_y)+";"+String.valueOf(loc_z)+";"+String.valueOf(spawn_loc.getYaw())+";"+String.valueOf(spawn_loc.getPitch());
                spawn_loc = new Location(spawn_loc.getWorld(),loc_x,loc_y,loc_z);
            }
            String Location_final = Location;
            
            Map<String,String> sub_values = new HashMap<>();
            sub_values.put("id", id_final);
            sub_values.put("uuid", uuid);
            sub_values.put("owner", "admin");
            sub_values.put("claim_name", claim_name);
            sub_values.put("description", description);
            sub_values.put("X", X);
            sub_values.put("Z", Z);
            sub_values.put("World", World);
            sub_values.put("Location", Location_final);
            sub_values.put("Members", "");
            sub_values.put("Permissions", ClaimSettings.getDefaultValuesCode());
            values.put(chunk, sub_values);
            
            listClaims.put(chunk, new Claim(chunk,"admin",new HashSet<>(),spawn_loc,claim_name,description,perms,false,0.0, new HashSet<>()));

            if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.createChunkZone(chunk, claim_name, "admin");
            
            if(SimpleClaimSystem.isFolia()) {
	    		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
	    			for(Entity e : chunk.getEntities()) {
	    				if(!(e instanceof Player)) continue;
	    				Player p = (Player) e;
	    				ClaimEventsEnterLeave.activeBossBar(p,chunk);
	    			}
	    		});
            } else {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.activeBossBar(p,chunk);
    			}
            }
		}
		
		displayChunkBorderWithRadius(player,player.getLocation().getChunk(),radius);
		
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	for(Chunk chunk : chunksToClaim) {
		                	preparedStatement.setString(1, values.get(chunk).get("id"));
		                	preparedStatement.setString(2, values.get(chunk).get("uuid"));
		                	preparedStatement.setString(3, values.get(chunk).get("owner"));
		                    preparedStatement.setString(4, values.get(chunk).get("claim_name"));
		                    preparedStatement.setString(5, values.get(chunk).get("description"));
		                    preparedStatement.setString(6, values.get(chunk).get("X"));
		                    preparedStatement.setString(7, values.get(chunk).get("Z"));
		                    preparedStatement.setString(8, values.get(chunk).get("World"));
		                    preparedStatement.setString(9, values.get(chunk).get("Location"));
		                    preparedStatement.setString(10, values.get(chunk).get("Members"));
		                    preparedStatement.setString(11, values.get(chunk).get("Permissions"));
		                    preparedStatement.addBatch();
	                	}
	                	preparedStatement.executeBatch();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
        		try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String insertQuery = "INSERT INTO scs_claims (id, uuid, name, claim_name, claim_description, X, Z, World, Location, Members, Permissions) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(insertQuery)) {
	                	for(Chunk chunk : chunksToClaim) {
		                	preparedStatement.setString(1, values.get(chunk).get("id"));
		                	preparedStatement.setString(2, values.get(chunk).get("uuid"));
		                	preparedStatement.setString(3, values.get(chunk).get("owner"));
		                    preparedStatement.setString(4, values.get(chunk).get("claim_name"));
		                    preparedStatement.setString(5, values.get(chunk).get("description"));
		                    preparedStatement.setString(6, values.get(chunk).get("X"));
		                    preparedStatement.setString(7, values.get(chunk).get("Z"));
		                    preparedStatement.setString(8, values.get(chunk).get("World"));
		                    preparedStatement.setString(9, values.get(chunk).get("Location"));
		                    preparedStatement.setString(10, values.get(chunk).get("Members"));
		                    preparedStatement.setString(11, values.get(chunk).get("Permissions"));
		                    preparedStatement.addBatch();
	                	}
	                	preparedStatement.executeBatch();
	                }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    		});
    	}
    	player.sendMessage(ClaimLanguage.getMessage("create-protected-area-radius-success").replaceAll("%number%", String.valueOf(chunksToClaim.size())));
    	return true;
    }
    
    // Check if a claim exists in a chunk
    public static boolean checkIfClaimExists(Chunk chunk) {
    	return listClaims.containsKey(chunk);
    }
    
    // Get the owner of a claim by the chunk
    public static String getOwnerInClaim(Chunk chunk) {
    	if(listClaims.containsKey(chunk)) return listClaims.get(chunk).getOwner();
    	return "";
    }
    
    // Return the boolean of the given perm
    public static boolean canPermCheck(Chunk chunk, String perm) {
    	if(!listClaims.containsKey(chunk)) return false;
    	return listClaims.get(chunk).getPermissions().get(perm);
    }
    
    // Check if a player is member of a claim
    public static boolean checkMembre(Chunk chunk, Player player) {
    	if(!listClaims.containsKey(chunk)) return false;
    	return listClaims.get(chunk).getMembers().contains(player.getName());
    }
    
    // Check if a playername is member of a claim
    public static boolean checkMembre(Chunk chunk, String targetName) {
    	if(!listClaims.containsKey(chunk)) return false;
        Iterator<String> iterator = listClaims.get(chunk).getMembers().iterator();
        while (iterator.hasNext()) {
            String currentString = iterator.next();
            if (currentString.equalsIgnoreCase(targetName)) {
                return true;
            }
        }
    	return false;
    }
    
    // Check if a playername is banned from a claim
    public static boolean checkBan(Chunk chunk, String targetName) {
    	if(!listClaims.containsKey(chunk)) return false;
        Iterator<String> iterator = listClaims.get(chunk).getBans().iterator();
        while (iterator.hasNext()) {
            String currentString = iterator.next();
            if (currentString.equalsIgnoreCase(targetName)) {
                return true;
            }
        }
    	return false;
    }
    
    // Get the real name of the player from claim members
    public static String getRealNameFromClaimMembers(Chunk chunk, String targetName) {
    	if(!listClaims.containsKey(chunk)) return targetName;
        Iterator<String> iterator = listClaims.get(chunk).getMembers().iterator();
        while (iterator.hasNext()) {
            String currentString = iterator.next();
            if (currentString.equalsIgnoreCase(targetName)) {
                return currentString;
            }
        }
    	return targetName;
    }
    
    // Get the real name of the player from claim bans
    public static String getRealNameFromClaimBans(Chunk chunk, String targetName) {
    	if(!listClaims.containsKey(chunk)) return targetName;
        Iterator<String> iterator = listClaims.get(chunk).getBans().iterator();
        while (iterator.hasNext()) {
            String currentString = iterator.next();
            if (currentString.equalsIgnoreCase(targetName)) {
                return currentString;
            }
        }
    	return targetName;
    }
    
    // Check if a player is ban from a claim
    public static boolean checkBan(Chunk chunk, Player player) {
    	if(!listClaims.containsKey(chunk)) return false;
    	return listClaims.get(chunk).getBans().contains(player.getName());
    }
    
    // Get the members set
    public static Set<String> getClaimMembers(Chunk chunk){
    	if(!listClaims.containsKey(chunk)) return new HashSet<>();
    	return listClaims.get(chunk).getMembers();
    }
    
    // Get the bans set
    public static Set<String> getClaimBans(Chunk chunk){
    	if(!listClaims.containsKey(chunk)) return new HashSet<>();
    	return listClaims.get(chunk).getBans();
    }
    
    // Method to update a claim's permission
    public static boolean updatePerm(Player player, Chunk chunk, String perm, boolean result) {
    	if(!listClaims.containsKey(chunk)) return false;
        LinkedHashMap<String,Boolean> perms = listClaims.get(chunk).getPermissions();
        perms.put(perm, result);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    	        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    	        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to update admin claim's permission
    public static boolean updateAdminPerm(Chunk chunk, String perm, boolean result) {
    	if(!listClaims.containsKey(chunk)) return false;
        LinkedHashMap<String,Boolean> perms = listClaims.get(chunk).getPermissions();
        perms.put(perm, result);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    	        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    	        try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to remove a member from a claim
    public static boolean removeClaimMembers(Player player, Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.removeMember(name);
    	String membersString = String.join(";", claim.getMembers());
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("remove-claim-player").replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", player.getName()));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to remove a member from an admin claim
    public static boolean removeAdminClaimMembers(Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.removeMember(name);
    	String membersString = String.join(";", claim.getMembers());
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }

    // Method to apply current settings to all admin claims
    public static boolean applyAllSettingsAdmin(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim c = listClaims.get(chunk);
    	LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>(c.getPermissions());
    	listClaims.values().stream()
        	.filter(claim -> "admin".equals(claim.getOwner()))
        	.forEach(claim -> claim.setPermissions(perms));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to apply current settings to all player's claims
    public static boolean applyAllSettings(Chunk chunk, Player player) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim c = listClaims.get(chunk);
    	LinkedHashMap<String,Boolean> perms = new LinkedHashMap<>(c.getPermissions());
    	listClaims.values().stream()
        	.filter(claim -> player.getName().equals(claim.getOwner()))
        	.forEach(claim -> claim.setPermissions(perms));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
	    		StringBuilder sb = new StringBuilder();
	            for(String key : perms.keySet()) {
	            	if(perms.get(key)){
	            		sb.append("1");
	            	} else {
	            		sb.append("0");
	            	}
	            }
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Permissions = ? WHERE uuid = ? AND name = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, sb.toString());
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to ban a player from a player's claim
    public static boolean addClaimBan(Player player, Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.addBan(name);
    	String banString = String.join(";", claim.getBans());
    	if(claim.getMembers().contains(name)) removeClaimMembers(player,chunk,name);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to ban a member from an admin claim
    public static boolean addAdminClaimBan(Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.addBan(name);
    	String banString = String.join(";", claim.getBans());
    	if(claim.getMembers().contains(name)) removeAdminClaimMembers(chunk,name);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to unban a player from a player's claim
    public static boolean removeClaimBan(Player player, Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.removeBan(name);
    	String banString = String.join(";", claim.getBans());
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to unban a member from an admin claim
    public static boolean removeAdminClaimBan(Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.removeBan(name);
    	String banString = String.join(";", claim.getBans());
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, banString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to add a member to a player's claim
    public static boolean addClaimMembers(Player player, Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.addMember(name);
    	String membersString = String.join(";", claim.getMembers());
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("add-claim-player").replaceAll("%claim-name%", claim.getName()).replaceAll("%owner%", player.getName()));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to add a member to an admin claim
    public static boolean addAdminClaimMembers(Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.addMember(name);
    	String membersString = String.join(";", claim.getMembers());
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("add-claim-protected-area-player").replaceAll("%claim-name%", claim.getName()));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, membersString);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to add a member to all admin claims
    public static boolean addAllAdminClaimMembers(String name) {
    	listClaims.values().stream()
        	.filter(claim -> "admin".equals(claim.getOwner()))
        	.forEach(claim -> claim.addMember(name));
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("add-all-claim-protected-area-player"));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to ban a member from all player's claims
    public static boolean addAllClaimBan(Player player, String name) {
    	String playerName = player.getName();
    	listClaims.values().stream()
    		.filter(claim -> playerName.equals(claim.getOwner()))
    		.forEach(claim -> claim.addBan(name));
    	removeAllClaimMembers(player,name);

    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, player.getName());
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, player.getName());
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to unban a player from all player's claims
    public static boolean removeAllClaimBan(Player player, String name) {
    	String playerName = player.getName();
    	listClaims.values().stream()
			.filter(claim -> playerName.equals(claim.getOwner()))
			.forEach(claim -> claim.removeBan(name));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, player.getName());
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, player.getName());
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to ban a player to all admin claims
    public static boolean addAllAdminClaimBan(String name) {
    	listClaims.values().stream()
        	.filter(claim -> "admin".equals(claim.getOwner()))
        	.forEach(claim -> claim.addBan(name));
    	removeAllAdminClaimMembers(name);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to unban a member from all admin claims
    public static boolean removeAllAdminClaimBan(String name) {
    	listClaims.values().stream()
    		.filter(claim -> "admin".equals(claim.getOwner()))
    		.forEach(claim -> claim.removeBan(name));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Bans = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getBans());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to add a member to all player's claims
    public static boolean addAllClaimMembers(Player player, String name) {
    	String playerName = player.getName();
    	listClaims.values().stream()
    		.filter(claim -> playerName.equals(claim.getOwner()))
    		.forEach(claim -> claim.addMember(name));
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("add-all-claim-player").replaceAll("%owner%", playerName));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, playerName);
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, playerName);
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to remove a member from all admin claims
    public static boolean removeAllAdminClaimMembers(String name) {
    	listClaims.values().stream()
    		.filter(claim -> "admin".equals(claim.getOwner()))
    		.forEach(claim -> claim.removeMember(name));
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("remove-all-claim-protected-area-player"));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner("admin")) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, "aucun");
        	                preparedStatement.setString(3, "admin");
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to remove a member from all player's claims
    public static boolean removeAllClaimMembers(Player player, String name) {
    	String playerName = player.getName();
    	listClaims.values().stream()
			.filter(claim -> playerName.equals(claim.getOwner()))
			.forEach(claim -> claim.removeMember(name));
    	Player target = Bukkit.getPlayer(name);
    	if(target != null) target.sendMessage(ClaimLanguage.getMessage("remove-all-claim-player").replaceAll("%owner%", playerName));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, playerName);
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    	            String updateQuery = "UPDATE scs_claims SET Members = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	for(Chunk chunk : getChunksFromOwner(playerName)) {
    	            		Claim claim = listClaims.get(chunk);
    	            		String banString = String.join(";", claim.getMembers());
        	            	preparedStatement.setString(1, banString);
        	                preparedStatement.setString(2, player.getUniqueId().toString());
        	                preparedStatement.setString(3, playerName);
        	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
        	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
        	                preparedStatement.addBatch();
    	            	}
    	                preparedStatement.executeBatch();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to change a player's claim's name
    public static boolean setClaimName(Player player, Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setName(name);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.bossbarMessages(p, chunk, p.getName());
    			}
    		});
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, name);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		for(Entity e : chunk.getEntities()) {
    			if(!(e instanceof Player)) continue;
    			Player p = (Player) e;
    			ClaimEventsEnterLeave.bossbarMessages(p, chunk, p.getName());
    		}
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, name);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to change an admin's claim's name
    public static boolean setAdminClaimName(Chunk chunk, String name) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setName(name);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.bossbarMessages(p, chunk, p.getName());
    			}
    		});
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, name);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		for(Entity e : chunk.getEntities()) {
    			if(!(e instanceof Player)) continue;
    			Player p = (Player) e;
    			ClaimEventsEnterLeave.bossbarMessages(p, chunk, p.getName());
    		}
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_name = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, name);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to change the claim's spawn location
    public static boolean setClaimLocation(Player player, Chunk chunk, Location loc) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setLocation(loc);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			String loc_string = String.valueOf(loc.getX())+";"+String.valueOf(loc.getY())+";"+String.valueOf(loc.getZ())+";"+String.valueOf(loc.getYaw())+";"+String.valueOf(loc.getPitch());
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET Location = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, loc_string);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			String loc_string = String.valueOf(loc.getX())+";"+String.valueOf(loc.getY())+";"+String.valueOf(loc.getZ())+";"+String.valueOf(loc.getYaw())+";"+String.valueOf(loc.getPitch());
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET Location = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, loc_string);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to delete claim with radius
    public static boolean deleteClaimRadius(Player player, Set<Chunk> chunks) {
    	String owner = "";
    	String uuid = "";
    	List<Integer> ids = new ArrayList<>();
    	for(Chunk chunk : chunks) {
    		if(!listClaims.containsKey(chunk)) continue;
    		Claim claim = listClaims.get(chunk);
        	owner = claim.getOwner();
        	CPlayer cPlayer = CPlayerMain.getCPlayer(owner);
        	if(cPlayer != null) {
        		cPlayer.setClaimsCount(cPlayer.getClaimsCount()-1);
        	}
        	uuid = "";
        	if(owner.equals("admin")) {
        		ids.add(Integer.parseInt(claimsId.get("admin").get(chunk)));
        		uuid = "aucun";
        	} else {
        		ids.add(Integer.parseInt(claimsId.get(owner).get(chunk)));
                uuid = player.getUniqueId().toString();
        	}
        	claimsId.get(owner).remove(chunk);
        	if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
        	listClaims.remove(chunk);
        	if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
        	if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
        	if(SimpleClaimSystem.isFolia()) {
        		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
        			for(Entity e : chunk.getEntities()) {
        				if(!(e instanceof Player)) continue;
        				Player p = (Player) e;
        				ClaimEventsEnterLeave.disableBossBar(p);
        			}
        		});
        	} else {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.disableBossBar(p);
    			}
        	}
    	}
    	final String final_uuid = uuid;
		String idsString = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
        
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, final_uuid);
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, final_uuid);
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	}
    	return true;
    }
    
    // Method to delete claim
    public static boolean deleteClaim(Player player, Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	String owner = claim.getOwner();
    	listClaims.remove(chunk);
    	
    	CPlayer cPlayer = CPlayerMain.getCPlayer(owner);
    	if(cPlayer != null) {
    		cPlayer.setClaimsCount(cPlayer.getClaimsCount()-1);
    	}
    	
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.disableBossBar(p);
    			}
    		});
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
    	    	String id = claimsId.get(owner).get(chunk);
    	    	String uuid = player.getUniqueId().toString();
    	    	if(owner.equals("admin")) uuid = "aucun";
    	    	claimsId.get(owner).remove(chunk);
    	    	if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    	    	listClaims.remove(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id = ? AND uuid = ? AND name = ? AND X = ? AND Z = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, id);
	                    preparedStatement.setString(2, uuid);
	                    preparedStatement.setString(3, owner);
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	} else {
			for(Entity e : chunk.getEntities()) {
				if(!(e instanceof Player)) continue;
				Player p = (Player) e;
				ClaimEventsEnterLeave.disableBossBar(p);
			}
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
    	    	String id = claimsId.get(owner).get(chunk);
    	    	String uuid = player.getUniqueId().toString();
    	    	if(owner.equals("admin")) uuid = "aucun";
    	    	claimsId.get(owner).remove(chunk);
    	    	if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    	    	listClaims.remove(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id = ? AND uuid = ? AND name = ? AND X = ? AND Z = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, id);
	                    preparedStatement.setString(2, uuid);
	                    preparedStatement.setString(3, owner);
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	}
    	return true;
    }
    
    // Method to delete all player's claims
    public static boolean deleteAllClaim(Player player) {
    	String playerName = player.getName();
    	Set<Chunk> chunks = getChunksFromOwner(playerName);
    	String uuid = player.getUniqueId().toString();
    	List<Integer> ids = new ArrayList<>();
    	int i = 0;
    	CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
    	cPlayer.setClaimsCount(0);
    	for(Chunk chunk : chunks) {
    		if(!listClaims.containsKey(chunk)) continue;
    		if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
    		if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
        	ids.add(Integer.parseInt(claimsId.get(player.getName()).get(chunk)));
        	claimsId.get(playerName).remove(chunk);
        	if(claimsId.get(playerName).isEmpty()) claimsId.remove(playerName);
        	listClaims.remove(chunk);
        	if(SimpleClaimSystem.isFolia()) {
        		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
        			for(Entity e : chunk.getEntities()) {
        				if(!(e instanceof Player)) continue;
        				Player p = (Player) e;
        				ClaimEventsEnterLeave.disableBossBar(p);
        			}
        		});
        	} else {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.disableBossBar(p);
    			}
        	}
            i++;
    	}
		String idsString = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
        player.sendMessage(ClaimLanguage.getMessage("territory-delete-radius-success").replaceAll("%number%", String.valueOf(i)));
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid);
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid);
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	}
    	return true;
    }
    
    // Method to delete all player's claims
    public static boolean deleteAllClaim(String playerName) {
    	Set<Chunk> chunks = getChunksFromOwner(playerName);
    	Player player = Bukkit.getPlayer(playerName);
    	String uuid = "";
    	if(player == null) {
    		uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId().toString();
    	} else {
    		uuid = player.getUniqueId().toString();
    	}
    	List<Integer> ids = new ArrayList<>();
    	int i = 0;
    	CPlayer cPlayer = CPlayerMain.getCPlayer(playerName);
    	if(cPlayer != null) cPlayer.setClaimsCount(0);
    	for(Chunk chunk : chunks) {
    		if(!listClaims.containsKey(chunk)) continue;
    		if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
    		if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
        	ids.add(Integer.parseInt(claimsId.get(playerName).get(chunk)));
        	claimsId.get(playerName).remove(chunk);
        	if(claimsId.get(playerName).isEmpty()) claimsId.remove(playerName);
        	listClaims.remove(chunk);
        	if(SimpleClaimSystem.isFolia()) {
        		Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
        			for(Entity e : chunk.getEntities()) {
        				if(!(e instanceof Player)) continue;
        				Player p = (Player) e;
        				ClaimEventsEnterLeave.disableBossBar(p);
        			}
        		});
        	} else {
    			for(Entity e : chunk.getEntities()) {
    				if(!(e instanceof Player)) continue;
    				Player p = (Player) e;
    				ClaimEventsEnterLeave.disableBossBar(p);
    			}
        	}
            i++;
    	}
		String idsString = String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new));
		final String uuid_final = uuid;
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid_final);
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id IN (" + idsString + ") AND uuid = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, uuid_final);
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	}
    	return true;
    }
    
    // Method to force the deletion of a claim
    public static boolean forceDeleteClaim(Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return false;
    	
    	Claim claim = listClaims.get(chunk);
    	String owner = claim.getOwner();
    	if(owner.equals("admin")) {
    		return deleteClaim(null,chunk);
    	}
    	OfflinePlayer player = Bukkit.getOfflinePlayer(owner);
    	if(player.isOnline()) {
    		CPlayer cTarget = CPlayerMain.getCPlayer(owner);
    		cTarget.setClaimsCount(cTarget.getClaimsCount()-1);
    	}
    	String id = claimsId.get(player.getName()).get(chunk);
        claimsId.get(owner).remove(chunk);
    	if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    	listClaims.remove(chunk);
    	
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id = ? AND uuid = ? AND name = ? AND X = ? AND Z = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, id);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.deleteMarker(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.deleteMarker(chunk);
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
	                String deleteQuery = "DELETE FROM scs_claims WHERE id = ? AND uuid = ? AND name = ? AND X = ? AND Z = ?";
	                try (PreparedStatement preparedStatement = connection.prepareStatement(deleteQuery)) {
	                    preparedStatement.setString(1, id);
	                    preparedStatement.setString(2, player.getUniqueId().toString());
	                    preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
	                    preparedStatement.executeUpdate();
	                }
	            } catch (SQLException e) {
	                e.printStackTrace();
	            }
	    		return;
    		});
    	}
    	return true;
    }
    
    // Method to change player's claim's description
    public static boolean setChunkDescription(Player player, Chunk chunk, String description) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setDescription(description);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, description);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, description);
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
        return true;
    }
    
    // Method to change admin claim's description
    public static boolean setAdminChunkDescription(Chunk chunk, String description) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setDescription(description);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, description);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET claim_description = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, description);
    	                preparedStatement.setString(2, "aucun");
    	                preparedStatement.setString(3, "admin");
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
        return true;
    }
    
    // Method to put a claim in sale
    public static boolean setChunkSale(Player player, Chunk chunk, double price) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setSale(true);
    	claim.setPrice(price);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET isSale = true, SalePrice = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	                preparedStatement.setString(1, String.valueOf(price));
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET isSale = true, SalePrice = ? WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	                preparedStatement.setString(1, String.valueOf(price));
    	                preparedStatement.setString(2, player.getUniqueId().toString());
    	                preparedStatement.setString(3, player.getName());
    	                preparedStatement.setString(4, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(5, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method to remove a claim from sales
    public static boolean delChunkSale(Player player, Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return false;
    	Claim claim = listClaims.get(chunk);
    	claim.setSale(false);
    	claim.setPrice(0.0);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET isSale = false, SalePrice = 0  WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	                preparedStatement.setString(1, player.getUniqueId().toString());
    	                preparedStatement.setString(2, player.getName());
    	                preparedStatement.setString(3, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(4, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET isSale = false, SalePrice = 0  WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	                preparedStatement.setString(1, player.getUniqueId().toString());
    	                preparedStatement.setString(2, player.getName());
    	                preparedStatement.setString(3, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(4, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    	return true;
    }
    
    // Method when a claim is sold
    public static void sellChunk(Player player, Chunk chunk) {
    	if(!listClaims.containsKey(chunk)) return;
    	Claim claim = listClaims.get(chunk);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
		    	String playerName = player.getName();
		    	String owner = claim.getOwner();
		    	String claimName = claim.getName();
		    	double price = claim.getPrice();
		    	double balance = ClaimVault.getPlayerBalance(playerName);
		    	if(balance < price) {
		    		player.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> { player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money")); }, null);
		    		return;
		    	}
		    	ClaimVault.addPlayerBalance(owner, price);
		    	ClaimVault.removePlayerBalance(playerName, price);
		    	
		    	String uuid = "";
		    	Player ownerP = Bukkit.getPlayer(owner);
		    	if(ownerP == null) {
		    		OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
		    		uuid = ownerOP.getUniqueId().toString();
		    	} else {
		    		CPlayer cOwner = CPlayerMain.getCPlayer(owner);
		    		cOwner.setClaimsCount(cOwner.getClaimsCount()-1);
		    		uuid = ownerP.getUniqueId().toString();
		    	}
    	    	CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
    	    	cTarget.setClaimsCount(cTarget.getClaimsCount()+1);
    			int nextKey = findFreeId(playerName);
    	        Map<Chunk,String> newid = new HashMap<>();
    	        if(claimsId.get(playerName) != null) {
    	        	newid = new HashMap<>(claimsId.get(playerName));
    	        }
    	        newid.put(chunk, String.valueOf(nextKey));
    	        claimsId.put(playerName, newid);
    	        claimsId.get(owner).remove(chunk);
    	        if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    			claim.setOwner(playerName);
    			claim.setName("bought-"+claimName+"-"+String.valueOf(nextKey));
    			Set<String> members = new HashSet<>(claim.getMembers());
    	        if(!members.contains(playerName)) {
    	        	members.add(playerName);
    	        }
    	        members.remove(owner);
    	        claim.setMembers(members);
    	        String members_string = String.join(";", members);
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    	        Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    				for(Entity e : chunk.getEntities()) {
    					if(!(e instanceof Player)) continue;
    					Player p = (Player) e;
    					ClaimEventsEnterLeave.bossbarMessages(p, chunk, playerName);;
    				}
    	        });
    	        player.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
		            player.sendMessage(ClaimLanguage.getMessage("buy-claim-success").replaceAll("%name%", claimName).replaceAll("%price%", String.valueOf(price)).replaceAll("%owner%", owner));
		            player.closeInventory();
    	        }, null);
	            if(ownerP != null) {
	            	ownerP.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
	            		ownerP.sendMessage(ClaimLanguage.getMessage("claim-was-sold").replaceAll("%name%", claimName).replaceAll("%buyer%",playerName).replaceAll("%price%", String.valueOf(price)));
	            	}, null);
	            }
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0  WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, String.valueOf(nextKey));
    	            	preparedStatement.setString(2, player.getUniqueId().toString());
    	            	preparedStatement.setString(3, playerName);
    	                preparedStatement.setString(4, members_string);
    	                preparedStatement.setString(5, "bought-"+claimName+"-"+String.valueOf(nextKey));
    	                preparedStatement.setString(6, uuid);
    	                preparedStatement.setString(7, owner);
    	                preparedStatement.setString(8, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(9, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
		    	String playerName = player.getName();
		    	String owner = claim.getOwner();
		    	String claimName = claim.getName();
		    	double price = claim.getPrice();
		    	double balance = ClaimVault.getPlayerBalance(playerName);
		    	if(balance < price) {
		    		player.sendMessage(ClaimLanguage.getMessage("buy-but-not-enough-money"));
		    		return;
		    	}
		    	ClaimVault.addPlayerBalance(owner, price);
		    	ClaimVault.removePlayerBalance(playerName, price);
		    	
		    	String uuid = "";
		    	Player ownerP = Bukkit.getPlayer(owner);
		    	if(ownerP == null) {
		    		OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
		    		uuid = ownerOP.getUniqueId().toString();
		    	} else {
		    		CPlayer cOwner = CPlayerMain.getCPlayer(owner);
		    		cOwner.setClaimsCount(cOwner.getClaimsCount()-1);
		    		uuid = ownerP.getUniqueId().toString();
		    	}
    	    	CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
    	    	cTarget.setClaimsCount(cTarget.getClaimsCount()+1);
    			int nextKey = findFreeId(playerName);
    	        Map<Chunk,String> newid = new HashMap<>();
    	        if(claimsId.get(playerName) != null) {
    	        	newid = new HashMap<>(claimsId.get(playerName));
    	        }
    	        newid.put(chunk, String.valueOf(nextKey));
    	        claimsId.put(playerName, newid);
    	        claimsId.get(owner).remove(chunk);
    	        if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    			claim.setOwner(playerName);
    			claim.setName("bought-"+claimName+"-"+String.valueOf(nextKey));
    	        Set<String> members = new HashSet<>(claim.getMembers());
    	        if(!members.contains(playerName)) {
    	        	members.add(playerName);
    	        }
    	        members.remove(owner);
    	        claim.setMembers(members);
    	        String members_string = String.join(";", members);
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    	        Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), stask -> {
    				for(Entity e : chunk.getEntities()) {
    					if(!(e instanceof Player)) continue;
    					Player p = (Player) e;
    					ClaimEventsEnterLeave.bossbarMessages(p, chunk, playerName);;
    				}
		            player.sendMessage(ClaimLanguage.getMessage("buy-claim-success").replaceAll("%name%", claimName).replaceAll("%price%", String.valueOf(price)).replaceAll("%owner%", owner));
		            player.closeInventory();
		            if(ownerP != null) {
		            	ownerP.sendMessage(ClaimLanguage.getMessage("claim-was-sold").replaceAll("%name%", claimName).replaceAll("%buyer%",playerName).replaceAll("%price%", String.valueOf(price)));
		            }
    	        });
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0  WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, String.valueOf(nextKey));
    	            	preparedStatement.setString(2, player.getUniqueId().toString());
    	            	preparedStatement.setString(3, playerName);
    	                preparedStatement.setString(4, members_string);
    	                preparedStatement.setString(5, "bought-"+claimName+"-"+String.valueOf(nextKey));
    	                preparedStatement.setString(6, uuid);
    	                preparedStatement.setString(7, owner);
    	                preparedStatement.setString(8, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(9, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    }
    
    // Method to change the owner of a claim
    public static void setOwner(Player sender, String playerName, Chunk chunk, boolean msg) {
    	if(!listClaims.containsKey(chunk)) return;
    	Claim claim = listClaims.get(chunk);
    	if(SimpleClaimSystem.isFolia()) {
    		Bukkit.getAsyncScheduler().runNow(SimpleClaimSystem.getInstance(), task -> {
    			String owner = claim.getOwner();
    	    	String uuid = "";
    	    	Player ownerP = Bukkit.getPlayer(owner);
    	    	if(ownerP == null) {
    	    		OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
    	    		uuid = ownerOP.getUniqueId().toString();
    	    	} else {
    	    		CPlayer cOwner = CPlayerMain.getCPlayer(owner);
    	    		cOwner.setClaimsCount(cOwner.getClaimsCount()-1);
    	    		uuid = ownerP.getUniqueId().toString();
    	    	}
    	    	OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
    	    	if(player.isOnline()) {
    	    		CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
    	    		cTarget.setClaimsCount(cTarget.getClaimsCount()+1);
    	    	}
    			int nextKey = findFreeId(playerName);
    			claim.setOwner(playerName);
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    			claim.setName("claim-"+String.valueOf(nextKey));
    	        Map<Chunk,String> newid = new HashMap<>();
    	        if(claimsId.get(playerName) != null) {
    	        	newid = new HashMap<>(claimsId.get(playerName));
    	        }
    	        newid.put(chunk, String.valueOf(nextKey));
    	        claimsId.put(playerName, newid);
    	        claimsId.get(owner).remove(chunk);
    	        if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    	        Set<String> members = new HashSet<>(claim.getMembers());
    	        if(!members.contains(playerName)) {
    	        	members.add(playerName);
    	        }
    	        members.remove(owner);
    	        claim.setMembers(members);
    	        String members_string = String.join(";", members);
    	        Bukkit.getRegionScheduler().run(SimpleClaimSystem.getInstance(), chunk.getWorld(), chunk.getX(), chunk.getZ(), subtask -> {
    				for(Entity e : chunk.getEntities()) {
    					if(!(e instanceof Player)) continue;
    					Player p = (Player) e;
    					ClaimEventsEnterLeave.bossbarMessages(p, chunk, playerName);;
    				}
    	        });
    	        if(msg) {
        	        sender.getScheduler().run(SimpleClaimSystem.getInstance(), stask -> {
        	        	sender.sendMessage(ClaimLanguage.getMessage("setowner-success").replaceAll("%owner%", playerName));
        	        }, null);
    	        }
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0  WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, String.valueOf(nextKey));
    	            	preparedStatement.setString(2, player.getUniqueId().toString());
    	            	preparedStatement.setString(3, playerName);
    	                preparedStatement.setString(4, members_string);
    	                preparedStatement.setString(5, "claim-"+String.valueOf(nextKey));
    	                preparedStatement.setString(6, uuid);
    	                preparedStatement.setString(7, owner);
    	                preparedStatement.setString(8, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(9, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	} else {
    		Bukkit.getScheduler().runTaskAsynchronously(SimpleClaimSystem.getInstance(), task -> {
    			String owner = claim.getOwner();
    	    	String uuid = "";
    	    	Player ownerP = Bukkit.getPlayer(owner);
    	    	if(ownerP == null) {
    	    		OfflinePlayer ownerOP = Bukkit.getOfflinePlayer(owner);
    	    		uuid = ownerOP.getUniqueId().toString();
    	    	} else {
    	    		CPlayer cOwner = CPlayerMain.getCPlayer(owner);
    	    		cOwner.setClaimsCount(cOwner.getClaimsCount()-1);
    	    		uuid = ownerP.getUniqueId().toString();
    	    	}
    	    	OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
    	    	if(player.isOnline()) {
    	    		CPlayer cTarget = CPlayerMain.getCPlayer(playerName);
    	    		cTarget.setClaimsCount(cTarget.getClaimsCount()+1);
    	    	}
    			int nextKey = findFreeId(playerName);
    			claim.setOwner(playerName);
    			if(ClaimSettings.getBooleanSetting("dynmap")) ClaimDynmap.updateName(chunk);
    			if(ClaimSettings.getBooleanSetting("bluemap")) ClaimBluemap.updateName(chunk);
    			claim.setName("claim-"+String.valueOf(nextKey));
    	        Map<Chunk,String> newid = new HashMap<>();
    	        if(claimsId.get(playerName) != null) {
    	        	newid = new HashMap<>(claimsId.get(playerName));
    	        }
    	        newid.put(chunk, String.valueOf(nextKey));
    	        claimsId.put(playerName, newid);
    	        claimsId.get(owner).remove(chunk);
    	        if(claimsId.get(owner).isEmpty()) claimsId.remove(owner);
    	        Set<String> members = new HashSet<>(claim.getMembers());
    	        if(!members.contains(playerName)) {
    	        	members.add(playerName);
    	        }
    	        members.remove(owner);
    	        claim.setMembers(members);
    	        String members_string = String.join(";", members);
    	        Bukkit.getScheduler().runTask(SimpleClaimSystem.getInstance(), stask -> {
    				for(Entity e : chunk.getEntities()) {
    					if(!(e instanceof Player)) continue;
    					Player p = (Player) e;
    					ClaimEventsEnterLeave.bossbarMessages(p, chunk, playerName);;
    				}
    				if(msg) sender.sendMessage(ClaimLanguage.getMessage("setowner-success").replaceAll("%owner%", playerName));
    	        });
    			try (Connection connection = SimpleClaimSystem.getDataSource().getConnection()) {
    				String updateQuery = "UPDATE scs_claims SET id = ?, uuid = ?, name = ?, Members = ?, claim_name = ?, isSale = false, SalePrice = 0  WHERE uuid = ? AND name = ? AND X = ? AND Z = ?";
    	            try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
    	            	preparedStatement.setString(1, String.valueOf(nextKey));
    	            	preparedStatement.setString(2, player.getUniqueId().toString());
    	            	preparedStatement.setString(3, playerName);
    	                preparedStatement.setString(4, members_string);
    	                preparedStatement.setString(5, "claim-"+String.valueOf(nextKey));
    	                preparedStatement.setString(6, uuid);
    	                preparedStatement.setString(7, owner);
    	                preparedStatement.setString(8, String.valueOf(chunk.getX()));
    	                preparedStatement.setString(9, String.valueOf(chunk.getZ()));
    	                preparedStatement.executeUpdate();
    	            }
    	            
    	        } catch (SQLException e) {
    	            e.printStackTrace();
    	        }
    			return;
    		});
    	}
    }
    
    // Method to display chunk when claiming
    public static void displayChunk(Player player, Chunk chunk, boolean claim) {
    	Particle.DustOptions dustOptions;
    	if(!claim) {
	    	if(checkIfClaimExists(chunk)) {
	    		String playerName = player.getName();
	    		if(getOwnerInClaim(chunk).equals(playerName)){
	    			dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 2f);
	    		} else {
	    			dustOptions = new Particle.DustOptions(Color.fromRGB(255, 0, 0), 2f);
	    		}
	    	} else {
	    		dustOptions = new Particle.DustOptions(Color.fromRGB(255, 255, 255), 2f);
	    	}
    	} else {
    		dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 2f);
    	}
    	if(SimpleClaimSystem.isFolia()) {
    	    final int[] counter = {0};
    	    Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
    	        if (counter[0] >= 10) {
    	            task.cancel();
    	        }
    	        World world = player.getWorld();
    	        int xStart = chunk.getX() << 4;
    	        int zStart = chunk.getZ() << 4;
    	        int xEnd = xStart + 15;
    	        int zEnd = zStart + 15;
    	        int yStart = world.getMinHeight();
    	        int yEnd = world.getMaxHeight() - 1;

    	        for (int y = yStart; y <= yEnd; y++) {
    	            for (int x = xStart; x <= xEnd; x++) {
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd + 1), 1, 0, 0, 0, 0, dustOptions);
    	            }
    	            for (int z = zStart; z <= zEnd; z++) {
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd + 1, y, z), 1, 0, 0, 0, 0, dustOptions);
    	            }
    	        }
    	        counter[0]++;
    		}, 0, 500, TimeUnit.MILLISECONDS);
    	    return;
    	}
    	new BukkitRunnable() {
            int counter = 0;
            @Override
            public void run() {
                if (counter >= 10) {
                    this.cancel();
                }
                World world = player.getWorld();
                int xStart = chunk.getX() << 4;
                int zStart = chunk.getZ() << 4;
                int xEnd = xStart + 15;
                int zEnd = zStart + 15;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;
                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x <= xEnd; x++) {
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd + 1), 1, 0, 0, 0, 0, dustOptions);
    	            }
    	            for (int z = zStart; z <= zEnd; z++) {
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
    	                world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd + 1, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }
                counter++;
            }
        }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 10L);
    }
    
    // Method to display chunk when radius claiming
    public static void displayChunkBorderWithRadius(Player player, Chunk centralChunk, int radius) {
    	Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromRGB(0, 255, 0), 2f);
    	if(SimpleClaimSystem.isFolia()) {
    	    final int[] counter = {0};
    	    Bukkit.getAsyncScheduler().runAtFixedRate(SimpleClaimSystem.getInstance(), task -> {
    	    	if (counter[0] >= 10) {
                    task.cancel();
                }
                World world = player.getWorld();
                int xStart = (centralChunk.getX() - radius) << 4;
                int zStart = (centralChunk.getZ() - radius) << 4;
                int xEnd = (centralChunk.getX() + radius + 1) << 4;
                int zEnd = (centralChunk.getZ() + radius + 1) << 4;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;
                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z < zEnd; z++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }
                counter[0]++;
    	    }, 0, 500, TimeUnit.MILLISECONDS);
    	    return;
    	}
        new BukkitRunnable() {
            int counter = 0;
            @Override
            public void run() {
                if (counter >= 10) {
                    this.cancel();
                }
                World world = player.getWorld();
                int xStart = (centralChunk.getX() - radius) << 4;
                int zStart = (centralChunk.getZ() - radius) << 4;
                int xEnd = (centralChunk.getX() + radius + 1) << 4;
                int zEnd = (centralChunk.getZ() + radius + 1) << 4;
                int yStart = world.getMinHeight();
                int yEnd = world.getMaxHeight() - 1;
                for (int y = yStart; y <= yEnd; y++) {
                    for (int x = xStart; x < xEnd; x++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zStart), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, x, y, zEnd), 1, 0, 0, 0, 0, dustOptions);
                    }
                    for (int z = zStart; z < zEnd; z++) {
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xStart, y, z), 1, 0, 0, 0, 0, dustOptions);
                        world.spawnParticle(Particle.REDSTONE, new Location(world, xEnd, y, z), 1, 0, 0, 0, 0, dustOptions);
                    }
                }

                counter++;
            }
        }.runTaskTimer(SimpleClaimSystem.getInstance(), 0L, 10L);
    }
    
    // Method to send help for commands
    public static void getHelp(Player player, String help, String cmd) {
    	String help_msg = ClaimLanguage.getMessage("help-command."+cmd+"-"+help.toLowerCase());
    	if(!help_msg.isEmpty()) {
    		player.sendMessage(ClaimLanguage.getMessage("help-separator"));
    		player.sendMessage(help_msg);
    		player.sendMessage(ClaimLanguage.getMessage("help-separator"));
    		return;
    	}
    	player.sendMessage(ClaimLanguage.getMessage("sub-arg-not-found").replaceAll("%help-separator%", ClaimLanguage.getMessage("help-separator")).replaceAll("%arg%", help).replaceAll("%args%", String.join(", ", commandArgs)));
    	return;
    }
    
    // Method to get the direction (north, south, east or west)
	private static String getDirection(float yaw) {
	    yaw = yaw % 360;
	    if (yaw < 0) yaw += 360;
	    if (0 <= yaw && yaw < 45) return ClaimLanguage.getMessage("map-direction-south");
	    else if (45 <= yaw && yaw < 135) return ClaimLanguage.getMessage("map-direction-west");
	    else if (135 <= yaw && yaw < 225) return ClaimLanguage.getMessage("map-direction-north");
	    else if (225 <= yaw && yaw < 315) return ClaimLanguage.getMessage("map-direction-east");
	    else if (315 <= yaw && yaw < 360.0) return ClaimLanguage.getMessage("map-direction-south");
	    else return "Unknown";
	}
	
	// Method to get the map for a player
	public static void getMap(Player player, Chunk to) {
	    String direction = getDirection(player.getLocation().getYaw());
	    Chunk centerChunk = to;
	    int centerX = centerChunk.getX();
	    int centerZ = centerChunk.getZ();
	    boolean isClaimed = checkIfClaimExists(centerChunk);
	    String name = isClaimed ? ClaimLanguage.getMessage("map-actual-claim-name-message").replaceAll("%name%", getClaimNameByChunk(centerChunk)) : ClaimLanguage.getMessage("map-no-claim-name-message");
	    String coords = ClaimLanguage.getMessage("map-coords-message").replaceAll("%coords%", String.valueOf(centerChunk.getX())+","+String.valueOf(centerChunk.getZ()));
	    String directionS = ClaimLanguage.getMessage("map-direction-message").replaceAll("%direction%", direction);
	    String colorRelationNoClaim = ClaimLanguage.getMessage("map-no-claim-color");
	    StringBuilder mapMessage = new StringBuilder(name+" "+coords+" "+directionS+"\n"+colorRelationNoClaim);
	    String colorCursor = ClaimLanguage.getMessage("map-cursor-color");
	    String symbolNoClaim = ClaimLanguage.getMessage("map-symbol-no-claim");
	    String symbolClaim = ClaimLanguage.getMessage("map-symbol-claim");
	    String mapCursor = ClaimLanguage.getMessage("map-cursor");
	    World world = player.getWorld();
	    for (int dz = -4; dz <= 4; dz++) {
	        for (int dx = -12; dx <= 12; dx++) {
	            int[] offset = adjustDirection(dx, dz, direction);
	            int relX = offset[0];
	            int relZ = offset[1];
	            Chunk chunk = world.getChunkAt(centerX + relX, centerZ + relZ);
	            if(checkIfClaimExists(chunk)) {
	            	String color = getRelation(player,chunk);
	            	if(chunk.equals(centerChunk)) {
	            		mapMessage.append(color+mapCursor+colorRelationNoClaim);
	            		continue;
	            	}
	            	mapMessage.append(color+symbolClaim+colorRelationNoClaim);
	            	continue;
	            }
            	if(chunk.equals(centerChunk)) {
            		mapMessage.append(colorCursor+mapCursor+colorRelationNoClaim);
            		continue;
            	}
            	mapMessage.append(symbolNoClaim);
	        }
	        mapMessage.append("\n");
	    }
	    player.sendMessage(mapMessage.toString());
	}
	
	// Method to adjust direction (to get North, South, East or West)
	private static int[] adjustDirection(int dx, int dz, String direction) {
	    int relX = dx, relZ = dz;
	    if(direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-north"))) return new int[]{relX, relZ};
	    if(direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-south"))) {
            relX = -dx;
            relZ = -dz;
            return new int[]{relX, relZ};
	    }
	    if(direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-east"))) {
            relX = -dz;
            relZ = dx;
            return new int[]{relX, relZ};
	    }
	    if(direction.equalsIgnoreCase(ClaimLanguage.getMessage("map-direction-west"))) {
            relX = dz;
            relZ = -dx;
            return new int[]{relX, relZ};
	    }
	    return new int[]{relX, relZ};
	}
	
	// Method to get relation between a player and a claim
	public static String getRelation(Player player, Chunk chunk) {
		if(getClaimMembers(chunk).contains(player.getName())) {
			return ClaimLanguage.getMessage("map-claim-relation-member");
		}
		return ClaimLanguage.getMessage("map-claim-relation-visitor");
	}
}
