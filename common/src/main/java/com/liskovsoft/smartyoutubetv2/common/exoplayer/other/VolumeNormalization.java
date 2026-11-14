package com.liskovsoft.smartyoutubetv2.common.exoplayer.other;

/**
 * Player volume and loudness normalization ("auto volume").
 * <ul>
 * <li>TV style (SmartTube): the player volume is about halved and a {@link VolumeBooster} (the system LoudnessEnhancer,
 * a compressor) adds the gain back, so quiet videos are boosted on TV speakers.</li>
 * <li>Web style (SuperTube phone app): the same as YouTube in a browser. Loud videos are turned down, the others play
 * at full volume, and there is no booster. On phones the booster result depends on the device (on some it barely
 * works), so videos sounded quieter than in the browser.</li>
 * </ul>
 */
public final class VolumeNormalization {
    private static volatile boolean sWebStyle;

    private VolumeNormalization() {
    }

    public static void setWebStyle(boolean webStyle) {
        sWebStyle = webStyle;
    }

    public static boolean isWebStyle() {
        return sWebStyle;
    }

    /**
     * @param userVolume volume chosen in the player settings (1 is 100%, may be higher)
     * @param autoVolume loudness normalization enabled
     * @param videoLevel level of the video ({@code MediaItemFormatInfo.getVolumeLevel()})
     * @return the ExoPlayer volume (0...1)
     */
    public static float playerVolume(float userVolume, boolean autoVolume, float videoLevel, boolean isShorts) {
        if (sWebStyle) {
            float volume = Math.min(userVolume, 1f); // above 100% is done by the booster
            return autoVolume ? volume * webLevel(videoLevel) : volume;
        }

        float volume = userVolume;

        if (autoVolume) {
            if (volume < 1f) {
                volume *= videoLevel;
            } else {
                volume = videoLevel;
            }
        }

        if (isShorts) {
            volume /= 2;
        }

        return volume;
    }

    /**
     * Gain of the {@link VolumeBooster}. 1 or less means no booster.
     */
    public static float boostVolume(float userVolume, boolean autoVolume) {
        if (sWebStyle) {
            return userVolume; // only when the user asks for more than 100%
        }

        return autoVolume ? userVolume * 2.0f : userVolume;
    }

    /**
     * YouTube web player: {@code min(1, 10^(-loudnessDb / 20))}.<br/>
     * MediaServiceCore only gives the TV level: {@code (2 - 10^(loudnessDb / 20)) / 2}, with {@code 10^(loudnessDb / 20)}
     * capped to 1.5 above 1.95 (very loud videos), so {@code 10^(loudnessDb / 20) = 2 - 2 * level}.
     */
    static float webLevel(float videoLevel) {
        float loudness = 2f - 2f * videoLevel;

        if (loudness <= 1f) { // quieter than the target, unknown loudness or live (level 1): full volume
            return 1f;
        }

        return 1f / loudness;
    }
}
