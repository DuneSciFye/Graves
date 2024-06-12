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

    private final Map<UUID, ArmorStand> clickArmorstands = new HashMap<>();
    private final Map<UUID, Block> clickedBlocks = new HashMap<>();
    private final Map<Player, Inventory> openedInventories = new HashMap<>();
    private final Multimap<String, Player> playersOnInventory = ArrayListMultimap.create(); //Grave ID, Player

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
        if (!e.getInventory().equals(openedInventories.get(p))) return; //Checks if the click is the grave inv

        final ItemStack clickedItem = e.getCurrentItem();
        final ItemStack cursorItem = e.getCursor();

        int slot = e.getRawSlot();

        if (slot < 27) { //If clicking in the grave container and not inv
            if (!cursorItem.getType().isAir() || e.getClick() == ClickType.NUMBER_KEY) { //Cancel moving something into the inv via numbers
                e.setCancelled(true);
                return;
            }
            if (clickedItem == null || clickedItem.getType().isAir()) return;

            Bukkit.getScheduler().runTask(getPlugin(), () -> { //Run a tick later to get updated contents
                Inventory updatedInv = e.getInventory();
                ArrayList<ItemStack> containerContents = new ArrayList<>(); //Get new contents of grave
                for (ItemStack item : updatedInv.getContents()) {
                    if (item != null && !item.getType().isAir()) {
                        containerContents.add(item);
                    }
                }
                //Obtain PDC
                PersistentDataContainer container = clickedBlocks.containsKey(p.getUniqueId()) ?
                    new CustomBlockData(clickedBlocks.get(p.getUniqueId()), getPlugin()) :
                    clickArmorstands.get(p.getUniqueId()).getPersistentDataContainer();
                String graveID = container.get(keyGraveID, PersistentDataType.STRING);
                container.set(keyItems, DataType.ITEM_STACK_ARRAY, containerContents.toArray(new ItemStack[0])); //Update PDC with new items
                //mappedContainers.put(graveID, container);
                for (Player player : playersOnInventory.get(graveID)) { //Update all players that have grave open
                    if (player != p) {
                        openedInventories.put(player, updatedInv);
                        player.openInventory(updatedInv); //Give them updated inv
                    }
                }
            });
        } else { //Cancel shift clicking something from their inv into the grave inv
            if (e.getClick().isShiftClick()) e.setCancelled(true);
        }
    }

    // Cancel dragging in graves
    @EventHandler
    public void onInventoryDrag(final InventoryDragEvent e) {
        if (e.getInventory().equals(openedInventories.get((Player) e.getWhoClicked()))) {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent e) {
        Player p = (Player) e.getPlayer();
        Inventory inv = e.getInventory();
        if (!inv.equals(openedInventories.get(p))) return;

        UUID uuid = p.getUniqueId();
        ArrayList<ItemStack> containerContents = new ArrayList<>();
        for (ItemStack item : e.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                containerContents.add(item);
            }
        }

        openedInventories.remove(p);

        //Obtain PDC
        PersistentDataContainer container;
        if (clickedBlocks.containsKey(uuid))
            container = new CustomBlockData(clickedBlocks.get(p.getUniqueId()), getPlugin());
        else if (clickArmorstands.containsKey(uuid))
            container = clickArmorstands.get(p.getUniqueId()).getPersistentDataContainer();
        else
            return;

        String graveID = container.get(keyGraveID, PersistentDataType.STRING);

        if (containerContents.isEmpty()) { //If grave is now empty
            if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID);
            p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));

            if (clickedBlocks.containsKey(uuid)) {
                Block clickedBlock = clickedBlocks.get(uuid);
                clickedBlock.setType(Material.AIR);
                clickedBlocks.remove(uuid);
            } else if (clickArmorstands.containsKey(uuid)) {
                ArmorStand armorStand = clickArmorstands.get(uuid);
                armorStand.remove();
                clickArmorstands.remove(uuid);
            }
            for (Player player : playersOnInventory.get(graveID)) {
                if (player != p) {
                    clickArmorstands.remove(player.getUniqueId());
                    clickedBlocks.remove(player.getUniqueId());
                    player.closeInventory();
                }
            }
        }
        playersOnInventory.remove(graveID, p); //Remove player from playersOnInv

    }


    private void graveInteract(Player p, ArmorStand armorStand) {
        PersistentDataContainer container = armorStand.getPersistentDataContainer();
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
        String graveID = container.get(keyGraveID, PersistentDataType.STRING);

        if (p.isSneaking() && graveItems != null){ //Crouch click to quickly obtain items
            for (ItemStack item : graveItems) {
                Item drop = p.getWorld().dropItem(p.getLocation(), item);
                drop.setPickupDelay(0);
            }
            if (container.has(keyStoredExp, DataType.INTEGER)) {
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));
            }
            if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID); //Remove hologram
            armorStand.remove(); //Delete armorstand grave
        } else {
            Inventory inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");

            for (ItemStack item : graveItems) inv.addItem(item); //Put grave items in our custom inv

            openedInventories.put(p, inv); //Assign this inv to this player
            clickArmorstands.put(p.getUniqueId(), armorStand); //Assign armor stand to player as clicked grave
            playersOnInventory.put(graveID, p); //Assign the player to this grave

            p.openInventory(inv);
        }
    }
    private void graveInteract(Player p, Block block) {
        PersistentDataContainer container = new CustomBlockData(block, getPlugin());
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
        String graveID = container.get(keyGraveID, PersistentDataType.STRING);

        if (p.isSneaking() && graveItems != null) {
            for (ItemStack item : graveItems) {
                Item drop = p.getWorld().dropItem(p.getLocation(), item);
                drop.setPickupDelay(0);
            }
            if (container.has(keyStoredExp, DataType.INTEGER)) {
                p.giveExp(container.get(keyStoredExp, PersistentDataType.INTEGER));
            }
            block.setType(Material.AIR);
            if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID);
        } else {
            Inventory inv = Bukkit.createInventory(null, 27, container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave");

            clickedBlocks.put(p.getUniqueId(), block);

            for (ItemStack item : graveItems) inv.addItem(item);

            openedInventories.put(p, inv); //Assign this inv to this player
            clickedBlocks.put(p.getUniqueId(), block); //Assign block to player as clicked grave
            playersOnInventory.put(graveID, p); //Assign the player to this grave

            p.openInventory(inv);
        }
    }

}
