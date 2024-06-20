package com.schotzgoblin.test;

import com.schotzgoblin.database.Objective;
import com.schotzgoblin.database.PlayerQuest;
import com.schotzgoblin.database.Quest;
import com.schotzgoblin.enums.ObjectiveType;
import com.schotzgoblin.utils.Utils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class QuestSystemTest {
    @Test
    public void testGetTimeStringFromSecs() {
        assertEquals("00:00:00", Utils.getTimeStringFromSecs(0));
        assertEquals("00:01:00", Utils.getTimeStringFromSecs(60));
        assertEquals("01:00:00", Utils.getTimeStringFromSecs(3600));
        assertEquals("01:01:01", Utils.getTimeStringFromSecs(3661));
    }

    @Test
    public void testGetSecondsFromTimeString() {
        assertEquals(0, Utils.getSecondsFromTimeString("00:00:00"));
        assertEquals(60, Utils.getSecondsFromTimeString("00:01:00"));
        assertEquals(3600, Utils.getSecondsFromTimeString("01:00:00"));
        assertEquals(3661, Utils.getSecondsFromTimeString("01:01:01"));
    }

    @Test
    public void testCalculateProgress_Kill() {
        PlayerQuest playerQuest = new PlayerQuest();
        playerQuest.setQuest(new Quest());
        playerQuest.getQuest().setObjective(new Objective());
        playerQuest.getQuest().getObjective().setType(ObjectiveType.KILL.name());
        playerQuest.getQuest().getObjective().setCount(10);
        playerQuest.setProgress("5");

        assertEquals(0.5f, Utils.calculateProgress(playerQuest), 0.01f);
    }
}