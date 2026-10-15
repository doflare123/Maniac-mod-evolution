package org.example.maniacrevolution.techies;

import net.minecraft.world.item.Item;
import org.example.maniacrevolution.ModItems;

public enum MineType {
    RED("red", "Красная", 20, 5, 100, 10, 3, 40, 1, 1.0),
    GREEN("green", "Зелёная", 100, 10, 600, 4, 6, 60, 3, 3.0),
    BLUE("blue", "Синяя", 60, 20, 200, 10, 0, 40, 1, 1.0);

    public final String color, label;
    public final int install, cost, cooldown, activation, damage, slowTicks, slowLevel;
    /** Strengths explicitly approved after the user's playtest; feel still needs live tuning. */
    public final double push;
    MineType(String color, String label, int install, int cost, int cooldown, int activation,
             int damage, int slowTicks, int slowLevel, double push) {
        this.color=color; this.label=label; this.install=install; this.cost=cost;
        this.cooldown=cooldown; this.activation=activation; this.damage=damage;
        this.slowTicks=slowTicks; this.slowLevel=slowLevel; this.push=push;
    }
    public Item item() { return switch(this) {
        case RED -> ModItems.TECHIES_MINE_RED.get();
        case GREEN -> ModItems.TECHIES_MINE_GREEN.get();
        case BLUE -> ModItems.TECHIES_MINE_BLUE.get();
    }; }
}
