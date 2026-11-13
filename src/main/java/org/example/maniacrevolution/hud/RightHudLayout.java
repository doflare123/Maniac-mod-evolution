package org.example.maniacrevolution.hud;

/** Shared geometry of the computer/generator stack and the content below it. */
public final class RightHudLayout {
    public static final int TOP=5,GAP=4;
    private RightHudLayout() {}
    public static int generatorY(boolean computers) {return TOP+(computers?ComputerHackHud.HEIGHT+GAP:0);}
    public static int bottom(boolean computers,boolean generator) {
        return generator?generatorY(computers)+GeneratorChargeHud.HEIGHT:computers?TOP+ComputerHackHud.HEIGHT:0;
    }
}
