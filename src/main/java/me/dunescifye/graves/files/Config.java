package me.dunescifye.graves.files;

import me.dunescifye.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.logging.Logger;

public class Config {

    public static int minimumItemsPercentageDropped, maximumItemsPercentageDropped, minimumExpPercentageDropped,
        maximumExpPercentageDropped, minimumExpPercentageOfDroppedRetained, maximumExpPercentageOfDroppedRetained;
    public static boolean decentHologramsHook;
    private static Logger logger;
    private static FileConfiguration config;

    public static void setup(Graves plugin) {
        config = plugin.getConfig();
        logger = Bukkit.getLogger();

        minimumItemsPercentageDropped = getConfigValue("minimumItemsPercentageDropped", 10, 0, 100);
        maximumItemsPercentageDropped = getConfigValue("maximumItemsPercentageDropped", 15, 0, 100);
        minimumExpPercentageDropped = getConfigValue("minimumExpPercentageDropped", 0, 0, 100);
        maximumExpPercentageDropped = getConfigValue("maximumExpPercentageDropped", 30, 0, 100);
        minimumExpPercentageOfDroppedRetained = getConfigValue("minimumExpPercentageOfDroppedRetained", 50, 0, 100);
        maximumExpPercentageOfDroppedRetained = getConfigValue("maximumExpPercentageOfDroppedRetained", 100, 0, 100);
        decentHologramsHook = getConfigValue("Hooks.DecentHolograms", true);


        plugin.saveDefaultConfig();
    }

    private static int getConfigValue(String path, int defaultValue, int minValue, int maxValue) {
        if (!config.isSet(path)) {
            config.set(path, defaultValue);
            return defaultValue;
        }

        String valueStr = config.getString(path);

        if (!valueStr.matches("-?\\d+(\\.\\d+)?")) {
            logger.warning("[Graves] " + path + " is not a valid number. Must be a number between " + minValue + " and " + maxValue + ". Found " + valueStr + ". Using default value of " + defaultValue + ".");
            return defaultValue;
        }

        int value = Integer.parseInt(valueStr);

        if (value < minValue || value > maxValue) {
            logger.warning("[Graves] " + path + " is out of range. Must be a number between " + minValue + " and " + maxValue + ". Found " + value + ". Using default value of " + defaultValue + ".");
            return defaultValue;
        }

        return value;
    }
    private static Boolean getConfigValue(String path, Boolean defaultValue) {
        if (!config.isSet(path)) {
            config.set(path, defaultValue);
            return defaultValue;
        }

        String valueStr = config.getString(path);

        if (!valueStr.matches("(true|false)")) {
            logger.warning("[Graves] " + path + " is not a valid boolean. Must be either true or false. Using default value of " + defaultValue + ".");
            return defaultValue;
        }

        return config.getBoolean(path);
    }
}
