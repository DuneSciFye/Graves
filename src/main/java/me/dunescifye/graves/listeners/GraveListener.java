package me.dunescifye.graves.listeners;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
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
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static me.dunescifye.graves.Graves.*;

public class GraveListener implements Listener {

    public void PlayerInteractAtEntityHandler(Graves plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private Inventory inv;
    private PersistentDataContainer container;
    private final Map<UUID, ArmorStand> clickArmorstands = new HashMap<>();
    private final Map<UUID, Block> clickedBlocks = new HashMap<>();
    private Map<Player, Inventory> openedInventories = new HashMap<>();
    private Multimap<String, Player> playersOnInventory = ArrayListMultimap.create(); //Grave ID, Player
    private Map<Player, PersistentDataContainer> clickedContainers = new HashMap<>();
    private Map<String, PersistentDataContainer> mappedContainers = new HashMap<>(); //Grave ID, PDC

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
            if (e.getHand() != EquipmentSlot.HAND) return;
            PersistentDataContainer container = new CustomBlockData(block, getPlugin());
            if (container.has(keyItems, DataType.ITEM_STACK_ARRAY)) {
                e.setCancelled(true);
                graveInteract(p, block);
            }
        }
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        Player p = (Player) e.getWhoClicked();
        if (!e.getInventory().equals(openedInventories.get(p))) return;

        final ItemStack clickedItem = e.getCurrentItem();
        final ItemStack cursorItem = e.getCursor();

        int slot = e.getRawSlot();

        if (slot < 27) {
            if (!cursorItem.getType().isAir() || e.getClick() == ClickType.NUMBER_KEY) {
                e.setCancelled(true);
                return;
            }
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            Bukkit.getScheduler().runTask(getPlugin(), () -> {
                ArrayList<ItemStack> containerContents = new ArrayList<>();
                for (ItemStack item : e.getInventory().getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        containerContents.add(item);
                    }
                }
                PersistentDataContainer container = clickedBlocks.containsKey(p.getUniqueId()) ?
                    new CustomBlockData(clickedBlocks.get(p.getUniqueId()), getPlugin()) :
                    clickArmorstands.get(p.getUniqueId()).getPersistentDataContainer();
                String graveID = container.get(keyGraveUUID, PersistentDataType.STRING);
                container.set(keyItems, DataType.ITEM_STACK_ARRAY, containerContents.toArray(new ItemStack[0]));
                mappedContainers.put(graveID, container);
                for (Player player : playersOnInventory.get(graveID)) {
                    if (player != p) {
                        System.out.println(player.getName());
                        Inventory inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");
                        PersistentDataContainer container1 = mappedContainers.get(graveID);
                        for (ItemStack item : container1.get(keyItems, DataType.ITEM_STACK_ARRAY)) inv.addItem(item);
                        player.openInventory(inv);
                    }
                }
            });
        } else {
            if (e.getClick().isShiftClick()) e.setCancelled(true);
        }
    }

    // Cancel dragging in our inventory
    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (e.getInventory().equals(inv)) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        Inventory inv = e.getInventory();
        if (inv.equals(openedInventories.get(p))) {
            UUID uuid = p.getUniqueId();
            ArrayList<ItemStack> containerContents = new ArrayList<>();
            for (ItemStack item : e.getInventory().getContents()) {
                if (item != null && !item.getType().isAir()) {
                    containerContents.add(item);
                }
            }
            if (containerContents.isEmpty()) {
                PersistentDataContainer container = clickedContainers.get(p);
                String graveID = container.get(keyGraveUUID, PersistentDataType.STRING);
                if (decentHologramsEnabled) DHAPI.removeHologram(graveID);
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));


                if (clickedBlocks.containsKey(uuid)) {
                    Block clickedBlock = clickedBlocks.get(uuid);
                    clickedBlock.setType(Material.AIR);
                } else if (clickArmorstands.containsKey(uuid)) {
                    ArmorStand armorStand = clickArmorstands.get(uuid);
                    armorStand.remove();
                }
                System.out.println(playersOnInventory);
                for (Player player : playersOnInventory.get(graveID)) {
                    if (player != p) {
                        System.out.println(player.getName());
                        player.closeInventory();
                    }
                }
                playersOnInventory.removeAll(graveID);
            }
            clickedBlocks.remove(uuid);
            clickArmorstands.remove(uuid);
            openedInventories.remove(p);
        }


    }


    private void graveInteract(Player p, ArmorStand armorStand) {
        PersistentDataContainer container = armorStand.getPersistentDataContainer();
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
        String graveID = container.get(keyGraveUUID, PersistentDataType.STRING);

        if (p.isSneaking()){
            for (ItemStack item : graveItems) {
                Item drop = p.getWorld().dropItem(p.getLocation(), item);
                drop.setPickupDelay(0);
            }
            if (container.has(keyStoredExp, DataType.INTEGER)) {
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));
            }
            armorStand.remove();
            if (decentHologramsEnabled) DHAPI.removeHologram(graveID);
        } else {
            Inventory inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");

            clickArmorstands.put(p.getUniqueId(), armorStand);

            for (ItemStack item : graveItems) inv.addItem(item);

            openedInventories.put(p, inv);
            playersOnInventory.put(graveID, p);
            clickedContainers.put(p, container);
            mappedContainers.put(graveID, container);

            p.openInventory(inv);
        }
    }
    private void graveInteract(Player p, Block block) {
        PersistentDataContainer container = new CustomBlockData(block, getPlugin());
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
        String graveID = container.get(keyGraveUUID, PersistentDataType.STRING);

        if (p.isSneaking()) {
            for (ItemStack item : graveItems) {
                Item drop = p.getWorld().dropItem(p.getLocation(), item);
                drop.setPickupDelay(0);
            }
            if (container.has(keyStoredExp, DataType.INTEGER)) {
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));
            }
            block.setType(Material.AIR);
            if (decentHologramsEnabled) DHAPI.removeHologram(graveID);
        } else {
            Inventory inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");

            clickedBlocks.put(p.getUniqueId(), block);

            for (ItemStack item : graveItems) inv.addItem(item);

            openedInventories.put(p, inv);
            playersOnInventory.put(graveID, p);
            clickedContainers.put(p, container);
            mappedContainers.put(graveID, container);
            p.openInventory(inv);
        }
    }

}
