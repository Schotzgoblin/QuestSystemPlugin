package com.schotzgoblin.main;

import com.schotzgoblin.database.Quest;

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
        this.quests = new ArrayList<>();
    }

    public void createQuest(String name, String description, Map<String, Object> rewards) {
        // Create a new quest and store it in the database
    }

    public void deleteQuest(String name) {
        // Delete the quest from the database
    }

    public Quest getQuest(String name) {
        return quests.stream().filter(quest -> quest.getName().equals(name)).findFirst().orElse(null);
    }

}
