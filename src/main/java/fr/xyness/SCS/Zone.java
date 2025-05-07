package fr.xyness.SCS;

import com.flowpowered.math.vector.Vector3i;
// import com.zaxxer.hikari.HikariDataSource; // may vary depending on server? Use javax.sql.Connection instead.
import fr.xyness.SCS.Types.Claim;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
// import org.bukkit.util.BoundingBox; // double (we don't want that)
import org.bukkit.*;  // Location etc (whatever is available in the Minecraft server implementation)
import org.bukkit.entity.Player;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;

/**
 * Represents a subregion within a parent {@link Claim}. Permissions defined in a Zone override the
 * parent claim’s permissions if an event occurs within the region defined by {@code this.boundingBox}.
 *
 * <p>This class models claim zones stored in the {@code scs_zones} table. The {@code parentID} field refers to the
 * parent claim’s ID, corresponding to {@code scs_zones.zone_id} in the database.
 *
 * <p>Setting permissions is done by the normal claim menu, but get and set calls initiated by GUI are both redirected
 * to the zone if the player is in an area's boundingBox. See also {@code updatePerm} in {@link ClaimMain}.
 * Only the owner is inherited in the initial implementation.
 *
 * <p>NOTE: implementing per-permission inheritance would require implementing an undefined flag character in the
 * permission serializer (override the serializer method in this subclass).
 */
public class Zone extends Claim {
    /** The auto-incremented ID added by the database system **/
    // public int autoID = -1;  // somewhat useless, but reserved for future use (should map to scs_zones.id AUTO value, *not* area.id which is scs_zones.zone_id)
    public static int highestAreaID = -1;
    public static boolean zoneTableIsLoaded = false;
    /** The ID of the parent {@link Claim} this area belongs to. Should correspond to a Claim object’s ID. */
    private int parentID = -1;
    private BoundingBox boundingBox = null;  // NOTE: me.ryanhamshire.GriefPrevention.util.BoundingBox is int which is what we need
    private final Map<String, Zone> zones = null;
    public Zone(BoundingBox boundingBox, Set<UUID> members, String name, String description, Map<String,LinkedHashMap<String, Boolean>> permissions, boolean sale, long price, Set<UUID> bans, int parentClaimID) {
        super(true);  // here for explicitness (A Boolean argument was added since Claim() runs regardless of super(), and we don't want Claim attributes set to null for anything except a Zone).
        this.parentID = parentClaimID;
        this.boundingBox = boundingBox.clone();
        this.uuid_owner = null;  // not applicable (use owner of parent)
        this.owner = null;  // not applicable (use owner of parent)
        this.members = new HashSet<>(members);
        this.name = name;
        this.description = description;
        this.permissions = new HashMap<>(permissions);
        this.sale = sale; // not implemented in initial implementation of Zone
        this.price = price; // not implemented in initial implementation of Zone
        this.bans = new HashSet<>(bans);
        this.id = getNextID();
    }

    public BoundingBox getBoundingBox() {
        return this.boundingBox;
    }

    /**
     * Get an area id (area.id is unique to the whole table, but name only has to be unique to claim).
     * @return next available area id
     */
    public static int getNextID() {
        if (!zoneTableIsLoaded)  // Set true after scs_zones is completely loaded & setHighestID is called on each
            throw new IllegalStateException("Only call this after loading scs_zones (which should increment highestAreaID for each entry)!");
        return ++highestAreaID;  // ++ is first to get *new* value
    }

    @Override
    public String toString() {
        return Zone.serializeBoundingBox(this.boundingBox);
    }

    /**
     * Sets the highest used area ID if necessary.
     * The result of calling this for each table row is that {@code highestAreaID} becomes prepared so getNextID works
     * (But you must still manually set {@code zoneTableIsLoaded} to {@code true} after the whole table is loaded).
     * @param id The id to affect {@code highestAreaID} if {@code id} is higher.
     * @return Whether the argument is the highest. The return is not useful for the scs_zones table load method unless
     *         the table is sorted by zone_id.
     */
    public static boolean setHighestIDIfGreater(int id) {
        if (id <= highestAreaID) {
            return false;
        }
        highestAreaID = id;
        return true;
    }

    /**
     * Convert a BoundingBox (typically an area) to a database string field.
     * @param boundingBox Any bukkit BoundingBox
     * @return A string ready to save to the database.
     */
    public static String serializeBoundingBox(BoundingBox boundingBox) {
        return String.format("(%s,%s,%s),(%s,%s,%s)",
                boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                boundingBox.getMaxY(), boundingBox.getMaxY(), boundingBox.getMaxZ()
        );
    }

