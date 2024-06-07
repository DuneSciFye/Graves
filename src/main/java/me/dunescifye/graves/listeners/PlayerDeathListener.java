package me.dunescifye.graves.listeners;

import com.jeff_media.customblockdata.CustomBlockData;
import com.jeff_media.morepersistentdatatypes.DataType;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import eu.decentsoftware.holograms.api.DHAPI;
import me.dunescifye.graves.Graves;
import org.bukkit.Bukkit;
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
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static com.loohp.interactivechat.api.InteractiveChatAPI.createItemDisplayComponent;
import static me.dunescifye.graves.Graves.*;
import static me.dunescifye.graves.files.Config.*;

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
        e.getDrops().clear(); //Remove dropped items

        Player p = e.getEntity();
        int expLoss = (int) (p.getTotalExperience() * ThreadLocalRandom.current().nextDouble(0, 0.30));
        e.setNewTotalExp(p.getTotalExperience() - expLoss);

        List<ItemStack> drops = new ArrayList<>(List.of(p.getInventory().getContents()));
        Collections.shuffle(drops); // Shuffle to ensure randomness

        int dropCount = (int) Math.ceil(drops.size() * ThreadLocalRandom.current().nextDouble(minimumItemsPercentageDropped, maximumItemsPercentageDropped));
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
        if (p.getLocation().getBlock().getType() == Material.AIR) { //Block grave is air
            Block block = p.getLocation().getBlock();
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
            skullMeta.setOwner(p.getName());
            skull.setItemMeta(skullMeta);
            grave.getEquipment().setHelmet(skull);
            grave.setGravity(false);
            grave.setVisible(false);

            container = grave.getPersistentDataContainer();
        }
        //Setting grave data
        String graveID = UUID.randomUUID().toString();
        int storedXP = (int) (expLoss * ThreadLocalRandom.current().nextDouble(0.5, 1));
        container.set(keyItems, DataType.ITEM_STACK_ARRAY, dropsArray);
        container.set(keyStoredExp, PersistentDataType.INTEGER, storedXP);
        container.set(keyGraveOwner, PersistentDataType.STRING, p.getName());
        container.set(keyGraveUUID, PersistentDataType.STRING, graveID);
        //Creating hologram
        if (decentHologramsEnabled) {
            DHAPI.createHologram(graveID, p.getLocation().add(0, 1.2, 0), Arrays.asList(
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
                p.getInventory().removeItem(drop);
                droppedItemsComponent = droppedItemsComponent.append(createItemDisplayComponent(p, drop).append(separator));
            }
            p.getInventory().removeItem(newDrops.get(lastItem));
            droppedItemsComponent = droppedItemsComponent.append(createItemDisplayComponent(p, newDrops.get(lastItem)));
        } catch (
            Exception ex) {
            throw new RuntimeException(ex);
        }
        InteractiveChatAPI.sendMessage(e.getEntity(), startComponent.append(droppedItemsComponent));

    }


}
