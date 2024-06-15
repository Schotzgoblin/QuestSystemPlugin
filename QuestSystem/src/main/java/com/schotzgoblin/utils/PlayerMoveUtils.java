package com.schotzgoblin.utils;

import com.schotzgoblin.database.PlayerQuest;

import java.util.*;

public class PlayerMoveUtils {
    public static Map<UUID, List<PlayerQuest>> playerQuestConfig = new HashMap<>(Collections.synchronizedMap(new HashMap<>()));
}
