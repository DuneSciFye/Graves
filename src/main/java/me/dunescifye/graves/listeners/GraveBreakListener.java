package me.dunescifye.graves.listeners;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import me.dunescifye.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.persistence.PersistentDataContainer;

import static me.dunescifye.graves.Graves.getPlugin;
import static me.dunescifye.graves.Graves.keyItems;

public class GraveBreakListener implements Listener {

    public void graveBreakHandler(Graves plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    // Checks keyItems (not keyGraveID) since keyGraveID is never cleared from a looted grave's
    // block, whereas keyItems is - using keyGraveID would keep treating that location as a grave forever.
    private boolean isActiveGrave(PersistentDataContainer container) {
        return container.has(keyItems, DataType.ITEM_STACK_ARRAY);
    }

    // Covers explosions where the block itself is the source (e.g. beds/respawn anchors)
    @EventHandler
    public void onBlockExplode(BlockExplodeEvent e) {
        Block b = e.getBlock();
        PersistentDataContainer container = new CustomBlockData(b, getPlugin());
        if (isActiveGrave(container)) e.setCancelled(true);
    }

    // Covers regular TNT/creeper explosions, which fire this event (via the exploding entity) rather than BlockExplodeEvent
    @EventHandler
    public void onEntityExplode(EntityExplodeEvent e) {
        e.blockList().removeIf(block -> isActiveGrave(new CustomBlockData(block, getPlugin())));
    }

    // Armorstand-based graves aren't blocks, so they need explosion damage cancelled directly
    @EventHandler
    public void onEntityDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof ArmorStand armorStand)) return;
        if (!isActiveGrave(armorStand.getPersistentDataContainer())) return;
        switch (e.getCause()) {
            case BLOCK_EXPLOSION, ENTITY_EXPLOSION -> e.setCancelled(true);
            default -> { }
        }
    }
}
