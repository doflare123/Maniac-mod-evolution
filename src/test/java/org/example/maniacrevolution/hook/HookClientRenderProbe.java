package org.example.maniacrevolution.hook;

import com.mojang.authlib.GameProfile;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexSorting;
import com.mojang.math.Axis;
import java.nio.file.Path;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.example.maniacrevolution.Maniacrev;
import org.example.maniacrevolution.ModItems;
import org.example.maniacrevolution.entity.HookEntity;
import org.example.maniacrevolution.entity.ModEntities;
import org.example.maniacrevolution.item.HookItem;
import org.joml.Matrix4f;

/** Opt-in real-engine render/owner/cooldown regression check; never loaded in release jars. */
@Mod.EventBusSubscriber(modid = Maniacrev.MODID, value = Dist.CLIENT)
public final class HookClientRenderProbe {
    private static boolean done;

    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void captureHookFrame(TickEvent.RenderTickEvent event) {
        var mc = Minecraft.getInstance();
        if (!Boolean.getBoolean("maniacrev.hook.renderProbe")) return;
        mc.options.pauseOnLostFocus = false;
        if (mc.screen instanceof net.minecraft.client.gui.screens.PauseScreen) mc.setScreen(null);
        if (done || event.phase != TickEvent.Phase.END
                || mc.level == null || mc.player == null || mc.player.tickCount < 30) return;
        done = true;
        boolean[] leftHanded = {false};
        var player = new RemotePlayer(mc.level, new GameProfile(UUID.randomUUID(), "HookProbe")) {
            @Override public net.minecraft.world.entity.HumanoidArm getMainArm() {
                return leftHanded[0] ? net.minecraft.world.entity.HumanoidArm.LEFT : net.minecraft.world.entity.HumanoidArm.RIGHT;
            }
        };
        player.setPos(mc.player.position());
        player.setOldPosAndRot();
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.HOOK.get()));
        mc.level.addPlayer(-601, player);
        var hook = new HookEntity(ModEntities.HOOK.get(), mc.level);
        hook.setPos(player.position().add(0, 1, 8));
        hook.setOldPosAndRot();
        mc.level.putNonPlayerEntity(-602, hook);
        var target = new TextureTarget(1280, 800, true, Minecraft.ON_OSX);
        var projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        var sorting = RenderSystem.getVertexSorting();
        var view = RenderSystem.getModelViewStack();
        view.pushPose();
        try {
            // Simulate only the metadata delivered to a tracking client, without calling setOwner.
            var ownerField = HookEntity.class.getDeclaredField("DATA_OWNER_ID");
            ownerField.setAccessible(true);
            hook.getEntityData().set((EntityDataAccessor<Integer>) ownerField.get(null), player.getId());
            if (hook.getOwner() != player) throw new AssertionError("Client cannot resolve chain owner");
            var item = (HookItem) ModItems.HOOK.get();
            verifyAimInput(mc, item);
            mc.getSingleplayerServer().submit(() -> {
                var caster = mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
                var previousStack = caster.getMainHandItem().copy();
                caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
                caster.getCooldowns().removeCooldown(item);
                caster.getCapability(org.example.maniacrevolution.mana.ManaProvider.MANA)
                        .ifPresent(mana -> mana.setMana(mana.getMaxMana()));
                float before = org.example.maniacrevolution.util.ManaUtil.getMana(caster);
                var result = item.use(caster.level(), caster, InteractionHand.MAIN_HAND);
                var shots = caster.level().getEntitiesOfClass(HookEntity.class,
                        caster.getBoundingBox().inflate(32), shot -> shot.getOwner() == caster);
                if (!result.getResult().consumesAction() || shots.size() != 1 || caster.isUsingItem())
                    throw new AssertionError("Cast did not launch exactly one hook immediately");
                if (org.example.maniacrevolution.util.ManaUtil.getMana(caster) != before - 5)
                    throw new AssertionError("Cast must consume exactly five mana");
                var deployedStack = caster.getMainHandItem();
                var copiedStack = deployedStack.copy();
                if (!org.example.maniacrevolution.entity.HookDeployment.hasActiveHook(caster)
                        || !org.example.maniacrevolution.entity.HookDeployment.isDeployed(deployedStack, caster.level())
                        || deployedStack.getCount() != 1)
                    throw new AssertionError("Flight state must keep the original item and mark it deployed");
                caster.getCooldowns().removeCooldown(item);
                if (item.use(caster.level(), caster, InteractionHand.MAIN_HAND).getResult()
                        != net.minecraft.world.InteractionResult.FAIL)
                    throw new AssertionError("Returning hook must prevent a second cast even after cooldown ends");
                if (org.example.maniacrevolution.util.ManaUtil.getMana(caster) != before - 5)
                    throw new AssertionError("Rejected repeated cast must not consume mana");
                caster.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.IRON_SWORD));
                var attackTarget = new net.minecraft.world.entity.decoration.ArmorStand(caster.level(), 0, 170, 0);
                var blockedAttack = new net.minecraftforge.event.entity.player.AttackEntityEvent(caster, attackTarget);
                if (!net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(blockedAttack))
                    throw new AssertionError("Server must block attacks after switching weapons during flight");
                shots.forEach(HookEntity::discard);
                if (org.example.maniacrevolution.entity.HookDeployment.hasActiveHook(caster)
                        || org.example.maniacrevolution.entity.HookDeployment.isDeployed(deployedStack, caster.level())
                        || (deployedStack.hasTag() && deployedStack.getTag().contains("maniacrevHookFlight")))
                    throw new AssertionError("Hook removal must immediately restore the source stack and attacks");
                if (org.example.maniacrevolution.entity.HookDeployment.isDeployed(copiedStack, caster.level()))
                    throw new AssertionError("Copied/moved stack must not remain deployed after hook removal");
                org.example.maniacrevolution.entity.HookDeployment.clearStale(copiedStack, caster.level());
                if (copiedStack.hasTag() && copiedStack.getTag().contains("maniacrevHookFlight"))
                    throw new AssertionError("Stale flight metadata was not cleared");
                var restoredAttack = new net.minecraftforge.event.entity.player.AttackEntityEvent(caster, attackTarget);
                org.example.maniacrevolution.event.HookDeploymentEvents.preventDeployedHookAttack(restoredAttack);
                if (restoredAttack.isCanceled()) throw new AssertionError("Attacks must resume after return");
                caster.setItemInHand(InteractionHand.MAIN_HAND, previousStack);
                caster.getCooldowns().removeCooldown(item);
                var origin = new net.minecraft.world.phys.Vec3(0.5, 170.5, 0.5);
                var end = origin.add(0, 0, HookEntity.MAX_DISTANCE);
                var wall = new net.minecraft.core.BlockPos(0, 170, 4);
                var floor = new net.minecraft.core.BlockPos(0, 165, 8);
                var previousBlock = caster.level().getBlockState(wall);
                var previousFloor = caster.level().getBlockState(floor);
                var victim = new net.minecraft.world.entity.decoration.ArmorStand(caster.level(), 0.5, 170, 8.5);
                caster.level().addFreshEntity(victim);
                try {
                    caster.level().setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    var blocked = org.example.maniacrevolution.entity.HookTrajectory.trace(caster, caster, origin, end);
                    if (blocked.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK)
                        throw new AssertionError("Aim must stop at the wall before a living target");
                    caster.level().setBlockAndUpdate(wall, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                    var targetHit = org.example.maniacrevolution.entity.HookTrajectory.trace(caster, caster, origin, end);
                    if (!(targetHit instanceof net.minecraft.world.phys.EntityHitResult entityHit) || entityHit.getEntity() != victim)
                        throw new AssertionError("Aim must select the first living target");
                    if (targetHit.getLocation().z >= victim.getZ())
                        throw new AssertionError("Aim must stop at the target surface, not its centre");
                    caster.level().setBlockAndUpdate(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                    var overhead = new net.minecraft.world.phys.Vec3(0.5, 170.5, 8.5);
                    var ground = org.example.maniacrevolution.entity.HookTrajectory.groundProjection(caster, overhead);
                    if (ground.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK
                            || Math.abs(ground.getLocation().y - 166) > 0.001
                            || ground.getLocation().x != overhead.x || ground.getLocation().z != overhead.z)
                        throw new AssertionError("Endpoint projection must fall vertically to the ground surface");
                    victim.setPos(0.5, 170, 30.5);
                    var missed = org.example.maniacrevolution.entity.HookTrajectory.trace(caster, caster, origin, end);
                    if (missed.getType() != net.minecraft.world.phys.HitResult.Type.MISS
                            || Math.abs(missed.getLocation().distanceTo(origin) - HookEntity.MAX_DISTANCE) > 0.001)
                        throw new AssertionError("Aim must respect maximum hook range");
                } finally {
                    victim.discard();
                    caster.level().setBlockAndUpdate(wall, previousBlock);
                    caster.level().setBlockAndUpdate(floor, previousFloor);
                }
                return null;
            }).join();
            int cooldownTicks = item.getMaxCooldownSeconds() * 20;
            if (cooldownTicks != 400) throw new AssertionError("Hook cooldown must be twenty seconds");
            player.getCooldowns().addCooldown(item, cooldownTicks);
            for (int tick = 0; tick <= cooldownTicks; tick++) {
                int expectedSeconds = (cooldownTicks - tick + 19) / 20;
                if (item.getCooldownSeconds(player) != expectedSeconds
                        || item.isOnCooldown(player) != player.getCooldowns().isOnCooldown(item)
                        || item.getCooldownProgress(player) != player.getCooldowns().getCooldownPercent(item, 0))
                    throw new AssertionError("HUD/slot cooldown mismatch at tick " + tick);
                player.getCooldowns().tick();
            }
            var mesh = mc.getItemRenderer().getModel(player.getMainHandItem(), mc.level, player, 0);
            if (mesh.getQuads(null, null, RandomSource.create()).isEmpty())
                throw new AssertionError("Hook icon baked to an empty mesh");
            var originalGrip = mesh.getTransforms().getTransform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND);
            if (!originalGrip.rotation.equals(new org.joml.Vector3f())
                    || !originalGrip.scale.equals(new org.joml.Vector3f(1))
                    || !originalGrip.translation.equals(new org.joml.Vector3f(0, 0.75f / 16, 2.5f / 16)))
                throw new AssertionError("Original hook hand transform changed");
            var flightData = new net.minecraft.nbt.CompoundTag();
            flightData.putUUID("Entity", hook.getUUID());
            flightData.putInt("EntityId", hook.getId());
            player.getMainHandItem().getOrCreateTag().put("maniacrevHookFlight", flightData);
            var deployedModel = mc.getItemRenderer().getModel(player.getMainHandItem(), mc.level, player, 0);
            var guiModel = deployedModel.applyTransform(net.minecraft.world.item.ItemDisplayContext.GUI, new com.mojang.blaze3d.vertex.PoseStack(), false);
            if (!guiModel.getQuads(null, null, RandomSource.create()).isEmpty())
                throw new AssertionError("Deployed hook must have an empty inventory icon");
            var heldModel = deployedModel.applyTransform(net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                    new com.mojang.blaze3d.vertex.PoseStack(), false);
            var heldQuads = heldModel.getQuads(null, null, RandomSource.create());
            if (heldQuads.isEmpty()) throw new AssertionError("Deployed hook must leave a visible grip and chain");
            for (var quad : heldQuads) {
                int[] data = quad.getVertices();
                for (int vertex = 0; vertex < 4; vertex++)
                    if (Float.intBitsToFloat(data[vertex * 8 + 1]) > 0.67f)
                        throw new AssertionError("Blade must not remain on the held grip during flight");
            }
            target.setClearColor(0.10f, 0.13f, 0.17f, 1);
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0, 1280, 800, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);
            view.setIdentity();
            view.translate(0, 0, -10000);
            RenderSystem.applyModelViewMatrix();
            var graphics = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
            hook.tickCount = 40;
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 350, 800, 280, 2.25f, 0, player);
            leftHanded[0] = true;
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 930, 800, 280, -3.5f, 0, player);
            leftHanded[0] = false;
            graphics.flush();
            try (var screenshot = Screenshot.takeScreenshot(target)) {
                screenshot.writeToFile(Path.of("hook-grip-closeup.png"));
            }
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 250, 470, 190, 0.15f, 0, player);
            graphics.drawCenteredString(mc.font, "Hook away: grip and chain", 250, 500, 0xFFFFFF);
            hook.tickCount = 2;
            InventoryScreen.renderEntityInInventoryFollowsAngle(graphics, 650, 470, 190, 0.15f, 0, player);
            graphics.drawCenteredString(mc.font, "Instant cast follow-through", 650, 500, 0xFFFFFF);
            graphics.pose().pushPose();
            graphics.pose().translate(1040, 100, 0);
            graphics.pose().scale(8, 8, 8);
            graphics.fill(0, 0, 16, 16, 0x303A4048);
            graphics.renderOutline(0, 0, 16, 16, 0xFF627080);
            graphics.renderItem(player.getMainHandItem(), 0, 0);
            graphics.pose().popPose();
            graphics.drawCenteredString(mc.font, "Inventory icon hidden while away", 1100, 260, 0xFFFFFF);
            graphics.flush();
            var extension = net.minecraftforge.client.extensions.common.IClientItemExtensions.of(item);
            hook.discard();
            var restoredModel = mc.getItemRenderer().getModel(player.getMainHandItem(), mc.level, player, 0)
                    .applyTransform(net.minecraft.world.item.ItemDisplayContext.GUI, new com.mojang.blaze3d.vertex.PoseStack(), false);
            if (restoredModel.getQuads(null, null, RandomSource.create()).isEmpty())
                throw new AssertionError("Inventory icon must reappear as soon as the projectile is removed");
            if (extension.getArmPose(player, InteractionHand.MAIN_HAND, player.getMainHandItem()) == null)
                throw new AssertionError("Returning hook prematurely cancelled player follow-through");
            var poses = graphics.pose();
            poses.pushPose();
            poses.translate(1120, 670, 200);
            poses.scale(85, -85, 85);
            poses.mulPose(Axis.YP.rotationDegrees(60));
            mc.getEntityRenderDispatcher().getRenderer(hook).render(hook, 0, 0, poses,
                    mc.renderBuffers().bufferSource(), 0xF000F0);
            mc.renderBuffers().bufferSource().endBatch();
            poses.popPose();
            graphics.drawCenteredString(mc.font, "Projectile and linked chain (client owner metadata only)", 800, 760, 0xFFFFFF);
            graphics.flush();
            poses.pushPose();
            poses.translate(510, 565, 200);
            poses.scale(50, -50, 50);
            poses.mulPose(Axis.YP.rotationDegrees(60));
            org.example.maniacrevolution.client.HookAimClient.drawGuide(poses,
                    mc.renderBuffers().bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.lines()),
                    net.minecraft.world.phys.Vec3.ZERO, new net.minecraft.world.phys.Vec3(0, 0, 8),
                    new net.minecraft.world.phys.Vec3(1, 0, 0), new net.minecraft.world.phys.Vec3(0, 1, 0), 105, 195, 200);
            org.example.maniacrevolution.client.HookAimClient.drawGroundProjection(poses,
                    mc.renderBuffers().bufferSource().getBuffer(net.minecraft.client.renderer.RenderType.lines()),
                    new net.minecraft.world.phys.Vec3(0, 0, 8), new net.minecraft.world.phys.Vec3(0, -1.2, 8),
                    true, 105, 195, 200);
            mc.renderBuffers().bufferSource().endBatch(net.minecraft.client.renderer.RenderType.lines());
            poses.popPose();
            graphics.drawCenteredString(mc.font, "Aim preview (hold RMB) + ground projection", 700, 590, 0xFFFFFF);
            graphics.flush();
            try (var screenshot = Screenshot.takeScreenshot(target)) {
                screenshot.writeToFile(Path.of("hook-preview-probe.png"));
                screenshot.writeToFile(Path.of("hook-flight-state-probe.png"));
            }
            Maniacrev.LOGGER.info("HOOK PROBE PASSED: cast, mana, cooldown, target/wall/range prediction, original model and aim guide render");
        } catch (Exception error) {
            throw new IllegalStateException("Hook render probe failed", error);
        } finally {
            view.popPose();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(projection, sorting);
            mc.getMainRenderTarget().bindWrite(true);
            target.destroyBuffers();
            mc.stop();
        }
    }

    private static void verifyAimInput(Minecraft mc, HookItem item) throws Exception {
        var field = org.example.maniacrevolution.client.HookAimClient.class.getDeclaredField("AIM");
        field.setAccessible(true);
        var aim = (org.example.maniacrevolution.client.HookAimState) field.get(null);
        var originalMode = mc.gameMode;
        var originalStack = mc.player.getMainHandItem();
        boolean originalFocus = mc.isWindowActive();
        int[] casts = {0};
        mc.gameMode = new net.minecraft.client.multiplayer.MultiPlayerGameMode(mc, mc.getConnection()) {
            @Override public net.minecraft.world.InteractionResult useItem(net.minecraft.world.entity.player.Player player,
                                                                          InteractionHand hand) {
                casts[0]++;
                return net.minecraft.world.InteractionResult.CONSUME;
            }
        };
        var stack = new ItemStack(item);
        mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        mc.setWindowActive(true);
        aim.reset();
        try {
            mc.options.keyUse.setDown(true);
            var press = net.minecraftforge.client.ForgeHooksClient.onClickInput(1, mc.options.keyUse, InteractionHand.MAIN_HAND);
            if (!press.isCanceled() || press.shouldSwingHand() || !aim.isAiming() || casts[0] != 0)
                throw new AssertionError("RMB must aim without swinging or throwing");
            mc.options.keyUse.setDown(false);
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            if (casts[0] != 1) throw new AssertionError("RMB release must call item use once");

            mc.options.keyUse.setDown(true);
            net.minecraftforge.client.ForgeHooksClient.onClickInput(1, mc.options.keyUse, InteractionHand.MAIN_HAND);
            mc.options.keyAttack.setDown(true);
            for (int i = 0; i < 3; i++) {
                var cancel = net.minecraftforge.client.ForgeHooksClient.onClickInput(0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
                if (!cancel.isCanceled() || cancel.shouldSwingHand()) throw new AssertionError("LMB hold must stay consumed");
            }
            mc.options.keyUse.setDown(false);
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            if (aim.isAiming() || casts[0] != 1) throw new AssertionError("LMB cancellation must not cast on release");
            mc.options.keyAttack.setDown(false);
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            var idleLmb = net.minecraftforge.client.ForgeHooksClient.onClickInput(0, mc.options.keyAttack, InteractionHand.MAIN_HAND);
            if (idleLmb.isCanceled() || casts[0] != 1) throw new AssertionError("LMB outside aiming must remain a normal melee attack");
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));

            mc.options.keyUse.setDown(true);
            net.minecraftforge.client.ForgeHooksClient.onClickInput(1, mc.options.keyUse, InteractionHand.MAIN_HAND);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(net.minecraft.world.item.Items.STICK));
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            net.minecraftforge.client.ForgeHooksClient.onClickInput(1, mc.options.keyUse, InteractionHand.MAIN_HAND);
            mc.options.keyUse.setDown(false);
            org.example.maniacrevolution.client.HookAimClient.hookAimTick(new TickEvent.ClientTickEvent(TickEvent.Phase.END));
            if (aim.isAiming() || casts[0] != 1) throw new AssertionError("Switching away and back must cancel release");
            Maniacrev.LOGGER.info("HOOK INPUT PASSED: Forge RMB hold/release, LMB cancellation/repeats and weapon changes");
        } finally {
            aim.reset();
            mc.options.keyUse.setDown(false);
            mc.options.keyAttack.setDown(false);
            mc.player.setItemInHand(InteractionHand.MAIN_HAND, originalStack);
            mc.gameMode = originalMode;
            mc.setWindowActive(originalFocus);
        }
    }
}
