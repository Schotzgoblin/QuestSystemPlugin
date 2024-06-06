package com.schotzgoblin.database;

import com.schotzgoblin.main.QuestStatus;
import jakarta.persistence.*;

import java.util.Map;
import java.util.Set;

@Entity
@Table(name = "quest")
public class Quest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @Column(name = "time_limit")
    private int timeLimit;

    @Column(name = "objective")
    private String objective;

    @OneToMany(mappedBy = "quest")
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
