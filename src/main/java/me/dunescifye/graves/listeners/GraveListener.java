package me.dunescifye.graves.listeners;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import eu.decentsoftware.holograms.api.DHAPI;
import me.dunescifye.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;

import static me.dunescifye.graves.Graves.*;

public class GraveListener implements Listener {

    public void PlayerInteractAtEntityHandler(Graves plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private Inventory inv;
    private PersistentDataContainer container;

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getRightClicked();
        if (entity instanceof ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(keyItems, DataType.ITEM_STACK_ARRAY)) {
                e.setCancelled(true);
                graveInteract(p, armorStand);
            }
        }
    }

    @EventHandler
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();
        if (damager instanceof Player p) {
            if (entity instanceof ArmorStand armorStand) {
                if (armorStand.getPersistentDataContainer().has(keyItems)) {
                    e.setCancelled(true);
                    graveInteract(p, armorStand);
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block != null) {
            PersistentDataContainer container = new CustomBlockData(block, getPlugin());
            if (container.has(keyItems, DataType.ITEM_STACK_ARRAY)) {
                e.setCancelled(true);
                graveInteract(p, block);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(final InventoryClickEvent e) {
        if (!e.getInventory().equals(inv)) return;

        final ItemStack clickedItem = e.getCurrentItem();
        final ItemStack cursorItem = e.getCursor();

        int slot = e.getRawSlot();

        if (slot < 27) {
            if (!cursorItem.getType().isAir()) e.setCancelled(true);
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                ArrayList<ItemStack> containerContents = new ArrayList<>();
                for (ItemStack item : e.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        containerContents.add(item);
                    }
                }
                if (containerContents.isEmpty()) {
                    DHAPI.removeHologram(container.get(keyGraveUUID, PersistentDataType.STRING));

                } else {
                    container.set(keyItems, DataType.ITEM_STACK_ARRAY, containerContents.toArray(new ItemStack[0]));
                }
            });
        }
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryClick(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }


    private void graveInteract(Player p, ArmorStand armorStand) {
        container = armorStand.getPersistentDataContainer();
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);

        if (p.isSneaking()){
            for (ItemStack item : graveItems) {
                Item drop = p.getWorld().dropItem(p.getLocation(), item);
                drop.setPickupDelay(0);
            }
            if (container.has(keyStoredExp, DataType.INTEGER)) {
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));
            }
            armorStand.remove();
            DHAPI.removeHologram(container.get(keyGraveUUID, PersistentDataType.STRING));
        } else {
            inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");


            for (ItemStack item : graveItems) {
                inv.addItem(item);
            }
            p.openInventory(inv);
        }
    }
    private void graveInteract(Player p, Block block) {
        container = new CustomBlockData(block, getPlugin());
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);

        if (p.isSneaking()){
            for (ItemStack item : graveItems) {
                Item drop = p.getWorld().dropItem(p.getLocation(), item);
                drop.setPickupDelay(0);
            }
            if (container.has(keyStoredExp, DataType.INTEGER)) {
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));
            }
            block.setType(Material.AIR);
            DHAPI.removeHologram(container.get(keyGraveUUID, PersistentDataType.STRING));
        } else {
            inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");


            for (ItemStack item : graveItems) {
                inv.addItem(item);
            }
            p.openInventory(inv);
        }
    }

}
