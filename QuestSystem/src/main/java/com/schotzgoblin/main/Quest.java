package com.schotzgoblin.main;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class Quest {
    private static final Gson gson = new Gson();
    private String name;
    private String description;
    private Map<String, Object> rewards; // could be a more specific type
    private QuestStatus status;
    private long timeLimit; // in milliseconds
    private String objective;

    public Quest(String name, String description, Map<String, Object> rewards, long timeLimit, String objective) {
        this.name = name;
        this.description = description;
        this.rewards = rewards;
        this.status = QuestStatus.NOT_STARTED;
        this.timeLimit = timeLimit;
        this.objective = objective;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getRewards() {
        return rewards;
    }

    public void setRewards(Map<String, Object> rewards) {
        this.rewards = rewards;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public boolean isCompleted() {
        return this.status == QuestStatus.COMPLETED;
    }
    public String serializeRewards() {
        return gson.toJson(rewards);
    }

    public static Map<String, Object> deserializeRewards(String rewardsJson) {
        Type type = new TypeToken<Map<String, Object>>() {}.getType();
        return gson.fromJson(rewardsJson, type);
    }

    public String serialize() {
        return String.format("{\"name\": \"%s\", \"description\": \"%s\", \"rewards\": %s, \"status\": \"%s\", \"timeLimit\": %d, \"objective\": \"%s\"}",
                name, description, rewards.toString(), status, timeLimit, objective);
    }
}
