package org.example.maniacrevolution.scp173;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;

/** Reject native damage knockback, never roll back positions/velocities or blanket-block perk pushes. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID)
public final class Scp173ImpulsePolicy {
    private static final String ORIGIN = "maniacrev_ability_impulse";
    private static final UUID GUARD = UUID.fromString("b49b6ab1-9e63-4638-bd7e-76f74f4f7b81");
    private static final Set<ServerPlayer> GUARDED = new HashSet<>();
    private static final Set<ServerPlayer> ABILITY_DAMAGE = new HashSet<>();
    private static final StackWalker WALKER = StackWalker.getInstance();
    private Scp173ImpulsePolicy() {}

    /** Automatic provenance for project gameplay code, plus an explicit marker for integrations. */
    public static void markAbility(Entity entity) { entity.getPersistentData().putBoolean(ORIGIN, true); }
    private static boolean gameplay(String name) {
        String root = "org.example.maniacrevolution.";
        return name.startsWith(root) && !name.startsWith(root + "scp173.")
                && !name.startsWith(root + "network.")
                && !name.equals(root + "warden.WardenCombatManager"); // Its bare-hand strike is an ordinary hit; $Wave remains an ability.
    }
    private static boolean abilityCall() { return WALKER.walk(frames -> frames.anyMatch(f -> gameplay(f.getClassName()))); }
    private static boolean abilityEntity(Entity e) {
        return e != null && (e.getPersistentData().getBoolean(ORIGIN) || gameplay(e.getClass().getName()));
    }
    private static void release(ServerPlayer p) {
        var resistance = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null) resistance.removeModifier(GUARD);
        GUARDED.remove(p);
    }
    @SubscribeEvent public static void provenance(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide() && abilityCall()) markAbility(event.getEntity());
    }
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void damage(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Scp173Manager.active(p)) return;
        if (abilityCall() || abilityEntity(event.getSource().getDirectEntity())) { ABILITY_DAMAGE.add(p); release(p); return; }
        ABILITY_DAMAGE.remove(p);
        // AbstractArrow applies Punch with push(), not knockback(). Its own resistance check must
        // see 1 during this native damage transaction. Perk knockback releases this guard first.
        var resistance = p.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (resistance != null && resistance.getModifier(GUARD) == null)
            resistance.addTransientModifier(new AttributeModifier(GUARD, "SCP-173 native damage transaction", 1, AttributeModifier.Operation.ADDITION));
        GUARDED.add(p);
    }
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void knockback(LivingKnockBackEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !Scp173Manager.active(p)) return;
        boolean nativeAttack = WALKER.walk(frames -> {
            var classes = frames.map(StackWalker.StackFrame::getClassName).toList();
            return classes.stream().filter(n -> n.equals("net.minecraft.world.entity.LivingEntity")).count() > 1
                    || classes.contains("net.minecraft.world.entity.player.Player") || classes.contains("net.minecraft.world.entity.Mob");
        });
        if (nativeAttack && !abilityCall() && !ABILITY_DAMAGE.contains(p)) event.setCanceled(true);
        else release(p); // Any direct external knockback API, including new perks/items, remains allowed.
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            for (var p : Set.copyOf(GUARDED)) release(p);
            ABILITY_DAMAGE.clear();
        }
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) {
        for (var p : Set.copyOf(GUARDED)) release(p);
        ABILITY_DAMAGE.clear();
    }
}
