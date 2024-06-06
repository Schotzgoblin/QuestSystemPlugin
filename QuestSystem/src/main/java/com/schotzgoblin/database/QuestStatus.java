package com.schotzgoblin.database;

import jakarta.persistence.*;
import org.hibernate.mapping.Set;

@Entity
@Table(name = "quest_status")
public class QuestStatus {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Column(name = "status", nullable = false)
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