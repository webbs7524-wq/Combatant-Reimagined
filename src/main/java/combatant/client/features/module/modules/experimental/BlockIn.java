/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.experimental;

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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@ModuleInfo(id = "blockin", displayName = "BlockIn", category = ModuleCategory.COMBAT,
        subcategory = ModuleSubcategory.PROTECT, experimental = true,
        description = "module.blockin.description")
public final class BlockIn extends Module {
    private final Minecraft mc = Minecraft.getInstance();
    private int delay;


    @Override
    public void onTick() {
        // Automatic: it seals the spot under you while you are standing still.
        if (!isEnabled() || mc.player == null || mc.gameMode == null || mc.level == null
                || !mc.player.onGround()
                || mc.player.getDeltaMovement().horizontalDistanceSqr() > 0.0025) return;
        if (delay > 0) { delay--; return; }
        BlockPos below = mc.player.blockPosition().below();
        if (!mc.level.getBlockState(below).isAir()) return;
        BlockPos support = below.below();
        if (mc.level.getBlockState(support).isAir()) return;
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(support).add(0.0, 0.5, 0.0),
                Direction.UP, support, false);
        boolean[] placed = {false};
        InventorySwapRequest request = InventorySwapRequest.builder(stack -> stack.getItem() instanceof BlockItem,
                        () -> placed[0] = BlockInteractionPipeline.INSTANCE.useOnBlock(
                                this, mc.player, net.minecraft.world.InteractionHand.MAIN_HAND,
                                -1, 0, hit, false).consumesAction())
                .scope(InventorySearchScope.FULL)
                .visibility(InventorySwapVisibility.SILENT)
                .restore(true)
                .policy(InventorySwapPolicy.NONE)
                .actionKind(InventoryActionKind.BLOCK_INTERACT)
                .build();
        if (InventorySwap.INSTANCE.execute(request) && placed[0]) delay = 2;
    }

    @Override
    public void onDisable() { delay = 0; }
}
