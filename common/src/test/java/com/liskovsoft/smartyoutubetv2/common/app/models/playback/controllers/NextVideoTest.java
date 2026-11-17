package com.liskovsoft.smartyoutubetv2.common.app.models.playback.controllers;

import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Playlist;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.SimpleMediaItem;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;
import com.liskovsoft.smartyoutubetv2.common.app.views.PlaybackView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Which video comes next (or before) when the user skips or the video ends.
 *
 * A song opened from a feed (Home, Music, Search, Subscriptions...) must continue with related music (YouTube's next
 * video), not with the next card of that feed, which has nothing to do with it. Only lists with an order of their own
 * (playlists, Watch later, a channel's videos) are played one after another.
 */
public class NextVideoTest {
    /** Video holds its list in a WeakReference: the tests keep theirs alive like the screens showing them do */
    private static final List<VideoGroup> sLists = new ArrayList<>();

    @Before
    public void setUp() {
        Playlist.instance().clear();
    }

    @After
    public void tearDown() {
        Playlist.instance().clear();
        sLists.clear();
    }

    private static VideoGroup keep(VideoGroup group) {
        sLists.add(group);
        return group;
    }

    // Next

    @Test
    public void songFromHomeContinuesWithRelatedMusic() throws Exception {
        assertNextIsRelated(MediaGroup.TYPE_HOME);
    }

    @Test
    public void songFromMusicSectionContinuesWithRelatedMusic() throws Exception {
        assertNextIsRelated(MediaGroup.TYPE_MUSIC);
    }

    @Test
    public void songFromSearchContinuesWithRelatedMusic() throws Exception {
        assertNextIsRelated(MediaGroup.TYPE_SEARCH);
    }

    @Test
    public void songFromSubscriptionsContinuesWithRelatedMusic() throws Exception {
        assertNextIsRelated(MediaGroup.TYPE_SUBSCRIPTIONS);
    }

    @Test
    public void songFromHistoryContinuesWithRelatedMusic() throws Exception {
        assertNextIsRelated(MediaGroup.TYPE_HISTORY);
    }

    @Test
    public void suggestionContinuesWithRelatedMusic() throws Exception {
        assertNextIsRelated(MediaGroup.TYPE_SUGGESTIONS);
    }

    @Test
    public void playlistContinuesWithTheNextVideoOfTheList() throws Exception {
        Video current = video("current", "WL");
        Video listNext = video("listNext", "WL");
        keep(VideoGroup.from(new ArrayList<>(Arrays.asList(video("first", "WL"), current, listNext))));
        current.nextMediaItem = SimpleMediaItem.from(video("related", null));

        TestController controller = new TestController(current);
        controller.onNewVideo(current);
        setSectionNext(controller, listNext); // found once the metadata arrives

        assertEquals("listNext", controller.getNext().videoId);
    }

    @Test
    public void channelVideosContinueWithTheNextUpload() throws Exception {
        Video current = video("current", null);
        Video upload = video("nextUpload", null);
        keep(typedGroup(MediaGroup.TYPE_CHANNEL_UPLOADS, current, upload));
        current.nextMediaItem = SimpleMediaItem.from(video("related", null));

        TestController controller = new TestController(current);
        controller.onNewVideo(current);
        setSectionNext(controller, upload);

        assertEquals("nextUpload", controller.getNext().videoId);
    }

    @Test
    public void nextOfThePreviousVideoIsForgottenWhenANewOneStarts() throws Exception {
        // Watch later video A: its next was B
        Video first = video("A", "WL");
        keep(VideoGroup.from(new ArrayList<>(Arrays.asList(first, video("B", "WL")))));
        TestController controller = new TestController(first);
        controller.onNewVideo(first);
        setSectionNext(controller, video("B", "WL"));

        // Then a video of another playlist starts and the car skips before its metadata arrived
        Video other = video("X", "PLother");
        keep(VideoGroup.from(new ArrayList<>(Arrays.asList(other, video("Y", "PLother")))));
        other.nextMediaItem = SimpleMediaItem.from(video("Y", "PLother"));
        controller.setVideo(other);
        controller.onNewVideo(other);

        assertEquals("Y", controller.getNext().videoId);
    }

    @Test
    public void historyGoesForwardAfterGoingBack() {
        Video first = video("first", null);
        Video second = video("second", null);
        Playlist.instance().add(first);
        Playlist.instance().add(second);
        Playlist.instance().setCurrent(first); // the user went back

        first.nextMediaItem = SimpleMediaItem.from(video("related", null));
        TestController controller = new TestController(first);

        assertEquals("second", controller.getNext().videoId);
    }

    // Previous

    @Test
    public void previousOfAHomeSongIsTheSongPlayedBefore() {
        Video played = video("playedBefore", null);
        Video current = video("current", null);
        keep(typedGroup(MediaGroup.TYPE_HOME, video("cardBefore", null), current));
        Playlist.instance().add(played);
        Playlist.instance().add(current);
        Playlist.instance().setCurrent(current);

        TestController controller = new TestController(current);
        controller.onNewVideo(current); // keeps its list alive, as the player does

        assertEquals("playedBefore", controller.getPrevious().videoId);
    }

    @Test
    public void previousOfAPlaylistVideoIsThePreviousOfTheList() {
        Video listPrevious = video("listPrevious", "WL");
        Video current = video("current", "WL");
        keep(VideoGroup.from(new ArrayList<>(Arrays.asList(listPrevious, current, video("next", "WL")))));
        Playlist.instance().add(video("playedBefore", null));
        Playlist.instance().add(current);
        Playlist.instance().setCurrent(current); // also a video played before: the list wins

        TestController controller = new TestController(current);
        controller.onNewVideo(current); // keeps its list alive, as the player does

        assertEquals("listPrevious", controller.getPrevious().videoId);
    }

    @Test
    public void noPreviousForTheFirstVideo() {
        Video current = video("current", null);
        Playlist.instance().add(current);
        Playlist.instance().setCurrent(current);

        assertNull(new TestController(current).getPrevious());
    }

    // Lists

    @Test
    public void onlyListsWithAnOrderArePlayedInOrder() {
        int[] feeds = {MediaGroup.TYPE_HOME, MediaGroup.TYPE_MUSIC, MediaGroup.TYPE_SEARCH, MediaGroup.TYPE_SUBSCRIPTIONS,
                MediaGroup.TYPE_HISTORY, MediaGroup.TYPE_TRENDING, MediaGroup.TYPE_NEWS, MediaGroup.TYPE_GAMING,
                MediaGroup.TYPE_NOTIFICATIONS, MediaGroup.TYPE_SUGGESTIONS, MediaGroup.TYPE_UNDEFINED};
        for (int type : feeds) {
            Video video = video("v", null);
            VideoGroup group = typedGroup(type, video, video("w", null));
            assertFalse("Feed type " + type, video.belongsToOrderedList());
            assertEquals(type, group.getType()); // keeps the list alive (the video holds it weakly)
        }

        int[] lists = {MediaGroup.TYPE_USER_PLAYLISTS, MediaGroup.TYPE_CHANNEL, MediaGroup.TYPE_CHANNEL_UPLOADS,
                MediaGroup.TYPE_MY_VIDEOS, MediaGroup.TYPE_SHORTS};
        for (int type : lists) {
            Video video = video("v", null);
            VideoGroup group = typedGroup(type, video, video("w", null));
            assertTrue("List type " + type, video.belongsToOrderedList());
            assertEquals(type, group.getType());
        }
    }

    @Test
    public void aPlaylistInsideAFeedIsAList() {
        Video video = video("v", "PLsongs");
        VideoGroup group = typedGroup(MediaGroup.TYPE_MUSIC, video, video("w", "PLsongs"));

        assertTrue(video.belongsToOrderedList());
        assertEquals(2, group.getSize());
    }

    @Test
    public void aVideoWithoutListIsNotAList() {
        assertFalse(video("v", null).belongsToOrderedList());
    }

    private static void assertNextIsRelated(int feedType) throws Exception {
        Video current = video("current", null);
        Video nextCard = video("nextCard", null);
        keep(typedGroup(feedType, video("before", null), current, nextCard));
        current.nextMediaItem = SimpleMediaItem.from(video("related", null));

        TestController controller = new TestController(current);
        controller.onNewVideo(current);
        setSectionNext(controller, nextCard); // as the old code found it

        assertEquals("related", controller.getNext().videoId);
    }

    private static VideoGroup typedGroup(int type, Video... videos) {
        VideoGroup group = VideoGroup.from(new ArrayList<>(Arrays.asList(videos)));
        group.setType(type);
        return group;
    }

    private static void setSectionNext(SuggestionsController controller, Video next) throws Exception {
        Field field = SuggestionsController.class.getDeclaredField("mNextSectionVideo");
        field.setAccessible(true);
        field.set(controller, next);
    }

    static Video video(String videoId, String playlistId) {
        Video video = new Video();
        video.videoId = videoId;
        video.playlistId = playlistId;
        return video;
    }

    /** A player that does nothing and answers zero/false/null */
    static PlaybackView idlePlayer() {
        return mock(PlaybackView.class);
    }

    private static class TestController extends SuggestionsController {
        private final PlaybackView mPlayer = idlePlayer();
        private Video mVideo;

        private TestController(Video video) {
            mVideo = video;
        }

        void setVideo(Video video) {
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
    }
}
