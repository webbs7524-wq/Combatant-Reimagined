/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.render.engine.rhi.resource;

import com.mojang.blaze3d.GpuFormat;
import combatant.client.render.engine.msaa.MsaaFramebuffer;
import combatant.client.util.logging.DebugLog;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Queue;

/**
 * Physical pool for multisampled transient render targets.
 *
 * <p>{@link FramebufferPool} owns single-sample {@code TextureTarget}s. Mojang's TextureTarget
 * cannot honestly represent Combatant's backend-created multisample color/S8 topology, so MSAA
 * stays a separate physical pool while sharing the same {@link TransientTargetDescriptor}
 * lifetime/compatibility contract.</p>
 */
public final class MsaaFramebufferPool implements AutoCloseable {
    private static final int MAX_IDLE_TARGETS = Math.max(
            2, Integer.getInteger("combatant.rhi.transientMsaaTargets", 16)
    );
    private static final long MAX_IDLE_BYTES = Math.max(
            16L * 1024L * 1024L,
            Long.getLong("combatant.rhi.transientMsaaBytes", 256L * 1024L * 1024L)
    );

    private final ResourceLeakTracker leakTracker;
    private final Map<String, FrameTransient> frameLive = new HashMap<>();
    private final Map<String, Queue<MsaaFramebuffer>> available = new HashMap<>();
    private final ArrayDeque<MsaaFramebuffer> lru = new ArrayDeque<>();
    private final Map<MsaaFramebuffer, String> availableKeys = new IdentityHashMap<>();
    private final Map<MsaaFramebuffer, Long> bytes = new IdentityHashMap<>();
    /** Distinguishes stale allocations parked after a mid-frame descriptor change. */
    private long staleTransientSerial;

    private long acquires;
    private long reuses;
    private long releases;
    private long creates;
    private long evictions;
    private int peakFrameLive;
    private long idleBytes;

    public MsaaFramebufferPool(ResourceLeakTracker leakTracker) {
        this.leakTracker = leakTracker;
    }

    public MsaaFramebuffer acquireFrameTransient(TransientTargetDescriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("descriptor");
        if (descriptor.lifetime() != TransientTargetDescriptor.Lifetime.FRAME) {
            throw new IllegalArgumentException("MSAA pool currently requires FRAME lifetime: "
                    + descriptor.logicalName());
        }
        if (descriptor.samples() <= 1) {
            throw new IllegalArgumentException("MSAA pool requires samples > 1: "
                    + descriptor.logicalName() + " samples=" + descriptor.samples());
        }
        // MsaaFramebuffer supports RGBA8 multisample color storage only.
        if (descriptor.colorFormat() != GpuFormat.RGBA8_UNORM) {
            throw new IllegalArgumentException("Unsupported transient MSAA color format: "
                    + descriptor.colorFormat());
        }

        String logicalKey = descriptor.logicalKey();
        FrameTransient existing = frameLive.get(logicalKey);
        if (existing != null) {
            if (existing.descriptor.compatibilityKey().equals(descriptor.compatibilityKey())) {
                return existing.target;
            }
            // The window/surface can change size between two uses of one logical target inside a single
            // frame (startup window sizing, a manual resize, or a fullscreen toggle). That is an external
            // event rather than a renderer logic error, and failing the frame over it aborts rendering.
            // Park the stale allocation under a unique key so the normal frame-tail release still returns
            // it to the pool - it stays alive and untouched for anything still sampling it this frame -
            // then allocate a correctly sized target for the logical slot below.
            frameLive.put(logicalKey + "#stale#" + (++staleTransientSerial), existing);
            DebugLog.warnOnce("msaa-frame-transient-resize:" + logicalKey,
                    "MSAA frame transient %s changed descriptor inside one frame (old=%s new=%s); "
                            + "reallocating it for the rest of the frame",
                    logicalKey, existing.descriptor.compatibilityKey(), descriptor.compatibilityKey());
        }

        String compatibilityKey = descriptor.compatibilityKey();
        Queue<MsaaFramebuffer> compatible = available.computeIfAbsent(
                compatibilityKey, ignored -> new ArrayDeque<>()
        );
        MsaaFramebuffer target = compatible.poll();
        if (target == null) {
            target = new MsaaFramebuffer(
                    descriptor.logicalName(),
                    descriptor.width(),
                    descriptor.height(),
                    descriptor.depth(),
                    descriptor.samples()
            );
            leakTracker.register(target);
            creates++;
        } else {
            lru.remove(target);
            availableKeys.remove(target);
            idleBytes = Math.max(0L, idleBytes - bytes.getOrDefault(target, 0L));
            reuses++;
        }

        bytes.putIfAbsent(target, estimateBytes(descriptor));
        frameLive.put(logicalKey, new FrameTransient(descriptor, target));
        acquires++;
        peakFrameLive = Math.max(peakFrameLive, frameLive.size());
        return target;
    }

