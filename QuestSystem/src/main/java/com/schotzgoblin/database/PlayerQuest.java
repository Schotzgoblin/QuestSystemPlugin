package com.schotzgoblin.database;


public class PlayerQuest implements Identifiable{
    private int id;

    private String playerUuid;

    private Quest quest;
    private int questId;

    private QuestStatus questStatus;
    private int questStatusId;

    public PlayerQuest() {
    }

    public PlayerQuest(String playerUuid, Quest quest, QuestStatus questStatus) {
        this.playerUuid = playerUuid;
        this.quest = quest;
        this.questStatus = questStatus;
    }

    public int getQuestStatusId() {
        return questStatusId;
    }

    public void setQuestStatusId(int questStatusId) {
        this.questStatusId = questStatusId;
    }

    public int getQuestId() {
        return questId;
    }

    public void setQuestId(int questId) {
        this.questId = questId;
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

    @Override
    public String toString() {
        return "PlayerQuest{" +
                "id=" + id +
                ", playerUuid='" + playerUuid + '\'' +
                ", quest=" + questId +
                ", questStatus=" + questStatusId +
                '}';
    }
}