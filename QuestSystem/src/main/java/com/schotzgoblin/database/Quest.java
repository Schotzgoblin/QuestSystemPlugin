package com.schotzgoblin.database;
import java.util.Map;
import java.util.Set;

public class Quest implements Identifiable{
    private int id;

    private String name;

    private String description;

    private int timeLimit;

    private String objective;

    private Set<QuestReward> questRewards;

    public Quest() {
    }

    public Quest(String name, String description, int timeLimit, String objective) {
        this.name = name;
        this.description = description;
        this.timeLimit = timeLimit;
        this.objective = objective;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

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

    public int getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(int timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public Set<QuestReward> getQuestRewards() {
        return questRewards;
    }

    public void setQuestRewards(Set<QuestReward> questRewards) {
        this.questRewards = questRewards;
    }
}
