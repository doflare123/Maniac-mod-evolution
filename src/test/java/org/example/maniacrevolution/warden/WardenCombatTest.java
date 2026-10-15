package org.example.maniacrevolution.warden;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.*;
import org.example.maniacrevolution.network.packets.*;
import org.example.maniacrevolution.warden.client.WardenReticleLayout;
import java.nio.file.*;

public final class WardenCombatTest {
    public static void run() {
        check(WardenCombatRules.shot(3) == null, "short click never shoots");
        var min = WardenCombatRules.shot(4); var middle = WardenCombatRules.shot(10); var max = WardenCombatRules.shot(20);
        check(min.damage() == 3 && min.range() == 8 && middle.damage() == 6 && middle.range() == 20 && max.damage() == 12 && max.range() == 40, "three charge anchors");
        check(WardenCombatRules.shot(60).equals(max) && max.strength() > min.strength(), "charge capped, sound increases");
        float previous = 0;
        for (int tick = 4; tick <= 20; tick++) { var s = WardenCombatRules.shot(tick); check(s.damage() > previous && s.range() == tick * 2, "smooth damage and range"); previous = s.damage(); }
        var a = new WardenCombatCycle(); var b = new WardenCombatCycle();
        check(a.melee(100) && !a.melee(199) && b.melee(101) && a.melee(200), "melee exact five seconds, independent users");
        check(a.start(201, 1) && !a.melee(300) && !a.start(202, 2) && a.release(211, 2) == null && a.charging(), "charging blocks melee/restarts, wrong release ignored");
        check(a.release(211, 1).equals(middle) && a.waveCooldown(211) == 600 && a.waveCooldown(810) == 1, "wave thirty seconds from release");
        check(!a.start(810, 2) && a.start(811, 2), "wave exact cooldown boundary");
        a.cancel(1); check(a.charging(), "stale cancel ignored"); a.cancel(2);
        check(!a.charging() && a.waveCooldown(812) == 0 && !a.start(812, 2), "cancel no cooldown, duplicate token rejected");
        check(a.start(813, 3) && a.release(816, 3) == null && !a.charging() && a.waveCooldown(816) == 0, "short hold releases without cooldown");
        check(a.start(900, 4), "start lease"); a.expire(960); check(a.charging(), "lease boundary"); a.expire(961); check(!a.charging() && a.waveCooldown(961) == 0, "lost release expires without firing");
        check(a.start(1000, 5), "start long hold");
        for (int tick = 1020; tick <= 1200; tick += 20) { a.keepAlive(tick, 5); a.expire(tick); check(a.charging() && a.charge(tick) == 20, "held input renews lease, charge stays capped"); }
        check(a.release(1201, 5).equals(max), "holding at maximum still releases a full wave");
        var stale = new WardenCombatCycle(); stale.start(10, 1); stale.keepAlive(30, 2); stale.expire(71); check(!stale.charging(), "wrong heartbeat cannot extend gesture");
        var victim = new AABB(1.2, -0.3, -0.3, 1.8, 0.3, 0.3);
        var contact = WardenCombatGeometry.contact(victim, Vec3.ZERO, new Vec3(2, 0, 0), 0.35);
        check(contact.isPresent() && contact.get().x == 1.2, "swept contact lies on actual victim");
        check(WardenCombatGeometry.contact(victim, Vec3.ZERO, new Vec3(0.5, 0, 0), 0.35).isEmpty(), "victim beyond segment not hit");
        check(!WardenCombatGeometry.unobstructed(contact.get(), new Vec3(1, 0, 0)), "wall rejects victim reached only by inflated radius");
        check(WardenCombatGeometry.unobstructed(contact.get(), contact.get()), "unobstructed contact accepted");
        check(WardenCombatGeometry.contact(victim.move(0, 2, 0), Vec3.ZERO, new Vec3(2, 0, 0), 0.35).isEmpty(), "dodged trajectory misses");
        var edgeTarget = new AABB(1.5, 0.05, -0.1, 1.8, 0.3, 0.1);
        check(edgeTarget.clip(Vec3.ZERO, new Vec3(2, 0, 0)).isEmpty(), "exact ray misses body edge");
        var edgeHit = WardenCombatGeometry.contact(edgeTarget, Vec3.ZERO, new Vec3(2, 0, 0), 0.1);
        check(edgeHit.isPresent() && edgeTarget.contains(edgeHit.get().add(0.001, 0.001, 0)), "vanilla pick margin clamps contact to real body");
        check(WardenCombatGeometry.contact(edgeTarget.move(1, 0, 0), Vec3.ZERO, new Vec3(2, 0, 0), 0.1).isEmpty(), "pick margin does not add a third block of reach");
        var meleeEnd = new Vec3(WardenCombatRules.MELEE_RANGE, 0, 0);
        var meleeTarget = new AABB(2.5, 0.12, -0.1, 2.8, 0.4, 0.1);
        check(WardenCombatGeometry.contact(meleeTarget, Vec3.ZERO, new Vec3(2, 0, 0), 0).isEmpty(), "old short exact ray misses target");
        var meleeHit = WardenCombatGeometry.meleeContact(meleeTarget, Vec3.ZERO, meleeEnd, 0);
        check(meleeHit.isPresent(), "three-block melee accepts a small aim error at the actual body");
        check(WardenCombatGeometry.meleeContact(meleeTarget.move(0, 0.2, 0), Vec3.ZERO, meleeEnd, 0).isEmpty(), "large aim error still misses");
        check(WardenCombatGeometry.meleeContact(new AABB(3.01, -0.1, -0.1, 3.4, 0.1, 0.1), Vec3.ZERO, meleeEnd, 0).isEmpty(), "aim margin does not extend three-block reach");
        check(!WardenCombatGeometry.unobstructed(meleeHit.get(), new Vec3(2, 0, 0)), "melee aim margin does not bypass a wall");
        check(!WardenInteractionPolicy.hasEntityInteraction(net.minecraft.server.level.ServerPlayer.class)
                && !WardenInteractionPolicy.hasEntityInteraction(net.minecraft.client.player.RemotePlayer.class), "aiming at a survivor permits charging on both sides");
        check(WardenInteractionPolicy.hasEntityInteraction(net.minecraft.world.entity.npc.Villager.class)
                && WardenInteractionPolicy.hasEntityInteraction(net.minecraft.world.entity.decoration.ArmorStand.class), "entity use retains priority");
        var dim = new ResourceLocation("minecraft", "overworld");
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var request = new WardenCombatInputPacket(WardenCombatInputPacket.Action.RELEASE, 4);
            request.encode(buf); check(buf.readableBytes() == 2 && request.equals(WardenCombatInputPacket.decode(buf)), "request has action/token only, no time/damage/coordinates");
            buf.clear(); var state = new WardenCombatStatePacket(dim, 100, 4, true, 20, 100, 600);
            state.encode(buf); check(state.valid() && state.equals(WardenCombatStatePacket.decode(buf)), "combat state round trip");
            buf.clear(); var wave = new WardenWavePacket(dim, Vec3.ZERO, new Vec3(1, 0, 0), 100, 1);
            wave.encode(buf); check(wave.valid() && wave.equals(WardenWavePacket.decode(buf)), "ring packet round trip");
            buf.clear(); var feedback = new WardenMeleeFeedbackPacket(dim, 100, true);
            feedback.encode(buf); check(feedback.valid() && feedback.equals(WardenMeleeFeedbackPacket.decode(buf)), "server attack acknowledgement round trip");
        } finally { buf.release(); }
        check(!new WardenCombatStatePacket(dim, 1, 0, false, 1, 0, 0).valid() && !new WardenCombatStatePacket(dim, 1, 0, true, 21, 0, 0).valid(), "invalid charging state rejected");
        check(!new WardenWavePacket(dim, Vec3.ZERO, new Vec3(2, 0, 0), 100, 1).valid() && !new WardenWavePacket(dim, Vec3.ZERO, new Vec3(1, 0, 0), 100, Float.NaN).valid(), "invalid direction/strength rejected");
        var idle = WardenReticleLayout.build(0, 0); var half = WardenReticleLayout.build(0.5F, 0); var full = WardenReticleLayout.build(1, 0);
        check(half.size() > idle.size() && full.size() > half.size(), "charge progressively fills spectrum");
        check(idle.equals(WardenReticleLayout.build(-1, 0)) && full.equals(WardenReticleLayout.build(2, 0)), "HUD bounded charge");
        for (var r : full) { check(r.width() > 0 && r.height() > 0 && Math.abs(r.x()) <= 48 && Math.abs(r.y()) <= 13, "compact valid HUD geometry");
            if (r.width() == 2 && Math.abs(r.x()) >= 15) check(full.contains(new WardenReticleLayout.Rect(-r.x() - 1, r.y(), r.width(), r.height(), r.color())), "symmetrical spectrum"); }
        var lone = WardenReticleLayout.cooldowns(21, 0, s -> s.length() * 6);
        check(lone.size() == 1 && lone.get(0).text().equals("Удар 2с") && lone.get(0).x() == -lone.get(0).text().length() * 3, "single action cooldown centered");
        var onlyWave = WardenReticleLayout.cooldowns(0, 600, s -> s.length() * 6);
        check(onlyWave.size() == 1 && onlyWave.get(0).wave() && onlyWave.get(0).text().equals("Волна 30с"), "wave uses action name");
        var pair = WardenReticleLayout.cooldowns(100, 600, s -> s.length() * 6);
        check(pair.size() == 2 && pair.get(1).x() - pair.get(0).x() - pair.get(0).text().length() * 6 == 8
                && pair.get(0).x() == -(pair.get(0).text().length() * 6 + 8 + pair.get(1).text().length() * 6) / 2, "pair centered as one group with readable gap");
        check(WardenReticleLayout.cooldowns(0, 0, String::length).isEmpty(), "no idle labels");
        check(WardenReticleLayout.strike(0, true).isEmpty() && WardenReticleLayout.strike(1, true).size() > WardenReticleLayout.strike(1, false).size(), "feedback expires and distinguishes hit from miss");
        check((WardenReticleLayout.strike(0.5F, true).get(0).color() >>> 24) < (WardenReticleLayout.strike(1, true).get(0).color() >>> 24), "strike feedback fades");
        preview();
        System.out.println("Warden combat: charge anchors, cooldowns, gesture cancellation/lease, swept collision/wall guard, wire validation and reticle geometry passed.");
    }
    private static void preview() {
        var svg = new StringBuilder("<svg xmlns='http://www.w3.org/2000/svg' width='900' height='260' viewBox='0 0 300 86'><rect width='300' height='86' fill='#080E12'/>");
        float[] charges = {0, 0.5F, 1}; String[] labels = {"IDLE", "0.5 s", "1.0 s"};
        for (int i = 0; i < 3; i++) {
            svg.append("<g transform='translate(").append(50 + i * 100).append(" 40)'>");
            for (var r : WardenReticleLayout.build(charges[i], 0)) svg.append("<rect x='").append(r.x()).append("' y='").append(r.y()).append("' width='").append(r.width()).append("' height='").append(r.height()).append("' fill='#").append(String.format("%06x", r.color() & 0xFFFFFF)).append("'/>");
            svg.append("</g><text x='").append(50 + i * 100).append("' y='70' text-anchor='middle' font-family='monospace' font-size='5' fill='#92B6BF'>").append(labels[i]).append("</text>");
        }
        svg.append("</svg>");
        try {
            Files.createDirectories(Path.of("build")); Files.writeString(Path.of("build/warden-reticle-preview.svg"), svg);
            var image = new java.awt.image.BufferedImage(900, 260, java.awt.image.BufferedImage.TYPE_INT_RGB);
            var g = image.createGraphics();
            try {
                g.setColor(new java.awt.Color(0x080E12)); g.fillRect(0, 0, 900, 260);
                for (int i = 0; i < 3; i++) {
                    for (var r : WardenReticleLayout.build(charges[i], 0)) { g.setColor(new java.awt.Color(r.color(), true)); g.fillRect((50 + i * 100 + r.x()) * 3, (40 + r.y()) * 3, r.width() * 3, r.height() * 3); }
                    g.setColor(new java.awt.Color(0x92B6BF)); g.setFont(new java.awt.Font(java.awt.Font.MONOSPACED, java.awt.Font.PLAIN, 16));
                    g.drawString(labels[i], (50 + i * 100) * 3 - g.getFontMetrics().stringWidth(labels[i]) / 2, 210);
                }
            } finally { g.dispose(); }
            javax.imageio.ImageIO.write(image, "png", Path.of("build/warden-reticle-preview.png").toFile());
        }
        catch (java.io.IOException e) { throw new AssertionError("reticle preview", e); }
    }
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
