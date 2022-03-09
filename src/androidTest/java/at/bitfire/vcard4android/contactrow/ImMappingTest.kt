/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

class ImMappingTest {

    @Test
    fun testMessengerToUri_Known() {
        assertEquals(URI("aim:user"), ImMapping.messengerToUri("AIM", "user"))
        assertEquals(URI("https://facebook.com/user"), ImMapping.messengerToUri("Facebook", "user"))
        assertEquals(URI("https://icq.im/user"), ImMapping.messengerToUri("ICQ", "user"))
        assertEquals(URI("irc:/network/user"), ImMapping.messengerToUri("IRC", "/network/user"))
        assertEquals(URI("mqq://im/chat?uin=user"), ImMapping.messengerToUri("QQ", "user"))
        assertEquals(URI("skype:user"), ImMapping.messengerToUri("Skype", "user"))
        assertEquals(
            URI("https://threema.id/THREEMA_ID"),
            ImMapping.messengerToUri("Threema", "THREEMA_ID")
        )
        assertEquals(
            URI("xmpp:user@example.com"),
            ImMapping.messengerToUri("XMPP", "user@example.com")
        )
    }

    @Test
    fun testMessengerToUri_Null() {
        assertEquals(URI("user@example.com"), ImMapping.messengerToUri(null, "user@example.com"))
    }

    @Test
    fun testMessengerToUri_Unknown() {
        assertEquals(
            URI("unknown-massenger:user@example.com"),
            ImMapping.messengerToUri("Unknown Mässenger", "user@example.com")
        )
    }


    @Test
    fun testUriToMessenger_Blank() {
        assertEquals(
            Pair(null, ""),
            ImMapping.uriToMessenger(URI(""))
        )
    }

    @Test
    fun testUriToMessenger_Known() {
        assertEquals(
            Pair("AIM", "user"),
            ImMapping.uriToMessenger(URI("aim:user"))
        )
        assertEquals(
            Pair("facebook", "user"),
            ImMapping.uriToMessenger(URI("https://facebook.com/user"))
        )
        assertEquals(
            Pair("facebook", "user@example.com"),
            ImMapping.uriToMessenger(URI("xmpp:user@example.com"), "Facebook")
        )
        assertEquals(
            Pair("facebook", "user"),
            ImMapping.uriToMessenger(URI("xmpp:user@facebook.com"), "Facebook")
        )
        assertEquals(
            Pair("ICQ", "user"),
            ImMapping.uriToMessenger(URI("icq:user"))
        )
        assertEquals(
            Pair("ICQ", "user"),
            ImMapping.uriToMessenger(URI("https://icq.im/user"))
        )
        assertEquals(
            Pair("IRC", "freenode/user,isnick"),
            ImMapping.uriToMessenger(URI("irc:freenode/user,isnick"))
        )
        assertEquals(
            Pair("QQ", "user"),
            ImMapping.uriToMessenger(URI("mqq://im/chat?chat_type=wpa&uin=user"))
        )
        assertEquals(
            Pair("QQ", "user"),
            ImMapping.uriToMessenger(URI("mqq:user"))
        )
        assertEquals(
            Pair("QQ", "user"),
            ImMapping.uriToMessenger(URI("qq:user"))
        )
        assertEquals(
            Pair("Skype", "user"),
            ImMapping.uriToMessenger(URI("skype:user"))
        )
        assertEquals(
            Pair("Threema", "THREEMA_ID"),
            ImMapping.uriToMessenger(URI("https://threema.id/THREEMA_ID"))
        )
        assertEquals(
            Pair("XMPP", "user@example.com"),
            ImMapping.uriToMessenger(URI("xmpp:user@example.com"))
        )
    }

    @Test
    fun testUriToMessenger_RelativeUri() {
        assertEquals(
            Pair(null, "relative/uri@example.com"),
            ImMapping.uriToMessenger(URI("relative/uri@example.com"))
        )
    }

    @Test
    fun testUriToMessenger_RelativeUri_WithType() {
        assertEquals(
            Pair("MyMessenger", "uri@example.com"),
            ImMapping.uriToMessenger(URI("uri@example.com"), "MyMessenger")
        )
    }

    @Test
    fun testUriToMessenger_Unknown() {
        assertEquals(
            Pair("Unknown", "write?uid=test"),
            ImMapping.uriToMessenger(URI("unknown:write?uid=test"))
        )
    }

}