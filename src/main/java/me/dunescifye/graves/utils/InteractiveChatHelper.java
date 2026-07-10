package me.dunescifye.graves.utils;

import com.loohp.interactivechat.api.InteractiveChatAPI;
import com.loohp.interactivechat.libs.net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class InteractiveChatHelper {

    public static void sendDeathMessage(Player player, List<ItemStack> drops, int x, int y, int z) throws Exception {
        Component start = Component.text(Utils.deathLocationText(x, y, z) + ", and dropped ");
        Component droppedItems = Component.empty();
        Component separator = Component.text(", ");
        int last = drops.size() - 1;
        for (int i = 0; i < last; i++) {
            ItemStack drop = drops.get(i);
            droppedItems = droppedItems.append(InteractiveChatAPI.createItemDisplayComponent(player, drop).append(separator));
            drop.setAmount(0);
        }
        droppedItems = droppedItems.append(InteractiveChatAPI.createItemDisplayComponent(player, drops.get(last)));
        drops.get(last).setAmount(0);
        InteractiveChatAPI.sendMessage(player, start.append(droppedItems));
    }
}
