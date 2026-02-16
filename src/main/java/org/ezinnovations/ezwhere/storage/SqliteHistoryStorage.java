package org.ezinnovations.ezwhere.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SqliteHistoryStorage implements HistoryStorage {
    private final JavaPlugin plugin;
    private final String jdbcUrl;

    public SqliteHistoryStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        File dbFile = new File(plugin.getDataFolder(), "history.db");
        this.jdbcUrl = "jdbc:sqlite:" + dbFile.getAbsolutePath();
        initialize();
    }

    private void initialize() {
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS where_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        uuid TEXT NOT NULL,
                        timestamp INTEGER NOT NULL,
                        world TEXT NOT NULL,
                        x INTEGER NOT NULL,
                        y INTEGER NOT NULL,
                        z INTEGER NOT NULL,
                        yaw REAL,
                        pitch REAL
                    )
                    """);
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_where_history_uuid_ts ON where_history(uuid, timestamp)");
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to initialize SQLite storage: " + e.getMessage());
        }
    }

    @Override
    public List<WhereSnapshot> load(UUID uuid) {
        List<WhereSnapshot> snapshots = new ArrayList<>();
        String sql = "SELECT timestamp, world, x, y, z, yaw, pitch FROM where_history WHERE uuid = ? ORDER BY timestamp ASC";
        try (Connection connection = DriverManager.getConnection(jdbcUrl);
             PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, uuid.toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Float yaw = rs.getObject("yaw", Float.class);
                    Float pitch = rs.getObject("pitch", Float.class);
                    snapshots.add(new WhereSnapshot(
                            Instant.ofEpochMilli(rs.getLong("timestamp")),
                            rs.getString("world"),
                            rs.getInt("x"),
                            rs.getInt("y"),
                            rs.getInt("z"),
                            yaw,
                            pitch
                    ));
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to load SQLite history for " + uuid + ": " + e.getMessage());
        }
        return snapshots;
    }

    @Override
    public void save(UUID uuid, List<WhereSnapshot> snapshots) {
        String deleteSql = "DELETE FROM where_history WHERE uuid = ?";
        String insertSql = "INSERT INTO where_history(uuid, timestamp, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            connection.setAutoCommit(false);

            try (PreparedStatement deletePs = connection.prepareStatement(deleteSql)) {
                deletePs.setString(1, uuid.toString());
                deletePs.executeUpdate();
            }

            try (PreparedStatement insertPs = connection.prepareStatement(insertSql)) {
                for (WhereSnapshot snapshot : snapshots) {
                    insertPs.setString(1, uuid.toString());
                    insertPs.setLong(2, snapshot.timestamp().toEpochMilli());
                    insertPs.setString(3, snapshot.world());
                    insertPs.setInt(4, snapshot.x());
                    insertPs.setInt(5, snapshot.y());
                    insertPs.setInt(6, snapshot.z());
                    if (snapshot.yaw() == null) {
                        insertPs.setNull(7, java.sql.Types.REAL);
                    } else {
                        insertPs.setFloat(7, snapshot.yaw());
                    }
                    if (snapshot.pitch() == null) {
                        insertPs.setNull(8, java.sql.Types.REAL);
                    } else {
                        insertPs.setFloat(8, snapshot.pitch());
                    }
                    insertPs.addBatch();
                }
                insertPs.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            plugin.getLogger().severe("Failed to save SQLite history for " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // connections are short-lived and closed per operation.
    }
}