    public void beginFrame() {
        // Recover if an exceptional presentation path skipped the previous frame tail.
        releaseFrameTransients();
        acquires = 0L;
        reuses = 0L;
        releases = 0L;
        peakFrameLive = frameLive.size();
    }

    public void releaseFrameTransients() {
        if (frameLive.isEmpty()) return;
        for (FrameTransient allocation : frameLive.values()) {
            String key = allocation.descriptor.compatibilityKey();
            available.computeIfAbsent(key, ignored -> new ArrayDeque<>()).add(allocation.target);
            lru.addLast(allocation.target);
            availableKeys.put(allocation.target, key);
            idleBytes += bytes.getOrDefault(allocation.target, estimateBytes(allocation.descriptor));
            releases++;
        }
        frameLive.clear();
        trimIdle();
    }

    private void trimIdle() {
        while (lru.size() > MAX_IDLE_TARGETS || idleBytes > MAX_IDLE_BYTES) {
            MsaaFramebuffer target = lru.pollFirst();
            if (target == null) break;
            String key = availableKeys.remove(target);
            Queue<MsaaFramebuffer> queue = key != null ? available.get(key) : null;
            if (queue != null) {
                queue.remove(target);
                if (queue.isEmpty()) available.remove(key);
            }
            long allocationBytes = bytes.getOrDefault(target, 0L);
            idleBytes = Math.max(0L, idleBytes - allocationBytes);
            bytes.remove(target);
            leakTracker.release(target);
            target.destroyBuffers();
            evictions++;
        }
    }

    public void invalidate() {
        closeTargets();
    }

    private void closeTargets() {
        for (FrameTransient allocation : frameLive.values()) {
            leakTracker.release(allocation.target);
            allocation.target.destroyBuffers();
        }
        frameLive.clear();
        for (Queue<MsaaFramebuffer> queue : available.values()) {
            while (!queue.isEmpty()) {
                MsaaFramebuffer target = queue.poll();
                leakTracker.release(target);
                target.destroyBuffers();
            }
        }
        available.clear();
        lru.clear();
        availableKeys.clear();
        bytes.clear();
        idleBytes = 0L;
    }

    private static long estimateBytes(TransientTargetDescriptor descriptor) {
        long pixels = (long) descriptor.width() * descriptor.height() * descriptor.samples();
        long color = pixels * Math.max(1, descriptor.colorFormat().blockSize());
        long depth = descriptor.depth() ? pixels * 4L : 0L;
        // Stencil attachments used by complex UI clipping are owned by the backend stencil layer,
        // not by this color/depth target pool, so do not double-count them here.
        return color + depth;
    }

    public int activeFrameTransientCount() {
        return frameLive.size();
    }

    public int pooledCount() {
        int total = frameLive.size();
        for (Queue<MsaaFramebuffer> queue : available.values()) total += queue.size();
        return total;
    }

    public long acquires() {
        return acquires;
    }

    public long reuses() {
        return reuses;
    }

    public long releases() {
        return releases;
    }

    public long creates() {
        return creates;
    }

    public long evictions() {
        return evictions;
    }

    public int peakFrameLive() {
        return peakFrameLive;
    }

    public long idleBytes() {
        return idleBytes;
    }

    @Override
    public void close() {
        closeTargets();
    }

    private record FrameTransient(TransientTargetDescriptor descriptor, MsaaFramebuffer target) {
    }
}
