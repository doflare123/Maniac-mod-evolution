package org.example.maniacrevolution.warden;

/** Server timing and gesture token: no client-provided charge duration. */
public final class WardenCombatCycle {
    private long meleeUntil, waveUntil, begun = -1, leaseUntil;
    private int token;
    public boolean melee(long now) { if (charging() || meleeCooldown(now) > 0) return false; meleeUntil = now + WardenCombatRules.MELEE_CD; return true; }
    public boolean start(long now, int token) {
        if (token <= 0 || token == this.token || charging() || waveCooldown(now) > 0) return false;
        this.token = token; begun = now; leaseUntil = now + WardenCombatRules.INPUT_LEASE; return true;
    }
    public WardenCombatRules.Shot release(long now, int token) {
        if (!charging() || token != this.token) return null;
        var shot = now > leaseUntil ? null : WardenCombatRules.shot(now - begun);
        begun = -1;
        if (shot != null) waveUntil = now + WardenCombatRules.WAVE_CD;
        return shot;
    }
    public void cancel(int token) { if (this.token == token) begun = -1; }
    public void keepAlive(long now, int token) { if (charging() && this.token == token && now <= leaseUntil) leaseUntil = now + WardenCombatRules.INPUT_LEASE; }
    public void expire(long now) { if (charging() && now > leaseUntil) begun = -1; }
    public boolean charging() { return begun >= 0; }
    public int token() { return token; }
    public int charge(long now) { return charging() ? (int) Math.min(WardenCombatRules.MAX_CHARGE, Math.max(0, now - begun)) : 0; }
    public int meleeCooldown(long now) { return (int) Math.max(0, meleeUntil - now); }
    public int waveCooldown(long now) { return (int) Math.max(0, waveUntil - now); }
}
