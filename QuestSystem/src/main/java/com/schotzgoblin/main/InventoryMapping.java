package com.schotzgoblin.main;

import org.bukkit.inventory.Inventory;

public class InventoryMapping {
    private Inventory inventory;
    private String type = "All Quests";
    private int menuSlot = 2;

    public InventoryMapping() {
    }

    public InventoryMapping(Inventory inventory, String type, int menuSlot) {
        this.inventory = inventory;
        this.type = type;
        this.menuSlot = menuSlot;
    }

    public InventoryMapping(Inventory inventory) {
        this.inventory = inventory;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getMenuSlot() {
        return menuSlot;
    }

    public void setMenuSlot(int menuSlot) {
        this.menuSlot = menuSlot;
    }
}
