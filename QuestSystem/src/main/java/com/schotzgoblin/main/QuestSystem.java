package com.schotzgoblin.main;

import com.schotzgoblin.commands.AdminQuestCommand;
import com.schotzgoblin.commands.QuestCommand;
import com.schotzgoblin.listener.*;
import com.schotzgoblin.listener.edit.objective.EditAllObjectivesListener;
import com.schotzgoblin.listener.edit.objective.EditSingleObjectiveListener;
import com.schotzgoblin.listener.edit.quest.EditAllQuestsListener;
import com.schotzgoblin.listener.edit.quest.EditSingleQuestListener;
import com.schotzgoblin.listener.edit.reward.EditAllRewardsListener;
import com.schotzgoblin.listener.edit.reward.EditSingleRewardListener;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.command.CommandExecutor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestSystem extends JavaPlugin implements Listener, PluginMessageListener {

    private static final Logger logger = LoggerFactory.getLogger(QuestSystem.class);
    public final String bungeeCordChannelName = "BungeeCord";

    public static QuestSystem getInstance() {
        return getPlugin(QuestSystem.class);
    }

    @Override
    public void onLoad() {
        super.onLoad();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        Bukkit.getPluginManager().registerEvents(this, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, bungeeCordChannelName);
        Bukkit.getMessenger().registerIncomingPluginChannel(this, bungeeCordChannelName, this);
        registerAllListeners();
        registerCommand("quests",new QuestCommand());
        registerCommand("quest",new AdminQuestCommand());
    }

    private void registerAllListeners() {
        addListener(QuestManager.getInstance());
        addListener(new TrackPlayerQuestProgress());
        addListener(new EditAllQuestsListener());
        addListener(new EditSingleQuestListener());
        addListener(new EditAllRewardsListener());
        addListener(new EditSingleRewardListener());
        addListener(new EditAllObjectivesListener());
        addListener(new EditSingleObjectiveListener());
        addListener(new QuestNpc());
        addListener(new SignListener());
        addListener(new ParticalListener());
    }

    private void addListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }

    public void registerCommand(String commandName, CommandExecutor executor) {
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
        logger.info("Shutting down");
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
        if(event.getEntity()instanceof Player){
            event.setCancelled(true);
        }
    }

    @Override
    public void onPluginMessageReceived(@NotNull String s, @NotNull Player player, byte[] bytes) {
        logger.info("Received message from BungeeCord");
    }

}