package me.dunescifye.graves.utils;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    public static final Map<UUID, Instant> clickCooldowns = new HashMap<>();

    // Set cooldown
    public static void setCooldown(Map<UUID, Instant>map, UUID key, Duration duration) {
        map.put(key, Instant.now().plus(duration));
    }

    // Check if cooldown has expired
    public static boolean hasCooldown(Map<UUID, Instant> map, UUID key) {
        Instant cooldown = map.get(key);
        return cooldown != null && Instant.now().isBefore(cooldown);
    }
}
