package com.schotzgoblin.database;


public class QuestReward implements Identifiable {
    private int id;

    private Quest quest;
    private int questId;

    private Reward reward;
    private int rewardId;

    public QuestReward() {
    }

    public QuestReward(Quest quest, Reward reward) {
        this.quest = quest;
        this.reward = reward;
    }

    public int getQuestId() {
        return questId;
    }

    public void setQuestId(int questId) {
        this.questId = questId;
    }

    public int getRewardId() {
        return rewardId;
    }

    public void setRewardId(int rewardId) {
        this.rewardId = rewardId;
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