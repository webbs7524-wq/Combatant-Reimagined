/*
 * This file is part of the Combatant Client distribution.
 * Copyright (c) 2026 pivosos2007.
 *
 * Licensed under the GNU General Public License v3.0.
 */

package combatant.client.features.gui.clickgui.sections;

import combatant.client.features.gui.clickgui.ClickGuiRenderer;
import combatant.client.features.theme.Theme;
import combatant.client.render.engine.renderer.Renderer2D;
import combatant.client.render.engine.text.BuiltinFontCatalog;
import combatant.client.render.engine.text.TextRenderer;
import combatant.client.util.media.LocalMediaPlayerService;
import org.lwjgl.glfw.GLFW;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

@ClickGuiSectionInfo(id = "combatant:mediaplayer", label = "Media Player", order = 300)
public final class MediaPlayerSection implements ClickGuiSection {
    private static final int ROWS_PER_PAGE = 12;
    private final List<Path> tracks = new ArrayList<>();
    private float x, y, w, h, listX, listY, listW, rowH;
    private int scroll;
    private boolean draggingListScrollbar;
    private boolean draggingVolumeSlider;
    private String status = "";
    private long statusUntil;

    @Override
    public void layout(float x, float y, float w, float h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        listX = x + 28f;
        listY = y + 76f;
        listW = Math.max(240f, w * 0.56f);
        rowH = 42f;
    }

    @Override
    public void onSelected() {
        scanTracks();
    }

