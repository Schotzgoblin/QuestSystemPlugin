package com.schotzgoblin.database;


public class PlayerQuest implements Identifiable{
    private int id;

    private String playerUuid;

    private Quest quest;

    private QuestStatus questStatus;

    public PlayerQuest() {
    }

    public PlayerQuest(String playerUuid, Quest quest, QuestStatus questStatus) {
        this.playerUuid = playerUuid;
        this.quest = quest;
        this.questStatus = questStatus;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPlayerUuid() {
        return playerUuid;
    }

    public void setPlayerUuid(String playerUuid) {
        this.playerUuid = playerUuid;
    }

    public Quest getQuest() {
        return quest;
    }

    public void setQuest(Quest quest) {
        this.quest = quest;
    }

    public QuestStatus getQuestStatus() {
        return questStatus;
    }

    public void setQuestStatus(QuestStatus questStatus) {
        this.questStatus = questStatus;
    }
}