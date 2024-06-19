package com.schotzgoblin.database;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class Quest implements Identifiable{
    private int id;

    private String name;

    private String description;

    private int timeLimit;

    private int objectiveId;
    private Objective objective;

    private Set<QuestReward> questRewards;

    public Quest() {
    }

    public Quest(String name, String description, int timeLimit) {
        this.name = name;
        this.description = description;
        this.timeLimit = timeLimit;
    }

    public Objective getObjective() {
        return objective;
    }

    public void setObjective(Objective objective) {
        this.objective = objective;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getObjectiveId() {
        return objectiveId;
    }

    public void setObjectiveId(int objectiveId) {
        this.objectiveId = objectiveId;
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

    public Set<QuestReward> getQuestRewards() {
        return questRewards;
    }

    public void setQuestRewards(Set<QuestReward> questRewards) {
        this.questRewards = questRewards;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Quest quest = (Quest) o;
        return id == quest.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
