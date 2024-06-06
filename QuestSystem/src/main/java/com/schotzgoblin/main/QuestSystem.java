package com.schotzgoblin.main;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public class QuestSystem extends JavaPlugin implements Listener, PluginMessageListener {
    final String bungeeCordChannelName = "BungeeCord";
    private QuestManager questManager;
    private DatabaseHandler databaseHandler;

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getLogger().info("Hello World");
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, bungeeCordChannelName);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, bungeeCordChannelName, this);

        databaseHandler = new DatabaseHandler();
        questManager = new QuestManager(this, databaseHandler);

        getCommand("quest").setExecutor(new QuestCommand(questManager));
    }

    @Override
    public void onDisable() {
        super.onDisable();
        Bukkit.getLogger().info("Shutting down");
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this, bungeeCordChannelName);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this, bungeeCordChannelName);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        var player = event.getPlayer();
        player.sendMessage(Component.text("Hello, " + event.getPlayer().getName() + "!"));
        player.sendMessage(Component.text("Click here to join other lobby world!").clickEvent(ClickEvent.callback(audience -> {
            sendPlayerToServer(player, "lobby2");
        })));
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
//        event.setCancelled(true);
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, @NotNull byte[] bytes) {

    }

    private void sendPlayerToServer(Player player, String serverName) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Connect");
        out.writeUTF(serverName);
        player.sendPluginMessage(this, bungeeCordChannelName, out.toByteArray());
    }
}