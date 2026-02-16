package org.ezinnovations.ezwhere.storage;

public enum StorageMode {
    YAML,
    SQLITE;

    public static StorageMode fromConfig(String value) {
        if (value == null) {
            return YAML;
        }

        try {
            return StorageMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return YAML;
        }
    }
}