    /**
     * Deserialize a Vector3
     * @param coordinates Comma-separated, 3 numbers
     * @return A 3D bukkit Vector (not java.util.Vector which is a generic)
     */
    public static Vector3i deserializeVector3i(String coordinates) {
        String[] parts = null;
        if (coordinates.startsWith("(") && coordinates.endsWith(")"))
            parts = coordinates.substring(1, coordinates.length()-1).split(",");
        else
            parts = coordinates.split(",");

        if (parts.length != 3) {
            throw new IllegalArgumentException(String.format(
                    "Bad Vector3 length: Expected 2 commas for 3 dimensions but got \"%s\"", coordinates));
        }
        return new Vector3i(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
        );
    }

    /**
     * Load a BoundingBox (typically an area) from a database string field.
     * @param boundingBoxStr A string of two 3D points formatted like "(0,0,0),(1,1,1)" (See serializeBoundingBox)
     * @return A bukkit BoundingBox
     */
    public static BoundingBox deserializeBoundingBox(String boundingBoxStr) {
        int limitsDelimiterIdx = boundingBoxStr.indexOf(",(");
        if (limitsDelimiterIdx <= 0) {
            throw new IllegalArgumentException(String.format(
                    "Missing \"),(\": Expected BoundingBox like \"(0,0,0),(1,1,1)\" but got \"%s\"", boundingBoxStr));
        }
        String minCoords = boundingBoxStr.substring(0, limitsDelimiterIdx);
        String maxCoords = boundingBoxStr.substring(limitsDelimiterIdx+1);  // +1 skips ","
        Vector3i minimums = deserializeVector3i(minCoords);
        Vector3i maximums = deserializeVector3i(maxCoords);
        return new BoundingBox(
                minimums.getX(), minimums.getY(), minimums.getZ(),
                maximums.getX(), maximums.getY(), maximums.getZ()
        );
    }

    /**
     * Get SQL for creating a table that can store an instance of this class
     * (no database action is performed here).
     * @param sql_flavor Set to "mysql" or "sqlite" to choose syntax.
     * @return SQL "CREATE TABLE..." command
     */
    public static String createTableSql(String sql_flavor) {
        // id_area, id_claim, name, description, boundingbox, members, permissions, bans

        // Use zone_id for queries since id is automatic:
        // - we want to be able to restore from backups and keep referential integrity
        // - we don't want to have to read the database after a write to know what the id was (compute zone_id early instead)
        String sql = "CREATE TABLE IF NOT EXISTS scs_zones " +
                "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "zone_id INT NOT NULL, " +
                "parent_id INT NOT NULL, " +
                "name VARCHAR(255) NOT NULL, " +
                "description VARCHAR(255) NOT NULL, " +
                "boundingbox VARCHAR(255) NOT NULL, " +
                "members TEXT NOT NULL, " +
                "permissions VARCHAR(510) NOT NULL, " +
                "for_sale TINYINT(1) NOT NULL DEFAULT 0, " +
                "sale_price INT NOT NULL DEFAULT 0, " +
                "bans TEXT NOT NULL DEFAULT '')";
        if (sql_flavor.equals("mysql")) return sql;
        else if (sql_flavor.equals("sqlite")) {
            sql = sql.replaceAll("INT AUTO_INCREMENT PRIMARY KEY", "INTEGER PRIMARY KEY AUTOINCREMENT");
        }
        else {
            throw new IllegalArgumentException(String.format("Expected mysql or sqlite, got %s", sql_flavor));
        }
        return sql;
    }

