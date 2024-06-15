package com.schotzgoblin.enums;

public enum QuestStatus {
    NOT_STARTED,
    IN_PROGRESS,
    COMPLETED,
    CANCELED;

    public static QuestStatus fromString(String statusString) {
        try {
            return valueOf(statusString.toUpperCase());
        } catch (IllegalArgumentException e) {
            return NOT_STARTED; // Default to NOT_STARTED if statusString is invalid
        }
    }

}
