package com.liskovsoft.smartyoutubetv2.common.app.views;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoGroup;

public interface ChannelUploadsView {
    void update(VideoGroup videoGroup);
    void showProgressBar(boolean show);
    void clear();

    /**
     * Watch later with the last saved video first (true), oldest first (false), or as YouTube sends it (null: screens
     * without the option, e.g. TV)
     */
    default Boolean isWatchLaterNewestFirst() {
        return null;
    }
}