    @Override
    public void render(float mouseX, float mouseY) {
        Renderer2D r = ClickGuiRenderer.currentRenderer();
        if (r == null) return;
        int text = Theme.theme().textPrimary();
        int muted = Theme.theme().textMuted();
        TextRenderer medium = BuiltinFontCatalog.ONEST_MEDIUM.renderer(ClickGuiRenderer.getInterRegular());
        TextRenderer regular = BuiltinFontCatalog.ONEST_REGULAR.renderer(ClickGuiRenderer.getInterRegular());
        r.roundedRect(x + 12f, y + 8f, w - 24f, h - 16f, 16f, 0xD91A1D24);
        ClickGuiRenderer.drawText(medium, "Media Player", listX, y + 28f, 21f, text, false);
        ClickGuiRenderer.drawText(regular, "MP3 files in your game music folder  •  drop MP3s here to add them", listX, y + 53f, 11f, muted, false);

        float buttonW = 92f;
        float buttonX = x + w - 28f - buttonW;
        float buttonY = y + 24f;
        drawButton(r, medium, buttonX, buttonY, buttonW, 30f, "Refresh", mouseX, mouseY);

        float listH = Math.max(90f, h - 106f);
        r.roundedRect(listX, listY, listW, listH, 10f, 0x9923262E);
        if (tracks.isEmpty()) {
            String folder = LocalMediaPlayerService.musicFolder().toString();
            ClickGuiRenderer.drawText(regular, "No MP3 files found.", listX + 14f, listY + 20f, 12f, muted, false);
            ClickGuiRenderer.drawText(regular, ClickGuiRenderer.fitText(regular, folder, 10f, listW - 28f), listX + 14f, listY + 42f, 10f, muted, false);
        }
        int visible = visibleTrackCount(listH);
        int maxScroll = Math.max(0, tracks.size() - visible);
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int start = scroll;
        for (int i = start; i < Math.min(tracks.size(), start + visible); i++) {
            float ry = listY + 6f + (i - start) * rowH;
            boolean hover = mouseX >= listX + 4f && mouseX <= listX + listW - 4f && mouseY >= ry && mouseY <= ry + rowH - 2f;
            if (hover) r.roundedRect(listX + 4f, ry, listW - 8f, rowH - 2f, 7f, 0x553F4654);
            String fileName = tracks.get(i).getFileName().toString();
            ClickGuiRenderer.drawText(regular, ClickGuiRenderer.fitText(regular, fileName, 12f, listW - 66f), listX + 14f, ry + 12f, 12f, text, false);
            drawButton(r, medium, listX + listW - 52f, ry + 6f, 42f, 26f, "Play", mouseX, mouseY);
        }
        if (maxScroll > 0) {
            drawVerticalScrollbar(r, listX + listW - 7f, listY + 6f, 4f, listH - 12f,
                    tracks.size(), visible, scroll);
            ClickGuiRenderer.drawText(regular, "Scroll to browse  •  " + tracks.size() + " tracks", listX + 9f, listY + listH - 17f, 9f, muted, false);
        }

        float panelX = x + w * 0.62f;
        float panelY = y + 76f;
        float panelW = w * 0.33f;
        float panelH = Math.min(224f, Math.max(180f, h - 106f));
        r.roundedRect(panelX, panelY, panelW, panelH, 10f, 0x9923262E);
        ClickGuiRenderer.drawText(medium, "Now playing", panelX + 14f, panelY + 18f, 13f, text, false);
        LocalMediaPlayerService.Snapshot local = LocalMediaPlayerService.snapshot();
        String title = local.title().isBlank() ? "Nothing playing" : local.title();
        ClickGuiRenderer.drawText(medium, ClickGuiRenderer.fitText(medium, title, 14f, panelW - 28f), panelX + 14f, panelY + 49f, 14f, text, false);
        String playbackError = LocalMediaPlayerService.playbackError();
        String subline = playbackError.isBlank() ? (local.playing() ? "Playing" : "Paused") : playbackError;
        ClickGuiRenderer.drawText(regular, ClickGuiRenderer.fitText(regular, subline, 10f, panelW - 28f), panelX + 14f, panelY + 71f, 10f, muted, false);
        drawButton(r, medium, panelX + 14f, panelY + 98f, 78f, 32f, local.playing() ? "Pause" : "Play", mouseX, mouseY);
        drawButton(r, medium, panelX + 100f, panelY + 98f, 76f, 32f, "Stop", mouseX, mouseY);
        float volumeX = panelX + panelW - 22f;
        float volumeY = panelY + 52f;
        float volumeH = 94f;
        float volume = LocalMediaPlayerService.volume();
        drawVerticalSlider(r, regular, volumeX, volumeY, 8f, volumeH, volume, mouseX, mouseY);
        ClickGuiRenderer.drawText(regular, "VOL", volumeX - 5f, volumeY + volumeH + 7f, 8f, muted, false);
        long duration = Math.max(0L, local.durationSeconds());
        long position = Math.max(0L, Math.min(duration, local.positionSeconds()));
        float progress = duration == 0L ? 0f : position / (float) duration;
        float barX = panelX + 14f;
        float barY = panelY + 157f;
        float barW = Math.max(1f, panelW - 28f);
        r.roundedRect(barX, barY, barW, 5f, 2.5f, 0xFF464B56);
        r.roundedRect(barX, barY, Math.max(0f, barW * progress), 5f, 2.5f, Theme.theme().accent());
        r.roundedRect(barX + barW * progress - 4f, barY - 2f, 9f, 9f, 4.5f, Theme.theme().accent());
        ClickGuiRenderer.drawText(regular, formatTime(position), barX, barY + 13f, 10f, muted, false);
        ClickGuiRenderer.drawText(regular, formatTime(duration), barX + barW - 36f, barY + 13f, 10f, muted, false);
        if (!status.isBlank() && System.currentTimeMillis() < statusUntil) {
            ClickGuiRenderer.drawText(regular, ClickGuiRenderer.fitText(regular, status, 10f, panelW - 28f), panelX + 14f, panelY + panelH - 16f, 10f, muted, false);
        }
    }

    private static int visibleTrackCount(float listH) {
        return Math.max(1, Math.min(ROWS_PER_PAGE, (int) ((listH - 8f) / 42f)));
    }

    private void drawVerticalScrollbar(Renderer2D r, float sx, float sy, float sw, float sh,
                                       int itemCount, int visibleCount, int offset) {
        r.roundedRect(sx, sy, sw, sh, sw * 0.5f, 0x88464B56);
        float thumbH = Math.max(24f, sh * visibleCount / (float) itemCount);
        float travel = Math.max(0f, sh - thumbH);
        float thumbY = sy + travel * offset / (itemCount - visibleCount);
        r.roundedRect(sx - 1f, thumbY, sw + 2f, thumbH, (sw + 2f) * 0.5f, Theme.theme().accent());
    }

