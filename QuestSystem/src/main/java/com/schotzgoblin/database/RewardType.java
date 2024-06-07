package com.schotzgoblin.database;

public class RewardType implements Identifiable{
    private int id;

    private String name;

    public RewardType() {
    }

    public RewardType(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}