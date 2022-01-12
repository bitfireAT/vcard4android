/***************************************************************************************************
 * Copyright © All Contributors. See LICENSE and AUTHORS in the root directory for details.
 **************************************************************************************************/

package at.bitfire.vcard4android.contactrow

import android.content.ContentValues
import android.provider.ContactsContract.CommonDataKinds.Event
import at.bitfire.vcard4android.Contact
import ezvcard.util.PartialDate
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class EventHandlerTest {

    @Test
    fun testStartDate_Empty() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            putNull(Event.START_DATE)
        }, contact)
        assertNull(contact.anniversary)
        assertNull(contact.birthDay)
        assertTrue(contact.customDates.isEmpty())
    }

    @Test
    fun testStartDate_Full() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            put(Event.START_DATE, "1984-08-20")
        }, contact)
        assertEquals(
            Calendar.getInstance().apply {
                set(1984, /* zero-based */7,  20, 0, 0, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis,
            contact.customDates[0].property.date.time)
    }

    @Test
    fun testStartDate_Partial() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            put(Event.START_DATE, "--08-20")
        }, contact)
        assertEquals(PartialDate.parse("--0820"), contact.customDates[0].property.partialDate)
    }


    @Test
    fun testType_Anniversary() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            put(Event.START_DATE, "--08-20")
            put(Event.TYPE, Event.TYPE_ANNIVERSARY)
        }, contact)
        assertNotNull(contact.anniversary)
        assertNull(contact.birthDay)
        assertTrue(contact.customDates.isEmpty())
    }

    @Test
    fun testType_Birthday() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            put(Event.START_DATE, "--08-20")
            put(Event.TYPE, Event.TYPE_BIRTHDAY)
        }, contact)
        assertNull(contact.anniversary)
        assertNotNull(contact.birthDay)
        assertTrue(contact.customDates.isEmpty())
    }

    @Test
    fun testType_Custom_Label() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            put(Event.START_DATE, "--08-20")
            put(Event.TYPE, Event.TYPE_CUSTOM)
            put(Event.LABEL, "Label 1")
        }, contact)
        assertNull(contact.anniversary)
        assertNull(contact.birthDay)
        assertFalse(contact.customDates.isEmpty())
        assertEquals("Label 1", contact.customDates[0].label)
    }

    @Test
    fun testType_Other() {
        val contact = Contact()
        EventHandler.handle(ContentValues().apply {
            put(Event.START_DATE, "--08-20")
            put(Event.TYPE, Event.TYPE_OTHER)
        }, contact)
        assertNull(contact.anniversary)
        assertNull(contact.birthDay)
        assertFalse(contact.customDates.isEmpty())
    }

}