    private void drawVerticalSlider(Renderer2D r, TextRenderer font, float sx, float sy, float sw, float sh,
                                    float value, float mx, float my) {
        float normalized = Math.max(0f, Math.min(1f, value));
        r.roundedRect(sx + 2f, sy, Math.max(2f, sw - 4f), sh, sw * 0.5f, 0xFF464B56);
        float filledH = sh * normalized;
        r.roundedRect(sx + 2f, sy + sh - filledH, Math.max(2f, sw - 4f), filledH, sw * 0.5f, Theme.theme().accent());
        float knobY = sy + sh * (1f - normalized);
        boolean hover = contains(mx, my, sx - 6f, sy - 4f, sw + 12f, sh + 8f);
        r.roundedRect(sx - 3f, knobY - 4f, sw + 6f, 8f, 4f, hover ? Theme.theme().textPrimary() : 0xFFE2E5EC);
        ClickGuiRenderer.drawText(font, Math.round(normalized * 100f) + "%", sx - 13f, sy - 19f, 9f, Theme.theme().textPrimary(), false);
    }

    private void drawButton(Renderer2D r, TextRenderer font, float bx, float by, float bw, float bh, String label, float mx, float my) {
        boolean hover = mx >= bx && mx <= bx + bw && my >= by && my <= by + bh;
        r.roundedRect(bx, by, bw, bh, bh * 0.35f, hover ? 0xFF444B59 : 0xFF343A45);
        ClickGuiRenderer.drawText(font, label, bx + 9f, by + (bh - 13f) * 0.5f, 11f, Theme.theme().textPrimary(), false);
    }

