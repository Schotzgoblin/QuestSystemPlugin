package com.schotzgoblin.database;


public class QuestReward implements Identifiable{
    private int id;

    private Quest quest;

    private Reward reward;

    public QuestReward() {
    }

    public QuestReward(Quest quest, Reward reward) {
        this.quest = quest;
        this.reward = reward;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public Reward getReward() {
        return reward;
    }

    public void setReward(Reward reward) {
        this.reward = reward;
    }
}