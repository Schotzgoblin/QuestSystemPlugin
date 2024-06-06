package com.schotzgoblin.main;

import com.schotzgoblin.database.Quest;
import com.schotzgoblin.database.Reward;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestManager {
    private final QuestSystem plugin;
    private final DatabaseHandler databaseHandler;
    private List<Quest> quests;

    public QuestManager(QuestSystem plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        this.quests = new ArrayList<>();
        loadQuests();
    }

    private void loadQuests() {
        this.quests = databaseHandler.getAll(Quest.class);
    }

    public void createQuest(String name, String description, List<Reward> rewards, String objective, int timeLimit) {
        Quest quest = new Quest(name, description, timeLimit, objective);
        databaseHandler.save(quest);
    }

    public void deleteQuest(String name) {
        Quest savedQuest = databaseHandler.getQuestByName(name);
        System.out.println("Saved Quest: " + savedQuest.getName());
        databaseHandler.delete(savedQuest);
    }

    public Quest getQuest(String name) {
        return quests.stream().filter(quest -> quest.getName().equals(name)).findFirst().orElse(null);
    }

}
