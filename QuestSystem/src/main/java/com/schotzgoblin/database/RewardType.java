package com.schotzgoblin.database;
import jakarta.persistence.*;
import org.hibernate.mapping.Set;

@Entity
@Table(name = "reward_type")
public class RewardType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "name", nullable = false)
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