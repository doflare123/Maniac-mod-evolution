package org.example.maniacrevolution.cloak;

public enum CloakStage {
    IDLE("idle", 0, true), DEPLOY("carpet_deploy", 20, false),
    HOVER("carpet_ride", 0, true), LAND("carpet_land", 20, false),
    DETACH("detach", 12, false), OUTBOUND("fly_to_target", 0, true),
    CAPTURE("capture_wrap", 15, false), FALL("victim_fall", 21, false),
    BOUND("bound_struggle", 0, true), RELEASE("release", 14, false),
    LAUNCH("return_launch", 12, false), RETURN("fly_return", 0, true),
    REATTACH("reattach", 13, false);

    public final String clip;
    public final int ticks;
    public final boolean loop;

    CloakStage(String clip, int ticks, boolean loop) {
        this.clip = clip;
        this.ticks = ticks;
        this.loop = loop;
    }

    public boolean attached() { return ordinal() <= DETACH.ordinal() || this == REATTACH; }
    public boolean binding() { return this == CAPTURE || this == FALL || this == BOUND; }
    public boolean hovering() { return this == DEPLOY || this == HOVER; }
}
