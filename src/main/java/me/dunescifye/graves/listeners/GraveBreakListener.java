package me.dunescifye.graves.listeners;

import com.jeff_media.customblockdata.CustomBlockData;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;

import static me.dunescifye.graves.Graves.getPlugin;
import static me.dunescifye.graves.Graves.keyGraveID;

public class GraveBreakListener implements Listener {

    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        Block b = e.getBlock();
        PersistentDataContainer container = new CustomBlockData(b, getPlugin());
        if (container.has(keyGraveID)) e.setCancelled(true);
    }
}
