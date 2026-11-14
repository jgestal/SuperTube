package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

import org.junit.Test;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * A playlist opened from a list (Watch later, a user playlist...) continues with the next video of that list
 * (the "section playlist"). [Video] keeps its list in a WeakReference. On phones the screen showing the list is often
 * refreshed or gone while the playlist plays, the list was collected and, after a couple of videos, the player fell
 * back to YouTube autoplay (music or anything else).
 */
public class SectionPlaylistTest {
    @Test
    public void playingVideoKeepsItsListAlive() {
        Video first = video("first");
        Video second = video("second");
        WeakReference<VideoGroup> list = new WeakReference<>(VideoGroup.from(new ArrayList<>(Arrays.asList(first, second))));
        assertNotNull(first.getGroup());

        TestController controller = new TestController(); // lives as long as the player presenter
        controller.onNewVideo(first);
        forceGc();

        assertNotNull("The playing video lost its list", first.getGroup());
        assertNotNull(controller);
        assertEquals(Arrays.asList(first, second), list.get().getVideos());
    }

    @Test
    public void nextVideoOfTheListKeepsItAlive() {
        Video first = video("first");
        Video second = video("second");
        VideoGroup group = VideoGroup.from(new ArrayList<>(Arrays.asList(first, second, video("third"))));
        TestController controller = new TestController();
        controller.onNewVideo(first);

        Video next = group.getVideos().get(1); // what the player opens next
        group = null;
        controller.onNewVideo(next);
        forceGc();

        assertNotNull(controller);
        assertNotNull(second.getGroup());
        assertEquals("third", second.getGroup().getVideos().get(2).videoId);
    }

    @Test
    public void listIsReleasedWhenAVideoFromElsewhereStarts() {
        Video first = video("first");
        WeakReference<VideoGroup> list = new WeakReference<>(VideoGroup.from(new ArrayList<>(Arrays.asList(first, video("second")))));
        TestController controller = new TestController();
        controller.onNewVideo(first);

        controller.onNewVideo(video("autoplay")); // no list: YouTube suggestion, link, search...
        forceGc();

        assertNull(list.get());
    }

    private static void forceGc() {
        WeakReference<Object> sentinel = new WeakReference<>(new Object());
        for (int i = 0; i < 50 && sentinel.get() != null; i++) {
            System.gc();
            System.runFinalization();
        }
        System.gc();
        System.runFinalization();
    }

    private static Video video(String videoId) {
        Video video = new Video();
        video.videoId = videoId;
        video.playlistId = "WL";
        return video;
    }

    /** Only the playlist bookkeeping is exercised: no player, no network */
    private static class TestController extends SuggestionsController {
    }
}
