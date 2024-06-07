package me.dunescifye.graves.files;

import me.dunescifye.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class Config {

    public static int minimumItemsPercentageDropped, maximumItemsPercentageDropped, minimumExpPercentageDropped,
        maximumExpPercentageDropped, minimumExpPercentageOfDroppedRetained, maximumExpPercentageOfDroppedRetained;
    private static Logger logger;

    public static void setup(Graves plugin) {
        FileConfiguration config = plugin.getConfig();
        logger = Bukkit.getLogger();

        minimumItemsPercentageDropped = getConfigValue(config, "minimumItemsPercentageDropped", 10, 0, 100);
        maximumItemsPercentageDropped = getConfigValue(config, "maximumItemsPercentageDropped", 15, 0, 100);
        minimumExpPercentageDropped = getConfigValue(config, "minimumExpPercentageDropped", 0, 0, 100);
        maximumExpPercentageDropped = getConfigValue(config, "maximumExpPercentageDropped", 30, 0, 100);
        minimumExpPercentageOfDroppedRetained = getConfigValue(config, "minimumExpPercentageOfDroppedRetained", 50, 0, 100);
        maximumExpPercentageOfDroppedRetained = getConfigValue(config, "maximumExpPercentageOfDroppedRetained", 100, 0, 100);

        plugin.saveDefaultConfig();
    }

    private static int getConfigValue(FileConfiguration config, String path, int defaultValue, int minValue, int maxValue) {
        if (!config.isSet(path)) {
            config.set(path, defaultValue);
            return defaultValue;
        } else {
            int value = config.getInt(path);
            if (value < minValue || value > maxValue) {
                logger.warning(path + " is out of range. Must be a number between " + minValue + " and " + maxValue + ". Found " + value + ". Using default value of " + defaultValue + ".");
                return defaultValue;
            } else {
                return value;
            }
        }
    }
}
