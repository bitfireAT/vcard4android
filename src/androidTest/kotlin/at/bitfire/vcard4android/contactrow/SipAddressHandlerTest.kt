/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.SipAddress
import at.bitfire.vcard4android.Contact
import ezvcard.parameter.ImppType
import org.junit.Assert.*
import org.junit.Test

class SipAddressHandlerTest {

    @Test
    fun testHandle_Empty() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            putNull(SipAddress.SIP_ADDRESS)
        }, contact)
        assertTrue(contact.impps.isEmpty())
    }

    @Test
    fun testHandle_Value() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            put(SipAddress.SIP_ADDRESS, "voip@example.com")
        }, contact)
        assertEquals("voip@example.com", contact.impps[0].property.handle)
    }


    @Test
    fun testTypeCustom_NoLabel() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            put(SipAddress.SIP_ADDRESS, "voip@example.com")
            put(SipAddress.TYPE, SipAddress.TYPE_CUSTOM)
        }, contact)
        assertTrue(contact.impps[0].property.types.isEmpty())
        assertNull(contact.impps[0].label)
    }

    @Test
    fun testTypeCustom_WithLabel() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            put(SipAddress.SIP_ADDRESS, "voip@example.com")
            put(SipAddress.TYPE, SipAddress.TYPE_CUSTOM)
            put(SipAddress.LABEL, "New SIP Type")
        }, contact)
        assertTrue(contact.impps[0].property.types.isEmpty())
        assertEquals("New SIP Type", contact.impps[0].label)
    }

    @Test
    fun testType_Home() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            put(SipAddress.SIP_ADDRESS, "voip@example.com")
            put(SipAddress.TYPE, SipAddress.TYPE_HOME)
        }, contact)
        assertEquals(ImppType.HOME, contact.impps[0].property.types[0])
    }

    @Test
    fun testType_Other() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            put(SipAddress.SIP_ADDRESS, "voip@example.com")
            put(SipAddress.TYPE, SipAddress.TYPE_OTHER)
        }, contact)
        assertTrue(contact.impps[0].property.types.isEmpty())
    }

    @Test
    fun testType_Work() {
        val contact = Contact()
        SipAddressHandler.handle(ContentValues().apply {
            put(SipAddress.SIP_ADDRESS, "voip@example.com")
            put(SipAddress.TYPE, SipAddress.TYPE_WORK)
        }, contact)
        assertEquals(ImppType.WORK, contact.impps[0].property.types[0])
    }

}