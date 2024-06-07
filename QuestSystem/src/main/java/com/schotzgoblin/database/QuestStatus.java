package com.schotzgoblin.database;


public class QuestStatus implements Identifiable{
    private int id;

    private String status;

    public QuestStatus() {
    }

    public QuestStatus(String status) {
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}