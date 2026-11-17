package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SimpleMediaItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.listener.PlayerEventListener;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;
import com.liskovsoft.smartyoutubetv2.common.prefs.PlayerData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers.NextVideoTest.video;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The previous/next buttons (player, notification, headset, car) always change the video: they never seek inside it.
 */
public class SkipVideoTest {
    private final List<String> mOpened = new ArrayList<>();
    private final List<Long> mSeeks = new ArrayList<>();

    @Before
    public void setUp() {
        Playlist.instance().clear();
    }

    @After
    public void tearDown() {
        Playlist.instance().clear();
    }

    @Test
    public void nextOpensTheNextVideo() {
        Video current = video("current", null);
        current.isSynced = true;
        current.nextMediaItem = SimpleMediaItem.from(video("related", null));

        controller(current).loadNext();

        assertEquals(Collections.singletonList("related"), mOpened);
        assertTrue("Next must not seek", mSeeks.isEmpty());
    }

    @Test
    public void previousOpensTheVideoPlayedBefore() {
        Video before = video("before", null);
        Video current = video("current", null);
        Playlist.instance().add(before);
        Playlist.instance().add(current);
        Playlist.instance().setCurrent(current);

        controller(current).loadPrevious();

        assertEquals(Collections.singletonList("before"), mOpened);
        assertTrue("Previous must not seek", mSeeks.isEmpty());
    }

    @Test
    public void previousFromTheCarInTheMiddleOfASongOpensTheSongBefore() {
        // The car, headset and lock screen call loadPrevious directly: no "restart first" past the first 10 s
        Video before = video("before", null);
        Video current = video("current", null);
        Playlist.instance().add(before);
        Playlist.instance().add(current);
        Playlist.instance().setCurrent(current);
        TestLoader loader = controller(current);
        when(loader.getPlayer().getPositionMs()).thenReturn(95_000L);

        loader.loadPrevious();

        assertEquals(Collections.singletonList("before"), mOpened);
        assertTrue("Must not seek back", mSeeks.isEmpty());
    }

    @Test
    public void previousWithNothingBeforeRestartsTheVideo() {
        Video current = video("current", null);
        Playlist.instance().add(current);

        controller(current).loadPrevious();

        assertTrue(mOpened.isEmpty());
        assertEquals(Collections.singletonList(0L), mSeeks);
    }

    @Test
    public void nextPressedWhileLoadingIsDoneWhenTheVideosArrive() {
        Video current = video("current", null); // just started: related videos not loaded yet
        TestLoader loader = controller(current);

        loader.loadNext();
        assertTrue("Nothing to open yet", mOpened.isEmpty());

        current.nextMediaItem = SimpleMediaItem.from(video("related", null));
        current.isSynced = true;
        loader.onMetadata(null);

        assertEquals(Collections.singletonList("related"), mOpened);
    }

    @Test
    public void pendingNextIsDroppedWhenAnotherVideoStarts() {
        Video current = video("current", null);
        TestLoader loader = controller(current);
        loader.loadNext();

        Video chosen = video("chosen", null); // e.g. the user tapped another video
        loader.onNewVideo(chosen);
        loader.mVideo = chosen;
        chosen.nextMediaItem = SimpleMediaItem.from(video("related", null));
        chosen.isSynced = true;
        loader.onMetadata(null);

        assertTrue("Skipped the video the user chose: " + mOpened, mOpened.isEmpty());
    }

    @Test
    public void metadataWithoutPendingNextDoesNotSkip() {
        Video current = video("current", null);
        current.nextMediaItem = SimpleMediaItem.from(video("related", null));
        current.isSynced = true;

        controller(current).onMetadata(null);

        assertTrue(mOpened.isEmpty());
    }

    private TestLoader controller(Video current) {
        TestLoader loader = new TestLoader(current);
        loader.onInit();
        return loader;
    }

    private class TestLoader extends VideoLoaderController {
        private final SuggestionsController mSuggestions = new SuggestionsController() {
            @Override
            public PlaybackView getPlayer() {
                return mPlayer;
            }

            @Override
            public Video getVideo() {
                return mVideo;
            }
        };
        private final PlaybackView mPlayer = mock(PlaybackView.class);
        /** Records the videos the loader opens (the presenter would start them) */
        private final PlayerEventListener mMain = mock(PlayerEventListener.class);

        {
            when(mPlayer.getDurationMs()).thenReturn(300_000L); // playing, not at the end
            doAnswer(call -> mSeeks.add(call.getArgument(0))).when(mPlayer).setPositionMs(anyLong());
            doAnswer(call -> mOpened.add(((Video) call.getArgument(0)).videoId)).when(mMain).onNewVideo(any());
        }
        private final PlayerData mPlayerData = mock(PlayerData.class);
        private Video mVideo;

        private TestLoader(Video video) {
            mVideo = video;
        }

        @Override
        public PlaybackView getPlayer() {
            return mPlayer;
        }

        @Override
        public Video getVideo() {
            return mVideo;
        }

        @Override
        protected PlayerEventListener getMainController() {
            return mMain;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected <T extends PlayerEventListener> T getController(Class<T> clazz) {
            return clazz == SuggestionsController.class ? (T) mSuggestions : null;
        }

        @Override
        protected PlayerData getPlayerData() {
            return mPlayerData;
        }

        @Override
        protected boolean isPlayerUiOnNextEnabled() {
            return false;
        }
    }
}
