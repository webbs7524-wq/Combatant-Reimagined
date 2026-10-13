/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

import combatant.client.config.values.NumberValue;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.util.block.interaction.BlockInteractionPipeline;
import combatant.client.util.player.inventory.InventoryActionKind;
import combatant.client.util.player.inventory.InventorySearchScope;
import combatant.client.util.player.inventory.InventorySwap;
import combatant.client.util.player.inventory.InventorySwapPolicy;
import combatant.client.util.player.inventory.InventorySwapRequest;
import combatant.client.util.player.inventory.InventorySwapVisibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Fall clutch: places water under the player just before a long fall lands.
 */
@ModuleInfo(id = "mlg", displayName = "MLG", category = ModuleCategory.PLAYER,
        subcategory = ModuleSubcategory.AUTOMATION, experimental = true,
        description = "module.mlg.description")
public final class MLG extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private final NumberValue<Float> minimumFallDistance = num("minimum_fall_distance", 4.0f, 2.0f, 24.0f);
    private final NumberValue<Integer> scanDepth = num("scan_depth", 4, 1, 8);
    private int delay;

    @Override
    public void onTick() {
        LocalPlayer player = mc.player;
        if (!isEnabled() || player == null || mc.level == null || mc.gameMode == null) return;
        if (delay > 0) {
            delay--;
            return;
        }
        if (player.onGround() || player.isFallFlying() || player.isInWater()) return;
        if (player.getDeltaMovement().y > -0.05) return;
        // Only worth it once the fall is actually dangerous.
        if (player.fallDistance < minimumFallDistance.get()) return;
        if (!hasWaterBucket(player)) return;
        BlockPos support = findLandingSupport(player);
        if (support == null) return;

        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.0, 0.5, 0.0),
                Direction.UP, support, false);
        InventorySwapRequest request = InventorySwapRequest.builder(stack -> stack.is(Items.WATER_BUCKET), () -> {
                    if (mc.player == null || mc.player.onGround()) return;
                    BlockInteractionPipeline.INSTANCE.useOnBlock(this, mc.player, InteractionHand.MAIN_HAND,
                            -1, 0, hit, true);
                })
                .scope(InventorySearchScope.FULL)
                .visibility(InventorySwapVisibility.SILENT)
                .restore(true)
                .policy(InventorySwapPolicy.NONE)
                .actionKind(InventoryActionKind.BLOCK_INTERACT)
                .build();
        InventorySwap.INSTANCE.execute(request);
        delay = 2;
    }

    /**
     * The first solid block below the player, and only while it is close enough that placing the
     * water now breaks the fall. Returns the block the water should be placed on.
     */
    private BlockPos findLandingSupport(LocalPlayer player) {
        BlockPos origin = player.blockPosition();
        int depth = Math.max(1, scanDepth.get());
        for (int offset = 1; offset <= depth; offset++) {
            BlockPos candidate = origin.below(offset);
            if (mc.level.getBlockState(candidate).canBeReplaced()) continue;
            return mc.level.getBlockState(candidate.above()).canBeReplaced() ? candidate : null;
        }
        return null;
    }

    /** The bucket may sit anywhere in the inventory: the swap request brings it to hand. */
    private boolean hasWaterBucket(LocalPlayer player) {
        if (player.getMainHandItem().is(Items.WATER_BUCKET)
                || player.getOffhandItem().is(Items.WATER_BUCKET)) return true;
        for (int slot = 0; slot < 36; slot++) {
            if (player.getInventory().getItem(slot).is(Items.WATER_BUCKET)) return true;
        }
        return false;
    }

    @Override
    public void onDisable() {
        delay = 0;
    }
}
