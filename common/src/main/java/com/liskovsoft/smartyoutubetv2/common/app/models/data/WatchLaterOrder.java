package com.liskovsoft.smartyoutubetv2.common.app.models.data;

import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.mediaserviceinterfaces.data.MediaGroup;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Order of the Watch later list. YouTube adds every saved video at the end, so the one just saved is the last of a
 * long list. Newest first shows the list the other way round: it needs every page, as the newest video is on the last one.
 */
public final class WatchLaterOrder {
    /** Browse id of the Watch later card (Playlists section) */
    public static final String CHANNEL_ID = "VLWL";
    public static final String PLAYLIST_ID = "WL";
    /** Watch later holds up to 5000 videos: room for all its pages, but never an endless loop */
    static final int MAX_PAGES = 200;

    /** Loads the page after the given one (null at the end of the list) */
    public interface Pages {
        MediaGroup next(MediaGroup page) throws Exception;
    }

    private WatchLaterOrder() {
    }

    public static boolean isWatchLater(Video item) {
        return item != null && CHANNEL_ID.equals(item.channelId);
    }

    /**
     * Whether YouTube's list has to be turned around to get the wanted order.
     *
     * @param youtubeOrder the order the playlist was given in "Sort playlist" (-1: never sorted, YouTube's default
     *                     oldest first)
     */
    public static boolean shouldReverse(boolean newestFirst, int youtubeOrder) {
        switch (youtubeOrder) {
            case MediaItemService.PLAYLIST_ORDER_ADDED_DATE_NEWER_FIRST:
                return !newestFirst;
            case MediaItemService.PLAYLIST_ORDER_POPULARITY:
            case MediaItemService.PLAYLIST_ORDER_PUBLISHED_DATE_NEWER_FIRST:
            case MediaItemService.PLAYLIST_ORDER_PUBLISHED_DATE_OLDER_FIRST:
                return false; // sorted by something else on purpose: keep it
            default:
                return newestFirst;
        }
    }

    /** The first page and every page after it */
    public static List<MediaGroup> loadAll(MediaGroup first, Pages pages) throws Exception {
        List<MediaGroup> result = new ArrayList<>();
        MediaGroup page = first;

        while (page != null && result.size() < MAX_PAGES) {
            result.add(page);
            page = page.getNextPageKey() != null ? pages.next(page) : null;
        }

        return result;
    }

    /** The videos of all the pages, last saved first, without the next page of YouTube's order (there is none) */
    public static VideoGroup reversed(List<MediaGroup> pages, String fallbackTitle) {
        List<Video> videos = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        String title = null;
        int type = MediaGroup.TYPE_UNDEFINED;

        for (MediaGroup page : pages) {
            if (title == null && page.getTitle() != null && !page.getTitle().isEmpty()) {
                title = page.getTitle();
            }
            if (type == MediaGroup.TYPE_UNDEFINED) {
                type = page.getType();
            }
            if (page.getMediaItems() == null) {
                continue;
            }
            for (MediaItem item : page.getMediaItems()) {
                Video video = Video.from(item);
                // A video moved while the pages were loading may come twice
                if (video != null && (video.videoId == null || seen.add(video.videoId))) {
                    videos.add(video);
                }
            }
        }

        Collections.reverse(videos);
        VideoGroup group = VideoGroup.from(videos); // no media group: nothing left to continue
        group.setTitle(title != null ? title : fallbackTitle);
        group.setType(type);
        group.setId(group.hashCode()); // like the groups made from a page
        return group;
    }
}
