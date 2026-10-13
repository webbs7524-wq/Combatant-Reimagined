/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.util.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.EntityHitResult;
import combatant.client.util.anticheat.AntiCheatPreset;
import combatant.client.util.target.TargetingUtil;

/**
 * Shared legit mace smash used by AutoMace and MaceKill.
 * <p>
 * The strike belongs to the pre-tick hook: {@code Module.onTick()} runs from {@code END_CLIENT_TICK}, i.e.
 * behind the tick's movement packet, while a vanilla client clicks in front of it. Grim's Post check flags
 * that reversed order on attack packets. Nothing is forged here either - the server grants the smash bonus
 * itself from the real fall distance, so Grim's Simulation and TimerLimit stay quiet.
 */
public final class MaceSmash {

    public static final MaceSmash INSTANCE = new MaceSmash();

    /** Vanilla grants the mace smash bonus from 1.5 blocks of fall distance up. */
    public static final double MIN_FALL_DISTANCE = 1.5;
    private static final double TARGET_RANGE = 4.5;
    private static final float TARGET_FOV = 60.0f;

    private int lastStrikeTick = Integer.MIN_VALUE;

    private MaceSmash() {
    }

    /**
     * Whether a mace module may write forged movement packets onto the wire. Only a real falling smash is
     * sent under {@link AntiCheatPreset#GRIM}: the groundless and teleport packets that {@code CUSTOM} and
     * {@code VANILLA} allow are exactly what Grim's Simulation (prediction offset) and TimerLimit (tick
     * packet rate) measure.
     */
    public static boolean forgesMovementPackets(AntiCheatPreset preset) {
        return preset != AntiCheatPreset.GRIM;
    }

    /** Mace in hand, airborne and really falling far enough for the vanilla smash bonus. */
    public static boolean isSmashReady(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return false;
        }

        LocalPlayer player = mc.player;
        return player.getMainHandItem().is(Items.MACE)
                && !player.onGround()
                && !player.isFallFlying()
                && player.fallDistance >= MIN_FALL_DISTANCE;
    }

    /** The entity under the crosshair when there is one, otherwise the closest valid target. */
    public static LivingEntity findTarget(Minecraft mc) {
        if (mc == null || mc.player == null) {
            return null;
        }

        if (mc.hitResult instanceof EntityHitResult hit && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }

        return TargetingUtil.findBestTarget(mc, new TargetingUtil.TargetingSettings(
                TARGET_RANGE,
                TARGET_FOV,
                true,
                true,
                true,
                false,
                false,
                true,
                true,
                TargetingUtil.TargetPriority.DISTANCE
        ));
    }

    /**
     * One smash attempt. Pre-tick hook only; a second call inside the same player tick is ignored so two
     * modules cannot click twice.
     */
    public boolean strike(Minecraft mc) {
        if (mc == null || mc.player == null || mc.gameMode == null) {
            return false;
        }

        if (mc.player.tickCount == lastStrikeTick) {
            return false;
        }

        if (!isSmashReady(mc)) {
            return false;
        }

        LivingEntity target = findTarget(mc);
        if (!TargetingUtil.isValidCombatTarget(target)) {
            return false;
        }

        boolean attacked = CombatStrikeController.INSTANCE.tryAttack(
                mc,
                target,
                false,
                1,
                CombatStrikeController.SprintResetMode.LEGIT,
                false,
                null
        );

        if (attacked) {
            lastStrikeTick = mc.player.tickCount;
        }
        return attacked;
    }
}
