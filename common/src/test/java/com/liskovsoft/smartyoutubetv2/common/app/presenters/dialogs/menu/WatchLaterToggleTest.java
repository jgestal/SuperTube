package com.liskovsoft.smartyoutubetv2.common.app.presenters.dialogs.menu;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The one tap Watch later button removes the videos of the Watch later list and adds the rest.
 */
public class WatchLaterToggleTest {
    @Test
    public void videosOfTheWatchLaterListAreRemoved() {
        assertTrue(VideoMenuPresenter.isWatchLaterItem(video("WL")));
    }

    @Test
    public void otherVideosAreAdded() {
        assertFalse(VideoMenuPresenter.isWatchLaterItem(video(null))); // home, search, subscriptions...
        assertFalse(VideoMenuPresenter.isWatchLaterItem(video("PLxxxx"))); // user playlist
        assertFalse(VideoMenuPresenter.isWatchLaterItem(video("LL"))); // liked videos
        assertFalse(VideoMenuPresenter.isWatchLaterItem(null));
    }

    private static Video video(String playlistId) {
        Video video = new Video();
        video.videoId = "id";
        video.playlistId = playlistId;
        return video;
    }
}
