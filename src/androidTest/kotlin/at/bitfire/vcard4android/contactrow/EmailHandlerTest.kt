/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Email
import at.bitfire.vcard4android.Contact
import at.bitfire.vcard4android.property.CustomType
import ezvcard.parameter.EmailType
import org.junit.Assert.*
import org.junit.Test

class EmailHandlerTest {

    @Test
    fun testAddress_Empty() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            putNull(Email.ADDRESS)
        }, contact)
        assertTrue(contact.emails.isEmpty())
    }

    @Test
    fun testAddress_WithAddress() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
        }, contact)
        assertEquals("test@example.com", contact.emails[0].property.value)
    }


    @Test
    fun testIsPrimary_False() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.IS_PRIMARY, 0)
        }, contact)
        assertNull(contact.emails[0].property.pref)
    }

    @Test
    fun testIsPrimary_True() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.IS_PRIMARY, 1)
        }, contact)
        assertEquals(1, contact.emails[0].property.pref)
    }


    @Test
    fun testTypeCustom_NoLabel() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.TYPE, Email.TYPE_CUSTOM)
        }, contact)
        assertTrue(contact.emails[0].property.types.isEmpty())
        assertNull(contact.emails[0].label)
    }

    @Test
    fun testTypeCustom_WithLabel() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.TYPE, Email.TYPE_CUSTOM)
            put(Email.LABEL, "My Email")
        }, contact)
        assertTrue(contact.emails[0].property.types.isEmpty())
        assertEquals(contact.emails[0].label, "My Email")
    }

    @Test
    fun testTypeHome() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.TYPE, Email.TYPE_HOME)
        }, contact)
        assertArrayEquals(arrayOf(EmailType.HOME), contact.emails[0].property.types.toTypedArray())
        assertNull(contact.emails[0].label)
    }

    @Test
    fun testTypeOther() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.TYPE, Email.TYPE_OTHER)
        }, contact)
        assertTrue(contact.emails[0].property.types.isEmpty())
        assertNull(contact.emails[0].label)
    }

    @Test
    fun testTypeWork() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.TYPE, Email.TYPE_WORK)
        }, contact)
        assertArrayEquals(arrayOf(EmailType.WORK), contact.emails[0].property.types.toTypedArray())
        assertNull(contact.emails[0].label)
    }

    @Test
    fun testTypeMobile() {
        val contact = Contact()
        EmailHandler.handle(ContentValues().apply {
            put(Email.ADDRESS, "test@example.com")
            put(Email.TYPE, Email.TYPE_MOBILE)
        }, contact)
        assertArrayEquals(arrayOf(CustomType.Email.MOBILE), contact.emails[0].property.types.toTypedArray())
        assertNull(contact.emails[0].label)
    }

}