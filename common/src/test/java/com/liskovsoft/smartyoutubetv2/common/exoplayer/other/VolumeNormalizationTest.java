package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class VolumeNormalizationTest {
    private static final float DELTA = 0.01f;

    @After
    public void tearDown() {
        VolumeNormalization.setWebStyle(false);
    }

    @Test
    public void webStyleMatchesTheYouTubeWebPlayer() {
        VolumeNormalization.setWebStyle(true);

        // YouTube web: min(1, 10^(-loudnessDb / 20))
        assertEquals(1f, webVolume(0f), DELTA); // already at the target
        assertEquals(1f, webVolume(-6f), DELTA); // quiet videos are never turned down
        assertEquals(0.71f, webVolume(3f), DELTA); // loud ones are
        assertEquals(0.5f, webVolume(6f), 0.17f); // very loud: MediaServiceCore caps the level, close enough
    }

    @Test
    public void webStyleWithoutLoudnessInfoPlaysAtFullVolume() {
        VolumeNormalization.setWebStyle(true);

        assertEquals(1f, VolumeNormalization.playerVolume(1f, true, 1f, false), DELTA); // live / no format info
        assertEquals(1f, VolumeNormalization.playerVolume(1f, true, 0.5f, false), DELTA); // loudnessDb missing
    }

    @Test
    public void webStyleKeepsTheUserVolumeAndHasNoBooster() {
        VolumeNormalization.setWebStyle(true);

        assertEquals(0.5f, VolumeNormalization.playerVolume(0.5f, true, levelFromLoudnessDb(-3f), false), DELTA);
        assertEquals(0.36f, VolumeNormalization.playerVolume(0.5f, true, levelFromLoudnessDb(3f), false), DELTA);
        assertEquals(1f, VolumeNormalization.playerVolume(1f, false, levelFromLoudnessDb(3f), false), DELTA);
        assertEquals(0f, VolumeNormalization.playerVolume(0f, true, levelFromLoudnessDb(0f), false), DELTA); // sound off
        assertEquals(1f, VolumeNormalization.playerVolume(1f, true, levelFromLoudnessDb(0f), true), DELTA); // shorts too

        assertEquals(1f, VolumeNormalization.boostVolume(1f, true), DELTA); // no booster
        assertEquals(1.5f, VolumeNormalization.boostVolume(1.5f, true), DELTA); // user asked for 150%
        assertEquals(1f, VolumeNormalization.playerVolume(1.5f, true, levelFromLoudnessDb(0f), false), DELTA);
    }

    @Test
    public void tvStyleIsUnchanged() {
        assertEquals(0.5f, VolumeNormalization.playerVolume(1f, true, 0.5f, false), DELTA);
        assertEquals(0.25f, VolumeNormalization.playerVolume(0.5f, true, 0.5f, false), DELTA);
        assertEquals(0.25f, VolumeNormalization.playerVolume(1f, true, 0.5f, true), DELTA);
        assertEquals(1f, VolumeNormalization.playerVolume(1f, false, 0.5f, false), DELTA);
        assertEquals(2f, VolumeNormalization.boostVolume(1f, true), DELTA);
        assertEquals(1f, VolumeNormalization.boostVolume(1f, false), DELTA);
    }

    private static float webVolume(float loudnessDb) {
        return VolumeNormalization.playerVolume(1f, true, levelFromLoudnessDb(loudnessDb), false);
    }

    /**
     * Copy of MediaItemFormatInfoImpl.getVolumeLevel() (MediaServiceCore).
     */
    private static float levelFromLoudnessDb(float loudnessDb) {
        float result = 1.0f;

        if (loudnessDb != 0f) {
            float normalLevel = (float) Math.pow(10.0, loudnessDb / 20.0f);
            if (normalLevel > 1.95) {
                normalLevel = 1.5f;
            }
            result = 2.0f - normalLevel;
        }

        return result / 2;
    }
}