    /**
     * Inserts a new area (box within a claim) into the database.
     *
     * @param connection     Such as instance.getDataSource().getConnection() (HikariDataSource or other that implements
     *                       javax.sql.DataSource and java.io.CLoseable)
     * @param parent_id      the id of the parent claim in the scs_claims_1 table.
     * @param uuid           Owner uuid: Used if owner is not "*".
     * @param owner          Owner string: if "*", ignored and uuid is used.
     * @param zoneName       the name of the area (Claim instance must ensure it is not used in the specific claim yet).
     * @param description    the description of the area
     * @param boundingBox    the 3D area selected, in world block coordinates
     * @param permissions    Starting permissions: copy from parent, or instance.getSettings().getDefaultValuesCode("all")
     */
    private static boolean insertAreaIntoDatabase(Connection connection, int parent_id, String uuid, String owner, String zoneName, String description, BoundingBox boundingBox, String permissions) {
        try (PreparedStatement stmt = connection.prepareStatement(
                     "INSERT INTO scs_zones (zone_id, parent_claim_id, name, description, boundingbox, members, permissions, bans) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
            stmt.setInt(1, getNextID());
            stmt.setInt(2, parent_id);  // parent
            stmt.setString(3, zoneName);
            stmt.setString(4, description);
            stmt.setString(5, serializeBoundingBox(boundingBox));
            // TODO: Copy the following from the parent (keep same format conventions as claim, to reuse code)
            stmt.setString(6, owner.equals("*") ? "" : uuid);  // members
            stmt.setString(7, permissions);
            stmt.setString(8, ""); // bans
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean contains(Location position) {
        return boundingBox.contains(position);
    }

    /**
     * Store the given permissions string to the database (does not affect this.permissions).
     * @param connection
     * @param permissionsString
     * @return
     */
    public boolean dbUpdatePermissions(Connection connection, String permissionsString) {
        // Update the database
        String updateQuery = "UPDATE scs_zones SET permissions = ? WHERE zone_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, permissionsString);
            preparedStatement.setInt(2, this.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            // throw new RuntimeException(e);
            System.err.println(String.format("Failed to save new permissions (\"%s\") for area %s", permissionsString, getId()));
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean dbUpdateName(Connection connection) {
        // Update the database
        String updateQuery = "UPDATE scs_zones SET name = ? WHERE zone_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, this.getName());
            preparedStatement.setInt(2, this.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Set the parent to claim_id of a claim (does not affect database. Run dbUpdateParent after this if differs).
     * @param parentID
     * @returns true if changed, false if was already parentID.
     */
    public boolean setParent(int parentID) {
        if (parentID != this.parentID) {
            this.parentID = parentID;
            return true;
        }
        return false;
    }

    public boolean dbUpdateParent(Connection connection) {
        // Update the database
        String updateQuery = "UPDATE scs_zones SET parent_claim_id = ? WHERE zone_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setInt(1, this.parentID);
            preparedStatement.setInt(2, this.getId());
            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            System.err.println(String.format("Failed to save new parent %s for area %s", this.parentID, this.getId()));
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Update the permissions database field for this zone_id (does not affect this.members).
     * @param connection The database connection such as instance.getDataSource().getConnection()
     * @param membersString The members string, converted from iterable such as using {@code getMembersString()}
     *                      inherited from {@link Claim}.
     * @return
     */
    public boolean dbUpdateMembers(Connection connection, String membersString) {
        String updateQuery = "UPDATE scs_claims_1 SET members = ? WHERE zone_id = ?";
        try (PreparedStatement preparedStatement = connection.prepareStatement(updateQuery)) {
            preparedStatement.setString(1, membersString);
            preparedStatement.setInt(2, getId());
            preparedStatement.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println(String.format("Failed to set members of zone_id %s to \"%s\"", getId(), membersString));
            e.printStackTrace();
        }
        return false;
    }

    // The following are only valid for Claim not Zone, since they involve the owner, area list, etc.

    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void clearZones() { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone removeZone(String zoneName) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void dbDeleteZones(DataSource datasource) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void dbDeleteZone(DataSource datasource, String zoneName) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }

    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone popZoneOfPlayerGUI(Player player) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone popZoneOfPlayerGUI(UUID playerId) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone getZoneOfPlayerGUI(Player player) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone getZoneOfPlayerGUI(UUID playerId) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone setZoneOfGUIByLocation(Player player) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone getZone(String zoneName) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    @Deprecated
    public Zone getZoneById(Integer zoneID) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone getZoneAt(Location location) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone getZoneAt(Player player) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void setUUID(UUID uuid_owner) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void setChunks(Set<Chunk> chunks) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void setOwner(String owner) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void setLocation(Location location) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public UUID getUUID() { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Set<Chunk> getChunks() { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public String getOwner() { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Location getLocation() { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public void addChunk(Chunk chunk) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public int getZoneId(String zoneName) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Zone mergeAsUniqueName(Zone zone, String prefix) { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */
    @Deprecated
    public Map<String, Zone> getZones() { throw new UnsupportedOperationException("Call is only valid for Claim (superclass)"); }
    /** only valid for Claim, but this is a Zone */

//
//    public boolean create(HikariDataSource datasource) {
//        // String permissionsStr = ...;
//        return insertAreaIntoDatabase(datasource, id, parentID, uuid_owner, owner, name, description, boundingBox, permissionsStr);
//    }
//
//    public boolean update(HikariDataSource datasource) {
//
//    }
//    public int getAutoID() {
//        return this.autoID;
//    }

}
