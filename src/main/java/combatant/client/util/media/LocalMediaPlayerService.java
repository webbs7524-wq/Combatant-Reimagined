/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.util.media;

import combatant.client.util.logging.DebugLog;
import javafx.application.Platform;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;
import net.minecraft.client.Minecraft;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Local file playback, independent of external operating-system media sessions. */
public final class LocalMediaPlayerService {
    private static final Object LOCK = new Object();
    private static volatile boolean toolkitReady;
    private static volatile String toolkitError = "";
    private static volatile String playbackError = "";
    private static volatile State state = State.empty();
    private static MediaPlayer player;
    private static volatile long generation;
    private static volatile double volume = 1.0;

    private LocalMediaPlayerService() {}

    public static Path musicFolder() {
        Path folder = Minecraft.getInstance().gameDirectory.toPath().resolve("music");
        try {
            Files.createDirectories(folder);
        } catch (java.io.IOException error) {
            DebugLog.warn("[LocalMediaPlayer] Could not create music folder %s: %s", folder, errorText(error));
        }
        return folder;
    }

    public static String playbackError() {
        return playbackError;
    }

    public static float volume() {
        return (float) volume;
    }

    public static void setVolume(float amount) {
        if (!Float.isFinite(amount)) return;
        double nextVolume = Math.max(0.0, Math.min(1.0, amount));
        if (Math.abs(volume - nextVolume) < 0.001) return;
        volume = nextVolume;
        if (toolkitReady) {
            Platform.runLater(() -> {
                MediaPlayer current = player;
                if (current != null) current.setVolume(volume);
            });
        }
    }

    public static boolean isPlaying() {
        return state.playing;
    }

    public static Snapshot snapshot() {
        State current = state;
        long position = current.positionMs;
        if (current.playing) position += Math.max(0L, System.currentTimeMillis() - current.updatedAtMs);
        if (current.durationMs > 0L) position = Math.min(position, current.durationMs);
        return new Snapshot(current.title, current.playing, position / 1000L, current.durationMs / 1000L);
    }

    public static void play(Path file) {
        if (file == null || !Files.isRegularFile(file)) throw new IllegalArgumentException("Choose a valid music file");
        String name = file.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
        if (!name.endsWith(".mp3")) {
            throw new IllegalArgumentException("Only MP3 files are supported");
        }
        startToolkit();
        if (!toolkitReady) throw new IllegalStateException(toolkitError.isBlank()
                ? "JavaFX media runtime could not be started" : toolkitError);

        final long request;
        synchronized (LOCK) {
            request = ++generation;
            playbackError = "";
            disposePlayer();
            state = new State(file.getFileName().toString(), false, 0L, 0L, System.currentTimeMillis());
        }
        Platform.runLater(() -> {
            if (request != generation) return;
            try {
                Media media = new Media(file.toAbsolutePath().normalize().toUri().toString());
                MediaPlayer next = new MediaPlayer(media);
                synchronized (LOCK) {
                    if (request != generation) {
                        next.dispose();
                        return;
                    }
                    player = next;
                }
                media.setOnError(() -> {
                    if (request == generation) fail(request, "Unsupported or unreadable media: " + errorText(media.getError()));
                });
                next.setOnError(() -> {
                    if (request == generation) fail(request, "Media playback failed: " + errorText(next.getError()));
                });
                next.setOnReady(() -> {
                    if (request != generation) return;
                    Map<String, Object> tags = media.getMetadata();
                    Object tagTitle = tags.get("title");
                    String title = tagTitle instanceof String value && !value.isBlank()
                            ? value : file.getFileName().toString();
                    Duration duration = next.getTotalDuration();
                    long durationMs = duration == null || duration.isUnknown() ? 0L : Math.max(0L, (long) duration.toMillis());
                    updateState(title, true, 0L, durationMs);
                    next.currentTimeProperty().addListener((observable, oldTime, newTime) -> {
                        if (request == generation && newTime != null) updatePosition(request, (long) newTime.toMillis());
                    });
                    next.setOnEndOfMedia(() -> {
                        if (request == generation) updateState(title, false, durationMs, durationMs);
                    });
                    next.setVolume(volume);
                    next.play();
                });
            } catch (Throwable error) {
                if (request == generation) fail(request, "Could not open file: " + errorText(error));
            }
        });
    }

