package com.schotzgoblin.listener;

import com.schotzgoblin.config.ConfigHandler;
import com.schotzgoblin.main.QuestSystem;
import com.schotzgoblin.utils.SignUtils;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.EquipmentSlot;

import static com.schotzgoblin.utils.SignUtils.*;


public class SignListener implements Listener {
    private final QuestSystem plugin;
    private final ConfigHandler config = ConfigHandler.getInstance();

    public SignListener() {
        this.plugin = QuestSystem.getInstance();
        this.updateNearbySignsOfOnlinePlayersDelayed();
    }

    @EventHandler
    void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        updateNearbySignsDelayed(player);
    }

    @EventHandler(
            ignoreCancelled = true
    )
    void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() == EquipmentSlot.HAND) {// 34
            if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.getClickedBlock() != null) {// 36
                Block block = event.getClickedBlock();// 37
                Material blockType = block.getType();// 38
                if (SignUtils.isSign(blockType)) {// 39
                    Player player = event.getPlayer();// 40
                    Sign sign = (Sign) block.getState();// 41
                    var signTitle = config.getStringAsync("sign-messages.quests-title").join();
                    if(((TextComponent)sign.getSide(Side.FRONT).line(0)).content().contains(signTitle))return;
                    if(playerSignChange.containsKey(player.getUniqueId())) {
                        playerSignChange.put(player.getUniqueId(), playerSignChange.get(player.getUniqueId()) + 1);
                    } else {
                        playerSignChange.put(player.getUniqueId(), 0);
                    }
                    player.playSound(player.getLocation(), Sound.BLOCK_BAMBOO_WOOD_PLACE, 1.0f, 1.0f);
                    sendSignUpdate(player, sign);// 42
                }
            }

        }
    }

    private void updateNearbySignsOfOnlinePlayersDelayed() {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                updateNearbySigns(player);
            }
        }, 2,20);
    }

    void updateNearbySignsDelayed(Player player) {
        Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (player.isOnline()) {
                updateNearbySigns(player);
            }
        }, 2,20);
    }
}
