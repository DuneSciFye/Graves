package me.dunescifye.graves;

import com.jeff_media.customblockdata.CustomBlockData;
import me.dunescifye.graves.files.Config;
import me.dunescifye.graves.listeners.PlayerDeathListener;
import me.dunescifye.graves.listeners.GraveListener;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;


public final class Graves extends JavaPlugin {

    public static final NamespacedKey keyItems = new NamespacedKey("graves", "items");
    public static final NamespacedKey keyStoredExp = new NamespacedKey("graves", "storedexp");
    public static final NamespacedKey keyGraveOwner = new NamespacedKey("graves", "graveowner");
    public static final NamespacedKey keyGraveUUID = new NamespacedKey("graves", "uuid");
    public static boolean decentHologramsEnabled = false;

    private static Graves plugin;

    public static Graves getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        plugin = this;
        new PlayerDeathListener().playerDeathHandler(this);
        new GraveListener().PlayerInteractAtEntityHandler(this);
        CustomBlockData.registerListener(this);
        Bukkit.getLogger().info("[Graves] Graves enabled");
        Config.setup(plugin);

        if (Bukkit.getPluginManager().isPluginEnabled("DecentHolograms")) {
            Bukkit.getLogger().info("[Graves] Detected DecentHolograms plugin, enabling holograms.");
            decentHologramsEnabled = true;
        }

    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }
}
