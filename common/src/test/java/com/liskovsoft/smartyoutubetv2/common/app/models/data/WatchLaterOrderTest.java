package com.liskovsoft.smartyoutubetv2.common.app.models.data;

import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * Watch later shows the last saved video first by default (YouTube adds it at the end of the list), and the user can
 * go back to YouTube's order, oldest first.
 */
public class WatchLaterOrderTest {
    // Which order

    @Test
    public void newestFirstTurnsYouTubesOrderAround() {
        assertTrue(WatchLaterOrder.shouldReverse(true, -1)); // never sorted: YouTube's order, oldest first
    }

    @Test
    public void oldestFirstIsYouTubesOwnOrder() {
        assertFalse(WatchLaterOrder.shouldReverse(false, -1));
    }

    @Test
    public void listAlreadySortedNewestFirstOnYouTubeIsNotTurnedAround() {
        assertFalse(WatchLaterOrder.shouldReverse(true, MediaItemService.PLAYLIST_ORDER_ADDED_DATE_NEWER_FIRST));
        assertTrue(WatchLaterOrder.shouldReverse(false, MediaItemService.PLAYLIST_ORDER_ADDED_DATE_NEWER_FIRST));
    }

    @Test
    public void listSortedOldestFirstOnYouTube() {
        assertTrue(WatchLaterOrder.shouldReverse(true, MediaItemService.PLAYLIST_ORDER_ADDED_DATE_OLDER_FIRST));
        assertFalse(WatchLaterOrder.shouldReverse(false, MediaItemService.PLAYLIST_ORDER_ADDED_DATE_OLDER_FIRST));
    }

    @Test
    public void listSortedBySomethingElseIsKept() {
        for (int order : new int[] {MediaItemService.PLAYLIST_ORDER_POPULARITY,
                MediaItemService.PLAYLIST_ORDER_PUBLISHED_DATE_NEWER_FIRST, MediaItemService.PLAYLIST_ORDER_PUBLISHED_DATE_OLDER_FIRST}) {
            assertFalse("Order " + order, WatchLaterOrder.shouldReverse(true, order));
        }
    }

    // Which list

    @Test
    public void onlyTheWatchLaterListIsSorted() {
        Video watchLater = new Video();
        watchLater.channelId = "VLWL";
        Video otherPlaylist = new Video();
        otherPlaylist.channelId = "VLPLsongs";

        assertTrue(WatchLaterOrder.isWatchLater(watchLater));
        assertFalse(WatchLaterOrder.isWatchLater(otherPlaylist));
        assertFalse(WatchLaterOrder.isWatchLater(new Video()));
        assertFalse(WatchLaterOrder.isWatchLater(null));
    }

    // Newest first

    @Test
    public void lastSavedVideoComesFirst() throws Exception {
        // YouTube: oldest first, in pages; the video just saved is the last of the last page
        Page third = page(null, "e");
        Page second = page(third, "c", "d");
        Page first = page(second, "a", "b");

        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(first, Page::following), "Watch later");

