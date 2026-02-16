package org.ezinnovations.ezwhere.storage;

import org.bukkit.Location;

import java.time.Instant;

public record WhereSnapshot(
        Instant timestamp,
        String world,
        int x,
        int y,
        int z,
        Float yaw,
        Float pitch
) {
    public static WhereSnapshot fromLocation(Location location, boolean includeYawPitch) {
        Float yaw = includeYawPitch ? location.getYaw() : null;
        Float pitch = includeYawPitch ? location.getPitch() : null;
        return new WhereSnapshot(
                Instant.now(),
                location.getWorld() != null ? location.getWorld().getName() : "unknown",
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ(),
                yaw,
                pitch
        );
    }
}
