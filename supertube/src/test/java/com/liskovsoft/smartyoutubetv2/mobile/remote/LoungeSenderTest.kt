package com.liskovsoft.smartyoutubetv2.mobile.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoungeSenderTest {
    /** First answer of /bc/bind for a REMOTE_CONTROL device (shortened) */
    private val bindAnswer = """
        |245
        |[[0,["c","3A1B2C3D4E5F6A7B","",8]]
        |,[1,["S","HJ_kTq1nM2pXyZ"]]
        |,[2,["loungeStatus",{"devices":"[{\"app\":\"lb-v4\",\"name\":\"Salotto (SuperTube)\",\"type\":\"LOUNGE_SCREEN\"}]"}]]
        |,[3,["playlistModified",{"videoIds":""}]]
        |]
        |""".trimMargin()

    @Test
    fun readsSessionIds() {
        val ids = LoungeSender.parseChunks(bindAnswer)
        assertEquals("3A1B2C3D4E5F6A7B", ids["c"])
        assertEquals("HJ_kTq1nM2pXyZ", ids["S"])
    }

    @Test
    fun missingSession() {
        val ids = LoungeSender.parseChunks("4\n[[]]")
        assertNull(ids["c"])
        assertNull(ids["S"])
    }
}
