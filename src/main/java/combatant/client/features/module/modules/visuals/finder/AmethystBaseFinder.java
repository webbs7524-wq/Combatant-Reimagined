/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.module.modules.visuals.finder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;
import combatant.client.config.values.NumberValue;
import combatant.client.config.values.RGBAColorValue;
import combatant.client.events.EventHandler;
import combatant.client.events.impl.PacketEvent;
import combatant.client.features.module.Module;
import combatant.client.features.module.ModuleCategory;
import combatant.client.features.module.ModuleInfo;
import combatant.client.features.module.ModuleSubcategory;
import combatant.client.features.module.WorldPhase;
import combatant.client.render.engine.renderer.Renderer3D;
import combatant.client.util.finder.ChunkBlocks;
import combatant.client.util.finder.FinderRender;
import combatant.client.util.finder.LoadedChunkScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Highlights large amethyst buds at their exact block positions. */
@ModuleInfo(
        id = "amethystbasefinder",
        displayName = "AmethystBaseFinder",
        aliases = {"AmethystFinder", "AmethystESP"},
        category = ModuleCategory.VISUALS, subcategory = ModuleSubcategory.ESP,
        description = "module.amethystbasefinder.description")
public final class AmethystBaseFinder extends Module {

    private static final int MAX_BOXES = 4096;
    private static final int MAX_POSITIONS_PER_CHUNK = 256;

    private final NumberValue<Integer> rangeChunks = num("range", 6, 1, 12);
    private final NumberValue<Integer> maxDistance = num("max_distance", 128, 16, 512);
    private final RGBAColorValue fillColor = color("fill_color", "#403B82F6");
    private final RGBAColorValue lineColor = color("line_color", "#FF3B82F6");

    private final Minecraft mc = Minecraft.getInstance();
    private final LoadedChunkScanner<List<BlockPos>> scanner = new LoadedChunkScanner<>(
            AmethystBaseFinder::findAmethyst, rangeChunks::get, 6, 3000L);

    @Override
    public void onEnable() {
        scanner.clear();
    }

    @Override
    public void onDisable() {
        scanner.clear();
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        scanner.onPacket(event.getPacket());
    }

    @Override
    public void onTick() {
        if (!isEnabled() || mc.level == null || mc.player == null) return;
        scanner.tick(mc);
    }

    private static List<BlockPos> findAmethyst(ClientLevel level, LevelChunk chunk) {
        List<BlockPos> found = new ArrayList<>();
        ChunkBlocks.forEach(chunk, level.getMinY(), level.getMaxY() - 1,
                state -> state.is(Blocks.LARGE_AMETHYST_BUD),
                (x, y, z, state) -> {
                    found.add(new BlockPos(x, y, z));
                    return found.size() < MAX_POSITIONS_PER_CHUNK;
                });
        return found.isEmpty() ? null : List.copyOf(found);
    }

    private boolean inRange(LocalPlayer player, BlockPos pos) {
        double distance = maxDistance.get();
        return player.distanceToSqr(Vec3.atCenterOf(pos)) <= distance * distance;
    }

    @Override
    public WorldPhase getWorldPhase() {
        return WorldPhase.END_MAIN;
    }

    @Override
    public void onRenderWorldEngine(Renderer3D renderer, Renderer3D depthRenderer, float tickDelta) {
        LocalPlayer player = mc.player;
        if (!isEnabled() || renderer == null || player == null) return;

        FinderRender draw = FinderRender.begin(renderer, 1.5f);
        int fill = fillColor.getArgb();
        int line = lineColor.getArgb();
        int range = rangeChunks.get();
        ChunkPos center = player.chunkPosition();
        Map<Long, List<BlockPos>> results = scanner.results();
        int drawn = 0;
        for (int ring = 0; ring <= range; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                boolean edgeColumn = dx == -ring || dx == ring;
                for (int dz = -ring; dz <= ring; dz += edgeColumn ? 1 : 2 * ring) {
                    List<BlockPos> blocks = results.get(ChunkPos.pack(center.x() + dx, center.z() + dz));
                    if (blocks == null) continue;
                    for (BlockPos pos : blocks) {
                        if (!inRange(player, pos)) continue;
                        draw.blockBox(pos.getX(), pos.getY(), pos.getZ(), fill, line);
                        if (++drawn >= MAX_BOXES) return;
                    }
                }
            }
        }
    }
}
