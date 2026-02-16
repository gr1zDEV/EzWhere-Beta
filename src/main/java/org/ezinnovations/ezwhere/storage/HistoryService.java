package org.ezinnovations.ezwhere.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryService {
    private final JavaPlugin plugin;
    private final HistoryStorage storage;
    private final int maxHistory;
    private final long cacheExpireMillis;
    private final ExecutorService ioExecutor;
    private final Map<UUID, CacheEntry> cache = new ConcurrentHashMap<>();

    public HistoryService(JavaPlugin plugin, HistoryStorage storage, int maxHistory) {
        this.plugin = plugin;
        this.storage = storage;
        this.maxHistory = maxHistory;
        this.cacheExpireMillis = Duration.ofMinutes(5).toMillis();
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "ezwhere-io");
            thread.setDaemon(true);
            return thread;
        });
    }

    public CompletableFuture<List<WhereSnapshot>> loadHistory(UUID uuid) {
        CacheEntry existing = cache.get(uuid);
        if (existing != null) {
            existing.touch();
            return CompletableFuture.completedFuture(existing.historyNewestFirst());
        }

        return CompletableFuture.supplyAsync(() -> {
            List<WhereSnapshot> loaded = storage.load(uuid);
            loaded.sort(Comparator.comparing(WhereSnapshot::timestamp));
            CacheEntry entry = new CacheEntry(loaded);
            cache.put(uuid, entry);
            return entry.historyNewestFirst();
        }, ioExecutor);
    }

    public void addSnapshot(UUID uuid, WhereSnapshot snapshot) {
        CacheEntry entry = cache.computeIfAbsent(uuid, ignored -> new CacheEntry(storage.load(uuid)));
        synchronized (entry) {
            entry.history.add(snapshot);
            while (entry.history.size() > maxHistory) {
                entry.history.remove(0);
            }
            entry.touch();
            List<WhereSnapshot> copy = new ArrayList<>(entry.history);
            CompletableFuture.runAsync(() -> storage.save(uuid, copy), ioExecutor)
                    .exceptionally(throwable -> {
                        plugin.getLogger().severe("Failed to save history for " + uuid + ": " + throwable.getMessage());
                        return null;
                    });
        }
    }

    public void flushAndClose() {
        for (Map.Entry<UUID, CacheEntry> cacheEntry : cache.entrySet()) {
            UUID uuid = cacheEntry.getKey();
            CacheEntry entry = cacheEntry.getValue();
            synchronized (entry) {
                storage.save(uuid, new ArrayList<>(entry.history));
            }
        }
        storage.close();
        ioExecutor.shutdown();
    }

    public void purgeExpiredEntries() {
        long now = System.currentTimeMillis();
        List<UUID> expired = new ArrayList<>();

        for (Map.Entry<UUID, CacheEntry> cacheEntry : cache.entrySet()) {
            if (now - cacheEntry.getValue().lastAccess > cacheExpireMillis) {
                expired.add(cacheEntry.getKey());
            }
        }

        for (UUID uuid : expired) {
            CacheEntry entry = cache.remove(uuid);
            if (entry != null) {
                synchronized (entry) {
                    List<WhereSnapshot> copy = new ArrayList<>(entry.history);
                    CompletableFuture.runAsync(() -> storage.save(uuid, copy), ioExecutor);
                }
            }
        }
    }

    private static class CacheEntry {
        private final List<WhereSnapshot> history;
        private volatile long lastAccess;

        private CacheEntry(List<WhereSnapshot> loadedHistory) {
            this.history = new ArrayList<>(loadedHistory);
            this.lastAccess = System.currentTimeMillis();
        }

        private synchronized List<WhereSnapshot> historyNewestFirst() {
            List<WhereSnapshot> copy = new ArrayList<>(history);
            copy.sort(Comparator.comparing(WhereSnapshot::timestamp).reversed());
            touch();
            return copy;
        }

        private void touch() {
            this.lastAccess = System.currentTimeMillis();
        }
    }
}
