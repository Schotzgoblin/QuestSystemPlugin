package com.schotzgoblin.database;

import jakarta.persistence.*;

@Entity
@Table(name = "player_quest")
public class PlayerQuest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "player_uuid", nullable = false)
    private String playerUuid;

    @ManyToOne
    @JoinColumn(name = "quest_id", nullable = false)
    private Quest quest;

    @ManyToOne
    @JoinColumn(name = "quest_status_id", nullable = false)
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