    @Override
    public void mouseReleased(float mx, float my, int button) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            draggingListScrollbar = false;
            draggingVolumeSlider = false;
        }
    }

    @Override
    public boolean mousePressed(float mx, float my, int button) {
        if (button != GLFW.GLFW_MOUSE_BUTTON_LEFT) return false;
        float listH = Math.max(90f, h - 106f);
        int visible = visibleTrackCount(listH);
        int maxScroll = Math.max(0, tracks.size() - visible);
        float scrollbarX = listX + listW - 8f;
        float scrollbarY = listY + 6f;
        float scrollbarH = listH - 12f;
        if (maxScroll > 0 && contains(mx, my, scrollbarX - 5f, scrollbarY, 14f, scrollbarH)) {
            draggingListScrollbar = true;
            updateListScroll(my, scrollbarY, scrollbarH, visible, maxScroll);
            return true;
        }
        float panelX = x + w * 0.62f;
        float panelY = y + 76f;
        float panelW = w * 0.33f;
        float volumeX = panelX + panelW - 22f;
        float volumeY = panelY + 52f;
        float volumeH = 94f;
        if (contains(mx, my, volumeX - 8f, volumeY - 6f, 24f, volumeH + 12f)) {
            draggingVolumeSlider = true;
            updateVolume(my, volumeY, volumeH);
            return true;
        }
        if (contains(mx, my, panelX + 14f, panelY + 98f, 78f, 32f)) {
            LocalMediaPlayerService.playPause();
            return true;
        }
        if (contains(mx, my, panelX + 100f, panelY + 98f, 76f, 32f)) {
            LocalMediaPlayerService.stop();
            return true;
        }
        float barX = panelX + 14f;
        float barY = panelY + 157f;
        float barW = Math.max(1f, panelW - 28f);
        if (barW > 0f && contains(mx, my, barX, barY - 6f, barW, 17f)) {
            LocalMediaPlayerService.seek(Math.max(0f, Math.min(1f, (mx - barX) / barW)));
            return true;
        }
        if (contains(mx, my, x + w - 120f, y + 24f, 92f, 30f)) {
            scanTracks();
            return true;
        }
        if (mx < listX || mx > listX + listW || my < listY || my > listY + listH) return false;
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int start = scroll;
        for (int i = start; i < Math.min(tracks.size(), start + visible); i++) {
            float ry = listY + 6f + (i - start) * rowH;
            if (contains(mx, my, listX + listW - 56f, ry + 4f, 50f, 30f)) {
                playTrack(tracks.get(i));
                return true;
            }
        }
        return true;
    }

    @Override
    public void mouseMoved(float mx, float my) {
        if (draggingListScrollbar) {
            float listH = Math.max(90f, h - 106f);
            int visible = visibleTrackCount(listH);
            int maxScroll = Math.max(0, tracks.size() - visible);
            if (maxScroll > 0) updateListScroll(my, listY + 6f, listH - 12f, visible, maxScroll);
        }
        if (draggingVolumeSlider) updateVolume(my, y + 76f + 52f, 94f);
    }

    @Override
    public boolean mouseScrolled(float mx, float my, double amount) {
        if (!contains(mx, my, listX, listY, listW, Math.max(90f, h - 106f))) return false;
        int maxScroll = Math.max(0, tracks.size() - visibleTrackCount(Math.max(90f, h - 106f)));
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount) * 3));
        return true;
    }

    private void updateListScroll(float mouseY, float scrollbarY, float scrollbarH, int visible, int maxScroll) {
        float thumbH = Math.max(24f, scrollbarH * visible / (float) tracks.size());
        float travel = Math.max(1f, scrollbarH - thumbH);
        float thumbCenterY = Math.max(scrollbarY + thumbH * 0.5f,
                Math.min(scrollbarY + scrollbarH - thumbH * 0.5f, mouseY));
        scroll = Math.round((thumbCenterY - scrollbarY - thumbH * 0.5f) / travel * maxScroll);
    }

    private void updateVolume(float mouseY, float sliderY, float sliderH) {
        float normalized = 1f - Math.max(0f, Math.min(1f, (mouseY - sliderY) / sliderH));
        LocalMediaPlayerService.setVolume(normalized);
    }

    private void playTrack(Path track) {
        try {
            LocalMediaPlayerService.play(track);
            status = "Loading " + track.getFileName();
            statusUntil = System.currentTimeMillis() + 4000L;
        } catch (RuntimeException ex) {
            status = ex.getMessage() == null ? "Could not play this file" : ex.getMessage();
            statusUntil = System.currentTimeMillis() + 6000L;
        }
    }

    /** Copies dropped MP3 files into the game's music folder; other files are skipped. */
    @Override
    public boolean onFilesDrop(List<Path> paths) {
        if (paths == null || paths.isEmpty()) return false;
        Path folder = LocalMediaPlayerService.musicFolder();
        int copied = 0;
        int skipped = 0;
        String error = null;
        for (Path source : paths) {
            if (source == null || !Files.isRegularFile(source) || !isSupported(source)) {
                skipped++;
                continue;
            }
            try {
                Files.copy(source, uniqueTarget(folder, source.getFileName().toString()));
                copied++;
            } catch (IOException ex) {
                error = ex.getMessage();
            }
        }
        scanTracks();
        if (copied > 0) {
            status = "Added " + copied + " track" + (copied == 1 ? "" : "s") + " to the music folder";
            if (skipped > 0) status += " (" + skipped + " skipped)";
        } else if (error != null) {
            status = "Could not copy file: " + error;
        } else {
            status = "Only MP3 files can be added";
        }
        statusUntil = System.currentTimeMillis() + 6000L;
        return true;
    }

    /** Picks a free name so an existing track with the same file name is never overwritten. */
    private static Path uniqueTarget(Path folder, String fileName) {
        Path target = folder.resolve(fileName);
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        for (int i = 2; Files.exists(target); i++) {
            target = folder.resolve(base + " (" + i + ")" + ext);
        }
        return target;
    }

    private void scanTracks() {
        Path folder = LocalMediaPlayerService.musicFolder();
        tracks.clear();
        try {
            Files.createDirectories(folder);
            try (Stream<Path> files = Files.list(folder)) {
                files.filter(Files::isRegularFile)
                        .filter(MediaPlayerSection::isSupported)
                        .sorted(Comparator.comparing(p -> p.getFileName().toString().toLowerCase(Locale.ROOT)))
                        .forEach(tracks::add);
            }
            status = tracks.size() + " supported tracks found";
        } catch (IOException ex) {
            status = "Unable to read music folder: " + ex.getMessage();
        }
        statusUntil = System.currentTimeMillis() + 4000L;
        scroll = 0;
    }

    private static boolean isSupported(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3");
    }

    private static boolean contains(float px, float py, float bx, float by, float bw, float bh) {
        return px >= bx && px <= bx + bw && py >= by && py <= by + bh;
    }

    private static String formatTime(long seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
