package com.schotzgoblin.database;

import java.util.Objects;

public class Objective implements Identifiable{
    private int id;
    private String objective;
    private String type;
    private String value;
    private int count;

    public Objective() {
    }

    public Objective(String objective, String type, String value, int count) {
        this.objective = objective;
        this.type = type;
        this.value = value;
        this.count = count;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public void setId(int id) {
        this.id = id;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Objective objective = (Objective) o;
        return id == objective.id;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