        assertEquals(Arrays.asList("e", "d", "c", "b", "a"), ids(group));
    }

    @Test
    public void everyPageIsLoaded() throws Exception {
        List<String> saved = new ArrayList<>();
        Page next = null;
        for (int i = 9; i >= 0; i--) { // 10 pages of 2 videos
            next = page(next, "v" + (2 * i), "v" + (2 * i + 1));
        }
        for (int i = 0; i < 20; i++) {
            saved.add("v" + i);
        }

        List<MediaGroup> pages = WatchLaterOrder.loadAll(next, Page::following);
        Collections.reverse(saved);

        assertEquals(10, pages.size());
        assertEquals(saved, ids(WatchLaterOrder.reversed(pages, "Watch later")));
    }

    @Test
    public void pageThatFailsToComeEndsTheList() throws Exception {
        Page first = page(null, "a", "b");
        first.nextPageKey = "more"; // says there's more, but the next page doesn't come

        List<MediaGroup> pages = WatchLaterOrder.loadAll(first, page -> null);

        assertEquals(1, pages.size());
    }

    @Test
    public void listThatNeverEndsIsCut() throws Exception {
        Page endless = page(null, "a");
        endless.nextPageKey = "more";

        List<MediaGroup> pages = WatchLaterOrder.loadAll(endless, page -> endless);

        assertEquals(WatchLaterOrder.MAX_PAGES, pages.size());
    }

    @Test
    public void emptyPagesInTheMiddleAreSkipped() throws Exception {
        // e.g. "Hide watched from Watch later" emptied a page
        Page last = page(null, "c");
        Page empty = page(last);
        Page first = page(empty, "a", "b");

        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(first, Page::following), "Watch later");

        assertEquals(Arrays.asList("c", "b", "a"), ids(group));
    }

    @Test
    public void videoListedTwiceIsShownOnce() throws Exception {
        Page second = page(null, "b", "c");
        Page first = page(second, "a", "b");

        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(first, Page::following), "Watch later");

        assertEquals(Arrays.asList("c", "b", "a"), ids(group));
    }

    @Test
    public void emptyWatchLater() throws Exception {
        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(page(null), Page::following), "Watch later");

        assertTrue(group.isEmpty());
    }

    @Test
    public void wholeListIsShownSoNothingIsLoadedOnScroll() throws Exception {
        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(page(null, "a", "b"), Page::following), "Watch later");

        assertNull("A media group would make the screen load YouTube's next page", group.getMediaGroup());
    }

    @Test
    public void listKeepsItsTitleAndType() throws Exception {
        Page first = page(null, "a");
        first.title = "Ver más tarde";

        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(first, Page::following), "fallback");

        assertEquals("Ver más tarde", group.getTitle());
        assertEquals(MediaGroup.TYPE_USER_PLAYLISTS, group.getType());
        assertEquals("fallback", WatchLaterOrder.reversed(WatchLaterOrder.loadAll(page(null, "a"), Page::following), "fallback").getTitle());
    }

    @Test
    public void playingGoesDownTheShownList() throws Exception {
        Page second = page(null, "c");
        Page first = page(second, "a", "b");

        VideoGroup group = WatchLaterOrder.reversed(WatchLaterOrder.loadAll(first, Page::following), "Watch later");

        for (Video video : group.getVideos()) {
            assertSame("Each video plays its own list", group, video.getGroup());
            assertTrue(video.belongsToOrderedList()); // the next video is the next card, not a related one
        }
        assertEquals(Arrays.asList("c", "b", "a"), ids(group));
    }

    private static List<String> ids(VideoGroup group) {
        List<String> ids = new ArrayList<>();
        for (Video video : group.getVideos()) {
            ids.add(video.videoId);
        }
        return ids;
    }

    /** A page of Watch later as YouTube sends it */
    private static Page page(Page next, String... videoIds) {
        Page page = new Page();
        page.next = next;
        page.nextPageKey = next != null ? "page-" + System.identityHashCode(next) : null;
        for (String id : videoIds) {
            Video video = new Video();
            video.videoId = id;
            video.playlistId = WatchLaterOrder.PLAYLIST_ID;
            page.items.add(SimpleMediaItem.from(video));
        }
        return page;
    }

    private static class Page implements MediaGroup {
        final List<MediaItem> items = new ArrayList<>();
        Page next;
        String nextPageKey;
        String title;

        static MediaGroup following(MediaGroup page) {
            return ((Page) page).next;
        }

        @Override
        public int getType() {
            return TYPE_USER_PLAYLISTS;
        }

        @Override
        public List<MediaItem> getMediaItems() {
            return items;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getChannelId() {
            return WatchLaterOrder.CHANNEL_ID;
        }

        @Override
        public String getParams() {
            return null;
        }

        @Override
        public String getReloadPageKey() {
            return null;
        }

        @Override
        public String getNextPageKey() {
            return nextPageKey;
        }

        @Override
        public String getChannelUrl() {
            return null;
        }

        @Override
        public boolean isEmpty() {
            return items.isEmpty();
        }
    }
}
