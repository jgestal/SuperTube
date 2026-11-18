package com.liskovsoft.smartyoutubetv2.mobile.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * In Android Auto the previous/next buttons stayed greyed out: the car only hands its buttons to the media apps it
 * knows, and SuperTube didn't say it was one. It must declare a media browser service and the "media" use, and that
 * service must hand the car the session the player plugs into.
 */
class CarMediaTest {
    private val ownPackage = "es.gestal.supertube"

    // The app tells Android Auto it's a media app

    @Test
    fun appDeclaresItselfAMediaAppOfTheCar() {
        val descriptor = manifestApplication().children("meta-data")
            .firstOrNull { it.android("name") == "com.google.android.gms.car.application" }

        assertNotNull("Android Auto doesn't know SuperTube is a media app", descriptor)
        assertEquals("@xml/automotive_app_desc", descriptor!!.android("resource"))

        val uses = parse(moduleFile("src/main/res/xml/automotive_app_desc.xml")).documentElement.children("uses")
        assertTrue("The descriptor must declare the media use", uses.any { it.getAttribute("name") == "media" })
    }

    @Test
    fun carCanConnectToTheMediaService() {
        val service = manifestApplication().children("service")
            .firstOrNull { it.android("name") == ".playback.CarMediaService" }

        assertNotNull("No media service for the car", service)
        assertEquals("The car app must be able to bind it", "true", service!!.android("exported"))
        val actions = service.children("intent-filter").flatMap { it.children("action") }.map { it.android("name") }
        assertTrue(actions.contains("android.media.browse.MediaBrowserService"))
    }

    @Test
    fun declaredServiceIsTheCarMediaService() {
        // Not initialized: the media library reads Android's log settings when it loads
        val service = Class.forName("com.liskovsoft.smartyoutubetv2.mobile.playback.CarMediaService", false, javaClass.classLoader)

        assertTrue(androidx.media.MediaBrowserServiceCompat::class.java.isAssignableFrom(service))
    }

    // Who may connect

    @Test
    fun androidAutoMayConnect() {
        assertTrue(CarMedia.isAllowedClient("com.google.android.projection.gearhead", ownPackage))
    }

    @Test
    fun carsWithAndroidBuiltInMayConnect() {
        assertTrue(CarMedia.isAllowedClient("com.android.car.media", ownPackage))
    }

    @Test
    fun theAppItselfMayConnect() {
        assertTrue(CarMedia.isAllowedClient(ownPackage, ownPackage))
    }

    @Test
    fun otherAppsOfThePhoneMayNot() {
        assertFalse(CarMedia.isAllowedClient("com.example.spy", ownPackage))
        assertFalse(CarMedia.isAllowedClient("com.android.systemui", ownPackage))
        assertFalse(CarMedia.isAllowedClient(null, ownPackage))
    }

    // One session for the car, every player plugs into it

    @Test
    fun playerReleasedSwitchesTheSessionOff() {
        val ownership = SessionOwnership()
        val player = Any()
        ownership.claim(player)

        assertTrue(ownership.release(player))
        assertFalse(ownership.isOwner(player))
    }

    @Test
    fun oldPlayerReleasingLateLeavesTheNewOnesSession() {
        val ownership = SessionOwnership()
        val oldPlayer = Any()
        val newPlayer = Any()
        ownership.claim(oldPlayer)
        ownership.claim(newPlayer) // the next video's engine took the session

        assertFalse("The car would lose the buttons of the video playing", ownership.release(oldPlayer))
        assertTrue(ownership.isOwner(newPlayer))
    }

    @Test
    fun releasingTwiceIsHarmless() {
        val ownership = SessionOwnership()
        val player = Any()
        ownership.claim(player)
        ownership.release(player)

        assertFalse(ownership.release(player))
    }

    @Test
    fun aNewPlayerCanTakeTheSessionAfterTheOldOneLeft() {
        val ownership = SessionOwnership()
        val first = Any()
        val second = Any()
        ownership.claim(first)
        ownership.release(first)
        ownership.claim(second)

        assertTrue(ownership.isOwner(second))
        assertTrue(ownership.release(second))
    }

    private fun manifestApplication(): Element =
        parse(moduleFile("src/main/AndroidManifest.xml")).documentElement.children("application").single()

    /** Unit tests run from the module folder; also from the project folder */
    private fun moduleFile(path: String): File = File(path).takeIf { it.exists() } ?: File("supertube", path)

    private fun parse(file: File) = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        .newDocumentBuilder().parse(file)

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length).map { nodes.item(it) }.filterIsInstance<Element>().filter { it.tagName == tag }
    }

    private fun Element.android(attribute: String): String =
        getAttributeNS("http://schemas.android.com/apk/res/android", attribute)
}
