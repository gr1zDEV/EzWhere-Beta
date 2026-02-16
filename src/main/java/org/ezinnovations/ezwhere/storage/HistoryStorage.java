package org.ezinnovations.ezwhere.storage;

import java.util.List;
import java.util.UUID;

public interface HistoryStorage {
    List<WhereSnapshot> load(UUID uuid);

    void save(UUID uuid, List<WhereSnapshot> snapshots);

    void close();
}
