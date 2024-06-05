package com.schotzgoblin.main;

public class Quest {
    private String name;
    private String description;
    private Map<String, Object> rewards; // could be a more specific type
    private QuestStatus status;
    private long timeLimit; // in milliseconds
    private String objective;

    public Quest(String name, String description, Map<String, Object> rewards, long timeLimit, String objective) {
        this.name = name;
        this.description = description;
        this.rewards = rewards;
        this.status = QuestStatus.NOT_STARTED;
        this.timeLimit = timeLimit;
        this.objective = objective;
    }

    // Getters and setters
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

    public Map<String, Object> getRewards() {
        return rewards;
    }

    public void setRewards(Map<String, Object> rewards) {
        this.rewards = rewards;
    }

    public QuestStatus getStatus() {
        return status;
    }

    public void setStatus(QuestStatus status) {
        this.status = status;
    }

    public long getTimeLimit() {
        return timeLimit;
    }

    public void setTimeLimit(long timeLimit) {
        this.timeLimit = timeLimit;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    // Method to check if the quest is completed
    public boolean isCompleted() {
        return this.status == QuestStatus.COMPLETED;
    }

    // Serialization to store in database
    public String serialize() {
        // Convert the quest object to a string representation for storage
        // Example: JSON or another format
        return String.format("{\"name\": \"%s\", \"description\": \"%s\", \"rewards\": %s, \"status\": \"%s\", \"timeLimit\": %d, \"objective\": \"%s\"}",
                name, description, rewards.toString(), status, timeLimit, objective);
    }

    // Deserialization to create a quest object from a string
    public static Quest deserialize(String serializedQuest) {
        // Convert the string representation back to a Quest object
        // Example: Parse JSON or another format
        // This is just a placeholder, implement the actual deserialization logic
        return null;
    }
}
