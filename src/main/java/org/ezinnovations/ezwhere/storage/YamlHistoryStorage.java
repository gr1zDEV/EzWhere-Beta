package org.ezinnovations.ezwhere.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class YamlHistoryStorage implements HistoryStorage {
    private final JavaPlugin plugin;
    private final File historyFolder;

    public YamlHistoryStorage(JavaPlugin plugin) {
        this.plugin = plugin;
        this.historyFolder = new File(plugin.getDataFolder(), "history");
        if (!historyFolder.exists() && !historyFolder.mkdirs()) {
            plugin.getLogger().warning("Could not create history folder: " + historyFolder.getAbsolutePath());
        }
    }

    @Override
    public List<WhereSnapshot> load(UUID uuid) {
        File file = new File(historyFolder, uuid + ".yml");
        if (!file.exists()) {
            return new ArrayList<>();
        }

        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection entries = yaml.getConfigurationSection("history");
        if (entries == null) {
            return new ArrayList<>();
        }

        List<WhereSnapshot> snapshots = new ArrayList<>();
        for (String key : entries.getKeys(false)) {
            ConfigurationSection section = entries.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            long timestamp = section.getLong("timestamp", 0L);
            String world = section.getString("world", "unknown");
            int x = section.getInt("x");
            int y = section.getInt("y");
            int z = section.getInt("z");
            Float yaw = section.contains("yaw") ? (float) section.getDouble("yaw") : null;
            Float pitch = section.contains("pitch") ? (float) section.getDouble("pitch") : null;

            snapshots.add(new WhereSnapshot(Instant.ofEpochMilli(timestamp), world, x, y, z, yaw, pitch));
        }

        snapshots.sort(Comparator.comparing(WhereSnapshot::timestamp));
        return snapshots;
    }

    @Override
    public void save(UUID uuid, List<WhereSnapshot> snapshots) {
        File file = new File(historyFolder, uuid + ".yml");
        YamlConfiguration yaml = new YamlConfiguration();
        for (int i = 0; i < snapshots.size(); i++) {
            WhereSnapshot snapshot = snapshots.get(i);
            String path = "history." + i;
            yaml.set(path + ".timestamp", snapshot.timestamp().toEpochMilli());
            yaml.set(path + ".world", snapshot.world());
            yaml.set(path + ".x", snapshot.x());
            yaml.set(path + ".y", snapshot.y());
            yaml.set(path + ".z", snapshot.z());
            if (snapshot.yaw() != null) {
                yaml.set(path + ".yaw", snapshot.yaw());
            }
            if (snapshot.pitch() != null) {
                yaml.set(path + ".pitch", snapshot.pitch());
            }
        }

        try {
            yaml.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save history for " + uuid + ": " + e.getMessage());
        }
    }

    @Override
    public void close() {
        // No resources to close.
    }
}
