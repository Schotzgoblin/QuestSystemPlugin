package com.schotzgoblin.main;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

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
        addListener(questManager);
        addListener(new TrackPlayerQuestProgress(this, questManager, databaseHandler));
        addListener(new QuestNpc(this,questManager,databaseHandler));
        registerCommand("quests",new QuestCommand(questManager));
        registerCommand("quest",new AdminQuestCommand(questManager,databaseHandler));
    }
    private void addListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        var commandKit = this.getCommand(commandName);
        if (commandKit != null) {
            commandKit.setExecutor(executor);
            if(executor instanceof Listener) {
                addListener((Listener) executor);
            }
        } else {
            getLogger().warning("Failed to register command: " + commandName);
        }
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
        player.setGameMode(GameMode.ADVENTURE);
//        player.teleport(new Location(player.getWorld(),65.5,142,44.5));
    }

    @EventHandler
    public void onEntityDamageEvent(EntityDamageEvent event) {
        if(event.getEntity()instanceof Player player){
            event.setCancelled(true);
        }
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