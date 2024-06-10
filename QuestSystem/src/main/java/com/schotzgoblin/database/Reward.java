package com.schotzgoblin.database;


public class Reward implements Identifiable {
    private int id;

    private String name;

    private RewardType rewardType;
    private int rewardTypeId;

    private int amount;
    private String value;


    public Reward() {
    }

    public Reward(String name, RewardType rewardType, int amount) {
        this.name = name;
        this.rewardType = rewardType;
        this.amount = amount;
    }

    public int getRewardTypeId() {
        return rewardTypeId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public void setRewardTypeId(int rewardTypeId) {
        this.rewardTypeId = rewardTypeId;
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

    public RewardType getRewardType() {
        return rewardType;
    }

    public void setRewardType(RewardType rewardType) {
        this.rewardType = rewardType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }
}