    public static void playPause() {
        if (!toolkitReady) return;
        Platform.runLater(() -> {
            MediaPlayer current = player;
            if (current == null) return;
            if (current.getStatus() == MediaPlayer.Status.PLAYING) {
                current.pause();
                updateState(state.title, false, (long) current.getCurrentTime().toMillis(), totalMs(current));
            } else {
                current.play();
                updateState(state.title, true, (long) current.getCurrentTime().toMillis(), totalMs(current));
            }
        });
    }

    public static void stop() {
        if (!toolkitReady) return;
        Platform.runLater(() -> {
            synchronized (LOCK) {
                generation++;
                disposePlayer();
                state = State.empty();
                playbackError = "";
            }
        });
    }

    public static void seek(float fraction) {
        if (!toolkitReady) return;
        float amount = Math.max(0f, Math.min(1f, fraction));
        Platform.runLater(() -> {
            MediaPlayer current = player;
            if (current == null) return;
            Duration duration = current.getTotalDuration();
            if (duration == null || duration.isUnknown()) return;
            Duration target = duration.multiply(amount);
            current.seek(target);
            updatePosition(generation, (long) target.toMillis());
        });
    }

    private static void startToolkit() {
        if (toolkitReady || !toolkitError.isBlank()) return;
        synchronized (LOCK) {
            if (toolkitReady || !toolkitError.isBlank()) return;
            CountDownLatch started = new CountDownLatch(1);
            Thread startup = new Thread(() -> {
                try {
                    Platform.startup(() -> {
                        toolkitReady = true;
                        started.countDown();
                    });
                } catch (IllegalStateException alreadyStarted) {
                    toolkitReady = true;
                    started.countDown();
                } catch (Throwable error) {
                    toolkitError = errorText(error);
                    started.countDown();
                }
            }, "CombatantLocalMediaStartup");
            startup.setDaemon(true);
            startup.start();
            try {
                if (!started.await(10L, TimeUnit.SECONDS)) toolkitError = "Timed out starting JavaFX media runtime";
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                toolkitError = "Interrupted while starting JavaFX media runtime";
            }
        }
    }

    private static long totalMs(MediaPlayer mediaPlayer) {
        Duration duration = mediaPlayer.getTotalDuration();
        return duration == null || duration.isUnknown() ? 0L : Math.max(0L, (long) duration.toMillis());
    }

    private static void disposePlayer() {
        MediaPlayer old = player;
        player = null;
        if (old == null) return;
        try { old.stop(); } catch (Throwable ignored) {}
        try { old.dispose(); } catch (Throwable ignored) {}
    }

    private static void updateState(String title, boolean playing, long positionMs, long durationMs) {
        state = new State(title, playing, Math.max(0L, positionMs), Math.max(0L, durationMs), System.currentTimeMillis());
    }

    private static void updatePosition(long request, long positionMs) {
        if (request != generation) return;
        State current = state;
        updateState(current.title, current.playing, positionMs, current.durationMs);
    }

    private static void fail(long request, String message) {
        if (request != generation) return;
        playbackError = message;
        DebugLog.warn("[LocalMediaPlayer] %s", message);
        State current = state;
        state = new State(current.title, false, current.positionMs, current.durationMs, System.currentTimeMillis());
    }

    private static String errorText(Throwable error) {
        if (error == null) return "unknown JavaFX media error";
        String message = error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    public record Snapshot(String title, boolean playing, long positionSeconds, long durationSeconds) {
        public Snapshot { title = title == null ? "" : title; }
    }

    private record State(String title, boolean playing, long positionMs, long durationMs, long updatedAtMs) {
        private static State empty() { return new State("", false, 0L, 0L, System.currentTimeMillis()); }
    }
}
