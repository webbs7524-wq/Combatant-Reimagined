/*
 * This file is part of the Combatant Client distribution.
 * Combatant modifications copyright (c) 2026 pivosos2007.
 *
 * Portions of this file are based on LiquidBounce
 * (https://github.com/CCBlueX/LiquidBounce).
 * Copyright (c) 2015-2026 CCBlueX.
 *
 * LiquidBounce portions are licensed under GPLv3-or-later.
 * Combatant modifications are licensed under GPLv3.
 * See THIRD_PARTY_NOTICES.md for details.
 */

package combatant.client.features.module.modules.combat;

import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import combatant.client.config.common.CommonSettingSchemas;
import combatant.client.config.values.EnumValue;
import combatant.client.config.values.NumberValue;
import combatant.client.events.EventHandler;
import combatant.client.events.impl.GameTickEvent;
import combatant.client.events.impl.PacketEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.util.anticheat.AntiCheatPreset;
import combatant.client.util.combat.MaceSmash;

//Такая хуйня на более менее ач работать не будет
// thx liquidbounce
@ModuleInfo(
        id = "macekill",
        displayName = "MaceKill",
        category = ModuleCategory.COMBAT, subcategory = ModuleSubcategory.ATTACK,
        description = "module.macekill.description")
public final class MaceKill extends Module {
    public static boolean cancelCrit;

    private static final int MAX_BOOST_HEIGHT = 170;

    private final Minecraft mc = Minecraft.getInstance();
    // custom = the fall height below, forged into movement packets; grim (default) = no forged movement
    // packet at all, only a real falling smash, because Grim's Simulation and TimerLimit measure exactly the
    // forged groundless/teleport packets; vanilla = the boost at maximum height for servers without an anti-cheat.
    private final EnumValue<AntiCheatPreset> anticheat =
            enumCommon("anticheat_mode", CommonSettingSchemas.ANTICHEAT_MODE, AntiCheatPreset.GRIM);
    private final NumberValue<Integer> fallHeight =
            visibleWhen(num("height", 22, 1, MAX_BOOST_HEIGHT), this::forgesMovementPackets);

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if (!isEnabled() || event.isCancelled()) return;
        if (!(event.getPacket() instanceof ServerboundAttackPacket packet)) return;
        if (!forgesMovementPackets() || cancelCrit) return;
        if (!isHoldingMace()) return;

        Entity ent = getEntity(packet);
        if (ent == null) return;

        doCrit();
    }

    /**
     * Grim path: the smash is our own attack, sent from the pre-tick hook (in front of the tick's movement
     * packet, like a vanilla click) and with no forged movement packets, so no Post/TimerLimit/Simulation.
     */
    @EventHandler
    private void onGameTick(GameTickEvent event) {
        if (!isEnabled() || forgesMovementPackets()) return;

        MaceSmash.INSTANCE.strike(mc);
    }

    /** Only custom and vanilla write movement packets onto the wire. */
    private boolean forgesMovementPackets() {
        return MaceSmash.forgesMovementPackets(anticheat.get());
    }

    private void doCrit() {
        if (mc == null || mc.player == null || mc.level == null) return;
        int height = determineHeight();
        if (height <= 0) return;

        // Paper/Spigot exploit: send multiple groundless packets for large heights
        if (height > 10) {
            int repeats = (int) Math.ceil(Math.abs(height / 10.0));
            for (int i = 0; i < repeats; i++) {
                sendOnGroundOnly(false);
            }
        } else {
            // Do it at least twice to neutralize horizontal distance
            boolean onGround = mc.player.onGround();
            sendOnGroundOnly(onGround);
            sendOnGroundOnly(onGround);
        }

        // Teleport to the calculated height
        sendPosition(mc.player.getX(), mc.player.getY() + height, mc.player.getZ(), false);
        // Ensure the player returns to ground state.
        sendPosition(mc.player.getX(), mc.player.getY(), mc.player.getZ(), false);
    }

    private void sendOnGroundOnly(boolean onGround) {
        if (mc == null || mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().send(
                new ServerboundMovePlayerPacket.StatusOnly(onGround, mc.player.horizontalCollision)
        );
    }

    private void sendPosition(double x, double y, double z, boolean onGround) {
        if (mc == null || mc.player == null || mc.getConnection() == null) return;
        mc.getConnection().send(
                new ServerboundMovePlayerPacket.Pos(
                        x, y, z, onGround, mc.player.horizontalCollision
                )
        );
    }

    private int determineHeight() {
        if (mc == null || mc.player == null || mc.level == null) return 0;

        AABB bb = mc.player.getBoundingBox();
        int max = anticheat.get().pick(fallHeight.get(), fallHeight.get(), MAX_BOOST_HEIGHT);
        for (int i = max; i >= 1; i--) {
            AABB shifted = bb.move(0.0, i, 0.0);
            if (isBoxClear(shifted)) {
                return i;
            }
        }
        return 0;
    }

    private boolean isBoxClear(AABB box) {
        if (mc == null || mc.player == null || mc.level == null) return false;
        for (VoxelShape shape : mc.level.getBlockCollisions(mc.player, box)) {
            if (!shape.isEmpty()) return false;
        }
        return true;
    }

    private boolean isHoldingMace() {
        if (mc == null || mc.player == null) return false;
        ItemStack mainHand = mc.player.getMainHandItem();
        return mainHand != null && mainHand.getItem() == Items.MACE;
    }

    private Entity getEntity(ServerboundAttackPacket packet) {
        if (mc == null || mc.level == null) return null;
        int id = packet.entityId();
        return mc.level.getEntity(id);
    }


}
