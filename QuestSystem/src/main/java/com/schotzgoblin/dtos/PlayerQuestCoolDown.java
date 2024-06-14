package com.schotzgoblin.dtos;

import com.schotzgoblin.database.PlayerQuest;

import java.util.Objects;

public class PlayerQuestCoolDown {
    private PlayerQuest playerQuest;
    private long coolDown;

    public PlayerQuestCoolDown(PlayerQuest playerQuest, long coolDown) {
        this.playerQuest = playerQuest;
        this.coolDown = coolDown;
    }

    public PlayerQuest getPlayerQuest() {
        return playerQuest;
    }

    public void setPlayerQuest(PlayerQuest playerQuest) {
        this.playerQuest = playerQuest;
    }

    public long getCoolDown() {
        return coolDown;
    }

    public void setCoolDown(long coolDown) {
        this.coolDown = coolDown;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerQuestCoolDown that = (PlayerQuestCoolDown) o;
        return Objects.equals(playerQuest, that.playerQuest);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(playerQuest);
    }
}
