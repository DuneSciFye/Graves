package me.dunescifye.graves.listeners;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.jeff_media.morepersistentdatatypes.datatypes.GenericDataType;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import eu.decentsoftware.holograms.api.DHAPI;
import me.dunescifye.graves.Graves;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.loohp.interactivechat.api.InteractiveChatAPI.createItemDisplayComponent;
import static me.dunescifye.graves.Graves.*;
import static me.dunescifye.graves.files.Config.*;
import static me.dunescifye.graves.utils.Utils.getPlayerExp;

import com.loohp.interactivechat.api.InteractiveChatAPI;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class PlayerDeathListener implements Listener {

    public void playerDeathHandler(Graves plugin){
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        e.setKeepInventory(true);
        e.setShouldDropExperience(false);

        Player p = e.getEntity();
        int exp = getPlayerExp(p);
        int expLoss = exp * ThreadLocalRandom.current().nextInt(minimumExpPercentageDropped, maximumExpPercentageDropped) / 100;
        e.setNewExp(exp - expLoss);

        List<ItemStack> drops = new ArrayList<>(e.getDrops());
        Collections.shuffle(drops); // Shuffle to ensure randomness
        e.getDrops().clear(); //Remove dropped items

        int dropCount = (int) Math.ceil(drops.size() * ThreadLocalRandom.current().nextDouble(minimumItemsPercentageDropped, maximumItemsPercentageDropped) / 100);
        List<ItemStack> newDrops = drops.subList(0, Math.min(dropCount, drops.size()));
        ItemStack[] dropsArray = newDrops.toArray(new ItemStack[0]);

        Component startComponent = Component.text()
            .content("You died at " + (int) (p.getX()) + ", " + (int) (p.getY()) + ", " + (int) (p.getZ()) + ", and dropped ")
            .build();

        if (newDrops.isEmpty()){
            InteractiveChatAPI.sendMessage(e.getEntity(), startComponent.append(Component.text("nothing.")));
            return;
        }

        //Summoning Grave
        PersistentDataContainer container = null;
        Location location = p.getLocation().add(0, 1.2, 0);
        if (p.getLocation().getBlock().getType() == Material.AIR) { //Block grave is air
            Block block = p.getLocation().getBlock();
            location = block.getLocation().toCenterLocation().add(0, 0.7, 0);
            block.setType(Material.PLAYER_HEAD);
            BlockState state = block.getState();
            if (state instanceof Skull skull) {
                skull.setOwningPlayer(p);
                skull.update();
                container = new CustomBlockData(block, getPlugin());
            }
        } else { //Armorstand grave if not air
            ArmorStand grave = (ArmorStand) p.getWorld().spawnEntity(p.getLocation().add(0, -1.2, 0), EntityType.ARMOR_STAND);
            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
            skullMeta.setOwningPlayer(p);
            skull.setItemMeta(skullMeta);
            grave.getEquipment().setHelmet(skull);
            grave.setGravity(false);
            grave.setVisible(false);

            container = grave.getPersistentDataContainer();
        }

        //Setting grave data
        String graveID = UUID.randomUUID().toString();
        int storedXP = (int) (expLoss * ThreadLocalRandom.current().nextDouble(0.5, 1));
        assert container != null;
        container.set(keyItems, DataType.ITEM_STACK_ARRAY, dropsArray);
        container.set(keyStoredExp, PersistentDataType.INTEGER, storedXP);
        container.set(keyGraveOwner, PersistentDataType.STRING, p.getName());
        container.set(keyGraveID, PersistentDataType.STRING, graveID);
        //Creating hologram
        if (decentHologramsEnabled) {
            DHAPI.createHologram(graveID, location, Arrays.asList(
                p.getName() + "'s Grave",
                "Stored XP: " + storedXP
            ));
        }

        //Message in chat for dropped items
        Component droppedItemsComponent = Component.text("");
        final Component separator = Component.text(", ");
        int lastItem = newDrops.size() - 1;
        try {
            for (int i = 0; i < lastItem; i++) {
                ItemStack drop = newDrops.get(i);
                droppedItemsComponent = droppedItemsComponent.append(createItemDisplayComponent(p, drop).append(separator));
                drop.setAmount(0);
            }
            droppedItemsComponent = droppedItemsComponent.append(createItemDisplayComponent(p, newDrops.get(lastItem)));
            newDrops.get(lastItem).setAmount(0);
        } catch (
            Exception ex) {
            throw new RuntimeException(ex);
        }
        InteractiveChatAPI.sendMessage(e.getEntity(), startComponent.append(droppedItemsComponent));

    }


}
