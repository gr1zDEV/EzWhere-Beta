package org.ezinnovations.ezwhere;

import org.ezinnovations.ezwhere.commands.WhereCommand;
import org.ezinnovations.ezwhere.gui.HistoryGui;
import org.ezinnovations.ezwhere.storage.HistoryService;
import org.ezinnovations.ezwhere.storage.HistoryStorage;
import org.ezinnovations.ezwhere.storage.SqliteHistoryStorage;
import org.ezinnovations.ezwhere.storage.StorageMode;
import org.ezinnovations.ezwhere.storage.WhereSnapshot;
import org.ezinnovations.ezwhere.storage.YamlHistoryStorage;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class EZWhere extends JavaPlugin implements Listener {
    private FileConfiguration messages;
    private HistoryService historyService;
    private HistoryGui historyGui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResourceIfMissing("messages.yml");

        messages = YamlConfiguration.loadConfiguration(new File(getDataFolder(), "messages.yml"));

        StorageMode storageMode = StorageMode.fromConfig(getConfig().getString("storage", "YAML"));
        HistoryStorage historyStorage = storageMode == StorageMode.SQLITE
                ? new SqliteHistoryStorage(this)
                : new YamlHistoryStorage(this);

        historyService = new HistoryService(this, historyStorage, getConfig().getInt("max_history", 250));
        historyGui = new HistoryGui(
                getConfig().getString("gui.title", "EZWhere History"),
                getConfig().getString("date_time_format", "yyyy-MM-dd HH:mm:ss"),
                shouldStoreYawPitch()
        );

        WhereCommand whereCommand = new WhereCommand(this, historyService);
        if (getCommand("where") != null) {
            getCommand("where").setExecutor(whereCommand);
            getCommand("where").setTabCompleter(whereCommand);
        }

        getServer().getPluginManager().registerEvents(historyGui, this);
        getServer().getPluginManager().registerEvents(this, this);

        getServer().getScheduler().runTaskTimerAsynchronously(this,
                historyService::purgeExpiredEntries,
                20L * 60,
                20L * 60);

        getLogger().info("EZWhere enabled with storage mode: " + storageMode);
    }

    @Override
    public void onDisable() {
        if (historyService != null) {
            historyService.flushAndClose();
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("record_on_join", false)) {
            recordSnapshot(event.getPlayer());
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        if (getConfig().getBoolean("record_on_quit", false)) {
            recordSnapshot(event.getPlayer());
        }
    }

    private void recordSnapshot(Player player) {
        WhereSnapshot snapshot = WhereSnapshot.fromLocation(player.getLocation(), shouldStoreYawPitch());
        historyService.addSnapshot(player.getUniqueId(), snapshot);
    }

    private void saveResourceIfMissing(String resource) {
        File file = new File(getDataFolder(), resource);
        if (!file.exists()) {
            saveResource(resource, false);
        }
    }

    public FileConfiguration getMessages() {
        return messages;
    }

    public HistoryGui getHistoryGui() {
        return historyGui;
    }

    public boolean shouldStoreYawPitch() {
        return getConfig().getBoolean("store_yaw_pitch", false);
    }
}
