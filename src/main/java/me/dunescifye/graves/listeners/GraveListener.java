package me.dunescifye.graves.listeners;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import eu.decentsoftware.holograms.api.DHAPI;
import me.dunescifye.graves.Graves;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
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

import java.time.Duration;
import java.util.*;

import static me.dunescifye.graves.Graves.*;
import static me.dunescifye.graves.utils.CooldownManager.*;

public class GraveListener implements Listener {

    public void PlayerInteractAtEntityHandler(Graves plugin) {
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private final Map<UUID, ArmorStand> clickArmorstands = new HashMap<>();
    private final Map<UUID, Block> clickedBlocks = new HashMap<>();
    private final Map<Player, Inventory> openedInventories = new HashMap<>();
    private final Map<String, Inventory> linkedInventories = new HashMap<>(); //Grave ID, Inventory

    @EventHandler
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent e) {
        Player p = e.getPlayer();
        Entity entity = e.getRightClicked();
        if (entity instanceof ArmorStand armorStand) {
            if (armorStand.getPersistentDataContainer().has(keyItems, DataType.ITEM_STACK_ARRAY)) {
                e.setCancelled(true);
                if (hasCooldown(clickCooldowns, p.getUniqueId())) return;
                setCooldown(clickCooldowns, p.getUniqueId(), Duration.ofSeconds(1));
                PersistentDataContainer container = armorStand.getPersistentDataContainer();
                ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
                String graveID = container.get(keyGraveID, PersistentDataType.STRING);

                if (p.isSneaking() && graveItems != null){ //Crouch click to quickly obtain items
                    armorStand.remove(); //Delete armorstand grave
                    container.remove(keyItems);
                    //If grave has been opened, close inv for viewers
                    if (linkedInventories.containsKey(graveID)) {
                        List<HumanEntity> viewers = new ArrayList<>(linkedInventories.get(graveID).getViewers());
                        for (HumanEntity viewer : viewers) {
                            openedInventories.remove((Player) viewer);
                            viewer.closeInventory();
                        }
                        linkedInventories.remove(graveID);
                    }

                    for (ItemStack item : graveItems) {
                        Item drop = p.getWorld().dropItem(p.getLocation(), item);
                        drop.setPickupDelay(0);
                    }
                    if (container.has(keyStoredExp, PersistentDataType.INTEGER)) {
                        Integer storedExp = container.get(keyStoredExp, PersistentDataType.INTEGER);
                        if (storedExp != null) {
                            p.giveExp(storedExp);
                        }
                    }
                    if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID); //Remove hologram
                    linkedInventories.remove(graveID);
                } else {
                    Inventory inv;
                    if (!linkedInventories.containsKey(graveID)) {
                        inv = Bukkit.createInventory(null, 27, Component.text(container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave"));

                        assert graveItems != null;
                        for (ItemStack item : graveItems) inv.addItem(item); //Put grave items in our custom inv
                        linkedInventories.put(graveID, inv);
                    } else {
                        inv = linkedInventories.get(graveID);
                    }
                    openedInventories.put(p, inv); //Assign this inv to this player
                    clickArmorstands.put(p.getUniqueId(), armorStand); //Assign armor stand to player as clicked grave
                    p.openInventory(inv);
                }
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
                    if (hasCooldown(clickCooldowns, p.getUniqueId())) return;
                    setCooldown(clickCooldowns, p.getUniqueId(), Duration.ofSeconds(1));
                    PersistentDataContainer container = armorStand.getPersistentDataContainer();
                    ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
                    String graveID = container.get(keyGraveID, PersistentDataType.STRING);

                    if (p.isSneaking() && graveItems != null){ //Crouch click to quickly obtain items
                        armorStand.remove(); //Delete armorstand grave
                        container.remove(keyItems);
                        //If grave has been opened, close inv for viewers
                        if (linkedInventories.containsKey(graveID)) {
                            List<HumanEntity> viewers = new ArrayList<>(linkedInventories.get(graveID).getViewers());
                            for (HumanEntity viewer : viewers) {
                                openedInventories.remove((Player) viewer);
                                viewer.closeInventory();
                            }
                            linkedInventories.remove(graveID);
                        }

                        for (ItemStack item : graveItems) {
                            Item drop = p.getWorld().dropItem(p.getLocation(), item);
                            drop.setPickupDelay(0);
                        }
                        if (container.has(keyStoredExp, PersistentDataType.INTEGER)) {
                            Integer storedExp = container.get(keyStoredExp, PersistentDataType.INTEGER);
                            if (storedExp != null) {
                                p.giveExp(storedExp);
                            }
                        }
                        if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID); //Remove hologram
                        linkedInventories.remove(graveID);
                    } else {
                        Inventory inv;
                        if (!linkedInventories.containsKey(graveID)) {
                            inv = Bukkit.createInventory(null, 27, Component.text(container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave"));

                            assert graveItems != null;
                            for (ItemStack item : graveItems) inv.addItem(item); //Put grave items in our custom inv
                            linkedInventories.put(graveID, inv);
                        } else {
                            inv = linkedInventories.get(graveID);
                        }
                        openedInventories.put(p, inv); //Assign this inv to this player
                        clickArmorstands.put(p.getUniqueId(), armorStand); //Assign armor stand to player as clicked grave
                        p.openInventory(inv);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Block block = e.getClickedBlock();
        if (block == null || e.getHand() != EquipmentSlot.HAND) return;
        PersistentDataContainer container = new CustomBlockData(block, getPlugin());
        if (!container.has(keyItems, DataType.ITEM_STACK_ARRAY)) return;

        //We now know it is a grave
        e.setCancelled(true);
        if (hasCooldown(clickCooldowns, p.getUniqueId())) return;
        //Prevent spam clicking for item dupe
        setCooldown(clickCooldowns, p.getUniqueId(), Duration.ofSeconds(1));
        ItemStack[] graveItems = container.get(keyItems, DataType.ITEM_STACK_ARRAY);
        String graveID = container.get(keyGraveID, PersistentDataType.STRING);

        //Sneak to insta grab all items
        if (p.isSneaking()) {
            block.setType(Material.AIR);
            container.remove(keyItems);

            //If grave has been opened, close inv for viewers
            if (linkedInventories.containsKey(graveID)) {
                List<HumanEntity> viewers = new ArrayList<>(linkedInventories.get(graveID).getViewers());
                for (HumanEntity viewer : viewers) {
                    openedInventories.remove((Player) viewer);
                    viewer.closeInventory();
                }
                linkedInventories.remove(graveID);
            }

            //Give player grave items
            if (graveItems != null) {
                for (ItemStack item : graveItems) {
                    Item drop = p.getWorld().dropItem(p.getLocation(), item);
                    drop.setPickupDelay(0);
                }
            }

            //Give player any xp
            if (container.has(keyStoredExp, PersistentDataType.INTEGER)) {
                Integer storedExp = container.get(keyStoredExp, PersistentDataType.INTEGER);
                if (storedExp != null) p.giveExp(storedExp);
            }

            //Remove hologram
            if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID);
        } else {
            Inventory inv;
            if (!linkedInventories.containsKey(graveID)) {
                inv = Bukkit.createInventory(null, 27, Component.text(container.has(keyGraveOwner, PersistentDataType.STRING) ? container.get(keyGraveOwner, PersistentDataType.STRING) + "'s Grave" : "Unknown Grave"));

                clickedBlocks.put(p.getUniqueId(), block);

                assert graveItems != null;
                for (ItemStack item : graveItems) inv.addItem(item);
                linkedInventories.put(graveID, inv);
            } else {
                inv = linkedInventories.get(graveID);
            }
            openedInventories.put(p, inv); //Assign this inv to this player
            clickedBlocks.put(p.getUniqueId(), block); //Assign block to player as clicked grave

            p.openInventory(inv);
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
                container.set(keyItems, DataType.ITEM_STACK_ARRAY, containerContents.toArray(new ItemStack[0])); //Update PDC with new items
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
        if (openedInventories.get(p) == null || !inv.equals(openedInventories.get(p))) return;
        UUID uuid = p.getUniqueId();
        ArrayList<ItemStack> containerContents = new ArrayList<>();
        for (ItemStack item : e.getInventory().getContents()) {
            if (item != null && !item.getType().isAir()) {
                containerContents.add(item);
            }
        }

        // Delay removal to avoid ConcurrentModificationException
        Bukkit.getScheduler().runTask(getPlugin(), () -> {
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

            if (containerContents.isEmpty() && linkedInventories.containsKey(graveID)) { //If grave is now empty
                if (decentHologramsEnabled && graveID != null) DHAPI.removeHologram(graveID);

                if (container.has(keyStoredExp, PersistentDataType.INTEGER)) {
                    Integer storedExp = container.get(keyStoredExp, PersistentDataType.INTEGER);
                    if (storedExp != null) {
                        p.giveExp(storedExp);
                    }
                }

                if (clickedBlocks.containsKey(uuid)) {
                    Block clickedBlock = clickedBlocks.get(uuid);
                    clickedBlock.setType(Material.AIR);
                    clickedBlocks.remove(uuid);
                } else if (clickArmorstands.containsKey(uuid)) {
                    ArmorStand armorStand = clickArmorstands.get(uuid);
                    armorStand.remove();
                    clickArmorstands.remove(uuid);
                }

                List<HumanEntity> viewers = new ArrayList<>(linkedInventories.get(graveID).getViewers());
                for (HumanEntity viewer : viewers) {
                    viewer.closeInventory();
                }
                linkedInventories.remove(graveID);

            }
        });
    }

}
