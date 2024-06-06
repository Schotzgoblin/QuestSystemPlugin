package com.schotzgoblin.database;

import jakarta.persistence.*;
import org.hibernate.mapping.Set;

@Entity
@Table(name = "rewards")
public class Reward {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne
    @JoinColumn(name = "reward_type_id", nullable = false)
    private RewardType rewardType;

    @Column(name = "amount", nullable = false)
    private int amount;


    public Reward() {
    }

    public Reward(String name, RewardType rewardType, int amount) {
        this.name = name;
        this.rewardType = rewardType;
        this.amount = amount;
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