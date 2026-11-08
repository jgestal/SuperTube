package com.liskovsoft.smartyoutubetv2.mobile.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenQualityTest {
    @Test
    fun picksTheBiggestStepThatFitsTheShortSide() {
        assertEquals(1080, ScreenQuality.fitHeight(1280)) // Xiaomi 17T Pro: 2772 x 1280
        assertEquals(1080, ScreenQuality.fitHeight(1080)) // FHD+ phones
        assertEquals(1440, ScreenQuality.fitHeight(1440)) // QHD+ phones
        assertEquals(1440, ScreenQuality.fitHeight(1600)) // 2560 x 1600 tablets
        assertEquals(2160, ScreenQuality.fitHeight(2160))
        assertEquals(720, ScreenQuality.fitHeight(720))
    }

    @Test
    fun screensSlightlyShortOfAStepKeepIt() {
        assertEquals(1080, ScreenQuality.fitHeight(1072))
        assertEquals(720, ScreenQuality.fitHeight(900))
    }

    @Test
    fun tinyOrUnknownScreens() {
        assertEquals(360, ScreenQuality.fitHeight(200))
        assertEquals(1080, ScreenQuality.fitHeight(0))
    }
}
