package com.liskovsoft.smartyoutubetv2.mobile.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {
    @Test
    fun picksThePhoneApkFromAReleaseWithTvApks() {
        assertTrue(UpdateChecker.isPhoneApk("SuperTube-1.4.apk"))
        assertTrue(UpdateChecker.isPhoneApk("SuperTube_1.4_release.apk"))
        assertFalse(UpdateChecker.isPhoneApk("SuperTubeTV_stable_1.4_arm64-v8a.apk"))
        assertFalse(UpdateChecker.isPhoneApk("SuperTubeTV_stable_1.4_universal.apk"))
        assertFalse(UpdateChecker.isPhoneApk("supertube_tv.json"))
    }

    @Test
    fun comparesVersions() {
        assertTrue(UpdateChecker.isNewer("1.4", "1.3.2"))
        assertTrue(UpdateChecker.isNewer("1.10", "1.9"))
        assertFalse(UpdateChecker.isNewer("1.4", "1.4"))
        assertFalse(UpdateChecker.isNewer("1.4", "1.4-nightly-12"))
    }
}
