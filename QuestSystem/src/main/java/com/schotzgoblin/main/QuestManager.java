package com.schotzgoblin.main;

import java.util.HashMap;
import java.util.Map;

public class QuestManager {
    private final QuestSystem plugin;
    private final DatabaseHandler databaseHandler;
    private Map<String, Quest> quests;

    public QuestManager(QuestSystem plugin, DatabaseHandler databaseHandler) {
        this.plugin = plugin;
        this.databaseHandler = databaseHandler;
        this.quests = new HashMap<>();
        loadQuests();
    }

    private void loadQuests() {
        this.quests = new HashMap<>();
    }

    public void createQuest(String name, String description, Map<String, Object> rewards) {
        // Create a new quest and store it in the database
    }

    public void deleteQuest(String name) {
        // Delete the quest from the database
    }

    public Quest getQuest(String name) {
        return quests.get(name);
    }

}
